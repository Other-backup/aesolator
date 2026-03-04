#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
: "${ADB:=adb}"
: "${WLT_PACKAGE:=by.aero.so.benchmark}"
: "${WLT_LABEL:=forensic-issue}"
: "${WLT_BUNDLE_DIR:=${ROOT_DIR}/out/adb-issues}"
: "${WLT_PREFER_ROOT:=0}"
: "${WLT_SERIAL:=}"
: "${WLT_GH_CREATE_ISSUE:=0}"
: "${WLT_GH_REPO:=${GITHUB_REPOSITORY:-}}"

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

log() {
  printf '[forensic-adb] %s\n' "$*"
}

best_effort() {
  "$@" || true
}

capture_shell() {
  local outfile="$1"; shift
  adb_cmd shell "$@" >"${outfile}" 2>&1 || true
}

root_shell_available() {
  [[ "${WLT_PREFER_ROOT}" == "1" ]] || return 1
  adb_cmd shell 'su -c id -u' 2>/dev/null | tr -d '\r' | grep -qx '0'
}

copy_app_private_forensics() {
  local scenario_dir="$1"
  adb_cmd exec-out run-as "${WLT_PACKAGE}" sh -c 'for f in files/Winlator/logs/forensics/*.jsonl; do [ -f "$f" ] && cat "$f"; done' \
    >"${scenario_dir}/forensics-jsonl-tail.txt" 2>/dev/null || true
  [[ -s "${scenario_dir}/forensics-jsonl-tail.txt" ]] || rm -f "${scenario_dir}/forensics-jsonl-tail.txt"
}

pull_runtime_logs() {
  local scenario_dir="$1"
  local runtime_dir="${scenario_dir}/runtime-logs"
  mkdir -p "${runtime_dir}"
  local pulled=0
  local roots=("/sdcard/Ae.solator/logs" "/sdcard/Winlator/logs")
  local root remote_list remote
  for root in "${roots[@]}"; do
    remote_list="$(adb_cmd shell "find '${root}' -maxdepth 1 -type f 2>/dev/null" | tr -d '\r' || true)"
    [[ -n "${remote_list}" ]] || continue
    while IFS= read -r remote; do
      [[ -n "${remote}" ]] || continue
      adb_cmd pull "${remote}" "${runtime_dir}/" >/dev/null 2>&1 || true
      pulled=1
    done <<<"${remote_list}"
  done
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

  printf 'label=%s\npackage=%s\nserial=%s\n' "${WLT_LABEL}" "${WLT_PACKAGE}" "${WLT_SERIAL}" >"${scenario_dir}/scenario_meta.txt"
  printf 'elapsed_sec=0\nsaw_intent=unknown\nsaw_submit=unknown\nsaw_terminal=unknown\n' >"${scenario_dir}/wait-status.txt"

  log "Capturing logcat and device state into ${scenario_dir}"
  adb_cmd logcat -d -v threadtime >"${scenario_dir}/logcat-full.txt" 2>&1 || true
  grep -Ei 'ForensicLogger|AERO_|VKD3D|DXVK|box64|fex|turnip|wine_loader|Pulse|ALSA|assert|undefined symbol|dlopen failed' \
    "${scenario_dir}/logcat-full.txt" >"${scenario_dir}/logcat-filtered.txt" || true
  capture_shell "${scenario_dir}/getprop.txt" getprop
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
    adb_cmd shell 'su -c dmesg' >"${scenario_dir}/dmesg.txt" 2>&1 || true
    adb_cmd shell 'su -c logcat -b all -d -v threadtime' >"${scenario_dir}/logcat-all-buffers.txt" 2>&1 || true
    adb_cmd shell 'su -c cat /proc/meminfo' >"${scenario_dir}/proc-meminfo.txt" 2>&1 || true
    adb_cmd shell 'su -c cat /proc/pressure/memory' >"${scenario_dir}/proc-pressure-memory.txt" 2>&1 || true
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
