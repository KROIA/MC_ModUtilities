# Automated in-game tests

`run_matrix.py` starts the real game and runs every registered in-game unit test
(`TestRegistry`, the same suites as `/modutilities test`, `/banksystem test`, `/stockmarket test`)
on Fabric and NeoForge in four server configurations, then prints a pass/fail table.
Design: [DESIGN.md](DESIGN.md).

## Requirements

- The script compiles the project with `-Penable_dev_features=true`, whatever `gradle.properties` says
  (BankSystem/StockMarket additionally need a ModUtilities jar built with dev features).
- Python 3.9+ (stdlib only), a JDK 21+ `javac`/`jar` on `PATH`, Windows (Linux works too).
- Free local ports 25611, 25612 (game) and 25690 (multi-server link).

## Usage

```
python tools/autotest/run_matrix.py <project folder> [<project folder> ...]
python tools/autotest/run_matrix.py --root <mods root>/MC_ModUtilities --versions 1.21.1 26.3
python tools/autotest/run_matrix.py <project folder> --world <world folder>
python tools/autotest/run_matrix.py <project folder> --play fabric mp [--world <world folder>]
```

Per project it first builds the loaders' mod jars (`gradlew :common:build :<loader>:build -x test -Penable_dev_features=true`; `:common` because dependents
compile against its plain/`-dev` jar via `flatDir`),
so the tested code and the jars are the same; a failed build marks the project's cells as error. Before that it
builds the `flatDir` dependency projects the same way (ModUtilities, then BankSystem; each once per call, also
in `--play`), so a dependent mod never runs against stale dependency jars.
Tests run in a fresh superflat world, or with `--world` in a fresh copy of that world (place blocks there to
test such cases; the same world is used for every MC version).

Options:

