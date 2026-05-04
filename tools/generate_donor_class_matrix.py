#!/usr/bin/env python3
"""Generate a Black-Diamond donor sweep matrix for every Java/Kotlin class in repo.

Optionally compares against checked-out donor trees under `donors/<name>/` and
reports path matches + hash parity to accelerate literal file-by-file transfer.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from dataclasses import dataclass
from datetime import date
from pathlib import Path
from typing import Iterable

REPO_ROOT = Path(__file__).resolve().parents[1]
OUT_PATH = REPO_ROOT / "docs" / "DONOR_CLASS_SWEEP_MATRIX_2026-05-04.md"

DONOR_CONFIG = REPO_ROOT / "tools" / "donor_sources_2026_05_04.json"
IGNORE_DIRS = {".git", ".gradle", "build", ".idea", "out", "node_modules"}


@dataclass(frozen=True)
class ClassFile:
    rel: str
    lane: str
    kind: str
    digest12: str


@dataclass(frozen=True)
class DonorMatch:
    donor_key: str
    donor_path: str
    donor_digest12: str
    status: str


def file_digest12(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 128), b""):
            h.update(chunk)
    return h.hexdigest()[:12]


def walk_class_files(root: Path) -> list[ClassFile]:
    rows: list[ClassFile] = []
    for path in sorted(root.rglob("*")):
        if not path.is_file():
            continue
        if path.suffix not in {".java", ".kt"}:
            continue
        if any(part in IGNORE_DIRS for part in path.relative_to(root).parts):
            continue
        rel = path.relative_to(root).as_posix()
        lane = "app-runtime" if "com/winlator" in rel else "repo-support"
        kind = "kotlin" if path.suffix == ".kt" else "java"
        rows.append(ClassFile(rel=rel, lane=lane, kind=kind, digest12=file_digest12(path)))
    return rows


def build_donor_index(donor_root: Path) -> dict[str, list[Path]]:
    index: dict[str, list[Path]] = {}
    if not donor_root.exists():
        return index
    for path in donor_root.rglob("*"):
        if not path.is_file() or path.suffix not in {".java", ".kt"}:
            continue
        index.setdefault(path.name, []).append(path)
    return index


def pick_best_candidate(local_rel: str, candidates: Iterable[Path]) -> Path | None:
    local_name = Path(local_rel).name
    best: tuple[int, Path] | None = None
    for cand in candidates:
        score = 0
        c = cand.as_posix()
        if "/com/winlator/" in local_rel and "/com/winlator/" in c:
            score += 4
        if "/src/main/" in c:
            score += 2
        if c.endswith(local_rel):
            score += 6
        if local_name == cand.name:
            score += 1
        if best is None or score > best[0]:
            best = (score, cand)
    return best[1] if best else None


def donor_match(local: ClassFile, donor_dir: Path, donor_key: str, donor_index: dict[str, list[Path]]) -> DonorMatch | None:
    candidates = donor_index.get(Path(local.rel).name, [])
    if not candidates:
        return None
    best = pick_best_candidate(local.rel, candidates)
    if not best:
        return None
    digest = file_digest12(best)
    status = "matched" if digest == local.digest12 else "review"
    return DonorMatch(
        donor_key=donor_key,
        donor_path=best.relative_to(donor_dir).as_posix(),
        donor_digest12=digest,
        status=status,
    )


def load_donors(config: Path) -> list[dict[str, str]]:
    data = json.loads(config.read_text(encoding="utf-8"))
    donors = data.get("donors", [])
    if not donors:
        raise ValueError("No donors in config")
    return donors


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--donors-root", default=str(REPO_ROOT / "donors"), help="Folder containing donor checkouts by key")
    ap.add_argument("--out", default=str(OUT_PATH))
    ap.add_argument("--donor-config", default=str(DONOR_CONFIG))
    args = ap.parse_args()

    donors_root = Path(args.donors_root)
    out = Path(args.out)
    classes = walk_class_files(REPO_ROOT)
    donors = load_donors(Path(args.donor_config))

    header = [
        "# Donor Class Sweep Matrix (Java/Kotlin)",
        "",
        f"Updated: `{date.today().isoformat()}`",
        "",
        "Black-Diamond whole-repo matrix for literal class-by-class donor transfer.",
        "Scope is all Java/Kotlin classes in this repository (excluding generated/build cache paths).",
        "",
        "## Status legend",
        "",
        "- `pending`: class not yet reviewed against donor",
        "- `matched`: donor class hash equals local class hash",
        "- `review`: donor candidate exists but differs and needs selective transfer",
        "- `hold`: explicitly rejected with evidence",
        "",
        "## Matrix",
        "",
    ]

    cols = ["Class", "Lane", "Kind", "Local SHA12"]
    for donor in donors:
        label = donor["label"]
        cols += [f"{label}:status", f"{label}:path", f"{label}:sha12"]
    cols += ["Decision", "Evidence"]

    lines = ["| " + " | ".join(cols) + " |", "|" + "|".join([" --- " for _ in cols]) + "|"]

    donor_roots = {d["key"]: donors_root / d["key"] for d in donors}
    donor_indexes = {d["key"]: build_donor_index(donor_roots[d["key"]]) for d in donors}

    for c in classes:
        row = [c.rel, c.lane, c.kind, c.digest12]
        for donor in donors:
            key = donor["key"]
            m = donor_match(c, donor_roots[key], key, donor_indexes[key])
            if m is None:
                row += ["pending", "-", "-"]
            else:
                row += [m.status, m.donor_path, m.donor_digest12]
        row += ["pending", "docs/GAMENATIVE_RUNTIME_GAP_INVENTORY.md"]
        lines.append("| " + " | ".join(row) + " |")

    out.write_text("\n".join(header + lines) + "\n", encoding="utf-8")
    print(f"Wrote {out} with {len(classes)} class rows")


if __name__ == "__main__":
    main()
