package com.winlator.cmod.xenvironment.components;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.winlator.cmod.container.Shortcut;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.core.EnvVars;
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
        launchEnv.put("WINE_OPEN_WITH_ANDROID_BROwSER", "1");
        launchEnv.put("WINE_OPEN_WITH_ANDROID_BROWSER", "1");
        if (preferences.getBoolean("enable_peb_logs", false)) {
            launchEnv.put("WINE_LOG_PEB_DATA", "1");
        }

        File sysvPath = new File(imageFs.getLibDir(), "libandroid-sysvshm.so");
        File evshimPath = new File(imageFs.getLibDir(), "libevshim.so");
        File redirectPath = new File(imageFs.getLibDir(), "libredirect-bionic.so");

        StringBuilder ldPreload = new StringBuilder();
        appendLdPreload(ldPreload, launchEnv.get("LD_PRELOAD"));
        appendLdPreload(ldPreload, sysvPath.isFile() ? sysvPath.getPath() : "");
        appendLdPreload(ldPreload, evshimPath.isFile() ? evshimPath.getPath() : "");
        appendLdPreload(ldPreload, redirectPath.isFile() ? redirectPath.getPath() : "");
        if (ldPreload.length() > 0) {
            launchEnv.put("LD_PRELOAD", ldPreload.toString());
        }

        if (evshimPath.isFile()) {
            launchEnv.put("EVSHIM_MAX_PLAYERS", "1");
            launchEnv.put("EVSHIM_SHM_ID", "1");
            launchEnv.put("EVSHIM_SHM_NAME", "controller-shm0");
        }
    }

    private void appendLdPreload(StringBuilder builder, String value) {
        if (value == null || value.trim().isEmpty()) return;
        if (builder.length() > 0) builder.append(':');
        builder.append(value.trim());
    }
}
