#!/usr/bin/env bash
set -euo pipefail

ROOT="${ROOT:-/data/data/com.termux/files/home}"
AESOLATOR_ROOT="${AESOLATOR_ROOT:-$ROOT/aesolator}"
MESA_REPO="${MESA_REPO:-$ROOT/.cache/donors/mesa-main}"
MESA_REF="${MESA_REF:-origin/staging/26.1}"
AEMALI_MESA_PATCH_DIR="${AEMALI_MESA_PATCH_DIR:-$AESOLATOR_ROOT/patches/mesa/aemali}"
ENABLE_AEMALI_MESA_PATCHSET="${ENABLE_AEMALI_MESA_PATCHSET:-1}"
LIBDRM_REPO="${LIBDRM_REPO:-$ROOT/.cache/donors/libdrm}"
LIBDRM_URL="${LIBDRM_URL:-https://gitlab.freedesktop.org/mesa/drm.git}"
LIBDRM_REF="${LIBDRM_REF:-libdrm-2.4.131}"
REFRESH_UPSTREAM_REFS="${REFRESH_UPSTREAM_REFS:-0}"
ALLOW_MAIN="${ALLOW_MAIN:-0}"
LANE="${1:-turnip-android}"
ACTION="${ACTION:-all}"
JOBS="${JOBS:-8}"
ANDROID_API="${ANDROID_API:-34}"
LLVM_VERSION="${LLVM_VERSION:-22.1.1}"
LLVM_ROOT="${LLVM_ROOT:-$ROOT/.toolchains/llvm-22.1.1-termux}"
NDK_ROOT="${NDK_ROOT:-$ROOT/android-sdk/ndk/29.0.14206865}"
NDK_PREBUILT_ROOT="${NDK_PREBUILT_ROOT:-}"
NDK_HOST_TAG="${NDK_HOST_TAG:-}"
NDK_SYSROOT="${NDK_SYSROOT:-}"
TERMUX_PREFIX="${TERMUX_PREFIX:-/data/data/com.termux/files/usr}"
ANDROID_SYSROOT="${ANDROID_SYSROOT:-}"
CLANG_RESOURCE_DIR="${CLANG_RESOURCE_DIR:-}"
HOST_TARGET="${HOST_TARGET:-aarch64-unknown-linux-android$ANDROID_API}"
HOST_CLANG_RESOURCE_DIR="${HOST_CLANG_RESOURCE_DIR:-}"
HOST_PKG_CONFIG="${HOST_PKG_CONFIG:-$(command -v pkg-config || true)}"
HOST_LIBCXX_SHARED="${HOST_LIBCXX_SHARED:-}"
ANDROID_SYSVSHM_SRC_DIR="${ANDROID_SYSVSHM_SRC_DIR:-}"
ANDROID_SYSVSHM_LIB="${ANDROID_SYSVSHM_LIB:-}"
ALLOW_SYSTEM_LLVM_FOR_CLC="${ALLOW_SYSTEM_LLVM_FOR_CLC:-0}"
LLVM_PROJECT_SRC="${LLVM_PROJECT_SRC:-$ROOT/.toolchains-src/llvm-project-$LLVM_VERSION}"
LLVM_PROJECT_ARCHIVE="${LLVM_PROJECT_ARCHIVE:-$ROOT/.toolchains-src/llvm-project-$LLVM_VERSION.tar.gz}"
LLVM_PROJECT_URL="${LLVM_PROJECT_URL:-https://github.com/llvm/llvm-project/archive/refs/tags/llvmorg-$LLVM_VERSION.tar.gz}"
SPIRV_LLVM_TRANSLATOR_REPO="${SPIRV_LLVM_TRANSLATOR_REPO:-$ROOT/.cache/donors/SPIRV-LLVM-Translator}"
SPIRV_LLVM_TRANSLATOR_URL="${SPIRV_LLVM_TRANSLATOR_URL:-https://github.com/KhronosGroup/SPIRV-LLVM-Translator.git}"
SPIRV_LLVM_TRANSLATOR_REF="${SPIRV_LLVM_TRANSLATOR_REF:-v$LLVM_VERSION}"
OUT_ROOT="${OUT_ROOT:-$AESOLATOR_ROOT/out/graphics-source-builds}"
PYTHON_BIN="${PYTHON_BIN:-python3}"
GRAPHICS_META_TOOL="${GRAPHICS_META_TOOL:-$AESOLATOR_ROOT/tools/generate_graphics_driver_meta.py}"
PATCH_HUNK_VALIDATOR="${PATCH_HUNK_VALIDATOR:-$AESOLATOR_ROOT/tools/validate_unified_patch_hunks.py}"
INSTALL_APP_ASSET="${INSTALL_APP_ASSET:-0}"
APP_GRAPHICS_ASSET_DIR="${APP_GRAPHICS_ASSET_DIR:-$AESOLATOR_ROOT/app/src/main/assets/graphics_driver}"

