#!/usr/bin/env bash
set -euo pipefail

ROOT="${ROOT:-/data/data/com.termux/files/home}"
AESOLATOR_ROOT="${AESOLATOR_ROOT:-$ROOT/aesolator}"
LLVM_ROOT="${LLVM_ROOT:-$ROOT/.toolchains/llvm-22.1.1-termux}"
ANDROID_API="${ANDROID_API:-34}"
NDK_ROOT="${NDK_ROOT:-$ROOT/android-sdk/ndk/29.0.14206865}"
NDK_PREBUILT_ROOT="${NDK_PREBUILT_ROOT:-}"
NDK_HOST_TAG="${NDK_HOST_TAG:-}"
NDK_SYSROOT="${NDK_SYSROOT:-}"
CLANG_RESOURCE_DIR="${CLANG_RESOURCE_DIR:-}"
HOST_PKG_CONFIG="${HOST_PKG_CONFIG:-$(command -v pkg-config || true)}"
OUT_ROOT="${OUT_ROOT:-$AESOLATOR_ROOT/out/graphics-source-builds}"
JOBS="${JOBS:-8}"

VIRGL_URL="${VIRGL_URL:-https://gitlab.freedesktop.org/virgl/virglrenderer}"
VIRGL_REPO="${VIRGL_REPO:-$ROOT/.cache/virglrenderer-1.3.0-audit}"
VIRGL_BASE_REF="${VIRGL_BASE_REF:-virglrenderer-1.3.0}"
VIRGL_OVERLAY_FETCH_REF="${VIRGL_OVERLAY_FETCH_REF:-refs/merge-requests/1615/head}"
VIRGL_OVERLAY_REF="${VIRGL_OVERLAY_REF:-refs/remotes/origin/merge-requests/1615/head}"
APPLY_VIRGL_OVERLAY="${APPLY_VIRGL_OVERLAY:-1}"
VIRGL_PATCH="${VIRGL_PATCH:-$AESOLATOR_ROOT/patches/virglrenderer/android-external-egl-no-gbm.patch}"

LIBEPOXY_URL="${LIBEPOXY_URL:-https://github.com/anholt/libepoxy.git}"
LIBEPOXY_REPO="${LIBEPOXY_REPO:-$ROOT/.cache/libepoxy-1.5.10-audit}"
LIBEPOXY_REF="${LIBEPOXY_REF:-c84bc9459357a40e46e2fec0408d04fbdde2c973}"

TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
RUN_ROOT="${RUN_ROOT:-$OUT_ROOT/$TIMESTAMP-virgl-wrapper-android}"
LOG_DIR="$RUN_ROOT/logs"
SRC_ROOT="$RUN_ROOT/src"
BUILD_ROOT="$RUN_ROOT/build"
SUPPORT_ROOT="$RUN_ROOT/support"
SUPPORT_DESTDIR="$RUN_ROOT/support-stage"
STAGE_ROOT="$RUN_ROOT/stage"
JNI_ROOT="$RUN_ROOT/jni/arm64-v8a"
PACKAGE_ROOT="$RUN_ROOT/package"
TOOLCHAIN_ROOT="$RUN_ROOT/toolchain"
LOG_FILE="$LOG_DIR/build.log"
ANDROID_COMPAT_INCLUDE_DIR="$TOOLCHAIN_ROOT/android-compat/include"

mkdir -p "$LOG_DIR" "$SRC_ROOT" "$BUILD_ROOT" "$SUPPORT_ROOT" "$SUPPORT_DESTDIR" "$STAGE_ROOT" "$JNI_ROOT" "$PACKAGE_ROOT" "$TOOLCHAIN_ROOT"
exec > >(tee -a "$LOG_FILE") 2>&1

require_tool() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required tool: $1" >&2
    exit 69
  fi
}

require_path() {
  if [ ! -e "$1" ]; then
    echo "Missing required path: $1" >&2
    exit 69
  fi
}

