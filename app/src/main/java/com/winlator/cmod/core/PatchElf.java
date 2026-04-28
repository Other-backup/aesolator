package com.winlator.cmod.core;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.Locale;

public class PatchElf {
    static {
        NativeLibraryLoader.ensureLoaded("patchelf", "PatchElf");
    }

    private long elfInstancePtr = 0;
    private File elfFile = null;

    public boolean loadElf(File file) {
        if (elfInstancePtr != 0) {
            logPatchElfEvent("PATCHELF_LOAD_REJECTED", "already_loaded", file);
            return false;
        }
        if (file == null || !file.exists() || file.isDirectory()) {
            logPatchElfEvent("PATCHELF_LOAD_REJECTED", "invalid_input", file);
            return false;
        }
        long nativePtr = createElfObject(file.getAbsolutePath());
        if (nativePtr != 0) {
            elfInstancePtr = nativePtr;
            elfFile = file;
            return true;
        }
        logPatchElfEvent("PATCHELF_LOAD_REJECTED", "bridge_unavailable_or_load_failed", file);
        return false;
    }

    public boolean loadElf(@NonNull String path) {
        return loadElf(new File(path));
    }

    public void unloadElf() {
        if (elfInstancePtr != 0) {
            destroyElfObject(elfInstancePtr);
        }
        elfInstancePtr = 0;
        elfFile = null;
    }

    public boolean saveElf(@NonNull File file) {
        if (elfInstancePtr == 0 || elfFile == null || file == null) return false;
        if (!elfFile.equals(file)) {
            logPatchElfEvent("PATCHELF_SAVE_REJECTED", "cross_file_save_unsupported", file);
            return false;
        }
        if (!isChanged(elfInstancePtr)) return true;
        boolean saved = rewriteElfObject(elfInstancePtr);
        if (!saved) logPatchElfEvent("PATCHELF_SAVE_REJECTED", "native_rewrite_failed", file);
        return saved;
    }

    public boolean saveElf() {
        if (elfFile == null) return false;
        return saveElf(elfFile);
    }

    public String getInterpreter() {
        if (elfInstancePtr == 0) return "";
        String interpreter = getInterpreter(elfInstancePtr);
        return interpreter != null ? interpreter : "";
    }

    public boolean setInterpreter(@NonNull String interpreter) {
        return elfInstancePtr != 0 && setInterpreter(elfInstancePtr, interpreter);
    }

    private native long createElfObject(String path);
    private native boolean destroyElfObject(long objectPtr);
    private native boolean isChanged(long objectPtr);
    private native boolean rewriteElfObject(long objectPtr);
    private native String getInterpreter(long objectPtr);
    private native boolean setInterpreter(long objectPtr, String interpreter);
    private native String getOsAbi(long objectPtr);
    private native boolean replaceOsAbi(long objectPtr, String osAbi);
    private native String getSoName(long objectPtr);
    private native boolean replaceSoName(long objectPtr, String soName);
    private native String[] getRPath(long objectPtr);
    private native boolean addRPath(long objectPtr, String rpath);
    private native boolean removeRPath(long objectPtr, String rpath);
    private native String[] getNeeded(long objectPtr);
    private native boolean addNeeded(long objectPtr, String needed);
    private native boolean removeNeeded(long objectPtr, String needed);

    private void logPatchElfEvent(String eventId, String reason, File file) {
        ForensicLogger.logEvent(
                ForensicLogger.getAppContext(),
                "warn",
                eventId,
                null,
                "patchelf",
                reason,
                ForensicLogger.fields(
                        "reason", reason,
                        "path", file != null ? file.getAbsolutePath() : "-",
                        "exists", file != null && file.exists(),
                        "is_directory", file != null && file.isDirectory(),
                        "loaded_path", elfFile != null ? elfFile.getAbsolutePath() : "-",
                        "instance_active", elfInstancePtr != 0,
                        "file_name", file != null ? file.getName().toLowerCase(Locale.US) : "-"
                )
        );
    }
}
