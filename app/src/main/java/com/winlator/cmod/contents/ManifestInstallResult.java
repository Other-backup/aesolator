package com.winlator.cmod.contents;

public final class ManifestInstallResult {
    public final boolean success;
    public final String message;

    public ManifestInstallResult(boolean success, String message) {
        this.success = success;
        this.message = message == null ? "" : message;
    }
}