resolve_ndk_prebuilt_root() {
  local prebuilt_root host_uname preferred

  if [ -n "${NDK_PREBUILT_ROOT:-}" ] && [ -d "$NDK_PREBUILT_ROOT/sysroot" ]; then
    printf '%s\n' "$NDK_PREBUILT_ROOT"
    return 0
  fi

  prebuilt_root="$NDK_ROOT/toolchains/llvm/prebuilt"
  [ -d "$prebuilt_root" ] || return 1

  if [ -n "${NDK_HOST_TAG:-}" ] && [ -d "$prebuilt_root/$NDK_HOST_TAG/sysroot" ]; then
    printf '%s\n' "$prebuilt_root/$NDK_HOST_TAG"
    return 0
  fi

  host_uname="$(uname -m 2>/dev/null || true)"
  case "$host_uname" in
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

  if [ -n "$preferred" ] && [ -d "$prebuilt_root/$preferred/sysroot" ]; then
    printf '%s\n' "$prebuilt_root/$preferred"
    return 0
  fi

  find "$prebuilt_root" -mindepth 1 -maxdepth 1 -type d | while IFS= read -r candidate; do
    [ -d "$candidate/sysroot" ] || continue
    printf '%s\n' "$candidate"
    break
  done
}

resolve_clang_resource_dir() {
  local resolved=""

  if [ -n "${CLANG_RESOURCE_DIR:-}" ] && [ -d "$CLANG_RESOURCE_DIR/include" ]; then
    printf '%s\n' "$CLANG_RESOURCE_DIR"
    return 0
  fi

  if [ -x "$LLVM_ROOT/bin/clang" ]; then
    resolved="$("$LLVM_ROOT/bin/clang" --print-resource-dir 2>/dev/null || true)"
    if [ -n "$resolved" ] && [ -d "$resolved/include" ]; then
      printf '%s\n' "$resolved"
      return 0
    fi
  fi

  if [ -n "${NDK_PREBUILT_ROOT:-}" ]; then
    find "$NDK_PREBUILT_ROOT/lib/clang" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -n 1
    return 0
  fi

  return 1
}

for tool in git ninja patchelf pkg-config tar zstd python3; do
  require_tool "$tool"
done

require_path "$HOST_PKG_CONFIG"

require_path "$LLVM_ROOT/bin/clang"
require_path "$LLVM_ROOT/bin/clang++"
require_path "$LLVM_ROOT/bin/llvm-ar"
require_path "$LLVM_ROOT/bin/llvm-ranlib"
require_path "$LLVM_ROOT/bin/llvm-strip"
require_path "$LLVM_ROOT/bin/ld.lld"
NDK_PREBUILT_ROOT="$(resolve_ndk_prebuilt_root)"
[ -n "$NDK_HOST_TAG" ] || NDK_HOST_TAG="$(basename "$NDK_PREBUILT_ROOT")"
[ -n "$NDK_SYSROOT" ] || NDK_SYSROOT="$NDK_PREBUILT_ROOT/sysroot"
CLANG_RESOURCE_DIR="$(resolve_clang_resource_dir)"
require_path "$NDK_SYSROOT/usr/include/EGL/egl.h"
require_path "$CLANG_RESOURCE_DIR/include/stddef.h"
require_path "$VIRGL_PATCH"

echo "run_root=$RUN_ROOT"
echo "llvm_root=$LLVM_ROOT"
echo "ndk_root=$NDK_ROOT"
echo "ndk_sysroot=$NDK_SYSROOT"
echo "clang_resource_dir=$CLANG_RESOURCE_DIR"
echo "virgl_repo=$VIRGL_REPO"
echo "virgl_base_ref=$VIRGL_BASE_REF"
echo "virgl_overlay_ref=$VIRGL_OVERLAY_REF"
echo "apply_virgl_overlay=$APPLY_VIRGL_OVERLAY"
echo "libepoxy_repo=$LIBEPOXY_REPO"
echo "libepoxy_ref=$LIBEPOXY_REF"

mkdir -p "$ANDROID_COMPAT_INCLUDE_DIR/cutils" "$ANDROID_COMPAT_INCLUDE_DIR/log"
cat > "$ANDROID_COMPAT_INCLUDE_DIR/log/log.h" <<'EOF'
#ifndef AERO_ANDROID_COMPAT_LOG_LOG_H
#define AERO_ANDROID_COMPAT_LOG_LOG_H

