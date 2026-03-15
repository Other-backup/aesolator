#!/usr/bin/env python3
"""Assemble per-scenario runtime logs into a normalized low-level debug report."""

from __future__ import annotations

import argparse
import json
import re
from collections import Counter
from pathlib import Path


ISSUE_PATTERNS = [
    (
        "import_dll_missing",
        "high",
        re.compile(r"err:module:import_dll Library ([^ ]+) .* not found", re.IGNORECASE),
    ),
    (
        "wine_loader_init_failed",
        "high",
        re.compile(r'err:module:attach_process_dlls .*?"([^"]+\.dll)".*failed to initialize', re.IGNORECASE),
    ),
    (
        "undefined_symbol",
        "high",
        re.compile(r"undefined symbol: ([A-Za-z0-9_@.]+)", re.IGNORECASE),
    ),
    (
        "wine_loader_missing_module",
        "high",
        re.compile(r"trace:(?:loaddll|module):.*?(?:failed to load|not found).*?([A-Za-z0-9_./-]+\.(?:so|dll))", re.IGNORECASE),
    ),
    (
        "wine_loader_missing_module",
        "high",
        re.compile(r"trace:(?:loaddll|module):.*?([A-Za-z0-9_./-]+\.(?:so|dll)).*(?:not found|failed)", re.IGNORECASE),
    ),
    (
        "dlopen_failed",
        "high",
        re.compile(r"dlopen failed:.*?([A-Za-z0-9_./-]+\.(?:so|dll))", re.IGNORECASE),
    ),
    (
        "load_failed",
        "high",
        re.compile(r"(?:could not load|failed to load) ([A-Za-z0-9_./-]+\.(?:so|dll))", re.IGNORECASE),
    ),
    (
        "abi_mismatch",
        "high",
        re.compile(r"\b(status c000007b|wrong ELF class|Exec format error)\b", re.IGNORECASE),
    ),
    (
        "assertion_failed",
        "high",
        re.compile(r"assertion .* failed", re.IGNORECASE),
    ),
    (
        "module_not_found",
        "high",
        re.compile(r"cannot open shared object file.*?([A-Za-z0-9_./-]+\.(?:so|dll))", re.IGNORECASE),
    ),
    (
        "module_not_found",
        "high",
        re.compile(r'library "([^"]+\.(?:so|dll))" not found', re.IGNORECASE),
    ),
    (
        "wine_loader_unresolved_import",
        "high",
        re.compile(r"unresolved import.*?([A-Za-z0-9_@.]+)", re.IGNORECASE),
    ),
    (
        "vkbasalt_guard",
        "high",
        re.compile(r"AERO_UPSCALE_VKBASALT_REASON=(fsr_assert_guard)", re.IGNORECASE),
    ),
    (
        "vulkan_validation_error",
        "high",
        re.compile(r"(Validation Error:|VUID-[A-Za-z0-9_-]+)", re.IGNORECASE),
    ),
    (
        "vulkan_validation_warning",
        "medium",
        re.compile(r"Validation Warning:", re.IGNORECASE),
    ),
    (
        "vulkan_loader_error",
        "high",
        re.compile(r"\[Loader Message\].*(failed|not found|unable|error)", re.IGNORECASE),
    ),
    (
        "vulkan_layer_missing",
        "high",
        re.compile(r"(VK_LAYER_[A-Za-z0-9_]+).*(not found|cannot be found|failed to load)", re.IGNORECASE),
    ),
    (
        "vulkan_icd_missing",
        "high",
        re.compile(r"(?:driver manifest|ICD).*(not found|cannot be found|failed to open|failed to load)", re.IGNORECASE),
    ),
]

EMBEDDED_FORENSIC_JSON_RE = re.compile(r'(\{.*"event_id"\s*:\s*"[^"]+".*\})')
EVENT_RULES = {
    "RUNTIME_LIBRARY_COMPONENT_CONFLICT": ("runtime_component_conflict", "high"),
    "RUNTIME_LIBRARY_CONFLICT_DETECTED": ("runtime_library_conflict", "high"),
    "RUNTIME_LIBRARY_CONFLICT_SNAPSHOT": ("runtime_conflict_snapshot_dirty", "medium"),
    "RUNTIME_LOADER_TRACE_CONTRACT_SNAPSHOT": ("loader_trace_contract_snapshot", "medium"),
    "TURNIP_SOURCE_FAILED": ("turnip_source_failed", "high"),
    "TURNIP_INSTALL_REJECTED": ("turnip_install_rejected", "high"),
    "TURNIP_DOWNLOAD_FAILED": ("turnip_download_failed", "high"),
    "ROUTE_CONTAINER_NOT_FOUND": ("route_container_not_found", "high"),
    "PARSER_CONTAINER_CONFIG_ERROR": ("container_config_error", "high"),
    "PARSER_CONTAINER_RUNTIME_ERROR": ("container_runtime_error", "high"),
    "RUNTIME_DRIFT_DETECTED": ("runtime_drift_detected", "medium"),
    "UPSCALE_MODULE_SKIPPED": ("upscale_module_skipped", "medium"),
}
SEVERITY_ORDER = {"info": 0, "low": 1, "medium": 2, "high": 3}


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


