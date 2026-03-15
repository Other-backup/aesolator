package com.winlator.cmod.contents;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.TarCompressorUtils;
import com.winlator.cmod.xenvironment.ImageFs;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DgVoodooManager {
    private static final String TAG = "DgVoodooManager";
    private static final String CONTENT_DIR = "contents/dgvoodoo";
    private static final String PACKAGE_DIR = "current";
    private static final String TEMP_DIR = "tmp";
    private static final String META_FILE = ".meta";
    private static final String STAGE_MARKER = ".aero_dgvoodoo_stage";
    private static final String BACKUP_PREFIX = ".aero_dgvoodoo_backup.";
    private static final String TEMP_ARCHIVE = "tmp_package.bin";
    private static final String ARCH_X86 = "x86";
    private static final String ARCH_X64 = "x64";
    private static final String ARCH_ARM64 = "arm64";
    private static final String ARCH_ARM64EC = "arm64ec";

    private final Context context;
    private final File rootDir;

    public DgVoodooManager(Context context) {
        this.context = context;
        this.rootDir = new File(context.getFilesDir(), CONTENT_DIR);
        if (!rootDir.exists()) rootDir.mkdirs();
    }

    public boolean isInstalled() {
        for (File packageRoot : getPackageRootsSorted()) {
            if (hasAnyRuntimeArch(packageRoot)) return true;
        }
        return false;
    }

    public boolean isArchInstalled(String arch) {
        return resolveBestPackageRootForArch(arch) != null;
    }

    public String getVersionHint() {
        for (File packageRoot : getPackageRootsSorted()) {
            String versionHint = readVersionHint(packageRoot);
            if (!versionHint.isEmpty()) return versionHint;
        }
        return isInstalled() ? "local" : "missing";
    }

    public ArrayList<String> getInstalledArchitectures() {
        ArrayList<String> architectures = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();
        for (File packageRoot : getPackageRootsSorted()) {
            appendInstalledArch(packageRoot, ARCH_X86, architectures, seen);
            appendInstalledArch(packageRoot, ARCH_X64, architectures, seen);
            appendInstalledArch(packageRoot, ARCH_ARM64, architectures, seen);
            appendInstalledArch(packageRoot, ARCH_ARM64EC, architectures, seen);
        }
        return architectures;
    }

    public String getInstalledArchitectureSummary() {
        ArrayList<String> architectures = getInstalledArchitectures();
        if (architectures.isEmpty()) return "-";
        return String.join(", ", architectures);
    }

    public String installPackage(Uri packageUri) {
        if (packageUri == null) return "";

        File tempArchive = new File(rootDir, TEMP_ARCHIVE);
        try (InputStream inputStream = context.getContentResolver().openInputStream(packageUri)) {
            if (inputStream == null) return "";
            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tempArchive))) {
                byte[] buffer = new byte[16 * 1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            String displayName = FileUtils.getUriFileName(context, packageUri);
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = String.valueOf(packageUri.getLastPathSegment());
            }
            return installPackageFromFile(tempArchive, displayName);
        } catch (IOException e) {
            Log.d(TAG, "Failed to install dgVoodoo package", e);
            return "";
        } finally {
            if (tempArchive.exists()) FileUtils.delete(tempArchive);
        }
    }

    public String installPackage(File packageFile) {
        if (packageFile == null || !packageFile.isFile()) return "";
        return installPackageFromFile(packageFile, packageFile.getName());
    }

    public void removePackage() {
        for (File packageRoot : getPackageRootsSorted()) {
            if (packageRoot != null && packageRoot.exists()) {
                FileUtils.delete(packageRoot);
            }
        }
        File packageDir = getPackageDir();
        if (packageDir.exists() && !packageDir.isDirectory()) {
            FileUtils.delete(packageDir);
        }
    }

    public boolean matchesProfile(ContentProfile profile) {
        if (profile == null || profile.type != ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO) return false;
        if (!isInstalled()) return false;
        String archTag = profile.getArchitectureTag();
        if ("generic".equalsIgnoreCase(archTag)) return true;
        if ("bundle".equalsIgnoreCase(archTag)) {
            return isArchInstalled(ARCH_X64) && (isArchInstalled(ARCH_ARM64EC) || isArchInstalled(ARCH_ARM64));
        }
        if ("x86_64".equalsIgnoreCase(archTag)) return isArchInstalled(ARCH_X64);
        return isArchInstalled(archTag);
    }

    public File resolveShortcutTargetDir(File imageFsRoot, String shortcutPath) {
        if (shortcutPath == null || shortcutPath.trim().isEmpty()) return null;
        if (shortcutPath.toLowerCase(Locale.ROOT).endsWith(".lnk")) return null;
        File targetFile = resolveDosPath(imageFsRoot, shortcutPath);
        return targetFile != null ? targetFile.getParentFile() : null;
    }

    public String resolvePreferredArch(String shortcutPath, String configuredArch) {
        String configured = configuredArch == null ? "" : configuredArch.trim().toLowerCase(Locale.ROOT);
        if (ARCH_X86.equals(configured)
                || ARCH_X64.equals(configured)
                || ARCH_ARM64.equals(configured)
                || ARCH_ARM64EC.equals(configured)) {
            return configured;
        }

        String value = shortcutPath == null ? "" : shortcutPath.toLowerCase(Locale.ROOT);
        if (value.contains("program files (x86)") || value.contains("syswow64")) {
            if (hasInstalledRuntimeArch(ARCH_X86)) return ARCH_X86;
        }

        if (hasInstalledRuntimeArch(ARCH_ARM64EC)) return ARCH_ARM64EC;
        if (hasInstalledRuntimeArch(ARCH_X64)) return ARCH_X64;
        if (hasInstalledRuntimeArch(ARCH_ARM64)) return ARCH_ARM64;
        if (hasInstalledRuntimeArch(ARCH_X86)) return ARCH_X86;
        return ARCH_X64;
    }

    public boolean stageRuntime(File targetDir, String arch) {
        if (targetDir == null || !targetDir.isDirectory()) return false;

        cleanupStagedRuntime(targetDir);

        String normalizedArch = normalizeRuntimeArch(arch);
        File packageRoot = resolveBestPackageRootForArch(normalizedArch);
        File sourceDir = packageRoot == null ? new File("/nonexistent") : resolveRuntimeDir(packageRoot, normalizedArch);
        if (!sourceDir.isDirectory() && ARCH_ARM64EC.equals(normalizedArch)) {
            packageRoot = resolveBestPackageRootForArch(ARCH_X64);
            sourceDir = packageRoot == null ? new File("/nonexistent") : resolveRuntimeDir(packageRoot, ARCH_X64);
            if (!sourceDir.isDirectory()) {
                packageRoot = resolveBestPackageRootForArch(ARCH_ARM64);
                sourceDir = packageRoot == null ? new File("/nonexistent") : resolveRuntimeDir(packageRoot, ARCH_ARM64);
            }
        } else if (!sourceDir.isDirectory() && ARCH_ARM64.equals(normalizedArch)) {
            packageRoot = resolveBestPackageRootForArch(ARCH_ARM64EC);
            sourceDir = packageRoot == null ? new File("/nonexistent") : resolveRuntimeDir(packageRoot, ARCH_ARM64EC);
            if (!sourceDir.isDirectory()) {
                packageRoot = resolveBestPackageRootForArch(ARCH_X64);
                sourceDir = packageRoot == null ? new File("/nonexistent") : resolveRuntimeDir(packageRoot, ARCH_X64);
            }
        }
        if (!sourceDir.isDirectory() || packageRoot == null) return false;

        ArrayList<String> stagedFiles = new ArrayList<>();
        File[] files = sourceDir.listFiles();
        if (files == null) return false;

        for (File sourceFile : files) {
            if (!sourceFile.isFile()) continue;
            String lowerName = sourceFile.getName().toLowerCase(Locale.ROOT);
            if (!lowerName.endsWith(".dll")) continue;
            File targetFile = new File(targetDir, sourceFile.getName());
            if (!backupExistingFile(targetFile)) {
                cleanupStagedFiles(targetDir, stagedFiles);
                return false;
            }
            if (!FileUtils.copy(sourceFile, targetFile)) {
                cleanupStagedFiles(targetDir, stagedFiles);
                return false;
            }
            stagedFiles.add(sourceFile.getName());
        }

        if (!copyOptionalFile(new File(packageRoot, "dgVoodoo.conf"), targetDir, stagedFiles)
                || !copyOptionalFile(new File(packageRoot, "dgVoodooCpl.exe"), targetDir, stagedFiles)) {
            cleanupStagedFiles(targetDir, stagedFiles);
            return false;
        }

        File marker = new File(targetDir, STAGE_MARKER);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < stagedFiles.size(); i++) {
            if (i > 0) builder.append('\n');
            builder.append(stagedFiles.get(i));
        }
        if (!FileUtils.writeString(marker, builder.toString())) {
            cleanupStagedFiles(targetDir, stagedFiles);
            return false;
        }
        return true;
    }

    public void cleanupStagedRuntime(File targetDir) {
        if (targetDir == null) return;
        File marker = new File(targetDir, STAGE_MARKER);
        if (!marker.isFile()) return;

        ArrayList<String> lines = FileUtils.readLines(marker);
        for (String line : lines) {
            String value = line == null ? "" : line.trim();
            if (value.isEmpty()) continue;
            File stagedFile = new File(targetDir, value);
            if (stagedFile.exists()) stagedFile.delete();
            restoreBackup(targetDir, value);
        }
        marker.delete();
    }

    private String installPackageFromFile(File packageFile, String displayName) {
        if (packageFile == null || !packageFile.isFile()) return "";

        File tempDir = new File(rootDir, TEMP_DIR);
        if (tempDir.exists()) FileUtils.delete(tempDir);
        if (!tempDir.mkdirs()) return "";

        try {
            if (!extractPackage(packageFile, displayName, tempDir)) {
                FileUtils.delete(tempDir);
                return "";
            }

            File packageRoot = findPackageRoot(tempDir);
            if (packageRoot == null) {
                FileUtils.delete(tempDir);
                return "";
            }

            ContentProfile profile = readProfile(packageRoot);
            File destination = resolveInstallDestination(profile);
            if (destination.exists()) FileUtils.delete(destination);
            if (!destination.mkdirs()) {
                FileUtils.delete(tempDir);
                return "";
            }
            if (!FileUtils.copy(packageRoot, destination)) {
                FileUtils.delete(tempDir);
                return "";
            }

            String versionHint = resolveVersionHint(packageRoot, displayName);
            FileUtils.writeString(new File(destination, META_FILE), versionHint);
            FileUtils.delete(tempDir);
            return versionHint;
        } catch (Exception e) {
            Log.d(TAG, "Invalid dgVoodoo package selected", e);
            FileUtils.delete(tempDir);
            return "";
        }
    }

    private boolean extractPackage(File packageFile, String displayName, File destination) {
        String lower = normalizeArchiveName(displayName == null ? packageFile.getName() : displayName);
        if (lower.endsWith(".zip")) {
            return unzipInto(packageFile, destination);
        }

        if (lower.endsWith(".wcp") || lower.endsWith(".wcp.xz") || lower.endsWith(".xz")) {
            if (TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, packageFile, destination)) return true;
            if (lower.endsWith(".wcp") && TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, packageFile, destination)) return true;
            return false;
        }

        if (lower.endsWith(".wcp.zst") || lower.endsWith(".zst")) {
            return TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, packageFile, destination);
        }

        if (unzipInto(packageFile, destination)) return true;
        FileUtils.delete(destination);
        if (!destination.mkdirs()) return false;
        if (TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, packageFile, destination)) return true;
        FileUtils.delete(destination);
        if (!destination.mkdirs()) return false;
        return TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, packageFile, destination);
    }

    private boolean unzipInto(File zipFile, File destination) {
        try (InputStream inputStream = Files.newInputStream(zipFile.toPath());
             ZipInputStream zis = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File target = getSafeZipEntryFile(destination, entry);
                if (target == null) {
                    FileUtils.delete(destination);
                    return false;
                }
                if (entry.isDirectory()) {
                    if (!target.exists() && !target.mkdirs()) {
                        FileUtils.delete(destination);
                        return false;
                    }
                    continue;
                }

                File parent = target.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    FileUtils.delete(destination);
                    return false;
                }
                Files.copy(zis, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            Log.d(TAG, "Invalid dgVoodoo zip selected", e);
            FileUtils.delete(destination);
            return false;
        }
    }

    private String normalizeArchiveName(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private File getPackageDir() {
        return new File(rootDir, PACKAGE_DIR);
    }

    private File resolveInstallDestination(ContentProfile profile) {
        if (profile != null && profile.type == ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO) {
            return ContentsManager.getInstallDir(context, profile);
        }
        return getPackageDir();
    }

    private ArrayList<File> getPackageRootsSorted() {
        ArrayList<File> roots = new ArrayList<>();

        File legacyRoot = getPackageDir();
        if (isValidPackageRoot(legacyRoot)) roots.add(legacyRoot);

        File contentsRoot = ContentsManager.getContentTypeDir(context, ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO);
        File[] installedDirs = contentsRoot.listFiles();
        if (installedDirs != null) {
            for (File dir : installedDirs) {
                if (dir != null && dir.isDirectory() && isValidPackageRoot(dir)) {
                    roots.add(dir);
                }
            }
        }

        roots.sort((left, right) -> {
            int versionCompare = Integer.compare(resolvePackageVersionCode(right), resolvePackageVersionCode(left));
            if (versionCompare != 0) return versionCompare;
            int modifiedCompare = Long.compare(right.lastModified(), left.lastModified());
            if (modifiedCompare != 0) return modifiedCompare;
            return left.getAbsolutePath().compareToIgnoreCase(right.getAbsolutePath());
        });
        return roots;
    }

    private int resolvePackageVersionCode(File packageRoot) {
        ContentProfile profile = readProfile(packageRoot);
        return profile != null ? profile.verCode : 0;
    }

    private ContentProfile readProfile(File packageRoot) {
        if (packageRoot == null) return null;
        File profileFile = new File(packageRoot, ContentsManager.PROFILE_NAME);
        if (!profileFile.isFile()) return null;
        try {
            return new ContentsManager(context).readProfile(profileFile);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String readVersionHint(File packageRoot) {
        if (packageRoot == null) return "";
        File metaFile = new File(packageRoot, META_FILE);
        if (metaFile.isFile()) {
            ArrayList<String> lines = FileUtils.readLines(metaFile);
            if (!lines.isEmpty()) {
                String value = lines.get(0).trim();
                if (!value.isEmpty()) return value;
            }
        }

        ContentProfile profile = readProfile(packageRoot);
        if (profile != null && profile.verName != null && !profile.verName.trim().isEmpty()) {
            return profile.verName.trim();
        }
        return "";
    }

    private void appendInstalledArch(File packageRoot, String arch, ArrayList<String> architectures, HashSet<String> seen) {
        if (!hasRuntimeArch(packageRoot, arch)) return;
        if (seen.add(arch)) architectures.add(arch);
    }

    private boolean hasInstalledRuntimeArch(String arch) {
        return resolveBestPackageRootForArch(arch) != null;
    }

    private File resolveBestPackageRootForArch(String arch) {
        for (File packageRoot : getPackageRootsSorted()) {
            if (hasRuntimeArch(packageRoot, arch)) return packageRoot;
        }
        return null;
    }

    private File findPackageRoot(File tempDir) {
        if (isValidPackageRoot(tempDir)) return tempDir;

        File[] files = tempDir.listFiles();
        if (files == null) return null;
        for (File file : files) {
            if (!file.isDirectory()) continue;
            if (isValidPackageRoot(file)) return file;
            File nested = findPackageRoot(file);
            if (nested != null) return nested;
        }
        return null;
    }

    private boolean isValidPackageRoot(File root) {
        if (hasAnyRuntimeArch(root)) return true;
        File payloadRuntime = new File(root, "payload/runtime");
        return hasAnyRuntimeArch(payloadRuntime);
    }

    private File getSafeZipEntryFile(File rootDir, ZipEntry entry) throws IOException {
        File target = new File(rootDir, entry.getName());
        String rootPath = rootDir.getCanonicalPath() + File.separator;
        String targetPath = target.getCanonicalPath();
        if (!targetPath.startsWith(rootPath)) return null;
        return target;
    }

    private String resolveVersionHint(File packageRoot, String displayName) {
        String directoryHint = packageRoot != null ? normalizeVersionHint(packageRoot.getName()) : "local";
        if (!"local".equals(directoryHint)) return directoryHint;
        return normalizeVersionHint(displayName);
    }

    private String normalizeVersionHint(String value) {
        String result = value == null ? "" : value.trim();
        if (result.isEmpty()) return "local";
        String lowerResult = result.toLowerCase(Locale.ROOT);
        if (lowerResult.endsWith(".wcp.zst")) {
            result = result.substring(0, result.length() - 8);
        } else if (lowerResult.endsWith(".wcp.xz")) {
            result = result.substring(0, result.length() - 7);
        } else if (lowerResult.endsWith(".wcp")) {
            result = result.substring(0, result.length() - 4);
        } else if (lowerResult.endsWith(".zip")) {
            result = result.substring(0, result.length() - 4);
        }
        String lower = result.toLowerCase(Locale.ROOT);
        if (lower.startsWith("dgvoodoo2_")) {
            String tail = result.substring("dgVoodoo2_".length());
            tail = tail.replaceAll("(?i)_dev64$", "-dev64");
            tail = tail.replace('_', '.');
            if (!tail.trim().isEmpty()) {
                result = tail;
            }
        }
        if (TEMP_DIR.equalsIgnoreCase(result) || PACKAGE_DIR.equalsIgnoreCase(result)) {
            return "local";
        }
        return result;
    }

    private boolean copyOptionalFile(File sourceFile, File targetDir, ArrayList<String> stagedFiles) {
        if (!sourceFile.isFile()) return true;
        File targetFile = new File(targetDir, sourceFile.getName());
        if (!backupExistingFile(targetFile)) return false;
        if (!FileUtils.copy(sourceFile, targetFile)) return false;
        stagedFiles.add(sourceFile.getName());
        return true;
    }

    private void cleanupStagedFiles(File targetDir, ArrayList<String> stagedFiles) {
        for (String name : stagedFiles) {
            File targetFile = new File(targetDir, name);
            if (targetFile.exists()) targetFile.delete();
            restoreBackup(targetDir, name);
        }
        File marker = new File(targetDir, STAGE_MARKER);
        if (marker.exists()) marker.delete();
    }

    private boolean backupExistingFile(File targetFile) {
        if (!targetFile.exists()) return true;
        if (targetFile.isDirectory()) return false;

        File backupFile = getBackupFile(targetFile.getParentFile(), targetFile.getName());
        if (backupFile.exists()) return true;
        if (targetFile.renameTo(backupFile)) return true;
        return FileUtils.copy(targetFile, backupFile) && targetFile.delete();
    }

    private void restoreBackup(File targetDir, String fileName) {
        File backupFile = getBackupFile(targetDir, fileName);
        if (!backupFile.isFile()) return;

        File targetFile = new File(targetDir, fileName);
        if (targetFile.exists()) targetFile.delete();
        if (!backupFile.renameTo(targetFile)) {
            if (FileUtils.copy(backupFile, targetFile)) {
                backupFile.delete();
            }
        }
    }

    private File getBackupFile(File targetDir, String fileName) {
        return new File(targetDir, BACKUP_PREFIX + fileName);
    }

    private File resolveDosPath(File imageFsRoot, String shortcutPath) {
        if (shortcutPath == null || shortcutPath.length() < 2 || shortcutPath.charAt(1) != ':') {
            return null;
        }

        char driveLetter = Character.toLowerCase(shortcutPath.charAt(0));
        String relativePath = shortcutPath.substring(2).replace('\\', '/');
        while (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        File baseDir;
        if (driveLetter == 'c') {
            baseDir = new File(imageFsRoot, ImageFs.WINEPREFIX + "/drive_c");
        } else {
            baseDir = new File(imageFsRoot, ImageFs.WINEPREFIX + "/dosdevices/" + driveLetter + ":");
            try {
                if (!baseDir.exists()) return null;
                baseDir = baseDir.getCanonicalFile();
            } catch (IOException e) {
                return null;
            }
        }

        return relativePath.isEmpty() ? baseDir : new File(baseDir, relativePath);
    }

    private String normalizeRuntimeArch(String arch) {
        String normalized = arch == null ? "" : arch.trim().toLowerCase(Locale.ROOT);
        if (ARCH_X86.equals(normalized)) return ARCH_X86;
        if (ARCH_X64.equals(normalized)) return ARCH_X64;
        if (ARCH_ARM64.equals(normalized) || "aarch64".equals(normalized)) return ARCH_ARM64;
        if (ARCH_ARM64EC.equals(normalized) || "arm64-ec".equals(normalized)) return ARCH_ARM64EC;
        return ARCH_X86;
    }

    private boolean hasAnyRuntimeArch(File root) {
        return hasRuntimeArch(root, ARCH_X86)
                || hasRuntimeArch(root, ARCH_X64)
                || hasRuntimeArch(root, ARCH_ARM64)
                || hasRuntimeArch(root, ARCH_ARM64EC);
    }

    private boolean hasRuntimeArch(File root, String arch) {
        return resolveRuntimeDir(root, arch).isDirectory();
    }

    private File resolveRuntimeDir(File packageRoot, String arch) {
        if (packageRoot == null || arch == null || arch.trim().isEmpty()) return new File("/nonexistent");
        String normalized = normalizeRuntimeArch(arch);

        ArrayList<File> roots = new ArrayList<>();
        roots.add(packageRoot);
        File payloadRuntime = new File(packageRoot, "payload/runtime");
        if (payloadRuntime.isDirectory()) roots.add(payloadRuntime);

        ArrayList<String> candidates = new ArrayList<>();
        candidates.add("MS/" + normalized);
        candidates.add(normalized);
        candidates.add("Release/" + normalized);
        candidates.add("release/" + normalized);
        candidates.add("bin/" + normalized);
        if (ARCH_X64.equals(normalized)) {
            candidates.add("MS/x86_64");
            candidates.add("x86_64");
            candidates.add("Release/x86_64");
            candidates.add("release/x86_64");
            candidates.add("bin/x86_64");
            candidates.add("MS/amd64");
            candidates.add("amd64");
            candidates.add("Release/amd64");
            candidates.add("release/amd64");
            candidates.add("bin/amd64");
        }
        if (ARCH_ARM64.equals(normalized)) {
            candidates.add("MS/aarch64");
            candidates.add("aarch64");
            candidates.add("Release/aarch64");
            candidates.add("release/aarch64");
            candidates.add("bin/aarch64");
        }
        if (ARCH_ARM64EC.equals(normalized)) {
            candidates.add("arm64-ec");
            candidates.add("MS/arm64-ec");
            candidates.add("Release/arm64-ec");
            candidates.add("release/arm64-ec");
        }

        for (File root : roots) {
            for (String relative : candidates) {
                File dir = new File(root, relative);
                if (dir.isDirectory()) return dir;
            }
        }
        return new File(packageRoot, ".missing-" + normalized);
    }
}
