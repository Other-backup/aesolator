package com.winlator.cmod.launchdeps;

import android.content.Context;

import androidx.annotation.Nullable;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.Shortcut;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.xenvironment.components.GuestProgramLauncherComponent;

import java.util.ArrayList;
import java.util.List;

public final class LaunchDependencyRegistry {
    private static final List<LaunchDependency> LAUNCH_DEPENDENCIES = new ArrayList<>();
    private static final List<PreLaunchStep> PRE_LAUNCH_STEPS = new ArrayList<>();
    private static boolean BUILTINS_REGISTERED = false;

    private LaunchDependencyRegistry() {
    }

    public static void registerDependency(LaunchDependency dependency) {
        if (dependency == null) return;
        if (!LAUNCH_DEPENDENCIES.contains(dependency)) {
            LAUNCH_DEPENDENCIES.add(dependency);
        }
    }

    public static void registerPreLaunchStep(PreLaunchStep step) {
        if (step == null) return;
        if (!PRE_LAUNCH_STEPS.contains(step)) {
            PRE_LAUNCH_STEPS.add(step);
        }
    }

    public static final class DependencyRunResult {
        public final boolean success;
        public final String dependencyId;
        public final String message;

        private DependencyRunResult(boolean success, String dependencyId, String message) {
            this.success = success;
            this.dependencyId = dependencyId;
            this.message = message;
        }

        public static DependencyRunResult success() {
            return new DependencyRunResult(true, "", "");
        }

        public static DependencyRunResult failed(String dependencyId, String message) {
            return new DependencyRunResult(false, dependencyId == null ? "" : dependencyId, message == null ? "" : message);
        }
    }

    public static void clearRegisteredContracts() {
        LAUNCH_DEPENDENCIES.clear();
        PRE_LAUNCH_STEPS.clear();
        BUILTINS_REGISTERED = false;
    }

    public static DependencyRunResult runDependencies(
            Context context,
            Container container,
            @Nullable Shortcut shortcut,
            @Nullable String appId
    ) {
        ensureBuiltIns();
        for (LaunchDependency dependency : LAUNCH_DEPENDENCIES) {
            try {
                if (!dependency.appliesTo(container, shortcut, appId)) continue;
                if (dependency.isSatisfied(context, container, shortcut, appId)) {
                    log(context, "LAUNCH_DEP_SATISFIED", dependency.getId(), appId, null);
                    continue;
                }
                String message = dependency.getLoadingMessage(context, container, shortcut, appId);
                log(context, "LAUNCH_DEP_INSTALL_START", dependency.getId(), appId, message);
                dependency.install(context, container, shortcut, appId, NO_OP_CALLBACKS);
                if (!dependency.isSatisfied(context, container, shortcut, appId)) {
                    String detail = "Dependency still unsatisfied after install: " + dependency.getId();
                    log(context, "LAUNCH_DEP_INSTALL_FAILED", dependency.getId(), appId, detail);
                    return DependencyRunResult.failed(dependency.getId(), detail);
                }
                log(context, "LAUNCH_DEP_INSTALL_DONE", dependency.getId(), appId, null);
            } catch (Exception e) {
                String detail = e.getMessage() == null ? "Dependency install failed" : e.getMessage();
                log(context, "LAUNCH_DEP_INSTALL_FAILED", dependency.getId(), appId, detail);
                return DependencyRunResult.failed(dependency.getId(), detail);
            }
        }
        return DependencyRunResult.success();
    }

    public static void runPreLaunchSteps(
            Context context,
            Container container,
            @Nullable Shortcut shortcut,
            @Nullable String appId,
            GuestProgramLauncherComponent launcher
    ) {
        ensureBuiltIns();
        for (PreLaunchStep step : PRE_LAUNCH_STEPS) {
            try {
                if (!step.appliesTo(container, shortcut, appId)) continue;
                log(context, "PRELAUNCH_STEP_START", step.getId(), appId, null);
                step.run(context, container, shortcut, appId, launcher);
                log(context, "PRELAUNCH_STEP_DONE", step.getId(), appId, null);
            } catch (Exception e) {
                log(context, "PRELAUNCH_STEP_FAILED", step.getId(), appId, e.getMessage());
            }
        }
    }

    private static synchronized void ensureBuiltIns() {
        if (BUILTINS_REGISTERED) return;
        registerDependency(new WineRuntimePresenceDependency());
        registerDependency(new EmulatorRuntimePresenceDependency());
        registerDependency(new WrapperRuntimePresenceDependency());
        BUILTINS_REGISTERED = true;
    }

    private static void log(Context context, String eventId, String id, @Nullable String appId, @Nullable String detail) {
        ForensicLogger.logEvent(
                context,
                "info",
                eventId,
                null,
                "launch_dependency",
                detail == null ? id : detail,
                ForensicLogger.fields(
                        "dependency_id", id,
                        "app_id", appId == null ? "-" : appId
                )
        );
    }

    private static final LaunchDependencyCallbacks NO_OP_CALLBACKS = new LaunchDependencyCallbacks() {
        @Override
        public void setLoadingMessage(String message) {
        }

        @Override
        public void setLoadingProgress(float progress) {
        }
    };
}
