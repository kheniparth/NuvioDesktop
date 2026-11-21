# NuvioDesktop progress

Last updated: 2026-11-21. Single source of truth for picking Desktop fork work back up with any agent.
Update it at the end of every work session (see [AGENTS.md](AGENTS.md)). Cross-repo plan and TV
progress: `~/nuvio/nuvio-tv/PROGRESS.md` and `~/nuvio/nuvio-tv/LIVE_TV_PORT_PLAN.md`.

## Goal
Keep this fork current with upstream `NuvioMedia/NuvioDesktop` (`Dev`), then port Live TV from Nuvio
Enhanced (copy files only) for Windows and macOS M1.

## Repos and checkouts (all in WSL Ubuntu on "beast")
| What | GitHub | Base branch | Checkout |
|---|---|---|---|
| Desktop fork (this repo) | kheniparth/NuvioDesktop (upstream NuvioMedia/NuvioDesktop) | `Dev` | `~/nuvio/nuvio-desktop` |
| TV fork (sibling) | kheniparth/NuvioTV | `dev` | `~/nuvio/nuvio-tv` |
| Enhanced (reference source, read only) | luqmanfadlli/NuvioMobile-Enhanced | `enhanced` | `~/nuvio/nuvio-enhanced` |
| Desktop pieces already written | Enhanced `windows-desktop` | commit `7a790e185` | read via `git show` |

## Past work
- 2026-11-17: Forked from NuvioMedia/NuvioDesktop; remotes `origin` + `upstream` configured.
- 2026-11-20: [TEC-10](/TEC/issues/TEC-10) — AGENTS.md, PROGRESS.md, `scripts/upstream-sync.sh`, first
  upstream merge on `sync/upstream-20261120`, Gradle checks recorded (see Current state).
