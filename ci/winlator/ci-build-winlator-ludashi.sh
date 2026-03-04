#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
OUT_DIR="${WINLATOR_OUTPUT_DIR:-${ROOT_DIR}/out/winlator}"
SRC_DIR="${WINLATOR_SRC_DIR:-${ROOT_DIR}}"
LOG_DIR="${OUT_DIR}/logs"
DOC_REPORT="${WINLATOR_ANALYSIS_REPORT:-${ROOT_DIR}/docs/WINLATOR_LUDASHI_REFLECTIVE_ANALYSIS.md}"

: "${WINLATOR_GRADLE_TASK:=assembleDebug}"
: "${WINLATOR_APK_BASENAME:=by.aero.so.benchmark-debug}"
: "${AEO_ADRENOTOOLS_REPO:=https://github.com/Pipetto-crypto/libadrenotools.git}"
: "${AEO_ADRENOTOOLS_REF:=main}"

log() { printf '[winlator-ci] %s\n' "$*"; }
fail() { printf '[winlator-ci][error] %s\n' "$*" >&2; exit 1; }
require_cmd() { command -v "$1" >/dev/null 2>&1 || fail "Required command not found: $1"; }

SOURCE_COMMIT="unknown"
SOURCE_SHORT_SHA="unknown"
SOURCE_BRANCH="unknown"

configure_app_version_env() {
  local code name

  if [[ -z "${AEROSO_APP_VERSION_CODE:-}" ]]; then
    if [[ "${GITHUB_RUN_NUMBER:-}" =~ ^[0-9]+$ ]]; then
      code=$((200000 + GITHUB_RUN_NUMBER))
    else
      code="$(date -u +%y%m%d%H)"
    fi
    export AEROSO_APP_VERSION_CODE="${code}"
  fi

  if [[ -z "${AEROSO_APP_VERSION_NAME:-}" ]]; then
    if [[ "${GITHUB_RUN_NUMBER:-}" =~ ^[0-9]+$ ]]; then
      name="0.9c+.${GITHUB_RUN_NUMBER}"
    else
      name="0.9c+.$(date -u +%Y%m%d%H%M)"
    fi
    export AEROSO_APP_VERSION_NAME="${name}"
  fi

  log "App version env: code=${AEROSO_APP_VERSION_CODE} name=${AEROSO_APP_VERSION_NAME}"
}

prepare_layout() {
  mkdir -p "${OUT_DIR}" "${LOG_DIR}"
}

require_native_tree() {
  [[ -f "${SRC_DIR}/settings.gradle" ]] || fail "Native source tree missing settings.gradle in ${SRC_DIR}"
  [[ -f "${SRC_DIR}/app/build.gradle" ]] || fail "Native source tree missing app/build.gradle in ${SRC_DIR}"
  [[ -f "${SRC_DIR}/gradlew" ]] || fail "Native source tree missing gradlew in ${SRC_DIR}"
}

ensure_adrenotools_tree() {
  local adrenotools_dir
  adrenotools_dir="${SRC_DIR}/app/src/main/cpp/adrenotools"

  if [[ -f "${adrenotools_dir}/CMakeLists.txt" ]]; then
    log "adrenotools source present: ${adrenotools_dir}"
    return 0
  fi

  log "adrenotools source missing, fetching ${AEO_ADRENOTOOLS_REPO}@${AEO_ADRENOTOOLS_REF}"
  rm -rf "${adrenotools_dir}"
  mkdir -p "$(dirname "${adrenotools_dir}")"
  git clone --depth 1 --branch "${AEO_ADRENOTOOLS_REF}" "${AEO_ADRENOTOOLS_REPO}" "${adrenotools_dir}" >/dev/null 2>&1 \
    || fail "Unable to fetch adrenotools source from ${AEO_ADRENOTOOLS_REPO}@${AEO_ADRENOTOOLS_REF}"
  [[ -f "${adrenotools_dir}/CMakeLists.txt" ]] || fail "Fetched adrenotools tree is invalid (missing CMakeLists.txt)"
}

capture_source_metadata() {
  if git -C "${SRC_DIR}" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    SOURCE_COMMIT="$(git -C "${SRC_DIR}" rev-parse HEAD)"
    SOURCE_SHORT_SHA="$(git -C "${SRC_DIR}" rev-parse --short HEAD)"
    SOURCE_BRANCH="$(git -C "${SRC_DIR}" rev-parse --abbrev-ref HEAD 2>/dev/null || true)"
    [[ -n "${SOURCE_BRANCH}" ]] || SOURCE_BRANCH="detached"
  fi

  cat > "${LOG_DIR}/native-source-metadata.txt" <<EOF
source_dir=${SRC_DIR}
source_branch=${SOURCE_BRANCH}
source_commit=${SOURCE_COMMIT}
source_short_sha=${SOURCE_SHORT_SHA}
EOF

  cat > "${DOC_REPORT}" <<EOF
# Aesolator Native Source Snapshot

- Source mode: native tree (no CI patch overlay)
- Source directory: \`${SRC_DIR}\`
- Branch: \`${SOURCE_BRANCH}\`
- Commit: \`${SOURCE_COMMIT}\`
- Captured at (UTC): $(date -u +"%Y-%m-%dT%H:%M:%SZ")
EOF
}

build_apk() {
  local apk_path out_apk version_name_safe

  chmod +x "${SRC_DIR}/gradlew"
  pushd "${SRC_DIR}" >/dev/null
  ./gradlew --no-daemon "${WINLATOR_GRADLE_TASK}"
  popd >/dev/null

  apk_path="$(find "${SRC_DIR}/app/build/outputs/apk" -type f -name '*.apk' | sort | head -n1)"
  [[ -n "${apk_path}" ]] || fail "Unable to locate built APK under app/build/outputs/apk"

  version_name_safe="$(printf '%s' "${AEROSO_APP_VERSION_NAME}" | tr -cs '[:alnum:]._-' '_')"
  out_apk="${OUT_DIR}/${WINLATOR_APK_BASENAME}-${version_name_safe}-${SOURCE_SHORT_SHA}.apk"
  cp -f "${apk_path}" "${out_apk}"

  (
    cd "${OUT_DIR}"
    sha256sum "$(basename -- "${out_apk}")" > SHA256SUMS
  )

  log "Built APK: ${out_apk}"
}

main() {
  require_cmd bash
  require_cmd git
  require_cmd sha256sum

  require_native_tree
  prepare_layout
  ensure_adrenotools_tree
  capture_source_metadata
  configure_app_version_env
  build_apk
}

main "$@"
