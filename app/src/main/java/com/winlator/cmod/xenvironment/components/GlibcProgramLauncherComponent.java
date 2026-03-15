package com.winlator.cmod.xenvironment.components;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.winlator.cmod.box64.Box64PresetManager;
import com.winlator.cmod.container.Shortcut;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.core.EnvVars;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.ProcessHelper;
import com.winlator.cmod.xenvironment.ImageFs;

import java.io.File;

public class GlibcProgramLauncherComponent extends GuestProgramLauncherComponent {
    public GlibcProgramLauncherComponent(ContentsManager contentsManager, ContentProfile wineProfile, Shortcut shortcut) {
        super(contentsManager, wineProfile, shortcut);
    }

    @Override
    protected String getLauncherModel(ImageFs imageFs) {
        return "glibc";
    }

    @Override
    public void start() {
        copyDefaultBox64RCFile();
        super.start();
    }

    @Override
    protected void applyLauncherSpecificEnvVars(Context context, ImageFs imageFs, File rootDir, EnvVars launchEnv) {
        super.applyLauncherSpecificEnvVars(context, imageFs, rootDir, launchEnv);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean enableBox64Logs = preferences.getBoolean("enable_box64_logs", false);
        launchEnv.put("AERO_RUNTIME_EXECUTION_MODEL", "glibc_box64_guest");
        launchEnv.put("AERO_RUNTIME_ANDROID_BIONIC_ONLY", "0");
        launchEnv.put("WINEESYNC_WINLATOR", "1");
        launchEnv.put("BOX64_LD_LIBRARY_PATH", imageFs.getRootDir().getPath() + "/usr/lib/x86_64-linux-gnu");
        launchEnv.putAll(Box64PresetManager.getEnvVars("box64", context, getBox64Preset()));
        if (!launchEnv.has("BOX64_NOBANNER")) {
            launchEnv.put("BOX64_NOBANNER", ProcessHelper.PRINT_DEBUG && enableBox64Logs ? "0" : "1");
        }
        if (!launchEnv.has("BOX64_X11GLX")) launchEnv.put("BOX64_X11GLX", "1");

        File glibc64Dir = imageFs.getGlibc64Dir();
        File sysvshm64 = new File(glibc64Dir, "libandroid-sysvshm.so");
        File libredirect64 = new File(glibc64Dir, "libredirect.so");
        if (sysvshm64.exists() || libredirect64.exists()) {
            StringBuilder ldPreload = new StringBuilder();
            if (libredirect64.exists()) ldPreload.append(libredirect64.getPath());
            if (sysvshm64.exists()) {
                if (ldPreload.length() > 0) ldPreload.append(" ");
                ldPreload.append(sysvshm64.getPath());
            }
            launchEnv.put("LD_PRELOAD", ldPreload.toString());
        }
    }

    @Override
    protected String buildGuestCommand(Context context, ImageFs imageFs, File rootDir, EnvVars launchEnv,
                                       String winePath, String effectiveEmulator, boolean desktopShellBootstrap) {
        if (getWineInfo() != null && getWineInfo().isArm64EC()) {
            return super.buildGuestCommand(context, imageFs, rootDir, launchEnv, winePath, effectiveEmulator, desktopShellBootstrap);
        }
        File usrLocalBox64 = new File(rootDir, "/usr/local/bin/box64");
        String box64Path = usrLocalBox64.isFile() ? usrLocalBox64.getPath() : imageFs.getBinDir() + "/box64";
        return box64Path + " " + getGuestExecutable();
    }

    private void copyDefaultBox64RCFile() {
        Context context = environment.getContext();
        ImageFs imageFs = ImageFs.find(context);
        File rootDir = imageFs.getRootDir();
        File rcFile = new File(rootDir, "/etc/config.box64rc");
        if (rcFile.isFile()) return;
        File fallbackRc = new File(rootDir, "/usr/etc/config.box64rc");
        if (fallbackRc.isFile()) {
            FileUtils.copy(fallbackRc, rcFile);
            FileUtils.chmod(rcFile, 0644);
        } else {
            Log.d("GlibcProgramLauncher", "No donor rc file found, keeping current runtime defaults");
        }
    }
}
