package com.winlator.cmod.gamefixes;

import android.content.Context;

import androidx.annotation.Nullable;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.Shortcut;
import com.winlator.cmod.launchdeps.PreLaunchStep;
import com.winlator.cmod.xenvironment.components.GuestProgramLauncherComponent;

public final class GameFixesPreLaunchStep implements PreLaunchStep {
    @Override
    public String getId() {
        return "gamefixes_registry";
    }

    @Override
    public boolean appliesTo(Container container, @Nullable Shortcut shortcut, @Nullable String appId) {
        return container != null && GameFixesRegistry.hasFixFor(appId, container);
    }

    @Override
    public void run(
            Context context,
            Container container,
            @Nullable Shortcut shortcut,
            @Nullable String appId,
            GuestProgramLauncherComponent launcher
    ) {
        GameFixesRegistry.applyFor(context, appId, container, shortcut);
    }
}
