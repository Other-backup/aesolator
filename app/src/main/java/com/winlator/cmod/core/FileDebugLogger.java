package com.winlator.cmod.core;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

public final class FileDebugLogger implements Callback<String> {
    private static final String TAG = "FileDebugLogger";

    private final File logFile;
    private final String[] filters;

    public FileDebugLogger(Context context, String prefix, String... filters) {
        Context appContext = context != null ? context.getApplicationContext() : null;
        this.logFile = WinlatorLogUtils.createTimestampedLogFile(appContext, safePrefix(prefix));
        this.filters = filters != null ? filters : new String[0];
        appendHeader(prefix);
    }

    @Override
    public void call(String line) {
        if (!matches(line)) return;
        appendLine(line);
    }

    private void appendHeader(String prefix) {
        appendLine("# debug stream: " + safePrefix(prefix));
    }

    private boolean matches(String line) {
        if (filters.length == 0) return true;
        String text = line == null ? "" : line.toLowerCase(Locale.ROOT);
        for (String filter : filters) {
            if (filter == null || filter.trim().isEmpty()) continue;
            if (text.contains(filter.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private void appendLine(String line) {
        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write((line == null ? "" : line));
            writer.write('\n');
        }
        catch (IOException e) {
            Log.e(TAG, "Failed to write debug log line", e);
        }
    }

    private static String safePrefix(String value) {
        if (value == null || value.trim().isEmpty()) return "runtime";
        return value.trim();
    }
}
