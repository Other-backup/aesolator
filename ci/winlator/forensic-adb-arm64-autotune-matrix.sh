#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"

: "${ADB:=adb}"
: "${WLT_PACKAGE:=com.winlator.cmod}"
: "${WLT_ACTIVITY:=com.winlator.cmod.XServerDisplayActivity}"
: "${WLT_CONTAINER_ID:=1}"
: "${WLT_SERIAL:=}"
: "${WLT_CONTENT_NAME:=}"
: "${WLT_REQUESTED_PROFILE:=auto}"
: "${WLT_TRIGGER_LAUNCH:=1}"
: "${WLT_REQUIRE_LOGCAT:=1}"
: "${WLT_REQUIRE_PROFILE_AUTO:=1}"
: "${WLT_FAIL_ON_MISMATCH:=1}"
: "${WLT_LAUNCH_WAIT_SEC:=5}"
: "${WLT_OUT_DIR:=/tmp/forensic-adb-arm64-autotune-$(date +%Y%m%d_%H%M%S)}"
WLT_APP_DATA_DIR=""
WLT_WINE_CONTENTS_DIR=""

log() { printf '[forensic-autotune] %s\n' "$*"; }
warn() { printf '[forensic-autotune][warn] %s\n' "$*" >&2; }
fail() { printf '[forensic-autotune][error] %s\n' "$*" >&2; exit 1; }

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || fail "required command not found: $1"
}

require_bool() {
  [[ "$2" =~ ^[01]$ ]] || fail "$1 must be 0 or 1"
}

require_profile() {
  [[ "$1" =~ ^(auto|conservative|balanced|aggressive)$ ]] || \
    fail "WLT_REQUESTED_PROFILE must be one of: auto, conservative, balanced, aggressive"
}

pick_serial() {
  local serial
  serial="${WLT_SERIAL:-${ADB_SERIAL:-}}"
  if [[ -n "${serial}" ]]; then
    printf '%s\n' "${serial}"
    return 0
  fi
  "${ADB}" devices | awk 'NR>1 && $2=="device" {print $1; exit}'
}

adb_s() {
  if [[ -n "${ADB_SERIAL_PICKED:-}" ]]; then
    "${ADB}" -s "${ADB_SERIAL_PICKED}" "$@"
  else
    "${ADB}" "$@"
  fi
}

trim() {
  local s="$1"
  s="${s#${s%%[![:space:]]*}}"
  s="${s%${s##*[![:space:]]}}"
  printf '%s' "${s}"
}

