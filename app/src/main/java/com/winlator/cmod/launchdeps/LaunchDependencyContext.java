package com.winlator.cmod.launchdeps;

import android.content.Context;

import com.winlator.cmod.contents.ManifestData;
import com.winlator.cmod.contents.ManifestRepository;
import com.winlator.cmod.contents.ContentsManager;

public final class LaunchDependencyContext {
    private final Context context;
    private final ContentsManager contentsManager;
    private ManifestData manifest;

    public LaunchDependencyContext(Context context, ContentsManager contentsManager) {
        this.context = context;
        this.contentsManager = contentsManager;
    }

    public Context getContext() {
        return context;
    }

    public ContentsManager getContentsManager() {
        return contentsManager;
    }

    public ManifestData getManifest() {
        if (manifest == null) {
            manifest = ManifestRepository.loadManifest(context);
        }
        return manifest;
    }
}
