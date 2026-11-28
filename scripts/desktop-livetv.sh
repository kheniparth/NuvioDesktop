#!/usr/bin/env bash
# Proxy M3U + Xtream sources from the stalker .env into nuvio_live_tv.properties (app closed).
set -euo pipefail

usage() {
  cat <<'EOF' >&2
Usage: NUVIO_DESKTOP_CONFIG_DIR=... scripts/desktop-livetv.sh [--dry-run]

Writes M3U / Xtream proxy Live TV sources for NUVIO_PROFILE_ID (default 1).
Refuses with exit 3 while the Nuvio desktop app is running. Never prints secrets.
EOF
  exit 2
}

root="$(cd "$(dirname "$0")/.." && pwd)"
dry_run=0
[[ $# -eq 0 ]] || [[ $# -eq 1 && "$1" == "--dry-run" ]] || usage
[[ $# -eq 1 ]] && dry_run=1

profile="${NUVIO_PROFILE_ID:-1}"
[[ "$profile" =~ ^[1-9][0-9]*$ ]] || {
  echo "NUVIO_PROFILE_ID must be a positive integer" >&2
  exit 2
}

resolve_config_dir() {
  if [[ -n "${NUVIO_DESKTOP_CONFIG_DIR:-}" ]]; then
    printf '%s' "$NUVIO_DESKTOP_CONFIG_DIR"
    return
  fi
  if grep -qi microsoft /proc/version 2>/dev/null; then
    local appdata
    appdata="$(wslpath "$(cmd.exe /c 'echo %APPDATA%' | tr -d '\r')" 2>/dev/null | sed 's:/*$::')"
    printf '%s/Nuvio' "$appdata"
    return
  fi
  case "$(uname -s)" in
    Darwin) printf '%s/Library/Application Support/Nuvio' "$HOME" ;;
    *) printf '%s/nuvio' "${XDG_CONFIG_HOME:-$HOME/.config}" ;;
  esac
}

config_dir="$(resolve_config_dir)"
props_file="$config_dir/nuvio_live_tv.properties"

app_running() {
  if [[ -n "${NUVIO_DESKTOP_SIMULATE_RUNNING:-}" ]]; then
    return 0
  fi
  if [[ -n "${NUVIO_DESKTOP_RUNNING_CHECK_PATTERN:-}" ]]; then
    pgrep -f "$NUVIO_DESKTOP_RUNNING_CHECK_PATTERN" >/dev/null 2>&1
    return
  fi
  if grep -qi microsoft /proc/version 2>/dev/null; then
    # Avoid grep -q: early close SIGPIPEs tasklist and pipefail yields 141 ("not running").
    tasklist.exe 2>/dev/null | grep -i '^Nuvio\.exe' >/dev/null
    return
  fi
  pgrep -f 'com.nuvio.app.MainKt' >/dev/null 2>&1
}

if app_running; then
  echo "Close Nuvio first (desktop app is running)." >&2
  exit 3
fi

stalker_env="${STALKER_DIR:-/mnt/d/stalker}/.env"
enhanced_env="${NUVIO_ENHANCED_ENV:-$HOME/nuvio/nuvio-enhanced/.env}"

sv() { grep -m1 "^$1=" "$2" 2>/dev/null | cut -d= -f2- | tr -d '\r' || true; }

m3u_url="" xtream_url="" xtream_user="" xtream_password="" mask_token=""

if [[ -f "$stalker_env" ]]; then
  public="$(sv ADDON_PUBLIC_URL "$stalker_env")"
  public="${public%/}"
  token="$(sv ADDON_TOKEN "$stalker_env")"
  if [[ -z "$public" || -z "$token" ]]; then
    echo "$stalker_env is missing ADDON_PUBLIC_URL or ADDON_TOKEN" >&2
    exit 1
  fi
  m3u_url="$public/$token/playlist.m3u"
  xtream_url="$public/$token"
  xtream_user="test"
  xtream_password="test"
  mask_token="$token"
elif [[ -f "$enhanced_env" ]]; then
  m3u_url="$(sv NUVIO_M3U_URL "$enhanced_env")"
  xtream_url="$(sv NUVIO_XTREAM_URL "$enhanced_env")"
  xtream_user="$(sv NUVIO_XTREAM_USERNAME "$enhanced_env")"
  xtream_password="$(sv NUVIO_XTREAM_PASSWORD "$enhanced_env")"
  xtream_user="${xtream_user:-test}"
  xtream_password="${xtream_password:-test}"
  if [[ -z "$m3u_url" && -z "$xtream_url" ]]; then
    echo "$enhanced_env is missing NUVIO_M3U_URL / NUVIO_XTREAM_URL" >&2
    exit 1
  fi
else
  echo "No stalker .env ($stalker_env) or $enhanced_env" >&2
  exit 1
fi

py_args=(
  --config-file "$props_file"
  --profile-id "$profile"
  --m3u-url "$m3u_url"
  --xtream-url "$xtream_url"
  --xtream-user "$xtream_user"
  --xtream-password "$xtream_password"
  --mask-token "$mask_token"
)
(( dry_run )) && py_args+=(--dry-run)

if (( dry_run )); then
  python3 -I "$root/scripts/desktop_livetv.py" "${py_args[@]}"
  echo "# dry-run: no file written ($(date -u +%Y-%m-%dT%H:%M:%SZ))"
  exit 0
fi

rc=0
python3 -I "$root/scripts/desktop_livetv.py" "${py_args[@]}" || rc=$?

case "$rc" in
  0)
    echo "Live TV sources written for profile $profile in $props_file."
    ;;
  3)
    echo "Live TV sources in $props_file are already up to date."
    ;;
  1)
    echo "Could not update $props_file" >&2
    exit 1
    ;;
  *)
    echo "Unexpected error from desktop_livetv.py (exit $rc)" >&2
    exit 1
    ;;
esac