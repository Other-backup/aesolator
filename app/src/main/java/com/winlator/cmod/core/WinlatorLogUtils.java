package com.winlator.cmod.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.format.DateFormat;

import androidx.preference.PreferenceManager;

import com.winlator.cmod.SettingsFragment;

import java.io.File;
import java.util.Date;

public abstract class WinlatorLogUtils {
    public static File getLogsDir(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String winlatorPath = sp.getString("winlator_path_uri", null);
        File logsDir;

        if (winlatorPath != null) {
            Uri winlatorUri = Uri.parse(winlatorPath);
            logsDir = new File(FileUtils.getFilePathFromUri(context, winlatorUri), "logs");
        } else {
            logsDir = new File(SettingsFragment.getResolvedDefaultStoragePath(), "logs");
        }

        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        return logsDir;
    }

    public static File createTimestampedLogFile(Context context, String prefix) {
        String safePrefix = (prefix == null || prefix.trim().isEmpty()) ? "runtime" : prefix.trim();
        safePrefix = safePrefix.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
        String fileName = safePrefix + "_" + DateFormat.format("yyyy-MM-dd_HH-mm-ss", new Date()) + ".txt";
        return new File(getLogsDir(context), fileName);
    }
}