| option | default | meaning |
|---|---|---|
| `--loaders` | `fabric neoforge` | loaders to test |
| `--configs` | `sp mp slave spslave` | `sp` singleplayer; `mp` client on a master server; `slave` master + slave server, client on the slave; `spslave` master server + singleplayer world as its slave |
| `--timeout` | `900` | per-instance watchdog (seconds) |
| `--mods` | all | comma separated mod ids to test, e.g. `banksystem,stockmarket` |
| `--out` | `<mods root>/autotest-out` | output folder (shared by every run, so the GUI shows CLI runs too) |
| `--offline` | off | pass `--offline` to Gradle |
| `--gui` | off | also open `matrix_gui.py` on the same `--out` (unless one is already open) |
| `--play L C` | off | manual play of one project, loader `L`, config `C`: the same instances and multi-server setup as the test cell, no tests, no auto-stop. Ends when the game client is closed or `<out>/play/stop` exists (GUI Stop); servers get `stop`, then are killed. Output in `<out>/play/<project>/<loader>/<config>/` |
| `--clean-build` | off | no tests: `gradlew clean build -x test -Penable_dev_features=false` (release jars) of every given project, after clearing its Loom remap cache (locked cache = failure, no `gradlew --stop`: that would kill the parallel builds). One chain per MC version (`mc_<version>_` folder prefix) in the given order (dependencies first); `--jobs` chains run at once. A failure skips the rest of its chain (`dependency <name> failed`), other versions go on. Log `<out>/<project>/clean-build.log`, status `<out>/clean-build.json`, result per loader in `<out>/<project>/build.json`; exit code 1 if any failed |
| `--jobs N` | `4` | `--clean-build`: MC versions built at once |
| `--build-kind K` | `release` | `--clean-build`: `release` (`-Penable_dev_features=false`) or `dev` (`true`, jars include the tests). A test run right after a dev clean build finds the jars UP-TO-DATE (no recompile); it still clears the Loom remap cache of mods with `flatDir` dependencies, so those re-remap their dependency jars (seconds) |
| `--test-jobs N` | `2` | `sp` cells run at once (own game dir, no ports, ~2-4 GB RAM each, one game window each), beside the multi-server cells, which still run one at a time (shared ports), and beside the builds/captures of the next projects. `1` = everything sequential |
| `--world DIR` | new superflat | tests and `--play`: world folder (with `level.dat`) copied fresh into every instance of every cell: servers `<instance>/world`, singleplayer client `saves/play` (opened directly). The template is never modified. An older world is upgraded (the client confirms the backup/upgrade prompts by itself); a world newer than the cell's MC fails the cell (`world ... is from a newer Minecraft`) |
| `--luckperms M` | `off` | LuckPerms dimension, see [LuckPerms](#luckperms). `off`: the plain cells (unchanged); `on`: `<config>-lp` cells only; `both`: plain + `-lp`; `removed`: only the two-phase `lp-removed` cell per loader; `all`: plain + `-lp` + `lp-removed` |
| `--exclude KEY ...` | none | test topics not to run in this call, added to the selection file's list, see [Test topics](#test-topics) |
| `--only KEY ...` | none | run only these test topics; ignores the selection file's exclude list (`--exclude` and the offline modes still apply) |
| `--all-tests` | off | ignore the selection file's exclude list: every topic on, each in its mode (online/offline). Use it for the pre-release run |
| `--offline-tests KEY ...` | none | also run these topics offline (JUnit) instead of in game, added to the selection file's offline list; only suites that need no game |
| `--no-offline-tests` | off | run every topic in game (ignores the offline list and `--offline-tests`) |
| `--selection FILE` | `<out>/test-selection.json` | another selection file |
| `--wait-lock` | off | another run holds the port lock: wait (poll every 5 s) instead of exiting |

Exit code 0 when every cell passed. Only one run at a time, whatever its `--out`: every run (tests, `--play`,
`--clean-build`) takes the global port lock `<mods root>/autotest-out/running.lock` (all runs share the test ports).
A second one exits with a message, or with `--wait-lock` waits until the first one ends.

## Test topics

A topic is a test suite, key `<modid>/<Category>/<SuiteClass>`, e.g. `modutilities/mu_gui/GuiLogicTests`
(category = `TestCategory` name, the same `<Category>/<SuiteClass>` key as in `results.json`). A pattern is a key
or a prefix of it on `/` boundaries: `<modid>/<Category>` = every suite of that category, `<modid>` = the whole
mod. A `*` segment matches any one segment: `stockmarket/*/BotStressTestSuite`. Exact, case-sensitive, no regex.

- **Modes**: every topic is `online` (in game, the default), `offline` (JUnit via `gradlew :common:test`, no game;
  only suites whose `needsGame()` is false; such a suite can still run online) or `off` (not run).
- **Selection file** `<out>/test-selection.json` (same folder as `gui-settings.json`):
  `{"exclude": [patterns], "offline": [patterns]}`. `exclude` = off, `offline` = offline (off wins); everything
  else is online, so new suites run in game by default. The GUI's **Topics...** dialog writes it; edit it by hand or
  from a script the same way. Every test run reads it (CLI and GUI runs alike); a missing file = full run in game.
- **Game side** (mod-agnostic, `TestRegistry`): system properties `modutilities.tests.exclude` and
  `modutilities.tests.include` (comma separated patterns). If include is set, only matching suites run; exclude
  applies on top. A deselected suite is not run (no setup/teardown, no counts); `results.json` lists it per mod in
  `excludedSuites` (not `skippedSuites`), the top level has `excludedSuites` = how many suites; the unit-test log
  has an `EXCLUDED <key>` line. Every mod's `topics` lists all its registered suites, deselected ones included,
  as `{"key": "<Category>/<SuiteClass>", "needsGame": bool}`. `modutilities.tests.offline` (patterns): a matching
  suite that needs no game is skipped in game (log `OFFLINE <key>`, per mod `offlineSuites` in `results.json`, not
  counted as excluded); a matching suite that needs the game still runs in game, so a wrong key never drops a suite.
  run_matrix passes these properties to every instance.
- **Topic list**: after each cell run_matrix copies each mod's `topics` into `<out>/topics.json`
  (`{modid: [{"key", "needsGame"}]}`, per mod the latest run's list; the older list of plain keys still reads,
  as needsGame). The GUI menu is built from it, so it is empty until a mod ran once.
- **Offline run**: if the offline list is not empty, run_matrix runs per project, once per call, after its jar build
  and before its cells: stale `common/build/test-results/test/TEST-*.xml` deleted, then `gradlew :common:cleanTest
  :common:test -Penable_dev_features=true -Dmodutilities.tests.offline=<list>` (plus include/exclude). It parses the
  JUnit XML into the project's **offline row** (`loader` `junit`, `config` `offline`: passed/failed/errors/skipped,
  `excludedSuites`, `failures`; `<out>/<project>/offline.json` for the GUI) and passes the in-game cells
  `modutilities.tests.offline` = exactly the suite keys that ran offline, so nothing runs twice and nothing is lost.
  A project without an `OfflineTests.run(` caller in `common/src/test` gets
  `n/a: no offline tests` (ok) and runs all its suites in game; so does a project whose Gradle run produced no
  results (offline row: error). A Gradle failure without a failed test (compile error, timeout) is one error.
