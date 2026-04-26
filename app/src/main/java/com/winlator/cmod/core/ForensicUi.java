package com.winlator.cmod.core;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.winlator.cmod.R;
import com.winlator.cmod.contentdialog.ContentDialog;
import com.winlator.cmod.widget.LogView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public final class ForensicUi {
    private ForensicUi() {}

    public static void renderWineDebugChannels(Fragment fragment, LinearLayout container, ArrayList<String> debugChannels, Runnable onChanged) {
        Context context = fragment.requireContext();
        container.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView = inflater.inflate(R.layout.wine_debug_channel_list_item, container, false);
        itemView.findViewById(R.id.TextView).setVisibility(View.GONE);
        itemView.findViewById(R.id.BTRemove).setVisibility(View.GONE);

        View addButton = itemView.findViewById(R.id.BTAdd);
        addButton.setVisibility(View.VISIBLE);
        addButton.setOnClickListener(v -> {
            JSONArray jsonArray = null;
            try {
                jsonArray = new JSONArray(FileUtils.readString(context, "wine_debug_channels.json"));
            }
            catch (Exception ignored) { /* best-effort path; keep surrounding flow intact. */ }

            final String[] items = ArrayUtils.toStringArray(jsonArray);
            ContentDialog.showMultipleChoiceList(context, R.string.wine_debug_channel, items, selectedPositions -> {
                for (int selectedPosition : selectedPositions) {
                    if (selectedPosition >= 0 && selectedPosition < items.length && !debugChannels.contains(items[selectedPosition])) {
                        debugChannels.add(items[selectedPosition]);
                    }
                }
                renderWineDebugChannels(fragment, container, debugChannels, onChanged);
                if (onChanged != null) onChanged.run();
            });
        });

        View resetButton = itemView.findViewById(R.id.BTReset);
        resetButton.setVisibility(View.VISIBLE);
        resetButton.setOnClickListener(v -> {
            debugChannels.clear();
            debugChannels.addAll(Arrays.asList(ForensicConfig.DEFAULT_WINE_DEBUG_CHANNELS.split(",")));
            renderWineDebugChannels(fragment, container, debugChannels, onChanged);
            if (onChanged != null) onChanged.run();
        });
        container.addView(itemView);

        for (int i = 0; i < debugChannels.size(); i++) {
            itemView = inflater.inflate(R.layout.wine_debug_channel_list_item, container, false);
            TextView textView = itemView.findViewById(R.id.TextView);
            textView.setText(debugChannels.get(i));
            final int index = i;
            itemView.findViewById(R.id.BTRemove).setOnClickListener(v -> {
                debugChannels.remove(index);
                renderWineDebugChannels(fragment, container, debugChannels, onChanged);
                if (onChanged != null) onChanged.run();
            });
            container.addView(itemView);
        }
    }

    public static void showForensicLogViewer(Context context, String ownerStage) {
        if (context == null) return;

        File latestFile = ForensicLogger.getLatestLogFile(context);
        String tail = normalizeForensicTail(context, ForensicLogger.readLatestTraceTail(context, 1200, 150000));
        ForensicLogger.logEvent(
                context,
                "info",
                "FORENSIC_LOG_VIEW_OPENED",
                null,
                ownerStage == null || ownerStage.trim().isEmpty() ? "runtime_drawer" : ownerStage.trim(),
                "forensic_log_view_opened",
                ForensicLogger.fields(
                        "log_file", latestFile != null ? latestFile.getAbsolutePath() : "",
                        "log_exists", latestFile != null && latestFile.isFile() ? "1" : "0",
                        "tail_chars", tail.length()
                )
        );

        ContentDialog dialog = new ContentDialog(context, R.layout.forensic_log_viewer_dialog);
        dialog.setTitle(R.string.diagnostics_forensic_log_title);
        dialog.setIcon(R.drawable.ae_icon_diagnostics);
        dialog.setBottomBarText(null);

        View message = dialog.findViewById(R.id.TVMessage);
        if (message != null) message.setVisibility(View.GONE);
        View confirm = dialog.findViewById(R.id.BTConfirm);
        View cancel = dialog.findViewById(R.id.BTCancel);
        if (confirm != null) confirm.setVisibility(View.GONE);
        if (cancel != null) cancel.setVisibility(View.GONE);
        View bottomBar = dialog.findViewById(R.id.LLBottomBar);
        if (bottomBar != null) bottomBar.setVisibility(View.GONE);

        TextView tvFile = dialog.findViewById(R.id.TVForensicLogFile);
        TextView tvStats = dialog.findViewById(R.id.TVForensicLogStats);
        TextView tvTicker = dialog.findViewById(R.id.TVForensicLogTicker);
        LogView logView = dialog.findViewById(R.id.LVForensicLogBody);

        Handler handler = new Handler(Looper.getMainLooper());
        final boolean[] autoRefresh = new boolean[] {true};
        final String[] currentTail = new String[] {tail};
        final File[] currentFile = new File[] {latestFile};

        Runnable applyTail = () -> {
            File activeFile = currentFile[0];
            String activeTail = currentTail[0];
            if (tvFile != null) {
                if (activeFile != null) {
                    tvFile.setVisibility(View.VISIBLE);
                    tvFile.setText(context.getString(R.string.diagnostics_forensic_log_file, activeFile.getName()));
                    tvFile.setSelected(true);
                } else {
                    tvFile.setVisibility(View.GONE);
                }
            }
            if (tvStats != null) {
                tvStats.setText(buildCompactLogViewerStats(activeFile, activeTail));
                tvStats.setSelected(true);
            }
            if (tvTicker != null) {
                tvTicker.setText(context.getString(
                        autoRefresh[0]
                                ? R.string.diagnostics_forensic_log_live
                                : R.string.diagnostics_forensic_log_paused,
                        DateFormat.format("HH:mm:ss", new Date())
                ));
                tvTicker.setSelected(true);
            }
            if (logView != null) {
                logView.replaceRawText(formatForensicTailForConsolePlain(context, activeTail));
            }
        };
        applyTail.run();

        View btExport = dialog.findViewById(R.id.BTForensicExportLog);
        if (btExport != null) {
            btExport.setOnClickListener(v -> exportForensicSnapshot(context, currentFile[0], currentTail[0]));
        }
        View btCopy = dialog.findViewById(R.id.BTForensicCopyLog);
        if (btCopy != null) {
            btCopy.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard == null) return;
                clipboard.setPrimaryClip(ClipData.newPlainText("forensic_log_tail", currentTail[0]));
                AppUtils.showToast(context, R.string.copied_to_clipboard);
            });
        }
        View btPause = dialog.findViewById(R.id.BTForensicPauseLog);
        if (btPause instanceof TextView) {
            ((TextView) btPause).setText(R.string.pause);
            btPause.setOnClickListener(v -> {
                autoRefresh[0] = !autoRefresh[0];
                ((TextView) btPause).setText(autoRefresh[0] ? R.string.pause : R.string.resume);
                applyTail.run();
            });
        }
        View btClose = dialog.findViewById(R.id.BTForensicCloseLog);
        if (btClose != null) {
            btClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
        styleForensicLogViewerDialog(context, dialog);
        Runnable refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (!dialog.isShowing()) return;
                if (autoRefresh[0]) {
                    currentFile[0] = ForensicLogger.getLatestLogFile(context);
                    currentTail[0] = normalizeForensicTail(context, ForensicLogger.readLatestTraceTail(context, 1200, 150000));
                    applyTail.run();
                } else if (tvTicker != null) {
                    tvTicker.setText(context.getString(R.string.diagnostics_forensic_log_paused, DateFormat.format("HH:mm:ss", new Date())));
                }
                handler.postDelayed(this, 1250L);
            }
        };
        handler.postDelayed(refreshRunnable, 1250L);
        dialog.setOnDismissListener(ignored -> handler.removeCallbacksAndMessages(null));

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            dialog.getWindow().setLayout(
                    Math.round(AppUtils.getScreenWidth() * 0.992f),
                    Math.round(AppUtils.getScreenHeight() * 0.986f)
            );
        }
        ViewGroup.LayoutParams contentParams = dialog.getContentView().getLayoutParams();
        if (contentParams != null) {
            contentParams.height = Math.round(AppUtils.getScreenHeight() * 0.976f);
            dialog.getContentView().setLayoutParams(contentParams);
        }
        dialog.getContentView().setMinimumHeight(Math.round(AppUtils.getScreenHeight() * 0.976f));
    }

    private static String normalizeForensicTail(Context context, String tail) {
        if (tail == null || tail.trim().isEmpty()) {
            return context.getString(R.string.diagnostics_forensic_log_empty);
        }
        return tail;
    }

    private static String buildCompactLogViewerStats(File latestFile, String tail) {
        int lineCount = tail == null || tail.isEmpty() ? 0 : tail.split("\n").length;
        long fileSize = latestFile != null && latestFile.isFile() ? latestFile.length() : 0L;
        int infoCount = 0;
        int warnCount = 0;
        int errorCount = 0;
        if (tail != null && !tail.isEmpty()) {
            String[] lines = tail.split("\n");
            for (String line : lines) {
                if (line == null || line.trim().isEmpty()) continue;
                try {
                    JSONObject obj = new JSONObject(line);
                    String severity = resolveForensicSeverity(obj);
                    if ("info".equals(severity)) infoCount++;
                    else if ("warn".equals(severity) || "warning".equals(severity)) warnCount++;
                    else if ("error".equals(severity)) errorCount++;
                } catch (Exception ignored) {
                }
            }
        }
        return "file " + (latestFile != null ? latestFile.getName() : "-")
                + "  |  size " + fileSize + " B"
                + "  |  lines " + lineCount
                + "  |  info " + infoCount
                + "  |  warn " + warnCount
                + "  |  err " + errorCount;
    }

    private static String formatForensicTailForConsolePlain(Context context, String tail) {
        if (tail == null || tail.trim().isEmpty()) {
            return context.getString(R.string.diagnostics_forensic_log_empty);
        }
        StringBuilder builder = new StringBuilder();
        String[] lines = tail.split("\n");
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) continue;
            if (builder.length() > 0) builder.append('\n');
            builder.append(formatForensicConsoleLinePlain(line));
        }
        return builder.length() > 0 ? builder.toString() : context.getString(R.string.diagnostics_forensic_log_empty);
    }

    private static String formatForensicConsoleLinePlain(String rawLine) {
        try {
            JSONObject obj = new JSONObject(rawLine);
            String timestamp = compactForensicTimestamp(obj.optString("ts", ""));
            String severity = padRight(resolveForensicSeverity(obj).toUpperCase(Locale.US), 5);
            String stage = clipForensicValue(obj.optString("stage", "-"), 16);
            String eventId = clipForensicValue(obj.optString("event_id", "-"), 34);
            String message = clipForensicValue(obj.optString("message", ""), 88);
            StringBuilder builder = new StringBuilder();
            builder.append(timestamp)
                    .append("  ")
                    .append(severity)
                    .append("  ")
                    .append(stage)
                    .append("  ")
                    .append(eventId);
            if (!message.isEmpty()) builder.append("  ").append(message);
            String extras = buildForensicExtraSummary(obj);
            if (!extras.isEmpty()) builder.append("\n             ").append(extras);
            return builder.toString();
        } catch (Exception ignored) {
            return rawLine;
        }
    }

    private static String buildForensicExtraSummary(JSONObject obj) {
        JSONArray names = obj.names();
        if (names == null || names.length() == 0) return "";
        ArrayList<String> parts = new ArrayList<>();
        for (int i = 0; i < names.length(); i++) {
            String key = names.optString(i, "");
            if (key.isEmpty()
                    || "ts".equals(key)
                    || "event_id".equals(key)
                    || "severity".equals(key)
                    || "level".equals(key)
                    || "trace_id".equals(key)
                    || "stage".equals(key)
                    || "message".equals(key)) {
                continue;
            }
            Object value = obj.opt(key);
            if (value == null) continue;
            String stringValue = clipForensicValue(String.valueOf(value), 44);
            if (stringValue.isEmpty()) continue;
            parts.add(key + "=" + stringValue);
            if (parts.size() >= 4) break;
        }
        if (parts.isEmpty()) return "";
        return String.join("  ", parts);
    }

    private static String resolveForensicSeverity(JSONObject obj) {
        String severity = obj.optString("severity", "").trim().toLowerCase(Locale.US);
        if (!severity.isEmpty()) return severity;
        return obj.optString("level", "").trim().toLowerCase(Locale.US);
    }

    private static String compactForensicTimestamp(String timestamp) {
        if (timestamp == null) return "--:--:--.---";
        String normalized = timestamp.trim();
        if (normalized.length() >= 23 && normalized.charAt(10) == 'T') {
            return normalized.substring(11, 23);
        }
        return clipForensicValue(normalized, 12);
    }

    private static String clipForensicValue(String value, int maxLength) {
        if (value == null) return "";
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        if (normalized.length() <= maxLength) return normalized;
        if (maxLength <= 3) return normalized.substring(0, Math.max(0, maxLength));
        return normalized.substring(0, maxLength - 3) + "...";
    }

    private static String padRight(String value, int width) {
        String normalized = value != null ? value : "";
        if (normalized.length() >= width) return normalized.substring(0, width);
        StringBuilder builder = new StringBuilder(normalized);
        while (builder.length() < width) builder.append(' ');
        return builder.toString();
    }

    private static void exportForensicSnapshot(Context context, File latestFile, String tail) {
        String ts = DateFormat.format("yyyy-MM-dd_HH-mm-ss", new Date()).toString();
        File outFile = ForensicLogger.createExportFile(context, String.format(Locale.US, "forensics_%s.jsonl", ts));
        String exportBody = ForensicLogger.buildExportBody(context, latestFile, tail);
        if (!FileUtils.writeString(outFile, exportBody)) {
            AppUtils.showToast(context, R.string.diagnostics_forensic_log_export_fail);
            return;
        }
        AppUtils.showToast(context, context.getString(R.string.diagnostics_forensic_log_export_ok, outFile.getAbsolutePath()));
        ForensicLogger.logEvent(
                context,
                "info",
                "FORENSIC_LOG_EXPORTED",
                null,
                "forensic_log_viewer",
                "forensic_log_exported",
                ForensicLogger.fields(
                        "source_file", latestFile != null ? latestFile.getAbsolutePath() : "",
                        "source_file_size", latestFile != null && latestFile.isFile() ? latestFile.length() : 0L,
                        "export_file", outFile.getAbsolutePath(),
                        "export_chars", exportBody.length(),
                        "tail_chars", tail != null ? tail.length() : 0
                )
        );
    }

    private static void styleForensicLogViewerDialog(Context context, ContentDialog dialog) {
        if (context == null || dialog == null) return;
        int horizontalPadding = Math.round(context.getResources().getDisplayMetrics().density * 3f);
        int topPadding = Math.round(context.getResources().getDisplayMetrics().density * 3f);
        View root = dialog.getContentView();
        if (root != null) {
            root.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
            root.setPadding(horizontalPadding, topPadding, horizontalPadding, topPadding);
        }
        View frameLayout = dialog.findViewById(R.id.FrameLayout);
        if (frameLayout != null) frameLayout.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
        View titleBar = dialog.findViewById(R.id.LLTitleBar);
        if (titleBar != null) {
            titleBar.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
            titleBar.setPadding(horizontalPadding, topPadding, horizontalPadding, 0);
        }
        TextView titleView = dialog.findViewById(R.id.TVTitle);
        if (titleView != null) titleView.setTextColor(ContextCompat.getColor(context, R.color.surface_runtime_taskmgr_text));
        android.widget.ImageView iconView = dialog.findViewById(R.id.IVIcon);
        if (iconView != null) iconView.setColorFilter(ContextCompat.getColor(context, R.color.surface_runtime_taskmgr_text));
        restyleForensicActionButton(context, dialog.findViewById(R.id.BTForensicCopyLog));
        restyleForensicActionButton(context, dialog.findViewById(R.id.BTForensicExportLog));
        restyleForensicActionButton(context, dialog.findViewById(R.id.BTForensicPauseLog));
        restyleForensicActionButton(context, dialog.findViewById(R.id.BTForensicCloseLog));
    }

    private static void restyleForensicActionButton(Context context, View buttonView) {
        if (!(buttonView instanceof TextView)) return;
        buttonView.setBackgroundResource(R.drawable.surface_runtime_button_neutral);
        ((TextView) buttonView).setTextColor(ContextCompat.getColor(context, R.color.surface_runtime_button_text));
    }
}
