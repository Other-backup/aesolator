package com.winlator.cmod;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.navigation.NavigationView;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.ForensicConfig;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.contentdialog.ContentDialog;

import java.io.File;
import java.util.Date;
import java.util.Locale;

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
        TextView badgeFreeWine = view.findViewById(R.id.TVPolicyBadgeFreeWine);
        TextView badgeDxvk = view.findViewById(R.id.TVPolicyBadgeDxvk);
        TextView badgeDgVoodoo = view.findViewById(R.id.TVPolicyBadgeDgVoodoo);
        TextView adbCommand = view.findViewById(R.id.TVAdbCommand);

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
        badgeFreeWine.setBackgroundResource(badgeBackground);
        badgeDxvk.setBackgroundResource(badgeBackground);
        badgeDgVoodoo.setBackgroundResource(badgeBackground);
        badgeFreeWine.setTextColor(badgeTextColor);
        badgeDxvk.setTextColor(badgeTextColor);
        badgeDgVoodoo.setTextColor(badgeTextColor);

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
                        "dri3_mode", preferences.getString("dri3_mode", "auto"),
                        "dri3_present_wait", preferences.getBoolean("dri3_present_wait", true) ? "1" : "0",
                        "dri3_force_sw_wsi", preferences.getBoolean("dri3_force_sw_wsi", false) ? "1" : "0"
                )
        );
        String[] dri3Labels = getResources().getStringArray(R.array.dri3_mode_entries);
        String[] dri3Values = getResources().getStringArray(R.array.dri3_mode_values);
        String selectedDri3Mode = preferences.getString("dri3_mode", cbUseDri3.isChecked() ? "auto" : "off");
        sDri3Mode.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, dri3Labels));
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

        view.findViewById(R.id.BTCopyAdbCommand).setOnClickListener(v -> {
            copyAdbCommandToClipboard();
        });
        adbCommand.setOnClickListener(v -> copyAdbCommandToClipboard());

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
                            "dri3_mode", preferences.getString("dri3_mode", "auto"),
                            "dri3_present_wait", preferences.getBoolean("dri3_present_wait", true) ? "1" : "0",
                            "dri3_force_sw_wsi", preferences.getBoolean("dri3_force_sw_wsi", false) ? "1" : "0"
                    )
            );
            AppUtils.showToast(getContext(), R.string.diagnostics_saved);
        });

        view.findViewById(R.id.BTViewForensicLog).setOnClickListener(v -> showForensicLogViewer());

        view.findViewById(R.id.BTForensicOpenX11).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                NavigationView navigationView = getActivity().findViewById(R.id.NavigationView);
                if (navigationView != null) {
                    ForensicLogger.logEvent(
                            requireContext(),
                            "info",
                            "FORENSIC_OPEN_X11_SETTINGS",
                            null,
                            "forensic_center",
                            "open_x11_settings_from_forensic",
                            null
                    );
                    navigationView.setCheckedItem(R.id.main_menu_settings);
                    ((MainActivity) getActivity()).onNavigationItemSelected(
                            navigationView.getMenu().findItem(R.id.main_menu_settings)
                    );
                }
            }
        });

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
        TextView tvBody = dialog.findViewById(R.id.TVForensicLogBody);
        tvFile.setText(getString(R.string.diagnostics_forensic_log_file, latestFile != null ? latestFile.getName() : "-"));
        tvBody.setText(tail);

        View btConfirm = dialog.findViewById(R.id.BTConfirm);
        View btCancel = dialog.findViewById(R.id.BTCancel);
        if (btConfirm instanceof TextView) {
            ((TextView) btConfirm).setText(R.string.copy);
        }
        if (btCancel instanceof TextView) {
            ((TextView) btCancel).setText(R.string.cancel);
        }

        final String finalTail = tail;
        dialog.setOnConfirmCallback(() -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("forensic_log_tail", finalTail));
                AppUtils.showToast(context, R.string.copied_to_clipboard);
                ForensicLogger.logEvent(
                        context,
                        "info",
                        "FORENSIC_LOG_TAIL_COPIED",
                        null,
                        "forensic_center",
                        "forensic_log_tail_copied",
                        ForensicLogger.fields("tail_chars", finalTail.length())
                );
            }
        });

        View btExport = dialog.findViewById(R.id.BTForensicExportLog);
        btExport.setOnClickListener(v -> {
            File rootDir = context.getExternalFilesDir(null);
            if (rootDir == null) {
                AppUtils.showToast(context, R.string.diagnostics_forensic_log_export_fail);
                return;
            }
            File outDir = new File(rootDir, "forensics/exports");
            if (!outDir.exists() && !outDir.mkdirs()) {
                AppUtils.showToast(context, R.string.diagnostics_forensic_log_export_fail);
                return;
            }
            String ts = DateFormat.format("yyyy-MM-dd_HH-mm-ss", new Date()).toString();
            File outFile = new File(outDir, String.format(Locale.US, "forensics_%s.jsonl", ts));
            if (FileUtils.writeString(outFile, finalTail)) {
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
                                "tail_chars", finalTail.length()
                        )
                );
            } else {
                AppUtils.showToast(context, R.string.diagnostics_forensic_log_export_fail);
                ForensicLogger.logEvent(
                        context,
                        "warn",
                        "FORENSIC_LOG_EXPORT_FAILED",
                        null,
                        "forensic_center",
                        "forensic_log_export_failed",
                        ForensicLogger.fields("export_file", outFile.getAbsolutePath())
                );
            }
        });

        dialog.show();
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
}