resolve_app_data_dir() {
  local app_dir
  app_dir="$(adb_s shell "run-as ${WLT_PACKAGE} pwd" | tr -d '\r' | tail -n1)"
  app_dir="$(trim "${app_dir}")"
  [[ "${app_dir}" == /data/* ]] || fail "unexpected app data dir for ${WLT_PACKAGE}: ${app_dir:-<empty>}"
  printf '%s\n' "${app_dir}"
}

collect_soc_props() {
  local out_file="$1"
  local key value cpu_count
  local -a prop_keys=(
    "ro.board.platform"
    "ro.soc.model"
    "ro.soc.manufacturer"
    "ro.hardware"
    "ro.product.board"
  )

  : > "${out_file}"
  for key in "${prop_keys[@]}"; do
    value="$(adb_s shell getprop "${key}" 2>/dev/null | tr -d '\r' | sed 's/[[:space:]]*$//' || true)"
    printf '%s=%s\n' "${key}" "${value}" >> "${out_file}"
  done

  cpu_count="$(adb_s shell getconf _NPROCESSORS_ONLN 2>/dev/null | tr -d '\r' | tr -d '[:space:]' || true)"
  if [[ -z "${cpu_count}" || ! "${cpu_count}" =~ ^[0-9]+$ ]]; then
    cpu_count="$(adb_s shell cat /sys/devices/system/cpu/online 2>/dev/null | tr -d '\r' | tr -d '[:space:]' || true)"
    if [[ "${cpu_count}" =~ ^([0-9]+)-([0-9]+)$ ]]; then
      cpu_count="$((BASH_REMATCH[2] - BASH_REMATCH[1] + 1))"
    else
      cpu_count=0
    fi
  fi
  printf 'cpu_count=%s\n' "${cpu_count}" >> "${out_file}"
}

derive_soc_matrix() {
  local props_file="$1"
  local requested="$2"
  local out_env="$3"
  python3 - "${props_file}" "${requested}" "${out_env}" <<'PY'
import shlex
import sys
from pathlib import Path

props_file = Path(sys.argv[1])
requested = sys.argv[2]
out_env = Path(sys.argv[3])

props = {}
for line in props_file.read_text(encoding="utf-8", errors="ignore").splitlines():
    if "=" not in line:
        continue
    key, value = line.split("=", 1)
    props[key.strip()] = value.strip()

cpu_count = int(props.get("cpu_count", "0") or "0")
info = " ".join(
    [
        props.get("ro.board.platform", ""),
        props.get("ro.soc.model", ""),
        props.get("ro.soc.manufacturer", ""),
        props.get("ro.hardware", ""),
        props.get("ro.product.board", ""),
    ]
).lower()

high_tokens = [
    "sm8750", "sm8650", "sm8635", "sm8550", "sm8475",
    "dimensity 9400", "dimensity 9300", "dimensity 9200",
    "exynos 2400", "exynos 2300",
    "tensor g4", "tensor g3",
    "adreno 830", "adreno 760", "adreno 750",
]
mid_tokens = [
    "sm8450", "sm8350", "sm8250", "sm7325", "sm730",
    "sm778", "sm7150", "sm7125",
    "dimensity 9000", "dimensity 8300", "dimensity 8200",
    "exynos 2200", "exynos 2100",
    "tensor g2", "adreno 740", "adreno 730", "adreno 725",
]

soc_class = "entry"
if any(token in info for token in high_tokens):
    soc_class = "high-end"
elif any(token in info for token in mid_tokens):
    soc_class = "mid-range"
elif cpu_count >= 10:
    soc_class = "high-end"
elif cpu_count >= 8:
    soc_class = "mid-range"

soc_map = {
    "entry": "conservative",
    "mid-range": "balanced",
    "high-end": "aggressive",
}
expected_profile = soc_map[soc_class] if requested == "auto" else requested

rows = [
    ("SOC_CLASS", soc_class),
    ("EXPECTED_PROFILE", expected_profile),
    ("SOC_MAP_ENTRY", "conservative"),
    ("SOC_MAP_MID_RANGE", "balanced"),
    ("SOC_MAP_HIGH_END", "aggressive"),
    ("MATRIX_VERSION", "android-arm64-v1"),
]
out_env.write_text(
    "\n".join(f"{k}={shlex.quote(str(v))}" for k, v in rows) + "\n",
    encoding="utf-8",
)
PY
}

select_profile_json_path() {
  local profile_path
  if [[ -n "${WLT_CONTENT_NAME}" ]]; then
    profile_path="${WLT_WINE_CONTENTS_DIR}/${WLT_CONTENT_NAME}/profile.json"
    adb_s exec-out run-as "${WLT_PACKAGE}" cat "${profile_path}" >/dev/null 2>&1 || \
      fail "profile.json not found for content: ${WLT_CONTENT_NAME}"
    printf '%s\n' "${profile_path}"
    return 0
  fi

  profile_path="$(
    adb_s shell "run-as ${WLT_PACKAGE} find ${WLT_WINE_CONTENTS_DIR} -type f -name profile.json 2>/dev/null" \
      | tr -d '\r' | sort | head -n1
  )"
  [[ -n "${profile_path}" ]] || fail "no installed Wine profile.json found in app-private contents"
  printf '%s\n' "${profile_path}"
}

extract_profile_json() {
  local profile_path="$1"
  local out_json="$2"
  adb_s exec-out run-as "${WLT_PACKAGE}" cat "${profile_path}" > "${out_json}" \
    || fail "failed to read ${profile_path}"
}

extract_profile_runtime_fields() {
  local profile_json="$1"
  local out_env="$2"
  python3 - "${profile_json}" "${out_env}" <<'PY'
import json
import shlex
import sys
from pathlib import Path

profile = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
runtime = profile.get("runtime", {})
autotune = runtime.get("arm64AndroidAutotune", {})
soc_map = autotune.get("socMap", {})

fields = {
    "PROFILE_JSON_NAME": profile.get("name", ""),
    "PROFILE_DEFAULT": autotune.get("defaultProfile", ""),
    "PROFILE_MODE": autotune.get("profileMode", ""),
    "PROFILE_MATRIX_VERSION": autotune.get("matrixVersion", ""),
    "PROFILE_SOC_ENTRY": soc_map.get("entry", ""),
    "PROFILE_SOC_MID_RANGE": soc_map.get("mid-range", ""),
    "PROFILE_SOC_HIGH_END": soc_map.get("high-end", ""),
}

Path(sys.argv[2]).write_text(
    "\n".join(f"{k}={shlex.quote(str(v))}" for k, v in fields.items()) + "\n",
    encoding="utf-8",
)
PY
}

trigger_launch_if_needed() {
  local trace_id
  if [[ "${WLT_TRIGGER_LAUNCH}" != "1" ]]; then
    return 0
  fi

  trace_id="autotune-$(date +%s)"
  adb_s logcat -c || true
  log "starting activity for runtime autotune line (container=${WLT_CONTAINER_ID}, trace=${trace_id})"
  adb_s shell am start \
    -n "${WLT_PACKAGE}/${WLT_ACTIVITY}" \
    --ez forensic_mode true \
    --es forensic_trace_id "${trace_id}" \
    --es forensic_route_source adb_autotune_matrix \
    --ei container_id "${WLT_CONTAINER_ID}" >/dev/null
  sleep "${WLT_LAUNCH_WAIT_SEC}"
}

parse_autotune_logcat_line() {
  local logcat_file="$1"
  local out_env="$2"
  python3 - "${logcat_file}" "${out_env}" <<'PY'
import re
import shlex
import sys
from pathlib import Path

logcat = Path(sys.argv[1]).read_text(encoding="utf-8", errors="ignore").splitlines()
pattern = re.compile(
    r"AEO arm64 autotune: soc=(\S+) profile=(\S+) cpu=(\S+) "
    r"WINEESYNC=(\S*) WINEFSYNC=(\S*) WSI=(\S*) SHADER_CACHE=(\S*)"
)
match_line = ""
groups = None
for line in logcat:
    m = pattern.search(line)
    if m:
        match_line = line
        groups = m.groups()

if not groups:
    Path(sys.argv[2]).write_text("AUTOTUNE_LOG_PRESENT=0\n", encoding="utf-8")
    raise SystemExit(0)

soc, profile, cpu, wineesync, winefsync, wsi, shader_cache = groups
rows = [
    ("AUTOTUNE_LOG_PRESENT", "1"),
    ("AUTOTUNE_LOG_LINE", match_line),
    ("LOG_SOC", soc),
    ("LOG_PROFILE", profile),
    ("LOG_CPU", cpu),
    ("LOG_WINEESYNC", wineesync),
    ("LOG_WINEFSYNC", winefsync),
    ("LOG_WSI", wsi),
    ("LOG_SHADER_CACHE", shader_cache),
]
Path(sys.argv[2]).write_text(
    "\n".join(f"{k}={shlex.quote(str(v))}" for k, v in rows) + "\n",
    encoding="utf-8",
)
PY
}

expected_env_for_profile() {
  case "$1" in
    conservative)
      printf '1\t0\tfifo\t192M\n'
      ;;
    balanced)
      printf '1\t1\tmailbox\t384M\n'
      ;;
    aggressive)
      printf '1\t1\timmediate\t768M\n'
      ;;
    *)
      fail "unsupported expected profile: $1"
      ;;
  esac
}

main() {
  local soc_props_file soc_env_file profile_path profile_json_file profile_env_file
  local logcat_file log_env_file summary_file fail_flag
  local expected_wineesync expected_winefsync expected_wsi expected_shader_cache

  require_cmd "${ADB}"
  require_cmd python3
  require_bool WLT_TRIGGER_LAUNCH "${WLT_TRIGGER_LAUNCH}"
  require_bool WLT_REQUIRE_LOGCAT "${WLT_REQUIRE_LOGCAT}"
  require_bool WLT_REQUIRE_PROFILE_AUTO "${WLT_REQUIRE_PROFILE_AUTO}"
  require_bool WLT_FAIL_ON_MISMATCH "${WLT_FAIL_ON_MISMATCH}"
  require_profile "${WLT_REQUESTED_PROFILE}"

  ADB_SERIAL_PICKED="$(pick_serial)"
  [[ -n "${ADB_SERIAL_PICKED}" ]] || fail "no active adb device"
  WLT_APP_DATA_DIR="$(resolve_app_data_dir)"
  WLT_WINE_CONTENTS_DIR="${WLT_APP_DATA_DIR}/files/contents/Wine"

  mkdir -p "${WLT_OUT_DIR}"
  soc_props_file="${WLT_OUT_DIR}/soc-props.txt"
  soc_env_file="${WLT_OUT_DIR}/soc-matrix.env"
  profile_json_file="${WLT_OUT_DIR}/profile.json"
  profile_env_file="${WLT_OUT_DIR}/profile-runtime.env"
  logcat_file="${WLT_OUT_DIR}/logcat.txt"
  log_env_file="${WLT_OUT_DIR}/autotune-log.env"
  summary_file="${WLT_OUT_DIR}/summary.txt"

  log "device=${ADB_SERIAL_PICKED} package=${WLT_PACKAGE} out=${WLT_OUT_DIR}"

  collect_soc_props "${soc_props_file}"
  derive_soc_matrix "${soc_props_file}" "${WLT_REQUESTED_PROFILE}" "${soc_env_file}"
  # shellcheck disable=SC1090
  source "${soc_env_file}"

  profile_path="$(select_profile_json_path)"
  extract_profile_json "${profile_path}" "${profile_json_file}"
  extract_profile_runtime_fields "${profile_json_file}" "${profile_env_file}"
  # shellcheck disable=SC1090
  source "${profile_env_file}"

  trigger_launch_if_needed
  adb_s logcat -d -v threadtime > "${logcat_file}" 2>&1 || true
  parse_autotune_logcat_line "${logcat_file}" "${log_env_file}"
  # shellcheck disable=SC1090
  source "${log_env_file}"

  IFS=$'\t' read -r expected_wineesync expected_winefsync expected_wsi expected_shader_cache \
    < <(expected_env_for_profile "${EXPECTED_PROFILE}")

  fail_flag=0
  {
    echo "requested_profile=${WLT_REQUESTED_PROFILE}"
    echo "soc_class_detected=${SOC_CLASS}"
    echo "expected_profile=${EXPECTED_PROFILE}"
    echo "profile_json_path=${profile_path}"
    echo "profile_json_name=${PROFILE_JSON_NAME:-}"
    echo "profile_json_default_profile=${PROFILE_DEFAULT:-}"
    echo "profile_json_mode=${PROFILE_MODE:-}"
    echo "profile_json_matrix_version=${PROFILE_MATRIX_VERSION:-}"
    echo "profile_json_soc_map=entry:${PROFILE_SOC_ENTRY:-},mid-range:${PROFILE_SOC_MID_RANGE:-},high-end:${PROFILE_SOC_HIGH_END:-}"
    echo "matrix_version_expected=${MATRIX_VERSION}"
  } > "${summary_file}"

  if [[ "${WLT_REQUIRE_PROFILE_AUTO}" == "1" && "${PROFILE_DEFAULT:-}" != "auto" ]]; then
    echo "check_profile_default=FAIL expected=auto actual=${PROFILE_DEFAULT:-}" >> "${summary_file}"
    fail_flag=1
  else
    echo "check_profile_default=OK" >> "${summary_file}"
  fi

  if [[ "${PROFILE_MODE:-}" != "soc-auto-matrix" ]]; then
    echo "check_profile_mode=FAIL expected=soc-auto-matrix actual=${PROFILE_MODE:-}" >> "${summary_file}"
    fail_flag=1
  else
    echo "check_profile_mode=OK" >> "${summary_file}"
  fi

  if [[ "${PROFILE_MATRIX_VERSION:-}" != "${MATRIX_VERSION}" ]]; then
    echo "check_profile_matrix_version=FAIL expected=${MATRIX_VERSION} actual=${PROFILE_MATRIX_VERSION:-}" >> "${summary_file}"
    fail_flag=1
  else
    echo "check_profile_matrix_version=OK" >> "${summary_file}"
  fi

  if [[ "${PROFILE_SOC_ENTRY:-}" != "conservative" || "${PROFILE_SOC_MID_RANGE:-}" != "balanced" || "${PROFILE_SOC_HIGH_END:-}" != "aggressive" ]]; then
    echo "check_profile_soc_map=FAIL expected=entry:conservative,mid-range:balanced,high-end:aggressive actual=entry:${PROFILE_SOC_ENTRY:-},mid-range:${PROFILE_SOC_MID_RANGE:-},high-end:${PROFILE_SOC_HIGH_END:-}" >> "${summary_file}"
    fail_flag=1
  else
    echo "check_profile_soc_map=OK" >> "${summary_file}"
  fi

  if [[ "${AUTOTUNE_LOG_PRESENT:-0}" != "1" ]]; then
    if [[ "${WLT_REQUIRE_LOGCAT}" == "1" ]]; then
      echo "check_logcat_autotune_line=FAIL line_missing=1" >> "${summary_file}"
      fail_flag=1
    else
      echo "check_logcat_autotune_line=SKIP line_missing=1" >> "${summary_file}"
    fi
  else
    echo "logcat_autotune_line=${AUTOTUNE_LOG_LINE}" >> "${summary_file}"

    if [[ "${LOG_SOC:-}" != "${SOC_CLASS}" ]]; then
      echo "check_log_soc=FAIL expected=${SOC_CLASS} actual=${LOG_SOC:-}" >> "${summary_file}"
      fail_flag=1
    else
      echo "check_log_soc=OK" >> "${summary_file}"
    fi

    if [[ "${LOG_PROFILE:-}" != "${EXPECTED_PROFILE}" ]]; then
      echo "check_log_profile=FAIL expected=${EXPECTED_PROFILE} actual=${LOG_PROFILE:-}" >> "${summary_file}"
      fail_flag=1
    else
      echo "check_log_profile=OK" >> "${summary_file}"
    fi

    if [[ "${LOG_WINEESYNC:-}" != "${expected_wineesync}" ]]; then
      echo "check_log_wineesync=FAIL expected=${expected_wineesync} actual=${LOG_WINEESYNC:-}" >> "${summary_file}"
      fail_flag=1
    else
      echo "check_log_wineesync=OK" >> "${summary_file}"
    fi

    if [[ "${LOG_WINEFSYNC:-}" != "${expected_winefsync}" ]]; then
      echo "check_log_winefsync=FAIL expected=${expected_winefsync} actual=${LOG_WINEFSYNC:-}" >> "${summary_file}"
      fail_flag=1
    else
      echo "check_log_winefsync=OK" >> "${summary_file}"
    fi

    if [[ "${LOG_WSI:-}" != "${expected_wsi}" ]]; then
      echo "check_log_wsi=FAIL expected=${expected_wsi} actual=${LOG_WSI:-}" >> "${summary_file}"
      fail_flag=1
    else
      echo "check_log_wsi=OK" >> "${summary_file}"
    fi

    if [[ "${LOG_SHADER_CACHE:-}" != "${expected_shader_cache}" ]]; then
      echo "check_log_shader_cache=FAIL expected=${expected_shader_cache} actual=${LOG_SHADER_CACHE:-}" >> "${summary_file}"
      fail_flag=1
    else
      echo "check_log_shader_cache=OK" >> "${summary_file}"
    fi
  fi

  if [[ "${fail_flag}" -ne 0 ]]; then
    warn "autotune matrix validation found mismatches"
    if [[ "${WLT_FAIL_ON_MISMATCH}" == "1" ]]; then
      fail "validation failed; see ${summary_file}"
    fi
  fi

  log "validation completed: ${summary_file}"
  sed 's/^/[forensic-autotune]   /' "${summary_file}"
}

main "$@"
