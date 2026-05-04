package com.winlator.cmod.contentdialog;



import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.tabs.TabLayout;
import com.winlator.cmod.ContainerDetailFragment;
import com.winlator.cmod.R;
import com.winlator.cmod.ShortcutsFragment;
import com.winlator.cmod.box64.Box64PresetManager;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.container.GraphicsDrivers;
import com.winlator.cmod.container.Shortcut;
import com.winlator.cmod.contents.AdrenotoolsManager;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.DefaultVersion;
import com.winlator.cmod.core.EnvVars;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.core.SpinnerAdapters;
import com.winlator.cmod.core.StringUtils;
import com.winlator.cmod.core.UpscalerProfileStore;
import com.winlator.cmod.core.WineInfo;
import com.winlator.cmod.fexcore.FEXCoreManager;
import com.winlator.cmod.fexcore.FEXCorePreset;
import com.winlator.cmod.fexcore.FEXCorePresetManager;
import com.winlator.cmod.inputcontrols.ControlsProfile;
import com.winlator.cmod.inputcontrols.InputControlsManager;
import com.winlator.cmod.midi.MidiManager;
import com.winlator.cmod.widget.CPUListView;
import com.winlator.cmod.widget.EnvVarsView;
import com.winlator.cmod.winhandler.WinHandler;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ShortcutSettingsDialog extends ContentDialog {
    private static final String TOUCHPAD_PROFILE_GLOBAL = "global";
    private static final String TOUCHPAD_PROFILE_BALANCED = "balanced";
    private static final String TOUCHPAD_PROFILE_AGGRESSIVE = "aggressive";
    private static final String TOUCHPAD_PROFILE_COMPAT = "compat";
    private static final String UPSCALER_BACKEND_OFF = "off";
    private static final String UPSCALER_BACKEND_VKBASALT = "vkbasalt";
    private static final String UPSCALER_BACKEND_LSFG = "lsfg";
    private static final String UPSCALER_EFFECT_NONE = "none";
    private static final String FG_SOURCE_NATIVE = "native";
    private static final String FG_SOURCE_OPTI_FG = "opti_fg";
    private static final String FG_OUTPUT_AUTO = "auto";
    private static final String FG_OUTPUT_LSFG = "lsfg";
    private static final String FRAMEGEN_MODE_BALANCED = "balanced";
    private static final String FRAMEGEN_MODE_QUALITY = "quality";
    private static final String FRAMEGEN_MODE_LOW_LATENCY = "low_latency";
    private static final String UPSCALER_PRESET_AUTO = "auto";
    private static final String UPSCALER_PRESET_CONSERVATIVE = "conservative";
    private static final String UPSCALER_PRESET_BALANCED = "balanced";
    private static final String UPSCALER_PRESET_AGGRESSIVE = "aggressive";
    private final ShortcutsFragment fragment;
    private final Shortcut shortcut;
    private InputControlsManager inputControlsManager;
    private TextView tvGraphicsDriverVersion;
    private String box64Version;

    private static final class TouchpadGestureDefaults {
        final boolean strictFsm;
        final int tapTimeoutMs;
        final int tapTravelPx;
        final int scrollStepPx;
        final int scrollZonePx;

        TouchpadGestureDefaults(boolean strictFsm, int tapTimeoutMs, int tapTravelPx, int scrollStepPx, int scrollZonePx) {
            this.strictFsm = strictFsm;
            this.tapTimeoutMs = tapTimeoutMs;
            this.tapTravelPx = tapTravelPx;
            this.scrollStepPx = scrollStepPx;
            this.scrollZonePx = scrollZonePx;
        }
    }


    public ShortcutSettingsDialog(ShortcutsFragment fragment, Shortcut shortcut) {
        super(fragment.getContext(), R.layout.shortcut_settings_dialog);
        this.fragment = fragment;
        this.shortcut = shortcut;
        setTitle(shortcut.name);
        setIcon(R.drawable.ae_icon_settings);

        ContainerManager containerManager = shortcut.container.getManager();

        createContentView();
    }

    private void createContentView() {
        final Context context = fragment.getContext();
        inputControlsManager = new InputControlsManager(context);
        LinearLayout llContent = findViewById(R.id.LLContent);
        llContent.getLayoutParams().width = AppUtils.getPreferredDialogWidth(context);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);

        applyDynamicStyles(findViewById(R.id.LLContent), isDarkMode);
        applyFieldSetLabelStylesDynamically(llContent, isDarkMode);

        // Initialize the turnip version TextView
        tvGraphicsDriverVersion = findViewById(R.id.TVGraphicsDriverVersion);

        final EditText etName = findViewById(R.id.ETName);
        etName.setText(shortcut.name);

        final EditText etExecArgs = findViewById(R.id.ETExecArgs);
        etExecArgs.setText(shortcut.getExtra("execArgs"));

        ContainerDetailFragment containerDetailFragment = new ContainerDetailFragment(shortcut.container.id);
//        containerDetailFragment.loadScreenSizeSpinner(getContentView(), shortcut.getExtra("screenSize", shortcut.container.getScreenSize()));

        loadScreenSizeSpinner(getContentView(), shortcut.getExtra("screenSize", shortcut.container.getScreenSize()), isDarkMode);


        final Spinner sGraphicsDriver = findViewById(R.id.SGraphicsDriver);

        final Spinner sDXWrapper = findViewById(R.id.SDXWrapper);

        final Spinner sBox64Version = findViewById(R.id.SBox64Version);

        ContentsManager contentsManager = new ContentsManager(context);

        contentsManager.syncContents();

        final View vGraphicsDriverConfig = findViewById(R.id.BTGraphicsDriverConfig);
        String resolvedInitialGraphicsDriverConfig = shortcut.getExtra("graphicsDriverConfig", shortcut.container.getGraphicsDriverConfig());
        if (resolvedInitialGraphicsDriverConfig == null || resolvedInitialGraphicsDriverConfig.trim().isEmpty()) {
            resolvedInitialGraphicsDriverConfig = Container.DEFAULT_GRAPHICSDRIVERCONFIG;
        }
        final String initialGraphicsDriverConfig = resolvedInitialGraphicsDriverConfig;
        vGraphicsDriverConfig.setTag(initialGraphicsDriverConfig);

        final View vDXWrapperConfig = findViewById(R.id.BTDXWrapperConfig);
        vDXWrapperConfig.setTag(shortcut.getExtra("dxwrapperConfig", shortcut.container.getDXWrapperConfig()));

        loadGraphicsDriverSpinner(sGraphicsDriver, sDXWrapper, vGraphicsDriverConfig, Container.normalizeGraphicsDriver(shortcut.getExtra("graphicsDriver", shortcut.container.getGraphicsDriver())),
            shortcut.getExtra("dxwrapper", shortcut.container.getDXWrapper()));

        findViewById(R.id.BTHelpDXWrapper).setOnClickListener((v) -> AppUtils.showHelpBox(context, v, R.string.dxwrapper_help_content));

        final Spinner sAudioDriver = findViewById(R.id.SAudioDriver);
        AppUtils.setSpinnerSelectionFromIdentifier(sAudioDriver, shortcut.getExtra("audioDriver", shortcut.container.getAudioDriver()));
        final Spinner sEmulator = findViewById(R.id.SEmulator);
        AppUtils.setSpinnerSelectionFromIdentifier(sEmulator, shortcut.getExtra("emulator", shortcut.container.getEmulator()));
        final Spinner sEmulator64 = findViewById(R.id.SEmulator64);
        sEmulator64.setEnabled(false);
        final Spinner sMIDISoundFont = findViewById(R.id.SMIDISoundFont);
        MidiManager.loadSFSpinner(sMIDISoundFont);
        AppUtils.setSpinnerSelectionFromValue(sMIDISoundFont, shortcut.getExtra("midiSoundFont", shortcut.container.getMIDISoundFont()));

        final EditText etLC_ALL = findViewById(R.id.ETlcall);
        etLC_ALL.setText(shortcut.getExtra("lc_all", shortcut.container.getLC_ALL()));

        final View btShowLCALL = findViewById(R.id.BTShowLCALL);
        btShowLCALL.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(context, v);
            String[] lcs = context.getResources().getStringArray(R.array.some_lc_all);
            for (int i = 0; i < lcs.length; i++)
                popupMenu.getMenu().add(Menu.NONE, i, Menu.NONE, lcs[i]);
            popupMenu.setOnMenuItemClickListener(item -> {
                etLC_ALL.setText(item.toString() + ".UTF-8");
                return true;
            });
            popupMenu.show();
        });

        FrameLayout fexcoreFL = findViewById(R.id.fexcoreFrame);
        String wineVersion = shortcut.container.getWineVersion();
        WineInfo wineInfo = WineInfo.fromIdentifier(
                context,
                contentsManager,
                wineVersion,
                ContentProfile.inferRuntimeModelFromEntryName(wineVersion)
        );
        if (wineInfo.isArm64EC()) {
            fexcoreFL.setVisibility(View.VISIBLE);
            sEmulator.setEnabled(true);
            sEmulator64.setSelection(0);
            AppUtils.setSpinnerSelectionFromIdentifier(sEmulator, shortcut.getExtra("emulator", "fexcore"));
        }
        else {
            fexcoreFL.setVisibility(View.GONE);
            sEmulator.setEnabled(false);
            AppUtils.setSpinnerSelectionFromIdentifier(sEmulator, "box64");
            sEmulator64.setSelection(1);
        }

        ContainerDetailFragment.setupDXWrapperSpinner(sDXWrapper, vDXWrapperConfig, wineInfo.isArm64EC());
        loadBox64VersionSpinner(context, contentsManager, sBox64Version, wineInfo.isArm64EC());

        // Add this part to set the initial spinner selection based on the shortcut
        String currentBox64Version = shortcut.getExtra("box64Version", shortcut.container.getBox64Version());
        if (currentBox64Version != null) {
            AppUtils.setSpinnerSelectionFromValue(sBox64Version, currentBox64Version);
        } else {
            // Default selection or use a preferred default version
            AppUtils.setSpinnerSelectionFromValue(sBox64Version, wineInfo.isArm64EC() ? DefaultVersion.WOWBOX64 : DefaultVersion.BOX64);
        }

        // Set OnItemSelectedListener for the Box64 version spinner
        sBox64Version.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedVersion = parent.getItemAtPosition(position).toString();
                if (AppUtils.isMissingComponentValue(selectedVersion)) return;
                box64Version = selectedVersion;
                shortcut.putExtra("box64Version", selectedVersion);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // This method must be implemented, even if it's empty.
                // Optional: You can handle the case where no item is selected, if needed.
            }
        });

        final CheckBox cbFullscreenStretched =  findViewById(R.id.CBFullscreenStretched);
        boolean fullscreenStretched = shortcut.getExtraBoolean("fullscreenStretched", false);
        cbFullscreenStretched.setChecked(fullscreenStretched);

        final Runnable showInputWarning = () -> ContentDialog.alert(context, R.string.enable_xinput_and_dinput_same_time, null);
        final CheckBox cbEnableXInput = findViewById(R.id.CBEnableXInput);
        final CheckBox cbEnableDInput = findViewById(R.id.CBEnableDInput);
        final View llDInputType = findViewById(R.id.LLDinputMapperType);
        final View btHelpXInput = findViewById(R.id.BTXInputHelp);
        final View btHelpDInput = findViewById(R.id.BTDInputHelp);
        Spinner SDInputType = findViewById(R.id.SDInputType);
        int inputType = Integer.parseInt(shortcut.getExtra("inputType", String.valueOf(shortcut.container.getInputType())));


        cbEnableXInput.setChecked((inputType & WinHandler.FLAG_INPUT_TYPE_XINPUT) == WinHandler.FLAG_INPUT_TYPE_XINPUT);
        cbEnableDInput.setChecked((inputType & WinHandler.FLAG_INPUT_TYPE_DINPUT) == WinHandler.FLAG_INPUT_TYPE_DINPUT);
        cbEnableDInput.setOnCheckedChangeListener((buttonView, isChecked) -> {
            llDInputType.setVisibility(isChecked?View.VISIBLE:View.GONE);
            if (isChecked && cbEnableXInput.isChecked())
                showInputWarning.run();
        });
        cbEnableXInput.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && cbEnableDInput.isChecked())
                showInputWarning.run();
        });
        btHelpXInput.setOnClickListener(v -> AppUtils.showHelpBox(context, v, R.string.help_xinput));
        btHelpDInput.setOnClickListener(v -> AppUtils.showHelpBox(context, v, R.string.help_dinput));
        SDInputType.setSelection(((inputType & WinHandler.FLAG_DINPUT_MAPPER_STANDARD) == WinHandler.FLAG_DINPUT_MAPPER_STANDARD) ? 0 : 1);
        llDInputType.setVisibility(cbEnableDInput.isChecked()?View.VISIBLE:View.GONE);

        final Spinner sBox64Preset = findViewById(R.id.SBox64Preset);
        Box64PresetManager.loadSpinner("box64", sBox64Preset, shortcut.getExtra("box64Preset", shortcut.container.getBox64Preset()));

        final Spinner sFEXCoreVersion = findViewById(R.id.SFEXCoreVersion);
        FEXCoreManager.loadFEXCoreVersion(context, contentsManager, sFEXCoreVersion, shortcut.getExtra("fexcoreVersion", shortcut.container.getFEXCoreVersion()));

        final Spinner sFEXCorePreset = findViewById(R.id.SFEXCorePreset);
        FEXCorePresetManager.loadSpinner(sFEXCorePreset, shortcut.getExtra("fexcorePreset", shortcut.container.getFEXCorePreset()));

        final Spinner sControlsProfile = findViewById(R.id.SControlsProfile);
        loadControlsProfileSpinner(sControlsProfile, shortcut.getExtra("controlsProfile", "0"));

        final CheckBox cbDisabledXInput = findViewById(R.id.CBDisabledXInput);
        cbDisabledXInput.setChecked(shortcut.getExtraBoolean("disableXinput", false));

        final CheckBox cbSimTouchScreen = findViewById(R.id.CBTouchscreenMode);
        cbSimTouchScreen.setChecked(shortcut.getExtraBoolean("simTouchScreen", false));
        final Spinner sTouchpadGestureProfile = findViewById(R.id.STouchpadGestureProfile);
        final CheckBox cbTouchpadStrictGestureFsm = findViewById(R.id.CBTouchpadStrictGestureFsm);
        final SeekBar sbTapTimeoutMs = findViewById(R.id.SBTapTimeoutMs);
        final SeekBar sbTapTravelPx = findViewById(R.id.SBTapTravelPx);
        final SeekBar sbScrollStepPx = findViewById(R.id.SBScrollStepPx);
        final SeekBar sbScrollZonePx = findViewById(R.id.SBScrollZonePx);
        final TextView tvTapTimeoutMs = findViewById(R.id.TVTapTimeoutMs);
        final TextView tvTapTravelPx = findViewById(R.id.TVTapTravelPx);
        final TextView tvScrollStepPx = findViewById(R.id.TVScrollStepPx);
        final TextView tvScrollZonePx = findViewById(R.id.TVScrollZonePx);
        final String[] touchpadProfileValues = context.getResources().getStringArray(R.array.touchpad_gesture_profile_values);
        String shortcutTouchpadProfile = shortcut.getExtra("touchpadGestureProfile", TOUCHPAD_PROFILE_GLOBAL);
        int touchpadProfileIndex = findStringValueIndex(touchpadProfileValues, shortcutTouchpadProfile);
        if (touchpadProfileIndex < 0) touchpadProfileIndex = 0;
        sTouchpadGestureProfile.setSelection(touchpadProfileIndex, false);

        final Runnable updateGestureMetricLabels = () -> {
            tvTapTimeoutMs.setText(sbTapTimeoutMs.getProgress() + " ms");
            tvTapTravelPx.setText(sbTapTravelPx.getProgress() + " px");
            tvScrollStepPx.setText(sbScrollStepPx.getProgress() + " px");
            tvScrollZonePx.setText(sbScrollZonePx.getProgress() + " px");
        };

        final Runnable bindTouchpadProfileControls = () -> {
            int selectedIndex = Math.max(0, sTouchpadGestureProfile.getSelectedItemPosition());
            if (selectedIndex >= touchpadProfileValues.length) selectedIndex = 0;
            String selectedProfileId = touchpadProfileValues[selectedIndex];
            TouchpadGestureDefaults defaults = resolveTouchpadGestureDefaults(selectedProfileId);
            boolean isGlobalProfile = TOUCHPAD_PROFILE_GLOBAL.equals(selectedProfileId);

            cbTouchpadStrictGestureFsm.setEnabled(!isGlobalProfile);
            sbTapTimeoutMs.setEnabled(!isGlobalProfile);
            sbTapTravelPx.setEnabled(!isGlobalProfile);
            sbScrollStepPx.setEnabled(!isGlobalProfile);
            sbScrollZonePx.setEnabled(!isGlobalProfile);

            cbTouchpadStrictGestureFsm.setChecked(defaults.strictFsm);
            sbTapTimeoutMs.setProgress(defaults.tapTimeoutMs);
            sbTapTravelPx.setProgress(defaults.tapTravelPx);
            sbScrollStepPx.setProgress(defaults.scrollStepPx);
            sbScrollZonePx.setProgress(defaults.scrollZonePx);
            updateGestureMetricLabels.run();
        };

        sTouchpadGestureProfile.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                bindTouchpadProfileControls.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        sbTapTimeoutMs.setOnSeekBarChangeListener(new SimpleSeekbarListener(updateGestureMetricLabels));
        sbTapTravelPx.setOnSeekBarChangeListener(new SimpleSeekbarListener(updateGestureMetricLabels));
        sbScrollStepPx.setOnSeekBarChangeListener(new SimpleSeekbarListener(updateGestureMetricLabels));
        sbScrollZonePx.setOnSeekBarChangeListener(new SimpleSeekbarListener(updateGestureMetricLabels));
        bindTouchpadProfileControls.run();

        String shortcutStrictGesture = shortcut.getExtra("touchpadStrictGestureFsm", "");
        String shortcutTapTimeoutMs = shortcut.getExtra("touchpadTapTimeoutMs", "");
        String shortcutTapTravelPx = shortcut.getExtra("touchpadTapTravelPx", "");
        String shortcutScrollStepPx = shortcut.getExtra("touchpadScrollStepPx", "");
        String shortcutScrollZonePx = shortcut.getExtra("touchpadScrollZonePx", "");
        if (!shortcutStrictGesture.isEmpty()) cbTouchpadStrictGestureFsm.setChecked(parseBooleanValue(shortcutStrictGesture));
        if (!shortcutTapTimeoutMs.isEmpty()) sbTapTimeoutMs.setProgress(parseBoundedInt(shortcutTapTimeoutMs, sbTapTimeoutMs.getProgress(), 80, 500));
        if (!shortcutTapTravelPx.isEmpty()) sbTapTravelPx.setProgress(parseBoundedInt(shortcutTapTravelPx, sbTapTravelPx.getProgress(), 4, 24));
        if (!shortcutScrollStepPx.isEmpty()) sbScrollStepPx.setProgress(parseBoundedInt(shortcutScrollStepPx, sbScrollStepPx.getProgress(), 40, 240));
        if (!shortcutScrollZonePx.isEmpty()) sbScrollZonePx.setProgress(parseBoundedInt(shortcutScrollZonePx, sbScrollZonePx.getProgress(), 120, 700));
        updateGestureMetricLabels.run();

        ContainerDetailFragment.createWinComponentsTabFromShortcut(this, getContentView(),
                shortcut.getExtra("wincomponents", shortcut.container.getWinComponents()), isDarkMode);

        final EnvVarsView envVarsView = createEnvVarsTab();

        AppUtils.setupTabLayout(getContentView(), R.id.TabLayout, R.id.LLTabWinComponents, R.id.LLTabEnvVars, R.id.LLTabAdvanced);

        TabLayout tabLayout = findViewById(R.id.TabLayout);

        if (isDarkMode) {
            tabLayout.setBackgroundResource(R.drawable.tab_layout_background_dark);
        } else {
            tabLayout.setBackgroundResource(R.drawable.tab_layout_background);
        }

        findViewById(R.id.BTExtraArgsMenu).setOnClickListener((v) -> {
            PopupMenu popupMenu = new PopupMenu(context, v);
            popupMenu.inflate(R.menu.extra_args_popup_menu);
            popupMenu.setOnMenuItemClickListener((menuItem) -> {
                String value = String.valueOf(menuItem.getTitle());
                String execArgs = etExecArgs.getText().toString();
                if (!execArgs.contains(value)) etExecArgs.setText(!execArgs.isEmpty() ? execArgs + " " + value : value);
                return true;
            });
            popupMenu.show();
        });

        String selectedDriver = sGraphicsDriver.getSelectedItem().toString();
        sGraphicsDriver.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
        AppUtils.setSpinnerSelectionFromValue(sGraphicsDriver, selectedDriver);

        final Spinner sStartupSelection = findViewById(R.id.SStartupSelection);
        sStartupSelection.setSelection(Integer.parseInt(shortcut.getExtra("startupSelection", String.valueOf(shortcut.container.getStartupSelection()))));

        final Spinner sUpscalerPreset = findViewById(R.id.SUpscalerPreset);
        final Spinner sUpscalerBackend = findViewById(R.id.SUpscalerBackend);
        final Spinner sUpscalerEffect = findViewById(R.id.SUpscalerEffect);
        final Spinner sUpscalerScale = findViewById(R.id.SUpscalerScale);
        final CheckBox cbEnableFrameGeneration = findViewById(R.id.CBEnableFrameGeneration);
        final Spinner sGeneratedFrames = findViewById(R.id.SGeneratedFrames);
        final Spinner sUpscalerFgSource = findViewById(R.id.SUpscalerFgSource);
        final Spinner sUpscalerFgOutput = findViewById(R.id.SUpscalerFgOutput);
        final Spinner sUpscalerFramegenMode = findViewById(R.id.SUpscalerFramegenMode);
        final CheckBox cbUpscalerThermalGuard = findViewById(R.id.CBUpscalerThermalGuard);
        final SeekBar sbUpscalerTargetFps = findViewById(R.id.SBUpscalerTargetFps);
        final TextView tvUpscalerTargetFps = findViewById(R.id.TVUpscalerTargetFps);
        final SeekBar sbInterpolationFactor = findViewById(R.id.SBInterpolationFactor);
        final TextView tvInterpolationFactor = findViewById(R.id.TVInterpolationFactor);
        final CheckBox cbUpscalerDebugOverlay = findViewById(R.id.CBUpscalerDebugOverlay);
        final CheckBox cbUpscalerDebugTearLines = findViewById(R.id.CBUpscalerDebugTearLines);
        final CheckBox cbUpscalerInterpolatedOnly = findViewById(R.id.CBUpscalerInterpolatedOnly);
        final CheckBox cbEnableVulkanValidationLayer = findViewById(R.id.CBEnableVulkanValidationLayer);
        final SeekBar sbSharpnessLevel = findViewById(R.id.SBSharpnessLevel);
        final SeekBar sbSharpnessDenoise = findViewById(R.id.SBSharpnessDenoise);
        final TextView tvSharpnessLevel = findViewById(R.id.TVSharpnessLevel);
        final TextView tvSharpnessDenoise = findViewById(R.id.TVSharpnessDenoise);

        UpscalerProfileStore.Profile globalUpscalerProfile = UpscalerProfileStore.getSelectedProfile(prefs);
        String initialUpscalerBackend = UpscalerProfileStore.normalizeBackend(
                shortcut.getExtra("upscalerBackend", globalUpscalerProfile.backend)
        );
        String initialUpscalerEffect = normalizeUpscalerEffect(
                shortcut.getExtra("upscalerEffect", globalUpscalerProfile.effect)
        );
        String initialUpscalerPreset = normalizeUpscalerPreset(
                shortcut.getExtra("upscalerPreset", globalUpscalerProfile.preset)
        );
        int initialUpscalerScale = parseBoundedInt(
                shortcut.getExtra("upscalerScale", String.valueOf(globalUpscalerProfile.scalePercent)),
                100,
                100,
                200
        );
        int initialGeneratedFrames = parseBoundedInt(
                shortcut.getExtra("upscalerGeneratedFrames", String.valueOf(globalUpscalerProfile.generatedFrames)),
                1,
                1,
                3
        );
        boolean initialFrameGenerationEnabled = parseBooleanValue(
                shortcut.getExtra(
                        "upscalerFrameGeneration",
                        globalUpscalerProfile.frameGeneration ? "1" : "0"
                )
        );
        if (!UPSCALER_BACKEND_LSFG.equals(initialUpscalerBackend)) {
            initialFrameGenerationEnabled = false;
        }
        String initialFgSource = normalizeFgSource(
                shortcut.getExtra("upscalerFgSource", globalUpscalerProfile.fgSource)
        );
        String initialFgOutput = normalizeFgOutput(
                shortcut.getExtra("upscalerFgOutput", globalUpscalerProfile.fgOutput)
        );
        String initialFramegenMode = normalizeFramegenMode(
                shortcut.getExtra("upscalerFramegenMode", globalUpscalerProfile.framegenMode)
        );
        boolean initialUpscalerThermalGuard = parseBooleanValue(
                shortcut.getExtra(
                        "upscalerThermalGuard",
                        globalUpscalerProfile.thermalGuard ? "1" : "0"
                )
        );
        int initialUpscalerTargetFps = parseBoundedInt(
                shortcut.getExtra("upscalerTargetFps", String.valueOf(globalUpscalerProfile.targetFps)),
                60,
                30,
                144
        );
        int initialInterpolationFactor = parseBoundedIntAllowZero(
                shortcut.getExtra(
                        "upscalerInterpolationFactor",
                        String.valueOf(globalUpscalerProfile.interpolationFactor)
                ),
                50,
                0,
                100
        );
        boolean initialUpscalerDebugOverlay = parseBooleanValue(
                shortcut.getExtra(
                        "upscalerDebugOverlay",
                        globalUpscalerProfile.debugOverlay ? "1" : "0"
                )
        );
        boolean initialUpscalerDebugTearLines = parseBooleanValue(
                shortcut.getExtra(
                        "upscalerDebugTearLines",
                        globalUpscalerProfile.debugTearLines ? "1" : "0"
                )
        );
        boolean initialUpscalerInterpolatedOnly = parseBooleanValue(
                shortcut.getExtra(
                        "upscalerInterpolatedOnly",
                        globalUpscalerProfile.interpolatedOnly ? "1" : "0"
                )
        );
        boolean initialVulkanValidationLayer = parseBooleanValue(
                shortcut.getExtra(
                        "vulkanValidationLayer",
                        globalUpscalerProfile.vulkanValidationLayer ? "1" : "0"
                )
        );
        int initialSharpnessLevel = parseBoundedIntAllowZero(
                shortcut.getExtra("upscalerSharpness", String.valueOf(globalUpscalerProfile.sharpness)),
                100,
                0,
                100
        );
        int initialSharpnessDenoise = parseBoundedIntAllowZero(
                shortcut.getExtra("upscalerDenoise", String.valueOf(globalUpscalerProfile.denoise)),
                100,
                0,
                100
        );

        AppUtils.setSpinnerSelectionFromIdentifier(sUpscalerPreset, initialUpscalerPreset);
        AppUtils.setSpinnerSelectionFromIdentifier(sUpscalerBackend, initialUpscalerBackend);
        AppUtils.setSpinnerSelectionFromIdentifier(sUpscalerEffect, initialUpscalerEffect);
        AppUtils.setSpinnerSelectionFromValue(sUpscalerScale, String.valueOf(initialUpscalerScale));
        AppUtils.setSpinnerSelectionFromValue(sGeneratedFrames, String.valueOf(initialGeneratedFrames));
        AppUtils.setSpinnerSelectionFromIdentifier(sUpscalerFgSource, initialFgSource);
        AppUtils.setSpinnerSelectionFromIdentifier(sUpscalerFgOutput, initialFgOutput);
        AppUtils.setSpinnerSelectionFromIdentifier(sUpscalerFramegenMode, initialFramegenMode);
        cbEnableFrameGeneration.setChecked(initialFrameGenerationEnabled);
        cbUpscalerThermalGuard.setChecked(initialUpscalerThermalGuard);
        sbUpscalerTargetFps.setProgress(initialUpscalerTargetFps);
        tvUpscalerTargetFps.setText(String.valueOf(initialUpscalerTargetFps));
        sbInterpolationFactor.setProgress(initialInterpolationFactor);
        tvInterpolationFactor.setText(formatInterpolationFactor(initialInterpolationFactor));
        cbUpscalerDebugOverlay.setChecked(initialUpscalerDebugOverlay);
        cbUpscalerDebugTearLines.setChecked(initialUpscalerDebugTearLines);
        cbUpscalerInterpolatedOnly.setChecked(initialUpscalerInterpolatedOnly);
        cbEnableVulkanValidationLayer.setChecked(initialVulkanValidationLayer);

        sbUpscalerTargetFps.setOnSeekBarChangeListener(new SimpleSeekbarListener(
                () -> tvUpscalerTargetFps.setText(String.valueOf(sbUpscalerTargetFps.getProgress()))
        ));
        sbInterpolationFactor.setOnSeekBarChangeListener(new SimpleSeekbarListener(
                () -> tvInterpolationFactor.setText(formatInterpolationFactor(sbInterpolationFactor.getProgress()))
        ));

        sbSharpnessLevel.setProgress(initialSharpnessLevel);
        tvSharpnessLevel.setText(initialSharpnessLevel + "%");
        sbSharpnessLevel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvSharpnessLevel.setText(progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        sbSharpnessDenoise.setProgress(initialSharpnessDenoise);
        tvSharpnessDenoise.setText(initialSharpnessDenoise + "%");
        sbSharpnessDenoise.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvSharpnessDenoise.setText(progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final Runnable updateUpscalerUiState = () -> {
            String backendId = StringUtils.parseIdentifier(sUpscalerBackend.getSelectedItem());
            boolean upscalerEnabled = !UPSCALER_BACKEND_OFF.equals(backendId);
            boolean frameGenerationSupported = UPSCALER_BACKEND_LSFG.equals(backendId);
            boolean lsfgDebugSupported = UPSCALER_BACKEND_LSFG.equals(backendId);
            sUpscalerPreset.setEnabled(upscalerEnabled);
            sUpscalerEffect.setEnabled(upscalerEnabled);
            sUpscalerScale.setEnabled(upscalerEnabled);
            cbEnableFrameGeneration.setEnabled(upscalerEnabled && frameGenerationSupported);
            if ((!upscalerEnabled || !frameGenerationSupported) && cbEnableFrameGeneration.isChecked()) {
                cbEnableFrameGeneration.setChecked(false);
            }
            sGeneratedFrames.setEnabled(upscalerEnabled && frameGenerationSupported && cbEnableFrameGeneration.isChecked());
            sUpscalerFgSource.setEnabled(upscalerEnabled && cbEnableFrameGeneration.isChecked());
            sUpscalerFgOutput.setEnabled(upscalerEnabled && cbEnableFrameGeneration.isChecked());
            sUpscalerFramegenMode.setEnabled(upscalerEnabled && cbEnableFrameGeneration.isChecked());
            cbUpscalerThermalGuard.setEnabled(upscalerEnabled && cbEnableFrameGeneration.isChecked());
            sbUpscalerTargetFps.setEnabled(upscalerEnabled && cbEnableFrameGeneration.isChecked());
            sbInterpolationFactor.setEnabled(upscalerEnabled && cbEnableFrameGeneration.isChecked());
            cbUpscalerDebugOverlay.setEnabled(upscalerEnabled && lsfgDebugSupported && cbEnableFrameGeneration.isChecked());
            cbUpscalerDebugTearLines.setEnabled(upscalerEnabled && lsfgDebugSupported && cbEnableFrameGeneration.isChecked());
            cbUpscalerInterpolatedOnly.setEnabled(upscalerEnabled && lsfgDebugSupported && cbEnableFrameGeneration.isChecked());
            cbEnableVulkanValidationLayer.setEnabled(upscalerEnabled);
            sbSharpnessLevel.setEnabled(upscalerEnabled);
            sbSharpnessDenoise.setEnabled(upscalerEnabled);
        };

        sUpscalerBackend.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateUpscalerUiState.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateUpscalerUiState.run();
            }
        });
        cbEnableFrameGeneration.setOnCheckedChangeListener((buttonView, isChecked) -> updateUpscalerUiState.run());
        updateUpscalerUiState.run();

        final CPUListView cpuListView = findViewById(R.id.CPUListView);
        cpuListView.setCheckedCPUList(shortcut.getExtra("cpuList", shortcut.container.getCPUList(true)));

        setOnConfirmCallback(() -> {
            String name = etName.getText().toString().trim();
            boolean nameChanged = !shortcut.name.equals(name) && !name.isEmpty();

            // First, handle renaming if the name has changed
            if (nameChanged) {
                renameShortcut(name);
            }


            // Determine if renaming is needed
            boolean renamingSuccess = !nameChanged || new File(shortcut.file.getParent(), name + ".desktop").exists();

            if (renamingSuccess) {
                String selectedGraphicsDriver = StringUtils.parseIdentifier(sGraphicsDriver.getSelectedItem());
                String graphicsDriver = Container.normalizeGraphicsDriver(selectedGraphicsDriver);
                Object graphicsDriverConfigTag = vGraphicsDriverConfig.getTag();
                String graphicsDriverConfig = graphicsDriverConfigTag instanceof String
                        ? (String) graphicsDriverConfigTag
                        : GraphicsDrivers.defaultConfig(selectedGraphicsDriver);
                graphicsDriverConfig = GraphicsDrivers.sanitizeConfigShape(selectedGraphicsDriver, graphicsDriverConfig);
                if (!GraphicsDrivers.usesKeyValueConfig(selectedGraphicsDriver)) {
                    graphicsDriverConfig = Container.reconcileLegacyGraphicsConfig(selectedGraphicsDriver, graphicsDriverConfig);
                    HashMap<String, String> config = GraphicsDrivers.parseConfig(selectedGraphicsDriver, graphicsDriverConfig);
                    String graphicsDriverVersion = config.get("version");
                    if (graphicsDriverVersion == null || graphicsDriverVersion.trim().isEmpty()) {
                        config.put("version", new AdrenotoolsManager(context).getPreferredWrapperDriverId());
                        graphicsDriverConfig = GraphicsDriverConfigDialog.toGraphicsDriverConfig(config);
                    }
                }
                String dxwrapper = StringUtils.parseIdentifier(sDXWrapper.getSelectedItem());
                String dxwrapperConfig = vDXWrapperConfig.getTag().toString();
                String audioDriver = StringUtils.parseIdentifier(sAudioDriver.getSelectedItem());
                String emulator = StringUtils.parseIdentifier(sEmulator.getSelectedItem());
                String lc_all = etLC_ALL.getText().toString();
                String midiSoundFont = sMIDISoundFont.getSelectedItemPosition() == 0 ? "" : sMIDISoundFont.getSelectedItem().toString();
                String screenSize = containerDetailFragment.getScreenSize(getContentView());

                int finalInputType = 0;
                finalInputType |= cbEnableXInput.isChecked() ? WinHandler.FLAG_INPUT_TYPE_XINPUT : 0;
                finalInputType |= cbEnableDInput.isChecked() ? WinHandler.FLAG_INPUT_TYPE_DINPUT : 0;
                finalInputType |= SDInputType.getSelectedItemPosition() == 0 ?  WinHandler.FLAG_DINPUT_MAPPER_STANDARD : WinHandler.FLAG_DINPUT_MAPPER_XINPUT;


                shortcut.putExtra("inputType", String.valueOf(finalInputType));

                boolean disabledXInput = cbDisabledXInput.isChecked();
                shortcut.putExtra("disableXinput", disabledXInput ? "1" : null);

                boolean touchscreenMode = cbSimTouchScreen.isChecked();
                shortcut.putExtra("simTouchScreen", touchscreenMode ? "1" : "0");
                int selectedGestureProfileIndex = Math.max(0, sTouchpadGestureProfile.getSelectedItemPosition());
                if (selectedGestureProfileIndex >= touchpadProfileValues.length) selectedGestureProfileIndex = 0;
                String selectedGestureProfile = touchpadProfileValues[selectedGestureProfileIndex];
                shortcut.putExtra("touchpadGestureProfile", selectedGestureProfile);
                if (TOUCHPAD_PROFILE_GLOBAL.equals(selectedGestureProfile)) {
                    shortcut.putExtra("touchpadStrictGestureFsm", null);
                    shortcut.putExtra("touchpadTapTimeoutMs", null);
                    shortcut.putExtra("touchpadTapTravelPx", null);
                    shortcut.putExtra("touchpadScrollStepPx", null);
                    shortcut.putExtra("touchpadScrollZonePx", null);
                } else {
                    shortcut.putExtra("touchpadStrictGestureFsm", cbTouchpadStrictGestureFsm.isChecked() ? "1" : "0");
                    shortcut.putExtra("touchpadTapTimeoutMs", String.valueOf(sbTapTimeoutMs.getProgress()));
                    shortcut.putExtra("touchpadTapTravelPx", String.valueOf(sbTapTravelPx.getProgress()));
                    shortcut.putExtra("touchpadScrollStepPx", String.valueOf(sbScrollStepPx.getProgress()));
                    shortcut.putExtra("touchpadScrollZonePx", String.valueOf(sbScrollZonePx.getProgress()));
                }

                String execArgs = etExecArgs.getText().toString();
                shortcut.putExtra("execArgs", !execArgs.isEmpty() ? execArgs : null);
                shortcut.putExtra("screenSize", screenSize);
                shortcut.putExtra("graphicsDriver", graphicsDriver);
                shortcut.putExtra("graphicsDriverConfig", graphicsDriverConfig);
                shortcut.putExtra("dxwrapper", dxwrapper);
                shortcut.putExtra("dxwrapperConfig", dxwrapperConfig);
                shortcut.putExtra("audioDriver", audioDriver);
                shortcut.putExtra("emulator", emulator);
                shortcut.putExtra("midiSoundFont", midiSoundFont);
                shortcut.putExtra("lc_all", lc_all);

                shortcut.putExtra("fullscreenStretched", cbFullscreenStretched.isChecked() ? "1" : null);

                String wincomponents = containerDetailFragment.getWinComponents(getContentView());
                shortcut.putExtra("wincomponents", wincomponents);

                String envVars = envVarsView.getEnvVars();
                shortcut.putExtra("envVars", !envVars.isEmpty() ? envVars : null);

                String fexcoreVersion = sFEXCoreVersion.getSelectedItem() != null ? sFEXCoreVersion.getSelectedItem().toString() : "";
                if (!AppUtils.isMissingComponentValue(fexcoreVersion)) {
                    shortcut.putExtra("fexcoreVersion", fexcoreVersion);
                }

                String fexcorePreset = FEXCorePresetManager.getSpinnerSelectedId(sFEXCorePreset);
                shortcut.putExtra("fexcorePreset", fexcorePreset);

                String box64Preset = Box64PresetManager.getSpinnerSelectedId(sBox64Preset);
                shortcut.putExtra("box64Preset", box64Preset);

                byte startupSelection = (byte)sStartupSelection.getSelectedItemPosition();
                shortcut.putExtra("startupSelection", String.valueOf(startupSelection));

                String upscalerPreset = normalizeUpscalerPreset(
                        StringUtils.parseIdentifier(sUpscalerPreset.getSelectedItem())
                );
                String upscalerBackend = StringUtils.parseIdentifier(sUpscalerBackend.getSelectedItem());
                String upscalerEffect = StringUtils.parseIdentifier(sUpscalerEffect.getSelectedItem());
                String upscalerScale = String.valueOf(parseBoundedIntAllowZero(
                        String.valueOf(sUpscalerScale.getSelectedItem()),
                        100,
                        100,
                        200
                ));
                String generatedFrames = String.valueOf(parseBoundedIntAllowZero(
                        String.valueOf(sGeneratedFrames.getSelectedItem()),
                        1,
                        1,
                        3
                ));
                String upscalerFgSource = normalizeFgSource(
                        StringUtils.parseIdentifier(sUpscalerFgSource.getSelectedItem())
                );
                String upscalerFgOutput = normalizeFgOutput(
                        StringUtils.parseIdentifier(sUpscalerFgOutput.getSelectedItem())
                );
                String upscalerFramegenMode = normalizeFramegenMode(
                        StringUtils.parseIdentifier(sUpscalerFramegenMode.getSelectedItem())
                );
                String upscalerThermalGuard = cbUpscalerThermalGuard.isChecked() ? "1" : "0";
                String upscalerTargetFps = String.valueOf(parseBoundedIntAllowZero(
                        String.valueOf(sbUpscalerTargetFps.getProgress()),
                        60,
                        30,
                        144
                ));
                String upscalerInterpolationFactor = String.valueOf(parseBoundedIntAllowZero(
                        String.valueOf(sbInterpolationFactor.getProgress()),
                        50,
                        0,
                        100
                ));
                String upscalerDebugOverlay = cbUpscalerDebugOverlay.isChecked() ? "1" : "0";
                String upscalerDebugTearLines = cbUpscalerDebugTearLines.isChecked() ? "1" : "0";
                String upscalerInterpolatedOnly = cbUpscalerInterpolatedOnly.isChecked() ? "1" : "0";
                String vulkanValidationLayer = cbEnableVulkanValidationLayer.isChecked() ? "1" : "0";
                String sharpnessLevel = String.valueOf(sbSharpnessLevel.getProgress());
                String sharpnessDenoise = String.valueOf(sbSharpnessDenoise.getProgress());
                String frameGenerationEnabled = cbEnableFrameGeneration.isChecked() ? "1" : "0";

                shortcut.putExtra("upscalerPreset", upscalerPreset);
                shortcut.putExtra("upscalerBackend", upscalerBackend);
                shortcut.putExtra("upscalerEffect", upscalerEffect);
                shortcut.putExtra("upscalerScale", upscalerScale);
                shortcut.putExtra("upscalerFrameGeneration", frameGenerationEnabled);
                shortcut.putExtra("upscalerGeneratedFrames", generatedFrames);
                shortcut.putExtra("upscalerFgSource", upscalerFgSource);
                shortcut.putExtra("upscalerFgOutput", upscalerFgOutput);
                shortcut.putExtra("upscalerFramegenMode", upscalerFramegenMode);
                shortcut.putExtra("upscalerThermalGuard", upscalerThermalGuard);
                shortcut.putExtra("upscalerTargetFps", upscalerTargetFps);
                shortcut.putExtra("upscalerInterpolationFactor", upscalerInterpolationFactor);
                shortcut.putExtra("upscalerDebugOverlay", upscalerDebugOverlay);
                shortcut.putExtra("upscalerDebugTearLines", upscalerDebugTearLines);
                shortcut.putExtra("upscalerInterpolatedOnly", upscalerInterpolatedOnly);
                shortcut.putExtra("vulkanValidationLayer", vulkanValidationLayer);
                shortcut.putExtra("upscalerSharpness", sharpnessLevel);
                shortcut.putExtra("upscalerDenoise", sharpnessDenoise);

                ArrayList<ControlsProfile> profiles = inputControlsManager.getProfiles(true);
                int controlsProfile = sControlsProfile.getSelectedItemPosition() > 0 ? profiles.get(sControlsProfile.getSelectedItemPosition() - 1).id : 0;
                shortcut.putExtra("controlsProfile", controlsProfile > 0 ? String.valueOf(controlsProfile) : null);

                String cpuList = cpuListView.getCheckedCPUListAsString();
                shortcut.putExtra("cpuList", cpuList);

                // Save all changes to the shortcut
                shortcut.saveData();
                boolean requireRestart = GraphicsDrivers.isMediaTekWrapperFamily(graphicsDriver)
                        && VortekConfigDialog.isRequireRestart(initialGraphicsDriverConfig, graphicsDriverConfig);
                if (requireRestart) {
                    ContentDialog.confirm(
                            context,
                            R.string.the_settings_have_been_changed_do_you_want_to_restart_the_app,
                            () -> AppUtils.restartApplication(context)
                    );
                }
                ForensicLogger.logEvent(
                        context,
                        "info",
                        "SHORTCUT_SETTINGS_SAVED",
                        shortcut.path,
                        "shortcut_settings",
                        "shortcut_settings_saved",
                        ForensicLogger.fields(
                                "shortcut_name", shortcut.name,
                                "graphics_driver", graphicsDriver,
                                "dxwrapper", dxwrapper,
                                "audio_driver", audioDriver,
                                "emulator", emulator,
                                "upscaler_backend", upscalerBackend,
                                "upscaler_preset", upscalerPreset,
                                "upscaler_effect", upscalerEffect,
                                "upscaler_frame_generation", frameGenerationEnabled,
                                "upscaler_fg_source", upscalerFgSource,
                                "upscaler_fg_output", upscalerFgOutput,
                                "vulkan_validation_layer", vulkanValidationLayer,
                                "box64_preset", box64Preset,
                                "fexcore_preset", fexcorePreset,
                                "startup_selection", startupSelection,
                                "touchpad_profile", selectedGestureProfile
                        )
                );
            }
        });
    }

    // Utility method to apply styles to dynamically added TextViews based on their content
    private void applyFieldSetLabelStylesDynamically(ViewGroup rootView, boolean isDarkMode) {
        for (int i = 0; i < rootView.getChildCount(); i++) {
            View child = rootView.getChildAt(i);
            if (child instanceof ViewGroup) {
                applyFieldSetLabelStylesDynamically((ViewGroup) child, isDarkMode); // Recursive call for nested ViewGroups
            } else if (child instanceof TextView) {
                TextView textView = (TextView) child;
                // Apply the style based on the content of the TextView
                if (isFieldSetLabel(textView.getText().toString())) {
                    applyModernSectionCardStyle(textView, isDarkMode);
                }
            }
        }
    }

    // Method to check if the text content matches any fieldset label
    private boolean isFieldSetLabel(String text) {
        return text.equalsIgnoreCase("DirectX") ||
                text.equalsIgnoreCase("General") ||
                text.equalsIgnoreCase("Box64") ||
                text.equalsIgnoreCase("FEXCore Config") ||
                text.equalsIgnoreCase("Input Controls") ||
                text.equalsIgnoreCase("Touchpad Help") ||
                text.equalsIgnoreCase("Game Controller") ||
                text.equalsIgnoreCase("System") ||
                text.equalsIgnoreCase("vkBasalt") ||
                text.equalsIgnoreCase("AE Upscaler / Frame Generation");
    }

    public void onWinComponentsViewsAdded(boolean isDarkMode) {
        // Apply styles to all dynamically added TextViews
        ViewGroup llContent = findViewById(R.id.LLContent);
        applyFieldSetLabelStylesDynamically(llContent, isDarkMode);
    }

    private static final class SimpleSeekbarListener implements SeekBar.OnSeekBarChangeListener {
        private final Runnable onValueChanged;

        SimpleSeekbarListener(Runnable onValueChanged) {
            this.onValueChanged = onValueChanged;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (onValueChanged != null) onValueChanged.run();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
    }

    private TouchpadGestureDefaults resolveTouchpadGestureDefaults(String profileId) {
        String normalized = profileId == null ? TOUCHPAD_PROFILE_GLOBAL : profileId.trim().toLowerCase(Locale.ENGLISH);
        return switch (normalized) {
            case TOUCHPAD_PROFILE_BALANCED -> new TouchpadGestureDefaults(true, 190, 10, 95, 350);
            case TOUCHPAD_PROFILE_AGGRESSIVE -> new TouchpadGestureDefaults(true, 145, 8, 75, 300);
            case TOUCHPAD_PROFILE_COMPAT -> new TouchpadGestureDefaults(false, 240, 14, 130, 430);
            case TOUCHPAD_PROFILE_GLOBAL -> new TouchpadGestureDefaults(false, 200, 10, 100, 350);
            default -> new TouchpadGestureDefaults(true, 190, 10, 95, 350);
        };
    }

    private int findStringValueIndex(String[] values, String target) {
        if (values == null || values.length == 0) return -1;
        String normalizedTarget = target == null ? "" : target.trim().toLowerCase(Locale.ENGLISH);
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null && values[i].trim().toLowerCase(Locale.ENGLISH).equals(normalizedTarget)) {
                return i;
            }
        }
        return -1;
    }

    private int parseBoundedInt(String value, int fallback, int min, int max) {
        if (value == null || value.trim().isEmpty()) return fallback;
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed <= 0) return fallback;
            return Math.max(min, Math.min(max, parsed));
        } catch (Exception ignored) {
            return fallback;
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

    private String formatInterpolationFactor(int value) {
        int clamped = Math.max(0, Math.min(100, value));
        return clamped + "%";
    }

    private boolean parseBooleanValue(String value) {
        return "1".equals(value)
                || "true".equalsIgnoreCase(value)
                || "yes".equalsIgnoreCase(value);
    }

    private String normalizeUpscalerEffect(String effect) {
        String normalized = effect == null ? UPSCALER_EFFECT_NONE : effect.trim().toLowerCase(Locale.ENGLISH);
        return switch (normalized) {
            case "cas", "dls", "fsr", "nis" -> normalized;
            default -> UPSCALER_EFFECT_NONE;
        };
    }

    private String normalizeUpscalerPreset(String preset) {
        String normalized = preset == null ? UPSCALER_PRESET_AUTO : preset.trim().toLowerCase(Locale.ENGLISH);
        return switch (normalized) {
            case UPSCALER_PRESET_CONSERVATIVE -> UPSCALER_PRESET_CONSERVATIVE;
            case UPSCALER_PRESET_BALANCED -> UPSCALER_PRESET_BALANCED;
            case UPSCALER_PRESET_AGGRESSIVE -> UPSCALER_PRESET_AGGRESSIVE;
            default -> UPSCALER_PRESET_AUTO;
        };
    }

    private String normalizeFramegenMode(String mode) {
        String normalized = mode == null ? FRAMEGEN_MODE_BALANCED : mode.trim().toLowerCase(Locale.ENGLISH);
        return switch (normalized) {
            case FRAMEGEN_MODE_QUALITY -> FRAMEGEN_MODE_QUALITY;
            case FRAMEGEN_MODE_LOW_LATENCY, "low-latency" -> FRAMEGEN_MODE_LOW_LATENCY;
            default -> FRAMEGEN_MODE_BALANCED;
        };
    }

    private String normalizeFgSource(String source) {
        String normalized = source == null ? FG_SOURCE_NATIVE : source.trim().toLowerCase(Locale.ENGLISH);
        return switch (normalized) {
            case FG_SOURCE_OPTI_FG, "optifg" -> FG_SOURCE_OPTI_FG;
            default -> FG_SOURCE_NATIVE;
        };
    }

    private String normalizeFgOutput(String output) {
        String normalized = output == null ? FG_OUTPUT_AUTO : output.trim().toLowerCase(Locale.ENGLISH);
        return switch (normalized) {
            case FG_OUTPUT_LSFG -> FG_OUTPUT_LSFG;
            case "dlssg_to_fsr3", "dlssg-to-fsr3", "dlssgtofsr3" -> FG_OUTPUT_LSFG;
            default -> FG_OUTPUT_AUTO;
        };
    }


    public static void loadScreenSizeSpinner(View view, String selectedValue, boolean isDarkMode) {
        final Spinner sScreenSize = view.findViewById(R.id.SScreenSize);

        final LinearLayout llCustomScreenSize = view.findViewById(R.id.LLCustomScreenSize);

        applyDarkThemeToEditText(view.findViewById(R.id.ETScreenWidth), isDarkMode);
        applyDarkThemeToEditText(view.findViewById(R.id.ETScreenHeight), isDarkMode);


        sScreenSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String value = sScreenSize.getItemAtPosition(position).toString();
                llCustomScreenSize.setVisibility(value.equalsIgnoreCase("custom") ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        boolean found = AppUtils.setSpinnerSelectionFromIdentifier(sScreenSize, selectedValue);
        if (!found) {
            AppUtils.setSpinnerSelectionFromValue(sScreenSize, "custom");
            String[] screenSize = selectedValue.split("x");
            ((EditText)view.findViewById(R.id.ETScreenWidth)).setText(screenSize[0]);
            ((EditText)view.findViewById(R.id.ETScreenHeight)).setText(screenSize[1]);
        }
    }

    private void applyDynamicStyles(View view, boolean isDarkMode) {
        SpinnerAdapters.applySurfaceRecursively(view, isDarkMode);

        // Update edit text
        EditText etName = view.findViewById(R.id.ETName);
        applyDarkThemeToEditText(etName, isDarkMode);

        // Update Spinners
        Spinner sGraphicsDriver = view.findViewById(R.id.SGraphicsDriver);
        Spinner sDXWrapper = view.findViewById(R.id.SDXWrapper);
        Spinner sAudioDriver = view.findViewById(R.id.SAudioDriver);
        Spinner sEmulatorSpinner = view.findViewById(R.id.SEmulator);
        Spinner sBox64Preset = view.findViewById(R.id.SBox64Preset);
        Spinner sControlsProfile = view.findViewById(R.id.SControlsProfile);
        Spinner sDInputType = view.findViewById(R.id.SDInputType);
        Spinner sMIDISoundFont = view.findViewById(R.id.SMIDISoundFont);
        Spinner sBox64Version = view.findViewById(R.id.SBox64Version);
        Spinner sFEXCoreVersion = view.findViewById(R.id.SFEXCoreVersion);
        Spinner sFEXCorePreset = view.findViewById(R.id.SFEXCorePreset);
        Spinner sTouchpadGestureProfile = view.findViewById(R.id.STouchpadGestureProfile);
        Spinner sStartupSelection = findViewById(R.id.SStartupSelection);
        Spinner sUpscalerPreset = view.findViewById(R.id.SUpscalerPreset);
        Spinner sUpscalerBackend = view.findViewById(R.id.SUpscalerBackend);
        Spinner sUpscalerEffect = view.findViewById(R.id.SUpscalerEffect);
        Spinner sUpscalerScale = view.findViewById(R.id.SUpscalerScale);
        Spinner sGeneratedFrames = view.findViewById(R.id.SGeneratedFrames);
        Spinner sUpscalerFgSource = view.findViewById(R.id.SUpscalerFgSource);
        Spinner sUpscalerFgOutput = view.findViewById(R.id.SUpscalerFgOutput);
        Spinner sUpscalerFramegenMode = view.findViewById(R.id.SUpscalerFramegenMode);


        // Set dark or light mode background for spinners
        sGraphicsDriver.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
        sDXWrapper.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
        sAudioDriver.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
        sEmulatorSpinner.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
        sBox64Preset.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
        sControlsProfile.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
        sDInputType.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
        sMIDISoundFont.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
        sBox64Version.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
        sFEXCorePreset.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
        sFEXCoreVersion.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
        sTouchpadGestureProfile.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
        sStartupSelection.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
        sUpscalerPreset.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
        sUpscalerBackend.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
        sUpscalerEffect.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
        sUpscalerScale.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
        sGeneratedFrames.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
        sUpscalerFgSource.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
        sUpscalerFgOutput.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
        sUpscalerFramegenMode.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);

//        EditText etLC_ALL = view.findViewById(R.id.ETlcall);
        EditText etExecArgs = view.findViewById(R.id.ETExecArgs);

//        applyDarkThemeToEditText(etLC_ALL, isDarkMode);
        applyDarkThemeToEditText(etExecArgs, isDarkMode);

    }

    private void applyFieldSetLabelStyle(TextView textView, boolean isDarkMode) {
        if (textView == null) return;
        Context context = textView.getContext();
        textView.setBackgroundResource(isDarkMode
                ? R.drawable.surface_badge_background_dark
                : R.drawable.surface_badge_background);
        textView.setTextColor(ContextCompat.getColor(
                context,
                isDarkMode ? R.color.surface_badge_text_dark : R.color.surface_badge_text
        ));
        textView.bringToFront();
    }

    private void applyModernSectionCardStyle(TextView textView, boolean isDarkMode) {
        if (textView == null) return;
        applyFieldSetLabelStyle(textView, isDarkMode);
        View parent = (View) textView.getParent();
        if (!(parent instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) parent;
        int panelBackground = isDarkMode
                ? R.drawable.surface_card_background_dark
                : R.drawable.surface_card_background;
        int horizontalPadding = dpToPx(12f);
        int topPadding = dpToPx(18f);
        int bottomPadding = dpToPx(12f);
        int topMargin = dpToPx(6f);

        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (!(child instanceof LinearLayout)) continue;
            child.setBackgroundResource(panelBackground);
            child.setPadding(horizontalPadding, topPadding, horizontalPadding, bottomPadding);
            ViewGroup.LayoutParams rawParams = child.getLayoutParams();
            if (rawParams instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) rawParams;
                marginParams.topMargin = topMargin;
                child.setLayoutParams(marginParams);
            }
        }
    }

    private int dpToPx(float dp) {
        return Math.round(dp * getContext().getResources().getDisplayMetrics().density);
    }

    private static void applyDarkThemeToEditText(EditText editText, boolean isDarkMode) {
        int textColor = ContextCompat.getColor(
                editText.getContext(),
                isDarkMode ? R.color.surface_badge_text_dark : R.color.surface_badge_text
        );
        int hintColor = ContextCompat.getColor(
                editText.getContext(),
                isDarkMode ? R.color.surface_body_text_dark : R.color.surface_body_text
        );
        editText.setTextColor(textColor);
        editText.setHintTextColor(hintColor);
        editText.setBackgroundResource(isDarkMode ? R.drawable.edit_text_dark : R.drawable.edit_text);
    }

    private void updateExtra(String extraName, String containerValue, String newValue) {
        String extraValue = shortcut.getExtra(extraName);
        if (extraValue.isEmpty() && containerValue.equals(newValue))
            return;
        shortcut.putExtra(extraName, newValue);
    }

    private void renameShortcut(String newName) {
        File parent = shortcut.file.getParentFile();
        File oldDesktopFile = shortcut.file; // Reference to the old file
        File newDesktopFile = new File(parent, newName + ".desktop");

        // Rename the desktop file if the new one doesn't exist
        if (!newDesktopFile.isFile() && oldDesktopFile.renameTo(newDesktopFile)) {
            // Successfully renamed, update the shortcut's file reference
            updateShortcutFileReference(newDesktopFile); // New helper method

            // As a precaution, delete any remaining old file
            deleteOldFileIfExists(oldDesktopFile);
        }

        // Rename link file if applicable
        File linkFile = new File(parent, shortcut.name + ".lnk");
        if (linkFile.isFile()) {
            File newLinkFile = new File(parent, newName + ".lnk");
            if (!newLinkFile.isFile()) linkFile.renameTo(newLinkFile);
        }

        fragment.loadShortcutsList();
        fragment.updateShortcutOnScreen(newName, newName, shortcut.container.id, newDesktopFile.getAbsolutePath(),
                Icon.createWithBitmap(shortcut.icon), shortcut.getExtra("uuid"));
    }

    // Method to ensure no old file remains
    private void deleteOldFileIfExists(File oldFile) {
        if (oldFile.exists()) {
            if (!oldFile.delete()) {
                Log.e("ShortcutSettingsDialog", "Failed to delete old file: " + oldFile.getPath());
            }
        }
    }

    // Update the shortcut's file reference to ensure saveData() writes to the correct file
    private void updateShortcutFileReference(File newFile) {
        try {
            Field fileField = Shortcut.class.getDeclaredField("file");
            fileField.setAccessible(true);
            fileField.set(shortcut, newFile);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e("ShortcutSettingsDialog", "Error updating shortcut file reference", e);
        }
    }


    private EnvVarsView createEnvVarsTab() {
        final View view = getContentView();
        final Context context = view.getContext();

        // Retrieve the existing EnvVarsView
        final EnvVarsView envVarsView = view.findViewById(R.id.EnvVarsView);

        // Update the dark mode setting of the existing instance
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        envVarsView.setDarkMode(isDarkMode);

        // Set the environment variables in the existing EnvVarsView
        envVarsView.setEnvVars(new EnvVars(shortcut.getExtra("envVars")));

        // Set the click listener for adding new environment variables
        view.findViewById(R.id.BTAddEnvVar).setOnClickListener((v) ->
                new AddEnvVarDialog(context, envVarsView).show()
        );

        return envVarsView;
    }

    private void loadControlsProfileSpinner(Spinner spinner, String selectedValue) {
        final Context context = fragment.getContext();
        final boolean isDarkMode = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("dark_mode", false);
        final ArrayList<ControlsProfile> profiles = inputControlsManager.getProfiles(true);
        ArrayList<String> values = new ArrayList<>();
        values.add(context.getString(R.string.none));

        int selectedPosition = 0;
        int selectedId = Integer.parseInt(selectedValue);
        for (int i = 0; i < profiles.size(); i++) {
            ControlsProfile profile = profiles.get(i);
            if (profile.id == selectedId) selectedPosition = i + 1;
            values.add(profile.getName());
        }

        spinner.setAdapter(SpinnerAdapters.create(context, isDarkMode, values));
        spinner.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
        spinner.setSelection(selectedPosition, false);
    }

    private void showInputWarning() {
        final Context context = fragment.getContext();
        ContentDialog.alert(context, R.string.enable_xinput_and_dinput_same_time, null);
    }

    public static void loadBox64VersionSpinner(Context context, ContentsManager manager, Spinner spinner, boolean isArm64EC) {
        ContainerDetailFragment.loadBox64VersionSpinner(context, null, manager, spinner, isArm64EC);
    }

    public void loadGraphicsDriverSpinner(final Spinner sGraphicsDriver, final Spinner sDXWrapper, final View vGraphicsDriverConfig, String selectedGraphicsDriver, String selectedDXWrapper) {
        final Context context = sGraphicsDriver.getContext();
        final boolean isDarkMode = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("dark_mode", false);
        final String initialGraphicsDriver = GraphicsDrivers.getTopLevelSelectableDriver(selectedGraphicsDriver);
        Object initialGraphicsDriverConfigTag = vGraphicsDriverConfig.getTag();
        String initialGraphicsDriverConfig = initialGraphicsDriverConfigTag instanceof String
                ? (String) initialGraphicsDriverConfigTag
                : GraphicsDrivers.defaultConfig(selectedGraphicsDriver);
        vGraphicsDriverConfig.setTag(
                GraphicsDrivers.migrateToUnifiedTopLevelConfig(selectedGraphicsDriver, initialGraphicsDriverConfig)
        );

        ContainerDetailFragment.updateGraphicsDriverSpinner(context, sGraphicsDriver);

        final String[] dxwrapperEntries = context.getResources().getStringArray(R.array.dxwrapper_entries);

        Runnable update = () -> {
            String graphicsDriver = Container.normalizeGraphicsDriver(StringUtils.parseIdentifier(sGraphicsDriver.getSelectedItem()));
            Object graphicsDriverConfigTag = vGraphicsDriverConfig.getTag();
            String graphicsDriverConfig = graphicsDriverConfigTag instanceof String
                    ? (String) graphicsDriverConfigTag
                    : GraphicsDrivers.defaultConfig(graphicsDriver);
            graphicsDriverConfig = GraphicsDrivers.sanitizeConfigShape(graphicsDriver, graphicsDriverConfig);
            vGraphicsDriverConfig.setTag(graphicsDriverConfig);

            tvGraphicsDriverVersion.setText(GraphicsDrivers.getDisplayVersion(context, graphicsDriver, graphicsDriverConfig));

            vGraphicsDriverConfig.setOnClickListener((v) -> {
                try {
                    if (GraphicsDrivers.isVortek(graphicsDriver)) {
                        new VortekConfigDialog(vGraphicsDriverConfig).show();
                    } else if (GraphicsDrivers.isVirgl(graphicsDriver)) {
                        new VirGLConfigDialog(vGraphicsDriverConfig).show();
                    } else if (GraphicsDrivers.isMesaOpenGlBridge(graphicsDriver)) {
                        new MesaOpenGLConfigDialog(vGraphicsDriverConfig, graphicsDriver).show();
                    } else if (GraphicsDrivers.isGladio(graphicsDriver)) {
                        new VortekConfigDialog(vGraphicsDriverConfig, graphicsDriver).show();
                    } else {
                        new GraphicsDriverConfigDialog(vGraphicsDriverConfig, graphicsDriver, tvGraphicsDriverVersion).show();
                    }
                } catch (Throwable t) {
                    Log.e("ShortcutSettingsDialog", "Failed to open graphics driver config dialog", t);
                    ForensicLogger.error(
                            context,
                            "GRAPHICS_DRIVER_CONFIG_OPEN_FAIL",
                            null,
                            "graphics_config",
                            "graphics_driver_config_open_failed",
                            t,
                            ForensicLogger.fields(
                                    "graphics_driver", graphicsDriver,
                                    "config_tag", String.valueOf(vGraphicsDriverConfig.getTag()),
                                    "scope", "shortcut"
                            )
                    );
                    AppUtils.showToast(context, R.string.unable_to_open_graphics_driver_configuration);
                }
            });

            ArrayList<String> items = new ArrayList<>();
            for (String value : dxwrapperEntries) {
                    items.add(value);
            }
            sDXWrapper.setAdapter(SpinnerAdapters.create(context, isDarkMode, items));
            sDXWrapper.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
            AppUtils.setSpinnerSelectionFromIdentifier(sDXWrapper, selectedDXWrapper);
        };

        sGraphicsDriver.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                update.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        AppUtils.setSpinnerSelectionFromIdentifier(sGraphicsDriver, initialGraphicsDriver);
        update.run();
    }
}
