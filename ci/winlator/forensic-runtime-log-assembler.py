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
    "VULKAN_LAYER_REQUEST_SKIPPED": ("vulkan_layer_request_skipped", "medium"),
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
FREEWINE_START_STEP_RE = re.compile(
    r"freewine-loader: start_main_thread (?P<step>\S+)(?: (?P<detail>.*))?$"
)
FREEWINE_BOOTSTRAP_SCOPE_RE = re.compile(
    r"freewine-bootstrap: scope=(?P<scope>[^ ]+) phase=(?P<phase>[^ ]+)(?: (?P<detail>.*))?$"
)
FREEWINE_LOADER_INIT_RE = re.compile(r"freewine-loader-init: (?P<step>.+)$")
FREEWINE_LOADER_INIT_STATE_RE = re.compile(
    r"freewine-loader-init-state: step=(?P<step>[^ ]+) (?P<key>[^= ]+)=(?P<value>\S+)"
)
FREEWINE_LOADER_INIT_STATUS_RE = re.compile(
    r"freewine-loader-init-status: step=(?P<step>[^ ]+) status=(?P<status>\S+)"
)
FREEWINE_IMPORT_RE = re.compile(
    r"freewine-loader-import: importer=(?P<importer>.+?) import_dll=(?P<import_dll>\S+)"
    r"(?: symbol=(?P<symbol>[^\s]+)| ordinal=(?P<ordinal>\S+)) stub=(?P<stub>\S+)"
)
FREEWINE_PROCESS_ATTACH_RE = re.compile(
    r'freewine-process-attach: phase=(?P<phase>[^ ]+) module=(?P<module>L"[^"]+"|[^ ]+) status=(?P<status>\S+)'
)
FREEWINE_MODULE_INIT_RE = re.compile(
    r'freewine-module-init: phase=(?P<phase>[^ ]+) module=(?P<module>L"[^"]+"|[^ ]+) '
    r"reason=(?P<reason>\S+) status=(?P<status>\S+)"
)
FREEWINE_LOADER_STATUS_RE = re.compile(
    r'freewine-loader-status: tag=(?P<tag>[^ ]+) module=(?P<module>L"[^"]+"|[^ ]+) status=(?P<status>\S+)'
)
FREEWINE_LOADER_LOAD_DLL_RE = re.compile(
    r'freewine-loader-load-dll: lib=(?P<lib>L"[^"]+"|[^ ]+) load_path=(?P<load_path>L"[^"]+"|[^ ]+) status=(?P<status>\S+)'
)
FREEWINE_UNIXLIB_PHASE_RE = re.compile(
    r"freewine-unixlib: phase=(?P<phase>[^ ]+)"
    r"(?: module=(?P<module>[^ ]+))?"
    r"(?: path=(?P<path>[^ ]+))?"
    r"(?: symbol=(?P<symbol>[^ ]+))?"
    r"(?: handle=(?P<handle>[^ ]+))?"
    r"(?: funcs=(?P<funcs>[^ ]+))?"
    r"(?: status=(?P<status>\S+))?"
    r"(?: error=(?P<error>.*))?$"
)
FREEWINE_WIN32U_RE = re.compile(
    r'freewine-win32u: phase=(?P<phase>[^ ]+) module=(?P<module>L"[^"]+"|[^ ]+)'
    r"(?: name=(?P<name>[^ ]+))?"
    r"(?: ptr=(?P<ptr>\S+))?"
    r" status=(?P<status>\S+)"
)
FREEWINE_LAUNCHER_RE = re.compile(
    r"freewine-launcher: phase=(?P<phase>[^ ]+)"
    r"(?: path=(?P<path>[^ ]+))?"
    r"(?: name=(?P<name>[^ ]+))?"
    r"(?: ptr=(?P<ptr>\S+))?"
    r"(?: error=(?P<error>.*))?$"
)
FREEWINE_KERNELBASE_RE = re.compile(
    r"freewine-kernelbase: phase=(?P<phase>[^ ]+)"
    r"(?: name=(?P<name>[^ ]+))?"
    r"(?: value=(?P<value>\S+))?"
    r" status=(?P<status>\S+)"
)
FREEWINE_KERNEL32_RE = re.compile(
    r"freewine-kernel32: phase=(?P<phase>[^ ]+)"
    r"(?: name=(?P<name>[^ ]+))?"
    r"(?: value=(?P<value>\S+))?"
    r" status=(?P<status>\S+)"
)
FREEWINE_USER32_RE = re.compile(
    r"freewine-user32: phase=(?P<phase>[^ ]+)"
    r"(?: name=(?P<name>[^ ]+) value=(?P<value>\S+))?"
    r"(?: detail=(?P<detail>L\"[^\"]+\"|\"[^\"]+\"|[^ ]+))?"
    r" status=(?P<status>\S+)"
)
FREEWINE_SERVER_RE = re.compile(
    r"freewine-server: phase=(?P<phase>[^ ]+)"
    r"(?: name=(?P<name>[^ ]+) value=(?P<value>\S+))?"
    r" status=(?P<status>\S+)"
)
FREEWINE_USERDRIVER_RE = re.compile(
    r"freewine-userdriver: phase=(?P<phase>[^ ]+)"
    r"(?: driver=(?P<driver>L\"[^\"]+\"|\"[^\"]+\"|[^ ]+))?"
    r"(?: name=(?P<name>[^ ]+))?"
    r"(?: value=(?P<value>\S+))?"
    r" status=(?P<status>\S+)"
)
FREEWINE_EXPLORER_RE = re.compile(
    r"freewine-explorer: phase=(?P<phase>[^ ]+)"
    r"(?: name=(?P<name>L\"[^\"]+\"|\"[^\"]+\"|[^ ]+))?"
    r"(?: value=(?P<value>\S+))?"
    r" status=(?P<status>\S+)"
)
FREEWINE_EXPLORER_MAIN_RE = re.compile(
    r"freewine-explorer-main: phase=(?P<phase>[^ ]+)"
    r"(?: name=(?P<name>L\"[^\"]+\"|\"[^\"]+\"|[^ ]+))?"
    r"(?: value=(?P<value>\S+))?"
    r" status=(?P<status>\S+)"
)
FREEWINE_WINEBOOT_RE = re.compile(r"freewine-wineboot: (?P<detail>.+)$")
FREEWINE_SERVICES_RE = re.compile(r"freewine-services: (?P<detail>.+)$")
FREEWINE_LDRDISABLE_RE = re.compile(r"freewine-ldrdisable: (?P<detail>.+)$")
FREEWINE_LDRTHUNK_RE = re.compile(r"freewine-ldrthunk: (?P<detail>.+)$")
FREEWINE_NTDLL_ENV_NLS_RE = re.compile(r"freewine-ntdll-env-nls: (?P<detail>.+)$")
FREEWINE_PREFIX_RE = re.compile(r"freewine-(?P<prefix>[a-z0-9-]+): (?P<detail>.+)$")
FREEWINE_BOOTSTRAP_FILE_RE = re.compile(r"(?:^| )file=(?P<file>.+)$")
ANDROID_LINKER_RE = re.compile(
    r"\blink(?:er64)?\b\s*:\s*(?P<message>.*)$", re.IGNORECASE
)
FREEWINE_PROCESS_PARAMS_RE = re.compile(
    r"freewine-process-params: phase=(?P<phase>[^ ]+)"
    r"(?: (?P<key>[^= ]+)=(?P<value>\S+)| status=(?P<status>\S+))"
)
FREEWINE_NTCREATE_RE = re.compile(
    r"freewine-ntcreate: phase=(?P<phase>[^ ]+)"
    r"(?: (?P<key>[^= ]+)=(?P<value>\S+)| status=(?P<status>\S+))"
)
FREEWINE_SPAWN_RE = re.compile(
    r"freewine-spawn: phase=(?P<phase>[^ ]+) (?P<key>[^= ]+)=(?P<value>\S+)"
)
FREEWINE_UNIX_LOADER_RE = re.compile(
    r"freewine-unix-loader: phase=(?P<phase>[^ ]+)(?: .*)?$"
)
FREEWINE_HEAP_INVALID_RE = re.compile(
    r"freewine-heap-invalid: phase=(?P<phase>[^ ]+) handle=(?P<handle>\S+).*process_heap=(?P<process_heap>\S+).*process_params=(?P<process_params>\S+)"
)
FREEWINE_HEAP_CREATE_RE = re.compile(
    r"freewine-heap-create: phase=(?P<phase>[^ ]+) flags=(?P<flags>\S+).*heap=(?P<heap>\S+).*"
    r"process_heap_static=(?P<process_heap_static>\S+).*peb_process_heap=(?P<peb_process_heap>\S+)"
)
FREEWINE_HEAP_OP_RE = re.compile(
    r"freewine-heap-op: op=(?P<op>[^ ]+) handle=(?P<handle>\S+) flags=(?P<flags>\S+) "
    r"(?P<key>[^= ]+)=(?P<value>\S+) result=(?P<result>\S+).*"
    r"process_heap_static=(?P<process_heap_static>\S+).*peb_process_heap=(?P<peb_process_heap>\S+).*"
    r"process_params=(?P<process_params>\S+).*caller=(?P<caller>\S+)"
)
RAW_HEAP_INVALID_RE = re.compile(
    r"err:heap:unsafe_heap_from_handle Invalid handle", re.IGNORECASE
)
STATUS_ZERO_TOKENS = {
    "0",
    "00000000",
    "0000000000000000",
    "0x0",
    "0x00000000",
    "0x0000000000000000",
    "00000000000000000000000000000000",
}


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
    for name in ("forensics-jsonl-tail.txt", "logcat-filtered.txt", "logcat-full.txt", "logcat-linker.txt"):
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
        if current is None or normalize_runtime_log_name(path.name) > normalize_runtime_log_name(current.name):
            latest_by_stream[stream] = path
    return sorted(latest_by_stream.values())