usage() {
  cat <<'EOF'
Usage:
  build-mesa-staging-graphics.sh <lane>

Lanes:
  turnip-android         Vulkan/Turnip KGSL Android driver from Mesa staging.
  panvk-android          Vulkan/PanVK Mali driver from Mesa staging; experimental on Android.
  mali-gallium-android   OpenGL/GLES Mali Gallium lane with Panfrost/Lima/Zink/softpipe.
  zink-android           Android Zink OpenGL-over-Vulkan lane with Turnip KGSL.
  virgl-gallium-android  Android Mesa Gallium VirGL lane for source parity probes.
  mesa-x11-bionic        Bionic/X11 desktop OpenGL payload for shipped Zink/VirGL assets.

Environment:
  MESA_REF     Default: origin/staging/26.1. Use auto, auto-staging, or freshest-nonmain
               to pick the newest origin/staging/* line.
               main/origin/main is rejected unless ALLOW_MAIN=1.
  LIBDRM_REF   Default: libdrm-2.4.131. Used for source-built libdrm support in Gallium lanes.
  REFRESH_UPSTREAM_REFS
               Set to 1 with MESA_REF=auto, auto-staging, or freshest-nonmain
               to fetch origin staging refs before selection.
  ACTION       plan | setup | compile | install | package | all. Default: all.
  JOBS         Ninja jobs. Default: 8.
  ANDROID_API  Android platform-sdk-version. Default: 34.
  ANDROID_SYSVSHM_SRC_DIR
               Default: prefers $AESOLATOR_ROOT/android_sysvshm, then
               $ROOT/freewine11/android/android_sysvshm.
  ANDROID_SYSVSHM_LIB
               Optional prebuilt override. Default is a source-built support lib under RUN_ROOT.
  ENABLE_AEMALI_MESA_PATCHSET
               Default: 1. Applies AeMali downstream PanVK/Panfrost/Lima
               policy patches for Mali lanes before Meson setup.
  ALLOW_SYSTEM_LLVM_FOR_CLC
               Default: 0. PanVK/Mali lanes require the LLVM/CLC provider from
               LLVM_ROOT. Set to 1 only for an explicit diagnostic probe with
               the host LLVM, never for Chapter 2 release payloads.
  LLVM_PROJECT_SRC
               Default: $ROOT/.toolchains-src/llvm-project-$LLVM_VERSION.
               Used to build libclc support for PanVK/Mali lanes.
  SPIRV_LLVM_TRANSLATOR_REF
               Default: v$LLVM_VERSION. Used to build LLVMSPIRVLib/llvm-spirv
               support for Mesa CLC without falling back to older Termux LLVM.
EOF
}

case "${1:-}" in
  -h|--help)
    usage
    exit 0
    ;;
esac

case "$LANE" in
  turnip-android|panvk-android|mali-gallium-android|zink-android|virgl-gallium-android|mesa-x11-bionic) ;;
  *)
    echo "Unsupported lane: $LANE" >&2
    usage >&2
    exit 64
    ;;
esac

case "$ACTION" in
  plan|setup|compile|install|package|all) ;;
  *)
    echo "Unsupported ACTION=$ACTION" >&2
    usage >&2
    exit 64
    ;;
esac

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

resolve_android_sysroot() {
  if [ -n "${ANDROID_SYSROOT:-}" ] && [ -d "$ANDROID_SYSROOT/usr/include" ]; then
    printf '%s\n' "$ANDROID_SYSROOT"
    return 0
  fi

  if [ -d "/data/data/com.termux/files/usr/include" ]; then
    printf '%s\n' "/data/data/com.termux/files"
    return 0
  fi

  if [ -n "${NDK_SYSROOT:-}" ] && [ -d "$NDK_SYSROOT/usr/include" ]; then
    printf '%s\n' "$NDK_SYSROOT"
    return 0
  fi

  if [ -n "${NDK_PREBUILT_ROOT:-}" ] && [ -d "$NDK_PREBUILT_ROOT/sysroot/usr/include" ]; then
    printf '%s\n' "$NDK_PREBUILT_ROOT/sysroot"
    return 0
  fi

  return 1
}

resolve_clang_resource_dir() {
  local requested="$1"
  local resolved=""

  if [ -n "$requested" ] && [ -d "$requested/include" ]; then
    printf '%s\n' "$requested"
    return 0
  fi

  if [ -x "$LLVM_ROOT/bin/clang" ]; then
    resolved="$("$LLVM_ROOT/bin/clang" --print-resource-dir 2>/dev/null || true)"
    if [ -n "$resolved" ] && [ -d "$resolved/include" ]; then
      printf '%s\n' "$resolved"
      return 0
    fi
  fi

  if [ -n "${NDK_PREBUILT_ROOT:-}" ] && [ -d "$NDK_PREBUILT_ROOT/lib/clang" ]; then
    find "$NDK_PREBUILT_ROOT/lib/clang" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -n 1
    return 0
  fi

  if [ -d "$TERMUX_PREFIX/lib/clang" ]; then
    find "$TERMUX_PREFIX/lib/clang" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -n 1
    return 0
  fi

  return 1
}

resolve_host_libcxx_shared() {
  if [ -n "${HOST_LIBCXX_SHARED:-}" ] && [ -f "$HOST_LIBCXX_SHARED" ]; then
    printf '%s\n' "$HOST_LIBCXX_SHARED"
    return 0
  fi

  if [ -f "$TERMUX_PREFIX/lib/libc++_shared.so" ]; then
    printf '%s\n' "$TERMUX_PREFIX/lib/libc++_shared.so"
    return 0
  fi

  if [ -n "${ANDROID_SYSROOT:-}" ] && [ -f "$ANDROID_SYSROOT/usr/lib/aarch64-linux-android/libc++_shared.so" ]; then
    printf '%s\n' "$ANDROID_SYSROOT/usr/lib/aarch64-linux-android/libc++_shared.so"
    return 0
  fi

  if [ -n "${NDK_SYSROOT:-}" ] && [ -f "$NDK_SYSROOT/usr/lib/aarch64-linux-android/libc++_shared.so" ]; then
    printf '%s\n' "$NDK_SYSROOT/usr/lib/aarch64-linux-android/libc++_shared.so"
    return 0
  fi

  return 1
}

resolve_android_sysvshm_src_dir() {
  local candidate

  if [ -n "${ANDROID_SYSVSHM_SRC_DIR:-}" ] && [ -f "$ANDROID_SYSVSHM_SRC_DIR/android_sysvshm.c" ]; then
    printf '%s\n' "$ANDROID_SYSVSHM_SRC_DIR"
    return 0
  fi

  for candidate in \
    "$AESOLATOR_ROOT/android_sysvshm" \
    "$ROOT/aesolator/android_sysvshm" \
    "$ROOT/freewine11/android/android_sysvshm"
  do
    if [ -f "$candidate/android_sysvshm.c" ]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done

  return 1
}

for tool in cmake curl git ninja patchelf pkg-config tar zstd; do
  require_tool "$tool"
done
require_tool "$PYTHON_BIN"
require_path "$GRAPHICS_META_TOOL"
require_path "$HOST_PKG_CONFIG"
require_path "$LLVM_ROOT/bin/clang"
require_path "$LLVM_ROOT/bin/clang++"
require_path "$LLVM_ROOT/bin/llvm-ar"
require_path "$LLVM_ROOT/bin/llvm-ranlib"
require_path "$LLVM_ROOT/bin/llvm-strip"
require_path "$LLVM_ROOT/bin/ld.lld"
require_path "$LLVM_ROOT/bin/llvm-config"
NDK_PREBUILT_ROOT="$(resolve_ndk_prebuilt_root || true)"
[ -n "${NDK_SYSROOT:-}" ] || [ -z "${NDK_PREBUILT_ROOT:-}" ] || NDK_SYSROOT="$NDK_PREBUILT_ROOT/sysroot"
ANDROID_SYSROOT="$(resolve_android_sysroot)"
CLANG_RESOURCE_DIR="$(resolve_clang_resource_dir "$CLANG_RESOURCE_DIR")"
HOST_CLANG_RESOURCE_DIR="$(resolve_clang_resource_dir "$HOST_CLANG_RESOURCE_DIR")"
HOST_LIBCXX_SHARED="$(resolve_host_libcxx_shared)"
ANDROID_SYSVSHM_SRC_DIR="$(resolve_android_sysvshm_src_dir)"
require_path "$ANDROID_SYSROOT/usr/include"
require_path "$CLANG_RESOURCE_DIR/include/stddef.h"
require_path "$HOST_CLANG_RESOURCE_DIR/include/stddef.h"
require_path "$HOST_LIBCXX_SHARED"
if [ -n "$ANDROID_SYSVSHM_LIB" ]; then
  require_path "$ANDROID_SYSVSHM_LIB"
fi
require_path "$ANDROID_SYSVSHM_SRC_DIR/android_sysvshm.c"
require_path "$ANDROID_SYSVSHM_SRC_DIR/android_sysvshm.exports.map.txt"
require_path "$ANDROID_SYSVSHM_SRC_DIR/sys/shm.h"

if [ ! -d "$MESA_REPO/.git" ]; then
  echo "Mesa donor cache is not a git worktree: $MESA_REPO" >&2
  exit 69
fi

resolve_mesa_ref() {
  case "$MESA_REF" in
    auto|auto-staging|freshest-nonmain)
      if [ "$REFRESH_UPSTREAM_REFS" = "1" ]; then
        git -C "$MESA_REPO" fetch --prune origin \
          '+refs/heads/staging/*:refs/remotes/origin/staging/*'
      fi
      git -C "$MESA_REPO" for-each-ref \
        --format='%(committerdate:iso8601-strict)%09%(refname:short)%09%(objectname)%09%(subject)' \
        refs/remotes/origin/staging |
        "$PYTHON_BIN" -c '
import re
import sys

rows = []
for line in sys.stdin:
    line = line.rstrip("\n")
    if not line:
        continue
    date, ref, sha, subject = line.split("\t", 3)
    match = re.search(r"staging/([0-9]+)\.([0-9]+)$", ref)
    if not match:
        continue
    rows.append(((int(match.group(1)), int(match.group(2))), date, ref, sha, subject))

if not rows:
    raise SystemExit("No origin/staging/* Mesa refs found")

rows.sort(key=lambda row: (row[0], row[1]), reverse=True)
print(rows[0][2])
'
      ;;
    *)
      printf '%s\n' "$MESA_REF"
      ;;
  esac
}

RESOLVED_MESA_REF="$(resolve_mesa_ref)"

case "$MESA_REF" in
  main|origin/main|refs/heads/main|refs/remotes/origin/main|*/main)
    if [ "$ALLOW_MAIN" != "1" ]; then
      echo "Refusing Mesa main as release payload source: MESA_REF=$MESA_REF" >&2
      echo "Use a regularly updated non-main line such as origin/staging/26.1, or set ALLOW_MAIN=1 for a one-off probe." >&2
      exit 65
    fi
    ;;
esac

case "$RESOLVED_MESA_REF" in
  main|origin/main|refs/heads/main|refs/remotes/origin/main|*/main)
    if [ "$ALLOW_MAIN" != "1" ]; then
      echo "Refusing resolved Mesa main as release payload source: RESOLVED_MESA_REF=$RESOLVED_MESA_REF" >&2
      exit 65
    fi
    ;;
esac

COMMIT="$(git -C "$MESA_REPO" rev-parse "$RESOLVED_MESA_REF^{commit}")"
SHORT_COMMIT="$(printf '%s' "$COMMIT" | cut -c1-12)"
COMMIT_DATE="$(git -C "$MESA_REPO" show -s --format='%cI' "$COMMIT")"
COMMIT_TITLE="$(git -C "$MESA_REPO" show -s --format='%s' "$COMMIT")"

sanitize_asset_fragment() {
  printf '%s' "$1" | tr '/:' '--' | tr -c 'A-Za-z0-9._-' '-'
}

derive_mesa_asset_series() {
  case "$RESOLVED_MESA_REF" in
    refs/remotes/origin/staging/*|origin/staging/*)
      printf '%s-staging' "$(sanitize_asset_fragment "${RESOLVED_MESA_REF##*/}")"
      ;;
    refs/remotes/origin/*|origin/*)
      printf '%s' "$(sanitize_asset_fragment "${RESOLVED_MESA_REF##*/}")"
      ;;
    *)
      printf '%s' "$(sanitize_asset_fragment "$RESOLVED_MESA_REF")"
      ;;
  esac
}

MESA_ASSET_SERIES="$(derive_mesa_asset_series)"
MESA_ASSET_VERSION="${MESA_ASSET_SERIES}-${SHORT_COMMIT}"

TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
RUN_ROOT="${RUN_ROOT:-$OUT_ROOT/$TIMESTAMP-$LANE-$ACTION-$SHORT_COMMIT}"
SRC_DIR="$RUN_ROOT/src/mesa"
BUILD_DIR="$RUN_ROOT/build"
DESTDIR="$RUN_ROOT/stage"
PACKAGE_DIR="$RUN_ROOT/package"
LOG_DIR="$RUN_ROOT/logs"
TOOLCHAIN_DIR="$RUN_ROOT/toolchain"
mkdir -p "$RUN_ROOT" "$LOG_DIR" "$TOOLCHAIN_DIR" "$PACKAGE_DIR"
LOG_FILE="$LOG_DIR/build.log"

exec > >(tee -a "$LOG_FILE") 2>&1

