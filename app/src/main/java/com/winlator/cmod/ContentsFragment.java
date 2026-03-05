package com.winlator.cmod;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.contentdialog.ContentDialog;
import com.winlator.cmod.contentdialog.ContentInfoDialog;
import com.winlator.cmod.contentdialog.ContentUntrustedDialog;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.contents.Downloader;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.PreloaderDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class ContentsFragment extends Fragment {
    private static final List<ContentProfile.ContentType> SUPPORTED_CONTENT_TYPES = Arrays.asList(
            ContentProfile.ContentType.CONTENT_TYPE_WINE,
            ContentProfile.ContentType.CONTENT_TYPE_PROTON,
            ContentProfile.ContentType.CONTENT_TYPE_DXVK,
            ContentProfile.ContentType.CONTENT_TYPE_VKD3D,
            ContentProfile.ContentType.CONTENT_TYPE_VULKAN_SDK,
            ContentProfile.ContentType.CONTENT_TYPE_TURNIP_DRIVER,
            ContentProfile.ContentType.CONTENT_TYPE_OPENGL_DRIVER,
            ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO,
            ContentProfile.ContentType.CONTENT_TYPE_BOX64,
            ContentProfile.ContentType.CONTENT_TYPE_WOWBOX64,
            ContentProfile.ContentType.CONTENT_TYPE_FEXCORE
    );

    private RecyclerView recyclerView;
    private View emptyText;
    private ContentsManager manager;
    private SharedPreferences sharedPreferences;
    private ContentProfile.ContentType currentContentType = ContentProfile.ContentType.CONTENT_TYPE_WINE;
    private Spinner sContentType;
    private boolean isDarkMode;
    private CheckBox cbSourceWcpHub;
    private CheckBox cbSourceFallback;
    private CheckBox cbSourceAesolator;
    private CheckBox cbFilterArm64ec;
    private CheckBox cbFilterX64;
    private CheckBox cbFilterBeta;
    private boolean sourceWcpHubEnabled;
    private boolean sourceFallbackEnabled;
    private boolean sourceAesolatorEnabled;
    private boolean filterArm64ecEnabled;
    private boolean filterX64Enabled;
    private boolean filterBetaEnabled;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        manager = new ContentsManager(getContext());
        manager.syncContents();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        sourceWcpHubEnabled = sharedPreferences.getBoolean("contents_source_wcphub", true);
        sourceFallbackEnabled = sharedPreferences.getBoolean("contents_source_fallback", true);
        sourceAesolatorEnabled = sharedPreferences.getBoolean("contents_source_aesolator", true);
        filterArm64ecEnabled = sharedPreferences.getBoolean("contents_filter_arm64ec", true);
        filterX64Enabled = sharedPreferences.getBoolean("contents_filter_x64", true);
        filterBetaEnabled = sharedPreferences.getBoolean("contents_filter_beta", false);
    }

    @Override
    public void onDestroy() {
        FileUtils.clear(requireContext().getCacheDir());
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadRemoteContents();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle(R.string.contents);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.contents_fragment, container, false);

        sContentType = layout.findViewById(R.id.SContentType);
        updateContentTypeSpinner(sContentType);
        sContentType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentContentType = SUPPORTED_CONTENT_TYPES.get(position);
                if (emptyText != null && recyclerView != null) {
                    loadContentList();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        applyPreferredContentTypeSelection();

        cbSourceWcpHub = layout.findViewById(R.id.CBSourceWcpHub);
        cbSourceFallback = layout.findViewById(R.id.CBSourceFallback);
        cbSourceAesolator = layout.findViewById(R.id.CBSourceAesolator);
        cbFilterArm64ec = layout.findViewById(R.id.CBFilterArm64ec);
        cbFilterX64 = layout.findViewById(R.id.CBFilterX64);
        cbFilterBeta = layout.findViewById(R.id.CBFilterBeta);

        cbSourceWcpHub.setChecked(sourceWcpHubEnabled);
        cbSourceFallback.setChecked(sourceFallbackEnabled);
        cbSourceAesolator.setChecked(sourceAesolatorEnabled);
        cbFilterArm64ec.setChecked(filterArm64ecEnabled);
        cbFilterX64.setChecked(filterX64Enabled);
        cbFilterBeta.setChecked(filterBetaEnabled);

        CompoundButton.OnCheckedChangeListener refreshRemoteListener = (buttonView, isChecked) -> {
            updateFilterPreferencesFromUi();
            reloadRemoteContents();
        };
        CompoundButton.OnCheckedChangeListener refreshListListener = (buttonView, isChecked) -> {
            updateFilterPreferencesFromUi();
            loadContentList();
        };

        cbSourceWcpHub.setOnCheckedChangeListener(refreshRemoteListener);
        cbSourceFallback.setOnCheckedChangeListener(refreshRemoteListener);
        cbSourceAesolator.setOnCheckedChangeListener(refreshRemoteListener);
        cbFilterArm64ec.setOnCheckedChangeListener(refreshListListener);
        cbFilterX64.setOnCheckedChangeListener(refreshListListener);
        cbFilterBeta.setOnCheckedChangeListener(refreshListListener);

        emptyText = layout.findViewById(R.id.TVEmptyText);

        View btInstallContent = layout.findViewById(R.id.BTInstallContent);
        btInstallContent.setOnClickListener(v -> ContentDialog.confirm(
                getContext(),
                getString(R.string.do_you_want_to_install_content) + " "
                        + getString(R.string.pls_make_sure_content_trustworthy) + " "
                        + getString(R.string.content_suffix_is_wcp_packed_xz_zst),
                () -> {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                    requireActivity().startActivityFromFragment(this, intent, MainActivity.OPEN_FILE_REQUEST_CODE);
                }
        ));

        recyclerView = layout.findViewById(R.id.RecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));
        loadContentList();

        return layout;
    }

    private void updateContentTypeSpinner(Spinner spinner) {
        List<String> typeList = new ArrayList<>();
        for (ContentProfile.ContentType type : SUPPORTED_CONTENT_TYPES) {
            typeList.add(getTypeLabel(type));
        }
        spinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, typeList));
        spinner.setPopupBackgroundResource(isDarkMode ? R.drawable.content_dialog_background_dark : R.drawable.content_dialog_background);
    }

    private String getTypeLabel(ContentProfile.ContentType type) {
        if (type == ContentProfile.ContentType.CONTENT_TYPE_PROTON) return "Proton";
        if (type == ContentProfile.ContentType.CONTENT_TYPE_VULKAN_SDK) return "Vulkan SDK";
        if (type == ContentProfile.ContentType.CONTENT_TYPE_TURNIP_DRIVER) return "Turnip";
        if (type == ContentProfile.ContentType.CONTENT_TYPE_OPENGL_DRIVER) return "OpenGL Driver";
        if (type == ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO) return "dgVoodoo";
        return type.toString();
    }

    private boolean isProtonLike(ContentProfile profile) {
        if (profile.type == ContentProfile.ContentType.CONTENT_TYPE_PROTON) return true;
        String line = ((profile.verName == null ? "" : profile.verName) + " "
                + (profile.desc == null ? "" : profile.desc)).toLowerCase(Locale.US);
        return line.contains("proton");
    }

    private List<ContentProfile> getVisibleProfiles() {
        List<ContentProfile> profiles = manager.getProfiles(currentContentType);
        if (profiles == null) return new ArrayList<>();

        ArrayList<ContentProfile> filtered = new ArrayList<>();
        for (ContentProfile profile : profiles) {
            if (currentContentType == ContentProfile.ContentType.CONTENT_TYPE_WINE && isProtonLike(profile)) {
                continue;
            }
            if (currentContentType == ContentProfile.ContentType.CONTENT_TYPE_PROTON && !isProtonLike(profile)) {
                continue;
            }
            if (!filterBetaEnabled && profile.isBetaLike()) {
                continue;
            }
            if (isGraphicsStackType(currentContentType)) {
                boolean arm64ec = isArm64EcProfile(profile);
                boolean x64 = isX64Profile(profile);
                if (arm64ec && !filterArm64ecEnabled) continue;
                if (x64 && !filterX64Enabled) continue;
            }
            filtered.add(profile);
        }
        return filtered;
    }

    private void updateFilterPreferencesFromUi() {
        sourceWcpHubEnabled = cbSourceWcpHub != null && cbSourceWcpHub.isChecked();
        sourceFallbackEnabled = cbSourceFallback != null && cbSourceFallback.isChecked();
        sourceAesolatorEnabled = cbSourceAesolator != null && cbSourceAesolator.isChecked();
        filterArm64ecEnabled = cbFilterArm64ec != null && cbFilterArm64ec.isChecked();
        filterX64Enabled = cbFilterX64 != null && cbFilterX64.isChecked();
        filterBetaEnabled = cbFilterBeta != null && cbFilterBeta.isChecked();

        sharedPreferences.edit()
                .putBoolean("contents_source_wcphub", sourceWcpHubEnabled)
                .putBoolean("contents_source_fallback", sourceFallbackEnabled)
                .putBoolean("contents_source_aesolator", sourceAesolatorEnabled)
                .putBoolean("contents_filter_arm64ec", filterArm64ecEnabled)
                .putBoolean("contents_filter_x64", filterX64Enabled)
                .putBoolean("contents_filter_beta", filterBetaEnabled)
                .apply();
    }

    private void applyPreferredContentTypeSelection() {
        String preferredType = sharedPreferences.getString("contents_preselected_type", "");
        if (preferredType == null || preferredType.trim().isEmpty()) return;
        ContentProfile.ContentType preferred = ContentProfile.ContentType.getTypeByName(preferredType.trim());
        if (preferred == null) return;
        int index = SUPPORTED_CONTENT_TYPES.indexOf(preferred);
        if (index >= 0) {
            sContentType.setSelection(index);
            currentContentType = preferred;
        }
        sharedPreferences.edit().remove("contents_preselected_type").apply();
    }

    private boolean isGraphicsStackType(ContentProfile.ContentType type) {
        return type == ContentProfile.ContentType.CONTENT_TYPE_DXVK
                || type == ContentProfile.ContentType.CONTENT_TYPE_VKD3D
                || type == ContentProfile.ContentType.CONTENT_TYPE_VULKAN_SDK
                || type == ContentProfile.ContentType.CONTENT_TYPE_TURNIP_DRIVER
                || type == ContentProfile.ContentType.CONTENT_TYPE_OPENGL_DRIVER
                || type == ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO;
    }

    private boolean isArm64EcProfile(ContentProfile profile) {
        if (profile == null) return false;
        String combined = (
                (profile.verName == null ? "" : profile.verName) + " " +
                        (profile.desc == null ? "" : profile.desc) + " " +
                        (profile.remoteUrl == null ? "" : profile.remoteUrl) + " " +
                        (profile.releaseTag == null ? "" : profile.releaseTag)
        ).toLowerCase(Locale.US);
        return combined.contains("arm64ec") || combined.contains("arm64-ec");
    }

    private boolean isX64Profile(ContentProfile profile) {
        if (profile == null) return false;
        String combined = (
                (profile.verName == null ? "" : profile.verName) + " " +
                        (profile.desc == null ? "" : profile.desc) + " " +
                        (profile.remoteUrl == null ? "" : profile.remoteUrl) + " " +
                        (profile.releaseTag == null ? "" : profile.releaseTag)
        ).toLowerCase(Locale.US);
        return combined.contains("x86_64") || combined.contains("x86-64") || combined.contains("amd64");
    }

    private void reloadRemoteContents() {
        new Thread(() -> {
            try {
                ArrayList<String> payloads = new ArrayList<>();
                HashSet<String> sources = new HashSet<>();
                boolean useWcpHub = sourceWcpHubEnabled;
                boolean useFallback = sourceFallbackEnabled;
                boolean useAesolator = sourceAesolatorEnabled;

                String preferredUrl = sharedPreferences.getString("downloadable_contents_url", ContentsManager.REMOTE_PROFILES);
                if (preferredUrl != null && !preferredUrl.trim().isEmpty()
                        && !ContentsManager.REMOTE_PROFILES.equals(preferredUrl.trim())
                        && !ContentsManager.REMOTE_PROFILES_FALLBACK.equals(preferredUrl.trim())
                        && !ContentsManager.REMOTE_PROFILES_AE.equals(preferredUrl.trim())) {
                    addRemoteFeed(payloads, sources, preferredUrl);
                }
                if (useWcpHub) addRemoteFeed(payloads, sources, ContentsManager.REMOTE_PROFILES);
                if (useFallback) addRemoteFeed(payloads, sources, ContentsManager.REMOTE_PROFILES_FALLBACK);
                if (useAesolator) addRemoteFeed(payloads, sources, ContentsManager.REMOTE_PROFILES_AE);

                if (!isAdded() || getActivity() == null) return;
                if (payloads.isEmpty()) {
                    getActivity().runOnUiThread(() -> {
                        manager.setRemoteProfiles("[]");
                        loadContentList();
                    });
                    return;
                }

                String merged = mergeJsonArrays(payloads);
                getActivity().runOnUiThread(() -> {
                    manager.setRemoteProfiles(merged);
                    loadContentList();
                });
            } catch (Exception ignored) {
            }
        }).start();
    }

    private void addRemoteFeed(List<String> payloads, HashSet<String> seenSources, @Nullable String url) {
        if (url == null) return;
        String normalized = url.trim();
        if (normalized.isEmpty() || seenSources.contains(normalized)) return;
        seenSources.add(normalized);
        String json = Downloader.downloadString(normalized);
        if (json != null && !json.trim().isEmpty()) payloads.add(json);
    }

    private String mergeJsonArrays(List<String> payloads) {
        JSONArray merged = new JSONArray();
        HashSet<String> seenEntries = new HashSet<>();
        for (String payload : payloads) {
            try {
                JSONArray array = new JSONArray(payload);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject object = array.getJSONObject(i);
                    String type = object.optString("type", "");
                    String verName = object.optString("verName", "");
                    String verCode = String.valueOf(object.opt("verCode"));
                    String remoteUrl = object.optString("remoteUrl", "");
                    String key = (type + "|" + verName + "|" + verCode + "|" + remoteUrl).toLowerCase(Locale.US);
                    if (seenEntries.add(key)) {
                        merged.put(object);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return merged.toString();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == MainActivity.OPEN_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            PreloaderDialog preloaderDialog = new PreloaderDialog(requireActivity());
            preloaderDialog.showOnUiThread(R.string.installing_content);
            try {
                ContentsManager.OnInstallFinishedCallback callback = new ContentsManager.OnInstallFinishedCallback() {
                    private boolean isExtracting = true;

                    @Override
                    public void onFailed(ContentsManager.InstallFailedReason reason, Exception e) {
                        int msgId = switch (reason) {
                            case ERROR_BADTAR -> R.string.file_cannot_be_recognied;
                            case ERROR_NOPROFILE -> R.string.profile_not_found_in_content;
                            case ERROR_BADPROFILE -> R.string.profile_cannot_be_recognized;
                            case ERROR_EXIST -> R.string.content_already_exist;
                            case ERROR_MISSINGFILES -> R.string.content_is_incomplete;
                            case ERROR_UNTRUSTPROFILE -> R.string.content_cannot_be_trusted;
                            default -> R.string.unable_to_install_content;
                        };
                        requireActivity().runOnUiThread(() -> ContentDialog.alert(
                                getContext(),
                                getString(R.string.install_failed) + ": " + getString(msgId),
                                preloaderDialog::closeOnUiThread
                        ));
                    }

                    @Override
                    public void onSucceed(ContentProfile profile) {
                        if (isExtracting) {
                            ContentsManager.OnInstallFinishedCallback cb = this;
                            requireActivity().runOnUiThread(() -> {
                                ContentInfoDialog dialog = new ContentInfoDialog(getContext(), profile);
                                ((TextView) dialog.findViewById(R.id.BTConfirm)).setText(R.string._continue);
                                dialog.setOnConfirmCallback(() -> {
                                    isExtracting = false;
                                    List<ContentProfile.ContentFile> untrustedFiles = manager.getUnTrustedContentFiles(profile);
                                    if (!untrustedFiles.isEmpty()) {
                                        ContentUntrustedDialog untrustedDialog = new ContentUntrustedDialog(getContext(), untrustedFiles);
                                        untrustedDialog.setOnCancelCallback(preloaderDialog::closeOnUiThread);
                                        untrustedDialog.setOnConfirmCallback(() -> manager.finishInstallContent(profile, cb));
                                        untrustedDialog.show();
                                    } else {
                                        manager.finishInstallContent(profile, cb);
                                    }
                                });
                                dialog.setOnCancelCallback(preloaderDialog::closeOnUiThread);
                                dialog.show();
                            });
                        } else {
                            preloaderDialog.closeOnUiThread();
                            requireActivity().runOnUiThread(() -> {
                                ContentDialog.alert(getContext(), R.string.content_installed_success, null);
                                manager.syncContents();
                                loadContentList();
                            });
                        }
                    }
                };
                Executors.newSingleThreadExecutor().execute(() -> manager.extraContentFile(data.getData(), callback));
            } catch (Exception e) {
                preloaderDialog.closeOnUiThread();
                AppUtils.showToast(getContext(), R.string.unable_to_import_profile);
            }
        }
    }

    private void loadContentList() {
        List<ContentProfile> profiles = getVisibleProfiles();
        if (profiles.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setAdapter(new ContentItemAdapter(profiles));
        }
    }

    private String getSourceLabel(ContentProfile profile) {
        if (profile.remoteUrl == null || profile.remoteUrl.trim().isEmpty()) {
            return "Local package";
        }
        try {
            Uri uri = Uri.parse(profile.remoteUrl);
            String host = uri.getHost();
            if (host != null && !host.isEmpty()) {
                return "Remote: " + host;
            }
        } catch (Exception ignored) {
        }
        return "Remote package";
    }

    private String buildProfileTitleLine(ContentProfile profile) {
        String versionName = profile.verName != null ? profile.verName.trim() : "";
        if (!versionName.isEmpty()) return versionName;
        return getDisplayTypeLabel(profile.type);
    }

    private String buildProfileMetaLine(ContentProfile profile) {
        StringBuilder meta = new StringBuilder(getString(R.string.version_code) + ": " + profile.verCode);
        if (profile.isBetaLike()) meta.append(" • beta");
        if (profile.isInstalledLocally()) meta.append(" • installed");
        return meta.toString();
    }

    private String buildProfileSourceLine(ContentProfile profile) {
        String sourceLabel = getSourceLabel(profile);
        String repo = profile.sourceRepo != null ? profile.sourceRepo.trim() : "";
        String tag = profile.releaseTag != null ? profile.releaseTag.trim() : "";
        if (!repo.isEmpty() && !tag.isEmpty()) return sourceLabel + " • " + repo + "@" + tag;
        if (!repo.isEmpty()) return sourceLabel + " • " + repo;
        if (!tag.isEmpty()) return sourceLabel + " • " + tag;
        return sourceLabel;
    }

    private String getDisplayTypeLabel(ContentProfile.ContentType type) {
        return getTypeLabel(type);
    }

    private int resolveProfileAccentColor(ContentProfile profile) {
        int fallbackRes = isDarkMode ? R.color.colorAccentDark : R.color.colorAccent;
        int colorRes = switch (profile.type) {
            case CONTENT_TYPE_WINE -> isDarkMode ? R.color.contents_lane_wine_dark : R.color.contents_lane_wine;
            case CONTENT_TYPE_PROTON -> isDarkMode ? R.color.contents_lane_proton_dark : R.color.contents_lane_proton;
            case CONTENT_TYPE_VULKAN_SDK -> resolveVulkanFamilyAccentColor(profile);
            case CONTENT_TYPE_TURNIP_DRIVER -> isDarkMode ? R.color.contents_lane_turnip_dark : R.color.contents_lane_turnip;
            case CONTENT_TYPE_OPENGL_DRIVER -> isDarkMode ? R.color.contents_lane_opengl_dark : R.color.contents_lane_opengl;
            case CONTENT_TYPE_DGVOODOO -> isDarkMode ? R.color.contents_lane_dgvoodoo_dark : R.color.contents_lane_dgvoodoo;
            case CONTENT_TYPE_DXVK -> isDarkMode ? R.color.contents_lane_dxvk_dark : R.color.contents_lane_dxvk;
            case CONTENT_TYPE_VKD3D -> isDarkMode ? R.color.contents_lane_vkd3d_dark : R.color.contents_lane_vkd3d;
            default -> fallbackRes;
        };
        return ContextCompat.getColor(requireContext(), colorRes);
    }

    private int resolveVulkanFamilyAccentColor(ContentProfile profile) {
        String category = profile.getDisplayCategory();
        String source = profile.sourceRepo != null ? profile.sourceRepo : "";
        String version = profile.verName != null ? profile.verName : "";
        String laneHint = (category + " " + source + " " + version).toLowerCase(Locale.US);
        if (laneHint.contains("turnip")) {
            return isDarkMode ? R.color.contents_lane_turnip_dark : R.color.contents_lane_turnip;
        }
        if (laneHint.contains("opengl") || laneHint.contains("zink")) {
            return isDarkMode ? R.color.contents_lane_opengl_dark : R.color.contents_lane_opengl;
        }
        return isDarkMode ? R.color.contents_lane_vulkansdk_dark : R.color.contents_lane_vulkansdk;
    }

    private GradientDrawable buildCategoryBadgeBackground(int accentColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dpToPx(10f));
        drawable.setColor(withAlpha(accentColor, isDarkMode ? 74 : 36));
        drawable.setStroke(dpToPx(1f), withAlpha(accentColor, isDarkMode ? 220 : 132));
        return drawable;
    }

    private int withAlpha(int color, int alpha) {
        int clampedAlpha = Math.max(0, Math.min(255, alpha));
        return (color & 0x00ffffff) | (clampedAlpha << 24);
    }

    private int dpToPx(float dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    private class ContentItemAdapter extends RecyclerView.Adapter<ContentItemAdapter.ViewHolder> {
        private final List<ContentProfile> data;

        private class ViewHolder extends RecyclerView.ViewHolder {
            private final ImageView ivIcon;
            private final TextView tvVersionName;
            private final TextView tvCategoryBadge;
            private final TextView tvVersionCode;
            private final TextView tvDescription;
            private final TextView tvSource;
            private final ImageButton ibMenu;
            private final ImageButton ibDownload;
            private final ProgressBar progressBar;

            private ViewHolder(@NonNull View view) {
                super(view);
                ivIcon = view.findViewById(R.id.IVIcon);
                tvVersionName = view.findViewById(R.id.TVVersionName);
                tvCategoryBadge = view.findViewById(R.id.TVCategoryBadge);
                tvVersionCode = view.findViewById(R.id.TVVersionCode);
                tvDescription = view.findViewById(R.id.TVDescription);
                tvSource = view.findViewById(R.id.TVSource);
                ibMenu = view.findViewById(R.id.BTMenu);
                ibDownload = view.findViewById(R.id.BTDownload);
                progressBar = view.findViewById(R.id.Progress);
            }
        }

        private ContentItemAdapter(List<ContentProfile> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.content_list_item, parent, false));
        }

        @Override
        public void onViewRecycled(@NonNull ViewHolder holder) {
            holder.ibMenu.setOnClickListener(null);
            holder.ibDownload.setOnClickListener(null);
            super.onViewRecycled(holder);
        }

        @SuppressLint("StringFormatInvalid")
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final ContentProfile profile = data.get(position);
            int iconId = switch (profile.type) {
                case CONTENT_TYPE_WINE, CONTENT_TYPE_PROTON -> R.drawable.icon_wine;
                case CONTENT_TYPE_DXVK,
                     CONTENT_TYPE_VKD3D,
                     CONTENT_TYPE_VULKAN_SDK,
                     CONTENT_TYPE_TURNIP_DRIVER,
                     CONTENT_TYPE_OPENGL_DRIVER,
                     CONTENT_TYPE_DGVOODOO -> R.drawable.icon_open;
                default -> R.drawable.icon_settings;
            };
            holder.ivIcon.setImageResource(iconId);
            int accentColor = resolveProfileAccentColor(profile);
            int secondaryColor = withAlpha(accentColor, isDarkMode ? 228 : 176);
            holder.ivIcon.setColorFilter(accentColor);

            holder.tvVersionName.setText(buildProfileTitleLine(profile));
            holder.tvVersionName.setTextColor(accentColor);

            String categoryBadgeText = profile.getDisplayCategory();
            if (categoryBadgeText == null || categoryBadgeText.trim().isEmpty()) {
                categoryBadgeText = getDisplayTypeLabel(profile.type);
            }
            holder.tvCategoryBadge.setText(categoryBadgeText);
            holder.tvCategoryBadge.setTextColor(accentColor);
            holder.tvCategoryBadge.setBackground(buildCategoryBadgeBackground(accentColor));
            holder.tvCategoryBadge.setVisibility(View.VISIBLE);

            holder.tvVersionCode.setText(buildProfileMetaLine(profile));
            holder.tvVersionCode.setTextColor(secondaryColor);
            holder.tvDescription.setText((profile.desc == null || profile.desc.trim().isEmpty()) ? getTypeLabel(profile.type) : profile.desc);
            holder.tvDescription.setTextColor(secondaryColor);
            holder.tvSource.setText(buildProfileSourceLine(profile));
            holder.tvSource.setTextColor(secondaryColor);

            holder.ibMenu.setVisibility(profile.remoteUrl == null ? View.VISIBLE : View.GONE);
            holder.ibMenu.setOnClickListener(v -> {
                PopupMenu selectionMenu = new PopupMenu(getContext(), holder.ibMenu);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) selectionMenu.setForceShowIcon(true);
                selectionMenu.inflate(R.menu.content_popup_menu);
                selectionMenu.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.content_info) {
                        new ContentInfoDialog(getContext(), profile).show();
                    } else if (itemId == R.id.remove_content) {
                        ContentDialog.confirm(getContext(), R.string.do_you_want_to_remove_this_content, () -> {
                            if (profile.type == ContentProfile.ContentType.CONTENT_TYPE_WINE
                                    || profile.type == ContentProfile.ContentType.CONTENT_TYPE_PROTON) {
                                ContainerManager containerManager = new ContainerManager(getContext());
                                for (Container container : containerManager.getContainers()) {
                                    if (container.getWineVersion().equals(ContentsManager.getEntryName(profile))) {
                                        ContentDialog.alert(getContext(), String.format(
                                                getString(R.string.unable_to_remove_content_since_container_using),
                                                container.getName()
                                        ), null);
                                        return;
                                    }
                                }
                            }
                            manager.removeContent(profile);
                            loadContentList();
                        });
                    }
                    return true;
                });
                selectionMenu.show();
            });

            holder.ibDownload.setVisibility((profile.remoteUrl != null) && (holder.progressBar.getVisibility() == View.GONE) ? View.VISIBLE : View.GONE);
            holder.ibDownload.setOnClickListener(v -> {
                holder.ibDownload.setVisibility(View.GONE);
                holder.progressBar.setVisibility(View.VISIBLE);

                Intent intent = new Intent();
                intent.setData(Uri.parse(profile.remoteUrl));
                new Thread(() -> {
                    long timestamp = System.currentTimeMillis();
                    File output = new File(requireContext().getCacheDir(), "temp_" + timestamp);
                    if (Downloader.downloadFile(profile.remoteUrl, output)) {
                        intent.setData(Uri.parse(output.getAbsolutePath()));
                    }
                    requireActivity().runOnUiThread(() -> {
                        holder.progressBar.setVisibility(View.GONE);
                        holder.ibDownload.setVisibility(View.VISIBLE);
                        onActivityResult(MainActivity.OPEN_FILE_REQUEST_CODE, Activity.RESULT_OK, intent);
                    });
                }).start();
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
}
