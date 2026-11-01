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
```

Options:

| option | default | meaning |
|---|---|---|
| `--loaders` | `fabric neoforge` | loaders to test |
| `--configs` | `sp mp slave spslave` | `sp` singleplayer; `mp` client on a master server; `slave` master + slave server, client on the slave; `spslave` master server + singleplayer world as its slave |
| `--timeout` | `900` | per-instance watchdog (seconds) |
| `--mods` | all | comma separated mod ids to test, e.g. `banksystem,stockmarket` |
| `--out` | `./autotest-out` | output folder |
| `--offline` | off | pass `--offline` to Gradle |

Exit code 0 when every cell passed.

## Output

```
<out>/summary.json
<out>/<project>/gradle-capture.log
<out>/<project>/<loader>/<config>/<instance>/console.log      game log
<out>/<project>/<loader>/<config>/<instance>/results.json     test report of that server
<out>/<project>/<loader>/<config>/client/results.json.client.json   client status
<out>/<project>/<loader>/<config>/<instance>/logs/UnitTests/<mod>.log   human readable report
```

## Running a single instance by hand

The hook works in any dev run: add e.g. `-Dmodutilities.autotest=me -Dmodutilities.autotest.world=new`
to the VM options of an IDE run configuration of the client; it creates a world, runs the tests and quits.
All properties are listed in `AutoTest.java`.