build_android_sysvshm_support() {
  local support_dir out_file

  if [ "$ACTION" = "plan" ]; then
    ANDROID_SYSVSHM_LIB_DIR="$TOOLCHAIN_DIR/support"
    return
  fi

  if [ -n "$ANDROID_SYSVSHM_LIB" ]; then
    ANDROID_SYSVSHM_LIB_DIR="$(dirname "$ANDROID_SYSVSHM_LIB")"
    return
  fi

  support_dir="$TOOLCHAIN_DIR/support"
  mkdir -p "$support_dir"
  out_file="$support_dir/libandroid-sysvshm.so"
  "$LLVM_ROOT/bin/clang" \
    "--target=aarch64-unknown-linux-android$ANDROID_API" \
    "--sysroot=$ANDROID_SYSROOT" \
    "-resource-dir" "$CLANG_RESOURCE_DIR" \
    "-B$TERMUX_PREFIX/bin" \
    -Wall -Wextra -std=gnu99 -fPIC -shared \
    -I"$ANDROID_SYSVSHM_SRC_DIR" \
    -L"$TERMUX_PREFIX/lib" \
    -L/system/lib64 \
    "--ld-path=$LLVM_ROOT/bin/ld.lld" \
    -Wl,-dynamic-linker,/system/bin/linker64 \
    -Wl,--version-script="$ANDROID_SYSVSHM_SRC_DIR/android_sysvshm.exports.map.txt" \
    -Wl,-soname,libandroid-sysvshm.so \
    -o "$out_file" \
    "$ANDROID_SYSVSHM_SRC_DIR/android_sysvshm.c"
  install -m 0644 "$ANDROID_SYSVSHM_SRC_DIR/sys/shm.h" "$support_dir/shm.h"
  ANDROID_SYSVSHM_LIB="$out_file"
  ANDROID_SYSVSHM_LIB_DIR="$support_dir"
}

build_android_sysvshm_support

AUTO_LINK_ANDROID_SYSVSHM=1

echo "lane=$LANE"
echo "action=$ACTION"
echo "mesa_ref=$MESA_REF"
echo "resolved_mesa_ref=$RESOLVED_MESA_REF"
echo "mesa_commit=$COMMIT"
echo "mesa_commit_date=$COMMIT_DATE"
echo "mesa_commit_title=$COMMIT_TITLE"
echo "mesa_asset_series=$MESA_ASSET_SERIES"
echo "mesa_asset_version=$MESA_ASSET_VERSION"
echo "llvm_root=$LLVM_ROOT"
echo "clang=$LLVM_ROOT/bin/clang"
echo "android_api=$ANDROID_API"
echo "host_target=$HOST_TARGET"
echo "android_sysvshm_lib=$ANDROID_SYSVSHM_LIB"
echo "android_sysvshm_src_dir=$ANDROID_SYSVSHM_SRC_DIR"
echo "libdrm_ref=$LIBDRM_REF"
echo "run_root=$RUN_ROOT"
echo "aemali_mesa_patch_dir=$AEMALI_MESA_PATCH_DIR"
echo "enable_aemali_mesa_patchset=$ENABLE_AEMALI_MESA_PATCHSET"
echo "allow_system_llvm_for_clc=$ALLOW_SYSTEM_LLVM_FOR_CLC"

ANDROID_COMPAT_INCLUDE_DIR="$TOOLCHAIN_DIR/android-compat/include"
MESA_ANDROID_STUB_INCLUDE_DIR="$SRC_DIR/include/android_stub"
mkdir -p "$ANDROID_COMPAT_INCLUDE_DIR/cutils" "$ANDROID_COMPAT_INCLUDE_DIR/log"
cat > "$ANDROID_COMPAT_INCLUDE_DIR/cutils/trace.h" <<'EOF'
#ifndef AERO_ANDROID_COMPAT_CUTILS_TRACE_H
#define AERO_ANDROID_COMPAT_CUTILS_TRACE_H

#include <stdint.h>

#ifndef AERO_ANDROID_COMPAT_NO_ATRACE
#include <android/trace.h>
#endif

#ifdef __cplusplus
extern "C" {
#endif

#ifndef ATRACE_TAG_NOT_READY
#define ATRACE_TAG_NOT_READY 0ULL
#endif

#ifndef ATRACE_TAG_GRAPHICS
#define ATRACE_TAG_GRAPHICS (1ULL << 1)
#endif

#ifndef AERO_ANDROID_COMPAT_NO_ATRACE
static inline void atrace_begin(uint64_t tag, const char *name)
{
  (void)tag;
  ATrace_beginSection(name);
}

static inline void atrace_end(uint64_t tag)
{
  (void)tag;
  ATrace_endSection();
}

static inline void atrace_init(void)
{
}
#else
static inline void atrace_begin(uint64_t tag, const char *name)
{
  (void)tag;
  (void)name;
}

static inline void atrace_end(uint64_t tag)
{
  (void)tag;
}

static inline void atrace_init(void)
{
}
#endif

#ifdef __cplusplus
}
#endif

#endif
EOF

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

cat > "$ANDROID_COMPAT_INCLUDE_DIR/cutils/log.h" <<'EOF'
#ifndef AERO_ANDROID_COMPAT_CUTILS_LOG_H
#define AERO_ANDROID_COMPAT_CUTILS_LOG_H

#include <log/log.h>

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

cat > "$ANDROID_COMPAT_INCLUDE_DIR/cutils/native_handle.h" <<'EOF'
#ifndef AERO_ANDROID_COMPAT_CUTILS_NATIVE_HANDLE_H
#define AERO_ANDROID_COMPAT_CUTILS_NATIVE_HANDLE_H

#include <stdalign.h>

#ifdef __cplusplus
extern "C" {
#endif

#define NATIVE_HANDLE_MAX_FDS 1024
#define NATIVE_HANDLE_MAX_INTS 1024

#define NATIVE_HANDLE_DECLARE_STORAGE(name, maxFds, maxInts) \
  alignas(native_handle_t) char (name)[sizeof(native_handle_t) + sizeof(int) * ((maxFds) + (maxInts))]

typedef struct native_handle
{
  int version;
  int numFds;
  int numInts;
#if defined(__clang__)
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wzero-length-array"
#endif
  int data[0];
#if defined(__clang__)
#pragma clang diagnostic pop
#endif
} native_handle_t;

typedef const native_handle_t *buffer_handle_t;

int native_handle_close(const native_handle_t *h);
native_handle_t *native_handle_init(char *storage, int numFds, int numInts);
native_handle_t *native_handle_create(int numFds, int numInts);
native_handle_t *native_handle_clone(const native_handle_t *handle);
int native_handle_delete(native_handle_t *h);

#ifdef __cplusplus
}
#endif

#endif
EOF

cat > "$TOOLCHAIN_DIR/clang-android22" <<EOF
#!/usr/bin/env bash
link_mode=1
for arg in "\$@"; do
  case "\$arg" in
    -c|-E|-S|-M|-MM) link_mode=0 ;;
  esac
done
base=(
  "$LLVM_ROOT/bin/clang"
  "--target=aarch64-unknown-linux-android$ANDROID_API"
  "--sysroot=$ANDROID_SYSROOT"
  "-resource-dir" "$CLANG_RESOURCE_DIR"
  "-B$TERMUX_PREFIX/bin"
  "-I$MESA_ANDROID_STUB_INCLUDE_DIR"
)
  if [ "\$link_mode" -eq 1 ]; then
  extra=(
    -L"$TOOLCHAIN_DIR/support/usr/lib"
    -L"$TERMUX_PREFIX/lib"
    -L"$ANDROID_SYSVSHM_LIB_DIR"
    -L/system/lib64
    "--ld-path=$LLVM_ROOT/bin/ld.lld"
    -Wl,-dynamic-linker,/system/bin/linker64
    -landroid
    -llog
  )
  if [ "$AUTO_LINK_ANDROID_SYSVSHM" = "1" ]; then
    extra+=(-landroid-sysvshm)
  fi
  exec "\${base[@]}" "\$@" "\${extra[@]}"
fi
exec "\${base[@]}" "\$@"
EOF

cat > "$TOOLCHAIN_DIR/clangxx-android22" <<EOF
#!/usr/bin/env bash
link_mode=1
for arg in "\$@"; do
  case "\$arg" in
    -c|-E|-S|-M|-MM) link_mode=0 ;;
  esac
done
base=(
  "$LLVM_ROOT/bin/clang++"
  "--target=aarch64-unknown-linux-android$ANDROID_API"
  "--sysroot=$ANDROID_SYSROOT"
  "-resource-dir" "$CLANG_RESOURCE_DIR"
  "-B$TERMUX_PREFIX/bin"
  "-I$MESA_ANDROID_STUB_INCLUDE_DIR"
)
  if [ "\$link_mode" -eq 1 ]; then
  extra=(
    -L"$TOOLCHAIN_DIR/support/usr/lib"
    -L"$TERMUX_PREFIX/lib"
    -L"$ANDROID_SYSVSHM_LIB_DIR"
    -L/system/lib64
    "--ld-path=$LLVM_ROOT/bin/ld.lld"
    -Wl,-dynamic-linker,/system/bin/linker64
    -landroid
    -llog
  )
  if [ "$AUTO_LINK_ANDROID_SYSVSHM" = "1" ]; then
    extra+=(-landroid-sysvshm)
  fi
  extra+=(-l:libc++_shared.so)
  exec "\${base[@]}" "\$@" "\${extra[@]}"
fi
exec "\${base[@]}" "\$@"
EOF

cat > "$TOOLCHAIN_DIR/clang-native22" <<EOF
#!/usr/bin/env bash
link_mode=1
for arg in "\$@"; do
  case "\$arg" in
    -c|-E|-S|-M|-MM) link_mode=0 ;;
  esac
done
base=(
  "$LLVM_ROOT/bin/clang"
  "--target=$HOST_TARGET"
  "--sysroot=$ANDROID_SYSROOT"
  "-resource-dir" "$HOST_CLANG_RESOURCE_DIR"
  "-B$TERMUX_PREFIX/bin"
  "-femulated-tls"
  "-DAERO_ANDROID_COMPAT_NO_ATRACE=1"
  "-I$ANDROID_COMPAT_INCLUDE_DIR"
  "-I$MESA_ANDROID_STUB_INCLUDE_DIR"
)
if [ "\$link_mode" -eq 1 ]; then
  extra=(
    -L"$TOOLCHAIN_DIR/support/usr/lib"
    -L"$TERMUX_PREFIX/lib"
    -L/system/lib64
    "--ld-path=$LLVM_ROOT/bin/ld.lld"
    -Wl,-dynamic-linker,/system/bin/linker64
    -Wl,-rpath,$LLVM_ROOT/lib
    -Wl,-rpath,$TERMUX_PREFIX/lib
    -llog
  )
  exec "\${base[@]}" "\$@" "\${extra[@]}"