#include <android/log.h>

#ifdef __cplusplus
extern "C" {
#endif

#ifndef LOG_PRI
#define LOG_PRI(priority, tag, ...) __android_log_print((priority), (tag), __VA_ARGS__)
#endif

#ifdef __cplusplus
}
#endif

#endif
EOF

cat > "$ANDROID_COMPAT_INCLUDE_DIR/cutils/properties.h" <<'EOF'
#ifndef AERO_ANDROID_COMPAT_CUTILS_PROPERTIES_H
#define AERO_ANDROID_COMPAT_CUTILS_PROPERTIES_H

#include <stddef.h>
#include <string.h>
#include <sys/system_properties.h>

#ifdef __cplusplus
extern "C" {
#endif

#ifndef PROPERTY_KEY_MAX
#define PROPERTY_KEY_MAX PROP_NAME_MAX
#endif

#ifndef PROPERTY_VALUE_MAX
#define PROPERTY_VALUE_MAX PROP_VALUE_MAX
#endif

static inline int property_get(const char *key, char *value, const char *default_value)
{
  int len = value ? __system_property_get(key, value) : 0;
  if (len > 0) return len;
  if (!value) return 0;
  if (!default_value) {
    value[0] = '\0';
    return 0;
  }
  size_t copy_len = strlen(default_value);
  if (copy_len >= (size_t)PROPERTY_VALUE_MAX) copy_len = (size_t)PROPERTY_VALUE_MAX - 1;
  memcpy(value, default_value, copy_len);
  value[copy_len] = '\0';
  return (int)copy_len;
}

#ifdef __cplusplus
}
#endif

#endif
EOF

ensure_repo() {
  local repo="$1"
  local url="$2"
  if [ ! -d "$repo/.git" ]; then
    mkdir -p "$(dirname "$repo")"
    git clone --filter=blob:none "$url" "$repo"
  fi
}

ensure_repo "$VIRGL_REPO" "$VIRGL_URL"
ensure_repo "$LIBEPOXY_REPO" "$LIBEPOXY_URL"

git -C "$VIRGL_REPO" fetch --tags origin "$VIRGL_OVERLAY_FETCH_REF:$VIRGL_OVERLAY_REF"
git -C "$LIBEPOXY_REPO" fetch --tags origin

VIRGL_BASE_COMMIT="$(git -C "$VIRGL_REPO" rev-parse "$VIRGL_BASE_REF^{commit}")"
LIBEPOXY_COMMIT="$(git -C "$LIBEPOXY_REPO" rev-parse "$LIBEPOXY_REF^{commit}")"
if [ "$APPLY_VIRGL_OVERLAY" = "1" ]; then
  VIRGL_OVERLAY_COMMIT="$(git -C "$VIRGL_REPO" rev-parse "$VIRGL_OVERLAY_REF^{commit}")"
else
  VIRGL_OVERLAY_COMMIT=""
fi

VIRGL_SRC="$SRC_ROOT/virglrenderer"
LIBEPOXY_SRC="$SRC_ROOT/libepoxy"
git -C "$VIRGL_REPO" worktree add --detach "$VIRGL_SRC" "$VIRGL_BASE_COMMIT"
git -C "$LIBEPOXY_REPO" worktree add --detach "$LIBEPOXY_SRC" "$LIBEPOXY_COMMIT"

if [ "$APPLY_VIRGL_OVERLAY" = "1" ]; then
  git -C "$VIRGL_SRC" \
    -c user.name='Codex' \
    -c user.email='codex@localhost.invalid' \
    cherry-pick --keep-redundant-commits "$VIRGL_OVERLAY_COMMIT"
fi

git -C "$VIRGL_SRC" apply "$VIRGL_PATCH"

