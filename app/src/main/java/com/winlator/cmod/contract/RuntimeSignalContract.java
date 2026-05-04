package com.winlator.cmod.contract;

import com.winlator.cmod.core.EnvVars;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public final class RuntimeSignalContract {
    public static final String WINLATOR_SIGNAL_POLICY = "WINLATOR_SIGNAL_POLICY";
    public static final String WINLATOR_SIGNAL_SOURCES = "WINLATOR_SIGNAL_SOURCES";
    public static final String WINLATOR_SIGNAL_DECISION_HASH = "WINLATOR_SIGNAL_DECISION_HASH";
    public static final String WINLATOR_RUNTIME_PRESET_GUARD_REASON = "WINLATOR_RUNTIME_PRESET_GUARD_REASON";
    public static final String WINLATOR_UPSCALER_BINDING_GUARD_REASON = "WINLATOR_UPSCALER_BINDING_GUARD_REASON";
    public static final String WINLATOR_SIGNAL_INPUT_ROUTE = "WINLATOR_SIGNAL_INPUT_ROUTE";
    public static final String WINLATOR_SIGNAL_INPUT_LAUNCH_KIND = "WINLATOR_SIGNAL_INPUT_LAUNCH_KIND";
    public static final String WINLATOR_SIGNAL_INPUT_PRECHECK_REASON = "WINLATOR_SIGNAL_INPUT_PRECHECK_REASON";
    public static final String WINLATOR_SIGNAL_INPUT_PRECHECK_FALLBACK = "WINLATOR_SIGNAL_INPUT_PRECHECK_FALLBACK";
    public static final String WINLATOR_LSFG_EFFECTIVE_BACKEND = "WINLATOR_LSFG_EFFECTIVE_BACKEND";
    public static final String WINLATOR_LSFG_EFFECTIVE_FRAMEGEN = "WINLATOR_LSFG_EFFECTIVE_FRAMEGEN";
    public static final String WINLATOR_LSFG_EFFECTIVE_MODE = "WINLATOR_LSFG_EFFECTIVE_MODE";
    public static final String WINLATOR_LSFG_EFFECTIVE_SOURCE_CHAIN = "WINLATOR_LSFG_EFFECTIVE_SOURCE_CHAIN";

    private RuntimeSignalContract() {}

    public static void putSignalInputMarkers(
            EnvVars envVars,
            String route,
            String launchKind,
            String precheckReason,
            String precheckFallback) {
        if (envVars == null) return;
        envVars.put(WINLATOR_SIGNAL_INPUT_ROUTE, normalize(route, "unknown"));
        envVars.put(WINLATOR_SIGNAL_INPUT_LAUNCH_KIND, normalize(launchKind, "unknown"));
        envVars.put(WINLATOR_SIGNAL_INPUT_PRECHECK_REASON, normalize(precheckReason, "none"));
        envVars.put(WINLATOR_SIGNAL_INPUT_PRECHECK_FALLBACK, normalize(precheckFallback, "none"));
        refreshDecisionHash(envVars);
    }

    public static void putSignalPolicyMarkers(
            EnvVars envVars,
            String signalPolicy,
            String signalSources,
            String runtimePresetGuardReason,
            String upscalerBindingGuardReason) {
        if (envVars == null) return;
        envVars.put(WINLATOR_SIGNAL_POLICY, normalize(signalPolicy, "unset"));
        envVars.put(WINLATOR_SIGNAL_SOURCES, normalize(signalSources, "unset"));
        envVars.put(WINLATOR_RUNTIME_PRESET_GUARD_REASON, normalize(runtimePresetGuardReason, "none"));
        envVars.put(WINLATOR_UPSCALER_BINDING_GUARD_REASON, normalize(upscalerBindingGuardReason, "none"));
        refreshDecisionHash(envVars);
    }


    public static void putLsfgEffectiveMarkers(
            EnvVars envVars,
            String backend,
            boolean framegenEnabled,
            String framegenMode,
            String sourceChain) {
        if (envVars == null) return;
        envVars.put(WINLATOR_LSFG_EFFECTIVE_BACKEND, normalize(backend, "off"));
        envVars.put(WINLATOR_LSFG_EFFECTIVE_FRAMEGEN, framegenEnabled ? "1" : "0");
        envVars.put(WINLATOR_LSFG_EFFECTIVE_MODE, normalize(framegenMode, "balanced"));
        envVars.put(WINLATOR_LSFG_EFFECTIVE_SOURCE_CHAIN, normalize(sourceChain, "global_profile"));
        refreshDecisionHash(envVars);
    }

    public static String get(EnvVars envVars, String key) {
        if (envVars == null || key == null || key.isEmpty()) return "";
        return envVars.get(key);
    }

    private static void refreshDecisionHash(EnvVars envVars) {
        String data = String.join("|",
                get(envVars, WINLATOR_SIGNAL_POLICY),
                get(envVars, WINLATOR_SIGNAL_SOURCES),
                get(envVars, WINLATOR_RUNTIME_PRESET_GUARD_REASON),
                get(envVars, WINLATOR_UPSCALER_BINDING_GUARD_REASON),
                get(envVars, WINLATOR_SIGNAL_INPUT_ROUTE),
                get(envVars, WINLATOR_SIGNAL_INPUT_LAUNCH_KIND),
                get(envVars, WINLATOR_SIGNAL_INPUT_PRECHECK_REASON),
                get(envVars, WINLATOR_SIGNAL_INPUT_PRECHECK_FALLBACK));
        envVars.put(WINLATOR_SIGNAL_DECISION_HASH, sha256Short(data));
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static String sha256Short(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) sb.append(String.format(Locale.US, "%02x", b));
            String full = sb.toString();
            return full.length() > 16 ? full.substring(0, 16) : full;
        }
        catch (NoSuchAlgorithmException e) {
            return Integer.toHexString((value == null ? "" : value).hashCode());
        }
    }
}
