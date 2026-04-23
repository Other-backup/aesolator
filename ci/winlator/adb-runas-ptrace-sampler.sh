#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"

: "${WLT_PACKAGE:=com.winlator.cmod}"
: "${WLT_RUNAS_SAMPLER_SAMPLES:=5}"
: "${WLT_RUNAS_SAMPLER_INTERVAL_MS:=200}"
: "${WLT_RUNAS_SAMPLER_OUT:=}"
: "${WLT_RUNAS_SAMPLER_OUT_DIR:=}"
: "${WLT_RUNAS_SAMPLER_BASENAME:=sampler}"
: "${WLT_RUNAS_SAMPLER_HOST_TIMEOUT_SEC:=25}"
: "${WLT_RUNAS_APP_HOME:=}"

log() { printf '[runas-ptrace] %s\n' "$*" >&2; }
fail() { printf '[runas-ptrace][error] %s\n' "$*" >&2; exit 1; }
require_cmd() { command -v "$1" >/dev/null 2>&1 || fail "Required command not found: $1"; }

pick_serial() {
  local serial="${ADB_SERIAL:-}"
  if [[ -n "${serial}" ]]; then
    printf '%s\n' "${serial}"
    return 0
  fi
  adb devices | awk 'NR>1 && $2=="device" {print $1; exit}'
}

adb_s() { adb -s "${ADB_SERIAL_PICKED}" "$@"; }

resolve_app_home() {
  local app_home="${WLT_RUNAS_APP_HOME:-}"
  if [[ -n "${app_home}" ]]; then
    printf '%s\n' "${app_home}"
    return 0
  fi
  adb_s shell "run-as ${WLT_PACKAGE} sh -lc 'pwd'" | tr -d '\r' | awk 'NF{print; exit}'
}

capture_sidecar() {
  local pid="$1"
  local remote_expr="$2"
  local out_file="$3"
  adb_s exec-out run-as "${WLT_PACKAGE}" sh -c "${remote_expr}" > "${out_file}" 2>/dev/null \
    || adb_s shell "${remote_expr}" > "${out_file}" 2>/dev/null \
    || true
}

build_sampler() {
  local out="${ROOT_DIR}/out/tools/runas-ptrace-sampler"
  mkdir -p "$(dirname -- "${out}")"
  clang -O2 -g -fPIE -pie \
    -o "${out}" \
    "${ROOT_DIR}/ci/winlator/runas-ptrace-sampler.c"
  printf '%s\n' "${out}"
}

resolve_output_paths() {
  local out_file="${WLT_RUNAS_SAMPLER_OUT:-}"
  local out_dir="${WLT_RUNAS_SAMPLER_OUT_DIR:-}"
  local base_name="${WLT_RUNAS_SAMPLER_BASENAME:-sampler}"
  local base_file

  if [[ -n "${out_file}" ]]; then
    :
  elif [[ -n "${out_dir}" ]]; then
    mkdir -p "${out_dir}"
    out_file="${out_dir%/}/${base_name}.jsonl"
  else
    out_file="/dev/stdout"
  fi

  if [[ "${out_file}" == "/dev/stdout" || "${out_file}" == "/dev/stderr" ]]; then
    if [[ -n "${out_dir}" ]]; then
      mkdir -p "${out_dir}"
      base_file="${out_dir%/}/${base_name}"
    else
      base_file=""
    fi
  else
    mkdir -p "$(dirname -- "${out_file}")"
    base_file="${out_file%.jsonl}"
  fi

  printf '%s\n%s\n' "${out_file}" "${base_file}"
}

resolve_app_ps_user() {
  adb_s exec-out run-as "${WLT_PACKAGE}" id -un 2>/dev/null | tr -d '\r'
}

resolve_pid() {
  local pattern="$1"
  local app_ps_user="${APP_PS_USER:-}"
  if [[ -z "${app_ps_user}" ]]; then
    app_ps_user="$(resolve_app_ps_user)"
  fi
  if [[ -n "${app_ps_user}" ]]; then
    adb_s shell "ps -A -o USER,PID,NAME,ARGS | awk -v app_user='${app_ps_user}' -v pat='${pattern}' 'BEGIN{IGNORECASE=1} NR>1 && \$1==app_user && \$0 ~ pat {print \$2; exit}'" \
      | tr -d '\r'
    return 0
  fi
  adb_s shell "ps -A -o PID,NAME,ARGS | awk -v pat='${pattern}' 'BEGIN{IGNORECASE=1} NR>1 && \$0 ~ pat {print \$1; exit}'" | tr -d '\r'
}

