package com.winlator.cmod;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import com.winlator.cmod.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.navigation.NavigationView;
import com.winlator.cmod.contentdialog.ContentDialog;
import com.winlator.cmod.contents.AdrenotoolsManager;
import java.util.ArrayList;
import java.util.Locale;

public class AdrenotoolsFragment extends Fragment {
    
    private AdrenotoolsManager adrenotoolsManager;
    private RecyclerView recyclerView;
    
    @Override 
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.adrenotoolsManager = new AdrenotoolsManager(getActivity());
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup layout = (ViewGroup)inflater.inflate(R.layout.adrenotools_fragment, container, false);
        recyclerView = layout.findViewById(R.id.RecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(new DriversAdapter(adrenotoolsManager.enumarateInstalledDrivers()));
        View btInstallDriver = layout.findViewById(R.id.BTInstallDriver);
        btInstallDriver.setOnClickListener((v) -> openZipInstaller());

        View btOpenContentsGraphics = layout.findViewById(R.id.BTOpenContentsGraphics);
        btOpenContentsGraphics.setOnClickListener(v -> {
            openContents();
        });

        layout.findViewById(R.id.BTLaneTurnip).setOnClickListener(v -> openZipInstaller());
        layout.findViewById(R.id.BTLaneOpenGL).setOnClickListener(v -> openZipInstaller());
        layout.findViewById(R.id.BTLaneDgVoodoo).setOnClickListener(v ->
                openContentsForType("DgVoodoo"));
        layout.findViewById(R.id.BTLaneDxvkVkd3d).setOnClickListener(v ->
                openContentsForType("DXVK"));
        layout.findViewById(R.id.BTLaneVulkanSdk).setOnClickListener(v ->
                openContentsForType("VulkanSDK"));

        layout.findViewById(R.id.BTDri3Settings).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                NavigationView navigationView = getActivity().findViewById(R.id.NavigationView);
                if (navigationView != null) {
                    navigationView.setCheckedItem(R.id.main_menu_settings);
                    ((MainActivity) getActivity()).onNavigationItemSelected(navigationView.getMenu().findItem(R.id.main_menu_settings));
                }
            }
        });

        layout.findViewById(R.id.BTForensicCenter).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                NavigationView navigationView = getActivity().findViewById(R.id.NavigationView);
                if (navigationView != null) {
                    navigationView.setCheckedItem(R.id.main_menu_diagnostics);
                    ((MainActivity) getActivity()).onNavigationItemSelected(navigationView.getMenu().findItem(R.id.main_menu_diagnostics));
                }
            }
        });
        styleGraphicsCenterButtons(layout);
        return layout;
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.adrenotools_gpu_drivers);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MainActivity.OPEN_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            String driver = adrenotoolsManager.installDriver(uri);
            if (!driver.isEmpty())
                ((DriversAdapter)recyclerView.getAdapter()).addItem(driver);
        }
     }       
    
    private class DriversAdapter extends RecyclerView.Adapter<DriversAdapter.ViewHolder> {
        private ArrayList<String> driversList;

        public class ViewHolder extends RecyclerView.ViewHolder {
            private ImageView ivIcon;
            private TextView tvName;
            private TextView tvVersion;
            private TextView tvMeta;
            private ImageButton btMenu;

            public ViewHolder(View v) {
                super(v);
                ivIcon = v.findViewById(R.id.IVIcon);
                tvName = v.findViewById(R.id.TVName);
                tvVersion = v.findViewById(R.id.TVVersion);
                tvMeta = v.findViewById(R.id.TVMeta);
                btMenu = v.findViewById(R.id.BTMenu);
            }
        }
        
        public DriversAdapter(ArrayList<String> driversList) {
            this.driversList = driversList;
        }
        
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.adrenotools_list_item, viewGroup, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder viewHolder, final int position) {
            final String entryId = driversList.get(position);
            viewHolder.tvName.setText(adrenotoolsManager.getDriverName(driversList.get(position)));
            viewHolder.tvVersion.setText(adrenotoolsManager.getDriverVersion(driversList.get(position)));
            viewHolder.tvMeta.setText(buildDriverMeta(entryId));
            viewHolder.ivIcon.setImageResource(R.drawable.icon_open);
            viewHolder.btMenu.setOnClickListener((v) -> {
                removeAtIndex(position);
            });
        }
        
        public void addItem(String item) {
            driversList.add(item);
            notifyItemInserted(getItemCount() - 1);
        }
        
        public void removeAtIndex(int index) {
            String deletedDriver = driversList.remove(index);
            adrenotoolsManager.removeDriver(deletedDriver);
            notifyItemRemoved(index);
            notifyItemRangeChanged(index, getItemCount());
        }
        
        @Override
        public int getItemCount() {
            return driversList.size();
        }

        private String buildDriverMeta(String entryId) {
            String normalized = entryId == null ? "" : entryId.toLowerCase(Locale.US);
            String arch;
            if (normalized.contains("arm64ec")) arch = "ARM64EC";
            else if (normalized.contains("x86_64") || normalized.contains("amd64")) arch = "x86_64";
            else if (normalized.contains("arm64")) arch = "ARM64";
            else arch = "generic";
            return "Installed • " + arch;
        }
    }

    private void openContentsForType(String typeName) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        preferences.edit().putString("contents_preselected_type", typeName).apply();
        openContents();
    }

    private void openZipInstaller() {
        ContentDialog.confirm(getContext(), getString(R.string.install_drivers_message) + " " + getString(R.string.install_drivers_warning), () -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            getActivity().startActivityFromFragment(this, intent, MainActivity.OPEN_FILE_REQUEST_CODE);
        });
    }

    private void openContents() {
        if (getActivity() instanceof MainActivity) {
            NavigationView navigationView = getActivity().findViewById(R.id.NavigationView);
            if (navigationView != null) {
                navigationView.setCheckedItem(R.id.main_menu_contents);
                ((MainActivity) getActivity()).onNavigationItemSelected(navigationView.getMenu().findItem(R.id.main_menu_contents));
            }
        }
    }

    private void styleGraphicsCenterButtons(View root) {
        boolean isDarkMode = PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean("dark_mode", false);
        styleLaneButton(root, R.id.BTLaneTurnip, R.color.contents_lane_turnip, R.color.contents_lane_turnip_dark, isDarkMode);
        styleLaneButton(root, R.id.BTLaneOpenGL, R.color.contents_lane_opengl, R.color.contents_lane_opengl_dark, isDarkMode);
        styleLaneButton(root, R.id.BTLaneDgVoodoo, R.color.contents_lane_dgvoodoo, R.color.contents_lane_dgvoodoo_dark, isDarkMode);
        styleLaneButton(root, R.id.BTLaneDxvkVkd3d, R.color.contents_lane_dxvk, R.color.contents_lane_dxvk_dark, isDarkMode);
        styleLaneButton(root, R.id.BTLaneVulkanSdk, R.color.contents_lane_vulkansdk, R.color.contents_lane_vulkansdk_dark, isDarkMode);
        styleLaneButton(root, R.id.BTDri3Settings, R.color.colorPrimary, R.color.colorAccentDark, isDarkMode);
        styleLaneButton(root, R.id.BTForensicCenter, R.color.colorPrimary, R.color.colorAccentDark, isDarkMode);
    }

    private void styleLaneButton(View root, int buttonId, int lightColorRes, int darkColorRes, boolean isDarkMode) {
        View rawButton = root.findViewById(buttonId);
        if (!(rawButton instanceof Button)) return;
        Button button = (Button) rawButton;
        int accent = ContextCompat.getColor(requireContext(), isDarkMode ? darkColorRes : lightColorRes);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(10f));
        bg.setColor(withAlpha(accent, isDarkMode ? 62 : 26));
        bg.setStroke(dpToPx(1f), withAlpha(accent, isDarkMode ? 238 : 180));

        button.setBackground(bg);
        button.setTextColor(isDarkMode ? Color.WHITE : accent);
    }

    private int dpToPx(float dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    private int withAlpha(int color, int alpha) {
        int clampedAlpha = Math.max(0, Math.min(255, alpha));
        return (color & 0x00ffffff) | (clampedAlpha << 24);
    }
}
