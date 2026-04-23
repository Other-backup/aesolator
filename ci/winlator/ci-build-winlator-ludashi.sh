#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
OUT_DIR="${WINLATOR_OUTPUT_DIR:-${ROOT_DIR}/out/winlator}"
SRC_DIR="${WINLATOR_SRC_DIR:-${ROOT_DIR}}"
LOG_DIR="${OUT_DIR}/logs"
DOC_REPORT="${WINLATOR_ANALYSIS_REPORT:-${ROOT_DIR}/docs/WINLATOR_LUDASHI_REFLECTIVE_ANALYSIS.md}"

: "${WINLATOR_GRADLE_TASK:=assembleDebug}"
: "${WINLATOR_APK_BASENAME:=by.aero.so.benchmark-debug}"
: "${AEO_BUILD_FRESH_GRAPHICS:=1}"
: "${AEO_GRAPHICS_BUILD_JOBS:=8}"
: "${AEO_HOST_LLVM_VERSION:=22.1.1}"
: "${AEO_MESA_REF:=freshest-nonmain}"
: "${AEO_ADRENOTOOLS_REPO:=https://github.com/Pipetto-crypto/libadrenotools.git}"
: "${AEO_ADRENOTOOLS_REF:=master}"
: "${AEO_ROOTFS_BIONIC_URL:=https://downloads.gamenative.app/imagefs_bionic.txz}"
: "${AEO_ROOTFS_GAMENATIVE_URL:=https://downloads.gamenative.app/imagefs_gamenative.txz}"
: "${AEO_ROOTFS_PATCHES_URL:=https://downloads.gamenative.app/imagefs_patches_gamenative.tzst}"

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
      name="0.9q+.${GITHUB_RUN_NUMBER}"
    else
      name="0.9q+.$(date -u +%Y%m%d%H%M)"
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
  git clone --depth 1 --branch "${AEO_ADRENOTOOLS_REF}" --recurse-submodules --shallow-submodules \
    "${AEO_ADRENOTOOLS_REPO}" "${adrenotools_dir}" \
    || fail "Unable to fetch adrenotools source from ${AEO_ADRENOTOOLS_REPO}@${AEO_ADRENOTOOLS_REF}"
  [[ -f "${adrenotools_dir}/CMakeLists.txt" ]] || fail "Fetched adrenotools tree is invalid (missing CMakeLists.txt)"
}

ensure_rootfs_assets() {
  local asset_dir
  asset_dir="${SRC_DIR}/app/src/main/assets"
  mkdir -p "${asset_dir}"

  ensure_rootfs_asset "${AEO_ROOTFS_BIONIC_URL}" "${asset_dir}/imagefs_bionic.txz"
  ensure_rootfs_asset "${AEO_ROOTFS_GAMENATIVE_URL}" "${asset_dir}/imagefs_gamenative.txz"
  ensure_rootfs_asset "${AEO_ROOTFS_PATCHES_URL}" "${asset_dir}/imagefs_patches_gamenative.tzst"
}

ensure_rootfs_asset() {
  local url destination tmp
  url="$1"
  destination="$2"
  tmp="${destination}.part"

  if [[ -s "${destination}" ]]; then
    log "rootfs asset present: ${destination}"
    return 0
  fi

  log "fetching rootfs asset ${url}"
  rm -f "${tmp}"
  curl --fail --location --retry 5 --retry-delay 3 --output "${tmp}" "${url}" \
    || fail "Unable to download rootfs asset from ${url}"
  [[ -s "${tmp}" ]] || fail "Downloaded rootfs asset is empty: ${url}"
  mv -f "${tmp}" "${destination}"
}

resolve_workspace_root() {
  cd -- "${SRC_DIR}/.." && pwd
}

resolve_ndk_prebuilt_root() {
  local ndk_dir prebuilt_root host_arch preferred
  ndk_dir="${ANDROID_SDK_ROOT}/ndk/29.0.14206865"
  prebuilt_root="${ndk_dir}/toolchains/llvm/prebuilt"
  [[ -d "${prebuilt_root}" ]] || fail "NDK prebuilt root missing: ${prebuilt_root}"

  host_arch="$(uname -m)"
  case "${host_arch}" in
    aarch64|arm64)
      preferred="linux-aarch64"
      ;;
    x86_64|amd64)
      preferred="linux-x86_64"
      ;;
    *)
      preferred=""
      ;;
  esac

  if [[ -n "${preferred}" && -d "${prebuilt_root}/${preferred}/sysroot" ]]; then
    printf '%s\n' "${prebuilt_root}/${preferred}"
    return 0
  fi

  find "${prebuilt_root}" -mindepth 1 -maxdepth 1 -type d | sort | while IFS= read -r candidate; do
    [[ -d "${candidate}/sysroot" ]] || continue
    printf '%s\n' "${candidate}"
    break
  done
}

