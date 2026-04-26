package com.winlator.cmod.contents;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class RuntimePayloadClassifier {
    static final class Result {
        final String runtimeModel;
        final int bionicScore;
        final int glibcScore;
        final String signals;

        private Result(String runtimeModel, int bionicScore, int glibcScore, String signals) {
            this.runtimeModel = runtimeModel;
            this.bionicScore = bionicScore;
            this.glibcScore = glibcScore;
            this.signals = signals;
        }
    }

    private static final String[] BIONIC_STRONG_PATHS = {
            "arm64-v8a/bin/wine",
            "arm64-v8a/bin/wine64",
            "arm64-v8a/lib/wine/aarch64-unix/wineandroid.so",
            "arm64-v8a/lib/wine/aarch64-unix/winex11.so",
            "arm64-v8a/lib/wine/aarch64-windows/wineandroid.drv",
            "arm64-v8a/lib/wine/aarch64-windows/winex11.drv"
    };

    private static final String[] BIONIC_WEAK_PATHS = {
            "arm64-v8a/bin",
            "arm64-v8a/lib",
            "arm64-v8a/share",
            "lib/wine/aarch64-unix/wineandroid.so",
            "lib/wine/aarch64-unix/winex11.so"
    };

    private static final String[] ELF_PROBE_PATHS = {
            "bin/wine",
            "bin/wine64",
            "arm64-v8a/bin/wine",
            "arm64-v8a/bin/wine64",
            "lib/wine/aarch64-unix/ntdll.so",
            "lib/wine/aarch64-unix/winex11.so",
            "lib/wine/aarch64-unix/wineandroid.so",
            "arm64-v8a/lib/wine/aarch64-unix/ntdll.so",
            "arm64-v8a/lib/wine/aarch64-unix/winex11.so",
            "arm64-v8a/lib/wine/aarch64-unix/wineandroid.so",
            "lib/wine/x86_64-unix/ntdll.so",
            "lib/wine/i386-unix/ntdll.so"
    };

    private static final String[] BIONIC_ELF_NEEDLES = {
            "__bionic",
            ".note.android.ident",
            "android_update_LD_LIBRARY_PATH",
            "/system/bin/linker",
            "/apex/com.android.runtime/bin/linker"
    };

    private static final String[] GLIBC_ELF_NEEDLES = {
            "GLIBC_",
            "GNU C Library",
            "ld-linux-aarch64.so.1",
            "ld-linux-x86-64.so.2",
            "/lib64/ld-linux",
            "/lib/ld-linux"
    };

    private static final String[] GLIBC_STRONG_PATHS = {
            "lib/ld-linux-aarch64.so.1",
            "lib64/ld-linux-aarch64.so.1",
            "usr/glibc/bin",
            "usr/glibc/lib",
            "usr/lib/aarch64-linux-gnu",
            "lib/aarch64-linux-gnu"
    };

    private static final String[] GLIBC_WEAK_PATHS = {
            "etc/ld.so.conf",
            "usr/etc/ld.so.conf",
            "usr/lib/x86_64-linux-gnu",
            "lib/x86_64-linux-gnu"
    };
    private static final int MAX_ELF_PROBE_BYTES = 1024 * 1024;
    private static final int MAX_CLASSIFICATION_CACHE_ENTRIES = 192;
    private static final byte[][] BIONIC_ELF_NEEDLE_BYTES = encodeNeedles(BIONIC_ELF_NEEDLES);
    private static final byte[][] GLIBC_ELF_NEEDLE_BYTES = encodeNeedles(GLIBC_ELF_NEEDLES);
    private static final LinkedHashMap<String, Result> CLASSIFICATION_CACHE =
            new LinkedHashMap<String, Result>(MAX_CLASSIFICATION_CACHE_ENTRIES, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Result> eldest) {
                    return size() > MAX_CLASSIFICATION_CACHE_ENTRIES;
                }
            };

    private RuntimePayloadClassifier() {
    }

    @NonNull
    static Result classify(@Nullable File rootDir,
                           @Nullable ContentProfile parsedProfile,
                           @Nullable ContentProfile remoteHint,
                           @Nullable String importDisplayName) {
        String cacheKey = buildClassificationCacheKey(rootDir, parsedProfile, remoteHint, importDisplayName);
        if (cacheKey != null) {
            synchronized (CLASSIFICATION_CACHE) {
                Result cached = CLASSIFICATION_CACHE.get(cacheKey);
                if (cached != null) return cached;
            }
        }

        Result result = classifyUncached(rootDir, parsedProfile, remoteHint, importDisplayName);
        if (cacheKey != null) {
            synchronized (CLASSIFICATION_CACHE) {
                CLASSIFICATION_CACHE.put(cacheKey, result);
            }
        }
        return result;
    }

    @NonNull
    private static Result classifyUncached(@Nullable File rootDir,
                                           @Nullable ContentProfile parsedProfile,
                                           @Nullable ContentProfile remoteHint,
                                           @Nullable String importDisplayName) {
        int bionicScore = 0;
        int glibcScore = 0;
        ArrayList<String> signals = new ArrayList<>();
        ElfNeedleScan elfNeedleScan = new ElfNeedleScan();

        if (rootDir != null && rootDir.isDirectory()) {
            bionicScore += scorePaths(rootDir, BIONIC_STRONG_PATHS, 8, "bionic-strong", signals);
            bionicScore += scorePaths(rootDir, BIONIC_WEAK_PATHS, 3, "bionic", signals);
            glibcScore += scorePaths(rootDir, GLIBC_STRONG_PATHS, 8, "glibc-strong", signals);
            glibcScore += scorePaths(rootDir, GLIBC_WEAK_PATHS, 3, "glibc", signals);

            elfNeedleScan = scanElfNeedles(rootDir, signals);
            bionicScore += elfNeedleScan.bionicHits * 10;
            glibcScore += elfNeedleScan.glibcHits * 10;
        }

        int strongBionicScore = countExistingPaths(rootDir, BIONIC_STRONG_PATHS);
        int strongGlibcScore = countExistingPaths(rootDir, GLIBC_STRONG_PATHS);
        int strongBionicElfScore = elfNeedleScan.bionicHits;
        int strongGlibcElfScore = elfNeedleScan.glibcHits;
        String explicit = firstRuntimeModelHint(parsedProfile, remoteHint, importDisplayName);
        if (ContentProfile.RUNTIME_MODEL_BIONIC.equals(explicit)) {
            bionicScore += 2;
            signals.add("hint:bionic");
        } else if (ContentProfile.RUNTIME_MODEL_GLIBC.equals(explicit)) {
            glibcScore += 2;
            signals.add("hint:glibc");
        }

        String model = "";
        if (strongBionicElfScore > 0 && strongGlibcElfScore == 0) {
            model = ContentProfile.RUNTIME_MODEL_BIONIC;
        } else if (strongGlibcElfScore > 0 && strongBionicElfScore == 0) {
            model = ContentProfile.RUNTIME_MODEL_GLIBC;
        } else if (strongBionicScore > 0) {
            model = ContentProfile.RUNTIME_MODEL_BIONIC;
        } else if (strongGlibcScore > 0) {
            model = ContentProfile.RUNTIME_MODEL_GLIBC;
        } else if (bionicScore > 0 || glibcScore > 0) {
            model = bionicScore >= glibcScore ? ContentProfile.RUNTIME_MODEL_BIONIC : ContentProfile.RUNTIME_MODEL_GLIBC;
        } else {
            model = explicit;
        }

        return new Result(model, bionicScore, glibcScore, String.join(",", signals));
    }

    static String describe(@Nullable File rootDir,
                           @Nullable ContentProfile parsedProfile,
                           @Nullable ContentProfile remoteHint,
                           @Nullable String importDisplayName) {
        Result result = classify(rootDir, parsedProfile, remoteHint, importDisplayName);
        return "model=" + (result.runtimeModel.isEmpty() ? "-" : result.runtimeModel)
                + ",bionic_score=" + result.bionicScore
                + ",glibc_score=" + result.glibcScore
                + ",signals=" + (result.signals.isEmpty() ? "-" : result.signals);
    }

    private static int scorePaths(File rootDir,
                                  String[] paths,
                                  int scorePerPath,
                                  String label,
                                  List<String> signals) {
        if (rootDir == null || paths == null) return 0;
        int score = 0;
        for (String path : paths) {
            if (path == null || path.isEmpty()) continue;
            if (new File(rootDir, path).exists()) {
                score += scorePerPath;
                signals.add(label + ":" + path);
            }
        }
        return score;
    }

    private static int countExistingPaths(@Nullable File rootDir, String[] paths) {
        if (rootDir == null || paths == null) return 0;
        int count = 0;
        for (String path : paths) {
            if (path != null && !path.isEmpty() && new File(rootDir, path).exists()) count++;
        }
        return count;
    }

    private static ElfNeedleScan scanElfNeedles(File rootDir, List<String> signals) {
        ElfNeedleScan total = new ElfNeedleScan();
        if (rootDir == null) return total;
        for (String probePath : ELF_PROBE_PATHS) {
            File probeFile = new File(rootDir, probePath);
            if (!probeFile.isFile()) continue;
            FileNeedleScan fileScan = scanFileForNeedles(probeFile);
            if (fileScan.bionicNeedle != null) {
                total.bionicHits++;
                signals.add("bionic-elf:" + probePath + ":" + fileScan.bionicNeedle);
            }
            if (fileScan.glibcNeedle != null) {
                total.glibcHits++;
                signals.add("glibc-elf:" + probePath + ":" + fileScan.glibcNeedle);
            }
        }
        return total;
    }

    @NonNull
    private static FileNeedleScan scanFileForNeedles(File file) {
        FileNeedleScan result = new FileNeedleScan();
        if (file == null || !file.isFile()) return result;
        byte[] buffer = new byte[65536];
        byte[] carry = new byte[128];
        int carryLength = 0;
        long totalRead = 0;
        try (FileInputStream input = new FileInputStream(file)) {
            int read;
            while ((read = input.read(buffer)) != -1 && totalRead < MAX_ELF_PROBE_BYTES) {
                int scanLength = carryLength + read;
                byte[] scanBuffer = new byte[scanLength];
                System.arraycopy(carry, 0, scanBuffer, 0, carryLength);
                System.arraycopy(buffer, 0, scanBuffer, carryLength, read);
                if (result.bionicNeedle == null) {
                    result.bionicNeedle = firstNeedleInBuffer(scanBuffer, scanLength, BIONIC_ELF_NEEDLES, BIONIC_ELF_NEEDLE_BYTES);
                }
                if (result.glibcNeedle == null) {
                    result.glibcNeedle = firstNeedleInBuffer(scanBuffer, scanLength, GLIBC_ELF_NEEDLES, GLIBC_ELF_NEEDLE_BYTES);
                }
                if (result.bionicNeedle != null && result.glibcNeedle != null) {
                    return result;
                }
                carryLength = Math.min(carry.length, scanLength);
                System.arraycopy(scanBuffer, scanLength - carryLength, carry, 0, carryLength);
                totalRead += read;
            }
        } catch (IOException ignored) {
        }
        return result;
    }

    @Nullable
    private static String firstNeedleInBuffer(byte[] haystack, int haystackLength, String[] needles, byte[][] encodedNeedles) {
        if (needles == null || encodedNeedles == null) return null;
        for (int i = 0; i < encodedNeedles.length; i++) {
            if (indexOf(haystack, haystackLength, encodedNeedles[i]) >= 0) {
                return needles[i];
            }
        }
        return null;
    }

    private static byte[][] encodeNeedles(String[] needles) {
        byte[][] encoded = new byte[needles.length][];
        for (int i = 0; i < needles.length; i++) {
            encoded[i] = needles[i].getBytes(StandardCharsets.UTF_8);
        }
        return encoded;
    }

    private static int indexOf(byte[] haystack, int haystackLength, byte[] needle) {
        if (haystack == null || needle == null || needle.length == 0 || haystackLength < needle.length) return -1;
        int max = haystackLength - needle.length;
        for (int i = 0; i <= max; i++) {
            int j = 0;
            while (j < needle.length && haystack[i + j] == needle[j]) {
                j++;
            }
            if (j == needle.length) return i;
        }
        return -1;
    }

    @Nullable
    private static String buildClassificationCacheKey(@Nullable File rootDir,
                                                      @Nullable ContentProfile parsedProfile,
                                                      @Nullable ContentProfile remoteHint,
                                                      @Nullable String importDisplayName) {
        if (rootDir == null || !rootDir.isDirectory()) return null;
        StringBuilder builder = new StringBuilder(rootDir.getAbsolutePath()).append('|');
        for (String probePath : ELF_PROBE_PATHS) {
            File probeFile = new File(rootDir, probePath);
            if (!probeFile.isFile()) continue;
            builder.append(probePath)
                    .append(':')
                    .append(probeFile.length())
                    .append(':')
                    .append(probeFile.lastModified())
                    .append('|');
        }
        builder.append(firstRuntimeModelHint(parsedProfile, remoteHint, importDisplayName));
        return builder.toString();
    }

    private static final class ElfNeedleScan {
        private int bionicHits;
        private int glibcHits;
    }

    private static final class FileNeedleScan {
        @Nullable
        private String bionicNeedle;
        @Nullable
        private String glibcNeedle;
    }

    private static String firstRuntimeModelHint(@Nullable ContentProfile parsedProfile,
                                                @Nullable ContentProfile remoteHint,
                                                @Nullable String importDisplayName) {
        String parsed = parsedProfile != null ? ContentProfile.normalizeRuntimeModel(parsedProfile.runtimeModel) : "";
        if (!parsed.isEmpty()) return parsed;
        String remote = remoteHint != null ? ContentProfile.normalizeRuntimeModel(remoteHint.runtimeModel) : "";
        if (!remote.isEmpty()) return remote;
        String inferred = ContentProfile.inferRuntimeModel(
                null,
                importDisplayName,
                parsedProfile != null ? parsedProfile.verName : "",
                parsedProfile != null ? parsedProfile.desc : "",
                parsedProfile != null ? parsedProfile.artifactName : "",
                remoteHint != null ? remoteHint.verName : "",
                remoteHint != null ? remoteHint.desc : "",
                remoteHint != null ? remoteHint.artifactName : "",
                remoteHint != null ? remoteHint.remoteUrl : ""
        );
        return inferred == null ? "" : inferred.trim().toLowerCase(Locale.US);
    }
}
