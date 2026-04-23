#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"

: "${WLT_PACKAGE:=com.winlator.cmod}"
: "${WLT_REVDBG_PATTERNS:=wineboot\\.exe --init|explorer\\.exe|wineserver|WinHandler\\.exe|wfm\\.exe}"
: "${WLT_REVDBG_OUT_DIR:=${ROOT_DIR}/out/revdbg-$(date +%Y%m%d_%H%M%S)}"
: "${WLT_RUNAS_SAMPLER_SAMPLES:=12}"
: "${WLT_RUNAS_SAMPLER_INTERVAL_MS:=250}"
: "${WLT_REVDBG_CAPTURE_DEBUGGERD:=1}"
: "${WLT_REVDBG_DISCOVERY_SEC:=6}"
: "${WLT_REVDBG_DISCOVERY_INTERVAL_MS:=150}"

log() { printf '[runas-revdbg] %s\n' "$*" >&2; }
fail() { printf '[runas-revdbg][error] %s\n' "$*" >&2; exit 1; }

pick_serial() {
  local serial="${ADB_SERIAL:-}"
  if [[ -n "${serial}" ]]; then
    printf '%s\n' "${serial}"
    return 0
  fi
  adb devices | awk 'NR>1 && $2=="device" {print $1; exit}'
}

adb_s() { adb -s "${ADB_SERIAL_PICKED}" "$@"; }

discovery_interval_sec() {
  local ms="${WLT_REVDBG_DISCOVERY_INTERVAL_MS}"
  printf '%s.%03d' "$((ms / 1000))" "$((ms % 1000))"
}

capture_debuggerd_backtrace() {
  local pid="$1"
  local out_file="$2"

  [[ "${WLT_REVDBG_CAPTURE_DEBUGGERD}" == "1" ]] || return 0

  adb_s exec-out run-as "${WLT_PACKAGE}" sh -c "debuggerd -b '${pid}'" > "${out_file}" 2>/dev/null \
    || adb_s shell "run-as ${WLT_PACKAGE} sh -c 'debuggerd -b \"${pid}\"'" > "${out_file}" 2>/dev/null \
    || rm -f "${out_file}"
}

resolve_app_ps_user() {
  adb_s shell "run-as ${WLT_PACKAGE} sh -c 'id -un'" 2>/dev/null | tr -d '\r'
}

list_targets() {
  adb_s shell "ps -A -o USER,PID,NAME,ARGS | awk 'BEGIN{IGNORECASE=1} NR>1 && \$1==\"${app_ps_user}\" && \$0 ~ /${WLT_REVDBG_PATTERNS}/ { printf \"%s\\t%s\\t\", \$2, \$3; \$1=\$2=\$3=\"\"; sub(/^[ \t]+/, \"\", \$0); print \$0; }'" \
    | tr -d '\r'
}

sanitize_label() {
  local raw="$1" safe
  safe="$(printf '%s' "${raw}" | tr -cs '[:alnum:]._:-' '_')"
  safe="${safe//:/_}"
  safe="${safe##_}"
  safe="${safe%%_}"
  [[ -n "${safe}" ]] || safe="target"
  printf '%s\n' "${safe}"
}

capture_target() {
  local pid="$1" name="$2" args="$3" safe

  safe="$(sanitize_label "${name}-${pid}")"
  printf '%s\t%s\t%s\n' "${pid}" "${name}" "${args}" >> "${WLT_REVDBG_OUT_DIR}/targets.tsv"
  log "capture pid=${pid} name=${name} args=${args}"
  capture_debuggerd_backtrace "${pid}" "${WLT_REVDBG_OUT_DIR}/debuggerd/${safe}.backtrace.txt"
  ADB_SERIAL="${ADB_SERIAL_PICKED}" \
  APP_PS_USER="${app_ps_user}" \
  WLT_PACKAGE="${WLT_PACKAGE}" \
  WLT_RUNAS_SAMPLER_SAMPLES="${WLT_RUNAS_SAMPLER_SAMPLES}" \
  WLT_RUNAS_SAMPLER_INTERVAL_MS="${WLT_RUNAS_SAMPLER_INTERVAL_MS}" \
  WLT_RUNAS_SAMPLER_OUT="${WLT_REVDBG_OUT_DIR}/runas-ptrace/${safe}.jsonl" \
    "${ROOT_DIR}/ci/winlator/adb-runas-ptrace-sampler.sh" "${pid}"
}

main() {
  local app_ps_user pid name args
  local deadline now found_any=0
  local interval_sec
  local -A seen_pids=()
  command -v adb >/dev/null 2>&1 || fail "adb not found"
  command -v python3 >/dev/null 2>&1 || fail "python3 not found"

  ADB_SERIAL_PICKED="$(pick_serial)"
  [[ -n "${ADB_SERIAL_PICKED}" ]] || fail "No active adb device"
  export ADB_SERIAL_PICKED

  app_ps_user="$(resolve_app_ps_user)"
  [[ -n "${app_ps_user}" ]] || fail "Could not resolve app ps user for ${WLT_PACKAGE}"

  mkdir -p "${WLT_REVDBG_OUT_DIR}/runas-ptrace"
  mkdir -p "${WLT_REVDBG_OUT_DIR}/debuggerd"
  printf 'package=%s\nserial=%s\napp_ps_user=%s\ntime=%s\npatterns=%s\n' \
    "${WLT_PACKAGE}" "${ADB_SERIAL_PICKED}" "${app_ps_user}" "$(date -Is)" "${WLT_REVDBG_PATTERNS}" \
    > "${WLT_REVDBG_OUT_DIR}/revdbg-meta.env"
  printf 'capture_debuggerd=%s\n' "${WLT_REVDBG_CAPTURE_DEBUGGERD}" >> "${WLT_REVDBG_OUT_DIR}/revdbg-meta.env"
  printf 'discovery_sec=%s\ndiscovery_interval_ms=%s\n' \
    "${WLT_REVDBG_DISCOVERY_SEC}" "${WLT_REVDBG_DISCOVERY_INTERVAL_MS}" >> "${WLT_REVDBG_OUT_DIR}/revdbg-meta.env"
  : > "${WLT_REVDBG_OUT_DIR}/targets.tsv"
  interval_sec="$(discovery_interval_sec)"
  deadline=$((SECONDS + WLT_REVDBG_DISCOVERY_SEC))

  while :; do
    while IFS=$'\t' read -r pid name args; do
      [[ -n "${pid}" ]] || continue
      [[ -n "${seen_pids[${pid}]:-}" ]] && continue
      seen_pids["${pid}"]=1
      found_any=1
      capture_target "${pid}" "${name}" "${args}"
    done < <(list_targets)

    now=${SECONDS}
    (( now >= deadline )) && break
    sleep "${interval_sec}"
  done

  [[ "${found_any}" -eq 1 ]] || log "no matching targets observed during discovery window"

  python3 "${ROOT_DIR}/ci/winlator/forensic-runas-ptrace-symbolize.py" \
    --scenario-dir "${WLT_REVDBG_OUT_DIR}" \
    --workspace "${ROOT_DIR}"

  log "summary=${WLT_REVDBG_OUT_DIR}/runas-revdbg.summary.txt"
}

main "$@"
