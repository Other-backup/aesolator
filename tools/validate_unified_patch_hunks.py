#!/usr/bin/env python3
from __future__ import annotations

import re
import sys
from pathlib import Path


HUNK_RE = re.compile(r"^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@")


def iter_patch_files(args: list[str]) -> list[Path]:
    patch_files: list[Path] = []
    for raw in args:
        path = Path(raw)
        if path.is_dir():
            patch_files.extend(sorted(path.glob("*.patch")))
        else:
            patch_files.append(path)
    return patch_files


def validate_patch(path: Path) -> list[str]:
    issues: list[str] = []
    lines = path.read_text(encoding="utf-8").splitlines()
    current_file = "<unknown>"
    idx = 0

    while idx < len(lines):
        line = lines[idx]
        if line.startswith("diff --git "):
            current_file = line.split(" b/", 1)[1] if " b/" in line else "<unknown>"
            idx += 1
            continue

        match = HUNK_RE.match(line)
        if not match:
            idx += 1
            continue

        old_expected = int(match.group(2) or "1")
        new_expected = int(match.group(4) or "1")
        old_seen = 0
        new_seen = 0
        idx += 1

        while idx < len(lines):
            line = lines[idx]
            if line.startswith("diff --git ") or line.startswith("@@ "):
                break
            if line == r"\ No newline at end of file":
                idx += 1
                continue
            if line.startswith("+") and not line.startswith("+++ "):
                new_seen += 1
            elif line.startswith("-") and not line.startswith("--- "):
                old_seen += 1
            else:
                old_seen += 1
                new_seen += 1
            idx += 1

        if old_seen != old_expected or new_seen != new_expected:
            issues.append(
                f"{path}:{current_file}: expected -{old_expected}/+{new_expected}, "
                f"saw -{old_seen}/+{new_seen}"
            )

    return issues


def main(argv: list[str]) -> int:
    if len(argv) < 2:
        print("usage: validate_unified_patch_hunks.py <patch-or-dir> [...]", file=sys.stderr)
        return 2

    patch_files = iter_patch_files(argv[1:])
    if not patch_files:
        print("no patch files found", file=sys.stderr)
        return 2

    issues: list[str] = []
    for patch in patch_files:
        issues.extend(validate_patch(patch))

    if issues:
        print("unified diff hunk count mismatch detected:", file=sys.stderr)
        for issue in issues:
            print(f"  {issue}", file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
