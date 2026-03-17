package com.winlator.cmod.core;

import android.app.Activity;
import android.app.Dialog;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.winlator.cmod.R;

public class PreloaderDialog {
    private final Activity activity;
    private Dialog dialog;
    private CharSequence baseText = "";

    public PreloaderDialog(Activity activity) {
        this.activity = activity;
    }

    private void create() {
        if (dialog != null) return;
        dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.preloader_dialog);
        boolean isDarkMode = PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("dark_mode", false);
        ThemeAssetPainter.apply(activity, dialog.getWindow() != null ? dialog.getWindow().getDecorView() : dialog.findViewById(android.R.id.content), isDarkMode);
        TextView textView = dialog.findViewById(R.id.TextView);
        if (textView != null) {
            textView.setTextColor(ContextCompat.getColor(
                    activity,
                    isDarkMode ? R.color.surface_body_text_dark : R.color.surface_body_text
            ));
            baseText = textView.getText();
        }

        Window window = dialog.getWindow();
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
    }

    public synchronized void show(int textResId) {
        if (isShowing()) return;
        close();
        if (dialog == null) create();
        TextView textView = dialog.findViewById(R.id.TextView);
        if (textView != null) {
            textView.setText(textResId);
            baseText = textView.getText();
        }
        dialog.show();
    }

    public synchronized void setProgress(int progress) {
        if (dialog == null) return;
        TextView textView = dialog.findViewById(R.id.TextView);
        if (textView == null) return;
        int boundedProgress = Math.max(0, Math.min(100, progress));
        CharSequence message = baseText != null && baseText.length() > 0 ? baseText : textView.getText();
        textView.setText(message + " " + boundedProgress + "%");
    }

    public void showOnUiThread(final int textResId) {
        activity.runOnUiThread(() -> show(textResId));
    }

    public synchronized void close() {
        try {
            if (dialog != null) {
                dialog.dismiss();
            }
        }
        catch (Exception e) {}
    }

    public void closeOnUiThread() {
        activity.runOnUiThread(this::close);
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }
}