def infer_runtime_stream(path: Path) -> str:
    logical_name = normalize_runtime_log_name(path.name)
    match = re.match(r"(.+)_\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}\.txt$", logical_name)
    if match:
        return match.group(1)
    return Path(logical_name).stem


def normalize_runtime_log_name(name: str) -> str:
    for prefix in ("app-private__", "external__"):
        if name.startswith(prefix):
            return name[len(prefix):]
    return name


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


def normalize_module_token(token: str) -> str:
    value = (token or "").strip()
    if value.startswith('L"') and value.endswith('"'):
        return value[2:-1]
    if value.startswith('"') and value.endswith('"'):
        return value[1:-1]
    return value


def status_is_zero(token: str) -> bool:
    value = (token or "").strip().lower()
    return value in STATUS_ZERO_TOKENS


def compact_freewine_detail(*parts: str) -> str:
    return " | ".join(part for part in parts if part)


def append_unique(items: list[str], value: str) -> None:
    if value and value not in items:
        items.append(value)


def classify_autotouch_file(path: str) -> str:
    value = (path or "").strip()
    if not value:
        return "unknown"
    for marker in ("/wine-src/", "/freewine11-head/"):
        if marker in value:
            value = value.split(marker, 1)[1]
            break
    value = value.lstrip("./")
    parts = [part for part in value.split("/") if part]
    if len(parts) >= 2 and parts[0] in {"dlls", "programs", "libs", "tools"}:
        return f"{parts[0]}/{parts[1]}"
    if parts and parts[0] == "server":
        return "server"
    return parts[0] if parts else "unknown"


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


