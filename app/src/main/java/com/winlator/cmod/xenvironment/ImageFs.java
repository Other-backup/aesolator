package com.winlator.cmod.xenvironment;

import android.content.Context;

import androidx.annotation.NonNull;

import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.core.WineInfo;
import com.winlator.cmod.core.WineUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;

public class ImageFs {
    public static final String ACTIVE_ROOT_DIR_NAME = "imagefs";
    public static final String BIONIC_ROOT_DIR_NAME = "imagefs-bionic";
    public static final String GLIBC_ROOT_DIR_NAME = "imagefs-glibc";
    public static final String PACKAGE_ROOT_PREFIX = "imagefs-runtime-";
    public static final String ROOTFS_PROVIDER_GAMENATIVE = "gamenative";
    public static final String ROOTFS_PROVIDER_WAIM = "waim";
    public static final String ROOTFS_PROVIDER_MOZE = "moze";
    public static final String ROOTFS_PROVIDER_ROOTFS_WINLATOR = "rootfs-winlator";
    public static final String ROOTFS_PROVIDER_COMMUNITY = "community";
    public static final String ROOTFS_PROVIDER_CUSTOM = "custom";
    public static final String ROOTFS_LAYOUT_UBUNTUFS = "ubuntufs";
    public static final String ROOTFS_LAYOUT_IMAGEFS = "imagefs";
    public static final String ROOTFS_LAYOUT_CUSTOM = "custom";
    public static final String USER = "xuser";
    public static final String HOME_PATH = "/home/"+USER;
    public static final String CACHE_PATH = HOME_PATH+"/.cache";
    public static final String CONFIG_PATH = HOME_PATH+"/.config";
    public static final String WINEPREFIX = HOME_PATH+"/.wine";
    private final File rootDir;
    public String winePath;
    public String home_path;
    public String cache_path;
    public String config_path;
    public String wineprefix;

    private ImageFs(File rootDir) {
        this.rootDir = rootDir;
        winePath = WineUtils.resolveCanonicalRuntimeRoot(resolveMainWineDir(rootDir)).getPath();
        setHomeDir(resolveActiveHomeDir(rootDir));
    }

    private static File resolveMainWineDir(File rootDir) {
        File donorStyleDir = new File(rootDir, "opt/wine");
        if (donorStyleDir.isDirectory()) return donorStyleDir;
        return new File(rootDir, "opt/" + WineInfo.MAIN_WINE_VERSION.identifier());
    }

    public static ImageFs find(Context context) {
        return new ImageFs(getActiveRootDir(context));
    }

    public static ImageFs find(Context context, String runtimeModel) {
        return new ImageFs(ensureActiveRuntimeRoot(context, runtimeModel));
    }

    public static ImageFs find(Context context, String runtimeModel, String runtimeIdentity) {
        return new ImageFs(ensureActiveRuntimeRoot(context, runtimeModel, runtimeIdentity));
    }

    public static ImageFs find(File rootDir) {
        return new ImageFs(rootDir);
    }

    public static File getActiveRootDir(Context context) {
        return getActiveRootDir(context.getFilesDir());
    }

    static File getActiveRootDir(File filesDir) {
        return new File(filesDir, ACTIVE_ROOT_DIR_NAME);
    }

    public static File getRuntimeRootDir(Context context, String runtimeModel) {
        return getRuntimeRootDir(context.getFilesDir(), runtimeModel);
    }

    static File getRuntimeRootDir(File filesDir, String runtimeModel) {
        return new File(filesDir, getRuntimeRootDirName(runtimeModel));
    }

    public static File getRuntimeRootDir(Context context, String runtimeModel, String runtimeIdentity) {
        return getRuntimeRootDir(context.getFilesDir(), runtimeModel, runtimeIdentity);
    }

    static File getRuntimeRootDir(File filesDir, String runtimeModel, String runtimeIdentity) {
        return new File(filesDir, getRuntimeRootDirName(runtimeModel, runtimeIdentity));
    }

    public static String getRuntimeRootDirName(String runtimeModel) {
        return ContentProfile.RUNTIME_MODEL_GLIBC.equals(normalizeRuntimeModel(runtimeModel))
                ? GLIBC_ROOT_DIR_NAME
                : BIONIC_ROOT_DIR_NAME;
    }

