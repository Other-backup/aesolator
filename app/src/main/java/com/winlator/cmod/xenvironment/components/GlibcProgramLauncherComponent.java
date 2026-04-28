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
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.ProcessHelper;
import com.winlator.cmod.core.WineUtils;
import com.winlator.cmod.xenvironment.ImageFs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;

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
        launchEnv.put("AERO_RUNTIME_ANDROID_BIONIC_ONLY", "0");
        launchEnv.put("WINEESYNC_WINLATOR", "1");
        launchEnv.putAll(Box64PresetManager.getEnvVars("box64", context, getBox64Preset()));
        if (!launchEnv.has("BOX64_NOBANNER")) {
            launchEnv.put("BOX64_NOBANNER", ProcessHelper.PRINT_DEBUG && enableBox64Logs ? "0" : "1");
        }
        if (!launchEnv.has("BOX64_X11GLX")) launchEnv.put("BOX64_X11GLX", "1");
        applyGlibcWrapperContract(context, imageFs, rootDir, launchEnv, directArm64EcGuest);
        applyGlibcCwdRootAliasContract(context, imageFs, rootDir, launchEnv);

        applyGlibcOwnedPreloadContract(context, imageFs, launchEnv, "launcher_specific");
        applyGlibcProotContract(context, imageFs, rootDir, launchEnv, directArm64EcGuest);
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
                        ? "glibc_fex_guest"
                        : "glibc_wowbox64_guest"
        );
        launchEnv.put("AERO_RUNTIME_ANDROID_BIONIC_ONLY", "0");
        launchEnv.put("AERO_RUNTIME_REDIRECT_MODE", "glibc_interpreter_rebound");
        GlibcPreloadContract preloadContract = applyGlibcOwnedPreloadContract(context, imageFs, launchEnv, "android_host_env");
        ForensicLogger.logEvent(
                context,
                "info",
                "GLIBC_GUEST_ENV_APPLIED",
                null,
                "guest_program_launcher",
                "glibc_guest_env_applied",
                ForensicLogger.fields(
                        "effective_emulator", effectiveEmulator,
                        "runtime_model", "glibc",
                        "redirect_mode", launchEnv.get("AERO_RUNTIME_REDIRECT_MODE"),
                        "ld_preload_removed", !preloadContract.hasPreload,
                        "ld_preload_head", summarizePreloadHead(launchEnv.get("LD_PRELOAD"), 4),
                        "redirect_preload_present", preloadContract.redirectPresent,
                        "sysvshm_preload_present", preloadContract.sysvshmPresent,
                        "ld_library_path_head", summarizePathHead(launchEnv.get("LD_LIBRARY_PATH"), 8),
                        "box64_ld_library_path_head", summarizePathHead(launchEnv.get("BOX64_LD_LIBRARY_PATH"), 8)
                )
        );
    }

    @Override
    protected String buildGuestCommand(Context context, ImageFs imageFs, File rootDir, EnvVars launchEnv,
                                       String winePath, String effectiveEmulator, boolean desktopShellBootstrap) {
        String command;
        if (getWineInfo() != null && getWineInfo().isArm64EC()) {
            if (shouldUseDirectArm64EcGuestLaunch(imageFs, effectiveEmulator, desktopShellBootstrap)) {
                Log.i("GlibcProgramLauncher", "Using direct arm64ec guest launcher via " + effectiveEmulator);
                command = super.buildGuestCommand(context, imageFs, rootDir, launchEnv, winePath, effectiveEmulator, desktopShellBootstrap);
                return wrapGlibcCommandWithProot(context, imageFs, rootDir, launchEnv, command);
            }
            File wineBinary = new File(winePath, "wine");
            if (shouldWrapArm64EcWineWithBox64(wineBinary)) {
                File usrLocalBox64 = new File(imageFs.getLocalBinDir(), "box64");
                String box64Path = usrLocalBox64.isFile() ? usrLocalBox64.getPath() : imageFs.getBinDir() + "/box64";
                Log.w("GlibcProgramLauncher", "Wrapping arm64ec wine ELF with box64: " + wineBinary.getPath());
                command = box64Path + " " + getGuestExecutable();
                return wrapGlibcCommandWithProot(context, imageFs, rootDir, launchEnv, command);
            }
            command = super.buildGuestCommand(context, imageFs, rootDir, launchEnv, winePath, effectiveEmulator, desktopShellBootstrap);
            return wrapGlibcCommandWithProot(context, imageFs, rootDir, launchEnv, command);
        }
        File usrLocalBox64 = new File(imageFs.getLocalBinDir(), "box64");
        String box64Path = usrLocalBox64.isFile() ? usrLocalBox64.getPath() : imageFs.getBinDir() + "/box64";
        command = box64Path + " " + getGuestExecutable();
        return wrapGlibcBox64CommandForAndroidHost(context, imageFs, rootDir, launchEnv, command);
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

    private void copyDefaultBox64RCFile() {
        Context context = environment.getContext();
        ImageFs imageFs = environment.getImageFs() != null ? environment.getImageFs() : ImageFs.find(context);
        File rootDir = imageFs.getRootDir();
        File rcFile = new File(rootDir, "etc/config.box64rc");
        if (rcFile.isFile()) return;
        File fallbackRc = new File(rootDir, "usr/etc/config.box64rc");
        if (fallbackRc.isFile()) {
            FileUtils.copy(fallbackRc, rcFile);
            FileUtils.chmod(rcFile, 0644);
        } else {
            Log.d("GlibcProgramLauncher", "No donor rc file found, keeping current runtime defaults");
        }
    }

    private void applyGlibcWrapperContract(Context context, ImageFs imageFs, File rootDir,
                                           EnvVars launchEnv, boolean directArm64EcGuest) {
        if (imageFs == null || rootDir == null || launchEnv == null) return;

        String previousLdLibraryPath = launchEnv.get("LD_LIBRARY_PATH");
        String previousBox64LdLibraryPath = launchEnv.get("BOX64_LD_LIBRARY_PATH");
        String previousBox64Path = launchEnv.get("BOX64_PATH");

        launchEnv.put("LD_LIBRARY_PATH", buildGlibcGuestLdLibraryPath(imageFs, previousLdLibraryPath));
        launchEnv.put("BOX64_LD_LIBRARY_PATH", buildGlibcGuestBox64LdLibraryPath(imageFs, previousBox64LdLibraryPath));
        launchEnv.put("BOX86_LD_LIBRARY_PATH", buildGlibcGuestBox86LdLibraryPath(imageFs, launchEnv.get("BOX86_LD_LIBRARY_PATH")));
        launchEnv.put("BOX64_PATH", buildGlibcBox64Path(imageFs, previousBox64Path));
        launchEnv.put("BOX86_PATH", buildGlibcBox86Path(imageFs, launchEnv.get("BOX86_PATH")));

        File rcFile = resolveBox64RcFile(rootDir);
        boolean rcReady = rcFile != null && rcFile.isFile();
        if (rcReady) {
            launchEnv.put("BOX64_RCFILE", rcFile.getPath());
            launchEnv.put("BOX64_NORCFILES", "0");
        }

        ForensicLogger.logEvent(
                context,
                "info",
                "GLIBC_WRAPPER_CONTRACT_APPLIED",
                null,
                "guest_program_launcher",
                "glibc_wrapper_contract_applied",
                ForensicLogger.fields(
                        "direct_arm64ec_guest", directArm64EcGuest,
                        "box64_rcfile_ready", rcReady,
                        "box64_rcfile", rcReady ? rcFile.getPath() : "",
                        "box64_norcfiles", launchEnv.get("BOX64_NORCFILES"),
                        "ld_library_path_before", summarizePathHead(previousLdLibraryPath, 6),
                        "ld_library_path_head", summarizePathHead(launchEnv.get("LD_LIBRARY_PATH"), 10),
                        "box64_ld_library_path_before", summarizePathHead(previousBox64LdLibraryPath, 6),
                        "box64_ld_library_path_head", summarizePathHead(launchEnv.get("BOX64_LD_LIBRARY_PATH"), 10),
                        "box86_ld_library_path_head", summarizePathHead(launchEnv.get("BOX86_LD_LIBRARY_PATH"), 8),
                        "box64_path_head", summarizePathHead(launchEnv.get("BOX64_PATH"), 8),
                        "box86_path_head", summarizePathHead(launchEnv.get("BOX86_PATH"), 8)
                )
        );
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

    private void applyGlibcProotContract(Context context, ImageFs imageFs, File rootDir,
                                         EnvVars launchEnv, boolean directArm64EcGuest) {
        GlibcProotContract contract = buildGlibcProotContract(imageFs, rootDir);
        if (launchEnv != null && contract.shouldUse) {
            launchEnv.put("PROOT_LOADER", contract.loaderPath);
            launchEnv.put("PROOT_TMP_DIR", contract.tmpPath);
            launchEnv.put("PROOT_IGNORE_MISSING_BINDINGS", "1");
            launchEnv.put("AERO_GLIBC_PROOT_ROOTFS", "1");
        } else if (launchEnv != null) {
            launchEnv.remove("PROOT_LOADER");
            launchEnv.remove("PROOT_TMP_DIR");
            launchEnv.remove("PROOT_IGNORE_MISSING_BINDINGS");
            launchEnv.remove("AERO_GLIBC_PROOT_ROOTFS");
        }
        ForensicLogger.logEvent(
                context,
                contract.available || contract.hostBoundInterpreter ? "info" : "warn",
                "GLIBC_PROOT_CONTRACT_APPLIED",
                null,
                "guest_program_launcher",
                "glibc_proot_contract_applied",
                ForensicLogger.fields(
                        "available", contract.available,
                        "should_use", contract.shouldUse,
                        "host_bound_interpreter", contract.hostBoundInterpreter,
                        "direct_arm64ec_guest", directArm64EcGuest,
                        "proot_path", contract.prootPath,
                        "proot_present", contract.prootPresent,
                        "loader_path", contract.loaderPath,
                        "loader_present", contract.loaderPresent,
                        "tmp_path", contract.tmpPath,
                        "tmp_present", contract.tmpPresent,
                        "proot_no_seccomp_supported", false,
                        "root_path", contract.rootPath
                )
        );
    }

    private String wrapGlibcBox64CommandForAndroidHost(Context context, ImageFs imageFs, File rootDir,
                                                      EnvVars launchEnv, String command) {
        File runtimeRoot = imageFs != null ? WineUtils.resolveCanonicalRuntimeRoot(new File(imageFs.getWinePath())) : null;
        WineUtils.RuntimeAbiContract abiContract = WineUtils.validateRuntimeAbiContract(
                rootDir,
                runtimeRoot,
                getWineInfo(),
                ContentProfile.RUNTIME_MODEL_GLIBC
        );
        if (launchEnv != null) {
            launchEnv.remove("LD_LIBRARY_PATH");
            launchEnv.remove("LD_PRELOAD");
            launchEnv.remove("PROOT_LOADER");
            launchEnv.remove("PROOT_TMP_DIR");
            launchEnv.remove("PROOT_IGNORE_MISSING_BINDINGS");
            launchEnv.remove("AERO_GLIBC_PROOT_ROOTFS");
            launchEnv.put("AERO_GLIBC_BOX64_HOST_DIRECT", "1");
        }
        ForensicLogger.logEvent(
                context,
                abiContract.complete ? "info" : "warn",
                "GLIBC_BOX64_HOST_DIRECT_COMMAND",
                null,
                "guest_program_launcher",
                abiContract.complete ? "glibc_box64_host_direct_command" : "glibc_box64_host_direct_command_blocked_by_abi",
                ForensicLogger.fields(
                        "runtime_root", abiContract.runtimeRootPath,
                        "wine_arch", abiContract.arch,
                        "abi_complete", abiContract.complete,
                        "abi_missing", abiContract.missing,
                        "abi_glibc_loader_rejected", abiContract.glibcLoaderRejectedPath,
                        "abi_glibc_libc_rejected", abiContract.glibcLibcRejectedPath,
                        "abi_glibc_guest_loader_mode", abiContract.glibcGuestLoaderMode,
                        "abi_glibc_guest_support", abiContract.glibcGuestSupportPath,
                        "abi_glibc_guest_support_rejected", abiContract.glibcGuestSupportRejectedPath,
                        "command_head", summarizePathHead(command == null ? "" : command.replace(' ', ':'), 6),
                        "outer_ld_path_cleared", launchEnv == null || !launchEnv.has("LD_LIBRARY_PATH"),
                        "outer_ld_preload_cleared", launchEnv == null || !launchEnv.has("LD_PRELOAD"),
                        "proot_removed", launchEnv == null || !launchEnv.has("PROOT_LOADER")
                )
        );
        return command;
    }

    private String wrapGlibcCommandWithProot(Context context, ImageFs imageFs, File rootDir,
                                             EnvVars launchEnv, String command) {
        GlibcProotContract contract = buildGlibcProotContract(imageFs, rootDir);
        if (!contract.shouldUse || command == null || command.trim().isEmpty()) {
            String status = contract.hostBoundInterpreter
                    ? "glibc_proot_skipped_host_bound_interpreter"
                    : "glibc_proot_command_wrap_skipped";
            ForensicLogger.logEvent(
                    context,
                    (contract.available || contract.hostBoundInterpreter) ? "info" : "warn",
                    "GLIBC_PROOT_COMMAND_WRAP_SKIPPED",
                    null,
                    "guest_program_launcher",
                    status,
                    ForensicLogger.fields(
                            "available", contract.available,
                            "command_empty", command == null || command.trim().isEmpty(),
                            "host_bound_interpreter", contract.hostBoundInterpreter,
                            "proot_path", contract.prootPath,
                            "loader_path", contract.loaderPath
                    )
            );
            return command;
        }

        if (launchEnv != null) {
            launchEnv.put("PROOT_LOADER", contract.loaderPath);
            launchEnv.put("PROOT_TMP_DIR", contract.tmpPath);
            launchEnv.put("PROOT_IGNORE_MISSING_BINDINGS", "1");
            launchEnv.put("AERO_GLIBC_PROOT_ROOTFS", "1");
        }

        String[] guestCommandParts = toProotGuestCommandParts(contract.rootPath, command.trim());
        String guestCommand = toShellCommandLine(guestCommandParts);
        File launchScript = writeProotLaunchScript(context, imageFs, contract, launchEnv, guestCommandParts);
        if (launchScript == null || !launchScript.isFile()) {
            ForensicLogger.logEvent(
                    context,
                    "error",
                    "GLIBC_PROOT_COMMAND_WRAP_FAILED",
                    null,
                    "guest_program_launcher",
                    "glibc_proot_launch_script_missing",
                    ForensicLogger.fields(
                            "root_path", contract.rootPath,
                            "command_head", summarizePathHead(command.replace(' ', ':'), 6)
                    )
            );
            return command;
        }

        if (launchEnv != null) {
            launchEnv.remove("LD_LIBRARY_PATH");
            launchEnv.remove("LD_PRELOAD");
        }

        String guestScriptPath = toProotGuestPath(contract.rootPath, launchScript.getAbsolutePath());
        String wrapped = contract.prootPath
                + " -r " + contract.rootPath
                + " -b /proc"
                + " -b /dev"
                + " -b /sys"
                + " -b " + contract.rootPath + ":" + contract.rootPath
                + " -w / "
                + "/bin/sh " + guestScriptPath;
        ForensicLogger.logEvent(
                context,
                "info",
                "GLIBC_PROOT_COMMAND_WRAPPED",
                null,
                "guest_program_launcher",
                "glibc_proot_command_wrapped",
                ForensicLogger.fields(
                        "root_path", contract.rootPath,
                        "proot_path", contract.prootPath,
                        "loader_path", contract.loaderPath,
                        "tmp_path", contract.tmpPath,
                        "launch_script", launchScript.getAbsolutePath(),
                        "guest_script", guestScriptPath,
                        "command_head", summarizePathHead(command.replace(' ', ':'), 6),
                        "guest_command_head", summarizePathHead(guestCommand.replace(' ', ':'), 6),
                        "outer_ld_path_cleared", launchEnv == null || !launchEnv.has("LD_LIBRARY_PATH"),
                        "outer_ld_preload_cleared", launchEnv == null || !launchEnv.has("LD_PRELOAD")
                )
        );
        return wrapped;
    }

    private File writeProotLaunchScript(Context context, ImageFs imageFs, GlibcProotContract contract,
                                        EnvVars launchEnv, String[] guestCommandParts) {
        if (imageFs == null || contract == null || guestCommandParts == null || guestCommandParts.length == 0) return null;
        File tmpDir = imageFs.getTmpDir();
        if (tmpDir == null) return null;
        if (!tmpDir.isDirectory()) tmpDir.mkdirs();

        String guestCommand = toShellCommandLine(guestCommandParts);
        String scriptName = "ae-glibc-proot-launch-"
                + ForensicLogger.sha256Hex(guestCommand + "\n" + (launchEnv != null ? launchEnv.toString() : ""))
                .substring(0, 16)
                + ".sh";
        File script = new File(tmpDir, scriptName);
        StringBuilder body = new StringBuilder();
        body.append("#!/bin/sh\n");
        body.append("export AERO_GLIBC_PROOT_INNER=1\n");
        body.append("cd /\n");
        if (launchEnv != null) {
            for (String key : launchEnv) {
                String value = launchEnv.get(key);
                if (key == null || key.trim().isEmpty() || value == null) continue;
                body.append("export ")
                        .append(key)
                        .append("=")
                        .append(shellQuote(toProotGuestEnvValue(contract.rootPath, value)))
                        .append("\n");
            }
        }
        body.append("exec ").append(guestCommand).append("\n");

        if (!FileUtils.writeString(script, body.toString())) return null;
        FileUtils.chmod(script, 0700);
        ForensicLogger.logEvent(
                context,
                "info",
                "GLIBC_PROOT_LAUNCH_SCRIPT_READY",
                null,
                "guest_program_launcher",
                "glibc_proot_launch_script_ready",
                ForensicLogger.fields(
                        "script", script.getAbsolutePath(),
                        "guest_script", toProotGuestPath(contract.rootPath, script.getAbsolutePath()),
                        "guest_command_head", summarizePathHead(guestCommand.replace(' ', ':'), 8),
                        "env_export_count", launchEnv != null ? launchEnv.toStringArray().length : 0
                )
        );
        return script;
    }

    private String[] toProotGuestCommandParts(String rootPath, String command) {
        String[] parts = ProcessHelper.splitCommand(command);
        if (parts.length == 0) return parts;
        parts[0] = toProotGuestPath(rootPath, parts[0]);
        return parts;
    }

    private String toShellCommandLine(String[] parts) {
        if (parts == null || parts.length == 0) return "";
        ArrayList<String> out = new ArrayList<>();
        for (String part : parts) out.add(shellQuote(part == null ? "" : part));
        return String.join(" ", out);
    }

    private String shellQuote(String value) {
        if (value == null || value.isEmpty()) return "''";
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    private String toProotGuestEnvValue(String rootPath, String value) {
        if (rootPath == null || rootPath.isEmpty() || value == null || value.isEmpty()) return value == null ? "" : value;
        return value.replace(rootPath, "");
    }

    private String toProotGuestPath(String rootPath, String path) {
        if (rootPath == null || rootPath.isEmpty() || path == null || path.isEmpty()) return path == null ? "" : path;
        if (path.equals(rootPath)) return "/";
        if (path.startsWith(rootPath + "/")) return path.substring(rootPath.length());
        return path;
    }

    private GlibcProotContract buildGlibcProotContract(ImageFs imageFs, File rootDir) {
        GlibcProotContract contract = new GlibcProotContract();
        File resolvedRoot = rootDir != null ? rootDir : (imageFs != null ? imageFs.getRootDir() : null);
        contract.rootPath = resolvedRoot != null ? resolvedRoot.getAbsolutePath() : "";
        File proot = resolvedRoot != null ? new File(resolvedRoot, "usr/bin/proot") : null;
        File loader = resolvedRoot != null ? new File(resolvedRoot, "usr/lib/proot-loader.so") : null;
        File tmp = imageFs != null ? imageFs.getTmpDir() : (resolvedRoot != null ? new File(resolvedRoot, "tmp") : null);
        contract.prootPath = proot != null ? proot.getAbsolutePath() : "";
        contract.loaderPath = loader != null ? loader.getAbsolutePath() : "";
        contract.tmpPath = tmp != null ? tmp.getAbsolutePath() : "";
        contract.prootPresent = proot != null && proot.isFile();
        contract.loaderPresent = loader != null && loader.isFile();
        contract.tmpPresent = tmp != null && tmp.isDirectory();
        contract.available = contract.prootPresent && contract.loaderPresent && contract.tmpPresent;
        contract.hostBoundInterpreter = hasHostBoundGlibcInterpreter(imageFs);
        contract.shouldUse = contract.available && !contract.hostBoundInterpreter;
        return contract;
    }

    private void applyGlibcCwdRootAliasContract(Context context, ImageFs imageFs, File rootDir, EnvVars launchEnv) {
        if (imageFs == null || rootDir == null || launchEnv == null) return;
        File runtimeRoot = WineUtils.resolveCanonicalRuntimeRoot(new File(imageFs.getWinePath()));
        File winePrefix = resolveWinePrefixDirectory(imageFs, launchEnv);
        ArrayList<String> touched = new ArrayList<>();
        ArrayList<String> blocked = new ArrayList<>();
        ArrayList<String> preserved = new ArrayList<>();

        ensureDirectoryIfMissing(imageFs.getTmpDir());
        ensureCwdRootAliasSet(winePrefix, rootDir, touched, blocked, preserved);
        ensureCwdRootAliasSet(runtimeRoot, rootDir, touched, blocked, preserved);
        ensureCwdRootAliasSet(new File(runtimeRoot, "bin"), rootDir, touched, blocked, preserved);
        ensureCwdRootAliasSet(WineUtils.resolveRuntimeWineUnixDir(runtimeRoot, getWineInfo()), rootDir, touched, blocked, preserved);
        ensureCwdRootAliasSet(new File(runtimeRoot, "lib/wine/x86_64-unix"), rootDir, touched, blocked, preserved);
        ensureCwdRootAliasSet(new File(runtimeRoot, "lib/wine/i386-unix"), rootDir, touched, blocked, preserved);

        ForensicLogger.logEvent(
                context,
                blocked.isEmpty() ? "info" : "warn",
                "GLIBC_CWD_ROOT_ALIAS_CONTRACT_APPLIED",
                null,
                "guest_program_launcher",
                "glibc_cwd_root_alias_contract_applied",
                ForensicLogger.fields(
                        "runtime_root", runtimeRoot.getPath(),
                        "wineprefix", winePrefix != null ? winePrefix.getPath() : "",
                        "touched_count", touched.size(),
                        "blocked_count", blocked.size(),
                        "preserved_count", preserved.size(),
                        "touched", summarizeList(touched, 12),
                        "blocked", summarizeList(blocked, 12),
                        "preserved", summarizeList(preserved, 12)
                )
        );
    }

    private File resolveWinePrefixDirectory(ImageFs imageFs, EnvVars launchEnv) {
        String value = launchEnv != null ? launchEnv.get("WINEPREFIX") : "";
        if (value != null && !value.trim().isEmpty()) return new File(value.trim());
        return imageFs != null ? new File(imageFs.home_path, ".wine") : null;
    }

    private void ensureDirectoryIfMissing(File directory) {
        if (directory != null && !directory.isDirectory()) {
            directory.mkdirs();
        }
    }

    private void ensureCwdRootAliasSet(File ownerDir, File rootDir, ArrayList<String> touched, ArrayList<String> blocked, ArrayList<String> preserved) {
        if (ownerDir == null || rootDir == null || !ownerDir.isDirectory()) return;
        ensureRootAlias(ownerDir, "tmp", new File(rootDir, "tmp"), touched, blocked, preserved);
        ensureRootAlias(ownerDir, "usr", new File(rootDir, "usr"), touched, blocked, preserved);
        ensureRootAlias(ownerDir, "share", new File(rootDir, "usr/share"), touched, blocked, preserved);
        ensureRootAlias(ownerDir, "home", new File(rootDir, "home"), touched, blocked, preserved);
        ensureRootAlias(ownerDir, "opt", new File(rootDir, "opt"), touched, blocked, preserved);
        ensureRootAlias(ownerDir, "etc", new File(rootDir, "etc"), touched, blocked, preserved);
    }

    private void ensureRootAlias(File ownerDir, String name, File target, ArrayList<String> touched, ArrayList<String> blocked, ArrayList<String> preserved) {
        if (ownerDir == null || name == null || name.trim().isEmpty() || target == null || !target.exists()) return;
        File link = new File(ownerDir, name);
        String targetPath = target.getAbsolutePath();
        try {
            if (link.getAbsolutePath().equals(targetPath)) return;
            if (link.exists() || FileUtils.isSymlink(link)) {
                if (FileUtils.isSymlink(link) && isAliasTargetCurrent(link, targetPath)) return;
                if (link.isDirectory() && !FileUtils.isSymlink(link)) {
                    preserved.add(ownerDir.getPath() + "/" + name + ":directory");
                    return;
                }
                if (!link.delete()) {
                    blocked.add(ownerDir.getPath() + "/" + name + ":delete_failed");
                    return;
                }
            }
            Files.createSymbolicLink(link.toPath(), target.toPath());
            touched.add(ownerDir.getPath() + "/" + name + "->" + targetPath);
        } catch (Throwable error) {
            blocked.add(ownerDir.getPath() + "/" + name + ":" + error.getClass().getSimpleName());
        }
    }

    private boolean isAliasTargetCurrent(File link, String targetPath) {
        String current = FileUtils.readSymlink(link);
        if (current == null || current.trim().isEmpty()) return false;
        if (current.equals(targetPath)) return true;
        File currentFile = new File(current);
        if (!currentFile.isAbsolute()) currentFile = new File(link.getParentFile(), current);
        return !link.getAbsolutePath().equals(currentFile.getAbsolutePath())
                && targetPath.equals(currentFile.getAbsolutePath());
    }

    private boolean hasHostBoundGlibcInterpreter(ImageFs imageFs) {
        if (imageFs == null || imageFs.getRootDir() == null) return false;
        File runtimeRoot = WineUtils.resolveCanonicalRuntimeRoot(new File(imageFs.getWinePath()));
        File wineBinary = WineUtils.resolveRuntimeWineBinary(runtimeRoot);
        if (wineBinary == null || !wineBinary.isFile()) wineBinary = new File(runtimeRoot, "bin/wine");
        if (!wineBinary.isFile()) return false;
        String rootPath = imageFs.getRootDir().getAbsolutePath();
        return containsAsciiToken(wineBinary, rootPath + "/usr/lib/ld-linux", "/data/user/0/", "/data/data/");
    }

    private String buildGlibcGuestLdLibraryPath(ImageFs imageFs, String currentPath) {
        LinkedHashSet<String> segments = new LinkedHashSet<>();
        addFilteredGlibcPathSegments(segments, currentPath);
        if (imageFs != null) {
            File rootDir = imageFs.getRootDir();
            addPathIfDirectory(segments, imageFs.getGlibc64Dir());
            addPathIfDirectory(segments, new File(rootDir, "usr/lib64"));
            addPathIfDirectory(segments, new File(rootDir, "lib"));
            addPathIfDirectory(segments, new File(rootDir, "lib64"));
            addPathIfDirectory(segments, new File(rootDir, "usr/lib/aarch64-linux-gnu"));
            addPathIfDirectory(segments, new File(rootDir, "usr/glibc/lib"));
            addPathIfDirectory(segments, imageFs.getGlibc32Dir());
        }
        return String.join(":", segments);
    }

    private String buildGlibcGuestBox64LdLibraryPath(ImageFs imageFs, String currentPath) {
        LinkedHashSet<String> segments = new LinkedHashSet<>();
        addFilteredGlibcPathSegments(segments, currentPath);
        if (imageFs != null) {
            File rootDir = imageFs.getRootDir();
            File runtimeRoot = new File(imageFs.getWinePath());
            WineUtils.RuntimeLayout runtimeLayout = WineUtils.resolveRuntimeLayout(runtimeRoot);
            addPathIfDirectory(segments, runtimeLayout.binDir);
            addPathIfDirectory(segments, runtimeLayout.libDir);
            addPathIfDirectory(segments, runtimeLayout.wineLibDir);
            addPathIfDirectory(segments, WineUtils.resolveRuntimeWineUnixDir(runtimeRoot, getWineInfo()));
            addPathIfDirectory(segments, new File(runtimeRoot, "lib"));
            addPathIfDirectory(segments, new File(runtimeRoot, "lib64"));
            addPathIfDirectory(segments, new File(rootDir, "usr/lib/x86_64-linux-gnu"));
            addPathIfDirectory(segments, new File(rootDir, "lib/x86_64-linux-gnu"));
            addPathIfDirectory(segments, new File(rootDir, "usr/local/lib/x86_64-linux-gnu"));
            addPathIfDirectory(segments, new File(rootDir, "usr/lib/box64-x86_64-linux-gnu"));
            addPathIfDirectory(segments, new File(rootDir, "usr/lib/box64-i386-linux-gnu"));
            addPathIfDirectory(segments, new File(rootDir, "lib/box64-x86_64-linux-gnu"));
            addPathIfDirectory(segments, new File(rootDir, "lib/box64-i386-linux-gnu"));
            addPathIfDirectory(segments, new File(rootDir, "usr/local/lib"));
            addPathIfDirectory(segments, imageFs.getGlibc64Dir());
        }
        return String.join(":", segments);
    }

    private String buildGlibcGuestBox86LdLibraryPath(ImageFs imageFs, String currentPath) {
        LinkedHashSet<String> segments = new LinkedHashSet<>();
        addFilteredGlibcPathSegments(segments, currentPath);
        if (imageFs != null) {
            File rootDir = imageFs.getRootDir();
            addPathIfDirectory(segments, new File(rootDir, "usr/lib/i386-linux-gnu"));
            addPathIfDirectory(segments, new File(rootDir, "lib/i386-linux-gnu"));
            addPathIfDirectory(segments, new File(rootDir, "usr/lib/box86"));
            addPathIfDirectory(segments, new File(rootDir, "usr/lib/box86-i386-linux-gnu"));
            addPathIfDirectory(segments, new File(rootDir, "usr/lib/box64-i386-linux-gnu"));
            addPathIfDirectory(segments, new File(rootDir, "usr/lib/i686-pc-linux-gnu"));
            addPathIfDirectory(segments, new File(rootDir, "usr/lib32"));
            addPathIfDirectory(segments, new File(rootDir, "lib32"));
            addPathIfDirectory(segments, imageFs.getGlibc32Dir());
        }
        return String.join(":", segments);
    }

    private String buildGlibcBox64Path(ImageFs imageFs, String currentPath) {
        LinkedHashSet<String> segments = new LinkedHashSet<>();
        addFilteredGlibcPathSegments(segments, currentPath);
        if (imageFs != null) {
            File rootDir = imageFs.getRootDir();
            WineUtils.RuntimeLayout runtimeLayout = WineUtils.resolveRuntimeLayout(new File(imageFs.getWinePath()));
            addPathIfDirectory(segments, runtimeLayout.binDir);
            addPathIfDirectory(segments, imageFs.getLocalBinDir());
            addPathIfDirectory(segments, imageFs.getBinDir());
            addPathIfDirectory(segments, new File(rootDir, "usr/local/bin"));
            addPathIfDirectory(segments, new File(rootDir, "usr/glibc/bin"));
        }
        return String.join(":", segments);
    }

    private String buildGlibcBox86Path(ImageFs imageFs, String currentPath) {
        LinkedHashSet<String> segments = new LinkedHashSet<>();
        addFilteredGlibcPathSegments(segments, currentPath);
        if (imageFs != null) {
            File rootDir = imageFs.getRootDir();
            WineUtils.RuntimeLayout runtimeLayout = WineUtils.resolveRuntimeLayout(new File(imageFs.getWinePath()));
            addPathIfDirectory(segments, runtimeLayout.binDir);
            addPathIfDirectory(segments, imageFs.getLocalBinDir());
            addPathIfDirectory(segments, imageFs.getBinDir());
            addPathIfDirectory(segments, new File(rootDir, "usr/local/bin"));
            addPathIfDirectory(segments, new File(rootDir, "usr/glibc/bin"));
        }
        return String.join(":", segments);
    }

    private GlibcPreloadContract applyGlibcOwnedPreloadContract(Context context, ImageFs imageFs,
                                                               EnvVars launchEnv, String owner) {
        GlibcPreloadContract contract = buildGlibcOwnedPreloadContract(imageFs);
        if (launchEnv == null) return contract;
        String previous = launchEnv.get("LD_PRELOAD");
        if (contract.hasPreload) {
            launchEnv.put("LD_PRELOAD", contract.ldPreload);
        } else {
            launchEnv.remove("LD_PRELOAD");
        }
        ForensicLogger.logEvent(
                context,
                contract.hasPreload || contract.skippedIncompatiblePreload ? "info" : "warn",
                "GLIBC_OWNED_PRELOAD_CONTRACT_APPLIED",
                null,
                "guest_program_launcher",
                "glibc_owned_preload_contract_applied",
                ForensicLogger.fields(
                        "owner", owner == null ? "" : owner,
                        "ld_preload_before", summarizePreloadHead(previous, 4),
                        "ld_preload_head", summarizePreloadHead(launchEnv.get("LD_PRELOAD"), 4),
                        "redirect_preload_present", contract.redirectPresent,
                        "redirect_file_present", contract.redirectFilePresent,
                        "redirect_abi_compatible", contract.redirectAbiCompatible,
                        "sysvshm_preload_present", contract.sysvshmPresent,
                        "sysvshm_file_present", contract.sysvshmFilePresent,
                        "sysvshm_abi_compatible", contract.sysvshmAbiCompatible,
                        "skipped_incompatible_preload", contract.skippedIncompatiblePreload,
                        "removed_foreign_preload", previous != null
                                && !previous.trim().isEmpty()
                                && !previous.trim().equals(launchEnv.get("LD_PRELOAD"))
                )
        );
        return contract;
    }

    private GlibcPreloadContract buildGlibcOwnedPreloadContract(ImageFs imageFs) {
        GlibcPreloadContract contract = new GlibcPreloadContract();
        if (imageFs == null || imageFs.getRootDir() == null) return contract;
        ArrayList<String> preloadEntries = new ArrayList<>();
        File glibc64Dir = imageFs.getGlibc64Dir();
        File libredirect64 = new File(glibc64Dir, "libredirect.so");
        File sysvshm64 = new File(glibc64Dir, "libandroid-sysvshm.so");
        contract.redirectFilePresent = libredirect64.isFile();
        contract.sysvshmFilePresent = sysvshm64.isFile();
        contract.redirectAbiCompatible = isGlibcAbiCompatiblePreload(libredirect64);
        contract.sysvshmAbiCompatible = isGlibcAbiCompatiblePreload(sysvshm64);
        contract.redirectPresent = contract.redirectAbiCompatible && appendOwnedPreload(preloadEntries, imageFs, libredirect64);
        contract.sysvshmPresent = contract.sysvshmAbiCompatible && appendOwnedPreload(preloadEntries, imageFs, sysvshm64);
        contract.skippedIncompatiblePreload =
                (contract.redirectFilePresent && !contract.redirectAbiCompatible)
                        || (contract.sysvshmFilePresent && !contract.sysvshmAbiCompatible);
        contract.hasPreload = !preloadEntries.isEmpty();
        contract.ldPreload = String.join(" ", preloadEntries);
        return contract;
    }

    private boolean isGlibcAbiCompatiblePreload(File library) {
        if (library == null || !library.isFile()) return false;
        boolean needsGlibc = containsAsciiToken(library, "libc.so.6", "GLIBC_");
        boolean needsAndroidBionic = containsAsciiCString(library, "libc.so", "libdl.so", "liblog.so");
        return needsGlibc && !needsAndroidBionic;
    }

    private boolean containsAsciiToken(File file, String... tokens) {
        return scanAsciiFile(file, false, tokens);
    }

    private boolean containsAsciiCString(File file, String... tokens) {
        return scanAsciiFile(file, true, tokens);
    }

    private boolean scanAsciiFile(File file, boolean requireTrailingNul, String... tokens) {
        if (file == null || tokens == null || tokens.length == 0 || !file.isFile()) return false;
        byte[] buffer = new byte[65536];
        byte[] carry = new byte[256];
        int carryLen = 0;
        try (InputStream inputStream = new FileInputStream(file)) {
            int read;
            while ((read = inputStream.read(buffer)) > 0) {
                byte[] window = new byte[carryLen + read];
                System.arraycopy(carry, 0, window, 0, carryLen);
                System.arraycopy(buffer, 0, window, carryLen, read);
                for (String token : tokens) {
                    if (requireTrailingNul ? containsAsciiCString(window, token) : containsAsciiToken(window, token)) {
                        return true;
                    }
                }
                carryLen = Math.min(carry.length, window.length);
                System.arraycopy(window, window.length - carryLen, carry, 0, carryLen);
            }
        } catch (IOException e) {
            Log.w("GlibcProgramLauncher", "Unable to scan ELF string surface: " + file.getPath(), e);
        }
        return false;
    }

    private boolean containsAsciiToken(byte[] data, String token) {
        if (data == null || token == null || token.isEmpty()) return false;
        byte[] needle = token.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        if (needle.length == 0 || data.length < needle.length) return false;
        int last = data.length - needle.length;
        for (int i = 0; i <= last; i++) {
            int j = 0;
            while (j < needle.length && data[i + j] == needle[j]) j++;
            if (j == needle.length) return true;
        }
        return false;
    }

    private boolean containsAsciiCString(byte[] data, String token) {
        if (data == null || token == null || token.isEmpty()) return false;
        byte[] needle = token.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        if (needle.length == 0 || data.length <= needle.length) return false;
        int last = data.length - needle.length;
        for (int i = 0; i <= last; i++) {
            int j = 0;
            while (j < needle.length && data[i + j] == needle[j]) j++;
            if (j == needle.length && i + needle.length < data.length && data[i + needle.length] == 0) {
                return true;
            }
        }
        return false;
    }

    private boolean appendOwnedPreload(ArrayList<String> preloadEntries, ImageFs imageFs, File library) {
        if (preloadEntries == null || imageFs == null || library == null || !library.isFile()) return false;
        try {
            File canonicalRoot = imageFs.getRootDir().getCanonicalFile();
            File canonicalLibrary = library.getCanonicalFile();
            if (!canonicalLibrary.toPath().startsWith(canonicalRoot.toPath())) return false;
            preloadEntries.add(canonicalLibrary.getPath());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void addFilteredGlibcPathSegments(LinkedHashSet<String> segments, String pathList) {
        if (segments == null || pathList == null || pathList.trim().isEmpty()) return;
        String[] parts = pathList.split(":");
        for (String part : parts) {
            String path = part == null ? "" : part.trim();
            if (path.isEmpty()) continue;
            String lower = path.toLowerCase(Locale.US);
            if (lower.startsWith("/system/") || lower.startsWith("/apex/")) continue;
            if (lower.contains("/android-host")) continue;
            segments.add(path);
        }
    }

    private void addPathIfDirectory(LinkedHashSet<String> segments, File directory) {
        if (segments != null && directory != null && directory.isDirectory()) {
            segments.add(directory.getPath());
        }
    }

    private File resolveBox64RcFile(File rootDir) {
        if (rootDir == null) return null;
        File rcFile = new File(rootDir, "etc/config.box64rc");
        if (rcFile.isFile()) return rcFile;
        File fallbackRc = new File(rootDir, "usr/etc/config.box64rc");
        return fallbackRc.isFile() ? fallbackRc : rcFile;
    }

    private String summarizePreloadHead(String preload, int limit) {
        if (preload == null || preload.trim().isEmpty()) return "";
        String normalized = preload.trim().replace(' ', ':');
        return summarizePathHead(normalized, limit);
    }

    private String summarizeList(ArrayList<String> values, int limit) {
        if (values == null || values.isEmpty()) return "";
        int max = Math.max(0, limit);
        if (max == 0) return "";
        ArrayList<String> sample = new ArrayList<>();
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) continue;
            sample.add(value.trim());
            if (sample.size() >= max) break;
        }
        return String.join(" | ", sample);
    }

    private static final class GlibcPreloadContract {
        boolean redirectPresent;
        boolean redirectFilePresent;
        boolean redirectAbiCompatible;
        boolean sysvshmPresent;
        boolean sysvshmFilePresent;
        boolean sysvshmAbiCompatible;
        boolean skippedIncompatiblePreload;
        boolean hasPreload;
        String ldPreload = "";
    }

    private static final class GlibcProotContract {
        boolean available;
        boolean shouldUse;
        boolean hostBoundInterpreter;
        boolean prootPresent;
        boolean loaderPresent;
        boolean tmpPresent;
        String rootPath = "";
        String prootPath = "";
        String loaderPath = "";
        String tmpPath = "";
    }
}
