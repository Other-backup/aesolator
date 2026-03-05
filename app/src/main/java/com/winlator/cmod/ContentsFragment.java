package com.winlator.cmod;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.database.Cursor;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.contentdialog.ContentDialog;
import com.winlator.cmod.contentdialog.ContentInfoDialog;
import com.winlator.cmod.contentdialog.ContentUntrustedDialog;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.contents.Downloader;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.core.PreloaderDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class ContentsFragment extends Fragment {
    private enum ImportArchHint {
        UNKNOWN,
        ARM64EC,
        X86_64,
        ARM64
    }

    private static final String PREF_REMOTE_CACHE_JSON = "contents_remote_cache_json";
    private static final List<ContentProfile.ContentType> SUPPORTED_CONTENT_TYPES = Arrays.asList(
            ContentProfile.ContentType.CONTENT_TYPE_WINE,
            ContentProfile.ContentType.CONTENT_TYPE_PROTON,
            ContentProfile.ContentType.CONTENT_TYPE_DXVK,
            ContentProfile.ContentType.CONTENT_TYPE_VKD3D,
            ContentProfile.ContentType.CONTENT_TYPE_VULKAN_SDK,
            ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO,
            ContentProfile.ContentType.CONTENT_TYPE_BOX64,
            ContentProfile.ContentType.CONTENT_TYPE_WOWBOX64,
            ContentProfile.ContentType.CONTENT_TYPE_FEXCORE
    );

    private RecyclerView recyclerView;
    private View emptyText;
    private ContentsManager manager;
    private SharedPreferences sharedPreferences;
    private ContentProfile.ContentType currentContentType = ContentProfile.ContentType.CONTENT_TYPE_WINE;
    private Spinner sContentType;
    private boolean isDarkMode;
    private TextView tvContentsFiltersLabel;
    private ViewGroup llContentsFilters;
    private CheckBox cbSourceWcpHub;
    private CheckBox cbSourceFallback;
    private CheckBox cbSourceAesolator;
    private CheckBox cbFilterArm64ec;
    private CheckBox cbFilterX64;
    private CheckBox cbFilterBeta;
    private boolean sourceWcpHubEnabled;
    private boolean sourceFallbackEnabled;
    private boolean sourceAesolatorEnabled;
    private boolean filterArm64ecEnabled;
    private boolean filterX64Enabled;
    private boolean filterBetaEnabled;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        manager = new ContentsManager(getContext());
        manager.syncContents();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        sourceWcpHubEnabled = sharedPreferences.getBoolean("contents_source_wcphub", true);
        sourceFallbackEnabled = sharedPreferences.getBoolean("contents_source_fallback", true);
        sourceAesolatorEnabled = sharedPreferences.getBoolean("contents_source_aesolator", true);
        filterArm64ecEnabled = sharedPreferences.getBoolean("contents_filter_arm64ec", true);
        filterX64Enabled = sharedPreferences.getBoolean("contents_filter_x64", true);
        filterBetaEnabled = sharedPreferences.getBoolean("contents_filter_beta", false);
    }

    @Override
    public void onDestroy() {
        FileUtils.clear(requireContext().getCacheDir());
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadRemoteContents();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle(R.string.contents);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.contents_fragment, container, false);

        sContentType = layout.findViewById(R.id.SContentType);
        updateContentTypeSpinner(sContentType);
        sContentType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentContentType = SUPPORTED_CONTENT_TYPES.get(position);
                updateFilterControlsVisibility();
                if (emptyText != null && recyclerView != null) {
                    loadContentList();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        applyPreferredContentTypeSelection();

        cbSourceWcpHub = layout.findViewById(R.id.CBSourceWcpHub);
        cbSourceFallback = layout.findViewById(R.id.CBSourceFallback);
        cbSourceAesolator = layout.findViewById(R.id.CBSourceAesolator);
        tvContentsFiltersLabel = layout.findViewById(R.id.TVContentsFiltersLabel);
        llContentsFilters = layout.findViewById(R.id.LLContentsFilters);
        cbFilterArm64ec = layout.findViewById(R.id.CBFilterArm64ec);
        cbFilterX64 = layout.findViewById(R.id.CBFilterX64);
        cbFilterBeta = layout.findViewById(R.id.CBFilterBeta);

        cbSourceWcpHub.setChecked(sourceWcpHubEnabled);
        cbSourceFallback.setChecked(sourceFallbackEnabled);
        cbSourceAesolator.setChecked(sourceAesolatorEnabled);
        cbFilterArm64ec.setChecked(filterArm64ecEnabled);
        cbFilterX64.setChecked(filterX64Enabled);
        cbFilterBeta.setChecked(filterBetaEnabled);

        CompoundButton.OnCheckedChangeListener refreshRemoteListener = (buttonView, isChecked) -> {
            updateFilterPreferencesFromUi();
            reloadRemoteContents();
        };
        CompoundButton.OnCheckedChangeListener refreshListListener = (buttonView, isChecked) -> {
            updateFilterPreferencesFromUi();
            loadContentList();
        };

        cbSourceWcpHub.setOnCheckedChangeListener(refreshRemoteListener);
        cbSourceFallback.setOnCheckedChangeListener(refreshRemoteListener);
        cbSourceAesolator.setOnCheckedChangeListener(refreshRemoteListener);
        cbFilterArm64ec.setOnCheckedChangeListener(refreshListListener);
        cbFilterX64.setOnCheckedChangeListener(refreshListListener);
        cbFilterBeta.setOnCheckedChangeListener(refreshListListener);
        updateFilterControlsVisibility();

        emptyText = layout.findViewById(R.id.TVEmptyText);

        View btInstallContent = layout.findViewById(R.id.BTInstallContent);
        btInstallContent.setOnClickListener(v -> ContentDialog.confirm(
                getContext(),
                getString(R.string.do_you_want_to_install_content) + " "
                        + getString(R.string.pls_make_sure_content_trustworthy) + " "
                        + getString(R.string.content_suffix_is_wcp_packed_xz_zst),
                () -> {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                    requireActivity().startActivityFromFragment(this, intent, MainActivity.OPEN_FILE_REQUEST_CODE);
                }
        ));

        recyclerView = layout.findViewById(R.id.RecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));
        loadContentList();

        return layout;
    }

    private void updateContentTypeSpinner(Spinner spinner) {
        List<String> typeList = new ArrayList<>();
        for (ContentProfile.ContentType type : SUPPORTED_CONTENT_TYPES) {
            typeList.add(getTypeLabel(type));
        }
        spinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, typeList));
        spinner.setPopupBackgroundResource(isDarkMode ? R.drawable.content_dialog_background_dark : R.drawable.content_dialog_background);
    }

    private String getTypeLabel(ContentProfile.ContentType type) {
        if (type == ContentProfile.ContentType.CONTENT_TYPE_PROTON) return "Proton";
        if (type == ContentProfile.ContentType.CONTENT_TYPE_VULKAN_SDK) return "Vulkan SDK";
        if (type == ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO) return "dgVoodoo";
        return type.toString();
    }

    private boolean isProtonLike(ContentProfile profile) {
        if (profile.type == ContentProfile.ContentType.CONTENT_TYPE_PROTON) return true;
        String line = ((profile.verName == null ? "" : profile.verName) + " "
                + (profile.desc == null ? "" : profile.desc)).toLowerCase(Locale.US);
        return line.contains("proton");
    }

    private List<ContentProfile> getVisibleProfiles() {
        List<ContentProfile> profiles = manager.getProfiles(currentContentType);
        if (profiles == null) return new ArrayList<>();

        ArrayList<ContentProfile> filtered = new ArrayList<>();
        for (ContentProfile profile : profiles) {
            if (currentContentType == ContentProfile.ContentType.CONTENT_TYPE_WINE && isProtonLike(profile)) {
                continue;
            }
            if (currentContentType == ContentProfile.ContentType.CONTENT_TYPE_PROTON && !isProtonLike(profile)) {
                continue;
            }
            if (!filterBetaEnabled && profile.isBetaLike()) {
                continue;
            }
            if (isGraphicsStackType(currentContentType)) {
                boolean arm64ec = isArm64EcProfile(profile);
                boolean x64 = isX64Profile(profile);
                if (arm64ec && !filterArm64ecEnabled) continue;
                if (x64 && !filterX64Enabled) continue;
            }
            filtered.add(profile);
        }
        return filtered;
    }

    private void updateFilterPreferencesFromUi() {
        sourceWcpHubEnabled = cbSourceWcpHub != null && cbSourceWcpHub.isChecked();
        sourceFallbackEnabled = cbSourceFallback != null && cbSourceFallback.isChecked();
        sourceAesolatorEnabled = cbSourceAesolator != null && cbSourceAesolator.isChecked();
        filterArm64ecEnabled = cbFilterArm64ec != null && cbFilterArm64ec.isChecked();
        filterX64Enabled = cbFilterX64 != null && cbFilterX64.isChecked();
        filterBetaEnabled = cbFilterBeta != null && cbFilterBeta.isChecked();

        sharedPreferences.edit()
                .putBoolean("contents_source_wcphub", sourceWcpHubEnabled)
                .putBoolean("contents_source_fallback", sourceFallbackEnabled)
                .putBoolean("contents_source_aesolator", sourceAesolatorEnabled)
                .putBoolean("contents_filter_arm64ec", filterArm64ecEnabled)
                .putBoolean("contents_filter_x64", filterX64Enabled)
                .putBoolean("contents_filter_beta", filterBetaEnabled)
                .apply();
    }

    private void applyPreferredContentTypeSelection() {
        String preferredType = sharedPreferences.getString("contents_preselected_type", "");
        if (preferredType == null || preferredType.trim().isEmpty()) return;
        ContentProfile.ContentType preferred = ContentProfile.ContentType.getTypeByName(preferredType.trim());
        if (preferred == null) return;
        int index = SUPPORTED_CONTENT_TYPES.indexOf(preferred);
        if (index >= 0) {
            sContentType.setSelection(index);
            currentContentType = preferred;
        }
        sharedPreferences.edit().remove("contents_preselected_type").apply();
    }

    private boolean isGraphicsStackType(ContentProfile.ContentType type) {
        return type == ContentProfile.ContentType.CONTENT_TYPE_DXVK
                || type == ContentProfile.ContentType.CONTENT_TYPE_VKD3D
                || type == ContentProfile.ContentType.CONTENT_TYPE_VULKAN_SDK
                || type == ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO;
    }

    private boolean supportsArchitectureFilters(ContentProfile.ContentType type) {
        return type == ContentProfile.ContentType.CONTENT_TYPE_DXVK
                || type == ContentProfile.ContentType.CONTENT_TYPE_VKD3D
                || type == ContentProfile.ContentType.CONTENT_TYPE_VULKAN_SDK
                || type == ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO;
    }

    private boolean supportsChannelFilter(ContentProfile.ContentType type) {
        return type == ContentProfile.ContentType.CONTENT_TYPE_WINE
                || type == ContentProfile.ContentType.CONTENT_TYPE_PROTON
                || type == ContentProfile.ContentType.CONTENT_TYPE_DXVK
                || type == ContentProfile.ContentType.CONTENT_TYPE_VKD3D
                || type == ContentProfile.ContentType.CONTENT_TYPE_VULKAN_SDK
                || type == ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO;
    }

    private void updateFilterControlsVisibility() {
        if (!isAdded()) return;
        boolean showArchFilters = supportsArchitectureFilters(currentContentType);
        boolean showChannelFilter = supportsChannelFilter(currentContentType);

        cbFilterArm64ec.setVisibility(showArchFilters ? View.VISIBLE : View.GONE);
        cbFilterX64.setVisibility(showArchFilters ? View.VISIBLE : View.GONE);
        cbFilterBeta.setVisibility(showChannelFilter ? View.VISIBLE : View.GONE);

        boolean showFiltersCard = showArchFilters || showChannelFilter;
        if (tvContentsFiltersLabel != null) tvContentsFiltersLabel.setVisibility(showFiltersCard ? View.VISIBLE : View.GONE);
        if (llContentsFilters != null) llContentsFilters.setVisibility(showFiltersCard ? View.VISIBLE : View.GONE);
    }

    private boolean isArm64EcProfile(ContentProfile profile) {
        if (profile == null) return false;
        String combined = (
                (profile.verName == null ? "" : profile.verName) + " " +
                        (profile.desc == null ? "" : profile.desc) + " " +
                        (profile.remoteUrl == null ? "" : profile.remoteUrl) + " " +
                        (profile.releaseTag == null ? "" : profile.releaseTag)
        ).toLowerCase(Locale.US);
        return combined.contains("arm64ec") || combined.contains("arm64-ec");
    }

    private boolean isX64Profile(ContentProfile profile) {
        if (profile == null) return false;
        String combined = (
                (profile.verName == null ? "" : profile.verName) + " " +
                        (profile.desc == null ? "" : profile.desc) + " " +
                        (profile.remoteUrl == null ? "" : profile.remoteUrl) + " " +
                        (profile.releaseTag == null ? "" : profile.releaseTag)
        ).toLowerCase(Locale.US);
        return combined.contains("x86_64") || combined.contains("x86-64") || combined.contains("amd64");
    }

    private void reloadRemoteContents() {
        ForensicLogger.logEvent(
                getContext(),
                "info",
                "CONTENTS_FEED_REFRESH_START",
                null,
                "contents",
                "refresh_remote_feeds",
                ForensicLogger.fields(
                        "source_wcphub", sourceWcpHubEnabled,
                        "source_fallback", sourceFallbackEnabled,
                        "source_aesolator", sourceAesolatorEnabled
                )
        );
        new Thread(() -> {
            try {
                ArrayList<String> payloads = new ArrayList<>();
                HashSet<String> sources = new HashSet<>();
                boolean useWcpHub = sourceWcpHubEnabled;
                boolean useFallback = sourceFallbackEnabled;
                boolean useAesolator = sourceAesolatorEnabled;
                boolean autoEnabledHub = false;
                if (!useWcpHub && !useFallback && !useAesolator) {
                    useWcpHub = true;
                    autoEnabledHub = true;
                }
                final boolean autoEnabledHubFinal = autoEnabledHub;

                String preferredUrl = sharedPreferences.getString("downloadable_contents_url", ContentsManager.REMOTE_PROFILES);
                if (preferredUrl != null && !preferredUrl.trim().isEmpty()
                        && !ContentsManager.REMOTE_PROFILES.equals(preferredUrl.trim())
                        && !ContentsManager.REMOTE_PROFILES_FALLBACK.equals(preferredUrl.trim())
                        && !ContentsManager.REMOTE_PROFILES_AE.equals(preferredUrl.trim())) {
                    addRemoteFeed(payloads, sources, preferredUrl);
                }
                if (useWcpHub) addRemoteFeed(payloads, sources, ContentsManager.REMOTE_PROFILES);
                if (useFallback) addRemoteFeed(payloads, sources, ContentsManager.REMOTE_PROFILES_FALLBACK);
                if (useAesolator) addRemoteFeed(payloads, sources, ContentsManager.REMOTE_PROFILES_AE);

                if (!isAdded() || getActivity() == null) return;
                if (payloads.isEmpty()) {
                    getActivity().runOnUiThread(() -> {
                        String cached = sharedPreferences.getString(PREF_REMOTE_CACHE_JSON, "[]");
                        boolean useCached = cached != null && !cached.trim().isEmpty() && !"[]".equals(cached.trim());
                        manager.setRemoteProfiles(cached != null && !cached.trim().isEmpty() ? cached : "[]");
                        ForensicLogger.logEvent(
                                getContext(),
                                useCached ? "warn" : "warn",
                                "CONTENTS_FEED_REFRESH_FALLBACK",
                                null,
                                "contents",
                                useCached ? "all_sources_failed_use_cached" : "all_sources_failed_empty",
                                ForensicLogger.fields(
                                        "sources_enabled", sources.size(),
                                        "cached_used", useCached,
                                        "auto_enabled_wcphub", autoEnabledHubFinal
                                )
                        );
                        loadContentList();
                    });
                    return;
                }

                String merged = mergeJsonArrays(payloads);
                getActivity().runOnUiThread(() -> {
                    sharedPreferences.edit().putString(PREF_REMOTE_CACHE_JSON, merged).apply();
                    manager.setRemoteProfiles(merged);
                    ForensicLogger.logEvent(
                            getContext(),
                            "info",
                            "CONTENTS_FEED_REFRESH_DONE",
                            null,
                            "contents",
                            "refresh_complete",
                            ForensicLogger.fields(
                                    "sources_polled", sources.size(),
                                    "payloads_received", payloads.size(),
                                    "merged_size", merged.length(),
                                    "auto_enabled_wcphub", autoEnabledHubFinal
                            )
                    );
                    loadContentList();
                });
            } catch (Exception ignored) {
                if (!isAdded() || getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    String cached = sharedPreferences.getString(PREF_REMOTE_CACHE_JSON, "[]");
                    boolean useCached = cached != null && !cached.trim().isEmpty() && !"[]".equals(cached.trim());
                    manager.setRemoteProfiles(cached != null && !cached.trim().isEmpty() ? cached : "[]");
                    ForensicLogger.logEvent(
                            getContext(),
                            "warn",
                            "CONTENTS_FEED_REFRESH_EXCEPTION",
                            null,
                            "contents",
                            useCached ? "refresh_exception_use_cached" : "refresh_exception_empty",
                            ForensicLogger.fields(
                                    "cached_used", useCached,
                                    "auto_enabled_wcphub", false
                            )
                    );
                    loadContentList();
                });
            }
        }).start();
    }

    private void addRemoteFeed(List<String> payloads, HashSet<String> seenSources, @Nullable String url) {
        if (url == null) return;
        String normalized = url.trim();
        if (normalized.isEmpty() || seenSources.contains(normalized) || !isAllowedFeedUrl(normalized)) return;
        seenSources.add(normalized);
        String json = Downloader.downloadString(normalized);
        if (json != null && !json.trim().isEmpty()) payloads.add(json);
    }

    private boolean isAllowedFeedUrl(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null) return false;
            String normalizedScheme = scheme.trim().toLowerCase(Locale.US);
            if (!"https".equals(normalizedScheme) && !"http".equals(normalizedScheme)) return false;
            String host = uri.getHost();
            if (host == null || host.trim().isEmpty()) return false;
            String normalizedHost = host.trim().toLowerCase(Locale.US);
            if ("http".equals(normalizedScheme) && !isLocalhostHost(normalizedHost)) return false;
            // Reject user:pass@host patterns in feed sources.
            return uri.getUserInfo() == null || uri.getUserInfo().trim().isEmpty();
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isLocalhostHost(String host) {
        return "localhost".equals(host) || "127.0.0.1".equals(host) || "::1".equals(host);
    }

    private String mergeJsonArrays(List<String> payloads) {
        Map<String, JSONObject> bestByEntry = new LinkedHashMap<>();
        for (String payload : payloads) {
            try {
                JSONArray array = new JSONArray(payload);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject candidate = array.getJSONObject(i);
                    String key = buildMergeEntryKey(candidate);
                    JSONObject currentBest = bestByEntry.get(key);
                    if (currentBest == null || isBetterRemoteCandidate(candidate, currentBest)) {
                        bestByEntry.put(key, candidate);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        JSONArray merged = new JSONArray();
        for (JSONObject selected : bestByEntry.values()) {
            merged.put(selected);
        }
        return merged.toString();
    }

    private String buildMergeEntryKey(JSONObject object) {
        String type = object.optString("type", "").trim().toLowerCase(Locale.US);
        String verName = object.optString("verName", "").trim().toLowerCase(Locale.US);
        String channel = object.optString(ContentProfile.MARK_CHANNEL, "").trim().toLowerCase(Locale.US);
        String displayCategory = object.optString(ContentProfile.MARK_DISPLAY_CATEGORY, "").trim().toLowerCase(Locale.US);
        String archHint = resolveRemoteArchHint(object);
        return type + "|" + verName + "|" + channel + "|" + displayCategory + "|" + archHint;
    }

    private boolean isBetterRemoteCandidate(JSONObject candidate, JSONObject currentBest) {
        int candidateVerCode = parseRemoteVerCode(candidate);
        int currentVerCode = parseRemoteVerCode(currentBest);
        if (candidateVerCode != currentVerCode) return candidateVerCode > currentVerCode;

        int candidateSourcePriority = resolveRemoteSourcePriority(candidate);
        int currentSourcePriority = resolveRemoteSourcePriority(currentBest);
        if (candidateSourcePriority != currentSourcePriority) return candidateSourcePriority > currentSourcePriority;

        int candidateChannelPriority = resolveChannelPriority(candidate.optString(ContentProfile.MARK_CHANNEL, ""));
        int currentChannelPriority = resolveChannelPriority(currentBest.optString(ContentProfile.MARK_CHANNEL, ""));
        if (candidateChannelPriority != currentChannelPriority) return candidateChannelPriority > currentChannelPriority;

        // Stable tie-breaker to avoid flicker.
        String candidateUrl = candidate.optString("remoteUrl", "");
        String currentUrl = currentBest.optString("remoteUrl", "");
        return candidateUrl.compareToIgnoreCase(currentUrl) < 0;
    }

    private int parseRemoteVerCode(JSONObject object) {
        Object raw = object.opt("verCode");
        if (raw instanceof Number) return ((Number) raw).intValue();
        if (raw instanceof String) {
            try {
                return Integer.parseInt(((String) raw).trim());
            } catch (Exception ignored) {
            }
        }
        return 0;
    }

    private int resolveRemoteSourcePriority(JSONObject object) {
        String sourceRepo = object.optString(ContentProfile.MARK_SOURCE_REPO, "").toLowerCase(Locale.US);
        String remoteUrl = object.optString("remoteUrl", "").toLowerCase(Locale.US);
        String joined = sourceRepo + " " + remoteUrl;
        if (joined.contains("kosoymiki") || joined.contains("ae.solator") || joined.contains("aesolator")) return 300;
        if (joined.contains("open-wine-components") || joined.contains("wcphub") || joined.contains("arihany")) return 200;
        if (joined.contains("stevenmxz") || joined.contains("winlator-contents")) return 100;
        return 50;
    }

    private int resolveChannelPriority(String channel) {
        String normalized = channel == null ? "" : channel.trim().toLowerCase(Locale.US);
        if (ContentProfile.CHANNEL_STABLE.equals(normalized)) return 30;
        if (ContentProfile.CHANNEL_BETA.equals(normalized)) return 20;
        if (ContentProfile.CHANNEL_NIGHTLY.equals(normalized)) return 10;
        return 0;
    }

    private String resolveRemoteArchHint(JSONObject object) {
        String combined = (
                object.optString("verName", "") + " "
                        + object.optString("description", "") + " "
                        + object.optString("remoteUrl", "") + " "
                        + object.optString(ContentProfile.MARK_RELEASE_TAG, "")
        ).toLowerCase(Locale.US);
        if (combined.contains("arm64ec") || combined.contains("arm64-ec")) return "arm64ec";
        if (combined.contains("x86_64") || combined.contains("x86-64") || combined.contains("amd64")) return "x86_64";
        if (combined.contains("arm64")) return "arm64";
        return "generic";
    }

    private String normalizeSha256(String value) {
        if (value == null) return "";
        String normalized = value.trim().toLowerCase(Locale.US).replaceAll("[^0-9a-f]", "");
        return normalized.length() == 64 ? normalized : "";
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == MainActivity.OPEN_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            PreloaderDialog preloaderDialog = new PreloaderDialog(requireActivity());
            preloaderDialog.showOnUiThread(R.string.installing_content);
            try {
                final Uri importUri = data.getData();
                if (importUri == null) {
                    preloaderDialog.closeOnUiThread();
                    AppUtils.showToast(getContext(), R.string.unable_to_import_profile);
                    return;
                }
                final String importFileName = resolveImportFileName(importUri);
                final ContentProfile.ContentType expectedType = detectExpectedTypeFromName(importFileName);
                final ImportArchHint expectedArch = detectExpectedArchFromName(importFileName);
                final boolean glibcTaggedArchive = detectGlibcTaggedArchive(importFileName);
                ContentsManager.OnInstallFinishedCallback callback = new ContentsManager.OnInstallFinishedCallback() {
                    private boolean isExtracting = true;

                    @Override
                    public void onFailed(ContentsManager.InstallFailedReason reason, Exception e) {
                        String reasonCode = switch (reason) {
                            case ERROR_BADTAR -> "bad_archive";
                            case ERROR_NOPROFILE -> "missing_profile";
                            case ERROR_BADPROFILE -> "invalid_profile";
                            case ERROR_EXIST -> "already_exists";
                            case ERROR_MISSINGFILES -> "missing_files";
                            case ERROR_UNTRUSTPROFILE -> "untrusted_profile";
                            default -> "unknown";
                        };
                        ForensicLogger.logEvent(
                                getContext(),
                                "warn",
                                "CONTENTS_IMPORT_REJECTED",
                                null,
                                "contents_import",
                                "import_rejected",
                                ForensicLogger.fields(
                                        "reason", reasonCode,
                                        "file_name", importFileName,
                                        "expected_type", expectedType != null ? expectedType.toString() : "-",
                                        "expected_arch", getImportArchLabel(expectedArch)
                                )
                        );
                        int msgId = switch (reason) {
                            case ERROR_BADTAR -> R.string.file_cannot_be_recognied;
                            case ERROR_NOPROFILE -> R.string.profile_not_found_in_content;
                            case ERROR_BADPROFILE -> R.string.profile_cannot_be_recognized;
                            case ERROR_EXIST -> R.string.content_already_exist;
                            case ERROR_MISSINGFILES -> R.string.content_is_incomplete;
                            case ERROR_UNTRUSTPROFILE -> R.string.content_cannot_be_trusted;
                            default -> R.string.unable_to_install_content;
                        };
                        requireActivity().runOnUiThread(() -> ContentDialog.alert(
                                getContext(),
                                getString(R.string.install_failed) + ": " + getString(msgId),
                                preloaderDialog::closeOnUiThread
                        ));
                    }

                    @Override
                    public void onSucceed(ContentProfile profile) {
                        if (isExtracting) {
                            if (expectedType != null && profile.type != expectedType) {
                                ForensicLogger.logEvent(
                                        getContext(),
                                        "warn",
                                        "CONTENTS_IMPORT_REJECTED",
                                        null,
                                        "contents_import",
                                        "import_rejected",
                                        ForensicLogger.fields(
                                                "reason", "type_mismatch",
                                                "file_name", importFileName,
                                                "expected_type", expectedType.toString(),
                                                "detected_type", profile.type.toString(),
                                                "expected_arch", getImportArchLabel(expectedArch)
                                        )
                                );
                                preloaderDialog.closeOnUiThread();
                                requireActivity().runOnUiThread(() -> ContentDialog.alert(
                                        getContext(),
                                        getString(R.string.install_failed) + ": "
                                                + getString(R.string.content_type_mismatch_import,
                                                getTypeLabel(expectedType),
                                                getTypeLabel(profile.type)),
                                        null
                                ));
                                return;
                            }

                            if (glibcTaggedArchive
                                    && (profile.type == ContentProfile.ContentType.CONTENT_TYPE_WINE
                                    || profile.type == ContentProfile.ContentType.CONTENT_TYPE_PROTON)) {
                                ForensicLogger.logEvent(
                                        getContext(),
                                        "warn",
                                        "CONTENTS_IMPORT_REJECTED",
                                        null,
                                        "contents_import",
                                        "import_rejected",
                                        ForensicLogger.fields(
                                                "reason", "glibc_variant_unsupported",
                                                "file_name", importFileName,
                                                "detected_type", profile.type.toString(),
                                                "expected_arch", getImportArchLabel(expectedArch)
                                        )
                                );
                                preloaderDialog.closeOnUiThread();
                                requireActivity().runOnUiThread(() -> ContentDialog.alert(
                                        getContext(),
                                        getString(R.string.install_failed) + ": "
                                                + getString(R.string.content_glibc_import_unsupported),
                                        null
                                ));
                                return;
                            }

                            ImportArchHint detectedArch = detectProfileArch(profile);
                            if (isImportArchMismatch(expectedArch, detectedArch)) {
                                ForensicLogger.logEvent(
                                        getContext(),
                                        "warn",
                                        "CONTENTS_IMPORT_REJECTED",
                                        null,
                                        "contents_import",
                                        "import_rejected",
                                        ForensicLogger.fields(
                                                "reason", "arch_mismatch",
                                                "file_name", importFileName,
                                                "detected_type", profile.type.toString(),
                                                "expected_arch", getImportArchLabel(expectedArch),
                                                "detected_arch", getImportArchLabel(detectedArch)
                                        )
                                );
                                preloaderDialog.closeOnUiThread();
                                requireActivity().runOnUiThread(() -> ContentDialog.alert(
                                        getContext(),
                                        getString(R.string.install_failed) + ": "
                                                + getString(R.string.content_arch_mismatch_import,
                                                getImportArchLabel(expectedArch),
                                                getImportArchLabel(detectedArch)),
                                        null
                                ));
                                return;
                            }

                            ContentsManager.OnInstallFinishedCallback cb = this;
                            requireActivity().runOnUiThread(() -> {
                                ContentInfoDialog dialog = new ContentInfoDialog(getContext(), profile);
                                ((TextView) dialog.findViewById(R.id.BTConfirm)).setText(R.string._continue);
                                dialog.setOnConfirmCallback(() -> {
                                    isExtracting = false;
                                    List<ContentProfile.ContentFile> untrustedFiles = manager.getUnTrustedContentFiles(profile);
                                    if (!untrustedFiles.isEmpty()) {
                                        ContentUntrustedDialog untrustedDialog = new ContentUntrustedDialog(getContext(), untrustedFiles);
                                        untrustedDialog.setOnCancelCallback(preloaderDialog::closeOnUiThread);
                                        untrustedDialog.setOnConfirmCallback(() -> manager.finishInstallContent(profile, cb));
                                        untrustedDialog.show();
                                    } else {
                                        manager.finishInstallContent(profile, cb);
                                    }
                                });
                                dialog.setOnCancelCallback(preloaderDialog::closeOnUiThread);
                                dialog.show();
                            });
                        } else {
                            preloaderDialog.closeOnUiThread();
                            requireActivity().runOnUiThread(() -> {
                                ContentDialog.alert(getContext(), R.string.content_installed_success, null);
                                manager.syncContents();
                                loadContentList();
                            });
                        }
                    }
                };
                Executors.newSingleThreadExecutor().execute(() -> manager.extraContentFile(importUri, callback));
            } catch (Exception e) {
                preloaderDialog.closeOnUiThread();
                AppUtils.showToast(getContext(), R.string.unable_to_import_profile);
            }
        }
    }

    private void loadContentList() {
        List<ContentProfile> profiles = getVisibleProfiles();
        if (profiles.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setAdapter(new ContentItemAdapter(profiles));
        }
    }

    private String getSourceLabel(ContentProfile profile) {
        if (profile.remoteUrl == null || profile.remoteUrl.trim().isEmpty()) {
            return "Local package";
        }
        try {
            Uri uri = Uri.parse(profile.remoteUrl);
            String host = uri.getHost();
            if (host != null && !host.isEmpty()) {
                return "Remote: " + host;
            }
        } catch (Exception ignored) {
        }
        return "Remote package";
    }

    private String buildProfileTitleLine(ContentProfile profile) {
        String versionName = profile.verName != null ? profile.verName.trim() : "";
        if (!versionName.isEmpty()) return versionName;
        return getDisplayTypeLabel(profile.type);
    }

    private String buildProfileMetaLine(ContentProfile profile) {
        StringBuilder meta = new StringBuilder(getString(R.string.version_code) + ": " + profile.verCode);
        if (profile.isBetaLike()) meta.append(" • beta");
        if (profile.isInstalledLocally()) meta.append(" • installed");
        return meta.toString();
    }

    private String buildProfileSourceLine(ContentProfile profile) {
        String sourceLabel = getSourceLabel(profile);
        String repo = profile.sourceRepo != null ? profile.sourceRepo.trim() : "";
        String tag = profile.releaseTag != null ? profile.releaseTag.trim() : "";
        if (!repo.isEmpty() && !tag.isEmpty()) return sourceLabel + " • " + repo + "@" + tag;
        if (!repo.isEmpty()) return sourceLabel + " • " + repo;
        if (!tag.isEmpty()) return sourceLabel + " • " + tag;
        return sourceLabel;
    }

    private String getDisplayTypeLabel(ContentProfile.ContentType type) {
        return getTypeLabel(type);
    }

    private String resolveImportFileName(@Nullable Uri uri) {
        if (uri == null) return "";
        String fileName = "";
        try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) fileName = cursor.getString(nameIndex);
            }
        } catch (Exception ignored) {
        }

        if (fileName == null || fileName.trim().isEmpty()) {
            String fallback = uri.getLastPathSegment();
            fileName = fallback == null ? "" : fallback;
        }
        return fileName.trim();
    }

    private ContentProfile.ContentType detectExpectedTypeFromName(String fileName) {
        if (fileName == null) return null;
        String lower = fileName.trim().toLowerCase(Locale.US);
        if (lower.isEmpty()) return null;
        boolean containsWine = lower.contains("wine");
        boolean containsProton = lower.contains("proton");
        if (containsProton && !containsWine) return ContentProfile.ContentType.CONTENT_TYPE_PROTON;
        if (containsWine && !containsProton) return ContentProfile.ContentType.CONTENT_TYPE_WINE;
        return null;
    }

    private boolean detectGlibcTaggedArchive(String fileName) {
        if (fileName == null) return false;
        return fileName.trim().toLowerCase(Locale.US).contains("glibc");
    }

    private ImportArchHint detectExpectedArchFromName(String fileName) {
        if (fileName == null) return ImportArchHint.UNKNOWN;
        String lower = fileName.trim().toLowerCase(Locale.US);
        if (lower.isEmpty()) return ImportArchHint.UNKNOWN;
        if (lower.contains("arm64ec") || lower.contains("arm64-ec")) return ImportArchHint.ARM64EC;
        if (lower.contains("x86_64") || lower.contains("x86-64") || lower.contains("amd64")) return ImportArchHint.X86_64;
        if (lower.contains("arm64")) return ImportArchHint.ARM64;
        return ImportArchHint.UNKNOWN;
    }

    private ImportArchHint detectProfileArch(ContentProfile profile) {
        if (profile == null) return ImportArchHint.UNKNOWN;
        if (isArm64EcProfile(profile)) return ImportArchHint.ARM64EC;
        if (isX64Profile(profile)) return ImportArchHint.X86_64;
        String combined = (
                (profile.verName == null ? "" : profile.verName) + " " +
                        (profile.desc == null ? "" : profile.desc) + " " +
                        (profile.remoteUrl == null ? "" : profile.remoteUrl) + " " +
                        (profile.releaseTag == null ? "" : profile.releaseTag)
        ).toLowerCase(Locale.US);
        if (combined.contains("arm64")) return ImportArchHint.ARM64;
        return ImportArchHint.UNKNOWN;
    }

    private boolean isImportArchMismatch(ImportArchHint expected, ImportArchHint detected) {
        if (expected == ImportArchHint.UNKNOWN || detected == ImportArchHint.UNKNOWN) return false;
        return expected != detected;
    }

    private String getImportArchLabel(ImportArchHint archHint) {
        if (archHint == null) return "unknown";
        return switch (archHint) {
            case ARM64EC -> "arm64ec";
            case X86_64 -> "x86_64";
            case ARM64 -> "arm64";
            default -> "unknown";
        };
    }

    private int resolveProfileAccentColor(ContentProfile profile) {
        int fallbackRes = isDarkMode ? R.color.colorAccentDark : R.color.colorAccent;
        int colorRes = switch (profile.type) {
            case CONTENT_TYPE_WINE -> isDarkMode ? R.color.contents_lane_wine_dark : R.color.contents_lane_wine;
            case CONTENT_TYPE_PROTON -> isDarkMode ? R.color.contents_lane_proton_dark : R.color.contents_lane_proton;
            case CONTENT_TYPE_VULKAN_SDK -> resolveVulkanFamilyAccentColor(profile);
            case CONTENT_TYPE_TURNIP_DRIVER -> isDarkMode ? R.color.contents_lane_turnip_dark : R.color.contents_lane_turnip;
            case CONTENT_TYPE_OPENGL_DRIVER -> isDarkMode ? R.color.contents_lane_opengl_dark : R.color.contents_lane_opengl;
            case CONTENT_TYPE_DGVOODOO -> isDarkMode ? R.color.contents_lane_dgvoodoo_dark : R.color.contents_lane_dgvoodoo;
            case CONTENT_TYPE_DXVK -> isDarkMode ? R.color.contents_lane_dxvk_dark : R.color.contents_lane_dxvk;
            case CONTENT_TYPE_VKD3D -> isDarkMode ? R.color.contents_lane_vkd3d_dark : R.color.contents_lane_vkd3d;
            default -> fallbackRes;
        };
        return ContextCompat.getColor(requireContext(), colorRes);
    }

    private int resolveVulkanFamilyAccentColor(ContentProfile profile) {
        String category = profile.getDisplayCategory();
        String source = profile.sourceRepo != null ? profile.sourceRepo : "";
        String version = profile.verName != null ? profile.verName : "";
        String laneHint = (category + " " + source + " " + version).toLowerCase(Locale.US);
        if (laneHint.contains("turnip")) {
            return isDarkMode ? R.color.contents_lane_turnip_dark : R.color.contents_lane_turnip;
        }
        if (laneHint.contains("opengl") || laneHint.contains("zink")) {
            return isDarkMode ? R.color.contents_lane_opengl_dark : R.color.contents_lane_opengl;
        }
        return isDarkMode ? R.color.contents_lane_vulkansdk_dark : R.color.contents_lane_vulkansdk;
    }

    private GradientDrawable buildCategoryBadgeBackground(int accentColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dpToPx(10f));
        drawable.setColor(withAlpha(accentColor, isDarkMode ? 74 : 36));
        drawable.setStroke(dpToPx(1f), withAlpha(accentColor, isDarkMode ? 220 : 132));
        return drawable;
    }

    private int withAlpha(int color, int alpha) {
        int clampedAlpha = Math.max(0, Math.min(255, alpha));
        return (color & 0x00ffffff) | (clampedAlpha << 24);
    }

    private int dpToPx(float dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    private class ContentItemAdapter extends RecyclerView.Adapter<ContentItemAdapter.ViewHolder> {
        private final List<ContentProfile> data;

        private class ViewHolder extends RecyclerView.ViewHolder {
            private final ImageView ivIcon;
            private final TextView tvVersionName;
            private final TextView tvCategoryBadge;
            private final TextView tvVersionCode;
            private final TextView tvDescription;
            private final TextView tvSource;
            private final ImageButton ibMenu;
            private final ImageButton ibDownload;
            private final ProgressBar progressBar;

            private ViewHolder(@NonNull View view) {
                super(view);
                ivIcon = view.findViewById(R.id.IVIcon);
                tvVersionName = view.findViewById(R.id.TVVersionName);
                tvCategoryBadge = view.findViewById(R.id.TVCategoryBadge);
                tvVersionCode = view.findViewById(R.id.TVVersionCode);
                tvDescription = view.findViewById(R.id.TVDescription);
                tvSource = view.findViewById(R.id.TVSource);
                ibMenu = view.findViewById(R.id.BTMenu);
                ibDownload = view.findViewById(R.id.BTDownload);
                progressBar = view.findViewById(R.id.Progress);
            }
        }

        private ContentItemAdapter(List<ContentProfile> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.content_list_item, parent, false));
        }

        @Override
        public void onViewRecycled(@NonNull ViewHolder holder) {
            holder.ibMenu.setOnClickListener(null);
            holder.ibDownload.setOnClickListener(null);
            super.onViewRecycled(holder);
        }

        @SuppressLint("StringFormatInvalid")
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final ContentProfile profile = data.get(position);
            int iconId = switch (profile.type) {
                case CONTENT_TYPE_WINE, CONTENT_TYPE_PROTON -> R.drawable.icon_wine;
                case CONTENT_TYPE_DXVK,
                     CONTENT_TYPE_VKD3D,
                     CONTENT_TYPE_VULKAN_SDK,
                     CONTENT_TYPE_TURNIP_DRIVER,
                     CONTENT_TYPE_OPENGL_DRIVER,
                     CONTENT_TYPE_DGVOODOO -> R.drawable.icon_open;
                default -> R.drawable.icon_settings;
            };
            holder.ivIcon.setImageResource(iconId);
            int accentColor = resolveProfileAccentColor(profile);
            int secondaryColor = withAlpha(accentColor, isDarkMode ? 228 : 176);
            holder.ivIcon.setColorFilter(accentColor);

            holder.tvVersionName.setText(buildProfileTitleLine(profile));
            holder.tvVersionName.setTextColor(accentColor);

            String categoryBadgeText = profile.getDisplayCategory();
            if (categoryBadgeText == null || categoryBadgeText.trim().isEmpty()) {
                categoryBadgeText = getDisplayTypeLabel(profile.type);
            }
            holder.tvCategoryBadge.setText(categoryBadgeText);
            holder.tvCategoryBadge.setTextColor(accentColor);
            holder.tvCategoryBadge.setBackground(buildCategoryBadgeBackground(accentColor));
            holder.tvCategoryBadge.setVisibility(View.VISIBLE);

            holder.tvVersionCode.setText(buildProfileMetaLine(profile));
            holder.tvVersionCode.setTextColor(secondaryColor);
            holder.tvDescription.setText((profile.desc == null || profile.desc.trim().isEmpty()) ? getTypeLabel(profile.type) : profile.desc);
            holder.tvDescription.setTextColor(secondaryColor);
            holder.tvSource.setText(buildProfileSourceLine(profile));
            holder.tvSource.setTextColor(secondaryColor);

            holder.ibMenu.setVisibility(profile.remoteUrl == null ? View.VISIBLE : View.GONE);
            holder.ibMenu.setOnClickListener(v -> {
                PopupMenu selectionMenu = new PopupMenu(getContext(), holder.ibMenu);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) selectionMenu.setForceShowIcon(true);
                selectionMenu.inflate(R.menu.content_popup_menu);
                selectionMenu.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.content_info) {
                        new ContentInfoDialog(getContext(), profile).show();
                    } else if (itemId == R.id.remove_content) {
                        ContentDialog.confirm(getContext(), R.string.do_you_want_to_remove_this_content, () -> {
                            if (profile.type == ContentProfile.ContentType.CONTENT_TYPE_WINE
                                    || profile.type == ContentProfile.ContentType.CONTENT_TYPE_PROTON) {
                                ContainerManager containerManager = new ContainerManager(getContext());
                                for (Container container : containerManager.getContainers()) {
                                    if (container.getWineVersion().equals(ContentsManager.getEntryName(profile))) {
                                        ContentDialog.alert(getContext(), String.format(
                                                getString(R.string.unable_to_remove_content_since_container_using),
                                                container.getName()
                                        ), null);
                                        return;
                                    }
                                }
                            }
                            manager.removeContent(profile);
                            loadContentList();
                        });
                    }
                    return true;
                });
                selectionMenu.show();
            });

            holder.ibDownload.setVisibility((profile.remoteUrl != null) && (holder.progressBar.getVisibility() == View.GONE) ? View.VISIBLE : View.GONE);
            holder.ibDownload.setOnClickListener(v -> {
                holder.ibDownload.setVisibility(View.GONE);
                holder.progressBar.setVisibility(View.VISIBLE);
                ForensicLogger.logEvent(
                        getContext(),
                        "info",
                        "CONTENTS_PACKAGE_DOWNLOAD_START",
                        null,
                        "contents",
                        "download_start",
                        ForensicLogger.fields(
                                "type", profile.type.toString(),
                                "ver_name", profile.verName,
                                "ver_code", profile.verCode,
                                "url", profile.remoteUrl
                        )
                );

                new Thread(() -> {
                    long timestamp = System.currentTimeMillis();
                    File output = new File(requireContext().getCacheDir(), "temp_" + timestamp);
                    boolean downloaded = Downloader.downloadFile(profile.remoteUrl, output);
                    String expectedSha256 = normalizeSha256(profile.remoteSha256);
                    String actualSha256 = "";
                    boolean checksumVerified = false;
                    if (downloaded && !expectedSha256.isEmpty()) {
                        actualSha256 = normalizeSha256(Downloader.sha256Hex(output));
                        checksumVerified = expectedSha256.equals(actualSha256);
                        if (!checksumVerified) {
                            downloaded = false;
                            if (output.exists()) output.delete();
                        }
                    }
                    boolean checksumRequired = !expectedSha256.isEmpty();
                    boolean finalChecksumVerified = checksumVerified;
                    String finalActualSha256 = actualSha256;
                    String finalExpectedSha256 = expectedSha256;
                    boolean finalDownloaded = downloaded;
                    requireActivity().runOnUiThread(() -> {
                        holder.progressBar.setVisibility(View.GONE);
                        holder.ibDownload.setVisibility(View.VISIBLE);
                        if (!finalDownloaded) {
                            if (checksumRequired && !finalChecksumVerified) {
                                ForensicLogger.logEvent(
                                        getContext(),
                                        "warn",
                                        "CONTENTS_PACKAGE_DOWNLOAD_HASH_FAIL",
                                        null,
                                        "contents",
                                        "sha256_mismatch",
                                        ForensicLogger.fields(
                                                "type", profile.type.toString(),
                                                "ver_name", profile.verName,
                                                "url", profile.remoteUrl,
                                                "expected_sha256", finalExpectedSha256,
                                                "actual_sha256", finalActualSha256
                                        )
                                );
                                ContentDialog.alert(getContext(), R.string.content_cannot_be_trusted, null);
                                return;
                            }
                            ForensicLogger.logEvent(
                                    getContext(),
                                    "warn",
                                    "CONTENTS_PACKAGE_DOWNLOAD_FAIL",
                                    null,
                                    "contents",
                                    "download_failed",
                                    ForensicLogger.fields(
                                            "type", profile.type.toString(),
                                            "ver_name", profile.verName,
                                            "url", profile.remoteUrl
                                    )
                            );
                            ContentDialog.alert(getContext(), R.string.unable_to_install_content, null);
                            return;
                        }
                        ForensicLogger.logEvent(
                                getContext(),
                                "info",
                                "CONTENTS_PACKAGE_DOWNLOAD_DONE",
                                null,
                                "contents",
                                "download_complete",
                                ForensicLogger.fields(
                                        "type", profile.type.toString(),
                                        "ver_name", profile.verName,
                                        "file", output.getAbsolutePath(),
                                        "size_bytes", output.length(),
                                        "sha256", finalActualSha256,
                                        "sha256_expected", finalExpectedSha256,
                                        "sha256_verified", !checksumRequired || finalChecksumVerified
                                )
                        );
                        Intent intent = new Intent();
                        intent.setData(Uri.parse(output.getAbsolutePath()));
                        onActivityResult(MainActivity.OPEN_FILE_REQUEST_CODE, Activity.RESULT_OK, intent);
                    });
                }).start();
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
}
