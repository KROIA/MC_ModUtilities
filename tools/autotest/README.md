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
python tools/autotest/run_matrix.py --root E:/Dokumente/Minecraft/mods/MC_ModUtilities --versions 1.21.1 26.3
python tools/autotest/run_matrix.py <project folder> --play fabric mp [--world <world folder>]
```

Per project it first builds the loaders' mod jars (`gradlew :<loader>:build -x test -Penable_dev_features=true`),
so the tested code and the jars are the same; a failed build marks the project's cells as error. Before that it
builds the `flatDir` dependency projects the same way (ModUtilities, then BankSystem; each once per call, also
in `--play`), so a dependent mod never runs against stale dependency jars.
Test worlds are always fresh superflat worlds.

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
| `--world DIR` | new superflat | `--play`: world folder (with `level.dat`) copied fresh into every instance: servers `<instance>/world`, singleplayer client `saves/play` (opened directly). The template is never modified |

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
| lighter colour, `*` suffix | stale: a mod jar changed since this result (tooltip: which jar, or `jar missing`) |

- Results persist: reopening the GUI shows the last results (read from `cell.json`). Only
  **Clear results** (all cells; logs, `play/` and settings are kept) or **Clear result** in a cell's
  menu resets cells to "not run".
- Stale: each `cell.json` stores the sha256 of the jars the run used (the project's own
  `<loader>/build/libs` jar plus the ModUtilities/BankSystem jars it gets via `flatDir`). The GUI
  compares them with the current jars (hashes cached by mtime/size). A ModUtilities jar change makes
  ModUtilities, BankSystem and StockMarket cells stale; a BankSystem jar change the StockMarket (and
  BankSystem) cells. Results from before fingerprinting count as stale. Stale cells are rerun by
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
- **Choose play world...**: pick a world folder (must contain `level.dat`) for Play; cancel the dialog to
  go back to a new superflat world. Saved in `<out>/gui-settings.json`.
- **Run all** / **Run not passed**: every (non-green or stale) cell of the ticked mods. Runs are queued,
  one `run_matrix.py` call per project; output goes to `<out>/gui-run.log`.
- **Stop**: a play session is stopped gracefully (`<out>/play/stop`); otherwise (or pressed again) it kills
  the active run_matrix (also one started from the command line) and its games.
- Run/Play are refused while any run or play session holds `running.lock` (shared ports).
- New MC version: [ADDING_A_VERSION.md](ADDING_A_VERSION.md).

## Output

```
<out>/summary.json
<out>/running.lock, gui.lock, gui-run.log, gui-settings.json
<out>/play/play.json, play/<project>/<loader>/<config>/<instance>/   play sessions
<out>/<project>/gradle-capture.log, capture.json
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