def parse_freewine_bootstrap(files: list[Path], scenario_dir: Path) -> dict[str, object]:
    prefix_counter: Counter[str] = Counter()
    autotouch_module_counter: Counter[str] = Counter()
    autotouch_exec_module_counter: Counter[str] = Counter()
    unknown_prefix_counter: Counter[str] = Counter()
    state: dict[str, object] = {
        "start_steps": [],
        "bootstrap_scope_events": [],
        "loader_init_steps": [],
        "loader_init_states": [],
        "loader_init_statuses": [],
        "launcher_events": [],
        "kernelbase_events": [],
        "kernel32_events": [],
        "user32_events": [],
        "server_events": [],
        "attach_begin_order": [],
        "dependency_cascade": [],
        "critical_import_stubs": [],
        "loader_statuses": [],
        "loader_load_dll_events": [],
        "module_init_failures": [],
        "process_attach_failures": [],
        "process_params_events": [],
        "ntcreate_events": [],
        "spawn_events": [],
        "unix_loader_events": [],
        "heap_events": [],
        "unixlib_events": [],
        "win32u_events": [],
        "userdriver_events": [],
        "explorer_events": [],
        "explorer_main_events": [],
        "wineboot_events": [],
        "services_events": [],
        "ldrdisable_events": [],
        "ldrthunk_events": [],
        "ntdll_env_nls_events": [],
        "android_linker_events": [],
        "unknown_freewine_events": [],
        "autotouch_files": [],
        "autotouch_exec_files": [],
        "freewine_prefix_counts": {},
        "autotouch_module_counts": {},
        "autotouch_exec_module_counts": {},
        "unknown_freewine_prefix_counts": {},
        "first_failure": None,
    }
    seen_imports: set[tuple[str, str, str]] = set()
    seen_failure_keys: set[tuple[str, str, str, str]] = set()

    def remember_failure(kind: str, module: str, status: str, source: str, lineno: int, detail: str) -> None:
        if state["first_failure"] is not None or status_is_zero(status):
            return
        state["first_failure"] = {
            "kind": kind,
            "module": module,
            "status": status,
            "source": source,
            "lineno": str(lineno),
            "detail": detail,
        }

    for path in files:
        text = read_text(path)
        source = str(path.relative_to(scenario_dir))
        for lineno, line in enumerate(text.splitlines(), start=1):
            if prefix_match := FREEWINE_PREFIX_RE.search(line):
                prefix_counter[prefix_match.group("prefix")] += 1

            if match := FREEWINE_LAUNCHER_RE.search(line):
                event = {
                    "phase": match.group("phase"),
                    "path": (match.group("path") or "").strip(),
                    "name": (match.group("name") or "").strip(),
                    "ptr": (match.group("ptr") or "").strip(),
                    "error": (match.group("error") or "").strip(),
                    "source": source,
                    "lineno": str(lineno),
                    "detail": line.strip(),
                }
                state["launcher_events"].append(event)
                if event["phase"] in {"dlopen_fail", "dlsym_fail", "load_ntdll_fail", "wine_main_missing"}:
                    remember_failure(
                        f"launcher_{event['phase']}",
                        event["path"] or event["name"] or "launcher",
                        "failed",
                        source,
                        lineno,
                        line.strip(),
                    )
                continue

            if match := FREEWINE_START_STEP_RE.search(line):
                step = match.group("step")
                detail = (match.group("detail") or "").strip()
                append_unique(state["start_steps"], compact_freewine_detail(step, detail))
                continue

            if match := FREEWINE_BOOTSTRAP_SCOPE_RE.search(line):
                scope = match.group("scope")
                phase = match.group("phase")
                detail = (match.group("detail") or "").strip()
                event = {
                    "scope": scope,
                    "phase": phase,
                    "detail": detail,
                    "source": source,
                    "lineno": str(lineno),
                }
                state["bootstrap_scope_events"].append(event)

                if scope == "autotouch":
                    if file_match := FREEWINE_BOOTSTRAP_FILE_RE.search(detail):
                        file_path = file_match.group("file").strip()
                        if phase == "tu_exec":
                            append_unique(state["autotouch_exec_files"], file_path)
                            autotouch_exec_module_counter[classify_autotouch_file(file_path)] += 1
                        else:
                            append_unique(state["autotouch_files"], file_path)
                            autotouch_module_counter[classify_autotouch_file(file_path)] += 1
                    continue

                if scope == "kernelbase":
                    state["kernelbase_events"].append({**event, "name": "", "value": "", "status": ""})
                    continue
                if scope in {"kernel32", "kernel32_process_attach"}:
                    state["kernel32_events"].append({**event, "name": "", "value": "", "status": ""})
                    continue
                if scope == "user32":
                    state["user32_events"].append({**event, "name": "", "value": "", "detail_name": "", "status": ""})
                    continue
                if scope in {"wineserver", "ntdll_unix_server"}:
                    state["server_events"].append({**event, "name": "", "value": "", "status": ""})
                    continue
                if scope == "win32u":
                    state["win32u_events"].append({**event, "module": "win32u.dll", "name": "", "ptr": "", "status": ""})
                    continue
                if scope == "wineandroid_drv":
                    state["userdriver_events"].append({**event, "driver": "wineandroid.drv", "name": "", "value": "", "status": ""})
                    continue
                if scope in {"explorer", "explorer_desktop"}:
                    state["explorer_events"].append({**event, "name": "explorer.exe", "value": "", "status": ""})
                    continue
                if scope == "wineboot":
                    state["wineboot_events"].append(
                        {
                            "detail": compact_freewine_detail(phase, detail),
                            "source": source,
                            "lineno": str(lineno),
                        }
                    )
                    continue
                if scope in {"services", "services_rpc", "rpcss", "winedevice"}:
                    state["services_events"].append(
                        {
                            "detail": compact_freewine_detail(scope, phase, detail),
                            "source": source,
                            "lineno": str(lineno),
                        }
                    )
                    continue
                continue

            if match := FREEWINE_LOADER_INIT_RE.search(line):
                append_unique(state["loader_init_steps"], match.group("step").strip())
                continue

            if match := FREEWINE_LOADER_INIT_STATE_RE.search(line):
                state["loader_init_states"].append(
                    {
                        "step": match.group("step"),
                        "key": match.group("key"),
                        "value": match.group("value"),
                        "source": source,
                        "lineno": str(lineno),
                    }
                )
                continue

            if match := FREEWINE_LOADER_INIT_STATUS_RE.search(line):
                event = {
                    "step": match.group("step"),
                    "status": match.group("status"),
                    "source": source,
                    "lineno": str(lineno),
                }
                state["loader_init_statuses"].append(event)
                if not status_is_zero(event["status"]):
                    remember_failure(
                        "loader_init_status",
                        event["step"],
                        event["status"],
                        source,
                        lineno,
                        line.strip(),
                    )
                continue

            if match := FREEWINE_IMPORT_RE.search(line):
                importer = normalize_module_token(match.group("importer"))
                import_dll = match.group("import_dll")
                symbol = match.group("symbol") or f"ordinal:{match.group('ordinal')}"
                stub = match.group("stub")
                key = (importer, import_dll, symbol)
                if key not in seen_imports:
                    seen_imports.add(key)
                    state["critical_import_stubs"].append(
                        {
                            "importer": importer,
                            "import_dll": import_dll,
                            "symbol": symbol,
                            "stub": stub,
                            "source": source,
                            "lineno": str(lineno),
                        }
                    )
                continue

            if match := FREEWINE_PROCESS_ATTACH_RE.search(line):
                phase = match.group("phase")
                module = normalize_module_token(match.group("module"))
                status = match.group("status")
                if phase == "begin":
                    append_unique(state["attach_begin_order"], module)
                if phase in {"dependency-failure", "init-failure"}:
                    key = (phase, module, status, source)
                    if key not in seen_failure_keys:
                        seen_failure_keys.add(key)
                        state["process_attach_failures"].append(
                            {
                                "phase": phase,
                                "module": module,
                                "status": status,
                                "source": source,
                                "lineno": str(lineno),
                                "detail": line.strip(),
                            }
                        )
                    if phase == "dependency-failure":
                        append_unique(state["dependency_cascade"], module)
                    remember_failure(
                        f"process_attach_{phase.replace('-', '_')}",
                        module,
                        status,
                        source,
                        lineno,
                        line.strip(),
                    )
                continue

            if match := FREEWINE_MODULE_INIT_RE.search(line):
                phase = match.group("phase")
                module = normalize_module_token(match.group("module"))
                reason = match.group("reason")
                status = match.group("status")
                if phase in {"exception", "failure"}:
                    key = (phase, module, status, source)
                    if key not in seen_failure_keys:
                        seen_failure_keys.add(key)
                        state["module_init_failures"].append(
                            {
                                "phase": phase,
                                "module": module,
                                "reason": reason,
                                "status": status,
                                "source": source,
                                "lineno": str(lineno),
                                "detail": line.strip(),
                            }
                        )
                    remember_failure(
                        f"module_init_{phase}",
                        module,
                        status,
                        source,
                        lineno,
                        line.strip(),
                    )
                continue

            if match := FREEWINE_LOADER_STATUS_RE.search(line):
                tag = match.group("tag")
                module = normalize_module_token(match.group("module"))
                status = match.group("status")
                state["loader_statuses"].append(
                    {
                        "tag": tag,
                        "module": module,
                        "status": status,
                        "source": source,
                        "lineno": str(lineno),
                    }
                )
                if tag in {"loader_init_last_failed_modref", "loader_init_kernel32", "build_module_fixup_imports"}:
                    remember_failure(f"loader_status_{tag}", module, status, source, lineno, line.strip())
                continue

            if match := FREEWINE_LOADER_LOAD_DLL_RE.search(line):
                event = {
                    "lib": normalize_module_token(match.group("lib")),
                    "load_path": normalize_module_token(match.group("load_path")),
                    "status": match.group("status"),
                    "source": source,
                    "lineno": str(lineno),
                    "detail": line.strip(),
                }
                state["loader_load_dll_events"].append(event)
                if not status_is_zero(event["status"]):
                    remember_failure(
                        "loader_load_dll",
                        event["lib"],
                        event["status"],
                        source,
                        lineno,
                        line.strip(),
                    )
                continue

            if match := FREEWINE_UNIXLIB_PHASE_RE.search(line):
                event = {
                    "phase": match.group("phase"),
                    "module": (match.group("module") or "").strip(),
                    "path": (match.group("path") or "").strip(),
                    "symbol": (match.group("symbol") or "").strip(),
                    "handle": (match.group("handle") or "").strip(),
                    "funcs": (match.group("funcs") or "").strip(),
                    "status": (match.group("status") or "").strip(),
                    "error": (match.group("error") or "").strip(),
                    "source": source,
                    "lineno": str(lineno),
                    "detail": line.strip(),
                }
                state["unixlib_events"].append(event)
                if event["phase"] in {"dlopen_fail", "dlsym_fail"}:
                    remember_failure(
                        f"unixlib_{event['phase']}",
                        event["path"] or event["module"] or "unixlib",
                        event["status"] or "unknown",
                        source,
                        lineno,
                        line.strip(),
                    )
                continue

            if match := FREEWINE_WIN32U_RE.search(line):
                event = {
                    "phase": match.group("phase"),
                    "module": normalize_module_token(match.group("module")),
                    "name": (match.group("name") or "").strip(),
                    "ptr": (match.group("ptr") or "").strip(),
                    "status": match.group("status"),
                    "source": source,
                    "lineno": str(lineno),
                    "detail": line.strip(),
                }
                state["win32u_events"].append(event)
                if not status_is_zero(event["status"]):
                    remember_failure(
                        f"win32u_{event['phase']}",
                        event["module"],
                        event["status"],
                        source,
                        lineno,
                        line.strip(),
                    )
                continue

            if match := FREEWINE_KERNELBASE_RE.search(line):
                event = {
                    "phase": match.group("phase"),
                    "name": (match.group("name") or "").strip(),
                    "value": (match.group("value") or "").strip(),
                    "status": match.group("status"),
                    "source": source,
                    "lineno": str(lineno),
                    "detail": line.strip(),
                }
                state["kernelbase_events"].append(event)
                if not status_is_zero(event["status"]):
                    remember_failure(
                        f"kernelbase_{event['phase']}",
                        "kernelbase.dll",
                        event["status"],
                        source,
                        lineno,
                        line.strip(),
                    )
                continue

            if match := FREEWINE_KERNEL32_RE.search(line):
                event = {
                    "phase": match.group("phase"),
                    "name": (match.group("name") or "").strip(),
                    "value": (match.group("value") or "").strip(),
                    "status": match.group("status"),
                    "source": source,
                    "lineno": str(lineno),
                    "detail": line.strip(),
                }
                state["kernel32_events"].append(event)
                if not status_is_zero(event["status"]):
                    remember_failure(
                        f"kernel32_{event['phase']}",
                        "kernel32.dll",
                        event["status"],
                        source,
                        lineno,
                        line.strip(),
                    )
                continue

            if match := FREEWINE_USER32_RE.search(line):
                event = {
                    "phase": match.group("phase"),
                    "name": (match.group("name") or "").strip(),
                    "value": (match.group("value") or "").strip(),
                    "detail_name": normalize_module_token(match.group("detail") or ""),
                    "status": match.group("status"),
                    "source": source,
                    "lineno": str(lineno),
                    "detail": line.strip(),
                }
                state["user32_events"].append(event)
                if not status_is_zero(event["status"]):
                    remember_failure(
                        f"user32_{event['phase']}",
                        "user32.dll",
                        event["status"],
                        source,
                        lineno,
                        line.strip(),
                    )
                continue

            if match := FREEWINE_SERVER_RE.search(line):
                event = {
                    "phase": match.group("phase"),
                    "name": (match.group("name") or "").strip(),
                    "value": (match.group("value") or "").strip(),
                    "status": match.group("status"),
                    "source": source,
                    "lineno": str(lineno),
                    "detail": line.strip(),
                }
                state["server_events"].append(event)
                if not status_is_zero(event["status"]):
                    remember_failure(
                        f"server_{event['phase']}",
                        "wineserver",
                        event["status"],
                        source,
                        lineno,
                        line.strip(),
                    )
                continue

            if match := FREEWINE_USERDRIVER_RE.search(line):
                event = {
                    "phase": match.group("phase"),
                    "driver": normalize_module_token(match.group("driver") or ""),
                    "name": (match.group("name") or "").strip(),
                    "value": (match.group("value") or "").strip(),
                    "status": match.group("status"),
                    "source": source,
                    "lineno": str(lineno),
                    "detail": line.strip(),
                }
                state["userdriver_events"].append(event)
                if not status_is_zero(event["status"]):
                    remember_failure(
                        f"userdriver_{event['phase']}",
                        event["driver"] or "userdriver",
                        event["status"],
                        source,
                        lineno,
                        line.strip(),
                    )
                continue

            if match := FREEWINE_EXPLORER_RE.search(line):
                event = {
                    "phase": match.group("phase"),
                    "name": normalize_module_token(match.group("name") or ""),
                    "value": (match.group("value") or "").strip(),
                    "status": match.group("status"),
                    "source": source,
                    "lineno": str(lineno),
                    "detail": line.strip(),
                }
                state["explorer_events"].append(event)
                if not status_is_zero(event["status"]):
                    remember_failure(
                        f"explorer_{event['phase']}",
                        event["name"] or "explorer.exe",
                        event["status"],
                        source,
                        lineno,
                        line.strip(),
                    )
                continue

            if match := FREEWINE_EXPLORER_MAIN_RE.search(line):
                event = {
                    "phase": match.group("phase"),
                    "name": normalize_module_token(match.group("name") or ""),
                    "value": (match.group("value") or "").strip(),
                    "status": match.group("status"),
                    "source": source,
                    "lineno": str(lineno),
                    "detail": line.strip(),
                }
                state["explorer_main_events"].append(event)
                if not status_is_zero(event["status"]):
                    remember_failure(
                        f"explorer_main_{event['phase']}",
                        event["name"] or "explorer.exe",
                        event["status"],
                        source,
                        lineno,
                        line.strip(),
                    )
                continue

            if match := FREEWINE_PROCESS_PARAMS_RE.search(line):
                event = {
                    "phase": match.group("phase"),
                    "key": (match.group("key") or "").strip(),
                    "value": (match.group("value") or "").strip(),
                    "status": (match.group("status") or "").strip(),
                    "source": source,
                    "lineno": str(lineno),
                    "detail": line.strip(),
                }
                state["process_params_events"].append(event)
                if event["status"] and not status_is_zero(event["status"]):
                    remember_failure(
                        f"process_params_{event['phase']}",
                        "process_params",
                        event["status"],
                        source,
                        lineno,
                        line.strip(),
                    )
                continue

            if match := FREEWINE_NTCREATE_RE.search(line):
                event = {
                    "phase": match.group("phase"),
                    "key": (match.group("key") or "").strip(),
                    "value": (match.group("value") or "").strip(),
                    "status": (match.group("status") or "").strip(),
                    "source": source,
                    "lineno": str(lineno),
                    "detail": line.strip(),
                }
                state["ntcreate_events"].append(event)
                if event["status"] and not status_is_zero(event["status"]):
                    remember_failure(
                        f"ntcreate_{event['phase']}",
                        "ntcreate",
                        event["status"],
                        source,
                        lineno,
                        line.strip(),
                    )
                continue

            if match := FREEWINE_SPAWN_RE.search(line):
                state["spawn_events"].append(
                    {
                        "phase": match.group("phase"),
                        "key": match.group("key"),
                        "value": match.group("value"),
                        "source": source,
                        "lineno": str(lineno),
                        "detail": line.strip(),
                    }
                )
                continue

            if match := FREEWINE_UNIX_LOADER_RE.search(line):
                state["unix_loader_events"].append(
                    {
                        "phase": match.group("phase"),
                        "source": source,
                        "lineno": str(lineno),
                        "detail": line.strip(),
                    }
                )
                continue

            if match := FREEWINE_WINEBOOT_RE.search(line):
                state["wineboot_events"].append(
                    {
                        "detail": match.group("detail").strip(),
                        "source": source,
                        "lineno": str(lineno),
                    }
                )
                continue

            if match := FREEWINE_SERVICES_RE.search(line):
                state["services_events"].append(
                    {
                        "detail": match.group("detail").strip(),
                        "source": source,
                        "lineno": str(lineno),
                    }
                )
                continue

            if match := FREEWINE_LDRDISABLE_RE.search(line):
                state["ldrdisable_events"].append(
                    {
                        "detail": match.group("detail").strip(),
                        "source": source,
                        "lineno": str(lineno),
                    }
                )
                continue

            if match := FREEWINE_LDRTHUNK_RE.search(line):
                state["ldrthunk_events"].append(
                    {
                        "detail": match.group("detail").strip(),
                        "source": source,
                        "lineno": str(lineno),
                    }
                )
                continue

            if match := FREEWINE_NTDLL_ENV_NLS_RE.search(line):
                state["ntdll_env_nls_events"].append(
                    {
                        "detail": match.group("detail").strip(),
                        "source": source,
                        "lineno": str(lineno),
                    }
                )
                continue

            if match := FREEWINE_HEAP_INVALID_RE.search(line):
                state["heap_events"].append(
                    {
                        "kind": "invalid_handle",
                        "phase": match.group("phase"),
                        "handle": match.group("handle"),
                        "process_heap": match.group("process_heap"),
                        "process_params": match.group("process_params"),
                        "source": source,
                        "lineno": str(lineno),
                        "detail": line.strip(),
                    }
                )
                remember_failure(
                    "heap_invalid_handle",
                    "process_heap",
                    "invalid_handle",
                    source,
                    lineno,
                    line.strip(),
                )
                continue

            if match := FREEWINE_HEAP_CREATE_RE.search(line):
                state["heap_events"].append(
                    {
                        "kind": "create",
                        "phase": match.group("phase"),
                        "flags": match.group("flags"),
                        "heap": match.group("heap"),
                        "process_heap_static": match.group("process_heap_static"),
                        "peb_process_heap": match.group("peb_process_heap"),
                        "source": source,
                        "lineno": str(lineno),
                        "detail": line.strip(),
                    }
                )
                continue

            if match := FREEWINE_HEAP_OP_RE.search(line):
                event = {
                    "kind": "op",
                    "op": match.group("op"),
                    "handle": match.group("handle"),
                    "flags": match.group("flags"),
                    "key": match.group("key"),
                    "value": match.group("value"),
                    "result": match.group("result"),
                    "process_heap_static": match.group("process_heap_static"),
                    "peb_process_heap": match.group("peb_process_heap"),
                    "process_params": match.group("process_params"),
                    "caller": match.group("caller"),
                    "source": source,
                    "lineno": str(lineno),
                    "detail": line.strip(),
                }
                state["heap_events"].append(event)
                if event["op"] == "allocate_invalid_handle":
                    remember_failure(
                        "heap_allocate_invalid_handle",
                        "process_heap",
                        "invalid_handle",
                        source,
                        lineno,
                        line.strip(),
                    )
                continue

            if RAW_HEAP_INVALID_RE.search(line):
                remember_failure(
                    "heap_invalid_handle_raw",
                    "process_heap",
                    "invalid_handle",
                    source,
                    lineno,
                    line.strip(),
                )
                continue

            if match := ANDROID_LINKER_RE.search(line):
                message = match.group("message").strip()
                if "dlopen(" in message or "dlsym(" in message or "dlerror set to " in message:
                    state["android_linker_events"].append(
                        {
                            "message": message,
                            "source": source,
                            "lineno": str(lineno),
                        }
                    )
                continue

            if match := FREEWINE_PREFIX_RE.search(line):
                unknown_prefix_counter[match.group("prefix")] += 1
                state["unknown_freewine_events"].append(
                    {
                        "prefix": match.group("prefix"),
                        "detail": match.group("detail").strip(),
                        "source": source,
                        "lineno": str(lineno),
                    }
                )
                continue

    state["freewine_prefix_counts"] = dict(prefix_counter)
    state["autotouch_module_counts"] = dict(autotouch_module_counter)
    state["autotouch_exec_module_counts"] = dict(autotouch_exec_module_counter)
    state["unknown_freewine_prefix_counts"] = dict(unknown_prefix_counter)
    return state


