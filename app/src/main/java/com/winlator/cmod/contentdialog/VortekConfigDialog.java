package com.winlator.cmod.contentdialog;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.AdapterView;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.winlator.cmod.MainActivity;
import com.winlator.cmod.R;
import com.winlator.cmod.container.GraphicsDrivers;
import com.winlator.cmod.contents.GladioOpenGLDriverPackageManager;
import com.winlator.cmod.contents.VortekVulkanDriverPackageManager;
import com.winlator.cmod.contents.VortekWrapperPackageManager;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.GPUHelper;
import com.winlator.cmod.core.KeyValueSet;
import com.winlator.cmod.core.SpinnerAdapters;
import com.winlator.cmod.core.StringUtils;
import com.winlator.cmod.core.VortekExtensionPolicy;
import com.winlator.cmod.xenvironment.components.VortekRendererComponent;
import com.winlator.cmod.widget.MultiSelectionComboBox;

public class VortekConfigDialog extends ContentDialog {
    private static final String DEFAULT_MALI_GL_VERSION = "3.1";
    private static final String DEFAULT_MALI_GALLIUM_DRIVER = "panfrost";
    public static final String DEFAULT_VK_MAX_VERSION =
            GPUHelper.vkVersionMajor(VortekRendererComponent.VK_MAX_VERSION)
                    + "."
                    + GPUHelper.vkVersionMinor(VortekRendererComponent.VK_MAX_VERSION);
    public static final String ROUTING_AUTO = "auto";
    public static final String ROUTING_VULKAN_FIRST = "vulkan-first";
    public static final String ROUTING_OPENGL_FIRST = "opengl-first";

    public VortekConfigDialog(final View anchor) {
        this(anchor, GraphicsDrivers.VORTEK);
    }

