#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"

: "${WLT_PACKAGE:=com.winlator.cmod}"
: "${WLT_APK_PATH:=${ROOT_DIR}/app/build/outputs/apk/debug/app-debug.apk}"
: "${WLT_GRAPHICS_ASSET_DIR:=${ROOT_DIR}/app/src/main/assets/graphics_driver}"
: "${WLT_BUILD_JOBS:=8}"
: "${WLT_INSTALL_APK:=1}"
: "${WLT_SEED_CONTAINER_ID:=2}"
: "${WLT_VORTEK_CONTAINER_ID:=5}"
: "${WLT_GLADIO_CONTAINER_ID:=6}"
: "${WLT_BOOTSTRAP_WAIT_SEC:=45}"
: "${WLT_DEVICE_WAIT_AFTER_INSTALL_SEC:=8}"
: "${WLT_DEVICE_WAIT_AFTER_MAIN_SEC:=12}"
: "${WLT_ROUTE_WAIT_INTENT_TIMEOUT_SEC:=25}"
: "${WLT_ROUTE_WAIT_POST_INTENT_TIMEOUT_SEC:=25}"
: "${WLT_OUT_DIR:=${ROOT_DIR}/out/winlator-aemali-batch-$(date +%Y%m%d_%H%M%S)}"

log() { printf '[aemali-batch] %s\n' "$*"; }
fail() { printf '[aemali-batch][error] %s\n' "$*" >&2; exit 1; }
require_cmd() { command -v "$1" >/dev/null 2>&1 || fail "required command not found: $1"; }

pick_serial() {
  local serial
  serial="${ADB_SERIAL:-}"
  if [[ -n "${serial}" ]]; then
    printf '%s\n' "${serial}"
    return 0
  fi
  adb devices | awk 'NR>1 && $2=="device" {print $1; exit}'
}

adb_s() { adb -s "${ADB_SERIAL_PICKED}" "$@"; }

ensure_host_requirements() {
  require_cmd adb
  require_cmd bash
  require_cmd python3
  require_cmd jq
  require_cmd sha256sum
}

select_asset_json() {
  python3 - "${WLT_GRAPHICS_ASSET_DIR}" <<'PY'
import json
import os
import re
import sys
from pathlib import Path

asset_dir = Path(sys.argv[1])
if not asset_dir.is_dir():
    raise SystemExit("asset dir missing: %s" % asset_dir)

prefixes = ("aemali-panvk", "aemali-gallium", "vortek", "gladio")
rows = {}
for prefix in prefixes:
    best = None
    pattern = re.compile(rf"^{re.escape(prefix)}-(.+)\.tzst$")
    for path in asset_dir.iterdir():
        if not path.is_file():
            continue
        match = pattern.match(path.name)
        if not match:
            continue
        row = {
            "file": path.name,
            "path": str(path),
            "version": match.group(1),
            "mtime": path.stat().st_mtime,
        }
        if best is None or row["mtime"] > best["mtime"]:
            best = row
    if best is None:
        raise SystemExit(f"missing asset for prefix: {prefix}")
    rows[prefix] = best

print(json.dumps(rows, ensure_ascii=True))
PY
}

extract_asset_field() {
  local json="$1"
  local prefix="$2"
  local field="$3"
  jq -r --arg prefix "${prefix}" --arg field "${field}" '.[$prefix][$field]' <<< "${json}"
}

