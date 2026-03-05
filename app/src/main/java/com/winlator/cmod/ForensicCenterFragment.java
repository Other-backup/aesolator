package com.winlator.cmod;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.winlator.cmod.core.AppUtils;

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
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("adb_forensic_command", getString(R.string.diagnostics_adb_command)));
                AppUtils.showToast(getContext(), R.string.copied_to_clipboard);
            }
        });

        view.findViewById(R.id.BTSaveForensic).setOnClickListener(v -> {
            preferences.edit()
                    .putBoolean("enable_wine_debug", cbWineDebug.isChecked())
                    .putBoolean("enable_box64_logs", cbBox64Logs.isChecked())
                    .putBoolean("use_dri3", cbUseDri3.isChecked())
                    .putString("dri3_mode", dri3Values[sDri3Mode.getSelectedItemPosition()])
                    .putBoolean("dri3_present_wait", cbDri3PresentWait.isChecked())
                    .putBoolean("dri3_force_sw_wsi", cbDri3ForceSwWsi.isChecked())
                    .apply();
            AppUtils.showToast(getContext(), R.string.diagnostics_saved);
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
}
