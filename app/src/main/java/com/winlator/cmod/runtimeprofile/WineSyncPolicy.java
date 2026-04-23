package com.winlator.cmod.runtimeprofile;

import com.winlator.cmod.core.EnvVars;
import com.winlator.cmod.contents.ContentProfile;

import java.io.File;
import java.util.Locale;

public final class WineSyncPolicy {
    public static final String ENV_SYNC_BACKEND = "AERO_WINE_SYNC_BACKEND";

    public static final String BACKEND_AUTO = "auto";
    public static final String BACKEND_NTSYNC = "ntsync";
    public static final String BACKEND_AESYNC = "aesync";
    public static final String BACKEND_FSYNC = "fsync";
    public static final String BACKEND_ESYNC = "esync";
    public static final String BACKEND_SERVER = "server";
    public static final String BACKEND_LEGACY_DUAL = "legacy_dual_fastpaths";

    /*
     * The current FreeWine source line already carries Wine's in-process ntsync
     * path. Whether the shipped runtime was built with the exact ntsync header
     * support is a separate packaging/build fact, so keep that part explicit in
     * forensic markers instead of pretending the source line lacks the backend.
     */
    private static final boolean NTSYNC_SOURCE_TREE_PRESENT = true;
    private static final String NTSYNC_COMPILED_SUPPORT_UNKNOWN = "unknown";
    private static final String NTSYNC_ENV_SWITCHABLE = "0";
    private static final String AESYNC_SCOPE_CHAPTER2_FREEWINE = "chapter2_freewine11";
    private static final String AESYNC_SCOPE_OTHER_RUNTIME = "other_runtime";

    private WineSyncPolicy() {}

    public static void apply(EnvVars envVars) {
        apply(envVars, null);
    }

