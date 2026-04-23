#!/data/data/com.termux/files/usr/bin/sh
set -eu

ROOT="/data/data/com.termux/files/home"
SDK_ROOT="${ANDROID_SDK_ROOT:-$ROOT/android-sdk}"
NDK_VERSION="${AEO_ANDROID_NDK_VERSION:-29.0.14206865}"
HOST_LLVM_VERSION="${AEO_HOST_LLVM_VERSION:-22.1.1}"
HOST_LLVM_ROOT="${AEO_HOST_LLVM_ROOT:-$ROOT/.toolchains/llvm-$HOST_LLVM_VERSION-termux}"
NDK_PREBUILT_ROOT="$SDK_ROOT/ndk/$NDK_VERSION/toolchains/llvm/prebuilt"
TERMUX_SH="/data/data/com.termux/files/usr/bin/sh"

log() {
  printf '[install-ndk-termux-shims] %s\n' "$*"
}

resolve_tool() {
  tool_name="$1"
  if [ -x "$HOST_LLVM_ROOT/bin/$tool_name" ]; then
    printf '%s\n' "$HOST_LLVM_ROOT/bin/$tool_name"
    return 0
  fi

  if command -v "$tool_name" >/dev/null 2>&1; then
    command -v "$tool_name"
    return 0
  fi

  return 1
}

install_wrapper() {
  destination="$1"
  target_bin="$2"
  backup_path="${destination}.termux-orig"

  mkdir -p "$(dirname -- "$destination")"

  if [ -L "$destination" ]; then
    if [ ! -e "$backup_path" ] && [ ! -L "$backup_path" ]; then
      mv "$destination" "$backup_path"
    else
      rm -f "$destination"
    fi
  elif [ -f "$destination" ]; then
    first_line="$(sed -n '1p' "$destination" 2>/dev/null || true)"
    if [ "$first_line" != "#!$TERMUX_SH" ] && [ ! -e "$backup_path" ] && [ ! -L "$backup_path" ]; then
      mv "$destination" "$backup_path"
    fi
  fi

  cat >"$destination" <<EOF
#!$TERMUX_SH
exec $target_bin "\$@"
EOF
  chmod 0755 "$destination"
}

main() {
  strip_bin="$(resolve_tool llvm-strip)" || {
    log "llvm-strip not found in host LLVM or PATH"
    exit 1
  }
  objcopy_bin="$(resolve_tool llvm-objcopy)" || {
    log "llvm-objcopy not found in host LLVM or PATH"
    exit 1
  }

  install_wrapper "$NDK_PREBUILT_ROOT/bin/llvm-strip" "$strip_bin"
  install_wrapper "$NDK_PREBUILT_ROOT/bin/llvm-objcopy" "$objcopy_bin"
  install_wrapper "$NDK_PREBUILT_ROOT/linux-x86_64/bin/llvm-strip" "$strip_bin"
  install_wrapper "$NDK_PREBUILT_ROOT/linux-x86_64/bin/llvm-objcopy" "$objcopy_bin"

  log "Installed Termux-host NDK wrappers:"
  log "  llvm-strip -> $strip_bin"
  log "  llvm-objcopy -> $objcopy_bin"
}

main "$@"
