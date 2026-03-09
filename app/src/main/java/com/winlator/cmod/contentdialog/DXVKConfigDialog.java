package com.winlator.cmod.contentdialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.ToggleButton;

import com.winlator.cmod.R;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.DefaultVersion;
import com.winlator.cmod.core.EnvVars;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.core.KeyValueSet;
import com.winlator.cmod.core.SpinnerAdapters;
import com.winlator.cmod.core.StringUtils;
import com.winlator.cmod.core.VKD3DVersionItem;
import com.winlator.cmod.xenvironment.ImageFs;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.preference.PreferenceManager;

public class DXVKConfigDialog extends ContentDialog {
    public static final String DEFAULT_CONFIG = Container.DEFAULT_DXWRAPPERCONFIG;
    public static final int DXVK_TYPE_NONE = 0;
    public static final int DXVK_TYPE_ASYNC = 1;
    public static final int DXVK_TYPE_GPLASYNC = 2;
    private final ToggleButton swAsync;
    private boolean isARM64EC = false;
    private final ToggleButton swAsyncCache;
    private final View llAsync;
    private final View llAsyncCache;
    private final Context context;
    private static List<String> dxvkVersions;
    private static final Pattern SEMVER = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    private static Integer tryGetMajor(String s) {
        if (s == null) return null;
        Matcher m = SEMVER.matcher(s);
        if (!m.find()) return null;
        try {
            return Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static final String[] VKD3D_FEATURE_LEVEL = {"12_0", "12_1", "12_2", "11_1", "11_0", "10_1", "10_0", "9_3", "9_2", "9_1"};

    private static int compareVersion(String varA, String varB) {
        final String[] levelsA = varA.split("\\.");
        final String[] levelsB = varB.split("\\.");
        int minLen = Math.min(levelsA.length, levelsB.length);
        int numA, numB;

        for (int i = 0; i < minLen; i++) {
            numA = Integer.parseInt(levelsA[i]);
            numB = Integer.parseInt(levelsB[i]);
            if (numA != numB)
                return numA - numB;
        }

        if (levelsA.length != levelsB.length)
            return levelsA.length - levelsB.length;

        return 0;
    }

    public DXVKConfigDialog(View anchor, boolean isARM64EC) {
        super(anchor.getContext(), R.layout.dxvk_config_dialog);
        context = anchor.getContext();
        setIcon(R.drawable.ae_icon_settings);
        // Marker for folded-contract checks: DXVK + VKD3D.
        setTitle(context.getString(R.string.dxvk_vkd3d_configuration));

        final Spinner sDXVKVersion = findViewById(R.id.SDXVKVersion);
        final Spinner sVKD3DVersion = findViewById(R.id.SVKD3DVersion);
        final Spinner sFramerate = findViewById(R.id.SFramerate);
        final Spinner sVKD3DFeatureLevel = findViewById(R.id.SVKD3DFeatureLevel);
        swAsync = findViewById(R.id.SWAsync);
        swAsyncCache = findViewById(R.id.SWAsyncCache);
        llAsync = findViewById(R.id.LLAsync);
        llAsyncCache = findViewById(R.id.LLAsyncCache);
        applyPopupTheme(sDXVKVersion, sVKD3DVersion, sFramerate, sVKD3DFeatureLevel);

        ContentsManager contentsManager = new ContentsManager(context);
        contentsManager.syncContents();

        KeyValueSet config = parseConfig(anchor.getTag());
        loadDxvkVersionSpinner(contentsManager, sDXVKVersion, isARM64EC);
        loadVkd3dVersionSpinner(contentsManager, sVKD3DVersion);

        sVKD3DFeatureLevel.setAdapter(SpinnerAdapters.create(context, isDarkMode(), Arrays.asList(VKD3D_FEATURE_LEVEL)));

        setDXVKSpinner(sDXVKVersion, config, contentsManager, isARM64EC);
        AppUtils.setSpinnerSelectionFromIdentifier(sFramerate, config.get("framerate"));
        AppUtils.setSpinnerSelectionFromIdentifier(sVKD3DVersion, config.get("vkd3dVersion"));
        AppUtils.setSpinnerSelectionFromIdentifier(sVKD3DFeatureLevel, config.get("vkd3dLevel"));
        swAsync.setChecked(config.get("async").equals("1"));
        swAsyncCache.setChecked(config.get("asyncCache").equals("1"));

        updateConfigVisibility(getDXVKType(sDXVKVersion.getSelectedItemPosition()));

        sDXVKVersion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateConfigVisibility(getDXVKType(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        sVKD3DVersion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                VKD3DVersionItem selectedItem = (VKD3DVersionItem) sVKD3DVersion.getSelectedItem();
                String selectedVersion = selectedItem != null ? selectedItem.getIdentifier() : "";
                String currentDXVKVersion = config.get("version");

                if (!"None".equalsIgnoreCase(selectedVersion) && !AppUtils.isMissingComponentValue(selectedVersion)) {
                    ArrayList<String> versions = new ArrayList<>();

                    for (int i = 0; i < dxvkVersions.size(); i++) {
                        if (AppUtils.isMissingComponentValue(dxvkVersions.get(i))) continue;
                        Integer major = tryGetMajor(dxvkVersions.get(i));
                        if (major != null && major < 2) {
                            versions.add(dxvkVersions.get(i));
                        }
                    }

                    dxvkVersions.removeAll(versions);
                    if (dxvkVersions.isEmpty()) {
                        dxvkVersions.add(AppUtils.MISSING_COMPONENT_PLACEHOLDER);
                    }

                    sDXVKVersion.setAdapter(SpinnerAdapters.create(context, isDarkMode(), dxvkVersions));
                    sDXVKVersion.setEnabled(!AppUtils.isMissingComponentValue(dxvkVersions.get(0)));

                    Integer curMajor = tryGetMajor(currentDXVKVersion);
                    AppUtils.setSpinnerSelectionFromIdentifier(
                            sDXVKVersion,
                            (curMajor != null && curMajor >= 2) ? currentDXVKVersion : DefaultVersion.DXVK
                    );
                    updateConfigVisibility(getDXVKType(sDXVKVersion.getSelectedItemPosition()));
                }
                else {
                    loadDxvkVersionSpinner(contentsManager, sDXVKVersion, isARM64EC);
                    AppUtils.setSpinnerSelectionFromIdentifier(sDXVKVersion, config.get("version"));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        setOnConfirmCallback(() -> {
            String selectedDxvkVersion = sDXVKVersion.getSelectedItem() != null ? sDXVKVersion.getSelectedItem().toString() : "";
            if (!AppUtils.isMissingComponentValue(selectedDxvkVersion)) {
                config.put("version", selectedDxvkVersion);
            }
            config.put("framerate", StringUtils.parseNumber(sFramerate.getSelectedItem()));
            config.put("async", ((swAsync.isChecked())&&(llAsync.getVisibility()==View.VISIBLE))?"1":"0");
            config.put("asyncCache", ((swAsyncCache.isChecked())&&(llAsyncCache.getVisibility()==View.VISIBLE))?"1":"0");
            VKD3DVersionItem selectedItem = (VKD3DVersionItem) sVKD3DVersion.getSelectedItem();
            String selectedVkd3dIdentifier = selectedItem != null ? selectedItem.getIdentifier() : "None";
            config.put("vkd3dVersion", AppUtils.isMissingComponentValue(selectedVkd3dIdentifier) ? "None" : selectedVkd3dIdentifier);
            config.put("vkd3dLevel", sVKD3DFeatureLevel.getSelectedItem().toString());
            // Legacy DDraw wrapper key is removed from DXVK config; dgVoodoo has its own wrapper mode.
            config.remove("ddrawrapper");
            anchor.setTag(config.toString());
            ForensicLogger.logEvent(
                    context,
                    "info",
                    "DXVK_VKD3D_CONFIG_SAVED",
                    null,
                    "wrapper_config",
                    "dxvk_vkd3d_config_saved",
                    ForensicLogger.fields(
                            "dxvk_version", config.get("version"),
                            "vkd3d_version", config.get("vkd3dVersion"),
                            "vkd3d_feature_level", config.get("vkd3dLevel"),
                            "framerate", config.get("framerate"),
                            "async", config.get("async"),
                            "async_cache", config.get("asyncCache"),
                            "is_arm64ec", isARM64EC ? "1" : "0"
                    )
            );
        });
    }

    @Override
    public void show() {
        super.show();
        if (getWindow() != null) {
            getWindow().setLayout(
                    AppUtils.getPreferredWideDialogWidth(getContext()),
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void updateConfigVisibility(int dxvkType) {
        if (dxvkType == DXVK_TYPE_ASYNC) {
            llAsync.setVisibility(View.VISIBLE);
            llAsyncCache.setVisibility(View.GONE);
        } else if (dxvkType == DXVK_TYPE_GPLASYNC) {
            llAsync.setVisibility(View.VISIBLE);
            llAsyncCache.setVisibility(View.VISIBLE);
        } else {
            llAsync.setVisibility(View.GONE);
            llAsyncCache.setVisibility(View.GONE);
        }
    }

    private int getDXVKType(int pos) {
        if (dxvkVersions == null || pos < 0 || pos >= dxvkVersions.size()) return DXVK_TYPE_NONE;
        final String v = dxvkVersions.get(pos);
        if (AppUtils.isMissingComponentValue(v)) return DXVK_TYPE_NONE;
        int dxvkType = DXVK_TYPE_NONE;
        if (v.contains("gplasync"))
            dxvkType = DXVK_TYPE_GPLASYNC;
        else if (v.contains("async"))
            dxvkType = DXVK_TYPE_ASYNC;
        return dxvkType;
    }

    private void setDXVKSpinner(Spinner sDXVKVersion, KeyValueSet config, ContentsManager contentsManager, boolean isARM64EC) {
        if (dxvkVersions == null || dxvkVersions.isEmpty() || AppUtils.isMissingComponentValue(dxvkVersions.get(0))) {
            sDXVKVersion.setSelection(0, false);
            return;
        }

        String selectedVersion = config.get("vkd3dVersion");
        String currentDXVKVersion = config.get("version");
        if (!selectedVersion.equals("None")) {
            ArrayList<String> versions = new ArrayList<>();

            for (int i = 0; i < dxvkVersions.size(); i++) {
                if (AppUtils.isMissingComponentValue(dxvkVersions.get(i))) continue;
                Integer major = tryGetMajor(dxvkVersions.get(i));
                if (major != null && major < 2) {
                    versions.add(dxvkVersions.get(i));
                }
            }

            dxvkVersions.removeAll(versions);
            if (dxvkVersions.isEmpty()) {
                dxvkVersions.add(AppUtils.MISSING_COMPONENT_PLACEHOLDER);
            }

            sDXVKVersion.setAdapter(SpinnerAdapters.create(context, isDarkMode(), dxvkVersions));
            sDXVKVersion.setEnabled(!AppUtils.isMissingComponentValue(dxvkVersions.get(0)));

            Integer curMajor = tryGetMajor(currentDXVKVersion);
            AppUtils.setSpinnerSelectionFromIdentifier(
                    sDXVKVersion,
                    (curMajor != null && curMajor >= 2) ? currentDXVKVersion : DefaultVersion.DXVK
            );
        }
        else
            AppUtils.setSpinnerSelectionFromIdentifier(sDXVKVersion, currentDXVKVersion);
    }

    public static KeyValueSet parseConfig(Object config) {
        String data = config != null && !config.toString().isEmpty() ? config.toString() :  DEFAULT_CONFIG;
        return new KeyValueSet(data);
    }

    public static void setEnvVars(Context context, KeyValueSet config, EnvVars envVars) {
        String content = "";

        String framerate = config.get("framerate");

        if (!framerate.isEmpty() && !framerate.equals("0")) {
            content += "dxgi.maxFrameRate = " + framerate + "; ";
            content += "d3d9.maxFrameRate = " + framerate;
            envVars.put("DXVK_FRAME_RATE", framerate);
        }

        String async = config.get("async");
        if (!async.isEmpty() && !async.equals("0"))
            envVars.put("DXVK_ASYNC", "1");

        String asyncCache = config.get("asyncCache");
        if (!asyncCache.isEmpty() && !asyncCache.equals("0"))
            envVars.put("DXVK_GPLASYNCCACHE", "1");

        if (!content.isEmpty())
            envVars.put("DXVK_CONFIG", content);

        envVars.put("VKD3D_FEATURE_LEVEL", config.get("vkd3dLevel"));
        envVars.put("DXVK_STATE_CACHE_PATH", context.getFilesDir() + "/imagefs/" + ImageFs.CACHE_PATH);
    }

    private void loadDxvkVersionSpinner(ContentsManager manager, Spinner spinner, boolean isARM64EC) {
        this.isARM64EC = isARM64EC;
        List<String> itemList = new ArrayList<>();

        String[] originalItems = context.getResources().getStringArray(R.array.dxvk_version_entries);
        for (String version : originalItems) {
            if (FileUtils.getSize(context, "dxwrapper/dxvk-" + version + ".tzst") > 0) {
                itemList.add(version);
            }
        }

        List<ContentProfile> profiles = manager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_DXVK);
        if (profiles != null) {
            for (ContentProfile profile : profiles) {
                if (profile == null || !profile.locallyInstalled) continue;
                if (profile.verName == null || profile.verName.trim().isEmpty()) continue;
                if (!itemList.contains(profile.verName)) {
                    itemList.add(profile.verName);
                }
            }
        }

        Iterator<String> iterator = itemList.iterator();
        while (iterator.hasNext()) {
            String value = iterator.next();
            if (value.contains("arm64ec") && !isARM64EC) {
                iterator.remove();
            }
        }

        boolean hasVersions = !itemList.isEmpty();
        if (!hasVersions) {
            itemList.add(AppUtils.MISSING_COMPONENT_PLACEHOLDER);
        }

        spinner.setAdapter(SpinnerAdapters.create(context, isDarkMode(), itemList));
        spinner.setEnabled(hasVersions);
        dxvkVersions = itemList;
    }

    private void loadVkd3dVersionSpinner(ContentsManager manager, Spinner spinner) {
        List<VKD3DVersionItem> itemList = new ArrayList<>();
        boolean hasRuntimeVersions = false;

        // Add predefined bundled versions when embedded archives exist.
        String[] originalItems = context.getResources().getStringArray(R.array.vkd3d_version_entries);
        for (String version : originalItems) {
            if ("None".equalsIgnoreCase(version)) continue;
            if (FileUtils.getSize(context, "dxwrapper/vkd3d-" + version + ".tzst") > 0) {
                itemList.add(new VKD3DVersionItem(version));
                hasRuntimeVersions = true;
            }
        }

        // Add installed content profiles
        List<ContentProfile> profiles = manager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_VKD3D);
        if (profiles != null) {
            for (ContentProfile profile : profiles) {
                if (profile == null || !profile.locallyInstalled) continue;
                String displayName = profile.verName;
                int versionCode = profile.verCode;
                itemList.add(new VKD3DVersionItem(displayName, versionCode));
                hasRuntimeVersions = true;
            }
        }

        if (hasRuntimeVersions) {
            itemList.add(0, new VKD3DVersionItem("None"));
        } else {
            itemList.add(new VKD3DVersionItem(AppUtils.MISSING_COMPONENT_PLACEHOLDER));
        }

        ArrayAdapter<VKD3DVersionItem> adapter = SpinnerAdapters.createGeneric(context, isDarkMode(), itemList);
        spinner.setAdapter(adapter);
        spinner.setEnabled(hasRuntimeVersions);
    }

    private void applyPopupTheme(Spinner... spinners) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isDarkMode = preferences.getBoolean("dark_mode", false);
        int popupBg = isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background;
        for (Spinner spinner : spinners) {
            if (spinner != null) {
                SpinnerAdapters.applySurface(spinner, isDarkMode);
                spinner.setPopupBackgroundResource(popupBg);
            }
        }
    }

    private boolean isDarkMode() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean("dark_mode", false);
    }
}
