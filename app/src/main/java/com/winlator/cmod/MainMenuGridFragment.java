package com.winlator.cmod;

import android.content.SharedPreferences;
import android.graphics.Rect;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
            new MenuCardEntry(R.id.main_menu_shortcuts, R.string.shortcuts, R.string.main_menu_hint_shortcuts, R.drawable.ae_icon_duplicate, R.color.colorPrimary, R.color.colorAccentDark),
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
        RecyclerView recyclerView = view.findViewById(R.id.RVMainMenuCards);

        int panelBackground = isDarkMode ? R.drawable.surface_card_background_dark : R.drawable.surface_card_background;
        int titleColor = ContextCompat.getColor(requireContext(), isDarkMode ? R.color.surface_badge_text_dark : R.color.surface_badge_text);
        int bodyColor = ContextCompat.getColor(requireContext(), isDarkMode ? R.color.surface_body_text_dark : R.color.surface_body_text);
        heroCard.setBackgroundResource(panelBackground);
        heroTitle.setTextColor(titleColor);
        heroSubtitle.setTextColor(bodyColor);
        sectionLabel.setTextColor(titleColor);

        int spanCount = resolveSpanCount();
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), spanCount));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);
        if (recyclerView.getItemDecorationCount() == 0) {
            recyclerView.addItemDecoration(new GridSpacingItemDecoration(spanCount, dpToPx(4f)));
        }
        recyclerView.setAdapter(new MainMenuCardAdapter(MENU_ENTRIES));
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle(R.string.app_name);
    }

    private final class MainMenuCardAdapter extends RecyclerView.Adapter<MainMenuCardAdapter.ViewHolder> {
        private final List<MenuCardEntry> entries;

        private MainMenuCardAdapter(List<MenuCardEntry> entries) {
            this.entries = entries;
        }

        private final class ViewHolder extends RecyclerView.ViewHolder {
            private final LinearLayout card;
            private final ImageView icon;
            private final TextView title;
            private final TextView hint;

            private ViewHolder(@NonNull View itemView) {
                super(itemView);
                card = itemView.findViewById(R.id.LLMainMenuCard);
                icon = itemView.findViewById(R.id.IVMainMenuCardIcon);
                title = itemView.findViewById(R.id.TVMainMenuCardTitle);
                hint = itemView.findViewById(R.id.TVMainMenuCardHint);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.main_menu_card_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MenuCardEntry entry = entries.get(position);
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
            holder.card.setBackground(background);

            holder.icon.setImageResource(entry.iconRes);
            holder.icon.setColorFilter(accent);
            holder.title.setText(entry.titleRes);
            holder.title.setTextColor(titleColor);
            holder.hint.setText(entry.hintRes);
            holder.hint.setTextColor(bodyColor);
            holder.card.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    MainActivity activity = (MainActivity) getActivity();
                    activity.openMainMenuItem(entry.menuId, false);
                }
            });
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }
    }

    private int withAlpha(int color, int alpha) {
        int clampedAlpha = Math.max(0, Math.min(255, alpha));
        return (color & 0x00ffffff) | (clampedAlpha << 24);
    }

    private int resolveSpanCount() {
        float widthDp = requireContext().getResources().getDisplayMetrics().widthPixels
                / requireContext().getResources().getDisplayMetrics().density;
        return widthDp >= 960f ? 4 : 2;
    }

    private int dpToPx(float dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    private static final class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private final int spanCount;
        private final int spacing;

        private GridSpacingItemDecoration(int spanCount, int spacing) {
            this.spanCount = Math.max(1, spanCount);
            this.spacing = Math.max(0, spacing);
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            if (position == RecyclerView.NO_POSITION) return;
            int column = position % spanCount;
            outRect.left = spacing - column * spacing / spanCount;
            outRect.right = (column + 1) * spacing / spanCount;
            outRect.top = position < spanCount ? 0 : spacing;
        }
    }
}
