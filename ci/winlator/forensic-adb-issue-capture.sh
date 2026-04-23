#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
: "${ADB:=adb}"
: "${WLT_PACKAGE:=com.winlator.cmod}"
: "${WLT_LABEL:=forensic-issue}"
: "${WLT_BUNDLE_DIR:=${ROOT_DIR}/out/adb-issues}"
: "${WLT_PREFER_ROOT:=0}"
: "${WLT_SERIAL:=}"
: "${WLT_GH_CREATE_ISSUE:=0}"
: "${WLT_GH_REPO:=${GITHUB_REPOSITORY:-}}"
: "${WLT_RUNTIME_LOG_ROOTS:=/sdcard/Ae.solator/logs /storage/emulated/0/Ae.solator/logs /sdcard/Winlator/logs}"
: "${WLT_APP_PRIVATE_RUNTIME_LOG_ROOT:=./files/Winlator/logs}"
: "${WLT_CAPTURE_LINKER_TELEMETRY:=1}"
: "${WLT_LINKER_DEBUG_FLAGS:=dlopen,dlsym,dlerror}"
: "${WLT_ADB_HOST_TIMEOUT_SEC:=12}"
WLT_APP_DATA_DIR=""
WLT_APP_PRIVATE_RUNTIME_LOG_ROOT_ABS=""

usage() {
  cat <<USAGE
Usage: $0 [--serial SERIAL] [--package PKG] [--bundle-dir DIR] [--label NAME] [--prefer-root] [--create-issue] [--repo owner/repo]
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --serial) WLT_SERIAL="$2"; shift 2 ;;
    --package) WLT_PACKAGE="$2"; shift 2 ;;
    --bundle-dir) WLT_BUNDLE_DIR="$2"; shift 2 ;;
    --label) WLT_LABEL="$2"; shift 2 ;;
    --prefer-root) WLT_PREFER_ROOT=1; shift ;;
    --create-issue) WLT_GH_CREATE_ISSUE=1; shift ;;
    --repo) WLT_GH_REPO="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown arg: $1" >&2; usage >&2; exit 2 ;;
  esac
done

adb_cmd() {
  if [[ -n "${WLT_SERIAL}" ]]; then
    "${ADB}" -s "${WLT_SERIAL}" "$@"
  else
    "${ADB}" "$@"
  fi
}

adb_capture_timeout() {
  local out_file="$1"
  shift
  if command -v timeout >/dev/null 2>&1; then
    if [[ -n "${WLT_SERIAL}" ]]; then
      timeout --foreground "${WLT_ADB_HOST_TIMEOUT_SEC}" "${ADB}" -s "${WLT_SERIAL}" "$@" \
        < /dev/null >"${out_file}" 2>&1 || true
    else
      timeout --foreground "${WLT_ADB_HOST_TIMEOUT_SEC}" "${ADB}" "$@" \
        < /dev/null >"${out_file}" 2>&1 || true
    fi
  else
    adb_cmd "$@" < /dev/null >"${out_file}" 2>&1 || true
  fi
}

adb_pull_timeout() {
  local remote="$1"
  local local_path="$2"
  if command -v timeout >/dev/null 2>&1; then
    if [[ -n "${WLT_SERIAL}" ]]; then
      timeout --foreground "${WLT_ADB_HOST_TIMEOUT_SEC}" "${ADB}" -s "${WLT_SERIAL}" pull "${remote}" "${local_path}" \
        < /dev/null >/dev/null 2>&1 || true
    else
      timeout --foreground "${WLT_ADB_HOST_TIMEOUT_SEC}" "${ADB}" pull "${remote}" "${local_path}" \
        < /dev/null >/dev/null 2>&1 || true
    fi
  else
    adb_cmd pull "${remote}" "${local_path}" < /dev/null >/dev/null 2>&1 || true
  fi
}

log() {
  printf '[forensic-adb] %s\n' "$*"
}

trim() {
  local s="$1"
  s="${s#${s%%[![:space:]]*}}"
  s="${s%${s##*[![:space:]]}}"
  printf '%s' "${s}"
}

linker_prop_name() {
  printf 'debug.ld.app.%s\n' "${WLT_PACKAGE}"
}

enable_linker_telemetry() {
  local prop
  [[ "${WLT_CAPTURE_LINKER_TELEMETRY}" == "1" ]] || return 0
  prop="$(linker_prop_name)"
  adb_cmd shell setprop "${prop}" "${WLT_LINKER_DEBUG_FLAGS}" >/dev/null 2>&1 || true
}

