package com.winlator.cmod.contents;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.core.PatchElf;
import com.winlator.cmod.core.WineUtils;
import com.winlator.cmod.xenvironment.ImageFs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WineRuntimeElfInterpreterSanitizer {
    private static final String TAG = "WineElfInterpSanitizer";
    private static final int MAX_SAMPLE_COUNT = 12;
    private static final String MARKER_NAME = ".aeso_elf_interpreter_sanitizer_version";
    private static final String MARKER_VERSION = "3";

    private WineRuntimeElfInterpreterSanitizer() {
    }

    public static Result sanitizeWineRuntime(@Nullable File installPath,
                                             @Nullable ContentProfile profile,
                                             @Nullable ImageFs imageFs) {
        File runtimeRoot = resolveRuntimeRoot(installPath, profile);
        return sanitizeRuntimeCriticalSurface(runtimeRoot, imageFs);
    }

    public static Result sanitizeCurrentWineRuntime(@Nullable ImageFs imageFs) {
        if (imageFs == null) return new Result("runtime");
        File runtimeRoot = WineUtils.resolveCanonicalRuntimeRoot(new File(imageFs.getWinePath()));
        return sanitizeRuntimeCriticalSurface(runtimeRoot, imageFs);
    }

    public static Result sanitizeRootfsCriticalSurface(@Nullable ImageFs imageFs) {
        Result aggregate = new Result("rootfs");
        if (imageFs == null || imageFs.getRootDir() == null || !imageFs.getRootDir().isDirectory()) return aggregate;

        File rootDir = imageFs.getRootDir();
        File markerDir = imageFs.getConfigDir();
        if (hasFreshMarker(markerDir)) {
            aggregate.markerFresh = true;
            return aggregate;
        }
        File[] roots = new File[] {
                new File(rootDir, "usr/bin"),
                new File(rootDir, "usr/local/bin"),
                new File(rootDir, "bin")
        };

        for (File root : roots) {
            aggregate.merge(sanitizeTree(root, imageFs, "rootfs"));
        }
        writeFreshMarkerIfClean(markerDir, aggregate);
        return aggregate;
    }

    public static Result sanitizeRuntimeCriticalSurface(@Nullable File runtimeRoot,
                                                        @Nullable ImageFs imageFs) {
        Result aggregate = new Result("runtime");
        if (runtimeRoot == null || imageFs == null || !runtimeRoot.exists()) return aggregate;
        if (hasFreshMarker(runtimeRoot)) {
            aggregate.markerFresh = true;
            return aggregate;
        }

        File[] roots = new File[] {
                new File(runtimeRoot, "bin"),
                new File(runtimeRoot, "lib"),
                new File(runtimeRoot, "lib64"),
                new File(runtimeRoot, "lib/wine/aarch64-unix"),
                new File(runtimeRoot, "lib/wine/x86_64-unix"),
                new File(runtimeRoot, "lib/wine/i386-unix"),
                new File(runtimeRoot, "arm64-v8a/bin"),
                new File(runtimeRoot, "arm64-v8a/lib"),
                new File(runtimeRoot, "arm64-v8a/lib/wine/aarch64-unix"),
                new File(runtimeRoot, "arm64-v8a/lib/wine/x86_64-unix"),
                new File(runtimeRoot, "arm64-v8a/lib/wine/i386-unix")
        };

        for (File root : roots) {
            aggregate.merge(sanitizeTree(root, imageFs, "runtime"));
        }
        writeFreshMarkerIfClean(runtimeRoot, aggregate);
        return aggregate;
    }

    public static Result sanitizeTreeWithMarker(@Nullable File scanRoot,
                                                @Nullable ImageFs imageFs,
                                                @Nullable String owner,
                                                @Nullable File markerDir) {
        Result result = new Result(owner);
        if (hasFreshMarker(markerDir)) {
            result.markerFresh = true;
            return result;
        }
        result.merge(sanitizeTree(scanRoot, imageFs, owner));
        writeFreshMarkerIfClean(markerDir, result);
        return result;
    }

    public static Result sanitizeTree(@Nullable File scanRoot,
                                      @Nullable ImageFs imageFs,
                                      @Nullable String owner) {
        Result result = new Result(owner);
        if (scanRoot == null || imageFs == null || !scanRoot.exists()) return result;

        ArrayList<File> files = new ArrayList<>();
        collectFiles(scanRoot, files);
        for (File file : files) {
            PatchOutcome outcome = sanitizeFile(file, imageFs, result);
            switch (outcome) {
                case NOT_ELF -> result.nonElfFiles++;
                case UNCHANGED -> result.elfFilesScanned++;
                case MISSING_TARGET -> {
                    result.elfFilesScanned++;
                    result.missingTargetFiles++;
                }
                case PATCHED -> {
                    result.elfFilesScanned++;
                    result.patchedFiles++;
                }
                case FAILED -> {
                    result.elfFilesScanned++;
                    result.failedFiles++;
                }
            }
        }
        return result;
    }

    public static void logResult(Context context, String eventId, Result result, @Nullable File scanRoot) {
        if (result == null || !result.hasSignal()) return;
        ForensicLogger.logEvent(
                context,
                result.failedFiles > 0 || result.missingTargetFiles > 0 ? "warn" : "info",
                eventId,
                null,
                "elf_interpreter",
                "elf_interpreter_rebind",
                ForensicLogger.fields(
                        "owner", result.owner,
                        "scan_root", scanRoot != null ? scanRoot.getAbsolutePath() : "",
                        "elf_scanned", result.elfFilesScanned,
                        "patched", result.patchedFiles,
                        "failed", result.failedFiles,
                        "missing_target", result.missingTargetFiles,
                        "stale_interpreters", result.staleInterpreterFiles,
                        "skipped_non_elf", result.nonElfFiles,
                        "marker_fresh", result.markerFresh,
                        "sample_count", result.samples.size(),
                        "samples", String.join(" | ", result.samples)
                )
        );
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
        if (result.failedFiles > 0 || result.missingTargetFiles > 0) return;
        if (!markerDir.isDirectory() && !markerDir.mkdirs()) return;
        File marker = new File(markerDir, MARKER_NAME);
        if (FileUtils.writeString(marker, MARKER_VERSION)) {
            FileUtils.chmod(marker, 0660);
        }
    }

    private static void collectFiles(File root, List<File> files) {
        if (root == null || !root.exists()) return;
        if (Files.isSymbolicLink(root.toPath())) return;
        if (shouldSkipName(root)) return;
        if (root.isFile()) {
            files.add(root);
            return;
        }

        File[] children = root.listFiles();
        if (children == null) return;
        for (File child : children) {
            collectFiles(child, files);
        }
    }

    private static PatchOutcome sanitizeFile(File file, ImageFs imageFs, Result result) {
        if (file == null || !file.isFile()) return PatchOutcome.NOT_ELF;
        if (shouldSkipName(file) || !looksLikeElf(file)) return PatchOutcome.NOT_ELF;

        PatchElf patchElf = new PatchElf();
        try {
            if (!patchElf.loadElf(file)) return PatchOutcome.NOT_ELF;
            String currentInterpreter = normalizePath(patchElf.getInterpreter());
            if (currentInterpreter.isEmpty()) return PatchOutcome.UNCHANGED;
            if (!isGlibcInterpreter(currentInterpreter)) return PatchOutcome.UNCHANGED;
            if (isForeignGuestInterpreter(currentInterpreter)) return PatchOutcome.UNCHANGED;

            String targetInterpreter = resolveTargetInterpreter(imageFs, currentInterpreter);
            if (targetInterpreter.isEmpty()) {
                result.missingInterpreterSamples++;
                result.addSample("missing-target:" + relativeToImageFs(imageFs, file) + ":" + currentInterpreter);
                return PatchOutcome.MISSING_TARGET;
            }

            if (targetInterpreter.equals(currentInterpreter)) return PatchOutcome.UNCHANGED;

            result.staleInterpreterFiles++;
            result.addSample("rebind:" + relativeToImageFs(imageFs, file)
                    + ":" + currentInterpreter + "->" + targetInterpreter);
            boolean wasExecutable = file.canExecute();
            FileUtils.chmod(file, 0755);
            if (!patchElf.setInterpreter(targetInterpreter)) {
                Log.w(TAG, "Failed to set ELF interpreter for " + file.getAbsolutePath());
                return PatchOutcome.FAILED;
            }
            if (!patchElf.saveElf()) {
                Log.w(TAG, "Failed to save ELF after interpreter rebind: " + file.getAbsolutePath());
                return PatchOutcome.FAILED;
            }
            FileUtils.chmod(file, wasExecutable ? 0755 : 0644);
            return PatchOutcome.PATCHED;
        } catch (Throwable error) {
            Log.w(TAG, "Failed to sanitize ELF interpreter for " + file.getAbsolutePath(), error);
            result.addSample("failed:" + relativeToImageFs(imageFs, file) + ":" + error.getClass().getSimpleName());
            return PatchOutcome.FAILED;
        } finally {
            patchElf.unloadElf();
        }
    }

    private static boolean shouldSkipName(File file) {
        if (file == null) return true;
        String name = file.getName();
        return name.startsWith("._")
                || ".DS_Store".equals(name)
                || MARKER_NAME.equals(name);
    }

    private static boolean looksLikeElf(File file) {
        if (file == null || !file.isFile() || file.length() < 4L) return false;
        byte[] magic = new byte[4];
        try (FileInputStream inputStream = new FileInputStream(file)) {
            if (inputStream.read(magic) != magic.length) return false;
            return magic[0] == 0x7f
                    && magic[1] == 'E'
                    && magic[2] == 'L'
                    && magic[3] == 'F';
        } catch (IOException ignored) {
            return false;
        }
    }

    private static boolean isGlibcInterpreter(String interpreter) {
        String normalized = interpreter.toLowerCase(Locale.US);
        return normalized.contains("ld-linux")
                || normalized.contains("ld.so")
                || normalized.contains("libc.musl");
    }

    private static boolean isForeignGuestInterpreter(String interpreter) {
        String basename = new File(interpreter).getName();
        return "ld-linux-x86-64.so.2".equals(basename)
                || "ld-linux.so.2".equals(basename);
    }

    private static String resolveTargetInterpreter(ImageFs imageFs, String currentInterpreter) {
        if (imageFs == null || currentInterpreter == null) return "";
        String basename = new File(currentInterpreter).getName();
        File rootDir = imageFs.getRootDir();
        if (rootDir == null) return "";

        String donorRelativeTarget = resolveGuestAbsoluteTargetFromDonorInterpreter(rootDir, currentInterpreter);
        if (!donorRelativeTarget.isEmpty()) return donorRelativeTarget;

        ArrayList<File> candidates = new ArrayList<>();
        if ("ld-linux-aarch64.so.1".equals(basename)) {
            candidates.add(new File(rootDir, "usr/lib/ld-linux-aarch64.so.1"));
            candidates.add(new File(rootDir, "lib/ld-linux-aarch64.so.1"));
            candidates.add(new File(rootDir, "lib64/ld-linux-aarch64.so.1"));
        } else if ("ld-linux-armhf.so.3".equals(basename)) {
            candidates.add(new File(rootDir, "usr/lib/arm-linux-gnueabihf/ld-linux-armhf.so.3"));
            candidates.add(new File(rootDir, "lib/ld-linux-armhf.so.3"));
        } else if ("ld-linux-x86-64.so.2".equals(basename)) {
            candidates.add(new File(rootDir, "usr/lib/x86_64-linux-gnu/ld-linux-x86-64.so.2"));
            candidates.add(new File(rootDir, "lib64/ld-linux-x86-64.so.2"));
            candidates.add(new File(rootDir, "lib/ld-linux-x86-64.so.2"));
        } else if ("ld-linux.so.2".equals(basename)) {
            candidates.add(new File(rootDir, "usr/lib/i386-linux-gnu/ld-linux.so.2"));
            candidates.add(new File(rootDir, "lib/ld-linux.so.2"));
        } else {
            candidates.add(new File(rootDir, "usr/lib/" + basename));
            candidates.add(new File(rootDir, "lib/" + basename));
            candidates.add(new File(rootDir, "lib64/" + basename));
        }

        for (File candidate : candidates) {
            String guestAbsoluteTarget = toGuestAbsoluteTarget(rootDir, candidate);
            if (!guestAbsoluteTarget.isEmpty()) return guestAbsoluteTarget;
        }
        return "";
    }

    private static String resolveGuestAbsoluteTargetFromDonorInterpreter(File rootDir, String currentInterpreter) {
        if (rootDir == null || currentInterpreter == null) return "";
        String normalized = currentInterpreter.trim().replace('\\', '/');
        String[] anchors = new String[] {
                "/proc/self/cwd/",
                "/files/imagefs/",
                "/files/imagefs-glibc/",
                "/files/imagefs-bionic/"
        };
        for (String anchor : anchors) {
            int index = normalized.indexOf(anchor);
            if (index < 0) continue;
            String relative = normalized.substring(index + anchor.length());
            String target = toGuestAbsoluteTarget(rootDir, new File(rootDir, relative));
            if (!target.isEmpty()) return target;
        }
        return "";
    }

    private static String toGuestAbsoluteTarget(File rootDir, File candidate) {
        if (rootDir == null || candidate == null || !candidate.isFile()) return "";
        try {
            String relative = rootDir.toPath()
                    .toAbsolutePath()
                    .normalize()
                    .relativize(candidate.toPath().toAbsolutePath().normalize())
                    .toString()
                    .replace(File.separatorChar, '/');
            if (relative.isEmpty() || relative.startsWith("../")) return "";
            return "/" + relative;
        } catch (Exception ignored) {
            return "";
        }
    }

    @Nullable
    private static File resolveRuntimeRoot(@Nullable File installPath, @Nullable ContentProfile profile) {
        if (installPath == null || profile == null || !profile.isWineProtonFamily()) return null;
        return WineUtils.resolveCanonicalRuntimeRoot(installPath);
    }

    private static String normalizePath(String value) {
        if (value == null) return "";
        String normalized = value.trim();
        int nulIndex = normalized.indexOf('\0');
        if (nulIndex >= 0) normalized = normalized.substring(0, nulIndex).trim();
        while (normalized.endsWith("]")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized;
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

    public static final class Result {
        public final String owner;
        public int nonElfFiles;
        public int elfFilesScanned;
        public int patchedFiles;
        public int failedFiles;
        public int missingTargetFiles;
        public int missingInterpreterSamples;
        public int staleInterpreterFiles;
        public boolean markerFresh;
        public final ArrayList<String> samples = new ArrayList<>();

        Result(@Nullable String owner) {
            this.owner = owner == null || owner.trim().isEmpty() ? "unknown" : owner.trim();
        }

        public boolean hasSignal() {
            return patchedFiles > 0 || failedFiles > 0 || missingTargetFiles > 0 || staleInterpreterFiles > 0;
        }

        void addSample(String sample) {
            if (sample == null || sample.trim().isEmpty()) return;
            if (samples.size() < MAX_SAMPLE_COUNT) samples.add(sample);
        }

        void merge(Result other) {
            if (other == null) return;
            nonElfFiles += other.nonElfFiles;
            elfFilesScanned += other.elfFilesScanned;
            patchedFiles += other.patchedFiles;
            failedFiles += other.failedFiles;
            missingTargetFiles += other.missingTargetFiles;
            missingInterpreterSamples += other.missingInterpreterSamples;
            staleInterpreterFiles += other.staleInterpreterFiles;
            markerFresh |= other.markerFresh;
            for (String sample : other.samples) addSample(sample);
        }

        public String toSummary() {
            return String.format(Locale.US,
                    "owner=%s elf_scanned=%d patched=%d failed=%d missing_target=%d stale=%d skipped_non_elf=%d marker_fresh=%s",
                    owner,
                    elfFilesScanned,
                    patchedFiles,
                    failedFiles,
                    missingTargetFiles,
                    staleInterpreterFiles,
                    nonElfFiles,
                    markerFresh);
        }
    }

    private enum PatchOutcome {
        NOT_ELF,
        UNCHANGED,
        MISSING_TARGET,
        PATCHED,
        FAILED
    }
}
