#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"

: "${WLT_PACKAGE:=com.winlator.cmod}"
: "${WLT_ACTIVITY:=com.winlator.cmod.XServerDisplayActivity}"
: "${WLT_CONTAINER_IDS:=1 2}"
: "${WLT_SCENARIOS:=}"
: "${WLT_TMP_ROOT:=${TMPDIR:-${ROOT_DIR}/out/forensics-tmp}}"
: "${WLT_OUT_DIR:=${WLT_TMP_ROOT}/winlator-complete-forensics-$(date +%Y%m%d_%H%M%S)}"
: "${WLT_WAIT_TIMEOUT_SEC:=20}"
: "${WLT_WAIT_INTENT_TIMEOUT_SEC:=${WLT_WAIT_TIMEOUT_SEC}}"
: "${WLT_WAIT_POST_INTENT_TIMEOUT_SEC:=${WLT_WAIT_TIMEOUT_SEC}}"
: "${WLT_POLL_SEC:=1}"
: "${WLT_LOGCAT_LINES:=4000}"
: "${WLT_CAPTURE_UI:=1}"
: "${WLT_CAPTURE_PREFS:=1}"
: "${WLT_CAPTURE_RUNTIME_CONTENTS:=1}"
: "${WLT_CAPTURE_CONFLICT_LOGS:=1}"
: "${WLT_CAPTURE_RUNAS_PTRACE:=1}"
: "${WLT_RUNAS_PTRACE_SYMBOLIZE:=1}"
: "${WLT_RUNAS_PTRACE_PATTERNS:=wineboot\\.exe --init|explorer\\.exe|services\\.exe|wineserver|WinHandler\\.exe|wfm\\.exe}"
: "${WLT_RUNAS_SAMPLER_SAMPLES:=5}"
: "${WLT_RUNAS_SAMPLER_INTERVAL_MS:=200}"
: "${WLT_RUNTIME_LOG_ROOTS:=/sdcard/Ae.solator/logs /storage/emulated/0/Ae.solator/logs /sdcard/Winlator/logs}"
: "${WLT_APP_PRIVATE_RUNTIME_LOG_ROOT:=files/Winlator/logs}"
: "${WLT_PROCESS_SAMPLE_SEC:=1}"
: "${WLT_CAPTURE_LINKER_TELEMETRY:=1}"
: "${WLT_LINKER_DEBUG_FLAGS:=dlopen,dlsym,dlerror}"
: "${WLT_ADB_HOST_TIMEOUT_SEC:=12}"
: "${WLT_ACTIVITY_START_HOST_TIMEOUT_SEC:=20}"
: "${WLT_ADB_FALLBACK_ENDPOINTS:=127.0.0.1:5555}"
: "${WLT_ADB_CONNECT_TIMEOUT_SEC:=4}"
: "${WLT_ADB_OFFLINE_STUB:=1}"
WLT_APP_DATA_DIR=""
WLT_APP_FILES_DIR=""
WLT_APP_PRIVATE_RUNTIME_LOG_ROOT_ABS=""
WLT_IMAGEFS_HOME_DIR=""
WLT_WINE_CONTENTS_DIR=""

if [[ -z "${WLT_PROCESS_SAMPLES:-}" ]]; then
  process_window_target="${WLT_WAIT_POST_INTENT_TIMEOUT_SEC:-${WLT_WAIT_TIMEOUT_SEC}}"
  case "${process_window_target}" in
    ''|*[!0-9]*)
      process_window_target=12
      ;;
  esac
  if (( process_window_target < 12 )); then
    process_window_target=12
  fi
  WLT_PROCESS_SAMPLES="${process_window_target}"
fi

log() { printf '[forensic-complete] %s\n' "$*" >&2; }
fail() { printf '[forensic-complete][error] %s\n' "$*" >&2; exit 1; }

require_cmd() { command -v "$1" >/dev/null 2>&1 || fail "Required command not found: $1"; }

adb_state_for_serial() {
  local serial="$1"
  [[ -n "${serial}" ]] || return 0
  adb -s "${serial}" get-state 2>/dev/null | tr -d '\r' | awk 'NF{print; exit}' || true
}

first_active_adb_serial() {
  adb devices | awk 'NR>1 && $2=="device" {print $1; exit}'
}

effective_adb_fallback_endpoints() {
  local requested endpoint host seen=""
  requested="${ADB_SERIAL:-}"
  if [[ -n "${requested}" && "${requested}" == *:* ]]; then
    host="${requested%:*}"
    if [[ -n "${host}" && "${host}" != "${requested}" ]]; then
      endpoint="${host}:5555"
      if [[ " ${seen} " != *" ${endpoint} "* ]]; then
        printf '%s\n' "${endpoint}"
        seen="${seen} ${endpoint}"
      fi
    fi
  fi
  for endpoint in ${WLT_ADB_FALLBACK_ENDPOINTS}; do
    [[ -n "${endpoint}" ]] || continue
    if [[ " ${seen} " == *" ${endpoint} "* ]]; then
      continue
    fi
    printf '%s\n' "${endpoint}"
    seen="${seen} ${endpoint}"
  done
}

join_effective_adb_fallback_endpoints() {
  local endpoint joined=""
  while IFS= read -r endpoint; do
    [[ -n "${endpoint}" ]] || continue
    joined="${joined}${joined:+ }${endpoint}"
  done < <(effective_adb_fallback_endpoints)
  printf '%s' "${joined}"
}

record_adb_connect_attempt() {
  local endpoint="$1"
  local detail="$2"
  [[ -n "${WLT_OUT_DIR:-}" ]] || return 0
  mkdir -p "${WLT_OUT_DIR}" 2>/dev/null || return 0
  printf '%s endpoint=%s %s\n' "$(iso_now 2>/dev/null || date -Is)" "${endpoint}" "${detail}" \
    >> "${WLT_OUT_DIR}/adb-connect-attempts.txt"
}

try_connect_adb_endpoint() {
  local endpoint="$1"
  local output state
  [[ -n "${endpoint}" ]] || return 1
  if command -v timeout >/dev/null 2>&1; then
    output="$(timeout --foreground "${WLT_ADB_CONNECT_TIMEOUT_SEC}" adb connect "${endpoint}" 2>&1 || true)"
  else
    output="$(adb connect "${endpoint}" 2>&1 || true)"
  fi
  state="$(adb_state_for_serial "${endpoint}")"
  record_adb_connect_attempt "${endpoint}" "state=${state:-none} output=$(printf '%s' "${output}" | tr '\n\r' '  ')"
  [[ "${state}" == "device" ]]
}