disable_linker_telemetry() {
  local prop
  [[ "${WLT_CAPTURE_LINKER_TELEMETRY}" == "1" ]] || return 0
  prop="$(linker_prop_name)"
  adb_cmd shell setprop "${prop}" "" >/dev/null 2>&1 || true
}

resolve_app_data_dir() {
  local app_dir
  app_dir="$(adb_cmd shell "run-as ${WLT_PACKAGE} pwd" | tr -d '\r' | tail -n1)"
  app_dir="$(trim "${app_dir}")"
  [[ "${app_dir}" == /data/* ]] || fail "unexpected app data dir for ${WLT_PACKAGE}: ${app_dir:-<empty>}"
  printf '%s\n' "${app_dir}"
}

best_effort() {
  "$@" || true
}

capture_shell() {
  local outfile="$1"; shift
  adb_capture_timeout "${outfile}" shell "$@"
}

root_shell_available() {
  [[ "${WLT_PREFER_ROOT}" == "1" ]] || return 1
  adb_cmd shell 'su -c id -u' 2>/dev/null | tr -d '\r' | grep -qx '0'
}

copy_app_private_forensics() {
  local scenario_dir="$1"
  local index_file="${scenario_dir}/forensics-jsonl-tail.index.txt"
  : > "${scenario_dir}/forensics-jsonl-tail.txt"
  adb_capture_timeout "${index_file}" shell \
    "run-as ${WLT_PACKAGE} find ${WLT_APP_PRIVATE_RUNTIME_LOG_ROOT_ABS}/forensics -type f -name '*.jsonl' 2>/dev/null"
  while IFS= read -r remote; do
    [[ -n "${remote}" ]] || continue
    printf '===== %s =====\n' "${remote}" >> "${scenario_dir}/forensics-jsonl-tail.txt"
    adb_capture_timeout "${scenario_dir}/.tmp-forensics-tail.txt" exec-out run-as "${WLT_PACKAGE}" cat "${remote}"
    cat "${scenario_dir}/.tmp-forensics-tail.txt" >> "${scenario_dir}/forensics-jsonl-tail.txt"
    printf '\n' >> "${scenario_dir}/forensics-jsonl-tail.txt"
  done < <(tr -d '\r' < "${index_file}" | sort)
  rm -f "${scenario_dir}/.tmp-forensics-tail.txt"
  [[ -s "${scenario_dir}/forensics-jsonl-tail.txt" ]] || rm -f "${scenario_dir}/forensics-jsonl-tail.txt"
}

pull_runtime_logs() {
  local scenario_dir="$1"
  local runtime_dir="${scenario_dir}/runtime-logs"
  mkdir -p "${runtime_dir}"
  local pulled=0
  local remote_list remote list_file app_private_list_file app_private_list
  list_file="${scenario_dir}/runtime-logs-external-index.txt"
  adb_capture_timeout "${list_file}" shell \
    "sh -c 'for d in ${WLT_RUNTIME_LOG_ROOTS}; do [ -d \"\$d\" ] || continue; find \"\$d\" -maxdepth 1 -type f 2>/dev/null; done | sort -u'"
  remote_list="$(tr -d '\r' < "${list_file}" || true)"
  while IFS= read -r remote; do
    [[ -n "${remote}" ]] || continue
    adb_pull_timeout "${remote}" "${runtime_dir}/"
    pulled=1
  done <<<"${remote_list}"
  app_private_list_file="${scenario_dir}/runtime-logs-app-private-index.txt"
  adb_capture_timeout "${app_private_list_file}" shell \
    "run-as ${WLT_PACKAGE} find ${WLT_APP_PRIVATE_RUNTIME_LOG_ROOT_ABS} -type f -name '*.txt' 2>/dev/null"
  app_private_list="$(tr -d '\r' < "${app_private_list_file}" || true)"
  while IFS= read -r remote; do
    [[ -n "${remote}" ]] || continue
    adb_capture_timeout "${runtime_dir}/app-private__$(basename "${remote}")" exec-out \
      run-as "${WLT_PACKAGE}" cat "${remote}"
    pulled=1
  done <<<"${app_private_list}"
  [[ "${pulled}" == "1" ]] || rmdir "${runtime_dir}" 2>/dev/null || true
}

main() {
  command -v "${ADB}" >/dev/null 2>&1 || { echo "adb not found" >&2; exit 1; }
  command -v python3 >/dev/null 2>&1 || { echo "python3 not found" >&2; exit 1; }

  mkdir -p "${WLT_BUNDLE_DIR}"
  local stamp scenario_dir root_mode issue_md title
  stamp="$(date -u +%Y%m%d_%H%M%S)"
  scenario_dir="${WLT_BUNDLE_DIR}/${stamp}_${WLT_LABEL}"
  mkdir -p "${scenario_dir}"
  WLT_APP_DATA_DIR="$(resolve_app_data_dir)"
  WLT_APP_PRIVATE_RUNTIME_LOG_ROOT_ABS="${WLT_APP_DATA_DIR}/files/Winlator/logs"

  printf 'label=%s\npackage=%s\nserial=%s\n' "${WLT_LABEL}" "${WLT_PACKAGE}" "${WLT_SERIAL}" >"${scenario_dir}/scenario_meta.txt"
  printf 'app_data_dir=%s\napp_private_runtime_log_root_abs=%s\n' \
    "${WLT_APP_DATA_DIR}" "${WLT_APP_PRIVATE_RUNTIME_LOG_ROOT_ABS}" >> "${scenario_dir}/scenario_meta.txt"
  printf 'elapsed_sec=0\nsaw_intent=unknown\nsaw_submit=unknown\nsaw_terminal=unknown\n' >"${scenario_dir}/wait-status.txt"

  log "Capturing logcat and device state into ${scenario_dir}"
  trap 'disable_linker_telemetry || true' EXIT INT TERM
  enable_linker_telemetry
  adb_capture_timeout "${scenario_dir}/logcat-full.txt" logcat -d -v threadtime
  grep -Ei 'ForensicLogger|freewine-|[[:space:]]linker[[:space:]]*:|AERO_|VKD3D|DXVK|box64|fex|turnip|wine_loader|Pulse|ALSA|assert|undefined symbol|dlopen failed' \
    "${scenario_dir}/logcat-full.txt" >"${scenario_dir}/logcat-filtered.txt" || true
  grep -E '[[:space:]]linker[[:space:]]*:' "${scenario_dir}/logcat-full.txt" >"${scenario_dir}/logcat-linker.txt" || true
  capture_shell "${scenario_dir}/getprop.txt" getprop
  capture_shell "${scenario_dir}/linker-prop.txt" getprop "$(linker_prop_name)"
  capture_shell "${scenario_dir}/ps.txt" ps -A -o USER,PID,PPID,NAME,ARGS
  capture_shell "${scenario_dir}/meminfo.txt" dumpsys meminfo "${WLT_PACKAGE}"
  capture_shell "${scenario_dir}/gfxinfo.txt" dumpsys gfxinfo "${WLT_PACKAGE}"
  capture_shell "${scenario_dir}/surfaceflinger.txt" dumpsys SurfaceFlinger
  capture_shell "${scenario_dir}/window.txt" dumpsys window
  capture_shell "${scenario_dir}/activity_top.txt" dumpsys activity top
  copy_app_private_forensics "${scenario_dir}"
  pull_runtime_logs "${scenario_dir}"

  root_mode="nonroot"
  if root_shell_available; then
    root_mode="root"
    log "Capturing root extras"
    adb_capture_timeout "${scenario_dir}/dmesg.txt" shell 'su -c dmesg'
    adb_capture_timeout "${scenario_dir}/logcat-all-buffers.txt" shell 'su -c logcat -b all -d -v threadtime'
    adb_capture_timeout "${scenario_dir}/proc-meminfo.txt" shell 'su -c cat /proc/meminfo'
    adb_capture_timeout "${scenario_dir}/proc-pressure-memory.txt" shell 'su -c cat /proc/pressure/memory'
  fi

  python3 "${ROOT_DIR}/ci/winlator/forensic-runtime-log-assembler.py" --input "${scenario_dir}"
  python3 "${ROOT_DIR}/ci/winlator/forensic-issue-bundle.py" --scenario-dir "${scenario_dir}" --root-mode "${root_mode}"

  issue_md="${scenario_dir}/ISSUE.md"
  title="$(python3 - <<'PY' "${issue_md}"
from pathlib import Path
import sys
path = Path(sys.argv[1])
line = path.read_text(encoding='utf-8', errors='ignore').splitlines()[0] if path.is_file() else 'Ae.solator forensic issue'
print(line.lstrip('# ').strip())
PY
)"

  if [[ "${WLT_GH_CREATE_ISSUE}" == "1" ]]; then
    command -v gh >/dev/null 2>&1 || { echo "gh not found" >&2; exit 1; }
    [[ -n "${WLT_GH_REPO}" ]] || { echo "--repo is required with --create-issue" >&2; exit 1; }
    gh issue create --repo "${WLT_GH_REPO}" --title "${title}" --body-file "${issue_md}"
  fi

  log "Issue bundle ready: ${scenario_dir}"
  log "Markdown: ${issue_md}"
}

main "$@"
