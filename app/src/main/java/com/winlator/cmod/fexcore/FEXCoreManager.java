package com.winlator.cmod.fexcore;

import com.winlator.cmod.R;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.EnvVars;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.KeyValueSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class FEXCoreManager {
    public static void loadFEXCoreVersion(Context context, ContentsManager contentsManager, Spinner spinner, String fexcoreVersion) {
        List<String> itemList = new ArrayList<>();

        String[] originalItems = context.getResources().getStringArray(R.array.fexcore_version_entries);
        for (String version : originalItems) {
            if (FileUtils.getSize(context, "fexcore/fexcore-" + version + ".tzst") > 0) {
                itemList.add(version);
            }
        }

        List<ContentProfile> profiles = contentsManager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_FEXCORE);
        if (profiles != null) {
            for (ContentProfile profile : profiles) {
                if (profile == null || !profile.locallyInstalled) continue;
                if (profile.verName == null || profile.verName.trim().isEmpty()) continue;
                if (!itemList.contains(profile.verName)) {
                    itemList.add(profile.verName);
                }
            }
        }

        boolean hasVersions = !itemList.isEmpty();
        if (!hasVersions) itemList.add(AppUtils.MISSING_COMPONENT_PLACEHOLDER);

        spinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, itemList));
        spinner.setEnabled(hasVersions);
        if (hasVersions) {
            AppUtils.setSpinnerSelectionFromValue(spinner, fexcoreVersion);
        } else {
            spinner.setSelection(0, false);
        }
    }
}
