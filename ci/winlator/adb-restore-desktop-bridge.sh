#!/usr/bin/env bash
set -euo pipefail

: "${ADB_SERIAL:=127.0.0.1:5555}"
: "${WLT_PACKAGE:=com.winlator.cmod}"
: "${SOURCE_PREFIX:=files/forensic-backups/prefix-refresh-20260330-212626/xuser-2-.wine}"
: "${TARGET_PREFIX:=}"

ADB_BIN="${ADB_BIN:-adb}"
BRIDGE_FILES=(wfm.exe winhandler.exe)

log() { printf '[adb-restore-desktop-bridge] %s\n' "$*"; }
fail() { printf '[adb-restore-desktop-bridge][error] %s\n' "$*" >&2; exit 1; }

run_as() {
  "${ADB_BIN}" shell run-as "${WLT_PACKAGE}" sh -c "cd \"\$HOME\" && $1"
}

resolve_target_prefix() {
  if [[ -n "${TARGET_PREFIX}" ]]; then
    printf '%s\n' "${TARGET_PREFIX}"
    return
  fi

  local target_rel
  target_rel="$(run_as 'target="$(readlink files/imagefs/home/xuser 2>/dev/null || true)"; if [ -n "$target" ]; then printf "%s/.wine" "${target#./}"; else printf "files/imagefs/home/xuser/.wine"; fi')"
  [[ -n "${target_rel}" ]] || fail "Unable to resolve target prefix"
  printf '%s\n' "${target_rel}"
}

main() {
  local target_prefix target_windows source_windows verify_cmd
  target_prefix="$(resolve_target_prefix)"
  source_windows="${SOURCE_PREFIX}/drive_c/windows"
  target_windows="${target_prefix}/drive_c/windows"

  log "serial=${ADB_SERIAL}"
  log "package=${WLT_PACKAGE}"
  log "source_prefix=${SOURCE_PREFIX}"
  log "target_prefix=${target_prefix}"

  run_as "[ -d '${SOURCE_PREFIX}' ]" || fail "Source prefix is missing: ${SOURCE_PREFIX}"
  run_as "mkdir -p '${target_windows}'"

  for filename in "${BRIDGE_FILES[@]}"; do
    run_as "[ -f '${source_windows}/${filename}' ]" || fail "Missing bridge file in source prefix: ${filename}"
    run_as "install -m 0775 '${source_windows}/${filename}' '${target_windows}/${filename}'"
  done

  verify_cmd="ls -l"
  for filename in "${BRIDGE_FILES[@]}"; do
    verify_cmd+=" '${target_windows}/${filename}'"
  done
  run_as "${verify_cmd}"

  verify_cmd="sha256sum"
  for filename in "${BRIDGE_FILES[@]}"; do
    verify_cmd+=" '${target_windows}/${filename}'"
  done
  run_as "${verify_cmd}"

  log "desktop bridge restore complete"
}

main "$@"