    public VortekConfigDialog(final View anchor, final String selectedGraphicsDriver) {
        super(anchor.getContext(), R.layout.vortek_config_dialog);
        Context context = anchor.getContext();
        String normalizedDriver = GraphicsDrivers.normalize(selectedGraphicsDriver);
        String migratedConfig = GraphicsDrivers.migrateToUnifiedTopLevelConfig(
                normalizedDriver,
                anchor.getTag() == null ? "" : anchor.getTag().toString()
        );
        anchor.setTag(migratedConfig);
        setIcon(R.drawable.ae_icon_settings);
        setTitle(buildTitle(context, normalizedDriver));

        final Spinner vortekVulkanDriverSpinner = findViewById(R.id.SVortekVulkanDriver);
        final Spinner vortekPackageSpinner = findViewById(R.id.SVortekPackage);
        final Spinner gladioPackageSpinner = findViewById(R.id.SGladioPackage);
        final TextView gladioGalliumDriverLabel = findViewById(R.id.TVGladioGalliumDriverLabel);
        final Spinner gladioGalliumDriverSpinner = findViewById(R.id.SGladioGalliumDriver);
        final TextView gladioOpenGlVersionLabel = findViewById(R.id.TVGladioOpenGlVersionLabel);
        final Spinner gladioOpenGlVersionSpinner = findViewById(R.id.SGladioOpenGlVersion);
        final View openGlPolicySection = findViewById(R.id.FLOpenGlPolicySection);
        final CheckBox cbGladioDisableVertexArrayBGRA = findViewById(R.id.CBGladioDisableVertexArrayBGRA);
        final CheckBox cbGladioDisableGLKHRDebug = findViewById(R.id.CBGladioDisableGLKHRDebug);
        final TextView gladioExtraDisabledExtensionsLabel = findViewById(R.id.TVGladioExtraDisabledExtensionsLabel);
        final EditText gladioExtraDisabledExtensionsEditText = findViewById(R.id.ETGladioExtraDisabledExtensions);
        final Button installVulkanDriverButton = findViewById(R.id.BTVortekInstallVulkanDriver);
        final Button deleteVulkanDriverButton = findViewById(R.id.BTVortekDeleteVulkanDriver);
        final Button installGladioPackageButton = findViewById(R.id.BTGladioInstallPackage);
        final Button deleteGladioPackageButton = findViewById(R.id.BTGladioDeletePackage);
        final Spinner mediaTekWrapperModeSpinner = findViewById(R.id.SMediaTekWrapperMode);
        final Spinner extensionProfileSpinner = findViewById(R.id.SVortekExtensionProfile);
        final Spinner vkMaxVersionSpinner = findViewById(R.id.SVkMaxVersion);
        final Spinner maxDeviceMemorySpinner = findViewById(R.id.SMaxDeviceMemory);
        final Spinner imageCacheSizeSpinner = findViewById(R.id.SImageCacheSize);
        final Spinner resourceMemoryTypeSpinner = findViewById(R.id.SResourceMemoryType);
        final CheckBox cbGladioNoError = findViewById(R.id.CBGladioNoError);
        final TextView vulkanDriverSummary = findViewById(R.id.TVVortekVulkanDriverSummary);
        final TextView vortekPackageSummary = findViewById(R.id.TVVortekPackageSummary);
        final TextView gladioPackageSummary = findViewById(R.id.TVGladioPackageSummary);
        final TextView extensionProfileSummary = findViewById(R.id.TVVortekExtensionProfileSummary);
        final MultiSelectionComboBox exposedExtensionsBox = findViewById(R.id.MSCBExposedExtensions);
        final VortekVulkanDriverPackageManager vulkanDriverPackageManager = new VortekVulkanDriverPackageManager(context);
        final VortekWrapperPackageManager vortekPackageManager = new VortekWrapperPackageManager(context);
        final GladioOpenGLDriverPackageManager gladioPackageManager = new GladioOpenGLDriverPackageManager(context);

        final String[] deviceExtensions = VortekExtensionPolicy.buildCandidateExtensions(GPUHelper.vkGetDeviceExtensions());
        exposedExtensionsBox.setPopupWindowWidth(360);
        exposedExtensionsBox.setDisplayText(context.getString(R.string.multiselection_combobox_display_text));
        exposedExtensionsBox.setItems(deviceExtensions);

        KeyValueSet config = new KeyValueSet(migratedConfig);
        String vulkanDriverEntry = VortekVulkanDriverPackageManager.normalizeEntry(
                config.get("vulkanDriverEntry", VortekVulkanDriverPackageManager.SYSTEM_ENTRY)
        );
        String extensionProfile = VortekExtensionPolicy.normalizeProfile(
                config.get("extensionProfile", VortekExtensionPolicy.PROFILE_MALI_SYSTEM)
        );
        String exposedDeviceExtensions = config.get("exposedDeviceExtensions", "");
        if (exposedDeviceExtensions.isEmpty()) {
            exposedExtensionsBox.setSelectedItems(VortekExtensionPolicy.getSelectedExtensionsForProfile(extensionProfile, deviceExtensions));
        } else if ("all".equals(exposedDeviceExtensions)) {
            exposedExtensionsBox.setSelectedItems(deviceExtensions);
        } else {
            extensionProfile = VortekExtensionPolicy.PROFILE_CUSTOM;
            exposedExtensionsBox.setSelectedItems(exposedDeviceExtensions.split("\\|"));
        }
        AppUtils.setSpinnerSelectionFromIdentifier(extensionProfileSpinner, extensionProfile);
        extensionProfileSummary.setText(VortekExtensionPolicy.describeProfile(extensionProfile));
        refreshVulkanDriverSpinner(context, vulkanDriverPackageManager, vortekVulkanDriverSpinner, vulkanDriverEntry);
        updateVulkanDriverSummary(context, vulkanDriverPackageManager, vulkanDriverSummary, vulkanDriverEntry);
        vortekVulkanDriverSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedEntry = String.valueOf(parent.getItemAtPosition(position));
                updateVulkanDriverSummary(context, vulkanDriverPackageManager, vulkanDriverSummary, selectedEntry);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        extensionProfileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String profile = VortekExtensionPolicy.normalizeProfile(String.valueOf(parent.getItemAtPosition(position)));
                extensionProfileSummary.setText(VortekExtensionPolicy.describeProfile(profile));
                if (!VortekExtensionPolicy.PROFILE_CUSTOM.equals(profile)) {
                    exposedExtensionsBox.setSelectedItems(VortekExtensionPolicy.getSelectedExtensionsForProfile(profile, deviceExtensions));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        String selectedVortekPackage = config.get(
                "vortekPackageVersion",
                GraphicsDrivers.getBundledDriverAsset(context, GraphicsDrivers.VORTEK).version
        );
        refreshVortekPackageSpinner(context, vortekPackageManager, vortekPackageSpinner, selectedVortekPackage);
        updateVortekPackageSummary(context, vortekPackageManager, vortekPackageSummary, selectedVortekPackage);
        vortekPackageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateVortekPackageSummary(
                        context,
                        vortekPackageManager,
                        vortekPackageSummary,
                        String.valueOf(parent.getItemAtPosition(position))
                );
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        String selectedGladioPackage = config.get(
                "gladioPackageVersion",
                GraphicsDrivers.getBundledDriverAsset(context, GraphicsDrivers.GLADIO).version
        );
        refreshGladioPackageSpinner(context, gladioPackageManager, gladioPackageSpinner, selectedGladioPackage);
        updateGladioPackageSummary(context, gladioPackageManager, gladioPackageSummary, selectedGladioPackage);
        gladioGalliumDriverSpinner.setAdapter(
                SpinnerAdapters.create(context, context.getResources().getStringArray(R.array.aemali_gallium_driver_entries))
        );
        AppUtils.setSpinnerSelectionFromIdentifier(
                gladioGalliumDriverSpinner,
                normalizeMaliGalliumDriver(config.get("galliumDriver", DEFAULT_MALI_GALLIUM_DRIVER))
        );
        AppUtils.setSpinnerSelectionFromIdentifier(
                gladioOpenGlVersionSpinner,
                StringUtils.parseNumber(config.get("glVersion", DEFAULT_MALI_GL_VERSION), DEFAULT_MALI_GL_VERSION)
        );
        cbGladioDisableVertexArrayBGRA.setChecked(config.getBoolean("disableVertexArrayBGRA", true));
        cbGladioDisableGLKHRDebug.setChecked(config.getBoolean("disableGLKHRDebug", true));
        gladioExtraDisabledExtensionsEditText.setText(
                GraphicsDrivers.normalizeGraphicsExtensionList(config.get("extraDisabledExtensions", ""))
        );
        updateAeMaliOpenGlControls(
                openGlPolicySection,
                gladioPackageSpinner,
                gladioGalliumDriverLabel,
                gladioGalliumDriverSpinner,
                gladioOpenGlVersionLabel,
                gladioOpenGlVersionSpinner,
                cbGladioDisableVertexArrayBGRA,
                cbGladioDisableGLKHRDebug,
                gladioExtraDisabledExtensionsLabel,
                gladioExtraDisabledExtensionsEditText
        );
        gladioPackageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateAeMaliOpenGlControls(
                        openGlPolicySection,
                        gladioPackageSpinner,
                        gladioGalliumDriverLabel,
                        gladioGalliumDriverSpinner,
                        gladioOpenGlVersionLabel,
                        gladioOpenGlVersionSpinner,
                        cbGladioDisableVertexArrayBGRA,
                        cbGladioDisableGLKHRDebug,
                        gladioExtraDisabledExtensionsLabel,
                        gladioExtraDisabledExtensionsEditText
                );
                updateGladioPackageSummary(
                        context,
                        gladioPackageManager,
                        gladioPackageSummary,
                        String.valueOf(parent.getItemAtPosition(position))
                );
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        AppUtils.setSpinnerSelectionFromIdentifier(mediaTekWrapperModeSpinner, normalizeRoutingMode(config.get("routingMode", ROUTING_AUTO)));

        AppUtils.setSpinnerSelectionFromValue(vkMaxVersionSpinner, config.get("vkMaxVersion", DEFAULT_VK_MAX_VERSION));
        AppUtils.setSpinnerSelectionFromMemorySize(maxDeviceMemorySpinner, config.get("maxDeviceMemory", "0"));
        AppUtils.setSpinnerSelectionFromNumber(imageCacheSizeSpinner, config.get("imageCacheSize", String.valueOf(VortekRendererComponent.IMAGE_CACHE_SIZE)));
        resourceMemoryTypeSpinner.setSelection(config.getInt("resourceMemoryType"));
        cbGladioNoError.setChecked(config.getBoolean("gladioNoError", true));

        installVulkanDriverButton.setOnClickListener((view) -> ContentDialog.confirm(context, context.getString(R.string.vortek_custom_vulkan_driver_install_message), () -> {
            if (!(context instanceof Activity)) {
                AppUtils.showToast(context, R.string.vortek_custom_vulkan_driver_install_failed);
                return;
            }
            MainActivity.setDriverPackagePickerCallback((uri) -> {
                String entryId = vulkanDriverPackageManager.installDriver(uri);
                if (entryId.isEmpty()) {
                    AppUtils.showToast(context, R.string.vortek_custom_vulkan_driver_install_failed);
                    return;
                }
                String entry = VortekVulkanDriverPackageManager.toCustomEntry(entryId);
                refreshVulkanDriverSpinner(context, vulkanDriverPackageManager, vortekVulkanDriverSpinner, entry);
                updateVulkanDriverSummary(context, vulkanDriverPackageManager, vulkanDriverSummary, entry);
                AppUtils.showToast(context, context.getString(R.string.vortek_custom_vulkan_driver_installed, entryId));
            });
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            ((Activity) context).startActivityForResult(intent, MainActivity.OPEN_DRIVER_PACKAGE_REQUEST_CODE);
        }));

        deleteVulkanDriverButton.setOnClickListener((view) -> {
            String selectedEntry = String.valueOf(vortekVulkanDriverSpinner.getSelectedItem());
            if (!VortekVulkanDriverPackageManager.isCustomPackageEntry(selectedEntry)) {
                AppUtils.showToast(context, R.string.vortek_custom_vulkan_driver_delete_builtin);
                return;
            }
            ContentDialog.confirm(context, context.getString(R.string.vortek_custom_vulkan_driver_delete_message), () -> {
                if (vulkanDriverPackageManager.removeDriver(selectedEntry)) {
                    refreshVulkanDriverSpinner(
                            context,
                            vulkanDriverPackageManager,
                            vortekVulkanDriverSpinner,
                            VortekVulkanDriverPackageManager.SYSTEM_ENTRY
                    );
                    updateVulkanDriverSummary(
                            context,
                            vulkanDriverPackageManager,
                            vulkanDriverSummary,
                            VortekVulkanDriverPackageManager.SYSTEM_ENTRY
                    );
                    AppUtils.showToast(context, R.string.vortek_custom_vulkan_driver_deleted);
                }
            });
        });

        installGladioPackageButton.setOnClickListener((view) -> ContentDialog.confirm(context, context.getString(R.string.gladio_custom_driver_install_message), () -> {
            if (!(context instanceof Activity)) {
                AppUtils.showToast(context, R.string.gladio_custom_driver_install_failed);
                return;
            }
            MainActivity.setDriverPackagePickerCallback((uri) -> {
                String entryId = gladioPackageManager.installDriver(uri);
                if (entryId.isEmpty()) {
                    AppUtils.showToast(context, R.string.gladio_custom_driver_install_failed);
                    return;
                }
                String entry = GladioOpenGLDriverPackageManager.toCustomEntry(entryId);
                refreshGladioPackageSpinner(context, gladioPackageManager, gladioPackageSpinner, entry);
                updateGladioPackageSummary(context, gladioPackageManager, gladioPackageSummary, entry);
                AppUtils.showToast(context, context.getString(R.string.gladio_custom_driver_installed, entryId));
            });
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            ((Activity) context).startActivityForResult(intent, MainActivity.OPEN_DRIVER_PACKAGE_REQUEST_CODE);
        }));

        deleteGladioPackageButton.setOnClickListener((view) -> {
            String selectedEntry = String.valueOf(gladioPackageSpinner.getSelectedItem());
            if (!GladioOpenGLDriverPackageManager.isManagedCustomPackageEntry(selectedEntry)) {
                AppUtils.showToast(context, R.string.gladio_custom_driver_delete_builtin);
                return;
            }
            ContentDialog.confirm(context, context.getString(R.string.gladio_custom_driver_delete_message), () -> {
                if (gladioPackageManager.removeDriver(selectedEntry)) {
                    String bundledVersion = GraphicsDrivers.getBundledDriverAsset(context, GraphicsDrivers.GLADIO).version;
                    refreshGladioPackageSpinner(context, gladioPackageManager, gladioPackageSpinner, bundledVersion);
                    updateGladioPackageSummary(context, gladioPackageManager, gladioPackageSummary, bundledVersion);
                    AppUtils.showToast(context, R.string.gladio_custom_driver_deleted);
                }
            });
        });

        setOnConfirmCallback(() -> {
            KeyValueSet newConfig = new KeyValueSet();
            newConfig.put("vulkanDriverEntry", VortekVulkanDriverPackageManager.normalizeEntry(String.valueOf(vortekVulkanDriverSpinner.getSelectedItem())));
            newConfig.put("vortekPackageVersion", String.valueOf(vortekPackageSpinner.getSelectedItem()));
            newConfig.put("gladioPackageVersion", String.valueOf(gladioPackageSpinner.getSelectedItem()));
            newConfig.put("routingMode", normalizeRoutingMode(StringUtils.parseIdentifier(mediaTekWrapperModeSpinner.getSelectedItem())));
            if (isAeMaliOpenGlEntry(String.valueOf(gladioPackageSpinner.getSelectedItem()))) {
                newConfig.put(
                        "galliumDriver",
                        normalizeMaliGalliumDriver(StringUtils.parseIdentifier(gladioGalliumDriverSpinner.getSelectedItem()))
                );
                newConfig.put("glVersion", StringUtils.parseNumber(gladioOpenGlVersionSpinner.getSelectedItem(), DEFAULT_MALI_GL_VERSION));
                newConfig.put("disableVertexArrayBGRA", cbGladioDisableVertexArrayBGRA.isChecked() ? "1" : "0");
                newConfig.put("disableGLKHRDebug", cbGladioDisableGLKHRDebug.isChecked() ? "1" : "0");
                newConfig.put(
                        "extraDisabledExtensions",
                        GraphicsDrivers.normalizeGraphicsExtensionList(gladioExtraDisabledExtensionsEditText.getText().toString())
                );
            }
            newConfig.put("vkMaxVersion", StringUtils.parseNumber(vkMaxVersionSpinner.getSelectedItem(), "0"));
            newConfig.put("maxDeviceMemory", StringUtils.parseMemorySize(maxDeviceMemorySpinner.getSelectedItem()));
            newConfig.put("imageCacheSize", StringUtils.parseNumber(imageCacheSizeSpinner.getSelectedItem()));
            newConfig.put("resourceMemoryType", resourceMemoryTypeSpinner.getSelectedItemPosition());
            newConfig.put("gladioNoError", cbGladioNoError.isChecked() ? "1" : "0");

            String profile = VortekExtensionPolicy.normalizeProfile(String.valueOf(extensionProfileSpinner.getSelectedItem()));
            newConfig.put("extensionProfile", profile);
            String disabledExtensions = VortekExtensionPolicy.joinExtensions(VortekExtensionPolicy.getDisabledExtensionsForProfile(profile));
            if (!disabledExtensions.isEmpty()) newConfig.put("disabledDeviceExtensions", disabledExtensions);

            String[] selectedItems = exposedExtensionsBox.getSelectedItems();
            if (selectedItems.length > 0) {
                newConfig.put("exposedDeviceExtensions", VortekExtensionPolicy.joinExtensions(selectedItems));
            }
            anchor.setTag(newConfig.toString());
        });
    }

    private static String buildTitle(Context context, String graphicsDriver) {
        if (GraphicsDrivers.isGladio(graphicsDriver)) {
            return "MediaTek OpenGL " + context.getString(R.string.configuration);
        }
        return "MediaTek Wrapper " + context.getString(R.string.configuration);
    }

    public static String normalizeRoutingMode(String routingMode) {
        String normalized = StringUtils.parseIdentifier(routingMode == null ? "" : routingMode);
        if (ROUTING_VULKAN_FIRST.equals(normalized) || ROUTING_OPENGL_FIRST.equals(normalized)) return normalized;
        return ROUTING_AUTO;
    }

    public static boolean isRequireRestart(String oldGraphicsDriverConfig, String newGraphicsDriverConfig) {
        return GraphicsDrivers.isMediaTekConfigRestartRequired(oldGraphicsDriverConfig, newGraphicsDriverConfig);
    }

    private static void refreshVulkanDriverSpinner(Context context,
                                                   VortekVulkanDriverPackageManager packageManager,
                                                   Spinner spinner,
                                                   String selectedEntry) {
        spinner.setAdapter(SpinnerAdapters.create(context, packageManager.getSelectableDriverEntries()));
        AppUtils.setSpinnerSelectionFromValue(spinner, VortekVulkanDriverPackageManager.normalizeEntry(selectedEntry));
    }

    private static void refreshGladioPackageSpinner(Context context,
                                                    GladioOpenGLDriverPackageManager packageManager,
                                                    Spinner spinner,
                                                    String selectedEntry) {
        spinner.setAdapter(SpinnerAdapters.create(context, packageManager.getSelectablePackageEntries()));
        AppUtils.setSpinnerSelectionFromValue(spinner, selectedEntry);
    }

    private static void updateAeMaliOpenGlControls(View openGlPolicySection,
                                                   Spinner gladioPackageSpinner,
                                                   TextView gladioGalliumDriverLabel,
                                                   Spinner gladioGalliumDriverSpinner,
                                                   TextView gladioOpenGlVersionLabel,
                                                   Spinner gladioOpenGlVersionSpinner,
                                                   CheckBox cbGladioDisableVertexArrayBGRA,
                                                   CheckBox cbGladioDisableGLKHRDebug,
                                                   TextView gladioExtraDisabledExtensionsLabel,
                                                   EditText gladioExtraDisabledExtensionsEditText) {
        boolean visible = isAeMaliOpenGlEntry(String.valueOf(gladioPackageSpinner.getSelectedItem()));
        int visibility = visible ? View.VISIBLE : View.GONE;
        if (openGlPolicySection != null) openGlPolicySection.setVisibility(visibility);
        gladioGalliumDriverLabel.setVisibility(visibility);
        gladioGalliumDriverSpinner.setVisibility(visibility);
        gladioOpenGlVersionLabel.setVisibility(visibility);
        gladioOpenGlVersionSpinner.setVisibility(visibility);
        cbGladioDisableVertexArrayBGRA.setVisibility(visibility);
        cbGladioDisableGLKHRDebug.setVisibility(visibility);
        gladioExtraDisabledExtensionsLabel.setVisibility(visibility);
        gladioExtraDisabledExtensionsEditText.setVisibility(visibility);
    }

    private static void refreshVortekPackageSpinner(Context context,
                                                    VortekWrapperPackageManager packageManager,
                                                    Spinner spinner,
                                                    String selectedEntry) {
        spinner.setAdapter(SpinnerAdapters.create(context, packageManager.getSelectablePackageEntries()));
        AppUtils.setSpinnerSelectionFromValue(spinner, selectedEntry);
    }

    private static void updateVulkanDriverSummary(Context context,
                                                  VortekVulkanDriverPackageManager packageManager,
                                                  TextView summaryView,
                                                  String selectedEntry) {
        VortekVulkanDriverPackageManager.PackageInfo info = packageManager.getPackageInfo(selectedEntry);
        if (info == null) {
            summaryView.setText("");
            return;
        }

        String kind = describeSummaryValue(info.driverKind, "android-hal");
        String transport = describeSummaryValue(info.transport, info.builtin ? "android-hal" : "userspace-wrapper");
        summaryView.setText(buildCompactPackageSummary(
                context,
                info.getDisplayLabel(),
                info.notes,
                kind,
                transport,
                info.providerLane,
                info.routeId,
                info.supportClass,
                info.sourceRepo,
                info.vulkanApiCeiling,
                "",
                "",
                info.requiresRenderNode,
                info.experimental
        ));
    }

    private static String describeSummaryValue(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static void updateVortekPackageSummary(Context context,
                                                   VortekWrapperPackageManager packageManager,
                                                   TextView summaryView,
                                                   String selectedEntry) {
        VortekWrapperPackageManager.PackageInfo info = packageManager.getPackageInfo(selectedEntry);
        if (info == null) {
            GraphicsDrivers.BundledDriverAsset asset = GraphicsDrivers.getBundledDriverAsset(
                    context,
                    GraphicsDrivers.VORTEK,
                    selectedEntry
            );
            summaryView.setText(buildCompactPackageSummary(
                    context,
                    asset.packageLabel + " " + asset.version,
                    "",
                    "vortek-wrapper",
                    "bundled-root-overlay",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    false,
                    false
            ));
            return;
        }

        summaryView.setText(buildCompactPackageSummary(
                context,
                info.getDisplayLabel(),
                info.notes,
                describeSummaryValue(info.driverKind, "vortek-wrapper"),
                describeSummaryValue(info.transport, "bundled-root-overlay"),
                info.providerLane,
                info.routeId,
                info.supportClass,
                info.sourceRepo,
                "",
                "",
                info.graphicsStackProfile,
                info.requiresRenderNode,
                false
        ));
    }

    private static void updateGladioPackageSummary(Context context,
                                                   GladioOpenGLDriverPackageManager packageManager,
                                                   TextView summaryView,
                                                   String selectedEntry) {
        GladioOpenGLDriverPackageManager.PackageInfo info = packageManager.getPackageInfo(selectedEntry);
        if (info == null) {
            GraphicsDrivers.BundledDriverAsset asset = GraphicsDrivers.getBundledDriverAsset(
                    context,
                    GraphicsDrivers.GLADIO,
                    selectedEntry
            );
            summaryView.setText(buildCompactPackageSummary(
                    context,
                    asset.packageLabel + " " + asset.version,
                    "",
                    "opengl-wrapper",
                    "bundled-root-overlay",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    false,
                    false
            ));
            return;
        }

        summaryView.setText(buildCompactPackageSummary(
                context,
                info.getDisplayLabel(),
                info.notes,
                describeSummaryValue(info.driverKind, "opengl-wrapper"),
                describeSummaryValue(info.transport, "root-overlay"),
                info.providerLane,
                info.routeId,
                info.supportClass,
                info.sourceRepo,
                "",
                info.preferredGalliumDriver,
                info.graphicsStackProfile,
                info.requiresRenderNode,
                false
        ));
    }

    private static String buildCompactPackageSummary(Context context,
                                                     String label,
                                                     String notes,
                                                     String driverKind,
                                                     String transport,
                                                     String providerLane,
                                                     String routeId,
                                                     String supportClass,
                                                     String sourceRepo,
                                                     String vulkanApiCeiling,
                                                     String preferredGalliumDriver,
                                                     String graphicsStackProfile,
                                                     boolean requiresRenderNode,
                                                     boolean experimental) {
        StringBuilder summary = new StringBuilder();
        appendSummaryParagraph(summary, compactSummaryText(label, 120));
        appendSummaryParagraph(summary, compactSummaryNote(notes));
        appendSummaryLine(
                summary,
                context.getString(R.string.graphics_driver_summary_driver_kind),
                joinSummaryParts(
                        " • ",
                        compactSummaryValue(driverKind, 1, 28),
                        compactSummaryValue(transport, 1, 28)
                )
        );
        appendSummaryLine(
                summary,
                context.getString(R.string.graphics_driver_summary_route),
                joinSummaryParts(
                        " • ",
                        compactSummaryValue(providerLane, 1, 32),
                        compactSummaryValue(routeId, 1, 32)
                )
        );
        appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_source_repo), compactSourceRepo(sourceRepo));
        appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_vulkan_ceiling), compactSummaryValue(vulkanApiCeiling, 1, 32));
        appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_preferred_gallium), compactSummaryValue(preferredGalliumDriver, 2, 48));
        appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_stack_profile), compactSummaryValue(graphicsStackProfile, 1, 32));
        if (requiresRenderNode) {
            appendSummaryLine(
                    summary,
                    context.getString(R.string.graphics_driver_summary_render_node),
                    context.getString(R.string.graphics_driver_render_node_required)
            );
        }
        if (experimental) {
            appendSummaryLine(
                    summary,
                    context.getString(R.string.graphics_driver_summary_experimental),
                    context.getString(R.string.graphics_driver_yes)
            );
        }
        return summary.toString();
    }

    private static String compactSummaryNote(String value) {
        if (value == null) return "";
        String normalized = value.replace('\n', ' ').trim();
        if (normalized.isEmpty()) return "";
        int pipeIndex = normalized.indexOf('|');
        if (pipeIndex > 0) normalized = normalized.substring(0, pipeIndex).trim();
        int sentenceIndex = normalized.indexOf(". ");
        if (sentenceIndex > 0) normalized = normalized.substring(0, sentenceIndex + 1).trim();
        return compactSummaryText(normalized, 140);
    }

    private static String compactSourceRepo(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) return "";
        normalized = normalized.replace("https://", "").replace("http://", "");
        int slashIndex = normalized.indexOf('/');
        if (slashIndex > 0) {
            String host = normalized.substring(0, slashIndex);
            String path = normalized.substring(slashIndex + 1);
            return compactSummaryText(host + "/" + compactSummaryText(path, 36), 56);
        }
        return compactSummaryText(normalized, 56);
    }

    private static String compactSummaryValue(String value, int maxItems, int maxChars) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) return "";
        normalized = normalized.replace('\n', ',').replace('|', ',');
        String[] rawItems = normalized.split("\\s*,\\s*");
        StringBuilder builder = new StringBuilder();
        int appended = 0;
        int total = 0;
        for (String rawItem : rawItems) {
            String item = rawItem == null ? "" : rawItem.trim();
            if (item.isEmpty()) continue;
            total++;
            if (appended >= maxItems) continue;
            if (builder.length() > 0) builder.append(", ");
            builder.append(compactSummaryText(item, Math.max(12, maxChars / Math.max(1, maxItems))));
            appended++;
        }
        if (builder.length() == 0) {
            return compactSummaryText(normalized, maxChars);
        }
        if (total > appended) builder.append(" +").append(total - appended);
        return compactSummaryText(builder.toString(), maxChars);
    }

    private static String joinSummaryParts(String separator, String... values) {
        if (values == null || values.length == 0) return "";
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            String normalized = value == null ? "" : value.trim();
            if (normalized.isEmpty()) continue;
            if (builder.length() > 0) builder.append(separator);
            builder.append(normalized);
        }
        return builder.toString();
    }

    private static String compactSummaryText(String value, int maxChars) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= maxChars) return normalized;
        if (maxChars <= 1) return normalized.substring(0, 1);
        return normalized.substring(0, Math.max(0, maxChars - 1)).trim() + "…";
    }

    private static void appendSummaryParagraph(StringBuilder summary, String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) return;
        if (summary.length() > 0) summary.append('\n');
        summary.append(normalized);
    }

    private static String appendMetadataSummary(Context context,
                                                String base,
                                                String providerLane,
                                                String routeId,
                                                String ownerLane,
                                                String supportClass,
                                                String kernelEvidenceClass,
                                                String transportRequirements,
                                                String sourceRepo,
                                                String vulkanApiCeiling,
                                                String preferredGalliumDriver,
                                                String graphicsStackProfile,
                                                String rankedKernelDonors,
                                                String diagnosticKeys,
                                                boolean requiresRenderNode,
                                                boolean experimental) {
        StringBuilder summary = new StringBuilder(base == null ? "" : base);
        appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_provider_lane), providerLane);
        appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_route), routeId);
        appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_owner_lane), ownerLane);
        appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_support), supportClass);
        appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_kernel_evidence), kernelEvidenceClass);
        appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_requirements), transportRequirements);
        appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_source_repo), sourceRepo);
        appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_vulkan_ceiling), vulkanApiCeiling);
        appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_preferred_gallium), preferredGalliumDriver);
        appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_stack_profile), graphicsStackProfile);
        appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_ranked_donors), rankedKernelDonors);
        appendSummaryLine(summary, context.getString(R.string.graphics_driver_summary_diagnostic_keys), diagnosticKeys);
        appendSummaryLine(
                summary,
                context.getString(R.string.graphics_driver_summary_render_node),
                requiresRenderNode
                        ? context.getString(R.string.graphics_driver_render_node_required)
                        : context.getString(R.string.graphics_driver_render_node_not_required)
        );
        appendSummaryLine(
                summary,
                context.getString(R.string.graphics_driver_summary_experimental),
                experimental ? context.getString(R.string.graphics_driver_yes) : ""
        );
        return summary.toString();
    }

    private static boolean isAeMaliOpenGlEntry(String selectedEntry) {
        return GladioOpenGLDriverPackageManager.isAeMaliPackageEntry(selectedEntry);
    }

    private static String normalizeMaliGalliumDriver(String requestedDriver) {
        String normalized = StringUtils.parseIdentifier(requestedDriver == null ? "" : requestedDriver);
        if ("panfrost".equals(normalized) || "lima".equals(normalized) || "zink".equals(normalized) || "softpipe".equals(normalized)) {
            return normalized;
        }
        return DEFAULT_MALI_GALLIUM_DRIVER;
    }

    private static void appendSummaryLine(StringBuilder summary, String label, String value) {
        if (summary == null) return;
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) return;
        if (summary.length() > 0) summary.append('\n');
        summary.append(label).append(": ").append(normalized);
    }
}
