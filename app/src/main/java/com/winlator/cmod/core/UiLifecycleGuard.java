package com.winlator.cmod.core;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public final class UiLifecycleGuard {
    private UiLifecycleGuard() {}

    public static boolean canShowDialog(@Nullable Context context, String owner, String action) {
        Activity activity = resolveActivity(context);
        if (activity == null) {
            return true;
        }
        if (isActivityInvalid(activity)) {
            log(activity, "warn", "UI_DIALOG_SHOW_SKIPPED", owner, action, "activity_invalid", true, "state_saved", isStateSaved(activity));
            return false;
        }
        if (isStateSaved(activity)) {
            log(activity, "warn", "UI_DIALOG_SHOW_SKIPPED", owner, action, "activity_invalid", false, "state_saved", true);
            return false;
        }
        return true;
    }

    public static boolean commit(@Nullable FragmentActivity activity, @Nullable FragmentTransaction transaction, String owner, String action) {
        if (activity == null || transaction == null) return false;
        if (isActivityInvalid(activity)) {
            log(activity, "warn", "UI_FRAGMENT_TRANSACTION_SKIPPED", owner, action, "activity_invalid", true, "state_saved", isStateSaved(activity));
            return false;
        }

        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        try {
            if (fragmentManager.isStateSaved()) {
                log(activity, "warn", "UI_FRAGMENT_TRANSACTION_ALLOWING_STATE_LOSS", owner, action, "activity_invalid", false, "state_saved", true);
                transaction.commitAllowingStateLoss();
            } else {
                transaction.commit();
            }
            return true;
        } catch (IllegalStateException e) {
            ForensicLogger.error(
                    activity,
                    "UI_FRAGMENT_TRANSACTION_FAILED",
                    null,
                    "ui_lifecycle",
                    "Fragment transaction failed",
                    e,
                    ForensicLogger.fields(
                            "owner", owner,
                            "action", action,
                            "activity_invalid", isActivityInvalid(activity),
                            "state_saved", fragmentManager.isStateSaved()
                    )
            );
            return false;
        }
    }

    public static boolean popBackStack(@Nullable Fragment fragment, String owner, String action) {
        if (fragment == null) return false;
        FragmentActivity activity = fragment.getActivity();
        if (activity == null || isActivityInvalid(activity)) {
            log(activity, "warn", "UI_BACKSTACK_POP_SKIPPED", owner, action, "activity_invalid", true, "state_saved", isStateSaved(activity));
            return false;
        }
        FragmentManager fragmentManager = fragment.getParentFragmentManager();
        if (fragmentManager.isStateSaved()) {
            log(activity, "warn", "UI_BACKSTACK_POP_SKIPPED", owner, action, "activity_invalid", false, "state_saved", true);
            return false;
        }
        fragmentManager.popBackStack();
        return true;
    }

    public static boolean dispatchBackPress(@Nullable Fragment fragment, String owner, String action) {
        if (fragment == null) return false;
        FragmentActivity activity = fragment.getActivity();
        if (activity == null || isActivityInvalid(activity)) {
            log(activity, "warn", "UI_BACK_NAVIGATION_SKIPPED", owner, action, "activity_invalid", true, "state_saved", isStateSaved(activity));
            return false;
        }
        if (activity.getSupportFragmentManager().isStateSaved()) {
            log(activity, "warn", "UI_BACK_NAVIGATION_SKIPPED", owner, action, "activity_invalid", false, "state_saved", true);
            return false;
        }
        activity.getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    public static boolean runOnUiThread(@Nullable Fragment fragment, @Nullable Runnable action, String owner, String operation) {
        if (fragment == null || action == null) return false;
        FragmentActivity activity = fragment.getActivity();
        if (activity == null || isActivityInvalid(activity)) {
            log(activity, "warn", "UI_FRAGMENT_CALLBACK_SKIPPED", owner, operation, "activity_invalid", true, "state_saved", isStateSaved(activity));
            return false;
        }
        activity.runOnUiThread(() -> {
            FragmentActivity currentActivity = fragment.getActivity();
            if (!fragment.isAdded() || currentActivity == null || isActivityInvalid(currentActivity)) {
                log(currentActivity, "warn", "UI_FRAGMENT_CALLBACK_SKIPPED", owner, operation, "activity_invalid", true, "state_saved", isStateSaved(currentActivity));
                return;
            }
            try {
                action.run();
            } catch (IllegalStateException e) {
                ForensicLogger.error(
                        currentActivity,
                        "UI_FRAGMENT_CALLBACK_FAILED",
                        null,
                        "ui_lifecycle",
                        "Fragment UI callback failed",
                        e,
                        ForensicLogger.fields(
                                "owner", owner,
                                "action", operation,
                                "activity_invalid", isActivityInvalid(currentActivity),
                                "state_saved", currentActivity.getSupportFragmentManager().isStateSaved()
                        )
                );
            }
        });
        return true;
    }

    private static void log(@Nullable Context context, String severity, String eventId, String owner, String action, Object... extraFields) {
        ForensicLogger.logEvent(
                context,
                severity,
                eventId,
                null,
                "ui_lifecycle",
                action,
                ForensicLogger.fields(merge(owner, action, extraFields))
        );
    }

    private static Object[] merge(String owner, String action, Object[] extraFields) {
        Object[] fields = new Object[(extraFields == null ? 0 : extraFields.length) + 4];
        fields[0] = "owner";
        fields[1] = owner;
        fields[2] = "action";
        fields[3] = action;
        if (extraFields != null && extraFields.length > 0) {
            System.arraycopy(extraFields, 0, fields, 4, extraFields.length);
        }
        return fields;
    }

    private static boolean isActivityInvalid(@Nullable Activity activity) {
        return activity == null || activity.isFinishing() || activity.isDestroyed();
    }

    private static boolean isStateSaved(@Nullable Activity activity) {
        if (!(activity instanceof FragmentActivity fragmentActivity)) return false;
        return fragmentActivity.getSupportFragmentManager().isStateSaved();
    }

    @Nullable
    private static Activity resolveActivity(@Nullable Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity activity) {
                return activity;
            }
            current = ((ContextWrapper) current).getBaseContext();
        }
        return current instanceof Activity ? (Activity) current : null;
    }
}
