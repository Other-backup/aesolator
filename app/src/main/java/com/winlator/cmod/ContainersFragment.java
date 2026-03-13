package com.winlator.cmod;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.cmod.R;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.container.Shortcut;
import com.winlator.cmod.contentdialog.ContentDialog;
import com.winlator.cmod.contentdialog.StorageInfoDialog;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.LaunchSecurity;
import com.winlator.cmod.core.PreloaderDialog;
import com.winlator.cmod.core.UnitUtils;
import com.winlator.cmod.xenvironment.ImageFs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ContainersFragment extends Fragment {
    private RecyclerView recyclerView;
    private TextView emptyTextView;
    private ContainerManager manager;
    private PreloaderDialog preloaderDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        preloaderDialog = new PreloaderDialog(getActivity());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        manager = new ContainerManager(getContext());
        loadContainersList();
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.containers);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FrameLayout frameLayout = (FrameLayout) inflater.inflate(R.layout.containers_fragment, container, false);
        recyclerView = frameLayout.findViewById(R.id.RecyclerView);
        emptyTextView = frameLayout.findViewById(R.id.TVEmptyText);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        bindPrimaryActions(frameLayout);
        return frameLayout;
    }

    private void loadContainersList() {
        ArrayList<Container> containers = manager.getContainers();
        recyclerView.setAdapter(new ContainersAdapter(containers));
        emptyTextView.setVisibility(containers.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void bindPrimaryActions(View root) {
        boolean isDarkMode = PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean("dark_mode", false);
        bindActionCard(
                root.findViewById(R.id.LLCreateContainerCard),
                root.findViewById(R.id.FLCreateContainerIconBadge),
                root.findViewById(R.id.IVCreateContainerIcon),
                root.findViewById(R.id.TVCreateContainerEyebrow),
                root.findViewById(R.id.TVCreateContainerTitle),
                root.findViewById(R.id.TVCreateContainerHint),
                isDarkMode,
                R.color.contents_lane_wine,
                R.color.contents_lane_wine_dark,
                this::openNewContainer
        );
        bindActionCard(
                root.findViewById(R.id.LLBigPictureCard),
                root.findViewById(R.id.FLBigPictureIconBadge),
                root.findViewById(R.id.IVBigPictureIcon),
                root.findViewById(R.id.TVBigPictureEyebrow),
                root.findViewById(R.id.TVBigPictureTitle),
                root.findViewById(R.id.TVBigPictureHint),
                isDarkMode,
                R.color.contents_lane_opengl,
                R.color.contents_lane_opengl_dark,
                this::openBigPictureMode
        );
    }

    private void bindActionCard(
            View card,
            View badge,
            ImageView icon,
            TextView eyebrow,
            TextView title,
            TextView hint,
            boolean isDarkMode,
            int lightColorRes,
            int darkColorRes,
            Runnable action
    ) {
        int accent = ContextCompat.getColor(requireContext(), isDarkMode ? darkColorRes : lightColorRes);
        int titleColor = ContextCompat.getColor(requireContext(), isDarkMode ? R.color.surface_badge_text_dark : R.color.surface_badge_text);
        int bodyColor = ContextCompat.getColor(requireContext(), isDarkMode ? R.color.surface_body_text_dark : R.color.surface_body_text);

        card.setBackground(buildActionCardBackground(accent, isDarkMode));
        badge.setBackground(buildActionBadgeBackground(accent, isDarkMode));
        icon.clearColorFilter();
        icon.setColorFilter(accent);
        eyebrow.setTextColor(withAlpha(titleColor, isDarkMode ? 210 : 190));
        title.setTextColor(titleColor);
        hint.setTextColor(bodyColor);
        card.setOnClickListener(v -> action.run());
    }

    private void openNewContainer() {
        Context context = getContext();
        if (context == null || !ImageFs.find(context).isValid()) return;
        FragmentManager fragmentManager = getParentFragmentManager();
        fragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_down, R.anim.slide_in_down, R.anim.slide_out_up)
                .addToBackStack(null)
                .replace(R.id.FLFragmentContainer, new ContainerDetailFragment())
                .commit();
    }

    private void openBigPictureMode() {
        Intent intent = new Intent(getContext(), BigPictureActivity.class);
        startActivity(intent);
        getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }


    private class ContainersAdapter extends RecyclerView.Adapter<ContainersAdapter.ViewHolder> {
        private final List<Container> data;

        private class ViewHolder extends RecyclerView.ViewHolder {
            private final ImageView runButton; // Changed to ImageButton
            private final ImageView menuButton; // Changed to ImageButton
            private final ImageView imageView;
            private final TextView title;

            private ViewHolder(View view) {
                super(view);
                this.runButton = view.findViewById(R.id.BTRun); // Find by correct ID
                this.imageView = view.findViewById(R.id.ImageView);
                this.title = view.findViewById(R.id.TVTitle);
                this.menuButton = view.findViewById(R.id.BTMenu);
            }
        }

        public ContainersAdapter(List<Container> data) {
            this.data = data;
        }

        @Override
        public final ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.container_list_item, parent, false));
        }

        @Override
        public void onViewRecycled(@NonNull ViewHolder holder) {
            holder.runButton.setOnClickListener(null); // Remove listeners
            holder.menuButton.setOnClickListener(null); // Remove listeners
            super.onViewRecycled(holder);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            final Container item = data.get(position); // Use 'item' instead of undefined 'container'
            boolean isDarkMode = PreferenceManager.getDefaultSharedPreferences(holder.itemView.getContext()).getBoolean("dark_mode", false);
            int accent = androidx.core.content.ContextCompat.getColor(
                    holder.itemView.getContext(),
                    isDarkMode ? R.color.colorAccentDark : R.color.colorAccent
            );
            holder.imageView.setImageResource(R.drawable.ae_icon_package);
            holder.imageView.setColorFilter(accent);
            holder.title.setText(item.getName());
            holder.title.setTextColor(accent);
            holder.itemView.setBackground(buildRowBackground(accent, isDarkMode));

            holder.runButton.setOnClickListener(view -> runContainer(item)); // Correct item reference

            holder.menuButton.setOnClickListener(view -> showListItemMenu(view, item));
        }

        @Override
        public final int getItemCount() {
            return data.size();
        }

        private void runContainer(Container container) {
            final Context context = getContext();
            if (!XrActivity.isEnabled(getContext())) {
                Intent intent = new Intent(context, XServerDisplayActivity.class);
                intent.putExtra("container_id", container.id);
                LaunchSecurity.signXServerLaunchIntent(context, intent);
                requireActivity().startActivity(intent);
            } else {
                XrActivity.openIntent(getActivity(), container.id, null);
            }
        }

        private void showListItemMenu(View anchorView, Container container) {
            final Context context = getContext();
            PopupMenu listItemMenu = new PopupMenu(context, anchorView);
            listItemMenu.inflate(R.menu.container_popup_menu);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) listItemMenu.setForceShowIcon(true);

            listItemMenu.setOnMenuItemClickListener((menuItem) -> {
                switch (menuItem.getItemId()) {
                    case R.id.container_edit:
                        FragmentManager fragmentManager = getParentFragmentManager();
                        fragmentManager.beginTransaction()
                                .setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_down, R.anim.slide_in_down, R.anim.slide_out_up)
                                .addToBackStack(null)
                                .replace(R.id.FLFragmentContainer, new ContainerDetailFragment(container.id))
                                .commit();
                        break;
                    case R.id.container_duplicate:
                        ContentDialog.confirm(getContext(), R.string.do_you_want_to_duplicate_this_container, () -> {
                            preloaderDialog.show(R.string.duplicating_container);
                            manager.duplicateContainerAsync(container, () -> {
                                preloaderDialog.close();
                                loadContainersList();
                            });
                        });
                        break;
                    case R.id.container_remove:
                        ContentDialog.confirm(getContext(), R.string.do_you_want_to_remove_this_container, () -> {
                            preloaderDialog.show(R.string.removing_container);
                            for (Shortcut shortcut : manager.loadShortcuts()) {
                                if (shortcut.container == container)
                                    ShortcutsFragment.disableShortcutOnScreen(context, shortcut);
                            }
                            manager.removeContainerAsync(container, () -> {
                                preloaderDialog.close();
                                loadContainersList();
                            });
                        });
                        break;
                    case R.id.container_info:
                        (new StorageInfoDialog(getActivity(), container)).show();
                        break;
                }
                return true;
            });
            listItemMenu.show();
        }
    }


    private GradientDrawable buildRowBackground(int accent, boolean isDarkMode) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(UnitUtils.dpToPx(16));
        background.setColor(withAlpha(accent, isDarkMode ? 50 : 20));
        background.setStroke(Math.round(UnitUtils.dpToPx(1)), withAlpha(accent, isDarkMode ? 220 : 130));
        return background;
    }

    private GradientDrawable buildActionCardBackground(int accent, boolean isDarkMode) {
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                        withAlpha(accent, isDarkMode ? 78 : 34),
                        withAlpha(accent, isDarkMode ? 28 : 10)
                }
        );
        background.setCornerRadius(UnitUtils.dpToPx(18));
        background.setStroke(Math.round(UnitUtils.dpToPx(1)), withAlpha(accent, isDarkMode ? 230 : 150));
        return background;
    }

    private GradientDrawable buildActionBadgeBackground(int accent, boolean isDarkMode) {
        GradientDrawable badge = new GradientDrawable();
        badge.setShape(GradientDrawable.OVAL);
        badge.setColor(withAlpha(accent, isDarkMode ? 64 : 28));
        badge.setStroke(Math.round(UnitUtils.dpToPx(1)), withAlpha(accent, isDarkMode ? 220 : 145));
        return badge;
    }

    private int withAlpha(int color, int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return (color & 0x00ffffff) | (clamped << 24);
    }
}