main() {
  local pid="${1:-}"
  local pattern="${2:-wineboot\\.exe --init}"
  local host_bin remote_bin out_file
  local base_file
  local resolved_out
  local app_home

  require_cmd adb
  require_cmd clang
  ADB_SERIAL_PICKED="$(pick_serial)"
  [[ -n "${ADB_SERIAL_PICKED}" ]] || fail "No active adb device"
  export ADB_SERIAL_PICKED

  app_home="$(resolve_app_home)"
  [[ "${app_home}" == /data/* ]] || fail "Could not resolve run-as app home"

  if [[ -z "${pid}" ]]; then
    pid="$(resolve_pid "${pattern}")"
  fi
  [[ -n "${pid}" ]] || fail "Could not resolve target pid"

  host_bin="$(build_sampler)"
  remote_bin="files/.runas-ptrace-sampler"
  resolved_out="$(resolve_output_paths)"
  out_file="$(printf '%s\n' "${resolved_out}" | sed -n '1p')"
  base_file="$(printf '%s\n' "${resolved_out}" | sed -n '2p')"

  if [[ -n "${base_file}" ]]; then
    capture_sidecar "${pid}" "cat /proc/${pid}/maps" "${base_file}.maps"
    capture_sidecar "${pid}" "cat /proc/${pid}/status" "${base_file}.status"
    capture_sidecar "${pid}" "ps -A -T -o USER,PID,TID,NAME,ARGS | awk 'NR==1 || \$2==${pid}'" "${base_file}.threads"
    capture_sidecar "${pid}" "for t in /proc/${pid}/task/*; do [ -d \"\$t\" ] || continue; tid=\${t##*/}; printf '=== %s ===\n' \"\$tid\"; cat \"\$t/wchan\" 2>/dev/null || true; printf '\n'; done" "${base_file}.task-wchan.txt"
    capture_sidecar "${pid}" "for t in /proc/${pid}/task/*; do [ -d \"\$t\" ] || continue; tid=\${t##*/}; printf '=== %s ===\n' \"\$tid\"; cat \"\$t/stat\" 2>/dev/null || true; printf '\n'; done" "${base_file}.task-stat.txt"
    capture_sidecar "${pid}" "for t in /proc/${pid}/task/*; do [ -d \"\$t\" ] || continue; tid=\${t##*/}; printf '=== %s ===\n' \"\$tid\"; cat \"\$t/syscall\" 2>/dev/null || true; printf '\n'; done" "${base_file}.task-syscall.txt"
    capture_sidecar "${pid}" "tr '\\000' ' ' < /proc/${pid}/cmdline" "${base_file}.cmdline"
    capture_sidecar "${pid}" "readlink /proc/${pid}/exe" "${base_file}.exe"
    capture_sidecar "${pid}" "readlink /proc/${pid}/cwd" "${base_file}.cwd"
    capture_sidecar "${pid}" "ls -l /proc/${pid}/fd" "${base_file}.fd.txt"
    capture_sidecar "${pid}" "cat /proc/${pid}/smaps_rollup" "${base_file}.smaps_rollup"
    capture_sidecar "${pid}" "cat /proc/${pid}/mountinfo" "${base_file}.mountinfo"
  fi

  adb_s shell "run-as ${WLT_PACKAGE} sh -c 'cd \"${app_home}\" && cat > \"${remote_bin}\" && chmod 700 \"${remote_bin}\"'" < "${host_bin}"
  if command -v timeout >/dev/null 2>&1; then
    timeout --foreground "${WLT_RUNAS_SAMPLER_HOST_TIMEOUT_SEC}" \
      adb -s "${ADB_SERIAL_PICKED}" exec-out run-as "${WLT_PACKAGE}" sh -c \
        "cd \"${app_home}\" && './${remote_bin}' '${pid}' '${WLT_RUNAS_SAMPLER_SAMPLES}' '${WLT_RUNAS_SAMPLER_INTERVAL_MS}'" \
        > "${out_file}" || true
  else
    adb_s exec-out run-as "${WLT_PACKAGE}" sh -c \
      "cd \"${app_home}\" && './${remote_bin}' '${pid}' '${WLT_RUNAS_SAMPLER_SAMPLES}' '${WLT_RUNAS_SAMPLER_INTERVAL_MS}'" \
      > "${out_file}" || true
  fi
}

main "$@"
