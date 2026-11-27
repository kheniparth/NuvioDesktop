# NuvioDesktop progress

Last updated: 2026-11-27. Single source of truth for picking Desktop fork work back up with any agent.
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
- 2026-11-26: [TEC-11](/TEC/issues/TEC-11) — Live TV data layer landed in three code commits:
  `32ac874d2` (AppFileCache + 8 data files + actuals), `d98d5ef5a` (Stalker gate + 401 refresh),
  `8043b4215` (16 shared tests). Sub-tasks [TEC-97](/TEC/issues/TEC-97),
  [TEC-99](/TEC/issues/TEC-99) and [TEC-267](/TEC/issues/TEC-267) are done; Tester3 PASS and
  [PR #12](https://github.com/kheniparth/NuvioDesktop/pull/12) merged into `Dev` as `fe34af309`.
- 2026-11-27: [TEC-369](/TEC/issues/TEC-369) — copied all `live_tv_*` localization strings from Enhanced
  (`windows-desktop` commit `7a790e185`) to this fork across 5 strings files and all supported language
  locales. Strings cover UI elements: channel groups, search, filters, guide sections, sports categories.
  Branch `paperclip/TEC-369-copy-live-tv-strings` ready for Tester3 verification; PR body prepared.

## Current state (2026-11-27)
- `Dev` is `fe34af309` (PR #12 merge on top of PR #11 `baa29326c`): `AGENTS.md`,
  `PROGRESS.md`, `scripts/upstream-sync.sh`, the upstream `Dev` merge through `bcb930c2d`, and the
  Live TV data layer.
- `paperclip/TEC-369-copy-live-tv-strings` is branch `00f47bfd9` with all `live_tv_*` localization
  strings copied from Enhanced. Ready for Tester3: PR `TEC-369: Copy Live TV strings from Enhanced`
  targets `Dev` base. 70 commits ahead of `Dev` (includes all prior TEC-11 work).
- `sync/upstream-20261120` stays at `e69090674` (merged; not rebased or force-pushed).
- Live TV data layer on `Dev` from [TEC-11](/TEC/issues/TEC-11), no UI and nothing wired into a screen:
  M3U/Xtream/Stalker loaders in `features/livetv/`, EPG parsing incl. gzip, `AppFileCache` +
  `LiveTvStorage` actuals for desktop/Android/iOS, and `LiveTvStalkerGate` with a once-only 401
  re-handshake. The worktree `/home/techmuzz/nuvio/scratch/nuvio-desktop-tec-11` is no longer needed.
- Live TV localization strings on `paperclip/TEC-369-copy-live-tv-strings` branch (not yet merged):
  all `live_tv_*` and `settings_integrations_live_tv_*` keys across 5 strings files and all language
  locales, ready for UI wiring ([TEC-370](/TEC/issues/TEC-370)).
- Live TV storage keys: desktop and Android use store/preferences name `nuvio_live_tv`; iOS uses
  `NSUserDefaults`. Keys (`playlist_url`, `playlists_blob`, `stalker_settings`, `xtream_settings`,
  `epg_sources_blob`, …) are all profile-scoped through `ProfileScopedKey`.
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
- TEC-11 results (Tester3 on head `fbd55a4bc`, same native exclusions): `:composeApp:desktopTest` —
  2045 tests, 2 failed, 11 skipped; the same two failures as the `Dev` baseline, and all 16 new Live TV
  tests pass. `:composeApp:compileKotlinDesktop` — **PASS**. `:composeApp:compileAndroidMain` —
  **BUILD FAILED** only on the 10 documented `PointerButton` / `onPointerEvent` / `button` errors above;
  the Live TV files add no Android-only error. `validate_pr_body` CI — green.
  Known gap: acceptance line 5 (`LiveTvStorage` desktop write/read) has no dedicated test; it was
  verified by inspection only (recorded in [TEC-361](/TEC/issues/TEC-361)).
- [PR #10](https://github.com/kheniparth/NuvioDesktop/pull/10),
  [PR #11](https://github.com/kheniparth/NuvioDesktop/pull/11) and
  [PR #12](https://github.com/kheniparth/NuvioDesktop/pull/12) are `MERGED`.

## Next steps
1. Merge [TEC-369](/TEC/issues/TEC-369) PR (Live TV strings) to `Dev` after Tester3 verification.
2. Wire the Live TV UI: [TEC-370](/TEC/issues/TEC-370) (settings/player wiring) — consume the data
   layer and strings to build settings screens and player UI. No screen calls the data layer yet.
3. macOS builds: test on Mac or a macOS CI runner; Parth checks by hand.
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
