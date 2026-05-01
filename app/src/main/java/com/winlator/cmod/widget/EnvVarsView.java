package com.winlator.cmod.widget;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.winlator.cmod.R;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.EnvVars;
import com.winlator.cmod.core.SpinnerAdapters;
import com.winlator.cmod.core.UnitUtils;

import java.util.Arrays;

public class EnvVarsView extends FrameLayout {
    public static final String[][] knownEnvVars = {
        {"ZINK_DESCRIPTORS", "SELECT", "auto", "lazy", "cached", "notemplates"},
        {"ZINK_DEBUG", "SELECT_MULTIPLE", "nir", "spirv", "tgsi", "validation", "sync", "compact", "noreorder"},
        {"MESA_SHADER_CACHE_DISABLE", "CHECKBOX", "false", "true"},
        {"mesa_glthread", "CHECKBOX", "false", "true"},
        {"AERO_WINE_SYNC_BACKEND", "SELECT", "auto", "ntsync", "aesync", "fsync", "esync", "server"},
        {"WINEAESYNC", "CHECKBOX", "0", "1"},
        {"WINEESYNC", "CHECKBOX", "0", "1"},
        {"WINEFSYNC", "CHECKBOX", "0", "1"},
        {"FD_DEV_FEATURES", "SELECT_MULTIPLE", "enable_tp_ubwc_flag_hint=1", "storage_8bit=1"},
        {"TU_DEBUG", "SELECT_MULTIPLE", "forcecb", "nocb", "startup", "deck_emu", "nir", "nobin", "sysmem", "gmem", "forcebin", "layout", "noubwc", "nomultipos", "nolrz", "nolrzfc", "perf", "perfc", "flushall", "syncdraw", "push_consts_per_stage", "rast_order", "unaligned_store", "log_skip_gmem_ops", "dynamic", "bos", "3d_load", "fdm", "noconform", "rd"},
        {"IR3_SHADER_DEBUG", "SELECT_MULTIPLE", "nouboopt", "nopreamble", "noearlypreamble"},
        {"DXVK_HUD", "SELECT_MULTIPLE", "scale=0.5", "scale=0.7", "opacity=0.5", "opacity=0.7", "devinfo", "fps", "frametimes", "submissions", "drawcalls", "pipelines", "descriptors", "memory", "gpuload", "version", "api", "cs", "compiler", "samplers"},
        {"MESA_EXTENSION_MAX_YEAR", "TEXT"},
        {"WRAPPER_MAX_IMAGE_COUNT", "TEXT"},
        {"MESA_GL_VERSION_OVERRIDE", "TEXT"},
        {"PULSE_LATENCY_MSEC", "NUMBER"},
        {"ANDROID_ALSA_LATENCY_MS", "NUMBER"},
        {"ANDROID_ALSA_VOLUME", "DECIMAL"},
        {"ANDROID_ALSA_BASS_BOOST", "DECIMAL"},
        {"ANDROID_ALSA_PERFORMANCE_MODE", "SELECT", "low_latency", "none", "power_saving"},
        {"WINNATIVE_ALSA_LATENCY_MS", "NUMBER"},
        {"WINNATIVE_ALSA_VOLUME", "DECIMAL"},
        {"WINNATIVE_ALSA_BASS_BOOST", "DECIMAL"},
        {"WINNATIVE_ALSA_PERFORMANCE_MODE", "SELECT", "low_latency", "none", "power_saving"},
        {"WINE_DO_NOT_CREATE_DXGI_DEVICE_MANAGER", "CHECKBOX", "0", "1"},
        {"WINE_NEW_MEDIASOURCE", "CHECKBOX", "0", "1"},
        {"GALLIUM_HUD", "SELECT_MULTIPLE", "simple", "fps", "frametime"}
    };
    private final LinearLayout container;
    private final TextView emptyTextView;
    private final LayoutInflater inflater;
    private boolean isDarkMode; // Field to track dark mode state

    // Interface for callback to get value from dynamically created views
    private interface GetValueCallback {
        String call();
    }

    // Default constructor
    public EnvVarsView(Context context) {
        this(context, null);
    }

    // Constructor with AttributeSet
    public EnvVarsView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    // Constructor with AttributeSet and defStyleAttr
    public EnvVarsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    // Constructor with AttributeSet, defStyleAttr, and defStyleRes
    public EnvVarsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        inflater = LayoutInflater.from(context);
        container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(container);

