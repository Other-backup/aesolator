package com.winlator.cmod.contentdialog;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.CompoundButtonCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.cmod.R;
import com.winlator.cmod.XServerDisplayActivity;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.Callback;
import com.winlator.cmod.core.SpinnerAdapters;
import com.winlator.cmod.core.ThemeAssetPainter;
import com.winlator.cmod.core.UiLifecycleGuard;
import com.winlator.cmod.widget.MultiSelectionComboBox;

import java.util.ArrayList;

public class ContentDialog extends Dialog {
    public Runnable onConfirmCallback;
    private Runnable onCancelCallback;
    private final View contentView;
    private final boolean runtimeSurfaceMode;

    private boolean isDarkMode;

    public ContentDialog(@NonNull Context context) {
        this(context, 0);
    }

    private View inflatedLayout;

    public ContentDialog(@NonNull Context context, int layoutResId) {
        super(context, R.style.ContentDialog);
        contentView = LayoutInflater.from(context).inflate(R.layout.content_dialog, null);
        runtimeSurfaceMode = context instanceof XServerDisplayActivity;


        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        isDarkMode = sharedPreferences.getBoolean("dark_mode", false);

        if (isDarkMode) {
            this.getContext().setTheme(R.style.ContentDialog_Dark);
        }


        if (layoutResId > 0) {
            FrameLayout frameLayout = contentView.findViewById(R.id.FrameLayout);
            frameLayout.setVisibility(View.VISIBLE);
            View view = LayoutInflater.from(context).inflate(layoutResId, frameLayout, false);
            inflatedLayout = view;
            frameLayout.addView(view);
        }

        LinearLayout titleBar = contentView.findViewById(R.id.LLTitleBar);
        titleBar.setBackgroundResource(isDarkMode
                ? R.drawable.surface_card_background_dark
                : R.drawable.surface_card_background);

        View confirmButton = contentView.findViewById(R.id.BTConfirm);
        confirmButton.setOnClickListener((v) -> {
            if (onConfirmCallback != null) onConfirmCallback.run();
            dismiss();
        });

        View cancelButton = contentView.findViewById(R.id.BTCancel);
        cancelButton.setOnClickListener((v) -> {
            if (onCancelCallback != null) onCancelCallback.run();
            dismiss();
        });

        View titleBackButton = contentView.findViewById(R.id.BTTitleBack);
        titleBackButton.setOnClickListener((v) -> {
            if (onCancelCallback != null) onCancelCallback.run();
            dismiss();
        });

        setContentView(contentView);
        ThemeAssetPainter.apply(context, contentView, isDarkMode);
        if (runtimeSurfaceMode) applyRuntimeSurfaceStyle();
    }

    public View getInflatedLayout() {
        return inflatedLayout;
    }

    public View getContentView() {
        return contentView;
    }

    public void setOnConfirmCallback(Runnable onConfirmCallback) {
        this.onConfirmCallback = onConfirmCallback;
    }

    public void setOnCancelCallback(Runnable onCancelCallback) {
        this.onCancelCallback = onCancelCallback;
    }

    @Override
    public void show() {
        if (!UiLifecycleGuard.canShowDialog(getContext(), "ContentDialog", "show")) {
            return;
        }
        super.show();
        if (runtimeSurfaceMode) applyRuntimeSurfaceStyle();
        View dialogSurface = inflatedLayout != null ? inflatedLayout : contentView;
        boolean scrollableSurface = containsWideScrollableSurface(dialogSurface);
        boolean wideScrollableRuntimeSurface = runtimeSurfaceMode && scrollableSurface;
        int horizontalMargin = dp(16);
        int verticalMargin = dp(20);
        int preferredWidth = wideScrollableRuntimeSurface
                ? AppUtils.getPreferredWideDialogWidth(getContext())
                : AppUtils.getPreferredDialogWidth(getContext());
        int safeWidth = Math.min(preferredWidth, AppUtils.getScreenWidth() - horizontalMargin * 2);
        int safeDialogHeight = Math.min(
                Math.round(AppUtils.getScreenHeight() * (runtimeSurfaceMode ? 0.88f : 0.90f)),
                AppUtils.getScreenHeight() - verticalMargin * 2
        );
        if (getWindow() != null) {
            getWindow().setLayout(
                    safeWidth,
                    scrollableSurface ? safeDialogHeight : WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
        ViewGroup.LayoutParams rootParams = contentView.getLayoutParams();
        if (rootParams != null) {
            rootParams.height = scrollableSurface
                    ? Math.max(0, safeDialogHeight)
                    : WindowManager.LayoutParams.WRAP_CONTENT;
            contentView.setLayoutParams(rootParams);
        }

        View frameLayout = contentView.findViewById(R.id.FrameLayout);
        if (frameLayout != null) {
            ViewGroup.LayoutParams params = frameLayout.getLayoutParams();
            if (params instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) params;
                layoutParams.height = scrollableSurface ? 0 : WindowManager.LayoutParams.WRAP_CONTENT;
                layoutParams.weight = scrollableSurface ? 1.0f : 0.0f;
                frameLayout.setLayoutParams(layoutParams);
            }
        }

        View listView = contentView.findViewById(R.id.ListView);
        if (listView != null) {
            ViewGroup.LayoutParams params = listView.getLayoutParams();
            if (params instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) params;
                layoutParams.height = scrollableSurface ? 0 : WindowManager.LayoutParams.WRAP_CONTENT;
                layoutParams.weight = scrollableSurface ? 1.0f : 0.0f;
                listView.setLayoutParams(layoutParams);
            }
        }

        contentView.setMinimumHeight(0);
    }

