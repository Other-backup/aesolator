#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
PATCH_DIR="${1:-${ROOT_DIR}/ci/winlator/patches}"

log() { printf '[ae-x11-contract] %s\n' "$*"; }
fail() { printf '[ae-x11-contract][error] %s\n' "$*" >&2; exit 1; }

[[ -d "${PATCH_DIR}" ]] || fail "Patch directory not found: ${PATCH_DIR}"

require_marker() {
  local family="$1"
  local marker="$2"
  local hit

  if command -v rg >/dev/null 2>&1; then
    hit="$(rg -n --fixed-strings --glob '*.patch' -- "${marker}" "${PATCH_DIR}" | sed -n '1p' || true)"
  else
    hit="$(grep -RFn --include='*.patch' -- "${marker}" "${PATCH_DIR}" | sed -n '1p' || true)"
  fi
  [[ -n "${hit}" ]] || fail "Missing ${family} marker: ${marker}"
  log "ok: ${family} marker '${marker}' (${hit})"
}

require_any_marker() {
  local family="$1"
  shift
  local marker
  local hit=""

  for marker in "$@"; do
    if command -v rg >/dev/null 2>&1; then
      hit="$(rg -n --fixed-strings --glob '*.patch' -- "${marker}" "${PATCH_DIR}" | sed -n '1p' || true)"
    else
      hit="$(grep -RFn --include='*.patch' -- "${marker}" "${PATCH_DIR}" | sed -n '1p' || true)"
    fi
    if [[ -n "${hit}" ]]; then
      log "ok: ${family} marker '${marker}' (${hit})"
      return 0
    fi
  done

  fail "Missing ${family} marker set: $*"
}

log "Checking Ae emerald identity markers"
require_any_marker "ae-emerald" \
  "<color name=\"colorAccent\">#0f9d73</color>" \
  "<color name=\"colorAccent\">#12b886</color>"
require_any_marker "ae-emerald" \
  "<color name=\"colorAccentDark\">#5de2b0</color>" \
  "<color name=\"colorAccentDark\">#6ff0bf</color>"
require_marker "ae-emerald" "<color name=\"button_positive_dark\">#10b981</color>"
require_marker "ae-emerald" "<color name=\"material_deep_teal_200\">#ff5de2b0</color>"
require_marker "ae-emerald" "<color name=\"material_deep_teal_500\">#ff0f9d73</color>"
require_any_marker "ae-emerald" \
  "<color name=\"preference_fallback_accent_color\">#ff0f9d73</color>" \
  "<color name=\"preference_fallback_accent_color\">#ff12b886</color>"

log "Checking X11-first runtime markers"
x11_markers=(
  "AERO_X11_GRAPHICS_STACK"
  "\"x11_graphics_stack\""
)
for marker in "${x11_markers[@]}"; do
  require_marker "x11-first" "${marker}"
done

log "Ae/X11 contract passed for patch stack: ${PATCH_DIR}"
