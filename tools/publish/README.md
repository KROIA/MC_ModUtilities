# publish

Uploads already-built jars (`<project>/<loader>/build/libs/<archives_name>-<loader>-<mod_version>.jar`) of
BankSystem and StockMarket to Modrinth and CurseForge. Builds nothing. ModUtilities is not published: it is
bundled (jar-in-jar) in BankSystem, and StockMarket gets it through BankSystem.

```
python publish.py                                   # dry run: everything in publish.json
python publish.py --mods BankSystem --versions 1.21.1 1.21.2 --loaders fabric --platforms modrinth
python publish.py ... --upload                      # really upload
python publish.py --selftest
```

GUI: double-click `publish_gui.bat` (or `python publish_gui.py`). Tabs: **Jars** (dry-run state per jar and
platform, details of the selected row, Upload selected / Upload all ready, both ask before uploading; log below)
and **Settings** (edits publish.json; Save validates it). It shows whether the token env vars are set, never their
values. Unsaved settings are not used for uploads.

Tokens come only from env vars `MODRINTH_API` (scope VERSION_CREATE) and `CURSEFORGE_API`
(CurseForge > Account > API Tokens). Never put them in `publish.json`.

## Before the first upload

Per mod in `publish.json`: `modrinth` (slug or id, resolved via the public `GET /v2/project/{slug}`),
`curseforge_id` (numeric, project overview page), `curseforge_slug`. Check `dependencies` (`type`: required / optional / embedded / incompatible; `loaders`
limits a dependency to some loaders; `{"mod": "..."}` refers to another mod in this file).

## Rules

- `curseforge_manual_release` (default true) sends CurseForge `isMarkedForManualRelease`: an approved file is not
  released until you release it by hand on CurseForge.
- `--upload` refuses if any selected jar has a problem: jar missing, DEV build (dev-only packages from
  `dev_packages` found, also inside jar-in-jar), mod version differs from the other versions, wrong git branch,
  missing `changelog/v<modver>.md`, unsupported/open range, range matching no release. Build with `enable_dev_features=false`.
- Game versions come from the jar's own metadata (single source of truth): fabric.mod.json `depends.minecraft` /
  neoforge.mods.toml `minecraft` `versionRange`, matched against each platform's release versions (Modrinth public
  tag list in the dry run; CurseForge `minecraft-*` versions before upload). Accepted forms: fabric exact version,
  `>=a <b`, or a list of those; maven `[a]`, `[a,b)`, `[a,b]`. Anything else (`~`, wildcards, open ranges like
  `[a,)`) is a problem: tighten the mod metadata. To widen compatibility, edit the mod metadata, not this tool.
- Before the first upload all game versions, loaders and dependency slugs are resolved; unknown names abort.
- Uploads run BankSystem, then StockMarket (dependency order) and stop at the first error.
- `published.json` (gitignored) records every successful upload and is written after each one; recorded
  entries are skipped. Delete an entry to re-upload it.

## Templates

`display_name`, `version_number`, `changelog` placeholders: `{mod}` `{mc}` (project folder version, e.g. 26.1) `{modver}` (2.2.0)
`{mod_version}` (1.21.1-2.2.0) `{loader}` (fabric) `{Loader}` (Fabric) `{repo}` `{branch}`.
