package com.winlator.cmod.contentdialog;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.winlator.cmod.R;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.Callback;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.StringUtils;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

public class StorageInfoDialog extends ContentDialog {
    public StorageInfoDialog(@NonNull Activity activity, Container container) {
        super(activity, R.layout.container_storage_info_dialog);

        setTitle(R.string.storage_info);
        setIcon(R.drawable.ae_icon_info);

        AtomicLong driveCSize = new AtomicLong();
        driveCSize.set(0);

        AtomicLong cacheSize = new AtomicLong();
        cacheSize.set(0);

        AtomicLong totalSize = new AtomicLong();
        totalSize.set(0);

        final TextView tvDriveCSize = findViewById(R.id.TVDriveCSize);
        final TextView tvCacheSize = findViewById(R.id.TVCacheSize);
        final TextView tvTotalSize = findViewById(R.id.TVTotalSize);
        final TextView tvUsedSpace = findViewById(R.id.TVUsedSpace);
        final CircularProgressIndicator circularProgressIndicator = findViewById(R.id.CircularProgressIndicator);

        final long internalStorageSize = FileUtils.getInternalStorageSize();

        Runnable updateUI = () -> {
            tvDriveCSize.setText(StringUtils.formatBytes(driveCSize.get()));
            tvCacheSize.setText(StringUtils.formatBytes(cacheSize.get()));
            tvTotalSize.setText(StringUtils.formatBytes(totalSize.get()));

            int progress = internalStorageSize > 0
                    ? Math.min(100, Math.round((totalSize.get() * 100f) / (float) internalStorageSize))
                    : 0;
            tvUsedSpace.setText(progress + "%");
            circularProgressIndicator.setProgress(progress, true);
        };

        File rootDir = container.getRootDir();
        final File driveCDir = new File(rootDir, ".wine/drive_c");
        final File cacheDir = new File(rootDir, ".cache");
        AtomicLong lastTime = new AtomicLong(System.currentTimeMillis());




        final Callback<Long> onAddSize = (size) -> {
            totalSize.addAndGet(size);
            long currTime = System.currentTimeMillis();
            int elapsedTime = (int)(currTime - lastTime.get());
            if (elapsedTime > 30) {
                activity.runOnUiThread(updateUI);
                lastTime.set(currTime);
            }
        };

        FileUtils.getSizeAsync(driveCDir, (size) -> {
            driveCSize.addAndGet(size);
            onAddSize.call(size);
        });

        FileUtils.getSizeAsync(cacheDir, (size) -> {
            cacheSize.addAndGet(size);
            onAddSize.call(size);
        });

        updateUI.run();

        Button cancelButton = findViewById(R.id.BTCancel);
        if (cancelButton != null) {
            cancelButton.setText(R.string.clear_cache);
            cancelButton.setBackgroundResource(R.drawable.surface_runtime_button_neutral);
            cancelButton.setTextColor(activity.getColor(R.color.surface_runtime_button_text));
        }
        setOnCancelCallback(() -> {
            FileUtils.clear(cacheDir);

            container.putExtra("desktopTheme", null);
            container.saveData();
        });
    }

    @Override
    public void show() {
        super.show();
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(0));
            getWindow().setLayout(
                    Math.round(AppUtils.getScreenWidth() * 0.988f),
                    Math.round(AppUtils.getScreenHeight() * 0.842f)
            );
        }
        ViewGroup.LayoutParams params = getContentView().getLayoutParams();
        if (params != null) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = Math.round(AppUtils.getScreenHeight() * 0.786f);
            getContentView().setLayoutParams(params);
        }
        getContentView().setMinimumHeight(Math.round(AppUtils.getScreenHeight() * 0.786f));
        applyRuntimeChrome();
    }

    private void applyRuntimeChrome() {
        int horizontalPadding = Math.round(getContext().getResources().getDisplayMetrics().density * 5f);
        int topPadding = Math.round(getContext().getResources().getDisplayMetrics().density * 4f);
        View root = getContentView();
        if (root != null) {
            root.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
            root.setPadding(horizontalPadding, topPadding, horizontalPadding, topPadding);
        }
        View frameLayout = findViewById(R.id.FrameLayout);
        if (frameLayout != null) frameLayout.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
        View titleBar = findViewById(R.id.LLTitleBar);
        if (titleBar != null) {
            titleBar.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
            titleBar.setPadding(horizontalPadding, topPadding, horizontalPadding, 0);
        }
        View bottomBar = findViewById(R.id.LLBottomBar);
        if (bottomBar != null) bottomBar.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
        TextView titleView = findViewById(R.id.TVTitle);
        if (titleView != null) titleView.setTextColor(ContextCompat.getColor(getContext(), R.color.surface_runtime_taskmgr_text));
        TextView messageView = findViewById(R.id.TVMessage);
        if (messageView != null) messageView.setTextColor(ContextCompat.getColor(getContext(), R.color.surface_runtime_taskmgr_muted));
        View iconView = findViewById(R.id.IVIcon);
        if (iconView instanceof android.widget.ImageView) {
            ((android.widget.ImageView) iconView).setColorFilter(ContextCompat.getColor(getContext(), R.color.surface_runtime_taskmgr_text));
        }
        View titleBackButton = findViewById(R.id.BTTitleBack);
        if (titleBackButton instanceof android.widget.ImageButton) {
            ((android.widget.ImageButton) titleBackButton).setColorFilter(ContextCompat.getColor(getContext(), R.color.surface_runtime_taskmgr_text));
            titleBackButton.setBackgroundResource(R.drawable.surface_runtime_button_neutral);
        }
        Button confirmButton = findViewById(R.id.BTConfirm);
        if (confirmButton != null) {
            confirmButton.setBackgroundResource(R.drawable.surface_runtime_button_positive);
            confirmButton.setTextColor(ContextCompat.getColor(getContext(), R.color.surface_runtime_button_positive_text));
        }
    }
}
