package com.winlator.cmod.contentdialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.winlator.cmod.R;
import com.winlator.cmod.contents.AdrenotoolsManager;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.DefaultVersion;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.core.GPUInformation;
import com.winlator.cmod.core.SpinnerAdapters;
import com.winlator.cmod.core.StringUtils;
import com.winlator.cmod.widget.MultiSelectionComboBox;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GraphicsDriverConfigDialog extends ContentDialog {

    private static final String TAG = "GraphicsDriverConfigDialog"; // Tag for logging
    private static final Pattern VULKAN_API_PATTERN = Pattern.compile("1\\.(\\d+)");
    private Spinner sVersion;
    private Spinner sVulkanVersion;
    private MultiSelectionComboBox mscAvailableExtensions;
    private Spinner sGPUName;
    private Spinner sMaxDeviceMemory;
    private Spinner sPresentMode;
    private Spinner sResourceType;
    private Spinner sBCnEmulation;
    private Spinner sBCnEmulationType;
    private Spinner sBCnEmulationCache;
    private CheckBox cbSyncFrame;
    private CheckBox cbDisablePresentWait;

    private static String selectedVulkanVersion;
    private static String selectedVersion;
    private static String blacklistedExtensions = "";
    private static String selectedGPUName;
    private static String selectedDeviceMemory;

    private static String isSyncFrame;
    private static String isDisablePresentWait;
    private static String selectedPresentMode;
    private static String selectedResourceType;
    private static String selectedBCnEmulation;
    private static String selectedBCnEmulationType;
    private static String isBCnCacheEnabled;

    private void loadGPUNameSpinner(Context context, Spinner spinner)  {
        String gpuNameList = FileUtils.readString(context, "gpu_cards.json");
        ArrayList<String> entries = new ArrayList<>();

        entries.add("Device");

        try {
            JSONArray jarray = new JSONArray(gpuNameList);
            for (int i = 0; i < jarray.length(); i++) {
                JSONObject jobj = jarray.getJSONObject(i);
                String gpuName = jobj.getString("name");
                entries.add(gpuName);
            }
            spinner.setAdapter(SpinnerAdapters.create(context, isDarkMode(context), entries));
        }
        catch (JSONException e) {
        }
    }

    public static HashMap<String, String> parseGraphicsDriverConfig(String graphicsDriverConfig) {
        HashMap<String, String> mappedConfig = new HashMap<>();
        String[] configElements = graphicsDriverConfig.split(";");
        for (String element : configElements) {
            String key;
            String value;
            String[] splittedElement = element.split("=");
            key = splittedElement[0];
            if (splittedElement.length > 1)
                value = element.split("=")[1];
            else
                value = "";
            mappedConfig.put(key, value);
        }
        return mappedConfig;
    }

    public static String toGraphicsDriverConfig(HashMap<String, String> config) {
        String graphicsDriverConfig = "";
        for (Map.Entry<String, String> entry : config.entrySet()) {
            graphicsDriverConfig += entry.getKey() + "=" + entry.getValue() + ";";
        }
        return graphicsDriverConfig.substring(0, graphicsDriverConfig.length() - 1);
    }

    public static String getVersion(String graphicsDriverConfig) {
        HashMap<String, String> config = parseGraphicsDriverConfig(graphicsDriverConfig);
        return config.get("version");
    }

    public static String getExtensionsBlacklist(String graphicsDriverConfig) {
        HashMap<String, String> config = parseGraphicsDriverConfig(graphicsDriverConfig);
        return config.get("blacklistedExtensions");
    }

    public static String writeGraphicsDriverConfig() {
        String graphicsDriverConfig = "vulkanVersion=" + selectedVulkanVersion + ";" +
                "version=" + selectedVersion + ";" +
                "blacklistedExtensions=" + blacklistedExtensions + ";" +
                "maxDeviceMemory=" + StringUtils.parseNumber(selectedDeviceMemory) + ";" +
                "presentMode=" + selectedPresentMode + ";" +
                "syncFrame=" + isSyncFrame + ";" +
                "disablePresentWait=" + isDisablePresentWait + ";" +
                "resourceType=" + selectedResourceType + ";" +
                "bcnEmulation=" + selectedBCnEmulation + ";" +
                "bcnEmulationType=" + selectedBCnEmulationType + ";" +
                "bcnEmulationCache=" + isBCnCacheEnabled + ";" +
                "gpuName=" + selectedGPUName;
        Log.i(TAG, "Written config " + graphicsDriverConfig);
        return graphicsDriverConfig;
    }

    private String[] queryAvailableExtensions(String driver, Context context) {
        return GPUInformation.enumerateExtensions(driver, context);
    }

    public GraphicsDriverConfigDialog(View anchor, String graphicsDriver, TextView graphicsDriverVersionView) {
        super(anchor.getContext(), R.layout.graphics_driver_config_dialog);
        initializeDialog(anchor, graphicsDriver, graphicsDriverVersionView);
    }

    private void initializeDialog(View anchor, String graphicsDriver, TextView graphicsDriverVersionView) {
        setIcon(R.drawable.ae_icon_settings);
        setTitle(anchor.getContext().getString(R.string.graphics_driver_configuration));

        String graphicsDriverConfig = anchor.getTag().toString();

        sVersion = findViewById(R.id.SGraphicsDriverVersion);
        sVulkanVersion = findViewById(R.id.SGraphicsDriverVulkanVersion);
        mscAvailableExtensions = findViewById(R.id.MSCAvailableExtensions);
        sPresentMode = findViewById(R.id.SGraphicsDriverPresentMode);
        sGPUName = findViewById(R.id.SGraphicsDriverGPUName);
        sMaxDeviceMemory = findViewById(R.id.SGraphicsDriverMaxDeviceMemory);
        sResourceType = findViewById(R.id.SGraphicsDriverResourceType);
        sBCnEmulation = findViewById(R.id.SGraphicsDriverBCnEmulation);
        sBCnEmulationType = findViewById(R.id.SGraphicsDriverBCnEmulationType);
        sBCnEmulationCache = findViewById(R.id.SGraphicsDriverBCnEmulationCache);
        cbSyncFrame = findViewById(R.id.CBSyncFrame);
        cbDisablePresentWait = findViewById(R.id.CBDisablePresentWait);
        applyPopupTheme(anchor.getContext());

        HashMap<String, String> config = parseGraphicsDriverConfig(graphicsDriverConfig);

        String vulkanVersion = config.get("vulkanVersion");
        String initialVersion = config.get("version");
        String blExtensions = config.get("blacklistedExtensions");
        String gpuName = config.get("gpuName");
        String maxDeviceMemory = config.get("maxDeviceMemory");
        String syncFrame = config.get("syncFrame");
        String disablePresentWait = config.get("disablePresentWait");
        String presentMode = config.get("presentMode");
        String resourceType = config.get("resourceType");
        String bcnEmulation = config.get("bcnEmulation");
        String bcnEmulationType = config.get("bcnEmulationType");
        String bcnEmulationCache = config.get("bcnEmulationCache");

        // Update the selectedVersion whenever the user selects a different version
        sVersion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedVersion = sVersion.getSelectedItem().toString();
                if (AppUtils.isMissingComponentValue(selectedVersion)) {
                    mscAvailableExtensions.setItems(new String[0], "Extensions");
                    return;
                }
                String[] availableExtensions = queryAvailableExtensions(selectedVersion, anchor.getContext());
                String blacklistedExtensions = "";

                mscAvailableExtensions.setItems(availableExtensions, "Extensions");
                mscAvailableExtensions.setSelectedItems(availableExtensions);

                if(selectedVersion.equals(initialVersion))
                    blacklistedExtensions = blExtensions;

                String[] bl = blacklistedExtensions.split("\\,");

                for (String extension : bl) {
                    mscAvailableExtensions.unsetSelectedItem(extension);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedVersion = sVersion.getSelectedItem().toString();
                Log.d(TAG, "User selected version: " + selectedVersion);
            }
        });

        sVulkanVersion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (sVulkanVersion.isEnabled()) {
                    selectedVulkanVersion = sVulkanVersion.getSelectedItem().toString();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        sGPUName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedGPUName = sGPUName.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        sMaxDeviceMemory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDeviceMemory = sMaxDeviceMemory.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        sPresentMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPresentMode = sPresentMode.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        sResourceType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedResourceType = sResourceType.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        sBCnEmulation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedBCnEmulation = sBCnEmulation.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        sBCnEmulationType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedBCnEmulationType = sBCnEmulationType.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        sBCnEmulationCache.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                isBCnCacheEnabled = sBCnEmulationCache.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        isSyncFrame = syncFrame;
        cbSyncFrame.setChecked(isSyncFrame.equals("1") ? true : false);
        cbSyncFrame.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isSyncFrame = isChecked ? "1" : "0";
        });

        isDisablePresentWait = disablePresentWait;
        cbDisablePresentWait.setChecked(isDisablePresentWait.equals("1") ? true : false);
        cbDisablePresentWait.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isDisablePresentWait = isChecked ? "1" : "0";
        });

        // Ensure ContentsManager syncContents is called
        ContentsManager contentsManager = new ContentsManager(anchor.getContext());
        contentsManager.syncContents();

        // Populate the spinner with available versions from ContentsManager and pre-select the initial version
        populateGraphicsDriverVersions(anchor.getContext(), contentsManager, vulkanVersion, initialVersion, blExtensions, gpuName, maxDeviceMemory, presentMode, resourceType, bcnEmulation, bcnEmulationType, bcnEmulationCache, graphicsDriver);

        setOnConfirmCallback(() -> {
            if (AppUtils.isMissingComponentValue(selectedVersion)) {
                selectedVersion = initialVersion != null ? initialVersion : "";
            }
            blacklistedExtensions = mscAvailableExtensions.getUnSelectedItemsAsString();

            if (graphicsDriverVersionView != null)
                graphicsDriverVersionView.setText(selectedVersion);

            anchor.setTag(writeGraphicsDriverConfig());
            ForensicLogger.logEvent(
                    anchor.getContext(),
                    "info",
                    "GRAPHICS_DRIVER_CONFIG_SAVED",
                    null,
                    "graphics_config",
                    "graphics_driver_config_saved",
                    ForensicLogger.fields(
                            "graphics_driver", graphicsDriver,
                            "driver_version", selectedVersion,
                            "vulkan_api", selectedVulkanVersion,
                            "gpu_name", selectedGPUName,
                            "max_device_memory", selectedDeviceMemory,
                            "present_mode", selectedPresentMode,
                            "resource_type", selectedResourceType,
                            "bcn_emulation", selectedBCnEmulation,
                            "bcn_emulation_type", selectedBCnEmulationType,
                            "bcn_cache", isBCnCacheEnabled,
                            "sync_frame", isSyncFrame,
                            "disable_present_wait", isDisablePresentWait
                    )
            );
        });
    }

    private void populateGraphicsDriverVersions(Context context, ContentsManager contentsManager, String vulkanVersion, @Nullable String initialVersion, @Nullable String blExtensions, String gpuName, String maxDeviceMemory, String presentMode, String selectedResourceType, String bcnEmulation, String bcnEmulationType, String bcnEmulationCache, String graphicsDriver) {
        List<String> wrapperVersions = new ArrayList<>();
        String[] wrapperDefaultVersions = context.getResources().getStringArray(R.array.wrapper_graphics_driver_version_entries);

        for (String version : wrapperDefaultVersions) {
            if (GPUInformation.isDriverSupported(version, context))
                wrapperVersions.add(version);
        }

        // Add installed versions from AdrenotoolsManager
        AdrenotoolsManager adrenotoolsManager = new AdrenotoolsManager(context);
        wrapperVersions.addAll(adrenotoolsManager.enumarateInstalledDrivers());

        // Set the adapter and select the initial version
        boolean hasVersions = !wrapperVersions.isEmpty();
        if (!hasVersions) {
            wrapperVersions.add(AppUtils.MISSING_COMPONENT_PLACEHOLDER);
        }
        sVersion.setAdapter(SpinnerAdapters.create(context, isDarkMode(context), wrapperVersions));
        sVersion.setEnabled(hasVersions);
        mscAvailableExtensions.setEnabled(hasVersions);

        // We can start logging selected graphics driver and initial version
        Log.d(TAG, "Graphics driver: " + graphicsDriver);
        Log.d(TAG, "Initial version: " + initialVersion);

        loadGPUNameSpinner(context, sGPUName);
        populateVulkanVersionSpinner(context, contentsManager, vulkanVersion);

        // Use the custom selection logic
        if (hasVersions) {
            setSpinnerSelectionWithFallback(sVersion, initialVersion, graphicsDriver);
        } else {
            selectedVersion = AppUtils.MISSING_COMPONENT_PLACEHOLDER;
            sVersion.setSelection(0, false);
        }
        AppUtils.setSpinnerSelectionFromValue(sGPUName, gpuName);
        AppUtils.setSpinnerSelectionFromNumber(sMaxDeviceMemory, maxDeviceMemory);
        AppUtils.setSpinnerSelectionFromValue(sPresentMode, presentMode);
        AppUtils.setSpinnerSelectionFromValue(sResourceType, selectedResourceType);
        AppUtils.setSpinnerSelectionFromValue(sBCnEmulation, bcnEmulation);
        AppUtils.setSpinnerSelectionFromValue(sBCnEmulationType, bcnEmulationType);
        AppUtils.setSpinnerSelectionFromValue(sBCnEmulationCache, bcnEmulationCache);

        // We can log the spinner values now
        Log.d(TAG, "Spinner selected position: " + sVersion.getSelectedItemPosition());
        Log.d(TAG, "Spinner selected value: " + sVersion.getSelectedItem());
    }

    private void populateVulkanVersionSpinner(Context context, ContentsManager contentsManager, String selectedValue) {
        ArrayList<String> sdkApiVersions = collectInstalledVulkanSdkApiVersions(contentsManager);
        if (sdkApiVersions.isEmpty()) {
            // No installed Vulkan SDK lane: keep previous config value and disable selector.
            sdkApiVersions.add("—");
            sVulkanVersion.setEnabled(false);
            selectedVulkanVersion = selectedValue != null && !selectedValue.trim().isEmpty() ? selectedValue : "1.3";
            sVulkanVersion.setAdapter(SpinnerAdapters.create(context, isDarkMode(context), sdkApiVersions));
            sVulkanVersion.setSelection(0);
            return;
        }

        sVulkanVersion.setEnabled(true);
        sVulkanVersion.setAdapter(SpinnerAdapters.create(context, isDarkMode(context), sdkApiVersions));
        if (!AppUtils.setSpinnerSelectionFromValue(sVulkanVersion, selectedValue)) {
            // Default to highest API exposed by installed SDK lanes.
            sVulkanVersion.setSelection(sdkApiVersions.size() - 1);
        }
        selectedVulkanVersion = sVulkanVersion.getSelectedItem().toString();
    }

    private ArrayList<String> collectInstalledVulkanSdkApiVersions(ContentsManager contentsManager) {
        TreeSet<Integer> apiMinors = new TreeSet<>();
        List<ContentProfile> profiles = contentsManager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_VULKAN_SDK);
        if (profiles == null) return new ArrayList<>();

        for (ContentProfile profile : profiles) {
            if (profile == null || !profile.locallyInstalled) continue;
            int[] range = resolveVulkanApiRangeFromProfile(profile);
            int minMinor = range[0];
            int maxMinor = range[1];
            if (maxMinor < 1) continue;
            for (int minor = minMinor; minor <= maxMinor; minor++) {
                apiMinors.add(minor);
            }
        }

        ArrayList<String> versions = new ArrayList<>();
        for (Integer minor : apiMinors) {
            versions.add(String.format(Locale.US, "1.%d", minor));
        }
        return versions;
    }

    private int parseMaxVulkanMinorFromProfile(ContentProfile profile) {
        return resolveVulkanApiRangeFromProfile(profile)[1];
    }

    private int[] resolveVulkanApiRangeFromProfile(ContentProfile profile) {
        int min = profile != null ? profile.vulkanApiMin : 0;
        int max = profile != null ? profile.vulkanApiMax : 0;
        if (min > 0 && max > 0) {
            if (min > max) {
                int tmp = min;
                min = max;
                max = tmp;
            }
            return new int[]{min, max};
        }

        int inferredMax = 0;
        inferredMax = Math.max(inferredMax, parseMaxVulkanMinorFromString(profile != null ? profile.verName : null));
        inferredMax = Math.max(inferredMax, parseMaxVulkanMinorFromString(profile != null ? profile.desc : null));
        inferredMax = Math.max(inferredMax, parseMaxVulkanMinorFromString(profile != null ? profile.releaseTag : null));
        inferredMax = Math.max(inferredMax, parseMaxVulkanMinorFromString(profile != null ? profile.vulkanSdkVersion : null));
        if (inferredMax > 0) return new int[]{1, inferredMax};
        return new int[]{0, 0};
    }

    private int parseMaxVulkanMinorFromString(String raw) {
        if (raw == null || raw.trim().isEmpty()) return 0;
        int maxMinor = 0;
        Matcher matcher = VULKAN_API_PATTERN.matcher(raw);
        while (matcher.find()) {
            try {
                maxMinor = Math.max(maxMinor, Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException ignored) {
            }
        }
        return maxMinor;
    }

    private void setSpinnerSelectionWithFallback(Spinner spinner, String version, String graphicsDriver) {
        if (spinner.getCount() == 0) return;
        String firstItem = spinner.getItemAtPosition(0).toString();
        if (AppUtils.isMissingComponentValue(firstItem)) {
            spinner.setSelection(0, false);
            selectedVersion = firstItem;
            return;
        }

        // First, attempt to find an exact match (case-insensitive)
        for (int i = 0; i < spinner.getCount(); i++) {
            String item = spinner.getItemAtPosition(i).toString();
            if (item.equalsIgnoreCase(version)) {
                spinner.setSelection(i);
                return;
            }
        }

        AppUtils.setSpinnerSelectionFromValue(spinner, GPUInformation.isDriverSupported(DefaultVersion.WRAPPER_ADRENO, getContext()) ? DefaultVersion.WRAPPER_ADRENO : DefaultVersion.WRAPPER);
    }

    private void applyPopupTheme(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isDarkMode = preferences.getBoolean("dark_mode", false);
        int popupBg = isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background;
        SpinnerAdapters.applySurface(sVersion, isDarkMode);
        SpinnerAdapters.applySurface(sVulkanVersion, isDarkMode);
        SpinnerAdapters.applySurface(sPresentMode, isDarkMode);
        SpinnerAdapters.applySurface(sGPUName, isDarkMode);
        SpinnerAdapters.applySurface(sMaxDeviceMemory, isDarkMode);
        SpinnerAdapters.applySurface(sResourceType, isDarkMode);
        SpinnerAdapters.applySurface(sBCnEmulation, isDarkMode);
        SpinnerAdapters.applySurface(sBCnEmulationType, isDarkMode);
        SpinnerAdapters.applySurface(sBCnEmulationCache, isDarkMode);
        SpinnerAdapters.applySurface(mscAvailableExtensions, isDarkMode);
        sVersion.setPopupBackgroundResource(popupBg);
        sVulkanVersion.setPopupBackgroundResource(popupBg);
        sPresentMode.setPopupBackgroundResource(popupBg);
        sGPUName.setPopupBackgroundResource(popupBg);
        sMaxDeviceMemory.setPopupBackgroundResource(popupBg);
        sResourceType.setPopupBackgroundResource(popupBg);
        sBCnEmulation.setPopupBackgroundResource(popupBg);
        sBCnEmulationType.setPopupBackgroundResource(popupBg);
        sBCnEmulationCache.setPopupBackgroundResource(popupBg);
    }

    private boolean isDarkMode(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean("dark_mode", false);
    }

}
