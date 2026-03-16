#!/data/data/com.termux/files/usr/bin/sh
set -eu

LLVM_VERSION="${LLVM_VERSION:-22.1.1}"
ROOT="/data/data/com.termux/files/home"
REPO="${AEO_HOST_LLVM_REPO:-kosoymiki/aesolator}"
RELEASE_TAG="${AEO_HOST_LLVM_RELEASE_TAG:-host-llvm-${LLVM_VERSION}}"
ASSET_NAME="${AEO_HOST_LLVM_ASSET_NAME:-llvm-${LLVM_VERSION}-termux-android-aarch64.tar.zst}"
TMP_DIR="${ROOT}/.tmp_host_llvm_release"
TARGET_ROOT="${ROOT}/.toolchains"
TARGET_DIR="${TARGET_ROOT}/llvm-${LLVM_VERSION}-termux"
API_ROOT="https://api.github.com/repos/${REPO}"
RELEASE_JSON="${TMP_DIR}/release.json"
ASSET_PATH="${TMP_DIR}/${ASSET_NAME}"
AUTH_HEADER=""

if [ -n "${AEO_GITHUB_TOKEN:-}" ]; then
  AUTH_HEADER="Authorization: Bearer ${AEO_GITHUB_TOKEN}"
elif [ -n "${GITHUB_TOKEN:-}" ]; then
  AUTH_HEADER="Authorization: Bearer ${GITHUB_TOKEN}"
fi

curl_json() {
  if [ -n "${AUTH_HEADER}" ]; then
    curl -fsSL -H "${AUTH_HEADER}" -H "Accept: application/vnd.github+json" "$@"
  else
    curl -fsSL -H "Accept: application/vnd.github+json" "$@"
  fi
}

curl_asset() {
  if [ -n "${AUTH_HEADER}" ]; then
    curl -fL -H "${AUTH_HEADER}" -H "Accept: application/octet-stream" "$@"
  else
    curl -fL -H "Accept: application/octet-stream" "$@"
  fi
}

asset_api_url() {
  awk -v asset="${ASSET_NAME}" '
    BEGIN { RS="\\{"; FS="\n" }
    $0 ~ "\"name\": \"" asset "\"" {
      if (match($0, /"url": "[^"]+\/releases\/assets\/[0-9]+"/)) {
        url = substr($0, RSTART, RLENGTH)
        sub(/"url": "/, "", url)
        sub(/"$/, "", url)
        print url
        exit
      }
    }
  ' "${RELEASE_JSON}"
}

mkdir -p "${TMP_DIR}" "${TARGET_ROOT}"
rm -rf "${TARGET_DIR}"
rm -f "${ASSET_PATH}" "${RELEASE_JSON}"

curl_json "${API_ROOT}/releases/tags/${RELEASE_TAG}" -o "${RELEASE_JSON}"
DOWNLOAD_URL="$(asset_api_url)"

if [ -z "${DOWNLOAD_URL}" ]; then
  echo "failed to locate release asset ${ASSET_NAME} under ${REPO}:${RELEASE_TAG}" >&2
  exit 1
fi

curl_asset "${DOWNLOAD_URL}" -o "${ASSET_PATH}"
tar --zstd -xf "${ASSET_PATH}" -C "${TARGET_ROOT}"

"${TARGET_DIR}/bin/clang" --version | sed -n '1,4p'
"${TARGET_DIR}/bin/llvm-strip" --version | sed -n '1,4p'
