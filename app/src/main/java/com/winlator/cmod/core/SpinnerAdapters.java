package com.winlator.cmod.core;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.winlator.cmod.R;

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
        final int textColor = isDarkMode ? Color.WHITE : Color.BLACK;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.spinner_item_compact, new ArrayList<>(values)) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                styleText(view, textColor);
                return view;
            }

            @NonNull
            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                styleText(view, textColor);
                return view;
            }
        };
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_compact);
        return adapter;
    }

    private static void styleText(View view, int textColor) {
        if (!(view instanceof TextView)) return;
        TextView textView = (TextView) view;
        textView.setTextColor(textColor);
        textView.setSingleLine(false);
        textView.setEllipsize(TextUtils.TruncateAt.END);
    }
}
