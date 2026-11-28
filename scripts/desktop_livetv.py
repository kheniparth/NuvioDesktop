"""Write stalker M3U / Xtream proxy Live TV sources into nuvio_live_tv.properties.

Proxy-only (TEC-426): stalker portal/MAC are never changed. Merge rules mirror
com.nuvio.tv LiveTvImport.kt and desktop LiveTvRepository playlist encoding.

Run: python3 -I scripts/desktop_livetv.py --self-test
"""
from __future__ import annotations

import os
import re
import sys
from datetime import datetime, timezone
FS, RS = "\x1f", "\x1e"
PLAYLIST_ID = "stalker-m3u"
PROPS_HEADER = "#Nuvio desktop preferences\n"


class PropertiesParseError(Exception):
    pass


def props_unescape(s: str) -> str:
    out: list[str] = []
    i = 0
    while i < len(s):
        c = s[i]
        if c == "\\":
            i += 1
            if i >= len(s):
                raise PropertiesParseError("trailing backslash")
            n = s[i]
            if n == "u":
                if i + 4 >= len(s):
                    raise PropertiesParseError("short \\u escape")
                out.append(chr(int(s[i + 1 : i + 5], 16)))
                i += 5
                continue
            out.append({"t": "\t", "n": "\n", "r": "\r", "f": "\f"}.get(n, n))
            i += 1
            continue
        out.append(c)
        i += 1
    return "".join(out)


def props_escape(s: str, *, key: bool = False) -> str:
    s = (
        s.replace("\\", "\\\\")
        .replace("\t", "\\t")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\f", "\\f")
    )
    s = re.sub(r"([=:#!])", r"\\\1", s)
    if key:
        s = s.replace(" ", "\\ ")
    s = re.sub(r"^ ", r"\\ ", s)
    return re.sub(r"[^\x20-\x7e]", lambda m: f"\\u{ord(m.group()):04X}", s)


def props_load(text: str) -> dict[str, str]:
    out: dict[str, str] = {}
    for raw in text.splitlines():
        line = raw.strip()
        if not line or line[0] in "#!":
            continue
        # Logical lines only — DesktopStorage.store does not write continuations.
        sep = -1
        for i, ch in enumerate(raw):
            if ch == "=" and (i == 0 or raw[i - 1] != "\\"):
                sep = i
                break
        if sep < 0:
            raise PropertiesParseError(f"unparseable line: {raw!r}")
        key = props_unescape(raw[:sep].strip())
        val = props_unescape(raw[sep + 1 :].lstrip())
        out[key] = val
    return out


def props_store(d: dict[str, str]) -> str:
    when = datetime.now(timezone.utc).strftime("%a %b %d %H:%M:%S UTC %Y")
    lines = [f"#{when}", PROPS_HEADER.strip()]
    for k in sorted(d.keys()):
        lines.append(f"{props_escape(k, key=True)}={props_escape(d[k])}")
    return "\n".join(lines) + "\n"


def esc_field(s: str) -> str:
    return s.replace("%", "%25").replace(RS, "%1E").replace(FS, "%1F")


def decode_playlist_enabled(value: str | None) -> bool:
    return value is None or value.lower() != "false"


def merge_proxy_sources(
    store: dict[str, str],
    profile_id: int,
    *,
    m3u_url: str | None,
    xtream_url: str | None,
    xtream_user: str | None,
    xtream_password: str | None,
) -> bool:
    p = str(profile_id)
    changed = False

    def want(key: str, value: str) -> None:
        nonlocal changed
        if store.get(key) != value:
            store[key] = value
            changed = True

    if xtream_url:
        old = (store.get(f"xtream_settings_{p}") or "").split(FS)
        enabled = old[3] if len(old) > 3 else "true"
        user = xtream_user if xtream_user else (old[1] if len(old) > 1 and old[1] else "test")
        password = xtream_password if xtream_password else (old[2] if len(old) > 2 and old[2] else "test")
        want(f"xtream_settings_{p}", FS.join([xtream_url, user, password, enabled]))

    if m3u_url:
        blob_key = f"playlists_blob_{p}"
        records = [r for r in (store.get(blob_key) or "").split(RS) if r]
        ours = next((r for r in records if r.split(FS, 1)[0] == PLAYLIST_ID), None)
        name, ptype, enabled = "Stalker M3U", "Url", "true"
        if ours:
            parts = ours.split(FS)
            if len(parts) > 1 and parts[1]:
                name = parts[1]
            if len(parts) > 2 and parts[2]:
                ptype = parts[2]
            if len(parts) > 4:
                enabled = parts[4]
        record = FS.join(esc_field(f) for f in [PLAYLIST_ID, name, ptype, m3u_url, enabled])
        if ours is None:
            merged = records + [record]
        else:
            merged = [record if r is ours else r for r in records]
        want(blob_key, RS.join(merged))

    return changed


def mask_value(value: str, *, token: str | None, mac_substrings: list[str]) -> str:
    out = value
    if token:
        out = out.replace(token, "***")
    for mac in mac_substrings:
        if mac:
            out = re.sub(re.escape(mac), "***", out, flags=re.I)
    return out


def mask_properties_text(text: str, *, token: str | None, mac_substrings: list[str]) -> str:
    if not token and not mac_substrings:
        return text
    try:
        d = props_load(text)
    except PropertiesParseError:
        return text
    masked = {
        k: mask_value(v, token=token, mac_substrings=mac_substrings)
        for k, v in d.items()
    }
    return props_store(masked)