run_build_wave() {
  local out_dir="$1"
  mkdir -p "${out_dir}"
  log "build wave -> ${out_dir}"
  (
    cd "${ROOT_DIR}"
    {
      printf 'started_at=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
      printf 'jobs=%s\n' "${WLT_BUILD_JOBS}"
      env INSTALL_APP_ASSET=1 JOBS="${WLT_BUILD_JOBS}" ACTION=all \
        ./tools/build-mesa-staging-graphics.sh panvk-android
      env INSTALL_APP_ASSET=1 JOBS="${WLT_BUILD_JOBS}" ACTION=all \
        ./tools/build-mesa-staging-graphics.sh mali-gallium-android
      ls -l app/src/main/assets/graphics_driver/aemali-*.tzst
      sha256sum app/src/main/assets/graphics_driver/aemali-*.tzst
      . ./tools/env-android-local.sh
      ./gradlew --no-daemon assembleDebug
      ls -l "${WLT_APK_PATH}"
      sha256sum "${WLT_APK_PATH}"
      printf 'finished_at=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    } > >(tee -a "${out_dir}/build.log") 2>&1
  )
}

install_apk_if_requested() {
  if [[ "${WLT_INSTALL_APK}" != "1" ]]; then
    log "skip apk install"
    return 0
  fi
  [[ -f "${WLT_APK_PATH}" ]] || fail "apk missing: ${WLT_APK_PATH}"
  log "install apk -> ${ADB_SERIAL_PICKED}"
  adb_s install -r -d "${WLT_APK_PATH}" > "${WLT_OUT_DIR}/apk-install.txt" 2>&1 \
    || fail "adb install failed (see ${WLT_OUT_DIR}/apk-install.txt)"
  sleep "${WLT_DEVICE_WAIT_AFTER_INSTALL_SEC}"
}

wait_for_seed_container() {
  local seed_path="files/imagefs/home/xuser-${WLT_SEED_CONTAINER_ID}/.container"
  local deadline=$(( $(date +%s) + WLT_BOOTSTRAP_WAIT_SEC ))

  adb_s shell am start -n "${WLT_PACKAGE}/.MainActivity" >/dev/null 2>&1 || true
  sleep "${WLT_DEVICE_WAIT_AFTER_MAIN_SEC}"

  while (( $(date +%s) < deadline )); do
    if adb_s exec-out run-as "${WLT_PACKAGE}" sh -c "test -f ${seed_path}" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  fail "seed container missing after bootstrap wait: ${seed_path}"
}

seed_wrapper_container() {
  local target_id="$1"
  local target_name="$2"
  local graphics_driver="$3"
  local routing_mode="$4"
  local tmp_json="${WLT_OUT_DIR}/container-${target_id}.json"

  adb_s shell "run-as ${WLT_PACKAGE} sh -c 'set -e; src=\"files/imagefs/home/xuser-${WLT_SEED_CONTAINER_ID}\"; dst=\"files/imagefs/home/xuser-${target_id}\"; rm -rf \"\$dst\"; cp -a \"\$src\" \"\$dst\"'" \
    >/dev/null || fail "failed to seed xuser-${target_id} from xuser-${WLT_SEED_CONTAINER_ID}"

  adb_s exec-out run-as "${WLT_PACKAGE}" sh -c "cat files/imagefs/home/xuser-${target_id}/.container" > "${tmp_json}" \
    || fail "failed to read seeded container xuser-${target_id}"

  python3 - "${tmp_json}" "${target_id}" "${target_name}" "${graphics_driver}" "${routing_mode}" \
    "${AEMALI_PANVK_VERSION}" "${AEMALI_GALLIUM_VERSION}" "${VORTEK_VERSION}" <<'PY'
import json
import sys
from collections import OrderedDict
from pathlib import Path

json_path = Path(sys.argv[1])
target_id = int(sys.argv[2])
target_name = sys.argv[3]
graphics_driver = sys.argv[4]
routing_mode = sys.argv[5]
panvk_version = sys.argv[6]
gallium_version = sys.argv[7]
vortek_version = sys.argv[8]

obj = json.loads(json_path.read_text(encoding="utf-8"))
obj["id"] = target_id
obj["name"] = target_name
obj["graphicsDriver"] = graphics_driver

config = OrderedDict()
config["vulkanDriverEntry"] = f"aemali-panvk:{panvk_version}"
config["vortekPackageVersion"] = vortek_version
config["gladioPackageVersion"] = f"aemali-gallium:{gallium_version}"
config["routingMode"] = routing_mode
config["extensionProfile"] = "mali-system"
config["gladioNoError"] = "1"
obj["graphicsDriverConfig"] = ",".join(f"{k}={v}" for k, v in config.items())

json_path.write_text(json.dumps(obj, ensure_ascii=True, separators=(",", ":")) + "\n", encoding="utf-8")
PY

  adb_s shell "run-as ${WLT_PACKAGE} sh -c 'cat > files/imagefs/home/xuser-${target_id}/.container'" < "${tmp_json}" \
    || fail "failed to write xuser-${target_id}/.container"
}

capture_device_graphics_truth() {
  adb_s shell 'ls -l /dev/dri/render* 2>/dev/null || true; ls -l /dev/kgsl-3d0 2>/dev/null || true; getprop ro.hardware.vulkan 2>/dev/null; getprop ro.board.platform 2>/dev/null; getprop ro.hardware.egl 2>/dev/null' \
    > "${WLT_OUT_DIR}/device-graphics-truth.txt" 2>&1 || true
}

capture_imported_package_truth() {
  local panvk_dir="files/contents/vortek_vulkan_driver/aemali-panvk-${AEMALI_PANVK_VERSION}"
  local gallium_dir="files/contents/gladio_opengl_driver/aemali-gallium-${AEMALI_GALLIUM_VERSION}"

  adb_s exec-out run-as "${WLT_PACKAGE}" sh -c "set -e; cd ${panvk_dir}; /system/bin/ls -1; printf '\n===== meta.json =====\n'; cat meta.json; printf '\n===== profile.json =====\n'; cat profile.json" \
    > "${WLT_OUT_DIR}/aemali-panvk-import.txt" 2>&1 || true
  adb_s exec-out run-as "${WLT_PACKAGE}" sh -c "set -e; cd ${gallium_dir}; /system/bin/ls -1; printf '\n===== meta.json =====\n'; cat meta.json; printf '\n===== profile.json =====\n'; cat profile.json" \
    > "${WLT_OUT_DIR}/aemali-gallium-import.txt" 2>&1 || true
}

collect_trace_rows() {
  local label="$1"
  local trace_id="$2"
  local out_jsonl="${WLT_OUT_DIR}/${label}.trace.jsonl"
  adb_s exec-out run-as "${WLT_PACKAGE}" sh -c "for f in files/Winlator/logs/forensics/*.jsonl; do [ -f \"\$f\" ] || continue; grep '\"trace_id\":\"${trace_id}\"' \"\$f\" || true; done" \
    > "${out_jsonl}" 2>/dev/null || true
}

build_trace_summary() {
  python3 - "${WLT_OUT_DIR}" <<'PY'
import json
import sys
from pathlib import Path

root = Path(sys.argv[1])
summary = {}
for label in ("aemali-vortek", "aemali-gladio"):
    path = root / f"{label}.trace.jsonl"
    rows = []
    if path.is_file():
        for line in path.read_text(encoding="utf-8", errors="ignore").splitlines():
            line = line.strip()
            if not line:
                continue
            try:
                rows.append(json.loads(line))
            except json.JSONDecodeError:
                continue

    def last(event_id):
        for row in reversed(rows):
            if row.get("event_id") == event_id:
                return row
        return {}

    route = last("GRAPHICS_ROUTE_APPLIED")
    submit = last("LAUNCH_EXEC_SUBMIT")
    exit_row = last("LAUNCH_EXEC_EXIT")
    intent = last("ROUTE_INTENT_RECEIVED")
    summary[label] = {
        "trace_file": str(path),
        "route_event_present": bool(route),
        "submit_event_present": bool(submit),
        "exit_event_present": bool(exit_row),
        "intent_event_present": bool(intent),
        "graphics_driver": route.get("graphics_driver", ""),
        "driver_id": route.get("driver_id", ""),
        "selected_driver_entry": route.get("selected_driver_entry", ""),
        "active_provider_lane": route.get("active_provider_lane", ""),
        "active_provider_package": route.get("active_provider_package", ""),
        "active_provider_version": route.get("active_provider_version", ""),
        "companion_provider_lane": route.get("companion_provider_lane", ""),
        "route_degraded_reason": route.get("route_degraded_reason", ""),
        "opengl_overlay_active": route.get("opengl_overlay_active", ""),
        "vulkan_runtime_source": route.get("vulkan_runtime_source", ""),
        "vulkan_wrapper_icd": route.get("vulkan_wrapper_icd", ""),
        "vulkan_wrapper_api_max": route.get("vulkan_wrapper_api_max", ""),
        "guest_executable": submit.get("guest_executable", ""),
        "command": submit.get("command", ""),
        "exit_status": exit_row.get("status", ""),
    }

(root / "trace-summary.json").write_text(json.dumps(summary, ensure_ascii=True, indent=2) + "\n", encoding="utf-8")
with (root / "trace-summary.txt").open("w", encoding="utf-8") as fh:
    for label, row in summary.items():
        fh.write(f"[{label}]\n")
        for key in (
            "route_event_present",
            "intent_event_present",
            "submit_event_present",
            "exit_event_present",
            "graphics_driver",
            "driver_id",
            "selected_driver_entry",
            "active_provider_lane",
            "active_provider_package",
            "active_provider_version",
            "companion_provider_lane",
            "route_degraded_reason",
            "opengl_overlay_active",
            "vulkan_runtime_source",
            "vulkan_wrapper_api_max",
            "exit_status",
        ):
            fh.write(f"{key}={row.get(key, '')}\n")
        fh.write("\n")
PY
}

run_device_proof() {
  local proof_dir="${WLT_OUT_DIR}/forensics"
  capture_device_graphics_truth
  seed_wrapper_container "${WLT_VORTEK_CONTAINER_ID}" "AeMali Vortek" "vortek" "vulkan-first"
  seed_wrapper_container "${WLT_GLADIO_CONTAINER_ID}" "AeMali Gladio" "gladio" "opengl-first"
  WLT_OUT_DIR="${proof_dir}" \
  WLT_SCENARIOS="aemali-vortek:${WLT_VORTEK_CONTAINER_ID} aemali-gladio:${WLT_GLADIO_CONTAINER_ID}" \
  WLT_WAIT_INTENT_TIMEOUT_SEC="${WLT_ROUTE_WAIT_INTENT_TIMEOUT_SEC}" \
  WLT_WAIT_POST_INTENT_TIMEOUT_SEC="${WLT_ROUTE_WAIT_POST_INTENT_TIMEOUT_SEC}" \
  WLT_CAPTURE_RUNAS_PTRACE=0 \
  WLT_RUNAS_PTRACE_SYMBOLIZE=0 \
  bash "${ROOT_DIR}/ci/winlator/forensic-adb-complete-matrix.sh"

  collect_trace_rows "aemali-vortek" "$(cat "${proof_dir}/aemali-vortek/trace_id.txt")"
  collect_trace_rows "aemali-gladio" "$(cat "${proof_dir}/aemali-gladio/trace_id.txt")"
  capture_imported_package_truth
  build_trace_summary
}

main() {
  ensure_host_requirements
  mkdir -p "${WLT_OUT_DIR}"

  ADB_SERIAL_PICKED="$(pick_serial)"
  [[ -n "${ADB_SERIAL_PICKED}" ]] || fail "no active adb device"
  export ADB_SERIAL_PICKED

  run_build_wave "${WLT_OUT_DIR}/build"

  ASSET_JSON="$(select_asset_json)"
  export ASSET_JSON
  AEMALI_PANVK_VERSION="$(extract_asset_field "${ASSET_JSON}" "aemali-panvk" "version")"
  AEMALI_GALLIUM_VERSION="$(extract_asset_field "${ASSET_JSON}" "aemali-gallium" "version")"
  VORTEK_VERSION="$(extract_asset_field "${ASSET_JSON}" "vortek" "version")"
  GLADIO_VERSION="$(extract_asset_field "${ASSET_JSON}" "gladio" "version")"
  export AEMALI_PANVK_VERSION AEMALI_GALLIUM_VERSION VORTEK_VERSION GLADIO_VERSION

  printf '%s\n' "${ASSET_JSON}" > "${WLT_OUT_DIR}/asset-selection.json"
  install_apk_if_requested
  wait_for_seed_container
  run_device_proof

  {
    printf 'serial=%s\n' "${ADB_SERIAL_PICKED}"
    printf 'apk_path=%s\n' "${WLT_APK_PATH}"
    printf 'aemali_panvk_version=%s\n' "${AEMALI_PANVK_VERSION}"
    printf 'aemali_gallium_version=%s\n' "${AEMALI_GALLIUM_VERSION}"
    printf 'vortek_version=%s\n' "${VORTEK_VERSION}"
    printf 'gladio_version=%s\n' "${GLADIO_VERSION}"
    printf 'time=%s\n' "$(date -Is)"
  } > "${WLT_OUT_DIR}/session-meta.txt"

  log "done -> ${WLT_OUT_DIR}"
}

main "$@"
