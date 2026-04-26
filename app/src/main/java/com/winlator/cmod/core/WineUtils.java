package com.winlator.cmod.core;

import android.content.Context;
import android.util.Log;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.xenvironment.ImageFs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class WineUtils {
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
            return runtimeRootDir != null
                    && runtimeRootDir.isDirectory()
                    && binDir != null
                    && binDir.isDirectory()
                    && libDir != null
                    && libDir.isDirectory()
                    && prefixPack != null
                    && prefixPack.isFile();
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
        File wineLibDir = resolveRuntimeWineLibDir(runtimeRootDir);
        if (wineLibDir == null) return null;
        File wineUnixDir = new File(wineLibDir, "aarch64-unix");
        return wineUnixDir.isDirectory() ? wineUnixDir : null;
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
        File best = hasRuntimePayload(current) ? current : null;
        for (int depth = 0; depth < 4 && current != null; depth++) {
            current = current.getParentFile();
            if (current != null && hasRuntimePayload(current)) {
                best = current;
            }
        }
        return best != null ? best : runtimeRootDir;
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