def discover_input_files(scenario_dir: Path) -> list[Path]:
    files = []
    for name in ("forensics-jsonl-tail.txt", "logcat-filtered.txt", "logcat-full.txt"):
        path = scenario_dir / name
        if path.is_file():
            files.append(path)
    runtime_dir = scenario_dir / "runtime-logs"
    if runtime_dir.is_dir():
        files.extend(discover_latest_runtime_logs(runtime_dir))
    return files


def discover_latest_runtime_logs(runtime_dir: Path) -> list[Path]:
    latest_by_stream: dict[str, Path] = {}
    for path in sorted(p for p in runtime_dir.iterdir() if p.is_file()):
        stream = infer_runtime_stream(path)
        current = latest_by_stream.get(stream)
        if current is None or path.name > current.name:
            latest_by_stream[stream] = path
    return sorted(latest_by_stream.values())


def infer_runtime_stream(path: Path) -> str:
    match = re.match(r"(.+)_\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}\.txt$", path.name)
    if match:
        return match.group(1)
    return path.stem


def infer_library(category: str, match: re.Match[str], line: str) -> str:
    if category == "abi_mismatch":
        lowered = line.lower()
        if "kernel32.dll" in lowered:
            return "kernel32.dll"
        if "vkbasalt" in lowered or "reshade" in lowered:
            return "vkbasalt/reshade"
        return "runtime_loader"
    if category == "assertion_failed":
        lowered = line.lower()
        if "vkbasalt" in lowered or "reshade" in lowered:
            return "vkbasalt/reshade"
        return "runtime_assert"
    if category == "vkbasalt_guard":
        return "vkbasalt"
    if category.startswith("wine_loader"):
        if match.groups():
            return (match.group(1) or "").strip()
        return "wine_loader"
    if category.startswith("vulkan_validation"):
        return "vulkan_validation"
    if category == "vulkan_loader_error":
        return "vulkan_loader"
    if category == "vulkan_layer_missing":
        return match.group(1).strip() if match.groups() else "vulkan_layer"
    if category == "vulkan_icd_missing":
        return "vulkan_icd"
    if match.groups():
        return (match.group(1) or "").strip()
    return "unknown"


def extract_json_event(line: str) -> dict[str, object] | None:
    match = EMBEDDED_FORENSIC_JSON_RE.search(line)
    if not match:
        return None
    try:
        payload = json.loads(match.group(1))
    except json.JSONDecodeError:
        return None
    if "event_id" not in payload:
        return None
    return payload


def normalize_severity(value: str) -> str:
    lowered = (value or "").strip().lower()
    if lowered in SEVERITY_ORDER:
        return lowered
    if lowered == "warn":
        return "medium"
    if lowered == "error":
        return "high"
    return "low"


def infer_conflict_library(conflict: str) -> str:
    lowered = (conflict or "").lower()
    for token, library in (
        ("dxvk", "dxvk"),
        ("vkd3d", "vkd3d"),
        ("dgvoodoo", "dgvoodoo"),
        ("ddraw", "dgvoodoo"),
        ("turnip", "turnip"),
        ("layout", "layout"),
        ("translator", "translator"),
        ("fex", "fex"),
        ("box", "box"),
        ("nvapi", "nvapi"),
        ("emulator", "runtime_emulator"),
        ("logging", "runtime_logging"),
        ("x11", "x11"),
    ):
        if token in lowered:
            return library
    return conflict or "runtime_conflict"


def infer_event_library(event_id: str, payload: dict[str, object]) -> str:
    component = str(payload.get("component", "")).strip()
    if component:
        return component
    module = str(payload.get("module", "")).strip()
    if module:
        return module
    conflict = str(payload.get("conflict", "")).strip()
    if conflict:
        return infer_conflict_library(conflict)
    message = str(payload.get("message", "")).lower()
    if event_id.startswith("TURNIP_"):
        return "turnip"
    if event_id.startswith("VULKAN_"):
        return "vulkan_policy"
    if event_id.startswith("UPSCALE_"):
        return "upscale"
    if event_id.startswith("PARSER_"):
        return "container_parser"
    if event_id.startswith("RUNTIME_LOADER_TRACE_"):
        return "wine_loader"
    if event_id == "RUNTIME_DRIFT_DETECTED":
        return "runtime_profile"
    if "vkbasalt" in message or "reshade" in message:
        return "vkbasalt/reshade"
    return event_id.lower()


