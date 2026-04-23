#!/usr/bin/env sh
set -eu

JOBS="${JOBS:-8}"
ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
ROOT_HOME="$(CDPATH= cd -- "$ROOT_DIR/.." && pwd)"
AESOLATOR_ROOT="${AESOLATOR_ROOT:-$ROOT_DIR}"
LLVM_ROOT="${LLVM_ROOT:-$ROOT_HOME/.toolchains/llvm-22.1.1-termux}"
ANDROID_API="${ANDROID_API:-34}"
NDK_ROOT="${NDK_ROOT:-$ROOT_HOME/android-sdk/ndk/29.0.14206865}"
NDK_PREBUILT_ROOT="${NDK_PREBUILT_ROOT:-}"
NDK_HOST_TAG="${NDK_HOST_TAG:-}"
NDK_SYSROOT="${NDK_SYSROOT:-}"
NDK_TARGET_LIB_DIR="${NDK_TARGET_LIB_DIR:-}"
CLANG_RESOURCE_DIR="${CLANG_RESOURCE_DIR:-}"
TERMUX_PREFIX="${TERMUX_PREFIX:-/data/data/com.termux/files/usr}"
GRAPHICS_META_TOOL="${GRAPHICS_META_TOOL:-$AESOLATOR_ROOT/tools/generate_graphics_driver_meta.py}"
APP_GRAPHICS_ASSET_DIR="${APP_GRAPHICS_ASSET_DIR:-$AESOLATOR_ROOT/app/src/main/assets/graphics_driver}"
INSTALL_APP_ASSET="${INSTALL_APP_ASSET:-0}"
WORK_ROOT="${WORK_ROOT:-$ROOT_DIR/out/vortek-wrapper-client}"
SRC_DIR="${VORTEK_SRC:-$HOME/.cache/research/vortek}"
SOURCE_REPO="${VORTEK_REPO:-https://github.com/brunodev85/vortek}"
SOURCE_REF="${VORTEK_REF:-ab7329c}"
PACKAGE_NAME="${VORTEK_PACKAGE_NAME:-com.winlator.cmod}"
IMAGEFS_ROOT="/data/data/$PACKAGE_NAME/files/imagefs"
SERVER_PATH="$IMAGEFS_ROOT/tmp/.vortek/V0"
BUILD_DIR="$WORK_ROOT/build"
WORK_SRC="$WORK_ROOT/src"
OUT_LIB="${OUT_LIB:-$WORK_ROOT/libvulkan_vortek.so}"
X11_COMPAT_INCLUDE_DIR="$WORK_ROOT/x11-compat/include"

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