fi
exec "\${base[@]}" "\$@"
EOF

cat > "$TOOLCHAIN_DIR/clangxx-native22" <<EOF
#!/usr/bin/env bash
link_mode=1
for arg in "\$@"; do
  case "\$arg" in
    -c|-E|-S|-M|-MM) link_mode=0 ;;
  esac
done
base=(
  "$LLVM_ROOT/bin/clang++"
  "--target=$HOST_TARGET"
  "--sysroot=$ANDROID_SYSROOT"
  "-resource-dir" "$HOST_CLANG_RESOURCE_DIR"
  "-B$TERMUX_PREFIX/bin"
  "-femulated-tls"
  "-DAERO_ANDROID_COMPAT_NO_ATRACE=1"
  "-I$ANDROID_COMPAT_INCLUDE_DIR"
  "-I$MESA_ANDROID_STUB_INCLUDE_DIR"
)
if [ "\$link_mode" -eq 1 ]; then
  extra=(
    -L"$TOOLCHAIN_DIR/support/usr/lib"
    -L"$TERMUX_PREFIX/lib"
    -L/system/lib64
    "--ld-path=$LLVM_ROOT/bin/ld.lld"
    -Wl,-dynamic-linker,/system/bin/linker64
    -Wl,-rpath,$LLVM_ROOT/lib
    -Wl,-rpath,$TERMUX_PREFIX/lib
    -llog
    -l:libc++_shared.so
  )
  exec "\${base[@]}" "\$@" "\${extra[@]}"
fi
exec "\${base[@]}" "\$@"
EOF
chmod 755 \
  "$TOOLCHAIN_DIR/clang-android22" \
  "$TOOLCHAIN_DIR/clangxx-android22" \
  "$TOOLCHAIN_DIR/clang-native22" \
  "$TOOLCHAIN_DIR/clangxx-native22"

cat > "$TOOLCHAIN_DIR/android-aarch64-llvm22.ini" <<EOF
[binaries]
c = '$TOOLCHAIN_DIR/clang-android22'
cpp = '$TOOLCHAIN_DIR/clangxx-android22'
ar = '$LLVM_ROOT/bin/llvm-ar'
ranlib = '$LLVM_ROOT/bin/llvm-ranlib'
strip = '$LLVM_ROOT/bin/llvm-strip'
llvm-config = '$LLVM_ROOT/bin/llvm-config'
pkg-config = '$HOST_PKG_CONFIG'

[properties]
pkg_config_libdir = '$TOOLCHAIN_DIR/support/usr/lib/pkgconfig:$TOOLCHAIN_DIR/support/usr/share/pkgconfig:$LLVM_ROOT/lib/pkgconfig:$TERMUX_PREFIX/lib/pkgconfig:$TERMUX_PREFIX/share/pkgconfig'
needs_exe_wrapper = true

[host_machine]
system = 'android'
cpu_family = 'aarch64'
cpu = 'armv8'
endian = 'little'
EOF

cat > "$TOOLCHAIN_DIR/native-aarch64-llvm22.ini" <<EOF
[binaries]
c = '$TOOLCHAIN_DIR/clang-native22'
cpp = '$TOOLCHAIN_DIR/clangxx-native22'
ar = '$LLVM_ROOT/bin/llvm-ar'
ranlib = '$LLVM_ROOT/bin/llvm-ranlib'
strip = '$LLVM_ROOT/bin/llvm-strip'
llvm-config = '$LLVM_ROOT/bin/llvm-config'
pkg-config = '$HOST_PKG_CONFIG'

[built-in options]
c_args = ['-Qunused-arguments']
cpp_args = ['-Qunused-arguments']
EOF

clc_pkg_config_path() {
  printf '%s:%s:%s:%s:%s' \
    "$TOOLCHAIN_DIR/support/usr/lib/pkgconfig" \
    "$TOOLCHAIN_DIR/support/usr/share/pkgconfig" \
    "$LLVM_ROOT/lib/pkgconfig" \
    "$TERMUX_PREFIX/lib/pkgconfig" \
    "$TERMUX_PREFIX/share/pkgconfig"
}

clc_env() {
  env \
    PATH="$TOOLCHAIN_DIR/support/usr/bin:$LLVM_ROOT/bin:$PATH" \
    LD_LIBRARY_PATH="$TOOLCHAIN_DIR/support/usr/lib:$LLVM_ROOT/lib:${LD_LIBRARY_PATH:-}" \
    PKG_CONFIG_PATH="$(clc_pkg_config_path):${PKG_CONFIG_PATH:-}" \
    LLVM_CONFIG="$LLVM_ROOT/bin/llvm-config" \
    CMAKE_PREFIX_PATH="$TOOLCHAIN_DIR/support/usr:$LLVM_ROOT:$TERMUX_PREFIX:${CMAKE_PREFIX_PATH:-}" \
    "$@"
}

fetch_llvm_project_source_for_libclc() {
  if [ -d "$LLVM_PROJECT_SRC/libclc" ]; then
    return
  fi

  mkdir -p "$(dirname "$LLVM_PROJECT_ARCHIVE")"
  if [ ! -f "$LLVM_PROJECT_ARCHIVE" ]; then
    curl --fail --location --retry 5 --retry-delay 3 \
      --output "$LLVM_PROJECT_ARCHIVE" "$LLVM_PROJECT_URL"
  fi

  if [ ! -d "$LLVM_PROJECT_SRC" ]; then
    tar -C "$(dirname "$LLVM_PROJECT_SRC")" -xf "$LLVM_PROJECT_ARCHIVE"
    if [ -d "$(dirname "$LLVM_PROJECT_SRC")/llvm-project-llvmorg-$LLVM_VERSION" ]; then
      mv "$(dirname "$LLVM_PROJECT_SRC")/llvm-project-llvmorg-$LLVM_VERSION" "$LLVM_PROJECT_SRC"
    fi
  fi

  require_path "$LLVM_PROJECT_SRC/libclc/CMakeLists.txt"
}

ensure_spirv_llvm_translator_support() {
  local support_prefix="$TOOLCHAIN_DIR/support/usr"
  local src="$TOOLCHAIN_DIR/support-src/SPIRV-LLVM-Translator"
  local build="$TOOLCHAIN_DIR/support-build/SPIRV-LLVM-Translator"
  local commit

  if clc_env pkg-config --exists LLVMSPIRVLib && [ -x "$support_prefix/bin/llvm-spirv" ]; then
    return
  fi

  if [ ! -d "$SPIRV_LLVM_TRANSLATOR_REPO/.git" ]; then
    mkdir -p "$(dirname "$SPIRV_LLVM_TRANSLATOR_REPO")"
    git clone --filter=blob:none "$SPIRV_LLVM_TRANSLATOR_URL" "$SPIRV_LLVM_TRANSLATOR_REPO"
  fi
  git -C "$SPIRV_LLVM_TRANSLATOR_REPO" fetch --tags origin
  commit="$(git -C "$SPIRV_LLVM_TRANSLATOR_REPO" rev-parse "$SPIRV_LLVM_TRANSLATOR_REF^{commit}")"

  if [ ! -d "$src/.git" ]; then
    mkdir -p "$(dirname "$src")"
    git -C "$SPIRV_LLVM_TRANSLATOR_REPO" worktree add --detach "$src" "$commit"
  else
    git -C "$src" checkout --detach "$commit"
  fi

  clc_env cmake -S "$src" -B "$build" -G Ninja \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_INSTALL_PREFIX="$support_prefix" \
    -DCMAKE_C_COMPILER="$TOOLCHAIN_DIR/clang-native22" \
    -DCMAKE_CXX_COMPILER="$TOOLCHAIN_DIR/clangxx-native22" \
    -DCMAKE_AR="$LLVM_ROOT/bin/llvm-ar" \
    -DCMAKE_RANLIB="$LLVM_ROOT/bin/llvm-ranlib" \
    -DLLVM_DIR="$LLVM_ROOT/lib/cmake/llvm" \
    "-DCMAKE_INSTALL_RPATH=$LLVM_ROOT/lib;$support_prefix/lib;$TERMUX_PREFIX/lib" \
    "-DCMAKE_BUILD_RPATH=$LLVM_ROOT/lib;$support_prefix/lib;$TERMUX_PREFIX/lib" \
    -DLLVM_SPIRV_INCLUDE_TESTS=OFF \
    -DBUILD_SHARED_LIBS=OFF
  clc_env cmake --build "$build" --target install -- -j "$JOBS"
}