    public static void apply(EnvVars envVars, ContentProfile runtimeProfile) {
        RuntimeAcceptance runtimeAcceptance = classifyRuntimeAcceptance(runtimeProfile);
        boolean explicitBackend = hasNonEmpty(envVars, ENV_SYNC_BACKEND);
        boolean explicitLegacyEnv = envVars.has("WINEAESYNC") || envVars.has("WINEFSYNC") || envVars.has("WINEESYNC");
        boolean ntsyncDevicePresent = new File("/dev/ntsync").exists();
        boolean ntsyncRuntimeReachable = NTSYNC_SOURCE_TREE_PRESENT && ntsyncDevicePresent;
        boolean aesyncAccepted = runtimeAcceptance.acceptsAesync;
        boolean staleLegacyEsyncDefault = !explicitBackend && shouldPromoteLegacyEsyncDefault(envVars, runtimeAcceptance);

        String requested = explicitBackend ? normalizeBackend(envVars.get(ENV_SYNC_BACKEND)) : BACKEND_AUTO;
        String effective;
        String userspacePolicy;
        String expectedPath;
        String decisionSource;
        String reason;

        if (staleLegacyEsyncDefault) {
            setUserspaceSyncVars(envVars, true, true, true);
            effective = BACKEND_AUTO;
            userspacePolicy = BACKEND_AESYNC;
            decisionSource = "legacy_env_auto_promotion";
            reason = ntsyncRuntimeReachable
                    ? "legacy_esync_default_promoted_to_aesync_kernel_ntsync_still_possible"
                    : "legacy_esync_default_promoted_to_aesync";
        } else if (!explicitBackend && explicitLegacyEnv) {
            canonicalizeLegacySyncVars(envVars);
            boolean legacyRequestedAesync = "1".equals(trim(envVars.get("WINEAESYNC")));
            if (!aesyncAccepted && legacyRequestedAesync) {
                envVars.put("WINEAESYNC", "0");
            }
            userspacePolicy = classifyLegacyUserspacePolicy(envVars);
            effective = userspacePolicy;
            decisionSource = "legacy_env";
            reason = !aesyncAccepted && legacyRequestedAesync
                    ? "legacy_aesync_denied_non_chapter2_freewine_keep_legacy_fastpaths"
                    : "manual_wine_sync_env_present";
        } else {
            switch (requested) {
                case BACKEND_NTSYNC -> {
                    /*
                     * The app cannot hard-switch kernel ntsync on or off.
                     * Keep the userspace fast-path plane explicit as degrade:
                     * Chapter 2 FreeWine gets Aesync, other runtimes get legacy
                     * fsync+esync. If a compatible runtime later reaches
                     * /dev/ntsync, Wine's own inproc path can still take it.
                     */
                    setUserspaceSyncVars(envVars, aesyncAccepted, true, true);
                    effective = BACKEND_NTSYNC;
                    userspacePolicy = aesyncAccepted ? BACKEND_AESYNC : BACKEND_LEGACY_DUAL;
                    decisionSource = explicitBackend ? "aero_sync_backend" : "default_auto";
                    if (aesyncAccepted) {
                        reason = ntsyncRuntimeReachable
                                ? "ntsync_requested_kernel_plane_present_keep_aesync_fallbacks"
                                : "ntsync_requested_kernel_plane_unavailable_keep_aesync_fallbacks";
                    } else {
                        reason = ntsyncRuntimeReachable
                                ? "ntsync_requested_non_chapter2_runtime_keep_legacy_fastpaths"
                                : "ntsync_requested_non_chapter2_runtime_no_kernel_keep_legacy_fastpaths";
                    }
                }
                case BACKEND_AESYNC -> {
                    setUserspaceSyncVars(envVars, aesyncAccepted, true, true);
                    effective = aesyncAccepted ? BACKEND_AESYNC : BACKEND_LEGACY_DUAL;
                    userspacePolicy = aesyncAccepted ? BACKEND_AESYNC : BACKEND_LEGACY_DUAL;
                    decisionSource = explicitBackend ? "aero_sync_backend" : "default_auto";
                    if (aesyncAccepted) {
                        reason = ntsyncRuntimeReachable
                                ? "explicit_aesync_userspace_policy_kernel_ntsync_still_possible"
                                : "explicit_aesync";
                    } else {
                        reason = ntsyncRuntimeReachable
                                ? "explicit_aesync_denied_non_chapter2_freewine_keep_legacy_fastpaths_kernel_ntsync_still_possible"
                                : "explicit_aesync_denied_non_chapter2_freewine_keep_legacy_fastpaths";
                    }
                }
                case BACKEND_FSYNC -> {
                    setUserspaceSyncVars(envVars, false, true, false);
                    effective = BACKEND_FSYNC;
                    userspacePolicy = BACKEND_FSYNC;
                    decisionSource = explicitBackend ? "aero_sync_backend" : "default_auto";
                    reason = ntsyncRuntimeReachable
                            ? "explicit_fsync_userspace_policy_kernel_ntsync_still_possible"
                            : "explicit_fsync";
                }
                case BACKEND_ESYNC -> {
                    setUserspaceSyncVars(envVars, false, false, true);
                    effective = BACKEND_ESYNC;
                    userspacePolicy = BACKEND_ESYNC;
                    decisionSource = explicitBackend ? "aero_sync_backend" : "default_auto";
                    reason = ntsyncRuntimeReachable
                            ? "explicit_esync_userspace_policy_kernel_ntsync_still_possible"
                            : "explicit_esync";
                }
                case BACKEND_SERVER -> {
                    setUserspaceSyncVars(envVars, false, false, false);
                    effective = BACKEND_SERVER;
                    userspacePolicy = BACKEND_SERVER;
                    decisionSource = explicitBackend ? "aero_sync_backend" : "default_auto";
                    reason = ntsyncRuntimeReachable
                            ? "explicit_server_userspace_only_kernel_ntsync_not_env_switchable"
                            : "explicit_server";
                }
                case BACKEND_AUTO -> {
                    setUserspaceSyncVars(envVars, aesyncAccepted, true, true);
                    effective = aesyncAccepted ? BACKEND_AUTO : BACKEND_LEGACY_DUAL;
                    userspacePolicy = aesyncAccepted ? BACKEND_AESYNC : BACKEND_LEGACY_DUAL;
                    decisionSource = explicitBackend ? "aero_sync_backend" : "default_auto";
                    if (aesyncAccepted) {
                        reason = ntsyncRuntimeReachable
                                ? "auto_kernel_ntsync_present_keep_aesync_fastpaths_as_fallback"
                                : "auto_keep_aesync_fastpaths";
                    } else {
                        reason = ntsyncRuntimeReachable
                                ? "auto_non_chapter2_runtime_keep_legacy_fastpaths_kernel_ntsync_still_possible"
                                : "auto_non_chapter2_runtime_keep_legacy_fastpaths";
                    }
                }
                default -> {
                    setUserspaceSyncVars(envVars, aesyncAccepted, true, true);
                    effective = aesyncAccepted ? BACKEND_AUTO : BACKEND_LEGACY_DUAL;
                    userspacePolicy = aesyncAccepted ? BACKEND_AESYNC : BACKEND_LEGACY_DUAL;
                    decisionSource = explicitBackend ? "aero_sync_backend" : "default_auto";
                    reason = aesyncAccepted
                            ? "unknown_backend_fell_back_to_auto"
                            : "unknown_backend_non_chapter2_runtime_fell_back_to_legacy_fastpaths";
                }
            }
        }

        expectedPath = classifyExpectedPath(ntsyncRuntimeReachable, userspacePolicy);

        envVars.put("AERO_WINE_SYNC_REQUESTED", requested);
        envVars.put("AERO_WINE_SYNC_POLICY_EFFECTIVE", effective);
        envVars.put("AERO_WINE_SYNC_USERSPACE_POLICY_EFFECTIVE", userspacePolicy);
        envVars.put("AERO_WINE_SYNC_EXPECTED_PATH", expectedPath);
        envVars.put("AERO_WINE_SYNC_DECISION_SOURCE", decisionSource);
        envVars.put("AERO_WINE_SYNC_REASON", reason);
        envVars.put("AERO_WINE_SYNC_NTSYNC_DEVICE_PRESENT", ntsyncDevicePresent ? "1" : "0");
        envVars.put("AERO_WINE_SYNC_NTSYNC_SOURCE_SUPPORTED", NTSYNC_SOURCE_TREE_PRESENT ? "1" : "0");
        envVars.put("AERO_WINE_SYNC_NTSYNC_SOURCE_TREE_PRESENT", NTSYNC_SOURCE_TREE_PRESENT ? "1" : "0");
        envVars.put("AERO_WINE_SYNC_NTSYNC_COMPILED_SUPPORT", NTSYNC_COMPILED_SUPPORT_UNKNOWN);
        envVars.put("AERO_WINE_SYNC_NTSYNC_ENV_SWITCHABLE", NTSYNC_ENV_SWITCHABLE);
        envVars.put("AERO_WINE_SYNC_RUNTIME_ACCEPTS_AESYNC", runtimeAcceptance.acceptsAesync ? "1" : "0");
        envVars.put("AERO_WINE_SYNC_RUNTIME_SCOPE", runtimeAcceptance.scope);
        envVars.put("AERO_WINE_SYNC_RUNTIME_FAMILY", runtimeAcceptance.family);
        envVars.put("AERO_WINE_SYNC_RUNTIME_MODEL", runtimeAcceptance.runtimeModel);
        envVars.put("AERO_WINE_SYNC_RUNTIME_SOURCE_REPO", runtimeAcceptance.sourceRepo);
        envVars.put("AERO_WINE_SYNC_RUNTIME_RELEASE_TAG", runtimeAcceptance.releaseTag);
        envVars.put("AERO_WINE_SYNC_RUNTIME_ARTIFACT_NAME", runtimeAcceptance.artifactName);
        envVars.put("AERO_WINE_SYNC_RUNTIME_ENTRY", runtimeAcceptance.entryName);
        envVars.put("AERO_WINE_SYNC_WINEAESYNC_EFFECTIVE", envVars.get("WINEAESYNC"));
        envVars.put("AERO_WINE_SYNC_WINEFSYNC_EFFECTIVE", envVars.get("WINEFSYNC"));
        envVars.put("AERO_WINE_SYNC_WINEESYNC_EFFECTIVE", envVars.get("WINEESYNC"));
    }