- 2026-11-21: [TEC-10](/TEC/issues/TEC-10) — Tester3 PASS (`desktopTest` 2 of 2029 failures recorded);
  Architect 2 reviewed as a Parth-authorized one-off (no non-author senior developer was free) and merged
  [PR #10](https://github.com/kheniparth/NuvioDesktop/pull/10) into `Dev` as `178459874`; docs follow-up
  [PR #11](https://github.com/kheniparth/NuvioDesktop/pull/11) → `baa29326c`.
- 2026-11-21: [TEC-11](/TEC/issues/TEC-11) — worktree + branch `paperclip/TEC-11-livetv-data` created;
  sub-task 1 filed as [TEC-97](/TEC/issues/TEC-97) for Dev1.

## Current state (2026-11-21)
- `Dev` is `baa29326c` (PR #11 docs follow-up on top of PR #10 `178459874`): `AGENTS.md`,
  `PROGRESS.md`, `scripts/upstream-sync.sh`, and the upstream `Dev` merge through `bcb930c2d`.
- `sync/upstream-20261120` stays at `e69090674` (merged; not rebased or force-pushed).
- [TEC-11](/TEC/issues/TEC-11) unblocked after [TEC-10](/TEC/issues/TEC-10). Worktree:
  `/home/techmuzz/nuvio/scratch/nuvio-desktop-tec-11` on `paperclip/TEC-11-livetv-data` @ `baa29326c`.
  Sub-task 1 is [TEC-97](/TEC/issues/TEC-97) (copy AppFileCache + data files). No Live TV code in the
  tree yet.
- Gradle needs a local gitignored `local.properties` (`sdk.dir=...`) and `JAVA_HOME` pointing at a JDK 21
  (`$HOME/.local/sdk/jdk-21` on beast). Both are local, not committed.
- WSL native player toolchain missing (`pkg-config` / `libmpv-dev` / gstreamer). Checks must use:
  `-x :composeMediaPlayer:buildNativeLinux -x :composeMediaPlayer:cleanLegacyLinuxNativeResource -x :composeApp:buildLinuxPlayerBridge`
  (now baked into `scripts/upstream-sync.sh` and documented in AGENTS.md).
- Check results (recorded on the sync branch head `e69090674`, still the baseline for Dev):
  - `:composeApp:desktopTest` with native exclusions: **FAIL** — 2029 tests, 2 failed, 11 skipped.
    - `NativePlayerControllerTeardownTest.sourceGapsDisposeSuccessfulSupersededCreatesBeforeReplacementPublication`: `AssertionError: Expected value to be true.` at `NativePlayerControllerTeardownTest.kt:118`
    - `SettingsNavigationTest.bufferOptionsFollowThePlatformAndPlaybackEngine`: `AssertionError: expected:<true> but was:<false>` at `SettingsNavigationTest.kt:39`
  - `:composeApp:compileKotlinDesktop` with native exclusions: **PASS**
  - `:composeApp:compileAndroidMain` with native exclusions: **FAIL** — unresolved `PointerButton` /
    `onPointerEvent` / `button` in commonMain (`MainAppContent.kt`, `HomePosterHoverPreview.kt`,
    `PlayerScreenContent.kt`). Upstream commonMain desktop pointer APIs do not resolve on the Android
    shared compile target. Verbatim compiler output (Gradle daemon log of the failing run; all line
    numbers re-checked against `Dev` `178459874`):
    ```text
    e: file:///home/techmuzz/nuvio/nuvio-desktop/composeApp/src/commonMain/kotlin/com/nuvio/app/MainAppContent.kt:205:42 Unresolved reference 'PointerButton'.
    e: file:///home/techmuzz/nuvio/nuvio-desktop/composeApp/src/commonMain/kotlin/com/nuvio/app/MainAppContent.kt:1365:51 Unresolved reference 'button'.
    e: file:///home/techmuzz/nuvio/nuvio-desktop/composeApp/src/commonMain/kotlin/com/nuvio/app/MainAppContent.kt:1365:61 Unresolved reference 'PointerButton'.
    e: file:///home/techmuzz/nuvio/nuvio-desktop/composeApp/src/commonMain/kotlin/com/nuvio/app/features/home/components/HomePosterHoverPreview.kt:59:42 Unresolved reference 'onPointerEvent'.
    e: file:///home/techmuzz/nuvio/nuvio-desktop/composeApp/src/commonMain/kotlin/com/nuvio/app/features/home/components/HomePosterHoverPreview.kt:332:30 Unresolved reference 'onPointerEvent'.
    e: file:///home/techmuzz/nuvio/nuvio-desktop/composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerScreenContent.kt:19:42 Unresolved reference 'PointerButton'.
    e: file:///home/techmuzz/nuvio/nuvio-desktop/composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerScreenContent.kt:164:39 Unresolved reference 'button'.
    e: file:///home/techmuzz/nuvio/nuvio-desktop/composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerScreenContent.kt:164:49 Unresolved reference 'PointerButton'.
    e: file:///home/techmuzz/nuvio/nuvio-desktop/composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerScreenContent.kt:166:46 Unresolved reference 'button'.
    e: file:///home/techmuzz/nuvio/nuvio-desktop/composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerScreenContent.kt:166:56 Unresolved reference 'PointerButton'.
    ```
- [PR #10](https://github.com/kheniparth/NuvioDesktop/pull/10) and
  [PR #11](https://github.com/kheniparth/NuvioDesktop/pull/11) are `MERGED`.

## Next steps
1. Finish [TEC-97](/TEC/issues/TEC-97) (copy AppFileCache + Live TV data files into the TEC-11 worktree).
2. On [TEC-11](/TEC/issues/TEC-11): Stalker gate + 401 refresh, then shared tests; one PR, three commits.
3. Later: settings/player wiring and UI issues; macOS builds need the Mac or a macOS CI runner.
4. Future upstream syncs: `bash scripts/upstream-sync.sh` from a clean `Dev`, then open the sync PR.

## How to build and test (Desktop)
- Checks (one Gradle build at a time on beast); see AGENTS.md for the native-exclusion array.
- Upstream: `bash scripts/upstream-sync.sh` from a clean `Dev` (creates `sync/upstream-YYYYMMDD`, runs
  checks, does not push).
- Parth checks Windows (and Mac for macOS) by hand.

## Decisions (do not reopen)
- Never merge Nuvio Enhanced into this fork; copy Live TV files in.
- Never change native player library versions (pin `mpv-android-lib`; 0.2.1 broke Enhanced's player).
- Never touch `NuvioMedia/*` on GitHub. PRs only against `kheniparth/NuvioDesktop` base `Dev`.
- WSL only for Gradle. One Gradle build at a time on beast.
- `.factory/` is out of scope for this repo.
- On WSL, skip Linux native player tasks (listed above) rather than installing apt packages mid-issue.

## Gotchas
- Base branch is `Dev` (capital D), unlike NuvioTV's `dev`.
- `local.properties` is required for Gradle (gitignored). Without it, `:composeApp:generateRuntimeConfigs` fails.
- `JAVA_HOME` must be a JDK 21 (`export JAVA_HOME=$HOME/.local/sdk/jdk-21` on beast); the wrapper exits
  with `JAVA_HOME is not set` otherwise.
- Every PR body must fill the sections in `.github/PULL_REQUEST_TEMPLATE.md`; the `validate_pr_body` CI
  job fails otherwise. The factory handoff block alone is not enough — keep both in the body.
- Public Paperclip/GitHub broker URL may SSO-gate; use `PAPERCLIP_GITHUB_BROKER_URL=http://127.0.0.1:3100`
  for `gh` when needed. Paperclip API similarly: `http://127.0.0.1:3100`.
- GitHub Issues are disabled on this fork; track work in Paperclip.
- Managed git wrappers ignore `GIT_AUTHOR_*`; `scripts/upstream-sync.sh` uses `git -c user.*` for merges.
- Agent runs need a dedicated (agent-scoped) managed GitHub grant for SD3 company-default wakes; personal board grants alone are ignored for those runs.

## Secrets (never commit)
`local.properties`, keystores and passwords, any `.env` used for Live TV provider bootstrap
(`/mnt/d/stalker/.env` when that lands).
