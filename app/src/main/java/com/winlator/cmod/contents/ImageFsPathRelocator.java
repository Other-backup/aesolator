package com.winlator.cmod.contents;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.core.WineUtils;
import com.winlator.cmod.xenvironment.ImageFs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class ImageFsPathRelocator {
    private static final String TAG = "ImageFsPathRelocator";
    private static final int MAX_SAMPLE_COUNT = 12;
    private static final String MARKER_NAME = ".aeso_imagefs_path_relocator_version";
    private static final String MARKER_VERSION = "2";
    private static final long MAX_IN_MEMORY_FILE_BYTES = 8L * 1024L * 1024L;
    private static final int MAX_BINARY_SEGMENT_BYTES = 128 * 1024;
    private static final String CWD_ROOT_ALIAS = "/proc/self/cwd";
    private static final String[] STALE_ROOTS = {
            "/data/data/com.winlator/files/rootfs",
            "/data/user/0/com.winlator/files/rootfs",
            "/data/data/app.gamenative/files/imagefs",
            "/data/user/0/app.gamenative/files/imagefs",
            "/data/data/com.gamenative/files/imagefs",
            "/data/user/0/com.gamenative/files/imagefs"
    };

    private ImageFsPathRelocator() {
    }

    public static Result relocateWineRuntime(@Nullable File installPath,
                                             @Nullable ContentProfile profile,
                                             @Nullable ImageFs imageFs) {
        File runtimeRoot = resolveRuntimeRoot(installPath, profile);
        return relocateTreeWithMarker(runtimeRoot, imageFs, "runtime", runtimeRoot);
    }

    public static Result relocateCurrentWineRuntime(@Nullable ImageFs imageFs) {
        if (imageFs == null) return new Result("runtime");
        File runtimeRoot = WineUtils.resolveCanonicalRuntimeRoot(new File(imageFs.getWinePath()));
        return relocateTreeWithMarker(runtimeRoot, imageFs, "runtime", runtimeRoot);
    }

    public static Result relocateRootfsCriticalSurface(@Nullable ImageFs imageFs) {
        Result aggregate = new Result("rootfs");
        if (imageFs == null || imageFs.getRootDir() == null || !imageFs.getRootDir().isDirectory()) return aggregate;

        File markerDir = imageFs.getConfigDir();
        if (hasFreshMarker(markerDir)) {
            aggregate.markerFresh = true;
            return aggregate;
        }

        File rootDir = imageFs.getRootDir();
        File[] roots = new File[] {
                new File(rootDir, "usr/bin"),
                new File(rootDir, "usr/local/bin"),
                new File(rootDir, "bin"),
                new File(rootDir, "usr/lib"),
                new File(rootDir, "usr/lib32"),
                new File(rootDir, "usr/lib64"),
                new File(rootDir, "lib"),
                new File(rootDir, "lib32"),
                new File(rootDir, "lib64"),
                new File(rootDir, "etc"),
                new File(rootDir, "usr/etc"),
                new File(rootDir, "usr/share/pkgconfig"),
                new File(rootDir, "usr/lib/pkgconfig")
        };

        for (File root : roots) {
            aggregate.merge(relocateTree(root, imageFs, "rootfs"));
        }
        writeFreshMarkerIfClean(markerDir, aggregate);
        return aggregate;
    }

    public static Result relocateTreeWithMarker(@Nullable File scanRoot,
                                                @Nullable ImageFs imageFs,
                                                @Nullable String owner,
                                                @Nullable File markerDir) {
        Result result = new Result(owner);
        if (hasFreshMarker(markerDir)) {
            result.markerFresh = true;
            return result;
        }
        result.merge(relocateTree(scanRoot, imageFs, owner));
        writeFreshMarkerIfClean(markerDir, result);
        return result;
    }

    public static Result relocateTree(@Nullable File scanRoot,
                                      @Nullable ImageFs imageFs,
                                      @Nullable String owner) {
        Result result = new Result(owner);
        if (scanRoot == null || imageFs == null || !scanRoot.exists()) return result;

        ArrayList<File> files = new ArrayList<>();
        collectFiles(scanRoot, files);
        for (File file : files) relocateFile(file, imageFs, result);
        return result;
    }

    public static void logResult(Context context, String eventId, Result result, @Nullable File scanRoot) {
        if (context == null || result == null || !result.hasSignal()) return;
        ForensicLogger.logEvent(
                context,
                result.failedFiles > 0 || result.overlongStrings > 0 ? "warn" : "info",
                eventId,
                null,
                "imagefs_path_relocator",
                result.failedFiles > 0 || result.overlongStrings > 0
                        ? "imagefs_path_relocation_incomplete"
                        : "imagefs_path_relocation_complete",
                ForensicLogger.fields(
                        "owner", result.owner,
                        "scan_root", scanRoot != null ? scanRoot.getAbsolutePath() : "",
                        "files_scanned", result.filesScanned,
                        "files_patched", result.patchedFiles,
                        "replacements", result.replacements,
                        "failed", result.failedFiles,
                        "overlong", result.overlongStrings,
                        "skipped", result.skippedFiles,
                        "marker_fresh", result.markerFresh,
                        "sample_count", result.samples.size(),
                        "samples", String.join(" | ", result.samples)
                )
        );
    }

    private static void relocateFile(File file, ImageFs imageFs, Result result) {
        if (file == null || !file.isFile() || shouldSkipName(file)) {
            result.skippedFiles++;
            return;
        }
        result.filesScanned++;

        if (file.length() > MAX_IN_MEMORY_FILE_BYTES) {
            relocateLargeBinaryFileInPlace(file, imageFs, result);
            return;
        }

        byte[] original;
        try {
            original = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            result.failedFiles++;
            result.addSample("read_failed:" + relativeToImageFs(imageFs, file) + ":" + e.getClass().getSimpleName());
            return;
        }
        if (!containsAnyStaleRoot(original)) return;

        boolean binary = containsNul(original);
        PatchBytes patch = binary
                ? relocateBinaryStrings(original)
                : relocateTextBytes(original, imageFs.getRootDir().getAbsolutePath());
        if (patch.replacements == 0 && patch.overlong == 0) return;
        if (patch.overlong > 0 || patch.bytes == null) {
            result.overlongStrings += patch.overlong;
            result.addSample("overlong:" + relativeToImageFs(imageFs, file));
            return;
        }

        boolean wasExecutable = file.canExecute();
        try {
            FileUtils.chmod(file, 0644);
            Files.write(file.toPath(), patch.bytes);
            FileUtils.chmod(file, wasExecutable ? 0755 : 0644);
            result.patchedFiles++;
            result.replacements += patch.replacements;
            result.addSample("patched:" + relativeToImageFs(imageFs, file) + ":" + patch.replacements);
        } catch (Throwable e) {
            Log.w(TAG, "Failed to relocate stale imagefs path in " + file.getAbsolutePath(), e);
            result.failedFiles++;
            result.addSample("write_failed:" + relativeToImageFs(imageFs, file) + ":" + e.getClass().getSimpleName());
        }
    }

    private static void relocateLargeBinaryFileInPlace(File file, ImageFs imageFs, Result result) {
        boolean wasExecutable = file.canExecute();
        int replacements = 0;
        int overlong = 0;
        int skippedSegments = 0;
        try {
            if (!largeFileContainsAnyStaleRoot(file)) return;
            FileUtils.chmod(file, 0644);
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
                ByteArrayOutputStream segment = new ByteArrayOutputStream(Math.min(4096, MAX_BINARY_SEGMENT_BYTES));
                boolean segmentTooLarge = false;
                long segmentStart = 0L;
                long position = 0L;
                int value;
                while ((value = randomAccessFile.read()) != -1) {
                    if (value == 0) {
                        PatchStats stats = relocateLargeBinarySegment(randomAccessFile, segmentStart, segment);
                        replacements += stats.replacements;
                        overlong += stats.overlong;
                        if (segmentTooLarge) skippedSegments++;
                        segment.reset();
                        segmentTooLarge = false;
                        segmentStart = position + 1L;
                    } else if (!segmentTooLarge) {
                        if (segment.size() < MAX_BINARY_SEGMENT_BYTES) {
                            segment.write(value);
                        } else {
                            segment.reset();
                            segmentTooLarge = true;
                        }
                    }
                    position++;
                }
                if (!segmentTooLarge && segment.size() > 0) {
                    PatchStats stats = relocateLargeBinarySegment(randomAccessFile, segmentStart, segment);
                    replacements += stats.replacements;
                    overlong += stats.overlong;
                } else if (segmentTooLarge) {
                    skippedSegments++;
                }
            } finally {
                FileUtils.chmod(file, wasExecutable ? 0755 : 0644);
            }

            if (replacements > 0) {
                result.patchedFiles++;
                result.replacements += replacements;
                result.addSample("patched_large:" + relativeToImageFs(imageFs, file) + ":" + replacements);
            }
            if (overlong > 0) {
                result.overlongStrings += overlong;
                result.addSample("overlong_large:" + relativeToImageFs(imageFs, file) + ":" + overlong);
            }
            if (skippedSegments > 0 && replacements == 0 && overlong == 0) {
                result.addSample("large_segments_skipped:" + relativeToImageFs(imageFs, file) + ":" + skippedSegments);
            }
        } catch (Throwable e) {
            Log.w(TAG, "Failed to stream-relocate stale imagefs path in " + file.getAbsolutePath(), e);
            result.failedFiles++;
            result.addSample("stream_failed:" + relativeToImageFs(imageFs, file) + ":" + e.getClass().getSimpleName());
            try {
                FileUtils.chmod(file, wasExecutable ? 0755 : 0644);
            } catch (Throwable ignored) {
            }
        }
    }

    private static boolean largeFileContainsAnyStaleRoot(File file) throws IOException {
        int longestRoot = 0;
        ArrayList<byte[]> needles = new ArrayList<>();
        for (String staleRoot : STALE_ROOTS) {
            byte[] needle = staleRoot.getBytes(StandardCharsets.ISO_8859_1);
            needles.add(needle);
            if (needle.length > longestRoot) longestRoot = needle.length;
        }
        int overlap = Math.max(0, longestRoot - 1);
        byte[] carry = new byte[overlap];
        int carryLength = 0;
        byte[] chunk = new byte[256 * 1024];
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            int read;
            while ((read = inputStream.read(chunk)) != -1) {
                byte[] window = new byte[carryLength + read];
                if (carryLength > 0) System.arraycopy(carry, 0, window, 0, carryLength);
                System.arraycopy(chunk, 0, window, carryLength, read);
                for (byte[] needle : needles) {
                    if (indexOf(window, needle) >= 0) return true;
                }
                carryLength = Math.min(overlap, window.length);
                if (carryLength > 0) {
                    System.arraycopy(window, window.length - carryLength, carry, 0, carryLength);
                }
            }
        }
        return false;
    }

    private static PatchStats relocateLargeBinarySegment(RandomAccessFile randomAccessFile,
                                                         long segmentStart,
                                                         ByteArrayOutputStream segment) throws IOException {
        if (segment == null || segment.size() == 0) return PatchStats.none();
        byte[] original = segment.toByteArray();
        if (!containsAnyStaleRoot(original)) return PatchStats.none();

        String value = new String(original, StandardCharsets.ISO_8859_1);
        String relocated = relocateBinaryString(value);
        if (relocated.equals(value)) return PatchStats.none();

        byte[] relocatedBytes = relocated.getBytes(StandardCharsets.ISO_8859_1);
        if (relocatedBytes.length > original.length) return new PatchStats(0, 1);

        randomAccessFile.seek(segmentStart);
        randomAccessFile.write(relocatedBytes);
        for (int i = relocatedBytes.length; i < original.length; i++) {
            randomAccessFile.write(0);
        }
        return new PatchStats(1, 0);
    }

    private static PatchBytes relocateTextBytes(byte[] original, String replacementRoot) {
        String text = new String(original, StandardCharsets.ISO_8859_1);
        int replacements = 0;
        for (String staleRoot : STALE_ROOTS) {
            int before = countOccurrences(text, staleRoot);
            if (before > 0) {
                text = text.replace(staleRoot, replacementRoot);
                replacements += before;
            }
        }
        if (replacements == 0) return PatchBytes.none();
        return new PatchBytes(text.getBytes(StandardCharsets.ISO_8859_1), replacements, 0);
    }

    private static PatchBytes relocateBinaryStrings(byte[] original) {
        byte[] patched = Arrays.copyOf(original, original.length);
        int replacements = 0;
        int overlong = 0;
        int start = 0;
        for (int i = 0; i <= original.length; i++) {
            if (i < original.length && original[i] != 0) continue;
            int length = i - start;
            if (length > 0) {
                String segment = new String(original, start, length, StandardCharsets.ISO_8859_1);
                String relocated = relocateBinaryString(segment);
                if (!relocated.equals(segment)) {
                    byte[] relocatedBytes = relocated.getBytes(StandardCharsets.ISO_8859_1);
                    if (relocatedBytes.length <= length) {
                        System.arraycopy(relocatedBytes, 0, patched, start, relocatedBytes.length);
                        Arrays.fill(patched, start + relocatedBytes.length, i, (byte) 0);
                        replacements++;
                    } else {
                        overlong++;
                    }
                }
            }
            start = i + 1;
        }
        if (replacements == 0 && overlong == 0) return PatchBytes.none();
        return new PatchBytes(patched, replacements, overlong);
    }

    private static String relocateBinaryString(String value) {
        String relocated = value;
        for (String staleRoot : STALE_ROOTS) {
            relocated = relocated.replace(staleRoot, CWD_ROOT_ALIAS);
        }
        return relocated;
    }

    private static boolean containsAnyStaleRoot(byte[] data) {
        if (data == null || data.length == 0) return false;
        for (String staleRoot : STALE_ROOTS) {
            if (indexOf(data, staleRoot.getBytes(StandardCharsets.ISO_8859_1)) >= 0) return true;
        }
        return false;
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        if (haystack == null || needle == null || needle.length == 0 || haystack.length < needle.length) return -1;
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    private static boolean containsNul(byte[] data) {
        if (data == null) return false;
        for (byte value : data) {
            if (value == 0) return true;
        }
        return false;
    }

    private static int countOccurrences(String text, String needle) {
        if (text == null || needle == null || needle.isEmpty()) return 0;
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private static boolean hasFreshMarker(@Nullable File markerDir) {
        if (markerDir == null || !markerDir.isDirectory()) return false;
        File marker = new File(markerDir, MARKER_NAME);
        if (!marker.isFile()) return false;
        String value = FileUtils.readString(marker);
        return MARKER_VERSION.equals(value != null ? value.trim() : "");
    }

    private static void writeFreshMarkerIfClean(@Nullable File markerDir, Result result) {
        if (markerDir == null || result == null) return;
        if (result.failedFiles > 0 || result.overlongStrings > 0) return;
        if (!markerDir.isDirectory() && !markerDir.mkdirs()) return;
        File marker = new File(markerDir, MARKER_NAME);
        if (FileUtils.writeString(marker, MARKER_VERSION)) FileUtils.chmod(marker, 0660);
    }

    private static void collectFiles(File root, List<File> files) {
        if (root == null || !root.exists()) return;
        try {
            if (Files.isSymbolicLink(root.toPath())) return;
        } catch (Exception ignored) {
        }
        if (shouldSkipName(root)) return;
        if (root.isFile()) {
            files.add(root);
            return;
        }

        File[] children = root.listFiles();
        if (children == null) return;
        for (File child : children) collectFiles(child, files);
    }

    private static boolean shouldSkipName(File file) {
        if (file == null) return true;
        String name = file.getName();
        return name.startsWith("._")
                || ".DS_Store".equals(name)
                || MARKER_NAME.equals(name);
    }

    @Nullable
    private static File resolveRuntimeRoot(@Nullable File installPath, @Nullable ContentProfile profile) {
        if (installPath == null || profile == null || !profile.isWineProtonFamily()) return null;
        return WineUtils.resolveCanonicalRuntimeRoot(installPath);
    }

    private static String relativeToImageFs(ImageFs imageFs, File file) {
        if (file == null) return "";
        if (imageFs == null || imageFs.getRootDir() == null) return file.getAbsolutePath();
        try {
            return imageFs.getRootDir()
                    .toPath()
                    .toAbsolutePath()
                    .normalize()
                    .relativize(file.toPath().toAbsolutePath().normalize())
                    .toString()
                    .replace(File.separatorChar, '/');
        } catch (Exception ignored) {
            return file.getAbsolutePath();
        }
    }

    private static final class PatchBytes {
        final byte[] bytes;
        final int replacements;
        final int overlong;

        PatchBytes(byte[] bytes, int replacements, int overlong) {
            this.bytes = bytes;
            this.replacements = replacements;
            this.overlong = overlong;
        }

        static PatchBytes none() {
            return new PatchBytes(null, 0, 0);
        }
    }

    private static final class PatchStats {
        final int replacements;
        final int overlong;

        PatchStats(int replacements, int overlong) {
            this.replacements = replacements;
            this.overlong = overlong;
        }

        static PatchStats none() {
            return new PatchStats(0, 0);
        }
    }

    public static final class Result {
        public final String owner;
        public int filesScanned;
        public int patchedFiles;
        public int replacements;
        public int failedFiles;
        public int overlongStrings;
        public int skippedFiles;
        public boolean markerFresh;
        public final ArrayList<String> samples = new ArrayList<>();

        Result(@Nullable String owner) {
            this.owner = owner == null || owner.trim().isEmpty() ? "unknown" : owner.trim();
        }

        public boolean hasSignal() {
            return patchedFiles > 0 || failedFiles > 0 || overlongStrings > 0 || replacements > 0;
        }

        void addSample(String sample) {
            if (sample == null || sample.trim().isEmpty()) return;
            if (samples.size() < MAX_SAMPLE_COUNT) samples.add(sample);
        }

        void merge(Result other) {
            if (other == null) return;
            filesScanned += other.filesScanned;
            patchedFiles += other.patchedFiles;
            replacements += other.replacements;
            failedFiles += other.failedFiles;
            overlongStrings += other.overlongStrings;
            skippedFiles += other.skippedFiles;
            markerFresh |= other.markerFresh;
            for (String sample : other.samples) addSample(sample);
        }

        public String toSummary() {
            return String.format(Locale.US,
                    "owner=%s files_scanned=%d files_patched=%d replacements=%d failed=%d overlong=%d skipped=%d marker_fresh=%s",
                    owner,
                    filesScanned,
                    patchedFiles,
                    replacements,
                    failedFiles,
                    overlongStrings,
                    skippedFiles,
                    markerFresh);
        }
    }
}
