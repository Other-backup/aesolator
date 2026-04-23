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
    private final String sinkId;

    public FileDebugLogger(Context context, String prefix, String... filters) {
        Context appContext = context != null ? context.getApplicationContext() : null;
        String safePrefix = safePrefix(prefix);
        WinlatorLogUtils.LogFileTarget target = WinlatorLogUtils.createTimestampedLogTarget(appContext, safePrefix);
        this.logFile = target.file;
        this.sinkId = target.sinkId;
        this.filters = filters != null ? filters : new String[0];
        appendHeader(safePrefix);
        emitReadyEvent(appContext, safePrefix, target);
    }

    @Override
    public void call(String line) {
        if (!matches(line)) return;
        appendLine(line);
    }

    private void appendHeader(String prefix) {
        appendLine("# debug stream: " + safePrefix(prefix));
        appendLine("# sink: " + sinkId);
    }

    private void emitReadyEvent(Context context, String prefix, WinlatorLogUtils.LogFileTarget target) {
        if (context == null) return;
        String filterSummary = filters.length == 0 ? "*" : String.join(",", filters);
        ForensicLogger.logEvent(
                context,
                target.fallbackUsed ? "warn" : "info",
                target.fallbackUsed ? "RUNTIME_LOG_SINK_SWITCH" : "RUNTIME_LOG_FILE_READY",
                null,
                "runtime_log_sink",
                target.fallbackUsed ? "runtime_log_sink_switch" : "runtime_log_file_ready",
                ForensicLogger.fields(
                        "prefix", prefix,
                        "sink_id", target.sinkId,
                        "path", logFile.getAbsolutePath(),
                        "filters", filterSummary,
                        "fallback_used", target.fallbackUsed ? "1" : "0",
                        "fallback_reason", target.fallbackReason != null ? String.valueOf(target.fallbackReason.getMessage()) : ""
                )
        );
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
