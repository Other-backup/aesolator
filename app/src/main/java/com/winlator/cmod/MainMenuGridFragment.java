package com.winlator.cmod;

import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import java.util.Arrays;
import java.util.List;

public class MainMenuGridFragment extends Fragment {
    private static final class MenuCardEntry {
        private final int menuId;
        private final int titleRes;
        private final int hintRes;
        private final int iconRes;
        private final int lightColorRes;
        private final int darkColorRes;

        private MenuCardEntry(int menuId, int titleRes, int hintRes, int iconRes, int lightColorRes, int darkColorRes) {
            this.menuId = menuId;
            this.titleRes = titleRes;
            this.hintRes = hintRes;
            this.iconRes = iconRes;
            this.lightColorRes = lightColorRes;
            this.darkColorRes = darkColorRes;
        }
    }

    private static final List<MenuCardEntry> MENU_ENTRIES = Arrays.asList(
            new MenuCardEntry(R.id.main_menu_containers, R.string.containers, R.string.main_menu_hint_containers, R.drawable.ae_icon_package, R.color.colorAccent, R.color.colorAccentDark),
            new MenuCardEntry(R.id.main_menu_new_container, R.string.new_container, R.string.main_menu_hint_new_container, R.drawable.ae_icon_add, R.color.contents_lane_wine, R.color.contents_lane_wine_dark),
            new MenuCardEntry(R.id.main_menu_shortcuts, R.string.shortcuts, R.string.main_menu_hint_shortcuts, R.drawable.ae_icon_duplicate, R.color.colorPrimary, R.color.colorAccentDark),
            new MenuCardEntry(R.id.main_menu_big_picture, R.string.big_picture_mode, R.string.main_menu_hint_big_picture, R.drawable.ic_big_picture_mode, R.color.contents_lane_proton, R.color.contents_lane_proton_dark),
            new MenuCardEntry(R.id.main_menu_contents, R.string.contents, R.string.main_menu_hint_contents, R.drawable.ae_icon_download, R.color.contents_lane_vulkansdk, R.color.contents_lane_vulkansdk_dark),
            new MenuCardEntry(R.id.main_menu_adrenotools_gpu_drivers, R.string.adrenotools_gpu_drivers, R.string.main_menu_hint_graphics, R.drawable.ae_icon_turnip_lane, R.color.contents_lane_turnip, R.color.contents_lane_turnip_dark),
            new MenuCardEntry(R.id.main_menu_diagnostics, R.string.diagnostics, R.string.main_menu_hint_forensic, R.drawable.ae_icon_diagnostics, R.color.contents_lane_dgvoodoo, R.color.contents_lane_dgvoodoo_dark),
            new MenuCardEntry(R.id.main_menu_settings, R.string.settings, R.string.main_menu_hint_settings, R.drawable.ae_icon_settings, R.color.contents_lane_opengl, R.color.contents_lane_opengl_dark),
            new MenuCardEntry(R.id.main_menu_input_controls, R.string.input_controls, R.string.main_menu_hint_input, R.drawable.ae_icon_gamepad, R.color.contents_lane_dxvk, R.color.contents_lane_dxvk_dark),
            new MenuCardEntry(R.id.main_menu_about, R.string.about, R.string.main_menu_hint_about, R.drawable.ae_icon_about, R.color.contents_lane_proton, R.color.contents_lane_proton_dark)
    );

