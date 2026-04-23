package com.winlator.cmod.contentdialog;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Environment;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.winlator.cmod.R;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.Callback;
import com.winlator.cmod.widget.LogView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class DebugDialog extends ContentDialog implements Callback<String> {
    private final LogView logView;
    private static boolean paused = false;
    private final File logFile;
    private final TextView fileView;
    private final TextView statsView;
    private final TextView tickerView;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Object pendingLock = new Object();
    private final ArrayList<String> pendingUiLines = new ArrayList<>();
    private BufferedWriter writer;
    private boolean flushPosted = false;
    private final Runnable flushRunnable = new Runnable() {
        @Override
        public void run() {
            ArrayList<String> batch;
            synchronized (pendingLock) {
                flushPosted = false;
                if (pendingUiLines.isEmpty()) {
                    refreshHeader(false);
                    return;
                }
                batch = new ArrayList<>(pendingUiLines);
                pendingUiLines.clear();
            }
            if (!getPaused()) {
                logView.appendBatch(batch);
            }
            refreshHeader(true);
            synchronized (pendingLock) {
                if (!pendingUiLines.isEmpty() && !flushPosted) {
                    flushPosted = true;
                    mainHandler.postDelayed(this, 48L);
                }
            }
        }
    };

    public DebugDialog(@NonNull Context context) {
        super(context, R.layout.debug_dialog);
        setIcon(R.drawable.ae_icon_diagnostics);
        setTitle(context.getString(R.string.logs));
        logView = findViewById(R.id.LogView);
        fileView = findViewById(R.id.TVDebugLogFile);
        statsView = findViewById(R.id.TVDebugLogStats);
        tickerView = findViewById(R.id.TVDebugLogTicker);
        if (fileView != null) fileView.setSelected(true);
        if (statsView != null) statsView.setSelected(true);
        if (tickerView != null) tickerView.setSelected(true);

        getContentView().setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
        View frameLayout = findViewById(R.id.FrameLayout);
        if (frameLayout != null) frameLayout.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
        View titleBar = findViewById(R.id.LLTitleBar);
        if (titleBar != null) titleBar.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
        TextView titleView = findViewById(R.id.TVTitle);
        if (titleView != null) titleView.setTextColor(ContextCompat.getColor(context, R.color.surface_runtime_taskmgr_text));
        TextView messageView = findViewById(R.id.TVMessage);
        if (messageView != null) messageView.setTextColor(ContextCompat.getColor(context, R.color.surface_runtime_taskmgr_muted));
        ImageView iconView = findViewById(R.id.IVIcon);
        if (iconView != null) iconView.setColorFilter(ContextCompat.getColor(context, R.color.surface_runtime_taskmgr_text));
        View titleBackButton = findViewById(R.id.BTTitleBack);
        if (titleBackButton instanceof android.widget.ImageButton) {
            ((android.widget.ImageButton) titleBackButton).setColorFilter(ContextCompat.getColor(context, R.color.surface_runtime_taskmgr_text));
            titleBackButton.setBackgroundResource(R.drawable.surface_runtime_button_neutral);
        }

        findViewById(R.id.BTCancel).setVisibility(View.GONE);
        Button confirmButton = findViewById(R.id.BTConfirm);
        if (confirmButton != null) {
            confirmButton.setVisibility(View.GONE);
        }

        Button pauseButton = findViewById(R.id.BTPause);
        Button copyButton = findViewById(R.id.BTDebugCopy);
        Button exportButton = findViewById(R.id.BTDebugExport);
        if (copyButton != null) {
            copyButton.setBackgroundResource(R.drawable.surface_runtime_button_neutral);
            copyButton.setTextColor(ContextCompat.getColor(context, R.color.surface_runtime_button_text));
            copyButton.setOnClickListener(v -> copyRuntimeLogToClipboard());
        }
        if (exportButton != null) {
            exportButton.setBackgroundResource(R.drawable.surface_runtime_button_neutral);
            exportButton.setTextColor(ContextCompat.getColor(context, R.color.surface_runtime_button_text));
            exportButton.setOnClickListener(v -> exportRuntimeLog());
        }
        if (pauseButton != null) {
            pauseButton.setBackgroundResource(R.drawable.surface_runtime_button_neutral);
            pauseButton.setTextColor(ContextCompat.getColor(context, R.color.surface_runtime_button_text));
            pauseButton.setOnClickListener((v) -> {
                setPaused(!paused);
                ((Button) v).setText(getPaused() ? R.string.resume : R.string.pause);
                refreshHeader(false);
            });
        }
        Button closeButton = findViewById(R.id.BTCloseLog);
        if (closeButton != null) {
            closeButton.setBackgroundResource(R.drawable.surface_runtime_button_neutral);
            closeButton.setTextColor(ContextCompat.getColor(context, R.color.surface_runtime_button_text));
            closeButton.setOnClickListener(v -> dismiss());
        }
        View bottomBar = findViewById(R.id.LLBottomBar);
        if (bottomBar != null) {
            bottomBar.setVisibility(View.GONE);
        }
        logFile = logView.getLogFile(context);
        try {
            writer = new BufferedWriter(new FileWriter(logFile));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (pauseButton != null) {
            pauseButton.setText(getPaused() ? R.string.resume : R.string.pause);
        }
        refreshHeader(false);
    }

    @Override
    public void show() {
        super.show();
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(0));
            getWindow().setLayout(
                    Math.round(AppUtils.getScreenWidth() * 0.992f),
                    Math.round(AppUtils.getScreenHeight() * 0.986f)
            );
        }
        ViewGroup.LayoutParams params = getContentView().getLayoutParams();
        if (params != null) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = Math.round(AppUtils.getScreenHeight() * 0.976f);
            getContentView().setLayoutParams(params);
        }
        getContentView().setMinimumHeight(Math.round(AppUtils.getScreenHeight() * 0.976f));
        int horizontalPadding = Math.round(getContext().getResources().getDisplayMetrics().density * 3f);
        int topPadding = Math.round(getContext().getResources().getDisplayMetrics().density * 3f);
        getContentView().setPadding(horizontalPadding, topPadding, horizontalPadding, topPadding);
        View titleBar = findViewById(R.id.LLTitleBar);
        if (titleBar != null) {
            titleBar.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
            titleBar.setPadding(horizontalPadding, topPadding, horizontalPadding, 0);
        }
    }

    @Override
    public void call(final String line) {
        if (writer != null) {
            try {
                writer.write(line + "\n");
                writer.flush();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        synchronized (pendingLock) {
            pendingUiLines.add(line);
            if (!flushPosted) {
                flushPosted = true;
                mainHandler.post(flushRunnable);
            }
        }
    }

    public static void setPaused(boolean cond) {
        paused = cond;
    }

    public static boolean getPaused() {
        return paused;
    }

    private void refreshHeader(boolean liveTick) {
        String name = logFile != null ? logFile.getName() : "";
        if (fileView != null) {
            fileView.setText(getContext().getString(R.string.diagnostics_forensic_log_file, name));
        }
        if (statsView != null) {
            long sizeBytes = logFile != null && logFile.isFile() ? logFile.length() : 0L;
            statsView.setText(String.format(
                    Locale.US,
                    "lines=%d  •  size=%.1f KB  •  state=%s",
                    logView.getLineCount(),
                    sizeBytes / 1024f,
                    getPaused() ? "paused" : "live"
            ));
        }
        if (tickerView != null) {
            CharSequence stamp = DateFormat.format("HH:mm:ss", new Date());
            tickerView.setText(getContext().getString(
                    getPaused()
                            ? R.string.diagnostics_forensic_log_paused
                            : R.string.diagnostics_forensic_log_live,
                    stamp
            ));
            tickerView.setSelected(true);
        }
    }

    private void copyRuntimeLogToClipboard() {
        ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager == null) return;
        String text = "";
        if (logFile != null && logFile.isFile()) {
            String fileBody = com.winlator.cmod.core.FileUtils.readString(logFile);
            if (fileBody != null) text = fileBody;
        }
        if (text.length() > 150000) {
            text = text.substring(text.length() - 150000);
        }
        clipboardManager.setPrimaryClip(ClipData.newPlainText("Ae.solator runtime log", text));
        AppUtils.showToast(getContext(), R.string.copied_to_clipboard);
    }

    private void exportRuntimeLog() {
        if (logFile == null || !logFile.isFile()) return;
        File exportDir = new File(Environment.getExternalStorageDirectory(), "Ae.solator/logs/exports");
        if (!exportDir.exists() && !exportDir.mkdirs()) {
            AppUtils.showToast(getContext(), R.string.diagnostics_forensic_log_export_fail);
            return;
        }
        String exportName = String.format(Locale.US, "runtime_log_%d.txt", System.currentTimeMillis());
        File exportFile = new File(exportDir, exportName);
        if (!com.winlator.cmod.core.FileUtils.copy(logFile, exportFile)) {
            AppUtils.showToast(getContext(), R.string.diagnostics_forensic_log_export_fail);
            return;
        }
        AppUtils.showToast(getContext(), getContext().getString(R.string.diagnostics_forensic_log_export_ok, exportFile.getAbsolutePath()));
    }

    @Override
    public void dismiss() {
        mainHandler.removeCallbacksAndMessages(null);
        synchronized (pendingLock) {
            pendingUiLines.clear();
            flushPosted = false;
        }
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
            }
            catch (IOException ignored) {
            }
            writer = null;
        }
        super.dismiss();
    }
}