pick_serial() {
  local serial state endpoint
  serial="${ADB_SERIAL:-}"
  if [[ -n "${serial}" ]]; then
    state="$(adb_state_for_serial "${serial}")"
    if [[ "${state}" == "device" ]]; then
      printf '%s\n' "${serial}"
      return 0
    fi
    record_adb_connect_attempt "${serial}" "requested_state=${state:-none}"
  fi
  serial="$(first_active_adb_serial)"
  if [[ -n "${serial}" ]]; then
    printf '%s\n' "${serial}"
    return 0
  fi
  while IFS= read -r endpoint; do
    [[ -n "${endpoint}" ]] || continue
    if try_connect_adb_endpoint "${endpoint}"; then
      printf '%s\n' "${endpoint}"
      return 0
    fi
  done < <(effective_adb_fallback_endpoints)
  first_active_adb_serial
}

adb_s() { adb -s "${ADB_SERIAL_PICKED}" "$@"; }

trim() {
  local s="$1"
  s="${s#${s%%[![:space:]]*}}"
  s="${s%${s##*[![:space:]]}}"
  printf '%s' "${s}"
}

adb_capture_timeout() {
  local out_file="$1"
  shift
  if command -v timeout >/dev/null 2>&1; then
    timeout --foreground "${WLT_ADB_HOST_TIMEOUT_SEC}" adb -s "${ADB_SERIAL_PICKED}" "$@" \
      < /dev/null > "${out_file}" 2>&1 || true
  else
    adb_s "$@" < /dev/null > "${out_file}" 2>&1 || true
  fi
}

adb_capture_append_timeout() {
  local out_file="$1"
  shift
  if command -v timeout >/dev/null 2>&1; then
    timeout --foreground "${WLT_ADB_HOST_TIMEOUT_SEC}" adb -s "${ADB_SERIAL_PICKED}" "$@" \
      < /dev/null >> "${out_file}" 2>&1 || true
  else
    adb_s "$@" < /dev/null >> "${out_file}" 2>&1 || true
  fi
}

linker_prop_name() {
  printf 'debug.ld.app.%s\n' "${WLT_PACKAGE}"
}