write_cross_file() {
  local out_file="$1"
  local pkgconfig_dir="$2"
  cat > "$out_file" <<EOF
[binaries]
c = '$LLVM_ROOT/bin/clang'
cpp = '$LLVM_ROOT/bin/clang++'
ar = '$LLVM_ROOT/bin/llvm-ar'
ranlib = '$LLVM_ROOT/bin/llvm-ranlib'
strip = '$LLVM_ROOT/bin/llvm-strip'
pkg-config = '$HOST_PKG_CONFIG'

[properties]
needs_exe_wrapper = true
pkg_config_libdir = '$pkgconfig_dir'

[built-in options]
c_args = ['--target=aarch64-linux-android$ANDROID_API','--sysroot=$NDK_SYSROOT','-resource-dir','$CLANG_RESOURCE_DIR','-I$ANDROID_COMPAT_INCLUDE_DIR','-D__INTRODUCED_IN(api_level)=']
c_link_args = ['--target=aarch64-linux-android$ANDROID_API','--sysroot=$NDK_SYSROOT','-resource-dir','$CLANG_RESOURCE_DIR','--ld-path=$LLVM_ROOT/bin/ld.lld','-llog']
cpp_args = ['--target=aarch64-linux-android$ANDROID_API','--sysroot=$NDK_SYSROOT','-resource-dir','$CLANG_RESOURCE_DIR','-I$ANDROID_COMPAT_INCLUDE_DIR','-D__INTRODUCED_IN(api_level)=']
cpp_link_args = ['--target=aarch64-linux-android$ANDROID_API','--sysroot=$NDK_SYSROOT','-resource-dir','$CLANG_RESOURCE_DIR','--ld-path=$LLVM_ROOT/bin/ld.lld','-llog']

[host_machine]
system = 'android'
cpu_family = 'aarch64'
cpu = 'armv8'
endian = 'little'
EOF
}

EPOXY_CROSS_FILE="$TOOLCHAIN_ROOT/epoxy-android-aarch64.ini"
VIRGL_CROSS_FILE="$TOOLCHAIN_ROOT/virgl-android-aarch64.ini"
write_cross_file "$EPOXY_CROSS_FILE" "/nonexistent"

python3 -m mesonbuild.mesonmain setup "$BUILD_ROOT/libepoxy" "$LIBEPOXY_SRC" --wipe \
  "--cross-file" "$EPOXY_CROSS_FILE" \
  "-Dprefix=/usr" \
  "-Dbuildtype=release" \
  "-Ddefault_library=shared" \
  "-Dglx=no" \
  "-Dx11=false" \
  "-Degl=yes" \
  "-Dtests=false"
python3 -m mesonbuild.mesonmain compile -C "$BUILD_ROOT/libepoxy" -j "$JOBS"
DESTDIR="$SUPPORT_DESTDIR" python3 -m mesonbuild.mesonmain install -C "$BUILD_ROOT/libepoxy" --no-rebuild

if [ -f "$SUPPORT_DESTDIR/usr/lib/pkgconfig/epoxy.pc" ]; then
  sed -i "s|^prefix=/usr|prefix=$SUPPORT_DESTDIR/usr|" "$SUPPORT_DESTDIR/usr/lib/pkgconfig/epoxy.pc"
fi

write_cross_file "$VIRGL_CROSS_FILE" "$SUPPORT_DESTDIR/usr/lib/pkgconfig"

python3 -m mesonbuild.mesonmain setup "$BUILD_ROOT/virglrenderer" "$VIRGL_SRC" --wipe \
  "--cross-file" "$VIRGL_CROSS_FILE" \
  "-Dprefix=/usr" \
  "-Dbuildtype=release" \
  "-Ddefault_library=shared" \
  "-Dplatforms=egl" \
  "-Dtests=false" \
  "-Dvideo=false" \
  "-Dvenus=false" \
  "-Dtracing=none"
python3 -m mesonbuild.mesonmain compile -C "$BUILD_ROOT/virglrenderer" -j "$JOBS"
DESTDIR="$STAGE_ROOT" python3 -m mesonbuild.mesonmain install -C "$BUILD_ROOT/virglrenderer" --no-rebuild

