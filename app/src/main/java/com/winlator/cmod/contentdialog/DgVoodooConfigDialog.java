package com.winlator.cmod.contentdialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.preference.PreferenceManager;

import com.winlator.cmod.R;
import com.winlator.cmod.XServerDisplayActivity;
import com.winlator.cmod.contents.DgVoodooManager;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.DefaultVersion;
import com.winlator.cmod.core.EnvVars;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.core.KeyValueSet;
import com.winlator.cmod.core.SpinnerAdapters;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DgVoodooConfigDialog extends ContentDialog {
    private static final String DEFAULT_ARCH = "auto";
    private static final String DEFAULT_FORCE_D3D11 = "0";
    private static final String DEFAULT_VSYNC = "0";
    private static final String DEFAULT_FLIP_MODEL = "1";
    private static final String LEGACY_DXVK_X64 = "1.12.0";
    private static final String LEGACY_DXVK_ARM64EC = "1.12.0-dyasync-arm64ec";
    private static final Pattern SEMVER_LOOSE = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    public DgVoodooConfigDialog(View anchor) {
        super(anchor.getContext(), R.layout.dgvoodoo_config_dialog);
        Context context = anchor.getContext();
        boolean runtimeMode = context instanceof XServerDisplayActivity;
        DgVoodooManager manager = new DgVoodooManager(context);
        boolean packageInstalled = manager.isInstalled();
        ArrayList<String> installedArchitectures = manager.getInstalledArchitectures();
        ArrayList<String> installedPackageLanes = manager.getInstalledPackageLanes();
        String installedArchitectureSummary = buildRuntimeArchSummary(installedArchitectures);
        String installedPackageLaneSummary = buildPackageLaneSummary(installedPackageLanes);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isDarkMode = preferences.getBoolean("dark_mode", false);

        setIcon(R.drawable.ae_icon_settings);
        setTitle(R.string.dgvoodoo_configuration);

        TextView packageSummaryView = findViewById(R.id.TVDgVoodooPackageSummary);
        TextView runtimeSetsSummaryView = findViewById(R.id.TVDgVoodooRuntimeSetsSummary);
        TextView archAvailabilityView = findViewById(R.id.TVDgVoodooArchAvailability);
        TextView laneSummaryView = findViewById(R.id.TVDgVoodooLaneSummary);
        Spinner archSpinner = findViewById(R.id.SDgVoodooArch);
        SwitchCompat forceD3D11Switch = findViewById(R.id.SWDgVoodooForceD3D11);
        SwitchCompat vsyncSwitch = findViewById(R.id.SWDgVoodooVSync);
        SwitchCompat flipModelSwitch = findViewById(R.id.SWDgVoodooFlipModel);

        packageSummaryView.setText(context.getString(
                R.string.dgvoodoo_config_summary,
                packageInstalled
                        ? context.getString(
                                R.string.dgvoodoo_package_state_installed,
                                compactDisplayValue(manager.getVersionHint(), 48)
                        )
                        : context.getString(R.string.dgvoodoo_package_state_missing)
        ));
        runtimeSetsSummaryView.setText(context.getString(R.string.dgvoodoo_config_runtime_sets, installedArchitectureSummary));
        archAvailabilityView.setText(context.getString(R.string.dgvoodoo_config_arch_available, installedArchitectureSummary));
        laneSummaryView.setText(context.getString(R.string.dgvoodoo_config_package_lanes, installedPackageLaneSummary));

        ArrayList<String> labels = new ArrayList<>();
        ArrayList<String> values = new ArrayList<>();
        if (packageInstalled) {
            labels.add(context.getString(R.string.dgvoodoo_arch_auto));
            values.add(DEFAULT_ARCH);
            for (String arch : installedArchitectures) {
                if ("x86".equalsIgnoreCase(arch)) {
                    labels.add(context.getString(R.string.dgvoodoo_arch_x86));
                    values.add("x86");
                } else if ("x64".equalsIgnoreCase(arch)) {
                    labels.add(context.getString(R.string.dgvoodoo_arch_x64));
                    values.add("x64");
                } else if ("arm64".equalsIgnoreCase(arch) || "aarch64".equalsIgnoreCase(arch)) {
                    labels.add(context.getString(R.string.dgvoodoo_arch_arm64));
                    values.add("arm64");
                } else if ("arm64ec".equalsIgnoreCase(arch) || "arm64-ec".equalsIgnoreCase(arch)) {
                    labels.add(context.getString(R.string.dgvoodoo_arch_arm64ec));
                    values.add("arm64ec");
                }
            }
            if (values.size() == 1) {
                labels.add(context.getString(R.string.dgvoodoo_arch_x64));
                values.add("x64");
            }
        } else {
            labels.add(AppUtils.MISSING_COMPONENT_PLACEHOLDER);
            values.add(DEFAULT_ARCH);
        }
        archSpinner.setAdapter(runtimeMode
                ? SpinnerAdapters.createRuntime(context, labels)
                : SpinnerAdapters.create(context, isDarkMode, labels));
        if (runtimeMode) {
            SpinnerAdapters.applyRuntimeSurface(archSpinner);
        } else {
            SpinnerAdapters.applySurface(archSpinner, isDarkMode);
            archSpinner.setPopupBackgroundResource(isDarkMode
                    ? R.drawable.surface_dialog_background_dark
                    : R.drawable.surface_dialog_background);
        }

        KeyValueSet config = parseConfig(anchor.getTag());
        int selectedIndex = packageInstalled
                ? indexOf(values, normalizeArch(config.get("dgvoodooArch")))
                : 0;
        archSpinner.setSelection(selectedIndex, false);
        archSpinner.setEnabled(packageInstalled);

        forceD3D11Switch.setChecked(parseSwitch(config.get("dgvoodooForceD3D11"), false));
        vsyncSwitch.setChecked(parseSwitch(config.get("dgvoodooVSync"), false));
        flipModelSwitch.setChecked(parseSwitch(config.get("dgvoodooFlipModel"), true));
        forceD3D11Switch.setEnabled(packageInstalled);
        vsyncSwitch.setEnabled(packageInstalled);
        flipModelSwitch.setEnabled(packageInstalled);

        setOnConfirmCallback(() -> {
            if (packageInstalled) {
                int currentIndex = archSpinner.getSelectedItemPosition();
                if (currentIndex < 0 || currentIndex >= values.size()) currentIndex = 0;
                config.put("dgvoodooArch", values.get(currentIndex));
                config.put("dgvoodooForceD3D11", forceD3D11Switch.isChecked() ? "1" : "0");
                config.put("dgvoodooVSync", vsyncSwitch.isChecked() ? "1" : "0");
                config.put("dgvoodooFlipModel", flipModelSwitch.isChecked() ? "1" : "0");
            }
            config.put("dgvoodooVersionHint", manager.getVersionHint());
            anchor.setTag(config.toString());
            ForensicLogger.logEvent(
                    context,
                    "info",
                    "DGVOODOO_CONFIG_SAVED",
                    null,
                    "wrapper_config",
                    "dgvoodoo_config_saved",
                    ForensicLogger.fields(
                            "package_installed", packageInstalled ? "1" : "0",
                            "version_hint", manager.getVersionHint(),
                            "available_arches", installedArchitectureSummary,
                            "arch", normalizeArch(config.get("dgvoodooArch")),
                            "force_d3d11", normalizeToggle(config.get("dgvoodooForceD3D11"), DEFAULT_FORCE_D3D11),
                            "vsync", normalizeToggle(config.get("dgvoodooVSync"), DEFAULT_VSYNC),
                            "flip_model", normalizeToggle(config.get("dgvoodooFlipModel"), DEFAULT_FLIP_MODEL)
                    )
            );
        });
    }

    public static KeyValueSet parseConfig(Object config) {
        return new KeyValueSet(config != null ? String.valueOf(config) : "");
    }

    public static void setEnvVars(Context context, KeyValueSet config, EnvVars vars, DgVoodooManager manager) {
        vars.put("AERO_DGVOODOO_MODE", "local_import");
        vars.put("AERO_DGVOODOO_VERSION_HINT", manager.getVersionHint());
        vars.put("AERO_DGVOODOO_ARCH_REQUESTED", normalizeArch(config.get("dgvoodooArch")));
        vars.put("AERO_DGVOODOO_FORCE_D3D11", normalizeToggle(config.get("dgvoodooForceD3D11"), DEFAULT_FORCE_D3D11));
        vars.put("AERO_DGVOODOO_VSYNC", normalizeToggle(config.get("dgvoodooVSync"), DEFAULT_VSYNC));
        vars.put("AERO_DGVOODOO_FLIP_MODEL", normalizeToggle(config.get("dgvoodooFlipModel"), DEFAULT_FLIP_MODEL));
    }

    public static String resolveCompanionDxvkVersion(KeyValueSet config, String runtimeArch, boolean requireLegacyCompat) {
        return resolveCompanionDxvkVersion(config, runtimeArch, requireLegacyCompat, null);
    }

    public static String resolveCompanionDxvkVersion(
            KeyValueSet config,
            String runtimeArch,
            boolean requireLegacyCompat,
            List<String> installedVersions
    ) {
        String requested = sanitizeVersion(config.get("version"), DefaultVersion.DXVK);
        if (!requireLegacyCompat) return requested;
        if (requested.startsWith("1.")) return requested;
        String preferredInstalled = pickBestLegacyDxvkCandidate(installedVersions, runtimeArch);
        if (!preferredInstalled.isEmpty()) return preferredInstalled;
        return "arm64ec".equalsIgnoreCase(runtimeArch) || "arm64".equalsIgnoreCase(runtimeArch)
                ? LEGACY_DXVK_ARM64EC
                : LEGACY_DXVK_X64;
    }

    public static String resolveCompanionVkd3dVersion(KeyValueSet config, boolean requireLegacyCompat) {
        if (requireLegacyCompat) return "None";
        return sanitizeVersion(config.get("vkd3dVersion"), "None");
    }

    public static boolean resolveCompanionForceD3d11(KeyValueSet config, boolean requireLegacyCompat) {
        return requireLegacyCompat || parseSwitch(config.get("dgvoodooForceD3D11"), false);
    }

    public static String normalizeArch(String value) {
        if ("x86".equalsIgnoreCase(value)) return "x86";
        if ("x64".equalsIgnoreCase(value)) return "x64";
        if ("arm64".equalsIgnoreCase(value) || "aarch64".equalsIgnoreCase(value)) return "arm64";
        if ("arm64ec".equalsIgnoreCase(value) || "arm64-ec".equalsIgnoreCase(value)) return "arm64ec";
        return DEFAULT_ARCH;
    }

    private static String sanitizeVersion(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        String normalized = value.trim();
        return AppUtils.isMissingComponentValue(normalized) ? fallback : normalized;
    }

    private static String pickBestLegacyDxvkCandidate(List<String> installedVersions, String runtimeArch) {
        if (installedVersions == null || installedVersions.isEmpty()) return "";
        String normalizedArch = normalizeArch(runtimeArch);
        boolean arm64ecRoute = "arm64ec".equalsIgnoreCase(normalizedArch) || "arm64".equalsIgnoreCase(normalizedArch);
        String best = "";

        for (String candidate : installedVersions) {
            String normalized = sanitizeVersion(candidate, "");
            if (normalized.isEmpty() || !normalized.startsWith("1.")) continue;
            boolean candidateArm64ec = normalized.toLowerCase(Locale.ROOT).contains("arm64ec");
            if (arm64ecRoute && !candidateArm64ec) continue;
            if (!arm64ecRoute && candidateArm64ec) continue;
            if (best.isEmpty() || compareLegacyDxvkCandidate(normalized, best) > 0) {
                best = normalized;
            }
        }

        if (!best.isEmpty()) return best;
        for (String candidate : installedVersions) {
            String normalized = sanitizeVersion(candidate, "");
            if (normalized.startsWith("1.")) return normalized;
        }
        return "";
    }

    private static int compareLegacyDxvkCandidate(String left, String right) {
        int[] lv = parseSemverLoose(left);
        int[] rv = parseSemverLoose(right);
        if (lv[0] != rv[0]) return lv[0] - rv[0];
        if (lv[1] != rv[1]) return lv[1] - rv[1];
        if (lv[2] != rv[2]) return lv[2] - rv[2];

        int leftFlavor = legacyFlavorRank(left);
        int rightFlavor = legacyFlavorRank(right);
        if (leftFlavor != rightFlavor) return leftFlavor - rightFlavor;
        return left.compareToIgnoreCase(right);
    }

    private static int[] parseSemverLoose(String value) {
        if (value == null) return new int[]{0, 0, 0};
        Matcher matcher = SEMVER_LOOSE.matcher(value);
        int[] parts = new int[]{0, 0, 0};
        if (!matcher.find()) return parts;
        parts[0] = parseIntSafe(matcher.group(1));
        parts[1] = parseIntSafe(matcher.group(2));
        parts[2] = parseIntSafe(matcher.group(3));
        return parts;
    }

    private static int parseIntSafe(String value) {
        if (value == null || value.trim().isEmpty()) return 0;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static int legacyFlavorRank(String value) {
        if (value == null) return 0;
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.contains("dyasync")) return 3;
        if (normalized.contains("sarek")) return 2;
        if (normalized.contains("async")) return 1;
        return 0;
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

    private static int indexOf(ArrayList<String> items, String value) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).equalsIgnoreCase(value)) return i;
        }
        return 0;
    }

    private static String buildRuntimeArchSummary(List<String> values) {
        if (values == null || values.isEmpty()) return "—";
        ArrayList<String> labels = new ArrayList<>();
        for (String value : values) {
            String label = formatRuntimeArchLabel(value);
            if (!label.isEmpty() && !labels.contains(label)) labels.add(label);
        }
        return labels.isEmpty() ? "—" : String.join(", ", labels);
    }

    private static String buildPackageLaneSummary(List<String> values) {
        if (values == null || values.isEmpty()) return "—";
        ArrayList<String> labels = new ArrayList<>();
        for (String value : values) {
            String label = formatPackageLaneLabel(value);
            if (!label.isEmpty() && !labels.contains(label)) labels.add(label);
        }
        return labels.isEmpty() ? "—" : String.join(", ", labels);
    }

    private static String formatRuntimeArchLabel(String value) {
        String normalized = normalizeArch(value);
        if ("x86".equals(normalized)) return "x86";
        if ("x64".equals(normalized)) return "x64";
        if ("arm64".equals(normalized)) return "arm64";
        if ("arm64ec".equals(normalized)) return "arm64ec";
        return compactDisplayValue(value, 24);
    }

    private static String formatPackageLaneLabel(String value) {
        if (value == null) return "";
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || "-".equals(normalized)) return "";
        if ("x86_64".equals(normalized) || "x86-64".equals(normalized) || "amd64".equals(normalized)) return "x86_64";
        if ("arm64ec".equals(normalized) || "arm64-ec".equals(normalized)) return "arm64ec";
        return compactDisplayValue(value, 32);
    }

    private static String compactDisplayValue(String value, int maxChars) {
        if (value == null) return "";
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) return "";
        if (normalized.length() <= maxChars) return normalized;
        if (maxChars <= 1) return normalized.substring(0, 1);
        return normalized.substring(0, Math.max(0, maxChars - 1)).trim() + "…";
    }
}