resolve_app_data_dir() {
  local app_dir
  app_dir="$(adb_s shell "run-as ${WLT_PACKAGE} pwd" | tr -d '\r' | tail -n1)"
  app_dir="$(trim "${app_dir}")"
  [[ "${app_dir}" == /data/* ]] || fail "unexpected app data dir for ${WLT_PACKAGE}: ${app_dir:-<empty>}"
  printf '%s\n' "${app_dir}"
}

enable_linker_telemetry() {
  local prop
  [[ "${WLT_CAPTURE_LINKER_TELEMETRY}" == "1" ]] || return 0
  prop="$(linker_prop_name)"
  adb_s shell setprop "${prop}" "${WLT_LINKER_DEBUG_FLAGS}" >/dev/null 2>&1 || true
}

disable_linker_telemetry() {
  local prop
  [[ "${WLT_CAPTURE_LINKER_TELEMETRY}" == "1" ]] || return 0
  prop="$(linker_prop_name)"
  adb_s shell setprop "${prop}" "" >/dev/null 2>&1 || true
}

iso_now() { date -Is; }

sanitize_label() {
  local raw="$1" safe
  safe="$(printf '%s' "${raw}" | tr -cs '[:alnum:]._:-' '_')"
  safe="${safe//:/_}"
  safe="${safe##_}"
  safe="${safe%%_}"
  [[ -n "${safe}" ]] || safe="scenario"
  printf '%s\n' "${safe}"
}

write_adb_offline_stub() {
  local scenario_specs=()
  local spec label safe_label cid scenario_dir trace_id now effective_endpoints
  now="$(iso_now)"
  effective_endpoints="$(join_effective_adb_fallback_endpoints)"
  mkdir -p "${WLT_OUT_DIR}"
  adb devices -l > "${WLT_OUT_DIR}/adb-devices.txt" 2>&1 || true
  printf 'package=%s\nserial=\ntime=%s\ncontainer_ids=%s\nscenarios=%s\nadb_stub_active=1\nadb_stub_kind=offline\nadb_fallback_endpoints_config=%s\nadb_fallback_endpoints_effective=%s\nadb_connect_timeout_sec=%s\nblocker=no_active_adb_device\n' \
    "${WLT_PACKAGE}" "${now}" "${WLT_CONTAINER_IDS}" "${WLT_SCENARIOS}" "${WLT_ADB_FALLBACK_ENDPOINTS}" "${effective_endpoints}" "${WLT_ADB_CONNECT_TIMEOUT_SEC}" \
    > "${WLT_OUT_DIR}/session_meta.txt"
  if [[ -n "${WLT_SCENARIOS}" ]]; then
    read -r -a scenario_specs <<< "${WLT_SCENARIOS}"
  else
    for cid in ${WLT_CONTAINER_IDS}; do
      scenario_specs+=("container-${cid}:${cid}")
    done
  fi
  for spec in "${scenario_specs[@]}"; do
    [[ "${spec}" == *:* ]] || continue
    label="${spec%%:*}"
    cid="${spec##*:}"
    safe_label="$(sanitize_label "${label}")"
    scenario_dir="${WLT_OUT_DIR}/${safe_label}"
    trace_id="${safe_label}-adb-offline-stub-$(date +%s)"
    mkdir -p "${scenario_dir}"
    printf 'label=%s\nsafe_label=%s\ncontainer_id=%s\ntime=%s\nadb_stub_active=1\n' \
      "${label}" "${safe_label}" "${cid}" "${now}" > "${scenario_dir}/scenario_meta.txt"
    printf '%s\n' "${trace_id}" > "${scenario_dir}/trace_id.txt"
    cat > "${scenario_dir}/wait-status.txt" <<EOF
trace_id=${trace_id}
elapsed_sec=0
intent_elapsed_sec=0
post_intent_elapsed_sec=0
phase=adb_offline_stub
timed_out_phase=adb_offline_stub
intent_seen_at_sec=-1
submit_seen_at_sec=-1
terminal_seen_at_sec=-1
saw_intent=0
saw_submit=0
saw_terminal=0
adb_stub_active=1
blocker=no_active_adb_device
EOF
    cat > "${scenario_dir}/runtime-log-assembler.summary.txt" <<EOF
runtime_log_assembler_summary
issue_count=1
max_severity=high
category_counts=adb_offline_stub:1
library_counts=adb:1
EOF
    cat > "${scenario_dir}/runtime-log-assembler.md" <<EOF
# Runtime Log Assembler

- trace_id: \`${trace_id}\`
- adb_stub_active: \`1\`
- blocker: \`no_active_adb_device\`

No device-backed runtime capture was performed. The effective ADB fallback
endpoint list was exhausted before launch: \`${effective_endpoints}\`.
EOF
  done
  cat > "${WLT_OUT_DIR}/OFFLINE_STUB.md" <<EOF
# ADB Offline Stub

- time: \`${now}\`
- package: \`${WLT_PACKAGE}\`
- configured fallback endpoints: \`${WLT_ADB_FALLBACK_ENDPOINTS}\`
- effective fallback endpoints: \`${effective_endpoints}\`
- status: no active ADB device after fallback attempts

This artifact is a blocker record, not a successful device run.
EOF
  log "ADB unavailable; wrote offline stub artifacts to ${WLT_OUT_DIR}"
}

logcat_has_trace_event() {
  local file="$1"
  local trace_id="$2"
  local event_id="$3"
  grep -F "\"trace_id\":\"${trace_id}\"" "${file}" 2>/dev/null \
    | grep -F "\"event_id\":\"${event_id}\"" >/dev/null 2>&1
}

resolve_app_ps_user() {
  local app_ps_user
  app_ps_user="$(adb_s shell "run-as ${WLT_PACKAGE} id -un" 2>/dev/null | tr -d '\r' | awk 'NF{print; exit}')"
  if [[ -n "${app_ps_user}" ]]; then
    printf '%s\n' "${app_ps_user}"
    return 0
  fi
  adb_s shell "ps -A -o USER,NAME,ARGS | awk 'NR>1 && (\$2==\"${WLT_PACKAGE}\" || \$0 ~ /${WLT_PACKAGE//./\\.}/) {print \$1; exit}'" | tr -d '\r'
}

start_direct_route() {
  local container_id="$1"
  local trace_suffix="${2:-}"
  local scenario_dir="${3:-}"
  local trace_id="complete-${container_id}-$(date +%s)"
  local start_out=""
  local rc=0
  [[ -n "${trace_suffix}" ]] && trace_id="${trace_suffix}-$(date +%s)"
  log "Start direct forensic route container=${container_id} trace=${trace_id}"
  if [[ -n "${scenario_dir}" ]]; then
    start_out="${scenario_dir}/start-activity.txt"
  fi
  if command -v timeout >/dev/null 2>&1; then
    if [[ -n "${start_out}" ]]; then
      timeout --foreground "${WLT_ACTIVITY_START_HOST_TIMEOUT_SEC}" \
        adb -s "${ADB_SERIAL_PICKED}" shell am start -W -S \
          -n "${WLT_PACKAGE}/${WLT_ACTIVITY}" \
          --ez forensic_mode true \
          --es forensic_trace_id "${trace_id}" \
          --es forensic_route_source direct_diag_adb \
          --ez forensic_skip_playtime true \
          --ei container_id "${container_id}" > "${start_out}" 2>&1 || rc=$?
    else
      timeout --foreground "${WLT_ACTIVITY_START_HOST_TIMEOUT_SEC}" \
        adb -s "${ADB_SERIAL_PICKED}" shell am start -W -S \
          -n "${WLT_PACKAGE}/${WLT_ACTIVITY}" \
          --ez forensic_mode true \
          --es forensic_trace_id "${trace_id}" \
          --es forensic_route_source direct_diag_adb \
          --ez forensic_skip_playtime true \
          --ei container_id "${container_id}" >/dev/null 2>&1 || rc=$?
    fi
  else
    if [[ -n "${start_out}" ]]; then
      adb_s shell am start -S \
        -n "${WLT_PACKAGE}/${WLT_ACTIVITY}" \
        --ez forensic_mode true \
        --es forensic_trace_id "${trace_id}" \
        --es forensic_route_source direct_diag_adb \
        --ez forensic_skip_playtime true \
        --ei container_id "${container_id}" > "${start_out}" 2>&1 || rc=$?
    else
      adb_s shell am start -S \
        -n "${WLT_PACKAGE}/${WLT_ACTIVITY}" \
        --ez forensic_mode true \
        --es forensic_trace_id "${trace_id}" \
        --es forensic_route_source direct_diag_adb \
        --ez forensic_skip_playtime true \
        --ei container_id "${container_id}" >/dev/null 2>&1 || rc=$?
    fi
  fi
  if [[ -n "${scenario_dir}" ]]; then
    printf 'trace_id=%s\nactivity_start_host_rc=%s\nactivity_start_host_timeout_sec=%s\n' \
      "${trace_id}" "${rc}" "${WLT_ACTIVITY_START_HOST_TIMEOUT_SEC}" \
      > "${scenario_dir}/start-activity-meta.txt"
  fi
  if [[ "${rc}" -eq 124 ]]; then
    log "Activity start host wait timed out for container=${container_id} trace=${trace_id}; continuing with trace-based capture"
  elif [[ "${rc}" -ne 0 ]]; then
    log "Activity start returned rc=${rc} for container=${container_id} trace=${trace_id}; continuing with trace-based capture"
  fi
  printf '%s\n' "${trace_id}"
}

wait_for_trace_settle() {
  local trace_id="$1"
  local out_dir="$2"
  local elapsed_total=0
  local elapsed_intent=0
  local elapsed_post=0
  local saw_intent=0
  local saw_submit=0
  local saw_terminal=0
  local phase="intent"
  local timed_out_phase="none"
  local intent_seen_at="-1"
  local submit_seen_at="-1"
  local terminal_seen_at="-1"
  while :; do
    adb_s logcat -d > "${out_dir}/_poll.logcat" 2>/dev/null || true
    if logcat_has_trace_event "${out_dir}/_poll.logcat" "${trace_id}" "ROUTE_INTENT_RECEIVED"; then
      saw_intent=1
      [[ "${intent_seen_at}" == "-1" ]] && intent_seen_at="${elapsed_total}"
    fi
    if logcat_has_trace_event "${out_dir}/_poll.logcat" "${trace_id}" "LAUNCH_EXEC_SUBMIT"; then
      saw_submit=1
      [[ "${submit_seen_at}" == "-1" ]] && submit_seen_at="${elapsed_total}"
    fi
    if logcat_has_trace_event "${out_dir}/_poll.logcat" "${trace_id}" "LAUNCH_EXEC_EXIT" \
      || logcat_has_trace_event "${out_dir}/_poll.logcat" "${trace_id}" "SESSION_EXIT_COMPLETED"; then
      saw_terminal=1
      [[ "${terminal_seen_at}" == "-1" ]] && terminal_seen_at="${elapsed_total}"
      break
    fi
    if (( saw_intent || saw_submit )); then
      phase="post_intent"
    fi
    if [[ "${phase}" == "post_intent" ]]; then
      if (( elapsed_post >= WLT_WAIT_POST_INTENT_TIMEOUT_SEC )); then
        timed_out_phase="post_intent"
        break
      fi
    else
      if (( elapsed_intent >= WLT_WAIT_INTENT_TIMEOUT_SEC )); then
        timed_out_phase="intent"
        break
      fi
    fi
    sleep "${WLT_POLL_SEC}"
    elapsed_total=$((elapsed_total + WLT_POLL_SEC))
    if [[ "${phase}" == "post_intent" ]]; then
      elapsed_post=$((elapsed_post + WLT_POLL_SEC))
    else
      elapsed_intent=$((elapsed_intent + WLT_POLL_SEC))
    fi
  done
  printf 'trace_id=%s\nelapsed_sec=%s\nintent_elapsed_sec=%s\npost_intent_elapsed_sec=%s\nphase=%s\ntimed_out_phase=%s\nintent_seen_at_sec=%s\nsubmit_seen_at_sec=%s\nterminal_seen_at_sec=%s\nsaw_intent=%s\nsaw_submit=%s\nsaw_terminal=%s\n' \
    "${trace_id}" "${elapsed_total}" "${elapsed_intent}" "${elapsed_post}" "${phase}" "${timed_out_phase}" \
    "${intent_seen_at}" "${submit_seen_at}" "${terminal_seen_at}" "${saw_intent}" "${saw_submit}" "${saw_terminal}" > "${out_dir}/wait-status.txt"
  if [[ -f "${out_dir}/_poll.logcat" ]]; then
    mv -f "${out_dir}/_poll.logcat" "${out_dir}/poll.logcat"
  fi
}

dump_ui() {
  local out_dir="$1"
  [[ "${WLT_CAPTURE_UI}" == "1" ]] || return 0
  adb_capture_timeout "/dev/null" shell uiautomator dump /sdcard/winlator_ui.xml
  adb_capture_timeout "${out_dir}/ui.xml" shell cat /sdcard/winlator_ui.xml
  adb_capture_timeout "${out_dir}/screen.png" exec-out screencap -p
}

collect_logcat_filtered() {
  local out_file="$1"
  local raw_file="${out_file}.raw"
  adb_capture_timeout "${raw_file}" logcat -d
  grep -E \
    'ForensicLogger|freewine-|[[:space:]]linker[[:space:]]*:|RUNTIME_(GRAPHICS_SUITABILITY|PERF_PRESET_DOWNGRADED|UPSCALER_GUARD_APPLIED|SWFG_EFFECTIVE_CONFIG|SWFG_DISABLED_BY_GUARD|CONTAINER_UPSCALE_CONFIG_APPLIED|UPSCALE_LAUNCH_ENV_NORMALIZED|GLIBC_COMPAT_APPLIED|GLIBC_PRELOAD_STRIPPED|SUBSYSTEM_SNAPSHOT|LOGGING_CONTRACT_SNAPSHOT|LIBRARY_COMPONENT_SIGNAL|LIBRARY_COMPONENT_CONFLICT|LIBRARY_CONFLICT_(SNAPSHOT|DETECTED)|DX_CAPABILITY_ENVELOPE|DX_ROUTE_POLICY|UPSCALE_RUNTIME_MATRIX)|AERO_(RUNTIME|LIBRARY|DXVK|VKD3D|UPSCALE|TURNIP|X11)_|WINLATOR_SIGNAL_|LAUNCH_EXEC_(SUBMIT|EXIT)|SESSION_EXIT_|PARSER_(LOAD_SUMMARY|CONTAINER_MISSING_CONFIG)|ROUTE_(INTENT_RECEIVED|RESOLVED)|CONTAINER_CREATE_|undefined symbol|dlopen failed' \
    "${raw_file}" | tail -n "${WLT_LOGCAT_LINES}" > "${out_file}" || true
  rm -f "${raw_file}"
}

collect_linker_logcat() {
  local full_file="$1"
  local out_file="$2"
  grep -E '[[:space:]]linker[[:space:]]*:' "${full_file}" > "${out_file}" 2>/dev/null || true
}

collect_runtime_conflict_contour() {
  local out_dir="$1"
  [[ "${WLT_CAPTURE_CONFLICT_LOGS}" == "1" ]] || return 0

  local contour_file="${out_dir}/logcat-runtime-conflict-contour.txt"
  local source_files=(
    "${out_dir}/logcat-full.txt"
    "${out_dir}/forensics-jsonl-tail.txt"
    "${out_dir}/runtime-logs"
  )

  {
    rg -n \
      'RUNTIME_(SUBSYSTEM_SNAPSHOT|LOGGING_CONTRACT_SNAPSHOT|LIBRARY_COMPONENT_SIGNAL|LIBRARY_COMPONENT_CONFLICT|LIBRARY_CONFLICT_(SNAPSHOT|DETECTED)|DX_CAPABILITY_ENVELOPE|UPSCALE_RUNTIME_MATRIX)|AERO_(RUNTIME|LIBRARY|DXVK|VKD3D|UPSCALE|TURNIP|X11)_|WINLATOR_SIGNAL_' \
      "${source_files[@]}" -S 2>/dev/null || true
  } > "${contour_file}"

  python3 - "${contour_file}" > "${out_dir}/runtime-conflict-contour.summary.txt" <<'PY'
import re
import sys
from pathlib import Path

path = Path(sys.argv[1])
text = path.read_text(encoding="utf-8", errors="ignore") if path.exists() else ""
tokens = {
    "runtime_subsystem_snapshot": r"RUNTIME_SUBSYSTEM_SNAPSHOT",
    "runtime_logging_contract_snapshot": r"RUNTIME_LOGGING_CONTRACT_SNAPSHOT",
    "runtime_library_component_signal": r"RUNTIME_LIBRARY_COMPONENT_SIGNAL",
    "runtime_library_component_conflict": r"RUNTIME_LIBRARY_COMPONENT_CONFLICT",
    "runtime_library_conflict_snapshot": r"RUNTIME_LIBRARY_CONFLICT_SNAPSHOT",
    "runtime_library_conflict_detected": r"RUNTIME_LIBRARY_CONFLICT_DETECTED",
    "aero_runtime_markers": r"AERO_RUNTIME_",
    "aero_library_markers": r"AERO_LIBRARY_",
    "aero_dxvk_markers": r"AERO_DXVK_",
    "aero_vkd3d_markers": r"AERO_VKD3D_",
    "aero_upscale_markers": r"AERO_UPSCALE_",
    "aero_turnip_markers": r"AERO_TURNIP_",
    "aero_x11_markers": r"AERO_X11_",
    "signal_input_markers": r"WINLATOR_SIGNAL_INPUT_",
}
print("runtime_conflict_contour")
for key, pattern in tokens.items():
    print(f"{key}={len(re.findall(pattern, text))}")
PY
}

collect_app_snapshots() {
  local out_dir="$1"
  local containers_list_file="${out_dir}/containers-list.txt"
  local wine_profiles_index="${out_dir}/wine-profiles.index.txt"
  local forensics_tail_index="${out_dir}/forensics-jsonl-tail.index.txt"
  local shared_prefs_index="${out_dir}/shared-prefs.index.txt"
  mkdir -p "${out_dir}"
  # Android toybox find doesn't support -maxdepth on many devices.
  adb_capture_timeout "${containers_list_file}" shell \
    "run-as ${WLT_PACKAGE} find ${WLT_IMAGEFS_HOME_DIR} -type f -name .container"
  : > "${out_dir}/containers-json.txt"
  while IFS= read -r path; do
    [[ -n "${path}" ]] || continue
    printf '===== %s =====\n' "${path}" >> "${out_dir}/containers-json.txt"
    adb_capture_timeout "${out_dir}/.tmp-container-cat.txt" exec-out run-as "${WLT_PACKAGE}" cat "${path}"
    cat "${out_dir}/.tmp-container-cat.txt" >> "${out_dir}/containers-json.txt"
    printf '\n' >> "${out_dir}/containers-json.txt"
  done < <(tr -d '\r' < "${containers_list_file}" | sort)
  rm -f "${out_dir}/.tmp-container-cat.txt"
  adb_capture_timeout "${out_dir}/app-log-files.txt" shell \
    "run-as ${WLT_PACKAGE} find ${WLT_APP_FILES_DIR}/Winlator/logs -type f 2>/dev/null"
  adb_capture_timeout "${out_dir}/forensics-files.txt" shell \
    "run-as ${WLT_PACKAGE} find ${WLT_APP_FILES_DIR}/Winlator/logs/forensics -type f 2>/dev/null"
  adb_capture_timeout "${wine_profiles_index}" shell \
    "run-as ${WLT_PACKAGE} find ${WLT_WINE_CONTENTS_DIR} -type f -name profile.json 2>/dev/null"
  : > "${out_dir}/wine-profiles.txt"
  while IFS= read -r path; do
    [[ -n "${path}" ]] || continue
    printf '===== %s =====\n' "${path}" >> "${out_dir}/wine-profiles.txt"
    adb_capture_timeout "${out_dir}/.tmp-profile-cat.txt" exec-out run-as "${WLT_PACKAGE}" cat "${path}"
    cat "${out_dir}/.tmp-profile-cat.txt" >> "${out_dir}/wine-profiles.txt"
    printf '\n' >> "${out_dir}/wine-profiles.txt"
  done < <(tr -d '\r' < "${wine_profiles_index}" | sort)
  rm -f "${out_dir}/.tmp-profile-cat.txt"
  adb_capture_timeout "${forensics_tail_index}" shell \
    "run-as ${WLT_PACKAGE} find ${WLT_APP_FILES_DIR}/Winlator/logs/forensics -type f -name '*.jsonl' 2>/dev/null"
  : > "${out_dir}/forensics-jsonl-tail.txt"
  while IFS= read -r path; do
    [[ -n "${path}" ]] || continue
    printf '===== %s =====\n' "${path}" >> "${out_dir}/forensics-jsonl-tail.txt"
    adb_capture_timeout "${out_dir}/.tmp-forensics-tail.txt" shell \
      "run-as ${WLT_PACKAGE} tail -n 200 ${path}"
    cat "${out_dir}/.tmp-forensics-tail.txt" >> "${out_dir}/forensics-jsonl-tail.txt"
    printf '\n' >> "${out_dir}/forensics-jsonl-tail.txt"
  done < <(tr -d '\r' < "${forensics_tail_index}" | sort)
  rm -f "${out_dir}/.tmp-forensics-tail.txt"
  if [[ "${WLT_CAPTURE_PREFS}" == "1" ]]; then
    adb_capture_timeout "${shared_prefs_index}" shell \
      "run-as ${WLT_PACKAGE} find ${WLT_APP_DATA_DIR}/shared_prefs -type f -name '*.xml' 2>/dev/null"
    : > "${out_dir}/shared-prefs.xml"
    while IFS= read -r path; do
      [[ -n "${path}" ]] || continue
      printf '===== %s =====\n' "${path}" >> "${out_dir}/shared-prefs.xml"
      adb_capture_timeout "${out_dir}/.tmp-pref-cat.txt" exec-out run-as "${WLT_PACKAGE}" cat "${path}"
      cat "${out_dir}/.tmp-pref-cat.txt" >> "${out_dir}/shared-prefs.xml"
      printf '\n' >> "${out_dir}/shared-prefs.xml"
    done < <(tr -d '\r' < "${shared_prefs_index}" | sort)
    rm -f "${out_dir}/.tmp-pref-cat.txt"
  fi
}

snapshot_sdcard_runtime_index() {
  local out_file="$1"
  adb_capture_timeout "${out_file}" shell \
    "sh -c 'for d in ${WLT_RUNTIME_LOG_ROOTS}; do [ -d \"\$d\" ] || continue; find \"\$d\" -maxdepth 1 -type f 2>/dev/null; done | sort -u'"
}

snapshot_app_private_runtime_index() {
  local out_file="$1"
  adb_capture_timeout "${out_file}" shell \
    "run-as ${WLT_PACKAGE} find ${WLT_APP_PRIVATE_RUNTIME_LOG_ROOT_ABS} -type f -name '*.txt' 2>/dev/null"
}

snapshot_prefix_forensic_index() {
  local out_file="$1"
  local raw_file="${out_file}.all"
  adb_capture_timeout "${raw_file}" shell \
    "run-as ${WLT_PACKAGE} find ${WLT_IMAGEFS_HOME_DIR} -type f 2>/dev/null"
  grep -E '/(\.freewine-|freewine-).*[.]log$' "${raw_file}" > "${out_file}" || true
  rm -f "${raw_file}"
}

collect_sdcard_runtime_logs() {
  local out_dir="$1"
  local before_index="${2:-}"
  local after_index="${out_dir}/sdcard-runtime-logs-index.txt"
  adb_capture_timeout "${after_index}" shell \
    "sh -c 'for d in ${WLT_RUNTIME_LOG_ROOTS}; do [ -d \"\$d\" ] || continue; find \"\$d\" -maxdepth 1 -type f 2>/dev/null; done | sort -u'"
  [[ "${WLT_CAPTURE_RUNTIME_CONTENTS}" == "1" ]] || return 0
  mkdir -p "${out_dir}/runtime-logs"
  if [[ -n "${before_index}" && -f "${before_index}" ]]; then
    comm -13 <(sort "${before_index}") <(sort "${after_index}") > "${out_dir}/sdcard-runtime-logs-new.txt" || true
  else
    cp "${after_index}" "${out_dir}/sdcard-runtime-logs-new.txt" 2>/dev/null || true
  fi
  : > "${out_dir}/sdcard-runtime-logs-new-ls.txt"
  while IFS= read -r path; do
    case "${path}" in
      /sdcard/Ae.solator/logs/*|/storage/emulated/0/Ae.solator/logs/*|/sdcard/Winlator/logs/*) ;;
      *) continue ;;
    esac
    local base
    base="external__$(basename "${path}")"
    adb_capture_append_timeout "${out_dir}/sdcard-runtime-logs-new-ls.txt" shell "ls -l '${path}'"
    adb_capture_timeout "${out_dir}/runtime-logs/${base}" shell "cat '${path}'"
  done < "${out_dir}/sdcard-runtime-logs-new.txt"
}

collect_app_private_runtime_logs() {
  local out_dir="$1"
  local before_index="${2:-}"
  local after_index="${out_dir}/app-private-runtime-logs-index.txt"
  local new_index="${out_dir}/app-private-runtime-logs-new.txt"
  local ls_file="${out_dir}/app-private-runtime-logs-new-ls.txt"
  snapshot_app_private_runtime_index "${after_index}"
  [[ "${WLT_CAPTURE_RUNTIME_CONTENTS}" == "1" ]] || return 0
  mkdir -p "${out_dir}/runtime-logs"
  if [[ -n "${before_index}" && -f "${before_index}" ]]; then
    comm -13 <(sort "${before_index}") <(sort "${after_index}") > "${new_index}" || true
  else
    cp "${after_index}" "${new_index}" 2>/dev/null || true
  fi
  : > "${ls_file}"
  while IFS= read -r path; do
    [[ "${path}" == "${WLT_APP_PRIVATE_RUNTIME_LOG_ROOT_ABS}"/* ]] || continue
    local base
    base="app-private__$(basename "${path}")"
    adb_capture_append_timeout "${ls_file}" shell \
      "run-as ${WLT_PACKAGE} ls -l ${path}"
    adb_capture_timeout "${out_dir}/runtime-logs/${base}" exec-out \
      run-as "${WLT_PACKAGE}" cat "${path}"
  done < "${new_index}"
}

collect_prefix_forensic_logs() {
  local out_dir="$1"
  local before_index="${2:-}"
  local index_file="${out_dir}/prefix-forensic-logs-index.txt"
  local new_index="${out_dir}/prefix-forensic-logs-new.txt"
  local ls_file="${out_dir}/prefix-forensic-logs-ls.txt"
  local base

  [[ "${WLT_CAPTURE_RUNTIME_CONTENTS}" == "1" ]] || return 0
  mkdir -p "${out_dir}/runtime-logs"
  : > "${ls_file}"
  snapshot_prefix_forensic_index "${index_file}"

  if [[ -n "${before_index}" && -f "${before_index}" ]]; then
    comm -13 <(sort "${before_index}") <(sort "${index_file}") > "${new_index}" || true
  else
    cp "${index_file}" "${new_index}" 2>/dev/null || true
  fi

  while IFS= read -r path; do
    [[ "${path}" == "${WLT_IMAGEFS_HOME_DIR}"/* ]] || continue
    base="prefix__$(sanitize_label "${path}")"
    adb_capture_append_timeout "${ls_file}" shell \
      "run-as ${WLT_PACKAGE} ls -l ${path}"
    adb_capture_timeout "${out_dir}/runtime-logs/${base}.txt" exec-out \
      run-as "${WLT_PACKAGE}" cat "${path}"
  done < "${new_index}"
}

assemble_runtime_logs() {
  local out_dir="$1"
  python3 "${ROOT_DIR}/ci/winlator/forensic-runtime-log-assembler.py" \
    --input "${out_dir}" \
    --output-prefix "${out_dir}/runtime-log-assembler"
  python3 "${ROOT_DIR}/ci/winlator/forensic-issue-bundle.py" \
    --scenario-dir "${out_dir}" \
    --root-mode nonroot >/dev/null
}

collect_device_state() {
  local out_dir="$1"
  adb_capture_timeout "${out_dir}/dumpsys-activity-top.txt" shell dumpsys activity top
  adb_capture_timeout "${out_dir}/pid.txt" shell pidof "${WLT_PACKAGE}"
  adb_capture_timeout "${out_dir}/linker-prop.txt" shell getprop "$(linker_prop_name)"
}

sample_live_ptrace_targets() {
  local out_dir="$1"
  local app_ps_user="$2"
  local targets_file="${out_dir}/runas-ptrace.targets.tsv"
  local seen_file="${out_dir}/runas-ptrace.seen"
  local pid name args safe
  [[ "${WLT_CAPTURE_RUNAS_PTRACE}" == "1" ]] || return 0
  while IFS=$'\t' read -r pid name args; do
    [[ -n "${pid}" ]] || continue
    if grep -qx "${pid}" "${seen_file}" 2>/dev/null; then
      continue
    fi
    printf '%s\n' "${pid}" >> "${seen_file}"
    safe="$(sanitize_label "${name}-${pid}")"
    printf '%s\t%s\t%s\n' "${pid}" "${name}" "${args}" >> "${targets_file}"
    log "run-as ptrace sampler pid=${pid} name=${name} ${args}"
    ADB_SERIAL="${ADB_SERIAL_PICKED}" \
    APP_PS_USER="${app_ps_user}" \
    WLT_PACKAGE="${WLT_PACKAGE}" \
    WLT_RUNAS_SAMPLER_SAMPLES="${WLT_RUNAS_SAMPLER_SAMPLES}" \
    WLT_RUNAS_SAMPLER_INTERVAL_MS="${WLT_RUNAS_SAMPLER_INTERVAL_MS}" \
    WLT_RUNAS_SAMPLER_OUT="${out_dir}/runas-ptrace/${safe}.jsonl" \
    "${ROOT_DIR}/ci/winlator/adb-runas-ptrace-sampler.sh" "${pid}" \
      < /dev/null >/dev/null 2>&1 || true
  done < <(
    adb_s shell "ps -A -o USER,PID,NAME,ARGS | awk 'BEGIN{IGNORECASE=1} NR>1 && \$1==\"${app_ps_user}\" && \$0 ~ /${WLT_RUNAS_PTRACE_PATTERNS}/ { printf \"%s\\t%s\\t\", \$2, \$3; \$1=\$2=\$3=\"\"; sub(/^[ \t]+/, \"\", \$0); print \$0; }'" \
      | tr -d '\r'
  )
}

capture_emergence_snapshot() {
  local out_dir="$1"
  local app_ps_user="$2"
  local sample_file="${out_dir}/ps-emergence-samples.txt"
  {
    printf '=== sample %s ===\n' "$(iso_now)"
    adb_s shell "ps -A -o USER,PID,PPID,NAME,ARGS | awk 'BEGIN{IGNORECASE=1} NR==1{print;next} {u=\$1; n=\$4; l=\$0; if ((u==\"${app_ps_user}\" || l ~ /${WLT_PACKAGE//./[.]}/) && (n ~ /(wine|wineserver|wineboot|services|explorer|jwm|box64|fex|xserver|dxvk|vkd3d|winhandler|wfm|linker64)/ || l ~ /${WLT_PACKAGE//./[.]}/)) print}'"
    printf '\n'
  } >> "${sample_file}" 2>/dev/null || true
  sample_live_ptrace_targets "${out_dir}" "${app_ps_user}"
}

collect_live_emergence() {
  local out_dir="$1"
  local sample_file="${out_dir}/ps-emergence-samples.txt"
  local app_ps_user
  local i
  app_ps_user="$(resolve_app_ps_user)"
  mkdir -p "${out_dir}/runas-ptrace"
  : > "${sample_file}"
  : > "${out_dir}/runas-ptrace.targets.tsv"
  : > "${out_dir}/runas-ptrace.seen"
  for ((i=1; i<=WLT_PROCESS_SAMPLES; i++)); do
    capture_emergence_snapshot "${out_dir}" "${app_ps_user}"
    sleep "${WLT_PROCESS_SAMPLE_SEC}"
  done
  local ws_count
  local wine_count
  local explorer_count
  local services_count
  local present=0
  ws_count="$(grep -i -c 'wineserver' "${sample_file}" 2>/dev/null || true)"
  wine_count="$(grep -E -i -c '\bwine\b' "${sample_file}" 2>/dev/null || true)"
  explorer_count="$(grep -i -c 'explorer' "${sample_file}" 2>/dev/null || true)"
  services_count="$(grep -i -c 'services' "${sample_file}" 2>/dev/null || true)"
  if [[ "${ws_count}" -gt 0 || "${wine_count}" -gt 0 || "${explorer_count}" -gt 0 || "${services_count}" -gt 0 ]]; then
    present=1
  fi
  printf 'app_ps_user=%s\nps_wineserver_count=%s\nps_wine_count=%s\nps_explorer_count=%s\nps_services_count=%s\nwine_process_present=%s\n' \
    "${app_ps_user}" "${ws_count}" "${wine_count}" "${explorer_count}" "${services_count}" "${present}" > "${out_dir}/process-emergence.env"
}

collect_live_emergence_tail() {
  local out_dir="$1"
  local app_ps_user
  app_ps_user="$(resolve_app_ps_user)"
  [[ -n "${app_ps_user}" ]] || return 0
  capture_emergence_snapshot "${out_dir}" "${app_ps_user}"
  sleep 1
  capture_emergence_snapshot "${out_dir}" "${app_ps_user}"
}

symbolize_live_emergence() {
  local out_dir="$1"
  [[ "${WLT_CAPTURE_RUNAS_PTRACE}" == "1" ]] || return 0
  [[ "${WLT_RUNAS_PTRACE_SYMBOLIZE}" == "1" ]] || return 0
  [[ -d "${out_dir}/runas-ptrace" ]] || return 0
  python3 "${ROOT_DIR}/ci/winlator/forensic-runas-ptrace-symbolize.py" \
    --scenario-dir "${out_dir}" >/dev/null 2>&1 || true
}

collect_artifact_picker_ui() {
  local out_dir="$1"
  mkdir -p "${out_dir}"
  adb_capture_timeout "/dev/null" logcat -c
  adb_capture_timeout "/dev/null" shell am start -n "${WLT_PACKAGE}/com.winlator.cmod.MainActivity"
  sleep 2
  dump_ui "${out_dir}"
  collect_device_state "${out_dir}"
  adb_capture_timeout "${out_dir}/logcat-full.txt" logcat -d
  collect_logcat_filtered "${out_dir}/logcat-filtered.txt"
  collect_linker_logcat "${out_dir}/logcat-full.txt" "${out_dir}/logcat-linker.txt"
  adb_capture_timeout "${out_dir}/wine-profiles.index.txt" shell \
    "run-as ${WLT_PACKAGE} find ${WLT_WINE_CONTENTS_DIR} -type f -name profile.json 2>/dev/null"
  : > "${out_dir}/wine-profiles.txt"
  while IFS= read -r path; do
    [[ -n "${path}" ]] || continue
    printf '===== %s =====\n' "${path}" >> "${out_dir}/wine-profiles.txt"
    adb_capture_timeout "${out_dir}/.tmp-ui-profile-cat.txt" exec-out run-as "${WLT_PACKAGE}" cat "${path}"
    cat "${out_dir}/.tmp-ui-profile-cat.txt" >> "${out_dir}/wine-profiles.txt"
    printf '\n' >> "${out_dir}/wine-profiles.txt"
  done < <(tr -d '\r' < "${out_dir}/wine-profiles.index.txt" | sort)
  rm -f "${out_dir}/.tmp-ui-profile-cat.txt"
}

main() {
  local cid trace_id scenario_dir
  require_cmd adb
  require_cmd python3
  [[ "${WLT_CAPTURE_CONFLICT_LOGS}" =~ ^[01]$ ]] || fail "WLT_CAPTURE_CONFLICT_LOGS must be 0 or 1"
  [[ "${WLT_ADB_OFFLINE_STUB}" =~ ^[01]$ ]] || fail "WLT_ADB_OFFLINE_STUB must be 0 or 1"
  mkdir -p "${WLT_OUT_DIR}"
  ADB_SERIAL_PICKED="$(pick_serial)"
  if [[ -z "${ADB_SERIAL_PICKED}" ]]; then
    if [[ "${WLT_ADB_OFFLINE_STUB}" == "1" ]]; then
      write_adb_offline_stub
      return 0
    fi
    fail "No active adb device"
  fi
  export ADB_SERIAL_PICKED
  WLT_APP_DATA_DIR="$(resolve_app_data_dir)"
  WLT_APP_FILES_DIR="${WLT_APP_DATA_DIR}/files"
  WLT_APP_PRIVATE_RUNTIME_LOG_ROOT_ABS="${WLT_APP_FILES_DIR}/Winlator/logs"
  WLT_IMAGEFS_HOME_DIR="${WLT_APP_FILES_DIR}/imagefs/home"
  WLT_WINE_CONTENTS_DIR="${WLT_APP_FILES_DIR}/contents/Wine"
  trap 'disable_linker_telemetry || true' EXIT INT TERM

  log "Using device ${ADB_SERIAL_PICKED}"
  log "App data ${WLT_APP_DATA_DIR}"
  enable_linker_telemetry
  printf 'package=%s\nserial=%s\ntime=%s\ncontainer_ids=%s\nscenarios=%s\n' \
    "${WLT_PACKAGE}" "${ADB_SERIAL_PICKED}" "$(iso_now)" "${WLT_CONTAINER_IDS}" "${WLT_SCENARIOS}" > "${WLT_OUT_DIR}/session_meta.txt"
  printf 'capture_linker_telemetry=%s\nlinker_prop=%s\nlinker_flags=%s\n' \
    "${WLT_CAPTURE_LINKER_TELEMETRY}" "$(linker_prop_name)" "${WLT_LINKER_DEBUG_FLAGS}" >> "${WLT_OUT_DIR}/session_meta.txt"
  printf 'app_data_dir=%s\napp_files_dir=%s\napp_runtime_log_root=%s\nimagefs_home_dir=%s\nwine_contents_dir=%s\n' \
    "${WLT_APP_DATA_DIR}" "${WLT_APP_FILES_DIR}" "${WLT_APP_PRIVATE_RUNTIME_LOG_ROOT_ABS}" "${WLT_IMAGEFS_HOME_DIR}" "${WLT_WINE_CONTENTS_DIR}" \
    >> "${WLT_OUT_DIR}/session_meta.txt"

  collect_artifact_picker_ui "${WLT_OUT_DIR}/ui-baseline" || true

  local scenario_specs=()
  if [[ -n "${WLT_SCENARIOS}" ]]; then
    # Format: label:containerId [label2:containerId2 ...]
    # Example: WLT_SCENARIOS="n2_scaleforce:1 wine11_scaleforce:2"
    read -r -a scenario_specs <<< "${WLT_SCENARIOS}"
  else
    for cid in ${WLT_CONTAINER_IDS}; do
      scenario_specs+=("container-${cid}:${cid}")
    done
  fi

  local spec label safe_label
  local scenario_dirs=()
  local -A seen_labels=()
  for spec in "${scenario_specs[@]}"; do
    [[ "${spec}" == *:* ]] || fail "Invalid scenario spec '${spec}' (expected label:containerId)"
    label="${spec%%:*}"
    cid="${spec##*:}"
    [[ -n "${label}" ]] || fail "Scenario label cannot be empty in '${spec}'"
    [[ "${cid}" =~ ^[0-9]+$ ]] || fail "Scenario container id must be numeric in '${spec}'"
    safe_label="$(sanitize_label "${label}")"
    if [[ -n "${seen_labels[${safe_label}]:-}" ]]; then
      fail "Duplicate scenario label after sanitization: '${label}' -> '${safe_label}'"
    fi
    seen_labels["${safe_label}"]=1
    local before_runtime_index
    local before_app_private_runtime_index
    local before_prefix_forensic_index
    scenario_dir="${WLT_OUT_DIR}/${safe_label}"
    mkdir -p "${scenario_dir}"
    before_runtime_index="${scenario_dir}/sdcard-runtime-before.txt"
    before_app_private_runtime_index="${scenario_dir}/app-private-runtime-before.txt"
    before_prefix_forensic_index="${scenario_dir}/prefix-forensic-before.txt"
    local live_sampler_pid=""
    printf 'label=%s\nsafe_label=%s\ncontainer_id=%s\ntime=%s\n' "${label}" "${safe_label}" "${cid}" "$(iso_now)" > "${scenario_dir}/scenario_meta.txt"
    scenario_dirs+=("${scenario_dir}")

    adb_s logcat -c || true
    snapshot_sdcard_runtime_index "${before_runtime_index}"
    snapshot_app_private_runtime_index "${before_app_private_runtime_index}"
    snapshot_prefix_forensic_index "${before_prefix_forensic_index}"
    trace_id="$(start_direct_route "${cid}" "${safe_label}" "${scenario_dir}")"
    printf '%s\n' "${trace_id}" > "${scenario_dir}/trace_id.txt"
    collect_live_emergence "${scenario_dir}" &
    live_sampler_pid=$!
    wait_for_trace_settle "${trace_id}" "${scenario_dir}"
    wait "${live_sampler_pid}" 2>/dev/null || true
    collect_live_emergence_tail "${scenario_dir}" || true

    adb_capture_timeout "${scenario_dir}/logcat-full.txt" logcat -d
    collect_logcat_filtered "${scenario_dir}/logcat-filtered.txt"
    collect_linker_logcat "${scenario_dir}/logcat-full.txt" "${scenario_dir}/logcat-linker.txt"
    collect_device_state "${scenario_dir}"
    dump_ui "${scenario_dir}"
    collect_app_snapshots "${scenario_dir}"
    collect_runtime_conflict_contour "${scenario_dir}"
    collect_sdcard_runtime_logs "${scenario_dir}" "${before_runtime_index}"
    collect_app_private_runtime_logs "${scenario_dir}" "${before_app_private_runtime_index}"
    collect_prefix_forensic_logs "${scenario_dir}" "${before_prefix_forensic_index}"
    assemble_runtime_logs "${scenario_dir}"
  done

  for scenario_dir in "${scenario_dirs[@]}"; do
    symbolize_live_emergence "${scenario_dir}"
  done

  log "Artifacts saved to ${WLT_OUT_DIR}"
}

main "$@"