    private static boolean hasNonEmpty(EnvVars envVars, String name) {
        return envVars.has(name) && !trim(envVars.get(name)).isEmpty();
    }

    private static void canonicalizeLegacySyncVars(EnvVars envVars) {
        if (!envVars.has("WINEAESYNC")) envVars.put("WINEAESYNC", "0");
        if (!envVars.has("WINEFSYNC")) envVars.put("WINEFSYNC", "0");
        if (!envVars.has("WINEESYNC")) envVars.put("WINEESYNC", "0");
    }

    private static boolean shouldPromoteLegacyEsyncDefault(EnvVars envVars, RuntimeAcceptance runtimeAcceptance) {
        if (runtimeAcceptance == null || !runtimeAcceptance.acceptsAesync) return false;
        boolean aesync = "1".equals(trim(envVars.get("WINEAESYNC")));
        boolean fsync = "1".equals(trim(envVars.get("WINEFSYNC")));
        boolean esync = "1".equals(trim(envVars.get("WINEESYNC")));
        return !aesync && !fsync && esync;
    }

    private static String classifyLegacyUserspacePolicy(EnvVars envVars) {
        boolean aesync = "1".equals(trim(envVars.get("WINEAESYNC")));
        boolean fsync = "1".equals(trim(envVars.get("WINEFSYNC")));
        boolean esync = "1".equals(trim(envVars.get("WINEESYNC")));
        if (aesync) return BACKEND_AESYNC;
        if (fsync && esync) return BACKEND_LEGACY_DUAL;
        if (fsync) return BACKEND_FSYNC;
        if (esync) return BACKEND_ESYNC;
        return BACKEND_SERVER;
    }

