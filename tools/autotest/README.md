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

Exit code 0 when every cell passed. Only one run per output folder at a time (`running.lock`;
all runs share the test ports): a second one exits with a message.

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
- **Stop**: a play session is stopped gracefully (`<out>/play/stop`); otherwise (or pressed again) it kills
  the active run_matrix (also one started from the command line) and its games.
- Run/Play are refused while any run or play session holds `running.lock` (shared ports).
- New MC version: [ADDING_A_VERSION.md](ADDING_A_VERSION.md).

## Output

```
<out>/summary.json
<out>/running.lock, gui.lock, gui-run.log, gui-settings.json, clean-build.json
<out>/play/play.json, play/<project>/<loader>/<config>/<instance>/   play sessions
<out>/<project>/gradle-capture.log, clean-build.log, capture.json, build.json   build.json: last build per loader (GUI build column)
<out>/<project>/<loader>/<config>/cell.json                   cell status (read by the GUI)
<out>/<project>/<loader>/<config>/<instance>/console.log      game log
<out>/<project>/<loader>/<config>/<instance>/results.json     test report of that server
<out>/<project>/<loader>/<config>/client/results.json.client.json   client status
<out>/<project>/<loader>/<config>/<instance>/logs/UnitTests/<mod>.log   human readable report
```

## Running a single instance by hand

The hook works in any dev run: add e.g. `-Dmodutilities.autotest=me -Dmodutilities.autotest.world=new`
to the VM options of an IDE run configuration of the client; it creates a world, runs the tests and quits.
All properties are listed in `AutoTest.java`.