def synthesize_freewine_issues(
    scenario_dir: Path,
    rows: list[dict[str, str]],
    bootstrap: dict[str, object],
) -> list[dict[str, str]]:
    seen = {
        (row["category"], row["severity"], row["library"], row["event_id"], row["detail"])
        for row in rows
    }

    def add_issue(source: str, lineno: str, category: str, severity: str, library: str, detail: str) -> None:
        key = (category, severity, library, "", detail)
        if key in seen:
            return
        seen.add(key)
        rows.append(
            {
                "source": source,
                "lineno": lineno,
                "category": category,
                "severity": severity,
                "library": library,
                "event_id": "",
                "stage": "freewine_bootstrap",
                "detail": detail,
            }
        )

    first_failure = bootstrap.get("first_failure")
    if isinstance(first_failure, dict):
        add_issue(
            str(first_failure.get("source", "")),
            str(first_failure.get("lineno", "")),
            "freewine_bootstrap_first_failure",
            "high",
            str(first_failure.get("module", "")) or "freewine_bootstrap",
            compact_freewine_detail(
                str(first_failure.get("kind", "")),
                f"status={first_failure.get('status', '')}",
                str(first_failure.get("detail", "")),
            ),
        )

    for item in bootstrap.get("module_init_failures", [])[:12]:
        add_issue(
            item["source"],
            item["lineno"],
            "freewine_module_init_failure",
            "high",
            item["module"],
            compact_freewine_detail(item["phase"], item["reason"], f"status={item['status']}", item["detail"]),
        )

    for item in bootstrap.get("process_attach_failures", [])[:12]:
        add_issue(
            item["source"],
            item["lineno"],
            "freewine_process_attach_failure",
            "high",
            item["module"],
            compact_freewine_detail(item["phase"], f"status={item['status']}", item["detail"]),
        )

    loader_statuses = [
        item for item in bootstrap.get("loader_statuses", [])
        if item.get("tag") in {"loader_init_last_failed_modref", "loader_init_kernel32", "build_module_fixup_imports"}
        and not status_is_zero(str(item.get("status", "")))
    ]
    for item in loader_statuses[:8]:
        add_issue(
            item["source"],
            item["lineno"],
            "freewine_loader_status_failure",
            "high",
            item["module"],
            compact_freewine_detail(item["tag"], f"status={item['status']}"),
        )

    for item in bootstrap.get("loader_load_dll_events", [])[:20]:
        status = str(item.get("status", ""))
        if status and not status_is_zero(status):
            add_issue(
                item["source"],
                item["lineno"],
                "freewine_loader_load_dll_failure",
                "high",
                str(item.get("lib", "")) or "loader",
                compact_freewine_detail(
                    str(item.get("lib", "")),
                    str(item.get("load_path", "")),
                    f"status={status}",
                ),
            )

    heap_failures: list[dict[str, str]] = []
    for item in bootstrap.get("heap_events", []):
        if item.get("kind") == "invalid_handle":
            heap_failures.append(item)
            continue
        if item.get("kind") == "op" and item.get("op") == "allocate_invalid_handle":
            heap_failures.append(item)

    for item in heap_failures[:8]:
        add_issue(
            item["source"],
            item["lineno"],
            "freewine_heap_invalid_handle",
            "high",
            "process_heap",
            compact_freewine_detail(
                item.get("phase") or item.get("op", ""),
                f"handle={item.get('handle', '')}",
                f"process_heap={item.get('process_heap', item.get('process_heap_static', ''))}",
                f"process_params={item.get('process_params', '')}",
            ),
        )

    for item in bootstrap.get("launcher_events", [])[:20]:
        phase = item.get("phase", "")
        if phase in {"dlopen_fail", "dlsym_fail", "load_ntdll_fail", "wine_main_missing"}:
            add_issue(
                item["source"],
                item["lineno"],
                "freewine_launcher_failure",
                "high",
                item.get("path") or item.get("name") or "launcher",
                compact_freewine_detail(
                    phase,
                    item.get("path", ""),
                    item.get("name", ""),
                    item.get("error", ""),
                ),
            )

    for bucket, category, library in (
        ("kernelbase_events", "freewine_kernelbase_failure", "kernelbase.dll"),
        ("kernel32_events", "freewine_kernel32_failure", "kernel32.dll"),
        ("user32_events", "freewine_user32_failure", "user32.dll"),
        ("server_events", "freewine_server_failure", "wineserver"),
    ):
        for item in bootstrap.get(bucket, [])[:20]:
            status = str(item.get("status", ""))
            if status and not status_is_zero(status):
                add_issue(
                    item["source"],
                    item["lineno"],
                    category,
                    "high",
                    library,
                    compact_freewine_detail(
                        str(item.get("phase", "")),
                        str(item.get("name", "")),
                        str(item.get("detail_name", "")),
                        f"status={status}",
                    ),
                )

    for item in bootstrap.get("userdriver_events", [])[:24]:
        phase = str(item.get("phase", ""))
        driver = str(item.get("driver", "")) or "userdriver"
        status = str(item.get("status", ""))
        if status and not status_is_zero(status):
            add_issue(
                item["source"],
                item["lineno"],
                "freewine_userdriver_failure",
                "high",
                driver,
                compact_freewine_detail(phase, f"status={status}", str(item.get("name", ""))),
            )
        elif phase in {"set_null_driver_from_registry", "driver_error_registry", "load_display_driver_fallback"}:
            add_issue(
                item["source"],
                item["lineno"],
                "freewine_userdriver_fallback",
                "medium",
                driver,
                compact_freewine_detail(phase, str(item.get("name", "")), str(item.get("value", ""))),
            )

    for bucket, category in (
        ("explorer_main_events", "freewine_explorer_main_failure"),
        ("explorer_events", "freewine_explorer_failure"),
    ):
        for item in bootstrap.get(bucket, [])[:24]:
            phase = str(item.get("phase", ""))
            status = str(item.get("status", ""))
            library = str(item.get("name", "")) or "explorer.exe"
            if status and not status_is_zero(status):
                add_issue(
                    item["source"],
                    item["lineno"],
                    category,
                    "high",
                    library,
                    compact_freewine_detail(phase, f"status={status}", str(item.get("value", ""))),
                )
            elif phase in {"load_graphics_driver_fail", "select_null_graphics_driver"}:
                add_issue(
                    item["source"],
                    item["lineno"],
                    "freewine_explorer_graphics_fallback",
                    "medium",
                    library,
                    compact_freewine_detail(phase, str(item.get("value", ""))),
                )

    for bucket, category, library in (
        ("wineboot_events", "freewine_wineboot_failure", "wineboot.exe"),
        ("services_events", "freewine_services_failure", "services.exe"),
    ):
        for item in bootstrap.get(bucket, [])[:24]:
            detail = str(item.get("detail", ""))
            lowered = detail.lower()
            if ("failed" in lowered or "exited code=" in lowered or " err=" in lowered) and not lowered.endswith("err=0"):
                add_issue(
                    item["source"],
                    item["lineno"],
                    category,
                    "high",
                    library,
                    detail,
                )

    for item in bootstrap.get("android_linker_events", [])[:24]:
        message = str(item.get("message", ""))
        if "dlerror set to" in message.lower():
            add_issue(
                item["source"],
                item["lineno"],
                "android_linker_dlerror",
                "high",
                "android_linker",
                message,
            )

    for item in bootstrap.get("unknown_freewine_events", [])[:12]:
        add_issue(
            item["source"],
            item["lineno"],
            "freewine_parser_gap",
            "medium",
            f"freewine-{item['prefix']}",
            str(item.get("detail", "")),
        )

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


