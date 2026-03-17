#!/data/data/com.termux/files/usr/bin/sh
set -eu

ROOT="/data/data/com.termux/files/home"
REPO_ROOT="${AEO_REPO_ROOT:-$ROOT/aesolator}"
SDK_ROOT="${ANDROID_SDK_ROOT:-$ROOT/android-sdk}"
NDK_VERSION="${AEO_ANDROID_NDK_VERSION:-29.0.14206865}"
ANDROID_PLATFORM="${AEO_ANDROID_PLATFORM:-android-34}"
BUILD_TOOLS_VERSION="${AEO_ANDROID_BUILD_TOOLS:-35.0.0}"
HOST_LLVM_VERSION="${AEO_HOST_LLVM_VERSION:-22.1.1}"
LOCAL_PROPERTIES="$REPO_ROOT/local.properties"

log() {
  printf '[bootstrap-termux-host] %s\n' "$*"
}

need_cmd() {
  command -v "$1" >/dev/null 2>&1
}

install_termux_packages() {
  if ! need_cmd pkg; then
    log "pkg not found; skipping Termux package install"
    return 0
  fi

  log "Installing baseline Termux packages"
  pkg update -y
  pkg install -y \
    git curl wget tar xz-utils zstd unzip zip \
    openjdk-17 android-tools cmake ninja clang make python
}

prepare_sdk_dirs() {
  log "Preparing Android SDK directories"
  mkdir -p "$SDK_ROOT"
}

install_sdk_packages() {
  SDKMANAGER="$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
  if [ ! -x "$SDKMANAGER" ]; then
    log "sdkmanager not present at $SDKMANAGER; leaving SDK package install for later"
    return 0
  fi

  export ANDROID_SDK_ROOT="$SDK_ROOT"
  export ANDROID_HOME="$SDK_ROOT"

  log "Installing Android SDK components"
  yes | "$SDKMANAGER" --licenses >/dev/null 2>&1 || true
  "$SDKMANAGER" \
    "platform-tools" \
    "platforms;$ANDROID_PLATFORM" \
    "build-tools;$BUILD_TOOLS_VERSION" \
    "ndk;$NDK_VERSION"
}

write_local_properties() {
  log "Writing local.properties"
  mkdir -p "$REPO_ROOT"
  printf '%s\n' \
    "sdk.dir=$SDK_ROOT" \
    "ndk.dir=$SDK_ROOT/ndk/$NDK_VERSION" \
    "cmake.dir=/data/data/com.termux/files/usr" >"$LOCAL_PROPERTIES"
}

fetch_host_llvm() {
  FETCH_SCRIPT="$REPO_ROOT/tools/fetch-host-llvm-release.sh"
  if [ ! -x "$FETCH_SCRIPT" ]; then
    log "Host LLVM fetch script not present; skipping"
    return 0
  fi

  if [ -n "${GITHUB_TOKEN:-}" ] || [ -n "${AEO_GITHUB_TOKEN:-}" ]; then
    log "Fetching host LLVM $HOST_LLVM_VERSION from release lane"
    sh "$FETCH_SCRIPT"
  else
    log "No GitHub token in shell; skipping host LLVM release fetch"
  fi
}

print_next_steps() {
  printf '%s\n' \
    "[bootstrap-termux-host] Done." \
    "" \
    "Next:" \
    "  1. cd $REPO_ROOT" \
    "  2. . tools/env-android-local.sh" \
    "  3. git branch --show-current" \
    "  4. git status --short" \
    "" \
    "Expected paths:" \
    "  SDK: $SDK_ROOT" \
    "  local.properties: $LOCAL_PROPERTIES" \
    "  host LLVM: $ROOT/.toolchains/llvm-$HOST_LLVM_VERSION-termux"
}

main() {
  install_termux_packages
  prepare_sdk_dirs
  install_sdk_packages
  write_local_properties
  fetch_host_llvm
  print_next_steps
}

main "$@"
