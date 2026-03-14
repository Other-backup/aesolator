package com.winlator.cmod.contentdialog;

import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.cmod.R;
import com.winlator.cmod.contents.ContentProfile;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ContentInfoDialog extends ContentDialog {
    public ContentInfoDialog(Context context, ContentProfile profile) {
        super(context, R.layout.content_info_dialog);
        setIcon(R.drawable.ae_icon_about);
        setTitle(R.string.content_info);

        TextView tvType = findViewById(R.id.TVType);
        TextView tvVersion = findViewById(R.id.TVVersion);
        TextView tvVersionCode = findViewById(R.id.TVVersionCode);
        TextView tvSource = findViewById(R.id.TVSource);
        TextView tvReleaseTag = findViewById(R.id.TVReleaseTag);
        TextView tvArtifact = findViewById(R.id.TVArtifact);
        TextView tvChannel = findViewById(R.id.TVChannel);
        TextView tvPublishedAt = findViewById(R.id.TVPublishedAt);
        TextView tvDescription = findViewById(R.id.TVDesc);
        TextView tvReleaseNotes = findViewById(R.id.TVReleaseNotes);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        tvType.setText(profile.type.toString());
        tvVersion.setText(profile.verName);
        tvVersionCode.setText(String.valueOf(profile.verCode));
        tvDescription.setText(profile.desc);
        bindOptionalRow(findViewById(R.id.LLInfoSource), tvSource, firstNonEmpty(profile.sourceLabel, profile.sourceRepo));
        bindOptionalRow(findViewById(R.id.LLInfoReleaseTag), tvReleaseTag, profile.releaseTag);
        bindOptionalRow(findViewById(R.id.LLInfoArtifact), tvArtifact, profile.artifactName);
        bindOptionalRow(findViewById(R.id.LLInfoChannel), tvChannel, formatChannel(profile.getChannel()));
        bindOptionalRow(findViewById(R.id.LLInfoPublishedAt), tvPublishedAt, profile.publishedAt);
        bindOptionalSection(findViewById(R.id.FLReleaseNotes), tvReleaseNotes, profile.releaseNotes);

        recyclerView.setAdapter(new ContentInfoFileAdapter(profile.fileList == null ? Collections.emptyList() : profile.fileList));
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
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
            return new ContentInfoFileAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.content_file_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.tvSource.setText(data.get(position).source);
            holder.tvtarget.setText(data.get(position).target);
        }

        @Override
        public int getItemCount() {
            return data == null ? 0 : data.size();
        }
    }
}