ensure_libclc_support() {
  local support_prefix="$TOOLCHAIN_DIR/support/usr"
  local build="$TOOLCHAIN_DIR/support-build/libclc"
  local clc_tools="$TOOLCHAIN_DIR/clc-tools"

  if clc_env pkg-config --exists libclc; then
    return
  fi

  fetch_llvm_project_source_for_libclc
  require_path "$LLVM_ROOT/bin/opt"
  require_path "$LLVM_ROOT/bin/llvm-as"
  require_path "$LLVM_ROOT/bin/llvm-link"
  require_path "$support_prefix/bin/llvm-spirv"

  mkdir -p "$clc_tools"
  ln -sf "$LLVM_ROOT/bin/clang" "$clc_tools/clang"
  ln -sf "$LLVM_ROOT/bin/llvm-as" "$clc_tools/llvm-as"
  ln -sf "$LLVM_ROOT/bin/llvm-link" "$clc_tools/llvm-link"
  ln -sf "$LLVM_ROOT/bin/opt" "$clc_tools/opt"
  ln -sf "$support_prefix/bin/llvm-spirv" "$clc_tools/llvm-spirv"

  clc_env cmake -S "$LLVM_PROJECT_SRC/libclc" -B "$build" -G Ninja \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_INSTALL_PREFIX="$support_prefix" \
    -DCMAKE_C_COMPILER="$TOOLCHAIN_DIR/clang-native22" \
    -DCMAKE_CXX_COMPILER="$TOOLCHAIN_DIR/clangxx-native22" \
    -DCMAKE_AR="$LLVM_ROOT/bin/llvm-ar" \
    -DCMAKE_RANLIB="$LLVM_ROOT/bin/llvm-ranlib" \
    -DLLVM_DIR="$LLVM_ROOT/lib/cmake/llvm" \
    "-DCMAKE_INSTALL_RPATH=$LLVM_ROOT/lib;$support_prefix/lib;$TERMUX_PREFIX/lib" \
    "-DCMAKE_BUILD_RPATH=$LLVM_ROOT/lib;$support_prefix/lib;$TERMUX_PREFIX/lib" \
    -DLLVM_TOOLS_BINARY_DIR="$clc_tools" \
    -DLIBCLC_CUSTOM_LLVM_TOOLS_BINARY_DIR="$clc_tools" \
    "-DLIBCLC_TARGETS_TO_BUILD=spirv-mesa3d-;spirv64-mesa3d-"
  clc_env cmake --build "$build" --target install -- -j "$JOBS"
}

ensure_mesa_clc_support() {
  ensure_spirv_llvm_translator_support
  ensure_libclc_support

  if ! clc_env pkg-config --exists LLVMSPIRVLib libclc SPIRV-Tools; then
    echo "PanVK/Mali lanes require pkg-config-visible LLVMSPIRVLib, libclc, and SPIRV-Tools." >&2
    clc_env pkg-config --modversion LLVMSPIRVLib libclc SPIRV-Tools >&2 || true
    exit 69
  fi

  echo "LLVMSPIRVLib_version=$(clc_env pkg-config --modversion LLVMSPIRVLib)"
  echo "libclc_version=$(clc_env pkg-config --modversion libclc)"
  echo "SPIRV-Tools_version=$(clc_env pkg-config --modversion SPIRV-Tools)"
}

verify_llvm_clc_surface() {
  local llvm_config="$LLVM_ROOT/bin/llvm-config"
  local version libdir libs_output

  case "$LANE" in
    panvk-android|mali-gallium-android) ;;
    *)
      return
      ;;
  esac

  if [ "$ACTION" = "plan" ]; then
    echo "PanVK/Mali plan records LLVM/CLC requirements; setup/compile will verify the concrete 22.1.1 link surface."
    return
  fi

  if [ "$ALLOW_SYSTEM_LLVM_FOR_CLC" = "1" ]; then
    echo "WARNING: ALLOW_SYSTEM_LLVM_FOR_CLC=1 is diagnostic-only and not a Chapter 2 release payload configuration." >&2
    llvm_config="$(command -v llvm-config)"
  fi

  version="$("$llvm_config" --version 2>/dev/null || true)"
  if [ "$ALLOW_SYSTEM_LLVM_FOR_CLC" != "1" ] && [ "$version" != "22.1.1" ]; then
    echo "PanVK/Mali lanes require LLVM_ROOT llvm-config 22.1.1 for CLC; found version='$version' at $llvm_config" >&2
    exit 69
  fi

  libdir="$("$llvm_config" --libdir 2>/dev/null || true)"
  libs_output="$("$llvm_config" --libs core 2>&1 || true)"
  if printf '%s\n' "$libs_output" | grep -qi 'missing:'; then
    echo "PanVK/Mali lanes require a complete LLVM/CLC link surface from $llvm_config." >&2
    echo "$libs_output" >&2
    echo "Install or restore the matching LLVM 22.1.1 libraries before building AeMali/PanVK." >&2
    exit 69
  fi
  if [ -z "$libdir" ] || { [ ! -e "$libdir/libLLVM.so" ] && [ ! -e "$libdir/libLLVMCore.a" ]; }; then
    echo "PanVK/Mali lanes require libLLVM.so or LLVM static libraries under llvm-config --libdir=$libdir" >&2
    exit 69
  fi
  if [ "$ALLOW_SYSTEM_LLVM_FOR_CLC" != "1" ]; then
    require_path "$LLVM_ROOT/bin/opt"
    require_path "$LLVM_ROOT/lib/cmake/llvm/LLVMConfig.cmake"
    require_path "$LLVM_ROOT/lib/cmake/clang/ClangConfig.cmake"
    if [ ! -e "$libdir/libclang-cpp.so" ]; then
      echo "PanVK/Mali lanes require libclang-cpp.so under llvm-config --libdir=$libdir" >&2
      exit 69
    fi
  fi
  ensure_mesa_clc_support
}

build_libdrm_support() {
  local support_root support_src support_build libdrm_commit pc_file

  if [ "$ACTION" = "plan" ]; then
    return
  fi

  case "$LANE" in
    panvk-android|mali-gallium-android|virgl-gallium-android|mesa-x11-bionic) ;;
    *)
      return
      ;;
  esac

  support_root="$TOOLCHAIN_DIR/support"
  support_src="$TOOLCHAIN_DIR/support-src/libdrm"
  support_build="$TOOLCHAIN_DIR/support-build/libdrm"

  if [ ! -d "$LIBDRM_REPO/.git" ]; then
    mkdir -p "$(dirname "$LIBDRM_REPO")"
    git clone --filter=blob:none "$LIBDRM_URL" "$LIBDRM_REPO"
  fi
  git -C "$LIBDRM_REPO" fetch --tags origin
  libdrm_commit="$(git -C "$LIBDRM_REPO" rev-parse "$LIBDRM_REF^{commit}")"

  if [ ! -e "$support_src/.git" ]; then
    mkdir -p "$(dirname "$support_src")"
    git -C "$LIBDRM_REPO" worktree add --detach "$support_src" "$libdrm_commit"
  fi

  "$PYTHON_BIN" -m mesonbuild.mesonmain setup "$support_build" "$support_src" --wipe \
    "--cross-file" "$TOOLCHAIN_DIR/android-aarch64-llvm22.ini" \
    "--native-file" "$TOOLCHAIN_DIR/native-aarch64-llvm22.ini" \
    "-Dprefix=/usr" \
    "-Dbuildtype=release" \
    "-Dtests=false" \
    "-Dinstall-test-programs=false" \
    "-Dman-pages=disabled" \
    "-Dvalgrind=disabled" \
    "-Dcairo-tests=disabled" \
    "-Dintel=disabled" \
    "-Dradeon=disabled" \
    "-Damdgpu=disabled" \
    "-Dnouveau=disabled" \
    "-Dvmwgfx=disabled" \
    "-Domap=disabled" \
    "-Dexynos=disabled" \
    "-Dtegra=disabled" \
    "-Dvc4=disabled" \
    "-Detnaviv=disabled" \
    "-Dfreedreno=disabled" \
    "-Dfreedreno-kgsl=false"
  "$PYTHON_BIN" -m mesonbuild.mesonmain compile -C "$support_build" -j "$JOBS"
  DESTDIR="$support_root" "$PYTHON_BIN" -m mesonbuild.mesonmain install -C "$support_build" --no-rebuild

  pc_file="$support_root/usr/lib/pkgconfig/libdrm.pc"
  if [ -f "$pc_file" ]; then
    sed -i "s|^prefix=/usr|prefix=$support_root/usr|" "$pc_file"
  fi
  echo "libdrm_commit=$libdrm_commit"
}

verify_llvm_clc_surface
build_libdrm_support

if [ "$ACTION" != "plan" ] && [ ! -e "$SRC_DIR/.git" ]; then
  mkdir -p "$(dirname "$SRC_DIR")"
  git -C "$MESA_REPO" worktree add --detach "$SRC_DIR" "$COMMIT"
fi

apply_aemali_mesa_patchset() {
  local patch

  if [ "$ACTION" = "plan" ]; then
    return
  fi

  if [ "$ENABLE_AEMALI_MESA_PATCHSET" != "1" ]; then
    echo "AeMali Mesa patchset disabled by ENABLE_AEMALI_MESA_PATCHSET=$ENABLE_AEMALI_MESA_PATCHSET"
    return
  fi

  case "$LANE" in
    panvk-android|mali-gallium-android) ;;
    *)
      return
      ;;
  esac

  if [ ! -d "$AEMALI_MESA_PATCH_DIR" ]; then
    echo "AeMali Mesa patch directory is missing: $AEMALI_MESA_PATCH_DIR" >&2
    exit 69
  fi

  require_path "$PATCH_HUNK_VALIDATOR"
  "$PYTHON_BIN" "$PATCH_HUNK_VALIDATOR" "$AEMALI_MESA_PATCH_DIR"

  : > "$RUN_ROOT/aemali-mesa-patches.tsv"
  while IFS= read -r patch; do
    [ -n "$patch" ] || continue
    if git -C "$SRC_DIR" apply --check "$patch"; then
      git -C "$SRC_DIR" apply "$patch"
      printf 'applied\t' >> "$RUN_ROOT/aemali-mesa-patches.tsv"
    elif git -C "$SRC_DIR" apply -R --check "$patch"; then
      printf 'already-applied\t' >> "$RUN_ROOT/aemali-mesa-patches.tsv"
    else
      echo "AeMali Mesa patch does not apply cleanly: $patch" >&2
      exit 65
    fi
    sha256sum "$patch" >> "$RUN_ROOT/aemali-mesa-patches.tsv"
  done < <(find "$AEMALI_MESA_PATCH_DIR" -maxdepth 1 -type f -name '*.patch' | sort)
  cat "$RUN_ROOT/aemali-mesa-patches.tsv"
}

