package com.winlator.cmod;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        manager = new ContentsManager(getContext());
        manager.syncContents();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
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
                loadContentList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

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

        if (currentContentType == ContentProfile.ContentType.CONTENT_TYPE_WINE
                || currentContentType == ContentProfile.ContentType.CONTENT_TYPE_PROTON) {
            ArrayList<ContentProfile> filtered = new ArrayList<>();
            for (ContentProfile profile : profiles) {
                if (currentContentType == ContentProfile.ContentType.CONTENT_TYPE_WINE && !isProtonLike(profile)) {
                    filtered.add(profile);
                } else if (currentContentType == ContentProfile.ContentType.CONTENT_TYPE_PROTON && isProtonLike(profile)) {
                    filtered.add(profile);
                }
            }
            return filtered;
        }
        return profiles;
    }

    private void reloadRemoteContents() {
        new Thread(() -> {
            try {
                ArrayList<String> payloads = new ArrayList<>();
                HashSet<String> sources = new HashSet<>();

                String preferredUrl = sharedPreferences.getString("downloadable_contents_url", ContentsManager.REMOTE_PROFILES);
                addRemoteFeed(payloads, sources, preferredUrl);
                addRemoteFeed(payloads, sources, ContentsManager.REMOTE_PROFILES);
                addRemoteFeed(payloads, sources, ContentsManager.REMOTE_PROFILES_FALLBACK);
                addRemoteFeed(payloads, sources, ContentsManager.REMOTE_PROFILES_AE);

                if (payloads.isEmpty() || !isAdded() || getActivity() == null) return;

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

    private class ContentItemAdapter extends RecyclerView.Adapter<ContentItemAdapter.ViewHolder> {
        private final List<ContentProfile> data;

        private class ViewHolder extends RecyclerView.ViewHolder {
            private final ImageView ivIcon;
            private final TextView tvVersionName;
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
                case CONTENT_TYPE_DXVK, CONTENT_TYPE_VKD3D, CONTENT_TYPE_VULKAN_SDK -> R.drawable.icon_open;
                default -> R.drawable.icon_settings;
            };
            holder.ivIcon.setImageResource(iconId);

            holder.tvVersionName.setText(getString(R.string.version) + ": " + profile.verName);
            holder.tvVersionCode.setText(getString(R.string.version_code) + ": " + profile.verCode);
            holder.tvDescription.setText((profile.desc == null || profile.desc.trim().isEmpty()) ? getTypeLabel(profile.type) : profile.desc);
            holder.tvSource.setText(getSourceLabel(profile));

            if (isDarkMode) {
                holder.tvDescription.setTextColor(0xFF6FE4A4);
                holder.tvSource.setTextColor(0xFFA5B5AF);
            }

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