def build_event_detail(payload: dict[str, object]) -> str:
    parts = []
    message = str(payload.get("message", "")).strip()
    if message:
        parts.append(message)
    for key in ("component", "state", "expected", "conflict", "module", "reason", "error_class", "error_detail"):
        value = str(payload.get(key, "")).strip()
        if value:
            parts.append(f"{key}={value}")
    return " | ".join(parts) if parts else str(payload.get("event_id", "forensic_event"))


def issue_from_event(payload: dict[str, object]) -> dict[str, str] | None:
    event_id = str(payload.get("event_id", "")).strip()
    if not event_id:
        return None

    category = ""
    severity = ""
    if event_id == "LAUNCH_EXEC_EXIT":
        exit_code = str(payload.get("exit_code", "")).strip()
        if exit_code and exit_code != "0":
            category = "launch_exit_nonzero"
            severity = "medium"
    elif event_id == "RUNTIME_LIBRARY_CONFLICT_SNAPSHOT":
        count = str(payload.get("count", "")).strip()
        if count and count != "0":
            category, severity = EVENT_RULES[event_id]
    elif event_id == "VULKAN_VERSION_POLICY_APPLIED":
        reason = str(payload.get("reason", "")).strip().lower()
        if "driver_cap" in reason:
            category = "vulkan_version_driver_cap"
            severity = "medium"
    elif event_id == "RUNTIME_LOADER_TRACE_CONTRACT_SNAPSHOT":
        if str(payload.get("loader_trace_effective", "")).strip() != "1":
            category = "loader_trace_disabled"
            severity = "high"
    else:
        mapped = EVENT_RULES.get(event_id)
        if mapped:
            category, severity = mapped

    if not category and normalize_severity(str(payload.get("severity", ""))) == "high":
        category = "forensic_error_event"
        severity = "high"

    if not category:
        return None

    if event_id == "UPSCALE_MODULE_SKIPPED":
        reason = str(payload.get("reason", "")).lower()
        if "guard" in reason or "assert" in reason:
            severity = "high"

    return {
        "source": "",
        "lineno": "",
        "category": category,
        "severity": severity,
        "library": infer_event_library(event_id, payload),
        "event_id": event_id,
        "stage": str(payload.get("stage", "")),
        "detail": build_event_detail(payload),
    }


def parse_issues(files: list[Path], scenario_dir: Path) -> list[dict[str, str]]:
    rows: list[dict[str, str]] = []
    seen = set()
    for path in files:
        text = read_text(path)
        for lineno, line in enumerate(text.splitlines(), start=1):
            for category, severity, pattern in ISSUE_PATTERNS:
                match = pattern.search(line)
                if not match:
                    continue
                library = infer_library(category, match, line)
                row = {
                    "source": str(path.relative_to(scenario_dir)),
                    "lineno": str(lineno),
                    "category": category,
                    "severity": severity,
                    "library": library,
                    "event_id": "",
                    "stage": "",
                    "detail": line.strip(),
                }
                key = (row["category"], row["severity"], row["library"], row["event_id"], row["detail"])
                if key in seen:
                    continue
                seen.add(key)
                rows.append(row)
                break
            payload = extract_json_event(line)
            if not payload:
                continue
            row = issue_from_event(payload)
            if not row:
                continue
            row["source"] = str(path.relative_to(scenario_dir))
            row["lineno"] = str(lineno)
            key = (row["category"], row["severity"], row["library"], row["event_id"], row["detail"])
            if key in seen:
                continue
            seen.add(key)
            rows.append(row)
    return rows


def parse_forensic_events(files: list[Path], scenario_dir: Path) -> list[dict[str, str]]:
    events: list[dict[str, str]] = []
    seen = set()
    for path in files:
        text = read_text(path)
        for lineno, line in enumerate(text.splitlines(), start=1):
            obj = extract_json_event(line)
            if not obj:
                continue
            event = {
                "ts": str(obj.get("ts", "")),
                "severity": str(obj.get("severity", "")),
                "event_id": str(obj.get("event_id", "")),
                "stage": str(obj.get("stage", "")),
                "message": str(obj.get("message", "")),
                "source": str(path.relative_to(scenario_dir)),
                "lineno": str(lineno),
            }
            key = tuple(event.items())
            if key in seen:
                continue
            seen.add(key)
            events.append(event)
    return events


