#!/data/data/com.termux/files/usr/bin/sh
set -eu

LLVM_VERSION="${LLVM_VERSION:-22.1.1}"
ROOT="/data/data/com.termux/files/home"
ARCHIVE_DIR="$ROOT/.toolchains-src"
BUILD_DIR="$ROOT/.toolchains-build/llvm-$LLVM_VERSION"
PREFIX_DIR="$ROOT/.toolchains/llvm-$LLVM_VERSION-termux"
SRC_ARCHIVE="$ARCHIVE_DIR/llvm-project-$LLVM_VERSION.tar.gz"
SRC_DIR="$ARCHIVE_DIR/llvm-project-$LLVM_VERSION"
SOURCE_URL="https://github.com/llvm/llvm-project/archive/refs/tags/llvmorg-$LLVM_VERSION.tar.gz"

detect_cpu_count() {
  if [ -r /sys/devices/system/cpu/possible ]; then
    possible="$(cat /sys/devices/system/cpu/possible)"
    case "$possible" in
      *-*)
        last="${possible##*-}"
        expr "$last" + 1 2>/dev/null && return 0
        ;;
      [0-9]*)
        expr "$possible" + 1 2>/dev/null && return 0
        ;;
    esac
  fi
  if command -v nproc >/dev/null 2>&1; then
    nproc
    return 0
  fi
  echo 4
}

CPU_COUNT="$(detect_cpu_count)"
DEFAULT_JOBS=4
if [ "$CPU_COUNT" -ge 8 ]; then
  DEFAULT_JOBS=6
elif [ "$CPU_COUNT" -ge 6 ]; then
  DEFAULT_JOBS=4
elif [ "$CPU_COUNT" -ge 4 ]; then
  DEFAULT_JOBS=3
else
  DEFAULT_JOBS=2
fi

JOBS="${JOBS:-$DEFAULT_JOBS}"

mkdir -p "$ARCHIVE_DIR" "$ROOT/.toolchains-build" "$ROOT/.toolchains"

if [ ! -f "$SRC_ARCHIVE" ]; then
  curl -L --fail "$SOURCE_URL" -o "$SRC_ARCHIVE"
fi

if [ ! -d "$SRC_DIR" ]; then
  tar -C "$ARCHIVE_DIR" -xf "$SRC_ARCHIVE"
  mv "$ARCHIVE_DIR/llvm-project-llvmorg-$LLVM_VERSION" "$SRC_DIR"
fi

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"
mkdir -p "$PREFIX_DIR"

cmake -S "$SRC_DIR/llvm" -B "$BUILD_DIR" -G Ninja \
  -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_INSTALL_PREFIX="$PREFIX_DIR" \
  -DLLVM_ENABLE_PROJECTS="clang;lld" \
  -DLLVM_TARGETS_TO_BUILD="AArch64;ARM;X86" \
  -DLLVM_ENABLE_BINDINGS=OFF \
  -DLLVM_ENABLE_TERMINFO=OFF \
  -DLLVM_ENABLE_LIBXML2=OFF \
  -DLLVM_INCLUDE_BENCHMARKS=OFF \
  -DLLVM_INCLUDE_EXAMPLES=OFF \
  -DLLVM_INCLUDE_TESTS=OFF \
  -DCLANG_INCLUDE_TESTS=OFF \
  -DLLVM_BUILD_TOOLS=ON \
  -DLLVM_BUILD_UTILS=ON \
  -DLLVM_USE_LINKER=lld \
  -DLLVM_PARALLEL_LINK_JOBS=1 \
  -DLLVM_DISTRIBUTION_COMPONENTS="clang;clang-resource-headers;lld;llvm-ar;llvm-as;llvm-config;llvm-cxxfilt;llvm-dlltool;llvm-lib;llvm-link;llvm-nm;llvm-objcopy;llvm-objdump;llvm-rc;llvm-ranlib;llvm-readelf;llvm-readobj;llvm-strip;llvm-windres" \
  -DCMAKE_C_COMPILER=clang \
  -DCMAKE_CXX_COMPILER=clang++

cmake --build "$BUILD_DIR" --target install-distribution -- -j"$JOBS"

ln -sf clang "$PREFIX_DIR/bin/cc"
ln -sf clang++ "$PREFIX_DIR/bin/c++"
ln -sf llvm-ar "$PREFIX_DIR/bin/ar"
ln -sf llvm-ranlib "$PREFIX_DIR/bin/ranlib"
ln -sf llvm-strip "$PREFIX_DIR/bin/strip"
ln -sf llvm-objcopy "$PREFIX_DIR/bin/objcopy"
ln -sf llvm-objdump "$PREFIX_DIR/bin/objdump"
ln -sf llvm-nm "$PREFIX_DIR/bin/nm"
ln -sf llvm-readelf "$PREFIX_DIR/bin/readelf"
ln -sf ld.lld "$PREFIX_DIR/bin/ld"

"$PREFIX_DIR/bin/clang" --version | sed -n '1,4p'
"$PREFIX_DIR/bin/llvm-strip" --version | sed -n '1,4p'