resolve_host_clang_resource_dir() {
  local llvm_root="$1"
  local resource_dir

  resource_dir="$("${llvm_root}/bin/clang" --print-resource-dir 2>/dev/null || true)"
  [[ -n "${resource_dir}" && -d "${resource_dir}/include" ]] \
    || fail "Unable to resolve clang resource dir from ${llvm_root}"
  printf '%s\n' "${resource_dir}"
}

ensure_host_llvm_tree() {
  local workspace_root llvm_root
  workspace_root="$(resolve_workspace_root)"
  llvm_root="${workspace_root}/.toolchains/llvm-${AEO_HOST_LLVM_VERSION}-termux"
  if [[ -x "${llvm_root}/bin/clang" && -f "${llvm_root}/lib/libLLVM.so" ]]; then
    log "host llvm present: ${llvm_root}"
    printf '%s\n' "${llvm_root}"
    return 0
  fi

  log "restoring host llvm ${AEO_HOST_LLVM_VERSION} into ${workspace_root}"
  ROOT="${workspace_root}" GITHUB_TOKEN="${GITHUB_TOKEN:-}" AEO_GITHUB_TOKEN="${GITHUB_TOKEN:-}" \
    bash "${SRC_DIR}/tools/fetch-host-llvm-release.sh"
  [[ -x "${llvm_root}/bin/clang" ]] || fail "host llvm restore failed: ${llvm_root}"
  printf '%s\n' "${llvm_root}"
}

prepare_android_host_prefix() {
  local workspace_root prefix_root ndk_prebuilt_root ndk_sysroot libcxx
  workspace_root="$(resolve_workspace_root)"
  prefix_root="${workspace_root}/.android-host-prefix"
  ndk_prebuilt_root="$(resolve_ndk_prebuilt_root)"
  ndk_sysroot="${ndk_prebuilt_root}/sysroot"
  libcxx="${ndk_sysroot}/usr/lib/aarch64-linux-android/libc++_shared.so"

  [[ -f "${libcxx}" ]] || fail "NDK libc++_shared missing: ${libcxx}"
  mkdir -p "${prefix_root}/bin" "${prefix_root}/lib"
  ln -sf "$(command -v pkg-config)" "${prefix_root}/bin/pkg-config"
  cp -f "${libcxx}" "${prefix_root}/lib/libc++_shared.so"
  printf '%s\n' "${prefix_root}"
}

assert_fresh_graphics_asset() {
  local pattern="$1"
  find "${SRC_DIR}/app/src/main/assets/graphics_driver" -maxdepth 1 -type f -name "${pattern}" | grep -q .
}

