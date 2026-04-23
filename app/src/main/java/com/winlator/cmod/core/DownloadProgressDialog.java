package com.winlator.cmod.core;

import android.app.Activity;
import android.app.Dialog;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.winlator.cmod.R;
import com.winlator.cmod.math.Mathf;

public class DownloadProgressDialog {
    private final Activity activity;
    private Dialog dialog;

    public DownloadProgressDialog(Activity activity) {
        this.activity = activity;
    }

    private void create() {
        if (dialog != null) return;
        dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.download_progress_dialog);
        TextView textView = dialog.findViewById(R.id.TextView);
        if (textView != null) {
            textView.setTextColor(ContextCompat.getColor(activity, R.color.surface_runtime_taskmgr_text));
        }
        TextView percentView = dialog.findViewById(R.id.TVProgress);
        if (percentView != null) {
            percentView.setTextColor(ContextCompat.getColor(activity, R.color.surface_runtime_taskmgr_text));
        }
        Button cancelButton = dialog.findViewById(R.id.BTCancel);
        if (cancelButton != null) {
            cancelButton.setBackgroundResource(R.drawable.surface_runtime_button_neutral);
            cancelButton.setTextColor(ContextCompat.getColor(activity, R.color.surface_runtime_button_text));
        }

        Window window = dialog.getWindow();
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
    }

    public void show() {
        show(null);
    }

    public void show(int textResId) {
        show(textResId, null);
    }

    public void show(Runnable onCancelCallback) {
        show(0, onCancelCallback);
    }

    public void show(int textResId, final Runnable onCancelCallback) {
        if (isShowing()) return;
        close();
        if (dialog == null) create();

        if (textResId > 0) ((TextView)dialog.findViewById(R.id.TextView)).setText(textResId);

        setProgress(0);
        dialog.findViewById(R.id.LLBottomBar).setVisibility(View.GONE);
        CircularProgressIndicator circularProgressIndicator = dialog.findViewById(R.id.CircularProgressIndicator);
        if (circularProgressIndicator != null) circularProgressIndicator.setIndeterminate(true);
        LinearProgressIndicator linearProgressIndicator = dialog.findViewById(R.id.LinearProgressIndicator);
        if (linearProgressIndicator != null) linearProgressIndicator.setIndeterminate(true);
        if (onCancelCallback != null) {
            dialog.findViewById(R.id.BTCancel).setOnClickListener((v) -> onCancelCallback.run());
            dialog.findViewById(R.id.LLBottomBar).setVisibility(View.VISIBLE);
        }
        dialog.show();
    }

    public void setProgress(int progress) {
        if (dialog == null) return;
        progress = Mathf.clamp(progress, 0, 100);
        CircularProgressIndicator circularProgressIndicator = dialog.findViewById(R.id.CircularProgressIndicator);
        if (circularProgressIndicator != null) {
            circularProgressIndicator.setIndeterminate(false);
            circularProgressIndicator.setProgressCompat(progress, true);
        }
        LinearProgressIndicator linearProgressIndicator = dialog.findViewById(R.id.LinearProgressIndicator);
        if (linearProgressIndicator != null) {
            linearProgressIndicator.setIndeterminate(false);
            linearProgressIndicator.setProgressCompat(progress, true);
        }
        ((TextView)dialog.findViewById(R.id.TVProgress)).setText(progress+"%");
    }

    public void close() {
        try {
            if (dialog != null) {
                dialog.dismiss();
            }
        }
        catch (Exception e) {
            Log.w("DownloadProgressDialog", "Failed to dismiss download progress dialog", e);
        }
    }

    public void closeOnUiThread() {
        activity.runOnUiThread(this::close);
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }
}