find_vulkan_headers() {
  if [ -n "${VULKAN_HEADERS_DIR:-}" ] && [ -f "$VULKAN_HEADERS_DIR/vulkan/vulkan.h" ]; then
    printf '%s\n' "$VULKAN_HEADERS_DIR"
    return 0
  fi

  if [ -f "$NDK_SYSROOT/usr/include/vulkan/vulkan.h" ]; then
    printf '%s\n' "$NDK_SYSROOT/usr/include"
    return 0
  fi

  for dir in \
    "$HOME/.cache/research/vulkan-headers-v1.4.349/include" \
    "$ROOT_DIR/out/graphics-source-builds"/*/src/mesa/include \
    "$HOME/.cache/donors/mesa-main/include" \
    "${ANDROID_HOME:-$HOME/android-sdk}/ndk"/*/toolchains/llvm/prebuilt/*/sysroot/usr/include \
    "$HOME/android-sdk/ndk"/*/toolchains/llvm/prebuilt/*/sysroot/usr/include
  do
    if [ -f "$dir/vulkan/vulkan.h" ]; then
      printf '%s\n' "$dir"
      return 0
    fi
  done

  echo "Vulkan headers not found; set VULKAN_HEADERS_DIR" >&2
  return 1
}

require_path "$LLVM_ROOT/bin/clang"
require_path "$LLVM_ROOT/bin/clang++"
require_path "$LLVM_ROOT/bin/llvm-ar"
require_path "$LLVM_ROOT/bin/llvm-nm"
require_path "$LLVM_ROOT/bin/llvm-ranlib"
require_path "$LLVM_ROOT/bin/llvm-readelf"
require_path "$LLVM_ROOT/bin/ld.lld"
NDK_PREBUILT_ROOT="$(resolve_ndk_prebuilt_root)"
[ -n "$NDK_HOST_TAG" ] || NDK_HOST_TAG="$(basename "$NDK_PREBUILT_ROOT")"
[ -n "$NDK_SYSROOT" ] || NDK_SYSROOT="$NDK_PREBUILT_ROOT/sysroot"
[ -n "$NDK_TARGET_LIB_DIR" ] || NDK_TARGET_LIB_DIR="$NDK_SYSROOT/usr/lib/aarch64-linux-android/$ANDROID_API"
CLANG_RESOURCE_DIR="$(resolve_clang_resource_dir)"
require_path "$NDK_SYSROOT/usr/include/stdio.h"
require_path "$NDK_TARGET_LIB_DIR/liblog.so"
require_path "$CLANG_RESOURCE_DIR/include/stddef.h"
require_path "$GRAPHICS_META_TOOL"

ANDROID_C_FLAGS="--target=aarch64-unknown-linux-android$ANDROID_API --sysroot=$NDK_SYSROOT -resource-dir $CLANG_RESOURCE_DIR -B$TERMUX_PREFIX/bin"
ANDROID_LINK_FLAGS="$ANDROID_C_FLAGS --ld-path=$LLVM_ROOT/bin/ld.lld -Wl,-dynamic-linker,/system/bin/linker64"

if [ ! -d "$SRC_DIR/.git" ]; then
  mkdir -p "$(dirname -- "$SRC_DIR")"
  git clone "$SOURCE_REPO" "$SRC_DIR"
fi

git -C "$SRC_DIR" fetch --depth=1 origin "$SOURCE_REF" >/dev/null 2>&1 || true
git -C "$SRC_DIR" checkout -q "$SOURCE_REF"

rm -rf "$WORK_ROOT"
mkdir -p "$WORK_ROOT" "$X11_COMPAT_INCLUDE_DIR/X11"
cp -a "$SRC_DIR" "$WORK_SRC"
cat > "$X11_COMPAT_INCLUDE_DIR/X11/Xlib.h" <<'EOF'
#ifndef AERO_VORTEK_X11_XLIB_COMPAT_H
#define AERO_VORTEK_X11_XLIB_COMPAT_H

typedef struct _XDisplay Display;
typedef unsigned long XID;
typedef XID Window;
typedef unsigned long VisualID;

#endif
EOF

perl -0pi -e 's#/data/data/com\.winlator/files/rootfs/tmp/\.vortek/V0#'"$SERVER_PATH"'#g' \
  "$WORK_SRC/include/vortek.h" \
  "$WORK_SRC/vortek_icd.aarch64.json"
perl -0pi -e 's/#ifdef __ANDROID__\r?\n#define VT_SERVER 1\r?\n#define VK_NO_PROTOTYPES 1\r?\n#endif/#if defined(__ANDROID__) \&\& !defined(VT_CLIENT)\n#define VT_SERVER 1\n#define VK_NO_PROTOTYPES 1\n#endif/s' \
  "$WORK_SRC/include/vortek.h"
perl -0pi -e 's#PFN_vkVoidFunction vk_icdGetInstanceProcAddr\(VkInstance instance, const char\* pName\) \{\n    if \(!vortekInitOnce\(\)\) return NULL;\n    return findVkDispatchFuncWithName\(pName\);\n\}\n\nVkResult vk_icdNegotiateLoaderICDInterfaceVersion\(uint32_t\* pSupportedVersion\) \{\n    \*pSupportedVersion = 3;\n    return VK_SUCCESS;\n\}#\#define VT_LOADER_ICD_INTERFACE_VERSION 5\n\nVkResult vk_icdNegotiateLoaderICDInterfaceVersion(uint32_t* pSupportedVersion) {\n    if (!pSupportedVersion) return VK_INCOMPLETE;\n    if (*pSupportedVersion > VT_LOADER_ICD_INTERFACE_VERSION) {\n        *pSupportedVersion = VT_LOADER_ICD_INTERFACE_VERSION;\n    }\n    return VK_SUCCESS;\n}\n\nPFN_vkVoidFunction vk_icdGetPhysicalDeviceProcAddr(VkInstance instance, const char* pName) {\n    (void)instance;\n    if (!vortekInitOnce()) return NULL;\n    return findVkDispatchFuncWithName(pName);\n}\n\nPFN_vkVoidFunction vk_icdGetInstanceProcAddr(VkInstance instance, const char* pName) {\n    (void)instance;\n    if (strcmp(pName, "vk_icdNegotiateLoaderICDInterfaceVersion") == 0) {\n        return (PFN_vkVoidFunction)vk_icdNegotiateLoaderICDInterfaceVersion;\n    }\n    if (strcmp(pName, "vk_icdGetPhysicalDeviceProcAddr") == 0) {\n        return (PFN_vkVoidFunction)vk_icdGetPhysicalDeviceProcAddr;\n    }\n    if (!vortekInitOnce()) return NULL;\n    return findVkDispatchFuncWithName(pName);\n}#s' \
  "$WORK_SRC/src/vulkan_calls.c"
printf '\ntarget_link_libraries(vulkan_vortek log)\n' >> "$WORK_SRC/CMakeLists.txt"

VULKAN_HEADERS="$(find_vulkan_headers)"
echo "llvm_root=$LLVM_ROOT"
echo "clang=$LLVM_ROOT/bin/clang"
echo "android_api=$ANDROID_API"
echo "ndk_sysroot=$NDK_SYSROOT"
echo "ndk_target_lib_dir=$NDK_TARGET_LIB_DIR"
echo "clang_resource_dir=$CLANG_RESOURCE_DIR"
echo "vulkan_headers=$VULKAN_HEADERS"
cmake -S "$WORK_SRC" -B "$BUILD_DIR" --fresh \
  -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_C_COMPILER="$LLVM_ROOT/bin/clang" \
  -DCMAKE_CXX_COMPILER="$LLVM_ROOT/bin/clang++" \
  -DCMAKE_AR="$LLVM_ROOT/bin/llvm-ar" \
  -DCMAKE_RANLIB="$LLVM_ROOT/bin/llvm-ranlib" \
  -DCMAKE_C_FLAGS="$ANDROID_C_FLAGS -DVT_CLIENT=1 -I$X11_COMPAT_INCLUDE_DIR -I$VULKAN_HEADERS -Wno-unknown-warning-option -Wno-incompatible-pointer-types-discards-qualifiers" \
  -DCMAKE_C_FLAGS_RELEASE="-O2" \
  -DCMAKE_SHARED_LINKER_FLAGS="$ANDROID_LINK_FLAGS -L$NDK_TARGET_LIB_DIR"
cmake --build "$BUILD_DIR" -j "$JOBS"

install -m 0755 "$BUILD_DIR/libvulkan_vortek.so" "$OUT_LIB"
patchelf --remove-rpath "$OUT_LIB"

if "$LLVM_ROOT/bin/llvm-readelf" -d "$OUT_LIB" | grep -q 'Shared library: \[liblog.so\]'; then
  :
else
  "$LLVM_ROOT/bin/llvm-readelf" -d "$OUT_LIB" >&2
  echo "Vortek wrapper is missing DT_NEEDED liblog.so" >&2
  exit 66
fi

if "$LLVM_ROOT/bin/llvm-readelf" -d "$OUT_LIB" | grep -Eq 'libc\.so\.6|ld-linux|RUNPATH|RPATH'; then
  "$LLVM_ROOT/bin/llvm-readelf" -d "$OUT_LIB" >&2
  echo "Forbidden Vortek wrapper dependency surface" >&2
  exit 65
fi

if "$LLVM_ROOT/bin/llvm-nm" -D "$OUT_LIB" | grep -q ' T vk_icdGetPhysicalDeviceProcAddr$'; then
  :
else
  "$LLVM_ROOT/bin/llvm-nm" -D "$OUT_LIB" >&2
  echo "Vortek wrapper is missing Loader/Driver interface v4 physical-device proc hook" >&2
  exit 67
fi

publish_app_asset() {
  local asset_path="$1"
  local asset_name family

  [ "$INSTALL_APP_ASSET" = "1" ] || return 0
  asset_name="$(basename "$asset_path")"
  mkdir -p "$APP_GRAPHICS_ASSET_DIR"
  family="${asset_name%%-*}"
  if [ "$family" = "vortek" ]; then
    find "$APP_GRAPHICS_ASSET_DIR" -maxdepth 1 -type f \
      \( -name 'vortek-*.tzst' -o -name 'vortek-*.tzst.sha256' \) \
      ! -name "$asset_name" ! -name "$asset_name.sha256" -delete
  fi
  install -m 0644 "$asset_path" "$APP_GRAPHICS_ASSET_DIR/$asset_name"
  sha256sum "$APP_GRAPHICS_ASSET_DIR/$asset_name" > "$APP_GRAPHICS_ASSET_DIR/$asset_name.sha256"
}

SOURCE_COMMIT="$(git -C "$WORK_SRC" rev-parse HEAD)"
SOURCE_SHORT_COMMIT="$(printf '%s' "$SOURCE_COMMIT" | cut -c1-8)"
SOURCE_COMMIT_DATE="$(git -C "$WORK_SRC" show -s --format=%cI HEAD)"
SOURCE_COMMIT_DAY="$(git -C "$WORK_SRC" show -s --format=%cs HEAD | tr -d '-')"
VORTEK_VERSION="${VORTEK_VERSION:-${SOURCE_COMMIT_DAY}-${SOURCE_SHORT_COMMIT}}"
PACKAGE_DIR="${PACKAGE_DIR:-$WORK_ROOT/package}"
ASSET_NAME="vortek-${VORTEK_VERSION}.tzst"
ASSET_ROOT="$PACKAGE_DIR/$ASSET_NAME.root"
ASSET_PATH="$PACKAGE_DIR/$ASSET_NAME"

rm -rf "$ASSET_ROOT"
mkdir -p "$ASSET_ROOT/usr/lib" "$ASSET_ROOT/usr/share/vulkan/icd.d" "$ASSET_ROOT/usr/share/aesolator/graphics_driver" "$PACKAGE_DIR"
install -m 0755 "$OUT_LIB" "$ASSET_ROOT/usr/lib/libvulkan_vortek.so"
install -m 0644 "$WORK_SRC/vortek_icd.aarch64.json" "$ASSET_ROOT/usr/share/vulkan/icd.d/vortek_icd.aarch64.json"
: > "$ASSET_ROOT/usr/share/aesolator/graphics_driver/$ASSET_NAME.installed"
python3 "$GRAPHICS_META_TOOL" vortek-wrapper \
  --version "$VORTEK_VERSION" \
  --source-repo "$SOURCE_REPO" \
  --source-ref "$SOURCE_REF" \
  --source-commit "$SOURCE_COMMIT" \
  --source-commit-date "$SOURCE_COMMIT_DATE" \
  --artifact-name "$ASSET_NAME" \
  --library-name "usr/lib/libvulkan_vortek.so" \
  --root-library-path "usr/lib/libvulkan_vortek.so" \
  --output "$ASSET_ROOT/meta.json"
tar -C "$ASSET_ROOT" --zstd -cf "$ASSET_PATH" .
sha256sum "$ASSET_PATH" > "$ASSET_PATH.sha256"
publish_app_asset "$ASSET_PATH"

printf '%s\n' "$OUT_LIB"
