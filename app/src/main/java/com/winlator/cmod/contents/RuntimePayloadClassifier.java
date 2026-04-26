package com.winlator.cmod.contents;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    private RuntimePayloadClassifier() {
    }

    @NonNull
    static Result classify(@Nullable File rootDir,
                           @Nullable ContentProfile parsedProfile,
                           @Nullable ContentProfile remoteHint,
                           @Nullable String importDisplayName) {
        int bionicScore = 0;
        int glibcScore = 0;
        ArrayList<String> signals = new ArrayList<>();

        if (rootDir != null && rootDir.isDirectory()) {
            bionicScore += scorePaths(rootDir, BIONIC_STRONG_PATHS, 8, "bionic-strong", signals);
            bionicScore += scorePaths(rootDir, BIONIC_WEAK_PATHS, 3, "bionic", signals);
            glibcScore += scorePaths(rootDir, GLIBC_STRONG_PATHS, 8, "glibc-strong", signals);
            glibcScore += scorePaths(rootDir, GLIBC_WEAK_PATHS, 3, "glibc", signals);

            int bionicElfScore = scoreElfNeedles(rootDir, BIONIC_ELF_NEEDLES, 10, "bionic-elf", signals);
            int glibcElfScore = scoreElfNeedles(rootDir, GLIBC_ELF_NEEDLES, 10, "glibc-elf", signals);
            bionicScore += bionicElfScore;
            glibcScore += glibcElfScore;
        }

        int strongBionicScore = countExistingPaths(rootDir, BIONIC_STRONG_PATHS);
        int strongGlibcScore = countExistingPaths(rootDir, GLIBC_STRONG_PATHS);
        int strongBionicElfScore = countElfNeedles(rootDir, BIONIC_ELF_NEEDLES);
        int strongGlibcElfScore = countElfNeedles(rootDir, GLIBC_ELF_NEEDLES);
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

    private static int scoreElfNeedles(File rootDir,
                                       String[] needles,
                                       int scorePerHit,
                                       String label,
                                       List<String> signals) {
        if (rootDir == null || needles == null) return 0;
        int score = 0;
        for (String probePath : ELF_PROBE_PATHS) {
            File probeFile = new File(rootDir, probePath);
            if (!probeFile.isFile()) continue;
            String matchedNeedle = firstNeedleInFile(probeFile, needles);
            if (matchedNeedle == null) continue;
            score += scorePerHit;
            signals.add(label + ":" + probePath + ":" + matchedNeedle);
        }
        return score;
    }

    private static int countElfNeedles(@Nullable File rootDir, String[] needles) {
        if (rootDir == null || needles == null) return 0;
        int count = 0;
        for (String probePath : ELF_PROBE_PATHS) {
            File probeFile = new File(rootDir, probePath);
            if (probeFile.isFile() && firstNeedleInFile(probeFile, needles) != null) count++;
        }
        return count;
    }

    @Nullable
    private static String firstNeedleInFile(File file, String[] needles) {
        if (file == null || needles == null || !file.isFile()) return null;
        byte[][] encodedNeedles = encodeNeedles(needles);
        byte[] buffer = new byte[65536];
        byte[] carry = new byte[128];
        int carryLength = 0;
        long totalRead = 0;
        try (FileInputStream input = new FileInputStream(file)) {
            int read;
            while ((read = input.read(buffer)) != -1 && totalRead < 2 * 1024 * 1024L) {
                int scanLength = carryLength + read;
                byte[] scanBuffer = new byte[scanLength];
                System.arraycopy(carry, 0, scanBuffer, 0, carryLength);
                System.arraycopy(buffer, 0, scanBuffer, carryLength, read);
                for (int i = 0; i < encodedNeedles.length; i++) {
                    if (indexOf(scanBuffer, scanLength, encodedNeedles[i]) >= 0) {
                        return needles[i];
                    }
                }
                carryLength = Math.min(carry.length, scanLength);
                System.arraycopy(scanBuffer, scanLength - carryLength, carry, 0, carryLength);
                totalRead += read;
            }
        } catch (IOException ignored) {
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
