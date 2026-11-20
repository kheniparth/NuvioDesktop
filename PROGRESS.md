# NuvioDesktop progress

Last updated: 2026-11-20. Single source of truth for picking Desktop fork work back up with any agent.
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
- 2026-11-20: [TEC-10](/TEC/issues/TEC-10) — adding AGENTS.md, PROGRESS.md, `scripts/upstream-sync.sh`, and
  first upstream sync PR.

## Current state (2026-11-20)
- Fork tracks upstream; no Live TV code in this checkout yet.
- Agent docs and upstream sync script are being added on `paperclip/TEC-10-fork-setup`.
- Shared WSL checkout may also be used by [TEC-9](/TEC/issues/TEC-9) (Live TV epic); commit small, no rebase/reset.

## Next steps
1. Finish [TEC-10](/TEC/issues/TEC-10): merge the sync PR after tester pass + senior review.
2. Port Enhanced `features/livetv/` (and settings/player wiring) by copying files — never merge Enhanced.
3. Reuse from `windows-desktop` @ `7a790e185`: desktop `LiveTvStorage` actual, `--windows` bootstrap,
   packaging helpers. Confirm libmpv header support for channel URLs.
4. macOS builds need the Mac or a macOS CI runner (owner checks by hand).

## How to build and test (Desktop)
- Checks (one Gradle build at a time on beast):
  - `./gradlew :composeApp:desktopTest`
  - `./gradlew :composeApp:compileKotlinDesktop`
  - `./gradlew :composeApp:compileAndroidMain` (shared common code; there is no `compileDebugKotlinAndroid` here)
- Upstream: `bash scripts/upstream-sync.sh` from a clean `Dev` (creates `sync/upstream-YYYYMMDD`, runs
  checks, does not push).
- Parth checks Windows (and Mac for macOS) by hand.

## Decisions (do not reopen)
- Never merge Nuvio Enhanced into this fork; copy Live TV files in.
- Never change native player library versions (pin `mpv-android-lib`; 0.2.1 broke Enhanced's player).
- Never touch `NuvioMedia/*` on GitHub. PRs only against `kheniparth/NuvioDesktop` base `Dev`.
- WSL only for Gradle. One Gradle build at a time on beast.
- `.factory/` is out of scope for this repo.

## Gotchas
- Base branch is `Dev` (capital D), unlike NuvioTV's `dev`.
- Public Paperclip/GitHub broker URL may SSO-gate; use `PAPERCLIP_GITHUB_BROKER_URL=http://127.0.0.1:3100`
  for `gh` when needed.
- GitHub Issues are disabled on this fork; track work in Paperclip.

## Secrets (never commit)
`local.properties`, keystores and passwords, any `.env` used for Live TV provider bootstrap
(`/mnt/d/stalker/.env` when that lands).