install -m 0755 "$SUPPORT_DESTDIR/usr/lib/libepoxy.so" "$STAGE_ROOT/usr/lib/libepoxy.so"
install -m 0755 "$STAGE_ROOT/usr/lib/libvirglrenderer.so" "$JNI_ROOT/libvirglrenderer.so"
install -m 0755 "$SUPPORT_DESTDIR/usr/lib/libepoxy.so" "$JNI_ROOT/libepoxy.so"

check_forbidden_paths() {
  local elf="$1"
  if strings "$elf" | grep -Eq '/data/data/com.termux/files/usr/lib|libc\\.so\\.6|ld-linux'; then
    echo "Forbidden host/glibc provenance leaked into $elf" >&2
    exit 65
  fi
}

check_needed_surface() {
  local elf="$1"
  local required="$2"
  local disallowed="$3"
  local needed

  needed="$(readelf -d "$elf" | sed -n 's/.*Shared library: \[\(.*\)\].*/\1/p' | sort -u)"
  echo "needed[$elf]"
  printf '%s\n' "$needed"

  while IFS= read -r entry; do
    [ -n "$entry" ] || continue
    if ! printf '%s\n' "$needed" | grep -Fxq "$entry"; then
      echo "Missing required dependency $entry in $elf" >&2
      exit 65
    fi
  done <<EOF
$required
EOF

  while IFS= read -r entry; do
    [ -n "$entry" ] || continue
    if printf '%s\n' "$needed" | grep -Fxq "$entry"; then
      echo "Disallowed dependency $entry in $elf" >&2
      exit 65
    fi
  done <<EOF
$disallowed
EOF
}

check_forbidden_paths "$JNI_ROOT/libepoxy.so"
check_forbidden_paths "$JNI_ROOT/libvirglrenderer.so"
check_needed_surface "$JNI_ROOT/libepoxy.so" $'libdl.so\nlibc.so' $'libdrm.so\nlibgbm.so\nlibcutils.so\nlibhardware.so\nlibnativewindow.so\nlibsync.so\nlibc++.so\nlibc++_shared.so'
check_needed_surface "$JNI_ROOT/libvirglrenderer.so" $'libepoxy.so\nlibm.so\nliblog.so\nlibc.so' $'libdrm.so\nlibgbm.so\nlibcutils.so\nlibhardware.so\nlibnativewindow.so\nlibsync.so\nlibc++.so\nlibc++_shared.so'

sha256sum "$JNI_ROOT/libepoxy.so" "$JNI_ROOT/libvirglrenderer.so" > "$RUN_ROOT/jni-sha256.txt"
cat > "$RUN_ROOT/jni-manifest.json" <<EOF
{
  "runRoot": "$RUN_ROOT",
  "jniRoot": "$RUN_ROOT/jni",
  "androidApi": "$ANDROID_API",
  "compiler": "$LLVM_ROOT/bin/clang",
  "libepoxyCommit": "$LIBEPOXY_COMMIT",
  "virglBaseCommit": "$VIRGL_BASE_COMMIT",
  "virglOverlayCommit": "$VIRGL_OVERLAY_COMMIT",
  "files": [
    "arm64-v8a/libepoxy.so",
    "arm64-v8a/libvirglrenderer.so"
  ]
}
EOF

tar -C "$RUN_ROOT/jni" --zstd -cf "$PACKAGE_ROOT/virgl-wrapper-jni-android.tar.zst" .
sha256sum "$PACKAGE_ROOT/virgl-wrapper-jni-android.tar.zst" > "$PACKAGE_ROOT/virgl-wrapper-jni-android.tar.zst.sha256"

printf '%s\n' "$RUN_ROOT" > "$OUT_ROOT/latest-virgl-wrapper-run.txt"
cp "$RUN_ROOT/jni-manifest.json" "$OUT_ROOT/latest-virgl-wrapper-manifest.json"

echo "virgl_base_commit=$VIRGL_BASE_COMMIT"
echo "virgl_overlay_commit=$VIRGL_OVERLAY_COMMIT"
echo "libepoxy_commit=$LIBEPOXY_COMMIT"
echo "jni_root=$JNI_ROOT"
echo "manifest=$RUN_ROOT/jni-manifest.json"
