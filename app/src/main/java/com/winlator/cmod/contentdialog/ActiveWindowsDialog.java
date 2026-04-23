package com.winlator.cmod.contentdialog;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.winlator.cmod.R;
import com.winlator.cmod.XServerDisplayActivity;
import com.winlator.cmod.xserver.Window;
import com.winlator.cmod.xserver.XLock;
import com.winlator.cmod.xserver.XServer;

import java.util.ArrayList;
import java.util.Locale;

public class ActiveWindowsDialog extends ContentDialog {
    private final XServerDisplayActivity activity;

    public ActiveWindowsDialog(XServerDisplayActivity activity) {
        super(activity, R.layout.active_windows_dialog);
        this.activity = activity;
        setTitle(R.string.active_windows);
        setIcon(R.drawable.ae_icon_front);

        View confirmButton = findViewById(R.id.BTConfirm);
        if (confirmButton != null) confirmButton.setVisibility(View.GONE);
        TextView cancelButton = findViewById(R.id.BTCancel);
        if (cancelButton != null) cancelButton.setText(R.string.close);

        loadWindowViews(collectActiveWindows());
    }

    private void loadWindowViews(ArrayList<Window> windows) {
        LinearLayout windowList = findViewById(R.id.LLWindowList);
        TextView emptyText = findViewById(R.id.TVEmptyText);
        if (windowList == null || emptyText == null) return;

        windowList.removeAllViews();
        if (windows.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            return;
        }

        emptyText.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(activity);
        XServer xServer = activity.getXServer();
        for (int index = windows.size() - 1; index >= 0; index--) {
            final Window window = windows.get(index);
            final String title = safeWindowTitle(window);
            final String className = safeClassName(window);

            View itemView = inflater.inflate(R.layout.active_window_list_item, windowList, false);
            ImageView iconView = itemView.findViewById(R.id.IVIcon);
            TextView titleView = itemView.findViewById(R.id.TVName);
            TextView classView = itemView.findViewById(R.id.TVClass);
            TextView metaView = itemView.findViewById(R.id.TVMeta);

            if (titleView != null) titleView.setText(title);
            if (classView != null) classView.setText(className);
            if (metaView != null) {
                metaView.setText(activity.getString(
                        R.string.active_windows_meta,
                        window.getProcessId(),
                        String.format(Locale.US, "0x%x", window.getHandle()),
                        (int) window.getWidth(),
                        (int) window.getHeight()
                ));
            }
            if (iconView != null) {
                iconView.setImageResource(R.drawable.taskmgr_process);
                if (xServer != null && xServer.pixmapManager != null) {
                    Bitmap icon = xServer.pixmapManager.getWindowIcon(window);
                    if (icon != null) iconView.setImageBitmap(icon);
                }
            }

            itemView.setOnClickListener(v -> {
                String targetName = !className.isEmpty() ? className : title;
                activity.getWinHandler().bringToFront(targetName, window.getHandle());
                dismiss();
            });
            windowList.addView(itemView);
        }
    }

    private ArrayList<Window> collectActiveWindows() {
        ArrayList<Window> result = new ArrayList<>();
        XServer xServer = activity.getXServer();
        if (xServer == null || xServer.windowManager == null) return result;

        XLock lock = xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.DRAWABLE_MANAGER);
        try {
            collectActiveWindows(xServer.windowManager.rootWindow, xServer.windowManager.rootWindow, result);
            return result;
        } finally {
            if (lock != null) lock.close();
        }
    }

    private void collectActiveWindows(Window window, Window rootWindow, ArrayList<Window> result) {
        if (window == null) return;
        if (window != rootWindow && window.isTrackedVisualWindow(rootWindow)) {
            result.add(window);
        }
        for (Window child : window.getChildren()) {
            collectActiveWindows(child, rootWindow, result);
        }
    }

    private String safeWindowTitle(Window window) {
        String title = trimToEmpty(window == null ? "" : window.getName());
        if (!title.isEmpty()) return title;
        String className = safeClassName(window);
        if (!className.isEmpty()) return className;
        return activity.getString(R.string.not_set);
    }

    private String safeClassName(Window window) {
        return trimToEmpty(window == null ? "" : window.getClassName());
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