    public static String getRuntimeRootDirName(String runtimeModel, String runtimeIdentity) {
        String normalizedIdentity = normalizeRuntimeIdentity(runtimeIdentity);
        if (normalizedIdentity.isEmpty()) return getRuntimeRootDirName(runtimeModel);
        return PACKAGE_ROOT_PREFIX + normalizeRuntimeModel(runtimeModel) + "-" + normalizedIdentity;
    }

    public static String normalizeRuntimeModel(String runtimeModel) {
        String normalized = ContentProfile.normalizeRuntimeModel(runtimeModel);
        return normalized.isEmpty() ? ContentProfile.RUNTIME_MODEL_BIONIC : normalized;
    }

    public static String normalizeRuntimeIdentity(String runtimeIdentity) {
        String normalized = runtimeIdentity == null ? "" : runtimeIdentity.trim().toLowerCase(Locale.US);
        if (normalized.isEmpty()) return "";
        normalized = normalized.replaceAll("[^a-z0-9._-]+", "_");
        normalized = normalized.replaceAll("^[._-]+", "").replaceAll("[._-]+$", "");
        if (normalized.length() > 128) normalized = normalized.substring(0, 128);
        return normalized;
    }

    public static synchronized File ensureActiveRuntimeRoot(Context context, String runtimeModel) {
        return ensureActiveRuntimeRoot(context, runtimeModel, "");
    }

    public static synchronized File ensureActiveRuntimeRoot(Context context, String runtimeModel, String runtimeIdentity) {
        String normalizedRuntimeModel = normalizeRuntimeModel(runtimeModel);
        String normalizedRuntimeIdentity = normalizeRuntimeIdentity(runtimeIdentity);
        File activeRoot = getActiveRootDir(context);
        File targetRoot = getRuntimeRootDir(context, normalizedRuntimeModel, normalizedRuntimeIdentity);

        materializeLegacyRootLane(context, activeRoot, targetRoot, normalizedRuntimeModel);
        materializePackageRootLane(context, targetRoot, normalizedRuntimeModel, normalizedRuntimeIdentity);
        if (!targetRoot.exists()) targetRoot.mkdirs();

        if (FileUtils.isSymlink(activeRoot)) {
            if (!symlinkPointsTo(activeRoot, targetRoot)) {
                activeRoot.delete();
                FileUtils.symlink(targetRoot.getAbsolutePath(), activeRoot.getAbsolutePath());
            }
        } else if (!activeRoot.exists()) {
            FileUtils.symlink(targetRoot.getAbsolutePath(), activeRoot.getAbsolutePath());
        } else if (!sameFile(activeRoot, targetRoot)) {
            File quarantine = new File(context.getFilesDir(), ACTIVE_ROOT_DIR_NAME + "-legacy-" + System.currentTimeMillis());
            boolean moved = activeRoot.renameTo(quarantine);
            if (moved) {
                FileUtils.symlink(targetRoot.getAbsolutePath(), activeRoot.getAbsolutePath());
            }
            ForensicLogger.logEvent(
                    context,
                    moved ? "warn" : "error",
                    moved ? "ROOTFS_ACTIVE_REALDIR_QUARANTINED" : "ROOTFS_ACTIVE_REALDIR_BLOCKED",
                    null,
                        "rootfs",
                        moved ? "active_realdir_quarantined_before_runtime_switch" : "active_realdir_blocked_runtime_switch",
                        ForensicLogger.fields(
                                "runtime_model", normalizedRuntimeModel,
                                "runtime_identity", normalizedRuntimeIdentity,
                                "active_root", activeRoot.getAbsolutePath(),
                                "target_root", targetRoot.getAbsolutePath(),
                                "quarantine_root", quarantine.getAbsolutePath(),
                            "moved", moved
                    )
            );
        }

        ForensicLogger.logEvent(
                context,
                "info",
                "ROOTFS_RUNTIME_LANE_SELECTED",
                null,
                        "rootfs",
                        "runtime_rootfs_lane_selected",
                        ForensicLogger.fields(
                                "runtime_model", normalizedRuntimeModel,
                                "runtime_identity", normalizedRuntimeIdentity,
                                "package_isolated", !normalizedRuntimeIdentity.isEmpty(),
                                "active_root", activeRoot.getAbsolutePath(),
                                "target_root", targetRoot.getAbsolutePath(),
                                "target_root_name", targetRoot.getName(),
                                "returned_root", targetRoot.getAbsolutePath(),
                                "returned_root_is_active_alias", false,
                                "active_is_symlink", FileUtils.isSymlink(activeRoot),
                                "active_symlink_target", FileUtils.isSymlink(activeRoot) ? FileUtils.readSymlink(activeRoot) : "",
                                "target_exists", targetRoot.exists()
                )
        );
        return targetRoot;
    }

