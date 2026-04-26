package com.winlator.cmod.xenvironment.components;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.winlator.cmod.container.Shortcut;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.core.EnvVars;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.xenvironment.ImageFs;

import java.io.File;

public class BionicProgramLauncherComponent extends GuestProgramLauncherComponent {
    public BionicProgramLauncherComponent(ContentsManager contentsManager, ContentProfile wineProfile, Shortcut shortcut) {
        super(contentsManager, wineProfile, shortcut);
    }

    @Override
    protected String getLauncherModel(ImageFs imageFs) {
        return "bionic";
    }

    @Override
    protected void applyLauncherSpecificEnvVars(Context context, ImageFs imageFs, File rootDir, EnvVars launchEnv) {
        super.applyLauncherSpecificEnvVars(context, imageFs, rootDir, launchEnv);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        launchEnv.put("AERO_RUNTIME_EXECUTION_MODEL", "android_bionic_guest");
        launchEnv.put("AERO_RUNTIME_ANDROID_BIONIC_ONLY", "1");
        launchEnv.put("AERO_RUNTIME_REDIRECT_MODE", "host_closure_preload_with_rootfs_socket_redirect");
        launchEnv.put("WINE_OPEN_WITH_ANDROID_BROWSER", "1");
        launchEnv.put("NODEVICE_SELECT", "1");
        launchEnv.put("DISABLE_BCN_COMPUTE", "1");
        applyBionicVulkanLayerContract(context, imageFs, rootDir, launchEnv);
        applyBionicX11OpenGlBackendContract(context, imageFs, launchEnv, "bionic_guest");
        File androidHostLibDir = imageFs.getAndroidHostLibDir();
        if (androidHostLibDir.isDirectory()) {
            String currentLdLibraryPath = launchEnv.get("LD_LIBRARY_PATH");
            StringBuilder ldLibraryPath = new StringBuilder();
            if (currentLdLibraryPath != null && !currentLdLibraryPath.trim().isEmpty()) {
                ldLibraryPath.append(currentLdLibraryPath.trim());
            }
            if (ldLibraryPath.length() > 0) {
                ldLibraryPath.append(':');
            }
            // Keep the guest unix-side closure ahead of android-host to avoid
            // shadowing winex11/winevulkan dependencies with host overlay copies.
            ldLibraryPath.append(androidHostLibDir.getPath());
            launchEnv.put("LD_LIBRARY_PATH", ldLibraryPath.toString());
            ForensicLogger.logEvent(
                    context,
                    "info",
                    "BIONIC_HOST_LIBPATH_ORDER_APPLIED",
                    null,
                    "guest_program_launcher",
                    "bionic_host_library_path_order_applied",
                    ForensicLogger.fields(
                            "mode", "guest_first_append_host_tail",
                            "host_lib_dir", androidHostLibDir.getPath(),
                            "ld_library_path_head", summarizePathHead(ldLibraryPath.toString(), 4)
                    )
            );
        }
        if (preferences.getBoolean("enable_peb_logs", false)) {
            launchEnv.put("WINE_LOG_PEB_DATA", "1");
        }

        StringBuilder ldPreload = new StringBuilder();
        appendExistingLdPreload(ldPreload, launchEnv);
        appendAndroidHostClosureLdPreload(ldPreload, imageFs, true);
        File evshimPath = resolveEvshimLibrary(imageFs);
        appendFileIfExists(ldPreload, evshimPath);
        if (ldPreload.length() > 0) {
            launchEnv.put("LD_PRELOAD", ldPreload.toString());
        }

        ForensicLogger.logEvent(
                context,
                "info",
                "BIONIC_HOST_PRELOAD_CONTRACT",
                null,
                "guest_program_launcher",
                "bionic_host_preload_contract",
                ForensicLogger.fields(
                        "evshim_present", evshimPath != null && evshimPath.isFile(),
                        "evshim_path", evshimPath != null ? evshimPath.getAbsolutePath() : "",
                        "ld_preload", launchEnv.get("LD_PRELOAD")
                )
        );

        if (evshimPath != null && evshimPath.isFile()) {
            launchEnv.put("EVSHIM_MAX_PLAYERS", "1");
            launchEnv.put("EVSHIM_SHM_ID", "1");
            launchEnv.put("EVSHIM_SHM_NAME", "controller-shm0");
        }
    }

