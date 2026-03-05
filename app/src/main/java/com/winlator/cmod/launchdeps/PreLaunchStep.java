package com.winlator.cmod.launchdeps;

import android.content.Context;

import androidx.annotation.Nullable;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.Shortcut;
import com.winlator.cmod.xenvironment.components.GuestProgramLauncherComponent;

public interface PreLaunchStep {
    String getId();

    boolean appliesTo(Container container, @Nullable Shortcut shortcut, @Nullable String appId);

    void run(
            Context context,
            Container container,
            @Nullable Shortcut shortcut,
            @Nullable String appId,
            GuestProgramLauncherComponent launcher
    ) throws Exception;
}