apply_aemali_mesa_patchset

mesa_cmd() {
  clc_env "$PYTHON_BIN" -m mesonbuild.mesonmain "$@"
}

host_tool_has_forbidden_auth_relr() {
  local bin="$1"

  "$LLVM_ROOT/bin/llvm-readelf" --dynamic-table "$bin" 2>/dev/null | \
    grep -Eq 'AARCH64_AUTH_RELR|AARCH64_AUTH_RELRSZ|AARCH64_AUTH_RELRENT'
}

validate_host_tool_elf_surface() {
  local target="$1"
  local bin="$2"

  require_path "$bin"
  if host_tool_has_forbidden_auth_relr "$bin"; then
    echo "Host tool emitted forbidden AUTH_RELR surface: $target -> $bin" >&2
    "$LLVM_ROOT/bin/llvm-readelf" --dynamic-table "$bin" | \
      grep 'AARCH64_AUTH\|ANDROID_RELR\|RUNPATH' >&2 || true
    return 1
  fi
}

repair_host_tool_elf_surface() {
  local host_build="$1"
  local target="$2"
  local bin="$3"

  if validate_host_tool_elf_surface "$target" "$bin"; then
    return 0
  fi

  echo "Re-linking $target serially to remove forbidden AUTH_RELR surface" >&2
  rm -f "$bin"
  mesa_cmd compile -C "$host_build" -j 1 "$target"
  validate_host_tool_elf_surface "$target" "$bin"
}

build_mesa_host_tool_support() {
  local support_root="$TOOLCHAIN_DIR/support"
  local host_build="$TOOLCHAIN_DIR/support-build/mesa-host-tools"

  if [ "$ACTION" = "plan" ]; then
    return
  fi

  case "$LANE" in
    panvk-android|mali-gallium-android) ;;
    *)
      return
      ;;
  esac

  mesa_cmd setup "$host_build" "$SRC_DIR" --wipe \
    "--native-file" "$TOOLCHAIN_DIR/native-aarch64-llvm22.ini" \
    "-Dprefix=/usr" \
    "-Dbuildtype=release" \
    "-Dstrip=true" \
    "-Dplatforms=" \
    "-Dgallium-drivers=" \
    "-Dvulkan-drivers=" \
    "-Dbuild-tests=false" \
    "-Dvalgrind=disabled" \
    "-Dlibunwind=disabled" \
    "-Dmicrosoft-clc=disabled" \
    "-Dintel-rt=disabled" \
    "-Dzstd=disabled" \
    "-Dgallium-va=disabled" \
    "-Dgallium-mediafoundation=disabled" \
    "-Dllvm=enabled" \
    "-Dmesa-clc=enabled" \
    "-Dinstall-mesa-clc=true" \
    "-Dprecomp-compiler=enabled" \
    "-Dinstall-precomp-compiler=true" \
    "-Dtools=panfrost" \
    "${mesa_rtti_options[@]}"
  mesa_cmd compile -C "$host_build" -j "$JOBS" \
    src/compiler/clc/mesa_clc \
    src/compiler/spirv/vtn_bindgen2 \
    src/panfrost/clc/panfrost_compile

  repair_host_tool_elf_surface "$host_build" \
    src/compiler/clc/mesa_clc \
    "$host_build/src/compiler/clc/mesa_clc"
  repair_host_tool_elf_surface "$host_build" \
    src/compiler/spirv/vtn_bindgen2 \
    "$host_build/src/compiler/spirv/vtn_bindgen2"
  repair_host_tool_elf_surface "$host_build" \
    src/panfrost/clc/panfrost_compile \
    "$host_build/src/panfrost/clc/panfrost_compile"

  mkdir -p "$support_root/usr/bin"
  install -m 0755 "$host_build/src/compiler/clc/mesa_clc" "$support_root/usr/bin/mesa_clc"
  install -m 0755 "$host_build/src/compiler/spirv/vtn_bindgen2" "$support_root/usr/bin/vtn_bindgen2"
  install -m 0755 "$host_build/src/panfrost/clc/panfrost_compile" "$support_root/usr/bin/panfrost_compile"

  validate_host_tool_elf_surface src/compiler/clc/mesa_clc "$support_root/usr/bin/mesa_clc"
  validate_host_tool_elf_surface src/compiler/spirv/vtn_bindgen2 "$support_root/usr/bin/vtn_bindgen2"
  validate_host_tool_elf_surface src/panfrost/clc/panfrost_compile "$support_root/usr/bin/panfrost_compile"
}

common_options=(
  "--cross-file" "$TOOLCHAIN_DIR/android-aarch64-llvm22.ini"
  "--native-file" "$TOOLCHAIN_DIR/native-aarch64-llvm22.ini"
  "--force-fallback-for=zlib"
  "-Dprefix=/usr"
  "-Dbuildtype=release"
  "-Dstrip=true"
  "-Dbuild-tests=false"
  "-Dvalgrind=disabled"
  "-Dlibunwind=disabled"
  "-Dmicrosoft-clc=disabled"
  "-Dintel-rt=disabled"
  "-Dzstd=disabled"
  "-Dgallium-va=disabled"
  "-Dgallium-mediafoundation=disabled"
)

llvm_disabled_options=(
  "-Dllvm=disabled"
)

llvm_clc_system_options=(
  "-Dllvm=enabled"
  "-Dmesa-clc=system"
  "-Dprecomp-compiler=system"
)

mesa_rtti_options=()

configure_mesa_rtti_surface() {
  local llvm_config="$LLVM_ROOT/bin/llvm-config"
  local llvm_rtti="UNKNOWN"

  case "$LANE" in
    panvk-android|mali-gallium-android) ;;
    *)
      return
      ;;
  esac

  llvm_rtti="$("$llvm_config" --has-rtti 2>/dev/null || true)"
  case "$llvm_rtti" in
    YES|ON|TRUE|true|1)
      ;;
    NO|OFF|FALSE|false|0)
      mesa_rtti_options+=("-Dcpp_rtti=false")
      ;;
    *)
      echo "Unable to classify LLVM RTTI surface from $llvm_config --has-rtti='$llvm_rtti'" >&2
      exit 69
      ;;
  esac
  echo "llvm_has_rtti=$llvm_rtti"
}

case "$LANE" in
  turnip-android)
    lane_llvm_options=("${llvm_disabled_options[@]}")
    lane_common_options=(
      "-Dplatforms=android"
      "-Dplatform-sdk-version=$ANDROID_API"
      "-Dandroid-stub=true"
      "-Dandroid-libbacktrace=disabled"
    )
    lane_options=(
      "-Degl=disabled"
      "-Dgles1=disabled"
      "-Dgles2=disabled"
      "-Dopengl=false"
      "-Dgbm=disabled"
      "-Dglx=disabled"
      "-Dxlib-lease=disabled"
      "-Dvulkan-drivers=freedreno"
      "-Dvulkan-layers="
      "-Dvulkan-beta=true"
      "-Dfreedreno-kmds=kgsl"
      "-Dgallium-drivers="
    )
    ;;
  panvk-android)
    lane_llvm_options=("${llvm_clc_system_options[@]}")
    lane_common_options=(
      "-Dplatforms=android"
      "-Dplatform-sdk-version=$ANDROID_API"
      "-Dandroid-stub=true"
      "-Dandroid-libbacktrace=disabled"
    )
    lane_options=(
      "-Degl=disabled"
      "-Dgles1=disabled"
      "-Dgles2=disabled"
      "-Dopengl=false"
      "-Dgbm=disabled"
      "-Dglx=disabled"
      "-Dxlib-lease=disabled"
      "-Dvulkan-drivers=panfrost"
      "-Dvulkan-layers="
      "-Dvulkan-beta=true"
      "-Dgallium-drivers="
    )
    ;;
  mali-gallium-android)
    lane_llvm_options=("${llvm_clc_system_options[@]}")
    lane_common_options=(
      "-Dplatforms=android"
      "-Dplatform-sdk-version=$ANDROID_API"
      "-Dandroid-stub=true"
      "-Dandroid-libbacktrace=disabled"
    )
    lane_options=(
      "-Degl=enabled"
      "-Dgles1=disabled"
      "-Dgles2=enabled"
      "-Dopengl=true"
      "-Dgbm=disabled"
      "-Dglx=disabled"
      "-Dglvnd=disabled"
      "-Dvulkan-drivers=panfrost"
      "-Dvulkan-layers="
      "-Dvulkan-beta=true"
      "-Dgallium-drivers=lima,panfrost,zink,softpipe"
      "-Dshared-glapi=enabled"
    )
    ;;
  zink-android)
    lane_llvm_options=("${llvm_disabled_options[@]}")
    lane_common_options=(
      "-Dplatforms=android"
      "-Dplatform-sdk-version=$ANDROID_API"
      "-Dandroid-stub=true"
      "-Dandroid-libbacktrace=disabled"
    )
    lane_options=(
      "-Degl=enabled"
      "-Dgles1=disabled"
      "-Dgles2=enabled"
      "-Dopengl=true"
      "-Dgbm=disabled"
      "-Dglx=disabled"
      "-Dglvnd=disabled"
      "-Dvulkan-drivers=freedreno"
      "-Dvulkan-layers="
      "-Dvulkan-beta=true"
      "-Dfreedreno-kmds=kgsl"
      "-Dgallium-drivers=zink"
      "-Dshared-glapi=enabled"
    )
    ;;
  virgl-gallium-android)
    lane_llvm_options=("${llvm_disabled_options[@]}")
    lane_common_options=(
      "-Dplatforms=android"
      "-Dplatform-sdk-version=$ANDROID_API"
      "-Dandroid-stub=true"
      "-Dandroid-libbacktrace=disabled"
    )
    lane_options=(
      "-Degl=enabled"
      "-Dgles1=disabled"
      "-Dgles2=enabled"
      "-Dopengl=true"
      "-Dgbm=disabled"
      "-Dglx=disabled"
      "-Dglvnd=disabled"
      "-Dvulkan-drivers="
      "-Dvulkan-layers="
      "-Dgallium-drivers=virgl,zink"
      "-Dshared-glapi=enabled"
    )
    ;;
  mesa-x11-bionic)
    lane_llvm_options=("${llvm_disabled_options[@]}")
    lane_common_options=(
      "-Dplatforms=x11"
      "-Dandroid-stub=false"
      "-Dandroid-libbacktrace=disabled"
    )
    lane_options=(
      "-Degl=disabled"
      "-Dgles1=disabled"
      "-Dgles2=disabled"
      "-Dopengl=true"
      "-Dgbm=disabled"
      "-Dglx=xlib"
      "-Dglvnd=disabled"
      "-Dxlib-lease=disabled"
      "-Dvulkan-drivers="
      "-Dvulkan-layers="
      "-Dgallium-drivers=virgl,zink,softpipe"
      "-Dshared-glapi=enabled"
    )
    ;;
