#!/usr/bin/env python3
"""Apply the Vantage setting defaults in vantage/defaults.tsv to Settings.java.

Usage: apply-defaults.py [--check]

Rewrites `("<key>", <upstream default>` to `("<key>", <Vantage default>` for every
row. Each row must match exactly once; any miss (upstream renamed the key, changed
its default, or reformatted the line) exits non-zero and lists every failing row.
--check validates without writing.
"""
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
TABLE = ROOT / "vantage" / "defaults.tsv"
TARGET = ROOT / "extensions/shared/src/main/java/app/morphe/extension/youtube/settings/Settings.java"


def main() -> int:
    check_only = "--check" in sys.argv[1:]
    source = TARGET.read_text(encoding="utf-8")
    errors = []
    applied = 0
    seen = set()
    for number, line in enumerate(TABLE.read_text(encoding="utf-8").splitlines(), 1):
        if not line.strip() or line.startswith("#"):
            continue
        parts = line.split("\t")
        if len(parts) != 3 or not all(p.strip() for p in parts):
            errors.append(f"defaults.tsv:{number}: expected 3 TAB separated columns")
            continue
        key, old, new = (p.strip() for p in parts)
        if key in seen:
            errors.append(f"defaults.tsv:{number}: duplicate key {key}")
            continue
        seen.add(key)
        needle = f'("{key}", {old}'
        count = source.count(needle)
        if count != 1:
            hint = "key not found" if f'"{key}"' not in source else "key found, default or formatting differs"
            errors.append(f"defaults.tsv:{number}: {needle!r} matched {count} times, expected 1 ({hint})")
            continue
        source = source.replace(needle, f'("{key}", {new}')
        applied += 1
        print(f"  {key}: {old} -> {new}")
    if errors:
        print("Vantage defaults FAILED - update vantage/defaults.tsv to match upstream:", file=sys.stderr)
        for error in errors:
            print(f"  {error}", file=sys.stderr)
        return 1
    if not check_only:
        TARGET.write_text(source, encoding="utf-8")
    print(f"Vantage defaults: {applied} {'validated' if check_only else 'applied'}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
