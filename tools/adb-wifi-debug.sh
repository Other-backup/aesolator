#!/data/data/com.termux/files/usr/bin/sh
set -eu

ROOT="/data/data/com.termux/files/home"
REPO_ROOT="${AEO_REPO_ROOT:-$ROOT/aesolator}"
DEFAULT_APK="${AEO_DEBUG_APK:-$REPO_ROOT/app/build/outputs/apk/debug/app-debug.apk}"
ADB_BIN="${ADB_BIN:-/data/data/com.termux/files/usr/bin/adb}"

log() {
  printf '[adb-wifi-debug] %s\n' "$*"
}

usage() {
  cat <<EOF
Usage:
  sh tools/adb-wifi-debug.sh status
  sh tools/adb-wifi-debug.sh pair <pair-host:port> <pair-code> [connect-host:port]
  sh tools/adb-wifi-debug.sh connect <host:port>
  sh tools/adb-wifi-debug.sh disconnect [host:port]
  sh tools/adb-wifi-debug.sh install-debug <serial> [apk-path]
  sh tools/adb-wifi-debug.sh launch <serial>

Examples:
  sh tools/adb-wifi-debug.sh pair 192.168.0.10:37099 123456 192.168.0.10:42363
  sh tools/adb-wifi-debug.sh connect 192.168.0.10:42363
  sh tools/adb-wifi-debug.sh install-debug 192.168.0.10:42363
  sh tools/adb-wifi-debug.sh launch 192.168.0.10:42363
EOF
}

need_adb() {
  if [ -x "$ADB_BIN" ]; then
    return 0
  fi

  if command -v adb >/dev/null 2>&1; then
    ADB_BIN="$(command -v adb)"
    return 0
  fi

  log "adb not found"
  exit 1
}

adb_cmd() {
  "$ADB_BIN" "$@"
}

ensure_server() {
  adb_cmd start-server >/dev/null 2>&1 || true
}

show_devices() {
  adb_cmd devices -l
}

pair_device() {
  pair_endpoint="$1"
  pair_code="$2"
  connect_endpoint="${3:-}"

  ensure_server
  adb_cmd pair "$pair_endpoint" "$pair_code"
  if [ -n "$connect_endpoint" ]; then
    adb_cmd connect "$connect_endpoint"
  fi
  show_devices
}

connect_device() {
  endpoint="$1"
  ensure_server
  adb_cmd connect "$endpoint"
  show_devices
}

disconnect_device() {
  endpoint="${1:-}"
  ensure_server
  if [ -n "$endpoint" ]; then
    adb_cmd disconnect "$endpoint"
  else
    adb_cmd disconnect
  fi
  show_devices
}

install_debug() {
  serial="$1"
  apk_path="${2:-$DEFAULT_APK}"

  if [ ! -f "$apk_path" ]; then
    log "APK not found: $apk_path"
    exit 1
  fi

  ensure_server
  adb_cmd -s "$serial" install -r -d "$apk_path"
}

launch_app() {
  serial="$1"
  ensure_server
  adb_cmd -s "$serial" shell monkey -p com.winlator.cmod -c android.intent.category.LAUNCHER 1
}

need_adb

command_name="${1:-status}"

case "$command_name" in
  status)
    ensure_server
    show_devices
    ;;
  pair)
    [ "$#" -ge 3 ] || {
      usage
      exit 1
    }
    pair_device "$2" "$3" "${4:-}"
    ;;
  connect)
    [ "$#" -eq 2 ] || {
      usage
      exit 1
    }
    connect_device "$2"
    ;;
  disconnect)
    disconnect_device "${2:-}"
    ;;
  install-debug)
    [ "$#" -ge 2 ] || {
      usage
      exit 1
    }
    install_debug "$2" "${3:-$DEFAULT_APK}"
    ;;
  launch)
    [ "$#" -eq 2 ] || {
      usage
      exit 1
    }
    launch_app "$2"
    ;;
  *)
    usage
    exit 1
    ;;
esac
