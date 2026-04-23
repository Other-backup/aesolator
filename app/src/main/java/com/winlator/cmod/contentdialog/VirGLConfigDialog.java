package com.winlator.cmod.contentdialog;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.winlator.cmod.MainActivity;
import com.winlator.cmod.R;
import com.winlator.cmod.container.GraphicsDrivers;
import com.winlator.cmod.contents.VirGLDriverPackageManager;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.EnvVars;
import com.winlator.cmod.core.KeyValueSet;
import com.winlator.cmod.core.SpinnerAdapters;
import com.winlator.cmod.core.StringUtils;

import java.util.ArrayList;
import java.util.Locale;

public class VirGLConfigDialog extends ContentDialog {
    public static final String DEFAULT_GL_VERSION = "3.1";
    public static final String DEFAULT_GALLIUM_DRIVER = "virpipe";

    public VirGLConfigDialog(final View anchor) {
        super(anchor.getContext(), R.layout.virgl_config_dialog);
        Context context = anchor.getContext();
        setIcon(R.drawable.ae_icon_settings);
        setTitle("VirGL / Universal " + context.getString(R.string.configuration));

        final Spinner sGLVersion = findViewById(R.id.SVersion);
        final Spinner sDriverPackage = findViewById(R.id.SDriverPackage);
        final Spinner sGalliumDriver = findViewById(R.id.SGalliumDriver);
        final Button btInstallPackage = findViewById(R.id.BTVirglInstallPackage);
        final Button btDeletePackage = findViewById(R.id.BTVirglDeletePackage);
        final CheckBox cbDisableVertexArrayBGRA = findViewById(R.id.CBDisableVertexArrayBGRA);
        final CheckBox cbDisableGLKHRDebug = findViewById(R.id.CBDisableGLKHRDebug);
        final CheckBox cbVirglNoReadback = findViewById(R.id.CBVirglNoReadback);
        final EditText etExtraDisabledExtensions = findViewById(R.id.ETVirglExtraDisabledExtensions);
        final TextView tvMesaBridgeSummary = findViewById(R.id.TVMesaBridgeSummary);
        final TextView tvGalliumDriver = findViewById(R.id.TVGalliumDriver);
        final VirGLDriverPackageManager packageManager = new VirGLDriverPackageManager(context);

        Object tag = anchor.getTag();
        KeyValueSet config = new KeyValueSet(tag != null ? tag.toString() : "");
        String galliumDriver = normalizeGalliumDriver(config.get("galliumDriver", GraphicsDrivers.getVirglGalliumDriver()));

        updateMesaBridgeSummary(
                context,
                packageManager,
                tvMesaBridgeSummary,
                config.get("packageVersion", GraphicsDrivers.getBundledDriverAsset(context, GraphicsDrivers.VIRGL).version),
                galliumDriver
        );
        tvGalliumDriver.setVisibility(View.GONE);
        refreshPackageSpinner(context, packageManager, sDriverPackage, config.get("packageVersion", GraphicsDrivers.getBundledDriverAsset(context, GraphicsDrivers.VIRGL).version));
        AppUtils.setSpinnerSelectionFromIdentifier(sGalliumDriver, galliumDriver);
        AppUtils.setSpinnerSelectionFromIdentifier(sGLVersion, config.get("glVersion", DEFAULT_GL_VERSION));
        cbDisableVertexArrayBGRA.setChecked(config.getBoolean("disableVertexArrayBGRA", true));
        cbDisableGLKHRDebug.setChecked(config.getBoolean("disableGLKHRDebug", true));
        cbVirglNoReadback.setChecked(config.getBoolean("virglNoReadback", true));
        etExtraDisabledExtensions.setText(GraphicsDrivers.normalizeGraphicsExtensionList(config.get("extraDisabledExtensions", "")));
        sDriverPackage.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateMesaBridgeSummary(
                        context,
                        packageManager,
                        tvMesaBridgeSummary,
                        String.valueOf(parent.getItemAtPosition(position)),
                        normalizeGalliumDriver(StringUtils.parseIdentifier(sGalliumDriver.getSelectedItem()))
                );
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
        sGalliumDriver.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateMesaBridgeSummary(
                        context,
                        packageManager,
                        tvMesaBridgeSummary,
                        String.valueOf(sDriverPackage.getSelectedItem()),
                        normalizeGalliumDriver(StringUtils.parseIdentifier(parent.getItemAtPosition(position)))
                );
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        btInstallPackage.setOnClickListener((view) -> ContentDialog.confirm(context, context.getString(R.string.virgl_custom_driver_install_message), () -> {
            if (!(context instanceof Activity)) {
                AppUtils.showToast(context, R.string.virgl_custom_driver_install_failed);
                return;
            }
            MainActivity.setDriverPackagePickerCallback((uri) -> {
                String entryId = packageManager.installDriver(uri);
                if (entryId.isEmpty()) {
                    AppUtils.showToast(context, R.string.virgl_custom_driver_install_failed);
                    return;
                }
                String entry = VirGLDriverPackageManager.toCustomEntry(entryId);
                refreshPackageSpinner(context, packageManager, sDriverPackage, entry);
                AppUtils.showToast(context, context.getString(R.string.virgl_custom_driver_installed, entryId));
            });
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            ((Activity) context).startActivityForResult(intent, MainActivity.OPEN_DRIVER_PACKAGE_REQUEST_CODE);
        }));

        btDeletePackage.setOnClickListener((view) -> {
            String selectedPackage = String.valueOf(sDriverPackage.getSelectedItem());
            if (!VirGLDriverPackageManager.isCustomPackageEntry(selectedPackage)) {
                AppUtils.showToast(context, R.string.virgl_custom_driver_delete_builtin);
                return;
            }
            ContentDialog.confirm(context, context.getString(R.string.virgl_custom_driver_delete_message), () -> {
                if (packageManager.removeDriver(selectedPackage)) {
                    refreshPackageSpinner(context, packageManager, sDriverPackage, GraphicsDrivers.getBundledDriverAsset(context, GraphicsDrivers.VIRGL).version);
                    AppUtils.showToast(context, R.string.virgl_custom_driver_deleted);
                }
            });
        });

        setOnConfirmCallback(() -> {
            KeyValueSet newConfig = new KeyValueSet();
            newConfig.put("packageVersion", String.valueOf(sDriverPackage.getSelectedItem()));
            newConfig.put("galliumDriver", normalizeGalliumDriver(StringUtils.parseIdentifier(sGalliumDriver.getSelectedItem())));
            newConfig.put("glVersion", StringUtils.parseNumber(sGLVersion.getSelectedItem()));
            newConfig.put("disableVertexArrayBGRA", cbDisableVertexArrayBGRA.isChecked() ? "1" : "0");
            newConfig.put("disableGLKHRDebug", cbDisableGLKHRDebug.isChecked() ? "1" : "0");
            newConfig.put("virglNoReadback", cbVirglNoReadback.isChecked() ? "1" : "0");
            newConfig.put("extraDisabledExtensions", GraphicsDrivers.normalizeGraphicsExtensionList(etExtraDisabledExtensions.getText().toString()));
            anchor.setTag(newConfig.toString());
        });
    }

