package com.winlator.cmod.launchdeps;

import android.content.Context;
import android.os.SystemClock;

import androidx.annotation.Nullable;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.Shortcut;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.gamefixes.GameFixesPreLaunchStep;
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
            ContentsManager contentsManager,
            Container container,
            @Nullable Shortcut shortcut,
            @Nullable String appId,
            @Nullable String traceId
    ) {
        ensureBuiltIns();
        LaunchDependencyContext dependencyContext = new LaunchDependencyContext(context, contentsManager);
        long syncStartedAt = SystemClock.elapsedRealtime();
        log(context, "LAUNCH_DEP_CONTEXT_SYNC_START", traceId, "contents_sync", appId, null, -1L);
        contentsManager.syncContents();
        log(
                context,
                "LAUNCH_DEP_CONTEXT_SYNC_DONE",
                traceId,
                "contents_sync",
                appId,
                null,
                SystemClock.elapsedRealtime() - syncStartedAt,
                "wine_profile_count", profileCount(contentsManager, ContentProfile.ContentType.CONTENT_TYPE_WINE),
                "proton_profile_count", profileCount(contentsManager, ContentProfile.ContentType.CONTENT_TYPE_PROTON),
                "dxvk_profile_count", profileCount(contentsManager, ContentProfile.ContentType.CONTENT_TYPE_DXVK),
                "vkd3d_profile_count", profileCount(contentsManager, ContentProfile.ContentType.CONTENT_TYPE_VKD3D),
                "box64_profile_count", profileCount(contentsManager, ContentProfile.ContentType.CONTENT_TYPE_BOX64),
                "wowbox64_profile_count", profileCount(contentsManager, ContentProfile.ContentType.CONTENT_TYPE_WOWBOX64),
                "fexcore_profile_count", profileCount(contentsManager, ContentProfile.ContentType.CONTENT_TYPE_FEXCORE)
        );
        for (LaunchDependency dependency : LAUNCH_DEPENDENCIES) {
            try {
                if (!dependency.appliesTo(container, shortcut, appId)) continue;
                long checkStartedAt = SystemClock.elapsedRealtime();
                log(context, "LAUNCH_DEP_CHECK_START", traceId, dependency.getId(), appId, null, -1L);
                if (dependency.isSatisfied(dependencyContext, container, shortcut, appId)) {
                    long elapsedMs = SystemClock.elapsedRealtime() - checkStartedAt;
                    log(context, "LAUNCH_DEP_CHECK_DONE", traceId, dependency.getId(), appId, "satisfied", elapsedMs);
                    log(context, "LAUNCH_DEP_SATISFIED", traceId, dependency.getId(), appId, null, elapsedMs);
                    continue;
                }
                String message = dependency.getLoadingMessage(dependencyContext, container, shortcut, appId);
                long checkElapsedMs = SystemClock.elapsedRealtime() - checkStartedAt;
                log(context, "LAUNCH_DEP_CHECK_DONE", traceId, dependency.getId(), appId, "needs_install", checkElapsedMs);
                log(context, "LAUNCH_DEP_INSTALL_START", traceId, dependency.getId(), appId, message, -1L);
                long installStartedAt = SystemClock.elapsedRealtime();
                dependency.install(dependencyContext, container, shortcut, appId, NO_OP_CALLBACKS);
                if (!dependency.isSatisfied(dependencyContext, container, shortcut, appId)) {
                    String detail = "Dependency still unsatisfied after install: " + dependency.getId();
                    log(context, "LAUNCH_DEP_INSTALL_FAILED", traceId, dependency.getId(), appId, detail, SystemClock.elapsedRealtime() - installStartedAt);
                    return DependencyRunResult.failed(dependency.getId(), detail);
                }
                log(context, "LAUNCH_DEP_INSTALL_DONE", traceId, dependency.getId(), appId, null, SystemClock.elapsedRealtime() - installStartedAt);
            } catch (Exception e) {
                String detail = e.getMessage() == null ? "Dependency install failed" : e.getMessage();
                log(context, "LAUNCH_DEP_CHECK_FAILED", traceId, dependency.getId(), appId, detail, -1L, "error_class", e.getClass().getName());
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
            @Nullable String traceId,
            GuestProgramLauncherComponent launcher
    ) {
        ensureBuiltIns();
        for (PreLaunchStep step : PRE_LAUNCH_STEPS) {
            try {
                if (!step.appliesTo(container, shortcut, appId)) continue;
                long startedAt = SystemClock.elapsedRealtime();
                log(context, "PRELAUNCH_STEP_START", traceId, step.getId(), appId, null, -1L);
                step.run(context, container, shortcut, appId, launcher);
                log(context, "PRELAUNCH_STEP_DONE", traceId, step.getId(), appId, null, SystemClock.elapsedRealtime() - startedAt);
            } catch (Exception e) {
                log(context, "PRELAUNCH_STEP_FAILED", traceId, step.getId(), appId, e.getMessage(), -1L, "error_class", e.getClass().getName());
            }
        }
    }

    private static synchronized void ensureBuiltIns() {
        if (BUILTINS_REGISTERED) return;
        registerDependency(new WineRuntimePresenceDependency());
        registerDependency(new EmulatorRuntimePresenceDependency());
        registerDependency(new WrapperRuntimePresenceDependency());
        registerPreLaunchStep(new GameFixesPreLaunchStep());
        BUILTINS_REGISTERED = true;
    }

    private static int profileCount(ContentsManager contentsManager, ContentProfile.ContentType type) {
        List<ContentProfile> profiles = contentsManager.getProfiles(type);
        return profiles == null ? 0 : profiles.size();
    }

    private static void log(Context context, String eventId, @Nullable String traceId, String id, @Nullable String appId,
                            @Nullable String detail, long elapsedMs, Object... extraFields) {
        ArrayList<Object> fields = new ArrayList<>();
        fields.add("dependency_id");
        fields.add(id);
        fields.add("app_id");
        fields.add(appId == null ? "-" : appId);
        if (elapsedMs >= 0L) {
            fields.add("elapsed_ms");
            fields.add(elapsedMs);
        }
        if (extraFields != null) {
            for (Object field : extraFields) fields.add(field);
        }
        ForensicLogger.logEvent(
                context,
                "info",
                eventId,
                traceId,
                "launch_dependency",
                detail == null ? id : detail,
                ForensicLogger.fields(fields.toArray())
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
