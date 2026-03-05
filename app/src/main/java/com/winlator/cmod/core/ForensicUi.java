package com.winlator.cmod.core;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.winlator.cmod.R;
import com.winlator.cmod.contentdialog.ContentDialog;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;

public final class ForensicUi {
    private ForensicUi() {}

    public static void renderWineDebugChannels(Fragment fragment, LinearLayout container, ArrayList<String> debugChannels, Runnable onChanged) {
        Context context = fragment.requireContext();
        container.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView = inflater.inflate(R.layout.wine_debug_channel_list_item, container, false);
        itemView.findViewById(R.id.TextView).setVisibility(View.GONE);
        itemView.findViewById(R.id.BTRemove).setVisibility(View.GONE);

        View addButton = itemView.findViewById(R.id.BTAdd);
        addButton.setVisibility(View.VISIBLE);
        addButton.setOnClickListener(v -> {
            JSONArray jsonArray = null;
            try {
                jsonArray = new JSONArray(FileUtils.readString(context, "wine_debug_channels.json"));
            }
            catch (Exception ignored) {}

            final String[] items = ArrayUtils.toStringArray(jsonArray);
            ContentDialog.showMultipleChoiceList(context, R.string.wine_debug_channel, items, selectedPositions -> {
                for (int selectedPosition : selectedPositions) {
                    if (selectedPosition >= 0 && selectedPosition < items.length && !debugChannels.contains(items[selectedPosition])) {
                        debugChannels.add(items[selectedPosition]);
                    }
                }
                renderWineDebugChannels(fragment, container, debugChannels, onChanged);
                if (onChanged != null) onChanged.run();
            });
        });

        View resetButton = itemView.findViewById(R.id.BTReset);
        resetButton.setVisibility(View.VISIBLE);
        resetButton.setOnClickListener(v -> {
            debugChannels.clear();
            debugChannels.addAll(Arrays.asList(ForensicConfig.DEFAULT_WINE_DEBUG_CHANNELS.split(",")));
            renderWineDebugChannels(fragment, container, debugChannels, onChanged);
            if (onChanged != null) onChanged.run();
        });
        container.addView(itemView);

        for (int i = 0; i < debugChannels.size(); i++) {
            itemView = inflater.inflate(R.layout.wine_debug_channel_list_item, container, false);
            TextView textView = itemView.findViewById(R.id.TextView);
            textView.setText(debugChannels.get(i));
            final int index = i;
            itemView.findViewById(R.id.BTRemove).setOnClickListener(v -> {
                debugChannels.remove(index);
                renderWineDebugChannels(fragment, container, debugChannels, onChanged);
                if (onChanged != null) onChanged.run();
            });
            container.addView(itemView);
        }
    }
}