    @Override
    public void setTitle(int titleResId) {
        setTitle(getContext().getString(titleResId));
    }

    public void setIcon(int iconResId) {
        ImageView imageView = findViewById(R.id.IVIcon);
        imageView.setImageResource(iconResId);
        imageView.setVisibility(View.VISIBLE);
    }

    public void setTitle(String title) {
        LinearLayout titleBar = findViewById(R.id.LLTitleBar);
        TextView tvTitle = findViewById(R.id.TVTitle);
        View titleBackButton = findViewById(R.id.BTTitleBack);

        if (title != null && !title.isEmpty()) {
            tvTitle.setText(title);
            titleBar.setVisibility(View.VISIBLE);
            if (titleBackButton != null) titleBackButton.setVisibility(View.VISIBLE);
        }
        else {
            tvTitle.setText("");
            titleBar.setVisibility(View.GONE);
            if (titleBackButton != null) titleBackButton.setVisibility(View.GONE);
        }
    }

    public void setBottomBarText(String bottomBarText) {
        TextView tvBottomBarText = findViewById(R.id.TVBottomBarText);

        if (bottomBarText != null && !bottomBarText.isEmpty()) {
            tvBottomBarText.setText(bottomBarText);
            tvBottomBarText.setVisibility(View.VISIBLE);
        }
        else {
            tvBottomBarText.setText("");
            tvBottomBarText.setVisibility(View.GONE);
        }
    }

    public void setMessage(int msgResId) {
        setMessage(getContext().getString(msgResId));
    }

    public void setMessage(String message) {
        TextView tvMessage = findViewById(R.id.TVMessage);

        if (message != null && !message.isEmpty()) {
            tvMessage.setText(message);
            tvMessage.setVisibility(View.VISIBLE);
        }
        else {
            tvMessage.setText("");
            tvMessage.setVisibility(View.GONE);
        }
    }

    public static void alert(Context context, int msgResId, Runnable callback) {
        ContentDialog dialog = new ContentDialog(context);
        dialog.setMessage(msgResId);
        dialog.setOnConfirmCallback(callback);
        dialog.findViewById(R.id.BTCancel).setVisibility(View.GONE);
        dialog.show();
    }

    public static void alert(Context context, String msg, Runnable callback) {
        ContentDialog dialog = new ContentDialog(context);
        dialog.setMessage(msg);
        dialog.setOnConfirmCallback(callback);
        dialog.findViewById(R.id.BTCancel).setVisibility(View.GONE);
        dialog.show();
    }

    public static void confirm(Context context, int msgResId, Runnable callback) {
        ContentDialog dialog = new ContentDialog(context);
        dialog.setMessage(msgResId);
        dialog.setOnConfirmCallback(callback);
        dialog.show();
    }

    public static void confirm(Context context, String msg, Runnable callback) {
        ContentDialog dialog = new ContentDialog(context);
        dialog.setMessage(msg);
        dialog.setOnConfirmCallback(callback);
        dialog.show();
    }

    public static void prompt(Context context, int titleResId, String defaultText, Callback<String> callback) {
        ContentDialog dialog = new ContentDialog(context);

        final EditText editText = dialog.findViewById(R.id.EditText);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        applyDarkThemeToEditText(editText, isDarkMode);

        editText.setHint(R.string.untitled);
        if (defaultText != null) editText.setText(defaultText);
        editText.setVisibility(View.VISIBLE);

        dialog.setTitle(titleResId);
        dialog.setOnConfirmCallback(() -> {
            String text = editText.getText().toString().trim();
            if (!text.isEmpty()) callback.call(text);
        });

        dialog.show();
    }

