package com.winlator.cmod.core;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.winlator.cmod.R;
import com.winlator.cmod.widget.MultiSelectionComboBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SpinnerAdapters {
    private SpinnerAdapters() {
    }

    public static ArrayAdapter<String> create(Context context, boolean isDarkMode, String[] values) {
        return create(context, isDarkMode, values == null ? new ArrayList<>() : Arrays.asList(values));
    }

    public static ArrayAdapter<String> create(Context context, boolean isDarkMode, List<String> values) {
        final int textColor = resolvePrimaryTextColor(context, isDarkMode);
        final int dropdownBackground = resolveThemeResource(
                context,
                R.attr.aeListChoiceBackground,
                isDarkMode ? R.drawable.list_selector_dark_accent : R.drawable.list_selector_light_accent
        );
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.spinner_item_compact, new ArrayList<>(values)) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                styleText(view, textColor, true);
                return view;
            }

            @NonNull
            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                styleText(view, textColor, false);
                view.setBackgroundResource(dropdownBackground);
                return view;
            }
        };
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_compact);
        return adapter;
    }

    public static ArrayAdapter<String> create(Context context, List<String> values) {
        return create(context, isDarkMode(context), values);
    }

    public static ArrayAdapter<String> create(Context context, String[] values) {
        return create(context, isDarkMode(context), values);
    }

    public static <T> ArrayAdapter<T> createGeneric(Context context, boolean isDarkMode, List<T> values) {
        final int textColor = resolvePrimaryTextColor(context, isDarkMode);
        final int dropdownBackground = resolveThemeResource(
                context,
                R.attr.aeListChoiceBackground,
                isDarkMode ? R.drawable.list_selector_dark_accent : R.drawable.list_selector_light_accent
        );
        ArrayAdapter<T> adapter = new ArrayAdapter<>(context, R.layout.spinner_item_compact, new ArrayList<>(values)) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                styleText(view, textColor, true);
                return view;
            }

            @NonNull
            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                styleText(view, textColor, false);
                view.setBackgroundResource(dropdownBackground);
                return view;
            }
        };
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_compact);
        return adapter;
    }

    public static <T> ArrayAdapter<T> createGeneric(Context context, List<T> values) {
        return createGeneric(context, isDarkMode(context), values);
    }

    public static <T> ArrayAdapter<T> createGeneric(Context context, boolean isDarkMode, T[] values) {
        return createGeneric(context, isDarkMode, values == null ? new ArrayList<>() : Arrays.asList(values));
    }

    public static <T> ArrayAdapter<T> createGeneric(Context context, T[] values) {
        return createGeneric(context, isDarkMode(context), values);
    }

    public static boolean isDarkMode(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("dark_mode", false);
    }

    public static void applySurface(Spinner spinner, boolean isDarkMode) {
        if (spinner == null) return;
        spinner.setBackgroundResource(resolveThemeResource(
                spinner.getContext(),
                R.attr.aeComboBoxBackground,
                isDarkMode ? R.drawable.combo_box_dark : R.drawable.combo_box
        ));
        spinner.setPopupBackgroundResource(resolveThemeResource(
                spinner.getContext(),
                R.attr.aePopupBackground,
                isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background
        ));
    }

    public static void applySurface(MultiSelectionComboBox comboBox, boolean isDarkMode) {
        if (comboBox == null) return;
        comboBox.setBackgroundResource(isDarkMode ? R.drawable.combo_box_dark : R.drawable.combo_box);
        comboBox.setTextColor(resolvePrimaryTextColor(comboBox.getContext(), isDarkMode));
        comboBox.setSingleLine(true);
        comboBox.setMaxLines(1);
        comboBox.setHorizontallyScrolling(true);
        comboBox.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        comboBox.setMarqueeRepeatLimit(-1);
        comboBox.setSelected(true);
        comboBox.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null);
    }

    public static void applySurface(Spinner spinner) {
        if (spinner == null) return;
        applySurface(spinner, isDarkMode(spinner.getContext()));
    }

    public static void applySurfaceRecursively(View view, boolean isDarkMode) {
        if (view == null) return;
        if (view instanceof Spinner spinner) {
            applySurface(spinner, isDarkMode);
        } else if (view instanceof MultiSelectionComboBox comboBox) {
            applySurface(comboBox, isDarkMode);
        }
        if (!(view instanceof ViewGroup group)) return;
        for (int i = 0; i < group.getChildCount(); i++) {
            applySurfaceRecursively(group.getChildAt(i), isDarkMode);
        }
    }

    private static void styleText(View view, int textColor, boolean marquee) {
        if (!(view instanceof TextView)) return;
        TextView textView = (TextView) view;
        textView.setTextColor(textColor);
        textView.setSingleLine(true);
        textView.setMaxLines(1);
        textView.setHorizontallyScrolling(true);
        textView.setEllipsize(marquee ? TextUtils.TruncateAt.MARQUEE : TextUtils.TruncateAt.END);
        textView.setMarqueeRepeatLimit(-1);
        textView.setSelected(marquee);
    }

    private static int resolvePrimaryTextColor(Context context, boolean isDarkMode) {
        if (context == null) return isDarkMode ? Color.WHITE : Color.BLACK;
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(R.attr.aePrimaryTextColor, typedValue, true)) {
            if (typedValue.resourceId != 0) return ContextCompat.getColor(context, typedValue.resourceId);
            if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return typedValue.data;
            }
        }
        return ContextCompat.getColor(context, isDarkMode ? R.color.surface_body_text_dark : R.color.surface_body_text);
    }

    private static int resolveThemeResource(Context context, int attrRes, int fallbackRes) {
        if (context == null) return fallbackRes;
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(attrRes, typedValue, true) && typedValue.resourceId != 0) {
            return typedValue.resourceId;
        }
        return fallbackRes;
    }
}
