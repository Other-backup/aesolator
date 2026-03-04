#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
APP_REPO="${AEO_APP_RELEASE_REPO:-kosoymiki/aeolator}"
WCP_REPO="${AEO_WCP_RELEASE_REPO:-kosoymiki/wcp-runtime-lanes}"
APPLY=0
WINLATOR_TAG="winlator-latest"
WCP_TAG="freewine11-arm64ec-latest"
WINLATOR_NOTES="${ROOT_DIR}/out/release-notes/winlator-v0.9c.md"
WCP_NOTES="${ROOT_DIR}/out/release-notes/wcp-stable.md"
STAGE_DIR="${ROOT_DIR}/out/release-staging"
WINLATOR_ASSETS=()
WCP_ASSETS=()

log() { printf '[release-publish] %s\n' "$*"; }
fail() { printf '[release-publish][error] %s\n' "$*" >&2; exit 1; }
usage() {
  cat <<USAGE
Usage: bash ci/release/publish-0.9c.sh [--apply] [--app-repo owner/repo] [--wcp-repo owner/repo] [--winlator-tag TAG] [--wcp-tag TAG]

Publishes or updates split releases:
- Ae.solator APK -> app repo
- FreeWine 11 WCP -> runtime repo
(dry-run by default)
USAGE
}
while [[ $# -gt 0 ]]; do
  case "$1" in
    --apply) APPLY=1 ;;
    --app-repo) APP_REPO="$2"; shift ;;
    --wcp-repo) WCP_REPO="$2"; shift ;;
    --winlator-tag) WINLATOR_TAG="$2"; shift ;;
    --wcp-tag) WCP_TAG="$2"; shift ;;
    -h|--help) usage; exit 0 ;;
    *) fail "Unknown argument: $1" ;;
  esac
  shift
done

command -v gh >/dev/null 2>&1 || fail "gh CLI is required"
[[ -f "${WINLATOR_NOTES}" ]] || fail "Missing notes file: ${WINLATOR_NOTES} (run ci/release/prepare-0.9c-notes.sh)"
[[ -f "${WCP_NOTES}" ]] || fail "Missing notes file: ${WCP_NOTES} (run ci/release/prepare-0.9c-notes.sh)"

for f in "${ROOT_DIR}/out/winlator"/*.apk "${ROOT_DIR}/out/winlator/SHA256SUMS"; do
  [[ -e "$f" ]] || fail "Missing Winlator asset: $f"
done
for f in \
  "${ROOT_DIR}/out/freewine11/freewine11-arm64ec.wcp"; do
  [[ -e "$f" ]] || fail "Missing WCP asset: $f"
done
if [[ -f "${ROOT_DIR}/out/freewine11/SHA256SUMS-freewine11-arm64ec.txt" ]]; then
  WCP_SHA_FILE="${ROOT_DIR}/out/freewine11/SHA256SUMS-freewine11-arm64ec.txt"
elif [[ -f "${ROOT_DIR}/out/freewine11/SHA256SUMS" ]]; then
  WCP_SHA_FILE="${ROOT_DIR}/out/freewine11/SHA256SUMS"
else
  fail "Missing WCP SHA256 asset under out/freewine11"
fi

mkdir -p "${STAGE_DIR}/wcp-stable" "${STAGE_DIR}/winlator-latest"
for apk in "${ROOT_DIR}/out/winlator"/*.apk; do
  apk_base="$(basename -- "${apk}")"
  apk_suffix="${apk_base#by.aero.so.benchmark-}"
  [[ "${apk_suffix}" != "${apk_base}" ]] || apk_suffix="${apk_base}"
  cp -f "${apk}" "${STAGE_DIR}/winlator-latest/Ae.solator-${apk_suffix}"
done
cp -f "${ROOT_DIR}/out/winlator/SHA256SUMS" "${STAGE_DIR}/winlator-latest/SHA256SUMS.txt"
WINLATOR_ASSETS=("${STAGE_DIR}/winlator-latest"/*.apk "${STAGE_DIR}/winlator-latest/SHA256SUMS.txt")

cp -f "${ROOT_DIR}/out/freewine11/freewine11-arm64ec.wcp" "${STAGE_DIR}/wcp-stable/"
cp -f "${WCP_SHA_FILE}" "${STAGE_DIR}/wcp-stable/SHA256SUMS-freewine11-arm64ec.txt"
WCP_ASSETS=(
  "${STAGE_DIR}/wcp-stable/freewine11-arm64ec.wcp"
  "${STAGE_DIR}/wcp-stable/SHA256SUMS-freewine11-arm64ec.txt"
)

log "App repo: ${APP_REPO}"
log "Runtime repo: ${WCP_REPO}"
log "App tag: ${WINLATOR_TAG}"
log "WCP tag: ${WCP_TAG}"
log "Winlator assets: ${#WINLATOR_ASSETS[@]}"
log "WCP assets: ${#WCP_ASSETS[@]}"

if [[ "${APPLY}" != "1" ]]; then
  log "Dry-run only. Re-run with --apply to publish releases."
  exit 0
fi

if gh release view "${WCP_TAG}" --repo "${WCP_REPO}" >/dev/null 2>&1; then
  gh release edit "${WCP_TAG}" --repo "${WCP_REPO}" --title "FreeWine 11 ARM64EC" --notes-file "${WCP_NOTES}"
else
  gh release create "${WCP_TAG}" --repo "${WCP_REPO}" --title "FreeWine 11 ARM64EC" --notes-file "${WCP_NOTES}"
fi
gh release upload "${WCP_TAG}" --repo "${WCP_REPO}" --clobber "${WCP_ASSETS[@]}"

if gh release view "${WINLATOR_TAG}" --repo "${APP_REPO}" >/dev/null 2>&1; then
  gh release edit "${WINLATOR_TAG}" --repo "${APP_REPO}" --title "Ae.solator Latest" --notes-file "${WINLATOR_NOTES}"
else
  gh release create "${WINLATOR_TAG}" --repo "${APP_REPO}" --title "Ae.solator Latest" --notes-file "${WINLATOR_NOTES}"
fi
gh release upload "${WINLATOR_TAG}" --repo "${APP_REPO}" --clobber "${WINLATOR_ASSETS[@]}"

log "Release publish completed."