- **Dependent setup** (BankSystem, StockMarket have it; see their `common/build.gradle` and `common/src/test`): a
  `@TestFactory` calling `OfflineTests.run(<registration>)`; the test task forwards the three
  `modutilities.tests.*` properties, inside `afterEvaluate { }` (Loom creates the mapped compile classpath only
  then); `test.include '**/*Test.class'` so the class scan does not load the suites (in release mode their
  `compileOnly` deps are missing); `compileTestJava`/`test` `onlyIf` the ModUtilities jar contains
  `OfflineTests.class` (release jars have no `testing` package). Suites that touch `@ExpectPlatform`,
  `UtilitiesPlatform`, registries or a server stay `needsGame() = true`.
- **Summary**: every row of `summary.json` (and `cell.json`, `offline.json`) has `only` and `excluded` (the
  patterns of that run); the table ends with `test topics: PARTIAL: only ...; excluded ...` or
  `test topics: all (full run)`, plus `; offline (JUnit ...): <patterns>`. Offline topics count as run: "full" =
  nothing excluded and no `--only`.
- **By hand** (`gradlew :common:test`): `OfflineTests` honours the same properties, `common/build.gradle` forwards
  them: `gradlew :common:test -Dmodutilities.tests.exclude=modutilities/mu_gui`. Tests are named
  `<modid>/<Category>/<SuiteClass>/<test>`; a deselected suite shows as one skipped `.../<excluded>` test. If
  `modutilities.tests.offline` is set only the capable suites it matches run; unset = every capable suite.

Workflow: run a new feature's suite on its own (`--only <key>`), then switch it off in **Topics...** (or add it to
the file) to keep everyday runs fast. Before a release: **All on** (or `--all-tests`) and run everything; check
that the summary says `all (full run)`.

```
python tools/autotest/run_matrix.py <project> --only stockmarket/*/BotStressTestSuite
python tools/autotest/run_matrix.py <project> --exclude modutilities/mu_gui stockmarket
python tools/autotest/run_matrix.py <project> --all-tests
python tools/autotest/run_matrix.py <project> --offline-tests modutilities/mu_utilities
python tools/autotest/run_matrix.py <project> --all-tests --no-offline-tests   (everything in game)
```

## LuckPerms

LuckPerms is never a dependency of the mods. For tests it is put on the **dev runtime** classpath only, opt-in with
the Gradle property `-Pwith_luckperms=true` (Loom `modLocalRuntime`: not in the jar, not in the published POM/module).
Versions (Modrinth Maven, separate builds per loader) are in `gradle.properties`: `luckperms_fabric_version`,
`luckperms_neoforge_version`. `gradlew :fabric:runServer -Pwith_luckperms=true` works by hand too.
NeoForge uses 5.4.149 (Modrinth tags it 1.21.3, its mods.toml has no Minecraft/NeoForge range): 5.4.140, the last
build tagged 1.21.1, kicks every joining player with "Capability has not been initialised" (LuckPerms #3963, #4048);
5.4.149 is the oldest build with the fix (LuckPerms commit b18717e) and passes the tests on 1.21.1 / NeoForge 21.1.84.

With `--luckperms on|both|all` the launch commands are captured a second time with `-Pwith_luckperms=true`
(`<out>/<project>/launch-lp/`) and every `--configs` entry also runs as `<config>-lp` (cell dir
`<out>/<project>/<loader>/<config>-lp/`). On NeoForge each instance gets `defaultconfigs/neoforge-server.toml` with
`permissionHandler = "luckperms:permission_handler"`, so `PermissionCompat` node checks go through LuckPerms
(LuckPerms also sets it itself when the value is the default). LuckPerms' Fabric build is server-only
(`"environment": "server"`), so on Fabric `sp-lp` / `spslave-lp` clients run without it and its tests skip; use `mp-lp`.

`lp-removed` (`--luckperms removed|all`, one cell per loader, cell dir `<loader>/lp-removed/phase1|phase2/`):
1. phase 1: `mp` with LuckPerms. Tests record expectations in `<world>/testing/luckperms_phase1.json`
   (`LuckPermsPhase.record`).
2. phase 2: `mp` without LuckPerms on a copy of phase 1's master world (NeoForge: the world's server config still
   names LuckPerms' handler, NeoForge falls back to its default). `LuckPermsRemovedTests` (and dependents' phase-2
   checks) run only there.

