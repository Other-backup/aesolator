package com.winlator.cmod.core;

import android.content.Context;
import android.util.Log;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.xenvironment.ImageFs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class WineUtils {
    private static final String X11_DRIVER_REGISTRY_KEY = "Software\\Wine\\X11 Driver";
    private static final String X11_APP_DEFAULTS_REGISTRY_PREFIX = "Software\\Wine\\AppDefaults";
    private static final String[] X11_OPENGL_BACKEND_APP_DEFAULTS = new String[] {
            "explorer.exe",
            "wfm.exe",
            "winhandler.exe",
            "wineboot.exe",
            "winecfg.exe",
            "regedit.exe",
            "start.exe"
    };

    public static String buildExplorerDesktopShellCommand(String screenInfo, String payloadCommand) {
        String geometry = screenInfo == null ? "" : screenInfo.trim();
        if (geometry.isEmpty()) throw new IllegalArgumentException("desktop shell geometry is required");

        String payload = payloadCommand == null ? "" : payloadCommand.trim();
        if (payload.isEmpty()) payload = buildExplorerDesktopShellPayload("explorer.exe");
        return "wine explorer /desktop=shell," + geometry + " " + payload;
    }

    public static String buildExplorerDesktopShellPayload(String executableName) {
        return "\"" + canonicalDesktopShellExecutableName(executableName, "explorer.exe") + "\"";
    }

    public static String buildWinHandlerDesktopShellPayload(String handlerExecutable, String shellExecutable) {
        return canonicalDesktopShellExecutableName(handlerExecutable, "winhandler.exe")
                + " \""
                + canonicalDesktopShellExecutableName(shellExecutable, "wfm.exe")
                + "\"";
    }

    public static String canonicalDesktopShellExecutableName(String executableName, String fallbackName) {
        String fallback = fallbackName == null || fallbackName.trim().isEmpty()
                ? "explorer.exe"
                : fallbackName.trim();
        String normalized = executableName == null ? "" : executableName.trim();
        if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        if (normalized.isEmpty()) return fallback;

        int windowsSeparator = normalized.lastIndexOf('\\');
        int unixSeparator = normalized.lastIndexOf('/');
        int separator = Math.max(windowsSeparator, unixSeparator);
        if (separator >= 0 && separator + 1 < normalized.length()) {
            normalized = normalized.substring(separator + 1).trim();
        }
        return normalized.isEmpty() ? fallback : normalized;
    }

    public static final class WindowsLaunchTarget {
        public final String rawCommand;
        public final String commandPath;
        public final String commandArgs;
        public final String workingDir;
        public final File hostTargetFile;
        public final File hostTargetDir;

        private WindowsLaunchTarget(
                String rawCommand,
                String commandPath,
                String commandArgs,
                String workingDir,
                File hostTargetFile,
                File hostTargetDir
        ) {
            this.rawCommand = rawCommand;
            this.commandPath = commandPath;
            this.commandArgs = commandArgs;
            this.workingDir = workingDir;
            this.hostTargetFile = hostTargetFile;
            this.hostTargetDir = hostTargetDir;
        }

        public boolean hasCommandPath() {
            return !commandPath.isEmpty();
        }

        public boolean isShortcutLink() {
            return commandPath.toLowerCase(Locale.ROOT).endsWith(".lnk");
        }

        public String getExecutableName() {
            return hasCommandPath() ? FileUtils.getName(commandPath) : "";
        }
    }

    public static final class RuntimeLayout {
        public final File runtimeRootDir;
        public final File binDir;
        public final File libDir;
        public final File wineLibDir;
        public final File prefixPack;

        private RuntimeLayout(File runtimeRootDir, File binDir, File libDir, File wineLibDir, File prefixPack) {
            this.runtimeRootDir = runtimeRootDir;
            this.binDir = binDir;
            this.libDir = libDir;
            this.wineLibDir = wineLibDir;
            this.prefixPack = prefixPack;
        }

        public boolean isComplete() {
            return hasCorePayload()
                    && prefixPack != null
                    && prefixPack.isFile();
        }

        public boolean hasCorePayload() {
            return runtimeRootDir != null
                    && runtimeRootDir.isDirectory()
                    && binDir != null
                    && binDir.isDirectory()
                    && new File(binDir, "wine").isFile()
                    && libDir != null
                    && libDir.isDirectory()
                    && wineLibDir != null
                    && wineLibDir.isDirectory();
        }
    }

    public static final class RuntimeAbiContract {
        public final boolean required;
        public final boolean complete;
        public final String arch;
        public final String runtimeModel;
        public final String reason;
        public final String missing;
        public final String runtimeRootPath;
        public final String wineBinaryPath;
        public final String wineUnixDirPath;
        public final String wineWindowsDirPath;
        public final String glibcLoaderPath;
        public final String glibcLibcPath;
        public final String glibcLoaderRejectedPath;
        public final String glibcLibcRejectedPath;
        public final String glibcGuestLoaderMode;
        public final String glibcGuestSupportPath;
        public final String glibcGuestSupportRejectedPath;

        private RuntimeAbiContract(
                boolean required,
                boolean complete,
                String arch,
                String runtimeModel,
                String reason,
                String missing,
                String runtimeRootPath,
                String wineBinaryPath,
                String wineUnixDirPath,
                String wineWindowsDirPath,
                String glibcLoaderPath,
                String glibcLibcPath,
                String glibcLoaderRejectedPath,
                String glibcLibcRejectedPath,
                String glibcGuestLoaderMode,
                String glibcGuestSupportPath,
                String glibcGuestSupportRejectedPath
        ) {
            this.required = required;
            this.complete = complete;
            this.arch = arch;
            this.runtimeModel = runtimeModel;
            this.reason = reason;
            this.missing = missing;
            this.runtimeRootPath = runtimeRootPath;
            this.wineBinaryPath = wineBinaryPath;
            this.wineUnixDirPath = wineUnixDirPath;
            this.wineWindowsDirPath = wineWindowsDirPath;
            this.glibcLoaderPath = glibcLoaderPath;
            this.glibcLibcPath = glibcLibcPath;
            this.glibcLoaderRejectedPath = glibcLoaderRejectedPath;
            this.glibcLibcRejectedPath = glibcLibcRejectedPath;
            this.glibcGuestLoaderMode = glibcGuestLoaderMode;
            this.glibcGuestSupportPath = glibcGuestSupportPath;
            this.glibcGuestSupportRejectedPath = glibcGuestSupportRejectedPath;
        }
    }

    private static final class ExpectedElfAbi {
        final int elfClass;
        final int machine;

        ExpectedElfAbi(int elfClass, int machine) {
            this.elfClass = elfClass;
            this.machine = machine;
        }
    }

    private static final class ElfHeader {
        final boolean valid;
        final int elfClass;
        final int machine;
        final String reason;

        private ElfHeader(boolean valid, int elfClass, int machine, String reason) {
            this.valid = valid;
            this.elfClass = elfClass;
            this.machine = machine;
            this.reason = reason;
        }

        static ElfHeader invalid(String reason) {
            return new ElfHeader(false, -1, -1, reason);
        }

        static ElfHeader valid(int elfClass, int machine) {
            return new ElfHeader(true, elfClass, machine, "");
        }
    }

    private static final class AbiFileResolution {
        final File file;
        final String rejected;

        AbiFileResolution(File file, String rejected) {
            this.file = file;
            this.rejected = rejected == null ? "" : rejected;
        }
    }

    private static final String[] RUNTIME_BIN_DIR_CANDIDATES = {
            "bin",
            "arm64-v8a/bin"
    };
    private static final String[] RUNTIME_LIB_DIR_CANDIDATES = {
            "lib",
            "arm64-v8a/lib"
    };
    private static final String[] RUNTIME_WINE_UNIX_DIR_CANDIDATES = {
            "aarch64-unix",
            "x86_64-unix",
            "i386-unix"
    };
    private static final String[] RUNTIME_WINE_WINDOWS_DIR_CANDIDATES = {
            "aarch64-windows",
            "x86_64-windows",
            "i386-windows"
    };
    private static final String[] RUNTIME_X86_64_GLIBC_LOADER_CANDIDATES = {
            "usr/lib/x86_64-linux-gnu/ld-linux-x86-64.so.2",
            "lib/x86_64-linux-gnu/ld-linux-x86-64.so.2",
            "lib64/ld-linux-x86-64.so.2",
            "lib/ld-linux-x86-64.so.2"
    };
    private static final String[] RUNTIME_X86_64_GLIBC_LIBC_CANDIDATES = {
            "usr/lib/x86_64-linux-gnu/libc.so.6",
            "lib/x86_64-linux-gnu/libc.so.6",
            "lib64/libc.so.6",
            "lib/libc.so.6"
    };
    private static final String[] RUNTIME_X86_64_BOX64_CANDIDATES = {
            "usr/local/bin/box64",
            "usr/bin/box64",
            "bin/box64"
    };
    private static final String[] RUNTIME_X86_64_GLIBC_SUPPORT_CANDIDATES = {
            "usr/lib/x86_64-linux-gnu/libgcc_s.so.1",
            "usr/local/lib/x86_64-linux-gnu/libgcc_s.so.1",
            "lib/x86_64-linux-gnu/libgcc_s.so.1",
            "lib64/libgcc_s.so.1",
            "lib/libgcc_s.so.1"
    };
    private static final String[] RUNTIME_X86_GLIBC_LOADER_CANDIDATES = {
            "usr/lib/i386-linux-gnu/ld-linux.so.2",
            "usr/lib/i686-linux-gnu/ld-linux.so.2",
            "lib/i386-linux-gnu/ld-linux.so.2",
            "lib/i686-linux-gnu/ld-linux.so.2",
            "lib/ld-linux.so.2"
    };
    private static final String[] RUNTIME_X86_GLIBC_LIBC_CANDIDATES = {
            "usr/lib/i386-linux-gnu/libc.so.6",
            "usr/lib/i686-linux-gnu/libc.so.6",
            "lib/i386-linux-gnu/libc.so.6",
            "lib/i686-linux-gnu/libc.so.6",
            "lib/libc.so.6"
    };
    private static final String[] RUNTIME_AARCH64_GLIBC_LOADER_CANDIDATES = {
            "usr/lib/ld-linux-aarch64.so.1",
            "usr/lib/aarch64-linux-gnu/ld-linux-aarch64.so.1",
            "lib/aarch64-linux-gnu/ld-linux-aarch64.so.1",
            "lib64/ld-linux-aarch64.so.1",
            "lib/ld-linux-aarch64.so.1"
    };
    private static final String[] RUNTIME_AARCH64_GLIBC_LIBC_CANDIDATES = {
            "usr/lib/libc.so.6",
            "usr/lib/aarch64-linux-gnu/libc.so.6",
            "lib/aarch64-linux-gnu/libc.so.6",
            "lib64/libc.so.6",
            "lib/libc.so.6"
    };
    private static final String[] RUNTIME_SHARE_DIR_CANDIDATES = {
            "share",
            "arm64-v8a/share"
    };
    private static final String[] RUNTIME_PREFIX_PACK_CANDIDATES = {
            "prefixPack.txz",
            "prefixPack.tzst",
            "arm64-v8a/prefixPack.txz",
            "arm64-v8a/prefixPack.tzst"
    };
    private static final String[] ANDROID_GRAPHICS_DRIVER_SENTINELS = {
            "arm64-v8a/lib/wine/aarch64-unix/wineandroid.so",
            "arm64-v8a/lib/wine/aarch64-windows/wineandroid.drv",
            "lib/wine/aarch64-unix/wineandroid.so",
            "lib/wine/aarch64-windows/wineandroid.drv"
    };
    private static final String[] X11_GRAPHICS_DRIVER_SENTINELS = {
            "arm64-v8a/lib/wine/aarch64-unix/winex11.so",
            "arm64-v8a/lib/wine/aarch64-windows/winex11.drv",
            "lib/wine/aarch64-unix/winex11.so",
            "lib/wine/aarch64-windows/winex11.drv"
    };
    private static final String[] WINDOWS_COMMAND_EXECUTABLE_EXTENSIONS = {
            ".exe",
            ".lnk",
            ".bat",
            ".cmd",
            ".com",
            ".msi"
    };
    private static final Pattern WINE_EXEC_TOKEN_PATTERN = Pattern.compile(
            "(?:^|\\s)(?:[^\\s\"']*[/\\\\])?wine(?:64)?\\s+(.+)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final int ELF_HEADER_MACHINE_OFFSET = 18;
    private static final int ELF_HEADER_MIN_BYTES = 20;
    private static final int ELFCLASS32 = 1;
    private static final int ELFCLASS64 = 2;
    private static final int ELFDATA2LSB = 1;
    private static final int ELFDATA2MSB = 2;
    private static final int EM_386 = 3;
    private static final int EM_X86_64 = 62;
    private static final int EM_AARCH64 = 183;

    public static File resolveHostWinePrefixDir(File rootDir) {
        if (rootDir == null) return new File("/nonexistent");

        if (new File(rootDir, "drive_c").isDirectory()) return rootDir;

        File containerStyle = new File(rootDir, ".wine");
        if (new File(containerStyle, "drive_c").isDirectory()) return containerStyle;

        File direct = new File(rootDir, ImageFs.WINEPREFIX.substring(1));
        if (new File(direct, "drive_c").isDirectory()) return direct;

        File homeDir = new File(rootDir, "home");
        File[] homeEntries = homeDir.listFiles();
        if (homeEntries != null) {
            for (File entry : homeEntries) {
                if (entry == null || !entry.isDirectory()) continue;
                File candidate = new File(entry, ".wine");
                if (new File(candidate, "drive_c").isDirectory()) return candidate;
            }
        }

        return direct;
    }

    public static File resolveHostWineDriveCRoot(File rootDir) {
        return new File(resolveHostWinePrefixDir(rootDir), "drive_c");
    }

    public static File resolveRuntimeBinDir(File runtimeRootDir) {
        return resolveExistingSubdir(runtimeRootDir, RUNTIME_BIN_DIR_CANDIDATES);
    }

    public static File resolveRuntimeLibDir(File runtimeRootDir) {
        return resolveExistingSubdir(runtimeRootDir, RUNTIME_LIB_DIR_CANDIDATES);
    }

    public static File resolveRuntimeWineLibDir(File runtimeRootDir) {
        File libDir = resolveRuntimeLibDir(runtimeRootDir);
        if (libDir == null) return null;
        File wineLibDir = new File(libDir, "wine");
        return wineLibDir.isDirectory() ? wineLibDir : null;
    }

    public static File resolveRuntimeWineUnixDir(File runtimeRootDir) {
        File[] wineUnixDirs = resolveRuntimeWineUnixDirs(runtimeRootDir);
        return wineUnixDirs.length > 0 ? wineUnixDirs[0] : null;
    }

    public static File resolveRuntimeWineUnixDir(File runtimeRootDir, WineInfo wineInfo) {
        File wineLibDir = resolveRuntimeWineLibDir(runtimeRootDir);
        if (wineLibDir == null) return null;

        String preferredName = getRuntimeWineUnixDirName(wineInfo);
        if (preferredName != null) {
            File preferredDir = new File(wineLibDir, preferredName);
            if (preferredDir.isDirectory()) return preferredDir;
        }

        return resolveRuntimeWineUnixDir(runtimeRootDir);
    }

    public static File resolveRuntimeWineWindowsDir(File runtimeRootDir, WineInfo wineInfo) {
        File wineLibDir = resolveRuntimeWineLibDir(runtimeRootDir);
        if (wineLibDir == null) return null;

        String preferredName = getRuntimeWineWindowsDirName(wineInfo);
        if (preferredName != null) {
            File preferredDir = new File(wineLibDir, preferredName);
            if (preferredDir.isDirectory()) return preferredDir;
        }

        File[] wineWindowsDirs = resolveRuntimeWineWindowsDirs(runtimeRootDir);
        return wineWindowsDirs.length > 0 ? wineWindowsDirs[0] : null;
    }

    public static File[] resolveRuntimeWineUnixDirs(File runtimeRootDir) {
        File wineLibDir = resolveRuntimeWineLibDir(runtimeRootDir);
        if (wineLibDir == null) return new File[0];

        File[] resolved = new File[RUNTIME_WINE_UNIX_DIR_CANDIDATES.length];
        int count = 0;
        for (String name : RUNTIME_WINE_UNIX_DIR_CANDIDATES) {
            File wineUnixDir = new File(wineLibDir, name);
            if (wineUnixDir.isDirectory()) {
                resolved[count++] = wineUnixDir;
            }
        }
        if (count == resolved.length) return resolved;

        File[] compact = new File[count];
        System.arraycopy(resolved, 0, compact, 0, count);
        return compact;
    }

    public static File[] resolveRuntimeWineWindowsDirs(File runtimeRootDir) {
        File wineLibDir = resolveRuntimeWineLibDir(runtimeRootDir);
        if (wineLibDir == null) return new File[0];

        File[] resolved = new File[RUNTIME_WINE_WINDOWS_DIR_CANDIDATES.length];
        int count = 0;
        for (String name : RUNTIME_WINE_WINDOWS_DIR_CANDIDATES) {
            File wineWindowsDir = new File(wineLibDir, name);
            if (wineWindowsDir.isDirectory()) {
                resolved[count++] = wineWindowsDir;
            }
        }
        if (count == resolved.length) return resolved;

        File[] compact = new File[count];
        System.arraycopy(resolved, 0, compact, 0, count);
        return compact;
    }

    private static String getRuntimeWineUnixDirName(WineInfo wineInfo) {
        if (wineInfo == null || wineInfo.getArch() == null) return null;
        switch (wineInfo.getArch().trim().toLowerCase(Locale.ROOT)) {
            case "x86_64":
                return "x86_64-unix";
            case "x86":
                return "i386-unix";
            case "arm64":
            case "arm64ec":
                return "aarch64-unix";
            default:
                return null;
        }
    }

    private static String getRuntimeWineWindowsDirName(WineInfo wineInfo) {
        if (wineInfo == null || wineInfo.getArch() == null) return null;
        switch (wineInfo.getArch().trim().toLowerCase(Locale.ROOT)) {
            case "x86_64":
                return "x86_64-windows";
            case "x86":
                return "i386-windows";
            case "arm64":
            case "arm64ec":
                return "aarch64-windows";
            default:
                return null;
        }
    }

    public static RuntimeAbiContract validateRuntimeAbiContract(
            File imageFsRootDir,
            File runtimeRootDir,
            WineInfo wineInfo,
            String runtimeModel
    ) {
        String normalizedRuntimeModel = runtimeModel == null ? "" : runtimeModel.trim().toLowerCase(Locale.ROOT);
        String arch = wineInfo != null && wineInfo.getArch() != null
                ? wineInfo.getArch().trim().toLowerCase(Locale.ROOT)
                : "";
        File wineBinary = resolveRuntimeWineBinary(runtimeRootDir);
        File wineUnixDir = resolveRuntimeWineUnixDir(runtimeRootDir, wineInfo);
        File wineWindowsDir = resolveRuntimeWineWindowsDir(runtimeRootDir, wineInfo);
        boolean glibcRuntime = "glibc".equals(normalizedRuntimeModel);
        boolean glibcAbiRequired = glibcRuntime
                && ("x86_64".equals(arch)
                || "x86".equals(arch)
                || "arm64".equals(arch)
                || "arm64ec".equals(arch));

        String missing = "";
        if (wineBinary == null || !wineBinary.isFile()) missing = appendMissing(missing, "wine_binary");
        if (wineUnixDir == null || !wineUnixDir.isDirectory()) missing = appendMissing(missing, "wine_unix_dir:" + nullToEmpty(getRuntimeWineUnixDirName(wineInfo)));
        if (wineWindowsDir == null || !wineWindowsDir.isDirectory()) missing = appendMissing(missing, "wine_windows_dir:" + nullToEmpty(getRuntimeWineWindowsDirName(wineInfo)));

        File glibcLoader = null;
        File glibcLibc = null;
        AbiFileResolution glibcLoaderResolution = new AbiFileResolution(null, "");
        AbiFileResolution glibcLibcResolution = new AbiFileResolution(null, "");
        AbiFileResolution glibcGuestSupportResolution = new AbiFileResolution(null, "");
        String glibcGuestLoaderMode = "";
        File glibcGuestSupport = null;
        if (glibcAbiRequired) {
            if ("x86_64".equals(arch)) {
                AbiFileResolution box64Resolution = resolveExistingRelativeAbiFile(
                        imageFsRootDir,
                        RUNTIME_X86_64_BOX64_CANDIDATES,
                        null
                );
                glibcGuestSupportResolution = resolveExistingRelativeAbiFile(
                        imageFsRootDir,
                        RUNTIME_X86_64_GLIBC_SUPPORT_CANDIDATES,
                        expectedGlibcElfAbi(arch)
                );
                glibcGuestSupport = glibcGuestSupportResolution.file;
                if (box64Resolution.file == null || !box64Resolution.file.isFile()) {
                    missing = appendMissing(missing, "x86_64_box64_launcher");
                } else {
                    glibcGuestLoaderMode = "box64_wrapped";
                    glibcLoader = box64Resolution.file;
                }
                if (glibcGuestSupport == null || !glibcGuestSupport.isFile()) {
                    missing = appendMissing(missing, "x86_64_glibc_support_libgcc");
                }
            } else {
                glibcLoaderResolution = resolveGlibcAbiFile(imageFsRootDir, runtimeRootDir, arch, true);
                glibcLibcResolution = resolveGlibcAbiFile(imageFsRootDir, runtimeRootDir, arch, false);
                glibcLoader = glibcLoaderResolution.file;
                glibcLibc = glibcLibcResolution.file;
                if (glibcLoader == null || !glibcLoader.isFile()) missing = appendMissing(missing, arch + "_glibc_loader");
                if (glibcLibc == null || !glibcLibc.isFile()) missing = appendMissing(missing, arch + "_glibc_libc");
            }
        }

        boolean required = glibcAbiRequired
                || wineBinary != null
                || wineUnixDir != null
                || wineWindowsDir != null;
        boolean complete = missing.isEmpty();
        String reason = complete
                ? "runtime_abi_contract_satisfied"
                : "runtime_abi_contract_missing_" + missing.replace(',', '_');
        return new RuntimeAbiContract(
                required,
                complete,
                arch,
                normalizedRuntimeModel,
                reason,
                missing,
                runtimeRootDir != null ? runtimeRootDir.getAbsolutePath() : "",
                wineBinary != null ? wineBinary.getAbsolutePath() : "",
                wineUnixDir != null ? wineUnixDir.getAbsolutePath() : "",
                wineWindowsDir != null ? wineWindowsDir.getAbsolutePath() : "",
                glibcLoader != null ? glibcLoader.getAbsolutePath() : "",
                glibcLibc != null ? glibcLibc.getAbsolutePath() : "",
                glibcLoaderResolution.rejected,
                glibcLibcResolution.rejected,
                glibcGuestLoaderMode,
                glibcGuestSupport != null ? glibcGuestSupport.getAbsolutePath() : "",
                glibcGuestSupportResolution.rejected
        );
    }

    private static AbiFileResolution resolveGlibcAbiFile(File imageFsRootDir, File runtimeRootDir, String arch, boolean loader) {
        String[] candidates;
        switch (arch == null ? "" : arch) {
            case "x86_64":
                candidates = loader ? RUNTIME_X86_64_GLIBC_LOADER_CANDIDATES : RUNTIME_X86_64_GLIBC_LIBC_CANDIDATES;
                break;
            case "x86":
                candidates = loader ? RUNTIME_X86_GLIBC_LOADER_CANDIDATES : RUNTIME_X86_GLIBC_LIBC_CANDIDATES;
                break;
            case "arm64":
            case "arm64ec":
                candidates = loader ? RUNTIME_AARCH64_GLIBC_LOADER_CANDIDATES : RUNTIME_AARCH64_GLIBC_LIBC_CANDIDATES;
                break;
            default:
                return new AbiFileResolution(null, "");
        }

        ExpectedElfAbi expectedAbi = expectedGlibcElfAbi(arch);
        AbiFileResolution runtimeFound = resolveExistingRelativeAbiFile(runtimeRootDir, candidates, expectedAbi);
        if (runtimeFound.file != null) return runtimeFound;

        AbiFileResolution imageFound = resolveExistingRelativeAbiFile(imageFsRootDir, candidates, expectedAbi);
        return new AbiFileResolution(
                imageFound.file,
                appendRejected(runtimeFound.rejected, imageFound.rejected)
        );
    }

    private static ExpectedElfAbi expectedGlibcElfAbi(String arch) {
        switch (arch == null ? "" : arch) {
            case "x86_64":
                return new ExpectedElfAbi(ELFCLASS64, EM_X86_64);
            case "x86":
                return new ExpectedElfAbi(ELFCLASS32, EM_386);
            case "arm64":
            case "arm64ec":
                return new ExpectedElfAbi(ELFCLASS64, EM_AARCH64);
            default:
                return null;
        }
    }

    private static AbiFileResolution resolveExistingRelativeAbiFile(File rootDir, String[] relativePaths, ExpectedElfAbi expectedAbi) {
        if (rootDir == null || relativePaths == null) return new AbiFileResolution(null, "");

        String rejected = "";
        for (String relativePath : relativePaths) {
            File candidate = new File(rootDir, relativePath);
            if (!candidate.isFile()) continue;

            String mismatch = describeElfAbiMismatch(candidate, expectedAbi);
            if (mismatch.isEmpty()) {
                return new AbiFileResolution(candidate, rejected);
            }
            rejected = appendRejected(rejected, mismatch);
        }
        return new AbiFileResolution(null, rejected);
    }

    private static String describeElfAbiMismatch(File candidate, ExpectedElfAbi expectedAbi) {
        if (expectedAbi == null) return "";

        ElfHeader header = readElfHeader(candidate);
        String path = candidate == null ? "" : candidate.getAbsolutePath();
        if (!header.valid) {
            return path + ":invalid_elf:" + header.reason;
        }
        if (header.elfClass == expectedAbi.elfClass && header.machine == expectedAbi.machine) {
            return "";
        }
        return path
                + ":class=" + header.elfClass
                + ":machine=" + header.machine
                + ":expectedClass=" + expectedAbi.elfClass
                + ":expectedMachine=" + expectedAbi.machine;
    }

    private static ElfHeader readElfHeader(File file) {
        if (file == null || !file.isFile()) return ElfHeader.invalid("missing_file");

        byte[] header = new byte[ELF_HEADER_MIN_BYTES];
        int total = 0;
        try (FileInputStream input = new FileInputStream(file)) {
            while (total < header.length) {
                int read = input.read(header, total, header.length - total);
                if (read < 0) break;
                total += read;
            }
        }
        catch (IOException e) {
            return ElfHeader.invalid("read_failed");
        }

        if (total < ELF_HEADER_MIN_BYTES) return ElfHeader.invalid("short_header");
        if ((header[0] & 0xff) != 0x7f || header[1] != 'E' || header[2] != 'L' || header[3] != 'F') {
            return ElfHeader.invalid("bad_magic");
        }

        int elfClass = header[4] & 0xff;
        int dataEncoding = header[5] & 0xff;
        int machineLow = header[ELF_HEADER_MACHINE_OFFSET] & 0xff;
        int machineHigh = header[ELF_HEADER_MACHINE_OFFSET + 1] & 0xff;
        int machine;
        if (dataEncoding == ELFDATA2LSB) {
            machine = machineLow | (machineHigh << 8);
        }
        else if (dataEncoding == ELFDATA2MSB) {
            machine = (machineLow << 8) | machineHigh;
        }
        else {
            return ElfHeader.invalid("unsupported_data_encoding:" + dataEncoding);
        }
        return ElfHeader.valid(elfClass, machine);
    }

    private static File resolveExistingRelativeFile(File rootDir, String[] relativePaths) {
        if (rootDir == null || relativePaths == null) return null;
        for (String relativePath : relativePaths) {
            File candidate = new File(rootDir, relativePath);
            if (candidate.isFile()) return candidate;
        }
        return null;
    }

    private static String appendMissing(String current, String value) {
        if (value == null || value.trim().isEmpty()) return current == null ? "" : current;
        if (current == null || current.isEmpty()) return value;
        return current + "," + value;
    }

    private static String appendRejected(String current, String value) {
        if (value == null || value.trim().isEmpty()) return current == null ? "" : current;
        if (current == null || current.isEmpty()) return value;
        return current + ";" + value;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public static File resolveRuntimeWineBinary(File runtimeRootDir) {
        File binDir = resolveRuntimeBinDir(runtimeRootDir);
        if (binDir == null) return null;
        File wineBinary = new File(binDir, "wine");
        return wineBinary.isFile() ? wineBinary : null;
    }

    public static File resolveRuntimeShareDir(File runtimeRootDir) {
        return resolveExistingSubdir(runtimeRootDir, RUNTIME_SHARE_DIR_CANDIDATES);
    }

    public static File resolveRuntimePrefixPack(File runtimeRootDir) {
        return resolveExistingFile(runtimeRootDir, RUNTIME_PREFIX_PACK_CANDIDATES);
    }

    public static boolean isRegistryFileValid(File regFile) {
        if (regFile == null || !regFile.isFile() || regFile.length() < 24) return false;
        String contents = FileUtils.readString(regFile);
        if (contents == null) return false;

        String normalized = contents.trim();
        return normalized.startsWith("WINE REGISTRY Version");
    }

    public static boolean isPrefixValid(File containerDir) {
        if (containerDir == null) return false;

        File prefixDir = new File(containerDir, ".wine");
        File systemRegFile = new File(prefixDir, "system.reg");
        File userRegFile = new File(prefixDir, "user.reg");
        File windowsDir = new File(prefixDir, "drive_c/windows");

        return prefixDir.isDirectory()
                && windowsDir.isDirectory()
                && isRegistryFileValid(systemRegFile)
                && isRegistryFileValid(userRegFile);
    }

    public static String extractWindowsCommandPath(String command) {
        return parseWindowsCommand(command).commandPath;
    }

    public static String extractWindowsCommandArgs(String command) {
        return parseWindowsCommand(command).commandArgs;
    }

    public static String extractWineExecPayload(String execLine) {
        String rawExecLine = execLine == null ? "" : execLine.trim();
        if (rawExecLine.isEmpty()) return "";

        Matcher matcher = WINE_EXEC_TOKEN_PATTERN.matcher(rawExecLine);
        String payload = matcher.find() ? matcher.group(1).trim() : rawExecLine;
        while (payload.contains("\\\\")) {
            payload = payload.replace("\\\\", "\\");
        }
        return payload;
    }

    public static String resolveWindowsParentDir(String windowsPath) {
        String normalized = normalizeWindowsPathSeparators(windowsPath);
        if (normalized.length() < 2 || normalized.charAt(1) != ':') return "";

        int separator = normalized.lastIndexOf('\\');
        if (separator <= 2) return normalized.substring(0, Math.min(normalized.length(), 3));
        return normalized.substring(0, separator);
    }

    public static File resolveHostPathFromWindowsPath(File rootDir, String windowsPath) {
        String normalizedPath = normalizeWindowsPathSeparators(windowsPath);
        if (rootDir == null || normalizedPath.length() < 2 || normalizedPath.charAt(1) != ':') return null;

        char driveLetter = Character.toLowerCase(normalizedPath.charAt(0));
        String relativePath = normalizedPath.substring(2).replace('\\', '/');
        while (relativePath.startsWith("/")) relativePath = relativePath.substring(1);

        File baseDir;
        if (driveLetter == 'c') {
            baseDir = resolveHostWineDriveCRoot(rootDir);
        } else {
            baseDir = resolveHostDosdevicesDriveRoot(rootDir, driveLetter);
            if (baseDir == null) return null;
        }

        return relativePath.isEmpty() ? baseDir : new File(baseDir, relativePath);
    }

    public static WindowsLaunchTarget resolveWindowsLaunchTarget(File rootDir, String command) {
        ParsedWindowsCommand parsedCommand = parseWindowsCommand(command);
        File hostTargetFile = resolveHostPathFromWindowsPath(rootDir, parsedCommand.commandPath);
        File hostTargetDir = null;
        if (hostTargetFile != null) {
            hostTargetDir = hostTargetFile.isDirectory() ? hostTargetFile : hostTargetFile.getParentFile();
        }
        return new WindowsLaunchTarget(
                parsedCommand.rawCommand,
                parsedCommand.commandPath,
                parsedCommand.commandArgs,
                resolveWindowsParentDir(parsedCommand.commandPath),
                hostTargetFile,
                hostTargetDir
        );
    }

    public static WindowsLaunchTarget remapWindowsLaunchTarget(
            File rootDir,
            WindowsLaunchTarget original,
            File hostTargetFile
    ) {
        if (rootDir == null || original == null || hostTargetFile == null) return original;

        File hostTargetDir = hostTargetFile.isDirectory() ? hostTargetFile : hostTargetFile.getParentFile();
        String commandPath = resolveWindowsPathFromHostPath(rootDir, hostTargetFile);
        if (commandPath.isEmpty()) return original;

        String workingDir = resolveWindowsPathFromHostPath(rootDir, hostTargetDir);
        if (workingDir.isEmpty()) workingDir = resolveWindowsParentDir(commandPath);

        return new WindowsLaunchTarget(
                original.rawCommand,
                commandPath,
                original.commandArgs,
                workingDir,
                hostTargetFile,
                hostTargetDir
        );
    }

    public static String resolveWindowsPathFromHostPath(File rootDir, File hostPath) {
        if (rootDir == null || hostPath == null) return "";

        File canonicalHost = canonicalFileOrNull(hostPath);
        if (canonicalHost == null) return "";

        String relativePath = relativePathWithinRoot(resolveHostWineDriveCRoot(rootDir), canonicalHost);
        if (relativePath != null) return buildWindowsPath('C', relativePath);

        for (char driveLetter = 'a'; driveLetter <= 'z'; driveLetter++) {
            if (driveLetter == 'c') continue;
            File driveRoot = resolveHostDosdevicesDriveRoot(rootDir, driveLetter);
            relativePath = relativePathWithinRoot(driveRoot, canonicalHost);
            if (relativePath != null) {
                return buildWindowsPath(Character.toUpperCase(driveLetter), relativePath);
            }
        }

        return "";
    }

    private static String normalizeWindowsPathSeparators(String value) {
        String normalized = value == null ? "" : value.trim().replace('/', '\\');
        while (normalized.contains("\\\\")) normalized = normalized.replace("\\\\", "\\");
        return normalized;
    }

    private static ParsedWindowsCommand parseWindowsCommand(String command) {
        String rawCommand = command == null ? "" : command.trim();
        if (rawCommand.isEmpty()) return new ParsedWindowsCommand("", "", "");

        String rawPath;
        String commandArgs;
        if (rawCommand.startsWith("\"")) {
            int closingQuote = rawCommand.indexOf('"', 1);
            if (closingQuote > 1) {
                rawPath = rawCommand.substring(1, closingQuote);
                commandArgs = rawCommand.substring(closingQuote + 1).trim();
            } else {
                rawPath = rawCommand.substring(1);
                commandArgs = "";
            }
        } else {
            int boundary = resolveUnquotedCommandBoundary(rawCommand);
            rawPath = rawCommand.substring(0, boundary).trim();
            commandArgs = boundary < rawCommand.length() ? rawCommand.substring(boundary).trim() : "";
        }

        return new ParsedWindowsCommand(rawCommand, normalizeWindowsPathSeparators(rawPath), commandArgs);
    }

    private static int resolveUnquotedCommandBoundary(String rawCommand) {
        int executableBoundary = findExecutableBoundary(rawCommand);
        if (executableBoundary > 0) return executableBoundary;

        int firstSpace = rawCommand.indexOf(' ');
        return firstSpace > 0 ? firstSpace : rawCommand.length();
    }

    private static int findExecutableBoundary(String rawCommand) {
        String lowerCommand = rawCommand.toLowerCase(Locale.ROOT);
        for (String extension : WINDOWS_COMMAND_EXECUTABLE_EXTENSIONS) {
            int fromIndex = 0;
            while (fromIndex >= 0 && fromIndex < lowerCommand.length()) {
                int extensionIndex = lowerCommand.indexOf(extension, fromIndex);
                if (extensionIndex < 0) break;

                int boundary = extensionIndex + extension.length();
                if (boundary >= rawCommand.length() || Character.isWhitespace(rawCommand.charAt(boundary))) {
                    return boundary;
                }
                fromIndex = extensionIndex + 1;
            }
        }
        return -1;
    }

    private static final class ParsedWindowsCommand {
        private final String rawCommand;
        private final String commandPath;
        private final String commandArgs;

        private ParsedWindowsCommand(String rawCommand, String commandPath, String commandArgs) {
            this.rawCommand = rawCommand;
            this.commandPath = commandPath;
            this.commandArgs = commandArgs;
        }
    }

    private static File resolveHostDosdevicesDriveRoot(File rootDir, char driveLetter) {
        File resolved = canonicalDriveLink(new File(resolveHostWinePrefixDir(rootDir), "dosdevices/" + driveLetter + ":"));
        if (resolved != null) return resolved;

        resolved = canonicalDriveLink(new File(rootDir, ".wine/dosdevices/" + driveLetter + ":"));
        if (resolved != null) return resolved;

        resolved = canonicalDriveLink(new File(rootDir, ImageFs.WINEPREFIX.substring(1) + "/dosdevices/" + driveLetter + ":"));
        if (resolved != null) return resolved;

        File homeDir = new File(rootDir, "home");
        File[] homeEntries = homeDir.listFiles();
        if (homeEntries != null) {
            for (File entry : homeEntries) {
                if (entry == null || !entry.isDirectory()) continue;
                resolved = canonicalDriveLink(new File(entry, ".wine/dosdevices/" + driveLetter + ":"));
                if (resolved != null) return resolved;
            }
        }
        return null;
    }

    private static File canonicalDriveLink(File driveLink) {
        try {
            if (driveLink == null || !driveLink.exists()) return null;
            return driveLink.getCanonicalFile();
        } catch (IOException e) {
            return null;
        }
    }

    private static File canonicalFileOrNull(File file) {
        try {
            if (file == null || !file.exists()) return null;
            return file.getCanonicalFile();
        } catch (IOException e) {
            return null;
        }
    }

    private static String relativePathWithinRoot(File rootDir, File candidate) {
        File canonicalRoot = canonicalFileOrNull(rootDir);
        if (canonicalRoot == null || candidate == null) return null;

        String rootPath = canonicalRoot.getPath();
        String candidatePath = candidate.getPath();
        if (!candidatePath.equals(rootPath) && !candidatePath.startsWith(rootPath + File.separator)) return null;

        String relativePath = candidatePath.equals(rootPath)
                ? ""
                : candidatePath.substring(rootPath.length() + 1);
        return relativePath.replace(File.separatorChar, '\\');
    }

    private static String buildWindowsPath(char driveLetter, String relativePath) {
        String normalizedRelativePath = relativePath == null ? "" : relativePath.replace('/', '\\');
        while (normalizedRelativePath.startsWith("\\")) normalizedRelativePath = normalizedRelativePath.substring(1);
        return normalizedRelativePath.isEmpty()
                ? Character.toUpperCase(driveLetter) + ":\\"
                : Character.toUpperCase(driveLetter) + ":\\" + normalizedRelativePath;
    }

    public static RuntimeLayout resolveRuntimeLayout(File runtimeRootDir) {
        File binDir = resolveRuntimeBinDir(runtimeRootDir);
        File libDir = resolveRuntimeLibDir(runtimeRootDir);
        File wineLibDir = resolveRuntimeWineLibDir(runtimeRootDir);
        File prefixPack = resolveRuntimePrefixPack(runtimeRootDir);
        return new RuntimeLayout(runtimeRootDir, binDir, libDir, wineLibDir, prefixPack);
    }

    public static File resolveCanonicalRuntimeRoot(File runtimeRootDir) {
        if (runtimeRootDir == null) return null;

        File current = runtimeRootDir;
        File best = hasRuntimeCorePayload(current) ? current : null;
        for (int depth = 0; depth < 4 && current != null; depth++) {
            current = current.getParentFile();
            if (current != null && hasRuntimeCorePayload(current)) {
                best = current;
            }
        }
        return best != null ? best : runtimeRootDir;
    }

    public static boolean hasRuntimeCorePayload(File runtimeRootDir) {
        return resolveRuntimeLayout(runtimeRootDir).hasCorePayload();
    }

    public static boolean hasRuntimePayload(File runtimeRootDir) {
        return resolveRuntimeLayout(runtimeRootDir).isComplete();
    }

    public static void ensureGraphicsDriverRegistry(File rootDir, String driver) {
        String normalizedDriver = driver != null ? driver.trim() : "";
        if (normalizedDriver.isEmpty()) return;

        File prefixDir = resolveHostWinePrefixDir(rootDir);
        File[] registryFiles = new File[] {
                new File(prefixDir, "user.reg"),
                new File(prefixDir, "userdef.reg")
        };

        for (File registryFile : registryFiles) {
            try (WineRegistryEditor registryEditor = new WineRegistryEditor(registryFile)) {
                registryEditor.setStringValue("Software\\Wine\\Drivers", "Graphics", normalizedDriver);
            }
        }

        if (graphicsDriverIncludesX11(normalizedDriver)) {
            ensureX11OpenGlBackendRegistry(rootDir, true);
        }
    }

    public static boolean ensureX11OpenGlBackendRegistry(File rootDir, boolean includeUserDef) {
        File prefixDir = resolveHostWinePrefixDir(rootDir);
        File[] registryFiles = includeUserDef
                ? new File[] { new File(prefixDir, "user.reg"), new File(prefixDir, "userdef.reg") }
                : new File[] { new File(prefixDir, "user.reg") };
        boolean applied = false;

        for (File registryFile : registryFiles) {
            try (WineRegistryEditor registryEditor = new WineRegistryEditor(registryFile)) {
                applyX11OpenGlBackendRegistry(registryEditor);
                applied = true;
            }
        }

        return applied;
    }

    private static void applyX11OpenGlBackendRegistry(WineRegistryEditor registryEditor) {
        registryEditor.setStringValue(X11_DRIVER_REGISTRY_KEY, "UseEGL", "N");
        for (String appName : X11_OPENGL_BACKEND_APP_DEFAULTS) {
            registryEditor.setStringValue(
                    X11_APP_DEFAULTS_REGISTRY_PREFIX + "\\" + appName + "\\X11 Driver",
                    "UseEGL",
                    "N"
            );
        }
    }

    public static boolean graphicsDriverIncludesX11(String driver) {
        if (driver == null) return false;
        for (String part : driver.split(",")) {
            if ("x11".equalsIgnoreCase(part.trim())) return true;
        }
        return false;
    }

    public static String resolvePreferredGraphicsDriver(File runtimeRootDir) {
        return resolvePreferredGraphicsDriver(runtimeRootDir, null);
    }

    public static String resolvePreferredGraphicsDriver(File runtimeRootDir, WineInfo wineInfo) {
        if (runtimeRootDir == null || !runtimeRootDir.isDirectory()) return "";

        boolean hasAndroidSurface = runtimeContainsAny(runtimeRootDir, ANDROID_GRAPHICS_DRIVER_SENTINELS);
        boolean hasX11Surface = runtimeContainsAny(runtimeRootDir, X11_GRAPHICS_DRIVER_SENTINELS);
        if (hasX11Surface) return "x11";
        if (wineInfo != null && wineInfo.type != null
                && "proton".equals(wineInfo.type.trim().toLowerCase(Locale.ENGLISH))) {
            if (hasAndroidSurface) return "android";
            return "x11";
        }
        if (hasAndroidSurface) return "android";
        return "";
    }

    private static boolean runtimeContainsAny(File runtimeRootDir, String[] relativePaths) {
        if (runtimeRootDir == null || relativePaths == null) return false;
        for (String relativePath : relativePaths) {
            if (relativePath == null || relativePath.isEmpty()) continue;
            if (new File(runtimeRootDir, relativePath).isFile()) return true;
        }
        return false;
    }

    private static File resolveExistingSubdir(File rootDir, String[] candidates) {
        if (rootDir == null || candidates == null) return null;
        for (String candidatePath : candidates) {
            if (candidatePath == null || candidatePath.isEmpty()) continue;
            File candidate = new File(rootDir, candidatePath);
            if (candidate.isDirectory()) return candidate;
        }
        return null;
    }

    private static File resolveExistingFile(File rootDir, String[] candidates) {
        if (rootDir == null || candidates == null) return null;
        for (String candidatePath : candidates) {
            if (candidatePath == null || candidatePath.isEmpty()) continue;
            File candidate = new File(rootDir, candidatePath);
            if (candidate.isFile()) return candidate;
        }
        return null;
    }

    public static void createDosdevicesSymlinks(Container container) {
        String dosdevicesPath = (new File(container.getRootDir(), ".wine/dosdevices")).getPath();
        File[] files = (new File(dosdevicesPath)).listFiles();
        if (files != null) for (File file : files) if (file.getName().matches("[a-z]:")) file.delete();

        FileUtils.symlink("../drive_c", dosdevicesPath+"/c:");
        FileUtils.symlink(container.getRootDir().getPath() + "/../..", dosdevicesPath+"/z:");

        for (String[] drive : container.drivesIterator()) {
            File linkTarget = new File(drive[1]);
            String path = linkTarget.getAbsolutePath();
            if (!linkTarget.isDirectory() && path.endsWith("/com.winlator.cmod/storage")) {
                linkTarget.mkdirs();
                FileUtils.chmod(linkTarget, 0771);
            }
            FileUtils.symlink(path, dosdevicesPath+"/"+drive[0].toLowerCase(Locale.ENGLISH)+":");
        }
    }

    private static void setWindowMetrics(WineRegistryEditor registryEditor) {
        byte[] fontNormalData = (new MSLogFont()).toByteArray();
        byte[] fontBoldData = (new MSLogFont()).setWeight(700).toByteArray();
        registryEditor.setHexValue("Control Panel\\Desktop\\WindowMetrics", "CaptionFont", fontBoldData);
        registryEditor.setHexValue("Control Panel\\Desktop\\WindowMetrics", "IconFont", fontNormalData);
        registryEditor.setHexValue("Control Panel\\Desktop\\WindowMetrics", "MenuFont", fontNormalData);
        registryEditor.setHexValue("Control Panel\\Desktop\\WindowMetrics", "MessageFont", fontNormalData);
        registryEditor.setHexValue("Control Panel\\Desktop\\WindowMetrics", "SmCaptionFont", fontNormalData);
        registryEditor.setHexValue("Control Panel\\Desktop\\WindowMetrics", "StatusFont", fontNormalData);
    }

    public static void applySystemTweaks(Context context, WineInfo wineInfo) {
        File rootDir = ImageFs.find(context).getRootDir();
        File prefixDir = resolveHostWinePrefixDir(rootDir);
        File systemRegFile = new File(prefixDir, "system.reg");
        File userRegFile = new File(prefixDir, "user.reg");

        try (WineRegistryEditor registryEditor = new WineRegistryEditor(systemRegFile)) {
            registryEditor.setStringValue("Software\\Classes\\.reg", null, "REGfile");
            registryEditor.setStringValue("Software\\Classes\\.reg", "Content Type", "application/reg");
            registryEditor.setStringValue("Software\\Classes\\REGfile\\Shell\\Open\\command", null, "C:\\windows\\regedit.exe /C \"%1\"");

            registryEditor.setStringValue("Software\\Classes\\dllfile\\DefaultIcon", null, "shell32.dll,-154");
            registryEditor.setStringValue("Software\\Classes\\lnkfile\\DefaultIcon", null, "shell32.dll,-30");
            registryEditor.setStringValue("Software\\Classes\\inifile\\DefaultIcon", null, "shell32.dll,-151");
        }

        final String[] direct3dLibs = {"d3d8", "d3d9", "d3d10", "d3d10_1", "d3d10core", "d3d11", "d3d12", "d3d12core", "ddraw", "dxgi", "wined3d"};
        final String[] xinputLibs = {"dinput", "dinput8", "xinput1_1", "xinput1_2", "xinput1_3", "xinput1_4", "xinput9_1_0", "xinputuap"};
        final String[] openglLibs = {"opengl32"};

        final String dllOverridesKey = "Software\\Wine\\DllOverrides";

        try (WineRegistryEditor registryEditor = new WineRegistryEditor(userRegFile)) {
            for (String name : direct3dLibs) registryEditor.setStringValue(dllOverridesKey, name, "native,builtin");
            for (String name : xinputLibs) registryEditor.setStringValue(dllOverridesKey, name, "builtin,native");
            if (wineInfo.isArm64EC() && !GPUInformation.getRenderer(null, null).contains("Mali")) for (String name : openglLibs) registryEditor.setStringValue(dllOverridesKey, name, "native,builtin");
            setWindowMetrics(registryEditor);
        }

        if (Container.BIONIC.equalsIgnoreCase(ImageFs.find(context).getVariant())) {
            File runtimeRootDir = wineInfo != null && wineInfo.path != null ? new File(wineInfo.path) : null;
            String preferredGraphicsDriver = resolvePreferredGraphicsDriver(runtimeRootDir, wineInfo);
            if (!preferredGraphicsDriver.isEmpty()) {
                ensureGraphicsDriverRegistry(rootDir, preferredGraphicsDriver);
            }
        }

        copyWineDllsToContainer(rootDir, wineInfo);
    }

    private static void copyWineDllsToContainer(File rootDir, WineInfo wineInfo) {
        if (rootDir == null || wineInfo == null || wineInfo.path == null || wineInfo.path.trim().isEmpty()) return;

        File runtimeRootDir = new File(wineInfo.path);
        File runtimeWineLibDir = resolveRuntimeWineLibDir(runtimeRootDir);
        if (runtimeWineLibDir == null) return;

        File wineSystem32Dir = new File(runtimeWineLibDir, wineInfo.usesAarch64WindowsTree() ? "aarch64-windows" : "x86_64-windows");
        File wineSysWow64Dir = new File(runtimeWineLibDir, "i386-windows");
        File prefixDir = resolveHostWinePrefixDir(rootDir);
        File containerSystem32Dir = new File(prefixDir, "drive_c/windows/system32");
        File containerSysWow64Dir = new File(prefixDir, "drive_c/windows/syswow64");

        if (!containerSystem32Dir.isDirectory()) containerSystem32Dir.mkdirs();
        if (wineInfo.isWin64() && !containerSysWow64Dir.isDirectory()) containerSysWow64Dir.mkdirs();

        String[] names = {
                "user32.dll",
                "shell32.dll",
                "winemenubuilder.exe",
                "explorer.exe"
        };

        boolean win64 = wineInfo.isWin64();
        for (String name : names) {
            File src32 = new File(wineSysWow64Dir, name);
            File dst32 = new File(win64 ? containerSysWow64Dir : containerSystem32Dir, name);
            if (src32.isFile()) {
                FileUtils.copy(src32, dst32);
            }
            if (!win64) continue;

            File src64 = new File(wineSystem32Dir, name);
            File dst64 = new File(containerSystem32Dir, name);
            if (src64.isFile()) {
                FileUtils.copy(src64, dst64);
            }
        }
    }

    public static void overrideWinComponentDlls(Context context, Container container, String identifier, boolean useNative) {
        final String dllOverridesKey = "Software\\Wine\\DllOverrides";
        File userRegFile = new File(resolveHostWinePrefixDir(container.getRootDir()), "user.reg");

        try (WineRegistryEditor registryEditor = new WineRegistryEditor(userRegFile)) {
            JSONObject wincomponentsJSONObject = new JSONObject(FileUtils.readString(context, "wincomponents/wincomponents.json"));
            JSONArray dlnames = wincomponentsJSONObject.getJSONArray(identifier);
            for (int i = 0; i < dlnames.length(); i++) {
                String dlname = dlnames.getString(i);
                if (useNative) {
                    registryEditor.setStringValue(dllOverridesKey, dlname, "native,builtin");
                }
                else registryEditor.removeValue(dllOverridesKey, dlname);
            }
        }
        catch (JSONException e) {
            Log.w("WineUtils", "Failed to apply wincomponent DLL overrides for " + identifier, e);
        }
    }

    public static void setWinComponentRegistryKeys(File systemRegFile, String identifier, boolean useNative, Context context) {
        if (identifier.equals("directsound")) {
            try (WineRegistryEditor registryEditor = new WineRegistryEditor(systemRegFile)) {
                final String key64 = "Software\\Classes\\CLSID\\{083863F1-70DE-11D0-BD40-00A0C911CE86}\\Instance\\{E30629D1-27E5-11CE-875D-00608CB78066}";
                final String key32 = "Software\\Classes\\Wow6432Node\\CLSID\\{083863F1-70DE-11D0-BD40-00A0C911CE86}\\Instance\\{E30629D1-27E5-11CE-875D-00608CB78066}";

                if (useNative) {
                    registryEditor.setStringValue(key32, "CLSID", "{E30629D1-27E5-11CE-875D-00608CB78066}");
                    registryEditor.setHexValue(key32, "FilterData", "02000000000080000100000000000000307069330200000000000000010000000000000000000000307479330000000038000000480000006175647300001000800000aa00389b710100000000001000800000aa00389b71");
                    registryEditor.setStringValue(key32, "FriendlyName", "Wave Audio Renderer");

                    registryEditor.setStringValue(key64, "CLSID", "{E30629D1-27E5-11CE-875D-00608CB78066}");
                    registryEditor.setHexValue(key64, "FilterData", "02000000000080000100000000000000307069330200000000000000010000000000000000000000307479330000000038000000480000006175647300001000800000aa00389b710100000000001000800000aa00389b71");
                    registryEditor.setStringValue(key64, "FriendlyName", "Wave Audio Renderer");
                }
                else {
                    registryEditor.removeKey(key32);
                    registryEditor.removeKey(key64);
                }
            }
        }
        else if (identifier.equals("xaudio")) {
            try (WineRegistryEditor registryEditor = new WineRegistryEditor(systemRegFile)) {
                if (useNative) {
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{074B110F-7F58-4743-AEA5-12F1B5074ED}\\InprocServer32", null, "C:\\windows\\syswow64\\xactengine3_5.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{0977D092-2D95-4E43-8D42-9DDCC2545ED5}\\InprocServer32", null, "C:\\windows\\syswow64\\xactengine3_4.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{0AA000AA-F404-11D9-BD7A-0010DC4F8F81}\\InprocServer32", null, "C:\\windows\\syswow64\\xactengine2_0.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{1138472B-D187-44E9-81F2-AE1B0E7785F1}\\InprocServer32", null, "C:\\windows\\syswow64\\xactengine2_3.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{1F1B577E-5E5A-4E8A-BA73-C657EA8E8598}\\InprocServer32", null, "C:\\windows\\syswow64\\xactengine2_1.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{248D8A3B-6256-44D3-A018-2AC96C459F47}\\InprocServer32", null, "C:\\windows\\syswow64\\xactengine3_6.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{343E68E6-8F82-4A8D-A2DA-6E9A944B378C}\\InprocServer32", null, "C:\\windows\\syswow64\\xactengine2_9.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{3A2495CE-31D0-435B-8CCF-E9F0843FD960}\\InprocServer32", null, "C:\\windows\\syswow64\\xactengine2_6.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{3B80EE2A-B0F5-4780-9E30-90CB39685B03}\\InprocServer32", null, "C:\\windows\\syswow64\\xactengine3_0.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{54B68BC7-3A45-416B-A8C9-19BF19EC1DF5}\\InprocServer32", null, "C:\\windows\\syswow64\\xactengine2_5.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{65D822A4-4799-42C6-9B18-D26CF66DD320}\\InprocServer32", null, "C:\\windows\\syswow64\\xactengine2_10.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{77C56BF4-18A1-42B0-88AF-5072CE814949}\\InprocServer32", null, "C:\\windows\\syswow64\\xactengine2_8.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{94C1AFFA-66E7-4961-9521-CFDEF3128D4F}\\InprocServer32", null, "C:\\windows\\syswow64\\xactengine3_3.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{962F5027-99BE-4692-A468-85802CF8DE61}\\InprocServer32", null, "C:\\windows\\syswow64\\xactengine3_1.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{BC3E0FC6-2E0D-4C45-BC61-D9C328319BD8}\\InprocServer32", null, "C:\\windows\\syswow64\\xactengine2_4.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{BCC782BC-6492-4C22-8C35-F5D72FE73C6E}\\InprocServer32", null, "C:\\windows\\syswow64\\xactengine3_7.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{C60FAE90-4183-4A3F-B2F7-AC1DC49B0E5C}\\InprocServer32", null, "C:\\windows\\syswow64\\xactengine2_2.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{CD0D66EC-8057-43F5-ACBD-66DFB36FD78C}\\InprocServer32", null, "C:\\windows\\syswow64\\xactengine2_7.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{D3332F02-3DD0-4DE9-9AEC-20D85C4111B6}\\InprocServer32", null, "C:\\windows\\syswow64\\xactengine3_2.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{03219E78-5BC3-44D1-B92E-F63D89CC6526}\\InprocServer32", null, "C:\\windows\\syswow64\\xaudio2_4.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{2139E6DA-C341-4774-9AC3-B4E026347F64}\\InprocServer32", null, "C:\\windows\\syswow64\\xaudio2_5.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{3EDA9B49-2085-498B-9BB2-39A6778493DE}\\InprocServer32", null, "C:\\windows\\syswow64\\xaudio2_6.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{4C5E637A-16C7-4DE3-9C46-5ED22181962D}\\InprocServer32", null, "C:\\windows\\syswow64\\xaudio2_3.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{4C9B6DDE-6809-46E6-A278-9B6A97588670}\\InprocServer32", null, "C:\\windows\\syswow64\\xaudio2_5.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{5A508685-A254-4FBA-9B82-9A24B00306AF}\\InprocServer32", null, "C:\\windows\\syswow64\\xaudio2_7.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{629CF0DE-3ECC-41E7-9926-F7E43EEBEC51}\\InprocServer32", null, "C:\\windows\\syswow64\\xaudio2_2.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{6A93130E-1D53-41D1-A9CF-E758800BB179}\\InprocServer32", null, "C:\\windows\\syswow64\\xaudio2_7.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{8BB7778B-645B-4475-9A73-1DE3170BD3AF}\\InprocServer32", null, "C:\\windows\\syswow64\\xaudio2_4.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{9CAB402C-1D37-44B4-886D-FA4F36170A4C}\\InprocServer32", null, "C:\\windows\\syswow64\\xaudio2_3.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{B802058A-464A-42DB-BC10-B650D6F2586A}\\InprocServer32", null, "C:\\windows\\syswow64\\xaudio2_2.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{C1E3F122-A2EA-442C-854F-20D98F8357A1}\\InprocServer32", null, "C:\\windows\\syswow64\\xaudio2_1.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{C7338B95-52B8-4542-AA79-42EB016C8C1C}\\InprocServer32", null, "C:\\windows\\syswow64\\xaudio2_4.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{CAC1105F-619B-4D04-831A-44E1CBF12D57}\\InprocServer32", null, "C:\\windows\\syswow64\\xaudio2_7.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{CECEC95A-D894-491A-BEE3-5E106FB59F2D}\\InprocServer32", null, "C:\\windows\\syswow64\\xaudio2_6.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{D06DF0D0-8518-441E-822F-5451D5C595B8}\\InprocServer32", null, "C:\\windows\\syswow64\\xaudio2_5.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{E180344B-AC83-4483-959E-18A5C56A5E19}\\InprocServer32", null, "C:\\windows\\syswow64\\xaudio2_3.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{E21A7345-EB21-468E-BE50-804DB97CF708}\\InprocServer32", null, "C:\\windows\\syswow64\\xaudio2_1.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{E48C5A3F-93EF-43BB-A092-2C7CEB946F27}\\InprocServer32", null, "C:\\windows\\syswow64\\xaudio2_6.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{F4769300-B949-4DF9-B333-00D33932E9A6}\\InprocServer32", null, "C:\\windows\\syswow64\\xaudio2_1.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{F5CA7B34-8055-42C0-B836-216129EB7E30}\\InprocServer32", null, "C:\\windows\\syswow64\\xaudio2_2.dll");
                } else {
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{074B110F-7F58-4743-AEA5-12F1B5074ED}\\InprocServer32", null, "C:\\windows\\system32\\xactengine3_5.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{0977D092-2D95-4E43-8D42-9DDCC2545ED5}\\InprocServer32", null, "C:\\windows\\system32\\xactengine3_4.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{0AA000AA-F404-11D9-BD7A-0010DC4F8F81}\\InprocServer32", null, "C:\\windows\\system32\\xactengine2_0.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{1138472B-D187-44E9-81F2-AE1B0E7785F1}\\InprocServer32", null, "C:\\windows\\system32\\xactengine2_3.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{1F1B577E-5E5A-4E8A-BA73-C657EA8E8598}\\InprocServer32", null, "C:\\windows\\system32\\xactengine2_1.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{248D8A3B-6256-44D3-A018-2AC96C459F47}\\InprocServer32", null, "C:\\windows\\system32\\xactengine3_6.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{343E68E6-8F82-4A8D-A2DA-6E9A944B378C}\\InprocServer32", null, "C:\\windows\\system32\\xactengine2_9.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{3A2495CE-31D0-435B-8CCF-E9F0843FD960}\\InprocServer32", null, "C:\\windows\\system32\\xactengine2_6.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{3B80EE2A-B0F5-4780-9E30-90CB39685B03}\\InprocServer32", null, "C:\\windows\\system32\\xactengine3_0.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{54B68BC7-3A45-416B-A8C9-19BF19EC1DF5}\\InprocServer32", null, "C:\\windows\\system32\\xactengine2_5.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{65D822A4-4799-42C6-9B18-D26CF66DD320}\\InprocServer32", null, "C:\\windows\\system32\\xactengine2_10.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{77C56BF4-18A1-42B0-88AF-5072CE814949}\\InprocServer32", null, "C:\\windows\\system32\\xactengine2_8.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{94C1AFFA-66E7-4961-9521-CFDEF3128D4F}\\InprocServer32", null, "C:\\windows\\system32\\xactengine3_3.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{962F5027-99BE-4692-A468-85802CF8DE61}\\InprocServer32", null, "C:\\windows\\system32\\xactengine3_1.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{BC3E0FC6-2E0D-4C45-BC61-D9C328319BD8}\\InprocServer32", null, "C:\\windows\\system32\\xactengine2_4.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{BCC782BC-6492-4C22-8C35-F5D72FE73C6E}\\InprocServer32", null, "C:\\windows\\system32\\xactengine3_7.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{C60FAE90-4183-4A3F-B2F7-AC1DC49B0E5C}\\InprocServer32", null, "C:\\windows\\system32\\xactengine2_2.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{CD0D66EC-8057-43F5-ACBD-66DFB36FD78C}\\InprocServer32", null, "C:\\windows\\system32\\xactengine2_7.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{D3332F02-3DD0-4DE9-9AEC-20D85C4111B6}\\InprocServer32", null, "C:\\windows\\system32\\xactengine3_2.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{03219E78-5BC3-44D1-B92E-F63D89CC6526}\\InprocServer32", null, "C:\\windows\\system32\\xaudio2_4.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{2139E6DA-C341-4774-9AC3-B4E026347F64}\\InprocServer32", null, "C:\\windows\\system32\\xaudio2_5.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{3EDA9B49-2085-498B-9BB2-39A6778493DE}\\InprocServer32", null, "C:\\windows\\system32\\xaudio2_6.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{4C5E637A-16C7-4DE3-9C46-5ED22181962D}\\InprocServer32", null, "C:\\windows\\system32\\xaudio2_3.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{4C9B6DDE-6809-46E6-A278-9B6A97588670}\\InprocServer32", null, "C:\\windows\\system32\\xaudio2_5.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{5A508685-A254-4FBA-9B82-9A24B00306AF}\\InprocServer32", null, "C:\\windows\\system32\\xaudio2_7.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{629CF0DE-3ECC-41E7-9926-F7E43EEBEC51}\\InprocServer32", null, "C:\\windows\\system32\\xaudio2_2.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{6A93130E-1D53-41D1-A9CF-E758800BB179}\\InprocServer32", null, "C:\\windows\\system32\\xaudio2_7.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{8BB7778B-645B-4475-9A73-1DE3170BD3AF}\\InprocServer32", null, "C:\\windows\\system32\\xaudio2_4.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{9CAB402C-1D37-44B4-886D-FA4F36170A4C}\\InprocServer32", null, "C:\\windows\\system32\\xaudio2_3.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{B802058A-464A-42DB-BC10-B650D6F2586A}\\InprocServer32", null, "C:\\windows\\system32\\xaudio2_2.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{C1E3F122-A2EA-442C-854F-20D98F8357A1}\\InprocServer32", null, "C:\\windows\\system32\\xaudio2_1.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{C7338B95-52B8-4542-AA79-42EB016C8C1C}\\InprocServer32", null, "C:\\windows\\system32\\xaudio2_4.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{CAC1105F-619B-4D04-831A-44E1CBF12D57}\\InprocServer32", null, "C:\\windows\\system32\\xaudio2_7.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{CECEC95A-D894-491A-BEE3-5E106FB59F2D}\\InprocServer32", null, "C:\\windows\\system32\\xaudio2_6.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{D06DF0D0-8518-441E-822F-5451D5C595B8}\\InprocServer32", null, "C:\\windows\\system32\\xaudio2_5.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{E180344B-AC83-4483-959E-18A5C56A5E19}\\InprocServer32", null, "C:\\windows\\system32\\xaudio2_3.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{E21A7345-EB21-468E-BE50-804DB97CF708}\\InprocServer32", null, "C:\\windows\\system32\\xaudio2_1.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{E48C5A3F-93EF-43BB-A092-2C7CEB946F27}\\InprocServer32", null, "C:\\windows\\system32\\xaudio2_6.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{F4769300-B949-4DF9-B333-00D33932E9A6}\\InprocServer32", null, "C:\\windows\\system32\\xaudio2_1.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{F5CA7B34-8055-42C0-B836-216129EB7E30}\\InprocServer32", null, "C:\\windows\\system32\\xaudio2_2.dll");
                }
            }
        }
    }

    public static void changeServicesStatus(Container container, boolean onlyEssential) {
        final String[] services = {"BITS:3", "Eventlog:2", "HTTP:3", "LanmanServer:3", "NDIS:2", "PlugPlay:2", "RpcSs:3", "scardsvr:3", "Schedule:3", "Spooler:3", "StiSvc:3", "TermService:3", "winebus:3", "winehid:3", "Winmgmt:3", "wuauserv:3"};
        File systemRegFile = new File(resolveHostWinePrefixDir(container.getRootDir()), "system.reg");

        try (WineRegistryEditor registryEditor = new WineRegistryEditor(systemRegFile)) {
            registryEditor.setCreateKeyIfNotExist(false);

            for (String service : services) {
                String name = service.substring(0, service.indexOf(":"));
                int value = onlyEssential ? 4 : Character.getNumericValue(service.charAt(service.length()-1));
                registryEditor.setDwordValue("System\\CurrentControlSet\\Services\\"+name, "Start", value);
            }
        }
    }
}
