#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
WINLATOR_SRC_DIR="${1:-}"
PATCH_DIR="${2:-${ROOT_DIR}/ci/winlator/patches}"
: "${WINLATOR_PATCH_FROM:=}"
: "${WINLATOR_PATCH_TO:=}"

log()  { printf '[winlator-patch] %s\n' "$*"; }
fail() { printf '[winlator-patch][error] %s\n' "$*" >&2; exit 1; }

[[ -n "${WINLATOR_SRC_DIR}" ]] || fail "usage: $0 <winlator-src-dir> [patch-dir]"
[[ -d "${WINLATOR_SRC_DIR}/.git" ]] || fail "Not a git checkout: ${WINLATOR_SRC_DIR}"
[[ -d "${PATCH_DIR}" ]] || fail "Patch directory not found: ${PATCH_DIR}"
[[ -z "${WINLATOR_PATCH_FROM}" || "${WINLATOR_PATCH_FROM}" =~ ^[0-9]{4}$ ]] || fail "WINLATOR_PATCH_FROM must be empty or NNNN"
[[ -z "${WINLATOR_PATCH_TO}" || "${WINLATOR_PATCH_TO}" =~ ^[0-9]{4}$ ]] || fail "WINLATOR_PATCH_TO must be empty or NNNN"

WINLATOR_SRC_DIR="$(cd -- "${WINLATOR_SRC_DIR}" && pwd)"
PATCH_DIR="$(cd -- "${PATCH_DIR}" && pwd)"

