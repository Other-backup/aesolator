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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import com.winlator.cmod.core.SpinnerAdapters;
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
    private static final String PREF_GRAPHICS_SOURCE_MODE = "graphics_feed_source_mode";
    private static final String PREF_GRAPHICS_BRANCH_MODE = "graphics_feed_branch_mode";
    private static final String LANE_TURNIP = "turnip";
    private static final String LANE_OPENGL = "opengl";

    private AdrenotoolsManager adrenotoolsManager;
    private SharedPreferences sharedPreferences;
    private RecyclerView recyclerView;
    private RecyclerView rvGraphicsFeed;
    private View rootView;
    private TextView tvGraphicsCenterStatus;
    private TextView tvGraphicsFeedEmpty;
    private Spinner sGraphicsFeedSourceMode;
    private Spinner sGraphicsFeedBranchMode;
    private int selectedLaneButtonId = R.id.BTLaneTurnip;
    private String selectedLane = LANE_TURNIP;
    private String sourceMode = "ae_archive";
    private String branchMode = "all";
    private String[] sourceValues;
    private final ArrayList<String> branchEntries = new ArrayList<>();
    private final ArrayList<String> branchValues = new ArrayList<>();
    private boolean suppressBranchCallback = false;
    private final HashSet<String> installingEntries = new HashSet<>();
    
    @Override 
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.adrenotoolsManager = new AdrenotoolsManager(getActivity());
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        sourceMode = sharedPreferences.getString(PREF_GRAPHICS_SOURCE_MODE, "ae_archive");
        branchMode = sharedPreferences.getString(PREF_GRAPHICS_BRANCH_MODE, "all");
        if (sourceMode == null || sourceMode.trim().isEmpty()) sourceMode = "ae_archive";
        if (branchMode == null || branchMode.trim().isEmpty()) branchMode = "all";
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
        sGraphicsFeedBranchMode = layout.findViewById(R.id.SGraphicsFeedBranchMode);

        sourceValues = getResources().getStringArray(R.array.graphics_feed_source_values);

        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        sGraphicsFeedSourceMode.setAdapter(SpinnerAdapters.create(
                requireContext(),
                isDarkMode,
                getResources().getStringArray(R.array.graphics_feed_source_entries)
        ));
        sGraphicsFeedBranchMode.setAdapter(SpinnerAdapters.create(
                requireContext(),
                isDarkMode,
                new ArrayList<>(Collections.singletonList(getString(R.string.graphics_center_branch_all)))
        ));
        int popupBackground = isDarkMode ? R.drawable.content_dialog_background_dark : R.drawable.content_dialog_background;
        sGraphicsFeedSourceMode.setPopupBackgroundResource(popupBackground);
        sGraphicsFeedBranchMode.setPopupBackgroundResource(popupBackground);
        applyFeedSpinnerTheme(isDarkMode);
        setSpinnerSelectionByValue(sGraphicsFeedSourceMode, sourceValues, sourceMode, 0);
        branchEntries.clear();
        branchEntries.add(getString(R.string.graphics_center_branch_all));
        branchValues.clear();
        branchValues.add("all");
        setSpinnerSelectionByValue(sGraphicsFeedBranchMode, branchValues.toArray(new String[0]), branchMode, 0);

        AdapterView.OnItemSelectedListener feedFilterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent == sGraphicsFeedBranchMode && suppressBranchCallback) return;
                updateFeedFilterPreferencesFromUi();
                refreshGraphicsFeed();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        sGraphicsFeedSourceMode.setOnItemSelectedListener(feedFilterListener);
        sGraphicsFeedBranchMode.setOnItemSelectedListener(feedFilterListener);

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
            navigateToMainMenuItem(R.id.main_menu_settings, new SettingsFragment());
        });

        layout.findViewById(R.id.BTForensicCenter).setOnClickListener(v -> {
            navigateToMainMenuItem(R.id.main_menu_diagnostics, new ForensicCenterFragment());
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
        sourceMode = sharedPreferences.getString(PREF_GRAPHICS_SOURCE_MODE, sourceMode);
        branchMode = sharedPreferences.getString(PREF_GRAPHICS_BRANCH_MODE, branchMode);
        if (sourceMode == null || sourceMode.trim().isEmpty()) sourceMode = "ae_archive";
        if (branchMode == null || branchMode.trim().isEmpty()) branchMode = "all";
        setSpinnerSelectionByValue(sGraphicsFeedSourceMode, sourceValues, sourceMode, 0);
        setSpinnerSelectionByValue(sGraphicsFeedBranchMode, branchValues.toArray(new String[0]), branchMode, 0);
        if (recyclerView != null) {
            recyclerView.setAdapter(new DriversAdapter(adrenotoolsManager.enumarateInstalledDrivers()));
        }
        styleGraphicsCenterButtons(rootView);
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
        branchMode = getSpinnerSelectedValue(sGraphicsFeedBranchMode, branchValues.toArray(new String[0]), branchMode);
        sharedPreferences.edit()
                .putString(PREF_GRAPHICS_SOURCE_MODE, sourceMode)
                .putString(PREF_GRAPHICS_BRANCH_MODE, branchMode)
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
            int accent = resolveInstalledDriverAccent(entryId);
            boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
            viewHolder.tvName.setText(adrenotoolsManager.getDriverName(driversList.get(position)));
            viewHolder.tvVersion.setText(adrenotoolsManager.getDriverVersion(driversList.get(position)));
            viewHolder.tvMeta.setText(buildDriverMeta(entryId));
            viewHolder.ivIcon.setImageResource(R.drawable.icon_open);
            viewHolder.ivIcon.setColorFilter(accent);
            viewHolder.tvName.setTextColor(accent);
            viewHolder.tvVersion.setTextColor(withAlpha(accent, isDarkMode ? 228 : 176));
            viewHolder.tvMeta.setTextColor(withAlpha(accent, isDarkMode ? 214 : 164));
            GradientDrawable rowBackground = new GradientDrawable();
            rowBackground.setShape(GradientDrawable.RECTANGLE);
            rowBackground.setCornerRadius(dpToPx(12f));
            rowBackground.setColor(withAlpha(accent, isDarkMode ? 56 : 26));
            rowBackground.setStroke(dpToPx(1f), withAlpha(accent, isDarkMode ? 210 : 138));
            viewHolder.itemView.setBackground(rowBackground);
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

        private int resolveInstalledDriverAccent(String entryId) {
            String normalized = entryId == null ? "" : entryId.toLowerCase(Locale.US);
            boolean openGlLike = normalized.contains("opengl")
                    || normalized.contains("gallium")
                    || normalized.contains("zink")
                    || normalized.contains("gl");
            boolean isDark = sharedPreferences.getBoolean("dark_mode", false);
            int colorRes = openGlLike
                    ? (isDark ? R.color.contents_lane_opengl_dark : R.color.contents_lane_opengl)
                    : (isDark ? R.color.contents_lane_turnip_dark : R.color.contents_lane_turnip);
            return ContextCompat.getColor(requireContext(), colorRes);
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
        navigateToMainMenuItem(R.id.main_menu_contents, new ContentsFragment());
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
        navigateToMainMenuItem(R.id.main_menu_shortcuts, new ShortcutsFragment());
        AppUtils.showToast(getContext(), R.string.graphics_center_upscaler_hint);
    }

    private void styleGraphicsCenterButtons(View root) {
        if (root == null || !isAdded()) return;
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        styleLaneButton(root, R.id.BTLaneTurnip, R.color.contents_lane_turnip, R.color.contents_lane_turnip_dark, isDarkMode, selectedLaneButtonId == R.id.BTLaneTurnip);
        styleLaneButton(root, R.id.BTLaneOpenGL, R.color.contents_lane_opengl, R.color.contents_lane_opengl_dark, isDarkMode, selectedLaneButtonId == R.id.BTLaneOpenGL);
        styleLaneButton(root, R.id.BTDri3Settings, R.color.colorPrimary, R.color.colorAccentDark, isDarkMode, false);
        styleLaneButton(root, R.id.BTForensicCenter, R.color.colorPrimary, R.color.colorAccentDark, isDarkMode, false);
        styleLaneButton(root, R.id.BTOpenUpscalerSettings, R.color.colorPrimary, R.color.colorAccentDark, isDarkMode, false);
        styleLaneButton(root, R.id.BTInstallDriver, R.color.colorPrimary, R.color.colorAccentDark, isDarkMode, false);
        styleLaneButton(root, R.id.BTOpenContentsGraphics, R.color.colorAccent, R.color.colorAccentDark, isDarkMode, false);
    }

    private void applyFeedSpinnerTheme(boolean isDarkMode) {
        int spinnerBackground = isDarkMode ? R.drawable.combo_box_dark : R.drawable.combo_box;
        if (sGraphicsFeedSourceMode != null) sGraphicsFeedSourceMode.setBackgroundResource(spinnerBackground);
        if (sGraphicsFeedBranchMode != null) sGraphicsFeedBranchMode.setBackgroundResource(spinnerBackground);
    }

    private void navigateToMainMenuItem(int menuItemId, Fragment fallbackFragment) {
        if (!isAdded() || getActivity() == null) return;
        if (getActivity() instanceof MainActivity) {
            NavigationView navigationView = getActivity().findViewById(R.id.NavigationView);
            if (navigationView != null && navigationView.getMenu() != null) {
                MenuItem target = navigationView.getMenu().findItem(menuItemId);
                if (target != null) {
                    navigationView.setCheckedItem(menuItemId);
                    ((MainActivity) getActivity()).onNavigationItemSelected(target);
                    return;
                }
            }
        }
        if (fallbackFragment != null) {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.FLFragmentContainer, fallbackFragment)
                    .commit();
        }
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
            List<ContentProfile> sourceProfiles = collectSourceProfiles(selectedLane, sourceMode);
            if (!isAdded() || getActivity() == null) return;
            requireActivity().runOnUiThread(() -> {
                updateBranchSelector(sourceProfiles);
                showGraphicsFeed(applyBranchFilter(sourceProfiles, branchMode));
            });
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

    private List<ContentProfile> collectSourceProfiles(String lane, String selectedSourceMode) {
        ArrayList<ContentProfile> profiles = new ArrayList<>();
        String source = selectedSourceMode == null ? "ae_archive" : selectedSourceMode.trim().toLowerCase(Locale.US);

        if ("stevenmxz".equals(source)) {
            profiles.addAll(fetchGitHubReleaseZipProfiles("StevenMXZ/freedreno_turnip-CI", lane, source));
        } else if ("gamenative".equals(source)) {
            profiles.addAll(fetchGameNativeZipProfiles(lane));
        } else if ("whitebelyash".equals(source)) {
            profiles.addAll(fetchGitHubReleaseZipProfiles("whitebelyash/freedreno_turnip-CI", lane, source));
        } else if ("mrpurple".equals(source)) {
            profiles.addAll(fetchGitHubReleaseZipProfiles("MrPurple666/purple-turnip", lane, source));
        } else {
            profiles.addAll(fetchGitHubReleaseZipProfiles("kosoymiki/wcp-graphics-lanes", lane, source));
        }

        Collections.sort(profiles, (left, right) -> {
            int codeCmp = Integer.compare(right.verCode, left.verCode);
            if (codeCmp != 0) return codeCmp;
            String rv = right.verName == null ? "" : right.verName;
            String lv = left.verName == null ? "" : left.verName;
            return rv.compareToIgnoreCase(lv);
        });
        return profiles;
    }

    private List<ContentProfile> fetchGitHubReleaseZipProfiles(String repo, String lane, String sourceKey) {
        ArrayList<ContentProfile> profiles = new ArrayList<>();
        try {
            String apiUrl = "https://api.github.com/repos/" + repo + "/releases?per_page=60";
            String payload = Downloader.downloadString(apiUrl);
            if (payload == null || payload.trim().isEmpty()) return profiles;
            JSONArray releases = new JSONArray(payload);

            int fallbackCode = 2_000_000_000;
            for (int i = 0; i < releases.length(); i++) {
                JSONObject release = releases.optJSONObject(i);
                if (release == null) continue;
                String tag = release.optString("tag_name", "").trim();
                String releaseName = release.optString("name", "").trim();
                String publishedAt = release.optString("published_at", "").trim();
                int verCode = parsePublishedAtVerCode(publishedAt, fallbackCode - i);
                JSONArray assets = release.optJSONArray("assets");
                if (assets == null) continue;

                for (int ai = 0; ai < assets.length(); ai++) {
                    JSONObject asset = assets.optJSONObject(ai);
                    if (asset == null) continue;
                    String assetName = asset.optString("name", "").trim();
                    String assetUrl = asset.optString("browser_download_url", "").trim();
                    if (assetName.isEmpty() || assetUrl.isEmpty()) continue;
                    String lowerName = (assetName + " " + assetUrl).toLowerCase(Locale.US);
                    if (!lowerName.contains(".zip")) continue;
                    if (!matchesAssetLane(lane, lowerName)) continue;

                    ContentProfile profile = new ContentProfile();
                    profile.type = LANE_OPENGL.equals(lane)
                            ? ContentProfile.ContentType.CONTENT_TYPE_OPENGL_DRIVER
                            : ContentProfile.ContentType.CONTENT_TYPE_TURNIP_DRIVER;
                    profile.verName = stripZipSuffix(assetName);
                    profile.verCode = verCode;
                    profile.desc = releaseName.isEmpty() ? assetName : releaseName;
                    profile.remoteUrl = assetUrl;
                    profile.sourceRepo = repo;
                    profile.releaseTag = tag;
                    profile.displayCategory = LANE_OPENGL.equals(lane) ? "OpenGL Driver" : "Turnip";
                    profile.delivery = resolveAssetBranch(sourceKey, lowerName, tag, releaseName);
                    profile.channel = ContentProfile.CHANNEL_STABLE;
                    profile.locallyInstalled = isLikelyInstalledDriver(profile.verName);
                    profiles.add(profile);
                }
            }
        } catch (Exception ignored) {
        }
        return profiles;
    }

    private List<ContentProfile> fetchGameNativeZipProfiles(String lane) {
        ArrayList<ContentProfile> profiles = new ArrayList<>();
        try {
            String html = Downloader.downloadString("https://gamenative.app/drivers/");
            if (html == null || html.trim().isEmpty()) return profiles;

            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
                    "href\\s*=\\s*\"([^\"]+\\.zip(?:\\?[^\"]*)?)\"",
                    java.util.regex.Pattern.CASE_INSENSITIVE
            ).matcher(html);

            HashSet<String> seen = new HashSet<>();
            int offset = 0;
            while (matcher.find()) {
                String raw = matcher.group(1);
                if (raw == null || raw.trim().isEmpty()) continue;
                String url = raw.startsWith("http://") || raw.startsWith("https://")
                        ? raw.trim()
                        : "https://gamenative.app" + (raw.startsWith("/") ? raw : "/" + raw);
                if (!seen.add(url)) continue;

                String lower = url.toLowerCase(Locale.US);
                if (!matchesAssetLane(lane, lower)) continue;

                String fileName = url.substring(url.lastIndexOf('/') + 1);
                String branch = lower.contains("qcom") ? "qcom"
                        : (lower.contains("turnip") ? "turnip" : "main");

                ContentProfile profile = new ContentProfile();
                profile.type = LANE_OPENGL.equals(lane)
                        ? ContentProfile.ContentType.CONTENT_TYPE_OPENGL_DRIVER
                        : ContentProfile.ContentType.CONTENT_TYPE_TURNIP_DRIVER;
                profile.verName = stripZipSuffix(fileName);
                profile.verCode = (int) ((System.currentTimeMillis() / 1000L) - offset++);
                profile.desc = fileName;
                profile.remoteUrl = url;
                profile.sourceRepo = "gamenative.app/drivers";
                profile.releaseTag = branch;
                profile.displayCategory = LANE_OPENGL.equals(lane) ? "OpenGL Driver" : "Turnip";
                profile.delivery = branch;
                profile.channel = ContentProfile.CHANNEL_STABLE;
                profile.locallyInstalled = isLikelyInstalledDriver(profile.verName);
                profiles.add(profile);
            }
        } catch (Exception ignored) {
        }
        return profiles;
    }

    private int parsePublishedAtVerCode(String publishedAt, int fallback) {
        if (publishedAt == null || publishedAt.trim().isEmpty()) return fallback;
        String digits = publishedAt.replaceAll("[^0-9]", "");
        if (digits.length() >= 10) digits = digits.substring(0, 10);
        try {
            return Integer.parseInt(digits);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private boolean matchesAssetLane(String lane, String lowerName) {
        boolean openGlAsset = lowerName.contains("opengl")
                || lowerName.contains("gallium")
                || lowerName.contains("zink")
                || lowerName.contains("aeopengl")
                || lowerName.contains("gl-driver");
        if (LANE_OPENGL.equals(lane)) return openGlAsset;
        return !openGlAsset;
    }

    private String resolveAssetBranch(String sourceKey, String lowerName, String tag, String releaseName) {
        String source = sourceKey == null ? "" : sourceKey.trim().toLowerCase(Locale.US);
        if ("stevenmxz".equals(source)) {
            if (lowerName.contains("gen8")) return "gen8";
            if (lowerName.contains("_r") || lowerName.contains("-r") || tag.toLowerCase(Locale.US).contains("-r")) return "r-series";
            return "mainline";
        }
        if ("whitebelyash".equals(source)) {
            return "turnip-ci";
        }
        if ("mrpurple".equals(source)) {
            return "purple";
        }
        if ("ae_archive".equals(source)) {
            if (lowerName.contains("experimental") || tag.toLowerCase(Locale.US).contains("exp")) return "experimental";
            return "mainline";
        }
        if (releaseName != null && !releaseName.trim().isEmpty()) return releaseName.trim().toLowerCase(Locale.US);
        return "main";
    }

    private String stripZipSuffix(String value) {
        if (value == null) return "";
        String out = value.trim();
        if (out.toLowerCase(Locale.US).endsWith(".zip")) {
            out = out.substring(0, out.length() - 4);
        }
        return out;
    }

    private boolean isLikelyInstalledDriver(String remoteName) {
        if (remoteName == null || remoteName.trim().isEmpty()) return false;
        String remoteToken = remoteName.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
        if (remoteToken.isEmpty()) return false;
        ArrayList<String> installed = adrenotoolsManager.enumarateInstalledDrivers();
        for (String local : installed) {
            if (local == null) continue;
            String localToken = local.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
            if (localToken.contains(remoteToken) || remoteToken.contains(localToken)) return true;
        }
        return false;
    }

    private void updateBranchSelector(List<ContentProfile> profiles) {
        if (!isAdded() || sGraphicsFeedBranchMode == null) return;
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("all", getString(R.string.graphics_center_branch_all));
        for (ContentProfile profile : profiles) {
            if (profile == null) continue;
            String key = profile.delivery == null ? "" : profile.delivery.trim().toLowerCase(Locale.US);
            if (key.isEmpty()) key = "main";
            if (!options.containsKey(key)) {
                options.put(key, key.replace('-', ' '));
            }
        }

        branchEntries.clear();
        branchValues.clear();
        for (Map.Entry<String, String> option : options.entrySet()) {
            branchValues.add(option.getKey());
            branchEntries.add(option.getValue());
        }

        if (!branchValues.contains(branchMode)) {
            branchMode = "all";
            sharedPreferences.edit().putString(PREF_GRAPHICS_BRANCH_MODE, branchMode).apply();
        }

        suppressBranchCallback = true;
        sGraphicsFeedBranchMode.setAdapter(SpinnerAdapters.create(
                requireContext(),
                sharedPreferences.getBoolean("dark_mode", false),
                branchEntries
        ));
        setSpinnerSelectionByValue(sGraphicsFeedBranchMode, branchValues.toArray(new String[0]), branchMode, 0);
        sGraphicsFeedBranchMode.setEnabled(branchValues.size() > 1);
        suppressBranchCallback = false;
    }

    private List<ContentProfile> applyBranchFilter(List<ContentProfile> profiles, String selectedBranch) {
        if (profiles == null || profiles.isEmpty()) return new ArrayList<>();
        String normalized = selectedBranch == null ? "all" : selectedBranch.trim().toLowerCase(Locale.US);
        if ("all".equals(normalized)) return profiles;

        ArrayList<ContentProfile> filtered = new ArrayList<>();
        for (ContentProfile profile : profiles) {
            if (profile == null) continue;
            String branch = profile.delivery == null ? "" : profile.delivery.trim().toLowerCase(Locale.US);
            if (branch.isEmpty()) branch = "main";
            if (normalized.equals(branch)) filtered.add(profile);
        }
        return filtered;
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
        if (!repo.isEmpty()) {
            String lower = repo.toLowerCase(Locale.US);
            if (lower.contains("stevenmxz")) return "StevenMXZ";
            if (lower.contains("whitebelyash")) return "whitebelyash";
            if (lower.contains("mrpurple")) return "MrPurple";
            if (lower.contains("gamenative")) return "GameNative";
            if (lower.contains("kosoymiki")) return "Ae.solator";
            return repo;
        }
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
        String branch = profile.delivery == null ? "" : profile.delivery.trim();
        if (!branch.isEmpty()) meta.append(" • ").append(branch.replace('-', ' '));
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
            boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
            boolean installed = profile != null && profile.locallyInstalled;
            boolean canInstall = profile != null && profile.remoteUrl != null && !profile.remoteUrl.trim().isEmpty();
            String entryName = ContentsManager.getEntryName(profile);
            boolean isInstalling = installingEntries.contains(entryName);

            holder.ivFeedIcon.setImageResource(profile != null && profile.type == ContentProfile.ContentType.CONTENT_TYPE_OPENGL_DRIVER
                    ? R.drawable.icon_screen_effect
                    : R.drawable.icon_cpu);
            holder.ivFeedIcon.setColorFilter(accent);
            holder.tvFeedTitle.setText(profile.verName == null || profile.verName.trim().isEmpty()
                    ? profile.getDisplayCategory()
                    : profile.verName);
            holder.tvFeedTitle.setTextColor(accent);
            holder.tvFeedMeta.setText(buildFeedMetaLine(profile));
            holder.tvFeedMeta.setTextColor(withAlpha(accent, 205));
            holder.tvFeedSource.setText(buildFeedSourceLabel(profile));
            holder.tvFeedSource.setTextColor(withAlpha(accent, 180));
            GradientDrawable cardBackground = new GradientDrawable();
            cardBackground.setShape(GradientDrawable.RECTANGLE);
            cardBackground.setCornerRadius(dpToPx(12f));
            cardBackground.setColor(withAlpha(accent, isDarkMode ? 56 : 26));
            cardBackground.setStroke(dpToPx(1f), withAlpha(accent, isDarkMode ? 210 : 138));
            holder.itemView.setBackground(cardBackground);

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
