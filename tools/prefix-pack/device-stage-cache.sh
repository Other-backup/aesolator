#!/data/data/com.termux/files/usr/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
CATALOG_PATH="$REPO_ROOT/app/src/main/assets/prefixpack/catalog.tsv"
CACHE_DIR="${PREFIX_PACK_CACHE_IN:-$REPO_ROOT/out/prefix-pack-cache}"
PACKAGE_NAME="${PREFIX_PACK_PACKAGE_NAME:-com.winlator.cmod}"
APP_DATA_DIR="/data/user/0/$PACKAGE_NAME"
REMOTE_CACHE_DIR="$APP_DATA_DIR/files/imagefs/opt/ae/prefix-pack/cache"
REMOTE_TOOLKIT_DIR="$APP_DATA_DIR/files/imagefs/opt/ae/prefix-pack"
REMOTE_MEDIA_STAGE_DIR="/data/local/tmp/$PACKAGE_NAME-prefix-pack-stage"
ADB_BIN="${ADB_BIN:-}"

log() {
  printf '[prefix-pack-device] %s\n' "$*"
}

usage() {
  cat <<EOF
Usage:
  sh tools/prefix-pack/device-stage-cache.sh status <serial>
  sh tools/prefix-pack/device-stage-cache.sh stage <serial> [catalog_id...]

Examples:
  sh tools/prefix-pack/device-stage-cache.sh status 10.0.0.1:40741
  sh tools/prefix-pack/device-stage-cache.sh stage 10.0.0.1:40741
  sh tools/prefix-pack/device-stage-cache.sh stage 10.0.0.1:40741 vcpp_aio wine_mono_11_0_0
EOF
}

need_adb() {
  if [ -n "$ADB_BIN" ] && [ -x "$ADB_BIN" ]; then
    return 0
  fi

  if command -v adb >/dev/null 2>&1; then
    ADB_BIN=$(command -v adb)
    return 0
  fi

  log "adb not found"
  exit 1
}

adb_cmd() {
  "$ADB_BIN" "$@" </dev/null
}

run_remote_sh() {
  serial="$1"
  remote_script="$2"
  adb_cmd -s "$serial" exec-out run-as "$PACKAGE_NAME" sh -c "$remote_script"
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

ensure_run_as() {
  serial="$1"
  adb_cmd -s "$serial" shell run-as "$PACKAGE_NAME" true >/dev/null
}

print_remote_status() {
  serial="$1"
  ensure_run_as "$serial"
  run_remote_sh "$serial" "set -eu; printf 'toolkit_version='; if [ -f '$REMOTE_TOOLKIT_DIR/VERSION' ]; then tr -d '\r' < '$REMOTE_TOOLKIT_DIR/VERSION'; else printf 'missing\n'; fi; printf 'remote_cache_dir=%s\n' '$REMOTE_CACHE_DIR'; mkdir -p '$REMOTE_CACHE_DIR'; for f in '$REMOTE_CACHE_DIR'/*; do [ -f \"\$f\" ] || continue; bytes=\$(wc -c < \"\$f\" | tr -d '[:space:]'); printf '%s\t%s\n' \"\$bytes\" \"\${f##*/}\"; done | sort -k2 || true"
}

stage_selected_files() {
  serial="$1"
  shift

  if [ ! -d "$CACHE_DIR" ]; then
    log "cache dir not found: $CACHE_DIR"
    exit 1
  fi

  ensure_run_as "$serial"
  run_remote_sh "$serial" "mkdir -p '$REMOTE_CACHE_DIR'"

  selected_any=0

  if [ "$#" -eq 0 ]; then
    for local_path in "$CACHE_DIR"/*; do
      if [ ! -f "$local_path" ]; then
        continue
      fi
      selected_any=1
      stage_one_file "$serial" "$local_path"
    done
  else
    matched_any=0
    while IFS="$(printf '\t')" read -r id file_name mode download_url install_group source_label source_page_url install_cmd summary; do
      [ -n "${id:-}" ] || continue
      case "$id" in
        \#*) continue ;;
      esac
      if ! matches_requested "$id" "$@"; then
        continue
      fi
      matched_any=1
      local_path="$CACHE_DIR/$file_name"
      if [ ! -f "$local_path" ]; then
        log "missing local cache payload for $id: $local_path"
        exit 1
      fi
      selected_any=1
      stage_one_file "$serial" "$local_path"
    done < "$CATALOG_PATH"

    if [ "$matched_any" -eq 0 ]; then
      log "no catalog entries matched the requested ids"
      exit 1
    fi
  fi

  if [ "$selected_any" -eq 0 ]; then
    log "no local cache payloads were selected"
    exit 1
  fi

  print_remote_status "$serial"
}

stage_one_file() {
  serial="$1"
  local_path="$2"
  file_name=$(basename -- "$local_path")
  remote_path="$REMOTE_CACHE_DIR/$file_name"
  temp_path="$REMOTE_MEDIA_STAGE_DIR/$file_name"
  local_size=$(wc -c < "$local_path" | tr -d '[:space:]')

  if [ "${local_size:-0}" -le 0 ]; then
    log "invalid local cache payload (empty): $local_path"
    exit 1
  fi

  log "stage $file_name ($local_size bytes)"
  adb_cmd -s "$serial" shell "mkdir -p '$REMOTE_MEDIA_STAGE_DIR'" >/dev/null
  adb_cmd -s "$serial" push "$local_path" "$temp_path" >/dev/null
  run_remote_sh "$serial" "mkdir -p '$REMOTE_CACHE_DIR'; cp '$temp_path' '$remote_path'; chmod 660 '$remote_path'"
  adb_cmd -s "$serial" shell "rm -f '$temp_path'" >/dev/null

  remote_size=$(
    run_remote_sh "$serial" "wc -c < '$remote_path'" \
      | tr -d '\r[:space:]'
  )

  if [ "$remote_size" != "$local_size" ]; then
    log "size mismatch for $file_name: local=$local_size remote=$remote_size"
    exit 1
  fi
}

need_adb

command_name="${1:-}"
serial="${2:-}"

case "$command_name" in
  status)
    [ -n "$serial" ] || {
      usage
      exit 1
    }
    print_remote_status "$serial"
    ;;
  stage)
    [ -n "$serial" ] || {
      usage
      exit 1
    }
    shift 2
    stage_selected_files "$serial" "$@"
    ;;
  *)
    usage
    exit 1
    ;;
esac