The cell fails if phase 1 wrote no phase file (LuckPerms missing, e.g. a dependent without the Gradle lines below),
or phase 2 ran no `LuckPermsRemoved*` test. Its counts are phase 1 + phase 2.

Skipped tests: a test returns `skip(reason)` (`TestResult.Status.SKIP`) when a precondition is missing, e.g.
LuckPerms tests without LuckPerms. They count as neither passed nor failed: `skipped` in `results.json` (per mod and
total), the `skip` column of the summary table, `SKIP <name> (<reason>)` in `logs/UnitTests/<mod>.log`, grey in chat.
The GUI only shows the plain config columns; `-lp` / `lp-removed` results are in the summary table, `summary.json`
and their `cell.json`.

### Dependent mods

Same lines in the dependent's `fabric/build.gradle` / `neoforge/build.gradle` (and the two versions in its
`gradle.properties`), e.g. for Fabric:

```groovy
if ((findProperty('with_luckperms') ?: 'false').toBoolean()) {
    repositories {
        maven {
            url = 'https://api.modrinth.com/maven'
            content { includeGroup 'maven.modrinth' }
        }
    }
    dependencies {
        modLocalRuntime "maven.modrinth:luckperms:${rootProject.luckperms_fabric_version}" // neoforge: luckperms_neoforge_version
        // Fabric only: LuckPerms needs fabric-permissions-api-v0 >= 0.3.1. ModUtilities ships it jar-in-jar, but that
        // copy is not loaded in a dependent's dev run, so without this line LuckPerms refuses to load.
        modLocalRuntime "me.lucko:fabric-permissions-api:${rootProject.fabric_permissions_api_version}"
    }
}
```

Fabric also needs `fabric_permissions_api_version = 0.3.1` in the dependent's `gradle.properties` (the same value as
ModUtilities'; 0.3.1 is the last release for MC <= 1.21.1).

Phase-file contract (`net.kroia.modutilities.testing.LuckPermsPhase`, dev builds only):
- Phase 1 (`LuckPermsBridge.isAvailable()`): `LuckPermsPhase.record(server, "<modid>", key, value)` stores strings
  under the mod id in `<world>/testing/luckperms_phase1.json` (`{"mods": {"<modid>": {key: value}}}`); a no-op
  (returns false) without LuckPerms. Record in a test of a suite that runs on every LuckPerms run; save the mod's own
  state as usual (the server saves on stop). Example keys: `admin.<uuid>=true`, `syncSetting=on`, `migrationMarker=1`.
- Phase 2: `LuckPermsPhase.isPhase2(server)` (phase file present, LuckPerms not loaded) gates the checks;
  `expected(server, modid, key)` / `expectations(server, modid)` return what phase 1 recorded. Return `skip(...)`
  when not in phase 2. Name the suite `*LuckPermsRemoved*` (the matrix counts those as phase-2 tests).
- Waiting for LuckPerms futures in a test: `TestSuite.await(future, ms)` / `pumpUntil` / `pump` keep running queued
  server tasks while waiting (LuckPermsBridge listeners are delivered as server tasks).

## GUI

`matrix_gui.py` shows every mod x MC version (rows) x loader x config (columns) live, and starts runs.
Open it by double-clicking `matrix_gui.bat`, with `python tools/autotest/matrix_gui.py [--out DIR]`,
or with `run_matrix.py ... --gui`. Default `--out` is `<mods root>/autotest-out` (`<mods root>` = 4 levels
above this folder), the same as run_matrix.py.