    private static void refreshPackageSpinner(Context context,
                                              VirGLDriverPackageManager packageManager,
                                              Spinner packageSpinner,
                                              String selectedPackage) {
        packageSpinner.setAdapter(SpinnerAdapters.create(context, packageManager.getSelectablePackageEntries()));
        AppUtils.setSpinnerSelectionFromValue(packageSpinner, selectedPackage);
    }

    private static void updateMesaBridgeSummary(Context context,
                                                VirGLDriverPackageManager packageManager,
                                                TextView summaryView,
                                                String selectedPackage,
                                                String galliumDriver) {
        String normalizedGallium = normalizeGalliumDriver(galliumDriver);
        StringBuilder summary = new StringBuilder(
                context.getString(R.string.virgl_bridge_summary, normalizedGallium.toUpperCase(Locale.US))
        );
        VirGLDriverPackageManager.PackageInfo info = packageManager.getPackageInfo(selectedPackage);
        if (info != null) {
            String label = info.getDisplayLabel();
            if (info.notes != null && !info.notes.trim().isEmpty()) label += "\n" + info.notes.trim();
            if (summary.length() > 0) summary.append('\n');
            summary.append(label);
            appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_driver_kind), info.driverKind);
            appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_transport), info.transport);
            appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_provider_lane), info.providerLane);
            appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_route), info.routeId);
            appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_owner_lane), info.ownerLane);
            appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_support), info.supportClass);
            appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_kernel_evidence), info.kernelEvidenceClass);
            appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_requirements), info.transportRequirements);
            appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_source_repo), info.sourceRepo);
            appendSummaryLine(
                    summary,
                    context.getString(R.string.graphics_driver_summary_render_node),
                    info.requiresRenderNode
                            ? context.getString(R.string.graphics_driver_render_node_required)
                            : context.getString(R.string.graphics_driver_render_node_not_required)
            );
            appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_diagnostic_keys), info.diagnosticKeys);
            appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_ranked_donors), info.rankedKernelDonors);
        } else {
            appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_provider_lane), "virgl-universal");
            appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_route), "virgl-universal-virtual-gpu");
            appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_support), "separate-transport");
        }
        summaryView.setText(summary.toString());
    }

    private static void appendSummaryLine(StringBuilder summary, String label, String value) {
        if (summary == null) return;
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) return;
        if (summary.length() > 0) summary.append('\n');
        summary.append(label).append(": ").append(normalized);
    }

    public static void setEnvVars(KeyValueSet config, EnvVars envVars) {
        String mesaExtensionOverride = GraphicsDrivers.buildMesaExtensionOverride(
                config.getBoolean("disableGLKHRDebug", true),
                config.getBoolean("disableVertexArrayBGRA", true),
                config.get("extraDisabledExtensions", "")
        );
        if (!mesaExtensionOverride.isEmpty()) envVars.put("MESA_EXTENSION_OVERRIDE", mesaExtensionOverride);
        envVars.put("GALLIUM_DRIVER", normalizeGalliumDriver(config.get("galliumDriver", DEFAULT_GALLIUM_DRIVER)));
        envVars.put("MESA_GL_VERSION_OVERRIDE", config.get("glVersion", DEFAULT_GL_VERSION));
        envVars.put("VIRGL_NO_READBACK", config.getBoolean("virglNoReadback", true) ? "true" : "false");
        envVars.put("AERO_VIRGL_GALLIUM_DRIVER", normalizeGalliumDriver(config.get("galliumDriver", DEFAULT_GALLIUM_DRIVER)));
    }

    public static String normalizeGalliumDriver(String requestedDriver) {
        String normalized = StringUtils.parseIdentifier(requestedDriver == null ? "" : requestedDriver);
        if ("llvmpipe".equals(normalized) || "softpipe".equals(normalized)) return normalized;
        return DEFAULT_GALLIUM_DRIVER;
    }
}
