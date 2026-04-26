package com.winlator.cmod.core;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.winlator.cmod.R;

import java.util.ArrayList;

public class PreloaderDialog {
    private final Activity activity;
    private Dialog dialog;
    private CharSequence baseText = "";
    private AnimatorSet iconAnimator;

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
        TextView textView = dialog.findViewById(R.id.TextView);
        if (textView != null) {
            textView.setTextColor(ContextCompat.getColor(activity, R.color.surface_runtime_taskmgr_text));
            baseText = textView.getText();
        }
        TextView hintView = dialog.findViewById(R.id.TVPreloaderHint);
        if (hintView != null) {
            hintView.setTextColor(ContextCompat.getColor(activity, R.color.surface_runtime_taskmgr_muted));
        }

        Window window = dialog.getWindow();
        if (window != null) {
            // The startup overlay is informational only; it must not steal
            // focus or touch ownership from the desktop host during bootstrap.
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
    }

    public synchronized void show(int textResId) {
        if (isShowing()) return;
        close();
        if (dialog == null) create();
        if (!UiLifecycleGuard.canShowDialog(activity, "PreloaderDialog", "show")) {
            return;
        }
        TextView textView = dialog.findViewById(R.id.TextView);
        if (textView != null) {
            textView.setText(textResId);
            baseText = textView.getText();
        }
        CircularProgressIndicator progressIndicator = dialog.findViewById(R.id.CircularProgressIndicator);
        if (progressIndicator != null) {
            progressIndicator.setIndeterminate(true);
            progressIndicator.setProgressCompat(0, false);
        }
        LinearProgressIndicator linearProgressIndicator = dialog.findViewById(R.id.LinearProgressIndicator);
        if (linearProgressIndicator != null) {
            linearProgressIndicator.setIndeterminate(true);
            linearProgressIndicator.setProgressCompat(0, false);
        }
        startIconAnimation();
        dialog.show();
        refreshWindowState();
    }

    public synchronized void setProgress(int progress) {
        if (dialog == null) return;
        TextView textView = dialog.findViewById(R.id.TextView);
        if (textView == null) return;
        int boundedProgress = Math.max(0, Math.min(100, progress));
        CharSequence message = baseText != null && baseText.length() > 0 ? baseText : textView.getText();
        textView.setText(message + " " + boundedProgress + "%");
        CircularProgressIndicator progressIndicator = dialog.findViewById(R.id.CircularProgressIndicator);
        if (progressIndicator != null) {
            progressIndicator.setIndeterminate(false);
            progressIndicator.setProgressCompat(boundedProgress, true);
        }
        LinearProgressIndicator linearProgressIndicator = dialog.findViewById(R.id.LinearProgressIndicator);
        if (linearProgressIndicator != null) {
            linearProgressIndicator.setIndeterminate(false);
            linearProgressIndicator.setProgressCompat(boundedProgress, true);
        }
    }

    public void showOnUiThread(final int textResId) {
        activity.runOnUiThread(() -> show(textResId));
    }

    public synchronized void close() {
        try {
            if (dialog != null) {
                stopIconAnimation();
                dialog.dismiss();
            }
        }
        catch (Exception e) {
            Log.w("PreloaderDialog", "Failed to dismiss preloader dialog", e);
        }
    }

    public void closeOnUiThread() {
        activity.runOnUiThread(this::close);
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    public synchronized void refreshWindowState() {
        if (dialog == null || !dialog.isShowing() || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;
        Window window = dialog.getWindow();
        if (window == null) return;
        View decorView = window.getDecorView();
        if (decorView == null) return;
        decorView.post(() -> {
            int width = decorView.getWidth();
            int height = decorView.getHeight();
            if (width <= 0 || height <= 0) return;
            ArrayList<Rect> exclusionRects = new ArrayList<>();
            exclusionRects.add(new Rect(0, 0, width, height));
            decorView.setSystemGestureExclusionRects(exclusionRects);
        });
    }

    private void startIconAnimation() {
        if (dialog == null) return;
        ImageView iconView = dialog.findViewById(R.id.IVPreloaderIcon);
        if (iconView == null) return;
        stopIconAnimation();
        iconView.setScaleX(1f);
        iconView.setScaleY(1f);
        iconView.setAlpha(1f);
        iconView.setTranslationX(0f);
        iconView.setTranslationY(0f);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(iconView, View.SCALE_X, 1f, 1.08f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(iconView, View.SCALE_Y, 1f, 1.08f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(iconView, View.ALPHA, 0.9f, 1f, 0.9f);
        float bobOffset = activity.getResources().getDisplayMetrics().density * 1.6f;
        ObjectAnimator translateY = ObjectAnimator.ofFloat(iconView, View.TRANSLATION_Y, 0f, -bobOffset, 0f);
        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        alpha.setRepeatCount(ObjectAnimator.INFINITE);
        translateY.setRepeatCount(ObjectAnimator.INFINITE);
        scaleX.setDuration(920L);
        scaleY.setDuration(920L);
        alpha.setDuration(920L);
        translateY.setDuration(1320L);
        scaleX.setInterpolator(new DecelerateInterpolator());
        scaleY.setInterpolator(new DecelerateInterpolator());
        alpha.setInterpolator(new AccelerateInterpolator());
        translateY.setInterpolator(new AccelerateDecelerateInterpolator());
        iconAnimator = new AnimatorSet();
        iconAnimator.playTogether(scaleX, scaleY, alpha, translateY);
        iconAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        iconAnimator.start();
    }

    private void stopIconAnimation() {
        if (iconAnimator != null) {
            iconAnimator.cancel();
            iconAnimator = null;
        }
    }
}