    @Override
    protected boolean usesAndroidBionicHostEnv(String effectiveEmulator, boolean desktopShellBootstrap) {
        return shouldUseDirectArm64EcGuestLaunch(environment.getImageFs(), effectiveEmulator, desktopShellBootstrap);
    }

    @Override
    protected void applyAndroidBionicHostEnv(Context context, ImageFs imageFs, File rootDir, EnvVars launchEnv) {
        String effectiveEmulator = resolveEffectiveArm64EcEmulator();
        launchEnv.put(
                "AERO_RUNTIME_EXECUTION_MODEL",
                "fexcore".equalsIgnoreCase(effectiveEmulator)
                        ? "android_bionic_fex_guest"
                        : "android_bionic_wowbox64_guest"
        );
        launchEnv.put("AERO_RUNTIME_ANDROID_BIONIC_ONLY", "1");
        launchEnv.put("AERO_RUNTIME_REDIRECT_MODE", "host_closure_preload_with_rootfs_socket_redirect");
        applyAndroidBionicHostLdLibraryPath(context, imageFs, launchEnv, "bionic_direct_arm64ec");
        applyBionicVulkanLayerContract(context, imageFs, rootDir, launchEnv);
        applyBionicX11OpenGlBackendContract(context, imageFs, launchEnv, "bionic_direct_arm64ec");

        StringBuilder ldPreload = new StringBuilder();
        appendAndroidHostClosureLdPreload(ldPreload, imageFs, true);
        if (ldPreload.length() > 0) {
            launchEnv.put("LD_PRELOAD", ldPreload.toString());
        } else {
            launchEnv.remove("LD_PRELOAD");
        }
    }

    private void applyBionicX11OpenGlBackendContract(Context context, ImageFs imageFs, EnvVars launchEnv, String owner) {
        if (launchEnv == null) return;
        launchEnv.put("WINE_X11FORCEGLX", "1");
        launchEnv.put("WINE_USE_EGL", "0");
        File eglCompatDir = resolveWineX11EglCompatDir(imageFs);
        boolean eglCompatReady = eglCompatDir != null
                && new File(eglCompatDir, "libEGL.so").isFile()
                && new File(eglCompatDir, "libEGL.so.1").isFile();
        String eglCompatPath = eglCompatDir != null ? eglCompatDir.getPath() : "";
        launchEnv.put("AERO_WINE_X11_EGL_COMPAT_DIR", eglCompatReady ? eglCompatPath : "");
        launchEnv.put("AERO_WINE_X11_EGL_STUB_GLOBAL_LD", "0");

        ForensicLogger.logEvent(
                context,
                "info",
                "BIONIC_X11_OPENGL_BACKEND_CONTRACT_APPLIED",
                null,
                "guest_program_launcher",
                "bionic_x11_opengl_backend_contract_applied",
                ForensicLogger.fields(
                        "owner", owner,
                        "wine_x11forceglx", launchEnv.get("WINE_X11FORCEGLX"),
                        "wine_use_egl", launchEnv.get("WINE_USE_EGL"),
                        "registry_key", "HKCU\\Software\\Wine\\X11 Driver",
                        "use_egl", "N",
                        "backend", "x11_glx_preferred_without_global_egl_stub",
                        "egl_compat_dir", eglCompatPath,
                        "egl_compat_ready", eglCompatReady,
                        "egl_stub_global_ld", false,
                        "contains_egl_stub_global_ld", containsLdLibraryPathSegment(launchEnv.get("LD_LIBRARY_PATH"), eglCompatPath),
                        "ld_library_path_head", summarizePathHead(launchEnv.get("LD_LIBRARY_PATH"), 4)
                )
        );
    }

