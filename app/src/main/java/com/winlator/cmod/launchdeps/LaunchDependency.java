package com.winlator.cmod.launchdeps;

import android.content.Context;

import androidx.annotation.Nullable;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.Shortcut;

public interface LaunchDependency {
    String getId();

    boolean appliesTo(Container container, @Nullable Shortcut shortcut, @Nullable String appId);

    boolean isSatisfied(Context context, Container container, @Nullable Shortcut shortcut, @Nullable String appId);

    String getLoadingMessage(Context context, Container container, @Nullable Shortcut shortcut, @Nullable String appId);

    void install(
            Context context,
            Container container,
            @Nullable Shortcut shortcut,
            @Nullable String appId,
            LaunchDependencyCallbacks callbacks
    ) throws Exception;
}
