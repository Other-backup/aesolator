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
import com.winlator.cmod.contents.DgVoodooManager;
import com.winlator.cmod.contents.Downloader;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.core.PreloaderDialog;
import com.winlator.cmod.core.SpinnerAdapters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    private static final String PREF_REMOTE_CACHE_SOURCE_SIGNATURE = "contents_remote_cache_source_signature";
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
    private Spinner sContentsSourceMode;
    private Spinner sContentsChannelMode;
    private Spinner sContentsArchMode;
    private View llContentsFilters;
    private boolean isDarkMode;
    private TextView tvContentsLaneScope;
    private String sourceMode;
    private String channelMode;
    private String archMode;
    private String preselectedDisplayCategory = "";
    private String[] sourceValues;
    private String[] channelValues;
    private String[] archValues;
    private boolean suppressFilterCallbacks;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        manager = new ContentsManager(getContext());
        manager.syncContents();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        sourceMode = sharedPreferences.getString("contents_source_mode", "wcphub");
        if (sourceMode == null || sourceMode.trim().isEmpty()) sourceMode = "wcphub";
        boolean migratedSourceMode = false;
        if ("all".equalsIgnoreCase(sourceMode) || "custom".equalsIgnoreCase(sourceMode)) {
            sourceMode = "wcphub";
            migratedSourceMode = true;
        }
        channelMode = sharedPreferences.getString("contents_channel_mode", "stable");
        if (channelMode == null || channelMode.trim().isEmpty()) channelMode = "stable";
        boolean migratedChannelMode = false;
        if ("all".equalsIgnoreCase(channelMode)) {
            channelMode = "stable";
            migratedChannelMode = true;
        }
        archMode = sharedPreferences.getString("contents_arch_mode", "all");
        if (archMode == null || archMode.trim().isEmpty()) archMode = "all";
        boolean migratedArchMode = false;
        if ("arm64".equalsIgnoreCase(archMode)) {
            archMode = "all";
            migratedArchMode = true;
        }
        if (migratedSourceMode || migratedChannelMode || migratedArchMode) {
            SharedPreferences.Editor migrationEditor = sharedPreferences.edit();
            if (migratedSourceMode) migrationEditor.putString("contents_source_mode", sourceMode);
            if (migratedChannelMode) migrationEditor.putString("contents_channel_mode", channelMode);
            if (migratedArchMode) migrationEditor.putString("contents_arch_mode", archMode);
            migrationEditor.apply();
        }
        preselectedDisplayCategory = sharedPreferences.getString("contents_preselected_display_category", "");
        if (preselectedDisplayCategory == null) preselectedDisplayCategory = "";
        sharedPreferences.edit().remove("contents_preselected_display_category").apply();
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
                if (currentContentType != ContentProfile.ContentType.CONTENT_TYPE_VULKAN_SDK
                        && preselectedDisplayCategory != null
                        && !preselectedDisplayCategory.trim().isEmpty()) {
                    preselectedDisplayCategory = "";
                }
                boolean sourceModeChanged = refreshSourceSpinnerForType();
                updateLaneScopeLabel();
                refreshTypeScopedFilterSpinners();
                updateFilterControlsVisibility();
                if (emptyText != null && recyclerView != null) {
                    if (sourceModeChanged) {
                        reloadRemoteContents();
                    } else {
                        loadContentList();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        applyPreferredContentTypeSelection();

        tvContentsLaneScope = layout.findViewById(R.id.TVContentsLaneScope);
        llContentsFilters = layout.findViewById(R.id.LLContentsFilters);
        sContentsSourceMode = layout.findViewById(R.id.SContentsSourceMode);
        sContentsChannelMode = layout.findViewById(R.id.SContentsChannelMode);
        sContentsArchMode = layout.findViewById(R.id.SContentsArchMode);

        channelValues = getResources().getStringArray(R.array.contents_channel_values);
        archValues = getResources().getStringArray(R.array.contents_arch_values);

        sContentsChannelMode.setAdapter(SpinnerAdapters.create(
                requireContext(),
                isDarkMode,
                getResources().getStringArray(R.array.contents_channel_entries)
        ));
        sContentsChannelMode.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);

        sContentsArchMode.setAdapter(SpinnerAdapters.create(
                requireContext(),
                isDarkMode,
                getResources().getStringArray(R.array.contents_arch_entries)
        ));
        sContentsArchMode.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
        applyFilterSpinnerTheme();

        refreshSourceSpinnerForType();
        refreshTypeScopedFilterSpinners();

        sContentsSourceMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressFilterCallbacks) return;
                updateFilterPreferencesFromUi();
                updateLaneScopeLabel();
                reloadRemoteContents();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        sContentsChannelMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressFilterCallbacks) return;
                updateFilterPreferencesFromUi();
                loadContentList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        sContentsArchMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressFilterCallbacks) return;
                updateFilterPreferencesFromUi();
                loadContentList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        updateLaneScopeLabel();
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
        spinner.setAdapter(SpinnerAdapters.create(requireContext(), isDarkMode, typeList));
        spinner.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
    }

    private void setSpinnerSelectionByValue(Spinner spinner, String[] values, String value, int fallbackIndex) {
        if (spinner == null || values == null || values.length == 0) return;
        int index = fallbackIndex;
        if (value != null) {
            for (int i = 0; i < values.length; i++) {
                if (value.equalsIgnoreCase(values[i])) {
                    index = i;
                    break;
                }
            }
        }
        if (index < 0 || index >= values.length) index = 0;
        spinner.setSelection(index, false);
    }

    private String getSpinnerSelectedValue(Spinner spinner, String[] values, String fallback) {
        if (spinner == null || values == null || values.length == 0) return fallback;
        int index = spinner.getSelectedItemPosition();
        if (index < 0 || index >= values.length) return fallback;
        return values[index];
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
            boolean locallyInstalled = profile != null && profile.locallyInstalled;
            if (!matchesSelectedSourceMode(profile) && !shouldBypassSourceFilter(profile)) {
                continue;
            }
            if (currentContentType == ContentProfile.ContentType.CONTENT_TYPE_VULKAN_SDK && profile.isBetaLike()) {
                continue;
            }
            if (!locallyInstalled && supportsChannelFilter(currentContentType)) {
                if ("stable".equalsIgnoreCase(channelMode) && profile.isBetaLike()) {
                    continue;
                }
                if ("nightly".equalsIgnoreCase(channelMode) && !profile.isBetaLike()) {
                    continue;
                }
            }
            if (supportsArchitectureFilters(currentContentType)
                    && archMode != null
                    && !"all".equalsIgnoreCase(archMode)) {
                String profileArch = resolveProfileArchTag(profile);
                if (!archMode.equalsIgnoreCase(profileArch)) continue;
            }
            if (currentContentType == ContentProfile.ContentType.CONTENT_TYPE_VULKAN_SDK
                    && !matchesPreselectedDisplayCategory(profile)) {
                continue;
            }
            filtered.add(profile);
        }
        sortVisibleProfiles(filtered);
        return filtered;
    }

    private boolean matchesSelectedSourceMode(ContentProfile profile) {
        if (profile == null) return false;
        String mode = sourceMode == null ? "wcphub" : sourceMode.trim().toLowerCase(Locale.US);
        String profileMode = resolveProfileSourceMode(profile);
        return mode.equals(profileMode);
    }

    private boolean shouldBypassSourceFilter(ContentProfile profile) {
        if (profile == null || !profile.locallyInstalled) return false;
        if (getAvailableSourceModesForType(currentContentType).size() > 1) return false;
        String resolvedSource = resolveProfileSourceMode(profile);
        return "remote".equalsIgnoreCase(resolvedSource)
                && (profile.sourceFeed == null || profile.sourceFeed.trim().isEmpty())
                && (profile.sourceRepo == null || profile.sourceRepo.trim().isEmpty());
    }

    private void sortVisibleProfiles(List<ContentProfile> profiles) {
        Collections.sort(profiles, (left, right) -> {
            boolean leftInstalled = isRuntimeInstalled(left);
            boolean rightInstalled = isRuntimeInstalled(right);
            if (leftInstalled != rightInstalled) {
                return leftInstalled ? -1 : 1;
            }

            int verCodeCompare = Integer.compare(right.verCode, left.verCode);
            if (verCodeCompare != 0) return verCodeCompare;

            int sourceCompare = Integer.compare(resolveProfileSourcePriority(right), resolveProfileSourcePriority(left));
            if (sourceCompare != 0) return sourceCompare;

            int channelCompare = Integer.compare(resolveProfileChannelPriority(right), resolveProfileChannelPriority(left));
            if (channelCompare != 0) return channelCompare;

            return buildProfileTitleLine(left).compareToIgnoreCase(buildProfileTitleLine(right));
        });
    }

    private int resolveProfileSourcePriority(ContentProfile profile) {
        String profileMode = resolveProfileSourceMode(profile);
        if ("aesolator".equals(profileMode)) return 300;
        if ("wcphub".equals(profileMode)) return 200;
        return 50;
    }

    private int resolveProfileChannelPriority(ContentProfile profile) {
        String channel = profile == null ? "" : profile.getChannel();
        if (ContentProfile.CHANNEL_STABLE.equalsIgnoreCase(channel)) return 30;
        if (ContentProfile.CHANNEL_BETA.equalsIgnoreCase(channel)) return 20;
        if (ContentProfile.CHANNEL_NIGHTLY.equalsIgnoreCase(channel)) return 10;
        return 0;
    }

    private String resolveProfileSourceMode(ContentProfile profile) {
        if (profile == null) return "remote";
        String sourceFeed = profile.sourceFeed == null ? "" : profile.sourceFeed.trim().toLowerCase(Locale.US);
        if (!sourceFeed.isEmpty()) return sourceFeed;

        String joined = (
                (profile.sourceRepo == null ? "" : profile.sourceRepo) + " " +
                        (profile.remoteUrl == null ? "" : profile.remoteUrl)
        ).toLowerCase(Locale.US);
        if (joined.contains("kosoymiki") || joined.contains("ae.solator") || joined.contains("aesolator")) return "aesolator";
        if (joined.contains("open-wine-components") || joined.contains("wcphub") || joined.contains("arihany") || joined.contains("winlatorwcphub")) {
            return "wcphub";
        }
        if (profile.locallyInstalled) {
            if (profile.type == ContentProfile.ContentType.CONTENT_TYPE_VULKAN_SDK
                    || profile.type == ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO) {
                return "aesolator";
            }
            return "wcphub";
        }
        return "remote";
    }

    private void updateFilterPreferencesFromUi() {
        sourceMode = getSpinnerSelectedValue(sContentsSourceMode, sourceValues, sourceMode);
        channelMode = getSpinnerSelectedValue(sContentsChannelMode, channelValues, channelMode);
        archMode = getSpinnerSelectedValue(sContentsArchMode, archValues, archMode);

        sharedPreferences.edit()
                .putString("contents_source_mode", sourceMode)
                .putString("contents_channel_mode", channelMode)
                .putString("contents_arch_mode", archMode)
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

    private void updateLaneScopeLabel() {
        if (tvContentsLaneScope == null) return;
        String lane = preselectedDisplayCategory == null ? "" : preselectedDisplayCategory.trim();
        String sourceLabel = getSourceEntryLabel(sourceMode);
        String sourceScope = "aesolator".equalsIgnoreCase(sourceMode)
                ? getString(R.string.contents_lane_scope_archive)
                : getString(R.string.contents_lane_scope_wcphub);
        if (currentContentType == ContentProfile.ContentType.CONTENT_TYPE_VULKAN_SDK && !lane.isEmpty()) {
            tvContentsLaneScope.setText(sourceLabel + " • " + sourceScope + " • " + getString(R.string.contents_lane_scope_format, lane));
        }
        else {
            tvContentsLaneScope.setText(sourceLabel + " • " + sourceScope);
        }
        tvContentsLaneScope.setVisibility(View.VISIBLE);
    }

    private boolean matchesPreselectedDisplayCategory(ContentProfile profile) {
        String lane = preselectedDisplayCategory == null ? "" : preselectedDisplayCategory.trim().toLowerCase(Locale.US);
        if (lane.isEmpty()) return true;
        String combined = (
                profile.getDisplayCategory() + " "
                        + (profile.verName == null ? "" : profile.verName) + " "
                        + (profile.desc == null ? "" : profile.desc) + " "
                        + (profile.releaseTag == null ? "" : profile.releaseTag)
        ).toLowerCase(Locale.US);
        if ("turnip".equals(lane)) return combined.contains("turnip");
        if ("opengl".equals(lane)) {
            return combined.contains("opengl") || combined.contains("zink") || combined.contains("gallium");
        }
        if ("vulkansdk".equals(lane) || "vulkan sdk".equals(lane) || "sdk".equals(lane)) {
            return combined.contains("vulkan") || combined.contains("sdk");
        }
        return combined.contains(lane);
    }

    private boolean supportsArchitectureFilters(ContentProfile.ContentType type) {
        return type == ContentProfile.ContentType.CONTENT_TYPE_WINE
                || type == ContentProfile.ContentType.CONTENT_TYPE_PROTON
                || type == ContentProfile.ContentType.CONTENT_TYPE_DXVK
                || type == ContentProfile.ContentType.CONTENT_TYPE_VKD3D
                || type == ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO;
    }

    private boolean supportsChannelFilter(ContentProfile.ContentType type) {
        return type == ContentProfile.ContentType.CONTENT_TYPE_BOX64
                || type == ContentProfile.ContentType.CONTENT_TYPE_WOWBOX64
                || type == ContentProfile.ContentType.CONTENT_TYPE_FEXCORE;
    }

    private boolean refreshSourceSpinnerForType() {
        if (getContext() == null || sContentsSourceMode == null) return false;
        String previousSourceMode = sourceMode == null ? "" : sourceMode;

        sourceValues = getSourceValuesForType(currentContentType);
        String[] sourceEntries = getSourceEntriesForType(currentContentType);

        suppressFilterCallbacks = true;
        try {
            sContentsSourceMode.setAdapter(SpinnerAdapters.create(requireContext(), isDarkMode, sourceEntries));
            sContentsSourceMode.setPopupBackgroundResource(
                    isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background
            );
            applyFilterSpinnerTheme();

            if (!containsIgnoreCase(sourceValues, sourceMode)) {
                sourceMode = sourceValues.length > 0 ? sourceValues[0] : "wcphub";
            }
            setSpinnerSelectionByValue(sContentsSourceMode, sourceValues, sourceMode, 0);
            sContentsSourceMode.setEnabled(sourceValues.length > 1);
            sContentsSourceMode.setAlpha(sourceValues.length > 1 ? 1.0f : 0.92f);
            sharedPreferences.edit().putString("contents_source_mode", sourceMode).apply();
        } finally {
            suppressFilterCallbacks = false;
        }
        return !previousSourceMode.equalsIgnoreCase(sourceMode);
    }

    private void updateFilterControlsVisibility() {
        if (!isAdded()) return;
        boolean showArchFilters = supportsArchitectureFilters(currentContentType) && currentSourceHasMultipleArchitectureCandidates();
        boolean showChannelFilter = supportsChannelFilter(currentContentType) && currentSourceHasNightlyCandidates();

        if (sContentsArchMode != null) sContentsArchMode.setVisibility(showArchFilters ? View.VISIBLE : View.GONE);
        if (sContentsChannelMode != null) sContentsChannelMode.setVisibility(showChannelFilter ? View.VISIBLE : View.GONE);

        boolean showFiltersCard = showArchFilters || showChannelFilter;
        if (llContentsFilters != null) llContentsFilters.setVisibility(showFiltersCard ? View.VISIBLE : View.GONE);
    }

    private void applyFilterSpinnerTheme() {
        SpinnerAdapters.applySurface(sContentType, isDarkMode);
        SpinnerAdapters.applySurface(sContentsSourceMode, isDarkMode);
        SpinnerAdapters.applySurface(sContentsChannelMode, isDarkMode);
        SpinnerAdapters.applySurface(sContentsArchMode, isDarkMode);
    }

    private void refreshTypeScopedFilterSpinners() {
        if (getContext() == null || sContentsChannelMode == null || sContentsArchMode == null) return;

        channelValues = getChannelValuesForType(currentContentType);
        archValues = getArchValuesForType(currentContentType);

        String[] channelEntries = getChannelEntriesForType(currentContentType);
        String[] archEntries = getArchEntriesForType(currentContentType);

        suppressFilterCallbacks = true;
        try {
            sContentsChannelMode.setAdapter(SpinnerAdapters.create(requireContext(), isDarkMode, channelEntries));
            sContentsArchMode.setAdapter(SpinnerAdapters.create(requireContext(), isDarkMode, archEntries));

            int popupBackground = isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background;
            sContentsChannelMode.setPopupBackgroundResource(popupBackground);
            sContentsArchMode.setPopupBackgroundResource(popupBackground);
            applyFilterSpinnerTheme();

            if (!containsIgnoreCase(channelValues, channelMode)) channelMode = channelValues.length > 0 ? channelValues[0] : "stable";
            if (!containsIgnoreCase(archValues, archMode)) archMode = archValues.length > 0 ? archValues[0] : "all";

            setSpinnerSelectionByValue(sContentsChannelMode, channelValues, channelMode, 0);
            setSpinnerSelectionByValue(sContentsArchMode, archValues, archMode, 0);
            sharedPreferences.edit()
                    .putString("contents_channel_mode", channelMode)
                    .putString("contents_arch_mode", archMode)
                    .apply();
        } finally {
            suppressFilterCallbacks = false;
        }
    }

    private boolean containsIgnoreCase(String[] values, String value) {
        if (values == null || values.length == 0 || value == null) return false;
        for (String item : values) {
            if (value.equalsIgnoreCase(item)) return true;
        }
        return false;
    }

    private String[] getChannelEntriesForType(ContentProfile.ContentType type) {
        if (!supportsChannelFilter(type) || !currentSourceHasNightlyCandidates()) {
            return new String[]{getString(R.string.contents_channel_mainline_release)};
        }
        return getResources().getStringArray(R.array.contents_channel_entries);
    }

    private String[] getChannelValuesForType(ContentProfile.ContentType type) {
        if (!supportsChannelFilter(type) || !currentSourceHasNightlyCandidates()) {
            return new String[]{"stable"};
        }
        return getResources().getStringArray(R.array.contents_channel_values);
    }

    private String[] getArchEntriesForType(ContentProfile.ContentType type) {
        if (!supportsArchitectureFilters(type) || !currentSourceHasMultipleArchitectureCandidates()) {
            return new String[]{getString(R.string.contents_arch_all_lanes)};
        }
        if (type == ContentProfile.ContentType.CONTENT_TYPE_DXVK
                || type == ContentProfile.ContentType.CONTENT_TYPE_VKD3D) {
            return getResources().getStringArray(R.array.contents_arch_entries_dxvk_vkd3d);
        }
        if (type == ContentProfile.ContentType.CONTENT_TYPE_WINE
                || type == ContentProfile.ContentType.CONTENT_TYPE_PROTON) {
            return getResources().getStringArray(R.array.contents_arch_entries_wine_proton);
        }
        return getResources().getStringArray(R.array.contents_arch_entries);
    }

    private String[] getSourceEntriesForType(ContentProfile.ContentType type) {
        ArrayList<String> entries = new ArrayList<>();
        for (String sourceValue : getAvailableSourceModesForType(type)) {
            entries.add(getSourceEntryLabel(sourceValue));
        }
        return entries.toArray(new String[0]);
    }

    private String[] getSourceValuesForType(ContentProfile.ContentType type) {
        ArrayList<String> values = getAvailableSourceModesForType(type);
        return values.toArray(new String[0]);
    }

    private ArrayList<String> getAvailableSourceModesForType(ContentProfile.ContentType type) {
        ArrayList<String> ordered = new ArrayList<>();
        if (type == ContentProfile.ContentType.CONTENT_TYPE_DXVK
                || type == ContentProfile.ContentType.CONTENT_TYPE_VKD3D) {
            ordered.add("wcphub");
            ordered.add("aesolator");
            return ordered;
        }
        if (type == ContentProfile.ContentType.CONTENT_TYPE_VULKAN_SDK
                || type == ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO) {
            ordered.add("aesolator");
            return ordered;
        }

        ordered.add("wcphub");
        return ordered;
    }

    private String getSourceEntryLabel(String sourceValue) {
        if ("aesolator".equalsIgnoreCase(sourceValue)) return getString(R.string.contents_source_aesolator_mainline);
        if ("wcphub".equalsIgnoreCase(sourceValue)) return getString(R.string.contents_source_wcphub_feed);
        return getString(R.string.contents_source_unknown);
    }

    private String[] getArchValuesForType(ContentProfile.ContentType type) {
        if (!supportsArchitectureFilters(type) || !currentSourceHasMultipleArchitectureCandidates()) {
            return new String[]{"all"};
        }
        if (type == ContentProfile.ContentType.CONTENT_TYPE_DXVK
                || type == ContentProfile.ContentType.CONTENT_TYPE_VKD3D) {
            return getResources().getStringArray(R.array.contents_arch_values_dxvk_vkd3d);
        }
        if (type == ContentProfile.ContentType.CONTENT_TYPE_WINE
                || type == ContentProfile.ContentType.CONTENT_TYPE_PROTON) {
            return getResources().getStringArray(R.array.contents_arch_values_wine_proton);
        }
        return getResources().getStringArray(R.array.contents_arch_values);
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

    private boolean isArm64Profile(ContentProfile profile) {
        if (profile == null) return false;
        String combined = (
                (profile.verName == null ? "" : profile.verName) + " " +
                        (profile.desc == null ? "" : profile.desc) + " " +
                        (profile.remoteUrl == null ? "" : profile.remoteUrl) + " " +
                        (profile.releaseTag == null ? "" : profile.releaseTag)
        ).toLowerCase(Locale.US);
        return combined.contains("arm64") || combined.contains("aarch64");
    }

    private boolean isNativeProfile(ContentProfile profile) {
        if (profile == null) return false;
        if (profile.type != ContentProfile.ContentType.CONTENT_TYPE_DXVK
                && profile.type != ContentProfile.ContentType.CONTENT_TYPE_VKD3D) {
            return false;
        }
        String combined = (
                (profile.verName == null ? "" : profile.verName) + " " +
                        (profile.desc == null ? "" : profile.desc) + " " +
                        (profile.remoteUrl == null ? "" : profile.remoteUrl) + " " +
                        (profile.releaseTag == null ? "" : profile.releaseTag)
        ).toLowerCase(Locale.US);
        return combined.contains("native");
    }

    private String resolveProfileArchTag(ContentProfile profile) {
        if (isArm64EcProfile(profile)) return "arm64ec";
        if (isX64Profile(profile)) return "x86_64";
        if (isArm64Profile(profile)) return "arm64";
        if (profile != null) {
            if (profile.type == ContentProfile.ContentType.CONTENT_TYPE_DXVK
                    || profile.type == ContentProfile.ContentType.CONTENT_TYPE_VKD3D
                    || profile.type == ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO
                    || profile.type == ContentProfile.ContentType.CONTENT_TYPE_WINE
                    || profile.type == ContentProfile.ContentType.CONTENT_TYPE_PROTON) {
                return "x86_64";
            }
        }
        return "generic";
    }

    private boolean currentSourceHasMultipleArchitectureCandidates() {
        List<ContentProfile> profiles = manager.getProfiles(currentContentType);
        if (profiles == null) return false;
        HashSet<String> archTags = new HashSet<>();
        for (ContentProfile profile : profiles) {
            if (profile == null || profile.locallyInstalled) continue;
            if (!matchesSelectedSourceMode(profile)) continue;
            String archTag = resolveProfileArchTag(profile);
            if ("generic".equalsIgnoreCase(archTag)) continue;
            archTags.add(archTag.toLowerCase(Locale.US));
            if (archTags.size() > 1) return true;
        }
        return false;
    }

    private boolean currentSourceHasNightlyCandidates() {
        List<ContentProfile> profiles = manager.getProfiles(currentContentType);
        if (profiles == null) return false;
        for (ContentProfile profile : profiles) {
            if (profile == null || profile.locallyInstalled) continue;
            if (!matchesSelectedSourceMode(profile)) continue;
            if (profile.isBetaLike()) return true;
        }
        return false;
    }

    private boolean isRuntimeInstalled(ContentProfile profile) {
        if (profile == null) return false;
        if (profile.isInstalledLocally()) return true;
        if (profile.type == ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO) {
            DgVoodooManager dgVoodooManager = new DgVoodooManager(requireContext());
            if (!dgVoodooManager.isInstalled()) return false;
            String profileArch = resolveProfileArchTag(profile);
            if ("generic".equalsIgnoreCase(profileArch)) return true;
            for (String installedArch : dgVoodooManager.getInstalledArchitectures()) {
                if (matchesDgVoodooArchitecture(profileArch, installedArch)) return true;
            }
            return false;
        }
        if (profile.type == ContentProfile.ContentType.CONTENT_TYPE_VULKAN_SDK) {
            return isVulkanSdkRuntimePresent();
        }
        return false;
    }

    private boolean matchesDgVoodooArchitecture(String profileArch, String installedArch) {
        String normalizedProfile = profileArch == null ? "" : profileArch.trim().toLowerCase(Locale.US);
        String normalizedInstalled = installedArch == null ? "" : installedArch.trim().toLowerCase(Locale.US);
        if ("x86_64".equals(normalizedProfile)) {
            return "x64".equals(normalizedInstalled) || "x86_64".equals(normalizedInstalled);
        }
        return normalizedProfile.equals(normalizedInstalled);
    }

    private boolean isVulkanSdkRuntimePresent() {
        File shareRoot = new File(requireContext().getFilesDir(), "imagefs/usr/share");
        File vulkanDir = new File(shareRoot, "vulkan");
        File sdkDir = new File(shareRoot, "vulkan-sdk");
        return (vulkanDir.isDirectory() && hasChildren(vulkanDir))
                || (sdkDir.isDirectory() && hasChildren(sdkDir));
    }

    private boolean hasChildren(File dir) {
        String[] list = dir.list();
        return list != null && list.length > 0;
    }

    private void reloadRemoteContents() {
        final String selectedSourceMode = sourceMode == null ? "wcphub" : sourceMode.trim().toLowerCase(Locale.US);
        final String sourceSignature = buildSourceSignature(selectedSourceMode);
        final String cachedSourceSignature = sharedPreferences.getString(PREF_REMOTE_CACHE_SOURCE_SIGNATURE, "");
        ForensicLogger.logEvent(
                getContext(),
                "info",
                "CONTENTS_FEED_REFRESH_START",
                null,
                "contents",
                "refresh_remote_feeds",
                ForensicLogger.fields(
                        "source_mode", selectedSourceMode,
                        "channel_mode", channelMode,
                        "arch_mode", archMode
                )
        );
        new Thread(() -> {
            try {
                ArrayList<String> payloads = new ArrayList<>();
                HashSet<String> sources = new HashSet<>();
                LinkedHashMap<String, String> payloadByUrl = new LinkedHashMap<>();
                for (String sourceUrl : resolveSelectedSourceUrls(selectedSourceMode)) {
                    String payload = addRemoteFeed(payloads, sources, sourceUrl);
                    if (payload != null && !payload.trim().isEmpty()) {
                        payloadByUrl.put(sourceUrl, payload);
                    }
                }

                if (!isAdded() || getActivity() == null) return;
                if (payloads.isEmpty()) {
                    getActivity().runOnUiThread(() -> {
                        String cached = sharedPreferences.getString(PREF_REMOTE_CACHE_JSON, "[]");
                        boolean sourceChanged = !sourceSignature.equalsIgnoreCase(cachedSourceSignature);
                        boolean useCached = !sourceChanged && cached != null && !cached.trim().isEmpty() && !"[]".equals(cached.trim());
                        manager.setRemoteProfiles(useCached ? cached : "[]");
                        ForensicLogger.logEvent(
                                getContext(),
                                useCached ? "warn" : "warn",
                                "CONTENTS_FEED_REFRESH_FALLBACK",
                                null,
                                "contents",
                                useCached ? "all_sources_failed_use_cached" : "all_sources_failed_empty",
                                ForensicLogger.fields(
                                        "source_mode", selectedSourceMode,
                                        "sources_enabled", sources.size(),
                                        "cached_used", useCached
                                )
                        );
                        refreshTypeScopedFilterSpinners();
                        updateFilterControlsVisibility();
                        loadContentList();
                    });
                    return;
                }

                String merged = mergeJsonArrays(payloads);
                getActivity().runOnUiThread(() -> {
                    sharedPreferences.edit()
                            .putString(PREF_REMOTE_CACHE_JSON, merged)
                            .putString(PREF_REMOTE_CACHE_SOURCE_SIGNATURE, sourceSignature)
                            .apply();
                    if ("aesolator".equals(selectedSourceMode)) {
                        manager.setArchiveRemoteProfiles(merged);
                    } else if ("wcphub".equals(selectedSourceMode)) {
                        manager.setHubRemoteProfiles(merged);
                    } else {
                        manager.setRemoteProfiles(merged);
                    }
                    ForensicLogger.logEvent(
                            getContext(),
                            "info",
                            "CONTENTS_FEED_REFRESH_DONE",
                            null,
                            "contents",
                            "refresh_complete",
                            ForensicLogger.fields(
                                    "source_mode", selectedSourceMode,
                                    "sources_polled", sources.size(),
                                    "payloads_received", payloads.size(),
                                    "merged_size", merged.length()
                            )
                    );
                    refreshTypeScopedFilterSpinners();
                    updateFilterControlsVisibility();
                    loadContentList();
                });
            } catch (Exception ignored) {
                if (!isAdded() || getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    String cached = sharedPreferences.getString(PREF_REMOTE_CACHE_JSON, "[]");
                    boolean sourceChanged = !sourceSignature.equalsIgnoreCase(cachedSourceSignature);
                    boolean useCached = !sourceChanged && cached != null && !cached.trim().isEmpty() && !"[]".equals(cached.trim());
                    manager.setRemoteProfiles(useCached ? cached : "[]");
                    ForensicLogger.logEvent(
                            getContext(),
                            "warn",
                            "CONTENTS_FEED_REFRESH_EXCEPTION",
                            null,
                            "contents",
                            useCached ? "refresh_exception_use_cached" : "refresh_exception_empty",
                            ForensicLogger.fields(
                                    "source_mode", selectedSourceMode,
                                    "cached_used", useCached
                            )
                    );
                    refreshTypeScopedFilterSpinners();
                    updateFilterControlsVisibility();
                    loadContentList();
                });
            }
        }).start();
    }

    private List<String> resolveSelectedSourceUrls(String selectedSourceMode) {
        ArrayList<String> urls = new ArrayList<>();
        if ("aesolator".equals(selectedSourceMode)
                && (currentContentType == ContentProfile.ContentType.CONTENT_TYPE_DXVK
                || currentContentType == ContentProfile.ContentType.CONTENT_TYPE_VKD3D
                || currentContentType == ContentProfile.ContentType.CONTENT_TYPE_VULKAN_SDK
                || currentContentType == ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO)) {
            urls.add(ContentsManager.REMOTE_WINE_PROTON_OVERLAY);
            return urls;
        }
        if ("wcphub".equals(selectedSourceMode)) {
            urls.add(ContentsManager.REMOTE_PROFILES);
            return urls;
        }
        urls.add(ContentsManager.REMOTE_PROFILES);
        return urls;
    }

    private String buildSourceSignature(String selectedSourceMode) {
        String normalized = selectedSourceMode == null ? "wcphub" : selectedSourceMode.trim().toLowerCase(Locale.US);
        return normalized;
    }

    private String addRemoteFeed(List<String> payloads, HashSet<String> seenSources, @Nullable String url) {
        if (url == null) return null;
        String normalized = url.trim();
        if (normalized.isEmpty() || seenSources.contains(normalized) || !isAllowedFeedUrl(normalized)) return null;
        seenSources.add(normalized);
        String json = Downloader.downloadString(normalized);
        if (json != null && !json.trim().isEmpty()) {
            String enriched = injectFeedSourceMetadata(json, normalized);
            payloads.add(enriched);
            return enriched;
        }
        return null;
    }

    private String injectFeedSourceMetadata(String json, String feedUrl) {
        try {
            JSONArray array = new JSONArray(json);
            String sourceId = deriveFeedSourceId(feedUrl);
            String sourceLabel = deriveFeedSourceLabel(feedUrl);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) continue;
                if (object.optString(ContentProfile.MARK_SOURCE_REPO, "").trim().isEmpty()) {
                    object.put(ContentProfile.MARK_SOURCE_REPO, sourceLabel);
                }
                if (object.optString(ContentProfile.MARK_SOURCE_FEED, "").trim().isEmpty()) {
                    object.put(ContentProfile.MARK_SOURCE_FEED, sourceId);
                }
                if (object.optString(ContentProfile.MARK_SOURCE_LABEL, "").trim().isEmpty()) {
                    object.put(ContentProfile.MARK_SOURCE_LABEL, sourceLabel);
                }
                if (object.optString(ContentProfile.MARK_RELEASE_TAG, "").trim().isEmpty()) {
                    object.put(ContentProfile.MARK_RELEASE_TAG, deriveFeedReleaseTag(feedUrl));
                }
            }
            return array.toString();
        } catch (Exception ignored) {
            return json;
        }
    }

    private String deriveFeedSourceLabel(String feedUrl) {
        String sourceId = deriveFeedSourceId(feedUrl);
        if ("aesolator".equals(sourceId)) return getString(R.string.contents_source_aesolator);
        if ("wcphub".equals(sourceId)) return getString(R.string.contents_source_wcphub);
        try {
            URI uri = new URI(feedUrl);
            String host = uri.getHost() == null ? "" : uri.getHost().trim().toLowerCase(Locale.US);
            String path = uri.getPath() == null ? "" : uri.getPath().trim();
            if ("raw.githubusercontent.com".equals(host) && !path.isEmpty()) {
                String[] parts = path.split("/");
                if (parts.length >= 3 && !parts[1].isEmpty() && !parts[2].isEmpty()) {
                    return parts[1] + "/" + parts[2];
                }
            }
            if (!host.isEmpty()) return host;
        } catch (Exception ignored) {
        }
        return "remote-feed";
    }

    private String deriveFeedSourceId(String feedUrl) {
        String lower = feedUrl == null ? "" : feedUrl.trim().toLowerCase(Locale.US);
        if (lower.contains("arihany/winlatorwcphub") || lower.contains("wcphub")) return "wcphub";
        if (lower.contains("kosoymiki/wcp-runtime-lanes")
                || lower.contains("kosoymiki/wcp-graphics-lanes")
                || lower.contains("ae.solator")
                || lower.contains("aesolator")) {
            return "aesolator";
        }
        return "remote";
    }

    private String deriveFeedReleaseTag(String feedUrl) {
        String lower = feedUrl == null ? "" : feedUrl.toLowerCase(Locale.US);
        if (lower.contains("nightly")) return ContentProfile.CHANNEL_NIGHTLY;
        if (lower.contains("beta") || lower.contains("rc")) return ContentProfile.CHANNEL_BETA;
        return ContentProfile.CHANNEL_STABLE;
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
        String sourceFeed = object.optString(ContentProfile.MARK_SOURCE_FEED, "").toLowerCase(Locale.US);
        String sourceRepo = object.optString(ContentProfile.MARK_SOURCE_REPO, "").toLowerCase(Locale.US);
        String remoteUrl = object.optString("remoteUrl", "").toLowerCase(Locale.US);
        String joined = sourceFeed + " " + sourceRepo + " " + remoteUrl;
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
        String normalizedType = object.optString("type", "").trim().toLowerCase(Locale.US);
        String combined = (
                object.optString("verName", "") + " "
                        + object.optString("description", "") + " "
                        + object.optString("remoteUrl", "") + " "
                        + object.optString(ContentProfile.MARK_RELEASE_TAG, "")
        ).toLowerCase(Locale.US);
        if (("dxvk".equals(normalizedType) || "vkd3d".equals(normalizedType)) && combined.contains("native")) return "x86_64";
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
        if (profile == null) return getString(R.string.contents_package_remote_generic);
        String sourceMode = resolveProfileSourceMode(profile);
        if ("aesolator".equals(sourceMode)) return getString(R.string.contents_source_aesolator);
        if ("wcphub".equals(sourceMode)) return getString(R.string.contents_source_wcphub);
        if (profile.sourceLabel != null && !profile.sourceLabel.trim().isEmpty()) {
            return profile.sourceLabel.trim();
        }
        if (profile.sourceRepo != null && !profile.sourceRepo.trim().isEmpty()) {
            String repo = profile.sourceRepo.trim().toLowerCase(Locale.US);
            if (repo.contains("wcp-runtime-lanes")) return getString(R.string.contents_source_runtime_lanes);
            if (repo.contains("wcp-graphics-lanes")) return getString(R.string.contents_source_graphics_lanes);
        }
        if (profile.remoteUrl == null || profile.remoteUrl.trim().isEmpty()) {
            return getString(R.string.contents_package_local);
        }
        try {
            Uri uri = Uri.parse(profile.remoteUrl);
            String host = uri.getHost();
            if (host != null && !host.isEmpty()) {
                return getString(R.string.contents_package_remote_host, host);
            }
        } catch (Exception ignored) {
        }
        return getString(R.string.contents_package_remote_generic);
    }

    private String buildProfileTitleLine(ContentProfile profile) {
        String versionName = profile.verName != null ? profile.verName.trim() : "";
        if (!versionName.isEmpty()) return versionName;
        return getDisplayTypeLabel(profile.type);
    }

    private String buildProfileMetaLine(ContentProfile profile) {
        StringBuilder meta = new StringBuilder(getString(R.string.version_code) + ": " + profile.verCode);
        String archTag = resolveProfileArchTag(profile);
        if (!"generic".equals(archTag)) {
            meta.append(" • ").append(resolveArchLabel(archTag));
        }
        if (profile.type == ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO) {
            DgVoodooManager dgVoodooManager = new DgVoodooManager(requireContext());
            String dgArch = dgVoodooManager.getInstalledArchitectureSummary();
            if (dgArch != null && !dgArch.trim().isEmpty() && !"-".equals(dgArch)) {
                meta.append(" • ")
                        .append(getString(R.string.contents_package_dgvoodoo_arch, dgArch.trim().toLowerCase(Locale.US)));
            }
        }
        boolean runtimeInstalled = isRuntimeInstalled(profile);
        if (runtimeInstalled) meta.append(" • ").append(getString(R.string.graphics_center_installed_action).toLowerCase(Locale.US));
        return meta.toString();
    }

    private String buildProfileSourceLine(ContentProfile profile) {
        StringBuilder line = new StringBuilder(resolveChannelLabel(profile));
        String sourceLabel = getSourceLabel(profile);
        String sourceDetail = getSourceDetailLabel(profile);
        String tag = profile.releaseTag != null ? profile.releaseTag.trim() : "";

        if (!sourceLabel.isEmpty()) {
            line.append(" • ").append(sourceLabel);
        }
        if (!sourceDetail.isEmpty() && !sourceDetail.equalsIgnoreCase(sourceLabel)) {
            line.append(" • ").append(sourceDetail);
        }

        if (!tag.isEmpty()
                && !tag.equalsIgnoreCase(profile.getChannel())
                && !tag.equalsIgnoreCase(ContentProfile.CHANNEL_STABLE)
                && !tag.equalsIgnoreCase(ContentProfile.CHANNEL_NIGHTLY)) {
            line.append(" • ").append(tag);
        }
        return line.toString();
    }

    private String getSourceDetailLabel(ContentProfile profile) {
        if (profile == null) return "";
        String sourceMode = resolveProfileSourceMode(profile);
        if ("wcphub".equals(sourceMode)) return "";
        if ("aesolator".equals(sourceMode) && profile.sourceRepo != null) {
            String repo = profile.sourceRepo.trim().toLowerCase(Locale.US);
            if (repo.contains("wcp-runtime-lanes")) return getString(R.string.contents_source_archive_runtime_lane);
            if (repo.contains("wcp-graphics-lanes")) return getString(R.string.contents_source_archive_graphics_lane);
        }
        if (profile.sourceRepo == null) return "";
        String repo = profile.sourceRepo.trim();
        if (repo.isEmpty()) return "";
        String lowerRepo = repo.toLowerCase(Locale.US);
        if (lowerRepo.contains("wcp-runtime-lanes")) return getString(R.string.contents_source_archive_runtime_lane);
        if (lowerRepo.contains("wcp-graphics-lanes")) return getString(R.string.contents_source_archive_graphics_lane);
        if (lowerRepo.contains("winlatorwcphub") || lowerRepo.contains("arihany")) return getString(R.string.contents_source_wcphub_lane);
        return repo;
    }

    private String resolveChannelLabel(ContentProfile profile) {
        String channel = profile.getChannel();
        if (ContentProfile.CHANNEL_NIGHTLY.equalsIgnoreCase(channel)) {
            return getString(R.string.contents_channel_nightly);
        }
        return getString(R.string.contents_channel_mainline);
    }

    private String getDisplayTypeLabel(ContentProfile.ContentType type) {
        return getTypeLabel(type);
    }

    private String resolveArchLabel(String archTag) {
        if ("arm64ec".equalsIgnoreCase(archTag)) return getString(R.string.contents_arch_arm64ec_runtime);
        if ("x86_64".equalsIgnoreCase(archTag)) return getString(R.string.contents_arch_x64_runtime);
        return archTag;
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
                case CONTENT_TYPE_WINE, CONTENT_TYPE_PROTON -> R.drawable.ae_icon_package;
                case CONTENT_TYPE_DXVK,
                     CONTENT_TYPE_VKD3D,
                     CONTENT_TYPE_VULKAN_SDK,
                     CONTENT_TYPE_TURNIP_DRIVER,
                     CONTENT_TYPE_OPENGL_DRIVER,
                     CONTENT_TYPE_DGVOODOO -> R.drawable.ae_icon_open;
                default -> R.drawable.ae_icon_settings;
            };
            holder.ivIcon.setImageResource(iconId);
            int accentColor = resolveProfileAccentColor(profile);
            int secondaryColor = withAlpha(accentColor, isDarkMode ? 228 : 176);
            holder.ivIcon.setColorFilter(accentColor);
            GradientDrawable itemBackground = new GradientDrawable();
            itemBackground.setShape(GradientDrawable.RECTANGLE);
            itemBackground.setCornerRadius(dpToPx(12f));
            itemBackground.setColor(withAlpha(accentColor, isDarkMode ? 56 : 26));
            itemBackground.setStroke(dpToPx(1f), withAlpha(accentColor, isDarkMode ? 210 : 138));
            holder.itemView.setBackground(itemBackground);

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
