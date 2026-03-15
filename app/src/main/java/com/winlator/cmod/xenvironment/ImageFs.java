package com.winlator.cmod.xenvironment;

import android.content.Context;

import androidx.annotation.NonNull;

import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.WineInfo;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class ImageFs {
    public static final String ROOTFS_PROVIDER_GAMENATIVE = "gamenative";
    public static final String ROOTFS_LAYOUT_UBUNTUFS = "ubuntufs";
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
        winePath = resolveMainWineDir(rootDir).getPath();
        home_path = rootDir + HOME_PATH;
        cache_path = rootDir + CACHE_PATH;
        config_path = rootDir + CONFIG_PATH;
        wineprefix = rootDir + WINEPREFIX;
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

    public File getRootDir() {
        return rootDir;
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
            return normalizeRootfsProvider(FileUtils.readLines(providerFile).get(0));
        }
        return ROOTFS_PROVIDER_GAMENATIVE;
    }

    public void createRootfsProviderFile(String provider) {
        getConfigDir().mkdirs();
        File file = getRootfsProviderFile();
        try {
            file.createNewFile();
            FileUtils.writeString(file, normalizeRootfsProvider(provider));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getRootfsLayout() {
        File layoutFile = getRootfsLayoutFile();
        if (layoutFile.exists()) {
            return normalizeRootfsLayout(FileUtils.readLines(layoutFile).get(0));
        }
        return ROOTFS_LAYOUT_UBUNTUFS;
    }

    public void createRootfsLayoutFile(String layout) {
        getConfigDir().mkdirs();
        File file = getRootfsLayoutFile();
        try {
            file.createNewFile();
            FileUtils.writeString(file, normalizeRootfsLayout(layout));
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

    private static String normalizeRootfsProvider(String provider) {
        return ROOTFS_PROVIDER_GAMENATIVE;
    }

    private static String normalizeRootfsLayout(String layout) {
        return ROOTFS_LAYOUT_UBUNTUFS;
    }

    public String getWinePath() {
        return winePath;
    }

    public void setWinePath(String winePath) {
        this.winePath = winePath;
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
        return resolveMainWineDir(rootDir);
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
