#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"

log() { printf '[runtime-log-assembler-selftest] %s\n' "$*"; }
fail() { printf '[runtime-log-assembler-selftest][error] %s\n' "$*" >&2; exit 1; }

tmp_dir="$(mktemp -d /tmp/runtime_log_assembler_selftest_XXXXXX)"
cleanup() { rm -rf "${tmp_dir}"; }
trap cleanup EXIT

scenario_dir="${tmp_dir}/protonwine10"
mkdir -p "${scenario_dir}/runtime-logs"

cat > "${scenario_dir}/scenario_meta.txt" <<'EOF'
label=protonwine10
container_id=2
EOF

cat > "${scenario_dir}/wait-status.txt" <<'EOF'
trace_id=trace-protonwine10
elapsed_sec=5
saw_intent=1
saw_submit=1
saw_terminal=0
EOF

cat > "${scenario_dir}/trace_id.txt" <<'EOF'
trace-protonwine10
EOF

cat > "${scenario_dir}/logcat-full.txt" <<'EOF'
03-01 02:00:00.000 1000 1000 I ForensicLogger: {"ts":"2026-03-01T02:00:00.000+0000","event_id":"RUNTIME_LIBRARY_COMPONENT_CONFLICT","severity":"warn","trace_id":"trace-protonwine10","stage":"graphics_driver","message":"runtime library component conflict","component":"translator","state":"fexcore","expected":"libwow64fex.dll"}
03-01 02:00:00.100 1000 1000 I ForensicLogger: {"ts":"2026-03-01T02:00:00.100+0000","event_id":"LAUNCH_EXEC_EXIT","severity":"info","trace_id":"trace-protonwine10","stage":"launcher","message":"guest program launcher exited","exit_code":1}
03-01 02:00:00.200 1000 1000 E linker64: CANNOT LINK EXECUTABLE "wine": library "libmissing.so" not found
03-01 02:00:00.300 1000 1000 F vkBasalt: ../src/reshade/effect_preprocessor.cpp:117: bool reshadefx::preprocessor::append_file(const string&): assertion "!path.empty()" failed
EOF

cat > "${scenario_dir}/logcat-filtered.txt" <<'EOF'
AERO_UPSCALE_VKBASALT_REASON=fsr_assert_guard
EOF

cat > "${scenario_dir}/forensics-jsonl-tail.txt" <<'EOF'
{"ts":"2026-03-01T02:00:00.400+0000","event_id":"RUNTIME_LIBRARY_CONFLICT_DETECTED","severity":"warn","trace_id":"trace-protonwine10","stage":"graphics_driver","message":"runtime library conflict detected: dxvk_artifact_source_unset","conflict":"dxvk_artifact_source_unset"}
{"ts":"2026-03-01T02:00:00.500+0000","event_id":"TURNIP_SOURCE_FAILED","severity":"error","trace_id":"trace-protonwine10","stage":"diagnostics_ui","message":"turnip source request failed","error_detail":"curl 404"}
{"ts":"2026-03-01T02:00:00.600+0000","event_id":"RUNTIME_LOADER_TRACE_CONTRACT_SNAPSHOT","severity":"info","trace_id":"trace-protonwine10","stage":"graphics_driver","message":"runtime loader trace contract snapshot prepared","loader_trace_effective":"1","loader_trace_mode":"wine:loaddll,module"}
EOF

cat > "${scenario_dir}/runtime-logs/wfm_2026-03-01_02-00-00.txt" <<'EOF'
wine: could not load libexample.so
err:module:import_dll Library api-ms-win-crt-runtime-l1-1-0.dll which is needed by L"Z:\\test.exe" not found
err:module:attach_process_dlls L"broken.dll" failed to initialize, aborting
trace:module:load_so_dll failed to load libvulkan_missing.so
trace:module:import_dll unresolved import D3D12CoreCreateDevice
undefined symbol: dxvk_submit_frame
EOF

python3 "${ROOT_DIR}/ci/winlator/forensic-runtime-log-assembler.py" \
  --input "${scenario_dir}" \
  --output-prefix "${scenario_dir}/runtime-log-assembler"

python3 - "${scenario_dir}/runtime-log-assembler.json" "${scenario_dir}/runtime-log-assembler.summary.txt" <<'PY'
import json
import sys
from pathlib import Path

payload = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
summary = Path(sys.argv[2]).read_text(encoding="utf-8")
rows = payload["issues"]
categories = {row["category"] for row in rows}
libraries = {row["library"] for row in rows}

required_categories = {
    "runtime_component_conflict",
    "runtime_library_conflict",
    "turnip_source_failed",
    "launch_exit_nonzero",
    "module_not_found",
    "assertion_failed",
    "vkbasalt_guard",
    "import_dll_missing",
    "wine_loader_init_failed",
    "wine_loader_missing_module",
    "wine_loader_unresolved_import",
    "undefined_symbol",
}
missing = required_categories - categories
assert not missing, f"missing categories: {sorted(missing)}"
assert "translator" in libraries
assert "dxvk" in libraries
assert "turnip" in libraries
assert "vkbasalt/reshade" in libraries or "vkbasalt" in libraries
assert "issue_count=" in summary
assert "max_severity=high" in summary
assert "vkbasalt_guard:1" in summary
assert "runtime_component_conflict:1" in summary
PY

log "runtime log assembler selftest passed"