build_fresh_graphics_surface() {
  local workspace_root llvm_root prefix_root ndk_prebuilt_root ndk_root ndk_sysroot clang_resource_dir android_sysvshm_src_dir common_env virgl_run_root

  if [[ "${AEO_BUILD_FRESH_GRAPHICS}" != "1" ]]; then
    log "fresh graphics build disabled"
    return 0
  fi

  workspace_root="$(resolve_workspace_root)"
  llvm_root="$(ensure_host_llvm_tree)"
  prefix_root="$(prepare_android_host_prefix)"
  ndk_prebuilt_root="$(resolve_ndk_prebuilt_root)"
  ndk_root="${ANDROID_SDK_ROOT}/ndk/29.0.14206865"
  ndk_sysroot="${ndk_prebuilt_root}/sysroot"
  clang_resource_dir="$(resolve_host_clang_resource_dir "${llvm_root}")"
  android_sysvshm_src_dir="${SRC_DIR}/android_sysvshm"
  [[ -f "${android_sysvshm_src_dir}/android_sysvshm.c" ]] \
    || fail "aesolator android_sysvshm source missing: ${android_sysvshm_src_dir}"

  common_env=(
    "ROOT=${workspace_root}"
    "AESOLATOR_ROOT=${SRC_DIR}"
    "LLVM_ROOT=${llvm_root}"
    "ANDROID_API=34"
    "NDK_ROOT=${ndk_root}"
    "NDK_PREBUILT_ROOT=${ndk_prebuilt_root}"
    "NDK_SYSROOT=${ndk_sysroot}"
    "ANDROID_SYSROOT=${ndk_sysroot}"
    "CLANG_RESOURCE_DIR=${clang_resource_dir}"
    "HOST_CLANG_RESOURCE_DIR=${clang_resource_dir}"
    "HOST_PKG_CONFIG=${prefix_root}/bin/pkg-config"
    "TERMUX_PREFIX=${prefix_root}"
    "HOST_LIBCXX_SHARED=${prefix_root}/lib/libc++_shared.so"
    "ANDROID_SYSVSHM_SRC_DIR=${android_sysvshm_src_dir}"
    "INSTALL_APP_ASSET=1"
    "JOBS=${AEO_GRAPHICS_BUILD_JOBS}"
  )

  log "build fresh mesa x11 bridges"
  env "${common_env[@]}" REFRESH_UPSTREAM_REFS=1 MESA_REF="${AEO_MESA_REF}" ACTION=all \
    bash "${SRC_DIR}/tools/build-mesa-staging-graphics.sh" mesa-x11-bionic

  log "build fresh aemali panvk"
  env "${common_env[@]}" REFRESH_UPSTREAM_REFS=1 MESA_REF="${AEO_MESA_REF}" ACTION=all \
    bash "${SRC_DIR}/tools/build-mesa-staging-graphics.sh" panvk-android

  log "build fresh aemali gallium"
  env "${common_env[@]}" REFRESH_UPSTREAM_REFS=1 MESA_REF="${AEO_MESA_REF}" ACTION=all \
    bash "${SRC_DIR}/tools/build-mesa-staging-graphics.sh" mali-gallium-android

  log "build fresh virglrenderer JNI surface"
  env "${common_env[@]}" VIRGL_OVERLAY_FETCH_REF="refs/merge-requests/1615/head" \
    VIRGL_OVERLAY_REF="refs/remotes/origin/merge-requests/1615/head" \
    bash "${SRC_DIR}/tools/build-virgl-wrapper-stack.sh"

  log "build fresh vortek package"
  env "${common_env[@]}" VORTEK_REF="main" INSTALL_APP_ASSET=1 \
    bash "${SRC_DIR}/tools/build-vortek-wrapper-client.sh"

  test -f "${SRC_DIR}/out/graphics-source-builds/latest-virgl-wrapper-run.txt" \
    || fail "missing latest VirGL wrapper pointer after fresh graphics build"
  virgl_run_root="$(tr -d '\n' < "${SRC_DIR}/out/graphics-source-builds/latest-virgl-wrapper-run.txt")"
  [[ -f "${virgl_run_root}/jni/arm64-v8a/libvirglrenderer.so" ]] \
    || fail "VirGL wrapper run is missing libvirglrenderer.so: ${virgl_run_root}"
  [[ -f "${virgl_run_root}/jni/arm64-v8a/libepoxy.so" ]] \
    || fail "VirGL wrapper run is missing libepoxy.so: ${virgl_run_root}"
  find "${SRC_DIR}/app/src/main/assets/graphics_driver" -maxdepth 1 -type f \
    \( -name 'aemali-panvk-*.tzst' -o -name 'aemali-gallium-*.tzst' -o -name 'virgl-*.tzst' -o -name 'zink-*.tzst' -o -name 'vortek-*.tzst' \) \
    | sort > "${LOG_DIR}/fresh-graphics-assets.txt"
  test -s "${LOG_DIR}/fresh-graphics-assets.txt" || fail "fresh graphics asset wave produced no bundled assets"
  assert_fresh_graphics_asset 'aemali-panvk-*.tzst' || fail "fresh aemali panvk asset missing"
  assert_fresh_graphics_asset 'aemali-gallium-*.tzst' || fail "fresh aemali gallium asset missing"
  assert_fresh_graphics_asset 'virgl-*.tzst' || fail "fresh virgl asset missing"
  assert_fresh_graphics_asset 'zink-*.tzst' || fail "fresh zink asset missing"
  assert_fresh_graphics_asset 'vortek-*.tzst' || fail "fresh vortek asset missing"
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

  build_fresh_graphics_surface
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
  require_cmd curl
  require_cmd git
  require_cmd sha256sum

  require_native_tree
  prepare_layout
  ensure_adrenotools_tree
  ensure_rootfs_assets
  capture_source_metadata
  configure_app_version_env
  build_apk
}

main "$@"
