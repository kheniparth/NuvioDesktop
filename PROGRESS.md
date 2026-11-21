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

## Current state (2026-11-21)
- Branch `sync/upstream-20261120`: docs + sync script + merge of `upstream/Dev` through `bcb930c2d`.
- Dirty-tree guard verified earlier (exit 1, no change).
- Gradle needs local gitignored `local.properties` (`sdk.dir=...`); present on this machine, not committed.
- WSL native player toolchain missing (`pkg-config` / `libmpv-dev` / gstreamer). Checks must use:
  `-x :composeMediaPlayer:buildNativeLinux -x :composeMediaPlayer:cleanLegacyLinuxNativeResource -x :composeApp:buildLinuxPlayerBridge`
  (now baked into `scripts/upstream-sync.sh` and documented in AGENTS.md).
- Check results on `sync/upstream-20261120` (2026-11-20):
  - `:composeApp:desktopTest` with native exclusions: **FAIL** — 2029 tests, 2 failed, 11 skipped.
    - `NativePlayerControllerTeardownTest.sourceGapsDisposeSuccessfulSupersededCreatesBeforeReplacementPublication`: `AssertionError: Expected value to be true.` at `NativePlayerControllerTeardownTest.kt:118`
    - `SettingsNavigationTest.bufferOptionsFollowThePlatformAndPlaybackEngine`: `AssertionError: expected:<true> but was:<false>` at `SettingsNavigationTest.kt:39`
  - `:composeApp:compileKotlinDesktop` with native exclusions: **PASS**
  - `:composeApp:compileAndroidMain` with native exclusions: **FAIL** — unresolved `PointerButton` / `onPointerEvent` / `button` in commonMain (`MainAppContent.kt`, `HomePosterHoverPreview.kt`, `PlayerScreenContent.kt`). Upstream commonMain desktop pointer APIs do not resolve on the Android shared compile target.
- Pushed and PR open: https://github.com/kheniparth/NuvioDesktop/pull/10 (base `Dev`). Managed GitHub identity resolved via [TEC-53](/TEC/issues/TEC-53).
- No Live TV code in this checkout yet.

## Next steps
1. Finish [TEC-10](/TEC/issues/TEC-10): tester pass on PR #10, then senior review + merge (not the author).
2. Port Enhanced `features/livetv/` (and settings/player wiring) by copying files — never merge Enhanced.
3. Reuse from `windows-desktop` @ `7a790e185`: desktop `LiveTvStorage` actual, `--windows` bootstrap,
   packaging helpers. Confirm libmpv header support for channel URLs.
4. macOS builds need the Mac or a macOS CI runner (owner checks by hand).

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
- Public Paperclip/GitHub broker URL may SSO-gate; use `PAPERCLIP_GITHUB_BROKER_URL=http://127.0.0.1:3100`
  for `gh` when needed. Paperclip API similarly: `http://127.0.0.1:3100`.
- GitHub Issues are disabled on this fork; track work in Paperclip.
- Managed git wrappers ignore `GIT_AUTHOR_*`; `scripts/upstream-sync.sh` uses `git -c user.*` for merges.
- Agent runs need a dedicated (agent-scoped) managed GitHub grant for SD3 company-default wakes; personal board grants alone are ignored for those runs.

## Secrets (never commit)
`local.properties`, keystores and passwords, any `.env` used for Live TV provider bootstrap
(`/mnt/d/stalker/.env` when that lands).