esac

configure_mesa_rtti_surface
build_mesa_host_tool_support

write_metadata() {
  cat > "$RUN_ROOT/source.json" <<EOF
{
  "lane": "$LANE",
  "mesaRef": "$MESA_REF",
  "resolvedMesaRef": "$RESOLVED_MESA_REF",
  "mesaCommit": "$COMMIT",
  "mesaCommitDate": "$COMMIT_DATE",
  "mesaCommitTitle": "$COMMIT_TITLE",
  "toolchain": "$LLVM_ROOT",
  "compiler": "$LLVM_ROOT/bin/clang",
  "llvmVersion": "$LLVM_VERSION",
  "llvmProjectSource": "$LLVM_PROJECT_SRC",
  "spirvLLVMTranslatorRef": "$SPIRV_LLVM_TRANSLATOR_REF",
  "androidSysvshmLib": "$ANDROID_SYSVSHM_LIB",
  "libdrmRef": "$LIBDRM_REF",
  "androidApi": "$ANDROID_API",
  "hostTarget": "$HOST_TARGET",
  "runRoot": "$RUN_ROOT"
}
EOF
}

setup_build() {
  if [ -d "$BUILD_DIR" ]; then
    mesa_cmd setup "$BUILD_DIR" "$SRC_DIR" --wipe "${common_options[@]}" "${lane_llvm_options[@]}" "${mesa_rtti_options[@]}" "${lane_common_options[@]}" "${lane_options[@]}"
  else
    mesa_cmd setup "$BUILD_DIR" "$SRC_DIR" "${common_options[@]}" "${lane_llvm_options[@]}" "${mesa_rtti_options[@]}" "${lane_common_options[@]}" "${lane_options[@]}"
  fi
}

compile_build() {
  mesa_cmd compile -C "$BUILD_DIR" -j "$JOBS"
}

stage_has_needed() {
  local needed="$1"
  local elf

  while IFS= read -r -d '' elf; do
    if grep -q "NEEDED.*\\[$needed\\]" < <(readelf -d "$elf" 2>/dev/null); then
      return 0
    fi
  done < <(find "$DESTDIR/usr/lib" -type f -name '*.so*' -print0 2>/dev/null)

  return 1
}

collect_stage_needed() {
  local elf

  find "$DESTDIR/usr/lib" -type f -name '*.so*' -print0 2>/dev/null |
    while IFS= read -r -d '' elf; do
      readelf -d "$elf" 2>/dev/null |
        sed -n 's/.*Shared library: \[\(.*\)\].*/\1/p'
    done |
    sort -u
}

stage_provides_lib() {
  [ -e "$DESTDIR/usr/lib/$1" ]
}

is_android_system_lib() {
  case "$1" in
    libc.so|libdl.so|libm.so|liblog.so|libcutils.so|libhardware.so|libnativewindow.so|libsync.so|libandroid.so)
      return 0
      ;;
  esac
  return 1
}

copy_runtime_lib() {
  local needed="$1"
  local source=""
  local candidate=""

  if [ "$needed" = "libz.so.1" ] && [ -f "$DESTDIR/usr/lib/libz.so" ]; then
    source="$DESTDIR/usr/lib/libz.so"
  fi

  case "$needed" in
    libc.so.6|ld-linux*)
      echo "Forbidden runtime dependency in Mesa graphics payload: $needed" >&2
      exit 65
      ;;
    libandroid-sysvshm.so)
      if [ -z "$source" ]; then
        source="$ANDROID_SYSVSHM_LIB"
      fi
      ;;
    libc++_shared.so)
      if [ -z "$source" ]; then
        source="$HOST_LIBCXX_SHARED"
      fi
      ;;
    *)
      if [ -z "$source" ]; then
        for candidate in \
          "$TOOLCHAIN_DIR/support/usr/lib/$needed" \
          "$LLVM_ROOT/lib/$needed" \
          "$TERMUX_PREFIX/lib/$needed"
        do
          if [ -f "$candidate" ]; then
            source="$candidate"
            break
          fi
        done
        if [ -z "$source" ]; then
          source="$TERMUX_PREFIX/lib/$needed"
        fi
      fi
      ;;
  esac

  if [ ! -f "$source" ]; then
    echo "Missing runtime support library for Mesa graphics payload: $needed ($source)" >&2
    exit 66
  fi

  install -m 755 "$source" "$DESTDIR/usr/lib/$needed"
  patchelf --remove-rpath "$DESTDIR/usr/lib/$needed" 2>/dev/null || true
  printf '%s %s\n' "$needed" "$source" >> "$RUN_ROOT/runtime-support-libs.txt.tmp"
}

install_runtime_support_if_needed() {
  local copied dep

  : > "$RUN_ROOT/runtime-support-libs.txt.tmp"

  while :; do
    copied=0
    while IFS= read -r dep; do
      [ -n "$dep" ] || continue
      is_android_system_lib "$dep" && continue
      stage_provides_lib "$dep" && continue
      copy_runtime_lib "$dep"
      copied=1
    done < <(collect_stage_needed)
    [ "$copied" -eq 1 ] || break
  done

  find "$DESTDIR/usr/lib" -maxdepth 1 -type f -name '*.so*' -print0 |
    while IFS= read -r -d '' elf; do
      readelf -d "$elf" >/dev/null 2>&1 || continue
      if grep -Eq 'RPATH|RUNPATH' < <(readelf -d "$elf"); then
        patchelf --remove-rpath "$elf" 2>/dev/null || true
      fi
    done

  sort -u "$RUN_ROOT/runtime-support-libs.txt.tmp" > "$RUN_ROOT/runtime-support-libs.txt"
  rm -f "$RUN_ROOT/runtime-support-libs.txt.tmp"
}

install_build() {
  rm -rf "$DESTDIR"
  mkdir -p "$DESTDIR"
  DESTDIR="$DESTDIR" mesa_cmd install -C "$BUILD_DIR" --no-rebuild
  mkdir -p "$DESTDIR/usr/lib"
  install_runtime_support_if_needed
  find "$DESTDIR" -type f | sort > "$RUN_ROOT/stage-files.txt"
}

package_build() {
  local artifact
  if [ ! -d "$DESTDIR/usr" ]; then
    echo "Install stage is missing: $DESTDIR/usr" >&2
    exit 66
  fi
  artifact="$PACKAGE_DIR/$LANE-mesa-26.1-staging-$SHORT_COMMIT.tar.zst"
  tar -C "$DESTDIR" --zstd -cf "$artifact" .
  sha256sum "$artifact" > "$artifact.sha256"
  find "$DESTDIR" -type f -print0 | sort -z | xargs -0 sha256sum > "$PACKAGE_DIR/stage-sha256.tsv"
  echo "artifact=$artifact"
}