if [[ -n "${WINLATOR_PATCH_FROM}" && -n "${WINLATOR_PATCH_TO}" ]]; then
  (( 10#${WINLATOR_PATCH_FROM} <= 10#${WINLATOR_PATCH_TO} )) || fail "WINLATOR_PATCH_FROM must be <= WINLATOR_PATCH_TO"
fi

shopt -s nullglob
patches=("${PATCH_DIR}"/*.patch)
shopt -u nullglob

(( ${#patches[@]} )) || { log "No patches found in ${PATCH_DIR}; skipping"; exit 0; }

filter_patch_window() {
  local selected=()
  local patch name num

  if [[ -z "${WINLATOR_PATCH_FROM}" && -z "${WINLATOR_PATCH_TO}" ]]; then
    return 0
  fi

  for patch in "${patches[@]}"; do
    name="$(basename -- "${patch}")"
    [[ "${name}" =~ ^([0-9]{4})- ]] || fail "Invalid patch filename (missing NNNN- prefix): ${name}"
    num="${BASH_REMATCH[1]}"
    if [[ -n "${WINLATOR_PATCH_FROM}" ]] && (( 10#${num} < 10#${WINLATOR_PATCH_FROM} )); then
      continue
    fi
    if [[ -n "${WINLATOR_PATCH_TO}" ]] && (( 10#${num} > 10#${WINLATOR_PATCH_TO} )); then
      continue
    fi
    selected+=("${patch}")
  done

  patches=("${selected[@]}")
}

filter_patch_window

(( ${#patches[@]} )) || {
  log "No patches matched requested window (${WINLATOR_PATCH_FROM:-start}..${WINLATOR_PATCH_TO:-end}); skipping"
  exit 0
}

if [[ -n "${WINLATOR_PATCH_FROM}" || -n "${WINLATOR_PATCH_TO}" ]]; then
  log "Selected patch window: ${WINLATOR_PATCH_FROM:-start} .. ${WINLATOR_PATCH_TO:-end} (${#patches[@]} patches)"
fi

heal_known_rejects() {
  local patch_name="$1"
  local rejs
  local strings_file strings_rej
  local styles_file styles_rej
  local wineinfo_file wineinfo_rej wineinfo_template
  local container_detail_file container_detail_rej container_detail_template
  local xserver_file xserver_rej xserver_template
  local xserver_vulkan_template
  local xserver_signal_inputs_template
  local content_profile_file content_profile_rej content_profile_template
  local contents_manager_file contents_manager_rej
  local contents_fragment_file contents_fragment_rej contents_fragment_tail_template
  local launcher_file launcher_rej launcher_signal_policy_template
  local healed=0

  case "${patch_name}" in
    0001-mainline-full-stack-consolidated.patch)
      strings_file="${WINLATOR_SRC_DIR}/app/src/main/res/values/strings.xml"
      strings_rej="${strings_file}.rej"
      styles_file="${WINLATOR_SRC_DIR}/app/src/main/res/values/styles.xml"
      styles_rej="${styles_file}.rej"
      wineinfo_file="${WINLATOR_SRC_DIR}/app/src/main/java/com/winlator/cmod/core/WineInfo.java"
      wineinfo_rej="${wineinfo_file}.rej"
      wineinfo_template="${ROOT_DIR}/ci/winlator/templates/WineInfo.arm64ec-route.java"
      container_detail_file="${WINLATOR_SRC_DIR}/app/src/main/java/com/winlator/cmod/ContainerDetailFragment.java"
      container_detail_rej="${container_detail_file}.rej"
      container_detail_template="${ROOT_DIR}/ci/winlator/templates/drift-heal/ContainerDetailFragment.java"
      xserver_file="${WINLATOR_SRC_DIR}/app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java"
      xserver_rej="${xserver_file}.rej"
      xserver_template="${ROOT_DIR}/ci/winlator/templates/drift-heal/XServerDisplayActivity.java"
      content_profile_file="${WINLATOR_SRC_DIR}/app/src/main/java/com/winlator/cmod/contents/ContentProfile.java"
      content_profile_rej="${content_profile_file}.rej"
      content_profile_template="${ROOT_DIR}/ci/winlator/templates/drift-heal/ContentProfile.java"
      contents_manager_file="${WINLATOR_SRC_DIR}/app/src/main/java/com/winlator/cmod/contents/ContentsManager.java"
      contents_manager_rej="${contents_manager_file}.rej"
      mapfile -t rejs < <(find "${WINLATOR_SRC_DIR}" -name '*.rej' -type f | sort)

      [[ "${#rejs[@]}" -gt 0 ]] || return 1

      if [[ -f "${strings_rej}" ]]; then
        grep -Fq 'setCompositeRemoteProfiles(' "${WINLATOR_SRC_DIR}/app/src/main/java/com/winlator/cmod/contents/ContentsManager.java" || return 1
        grep -Fq 'DASH_PLACEHOLDER' "${WINLATOR_SRC_DIR}/app/src/main/java/com/winlator/cmod/ContentsFragment.java" || return 1

        python3 - "${strings_file}" <<'PY'
import re
import sys
from pathlib import Path

path = Path(sys.argv[1])
text = path.read_text(encoding="utf-8")

replacements = [
    (
        r'<string name="get_more_contents_form_github">.*?</string>',
        '<string name="get_more_contents_form_github">Content packages are loaded from WCP Hub, while FreeWine and graphics runtime packages are provided from dedicated Ae.solator release repos.</string>',
    ),
    (
        r'<string name="show_beta_releases">.*?</string>',
        '<string name="show_beta_releases">Show beta / nightly builds (WCP Hub content)</string>',
    ),
]

updated = text
for pattern, repl in replacements:
    updated, count = re.subn(pattern, repl, updated, count=1)
    if count != 1:
        raise SystemExit(f"targeted reject-heal failed: pattern not found: {pattern}")

path.write_text(updated, encoding="utf-8")
PY

        rm -f "${strings_rej}"
        git -C "${WINLATOR_SRC_DIR}" add "app/src/main/res/values/strings.xml"
        healed=1
      fi

      if [[ -f "${styles_rej}" ]]; then
        python3 - "${styles_file}" <<'PY'
import sys
from pathlib import Path

path = Path(sys.argv[1])
text = path.read_text(encoding="utf-8")

if 'name="ContentsChannelSwitch"' not in text:
    anchor = "    <!-- Button Styles -->"
    block = """    <!-- Compatibility alias used by patched layouts -->
    <style name="SwitchCompat" parent="Widget.AppCompat.CompoundButton.Switch" />
    <style name="ContentsChannelSwitch" parent="SwitchCompat">
        <item name="android:textColor">?android:textColorPrimary</item>
        <item name="android:textSize">14sp</item>
        <item name="android:thumbTint">@color/colorAccent</item>
        <item name="android:trackTint">@color/colorPrimaryDark</item>
        <item name="android:showText">false</item>
        <item name="android:switchMinWidth">44dp</item>
        <item name="android:paddingStart">2dp</item>
        <item name="android:paddingEnd">2dp</item>
    </style>
"""
    if anchor not in text:
        raise SystemExit("targeted reject-heal failed: styles anchor not found")
    text = text.replace(anchor, block + "\n" + anchor, 1)

path.write_text(text, encoding="utf-8")
PY

        rm -f "${styles_rej}"
        git -C "${WINLATOR_SRC_DIR}" add "app/src/main/res/values/styles.xml"
        healed=1
      fi

      if [[ -f "${wineinfo_rej}" ]]; then
        [[ -f "${wineinfo_template}" ]] || return 1
        cp "${wineinfo_template}" "${wineinfo_file}"
        rm -f "${wineinfo_rej}"
        git -C "${WINLATOR_SRC_DIR}" add "app/src/main/java/com/winlator/cmod/core/WineInfo.java"
        healed=1
      fi

      if [[ -f "${container_detail_rej}" ]]; then
        [[ -f "${container_detail_template}" ]] || return 1
        cp "${container_detail_template}" "${container_detail_file}"
        rm -f "${container_detail_rej}"
        git -C "${WINLATOR_SRC_DIR}" add "app/src/main/java/com/winlator/cmod/ContainerDetailFragment.java"
        healed=1
      fi

      if [[ -f "${xserver_rej}" ]]; then
        [[ -f "${xserver_template}" ]] || return 1
        cp "${xserver_template}" "${xserver_file}"
        rm -f "${xserver_rej}"
        git -C "${WINLATOR_SRC_DIR}" add "app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java"
        healed=1
      fi

      if [[ -f "${content_profile_rej}" ]]; then
        [[ -f "${content_profile_template}" ]] || return 1
        cp "${content_profile_template}" "${content_profile_file}"
        rm -f "${content_profile_rej}"
        git -C "${WINLATOR_SRC_DIR}" add "app/src/main/java/com/winlator/cmod/contents/ContentProfile.java"
        healed=1
      fi

      if [[ -f "${contents_manager_rej}" ]]; then
        grep -Fq 'REMOTE_WINE_PROTON_OVERLAY' "${contents_manager_file}" || return 1
        grep -Fq 'public void setRemoteProfiles(String json)' "${contents_manager_file}" || return 1
        if grep -Fq 'setCompositeRemoteProfiles(' "${contents_manager_file}"; then
          rm -f "${contents_manager_rej}"
        else
          python3 - "${contents_manager_file}" <<'PY'
import re
import sys
from pathlib import Path

path = Path(sys.argv[1])
text = path.read_text(encoding="utf-8")

set_remote_pattern = re.compile(
    r'    public void setRemoteProfiles\(String json\) \{\n.*?\n    public void syncContents\(\) \{\n',
    re.S,
)
set_remote_replacement = """    public void setRemoteProfiles(String json) {
        setRemoteProfiles(json, false, false);
    }

    public void setRemoteProfiles(String json, boolean includeBeta, boolean ignoreWine) {
        remoteProfiles = new ArrayList<>();
        appendRemoteProfiles(json, includeBeta, ignoreWine, false);
        syncContents();
    }

    public void setCompositeRemoteProfiles(String hubJson, String repoOverlayJson, boolean showNightlyOnly) {
        remoteProfiles = new ArrayList<>();
        // WCP Hub: exclude repo-managed overlay types to avoid duplicates with our overlay.
        // Keep all hub channels in memory; UI applies per-tab filtering (nightly or ARM64EC).
        appendRemoteProfiles(hubJson, showNightlyOnly, true, false, true);
        // Repo overlay: repo-managed runtime lanes stay stable-only in UI.
        appendRemoteProfiles(repoOverlayJson, false, false, true);
        syncContents();
    }

    private void appendRemoteProfiles(String json, boolean includeBeta, boolean ignoreWine, boolean onlyRepoManaged) {
        appendRemoteProfiles(json, includeBeta, ignoreWine, onlyRepoManaged, false);
    }

    private void appendRemoteProfiles(String json, boolean includeBeta, boolean ignoreWine, boolean onlyRepoManaged, boolean keepAllChannels) {
        if (json == null || json.trim().isEmpty()) return;
        try {
            JSONArray content = new JSONArray(json);
            for (int i = 0; i < content.length(); i++) {
                try {
                    JSONObject object = content.getJSONObject(i);
                    ContentProfile remoteProfile = new ContentProfile();
                    remoteProfile.remoteUrl = readRemoteUrl(object);
                    if (remoteProfile.remoteUrl == null || remoteProfile.remoteUrl.isEmpty()) continue;
                    remoteProfile.type = ContentProfile.ContentType.getTypeByName(object.getString("type"));
                    if (remoteProfile.type == ContentProfile.ContentType.CONTENT_TYPE_WINE) {
                        String internalType = object.optString("internalType", "").trim().toLowerCase();
                        if (internalType.contains("proton")) {
                            remoteProfile.type = ContentProfile.ContentType.CONTENT_TYPE_PROTON;
                        }
                    }
                    remoteProfile.verName = object.optString("verName", "").trim();
                    remoteProfile.verCode = parseVerCode(object);
                    remoteProfile.desc = object.optString("description", "").trim();
                    remoteProfile.displayCategory = object.optString(ContentProfile.MARK_DISPLAY_CATEGORY, "");
                    remoteProfile.sourceRepo = object.optString(ContentProfile.MARK_SOURCE_REPO, "");
                    remoteProfile.releaseTag = object.optString(ContentProfile.MARK_RELEASE_TAG, "");
                    remoteProfile.delivery = object.optString(ContentProfile.MARK_DELIVERY, ContentProfile.DELIVERY_REMOTE);

                    if (remoteProfile.verName.isEmpty()) {
                        remoteProfile.verName = deriveVersionNameFromUrl(remoteProfile.remoteUrl);
                    }
                    if (remoteProfile.desc.isEmpty()) {
                        remoteProfile.desc = object.optString("name", "").trim();
                    }
                    if (remoteProfile.desc.isEmpty()) {
                        remoteProfile.desc = remoteProfile.verName != null ? remoteProfile.verName : "";
                    }

                    if (remoteProfile.type == null) continue;
                    if (onlyRepoManaged && !isRepoManagedOverlayType(remoteProfile.type)) continue;
                    if (ignoreWine && isRepoManagedOverlayType(remoteProfile.type)) continue;
                    String verName = remoteProfile.verName != null ? remoteProfile.verName.toLowerCase() : "";
                    String remoteUrl = remoteProfile.remoteUrl != null ? remoteProfile.remoteUrl.toLowerCase() : "";
                    String channel = object.optString(ContentProfile.MARK_CHANNEL, "").trim().toLowerCase();
                    if (channel.isEmpty()) {
                        channel = deriveLegacyChannel(object, verName, remoteUrl);
                    }
                    remoteProfile.channel = channel;
                    boolean isBeta = ContentProfile.CHANNEL_BETA.equals(channel)
                            || ContentProfile.CHANNEL_NIGHTLY.equals(channel);
                    // Repo overlay entries are intentionally presented as a single stable track in UI.
                    if (onlyRepoManaged && isBeta) continue;
                    if (!onlyRepoManaged && !keepAllChannels) {
                        // Switch OFF  -> stable only
                        // Switch ON   -> beta/nightly only (do not append to stable list)
                        if (includeBeta && !isBeta) continue;
                        if (!includeBeta && isBeta) continue;
                    }

                    remoteProfiles.add(remoteProfile);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void syncContents() {
"""
text, count = set_remote_pattern.subn(set_remote_replacement, text, count=1)
if count != 1:
    raise SystemExit("targeted reject-heal failed: ContentsManager setRemoteProfiles block not found")

read_profile_pattern = re.compile(
    r'    public ContentProfile readProfile\(File file\) \{\n.*?\n    public List<ContentProfile> getProfiles\(ContentProfile\.ContentType type\) \{\n',
    re.S,
)
read_profile_replacement = """    public ContentProfile readProfile(File file) {
        try {
            ContentProfile profile = new ContentProfile();
            JSONObject profileJSONObject = new JSONObject(FileUtils.readString(file));
            String typeName = profileJSONObject.getString(ContentProfile.MARK_TYPE);
            String verName = profileJSONObject.getString(ContentProfile.MARK_VERSION_NAME);
            int verCode = profileJSONObject.getInt(ContentProfile.MARK_VERSION_CODE);
            String desc = profileJSONObject.optString(ContentProfile.MARK_DESC, "");

            JSONArray fileJSONArray = profileJSONObject.getJSONArray(ContentProfile.MARK_FILE_LIST);
            List<ContentProfile.ContentFile> fileList = new ArrayList<>();
            for (int i = 0; i < fileJSONArray.length(); i++) {
                JSONObject contentFileJSONObject = fileJSONArray.getJSONObject(i);
                ContentProfile.ContentFile contentFile = new ContentProfile.ContentFile();
                contentFile.source = contentFileJSONObject.getString(ContentProfile.MARK_FILE_SOURCE);
                contentFile.target = contentFileJSONObject.getString(ContentProfile.MARK_FILE_TARGET);
                fileList.add(contentFile);
            }
            if (typeName.equals(ContentProfile.ContentType.CONTENT_TYPE_WINE.toString()) || typeName.equals(ContentProfile.ContentType.CONTENT_TYPE_PROTON.toString())) {
                JSONObject wineJSONObject = profileJSONObject.getJSONObject(ContentProfile.MARK_WINE);
                profile.wineLibPath = wineJSONObject.getString(ContentProfile.MARK_WINE_LIBPATH);
                profile.wineBinPath = wineJSONObject.getString(ContentProfile.MARK_WINE_BINPATH);
                profile.winePrefixPack = wineJSONObject.getString(ContentProfile.MARK_WINE_PREFIX_PACK);
            }

            profile.type = ContentProfile.ContentType.getTypeByName(typeName);
            profile.verName = verName;
            profile.verCode = verCode;
            profile.desc = (desc == null || desc.trim().isEmpty()) ? verName : desc;
            profile.fileList = fileList;
            profile.channel = profileJSONObject.optString(ContentProfile.MARK_CHANNEL, ContentProfile.CHANNEL_STABLE);
            profile.delivery = profileJSONObject.optString(ContentProfile.MARK_DELIVERY, "");
            profile.displayCategory = profileJSONObject.optString(ContentProfile.MARK_DISPLAY_CATEGORY, "");
            profile.sourceRepo = profileJSONObject.optString(ContentProfile.MARK_SOURCE_REPO, "");
            profile.releaseTag = profileJSONObject.optString(ContentProfile.MARK_RELEASE_TAG, "");
            profile.locallyInstalled = true;
            return profile;
        } catch (Exception e) {
            return null;
        }
    }

    private String deriveLegacyChannel(JSONObject object, String verName, String remoteUrl) {
        if (object.optBoolean("beta", false)) return ContentProfile.CHANNEL_BETA;
        if (verName.contains("nightly") || remoteUrl.contains("nightly")) return ContentProfile.CHANNEL_NIGHTLY;
        if (verName.contains("dev") || verName.contains("alpha")) return ContentProfile.CHANNEL_BETA;
        if (verName.contains("beta") || remoteUrl.contains("beta")) return ContentProfile.CHANNEL_BETA;
        return ContentProfile.CHANNEL_STABLE;
    }

    private int parseVerCode(JSONObject object) {
        Object verCodeValue = object.opt("verCode");
        if (verCodeValue instanceof Number) return ((Number) verCodeValue).intValue();
        if (verCodeValue instanceof String) {
            String text = (String) verCodeValue;
            try {
                return Integer.parseInt(text.trim());
            } catch (Exception ignored) {
                return 0;
            }
        }
        return 0;
    }

    private String readRemoteUrl(JSONObject object) {
        String remoteUrl = object.optString("remoteUrl", "").trim();
        if (!remoteUrl.isEmpty()) return remoteUrl;

        String downloadUrl = object.optString("downloadUrl", "").trim();
        if (!downloadUrl.isEmpty()) return downloadUrl;

        String url = object.optString("url", "").trim();
        if (!url.isEmpty()) return url;

        return "";
    }

    private String deriveVersionNameFromUrl(String remoteUrl) {
        if (remoteUrl == null || remoteUrl.trim().isEmpty()) return "";
        String normalized = remoteUrl.trim();
        int idx = normalized.lastIndexOf('/');
        String tail = idx >= 0 ? normalized.substring(idx + 1) : normalized;
        String lower = tail.toLowerCase();
        if (lower.endsWith(".wcp")) tail = tail.substring(0, tail.length() - 4);
        else if (lower.endsWith(".wcp.xz")) tail = tail.substring(0, tail.length() - 7);
        else if (lower.endsWith(".wcp.zst")) tail = tail.substring(0, tail.length() - 8);
        else if (lower.endsWith(".zip")) tail = tail.substring(0, tail.length() - 4);
        return tail;
    }

    public List<ContentProfile> getProfiles(ContentProfile.ContentType type) {
"""
text, count = read_profile_pattern.subn(read_profile_replacement, text, count=1)
if count != 1:
    raise SystemExit("targeted reject-heal failed: ContentsManager readProfile block not found")

legacy_apply = """    public boolean applyContent(ContentProfile profile) {
        if (profile.type != ContentProfile.ContentType.CONTENT_TYPE_WINE) {
"""
legacy_apply_replacement = """    public boolean applyContent(ContentProfile profile) {
        if (!profile.isWineProtonFamily()) {
"""
if legacy_apply in text:
    text = text.replace(legacy_apply, legacy_apply_replacement, 1)
elif legacy_apply_replacement not in text:
    raise SystemExit("targeted reject-heal failed: ContentsManager applyContent guard not found")

vulkan_prefix_line = """    public static final String[] VULKAN_SDK_TRUST_PREFIXES = {"${sharedir}/vulkan", "${sharedir}/vulkan-sdk"};
"""
graphics_prefix_block = """    public static final String[] TURNIP_DRIVER_TRUST_PREFIXES = {"${sharedir}/vulkan", "${libdir}"};
    public static final String[] OPENGL_DRIVER_TRUST_PREFIXES = {"${libdir}", "${sharedir}/glvnd", "${sharedir}/drirc.d"};
"""
if "TURNIP_DRIVER_TRUST_PREFIXES" not in text and "OPENGL_DRIVER_TRUST_PREFIXES" not in text:
    if vulkan_prefix_line not in text:
        raise SystemExit("targeted reject-heal failed: ContentsManager trust-prefix anchor not found")
    text = text.replace(vulkan_prefix_line, vulkan_prefix_line + graphics_prefix_block, 1)

preferred_profile_marker = """    public static File getInstallDir(Context context, ContentProfile profile) {
"""
preferred_profile_method = """    public ContentProfile getPreferredInstalledProfile(ContentProfile.ContentType type) {
        List<ContentProfile> profiles = getProfiles(type);
        if (profiles == null || profiles.isEmpty()) return null;

        ContentProfile preferred = null;
        for (ContentProfile candidate : profiles) {
            if (candidate == null || !candidate.isInstalledLocally()) continue;
            if (preferred == null) {
                preferred = candidate;
                continue;
            }
            if (candidate.verCode > preferred.verCode) {
                preferred = candidate;
                continue;
            }
            if (candidate.verCode == preferred.verCode) {
                String candidateName = candidate.verName == null ? "" : candidate.verName;
                String preferredName = preferred.verName == null ? "" : preferred.verName;
                if (candidateName.compareToIgnoreCase(preferredName) > 0) {
                    preferred = candidate;
                }
            }
        }
        return preferred;
    }

"""
if "public ContentProfile getPreferredInstalledProfile(ContentProfile.ContentType type)" not in text:
    if preferred_profile_marker not in text:
        raise SystemExit("targeted reject-heal failed: ContentsManager preferred-profile insert anchor not found")
    text = text.replace(preferred_profile_marker, preferred_profile_method + preferred_profile_marker, 1)

repo_managed_pattern = re.compile(
    r'    private boolean isRepoManagedOverlayType\(ContentProfile\.ContentType type\) \{\n.*?\n    \}\n',
    re.S,
)
repo_managed_replacement = """    private boolean isRepoManagedOverlayType(ContentProfile.ContentType type) {
        return isWineFamilyType(type)
                || type == ContentProfile.ContentType.CONTENT_TYPE_VULKAN_SDK
                || type == ContentProfile.ContentType.CONTENT_TYPE_DXVK
                || type == ContentProfile.ContentType.CONTENT_TYPE_VKD3D
                || type == ContentProfile.ContentType.CONTENT_TYPE_TURNIP_DRIVER
                || type == ContentProfile.ContentType.CONTENT_TYPE_OPENGL_DRIVER
                || type == ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO;
    }
"""
text, count = repo_managed_pattern.subn(repo_managed_replacement, text, count=1)
if count != 1:
    raise SystemExit("targeted reject-heal failed: ContentsManager repo-managed overlay block not found")

trusted_prefix_pattern = re.compile(
    r'    private boolean isTrustedByPrefix\(ContentProfile\.ContentType type, String normalizedTarget\) \{\n.*?\n    \}\n',
    re.S,
)
trusted_prefix_replacement = """    private boolean isTrustedByPrefix(ContentProfile.ContentType type, String normalizedTarget) {
        String[] prefixes = switch (type) {
            case CONTENT_TYPE_VULKAN_SDK -> VULKAN_SDK_TRUST_PREFIXES;
            case CONTENT_TYPE_TURNIP_DRIVER -> TURNIP_DRIVER_TRUST_PREFIXES;
            case CONTENT_TYPE_OPENGL_DRIVER -> OPENGL_DRIVER_TRUST_PREFIXES;
            default -> null;
        };
        if (prefixes == null) return false;
        for (String prefix : prefixes) {
            String resolvedPrefix = Paths.get(getPathFromTemplate(prefix)).toAbsolutePath().normalize().toString();
            if (normalizedTarget.startsWith(resolvedPrefix)) {
                return true;
            }
        }
        return false;
    }
"""
text, count = trusted_prefix_pattern.subn(trusted_prefix_replacement, text, count=1)
if count != 1:
    raise SystemExit("targeted reject-heal failed: ContentsManager trusted-prefix block not found")

path.write_text(text, encoding="utf-8")
PY
          rm -f "${contents_manager_rej}"
          git -C "${WINLATOR_SRC_DIR}" add "app/src/main/java/com/winlator/cmod/contents/ContentsManager.java"
          healed=1
        fi
      fi

      mapfile -t rejs < <(find "${WINLATOR_SRC_DIR}" -name '*.rej' -type f | sort)
      if [[ "${#rejs[@]}" -eq 0 && "${healed}" -eq 1 ]]; then
        log "Applied targeted reject-heal for ${patch_name} (strings/styles/wineinfo/contents drift)"
        return 0
      fi
      return 1
      ;;
    0014-downloads-storage-aesolator-ui.patch)
      contents_fragment_file="${WINLATOR_SRC_DIR}/app/src/main/java/com/winlator/cmod/ContentsFragment.java"
      contents_fragment_rej="${contents_fragment_file}.rej"
      contents_fragment_tail_template="${ROOT_DIR}/ci/winlator/templates/drift-heal/ContentsFragment.0014-tail.javafrag"

      [[ -f "${contents_fragment_rej}" ]] || return 1
      [[ -f "${contents_fragment_tail_template}" ]] || return 1
      grep -Fq 'private boolean isRemoteDownloadActive(ContentProfile profile)' "${contents_fragment_file}" || return 1
      grep -Fq 'private final ImageButton ibDownload;' "${contents_fragment_file}" || return 1

      python3 - "${contents_fragment_file}" "${contents_fragment_tail_template}" <<'PY'
import re
import sys
from pathlib import Path

path = Path(sys.argv[1])
tail = Path(sys.argv[2]).read_text(encoding="utf-8")
text = path.read_text(encoding="utf-8")

pattern = re.compile(r'    private void loadContentList\(\) \{\n.*\n\}\n\Z', re.S)
updated, count = pattern.subn(tail, text, count=1)
if count != 1:
    raise SystemExit("targeted reject-heal failed: ContentsFragment tail anchor not found")

path.write_text(updated, encoding="utf-8")
PY

      rm -f "${contents_fragment_rej}"
      git -C "${WINLATOR_SRC_DIR}" add "app/src/main/java/com/winlator/cmod/ContentsFragment.java"
      log "Applied targeted reject-heal for ${patch_name} (ContentsFragment managed-download tail)"
      return 0
      ;;
    0021-vulkan-arm-diagnostics.patch)
      xserver_file="${WINLATOR_SRC_DIR}/app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java"
      xserver_rej="${xserver_file}.rej"
      xserver_vulkan_template="${ROOT_DIR}/ci/winlator/templates/drift-heal/XServerDisplayActivity.0021-vulkan-block.javafrag"

      [[ -f "${xserver_rej}" ]] || return 1
      [[ -f "${xserver_vulkan_template}" ]] || return 1
      grep -Fq 'private static void appendUniqueEnvValue(EnvVars envVars, String key, String value)' "${xserver_file}" || return 1
      grep -Fq 'envVars.put("VK_ICD_FILENAMES", imageFs.getShareDir() + "/vulkan/icd.d/wrapper_icd.aarch64.json");' "${xserver_file}" || return 1

      python3 - "${xserver_file}" "${xserver_vulkan_template}" <<'PY'
import re
import sys
from pathlib import Path

path = Path(sys.argv[1])
block = Path(sys.argv[2]).read_text(encoding="utf-8")
text = path.read_text(encoding="utf-8")

pattern = re.compile(
    r'        boolean useDRI3 = preferences\.getBoolean\("use_dri3", true\);\n.*?(?=        if \(firstTimeBoot\) \{)',
    re.S,
)
updated, count = pattern.subn(block, text, count=1)
if count != 1:
    raise SystemExit("targeted reject-heal failed: XServerDisplayActivity Vulkan block anchor not found")

path.write_text(updated, encoding="utf-8")
PY

      rm -f "${xserver_rej}"
      git -C "${WINLATOR_SRC_DIR}" add "app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java"
      log "Applied targeted reject-heal for ${patch_name} (XServerDisplayActivity Vulkan block)"
      return 0
      ;;
    0025-runtime-signal-contract-helper.patch)
      xserver_file="${WINLATOR_SRC_DIR}/app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java"
      xserver_rej="${xserver_file}.rej"
      xserver_signal_inputs_template="${ROOT_DIR}/ci/winlator/templates/drift-heal/XServerDisplayActivity.0025-signal-inputs.javafrag"
      launcher_file="${WINLATOR_SRC_DIR}/app/src/main/java/com/winlator/cmod/xenvironment/components/GuestProgramLauncherComponent.java"
      launcher_rej="${launcher_file}.rej"
      launcher_signal_policy_template="${ROOT_DIR}/ci/winlator/templates/drift-heal/GuestProgramLauncherComponent.0025-signal-policy.javafrag"

      [[ -f "${xserver_rej}" || -f "${launcher_rej}" ]] || return 1
      [[ -f "${xserver_signal_inputs_template}" ]] || return 1
      [[ -f "${launcher_signal_policy_template}" ]] || return 1

      if [[ -f "${xserver_rej}" ]]; then
        grep -Fq 'overrideEnvVars.clear(); // Clear overrideEnvVars as per smali logic' "${xserver_file}" || return 1
        if ! grep -Fq 'RUNTIME_SIGNAL_INPUTS_PREPARED' "${xserver_file}"; then
          python3 - "${xserver_file}" "${xserver_signal_inputs_template}" <<'PY'
import sys
from pathlib import Path

path = Path(sys.argv[1])
block = Path(sys.argv[2]).read_text(encoding="utf-8")
text = path.read_text(encoding="utf-8")
anchor = "            overrideEnvVars.clear(); // Clear overrideEnvVars as per smali logic\n        }\n\n"
if anchor not in text:
    raise SystemExit("targeted reject-heal failed: XServerDisplayActivity signal anchor not found")
text = text.replace(anchor, anchor + block, 1)
path.write_text(text, encoding="utf-8")
PY
        fi
        rm -f "${xserver_rej}"
        git -C "${WINLATOR_SRC_DIR}" add "app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java"
      fi

      if [[ -f "${launcher_rej}" ]]; then
        grep -Fq 'FileUtils.chmod(box64File, 0755);' "${launcher_file}" || return 1
        if ! grep -Fq 'RUNTIME_SIGNAL_POLICY_APPLIED' "${launcher_file}"; then
          python3 - "${launcher_file}" "${launcher_signal_policy_template}" <<'PY'
import sys
from pathlib import Path

path = Path(sys.argv[1])
block = Path(sys.argv[2]).read_text(encoding="utf-8")
text = path.read_text(encoding="utf-8")
anchor = "        if (box64File.exists()) {\n            FileUtils.chmod(box64File, 0755);\n        }\n\n"
if anchor not in text:
    raise SystemExit("targeted reject-heal failed: GuestProgramLauncherComponent signal anchor not found")
text = text.replace(anchor, anchor + block, 1)
path.write_text(text, encoding="utf-8")
PY
        fi
        rm -f "${launcher_rej}"
        git -C "${WINLATOR_SRC_DIR}" add "app/src/main/java/com/winlator/cmod/xenvironment/components/GuestProgramLauncherComponent.java"
      fi

      log "Applied targeted reject-heal for ${patch_name} (runtime signal insertion blocks)"
      return 0
      ;;
  esac

  return 1
}

apply_one() {
  local patch="$1"
  local name; name="$(basename -- "$patch")"
  local reject_rc=0
  local rejs=()

  # If patch already applied, reverse-check succeeds -> skip
  if git -C "$WINLATOR_SRC_DIR" apply --reverse --check --recount --ignore-whitespace "$patch" >/dev/null 2>&1; then
    log "Already applied: $name (skipping)"
    return 0
  fi

  # Try clean apply (3way + stage)
  if git -C "$WINLATOR_SRC_DIR" apply --index --3way --recount --whitespace=nowarn --ignore-whitespace "$patch" >/dev/null 2>&1; then
    log "Applied: $name"
    return 0
  fi

  # Some sequential patches no longer 3way cleanly against the transient index,
  # but they still apply directly without rejects.
  if git -C "$WINLATOR_SRC_DIR" apply --check --recount --whitespace=nowarn --ignore-whitespace "$patch" >/dev/null 2>&1; then
    git -C "$WINLATOR_SRC_DIR" apply --recount --whitespace=nowarn --ignore-whitespace "$patch"
    git -C "$WINLATOR_SRC_DIR" add -A
    log "Applied direct: $name"
    return 0
  fi

  # Fallback: generate rejects (NO --3way with --reject)
  log "Conflicts, generating *.rej: $name"
  git -C "$WINLATOR_SRC_DIR" apply --recount --reject --whitespace=nowarn --ignore-whitespace "$patch" || reject_rc=$?
  mapfile -t rejs < <(find "${WINLATOR_SRC_DIR}" -name '*.rej' -type f | sort)

  # Some patches fail --3way due index drift but still apply cleanly in --reject mode.
  if [[ "${#rejs[@]}" -eq 0 ]]; then
    if [[ "${reject_rc}" -eq 0 ]] || git -C "$WINLATOR_SRC_DIR" apply --reverse --check --recount --ignore-whitespace "$patch" >/dev/null 2>&1; then
      log "Applied via reject fallback: $name"
      return 0
    fi
  fi

  if heal_known_rejects "$name"; then
    return 0
  fi

  mapfile -t rejs < <(find "${WINLATOR_SRC_DIR}" -name '*.rej' -type f | sort)
  if [[ "${#rejs[@]}" -gt 0 ]]; then
    printf '[winlator-patch][error] Reject files for %s:\n' "$name" >&2
    printf '  %s\n' "${rejs[@]}" >&2
  fi
  fail "Failed to apply $name. Show *.rej:\n  find \"$WINLATOR_SRC_DIR\" -name '*.rej' -print -exec sed -n '1,160p' {} \\;"
}

for patch in "${patches[@]}"; do
  log "Applying $(basename -- "$patch")"
  apply_one "$patch"
done

log "All patches applied"
