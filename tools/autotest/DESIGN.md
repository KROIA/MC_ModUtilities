# Automated in-game test runs: design

Goal: one command per MC version folder starts the real game, runs every registered in-game unit
test in every server configuration on Fabric and NeoForge, and prints pass/fail.

## 1. Dev-only code is compiled out of release builds

**One switch: `enable_dev_features` in `gradle.properties`.**

- `build.gradle` (root, `subprojects {}`): when the key is not `true`, `sourceSets.main.java` excludes
  `net/kroia/modutilities/testing/**` and `net/kroia/modutilities/sandbox/**` in every module.
  Excluded means *not compiled*: any release class that still references dev code is a compile error,
  so nothing leaks silently.
- Release code reaches dev code through exactly one entry point:
  `ModUtilitiesMod.init()` → `if (ENABLE_DEV_FEATURES) initDevFeatures()` →
  `Class.forName("net.kroia.modutilities.testing.DevBootstrap").getMethod("init").invoke(null)`.
  `DevBootstrap.init()` starts the sandbox (which registers the ModUtilities suites and the
  `/modutilities test` command) and `AutoTest.init()`.
- The Java flag follows from the Gradle switch, so the two can never disagree:

  ```java
  public static final boolean ENABLE_DEV_FEATURES = devClassesPresent(); // DevBootstrap compiled in?
  ```

  This is a variant of option (a): the key lives in gradle.properties, but instead of generating a
  Java source file the constant is derived from whether Gradle compiled the dev packages. No generated
  sources (IDE import works without running a task), no second place to edit.
- There are no test-only mixins or event listeners outside `testing/` and `sandbox/`, so no separate
  mixin config is needed.
- Not excluded: sandbox assets (blockstates/models/lang under `assets/modutilities`). They are data,
  not code; exclude them in `processResources` if that matters.

Where the user flips the switch: `gradle.properties` (`enable_dev_features = true|false`), not the
Java file any more.

Dependent mods (BankSystem, StockMarket) need the same pattern for their own test code, and a dev
build of them needs a dev build of ModUtilities (the `testing` API only exists there).

## 2. Test framework changes (ModUtilities `testing/`)

- `TestRunner` accepts a `null` player (headless run: log file only) and keeps every result
  (`getResults()`, keyed `category/SuiteClass`; setup/teardown failures become `<setup>`/`<teardown>`
  ERROR entries). Chat output and `logs/UnitTests/<mod>.log` are unchanged.
- New `testing/autotest/AutoTest` (common) and `AutoTestClient` (client-only class).

## 3. AutoTest hook

Inert unless the JVM has `-Dmodutilities.autotest=<instance label>` (and only exists in dev builds).

Server side (dedicated or integrated), every server tick until done:

1. First tick: if `config/modutilities/MultiServerConfig.json` enables multi-server but no mod created
   the link (ModUtilities alone; BankSystem creates it at `SERVER_STARTED`), create it from the config.
