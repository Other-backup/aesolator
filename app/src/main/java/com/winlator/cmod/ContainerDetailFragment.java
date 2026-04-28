package com.winlator.cmod;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.tabs.TabLayout;
import com.winlator.cmod.box64.Box64Preset;
import com.winlator.cmod.box64.Box64PresetManager;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.container.GraphicsDrivers;
import com.winlator.cmod.contentdialog.AddEnvVarDialog;
import com.winlator.cmod.contentdialog.ContentDialog;
import com.winlator.cmod.contentdialog.DXVKConfigDialog;
import com.winlator.cmod.contentdialog.DgVoodooConfigDialog;
import com.winlator.cmod.contentdialog.GraphicsDriverConfigDialog;
import com.winlator.cmod.contentdialog.MesaOpenGLConfigDialog;
import com.winlator.cmod.contentdialog.ShortcutSettingsDialog;
import com.winlator.cmod.contentdialog.VirGLConfigDialog;
import com.winlator.cmod.contentdialog.VortekConfigDialog;
import com.winlator.cmod.contentdialog.WineD3DConfigDialog;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.contents.AdrenotoolsManager;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.Callback;
import com.winlator.cmod.core.DefaultVersion;
import com.winlator.cmod.core.EnvVars;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.core.KeyValueSet;
import com.winlator.cmod.core.PreloaderDialog;
import com.winlator.cmod.core.SpinnerAdapters;
import com.winlator.cmod.core.StringUtils;
import com.winlator.cmod.core.ThemeAssetPainter;
import com.winlator.cmod.core.UiLifecycleGuard;
import com.winlator.cmod.core.WineUtils;
import com.winlator.cmod.core.UnitUtils;
import com.winlator.cmod.core.UpscalerProfileStore;
import com.winlator.cmod.core.WineInfo;
import com.winlator.cmod.core.WineRegistryEditor;
import com.winlator.cmod.core.WineThemeManager;
import com.winlator.cmod.fexcore.FEXCoreManager;
import com.winlator.cmod.fexcore.FEXCorePreset;
import com.winlator.cmod.fexcore.FEXCorePresetManager;
import com.winlator.cmod.midi.MidiManager;
import com.winlator.cmod.widget.CPUListView;
import com.winlator.cmod.widget.ColorPickerView;
import com.winlator.cmod.widget.EnvVarsView;
import com.winlator.cmod.widget.ImagePickerView;
import com.winlator.cmod.winhandler.WinHandler;
import com.winlator.cmod.xenvironment.ImageFs;
import com.winlator.cmod.xserver.XKeycode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public class ContainerDetailFragment extends Fragment {

    private static final String TAG = "FileUtils";

    private ContainerManager manager;
    private ContentsManager contentsManager;
    private final int containerId;
    private static Container container;
    private PreloaderDialog preloaderDialog;
    private JSONArray gpuCards;
    private Callback<String> openDirectoryCallback;
    private boolean runtimeSelectionRefreshQueued;

    private static boolean isDarkMode;

    public ContainerDetailFragment() {
        this(0);
    }

    public ContainerDetailFragment(int containerId) {
        this.containerId = containerId;
    }

    private static final String[] SDL2_ENV_VARS = {
            "SDL_JOYSTICK_WGI=0",
            "SDL_XINPUT_ENABLED=1",
            "SDL_JOYSTICK_RAWINPUT=0",
            "SDL_JOYSTICK_HIDAPI=0",
            "SDL_DIRECTINPUT_ENABLED=0",
            "SDL_JOYSTICK_ALLOW_BACKGROUND_EVENTS=1",
            "SDL_GAMECONTROLLER_ALLOW_STEAM_VIRTUAL_GAMEPAD=1",
            "PROTON_ENABLE_HIDRAW=0",
            "SDL_HINT_FORCE_RAISEWINDOW=0",
            "SDL_ALLOW_TOPMOST=0",
            "SDL_MOUSE_FOCUS_CLICKTHROUGH=1"
    };

    private static boolean resolveDarkMode(@Nullable Context context) {
        if (context == null) return isDarkMode;
        try {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("dark_mode", isDarkMode);
        } catch (Exception ignored) {
            return isDarkMode;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        preloaderDialog = new PreloaderDialog(getActivity());

        try {
            gpuCards = new JSONArray(FileUtils.readString(getContext(), "gpu_cards.json"));
        }
        catch (JSONException e) {
            Log.w("ContainerDetailFragment", "Failed to parse gpu_cards.json", e);
            gpuCards = new JSONArray();
        }
    }

    private static void applyFieldSetLabelStyle(TextView textView, boolean isDarkMode) {
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

    private static void applyModernSectionCardStyle(TextView textView, boolean isDarkMode) {
        if (textView == null) return;
        applyFieldSetLabelStyle(textView, isDarkMode);
        View parent = (View) textView.getParent();
        if (!(parent instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) parent;
        int panelBackground = isDarkMode
                ? R.drawable.surface_card_background_dark
                : R.drawable.surface_card_background;
        int horizontalPadding = dpToPx(textView.getContext(), 12f);
        int topPadding = dpToPx(textView.getContext(), 18f);
        int bottomPadding = dpToPx(textView.getContext(), 12f);
        int topMargin = dpToPx(textView.getContext(), 6f);

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

    private static int dpToPx(Context context, float dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    private static String getSelectedText(@Nullable Spinner spinner, @NonNull String fallback) {
        if (spinner == null) return fallback;
        Object selected = spinner.getSelectedItem();
        if (selected == null) return fallback;
        String value = selected.toString();
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static String getSelectedIdentifier(@Nullable Spinner spinner, @NonNull String fallback) {
        String selectedText = getSelectedText(spinner, fallback);
        return selectedText.isEmpty() ? fallback : StringUtils.parseIdentifier(selectedText);
    }

    private boolean isRuntimeSelectionAvailable(@Nullable Spinner spinner) {
        if (spinner == null || !spinner.isEnabled()) return false;
        return !AppUtils.isMissingComponentValue(getSelectedText(spinner, ""));
    }

    private void updateRuntimeSelectionUi(@NonNull View view) {
        Context context = view.getContext();
        boolean darkMode = resolveDarkMode(context);
        boolean runtimeReady = isRuntimeSelectionAvailable(view.findViewById(R.id.SWineVersion));
        TextView runtimeHint = view.findViewById(R.id.TVWineVersionHint);
        Button openContents = view.findViewById(R.id.BTOpenContents);
        Button confirmButton = view.findViewById(R.id.BTConfirm);

        runtimeHint.setText(runtimeReady
                ? R.string.container_runtime_hint
                : R.string.install_runtime_before_container);
        runtimeHint.setTextColor(ContextCompat.getColor(
                context,
                runtimeReady
                        ? (darkMode ? R.color.surface_body_text_dark : R.color.surface_body_text)
                        : (darkMode ? R.color.surface_badge_text_dark : R.color.surface_badge_text)
        ));

        openContents.setVisibility(runtimeReady ? View.GONE : View.VISIBLE);
        confirmButton.setEnabled(runtimeReady);
        confirmButton.setAlpha(runtimeReady ? 1f : 0.55f);
    }

    private void openContentsForRuntimeInstall() {
        Context context = getContext();
        if (context != null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            preferences.edit()
                    .putString("contents_preselected_type", ContentProfile.ContentType.CONTENT_TYPE_WINE.toString())
                    .putString("contents_source_mode", "archive")
                    .putString("contents_channel_mode", "stable")
                    .putString("contents_arch_mode", "all")
                    .remove("contents_preselected_display_category")
                    .apply();
        }

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity instanceof MainActivity mainActivity) {
            mainActivity.openMainMenuItem(R.id.main_menu_contents, false);
            return;
        }

        UiLifecycleGuard.commit(
                requireActivity(),
                getParentFragmentManager().beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_up,
                        R.anim.slide_out_down,
                        R.anim.slide_in_down,
                        R.anim.slide_out_up
                )
                .addToBackStack(null)
                .replace(R.id.FLFragmentContainer, new ContentsFragment()),
                "ContainerDetailFragment",
                "open_contents_for_runtime_install"
        );
    }

    private void refreshRuntimeSelectionIfNeeded(@NonNull View view) {
        Context context = getContext();
        if (context == null) return;
        if (contentsManager == null) contentsManager = new ContentsManager(context);
        contentsManager.syncContents();

        Spinner sWineVersion = view.findViewById(R.id.SWineVersion);
        Spinner sBox64Version = view.findViewById(R.id.SBox64Version);
        if (sWineVersion != null && sBox64Version != null && !sWineVersion.isEnabled()) {
            loadWineVersionSpinner(view, sWineVersion, sBox64Version);
        }
        updateRuntimeSelectionUi(view);
        if (isRuntimeSelectionAvailable(sWineVersion) || runtimeSelectionRefreshQueued) return;

        runtimeSelectionRefreshQueued = true;
        contentsManager.syncContentsAsync(() -> {
            runtimeSelectionRefreshQueued = false;
            if (!isAdded()) return;
            View currentView = getView();
            if (currentView == null) return;
            Spinner currentWineVersion = currentView.findViewById(R.id.SWineVersion);
            Spinner currentBox64Version = currentView.findViewById(R.id.SBox64Version);
            if (currentWineVersion != null && currentBox64Version != null) {
                loadWineVersionSpinner(currentView, currentWineVersion, currentBox64Version);
            }
            updateRuntimeSelectionUi(currentView);
        });
    }

    private void applyDynamicStyles(View view, boolean isDarkMode) {
        SpinnerAdapters.applySurfaceRecursively(view, isDarkMode);
    }

    private void applyDynamicStylesRecursively(View view, boolean isDarkMode) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                applyDynamicStylesRecursively(child, isDarkMode);
            }
        } else if (view instanceof TextView) {
            TextView textView = (TextView) view;
            if ("desktop".equals(textView.getText().toString())) { // Check for specific text if needed
                textView.setTextAppearance(getContext(), isDarkMode ? R.style.FieldSetLabel_Dark : R.style.FieldSetLabel);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == MainActivity.OPEN_DIRECTORY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                Log.d(TAG, "URI obtained in onActivityResult: " + uri.toString());
                String path = FileUtils.getFilePathFromUri(getContext(), uri);
                Log.d(TAG, "File path in onActivityResult: " + path);
                if (path != null) {
                    if (openDirectoryCallback != null) {
                        openDirectoryCallback.call(path);
                    }
                } else {
                    Toast.makeText(getContext(), R.string.invalid_directory_selected, Toast.LENGTH_SHORT).show();
                }
            }
            openDirectoryCallback = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        View view = getView();
        if (view != null) {
            refreshRuntimeSelectionIfNeeded(view);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(isEditMode() ? R.string.edit_container : R.string.new_container);

        // Find TextViews by ID and apply dynamic styles
        TextView desktopLabel = view.findViewById(R.id.TVDesktop);
        applyModernSectionCardStyle(desktopLabel, isDarkMode);

        TextView registryKeysLabel = view.findViewById(R.id.TVDirectInput);
        applyModernSectionCardStyle(registryKeysLabel, isDarkMode);

        // Win Components TextViews
        TextView directXLabel = view.findViewById(R.id.TVDirectX);
        applyModernSectionCardStyle(directXLabel, isDarkMode);

        TextView generalLabel = view.findViewById(R.id.TVGeneral);
        applyModernSectionCardStyle(generalLabel, isDarkMode);

        // Advanced Tab TextViews
        TextView box64Label = view.findViewById(R.id.TVBox64);
        applyModernSectionCardStyle(box64Label, isDarkMode);

        TextView fexCoreLabel = view.findViewById(R.id.TVFEXCore);
        applyModernSectionCardStyle(fexCoreLabel, isDarkMode);

        TextView systemLabel = view.findViewById(R.id.TVSystem);
        applyModernSectionCardStyle(systemLabel, isDarkMode);

        TextView gameControllerLabel = view.findViewById(R.id.TVGameController);
        applyModernSectionCardStyle(gameControllerLabel, isDarkMode);
        TextView containerFramegenLabel = view.findViewById(R.id.TVContainerFramegen);
        applyModernSectionCardStyle(containerFramegenLabel, isDarkMode);
        ThemeAssetPainter.apply(view.getContext(), view, isDarkMode);

    }

    public boolean isEditMode() {
        return container != null;
    }

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup root, @Nullable Bundle savedInstanceState) {
        final Context context = getContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final View view = inflater.inflate(R.layout.container_detail_fragment, root, false);

        // Keep the fragment theme in sync with the current app preference.
        isDarkMode = preferences.getBoolean("dark_mode", false);

        // Apply dynamic styles
        applyDynamicStyles(view, isDarkMode);

        // Apply dynamic styles recursively
//        applyDynamicStylesRecursively(view, isDarkMode);

        manager = new ContainerManager(context);
        container = containerId > 0 ? manager.getContainerById(containerId) : null;
        contentsManager = new ContentsManager(context);
        contentsManager.syncContents();

        final EditText etName = view.findViewById(R.id.ETName);
        final Button btConfirm = view.findViewById(R.id.BTConfirm);
        final Button btCancel = view.findViewById(R.id.BTCancel);
        final Button btOpenContents = view.findViewById(R.id.BTOpenContents);

        final Spinner sWineVersion = view.findViewById(R.id.SWineVersion);



        // Ensure the Wine version layout is visible
        final LinearLayout llWineVersion = view.findViewById(R.id.LLWineVersion);
        llWineVersion.setVisibility(View.VISIBLE);

        // Set container name and graphics driver version based on mode
        if (isEditMode()) {
            etName.setText(container.getName());
        } else {
            etName.setText(getString(R.string.container) + "-" + manager.getNextContainerId());
        }

        final Spinner sBox64Version = view.findViewById(R.id.SBox64Version);

        loadWineVersionSpinner(view, sWineVersion, sBox64Version);
        btConfirm.setText(isEditMode() ? R.string.save : R.string.create);
        btCancel.setOnClickListener((v) -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                if (UiLifecycleGuard.popBackStack(this, "ContainerDetailFragment", "cancel_pop_backstack")) {
                    return;
                }
            } else if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showHomeDashboard(true);
                return;
            }
            UiLifecycleGuard.dispatchBackPress(this, "ContainerDetailFragment", "cancel_back");
        });
        btOpenContents.setOnClickListener((v) -> openContentsForRuntimeInstall());
        updateRuntimeSelectionUi(view);

        loadScreenSizeSpinner(view, isEditMode() ? container.getScreenSize() : Container.DEFAULT_SCREEN_SIZE);

        final Spinner sGraphicsDriver = view.findViewById(R.id.SGraphicsDriver);

        final Spinner sDXWrapper = view.findViewById(R.id.SDXWrapper);

        final View vDXWrapperConfig = view.findViewById(R.id.BTDXWrapperConfig);
        vDXWrapperConfig.setTag(isEditMode() ? container.getDXWrapperConfig() : Container.DEFAULT_DXWRAPPERCONFIG);

        final View vGraphicsDriverConfig = view.findViewById(R.id.BTGraphicsDriverConfig);
        vGraphicsDriverConfig.setTag(isEditMode() ? container.getGraphicsDriverConfig() : Container.DEFAULT_GRAPHICSDRIVERCONFIG);
        final String oldGraphicsDriverConfig = vGraphicsDriverConfig.getTag() instanceof String
                ? (String) vGraphicsDriverConfig.getTag()
                : Container.DEFAULT_GRAPHICSDRIVERCONFIG;

        loadGraphicsDriverSpinner(sGraphicsDriver, sDXWrapper, vGraphicsDriverConfig,
                isEditMode() ? container.getGraphicsDriver() : Container.DEFAULT_GRAPHICS_DRIVER,
                isEditMode() ? container.getDXWrapper() : Container.DEFAULT_DXWRAPPER);

        view.findViewById(R.id.BTHelpDXWrapper).setOnClickListener((v) -> AppUtils.showHelpBox(context, v, R.string.dxwrapper_help_content));

        Spinner sAudioDriver = view.findViewById(R.id.SAudioDriver);
        AppUtils.setSpinnerSelectionFromIdentifier(sAudioDriver, isEditMode() ? container.getAudioDriver() : Container.DEFAULT_AUDIO_DRIVER);

        Spinner sEmulator = view.findViewById(R.id.SEmulator);
        AppUtils.setSpinnerSelectionFromIdentifier(sEmulator, isEditMode() ? container.getEmulator() : Container.DEFAULT_EMULATOR);

        Spinner sMIDISoundFont = view.findViewById(R.id.SMIDISoundFont);
        MidiManager.loadSFSpinner(sMIDISoundFont);
        AppUtils.setSpinnerSelectionFromValue(sMIDISoundFont, isEditMode() ? container.getMIDISoundFont() : "");

        final CompoundButton cbShowFPS = view.findViewById(R.id.CBShowFPS);
        cbShowFPS.setChecked(isEditMode() && container.isShowFPS());

        final CompoundButton cbFullscreenStretched = view.findViewById(R.id.CBFullscreenStretched);
        cbFullscreenStretched.setChecked(isEditMode() && container.isFullscreenStretched());
        final CompoundButton cbContainerFgEnable = view.findViewById(R.id.CBContainerFgEnable);
        final Spinner sContainerFgPreset = view.findViewById(R.id.SContainerFgPreset);
        final Spinner sContainerFgMode = view.findViewById(R.id.SContainerFgMode);
        final CompoundButton cbContainerFgThermalGuard = view.findViewById(R.id.CBContainerFgThermalGuard);
        UpscalerProfileStore.Profile globalUpscalerProfile = UpscalerProfileStore.getSelectedProfile(preferences);
        String containerFgPreset = UpscalerProfileStore.normalizePreset(isEditMode()
                ? container.getExtra("upscalerPreset", globalUpscalerProfile.preset)
                : globalUpscalerProfile.preset);
        String containerFgMode = UpscalerProfileStore.normalizeFramegenMode(isEditMode()
                ? container.getExtra("upscalerFramegenMode", globalUpscalerProfile.framegenMode)
                : globalUpscalerProfile.framegenMode);
        boolean containerFgEnable = "1".equals(isEditMode()
                ? container.getExtra("upscalerFrameGeneration", globalUpscalerProfile.frameGeneration ? "1" : "0")
                : (globalUpscalerProfile.frameGeneration ? "1" : "0"));
        boolean containerFgThermalGuard = "1".equals(isEditMode()
                ? container.getExtra("upscalerThermalGuard", globalUpscalerProfile.thermalGuard ? "1" : "0")
                : (globalUpscalerProfile.thermalGuard ? "1" : "0"));
        AppUtils.setSpinnerSelectionFromIdentifier(sContainerFgPreset, containerFgPreset);
        AppUtils.setSpinnerSelectionFromIdentifier(sContainerFgMode, containerFgMode);
        cbContainerFgEnable.setChecked(containerFgEnable);
        cbContainerFgThermalGuard.setChecked(containerFgThermalGuard);
        Runnable updateContainerFgState = () -> {
            boolean enabled = cbContainerFgEnable.isChecked();
            sContainerFgPreset.setEnabled(enabled);
            sContainerFgMode.setEnabled(enabled);
            cbContainerFgThermalGuard.setEnabled(enabled);
        };
        cbContainerFgEnable.setOnCheckedChangeListener((buttonView, isChecked) -> updateContainerFgState.run());
        updateContainerFgState.run();

        // Existing declarations of UI components and variables
        final Runnable showInputWarning = () -> ContentDialog.alert(context, R.string.enable_xinput_and_dinput_same_time, null);
        final CompoundButton cbEnableXInput = view.findViewById(R.id.CBEnableXInput);
        final CompoundButton cbEnableDInput = view.findViewById(R.id.CBEnableDInput);
        final View llDInputType = view.findViewById(R.id.LLDinputMapperType);
        final View btHelpXInput = view.findViewById(R.id.BTXInputHelp);
        final View btHelpDInput = view.findViewById(R.id.BTDInputHelp);
        final Spinner SDInputType = view.findViewById(R.id.SDInputType);

        // Check if we are in edit mode to set input type accordingly
        int inputType = isEditMode() ? container.getInputType() : WinHandler.DEFAULT_INPUT_TYPE;

        // New logic for enabling XInput and DInput
        cbEnableXInput.setChecked((inputType & WinHandler.FLAG_INPUT_TYPE_XINPUT) == WinHandler.FLAG_INPUT_TYPE_XINPUT);
        cbEnableDInput.setChecked((inputType & WinHandler.FLAG_INPUT_TYPE_DINPUT) == WinHandler.FLAG_INPUT_TYPE_DINPUT);

        cbEnableDInput.setOnCheckedChangeListener((buttonView, isChecked) -> {
            llDInputType.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked && cbEnableXInput.isChecked())
                showInputWarning.run();
        });

        cbEnableXInput.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && cbEnableDInput.isChecked())
                showInputWarning.run();
        });

        SDInputType.setSelection(((inputType & WinHandler.FLAG_DINPUT_MAPPER_STANDARD) == WinHandler.FLAG_DINPUT_MAPPER_STANDARD) ? 0 : 1);
        llDInputType.setVisibility(cbEnableDInput.isChecked() ? View.VISIBLE : View.GONE);

        btHelpXInput.setOnClickListener(v -> AppUtils.showHelpBox(context, v, R.string.help_xinput));
        btHelpDInput.setOnClickListener(v -> AppUtils.showHelpBox(context, v, R.string.help_dinput));

        final CompoundButton cbSdl2Toggle = view.findViewById(R.id.CBSdl2Toggle);
        cbSdl2Toggle.setChecked(isEditMode() && container.getEnvVars().contains("SDL_XINPUT_ENABLED=1"));

        final EditText etLC_ALL = view.findViewById(R.id.ETlcall);
        Locale systemLocal = Locale.getDefault();
        etLC_ALL.setText(isEditMode() ? container.getLC_ALL() : systemLocal.getLanguage() + '_' + systemLocal.getCountry() + ".UTF-8");

        final View btShowLCALL = view.findViewById(R.id.BTShowLCALL);
        btShowLCALL.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(context, v);
            String[] lcs = getResources().getStringArray(R.array.some_lc_all);
            for (int i = 0; i < lcs.length; i++)
                popupMenu.getMenu().add(Menu.NONE, i, Menu.NONE, lcs[i]);
            popupMenu.setOnMenuItemClickListener(item -> {
                etLC_ALL.setText(item.toString() + ".UTF-8");
                return true;
            });
            popupMenu.show();
        });

        final Spinner sStartupSelection = view.findViewById(R.id.SStartupSelection);
        byte previousStartupSelection = isEditMode() ? container.getStartupSelection() : -1;
        sStartupSelection.setSelection(previousStartupSelection != -1 ? previousStartupSelection : Container.STARTUP_SELECTION_ESSENTIAL);

        final Spinner sBox64Preset = view.findViewById(R.id.SBox64Preset);
        Box64PresetManager.loadSpinner("box64", sBox64Preset, isEditMode() ? container.getBox64Preset() : preferences.getString("box64_preset", Box64Preset.COMPATIBILITY));

        final Spinner sFEXCoreVersion = view.findViewById(R.id.SFEXCoreVersion);
        FEXCoreManager.loadFEXCoreVersion(context, contentsManager, sFEXCoreVersion, isEditMode() ? container.getFEXCoreVersion() : DefaultVersion.FEXCORE);

        final Spinner sFEXCorePreset = view.findViewById(R.id.SFEXCorePreset);
        FEXCorePresetManager.loadSpinner(sFEXCorePreset, isEditMode() ? container.getFEXCorePreset() : preferences.getString("fexcore_preset", FEXCorePreset.INTERMEDIATE));

        String selectedDriver = getSelectedText(sGraphicsDriver, Container.DEFAULT_GRAPHICS_DRIVER);
        updateGraphicsDriverSpinner(context, sGraphicsDriver);
        AppUtils.setSpinnerSelectionFromValue(sGraphicsDriver, selectedDriver);


        final CPUListView cpuListView = view.findViewById(R.id.CPUListView);
        final CPUListView cpuListViewWoW64 = view.findViewById(R.id.CPUListViewWoW64);

        cpuListView.setCheckedCPUList(isEditMode() ? container.getCPUList(true) : Container.getFallbackCPUList());
        cpuListViewWoW64.setCheckedCPUList(isEditMode() ? container.getCPUListWoW64(true) : Container.getFallbackCPUListWoW64());

        final Spinner sPrimaryController = view.findViewById(R.id.SPrimaryController);
        sPrimaryController.setSelection(isEditMode() ? container.getPrimaryController() : 1);
        setControllerMapping(view.findViewById(R.id.SButtonA), Container.XrControllerMapping.BUTTON_A, XKeycode.KEY_A.ordinal());
        setControllerMapping(view.findViewById(R.id.SButtonB), Container.XrControllerMapping.BUTTON_B, XKeycode.KEY_B.ordinal());
        setControllerMapping(view.findViewById(R.id.SButtonX), Container.XrControllerMapping.BUTTON_X, XKeycode.KEY_X.ordinal());
        setControllerMapping(view.findViewById(R.id.SButtonY), Container.XrControllerMapping.BUTTON_Y, XKeycode.KEY_Y.ordinal());
        setControllerMapping(view.findViewById(R.id.SButtonGrip), Container.XrControllerMapping.BUTTON_GRIP, XKeycode.KEY_SPACE.ordinal());
        setControllerMapping(view.findViewById(R.id.SButtonTrigger), Container.XrControllerMapping.BUTTON_TRIGGER, XKeycode.KEY_ENTER.ordinal());
        setControllerMapping(view.findViewById(R.id.SThumbstickUp), Container.XrControllerMapping.THUMBSTICK_UP, XKeycode.KEY_UP.ordinal());
        setControllerMapping(view.findViewById(R.id.SThumbstickDown), Container.XrControllerMapping.THUMBSTICK_DOWN, XKeycode.KEY_DOWN.ordinal());
        setControllerMapping(view.findViewById(R.id.SThumbstickLeft), Container.XrControllerMapping.THUMBSTICK_LEFT, XKeycode.KEY_LEFT.ordinal());
        setControllerMapping(view.findViewById(R.id.SThumbstickRight), Container.XrControllerMapping.THUMBSTICK_RIGHT, XKeycode.KEY_RIGHT.ordinal());

        createWineConfigurationTab(view);
        final EnvVarsView envVarsView = createEnvVarsTab(view);
        createWinComponentsTab(view, isEditMode() ? container.getWinComponents() : Container.DEFAULT_WINCOMPONENTS);
        createDrivesTab(view);

        AppUtils.setupTabLayout(view, R.id.TabLayout, R.id.LLTabWineConfiguration, R.id.LLTabWinComponents, R.id.LLTabEnvVars, R.id.LLTabDrives, R.id.LLTabAdvanced, R.id.LLTabXR);

        TabLayout tabLayout = view.findViewById(R.id.TabLayout);

        if (resolveDarkMode(context)) {
            tabLayout.setBackgroundResource(R.drawable.tab_layout_background_dark);
        } else {
            tabLayout.setBackgroundResource(R.drawable.tab_layout_background);
        }

        // Set up confirm button
        view.findViewById(R.id.BTConfirm).setOnClickListener((v) -> {
            try {
                // Capture and set container properties based on UI inputs
                String name = etName.getText().toString();
                String screenSize = getScreenSize(view);
                String envVars = envVarsView.getEnvVars();
                String selectedGraphicsDriver = getSelectedIdentifier(sGraphicsDriver, Container.DEFAULT_GRAPHICS_DRIVER);
                String graphicsDriver = Container.normalizeGraphicsDriver(selectedGraphicsDriver);
                String graphicsDriverConfig = vGraphicsDriverConfig.getTag() instanceof String
                        ? (String) vGraphicsDriverConfig.getTag()
                        : GraphicsDrivers.defaultConfig(graphicsDriver);
                graphicsDriverConfig = GraphicsDrivers.sanitizeConfigShape(graphicsDriver, graphicsDriverConfig);
                if (!GraphicsDrivers.usesKeyValueConfig(graphicsDriver)) {
                    graphicsDriverConfig = Container.reconcileLegacyGraphicsConfig(selectedGraphicsDriver, graphicsDriverConfig);
                    HashMap<String, String> config = GraphicsDrivers.parseConfig(graphicsDriver, graphicsDriverConfig);
                    String graphicsDriverVersion = config.get("version");
                    if (graphicsDriverVersion == null || graphicsDriverVersion.trim().isEmpty()) {
                        config.put("version", new AdrenotoolsManager(context).getPreferredWrapperDriverId());
                        graphicsDriverConfig = GraphicsDriverConfigDialog.toGraphicsDriverConfig(config);
                    }
                }
                String dxwrapper = getSelectedIdentifier(sDXWrapper, Container.DEFAULT_DXWRAPPER);
                String dxwrapperConfig = vDXWrapperConfig.getTag() instanceof String
                        ? (String) vDXWrapperConfig.getTag()
                        : "";
                String audioDriver = getSelectedIdentifier(sAudioDriver, Container.DEFAULT_AUDIO_DRIVER);
                String emulator = getSelectedIdentifier(sEmulator, Container.DEFAULT_EMULATOR);
                String wincomponents = getWinComponents(view);
                String drives = getDrives(view);
                boolean showFPS = cbShowFPS.isChecked();
                boolean fullscreenStretched = cbFullscreenStretched.isChecked();
                String cpuList = cpuListView.getCheckedCPUListAsString();
                String cpuListWoW64 = cpuListViewWoW64.getCheckedCPUListAsString();
                byte startupSelection = (byte) sStartupSelection.getSelectedItemPosition();
                String selectedWineVersion = getSelectedText(sWineVersion, "");
                if (AppUtils.isMissingComponentValue(selectedWineVersion)) {
                    if (isEditMode() && container != null && !AppUtils.isMissingComponentValue(container.getWineVersion())) {
                        selectedWineVersion = container.getWineVersion();
                    } else if (hasEmbeddedWineVersion(context, WineInfo.MAIN_WINE_VERSION.identifier())) {
                        selectedWineVersion = WineInfo.MAIN_WINE_VERSION.identifier();
                    } else {
                        AppUtils.showToast(context, R.string.install_runtime_before_container);
                        return;
                    }
                }
                String requestedRuntimeModel = ContentProfile.inferRuntimeModelFromEntryName(selectedWineVersion);
                selectedWineVersion = contentsManager.resolveBestRuntimeEntry(selectedWineVersion, requestedRuntimeModel);
                ContentProfile selectedRuntimeProfile = contentsManager.resolveBestRuntimeProfile(selectedWineVersion, requestedRuntimeModel);
                if (selectedRuntimeProfile != null && !selectedRuntimeProfile.getRuntimeModel().isEmpty()) {
                    requestedRuntimeModel = selectedRuntimeProfile.getRuntimeModel();
                }
                WineInfo selectedWineInfo = WineInfo.fromIdentifier(context, contentsManager, selectedWineVersion, requestedRuntimeModel);
                File selectedRuntimeRoot = selectedWineInfo.path == null || selectedWineInfo.path.trim().isEmpty()
                        ? null
                        : new File(selectedWineInfo.path);
                boolean runtimeReady = selectedRuntimeRoot != null && WineUtils.hasRuntimeCorePayload(selectedRuntimeRoot);
                ForensicLogger.logEvent(
                        context,
                        runtimeReady ? "info" : "warn",
                        "NEW_CONTAINER_RUNTIME_RESOLVE",
                        null,
                        "containers",
                        "runtime_resolve",
                        ForensicLogger.fields(
                                "selected_entry", selectedWineVersion,
                                "resolved_type", selectedWineInfo.type,
                                "resolved_version", selectedWineInfo.fullVersion(),
                                "resolved_arch", selectedWineInfo.getArch(),
                                "resolved_path", selectedWineInfo.path,
                                "runtime_model", requestedRuntimeModel,
                                "path_exists", selectedRuntimeRoot != null && selectedRuntimeRoot.exists(),
                                "runtime_ready", runtimeReady,
                                "runtime_payload_complete", selectedRuntimeRoot != null && WineUtils.hasRuntimePayload(selectedRuntimeRoot),
                                "prefix_pack_present", selectedRuntimeRoot != null && WineUtils.resolveRuntimePrefixPack(selectedRuntimeRoot) != null,
                                "profile_found", selectedRuntimeProfile != null,
                                "profile_type", selectedRuntimeProfile != null && selectedRuntimeProfile.type != null
                                        ? selectedRuntimeProfile.type.toString()
                                        : "",
                                "profile_ver_name", selectedRuntimeProfile != null ? selectedRuntimeProfile.verName : "",
                                "profile_ver_code", selectedRuntimeProfile != null ? selectedRuntimeProfile.verCode : -1
                        )
                );
                if (!runtimeReady) {
                    AppUtils.showToast(context, R.string.install_runtime_before_container);
                    return;
                }
                String containerUpscalerPreset = UpscalerProfileStore.normalizePreset(
                        getSelectedText(sContainerFgPreset, "auto")
                );
                String containerUpscalerFramegenMode = UpscalerProfileStore.normalizeFramegenMode(
                        getSelectedText(sContainerFgMode, "balanced")
                );
                String containerUpscalerFramegen = cbContainerFgEnable.isChecked() ? "1" : "0";
                String containerUpscalerThermalGuard = cbContainerFgThermalGuard.isChecked() ? "1" : "0";

                String box64Version = getSelectedText(sBox64Version, "");
                if (AppUtils.isMissingComponentValue(box64Version)) {
                    box64Version = isEditMode() ? container.getBox64Version() : "";
                }

                String fexcoreVersion = getSelectedText(sFEXCoreVersion, "");
                if (AppUtils.isMissingComponentValue(fexcoreVersion)) {
                    fexcoreVersion = isEditMode() ? container.getFEXCoreVersion() : "";
                }
                String fexcorePreset = FEXCorePresetManager.getSpinnerSelectedId(sFEXCorePreset);
                String box64Preset = Box64PresetManager.getSpinnerSelectedId(sBox64Preset);
                String desktopTheme = getDesktopTheme(view);
                // Capture missing properties
                String midiSoundFont = sMIDISoundFont.getSelectedItemPosition() == 0
                        ? ""
                        : getSelectedText(sMIDISoundFont, "");
                String lc_all = etLC_ALL.getText().toString();
                int primaryController = sPrimaryController.getSelectedItemPosition();
                if (primaryController < 0) primaryController = 1;
                String controllerMapping = getControllerMapping(view);

                // Define final input type
                int finalInputType = 0;
                int selectedInputMapper = SDInputType.getSelectedItemPosition();
                if (selectedInputMapper < 0) selectedInputMapper = 0;
                finalInputType |= cbEnableXInput.isChecked() ? WinHandler.FLAG_INPUT_TYPE_XINPUT : 0;
                finalInputType |= cbEnableDInput.isChecked() ? WinHandler.FLAG_INPUT_TYPE_DINPUT : 0;
                finalInputType |= selectedInputMapper == 0 ? WinHandler.FLAG_DINPUT_MAPPER_STANDARD : WinHandler.FLAG_DINPUT_MAPPER_XINPUT;

                // Handle SDL2 environment variables based on the toggle state
                if (cbSdl2Toggle.isChecked()) {
                    // Add SDL2 environment variables if the toggle is enabled
                    for (String envVar : SDL2_ENV_VARS) {
                        if (!envVars.contains(envVar)) {
                            envVars += (envVars.isEmpty() ? "" : " ") + envVar;
                        }
                    }
                } else {
                    // Remove SDL2 environment variables if the toggle is disabled
                    for (String envVar : SDL2_ENV_VARS) {
                        envVars = envVars.replace(envVar, "").replaceAll("\\s{2,}", " ").trim();
                    }
                }



                if (isEditMode()) {
                    // Update existing container properties
                    container.setName(name);
                    container.setScreenSize(screenSize);
                    container.setEnvVars(envVars);
                    container.setCPUList(cpuList);
                    container.setCPUListWoW64(cpuListWoW64);
                    container.setGraphicsDriver(graphicsDriver);
                    container.setGraphicsDriverConfig(graphicsDriverConfig);
                    container.setDXWrapper(dxwrapper);
                    container.setDXWrapperConfig(dxwrapperConfig);
                    container.setAudioDriver(audioDriver);
                    container.setEmulator(emulator);
                    container.setWinComponents(wincomponents);
                    container.setDrives(drives);
                    container.setShowFPS(showFPS);
                    container.setFullscreenStretched(fullscreenStretched);
                    container.setInputType(finalInputType);
                    container.setStartupSelection(startupSelection);
                    container.setBox64Version(box64Version);
                    container.setBox64Preset(box64Preset);
                    container.setFEXCoreVersion(fexcoreVersion);
                    container.setFEXCorePreset(fexcorePreset);
                    container.setDesktopTheme(desktopTheme);
                    container.setContainerVariant(requestedRuntimeModel);
                    container.setMidiSoundFont(midiSoundFont);
                    container.setLC_ALL(lc_all);
                    container.setPrimaryController(primaryController);
                    container.setControllerMapping(controllerMapping);
                    container.putExtra("upscalerPreset", containerUpscalerPreset);
                    container.putExtra("upscalerFramegenMode", containerUpscalerFramegenMode);
                    container.putExtra("upscalerFrameGeneration", containerUpscalerFramegen);
                    container.putExtra("upscalerThermalGuard", containerUpscalerThermalGuard);
                    container.putExtra("upscalerBackend", cbContainerFgEnable.isChecked() ? "mobfgsr" : null);
                    container.saveData();
                    saveWineRegistryKeys(view);
                    boolean requireRestart = GraphicsDrivers.isMediaTekWrapperFamily(graphicsDriver)
                            && VortekConfigDialog.isRequireRestart(oldGraphicsDriverConfig, graphicsDriverConfig);
                    if (requireRestart) {
                        ContentDialog.confirm(
                                context,
                                R.string.the_settings_have_been_changed_do_you_want_to_restart_the_app,
                                () -> AppUtils.restartApplication(context)
                        );
                    }
                    UiLifecycleGuard.dispatchBackPress(this, "ContainerDetailFragment", "save_existing_container");
                } else {
                    // Create new container with specified properties
                    JSONObject data = new JSONObject();
                    data.put("name", name);
                    data.put("screenSize", screenSize);
                    data.put("envVars", envVars);
                    data.put("cpuList", cpuList);
                    data.put("cpuListWoW64", cpuListWoW64);
                    data.put("graphicsDriver", Container.normalizeGraphicsDriver(graphicsDriver));
                    data.put("graphicsDriverConfig", graphicsDriverConfig);
                    data.put("dxwrapper", dxwrapper);
                    data.put("dxwrapperConfig", dxwrapperConfig);
                    data.put("audioDriver", audioDriver);
                    data.put("emulator", emulator);
                    data.put("wincomponents", wincomponents);
                    data.put("drives", drives);
                    data.put("showFPS", showFPS);
                    data.put("fullscreenStretched", fullscreenStretched);
                    data.put("inputType", finalInputType);
                    data.put("startupSelection", startupSelection);
                    data.put("box64Version", box64Version);
                    data.put("box64Preset", box64Preset);
                    data.put("fexcoreVersion", fexcoreVersion);
                    data.put("fexcorePreset", fexcorePreset);
                    data.put("desktopTheme", desktopTheme);
                    data.put("wineVersion", selectedWineVersion);
                    data.put("containerVariant", requestedRuntimeModel);
                    data.put("midiSoundFont", midiSoundFont);
                    data.put("lc_all", lc_all);
                    data.put("primaryController", primaryController);
                    data.put("controllerMapping", controllerMapping);
                    JSONObject extraData = new JSONObject();
                    extraData.put("upscalerPreset", containerUpscalerPreset);
                    extraData.put("upscalerFramegenMode", containerUpscalerFramegenMode);
                    extraData.put("upscalerFrameGeneration", containerUpscalerFramegen);
                    extraData.put("upscalerThermalGuard", containerUpscalerThermalGuard);
                    extraData.put("upscalerBackend", cbContainerFgEnable.isChecked() ? "mobfgsr" : JSONObject.NULL);
                    data.put("extraData", extraData);

                    preloaderDialog.show(R.string.creating_container);

                    manager.createContainerAsync(data, contentsManager, (container) -> {
                        if (!isAdded()) return;
                        preloaderDialog.close();
                        if (container == null) {
                            AppUtils.showToast(context, R.string.unable_to_create_container);
                            return;
                        }
                        this.container = container;
                        syncPendingWallpaperToContainerIfNeeded(desktopTheme, container);
                        saveWineRegistryKeys(view);
                        if (getActivity() instanceof MainActivity mainActivity) {
                            mainActivity.showContainersAfterContainerCreated(container);
                            return;
                        }
                        UiLifecycleGuard.dispatchBackPress(this, "ContainerDetailFragment", "create_container_async_complete");
                    });
                }
            } catch (JSONException e) {
                e.printStackTrace();
                AppUtils.showToast(context, R.string.unable_to_create_container);
            } catch (Exception e) {
                Log.e(TAG, "Container creation failed before async dispatch", e);
                AppUtils.showToast(context, R.string.unable_to_create_container);
            }
        });
        return view;
    }

    private void saveWineRegistryKeys(View view) {
        File userRegFile = new File(container.getRootDir(), ".wine/user.reg");
        if (!userRegFile.isFile()) return;
        try (WineRegistryEditor registryEditor = new WineRegistryEditor(userRegFile)) {
            Spinner sMouseWarpOverride = view.findViewById(R.id.SMouseWarpOverride);
            registryEditor.setStringValue(
                    "Software\\Wine\\DirectInput",
                    "MouseWarpOverride",
                    getSelectedText(sMouseWarpOverride, "disable").toLowerCase(Locale.ENGLISH)
            );
        } catch (Exception e) {
            Log.e(TAG, "Unable to persist Wine registry keys for container", e);
        }
    }

    private void syncPendingWallpaperToContainerIfNeeded(String desktopTheme, Container targetContainer) {
        if (!isAdded() || targetContainer == null || desktopTheme == null || desktopTheme.trim().isEmpty()) return;
        WineThemeManager.ThemeInfo themeInfo = new WineThemeManager.ThemeInfo(desktopTheme);
        if (themeInfo.backgroundType != WineThemeManager.BackgroundType.IMAGE) return;
        File sourceWallpaper = WineThemeManager.getUserWallpaperFile(ImageFs.find(requireContext()).getRootDir());
        if (!sourceWallpaper.isFile()) return;
        File targetWallpaper = WineThemeManager.getUserWallpaperFile(targetContainer.getRootDir());
        File parent = targetWallpaper.getParentFile();
        if (parent != null && !parent.isDirectory()) {
            parent.mkdirs();
        }
        FileUtils.copy(sourceWallpaper, targetWallpaper);
    }

    private void createWineConfigurationTab(View view) {
        Context context = getContext();

        WineThemeManager.ThemeInfo desktopTheme = new WineThemeManager.ThemeInfo(isEditMode() ? container.getDesktopTheme() : WineThemeManager.DEFAULT_DESKTOP_THEME);
        Spinner sDesktopTheme = view.findViewById(R.id.SDesktopTheme);
        sDesktopTheme.setSelection(desktopTheme.theme.ordinal());
        final ImagePickerView ipvDesktopBackgroundImage = view.findViewById(R.id.IPVDesktopBackgroundImage);
        File targetWallpaperFile = isEditMode() && container != null
                ? WineThemeManager.getUserWallpaperFile(container.getRootDir())
                : WineThemeManager.getUserWallpaperFile(ImageFs.find(requireContext()).getRootDir());
        ipvDesktopBackgroundImage.setTargetFile(targetWallpaperFile);
        final ColorPickerView cpvDesktopBackgroundColor = view.findViewById(R.id.CPVDesktopBackgroundColor);
        cpvDesktopBackgroundColor.setColor(desktopTheme.backgroundColor);

        Spinner sDesktopBackgroundType = view.findViewById(R.id.SDesktopBackgroundType);
        sDesktopBackgroundType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                WineThemeManager.BackgroundType type = WineThemeManager.BackgroundType.values()[position];
                ipvDesktopBackgroundImage.setVisibility(View.GONE);
                cpvDesktopBackgroundColor.setVisibility(View.GONE);

                if (type == WineThemeManager.BackgroundType.IMAGE) {
                    ipvDesktopBackgroundImage.setVisibility(View.VISIBLE);
                }
                else if (type == WineThemeManager.BackgroundType.COLOR) {
                    cpvDesktopBackgroundColor.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        sDesktopBackgroundType.setSelection(desktopTheme.backgroundType.ordinal());

        List<String> mouseWarpOverrideList = Arrays.asList(
                context.getString(R.string.disable),
                context.getString(R.string.enable),
                context.getString(R.string.force)
        );
        Spinner sMouseWarpOverride = view.findViewById(R.id.SMouseWarpOverride);
        sMouseWarpOverride.setAdapter(SpinnerAdapters.create(context, resolveDarkMode(context), mouseWarpOverrideList));

        String mouseWarpOverride = "disable";
        if (isEditMode()) {
            File userRegFile = new File(container.getRootDir(), ".wine/user.reg");
            try (WineRegistryEditor registryEditor = new WineRegistryEditor(userRegFile)) {
                mouseWarpOverride = registryEditor.getStringValue(
                        "Software\\Wine\\DirectInput",
                        "MouseWarpOverride",
                        "disable"
                );
            }
        }
        AppUtils.setSpinnerSelectionFromValue(sMouseWarpOverride, mouseWarpOverride);
    }

    private void loadGPUNameSpinner(Spinner spinner, int selectedDeviceID) {
        List<String> values = new ArrayList<>();
        int selectedPosition = 0;

        try {
            for (int i = 0; i < gpuCards.length(); i++) {
                JSONObject item = gpuCards.getJSONObject(i);
                if (item.getInt("deviceID") == selectedDeviceID) selectedPosition = i;
                values.add(item.getString("name"));
            }
        }
        catch (JSONException e) {
            Log.w("ContainerDetailFragment", "Failed to populate GPU card spinner", e);
        }

        spinner.setAdapter(SpinnerAdapters.create(getContext(), resolveDarkMode(getContext()), values));
        spinner.setSelection(selectedPosition);
    }

    public static String getScreenSize(View view) {
        Spinner sScreenSize = view.findViewById(R.id.SScreenSize);
        String value = getSelectedText(sScreenSize, Container.DEFAULT_SCREEN_SIZE);
        if (value.equalsIgnoreCase("custom")) {
            value = Container.DEFAULT_SCREEN_SIZE;
            String strWidth = ((EditText)view.findViewById(R.id.ETScreenWidth)).getText().toString().trim();
            String strHeight = ((EditText)view.findViewById(R.id.ETScreenHeight)).getText().toString().trim();
            if (strWidth.matches("[0-9]+") && strHeight.matches("[0-9]+")) {
                int width = Integer.parseInt(strWidth);
                int height = Integer.parseInt(strHeight);
                if ((width % 2) == 0 && (height % 2) == 0) return width+"x"+height;
            }
        }
        return StringUtils.parseIdentifier(value);
    }

    private String getDesktopTheme(View view) {
        Spinner sDesktopBackgroundType = view.findViewById(R.id.SDesktopBackgroundType);
        WineThemeManager.BackgroundType type = WineThemeManager.BackgroundType.values()[sDesktopBackgroundType.getSelectedItemPosition()];
        Spinner sDesktopTheme = view.findViewById(R.id.SDesktopTheme);
        ColorPickerView cpvDesktopBackground = view.findViewById(R.id.CPVDesktopBackgroundColor);
        WineThemeManager.Theme theme = WineThemeManager.Theme.values()[sDesktopTheme.getSelectedItemPosition()];

        String desktopTheme = theme+","+type+","+cpvDesktopBackground.getColorAsString();
        if (type == WineThemeManager.BackgroundType.IMAGE) {
            File userWallpaperFile = isEditMode() && container != null
                    ? WineThemeManager.getUserWallpaperFile(container.getRootDir())
                    : WineThemeManager.getUserWallpaperFile(ImageFs.find(requireContext()).getRootDir());
            desktopTheme += ","+(userWallpaperFile.isFile() ? userWallpaperFile.lastModified() : "0");
        }
        return desktopTheme;
    }

    public static void loadScreenSizeSpinner(View view, String selectedValue) {
        final Spinner sScreenSize = view.findViewById(R.id.SScreenSize);

        final LinearLayout llCustomScreenSize = view.findViewById(R.id.LLCustomScreenSize);
        sScreenSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String value = sScreenSize.getItemAtPosition(position).toString();
                llCustomScreenSize.setVisibility(value.equalsIgnoreCase("custom") ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        boolean found = AppUtils.setSpinnerSelectionFromIdentifier(sScreenSize, selectedValue);
        if (!found) {
            AppUtils.setSpinnerSelectionFromValue(sScreenSize, "custom");
            String[] screenSize = selectedValue.split("x");
            ((EditText) view.findViewById(R.id.ETScreenWidth)).setText(screenSize[0]);
            ((EditText) view.findViewById(R.id.ETScreenHeight)).setText(screenSize[1]);
        }
    }

    public void loadGraphicsDriverSpinner(final Spinner sGraphicsDriver, final Spinner sDXWrapper, final View vGraphicsDriverConfig, String selectedGraphicsDriver, String selectedDXWrapper) {
        final Context context = sGraphicsDriver.getContext();
        final String initialGraphicsDriver = GraphicsDrivers.getTopLevelSelectableDriver(selectedGraphicsDriver);
        Object initialGraphicsDriverConfigTag = vGraphicsDriverConfig.getTag();
        String initialGraphicsDriverConfig = initialGraphicsDriverConfigTag instanceof String
                ? (String) initialGraphicsDriverConfigTag
                : GraphicsDrivers.defaultConfig(selectedGraphicsDriver);
        vGraphicsDriverConfig.setTag(
                GraphicsDrivers.migrateToUnifiedTopLevelConfig(selectedGraphicsDriver, initialGraphicsDriverConfig)
        );

        // Update the spinner with the available graphics driver options
        updateGraphicsDriverSpinner(context, sGraphicsDriver);

        Runnable update = () -> {
            String graphicsDriver = Container.normalizeGraphicsDriver(getSelectedIdentifier(sGraphicsDriver, Container.DEFAULT_GRAPHICS_DRIVER));
            Object graphicsDriverConfigTag = vGraphicsDriverConfig.getTag();
            String graphicsDriverConfig = graphicsDriverConfigTag instanceof String
                    ? (String) graphicsDriverConfigTag
                    : GraphicsDrivers.defaultConfig(graphicsDriver);
            graphicsDriverConfig = GraphicsDrivers.sanitizeConfigShape(graphicsDriver, graphicsDriverConfig);
            vGraphicsDriverConfig.setTag(graphicsDriverConfig);

            // Update the DXWrapper spinner
            ArrayList<String> items = new ArrayList<>();
            for (String value : context.getResources().getStringArray(R.array.dxwrapper_entries)) {
                items.add(value);
            }
            sDXWrapper.setAdapter(SpinnerAdapters.create(context, resolveDarkMode(context), items));
            sDXWrapper.setPopupBackgroundResource(resolveDarkMode(context) ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
            AppUtils.setSpinnerSelectionFromIdentifier(sDXWrapper, selectedDXWrapper);

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
                        new GraphicsDriverConfigDialog(vGraphicsDriverConfig, graphicsDriver, null).show();
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "Failed to open graphics driver config dialog", t);
                    ForensicLogger.error(
                            context,
                            "GRAPHICS_DRIVER_CONFIG_OPEN_FAIL",
                            null,
                            "graphics_config",
                            "graphics_driver_config_open_failed",
                            t,
                            ForensicLogger.fields(
                                    "graphics_driver", graphicsDriver,
                                    "config_tag", String.valueOf(vGraphicsDriverConfig.getTag())
                            )
                    );
                    AppUtils.showToast(context, R.string.unable_to_open_graphics_driver_configuration);
                }
            });
            vGraphicsDriverConfig.setVisibility(View.VISIBLE);
        };

        sGraphicsDriver.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                update.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Set the spinner's initial selection
        AppUtils.setSpinnerSelectionFromIdentifier(sGraphicsDriver, initialGraphicsDriver);
        update.run();
    }

    public static void setupDXWrapperSpinner(final Spinner sDXWrapper, final View vDXWrapperConfig, boolean isARM64EC) {
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String dxwrapper = getSelectedIdentifier(sDXWrapper, Container.DEFAULT_DXWRAPPER);
                if (dxwrapper.contains("dxvk")) {
                    vDXWrapperConfig.setOnClickListener((v) -> (new DXVKConfigDialog(vDXWrapperConfig, isARM64EC)).show());
                } else if (dxwrapper.contains("dgvoodoo")) {
                    vDXWrapperConfig.setOnClickListener((v) -> (new DgVoodooConfigDialog(vDXWrapperConfig)).show());
                } else {
                    vDXWrapperConfig.setOnClickListener((v) -> (new WineD3DConfigDialog(vDXWrapperConfig)).show());
                }
                vDXWrapperConfig.setVisibility(View.VISIBLE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        sDXWrapper.setOnItemSelectedListener(listener);

        int selectedPosition = sDXWrapper.getSelectedItemPosition();
        if (selectedPosition >= 0) {
            listener.onItemSelected(
                    sDXWrapper,
                    sDXWrapper.getSelectedView(),
                    selectedPosition,
                    sDXWrapper.getSelectedItemId()
            );
        }
    }

    public static String getWinComponents(View view) {
        ViewGroup parent = view.findViewById(R.id.LLTabWinComponents);
        ArrayList<View> views = new ArrayList<>();
        AppUtils.findViewsWithClass(parent, Spinner.class, views);
        String[] wincomponents = new String[views.size()];

        for (int i = 0; i < views.size(); i++) {
            Spinner spinner = (Spinner)views.get(i);
            wincomponents[i] = spinner.getTag()+"="+spinner.getSelectedItemPosition();
        }
        return String.join(",", wincomponents);
    }

    public static void createWinComponentsTab(View view, String wincomponents) {
        Context context = view.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        ViewGroup tabView = view.findViewById(R.id.LLTabWinComponents);
        ViewGroup directxSectionView = tabView.findViewById(R.id.LLWinComponentsDirectX);
        ViewGroup generalSectionView = tabView.findViewById(R.id.LLWinComponentsGeneral);

        for (String[] wincomponent : new KeyValueSet(wincomponents)) {
            ViewGroup parent = wincomponent[0].startsWith("direct") ? directxSectionView : generalSectionView;
            View itemView = inflater.inflate(R.layout.wincomponent_list_item, parent, false);
            ((TextView)itemView.findViewById(R.id.TextView)).setText(StringUtils.getString(context, wincomponent[0]));
            Spinner spinner = itemView.findViewById(R.id.Spinner);
            spinner.setSelection(Integer.parseInt(wincomponent[1]), false);
            spinner.setTag(wincomponent[0]);

            // Set the background color of the spinners dynamically based on the current theme
            spinner.setPopupBackgroundResource(resolveDarkMode(context)
                    ? R.drawable.surface_dialog_background_dark
                    : R.drawable.surface_dialog_background);

            parent.addView(itemView);

        }
    }

    public static void createWinComponentsTabFromShortcut(ShortcutSettingsDialog dialog, View view, String wincomponents, boolean isDarkMode) {
        Context context = dialog.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        ViewGroup tabView = view.findViewById(R.id.LLTabWinComponents);
        ViewGroup directxSectionView = tabView.findViewById(R.id.LLWinComponentsDirectX);
        ViewGroup generalSectionView = tabView.findViewById(R.id.LLWinComponentsGeneral);

        for (String[] wincomponent : new KeyValueSet(wincomponents)) {
            ViewGroup parent = wincomponent[0].startsWith("direct") ? directxSectionView : generalSectionView;
            View itemView = inflater.inflate(R.layout.wincomponent_list_item, parent, false);
            ((TextView) itemView.findViewById(R.id.TextView)).setText(StringUtils.getString(context, wincomponent[0]));
            Spinner spinner = itemView.findViewById(R.id.Spinner);
            spinner.setSelection(Integer.parseInt(wincomponent[1]), false);
            spinner.setTag(wincomponent[0]);

            // Set the background color of the spinners dynamically based on the current theme
            spinner.setPopupBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);

            parent.addView(itemView);
        }

        // Notify that the views are ready
        dialog.onWinComponentsViewsAdded(isDarkMode);
    }

    private EnvVarsView createEnvVarsTab(final View view) {
        final Context context = view.getContext();
        final EnvVarsView envVarsView = view.findViewById(R.id.EnvVarsView);

        // Apply dark mode setting to the existing instance
        envVarsView.setDarkMode(resolveDarkMode(context));

        envVarsView.setEnvVars(new EnvVars(isEditMode() ? container.getEnvVars() : Container.DEFAULT_ENV_VARS));
        view.findViewById(R.id.BTAddEnvVar).setOnClickListener((v) -> (new AddEnvVarDialog(context, envVarsView)).show());
        return envVarsView;
    }

    private String getDrives(View view) {
        LinearLayout parent = view.findViewById(R.id.LLDrives);
        String drives = "";

        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            Spinner spinner = child.findViewById(R.id.Spinner);
            EditText editText = child.findViewById(R.id.EditText);
            String path = editText.getText().toString().trim();
            if (!path.isEmpty()) drives += spinner.getSelectedItem()+path;
        }
        return drives;
    }

    private void createDrivesTab(View view) {
        final Context context = getContext();

        final LinearLayout parent = view.findViewById(R.id.LLDrives);
        final View emptyTextView = view.findViewById(R.id.TVDrivesEmptyText);
        LayoutInflater inflater = LayoutInflater.from(context);
        final String drives = isEditMode() ? container.getDrives() : Container.DEFAULT_DRIVES;
        final String[] driveLetters = new String[Container.MAX_DRIVE_LETTERS];
        for (int i = 0; i < driveLetters.length; i++) driveLetters[i] = ((char)(i + 68))+":";

        Callback<String[]> addItem = (drive) -> {
            final View itemView = inflater.inflate(R.layout.drive_list_item, parent, false);
            int accent = ContextCompat.getColor(context, resolveDarkMode(context) ? R.color.colorAccentDark : R.color.colorAccent);
            itemView.setBackground(buildInlineCardBackground(accent, resolveDarkMode(context)));
            Spinner spinner = itemView.findViewById(R.id.Spinner);
            spinner.setAdapter(SpinnerAdapters.create(context, resolveDarkMode(context), driveLetters));
            AppUtils.setSpinnerSelectionFromValue(spinner, drive[0]+":");

            // Apply dark theme to the spinner popup background
            spinner.setPopupBackgroundResource(resolveDarkMode(context)
                    ? R.drawable.surface_dialog_background_dark
                    : R.drawable.surface_dialog_background);

            final EditText editText = itemView.findViewById(R.id.EditText);
            editText.setText(drive[1]);

            // Apply dark theme to EditText if necessary
            applyDarkThemeToEditText(editText);

            // Apply dark theme to the search button if necessary
            View btSearch = itemView.findViewById(R.id.BTSearch);
            applyDarkThemeToButton(btSearch);

            itemView.findViewById(R.id.BTSearch).setOnClickListener((v) -> {
                openDirectoryCallback = (path) -> {
                    drive[1] = path;
                    editText.setText(path);
                };
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.fromFile(Environment.getExternalStorageDirectory()));
                getActivity().startActivityFromFragment(this, intent, MainActivity.OPEN_DIRECTORY_REQUEST_CODE);
            });

            itemView.findViewById(R.id.BTRemove).setOnClickListener((v) -> {
                parent.removeView(itemView);
                if (parent.getChildCount() == 0) emptyTextView.setVisibility(View.VISIBLE);
            });
            parent.addView(itemView);

            // Hide empty text view if there are items
            emptyTextView.setVisibility(View.GONE);
        };
        for (String[] drive : Container.drivesIterator(drives)) addItem.call(drive);

        view.findViewById(R.id.BTAddDrive).setOnClickListener((v) -> {
            if (parent.getChildCount() >= Container.MAX_DRIVE_LETTERS) return;
            final String nextDriveLetter = String.valueOf(driveLetters[parent.getChildCount()].charAt(0));
            addItem.call(new String[]{nextDriveLetter, ""});
        });

        if (drives.isEmpty()) emptyTextView.setVisibility(View.VISIBLE);
    }

    private GradientDrawable buildInlineCardBackground(int accent, boolean darkMode) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(UnitUtils.dpToPx(14));
        background.setColor((accent & 0x00ffffff) | ((darkMode ? 50 : 20) << 24));
        background.setStroke(Math.round(UnitUtils.dpToPx(1)), (accent & 0x00ffffff) | ((darkMode ? 220 : 130) << 24));
        return background;
    }

    // Helper method to apply dark theme to EditText
    private static void applyDarkThemeToEditText(EditText editText) {
        boolean darkMode = resolveDarkMode(editText.getContext());
        int textColor = ContextCompat.getColor(
                editText.getContext(),
                darkMode ? R.color.surface_badge_text_dark : R.color.surface_badge_text
        );
        int hintColor = ContextCompat.getColor(
                editText.getContext(),
                darkMode ? R.color.surface_body_text_dark : R.color.surface_body_text
        );
        editText.setTextColor(textColor);
        editText.setHintTextColor(hintColor);
        editText.setBackgroundResource(darkMode ? R.drawable.edit_text_dark : R.drawable.edit_text);
    }

    // Helper method to apply dark theme to buttons or other clickable views
    private void applyDarkThemeToButton(View button) {

    }

    private void loadWineVersionSpinner(final View view, Spinner sWineVersion, Spinner sBox64Version) {
        final Context context = getContext();

        sWineVersion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                FrameLayout fexcoreFL = view.findViewById(R.id.fexcoreFrame);
                Spinner sEmulator = view.findViewById(R.id.SEmulator);
                Spinner sEmulator64 = view.findViewById(R.id.SEmulator64);
                Spinner sDXWrapper = view.findViewById(R.id.SDXWrapper);
                View vDXWrapperConfig = view.findViewById(R.id.BTDXWrapperConfig);
                sEmulator64.setEnabled(false);

                String wineVersion = getSelectedText(sWineVersion, "");
                if (AppUtils.isMissingComponentValue(wineVersion)) {
                    fexcoreFL.setVisibility(View.GONE);
                    sEmulator.setEnabled(false);
                    AppUtils.setSpinnerSelectionFromIdentifier(sEmulator, "box64");
                    sEmulator64.setSelection(1, false);
                    loadBox64VersionSpinner(context, container, contentsManager, sBox64Version, false);
                    setupDXWrapperSpinner(sDXWrapper, vDXWrapperConfig, false);
                    updateRuntimeSelectionUi(view);
                    return;
                }

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
                    if (!isEditMode()) AppUtils.setSpinnerSelectionFromIdentifier(sEmulator, "fexcore");
                }
                else {
                    fexcoreFL.setVisibility(View.GONE);
                    sEmulator.setEnabled(false);
                    AppUtils.setSpinnerSelectionFromIdentifier(sEmulator, "box64");
                    sEmulator64.setSelection(1);
                }
                loadBox64VersionSpinner(context, container, contentsManager, sBox64Version, wineInfo.isArm64EC());
                setupDXWrapperSpinner(sDXWrapper, vDXWrapperConfig, wineInfo.isArm64EC());
                updateRuntimeSelectionUi(view);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        view.findViewById(R.id.LLWineVersion).setVisibility(View.VISIBLE);
        LinkedHashSet<String> wineVersionSet = new LinkedHashSet<>();
        int embeddedCount = 0;
        for (String version : getResources().getStringArray(R.array.wine_entries)) {
            if (hasEmbeddedWineVersion(context, version)) {
                wineVersionSet.add(version);
                embeddedCount++;
            }
        }
        wineVersionSet.addAll(contentsManager.getInstalledRuntimeEntries(
                null,
                true,
                ContentProfile.ContentType.CONTENT_TYPE_WINE,
                ContentProfile.ContentType.CONTENT_TYPE_PROTON
        ));
        int localWineCount = contentsManager.countInstalledProfiles(ContentProfile.ContentType.CONTENT_TYPE_WINE, true);
        int localProtonCount = contentsManager.countInstalledProfiles(ContentProfile.ContentType.CONTENT_TYPE_PROTON, true);
        ArrayList<String> wineVersions = new ArrayList<>(wineVersionSet);
        boolean hasWineVersion = !wineVersions.isEmpty();
        if (!hasWineVersion) {
            wineVersions.add(AppUtils.MISSING_COMPONENT_PLACEHOLDER);
        }

        ForensicLogger.logEvent(
                context,
                "info",
                "NEW_CONTAINER_RUNTIME_SCAN",
                null,
                "containers",
                "runtime_scan",
                ForensicLogger.fields(
                        "embedded_runtime_count", embeddedCount,
                        "local_wine_count", localWineCount,
                        "local_proton_count", localProtonCount,
                        "total_entries", wineVersions.size(),
                        "runtime_available", hasWineVersion
                )
        );

        sWineVersion.setAdapter(SpinnerAdapters.create(context, resolveDarkMode(context), wineVersions));
        sWineVersion.setEnabled(hasWineVersion);

        if (isEditMode() && hasWineVersion) {
            AppUtils.setSpinnerSelectionFromValue(
                    sWineVersion,
                    contentsManager.resolveBestRuntimeEntry(container.getWineVersion(), container.getContainerVariant())
            );
        }

        updateRuntimeSelectionUi(view);
    }

    public String getControllerMapping(View view) {
        //The order has to be the same like Container.XrControllerMapping
        int[] ids = {
                R.id.SButtonA, R.id.SButtonB, R.id.SButtonX, R.id.SButtonY, R.id.SButtonGrip, R.id.SButtonTrigger,
                R.id.SThumbstickUp, R.id.SThumbstickDown, R.id.SThumbstickLeft, R.id.SThumbstickRight
        };
        byte[] controllerMapping = new byte[ids.length];
        for (int i = 0; i < ids.length; i++) {
            int index =  ((Spinner)view.findViewById(ids[i])).getSelectedItemPosition();
            byte value = XKeycode.values()[index].id;
            controllerMapping[i] = value;
        }
        return new String(controllerMapping);
    }

    public void setControllerMapping(Spinner spinner, Container.XrControllerMapping mapping, int defaultValue) {
        XKeycode[] values = XKeycode.values();
        ArrayList<String> array = new ArrayList<>();
        for (XKeycode value : values) {
            array.add(value.name());
        }
        spinner.setAdapter(SpinnerAdapters.create(spinner.getContext(), resolveDarkMode(spinner.getContext()), array));

        byte keycode = isEditMode() ? container.getControllerMapping(mapping) : (byte) defaultValue;
        int index = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].id == keycode) {
                index = i;
                break;
            }
        }
        spinner.setSelection(isEditMode() && (index != 0) ? index : defaultValue);
    }

    public static void updateGraphicsDriverSpinner(Context context, Spinner spinner) {
        List<String> itemList = GraphicsDrivers.getSelectableEntries(context);
        spinner.setAdapter(SpinnerAdapters.create(context, resolveDarkMode(context), itemList));
    }

    public static void loadBox64VersionSpinner(Context context, Container container, ContentsManager manager, Spinner spinner, boolean isArm64EC) {
        List<String> itemList = new ArrayList<>();
        String[] embeddedEntries = context.getResources().getStringArray(
                isArm64EC ? R.array.wowbox64_version_entries : R.array.box64_version_entries
        );
        for (String version : embeddedEntries) {
            if (hasEmbeddedEmulatorArchive(context, version, isArm64EC)) {
                itemList.add(version);
            }
        }

        List<ContentProfile> profiles = manager.getProfiles(
                isArm64EC ? ContentProfile.ContentType.CONTENT_TYPE_WOWBOX64 : ContentProfile.ContentType.CONTENT_TYPE_BOX64
        );
        appendInstalledContentVersions(itemList, manager, profiles);

        boolean hasVersions = !itemList.isEmpty();
        if (!hasVersions) {
            itemList.add(AppUtils.MISSING_COMPONENT_PLACEHOLDER);
        }

        spinner.setAdapter(SpinnerAdapters.create(context, resolveDarkMode(context), itemList));
        spinner.setEnabled(hasVersions);

        if (!hasVersions) return;

        if (container != null && AppUtils.setSpinnerSelectionFromValue(spinner, container.getBox64Version())) {
            return;
        }

        String defaultValue = isArm64EC ? DefaultVersion.WOWBOX64 : DefaultVersion.BOX64;
        AppUtils.setSpinnerSelectionFromValue(spinner, defaultValue);
    }

    private static boolean hasEmbeddedWineVersion(Context context, String version) {
        try {
            ImageFs imageFs = ImageFs.find(context);
            if (imageFs == null) return false;
            File wineDir = WineInfo.isMainWineVersion(version)
                    ? imageFs.getMainWineDir()
                    : WineUtils.resolveCanonicalRuntimeRoot(new File(imageFs.getRootDir(), "opt/" + version));
            if (!wineDir.isDirectory()) return false;
            return WineUtils.hasRuntimeCorePayload(wineDir);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean hasEmbeddedArchive(Context context, String assetPath) {
        return FileUtils.getSize(context, assetPath) > 0;
    }

    private static boolean hasEmbeddedEmulatorArchive(Context context, String version, boolean isArm64EC) {
        if (version == null || version.trim().isEmpty()) return false;
        String normalizedVersion = version.trim();
        if (isArm64EC) {
            return hasEmbeddedArchive(context, "wowbox64/wowbox64-" + normalizedVersion + ".tzst");
        }
        return hasEmbeddedArchive(context, "box86_64/box64-" + normalizedVersion + "-bionic.tzst")
                || hasEmbeddedArchive(context, "box86_64/box64-" + normalizedVersion + ".tzst")
                || hasEmbeddedArchive(context, "box64/box64-" + normalizedVersion + ".tzst");
    }

    private static void appendInstalledContentVersions(List<String> targetList, ContentsManager manager, List<ContentProfile> profiles) {
        if (profiles == null) return;
        for (ContentProfile profile : profiles) {
            if (profile == null || manager == null || !manager.isInstalledProfileUsable(profile)) continue;
            if (profile.verName == null || profile.verName.trim().isEmpty()) continue;
            if (!targetList.contains(profile.verName)) {
                targetList.add(profile.verName);
            }
        }
    }

}
