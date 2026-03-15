package com.winlator.cmod.contentdialog;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.winlator.cmod.R;
import com.winlator.cmod.inputcontrols.ControllerManager;

public class NavigationDialog extends ContentDialog {
    public static final int ACTION_KEYBOARD = 1;
    public static final int ACTION_INPUT_CONTROLS = 2;
    public static final int ACTION_EXIT_GAME = 3;
    public static final int ACTION_EDIT_CONTROLS = 4;
    public static final int ACTION_EDIT_PHYSICAL_CONTROLLER = 5;

    public interface NavigationListener {
        void onNavigationItemSelected(int itemId);
    }

    public NavigationDialog(@NonNull Context context, NavigationListener listener) {
        super(context, R.layout.navigation_dialog);
        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(R.drawable.navigation_dialog_background);
        }

        findViewById(R.id.LLTitleBar).setVisibility(View.GONE);
        findViewById(R.id.LLBottomBar).setVisibility(View.GONE);

        GridLayout grid = findViewById(R.id.main_menu_grid);
        int orientation = context.getResources().getConfiguration().orientation;
        grid.setColumnCount(orientation == Configuration.ORIENTATION_LANDSCAPE ? 5 : 2);

        ControllerManager controllerManager = ControllerManager.getInstance();
        controllerManager.scanForDevices();
        boolean hasPhysicalController = !controllerManager.getDetectedDevices().isEmpty();

        addMenuItem(context, grid, R.drawable.ae_icon_keyboard, R.string.keyboard, ACTION_KEYBOARD, listener, 1.0f);
        addMenuItem(context, grid, R.drawable.ae_icon_menu, R.string.input_controls, ACTION_INPUT_CONTROLS, listener, 1.0f);
        addMenuItem(context, grid, R.drawable.ae_icon_edit, R.string.edit_controls, ACTION_EDIT_CONTROLS, listener, 1.0f);
        if (hasPhysicalController) {
            addMenuItem(context, grid, R.drawable.ae_icon_gamepad, R.string.edit_physical_controller, ACTION_EDIT_PHYSICAL_CONTROLLER, listener, 1.0f);
        }
        addMenuItem(context, grid, R.drawable.ae_icon_remove, R.string.exit_game, ACTION_EXIT_GAME, listener, 1.0f);
    }

    private void addMenuItem(Context context, GridLayout grid, int iconRes, int titleRes, int itemId, NavigationListener listener, float alpha) {
        boolean isDarkMode = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("dark_mode", false);
        int textColor = ContextCompat.getColor(
                context,
                isDarkMode ? R.color.surface_badge_text_dark : R.color.surface_badge_text
        );

        int padding = dpToPx(6, context);
        LinearLayout layout = new LinearLayout(context);
        layout.setPadding(padding, padding, padding, padding);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setOnClickListener(view -> {
            listener.onNavigationItemSelected(itemId);
            dismiss();
        });

        int size = dpToPx(40, context);
        View icon = new View(context);
        icon.setBackground(AppCompatResources.getDrawable(context, iconRes));
        if (icon.getBackground() != null) {
            icon.getBackground().setTint(textColor);
        }
        icon.setAlpha(alpha);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(size, size);
        iconParams.gravity = Gravity.CENTER_HORIZONTAL;
        icon.setLayoutParams(iconParams);
        layout.addView(icon);

        int width = dpToPx(104, context);
        TextView text = new TextView(context);
        text.setLayoutParams(new ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT));
        text.setText(titleRes);
        text.setGravity(Gravity.CENTER);
        text.setLines(2);
        text.setTextColor(textColor);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setAlpha(alpha);
        layout.addView(text);

        grid.addView(layout);
    }

    private int dpToPx(float dp, Context context) {
        return (int) (dp * context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }
}
