package com.winlator.cmod.contentdialog;

import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.winlator.cmod.R;
import com.winlator.cmod.XServerDisplayActivity;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.KeyValueSet;
import com.winlator.cmod.core.SpinnerAdapters;
import com.winlator.cmod.renderer.GLRenderer;
import com.winlator.cmod.renderer.effects.ColorEffect;
import com.winlator.cmod.renderer.effects.CRTEffect;
import com.winlator.cmod.renderer.effects.FXAAEffect;
import com.winlator.cmod.renderer.effects.NTSCCombinedEffect;
import com.winlator.cmod.renderer.effects.ToonEffect;
import com.winlator.cmod.widget.SeekBar;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public class ScreenEffectDialog extends ContentDialog {

    private final XServerDisplayActivity activity;
    private final CheckBox cbEnableCRTShader;
    private final CheckBox cbEnableFXAA;
    private final CheckBox cbEnableToonShader;
    private final CheckBox cbEnableNTSCEffect;
    private final SharedPreferences preferences;
    private final Spinner sProfile;
    private final SeekBar sbBrightness;
    private final SeekBar sbContrast;
    private final SeekBar sbGamma;

    private static final String TAG = "ScreenEffectDialog";


    public ScreenEffectDialog(XServerDisplayActivity activity) {
        super(activity, R.layout.screen_effect_dialog);
        this.activity = activity;
        setTitle(R.string.screen_effect);
        setIcon(R.drawable.ae_icon_screen_effect);

        preferences = PreferenceManager.getDefaultSharedPreferences(activity);

        TextView lblColorAdjustment = findViewById(R.id.LBLColorAdjustment);
        if (lblColorAdjustment != null) {
            lblColorAdjustment.setTextColor(ContextCompat.getColor(activity, R.color.surface_runtime_taskmgr_text));
            lblColorAdjustment.setBackground(null);
        }

        sProfile = findViewById(R.id.SProfile);
        sbBrightness = findViewById(R.id.SBBrightness);
        sbContrast = findViewById(R.id.SBContrast);
        sbGamma = findViewById(R.id.SBGamma);
        cbEnableFXAA = findViewById(R.id.CBEnableFXAA);
        cbEnableCRTShader = findViewById(R.id.CBEnableCRTShader);

        cbEnableToonShader = findViewById(R.id.CBEnableToonShader);
        cbEnableNTSCEffect = findViewById(R.id.CBEnableNTSCEffect);


        GLRenderer renderer = activity.getXServerView().getRenderer();
        if (renderer == null) {
            Log.e(TAG, "Renderer is null in ScreenEffectDialog initialization!");
            return;
        }

        ColorEffect colorEffect = (ColorEffect) renderer.getEffectComposer().getEffect(ColorEffect.class);
        FXAAEffect fxaaEffect = (FXAAEffect) renderer.getEffectComposer().getEffect(FXAAEffect.class);
        CRTEffect crtEffect = (CRTEffect) renderer.getEffectComposer().getEffect(CRTEffect.class);
        ToonEffect toonEffect = (ToonEffect) renderer.getEffectComposer().getEffect(ToonEffect.class);
        NTSCCombinedEffect ntscEffect = (NTSCCombinedEffect) renderer.getEffectComposer().getEffect(NTSCCombinedEffect.class);

        Log.d(TAG, "ScreenEffectDialog initialized");

        if (colorEffect != null) {
            Log.d(TAG, "ColorEffect found");
            sbBrightness.setValue(colorEffect.getBrightness() * 100);
            sbContrast.setValue(colorEffect.getContrast() * 100);
            sbGamma.setValue(colorEffect.getGamma());
        } else {
            Log.d(TAG, "ColorEffect not found, resetting settings");
            resetSettings();
        }

        cbEnableFXAA.setChecked(fxaaEffect != null);
        cbEnableCRTShader.setChecked(crtEffect != null);
        cbEnableToonShader.setChecked(toonEffect != null);
        cbEnableNTSCEffect.setChecked(ntscEffect != null);

        loadProfileSpinner(sProfile, activity.getScreenEffectProfile());
        updateProfileActionButtons();

        sProfile.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateProfileActionButtons();
                if (position > 0) {
                    loadProfile(sProfile.getSelectedItem().toString());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateProfileActionButtons();
            }
        });

        Button resetButton = findViewById(R.id.BTReset);
        resetButton.setVisibility(View.VISIBLE);
        resetButton.setOnClickListener(v -> resetSettings());

        findViewById(R.id.BTConfirm).setOnClickListener(v -> {
            Log.d(TAG, "BTConfirm clicked. Preparing to save profile and apply effects.");
            saveProfile(sProfile);
            Log.d(TAG, "Profile saved.");

            // Directly calling applyEffects to ensure it's triggered
            Log.d(TAG, "Calling applyEffects() directly.");
            applyEffects(colorEffect, renderer, fxaaEffect, crtEffect, toonEffect, ntscEffect);

            Log.d(TAG, "Effects applied. Dismissing dialog.");
            dismiss(); // Close the dialog
            Log.d(TAG, "Dialog dismissed.");
        });



        findViewById(R.id.BTAddProfile).setOnClickListener(v -> promptAddProfile());
        findViewById(R.id.BTRemoveProfile).setOnClickListener(v -> promptDeleteProfile());

        applyRuntimeSurfaceStyle();
    }

    @Override
    public void show() {
        super.show();
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(0));
            getWindow().setLayout(
                    Math.round(AppUtils.getScreenWidth() * 0.992f),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        ViewGroup.LayoutParams params = getContentView().getLayoutParams();
        if (params != null) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            getContentView().setLayoutParams(params);
        }
        getContentView().setMinimumHeight(0);
        applyRuntimeSurfaceStyle();
    }

    private void applyRuntimeSurfaceStyle() {
        int brightText = ContextCompat.getColor(activity, R.color.surface_runtime_taskmgr_text);
        View root = getContentView();
        if (root != null) {
            root.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
            root.setPadding(dp(3), dp(3), dp(3), dp(3));
        }
        View frameLayout = findViewById(R.id.FrameLayout);
        if (frameLayout != null) {
            frameLayout.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
        }
        View titleBar = findViewById(R.id.LLTitleBar);
        if (titleBar != null) {
            titleBar.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
            titleBar.setPadding(dp(5), dp(3), dp(5), 0);
        }
        TextView titleView = findViewById(R.id.TVTitle);
        if (titleView != null) titleView.setTextColor(brightText);
        View iconView = findViewById(R.id.IVIcon);
        if (iconView instanceof android.widget.ImageView) {
            ((android.widget.ImageView) iconView).setColorFilter(brightText);
        }
        View resetButton = findViewById(R.id.BTReset);
        if (resetButton instanceof Button) {
            ((Button) resetButton).setBackgroundResource(R.drawable.surface_runtime_button_neutral);
            ((Button) resetButton).setTextColor(ContextCompat.getColor(activity, R.color.surface_runtime_button_text));
        }
        View confirmButton = findViewById(R.id.BTConfirm);
        if (confirmButton instanceof Button) {
            ((Button) confirmButton).setBackgroundResource(R.drawable.surface_runtime_button_positive);
            ((Button) confirmButton).setTextColor(ContextCompat.getColor(activity, R.color.surface_runtime_button_positive_text));
        }
        View cancelButton = findViewById(R.id.BTCancel);
        if (cancelButton instanceof Button) {
            ((Button) cancelButton).setBackgroundResource(R.drawable.surface_runtime_button_neutral);
            ((Button) cancelButton).setTextColor(ContextCompat.getColor(activity, R.color.surface_runtime_button_text));
        }
        int[] textIds = new int[] {
                R.id.CBEnableFXAA,
                R.id.CBEnableCRTShader,
                R.id.CBEnableToonShader,
                R.id.CBEnableNTSCEffect
        };
        for (int id : textIds) {
            TextView textView = findViewById(id);
            if (textView != null) textView.setTextColor(brightText);
        }
        TextView label = findViewById(R.id.LBLColorAdjustment);
        if (label != null) {
            label.setTextColor(brightText);
            label.setBackground(null);
        }
        Spinner profile = findViewById(R.id.SProfile);
        if (profile != null) {
            SpinnerAdapters.applyRuntimeSurface(profile);
        }
        View addButton = findViewById(R.id.BTAddProfile);
        View removeButton = findViewById(R.id.BTRemoveProfile);
        if (addButton != null) {
            addButton.setBackgroundResource(R.drawable.surface_runtime_button_neutral);
            if (addButton instanceof android.widget.ImageButton) {
                ((android.widget.ImageButton) addButton).setColorFilter(brightText);
            }
        }
        if (removeButton != null) {
            removeButton.setBackgroundResource(R.drawable.surface_runtime_button_neutral);
            if (removeButton instanceof android.widget.ImageButton) {
                ((android.widget.ImageButton) removeButton).setColorFilter(brightText);
            }
        }
        if (addButton != null && addButton.getParent() instanceof View) {
            View group = (View) addButton.getParent();
            if (group != null) {
                group.setBackground(null);
                group.setPadding(0, 0, 0, 0);
            }
        }
        updateProfileActionButtons();
    }

    private int dp(int value) {
        return Math.round(activity.getResources().getDisplayMetrics().density * value);
    }

    private void promptAddProfile() {
        ContentDialog.prompt(activity, R.string.do_you_want_to_add_a_new_profile, null, name -> addProfile(name, sProfile));
    }

    private void promptDeleteProfile() {
        if (sProfile.getSelectedItemPosition() > 0) {
            String selectedProfile = sProfile.getSelectedItem().toString();
            ContentDialog.confirm(activity, R.string.do_you_want_to_remove_this_profile, () -> removeProfile(selectedProfile, sProfile));
        } else {
            AppUtils.showToast(activity, R.string.no_profile_selected);
        }
    }

    private void addProfile(String newName, Spinner sProfile) {
        Set<String> profiles = new LinkedHashSet<>(preferences.getStringSet("screen_effect_profiles", new LinkedHashSet<>()));
        for (String profile : profiles) {
            String[] parts = profile.split(":");
            if (parts[0].equals(newName)) {
                return;
            }
        }
        profiles.add(newName + ":");
        preferences.edit().putStringSet("screen_effect_profiles", profiles).apply();
        loadProfileSpinner(sProfile, newName);
    }

    private void loadProfileSpinner(Spinner sProfile, String selectedName) {
        Set<String> profiles = new LinkedHashSet<>(preferences.getStringSet("screen_effect_profiles", new LinkedHashSet<>()));
        ArrayList<String> items = new ArrayList<>();
        items.add("-- " + activity.getString(R.string.default_profile) + " --");
        int selectedPosition = 0, position = 1;
        for (String profile : profiles) {
            String[] parts = profile.split(":");
            items.add(parts[0]);
            if (parts[0].equals(selectedName)) {
                selectedPosition = position;
            }
            position++;
        }
        sProfile.setAdapter(SpinnerAdapters.createRuntime(activity, items));
        sProfile.setSelection(selectedPosition);
        SpinnerAdapters.applyRuntimeSurface(sProfile);
        updateProfileActionButtons();
    }

    private void updateProfileActionButtons() {
        View removeButton = findViewById(R.id.BTRemoveProfile);
        if (removeButton == null) return;
        boolean hasCustomProfile = sProfile != null && sProfile.getSelectedItemPosition() > 0;
        removeButton.setEnabled(hasCustomProfile);
        removeButton.setAlpha(hasCustomProfile ? 1.0f : 0.46f);
    }

    private void loadProfile(String name) {
        Set<String> profiles = new LinkedHashSet<>(preferences.getStringSet("screen_effect_profiles", new LinkedHashSet<>()));
        for (String profile : profiles) {
            String[] parts = profile.split(":");
            if (parts[0].equals(name) && parts.length > 1 && !parts[1].isEmpty()) {
                KeyValueSet settings = new KeyValueSet(parts[1]);
                sbBrightness.setValue(settings.getFloat("brightness", 0));
                sbContrast.setValue(settings.getFloat("contrast", 1.0f));
                sbGamma.setValue(settings.getFloat("gamma", 1.0f));
                cbEnableFXAA.setChecked(settings.getBoolean("fxaa", false));
                cbEnableCRTShader.setChecked(settings.getBoolean("crt_shader", false));
                cbEnableToonShader.setChecked(settings.getBoolean("toon_shader", false));
                cbEnableNTSCEffect.setChecked(settings.getBoolean("ntsc_effect", false));
                return;
            }
        }
    }

    private void removeProfile(String targetName, Spinner sProfile) {
        Set<String> profiles = new LinkedHashSet<>(preferences.getStringSet("screen_effect_profiles", new LinkedHashSet<>()));
        profiles.removeIf(profile -> profile.split(":")[0].equals(targetName));
        preferences.edit().putStringSet("screen_effect_profiles", profiles).apply();
        loadProfileSpinner(sProfile, null);
        resetSettings();
    }

    private void resetSettings() {
        sbBrightness.setValue(0);
        sbContrast.setValue(0);
        sbGamma.setValue(1.0f);
        cbEnableFXAA.setChecked(false);
        cbEnableCRTShader.setChecked(false);
        cbEnableToonShader.setChecked(false);
        cbEnableNTSCEffect.setChecked(false);
    }

    private void saveProfile(Spinner sProfile) {
        if (sProfile.getSelectedItemPosition() > 0) {
            String selectedProfile = sProfile.getSelectedItem().toString();
            Set<String> oldProfiles = new LinkedHashSet<>(preferences.getStringSet("screen_effect_profiles", new LinkedHashSet<>()));
            Set<String> newProfiles = new LinkedHashSet<>();
            KeyValueSet settings = new KeyValueSet();
            settings.put("brightness", sbBrightness.getValue());
            settings.put("contrast", sbContrast.getValue());
            settings.put("gamma", sbGamma.getValue());
            settings.put("fxaa", cbEnableFXAA.isChecked());
            settings.put("crt_shader", cbEnableCRTShader.isChecked());
            settings.put("toon_shader", cbEnableToonShader.isChecked());
            settings.put("ntsc_effect", cbEnableNTSCEffect.isChecked());

            for (String profile : oldProfiles) {
                String[] parts = profile.split(":");
                if (parts[0].equals(selectedProfile)) {
                    newProfiles.add(selectedProfile + ":" + settings.toString());
                } else {
                    newProfiles.add(profile);
                }
            }
            preferences.edit().putStringSet("screen_effect_profiles", newProfiles).apply();
            activity.setScreenEffectProfile(selectedProfile);
        }
    }

    public void applyEffects(ColorEffect colorEffect, GLRenderer renderer, FXAAEffect fxaaEffect, CRTEffect crtEffect, ToonEffect toonEffect, NTSCCombinedEffect ntscEffect) {
        Log.d(TAG, "applyEffects() called");

        float brightness = sbBrightness.getValue();
        float contrast = sbContrast.getValue();
        float gamma = sbGamma.getValue();
        boolean enableFXAA = cbEnableFXAA.isChecked();
        boolean enableCRTShader = cbEnableCRTShader.isChecked();
        boolean enableToonShader = cbEnableToonShader.isChecked();
        boolean enableNTSCEffect = cbEnableNTSCEffect.isChecked();

        Log.d(TAG, "Settings - Brightness: " + brightness + ", Contrast: " + contrast + ", Gamma: " + gamma);
        Log.d(TAG, "FXAA Enabled: " + enableFXAA + ", CRT Shader Enabled: " + enableCRTShader);

        // Check ColorEffect state
        if (colorEffect == null) {
            Log.d(TAG, "ColorEffect is null, creating new instance.");
            colorEffect = new ColorEffect();
        }

        // Check if renderer and effect composer are non-null
        if (renderer == null) {
            Log.e(TAG, "Renderer is null!");
            return;
        }

        if (renderer.getEffectComposer() == null) {
            Log.e(TAG, "EffectComposer is null!");
            return;
        }

        // Apply or remove ColorEffect
        if (brightness == 0 && contrast == 0 && gamma == 1.0f) {
            Log.d(TAG, "No adjustments are applied. Removing ColorEffect if it exists.");
            renderer.getEffectComposer().removeEffect(colorEffect);
        } else {
            Log.d(TAG, "Applying ColorEffect adjustments.");
            colorEffect.setBrightness(brightness / 100f);
            colorEffect.setContrast(contrast / 100f);
            colorEffect.setGamma(gamma);
            renderer.getEffectComposer().addEffect(colorEffect);
            Log.d(TAG, "ColorEffect added/updated.");
        }

        // Apply or remove FXAAEffect
        if (enableFXAA) {
            if (fxaaEffect == null) {
                Log.d(TAG, "FXAAEffect is null, creating and adding new instance.");
                fxaaEffect = new FXAAEffect();
                renderer.getEffectComposer().addEffect(fxaaEffect);
            } else {
                Log.d(TAG, "FXAAEffect is already added.");
            }
        } else if (fxaaEffect != null) {
            Log.d(TAG, "FXAA is disabled. Removing FXAAEffect.");
            renderer.getEffectComposer().removeEffect(fxaaEffect);
        }

        // Apply or remove CRTEffect
        if (enableCRTShader) {
            if (crtEffect == null) {
                Log.d(TAG, "CRTEffect is null, creating and adding new instance.");
                crtEffect = new CRTEffect();
                renderer.getEffectComposer().addEffect(crtEffect);
            } else {
                Log.d(TAG, "CRTEffect is already added.");
            }
        } else if (crtEffect != null) {
            Log.d(TAG, "CRT Shader is disabled. Removing CRTEffect.");
            renderer.getEffectComposer().removeEffect(crtEffect);
        }


        // Apply or remove ToonEffect
        if (enableToonShader) {
            if (toonEffect == null) {
                Log.d(TAG, "ToonEffect is null, creating and adding new instance.");
                toonEffect = new ToonEffect();
                renderer.getEffectComposer().addEffect(toonEffect);
            } else {
                Log.d(TAG, "ToonEffect is already added.");
            }
        } else if (toonEffect != null) {
            Log.d(TAG, "Toon Shader is disabled. Removing ToonEffect.");
            renderer.getEffectComposer().removeEffect(toonEffect);
        }


        // Apply or remove NTSCCombinedEffect
        if (enableNTSCEffect) {
            if (ntscEffect == null) {
                Log.d(TAG, "NTSCCombinedEffect is null, creating and adding new instance.");
                ntscEffect = new NTSCCombinedEffect();
                renderer.getEffectComposer().addEffect(ntscEffect);
            } else {
                Log.d(TAG, "NTSCCombinedEffect is already added.");
            }
        } else if (ntscEffect != null) {
            Log.d(TAG, "NTSC Effect is disabled. Removing NTSCCombinedEffect.");
            renderer.getEffectComposer().removeEffect(ntscEffect);
        }

//        // Toggle ToonEffect
//        if (renderer != null && renderer.getEffectComposer() != null) {
//            if (enableToonShader) {
//                renderer.getEffectComposer().toggleToonEffect();
//            } else {
//                ToonEffect toonEffect = renderer.getEffectComposer().getEffect(ToonEffect.class);
//                if (toonEffect != null) {
//                    renderer.getEffectComposer().removeEffect(toonEffect);
//                }
//            }
//        }

        saveProfile(sProfile);
        Log.d(TAG, "Profile saved after applying effects.");
    }

    public void setOnConfirmCallback(Runnable confirmCallback) {
        Log.d(TAG, "Setting OnConfirm callback.");
        this.onConfirmCallback = confirmCallback;
    }


}