    public static synchronized File ensureContainerHomeForRuntime(Context context, int containerId, File sourceDir, String runtimeModel) {
        return ensureContainerHomeForRuntime(context, containerId, sourceDir, runtimeModel, "");
    }

    public static synchronized File ensureContainerHomeForRuntime(Context context, int containerId, File sourceDir, String runtimeModel, String runtimeIdentity) {
        String normalizedRuntimeModel = normalizeRuntimeModel(runtimeModel);
        String normalizedRuntimeIdentity = normalizeRuntimeIdentity(runtimeIdentity);
        File targetRoot = getRuntimeRootDir(context, normalizedRuntimeModel, normalizedRuntimeIdentity);
        File targetHomeRoot = new File(targetRoot, "home");
        File targetDir = new File(targetHomeRoot, USER + "-" + containerId);
        if (!targetHomeRoot.exists()) targetHomeRoot.mkdirs();

        boolean sourcePresent = sourceDir != null && sourceDir.isDirectory() && !FileUtils.isSymlink(sourceDir);
        boolean targetEmpty = targetDir.isDirectory() && FileUtils.isEmpty(targetDir);
        boolean targetReusable = !targetDir.exists() || targetEmpty;
        if (sourcePresent && !sameFile(sourceDir, targetDir) && targetReusable) {
            if (targetEmpty) FileUtils.delete(targetDir);
            File parent = targetDir.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            boolean copied = FileUtils.copy(sourceDir, targetDir);
            ForensicLogger.logEvent(
                    context,
                    copied ? "info" : "warn",
                    copied ? "ROOTFS_CONTAINER_HOME_MATERIALIZED" : "ROOTFS_CONTAINER_HOME_MATERIALIZE_FAILED",
                    null,
                    "rootfs",
                    copied ? "container_home_materialized_for_runtime_lane" : "container_home_materialize_failed",
                    ForensicLogger.fields(
                            "runtime_model", normalizedRuntimeModel,
                            "runtime_identity", normalizedRuntimeIdentity,
                            "container_id", containerId,
                            "source_dir", sourceDir.getAbsolutePath(),
                            "target_dir", targetDir.getAbsolutePath(),
                            "copied", copied,
                            "source_preserved", sourceDir.exists()
                    )
            );
        }

        if (!targetDir.exists()) targetDir.mkdirs();
        return targetDir;
    }

    public static ArrayList<File> getKnownRootDirs(Context context) {
        ArrayList<File> roots = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        addKnownRootDir(roots, seen, getRuntimeRootDir(context, ContentProfile.RUNTIME_MODEL_BIONIC));
        addKnownRootDir(roots, seen, getRuntimeRootDir(context, ContentProfile.RUNTIME_MODEL_GLIBC));
        File[] children = context.getFilesDir().listFiles();
        if (children != null) {
            Arrays.sort(children, (left, right) -> {
                String leftName = left != null ? left.getName() : "";
                String rightName = right != null ? right.getName() : "";
                return leftName.compareToIgnoreCase(rightName);
            });
            for (File child : children) {
                if (child == null) continue;
                String name = child.getName();
                if (name != null && name.startsWith(PACKAGE_ROOT_PREFIX)) {
                    addKnownRootDir(roots, seen, child);
                }
            }
        }
        addKnownRootDir(roots, seen, getActiveRootDir(context));
        return roots;
    }

    private static void addKnownRootDir(ArrayList<File> roots, LinkedHashSet<String> seen, File root) {
        if (root == null || !root.exists()) return;
        try {
            String key = root.getCanonicalPath();
            if (seen.add(key)) roots.add(root);
        } catch (IOException ignored) {
            String key = root.getAbsolutePath();
            if (seen.add(key)) roots.add(root);
        }
    }

