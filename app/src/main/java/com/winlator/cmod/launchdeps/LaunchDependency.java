package com.winlator.cmod.launchdeps;

import androidx.annotation.Nullable;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.Shortcut;

public interface LaunchDependency {
    String getId();

    boolean appliesTo(Container container, @Nullable Shortcut shortcut, @Nullable String appId);

    boolean isSatisfied(LaunchDependencyContext dependencyContext, Container container, @Nullable Shortcut shortcut, @Nullable String appId);

    String getLoadingMessage(LaunchDependencyContext dependencyContext, Container container, @Nullable Shortcut shortcut, @Nullable String appId);

    void install(
            LaunchDependencyContext dependencyContext,
            Container container,
            @Nullable Shortcut shortcut,
            @Nullable String appId,
            LaunchDependencyCallbacks callbacks
    ) throws Exception;
}
