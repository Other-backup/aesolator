#!/data/data/com.termux/files/usr/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
CATALOG_PATH="$REPO_ROOT/app/src/main/assets/prefixpack/catalog.tsv"
OUTPUT_DIR="${PREFIX_PACK_OUT:-$REPO_ROOT/out/prefix-pack-cache}"
RECON_DIR="${PREFIX_PACK_RECON_OUT:-$REPO_ROOT/out/prefix-pack-recon}"
RECON_TOOL="$REPO_ROOT/tools/prefix-pack/source-intake-recon.sh"

need_cmd() {
  command -v "$1" >/dev/null 2>&1
}

normalize_sha256() {
  value=$(printf '%s' "${1:-}" | tr '[:upper:]' '[:lower:]' | tr -d '[:space:]')
  if printf '%s' "$value" | grep -Eq '^[0-9a-f]{64}$'; then
    printf '%s\n' "$value"
  else
    printf '\n'
  fi
}

sha256_file() {
  file="$1"
  if need_cmd sha256sum; then
    sha256sum "$file" | awk '{print $1}'
    return 0
  fi
  if need_cmd shasum; then
    shasum -a 256 "$file" | awk '{print $1}'
    return 0
  fi
  if need_cmd openssl; then
    openssl dgst -sha256 "$file" | awk '{print $NF}'
    return 0
  fi
  return 1
}

resolve_cache_state() {
  target="$1"
  checksum="$2"
  if [ ! -f "$target" ]; then
    printf 'missing\n'
    return 0
  fi
  if [ ! -s "$target" ]; then
    printf 'empty\n'
    return 0
  fi
  expected=$(normalize_sha256 "$checksum")
  if [ -z "$expected" ]; then
    printf 'present\n'
    return 0
  fi
  actual=$(normalize_sha256 "$(sha256_file "$target" 2>/dev/null || true)")
  if [ -n "$actual" ] && [ "$actual" = "$expected" ]; then
    printf 'present\n'
  else
    printf 'sha256_mismatch\n'
  fi
}

download_file() {
  url="$1"
  output="$2"

  if need_cmd curl; then
    if curl --http1.1 -fL --retry 3 --connect-timeout 20 -o "$output" "$url"; then
      return 0
    fi
  fi

  if need_cmd wget; then
    if wget -O "$output" "$url"; then
      return 0
    fi
  fi

  printf 'fetch-cache.sh: all download methods failed for %s\n' "$url" >&2
  return 1
}

matches_requested() {
  entry_id="$1"
  shift || true
  if [ "$#" -eq 0 ]; then
    return 0
  fi
  for wanted in "$@"; do
    if [ "$wanted" = "$entry_id" ]; then
      return 0
    fi
  done
  return 1
}

usage() {
  cat <<EOF
Usage:
  sh tools/prefix-pack/fetch-cache.sh list
  sh tools/prefix-pack/fetch-cache.sh status
  sh tools/prefix-pack/fetch-cache.sh show [catalog_id...]
  sh tools/prefix-pack/fetch-cache.sh links [catalog_id...]
  sh tools/prefix-pack/fetch-cache.sh recon [catalog_id...]
  sh tools/prefix-pack/fetch-cache.sh fetch [catalog_id...]
  sh tools/prefix-pack/fetch-cache.sh [catalog_id...]

Optional:
  sh tools/prefix-pack/fetch-cache.sh --output DIR <command_or_ids...>
EOF
}

print_list() {
  while IFS="$(printf '\t')" read -r id file_name mode download_url install_group source_label source_page_url install_cmd summary sha256; do
    [ -n "${id:-}" ] || continue
    case "$id" in
      \#*) continue ;;
    esac
    printf '%s\t%s\t%s\t%s\t%s\t%s\n' \
      "$id" "$mode" "$install_group" "$file_name" "$install_cmd" "$source_page_url"
  done < "$CATALOG_PATH"
}

print_links() {
  shown_any=0
  while IFS="$(printf '\t')" read -r id file_name mode download_url install_group source_label source_page_url install_cmd summary sha256; do
    [ -n "${id:-}" ] || continue
    case "$id" in
      \#*) continue ;;
    esac
    if ! matches_requested "$id" "$@"; then
      continue
    fi
    shown_any=1
    printf '%s\n' "id=$id"
    printf '  file=%s\n' "$file_name"
    printf '  mode=%s\n' "$mode"
    printf '  install_group=%s\n' "$install_group"
    printf '  install_cmd=%s\n' "$install_cmd"
    printf '  source_label=%s\n' "$source_label"
    printf '  source_page=%s\n' "$source_page_url"
    printf '  download_url=%s\n' "$download_url"
    printf '  cache_path=%s\n' "$OUTPUT_DIR/$file_name"
    printf '  summary=%s\n' "$summary"
    if [ -n "$(normalize_sha256 "$sha256")" ]; then
      printf '  sha256=%s\n' "$(normalize_sha256 "$sha256")"
    fi
  done < "$CATALOG_PATH"

  if [ "$shown_any" -eq 0 ] && [ "$#" -gt 0 ]; then
    printf 'fetch-cache.sh: no catalog entries matched the requested ids\n' >&2
    exit 1
  fi
}