    private File resolveWineX11EglCompatDir(ImageFs imageFs) {
        if (imageFs == null) return null;
        return new File(imageFs.getAndroidHostLibDir(), "wine-x11-egl-stub");
    }

    private static boolean containsLdLibraryPathSegment(String ldLibraryPath, String path) {
        if (ldLibraryPath == null || ldLibraryPath.trim().isEmpty()) return false;
        if (path == null || path.trim().isEmpty()) return false;
        String normalizedPath = normalizePath(path);
        if (normalizedPath.isEmpty()) return false;
        for (String part : ldLibraryPath.split(":")) {
            if (normalizedPath.equals(normalizePath(part))) return true;
        }
        return false;
    }

    private static String normalizePath(String path) {
        if (path == null) return "";
        String normalized = path.trim();
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private void applyBionicVulkanLayerContract(Context context, ImageFs imageFs, File rootDir, EnvVars launchEnv) {
        if (rootDir == null || launchEnv == null) return;
        File explicitLayerDir = new File(rootDir, "usr/share/vulkan/explicit_layer.d");
        File emptyImplicitLayerDir = imageFs != null
                ? new File(imageFs.getTmpDir(), "vulkan-empty-implicit-layer.d")
                : new File(rootDir, "tmp/vulkan-empty-implicit-layer.d");
        if (!emptyImplicitLayerDir.isDirectory()) emptyImplicitLayerDir.mkdirs();

        launchEnv.put("VK_LAYER_PATH", explicitLayerDir.getPath());
        launchEnv.put("VK_IMPLICIT_LAYER_PATH", emptyImplicitLayerDir.getPath());
        launchEnv.remove("VK_ADD_LAYER_PATH");
        launchEnv.remove("VK_ADD_IMPLICIT_LAYER_PATH");
        launchEnv.put("VK_LOADER_LAYERS_DISABLE", appendLoaderDisableFilter(launchEnv.get("VK_LOADER_LAYERS_DISABLE"), "~implicit~"));
        launchEnv.put("NODEVICE_SELECT", "1");
        launchEnv.put("DISABLE_BCN_COMPUTE", "1");
        launchEnv.put("DISABLE_VKBASALT", "1");

        ForensicLogger.logEvent(
                context,
                "info",
                "BIONIC_VULKAN_LAYER_CONTRACT_APPLIED",
                null,
                "guest_program_launcher",
                "bionic_vulkan_explicit_only_layer_contract",
                ForensicLogger.fields(
                        "vk_layer_path", launchEnv.get("VK_LAYER_PATH"),
                        "vk_implicit_layer_path", launchEnv.get("VK_IMPLICIT_LAYER_PATH"),
                        "vk_loader_layers_disable", launchEnv.get("VK_LOADER_LAYERS_DISABLE"),
                        "nodevice_select", launchEnv.get("NODEVICE_SELECT"),
                        "disable_bcn_compute", launchEnv.get("DISABLE_BCN_COMPUTE"),
                        "disable_vkbasalt", launchEnv.get("DISABLE_VKBASALT")
                )
        );
    }

    private static String appendLoaderDisableFilter(String value, String filter) {
        String normalizedFilter = filter == null ? "" : filter.trim();
        if (normalizedFilter.isEmpty()) return value == null ? "" : value.trim();
        String normalizedValue = value == null ? "" : value.trim();
        if (normalizedValue.isEmpty()) return normalizedFilter;
        for (String part : normalizedValue.split(",")) {
            if (normalizedFilter.equalsIgnoreCase(part.trim())) return normalizedValue;
        }
        return normalizedValue + "," + normalizedFilter;
    }

}
