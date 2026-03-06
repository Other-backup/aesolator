package com.winlator.cmod.core;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.core.widget.TextViewCompat;

import com.winlator.cmod.R;

import java.util.Locale;

public final class ThemeAssetPainter {
    private ThemeAssetPainter() {
    }

    public static void apply(Context context, View root, boolean isDarkMode) {
        if (context == null || root == null) return;
        int buttonTint = ContextCompat.getColor(context, isDarkMode ? R.color.colorAccentDark : R.color.colorAccent);
        int iconTint = ContextCompat.getColor(context, isDarkMode ? R.color.forensic_badge_text_dark : R.color.colorPrimaryDark);
        int buttonDrawableTint = ContextCompat.getColor(context, R.color.white);
        traverse(root, buttonTint, iconTint, buttonDrawableTint);
    }

    private static void traverse(View view, int buttonTint, int iconTint, int buttonDrawableTint) {
        if (view instanceof ImageButton) {
            tint((ImageButton) view, buttonTint);
        } else if (view instanceof ImageView) {
            ImageView imageView = (ImageView) view;
            if (shouldTintImageView(imageView)) tint(imageView, iconTint);
        } else if (view instanceof TextView) {
            TextView textView = (TextView) view;
            int tintColor = (textView instanceof Button) ? buttonDrawableTint : iconTint;
            tintCompoundDrawables(textView, tintColor);
        }

        if (!(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            traverse(group.getChildAt(i), buttonTint, iconTint, buttonDrawableTint);
        }
    }

    private static boolean shouldTintImageView(ImageView imageView) {
        Object tag = imageView.getTag();
        if (tag != null && "no_theme_tint".equalsIgnoreCase(String.valueOf(tag))) return false;

        int viewId = imageView.getId();
        if (viewId == View.NO_ID) return false;

        String idName = "";
        try {
            idName = imageView.getResources().getResourceEntryName(viewId).toLowerCase(Locale.US);
        } catch (Exception ignored) {
        }
        if (idName.isEmpty()) return false;

        if (idName.contains("coverart")
                || idName.contains("wallpaper")
                || idName.contains("preview")
                || idName.contains("banner")
                || idName.contains("screenshot")
                || idName.contains("logo")
                || idName.contains("avatar")
                || idName.contains("thumbnail")
                || idName.contains("thumb")
                || idName.equals("imageview")) {
            return false;
        }

        if (idName.contains("icon")
                || idName.startsWith("bt")
                || idName.contains("help")
                || idName.contains("menu")
                || idName.contains("remove")
                || idName.contains("add")
                || idName.contains("settings")) {
            return true;
        }

        Drawable drawable = imageView.getDrawable();
        if (drawable == null) return false;
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        return width > 0 && height > 0 && width <= 96 && height <= 96;
    }

    private static void tint(ImageView imageView, int color) {
        ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(color));
        ImageViewCompat.setImageTintMode(imageView, PorterDuff.Mode.SRC_IN);
    }

    private static void tintCompoundDrawables(TextView textView, int color) {
        Drawable[] drawables = TextViewCompat.getCompoundDrawablesRelative(textView);
        boolean hasDrawable = false;
        for (Drawable drawable : drawables) {
            if (drawable != null) {
                hasDrawable = true;
                break;
            }
        }
        if (!hasDrawable) return;

        TextViewCompat.setCompoundDrawableTintList(textView, ColorStateList.valueOf(color));
        TextViewCompat.setCompoundDrawableTintMode(textView, PorterDuff.Mode.SRC_IN);
    }
}