    private static void materializeLegacyRootLane(Context context, File activeRoot, File requestedTargetRoot, String requestedRuntimeModel) {
        if (activeRoot == null || !activeRoot.exists() || FileUtils.isSymlink(activeRoot)) return;
        if (!activeRoot.isDirectory()) return;
        if (sameFile(activeRoot, requestedTargetRoot)) return;

        String legacyRuntimeModel = inferRuntimeModelFromRoot(activeRoot);
        if (legacyRuntimeModel.isEmpty()) legacyRuntimeModel = requestedRuntimeModel;
        File legacyTargetRoot = getRuntimeRootDir(context, legacyRuntimeModel);
        if (sameFile(activeRoot, legacyTargetRoot)) return;

        if (legacyTargetRoot.exists() && !FileUtils.isSymlink(legacyTargetRoot)) {
            migrateLegacyContainerHomes(context, activeRoot, legacyTargetRoot, legacyRuntimeModel);
            return;
        }

        boolean moved = activeRoot.renameTo(legacyTargetRoot);
        ForensicLogger.logEvent(
                context,
                moved ? "info" : "error",
                moved ? "ROOTFS_LEGACY_ROOT_MIGRATED" : "ROOTFS_LEGACY_ROOT_MIGRATION_FAILED",
                null,
                "rootfs",
                moved ? "legacy_root_migrated_to_runtime_lane" : "legacy_root_migration_failed",
                ForensicLogger.fields(
                        "legacy_runtime_model", legacyRuntimeModel,
                        "requested_runtime_model", requestedRuntimeModel,
                        "legacy_root", activeRoot.getAbsolutePath(),
                        "target_root", legacyTargetRoot.getAbsolutePath(),
                        "moved", moved
                )
        );
    }

    private static void materializePackageRootLane(Context context, File targetRoot, String runtimeModel, String runtimeIdentity) {
        if (targetRoot == null || targetRoot.exists() || runtimeIdentity == null || runtimeIdentity.isEmpty()) return;
        File familyRoot = getRuntimeRootDir(context, runtimeModel);
        if (!familyRoot.isDirectory() || FileUtils.isSymlink(familyRoot) || sameFile(familyRoot, targetRoot)) return;
        if (!rootMatchesRuntimeIdentity(familyRoot, runtimeIdentity)) return;

        boolean moved = familyRoot.renameTo(targetRoot);
        ForensicLogger.logEvent(
                context,
                moved ? "info" : "warn",
                moved ? "ROOTFS_FAMILY_ROOT_PROMOTED_TO_PACKAGE" : "ROOTFS_FAMILY_ROOT_PROMOTION_FAILED",
                null,
                "rootfs",
                moved ? "family_runtime_root_promoted_to_package_root" : "family_runtime_root_promotion_failed",
                ForensicLogger.fields(
                        "runtime_model", normalizeRuntimeModel(runtimeModel),
                        "runtime_identity", runtimeIdentity,
                        "family_root", familyRoot.getAbsolutePath(),
                        "target_root", targetRoot.getAbsolutePath(),
                        "moved", moved
                )
        );
    }

    private static boolean rootMatchesRuntimeIdentity(File root, String runtimeIdentity) {
        if (root == null || runtimeIdentity == null || runtimeIdentity.isEmpty()) return false;
        ImageFs imageFs = new ImageFs(root);
        String archIdentity = normalizeRuntimeIdentity(imageFs.getArch());
        if (runtimeIdentity.equals(archIdentity)) return true;

        File optDir = new File(root, "opt");
        File[] optChildren = optDir.listFiles();
        if (optChildren == null) return false;
        for (File child : optChildren) {
            if (child == null || !child.isDirectory()) continue;
            String childIdentity = normalizeRuntimeIdentity(child.getName());
            if (runtimeIdentity.equals(childIdentity)) return true;
            if (childIdentity.length() > 16
                    && (childIdentity.contains(runtimeIdentity) || runtimeIdentity.contains(childIdentity))) {
                return true;
            }
        }
        return false;
    }

