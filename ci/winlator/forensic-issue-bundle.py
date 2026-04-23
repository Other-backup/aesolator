#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path


def read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8", errors="ignore")
    except FileNotFoundError:
        return ""


def read_keyvals(path: Path) -> dict[str, str]:
    data: dict[str, str] = {}
    for raw in read_text(path).splitlines():
        if "=" not in raw:
            continue
        key, value = raw.split("=", 1)
        data[key.strip()] = value.strip()
    return data


def load_assembler(path: Path) -> dict:
    if not path.is_file():
        return {}
    try:
        return json.loads(path.read_text(encoding="utf-8", errors="ignore"))
    except json.JSONDecodeError:
        return {}


def build_markdown(scenario_dir: Path, root_mode: str) -> str:
    scenario_meta = read_keyvals(scenario_dir / "scenario_meta.txt")
    session_meta = read_keyvals(scenario_dir.parent / "session_meta.txt")
    wait_meta = read_keyvals(scenario_dir / "wait-status.txt")
    assembler = load_assembler(scenario_dir / "runtime-log-assembler.json")
    issues = assembler.get("issues", []) if isinstance(assembler, dict) else []
    events = assembler.get("forensic_events_tail", []) if isinstance(assembler, dict) else []
    summary = read_text(scenario_dir / "runtime-log-assembler.summary.txt").strip()
    ptrace_summary = read_text(scenario_dir / "runas-ptrace.summary.txt").strip()
    logcat_filtered = read_text(scenario_dir / "logcat-filtered.txt").splitlines()[-20:]
    files = sorted(p.name for p in scenario_dir.iterdir() if p.is_file())
    runtime_logs = sorted((scenario_dir / "runtime-logs").iterdir()) if (scenario_dir / "runtime-logs").is_dir() else []

    lines = [
        "# Ae.solator adb forensic issue bundle",
        "",
        "## Metadata",
        "",
        f"- label: `{scenario_meta.get('label', '')}`",
        f"- package: `{scenario_meta.get('package', session_meta.get('package', ''))}`",
        f"- serial: `{scenario_meta.get('serial', session_meta.get('serial', ''))}`",
        f"- root_mode: `{root_mode}`",
        f"- wait_status: `intent={wait_meta.get('saw_intent', '')} submit={wait_meta.get('saw_submit', '')} terminal={wait_meta.get('saw_terminal', '')}`",
        f"- assembler_summary: `{summary}`",
        "",
        "## Top issues",
        "",
    ]
    if not issues:
        lines.append("- none")
    else:
        for row in issues[:30]:
            lines.append(
                f"- `{row.get('severity', '')}` `{row.get('category', '')}` `{row.get('library', '')}` "
                f"from `{row.get('source', '')}:{row.get('lineno', '')}`: {row.get('detail', '')}"
            )
    lines.extend(["", "## Forensic events tail", ""])
    if not events:
        lines.append("- none")
    else:
        for row in events[-20:]:
            lines.append(
                f"- `{row.get('ts', '')}` `{row.get('severity', '')}` `{row.get('event_id', '')}` `{row.get('stage', '')}`: {row.get('message', '')}"
            )
    lines.extend(["", "## Logcat tail", "", "```text"])
    lines.extend(logcat_filtered or ["<empty>"])
    lines.extend(["```", "", "## run-as ptrace", "", "```text"])
    lines.extend((ptrace_summary.splitlines() if ptrace_summary else ["<empty>"]))
    lines.extend(["```", "", "## Files", ""])
    for name in files:
        lines.append(f"- `{name}`")
    for path in runtime_logs[:60]:
        lines.append(f"- `runtime-logs/{path.name}`")
    lines.extend(["", "## Host follow-up", "", "```bash"])
    lines.append(f"python3 ci/winlator/forensic-issue-bundle.py --scenario-dir {scenario_dir}")
    lines.append("```")
    return "\n".join(lines) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--scenario-dir", required=True)
    parser.add_argument("--root-mode", default="nonroot")
    parser.add_argument("--output", default="")
    args = parser.parse_args()

    scenario_dir = Path(args.scenario_dir).resolve()
    if not scenario_dir.is_dir():
        raise SystemExit(f"scenario dir not found: {scenario_dir}")
    output = Path(args.output) if args.output else scenario_dir / "ISSUE.md"
    output.write_text(build_markdown(scenario_dir, args.root_mode), encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
