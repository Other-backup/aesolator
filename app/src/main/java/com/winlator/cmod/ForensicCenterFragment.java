package com.winlator.cmod;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.ForensicConfig;
import com.winlator.cmod.core.ForensicIssueComposer;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.core.PreloaderDialog;
import com.winlator.cmod.core.UiLifecycleGuard;
import com.winlator.cmod.contentdialog.ContentDialog;
import com.winlator.cmod.widget.LogView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class ForensicCenterFragment extends Fragment {
    private static final String TAG = "ForensicCenterFragment";
    private SharedPreferences preferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.forensic_center_fragment, container, false);
        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        boolean isDarkMode = preferences.getBoolean("dark_mode", false);

        CheckBox cbWineDebug = view.findViewById(R.id.CBForensicWineDebug);
        CheckBox cbBox64Logs = view.findViewById(R.id.CBForensicBox64Logs);
        CheckBox cbLoaderTrace = view.findViewById(R.id.CBForensicLoaderTrace);
        CheckBox cbFexLogs = view.findViewById(R.id.CBForensicFexLogs);
        CheckBox cbTurnipLogs = view.findViewById(R.id.CBForensicTurnipLogs);
        CheckBox cbDxvkLogs = view.findViewById(R.id.CBForensicDxvkLogs);
        CheckBox cbVkd3dLogs = view.findViewById(R.id.CBForensicVkd3dLogs);
        CheckBox cbDgVoodooLogs = view.findViewById(R.id.CBForensicDgVoodooLogs);
        CheckBox cbVulkanApiDump = view.findViewById(R.id.CBForensicVulkanApiDump);
        CheckBox cbVulkanLoaderDebug = view.findViewById(R.id.CBForensicVulkanLoaderDebug);
        CheckBox cbVulkanValidation = view.findViewById(R.id.CBForensicVulkanValidation);
        CheckBox cbPulseLogs = view.findViewById(R.id.CBForensicPulseLogs);
        CheckBox cbAlsaLogs = view.findViewById(R.id.CBForensicAlsaLogs);
        CheckBox cbDeviceSnapshot = view.findViewById(R.id.CBForensicDeviceSnapshot);
        CheckBox cbNonRootCapture = view.findViewById(R.id.CBForensicNonRootCapture);
        CheckBox cbRootCapture = view.findViewById(R.id.CBForensicRootCapture);
        CheckBox cbShizukuCapture = view.findViewById(R.id.CBForensicShizukuCapture);
        TextView badgeFreeWine = view.findViewById(R.id.TVPolicyBadgeFreeWine);
        TextView badgeDxvk = view.findViewById(R.id.TVPolicyBadgeDxvk);
        TextView badgeGraphics = view.findViewById(R.id.TVPolicyBadgeGraphics);
        TextView badgeDgVoodoo = view.findViewById(R.id.TVPolicyBadgeDgVoodoo);
        TextView adbCommand = view.findViewById(R.id.TVAdbCommand);
        TextView adbCaptureCommand = view.findViewById(R.id.TVAdbCaptureCommand);

        int panelBackground = isDarkMode
                ? R.drawable.forensic_panel_background_dark
                : R.drawable.forensic_panel_background;
        int badgeBackground = isDarkMode
                ? R.drawable.forensic_badge_background_dark
                : R.drawable.forensic_badge_background;
        int commandBackground = isDarkMode
                ? R.drawable.forensic_command_background_dark
                : R.drawable.forensic_command_background;
        int badgeTextColor = ContextCompat.getColor(
                requireContext(),
                isDarkMode ? R.color.forensic_badge_text_dark : R.color.forensic_badge_text
        );

        view.findViewById(R.id.LLForensicPolicyCard).setBackgroundResource(panelBackground);
        view.findViewById(R.id.LLForensicTogglesCard).setBackgroundResource(panelBackground);
        view.findViewById(R.id.LLForensicAdbCard).setBackgroundResource(panelBackground);
        adbCommand.setBackgroundResource(commandBackground);
        adbCaptureCommand.setBackgroundResource(commandBackground);
        badgeFreeWine.setBackgroundResource(badgeBackground);
        badgeDxvk.setBackgroundResource(badgeBackground);
        badgeGraphics.setBackgroundResource(badgeBackground);
        badgeDgVoodoo.setBackgroundResource(badgeBackground);
        badgeFreeWine.setTextColor(badgeTextColor);
        badgeDxvk.setTextColor(badgeTextColor);
        badgeGraphics.setTextColor(badgeTextColor);
        badgeDgVoodoo.setTextColor(badgeTextColor);

        cbWineDebug.setChecked(preferences.getBoolean("enable_wine_debug", false));
        cbBox64Logs.setChecked(preferences.getBoolean("enable_box64_logs", false));
        cbLoaderTrace.setChecked(preferences.getBoolean(ForensicConfig.PREF_ENABLE_LOADER_TRACE, false));
        cbFexLogs.setChecked(preferences.getBoolean(ForensicConfig.PREF_ENABLE_FEX_LOGS, false));
        cbTurnipLogs.setChecked(preferences.getBoolean(ForensicConfig.PREF_ENABLE_TURNIP_LOGS, false));
        cbDxvkLogs.setChecked(preferences.getBoolean(ForensicConfig.PREF_ENABLE_DXVK_LOGS, false));
        cbVkd3dLogs.setChecked(preferences.getBoolean(ForensicConfig.PREF_ENABLE_VKD3D_LOGS, false));
        cbDgVoodooLogs.setChecked(preferences.getBoolean(ForensicConfig.PREF_ENABLE_DGVOODOO_LOGS, false));
        cbVulkanApiDump.setChecked(preferences.getBoolean(ForensicConfig.PREF_ENABLE_VULKAN_API_DUMP, false));
        cbVulkanLoaderDebug.setChecked(preferences.getBoolean(ForensicConfig.PREF_ENABLE_VULKAN_LOADER_DEBUG, false));
        cbVulkanValidation.setChecked(preferences.getBoolean(ForensicConfig.PREF_ENABLE_VULKAN_VALIDATION, false));
        cbPulseLogs.setChecked(preferences.getBoolean(ForensicConfig.PREF_ENABLE_PULSE_LOGS, false));
        cbAlsaLogs.setChecked(preferences.getBoolean(ForensicConfig.PREF_ENABLE_ALSA_LOGS, false));
        cbDeviceSnapshot.setChecked(preferences.getBoolean(ForensicConfig.PREF_ENABLE_DEVICE_SNAPSHOT, true));
        cbNonRootCapture.setChecked(preferences.getBoolean(ForensicConfig.PREF_ENABLE_NONROOT_CAPTURE, true));
        cbRootCapture.setChecked(preferences.getBoolean(ForensicConfig.PREF_ENABLE_ROOT_CAPTURE, true));
        cbShizukuCapture.setChecked(preferences.getBoolean(ForensicConfig.PREF_ENABLE_SHIZUKU_CAPTURE, false));
        if (!ForensicConfig.isShizukuInstalled(requireContext())) {
            cbShizukuCapture.setChecked(false);
            cbShizukuCapture.setEnabled(false);
        }
        updateCaptureCommandPreview(cbRootCapture, cbShizukuCapture, adbCaptureCommand);
        cbRootCapture.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateCaptureCommandPreview(cbRootCapture, cbShizukuCapture, adbCaptureCommand));
        cbShizukuCapture.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateCaptureCommandPreview(cbRootCapture, cbShizukuCapture, adbCaptureCommand));
        ForensicConfig.Snapshot openSnapshot = ForensicConfig.fromPreferences(preferences);
        ForensicLogger.logEvent(
                requireContext(),
                "info",
                "FORENSIC_CENTER_OPENED",
                null,
                "forensic_center",
                "forensic_center_opened",
                ForensicLogger.fields(
                        "runtime_summary", ForensicConfig.buildRuntimeSummary(openSnapshot),
                        "capture_summary", ForensicConfig.buildCaptureSummary(requireContext(), openSnapshot),
                        "adb_capture_mode", ForensicConfig.normalizeAdbCaptureMode(openSnapshot.adbCaptureMode),
                        "shizuku_requested", openSnapshot.enableShizukuCapture ? "1" : "0"
                )
        );

        view.findViewById(R.id.BTRunRootCapture).setOnClickListener(v ->
                runRootCaptureNow(cbRootCapture, adbCaptureCommand));
        view.findViewById(R.id.BTCopyAdbCaptureCommand).setOnClickListener(v ->
                copyAdbCaptureCommandToClipboard(adbCaptureCommand));
        adbCommand.setOnClickListener(v -> copyAdbCommandToClipboard());
        adbCaptureCommand.setOnClickListener(v -> copyAdbCaptureCommandToClipboard(adbCaptureCommand));

        view.findViewById(R.id.BTSaveForensic).setOnClickListener(v -> {
            preferences.edit()
                    .putBoolean("enable_wine_debug", cbWineDebug.isChecked())
                    .putBoolean("enable_box64_logs", cbBox64Logs.isChecked())
                    .putBoolean(ForensicConfig.PREF_ENABLE_LOADER_TRACE, cbLoaderTrace.isChecked())
                    .putBoolean(ForensicConfig.PREF_ENABLE_FEX_LOGS, cbFexLogs.isChecked())
                    .putBoolean(ForensicConfig.PREF_ENABLE_TURNIP_LOGS, cbTurnipLogs.isChecked())
                    .putBoolean(ForensicConfig.PREF_ENABLE_DXVK_LOGS, cbDxvkLogs.isChecked())
                    .putBoolean(ForensicConfig.PREF_ENABLE_VKD3D_LOGS, cbVkd3dLogs.isChecked())
                    .putBoolean(ForensicConfig.PREF_ENABLE_DGVOODOO_LOGS, cbDgVoodooLogs.isChecked())
                    .putBoolean(ForensicConfig.PREF_ENABLE_VULKAN_API_DUMP, cbVulkanApiDump.isChecked())
                    .putBoolean(ForensicConfig.PREF_ENABLE_VULKAN_LOADER_DEBUG, cbVulkanLoaderDebug.isChecked())
                    .putBoolean(ForensicConfig.PREF_ENABLE_VULKAN_VALIDATION, cbVulkanValidation.isChecked())
                    .putBoolean(ForensicConfig.PREF_ENABLE_PULSE_LOGS, cbPulseLogs.isChecked())
                    .putBoolean(ForensicConfig.PREF_ENABLE_ALSA_LOGS, cbAlsaLogs.isChecked())
                    .putBoolean(ForensicConfig.PREF_ENABLE_DEVICE_SNAPSHOT, cbDeviceSnapshot.isChecked())
                    .putBoolean(ForensicConfig.PREF_ENABLE_NONROOT_CAPTURE, cbNonRootCapture.isChecked())
                    .putBoolean(ForensicConfig.PREF_ENABLE_ROOT_CAPTURE, cbRootCapture.isChecked())
                    .putBoolean(ForensicConfig.PREF_ENABLE_SHIZUKU_CAPTURE, cbShizukuCapture.isChecked())
                    .putString(ForensicConfig.PREF_ADB_CAPTURE_MODE, resolveCaptureMode(cbRootCapture, cbShizukuCapture))
                    .apply();

            ForensicConfig.Snapshot savedSnapshot = ForensicConfig.fromPreferences(preferences);
            ForensicLogger.logEvent(
                    requireContext(),
                    "info",
                    "FORENSIC_PROFILE_SAVED",
                    null,
                    "forensic_center",
                    "forensic_profile_saved",
                    ForensicLogger.fields(
                            "runtime_summary", ForensicConfig.buildRuntimeSummary(savedSnapshot),
                            "capture_summary", ForensicConfig.buildCaptureSummary(requireContext(), savedSnapshot),
                            "adb_capture_mode", ForensicConfig.normalizeAdbCaptureMode(savedSnapshot.adbCaptureMode),
                            "shizuku_requested", savedSnapshot.enableShizukuCapture ? "1" : "0"
                    )
            );
            AppUtils.showToast(getContext(), R.string.diagnostics_saved);
        });

        view.findViewById(R.id.BTViewForensicLog).setOnClickListener(v -> showForensicLogViewer());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle(R.string.diagnostics);
    }

    private void showForensicLogViewer() {
        Context context = getContext();
        if (context == null) return;

        File latestFile = ForensicLogger.getLatestLogFile(context);
        String tail = ForensicLogger.readLatestTraceTail(context, 1200, 150000);
        if (tail == null || tail.trim().isEmpty()) {
            tail = getString(R.string.diagnostics_forensic_log_empty);
        }

        ForensicLogger.logEvent(
                context,
                "info",
                "FORENSIC_LOG_VIEW_OPENED",
                null,
                "forensic_center",
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
        View dialogMessage = dialog.findViewById(R.id.TVMessage);
        if (dialogMessage != null) dialogMessage.setVisibility(View.GONE);

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
                    tvFile.setText(getString(R.string.diagnostics_forensic_log_file, activeFile.getName()));
                } else {
                    tvFile.setVisibility(View.GONE);
                }
            }
            if (tvStats != null) {
                tvStats.setText(buildCompactLogViewerStats(activeFile, activeTail));
            }
            if (tvTicker != null) {
                tvTicker.setText(getString(
                        autoRefresh[0]
                                ? R.string.diagnostics_forensic_log_live
                                : R.string.diagnostics_forensic_log_paused,
                        DateFormat.format("HH:mm:ss", new Date())
                ));
                tvTicker.setSelected(true);
            }
            if (tvFile != null) tvFile.setSelected(true);
            if (tvStats != null) tvStats.setSelected(true);
            if (logView != null) {
                logView.replaceRawText(formatForensicTailForConsolePlain(activeTail));
            }
        };
        applyTail.run();

        View btConfirm = dialog.findViewById(R.id.BTConfirm);
        View btCancel = dialog.findViewById(R.id.BTCancel);
        if (btConfirm != null) btConfirm.setVisibility(View.GONE);
        if (btCancel != null) btCancel.setVisibility(View.GONE);

        View btExport = dialog.findViewById(R.id.BTForensicExportLog);
        btExport.setOnClickListener(v -> exportForensicSnapshot(currentFile[0], currentTail[0]));

        View btCopy = dialog.findViewById(R.id.BTForensicCopyLog);
        btCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null) return;
            clipboard.setPrimaryClip(ClipData.newPlainText("forensic_log_tail", currentTail[0]));
            AppUtils.showToast(context, R.string.copied_to_clipboard);
        });
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
        styleForensicLogViewerDialog(dialog);
        final Runnable[] refreshRunnable = new Runnable[1];
        refreshRunnable[0] = new Runnable() {
            @Override
            public void run() {
                if (!dialog.isShowing()) return;
                if (autoRefresh[0]) {
                    File nextFile = ForensicLogger.getLatestLogFile(context);
                    String nextTail = ForensicLogger.readLatestTraceTail(context, 1200, 150000);
                    if (nextTail == null || nextTail.trim().isEmpty()) {
                        nextTail = getString(R.string.diagnostics_forensic_log_empty);
                    }
                    currentFile[0] = nextFile;
                    currentTail[0] = nextTail;
                    applyTail.run();
                } else if (tvTicker != null) {
                    tvTicker.setText(getString(R.string.diagnostics_forensic_log_paused, DateFormat.format("HH:mm:ss", new Date())));
                }
                handler.postDelayed(this, 1250L);
            }
        };
        handler.postDelayed(refreshRunnable[0], 1250L);
        View bottomBar = dialog.findViewById(R.id.LLBottomBar);
        if (bottomBar != null) bottomBar.setVisibility(View.GONE);
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

    private void styleForensicLogViewerDialog(ContentDialog dialog) {
        if (dialog == null || getContext() == null) return;
        Context context = getContext();
        int horizontalPadding = Math.round(context.getResources().getDisplayMetrics().density * 3f);
        int topPadding = Math.round(context.getResources().getDisplayMetrics().density * 3f);
        View root = dialog.getContentView();
        if (root != null) {
            root.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
            root.setPadding(horizontalPadding, topPadding, horizontalPadding, topPadding);
        }
        View frameLayout = dialog.findViewById(R.id.FrameLayout);
        if (frameLayout != null) {
            frameLayout.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
        }
        View titleBar = dialog.findViewById(R.id.LLTitleBar);
        if (titleBar != null) {
            titleBar.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
            titleBar.setPadding(horizontalPadding, topPadding, horizontalPadding, 0);
        }
        View bottomBar = dialog.findViewById(R.id.LLBottomBar);
        if (bottomBar != null) {
            bottomBar.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
        }
        TextView titleView = dialog.findViewById(R.id.TVTitle);
        if (titleView != null) {
            titleView.setTextColor(ContextCompat.getColor(context, R.color.surface_runtime_taskmgr_text));
        }
        TextView messageView = dialog.findViewById(R.id.TVMessage);
        if (messageView != null) {
            messageView.setTextColor(ContextCompat.getColor(context, R.color.surface_runtime_taskmgr_muted));
        }
        android.widget.ImageView iconView = dialog.findViewById(R.id.IVIcon);
        if (iconView != null) {
            iconView.setColorFilter(ContextCompat.getColor(context, R.color.surface_runtime_taskmgr_text));
        }
        View titleBack = dialog.findViewById(R.id.BTTitleBack);
        if (titleBack instanceof android.widget.ImageButton) {
            ((android.widget.ImageButton) titleBack).setColorFilter(ContextCompat.getColor(context, R.color.surface_runtime_taskmgr_text));
            titleBack.setBackgroundResource(R.drawable.surface_runtime_button_neutral);
        }
        restyleForensicActionButton(dialog.findViewById(R.id.BTForensicCopyLog));
        restyleForensicActionButton(dialog.findViewById(R.id.BTForensicExportLog));
        restyleForensicActionButton(dialog.findViewById(R.id.BTForensicPauseLog));
        restyleForensicActionButton(dialog.findViewById(R.id.BTForensicCloseLog));
    }

    private void restyleForensicActionButton(View buttonView) {
        if (!(buttonView instanceof TextView) || getContext() == null) return;
        buttonView.setBackgroundResource(R.drawable.surface_runtime_button_neutral);
        ((TextView) buttonView).setTextColor(ContextCompat.getColor(getContext(), R.color.surface_runtime_button_text));
    }

    private String buildCompactLogViewerStats(File latestFile, String tail) {
        ForensicConfig.Snapshot snapshot = ForensicConfig.fromPreferences(preferences);
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
                + "  •  size " + fileSize + " B"
                + "  •  lines " + lineCount
                + "  •  info " + infoCount
                + "  •  warn " + warnCount
                + "  •  err " + errorCount
                + "  •  " + clipRuntimeSummary(ForensicConfig.buildRuntimeSummary(snapshot));
    }

    private String clipRuntimeSummary(String summary) {
        if (summary == null) return "-";
        String normalized = summary.replace('\n', ' ').replace('\r', ' ').trim();
        if (normalized.length() <= 52) return normalized;
        return normalized.substring(0, 49) + "...";
    }

    private CharSequence formatForensicTailForConsole(String tail) {
        if (tail == null || tail.trim().isEmpty()) {
            return getString(R.string.diagnostics_forensic_log_empty);
        }
        SpannableStringBuilder builder = new SpannableStringBuilder();
        String[] lines = tail.split("\n");
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) continue;
            if (builder.length() > 0) builder.append('\n');
            builder.append(formatForensicConsoleLine(line));
        }
        return builder.length() > 0 ? builder : getString(R.string.diagnostics_forensic_log_empty);
    }

    private String formatForensicTailForConsolePlain(String tail) {
        if (tail == null || tail.trim().isEmpty()) {
            return getString(R.string.diagnostics_forensic_log_empty);
        }
        StringBuilder builder = new StringBuilder();
        String[] lines = tail.split("\n");
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) continue;
            if (builder.length() > 0) builder.append('\n');
            builder.append(formatForensicConsoleLinePlain(line));
        }
        return builder.length() > 0 ? builder.toString() : getString(R.string.diagnostics_forensic_log_empty);
    }

    private String formatForensicConsoleLinePlain(String rawLine) {
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

    private CharSequence formatForensicConsoleLine(String rawLine) {
        try {
            JSONObject obj = new JSONObject(rawLine);
            String timestamp = compactForensicTimestamp(obj.optString("ts", ""));
            String severity = padRight(resolveForensicSeverity(obj).toUpperCase(Locale.US), 5);
            String stage = clipForensicValue(obj.optString("stage", "-"), 16);
            String eventId = clipForensicValue(obj.optString("event_id", "-"), 34);
            String message = clipForensicValue(obj.optString("message", ""), 88);
            SpannableStringBuilder builder = new SpannableStringBuilder();
            appendForensicSpan(builder, timestamp, 0xFF7FB4D4, Typeface.BOLD);
            builder.append("  ");
            appendForensicSpan(builder, severity, resolveForensicSeverityColor(resolveForensicSeverity(obj)), Typeface.BOLD);
            builder.append("  ");
            appendForensicSpan(builder, stage, 0xFF93B9CF, Typeface.BOLD);
            builder.append("  ");
            appendForensicSpan(builder, eventId, 0xFFE8F5FF, Typeface.BOLD);
            if (!message.isEmpty()) {
                builder.append("  ");
                appendForensicSpan(builder, message, 0xFFD8E6F0, Typeface.NORMAL);
            }
            String extras = buildForensicExtraSummary(obj);
            if (!extras.isEmpty()) {
                builder.append("\n             ");
                appendForensicSpan(builder, extras, 0xFF9CB5C7, Typeface.NORMAL);
            }
            return builder;
        } catch (Exception ignored) {
            return rawLine;
        }
    }

    private int resolveForensicSeverityColor(String severity) {
        if ("error".equals(severity)) return 0xFFFF8E7C;
        if ("warn".equals(severity) || "warning".equals(severity)) return 0xFFF3C969;
        return 0xFF7BE0D6;
    }

    private void appendForensicSpan(SpannableStringBuilder builder, String text, int color, int style) {
        if (text == null || text.isEmpty()) return;
        int start = builder.length();
        builder.append(text);
        int end = builder.length();
        builder.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (style != Typeface.NORMAL) {
            builder.setSpan(new StyleSpan(style), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private String buildForensicExtraSummary(JSONObject obj) {
        JSONArray names = obj.names();
        if (names == null || names.length() == 0) return "";
        ArrayList<String> parts = new ArrayList<>();
        for (int i = 0; i < names.length(); i++) {
            String key = names.optString(i, "");
            if (key.isEmpty()) continue;
            if ("ts".equals(key)
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
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) builder.append("  ");
            builder.append(parts.get(i));
        }
        return builder.toString();
    }

    private String resolveForensicSeverity(JSONObject obj) {
        String severity = obj.optString("severity", "").trim().toLowerCase(Locale.US);
        if (!severity.isEmpty()) return severity;
        return obj.optString("level", "").trim().toLowerCase(Locale.US);
    }

    private String compactForensicTimestamp(String timestamp) {
        if (timestamp == null) return "--:--:--.---";
        String normalized = timestamp.trim();
        if (normalized.length() >= 23 && normalized.charAt(10) == 'T') {
            return normalized.substring(11, 23);
        }
        return clipForensicValue(normalized, 12);
    }

    private String clipForensicValue(String value, int maxLength) {
        if (value == null) return "";
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        if (normalized.length() <= maxLength) return normalized;
        if (maxLength <= 3) return normalized.substring(0, Math.max(0, maxLength));
        return normalized.substring(0, maxLength - 3) + "...";
    }

    private String padRight(String value, int width) {
        String normalized = value != null ? value : "";
        if (normalized.length() >= width) return normalized.substring(0, width);
        StringBuilder builder = new StringBuilder(normalized);
        while (builder.length() < width) builder.append(' ');
        return builder.toString();
    }

    private String buildLogViewerStats(File latestFile, String tail) {
        ForensicConfig.Snapshot snapshot = ForensicConfig.fromPreferences(preferences);
        int lineCount = tail == null || tail.isEmpty() ? 0 : tail.split("\n").length;
        long fileSize = latestFile != null && latestFile.isFile() ? latestFile.length() : 0L;
        int jsonRecords = 0;
        int infoCount = 0;
        int warnCount = 0;
        int errorCount = 0;
        int dxvkHits = 0;
        int vkd3dHits = 0;
        int turnipHits = 0;
        int dgVoodooHits = 0;
        int box64Hits = 0;
        int fexHits = 0;

        if (tail != null && !tail.isEmpty()) {
            String[] lines = tail.split("\n");
            for (String line : lines) {
                if (line == null || line.trim().isEmpty()) continue;
                String lower = line.toLowerCase(Locale.US);
                if (lower.contains("dxvk")) dxvkHits++;
                if (lower.contains("vkd3d")) vkd3dHits++;
                if (lower.contains("turnip") || lower.contains("freedreno")) turnipHits++;
                if (lower.contains("dgvoodoo")) dgVoodooHits++;
                if (lower.contains("box64")) box64Hits++;
                if (lower.contains("fex")) fexHits++;
                try {
                    JSONObject obj = new JSONObject(line);
                    jsonRecords++;
                    String severity = resolveForensicSeverity(obj);
                    if ("info".equals(severity)) infoCount++;
                    else if ("warn".equals(severity) || "warning".equals(severity)) warnCount++;
                    else if ("error".equals(severity)) errorCount++;
                } catch (Exception ignored) {
                }
            }
        }

        return "Path: " + (latestFile != null ? latestFile.getAbsolutePath() : "-")
                + "\nSize: " + fileSize + " bytes"
                + "\nTail lines: " + lineCount
                + "\nJSON records: " + jsonRecords
                + "\nLevel counts: info=" + infoCount + " warn=" + warnCount + " error=" + errorCount
                + "\nSignals: dxvk=" + dxvkHits
                + " vkd3d=" + vkd3dHits
                + " turnip=" + turnipHits
                + " dgvoodoo=" + dgVoodooHits
                + " box64=" + box64Hits
                + " fex=" + fexHits
                + "\nRuntime: " + ForensicConfig.buildRuntimeSummary(snapshot)
                + "\nCapture: " + ForensicConfig.buildCaptureSummary(requireContext(), snapshot);
    }

    private void exportForensicSnapshot(File latestFile, String tail) {
        Context context = getContext();
        if (context == null) return;

        File outDir = new File(Environment.getExternalStorageDirectory(), "Winlator/forensics/exports");
        if (!outDir.exists() && !outDir.mkdirs()) {
            AppUtils.showToast(context, R.string.diagnostics_forensic_log_export_fail);
            return;
        }
        String ts = DateFormat.format("yyyy-MM-dd_HH-mm-ss", new Date()).toString();
        File outFile = new File(outDir, String.format(Locale.US, "forensics_%s.jsonl", ts));
        String exportBody = tail == null ? "" : tail;
        if (latestFile != null && latestFile.isFile()) {
            try {
                String fullLog = FileUtils.readString(latestFile);
                if (fullLog != null && !fullLog.trim().isEmpty()) {
                    exportBody = fullLog;
                }
            } catch (Exception ignored) {
            }
        }
        if (!FileUtils.writeString(outFile, exportBody)) {
            AppUtils.showToast(context, R.string.diagnostics_forensic_log_export_fail);
            return;
        }
        AppUtils.showToast(context, getString(R.string.diagnostics_forensic_log_export_ok, outFile.getAbsolutePath()));
        ForensicLogger.logEvent(
                context,
                "info",
                "FORENSIC_LOG_EXPORTED",
                null,
                "forensic_center",
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

    private void runRootCaptureNow(CheckBox cbRootCapture, TextView captureCommandView) {
        Context context = getContext();
        if (context == null) return;
        if (!cbRootCapture.isChecked() || !ForensicConfig.isRootBinaryPresent()) {
            AppUtils.showToast(context, R.string.forensic_root_unavailable);
            return;
        }

        PreloaderDialog preloaderDialog = new PreloaderDialog(requireActivity());
        preloaderDialog.showOnUiThread(R.string.diagnostics_forensic_root_capture_running);
        ForensicConfig.Snapshot snapshot = ForensicConfig.fromPreferences(preferences);
        String script = ForensicConfig.buildOnDeviceRootCaptureScript(context, snapshot);
        String previewCommand = captureCommandView != null
                ? String.valueOf(captureCommandView.getText())
                : ForensicConfig.buildOnDeviceRootCaptureCommand(context, snapshot);

        Executors.newSingleThreadExecutor().execute(() -> {
            boolean success = false;
            String output = "";
            int exitCode = -1;
            try {
                Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", script});
                StringBuilder builder = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (builder.length() > 0) builder.append('\n');
                        builder.append(line.trim());
                    }
                }
                exitCode = process.waitFor();
                output = builder.toString().trim();
                success = exitCode == 0;
            } catch (Exception e) {
                Log.e(TAG, "Root capture script failed before completion", e);
                output = e.getMessage() != null ? e.getMessage().trim() : "";
            }

            final boolean finalSuccess = success;
            final int finalExitCode = exitCode;
            final String finalOutput = output;
            UiLifecycleGuard.runOnUiThread(this, () -> {
                preloaderDialog.closeOnUiThread();
                if (finalSuccess) {
                    String location = "/sdcard/Winlator/forensics/issue-bundles";
                    if (finalOutput != null && !finalOutput.trim().isEmpty()) {
                        String[] lines = finalOutput.split("\n");
                        for (String line : lines) {
                            if (line == null) continue;
                            String trimmed = line.trim();
                            if (trimmed.startsWith("archive=")) {
                                location = trimmed.substring("archive=".length());
                                break;
                            }
                            if (trimmed.startsWith("bundle=")) {
                                location = trimmed.substring("bundle=".length());
                            }
                        }
                    }
                    AppUtils.showToast(context, getString(R.string.diagnostics_forensic_root_capture_ok, location));
                } else {
                    AppUtils.showToast(context, getString(R.string.diagnostics_forensic_root_capture_fail, finalExitCode));
                }
                ForensicLogger.logEvent(
                        context,
                        finalSuccess ? "info" : "warn",
                        "FORENSIC_ROOT_CAPTURE_RUN",
                        null,
                        "forensic_center",
                        finalSuccess ? "forensic_root_capture_ok" : "forensic_root_capture_fail",
                        ForensicLogger.fields(
                            "exit_code", finalExitCode,
                            "capture_mode", ForensicConfig.ADB_CAPTURE_MODE_ROOT,
                            "capture_command", previewCommand,
                            "output", finalOutput,
                            "runtime_summary", ForensicConfig.buildRuntimeSummary(snapshot),
                            "capture_summary", ForensicConfig.buildCaptureSummary(context, snapshot)
                        )
                );
            }, "ForensicCenterFragment", "root_capture_result");
        });
    }

    private void reportForensicIssue(String tail, File latestFile) {
        Context context = getContext();
        if (context == null) return;
        String issueTitle = "Forensic report: " + (latestFile != null ? latestFile.getName() : "runtime");
        PreloaderDialog preloaderDialog = new PreloaderDialog(requireActivity());
        preloaderDialog.showOnUiThread(R.string.diagnostics_forensic_report_running);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Map<String, String> extras = new LinkedHashMap<>();
                String summary = buildLogViewerStats(latestFile, tail);
                String digest = buildErrorDigest(tail);
                extras.put("forensic-tail.txt", tail == null ? "" : tail);
                extras.put("forensic-summary.txt", summary);
                extras.put("forensic-errors.txt", digest);
                extras.put("capture-preview.txt", ForensicConfig.buildCaptureCommand(context, ForensicConfig.fromPreferences(preferences)));
                JSONObject supplemental = ForensicLogger.fields(
                        "report_source", "forensic_log_viewer",
                        "log_file", latestFile != null ? latestFile.getAbsolutePath() : "",
                        "tail_chars", tail != null ? tail.length() : 0,
                        "runtime_summary", ForensicConfig.buildRuntimeSummary(ForensicConfig.fromPreferences(preferences)),
                        "capture_summary", ForensicConfig.buildCaptureSummary(context, ForensicConfig.fromPreferences(preferences))
                );
                ForensicIssueComposer.IssueBundleResult bundle = ForensicIssueComposer.createIssueBundle(
                        context,
                        issueTitle,
                        "Auto-generated from Forensic Log Viewer",
                        null,
                        extras,
                        supplemental
                );
                StringBuilder bodyBuilder = new StringBuilder();
                bodyBuilder.append("Auto-generated forensic report from Ae.solator.\n\n");
                bodyBuilder.append("Bundle ZIP path (attach manually): `").append(bundle.zipFile.getAbsolutePath()).append("`\n");
                bodyBuilder.append("Bundle directory: `").append(bundle.bundleDir.getAbsolutePath()).append("`\n\n");
                bodyBuilder.append("### Runtime summary\n```text\n")
                        .append(summary)
                        .append("\n```\n\n");
                bodyBuilder.append("### Error digest\n```text\n")
                        .append(digest)
                        .append("\n```\n\n");
                bodyBuilder.append("Full forensic logs are included in bundle files only.\n\n");
                bodyBuilder.append(bundle.markdown);
                String body = bodyBuilder.toString();
                if (body.length() > 7000) {
                    body = body.substring(0, 7000) + "\n\n[truncated by reporter]";
                }
                String url = "https://github.com/kosoymiki/wcp-runtime-lanes/issues/new?title="
                        + Uri.encode(issueTitle)
                        + "&body="
                        + Uri.encode(body);

                String finalBody = body;
                UiLifecycleGuard.runOnUiThread(this, () -> {
                    preloaderDialog.closeOnUiThread();
                    Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browser);
                    AppUtils.showToast(context, getString(R.string.diagnostics_forensic_report_bundle_ready, bundle.zipFile.getAbsolutePath()));
                    ForensicLogger.logEvent(
                            context,
                            "info",
                            "FORENSIC_REPORT_PREPARED",
                            null,
                            "forensic_center",
                            "forensic_report_bundle_ready",
                            ForensicLogger.fields(
                                    "bundle_zip", bundle.zipFile.getAbsolutePath(),
                                    "bundle_dir", bundle.bundleDir.getAbsolutePath(),
                                    "issue_body_chars", finalBody.length()
                            )
                    );
                }, "ForensicCenterFragment", "forensic_issue_prepared");
            } catch (Exception error) {
                UiLifecycleGuard.runOnUiThread(this, () -> {
                    preloaderDialog.closeOnUiThread();
                    AppUtils.showToast(context, R.string.diagnostics_forensic_report_failed);
                    ForensicLogger.logEvent(
                            context,
                            "error",
                            "FORENSIC_REPORT_FAILED",
                            null,
                            "forensic_center",
                            "forensic_report_failed",
                            ForensicLogger.fields("error", String.valueOf(error.getMessage()))
                    );
                }, "ForensicCenterFragment", "forensic_issue_failed");
            }
        });
    }

    private String buildErrorDigest(String tail) {
        if (tail == null || tail.trim().isEmpty()) {
            return "No log lines available.";
        }
        String[] lines = tail.split("\n");
        StringBuilder digest = new StringBuilder();
        int added = 0;
        for (int i = lines.length - 1; i >= 0 && added < 25; i--) {
            String line = lines[i];
            if (line == null) continue;
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            String lower = trimmed.toLowerCase(Locale.US);
            if (lower.contains(" error")
                    || lower.startsWith("error")
                    || lower.contains("\"level\":\"error\"")
                    || lower.contains(" fatal")
                    || lower.contains(" failed")
                    || lower.contains(" exception")) {
                digest.append(trimmed).append('\n');
                added++;
            }
        }
        if (added == 0) {
            return "No explicit error lines in current tail.";
        }
        return digest.toString().trim();
    }

    private void copyAdbCommandToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("adb_forensic_command", getString(R.string.diagnostics_adb_command)));
            AppUtils.showToast(getContext(), R.string.copied_to_clipboard);
            ForensicLogger.logEvent(
                    requireContext(),
                    "info",
                    "FORENSIC_ADB_COMMAND_COPIED",
                    null,
                    "forensic_center",
                    "adb_forensic_command_copied",
                    null
            );
        }
    }

    private void updateCaptureCommandPreview(CheckBox cbRootCapture,
                                             CheckBox cbShizukuCapture,
                                             TextView tvCaptureCommand) {
        if (tvCaptureCommand == null) return;
        ForensicConfig.Snapshot snapshot = ForensicConfig.fromPreferences(preferences);
        snapshot.enableRootCapture = cbRootCapture.isChecked();
        snapshot.enableShizukuCapture = cbShizukuCapture.isChecked();
        snapshot.adbCaptureMode = resolveCaptureMode(cbRootCapture, cbShizukuCapture);
        tvCaptureCommand.setText(ForensicConfig.buildCaptureCommand(requireContext(), snapshot));
    }

    private String resolveCaptureMode(CheckBox cbRootCapture, CheckBox cbShizukuCapture) {
        if (cbShizukuCapture != null
                && cbShizukuCapture.isChecked()
                && ForensicConfig.isShizukuInstalled(requireContext())) {
            return ForensicConfig.ADB_CAPTURE_MODE_SHIZUKU;
        }
        if (cbRootCapture != null
                && cbRootCapture.isChecked()
                && ForensicConfig.isRootBinaryPresent()) {
            return ForensicConfig.ADB_CAPTURE_MODE_ROOT;
        }
        return ForensicConfig.ADB_CAPTURE_MODE_AUTO;
    }

    private void copyAdbCaptureCommandToClipboard(TextView captureCommandView) {
        if (captureCommandView == null) return;
        CharSequence value = captureCommandView.getText();
        if (value == null || value.toString().trim().isEmpty()) return;
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) return;
        clipboard.setPrimaryClip(ClipData.newPlainText("adb_forensic_capture_command", value.toString()));
        AppUtils.showToast(getContext(), R.string.copied_to_clipboard);
        ForensicLogger.logEvent(
                requireContext(),
                "info",
                "FORENSIC_ADB_CAPTURE_COMMAND_COPIED",
                null,
                "forensic_center",
                "adb_forensic_capture_command_copied",
                null
        );
    }
}