    private static int migrateLegacyContainerHomes(Context context, File sourceRoot, File targetRoot, String runtimeModel) {
        File sourceHome = new File(sourceRoot, "home");
        File targetHome = new File(targetRoot, "home");
        File[] children = sourceHome.listFiles();
        if (children == null) return 0;
        if (!targetHome.exists()) targetHome.mkdirs();
        int movedCount = 0;
        for (File child : children) {
            if (child == null || !child.isDirectory() || FileUtils.isSymlink(child)) continue;
            if (!child.getName().startsWith(USER + "-")) continue;
            File target = new File(targetHome, child.getName());
            if (target.exists()) continue;
            if (child.renameTo(target)) movedCount++;
        }
        if (movedCount > 0) {
            ForensicLogger.logEvent(
                    context,
                    "info",
                    "ROOTFS_LEGACY_HOME_MERGED",
                    null,
                    "rootfs",
                    "legacy_container_homes_merged_to_runtime_lane",
                    ForensicLogger.fields(
                            "runtime_model", runtimeModel,
                            "source_root", sourceRoot.getAbsolutePath(),
                            "target_root", targetRoot.getAbsolutePath(),
                            "moved_count", movedCount
                    )
            );
        }
        return movedCount;
    }

    private static String inferRuntimeModelFromRoot(File root) {
        ImageFs imageFs = new ImageFs(root);
        String variantModel = ContentProfile.normalizeRuntimeModel(imageFs.getVariant());
        if (!variantModel.isEmpty()) return variantModel;
        String archModel = ContentProfile.inferRuntimeModelFromEntryName(imageFs.getArch());
        if (!archModel.isEmpty()) return archModel;
        String provider = imageFs.getRootfsProvider();
        if (ROOTFS_PROVIDER_WAIM.equals(provider) || ROOTFS_PROVIDER_MOZE.equals(provider)) {
            return ContentProfile.RUNTIME_MODEL_GLIBC;
        }
        if (ROOTFS_PROVIDER_GAMENATIVE.equals(provider)) {
            return ContentProfile.RUNTIME_MODEL_BIONIC;
        }
        return "";
    }

    private static boolean sameFile(File left, File right) {
        if (left == null || right == null) return false;
        try {
            return left.getCanonicalFile().equals(right.getCanonicalFile());
        } catch (IOException ignored) {
            return left.getAbsolutePath().equals(right.getAbsolutePath());
        }
    }

    private static boolean symlinkPointsTo(File link, File target) {
        if (link == null || target == null || !FileUtils.isSymlink(link)) return false;
        String rawTarget = FileUtils.readSymlink(link);
        if (rawTarget == null || rawTarget.trim().isEmpty()) return false;
        File resolved = rawTarget.startsWith("/")
                ? new File(rawTarget)
                : new File(link.getParentFile(), rawTarget);
        return sameFile(resolved, target);
    }

    private static File resolveActiveHomeDir(File rootDir) {
        File defaultHomeDir = new File(rootDir, HOME_PATH);
        try {
            File canonicalHomeDir = defaultHomeDir.getCanonicalFile();
            if (canonicalHomeDir.exists()) return canonicalHomeDir;
        }
        catch (IOException ignored) {
        }
        return defaultHomeDir;
    }

    public void setHomeDir(File homeDir) {
        File resolvedHomeDir = homeDir != null ? homeDir : new File(rootDir, HOME_PATH);
        home_path = resolvedHomeDir.getPath();
        cache_path = new File(resolvedHomeDir, ".cache").getPath();
        config_path = new File(resolvedHomeDir, ".config").getPath();
        wineprefix = new File(resolvedHomeDir, ".wine").getPath();
    }

    public File getRootDir() {
        return rootDir;
    }

    public File getHomeDir() {
        return new File(home_path);
    }

    public File getWinePrefixDir() {
        return new File(wineprefix);
    }

    public boolean isValid() {
        return rootDir.isDirectory() && getImgVersionFile().exists();
    }

    public int getVersion() {
        File imgVersionFile = getImgVersionFile();
        return imgVersionFile.exists() ? Integer.parseInt(FileUtils.readLines(imgVersionFile).get(0)) : 0;
    }

    public String getFormattedVersion() {
        return String.format(Locale.ENGLISH, "%.1f", (float)getVersion());
    }