def apply_file(
    path: str,
    profile_id: int,
    *,
    m3u_url: str | None,
    xtream_url: str | None,
    xtream_user: str | None,
    xtream_password: str | None,
    dry_run: bool,
) -> tuple[str, bool]:
    existing = ""
    if os.path.isfile(path):
        with open(path, encoding="utf-8") as f:
            existing = f.read()
    try:
        d = props_load(existing) if existing.strip() else {}
    except PropertiesParseError:
        raise
    changed = merge_proxy_sources(
        d,
        profile_id,
        m3u_url=m3u_url,
        xtream_url=xtream_url,
        xtream_user=xtream_user,
        xtream_password=xtream_password,
    )
    new_text = props_store(d)
    if not changed:
        return new_text, False
    if dry_run:
        return new_text, True
    tmp = f"{path}.tmp"
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    with open(tmp, "w", encoding="utf-8") as f:
        f.write(new_text)
    os.replace(tmp, path)
    return new_text, True


def self_test() -> None:
    env = {
        "m3u": "https://x/t/playlist.m3u?a=1&b=2",
        "xtream_url": "https://x/t",
        "xtream_user": "nu",
        "xtream_password": "np",
        "token": "SECRETTOKEN",
        "mac": "00:1a:79:aa:bb:cc",
    }
    other = FS.join(["p1", "Mine", "Url", "https://other/list.m3u", "false"])
    stalker_old = FS.join(["https://portal/old", env["mac"], "su", "sp", "false"])
    start = (
        "#Nuvio desktop preferences\n"
        f"channel_view_mode_1=Grid\n"
        f"playlists_blob_1={props_escape(other + RS + FS.join([PLAYLIST_ID, 'Old', 'Url', 'https://x/old', 'false']))}\n"
        f"stalker_settings_1={props_escape(stalker_old)}\n"
        f"xtream_settings_1={props_escape(FS.join(['https://x/old', 'ou', 'op', 'false']))}\n"
        f"weird\\:key=a\\=b\n"
        f"unicode_1=caf\\u00e9\n"
    )
    d = props_load(start)
    assert d["weird:key"] == "a=b"
    assert d["unicode_1"] == "café"
    roundtrip = props_store(d)
    assert props_load(roundtrip) == d

    changed = merge_proxy_sources(
        d,
        1,
        m3u_url=env["m3u"],
        xtream_url=env["xtream_url"],
        xtream_user=env["xtream_user"],
        xtream_password=env["xtream_password"],
    )
    assert changed
    assert d["stalker_settings_1"] == stalker_old
    assert d["xtream_settings_1"].split(FS)[3] == "false"
    assert d["xtream_settings_1"].split(FS)[0] == env["xtream_url"]
    recs = d["playlists_blob_1"].split(RS)
    assert recs[0] == other
    stalker_rec = next(r for r in recs if r.split(FS)[0] == PLAYLIST_ID)
    assert stalker_rec.split(FS)[3] == env["m3u"]
    assert stalker_rec.split(FS)[4] == "false"

    d2 = props_load(props_store(d))
    assert not merge_proxy_sources(
        d2,
        1,
        m3u_url=env["m3u"],
        xtream_url=env["xtream_url"],
        xtream_user=env["xtream_user"],
        xtream_password=env["xtream_password"],
    )

    bad_path = os.path.join(os.environ.get("TMPDIR", "/tmp"), "desktop_livetv_bad.props")
    with open(bad_path, "w", encoding="utf-8") as f:
        f.write("good=1\nno-equals-sign\n")
    try:
        with open(bad_path, encoding="utf-8") as f:
            try:
                props_load(f.read())
            except PropertiesParseError:
                pass
            else:
                raise AssertionError("expected parse error")
        before = os.path.getmtime(bad_path)
        try:
            apply_file(
                bad_path,
                1,
                m3u_url="https://x/t",
                xtream_url=None,
                xtream_user=None,
                xtream_password=None,
                dry_run=False,
            )
        except PropertiesParseError:
            pass
        else:
            raise AssertionError("apply_file should refuse bad file")
        assert os.path.getmtime(bad_path) == before
    finally:
        os.remove(bad_path)

    masked = mask_properties_text(
        props_store(d),
        token=env["token"],
        mac_substrings=[env["mac"]],
    )
    assert "SECRETTOKEN" not in masked and env["mac"].lower() not in masked.lower()
    assert "\\u001F" in masked or FS not in masked

    print("self-test ok")


def main(argv: list[str]) -> int:
    if argv == ["--self-test"]:
        self_test()
        return 0

    import argparse

    parser = argparse.ArgumentParser()
    parser.add_argument("--config-file", required=True)
    parser.add_argument("--profile-id", type=int, default=1)
    parser.add_argument("--m3u-url", default="")
    parser.add_argument("--xtream-url", default="")
    parser.add_argument("--xtream-user", default="")
    parser.add_argument("--xtream-password", default="")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--mask-token", default="")
    parser.add_argument("--mask-mac", action="append", default=[])
    args = parser.parse_args(argv)

    m3u = args.m3u_url or None
    xtream = args.xtream_url or None
    user = args.xtream_user or None
    password = args.xtream_password or None

    if os.path.isfile(args.config_file):
        with open(args.config_file, encoding="utf-8") as f:
            text = f.read()
        try:
            props_load(text)
        except PropertiesParseError:
            print("error: could not parse properties file", file=sys.stderr)
            return 1

    try:
        new_text, changed = apply_file(
            args.config_file,
            args.profile_id,
            m3u_url=m3u,
            xtream_url=xtream,
            xtream_user=user,
            xtream_password=password,
            dry_run=args.dry_run,
        )
    except PropertiesParseError:
        print("error: could not parse properties file", file=sys.stderr)
        return 1

    if args.dry_run:
        out = mask_properties_text(
            new_text,
            token=args.mask_token or None,
            mac_substrings=args.mask_mac,
        )
        sys.stdout.write(out)
        return 0

    return 0 if changed else 3


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))