package com.winlator.cmod.contentdialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import com.winlator.cmod.R;
import com.winlator.cmod.contents.DgVoodooManager;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.EnvVars;
import com.winlator.cmod.core.KeyValueSet;

import java.util.Locale;

public class DgVoodooConfigDialog extends ContentDialog {
    private static final String DEFAULT_ARCH = "auto";
    private static final String DEFAULT_FORCE_D3D11 = "0";
    private static final String DEFAULT_VSYNC = "0";
    private static final String DEFAULT_FLIP_MODEL = "1";

    public DgVoodooConfigDialog(View anchor) {
        super(anchor.getContext());
        Context context = anchor.getContext();
        DgVoodooManager manager = new DgVoodooManager(context);
        boolean packageInstalled = manager.isInstalled();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isDarkMode = preferences.getBoolean("dark_mode", false);
        int popupBg = isDarkMode ? R.drawable.content_dialog_background_dark : R.drawable.content_dialog_background;

        setIcon(R.drawable.icon_settings);
        setTitle(R.string.dgvoodoo_configuration);

        FrameLayout frameLayout = findViewById(R.id.FrameLayout);
        frameLayout.setVisibility(View.VISIBLE);

        int padding = (int) (16f * context.getResources().getDisplayMetrics().density);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(padding, padding / 2, padding, padding / 2);

        TextView stateView = new TextView(context);
        stateView.setText(context.getString(
                R.string.dgvoodoo_config_summary,
                packageInstalled
                        ? context.getString(R.string.dgvoodoo_package_state_ready, manager.getVersionHint())
                        : context.getString(R.string.dgvoodoo_package_state_missing)
        ));
        layout.addView(stateView);

        TextView noteView = new TextView(context);
        noteView.setPadding(0, padding / 4, 0, 0);
        noteView.setText(R.string.dgvoodoo_config_note);
        layout.addView(noteView);

        TextView labelView = new TextView(context);
        labelView.setPadding(0, padding / 2, 0, 0);
        labelView.setText(R.string.dgvoodoo_config_arch);
        layout.addView(labelView);

        Spinner archSpinner = new Spinner(context);
        String[] labels = packageInstalled
                ? new String[]{
                        context.getString(R.string.dgvoodoo_arch_auto),
                        context.getString(R.string.dgvoodoo_arch_x86),
                        context.getString(R.string.dgvoodoo_arch_x64),
                        context.getString(R.string.dgvoodoo_arch_arm64),
                        context.getString(R.string.dgvoodoo_arch_arm64ec)
                }
                : new String[]{AppUtils.MISSING_COMPONENT_PLACEHOLDER};
        String[] values = packageInstalled
                ? new String[]{"auto", "x86", "x64", "arm64", "arm64ec"}
                : new String[]{"auto"};
        archSpinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, labels));
        archSpinner.setPopupBackgroundResource(popupBg);
        KeyValueSet config = parseConfig(anchor.getTag());
        if (packageInstalled) {
            AppUtils.setSpinnerSelectionFromValue(archSpinner, labels[indexOf(values, normalizeArch(config.get("dgvoodooArch")))]);
        } else {
            archSpinner.setSelection(0, false);
        }
        archSpinner.setEnabled(packageInstalled);
        layout.addView(archSpinner);

        Switch forceD3D11Switch = new Switch(context);
        forceD3D11Switch.setText(R.string.dgvoodoo_force_d3d11);
        forceD3D11Switch.setChecked(parseSwitch(config.get("dgvoodooForceD3D11"), false));
        forceD3D11Switch.setEnabled(packageInstalled);
        layout.addView(forceD3D11Switch);

        Switch vsyncSwitch = new Switch(context);
        vsyncSwitch.setText(R.string.dgvoodoo_vsync);
        vsyncSwitch.setChecked(parseSwitch(config.get("dgvoodooVSync"), false));
        vsyncSwitch.setEnabled(packageInstalled);
        layout.addView(vsyncSwitch);

        Switch flipModelSwitch = new Switch(context);
        flipModelSwitch.setText(R.string.dgvoodoo_flip_model);
        flipModelSwitch.setChecked(parseSwitch(config.get("dgvoodooFlipModel"), true));
        flipModelSwitch.setEnabled(packageInstalled);
        layout.addView(flipModelSwitch);

        frameLayout.addView(layout);

        setOnConfirmCallback(() -> {
            if (packageInstalled) {
                config.put("dgvoodooArch", values[archSpinner.getSelectedItemPosition()]);
                config.put("dgvoodooForceD3D11", forceD3D11Switch.isChecked() ? "1" : "0");
                config.put("dgvoodooVSync", vsyncSwitch.isChecked() ? "1" : "0");
                config.put("dgvoodooFlipModel", flipModelSwitch.isChecked() ? "1" : "0");
            }
            config.put("dgvoodooVersionHint", manager.getVersionHint());
            anchor.setTag(config.toString());
        });
    }

    public static KeyValueSet parseConfig(Object config) {
        return new KeyValueSet(config != null ? String.valueOf(config) : "");
    }

    public static void setEnvVars(Context context, KeyValueSet config, EnvVars vars, DgVoodooManager manager) {
        WineD3DConfigDialog.setEnvVars(context, config, vars);
        vars.put("AERO_DGVOODOO_MODE", "local_import");
        vars.put("AERO_DGVOODOO_VERSION_HINT", manager.getVersionHint());
        vars.put("AERO_DGVOODOO_ARCH_REQUESTED", normalizeArch(config.get("dgvoodooArch")));
        vars.put("AERO_DGVOODOO_FORCE_D3D11", normalizeToggle(config.get("dgvoodooForceD3D11"), DEFAULT_FORCE_D3D11));
        vars.put("AERO_DGVOODOO_VSYNC", normalizeToggle(config.get("dgvoodooVSync"), DEFAULT_VSYNC));
        vars.put("AERO_DGVOODOO_FLIP_MODEL", normalizeToggle(config.get("dgvoodooFlipModel"), DEFAULT_FLIP_MODEL));
    }

    public static String normalizeArch(String value) {
        if ("x86".equalsIgnoreCase(value)) return "x86";
        if ("x64".equalsIgnoreCase(value)) return "x64";
        if ("arm64".equalsIgnoreCase(value) || "aarch64".equalsIgnoreCase(value)) return "arm64";
        if ("arm64ec".equalsIgnoreCase(value) || "arm64-ec".equalsIgnoreCase(value)) return "arm64ec";
        return DEFAULT_ARCH;
    }

    private static String normalizeToggle(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("1".equals(normalized) || "true".equals(normalized) || "yes".equals(normalized)) return "1";
        if ("0".equals(normalized) || "false".equals(normalized) || "no".equals(normalized)) return "0";
        return fallback;
    }

    private static boolean parseSwitch(String value, boolean fallback) {
        return "1".equals(normalizeToggle(value, fallback ? "1" : "0"));
    }

    private static int indexOf(String[] items, String value) {
        for (int i = 0; i < items.length; i++) {
            if (items[i].equalsIgnoreCase(value)) return i;
        }
        return 0;
    }
}
