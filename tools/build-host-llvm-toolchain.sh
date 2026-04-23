#!/data/data/com.termux/files/usr/bin/sh
set -eu

ROOT="${ROOT:-/data/data/com.termux/files/home}"
LLVM_VERSION="${LLVM_VERSION:-22.1.1}"
WCP_HOST_LLVM_BUILDER="${WCP_HOST_LLVM_BUILDER:-$ROOT/wcp-runtime-lanes/ci/toolchains/build-host-llvm-android.sh}"

if [ ! -f "$WCP_HOST_LLVM_BUILDER" ]; then
  echo "Missing canonical host LLVM builder: $WCP_HOST_LLVM_BUILDER" >&2
  echo "Host LLVM 22.1.1 is owned by wcp-runtime-lanes, not Ae.solator." >&2
  exit 69
fi

exec /data/data/com.termux/files/usr/bin/bash "$WCP_HOST_LLVM_BUILDER"
