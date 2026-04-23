package com.winlator.cmod.contentdialog;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import com.winlator.cmod.MainActivity;
import com.winlator.cmod.R;
import com.winlator.cmod.container.GraphicsDrivers;
import com.winlator.cmod.contents.MesaOpenGLDriverPackageManager;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.EnvVars;
import com.winlator.cmod.core.KeyValueSet;
import com.winlator.cmod.core.SpinnerAdapters;
import com.winlator.cmod.core.StringUtils;

import java.util.ArrayList;
import java.util.Locale;

public class MesaOpenGLConfigDialog extends ContentDialog {
    private static final String DEFAULT_MESA_GL_VERSION = "3.1";
    private static final String DEFAULT_ZINK_GL_VERSION = "4.0";

    public MesaOpenGLConfigDialog(final View anchor, final String graphicsDriver) {
        super(anchor.getContext(), R.layout.virgl_config_dialog);
        Context context = anchor.getContext();
        String normalizedDriver = GraphicsDrivers.normalize(graphicsDriver);
        boolean mesaDriver = GraphicsDrivers.isMesaOpenGlBridge(normalizedDriver);
        boolean aeMaliGallium = GraphicsDrivers.isAeMaliGallium(normalizedDriver);
        String galliumDriver = GraphicsDrivers.getMesaGalliumDriver(normalizedDriver);
        MesaOpenGLDriverPackageManager packageManager = aeMaliGallium
                ? new MesaOpenGLDriverPackageManager(context, normalizedDriver)
                : null;

        setIcon(R.drawable.ae_icon_settings);
        setTitle(buildTitle(context, normalizedDriver));

        final Spinner sGLVersion = findViewById(R.id.SVersion);
        final CheckBox cbDisableVertexArrayBGRA = findViewById(R.id.CBDisableVertexArrayBGRA);
        final CheckBox cbDisableGLKHRDebug = findViewById(R.id.CBDisableGLKHRDebug);
        final TextView tvMesaBridgeSummary = findViewById(R.id.TVMesaBridgeSummary);
        final TextView tvGalliumDriverLabel = findViewById(R.id.TVGalliumDriverLabel);
        final TextView tvGalliumDriver = findViewById(R.id.TVGalliumDriver);
        final Spinner sGalliumDriver = findViewById(R.id.SGalliumDriver);
        final TextView tvDriverPackageLabel = findViewById(R.id.TVDriverPackageLabel);
        final Spinner sDriverPackage = findViewById(R.id.SDriverPackage);
        final Button btInstallPackage = findViewById(R.id.BTVirglInstallPackage);
        final Button btDeletePackage = findViewById(R.id.BTVirglDeletePackage);
        final TextView tvOpenGLVersionLabel = findViewById(R.id.TVOpenGLVersionLabel);
        final CheckBox cbVirglNoReadback = findViewById(R.id.CBVirglNoReadback);

        Object tag = anchor.getTag();
        KeyValueSet config = new KeyValueSet(tag != null ? tag.toString() : "");

        tvMesaBridgeSummary.setText(buildBridgeSummary(context, normalizedDriver, packageManager, config.get("packageVersion", "")));
        cbVirglNoReadback.setVisibility(View.GONE);
        tvGalliumDriverLabel.setText(R.string.mesa_bridge_gallium_driver);

        if (aeMaliGallium && packageManager != null) {
            tvDriverPackageLabel.setText(R.string.aemali_gallium_driver_package);
            tvDriverPackageLabel.setVisibility(View.VISIBLE);
            sDriverPackage.setVisibility(View.VISIBLE);
            btInstallPackage.setVisibility(View.VISIBLE);
            btDeletePackage.setVisibility(View.VISIBLE);
            btInstallPackage.setText(R.string.aemali_gallium_install_custom_driver);
            btDeletePackage.setText(R.string.aemali_gallium_delete_custom_driver);
            sDriverPackage.setAdapter(SpinnerAdapters.create(context, packageManager.getSelectablePackageEntries()));
            String selectedPackage = config.get(
                    "packageVersion",
                    GraphicsDrivers.getBundledDriverAsset(context, GraphicsDrivers.AEMALI_GALLIUM).version
            );
            AppUtils.setSpinnerSelectionFromValue(sDriverPackage, selectedPackage);
            sDriverPackage.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    tvMesaBridgeSummary.setText(buildBridgeSummary(
                            context,
                            normalizedDriver,
                            packageManager,
                            String.valueOf(parent.getItemAtPosition(position))
                    ));
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {
                }
            });
            btInstallPackage.setOnClickListener((view) -> ContentDialog.confirm(context, context.getString(R.string.aemali_gallium_custom_driver_install_message), () -> {
                if (!(context instanceof Activity)) {
                    AppUtils.showToast(context, R.string.aemali_gallium_custom_driver_install_failed);
                    return;
                }
                MainActivity.setDriverPackagePickerCallback((uri) -> {
                    String entryId = packageManager.installDriver(uri);
                    if (entryId.isEmpty()) {
                        AppUtils.showToast(context, R.string.aemali_gallium_custom_driver_install_failed);
                        return;
                    }
                    String entry = MesaOpenGLDriverPackageManager.toCustomEntry(entryId);
                    sDriverPackage.setAdapter(SpinnerAdapters.create(context, packageManager.getSelectablePackageEntries()));
                    AppUtils.setSpinnerSelectionFromValue(sDriverPackage, entry);
                    tvMesaBridgeSummary.setText(buildBridgeSummary(context, normalizedDriver, packageManager, entry));
                    AppUtils.showToast(context, context.getString(R.string.aemali_gallium_custom_driver_installed, entryId));
                });
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                ((Activity) context).startActivityForResult(intent, MainActivity.OPEN_DRIVER_PACKAGE_REQUEST_CODE);
            }));
            btDeletePackage.setOnClickListener((view) -> {
                String selectedCustomPackage = String.valueOf(sDriverPackage.getSelectedItem());
                if (!MesaOpenGLDriverPackageManager.isCustomPackageEntry(selectedCustomPackage)) {
                    AppUtils.showToast(context, R.string.aemali_gallium_custom_driver_delete_builtin);
                    return;
                }
                ContentDialog.confirm(context, context.getString(R.string.aemali_gallium_custom_driver_delete_message), () -> {
                    if (packageManager.removeDriver(selectedCustomPackage)) {
                        String bundledVersion = GraphicsDrivers.getBundledDriverAsset(context, GraphicsDrivers.AEMALI_GALLIUM).version;
                        sDriverPackage.setAdapter(SpinnerAdapters.create(context, packageManager.getSelectablePackageEntries()));
                        AppUtils.setSpinnerSelectionFromValue(sDriverPackage, bundledVersion);
                        tvMesaBridgeSummary.setText(buildBridgeSummary(context, normalizedDriver, packageManager, bundledVersion));
                        AppUtils.showToast(context, R.string.aemali_gallium_custom_driver_deleted);
                    }
                });
            });
        } else {
            btInstallPackage.setVisibility(View.GONE);
            btDeletePackage.setVisibility(View.GONE);
            tvDriverPackageLabel.setVisibility(View.GONE);
            sDriverPackage.setVisibility(View.GONE);
        }

        if (galliumDriver.isEmpty()) {
            tvGalliumDriverLabel.setVisibility(View.GONE);
            tvGalliumDriver.setVisibility(View.GONE);
            sGalliumDriver.setVisibility(View.GONE);
        } else {
            tvGalliumDriver.setVisibility(View.GONE);
            sGalliumDriver.setVisibility(View.VISIBLE);
            sGalliumDriver.setAdapter(SpinnerAdapters.create(
                    context,
                    context.getResources().getStringArray(aeMaliGallium
                            ? R.array.aemali_gallium_driver_entries
                            : R.array.gallium_driver_entries)
            ));
            AppUtils.setSpinnerSelectionFromIdentifier(
                    sGalliumDriver,
                    normalizeGalliumDriver(normalizedDriver, config.get("galliumDriver", galliumDriver), galliumDriver)
            );
        }

        if (mesaDriver) {
            String defaultVersion = GraphicsDrivers.isZink(normalizedDriver)
                    ? DEFAULT_ZINK_GL_VERSION
                    : DEFAULT_MESA_GL_VERSION;
            AppUtils.setSpinnerSelectionFromIdentifier(sGLVersion, config.get("glVersion", defaultVersion));
            cbDisableGLKHRDebug.setChecked(config.getBoolean("disableGLKHRDebug", true));
        } else {
            tvOpenGLVersionLabel.setVisibility(View.GONE);
            sGLVersion.setVisibility(View.GONE);
            cbDisableVertexArrayBGRA.setVisibility(View.GONE);
            cbDisableGLKHRDebug.setVisibility(View.GONE);
        }

        cbDisableVertexArrayBGRA.setChecked(config.getBoolean("disableVertexArrayBGRA", true));

        setOnConfirmCallback(() -> {
            KeyValueSet newConfig = new KeyValueSet();
            if (mesaDriver) {
                newConfig.put("glVersion", StringUtils.parseNumber(sGLVersion.getSelectedItem()));
                newConfig.put("galliumDriver", normalizeGalliumDriver(
                        normalizedDriver,
                        StringUtils.parseIdentifier(sGalliumDriver.getSelectedItem()),
                        galliumDriver
                ));
                newConfig.put("disableVertexArrayBGRA", cbDisableVertexArrayBGRA.isChecked() ? "1" : "0");
                newConfig.put("disableGLKHRDebug", cbDisableGLKHRDebug.isChecked() ? "1" : "0");
                if (aeMaliGallium) newConfig.put("packageVersion", String.valueOf(sDriverPackage.getSelectedItem()));
            }
            anchor.setTag(newConfig.toString());
        });
    }

    public static void setEnvVars(KeyValueSet config, EnvVars envVars, String graphicsDriver) {
        String normalizedDriver = GraphicsDrivers.normalize(graphicsDriver);
        String mesaExtensionOverride = GraphicsDrivers.buildMesaExtensionOverride(
                config.getBoolean("disableGLKHRDebug", true),
                config.getBoolean("disableVertexArrayBGRA", true),
                config.get("extraDisabledExtensions", "")
        );
        if (!mesaExtensionOverride.isEmpty()) envVars.put("MESA_EXTENSION_OVERRIDE", mesaExtensionOverride);
        String galliumDriver = normalizeGalliumDriver(
                normalizedDriver,
                config.get("galliumDriver", GraphicsDrivers.getMesaGalliumDriver(normalizedDriver)),
                GraphicsDrivers.getMesaGalliumDriver(normalizedDriver)
        );
        envVars.put("GALLIUM_DRIVER", galliumDriver);
        String defaultVersion = GraphicsDrivers.isZink(normalizedDriver) ? DEFAULT_ZINK_GL_VERSION : DEFAULT_MESA_GL_VERSION;
        envVars.put("MESA_GL_VERSION_OVERRIDE", config.get("glVersion", defaultVersion));
        if (GraphicsDrivers.isAeMaliGallium(normalizedDriver)) {
            envVars.put("AEMALI_OPENGL", "1");
            envVars.put("AEMALI_OPENGL_DRIVER", "aemali-gallium");
            envVars.put("AEMALI_OPENGL_GALLIUM_DRIVER", galliumDriver);
            envVars.put("AEMALI_PROFILE", "aemali-universal");
            envVars.put("AEMALI_ROUTE", "opengl-gallium");
            envVars.put("AEMALI_TRANSPORT", "drm-render-node-experimental");
            envVars.put("AERO_MESA_DRIVER", "aemali-gallium");
            envVars.put("AERO_MESA_OPENGL_DRIVER", "aemali-gallium");
        }
    }

    private static String buildTitle(Context context, String graphicsDriver) {
        String label = GraphicsDrivers.getDisplayLabel(graphicsDriver);
        return label + " / Gallium " + context.getString(R.string.configuration);
    }

    private static String buildProviderSummary(String graphicsDriver) {
        String galliumDriver = GraphicsDrivers.getMesaGalliumDriver(graphicsDriver);
        if (GraphicsDrivers.isAeMaliGallium(graphicsDriver)) return "PANFROST/LIMA Gallium";
        if (!galliumDriver.isEmpty()) return galliumDriver.toUpperCase(Locale.US);
        return GraphicsDrivers.getDisplayLabel(graphicsDriver);
    }

    private static String buildBridgeSummary(Context context,
                                             String graphicsDriver,
                                             MesaOpenGLDriverPackageManager packageManager,
                                             String packageEntry) {
        String base = context.getString(
                R.string.mesa_bridge_summary,
                GraphicsDrivers.getDisplayLabel(graphicsDriver),
                buildProviderSummary(graphicsDriver)
        );
        if (packageManager == null) return base;

        MesaOpenGLDriverPackageManager.PackageInfo info = packageManager.getPackageInfo(packageEntry);
        if (info == null) return base;
        String label = info.getDisplayLabel();
        StringBuilder summary = new StringBuilder(base);
        if (!label.isEmpty()) {
            if (summary.length() > 0) summary.append('\n');
            summary.append(label);
        }
        if (info.notes != null && !info.notes.trim().isEmpty()) {
            if (summary.length() > 0) summary.append('\n');
            summary.append(info.notes.trim());
        }
        appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_provider_lane), info.providerLane);
        appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_route), info.routeId);
        appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_owner_lane), info.ownerLane);
        appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_support), info.supportClass);
        appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_kernel_evidence), info.kernelEvidenceClass);
        appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_requirements), info.transportRequirements);
        appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_preferred_gallium), info.preferredGalliumDriver);
        appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_fallback_gallium), info.fallbackGalliumDriver);
        return summary.toString();
    }

    private static String normalizeGalliumDriver(String graphicsDriver, String requestedDriver, String fallbackDriver) {
        String normalized = StringUtils.parseIdentifier(requestedDriver);
        if (GraphicsDrivers.isAeMaliGallium(graphicsDriver)) {
            if ("panfrost".equals(normalized) || "lima".equals(normalized) || "zink".equals(normalized) || "softpipe".equals(normalized)) {
                return normalized;
            }
            String fallback = StringUtils.parseIdentifier(fallbackDriver);
            return fallback.isEmpty() ? "panfrost" : fallback;
        }
        if ("freedreno".equals(normalized)) return normalized;
        if ("zink".equals(normalized)) return normalized;
        return "zink";
    }

    private static void appendSummaryLine(StringBuilder summary, String label, String value) {
        if (summary == null) return;
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) return;
        if (summary.length() > 0) summary.append('\n');
        summary.append(label).append(": ").append(normalized);
    }
}
