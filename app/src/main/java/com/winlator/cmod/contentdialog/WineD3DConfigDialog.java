package com.winlator.cmod.contentdialog;

import android.content.Context;
import android.view.View;
import android.widget.Spinner;

import androidx.preference.PreferenceManager;

import com.winlator.cmod.R;
import com.winlator.cmod.XServerDisplayActivity;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.EnvVars;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.core.KeyValueSet;
import com.winlator.cmod.core.SpinnerAdapters;
import com.winlator.cmod.core.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class WineD3DConfigDialog extends ContentDialog {
    public static String DEFAULT_CONFIG = Container.DEFAULT_DXWRAPPERCONFIG;
    public static String[] csmtValues = { "Enabled", "Disabled" };
    public static String[] strictShaderMathValues = { "Enabled", "Disabled" };
    public static String[] offscreenRenderingModeValues = { "fbo", "backbuffer" };
    public static String[] rendererValues = { "gl", "vulkan", "gdi" };
    private Context context;

    public WineD3DConfigDialog(View anchor) {
        super(anchor.getContext(), R.layout.wined3d_config_dialog);
        context = anchor.getContext();
        setIcon(R.drawable.ae_icon_settings);
        setTitle(R.string.wined3d_configuration_title);

        final Spinner sCSMT = findViewById(R.id.SCSMT);
        final Spinner sGPUName = findViewById(R.id.SGPUName);
        final Spinner sVideoMemorySize = findViewById(R.id.SVideoMemorySize);
        final Spinner sStrictShaderMath = findViewById(R.id.SStrictShaderMath);
        final Spinner sOffscreenRenderingMode = findViewById(R.id.SOffscreenRenderingMode);
        final Spinner sRenderer = findViewById(R.id.SRenderer);

        boolean darkMode = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("dark_mode", false);
        boolean runtimeMode = context instanceof XServerDisplayActivity;
        sCSMT.setAdapter(runtimeMode
                ? SpinnerAdapters.createRuntime(context, java.util.Arrays.asList(csmtValues))
                : SpinnerAdapters.create(context, darkMode, java.util.Arrays.asList(csmtValues)));
        sStrictShaderMath.setAdapter(runtimeMode
                ? SpinnerAdapters.createRuntime(context, java.util.Arrays.asList(strictShaderMathValues))
                : SpinnerAdapters.create(context, darkMode, java.util.Arrays.asList(strictShaderMathValues)));
        sOffscreenRenderingMode.setAdapter(runtimeMode
                ? SpinnerAdapters.createRuntime(context, java.util.Arrays.asList(offscreenRenderingModeValues))
                : SpinnerAdapters.create(context, darkMode, java.util.Arrays.asList(offscreenRenderingModeValues)));
        sRenderer.setAdapter(runtimeMode
                ? SpinnerAdapters.createRuntime(context, java.util.Arrays.asList(rendererValues))
                : SpinnerAdapters.create(context, darkMode, java.util.Arrays.asList(rendererValues)));
        int popupBg = darkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background;
        applySpinnerSurface(sCSMT, runtimeMode, darkMode, popupBg);
        applySpinnerSurface(sGPUName, runtimeMode, darkMode, popupBg);
        applySpinnerSurface(sVideoMemorySize, runtimeMode, darkMode, popupBg);
        applySpinnerSurface(sStrictShaderMath, runtimeMode, darkMode, popupBg);
        applySpinnerSurface(sOffscreenRenderingMode, runtimeMode, darkMode, popupBg);
        applySpinnerSurface(sRenderer, runtimeMode, darkMode, popupBg);

        loadGPUNameSpinner(sGPUName, runtimeMode, darkMode, popupBg);

        KeyValueSet config = parseConfig(anchor.getTag());

        sCSMT.setSelection(config.get("csmt").equals("3") ? 0 : 1);
        sStrictShaderMath.setSelection(config.get("strict_shader_math").equals("1") ? 0 : 1);
        AppUtils.setSpinnerSelectionFromValue(sOffscreenRenderingMode, config.get("OffscreenRenderingMode"));
        AppUtils.setSpinnerSelectionFromValue(sGPUName, config.get("gpuName"));
        AppUtils.setSpinnerSelectionFromValue(sRenderer, config.get("renderer"));
        AppUtils.setSpinnerSelectionFromNumber(sVideoMemorySize, config.get("videoMemorySize"));

        setOnConfirmCallback(() -> {
            config.put("csmt", sCSMT.getSelectedItem().toString().equals("Enabled") ? "3": "0");
            config.put("strict_shader_math", sStrictShaderMath.getSelectedItem().toString().equals("Enabled") ? "1" : "0");
            config.put("OffscreenRenderingMode", sOffscreenRenderingMode.getSelectedItem().toString());
            config.put("gpuName", sGPUName.getSelectedItem().toString());
            config.put("videoMemorySize", StringUtils.parseNumber(sVideoMemorySize.getSelectedItem().toString()));
            config.put("renderer", sRenderer.getSelectedItem().toString());
            anchor.setTag(config.toString());
            ForensicLogger.logEvent(
                    context,
                    "info",
                    "WINED3D_CONFIG_SAVED",
                    null,
                    "wrapper_config",
                    "wined3d_config_saved",
                    ForensicLogger.fields(
                            "csmt", config.get("csmt"),
                            "strict_shader_math", config.get("strict_shader_math"),
                            "offscreen_mode", config.get("OffscreenRenderingMode"),
                            "gpu_name", config.get("gpuName"),
                            "video_memory", config.get("videoMemorySize"),
                            "renderer", config.get("renderer")
                    )
            );
        });

    }

    public static KeyValueSet parseConfig(Object config) {
        String data = config != null && !config.toString().isEmpty() ? config.toString() :  DEFAULT_CONFIG;
        return new KeyValueSet(data);
    }

    private void loadGPUNameSpinner(Spinner spinner, boolean runtimeMode, boolean darkMode, int popupBg)  {
        String gpuNameList = FileUtils.readString(context, "gpu_cards.json");
        ArrayList<String> entries = new ArrayList<>();

        try {
            JSONArray jarray = new JSONArray(gpuNameList);
            for (int i = 0; i < jarray.length(); i++) {
                JSONObject jobj = jarray.getJSONObject(i);
                String gpuName = jobj.getString("name");
                entries.add(gpuName);
            }
            spinner.setAdapter(runtimeMode
                    ? SpinnerAdapters.createRuntime(context, entries)
                    : SpinnerAdapters.create(context, darkMode, entries));
            applySpinnerSurface(spinner, runtimeMode, darkMode, popupBg);
        }
        catch (JSONException e) {
        }
    }

    private void applySpinnerSurface(Spinner spinner, boolean runtimeMode, boolean darkMode, int popupBg) {
        if (runtimeMode) {
            SpinnerAdapters.applyRuntimeSurface(spinner);
            return;
        }
        SpinnerAdapters.applySurface(spinner, darkMode);
        spinner.setPopupBackgroundResource(popupBg);
    }

    public static String getDeviceIdFromGPUName(Context context, String gpuName) {
        String gpuNameList = FileUtils.readString(context, "gpu_cards.json");
        String deviceId = "";
        try {
            JSONArray jsonArray = new JSONArray(gpuNameList);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jobj = jsonArray.getJSONObject(i);
                if (jobj.getString("name").contains(gpuName)) {
                    deviceId = jobj.getString("deviceID");
                }
            }
        }
        catch (JSONException e) {
        }

        return deviceId;
    }

    public static String getVendorIdFromGPUName(Context context, String gpuName) {
        String gpuNameList = FileUtils.readString(context, "gpu_cards.json");
        String vendorId = "";
        try {
            JSONArray jsonArray = new JSONArray(gpuNameList);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jobj = jsonArray.getJSONObject(i);
                if (jobj.getString("name").contains(gpuName)) {
                    vendorId = jobj.getString("vendorID");
                }
            }
        }
        catch (JSONException e) {
        }

        return vendorId;
    }

    public static void setEnvVars(Context context, KeyValueSet config, EnvVars vars) {
        String deviceID = getDeviceIdFromGPUName(context, config.get("gpuName"));
        String vendorID = getVendorIdFromGPUName(context, config.get("gpuName"));
        String wined3dConfig = "csmt=0x" + config.get("csmt") + ",strict_shader_math=0x" + config.get("strict_shader_math") + ",OffscreenRenderingMode=" + config.get("OffscreenRenderingMode") + ",VideoMemorySize=" + config.get("videoMemorySize") + ",VideoPciDeviceID=" + deviceID + ",VideoPciVendorID=" + vendorID + ",renderer=" + config.get("renderer");
        vars.put("WINE_D3D_CONFIG", wined3dConfig);
    }
}
