package com.winlator.cmod;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import com.winlator.cmod.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.navigation.NavigationView;
import com.winlator.cmod.contentdialog.ContentDialog;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.core.PreloaderDialog;
import com.winlator.cmod.contents.AdrenotoolsManager;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.contents.Downloader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class AdrenotoolsFragment extends Fragment {
    private static final String PREF_REMOTE_CACHE_JSON = "contents_remote_cache_json";
    private static final String PREF_REMOTE_CACHE_SOURCE_SIGNATURE = "contents_remote_cache_source_signature";
    private static final String LANE_TURNIP = "turnip";
    private static final String LANE_OPENGL = "opengl";
    private static final long REMOTE_FEED_REFRESH_INTERVAL_MS = 180_000L;

    private AdrenotoolsManager adrenotoolsManager;
    private SharedPreferences sharedPreferences;
    private RecyclerView recyclerView;
    private RecyclerView rvGraphicsFeed;
    private View rootView;
    private TextView tvGraphicsCenterStatus;
    private TextView tvGraphicsFeedEmpty;
    private Spinner sGraphicsFeedSourceMode;
    private Spinner sGraphicsFeedChannelMode;
    private Spinner sGraphicsFeedArchMode;
    private int selectedLaneButtonId = R.id.BTLaneTurnip;
    private String selectedLane = LANE_TURNIP;
    private String sourceMode = "aesolator";
    private String channelMode = "stable";
    private String archMode = "all";
    private String[] sourceValues;
    private String[] channelValues;
    private String[] archValues;
    private long lastRemoteFeedRefreshMs = 0L;
    private final HashSet<String> installingEntries = new HashSet<>();
    
    @Override 
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.adrenotoolsManager = new AdrenotoolsManager(getActivity());
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        sourceMode = sharedPreferences.getString("contents_source_mode", "aesolator");
        channelMode = sharedPreferences.getString("contents_channel_mode", "stable");
        archMode = sharedPreferences.getString("contents_arch_mode", "all");
        if (sourceMode == null || sourceMode.trim().isEmpty()) sourceMode = "aesolator";
        if (channelMode == null || channelMode.trim().isEmpty()) channelMode = "stable";
        if (archMode == null || archMode.trim().isEmpty()) archMode = "all";
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup layout = (ViewGroup)inflater.inflate(R.layout.adrenotools_fragment, container, false);
        rootView = layout;

        recyclerView = layout.findViewById(R.id.RecyclerView);
        tvGraphicsCenterStatus = layout.findViewById(R.id.TVGraphicsCenterStatus);
        rvGraphicsFeed = layout.findViewById(R.id.RVGraphicsFeed);
        tvGraphicsFeedEmpty = layout.findViewById(R.id.TVGraphicsFeedEmpty);
        sGraphicsFeedSourceMode = layout.findViewById(R.id.SGraphicsFeedSourceMode);
        sGraphicsFeedChannelMode = layout.findViewById(R.id.SGraphicsFeedChannelMode);
        sGraphicsFeedArchMode = layout.findViewById(R.id.SGraphicsFeedArchMode);

        sourceValues = getResources().getStringArray(R.array.contents_source_values);
        channelValues = getResources().getStringArray(R.array.contents_channel_values);
        archValues = getResources().getStringArray(R.array.contents_arch_values);

        sGraphicsFeedSourceMode.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                getResources().getStringArray(R.array.contents_source_entries)
        ));
        sGraphicsFeedChannelMode.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                getResources().getStringArray(R.array.contents_channel_entries)
        ));
        sGraphicsFeedArchMode.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                getResources().getStringArray(R.array.contents_arch_entries)
        ));
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        int popupBackground = isDarkMode ? R.drawable.content_dialog_background_dark : R.drawable.content_dialog_background;
        sGraphicsFeedSourceMode.setPopupBackgroundResource(popupBackground);
        sGraphicsFeedChannelMode.setPopupBackgroundResource(popupBackground);
        sGraphicsFeedArchMode.setPopupBackgroundResource(popupBackground);
        setSpinnerSelectionByValue(sGraphicsFeedSourceMode, sourceValues, sourceMode, 0);
        setSpinnerSelectionByValue(sGraphicsFeedChannelMode, channelValues, channelMode, 0);
        setSpinnerSelectionByValue(sGraphicsFeedArchMode, archValues, archMode, 0);

        AdapterView.OnItemSelectedListener feedFilterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateFeedFilterPreferencesFromUi();
                refreshGraphicsFeed();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        sGraphicsFeedSourceMode.setOnItemSelectedListener(feedFilterListener);
        sGraphicsFeedChannelMode.setOnItemSelectedListener(feedFilterListener);
        sGraphicsFeedArchMode.setOnItemSelectedListener(feedFilterListener);

        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(new DriversAdapter(adrenotoolsManager.enumarateInstalledDrivers()));

        rvGraphicsFeed.setLayoutManager(new LinearLayoutManager(rvGraphicsFeed.getContext()));
        rvGraphicsFeed.addItemDecoration(new DividerItemDecoration(rvGraphicsFeed.getContext(), DividerItemDecoration.VERTICAL));
        rvGraphicsFeed.setAdapter(new GraphicsFeedAdapter(new ArrayList<>()));

        View btInstallDriver = layout.findViewById(R.id.BTInstallDriver);
        btInstallDriver.setOnClickListener((v) -> openZipInstaller());

        View btOpenContentsGraphics = layout.findViewById(R.id.BTOpenContentsGraphics);
        btOpenContentsGraphics.setOnClickListener(v -> openContents());

        layout.findViewById(R.id.BTLaneTurnip).setOnClickListener(v ->
                selectGraphicsLane(LANE_TURNIP, R.id.BTLaneTurnip));
        layout.findViewById(R.id.BTLaneOpenGL).setOnClickListener(v ->
                selectGraphicsLane(LANE_OPENGL, R.id.BTLaneOpenGL));

        layout.findViewById(R.id.BTDri3Settings).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                NavigationView navigationView = getActivity().findViewById(R.id.NavigationView);
                if (navigationView != null) {
                    navigationView.setCheckedItem(R.id.main_menu_settings);
                    ((MainActivity) getActivity()).onNavigationItemSelected(navigationView.getMenu().findItem(R.id.main_menu_settings));
                }
            }
        });

        layout.findViewById(R.id.BTForensicCenter).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                NavigationView navigationView = getActivity().findViewById(R.id.NavigationView);
                if (navigationView != null) {
                    navigationView.setCheckedItem(R.id.main_menu_diagnostics);
                    ((MainActivity) getActivity()).onNavigationItemSelected(navigationView.getMenu().findItem(R.id.main_menu_diagnostics));
                }
            }
        });

        layout.findViewById(R.id.BTOpenUpscalerSettings).setOnClickListener(v -> openUpscalerSettings());
        styleGraphicsCenterButtons(layout);
        refreshGraphicsCenterStatus();
        refreshGraphicsFeed();
        return layout;
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.adrenotools_gpu_drivers);
    }

    @Override
    public void onResume() {
        super.onResume();
        sourceMode = sharedPreferences.getString("contents_source_mode", sourceMode);
        channelMode = sharedPreferences.getString("contents_channel_mode", channelMode);
        archMode = sharedPreferences.getString("contents_arch_mode", archMode);
        if (sourceMode == null || sourceMode.trim().isEmpty()) sourceMode = "aesolator";
        if (channelMode == null || channelMode.trim().isEmpty()) channelMode = "stable";
        if (archMode == null || archMode.trim().isEmpty()) archMode = "all";
        setSpinnerSelectionByValue(sGraphicsFeedSourceMode, sourceValues, sourceMode, 0);
        setSpinnerSelectionByValue(sGraphicsFeedChannelMode, channelValues, channelMode, 0);
        setSpinnerSelectionByValue(sGraphicsFeedArchMode, archValues, archMode, 0);
        if (recyclerView != null) {
            recyclerView.setAdapter(new DriversAdapter(adrenotoolsManager.enumarateInstalledDrivers()));
        }
        refreshGraphicsCenterStatus();
        refreshGraphicsFeed();
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MainActivity.OPEN_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            String driver = adrenotoolsManager.installDriver(uri);
            if (!driver.isEmpty()) {
                ((DriversAdapter)recyclerView.getAdapter()).addItem(driver);
                refreshGraphicsCenterStatus();
            }
        }
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

    private void updateFeedFilterPreferencesFromUi() {
        sourceMode = getSpinnerSelectedValue(sGraphicsFeedSourceMode, sourceValues, sourceMode);
        channelMode = getSpinnerSelectedValue(sGraphicsFeedChannelMode, channelValues, channelMode);
        archMode = getSpinnerSelectedValue(sGraphicsFeedArchMode, archValues, archMode);
        sharedPreferences.edit()
                .putString("contents_source_mode", sourceMode)
                .putString("contents_channel_mode", channelMode)
                .putString("contents_arch_mode", archMode)
                .apply();
    }
    
    private class DriversAdapter extends RecyclerView.Adapter<DriversAdapter.ViewHolder> {
        private ArrayList<String> driversList;

        public class ViewHolder extends RecyclerView.ViewHolder {
            private ImageView ivIcon;
            private TextView tvName;
            private TextView tvVersion;
            private TextView tvMeta;
            private ImageButton btMenu;

            public ViewHolder(View v) {
                super(v);
                ivIcon = v.findViewById(R.id.IVIcon);
                tvName = v.findViewById(R.id.TVName);
                tvVersion = v.findViewById(R.id.TVVersion);
                tvMeta = v.findViewById(R.id.TVMeta);
                btMenu = v.findViewById(R.id.BTMenu);
            }
        }
        
        public DriversAdapter(ArrayList<String> driversList) {
            this.driversList = driversList;
        }
        
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.adrenotools_list_item, viewGroup, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder viewHolder, final int position) {
            final String entryId = driversList.get(position);
            viewHolder.tvName.setText(adrenotoolsManager.getDriverName(driversList.get(position)));
            viewHolder.tvVersion.setText(adrenotoolsManager.getDriverVersion(driversList.get(position)));
            viewHolder.tvMeta.setText(buildDriverMeta(entryId));
            viewHolder.ivIcon.setImageResource(R.drawable.icon_open);
            viewHolder.btMenu.setOnClickListener((v) -> {
                removeAtIndex(position);
            });
        }
        
        public void addItem(String item) {
            driversList.add(item);
            notifyItemInserted(getItemCount() - 1);
        }
        
        public void removeAtIndex(int index) {
            String deletedDriver = driversList.remove(index);
            adrenotoolsManager.removeDriver(deletedDriver);
            notifyItemRemoved(index);
            notifyItemRangeChanged(index, getItemCount());
            refreshGraphicsCenterStatus();
        }
        
        @Override
        public int getItemCount() {
            return driversList.size();
        }

        private String buildDriverMeta(String entryId) {
            String normalized = entryId == null ? "" : entryId.toLowerCase(Locale.US);
            String arch;
            if (normalized.contains("arm64ec")) arch = "ARM64EC";
            else if (normalized.contains("x86_64") || normalized.contains("amd64")) arch = "x86_64";
            else if (normalized.contains("arm64")) arch = "ARM64";
            else arch = "generic";
            return "Installed • " + arch;
        }
    }

    private void selectGraphicsLane(String lane, int buttonId) {
        selectedLane = lane;
        selectedLaneButtonId = buttonId;
        styleGraphicsCenterButtons(rootView);
        refreshGraphicsFeed();
    }

    private void openZipInstaller() {
        ContentDialog.confirm(getContext(), getString(R.string.install_drivers_message) + " " + getString(R.string.install_drivers_warning), () -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            getActivity().startActivityFromFragment(this, intent, MainActivity.OPEN_FILE_REQUEST_CODE);
        });
    }

    private void openContents() {
        sharedPreferences.edit().remove("contents_preselected_display_category").apply();
        ForensicLogger.logEvent(
                requireContext(),
                "info",
                "GRAPHICS_CENTER_OPEN_CONTENTS",
                null,
                "graphics_center",
                "open_contents_from_graphics_center",
                null
        );
        if (getActivity() instanceof MainActivity) {
            NavigationView navigationView = getActivity().findViewById(R.id.NavigationView);
            if (navigationView != null) {
                navigationView.setCheckedItem(R.id.main_menu_contents);
                ((MainActivity) getActivity()).onNavigationItemSelected(navigationView.getMenu().findItem(R.id.main_menu_contents));
            }
        }
    }

    private void openUpscalerSettings() {
        ForensicLogger.logEvent(
                requireContext(),
                "info",
                "GRAPHICS_CENTER_OPEN_UPSCALER",
                null,
                "graphics_center",
                "open_upscaler_settings_from_graphics_center",
                null
        );
        if (getActivity() instanceof MainActivity) {
            NavigationView navigationView = getActivity().findViewById(R.id.NavigationView);
            if (navigationView != null) {
                navigationView.setCheckedItem(R.id.main_menu_shortcuts);
                ((MainActivity) getActivity()).onNavigationItemSelected(navigationView.getMenu().findItem(R.id.main_menu_shortcuts));
                AppUtils.showToast(getContext(), R.string.graphics_center_upscaler_hint);
            }
        }
    }

    private void styleGraphicsCenterButtons(View root) {
        if (root == null || !isAdded()) return;
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        styleLaneButton(root, R.id.BTLaneTurnip, R.color.contents_lane_turnip, R.color.contents_lane_turnip_dark, isDarkMode, selectedLaneButtonId == R.id.BTLaneTurnip);
        styleLaneButton(root, R.id.BTLaneOpenGL, R.color.contents_lane_opengl, R.color.contents_lane_opengl_dark, isDarkMode, selectedLaneButtonId == R.id.BTLaneOpenGL);
        styleLaneButton(root, R.id.BTDri3Settings, R.color.colorPrimary, R.color.colorAccentDark, isDarkMode, false);
        styleLaneButton(root, R.id.BTForensicCenter, R.color.colorPrimary, R.color.colorAccentDark, isDarkMode, false);
        styleLaneButton(root, R.id.BTOpenUpscalerSettings, R.color.colorPrimary, R.color.colorAccentDark, isDarkMode, false);
    }

    private void styleLaneButton(View root, int buttonId, int lightColorRes, int darkColorRes, boolean isDarkMode, boolean isSelected) {
        View rawButton = root.findViewById(buttonId);
        if (!(rawButton instanceof Button)) return;
        Button button = (Button) rawButton;
        int accent = ContextCompat.getColor(requireContext(), isDarkMode ? darkColorRes : lightColorRes);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(10f));
        if (isSelected) {
            bg.setColor(withAlpha(accent, isDarkMode ? 172 : 222));
            bg.setStroke(dpToPx(1f), withAlpha(accent, 245));
            button.setTextColor(Color.WHITE);
        } else {
            bg.setColor(withAlpha(accent, isDarkMode ? 62 : 26));
            bg.setStroke(dpToPx(1f), withAlpha(accent, isDarkMode ? 238 : 180));
            button.setTextColor(isDarkMode ? Color.WHITE : accent);
        }

        button.setBackground(bg);
    }

    private int dpToPx(float dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    private int withAlpha(int color, int alpha) {
        int clampedAlpha = Math.max(0, Math.min(255, alpha));
        return (color & 0x00ffffff) | (clampedAlpha << 24);
    }

    private void refreshGraphicsCenterStatus() {
        if (!isAdded() || rootView == null || tvGraphicsCenterStatus == null) return;
        ContentsManager contentsManager = new ContentsManager(requireContext());
        contentsManager.syncContents();

        int localDrivers = adrenotoolsManager.enumarateInstalledDrivers().size();
        int turnip = countInstalled(contentsManager, ContentProfile.ContentType.CONTENT_TYPE_TURNIP_DRIVER);
        int openGl = countInstalled(contentsManager, ContentProfile.ContentType.CONTENT_TYPE_OPENGL_DRIVER);

        tvGraphicsCenterStatus.setText(getString(
                R.string.graphics_center_status_format,
                localDrivers,
                turnip,
                openGl
        ));
    }

    private int countInstalled(ContentsManager manager, ContentProfile.ContentType type) {
        List<ContentProfile> profiles = manager.getProfiles(type);
        if (profiles == null || profiles.isEmpty()) return 0;
        int count = 0;
        for (ContentProfile profile : profiles) {
            if (profile != null && profile.locallyInstalled) count++;
        }
        return count;
    }

    private void refreshGraphicsFeed() {
        if (!isAdded() || rvGraphicsFeed == null || tvGraphicsFeedEmpty == null) return;
        tvGraphicsFeedEmpty.setText(R.string.graphics_center_driver_feed_loading);
        tvGraphicsFeedEmpty.setVisibility(View.VISIBLE);
        rvGraphicsFeed.setVisibility(View.GONE);

        new Thread(() -> {
            if (!isAdded()) return;
            String mergedFeed = resolveMergedFeedJson();
            ContentsManager contentsManager = new ContentsManager(requireContext());
            if (mergedFeed != null && !mergedFeed.trim().isEmpty()) {
                contentsManager.setRemoteProfiles(mergedFeed);
            } else {
                contentsManager.syncContents();
            }
            List<ContentProfile> profiles = collectLaneProfiles(contentsManager, selectedLane);
            if (!isAdded() || getActivity() == null) return;
            requireActivity().runOnUiThread(() -> showGraphicsFeed(profiles));
        }).start();
    }

    private void showGraphicsFeed(List<ContentProfile> profiles) {
        if (!isAdded() || rvGraphicsFeed == null || tvGraphicsFeedEmpty == null) return;
        if (profiles == null || profiles.isEmpty()) {
            tvGraphicsFeedEmpty.setText(R.string.graphics_center_driver_feed_empty);
            tvGraphicsFeedEmpty.setVisibility(View.VISIBLE);
            rvGraphicsFeed.setVisibility(View.GONE);
            rvGraphicsFeed.setAdapter(new GraphicsFeedAdapter(new ArrayList<>()));
            return;
        }
        tvGraphicsFeedEmpty.setVisibility(View.GONE);
        rvGraphicsFeed.setVisibility(View.VISIBLE);
        rvGraphicsFeed.setAdapter(new GraphicsFeedAdapter(profiles));
    }

    private String resolveMergedFeedJson() {
        String cached = sharedPreferences.getString(PREF_REMOTE_CACHE_JSON, "[]");
        String sourceSignature = buildSourceSignature();
        String cachedSourceSignature = sharedPreferences.getString(PREF_REMOTE_CACHE_SOURCE_SIGNATURE, "");
        long now = System.currentTimeMillis();
        boolean shouldRefresh = !sourceSignature.equalsIgnoreCase(cachedSourceSignature)
                || (now - lastRemoteFeedRefreshMs) > REMOTE_FEED_REFRESH_INTERVAL_MS;
        if (shouldRefresh) {
            String fresh = fetchRemoteFeedJson();
            if (fresh != null && !fresh.trim().isEmpty() && !"[]".equals(fresh.trim())) {
                sharedPreferences.edit()
                        .putString(PREF_REMOTE_CACHE_JSON, fresh)
                        .putString(PREF_REMOTE_CACHE_SOURCE_SIGNATURE, sourceSignature)
                        .apply();
                lastRemoteFeedRefreshMs = now;
                return fresh;
            }
            if (!sourceSignature.equalsIgnoreCase(cachedSourceSignature)) {
                return "[]";
            }
        }
        return cached != null && !cached.trim().isEmpty() ? cached : "[]";
    }

    private String buildSourceSignature() {
        String normalizedMode = sourceMode == null ? "aesolator" : sourceMode.trim().toLowerCase(Locale.US);
        String customUrl = "";
        if ("custom".equals(normalizedMode)) {
            String value = sharedPreferences.getString("downloadable_contents_url", "");
            customUrl = value == null ? "" : value.trim().toLowerCase(Locale.US);
        }
        return normalizedMode + "|" + customUrl;
    }

    private String fetchRemoteFeedJson() {
        ArrayList<String> payloads = new ArrayList<>();
        HashSet<String> uniqueUrls = new HashSet<>();
        for (String url : resolveSelectedSourceUrls(sourceMode)) {
            if (url == null) continue;
            String normalized = url.trim();
            if (normalized.isEmpty() || !uniqueUrls.add(normalized)) continue;
            addFeedPayload(payloads, normalized);
        }
        if (payloads.isEmpty()) return "[]";
        return mergeFeedPayloads(payloads);
    }

    private List<String> resolveSelectedSourceUrls(String selectedSourceMode) {
        ArrayList<String> urls = new ArrayList<>();
        String normalized = selectedSourceMode == null ? "aesolator" : selectedSourceMode.trim().toLowerCase(Locale.US);
        if ("wcphub".equals(normalized)) {
            urls.add(ContentsManager.REMOTE_PROFILES);
            return urls;
        }
        if ("fallback".equals(normalized)) {
            urls.add(ContentsManager.REMOTE_PROFILES_FALLBACK);
            return urls;
        }
        if ("custom".equals(normalized)) {
            String preferredUrl = sharedPreferences.getString("downloadable_contents_url", "");
            if (preferredUrl != null && !preferredUrl.trim().isEmpty()) {
                urls.add(preferredUrl.trim());
            }
            return urls;
        }
        if ("all".equals(normalized)) {
            urls.add(ContentsManager.REMOTE_PROFILES_AE);
            urls.add(ContentsManager.REMOTE_PROFILES);
            urls.add(ContentsManager.REMOTE_PROFILES_FALLBACK);
            return urls;
        }
        urls.add(ContentsManager.REMOTE_PROFILES_AE);
        return urls;
    }

    private void addFeedPayload(List<String> payloads, String url) {
        if (url == null || url.trim().isEmpty()) return;
        try {
            String json = Downloader.downloadString(url.trim());
            if (json != null && !json.trim().isEmpty()) payloads.add(json);
        } catch (Exception ignored) {
        }
    }

    private String mergeFeedPayloads(List<String> payloads) {
        Map<String, JSONObject> mergedMap = new LinkedHashMap<>();
        for (String payload : payloads) {
            try {
                JSONArray array = new JSONArray(payload);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject candidate = array.getJSONObject(i);
                    String key = buildFeedEntryKey(candidate);
                    JSONObject current = mergedMap.get(key);
                    if (current == null || isBetterFeedCandidate(candidate, current)) {
                        mergedMap.put(key, candidate);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        JSONArray out = new JSONArray();
        for (JSONObject object : mergedMap.values()) out.put(object);
        return out.toString();
    }

    private String buildFeedEntryKey(JSONObject object) {
        String type = object.optString("type", "").trim().toLowerCase(Locale.US);
        String verName = object.optString("verName", "").trim().toLowerCase(Locale.US);
        String displayCategory = object.optString(ContentProfile.MARK_DISPLAY_CATEGORY, "").trim().toLowerCase(Locale.US);
        String channel = object.optString(ContentProfile.MARK_CHANNEL, "").trim().toLowerCase(Locale.US);
        String arch = resolveRemoteArchHint(object);
        return type + "|" + verName + "|" + displayCategory + "|" + channel + "|" + arch;
    }

    private boolean isBetterFeedCandidate(JSONObject candidate, JSONObject current) {
        int candidateVerCode = parseRemoteVerCode(candidate);
        int currentVerCode = parseRemoteVerCode(current);
        if (candidateVerCode != currentVerCode) return candidateVerCode > currentVerCode;

        int candidatePriority = resolveRemoteSourcePriority(candidate);
        int currentPriority = resolveRemoteSourcePriority(current);
        if (candidatePriority != currentPriority) return candidatePriority > currentPriority;

        String candidateUrl = candidate.optString("remoteUrl", "");
        String currentUrl = current.optString("remoteUrl", "");
        return candidateUrl.compareToIgnoreCase(currentUrl) < 0;
    }

    private int parseRemoteVerCode(JSONObject object) {
        Object raw = object.opt("verCode");
        if (raw instanceof Number) return ((Number) raw).intValue();
        if (raw instanceof String) {
            try {
                return Integer.parseInt(((String) raw).trim().replaceAll("[^0-9-]", ""));
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

    private String resolveRemoteArchHint(JSONObject object) {
        String combined = (
                object.optString("verName", "") + " "
                        + object.optString("description", "") + " "
                        + object.optString("remoteUrl", "") + " "
                        + object.optString(ContentProfile.MARK_RELEASE_TAG, "")
        ).toLowerCase(Locale.US);
        if (combined.contains("arm64ec") || combined.contains("arm64-ec")) return "arm64ec";
        if (combined.contains("x86_64") || combined.contains("x86-64") || combined.contains("amd64")) return "x86_64";
        if (combined.contains("arm64") || combined.contains("aarch64")) return "arm64";
        return "generic";
    }

    private List<ContentProfile> collectLaneProfiles(ContentsManager manager, String lane) {
        ArrayList<ContentProfile> selected = new ArrayList<>();
        if (manager == null) return selected;

        addMatchingProfiles(selected, manager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_TURNIP_DRIVER), lane);
        addMatchingProfiles(selected, manager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_OPENGL_DRIVER), lane);

        Collections.sort(selected, (left, right) -> {
            if (left == null && right == null) return 0;
            if (left == null) return 1;
            if (right == null) return -1;
            if (left.locallyInstalled != right.locallyInstalled) {
                return left.locallyInstalled ? -1 : 1;
            }
            int sourceCmp = buildFeedSourceLabel(left).compareToIgnoreCase(buildFeedSourceLabel(right));
            if (sourceCmp != 0) return sourceCmp;
            int channelCmp = Integer.compare(resolveChannelPriority(right.getChannel()), resolveChannelPriority(left.getChannel()));
            if (channelCmp != 0) return channelCmp;
            int codeCmp = Integer.compare(right.verCode, left.verCode);
            if (codeCmp != 0) return codeCmp;
            String lv = left.verName == null ? "" : left.verName;
            String rv = right.verName == null ? "" : right.verName;
            return rv.compareToIgnoreCase(lv);
        });
        return selected;
    }

    private void addMatchingProfiles(List<ContentProfile> out, List<ContentProfile> source, String lane) {
        if (source == null || source.isEmpty()) return;
        for (ContentProfile profile : source) {
            if (profile == null) continue;
            if (!profile.locallyInstalled && (profile.remoteUrl == null || profile.remoteUrl.trim().isEmpty())) continue;
            if (!matchesLane(profile, lane)) continue;
            if (!matchesChannelFilter(profile)) continue;
            if (!matchesArchitectureFilter(profile)) continue;
            out.add(profile);
        }
    }

    private int resolveChannelPriority(String channel) {
        String normalized = channel == null ? "" : channel.trim().toLowerCase(Locale.US);
        if (normalized.isEmpty()) return 30;
        if (ContentProfile.CHANNEL_STABLE.equals(normalized)) return 30;
        if (ContentProfile.CHANNEL_BETA.equals(normalized)) return 20;
        if (ContentProfile.CHANNEL_NIGHTLY.equals(normalized)) return 10;
        return 0;
    }

    private boolean matchesChannelFilter(ContentProfile profile) {
        if (profile == null) return false;
        String normalizedMode = channelMode == null ? "stable" : channelMode.trim().toLowerCase(Locale.US);
        if ("all".equals(normalizedMode)) return true;
        if ("stable".equals(normalizedMode)) return !profile.isBetaLike();
        if ("nightly".equals(normalizedMode)) return profile.isBetaLike();
        return true;
    }

    private boolean matchesArchitectureFilter(ContentProfile profile) {
        if (profile == null) return false;
        String normalizedMode = archMode == null ? "all" : archMode.trim().toLowerCase(Locale.US);
        if ("all".equals(normalizedMode)) return true;
        String profileArch = resolveProfileArchTag(profile);
        return normalizedMode.equalsIgnoreCase(profileArch);
    }

    private String resolveProfileArchTag(ContentProfile profile) {
        if (profile == null) return "generic";
        String combined = (
                (profile.verName == null ? "" : profile.verName) + " "
                        + (profile.desc == null ? "" : profile.desc) + " "
                        + (profile.remoteUrl == null ? "" : profile.remoteUrl) + " "
                        + (profile.releaseTag == null ? "" : profile.releaseTag)
        ).toLowerCase(Locale.US);
        if (combined.contains("arm64ec") || combined.contains("arm64-ec")) return "arm64ec";
        if (combined.contains("x86_64") || combined.contains("x86-64") || combined.contains("amd64")) return "x86_64";
        if (combined.contains("arm64") || combined.contains("aarch64")) return "arm64";
        return "generic";
    }

    private boolean matchesLane(ContentProfile profile, String lane) {
        if (profile == null || lane == null) return false;
        if (LANE_TURNIP.equals(lane)) {
            return profile.type == ContentProfile.ContentType.CONTENT_TYPE_TURNIP_DRIVER;
        }
        if (LANE_OPENGL.equals(lane)) {
            return profile.type == ContentProfile.ContentType.CONTENT_TYPE_OPENGL_DRIVER;
        }
        return false;
    }

    private int resolveFeedAccentColor(ContentProfile profile) {
        if (profile == null) return ContextCompat.getColor(requireContext(), R.color.colorAccent);
        int colorRes;
        switch (profile.type) {
            case CONTENT_TYPE_TURNIP_DRIVER -> colorRes = R.color.contents_lane_turnip;
            case CONTENT_TYPE_OPENGL_DRIVER -> colorRes = R.color.contents_lane_opengl;
            case CONTENT_TYPE_DGVOODOO -> colorRes = R.color.contents_lane_dgvoodoo;
            case CONTENT_TYPE_DXVK -> colorRes = R.color.contents_lane_dxvk;
            case CONTENT_TYPE_VKD3D -> colorRes = R.color.contents_lane_vkd3d;
            case CONTENT_TYPE_VULKAN_SDK -> colorRes = R.color.contents_lane_vulkansdk;
            default -> colorRes = R.color.colorAccent;
        }
        boolean isDark = sharedPreferences.getBoolean("dark_mode", false);
        if (isDark) {
            switch (profile.type) {
                case CONTENT_TYPE_TURNIP_DRIVER -> colorRes = R.color.contents_lane_turnip_dark;
                case CONTENT_TYPE_OPENGL_DRIVER -> colorRes = R.color.contents_lane_opengl_dark;
                case CONTENT_TYPE_DGVOODOO -> colorRes = R.color.contents_lane_dgvoodoo_dark;
                case CONTENT_TYPE_DXVK -> colorRes = R.color.contents_lane_dxvk_dark;
                case CONTENT_TYPE_VKD3D -> colorRes = R.color.contents_lane_vkd3d_dark;
                case CONTENT_TYPE_VULKAN_SDK -> colorRes = R.color.contents_lane_vulkansdk_dark;
                default -> colorRes = R.color.colorAccentDark;
            }
        }
        return ContextCompat.getColor(requireContext(), colorRes);
    }

    private String buildFeedSourceLabel(ContentProfile profile) {
        if (profile == null) return "unknown";
        String repo = profile.sourceRepo == null ? "" : profile.sourceRepo.trim();
        if (!repo.isEmpty()) return repo;
        if (profile.remoteUrl == null || profile.remoteUrl.trim().isEmpty()) return "local package";
        try {
            Uri uri = Uri.parse(profile.remoteUrl);
            String host = uri.getHost();
            if (host != null && !host.trim().isEmpty()) return host.trim();
        } catch (Exception ignored) {
        }
        return "remote package";
    }

    private String buildFeedMetaLine(ContentProfile profile) {
        StringBuilder meta = new StringBuilder(profile.getDisplayCategory());
        String arch = resolveProfileArchTag(profile);
        if (!"generic".equals(arch)) meta.append(" • ").append(arch);
        String channel = profile.getChannel();
        if (channel != null && !channel.trim().isEmpty()) meta.append(" • ").append(channel.toLowerCase(Locale.US));
        if (profile.releaseTag != null && !profile.releaseTag.trim().isEmpty()) {
            meta.append(" • ").append(profile.releaseTag.trim());
        }
        meta.append(" • ").append("verCode=").append(profile.verCode);
        if (profile.locallyInstalled) meta.append(" • installed");
        return meta.toString();
    }

    private String normalizeSha256(String value) {
        if (value == null) return "";
        String normalized = value.trim().toLowerCase(Locale.US).replaceAll("[^0-9a-f]", "");
        return normalized.length() == 64 ? normalized : "";
    }

    private void installRemoteProfile(ContentProfile requestedProfile) {
        if (!isAdded() || requestedProfile == null) return;
        if (requestedProfile.type != ContentProfile.ContentType.CONTENT_TYPE_TURNIP_DRIVER
                && requestedProfile.type != ContentProfile.ContentType.CONTENT_TYPE_OPENGL_DRIVER) {
            AppUtils.showToast(getContext(), R.string.graphics_center_install_via_contents);
            return;
        }
        if (requestedProfile.remoteUrl == null || requestedProfile.remoteUrl.trim().isEmpty()) {
            AppUtils.showToast(getContext(), R.string.graphics_center_install_failed);
            return;
        }
        final String sourceLabel = buildFeedSourceLabel(requestedProfile);
        final String profileLabel = (requestedProfile.verName == null || requestedProfile.verName.trim().isEmpty())
                ? requestedProfile.getDisplayCategory()
                : requestedProfile.verName;
        ContentDialog.confirm(
                getContext(),
                getString(R.string.graphics_center_install_confirm, profileLabel, sourceLabel),
                () -> performRemoteInstall(requestedProfile)
        );
    }

    private void performRemoteInstall(ContentProfile requestedProfile) {
        if (!isAdded()) return;
        final String entryName = ContentsManager.getEntryName(requestedProfile);
        installingEntries.add(entryName);
        refreshGraphicsFeed();

        PreloaderDialog preloaderDialog = new PreloaderDialog(requireActivity());
        preloaderDialog.showOnUiThread(R.string.installing_content);

        new Thread(() -> {
            File output = new File(requireContext().getCacheDir(), "graphics-feed-" + System.currentTimeMillis() + ".wcp");
            boolean downloaded = Downloader.downloadFile(requestedProfile.remoteUrl, output);
            String expectedSha256 = normalizeSha256(requestedProfile.remoteSha256);
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

            if (!downloaded) {
                if (output.exists()) output.delete();
                String finalActualSha = actualSha256;
                if (!isAdded() || getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    preloaderDialog.closeOnUiThread();
                    installingEntries.remove(entryName);
                    if (!expectedSha256.isEmpty() && !expectedSha256.equals(finalActualSha)) {
                        ContentDialog.alert(getContext(), R.string.content_cannot_be_trusted, null);
                    } else {
                        ContentDialog.alert(getContext(), R.string.graphics_center_install_failed, null);
                    }
                    refreshGraphicsFeed();
                });
                return;
            }

            installAdrenotoolsDriverArchive(output, requestedProfile, preloaderDialog, entryName);
        }).start();
    }

    private void installAdrenotoolsDriverArchive(
            File downloadedArchive,
            ContentProfile requestedProfile,
            PreloaderDialog preloaderDialog,
            String requestedEntry
    ) {
        Executors.newSingleThreadExecutor().execute(() -> {
            String installedDriverName = "";
            try {
                installedDriverName = adrenotoolsManager.installDriver(Uri.fromFile(downloadedArchive));
            } catch (Exception ignored) {
            }

            if (!isAdded() || getActivity() == null) {
                if (downloadedArchive.exists()) downloadedArchive.delete();
                return;
            }

            final String finalInstalledDriverName = installedDriverName;
            requireActivity().runOnUiThread(() -> {
                preloaderDialog.closeOnUiThread();
                installingEntries.remove(requestedEntry);
                if (finalInstalledDriverName == null || finalInstalledDriverName.trim().isEmpty()) {
                    ContentDialog.alert(getContext(), R.string.graphics_center_install_failed, null);
                } else {
                    ContentDialog.alert(getContext(), R.string.content_installed_success, null);
                    if (recyclerView != null) {
                        recyclerView.setAdapter(new DriversAdapter(adrenotoolsManager.enumarateInstalledDrivers()));
                    }
                    ForensicLogger.logEvent(
                            getContext(),
                            "info",
                            "GRAPHICS_CENTER_REMOTE_INSTALL",
                            null,
                            "graphics_center",
                            "driver_zip_installed",
                            ForensicLogger.fields(
                                    "type", requestedProfile.type.toString(),
                                    "ver_name", requestedProfile.verName,
                                    "installed_driver_name", finalInstalledDriverName
                            )
                    );
                }
                refreshGraphicsCenterStatus();
                refreshGraphicsFeed();
            });

            if (downloadedArchive.exists()) downloadedArchive.delete();
        });
    }

    private class GraphicsFeedAdapter extends RecyclerView.Adapter<GraphicsFeedAdapter.ViewHolder> {
        private final List<ContentProfile> profiles;

        private GraphicsFeedAdapter(List<ContentProfile> profiles) {
            this.profiles = profiles == null ? new ArrayList<>() : profiles;
        }

        private class ViewHolder extends RecyclerView.ViewHolder {
            private final ImageView ivFeedIcon;
            private final TextView tvFeedTitle;
            private final TextView tvFeedMeta;
            private final TextView tvFeedSource;
            private final ImageButton btFeedInstall;
            private final ProgressBar pbFeedInstall;

            private ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivFeedIcon = itemView.findViewById(R.id.IVFeedIcon);
                tvFeedTitle = itemView.findViewById(R.id.TVFeedTitle);
                tvFeedMeta = itemView.findViewById(R.id.TVFeedMeta);
                tvFeedSource = itemView.findViewById(R.id.TVFeedSource);
                btFeedInstall = itemView.findViewById(R.id.BTFeedInstall);
                pbFeedInstall = itemView.findViewById(R.id.PBFeedInstall);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.graphics_driver_feed_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ContentProfile profile = profiles.get(position);
            int accent = resolveFeedAccentColor(profile);
            boolean installed = profile != null && profile.locallyInstalled;
            boolean canInstall = profile != null && profile.remoteUrl != null && !profile.remoteUrl.trim().isEmpty();
            String entryName = ContentsManager.getEntryName(profile);
            boolean isInstalling = installingEntries.contains(entryName);

            holder.ivFeedIcon.setImageResource(R.drawable.icon_open);
            holder.ivFeedIcon.setColorFilter(accent);
            holder.tvFeedTitle.setText(profile.verName == null || profile.verName.trim().isEmpty()
                    ? profile.getDisplayCategory()
                    : profile.verName);
            holder.tvFeedTitle.setTextColor(accent);
            holder.tvFeedMeta.setText(buildFeedMetaLine(profile));
            holder.tvFeedMeta.setTextColor(withAlpha(accent, 205));
            holder.tvFeedSource.setText(buildFeedSourceLabel(profile));
            holder.tvFeedSource.setTextColor(withAlpha(accent, 180));

            holder.btFeedInstall.setVisibility(isInstalling ? View.GONE : View.VISIBLE);
            holder.pbFeedInstall.setVisibility(isInstalling ? View.VISIBLE : View.GONE);

            if (installed) {
                holder.btFeedInstall.setImageResource(canInstall ? R.drawable.icon_popup_menu_download : R.drawable.icon_confirm);
                holder.btFeedInstall.setContentDescription(getString(canInstall
                        ? R.string.graphics_center_update_action
                        : R.string.graphics_center_installed_action));
            } else {
                holder.btFeedInstall.setImageResource(R.drawable.icon_popup_menu_download);
                holder.btFeedInstall.setContentDescription(getString(R.string.graphics_center_install_action));
            }

            holder.btFeedInstall.setEnabled(canInstall);
            holder.btFeedInstall.setOnClickListener(v -> {
                if (canInstall) installRemoteProfile(profile);
            });
        }

        @Override
        public int getItemCount() {
            return profiles.size();
        }
    }
}
