package com.winlator.cmod.contentdialog;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.cmod.R;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentStateUi;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.ForensicLogger;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ContentInfoDialog extends ContentDialog {
    private final int fileCount;

    public ContentInfoDialog(Context context, ContentProfile profile) {
        super(context, R.layout.content_info_dialog);
        setIcon(R.drawable.ae_icon_about);
        setTitle(R.string.content_info);

        TextView tvType = findViewById(R.id.TVType);
        TextView tvVersion = findViewById(R.id.TVVersion);
        TextView tvVersionCode = findViewById(R.id.TVVersionCode);
        TextView tvStatus = findViewById(R.id.TVStatus);
        TextView tvSource = findViewById(R.id.TVSource);
        TextView tvReleaseTag = findViewById(R.id.TVReleaseTag);
        TextView tvArtifact = findViewById(R.id.TVArtifact);
        TextView tvChannel = findViewById(R.id.TVChannel);
        TextView tvPublishedAt = findViewById(R.id.TVPublishedAt);
        TextView tvDescription = findViewById(R.id.TVDesc);
        TextView tvReleaseNotes = findViewById(R.id.TVReleaseNotes);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        ContentsManager manager = new ContentsManager(context);
        manager.syncContents();

        tvType.setText(resolveTypeLabel(profile));
        tvVersion.setText(profile.verName);
        tvVersionCode.setText(String.valueOf(profile.verCode));
        tvDescription.setText(profile.desc);
        ContentsManager.InstalledProfileDiagnostics diagnostics = manager.resolveInstalledProfileDiagnostics(profile);
        String statusText = ContentStateUi.getStatusLabel(context, manager, profile, true);
        String statusSummary = manager.buildInstalledProfileUiSummary(diagnostics);
        if (!statusSummary.isEmpty()) {
            statusText = statusText + "\n" + statusSummary;
        }
        bindOptionalRow(findViewById(R.id.LLInfoStatus), tvStatus, statusText);
        bindOptionalRow(findViewById(R.id.LLInfoSource), tvSource, firstNonEmpty(profile.sourceLabel, profile.sourceRepo));
        bindOptionalRow(findViewById(R.id.LLInfoReleaseTag), tvReleaseTag, profile.releaseTag);
        bindOptionalRow(findViewById(R.id.LLInfoArtifact), tvArtifact, profile.artifactName);
        bindOptionalRow(findViewById(R.id.LLInfoChannel), tvChannel, formatChannel(profile.getChannel()));
        bindOptionalRow(findViewById(R.id.LLInfoPublishedAt), tvPublishedAt, profile.publishedAt);
        bindOptionalSection(findViewById(R.id.FLReleaseNotes), tvReleaseNotes, profile.releaseNotes);
        if (diagnostics.state.isBroken()) {
            ForensicLogger.warn(
                    context,
                    "CONTENTS_BROKEN_INSTALL_OBSERVED",
                    null,
                    "contents",
                    "broken_install_observed",
                    manager.buildInstalledProfileForensicFields(diagnostics)
            );
        }

        List<ContentProfile.ContentFile> files = profile.fileList == null ? Collections.emptyList() : profile.fileList;
        fileCount = files.size();
        recyclerView.setAdapter(new ContentInfoFileAdapter(files));
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        recyclerView.setHasFixedSize(false);
        recyclerView.setItemAnimator(null);
        recyclerView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
    }

    @Override
    public void show() {
        super.show();
        if (!isShowing()) return;
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(0));
            getWindow().setLayout(
                    Math.round(AppUtils.getScreenWidth() * 0.952f),
                    Math.round(AppUtils.getScreenHeight() * 0.902f)
            );
        }
        ViewGroup.LayoutParams params = getContentView().getLayoutParams();
        if (params != null) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = Math.round(AppUtils.getScreenHeight() * 0.848f);
            getContentView().setLayoutParams(params);
        }
        getContentView().setMinimumHeight(Math.round(AppUtils.getScreenHeight() * 0.848f));
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        if (recyclerView != null) {
            ViewGroup.LayoutParams recyclerParams = recyclerView.getLayoutParams();
            if (recyclerParams != null) {
                int estimatedRowsHeight = Math.max(dp(148), fileCount * dp(56));
                recyclerParams.height = Math.min(Math.round(AppUtils.getScreenHeight() * 0.44f), estimatedRowsHeight);
                recyclerView.setLayoutParams(recyclerParams);
            }
        }
    }

    private int dp(int value) {
        return Math.round(getContext().getResources().getDisplayMetrics().density * value);
    }

    private void bindOptionalRow(View row, TextView valueView, String value) {
        if (row == null || valueView == null) return;
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            row.setVisibility(View.GONE);
            return;
        }
        row.setVisibility(View.VISIBLE);
        valueView.setText(normalized);
    }

    private void bindOptionalSection(View section, TextView valueView, String value) {
        if (section == null || valueView == null) return;
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            section.setVisibility(View.GONE);
            return;
        }
        section.setVisibility(View.VISIBLE);
        valueView.setText(normalized);
        Linkify.addLinks(valueView, Linkify.WEB_URLS);
        valueView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private String firstNonEmpty(String first, String second) {
        String normalizedFirst = first == null ? "" : first.trim();
        if (!normalizedFirst.isEmpty()) return normalizedFirst;
        return second == null ? "" : second.trim();
    }

    private String formatChannel(String channel) {
        String normalized = channel == null ? "" : channel.trim().toLowerCase(Locale.US);
        if (normalized.isEmpty()) return "";
        if ("stable".equals(normalized)) return "Stable";
        if ("beta".equals(normalized)) return "Beta";
        if ("nightly".equals(normalized)) return "Nightly";
        return normalized;
    }

    private String resolveTypeLabel(ContentProfile profile) {
        if (profile == null || profile.type == null) return "";
        return switch (profile.type) {
            case CONTENT_TYPE_WINE -> "Wine";
            case CONTENT_TYPE_PROTON -> "Proton";
            case CONTENT_TYPE_DXVK -> "DXVK";
            case CONTENT_TYPE_VKD3D -> "VKD3D-Proton";
            case CONTENT_TYPE_DGVOODOO -> "dgVoodoo";
            case CONTENT_TYPE_BOX64 -> "Box64";
            case CONTENT_TYPE_WOWBOX64 -> "WowBox64";
            case CONTENT_TYPE_FEXCORE -> "FEXCore";
            case CONTENT_TYPE_TURNIP_DRIVER -> "Turnip Driver";
            case CONTENT_TYPE_OPENGL_DRIVER -> "OpenGL Driver";
            default -> profile.type.toString();
        };
    }

    public static class ContentInfoFileAdapter extends RecyclerView.Adapter<ContentInfoFileAdapter.ViewHolder> {
        private static class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvSource;
            private final TextView tvtarget;

            private ViewHolder(View view) {
                super(view);
                tvSource = view.findViewById(R.id.TVFileSource);
                tvtarget = view.findViewById(R.id.TVFileTarget);
            }
        }

        private final List<ContentProfile.ContentFile> data;

        public ContentInfoFileAdapter(List<ContentProfile.ContentFile> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.content_file_list_item, parent, false);
            view.setBackgroundResource(R.drawable.surface_runtime_taskmgr_row_background);
            return new ContentInfoFileAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.tvSource.setText(data.get(position).source);
            holder.tvtarget.setText(data.get(position).target);
            holder.tvSource.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.surface_runtime_taskmgr_text));
            holder.tvtarget.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.surface_runtime_taskmgr_muted));
        }

        @Override
        public int getItemCount() {
            return data == null ? 0 : data.size();
        }
    }
}
