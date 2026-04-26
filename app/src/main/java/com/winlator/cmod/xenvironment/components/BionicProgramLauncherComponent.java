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
        launchEnv.put("AERO_RUNTIME_REDIRECT_MODE", "host_closure_preload");
        launchEnv.put("WINE_OPEN_WITH_ANDROID_BROWSER", "1");
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
        appendAndroidHostClosureLdPreload(ldPreload, imageFs, false);
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
        launchEnv.put("AERO_RUNTIME_REDIRECT_MODE", "host_closure_preload");
        applyAndroidBionicHostLdLibraryPath(context, imageFs, launchEnv, "bionic_direct_arm64ec");

        StringBuilder ldPreload = new StringBuilder();
        appendAndroidHostClosureLdPreload(ldPreload, imageFs, false);
        if (ldPreload.length() > 0) {
            launchEnv.put("LD_PRELOAD", ldPreload.toString());
        } else {
            launchEnv.remove("LD_PRELOAD");
        }
    }

}
