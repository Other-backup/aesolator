#!/data/data/com.termux/files/usr/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
ASSET_ROOT="$REPO_ROOT/app/src/main/assets/prefixpack"
INPUT_DIR="${PREFIX_PACK_CACHE_IN:-$REPO_ROOT/out/prefix-pack-cache}"
OUTPUT_ARCHIVE="${PREFIX_PACK_OVERLAY_OUT:-$REPO_ROOT/out/prefix-pack-offline-overlay.tzst}"
STAGE_DIR=$(mktemp -d "$REPO_ROOT/.tmp-prefix-pack.XXXXXX")

cleanup() {
  rm -rf "$STAGE_DIR"
}

trap cleanup EXIT INT TERM

if [ "${1:-}" = "--input" ]; then
  if [ "$#" -lt 2 ]; then
    printf 'Usage: sh tools/prefix-pack/build-offline-overlay.sh [--input CACHE_DIR] [--output ARCHIVE]\n' >&2
    exit 1
  fi
  INPUT_DIR="$2"
  shift 2
fi

if [ "${1:-}" = "--output" ]; then
  if [ "$#" -lt 2 ]; then
    printf 'Usage: sh tools/prefix-pack/build-offline-overlay.sh [--input CACHE_DIR] [--output ARCHIVE]\n' >&2
    exit 1
  fi
  OUTPUT_ARCHIVE="$2"
  shift 2
fi

if [ ! -d "$INPUT_DIR" ]; then
  printf 'build-offline-overlay.sh: cache dir not found: %s\n' "$INPUT_DIR" >&2
  exit 1
fi

mkdir -p "$STAGE_DIR/opt/ae"
cp -R "$ASSET_ROOT" "$STAGE_DIR/opt/ae/prefix-pack"
mkdir -p "$STAGE_DIR/opt/ae/prefix-pack/cache"
mkdir -p "$(dirname -- "$OUTPUT_ARCHIVE")"

find "$INPUT_DIR" -maxdepth 1 -type f -exec sh -eu -c '
  destination="$1"
  shift
  for source_file do
    cp "$source_file" "$destination/"
  done
' sh "$STAGE_DIR/opt/ae/prefix-pack/cache" {} +

tar --zstd -cf "$OUTPUT_ARCHIVE" -C "$STAGE_DIR" opt
printf 'offline overlay ready: %s\n' "$OUTPUT_ARCHIVE"