    private static void setUserspaceSyncVars(EnvVars envVars, boolean aesync, boolean fsync, boolean esync) {
        envVars.put("WINEAESYNC", aesync ? "1" : "0");
        envVars.put("WINEFSYNC", fsync ? "1" : "0");
        envVars.put("WINEESYNC", esync ? "1" : "0");
    }

    private static String classifyExpectedPath(boolean ntsyncRuntimeReachable, String userspacePolicy) {
        if (ntsyncRuntimeReachable) {
            return switch (userspacePolicy) {
                case BACKEND_AESYNC -> "ntsync_or_fsync_then_esync_then_server";
                case BACKEND_LEGACY_DUAL -> "ntsync_or_legacy_fsync_and_esync_then_server";
                case BACKEND_FSYNC -> "ntsync_or_fsync_or_server";
                case BACKEND_ESYNC -> "ntsync_or_esync_or_server";
                case BACKEND_SERVER -> "ntsync_or_server";
                case BACKEND_NTSYNC, BACKEND_AUTO -> "ntsync_or_fsync_then_esync_then_server";
                default -> "ntsync_or_fsync_then_esync_then_server";
            };
        }
        return switch (userspacePolicy) {
            case BACKEND_AESYNC -> "fsync_then_esync_then_server";
            case BACKEND_LEGACY_DUAL -> "legacy_fsync_and_esync_then_server";
            case BACKEND_FSYNC -> "fsync_or_server";
            case BACKEND_ESYNC -> "esync_or_server";
            case BACKEND_SERVER -> "server_only";
            case BACKEND_NTSYNC, BACKEND_AUTO -> "fsync_then_esync_then_server";
            default -> "fsync_then_esync_then_server";
        };
    }

