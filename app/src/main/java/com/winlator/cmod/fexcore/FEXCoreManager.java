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
import com.winlator.cmod.core.SpinnerAdapters;

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

        for (String version : contentsManager.getInstalledVersionNames(ContentProfile.ContentType.CONTENT_TYPE_FEXCORE, true)) {
            if (!itemList.contains(version)) {
                itemList.add(version);
            }
        }

        boolean hasVersions = !itemList.isEmpty();
        if (!hasVersions) itemList.add(AppUtils.MISSING_COMPONENT_PLACEHOLDER);

        spinner.setAdapter(SpinnerAdapters.create(context, itemList));
        spinner.setEnabled(hasVersions);
        if (hasVersions) {
            AppUtils.setSpinnerSelectionFromValue(spinner, fexcoreVersion);
        } else {
            spinner.setSelection(0, false);
        }
    }
}