def write_tsv(path: Path, rows: list[dict[str, str]]) -> None:
    headers = ["source", "lineno", "category", "severity", "library", "event_id", "stage", "detail"]
    with path.open("w", encoding="utf-8") as fh:
        fh.write("\t".join(headers) + "\n")
        for row in rows:
            fh.write("\t".join(row.get(key, "").replace("\t", " ").replace("\n", " ") for key in headers) + "\n")


def write_json(path: Path, scenario_meta: dict[str, str], wait_meta: dict[str, str], rows: list[dict[str, str]], events: list[dict[str, str]]) -> None:
    payload = {
        "scenario": scenario_meta,
        "wait_status": wait_meta,
        "issue_count": len(rows),
        "issues": rows,
        "forensic_events_tail": events[-40:],
    }
    path.write_text(json.dumps(payload, ensure_ascii=True, indent=2), encoding="utf-8")


def write_summary(path: Path, rows: list[dict[str, str]]) -> None:
    category_counts = Counter(row["category"] for row in rows)
    library_counts = Counter(row["library"] for row in rows)
    max_severity = "info"
    if rows:
        max_severity = max(rows, key=lambda row: SEVERITY_ORDER.get(row["severity"], 0))["severity"]
    lines = [
        "runtime_log_assembler_summary",
        f"issue_count={len(rows)}",
        f"max_severity={max_severity}",
        "category_counts=" + ",".join(f"{key}:{category_counts[key]}" for key in sorted(category_counts)) if rows else "category_counts=none",
        "library_counts=" + ",".join(f"{key}:{library_counts[key]}" for key in sorted(library_counts)) if rows else "library_counts=none",
    ]
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def write_markdown(
    path: Path,
    scenario_meta: dict[str, str],
    wait_meta: dict[str, str],
    rows: list[dict[str, str]],
    events: list[dict[str, str]],
) -> None:
    lines = [
        "# Runtime Log Assembler",
        "",
        "## Scenario",
        "",
        f"- label: `{scenario_meta.get('label', '')}`",
        f"- container_id: `{scenario_meta.get('container_id', '')}`",
        f"- trace_id: `{read_text(path.parent / 'trace_id.txt').strip()}`",
        f"- elapsed_sec: `{wait_meta.get('elapsed_sec', '')}`",
        f"- saw_intent: `{wait_meta.get('saw_intent', '')}`",
        f"- saw_submit: `{wait_meta.get('saw_submit', '')}`",
        f"- saw_terminal: `{wait_meta.get('saw_terminal', '')}`",
        "",
        "## Issues",
        "",
    ]
    if not rows:
        lines.append("- none")
    else:
        for row in rows[:80]:
            lines.append(
                f"- `{row['severity']}` `{row['category']}` `{row['library']}` "
                f"from `{row['source']}:{row['lineno']}`"
                + (f" event=`{row['event_id']}`" if row["event_id"] else "")
                + (f" stage=`{row['stage']}`" if row["stage"] else "")
                + f": {row['detail']}"
            )
    lines.extend(
        [
            "",
            "## Forensic Events Tail",
            "",
        ]
    )
    if not events:
        lines.append("- none")
    else:
        for event in events[-25:]:
            lines.append(
                f"- `{event['ts']}` `{event['severity']}` `{event['event_id']}` "
                f"`{event['stage']}` `{event['source']}:{event['lineno']}` {event['message']}"
            )
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", "--scenario-dir", dest="scenario_dir", required=True)
    parser.add_argument("--output-prefix", default="")
    args = parser.parse_args()

    scenario_dir = Path(args.scenario_dir).resolve()
    if not scenario_dir.is_dir():
        raise SystemExit(f"scenario dir not found: {scenario_dir}")

    prefix = Path(args.output_prefix) if args.output_prefix else (scenario_dir / "runtime-log-assembler")
    scenario_meta = read_keyvals(scenario_dir / "scenario_meta.txt")
    wait_meta = read_keyvals(scenario_dir / "wait-status.txt")
    files = discover_input_files(scenario_dir)
    rows = parse_issues(files, scenario_dir)
    events = parse_forensic_events(files, scenario_dir)

    write_tsv(prefix.with_suffix(".tsv"), rows)
    write_json(prefix.with_suffix(".json"), scenario_meta, wait_meta, rows, events)
    write_summary(prefix.with_suffix(".summary.txt"), rows)
    write_markdown(prefix.with_suffix(".md"), scenario_meta, wait_meta, rows, events)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
