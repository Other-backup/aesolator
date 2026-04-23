package com.winlator.cmod.core;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class LaunchSecurity {
    private static final String PREFS_NAME = "aeso_launch_security";
    private static final String PREF_SECRET = "launch_secret_v1";
    private static final String EXTRA_XSERVER_SIGNATURE = "aeso_xserver_launch_sig";
    public static final String EXTRA_APP_ID = "app_id";
    public static final String EXTRA_LAUNCH_ROUTE_TOKEN = "aeso_launch_route_token";
    public static final String EXTRA_TEMP_OVERRIDE_APP_ID = "aeso_temp_override_app_id";
    private static final int SECRET_SIZE_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private LaunchSecurity() {}

    public static void signXServerLaunchIntent(Context context, Intent intent) {
        if (context == null || intent == null) return;
        String signature = computeSignature(context, intent);
        if (!signature.isEmpty()) intent.putExtra(EXTRA_XSERVER_SIGNATURE, signature);
    }

    public static boolean hasXServerLaunchSignature(Intent intent) {
        if (intent == null) return false;
        String value = intent.getStringExtra(EXTRA_XSERVER_SIGNATURE);
        return value != null && !value.trim().isEmpty();
    }

    public static boolean isTrustedXServerLaunchIntent(Context context, Intent intent) {
        if (context == null || intent == null) return false;
        String provided = safe(intent.getStringExtra(EXTRA_XSERVER_SIGNATURE));
        if (provided.isEmpty()) return false;
        String expected = computeSignature(context, intent);
        if (expected.isEmpty()) return false;
        return MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8)
        );
    }

    public static String getXServerLaunchTrustState(Context context, Intent intent) {
        if (intent == null) return "no_intent";
        if (!hasXServerLaunchSignature(intent)) return "unsigned_legacy";
        if (context == null) return "signed_unverified";
        return isTrustedXServerLaunchIntent(context, intent) ? "signed_trusted" : "signed_untrusted";
    }

    private static String computeSignature(Context context, Intent intent) {
        try {
            byte[] key = resolveOrCreateSecret(context.getApplicationContext());
            if (key.length == 0) return "";
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] digest = mac.doFinal(buildPayload(context, intent).getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(digest, Base64.NO_WRAP);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static byte[] resolveOrCreateSecret(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String encoded = safe(prefs.getString(PREF_SECRET, ""));
        if (!encoded.isEmpty()) {
            try {
                byte[] decoded = Base64.decode(encoded, Base64.NO_WRAP);
                if (decoded.length >= 16) return decoded;
            } catch (Exception ignored) {
            }
        }
        byte[] key = new byte[SECRET_SIZE_BYTES];
        RANDOM.nextBytes(key);
        prefs.edit().putString(PREF_SECRET, Base64.encodeToString(key, Base64.NO_WRAP)).apply();
        return key;
    }

    private static String buildPayload(Context context, Intent intent) {
        return context.getPackageName() + "|"
                + "com.winlator.cmod.XServerDisplayActivity|"
                + intent.getIntExtra("container_id", 0) + "|"
                + safe(intent.getStringExtra("shortcut_path")) + "|"
                + safe(intent.getStringExtra("shortcut_name")) + "|"
                + safe(intent.getStringExtra(EXTRA_APP_ID)) + "|"
                + safe(intent.getStringExtra(EXTRA_LAUNCH_ROUTE_TOKEN)) + "|"
                + safe(intent.getStringExtra(EXTRA_TEMP_OVERRIDE_APP_ID)) + "|"
                + safe(intent.getStringExtra("disableXinput")) + "|"
                + safe(intent.getAction());
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