    private static void applyDarkThemeToEditText(EditText editText, boolean isDarkMode) {
        int textColor = ContextCompat.getColor(
                editText.getContext(),
                isDarkMode ? R.color.surface_badge_text_dark : R.color.surface_badge_text
        );
        int hintColor = ContextCompat.getColor(
                editText.getContext(),
                isDarkMode ? R.color.surface_body_text_dark : R.color.surface_body_text
        );
        editText.setTextColor(textColor);
        editText.setHintTextColor(hintColor);
        editText.setBackgroundResource(isDarkMode ? R.drawable.edit_text_dark : R.drawable.edit_text);
    }

    public static void showMultipleChoiceList(Context context, int titleResId, final String[] items, Callback<ArrayList<Integer>> callback) {
        ContentDialog dialog = new ContentDialog(context);
        final boolean isDarkMode = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("dark_mode", false);
        final int textColor = ContextCompat.getColor(
                context,
                isDarkMode ? R.color.surface_badge_text_dark : R.color.surface_badge_text
        );

        final ListView listView = dialog.findViewById(R.id.ListView);
        listView.getLayoutParams().width = AppUtils.getPreferredDialogWidth(context);
        listView.setBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setAdapter(new ArrayAdapter<String>(context, R.layout.dialog_choice_item_multiple, items) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull android.view.ViewGroup parent) {
                CheckedTextView checkedTextView = (CheckedTextView) super.getView(position, convertView, parent);
                checkedTextView.setTextColor(textColor);
                return checkedTextView;
            }
        });
        listView.setVisibility(View.VISIBLE);

        dialog.setTitle(titleResId);
        dialog.setOnConfirmCallback(() -> {
            ArrayList<Integer> result = new ArrayList<>();
            SparseBooleanArray checkedItemPositions = listView.getCheckedItemPositions();
            for (int i = 0; i < checkedItemPositions.size(); i++) {
                if (checkedItemPositions.valueAt(i)) result.add(checkedItemPositions.keyAt(i));
            }
            callback.call(result);
        });

        dialog.show();
    }

    public static void showSingleChoiceList(Context context, int titleResId, final String[] items, Callback<Integer> callback) {
        ContentDialog dialog = new ContentDialog(context);
        final boolean isDarkMode = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("dark_mode", false);
        final int textColor = ContextCompat.getColor(
                context,
                isDarkMode ? R.color.surface_badge_text_dark : R.color.surface_badge_text
        );
        dialog.getContentView().findViewById(R.id.BTConfirm).setVisibility(View.GONE);

        final ListView listView = dialog.findViewById(R.id.ListView);
        listView.getLayoutParams().width = AppUtils.getPreferredDialogWidth(context);
        listView.setBackgroundResource(isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
        listView.setChoiceMode(ListView.CHOICE_MODE_NONE);
        listView.setAdapter(new ArrayAdapter<String>(context, R.layout.dialog_choice_item_single, items) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull android.view.ViewGroup parent) {
                CheckedTextView checkedTextView = (CheckedTextView) super.getView(position, convertView, parent);
                checkedTextView.setTextColor(textColor);
                return checkedTextView;
            }
        });
        listView.setVisibility(View.VISIBLE);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            callback.call(position);
            dialog.dismiss();
        });

        dialog.setTitle(titleResId);
        dialog.show();
    }

    private void applyRuntimeSurfaceStyle() {
        int brightText = ContextCompat.getColor(getContext(), R.color.surface_runtime_taskmgr_text);
        int mutedText = ContextCompat.getColor(getContext(), R.color.surface_runtime_taskmgr_muted);

        contentView.setPadding(dp(10), dp(8), dp(10), dp(8));
        contentView.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);

        LinearLayout titleBar = contentView.findViewById(R.id.LLTitleBar);
        if (titleBar != null) {
            titleBar.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
        }

        TextView titleView = contentView.findViewById(R.id.TVTitle);
        if (titleView != null) titleView.setTextColor(brightText);
        TextView messageView = contentView.findViewById(R.id.TVMessage);
        if (messageView != null) messageView.setTextColor(mutedText);
        TextView bottomBarText = contentView.findViewById(R.id.TVBottomBarText);
        if (bottomBarText != null) bottomBarText.setTextColor(brightText);

        ImageView iconView = contentView.findViewById(R.id.IVIcon);
        if (iconView != null) iconView.setColorFilter(brightText);
        ImageView backButton = contentView.findViewById(R.id.BTTitleBack);
        if (backButton != null) {
            backButton.setBackgroundResource(R.drawable.surface_runtime_button_neutral);
            backButton.setColorFilter(brightText);
        }

        styleRuntimeButton(contentView.findViewById(R.id.BTReset), false);
        styleRuntimeButton(contentView.findViewById(R.id.BTCancel), false);
        styleRuntimeButton(contentView.findViewById(R.id.BTConfirm), true);
        restyleRuntimeTaggedViews(contentView, brightText, mutedText);
    }

    private void styleRuntimeButton(View buttonView, boolean positive) {
        if (!(buttonView instanceof TextView)) return;
        buttonView.setBackgroundResource(positive
                ? R.drawable.surface_runtime_button_positive
                : R.drawable.surface_runtime_button_neutral);
        ((TextView) buttonView).setTextColor(ContextCompat.getColor(
                getContext(),
                positive ? R.color.surface_runtime_button_positive_text : R.color.surface_runtime_button_text
        ));
    }

    private void restyleRuntimeTaggedViews(View view, int brightText, int mutedText) {
        if (view == null) return;
        if (hasTagToken(view, "theme_card")) {
            view.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
        } else if (hasTagToken(view, "theme_badge")) {
            view.setBackgroundResource(R.drawable.surface_runtime_taskmgr_badge_background);
            if (view instanceof TextView) {
                ((TextView) view).setTextColor(brightText);
            }
        }

        if (view instanceof Spinner) {
            SpinnerAdapters.applyRuntimeSurface((Spinner) view);
        } else if (view instanceof MultiSelectionComboBox) {
            SpinnerAdapters.applyRuntimeSurface((MultiSelectionComboBox) view);
        } else if (view instanceof EditText) {
            EditText editText = (EditText) view;
            editText.setTextColor(brightText);
            editText.setHintTextColor(mutedText);
            editText.setBackgroundResource(R.drawable.surface_runtime_taskmgr_input_background);
        } else if (view instanceof SwitchCompat) {
            SwitchCompat switchCompat = (SwitchCompat) view;
            switchCompat.setTextColor(brightText);
            int[][] states = new int[][] {
                    new int[] {android.R.attr.state_checked},
                    new int[] {-android.R.attr.state_checked}
            };
            int[] colors = new int[] {
                    ContextCompat.getColor(getContext(), R.color.surface_toggle_on_dark),
                    ContextCompat.getColor(getContext(), R.color.surface_toggle_off_dark)
            };
            android.content.res.ColorStateList tint = new android.content.res.ColorStateList(states, colors);
            switchCompat.setThumbTintList(tint);
            switchCompat.setTrackTintList(tint);
        } else if (view instanceof Button) {
            Button button = (Button) view;
            int id = view.getId();
            if (id != R.id.BTConfirm && id != R.id.BTCancel && id != R.id.BTReset) {
                button.setBackgroundResource(R.drawable.surface_runtime_button_neutral);
                button.setTextColor(ContextCompat.getColor(getContext(), R.color.surface_runtime_button_text));
            }
        } else if (view instanceof TextView) {
            TextView textView = (TextView) view;
            if (hasTagToken(view, "theme_hint")) {
                textView.setTextColor(mutedText);
            } else if (hasTagToken(view, "theme_text")) {
                textView.setTextColor(brightText);
            } else if (view.getId() == R.id.TVMessage) {
                textView.setTextColor(mutedText);
            } else if (view.getId() == R.id.TVTitle || view.getId() == R.id.TVBottomBarText) {
                textView.setTextColor(brightText);
            }
        } else if (view instanceof ImageButton) {
            ImageButton imageButton = (ImageButton) view;
            imageButton.setBackgroundResource(R.drawable.surface_runtime_button_neutral);
            imageButton.setColorFilter(brightText);
        } else if (view instanceof CheckBox) {
            CheckBox checkBox = (CheckBox) view;
            checkBox.setTextColor(brightText);
            CompoundButtonCompat.setButtonTintList(
                    checkBox,
                    android.content.res.ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.surface_runtime_taskmgr_border))
            );
        } else if (view instanceof ImageView) {
            if (view.getId() != R.id.IVIcon && view.getId() != R.id.BTTitleBack) {
                ((ImageView) view).setColorFilter(brightText);
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                restyleRuntimeTaggedViews(group.getChildAt(i), brightText, mutedText);
            }
        }
    }

    private boolean hasTagToken(View view, String token) {
        Object tag = view.getTag();
        if (!(tag instanceof String)) return false;
        String[] tokens = ((String) tag).trim().split("[\\s,;|]+");
        for (String candidate : tokens) {
            if (token.equals(candidate)) return true;
        }
        return false;
    }

    private boolean containsWideScrollableSurface(View view) {
        if (view == null) return false;
        if (view.getVisibility() == View.GONE) return false;
        if (view instanceof RecyclerView
                || view instanceof ScrollView
                || view instanceof HorizontalScrollView
                || view instanceof ListView) {
            return true;
        }
        if (hasTagToken(view, "compact_runtime_dialog")) return false;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (containsWideScrollableSurface(group.getChildAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getContext().getResources().getDisplayMetrics()
        ));
    }
}
