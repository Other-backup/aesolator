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
import com.winlator.cmod.core.WineUtils;
import com.winlator.cmod.xenvironment.ImageFs;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

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
        boolean directArm64EcGuest = shouldUseDirectArm64EcGuestLaunch(imageFs, resolveEffectiveArm64EcEmulator(), isDesktopShellBootstrapLaunch());
        launchEnv.put(
                "AERO_RUNTIME_EXECUTION_MODEL",
                directArm64EcGuest
                        ? "glibc_fex_guest"
                        : "glibc_box64_guest"
        );
        launchEnv.put("AERO_RUNTIME_ANDROID_BIONIC_ONLY", directArm64EcGuest ? "1" : "0");
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
        File androidHostLibDir = imageFs.getAndroidHostLibDir();
        String androidHostLibPath = androidHostLibDir.isDirectory() ? androidHostLibDir.getPath() + ":" : "";
        launchEnv.put(
                "LD_LIBRARY_PATH",
                androidHostLibPath
                        + "/system/lib64:/apex/com.android.runtime/lib64"
        );

        StringBuilder ldPreload = new StringBuilder();
        appendAndroidHostClosureLdPreload(ldPreload, imageFs, false);
        if (ldPreload.length() > 0) {
            launchEnv.put("LD_PRELOAD", ldPreload.toString());
        } else {
            launchEnv.remove("LD_PRELOAD");
        }
    }

    @Override
    protected String buildGuestCommand(Context context, ImageFs imageFs, File rootDir, EnvVars launchEnv,
                                       String winePath, String effectiveEmulator, boolean desktopShellBootstrap) {
        if (getWineInfo() != null && getWineInfo().isArm64EC()) {
            if (shouldUseDirectArm64EcGuestLaunch(imageFs, effectiveEmulator, desktopShellBootstrap)) {
                Log.i("GlibcProgramLauncher", "Using direct arm64ec guest launcher via " + effectiveEmulator);
                return super.buildGuestCommand(context, imageFs, rootDir, launchEnv, winePath, effectiveEmulator, desktopShellBootstrap);
            }
            File wineBinary = new File(winePath, "wine");
            if (shouldWrapArm64EcWineWithBox64(wineBinary)) {
                File usrLocalBox64 = new File(imageFs.getLocalBinDir(), "box64");
                String box64Path = usrLocalBox64.isFile() ? usrLocalBox64.getPath() : imageFs.getBinDir() + "/box64";
                Log.w("GlibcProgramLauncher", "Wrapping arm64ec wine ELF with box64: " + wineBinary.getPath());
                return box64Path + " " + getGuestExecutable();
            }
            return super.buildGuestCommand(context, imageFs, rootDir, launchEnv, winePath, effectiveEmulator, desktopShellBootstrap);
        }
        File usrLocalBox64 = new File(imageFs.getLocalBinDir(), "box64");
        String box64Path = usrLocalBox64.isFile() ? usrLocalBox64.getPath() : imageFs.getBinDir() + "/box64";
        return box64Path + " " + getGuestExecutable();
    }

    @Override
    protected boolean requiresBox64ForArm64EcLaunch() {
        if (getWineInfo() == null || !getWineInfo().isArm64EC()) return false;
        if (shouldUseDirectArm64EcGuestLaunch(environment.getImageFs(), resolveEffectiveArm64EcEmulator(), isDesktopShellBootstrapLaunch())) {
            return false;
        }
        File runtimeRoot = getWineInfo().path == null ? null : new File(getWineInfo().path);
        File wineBinary = WineUtils.resolveRuntimeWineBinary(runtimeRoot);
        return shouldWrapArm64EcWineWithBox64(wineBinary);
    }

    private String resolveEffectiveArm64EcEmulator() {
        String requestedEmulator = getContainer() != null ? getContainer().getEmulator() : "";
        if (getShortcut() != null) {
            requestedEmulator = getShortcut().getExtra("emulator", requestedEmulator);
        }
        boolean desktopShellBootstrap = isDesktopShellBootstrapLaunch();
        return resolveEffectiveEmulator(environment.getImageFs(), requestedEmulator, desktopShellBootstrap);
    }

    private boolean isDesktopShellBootstrapLaunch() {
        String guestExecutable = getGuestExecutable();
        if (getShortcut() != null || guestExecutable == null) return false;
        String lowered = guestExecutable.toLowerCase(java.util.Locale.ROOT);
        return lowered.contains("explorer /desktop=shell")
                || lowered.contains("explorer.exe /desktop=shell");
    }

    private boolean shouldUseDirectArm64EcGuestLaunch(ImageFs imageFs, String effectiveEmulator, boolean desktopShellBootstrap) {
        if (getWineInfo() == null || !getWineInfo().isArm64EC() || imageFs == null) {
            return false;
        }

        if ("wowbox64".equalsIgnoreCase(effectiveEmulator)) {
            return hasWowbox64Payload(imageFs);
        }
        if ("fexcore".equalsIgnoreCase(effectiveEmulator)) {
            return hasFexArm64EcPayload(imageFs);
        }
        return false;
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

    protected boolean shouldWrapArm64EcWineWithBox64(File wineBinary) {
        if (wineBinary == null || !wineBinary.isFile()) return false;
        byte[] header = new byte[4];
        try (InputStream inputStream = new FileInputStream(wineBinary)) {
            int count = inputStream.read(header);
            return count == 4
                    && header[0] == 0x7f
                    && header[1] == 'E'
                    && header[2] == 'L'
                    && header[3] == 'F';
        } catch (Exception e) {
            Log.w("GlibcProgramLauncher", "Unable to inspect wine binary header: " + wineBinary.getPath(), e);
            return false;
        }
    }
}
