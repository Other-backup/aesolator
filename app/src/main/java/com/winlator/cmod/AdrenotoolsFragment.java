package com.winlator.cmod;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import com.winlator.cmod.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
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
import com.winlator.cmod.contentdialog.ContentDialog;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.core.PreloaderDialog;
import com.winlator.cmod.core.SpinnerAdapters;
import com.winlator.cmod.core.StringUtils;
import com.winlator.cmod.core.UpscalerProfileStore;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class AdrenotoolsFragment extends Fragment {
    private static final String PREF_GRAPHICS_SOURCE_MODE = "graphics_feed_source_mode";
    private static final String PREF_GRAPHICS_BRANCH_MODE = "graphics_feed_branch_mode";
    private static final String PREF_OPEN_X11_DIALOG_ONCE = "graphics_open_x11_dialog_once";
    private static final String LANE_TURNIP = "turnip";
    private static final String LANE_OPENGL = "opengl";
    private static final String UPSCALER_BACKEND_OFF = "off";
    private static final String UPSCALER_BACKEND_VKBASALT = "vkbasalt";
    private static final String UPSCALER_BACKEND_MOBFGSR = "mobfgsr";

    private AdrenotoolsManager adrenotoolsManager;
    private SharedPreferences sharedPreferences;
    private RecyclerView recyclerView;
    private RecyclerView rvGraphicsFeed;
    private View rootView;
    private TextView tvGraphicsCenterStatus;
    private TextView tvGraphicsFeedEmpty;
    private TextView tvGraphicsFeedBranchLabel;
    private Spinner sGraphicsFeedSourceMode;
    private Spinner sGraphicsFeedBranchMode;
    private int selectedLaneButtonId = R.id.BTLaneTurnip;
    private String selectedLane = LANE_TURNIP;
    private String sourceMode = "ae_archive";
    private String branchMode = "all";
    private String[] sourceEntries;
    private String[] sourceValues;
    private final ArrayList<String> activeSourceEntries = new ArrayList<>();
    private final ArrayList<String> activeSourceValues = new ArrayList<>();
    private final ArrayList<String> branchEntries = new ArrayList<>();
    private final ArrayList<String> branchValues = new ArrayList<>();
    private boolean suppressBranchCallback = false;
    private final HashSet<String> installingEntries = new HashSet<>();
    private DriversAdapter driversAdapter;
    private GraphicsFeedAdapter graphicsFeedAdapter;
    private int graphicsFeedRefreshToken = 0;
    private String sourceSelectorSignature = "";
    private String branchSelectorSignature = "";

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
        tvGraphicsFeedBranchLabel = layout.findViewById(R.id.TVGraphicsFeedBranchLabel);
        sGraphicsFeedSourceMode = layout.findViewById(R.id.SGraphicsFeedSourceMode);
        sGraphicsFeedBranchMode = layout.findViewById(R.id.SGraphicsFeedBranchMode);

        sourceEntries = getResources().getStringArray(R.array.graphics_feed_source_entries);
        sourceValues = getResources().getStringArray(R.array.graphics_feed_source_values);

        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        sGraphicsFeedSourceMode.setAdapter(SpinnerAdapters.create(
                requireContext(),
                isDarkMode,
                new ArrayList<>(Collections.singletonList(getString(R.string.graphics_center_driver_feed_loading))))
        );
        sGraphicsFeedBranchMode.setAdapter(SpinnerAdapters.create(
                requireContext(),
                isDarkMode,
                new ArrayList<>(Collections.singletonList(getString(R.string.graphics_center_branch_all)))
        ));
        int popupBackground = isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background;
        sGraphicsFeedSourceMode.setPopupBackgroundResource(popupBackground);
        sGraphicsFeedBranchMode.setPopupBackgroundResource(popupBackground);
        applyFeedSpinnerTheme(isDarkMode);
        branchEntries.clear();
        branchEntries.add(getString(R.string.graphics_center_branch_all));
        branchValues.clear();
        branchValues.add("all");
        updateSourceSelector();
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

        LinearLayoutManager installedLayoutManager = new LinearLayoutManager(recyclerView.getContext());
        installedLayoutManager.setAutoMeasureEnabled(true);
        recyclerView.setLayoutManager(installedLayoutManager);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));
        driversAdapter = new DriversAdapter(adrenotoolsManager.enumarateInstalledDrivers());
        recyclerView.setAdapter(driversAdapter);

        LinearLayoutManager feedLayoutManager = new LinearLayoutManager(rvGraphicsFeed.getContext());
        feedLayoutManager.setAutoMeasureEnabled(true);
        rvGraphicsFeed.setLayoutManager(feedLayoutManager);
        rvGraphicsFeed.setNestedScrollingEnabled(false);
        rvGraphicsFeed.addItemDecoration(new DividerItemDecoration(rvGraphicsFeed.getContext(), DividerItemDecoration.VERTICAL));
        graphicsFeedAdapter = new GraphicsFeedAdapter(new ArrayList<>());
        rvGraphicsFeed.setAdapter(graphicsFeedAdapter);

        View btInstallDriver = layout.findViewById(R.id.BTInstallDriver);
        btInstallDriver.setOnClickListener((v) -> openZipInstaller());

        layout.findViewById(R.id.BTLaneTurnip).setOnClickListener(v ->
                selectGraphicsLane(LANE_TURNIP, R.id.BTLaneTurnip));
        layout.findViewById(R.id.BTLaneOpenGL).setOnClickListener(v ->
                selectGraphicsLane(LANE_OPENGL, R.id.BTLaneOpenGL));

        layout.findViewById(R.id.BTDri3Settings).setOnClickListener(v -> showX11SettingsDialog());

        layout.findViewById(R.id.BTOpenUpscalerSettings).setOnClickListener(v -> showUpscalerSettingsDialog());
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
        updateSourceSelector();
        setSpinnerSelectionByValue(sGraphicsFeedBranchMode, branchValues.toArray(new String[0]), branchMode, 0);
        if (driversAdapter != null) driversAdapter.replaceItems(adrenotoolsManager.enumarateInstalledDrivers());
        styleGraphicsCenterButtons(rootView);
        refreshGraphicsCenterStatus();
        refreshGraphicsFeed();
        maybeOpenPendingX11Dialog();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MainActivity.OPEN_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            String driver = adrenotoolsManager.installDriver(uri);
            if (!driver.isEmpty()) {
                if (driversAdapter != null) driversAdapter.addItem(driver);
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
        sourceMode = getSpinnerSelectedValue(sGraphicsFeedSourceMode, activeSourceValues.toArray(new String[0]), sourceMode);
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
            viewHolder.ivIcon.setImageResource(R.drawable.ae_icon_package);
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

        public void replaceItems(ArrayList<String> updatedDriversList) {
            driversList.clear();
            if (updatedDriversList != null) driversList.addAll(updatedDriversList);
            notifyDataSetChanged();
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
            AdrenotoolsManager.DriverPackageInfo info = adrenotoolsManager.getDriverPackageInfo(entryId);
            if (info == null) return "Installed";

            StringBuilder meta = new StringBuilder("Installed");
            meta.append(" • ").append(info.getDisplayProviderLabel());
            meta.append(" • ").append(info.getDisplayRouteLabel());
            String arch = info.getArchLabel();
            if (!"generic".equalsIgnoreCase(arch)) meta.append(" • ").append(arch);
            String source = info.getSourceLabel();
            if (!source.isEmpty() && !"local package".equalsIgnoreCase(source)) meta.append(" • ").append(source);
            return meta.toString();
        }

        private int resolveInstalledDriverAccent(String entryId) {
            AdrenotoolsManager.DriverPackageInfo info = adrenotoolsManager.getDriverPackageInfo(entryId);
            String normalized = entryId == null ? "" : entryId.toLowerCase(Locale.US);
            boolean openGlLike = info != null
                    ? info.isOpenGlProvider()
                    : normalized.contains("opengl")
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
        updateSourceSelector();
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

    private void showUpscalerSettingsDialog() {
        if (!isAdded()) return;
        ContentDialog dialog = new ContentDialog(requireContext(), R.layout.graphics_upscaler_settings_dialog);
        dialog.setTitle(R.string.graphics_center_upscaler_dialog_title);
        dialog.setBottomBarText(null);

        final boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        final Spinner sProfile = dialog.findViewById(R.id.SUpscalerProfile);
        final Spinner sPreset = dialog.findViewById(R.id.SUpscalerPreset);
        final Spinner sBackend = dialog.findViewById(R.id.SUpscalerBackend);
        final Spinner sEffect = dialog.findViewById(R.id.SUpscalerEffect);
        final Spinner sScale = dialog.findViewById(R.id.SUpscalerScale);
        final CheckBox cbFramegen = dialog.findViewById(R.id.CBEnableFrameGeneration);
        final Spinner sGeneratedFrames = dialog.findViewById(R.id.SGeneratedFrames);
        final Spinner sFgSource = dialog.findViewById(R.id.SUpscalerFgSource);
        final Spinner sFgOutput = dialog.findViewById(R.id.SUpscalerFgOutput);
        final Spinner sFramegenMode = dialog.findViewById(R.id.SUpscalerFramegenMode);
        final CheckBox cbThermalGuard = dialog.findViewById(R.id.CBUpscalerThermalGuard);
        final SeekBar sbTargetFps = dialog.findViewById(R.id.SBUpscalerTargetFps);
        final TextView tvTargetFps = dialog.findViewById(R.id.TVUpscalerTargetFps);
        final SeekBar sbInterpolation = dialog.findViewById(R.id.SBInterpolationFactor);
        final TextView tvInterpolation = dialog.findViewById(R.id.TVInterpolationFactor);
        final CheckBox cbDebugOverlay = dialog.findViewById(R.id.CBUpscalerDebugOverlay);
        final CheckBox cbDebugTear = dialog.findViewById(R.id.CBUpscalerDebugTearLines);
        final CheckBox cbInterpolatedOnly = dialog.findViewById(R.id.CBUpscalerInterpolatedOnly);
        final CheckBox cbVulkanValidation = dialog.findViewById(R.id.CBEnableVulkanValidationLayer);
        final SeekBar sbSharpness = dialog.findViewById(R.id.SBSharpnessLevel);
        final TextView tvSharpness = dialog.findViewById(R.id.TVSharpnessLevel);
        final SeekBar sbDenoise = dialog.findViewById(R.id.SBSharpnessDenoise);
        final TextView tvDenoise = dialog.findViewById(R.id.TVSharpnessDenoise);
        final TextView tvProfileInfo = dialog.findViewById(R.id.TVUpscalerProfileInfo);

        int panelBackground = isDarkMode ? R.drawable.surface_card_background_dark : R.drawable.surface_card_background;
        int commandBackground = isDarkMode ? R.drawable.surface_command_background_dark : R.drawable.surface_command_background;
        int[] cardIds = new int[]{
                R.id.LLUpscalerProfileCard,
                R.id.LLUpscalerCoreCard,
                R.id.LLUpscalerFramegenCard,
                R.id.LLUpscalerDebugCard
        };
        for (int cardId : cardIds) {
            View card = dialog.findViewById(cardId);
            if (card != null) card.setBackgroundResource(panelBackground);
        }
        if (tvProfileInfo != null) {
            tvProfileInfo.setBackgroundResource(commandBackground);
            int profileTextColor = ContextCompat.getColor(
                    requireContext(),
                    isDarkMode ? R.color.surface_badge_text_dark : R.color.surface_badge_text
            );
            tvProfileInfo.setTextColor(profileTextColor);
        }

        int popupBackground = isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background;
        int spinnerBackground = isDarkMode ? R.drawable.combo_box_dark : R.drawable.combo_box;
        Spinner[] allSpinners = new Spinner[]{
                sProfile, sPreset, sBackend, sEffect, sScale, sGeneratedFrames, sFgSource, sFgOutput, sFramegenMode
        };
        for (Spinner spinner : allSpinners) {
            spinner.setPopupBackgroundResource(popupBackground);
            spinner.setBackgroundResource(spinnerBackground);
        }

        final ArrayList<UpscalerProfileStore.Profile> profiles = new ArrayList<>(UpscalerProfileStore.loadProfiles(sharedPreferences));
        final String[] selectedProfileId = new String[]{UpscalerProfileStore.getSelectedProfileId(sharedPreferences)};
        final boolean[] suppressSelectionCallbacks = new boolean[]{false};

        final Runnable updateUiState = () -> {
            String backend = UpscalerProfileStore.normalizeBackend(StringUtils.parseIdentifier(sBackend.getSelectedItem()));
            boolean upscalerEnabled = !UPSCALER_BACKEND_OFF.equals(backend);
            boolean framegenSupported = UPSCALER_BACKEND_VKBASALT.equals(backend) || UPSCALER_BACKEND_MOBFGSR.equals(backend);
            boolean mobfgsrDebug = UPSCALER_BACKEND_MOBFGSR.equals(backend);

            sPreset.setEnabled(upscalerEnabled);
            sEffect.setEnabled(upscalerEnabled);
            sScale.setEnabled(upscalerEnabled);
            cbFramegen.setEnabled(upscalerEnabled && framegenSupported);
            if ((!upscalerEnabled || !framegenSupported) && cbFramegen.isChecked()) cbFramegen.setChecked(false);

            boolean framegenActive = upscalerEnabled && cbFramegen.isChecked();
            sGeneratedFrames.setEnabled(framegenActive);
            sFgSource.setEnabled(framegenActive);
            sFgOutput.setEnabled(framegenActive);
            sFramegenMode.setEnabled(framegenActive);
            cbThermalGuard.setEnabled(framegenActive);
            sbTargetFps.setEnabled(framegenActive);
            sbInterpolation.setEnabled(framegenActive);
            cbDebugOverlay.setEnabled(framegenActive && mobfgsrDebug);
            cbDebugTear.setEnabled(framegenActive && mobfgsrDebug);
            cbInterpolatedOnly.setEnabled(framegenActive && mobfgsrDebug);
            cbVulkanValidation.setEnabled(upscalerEnabled);
            sbSharpness.setEnabled(upscalerEnabled);
            sbDenoise.setEnabled(upscalerEnabled);
        };

        final java.util.function.Consumer<UpscalerProfileStore.Profile> bindProfileToControls = (profile) -> {
            UpscalerProfileStore.Profile safe = UpscalerProfileStore.normalize(profile);
            AppUtils.setSpinnerSelectionFromIdentifier(sPreset, safe.preset);
            AppUtils.setSpinnerSelectionFromIdentifier(sBackend, safe.backend);
            AppUtils.setSpinnerSelectionFromIdentifier(sEffect, safe.effect);
            AppUtils.setSpinnerSelectionFromValue(sScale, String.valueOf(safe.scalePercent));
            cbFramegen.setChecked(safe.frameGeneration);
            AppUtils.setSpinnerSelectionFromValue(sGeneratedFrames, String.valueOf(safe.generatedFrames));
            AppUtils.setSpinnerSelectionFromIdentifier(sFgSource, safe.fgSource);
            AppUtils.setSpinnerSelectionFromIdentifier(sFgOutput, safe.fgOutput);
            AppUtils.setSpinnerSelectionFromIdentifier(sFramegenMode, safe.framegenMode);
            cbThermalGuard.setChecked(safe.thermalGuard);
            sbTargetFps.setProgress(safe.targetFps);
            tvTargetFps.setText(String.valueOf(safe.targetFps));
            sbInterpolation.setProgress(safe.interpolationFactor);
            tvInterpolation.setText(safe.interpolationFactor + "%");
            cbDebugOverlay.setChecked(safe.debugOverlay);
            cbDebugTear.setChecked(safe.debugTearLines);
            cbInterpolatedOnly.setChecked(safe.interpolatedOnly);
            cbVulkanValidation.setChecked(safe.vulkanValidationLayer);
            sbSharpness.setProgress(safe.sharpness);
            tvSharpness.setText(safe.sharpness + "%");
            sbDenoise.setProgress(safe.denoise);
            tvDenoise.setText(safe.denoise + "%");
            tvProfileInfo.setText(getString(R.string.upscaler_profile_info) + "\nID: " + safe.id);
            updateUiState.run();
        };

        final java.util.function.Supplier<UpscalerProfileStore.Profile> readControlsToProfile = () -> {
            UpscalerProfileStore.Profile out = new UpscalerProfileStore.Profile();
            out.preset = UpscalerProfileStore.normalizePreset(StringUtils.parseIdentifier(sPreset.getSelectedItem()));
            out.backend = UpscalerProfileStore.normalizeBackend(StringUtils.parseIdentifier(sBackend.getSelectedItem()));
            out.effect = UpscalerProfileStore.normalizeEffect(StringUtils.parseIdentifier(sEffect.getSelectedItem()));
            out.scalePercent = parseBoundedIntAllowZero(String.valueOf(sScale.getSelectedItem()), 100, 100, 200);
            out.frameGeneration = cbFramegen.isChecked();
            out.generatedFrames = parseBoundedIntAllowZero(String.valueOf(sGeneratedFrames.getSelectedItem()), 1, 1, 3);
            out.fgSource = UpscalerProfileStore.normalizeFgSource(StringUtils.parseIdentifier(sFgSource.getSelectedItem()));
            out.fgOutput = UpscalerProfileStore.normalizeFgOutput(StringUtils.parseIdentifier(sFgOutput.getSelectedItem()));
            out.framegenMode = UpscalerProfileStore.normalizeFramegenMode(StringUtils.parseIdentifier(sFramegenMode.getSelectedItem()));
            out.thermalGuard = cbThermalGuard.isChecked();
            out.targetFps = parseBoundedIntAllowZero(String.valueOf(sbTargetFps.getProgress()), 60, 30, 144);
            out.interpolationFactor = parseBoundedIntAllowZero(String.valueOf(sbInterpolation.getProgress()), 50, 0, 100);
            out.debugOverlay = cbDebugOverlay.isChecked();
            out.debugTearLines = cbDebugTear.isChecked();
            out.interpolatedOnly = cbInterpolatedOnly.isChecked();
            out.vulkanValidationLayer = cbVulkanValidation.isChecked();
            out.sharpness = parseBoundedIntAllowZero(String.valueOf(sbSharpness.getProgress()), 100, 0, 100);
            out.denoise = parseBoundedIntAllowZero(String.valueOf(sbDenoise.getProgress()), 100, 0, 100);
            return UpscalerProfileStore.normalize(out);
        };

        final Runnable refreshProfileSpinner = () -> {
            ArrayList<String> names = new ArrayList<>();
            int selectedIndex = 0;
            for (int i = 0; i < profiles.size(); i++) {
                UpscalerProfileStore.Profile profile = profiles.get(i);
                String label = profile.name + " [" + profile.id + "]";
                names.add(label);
                if (profile.id.equals(selectedProfileId[0])) selectedIndex = i;
            }
            suppressSelectionCallbacks[0] = true;
            sProfile.setAdapter(SpinnerAdapters.create(requireContext(), isDarkMode, names));
            sProfile.setSelection(selectedIndex, false);
            suppressSelectionCallbacks[0] = false;
            if (!profiles.isEmpty()) bindProfileToControls.accept(profiles.get(selectedIndex));
        };

        final Runnable saveCurrentProfile = () -> {
            if (profiles.isEmpty()) return;
            int index = sProfile.getSelectedItemPosition();
            if (index < 0 || index >= profiles.size()) index = 0;
            UpscalerProfileStore.Profile current = profiles.get(index);
            UpscalerProfileStore.Profile edited = readControlsToProfile.get();
            edited.id = current.id;
            edited.name = current.name;
            profiles.set(index, UpscalerProfileStore.normalize(edited));
            selectedProfileId[0] = edited.id;
            UpscalerProfileStore.saveProfiles(sharedPreferences, profiles);
            UpscalerProfileStore.setSelectedProfileId(sharedPreferences, selectedProfileId[0]);
            ForensicLogger.logEvent(
                    requireContext(),
                    "info",
                    "GRAPHICS_UPSCALER_PROFILE_SAVED",
                    null,
                    "graphics_center",
                    "upscaler_profile_saved",
                    ForensicLogger.fields(
                            "profile_id", edited.id,
                            "profile_name", edited.name,
                            "backend", edited.backend,
                            "effect", edited.effect,
                            "framegen", edited.frameGeneration ? "1" : "0"
                    )
            );
            AppUtils.showToast(getContext(), R.string.upscaler_profile_saved);
            refreshProfileSpinner.run();
        };

        sProfile.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressSelectionCallbacks[0]) return;
                if (position < 0 || position >= profiles.size()) return;
                selectedProfileId[0] = profiles.get(position).id;
                bindProfileToControls.accept(profiles.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        sBackend.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateUiState.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateUiState.run();
            }
        });
        cbFramegen.setOnCheckedChangeListener((buttonView, isChecked) -> updateUiState.run());
        sbTargetFps.setOnSeekBarChangeListener(new SimpleSeekbarListener(() ->
                tvTargetFps.setText(String.valueOf(sbTargetFps.getProgress()))
        ));
        sbInterpolation.setOnSeekBarChangeListener(new SimpleSeekbarListener(() ->
                tvInterpolation.setText(sbInterpolation.getProgress() + "%")
        ));
        sbSharpness.setOnSeekBarChangeListener(new SimpleSeekbarListener(() ->
                tvSharpness.setText(sbSharpness.getProgress() + "%")
        ));
        sbDenoise.setOnSeekBarChangeListener(new SimpleSeekbarListener(() ->
                tvDenoise.setText(sbDenoise.getProgress() + "%")
        ));

        dialog.findViewById(R.id.BTUpscalerProfileAdd).setOnClickListener(v -> {
            ContentDialog.prompt(requireContext(), R.string.upscaler_profile_name_prompt, "", name -> {
                UpscalerProfileStore.Profile base = readControlsToProfile.get();
                base.id = makeUniqueProfileId(name, profiles);
                base.name = name.trim();
                profiles.add(UpscalerProfileStore.normalize(base));
                selectedProfileId[0] = base.id;
                refreshProfileSpinner.run();
            });
        });

        dialog.findViewById(R.id.BTUpscalerProfileDuplicate).setOnClickListener(v -> {
            if (profiles.isEmpty()) return;
            int index = sProfile.getSelectedItemPosition();
            if (index < 0 || index >= profiles.size()) index = 0;
            UpscalerProfileStore.Profile source = profiles.get(index);
            ContentDialog.prompt(
                    requireContext(),
                    R.string.upscaler_profile_name_prompt,
                    source.name + " copy",
                    name -> {
                        UpscalerProfileStore.Profile copy = readControlsToProfile.get();
                        copy.id = makeUniqueProfileId(name, profiles);
                        copy.name = name.trim();
                        profiles.add(UpscalerProfileStore.normalize(copy));
                        selectedProfileId[0] = copy.id;
                        refreshProfileSpinner.run();
                    }
            );
        });

        dialog.findViewById(R.id.BTUpscalerProfileRename).setOnClickListener(v -> {
            if (profiles.isEmpty()) return;
            int index = sProfile.getSelectedItemPosition();
            if (index < 0 || index >= profiles.size()) index = 0;
            final int selectedIndex = index;
            UpscalerProfileStore.Profile profile = profiles.get(index);
            ContentDialog.prompt(requireContext(), R.string.upscaler_profile_name_prompt, profile.name, name -> {
                profile.name = name.trim();
                profiles.set(selectedIndex, UpscalerProfileStore.normalize(profile));
                refreshProfileSpinner.run();
            });
        });

        dialog.findViewById(R.id.BTUpscalerProfileRemove).setOnClickListener(v -> {
            if (profiles.isEmpty()) return;
            int index = sProfile.getSelectedItemPosition();
            if (index < 0 || index >= profiles.size()) index = 0;
            UpscalerProfileStore.Profile profile = profiles.get(index);
            if (UpscalerProfileStore.DEFAULT_PROFILE_ID.equals(profile.id)) {
                AppUtils.showToast(getContext(), R.string.upscaler_profile_remove_default);
                return;
            }
            profiles.remove(index);
            if (profiles.isEmpty()) profiles.add(UpscalerProfileStore.defaults());
            selectedProfileId[0] = profiles.get(Math.max(0, index - 1)).id;
            AppUtils.showToast(getContext(), R.string.upscaler_profile_removed);
            refreshProfileSpinner.run();
        });

        refreshProfileSpinner.run();

        dialog.setOnConfirmCallback(saveCurrentProfile);
        dialog.show();
    }

    private String makeUniqueProfileId(String name, List<UpscalerProfileStore.Profile> profiles) {
        String base = UpscalerProfileStore.sanitizeId(name);
        if (base.isEmpty()) base = "profile";
        String candidate = base;
        int seq = 1;
        while (containsProfileId(profiles, candidate)) {
            candidate = base + "_" + seq++;
        }
        return candidate;
    }

    private boolean containsProfileId(List<UpscalerProfileStore.Profile> profiles, String profileId) {
        if (profiles == null || profiles.isEmpty()) return false;
        String normalized = UpscalerProfileStore.sanitizeId(profileId);
        for (UpscalerProfileStore.Profile profile : profiles) {
            if (profile != null && normalized.equals(UpscalerProfileStore.sanitizeId(profile.id))) return true;
        }
        return false;
    }

    private static final class SimpleSeekbarListener implements SeekBar.OnSeekBarChangeListener {
        private final Runnable onChanged;

        private SimpleSeekbarListener(Runnable onChanged) {
            this.onChanged = onChanged;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (onChanged != null) onChanged.run();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }

    private int parseBoundedIntAllowZero(String value, int fallback, int min, int max) {
        if (value == null || value.trim().isEmpty()) return fallback;
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(min, Math.min(max, parsed));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private void maybeOpenPendingX11Dialog() {
        if (!sharedPreferences.getBoolean(PREF_OPEN_X11_DIALOG_ONCE, false)) return;
        sharedPreferences.edit().putBoolean(PREF_OPEN_X11_DIALOG_ONCE, false).apply();
        showX11SettingsDialog();
    }

    private void showX11SettingsDialog() {
        if (!isAdded()) return;
        ContentDialog dialog = new ContentDialog(requireContext(), R.layout.graphics_x11_settings_dialog);
        dialog.setTitle(R.string.graphics_center_x11_settings_title);
        dialog.setBottomBarText(null);

        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        CheckBox cbUseDri3 = dialog.findViewById(R.id.CBX11UseDri3);
        Spinner sDri3Mode = dialog.findViewById(R.id.SX11Dri3Mode);
        CheckBox cbDri3PresentWait = dialog.findViewById(R.id.CBX11Dri3PresentWait);
        CheckBox cbDri3ForceSwWsi = dialog.findViewById(R.id.CBX11Dri3ForceSwWsi);
        CheckBox cbCursorLock = dialog.findViewById(R.id.CBX11CursorLock);
        CheckBox cbXinputToggle = dialog.findViewById(R.id.CBX11XinputToggle);
        View llX11Dri3Card = dialog.findViewById(R.id.LLX11Dri3Card);
        View llX11InputCard = dialog.findViewById(R.id.LLX11InputCard);

        int panelBackground = isDarkMode ? R.drawable.surface_card_background_dark : R.drawable.surface_card_background;
        if (llX11Dri3Card != null) llX11Dri3Card.setBackgroundResource(panelBackground);
        if (llX11InputCard != null) llX11InputCard.setBackgroundResource(panelBackground);

        String[] dri3Entries = getResources().getStringArray(R.array.dri3_mode_entries);
        String[] dri3Values = getResources().getStringArray(R.array.dri3_mode_values);
        sDri3Mode.setAdapter(SpinnerAdapters.create(requireContext(), isDarkMode, dri3Entries));
        sDri3Mode.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
        sDri3Mode.setBackgroundResource(isDarkMode ? R.drawable.combo_box_dark : R.drawable.combo_box);

        cbUseDri3.setChecked(sharedPreferences.getBoolean("use_dri3", true));
        cbDri3PresentWait.setChecked(sharedPreferences.getBoolean("dri3_present_wait", true));
        cbDri3ForceSwWsi.setChecked(sharedPreferences.getBoolean("dri3_force_sw_wsi", false));
        cbCursorLock.setChecked(sharedPreferences.getBoolean("cursor_lock", false));
        cbXinputToggle.setChecked(sharedPreferences.getBoolean("xinput_toggle", false));

        String selectedDri3Mode = sharedPreferences.getString("dri3_mode", cbUseDri3.isChecked() ? "auto" : "off");
        setSpinnerSelectionByValue(sDri3Mode, dri3Values, selectedDri3Mode, 0);
        updateX11Dri3State(cbUseDri3, sDri3Mode, cbDri3PresentWait, cbDri3ForceSwWsi, dri3Values);

        cbUseDri3.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                setSpinnerSelectionByValue(sDri3Mode, dri3Values, "off", 0);
            } else if ("off".equalsIgnoreCase(dri3Values[sDri3Mode.getSelectedItemPosition()])) {
                setSpinnerSelectionByValue(sDri3Mode, dri3Values, "auto", 0);
            }
            updateX11Dri3State(cbUseDri3, sDri3Mode, cbDri3PresentWait, cbDri3ForceSwWsi, dri3Values);
        });

        sDri3Mode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = dri3Values[position];
                cbUseDri3.setChecked(!"off".equalsIgnoreCase(selected));
                updateX11Dri3State(cbUseDri3, sDri3Mode, cbDri3PresentWait, cbDri3ForceSwWsi, dri3Values);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        dialog.setOnConfirmCallback(() -> {
            sharedPreferences.edit()
                    .putBoolean("use_dri3", cbUseDri3.isChecked())
                    .putString("dri3_mode", dri3Values[sDri3Mode.getSelectedItemPosition()])
                    .putBoolean("dri3_present_wait", cbDri3PresentWait.isChecked())
                    .putBoolean("dri3_force_sw_wsi", cbDri3ForceSwWsi.isChecked())
                    .putBoolean("cursor_lock", cbCursorLock.isChecked())
                    .putBoolean("xinput_toggle", cbXinputToggle.isChecked())
                    .apply();
            ForensicLogger.logEvent(
                    requireContext(),
                    "info",
                    "GRAPHICS_X11_SETTINGS_SAVED",
                    null,
                    "graphics_center",
                    "x11_settings_saved",
                    ForensicLogger.fields(
                            "dri3_mode", dri3Values[sDri3Mode.getSelectedItemPosition()],
                            "dri3_present_wait", cbDri3PresentWait.isChecked() ? "1" : "0",
                            "dri3_force_sw_wsi", cbDri3ForceSwWsi.isChecked() ? "1" : "0",
                            "cursor_lock", cbCursorLock.isChecked() ? "1" : "0",
                            "xinput_toggle", cbXinputToggle.isChecked() ? "1" : "0"
                    )
            );
            AppUtils.showToast(getContext(), R.string.diagnostics_saved);
        });
        dialog.show();
    }

    private void updateX11Dri3State(CheckBox cbUseDri3,
                                    Spinner sDri3Mode,
                                    CheckBox cbDri3PresentWait,
                                    CheckBox cbDri3ForceSwWsi,
                                    String[] dri3Values) {
        int selectedIndex = sDri3Mode.getSelectedItemPosition();
        if (selectedIndex < 0 || selectedIndex >= dri3Values.length) selectedIndex = 0;
        String mode = dri3Values[selectedIndex];
        boolean enabled = cbUseDri3.isChecked() && !"off".equalsIgnoreCase(mode);
        cbDri3PresentWait.setEnabled(enabled);
        cbDri3ForceSwWsi.setEnabled(enabled);
    }

    private void styleGraphicsCenterButtons(View root) {
        if (root == null || !isAdded()) return;
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        styleLaneButton(root, R.id.BTLaneTurnip, R.color.contents_lane_turnip, R.color.contents_lane_turnip_dark, isDarkMode, selectedLaneButtonId == R.id.BTLaneTurnip);
        styleLaneButton(root, R.id.BTLaneOpenGL, R.color.contents_lane_opengl, R.color.contents_lane_opengl_dark, isDarkMode, selectedLaneButtonId == R.id.BTLaneOpenGL);
        styleLaneButton(root, R.id.BTDri3Settings, R.color.colorPrimary, R.color.colorAccentDark, isDarkMode, false);
        styleLaneButton(root, R.id.BTOpenUpscalerSettings, R.color.colorPrimary, R.color.colorAccentDark, isDarkMode, false);
        styleLaneButton(root, R.id.BTInstallDriver, R.color.colorPrimary, R.color.colorAccentDark, isDarkMode, false);
    }

    private void applyFeedSpinnerTheme(boolean isDarkMode) {
        SpinnerAdapters.applySurface(sGraphicsFeedSourceMode, isDarkMode);
        SpinnerAdapters.applySurface(sGraphicsFeedBranchMode, isDarkMode);
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
            button.setTextColor(ContextCompat.getColor(
                    requireContext(),
                    isDarkMode ? R.color.surface_badge_text_dark : R.color.surface_table_head_text
            ));
        } else {
            bg.setColor(withAlpha(accent, isDarkMode ? 62 : 26));
            bg.setStroke(dpToPx(1f), withAlpha(accent, isDarkMode ? 238 : 180));
            button.setTextColor(isDarkMode
                    ? ContextCompat.getColor(requireContext(), R.color.surface_badge_text_dark)
                    : accent);
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
        final int requestToken = ++graphicsFeedRefreshToken;
        if (graphicsFeedAdapter == null || graphicsFeedAdapter.getItemCount() == 0) {
            tvGraphicsFeedEmpty.setText(R.string.graphics_center_driver_feed_loading);
            tvGraphicsFeedEmpty.setVisibility(View.VISIBLE);
        }
        rvGraphicsFeed.setVisibility(View.VISIBLE);

        new Thread(() -> {
            if (!isAdded()) return;
            List<ContentProfile> sourceProfiles = collectSourceProfiles(selectedLane, sourceMode);
            if (!isAdded() || getActivity() == null) return;
            requireActivity().runOnUiThread(() -> {
                if (requestToken != graphicsFeedRefreshToken) return;
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
            rvGraphicsFeed.setVisibility(View.VISIBLE);
            if (graphicsFeedAdapter != null) graphicsFeedAdapter.setProfiles(new ArrayList<>());
            return;
        }
        tvGraphicsFeedEmpty.setVisibility(View.GONE);
        rvGraphicsFeed.setVisibility(View.VISIBLE);
        if (graphicsFeedAdapter != null) graphicsFeedAdapter.setProfiles(profiles);
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

        if (profiles.isEmpty()) {
            profiles.addAll(buildStaticFallbackProfiles(source, lane));
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
                    profile.locallyInstalled = isLikelyInstalledDriver(profile);
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
            LinkedHashSet<String> visitedPages = new LinkedHashSet<>();
            ArrayList<String> pendingPages = new ArrayList<>();
            pendingPages.add("https://gamenative.app/drivers/");
            LinkedHashMap<String, RemoteZipCandidate> candidates = new LinkedHashMap<>();

            int pageIndex = 0;
            while (pageIndex < pendingPages.size() && pageIndex < 18) {
                String pageUrl = pendingPages.get(pageIndex++);
                if (!visitedPages.add(pageUrl)) continue;
                String html = Downloader.downloadString(pageUrl);
                if (html == null || html.trim().isEmpty()) continue;

                collectGameNativeZipCandidates(html, pageUrl, candidates);
                for (String child : extractGameNativeChildPages(html, pageUrl)) {
                    if (!visitedPages.contains(child) && !pendingPages.contains(child)) {
                        pendingPages.add(child);
                    }
                }
            }

            int fallbackCode = (int) (System.currentTimeMillis() / 1000L);
            int offset = 0;
            for (RemoteZipCandidate candidate : candidates.values()) {
                String laneHint = (candidate.url + " " + candidate.hint + " " + candidate.sourcePage).toLowerCase(Locale.US);
                if (!matchesAssetLane(lane, laneHint)) continue;

                String fileName = candidate.url.substring(candidate.url.lastIndexOf('/') + 1);
                String branch = resolveGameNativeBranch(laneHint);
                ContentProfile profile = new ContentProfile();
                profile.type = LANE_OPENGL.equals(lane)
                        ? ContentProfile.ContentType.CONTENT_TYPE_OPENGL_DRIVER
                        : ContentProfile.ContentType.CONTENT_TYPE_TURNIP_DRIVER;
                profile.verName = stripZipSuffix(fileName);
                profile.verCode = deriveGameNativeVerCode(fileName, fallbackCode - offset++);
                profile.desc = (candidate.hint == null || candidate.hint.trim().isEmpty()) ? fileName : candidate.hint.trim();
                profile.remoteUrl = candidate.url;
                profile.sourceRepo = "gamenative.app/drivers";
                profile.releaseTag = branch;
                profile.displayCategory = LANE_OPENGL.equals(lane) ? "OpenGL Driver" : "Turnip";
                profile.delivery = branch;
                profile.channel = ContentProfile.CHANNEL_STABLE;
                profile.locallyInstalled = isLikelyInstalledDriver(profile);
                profiles.add(profile);
            }
        } catch (Exception ignored) {
        }
        return profiles;
    }

    private static final class RemoteZipCandidate {
        private final String url;
        private final String hint;
        private final String sourcePage;

        private RemoteZipCandidate(String url, String hint, String sourcePage) {
            this.url = url;
            this.hint = hint;
            this.sourcePage = sourcePage;
        }
    }

    private void collectGameNativeZipCandidates(String html,
                                                String pageUrl,
                                                LinkedHashMap<String, RemoteZipCandidate> outCandidates) {
        if (html == null || html.trim().isEmpty() || outCandidates == null) return;

        String[] patterns = new String[]{
                "(?i)(?:href|data-href|data-url|src)\\s*=\\s*\"([^\"]+?\\.zip(?:\\?[^\"#]*)?)\"",
                "(?i)(?:href|data-href|data-url|src)\\s*=\\s*'([^']+?\\.zip(?:\\?[^'#]*)?)'",
                "(?i)(?:href|data-href|data-url|src)\\s*=\\s*([^\\s\"'>]+?\\.zip(?:\\?[^\\s\"'>#]*)?)",
                "(?i)(https?://[^\\s\"'<>]+?\\.zip(?:\\?[^\\s\"'<>#]*)?)"
        };

        for (String patternText : patterns) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(patternText).matcher(html);
            while (matcher.find()) {
                String raw = matcher.group(1);
                String normalizedUrl = normalizeGameNativeUrl(raw, pageUrl);
                if (normalizedUrl.isEmpty()) continue;
                if (outCandidates.containsKey(normalizedUrl)) continue;
                String hint = extractGameNativeHint(html, matcher.start(), matcher.end());
                outCandidates.put(normalizedUrl, new RemoteZipCandidate(normalizedUrl, hint, pageUrl));
            }
        }

        java.util.regex.Matcher directDownloads = java.util.regex.Pattern.compile(
                "(?i)https?:\\\\?/\\\\?/downloads\\.gamenative\\.app/[^\\s\"'<>]+?\\.zip"
        ).matcher(html);
        while (directDownloads.find()) {
            String raw = directDownloads.group();
            String normalizedUrl = normalizeGameNativeUrl(raw, pageUrl);
            if (normalizedUrl.isEmpty()) continue;
            if (outCandidates.containsKey(normalizedUrl)) continue;
            String hint = extractGameNativeHint(html, directDownloads.start(), directDownloads.end());
            outCandidates.put(normalizedUrl, new RemoteZipCandidate(normalizedUrl, hint, pageUrl));
        }
    }

    private List<String> extractGameNativeChildPages(String html, String pageUrl) {
        ArrayList<String> pages = new ArrayList<>();
        if (html == null || html.trim().isEmpty()) return pages;

        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
                "(?i)href\\s*=\\s*\"([^\"]+)\"|href\\s*=\\s*'([^']+)'"
        ).matcher(html);
        while (matcher.find()) {
            String raw = matcher.group(1);
            if (raw == null || raw.trim().isEmpty()) raw = matcher.group(2);
            String normalized = normalizeGameNativeUrl(raw, pageUrl);
            if (normalized.isEmpty()) continue;
            String lower = normalized.toLowerCase(Locale.US);
            if (!lower.startsWith("https://gamenative.app/")) continue;
            if (!lower.contains("/drivers")) continue;
            if (lower.endsWith(".zip")) continue;
            if (!pages.contains(normalized)) pages.add(normalized);
        }
        return pages;
    }

    private String normalizeGameNativeUrl(String raw, String basePageUrl) {
        if (raw == null) return "";
        String normalized = raw.trim()
                .replace("&amp;", "&")
                .replace("\\/", "/")
                .replace("\\u002F", "/")
                .replace("\\u003A", ":");
        while (normalized.endsWith("\\") || normalized.endsWith("\"") || normalized.endsWith("'")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isEmpty() || normalized.startsWith("#") || normalized.startsWith("javascript:")) return "";
        try {
            java.net.URI baseUri = new java.net.URI(basePageUrl == null ? "https://gamenative.app/drivers/" : basePageUrl);
            java.net.URI resolved = baseUri.resolve(normalized);
            String out = resolved.toString();
            if (!out.startsWith("http://") && !out.startsWith("https://")) return "";
            out = out.replaceAll("\\\\+$", "");
            while (out.endsWith("/") && !out.endsWith("://")) out = out.substring(0, out.length() - 1);
            return out;
        } catch (Exception ignored) {
            return "";
        }
    }

    private String extractGameNativeHint(String html, int start, int end) {
        int left = Math.max(0, start - 110);
        int right = Math.min(html.length(), end + 160);
        String snippet = html.substring(left, right);
        return snippet
                .replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String resolveGameNativeBranch(String lower) {
        if (lower.contains("qcom-")) {
            return "qcom-opengl";
        }
        if (lower.contains("adreno_") || lower.contains("adreno-")) {
            return "adreno-opengl";
        }
        if (lower.contains("8elite2")) return "8elite2-opengl";
        if (lower.contains("8elite")) return "8elite-opengl";
        if (lower.contains("8egen5")) return "8egen5-opengl";
        if (lower.contains("8e-800") || lower.contains("a8xx") || lower.contains("gen8")) return "gen8-turnip";
        if (lower.contains("one_ui7")) return "turnip-oneui7";
        if (lower.contains("_a32")) return "turnip-a32";
        if (lower.contains("_mem")) return "turnip-mem";
        if (lower.contains("turnip") || lower.contains("freedreno")) return "turnip-rseries";
        return "main";
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
        String value = lowerName == null ? "" : lowerName.toLowerCase(Locale.US);
        boolean turnipAsset = value.contains("turnip")
                || value.contains("freedreno")
                || value.contains("turnip_")
                || value.contains("turnip-")
                || value.contains("gen8")
                || value.contains("a8xx")
                || value.contains("8e-800");
        boolean openGlAsset = value.contains("opengl")
                || value.contains("gallium")
                || value.contains("zink")
                || value.contains("aeopengl")
                || value.contains("gl-driver")
                || value.contains("qcom")
                || value.contains("qualcomm")
                || value.contains("adreno")
                || value.contains("8elite")
                || value.contains("8elite2")
                || value.contains("8egen5");
        if (LANE_OPENGL.equals(lane)) return openGlAsset && !turnipAsset;
        if (turnipAsset) return true;
        return !openGlAsset;
    }

    private String resolveAssetBranch(String sourceKey, String lowerName, String tag, String releaseName) {
        String source = sourceKey == null ? "" : sourceKey.trim().toLowerCase(Locale.US);
        if ("stevenmxz".equals(source)) {
            if (lowerName.contains("gen8")) return "gen8";
            if (lowerName.contains("autotuner")) return "autotuner";
            if (lowerName.contains("a6xx") && lowerName.contains("fix")) return "a6xx-fix";
            if (lowerName.contains("_r") || lowerName.contains("-r") || tag.toLowerCase(Locale.US).contains("-r")) return "r-series";
            return "mainline";
        }
        if ("whitebelyash".equals(source)) {
            if (lowerName.contains("a8xx") || lowerName.contains("gen8")) return "a8xx-gen8";
            if (lowerName.contains("noflushall")) return "noflushall";
            if (lowerName.contains("flushall")) return "flushall";
            if (lowerName.contains("main")) return "mainline";
            return "turnip-ci";
        }
        if ("mrpurple".equals(source)) {
            if (lowerName.contains("ayaneo")) return "ayaneo";
            if (lowerName.contains("toasted")) return "toasted";
            return "purple";
        }
        if ("ae_archive".equals(source)) {
            if (lowerName.contains("experimental") || tag.toLowerCase(Locale.US).contains("exp")) return "experimental";
            return "mainline";
        }
        if (releaseName != null && !releaseName.trim().isEmpty()) return releaseName.trim().toLowerCase(Locale.US);
        return "main";
    }

    private List<ContentProfile> buildStaticFallbackProfiles(String sourceKey, String lane) {
        ArrayList<ContentProfile> profiles = new ArrayList<>();
        String source = sourceKey == null ? "" : sourceKey.trim().toLowerCase(Locale.US);
        if (LANE_TURNIP.equals(lane) && ("stevenmxz".equals(source) || "ae_archive".equals(source))) {
            String sourceRepo = "stevenmxz".equals(source)
                    ? "StevenMXZ/freedreno_turnip-CI"
                    : "Ae.solator archive";
            profiles.add(buildStaticProfile(
                    lane,
                    "Turnip Gen8 V26",
                    "https://github.com/StevenMXZ/freedreno_turnip-CI/releases/download/v26.1.2/Turnip_Gen8_V26.zip",
                    sourceRepo,
                    "v26.1.2",
                    "gen8",
                    261200
            ));
            profiles.add(buildStaticProfile(
                    lane,
                    "Turnip v26.1.0 R4",
                    "https://github.com/StevenMXZ/freedreno_turnip-CI/releases/download/v26.1.0-R4/Turnip_v26.1.0_R4.zip",
                    sourceRepo,
                    "v26.1.0-R4",
                    "r-series",
                    261004
            ));
        }
        if (LANE_TURNIP.equals(lane) && "whitebelyash".equals(source)) {
            String sourceRepo = "whitebelyash/freedreno_turnip-CI";
            profiles.add(buildStaticProfile(
                    lane,
                    "mesa-turnip-main-V26.1.0-git_3",
                    "https://github.com/whitebelyash/freedreno_turnip-CI/releases/download/mesa_v26.1.0-git_3/mesa-turnip-main-V26.1.0-git_3.zip",
                    sourceRepo,
                    "mesa_v26.1.0-git_3",
                    "mainline",
                    261003
            ));
            profiles.add(buildStaticProfile(
                    lane,
                    "mesa-turnip-main-flushall-V26.1.0-git-hotfix",
                    "https://github.com/whitebelyash/freedreno_turnip-CI/releases/download/mesa_v26.1.0-git-hotfix/mesa-turnip-main-flushall-V26.1.0-git-hotfix.zip",
                    sourceRepo,
                    "mesa_v26.1.0-git-hotfix",
                    "flushall",
                    261002
            ));
            profiles.add(buildStaticProfile(
                    lane,
                    "mesa-turnip-main-noflushall-V26.1.0-git-hotfix",
                    "https://github.com/whitebelyash/freedreno_turnip-CI/releases/download/mesa_v26.1.0-git-hotfix/mesa-turnip-main-noflushall-V26.1.0-git-hotfix.zip",
                    sourceRepo,
                    "mesa_v26.1.0-git-hotfix",
                    "noflushall",
                    261001
            ));
            profiles.add(buildStaticProfile(
                    lane,
                    "a8xx-gen8-V23",
                    "https://github.com/whitebelyash/freedreno_turnip-CI/releases/download/tu_v23/a8xx-gen8-V23.zip",
                    sourceRepo,
                    "tu_v23",
                    "a8xx-gen8",
                    260923
            ));
        }
        if (LANE_TURNIP.equals(lane) && "mrpurple".equals(source)) {
            String sourceRepo = "MrPurple666/purple-turnip";
            profiles.add(buildStaticProfile(
                    lane,
                    "turnip_mrpurple_T24-toasted",
                    "https://github.com/MrPurple666/purple-turnip/releases/download/vturnip_mrpurple_T24-toasted.adpkg/turnip_mrpurple_T24-toasted.adpkg.zip",
                    sourceRepo,
                    "vturnip_mrpurple_T24-toasted.adpkg",
                    "toasted",
                    240024
            ));
            profiles.add(buildStaticProfile(
                    lane,
                    "turnip_mrpurple_T21-AYANEO",
                    "https://github.com/MrPurple666/purple-turnip/releases/download/vturnip_mrpurple_T21-AYANEO.adpkg/turnip_mrpurple_T21-AYANEO.adpkg.zip",
                    sourceRepo,
                    "vturnip_mrpurple_T21-AYANEO.adpkg",
                    "ayaneo",
                    240021
            ));
        }
        if (LANE_OPENGL.equals(lane) && "gamenative".equals(source)) {
            String sourceRepo = "gamenative.app/drivers";
            profiles.add(buildStaticProfile(
                    lane,
                    "qcom-849",
                    "https://downloads.gamenative.app/drivers/qcom-849.zip",
                    sourceRepo,
                    "qcom-849",
                    "qcom-opengl",
                    849000
            ));
            profiles.add(buildStaticProfile(
                    lane,
                    "8Elite2-842.6",
                    "https://downloads.gamenative.app/drivers/8Elite2-842.6.zip",
                    sourceRepo,
                    "8Elite2-842.6",
                    "8elite2-opengl",
                    842006
            ));
            profiles.add(buildStaticProfile(
                    lane,
                    "Adreno_819",
                    "https://downloads.gamenative.app/drivers/Adreno_819.zip",
                    sourceRepo,
                    "Adreno_819",
                    "adreno-opengl",
                    819000
            ));
        }
        return profiles;
    }

    private ContentProfile buildStaticProfile(String lane,
                                              String verName,
                                              String remoteUrl,
                                              String sourceRepo,
                                              String releaseTag,
                                              String delivery,
                                              int verCode) {
        ContentProfile profile = new ContentProfile();
        profile.type = LANE_OPENGL.equals(lane)
                ? ContentProfile.ContentType.CONTENT_TYPE_OPENGL_DRIVER
                : ContentProfile.ContentType.CONTENT_TYPE_TURNIP_DRIVER;
        profile.verName = verName;
        profile.verCode = verCode;
        profile.desc = verName + " (fallback mirror)";
        profile.remoteUrl = remoteUrl;
        profile.sourceRepo = sourceRepo;
        profile.releaseTag = releaseTag;
        profile.delivery = delivery;
        profile.channel = ContentProfile.CHANNEL_STABLE;
        profile.displayCategory = LANE_OPENGL.equals(lane) ? "OpenGL Driver" : "Turnip";
        profile.locallyInstalled = isLikelyInstalledDriver(profile);
        return profile;
    }

    private int deriveGameNativeVerCode(String fileName, int fallback) {
        if (fileName == null || fileName.trim().isEmpty()) return fallback;
        String digits = fileName.replaceAll("[^0-9]", "");
        if (digits.length() > 9) digits = digits.substring(0, 9);
        if (digits.isEmpty()) return fallback;
        try {
            return Integer.parseInt(digits);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String stripZipSuffix(String value) {
        if (value == null) return "";
        String out = value.trim();
        if (out.toLowerCase(Locale.US).endsWith(".zip")) {
            out = out.substring(0, out.length() - 4);
        }
        return out;
    }

    private boolean isLikelyInstalledDriver(ContentProfile remoteProfile) {
        if (remoteProfile == null) return false;
        ArrayList<AdrenotoolsManager.DriverPackageInfo> installed = adrenotoolsManager.enumerateInstalledDriverPackages();
        for (AdrenotoolsManager.DriverPackageInfo info : installed) {
            if (matchesInstalledDriverProfile(remoteProfile, info)) return true;
        }
        return false;
    }

    private boolean matchesInstalledDriverProfile(ContentProfile remoteProfile, AdrenotoolsManager.DriverPackageInfo info) {
        if (remoteProfile == null || info == null) return false;
        String expectedLane = remoteProfile.type == ContentProfile.ContentType.CONTENT_TYPE_OPENGL_DRIVER
                ? "freedreno-opengl"
                : "turnip-vulkan";
        if (!info.providerLane.isEmpty() && !expectedLane.equalsIgnoreCase(info.providerLane)) return false;

        String remoteNameToken = normalizeDriverToken(remoteProfile.verName);
        String remoteTagToken = normalizeDriverToken(remoteProfile.releaseTag);
        String remoteDescToken = normalizeDriverToken(remoteProfile.desc);

        ArrayList<String> localTokens = new ArrayList<>();
        localTokens.add(normalizeDriverToken(info.entryId));
        localTokens.add(normalizeDriverToken(info.name));
        localTokens.add(normalizeDriverToken(stripZipSuffix(info.artifactName)));
        localTokens.add(normalizeDriverToken(info.releaseTag));
        localTokens.add(normalizeDriverToken(info.driverVersion));

        for (String localToken : localTokens) {
            if (localToken.isEmpty()) continue;
            if (!remoteNameToken.isEmpty() && localToken.equals(remoteNameToken)) return true;
            if (!remoteTagToken.isEmpty() && localToken.equals(remoteTagToken)) return true;
        }

        boolean sameSource = info.sourceRepo != null
                && remoteProfile.sourceRepo != null
                && !info.sourceRepo.trim().isEmpty()
                && info.sourceRepo.equalsIgnoreCase(remoteProfile.sourceRepo);

        for (String localToken : localTokens) {
            if (localToken.isEmpty()) continue;
            if (remoteNameToken.length() >= 8 && (localToken.contains(remoteNameToken) || remoteNameToken.contains(localToken))) {
                return true;
            }
            if (sameSource && remoteDescToken.length() >= 10
                    && (localToken.contains(remoteDescToken) || remoteDescToken.contains(localToken))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeDriverToken(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
    }

    private void updateSourceSelector() {
        if (!isAdded() || sGraphicsFeedSourceMode == null) return;
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        boolean openGlLane = LANE_OPENGL.equals(selectedLane);

        for (int i = 0; i < sourceValues.length && i < sourceEntries.length; i++) {
            String value = sourceValues[i];
            if (value == null || value.trim().isEmpty()) continue;
            String normalized = value.trim().toLowerCase(Locale.US);
            if (openGlLane && !supportsOpenGlFeedSource(normalized)) continue;
            options.put(normalized, sourceEntries[i]);
        }

        activeSourceEntries.clear();
        activeSourceValues.clear();
        for (Map.Entry<String, String> option : options.entrySet()) {
            activeSourceValues.add(option.getKey());
            activeSourceEntries.add(option.getValue());
        }

        if (activeSourceValues.isEmpty()) {
            activeSourceValues.add("ae_archive");
            activeSourceEntries.add(getString(R.string.graphics_feed_source_ae_archive));
        }
        if (!activeSourceValues.contains(sourceMode)) {
            sourceMode = activeSourceValues.get(0);
            sharedPreferences.edit().putString(PREF_GRAPHICS_SOURCE_MODE, sourceMode).apply();
        }

        String nextSignature = selectedLane + "|" + String.join("|", activeSourceValues);
        boolean needsAdapterReset = !nextSignature.equals(sourceSelectorSignature);
        sourceSelectorSignature = nextSignature;
        if (needsAdapterReset) {
            sGraphicsFeedSourceMode.setAdapter(SpinnerAdapters.create(
                    requireContext(),
                    sharedPreferences.getBoolean("dark_mode", false),
                    activeSourceEntries
            ));
        }
        setSpinnerSelectionByValue(sGraphicsFeedSourceMode, activeSourceValues.toArray(new String[0]), sourceMode, 0);
        sGraphicsFeedSourceMode.setEnabled(activeSourceValues.size() > 1);
    }

    private boolean supportsOpenGlFeedSource(String source) {
        return "ae_archive".equals(source) || "gamenative".equals(source);
    }

    private void updateBranchSelector(List<ContentProfile> profiles) {
        if (!isAdded() || sGraphicsFeedBranchMode == null) return;
        LinkedHashSet<String> uniqueBranches = new LinkedHashSet<>();
        for (ContentProfile profile : profiles) {
            if (profile == null) continue;
            String key = profile.delivery == null ? "" : profile.delivery.trim().toLowerCase(Locale.US);
            if (key.isEmpty()) key = "main";
            uniqueBranches.add(key);
        }

        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        if (uniqueBranches.size() <= 1) {
            String onlyBranch = uniqueBranches.isEmpty() ? "main" : uniqueBranches.iterator().next();
            options.put(onlyBranch, formatReleaseLineLabel(onlyBranch));
        } else {
            options.put("all", getString(R.string.graphics_center_branch_all));
            for (String key : uniqueBranches) {
                if (!options.containsKey(key)) options.put(key, formatReleaseLineLabel(key));
            }
        }

        branchEntries.clear();
        branchValues.clear();
        for (Map.Entry<String, String> option : options.entrySet()) {
            branchValues.add(option.getKey());
            branchEntries.add(option.getValue());
        }

        if (!branchValues.contains(branchMode)) {
            branchMode = branchValues.get(0);
            sharedPreferences.edit().putString(PREF_GRAPHICS_BRANCH_MODE, branchMode).apply();
        }

        String nextSignature = String.join("|", branchValues);
        boolean needsAdapterReset = !nextSignature.equals(branchSelectorSignature);
        branchSelectorSignature = nextSignature;
        suppressBranchCallback = true;
        if (needsAdapterReset) {
            sGraphicsFeedBranchMode.setAdapter(SpinnerAdapters.create(
                    requireContext(),
                    sharedPreferences.getBoolean("dark_mode", false),
                    branchEntries
            ));
        }
        setSpinnerSelectionByValue(sGraphicsFeedBranchMode, branchValues.toArray(new String[0]), branchMode, 0);
        boolean hasSyntheticAllOption = !branchValues.isEmpty() && "all".equals(branchValues.get(0));
        sGraphicsFeedBranchMode.setEnabled(hasSyntheticAllOption ? branchValues.size() > 2 : branchValues.size() > 1);
        ViewGroup.LayoutParams layoutParams = sGraphicsFeedBranchMode.getLayoutParams();
        if (layoutParams != null) {
            layoutParams.width = uniqueBranches.size() <= 1 ? ViewGroup.LayoutParams.WRAP_CONTENT : ViewGroup.LayoutParams.MATCH_PARENT;
            sGraphicsFeedBranchMode.setLayoutParams(layoutParams);
        }
        updateBranchLabel(uniqueBranches.size());
        suppressBranchCallback = false;
    }

    private void updateBranchLabel(int uniqueBranchCount) {
        if (tvGraphicsFeedBranchLabel == null) return;
        int labelRes;
        if ("gamenative".equals(sourceMode)) {
            labelRes = uniqueBranchCount <= 1
                    ? R.string.graphics_center_driver_line_single
                    : R.string.graphics_center_driver_line_selector;
        } else {
            labelRes = uniqueBranchCount <= 1
                    ? R.string.graphics_center_release_line_single
                    : R.string.graphics_center_branch_selector;
        }
        tvGraphicsFeedBranchLabel.setText(labelRes);
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

    private String formatReleaseLineLabel(String key) {
        if (key == null || key.trim().isEmpty()) return "Mainline";
        String normalized = key.trim().toLowerCase(Locale.US);
        return switch (normalized) {
            case "main", "mainline" -> "Mainline";
            case "experimental" -> "Experimental";
            case "r-series", "rseries" -> "R-series";
            case "gen8" -> "Gen8";
            case "autotuner" -> "Autotuner";
            case "a6xx-fix" -> "A6xx Fix";
            case "a8xx-gen8", "gen8-turnip" -> "A8XX Gen8";
            case "qcom-opengl" -> "QCOM OpenGL";
            case "adreno-opengl" -> "Adreno OpenGL";
            case "8elite-opengl" -> "8 Elite OpenGL";
            case "8elite2-opengl" -> "8 Elite 2 OpenGL";
            case "8egen5-opengl" -> "8e Gen5 OpenGL";
            case "turnip-rseries" -> "Turnip R-series";
            case "turnip-mem" -> "Turnip MEM";
            case "turnip-oneui7" -> "One UI 7 Fix";
            case "turnip-a32" -> "A32";
            case "flushall" -> "Flushall";
            case "noflushall" -> "No Flushall";
            case "toasted" -> "Toasted";
            case "ayaneo" -> "AYANEO";
            case "purple" -> "Purple";
            case "turnip-ci" -> "Turnip CI";
            default -> toTitleCase(normalized.replace('-', ' '));
        };
    }

    private String toTitleCase(String value) {
        if (value == null || value.trim().isEmpty()) return "";
        String[] parts = value.trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) out.append(part.substring(1));
        }
        return out.toString();
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
            if (lower.contains("kosoymiki")) return "Ae Archive";
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
        if (profile.type == ContentProfile.ContentType.CONTENT_TYPE_OPENGL_DRIVER) {
            meta.append(" • Freedreno Gallium");
        } else if (profile.type == ContentProfile.ContentType.CONTENT_TYPE_TURNIP_DRIVER) {
            meta.append(" • Turnip Vulkan");
        }
        String branch = profile.delivery == null ? "" : profile.delivery.trim();
        if (!branch.isEmpty()) meta.append(" • ").append(formatReleaseLineLabel(branch));
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
                    if (driversAdapter != null) driversAdapter.replaceItems(adrenotoolsManager.enumarateInstalledDrivers());
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
            this.profiles = new ArrayList<>();
            if (profiles != null) this.profiles.addAll(profiles);
            setHasStableIds(true);
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
                    ? R.drawable.ae_icon_opengl_lane
                    : R.drawable.ae_icon_turnip_lane);
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
                holder.btFeedInstall.setImageResource(canInstall ? R.drawable.ae_icon_download : R.drawable.ae_icon_confirm);
                holder.btFeedInstall.setContentDescription(getString(canInstall
                        ? R.string.graphics_center_update_action
                        : R.string.graphics_center_installed_action));
            } else {
                holder.btFeedInstall.setImageResource(R.drawable.ae_icon_download);
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

        @Override
        public long getItemId(int position) {
            if (position < 0 || position >= profiles.size()) return RecyclerView.NO_ID;
            ContentProfile profile = profiles.get(position);
            if (profile == null) return RecyclerView.NO_ID;
            String idSeed = (profile.remoteUrl == null ? "" : profile.remoteUrl)
                    + "|"
                    + (profile.verName == null ? "" : profile.verName)
                    + "|"
                    + profile.verCode;
            return idSeed.hashCode();
        }

        private void setProfiles(List<ContentProfile> updatedProfiles) {
            profiles.clear();
            if (updatedProfiles != null) profiles.addAll(updatedProfiles);
            notifyDataSetChanged();
        }
    }
}