2. Readiness, re-checked every tick, no sleeps:
   - `waitPlayer` (default true): a player is online;
   - slave: `MultiServerManager.isSlave() && isRunning()` (link to master up);
   - master: `getConnectedSlaveIDs().size() >= waitSlaves`;
   - `after=<file>`: file exists (used to run the slave after the master's run).
   All conditions must then hold for 40 ticks (settle) before the run starts.
3. Run every mod's suites that `canRunOn(isSlave)` with the first online player (or none), on the
   server thread, like the `/x test` command does.
4. Write `results.json` atomically (tmp + rename; other instances poll it), then a dedicated server
   with `stop=true` halts itself. In singleplayer a static "finished" flag tells the client to quit.
5. After `SERVER_STOPPED` a dedicated autotest server calls `System.exit(0)` (from its own thread):
   the dev launcher (Architectury `TransformerRuntime`, TinyRemapper/classpath reading) leaves two
   non-daemon 32-thread pools (`pool-1`, `pool-2`) behind, so a dev server JVM never exits by itself.
   Dev-launch only; production servers are not affected.
   The client starts a daemon thread on quit that halts the JVM if `Minecraft.stop()` has not exited it
   within 30 s (NeoForge 26.x dev runs hang in their shutdown watchdog after closing the game module).

Client side (`AutoTestClient`): on the title screen (after 20 ticks) either
`ConnectScreen.startConnecting(host:port)` or `createWorldOpenFlows().createFreshLevel(...)` with a fixed
seed. Quits (`Minecraft.stop()`) when the integrated server finished, when the dedicated server drops
the connection after its run, or when the connection fails. Writes `results.json.client.json`.

Watchdog: a daemon thread per JVM; after `timeout` seconds it writes a `timeout` report and halts.
Peaceful difficulty is set on every autotest server.

Properties (prefix `modutilities.autotest.`): `results`, `waitPlayer`, `waitSlaves`, `after`, `stop`,
`timeout`, `mods`, `connect`, `world`.

## 4. API drift

None needed toggles. Checked with javap on all 14 versions:

| API | 1.21.1 … 26.3 |
|---|---|
| `WorldOpenFlows.createFreshLevel(String, LevelSettings, WorldOptions, Function<X, WorldDimensions>, Screen)` | X = `RegistryAccess` (1.21.1), `HolderLookup.Provider` (1.21.2+) — a method reference `WorldPresets::createNormalWorldDimensions` compiles against both |
| `LevelSettings` constructor | drifts (GameRules package 1.21.11, `DifficultySettings` 26.1) — avoided by using `MinecraftServer.DEMO_SETTINGS` (present everywhere) and setting peaceful difficulty server-side |
| `WorldOptions(long, boolean, boolean)`, `ConnectScreen.startConnecting(6 args)`, `ServerData(String, String, Type)`, `ServerAddress.parseString`, `Minecraft.stop/level/isLocalServer`, `MinecraftServer.halt/setDifficulty/isDedicatedServer` | stable |
| `Minecraft.screen` | removed in 26.2 → existing `ClientCompat.screen()` |
| flat world presets | `createFlatWorldDimensions` only 1.21.2–26.1 → singleplayer uses the normal preset |

## 5. Launching several instances of one project

Two `gradlew` runs in one project block each other (Loom's per-project cache lock is held for the
whole build). So Gradle is only used up front, to *record* the launch (one short Gradle run per
run task: on Gradle 9 `runClient` + `runServer` in one build fail task validation):

- `-Pautotest_agent=<capture.jar>=<dir>` adds `-javaagent:capture.jar=<dir>/<loader>-<task>.json` to
  `runClient`/`runServer` (root build.gradle, identical in every version folder).
- `CaptureAgent.premain` writes `java.home`, `java.class.path`, `sun.java.command`, the JVM input
  arguments (`--module-path=…`, `--add-opens=…`, Architectury's own agent, …) and the environment
  (NeoForge's `MOD_CLASSES`), then `halt(0)`s before the game starts. Gradle sees a normal exit.
- The orchestrator starts each instance as `java @jvm.args` (argfile: the classpath exceeds the
  Windows command-line limit) in its own run directory.

This uses only the JVM's view of its own launch, so it does not depend on Loom internals (which differ
between Loom 1.7 and 1.17) and works the same on Gradle 8/Loom remap and Gradle 9/loom-no-remap.
The capture passes `-Penable_dev_features=true` and runs `:common:compileJava`, `:common:jar` (writes `common/build/libs`, not the
`fabric/neoforge/build/libs` jars the dependent mods use), Loom's launch preparation, and nothing else.

## 6. Orchestrator (`run_matrix.py`)

Per project: compile the agent, capture, then for each loader × config a fresh folder
`<out>/<project>/<loader>/<config>/<instance>/` with generated `server.properties`
(distinct ports, `online-mode=false`, flat, peaceful, `max-tick-time=-1`), `eula.txt`,
`config/modutilities/MultiServerConfig.json`, client `options.txt` (no onboarding, no pause on focus loss).

| config | instances and order | tested instances |
|---|---|---|
| `sp` | client creates a world, multi-server off | client (integrated server) |
| `mp` | master (`waitPlayer`) → client joins master | master |
| `slave` | master (`waitSlaves=1`, no player, keeps running) → slave (`after=master results`, `waitPlayer`) → client joins slave | master, slave |
| `spslave` | master (`waitSlaves=1`, no player) → client creates a world configured as slave (`after=master results`) | master, client |

Shutdown: the client quits by itself (bounded by its watchdog, killed after timeout + 60 s);
self-stopping servers get 120 s; then every remaining server gets `stop` on stdin, 60 s, then
`taskkill /T /F`. Ready detection for servers: `Done (` in the console log.

Output: per-cell line while running, `summary.json`, and a table
project × loader × config → OK/FAIL, passed, failed, errors, seconds, instance statuses.
Exit code 0 only if every cell passed.
