#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
CATALOG_PATH="$ROOT_DIR/catalog.tsv"
CACHE_DIR="$ROOT_DIR/cache"
SAVE_ROOT="$ROOT_DIR/save_data"
STATE_ROOT="$SAVE_ROOT/state"

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
    curl --http1.1 -fL --retry 3 --connect-timeout 20 --max-time 0 -o "$output" "$url"
    return 0
  fi

  if need_cmd wget; then
    wget -O "$output" "$url"
    return 0
  fi

  printf 'prefixpack-loader: neither curl nor wget is available\n' >&2
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
  sh /opt/ae/prefix-pack/bin/prefixpack-loader.sh list
  sh /opt/ae/prefix-pack/bin/prefixpack-loader.sh status
  sh /opt/ae/prefix-pack/bin/prefixpack-loader.sh show [entry_id...]
  sh /opt/ae/prefix-pack/bin/prefixpack-loader.sh links [entry_id...]
  sh /opt/ae/prefix-pack/bin/prefixpack-loader.sh fetch [entry_id...]

Default command:
  fetch
EOF
}

print_list() {
  while IFS="$(printf '\t')" read -r id file_name mode download_url install_group source_label source_page_url install_cmd summary sha256; do
    [ -n "${id:-}" ] || continue
    case "$id" in
      \#*) continue ;;
    esac
    printf '%s\t%s\t%s\t%s\t%s\t%s\n' \
      "$id" "$mode" "$install_group" "$file_name" "$install_cmd" "$source_label"
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
    printf '  download_url=%s\n' "$download_url"
    printf '  source_page=%s\n' "$source_page_url"
    printf '  source_label=%s\n' "$source_label"
    printf '  cache_path=%s\n' "$CACHE_DIR/$file_name"
    printf '  summary=%s\n' "$summary"
    if [ -n "$(normalize_sha256 "$sha256")" ]; then
      printf '  sha256=%s\n' "$(normalize_sha256 "$sha256")"
    fi
  done < "$CATALOG_PATH"

  if [ "$shown_any" -eq 0 ] && [ "$#" -gt 0 ]; then
    printf 'prefixpack-loader: no catalog entries matched the requested ids\n' >&2
    return 1
  fi
}

print_status() {
  mkdir -p "$CACHE_DIR" "$SAVE_ROOT"
  printf 'root_dir=%s\n' "$ROOT_DIR"
  printf 'cache_dir=%s\n' "$CACHE_DIR"
  printf 'save_root=%s\n' "$SAVE_ROOT"
  printf 'state_root=%s\n' "$STATE_ROOT"
  printf '\n'

  while IFS="$(printf '\t')" read -r id file_name mode download_url install_group source_label source_page_url install_cmd summary sha256; do
    [ -n "${id:-}" ] || continue
    case "$id" in
      \#*) continue ;;
    esac
    state=$(resolve_cache_state "$CACHE_DIR/$file_name" "$sha256")
    printf '%s\t%s\t%s\t%s\t%s\t%s\n' \
      "$id" "$state" "$file_name" "$install_cmd" "$download_url" "$source_page_url"
  done < "$CATALOG_PATH"
}

fetch_entries() {
  mkdir -p "$CACHE_DIR" "$SAVE_ROOT"
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
    target="$CACHE_DIR/$file_name"

    if [ "$mode" != "download" ]; then
      printf 'prefixpack-loader: skip %s (%s)\n' "$id" "$mode"
      printf '  source page: %s\n' "$source_page_url"
      continue
    fi

    if [ -f "$target" ] && [ "$(resolve_cache_state "$target" "$sha256")" = "present" ]; then
      printf 'prefixpack-loader: keep %s\n' "$target"
      continue
    fi
    if [ -f "$target" ]; then
      printf 'prefixpack-loader: refresh %s (checksum mismatch or stale cache)\n' "$target"
      rm -f "$target"
    fi

    printf 'prefixpack-loader: download %s -> %s\n' "$id" "$target"
    printf '  source page: %s\n' "$source_page_url"
    download_file "$download_url" "$target"
    if [ "$(resolve_cache_state "$target" "$sha256")" != "present" ]; then
      printf 'prefixpack-loader: checksum verification failed for %s\n' "$target" >&2
      rm -f "$target"
      return 1
    fi
  done < "$CATALOG_PATH"

  if [ "$selected_count" -eq 0 ] && [ "$#" -gt 0 ]; then
    printf 'prefixpack-loader: no catalog entries matched the requested ids\n' >&2
    return 1
  fi
}

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
