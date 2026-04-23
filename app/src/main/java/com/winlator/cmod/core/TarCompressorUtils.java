package com.winlator.cmod.core;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class TarCompressorUtils {
    private static final String TAG = "TarCompressorUtils";

    public enum Type {XZ, ZSTD}

    // Interface to define the exclusion filter
    public interface ExclusionFilter {
        boolean shouldInclude(File file);
    }

    static File resolveExtractionTarget(File rootDir, String entryName) throws IOException {
        File target = FileUtils.resolveSafeArchiveEntry(rootDir, entryName);
        if (target == null) {
            logError("Blocked unsafe archive entry: " + entryName);
        }
        return target;
    }

    private static void logError(String message) {
        try {
            Log.e(TAG, message);
        } catch (RuntimeException ignored) {
            // Logger may be unavailable during bootstrap or test harness execution.
        }
    }

    private static void logError(String message, Throwable throwable) {
        try {
            Log.e(TAG, message, throwable);
        } catch (RuntimeException ignored) {
            // Logger may be unavailable during bootstrap or test harness execution.
        }
    }

    static String normalizeArchiveEntryName(String entryName) {
        if (entryName == null) return "";
        String normalized = entryName.replace('\\', '/');
        while (normalized.startsWith("./")) normalized = normalized.substring(2);
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    static String stripTopLevelDirectory(String entryName, String topLevelDirectory) {
        String normalizedEntry = normalizeArchiveEntryName(entryName);
        String normalizedTop = normalizeArchiveEntryName(topLevelDirectory);
        if (normalizedTop.isEmpty()) return normalizedEntry;
        if (normalizedEntry.equals(normalizedTop)) return "";
        String prefix = normalizedTop + "/";
        return normalizedEntry.startsWith(prefix) ? normalizedEntry.substring(prefix.length()) : normalizedEntry;
    }

    private static boolean isTmpDirectoryEntry(String entryName) {
        String normalized = normalizeArchiveEntryName(entryName);
        return normalized.equals("tmp") || normalized.startsWith("tmp/") || normalized.contains("/tmp/");
    }

    private static File applyListener(File rootDir, File file, long size, OnExtractFileListener onExtractFileListener) throws IOException {
        if (onExtractFileListener == null) return file;
        File mapped = onExtractFileListener.onExtractFile(file, size);
        if (mapped == null) return null;
        File canonical = mapped.getCanonicalFile();
        if (!FileUtils.isSameOrChild(rootDir, canonical)) {
            logError("Blocked listener-remapped archive entry outside destination: " + mapped.getAbsolutePath());
            return null;
        }
        return canonical;
    }

    private static boolean ensureParentDirectory(File file) {
        File parent = file.getParentFile();
        return parent == null || parent.isDirectory() || parent.mkdirs();
    }

    private static boolean extractEntryData(ArchiveInputStream tar, TarArchiveEntry entry, File file) throws IOException {
        if (entry.isDirectory()) {
            return file.isDirectory() || file.mkdirs();
        }
        if (!ensureParentDirectory(file)) return false;
        if (entry.isSymbolicLink()) {
            return FileUtils.symlink(entry.getLinkName(), file.getAbsolutePath());
        }
        try (BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(file), StreamUtils.BUFFER_SIZE)) {
            return StreamUtils.copy(tar, outStream);
        }
    }

    private static void addFile(ArchiveOutputStream tar, File file, String entryName) {
        try {
            tar.putArchiveEntry(tar.createArchiveEntry(file, entryName));
            try (BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(file), StreamUtils.BUFFER_SIZE)) {
                StreamUtils.copy(inStream, tar);
            }
            tar.closeArchiveEntry();
        }
        catch (Exception e) {
            logError("Failed to add archive entry: " + entryName, e);
        }
    }

    private static void addLinkFile(ArchiveOutputStream tar, File file, String entryName) {
        try {
            TarArchiveEntry entry = new TarArchiveEntry(entryName, TarConstants.LF_SYMLINK);
            entry.setLinkName(FileUtils.readSymlink(file));
            tar.putArchiveEntry(entry);
            tar.closeArchiveEntry();
        }
        catch (Exception e) {
            logError("Failed to add archive symlink: " + entryName, e);
        }
    }

    private static void addDirectory(ArchiveOutputStream tar, File folder, String basePath, ExclusionFilter filter) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (filter != null && !filter.shouldInclude(file)) {
                continue; // Skip files that should be excluded
            }
            if (FileUtils.isSymlink(file)) {
                addLinkFile(tar, file, basePath + file.getName());
            } else if (file.isDirectory()) {
                String entryName = basePath + file.getName() + "/";
                tar.putArchiveEntry(tar.createArchiveEntry(folder, entryName));
                tar.closeArchiveEntry();
                addDirectory(tar, file, entryName, filter);
            } else {
                addFile(tar, file, basePath + file.getName());
            }
        }
    }
    public static void compress(Type type, File file, File destination, int level) {
        compress(type, new File[]{file}, destination, level, null);
    }

    public static void compress(Type type, File file, File destination, int level, ExclusionFilter filter) {
        compress(type, new File[]{file}, destination, level, filter);
    }

    public static void compress(Type type, File[] files, File destination, int level, ExclusionFilter filter) {
        try (OutputStream outStream = getCompressorOutputStream(type, destination, level);
             TarArchiveOutputStream tar = new TarArchiveOutputStream(outStream)) {
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            for (File file : files) {
                if (filter != null && !filter.shouldInclude(file)) {
                    continue; // Skip files that should be excluded
                }
                if (FileUtils.isSymlink(file)) {
                    addLinkFile(tar, file, file.getName());
                } else if (file.isDirectory()) {
                    String basePath = file.getName() + "/";
                    tar.putArchiveEntry(tar.createArchiveEntry(file, basePath));
                    tar.closeArchiveEntry();
                    addDirectory(tar, file, basePath, filter);
                } else {
                    addFile(tar, file, file.getName());
                }
            }
            tar.finish();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static boolean extract(Type type, Context context, String assetFile, File destination) {
        return extract(type, context, assetFile, destination, null);
    }

    public static boolean extract(Type type, Context context, String assetFile, File destination, OnExtractFileListener onExtractFileListener) {
        try {
            return extract(type, context.getAssets().open(assetFile), destination, onExtractFileListener);
        }
        catch (IOException e) {
            return false;
        }
    }

    public static boolean extract(Type type, Context context, Uri source, File destination) {
        return extract(type, context, source, destination, null);
    }

    public static boolean extract(Type type, Context context, Uri source, File destination, OnExtractFileListener onExtractFileListener) {
        if (source == null) return false;
        try {
            if (source.toString().startsWith("/")) {
                return extract(type, new FileInputStream(source.toString()), destination, onExtractFileListener);
            } else {
                return extract(type, context.getContentResolver().openInputStream(source), destination, onExtractFileListener);
            }
        }
        catch (FileNotFoundException e) {
            return false;
        }
    }

    public static boolean extract(Type type, File source, File destination) {
        return extract(type, source, destination, null);
    }

    public static boolean extract(Type type, File source, File destination, OnExtractFileListener onExtractFileListener) {
        if (source == null || !source.isFile()) return false;
        try {
            return extract(type, new BufferedInputStream(new FileInputStream(source), StreamUtils.BUFFER_SIZE), destination, onExtractFileListener);
        }
        catch (FileNotFoundException e) {
            return false;
        }
    }

    private static boolean extract(Type type, InputStream source, File destination, OnExtractFileListener onExtractFileListener) {
        if (source == null) return false;
        File rootDir;
        try {
            rootDir = FileUtils.ensureCanonicalDirectory(destination);
        }
        catch (IOException e) {
            logError("Invalid extraction destination: " + destination, e);
            return false;
        }
        try (InputStream inStream = getCompressorInputStream(type, source);
             ArchiveInputStream tar = new TarArchiveInputStream(inStream)) {
            TarArchiveEntry entry;
            while ((entry = (TarArchiveEntry)tar.getNextEntry()) != null) {
                if (!tar.canReadEntryData(entry)) continue;
                File file = resolveExtractionTarget(rootDir, entry.getName());
                if (file == null) return false;
                file = applyListener(rootDir, file, entry.getSize(), onExtractFileListener);
                if (file == null) continue;
                if (!extractEntryData(tar, entry, file)) return false;
                if (!entry.isSymbolicLink()) FileUtils.chmod(file, 0771);
            }
            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static InputStream getCompressorInputStream(Type type, InputStream source) throws IOException {
        if (type == Type.XZ) {
            return new XZCompressorInputStream(source);
        }
        else if (type == Type.ZSTD) {
            return new ZstdCompressorInputStream(source);
        }
        return null;
    }

    private static OutputStream getCompressorOutputStream(Type type, File destination, int level) throws IOException {
        if (type == Type.XZ) {
            return new XZCompressorOutputStream(new BufferedOutputStream(new FileOutputStream(destination), StreamUtils.BUFFER_SIZE), level);
        }
        else if (type == Type.ZSTD) {
            return new ZstdCompressorOutputStream(new BufferedOutputStream(new FileOutputStream(destination), StreamUtils.BUFFER_SIZE), level);
        }
        return null;
    }

    public static void archive(File[] files, File destination, ExclusionFilter filter) {
        try (OutputStream outStream = new BufferedOutputStream(new FileOutputStream(destination), StreamUtils.BUFFER_SIZE);
             TarArchiveOutputStream tar = new TarArchiveOutputStream(outStream)) {
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            for (File file : files) {
                if (filter != null && !filter.shouldInclude(file)) {
                    continue; // Skip files that should be excluded
                }
                if (FileUtils.isSymlink(file)) {
                    addLinkFile(tar, file, file.getName());
                } else if (file.isDirectory()) {
                    String basePath = file.getName() + "/";
                    tar.putArchiveEntry(tar.createArchiveEntry(file, basePath));
                    tar.closeArchiveEntry();
                    addDirectory(tar, file, basePath, filter);
                } else {
                    addFile(tar, file, file.getName());
                }
            }
            tar.finish();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean extractTar(File source, File destination, OnExtractFileListener onExtractFileListener) {
        if (source == null || !source.isFile()) return false;
        File rootDir;
        try {
            rootDir = FileUtils.ensureCanonicalDirectory(destination);
        }
        catch (IOException e) {
            logError("Invalid tar extraction destination: " + destination, e);
            return false;
        }
        try (InputStream inStream = new BufferedInputStream(new FileInputStream(source), StreamUtils.BUFFER_SIZE);
             TarArchiveInputStream tar = new TarArchiveInputStream(inStream)) {
            TarArchiveEntry entry;
            String topLevelDirectory = null;
            while ((entry = (TarArchiveEntry) tar.getNextEntry()) != null) {
                if (!tar.canReadEntryData(entry)) continue;

                String entryName = normalizeArchiveEntryName(entry.getName());
                if (topLevelDirectory == null) {
                    if (entry.isDirectory()) {
                        topLevelDirectory = entryName;
                        continue; // Skip creating the top-level directory
                    }
                }

                // Skip the entire tmp directory
                if (isTmpDirectoryEntry(entryName)) {
                    Log.d("RestoreOp", "Skipping tmp directory: " + entryName);
                    continue;
                }

                // Adjust the extraction path to remove the top-level directory
                String adjustedName = stripTopLevelDirectory(entryName, topLevelDirectory);
                if (adjustedName.isEmpty()) continue;
                File file = resolveExtractionTarget(rootDir, adjustedName);
                if (file == null) return false;
                file = applyListener(rootDir, file, entry.getSize(), onExtractFileListener);
                if (file == null) continue;
                if (!extractEntryData(tar, entry, file)) return false;
                if (!entry.isSymbolicLink()) FileUtils.chmod(file, 0771);
            }
            return true;
        } catch (IOException e) {
            Log.e("RestoreOp", "Failed to extract tar file", e);
            return false;
        }
    }


}