    private static String normalizeBackend(String raw) {
        String normalized = trim(raw).toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case BACKEND_NTSYNC -> BACKEND_NTSYNC;
            case BACKEND_AESYNC -> BACKEND_AESYNC;
            case BACKEND_FSYNC -> BACKEND_FSYNC;
            case BACKEND_ESYNC -> BACKEND_ESYNC;
            case BACKEND_SERVER -> BACKEND_SERVER;
            case BACKEND_AUTO -> BACKEND_AUTO;
            default -> BACKEND_AUTO;
        };
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static RuntimeAcceptance classifyRuntimeAcceptance(ContentProfile runtimeProfile) {
        if (runtimeProfile == null) {
            return new RuntimeAcceptance(false, AESYNC_SCOPE_OTHER_RUNTIME, "", "", "", "", "", "");
        }

        String family = runtimeProfile.isProtonLike()
                ? "proton"
                : (runtimeProfile.isWineLike() ? "wine" : "");
        String runtimeModel = trim(runtimeProfile.getRuntimeModel());
        String sourceRepo = trim(runtimeProfile.sourceRepo);
        String releaseTag = trim(runtimeProfile.releaseTag);
        String artifactName = trim(runtimeProfile.artifactName);
        String entryName = buildEntryName(runtimeProfile);

        String metadataSurface = (
                trim(runtimeProfile.verName) + " "
                        + trim(runtimeProfile.desc) + " "
                        + sourceRepo + " "
                        + releaseTag + " "
                        + artifactName + " "
                        + entryName
        ).toLowerCase(Locale.ROOT);

        boolean wineFamily = runtimeProfile.isWineLike();
        boolean bionicRuntime = ContentProfile.RUNTIME_MODEL_BIONIC.equals(ContentProfile.normalizeRuntimeModel(runtimeModel));
        boolean wcpRuntimeRepo = sourceRepo.toLowerCase(Locale.ROOT).contains("wcp-runtime-lanes");
        boolean freewineSurface = metadataSurface.contains("freewine11")
                || metadataSurface.contains("freewine 11")
                || metadataSurface.contains("freewine");
        boolean acceptsAesync = wineFamily && bionicRuntime && wcpRuntimeRepo && freewineSurface;
        String scope = acceptsAesync ? AESYNC_SCOPE_CHAPTER2_FREEWINE : AESYNC_SCOPE_OTHER_RUNTIME;

        return new RuntimeAcceptance(
                acceptsAesync,
                scope,
                family,
                runtimeModel,
                sourceRepo,
                releaseTag,
                artifactName,
                entryName
        );
    }

    private static String buildEntryName(ContentProfile runtimeProfile) {
        if (runtimeProfile == null) return "";
        String type = runtimeProfile.type != null ? runtimeProfile.type.toString() : "";
        String version = trim(runtimeProfile.verName);
        if (type.isEmpty() || version.isEmpty()) return "";
        if (runtimeProfile.verCode > 0) return type + "-" + version + "-" + runtimeProfile.verCode;
        return type + "-" + version;
    }

    private static final class RuntimeAcceptance {
        private final boolean acceptsAesync;
        private final String scope;
        private final String family;
        private final String runtimeModel;
        private final String sourceRepo;
        private final String releaseTag;
        private final String artifactName;
        private final String entryName;

        private RuntimeAcceptance(
                boolean acceptsAesync,
                String scope,
                String family,
                String runtimeModel,
                String sourceRepo,
                String releaseTag,
                String artifactName,
                String entryName
        ) {
            this.acceptsAesync = acceptsAesync;
            this.scope = scope;
            this.family = family;
            this.runtimeModel = runtimeModel;
            this.sourceRepo = sourceRepo;
            this.releaseTag = releaseTag;
            this.artifactName = artifactName;
            this.entryName = entryName;
        }
    }
}