| color | meaning |
|---|---|
| grey | not run |
| light blue | Gradle is capturing the launch for this row (row label red: capture failed, see tooltip) |
| amber | running |
| green | passed (cell text: number of passed tests) |
| red | failed tests (`9 fail`) |
| orange | error: no results, timeout, crash, launch spec missing (`1 err`) |
| purple | interrupted: the run was killed while this cell ran |
| dark, `n/a` | no project folder for this mod/version |
| teal, `PLAY` | a play session of this cell is running |
| lighter colour, `*` suffix | stale: a mod jar or the test world changed since this result (tooltip: which jar, `jar missing` or `test world changed`) |
| `~` suffix | partial run: topics were deselected (tooltip: `test topics: PARTIAL: ...`) |

- **build** column (first per loader): the last Gradle build of the row's own project (BankSystem rows: the
  BankSystem project), recorded by run_matrix in `<out>/<project>/build.json` per loader. Green `release`
  (`--clean-build`), blue `dev` (test runs, `--build-kind dev`), red `FAIL` (`> Task :<loader>:... FAILED`, or every loader when
  e.g. `:common` failed or it timed out), orange `no jar` (built, jar gone), light grey `jar ?` (jar without a
  recorded build), grey empty (neither), amber `building` (a build of it runs now, also dependency builds of a
  test run or Play), purple `intr` (that build was killed). A `?` suffix: the jar is newer than the recorded build (built outside
  the tool). Hover: kind, rc, start/finish, jar path/time/size, build log path, error excerpt (`* What went wrong:`).
  Right-click: **Clean build** of that project (`--clean-build <project>`, kind from the combobox). The status bar counts `build failed`.
- Results persist: reopening the GUI shows the last results (read from `cell.json`). Only
  **Clear results** (all cells; logs, `play/` and settings are kept) or **Clear result** in a cell's
  menu resets cells to "not run".
- Stale: each `cell.json` stores the sha256 of the jars the run used (the project's own
  `<loader>/build/libs` jar plus the ModUtilities/BankSystem jars it gets via `flatDir`). The GUI
  compares them with the current jars (hashes cached by mtime/size). A ModUtilities jar change makes
  ModUtilities, BankSystem and StockMarket cells stale; a BankSystem jar change the StockMarket (and
  BankSystem) cells. Results from before fingerprinting count as stale. The test world counts too:
  `cell.json` stores its path (`world`) and fingerprint (`world_fp`: path + name/size/mtime of its files);
  choosing another test world or saving the template marks results stale. Stale cells are rerun by
  **Run not passed**.
