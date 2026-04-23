#!/data/data/com.termux/files/usr/bin/sh
set -eu

ROOT="/data/data/com.termux/files/home"
REPO_ROOT="${AEO_REPO_ROOT:-$ROOT/aesolator}"
SDK_ROOT="${ANDROID_SDK_ROOT:-$ROOT/android-sdk}"
NDK_VERSION="${AEO_ANDROID_NDK_VERSION:-29.0.14206865}"
ANDROID_PLATFORM="${AEO_ANDROID_PLATFORM:-android-34}"
BUILD_TOOLS_VERSION="${AEO_ANDROID_BUILD_TOOLS:-35.0.0}"
CMDLINE_TOOLS_REVISION="${AEO_ANDROID_CMDLINE_TOOLS_REVISION:-14742923}"
CMDLINE_TOOLS_ZIP="commandlinetools-linux-${CMDLINE_TOOLS_REVISION}_latest.zip"
CMDLINE_TOOLS_URL="${AEO_ANDROID_CMDLINE_TOOLS_URL:-https://dl.google.com/android/repository/${CMDLINE_TOOLS_ZIP}}"
HOST_LLVM_VERSION="${AEO_HOST_LLVM_VERSION:-22.1.1}"
LOCAL_PROPERTIES="$REPO_ROOT/local.properties"
BOOTSTRAP_TMP_DIR="${ROOT}/.tmp_android_sdk"

log() {
  printf '[bootstrap-termux-host] %s\n' "$*"
}

need_cmd() {
  command -v "$1" >/dev/null 2>&1
}

download_file() {
  url="$1"
  output="$2"

  if need_cmd wget; then
    wget -O "$output" "$url"
    return 0
  fi

  if need_cmd curl; then
    curl -fL "$url" -o "$output"
    return 0
  fi

  log "Neither wget nor curl is available for downloading $url"
  return 1
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
  mkdir -p "$SDK_ROOT" "$BOOTSTRAP_TMP_DIR"
}

extract_cmdline_tools_zip_safely() {
  ZIP_PATH="$1"
  DEST_ROOT="$2"
  EXTRACT_TMP="$(mktemp -d "$BOOTSTRAP_TMP_DIR/cmdline-tools-extract.XXXXXX")"

  python3 - "$ZIP_PATH" "$EXTRACT_TMP" <<'PY'
import pathlib
import sys
import zipfile

zip_path = pathlib.Path(sys.argv[1])
extract_root = pathlib.Path(sys.argv[2])

with zipfile.ZipFile(zip_path) as zf:
    names = zf.namelist()
    if not names:
        raise SystemExit("empty zip archive")
    for name in names:
        pure = pathlib.PurePosixPath(name)
        if pure.is_absolute() or ".." in pure.parts:
            raise SystemExit(f"unsafe zip entry: {name}")
        if pure.parts and pure.parts[0] != "cmdline-tools":
            raise SystemExit(f"unexpected zip root entry: {name}")

    for info in zf.infolist():
        pure = pathlib.PurePosixPath(info.filename)
        if not pure.parts:
            continue
        target = extract_root.joinpath(*pure.parts)
        target.parent.mkdir(parents=True, exist_ok=True)
        if info.is_dir():
            target.mkdir(parents=True, exist_ok=True)
            continue
        with zf.open(info) as src, target.open("wb") as dst:
            dst.write(src.read())
PY

  if [ ! -x "$EXTRACT_TMP/cmdline-tools/bin/sdkmanager" ]; then
    rm -rf "$EXTRACT_TMP"
    log "Extracted command-line tools are incomplete"
    return 1
  fi

  rm -rf "$DEST_ROOT/latest" "$DEST_ROOT/cmdline-tools"
  mv "$EXTRACT_TMP/cmdline-tools" "$DEST_ROOT/latest"
  rm -rf "$EXTRACT_TMP"
}

install_sdk_cmdline_tools() {
  SDKMANAGER="$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
  ZIP_PATH="$BOOTSTRAP_TMP_DIR/$CMDLINE_TOOLS_ZIP"

  if [ -x "$SDKMANAGER" ]; then
    return 0
  fi

  log "Installing Android SDK command-line tools"
  download_file "$CMDLINE_TOOLS_URL" "$ZIP_PATH"
  mkdir -p "$SDK_ROOT/cmdline-tools"
  extract_cmdline_tools_zip_safely "$ZIP_PATH" "$SDK_ROOT/cmdline-tools"
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
    "cmake.dir=/data/data/com.termux/files/usr" >"$LOCAL_PROPERTIES"
}

fetch_host_llvm() {
  FETCH_SCRIPT="$REPO_ROOT/tools/fetch-host-llvm-release.sh"
  if [ ! -f "$FETCH_SCRIPT" ]; then
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

install_ndk_termux_shims() {
  SHIM_SCRIPT="$REPO_ROOT/tools/install-ndk-termux-shims.sh"
  if [ ! -f "$SHIM_SCRIPT" ]; then
    log "NDK Termux shim script not present; skipping"
    return 0
  fi

  log "Installing Termux-host NDK shim wrappers"
  ANDROID_SDK_ROOT="$SDK_ROOT" \
  AEO_ANDROID_NDK_VERSION="$NDK_VERSION" \
  AEO_HOST_LLVM_VERSION="$HOST_LLVM_VERSION" \
  sh "$SHIM_SCRIPT"
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
  install_sdk_cmdline_tools
  install_sdk_packages
  write_local_properties
  fetch_host_llvm
  install_ndk_termux_shims
  print_next_steps
}

main "$@"