print_status() {
  mkdir -p "$OUTPUT_DIR"
  printf 'cache_dir=%s\n' "$OUTPUT_DIR"
  printf '\n'
  while IFS="$(printf '\t')" read -r id file_name mode download_url install_group source_label source_page_url install_cmd summary sha256; do
    [ -n "${id:-}" ] || continue
    case "$id" in
      \#*) continue ;;
    esac
    state=$(resolve_cache_state "$OUTPUT_DIR/$file_name" "$sha256")
    printf '%s\t%s\t%s\t%s\t%s\t%s\n' \
      "$id" "$state" "$file_name" "$install_cmd" "$download_url" "$source_page_url"
  done < "$CATALOG_PATH"
}

run_recon() {
  mkdir -p "$RECON_DIR"
  need_cmd python3 || {
    printf 'fetch-cache.sh: python3 is required for source-intake recon\n' >&2
    exit 1
  }
  [ -f "$RECON_TOOL" ] || {
    printf 'fetch-cache.sh: recon tool missing: %s\n' "$RECON_TOOL" >&2
    exit 1
  }

  selected_count=0
  while IFS="$(printf '\t')" read -r id file_name mode download_url install_group source_label source_page_url install_cmd summary sha256; do
    [ -n "${id:-}" ] || continue
    case "$id" in
      \#*) continue ;;
    esac
    if ! matches_requested "$id" "$@"; then
      continue
    fi
    selected_count=$((selected_count + 1))
    target_url="$source_page_url"
    if [ -z "$target_url" ]; then
      target_url="$download_url"
    fi
    if [ -z "$target_url" ]; then
      printf 'skip recon %s (no source/download URL)\n' "$id"
      continue
    fi
    out_dir="$RECON_DIR/$id"
    printf 'recon %s -> %s\n' "$id" "$out_dir"
    printf '  source page: %s\n' "$target_url"
    sh "$RECON_TOOL" \
      --id "$id" \
      --url "$target_url" \
      --source-label "$source_label" \
      --download-url "$download_url" \
      --output-dir "$out_dir"
  done < "$CATALOG_PATH"

  if [ "$selected_count" -eq 0 ] && [ "$#" -gt 0 ]; then
    printf 'fetch-cache.sh: no catalog entries matched the requested ids\n' >&2
    exit 1
  fi
  printf 'prefix-pack recon ready at %s\n' "$RECON_DIR"
}

fetch_entries() {
  mkdir -p "$OUTPUT_DIR"
  selected_count=0

  while IFS="$(printf '\t')" read -r id file_name mode download_url install_group source_label source_page_url install_cmd summary sha256; do
    [ -n "${id:-}" ] || continue
    case "$id" in
      \#*) continue ;;
    esac

    if ! matches_requested "$id" "$@"; then
      continue
    fi

    selected_count=$((selected_count + 1))
    target="$OUTPUT_DIR/$file_name"

    if [ "$mode" != "download" ]; then
      printf 'skip %s (%s)\n' "$id" "$mode"
      printf '  source page: %s\n' "$source_page_url"
      continue
    fi

    if [ -f "$target" ] && [ "$(resolve_cache_state "$target" "$sha256")" = "present" ]; then
      printf 'keep %s\n' "$target"
      continue
    fi
    if [ -f "$target" ]; then
      printf 'refresh %s (checksum mismatch or stale cache)\n' "$target"
      rm -f "$target"
    fi

    printf 'download %s -> %s\n' "$id" "$target"
    printf '  source page: %s\n' "$source_page_url"
    download_file "$download_url" "$target"
    if [ "$(resolve_cache_state "$target" "$sha256")" != "present" ]; then
      printf 'fetch-cache.sh: checksum verification failed for %s\n' "$target" >&2
      rm -f "$target"
      exit 1
    fi
  done < "$CATALOG_PATH"

  if [ "$selected_count" -eq 0 ] && [ "$#" -gt 0 ]; then
    printf 'fetch-cache.sh: no catalog entries matched the requested ids\n' >&2
    exit 1
  fi

  printf 'prefix-pack cache ready at %s\n' "$OUTPUT_DIR"
}

if [ "${1:-}" = "--output" ]; then
  if [ "$#" -lt 2 ]; then
    usage >&2
    exit 1
  fi
  OUTPUT_DIR="$2"
  shift 2
fi

command_name="${1:-fetch}"

case "$command_name" in
  list)
    print_list
    ;;
  status)
    print_status
    ;;
  show|links)
    shift
    print_links "$@"
    ;;
  recon|inspect)
    shift
    run_recon "$@"
    ;;
  fetch)
    shift
    fetch_entries "$@"
    ;;
  help|-h|--help)
    usage
    ;;
  *)
    fetch_entries "$@"
    ;;
esac