- BankSystem rows have no own runs: every StockMarket cell also runs the banksystem suites, so a
  BankSystem cell shows the banksystem counts of the matching StockMarket cell ("results via
  StockMarket"). Running or clearing a BankSystem cell does so for the StockMarket cell.

- Hover: details (counts, status string, seconds, finished time, cell folder, failed test names).
- Double-click a cell: run that cell; double-click a version label: run the whole row; right-click a
  cell: menu **Play** / **Run tests** / **Clear result** / **Open folder** /
  **Open play folder** (disabled until the cell was played).
- **Play**: starts that project/loader/config for manual play (`--play`, see above); the client joins
  the server or opens the world by itself. Stop by closing the game or with **Stop**.
- **Choose test world...** / **Choose play world...**: pick a world folder (must contain `level.dat`) for the
  test runs (`--world` of Run all / Run not passed / Run tests) and for Play, independently; cancel the dialog
  to go back to a new superflat world. Saved in `<out>/gui-settings.json` (`test_world`, `play_world`).
- Column checkboxes (under each test column header): only ticked columns take part in **Run all**,
  **Run not passed** and a row double-click; unticked headers are grey. Single-cell actions (cell double-click,
  **Run tests**, **Play**) ignore them. Right-click a loader header: tick/untick its 4 columns. Saved as
  `columns_off` (e.g. `["fabric/mp"]`) in `gui-settings.json`; all on by default.
- **Run all** / **Run not passed**: every (non-green or stale) cell of the ticked mods and columns. Runs are queued,
  one `run_matrix.py` call per set of projects with the same loaders/configs selection (Run all: one call), so
  sp cells of several projects run side by side; output goes to `<out>/gui-run.log`. **test jobs** (1-8, saved
  as `test_jobs` in `gui-settings.json`, default 2) is passed as `--test-jobs`. The status bar lists every
  running cell.
- **Clean build all**: `--clean-build --build-kind <kind> --jobs <build jobs>` of every existing project (per MC
  version ModUtilities, then BankSystem, then StockMarket), so fresh jars are in `<loader>/build/libs`. The
  combobox next to it picks the kind, `release` or `dev` (saved as `build_kind`, default `release`; also used by
  the right-click **Clean build**). **build jobs** (1-14, saved
  as `build_jobs` in `gui-settings.json`, default 4): MC versions built at once. The status bar shows the progress
  (`clean build 12/42: <running projects>`), and failed projects until the next run. Test results show stale afterwards (the next test run rebuilds dev jars).
- **Topics...**: tree mod > category > suite from `<out>/topics.json` ("run once to discover topics" while it is
  empty) with each node's mode (`online`, `offline`, `off`, `mixed`; suites that need the game say
  `(needs game)`; rows colored green / blue / gray / amber). Click a node: a menu sets the mode of it and everything below (`offline` only where possible,
  disabled as `offline (needs game)` when nothing below can). Saved at once to `<out>/test-selection.json` (as suite
  keys; patterns that match no listed suite are kept and shown below the hint) and used by every following run.
  **All on (full run)** empties the exclude list and keeps the modes; **All capable offline** / **All online** set
  the offline list. While the exclude list is not empty the main window shows a red **PARTIAL: n topics off**
  label (click: opens the dialog). See [Test topics](#test-topics).
- **offline** column (after the loader columns, header `junit`): the project's offline row of its last test run
  (`<out>/<project>/offline.json`; BankSystem rows: StockMarket's), colours as the test cells, `n/a` = no offline
  tests in that project, `~` = partial. Hover: counts, excluded suites, failed tests. It runs with every test run of
  the project while a topic is offline; **Clear results** clears it too.
- **Stop**: a play session is stopped gracefully (`<out>/play/stop`); otherwise (or pressed again) it kills
  the active run_matrix (also one started from the command line) and its games.
- Run/Play are refused while any run or play session holds the global `running.lock` (shared ports), also one
  with another `--out`; **Stop** then kills that run.
- New MC version: [ADDING_A_VERSION.md](ADDING_A_VERSION.md).

## Output

```
<out>/summary.json
<mods root>/autotest-out/running.lock                       global port lock (for every --out)
<out>/gui.lock, gui-run.log, gui-settings.json, clean-build.json
<out>/test-selection.json, topics.json                         test-topic selection and topic list
<out>/<project>/offline.json                                  offline (JUnit) row of the project's last test run
<out>/play/play.json, play/<project>/<loader>/<config>/<instance>/   play sessions
<out>/<project>/gradle-capture.log, clean-build.log, capture.json, build.json   build.json: last build per loader (GUI build column)
<out>/<project>/<loader>/<config>/cell.json                   cell status (read by the GUI)
<out>/<project>/<loader>/<config>-lp/...                      same with LuckPerms (--luckperms)
<out>/<project>/<loader>/lp-removed/phase1|phase2/<instance>/ two-phase LuckPerms-removed cell
<out>/<project>/<loader>/<config>/<instance>/console.log      game log
<out>/<project>/<loader>/<config>/<instance>/results.json     test report of that server
<out>/<project>/<loader>/<config>/client/results.json.client.json   client status
<out>/<project>/<loader>/<config>/<instance>/logs/UnitTests/<mod>.log   human readable report
```

## Known flakes

- **Architectury Transformer race (NeoForge, mostly slave/multi-instance cells):** a client dies at startup with
  `ClassCastException: HashMap$Node cannot be cast to HashMap$TreeNode` in `RuntimeFileAccess.modifyFile`
  (several instances transform the shared dev jars at once). The runner detects the crash and aborts the cell
  ("client crashed on startup, cell aborted"); re-run the cell. Not a mod bug.

## Running a single instance by hand

The hook works in any dev run: add e.g. `-Dmodutilities.autotest=me -Dmodutilities.autotest.world=new`
to the VM options of an IDE run configuration of the client; it creates a world, runs the tests and quits.
All properties are listed in `AutoTest.java`.
