# Adding a new MC version to the test matrix

Example: adding `26.4`. All 14+ ModUtilities folders keep an identical `tools/autotest`.

1. **Project folders** (under the mods root, e.g. `E:/Dokumente/Minecraft/mods`), exact naming:
   - `MC_ModUtilities/mc_26.4_MC_ModUtilities`
   - `BankSystem/mc_26.4_BankSystem`
   - `StockMarket/MC_26.4_StockMarket` (uppercase `MC_`)

   Matching is case-insensitive on the prefix `mc_<version>_`. Copies such as `... - Kopie` are ignored
   as long as the real folder exists (shortest name wins).

2. **Dev features**: the version must support `enable_dev_features` (ModUtilities root `build.gradle`
   excludes `testing/**` and `sandbox/**` unless it is `true`; BankSystem/StockMarket have the same switch).
   run_matrix passes `-Penable_dev_features=true` itself, but the dependent mods compile against the
   jars in the sibling folders' `build/libs` (`flatDir`), so build those as dev jars first:
   - ModUtilities: `gradlew build -Penable_dev_features=true` in `mc_26.4_MC_ModUtilities`
   - for StockMarket also BankSystem: the same in `mc_26.4_BankSystem`

3. **Copy `tools/autotest`** from an existing ModUtilities folder into `mc_26.4_MC_ModUtilities`
   (all files: `run_matrix.py`, `matrix_gui.py`, `matrix_gui.bat`, `capture.init.gradle`,
   `CaptureAgent.java`, docs).

4. **Add the version to the GUI**: the `VERSIONS` list near the top of `matrix_gui.py`, in every
   ModUtilities folder (keep them identical). run_matrix.py needs no change.

5. **Requirements**: JDK on `PATH` (`javac`/`jar`, 21+; the game itself runs on the JDK Gradle/Loom
   picks for that version), ports 25611, 25612, 25690 free (no other run active).

   Pitfalls:
   - Folders are named after the version line (`mc_26.1_*`), but `minecraft_version` in
     `gradle.properties` may be a patch version (26.1 folders build against `26.1.2`). BankSystem /
     StockMarket `build.gradle` `flatDir` paths built from `${minecraft_version}` then point to a
     non-existent folder (`mc_26.1.2_MC_ModUtilities`): pin the folder name there.
   - New MC versions can change vanilla `server.properties` defaults (26.3: `white-list=true`). Add
     such keys to the server.properties list in `prepare()` of `run_matrix.py`.

6. **Single cell first**:

   ```
   python tools/autotest/run_matrix.py E:/Dokumente/Minecraft/mods/MC_ModUtilities/mc_26.4_MC_ModUtilities --loaders fabric --configs sp --gui
   ```

   Then double-click the version label in the GUI to run the whole row, and do the same for
   BankSystem and StockMarket.

## When the launch capture breaks

Symptom: every cell of the row is orange with `launch spec missing`, the row label is red.

- Read `<out>/<project>/gradle-capture.log` (`<out>` = `<mods root>/autotest-out`).
- Compile errors: usually the dev jars of step 2 are missing or stale.
- The run tasks were renamed or no longer accept `jvmArgs`: adjust the task names / hook in
  `capture.init.gradle` (it adds `-javaagent:capture.jar=<dir>/<project>-<task>.json` to
  `runClient`/`runServer`).
- The capture agent records only the JVM's own view (`java.home`, classpath, JVM args, main command,
  environment), see `CaptureAgent.java` and DESIGN.md section 5; it rarely needs changes.
