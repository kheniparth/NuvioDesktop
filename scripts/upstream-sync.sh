#!/usr/bin/env bash
# Merge upstream/Dev onto sync/upstream-YYYYMMDD (local date), print the new
# upstream commits one per line, then run the checks. Start from a clean Dev.
# Does not push.
#   bash scripts/upstream-sync.sh
# shortcut: NUVIO_SYNC_CHECKS replaces Gradle (for dry tests); leave it unset for a real sync.
set -euo pipefail

root="$(git rev-parse --show-toplevel)"
cd "$root"

[[ "$(git rev-parse --abbrev-ref HEAD)" == "Dev" ]] || { echo "checkout Dev first" >&2; exit 1; }
if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "working tree is dirty" >&2
  exit 1
fi
git remote get-url upstream >/dev/null 2>&1 || { echo "no upstream remote" >&2; exit 1; }

branch="sync/upstream-$(date +%Y%m%d)"
if git show-ref --verify --quiet "refs/heads/$branch"; then
  echo "branch $branch already exists" >&2
  exit 1
fi

old="$(git rev-parse HEAD)"
git fetch upstream
git switch -c "$branch"
git merge --no-edit upstream/Dev

echo "upstream commits:"
lines="$(git --no-pager log --format='%h %s' "$old..upstream/Dev")"
if [[ -z "$lines" ]]; then
  echo "(none)"
else
  printf '%s\n' "$lines"
fi

run_checks() {
  # One Gradle invocation at a time on beast (RAM). Desktop tests, desktop
  # compile, then Android shared compile so commonMain still builds.
  echo "==> :composeApp:desktopTest"
  ./gradlew :composeApp:desktopTest
  echo "PASS :composeApp:desktopTest"
  echo "==> :composeApp:compileKotlinDesktop"
  ./gradlew :composeApp:compileKotlinDesktop
  echo "PASS :composeApp:compileKotlinDesktop"
  echo "==> :composeApp:compileAndroidMain"
  ./gradlew :composeApp:compileAndroidMain
  echo "PASS :composeApp:compileAndroidMain"
}

if [[ -n "${NUVIO_SYNC_CHECKS:-}" ]]; then
  bash -c "$NUVIO_SYNC_CHECKS"
else
  run_checks
fi
