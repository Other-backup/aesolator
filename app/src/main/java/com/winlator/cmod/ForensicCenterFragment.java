package com.winlator.cmod;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.Spinner;
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
import com.winlator.cmod.core.SpinnerAdapters;
import com.winlator.cmod.contentdialog.ContentDialog;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class ForensicCenterFragment extends Fragment {
    private SharedPreferences preferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.forensic_center_fragment, container, false);
        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        boolean isDarkMode = preferences.getBoolean("dark_mode", false);

        CheckBox cbWineDebug = view.findViewById(R.id.CBForensicWineDebug);
        CheckBox cbBox64Logs = view.findViewById(R.id.CBForensicBox64Logs);
        CheckBox cbUseDri3 = view.findViewById(R.id.CBForensicDRI3);
        Spinner sDri3Mode = view.findViewById(R.id.SForensicDri3Mode);
        CheckBox cbDri3PresentWait = view.findViewById(R.id.CBForensicDri3PresentWait);
        CheckBox cbDri3ForceSwWsi = view.findViewById(R.id.CBForensicDri3ForceSwWsi);
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
        int spinnerBackground = isDarkMode ? R.drawable.combo_box_dark : R.drawable.combo_box;
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
        badgeDgVoodoo.setBackgroundResource(badgeBackground);
        badgeFreeWine.setTextColor(badgeTextColor);
        badgeDxvk.setTextColor(badgeTextColor);
        badgeDgVoodoo.setTextColor(badgeTextColor);
        sDri3Mode.setBackgroundResource(spinnerBackground);

        cbWineDebug.setChecked(preferences.getBoolean("enable_wine_debug", false));
        cbBox64Logs.setChecked(preferences.getBoolean("enable_box64_logs", false));
        cbUseDri3.setChecked(preferences.getBoolean("use_dri3", true));
        cbDri3PresentWait.setChecked(preferences.getBoolean("dri3_present_wait", true));
        cbDri3ForceSwWsi.setChecked(preferences.getBoolean("dri3_force_sw_wsi", false));
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
                        "shizuku_requested", openSnapshot.enableShizukuCapture ? "1" : "0",
                        "dri3_mode", preferences.getString("dri3_mode", "auto"),
                        "dri3_present_wait", preferences.getBoolean("dri3_present_wait", true) ? "1" : "0",
                        "dri3_force_sw_wsi", preferences.getBoolean("dri3_force_sw_wsi", false) ? "1" : "0"
                )
        );
        String[] dri3Labels = getResources().getStringArray(R.array.dri3_mode_entries);
        String[] dri3Values = getResources().getStringArray(R.array.dri3_mode_values);
        String selectedDri3Mode = preferences.getString("dri3_mode", cbUseDri3.isChecked() ? "auto" : "off");
        sDri3Mode.setAdapter(SpinnerAdapters.create(requireContext(), isDarkMode, dri3Labels));
        sDri3Mode.setPopupBackgroundResource(isDarkMode ? R.drawable.content_dialog_background_dark : R.drawable.content_dialog_background);
        for (int i = 0; i < dri3Values.length; i++) {
            if (dri3Values[i].equalsIgnoreCase(selectedDri3Mode)) {
                sDri3Mode.setSelection(i);
                break;
            }
        }
        updateDri3UiState(cbUseDri3, sDri3Mode, cbDri3PresentWait, cbDri3ForceSwWsi, dri3Values);

        cbUseDri3.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                setDri3ModeSpinner(sDri3Mode, dri3Values, "off");
            } else if ("off".equalsIgnoreCase(dri3Values[sDri3Mode.getSelectedItemPosition()])) {
                setDri3ModeSpinner(sDri3Mode, dri3Values, "auto");
            }
            updateDri3UiState(cbUseDri3, sDri3Mode, cbDri3PresentWait, cbDri3ForceSwWsi, dri3Values);
        });

        sDri3Mode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view1, int position, long id) {
                cbUseDri3.setChecked(!"off".equalsIgnoreCase(dri3Values[position]));
                updateDri3UiState(cbUseDri3, sDri3Mode, cbDri3PresentWait, cbDri3ForceSwWsi, dri3Values);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

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
                    .putBoolean("use_dri3", cbUseDri3.isChecked())
                    .putString("dri3_mode", dri3Values[sDri3Mode.getSelectedItemPosition()])
                    .putBoolean("dri3_present_wait", cbDri3PresentWait.isChecked())
                    .putBoolean("dri3_force_sw_wsi", cbDri3ForceSwWsi.isChecked())
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
                            "shizuku_requested", savedSnapshot.enableShizukuCapture ? "1" : "0",
                            "dri3_mode", preferences.getString("dri3_mode", "auto"),
                            "dri3_present_wait", preferences.getBoolean("dri3_present_wait", true) ? "1" : "0",
                            "dri3_force_sw_wsi", preferences.getBoolean("dri3_force_sw_wsi", false) ? "1" : "0"
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

    private void setDri3ModeSpinner(Spinner spinner, String[] values, String selectedValue) {
        for (int i = 0; i < values.length; i++) {
            if (selectedValue.equalsIgnoreCase(values[i])) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void updateDri3UiState(CheckBox cbUseDri3, Spinner sDri3Mode, CheckBox cbDri3PresentWait,
                                   CheckBox cbDri3ForceSwWsi, String[] dri3Values) {
        String mode = dri3Values[sDri3Mode.getSelectedItemPosition()];
        boolean enabled = cbUseDri3.isChecked() && !"off".equalsIgnoreCase(mode);
        cbDri3PresentWait.setEnabled(enabled);
        cbDri3ForceSwWsi.setEnabled(enabled);
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
        dialog.setBottomBarText(null);
        View dialogMessage = dialog.findViewById(R.id.TVMessage);
        if (dialogMessage != null) dialogMessage.setVisibility(View.GONE);

        TextView tvFile = dialog.findViewById(R.id.TVForensicLogFile);
        TextView tvStats = dialog.findViewById(R.id.TVForensicLogStats);
        TextView tvBody = dialog.findViewById(R.id.TVForensicLogBody);
        tvFile.setText(getString(R.string.diagnostics_forensic_log_file, latestFile != null ? latestFile.getName() : "-"));
        tvStats.setText(buildLogViewerStats(latestFile, tail));
        tvBody.setText(tail);

        View btConfirm = dialog.findViewById(R.id.BTConfirm);
        View btCancel = dialog.findViewById(R.id.BTCancel);
        if (btConfirm instanceof TextView) {
            ((TextView) btConfirm).setText(R.string.diagnostics_forensic_log_report);
        }
        if (btCancel instanceof TextView) {
            ((TextView) btCancel).setText(R.string.cancel);
        }

        final String finalTail = tail;
        dialog.setOnConfirmCallback(() -> {
            ContentDialog.confirm(
                    context,
                    R.string.diagnostics_forensic_report_privacy_warning,
                    () -> reportForensicIssue(finalTail, latestFile)
            );
        });

        View btExport = dialog.findViewById(R.id.BTForensicExportLog);
        btExport.setOnClickListener(v -> exportForensicSnapshot(finalTail));

        View btCopy = dialog.findViewById(R.id.BTForensicCopyLog);
        btCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null) return;
            clipboard.setPrimaryClip(ClipData.newPlainText("forensic_log_tail", finalTail));
            AppUtils.showToast(context, R.string.copied_to_clipboard);
        });

        dialog.show();
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
                    String level = obj.optString("level", "").trim().toLowerCase(Locale.US);
                    if ("info".equals(level)) infoCount++;
                    else if ("warn".equals(level)) warnCount++;
                    else if ("error".equals(level)) errorCount++;
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

    private void exportForensicSnapshot(String tail) {
        Context context = getContext();
        if (context == null) return;

        File outDir = new File(Environment.getExternalStorageDirectory(), "Winlator/forensics/exports");
        if (!outDir.exists() && !outDir.mkdirs()) {
            AppUtils.showToast(context, R.string.diagnostics_forensic_log_export_fail);
            return;
        }
        String ts = DateFormat.format("yyyy-MM-dd_HH-mm-ss", new Date()).toString();
        File outFile = new File(outDir, String.format(Locale.US, "forensics_%s.jsonl", ts));
        if (!FileUtils.writeString(outFile, tail)) {
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
                        "export_file", outFile.getAbsolutePath(),
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
            } catch (Exception ignored) {
            }

            final boolean finalSuccess = success;
            final int finalExitCode = exitCode;
            final String finalOutput = output;
            if (!isAdded() || getActivity() == null) return;
            requireActivity().runOnUiThread(() -> {
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
            });
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

                if (!isAdded() || getActivity() == null) return;
                String finalBody = body;
                requireActivity().runOnUiThread(() -> {
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
                });
            } catch (Exception error) {
                if (!isAdded() || getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
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
                });
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
