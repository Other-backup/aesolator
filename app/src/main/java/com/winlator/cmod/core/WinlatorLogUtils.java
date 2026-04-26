package com.winlator.cmod.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.winlator.cmod.SettingsFragment;

import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

public abstract class WinlatorLogUtils {
    private static final String TAG = "WinlatorLogUtils";
    public static final String SINK_EXTERNAL = "external";
    public static final String SINK_APP_EXTERNAL = "app_external";
    public static final String SINK_APP_PRIVATE = "app_private";

    public static final class LogFileTarget {
        public final File file;
        public final String sinkId;
        public final boolean fallbackUsed;
        public final IOException fallbackReason;

        public LogFileTarget(File file, String sinkId, boolean fallbackUsed, IOException fallbackReason) {
            this.file = file;
            this.sinkId = sinkId;
            this.fallbackUsed = fallbackUsed;
            this.fallbackReason = fallbackReason;
        }
    }

    public static File getLogsDir(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String winlatorPath = sp.getString("winlator_path_uri", null);
        File logsDir;

        if (winlatorPath != null) {
            Uri winlatorUri = Uri.parse(winlatorPath);
            logsDir = new File(FileUtils.getFilePathFromUri(context, winlatorUri), "logs");
        } else {
            logsDir = new File(SettingsFragment.DEFAULT_WINLATOR_PATH, "logs");
        }

        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        return logsDir;
    }

    public static File getAppPrivateLogsDir(Context context) {
        Context appContext = context != null ? context.getApplicationContext() : null;
        if (appContext == null) return new File(SettingsFragment.DEFAULT_WINLATOR_PATH, "logs");
        File logsDir = new File(appContext.getFilesDir(), "Winlator/logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        return logsDir;
    }

    public static File getAppExternalLogsDir(Context context) {
        Context appContext = context != null ? context.getApplicationContext() : null;
        if (appContext == null) return new File(SettingsFragment.DEFAULT_WINLATOR_PATH, "logs");
        File externalFilesDir = appContext.getExternalFilesDir(null);
        File logsDir = externalFilesDir != null
                ? new File(externalFilesDir, "Winlator/logs")
                : new File(appContext.getFilesDir(), "Winlator/logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        return logsDir;
    }

    public static List<File> getCandidateLogsDirs(Context context) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        ArrayList<File> out = new ArrayList<>();
        addCandidateLogDir(out, seen, getAppExternalLogsDir(context));
        addCandidateLogDir(out, seen, getLogsDir(context));
        addCandidateLogDir(out, seen, getAppPrivateLogsDir(context));
        return out;
    }

    private static void addCandidateLogDir(ArrayList<File> out, LinkedHashSet<String> seen, File dir) {
        if (dir == null) return;
        String path = dir.getAbsolutePath();
        if (!seen.add(path)) return;
        if (!dir.exists()) {
            dir.mkdirs();
        }
        out.add(dir);
    }

    public static LogFileTarget createTimestampedLogTarget(Context context, String prefix) {
        String fileName = buildLogFileName(prefix);
        File appExternalFile = new File(getAppExternalLogsDir(context), fileName);
        IOException appExternalError = ensureWritableFile(appExternalFile);
        if (appExternalError == null) {
            return new LogFileTarget(appExternalFile, SINK_APP_EXTERNAL, false, null);
        }

        File externalFile = new File(getLogsDir(context), fileName);
        IOException externalError = ensureWritableFile(externalFile);
        if (externalError == null) {
            return new LogFileTarget(externalFile, SINK_EXTERNAL, true, appExternalError);
        }

        File appPrivateFile = new File(getAppPrivateLogsDir(context), fileName);
        IOException appPrivateError = ensureWritableFile(appPrivateFile);
        if (appPrivateError == null) {
            Log.w(TAG, "External logs dir unavailable, using app-private logs dir", externalError);
            return new LogFileTarget(appPrivateFile, SINK_APP_PRIVATE, true, externalError);
        }

        Log.e(TAG, "Unable to prepare any writable runtime log sink", appPrivateError);
        return new LogFileTarget(externalFile, SINK_EXTERNAL, false, externalError);
    }

    public static File createTimestampedLogFile(Context context, String prefix) {
        return createTimestampedLogTarget(context, prefix).file;
    }

    private static String buildLogFileName(String prefix) {
        String safePrefix = (prefix == null || prefix.trim().isEmpty()) ? "runtime" : prefix.trim();
        safePrefix = safePrefix.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
        return safePrefix + "_" + DateFormat.format("yyyy-MM-dd_HH-mm-ss", new Date()) + ".txt";
    }

    private static IOException ensureWritableFile(File file) {
        if (file == null) return new IOException("Log file target is null");
        File dir = file.getParentFile();
        try {
            if (dir == null || (!dir.exists() && !dir.mkdirs() && !dir.exists())) {
                return new IOException("Unable to create log dir: " + (dir == null ? "<null>" : dir.getAbsolutePath()));
            }
            try (FileWriter ignored = new FileWriter(file, true)) {
                // Probe writability and create the file up front for later tailers.
            }
            return null;
        } catch (IOException e) {
            return e;
        }
    }
}
