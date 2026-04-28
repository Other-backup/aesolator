package com.winlator.cmod.gamefixes;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerUtils;
import com.winlator.cmod.container.Shortcut;
import com.winlator.cmod.core.EnvVars;
import com.winlator.cmod.core.WineRegistryEditor;
import com.winlator.cmod.core.WineUtils;
import com.winlator.cmod.data.GameSource;
import com.winlator.cmod.xenvironment.ImageFs;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GameFixesRegistry {
    private static final String TAG = "GameFixes";
    static final String INSTALL_PATH_PLACEHOLDER = "<InstallPath>";
    static final String DEFAULT_WINDOWS_INSTALL_PATH = "A:\\";

    private static final Map<String, GameFix> FIXES = buildFixes();

    private static final class FixTarget {
        final GameSource source;
        final String catalogId;

        private FixTarget(GameSource source, String catalogId) {
            this.source = source;
            this.catalogId = catalogId;
        }
    }

    private GameFixesRegistry() {
    }

    public static boolean hasFixFor(@Nullable String appId, @Nullable Container container) {
        FixTarget target = resolveFixTarget(appId, container);
        return target != null && hasBuiltInFix(target.source, target.catalogId);
    }

    public static boolean applyFor(Context context, @Nullable String appId, @Nullable Container container, @Nullable Shortcut shortcut) {
        if (context == null) return false;
        FixTarget target = resolveFixTarget(appId, container);
        if (target == null) return false;

        GameFix fix = FIXES.get(buildKey(target.source, target.catalogId));
        if (fix == null) return false;

        String installPath = resolveInstallPath(container, shortcut);
        if (installPath.isEmpty()) {
            Log.w(TAG, "Skipping fix for " + target.source + " " + target.catalogId + ": missing install path");
            return false;
        }

        String installPathWindows = resolveWindowsInstallPath(container, shortcut);
        boolean applied = fix.apply(context, target.catalogId, installPath, installPathWindows, container, shortcut);
        if (applied) {
            Log.i(TAG, "Applied donor fix for " + target.source + " " + target.catalogId);
        }
        return applied;
    }

    static String resolveEpicCatalogId(
            String fallbackGameId,
            String sessionCatalogId,
            String extraCatalogId,
            String sessionEpicCatalogId,
            String extraEpicCatalogId
    ) {
        return firstNonEmpty(
                sessionCatalogId,
                extraCatalogId,
                sessionEpicCatalogId,
                extraEpicCatalogId,
                fallbackGameId
        );
    }

    static String resolveCatalogId(GameSource source, String appId, Container container) {
        String gameId = String.valueOf(ContainerUtils.INSTANCE.extractGameIdFromContainerId(appId));
        if (source != GameSource.EPIC) return gameId;
        return resolveEpicCatalogId(
                gameId,
                container.getSessionMetadata("catalogId", ""),
                container.getExtra("catalogId", ""),
                container.getSessionMetadata("epicCatalogId", ""),
                container.getExtra("epicCatalogId", "")
        );
    }

    @Nullable
    private static FixTarget resolveFixTarget(@Nullable String appId, @Nullable Container container) {
        if (appId == null || appId.trim().isEmpty() || container == null) return null;
        try {
            GameSource source = ContainerUtils.INSTANCE.extractGameSourceFromContainerId(appId);
            String catalogId = resolveCatalogId(source, appId, container);
            if (catalogId.isEmpty()) return null;
            return new FixTarget(source, catalogId);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    static String resolveInstallPath(Container container, @Nullable Shortcut shortcut) {
        WineUtils.WindowsLaunchTarget shortcutLaunchTarget = resolveShortcutLaunchTarget(container, shortcut);
        String shortcutInstallPathWindows = shortcutLaunchTarget.workingDir;
        String shortcutInstallPath = shortcutLaunchTarget.hostTargetDir != null && shortcutLaunchTarget.hostTargetDir.exists()
                ? shortcutLaunchTarget.hostTargetDir.getAbsolutePath()
                : resolveHostInstallPathFromContainer(container, shortcutInstallPathWindows);
        if (shortcutInstallPath.isEmpty()) {
            shortcutInstallPath = resolveHostInstallPathFromWindowsPath(
                    shortcutInstallPathWindows,
                    resolveDriveRoot(container, "A"),
                    resolveDriveRoot(container, "C")
            );
        }
        if (!shortcutInstallPath.isEmpty()) return shortcutInstallPath;

        String installPath = safe(container.getInstallPath()).trim();
        if (!installPath.isEmpty()) {
            File installDir = new File(installPath);
            if (installDir.exists()) return installDir.getAbsolutePath();
        }

        String aDrivePath = resolveDriveRoot(container, "A");
        if (!aDrivePath.isEmpty()) {
            File driveRoot = new File(aDrivePath);
            if (driveRoot.isDirectory()) return driveRoot.getAbsolutePath();
        }

        return "";
    }

    static String resolveWindowsInstallPath(Container container, @Nullable Shortcut shortcut) {
        String explicit = firstNonEmpty(
                container.getSessionMetadata("installPathWindows", ""),
                container.getExtra("installPathWindows", ""),
                container.getSessionMetadata("windowsInstallPath", ""),
                container.getExtra("windowsInstallPath", "")
        );
        if (!explicit.isEmpty()) return normalizeWindowsPath(explicit);

        String shortcutInstallPathWindows = resolveShortcutLaunchTarget(container, shortcut).workingDir;
        if (!shortcutInstallPathWindows.isEmpty()) return shortcutInstallPathWindows;

        String installPath = safe(container.getInstallPath()).trim();
        if (!installPath.isEmpty()) {
            String aPath = toWindowsDrivePath("A", resolveDriveRoot(container, "A"), installPath);
            if (!aPath.isEmpty()) return aPath;
            String cPath = toWindowsDrivePath("C", resolveDriveRoot(container, "C"), installPath);
            if (!cPath.isEmpty()) return cPath;
        }

        return DEFAULT_WINDOWS_INSTALL_PATH;
    }

    private static String normalizeWindowsPath(String path) {
        return safe(path).replace('/', '\\');
    }

    static String resolveShortcutInstallPathWindows(String shortcutPath) {
        return WineUtils.resolveWindowsLaunchTarget(null, shortcutPath).workingDir;
    }

    private static WineUtils.WindowsLaunchTarget resolveShortcutLaunchTarget(Container container, @Nullable Shortcut shortcut) {
        return WineUtils.resolveWindowsLaunchTarget(
                container != null ? container.getRootDir() : null,
                shortcut != null ? shortcut.path : ""
        );
    }

    private static String resolveHostInstallPathFromContainer(Container container, String windowsInstallPath) {
        File resolved = WineUtils.resolveHostPathFromWindowsPath(container.getRootDir(), windowsInstallPath);
        return resolved != null && resolved.exists() ? resolved.getAbsolutePath() : "";
    }

    static String resolveHostInstallPathFromWindowsPath(String windowsInstallPath, String aDrivePath, String cDrivePath) {
        String normalizedWindowsPath = normalizeWindowsPath(windowsInstallPath).trim();
        if (normalizedWindowsPath.length() < 2 || normalizedWindowsPath.charAt(1) != ':') return "";

        String driveLetter = normalizedWindowsPath.substring(0, 1).toUpperCase();
        String driveRoot = "";
        if ("A".equals(driveLetter)) driveRoot = safe(aDrivePath).trim();
        else if ("C".equals(driveLetter)) driveRoot = safe(cDrivePath).trim();
        if (driveRoot.isEmpty()) return "";

        String relative = normalizedWindowsPath.substring(2).replace('\\', File.separatorChar);
        while (relative.startsWith(File.separator)) relative = relative.substring(1);
        File target = relative.isEmpty() ? new File(driveRoot) : new File(driveRoot, relative);
        return target.exists() ? target.getAbsolutePath() : "";
    }

    private static String resolveDriveRoot(Container container, String driveLetter) {
        String normalizedDriveLetter = safe(driveLetter).trim().toUpperCase();
        if (normalizedDriveLetter.isEmpty()) return "";
        if ("C".equals(normalizedDriveLetter)) {
            return WineUtils.resolveHostWineDriveCRoot(container.getRootDir()).getAbsolutePath();
        }
        for (String[] drive : Container.drivesIterator(container.getDrives())) {
            if (drive.length < 2) continue;
            if (!normalizedDriveLetter.equalsIgnoreCase(safe(drive[0]).trim())) continue;
            String hostPath = safe(drive[1]).trim();
            if (!hostPath.isEmpty()) return hostPath;
        }
        return "";
    }

    private static String toWindowsDrivePath(String driveLetter, String driveRoot, String hostPath) {
        String normalizedDriveLetter = safe(driveLetter).trim().toUpperCase();
        String normalizedDriveRoot = safe(driveRoot).trim();
        String normalizedHostPath = safe(hostPath).trim();
        if (normalizedDriveLetter.isEmpty() || normalizedDriveRoot.isEmpty() || normalizedHostPath.isEmpty()) return "";

        File driveRootFile = new File(normalizedDriveRoot);
        File hostFile = new File(normalizedHostPath);
        String driveRootAbsolute = driveRootFile.getAbsolutePath();
        String hostAbsolute = hostFile.getAbsolutePath();
        if (!hostAbsolute.startsWith(driveRootAbsolute)) return "";

        String relative = hostAbsolute.substring(driveRootAbsolute.length()).replace(File.separatorChar, '\\');
        while (relative.startsWith("\\")) relative = relative.substring(1);
        if (relative.isEmpty()) return normalizedDriveLetter + ":\\";
        return normalizedDriveLetter + ":\\" + relative;
    }

    static boolean hasBuiltInFix(GameSource source, String gameId) {
        return source != null && gameId != null && FIXES.containsKey(buildKey(source, gameId));
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String buildKey(GameSource source, String gameId) {
        return source.name() + "|" + gameId;
    }

    private static void register(Map<String, GameFix> fixes, GameSource source, String gameId, GameFix fix) {
        fixes.put(buildKey(source, gameId), fix);
    }

    private static Map<String, GameFix> buildFixes() {
        LinkedHashMap<String, GameFix> fixes = new LinkedHashMap<>();

        // GameNative donor wall: line-by-line portable fixes only.
        register(fixes, GameSource.GOG, "1129934535", new LaunchArgFix("-lang=eng"));
        register(fixes, GameSource.GOG, "1141086411", new RegistryKeyFix(
                "Software\\Wow6432Node\\KONAMI\\SILENT HILL 4\\1.00.000",
                mapOf(
                        "Install Language", "English",
                        "Install Path", INSTALL_PATH_PLACEHOLDER,
                        "Movie Install", INSTALL_PATH_PLACEHOLDER,
                        "Uninstall Path", INSTALL_PATH_PLACEHOLDER
                )));
        register(fixes, GameSource.GOG, "1177610018", new LaunchArgFix("-lang=eng"));
        register(fixes, GameSource.GOG, "1453375253", new WineEnvVarFix(mapOf("WINEDLLOVERRIDES", "icu=n")));
        register(fixes, GameSource.GOG, "1454315831", new RegistryKeyFix(
                "Software\\Wow6432Node\\Bethesda Softworks\\Fallout3",
                mapOf("Installed Path", INSTALL_PATH_PLACEHOLDER)));
        register(fixes, GameSource.GOG, "1454587428", new RegistryKeyFix(
                "Software\\Wow6432Node\\Bethesda Softworks\\FalloutNV",
                mapOf("Installed Path", INSTALL_PATH_PLACEHOLDER)));
        register(fixes, GameSource.GOG, "1458058109", new RegistryKeyFix(
                "Software\\Wow6432Node\\Bethesda Softworks\\Oblivion",
                mapOf("Installed Path", INSTALL_PATH_PLACEHOLDER)));
        register(fixes, GameSource.GOG, "1635627436", new LaunchArgFix("--rendering-driver vulkan"));
        register(fixes, GameSource.GOG, "1787707874", new LaunchArgFix("-lang=eng"));

        register(fixes, GameSource.STEAM, "400", new LaunchArgFix("-game portal"));
        register(fixes, GameSource.STEAM, "22300", new RegistryKeyFix(
                "Software\\Wow6432Node\\Bethesda Softworks\\Fallout3",
                mapOf("Installed Path", INSTALL_PATH_PLACEHOLDER)));
        register(fixes, GameSource.STEAM, "22330", new RegistryKeyFix(
                "Software\\Wow6432Node\\Bethesda Softworks\\Oblivion",
                mapOf("Installed Path", INSTALL_PATH_PLACEHOLDER)));
        register(fixes, GameSource.STEAM, "22380", new RegistryKeyFix(
                "Software\\Wow6432Node\\Bethesda Softworks\\FalloutNV",
                mapOf("Installed Path", INSTALL_PATH_PLACEHOLDER)));
        register(fixes, GameSource.STEAM, "413150", new WineEnvVarFix(mapOf("WINEDLLOVERRIDES", "icu=n")));
        register(fixes, GameSource.STEAM, "3373660", new LaunchArgFix("--no-sandbox"));
        register(fixes, GameSource.STEAM, "1637320", new LaunchArgFix("--rendering-driver vulkan"));

        // Epic donor fixes need catalogId rather than the numeric app id. We keep them live
        // when that metadata is present, without hard-wiring a store service dependency.
        register(fixes, GameSource.EPIC, "59a0c86d02da42e8ba6444cb171e61bf", new RegistryKeyFix(
                "Software\\Wow6432Node\\Bethesda Softworks\\Oblivion",
                mapOf("Installed Path", INSTALL_PATH_PLACEHOLDER)));
        register(fixes, GameSource.EPIC, "b1b4e0b67a044575820cb5e63028dcae", new RegistryKeyFix(
                "Software\\Wow6432Node\\Bethesda Softworks\\Fallout3",
                mapOf("Installed Path", INSTALL_PATH_PLACEHOLDER)));
        register(fixes, GameSource.EPIC, "dabb52e328834da7bbe99691e374cb84", new RegistryKeyFix(
                "Software\\Wow6432Node\\Bethesda Softworks\\FalloutNV",
                mapOf("Installed Path", INSTALL_PATH_PLACEHOLDER)));

        return Collections.unmodifiableMap(fixes);
    }

    private static Map<String, String> mapOf(String... values) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        if (values == null) return map;
        for (int i = 0; i + 1 < values.length; i += 2) {
            map.put(values[i], values[i + 1]);
        }
        return map;
    }

    interface GameFix {
        boolean apply(
                Context context,
                String catalogId,
                String installPath,
                String installPathWindows,
                Container container,
                @Nullable Shortcut shortcut
        );
    }

    private static final class LaunchArgFix implements GameFix {
        private final String launchArgs;

        private LaunchArgFix(String launchArgs) {
            this.launchArgs = safe(launchArgs).trim();
        }

        @Override
        public boolean apply(
                Context context,
                String catalogId,
                String installPath,
                String installPathWindows,
                Container container,
                @Nullable Shortcut shortcut
        ) {
            if (launchArgs.isEmpty()) return false;

            String shortcutExecArgs = shortcut == null ? "" : safe(shortcut.getExtra("execArgs", "")).trim();
            String containerExecArgs = safe(container.getExecArgs()).trim();
            if (!shortcutExecArgs.isEmpty() || !containerExecArgs.isEmpty()) {
                return true;
            }

            try {
                container.setExecArgs(launchArgs);
                container.saveData();
                Log.i(TAG, "Added launch args '" + launchArgs + "' for " + catalogId);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Failed to apply launch args fix for " + catalogId, e);
                return false;
            }
        }
    }

    private static final class RegistryKeyFix implements GameFix {
        private final String registryKey;
        private final Map<String, String> defaultValues;

        private RegistryKeyFix(String registryKey, Map<String, String> defaultValues) {
            this.registryKey = safe(registryKey);
            this.defaultValues = defaultValues == null ? Collections.emptyMap() : defaultValues;
        }

        @Override
        public boolean apply(
                Context context,
                String catalogId,
                String installPath,
                String installPathWindows,
                Container container,
                @Nullable Shortcut shortcut
        ) {
            File rootDir = container != null && container.getRootDir() != null
                    ? container.getRootDir()
                    : ImageFs.find(context).getRootDir();
            File systemRegFile = new File(WineUtils.resolveHostWinePrefixDir(rootDir), "system.reg");
            if (!systemRegFile.isFile()) {
                Log.w(TAG, "system.reg not found at " + systemRegFile.getAbsolutePath());
                return false;
            }

            try (WineRegistryEditor editor = new WineRegistryEditor(systemRegFile)) {
                editor.setCreateKeyIfNotExist(true);
                for (Map.Entry<String, String> entry : defaultValues.entrySet()) {
                    String value = INSTALL_PATH_PLACEHOLDER.equals(entry.getValue())
                            ? normalizeWindowsPath(installPathWindows)
                            : entry.getValue();
                    String existing = editor.getStringValue(registryKey, entry.getKey(), null);
                    if (existing == null || existing.isEmpty()) {
                        editor.setStringValue(registryKey, entry.getKey(), value);
                    }
                }
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Failed to apply registry fix for " + catalogId, e);
                return false;
            }
        }
    }

    private static final class WineEnvVarFix implements GameFix {
        private final Map<String, String> envVarsToSet;

        private WineEnvVarFix(Map<String, String> envVarsToSet) {
            this.envVarsToSet = envVarsToSet == null ? Collections.emptyMap() : envVarsToSet;
        }

        @Override
        public boolean apply(
                Context context,
                String catalogId,
                String installPath,
                String installPathWindows,
                Container container,
                @Nullable Shortcut shortcut
        ) {
            try {
                EnvVars envVars = new EnvVars(container.getEnvVars());
                boolean hasChanges = false;
                for (Map.Entry<String, String> entry : envVarsToSet.entrySet()) {
                    if (envVars.has(entry.getKey())) continue;
                    envVars.put(entry.getKey(), entry.getValue());
                    hasChanges = true;
                }
                if (!hasChanges) return true;
                container.setEnvVars(envVars.toString());
                container.saveData();
                Log.i(TAG, "Added env vars for " + catalogId + ": " + envVarsToSet);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Failed to apply env-var fix for " + catalogId, e);
                return false;
            }
        }
    }
}
