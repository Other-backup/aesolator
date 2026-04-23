#!/usr/bin/env bash
set -euo pipefail

SRC_DIR="${1:-}"

log()  { printf '[native-contract] %s\n' "$*"; }
fail() { printf '[native-contract][error] %s\n' "$*" >&2; exit 1; }

[[ -n "${SRC_DIR}" ]] || fail "usage: $0 <aesolator-src-dir>"
[[ -d "${SRC_DIR}" ]] || fail "Source directory not found: ${SRC_DIR}"

has_pattern() {
  local file="$1"
  local pattern="$2"
  [[ -f "${file}" ]] || fail "Missing file: ${file}"
  if ! grep -Fq -- "${pattern}" "${file}"; then
    fail "Missing folded feature marker '${pattern}' in ${file}"
  fi
}

log "Checking folded core runtime markers"
has_pattern "${SRC_DIR}/app/src/main/java/com/winlator/cmod/MainActivity.java" "case R.id.main_menu_diagnostics:"
has_pattern "${SRC_DIR}/app/src/main/java/com/winlator/cmod/MainActivity.java" "new ForensicCenterFragment()"
has_pattern "${SRC_DIR}/app/src/main/res/menu/main_menu.xml" "main_menu_diagnostics"
has_pattern "${SRC_DIR}/app/src/main/res/values/strings.xml" "<string name=\"diagnostics\">Forensic Center</string>"

log "Checking folded contents and provider markers"
has_pattern "${SRC_DIR}/app/src/main/java/com/winlator/cmod/contents/ContentProfile.java" "CONTENT_TYPE_TURNIP_DRIVER"
has_pattern "${SRC_DIR}/app/src/main/java/com/winlator/cmod/contents/ContentProfile.java" "CONTENT_TYPE_OPENGL_DRIVER"
has_pattern "${SRC_DIR}/app/src/main/java/com/winlator/cmod/contents/ContentProfile.java" "CONTENT_TYPE_DGVOODOO"
has_pattern "${SRC_DIR}/app/src/main/java/com/winlator/cmod/ContentsFragment.java" "ContentsManager.REMOTE_PROFILES_AE"
has_pattern "${SRC_DIR}/app/src/main/java/com/winlator/cmod/ContentsFragment.java" "mergeJsonArrays"

log "Checking folded wrapper/runtime markers"
has_pattern "${SRC_DIR}/app/src/main/java/com/winlator/cmod/contentdialog/DXVKConfigDialog.java" "DXVK + VKD3D"
has_pattern "${SRC_DIR}/app/src/main/java/com/winlator/cmod/contentdialog/DXVKConfigDialog.java" "config.put(\"ddrawrapper\", \"none\")"
has_pattern "${SRC_DIR}/app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java" "dxwrapper = dxvkWrapper + \";\" + vkd3dWrapper;"
has_pattern "${SRC_DIR}/app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java" "Legacy DDraw wrapper payloads are superseded in DXVK lane."
has_pattern "${SRC_DIR}/app/src/main/java/com/winlator/cmod/contents/ContentsManager.java" "REMOTE_PROFILES_AE"

log "Folded native-source contract is satisfied"