        emptyTextView = new TextView(context);
        emptyTextView.setText(R.string.no_items_to_display);
        emptyTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        emptyTextView.setGravity(Gravity.CENTER);
        int padding = (int) UnitUtils.dpToPx(16);
        emptyTextView.setPadding(padding, padding, padding, padding);
        addView(emptyTextView);
    }

    // Constructor with dark mode support
    public EnvVarsView(Context context, boolean isDarkMode) {
        this(context, null, isDarkMode);
    }

    public EnvVarsView(Context context, @Nullable AttributeSet attrs, boolean isDarkMode) {
        this(context, attrs, 0, isDarkMode);
    }

    public EnvVarsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, boolean isDarkMode) {
        this(context, attrs, defStyleAttr, 0, isDarkMode);
    }

    public EnvVarsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes, boolean isDarkMode) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.isDarkMode = isDarkMode;
        inflater = LayoutInflater.from(context);
        container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(container);

        emptyTextView = new TextView(context);
        emptyTextView.setText(R.string.no_items_to_display);
        emptyTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        emptyTextView.setGravity(Gravity.CENTER);
        int padding = (int) UnitUtils.dpToPx(16);
        emptyTextView.setPadding(padding, padding, padding, padding);
        addView(emptyTextView);

        applyDarkTheme(emptyTextView);
    }

    // Method to find known environment variable configuration
    private String[] findKnownEnvVar(String name) {
        for (String[] values : knownEnvVars) {
            if (values[0].equals(name)) {
                return values;
            }
        }
        return null;
    }

    // Method to apply dark theme styles
    private void applyDarkTheme(View view) {
        int primaryTextColor = resolveThemeColor(
                R.attr.aePrimaryTextColor,
                ContextCompat.getColor(getContext(), isDarkMode ? R.color.surface_badge_text_dark : R.color.surface_badge_text)
        );
        int hintTextColor = resolveThemeColor(
                R.attr.aeHintTextColor,
                ContextCompat.getColor(getContext(), isDarkMode ? R.color.surface_body_text_dark : R.color.surface_body_text)
        );
        if (view instanceof EditText) {
            view.setBackgroundResource(resolveThemeDrawable(
                    R.attr.aeEditTextBackground,
                    isDarkMode ? R.drawable.edit_text_dark : R.drawable.edit_text
            ));
            ((EditText) view).setTextColor(primaryTextColor);
            ((EditText) view).setHintTextColor(hintTextColor);
        } else if (view instanceof Spinner) {
            Spinner spinner = (Spinner) view;
            SpinnerAdapters.applySurface(spinner, isDarkMode);
            spinner.setPopupBackgroundResource(resolveThemeDrawable(
                    R.attr.aePopupBackground,
                    isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background
            ));
        } else if (view instanceof TextView) {
            ((TextView) view).setTextColor(primaryTextColor);
        }
    }

    private int resolveThemeColor(int attrId, int fallbackColor) {
        TypedValue typedValue = new TypedValue();
        if (getContext().getTheme().resolveAttribute(attrId, typedValue, true)) {
            if (typedValue.resourceId != 0) {
                return ContextCompat.getColor(getContext(), typedValue.resourceId);
            }
            return typedValue.data;
        }
        return fallbackColor;
    }

    private int resolveThemeDrawable(int attrId, int fallbackDrawable) {
        TypedValue typedValue = new TypedValue();
        if (getContext().getTheme().resolveAttribute(attrId, typedValue, true) && typedValue.resourceId != 0) {
            return typedValue.resourceId;
        }
        return fallbackDrawable;
    }

    // Method to get environment variables as a string
    public String getEnvVars() {
        EnvVars envVars = new EnvVars();
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            GetValueCallback getValueCallback = (GetValueCallback) child.getTag();
            String name = ((TextView) child.findViewById(R.id.TextView)).getText().toString();
            String value = getValueCallback.call().trim().replace(" ", "");
            if (!value.isEmpty()) envVars.put(name, value);
        }
        return envVars.toString();
    }

    // Method to check if a name is contained in the environment variables
    public boolean containsName(String name) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            String text = ((TextView) child.findViewById(R.id.TextView)).getText().toString();
            if (name.equals(text)) return true;
        }
        return false;
    }

    // Method to add an environment variable
    public void add(String name, String value) {
        final Context context = getContext();
        final View itemView = inflater.inflate(R.layout.env_vars_list_item, container, false);
        TextView nameTextView = itemView.findViewById(R.id.TextView);
        nameTextView.setText(name);
        int accent = ContextCompat.getColor(context, isDarkMode ? R.color.colorAccentDark : R.color.colorAccent);
        itemView.setBackground(buildInlineCardBackground(accent));

        String[] knownEnvVar = findKnownEnvVar(name);
        GetValueCallback getValueCallback;

        String type = knownEnvVar != null ? knownEnvVar[1] : "TEXT";

        switch (type) {
            case "CHECKBOX":
                final CompoundButton toggleButton = itemView.findViewById(R.id.ToggleButton);
                toggleButton.setVisibility(VISIBLE);
                toggleButton.setChecked(value.equals("1") || value.equals("true"));
                getValueCallback = () -> toggleButton.isChecked() ? knownEnvVar[3] : knownEnvVar[2];
                break;
            case "SELECT":
                String[] items = Arrays.copyOfRange(knownEnvVar, 2, knownEnvVar.length);
                final Spinner spinner = itemView.findViewById(R.id.Spinner);
                spinner.setAdapter(SpinnerAdapters.createGeneric(context, items));
                AppUtils.setSpinnerSelectionFromValue(spinner, value);
                spinner.setVisibility(VISIBLE);
                applyDarkTheme(spinner); // Apply dark theme
                getValueCallback = () -> spinner.getSelectedItem().toString();
                break;
            case "SELECT_MULTIPLE":
                final MultiSelectionComboBox comboBox = itemView.findViewById(R.id.MultiSelectionComboBox);
                comboBox.setItems(Arrays.copyOfRange(knownEnvVar, 2, knownEnvVar.length));
                comboBox.setSelectedItems(value.split(","));
                comboBox.setVisibility(VISIBLE);
                // applyDarkTheme(comboBox); // Implement if required for custom views
                getValueCallback = comboBox::getSelectedItemsAsString;
                break;
            case "TEXT":
                EditText editText = itemView.findViewById(R.id.EditText);
                editText.setVisibility(VISIBLE);
                editText.setText(value);
                applyDarkTheme(editText);
                getValueCallback = () -> editText.getText().toString();
                break;
            case "NUMBER":
            case "DECIMAL":
            default:
                EditText editTextNumber = itemView.findViewById(R.id.EditText);
                editTextNumber.setVisibility(VISIBLE);
                editTextNumber.setText(value);
                if (type.equals("NUMBER")) editTextNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
                if (type.equals("DECIMAL")) {
                    editTextNumber.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                }
                applyDarkTheme(editTextNumber);
                getValueCallback = () -> editTextNumber.getText().toString();
                break;
        }

        applyItemTheme(itemView);
        itemView.setTag(getValueCallback);
        itemView.findViewById(R.id.BTRemove).setOnClickListener((v) -> {
            container.removeView(itemView);
            if (container.getChildCount() == 0) emptyTextView.setVisibility(View.VISIBLE);
        });
        container.addView(itemView);
        emptyTextView.setVisibility(View.GONE);
    }

    // Method to set the environment variables
    public void setEnvVars(EnvVars envVars) {
        container.removeAllViews();
        for (String name : envVars) add(name, envVars.get(name));
    }

    public void setDarkMode(boolean isDarkMode) {
        this.isDarkMode = isDarkMode;
        applyDarkTheme(emptyTextView); // Apply dark theme to the current instance
        for (int i = 0; i < container.getChildCount(); i++) {
            applyItemTheme(container.getChildAt(i));
        }
    }

    private GradientDrawable buildInlineCardBackground(int accent) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(UnitUtils.dpToPx(14));
        background.setColor((accent & 0x00ffffff) | ((isDarkMode ? 50 : 20) << 24));
        background.setStroke(Math.round(UnitUtils.dpToPx(1)), (accent & 0x00ffffff) | ((isDarkMode ? 220 : 130) << 24));
        return background;
    }

    private void applyItemTheme(View itemView) {
        View nameView = itemView.findViewById(R.id.TextView);
        if (nameView != null) applyDarkTheme(nameView);

        View spinner = itemView.findViewById(R.id.Spinner);
        if (spinner != null && spinner.getVisibility() == VISIBLE) applyDarkTheme(spinner);

        View editText = itemView.findViewById(R.id.EditText);
        if (editText != null && editText.getVisibility() == VISIBLE) applyDarkTheme(editText);

        View toggleButton = itemView.findViewById(R.id.ToggleButton);
        if (toggleButton != null && toggleButton.getVisibility() == VISIBLE) applyDarkTheme(toggleButton);
    }

}