    private boolean isDarkMode;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_menu_grid_fragment, container, false);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        isDarkMode = preferences.getBoolean("dark_mode", false);

        LinearLayout heroCard = view.findViewById(R.id.LLMainMenuHeroCard);
        TextView heroTitle = view.findViewById(R.id.TVMainMenuHeroTitle);
        TextView heroSubtitle = view.findViewById(R.id.TVMainMenuHeroSubtitle);
        TextView sectionLabel = view.findViewById(R.id.TVMainMenuSectionLabel);
        LinearLayout rowsContainer = view.findViewById(R.id.LLMainMenuRows);

        int panelBackground = isDarkMode ? R.drawable.surface_card_background_dark : R.drawable.surface_card_background;
        int titleColor = ContextCompat.getColor(requireContext(), isDarkMode ? R.color.surface_badge_text_dark : R.color.surface_badge_text);
        int bodyColor = ContextCompat.getColor(requireContext(), isDarkMode ? R.color.surface_body_text_dark : R.color.surface_body_text);
        heroCard.setBackgroundResource(panelBackground);
        heroTitle.setTextColor(titleColor);
        heroSubtitle.setTextColor(bodyColor);
        sectionLabel.setTextColor(titleColor);

        populateMenuRows(rowsContainer, LayoutInflater.from(requireContext()), resolveSpanCount());
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle(R.string.app_name);
    }

    private void populateMenuRows(LinearLayout rowsContainer, LayoutInflater inflater, int spanCount) {
        rowsContainer.removeAllViews();
        int spacing = dpToPx(spanCount >= 4 ? 4f : 3f);
        for (int i = 0; i < MENU_ENTRIES.size(); i += spanCount) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            row.setOrientation(LinearLayout.HORIZONTAL);
            if (i > 0) {
                ((LinearLayout.LayoutParams) row.getLayoutParams()).topMargin = spacing;
            }

            for (int column = 0; column < spanCount; column++) {
                int index = i + column;
                View itemView = inflater.inflate(R.layout.main_menu_card_item, row, false);
                LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                if (column > 0) itemParams.setMarginStart(spacing);
                itemView.setLayoutParams(itemParams);
                if (index < MENU_ENTRIES.size()) {
                    bindCard(itemView, MENU_ENTRIES.get(index));
                    row.addView(itemView);
                } else {
                    itemView.setVisibility(View.INVISIBLE);
                    row.addView(itemView);
                }
            }
            rowsContainer.addView(row);
        }
    }

    private void bindCard(View itemView, MenuCardEntry entry) {
        LinearLayout card = itemView.findViewById(R.id.LLMainMenuCard);
        ImageView icon = itemView.findViewById(R.id.IVMainMenuCardIcon);
        TextView title = itemView.findViewById(R.id.TVMainMenuCardTitle);
        TextView hint = itemView.findViewById(R.id.TVMainMenuCardHint);

        int accent = ContextCompat.getColor(
                requireContext(),
                isDarkMode ? entry.darkColorRes : entry.lightColorRes
        );
        int titleColor = ContextCompat.getColor(requireContext(), isDarkMode ? R.color.surface_badge_text_dark : R.color.surface_badge_text);
        int bodyColor = ContextCompat.getColor(requireContext(), isDarkMode ? R.color.surface_body_text_dark : R.color.surface_body_text);

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dpToPx(14f));
        background.setColor(withAlpha(accent, isDarkMode ? 56 : 24));
        background.setStroke(dpToPx(1f), withAlpha(accent, isDarkMode ? 220 : 132));
        card.setBackground(background);

        icon.setImageResource(entry.iconRes);
        icon.clearColorFilter();
        icon.setColorFilter(accent);
        title.setText(entry.titleRes);
        title.setTextColor(titleColor);
        hint.setText(entry.hintRes);
        hint.setTextColor(bodyColor);
        card.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                activity.openMainMenuItem(entry.menuId, false);
            }
        });
    }

    private int withAlpha(int color, int alpha) {
        int clampedAlpha = Math.max(0, Math.min(255, alpha));
        return (color & 0x00ffffff) | (clampedAlpha << 24);
    }

    private int resolveSpanCount() {
        float widthDp = requireContext().getResources().getDisplayMetrics().widthPixels
                / requireContext().getResources().getDisplayMetrics().density;
        if (widthDp >= 960f) return 5;
        if (widthDp >= 700f) return 4;
        if (widthDp >= 520f) return 3;
        return 2;
    }

    private int dpToPx(float dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }
}