publish_app_asset() {
  local asset_out="$1"
  local asset_name

  if [ "$INSTALL_APP_ASSET" != "1" ]; then
    return
  fi
  require_path "$asset_out"
  mkdir -p "$APP_GRAPHICS_ASSET_DIR"
  asset_name="$(basename "$asset_out")"
  case "$asset_name" in
    aemali-panvk-*.tzst)
      find "$APP_GRAPHICS_ASSET_DIR" -maxdepth 1 -type f \
        \( -name 'aemali-panvk-*.tzst' -o -name 'aemali-panvk-*.tzst.sha256' \) \
        ! -name "$asset_name" ! -name "$asset_name.sha256" -delete
      ;;
    aemali-gallium-*.tzst)
      find "$APP_GRAPHICS_ASSET_DIR" -maxdepth 1 -type f \
        \( -name 'aemali-gallium-*.tzst' -o -name 'aemali-gallium-*.tzst.sha256' \) \
        ! -name "$asset_name" ! -name "$asset_name.sha256" -delete
      ;;
    zink-*.tzst)
      find "$APP_GRAPHICS_ASSET_DIR" -maxdepth 1 -type f \
        \( -name 'zink-*.tzst' -o -name 'zink-*.tzst.sha256' \) \
        ! -name "$asset_name" ! -name "$asset_name.sha256" -delete
      ;;
    virgl-*.tzst)
      find "$APP_GRAPHICS_ASSET_DIR" -maxdepth 1 -type f \
        \( -name 'virgl-*.tzst' -o -name 'virgl-*.tzst.sha256' \) \
        ! -name "$asset_name" ! -name "$asset_name.sha256" -delete
      ;;
    turnip-*.tzst)
      find "$APP_GRAPHICS_ASSET_DIR" -maxdepth 1 -type f \
        \( -name 'turnip-*.tzst' -o -name 'turnip-*.tzst.sha256' \) \
        ! -name "$asset_name" ! -name "$asset_name.sha256" -delete
      ;;
  esac
  install -m 0644 "$asset_out" "$APP_GRAPHICS_ASSET_DIR/$asset_name"
  sha256sum "$APP_GRAPHICS_ASSET_DIR/$asset_name" > "$APP_GRAPHICS_ASSET_DIR/$asset_name.sha256"
  echo "installed_app_graphics_asset=$APP_GRAPHICS_ASSET_DIR/$asset_name"
}

package_x11_asset() {
  local asset_name="$1"
  local asset_root="$RUN_ROOT/assets/$asset_name"
  local asset_out="$PACKAGE_DIR/$asset_name.tzst"
  local needed lib_path

  rm -rf "$asset_root"
  mkdir -p "$asset_root/usr/lib"
  install -m 0755 "$DESTDIR/usr/lib/libGL.so" "$asset_root/usr/lib/libGL.so"
  install -m 0755 "$DESTDIR/usr/lib/libGL.so" "$asset_root/usr/lib/libGL.so.1"
  if [ -f "$DESTDIR/usr/lib/libglapi.so.0" ]; then
    install -m 0755 "$DESTDIR/usr/lib/libglapi.so.0" "$asset_root/usr/lib/libglapi.so.0"
  fi
  if [ -f "$RUN_ROOT/runtime-support-libs.txt" ]; then
    while read -r needed _; do
      [ -n "$needed" ] || continue
      lib_path="$DESTDIR/usr/lib/$needed"
      [ -f "$lib_path" ] || continue
      install -m 0755 "$lib_path" "$asset_root/usr/lib/$needed"
    done < "$RUN_ROOT/runtime-support-libs.txt"
  fi
  case "$asset_name" in
    virgl-*)
      "$PYTHON_BIN" "$GRAPHICS_META_TOOL" virgl-mesa-bridge \
        --version "$MESA_ASSET_VERSION" \
        --source-repo "https://gitlab.freedesktop.org/mesa/mesa" \
        --source-ref "$RESOLVED_MESA_REF" \
        --source-commit "$COMMIT" \
        --source-commit-date "$COMMIT_DATE" \
        --artifact-name "$asset_name.tzst" \
        --library-name "usr/lib/libGL.so" \
        --root-library-path "usr/lib/libGL.so" \
        --output "$asset_root/meta.json"
      ;;
  esac
  tar -C "$asset_root" --zstd -cf "$asset_out" .
  sha256sum "$asset_out" > "$asset_out.sha256"
  publish_app_asset "$asset_out"
}

package_mali_gallium_asset() {
  local asset_name="$1"
  local asset_root="$RUN_ROOT/assets/$asset_name"
  local asset_out="$PACKAGE_DIR/$asset_name.tzst"
  local needed lib_path

  rm -rf "$asset_root"
  mkdir -p "$asset_root/usr/lib"
  for lib_name in libGL.so libEGL.so libGLESv2.so libGLESv1_CM.so libgallium_dri.so; do
    if [ -f "$DESTDIR/usr/lib/$lib_name" ]; then
      install -m 0755 "$DESTDIR/usr/lib/$lib_name" "$asset_root/usr/lib/$lib_name"
    fi
  done
  if [ -f "$DESTDIR/usr/lib/libGL.so" ]; then
    install -m 0755 "$DESTDIR/usr/lib/libGL.so" "$asset_root/usr/lib/libGL.so.1"
  fi
  if [ -f "$DESTDIR/usr/lib/libglapi.so.0" ]; then
    install -m 0755 "$DESTDIR/usr/lib/libglapi.so.0" "$asset_root/usr/lib/libglapi.so.0"
  fi
  if [ -f "$RUN_ROOT/runtime-support-libs.txt" ]; then
    while read -r needed _; do
      [ -n "$needed" ] || continue
      lib_path="$DESTDIR/usr/lib/$needed"
      [ -f "$lib_path" ] || continue
      install -m 0755 "$lib_path" "$asset_root/usr/lib/$needed"
    done < "$RUN_ROOT/runtime-support-libs.txt"
  fi
  "$PYTHON_BIN" "$GRAPHICS_META_TOOL" aemali-gallium \
    --version "$MESA_ASSET_VERSION" \
    --source-repo "https://gitlab.freedesktop.org/mesa/mesa" \
    --source-ref "$RESOLVED_MESA_REF" \
    --source-commit "$COMMIT" \
    --source-commit-date "$COMMIT_DATE" \
    --artifact-name "$asset_name.tzst" \
    --library-name "usr/lib/libEGL.so" \
    --root-library-path "usr/lib/libEGL.so" \
    --output "$asset_root/meta.json"
  tar -C "$asset_root" --zstd -cf "$asset_out" .
  sha256sum "$asset_out" > "$asset_out.sha256"
  publish_app_asset "$asset_out"
}

package_turnip_asset() {
  local asset_name="$1"
  local asset_root="$RUN_ROOT/assets/$asset_name"
  local asset_out="$PACKAGE_DIR/$asset_name.tzst"

  rm -rf "$asset_root"
  mkdir -p "$asset_root/usr/lib" "$asset_root/usr/share/vulkan/icd.d"
  install -m 0755 "$DESTDIR/usr/lib/libvulkan_freedreno.so" "$asset_root/usr/lib/libvulkan_freedreno.so"
  install -m 0644 "$DESTDIR/usr/share/vulkan/icd.d/freedreno_icd.armv8.json" "$asset_root/usr/share/vulkan/icd.d/freedreno_icd.aarch64.json"
  tar -C "$asset_root" --zstd -cf "$asset_out" .
  sha256sum "$asset_out" > "$asset_out.sha256"
  publish_app_asset "$asset_out"
}

package_panvk_asset() {
  local asset_name="$1"
  local asset_root="$RUN_ROOT/assets/$asset_name"
  local asset_out="$PACKAGE_DIR/$asset_name.tzst"
  local icd_src

  rm -rf "$asset_root"
  mkdir -p "$asset_root/usr/lib" "$asset_root/usr/share/vulkan/icd.d"
  install -m 0755 "$DESTDIR/usr/lib/libvulkan_panfrost.so" "$asset_root/usr/lib/libvulkan_panfrost.so"
  icd_src="$(find "$DESTDIR/usr/share/vulkan/icd.d" -maxdepth 1 -type f -name 'panfrost_icd.*.json' | sort | head -n 1)"
  if [ -z "$icd_src" ]; then
    echo "PanVK ICD manifest is missing under $DESTDIR/usr/share/vulkan/icd.d" >&2
    exit 66
  fi
  install -m 0644 "$icd_src" "$asset_root/usr/share/vulkan/icd.d/panfrost_icd.aarch64.json"
  "$PYTHON_BIN" "$GRAPHICS_META_TOOL" aemali-panvk \
    --version "$MESA_ASSET_VERSION" \
    --source-repo "https://gitlab.freedesktop.org/mesa/mesa" \
    --source-ref "$RESOLVED_MESA_REF" \
    --source-commit "$COMMIT" \
    --source-commit-date "$COMMIT_DATE" \
    --artifact-name "$asset_name.tzst" \
    --library-name "usr/lib/libvulkan_panfrost.so" \
    --root-library-path "usr/lib/libvulkan_panfrost.so" \
    --output "$asset_root/meta.json"
  tar -C "$asset_root" --zstd -cf "$asset_out" .
  sha256sum "$asset_out" > "$asset_out.sha256"
  publish_app_asset "$asset_out"
}

package_app_assets() {
  case "$LANE" in
    mesa-x11-bionic)
      package_x11_asset "zink-$MESA_ASSET_VERSION"
      package_x11_asset "virgl-$MESA_ASSET_VERSION"
      ;;
    turnip-android)
      package_turnip_asset "turnip-$MESA_ASSET_VERSION"
      ;;
    panvk-android)
      package_panvk_asset "aemali-panvk-$MESA_ASSET_VERSION"
      ;;
    mali-gallium-android)
      package_mali_gallium_asset "aemali-gallium-$MESA_ASSET_VERSION"
      ;;
  esac
}

write_metadata

case "$ACTION" in
  plan)
    printf '%s\n' "${common_options[@]}" "${lane_llvm_options[@]}" "${lane_common_options[@]}" "${lane_options[@]}" > "$RUN_ROOT/meson-options.txt"
    echo "Plan written to $RUN_ROOT"
    ;;
  setup)
    setup_build
    ;;
  compile)
    compile_build
    ;;
  install)
    install_build
    ;;
  package)
    package_build
    package_app_assets
    ;;
  all)
    setup_build
    compile_build
    install_build
    package_build
    package_app_assets
    ;;
esac
