# Agent rules for kheniparth/NuvioDesktop

Fork of NuvioMedia/NuvioDesktop (Windows + macOS M1) adding Live TV ported from Nuvio Enhanced.

- **Start here:** read [PROGRESS.md](PROGRESS.md). It is the single progress doc for Desktop fork work
  in this checkout. Cross-repo Nuvio context also lives in `~/nuvio/nuvio-tv/PROGRESS.md` and
  `~/nuvio/nuvio-tv/LIVE_TV_PORT_PLAN.md`. "Continue the project" means: pick up PROGRESS.md "Next steps".
- **End of every interactive work session:** update PROGRESS.md (past, current state, next steps, open
  issues, decisions, gotchas; dates YYYY-MM-DD) and commit and push it through a PR to `Dev`. No secrets:
  name the file a secret lives in, never its value.
- Work in WSL (`~/nuvio/nuvio-desktop`), never Gradle on Windows or on `/mnt/d`.
- Branch from `Dev` as `paperclip/<issue-key>-<short-name>` (e.g. `paperclip/TEC-10-fork-setup`).
- Never open a PR, issue or comment on `NuvioMedia/*`. PRs target `kheniparth/NuvioDesktop`, base `Dev`.
- **Never merge Nuvio Enhanced into this fork** (~1,050 compile errors). Copy Live TV files in.
- One Gradle build at a time on beast (~13 GB of ~23 GB WSL RAM).
- Needs a local (gitignored) `local.properties` with at least `sdk.dir=...` pointing at the
  Android SDK (e.g. `/home/techmuzz/Android/Sdk`). Gradle 9 fails `generateRuntimeConfigs` if it
  is missing.

## Checks (run one at a time)

```bash
./gradlew :composeApp:desktopTest
./gradlew :composeApp:compileKotlinDesktop
./gradlew :composeApp:compileAndroidMain
```

(`compileDebugKotlinAndroid` does not exist on this project; shared Android/common compile is
`compileAndroidMain`.) Confirm desktop task names with
`./gradlew :composeApp:tasks --all | grep -i desktop` if they drift.

## Upstream sync

From a clean `Dev`: `bash scripts/upstream-sync.sh` creates `sync/upstream-YYYYMMDD`, merges
`upstream/Dev`, runs the checks above, and does **not** push. Open the sync PR yourself.