def synthesize_wait_process_issues(
    scenario_dir: Path,
    wait_meta: dict[str, str],
    rows: list[dict[str, str]],
    files: list[Path],
) -> list[dict[str, str]]:
    process_meta = read_keyvals(scenario_dir / "process-emergence.env")
    seen = {
        (row["category"], row["severity"], row["library"], row["event_id"], row["detail"])
        for row in rows
    }
    runtime_text = "\n".join(read_text(path) for path in files).lower()

    def add_issue(category: str, severity: str, library: str, detail: str) -> None:
        key = (category, severity, library, "", detail)
        if key in seen:
            return
        seen.add(key)
        rows.append(
            {
                "source": "process-emergence.env",
                "lineno": "1",
                "category": category,
                "severity": severity,
                "library": library,
                "event_id": "",
                "stage": "runtime_forensics",
                "detail": detail,
            }
        )

    wine_present = process_meta.get("wine_process_present", "0") == "1"
    saw_submit = wait_meta.get("saw_submit", "0")
    saw_terminal = wait_meta.get("saw_terminal", "0")
    wineboot_started = (
        "run_wineboot ntcreateuserprocess status=00000000" in runtime_text
        or "run_wineboot resumed thread=" in runtime_text
    )
    desktop_shell_submit = "launch_exec_submit" in runtime_text and "explorer \\/desktop=shell" in runtime_text
    preloader_shown = "xserver_bootstrap_preloader_show" in runtime_text
    false_visual_ready_without_window = (
        '"event_id":"xserver_guest_visual_ready"' in runtime_text
        and '"reason":"desktop_shell_process_proof"' in runtime_text
        and '"tracked_window_count":0' in runtime_text
    )
    false_preloader_fallback_exec = (
        '"event_id":"xserver_bootstrap_preloader_fallback_exec"' in runtime_text
        and '"tracked_window_count":0' in runtime_text
        and '"desktop_shell_launch_mode":"direct_explorer"' in runtime_text
    )
    preloader_ready_proof = (
        "xserver_app_window_mapped" in runtime_text
        or "preloader_map_fallback" in runtime_text
        or ("xserver_guest_visual_ready" in runtime_text and not false_visual_ready_without_window)
        or ("xserver_bootstrap_preloader_fallback_exec" in runtime_text and not false_preloader_fallback_exec)
    )

    if wine_present and saw_submit == "0":
        add_issue(
            "forensic_submit_marker_missing",
            "low",
            "forensic_wait_contract",
            "wine/wineserver emerged but wait-status never observed a submit marker; launch trace missed the start edge",
        )

    if wine_present and saw_submit == "0" and saw_terminal == "0":
        detail = "wine/wineserver emerged but wait-status never observed a terminal marker"
        if wineboot_started:
            detail += "; runtime logs show wineboot process creation/resume so bootstrap progressed past early wineserver startup"
        add_issue(
            "runtime_bootstrap_no_terminal",
            "medium",
            "wine_bootstrap",
            detail,
        )

    if (
        wine_present
        and saw_submit == "1"
        and saw_terminal == "0"
        and desktop_shell_submit
        and preloader_shown
        and not preloader_ready_proof
    ):
        detail = (
            "desktop-shell bootstrap was submitted and wine/wineserver emerged, "
            "but no mapped-window or preloader-dismiss proof was observed; "
            "the Starting up preloader can remain visible indefinitely"
        )
        if "wineboot.exe" in runtime_text:
            detail += "; wineboot.exe was still present during the forensic window"
        add_issue(
            "desktop_shell_handoff_stalled",
            "high",
            "xserver_desktop_shell",
            detail,
        )

    if false_visual_ready_without_window:
        add_issue(
            "desktop_shell_false_ready_without_window",
            "high",
            "xserver_desktop_shell",
            "desktop-shell bootstrap declared guest_visual_ready from process proof while tracked_window_count stayed 0; direct explorer route advanced without any mapped desktop window",
        )

    return rows