    public void createImgVersionFile(int version) {
        getConfigDir().mkdirs();
        File file = getImgVersionFile();
        try {
            file.createNewFile();
            FileUtils.writeString(file, String.valueOf(version));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getVariant() {
        File variantFile = getVariantFile();
        return variantFile.exists() ? FileUtils.readLines(variantFile).get(0) : "";
    }

    public void createVariantFile(String variant) {
        getConfigDir().mkdirs();
        File file = getVariantFile();
        try {
            file.createNewFile();
            FileUtils.writeString(file, variant);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getArch() {
        File archFile = getArchFile();
        return archFile.exists() ? FileUtils.readLines(archFile).get(0) : "";
    }

    public void createArchFile(String arch) {
        getConfigDir().mkdirs();
        File file = getArchFile();
        try {
            file.createNewFile();
            FileUtils.writeString(file, arch);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getRootfsProvider() {
        File providerFile = getRootfsProviderFile();
        if (providerFile.exists()) {
            String normalized = normalizeRootfsProvider(FileUtils.readLines(providerFile).get(0));
            if (!normalized.isEmpty()) return normalized;
        }
        return inferRootfsProvider();
    }

    public void createRootfsProviderFile(String provider) {
        getConfigDir().mkdirs();
        File file = getRootfsProviderFile();
        try {
            file.createNewFile();
            String normalized = normalizeRootfsProvider(provider);
            FileUtils.writeString(file, normalized.isEmpty() ? inferRootfsProvider() : normalized);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getRootfsLayout() {
        File layoutFile = getRootfsLayoutFile();
        if (layoutFile.exists()) {
            String normalized = normalizeRootfsLayout(FileUtils.readLines(layoutFile).get(0));
            if (!normalized.isEmpty()) return normalized;
        }
        return inferRootfsLayout();
    }

    public void createRootfsLayoutFile(String layout) {
        getConfigDir().mkdirs();
        File file = getRootfsLayoutFile();
        try {
            file.createNewFile();
            String normalized = normalizeRootfsLayout(layout);
            FileUtils.writeString(file, normalized.isEmpty() ? inferRootfsLayout() : normalized);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isGameNativeRootfs() {
        return ROOTFS_PROVIDER_GAMENATIVE.equalsIgnoreCase(getRootfsProvider());
    }

    public boolean isUbuntuFsLayout() {
        return ROOTFS_LAYOUT_UBUNTUFS.equalsIgnoreCase(getRootfsLayout());
    }

    private String inferRootfsProvider() {
        String normalizedVariant = getVariant() == null ? "" : getVariant().trim().toLowerCase(Locale.US);
        if (normalizedVariant.contains("gamenative")) return ROOTFS_PROVIDER_GAMENATIVE;
        return ROOTFS_PROVIDER_CUSTOM;
    }

    private String inferRootfsLayout() {
        File usrBinDir = new File(rootDir, "usr/bin");
        File usrEtcDir = new File(rootDir, "usr/etc");
        File usrLibDir = new File(rootDir, "usr/lib");
        File binDir = new File(rootDir, "bin");
        File etcDir = new File(rootDir, "etc");
        File libDir = new File(rootDir, "lib");
        File lib64Dir = new File(rootDir, "lib64");
        boolean imageFsSurface = (usrBinDir.isDirectory() || usrEtcDir.isDirectory() || usrLibDir.isDirectory())
                && (getCompatTmpDir().exists()
                || FileUtils.isSymlink(binDir)
                || FileUtils.isSymlink(etcDir)
                || FileUtils.isSymlink(libDir)
                || FileUtils.isSymlink(lib64Dir));
        if (imageFsSurface) return ROOTFS_LAYOUT_IMAGEFS;
        if (binDir.exists() || etcDir.exists() || libDir.exists()) return ROOTFS_LAYOUT_UBUNTUFS;
        return ROOTFS_LAYOUT_CUSTOM;
    }

    private static String normalizeRootfsProvider(String provider) {
        String normalized = provider == null ? "" : provider.trim().toLowerCase(Locale.US);
        if (normalized.isEmpty()) return "";
        if (normalized.contains("gamenative") || normalized.contains("game native")) return ROOTFS_PROVIDER_GAMENATIVE;
        if (normalized.contains("waim")) return ROOTFS_PROVIDER_WAIM;
        if (normalized.contains("moze") || normalized.contains("winlator-glibc")) return ROOTFS_PROVIDER_MOZE;
        if (normalized.contains("rootfs-winlator")) return ROOTFS_PROVIDER_ROOTFS_WINLATOR;
        if (normalized.contains("community")) return ROOTFS_PROVIDER_COMMUNITY;
        return ROOTFS_PROVIDER_CUSTOM;
    }

    private static String normalizeRootfsLayout(String layout) {
        String normalized = layout == null ? "" : layout.trim().toLowerCase(Locale.US);
        if (normalized.isEmpty()) return "";
        if (normalized.contains("ubuntufs") || normalized.contains("ubuntu")) return ROOTFS_LAYOUT_UBUNTUFS;
        if (normalized.contains("imagefs")) return ROOTFS_LAYOUT_IMAGEFS;
        return ROOTFS_LAYOUT_CUSTOM;
    }

    public String getWinePath() {
        return winePath;
    }

    public void setWinePath(String winePath) {
        File requestedRoot = winePath == null || winePath.trim().isEmpty()
                ? resolveMainWineDir(rootDir)
                : new File(winePath);
        this.winePath = WineUtils.resolveCanonicalRuntimeRoot(requestedRoot).getPath();
    }

    public File getConfigDir() {
        return new File(rootDir, ".winlator");
    }

    public File getImgVersionFile() {
        return new File(getConfigDir(), ".img_version");
    }

    public File getVariantFile() {
        return new File(getConfigDir(), ".variant");
    }

    public File getArchFile() {
        return new File(getConfigDir(), ".arch");
    }

    public File getRootfsProviderFile() {
        return new File(getConfigDir(), ".provider");
    }

    public File getRootfsLayoutFile() {
        return new File(getConfigDir(), ".layout");
    }

    public File getOptDir() {
        return new File(rootDir, "opt");
    }

    public File getInstalledWineDir() {
        return new File(rootDir, "opt/installed-wine");
    }

    public File getMainWineDir() {
        return WineUtils.resolveCanonicalRuntimeRoot(resolveMainWineDir(rootDir));
    }

    public File getTmpDir() {
        File canonicalTmpDir = new File(rootDir, "tmp");
        if (canonicalTmpDir.exists() || isGameNativeRootfs()) {
            return canonicalTmpDir;
        }
        return getCompatTmpDir();
    }

    public File getCompatTmpDir() {
        return new File(rootDir, "usr/tmp");
    }

    public File getLibDir() {
        return new File(rootDir, "usr/lib");
    }

    public File getAndroidHostLibDir() {
        return new File(rootDir, "usr/lib/android-host");
    }

    public File getLib32Dir() {
        return new File(rootDir, "usr/lib/arm-linux-gnueabihf");
    }

    public File getLib64Dir() {
        return new File(rootDir, "usr/lib");
    }

    public File getBinDir() { return new File(rootDir, "usr/bin"); }

    public File getLocalBinDir() {
        return new File(rootDir, "usr/local/bin");
    }

    public File getGlibcBinDir() {
        return new File(rootDir, "usr/glibc/bin");
    }

    public File getGlibc32Dir() {
        return new File(rootDir, "usr/lib/arm-linux-gnueabihf");
    }

    public File getGlibc64Dir() {
        return new File(rootDir, "usr/lib");
    }

    public File getShareDir() {
        return new File(rootDir, "usr/share");
    }

    public File getEtcDir() {
        return new File(rootDir, "usr/etc");
    }

    public File getStorageDir() {
        return new File(rootDir, "storage");
    }

    public File getFilesDir() {
        return rootDir.getParentFile();
    }

    public boolean isGlibcRuntimeAvailable() {
        File glibcBinDir = getGlibcBinDir();
        if (glibcBinDir.isDirectory()) return true;
        String variant = getVariant();
        return "glibc".equalsIgnoreCase(variant) || "gamenative".equalsIgnoreCase(variant);
    }

    public String getRuntimeLibcModel() {
        return isGlibcRuntimeAvailable() ? "glibc" : "bionic";
    }

    @NonNull
    @Override
    public String toString() {
        return rootDir.getPath();
    }
}
