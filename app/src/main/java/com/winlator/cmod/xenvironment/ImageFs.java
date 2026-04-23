package com.winlator.cmod.xenvironment;

import android.content.Context;

import androidx.annotation.NonNull;

import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.WineInfo;
import com.winlator.cmod.core.WineUtils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class ImageFs {
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
        File donorStyleDir = new File(rootDir, "/opt/wine");
        if (donorStyleDir.isDirectory()) return donorStyleDir;
        return new File(rootDir, "/opt/" + WineInfo.MAIN_WINE_VERSION.identifier());
    }

    public static ImageFs find(Context context) {
        return new ImageFs(new File(context.getFilesDir(), "imagefs"));
    }

    public static ImageFs find(File rootDir) {
        return new ImageFs(rootDir);
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
        return new File(rootDir, "/opt");
    }

    public File getInstalledWineDir() {
        return new File(rootDir, "/opt/installed-wine");
    }

    public File getMainWineDir() {
        return WineUtils.resolveCanonicalRuntimeRoot(resolveMainWineDir(rootDir));
    }

    public File getTmpDir() {
        File canonicalTmpDir = new File(rootDir, "/tmp");
        if (canonicalTmpDir.exists() || isGameNativeRootfs()) {
            return canonicalTmpDir;
        }
        return getCompatTmpDir();
    }

    public File getCompatTmpDir() {
        return new File(rootDir, "/usr/tmp");
    }

    public File getLibDir() {
        return new File(rootDir, "/usr/lib");
    }

    public File getAndroidHostLibDir() {
        return new File(rootDir, "/usr/lib/android-host");
    }

    public File getLib32Dir() {
        return new File(rootDir, "/usr/lib/arm-linux-gnueabihf");
    }

    public File getLib64Dir() {
        return new File(rootDir, "/usr/lib");
    }

    public File getBinDir() { return new File(rootDir, "/usr/bin"); }

    public File getLocalBinDir() {
        return new File(rootDir, "/usr/local/bin");
    }

    public File getGlibcBinDir() {
        return new File(rootDir, "/usr/glibc/bin");
    }

    public File getGlibc32Dir() {
        return new File(rootDir, "/usr/lib/arm-linux-gnueabihf");
    }

    public File getGlibc64Dir() {
        return new File(rootDir, "/usr/lib");
    }

    public File getShareDir() {
        return new File(rootDir, "/usr/share");
    }

    public File getEtcDir() {
        return new File(rootDir, "/usr/etc");
    }

    public File getStorageDir() {
        return new File(rootDir, "/storage");
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