def write_tsv(path: Path, rows: list[dict[str, str]]) -> None:
    headers = ["source", "lineno", "category", "severity", "library", "event_id", "stage", "detail"]
    with path.open("w", encoding="utf-8") as fh:
        fh.write("\t".join(headers) + "\n")
        for row in rows:
            fh.write("\t".join(row.get(key, "").replace("\t", " ").replace("\n", " ") for key in headers) + "\n")


def write_json(
    path: Path,
    scenario_meta: dict[str, str],
    wait_meta: dict[str, str],
    rows: list[dict[str, str]],
    events: list[dict[str, str]],
    bootstrap: dict[str, object],
) -> None:
    payload = {
        "scenario": scenario_meta,
        "wait_status": wait_meta,
        "issue_count": len(rows),
        "issues": rows,
        "forensic_events_tail": events[-40:],
        "freewine_bootstrap": bootstrap,
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


def write_bootstrap_summary(path: Path, bootstrap: dict[str, object]) -> None:
    first_failure = bootstrap.get("first_failure") or {}
    critical_imports = bootstrap.get("critical_import_stubs", [])
    scope_counts = Counter(str(item.get("scope", "")) for item in bootstrap.get("bootstrap_scope_events", []))
    prefix_counts = Counter({str(key): int(value) for key, value in dict(bootstrap.get("freewine_prefix_counts", {})).items()})
    autotouch_module_counts = Counter({str(key): int(value) for key, value in dict(bootstrap.get("autotouch_module_counts", {})).items()})
    autotouch_exec_module_counts = Counter({str(key): int(value) for key, value in dict(bootstrap.get("autotouch_exec_module_counts", {})).items()})
    unknown_prefix_counts = Counter({str(key): int(value) for key, value in dict(bootstrap.get("unknown_freewine_prefix_counts", {})).items()})
    import_summary = []
    scope_summary = []
    prefix_summary = []
    autotouch_module_summary = []
    autotouch_exec_module_summary = []
    unknown_prefix_summary = []
    for item in critical_imports[:12]:
        import_summary.append(f"{item['importer']}->{item['import_dll']}:{item['symbol']}")
    for key in sorted(scope_counts):
        if key:
            scope_summary.append(f"{key}:{scope_counts[key]}")
    for key, count in prefix_counts.most_common(24):
        if key:
            prefix_summary.append(f"{key}:{count}")
    for key, count in autotouch_module_counts.most_common(24):
        if key:
            autotouch_module_summary.append(f"{key}:{count}")
    for key, count in autotouch_exec_module_counts.most_common(24):
        if key:
            autotouch_exec_module_summary.append(f"{key}:{count}")
    for key, count in unknown_prefix_counts.most_common(24):
        if key:
            unknown_prefix_summary.append(f"{key}:{count}")
    lines = [
        "freewine_bootstrap_summary",
        "start_steps=" + ",".join(bootstrap.get("start_steps", [])) if bootstrap.get("start_steps") else "start_steps=none",
        "bootstrap_scope_summary=" + ",".join(scope_summary) if scope_summary else "bootstrap_scope_summary=none",
        "freewine_prefix_summary=" + ",".join(prefix_summary) if prefix_summary else "freewine_prefix_summary=none",
        "autotouch_module_summary=" + ",".join(autotouch_module_summary) if autotouch_module_summary else "autotouch_module_summary=none",
        "autotouch_exec_module_summary=" + ",".join(autotouch_exec_module_summary) if autotouch_exec_module_summary else "autotouch_exec_module_summary=none",
        "unknown_freewine_prefix_summary=" + ",".join(unknown_prefix_summary) if unknown_prefix_summary else "unknown_freewine_prefix_summary=none",
        "loader_init_steps=" + ",".join(bootstrap.get("loader_init_steps", [])) if bootstrap.get("loader_init_steps") else "loader_init_steps=none",
        "attach_begin_order=" + " > ".join(bootstrap.get("attach_begin_order", [])) if bootstrap.get("attach_begin_order") else "attach_begin_order=none",
        "dependency_cascade=" + " > ".join(bootstrap.get("dependency_cascade", [])) if bootstrap.get("dependency_cascade") else "dependency_cascade=none",
        f"first_failure_kind={first_failure.get('kind', 'none')}",
        f"first_failure_module={first_failure.get('module', 'none')}",
        f"first_failure_status={first_failure.get('status', 'none')}",
        f"first_failure_source={first_failure.get('source', 'none')}:{first_failure.get('lineno', '')}",
        f"loader_init_state_count={len(bootstrap.get('loader_init_states', []))}",
        f"loader_init_status_count={len(bootstrap.get('loader_init_statuses', []))}",
        f"bootstrap_scope_event_count={len(bootstrap.get('bootstrap_scope_events', []))}",
        f"autotouch_file_count={len(bootstrap.get('autotouch_files', []))}",
        f"autotouch_exec_file_count={len(bootstrap.get('autotouch_exec_files', []))}",
        f"loader_load_dll_event_count={len(bootstrap.get('loader_load_dll_events', []))}",
        f"process_params_event_count={len(bootstrap.get('process_params_events', []))}",
        f"ntcreate_event_count={len(bootstrap.get('ntcreate_events', []))}",
        f"spawn_event_count={len(bootstrap.get('spawn_events', []))}",
        f"unix_loader_event_count={len(bootstrap.get('unix_loader_events', []))}",
        f"heap_event_count={len(bootstrap.get('heap_events', []))}",
        f"launcher_event_count={len(bootstrap.get('launcher_events', []))}",
        f"kernelbase_event_count={len(bootstrap.get('kernelbase_events', []))}",
        f"kernel32_event_count={len(bootstrap.get('kernel32_events', []))}",
        f"user32_event_count={len(bootstrap.get('user32_events', []))}",
        f"server_event_count={len(bootstrap.get('server_events', []))}",
        f"win32u_event_count={len(bootstrap.get('win32u_events', []))}",
        f"userdriver_event_count={len(bootstrap.get('userdriver_events', []))}",
        f"explorer_event_count={len(bootstrap.get('explorer_events', []))}",
        f"explorer_main_event_count={len(bootstrap.get('explorer_main_events', []))}",
        f"wineboot_event_count={len(bootstrap.get('wineboot_events', []))}",
        f"services_event_count={len(bootstrap.get('services_events', []))}",
        f"ldrdisable_event_count={len(bootstrap.get('ldrdisable_events', []))}",
        f"ldrthunk_event_count={len(bootstrap.get('ldrthunk_events', []))}",
        f"ntdll_env_nls_event_count={len(bootstrap.get('ntdll_env_nls_events', []))}",
        f"android_linker_event_count={len(bootstrap.get('android_linker_events', []))}",
        f"unixlib_event_count={len(bootstrap.get('unixlib_events', []))}",
        f"unknown_freewine_event_count={len(bootstrap.get('unknown_freewine_events', []))}",
        "critical_import_stubs=" + ";".join(import_summary) if import_summary else "critical_import_stubs=none",
    ]
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def write_bootstrap_markdown(path: Path, bootstrap: dict[str, object]) -> None:
    scope_counts = Counter(str(item.get("scope", "")) for item in bootstrap.get("bootstrap_scope_events", []))
    prefix_counts = Counter({str(key): int(value) for key, value in dict(bootstrap.get("freewine_prefix_counts", {})).items()})
    autotouch_module_counts = Counter({str(key): int(value) for key, value in dict(bootstrap.get("autotouch_module_counts", {})).items()})
    autotouch_exec_module_counts = Counter({str(key): int(value) for key, value in dict(bootstrap.get("autotouch_exec_module_counts", {})).items()})
    unknown_prefix_counts = Counter({str(key): int(value) for key, value in dict(bootstrap.get("unknown_freewine_prefix_counts", {})).items()})
    lines = [
        "# FreeWine Bootstrap Frontier",
        "",
        "## First Failure",
        "",
    ]
    first_failure = bootstrap.get("first_failure") or {}
    if not first_failure:
        lines.append("- none")
    else:
        lines.append(
            f"- `{first_failure.get('kind', '')}` `{first_failure.get('module', '')}` "
            f"`{first_failure.get('status', '')}` from `{first_failure.get('source', '')}:{first_failure.get('lineno', '')}`"
        )
        detail = str(first_failure.get("detail", "")).strip()
        if detail:
            lines.append(f"- detail: {detail}")

    lines.extend(
        [
            "",
            "## Attach Order",
            "",
            "- " + " -> ".join(bootstrap.get("attach_begin_order", []))
            if bootstrap.get("attach_begin_order")
            else "- none",
            "",
            "## Dependency Cascade",
            "",
            "- " + " -> ".join(bootstrap.get("dependency_cascade", []))
            if bootstrap.get("dependency_cascade")
            else "- none",
            "",
            "## FreeWine Prefix Coverage",
            "",
        ]
    )
    if not prefix_counts:
        lines.append("- none")
    else:
        for prefix, count in prefix_counts.most_common(40):
            lines.append(f"- `{prefix}`: `{count}`")

    lines.extend(
        [
            "",
            "## Bootstrap Scope Coverage",
            "",
        ]
    )
    if not scope_counts:
        lines.append("- none")
    else:
        for scope in sorted(scope_counts):
            if not scope:
                continue
            lines.append(f"- `{scope}`: `{scope_counts[scope]}`")

    lines.extend(
        [
            "",
            "## Loader Init Boundary Events",
            "",
        ]
    )
    loader_init_states = bootstrap.get("loader_init_states", [])
    if not loader_init_states:
        lines.append("- none")
    else:
        for item in loader_init_states[:24]:
            lines.append(
                f"- `{item['step']}` `{item['key']}={item['value']}` from `{item['source']}:{item['lineno']}`"
            )

    lines.extend(
        [
            "",
            "## Autotouch Module Coverage",
            "",
        ]
    )
    if not autotouch_module_counts:
        lines.append("- none")
    else:
        for module, count in autotouch_module_counts.most_common(40):
            lines.append(f"- `{module}`: `{count}`")

    lines.extend(
        [
            "",
            "## Whole-Tree Autotouch Files",
            "",
        ]
    )
    autotouch_files = bootstrap.get("autotouch_files", [])
    if not autotouch_files:
        lines.append("- none")
    else:
        for file in autotouch_files[:120]:
            lines.append(f"- `{file}`")

    lines.extend(
        [
            "",
            "## Whole-Tree Autotouch Execution Coverage",
            "",
        ]
    )
    if not autotouch_exec_module_counts:
        lines.append("- none")
    else:
        for module, count in autotouch_exec_module_counts.most_common(40):
            lines.append(f"- `{module}`: `{count}`")

    lines.extend(
        [
            "",
            "## Whole-Tree Autotouch Execution Files",
            "",
        ]
    )
    autotouch_exec_files = bootstrap.get("autotouch_exec_files", [])
    if not autotouch_exec_files:
        lines.append("- none")
    else:
        for file in autotouch_exec_files[:120]:
            lines.append(f"- `{file}`")

    lines.extend(
        [
            "",
            "## Loader Load-Dll Events",
            "",
        ]
    )
    loader_load_dll_events = bootstrap.get("loader_load_dll_events", [])
    if not loader_load_dll_events:
        lines.append("- none")
    else:
        for item in loader_load_dll_events[:20]:
            lines.append(
                f"- `lib={item['lib']}` `load_path={item['load_path']}` `status={item['status']}` "
                f"from `{item['source']}:{item['lineno']}`"
            )

    lines.extend(
        [
            "",
            "## Process Params Events",
            "",
        ]
    )
    process_params_events = bootstrap.get("process_params_events", [])
    if not process_params_events:
        lines.append("- none")
    else:
        for item in process_params_events[:24]:
            payload = f"{item['key']}={item['value']}" if item["key"] else f"status={item['status']}"
            lines.append(
                f"- `{item['phase']}` {payload} from `{item['source']}:{item['lineno']}`"
            )

    lines.extend(
        [
            "",
            "## Launcher + Kernel Events",
            "",
        ]
    )
    launcher_events = bootstrap.get("launcher_events", [])
    kernelbase_events = bootstrap.get("kernelbase_events", [])
    kernel32_events = bootstrap.get("kernel32_events", [])
    user32_events = bootstrap.get("user32_events", [])
    server_events = bootstrap.get("server_events", [])
    if not launcher_events and not kernelbase_events and not kernel32_events and not user32_events and not server_events:
        lines.append("- none")
    else:
        for item in launcher_events[:16]:
            lines.append(
                f"- `launcher:{item['phase']}`"
                + (f" path=`{item['path']}`" if item["path"] else "")
                + (f" name=`{item['name']}`" if item["name"] else "")
                + (f" error=`{item['error']}`" if item["error"] else "")
                + f" from `{item['source']}:{item['lineno']}`"
            )
        for item in kernelbase_events[:12]:
            lines.append(
                f"- `kernelbase:{item['phase']}` status=`{item['status']}`"
                + (f" {item['name']}=`{item['value']}`" if item["name"] else "")
                + f" from `{item['source']}:{item['lineno']}`"
            )
        for item in kernel32_events[:12]:
            lines.append(
                f"- `kernel32:{item['phase']}` status=`{item['status']}`"
                + (f" {item['name']}=`{item['value']}`" if item["name"] else "")
                + f" from `{item['source']}:{item['lineno']}`"
            )
        for item in user32_events[:16]:
            lines.append(
                f"- `user32:{item['phase']}` status=`{item['status']}`"
                + (f" {item['name']}=`{item['value']}`" if item["name"] else "")
                + (f" detail=`{item['detail_name']}`" if item["detail_name"] else "")
                + f" from `{item['source']}:{item['lineno']}`"
            )
        for item in server_events[:16]:
            lines.append(
                f"- `server:{item['phase']}` status=`{item['status']}`"
                + (f" {item['name']}=`{item['value']}`" if item["name"] else "")
                + f" from `{item['source']}:{item['lineno']}`"
            )

    lines.extend(
        [
            "",
            "## NtCreate + Spawn Events",
            "",
        ]
    )
    ntcreate_events = bootstrap.get("ntcreate_events", [])
    spawn_events = bootstrap.get("spawn_events", [])
    if not ntcreate_events and not spawn_events:
        lines.append("- none")
    else:
        for item in ntcreate_events[:20]:
            payload = f"{item['key']}={item['value']}" if item["key"] else f"status={item['status']}"
            lines.append(
                f"- `ntcreate:{item['phase']}` {payload} from `{item['source']}:{item['lineno']}`"
            )
        for item in spawn_events[:12]:
            lines.append(
                f"- `spawn:{item['phase']}` `{item['key']}={item['value']}` from `{item['source']}:{item['lineno']}`"
            )

    lines.extend(
        [
            "",
            "## Heap Events",
            "",
        ]
    )
    heap_events = bootstrap.get("heap_events", [])
    if not heap_events:
        lines.append("- none")
    else:
        for item in heap_events[:12]:
            if item.get("kind") == "invalid_handle":
                lines.append(
                    f"- `invalid_handle/{item['phase']}` handle=`{item['handle']}` "
                    f"process_heap=`{item['process_heap']}` process_params=`{item['process_params']}` "
                    f"from `{item['source']}:{item['lineno']}`"
                )
            elif item.get("kind") == "create":
                lines.append(
                    f"- `create/{item['phase']}` flags=`{item['flags']}` heap=`{item['heap']}` "
                    f"process_heap_static=`{item['process_heap_static']}` "
                    f"peb_process_heap=`{item['peb_process_heap']}` "
                    f"from `{item['source']}:{item['lineno']}`"
                )
            else:
                lines.append(
                    f"- `op/{item['op']}` handle=`{item['handle']}` {item['key']}=`{item['value']}` "
                    f"result=`{item['result']}` process_params=`{item['process_params']}` "
                    f"from `{item['source']}:{item['lineno']}`"
                )

    lines.extend(
        [
            "",
            "## Explorer + Driver Events",
            "",
        ]
    )
    userdriver_events = bootstrap.get("userdriver_events", [])
    explorer_main_events = bootstrap.get("explorer_main_events", [])
    explorer_events = bootstrap.get("explorer_events", [])
    if not userdriver_events and not explorer_main_events and not explorer_events:
        lines.append("- none")
    else:
        for item in userdriver_events[:16]:
            lines.append(
                f"- `userdriver:{item['phase']}` status=`{item['status']}`"
                + (f" driver=`{item['driver']}`" if item["driver"] else "")
                + (f" {item['name']}=`{item['value']}`" if item["name"] else "")
                + f" from `{item['source']}:{item['lineno']}`"
            )
        for item in explorer_main_events[:12]:
            lines.append(
                f"- `explorer-main:{item['phase']}` status=`{item['status']}`"
                + (f" name=`{item['name']}`" if item["name"] else "")
                + (f" value=`{item['value']}`" if item["value"] else "")
                + f" from `{item['source']}:{item['lineno']}`"
            )
        for item in explorer_events[:16]:
            lines.append(
                f"- `explorer:{item['phase']}` status=`{item['status']}`"
                + (f" name=`{item['name']}`" if item["name"] else "")
                + (f" value=`{item['value']}`" if item["value"] else "")
                + f" from `{item['source']}:{item['lineno']}`"
            )

    lines.extend(
        [
            "",
            "## Win32u Events",
            "",
        ]
    )
    win32u_events = bootstrap.get("win32u_events", [])
    if not win32u_events:
        lines.append("- none")
    else:
        for item in win32u_events[:20]:
            lines.append(
                f"- `{item['phase']}` status=`{item['status']}`"
                + (f" name=`{item['name']}`" if item["name"] else "")
                + (f" ptr=`{item['ptr']}`" if item["ptr"] else "")
                + f" from `{item['source']}:{item['lineno']}`"
            )

    lines.extend(
        [
            "",
            "## Unixlib Events",
            "",
        ]
    )
    unixlib_events = bootstrap.get("unixlib_events", [])
    if not unixlib_events:
        lines.append("- none")
    else:
        for item in unixlib_events[:20]:
            lines.append(
                f"- `{item['phase']}`"
                + (f" path=`{item['path']}`" if item["path"] else "")
                + (f" symbol=`{item['symbol']}`" if item["symbol"] else "")
                + (f" status=`{item['status']}`" if item["status"] else "")
                + (f" error=`{item['error']}`" if item["error"] else "")
                + f" from `{item['source']}:{item['lineno']}`"
            )

    lines.extend(
        [
            "",
            "## Wineboot + Services Events",
            "",
        ]
    )
    wineboot_events = bootstrap.get("wineboot_events", [])
    services_events = bootstrap.get("services_events", [])
    if not wineboot_events and not services_events:
        lines.append("- none")
    else:
        for item in wineboot_events[:20]:
            lines.append(
                f"- `wineboot` {item['detail']} from `{item['source']}:{item['lineno']}`"
            )
        for item in services_events[:20]:
            lines.append(
                f"- `services` {item['detail']} from `{item['source']}:{item['lineno']}`"
            )

    lines.extend(
        [
            "",
            "## Ldr Thunk + NLS Events",
            "",
        ]
    )
    ldrdisable_events = bootstrap.get("ldrdisable_events", [])
    ldrthunk_events = bootstrap.get("ldrthunk_events", [])
    ntdll_env_nls_events = bootstrap.get("ntdll_env_nls_events", [])
    if not ldrdisable_events and not ldrthunk_events and not ntdll_env_nls_events:
        lines.append("- none")
    else:
        for item in ldrdisable_events[:12]:
            lines.append(
                f"- `ldrdisable` {item['detail']} from `{item['source']}:{item['lineno']}`"
            )
        for item in ldrthunk_events[:12]:
            lines.append(
                f"- `ldrthunk` {item['detail']} from `{item['source']}:{item['lineno']}`"
            )
        for item in ntdll_env_nls_events[:12]:
            lines.append(
                f"- `ntdll-env-nls` {item['detail']} from `{item['source']}:{item['lineno']}`"
            )

    lines.extend(
        [
            "",
            "## Android Linker Events",
            "",
        ]
    )
    android_linker_events = bootstrap.get("android_linker_events", [])
    if not android_linker_events:
        lines.append("- none")
    else:
        for item in android_linker_events[:20]:
            lines.append(
                f"- `{item['message']}` from `{item['source']}:{item['lineno']}`"
            )

    lines.extend(
        [
            "",
            "## Import Stub Frontier",
            "",
        ]
    )
    imports = bootstrap.get("critical_import_stubs", [])
    if not imports:
        lines.append("- none")
    else:
        for item in imports[:20]:
            lines.append(
                f"- `{item['importer']}` imports `{item['symbol']}` from `{item['import_dll']}` -> stub `{item['stub']}`"
            )

    lines.extend(
        [
            "",
            "## Unknown FreeWine Events",
            "",
        ]
    )
    unknown_freewine_events = bootstrap.get("unknown_freewine_events", [])
    if not unknown_freewine_events:
        lines.append("- none")
    else:
        if unknown_prefix_counts:
            lines.append(
                "- prefix-summary: "
                + ", ".join(f"`{prefix}:{count}`" for prefix, count in unknown_prefix_counts.most_common(20))
            )
        for item in unknown_freewine_events[:20]:
            lines.append(
                f"- `freewine-{item['prefix']}` {item['detail']} from `{item['source']}:{item['lineno']}`"
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
    bootstrap = parse_freewine_bootstrap(files, scenario_dir)
    rows = synthesize_freewine_issues(scenario_dir, rows, bootstrap)
    rows = synthesize_wait_process_issues(scenario_dir, wait_meta, rows, files)

    write_tsv(prefix.with_suffix(".tsv"), rows)
    write_json(prefix.with_suffix(".json"), scenario_meta, wait_meta, rows, events, bootstrap)
    write_summary(prefix.with_suffix(".summary.txt"), rows)
    write_markdown(prefix.with_suffix(".md"), scenario_meta, wait_meta, rows, events)
    write_bootstrap_summary(prefix.with_suffix(".bootstrap-summary.txt"), bootstrap)
    write_bootstrap_markdown(prefix.with_suffix(".bootstrap.md"), bootstrap)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
