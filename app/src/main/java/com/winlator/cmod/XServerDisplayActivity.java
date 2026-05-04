package com.winlator.cmod;

import static com.winlator.cmod.core.AppUtils.showToast;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import com.winlator.cmod.alsaserver.ALSAClient;
import com.winlator.cmod.box64.Box64EditPresetDialog;
import com.winlator.cmod.box64.Box64Preset;
import com.winlator.cmod.box64.Box64PresetManager;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.IntentLaunchManager;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.container.GraphicsDrivers;
import com.winlator.cmod.container.Shortcut;
import com.winlator.cmod.contentdialog.ActiveWindowsDialog;
import com.winlator.cmod.contentdialog.ContentDialog;
import com.winlator.cmod.contentdialog.DXVKConfigDialog;
import com.winlator.cmod.contentdialog.DebugDialog;
import com.winlator.cmod.contentdialog.DgVoodooConfigDialog;
import com.winlator.cmod.contentdialog.GraphicsDriverConfigDialog;
import com.winlator.cmod.contentdialog.MesaOpenGLConfigDialog;
import com.winlator.cmod.contentdialog.PrefixPackToolkitDialog;
import com.winlator.cmod.contentdialog.ScreenEffectDialog;
import com.winlator.cmod.contentdialog.VirGLConfigDialog;
import com.winlator.cmod.contentdialog.VortekConfigDialog;
import com.winlator.cmod.contentdialog.WineD3DConfigDialog;
import com.winlator.cmod.contract.RuntimeSignalContract;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentPayloadResolver;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.contents.AdrenotoolsManager;
import com.winlator.cmod.contents.DgVoodooManager;
import com.winlator.cmod.contents.Downloader;
import com.winlator.cmod.contents.GladioOpenGLDriverPackageManager;
import com.winlator.cmod.contents.MesaOpenGLDriverPackageManager;
import com.winlator.cmod.contents.RemoteFeedPayloadLoader;
import com.winlator.cmod.contents.RemoteProfileFeedMerger;
import com.winlator.cmod.contents.RuntimeFeedRegistry;
import com.winlator.cmod.contents.RuntimeLaunchPolicy;
import com.winlator.cmod.contents.VirGLDriverPackageManager;
import com.winlator.cmod.contents.VortekWrapperPackageManager;
import com.winlator.cmod.contents.VortekVulkanDriverPackageManager;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.Callback;
import com.winlator.cmod.core.DefaultVersion;
import com.winlator.cmod.core.EnvVars;
import com.winlator.cmod.core.FileDebugLogger;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.ForensicConfig;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.core.ForensicUi;
import com.winlator.cmod.core.GPUHelper;
import com.winlator.cmod.core.GPUInformation;
import com.winlator.cmod.core.KeyValueSet;
import com.winlator.cmod.core.LaunchSecurity;
import com.winlator.cmod.core.OnExtractFileListener;
import com.winlator.cmod.core.PreloaderDialog;
import com.winlator.cmod.core.ProcessHelper;
import com.winlator.cmod.core.SocClassifier;
import com.winlator.cmod.core.SpinnerAdapters;
import com.winlator.cmod.core.StringUtils;
import com.winlator.cmod.core.ThemeAssetPainter;
import com.winlator.cmod.core.WinlatorNative;
import com.winlator.cmod.core.TarCompressorUtils;
import com.winlator.cmod.core.UpscalerProfileStore;
import com.winlator.cmod.core.VulkanIcdManifestHelper;
import com.winlator.cmod.core.VortekExtensionPolicy;
import com.winlator.cmod.core.WineInfo;
import com.winlator.cmod.core.WineRegistryEditor;
import com.winlator.cmod.core.WineStartMenuCreator;
import com.winlator.cmod.core.WineThemeManager;
import com.winlator.cmod.core.WineUtils;
import com.winlator.cmod.fexcore.FEXCoreEditPresetDialog;
import com.winlator.cmod.fexcore.FEXCoreManager;
import com.winlator.cmod.fexcore.FEXCorePreset;
import com.winlator.cmod.fexcore.FEXCorePresetManager;
import com.winlator.cmod.graphics.GraphicsElfCompatibility;
import com.winlator.cmod.inputcontrols.ControlsProfile;
import com.winlator.cmod.inputcontrols.ExternalController;
import com.winlator.cmod.inputcontrols.InputControlsManager;
import com.winlator.cmod.math.Mathf;
import com.winlator.cmod.math.XForm;
import com.winlator.cmod.midi.MidiHandler;
import com.winlator.cmod.midi.MidiManager;
import com.winlator.cmod.renderer.GLRenderer;
import com.winlator.cmod.renderer.effects.CRTEffect;
import com.winlator.cmod.renderer.effects.ColorEffect;
import com.winlator.cmod.renderer.effects.FXAAEffect;
import com.winlator.cmod.renderer.effects.NTSCCombinedEffect;
import com.winlator.cmod.renderer.effects.ToonEffect;
import com.winlator.cmod.runtimeprofile.RuntimeProfile;
import com.winlator.cmod.runtimeprofile.RuntimeProfileManager;
import com.winlator.cmod.runtimeprofile.WineSyncPolicy;
import com.winlator.cmod.widget.FrameRating;
import com.winlator.cmod.widget.InputControlsView;
import com.winlator.cmod.widget.LogView;
import com.winlator.cmod.widget.MagnifierView;
import com.winlator.cmod.widget.TouchpadView;
import com.winlator.cmod.widget.XServerView;
import com.winlator.cmod.winhandler.MouseEventFlags;
import com.winlator.cmod.winhandler.TaskManagerDialog;
import com.winlator.cmod.winhandler.WinHandler;
import com.winlator.cmod.xconnector.UnixSocketConfig;
import com.winlator.cmod.xenvironment.ImageFs;
import com.winlator.cmod.xenvironment.ImageFsInstaller;
import com.winlator.cmod.xenvironment.XEnvironment;
import com.winlator.cmod.xenvironment.components.ALSAServerComponent;
import com.winlator.cmod.xenvironment.components.GuestProgramLauncherComponent;
import com.winlator.cmod.xenvironment.components.GuestProgramLauncherFactory;
import com.winlator.cmod.xenvironment.components.NetworkInfoUpdateComponent;
import com.winlator.cmod.xenvironment.components.PulseAudioComponent;
import com.winlator.cmod.xenvironment.components.SteamClientComponent;
import com.winlator.cmod.xenvironment.components.SysVSharedMemoryComponent;
import com.winlator.cmod.xenvironment.components.VirGLRendererComponent;
import com.winlator.cmod.xenvironment.components.VortekRendererComponent;
import com.winlator.cmod.xenvironment.components.WineRequestComponent;
import com.winlator.cmod.xenvironment.components.XServerComponent;
import com.winlator.cmod.xserver.Pointer;
import com.winlator.cmod.xserver.Property;
import com.winlator.cmod.xserver.ScreenInfo;
import com.winlator.cmod.xserver.Window;
import com.winlator.cmod.xserver.WindowManager;
import com.winlator.cmod.xserver.XLock;
import com.winlator.cmod.xserver.XServer;
import com.winlator.cmod.xserver.events.Event;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.sherlock.com.sun.media.sound.SF2Soundbank;

public class XServerDisplayActivity extends AppCompatActivity {
    public static String NOTIFICATION_CHANNEL_ID = "Aesolator";
    public static int NOTIFICATION_ID = -1;
    private static final String NOEXEC_LAUNCH_MIRROR_DIR = "AeLaunchMirror";
    private static final String NOEXEC_LAUNCH_MIRROR_STAMP = ".aelaunchmirror.json";
    private static final String EXTRA_APPLIED_CONTAINER_VARIANT = "appliedContainerVariant";
    private static final String EXTRA_APPLIED_RUNTIME_MODEL = "appliedRuntimeModel";
    private static final String EXTRA_APPLIED_WINE_VERSION = "appliedWineVersion";
    private static final String EXTRA_WINEPREFIX_RUNTIME_MODEL = "wineprefixRuntimeModel";
    private static final String EXTRA_WINEPREFIX_WINE_VERSION = "wineprefixWineVersion";
    private static final String[] ANDROID_HOST_WRAPPER_REQUIRED_LIBS = {
            "libandroid-sysvshm.so",
            "libadrenotools.so",
            "libnativewindow.so",
            "libxcb.so",
            "libX11-xcb.so",
            "libxcb-dri3.so",
            "libxcb-present.so",
            "libxcb-sync.so",
            "libxcb-randr.so",
            "libxcb-shm.so",
            "libxshmfence.so",
            "libdrm.so"
    };
    private XServerView xServerView;
    private InputControlsView inputControlsView;
    private TouchpadView touchpadView;
    private XEnvironment environment;
    private View xserverRootView;
    private View runtimeDrawerScrim;
    private View runtimeDrawerView;
    private boolean runtimeDrawerVisible = false;
    private boolean xServerViewLifecyclePaused = false;
    private ContainerManager containerManager;
    protected Container container;
    private XServer xServer;
    private InputControlsManager inputControlsManager;
    private ImageFs imageFs;
    private FrameRating frameRating = null;
    private Runnable editInputControlsCallback;
    private Shortcut shortcut;
    @Nullable
    private WineUtils.WindowsLaunchTarget effectiveShortcutLaunchTarget;
    private String graphicsDriver = Container.DEFAULT_GRAPHICS_DRIVER;
    private String legacyGraphicsRequestedDriver = "";
    private String legacyGraphicsProviderHint = "";
    private String legacyGraphicsPolicy = "";
    private String rawGraphicsDriverConfig = Container.DEFAULT_GRAPHICSDRIVERCONFIG;
    private HashMap<String, String> graphicsDriverConfig;
    private String audioDriver = Container.DEFAULT_AUDIO_DRIVER;
    private String emulator = Container.DEFAULT_EMULATOR;
    private String dxwrapper = Container.DEFAULT_DXWRAPPER;
    private KeyValueSet dxwrapperConfig;
    private String startupSelection;
    private WineInfo wineInfo;
    private ContentProfile selectedRuntimeProfile;
    private String effectiveRuntimeModel = Container.DEFAULT_VARIANT;
    private final EnvVars envVars = new EnvVars();
    private boolean firstTimeBoot = false;
    private SharedPreferences preferences;
    private OnExtractFileListener onExtractFileListener;
    private WinHandler winHandler;
    private float globalCursorSpeed = 1.0f;
    private boolean openWithAndroidBrowserEnabled;
    private boolean shareAndroidClipboardEnabled;
    private MagnifierView magnifierView;
    private DebugDialog debugDialog;
    private final ArrayList<Callback<String>> forensicRuntimeCallbacks = new ArrayList<>();
    private int taskAffinityMask = 0;
    private int taskAffinityMaskWoW64 = 0;
    private int frameRatingWindowId = -1;
    private boolean cursorLock; // Flag to track if pointer capture was requested
    private final float[] xform = XForm.getInstance();
    private ContentsManager contentsManager;
    private MidiHandler midiHandler;
    private String midiSoundFont = "";
    private String lc_all = "";
    private String vkbasaltConfig = "";
    private String upscalerPreset = "auto";
    private String upscalerBackend = "off";
    private String upscalerEffect = "none";
    private int upscalerScalePercent = 100;
    private int upscalerSharpnessPercent = 100;
    private int upscalerDenoisePercent = 100;
    private boolean upscalerFrameGeneration = false;
    private int upscalerGeneratedFrames = 1;
    private String upscalerFgSource = "native";
    private String upscalerFgOutput = "auto";
    private String upscalerFramegenMode = "balanced";
    private boolean upscalerThermalGuard = true;
    private int upscalerTargetFps = 60;
    private int upscalerInterpolationFactor = 50;
    private boolean upscalerDebugOverlay = false;
    private boolean upscalerDebugTearLines = false;
    private boolean upscalerInterpolatedOnly = false;
    private boolean upscalerVulkanValidationLayer = false;
    private String upscalerBackendSource = "global_profile";
    private String upscalerPresetSource = "global_profile";
    private String upscalerFramegenSource = "global_profile";
    private String upscalerValidationSource = "global_profile";
    private boolean upscalerDeprecatedAliasUsed = false;
    PreloaderDialog preloaderDialog = null;
    private Runnable configChangedCallback = null;
    private Runnable pendingBootstrapRunnable = null;
    private String pendingBootstrapSource = "";
    private boolean guestBootstrapSubmitted = false;
    private boolean bootstrapWaitingForFocus = false;
    private boolean isPaused = false;
    private boolean isRelativeMouseMovement = false;
    private final LinkedHashSet<Integer> mappedApplicationWindowIds = new LinkedHashSet<>();
    private static final String DESKTOP_SHELL_LAUNCH_MODE_WINHANDLER = "winhandler_shell";
    private static final String DESKTOP_SHELL_LAUNCH_MODE_DIRECT_EXPLORER = "direct_explorer";
    private static final long DESKTOP_SHELL_DIRECT_EXPLORER_FALLBACK_DELAY_MS = 6000L;
    private volatile boolean desktopShellBootstrapActive = false;
    private volatile String desktopShellLaunchMode = DESKTOP_SHELL_LAUNCH_MODE_WINHANDLER;
    private volatile boolean desktopShellWinHandlerFallbackAttempted = false;
    private volatile boolean desktopShellDetachedFallbackActive = false;
    private volatile boolean guestLauncherExited = false;
    private volatile int guestLauncherExitStatus = Integer.MIN_VALUE;
    private volatile long lastTrackedApplicationWindowMappedAtMs = 0L;
    private volatile String lastTrackedApplicationWindowClassName = "";
    private final Set<View> desktopGestureExclusionTrackedViews =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<View, String> desktopGestureExclusionLastState = new IdentityHashMap<>();
    private boolean bootstrapFirstDrawObserved = false;

    // Inside the XServerDisplayActivity class
    private SensorManager sensorManager;
    private Sensor gyroSensor;
    private HandlerThread gyroHandlerThread;
    private Handler gyroHandler;
    private boolean gyroListenerRegistered = false;
    private ExternalController controller;

    // Playtime stats tracking
    private long startTime;
    private SharedPreferences playtimePrefs;
    private String shortcutName;
    private Handler handler;
    private Runnable savePlaytimeRunnable;
    private static final long SAVE_INTERVAL_MS = 1000;
    private final ExecutorService exitTeardownExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService runtimeBootstrapExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "XServerBootstrap");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean exitInProgress = new AtomicBoolean(false);
    private int activeLaunchContainerId = 0;
    private String activeLaunchShortcutPath = "";
    private String activeLaunchAppId = "";
    private String activeLaunchRouteToken = "";
    private String activeTemporaryOverrideAppId = "";
    private boolean activeTemporaryOverrideRestored = false;
    private int launchBindingGeneration = 1;
    private static final String EXTRA_GLIBC_PROMOTION_PROBE_DONE =
            "com.winlator.cmod.extra.GLIBC_PROMOTION_PROBE_DONE";
    private static final long LAUNCH_RUNTIME_HYDRATION_TIMEOUT_MS = 8000L;
    private static final long READY_GLIBC_PROMOTION_HYDRATION_TIMEOUT_MS = 3500L;

    private Handler  timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable hideControlsRunnable;
    private static final long DESKTOP_RUNTIME_PAUSE_GRACE_MS = 1800L;
    private static final long DESKTOP_RUNTIME_STOP_BOOTSTRAP_GRACE_MS = 12000L;
    private static final long DESKTOP_RUNTIME_STOP_BOOTSTRAP_MAX_MS = 180000L;
    private static final long DESKTOP_SHELL_TERMINATION_GRACE_MS = 8000L;
    private static final long DESKTOP_SHELL_PRELOADER_FALLBACK_INITIAL_DELAY_MS = 3500L;
    private static final long DESKTOP_SHELL_PRELOADER_FALLBACK_RETRY_MS = 1200L;
    private static final int DESKTOP_SHELL_PRELOADER_FALLBACK_MAX_ATTEMPTS = 60;
    private static final long DESKTOP_SHELL_WINHANDLER_INIT_TIMEOUT_MS = 5000L;
    private static final long DESKTOP_SHELL_BOOTSTRAP_HORIZON_MS =
            DESKTOP_SHELL_PRELOADER_FALLBACK_INITIAL_DELAY_MS
                    + (DESKTOP_SHELL_PRELOADER_FALLBACK_RETRY_MS * DESKTOP_SHELL_PRELOADER_FALLBACK_MAX_ATTEMPTS);
    private static final class LaunchHydrationResult {
        final boolean completed;
        final boolean networkAvailable;
        final boolean networkAttempted;
        final long elapsedMs;
        final String failureClass;

        LaunchHydrationResult(boolean completed,
                              boolean networkAvailable,
                              boolean networkAttempted,
                              long elapsedMs,
                              String failureClass) {
            this.completed = completed;
            this.networkAvailable = networkAvailable;
            this.networkAttempted = networkAttempted;
            this.elapsedMs = elapsedMs;
            this.failureClass = failureClass == null ? "" : failureClass;
        }
    }
    private final Handler runtimePauseHandler = new Handler(Looper.getMainLooper());
    private boolean deferredDesktopPauseScheduled = false;
    private long deferredDesktopPauseDeadlineAtMs = 0L;
    private boolean deferredGuestTerminationScheduled = false;
    private long desktopShellBootstrapStartedAtMs = 0L;
    private volatile boolean guestVisualReady = false;
    private boolean desktopShellLiveNoWindowHorizonLogged = false;
    private int debugStartProbeTargetX = Integer.MIN_VALUE;
    private int debugStartProbeTargetY = Integer.MIN_VALUE;
    private int debugStartProbeTapCount = 1;
    private int debugStartProbeTapIntervalMs = 110;
    private boolean debugAutoOpenTaskManagerArmed = false;
    private boolean debugAutoOpenTaskManagerExecuted = false;
    private boolean debugAutoOpenRuntimeDrawerArmed = false;
    private boolean debugAutoOpenRuntimeDrawerExecuted = false;
    private boolean debugAutoOpenLogsArmed = false;
    private boolean debugAutoOpenLogsExecuted = false;
    private boolean debugAutoOpenPrefixPackArmed = false;
    private boolean debugAutoOpenPrefixPackExecuted = false;
    private String debugAutoInstallPrefixPackTarget = "";
    private boolean forensicModeLaunch = false;
    private String forensicTraceId = "";
    private boolean forensicTraceGenerated = false;
    private String forensicRouteSource = "";
    private static final long DEBUG_PREFIXPACK_FALLBACK_INITIAL_DELAY_MS = 6500L;
    private static final long DEBUG_PREFIXPACK_FALLBACK_RETRY_MS = 900L;
    private static final int DEBUG_PREFIXPACK_FALLBACK_MAX_ATTEMPTS = 18;
    private final Runnable deferredDesktopPauseRunnable = new Runnable() {
        @Override
        public void run() {
            deferredDesktopPauseScheduled = false;
            deferredDesktopPauseDeadlineAtMs = 0L;
            if (!shouldAutoSuspendRuntimeOnLifecycle()) {
                logDesktopRuntimePauseSkipped("deferred_background_pause_policy_skip");
                return;
            }
            int trackedWindowCount = getTrackedApplicationWindowCount();
            DesktopShellBootstrapProof proof = desktopShellBootstrapActive
                    ? collectDesktopShellBootstrapProof()
                    : null;
            if (shouldRenewDeferredDesktopRuntimePause(proof, trackedWindowCount)) {
                scheduleDeferredDesktopRuntimePause(DESKTOP_RUNTIME_STOP_BOOTSTRAP_GRACE_MS);
                ForensicLogger.logEvent(
                        XServerDisplayActivity.this,
                        "info",
                        "XSERVER_RUNTIME_PAUSE_RENEWED",
                        null,
                        "xserver",
                        "desktop_runtime_pause_renewed_for_live_bootstrap",
                        ForensicLogger.fields(
                                "desktop_shell_bootstrap", desktopShellBootstrapActive,
                                "tracked_window_count", trackedWindowCount,
                                "grace_ms", DESKTOP_RUNTIME_STOP_BOOTSTRAP_GRACE_MS,
                                "max_grace_ms", DESKTOP_RUNTIME_STOP_BOOTSTRAP_MAX_MS,
                                "bootstrap_elapsed_ms", proof != null ? proof.bootstrapElapsedMs : 0L,
                                "shell_launcher_present", proof != null && proof.shellLauncherPresent,
                                "shell_process_present", proof != null && proof.explorerProcessPresent,
                                "wineboot_process_present", proof != null && proof.winebootProcessPresent,
                                "wineserver_present", proof != null && proof.wineserverPresent,
                                "suspend_policy", getEffectiveSuspendPolicy()
                        )
                );
                return;
            }
            pauseDesktopRuntime("deferred_background_pause");
        }
    };
    private final Runnable deferredGuestTerminationRunnable = new Runnable() {
        @Override
        public void run() {
            deferredGuestTerminationScheduled = false;
            int trackedCount = getTrackedApplicationWindowCount();
            DesktopShellBootstrapProof proof = desktopShellBootstrapActive
                    ? collectDesktopShellBootstrapProof()
                    : null;
            if (!desktopShellBootstrapActive || !guestLauncherExited || trackedCount > 0) {
                ForensicLogger.logEvent(
                        XServerDisplayActivity.this,
                        "info",
                        "GUEST_PROGRAM_TERMINATION_DEFER_CANCELLED",
                        null,
                        "xserver",
                        "guest_program_termination_grace_cancelled",
                        ForensicLogger.fields(
                                "tracked_window_count", trackedCount,
                                "desktop_shell_bootstrap", desktopShellBootstrapActive,
                                "guest_launcher_exited", guestLauncherExited
                        )
                );
                return;
            }
            if (shouldKeepDesktopShellAliveAfterPrimaryTermination(proof, trackedCount)) {
                boolean withinHorizon = isDesktopShellBootstrapWithinHorizon(proof);
                if (!withinHorizon && !desktopShellLiveNoWindowHorizonLogged) {
                    desktopShellLiveNoWindowHorizonLogged = true;
                    logBootstrapWindowSnapshot(
                            "XSERVER_WINDOW_FRONTIER_LIVE_NONVISUAL_HORIZON_EXTENDED",
                            "desktop_shell_live_nonvisual_horizon_extended"
                    );
                    ForensicLogger.logEvent(
                            XServerDisplayActivity.this,
                            "warn",
                            "GUEST_PROGRAM_TERMINATION_HORIZON_EXTENDED_FOR_LIVE_SHELL",
                            null,
                            "xserver",
                            "guest_program_termination_horizon_extended_for_live_shell",
                            ForensicLogger.fields(
                                    "tracked_window_count", trackedCount,
                                    "guest_launcher_exit_status", guestLauncherExitStatus,
                                    "bootstrap_elapsed_ms", proof != null ? proof.bootstrapElapsedMs : 0L,
                                    "desktop_shell_launch_mode", desktopShellLaunchMode,
                                    "shell_launcher_present", proof != null && proof.shellLauncherPresent,
                                    "shell_process_present", proof != null && proof.explorerProcessPresent,
                                    "winhandler_process_present", proof != null && proof.winHandlerProcessPresent,
                                    "wfm_process_present", proof != null && proof.wfmProcessPresent,
                                    "wineboot_process_present", proof != null && proof.winebootProcessPresent,
                                    "wineserver_present", proof != null && proof.wineserverPresent,
                                    "winhandler_ready", winHandler != null && winHandler.isReady()
                            )
                    );
                }
                scheduleDeferredGuestTermination(guestLauncherExitStatus);
                ForensicLogger.logEvent(
                        XServerDisplayActivity.this,
                        "info",
                        "GUEST_PROGRAM_TERMINATION_DEFER_RENEWED",
                        null,
                        "xserver",
                        "guest_program_termination_grace_renewed",
                        ForensicLogger.fields(
                                "tracked_window_count", trackedCount,
                                "guest_launcher_exit_status", guestLauncherExitStatus,
                                "bootstrap_elapsed_ms", proof != null ? proof.bootstrapElapsedMs : 0L,
                                "desktop_shell_launch_mode", desktopShellLaunchMode,
                                "fallback_active", desktopShellDetachedFallbackActive
                        )
                );
                return;
            }

            ForensicLogger.logEvent(
                    XServerDisplayActivity.this,
                    "warn",
                    "GUEST_PROGRAM_TERMINATION_GRACE_EXPIRED",
                    null,
                    "xserver",
                    "guest_program_termination_grace_expired",
                    ForensicLogger.fields(
                            "tracked_window_count", trackedCount,
                            "guest_launcher_exit_status", guestLauncherExitStatus,
                            "bootstrap_elapsed_ms", Math.max(0L, System.currentTimeMillis() - desktopShellBootstrapStartedAtMs)
                    )
            );
            runOnUiThread(XServerDisplayActivity.this::exit);
        }
    };

    private boolean isDarkMode;

    private String screenEffectProfile;

    private GuestProgramLauncherComponent guestProgramLauncherComponent;
    private EnvVars overrideEnvVars;
    private static final String TOUCHPAD_PROFILE_GLOBAL = "global";
    private static final String TOUCHPAD_PROFILE_BALANCED = "balanced";
    private static final String TOUCHPAD_PROFILE_AGGRESSIVE = "aggressive";
    private static final String TOUCHPAD_PROFILE_COMPAT = "compat";
    private static final String UPSCALER_BACKEND_OFF = "off";
    private static final String UPSCALER_BACKEND_VKBASALT = "vkbasalt";
    private static final String UPSCALER_BACKEND_LSFG = "lsfg";
    private static final String UPSCALER_EFFECT_NONE = "none";
    private static final String FG_SOURCE_NATIVE = "native";
    private static final String FG_SOURCE_OPTI_FG = "opti_fg";
    private static final String FG_OUTPUT_AUTO = "auto";
    private static final String FG_OUTPUT_LSFG = "lsfg";
    private boolean debugStartProbeArmed = false;
    private boolean debugStartProbeExecuted = false;
    private static final String FRAMEGEN_MODE_BALANCED = "balanced";
    private static final String FRAMEGEN_MODE_QUALITY = "quality";
    private static final String FRAMEGEN_MODE_LOW_LATENCY = "low_latency";
    private static final String UPSCALER_PRESET_AUTO = "auto";
    private static final String UPSCALER_PRESET_CONSERVATIVE = "conservative";
    private static final String UPSCALER_PRESET_BALANCED = "balanced";
    private static final String UPSCALER_PRESET_AGGRESSIVE = "aggressive";

    private static final class TouchpadGestureDefaults {
        final boolean strictFsm;
        final int tapTimeoutMs;
        final int tapTravelPx;
        final int scrollStepPx;
        final int scrollZonePx;

        TouchpadGestureDefaults(boolean strictFsm, int tapTimeoutMs, int tapTravelPx, int scrollStepPx, int scrollZonePx) {
            this.strictFsm = strictFsm;
            this.tapTimeoutMs = tapTimeoutMs;
            this.tapTravelPx = tapTravelPx;
            this.scrollStepPx = scrollStepPx;
            this.scrollZonePx = scrollZonePx;
        }
    }

    private void createNotifcationChannel() {
        String name = "Aesolator";
        String description = "Aesolator XServer Messages";
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
        channel.setDescription(description);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (xServerView != null) {
            xServerView.requestLayout();
            xServerView.post(() -> {
                try {
                    xServerView.getHolder().setSizeFromLayout();
                } catch (Throwable ignored) {
                }
            });
        }
        if (inputControlsView != null) {
            inputControlsView.requestLayout();
        }
        if (touchpadView != null) touchpadView.toggleFullscreen();
        if (inputControlsView != null) inputControlsView.post(inputControlsView::invalidate);
        if (configChangedCallback != null) {
            logBootstrapCheckpoint(
                    "XSERVER_BOOTSTRAP_CONFIG_CALLBACK_RUN",
                    "bootstrap_configuration_callback_running",
                    "current_orientation", newConfig.orientation,
                    "activity_has_focus", hasWindowFocus(),
                    "activity_finishing", isFinishing(),
                    "activity_destroyed", isDestroyed()
            );
            configChangedCallback.run();
            configChangedCallback = null;
        }
    }


    private final SensorEventListener gyroListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                float gyroX = event.values[0]; // Rotation around the X-axis
                float gyroY = event.values[1]; // Rotation around the Y-axis

                winHandler.updateGyroData(gyroX, gyroY); // Send gyro data to WinHandler
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // No action needed
        }
    };

    private void initializeGyroSensor() {
        if (sensorManager == null) {
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        }
        if (sensorManager != null && gyroSensor == null) {
            gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }
    }

    private void registerGyroListenerIfEnabled(String reason) {
        if (!preferences.getBoolean("gyro_enabled", true)) {
            unregisterGyroListenerIfNeeded(reason + "_disabled");
            return;
        }
        initializeGyroSensor();
        if (sensorManager == null || gyroSensor == null) {
            ForensicLogger.logEvent(
                    this,
                    "warn",
                    "XSERVER_GYRO_SENSOR_UNAVAILABLE",
                    null,
                    "xserver",
                    "gyro_sensor_unavailable",
                    ForensicLogger.fields("reason", reason)
            );
            return;
        }
        if (gyroListenerRegistered) return;
        if (gyroHandlerThread == null || !gyroHandlerThread.isAlive()) {
            gyroHandlerThread = new HandlerThread("XServerGyro");
            gyroHandlerThread.start();
            gyroHandler = new Handler(gyroHandlerThread.getLooper());
        }
        gyroListenerRegistered = sensorManager.registerListener(
                gyroListener,
                gyroSensor,
                SensorManager.SENSOR_DELAY_GAME,
                gyroHandler
        );
        ForensicLogger.logEvent(
                this,
                gyroListenerRegistered ? "info" : "warn",
                gyroListenerRegistered ? "XSERVER_GYRO_REGISTERED" : "XSERVER_GYRO_REGISTER_FAILED",
                null,
                "xserver",
                gyroListenerRegistered ? "gyro_listener_registered" : "gyro_listener_register_failed",
                ForensicLogger.fields("reason", reason, "handler_thread", "XServerGyro")
        );
    }

    private void unregisterGyroListenerIfNeeded(String reason) {
        if (sensorManager == null || !gyroListenerRegistered) return;
        sensorManager.unregisterListener(gyroListener);
        gyroListenerRegistered = false;
        ForensicLogger.logEvent(
                this,
                "info",
                "XSERVER_GYRO_UNREGISTERED",
                null,
                "xserver",
                "gyro_listener_unregistered",
                ForensicLogger.fields("reason", reason)
        );
    }

    private void shutdownGyroThread() {
        unregisterGyroListenerIfNeeded("shutdown");
        if (gyroHandlerThread != null) {
            gyroHandlerThread.quitSafely();
            gyroHandlerThread = null;
            gyroHandler = null;
        }
    }

    private float pickHighestRefreshRate() {
        android.view.Display display = getWindowManager().getDefaultDisplay();
        android.view.Display.Mode[] modes = display.getSupportedModes();

        float maxRefresh = 0f;

        for (android.view.Display.Mode mode : modes) {
            if (mode.getRefreshRate() > maxRefresh) {
                maxRefresh = mode.getRefreshRate();
            }
        }

        Log.d("XServerDisplayActivity", "Picking refresh rate " + maxRefresh);

        return maxRefresh;
    }

    private boolean requiresSignedLaunchIntent(Intent intent) {
        if (intent == null) return false;
        if (!getClass().equals(XServerDisplayActivity.class)) return false;
        if (LaunchSecurity.hasXServerLaunchSignature(intent)) return true;
        return intent.hasExtra("shortcut_name")
                || intent.hasExtra("disableXinput")
                || intent.hasExtra(LaunchSecurity.EXTRA_APP_ID)
                || intent.hasExtra(LaunchSecurity.EXTRA_LAUNCH_ROUTE_TOKEN)
                || intent.hasExtra(LaunchSecurity.EXTRA_TEMP_OVERRIDE_APP_ID);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtils.hideSystemUI(this);
        AppUtils.keepScreenOn(this);

        android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
        params.preferredRefreshRate = pickHighestRefreshRate();
        getWindow().setAttributes(params);

        setContentView(R.layout.xserver_display_activity);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleDesktopBackNavigation();
            }
        });

        preloaderDialog = new PreloaderDialog(this);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        cursorLock = preferences.getBoolean("cursor_lock", false);

        // Check for Dark Mode
        isDarkMode = preferences.getBoolean("dark_mode", false);

        openWithAndroidBrowserEnabled = preferences.getBoolean("open_with_android_browser", false);
        shareAndroidClipboardEnabled = preferences.getBoolean("share_android_clipboard", false);

        // Initialize the WinHandler after context is set up
        winHandler = new WinHandler(this);
        winHandler.initializeController();
        controller = winHandler.getCurrentController();

        if (controller != null) {
            int triggerType = preferences.getInt("trigger_type", ExternalController.TRIGGER_IS_AXIS); // Default to TRIGGER_IS_AXIS
            controller.setTriggerType((byte) triggerType); // Cast to byte if needed
        }

        Intent launchIntent = getIntent();
        refreshForensicTrace(launchIntent);
        boolean debugBuild = (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        boolean hasExplicitProbeTarget = launchIntent != null
                && launchIntent.hasExtra("aeso_debug_probe_tap_x")
                && launchIntent.hasExtra("aeso_debug_probe_tap_y");
        debugStartProbeArmed = debugBuild
                && launchIntent != null
                && (launchIntent.getBooleanExtra("aeso_debug_probe_start_tap", false) || hasExplicitProbeTarget);
        if (debugBuild && launchIntent != null) {
            if (hasExplicitProbeTarget) {
                debugStartProbeTargetX = launchIntent.getIntExtra("aeso_debug_probe_tap_x", Integer.MIN_VALUE);
                debugStartProbeTargetY = launchIntent.getIntExtra("aeso_debug_probe_tap_y", Integer.MIN_VALUE);
            }
            debugStartProbeTapCount = Math.max(1, Math.min(4, launchIntent.getIntExtra("aeso_debug_probe_tap_count", 1)));
            debugStartProbeTapIntervalMs = Math.max(40, Math.min(400, launchIntent.getIntExtra("aeso_debug_probe_tap_interval_ms", 110)));
            debugAutoOpenTaskManagerArmed = launchIntent.getBooleanExtra("aeso_debug_open_task_manager", false);
            debugAutoOpenRuntimeDrawerArmed = launchIntent.getBooleanExtra("aeso_debug_open_runtime_drawer", false);
            debugAutoOpenLogsArmed = launchIntent.getBooleanExtra("aeso_debug_open_logs", false);
            debugAutoOpenPrefixPackArmed = launchIntent.getBooleanExtra("aeso_debug_open_prefix_pack", false);
            debugAutoInstallPrefixPackTarget = launchIntent.getStringExtra("aeso_debug_prefix_pack_install_target");
        }
        String launchTrustState = LaunchSecurity.getXServerLaunchTrustState(this, launchIntent);
        ForensicLogger.logEvent(
                this,
                "info",
                "XSERVER_LAUNCH_TRUST_EVAL",
                null,
                "xserver",
                "launch_trust_evaluated",
                ForensicLogger.fields(
                        "container_id", launchIntent != null ? launchIntent.getIntExtra("container_id", 0) : 0,
                        "shortcut_path", launchIntent != null ? launchIntent.getStringExtra("shortcut_path") : "",
                        "app_id", resolveIntentLaunchAppId(launchIntent),
                        "launch_route_token", resolveIntentLaunchRouteToken(launchIntent),
                        "temporary_override_app_id", resolveIntentTemporaryOverrideAppId(launchIntent),
                        "debug_start_probe_armed", debugStartProbeArmed,
                        "debug_probe_target_x", debugStartProbeTargetX,
                        "debug_probe_target_y", debugStartProbeTargetY,
                        "debug_probe_tap_count", debugStartProbeTapCount,
                        "debug_probe_tap_interval_ms", debugStartProbeTapIntervalMs,
                        "debug_auto_open_task_manager_armed", debugAutoOpenTaskManagerArmed,
                        "debug_auto_open_runtime_drawer_armed", debugAutoOpenRuntimeDrawerArmed,
                        "debug_auto_open_logs_armed", debugAutoOpenLogsArmed,
                        "debug_auto_open_prefix_pack_armed", debugAutoOpenPrefixPackArmed,
                        "debug_prefix_pack_install_target", debugAutoInstallPrefixPackTarget,
                        "requires_signature", requiresSignedLaunchIntent(launchIntent),
                        "has_signature", LaunchSecurity.hasXServerLaunchSignature(launchIntent),
                        "trust_state", launchTrustState
                )
        );
        if (forensicModeLaunch || !forensicTraceId.isEmpty()) {
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "ROUTE_INTENT_RECEIVED",
                    forensicTraceIdOrNull(),
                    "xserver",
                    "route_intent_received",
                    ForensicLogger.fields(
                            "container_id", launchIntent != null ? launchIntent.getIntExtra("container_id", 0) : 0,
                            "shortcut_path", launchIntent != null ? launchIntent.getStringExtra("shortcut_path") : "",
                            "app_id", resolveIntentLaunchAppId(launchIntent),
                            "launch_route_token", resolveIntentLaunchRouteToken(launchIntent),
                            "forensic_mode", forensicModeLaunch,
                            "forensic_trace_generated", forensicTraceGenerated,
                            "forensic_route_source", forensicRouteSource,
                            "debug_start_probe_armed", debugStartProbeArmed
                    )
            );
        }
        if (requiresSignedLaunchIntent(launchIntent)
                && !LaunchSecurity.isTrustedXServerLaunchIntent(this, launchIntent)) {
            ForensicLogger.logEvent(
                    this,
                    "warn",
                    "XSERVER_LAUNCH_REJECTED",
                    null,
                    "xserver",
                    "launch_signature_invalid",
                    ForensicLogger.fields(
                            "container_id", launchIntent.getIntExtra("container_id", 0),
                            "shortcut_path", launchIntent.getStringExtra("shortcut_path"),
                            "has_signature", LaunchSecurity.hasXServerLaunchSignature(launchIntent),
                            "trust_state", launchTrustState,
                            "adb_diagnostics_cmd", "adb logcat -d | grep XSERVER_LAUNCH_"
                    )
            );
            showToast(this, R.string.blocked_untrusted_launch_request);
            finish();
            return;
        }

        ForensicLogger.logEvent(
                this,
                "info",
                "XSERVER_CLIPBOARD_POLICY_APPLIED",
                null,
                "xserver",
                "clipboard_policy_applied",
                ForensicLogger.fields(
                        "share_android_clipboard", shareAndroidClipboardEnabled,
                        "open_with_android_browser", openWithAndroidBrowserEnabled
                )
        );



        // Check if xinputDisabled extra is passed
        boolean xinputDisabledFromShortcut = false;




        initializeGyroSensor();
        ForensicLogger.logEvent(
                this,
                "info",
                "XSERVER_GYRO_REGISTRATION_DEFERRED",
                null,
                "xserver",
                "gyro_registration_deferred_until_ui_bootstrap",
                ForensicLogger.fields(
                        "gyro_enabled", preferences.getBoolean("gyro_enabled", true),
                        "gyro_sensor_present", gyroSensor != null
                )
        );



        // Record the start time
        startTime = System.currentTimeMillis();

        // Initialize handler for periodic saving
        handler = new Handler(Looper.getMainLooper());
        savePlaytimeRunnable = new Runnable() {
            @Override
            public void run() {
                savePlaytimeData();
                handler.postDelayed(this, SAVE_INTERVAL_MS);
            }
        };
        handler.postDelayed(savePlaytimeRunnable, SAVE_INTERVAL_MS);
        scheduleDebugPrefixPackFallback();


        // Handler and Runnable to manage timeout for hiding controls

        boolean isTimeoutEnabled = preferences.getBoolean("touchscreen_timeout_enabled", true);

        hideControlsRunnable = () -> {
            if (isTimeoutEnabled) {
                inputControlsView.setVisibility(View.GONE);
                Log.d("XServerDisplayActivity", "Touchscreen controls hidden after timeout.");
            }
        };


        contentsManager = new ContentsManager(this);
        imageFs = ImageFs.find(this);
        ForensicLogger.logEvent(
                this,
                "info",
                "XSERVER_EARLY_ROOTFS_BRIDGE_DEFERRED",
                null,
                "xserver",
                "rootfs_payload_prepare_deferred_to_bootstrap_executor",
                ForensicLogger.fields("imagefs_root", imageFs.getRootDir().getAbsolutePath())
        );
        prepareRootfsDevInputPath();

        String screenSize = Container.DEFAULT_SCREEN_SIZE;
        containerManager = new ContainerManager(this);
        container = containerManager.getContainerById(getIntent().getIntExtra("container_id", 0));

        // Log shortcut_path
        String shortcutPath = getIntent().getStringExtra("shortcut_path");
        Log.d("XServerDisplayActivity", "Shortcut Path: " + shortcutPath);


        // Determine container ID
        int containerId = getIntent().getIntExtra("container_id", 0);
        Log.d("XServerDisplayActivity", "Container ID from Intent: " + containerId);
        if (containerId == 0) {
            Log.d("XServerDisplayActivity", "Container ID is 0, attempting to parse from .desktop file");
            // Proceed with .desktop file parsing
        }


        // If container_id is 0, read from the .desktop file
        if (containerId == 0 && shortcutPath != null && !shortcutPath.isEmpty()) {
            File shortcutFile = new File(shortcutPath);
            containerId = parseContainerIdFromDesktopFile(shortcutFile);
            Log.d("XServerDisplayActivity", "Parsed Container ID from .desktop file: " + containerId);
        }

        // Initialize playtime tracking
        playtimePrefs = getSharedPreferences("playtime_stats", MODE_PRIVATE);
        shortcutName = getIntent().getStringExtra("shortcut_name");

        // Ensure shortcutPath is not null before proceeding
        if (shortcutPath != null && !shortcutPath.isEmpty()) {
            if (shortcutName == null || shortcutName.isEmpty()) {
                shortcutName = parseShortcutNameFromDesktopFile(new File(shortcutPath));
                Log.d("XServerDisplayActivity", "Parsed Shortcut Name from .desktop file: " + shortcutName);
            }
        } else {
            Log.d("XServerDisplayActivity", "No shortcut path provided, skipping shortcut parsing.");
        }

        // Increment play count at the start of a session
        incrementPlayCount();

        // Log the final container_id
        Log.d("XServerDisplayActivity", "Final Container ID: " + containerId);

        // Retrieve the container and check if it's null
        container = containerManager.getContainerById(containerId);

        if (container == null) {
            Log.e("XServerDisplayActivity", "Failed to retrieve container with ID: " + containerId);
            finish();  // Gracefully exit the activity to avoid crashing
            return;
        }

        rememberActiveLaunchTarget(
                containerId,
                shortcutPath,
                resolveEffectiveLaunchAppId(launchIntent, container),
                resolveIntentLaunchRouteToken(launchIntent),
                resolveIntentTemporaryOverrideAppId(launchIntent)
        );

        if (shortcutPath != null && !shortcutPath.isEmpty()) {
            shortcut = new Shortcut(container, new File(shortcutPath));
        }

        taskAffinityMask = ProcessHelper.getAffinityMask(container.getCPUList(true));
        taskAffinityMaskWoW64 = ProcessHelper.getAffinityMask(container.getCPUListWoW64(true));

        if (shortcut != null) {
            taskAffinityMask = ProcessHelper.getAffinityMask(shortcut.getExtra("cpuList", container.getCPUList(true)));
            taskAffinityMaskWoW64 = ProcessHelper.getAffinityMask(shortcut.getExtra("cpuListWoW64", container.getCPUListWoW64(true)));
        }

        // Determine the class name for the startup workarounds
        String wmClass = shortcut != null ? shortcut.getExtra("wmClass", "") : "";
        Log.d("XServerDisplayActivity", "Startup wmClass: " + wmClass);

        firstTimeBoot = container.getExtra("appVersion").isEmpty();

        String requestedWineVersion = resolveLaunchWineVersion();
        effectiveRuntimeModel = resolveLaunchRuntimeModel(requestedWineVersion);
        String wineVersion = resolveEffectiveLaunchWineVersion(requestedWineVersion, effectiveRuntimeModel);
        bindActiveContainerState(container, effectiveRuntimeModel, wineVersion);
        prepareRootfsDevInputPath();
        if (ensureLaunchRootfsReady(wineVersion, effectiveRuntimeModel)) {
            return;
        }

        contentsManager.syncContentsForLaunch();
        effectiveRuntimeModel = resolveLaunchRuntimeModel(requestedWineVersion);
        wineVersion = resolveEffectiveLaunchWineVersion(requestedWineVersion, effectiveRuntimeModel);
        if (ensureSelectedRuntimeReady(wineVersion, effectiveRuntimeModel)) {
            return;
        }

        xserverRootView = findViewById(R.id.XServerRoot);
        applyDesktopGestureExclusion(getWindow() != null ? getWindow().getDecorView() : null);
        applyDesktopGestureExclusion(findViewById(R.id.FLXServerDisplay));
        applyDesktopGestureExclusion(xserverRootView);
        armGuestBootstrapAfterFirstDraw(getWindow() != null ? getWindow().getDecorView() : null, "decor_predraw");
        armGuestBootstrapAfterFirstDraw(xserverRootView, "activity_root_predraw");

        selectedRuntimeProfile = resolveLaunchRuntimeCandidate(wineVersion, effectiveRuntimeModel);
        String canonicalWineVersion = selectedRuntimeProfile != null
                ? ContentsManager.getEntryName(selectedRuntimeProfile)
                : contentsManager.resolveBestRuntimeEntry(wineVersion, effectiveRuntimeModel);
        if (selectedRuntimeProfile == null && canonicalWineVersion != null && !canonicalWineVersion.trim().isEmpty()) {
            selectedRuntimeProfile = contentsManager.resolveBestRuntimeProfile(canonicalWineVersion, effectiveRuntimeModel);
        }
        if (selectedRuntimeProfile != null && !selectedRuntimeProfile.getRuntimeModel().isEmpty()) {
            effectiveRuntimeModel = selectedRuntimeProfile.getRuntimeModel();
            canonicalWineVersion = ContentsManager.getEntryName(selectedRuntimeProfile);
        }
        persistResolvedLaunchRuntime(requestedWineVersion, canonicalWineVersion, effectiveRuntimeModel);
        bindActiveContainerState(container, effectiveRuntimeModel, canonicalWineVersion);
        prepareRootfsDevInputPath();
        wineInfo = WineInfo.fromIdentifier(this, contentsManager, canonicalWineVersion, effectiveRuntimeModel);
        String wineInfoPathBeforeBind = wineInfo != null ? safeTrim(wineInfo.path) : "";
        String launchWinePath = resolveLaunchRuntimeWinePath(wineInfo, selectedRuntimeProfile);
        if (!launchWinePath.isEmpty()
                && wineInfo != null
                && !launchWinePath.equals(wineInfoPathBeforeBind)) {
            wineInfo = new WineInfo(
                    wineInfo.type,
                    wineInfo.version,
                    wineInfo.subversion,
                    wineInfo.getArch(),
                    launchWinePath
            );
        }
        imageFs.setWinePath(!launchWinePath.isEmpty() ? launchWinePath : wineInfoPathBeforeBind);
        logBootstrapCheckpoint(
                "XSERVER_RUNTIME_WINE_PATH_BOUND",
                "runtime_wine_path_bound_before_guest_bootstrap",
                "requested_entry", safeTrim(requestedWineVersion),
                "canonical_entry", safeTrim(canonicalWineVersion),
                "runtime_model", safeTrim(effectiveRuntimeModel),
                "runtime_profile", selectedRuntimeProfile != null ? ContentsManager.getEntryName(selectedRuntimeProfile) : "-",
                "imagefs_root", imageFs.getRootDir().getAbsolutePath(),
                "imagefs_root_name", imageFs.getRootDir().getName(),
                "imagefs_root_is_active_alias", ImageFs.ACTIVE_ROOT_DIR_NAME.equals(imageFs.getRootDir().getName()) ? "1" : "0",
                "active_root_alias", ImageFs.getActiveRootDir(this).getAbsolutePath(),
                "active_root_alias_target", FileUtils.isSymlink(ImageFs.getActiveRootDir(this)) ? FileUtils.readSymlink(ImageFs.getActiveRootDir(this)) : "",
                "wine_info_path_before", wineInfoPathBeforeBind,
                "bound_wine_path", imageFs.getWinePath(),
                "wine_info_path_rewritten", !safeTrim(wineInfo != null ? wineInfo.path : "").equals(wineInfoPathBeforeBind) ? "1" : "0",
                "runtime_core_payload_present", WineUtils.hasRuntimeCorePayload(new File(imageFs.getWinePath())) ? "1" : "0"
        );

        ForensicConfig.Snapshot requestedForensicSnapshot = ForensicConfig.load(this);
        ForensicConfig.Snapshot runtimeForensicSnapshot = resolveRuntimeForensicSnapshot(requestedForensicSnapshot);
        boolean enableLogs = runtimeForensicSnapshot.enableWineDebug
                || runtimeForensicSnapshot.enableBox64Logs
                || forensicModeLaunch;

        ProcessHelper.removeAllDebugCallbacks();
        if (enableLogs) {
            LogView.setFilename(getExecutable());
            ProcessHelper.addDebugCallback(debugDialog = new DebugDialog(this));
        }
        installForensicRuntimeLogCallbacks(runtimeForensicSnapshot);
        ForensicLogger.logEvent(
                this,
                "info",
                "XSERVER_BOOTSTRAP_POST_HOOKS",
                null,
                "xserver",
                "runtime_debug_hooks_installed",
                ForensicLogger.fields(
                        "enable_logs", enableLogs,
                        "forensic_mode", forensicModeLaunch,
                        "runtime_diagnostics_forced", shouldForceRuntimeDiagnosticsForLaunch(),
                        "requested_runtime_summary", ForensicConfig.buildRuntimeSummary(requestedForensicSnapshot),
                        "effective_runtime_summary", ForensicConfig.buildRuntimeSummary(runtimeForensicSnapshot),
                        "runtime_profile_present", selectedRuntimeProfile != null,
                        "wine_version", canonicalWineVersion,
                        "runtime_model", effectiveRuntimeModel
                )
        );

        graphicsDriver = container.getGraphicsDriver();
        String graphicsDriverConfig = container.getGraphicsDriverConfig();
        audioDriver = container.getAudioDriver();
        emulator = container.getEmulator();
        midiSoundFont = container.getMIDISoundFont();
        dxwrapper = container.getDXWrapper();
        String dxwrapperConfig = container.getDXWrapperConfig();
        screenSize = container.getScreenSize();
        winHandler.setInputType((byte) container.getInputType());
        lc_all = container.getLC_ALL();

        // Log only extra keys to avoid leaking user/runtime secrets in logs
        Bundle extras = launchIntent.getExtras();
        Log.d("XServerDisplayActivity", "Intent Extra Keys: " + (extras == null ? "[]" : extras.keySet()));

        if (shortcut != null) {
            graphicsDriver = shortcut.getExtra("graphicsDriver", container.getGraphicsDriver());
            graphicsDriverConfig = shortcut.getExtra("graphicsDriverConfig", container.getGraphicsDriverConfig());
            audioDriver = shortcut.getExtra("audioDriver", container.getAudioDriver());
            emulator = shortcut.getExtra("emulator", container.getEmulator());
            dxwrapper = shortcut.getExtra("dxwrapper", container.getDXWrapper());
            dxwrapperConfig = shortcut.getExtra("dxwrapperConfig", container.getDXWrapperConfig());
            screenSize = shortcut.getExtra("screenSize", container.getScreenSize());
            lc_all = shortcut.getExtra("lc_all", container.getLC_ALL());
            String inputType = shortcut.getExtra("inputType");
            if (!inputType.isEmpty()) winHandler.setInputType(Byte.parseByte(inputType));
            xinputDisabledFromShortcut = shortcut.getExtraBoolean("disableXinput", false);
            winHandler.setXInputDisabled(xinputDisabledFromShortcut);
            Log.d("XServerDisplayActivity", "XInput Disabled from Shortcut: " + xinputDisabledFromShortcut);
        }
        parseUpscalerLaunchSettings(shortcut);

        legacyGraphicsRequestedDriver = Container.resolveLegacyGraphicsRequestedDriver(graphicsDriver, graphicsDriverConfig);
        legacyGraphicsProviderHint = Container.resolveLegacyGraphicsProviderHint(graphicsDriver, graphicsDriverConfig);
        legacyGraphicsPolicy = Container.resolveLegacyGraphicsPolicy(graphicsDriver, graphicsDriverConfig);
        graphicsDriver = Container.normalizeGraphicsDriver(graphicsDriver);
        rawGraphicsDriverConfig = GraphicsDrivers.sanitizeConfigShape(graphicsDriver, graphicsDriverConfig);
        this.graphicsDriverConfig = GraphicsDrivers.parseConfig(graphicsDriver, rawGraphicsDriverConfig);
        this.dxwrapperConfig = DXVKConfigDialog.parseConfig(dxwrapperConfig);
        ForensicLogger.logEvent(
                this,
                "info",
                "XSERVER_BOOTSTRAP_CONFIG_PARSED",
                null,
                "xserver",
                "runtime_launch_configuration_parsed",
                ForensicLogger.fields(
                        "graphics_driver", graphicsDriver,
                        "graphics_driver_legacy_requested", legacyGraphicsRequestedDriver,
                        "graphics_driver_legacy_hint", legacyGraphicsProviderHint,
                        "graphics_driver_legacy_policy", legacyGraphicsPolicy,
                        "audio_driver", audioDriver,
                        "emulator", emulator,
                        "dxwrapper", dxwrapper,
                        "screen_size", screenSize,
                        "lc_all", lc_all,
                        "xinput_disabled", xinputDisabledFromShortcut
                )
        );
        setupRuntimeDrawer();
        ForensicLogger.logEvent(
                this,
                "info",
                "XSERVER_BOOTSTRAP_DRAWER_READY",
                null,
                "xserver",
                "runtime_drawer_ready",
                null
        );

        if (!wineInfo.isWin64()) {
            onExtractFileListener = (file, size) -> {
                String path = file.getPath();
                if (path.contains("system32/")) return null;
                return new File(path.replace("syswow64/", "system32/"));
            };
        }

        ForensicLogger.logEvent(
                this,
                "info",
                "XSERVER_BOOTSTRAP_PRELOADER_SHOW",
                null,
                "xserver",
                "preloader_show_requested",
                ForensicLogger.fields("screen_size", screenSize)
        );
        preloaderDialog.show(R.string.starting_up);

        try {
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "XSERVER_BOOTSTRAP_INPUT_MANAGER_BEGIN",
                    null,
                    "xserver",
                    "input_controls_manager_init_begin",
                    null
            );
            inputControlsManager = new InputControlsManager(this);
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "XSERVER_BOOTSTRAP_INPUT_MANAGER_READY",
                    null,
                    "xserver",
                    "input_controls_manager_ready",
                    null
            );

            WinlatorNative.ensureLoaded(this, "xserver_bootstrap_preload");

            ForensicLogger.logEvent(
                    this,
                    "info",
                    "XSERVER_BOOTSTRAP_XSERVER_BEGIN",
                    null,
                    "xserver",
                    "xserver_construction_begin",
                    ForensicLogger.fields("screen_size", screenSize)
            );
            xServer = new XServer(new ScreenInfo(screenSize));
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "XSERVER_BOOTSTRAP_XSERVER_READY",
                    null,
                    "xserver",
                    "xserver_construction_ready",
                    ForensicLogger.fields(
                            "screen_width", xServer.screenInfo.width,
                            "screen_height", xServer.screenInfo.height
                    )
            );
            xServer.setWinHandler(winHandler);
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "XSERVER_BOOTSTRAP_WINHANDLER_BOUND",
                    null,
                    "xserver",
                    "xserver_winhandler_bound",
                    null
            );
        } catch (Throwable error) {
            ForensicLogger.error(
                    this,
                    "XSERVER_BOOTSTRAP_NATIVE_OR_JAVA_FAILURE",
                    null,
                    "xserver",
                    "bootstrap_failed_before_orientation_gate",
                    error,
                    ForensicLogger.fields(
                            "screen_size", screenSize,
                            "wine_version", canonicalWineVersion,
                            "runtime_model", effectiveRuntimeModel
                    )
            );
            throw error;
        }

        // Add the OnWindowModificationListener for dynamic workarounds
        xServer.windowManager.addOnWindowModificationListener(new WindowManager.OnWindowModificationListener() {
            @Override
            public void onUpdateWindowContent(Window window) {
                if (!guestVisualReady && isTrackedVisualWindow(window)) {
                    logBootstrapWindowCandidate(
                            "XSERVER_WINDOW_CONTENT_CANDIDATE",
                            "info",
                            "desktop_shell_window_content_candidate",
                            window
                    );
                    markGuestVisualReady("window_content", window, null);
                }

                if (frameRatingWindowId == window.id) frameRating.update();
            }

            @Override
            public void onMapWindow(Window window) {
                if (desktopShellBootstrapActive && !guestVisualReady) {
                    logBootstrapWindowCandidate(
                            "XSERVER_WINDOW_MAPPED",
                            "info",
                            "desktop_shell_window_mapped",
                            window
                    );
                }
                if (!guestVisualReady && isTrackedVisualWindow(window)) {
                    ForensicLogger.logEvent(
                            XServerDisplayActivity.this,
                            "warn",
                            "PRELOADER_MAP_FALLBACK",
                            null,
                            "xserver",
                            "preloader_closed_on_window_map",
                            ForensicLogger.fields(
                                    "class_name", window.getClassName(),
                                    "window_id", window.id
                            )
                    );
                    markGuestVisualReady("window_map", window, null);
                }
                // Log the class name of the mapped window
                Log.d("XServerDisplayActivity", "onMapWindow: Detected window className: " + window.getClassName());
                assignTaskAffinity(window);
                noteApplicationWindowMapped(window);
            }

            @Override
            public void onModifyWindowProperty(Window window, Property property) {
                changeFrameRatingVisibility(window, property);
            }

            @Override
            public void onUnmapWindow(Window window) {
                changeFrameRatingVisibility(window, null);
                noteApplicationWindowUnmapped(window);
            }
        });

        if (!midiSoundFont.equals("")) {
            InputStream in = null;
            InputStream finalIn = in;
            MidiManager.OnMidiLoadedCallback callback = new MidiManager.OnMidiLoadedCallback() {
                @Override
                public void onSuccess(SF2Soundbank soundbank) {
                    midiHandler = new MidiHandler();
                    midiHandler.setSoundBank(soundbank);
                    midiHandler.start();
                }

                @Override
                public void onFailed(Exception e) {
                    try {
                        finalIn.close();
                    } catch (Exception e2) {
                        Log.w("XServerDisplayActivity", "Failed to close MIDI soundfont stream", e2);
                    }
                }
            };
            try {
                if (midiSoundFont.equals(MidiManager.DEFAULT_SF2_FILE)) {
                    in = getAssets().open(MidiManager.SF2_ASSETS_DIR + "/" + midiSoundFont);
                    MidiManager.load(in, callback);
                } else
                    MidiManager.load(new File(MidiManager.getSoundFontDir(this), midiSoundFont), callback);
            } catch (Exception e) {
                Log.e("XServerDisplayActivity", "Failed to load MIDI soundfont " + midiSoundFont, e);
            }
        }

        // Check if a profile is defined by the shortcut
        String controlsProfile = shortcut != null ? shortcut.getExtra("controlsProfile", "") : "";

        createNotifcationChannel();

        Intent notificationIntent = new Intent(this, XServerDisplayActivity.class);
        notificationIntent.putExtra("container_id", container.id);
        if (shortcut != null) {
            notificationIntent.putExtra("shortcut_path", shortcut.file.getPath());
            notificationIntent.putExtra("shortcut_name", shortcut.name);
        }
        if (!activeLaunchAppId.isEmpty()) {
            notificationIntent.putExtra(LaunchSecurity.EXTRA_APP_ID, activeLaunchAppId);
        }
        if (!activeLaunchRouteToken.isEmpty()) {
            notificationIntent.putExtra(LaunchSecurity.EXTRA_LAUNCH_ROUTE_TOKEN, activeLaunchRouteToken);
        }
        if (!activeTemporaryOverrideAppId.isEmpty()) {
            notificationIntent.putExtra(LaunchSecurity.EXTRA_TEMP_OVERRIDE_APP_ID, activeTemporaryOverrideAppId);
        }
        notificationIntent.putExtra("disableXinput", xinputDisabledFromShortcut ? "1" : "0");
        LaunchSecurity.signXServerLaunchIntent(this, notificationIntent);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_settings)
                .setContentTitle(getString(R.string.notification_runtime_title))
                .setContentText(getString(R.string.notification_runtime_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false);

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build());

        Runnable runnable = () -> {
            logBootstrapCheckpoint(
                    "XSERVER_BOOTSTRAP_RUNNABLE_BEGIN",
                    "bootstrap_runnable_entered",
                    "current_orientation", getResources().getConfiguration().orientation,
                    "activity_has_focus", hasWindowFocus(),
                    "activity_finishing", isFinishing(),
                    "activity_destroyed", isDestroyed()
            );
            try {
                setupUI();
                registerGyroListenerIfEnabled("bootstrap_ui_ready");
                logBootstrapCheckpoint(
                        "XSERVER_BOOTSTRAP_UI_READY",
                        "bootstrap_ui_ready",
                        "activity_has_focus", hasWindowFocus(),
                        "desktop_shell_bootstrap", desktopShellBootstrapActive
                );
                if (controlsProfile.isEmpty()) {
                    // No profile defined, run the simulated dialog confirmation for input controls
                    simulateConfirmInputControlsDialog();
                    logBootstrapCheckpoint(
                            "XSERVER_BOOTSTRAP_CONTROLS_DIALOG_SIMULATED",
                            "bootstrap_controls_dialog_simulated"
                    );
                }
                logBootstrapCheckpoint(
                        "XSERVER_BOOTSTRAP_ASYNC_SUBMIT",
                        "bootstrap_async_submitted",
                        "activity_has_focus", hasWindowFocus(),
                        "activity_finishing", isFinishing(),
                        "activity_destroyed", isDestroyed()
                );
                runtimeBootstrapExecutor.execute(() -> {
                    logBootstrapCheckpoint(
                            "XSERVER_BOOTSTRAP_ASYNC_BEGIN",
                            "bootstrap_async_entered",
                            "activity_has_focus", hasWindowFocus(),
                            "activity_finishing", isFinishing(),
                            "activity_destroyed", isDestroyed()
                    );
                    try {
                        long stageStartedAt = System.currentTimeMillis();
                        logBootstrapCheckpoint("XSERVER_BOOTSTRAP_SYSTEM_FILES_BEGIN", "bootstrap_system_files_begin");
                        setupWineSystemFiles();
                        logBootstrapCheckpoint(
                                "XSERVER_BOOTSTRAP_SYSTEM_FILES_READY",
                                "bootstrap_system_files_ready",
                                "duration_ms", System.currentTimeMillis() - stageStartedAt
                        );

                        stageStartedAt = System.currentTimeMillis();
                        logBootstrapCheckpoint("XSERVER_BOOTSTRAP_GRAPHICS_FILES_BEGIN", "bootstrap_graphics_files_begin");
                        extractGraphicsDriverFiles();
                        logBootstrapCheckpoint(
                                "XSERVER_BOOTSTRAP_GRAPHICS_FILES_READY",
                                "bootstrap_graphics_files_ready",
                                "duration_ms", System.currentTimeMillis() - stageStartedAt
                        );

                        stageStartedAt = System.currentTimeMillis();
                        logBootstrapCheckpoint("XSERVER_BOOTSTRAP_AUDIO_DRIVER_BEGIN", "bootstrap_audio_driver_begin");
                        changeWineAudioDriver();
                        logBootstrapCheckpoint(
                                "XSERVER_BOOTSTRAP_AUDIO_DRIVER_READY",
                                "bootstrap_audio_driver_ready",
                                "duration_ms", System.currentTimeMillis() - stageStartedAt
                        );

                        stageStartedAt = System.currentTimeMillis();
                        logBootstrapCheckpoint("XSERVER_BOOTSTRAP_ENV_BEGIN", "bootstrap_environment_begin");
                        setupXEnvironment();
                        logBootstrapCheckpoint(
                                "XSERVER_BOOTSTRAP_ENV_READY",
                                "bootstrap_environment_ready",
                                "duration_ms", System.currentTimeMillis() - stageStartedAt,
                                "desktop_shell_bootstrap", desktopShellBootstrapActive
                        );
                    } catch (Throwable error) {
                        throw rethrowBootstrapFailure(
                                "XSERVER_BOOTSTRAP_ASYNC_FAILED",
                                "bootstrap_async_failed_before_guest_submit",
                                error,
                                "activity_has_focus", hasWindowFocus(),
                                "activity_finishing", isFinishing(),
                                "activity_destroyed", isDestroyed(),
                                "desktop_shell_bootstrap", desktopShellBootstrapActive
                        );
                    }
                });
            } catch (Throwable error) {
                throw rethrowBootstrapFailure(
                        "XSERVER_BOOTSTRAP_RUNNABLE_FAILED",
                        "bootstrap_runnable_failed_before_async_submit",
                        error,
                        "activity_has_focus", hasWindowFocus(),
                        "activity_finishing", isFinishing(),
                        "activity_destroyed", isDestroyed()
                );
            }
        };

        boolean landscapeReady = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        boolean portraitScreenInfo = xServer.screenInfo.height > xServer.screenInfo.width;
        prearmDesktopShellBootstrapIfNeeded("orientation_gate");
        ForensicLogger.logEvent(
                this,
                "info",
                "XSERVER_BOOTSTRAP_ORIENTATION_GATE",
                null,
                "xserver",
                "bootstrap_orientation_gate_evaluated",
                ForensicLogger.fields(
                        "screen_width", xServer.screenInfo.width,
                        "screen_height", xServer.screenInfo.height,
                        "portrait_screen_info", portraitScreenInfo,
                        "current_orientation", getResources().getConfiguration().orientation,
                        "landscape_ready", landscapeReady
                )
        );

        if (portraitScreenInfo && !landscapeReady) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            logBootstrapCheckpoint(
                    "XSERVER_BOOTSTRAP_CONFIG_CALLBACK_ARMED",
                    "bootstrap_configuration_callback_armed",
                    "requested_orientation", ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            );
            configChangedCallback = () -> armGuestBootstrapAfterFocus(runnable, "orientation_config_ready");
        } else {
            armGuestBootstrapAfterFocus(runnable, "create_ready");
        }
    }

    // Method to parse container_id from .desktop file
    private int parseContainerIdFromDesktopFile(File desktopFile) {
        int containerId = 0;
        if (desktopFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(desktopFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("container_id:")) {
                        containerId = Integer.parseInt(line.split(":")[1].trim());
                        break;
                    }
                }
            } catch (IOException | NumberFormatException e) {
                Log.e("XServerDisplayActivity", "Error parsing container_id from .desktop file", e);
            }
        }
        return containerId;
    }

    private String resolveLaunchWineVersion() {
        String wineVersion = shortcut != null
                ? shortcut.getExtra("wineVersion", container.getWineVersion())
                : container.getWineVersion();
        return wineVersion == null ? "" : wineVersion.trim();
    }

    private String resolveLaunchRuntimeModel(String wineVersion) {
        String normalizedWineVersion = wineVersion == null ? "" : wineVersion.trim();
        String inferredFromEntry = ContentProfile.inferRuntimeModelFromEntryName(normalizedWineVersion);
        String requestedRuntimeModel = !inferredFromEntry.isEmpty()
                ? inferredFromEntry
                : ContentProfile.normalizeRuntimeModel(container.getContainerVariant());
        ContentProfile profile = contentsManager.resolveBestRuntimeProfile(normalizedWineVersion, requestedRuntimeModel);
        if (profile != null && !profile.getRuntimeModel().isEmpty()) {
            return profile.getRuntimeModel();
        }

        if (!inferredFromEntry.isEmpty()) {
            return inferredFromEntry;
        }

        String containerVariant = ContentProfile.normalizeRuntimeModel(container.getContainerVariant());
        return containerVariant.isEmpty() ? Container.DEFAULT_VARIANT : containerVariant;
    }

    private String resolveEffectiveLaunchWineVersion(String wineVersion, String runtimeModel) {
        String normalizedWineVersion = wineVersion == null ? "" : wineVersion.trim();
        if (!ContentProfile.RUNTIME_MODEL_GLIBC.equals(ContentProfile.normalizeRuntimeModel(runtimeModel))) {
            return normalizedWineVersion;
        }

        ContentProfile promotedProfile = resolvePromotedGlibcLaunchRuntimeProfile(normalizedWineVersion, runtimeModel);
        if (promotedProfile == null) {
            return normalizedWineVersion;
        }

        String promotedEntry = ContentsManager.getEntryName(promotedProfile);
        ForensicLogger.logEvent(
                this,
                "info",
                WineInfo.isMainWineVersion(normalizedWineVersion)
                        ? "XSERVER_MAIN_WINE_PROMOTED"
                        : "XSERVER_GLIBC_RUNTIME_PROMOTED",
                null,
                "xserver",
                WineInfo.isMainWineVersion(normalizedWineVersion)
                        ? "main_wine_promoted_to_contents_runtime"
                        : "glibc_runtime_promoted_before_rootfs_binding",
                ForensicLogger.fields(
                        "requested_entry", normalizedWineVersion,
                        "promoted_entry", promotedEntry,
                        "runtime_model", runtimeModel,
                        "promotion_reason", resolveGlibcRuntimePromotionReason(normalizedWineVersion, runtimeModel, promotedProfile),
                        "installed_present", contentsManager.isInstalledProfilePresent(promotedProfile),
                        "installed_usable", contentsManager.isInstalledProfileUsable(promotedProfile),
                        "remote_downloadable", promotedProfile.isRemoteDownloadable(),
                        "source_label", promotedProfile.sourceLabel,
                        "source_repo", promotedProfile.sourceRepo,
                        "release_tag", promotedProfile.releaseTag,
                        "architecture", promotedProfile.getArchitectureTag()
                )
        );
        return promotedEntry;
    }

    @Nullable
    private ContentProfile resolvePromotedGlibcLaunchRuntimeProfile(String wineVersion, String runtimeModel) {
        if (!ContentProfile.RUNTIME_MODEL_GLIBC.equals(ContentProfile.normalizeRuntimeModel(runtimeModel))) {
            return null;
        }

        String normalizedWineVersion = wineVersion == null ? "" : wineVersion.trim();
        ContentProfile current = null;
        if (!WineInfo.isMainWineVersion(normalizedWineVersion)) {
            String canonicalEntry = contentsManager.resolveBestRuntimeEntry(normalizedWineVersion, runtimeModel);
            current = contentsManager.resolveBestRuntimeProfile(canonicalEntry, runtimeModel);
            if (current == null) current = contentsManager.getProfileByEntryName(canonicalEntry);
            if (current == null) current = contentsManager.getProfileByEntryName(normalizedWineVersion);
        }

        String requestedArch = resolveRuntimeArchHintFromEntry(normalizedWineVersion);
        ContentProfile preferred = WineInfo.isMainWineVersion(normalizedWineVersion)
                ? findPreferredMainWineRuntimeProfile(runtimeModel)
                : findPreferredLaunchRuntimeProfile(null, runtimeModel, requestedArch);
        if (preferred == null) {
            return null;
        }

        boolean currentUsable = current != null && contentsManager.isInstalledProfileUsable(current);
        boolean currentPresent = current != null && contentsManager.isInstalledProfilePresent(current);
        boolean preferredUsable = contentsManager.isInstalledProfileUsable(preferred);
        boolean preferredPresent = contentsManager.isInstalledProfilePresent(preferred);
        return RuntimeLaunchPolicy.shouldPromoteGlibcRuntime(
                current,
                currentUsable,
                currentPresent,
                preferred,
                preferredUsable,
                preferredPresent,
                normalizedWineVersion,
                runtimeModel
        ) ? preferred : null;
    }

    private String resolveGlibcRuntimePromotionReason(String wineVersion,
                                                     String runtimeModel,
                                                     @NonNull ContentProfile promotedProfile) {
        ContentProfile current = null;
        String normalizedWineVersion = wineVersion == null ? "" : wineVersion.trim();
        if (!WineInfo.isMainWineVersion(normalizedWineVersion)) {
            String canonicalEntry = contentsManager.resolveBestRuntimeEntry(normalizedWineVersion, runtimeModel);
            current = contentsManager.resolveBestRuntimeProfile(canonicalEntry, runtimeModel);
            if (current == null) current = contentsManager.getProfileByEntryName(canonicalEntry);
            if (current == null) current = contentsManager.getProfileByEntryName(normalizedWineVersion);
        }
        boolean currentUsable = current != null && contentsManager.isInstalledProfileUsable(current);
        boolean currentPresent = current != null && contentsManager.isInstalledProfilePresent(current);
        return RuntimeLaunchPolicy.resolvePromotionReason(
                current,
                currentUsable,
                currentPresent,
                promotedProfile,
                contentsManager.isInstalledProfileUsable(promotedProfile),
                contentsManager.isInstalledProfilePresent(promotedProfile),
                normalizedWineVersion,
                runtimeModel
        );
    }

    private void persistResolvedLaunchRuntime(String requestedWineVersion, String resolvedWineVersion, String runtimeModel) {
        if (container == null) return;

        String normalizedRequested = requestedWineVersion == null ? "" : requestedWineVersion.trim();
        String normalizedResolved = resolvedWineVersion == null ? "" : resolvedWineVersion.trim();
        String normalizedRuntimeModel = ContentProfile.normalizeRuntimeModel(runtimeModel);
        if (!shouldPersistResolvedLaunchRuntime(normalizedRequested, normalizedResolved, normalizedRuntimeModel)) {
            ForensicLogger.logEvent(
                    this,
                    "warn",
                    "XSERVER_RUNTIME_CANONICALIZE_PRESERVED_REQUESTED",
                    null,
                    "xserver",
                    "explicit_runtime_selection_not_overwritten_by_fallback",
                    ForensicLogger.fields(
                            "requested_entry", normalizedRequested,
                            "resolved_entry", normalizedResolved,
                            "container_variant", container.getContainerVariant(),
                            "runtime_model", normalizedRuntimeModel
                    )
            );
            return;
        }
        boolean changed = false;

        if (!normalizedResolved.isEmpty() && !normalizedResolved.equalsIgnoreCase(container.getWineVersion())) {
            container.setWineVersion(normalizedResolved);
            changed = true;
        }

        if (!normalizedRuntimeModel.isEmpty() && !normalizedRuntimeModel.equalsIgnoreCase(container.getContainerVariant())) {
            container.setContainerVariant(normalizedRuntimeModel);
            changed = true;
        }

        if (!changed) return;

        container.saveData();
        ForensicLogger.logEvent(
                this,
                "info",
                "XSERVER_RUNTIME_CANONICALIZED",
                null,
                "xserver",
                "runtime_selection_persisted_for_launch",
                ForensicLogger.fields(
                        "requested_entry", normalizedRequested,
                        "resolved_entry", normalizedResolved,
                        "container_variant", container.getContainerVariant(),
                        "runtime_model", normalizedRuntimeModel
                )
        );
    }

    private boolean shouldPersistResolvedLaunchRuntime(String requestedWineVersion, String resolvedWineVersion, String runtimeModel) {
        String normalizedRequested = requestedWineVersion == null ? "" : requestedWineVersion.trim();
        String normalizedResolved = resolvedWineVersion == null ? "" : resolvedWineVersion.trim();
        if (normalizedResolved.isEmpty()) return false;
        if (normalizedRequested.isEmpty()) return true;
        if (normalizedRequested.equalsIgnoreCase(normalizedResolved)) return true;
        return WineInfo.isMainWineVersion(normalizedRequested)
                || RuntimeLaunchPolicy.shouldPersistPromotedGlibcRuntime(normalizedRequested, normalizedResolved, runtimeModel);
    }

    @Nullable
    private ContentProfile findPreferredMainWineRuntimeProfile(String runtimeModel) {
        ContentProfile proton = findPreferredLaunchRuntimeProfile(ContentProfile.ContentType.CONTENT_TYPE_PROTON, runtimeModel);
        if (proton != null) return proton;
        return findPreferredLaunchRuntimeProfile(ContentProfile.ContentType.CONTENT_TYPE_WINE, runtimeModel);
    }

    @Nullable
    private ContentProfile findPreferredLaunchRuntimeProfile(@Nullable ContentProfile.ContentType preferredType, String runtimeModel) {
        return findPreferredLaunchRuntimeProfile(preferredType, runtimeModel, "");
    }

    @Nullable
    private ContentProfile findPreferredLaunchRuntimeProfile(@Nullable ContentProfile.ContentType preferredType,
                                                            String runtimeModel,
                                                            String requestedArch) {
        ContentProfile best = null;
        String normalizedRequestedArch = normalizeRuntimeArchHint(requestedArch);
        for (ContentProfile.ContentType type : new ContentProfile.ContentType[] {
                ContentProfile.ContentType.CONTENT_TYPE_PROTON,
                ContentProfile.ContentType.CONTENT_TYPE_WINE
        }) {
            if (preferredType != null && type != preferredType) continue;
            List<ContentProfile> profiles = contentsManager.getProfiles(type);
            if (profiles == null) continue;
            for (ContentProfile profile : profiles) {
                if (profile == null || !profile.isWineProtonFamily()) continue;
                String normalizedRuntimeModel = ContentProfile.normalizeRuntimeModel(runtimeModel);
                boolean runtimeModelMatches = profile.isRuntimeModelCompatible(runtimeModel)
                        && (normalizedRuntimeModel.isEmpty() || normalizedRuntimeModel.equals(profile.getRuntimeModel()));
                if (!runtimeModelMatches) {
                    continue;
                }
                if (!normalizedRequestedArch.isEmpty()
                        && !profile.matchesArchitectureFilter(normalizedRequestedArch)) {
                    continue;
                }
                best = pickPreferredLaunchRuntime(best, profile);
            }
        }
        return best;
    }

    private ContentProfile pickPreferredLaunchRuntime(@Nullable ContentProfile currentBest, @NonNull ContentProfile candidate) {
        if (currentBest == null) return candidate;

        int currentScore = computePreferredLaunchRuntimeScore(currentBest);
        int candidateScore = computePreferredLaunchRuntimeScore(candidate);
        if (candidateScore != currentScore) {
            return candidateScore > currentScore ? candidate : currentBest;
        }

        int publishedCompare = comparePublishedAt(candidate.publishedAt, currentBest.publishedAt);
        if (publishedCompare != 0) {
            return publishedCompare > 0 ? candidate : currentBest;
        }
        if (candidate.verCode != currentBest.verCode) {
            return candidate.verCode > currentBest.verCode ? candidate : currentBest;
        }
        return currentBest;
    }

    private int computePreferredLaunchRuntimeScore(@NonNull ContentProfile profile) {
        return RuntimeLaunchPolicy.computePreferredLaunchRuntimeScore(
                profile,
                contentsManager.isInstalledProfileUsable(profile),
                contentsManager.isInstalledProfilePresent(profile)
        );
    }

    private int comparePublishedAt(String left, String right) {
        String normalizedLeft = left == null ? "" : left.trim();
        String normalizedRight = right == null ? "" : right.trim();
        if (normalizedLeft.isEmpty() && normalizedRight.isEmpty()) return 0;
        if (normalizedLeft.isEmpty()) return -1;
        if (normalizedRight.isEmpty()) return 1;
        return normalizedLeft.compareToIgnoreCase(normalizedRight);
    }

    private boolean ensureLaunchRootfsReady(String wineVersion, String runtimeModel) {
        if (!ImageFsInstaller.isInstallRequired(this, container, runtimeModel, wineVersion)) {
            return false;
        }

        final int launchGeneration = launchBindingGeneration;
        final Intent restartIntent = new Intent(getIntent());

        ForensicLogger.logEvent(
                this,
                "warn",
                "XSERVER_ROOTFS_REINSTALL_REQUIRED",
                forensicTraceIdOrNull(),
                "xserver",
                "rootfs_reinstall_required_before_launch",
                ForensicLogger.fields(
                        "wine_version", wineVersion,
                        "runtime_model", runtimeModel,
                        "container_variant", container != null ? container.getContainerVariant() : "",
                        "current_rootfs_variant", imageFs != null ? imageFs.getVariant() : "",
                        "current_rootfs_provider", imageFs != null ? imageFs.getRootfsProvider() : "",
                        "current_rootfs_layout", imageFs != null ? imageFs.getRootfsLayout() : ""
                )
        );

        preloaderDialog.show(R.string.installing_system_files);
        logBootstrapCheckpoint(
                "XSERVER_ROOTFS_REINSTALL_BEGIN",
                "rootfs_reinstall_started_before_guest_bootstrap",
                "wine_version", wineVersion,
                "runtime_model", runtimeModel
        );
        Executors.newSingleThreadExecutor().execute(() -> {
            boolean success = false;
            try {
                success = ImageFsInstaller.installIfNeededFuture(
                        this,
                        getAssets(),
                        container,
                        runtimeModel,
                        wineVersion,
                        progress -> runOnUiThread(() -> preloaderDialog.setProgress(progress))
                ).get();
            } catch (Exception e) {
                Log.e("XServerDisplayActivity", "Unable to prepare rootfs for launch", e);
            }

            final boolean installSucceeded = success;
            runOnUiThread(() -> {
                if (!isLaunchBindingCurrent(launchGeneration)) {
                    preloaderDialog.closeOnUiThread();
                    return;
                }
                preloaderDialog.closeOnUiThread();
                if (!installSucceeded) {
                    logBootstrapCheckpoint(
                            "XSERVER_ROOTFS_REINSTALL_FAILED",
                            "rootfs_reinstall_failed_before_guest_bootstrap",
                            "wine_version", wineVersion,
                            "runtime_model", runtimeModel
                    );
                    AppUtils.showToast(this, R.string.unable_to_install_system_files);
                    finish();
                    return;
                }

                logBootstrapCheckpoint(
                        "XSERVER_ROOTFS_REINSTALL_READY",
                        "rootfs_reinstall_completed_before_guest_bootstrap",
                        "wine_version", wineVersion,
                        "runtime_model", runtimeModel
                );
                logBootstrapCheckpoint(
                        "XSERVER_ROOTFS_RELAUNCH_REQUESTED",
                        "rootfs_relaunch_requested_after_reinstall",
                        "wine_version", wineVersion,
                        "runtime_model", runtimeModel
                );
                recreateForLaunchRelaunch(restartIntent);
            });
        });
        return true;
    }

    private boolean ensureSelectedRuntimeReady(String wineVersion, String runtimeModel) {
        ContentProfile launchProfile = resolveLaunchRuntimeCandidate(wineVersion, runtimeModel);
        logRuntimeReadinessCheckpoint(
                "XSERVER_RUNTIME_READY_CHECK",
                "runtime_ready_check_before_guest_bootstrap",
                wineVersion,
                runtimeModel,
                launchProfile
        );
        final int launchGeneration = launchBindingGeneration;
        final Intent restartIntent = new Intent(getIntent());
        if (launchProfile == null) {
            boolean remoteBootstrapAllowed = shouldAttemptRemoteRuntimeBootstrap(wineVersion, runtimeModel);
            logBootstrapCheckpoint(
                    "XSERVER_RUNTIME_PROFILE_ABSENT",
                    "runtime_profile_absent_before_guest_bootstrap",
                    "requested_entry", safeTrim(wineVersion),
                    "runtime_model", safeTrim(runtimeModel),
                    "remote_bootstrap_allowed", remoteBootstrapAllowed ? "1" : "0"
            );
            if (!remoteBootstrapAllowed) {
                return false;
            }
            preloaderDialog.show(R.string.installing_content);
            runtimeBootstrapExecutor.execute(() -> {
                logBootstrapCheckpoint(
                        "XSERVER_RUNTIME_HYDRATE_BEGIN",
                        "runtime_profile_hydration_begin",
                        "requested_entry", safeTrim(wineVersion),
                        "runtime_model", safeTrim(runtimeModel)
                );
                LaunchHydrationResult hydrationResult = hydrateLaunchRuntimeProfilesForLaunch(
                        wineVersion,
                        runtimeModel,
                        LAUNCH_RUNTIME_HYDRATION_TIMEOUT_MS
                );
                logLaunchHydrationResult(
                        "XSERVER_RUNTIME_HYDRATE_SOURCE_RESULT",
                        "runtime_profile_hydration_source_result",
                        wineVersion,
                        runtimeModel,
                        hydrationResult
                );
                ContentProfile hydratedProfile = resolveLaunchRuntimeCandidate(wineVersion, runtimeModel);
                boolean hydratedReady = isRuntimeProfileReady(hydratedProfile);
                boolean installedFromRemote = false;
                if (!hydratedReady
                        && hydratedProfile != null
                        && hydratedProfile.isRemoteDownloadable()
                        && canAttemptRemoteRuntimeInstall(hydratedProfile)) {
                    logBootstrapCheckpoint(
                            "XSERVER_RUNTIME_REMOTE_INSTALL_BEGIN",
                            "runtime_remote_install_begin_after_hydration",
                            "requested_entry", safeTrim(wineVersion),
                            "runtime_model", safeTrim(runtimeModel),
                            "profile_entry", ContentsManager.getEntryName(hydratedProfile)
                    );
                    installedFromRemote = installRemoteRuntimeProfile(hydratedProfile);
                    hydratedReady = installedFromRemote && isRuntimeProfileReady(hydratedProfile);
                } else if (!hydratedReady && hydratedProfile != null && hydratedProfile.isRemoteDownloadable()) {
                    logRemoteRuntimeInstallBlockedOffline(hydratedProfile, wineVersion, runtimeModel);
                }
                boolean ready = hydratedProfile != null && hydratedReady;
                logRuntimeReadinessCheckpoint(
                        "XSERVER_RUNTIME_HYDRATE_RESULT",
                        "runtime_profile_hydration_result",
                        wineVersion,
                        runtimeModel,
                        hydratedProfile
                );
                logBootstrapCheckpoint(
                        "XSERVER_RUNTIME_GATE_RESULT",
                        "runtime_gate_result_after_hydration",
                        "requested_entry", safeTrim(wineVersion),
                        "runtime_model", safeTrim(runtimeModel),
                        "ready", ready ? "1" : "0",
                        "installed_from_remote", installedFromRemote ? "1" : "0"
                );
                runOnUiThread(() -> {
                    if (!isLaunchBindingCurrent(launchGeneration)) {
                        logBootstrapCheckpoint(
                                "XSERVER_RUNTIME_GATE_STALE",
                                "runtime_gate_result_ignored_for_stale_launch_binding",
                                "requested_entry", safeTrim(wineVersion),
                                "runtime_model", safeTrim(runtimeModel)
                        );
                        preloaderDialog.closeOnUiThread();
                        return;
                    }
                    preloaderDialog.closeOnUiThread();
                    if (!ready) {
                        AppUtils.showToast(this, R.string.unable_to_install_content);
                        finish();
                        return;
                    }

                    restartIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    recreateForLaunchRelaunch(restartIntent);
                });
            });
            return true;
        }
        if (isRuntimeProfileReady(launchProfile)
                && shouldProbeGlibcRuntimePromotion(wineVersion, runtimeModel, launchProfile)) {
            return hydrateAndMaybePromoteReadyGlibcRuntime(wineVersion, runtimeModel, launchProfile, launchGeneration, restartIntent);
        }
        if (isRuntimeProfileReady(launchProfile)) {
            logRuntimeReadinessCheckpoint(
                    "XSERVER_RUNTIME_READY",
                    "runtime_profile_ready_before_guest_bootstrap",
                    wineVersion,
                    runtimeModel,
                    launchProfile
            );
            return false;
        }
        if (!launchProfile.isRemoteDownloadable()) {
            ForensicLogger.logEvent(
                    this,
                    "warn",
                    "XSERVER_RUNTIME_PROFILE_MISSING",
                    null,
                    "xserver",
                    "runtime_profile_missing_before_launch",
                    ForensicLogger.fields(
                            "requested_entry", wineVersion,
                            "runtime_model", runtimeModel,
                            "profile_entry", ContentsManager.getEntryName(launchProfile),
                            "profile_remote_url", launchProfile.remoteUrl
                    )
            );
            AppUtils.showToast(this, R.string.unable_to_install_content);
            finish();
            return true;
        }

        if (!canAttemptRemoteRuntimeInstall(launchProfile)) {
            logRemoteRuntimeInstallBlockedOffline(launchProfile, wineVersion, runtimeModel);
            AppUtils.showToast(this, R.string.unable_to_install_content);
            finish();
            return true;
        }

        preloaderDialog.show(R.string.installing_content);
        runtimeBootstrapExecutor.execute(() -> {
            logBootstrapCheckpoint(
                    "XSERVER_RUNTIME_REMOTE_INSTALL_BEGIN",
                    "runtime_remote_install_begin",
                    "requested_entry", safeTrim(wineVersion),
                    "runtime_model", safeTrim(runtimeModel),
                    "profile_entry", ContentsManager.getEntryName(launchProfile)
            );
            boolean installed = installRemoteRuntimeProfile(launchProfile);
            boolean ready = installed && isRuntimeProfileReady(launchProfile);
            logRuntimeReadinessCheckpoint(
                    "XSERVER_RUNTIME_REMOTE_INSTALL_RESULT",
                    "runtime_remote_install_result",
                    wineVersion,
                    runtimeModel,
                    launchProfile
            );
            logBootstrapCheckpoint(
                    "XSERVER_RUNTIME_GATE_RESULT",
                    "runtime_gate_result_after_remote_install",
                    "requested_entry", safeTrim(wineVersion),
                    "runtime_model", safeTrim(runtimeModel),
                    "installed", installed ? "1" : "0",
                    "ready", ready ? "1" : "0"
            );
            runOnUiThread(() -> {
                if (!isLaunchBindingCurrent(launchGeneration)) {
                    logBootstrapCheckpoint(
                            "XSERVER_RUNTIME_GATE_STALE",
                            "runtime_gate_result_ignored_for_stale_launch_binding",
                            "requested_entry", safeTrim(wineVersion),
                            "runtime_model", safeTrim(runtimeModel)
                    );
                    preloaderDialog.closeOnUiThread();
                    return;
                }
                preloaderDialog.closeOnUiThread();
                if (!ready) {
                    AppUtils.showToast(this, R.string.unable_to_install_content);
                    finish();
                    return;
                }

                restartIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                recreateForLaunchRelaunch(restartIntent);
            });
        });
        return true;
    }

    private boolean hydrateAndMaybePromoteReadyGlibcRuntime(String wineVersion,
                                                           String runtimeModel,
                                                           @NonNull ContentProfile currentProfile,
                                                           int launchGeneration,
                                                           @NonNull Intent restartIntent) {
        preloaderDialog.show(R.string.installing_content);
        runtimeBootstrapExecutor.execute(() -> {
            String currentEntry = ContentsManager.getEntryName(currentProfile);
            logBootstrapCheckpoint(
                    "XSERVER_GLIBC_RUNTIME_PROMOTION_HYDRATE_BEGIN",
                    "glibc_runtime_promotion_hydration_begin",
                    "requested_entry", safeTrim(wineVersion),
                    "runtime_model", safeTrim(runtimeModel),
                    "current_entry", currentEntry,
                    "current_source", safeTrim(currentProfile.sourceRepo)
            );
            LaunchHydrationResult hydrationResult = hydrateLaunchRuntimeProfilesForLaunch(
                    wineVersion,
                    runtimeModel,
                    READY_GLIBC_PROMOTION_HYDRATION_TIMEOUT_MS
            );
            logLaunchHydrationResult(
                    "XSERVER_GLIBC_RUNTIME_PROMOTION_HYDRATE_SOURCE_RESULT",
                    "glibc_runtime_promotion_hydration_source_result",
                    wineVersion,
                    runtimeModel,
                    hydrationResult
            );
            ContentProfile promotedProfile = resolveLaunchRuntimeCandidate(wineVersion, runtimeModel);
            boolean sameProfile = isSameRuntimeProfile(currentProfile, promotedProfile);
            boolean promotedCandidateInstalled = contentsManager.isInstalledProfileUsable(promotedProfile)
                    || contentsManager.isInstalledProfilePresent(promotedProfile);
            boolean promotedCandidateLaunchable = promotedProfile != null
                    && (contentsManager.isInstalledProfileUsable(promotedProfile)
                    || (promotedProfile.isRemoteDownloadable() && canAttemptRemoteRuntimeInstall(promotedProfile)));
            boolean promoted = promotedProfile != null
                    && !sameProfile
                    && promotedCandidateLaunchable
                    && RuntimeLaunchPolicy.shouldPromoteGlibcRuntime(
                    currentProfile,
                    contentsManager.isInstalledProfileUsable(currentProfile),
                    contentsManager.isInstalledProfilePresent(currentProfile),
                    promotedProfile,
                    contentsManager.isInstalledProfileUsable(promotedProfile),
                    promotedCandidateInstalled,
                    wineVersion,
                    runtimeModel
            );
            boolean installedFromRemote = false;
            boolean ready = !promoted || isRuntimeProfileReady(promotedProfile);
            if (promoted
                    && !ready
                    && promotedProfile != null
                    && promotedProfile.isRemoteDownloadable()
                    && canAttemptRemoteRuntimeInstall(promotedProfile)) {
                logBootstrapCheckpoint(
                        "XSERVER_GLIBC_RUNTIME_PROMOTION_INSTALL_BEGIN",
                        "glibc_runtime_promotion_remote_install_begin",
                        "requested_entry", safeTrim(wineVersion),
                        "runtime_model", safeTrim(runtimeModel),
                        "current_entry", currentEntry,
                        "promoted_entry", ContentsManager.getEntryName(promotedProfile)
                );
                installedFromRemote = installRemoteRuntimeProfile(promotedProfile);
                ready = installedFromRemote && isRuntimeProfileReady(promotedProfile);
            } else if (promoted && !ready && promotedProfile != null && promotedProfile.isRemoteDownloadable()) {
                logRemoteRuntimeInstallBlockedOffline(promotedProfile, wineVersion, runtimeModel);
            }
            ContentProfile resultProfile = promoted ? promotedProfile : currentProfile;
            logRuntimeReadinessCheckpoint(
                    promoted ? "XSERVER_GLIBC_RUNTIME_PROMOTION_RESULT" : "XSERVER_GLIBC_RUNTIME_PROMOTION_UNCHANGED",
                    promoted ? "glibc_runtime_promotion_result" : "glibc_runtime_promotion_kept_current_runtime",
                    wineVersion,
                    runtimeModel,
                    resultProfile
            );
            logBootstrapCheckpoint(
                    "XSERVER_GLIBC_RUNTIME_PROMOTION_GATE_RESULT",
                    "glibc_runtime_promotion_gate_result",
                    "requested_entry", safeTrim(wineVersion),
                    "runtime_model", safeTrim(runtimeModel),
                    "current_entry", currentEntry,
                    "promoted", promoted ? "1" : "0",
                    "promoted_entry", promotedProfile != null ? ContentsManager.getEntryName(promotedProfile) : "-",
                    "installed_from_remote", installedFromRemote ? "1" : "0",
                    "ready", ready ? "1" : "0"
            );
            boolean finalReady = ready;
            boolean finalPromoted = promoted;
            runOnUiThread(() -> {
                if (!isLaunchBindingCurrent(launchGeneration)) {
                    logBootstrapCheckpoint(
                            "XSERVER_GLIBC_RUNTIME_PROMOTION_GATE_STALE",
                            "glibc_runtime_promotion_ignored_for_stale_launch_binding",
                            "requested_entry", safeTrim(wineVersion),
                            "runtime_model", safeTrim(runtimeModel)
                    );
                    preloaderDialog.closeOnUiThread();
                    return;
                }
                preloaderDialog.closeOnUiThread();
                if (finalPromoted && !finalReady) {
                    AppUtils.showToast(this, R.string.unable_to_install_content);
                    finish();
                    return;
                }

                restartIntent.putExtra(EXTRA_GLIBC_PROMOTION_PROBE_DONE, true);
                restartIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                recreateForLaunchRelaunch(restartIntent);
            });
        });
        return true;
    }

    private boolean shouldProbeGlibcRuntimePromotion(String wineVersion,
                                                    String runtimeModel,
                                                    @NonNull ContentProfile currentProfile) {
        if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_GLIBC_PROMOTION_PROBE_DONE, false)) {
            return false;
        }
        if (!ContentProfile.RUNTIME_MODEL_GLIBC.equals(ContentProfile.normalizeRuntimeModel(runtimeModel))) {
            return false;
        }
        if (!currentProfile.isWineProtonFamily() || currentProfile.isProtonLike()) {
            return RuntimeLaunchPolicy.isKnownLegacyGlibcLaunchRisk(currentProfile, wineVersion);
        }
        return true;
    }

    private boolean isSameRuntimeProfile(@Nullable ContentProfile left, @Nullable ContentProfile right) {
        if (left == null || right == null) return false;
        String leftEntry = ContentsManager.getEntryName(left);
        String rightEntry = ContentsManager.getEntryName(right);
        return !leftEntry.isEmpty() && leftEntry.equalsIgnoreCase(rightEntry);
    }

    private void logRuntimeReadinessCheckpoint(String eventId,
                                               String message,
                                               String wineVersion,
                                               String runtimeModel,
                                               @Nullable ContentProfile profile) {
        ContentsManager.InstalledProfileDiagnostics diagnostics = profile != null
                ? contentsManager.resolveInstalledProfileDiagnostics(profile)
                : null;
        logBootstrapCheckpoint(
                eventId,
                message,
                "requested_entry", safeTrim(wineVersion),
                "runtime_model", safeTrim(runtimeModel),
                "profile_present", profile != null ? "1" : "0",
                "profile_entry", profile != null ? ContentsManager.getEntryName(profile) : "-",
                "profile_type", profile != null && profile.type != null ? profile.type.toString() : "-",
                "profile_runtime_model", profile != null ? profile.getRuntimeModel() : "-",
                "profile_remote_downloadable", profile != null && profile.isRemoteDownloadable() ? "1" : "0",
                "profile_remote_url_present", profile != null && !safeTrim(profile.remoteUrl).isEmpty() ? "1" : "0",
                "state_present", diagnostics != null && diagnostics.state.present ? "1" : "0",
                "state_usable", diagnostics != null && diagnostics.state.usable ? "1" : "0",
                "broken_reason", diagnostics != null && !diagnostics.state.brokenReason.isEmpty()
                        ? diagnostics.state.brokenReason
                        : "-",
                "expected_install_root", diagnostics != null ? diagnostics.canonicalInstallDir : "-",
                "resolved_install_root", diagnostics != null ? diagnostics.resolvedInstallDir : "-",
                "runtime_root", diagnostics != null ? diagnostics.runtimeRoot : "-",
                "profile_json_present", diagnostics != null && diagnostics.profileJsonPresent ? "1" : "0",
                "runtime_root_present", diagnostics != null && diagnostics.runtimeRootPresent ? "1" : "0",
                "runtime_payload_present", diagnostics != null && diagnostics.runtimePayloadPresent ? "1" : "0",
                "alias_resolved", diagnostics != null && diagnostics.aliasResolved ? "1" : "0"
        );
    }

    private boolean shouldAttemptRemoteRuntimeBootstrap(String wineVersion, String runtimeModel) {
        String normalizedWineVersion = wineVersion == null ? "" : wineVersion.trim().toLowerCase(Locale.US);
        String normalizedRuntimeModel = ContentProfile.normalizeRuntimeModel(runtimeModel);
        return normalizedWineVersion.startsWith("proton-")
                || WineInfo.isMainWineVersion(wineVersion)
                || ContentProfile.RUNTIME_MODEL_GLIBC.equals(normalizedRuntimeModel);
    }

    private boolean isRuntimeProfileReady(@Nullable ContentProfile profile) {
        return profile != null
                && profile.isWineProtonFamily()
                && contentsManager.isInstalledProfileUsable(profile);
    }

    @Nullable
    private ContentProfile resolveLaunchRuntimeCandidate(String wineVersion, String runtimeModel) {
        if (WineInfo.isMainWineVersion(wineVersion)) {
            return findPreferredMainWineRuntimeProfile(runtimeModel);
        }

        ContentProfile promotedGlibcProfile = resolvePromotedGlibcLaunchRuntimeProfile(wineVersion, runtimeModel);
        if (promotedGlibcProfile != null) {
            return promotedGlibcProfile;
        }

        String canonicalEntry = contentsManager.resolveBestRuntimeEntry(wineVersion, runtimeModel);
        ContentProfile installed = contentsManager.resolveBestRuntimeProfile(canonicalEntry, runtimeModel);
        if (installed != null) {
            return installed;
        }

        ContentProfile candidate = contentsManager.getProfileByEntryName(canonicalEntry);
        if (candidate != null
                && candidate.isWineProtonFamily()
                && (candidate.isRuntimeModelCompatible(runtimeModel) || contentsManager.isInstalledProfileUsable(candidate))) {
            return candidate;
        }

        candidate = contentsManager.getProfileByEntryName(wineVersion);
        if (candidate != null
                && candidate.isWineProtonFamily()
                && (candidate.isRuntimeModelCompatible(runtimeModel) || contentsManager.isInstalledProfileUsable(candidate))) {
            return candidate;
        }

        ContentProfile.ContentType preferredType = wineVersion != null && wineVersion.toLowerCase(Locale.US).startsWith("proton-")
                ? ContentProfile.ContentType.CONTENT_TYPE_PROTON
                : ContentProfile.ContentType.CONTENT_TYPE_WINE;
        String requestedArch = resolveRuntimeArchHintFromEntry(wineVersion);
        ContentProfile fallback = findPreferredLaunchRuntimeProfile(preferredType, runtimeModel, requestedArch);
        if (fallback != null) {
            ForensicLogger.logEvent(
                    this,
                    "warn",
                    "XSERVER_RUNTIME_FALLBACK_SELECTED",
                    null,
                    "xserver",
                    "runtime_fallback_selected_for_missing_entry",
                    ForensicLogger.fields(
                            "requested_entry", wineVersion,
                            "fallback_entry", ContentsManager.getEntryName(fallback),
                            "runtime_model", runtimeModel,
                            "preferred_type", preferredType.toString()
                    )
            );
        }
        return fallback;
    }

    private String resolveRuntimeArchHintFromEntry(String entryName) {
        String lower = entryName == null ? "" : entryName.trim().toLowerCase(Locale.US);
        if (lower.contains("arm64ec") || lower.contains("arm64-ec")) return "arm64ec";
        if (lower.contains("x86_64") || lower.contains("x86-64") || lower.contains("amd64")) return "x86_64";
        if (lower.contains("arm64") || lower.contains("aarch64")) return "arm64";
        if (lower.contains("x86")) return "x86";
        return "";
    }

    private String normalizeRuntimeArchHint(String arch) {
        String lower = arch == null ? "" : arch.trim().toLowerCase(Locale.US);
        if (lower.equals("amd64") || lower.equals("x64") || lower.equals("x86-64")) return "x86_64";
        if (lower.equals("arm64-ec")) return "arm64ec";
        return lower;
    }

    private LaunchHydrationResult hydrateLaunchRuntimeProfilesForLaunch(String wineVersion,
                                                                         String runtimeModel,
                                                                         long timeoutMs) {
        long startedAt = android.os.SystemClock.elapsedRealtime();
        boolean networkAvailable = isLaunchHydrationNetworkAvailable();
        if (!networkAvailable) {
            hydrateLaunchRuntimeProfiles(wineVersion, runtimeModel, false);
            return new LaunchHydrationResult(
                    true,
                    false,
                    false,
                    android.os.SystemClock.elapsedRealtime() - startedAt,
                    "network_unavailable"
            );
        }

        ExecutorService hydrationExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "XServerRuntimeHydration");
            thread.setDaemon(true);
            return thread;
        });
        Future<?> future = hydrationExecutor.submit(() -> hydrateLaunchRuntimeProfiles(wineVersion, runtimeModel, true));
        try {
            future.get(Math.max(1L, timeoutMs), TimeUnit.MILLISECONDS);
            return new LaunchHydrationResult(
                    true,
                    true,
                    true,
                    android.os.SystemClock.elapsedRealtime() - startedAt,
                    ""
            );
        } catch (TimeoutException e) {
            future.cancel(true);
            return new LaunchHydrationResult(
                    false,
                    true,
                    true,
                    android.os.SystemClock.elapsedRealtime() - startedAt,
                    "timeout"
            );
        } catch (Exception e) {
            future.cancel(true);
            return new LaunchHydrationResult(
                    false,
                    true,
                    true,
                    android.os.SystemClock.elapsedRealtime() - startedAt,
                    e.getClass().getSimpleName()
            );
        } finally {
            hydrationExecutor.shutdownNow();
        }
    }

    private void logLaunchHydrationResult(String eventId,
                                          String message,
                                          String wineVersion,
                                          String runtimeModel,
                                          LaunchHydrationResult result) {
        logBootstrapCheckpoint(
                eventId,
                message,
                "requested_entry", safeTrim(wineVersion),
                "runtime_model", safeTrim(runtimeModel),
                "completed", result != null && result.completed ? "1" : "0",
                "network_available", result != null && result.networkAvailable ? "1" : "0",
                "network_attempted", result != null && result.networkAttempted ? "1" : "0",
                "elapsed_ms", result != null ? result.elapsedMs : -1L,
                "failure_class", result != null && !result.failureClass.isEmpty() ? result.failureClass : "-"
        );
    }

    private void hydrateLaunchRuntimeProfiles(String wineVersion, String runtimeModel, boolean allowNetwork) {
        try {
            ArrayList<String> payloads = new ArrayList<>();

            String bundledArchive = readAssetText("contents.json");
            if (bundledArchive != null && !bundledArchive.trim().isEmpty()) {
                payloads.add(RemoteFeedPayloadLoader.normalizePayload(
                        bundledArchive,
                        ContentsManager.REMOTE_WINE_PROTON_OVERLAY
                ));
            }

            if (allowNetwork) {
                for (RuntimeFeedRegistry.FeedSpec feed : RuntimeFeedRegistry.getLaunchHydrationFeeds(runtimeModel, wineVersion)) {
                    RemoteFeedPayloadLoader.FeedLoadResult result =
                            RuntimeFeedRegistry.looksLikeNightliesSource(feed.sourceRepo + " " + feed.url)
                                    ? RemoteFeedPayloadLoader.loadNightliesPayload()
                                    : RemoteFeedPayloadLoader.loadNormalizedFeed(feed);
                    if (result.hasPayload()) payloads.add(result.payload);
                }
            }

            String merged = RemoteProfileFeedMerger.mergePayloads(payloads);
            if (!merged.trim().isEmpty() && !"[]".equals(merged.trim())) {
                contentsManager.setHydratedRuntimeProfiles(merged);
            }
        } catch (Exception e) {
            Log.w("XServerDisplayActivity", "Unable to hydrate runtime profiles for launch", e);
        }
    }

    private boolean isLaunchHydrationNetworkAvailable() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            if (connectivityManager == null) return false;
            NetworkInfo info = connectivityManager.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean canAttemptRemoteRuntimeInstall(@Nullable ContentProfile remoteProfile) {
        if (remoteProfile == null || !remoteProfile.isRemoteDownloadable()) return false;
        File payloadFile = getRuntimePayloadDownloadFile(remoteProfile);
        return payloadFile.isFile() && payloadFile.length() > 0L || isLaunchHydrationNetworkAvailable();
    }

    private void logRemoteRuntimeInstallBlockedOffline(ContentProfile profile, String wineVersion, String runtimeModel) {
        File payloadFile = getRuntimePayloadDownloadFile(profile);
        logBootstrapCheckpoint(
                "XSERVER_RUNTIME_REMOTE_INSTALL_BLOCKED_OFFLINE",
                "runtime_remote_install_blocked_without_network_or_cache",
                "requested_entry", safeTrim(wineVersion),
                "runtime_model", safeTrim(runtimeModel),
                "profile_entry", profile != null ? ContentsManager.getEntryName(profile) : "-",
                "network_available", isLaunchHydrationNetworkAvailable() ? "1" : "0",
                "payload_file", payloadFile.getAbsolutePath(),
                "cached_bytes", payloadFile.isFile() ? payloadFile.length() : 0L
        );
    }

    private File getRuntimePayloadDownloadFile(@Nullable ContentProfile remoteProfile) {
        return getRuntimePayloadDownloadFile(new File(getCacheDir(), "contents-runtime-downloads"), remoteProfile);
    }

    private File getRuntimePayloadDownloadFile(File downloadDir, @Nullable ContentProfile remoteProfile) {
        String entryName = remoteProfile != null ? ContentsManager.getEntryName(remoteProfile) : "runtime";
        return new File(downloadDir, entryName + resolveRuntimePayloadSuffix(remoteProfile));
    }

    private String resolveRuntimePayloadSuffix(@Nullable ContentProfile remoteProfile) {
        String remoteUrl = remoteProfile != null && remoteProfile.remoteUrl != null
                ? remoteProfile.remoteUrl.toLowerCase(Locale.US)
                : "";
        if (remoteUrl.endsWith(".tzst")) return ".tzst";
        if (remoteUrl.endsWith(".txz")) return ".txz";
        if (remoteUrl.endsWith(".wcp.zst")) return ".wcp.zst";
        if (remoteUrl.endsWith(".wcp.xz")) return ".wcp.xz";
        if (remoteUrl.endsWith(".wcp")) return ".wcp";
        return ".pkg";
    }

    @Nullable
    private String readAssetText(String assetName) {
        if (assetName == null || assetName.trim().isEmpty()) return null;
        try (InputStream inputStream = getAssets().open(assetName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean installRemoteRuntimeProfile(ContentProfile remoteProfile) {
        File downloadDir = new File(getCacheDir(), "contents-runtime-downloads");
        if (!downloadDir.exists() && !downloadDir.mkdirs()) {
            logBootstrapCheckpoint(
                    "XSERVER_RUNTIME_REMOTE_PAYLOAD_CACHE_DIR_FAILED",
                    "runtime_remote_payload_cache_dir_failed",
                    "profile_entry", remoteProfile != null ? ContentsManager.getEntryName(remoteProfile) : "-",
                    "download_dir", downloadDir.getAbsolutePath()
            );
            return false;
        }

        File payloadFile = getRuntimePayloadDownloadFile(downloadDir, remoteProfile);
        long cachedPayloadBytes = payloadFile.isFile() ? payloadFile.length() : 0L;
        boolean materialized = cachedPayloadBytes > 0L
                || ContentPayloadResolver.materialize(getApplicationContext(), remoteProfile, payloadFile);
        logBootstrapCheckpoint(
                cachedPayloadBytes > 0L
                        ? "XSERVER_RUNTIME_REMOTE_PAYLOAD_CACHE_HIT"
                        : "XSERVER_RUNTIME_REMOTE_PAYLOAD_MATERIALIZE_RESULT",
                cachedPayloadBytes > 0L
                        ? "runtime_remote_payload_cache_hit"
                        : "runtime_remote_payload_materialize_result",
                "profile_entry", ContentsManager.getEntryName(remoteProfile),
                "remote_url_present", remoteProfile != null && remoteProfile.remoteUrl != null && !remoteProfile.remoteUrl.trim().isEmpty() ? "1" : "0",
                "payload_file", payloadFile.getAbsolutePath(),
                "cached_bytes", cachedPayloadBytes,
                "payload_bytes", payloadFile.isFile() ? payloadFile.length() : 0L,
                "materialized", materialized ? "1" : "0",
                "part_bytes", new File(payloadFile.getAbsolutePath() + ".part").isFile()
                        ? new File(payloadFile.getAbsolutePath() + ".part").length()
                        : 0L
        );
        if (!materialized || !payloadFile.isFile()) {
            logBootstrapCheckpoint(
                    "XSERVER_RUNTIME_REMOTE_PAYLOAD_MATERIALIZE_FAILED",
                    "runtime_remote_payload_materialize_failed",
                    "profile_entry", ContentsManager.getEntryName(remoteProfile),
                    "payload_file", payloadFile.getAbsolutePath(),
                    "payload_exists", payloadFile.isFile() ? "1" : "0",
                    "payload_bytes", payloadFile.isFile() ? payloadFile.length() : 0L
            );
            return false;
        }

        RuntimePayloadInstallResult installResult = installMaterializedRuntimePayload(remoteProfile, payloadFile);
        if (!installResult.success && cachedPayloadBytes > 0L && "extract".equals(installResult.failureStage)) {
            logBootstrapCheckpoint(
                    "XSERVER_RUNTIME_REMOTE_PAYLOAD_CACHE_REJECTED",
                    "runtime_remote_payload_cache_rejected_before_redownload",
                    "profile_entry", ContentsManager.getEntryName(remoteProfile),
                    "payload_file", payloadFile.getAbsolutePath(),
                    "cached_bytes", cachedPayloadBytes
            );
            FileUtils.delete(payloadFile);
            boolean redownloaded = ContentPayloadResolver.materialize(getApplicationContext(), remoteProfile, payloadFile);
            logBootstrapCheckpoint(
                    "XSERVER_RUNTIME_REMOTE_PAYLOAD_REDOWNLOAD_RESULT",
                    "runtime_remote_payload_redownload_result",
                    "profile_entry", ContentsManager.getEntryName(remoteProfile),
                    "payload_file", payloadFile.getAbsolutePath(),
                    "payload_bytes", payloadFile.isFile() ? payloadFile.length() : 0L,
                    "redownloaded", redownloaded ? "1" : "0"
            );
            if (redownloaded && payloadFile.isFile()) {
                installResult = installMaterializedRuntimePayload(remoteProfile, payloadFile);
            }
        }

        if (!installResult.success) {
            logBootstrapCheckpoint(
                    "XSERVER_RUNTIME_REMOTE_INSTALL_FAILED",
                    "runtime_remote_install_failed",
                    "profile_entry", ContentsManager.getEntryName(remoteProfile),
                    "failure_stage", installResult.failureStage,
                    "payload_file", payloadFile.getAbsolutePath(),
                    "payload_bytes", payloadFile.isFile() ? payloadFile.length() : 0L
            );
            return false;
        }

        contentsManager.syncContents();
        ContentProfile installedProfile = contentsManager.resolveBestRuntimeProfile(ContentsManager.getEntryName(remoteProfile), remoteProfile.getRuntimeModel());
        boolean ready = isRuntimeProfileReady(installedProfile != null ? installedProfile : remoteProfile);
        logBootstrapCheckpoint(
                "XSERVER_RUNTIME_REMOTE_INSTALL_READY_CHECK",
                "runtime_remote_install_ready_check",
                "profile_entry", ContentsManager.getEntryName(remoteProfile),
                "installed_profile_entry", installedProfile != null ? ContentsManager.getEntryName(installedProfile) : "-",
                "ready", ready ? "1" : "0"
        );
        return ready;
    }

    private static final class RuntimePayloadInstallResult {
        final boolean success;
        final String failureStage;

        private RuntimePayloadInstallResult(boolean success, String failureStage) {
            this.success = success;
            this.failureStage = failureStage == null ? "" : failureStage;
        }

        static RuntimePayloadInstallResult success() {
            return new RuntimePayloadInstallResult(true, "");
        }

        static RuntimePayloadInstallResult failure(String failureStage) {
            return new RuntimePayloadInstallResult(false, failureStage);
        }
    }

    private RuntimePayloadInstallResult installMaterializedRuntimePayload(ContentProfile remoteProfile, File payloadFile) {
        final ContentProfile[] extractedProfile = new ContentProfile[1];
        final boolean[] installSucceeded = new boolean[1];
        final String[] failureReason = new String[] { "" };
        final String[] failureDetail = new String[] { "" };

        contentsManager.extraContentFile(Uri.fromFile(payloadFile), remoteProfile, new ContentsManager.OnInstallFinishedCallback() {
            @Override
            public void onFailed(ContentsManager.InstallFailedReason reason, Exception e) {
                installSucceeded[0] = false;
                failureReason[0] = reason != null ? reason.name() : "";
                failureDetail[0] = e != null ? e.getClass().getName() + ":" + e.getMessage() : "";
            }

            @Override
            public void onSucceed(ContentProfile profile) {
                extractedProfile[0] = profile;
                installSucceeded[0] = true;
            }
        });
        if (!installSucceeded[0] || extractedProfile[0] == null) {
            logBootstrapCheckpoint(
                    "XSERVER_RUNTIME_REMOTE_PAYLOAD_EXTRACT_FAILED",
                    "runtime_remote_payload_extract_failed",
                    "profile_entry", ContentsManager.getEntryName(remoteProfile),
                    "payload_file", payloadFile != null ? payloadFile.getAbsolutePath() : "",
                    "reason", failureReason[0],
                    "detail", failureDetail[0]
            );
            return RuntimePayloadInstallResult.failure("extract");
        }

        installSucceeded[0] = false;
        failureReason[0] = "";
        failureDetail[0] = "";
        contentsManager.finishInstallContent(extractedProfile[0], new ContentsManager.OnInstallFinishedCallback() {
            @Override
            public void onFailed(ContentsManager.InstallFailedReason reason, Exception e) {
                installSucceeded[0] = false;
                failureReason[0] = reason != null ? reason.name() : "";
                failureDetail[0] = e != null ? e.getClass().getName() + ":" + e.getMessage() : "";
            }

            @Override
            public void onSucceed(ContentProfile profile) {
                installSucceeded[0] = true;
            }
        });
        if (!installSucceeded[0]) {
            logBootstrapCheckpoint(
                    "XSERVER_RUNTIME_REMOTE_PAYLOAD_FINISH_FAILED",
                    "runtime_remote_payload_finish_failed",
                    "profile_entry", ContentsManager.getEntryName(remoteProfile),
                    "extracted_profile_entry", extractedProfile[0] != null ? ContentsManager.getEntryName(extractedProfile[0]) : "-",
                    "payload_file", payloadFile != null ? payloadFile.getAbsolutePath() : "",
                    "reason", failureReason[0],
                    "detail", failureDetail[0]
            );
            return RuntimePayloadInstallResult.failure("finish");
        }

        logBootstrapCheckpoint(
                "XSERVER_RUNTIME_REMOTE_PAYLOAD_INSTALLED",
                "runtime_remote_payload_installed",
                "profile_entry", ContentsManager.getEntryName(remoteProfile),
                "extracted_profile_entry", extractedProfile[0] != null ? ContentsManager.getEntryName(extractedProfile[0]) : "-"
        );
        return RuntimePayloadInstallResult.success();
    }

    private String normalizeSha256(String value) {
        if (value == null) return "";
        String normalized = value.trim().toLowerCase(Locale.US).replaceAll("[^0-9a-f]", "");
        return normalized.length() == 64 ? normalized : "";
    }

    private String chooseRemotePayloadSuffix(ContentProfile remoteProfile, String fallback) {
        String remoteUrl = remoteProfile == null || remoteProfile.remoteUrl == null
                ? ""
                : remoteProfile.remoteUrl.toLowerCase(Locale.US);
        if (remoteUrl.endsWith(".zip")) return ".zip";
        if (remoteUrl.endsWith(".tzst")) return ".tzst";
        if (remoteUrl.endsWith(".txz")) return ".txz";
        if (remoteUrl.endsWith(".wcp.zst")) return ".wcp.zst";
        if (remoteUrl.endsWith(".wcp.xz")) return ".wcp.xz";
        if (remoteUrl.endsWith(".wcp")) return ".wcp";
        return fallback;
    }

    private String stripArchiveSuffix(String value) {
        if (value == null) return "";
        String normalized = value.trim();
        String lower = normalized.toLowerCase(Locale.US);
        String[] suffixes = {".zip", ".wcp.zst", ".wcp.xz", ".wcp", ".txz", ".tzst", ".tar.xz", ".tar.zst", ".tar"};
        for (String suffix : suffixes) {
            if (lower.endsWith(suffix)) {
                return normalized.substring(0, normalized.length() - suffix.length()).trim();
            }
        }
        return normalized;
    }

    @Nullable
    private ContentProfile.ContentType resolveGraphicsProfileTypeForLane(String providerLane) {
        String normalizedLane = safeTrim(providerLane).toLowerCase(Locale.US);
        return switch (normalizedLane) {
            case "freedreno-opengl", "zink-opengl", "aemali-gallium" ->
                    ContentProfile.ContentType.CONTENT_TYPE_OPENGL_DRIVER;
            case "turnip-vulkan", "aemali-panvk" ->
                    ContentProfile.ContentType.CONTENT_TYPE_TURNIP_DRIVER;
            default -> null;
        };
    }

    @Nullable
    private ContentProfile resolveGraphicsRemoteProfileForLane(String providerLane,
                                                              @Nullable AdrenotoolsManager.DriverPackageInfo referenceInfo) {
        if (contentsManager == null) return null;
        ContentProfile.ContentType contentType = resolveGraphicsProfileTypeForLane(providerLane);
        if (contentType == null) return null;

        LinkedHashSet<String> hints = new LinkedHashSet<>();
        if (referenceInfo != null) {
            if (!safeTrim(referenceInfo.entryId).isEmpty()) hints.add(referenceInfo.entryId);
            if (!safeTrim(referenceInfo.name).isEmpty()) hints.add(referenceInfo.name);
            if (!safeTrim(referenceInfo.artifactName).isEmpty()) hints.add(stripArchiveSuffix(referenceInfo.artifactName));
            if (!safeTrim(referenceInfo.releaseTag).isEmpty()) hints.add(referenceInfo.releaseTag);
        }
        if (contentType == ContentProfile.ContentType.CONTENT_TYPE_OPENGL_DRIVER) {
            hints.add("rolling-arm64");
        }

        for (String hint : hints) {
            if (hint == null || hint.trim().isEmpty()) continue;
            ContentProfile profile = contentsManager.findProfileByVersion(contentType, hint, false);
            if (profile != null && profile.remoteUrl != null && !profile.remoteUrl.trim().isEmpty()) {
                return profile;
            }
        }
        return null;
    }

    private boolean installRemoteGraphicsProfile(ContentProfile remoteProfile) {
        if (remoteProfile == null || remoteProfile.remoteUrl == null || remoteProfile.remoteUrl.trim().isEmpty()) {
            return false;
        }

        File downloadDir = new File(getCacheDir(), "contents-graphics-downloads");
        if (!downloadDir.exists() && !downloadDir.mkdirs()) {
            return false;
        }

        File payloadFile = new File(
                downloadDir,
                ContentsManager.getEntryName(remoteProfile) + chooseRemotePayloadSuffix(remoteProfile, ".zip")
        );

        boolean downloaded = Downloader.downloadFile(remoteProfile.remoteUrl, payloadFile);
        String expectedSha256 = normalizeSha256(remoteProfile.remoteSha256);
        if (downloaded && !expectedSha256.isEmpty()) {
            String actualSha256 = normalizeSha256(Downloader.sha256Hex(payloadFile));
            if (!expectedSha256.equals(actualSha256)) {
                downloaded = false;
            }
        }
        if (!downloaded || !payloadFile.isFile()) {
            if (payloadFile.exists()) payloadFile.delete();
            return false;
        }

        String installedDriverName = "";
        try {
            installedDriverName = new AdrenotoolsManager(this).installDriver(Uri.fromFile(payloadFile), remoteProfile);
        } catch (Exception ignored) {
        } finally {
            if (payloadFile.exists()) payloadFile.delete();
        }
        if (installedDriverName == null || installedDriverName.trim().isEmpty()) {
            return false;
        }

        contentsManager.syncContents();
        return true;
    }

    @Nullable
    private AdrenotoolsManager.DriverPackageInfo ensureGraphicsProviderLaneInstalled(
            AdrenotoolsManager adrenotoolsManager,
            String providerLane,
            @Nullable AdrenotoolsManager.DriverPackageInfo referenceInfo
    ) {
        if (adrenotoolsManager == null) return null;
        String normalizedLane = safeTrim(providerLane);
        if (normalizedLane.isEmpty()) return null;

        AdrenotoolsManager.DriverPackageInfo resolved = adrenotoolsManager.resolvePreferredDriverForLane(normalizedLane, referenceInfo);
        if (resolved != null) return resolved;

        ContentProfile remoteProfile = resolveGraphicsRemoteProfileForLane(normalizedLane, referenceInfo);
        ForensicLogger.logEvent(
                this,
                remoteProfile == null ? "warn" : "info",
                "GRAPHICS_COMPANION_PROVIDER_INSTALL_ATTEMPT",
                null,
                "graphics_provider",
                remoteProfile == null ? "graphics_companion_provider_missing_from_catalog" : "graphics_companion_provider_install_attempt",
                ForensicLogger.fields(
                        "provider_lane", normalizedLane,
                        "reference_entry", referenceInfo == null ? "" : firstNonEmpty(referenceInfo.entryId, referenceInfo.name),
                        "reference_provider_lane", referenceInfo == null ? "" : referenceInfo.providerLane,
                        "remote_entry", remoteProfile == null ? "" : ContentsManager.getEntryName(remoteProfile),
                        "remote_url", remoteProfile == null ? "" : firstNonEmpty(remoteProfile.remoteUrl, "")
                )
        );
        if (remoteProfile == null) return null;

        boolean installed = installRemoteGraphicsProfile(remoteProfile);
        resolved = adrenotoolsManager.resolvePreferredDriverForLane(normalizedLane, referenceInfo);
        ForensicLogger.logEvent(
                this,
                installed && resolved != null ? "info" : "warn",
                "GRAPHICS_COMPANION_PROVIDER_INSTALL_RESULT",
                null,
                "graphics_provider",
                installed && resolved != null ? "graphics_companion_provider_ready" : "graphics_companion_provider_install_failed",
                ForensicLogger.fields(
                        "provider_lane", normalizedLane,
                        "reference_entry", referenceInfo == null ? "" : firstNonEmpty(referenceInfo.entryId, referenceInfo.name),
                        "remote_entry", ContentsManager.getEntryName(remoteProfile),
                        "install_succeeded", installed ? "1" : "0",
                        "resolved_entry", resolved == null ? "" : resolved.entryId,
                        "resolved_package", resolved == null ? "" : resolved.name
                )
        );
        return resolved;
    }

    private boolean parseBoolean(String value) {
        // Return true for "true", "1", "yes" (case-insensitive)
        if ("true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value)) {
            return true;
        }
        // Return false for any other value, including "false", "0", "no"
        return false;
    }

    // Inside XServerDisplayActivity class
    private void handleCapturedPointer(MotionEvent event) {
        boolean handled = false;

        int actionButton = event.getActionButton();
        switch (event.getAction()) {
            case MotionEvent.ACTION_BUTTON_PRESS:
                if (actionButton == MotionEvent.BUTTON_PRIMARY) {
                    if (xServer.isRelativeMouseMovement())
                        xServer.getWinHandler().mouseEvent(MouseEventFlags.LEFTDOWN, 0, 0, 0);
                    else
                        xServer.injectPointerButtonPress(Pointer.Button.BUTTON_LEFT);
                } else if (actionButton == MotionEvent.BUTTON_SECONDARY) {
                    if (xServer.isRelativeMouseMovement())
                        xServer.getWinHandler().mouseEvent(MouseEventFlags.RIGHTDOWN, 0, 0, 0);
                    else
                        xServer.injectPointerButtonPress(Pointer.Button.BUTTON_RIGHT);
                } else if (actionButton == MotionEvent.BUTTON_TERTIARY) {
                    if (xServer.isRelativeMouseMovement())
                        xServer.getWinHandler().mouseEvent(MouseEventFlags.MIDDLEDOWN, 0, 0, 0);
                    else
                        xServer.injectPointerButtonPress(Pointer.Button.BUTTON_MIDDLE); // Add this line for middle mouse button press
                }
                handled = true;
                break;
            case MotionEvent.ACTION_BUTTON_RELEASE:
                if (actionButton == MotionEvent.BUTTON_PRIMARY) {
                    if (xServer.isRelativeMouseMovement())
                        xServer.getWinHandler().mouseEvent(MouseEventFlags.LEFTUP, 0, 0, 0);
                    else
                        xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_LEFT);
                } else if (actionButton == MotionEvent.BUTTON_SECONDARY) {
                    if (xServer.isRelativeMouseMovement())
                        xServer.getWinHandler().mouseEvent(MouseEventFlags.RIGHTUP, 0, 0, 0);
                    else
                        xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_RIGHT);
                } else if (actionButton == MotionEvent.BUTTON_TERTIARY) {
                    if (xServer.isRelativeMouseMovement())
                        xServer.getWinHandler().mouseEvent(MouseEventFlags.MIDDLEUP, 0, 0, 0);
                    else
                        xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_MIDDLE); // Add this line for middle mouse button release
                }
                handled = true;
                break;
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_HOVER_MOVE:
                float[] transformedPoint = XForm.transformPoint(xform, event.getX(), event.getY());
                if (xServer.isRelativeMouseMovement())
                    xServer.getWinHandler().mouseEvent(MouseEventFlags.MOVE, (int)transformedPoint[0], (int)transformedPoint[1], 0);
                else
                    xServer.injectPointerMoveDelta((int)transformedPoint[0], (int)transformedPoint[1]);
                handled = true;
                break;
            case MotionEvent.ACTION_SCROLL:
                float scrollY = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
                if (scrollY <= -1.0f) {
                    if (xServer.isRelativeMouseMovement())
                        xServer.getWinHandler().mouseEvent(MouseEventFlags.WHEEL, 0, 0, (int)scrollY * 270);
                    else {
                        xServer.injectPointerButtonPress(Pointer.Button.BUTTON_SCROLL_DOWN);
                        xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_SCROLL_DOWN);
                    }
                } else if (scrollY >= 1.0f) {
                    if (xServer.isRelativeMouseMovement())
                        xServer.getWinHandler().mouseEvent(MouseEventFlags.WHEEL, 0, 0,(int)scrollY * 270);
                    else {
                        xServer.injectPointerButtonPress(Pointer.Button.BUTTON_SCROLL_UP);
                        xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_SCROLL_UP);
                    }
                }
                handled = true;
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MainActivity.EDIT_INPUT_CONTROLS_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (editInputControlsCallback != null) {
                editInputControlsCallback.run();
                editInputControlsCallback = null;
            }
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        boolean launchTargetChanged = hasLaunchTargetChanged(intent);
        setIntent(intent);
        refreshForensicTrace(intent);
        logBootstrapCheckpoint(
                "XSERVER_NEW_INTENT_RECEIVED",
                "xserver_activity_received_new_intent",
                "container_id", intent != null ? intent.getIntExtra("container_id", 0) : 0,
                "shortcut_path", intent != null ? intent.getStringExtra("shortcut_path") : "",
                "app_id", resolveIntentLaunchAppId(intent),
                "launch_route_token", resolveIntentLaunchRouteToken(intent),
                "forensic_trace_generated", forensicTraceGenerated,
                "forensic_route_source", forensicRouteSource,
                "activity_has_focus", hasWindowFocus(),
                "guest_bootstrap_submitted", guestBootstrapSubmitted,
                "launch_target_changed", launchTargetChanged
        );
        if (launchTargetChanged) {
            restoreTemporaryOverrideIfNeeded("new_intent_recreate");
            launchBindingGeneration++;
            logBootstrapCheckpoint(
                    "XSERVER_NEW_INTENT_RECREATE",
                    "xserver_activity_recreate_for_new_launch_target",
                    "current_container_id", activeLaunchContainerId,
                    "incoming_container_id", resolveIntentContainerId(intent),
                    "current_shortcut_path", activeLaunchShortcutPath,
                    "incoming_shortcut_path", normalizeShortcutPath(intent != null ? intent.getStringExtra("shortcut_path") : null),
                    "current_app_id", activeLaunchAppId,
                    "incoming_app_id", resolveIntentLaunchAppId(intent),
                    "current_launch_route_token", activeLaunchRouteToken,
                    "incoming_launch_route_token", resolveIntentLaunchRouteToken(intent),
                    "guest_bootstrap_submitted", guestBootstrapSubmitted
            );
            recreate();
            return;
        }
        maybeRunPendingGuestBootstrap("new_intent");
    }


    @Override
    public void onResume() {
        super.onResume();
        cancelDeferredDesktopRuntimePause("resume");
        AppUtils.hideSystemUI(this);
        refreshDesktopGestureExclusion();
        if (bootstrapFirstDrawObserved || guestBootstrapSubmitted) {
            registerGyroListenerIfEnabled("activity_resume");
        } else {
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "XSERVER_GYRO_RESUME_REGISTRATION_DEFERRED",
                    null,
                    "xserver",
                    "gyro_resume_registration_deferred_until_bootstrap",
                    ForensicLogger.fields(
                            "first_draw_observed", bootstrapFirstDrawObserved,
                            "guest_bootstrap_submitted", guestBootstrapSubmitted
                    )
            );
        }

        if (environment != null) {
            resumeXServerViewSurface("activity_resume");
            environment.onResume();
        }
        startTime = System.currentTimeMillis();
        handler.postDelayed(savePlaytimeRunnable, SAVE_INTERVAL_MS);
        ProcessHelper.resumeAllWineProcesses();
        maybeRunPendingGuestBootstrap("resume");
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterGyroListenerIfNeeded("activity_pause");

        boolean enteringPictureInPicture = isInPictureInPictureMode();
        pauseXServerViewSurface("activity_pause");
        if (!shouldAutoSuspendRuntimeOnLifecycle()) {
            cancelDeferredDesktopRuntimePause("pause_suspend_policy");
            logDesktopRuntimePauseSkipped("pause_suspend_policy");
            return;
        }
        boolean deferDesktopRuntimePause = !enteringPictureInPicture && shouldKeepDesktopRuntimeActiveOnPause();

        if (!enteringPictureInPicture && !deferDesktopRuntimePause) {
            pauseDesktopRuntime("immediate_pause");
        } else if (deferDesktopRuntimePause) {
            scheduleDeferredDesktopRuntimePause();
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "XSERVER_RUNTIME_PAUSE_DELAYED",
                    null,
                    "xserver",
                    "desktop_runtime_pause_delayed_for_transient_focus_loss",
                    ForensicLogger.fields(
                            "desktop_shell_bootstrap", desktopShellBootstrapActive,
                            "tracked_window_count", getTrackedApplicationWindowCount(),
                            "runtime_drawer_visible", runtimeDrawerVisible,
                            "exit_in_progress", exitInProgress.get(),
                            "grace_ms", DESKTOP_RUNTIME_PAUSE_GRACE_MS
                    )
            );
        } else {
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "XSERVER_PIP_CONTINUITY",
                    null,
                    "xserver",
                    "pip_transition_without_runtime_pause",
                    ForensicLogger.fields(
                            "wine_paused", false,
                            "playtime_timer_kept", true
                    )
            );
        }
    }

    private void pauseXServerViewSurface(String reason) {
        if (xServerView == null || xServerViewLifecyclePaused) return;
        try {
            xServerView.onPause();
            xServerViewLifecyclePaused = true;
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "XSERVER_GLSURFACE_LIFECYCLE_PAUSED",
                    null,
                    "xserver_surface",
                    "xserver_glsurface_paused",
                    ForensicLogger.fields(
                            "reason", reason,
                            "desktop_shell_bootstrap", desktopShellBootstrapActive,
                            "tracked_window_count", getTrackedApplicationWindowCount(),
                            "runtime_drawer_visible", runtimeDrawerVisible
                    )
            );
        } catch (Throwable e) {
            ForensicLogger.error(
                    this,
                    "XSERVER_GLSURFACE_LIFECYCLE_PAUSE_FAILED",
                    null,
                    "xserver_surface",
                    "xserver_glsurface_pause_failed",
                    e,
                    ForensicLogger.fields("reason", reason)
            );
        }
    }

    private void resumeXServerViewSurface(String reason) {
        if (xServerView == null) return;
        try {
            xServerView.onResume();
            xServerViewLifecyclePaused = false;
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "XSERVER_GLSURFACE_LIFECYCLE_RESUMED",
                    null,
                    "xserver_surface",
                    "xserver_glsurface_resumed",
                    ForensicLogger.fields(
                            "reason", reason,
                            "desktop_shell_bootstrap", desktopShellBootstrapActive,
                            "tracked_window_count", getTrackedApplicationWindowCount(),
                            "runtime_drawer_visible", runtimeDrawerVisible
                    )
            );
        } catch (Throwable e) {
            ForensicLogger.error(
                    this,
                    "XSERVER_GLSURFACE_LIFECYCLE_RESUME_FAILED",
                    null,
                    "xserver_surface",
                    "xserver_glsurface_resume_failed",
                    e,
                    ForensicLogger.fields("reason", reason)
            );
        }
    }


    private void savePlaytimeData() {
        long endTime = System.currentTimeMillis();
        long playtime = endTime - startTime;

        // Ensure that playtime is not negative
        if (playtime < 0) {
            playtime = 0;
        }

        SharedPreferences.Editor editor = playtimePrefs.edit();
        String playtimeKey = shortcutName + "_playtime";

        // Accumulate the playtime into totalPlaytime
        long totalPlaytime = playtimePrefs.getLong(playtimeKey, 0) + playtime;
        editor.putLong(playtimeKey, totalPlaytime);
        editor.apply();

        // Reset startTime to the current time for the next interval
        startTime = System.currentTimeMillis();
    }


    private void incrementPlayCount() {
        SharedPreferences.Editor editor = playtimePrefs.edit();
        String playCountKey = shortcutName + "_play_count";
        int playCount = playtimePrefs.getInt(playCountKey, 0) + 1;
        editor.putInt(playCountKey, playCount);
        editor.apply();
    }

    private void waitForWineProcessesToTerminate(long timeoutMs) {
        long start = System.currentTimeMillis();
        while (!ProcessHelper.listRunningWineProcesses().isEmpty()) {
            if ((System.currentTimeMillis() - start) >= timeoutMs) {
                break;
            }
            try {
                Thread.sleep(50);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void performExitTeardown() {
        long teardownStart = System.currentTimeMillis();
        ForensicLogger.logEvent(
                this,
                "info",
                "XSERVER_EXIT_TEARDOWN_START",
                null,
                "xserver",
                "runtime_exit_teardown_started",
                null
        );

        try {
            savePlaytimeData();
            handler.removeCallbacks(savePlaytimeRunnable);
            if (midiHandler != null) midiHandler.stop();
            shutdownGyroThread();
            if (winHandler != null) winHandler.stop();
            if (environment != null) environment.stopEnvironmentComponents();
            ProcessHelper.terminateAllWineProcesses();
            waitForWineProcessesToTerminate(1500);
            cleanupWineTrashAfterShutdown();

            ForensicLogger.logEvent(
                    this,
                    "info",
                    "XSERVER_EXIT_TEARDOWN_DONE",
                    null,
                    "xserver",
                    "runtime_exit_teardown_completed",
                    ForensicLogger.fields(
                            "duration_ms", System.currentTimeMillis() - teardownStart
                    )
            );
        }
        catch (Throwable t) {
            Log.e("XServerDisplayActivity", "Exit teardown failed", t);
            ForensicLogger.logEvent(
                    this,
                    "error",
                    "XSERVER_EXIT_TEARDOWN_FAILED",
                    null,
                    "xserver",
                    "runtime_exit_teardown_failed",
                    ForensicLogger.fields(
                            "duration_ms", System.currentTimeMillis() - teardownStart,
                            "error_class", t.getClass().getName(),
                            "message", t.getMessage()
                    )
            );
        }
        finally {
            handler.post(() -> {
                if (preloaderDialog != null) preloaderDialog.closeOnUiThread();
                exitInProgress.set(false);
                AppUtils.restartApplication(getApplicationContext());
            });
        }
    }

    private void cleanupWineTrashAfterShutdown() {
        if (container == null || container.getRootDir() == null) return;
        File trashDir = new File(container.getRootDir(), ".local/share/Trash");
        File[] children = trashDir.listFiles();
        if (children == null) {
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "XSERVER_TRASH_CLEANUP_SKIPPED",
                    null,
                    "xserver",
                    "wine_xdg_trash_missing_or_unreadable",
                    ForensicLogger.fields("trash_dir", trashDir.getAbsolutePath())
            );
            return;
        }
        if (children.length == 0) {
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "XSERVER_TRASH_CLEANUP_EMPTY",
                    null,
                    "xserver",
                    "wine_xdg_trash_already_empty",
                    ForensicLogger.fields("trash_dir", trashDir.getAbsolutePath())
            );
            return;
        }

        int deleted = 0;
        for (File child : children) {
            if (FileUtils.delete(child)) deleted++;
        }
        ForensicLogger.logEvent(
                this,
                "info",
                "XSERVER_TRASH_CLEANUP_DONE",
                null,
                "xserver",
                "wine_xdg_trash_cleanup_complete",
                ForensicLogger.fields(
                        "trash_dir", trashDir.getAbsolutePath(),
                        "deleted_count", deleted,
                        "total_count", children.length
                )
        );
    }

    private void exit() {
        if (!exitInProgress.compareAndSet(false, true)) return;
        restoreTemporaryOverrideIfNeeded("runtime_exit");
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
        ForensicLogger.logEvent(
                this,
                "info",
                "XSERVER_EXIT_REQUESTED",
                null,
                "xserver",
                "runtime_exit_requested",
                null
        );
        preloaderDialog.showOnUiThread(R.string.shutdown);
        handler.postDelayed(() -> exitTeardownExecutor.execute(this::performExitTeardown), 1000);
    }

    @Override
    protected void onDestroy() {
        restoreTemporaryOverrideIfNeeded("activity_destroy");
        shutdownGyroThread();
        ForensicLogger.clearActiveTraceId(forensicTraceIdOrNull());
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!shouldAutoSuspendRuntimeOnLifecycle()) {
            cancelDeferredDesktopRuntimePause("stop_suspend_policy");
            logDesktopRuntimePauseSkipped("stop_suspend_policy");
            savePlaytimeData();
            handler.removeCallbacks(savePlaytimeRunnable);
            return;
        }
        int trackedWindowCount = getTrackedApplicationWindowCount();
        DesktopShellBootstrapProof proof = null;
        if (desktopShellBootstrapActive) {
            proof = collectDesktopShellBootstrapProof();
        }
        if (shouldKeepDesktopRuntimeActiveAcrossStop(proof, trackedWindowCount)) {
            scheduleDeferredDesktopRuntimePause(DESKTOP_RUNTIME_STOP_BOOTSTRAP_GRACE_MS);
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "XSERVER_RUNTIME_PAUSE_STOP_DEFERRED",
                    null,
                    "xserver",
                    "desktop_runtime_pause_deferred_during_live_bootstrap_stop",
                    ForensicLogger.fields(
                            "desktop_shell_bootstrap", desktopShellBootstrapActive,
                            "tracked_window_count", trackedWindowCount,
                            "grace_ms", DESKTOP_RUNTIME_STOP_BOOTSTRAP_GRACE_MS,
                            "bootstrap_elapsed_ms", proof != null ? proof.bootstrapElapsedMs : 0L,
                            "shell_launcher_present", proof != null && proof.shellLauncherPresent,
                            "shell_process_present", proof != null && proof.explorerProcessPresent,
                            "wineboot_process_present", proof != null && proof.winebootProcessPresent,
                            "wineserver_present", proof != null && proof.wineserverPresent
                    )
            );
        } else if (deferredDesktopPauseScheduled) {
            cancelDeferredDesktopRuntimePause("stop");
            pauseDesktopRuntime("stop_background_pause");
        }
        savePlaytimeData();
        handler.removeCallbacks(savePlaytimeRunnable);
    }

    @Override
    public void onBackPressed() {
        handleDesktopBackNavigation();
    }

    private void setupRuntimeDrawer() {
        runtimeDrawerScrim = findViewById(R.id.VRuntimeDrawerScrim);
        runtimeDrawerView = findViewById(R.id.SVRuntimeDrawer);
        if (runtimeDrawerScrim == null || runtimeDrawerView == null) return;

        runtimeDrawerScrim.setOnClickListener(v -> hideRuntimeDrawer());
        runtimeDrawerView.setOnClickListener(v -> {});
        View closeButton = findViewById(R.id.BTCloseRuntimeDrawer);
        if (closeButton != null) closeButton.setOnClickListener(v -> hideRuntimeDrawer());

        bindRuntimeDrawerAction(R.id.LLRuntimeActionKeyboard, R.id.main_menu_keyboard);
        bindRuntimeDrawerAction(R.id.LLRuntimeActionInputControls, R.id.main_menu_input_controls);
        bindRuntimeDrawerAction(R.id.LLRuntimeActionRelativeMouse, R.id.main_menu_relative_mouse_movement);
        bindRuntimeDrawerAction(R.id.LLRuntimeActionScreenEffects, R.id.main_menu_screen_effects);
        bindRuntimeDrawerAction(R.id.LLRuntimeActionFullscreen, R.id.main_menu_toggle_fullscreen);
        bindRuntimeDrawerAction(R.id.LLRuntimeActionPauseResume, R.id.main_menu_pause);
        bindRuntimeDrawerAction(R.id.LLRuntimeActionPip, R.id.main_menu_pip_mode);
        bindRuntimeDrawerAction(R.id.LLRuntimeActionTaskManager, R.id.main_menu_task_manager);
        bindRuntimeDrawerAction(R.id.LLRuntimeActionActiveWindows, R.id.main_menu_active_windows);
        bindRuntimeDrawerAction(R.id.LLRuntimeActionMagnifier, R.id.main_menu_magnifier);
        bindRuntimeDrawerAction(R.id.LLRuntimeActionLogs, R.id.main_menu_logs);
        View prefixPackRow = findViewById(R.id.LLRuntimeActionPrefixPack);
        if (prefixPackRow != null) {
            prefixPackRow.setOnClickListener(v -> {
                hideRuntimeDrawer();
                showPrefixPackGuide();
            });
        }
        View runtimeProfilesRow = findViewById(R.id.LLRuntimeActionRuntimeProfiles);
        if (runtimeProfilesRow != null) {
            runtimeProfilesRow.setOnClickListener(v -> {
                hideRuntimeDrawer();
                showRuntimeProfilesDialog();
            });
        }
        bindRuntimeDrawerAction(R.id.LLRuntimeActionExit, R.id.main_menu_exit);

        runtimeDrawerView.post(() -> runtimeDrawerView.setTranslationX(getRuntimeDrawerHiddenOffset()));
        refreshRuntimeDrawerState();
    }

    private void bindRuntimeDrawerAction(int rowId, int actionId) {
        View row = findViewById(rowId);
        if (row == null) return;
        row.setOnClickListener(v -> {
            hideRuntimeDrawer();
            handleRuntimeAction(actionId);
        });
    }

    private void toggleRuntimeDrawer() {
        if (runtimeDrawerVisible) hideRuntimeDrawer();
        else showRuntimeDrawer();
    }

    private void showRuntimeDrawer() {
        if (environment == null || runtimeDrawerView == null || runtimeDrawerScrim == null) return;
        refreshRuntimeDrawerState();
        runtimeDrawerVisible = true;
        runtimeDrawerScrim.setVisibility(View.VISIBLE);
        runtimeDrawerView.setVisibility(View.VISIBLE);
        if (runtimeDrawerView instanceof android.widget.ScrollView) {
            runtimeDrawerView.scrollTo(0, 0);
            runtimeDrawerView.post(() -> {
                if (runtimeDrawerVisible && runtimeDrawerView != null) {
                    runtimeDrawerView.scrollTo(0, 0);
                    runtimeDrawerView.requestLayout();
                }
            });
        }
        runtimeDrawerScrim.animate().cancel();
        runtimeDrawerView.animate().cancel();
        runtimeDrawerScrim.setAlpha(0f);
        runtimeDrawerView.setTranslationX(getRuntimeDrawerHiddenOffset());
        runtimeDrawerScrim.animate().alpha(1f).setDuration(160L).start();
        runtimeDrawerView.animate().translationX(0f).setDuration(220L).start();
    }

    private void hideRuntimeDrawer() {
        if (!runtimeDrawerVisible || runtimeDrawerView == null || runtimeDrawerScrim == null) return;
        runtimeDrawerVisible = false;
        final float hiddenOffset = getRuntimeDrawerHiddenOffset();
        runtimeDrawerScrim.animate().cancel();
        runtimeDrawerView.animate().cancel();
        runtimeDrawerScrim.animate().alpha(0f).setDuration(140L).withEndAction(() -> {
            if (!runtimeDrawerVisible && runtimeDrawerScrim != null) runtimeDrawerScrim.setVisibility(View.GONE);
        }).start();
        runtimeDrawerView.animate().translationX(hiddenOffset).setDuration(200L).withEndAction(() -> {
            if (!runtimeDrawerVisible && runtimeDrawerView != null) runtimeDrawerView.setVisibility(View.GONE);
        }).start();
    }

    private float getRuntimeDrawerHiddenOffset() {
        int width = runtimeDrawerView != null ? runtimeDrawerView.getWidth() : 0;
        if (width <= 0) width = Math.round(getResources().getDisplayMetrics().density * 360f);
        return -width - Math.round(getResources().getDisplayMetrics().density * 24f);
    }

    private void applyRuntimeThemeAssetPass() {
        if (xserverRootView != null) ThemeAssetPainter.apply(this, xserverRootView, isDarkMode);
    }

    private static String trimToEmpty(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            String normalized = trimToEmpty(value);
            if (!normalized.isEmpty()) return normalized;
        }
        return "";
    }

    private String joinNonEmptyCsv(String... values) {
        if (values == null || values.length == 0) return "";
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            String normalized = trimToEmpty(value);
            if (normalized.isEmpty()) continue;
            if (builder.length() > 0) builder.append(',');
            builder.append(normalized);
        }
        return builder.toString();
    }

    private String humanizeGraphicsLane(String lane) {
        String normalized = trimToEmpty(lane).toLowerCase(Locale.US);
        return switch (normalized) {
            case "aemali-panvk" -> "AeMali PanVK";
            case "aemali-gallium" -> "AeMali Gallium";
            case "turnip-vulkan" -> "Turnip Vulkan";
            case "freedreno-opengl" -> "Mesa OpenGL";
            case "zink-opengl" -> "Mesa OpenGL";
            case "virgl-universal" -> "VirGL Universal";
            case "gladio-opengl" -> "Gladio OpenGL";
            case "vortek-wrapper-vulkan" -> "Vortek Vulkan";
            case "system-graphics" -> "System Graphics";
            case "wrapper" -> "Wrapper";
            case "system" -> "System";
            case "" -> getString(R.string.not_set);
            default -> {
                String[] tokens = normalized.replace('_', ' ').replace('-', ' ').split("\\s+");
                StringBuilder builder = new StringBuilder();
                for (String token : tokens) {
                    if (token.isEmpty()) continue;
                    if (builder.length() > 0) builder.append(' ');
                    builder.append(Character.toUpperCase(token.charAt(0)));
                    if (token.length() > 1) builder.append(token.substring(1));
                }
                yield builder.length() > 0 ? builder.toString() : getString(R.string.not_set);
            }
        };
    }

    private String humanizeDxWrapper(String wrapper) {
        String normalized = trimToEmpty(wrapper).toLowerCase(Locale.US);
        if (normalized.contains("dxvk")) return "DXVK+VKD3D";
        if (normalized.contains("dgvoodoo")) return "dgVoodoo";
        if (normalized.contains("wine")) return "WineD3D";
        return normalized.isEmpty() ? getString(R.string.not_set) : humanizeGraphicsLane(normalized);
    }

    private boolean supportsDgVoodooVulkanBridge(String rawGraphicsDriver) {
        String normalized = Container.normalizeGraphicsDriver(rawGraphicsDriver);
        return GraphicsDrivers.isWrapper(normalized)
                || GraphicsDrivers.isVortek(normalized)
                || GraphicsDrivers.isGladio(normalized)
                || GraphicsDrivers.isVirgl(normalized)
                || GraphicsDrivers.isAeMaliGallium(normalized);
    }

    private void logNoexecDosDriveState(@Nullable File rootDir) {
        if (container == null || rootDir == null) return;
        StringBuilder noexecDrives = new StringBuilder();
        for (String[] drive : container.drivesIterator()) {
            if (drive == null || drive.length < 2) continue;
            String letter = trimToEmpty(drive[0]).toUpperCase(Locale.US);
            String hostPath = trimToEmpty(drive[1]);
            if (letter.isEmpty() || hostPath.isEmpty()) continue;
            if (!shouldMirrorNoexecLaunchTarget(rootDir, new File(hostPath))) continue;
            if (noexecDrives.length() > 0) noexecDrives.append(',');
            noexecDrives.append(letter).append(':').append(hostPath);
        }
        String value = noexecDrives.toString();
        setOrClearEnv("AERO_NOEXEC_DOS_DRIVES", value);
        if (value.isEmpty()) return;
        ForensicLogger.logEvent(
                this,
                "info",
                "NOEXEC_DOS_DRIVE_DETECTED",
                null,
                "xserver",
                "noexec_dos_drive_detected",
                ForensicLogger.fields(
                        "noexec_dos_drives", value,
                        "effect", "wine_pe_mmap_from_this_drive_can_fail_without_exec_safe_mirror"
                )
        );
    }

    private String resolveDxWrapperStatusText() {
        if (!trimToEmpty(dxwrapper).toLowerCase(Locale.US).contains("dgvoodoo")) {
            return humanizeDxWrapper(dxwrapper);
        }

        String arch = firstNonEmpty(
                envVars.get("AERO_DGVOODOO_PACKAGE_LANE"),
                envVars.get("AERO_DGVOODOO_ARCH_ACTIVE"),
                envVars.get("AERO_DGVOODOO_ARCH_REQUESTED"),
                getString(R.string.not_set)
        );
        String route = firstNonEmpty(
                envVars.get("AERO_DGVOODOO_ROUTE_STATE"),
                "1".equals(trimToEmpty(envVars.get("AERO_DGVOODOO_STAGE_READY"))) ? "dgvoodoo" : "wined3d-fallback"
        );
        String forceD3d11 = "1".equals(trimToEmpty(envVars.get("AERO_DGVOODOO_FORCE_D3D11"))) ? "on" : "off";
        return getString(R.string.runtime_graphics_status_dxwrapper_dgvoodoo, arch, route, forceD3d11);
    }

    private String humanizeAudioDriver(String driver) {
        String normalized = trimToEmpty(driver).toLowerCase(Locale.US);
        if ("alsa".equals(normalized)) return "ALSA";
        if (normalized.contains("pulse")) return "PulseAudio";
        return normalized.isEmpty() ? getString(R.string.not_set) : humanizeGraphicsLane(normalized);
    }

    private String humanizeUpscalerBackend(String backend) {
        String normalized = trimToEmpty(backend).toLowerCase(Locale.US);
        return switch (normalized) {
            case "lsfg" -> "LSFG";
            case "vkbasalt" -> "vkBasalt";
            case "", "off" -> getString(R.string.runtime_graphics_status_framegen_off);
            default -> humanizeGraphicsLane(normalized);
        };
    }

    private String humanizeFramegenMode(String mode) {
        String normalized = trimToEmpty(mode).toLowerCase(Locale.US);
        if (normalized.isEmpty()) return "auto";
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private String buildProviderSummary(String lane, String packageName, String version) {
        String label = humanizeGraphicsLane(lane);
        String pkg = trimToEmpty(packageName);
        String ver = trimToEmpty(version);
        if (pkg.isEmpty() && ver.isEmpty()) return label;
        if (pkg.isEmpty()) return label + " | " + ver;
        if (ver.isEmpty()) return label + " | " + pkg;
        if (pkg.equalsIgnoreCase(ver)) return label + " | " + pkg;
        return label + " | " + pkg + " • " + ver;
    }

    private String resolveConfiguredGraphicsSelection() {
        String configuredVersion = GraphicsDrivers.getDisplayVersion(this, graphicsDriver, rawGraphicsDriverConfig);
        return firstNonEmpty(
                envVars.get("AERO_GRAPHICS_SELECTED_DRIVER_PACKAGE"),
                envVars.get("AERO_GRAPHICS_SELECTED_DRIVER_ENTRY"),
                configuredVersion,
                graphicsDriver
        );
    }

    private String resolveRuntimeGraphicsPrimaryText() {
        String lane = firstNonEmpty(envVars.get("AERO_GRAPHICS_ACTIVE_PROVIDER_LANE"), graphicsDriver);
        String packageName = firstNonEmpty(
                envVars.get("AERO_GRAPHICS_ACTIVE_PROVIDER_PACKAGE"),
                envVars.get("AERO_GRAPHICS_SELECTED_DRIVER_PACKAGE"),
                resolveConfiguredGraphicsSelection()
        );
        String version = firstNonEmpty(
                envVars.get("AERO_GRAPHICS_ACTIVE_PROVIDER_VERSION"),
                GraphicsDrivers.getDisplayVersion(this, graphicsDriver, rawGraphicsDriverConfig)
        );
        String summary = buildProviderSummary(lane, packageName, version);
        return summary.equals(getString(R.string.not_set)) ? getString(R.string.runtime_graphics_status_unresolved) : summary;
    }

    private String resolveRuntimeGraphicsSecondaryText() {
        String companion = buildProviderSummary(
                firstNonEmpty(envVars.get("AERO_GRAPHICS_COMPANION_PROVIDER_LANE"), envVars.get("AERO_GRAPHICS_OPENGL_PROVIDER")),
                firstNonEmpty(envVars.get("AERO_GRAPHICS_COMPANION_PROVIDER_PACKAGE"), envVars.get("AERO_OPENGL_PACKAGE")),
                firstNonEmpty(envVars.get("AERO_GRAPHICS_COMPANION_PROVIDER_VERSION"), envVars.get("AERO_OPENGL_VERSION"))
        );
        return getString(
                R.string.runtime_graphics_status_secondary,
                companion,
                resolveDxWrapperStatusText(),
                humanizeAudioDriver(audioDriver)
        );
    }

    private String resolveRuntimeGraphicsTertiaryText() {
        String framegen = upscalerFrameGeneration && !UPSCALER_BACKEND_OFF.equalsIgnoreCase(upscalerBackend)
                ? getString(
                R.string.runtime_graphics_status_framegen_on,
                humanizeUpscalerBackend(upscalerBackend),
                humanizeFramegenMode(upscalerFramegenMode)
        )
                : getString(R.string.runtime_graphics_status_framegen_off);
        String vulkanApi = firstNonEmpty(
                envVars.get("AERO_VULKAN_API_SELECTED"),
                envVars.get("WRAPPER_VK_VERSION"),
                graphicsDriverConfig != null ? graphicsDriverConfig.get("vulkanVersion") : ""
        );
        if (vulkanApi.isEmpty()) vulkanApi = getString(R.string.not_set);
        String selection = resolveConfiguredGraphicsSelection();
        String legacyRequestedDriver = trimToEmpty(envVars.get("AERO_GRAPHICS_LEGACY_REQUESTED_DRIVER"));
        String legacyPolicy = trimToEmpty(envVars.get("AERO_GRAPHICS_LEGACY_POLICY"));
        if (!legacyRequestedDriver.isEmpty()) {
            String legacyLabel = humanizeGraphicsLane(legacyRequestedDriver);
            String legacyPrefix = "route-degraded".equals(legacyPolicy) ? "requested " : "compat ";
            selection = selection.isEmpty() ? legacyPrefix + legacyLabel : selection + " | " + legacyPrefix + legacyLabel;
        }
        if (selection.isEmpty()) selection = getString(R.string.not_set);
        return getString(R.string.runtime_graphics_status_tertiary, framegen, vulkanApi, selection);
    }

    private void applyRuntimeGraphicsStatusCardStyle() {
        View card = findViewById(R.id.LLRuntimeGraphicsStatusCard);
        if (card == null) return;

        card.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
        int textColor = ContextCompat.getColor(this, R.color.surface_runtime_taskmgr_text);
        int[] textIds = new int[] {
                R.id.TVRuntimeGraphicsStatusLabel,
                R.id.TVRuntimeGraphicsPrimary,
                R.id.TVRuntimeGraphicsSecondary,
                R.id.TVRuntimeGraphicsTertiary
        };
        for (int id : textIds) {
            TextView textView = findViewById(id);
            if (textView != null) textView.setTextColor(textColor);
        }
    }

    private void applyRuntimeDrawerSurfaceStyle() {
        if (runtimeDrawerView != null) {
            runtimeDrawerView.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
        }

        View headerCard = findViewById(R.id.LLRuntimeDrawerHeaderCard);
        if (headerCard != null) {
            headerCard.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
        }
        View closeButton = findViewById(R.id.BTCloseRuntimeDrawer);
        if (closeButton != null) {
            closeButton.setBackgroundResource(R.drawable.surface_runtime_button_neutral);
        }

        int brightText = ContextCompat.getColor(this, R.color.surface_table_head_text);
        int mutedText = ContextCompat.getColor(this, R.color.surface_runtime_taskmgr_muted);
        int accentText = ContextCompat.getColor(this, R.color.surface_button_positive_text);
        int[] headerTextIds = new int[] {
                R.id.TVRuntimeDrawerContainerName,
                R.id.TVRuntimeDrawerShortcutName,
                R.id.TVRuntimeDrawerRoute,
                R.id.TVRuntimeDrawerHint
        };
        for (int id : headerTextIds) {
            TextView textView = findViewById(id);
            if (textView != null) textView.setTextColor(brightText);
        }
        android.widget.ImageButton headerClose = findViewById(R.id.BTCloseRuntimeDrawer);
        if (headerClose != null) headerClose.setColorFilter(accentText);
        TextView headerBadge = findViewById(R.id.TVRuntimeDrawerHeaderBadge);
        if (headerBadge != null) {
            headerBadge.setBackgroundResource(R.drawable.surface_runtime_taskmgr_badge_background);
            headerBadge.setTextColor(brightText);
        }

        int[] rowIds = new int[] {
                R.id.LLRuntimeActionKeyboard,
                R.id.LLRuntimeActionInputControls,
                R.id.LLRuntimeActionRelativeMouse,
                R.id.LLRuntimeActionScreenEffects,
                R.id.LLRuntimeActionFullscreen,
                R.id.LLRuntimeActionPauseResume,
                R.id.LLRuntimeActionPip,
                R.id.LLRuntimeActionTaskManager,
                R.id.LLRuntimeActionActiveWindows,
                R.id.LLRuntimeActionMagnifier,
                R.id.LLRuntimeActionLogs,
                R.id.LLRuntimeActionPrefixPack,
                R.id.LLRuntimeActionRuntimeProfiles,
                R.id.LLRuntimeActionExit
        };
        for (int id : rowIds) {
            View row = findViewById(id);
            if (row != null) row.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
        }

        int[] rowTitleIds = new int[] {
                R.id.TVRuntimeActionKeyboardTitle,
                R.id.TVRuntimeActionInputControlsTitle,
                R.id.TVRuntimeActionRelativeMouseTitle,
                R.id.TVRuntimeActionScreenEffectsTitle,
                R.id.TVRuntimeActionFullscreenTitle,
                R.id.TVRuntimeActionPauseResumeTitle,
                R.id.TVRuntimeActionPipTitle,
                R.id.TVRuntimeActionTaskManagerTitle,
                R.id.TVRuntimeActionActiveWindowsTitle,
                R.id.TVRuntimeActionMagnifierTitle,
                R.id.TVRuntimeActionLogsTitle,
                R.id.TVRuntimeActionPrefixPackTitle,
                R.id.TVRuntimeActionRuntimeProfilesTitle,
                R.id.TVRuntimeActionExitTitle
        };
        for (int id : rowTitleIds) {
            TextView textView = findViewById(id);
            if (textView != null) textView.setTextColor(brightText);
        }
        int[] rowSummaryIds = new int[] {
                R.id.TVRuntimeActionKeyboardSummary,
                R.id.TVRuntimeActionInputControlsSummary,
                R.id.TVRuntimeActionRelativeMouseSummary,
                R.id.TVRuntimeActionScreenEffectsSummary,
                R.id.TVRuntimeActionFullscreenSummary,
                R.id.TVRuntimeActionPauseResumeSummary,
                R.id.TVRuntimeActionPipSummary,
                R.id.TVRuntimeActionTaskManagerSummary,
                R.id.TVRuntimeActionActiveWindowsSummary,
                R.id.TVRuntimeActionMagnifierSummary,
                R.id.TVRuntimeActionLogsSummary,
                R.id.TVRuntimeActionPrefixPackSummary,
                R.id.TVRuntimeActionRuntimeProfilesSummary,
                R.id.TVRuntimeActionExitSummary
        };
        for (int id : rowSummaryIds) {
            TextView textView = findViewById(id);
            if (textView != null) textView.setTextColor(mutedText);
        }

        int[] iconIds = new int[] {
                R.id.IVRuntimeActionKeyboard,
                R.id.IVRuntimeActionInputControls,
                R.id.IVRuntimeActionRelativeMouse,
                R.id.IVRuntimeActionScreenEffects,
                R.id.IVRuntimeActionFullscreen,
                R.id.IVRuntimeActionPauseResume,
                R.id.IVRuntimeActionPip,
                R.id.IVRuntimeActionTaskManager,
                R.id.IVRuntimeActionActiveWindows,
                R.id.IVRuntimeActionMagnifier,
                R.id.IVRuntimeActionLogs,
                R.id.IVRuntimeActionPrefixPack,
                R.id.IVRuntimeActionRuntimeProfiles,
                R.id.IVRuntimeActionExit
        };
        for (int id : iconIds) {
            android.widget.ImageView icon = findViewById(id);
            if (icon != null) icon.setColorFilter(brightText);
        }

        styleRuntimeDrawerActionHighlight(
                R.id.LLRuntimeActionInputControls,
                new int[] {R.id.TVRuntimeActionInputControlsTitle, R.id.TVRuntimeActionInputControlsSummary},
                R.id.IVRuntimeActionInputControls,
                R.drawable.surface_runtime_taskmgr_background,
                ContextCompat.getColor(this, R.color.surface_runtime_taskmgr_text)
        );
        styleRuntimeDrawerActionHighlight(
                R.id.LLRuntimeActionScreenEffects,
                new int[] {R.id.TVRuntimeActionScreenEffectsTitle, R.id.TVRuntimeActionScreenEffectsSummary},
                R.id.IVRuntimeActionScreenEffects,
                R.drawable.surface_runtime_taskmgr_background,
                ContextCompat.getColor(this, R.color.surface_runtime_taskmgr_text)
        );
        styleRuntimeDrawerActionHighlight(
                R.id.LLRuntimeActionTaskManager,
                new int[] {R.id.TVRuntimeActionTaskManagerTitle, R.id.TVRuntimeActionTaskManagerSummary},
                R.id.IVRuntimeActionTaskManager,
                R.drawable.surface_runtime_taskmgr_background,
                ContextCompat.getColor(this, R.color.surface_runtime_taskmgr_text)
        );
        styleRuntimeDrawerActionHighlight(
                R.id.LLRuntimeActionActiveWindows,
                new int[] {R.id.TVRuntimeActionActiveWindowsTitle, R.id.TVRuntimeActionActiveWindowsSummary},
                R.id.IVRuntimeActionActiveWindows,
                R.drawable.surface_runtime_taskmgr_background,
                ContextCompat.getColor(this, R.color.surface_runtime_taskmgr_text)
        );
        styleRuntimeDrawerActionHighlight(
                R.id.LLRuntimeActionPrefixPack,
                new int[] {R.id.TVRuntimeActionPrefixPackTitle, R.id.TVRuntimeActionPrefixPackSummary},
                R.id.IVRuntimeActionPrefixPack,
                R.drawable.surface_runtime_taskmgr_background,
                ContextCompat.getColor(this, R.color.surface_runtime_taskmgr_text)
        );
        styleRuntimeDrawerActionHighlight(
                R.id.LLRuntimeActionRuntimeProfiles,
                new int[] {R.id.TVRuntimeActionRuntimeProfilesTitle, R.id.TVRuntimeActionRuntimeProfilesSummary},
                R.id.IVRuntimeActionRuntimeProfiles,
                R.drawable.surface_runtime_taskmgr_background,
                ContextCompat.getColor(this, R.color.surface_runtime_taskmgr_text)
        );
        styleRuntimeDrawerActionHighlight(
                R.id.LLRuntimeActionLogs,
                new int[] {R.id.TVRuntimeActionLogsTitle, R.id.TVRuntimeActionLogsSummary},
                R.id.IVRuntimeActionLogs,
                isDarkMode ? R.drawable.surface_table_head_background_dark : R.drawable.surface_table_head_background,
                ContextCompat.getColor(this, R.color.surface_table_head_text)
        );
        styleRuntimeDrawerActionHighlight(
                R.id.LLRuntimeActionExit,
                new int[] {R.id.TVRuntimeActionExitTitle, R.id.TVRuntimeActionExitSummary},
                R.id.IVRuntimeActionExit,
                isDarkMode ? R.drawable.surface_table_head_background_dark : R.drawable.surface_table_head_background,
                ContextCompat.getColor(this, R.color.surface_table_head_text)
        );
    }

    private void styleRuntimeDrawerActionHighlight(int rowId, int[] textIds, int iconId, int backgroundRes, int textColor) {
        View row = findViewById(rowId);
        if (row != null) row.setBackgroundResource(backgroundRes);
        if (textIds != null) {
            for (int id : textIds) {
                TextView textView = findViewById(id);
                if (textView != null) textView.setTextColor(textColor);
            }
        }
        android.widget.ImageView icon = findViewById(iconId);
        if (icon != null) icon.setColorFilter(textColor);
    }

    private void styleRuntimeNestedDialog(ContentDialog dialog) {
        if (dialog == null) return;
        int brightText = ContextCompat.getColor(this, R.color.surface_runtime_taskmgr_text);
        int mutedText = ContextCompat.getColor(this, R.color.surface_runtime_taskmgr_muted);

        View root = dialog.getContentView();
        if (root != null) {
            root.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
            root.setPadding(dp(7), dp(5), dp(7), dp(5));
        }

        View frameLayout = dialog.findViewById(R.id.FrameLayout);
        if (frameLayout != null) frameLayout.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);

        View titleBar = dialog.findViewById(R.id.LLTitleBar);
        if (titleBar != null) {
            titleBar.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
            titleBar.setPadding(dp(7), dp(5), dp(7), 0);
        }

        View bottomBar = dialog.findViewById(R.id.LLBottomBar);
        if (bottomBar != null) {
            bottomBar.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
            bottomBar.setPadding(0, 0, 0, 0);
        }

        TextView titleView = dialog.findViewById(R.id.TVTitle);
        if (titleView != null) titleView.setTextColor(brightText);

        TextView messageView = dialog.findViewById(R.id.TVMessage);
        if (messageView != null) messageView.setTextColor(brightText);

        android.widget.ImageView iconView = dialog.findViewById(R.id.IVIcon);
        if (iconView != null) iconView.setColorFilter(brightText);

        View titleBackButton = dialog.findViewById(R.id.BTTitleBack);
        if (titleBackButton instanceof android.widget.ImageButton) {
            ((android.widget.ImageButton) titleBackButton).setColorFilter(brightText);
            titleBackButton.setBackgroundResource(R.drawable.surface_runtime_button_neutral);
            ViewGroup.LayoutParams layoutParams = titleBackButton.getLayoutParams();
            if (layoutParams != null) {
                layoutParams.width = dp(36);
                layoutParams.height = dp(36);
                titleBackButton.setLayoutParams(layoutParams);
            }
        }

        android.widget.Button confirmButton = dialog.findViewById(R.id.BTConfirm);
        if (confirmButton != null) {
            confirmButton.setBackgroundResource(R.drawable.surface_runtime_button_positive);
            confirmButton.setTextColor(ContextCompat.getColor(this, R.color.surface_runtime_button_positive_text));
            ViewGroup.LayoutParams layoutParams = confirmButton.getLayoutParams();
            if (layoutParams != null) {
                layoutParams.height = dp(34);
                confirmButton.setLayoutParams(layoutParams);
            }
        }
        android.widget.Button cancelButton = dialog.findViewById(R.id.BTCancel);
        if (cancelButton != null && cancelButton.getVisibility() == View.VISIBLE) {
            cancelButton.setBackgroundResource(R.drawable.surface_runtime_button_neutral);
            cancelButton.setTextColor(ContextCompat.getColor(this, R.color.surface_runtime_button_text));
            ViewGroup.LayoutParams layoutParams = cancelButton.getLayoutParams();
            if (layoutParams != null) {
                layoutParams.height = dp(34);
                cancelButton.setLayoutParams(layoutParams);
            }
        }

        tintRuntimeDialogTree(dialog.getInflatedLayout() != null ? dialog.getInflatedLayout() : root, brightText, mutedText);
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        ));
    }

    private boolean hasRuntimeTagToken(@Nullable View view, String token) {
        if (view == null || token == null || token.trim().isEmpty()) return false;
        Object tag = view.getTag();
        if (!(tag instanceof String)) return false;
        String[] tokens = ((String) tag).trim().split("[\\s,;|]+");
        for (String candidate : tokens) {
            if (token.equals(candidate)) return true;
        }
        return false;
    }

    private void tintRuntimeDialogTree(@Nullable View view, int brightText, int mutedText) {
        if (view == null) return;

        boolean preserveBackground = hasRuntimeTagToken(view, "preserve_background");
        boolean themeCard = hasRuntimeTagToken(view, "theme_card");
        boolean themeBadge = hasRuntimeTagToken(view, "theme_badge");
        if (themeCard && !preserveBackground) {
            view.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
        } else if (themeBadge && view instanceof TextView) {
            if (!preserveBackground) {
                view.setBackgroundResource(R.drawable.surface_runtime_taskmgr_badge_background);
            }
            ((TextView) view).setTextColor(brightText);
        }

        if (view instanceof CheckBox) {
            CheckBox checkBox = (CheckBox) view;
            checkBox.setTextColor(brightText);
            androidx.core.widget.CompoundButtonCompat.setButtonTintList(checkBox, ColorStateList.valueOf(brightText));
        } else if (view instanceof Spinner spinner) {
            SpinnerAdapters.applyRuntimeSurface(spinner);
        } else if (view instanceof TextView) {
            ((TextView) view).setTextColor(brightText);
        } else if (view instanceof android.widget.Button) {
            android.widget.Button button = (android.widget.Button) view;
            int id = button.getId();
            if (id == R.id.BTConfirm || id == R.id.BTReset) {
                button.setBackgroundResource(R.drawable.surface_runtime_button_positive);
                button.setTextColor(ContextCompat.getColor(this, R.color.surface_runtime_button_positive_text));
            } else {
                button.setBackgroundResource(R.drawable.surface_runtime_button_neutral);
                button.setTextColor(ContextCompat.getColor(this, R.color.surface_runtime_button_text));
            }
        } else if (view instanceof android.widget.ImageButton) {
            android.widget.ImageButton imageButton = (android.widget.ImageButton) view;
            imageButton.setBackgroundResource(R.drawable.surface_runtime_button_neutral);
            imageButton.setColorFilter(brightText);
        } else if (view instanceof android.widget.EditText) {
            android.widget.EditText editText = (android.widget.EditText) view;
            editText.setTextColor(brightText);
            editText.setHintTextColor(mutedText);
            editText.setBackgroundResource(R.drawable.surface_runtime_taskmgr_input_background);
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                tintRuntimeDialogTree(group.getChildAt(i), brightText, mutedText);
            }
        }
    }

    private void showPrefixPackGuide() {
        String autoInstallTarget = debugAutoInstallPrefixPackTarget;
        debugAutoInstallPrefixPackTarget = "";
        new PrefixPackToolkitDialog(this, autoInstallTarget).show();
    }

    private String resolveBox64PresetDisplayName(String presetId) {
        Box64Preset preset = Box64PresetManager.getPreset("box64", this, presetId);
        if (preset != null && preset.name != null && !preset.name.trim().isEmpty()) {
            return preset.name.trim();
        }
        return presetId == null || presetId.trim().isEmpty() ? getString(R.string.not_set) : presetId.trim();
    }

    private String resolveFexPresetDisplayName(String presetId) {
        FEXCorePreset preset = FEXCorePresetManager.getPreset(this, presetId);
        if (preset != null && preset.name != null && !preset.name.trim().isEmpty()) {
            return preset.name.trim();
        }
        return presetId == null || presetId.trim().isEmpty() ? getString(R.string.not_set) : presetId.trim();
    }

    private String resolveRuntimeProfilesDrawerSummary() {
        if (shortcut != null) {
            return getString(R.string.runtime_drawer_runtime_profiles_unavailable);
        }
        boolean fexPrimary = isRuntimeProfilesFexPrimary();
        if (fexPrimary) {
            return getString(
                    R.string.runtime_profile_current_route_fex,
                    firstNonEmpty(container != null ? container.getFEXCoreVersion() : "", DefaultVersion.FEXCORE),
                    resolveFexPresetDisplayName(container != null ? container.getFEXCorePreset() : FEXCorePreset.INTERMEDIATE)
            );
        }
        return getString(
                R.string.runtime_profile_current_route_box,
                firstNonEmpty(container != null ? container.getBox64Version() : "", DefaultVersion.BOX64),
                resolveBox64PresetDisplayName(container != null ? container.getBox64Preset() : Box64Preset.COMPATIBILITY)
        );
    }

    private WineInfo resolveCurrentRuntimeWineInfo() {
        if (wineInfo != null) return wineInfo;
        if (container == null || contentsManager == null) return null;
        try {
            return WineInfo.fromIdentifier(
                    this,
                    contentsManager,
                    container.getWineVersion(),
                    ContentProfile.inferRuntimeModelFromEntryName(container.getWineVersion())
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isRuntimeProfilesFexPrimary() {
        WineInfo activeWineInfo = resolveCurrentRuntimeWineInfo();
        return activeWineInfo == null || activeWineInfo.isArm64EC();
    }

    private void promoteRuntimeStringSpinner(@Nullable Spinner spinner) {
        if (spinner == null) return;
        SpinnerAdapter currentAdapter = spinner.getAdapter();
        if (currentAdapter == null) {
            SpinnerAdapters.applyRuntimeSurface(spinner);
            return;
        }
        int selection = spinner.getSelectedItemPosition();
        ArrayList<String> items = new ArrayList<>();
        for (int i = 0; i < currentAdapter.getCount(); i++) {
            Object item = currentAdapter.getItem(i);
            items.add(item != null ? item.toString() : "");
        }
        spinner.setAdapter(SpinnerAdapters.createRuntime(this, items));
        if (!items.isEmpty()) {
            spinner.setSelection(Math.max(0, Math.min(selection, items.size() - 1)), false);
        }
        SpinnerAdapters.applyRuntimeSurface(spinner);
    }

    private void loadRuntimeBox64PresetSpinner(@Nullable Spinner spinner, @Nullable String selectedId) {
        if (spinner == null) return;
        ArrayList<Box64Preset> presets = Box64PresetManager.getPresets("box64", this);
        int selectedPosition = 0;
        String normalizedSelectedId = selectedId != null ? selectedId : Box64Preset.COMPATIBILITY;
        for (int i = 0; i < presets.size(); i++) {
            if (normalizedSelectedId.equals(presets.get(i).id)) {
                selectedPosition = i;
                break;
            }
        }
        spinner.setAdapter(SpinnerAdapters.createRuntimeGeneric(this, presets));
        spinner.setSelection(selectedPosition, false);
        SpinnerAdapters.applyRuntimeSurface(spinner);
    }

    private void loadRuntimeFexPresetSpinner(@Nullable Spinner spinner, @Nullable String selectedId) {
        if (spinner == null) return;
        ArrayList<FEXCorePreset> presets = FEXCorePresetManager.getPresets(this);
        int selectedPosition = 0;
        String normalizedSelectedId = selectedId != null ? selectedId : FEXCorePreset.INTERMEDIATE;
        for (int i = 0; i < presets.size(); i++) {
            if (normalizedSelectedId.equals(presets.get(i).id)) {
                selectedPosition = i;
                break;
            }
        }
        spinner.setAdapter(SpinnerAdapters.createRuntimeGeneric(this, presets));
        spinner.setSelection(selectedPosition, false);
        SpinnerAdapters.applyRuntimeSurface(spinner);
    }

    private void showRuntimeProfilesDialog() {
        if (container == null) return;
        if (shortcut != null) {
            showToast(this, R.string.runtime_drawer_runtime_profiles_unavailable);
            return;
        }

        ContentDialog dialog = new ContentDialog(this, R.layout.runtime_emulator_profile_dialog);
        dialog.setTitle(R.string.runtime_profiles);
        dialog.setIcon(R.drawable.ae_icon_env_var);

        boolean fexPrimary = isRuntimeProfilesFexPrimary();
        Spinner box64VersionSpinner = dialog.findViewById(R.id.SRuntimeProfileBox64Version);
        Spinner box64PresetSpinner = dialog.findViewById(R.id.SRuntimeProfileBox64Preset);
        Spinner fexVersionSpinner = dialog.findViewById(R.id.SRuntimeProfileFexVersion);
        Spinner fexPresetSpinner = dialog.findViewById(R.id.SRuntimeProfileFexPreset);
        TextView currentRouteView = dialog.findViewById(R.id.TVRuntimeProfileCurrent);
        TextView modeBadgeView = dialog.findViewById(R.id.TVRuntimeProfileModeBadge);
        TextView modeSummaryView = dialog.findViewById(R.id.TVRuntimeProfileModeSummary);
        View fexCard = dialog.findViewById(R.id.LLRuntimeProfileFexCard);
        View box64Card = dialog.findViewById(R.id.LLRuntimeProfileBox64Card);

        if (fexCard != null) fexCard.setVisibility(fexPrimary ? View.VISIBLE : View.GONE);
        if (box64Card != null) box64Card.setVisibility(fexPrimary ? View.GONE : View.VISIBLE);

        if (box64VersionSpinner != null) {
            ContainerDetailFragment.loadBox64VersionSpinner(this, container, contentsManager, box64VersionSpinner, false);
            promoteRuntimeStringSpinner(box64VersionSpinner);
        }
        if (box64PresetSpinner != null) {
            loadRuntimeBox64PresetSpinner(box64PresetSpinner, container.getBox64Preset());
        }
        if (fexVersionSpinner != null) {
            FEXCoreManager.loadFEXCoreVersion(this, contentsManager, fexVersionSpinner, container.getFEXCoreVersion());
            promoteRuntimeStringSpinner(fexVersionSpinner);
        }
        if (fexPresetSpinner != null) {
            loadRuntimeFexPresetSpinner(fexPresetSpinner, container.getFEXCorePreset());
        }

        Runnable refreshCurrentRoute = () -> {
            if (currentRouteView == null) return;
            String selectedBox64Version = box64VersionSpinner != null && box64VersionSpinner.getSelectedItem() != null
                    ? box64VersionSpinner.getSelectedItem().toString()
                    : container.getBox64Version();
            String selectedBox64Preset = box64PresetSpinner != null
                    ? Box64PresetManager.getSpinnerSelectedId(box64PresetSpinner)
                    : container.getBox64Preset();
            String selectedFexVersion = fexVersionSpinner != null && fexVersionSpinner.getSelectedItem() != null
                    ? fexVersionSpinner.getSelectedItem().toString()
                    : container.getFEXCoreVersion();
            String selectedFexPreset = fexPresetSpinner != null
                    ? FEXCorePresetManager.getSpinnerSelectedId(fexPresetSpinner)
                    : container.getFEXCorePreset();
            if (fexPrimary) {
                currentRouteView.setText(getString(
                        R.string.runtime_profile_current_route_fex,
                        firstNonEmpty(selectedFexVersion, DefaultVersion.FEXCORE),
                        resolveFexPresetDisplayName(selectedFexPreset)
                ));
            } else {
                currentRouteView.setText(getString(
                        R.string.runtime_profile_current_route_box,
                        firstNonEmpty(selectedBox64Version, DefaultVersion.BOX64),
                        resolveBox64PresetDisplayName(selectedBox64Preset)
                ));
            }
        };
        refreshCurrentRoute.run();

        if (modeBadgeView != null) {
            modeBadgeView.setText(fexPrimary
                    ? R.string.runtime_profile_route_fex_badge
                    : R.string.runtime_profile_route_box_badge);
            modeBadgeView.setTextColor(ContextCompat.getColor(this, R.color.surface_runtime_taskmgr_text));
            modeBadgeView.setBackgroundResource(R.drawable.surface_runtime_taskmgr_header_background);
        }
        if (modeSummaryView != null) {
            modeSummaryView.setText(fexPrimary
                    ? R.string.runtime_profile_fex_summary
                    : R.string.runtime_profile_box64_summary);
            modeSummaryView.setTextColor(ContextCompat.getColor(this, R.color.surface_runtime_taskmgr_muted));
        }

        AdapterView.OnItemSelectedListener refreshListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshCurrentRoute.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                refreshCurrentRoute.run();
            }
        };
        if (box64VersionSpinner != null) box64VersionSpinner.setOnItemSelectedListener(refreshListener);
        if (box64PresetSpinner != null) box64PresetSpinner.setOnItemSelectedListener(refreshListener);
        if (fexVersionSpinner != null) fexVersionSpinner.setOnItemSelectedListener(refreshListener);
        if (fexPresetSpinner != null) fexPresetSpinner.setOnItemSelectedListener(refreshListener);

        TextView runtimeHintView = dialog.findViewById(R.id.TVRuntimeProfileHint);
        if (runtimeHintView != null) {
            runtimeHintView.setTextColor(ContextCompat.getColor(this, R.color.surface_runtime_taskmgr_muted));
        }

        View box64AddButton = dialog.findViewById(R.id.BTRuntimeProfileBox64Add);
        if (box64AddButton instanceof ImageButton) {
            box64AddButton.setOnClickListener(v -> {
                Box64EditPresetDialog editDialog = new Box64EditPresetDialog(this, "box64", null);
                editDialog.setOnConfirmCallback(() -> {
                    if (box64PresetSpinner != null) {
                        loadRuntimeBox64PresetSpinner(box64PresetSpinner, container.getBox64Preset());
                    }
                    refreshCurrentRoute.run();
                });
                editDialog.show();
                styleRuntimeNestedDialog(editDialog);
            });
        }
        View box64EditButton = dialog.findViewById(R.id.BTRuntimeProfileBox64Edit);
        if (box64EditButton instanceof ImageButton) {
            box64EditButton.setOnClickListener(v -> {
                String selectedId = box64PresetSpinner != null ? Box64PresetManager.getSpinnerSelectedId(box64PresetSpinner) : container.getBox64Preset();
                Box64EditPresetDialog editDialog = new Box64EditPresetDialog(this, "box64", selectedId);
                editDialog.setOnConfirmCallback(() -> {
                    if (box64PresetSpinner != null) {
                        loadRuntimeBox64PresetSpinner(box64PresetSpinner, selectedId);
                    }
                    refreshCurrentRoute.run();
                });
                editDialog.show();
                styleRuntimeNestedDialog(editDialog);
            });
        }

        View fexAddButton = dialog.findViewById(R.id.BTRuntimeProfileFexAdd);
        if (fexAddButton instanceof ImageButton) {
            fexAddButton.setOnClickListener(v -> {
                FEXCoreEditPresetDialog editDialog = new FEXCoreEditPresetDialog(this, null);
                editDialog.setOnConfirmCallback(() -> {
                    if (fexPresetSpinner != null) {
                        loadRuntimeFexPresetSpinner(fexPresetSpinner, container.getFEXCorePreset());
                    }
                    refreshCurrentRoute.run();
                });
                editDialog.show();
                styleRuntimeNestedDialog(editDialog);
            });
        }
        View fexEditButton = dialog.findViewById(R.id.BTRuntimeProfileFexEdit);
        if (fexEditButton instanceof ImageButton) {
            fexEditButton.setOnClickListener(v -> {
                String selectedId = fexPresetSpinner != null ? FEXCorePresetManager.getSpinnerSelectedId(fexPresetSpinner) : container.getFEXCorePreset();
                FEXCoreEditPresetDialog editDialog = new FEXCoreEditPresetDialog(this, selectedId);
                editDialog.setOnConfirmCallback(() -> {
                    if (fexPresetSpinner != null) {
                        loadRuntimeFexPresetSpinner(fexPresetSpinner, selectedId);
                    }
                    refreshCurrentRoute.run();
                });
                editDialog.show();
                styleRuntimeNestedDialog(editDialog);
            });
        }

        dialog.setOnConfirmCallback(() -> {
            String selectedBox64Version = box64VersionSpinner != null && box64VersionSpinner.getSelectedItem() != null
                    ? box64VersionSpinner.getSelectedItem().toString()
                    : container.getBox64Version();
            String selectedBox64Preset = box64PresetSpinner != null
                    ? Box64PresetManager.getSpinnerSelectedId(box64PresetSpinner)
                    : container.getBox64Preset();
            String selectedFexVersion = fexVersionSpinner != null && fexVersionSpinner.getSelectedItem() != null
                    ? fexVersionSpinner.getSelectedItem().toString()
                    : container.getFEXCoreVersion();
            String selectedFexPreset = fexPresetSpinner != null
                    ? FEXCorePresetManager.getSpinnerSelectedId(fexPresetSpinner)
                    : container.getFEXCorePreset();

            if (fexPrimary) {
                container.setEmulator("fexcore");
                container.setFEXCoreVersion(selectedFexVersion);
            } else {
                container.setEmulator("box64");
                container.setBox64Version(selectedBox64Version);
            }
            container.setBox64Preset(selectedBox64Preset);
            container.setFEXCorePreset(selectedFexPreset);
            container.saveData();

            emulator = container.getEmulator();
            if (guestProgramLauncherComponent != null) {
                guestProgramLauncherComponent.setContainer(container);
                guestProgramLauncherComponent.setBox64Preset(selectedBox64Preset);
                guestProgramLauncherComponent.setFEXCorePreset(selectedFexPreset);
            }

            ForensicLogger.logEvent(
                    this,
                    "info",
                    "RUNTIME_PROFILES_UPDATED",
                    null,
                    "runtime_ui",
                    "runtime_profiles_updated",
                    ForensicLogger.fields(
                            "runtime_family", fexPrimary ? "fexcore" : "box64",
                            "emulator", container.getEmulator(),
                            "box64_version", firstNonEmpty(container.getBox64Version(), ""),
                            "box64_preset", selectedBox64Preset,
                            "fexcore_version", firstNonEmpty(container.getFEXCoreVersion(), ""),
                            "fexcore_preset", selectedFexPreset
                    )
            );
            showToast(this, R.string.runtime_profile_saved);
            refreshRuntimeDrawerState();
        });

        dialog.show();
        styleRuntimeNestedDialog(dialog);
        if (currentRouteView != null) {
            currentRouteView.setTextColor(ContextCompat.getColor(this, R.color.surface_runtime_taskmgr_text));
            currentRouteView.setBackgroundResource(R.drawable.surface_runtime_taskmgr_header_background);
        }
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            dialog.getWindow().setLayout(
                    Math.round(AppUtils.getScreenWidth() * 0.986f),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        ViewGroup.LayoutParams params = dialog.getContentView().getLayoutParams();
        if (params != null) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getContentView().setLayoutParams(params);
        }
        dialog.getContentView().setMinimumHeight(0);
    }

    private void refreshRuntimeDrawerState() {
        TextView tvContainer = findViewById(R.id.TVRuntimeDrawerContainerName);
        TextView tvShortcut = findViewById(R.id.TVRuntimeDrawerShortcutName);
        TextView tvRoute = findViewById(R.id.TVRuntimeDrawerRoute);
        TextView tvHint = findViewById(R.id.TVRuntimeDrawerHint);
        TextView tvGraphicsPrimary = findViewById(R.id.TVRuntimeGraphicsPrimary);
        TextView tvGraphicsSecondary = findViewById(R.id.TVRuntimeGraphicsSecondary);
        TextView tvGraphicsTertiary = findViewById(R.id.TVRuntimeGraphicsTertiary);

        if (tvContainer != null) {
            String containerName = container != null ? container.getName() : getString(R.string.not_set);
            tvContainer.setText(getString(R.string.xserver_runtime_drawer_container, containerName));
        }

        if (tvShortcut != null) {
            if (shortcut != null && shortcut.name != null && !shortcut.name.trim().isEmpty()) {
                tvShortcut.setText(getString(R.string.xserver_runtime_drawer_shortcut, shortcut.name));
            } else {
                tvShortcut.setText(R.string.xserver_runtime_drawer_shortcut_none);
            }
        }

        if (tvRoute != null) {
            String routeGraphics = graphicsDriver == null || graphicsDriver.isEmpty() ? "-" : graphicsDriver;
            String routeWrapper = dxwrapper == null || dxwrapper.isEmpty() ? "-" : dxwrapper;
            String routeAudio = audioDriver == null || audioDriver.isEmpty() ? "-" : audioDriver;
            tvRoute.setText(getString(R.string.xserver_runtime_drawer_route, routeGraphics, routeWrapper, routeAudio));
        }

        if (tvHint != null) tvHint.setText(R.string.xserver_runtime_drawer_hint);
        if (tvGraphicsPrimary != null) tvGraphicsPrimary.setText(resolveRuntimeGraphicsPrimaryText());
        if (tvGraphicsSecondary != null) tvGraphicsSecondary.setText(resolveRuntimeGraphicsSecondaryText());
        if (tvGraphicsTertiary != null) tvGraphicsTertiary.setText(resolveRuntimeGraphicsTertiaryText());

        boolean fullscreenActive = xServerView != null && xServerView.getRenderer() != null && xServerView.getRenderer().isFullscreen();
        boolean logsEnabled = true;
        boolean magnifierEnabled = !XrActivity.isEnabled(this);
        boolean activeWindowsEnabled = xServer != null
                && xServer.windowManager != null
                && (getTrackedApplicationWindowCount() > 0 || (winHandler != null && winHandler.isReady()));

        updateRuntimeDrawerAction(
                R.id.LLRuntimeActionRelativeMouse,
                R.id.TVRuntimeActionRelativeMouseTitle,
                R.id.TVRuntimeActionRelativeMouseSummary,
                R.id.IVRuntimeActionRelativeMouse,
                R.string.toggle_relative_mouse_movement,
                isRelativeMouseMovement ? R.string.runtime_drawer_relative_mouse_summary_on : R.string.runtime_drawer_relative_mouse_summary_off,
                R.drawable.ae_icon_magnifier,
                true
        );
        updateRuntimeDrawerAction(
                R.id.LLRuntimeActionFullscreen,
                R.id.TVRuntimeActionFullscreenTitle,
                R.id.TVRuntimeActionFullscreenSummary,
                R.id.IVRuntimeActionFullscreen,
                R.string.toggle_fullscreen,
                fullscreenActive ? R.string.runtime_drawer_fullscreen_summary_on : R.string.runtime_drawer_fullscreen_summary_off,
                R.drawable.ae_icon_fullscreen,
                true
        );
        updateRuntimeDrawerAction(
                R.id.LLRuntimeActionPauseResume,
                R.id.TVRuntimeActionPauseResumeTitle,
                R.id.TVRuntimeActionPauseResumeSummary,
                R.id.IVRuntimeActionPauseResume,
                isPaused ? R.string.resume_container : R.string.pause_container,
                isPaused ? R.string.runtime_drawer_resume_summary : R.string.runtime_drawer_pause_summary,
                isPaused ? R.drawable.ae_icon_play : R.drawable.ae_icon_pause,
                true
        );
        updateRuntimeDrawerAction(
                R.id.LLRuntimeActionTaskManager,
                R.id.TVRuntimeActionTaskManagerTitle,
                R.id.TVRuntimeActionTaskManagerSummary,
                R.id.IVRuntimeActionTaskManager,
                R.string.task_manager,
                R.string.runtime_drawer_task_manager_summary,
                R.drawable.ae_icon_task_manager,
                true
        );
        updateRuntimeDrawerAction(
                R.id.LLRuntimeActionActiveWindows,
                R.id.TVRuntimeActionActiveWindowsTitle,
                R.id.TVRuntimeActionActiveWindowsSummary,
                R.id.IVRuntimeActionActiveWindows,
                R.string.active_windows,
                R.string.runtime_drawer_active_windows_summary,
                R.drawable.ae_icon_front,
                activeWindowsEnabled
        );
        updateRuntimeDrawerAction(
                R.id.LLRuntimeActionMagnifier,
                R.id.TVRuntimeActionMagnifierTitle,
                R.id.TVRuntimeActionMagnifierSummary,
                R.id.IVRuntimeActionMagnifier,
                R.string.magnifier,
                magnifierEnabled ? R.string.runtime_drawer_magnifier_summary : R.string.runtime_drawer_magnifier_unavailable,
                R.drawable.ae_icon_magnifier,
                magnifierEnabled
        );
        updateRuntimeDrawerAction(
                R.id.LLRuntimeActionLogs,
                R.id.TVRuntimeActionLogsTitle,
                R.id.TVRuntimeActionLogsSummary,
                R.id.IVRuntimeActionLogs,
                R.string.logs,
                R.string.runtime_drawer_logs_summary,
                R.drawable.ae_icon_diagnostics,
                logsEnabled
        );
        updateRuntimeDrawerAction(
                R.id.LLRuntimeActionPrefixPack,
                R.id.TVRuntimeActionPrefixPackTitle,
                R.id.TVRuntimeActionPrefixPackSummary,
                R.id.IVRuntimeActionPrefixPack,
                R.string.prefix_pack_toolkit,
                R.string.runtime_drawer_prefix_pack_summary,
                R.drawable.ae_icon_package,
                true
        );
        updateRuntimeDrawerActionText(
                R.id.LLRuntimeActionRuntimeProfiles,
                R.id.TVRuntimeActionRuntimeProfilesTitle,
                R.id.TVRuntimeActionRuntimeProfilesSummary,
                R.id.IVRuntimeActionRuntimeProfiles,
                R.string.runtime_profiles,
                resolveRuntimeProfilesDrawerSummary(),
                R.drawable.ae_icon_env_var,
                shortcut == null
        );

        applyRuntimeThemeAssetPass();
        applyRuntimeDrawerSurfaceStyle();
        applyRuntimeGraphicsStatusCardStyle();
    }

    private void updateRuntimeDrawerAction(int rowId, int titleId, int summaryId, int iconId,
                                           int titleResId, int summaryResId, int iconResId, boolean enabled) {
        updateRuntimeDrawerActionText(
                rowId,
                titleId,
                summaryId,
                iconId,
                titleResId,
                getString(summaryResId),
                iconResId,
                enabled
        );
    }

    private void updateRuntimeDrawerActionText(int rowId, int titleId, int summaryId, int iconId,
                                               int titleResId, String summaryText, int iconResId, boolean enabled) {
        View row = findViewById(rowId);
        TextView title = findViewById(titleId);
        TextView summary = findViewById(summaryId);
        View icon = findViewById(iconId);
        if (row == null || title == null || summary == null || icon == null) return;

        title.setText(titleResId);
        summary.setText(summaryText);
        if (icon instanceof android.widget.ImageView) {
            ((android.widget.ImageView) icon).setImageResource(iconResId);
        }
        row.setEnabled(enabled);
        row.setClickable(enabled);
        row.setAlpha(1.0f);
        title.setAlpha(enabled ? 1.0f : 0.86f);
        summary.setAlpha(enabled ? 1.0f : 0.74f);
        icon.setAlpha(enabled ? 1.0f : 0.82f);
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private void handleRuntimeAction(int actionId) {
        final GLRenderer renderer = xServerView != null ? xServerView.getRenderer() : null;
        switch (actionId) {
            case R.id.main_menu_keyboard:
                AppUtils.showKeyboard(this);
                break;
            case R.id.main_menu_input_controls:
                showInputControlsDialog();
                break;
            case R.id.main_menu_relative_mouse_movement:
                isRelativeMouseMovement = !isRelativeMouseMovement;
                xServer.setRelativeMouseMovement(isRelativeMouseMovement);
                break;
            case R.id.main_menu_toggle_fullscreen:
                if (renderer != null) renderer.toggleFullscreen();
                if (touchpadView != null) touchpadView.toggleFullscreen();
                break;
            case R.id.main_menu_pause:
                if (isPaused) {
                    ProcessHelper.resumeAllWineProcesses();
                }
                else {
                    ProcessHelper.pauseAllWineProcesses();
                }
                isPaused = !isPaused;
                break;
            case R.id.main_menu_pip_mode:
                enterPictureInPictureMode();
                break;
            case R.id.main_menu_task_manager:
                new TaskManagerDialog(this).show();
                break;
            case R.id.main_menu_active_windows:
                ActiveWindowsDialog activeWindowsDialog = new ActiveWindowsDialog(this);
                activeWindowsDialog.show();
                styleRuntimeNestedDialog(activeWindowsDialog);
                break;
            case R.id.main_menu_magnifier:
                if (magnifierView == null) {
                    FrameLayout container = findViewById(R.id.FLXServerDisplay);
                    magnifierView = new MagnifierView(this);
                    magnifierView.setZoomButtonCallback(value -> {
                        renderer.setMagnifierZoom(Mathf.clamp(renderer.getMagnifierZoom() + value, 1.0f, 3.0f));
                        magnifierView.setZoomValue(renderer.getMagnifierZoom());
                    });
                    magnifierView.setZoomValue(renderer.getMagnifierZoom());
                    magnifierView.setHideButtonCallback(() -> {
                        container.removeView(magnifierView);
                        magnifierView = null;
                    });
                    container.addView(magnifierView);
                }
                break;
            case R.id.main_menu_screen_effects:
                Log.d("ScreenEffectDialog", "Initializing ScreenEffectDialog");
                ScreenEffectDialog screenEffectDialog = new ScreenEffectDialog(this);
                screenEffectDialog.setOnConfirmCallback(() -> {
                    Log.d("ScreenEffectDialog", "Confirm callback triggered. About to apply effects.");
                    GLRenderer currentRenderer = xServerView.getRenderer();
                    ColorEffect colorEffect = (ColorEffect) currentRenderer.getEffectComposer().getEffect(ColorEffect.class);
                    FXAAEffect fxaaEffect = (FXAAEffect) currentRenderer.getEffectComposer().getEffect(FXAAEffect.class);
                    CRTEffect crtEffect = (CRTEffect) currentRenderer.getEffectComposer().getEffect(CRTEffect.class);
                    ToonEffect toonEffect = (ToonEffect) currentRenderer.getEffectComposer().getEffect(ToonEffect.class);
                    NTSCCombinedEffect ntscEffect = (NTSCCombinedEffect) currentRenderer.getEffectComposer().getEffect(NTSCCombinedEffect.class);

                    // Check if effects are null before applying
                    Log.d("ScreenEffectDialog", "ColorEffect: " + (colorEffect != null));
                    Log.d("ScreenEffectDialog", "FXAAEffect: " + (fxaaEffect != null));
                    Log.d("ScreenEffectDialog", "CRTEffect: " + (crtEffect != null));
                    Log.d("ScreenEffectDialog", "ToonEffect: " + (toonEffect != null));
                    Log.d("ScreenEffectDialog", "NTSCCombinedEffect: " + (ntscEffect != null));

                    Log.d("ScreenEffectDialog", "Calling applyEffects()");
                    screenEffectDialog.applyEffects(colorEffect, currentRenderer, fxaaEffect, crtEffect, toonEffect, ntscEffect);
                    Log.d("ScreenEffectDialog", "applyEffects() called.");
                });
                Log.d("ScreenEffectDialog", "Showing ScreenEffectDialog");
                screenEffectDialog.show();
                styleRuntimeNestedDialog(screenEffectDialog);
                break;
            case R.id.main_menu_logs:
                ForensicUi.showForensicLogViewer(this, "runtime_drawer");
                break;
            case R.id.main_menu_exit:
                exit();
                break;
        }
        refreshRuntimeDrawerState();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (touchpadView != null) {
            touchpadView.resetTransientInputState();
        }

        if (hasFocus) {
            cancelDeferredDesktopRuntimePause("window_focus_regained");
        }

        ForensicLogger.logEvent(
                this,
                "info",
                "XSERVER_WINDOW_FOCUS_CHANGED",
                null,
                "xserver",
                "xserver_window_focus_changed",
                ForensicLogger.fields(
                        "has_focus", hasFocus,
                        "desktop_shell_bootstrap", desktopShellBootstrapActive,
                        "tracked_window_count", getTrackedApplicationWindowCount(),
                        "runtime_drawer_visible", runtimeDrawerVisible,
                        "shortcut_launch", shortcut != null
                )
        );

        if (hasFocus) {
            AppUtils.hideSystemUI(this);
            refreshDesktopGestureExclusion();
            maybeRunPendingGuestBootstrap("window_focus");
        }

        if (hasFocus && cursorLock && touchpadView != null) {
            touchpadView.requestPointerCapture();
            touchpadView.setOnCapturedPointerListener(new View.OnCapturedPointerListener() {
                @Override
                public boolean onCapturedPointer(View view, MotionEvent event) {
                    handleCapturedPointer(event);
                    return true;
                }
            });
        }
        else if (!hasFocus && touchpadView != null) {
            touchpadView.releasePointerCapture();
            touchpadView.setOnCapturedPointerListener(null);
        }
    }

    private boolean hasBundledAsset(String assetName) {
        try {
            String[] assets = getAssets().list("");
            if (assets == null) return false;
            for (String asset : assets) {
                if (assetName.equals(asset)) return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private String resolveInputDllAsset() {
        if (wineInfo != null) {
            if (wineInfo.isArm64EC() && hasBundledAsset("arm64ec_input_dlls.tzst")) {
                return "arm64ec_input_dlls.tzst";
            }
            if ("x86_64".equalsIgnoreCase(wineInfo.getArch()) && hasBundledAsset("x86_64_input_dlls.tzst")) {
                return "x86_64_input_dlls.tzst";
            }
        }
        return "input_dlls.tzst";
    }

    private void extractInputDLLs() {
        String inputAsset = resolveInputDllAsset();
        File wineFolder = WineUtils.resolveRuntimeWineLibDir(new File(imageFs.getWinePath()));
        if (wineFolder == null) {
            Log.d("XServerDisplayActivity", "Skipping input dll extraction: runtime lib/wine path missing");
            return;
        }
        Log.d("XServerDisplayActivity", "Extracting input dlls from " + inputAsset + " to " + wineFolder.getPath());
        boolean success = TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, inputAsset, wineFolder);
        if (!success)
            Log.d("XServerDisplayActivity", "Failed to extract input dlls");
    }

    private boolean isTrackedApplicationWindow(Window window) {
        return isTrackedVisualWindow(window);
    }

    private void noteApplicationWindowMapped(Window window) {
        if (!isTrackedApplicationWindow(window)) return;
        int trackedCount;
        synchronized (mappedApplicationWindowIds) {
            if (!mappedApplicationWindowIds.add(window.id)) return;
            trackedCount = mappedApplicationWindowIds.size();
        }
        lastTrackedApplicationWindowMappedAtMs = System.currentTimeMillis();
        lastTrackedApplicationWindowClassName = window.getClassName() != null
                ? window.getClassName()
                : "";
        cancelDeferredGuestTermination("window_mapped");
        ForensicLogger.logEvent(
                this,
                "info",
                "XSERVER_APP_WINDOW_MAPPED",
                null,
                "xserver",
                "tracked_application_window_mapped",
                ForensicLogger.fields(
                        "window_id", window.id,
                        "class_name", window.getClassName(),
                        "tracked_count", trackedCount,
                        "desktop_shell_bootstrap", desktopShellBootstrapActive
                )
        );
        maybeRunDebugStartProbe(window, trackedCount);
        maybeRunDebugAutoOpenTaskManager(window, trackedCount);
        maybeRunDebugAutoOpenRuntimeDrawer(window, trackedCount);
        maybeRunDebugAutoOpenLogs(window, trackedCount);
        maybeRunDebugAutoOpenPrefixPack(window, trackedCount);
    }

    private void markGuestVisualReady(String reason, @Nullable Window window, @Nullable DesktopShellBootstrapProof proof) {
        if (guestVisualReady) return;
        guestVisualReady = true;
        if (xServerView != null && xServerView.getRenderer() != null) {
            xServerView.getRenderer().setCursorVisible(true);
        }
        if (preloaderDialog != null) {
            preloaderDialog.closeOnUiThread();
        }
        ForensicLogger.logEvent(
                this,
                "info",
                "XSERVER_GUEST_VISUAL_READY",
                null,
                "xserver",
                "guest_visual_ready",
                ForensicLogger.fields(
                        "reason", reason,
                        "class_name", window != null && window.getClassName() != null ? window.getClassName() : "",
                        "window_id", window != null ? window.id : -1,
                        "desktop_shell_bootstrap", desktopShellBootstrapActive,
                        "desktop_shell_launch_mode", desktopShellLaunchMode,
                        "bootstrap_elapsed_ms", Math.max(0L, System.currentTimeMillis() - desktopShellBootstrapStartedAtMs),
                        "tracked_window_count", getTrackedApplicationWindowCount(),
                        "winhandler_ready", winHandler != null && winHandler.isReady(),
                        "shell_launcher_present", proof != null && proof.shellLauncherPresent,
                        "shell_process_present", proof != null && proof.explorerProcessPresent,
                        "winhandler_process_present", proof != null && proof.winHandlerProcessPresent,
                        "wfm_process_present", proof != null && proof.wfmProcessPresent,
                        "wineboot_process_present", proof != null && proof.winebootProcessPresent
                )
        );
    }

    private void maybeRunDebugStartProbe(Window window, int trackedCount) {
        if (!debugStartProbeArmed || debugStartProbeExecuted) return;
        if (shortcut != null || !desktopShellBootstrapActive) return;
        if (window == null || !"explorer.exe".equalsIgnoreCase(window.getClassName())) return;
        if (trackedCount < 2) return;

        debugStartProbeExecuted = true;
        final int probeX = debugStartProbeTargetX != Integer.MIN_VALUE
                ? debugStartProbeTargetX
                : 28;
        final int probeY = debugStartProbeTargetY != Integer.MIN_VALUE
                ? debugStartProbeTargetY
                : (xServer != null ? Math.max(0, xServer.screenInfo.height - 14) : 0);
        final int tapCount = Math.max(1, debugStartProbeTapCount);
        final int tapIntervalMs = Math.max(40, debugStartProbeTapIntervalMs);
        ForensicLogger.logEvent(
                this,
                "info",
                "DESKTOP_DEBUG_START_PROBE_ARMED",
                null,
                "xserver",
                "desktop_debug_start_probe_armed",
                ForensicLogger.fields(
                        "tracked_count", trackedCount,
                        "target_x", probeX,
                        "target_y", probeY,
                        "tap_count", tapCount,
                        "tap_interval_ms", tapIntervalMs
                )
        );

        handler.postDelayed(() -> dispatchDebugStartProbeTaps(probeX, probeY, tapCount, tapIntervalMs), 180L);
    }

    private void maybeRunDebugAutoOpenTaskManager(Window window, int trackedCount) {
        if (!debugAutoOpenTaskManagerArmed || debugAutoOpenTaskManagerExecuted) return;
        if (shortcut != null || !desktopShellBootstrapActive) return;
        if (window == null || !"explorer.exe".equalsIgnoreCase(window.getClassName())) return;
        if (trackedCount < 2) return;

        debugAutoOpenTaskManagerExecuted = true;
        ForensicLogger.logEvent(
                this,
                "info",
                "DESKTOP_DEBUG_TASKMGR_AUTOOPEN_ARMED",
                null,
                "xserver",
                "desktop_debug_task_manager_auto_open_armed",
                ForensicLogger.fields("tracked_count", trackedCount)
        );
        handler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "DESKTOP_DEBUG_TASKMGR_AUTOOPEN_EXEC",
                    null,
                    "xserver",
                    "desktop_debug_task_manager_auto_open_exec",
                    ForensicLogger.fields("tracked_count", getTrackedApplicationWindowCount())
            );
            new TaskManagerDialog(this).show();
        }, 320L);
    }

    private void maybeRunDebugAutoOpenRuntimeDrawer(Window window, int trackedCount) {
        if (!debugAutoOpenRuntimeDrawerArmed || debugAutoOpenRuntimeDrawerExecuted) return;
        if (shortcut != null || !desktopShellBootstrapActive) return;
        if (window == null || !"explorer.exe".equalsIgnoreCase(window.getClassName())) return;
        if (trackedCount < 2) return;

        debugAutoOpenRuntimeDrawerExecuted = true;
        ForensicLogger.logEvent(
                this,
                "info",
                "DESKTOP_DEBUG_DRAWER_AUTOOPEN_ARMED",
                null,
                "xserver",
                "desktop_debug_drawer_auto_open_armed",
                ForensicLogger.fields("tracked_count", trackedCount)
        );
        handler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "DESKTOP_DEBUG_DRAWER_AUTOOPEN_EXEC",
                    null,
                    "xserver",
                    "desktop_debug_drawer_auto_open_exec",
                    ForensicLogger.fields(
                            "tracked_count", getTrackedApplicationWindowCount(),
                            "runtime_drawer_ready", runtimeDrawerView != null
                    )
            );
            showRuntimeDrawer();
        }, 380L);
    }

    private void maybeRunDebugAutoOpenLogs(Window window, int trackedCount) {
        if (!debugAutoOpenLogsArmed || debugAutoOpenLogsExecuted) return;
        if (shortcut != null || !desktopShellBootstrapActive) return;
        if (window == null || !"explorer.exe".equalsIgnoreCase(window.getClassName())) return;
        if (trackedCount < 2) return;

        debugAutoOpenLogsExecuted = true;
        ForensicLogger.logEvent(
                this,
                "info",
                "DESKTOP_DEBUG_LOGS_AUTOOPEN_ARMED",
                null,
                "xserver",
                "desktop_debug_logs_auto_open_armed",
                ForensicLogger.fields(
                        "tracked_count", trackedCount,
                        "debug_dialog_ready", debugDialog != null
                )
        );
        handler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "DESKTOP_DEBUG_LOGS_AUTOOPEN_EXEC",
                    null,
                    "xserver",
                    "desktop_debug_logs_auto_open_exec",
                    ForensicLogger.fields(
                            "tracked_count", getTrackedApplicationWindowCount(),
                            "debug_dialog_ready", debugDialog != null
                    )
            );
            if (debugDialog != null) {
                debugDialog.show();
            }
        }, 420L);
    }

    private void maybeRunDebugAutoOpenPrefixPack(Window window, int trackedCount) {
        if (!debugAutoOpenPrefixPackArmed || debugAutoOpenPrefixPackExecuted) return;
        if (shortcut != null || !desktopShellBootstrapActive) return;
        if (window == null || !"explorer.exe".equalsIgnoreCase(window.getClassName())) return;
        if (trackedCount < 2) return;

        debugAutoOpenPrefixPackExecuted = true;
        ForensicLogger.logEvent(
                this,
                "info",
                "DESKTOP_DEBUG_PREFIXPACK_AUTOOPEN_ARMED",
                null,
                "xserver",
                "desktop_debug_prefix_pack_auto_open_armed",
                ForensicLogger.fields("tracked_count", trackedCount)
        );
        handler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "DESKTOP_DEBUG_PREFIXPACK_AUTOOPEN_EXEC",
                    null,
                    "xserver",
                    "desktop_debug_prefix_pack_auto_open_exec",
                    ForensicLogger.fields("tracked_count", getTrackedApplicationWindowCount())
            );
            showPrefixPackGuide();
        }, 480L);
    }

    private void scheduleDebugPrefixPackFallback() {
        if (!debugAutoOpenPrefixPackArmed || handler == null) return;
        scheduleDebugPrefixPackFallbackAttempt(0, DEBUG_PREFIXPACK_FALLBACK_INITIAL_DELAY_MS);
    }

    private void scheduleDebugPrefixPackFallbackAttempt(int attempt, long delayMs) {
        handler.postDelayed(() -> {
            if (isFinishing() || isDestroyed() || debugAutoOpenPrefixPackExecuted) return;
            if (!isDesktopShellCommandReady()) {
                if (attempt + 1 < DEBUG_PREFIXPACK_FALLBACK_MAX_ATTEMPTS) {
                    ForensicLogger.logEvent(
                            this,
                            attempt == 0 ? "info" : "warning",
                            "DESKTOP_DEBUG_PREFIXPACK_FALLBACK_WAIT",
                            null,
                            "xserver",
                            "desktop_debug_prefix_pack_fallback_wait",
                            ForensicLogger.fields(
                                    "attempt", attempt + 1,
                                    "tracked_count", getTrackedApplicationWindowCount(),
                                    "desktop_shell_bootstrap_active", desktopShellBootstrapActive,
                                    "winhandler_ready", winHandler != null && winHandler.isReady()
                            )
                    );
                    scheduleDebugPrefixPackFallbackAttempt(attempt + 1, DEBUG_PREFIXPACK_FALLBACK_RETRY_MS);
                    return;
                }
                String deferredTarget = debugAutoInstallPrefixPackTarget;
                debugAutoInstallPrefixPackTarget = "";
                debugAutoOpenPrefixPackExecuted = true;
                ForensicLogger.logEvent(
                        this,
                        "warning",
                        "DESKTOP_DEBUG_PREFIXPACK_FALLBACK_TIMEOUT",
                        null,
                        "xserver",
                        "desktop_debug_prefix_pack_fallback_timeout",
                        ForensicLogger.fields(
                                "attempts", DEBUG_PREFIXPACK_FALLBACK_MAX_ATTEMPTS,
                                "tracked_count", getTrackedApplicationWindowCount(),
                                "desktop_shell_bootstrap_active", desktopShellBootstrapActive,
                                "winhandler_ready", winHandler != null && winHandler.isReady(),
                                "deferred_install_target", deferredTarget
                        )
                );
                showPrefixPackGuide();
                return;
            }
            debugAutoOpenPrefixPackExecuted = true;
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "DESKTOP_DEBUG_PREFIXPACK_FALLBACK_EXEC",
                    null,
                    "xserver",
                    "desktop_debug_prefix_pack_fallback_exec",
                    ForensicLogger.fields(
                            "tracked_count", getTrackedApplicationWindowCount(),
                            "desktop_shell_bootstrap_active", desktopShellBootstrapActive
                    )
            );
            showPrefixPackGuide();
        }, Math.max(120L, delayMs));
    }

    private void dispatchDebugStartProbeTaps(int probeX, int probeY, int tapCount, int tapIntervalMs) {
        for (int i = 0; i < tapCount; i++) {
            final int tapIndex = i + 1;
            handler.postDelayed(() -> dispatchSingleDebugStartProbeTap(probeX, probeY, tapIndex, tapCount), (long) i * tapIntervalMs);
        }
    }

    private void dispatchSingleDebugStartProbeTap(int probeX, int probeY, int tapIndex, int tapCount) {
        boolean accepted = false;
        if (touchpadView != null) {
            accepted = touchpadView.debugPerformCursorTap(probeX, probeY);
        }
        if (!accepted && xServer != null) {
            xServer.injectPointerMove(probeX, probeY);
            xServer.injectPointerButtonPress(Pointer.Button.BUTTON_LEFT);
            xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_LEFT);
        }
        ForensicLogger.logEvent(
                this,
                "info",
                "DESKTOP_DEBUG_START_PROBE_DISPATCHED",
                null,
                "xserver",
                "desktop_debug_start_probe_dispatched",
                ForensicLogger.fields(
                        "target_x", probeX,
                        "target_y", probeY,
                        "tap_index", tapIndex,
                        "tap_count", tapCount,
                        "transport", accepted ? "touchpad_view" : "xserver_fallback"
                )
        );
    }

    private void noteApplicationWindowUnmapped(Window window) {
        if (window == null) return;
        int trackedCount;
        synchronized (mappedApplicationWindowIds) {
            if (!mappedApplicationWindowIds.remove(window.id)) return;
            trackedCount = mappedApplicationWindowIds.size();
        }
        ForensicLogger.logEvent(
                this,
                "info",
                "XSERVER_APP_WINDOW_UNMAPPED",
                null,
                "xserver",
                "tracked_application_window_unmapped",
                ForensicLogger.fields(
                        "window_id", window.id,
                        "class_name", window.getClassName(),
                        "tracked_count", trackedCount,
                        "desktop_shell_bootstrap", desktopShellBootstrapActive,
                        "guest_launcher_exited", guestLauncherExited,
                        "guest_launcher_exit_status", guestLauncherExitStatus
                )
        );
        if (desktopShellBootstrapActive && guestLauncherExited && trackedCount == 0) {
            DesktopShellBootstrapProof proof = collectDesktopShellBootstrapProof();
            if (shouldKeepDesktopShellAliveAfterPrimaryTermination(proof, trackedCount)) {
                ForensicLogger.logEvent(
                        this,
                        "info",
                        "XSERVER_DEFERRED_EXIT_HELD_FOR_LIVE_SHELL",
                        null,
                        "xserver",
                        "deferred_exit_held_for_live_desktop_shell",
                        ForensicLogger.fields(
                                "guest_launcher_exit_status", guestLauncherExitStatus,
                                "bootstrap_elapsed_ms", proof.bootstrapElapsedMs,
                                "shell_process_present", proof.explorerProcessPresent,
                                "winhandler_process_present", proof.winHandlerProcessPresent,
                                "wfm_process_present", proof.wfmProcessPresent,
                                "wineboot_process_present", proof.winebootProcessPresent,
                                "wineserver_present", proof.wineserverPresent,
                                "winhandler_ready", winHandler != null && winHandler.isReady()
                        )
                );
                scheduleDeferredGuestTermination(guestLauncherExitStatus);
                return;
            }
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "XSERVER_DEFERRED_EXIT_RESUMED",
                    null,
                    "xserver",
                    "deferred_exit_resumed_after_last_window",
                    ForensicLogger.fields(
                            "guest_launcher_exit_status", guestLauncherExitStatus
                    )
            );
            runOnUiThread(this::exit);
        }
    }

    private boolean shouldDeferGuestTermination(int status) {
        if (!desktopShellBootstrapActive) return false;
        int trackedWindowCount = getTrackedApplicationWindowCount();
        if (trackedWindowCount > 0) return true;
        DesktopShellBootstrapProof proof = collectDesktopShellBootstrapProof();
        return shouldKeepDesktopShellAliveAfterPrimaryTermination(proof, trackedWindowCount);
    }

    private int getTrackedApplicationWindowCount() {
        synchronized (mappedApplicationWindowIds) {
            return mappedApplicationWindowIds.size();
        }
    }

    public boolean isDesktopShellCommandReady() {
        if (isFinishing() || isDestroyed() || exitInProgress.get()) return false;
        boolean winHandlerReady = winHandler != null && winHandler.isReady();
        if (shortcut != null) return true;
        if (!desktopShellBootstrapActive) return true;
        boolean requireWinHandler = desktopShellRequiresWinHandler();
        if (requireWinHandler && !winHandlerReady) return false;
        if (deferredDesktopPauseScheduled) return false;
        int trackedWindowCount = getTrackedApplicationWindowCount();
        if (trackedWindowCount > 0) return true;
        return collectDesktopShellBootstrapProof().hasProcessProof(winHandlerReady, requireWinHandler);
    }

    public boolean hasFreshTrackedApplicationWindowMappedSince(long startedAtMs) {
        long mappedAtMs = lastTrackedApplicationWindowMappedAtMs;
        if (mappedAtMs <= 0L) return false;
        long freshnessFloor = Math.max(0L, startedAtMs - 800L);
        if (mappedAtMs < freshnessFloor) return false;
        String className = lastTrackedApplicationWindowClassName != null
                ? lastTrackedApplicationWindowClassName.trim().toLowerCase(Locale.US)
                : "";
        if (className.isEmpty()) return true;
        return !"explorer.exe".equals(className)
                && !"wfm.exe".equals(className)
                && !"taskmgr.exe".equals(className);
    }

    private void scheduleDeferredGuestTermination(int status) {
        long elapsedMs = Math.max(0L, System.currentTimeMillis() - desktopShellBootstrapStartedAtMs);
        long remainingMs = Math.max(0L, DESKTOP_SHELL_BOOTSTRAP_HORIZON_MS - elapsedMs);
        long delayMs = remainingMs > 0L
                ? Math.min(DESKTOP_SHELL_PRELOADER_FALLBACK_RETRY_MS, remainingMs)
                : DESKTOP_SHELL_PRELOADER_FALLBACK_RETRY_MS;
        guestLauncherExitStatus = status;
        runtimePauseHandler.removeCallbacks(deferredGuestTerminationRunnable);
        deferredGuestTerminationScheduled = true;
        runtimePauseHandler.postDelayed(deferredGuestTerminationRunnable, delayMs);
    }

    private boolean shouldKeepDesktopShellAliveAfterPrimaryTermination(@Nullable DesktopShellBootstrapProof proof, int trackedWindowCount) {
        if (!desktopShellBootstrapActive || guestVisualReady) return false;
        if (trackedWindowCount > 0) return true;
        DesktopShellBootstrapProof liveProof = proof != null ? proof : collectDesktopShellBootstrapProof();
        if (desktopShellDetachedFallbackActive) return true;
        if (!isDesktopShellBootstrapWithinHorizon(liveProof)) {
            return liveProof.hasRuntimeLivenessProof(winHandler != null && winHandler.isReady());
        }
        if (desktopShellWinHandlerFallbackAttempted) {
            return liveProof.winHandlerProcessPresent
                    || liveProof.wfmProcessPresent
                    || (winHandler != null && winHandler.isReady())
                    || liveProof.wineserverPresent;
        }
        return liveProof.explorerProcessPresent
                || liveProof.winebootProcessPresent
                || liveProof.wineserverPresent;
    }

    private boolean isDesktopShellBootstrapWithinHorizon(@Nullable DesktopShellBootstrapProof proof) {
        long elapsedMs = proof != null
                ? proof.bootstrapElapsedMs
                : Math.max(0L, System.currentTimeMillis() - desktopShellBootstrapStartedAtMs);
        return elapsedMs < DESKTOP_SHELL_BOOTSTRAP_HORIZON_MS;
    }

    private boolean maybeLaunchDesktopShellFallbackOnPrimaryTermination(int status) {
        if (shortcut != null || !desktopShellBootstrapActive || guestVisualReady) return false;
        if (getTrackedApplicationWindowCount() > 0) return false;
        if (!DESKTOP_SHELL_LAUNCH_MODE_DIRECT_EXPLORER.equals(desktopShellLaunchMode)) return false;
        if (desktopShellWinHandlerFallbackAttempted) return false;
        if (!hasContainerShellExecutable("wfm.exe")) return false;

        DesktopShellBootstrapProof proof = collectDesktopShellBootstrapProof();
        if (!isDesktopShellBootstrapWithinHorizon(proof)) return false;

        attemptDesktopShellWinHandlerFallback(proof, -1);
        ForensicLogger.logEvent(
                this,
                desktopShellDetachedFallbackActive ? "warn" : "error",
                desktopShellDetachedFallbackActive
                        ? "XSERVER_DESKTOP_SHELL_FALLBACK_ON_TERMINATION"
                        : "XSERVER_DESKTOP_SHELL_FALLBACK_ON_TERMINATION_FAILED",
                null,
                "xserver",
                desktopShellDetachedFallbackActive
                        ? "desktop_shell_fallback_started_after_primary_termination"
                        : "desktop_shell_fallback_failed_after_primary_termination",
                ForensicLogger.fields(
                        "status", status,
                        "bootstrap_elapsed_ms", proof.bootstrapElapsedMs,
                        "wineserver_present", proof.wineserverPresent,
                        "wineboot_process_present", proof.winebootProcessPresent,
                        "explorer_process_present", proof.explorerProcessPresent
                )
        );
        return desktopShellDetachedFallbackActive;
    }

    private void cancelDeferredGuestTermination(String reason) {
        if (!deferredGuestTerminationScheduled) return;
        runtimePauseHandler.removeCallbacks(deferredGuestTerminationRunnable);
        deferredGuestTerminationScheduled = false;
        ForensicLogger.logEvent(
                this,
                "info",
                "GUEST_PROGRAM_TERMINATION_DEFER_CANCELLED",
                null,
                "xserver",
                "guest_program_termination_grace_cancelled",
                ForensicLogger.fields(
                        "reason", reason,
                        "tracked_window_count", getTrackedApplicationWindowCount(),
                        "desktop_shell_bootstrap", desktopShellBootstrapActive
                )
        );
    }

    private boolean shouldKeepDesktopRuntimeActiveOnPause() {
        if (shortcut != null) return false;
        if (isFinishing() || exitInProgress.get()) return false;
        return desktopShellBootstrapActive || getTrackedApplicationWindowCount() > 0;
    }

    private void scheduleDeferredDesktopRuntimePause() {
        scheduleDeferredDesktopRuntimePause(DESKTOP_RUNTIME_PAUSE_GRACE_MS);
    }

    private void scheduleDeferredDesktopRuntimePause(long delayMs) {
        long now = System.currentTimeMillis();
        long boundedDelayMs = Math.max(0L, delayMs);
        long requestedDeadlineAtMs = now + boundedDelayMs;
        if (deferredDesktopPauseScheduled && deferredDesktopPauseDeadlineAtMs >= requestedDeadlineAtMs) {
            return;
        }
        runtimePauseHandler.removeCallbacks(deferredDesktopPauseRunnable);
        deferredDesktopPauseScheduled = true;
        deferredDesktopPauseDeadlineAtMs = requestedDeadlineAtMs;
        runtimePauseHandler.postDelayed(deferredDesktopPauseRunnable, boundedDelayMs);
    }

    private boolean shouldKeepDesktopRuntimeActiveAcrossStop(@Nullable DesktopShellBootstrapProof proof, int trackedWindowCount) {
        if (shortcut != null) return false;
        if (isFinishing() || exitInProgress.get()) return false;
        if (!desktopShellBootstrapActive || guestVisualReady) return false;
        if (trackedWindowCount > 0) return false;
        if (proof == null) return false;
        if (proof.winebootProcessPresent) return true;
        return proof.wineserverPresent && (proof.explorerProcessPresent || proof.shellLauncherPresent);
    }

    private boolean shouldRenewDeferredDesktopRuntimePause(@Nullable DesktopShellBootstrapProof proof, int trackedWindowCount) {
        if (!shouldAutoSuspendRuntimeOnLifecycle()) return false;
        if (!shouldKeepDesktopRuntimeActiveAcrossStop(proof, trackedWindowCount)) return false;
        return proof != null && proof.bootstrapElapsedMs < DESKTOP_RUNTIME_STOP_BOOTSTRAP_MAX_MS;
    }

    private String getEffectiveSuspendPolicy() {
        return container != null ? container.getSuspendPolicy() : Container.SUSPEND_POLICY_AUTO;
    }

    private boolean shouldAutoSuspendRuntimeOnLifecycle() {
        return Container.SUSPEND_POLICY_AUTO.equals(getEffectiveSuspendPolicy());
    }

    private void logDesktopRuntimePauseSkipped(String reason) {
        ForensicLogger.logEvent(
                this,
                "info",
                "XSERVER_RUNTIME_PAUSE_SKIPPED",
                null,
                "xserver",
                "desktop_runtime_pause_skipped",
                ForensicLogger.fields(
                        "reason", reason,
                        "desktop_shell_bootstrap", desktopShellBootstrapActive,
                        "tracked_window_count", getTrackedApplicationWindowCount(),
                        "runtime_drawer_visible", runtimeDrawerVisible,
                        "suspend_policy", getEffectiveSuspendPolicy()
                )
        );
    }

    private void scheduleDesktopShellPreloaderFallback() {
        if (shortcut != null || handler == null) return;
        scheduleDesktopShellPreloaderFallbackAttempt(0, DESKTOP_SHELL_PRELOADER_FALLBACK_INITIAL_DELAY_MS);
    }

    private void scheduleDesktopShellPreloaderFallbackAttempt(int attempt, long delayMs) {
        handler.postDelayed(() -> {
            if (isFinishing() || isDestroyed() || guestVisualReady || preloaderDialog == null) return;
            if (!desktopShellBootstrapActive) {
                if (attempt + 1 < DESKTOP_SHELL_PRELOADER_FALLBACK_MAX_ATTEMPTS) {
                    scheduleDesktopShellPreloaderFallbackAttempt(attempt + 1, DESKTOP_SHELL_PRELOADER_FALLBACK_RETRY_MS);
                }
                return;
            }
            DesktopShellBootstrapProof proof = collectDesktopShellBootstrapProof();
            boolean winHandlerReady = winHandler != null && winHandler.isReady();
            boolean requireWinHandler = desktopShellRequiresWinHandler();
            int trackedWindowCount = getTrackedApplicationWindowCount();
            if (shouldAttemptDesktopShellWinHandlerFallback(proof, trackedWindowCount)) {
                attemptDesktopShellWinHandlerFallback(proof, attempt);
                if (attempt + 1 < DESKTOP_SHELL_PRELOADER_FALLBACK_MAX_ATTEMPTS) {
                    scheduleDesktopShellPreloaderFallbackAttempt(attempt + 1, DESKTOP_SHELL_PRELOADER_FALLBACK_RETRY_MS);
                }
                return;
            }
            boolean processProof = proof.hasProcessProof(winHandlerReady, requireWinHandler);
            boolean visualReady = proof.hasVisualProof(trackedWindowCount);
            if (visualReady) {
                ForensicLogger.logEvent(
                        this,
                        "info",
                        "XSERVER_BOOTSTRAP_PRELOADER_FALLBACK_EXEC",
                        null,
                        "xserver",
                        "preloader_closed_on_desktop_shell_visual_proof",
                        ForensicLogger.fields(
                                "attempt", attempt + 1,
                                "shell_launcher_present", proof.shellLauncherPresent,
                                "shell_process_present", proof.explorerProcessPresent,
                                "winhandler_process_present", proof.winHandlerProcessPresent,
                                "wfm_process_present", proof.wfmProcessPresent,
                                "wineboot_process_present", proof.winebootProcessPresent,
                                "wineserver_present", proof.wineserverPresent,
                                "bootstrap_elapsed_ms", proof.bootstrapElapsedMs,
                                "desktop_shell_launch_mode", desktopShellLaunchMode,
                                "require_winhandler", requireWinHandler,
                                "winhandler_ready", winHandlerReady,
                                "tracked_window_count", trackedWindowCount,
                                "process_proof", processProof
                        )
                );
                markGuestVisualReady("desktop_shell_visual_proof", null, proof);
                return;
            }
            if (processProof && shouldAcceptNonvisualDesktopShellProcessProof(proof, winHandlerReady, requireWinHandler, trackedWindowCount)) {
                ForensicLogger.logEvent(
                        this,
                        "info",
                        "XSERVER_BOOTSTRAP_PRELOADER_FALLBACK_EXEC",
                        null,
                        "xserver",
                        "preloader_closed_on_desktop_shell_process_proof",
                        ForensicLogger.fields(
                                "attempt", attempt + 1,
                                "shell_launcher_present", proof.shellLauncherPresent,
                                "shell_process_present", proof.explorerProcessPresent,
                                "winhandler_process_present", proof.winHandlerProcessPresent,
                                "wfm_process_present", proof.wfmProcessPresent,
                                "wineboot_process_present", proof.winebootProcessPresent,
                                "wineserver_present", proof.wineserverPresent,
                                "bootstrap_elapsed_ms", proof.bootstrapElapsedMs,
                                "desktop_shell_launch_mode", desktopShellLaunchMode,
                                "require_winhandler", requireWinHandler,
                                "winhandler_ready", winHandlerReady,
                                "tracked_window_count", trackedWindowCount,
                                "process_proof", true,
                                "nonvisual_process_proof_accepted", true
                        )
                );
                markGuestVisualReady("desktop_shell_process_proof", null, proof);
                return;
            }
            if (attempt + 1 < DESKTOP_SHELL_PRELOADER_FALLBACK_MAX_ATTEMPTS) {
                if (attempt == 0 || attempt + 1 == DESKTOP_SHELL_PRELOADER_FALLBACK_MAX_ATTEMPTS / 2) {
                    if (trackedWindowCount == 0) {
                        logBootstrapWindowSnapshot(
                                "XSERVER_WINDOW_FRONTIER_SNAPSHOT",
                                "desktop_shell_window_frontier_snapshot"
                        );
                    }
                    ForensicLogger.logEvent(
                            this,
                            "info",
                            "XSERVER_BOOTSTRAP_PRELOADER_FALLBACK_WAIT",
                            null,
                            "xserver",
                            "desktop_shell_process_proof_pending",
                            ForensicLogger.fields(
                                    "attempt", attempt + 1,
                                    "shell_launcher_present", proof.shellLauncherPresent,
                                    "shell_process_present", proof.explorerProcessPresent,
                                    "winhandler_process_present", proof.winHandlerProcessPresent,
                                    "wfm_process_present", proof.wfmProcessPresent,
                                    "wineboot_process_present", proof.winebootProcessPresent,
                                    "wineserver_present", proof.wineserverPresent,
                                    "bootstrap_elapsed_ms", proof.bootstrapElapsedMs,
                                    "desktop_shell_launch_mode", desktopShellLaunchMode,
                                    "require_winhandler", requireWinHandler,
                                    "winhandler_ready", winHandlerReady,
                                    "tracked_window_count", trackedWindowCount
                            )
                    );
                    if (processProof) {
                        ForensicLogger.logEvent(
                                this,
                                "warning",
                                "XSERVER_BOOTSTRAP_NONVISUAL_PROCESS_PROOF",
                                null,
                                "xserver",
                                "desktop_shell_process_proof_without_visual_window",
                                ForensicLogger.fields(
                                        "attempt", attempt + 1,
                                        "shell_launcher_present", proof.shellLauncherPresent,
                                        "shell_process_present", proof.explorerProcessPresent,
                                        "winhandler_process_present", proof.winHandlerProcessPresent,
                                        "wfm_process_present", proof.wfmProcessPresent,
                                        "wineboot_process_present", proof.winebootProcessPresent,
                                        "wineserver_present", proof.wineserverPresent,
                                        "bootstrap_elapsed_ms", proof.bootstrapElapsedMs,
                                        "desktop_shell_launch_mode", desktopShellLaunchMode,
                                        "require_winhandler", requireWinHandler,
                                        "winhandler_ready", winHandlerReady,
                                        "tracked_window_count", trackedWindowCount
                                )
                        );
                    }
                }
                scheduleDesktopShellPreloaderFallbackAttempt(attempt + 1, DESKTOP_SHELL_PRELOADER_FALLBACK_RETRY_MS);
                return;
            }
            if (trackedWindowCount == 0) {
                logBootstrapWindowSnapshot(
                        "XSERVER_WINDOW_FRONTIER_STALLED",
                        "desktop_shell_window_frontier_stalled"
                );
            }
            ForensicLogger.logEvent(
                    this,
                    "warning",
                    "XSERVER_BOOTSTRAP_PRELOADER_STALLED",
                    null,
                    "xserver",
                    "desktop_shell_process_proof_never_materialized",
                    ForensicLogger.fields(
                            "attempts", DESKTOP_SHELL_PRELOADER_FALLBACK_MAX_ATTEMPTS,
                            "shell_launcher_present", proof.shellLauncherPresent,
                            "shell_process_present", proof.explorerProcessPresent,
                            "winhandler_process_present", proof.winHandlerProcessPresent,
                            "wfm_process_present", proof.wfmProcessPresent,
                            "wineboot_process_present", proof.winebootProcessPresent,
                            "wineserver_present", proof.wineserverPresent,
                            "bootstrap_elapsed_ms", proof.bootstrapElapsedMs,
                            "desktop_shell_launch_mode", desktopShellLaunchMode,
                            "require_winhandler", requireWinHandler,
                            "winhandler_ready", winHandlerReady,
                            "tracked_window_count", trackedWindowCount,
                            "process_proof", processProof
                    )
            );
        }, Math.max(200L, delayMs));
    }

    private boolean shouldAcceptNonvisualDesktopShellProcessProof(
            DesktopShellBootstrapProof proof,
            boolean winHandlerReady,
            boolean requireWinHandler,
            int trackedWindowCount
    ) {
        // Process liveness is diagnostic only. Black-screen closure requires
        // an X11 window that reached the tracked visual frontier.
        return false;
    }

    private boolean shouldAttemptDesktopShellWinHandlerFallback(DesktopShellBootstrapProof proof, int trackedWindowCount) {
        if (shortcut != null) return false;
        if (!DESKTOP_SHELL_LAUNCH_MODE_DIRECT_EXPLORER.equals(desktopShellLaunchMode)) return false;
        if (desktopShellWinHandlerFallbackAttempted) return false;
        if (trackedWindowCount > 0) return false;
        if (proof == null) return false;
        if (!hasContainerShellExecutable("wfm.exe")) return false;
        if (!proof.explorerProcessPresent) return false;
        if (proof.winebootProcessPresent) return false;
        if (!proof.winebootProcessPresent && !proof.wineserverPresent) return false;
        return proof.bootstrapElapsedMs >= DESKTOP_SHELL_DIRECT_EXPLORER_FALLBACK_DELAY_MS;
    }

    private void attemptDesktopShellWinHandlerFallback(DesktopShellBootstrapProof proof, int attempt) {
        desktopShellWinHandlerFallbackAttempted = true;
        String fallbackExecutable = buildDesktopShellWinHandlerFallbackExecutable();
        boolean launched = launchDetachedGuestProgram(fallbackExecutable, "desktop_shell_winhandler_fallback", "");
        if (launched) {
            desktopShellLaunchMode = DESKTOP_SHELL_LAUNCH_MODE_WINHANDLER;
            desktopShellDetachedFallbackActive = true;
            cancelDeferredGuestTermination("desktop_shell_winhandler_fallback_started");
            scheduleDesktopShellWinHandlerInitProbe(attempt);
        }
        ForensicLogger.logEvent(
                this,
                launched ? "warn" : "error",
                launched ? "XSERVER_DESKTOP_SHELL_WINHANDLER_FALLBACK_LAUNCHED" : "XSERVER_DESKTOP_SHELL_WINHANDLER_FALLBACK_FAILED",
                null,
                "xserver",
                launched ? "desktop_shell_winhandler_fallback_launched" : "desktop_shell_winhandler_fallback_failed",
                ForensicLogger.fields(
                        "attempt", attempt + 1,
                        "tracked_window_count", getTrackedApplicationWindowCount(),
                        "bootstrap_elapsed_ms", proof != null ? proof.bootstrapElapsedMs : 0L,
                        "shell_process_present", proof != null && proof.explorerProcessPresent,
                        "wineboot_process_present", proof != null && proof.winebootProcessPresent,
                        "wineserver_present", proof != null && proof.wineserverPresent,
                        "fallback_guest_executable", fallbackExecutable,
                        "wfm_present", hasContainerShellExecutable("wfm.exe"),
                        "desktop_shell_launch_mode", desktopShellLaunchMode
                )
        );
    }

    private void scheduleDesktopShellWinHandlerInitProbe(int attempt) {
        if (handler == null) return;
        handler.postDelayed(() -> {
            if (isFinishing() || isDestroyed() || guestVisualReady) return;
            if (!desktopShellBootstrapActive || !desktopShellWinHandlerFallbackAttempted) return;

            DesktopShellBootstrapProof proof = collectDesktopShellBootstrapProof();
            boolean winHandlerReady = winHandler != null && winHandler.isReady();
            int trackedWindowCount = getTrackedApplicationWindowCount();
            if (winHandlerReady || trackedWindowCount > 0) return;

            if (trackedWindowCount == 0) {
                logBootstrapWindowSnapshot(
                        "XSERVER_WINDOW_FRONTIER_FALLBACK_TIMEOUT",
                        "desktop_shell_window_frontier_fallback_timeout"
                );
            }
            ForensicLogger.logEvent(
                    this,
                    "warning",
                    "XSERVER_DESKTOP_SHELL_WINHANDLER_INIT_TIMEOUT",
                    null,
                    "xserver",
                    "desktop_shell_winhandler_init_not_received",
                    ForensicLogger.fields(
                            "attempt", attempt + 1,
                            "shell_launcher_present", proof.shellLauncherPresent,
                            "shell_process_present", proof.explorerProcessPresent,
                            "winhandler_process_present", proof.winHandlerProcessPresent,
                            "wfm_process_present", proof.wfmProcessPresent,
                            "wineboot_process_present", proof.winebootProcessPresent,
                            "wineserver_present", proof.wineserverPresent,
                            "bootstrap_elapsed_ms", proof.bootstrapElapsedMs,
                            "desktop_shell_launch_mode", desktopShellLaunchMode,
                            "tracked_window_count", trackedWindowCount,
                            "winhandler_ready", winHandlerReady,
                            "fallback_active", desktopShellDetachedFallbackActive
                    )
            );
        }, DESKTOP_SHELL_WINHANDLER_INIT_TIMEOUT_MS);
    }

    private DesktopShellBootstrapProof collectDesktopShellBootstrapProof() {
        DesktopShellBootstrapProof proof = new DesktopShellBootstrapProof();
        proof.bootstrapElapsedMs = Math.max(0L, System.currentTimeMillis() - desktopShellBootstrapStartedAtMs);
        if (shortcut != null) return proof;

        String[] entries = new File("/proc").list();
        if (entries == null) return proof;
        for (String entry : entries) {
            if (entry == null || entry.isEmpty()) continue;
            for (int i = 0; i < entry.length(); i++) {
                if (!Character.isDigit(entry.charAt(i))) {
                    entry = null;
                    break;
                }
            }
            if (entry == null) continue;
            String commandLine = readProcCmdline(Integer.parseInt(entry)).toLowerCase(Locale.US);
            if (commandLine.isEmpty()) continue;
            String processImage = firstProcImage(commandLine);
            if (commandLine.contains("explorer /desktop=shell")
                    || commandLine.contains("explorer.exe /desktop=shell")) {
                proof.shellLauncherPresent = true;
            }
            if (procImageMatches(processImage, "explorer.exe")) proof.explorerProcessPresent = true;
            if (procImageMatches(processImage, "winhandler.exe")) proof.winHandlerProcessPresent = true;
            if (procImageMatches(processImage, "wfm.exe")) proof.wfmProcessPresent = true;
            if (procImageMatches(processImage, "wineboot.exe")) proof.winebootProcessPresent = true;
            if (procImageMatches(processImage, "wineserver") || commandLine.equals("wineserver")) {
                proof.wineserverPresent = true;
            }
        }
        return proof;
    }

    private String firstProcImage(String commandLine) {
        if (commandLine == null) return "";
        String normalized = commandLine.trim();
        if (normalized.isEmpty()) return "";
        int firstSpace = normalized.indexOf(' ');
        String image = firstSpace >= 0 ? normalized.substring(0, firstSpace) : normalized;
        if (image.startsWith("\"") && image.endsWith("\"") && image.length() > 1) {
            image = image.substring(1, image.length() - 1);
        }
        return image.replace('/', '\\');
    }

    private boolean procImageMatches(String processImage, String executableName) {
        if (processImage == null || executableName == null) return false;
        String image = processImage.trim().toLowerCase(Locale.US);
        String executable = executableName.trim().toLowerCase(Locale.US);
        if (image.isEmpty() || executable.isEmpty()) return false;
        return image.equals(executable) || image.endsWith("\\" + executable);
    }

    private boolean isTrackedVisualWindow(@Nullable Window window) {
        return window != null
                && xServer != null
                && window.isTrackedVisualWindow(xServer.windowManager.rootWindow);
    }

    private void logBootstrapWindowCandidate(String eventId, String severity, String message, @Nullable Window window) {
        if (window == null || xServer == null) return;
        Window rootWindow = xServer.windowManager.rootWindow;
        String className = window.getClassName() != null ? window.getClassName().trim() : "";
        String title = window.getName() != null ? window.getName().trim() : "";
        Window parent = window.getParent();
        int windowGroup = window.getWMHintsValue(Window.WMHints.WINDOW_GROUP);
        ForensicLogger.logEvent(
                this,
                severity,
                eventId,
                null,
                "xserver",
                message,
                ForensicLogger.fields(
                        "window_id", window.id,
                        "class_name", className,
                        "title", title,
                        "process_id", window.getProcessId(),
                        "window_handle", String.format(Locale.US, "0x%x", window.getHandle()),
                        "mapped", window.attributes.isMapped(),
                        "renderable", window.isRenderable(),
                        "strict_application_window", window.isApplicationWindow(),
                        "tracked_visual_window", window.isTrackedVisualWindow(rootWindow),
                        "identity_hints", window.hasIdentityHints(),
                        "parent_is_root", parent == rootWindow,
                        "window_group", String.format(Locale.US, "0x%x", windowGroup),
                        "geometry", window.getWidth() + "x" + window.getHeight(),
                        "tracked_window_count", getTrackedApplicationWindowCount(),
                        "desktop_shell_bootstrap", desktopShellBootstrapActive
                )
        );
    }

    private void logBootstrapWindowSnapshot(String eventId, String message) {
        if (xServer == null) return;
        ArrayList<Window> frontierWindows = new ArrayList<>();
        int renderableCount = 0;
        int trackedVisualCount = 0;
        int mappedCount = 0;
        int unmappedCount = 0;
        int rootChildCount = 0;
        boolean rootSubstructureRedirect = false;
        boolean rootSubstructureNotify = false;
        Window rootWindow = xServer.windowManager.rootWindow;
        try (XLock lock = xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
            rootSubstructureRedirect = rootWindow.hasEventListenerFor(Event.SUBSTRUCTURE_REDIRECT);
            rootSubstructureNotify = rootWindow.hasEventListenerFor(Event.SUBSTRUCTURE_NOTIFY);
            rootChildCount = rootWindow.getChildren().size();
            collectWindowFrontier(rootWindow, frontierWindows);
            for (Window window : frontierWindows) {
                if (window.attributes.isMapped()) mappedCount++;
                else unmappedCount++;
                if (window.isRenderable()) renderableCount++;
                if (window.isTrackedVisualWindow(rootWindow)) trackedVisualCount++;
            }
        }
        DesktopShellBootstrapProof proof = desktopShellBootstrapActive
                ? collectDesktopShellBootstrapProof()
                : null;

        ForensicLogger.logEvent(
                this,
                "info",
                eventId,
                null,
                "xserver",
                message,
                ForensicLogger.fields(
                        "created_window_total", frontierWindows.size(),
                        "mapped_window_total", mappedCount,
                        "unmapped_window_total", unmappedCount,
                        "renderable_window_total", renderableCount,
                        "tracked_visual_window_total", trackedVisualCount,
                        "tracked_window_count", getTrackedApplicationWindowCount(),
                        "root_child_count", rootChildCount,
                        "root_substructure_redirect", rootSubstructureRedirect,
                        "root_substructure_notify", rootSubstructureNotify,
                        "desktop_shell_bootstrap", desktopShellBootstrapActive,
                        "desktop_shell_launch_mode", desktopShellLaunchMode,
                        "bootstrap_elapsed_ms", proof != null ? proof.bootstrapElapsedMs : 0L,
                        "shell_launcher_present", proof != null && proof.shellLauncherPresent,
                        "shell_process_present", proof != null && proof.explorerProcessPresent,
                        "winhandler_process_present", proof != null && proof.winHandlerProcessPresent,
                        "wfm_process_present", proof != null && proof.wfmProcessPresent,
                        "wineboot_process_present", proof != null && proof.winebootProcessPresent,
                        "wineserver_present", proof != null && proof.wineserverPresent
                )
        );

        int limit = Math.min(12, frontierWindows.size());
        for (int i = 0; i < limit; i++) {
            logBootstrapWindowCandidate(
                    "XSERVER_WINDOW_FRONTIER_ENTRY",
                    "info",
                    "desktop_shell_window_frontier_entry",
                    frontierWindows.get(i)
            );
        }
    }

    private void collectWindowFrontier(@Nullable Window window, ArrayList<Window> out) {
        if (window == null) return;
        if (window != xServer.windowManager.rootWindow) {
            out.add(window);
        }
        for (Window child : window.getChildren()) {
            collectWindowFrontier(child, out);
        }
    }

    private String readProcCmdline(int pid) {
        File cmdlineFile = new File(String.format(Locale.US, "/proc/%d/cmdline", pid));
        if (!cmdlineFile.isFile()) return "";
        try (FileInputStream inputStream = new FileInputStream(cmdlineFile)) {
            byte[] buffer = new byte[4096];
            int count = inputStream.read(buffer);
            if (count <= 0) return "";
            StringBuilder builder = new StringBuilder(count);
            for (int i = 0; i < count; i++) {
                char ch = (char) buffer[i];
                builder.append(ch == '\0' ? ' ' : ch);
            }
            return builder.toString().trim().replaceAll("\\s+", " ");
        } catch (IOException ignored) {
            return "";
        }
    }

    private static final class DesktopShellBootstrapProof {
        boolean shellLauncherPresent;
        boolean explorerProcessPresent;
        boolean winHandlerProcessPresent;
        boolean wfmProcessPresent;
        boolean winebootProcessPresent;
        boolean wineserverPresent;
        long bootstrapElapsedMs;

        boolean hasRuntimeLivenessProof(boolean winHandlerReady) {
            return shellLauncherPresent
                    || explorerProcessPresent
                    || winHandlerProcessPresent
                    || wfmProcessPresent
                    || winebootProcessPresent
                    || wineserverPresent
                    || winHandlerReady;
        }

        boolean hasProcessProof(boolean winHandlerReady, boolean requireWinHandler) {
            if (!requireWinHandler) {
                return shellLauncherPresent || explorerProcessPresent || winebootProcessPresent || wineserverPresent;
            }
            if (!winHandlerReady) return false;
            return (shellLauncherPresent || explorerProcessPresent)
                    && (winHandlerProcessPresent || wfmProcessPresent);
        }

        boolean hasVisualProof(int trackedWindowCount) {
            return trackedWindowCount > 0;
        }
    }

    private void cancelDeferredDesktopRuntimePause(String reason) {
        if (!deferredDesktopPauseScheduled) return;
        runtimePauseHandler.removeCallbacks(deferredDesktopPauseRunnable);
        deferredDesktopPauseScheduled = false;
        deferredDesktopPauseDeadlineAtMs = 0L;
        ForensicLogger.logEvent(
                this,
                "info",
                "XSERVER_RUNTIME_PAUSE_CANCELLED",
                null,
                "xserver",
                "desktop_runtime_pause_cancelled",
                ForensicLogger.fields(
                        "reason", reason,
                        "desktop_shell_bootstrap", desktopShellBootstrapActive,
                        "tracked_window_count", getTrackedApplicationWindowCount()
                )
        );
    }

    private void pauseDesktopRuntime(String reason) {
        if (environment != null) {
            environment.onPause();
            pauseXServerViewSurface("runtime_pause_" + reason);
        }

        savePlaytimeData();
        handler.removeCallbacks(savePlaytimeRunnable);
        ProcessHelper.pauseAllWineProcesses();
        ForensicLogger.logEvent(
                this,
                "info",
                "XSERVER_RUNTIME_PAUSED",
                null,
                "xserver",
                "desktop_runtime_paused",
                ForensicLogger.fields(
                        "reason", reason,
                        "desktop_shell_bootstrap", desktopShellBootstrapActive,
                        "tracked_window_count", getTrackedApplicationWindowCount(),
                        "runtime_drawer_visible", runtimeDrawerVisible
                )
        );
    }

    private String resolveCurrentLaunchRuntimeModelMarker() {
        String normalized = ContentProfile.normalizeRuntimeModel(effectiveRuntimeModel);
        if (!normalized.isEmpty()) return normalized;
        if (container != null) {
            normalized = ContentProfile.normalizeRuntimeModel(container.getContainerVariant());
        }
        return normalized.isEmpty() ? ContentProfile.RUNTIME_MODEL_BIONIC : normalized;
    }

    private String resolveCurrentLaunchWineVersionMarker() {
        if (selectedRuntimeProfile != null) {
            return ContentsManager.getEntryName(selectedRuntimeProfile);
        }
        String requested = resolveLaunchWineVersion();
        String runtimeModel = resolveCurrentLaunchRuntimeModelMarker();
        String resolved = resolveEffectiveLaunchWineVersion(requested, runtimeModel);
        return resolved == null ? "" : resolved.trim();
    }

    private boolean markerMismatch(String target, String stored) {
        String normalizedTarget = target == null ? "" : target.trim();
        String normalizedStored = stored == null ? "" : stored.trim();
        return !normalizedTarget.isEmpty()
                && !normalizedStored.isEmpty()
                && !normalizedTarget.equalsIgnoreCase(normalizedStored);
    }

    private boolean markerMissingOrMismatch(String target, String stored) {
        String normalizedTarget = target == null ? "" : target.trim();
        String normalizedStored = stored == null ? "" : stored.trim();
        return !normalizedTarget.isEmpty()
                && (normalizedStored.isEmpty() || !normalizedTarget.equalsIgnoreCase(normalizedStored));
    }

    private void setupWineSystemFiles() {
        ensureWinePrefixReady();
        String appVersion = String.valueOf(AppUtils.getVersionCode(this));
        String imgVersion = String.valueOf(imageFs.getVersion());
        String launchRuntimeModel = resolveCurrentLaunchRuntimeModelMarker();
        String launchWineVersion = resolveCurrentLaunchWineVersionMarker();
        String appliedContainerVariant = container.getExtra(EXTRA_APPLIED_CONTAINER_VARIANT);
        String appliedRuntimeModel = container.getExtra(EXTRA_APPLIED_RUNTIME_MODEL);
        String appliedWineVersion = container.getExtra(EXTRA_APPLIED_WINE_VERSION);
        boolean containerDataChanged = false;
        boolean appOrImageChanged = !container.getExtra("appVersion").equals(appVersion)
                || !container.getExtra("imgVersion").equals(imgVersion);
        boolean runtimeModelChanged = markerMissingOrMismatch(launchRuntimeModel, appliedRuntimeModel);
        boolean containerVariantChanged = markerMissingOrMismatch(launchRuntimeModel, appliedContainerVariant);
        boolean wineVersionChanged = markerMissingOrMismatch(launchWineVersion, appliedWineVersion);

        if (appOrImageChanged || runtimeModelChanged || containerVariantChanged || wineVersionChanged) {
            applyGeneralPatches(container);
            container.putExtra("appVersion", appVersion);
            container.putExtra("imgVersion", imgVersion);
            container.putExtra(EXTRA_APPLIED_CONTAINER_VARIANT, launchRuntimeModel);
            container.putExtra(EXTRA_APPLIED_RUNTIME_MODEL, launchRuntimeModel);
            container.putExtra(EXTRA_APPLIED_WINE_VERSION, launchWineVersion);
            firstTimeBoot = true;
            containerDataChanged = true;
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "WINE_SYSTEM_FILES_RUNTIME_MARKERS_APPLIED",
                    null,
                    "xserver",
                    "wine_system_files_runtime_markers_applied",
                    ForensicLogger.fields(
                            "container_id", container.id,
                            "app_or_image_changed", appOrImageChanged,
                            "runtime_model_changed", runtimeModelChanged,
                            "container_variant_changed", containerVariantChanged,
                            "wine_version_changed", wineVersionChanged,
                            "launch_runtime_model", launchRuntimeModel,
                            "launch_wine_version", launchWineVersion,
                            "previous_applied_runtime_model", appliedRuntimeModel,
                            "previous_applied_wine_version", appliedWineVersion
                    )
            );
        }

        ensureWinePrefixEssentialFiles();

        String dxwrapperMode = this.dxwrapper;
        String dxwrapperSignature = dxwrapperMode;

        if (dxwrapperMode.contains("dxvk")) {
            String dxvkWrapper = sanitizeConfiguredWrapperVersion(dxwrapperConfig.get("version"), DefaultVersion.DXVK);
            String vkd3dWrapper = sanitizeConfiguredWrapperVersion(dxwrapperConfig.get("vkd3dVersion"), "None");
            dxwrapperSignature = "dxvk:" + dxvkWrapper + ":" + vkd3dWrapper;
        } else if (dxwrapperMode.contains("dgvoodoo")) {
            KeyValueSet dgConfig = DgVoodooConfigDialog.parseConfig(dxwrapperConfig);
            String archRequested = DgVoodooConfigDialog.normalizeArch(dgConfig.get("dgvoodooArch"));
            String versionHint = dgConfig.get("dgvoodooVersionHint");
            boolean dgVoodooVulkanBridge = supportsDgVoodooVulkanBridge(graphicsDriver);
            String dxvkWrapper = DgVoodooConfigDialog.resolveCompanionDxvkVersion(
                    dgConfig,
                    archRequested,
                    dgVoodooVulkanBridge,
                    contentsManager.getInstalledVersionNames(ContentProfile.ContentType.CONTENT_TYPE_DXVK, true)
            );
            String vkd3dWrapper = DgVoodooConfigDialog.resolveCompanionVkd3dVersion(dgConfig, dgVoodooVulkanBridge);
            String forceD3d11 = DgVoodooConfigDialog.resolveCompanionForceD3d11(dgConfig, dgVoodooVulkanBridge) ? "1" : "0";
            dxwrapperSignature = "dgvoodoo:" + archRequested + ":" + versionHint + ":" + dxvkWrapper + ":" + vkd3dWrapper + ":" + forceD3d11 + ":" + graphicsDriver;
        }

        if (!dxwrapperSignature.equals(container.getExtra("dxwrapper"))) {
            extractDXWrapperFiles(dxwrapperMode);
            container.putExtra("dxwrapper", dxwrapperSignature);
            containerDataChanged = true;
        }

        String wincomponents = shortcut != null ? shortcut.getExtra("wincomponents", container.getWinComponents()) : container.getWinComponents();
        if (!wincomponents.equals(container.getExtra("wincomponents"))) {
            extractWinComponentFiles();
            container.putExtra("wincomponents", wincomponents);
            containerDataChanged = true;
        }
        if (syncOpenAlRuntimeDlls()) {
            containerDataChanged = true;
        }

        String desktopTheme = container.getDesktopTheme();
        if (!(desktopTheme+","+xServer.screenInfo).equals(container.getExtra("desktopTheme"))) {
            File themeRootDir = container != null ? container.getRootDir() : ImageFs.find(this).getRootDir();
            WineThemeManager.apply(this, new WineThemeManager.ThemeInfo(desktopTheme), xServer.screenInfo, themeRootDir);
            container.putExtra("desktopTheme", desktopTheme+","+xServer.screenInfo);
            containerDataChanged = true;
        }

        WineStartMenuCreator.create(this, container);
        WineUtils.createDosdevicesSymlinks(container);
        logNoexecDosDriveState(container.getRootDir());

        if (shortcut != null)
            startupSelection = shortcut.getExtra("startupSelection", String.valueOf(container.getStartupSelection()));
        else
            startupSelection = String.valueOf(container.getStartupSelection());

        WineUtils.changeServicesStatus(container, Byte.parseByte(startupSelection) != Container.STARTUP_SELECTION_NORMAL);
        if (!startupSelection.equals(container.getExtra("startupSelection"))) {
            container.putExtra("startupSelection", startupSelection);
            containerDataChanged = true;
        }

        extractInputDLLs();

        if (containerDataChanged) container.saveData();
    }

    private void ensureWinePrefixReady() {
        if (container == null || wineInfo == null || containerManager == null) return;

        File containerDir = container.getRootDir();
        boolean prefixInvalid = !WineUtils.isPrefixValid(containerDir);
        String storedPrefixArch = container.getExtra("wineprefixArch");
        String targetRuntimeModel = resolveCurrentLaunchRuntimeModelMarker();
        String targetWineVersion = resolveCurrentLaunchWineVersionMarker();
        String storedPrefixRuntimeModel = container.getExtra(EXTRA_WINEPREFIX_RUNTIME_MODEL);
        String storedPrefixWineVersion = container.getExtra(EXTRA_WINEPREFIX_WINE_VERSION);
        if (storedPrefixWineVersion.isEmpty()) {
            storedPrefixWineVersion = container.getExtra(EXTRA_APPLIED_WINE_VERSION);
        }
        boolean archMismatch = !storedPrefixArch.isEmpty() && !storedPrefixArch.equalsIgnoreCase(wineInfo.getArch());
        boolean wineVersionMismatch = markerMismatch(targetWineVersion, storedPrefixWineVersion);
        boolean prefixNeedsUpdate = "t".equalsIgnoreCase(container.getExtra("wineprefixNeedsUpdate"));

        ForensicLogger.logEvent(
                this,
                prefixInvalid || archMismatch || wineVersionMismatch || prefixNeedsUpdate ? "warn" : "info",
                "WINE_PREFIX_HEALTH_EVAL",
                null,
                "xserver",
                "wine_prefix_health_evaluated",
                ForensicLogger.fields(
                        "container_id", container.id,
                        "prefix_invalid", prefixInvalid,
                        "arch_mismatch", archMismatch,
                        "stored_arch", storedPrefixArch,
                        "target_arch", wineInfo.getArch(),
                        "target_runtime_model", targetRuntimeModel,
                        "stored_prefix_runtime_model", storedPrefixRuntimeModel,
                        "target_wine_version", targetWineVersion,
                        "stored_prefix_wine_version", storedPrefixWineVersion,
                        "wine_version_mismatch", wineVersionMismatch,
                        "prefix_needs_update", prefixNeedsUpdate
                )
        );

        if (!prefixInvalid && !archMismatch && !wineVersionMismatch && !prefixNeedsUpdate) {
            if (storedPrefixArch.isEmpty()
                    || storedPrefixRuntimeModel.isEmpty()
                    || container.getExtra(EXTRA_WINEPREFIX_WINE_VERSION).isEmpty()) {
                container.putExtra("wineprefixArch", wineInfo.getArch());
                container.putExtra(EXTRA_WINEPREFIX_RUNTIME_MODEL, targetRuntimeModel);
                container.putExtra(EXTRA_WINEPREFIX_WINE_VERSION, targetWineVersion);
                container.putExtra("wineprefixNeedsUpdate", null);
                container.saveData();
            }
            return;
        }

        String repairWineVersion = selectedRuntimeProfile != null
                ? ContentsManager.getEntryName(selectedRuntimeProfile)
                : resolveEffectiveLaunchWineVersion(resolveLaunchWineVersion(), effectiveRuntimeModel);
        boolean repaired = containerManager.repairContainerWinePrefix(
                container,
                repairWineVersion,
                contentsManager,
                onExtractFileListener
        );
        ForensicLogger.logEvent(
                this,
                repaired ? "warn" : "error",
                repaired ? "WINE_PREFIX_REPAIR_APPLIED" : "WINE_PREFIX_REPAIR_FAILED",
                null,
                "xserver",
                repaired ? "wine_prefix_repair_applied" : "wine_prefix_repair_failed",
                ForensicLogger.fields(
                        "container_id", container.id,
                        "prefix_invalid", prefixInvalid,
                        "arch_mismatch", archMismatch,
                        "stored_arch", storedPrefixArch,
                        "target_arch", wineInfo.getArch(),
                        "target_runtime_model", targetRuntimeModel,
                        "stored_prefix_runtime_model", storedPrefixRuntimeModel,
                        "target_wine_version", targetWineVersion,
                        "stored_prefix_wine_version", storedPrefixWineVersion,
                        "wine_version_mismatch", wineVersionMismatch,
                        "prefix_needs_update", prefixNeedsUpdate
                )
        );
        if (repaired) {
            firstTimeBoot = true;
        }
    }

    private void ensureWinePrefixEssentialFiles() {
        if (container == null || imageFs == null) return;
        File containerWindowsDir = new File(WineUtils.resolveHostWineDriveCRoot(container.getRootDir()), "windows");
        String[] essentialFiles = {"winhandler.exe", "wfm.exe"};
        ArrayList<String> missingFiles = new ArrayList<>();
        for (String filename : essentialFiles) {
            if (!isUsableWineBridgeFile(new File(containerWindowsDir, filename))) {
                missingFiles.add(filename);
            }
        }

        ForensicLogger.logEvent(
                this,
                missingFiles.isEmpty() ? "info" : "warn",
                missingFiles.isEmpty() ? "WINE_PREFIX_BRIDGE_FILES_READY" : "WINE_PREFIX_BRIDGE_FILES_INCOMPLETE",
                null,
                "xserver",
                missingFiles.isEmpty() ? "wine_prefix_bridge_files_ready" : "wine_prefix_bridge_files_incomplete",
                ForensicLogger.fields(
                        "container_id", container.id,
                        "windows_dir", containerWindowsDir.getAbsolutePath(),
                        "missing_files", String.join(",", missingFiles)
                )
        );

        if (missingFiles.isEmpty()) return;

        File homeRoot = new File(imageFs.getRootDir(), "home");
        File[] homeDirs = homeRoot.listFiles();
        File sourceWindowsDir = null;
        if (homeDirs != null) {
            for (File dir : homeDirs) {
                if (dir == null || !dir.isDirectory()) continue;
                if (dir.getName().equals(ImageFs.USER)) continue;
                if (container.getRootDir() != null && dir.getAbsolutePath().equals(container.getRootDir().getAbsolutePath())) continue;
                File candidate = new File(dir, ".wine/drive_c/windows");
                if (isUsableWineBridgeFile(new File(candidate, "winhandler.exe"))
                        && isUsableWineBridgeFile(new File(candidate, "wfm.exe"))) {
                    sourceWindowsDir = candidate;
                    break;
                }
            }
        }

        containerWindowsDir.mkdirs();
        ArrayList<String> restoredFromContainer = new ArrayList<>();
        if (sourceWindowsDir != null) {
            for (String filename : missingFiles) {
                File source = new File(sourceWindowsDir, filename);
                File dest = new File(containerWindowsDir, filename);
                if (!isUsableWineBridgeFile(source)) continue;
                FileUtils.copy(source, dest);
                if (isUsableWineBridgeFile(dest)) {
                    restoredFromContainer.add(filename);
                }
            }
            ForensicLogger.logEvent(
                    this,
                    restoredFromContainer.isEmpty() ? "warn" : "info",
                    "WINE_PREFIX_BRIDGE_FILES_RESTORED_FROM_CONTAINER",
                    null,
                    "xserver",
                    "wine_prefix_bridge_files_restored_from_container",
                    ForensicLogger.fields(
                            "container_id", container.id,
                            "source_windows_dir", sourceWindowsDir.getAbsolutePath(),
                            "restored_files", String.join(",", restoredFromContainer),
                            "requested_files", String.join(",", missingFiles)
                    )
            );
        }

        if (restoredFromContainer.size() == missingFiles.size()) return;

        TarCompressorUtils.extract(
                TarCompressorUtils.Type.ZSTD,
                this,
                "container_pattern_common.tzst",
                imageFs.getRootDir(),
                onExtractFileListener
        );

        ArrayList<String> readyFiles = new ArrayList<>();
        for (String filename : essentialFiles) {
            if (isUsableWineBridgeFile(new File(containerWindowsDir, filename))) {
                readyFiles.add(filename);
            }
        }
        boolean ready = readyFiles.size() == essentialFiles.length;
        ForensicLogger.logEvent(
                this,
                ready ? "warn" : "error",
                ready ? "WINE_PREFIX_BRIDGE_FILES_RESTORED_FROM_ARCHIVE" : "WINE_PREFIX_BRIDGE_FILES_RESTORE_FAILED",
                null,
                "xserver",
                ready ? "wine_prefix_bridge_files_restored_from_archive" : "wine_prefix_bridge_files_restore_failed",
                ForensicLogger.fields(
                        "container_id", container.id,
                        "windows_dir", containerWindowsDir.getAbsolutePath(),
                        "ready_files", String.join(",", readyFiles),
                        "requested_files", String.join(",", missingFiles)
                )
        );
    }

    private boolean isUsableWineBridgeFile(@Nullable File file) {
        return file != null && file.isFile() && file.length() > 0;
    }

    private void setupXEnvironment() throws PackageManager.NameNotFoundException {
        ForensicConfig.Snapshot forensicSnapshot = ForensicConfig.load(this);
        composeLaunchEnvVars(forensicSnapshot);

        // Clear any temporary directory
        String rootPath = imageFs.getRootDir().getPath();
        ImageFsInstaller.ensureRootfsLaunchLayout(this, imageFs);
        FileUtils.clear(imageFs.getTmpDir());

        int bindingPathCount = 0;
        synchronized (mappedApplicationWindowIds) {
            mappedApplicationWindowIds.clear();
        }
        guestVisualReady = false;
        guestLauncherExited = false;
        guestLauncherExitStatus = Integer.MIN_VALUE;
        desktopShellBootstrapActive = false;
        desktopShellLaunchMode = DESKTOP_SHELL_LAUNCH_MODE_WINHANDLER;
        desktopShellWinHandlerFallbackAttempted = false;
        desktopShellDetachedFallbackActive = false;
        desktopShellLiveNoWindowHorizonLogged = false;
        desktopShellBootstrapStartedAtMs = 0L;
        cancelDeferredGuestTermination("setup_xenvironment");

        guestProgramLauncherComponent = GuestProgramLauncherFactory.create(
                imageFs,
                contentsManager,
                selectedRuntimeProfile != null
                        ? selectedRuntimeProfile
                        : contentsManager.resolveBestRuntimeProfile(container.getWineVersion(), effectiveRuntimeModel),
                shortcut,
                effectiveRuntimeModel
        );

        // Additional container checks and environment configuration
        if (container != null) {
            String launchVariant = effectiveRuntimeModel == null || effectiveRuntimeModel.trim().isEmpty()
                    ? container.getContainerVariant()
                    : effectiveRuntimeModel;
            imageFs.createVariantFile(launchVariant);
            imageFs.createArchFile(container.getWineVersion());
            if (!imageFs.getRootfsProviderFile().exists()) {
                imageFs.createRootfsProviderFile(imageFs.getRootfsProvider());
            }
            if (!imageFs.getRootfsLayoutFile().exists()) {
                imageFs.createRootfsLayoutFile(imageFs.getRootfsLayout());
            }
            if (Byte.parseByte(startupSelection) == Container.STARTUP_SELECTION_AGGRESSIVE) {
                winHandler.killProcess("services.exe");
            }
            guestProgramLauncherComponent.setContainer(this.container);
            guestProgramLauncherComponent.setWineInfo(this.wineInfo);
            ensureBionicGraphicsDriverRegistry();

            if (shortcut == null) {
                configureDesktopShellRegistry();
            }
            String guestExecutable = buildGuestExecutable();
            boolean desktopShellLaunch = shortcut == null && isDesktopShellGuestExecutable(guestExecutable);
            desktopShellBootstrapActive = desktopShellLaunch;
            if (desktopShellBootstrapActive) {
                if (desktopShellBootstrapStartedAtMs == 0L) {
                    desktopShellBootstrapStartedAtMs = System.currentTimeMillis();
                }
            } else {
                desktopShellBootstrapStartedAtMs = 0L;
            }

            guestProgramLauncherComponent.setGuestExecutable(guestExecutable);

            ArrayList<String> bindingPaths = new ArrayList<>();
            for (String[] drive : container.drivesIterator()) {
                bindingPaths.add(drive[1]);
            }
            bindingPathCount = bindingPaths.size();

            guestProgramLauncherComponent.setBindingPaths(bindingPaths.toArray(new String[0]));

            guestProgramLauncherComponent.setBox64Preset(
                    shortcut != null
                            ? shortcut.getExtra("box64Preset", container.getBox64Preset())
                            : container.getBox64Preset()
            );

            guestProgramLauncherComponent.setFEXCorePreset(
                    shortcut != null
                            ? shortcut.getExtra("fexcorePreset", container.getFEXCorePreset())
                            : container.getFEXCorePreset()
            );
        }

        // Create our overall XEnvironment with various components
        environment = new XEnvironment(this, imageFs);
        environment.addComponent(
                new SysVSharedMemoryComponent(
                        xServer,
                        UnixSocketConfig.createSocket(rootPath, UnixSocketConfig.SYSVSHM_SERVER_PATH)
                )
        );
        environment.addComponent(
                new XServerComponent(
                        xServer,
                        UnixSocketConfig.createSocket(rootPath, UnixSocketConfig.XSERVER_PATH)
                )
        );
        environment.addComponent(new NetworkInfoUpdateComponent());
        if (shouldAttachSteamClientComponent()) {
            environment.addComponent(new SteamClientComponent());
        }
        if (openWithAndroidBrowserEnabled || shareAndroidClipboardEnabled) {
            environment.addComponent(new WineRequestComponent());
        }

        // Audio driver logic
        if (audioDriver.equals("alsa")) {
            envVars.put("ANDROID_ALSA_SERVER", rootPath + UnixSocketConfig.ALSA_SERVER_PATH);
            envVars.put("ANDROID_ASERVER_USE_SHM", "true");
            environment.addComponent(
                    new ALSAServerComponent(
                            UnixSocketConfig.createSocket(rootPath, UnixSocketConfig.ALSA_SERVER_PATH),
                            ALSAClient.Options.fromEnvVars(envVars)
                    )
            );
        } else if (audioDriver.equals("pulseaudio")) {
            envVars.put("PULSE_SERVER", rootPath + UnixSocketConfig.PULSE_SERVER_PATH);
            environment.addComponent(
                    new PulseAudioComponent(
                            UnixSocketConfig.createSocket(rootPath, UnixSocketConfig.PULSE_SERVER_PATH)
                    )
            );
        }

        if (GraphicsDrivers.isVortek(graphicsDriver)) {
            VortekRendererComponent.Options options =
                    VortekRendererComponent.Options.fromKeyValueSet(this, getGraphicsDriverKeyValueConfig());
            environment.addComponent(
                    new VortekRendererComponent(
                            xServer,
                            UnixSocketConfig.createSocket(rootPath, UnixSocketConfig.VORTEK_SERVER_PATH),
                            options,
                            this
                    )
            );
        }
        if (GraphicsDrivers.isVirgl(graphicsDriver)) {
            environment.addComponent(
                    new VirGLRendererComponent(
                            xServer,
                            UnixSocketConfig.createSocket(rootPath, UnixSocketConfig.VIRGL_SERVER_PATH)
                    )
            );
        }

        // Pass final envVars to the launcher
        guestProgramLauncherComponent.setEnvVars(envVars);
        guestProgramLauncherComponent.setTerminationCallback((status) -> {
            guestLauncherExited = true;
            guestLauncherExitStatus = status;
            maybeLaunchDesktopShellFallbackOnPrimaryTermination(status);
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "GUEST_PROGRAM_TERMINATED",
                    null,
                    "xserver",
                    "guest_program_terminated",
                    ForensicLogger.fields(
                            "status", status
                    )
            );
            if (shouldDeferGuestTermination(status)) {
                int trackedWindowCount;
                synchronized (mappedApplicationWindowIds) {
                    trackedWindowCount = mappedApplicationWindowIds.size();
                }
                scheduleDeferredGuestTermination(status);
                ForensicLogger.logEvent(
                        this,
                        "info",
                        "GUEST_PROGRAM_TERMINATION_DEFERRED",
                        null,
                        "xserver",
                        "guest_program_termination_deferred",
                        ForensicLogger.fields(
                                "status", status,
                                "tracked_window_count", trackedWindowCount,
                                "desktop_shell_bootstrap", desktopShellBootstrapActive,
                                "bootstrap_elapsed_ms", Math.max(0L, System.currentTimeMillis() - desktopShellBootstrapStartedAtMs),
                                "termination_grace_ms", DESKTOP_SHELL_TERMINATION_GRACE_MS,
                                "bootstrap_horizon_ms", DESKTOP_SHELL_BOOTSTRAP_HORIZON_MS,
                                "fallback_active", desktopShellDetachedFallbackActive
                        )
                );
                return;
            }
            exit();
        });

        // Add the launcher to our environment
        environment.addComponent(guestProgramLauncherComponent);

        ForensicLogger.logEvent(
                this,
                "info",
                "RUNTIME_ENV_COMPONENTS_PREPARED",
                null,
                "xserver",
                "runtime_environment_components_prepared",
                ForensicLogger.fields(
                        "audio_driver", audioDriver,
                        "startup_selection", startupSelection,
                        "wine_version", container != null ? container.getWineVersion() : "",
                        "binding_paths_count", bindingPathCount,
                        "has_wine_request_component", openWithAndroidBrowserEnabled || shareAndroidClipboardEnabled
                )
        );

        // Start WinHandler before any guest launch so INIT cannot race a late UDP bind.
        winHandler.start();
        winHandler.preAssignConnectedControllers();

        // Start all environment components (XServer, Audio, etc.)
        environment.startEnvironmentComponents();
        scheduleDesktopShellPreloaderFallback();

        ForensicLogger.logEvent(
                this,
                "info",
                "RUNTIME_ENV_COMPONENTS_STARTED",
                null,
                "xserver",
                "runtime_environment_components_started",
                ForensicLogger.fields(
                        "audio_driver", audioDriver,
                        "has_wine_request_component", openWithAndroidBrowserEnabled || shareAndroidClipboardEnabled
                )
        );

        // Reset dxwrapper config
        dxwrapperConfig = null;

    }

    private void createWrapperScript(String path, String content) {
        File scriptFile = new File(path);
        FileUtils.writeString(scriptFile, content);
        scriptFile.setExecutable(true);
    }

    private void setupUI() {
        FrameLayout rootView = findViewById(R.id.FLXServerDisplay);
        xServerView = new XServerView(this, xServer);
        final GLRenderer renderer = xServerView.getRenderer();
        renderer.setCursorVisible(false);
        renderer.setDesktopCursorOwnershipMode(shortcut == null);

        if (shortcut != null) {
            renderer.setUnviewableWMClasses("explorer.exe");
        }

        xServer.setRenderer(renderer);
        rootView.addView(xServerView);

        globalCursorSpeed = preferences.getFloat("cursor_speed", 1.0f);
        touchpadView = new TouchpadView(this, xServer, timeoutHandler, hideControlsRunnable);
        touchpadView.setSensitivity(globalCursorSpeed);
        touchpadView.setFourFingersTapCallback(this::toggleRuntimeDrawer);
        touchpadView.setLeftEdgeSwipeCallback(this::showRuntimeDrawer);
        rootView.addView(touchpadView);
        applyDesktopGestureExclusion(rootView);
        applyDesktopGestureExclusion(xserverRootView);
        applyDesktopGestureExclusion(touchpadView);

        inputControlsView = new InputControlsView(this, timeoutHandler, hideControlsRunnable);
        inputControlsView.setOverlayOpacity(preferences.getFloat("overlay_opacity", InputControlsView.DEFAULT_OVERLAY_OPACITY));
        inputControlsView.setTouchpadView(touchpadView);
        inputControlsView.setXServer(xServer);
        inputControlsView.setVisibility(View.GONE);
        rootView.addView(inputControlsView);


        startTouchscreenTimeout();

        // Inside onCreate(), after initializing controls
        boolean isTimeoutEnabled = preferences.getBoolean("touchscreen_timeout_enabled", false);
        if (isTimeoutEnabled) {
            startTouchscreenTimeout();
        }

        if (container != null && container.isShowFPS()) {
            frameRating = new FrameRating(this, graphicsDriverConfig);
            frameRating.setVisibility(View.GONE);
            rootView.addView(frameRating);
        }

        boolean shouldStretch = false;
        if (shortcut != null && !shortcut.getExtra("fullscreenStretched").isEmpty()) {
            shouldStretch = shortcut.getExtraBoolean("fullscreenStretched", false);
        } else if (container != null && container.isFullscreenStretched()) {
            shouldStretch = true;
        }

        if (shouldStretch) {
            // Toggle fullscreen mode based on the final decision
            renderer.toggleFullscreen();
            touchpadView.toggleFullscreen();
        }

        if (shortcut != null) {
            String controlsProfile = shortcut.getExtra("controlsProfile");
            if (!controlsProfile.isEmpty()) {
                ControlsProfile profile = inputControlsManager.getProfile(Integer.parseInt(controlsProfile));
                if (profile != null) showInputControls(profile);
            }

            touchpadView.setTapToClickMovesCursor(false);
            touchpadView.setSimTouchScreen(shortcut.getExtraBoolean("simTouchScreen", false));
            applyShortcutTouchpadGestureProfile();
        } else {
            isRelativeMouseMovement = false;
            xServer.setRelativeMouseMovement(false);
            touchpadView.setTapToClickMovesCursor(false);
            touchpadView.setSimTouchScreen(false);
            renderer.setCursorVisible(true);
            renderer.setDesktopCursorOwnershipMode(true);
            int centerX = xServer.screenInfo.width / 2;
            int centerY = xServer.screenInfo.height / 2;
            xServer.injectPointerMove(centerX, centerY);
            touchpadView.setTrackpadCursorPosition(centerX, centerY);
            // Desktop sessions need a slightly more forgiving tap envelope than
            // game/shortcut launches, otherwise trackpad taps are too easy to
            // miss on high-density phones.
            touchpadView.setGestureRuntimeTuning(260, 20, 100, 350);
            touchpadView.setStrictGestureFsmOverride(false);
            xServerView.requestLifecycleRender("desktop_input_model_ready");
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "DESKTOP_INPUT_MODEL_APPLIED",
                    null,
                    "xserver",
                    "desktop_input_model_applied",
                    ForensicLogger.fields(
                            "shortcut_launch", false,
                            "simulate_touchscreen", touchpadView.isSimTouchScreen(),
                            "relative_mouse", xServer.isRelativeMouseMovement(),
                            "cursor_visible", true,
                            "tap_to_click_moves_cursor", false,
                            "desktop_cursor_owner_mode", true,
                            "tap_timeout_ms", 260,
                            "tap_travel_px", 20,
                            "pointer_x", xServer.pointer.getClampedX(),
                            "pointer_y", xServer.pointer.getClampedY(),
                            "input_mode", "cursor_trackpad"
                    )
            );
        }

        AppUtils.observeSoftKeyboardVisibility(xserverRootView != null ? xserverRootView : rootView, renderer::setScreenOffsetYRelativeToCursor);
    }

    private void applyDesktopGestureExclusion(View targetView) {
        if (targetView == null || shortcut != null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;

        if (desktopGestureExclusionTrackedViews.add(targetView)) {
            targetView.post(() -> updateDesktopGestureExclusionRects(targetView));
            targetView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                    updateDesktopGestureExclusionRects(targetView));
        }
    }

    private void armGuestBootstrapAfterFirstDraw(View targetView, String source) {
        if (targetView == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) return;

        ViewTreeObserver observer = targetView.getViewTreeObserver();
        if (!observer.isAlive()) return;

        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                ViewTreeObserver currentObserver = targetView.getViewTreeObserver();
                if (currentObserver.isAlive()) currentObserver.removeOnPreDrawListener(this);
                if (bootstrapFirstDrawObserved) return true;

                bootstrapFirstDrawObserved = true;
                logBootstrapCheckpoint(
                        "XSERVER_BOOTSTRAP_DRAW_GATE_PASSED",
                        "guest_bootstrap_draw_gate_passed_after_first_activity_draw",
                        "source", source,
                        "activity_has_focus", hasWindowFocus(),
                        "view_width", targetView.getWidth(),
                        "view_height", targetView.getHeight()
                );
                refreshDesktopGestureExclusion();
                maybeRunPendingGuestBootstrap(source);
                return true;
            }
        });
    }

    private void updateDesktopGestureExclusionRects(View targetView) {
        if (targetView == null || shortcut != null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;
        if (targetView.getWindowToken() == null || !targetView.isAttachedToWindow()) return;

        int width = targetView.getWidth();
        int height = targetView.getHeight();
        if (width <= 0 || height <= 0) return;

        String stateKey = System.identityHashCode(targetView.getWindowToken()) + ":" + width + "x" + height;
        if (stateKey.equals(desktopGestureExclusionLastState.get(targetView))) return;

        ArrayList<Rect> exclusionRects = new ArrayList<>();
        exclusionRects.add(new Rect(0, 0, width, height));
        try {
            targetView.setSystemGestureExclusionRects(exclusionRects);
            desktopGestureExclusionLastState.put(targetView, stateKey);
        }
        catch (RuntimeException error) {
            ForensicLogger.error(
                    this,
                    "DESKTOP_GESTURE_EXCLUSION_FAILED",
                    null,
                    "xserver",
                    "desktop_gesture_exclusion_failed",
                    error,
                    ForensicLogger.fields(
                            "view_width", width,
                            "view_height", height,
                            "window_token_present", targetView.getWindowToken() != null,
                            "attached_to_window", targetView.isAttachedToWindow()
                    )
            );
            return;
        }

        ForensicLogger.logEvent(
                this,
                "info",
                "DESKTOP_GESTURE_EXCLUSION_APPLIED",
                null,
                "xserver",
                "desktop_gesture_exclusion_applied",
                ForensicLogger.fields(
                        "shortcut_launch", shortcut != null,
                        "exclusion_mode", "full_view",
                        "view_width", width,
                        "view_height", height
                )
        );
    }

    private void refreshDesktopGestureExclusion() {
        if (shortcut != null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;
        updateDesktopGestureExclusionRects(xserverRootView);
        View desktopHostView = findViewById(R.id.FLXServerDisplay);
        updateDesktopGestureExclusionRects(desktopHostView);
        updateDesktopGestureExclusionRects(touchpadView);
        if (preloaderDialog != null) {
            preloaderDialog.refreshWindowState();
        }
    }

    public boolean launchDetachedGuestProgram(String guestExecutable, String source, String installTarget) {
        if (guestProgramLauncherComponent == null) {
            ForensicLogger.logEvent(
                    this,
                    "error",
                    "DETACHED_GUEST_PROGRAM_MISSING_LAUNCHER",
                    null,
                    "xserver",
                    "detached_guest_program_missing_launcher",
                    ForensicLogger.fields(
                            "source", source != null ? source : "",
                            "install_target", installTarget != null ? installTarget : ""
                    )
            );
            return false;
        }

        String normalizedExecutable = guestExecutable != null ? guestExecutable.trim() : "";
        if (normalizedExecutable.isEmpty()) {
            return false;
        }

        bindActiveContainerState(container, effectiveRuntimeModel, resolveActiveRuntimeIdentity());
        prepareRootfsDevInputPath();

        int detachedPid = guestProgramLauncherComponent.launchDetachedGuestProgram(
                normalizedExecutable,
                (status) -> {
                    boolean desktopShellFallback = "desktop_shell_winhandler_fallback".equals(source);
                    DesktopShellBootstrapProof proof = null;
                    boolean winHandlerReady = false;
                    int trackedWindowCount = getTrackedApplicationWindowCount();
                    if (desktopShellFallback && desktopShellBootstrapActive) {
                        proof = collectDesktopShellBootstrapProof();
                        winHandlerReady = winHandler != null && winHandler.isReady();
                    }
                    if (desktopShellFallback) {
                        desktopShellDetachedFallbackActive = false;
                    }
                    ForensicLogger.logEvent(
                            this,
                            "info",
                            "DETACHED_GUEST_PROGRAM_TERMINATED",
                            null,
                            "xserver",
                            "detached_guest_program_terminated",
                            ForensicLogger.fields(
                                    "source", source != null ? source : "",
                                    "install_target", installTarget != null ? installTarget : "",
                                    "status", status,
                                    "guest_executable", normalizedExecutable,
                                    "tracked_window_count", trackedWindowCount,
                                    "winhandler_ready", winHandlerReady,
                                    "shell_process_present", proof != null && proof.explorerProcessPresent,
                                    "winhandler_process_present", proof != null && proof.winHandlerProcessPresent,
                                    "wfm_process_present", proof != null && proof.wfmProcessPresent,
                                    "wineboot_process_present", proof != null && proof.winebootProcessPresent,
                                    "wineserver_present", proof != null && proof.wineserverPresent
                            )
                    );
                    if (desktopShellFallback) {
                        ForensicLogger.logEvent(
                                this,
                                "warning",
                                "XSERVER_DESKTOP_SHELL_FALLBACK_TERMINATION_STATE",
                                null,
                                "xserver",
                                "desktop_shell_fallback_termination_state",
                                ForensicLogger.fields(
                                        "status", status,
                                        "tracked_window_count", trackedWindowCount,
                                        "winhandler_ready", winHandlerReady,
                                        "shell_process_present", proof != null && proof.explorerProcessPresent,
                                        "winhandler_process_present", proof != null && proof.winHandlerProcessPresent,
                                        "wfm_process_present", proof != null && proof.wfmProcessPresent,
                                        "wineboot_process_present", proof != null && proof.winebootProcessPresent,
                                        "wineserver_present", proof != null && proof.wineserverPresent,
                                        "bootstrap_elapsed_ms", Math.max(0L, System.currentTimeMillis() - desktopShellBootstrapStartedAtMs)
                                )
                        );
                    }
                    if (desktopShellFallback
                            && desktopShellBootstrapActive
                            && guestLauncherExited
                            && trackedWindowCount == 0
                            && !guestVisualReady) {
                        logBootstrapWindowSnapshot(
                                "XSERVER_WINDOW_FRONTIER_FALLBACK_TERMINATION",
                                "desktop_shell_window_frontier_fallback_termination"
                        );
                        ForensicLogger.logEvent(
                                this,
                                "warn",
                                "XSERVER_DESKTOP_SHELL_FALLBACK_TERMINATED_NO_VISUAL_READY",
                                null,
                                "xserver",
                                "desktop_shell_fallback_terminated_without_visual_ready",
                                ForensicLogger.fields(
                                        "status", status,
                                        "guest_launcher_exit_status", guestLauncherExitStatus,
                                        "bootstrap_elapsed_ms", Math.max(0L, System.currentTimeMillis() - desktopShellBootstrapStartedAtMs),
                                        "winhandler_ready", winHandlerReady,
                                        "shell_process_present", proof != null && proof.explorerProcessPresent,
                                        "winhandler_process_present", proof != null && proof.winHandlerProcessPresent,
                                        "wfm_process_present", proof != null && proof.wfmProcessPresent,
                                        "wineboot_process_present", proof != null && proof.winebootProcessPresent,
                                        "wineserver_present", proof != null && proof.wineserverPresent
                                )
                        );
                        scheduleDeferredGuestTermination(guestLauncherExitStatus);
                    }
                }
        );
        if (detachedPid == -1) {
            ForensicLogger.logEvent(
                    this,
                    "error",
                    "DETACHED_GUEST_PROGRAM_START_FAILED",
                    null,
                    "xserver",
                    "detached_guest_program_start_failed",
                    ForensicLogger.fields(
                            "source", source != null ? source : "",
                            "install_target", installTarget != null ? installTarget : "",
                            "guest_executable", normalizedExecutable
                    )
            );
            return false;
        }

        ForensicLogger.logEvent(
                this,
                "info",
                "DETACHED_GUEST_PROGRAM_STARTED",
                null,
                "xserver",
                "detached_guest_program_started",
                ForensicLogger.fields(
                        "source", source != null ? source : "",
                        "install_target", installTarget != null ? installTarget : "",
                        "pid", detachedPid,
                        "guest_executable", normalizedExecutable
                )
        );
        return true;
    }

    private void handleDesktopBackNavigation() {
        if (runtimeDrawerVisible) {
            hideRuntimeDrawer();
            return;
        }

        if (environment != null) {
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "DESKTOP_BACK_GESTURE_CONSUMED",
                    null,
                    "xserver",
                    "desktop_back_gesture_consumed",
                    ForensicLogger.fields(
                            "shortcut_launch", shortcut != null,
                            "runtime_drawer_visible", runtimeDrawerVisible,
                            "desktop_shell_bootstrap", desktopShellBootstrapActive
                    )
            );
            return;
        }

        finish();
    }

    private void applyShortcutTouchpadGestureProfile() {
        if (touchpadView == null || shortcut == null) return;

        String profileIdRaw = shortcut.getExtra("touchpadGestureProfile", TOUCHPAD_PROFILE_GLOBAL);
        String profileId = profileIdRaw == null
                ? TOUCHPAD_PROFILE_GLOBAL
                : profileIdRaw.trim().toLowerCase(Locale.ENGLISH);
        if (profileId.isEmpty()) profileId = TOUCHPAD_PROFILE_GLOBAL;

        TouchpadGestureDefaults defaults = resolveTouchpadGestureDefaults(profileId);
        String strictRaw = shortcut.getExtra("touchpadStrictGestureFsm", "");
        String tapTimeoutRaw = shortcut.getExtra("touchpadTapTimeoutMs", "");
        String tapTravelRaw = shortcut.getExtra("touchpadTapTravelPx", "");
        String scrollStepRaw = shortcut.getExtra("touchpadScrollStepPx", "");
        String scrollZoneRaw = shortcut.getExtra("touchpadScrollZonePx", "");

        boolean strict = strictRaw.isEmpty() ? defaults.strictFsm : parseBoolean(strictRaw);
        int tapTimeoutMs = parseBoundedInt(tapTimeoutRaw, defaults.tapTimeoutMs, 80, 500);
        int tapTravelPx = parseBoundedInt(tapTravelRaw, defaults.tapTravelPx, 4, 24);
        int scrollStepPx = parseBoundedInt(scrollStepRaw, defaults.scrollStepPx, 40, 240);
        int scrollZonePx = parseBoundedInt(scrollZoneRaw, defaults.scrollZonePx, 120, 700);

        touchpadView.setGestureRuntimeTuning(tapTimeoutMs, tapTravelPx, scrollStepPx, scrollZonePx);
        touchpadView.setStrictGestureFsmOverride(strict);

        if (TOUCHPAD_PROFILE_GLOBAL.equals(profileId)
                && strictRaw.isEmpty()
                && tapTimeoutRaw.isEmpty()
                && tapTravelRaw.isEmpty()
                && scrollStepRaw.isEmpty()
                && scrollZoneRaw.isEmpty()) {
            touchpadView.resetGestureRuntimeTuning();
        }

        ForensicLogger.logEvent(
                this,
                "info",
                "TOUCHPAD_PROFILE_APPLIED",
                shortcut.path,
                "touchpad",
                "gesture_profile_applied",
                ForensicLogger.fields(
                        "profile_id", profileId,
                        "strict_fsm", strict ? 1 : 0,
                        "tap_timeout_ms", tapTimeoutMs,
                        "tap_travel_px", tapTravelPx,
                        "scroll_step_px", scrollStepPx,
                        "scroll_zone_px", scrollZonePx
                )
        );
    }

    private TouchpadGestureDefaults resolveTouchpadGestureDefaults(String profileId) {
        return switch (profileId) {
            case TOUCHPAD_PROFILE_BALANCED -> new TouchpadGestureDefaults(true, 190, 10, 95, 350);
            case TOUCHPAD_PROFILE_AGGRESSIVE -> new TouchpadGestureDefaults(true, 145, 8, 75, 300);
            case TOUCHPAD_PROFILE_COMPAT -> new TouchpadGestureDefaults(false, 240, 14, 130, 430);
            case TOUCHPAD_PROFILE_GLOBAL -> new TouchpadGestureDefaults(false, 200, 10, 100, 350);
            default -> new TouchpadGestureDefaults(true, 190, 10, 95, 350);
        };
    }

    private int parseBoundedInt(String raw, int fallback, int min, int max) {
        if (raw == null || raw.trim().isEmpty()) return fallback;
        int parsed = safeParseInt(raw.trim());
        if (parsed <= 0) return fallback;
        return Math.max(min, Math.min(max, parsed));
    }

    private int parseBoundedIntAllowZero(String raw, int fallback, int min, int max) {
        if (raw == null || raw.trim().isEmpty()) return fallback;
        int parsed = safeParseInt(raw.trim());
        return Math.max(min, Math.min(max, parsed));
    }

    private static final class ResolvedUpscalerValue {
        private final String value;
        private final String source;

        private ResolvedUpscalerValue(String value, String source) {
            this.value = value;
            this.source = source;
        }
    }

    private ResolvedUpscalerValue resolveUpscalerValue(
            @Nullable Shortcut activeShortcut,
            @NonNull String key,
            @NonNull String fallback
    ) {
        if (activeShortcut != null) {
            String shortcutValue = activeShortcut.getExtra(key, "");
            if (shortcutValue != null && !shortcutValue.trim().isEmpty()) {
                return new ResolvedUpscalerValue(shortcutValue, "shortcut");
            }
        }
        if (container != null) {
            String containerValue = container.getExtra(key, "");
            if (containerValue != null && !containerValue.trim().isEmpty()) {
                return new ResolvedUpscalerValue(containerValue, "container");
            }
        }
        return new ResolvedUpscalerValue(fallback, "global_profile");
    }

    private void parseUpscalerLaunchSettings(@Nullable Shortcut activeShortcut) {
        upscalerDeprecatedAliasUsed = false;
        UpscalerProfileStore.Profile globalProfile = UpscalerProfileStore.getSelectedProfile(preferences);
        ResolvedUpscalerValue backendSetting =
                resolveUpscalerValue(activeShortcut, "upscalerBackend", globalProfile.backend);
        String backendRaw = StringUtils.parseIdentifier(backendSetting.value);
        if ("mobfgsr".equals(backendRaw)) upscalerDeprecatedAliasUsed = true;
        String backend = backendRaw;
        if (!UPSCALER_BACKEND_VKBASALT.equals(backend) && !UPSCALER_BACKEND_LSFG.equals(backend)) {
            backend = UpscalerProfileStore.normalizeBackend(globalProfile.backend);
        }
        upscalerBackendSource = backendSetting.source;

        String effect = normalizeUpscalerEffect(
                resolveUpscalerValue(activeShortcut, "upscalerEffect", globalProfile.effect).value
        );

        ResolvedUpscalerValue presetSetting =
                resolveUpscalerValue(activeShortcut, "upscalerPreset", globalProfile.preset);
        String presetRaw = presetSetting.value;
        if (presetRaw == null || presetRaw.trim().isEmpty()) presetRaw = UPSCALER_PRESET_AUTO;
        upscalerPreset = normalizeUpscalerPreset(presetRaw);
        upscalerPresetSource = presetSetting.source;
        upscalerBackend = backend;
        upscalerEffect = effect;

        String scaleRaw = resolveUpscalerValue(
                activeShortcut,
                "upscalerScale",
                String.valueOf(globalProfile.scalePercent)
        ).value;
        upscalerScalePercent = parseBoundedInt(
                scaleRaw,
                100,
                100,
                200
        );

        String sharpnessRaw = resolveUpscalerValue(
                activeShortcut,
                "upscalerSharpness",
                String.valueOf(globalProfile.sharpness)
        ).value;
        upscalerSharpnessPercent = parseBoundedIntAllowZero(
                sharpnessRaw,
                100,
                0,
                100
        );

        String denoiseRaw = resolveUpscalerValue(
                activeShortcut,
                "upscalerDenoise",
                String.valueOf(globalProfile.denoise)
        ).value;
        upscalerDenoisePercent = parseBoundedIntAllowZero(
                denoiseRaw,
                100,
                0,
                100
        );

        ResolvedUpscalerValue framegenSetting = resolveUpscalerValue(
                activeShortcut,
                "upscalerFrameGeneration",
                globalProfile.frameGeneration ? "1" : "0"
        );
        upscalerFrameGeneration = parseBoolean(framegenSetting.value);
        upscalerFramegenSource = framegenSetting.source;

        String generatedFramesRaw = resolveUpscalerValue(
                activeShortcut,
                "upscalerGeneratedFrames",
                String.valueOf(globalProfile.generatedFrames)
        ).value;
        upscalerGeneratedFrames = parseBoundedInt(
                generatedFramesRaw,
                1,
                1,
                3
        );

        String fgSourceRaw = resolveUpscalerValue(activeShortcut, "upscalerFgSource", globalProfile.fgSource).value;
        upscalerFgSource = normalizeFgSource(fgSourceRaw);

        String fgOutputRaw = resolveUpscalerValue(activeShortcut, "upscalerFgOutput", globalProfile.fgOutput).value;
        if ("mobfgsr".equals(StringUtils.parseIdentifier(fgOutputRaw))) upscalerDeprecatedAliasUsed = true;
        upscalerFgOutput = normalizeFgOutput(fgOutputRaw);

        String framegenModeRaw = resolveUpscalerValue(
                activeShortcut,
                "upscalerFramegenMode",
                globalProfile.framegenMode
        ).value;
        upscalerFramegenMode = normalizeFramegenMode(framegenModeRaw);

        String thermalGuardRaw = resolveUpscalerValue(
                activeShortcut,
                "upscalerThermalGuard",
                globalProfile.thermalGuard ? "1" : "0"
        ).value;
        upscalerThermalGuard = parseBoolean(thermalGuardRaw);

        String targetFpsRaw = resolveUpscalerValue(
                activeShortcut,
                "upscalerTargetFps",
                String.valueOf(globalProfile.targetFps)
        ).value;
        upscalerTargetFps = parseBoundedIntAllowZero(
                targetFpsRaw,
                60,
                30,
                144
        );

        String interpolationRaw = resolveUpscalerValue(
                activeShortcut,
                "upscalerInterpolationFactor",
                String.valueOf(globalProfile.interpolationFactor)
        ).value;
        upscalerInterpolationFactor = parseBoundedIntAllowZero(
                interpolationRaw,
                50,
                0,
                100
        );

        String debugOverlayRaw = resolveUpscalerValue(
                activeShortcut,
                "upscalerDebugOverlay",
                globalProfile.debugOverlay ? "1" : "0"
        ).value;
        upscalerDebugOverlay = parseBoolean(debugOverlayRaw);

        String debugTearRaw = resolveUpscalerValue(
                activeShortcut,
                "upscalerDebugTearLines",
                globalProfile.debugTearLines ? "1" : "0"
        ).value;
        upscalerDebugTearLines = parseBoolean(debugTearRaw);

        String interpolatedOnlyRaw = resolveUpscalerValue(
                activeShortcut,
                "upscalerInterpolatedOnly",
                globalProfile.interpolatedOnly ? "1" : "0"
        ).value;
        upscalerInterpolatedOnly = parseBoolean(interpolatedOnlyRaw);

        ResolvedUpscalerValue vkValidationSetting = resolveUpscalerValue(
                activeShortcut,
                "vulkanValidationLayer",
                globalProfile.vulkanValidationLayer ? "1" : "0"
        );
        upscalerVulkanValidationLayer = parseBoolean(vkValidationSetting.value);
        upscalerValidationSource = vkValidationSetting.source;
        vkbasaltConfig = buildVkBasaltConfig(upscalerEffect, upscalerSharpnessPercent, upscalerDenoisePercent);
    }

    private String normalizeUpscalerEffect(String effect) {
        String normalized = StringUtils.parseIdentifier(effect);
        return switch (normalized) {
            case "cas", "dls", "fsr", "nis" -> normalized;
            default -> UPSCALER_EFFECT_NONE;
        };
    }

    private String normalizeUpscalerPreset(String preset) {
        String normalized = StringUtils.parseIdentifier(preset);
        return switch (normalized) {
            case UPSCALER_PRESET_CONSERVATIVE -> UPSCALER_PRESET_CONSERVATIVE;
            case UPSCALER_PRESET_BALANCED -> UPSCALER_PRESET_BALANCED;
            case UPSCALER_PRESET_AGGRESSIVE -> UPSCALER_PRESET_AGGRESSIVE;
            default -> UPSCALER_PRESET_AUTO;
        };
    }

    private String resolveUpscalerPresetForSoc(String requestedPreset, String socClass) {
        String normalizedRequested = normalizeUpscalerPreset(requestedPreset);
        if (!UPSCALER_PRESET_AUTO.equals(normalizedRequested)) {
            return normalizedRequested;
        }
        if (socClass == null || socClass.trim().isEmpty()) {
            return UPSCALER_PRESET_BALANCED;
        }
        return switch (socClass) {
            case "adreno-7xx" -> UPSCALER_PRESET_AGGRESSIVE;
            case "adreno-6xx-and-older" -> UPSCALER_PRESET_CONSERVATIVE;
            case "mali-g7xx-or-newer", "xclipse-rdna-mobile" -> UPSCALER_PRESET_BALANCED;
            default -> UPSCALER_PRESET_BALANCED;
        };
    }

    private String normalizeFramegenMode(String mode) {
        String normalized = StringUtils.parseIdentifier(mode);
        return switch (normalized) {
            case FRAMEGEN_MODE_QUALITY -> FRAMEGEN_MODE_QUALITY;
            case FRAMEGEN_MODE_LOW_LATENCY, "low-latency" -> FRAMEGEN_MODE_LOW_LATENCY;
            default -> FRAMEGEN_MODE_BALANCED;
        };
    }

    private String normalizeFgSource(String source) {
        String normalized = StringUtils.parseIdentifier(source);
        return switch (normalized) {
            case FG_SOURCE_OPTI_FG, "optifg" -> FG_SOURCE_OPTI_FG;
            default -> FG_SOURCE_NATIVE;
        };
    }

    private String normalizeFgOutput(String output) {
        String normalized = StringUtils.parseIdentifier(output);
        return switch (normalized) {
            case FG_OUTPUT_LSFG -> FG_OUTPUT_LSFG;
            case "dlssg_to_fsr3", "dlssg-to-fsr3", "dlssgtofsr3" -> FG_OUTPUT_LSFG;
            default -> FG_OUTPUT_AUTO;
        };
    }

    private String buildVkBasaltConfig(String effect, int sharpnessPercent, int denoisePercent) {
        String normalizedEffect = normalizeUpscalerEffect(effect);
        if (UPSCALER_EFFECT_NONE.equals(normalizedEffect)) return "";
        float sharpness = sharpnessPercent / 100.0f;
        float denoise = denoisePercent / 100.0f;
        return "effects=" + normalizedEffect + ";"
                + "casSharpness=" + sharpness + ";"
                + "dlsSharpness=" + sharpness + ";"
                + "dlsDenoise=" + denoise + ";"
                + "fsrSharpness=" + sharpness + ";"
                + "nisSharpness=" + sharpness + ";"
                + "enableOnLaunch=True";
    }

    private void applyUpscalerEnvVars(boolean dxvkBackedRoute, String socClass) {
        String guardReason = "none";
        String normalizedSocClass = socClass == null || socClass.trim().isEmpty() ? "unknown" : socClass.trim();
        boolean upscalerEnabled = !UPSCALER_BACKEND_OFF.equals(upscalerBackend)
                && !UPSCALER_EFFECT_NONE.equals(upscalerEffect);
        boolean frameGenerationRequested = upscalerFrameGeneration && upscalerEnabled;
        boolean frameGenerationBackendSupported = UPSCALER_BACKEND_LSFG.equals(upscalerBackend);
        boolean frameGenerationActive = frameGenerationRequested && frameGenerationBackendSupported;
        if (frameGenerationRequested && !frameGenerationBackendSupported) {
            guardReason = "framegen_requires_lsfg_backend";
        }
        String resolvedFgOutput = upscalerFgOutput;
        if (FG_OUTPUT_AUTO.equals(resolvedFgOutput)) {
            resolvedFgOutput = UPSCALER_BACKEND_LSFG.equals(upscalerBackend) ? FG_OUTPUT_LSFG : FG_OUTPUT_AUTO;
        }
        String requestedPreset = normalizeUpscalerPreset(upscalerPreset);
        String effectivePreset = resolveUpscalerPresetForSoc(requestedPreset, normalizedSocClass);
        int effectiveGeneratedFrames = upscalerGeneratedFrames;
        int effectiveTargetFps = upscalerTargetFps;
        int effectiveInterpolationFactor = upscalerInterpolationFactor;
        boolean effectiveThermalGuard = upscalerThermalGuard;
        float presetModeScaleMultiplier = 1.0f;
        float presetModeQualityMultiplier = 1.0f;
        float presetModeBudgetMultiplier = 1.0f;
        float depthDiffThresholdSr = 0.0100f;
        float colorDiffThresholdFg = 0.0100f;
        float depthDiffThresholdFg = 0.0040f;
        switch (effectivePreset) {
            case UPSCALER_PRESET_CONSERVATIVE -> {
                effectiveGeneratedFrames = Math.min(effectiveGeneratedFrames, 1);
                effectiveTargetFps = Math.min(effectiveTargetFps, 60);
                effectiveInterpolationFactor = Math.min(effectiveInterpolationFactor, 45);
                effectiveThermalGuard = true;
                presetModeScaleMultiplier = 0.85f;
                presetModeQualityMultiplier = 0.85f;
                presetModeBudgetMultiplier = 0.90f;
                depthDiffThresholdSr = 0.0150f;
                colorDiffThresholdFg = 0.0130f;
                depthDiffThresholdFg = 0.0060f;
            }
            case UPSCALER_PRESET_AGGRESSIVE -> {
                effectiveGeneratedFrames = Math.min(effectiveGeneratedFrames, 3);
                effectiveTargetFps = Math.min(effectiveTargetFps, 144);
                effectiveInterpolationFactor = Math.min(effectiveInterpolationFactor, 100);
                presetModeScaleMultiplier = 1.15f;
                presetModeQualityMultiplier = 1.10f;
                presetModeBudgetMultiplier = 1.10f;
                depthDiffThresholdSr = 0.0075f;
                colorDiffThresholdFg = 0.0085f;
                depthDiffThresholdFg = 0.0030f;
            }
            default -> {
                effectiveGeneratedFrames = Math.min(effectiveGeneratedFrames, 2);
                effectiveTargetFps = Math.min(effectiveTargetFps, 90);
                effectiveInterpolationFactor = Math.min(effectiveInterpolationFactor, 65);
            }
        }

        boolean lsfgDebugBridgeActive = frameGenerationActive && UPSCALER_BACKEND_LSFG.equals(upscalerBackend);
        float modeScale;
        float modeQuality;
        float modeBudgetMs;
        switch (upscalerFramegenMode) {
            case FRAMEGEN_MODE_QUALITY -> {
                modeScale = 0.75f;
                modeQuality = 0.80f;
                modeBudgetMs = 11.0f;
            }
            case FRAMEGEN_MODE_LOW_LATENCY -> {
                modeScale = 0.35f;
                modeQuality = 0.30f;
                modeBudgetMs = 6.5f;
            }
            default -> {
                modeScale = 0.50f;
                modeQuality = 0.50f;
                modeBudgetMs = 8.0f;
            }
        }
        modeScale = Math.max(0.20f, Math.min(1.25f, modeScale * presetModeScaleMultiplier));
        modeQuality = Math.max(0.20f, Math.min(1.20f, modeQuality * presetModeQualityMultiplier));
        modeBudgetMs = Math.max(4.0f, Math.min(14.0f, modeBudgetMs * presetModeBudgetMultiplier));

        if (frameGenerationActive && !dxvkBackedRoute) {
            frameGenerationActive = false;
            if ("none".equals(guardReason)) {
                guardReason = "framegen_requires_dxvk_route";
            }
            lsfgDebugBridgeActive = false;
        }
        if (!frameGenerationActive) {
            resolvedFgOutput = "off";
        }

        setOrClearEnv("AERO_UPSCALER_ENABLED", upscalerEnabled ? "1" : "0");
        setOrClearEnv("AERO_UPSCALER_BACKEND", upscalerBackend);
        setOrClearEnv("AERO_UPSCALER_EFFECT", upscalerEffect);
        setOrClearEnv("AERO_UPSCALER_PRESET_REQUESTED", requestedPreset);
        setOrClearEnv("AERO_UPSCALER_PRESET_EFFECTIVE", effectivePreset);
        setOrClearEnv("AERO_UPSCALER_SOC_CLASS", normalizedSocClass);
        setOrClearEnv("AERO_UPSCALER_SCALE_PERCENT", String.valueOf(upscalerScalePercent));
        setOrClearEnv("AERO_UPSCALER_SHARPNESS_PERCENT", String.valueOf(upscalerSharpnessPercent));
        setOrClearEnv("AERO_UPSCALER_DENOISE_PERCENT", String.valueOf(upscalerDenoisePercent));
        boolean requestedValidationLayer = upscalerEnabled && upscalerVulkanValidationLayer;
        Set<String> availableVkLayers = resolveAvailableVulkanLayerNames(envVars);
        boolean validationLayerAvailable = availableVkLayers.contains("VK_LAYER_KHRONOS_validation");
        boolean upscalerValidationLayerActive = requestedValidationLayer && validationLayerAvailable;
        if (requestedValidationLayer && !validationLayerAvailable && "none".equals(guardReason)) {
            guardReason = "vk_validation_layer_missing";
        }
        setOrClearEnv("AERO_VK_VALIDATION_REQUESTED", requestedValidationLayer ? "1" : "0");
        setOrClearEnv("AERO_VK_VALIDATION_LAYER", upscalerValidationLayerActive ? "1" : "0");
        setOrClearEnv(
                "AERO_VK_VALIDATION_GUARD",
                requestedValidationLayer && !validationLayerAvailable
                        ? "vulkan_validation_layer_missing"
                        : ""
        );
        String vkLayers = removeVkInstanceLayer(envVars.get("VK_INSTANCE_LAYERS"), "VK_LAYER_KHRONOS_validation");
        if (upscalerValidationLayerActive) {
            vkLayers = appendVkInstanceLayers(vkLayers, "VK_LAYER_KHRONOS_validation");
        } else if (requestedValidationLayer) {
            logMissingVulkanLayer("upscaler", "VK_LAYER_KHRONOS_validation", availableVkLayers);
        }
        setOrClearEnv("VK_INSTANCE_LAYERS", vkLayers);
        setOrClearEnv("AERO_UPSCALER_TARGET_FPS", String.valueOf(effectiveTargetFps));
        setOrClearEnv("AERO_FRAMEGEN_ENABLED", frameGenerationActive ? "1" : "0");
        setOrClearEnv("AERO_FRAMEGEN_GENERATED_FRAMES", String.valueOf(effectiveGeneratedFrames));
        setOrClearEnv("AERO_FRAMEGEN_SOURCE", frameGenerationActive ? upscalerFgSource : "");
        setOrClearEnv("AERO_FRAMEGEN_OUTPUT", resolvedFgOutput);
        setOrClearEnv("AERO_FRAMEGEN_MODE", frameGenerationActive ? upscalerFramegenMode : "");
        setOrClearEnv("AERO_FRAMEGEN_THERMAL_GUARD", frameGenerationActive && effectiveThermalGuard ? "1" : "0");
        setOrClearEnv("AERO_DLSSG_TO_FSR3_BRIDGE", "");
        setOrClearEnv("DLSSGTOFSR3_EnableDebugOverlay", "");
        setOrClearEnv("DLSSGTOFSR3_EnableDebugTearLines", "");
        setOrClearEnv("DLSSGTOFSR3_EnableInterpolatedFramesOnly", "");
        setOrClearEnv(
                "AERO_FRAMEGEN_INTERPOLATION_FACTOR",
                frameGenerationActive ? String.valueOf(effectiveInterpolationFactor) : ""
        );
        setOrClearEnv(
                "AERO_FRAMEGEN_DEBUG_OVERLAY",
                lsfgDebugBridgeActive && upscalerDebugOverlay ? "1" : "0"
        );
        setOrClearEnv(
                "AERO_FRAMEGEN_DEBUG_TEAR_LINES",
                lsfgDebugBridgeActive && upscalerDebugTearLines ? "1" : "0"
        );
        setOrClearEnv(
                "AERO_FRAMEGEN_INTERPOLATED_ONLY",
                lsfgDebugBridgeActive && upscalerInterpolatedOnly ? "1" : "0"
        );

        if (UPSCALER_BACKEND_VKBASALT.equals(upscalerBackend) && upscalerEnabled && !vkbasaltConfig.isEmpty()) {
            envVars.put("ENABLE_VKBASALT", "1");
            envVars.put("VKBASALT_CONFIG", vkbasaltConfig);
            setOrClearEnv("AERO_UPSCALER_PROVIDER", "vkbasalt");
        }
        else {
            envVars.remove("ENABLE_VKBASALT");
            envVars.remove("VKBASALT_CONFIG");
            setOrClearEnv("AERO_UPSCALER_PROVIDER", upscalerEnabled ? upscalerBackend : "");
        }

        if (UPSCALER_BACKEND_LSFG.equals(upscalerBackend) && upscalerEnabled) {
            setOrClearEnv("AERO_LSFG_ENABLE_SR", "1");
            setOrClearEnv("AERO_LSFG_ENABLE_INTERP", frameGenerationActive ? "1" : "0");
            setOrClearEnv("AERO_LSFG_PRESET", effectivePreset);
            setOrClearEnv("AERO_LSFG_SOC_CLASS", normalizedSocClass);
            setOrClearEnv("AERO_LSFG_GENERATED_FRAMES", String.valueOf(effectiveGeneratedFrames));
            setOrClearEnv("AERO_LSFG_RENDER_SCALE", String.format(Locale.US, "%.2f", upscalerScalePercent / 100.0f));
            setOrClearEnv("AERO_LSFG_MODE", upscalerFramegenMode);
            setOrClearEnv("AERO_LSFG_THERMAL_GUARD", effectiveThermalGuard ? "1" : "0");
            setOrClearEnv("AERO_LSFG_FG_SOURCE", upscalerFgSource);
            setOrClearEnv("AERO_LSFG_FG_OUTPUT", resolvedFgOutput);
            setOrClearEnv("AERO_LSFG_MODEL_SCALE", String.format(Locale.US, "%.2f", modeScale));
            setOrClearEnv("AERO_LSFG_QUALITY", String.format(Locale.US, "%.2f", modeQuality));
            setOrClearEnv("AERO_LSFG_FRAME_BUDGET_MS", String.format(Locale.US, "%.2f", modeBudgetMs));
            setOrClearEnv("AERO_LSFG_TARGET_FPS", String.valueOf(effectiveTargetFps));
            setOrClearEnv(
                    "AERO_LSFG_INTERPOLATION_FACTOR",
                    frameGenerationActive ? String.valueOf(effectiveInterpolationFactor) : ""
            );
            setOrClearEnv("AERO_LSFG_DEBUG_OVERLAY", upscalerDebugOverlay ? "1" : "0");
            setOrClearEnv("AERO_LSFG_DEBUG_TEAR_LINES", upscalerDebugTearLines ? "1" : "0");
            setOrClearEnv("AERO_LSFG_INTERPOLATED_ONLY", upscalerInterpolatedOnly ? "1" : "0");
            setOrClearEnv("AERO_LSFG_DEPTH_DIFF_THRESHOLD_SR", String.format(Locale.US, "%.4f", depthDiffThresholdSr));
            setOrClearEnv("AERO_LSFG_COLOR_DIFF_THRESHOLD_FG", String.format(Locale.US, "%.4f", colorDiffThresholdFg));
            setOrClearEnv("AERO_LSFG_DEPTH_DIFF_THRESHOLD_FG", String.format(Locale.US, "%.4f", depthDiffThresholdFg));
        }
        else {
            setOrClearEnv("AERO_LSFG_ENABLE_SR", "");
            setOrClearEnv("AERO_LSFG_ENABLE_INTERP", "");
            setOrClearEnv("AERO_LSFG_PRESET", "");
            setOrClearEnv("AERO_LSFG_SOC_CLASS", "");
            setOrClearEnv("AERO_LSFG_GENERATED_FRAMES", "");
            setOrClearEnv("AERO_LSFG_RENDER_SCALE", "");
            setOrClearEnv("AERO_LSFG_MODE", "");
            setOrClearEnv("AERO_LSFG_THERMAL_GUARD", "");
            setOrClearEnv("AERO_LSFG_FG_SOURCE", "");
            setOrClearEnv("AERO_LSFG_FG_OUTPUT", "");
            setOrClearEnv("AERO_LSFG_MODEL_SCALE", "");
            setOrClearEnv("AERO_LSFG_QUALITY", "");
            setOrClearEnv("AERO_LSFG_FRAME_BUDGET_MS", "");
            setOrClearEnv("AERO_LSFG_TARGET_FPS", "");
            setOrClearEnv("AERO_LSFG_INTERPOLATION_FACTOR", "");
            setOrClearEnv("AERO_LSFG_DEBUG_OVERLAY", "");
            setOrClearEnv("AERO_LSFG_DEBUG_TEAR_LINES", "");
            setOrClearEnv("AERO_LSFG_INTERPOLATED_ONLY", "");
            setOrClearEnv("AERO_LSFG_DEPTH_DIFF_THRESHOLD_SR", "");
            setOrClearEnv("AERO_LSFG_COLOR_DIFF_THRESHOLD_FG", "");
            setOrClearEnv("AERO_LSFG_DEPTH_DIFF_THRESHOLD_FG", "");
        }

        // Legacy mirror export for old runtime consumers during LSFG migration.
        setOrClearEnv("AERO_MOBFGSR_ENABLE_SR", envVars.get("AERO_LSFG_ENABLE_SR"));
        setOrClearEnv("AERO_MOBFGSR_ENABLE_INTERP", envVars.get("AERO_LSFG_ENABLE_INTERP"));
        setOrClearEnv("AERO_MOBFGSR_PRESET", envVars.get("AERO_LSFG_PRESET"));
        setOrClearEnv("AERO_MOBFGSR_SOC_CLASS", envVars.get("AERO_LSFG_SOC_CLASS"));
        setOrClearEnv("AERO_MOBFGSR_GENERATED_FRAMES", envVars.get("AERO_LSFG_GENERATED_FRAMES"));
        setOrClearEnv("AERO_MOBFGSR_RENDER_SCALE", envVars.get("AERO_LSFG_RENDER_SCALE"));
        setOrClearEnv("AERO_MOBFGSR_MODE", envVars.get("AERO_LSFG_MODE"));
        setOrClearEnv("AERO_MOBFGSR_THERMAL_GUARD", envVars.get("AERO_LSFG_THERMAL_GUARD"));
        setOrClearEnv("AERO_MOBFGSR_FG_SOURCE", envVars.get("AERO_LSFG_FG_SOURCE"));
        setOrClearEnv("AERO_MOBFGSR_FG_OUTPUT", envVars.get("AERO_LSFG_FG_OUTPUT"));
        setOrClearEnv("AERO_MOBFGSR_MODEL_SCALE", envVars.get("AERO_LSFG_MODEL_SCALE"));
        setOrClearEnv("AERO_MOBFGSR_QUALITY", envVars.get("AERO_LSFG_QUALITY"));
        setOrClearEnv("AERO_MOBFGSR_FRAME_BUDGET_MS", envVars.get("AERO_LSFG_FRAME_BUDGET_MS"));
        setOrClearEnv("AERO_MOBFGSR_TARGET_FPS", envVars.get("AERO_LSFG_TARGET_FPS"));
        setOrClearEnv("AERO_MOBFGSR_INTERPOLATION_FACTOR", envVars.get("AERO_LSFG_INTERPOLATION_FACTOR"));
        setOrClearEnv("AERO_MOBFGSR_DEBUG_OVERLAY", envVars.get("AERO_LSFG_DEBUG_OVERLAY"));
        setOrClearEnv("AERO_MOBFGSR_DEBUG_TEAR_LINES", envVars.get("AERO_LSFG_DEBUG_TEAR_LINES"));
        setOrClearEnv("AERO_MOBFGSR_INTERPOLATED_ONLY", envVars.get("AERO_LSFG_INTERPOLATED_ONLY"));
        setOrClearEnv("AERO_MOBFGSR_DEPTH_DIFF_THRESHOLD_SR", envVars.get("AERO_LSFG_DEPTH_DIFF_THRESHOLD_SR"));
        setOrClearEnv("AERO_MOBFGSR_COLOR_DIFF_THRESHOLD_FG", envVars.get("AERO_LSFG_COLOR_DIFF_THRESHOLD_FG"));
        setOrClearEnv("AERO_MOBFGSR_DEPTH_DIFF_THRESHOLD_FG", envVars.get("AERO_LSFG_DEPTH_DIFF_THRESHOLD_FG"));
        setOrClearEnv("AERO_FRAMEGEN_BACKEND_ALIAS", upscalerDeprecatedAliasUsed ? "mobfgsr_deprecated_alias" : "");

        String runtimeGuardReason = envVars.get(RuntimeSignalContract.WINLATOR_RUNTIME_PRESET_GUARD_REASON);
        RuntimeSignalContract.putSignalPolicyMarkers(
                envVars,
                "aero-signal-v1",
                "shortcut+graphics+runtime",
                runtimeGuardReason,
                guardReason
        );
        RuntimeSignalContract.putLsfgEffectiveMarkers(
                envVars,
                upscalerBackend,
                frameGenerationActive,
                upscalerFramegenMode,
                upscalerBackendSource + ">" + upscalerPresetSource + ">" + upscalerFramegenSource,
                resolvedFgOutput,
                effectiveGeneratedFrames,
                effectiveThermalGuard,
                upscalerDeprecatedAliasUsed
        );

        ForensicLogger.logEvent(
                this,
                "info",
                "LSFG_CONFIG_EFFECTIVE",
                null,
                "graphics_route",
                "Resolved effective LSFG/framegen launch config",
                ForensicLogger.fields(
                        "backend", upscalerBackend,
                        "backend_source", upscalerBackendSource,
                        "framegen_enabled", frameGenerationActive ? "1" : "0",
                        "framegen_mode", upscalerFramegenMode,
                        "source_chain", upscalerBackendSource + ">" + upscalerPresetSource + ">" + upscalerFramegenSource,
                        "preset_effective", effectivePreset,
                        "guard_reason", guardReason,
                        "deprecated_alias_used", upscalerDeprecatedAliasUsed ? "1" : "0"
                )
        );

        ForensicLogger.logEvent(
                this,
                "info",
                "UPSCALER_ROUTE_APPLIED",
                null,
                "graphics_route",
                "Applied upscaler/frame-generation contract",
                ForensicLogger.fields(
                        "backend", upscalerBackend,
                        "backend_source", upscalerBackendSource,
                        "preset_requested", requestedPreset,
                        "preset_source", upscalerPresetSource,
                        "preset_effective", effectivePreset,
                        "soc_class", normalizedSocClass,
                        "effect", upscalerEffect,
                        "scale_percent", upscalerScalePercent,
                        "sharpness_percent", upscalerSharpnessPercent,
                        "denoise_percent", upscalerDenoisePercent,
                        "vk_validation_layer_requested", requestedValidationLayer ? "1" : "0",
                        "vk_validation_layer", upscalerValidationLayerActive ? "1" : "0",
                        "vk_validation_guard", requestedValidationLayer && !validationLayerAvailable ? "vulkan_validation_layer_missing" : "none",
                        "vk_validation_source", upscalerValidationSource,
                        "vulkan_runtime_source", envVars.get("AERO_VULKAN_RUNTIME_SOURCE"),
                        "framegen_enabled", frameGenerationActive ? "1" : "0",
                        "framegen_source", upscalerFramegenSource,
                        "generated_frames", upscalerGeneratedFrames,
                        "generated_frames_effective", effectiveGeneratedFrames,
                        "fg_source", upscalerFgSource,
                        "fg_output", resolvedFgOutput,
                        "framegen_mode", upscalerFramegenMode,
                        "thermal_guard", upscalerThermalGuard ? "1" : "0",
                        "thermal_guard_effective", effectiveThermalGuard ? "1" : "0",
                        "target_fps", upscalerTargetFps,
                        "target_fps_effective", effectiveTargetFps,
                        "interpolation_factor", upscalerInterpolationFactor,
                        "interpolation_factor_effective", effectiveInterpolationFactor,
                        "debug_overlay", upscalerDebugOverlay ? "1" : "0",
                        "debug_tearlines", upscalerDebugTearLines ? "1" : "0",
                        "interpolated_only", upscalerInterpolatedOnly ? "1" : "0",
                        "guard_reason", guardReason
                )
        );
    }



    private ActivityResultLauncher<Intent> controlsEditorActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (editInputControlsCallback != null) {
                    editInputControlsCallback.run();
                    editInputControlsCallback = null;
                }
            }
    );

    private String parseShortcutNameFromDesktopFile(File desktopFile) {
        String shortcutName = "";
        if (desktopFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(desktopFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("Name=")) {
                        shortcutName = line.split("=")[1].trim();
                        break;
                    }
                }
            } catch (IOException e) {
                Log.e("XServerDisplayActivity", "Error reading shortcut name from .desktop file", e);
            }
        }
        return shortcutName;
    }

    private void showInputControlsDialog() {
        final ContentDialog dialog = new ContentDialog(this, R.layout.input_controls_dialog);
        dialog.setTitle(R.string.input_controls);
        dialog.setIcon(R.drawable.ae_icon_gamepad);

        final Spinner sProfile = dialog.findViewById(R.id.SProfile);

        Runnable loadProfileSpinner = () -> {
            ArrayList<ControlsProfile> profiles = inputControlsManager.getProfiles(true);
            ArrayList<String> profileItems = new ArrayList<>();
            int selectedPosition = 0;
            profileItems.add("-- "+getString(R.string.disabled)+" --");
            for (int i = 0; i < profiles.size(); i++) {
                ControlsProfile profile = profiles.get(i);
                if (inputControlsView.getProfile() != null && profile.id == inputControlsView.getProfile().id)
                    selectedPosition = i + 1;
                profileItems.add(profile.getName());
            }

            sProfile.setAdapter(SpinnerAdapters.createRuntime(this, profileItems));
            sProfile.setSelection(selectedPosition);
            SpinnerAdapters.applyRuntimeSurface(sProfile);
        };
        loadProfileSpinner.run();

        final CheckBox cbShowTouchscreenControls = dialog.findViewById(R.id.CBShowTouchscreenControls);
        cbShowTouchscreenControls.setChecked(inputControlsView.isShowTouchscreenControls());

        final CheckBox cbEnableTimeout = dialog.findViewById(R.id.CBEnableTimeout);
        cbEnableTimeout.setChecked(preferences.getBoolean("touchscreen_timeout_enabled", false));

        final CheckBox cbEnableHaptics = dialog.findViewById(R.id.CBEnableHaptics);
        cbEnableHaptics.setChecked(preferences.getBoolean("touchscreen_haptics_enabled", false));

        final Runnable updateProfile = () -> {
            int position = sProfile.getSelectedItemPosition();
            if (position > 0) {
                showInputControls(inputControlsManager.getProfiles().get(position - 1));
            }
            else hideInputControls();
        };

        dialog.findViewById(R.id.BTSettings).setOnClickListener((v) -> {
            int position = sProfile.getSelectedItemPosition();
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("edit_input_controls", true);
            intent.putExtra("selected_profile_id", position > 0 ? inputControlsManager.getProfiles().get(position - 1).id : 0);
            editInputControlsCallback = () -> {
                hideInputControls();
                inputControlsManager.loadProfiles(true);
                loadProfileSpinner.run();
                updateProfile.run();
            };
            controlsEditorActivityResultLauncher.launch(intent);
        });

        dialog.setOnConfirmCallback(() -> {
            inputControlsView.setShowTouchscreenControls(cbShowTouchscreenControls.isChecked());
            boolean isTimeoutEnabled = cbEnableTimeout.isChecked();
            boolean isHapticsEnabled = cbEnableHaptics.isChecked();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("touchscreen_timeout_enabled", isTimeoutEnabled);
            editor.putBoolean("touchscreen_haptics_enabled", isHapticsEnabled);
            editor.apply();

            if (isTimeoutEnabled) {
                startTouchscreenTimeout(); // Start the timeout functionality if enabled
            } else {
                touchpadView.setOnTouchListener(null); // Disable the listener if timeout is disabled
            }
            int position = sProfile.getSelectedItemPosition();
            if (position > 0) {
                showInputControls(inputControlsManager.getProfiles().get(position - 1));
            }
            else hideInputControls();
            updateProfile.run();
        });

        dialog.setOnCancelCallback(updateProfile::run);

        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        styleRuntimeNestedDialog(dialog);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            dialog.getWindow().setLayout(
                    Math.round(AppUtils.getScreenWidth() * 0.992f),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        ViewGroup.LayoutParams params = dialog.getContentView().getLayoutParams();
        if (params != null) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getContentView().setLayoutParams(params);
        }
        dialog.getContentView().setMinimumHeight(0);
    }

    private void simulateConfirmInputControlsDialog() {
        // Simulate setting the relative mouse movement and touchscreen controls from preferences

        boolean isShowTouchscreenControls = preferences.getBoolean("show_touchscreen_controls_enabled", false); // default is false (hidden)
        inputControlsView.setShowTouchscreenControls(isShowTouchscreenControls);

        boolean isTimeoutEnabled = preferences.getBoolean("touchscreen_timeout_enabled", false);
        boolean isHapticsEnabled = preferences.getBoolean("touchscreen_haptics_enabled", false);

        // Apply these settings as if the user confirmed the dialog
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("touchscreen_timeout_enabled", isTimeoutEnabled);
        editor.putBoolean("touchscreen_haptics_enabled", isHapticsEnabled);
        editor.apply();

        // If no profile is selected, hide the controls
        int selectedProfileIndex = preferences.getInt("selected_profile_index", -1); // Default to -1 for no profile

        if (selectedProfileIndex >= 0 && selectedProfileIndex < inputControlsManager.getProfiles().size()) {
            // A profile is selected, show the controls
            ControlsProfile profile = inputControlsManager.getProfiles().get(selectedProfileIndex);
            showInputControls(profile);
        } else {
            // No profile selected, ensure the controls are hidden
            hideInputControls();
        }

        // Timeout logic should only apply if the controls are visible
        if (isTimeoutEnabled && inputControlsView.getVisibility() == View.VISIBLE) {
            startTouchscreenTimeout(); // Start timeout if enabled and controls are visible
        } else {
            touchpadView.setOnTouchListener(null); // Disable the timeout listener if not needed
        }

        Log.d("XServerDisplayActivity", "Input controls simulated confirmation executed.");
    }

    private void startTouchscreenTimeout() {
        boolean isTimeoutEnabled = preferences.getBoolean("touchscreen_timeout_enabled", false);

        if (isTimeoutEnabled) {
            // Show controls initially and set up touch event listeners
            inputControlsView.setVisibility(View.VISIBLE);
            Log.d("XServerDisplayActivity", "Timeout is enabled, setting up timeout logic.");

            // Attach the OnTouchListener to reset the timeout on touch events
            touchpadView.setOnTouchListener((v, event) -> {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                    if (action == MotionEvent.ACTION_DOWN) {
                        try {
                            v.requestUnbufferedDispatch(event);
                        }
                        catch (Throwable t) {
                            Log.d("XServerDisplayActivity", "requestUnbufferedDispatch unavailable", t);
                        }
                    }
                    // Reset the timeout on any touch event
                    //Log.d("XServerDisplayActivity", "Touch detected, resetting timeout.");

                    // Keep the controls visible
                    inputControlsView.setVisibility(View.VISIBLE);

                    // Remove any pending hide callbacks and reset the timeout
                    timeoutHandler.removeCallbacks(hideControlsRunnable);
                    timeoutHandler.postDelayed(hideControlsRunnable, 5000); // Reset timeout
                }

                return false; // Allow the touch event to propagate
            });

            // Reset the timeout when the controls are initially displayed
            timeoutHandler.removeCallbacks(hideControlsRunnable);
            timeoutHandler.postDelayed(hideControlsRunnable, 5000); // Hide after 5 seconds of inactivity
        } else {
            // If timeout is disabled, keep the controls always visible
            Log.d("XServerDisplayActivity", "Timeout is disabled, controls will stay visible.");

            inputControlsView.setVisibility(View.VISIBLE); // Ensure controls are visible
            timeoutHandler.removeCallbacks(hideControlsRunnable); // Remove any existing hide callbacks
            touchpadView.setOnTouchListener(null); // Remove the touch listener
        }
    }

    private void showInputControls(ControlsProfile profile) {
        inputControlsView.setVisibility(View.VISIBLE);
        inputControlsView.requestFocus();
        inputControlsView.setProfile(profile);

        touchpadView.setSensitivity(profile.getCursorSpeed() * globalCursorSpeed);
        touchpadView.setPointerButtonRightEnabled(false);

        inputControlsView.invalidate();
    }

    private void hideInputControls() {
        inputControlsView.setShowTouchscreenControls(true);
        inputControlsView.setVisibility(View.GONE);
        inputControlsView.setProfile(null);

        touchpadView.setSensitivity(globalCursorSpeed);
        touchpadView.setPointerButtonLeftEnabled(true);
        touchpadView.setPointerButtonRightEnabled(true);

        inputControlsView.invalidate();
    }

    private static final Pattern VULKAN_API_MINOR_PATTERN = Pattern.compile("1\\.(\\d+)");
    private static final int VORTEK_LATEST_VULKAN_API_VERSION = GPUHelper.vkMakeVersion(1, 4, 349);
    private static final String VORTEK_LATEST_VULKAN_API_LABEL = "1.4";

    private String detectSoCClass() {
        SocClassifier.Tier tier = SocClassifier.detect(
                GPUInformation.getRenderer(null, this),
                readBuildField("SOC_MODEL"),
                readBuildField("HARDWARE"),
                readSystemProperty("ro.board.platform"),
                readSystemProperty("ro.product.board")
        );
        return switch (tier) {
            case ADRENO_7XX -> "adreno-7xx";
            case ADRENO_6XX, ADRENO_LEGACY -> "adreno-6xx-and-older";
            case XCLIPSE_RDNA_MOBILE -> "xclipse-rdna-mobile";
            case MALI_G7XX_OR_NEWER -> "mali-g7xx-or-newer";
            case MALI_LEGACY -> "mali-legacy";
            default -> "unknown";
        };
    }

    private String readBuildField(String fieldName) {
        try {
            Object value = Build.class.getField(fieldName).get(null);
            return value == null ? "" : String.valueOf(value);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private String readSystemProperty(String key) {
        try {
            Class<?> systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Object value = systemPropertiesClass
                    .getMethod("get", String.class, String.class)
                    .invoke(null, key, "");
            return value == null ? "" : String.valueOf(value);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private String resolveRuntimeProfileId() {
        return resolveRuntimeProfileId(envVars);
    }

    private String resolveRuntimeProfileId(EnvVars mergedEnv) {
        String globalProfile = preferences.getString("runtime_profile_global", RuntimeProfile.AUTO);
        if (globalProfile == null || globalProfile.trim().isEmpty()) {
            globalProfile = RuntimeProfile.AUTO;
        }

        String containerProfile = container != null ? container.getExtra("runtimeProfile", globalProfile) : globalProfile;
        if (shortcut != null) {
            containerProfile = shortcut.getExtra("runtimeProfile", containerProfile);
        }

        String envOverride = mergedEnv != null ? mergedEnv.get("AERO_RUNTIME_PROFILE") : "";
        if (envOverride != null && !envOverride.trim().isEmpty()) {
            return envOverride.trim();
        }
        return containerProfile;
    }

    private void composeLaunchEnvVars(ForensicConfig.Snapshot forensicSnapshot) {
        ForensicConfig.Snapshot runtimeForensicSnapshot = resolveRuntimeForensicSnapshot(forensicSnapshot);
        EnvVars mergedEnv = new EnvVars();
        String bootstrapTraceId;
        String bootstrapLogPath = "";
        // Preserve graphics/runtime route env prepared before setupXEnvironment().
        mergedEnv.putAll(envVars);

        mergedEnv.put("LC_ALL", lc_all);
        mergedEnv.put("WINEPREFIX", WineUtils.resolveHostWinePrefixDir(imageFs.getRootDir()).getAbsolutePath());

        if (container != null) {
            mergedEnv.putAll(container.getEnvVars());
        }
        if (shortcut != null) {
            String shortcutEnv = shortcut.getExtra("envVars");
            if (shortcutEnv != null && !shortcutEnv.trim().isEmpty()) {
                mergedEnv.putAll(shortcutEnv);
            }
        }

        String requestedRuntimeProfile = resolveRuntimeProfileId(mergedEnv);
        mergedEnv.putAll(RuntimeProfileManager.getEnvVars(this, requestedRuntimeProfile));
        applyBionicRuntimeMarkers(mergedEnv);
        String effectiveRuntimeProfile = mergedEnv.get("AERO_RUNTIME_PROFILE_EFFECTIVE");
        if (effectiveRuntimeProfile == null || effectiveRuntimeProfile.trim().isEmpty()) {
            effectiveRuntimeProfile = RuntimeProfileManager.resolveEffectiveProfileId(this, requestedRuntimeProfile);
        }
        mergedEnv.put("AERO_RUNTIME_PROFILE", effectiveRuntimeProfile);
        applyForensicEnvVars(mergedEnv, runtimeForensicSnapshot);

        if (overrideEnvVars != null) {
            mergedEnv.putAll(overrideEnvVars);
            overrideEnvVars.clear();
        }
        WineSyncPolicy.apply(mergedEnv, selectedRuntimeProfile);
        setOrClearEnv(mergedEnv, "AERO_FORENSIC_MODE", forensicModeLaunch ? "1" : "");
        setOrClearEnv(mergedEnv, "AERO_FORENSIC_TRACE_ID", forensicTraceId);
        setOrClearEnv(mergedEnv, "AERO_FORENSIC_ROUTE_SOURCE", forensicRouteSource);
        bootstrapTraceId = mergedEnv.get("AERO_FORENSIC_TRACE_ID");
        if (bootstrapTraceId != null) {
            bootstrapTraceId = bootstrapTraceId.trim();
        }
        if (bootstrapTraceId != null && !bootstrapTraceId.isEmpty()) {
            bootstrapTraceId = bootstrapTraceId.replaceAll("[^A-Za-z0-9._-]", "_");
            bootstrapLogPath = imageFs.home_path + "/.freewine-bootstrap-" + bootstrapTraceId + ".log";
        }
        setOrClearEnv(mergedEnv, "FREEWINE_BOOTSTRAP_LOG_PATH", bootstrapLogPath);
        mergedEnv.put("AERO_ENV_LAYER_ORDER", "graphics->container->shortcut->runtime->forensic->override");
        mergedEnv.put("AERO_FORENSIC_RUNTIME_SUMMARY", ForensicConfig.buildRuntimeSummary(runtimeForensicSnapshot));
        mergedEnv.put("AERO_FORENSIC_CAPTURE_SUMMARY", ForensicConfig.buildCaptureSummary(this, forensicSnapshot));

        ForensicLogger.logEvent(
                this,
                "info",
                "FORENSIC_ENV_APPLIED",
                null,
                "xserver",
                "forensic_env_applied",
                ForensicLogger.fields(
                        "requested_runtime_summary", ForensicConfig.buildRuntimeSummary(forensicSnapshot),
                        "runtime_summary", ForensicConfig.buildRuntimeSummary(runtimeForensicSnapshot),
                        "capture_summary", ForensicConfig.buildCaptureSummary(this, forensicSnapshot),
                        "forensic_mode", forensicModeLaunch,
                        "forensic_trace_id", mergedEnv.get("AERO_FORENSIC_TRACE_ID"),
                        "freewine_bootstrap_log_path", mergedEnv.get("FREEWINE_BOOTSTRAP_LOG_PATH"),
                        "forensic_route_source", mergedEnv.get("AERO_FORENSIC_ROUTE_SOURCE"),
                        "wine_debug", runtimeForensicSnapshot.enableWineDebug ? "1" : "0",
                        "loader_trace", runtimeForensicSnapshot.enableLoaderTrace ? "1" : "0",
                        "trace_mode", ForensicConfig.buildLoaderTraceMode(runtimeForensicSnapshot),
                        "vulkan_api_dump_requested", mergedEnv.get("AERO_FORENSIC_VULKAN_API_DUMP_REQUESTED"),
                        "vulkan_api_dump_applied", mergedEnv.get("AERO_FORENSIC_VULKAN_API_DUMP_APPLIED"),
                        "vulkan_validation_requested", mergedEnv.get("AERO_FORENSIC_VULKAN_VALIDATION_REQUESTED"),
                        "vulkan_validation_applied", mergedEnv.get("AERO_FORENSIC_VULKAN_VALIDATION_APPLIED"),
                        "available_vk_layers", mergedEnv.get("AERO_FORENSIC_AVAILABLE_VK_LAYERS"),
                        "wine_sync_requested", mergedEnv.get("AERO_WINE_SYNC_REQUESTED"),
                        "wine_sync_effective", mergedEnv.get("AERO_WINE_SYNC_POLICY_EFFECTIVE"),
                        "wine_sync_userspace_policy", mergedEnv.get("AERO_WINE_SYNC_USERSPACE_POLICY_EFFECTIVE"),
                        "wine_sync_expected_path", mergedEnv.get("AERO_WINE_SYNC_EXPECTED_PATH"),
                        "wine_sync_reason", mergedEnv.get("AERO_WINE_SYNC_REASON"),
                        "wine_sync_runtime_accepts_aesync", mergedEnv.get("AERO_WINE_SYNC_RUNTIME_ACCEPTS_AESYNC"),
                        "wine_sync_runtime_scope", mergedEnv.get("AERO_WINE_SYNC_RUNTIME_SCOPE"),
                        "wine_sync_runtime_family", mergedEnv.get("AERO_WINE_SYNC_RUNTIME_FAMILY"),
                        "wine_sync_runtime_model", mergedEnv.get("AERO_WINE_SYNC_RUNTIME_MODEL"),
                        "wine_sync_runtime_source_repo", mergedEnv.get("AERO_WINE_SYNC_RUNTIME_SOURCE_REPO"),
                        "wine_sync_runtime_release_tag", mergedEnv.get("AERO_WINE_SYNC_RUNTIME_RELEASE_TAG"),
                        "wine_sync_runtime_artifact_name", mergedEnv.get("AERO_WINE_SYNC_RUNTIME_ARTIFACT_NAME"),
                        "wine_sync_runtime_entry", mergedEnv.get("AERO_WINE_SYNC_RUNTIME_ENTRY"),
                        "wineaesync", mergedEnv.get("AERO_WINE_SYNC_WINEAESYNC_EFFECTIVE"),
                        "winefsync", mergedEnv.get("AERO_WINE_SYNC_WINEFSYNC_EFFECTIVE"),
                        "wineesync", mergedEnv.get("AERO_WINE_SYNC_WINEESYNC_EFFECTIVE"),
                        "ntsync_device_present", mergedEnv.get("AERO_WINE_SYNC_NTSYNC_DEVICE_PRESENT"),
                        "ntsync_source_supported", mergedEnv.get("AERO_WINE_SYNC_NTSYNC_SOURCE_SUPPORTED"),
                        "ntsync_source_tree_present", mergedEnv.get("AERO_WINE_SYNC_NTSYNC_SOURCE_TREE_PRESENT"),
                        "ntsync_compiled_support", mergedEnv.get("AERO_WINE_SYNC_NTSYNC_COMPILED_SUPPORT"),
                        "ntsync_env_switchable", mergedEnv.get("AERO_WINE_SYNC_NTSYNC_ENV_SWITCHABLE"),
                        "env_hash", ForensicLogger.hashEnvVars(mergedEnv)
                )
        );

        envVars.clear();
        envVars.putAll(mergedEnv);
    }

    private ForensicConfig.Snapshot resolveRuntimeForensicSnapshot(ForensicConfig.Snapshot snapshot) {
        return ForensicConfig.withRuntimeCaptureDefaults(snapshot, shouldForceRuntimeDiagnosticsForLaunch());
    }

    private boolean shouldForceRuntimeDiagnosticsForLaunch() {
        if (forensicModeLaunch) return true;
        String runtimeModel = ContentProfile.normalizeRuntimeModel(effectiveRuntimeModel);
        boolean knownRuntimeModel = ContentProfile.RUNTIME_MODEL_BIONIC.equals(runtimeModel)
                || ContentProfile.RUNTIME_MODEL_GLIBC.equals(runtimeModel);
        if (!knownRuntimeModel) return false;
        if (selectedRuntimeProfile == null) return wineInfo != null;
        return selectedRuntimeProfile.type == ContentProfile.ContentType.CONTENT_TYPE_WINE
                || selectedRuntimeProfile.type == ContentProfile.ContentType.CONTENT_TYPE_PROTON;
    }

    private void applyForensicEnvVars(ForensicConfig.Snapshot snapshot) {
        applyForensicEnvVars(envVars, snapshot);
    }

    private void applyForensicEnvVars(EnvVars targetEnv, ForensicConfig.Snapshot snapshot) {
        boolean loaderTraceEnabled = ForensicConfig.shouldEnableLoaderTrace(snapshot, false);
        String effectiveWineDebug = ForensicConfig.buildEffectiveWineDebug(
                snapshot.enableWineDebug,
                snapshot.wineDebugChannels,
                loaderTraceEnabled
        );
        targetEnv.put("WINEDEBUG", effectiveWineDebug);
        targetEnv.put("AERO_FORENSIC_LOADER_TRACE", loaderTraceEnabled ? "1" : "0");
        targetEnv.put("AERO_FORENSIC_TRACE_MODE", ForensicConfig.buildLoaderTraceMode(snapshot));

        setOrClearEnv(targetEnv, "BOX64_LOG", snapshot.enableBox64Logs ? "1" : "");
        setOrClearEnv(targetEnv, "BOX64_DYNAREC_MISSING", snapshot.enableBox64Logs ? "1" : "");
        setOrClearEnv(targetEnv, "FEX_LOG_LEVEL", snapshot.enableFexLogs ? "debug" : "");
        setOrClearEnv(targetEnv, "FEX_DEBUG", snapshot.enableFexLogs ? "1" : "");
        setOrClearEnv(targetEnv, "MESA_LOG_LEVEL", snapshot.enableTurnipLogs ? "debug" : "");
        setOrClearEnv(targetEnv, "MESA_DEBUG", snapshot.enableTurnipLogs ? "context" : "");
        setOrClearEnv(targetEnv, "DXVK_LOG_LEVEL", snapshot.enableDxvkLogs ? "info" : "none");
        setOrClearEnv(targetEnv, "VKD3D_DEBUG", snapshot.enableVkd3dLogs ? "warn" : "");
        setOrClearEnv(targetEnv, "AERO_DGVOODOO_LOGS", snapshot.enableDgVoodooLogs ? "1" : "0");
        setOrClearEnv(targetEnv, "PULSE_LOG", snapshot.enablePulseLogs ? "4" : "");
        setOrClearEnv(targetEnv, "ALSA_DEBUG", snapshot.enableAlsaLogs ? "1" : "");
        setOrClearEnv(targetEnv, "VK_LOADER_DEBUG", snapshot.enableVulkanLoaderDebug ? "all" : "");

        Set<String> availableVkLayers = resolveAvailableVulkanLayerNames(targetEnv);
        String availableVkLayersValue = availableVkLayers == null || availableVkLayers.isEmpty()
                ? ""
                : String.join(",", availableVkLayers);
        String vkLayers = removeVkInstanceLayer(targetEnv.get("VK_INSTANCE_LAYERS"), "VK_LAYER_LUNARG_api_dump");
        vkLayers = removeVkInstanceLayer(vkLayers, "VK_LAYER_KHRONOS_validation");
        boolean vulkanApiDumpRequested = snapshot.enableVulkanApiDump;
        boolean vulkanApiDumpApplied = vulkanApiDumpRequested && availableVkLayers.contains("VK_LAYER_LUNARG_api_dump");
        if (snapshot.enableVulkanApiDump) {
            if (vulkanApiDumpApplied) {
                vkLayers = appendVkInstanceLayers(vkLayers, "VK_LAYER_LUNARG_api_dump");
            }
        }
        boolean vulkanValidationRequested = snapshot.enableVulkanValidation;
        boolean vulkanValidationApplied = vulkanValidationRequested && availableVkLayers.contains("VK_LAYER_KHRONOS_validation");
        if (snapshot.enableVulkanValidation) {
            if (vulkanValidationApplied) {
                vkLayers = appendVkInstanceLayers(vkLayers, "VK_LAYER_KHRONOS_validation");
            }
        }
        setOrClearEnv(targetEnv, "VK_INSTANCE_LAYERS", vkLayers);
        setOrClearEnv(targetEnv, "AERO_FORENSIC_AVAILABLE_VK_LAYERS", availableVkLayersValue);
        setOrClearEnv(targetEnv, "AERO_FORENSIC_VULKAN_API_DUMP_REQUESTED", vulkanApiDumpRequested ? "1" : "0");
        setOrClearEnv(targetEnv, "AERO_FORENSIC_VULKAN_API_DUMP_APPLIED", vulkanApiDumpApplied ? "1" : "0");
        setOrClearEnv(targetEnv, "AERO_FORENSIC_VULKAN_VALIDATION_REQUESTED", vulkanValidationRequested ? "1" : "0");
        setOrClearEnv(targetEnv, "AERO_FORENSIC_VULKAN_VALIDATION_APPLIED", vulkanValidationApplied ? "1" : "0");
    }

    private void installForensicRuntimeLogCallbacks(ForensicConfig.Snapshot snapshot) {
        forensicRuntimeCallbacks.clear();
        if (snapshot == null) return;

        boolean runtimeStreamCapture = snapshot.hasRuntimeDiagnosticsEnabled();
        addForensicRuntimeFileCallback(runtimeStreamCapture,
                "runtime_omni");
        addForensicRuntimeFileCallback(forensicModeLaunch,
                "runtime_bootstrap", "freewine-", "wineboot", "wineserver", "explorer", "services", "winhandler", "wfm", "aesync");
        addForensicRuntimeFileCallback(snapshot.enableWineDebug || snapshot.enableLoaderTrace,
                "wine_loader", "wine", "loaddll", "module", "ntdll", "kernel32", "user32", "gdi32",
                "win32u", "winex11", "x11drv", "display", "unixlib", "dlopen", "nodrv", "CreateWindow", "winediag");
        addForensicRuntimeFileCallback(snapshot.enableBox64Logs,
                "box64", "box64", "dynarec");
        addForensicRuntimeFileCallback(snapshot.enableFexLogs,
                "fex_runtime", "fex", "thunk");
        addForensicRuntimeFileCallback(snapshot.enableTurnipLogs,
                "graphics_mesa", "turnip", "mesa", "freedreno", "gallium", "zink", "opengl");
        addForensicRuntimeFileCallback(snapshot.enableVulkanApiDump,
                "vulkan_api_dump", "api_dump", "vkcreate", "vkqueue", "vkcmd");
        addForensicRuntimeFileCallback(snapshot.enableVulkanLoaderDebug,
                "vulkan_loader", "vk_loader", "vulkan loader", "icd", "layer");
        addForensicRuntimeFileCallback(snapshot.enableDxvkLogs,
                "dxvk", "dxvk", "d3d9", "d3d11", "d3d12");
        addForensicRuntimeFileCallback(snapshot.enableVkd3dLogs,
                "vkd3d", "vkd3d", "d3d12");
        addForensicRuntimeFileCallback(snapshot.enableDgVoodooLogs,
                "dgvoodoo", "dgvoodoo", "ddraw", "glide", "d3d8");
        addForensicRuntimeFileCallback(snapshot.enablePulseLogs,
                "pulse", "pulse", "pulseaudio");
        addForensicRuntimeFileCallback(snapshot.enableAlsaLogs,
                "alsa", "alsa");

        if (!forensicRuntimeCallbacks.isEmpty()) {
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "FORENSIC_STREAM_HOOKS_READY",
                    null,
                    "xserver",
                    "Attached runtime forensic stream hooks",
                    ForensicLogger.fields(
                            "callbacks", forensicRuntimeCallbacks.size(),
                            "trace_mode", ForensicConfig.buildLoaderTraceMode(snapshot)
                    )
            );
        }
    }

    private void addForensicRuntimeFileCallback(boolean enabled, String prefix, String... filters) {
        if (!enabled) return;
        FileDebugLogger callback = new FileDebugLogger(this, prefix, filters);
        forensicRuntimeCallbacks.add(callback);
        ProcessHelper.addDebugCallback(callback);
    }

    private void applyBionicRuntimeMarkers(EnvVars targetEnv) {
        if (targetEnv == null) return;
        String runtimeLibc = effectiveRuntimeModel == null || effectiveRuntimeModel.trim().isEmpty()
                ? (imageFs != null ? imageFs.getRuntimeLibcModel() : "bionic")
                : effectiveRuntimeModel;
        targetEnv.put("AERO_RUNTIME_LIBC", runtimeLibc);
        targetEnv.put("AERO_RUNTIME_ANDROID_BIONIC_ONLY", "glibc".equalsIgnoreCase(runtimeLibc) ? "0" : "1");
        targetEnv.put("AERO_RUNTIME_ANDROID_SDK", String.valueOf(Build.VERSION.SDK_INT));
        targetEnv.put("AERO_RUNTIME_ANDROID_RELEASE", Build.VERSION.RELEASE == null ? "" : Build.VERSION.RELEASE);
        targetEnv.put("AERO_RUNTIME_HOST_ARCH", System.getProperty("os.arch", ""));
        if (Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0) {
            targetEnv.put("AERO_RUNTIME_DEVICE_ABI", Build.SUPPORTED_ABIS[0]);
            targetEnv.put("AERO_RUNTIME_DEVICE_ABIS", joinAbiList(Build.SUPPORTED_ABIS));
        } else {
            targetEnv.put("AERO_RUNTIME_DEVICE_ABI", "unknown");
            targetEnv.put("AERO_RUNTIME_DEVICE_ABIS", "unknown");
        }
        targetEnv.put("AERO_RUNTIME_WOW_ROUTE", wineInfo != null && wineInfo.isArm64EC() ? "arm64ec" : "box64");
    }

    private String joinAbiList(String[] values) {
        if (values == null || values.length == 0) return "unknown";
        StringBuilder joined = new StringBuilder();
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) continue;
            if (joined.length() > 0) joined.append(',');
            joined.append(value.trim());
        }
        return joined.length() == 0 ? "unknown" : joined.toString();
    }

    private void setOrClearEnv(String key, String value) {
        setOrClearEnv(envVars, key, value);
    }

    private void setOrClearEnv(EnvVars targetEnv, String key, String value) {
        if (value == null || value.trim().isEmpty()) {
            targetEnv.remove(key);
        } else {
            targetEnv.put(key, value);
        }
    }

    private String appendVkInstanceLayers(String baseLayers, String layer) {
        if (layer == null || layer.trim().isEmpty()) return baseLayers;
        String normalizedLayer = layer.trim();
        if (baseLayers == null || baseLayers.trim().isEmpty()) return normalizedLayer;
        String[] parts = baseLayers.split(":");
        for (String part : parts) {
            if (normalizedLayer.equals(part.trim())) return baseLayers;
        }
        return baseLayers + ":" + normalizedLayer;
    }

    private String removeVkInstanceLayer(String baseLayers, String layer) {
        if (baseLayers == null || baseLayers.trim().isEmpty()) return "";
        if (layer == null || layer.trim().isEmpty()) return baseLayers;
        String normalizedLayer = layer.trim();
        String[] parts = baseLayers.split(":");
        ArrayList<String> filtered = new ArrayList<>(parts.length);
        for (String part : parts) {
            String candidate = part == null ? "" : part.trim();
            if (candidate.isEmpty() || normalizedLayer.equals(candidate)) continue;
            filtered.add(candidate);
        }
        return filtered.isEmpty() ? "" : String.join(":", filtered);
    }

    private Set<String> resolveAvailableVulkanLayerNames(EnvVars sourceEnv) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        LinkedHashSet<String> manifestDirs = new LinkedHashSet<>();
        String rawLayerPath = sourceEnv != null ? sourceEnv.get("VK_LAYER_PATH") : null;
        if (rawLayerPath != null && !rawLayerPath.trim().isEmpty()) {
            String[] parts = rawLayerPath.split(":");
            for (String part : parts) {
                String candidate = part == null ? "" : part.trim();
                if (!candidate.isEmpty()) manifestDirs.add(candidate);
            }
        }
        if (imageFs != null) {
            manifestDirs.add(new File(imageFs.getShareDir(), "vulkan/implicit_layer.d").getAbsolutePath());
            manifestDirs.add(new File(imageFs.getShareDir(), "vulkan/explicit_layer.d").getAbsolutePath());
        }
        for (String manifestDirPath : manifestDirs) {
            File manifestDir = new File(manifestDirPath);
            File[] manifestFiles = manifestDir.listFiles((dir, name) -> name != null && name.endsWith(".json"));
            if (manifestFiles == null) continue;
            for (File manifestFile : manifestFiles) {
                collectVulkanLayerNames(manifestFile, names);
            }
        }
        return names;
    }

    private void collectVulkanLayerNames(File manifestFile, Set<String> names) {
        if (manifestFile == null || names == null || !manifestFile.isFile()) return;
        try {
            JSONObject object = new JSONObject(FileUtils.readString(manifestFile));
            JSONObject layer = object.optJSONObject("layer");
            if (layer != null) {
                String name = layer.optString("name", "").trim();
                if (!name.isEmpty()) names.add(name);
            }
            JSONArray layers = object.optJSONArray("layers");
            if (layers == null) return;
            for (int i = 0; i < layers.length(); i++) {
                JSONObject layerObject = layers.optJSONObject(i);
                if (layerObject == null) continue;
                String name = layerObject.optString("name", "").trim();
                if (!name.isEmpty()) names.add(name);
            }
        } catch (Exception ignored) {
        }
    }

    private void logMissingVulkanLayer(String requester, String layerName, Set<String> availableLayers) {
        ForensicLogger.logEvent(
                this,
                "warn",
                "VULKAN_LAYER_REQUEST_SKIPPED",
                null,
                "xserver",
                "requested_vulkan_layer_missing",
                ForensicLogger.fields(
                        "requester", requester,
                        "layer_name", layerName,
                        "available_layers", availableLayers == null || availableLayers.isEmpty()
                                ? ""
                                : String.join(",", availableLayers)
                )
        );
    }

    private String forensicTraceIdOrNull() {
        return forensicTraceId == null || forensicTraceId.trim().isEmpty()
                ? null
                : forensicTraceId.trim();
    }

    private void refreshForensicTrace(@Nullable Intent intent) {
        forensicModeLaunch = intent != null && intent.getBooleanExtra("forensic_mode", false);
        String explicitTraceId = intent != null ? safeTrim(intent.getStringExtra("forensic_trace_id")) : "";
        forensicTraceGenerated = explicitTraceId.isEmpty();
        forensicTraceId = forensicTraceGenerated ? ForensicLogger.newTraceId() : explicitTraceId;
        forensicRouteSource = intent != null ? safeTrim(intent.getStringExtra("forensic_route_source")) : "";
        ForensicLogger.setActiveTraceId(forensicTraceIdOrNull());
    }

    private void prearmDesktopShellBootstrapIfNeeded(String source) {
        if (desktopShellBootstrapActive) return;
        if (!shouldUseDirectDesktopShellBootstrap()) return;
        desktopShellBootstrapActive = true;
        desktopShellLaunchMode = DESKTOP_SHELL_LAUNCH_MODE_DIRECT_EXPLORER;
        if (desktopShellBootstrapStartedAtMs == 0L) {
            desktopShellBootstrapStartedAtMs = System.currentTimeMillis();
        }
        logBootstrapCheckpoint(
                "XSERVER_BOOTSTRAP_PREARMED",
                "desktop_shell_bootstrap_prearmed_before_guest_submit",
                "source", source,
                "launch_mode", desktopShellLaunchMode
        );
    }

    private void armGuestBootstrapAfterFocus(Runnable runnable, String source) {
        if (guestBootstrapSubmitted) return;
        pendingBootstrapRunnable = runnable;
        pendingBootstrapSource = source == null ? "" : source;
        bootstrapWaitingForFocus = false;
        logBootstrapCheckpoint(
                "XSERVER_BOOTSTRAP_FOCUS_GATE_ARMED",
                "guest_bootstrap_armed_waiting_for_window_focus",
                "source", pendingBootstrapSource,
                "activity_has_focus", hasWindowFocus(),
                "activity_finishing", isFinishing(),
                "activity_destroyed", isDestroyed()
        );
        maybeRunPendingGuestBootstrap(source);
    }

    private void maybeRunPendingGuestBootstrap(String source) {
        if (guestBootstrapSubmitted) return;
        Runnable runnable = pendingBootstrapRunnable;
        if (runnable == null) return;

        boolean hasRealWindowFocus = hasWindowFocus();
        boolean readyForBootstrap =
                !isFinishing() && !isDestroyed() && (hasRealWindowFocus || bootstrapFirstDrawObserved);
        if (!readyForBootstrap) {
            if (!bootstrapWaitingForFocus) {
                bootstrapWaitingForFocus = true;
                logBootstrapCheckpoint(
                        "XSERVER_BOOTSTRAP_FOCUS_GATE_WAITING",
                        "guest_bootstrap_waiting_for_real_window_focus_or_first_draw",
                        "source", source,
                        "armed_source", pendingBootstrapSource,
                        "activity_has_focus", hasWindowFocus(),
                        "first_draw_observed", bootstrapFirstDrawObserved,
                        "activity_finishing", isFinishing(),
                        "activity_destroyed", isDestroyed()
                );
            }
            return;
        }

        pendingBootstrapRunnable = null;
        guestBootstrapSubmitted = true;
        bootstrapWaitingForFocus = false;
        bindActiveContainerState(container, effectiveRuntimeModel, resolveActiveRuntimeIdentity());
        prepareRootfsDevInputPath();
        if (hasRealWindowFocus) {
            logBootstrapCheckpoint(
                    "XSERVER_BOOTSTRAP_FOCUS_GATE_PASSED",
                    "guest_bootstrap_released_after_window_focus",
                    "source", source,
                    "armed_source", pendingBootstrapSource,
                    "activity_has_focus", true,
                    "first_draw_observed", bootstrapFirstDrawObserved
            );
        } else {
            logBootstrapCheckpoint(
                    "XSERVER_BOOTSTRAP_DRAW_GATE_RELEASED",
                    "guest_bootstrap_released_after_first_draw_without_window_focus",
                    "source", source,
                    "armed_source", pendingBootstrapSource,
                    "activity_has_focus", false,
                    "first_draw_observed", bootstrapFirstDrawObserved
            );
        }
        runnable.run();
    }

    private void logBootstrapCheckpoint(String eventId, String message, Object... fields) {
        ForensicLogger.logEvent(
                this,
                "info",
                eventId,
                forensicTraceIdOrNull(),
                "xserver",
                message,
                fields == null || fields.length == 0 ? null : ForensicLogger.fields(fields)
        );
    }

    private RuntimeException rethrowBootstrapFailure(String eventId, String message, Throwable error, Object... fields) {
        ForensicLogger.error(
                this,
                eventId,
                forensicTraceIdOrNull(),
                "xserver",
                message,
                error,
                fields == null || fields.length == 0 ? null : ForensicLogger.fields(fields)
        );
        if (error instanceof RuntimeException) {
            return (RuntimeException) error;
        }
        return new RuntimeException(error);
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private void rememberActiveLaunchTarget(int containerId,
                                            @Nullable String shortcutPath,
                                            @Nullable String appId,
                                            @Nullable String launchRouteToken,
                                            @Nullable String temporaryOverrideAppId) {
        activeLaunchContainerId = containerId;
        activeLaunchShortcutPath = normalizeShortcutPath(shortcutPath);
        activeLaunchAppId = safeTrim(appId);
        activeLaunchRouteToken = safeTrim(launchRouteToken);
        activeTemporaryOverrideAppId = safeTrim(temporaryOverrideAppId);
        activeTemporaryOverrideRestored = false;
    }

    private String normalizeShortcutPath(@Nullable String shortcutPath) {
        return shortcutPath == null ? "" : shortcutPath.trim();
    }

    private String resolveIntentLaunchAppId(@Nullable Intent intent) {
        return safeTrim(intent != null ? intent.getStringExtra(LaunchSecurity.EXTRA_APP_ID) : null);
    }

    private String resolveIntentLaunchRouteToken(@Nullable Intent intent) {
        return safeTrim(intent != null ? intent.getStringExtra(LaunchSecurity.EXTRA_LAUNCH_ROUTE_TOKEN) : null);
    }

    private String resolveIntentTemporaryOverrideAppId(@Nullable Intent intent) {
        return safeTrim(intent != null ? intent.getStringExtra(LaunchSecurity.EXTRA_TEMP_OVERRIDE_APP_ID) : null);
    }

    private String resolveContainerLaunchAppId(@Nullable Container targetContainer) {
        return targetContainer == null ? "" : safeTrim(targetContainer.getSessionMetadata("appId", ""));
    }

    private String resolveEffectiveLaunchAppId(@Nullable Intent intent, @Nullable Container targetContainer) {
        String appId = resolveIntentLaunchAppId(intent);
        return appId.isEmpty() ? resolveContainerLaunchAppId(targetContainer) : appId;
    }

    private boolean isSteamExecutableSurface(@Nullable String value) {
        String normalized = safeTrim(value).toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) return false;
        return normalized.contains("steam.exe")
                || normalized.contains("steamwebhelper")
                || normalized.endsWith("/steam")
                || normalized.endsWith("\\steam");
    }

    private boolean shouldAttachSteamClientComponent() {
        if (container == null) return false;
        if (container.isLaunchRealSteam()) return true;

        String appId = safeTrim(activeLaunchAppId);
        if (appId.isEmpty()) appId = resolveIntentLaunchAppId(getIntent());
        if (appId.isEmpty()) appId = resolveContainerLaunchAppId(container);
        if (appId.isEmpty() && shortcut != null) {
            appId = safeTrim(shortcut.getExtra("appId", ""));
        }
        if (appId.startsWith("STEAM_")) return true;

        if (shortcut != null && isSteamExecutableSurface(shortcut.path)) return true;
        return isSteamExecutableSurface(container.getExecutablePath());
    }

    private int resolveIntentContainerId(@Nullable Intent intent) {
        if (intent == null) return 0;
        int containerId = intent.getIntExtra("container_id", 0);
        if (containerId != 0) return containerId;
        String shortcutPath = normalizeShortcutPath(intent.getStringExtra("shortcut_path"));
        if (shortcutPath.isEmpty()) return 0;
        return parseContainerIdFromDesktopFile(new File(shortcutPath));
    }

    private boolean hasLaunchTargetChanged(@Nullable Intent intent) {
        return resolveIntentContainerId(intent) != activeLaunchContainerId
                || !normalizeShortcutPath(intent != null ? intent.getStringExtra("shortcut_path") : null)
                .equals(activeLaunchShortcutPath)
                || !resolveIntentLaunchAppId(intent).equals(activeLaunchAppId)
                || !resolveIntentLaunchRouteToken(intent).equals(activeLaunchRouteToken);
    }

    private void restoreTemporaryOverrideIfNeeded(@NonNull String reason) {
        if (activeTemporaryOverrideRestored) return;
        String appId = safeTrim(activeTemporaryOverrideAppId);
        if (appId.isEmpty()) return;

        activeTemporaryOverrideRestored = true;
        try {
            IntentLaunchManager.INSTANCE.restoreOriginalConfiguration(this, appId);
            IntentLaunchManager.INSTANCE.clearTemporaryOverride(appId);
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "XSERVER_TEMP_OVERRIDE_RESTORED",
                    forensicTraceIdOrNull(),
                    "xserver",
                    "temporary_override_restored",
                    ForensicLogger.fields(
                            "app_id", appId,
                            "reason", reason
                    )
            );
        } catch (Throwable error) {
            ForensicLogger.logEvent(
                    this,
                    "warn",
                    "XSERVER_TEMP_OVERRIDE_RESTORE_FAILED",
                    forensicTraceIdOrNull(),
                    "xserver",
                    "temporary_override_restore_failed",
                    ForensicLogger.fields(
                            "app_id", appId,
                            "reason", reason,
                            "error_class", error.getClass().getName(),
                            "error_detail", String.valueOf(error.getMessage())
                    )
            );
        }
    }

    private boolean isLaunchBindingCurrent(int generation) {
        return generation == launchBindingGeneration && !isFinishing() && !isDestroyed();
    }

    private void recreateForLaunchRelaunch(Intent restartIntent) {
        if (restartIntent != null) {
            restartIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            setIntent(restartIntent);
        }
        recreate();
        overridePendingTransition(0, 0);
    }

    private void bindActiveContainerState(@Nullable Container targetContainer, @Nullable String runtimeModel, @Nullable String runtimeIdentity) {
        if (targetContainer == null || containerManager == null || imageFs == null) return;
        String normalizedRuntimeModel = ContentProfile.normalizeRuntimeModel(runtimeModel);
        if (normalizedRuntimeModel.isEmpty()) {
            normalizedRuntimeModel = ContainerManager.resolveContainerRuntimeModel(targetContainer);
        }
        String normalizedRuntimeIdentity = runtimeIdentity == null || runtimeIdentity.trim().isEmpty()
                ? ContainerManager.resolveContainerRuntimeIdentity(targetContainer)
                : runtimeIdentity.trim();
        imageFs = ImageFs.find(this, normalizedRuntimeModel, normalizedRuntimeIdentity);
        ImageFs.ensureContainerHomeForRuntime(this, targetContainer.id, targetContainer.getRootDir(), normalizedRuntimeModel, normalizedRuntimeIdentity);
        ContainerManager.activateContainerHome(new File(imageFs.getRootDir(), "home"), targetContainer);
        imageFs.setHomeDir(targetContainer.getRootDir());
    }

    private String resolveLaunchRuntimeWinePath(@Nullable WineInfo resolvedWineInfo,
                                                @Nullable ContentProfile runtimeProfile) {
        File profileRoot = resolveProfileRuntimeRoot(runtimeProfile);
        if (profileRoot != null) return profileRoot.getPath();

        String wineInfoPath = resolvedWineInfo != null ? safeTrim(resolvedWineInfo.path) : "";
        if (!wineInfoPath.isEmpty()) {
            File wineInfoRoot = WineUtils.resolveCanonicalRuntimeRoot(new File(wineInfoPath));
            if (wineInfoRoot != null && WineUtils.hasRuntimeCorePayload(wineInfoRoot)) {
                return wineInfoRoot.getPath();
            }
        }
        return wineInfoPath;
    }

    @Nullable
    private File resolveProfileRuntimeRoot(@Nullable ContentProfile runtimeProfile) {
        if (runtimeProfile == null
                || contentsManager == null
                || !runtimeProfile.isWineProtonFamily()
                || !contentsManager.isInstalledProfileUsable(runtimeProfile)) {
            return null;
        }

        File runtimeRoot = WineUtils.resolveCanonicalRuntimeRoot(contentsManager.getRuntimeRootDir(runtimeProfile));
        if (runtimeRoot != null && WineUtils.hasRuntimeCorePayload(runtimeRoot)) {
            return runtimeRoot;
        }

        ContentsManager.InstalledProfileDiagnostics diagnostics =
                contentsManager.resolveInstalledProfileDiagnostics(runtimeProfile);
        if (diagnostics.runtimePayloadPresent && !safeTrim(diagnostics.runtimeRoot).equals("-")) {
            runtimeRoot = WineUtils.resolveCanonicalRuntimeRoot(new File(diagnostics.runtimeRoot));
            if (runtimeRoot != null && WineUtils.hasRuntimeCorePayload(runtimeRoot)) {
                return runtimeRoot;
            }
        }
        return null;
    }

    private String resolveActiveRuntimeIdentity() {
        if (selectedRuntimeProfile != null) {
            return ContentsManager.getEntryName(selectedRuntimeProfile);
        }
        if (wineInfo != null) {
            return wineInfo.identifier();
        }
        String requestedWineVersion = resolveLaunchWineVersion();
        return resolveEffectiveLaunchWineVersion(requestedWineVersion, effectiveRuntimeModel);
    }

    private void prepareRootfsDevInputPath() {
        if (imageFs == null || winHandler == null) return;
        if (ImageFs.ACTIVE_ROOT_DIR_NAME.equals(imageFs.getRootDir().getName())) {
            logBootstrapCheckpoint(
                    "XSERVER_ROOTFS_DEV_INPUT_ALIAS_SKIPPED",
                    "rootfs_dev_input_prepare_skipped_until_runtime_root_bound",
                    "imagefs_root", imageFs.getRootDir().getAbsolutePath(),
                    "active_root_alias", ImageFs.getActiveRootDir(this).getAbsolutePath(),
                    "active_root_alias_target", FileUtils.isSymlink(ImageFs.getActiveRootDir(this)) ? FileUtils.readSymlink(ImageFs.getActiveRootDir(this)) : ""
            );
            return;
        }
        File devInputDir = new File(imageFs.getRootDir(), "dev/input");
        if (devInputDir.exists() || devInputDir.mkdirs()) {
            for (int i = 0; i < 4; i++) {
                File eventFile = new File(devInputDir, "event" + i);
                if (eventFile.exists() && !eventFile.delete()) {
                    Log.w("XServerDisplayActivity", "Failed to delete stale fake input node " + eventFile.getAbsolutePath());
                }
                File jsFile = new File(devInputDir, "js" + i);
                if (jsFile.exists() && !jsFile.delete()) {
                    Log.w("XServerDisplayActivity", "Failed to delete stale fake joystick node " + jsFile.getAbsolutePath());
                }
            }
        }
        winHandler.setFakeInputPath(devInputDir.getAbsolutePath());
    }

    private String normalizeRequestedVulkanApi(String raw) {
        if (raw == null || raw.trim().isEmpty()) return VORTEK_LATEST_VULKAN_API_LABEL;
        Matcher matcher = VULKAN_API_MINOR_PATTERN.matcher(raw);
        if (!matcher.find()) return VORTEK_LATEST_VULKAN_API_LABEL;
        try {
            int minor = Integer.parseInt(matcher.group(1));
            if (minor < 1) minor = 1;
            if (minor > 4) minor = 4;
            return "1." + minor;
        } catch (NumberFormatException ignored) {
            return VORTEK_LATEST_VULKAN_API_LABEL;
        }
    }

    private int getVulkanApiMinor(String apiVersion) {
        if (apiVersion == null || apiVersion.trim().isEmpty()) return 4;
        Matcher matcher = VULKAN_API_MINOR_PATTERN.matcher(apiVersion);
        if (!matcher.find()) return 4;
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return 4;
        }
    }

    private int parseRequestedVulkanApiCeiling(String apiVersion) {
        String normalized = normalizeRequestedVulkanApi(apiVersion);
        int requested = GPUHelper.vkMakeVersion(normalized);
        if (requested == 0) return VORTEK_LATEST_VULKAN_API_VERSION;
        int minor = GPUHelper.vkVersionMinor(requested);
        if (minor >= 4) return VORTEK_LATEST_VULKAN_API_VERSION;
        return GPUHelper.vkMakeVersion(1, minor, 4095);
    }

    private int queryPhysicalVulkanApiVersion() {
        try {
            int version = GPUHelper.vkGetApiVersion();
            return version == 0 ? GPUHelper.vkGetApiVersionSafe() : version;
        } catch (Throwable ignored) {
            return GPUHelper.vkGetApiVersionSafe();
        }
    }

    private String resolveVortekManifestApiVersion(String apiVersion) {
        int requested = parseRequestedVulkanApiCeiling(apiVersion);
        int physical = queryPhysicalVulkanApiVersion();
        int effective = GPUHelper.vkMinVersion(requested, physical);
        return GPUHelper.vkVersionToString(effective);
    }

    private int parseMaxVulkanMinor(ContentProfile profile) {
        return resolveVulkanApiRange(profile)[1];
    }

    private int parseMinVulkanMinor(ContentProfile profile) {
        return resolveVulkanApiRange(profile)[0];
    }

    private int[] resolveVulkanApiRange(ContentProfile profile) {
        if (profile == null) return new int[]{0, 0};
        int min = profile.vulkanApiMin;
        int max = profile.vulkanApiMax;
        if (min > 0 && max > 0) {
            if (min > max) {
                int swap = min;
                min = max;
                max = swap;
            }
            return new int[]{min, max};
        }

        int inferredMax = 0;
        inferredMax = Math.max(inferredMax, parseMaxVulkanMinor(profile.verName));
        inferredMax = Math.max(inferredMax, parseMaxVulkanMinor(profile.desc));
        inferredMax = Math.max(inferredMax, parseMaxVulkanMinor(profile.releaseTag));
        if (inferredMax > 0) return new int[]{1, inferredMax};
        return new int[]{0, 0};
    }

    private int parseMaxVulkanMinor(String raw) {
        if (raw == null || raw.trim().isEmpty()) return 0;
        int maxMinor = 0;
        Matcher matcher = VULKAN_API_MINOR_PATTERN.matcher(raw);
        while (matcher.find()) {
            try {
                maxMinor = Math.max(maxMinor, Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException ignored) {
            }
        }
        return maxMinor;
    }

    private long parsePublishedAtKey(String value) {
        if (value == null) return 0L;
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0L;
        if (digits.length() > 14) digits = digits.substring(0, 14);
        try {
            return Long.parseLong(digits);
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private long resolveProfilePublishedAtKey(ContentProfile profile) {
        return profile == null ? 0L : parsePublishedAtKey(profile.publishedAt);
    }

    private String joinCsv(List<String> values) {
        if (values == null || values.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) continue;
            if (builder.length() > 0) builder.append(',');
            builder.append(value.trim());
        }
        return builder.toString();
    }

    private KeyValueSet getGraphicsDriverKeyValueConfig() {
        return GraphicsDrivers.toKeyValueSetConfig(graphicsDriver, rawGraphicsDriverConfig);
    }

    private void ensureGraphicsDriverAssetExtracted(File rootDir, String assetName, String probeRelativePath) {
        if (rootDir == null || assetName == null || assetName.trim().isEmpty()) return;
        File probeFile = probeRelativePath == null || probeRelativePath.trim().isEmpty()
                ? null
                : new File(rootDir, probeRelativePath);
        if (probeFile != null && probeFile.isFile()) {
            if (!GraphicsElfCompatibility.hasForbiddenBionicToken(probeFile)) return;
            FileUtils.delete(probeFile);
        }
        TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, assetName, rootDir);
    }

    @Nullable
    private File rewriteVortekIcdFile(File rootDir, @Nullable String libraryRelativePath, @Nullable String apiVersion) {
        if (rootDir == null) return null;
        File icdFile = new File(rootDir, "usr/share/vulkan/icd.d/vortek_icd.aarch64.json");
        if (!icdFile.isFile()) return null;

        String normalizedLibraryPath = trimToEmpty(libraryRelativePath);
        if (normalizedLibraryPath.startsWith("/")) normalizedLibraryPath = normalizedLibraryPath.substring(1);
        File libraryFile = normalizedLibraryPath.isEmpty()
                ? new File(rootDir, "usr/lib/libvulkan_vortek.so")
                : new File(rootDir, normalizedLibraryPath);
        if (!GraphicsElfCompatibility.isBionicCompatibleLibrary(libraryFile)) return null;
        try {
            JSONObject root = new JSONObject(FileUtils.readString(icdFile));
            JSONObject icd = root.optJSONObject("ICD");
            if (icd == null) {
                icd = new JSONObject();
                root.put("ICD", icd);
            }
            icd.put("library_path", libraryFile.getAbsolutePath());
            icd.put("api_version", resolveVortekManifestApiVersion(apiVersion));
            FileUtils.writeString(icdFile, root.toString(2));
        } catch (Exception e) {
            Log.w(TAG, "Failed to rewrite Vortek ICD manifest", e);
        }
        return icdFile;
    }

    private boolean hasAccessibleRenderNode() {
        File driDir = new File("/dev/dri");
        File[] nodes = driDir.listFiles((dir, name) -> name != null && name.startsWith("renderD"));
        return nodes != null && nodes.length > 0;
    }

    private String buildVortekRuntimeSource(VortekVulkanDriverPackageManager.PackageInfo driverInfo) {
        if (driverInfo == null || driverInfo.builtin) return "android-system-hal";
        String transport = trimToEmpty(driverInfo.transport);
        if (transport.isEmpty()) return "custom-userspace-wrapper";
        return "custom-" + transport;
    }

    private void applyGraphicsDriverMetadataEnv(String prefix,
                                                String providerLane,
                                                String driverKind,
                                                String transport,
                                                String supportClass,
                                                String kernelEvidenceClass,
                                                String transportRequirements,
                                                String ownerLane,
                                                String routeId,
                                                String rankedKernelDonors,
                                                String diagnosticKeys,
                                                boolean requiresRenderNode) {
        setOrClearEnv(prefix + "_PROVIDER_LANE", trimToEmpty(providerLane));
        setOrClearEnv(prefix + "_DRIVER_KIND", trimToEmpty(driverKind));
        setOrClearEnv(prefix + "_DRIVER_TRANSPORT", trimToEmpty(transport));
        setOrClearEnv(prefix + "_SUPPORT_CLASS", trimToEmpty(supportClass));
        setOrClearEnv(prefix + "_KERNEL_EVIDENCE_CLASS", trimToEmpty(kernelEvidenceClass));
        setOrClearEnv(prefix + "_TRANSPORT_REQUIREMENTS", trimToEmpty(transportRequirements));
        setOrClearEnv(prefix + "_OWNER_LANE", trimToEmpty(ownerLane));
        setOrClearEnv(prefix + "_ROUTE_ID", trimToEmpty(routeId));
        setOrClearEnv(prefix + "_RANKED_KERNEL_DONORS", trimToEmpty(rankedKernelDonors));
        setOrClearEnv(prefix + "_DIAGNOSTIC_KEYS", trimToEmpty(diagnosticKeys));
        setOrClearEnv(prefix + "_REQUIRES_RENDER_NODE", requiresRenderNode ? "1" : "");
    }

    private boolean isAeMaliVulkanDriver(VortekVulkanDriverPackageManager.PackageInfo driverInfo) {
        if (driverInfo == null || driverInfo.builtin) return false;
        String kind = trimToEmpty(driverInfo.driverKind).toLowerCase(Locale.US);
        String lane = trimToEmpty(driverInfo.providerLane).toLowerCase(Locale.US);
        String repo = trimToEmpty(driverInfo.sourceRepo).toLowerCase(Locale.US);
        return kind.contains("aemali")
                || kind.contains("panvk")
                || lane.contains("aemali")
                || lane.contains("mali-panvk")
                || (repo.contains("mesa") && kind.contains("mali"));
    }

    private void applyAeMaliPolicyEnv(VortekVulkanDriverPackageManager.PackageInfo driverInfo,
                                      String extensionProfile,
                                      String selectedVulkanApi,
                                      boolean renderNodeAvailable) {
        boolean aeMaliDriver = isAeMaliVulkanDriver(driverInfo);
        String selectedApiCeiling = aeMaliDriver
                ? firstNonEmpty(trimToEmpty(driverInfo.vulkanApiCeiling), selectedVulkanApi)
                : "";
        boolean renderNodeRequired = aeMaliDriver && driverInfo.requiresRenderNode;
        setOrClearEnv("AEMALI_DRIVER", aeMaliDriver ? "1" : "");
        setOrClearEnv("AERO_MESA_DRIVER", aeMaliDriver ? "aemali" : "");
        setOrClearEnv("AEMALI_PROFILE", aeMaliDriver ? extensionProfile : "");
        setOrClearEnv("AEMALI_VK_API_CEILING", selectedApiCeiling);
        setOrClearEnv("AEMALI_ROUTE", aeMaliDriver ? buildVortekRuntimeSource(driverInfo) : "");
        setOrClearEnv(
                "AEMALI_TRANSPORT",
                aeMaliDriver ? firstNonEmpty(trimToEmpty(driverInfo.transport), "drm-render-node-experimental") : ""
        );
        applyGraphicsDriverMetadataEnv(
                "AEMALI",
                aeMaliDriver ? driverInfo.providerLane : "",
                aeMaliDriver ? driverInfo.driverKind : "",
                aeMaliDriver ? firstNonEmpty(trimToEmpty(driverInfo.transport), "drm-render-node-experimental") : "",
                aeMaliDriver ? driverInfo.supportClass : "",
                aeMaliDriver ? driverInfo.kernelEvidenceClass : "",
                aeMaliDriver ? driverInfo.transportRequirements : "",
                aeMaliDriver ? driverInfo.ownerLane : "",
                aeMaliDriver ? driverInfo.routeId : "",
                aeMaliDriver ? driverInfo.rankedKernelDonors : "",
                aeMaliDriver ? driverInfo.diagnosticKeys : "",
                renderNodeRequired
        );
        setOrClearEnv("AEMALI_RENDER_NODE_REQUIRED", renderNodeRequired ? "1" : "");
        setOrClearEnv("AEMALI_RENDER_NODE_PRESENT", aeMaliDriver && renderNodeAvailable ? "1" : "");
        setOrClearEnv(
                "AEMALI_TRANSPORT_BLOCK_REASON",
                renderNodeRequired && !renderNodeAvailable ? "missing_render_node" : ""
        );
    }

    private boolean isAeMaliOpenGlProvider(String providerLane, String driverKind, String sourceRepo) {
        String lane = trimToEmpty(providerLane).toLowerCase(Locale.US);
        String kind = trimToEmpty(driverKind).toLowerCase(Locale.US);
        String repo = trimToEmpty(sourceRepo).toLowerCase(Locale.US);
        return lane.contains("aemali")
                || kind.contains("aemali")
                || kind.contains("panfrost")
                || kind.contains("lima")
                || (repo.contains("mesa") && kind.contains("gallium"));
    }

    private void applyAeMaliOpenGlPolicyEnv(String providerLane,
                                            String driverKind,
                                            String sourceRepo,
                                            String transport,
                                            String supportClass,
                                            String kernelEvidenceClass,
                                            String transportRequirements,
                                            String ownerLane,
                                            String routeId,
                                            String rankedKernelDonors,
                                            String diagnosticKeys,
                                            boolean requiresRenderNode,
                                            boolean renderNodeAvailable,
                                            String galliumDriver,
                                            String graphicsStackProfile) {
        boolean aeMaliOpenGl = isAeMaliOpenGlProvider(providerLane, driverKind, sourceRepo);
        setOrClearEnv("AEMALI_OPENGL", aeMaliOpenGl ? "1" : "");
        setOrClearEnv("AEMALI_OPENGL_DRIVER", aeMaliOpenGl ? "aemali-gallium" : "");
        setOrClearEnv("AEMALI_OPENGL_GALLIUM_DRIVER", aeMaliOpenGl ? trimToEmpty(galliumDriver) : "");
        setOrClearEnv("AEMALI_OPENGL_PROFILE", aeMaliOpenGl ? firstNonEmpty(trimToEmpty(graphicsStackProfile), "aemali-universal") : "");
        setOrClearEnv("AEMALI_OPENGL_ROUTE", aeMaliOpenGl ? firstNonEmpty(trimToEmpty(routeId), "aemali-gallium") : "");
        setOrClearEnv(
                "AEMALI_OPENGL_TRANSPORT",
                aeMaliOpenGl ? firstNonEmpty(trimToEmpty(transport), "drm-render-node-experimental") : ""
        );
        applyGraphicsDriverMetadataEnv(
                "AEMALI_OPENGL",
                aeMaliOpenGl ? providerLane : "",
                aeMaliOpenGl ? driverKind : "",
                aeMaliOpenGl ? firstNonEmpty(trimToEmpty(transport), "drm-render-node-experimental") : "",
                aeMaliOpenGl ? supportClass : "",
                aeMaliOpenGl ? kernelEvidenceClass : "",
                aeMaliOpenGl ? transportRequirements : "",
                aeMaliOpenGl ? ownerLane : "",
                aeMaliOpenGl ? routeId : "",
                aeMaliOpenGl ? rankedKernelDonors : "",
                aeMaliOpenGl ? diagnosticKeys : "",
                aeMaliOpenGl && requiresRenderNode
        );
        setOrClearEnv("AEMALI_OPENGL_RENDER_NODE_REQUIRED", aeMaliOpenGl && requiresRenderNode ? "1" : "");
        setOrClearEnv("AEMALI_OPENGL_RENDER_NODE_PRESENT", aeMaliOpenGl && renderNodeAvailable ? "1" : "");
        setOrClearEnv(
                "AEMALI_OPENGL_TRANSPORT_BLOCK_REASON",
                aeMaliOpenGl && requiresRenderNode && !renderNodeAvailable ? "missing_render_node" : ""
        );
    }

    private void applyAeMaliOpenGlMesaCompatEnv(KeyValueSet driverKeyValueConfig,
                                                boolean aeMaliOpenGl,
                                                String galliumDriver) {
        String mesaExtensionOverride = aeMaliOpenGl
                ? GraphicsDrivers.buildMesaExtensionOverride(
                        driverKeyValueConfig.getBoolean("disableGLKHRDebug", true),
                        driverKeyValueConfig.getBoolean("disableVertexArrayBGRA", true),
                        driverKeyValueConfig.get("extraDisabledExtensions", "")
                )
                : "";
        setOrClearEnv("MESA_EXTENSION_OVERRIDE", mesaExtensionOverride);
        setOrClearEnv(
                "MESA_GL_VERSION_OVERRIDE",
                aeMaliOpenGl
                        ? firstNonEmpty(trimToEmpty(driverKeyValueConfig.get("glVersion")), "3.1")
                        : ""
        );
        if (aeMaliOpenGl && !trimToEmpty(galliumDriver).isEmpty()) {
            envVars.put("GALLIUM_DRIVER", trimToEmpty(galliumDriver));
        }
    }

    private static final class GladioOverlayState {
        GraphicsDrivers.BundledDriverAsset bundledAsset;
        GladioOpenGLDriverPackageManager.PackageInfo customInfo;
        String requestedEntry = "";
        String activeEntry = "";
        String packageLabel = "";
        String version = "";
        String sourceRepo = "";
        String driverKind = "";
        String transport = "";
        String providerLane = "";
        String supportClass = "";
        String kernelEvidenceClass = "";
        String transportRequirements = "";
        String ownerLane = "";
        String routeId = "";
        String rankedKernelDonors = "";
        String diagnosticKeys = "";
        String graphicsStackProfile = "";
        String preferredGalliumDriver = "";
        boolean requiresRenderNode = false;
        boolean customRequested = false;
        boolean customOverlayReady = false;
        boolean degraded = false;
        String degradedReason = "";
    }

    private static final class VortekWrapperState {
        GraphicsDrivers.BundledDriverAsset bundledAsset;
        VortekWrapperPackageManager.PackageInfo packageInfo;
        String requestedEntry = "";
        String activeEntry = "";
        String packageLabel = "";
        String version = "";
        String sourceRepo = "";
        String rootLibraryPath = "";
        String driverKind = "";
        String transport = "";
        String providerLane = "";
        String supportClass = "";
        String kernelEvidenceClass = "";
        String transportRequirements = "";
        String ownerLane = "";
        String routeId = "";
        String rankedKernelDonors = "";
        String diagnosticKeys = "";
        String graphicsStackProfile = "";
        boolean requiresRenderNode = false;
    }

    private VortekWrapperState resolveVortekWrapperState(String requestedEntry) {
        VortekWrapperState state = new VortekWrapperState();
        state.requestedEntry = trimToEmpty(requestedEntry);
        VortekWrapperPackageManager packageManager = new VortekWrapperPackageManager(this);
        state.bundledAsset = GraphicsDrivers.getBundledDriverAsset(
                this,
                GraphicsDrivers.VORTEK,
                state.requestedEntry
        );
        state.activeEntry = state.bundledAsset.version;
        state.packageInfo = packageManager.getPackageInfo(state.bundledAsset.version);
        state.packageLabel = state.packageInfo == null
                ? state.bundledAsset.packageLabel
                : state.packageInfo.getDisplayLabel();
        state.version = state.packageInfo == null || trimToEmpty(state.packageInfo.version).isEmpty()
                ? state.bundledAsset.version
                : trimToEmpty(state.packageInfo.version);
        state.sourceRepo = state.packageInfo == null ? "" : trimToEmpty(state.packageInfo.sourceRepo);
        state.rootLibraryPath = state.packageInfo == null
                ? "usr/lib/libvulkan_vortek.so"
                : firstNonEmpty(trimToEmpty(state.packageInfo.rootLibraryPath), "usr/lib/libvulkan_vortek.so");
        state.providerLane = state.packageInfo == null
                ? "vortek-wrapper-vulkan"
                : firstNonEmpty(trimToEmpty(state.packageInfo.providerLane), "vortek-wrapper-vulkan");
        state.driverKind = state.packageInfo == null
                ? "vortek-wrapper"
                : firstNonEmpty(trimToEmpty(state.packageInfo.driverKind), "vortek-wrapper");
        state.transport = state.packageInfo == null
                ? "bundled-root-overlay"
                : firstNonEmpty(trimToEmpty(state.packageInfo.transport), "bundled-root-overlay");
        state.supportClass = state.packageInfo == null ? "" : trimToEmpty(state.packageInfo.supportClass);
        state.kernelEvidenceClass = state.packageInfo == null ? "" : trimToEmpty(state.packageInfo.kernelEvidenceClass);
        state.transportRequirements = state.packageInfo == null ? "" : trimToEmpty(state.packageInfo.transportRequirements);
        state.ownerLane = state.packageInfo == null ? "" : trimToEmpty(state.packageInfo.ownerLane);
        state.routeId = state.packageInfo == null ? "" : trimToEmpty(state.packageInfo.routeId);
        state.rankedKernelDonors = state.packageInfo == null ? "" : trimToEmpty(state.packageInfo.rankedKernelDonors);
        state.diagnosticKeys = state.packageInfo == null ? "" : trimToEmpty(state.packageInfo.diagnosticKeys);
        state.graphicsStackProfile = state.packageInfo == null ? "" : trimToEmpty(state.packageInfo.graphicsStackProfile);
        state.requiresRenderNode = state.packageInfo != null && state.packageInfo.requiresRenderNode;
        return state;
    }

    private GladioOverlayState resolveGladioOverlayState(File rootDir, String requestedEntry) {
        GladioOverlayState state = new GladioOverlayState();
        state.requestedEntry = trimToEmpty(requestedEntry);
        GladioOpenGLDriverPackageManager packageManager = new GladioOpenGLDriverPackageManager(this);
        state.bundledAsset = GraphicsDrivers.getBundledDriverAsset(
                this,
                GraphicsDrivers.GLADIO,
                state.requestedEntry
        );
        GladioOpenGLDriverPackageManager.PackageInfo bundledInfo =
                packageManager.getPackageInfo(state.bundledAsset.version);
        state.activeEntry = state.bundledAsset.version;
        state.packageLabel = bundledInfo == null ? state.bundledAsset.packageLabel : bundledInfo.getDisplayLabel();
        state.version = bundledInfo == null || trimToEmpty(bundledInfo.version).isEmpty()
                ? state.bundledAsset.version
                : trimToEmpty(bundledInfo.version);
        state.sourceRepo = bundledInfo == null
                ? "https://github.com/Pipetto-crypto/gladiorenderer"
                : trimToEmpty(bundledInfo.sourceRepo);
        state.providerLane = bundledInfo == null
                ? "gladio-opengl"
                : firstNonEmpty(trimToEmpty(bundledInfo.providerLane), "gladio-opengl");
        state.driverKind = bundledInfo == null
                ? "opengl-wrapper"
                : firstNonEmpty(trimToEmpty(bundledInfo.driverKind), "opengl-wrapper");
        state.transport = bundledInfo == null
                ? "bundled-root-overlay"
                : firstNonEmpty(trimToEmpty(bundledInfo.transport), "bundled-root-overlay");
        state.supportClass = bundledInfo == null ? "" : trimToEmpty(bundledInfo.supportClass);
        state.kernelEvidenceClass = bundledInfo == null ? "" : trimToEmpty(bundledInfo.kernelEvidenceClass);
        state.transportRequirements = bundledInfo == null ? "" : trimToEmpty(bundledInfo.transportRequirements);
        state.ownerLane = bundledInfo == null ? "" : trimToEmpty(bundledInfo.ownerLane);
        state.routeId = bundledInfo == null ? "" : trimToEmpty(bundledInfo.routeId);
        state.rankedKernelDonors = bundledInfo == null ? "" : trimToEmpty(bundledInfo.rankedKernelDonors);
        state.diagnosticKeys = bundledInfo == null ? "" : trimToEmpty(bundledInfo.diagnosticKeys);
        state.graphicsStackProfile = bundledInfo == null ? "" : trimToEmpty(bundledInfo.graphicsStackProfile);
        state.preferredGalliumDriver = bundledInfo == null ? "" : trimToEmpty(bundledInfo.preferredGalliumDriver);
        state.requiresRenderNode = bundledInfo != null && bundledInfo.requiresRenderNode;

        boolean aeMaliGalliumRequested = GladioOpenGLDriverPackageManager.isAeMaliPackageEntry(state.requestedEntry);
        state.customRequested = GladioOpenGLDriverPackageManager.isCustomPackageEntry(state.requestedEntry)
                || aeMaliGalliumRequested;
        if (!state.customRequested) {
            ensureGraphicsDriverAssetExtracted(
                    rootDir,
                    state.bundledAsset.assetPath,
                    state.bundledAsset.extractProbePath
            );
            return state;
        }

        state.customInfo = packageManager.getPackageInfo(state.requestedEntry);
        if (state.customInfo == null) {
            state.degraded = true;
            state.degradedReason = aeMaliGalliumRequested
                    ? "gladio_aemali_gallium_package_missing"
                    : "gladio_custom_package_missing";
            ensureGraphicsDriverAssetExtracted(
                    rootDir,
                    state.bundledAsset.assetPath,
                    state.bundledAsset.extractProbePath
            );
            return state;
        }

        state.packageLabel = state.customInfo.getDisplayLabel();
        state.version = trimToEmpty(state.customInfo.version);
        if (state.version.isEmpty()) state.version = GladioOpenGLDriverPackageManager.toEntryId(state.requestedEntry);
        state.sourceRepo = trimToEmpty(state.customInfo.sourceRepo);
        state.providerLane = trimToEmpty(state.customInfo.providerLane);
        state.driverKind = firstNonEmpty(trimToEmpty(state.customInfo.driverKind), "opengl-wrapper");
        state.transport = firstNonEmpty(trimToEmpty(state.customInfo.transport), "root-overlay");
        state.supportClass = trimToEmpty(state.customInfo.supportClass);
        state.kernelEvidenceClass = trimToEmpty(state.customInfo.kernelEvidenceClass);
        state.transportRequirements = trimToEmpty(state.customInfo.transportRequirements);
        state.ownerLane = trimToEmpty(state.customInfo.ownerLane);
        state.routeId = trimToEmpty(state.customInfo.routeId);
        state.rankedKernelDonors = trimToEmpty(state.customInfo.rankedKernelDonors);
        state.diagnosticKeys = trimToEmpty(state.customInfo.diagnosticKeys);
        state.graphicsStackProfile = trimToEmpty(state.customInfo.graphicsStackProfile);
        state.preferredGalliumDriver = trimToEmpty(state.customInfo.preferredGalliumDriver);
        state.requiresRenderNode = state.customInfo.requiresRenderNode;
        state.activeEntry = state.requestedEntry;
        state.customOverlayReady = packageManager.deployPackageToRoot(rootDir, state.requestedEntry);
        if (!state.customOverlayReady) {
            state.degraded = true;
            state.degradedReason = aeMaliGalliumRequested
                    ? "gladio_aemali_gallium_overlay_deploy_failed"
                    : "gladio_custom_overlay_deploy_failed";
            state.activeEntry = state.bundledAsset.version;
            state.packageLabel = bundledInfo == null ? state.bundledAsset.packageLabel : bundledInfo.getDisplayLabel();
            state.version = bundledInfo == null || trimToEmpty(bundledInfo.version).isEmpty()
                    ? state.bundledAsset.version
                    : trimToEmpty(bundledInfo.version);
            state.sourceRepo = bundledInfo == null
                    ? "https://github.com/Pipetto-crypto/gladiorenderer"
                    : trimToEmpty(bundledInfo.sourceRepo);
            state.providerLane = bundledInfo == null
                    ? "gladio-opengl"
                    : firstNonEmpty(trimToEmpty(bundledInfo.providerLane), "gladio-opengl");
            state.driverKind = bundledInfo == null
                    ? "opengl-wrapper"
                    : firstNonEmpty(trimToEmpty(bundledInfo.driverKind), "opengl-wrapper");
            state.transport = bundledInfo == null
                    ? "bundled-root-overlay"
                    : firstNonEmpty(trimToEmpty(bundledInfo.transport), "bundled-root-overlay");
            state.supportClass = bundledInfo == null ? "" : trimToEmpty(bundledInfo.supportClass);
            state.kernelEvidenceClass = bundledInfo == null ? "" : trimToEmpty(bundledInfo.kernelEvidenceClass);
            state.transportRequirements = bundledInfo == null ? "" : trimToEmpty(bundledInfo.transportRequirements);
            state.ownerLane = bundledInfo == null ? "" : trimToEmpty(bundledInfo.ownerLane);
            state.routeId = bundledInfo == null ? "" : trimToEmpty(bundledInfo.routeId);
            state.rankedKernelDonors = bundledInfo == null ? "" : trimToEmpty(bundledInfo.rankedKernelDonors);
            state.diagnosticKeys = bundledInfo == null ? "" : trimToEmpty(bundledInfo.diagnosticKeys);
            state.graphicsStackProfile = bundledInfo == null ? "" : trimToEmpty(bundledInfo.graphicsStackProfile);
            state.preferredGalliumDriver = bundledInfo == null ? "" : trimToEmpty(bundledInfo.preferredGalliumDriver);
            state.requiresRenderNode = bundledInfo != null && bundledInfo.requiresRenderNode;
            ensureGraphicsDriverAssetExtracted(
                    rootDir,
                    state.bundledAsset.assetPath,
                    state.bundledAsset.extractProbePath
            );
        }
        return state;
    }

    private String resolveDriverVulkanPatch(String adrenoToolsDriverId) {
        try {
            String driverVersion = GPUInformation.getVulkanVersion(adrenoToolsDriverId, this);
            String[] parts = driverVersion.split("\\.");
            if (parts.length >= 3) return parts[2];
            if (parts.length >= 2) return parts[1];
        } catch (Exception ignored) {
        }
        return "0";
    }

    private void purgeLegacyVulkanRuntimeResidue(File rootDir) {
        if (rootDir == null) return;
        String legacyPayloadStem = "vulkan" + "-sdk";
        File legacyShare = new File(rootDir, "usr/share/" + legacyPayloadStem);
        File legacyLib = new File(rootDir, "usr/lib/" + legacyPayloadStem);
        if (legacyShare.exists()) FileUtils.delete(legacyShare);
        if (legacyLib.exists()) FileUtils.delete(legacyLib);
    }

    @Nullable
    private File resolveWrapperIcdSourceFile() {
        if (imageFs == null) return null;
        File runtimeShareDir = WineUtils.resolveRuntimeShareDir(new File(imageFs.getWinePath()));
        if (runtimeShareDir != null) {
            File runtimeWrapperIcd = new File(runtimeShareDir, "vulkan/icd.d/wrapper_icd.aarch64.json");
            if (runtimeWrapperIcd.isFile()) return runtimeWrapperIcd;
        }
        File imageFsWrapperIcd = new File(imageFs.getShareDir(), "vulkan/icd.d/wrapper_icd.aarch64.json");
        if (imageFsWrapperIcd.isFile()) return imageFsWrapperIcd;
        return null;
    }

    @Nullable
    private File rewriteWrapperIcdForAndroidHost(@Nullable File sourceIcdFile) {
        if (sourceIcdFile == null || !sourceIcdFile.isFile() || imageFs == null) return null;
        String nativeLibDir = trimToEmpty(AppUtils.getNativeLibDir(this));
        if (nativeLibDir.isEmpty()) return null;

        File nativeWrapperLib = new File(nativeLibDir, "libvulkan_wrapper.so");
        if (!nativeWrapperLib.isFile()) {
            ForensicLogger.logEvent(
                    this,
                    "warn",
                    "WRAPPER_ICD_ANDROID_HOST_REWRITE_MISSING_NATIVE_LIB",
                    null,
                    "graphics_provider",
                    "wrapper_icd_android_host_rewrite_missing_native_lib",
                    ForensicLogger.fields(
                            "source_icd_path", sourceIcdFile.getAbsolutePath(),
                            "native_wrapper_lib_path", nativeWrapperLib.getAbsolutePath()
                    )
            );
            return null;
        }

        File icdDir = new File(imageFs.getShareDir(), "vulkan/icd.d");
        File androidHostIcd = new File(icdDir, "wrapper_icd.android-host.aarch64.json");
        ensureAndroidHostWrapperDependencyClosure(new File(nativeLibDir), imageFs.getAndroidHostLibDir());
        String missingDependencies = collectAndroidHostWrapperMissingDependencies(new File(nativeLibDir), imageFs.getAndroidHostLibDir());
        if (!missingDependencies.isEmpty()) {
            if (androidHostIcd.isFile()) FileUtils.delete(androidHostIcd);
            ForensicLogger.logEvent(
                    this,
                    "warn",
                    "WRAPPER_ICD_ANDROID_HOST_REWRITE_UNSATISFIED_CLOSURE",
                    null,
                    "graphics_provider",
                    "wrapper_icd_android_host_rewrite_unsatisfied_closure",
                    ForensicLogger.fields(
                            "source_icd_path", sourceIcdFile.getAbsolutePath(),
                            "stale_icd_path", androidHostIcd.getAbsolutePath(),
                            "native_wrapper_lib_path", nativeWrapperLib.getAbsolutePath(),
                            "native_lib_dir", nativeLibDir,
                            "host_lib_dir", imageFs.getAndroidHostLibDir().getAbsolutePath(),
                            "missing_libs", missingDependencies
                    )
            );
            return null;
        }

        if (!icdDir.isDirectory() && !icdDir.mkdirs()) return null;
        try {
            String rewrittenManifest = VulkanIcdManifestHelper.rewriteLibraryPath(
                    FileUtils.readString(sourceIcdFile),
                    nativeWrapperLib.getAbsolutePath(),
                    null
            );
            if (!FileUtils.writeString(androidHostIcd, rewrittenManifest)) return null;
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "WRAPPER_ICD_ANDROID_HOST_REWRITE_APPLIED",
                    null,
                    "graphics_provider",
                    "wrapper_icd_android_host_rewrite_applied",
                    ForensicLogger.fields(
                            "source_icd_path", sourceIcdFile.getAbsolutePath(),
                            "resolved_icd_path", androidHostIcd.getAbsolutePath(),
                            "native_wrapper_lib_path", nativeWrapperLib.getAbsolutePath()
                    )
            );
            return androidHostIcd.isFile() ? androidHostIcd : null;
        } catch (Exception e) {
            Log.w(TAG, "Failed to rewrite wrapper ICD for Android host", e);
            return null;
        }
    }

    private void ensureAndroidHostWrapperDependencyClosure(File nativeLibDir, File hostLibDir) {
        if (imageFs == null || hostLibDir == null) return;
        File guestLibDir = imageFs.getLibDir();
        if (!hostLibDir.isDirectory()) hostLibDir.mkdirs();

        int copied = 0;
        int stillMissing = 0;
        ArrayList<String> samples = new ArrayList<>();
        for (String dependency : ANDROID_HOST_WRAPPER_REQUIRED_LIBS) {
            if (dependency == null || dependency.trim().isEmpty()) continue;
            if (hasAndroidHostWrapperDependency(dependency, nativeLibDir, hostLibDir)) continue;

            File source = new File(guestLibDir, dependency);
            File target = new File(hostLibDir, dependency);
            if (source.isFile()
                    && FileUtils.copy(source, target)
                    && target.isFile()
                    && target.length() == source.length()) {
                FileUtils.chmod(target, 0755);
                copied++;
                addSample(samples, "copied:" + dependency);
            } else {
                stillMissing++;
                addSample(samples, "missing:" + dependency);
            }
        }

        if (copied > 0 || stillMissing > 0) {
            ForensicLogger.logEvent(
                    this,
                    stillMissing == 0 ? "info" : "warn",
                    "WRAPPER_ANDROID_HOST_DEPENDENCY_CLOSURE",
                    null,
                    "graphics_provider",
                    stillMissing == 0 ? "wrapper_android_host_dependency_closure_ready" : "wrapper_android_host_dependency_closure_incomplete",
                    ForensicLogger.fields(
                            "native_lib_dir", nativeLibDir != null ? nativeLibDir.getAbsolutePath() : "",
                            "guest_lib_dir", guestLibDir.getAbsolutePath(),
                            "host_lib_dir", hostLibDir.getAbsolutePath(),
                            "copied", copied,
                            "still_missing", stillMissing,
                            "sample_count", samples.size(),
                            "samples", String.join(" | ", samples)
                    )
            );
        }
    }

    private String collectAndroidHostWrapperMissingDependencies(File nativeLibDir, File hostLibDir) {
        ArrayList<String> missing = new ArrayList<>();
        for (String dependency : ANDROID_HOST_WRAPPER_REQUIRED_LIBS) {
            if (dependency == null || dependency.trim().isEmpty()) continue;
            if (hasAndroidHostWrapperDependency(dependency, nativeLibDir, hostLibDir)) continue;
            missing.add(dependency);
        }
        return String.join(",", missing);
    }

    private boolean hasAndroidHostWrapperDependency(String dependency, File nativeLibDir, File hostLibDir) {
        if (new File(nativeLibDir, dependency).isFile()) return true;
        return new File(hostLibDir, dependency).isFile();
    }

    @Nullable
    private File resolveWrapperIcdFile() {
        File sourceIcdFile = resolveWrapperIcdSourceFile();
        if (sourceIcdFile == null) return null;
        File androidHostIcd = rewriteWrapperIcdForAndroidHost(sourceIcdFile);
        return androidHostIcd != null && androidHostIcd.isFile() ? androidHostIcd : null;
    }

    private int resolveWrapperIcdApiMinor(@Nullable File wrapperIcdFile) {
        if (wrapperIcdFile == null || !wrapperIcdFile.isFile()) return 0;
        try {
            return VulkanIcdManifestHelper.readApiMinor(FileUtils.readString(wrapperIcdFile));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static boolean isMaliSocClass(String socClass) {
        String normalized = trimToEmpty(socClass).toLowerCase(Locale.US);
        return normalized.startsWith("mali-");
    }

    private String resolveBundledAeMaliPanvkEntry() {
        GraphicsDrivers.BundledDriverAsset asset = GraphicsDrivers.resolveBundledDriverAsset(
                this,
                GraphicsDrivers.AEMALI_PANVK
        );
        return asset == null ? "" : VortekVulkanDriverPackageManager.toBundledAeMaliEntry(asset.version);
    }

    @Nullable
    private AdrenotoolsManager.DriverPackageInfo resolveOpenGlBridgeTurnipInfo(
            AdrenotoolsManager adrenotoolsManager,
            String requestedDriverId
    ) {
        String normalizedRequestedDriverId = trimToEmpty(requestedDriverId);
        AdrenotoolsManager.DriverPackageInfo referenceInfo = normalizedRequestedDriverId.isEmpty()
                ? null
                : adrenotoolsManager.getDriverPackageInfo(normalizedRequestedDriverId);

        if (referenceInfo == null || referenceInfo.isSystemSelection()) {
            String preferredDriverId = trimToEmpty(adrenotoolsManager.getPreferredWrapperDriverId());
            if (!preferredDriverId.isEmpty() && !DefaultVersion.WRAPPER.equalsIgnoreCase(preferredDriverId)) {
                AdrenotoolsManager.DriverPackageInfo preferredInfo =
                        adrenotoolsManager.getDriverPackageInfo(preferredDriverId);
                if (preferredInfo != null && !preferredInfo.isSystemSelection()) {
                    referenceInfo = preferredInfo;
                }
            }
        }

        AdrenotoolsManager.DriverPackageInfo resolved =
                adrenotoolsManager.resolvePreferredDriverForLane("turnip-vulkan", referenceInfo);
        return resolved != null && !resolved.isSystemSelection() ? resolved : null;
    }

    private static boolean shouldUseVulkanPrimaryRoute(boolean dxvkRoute, boolean dgVoodooRoute) {
        return dxvkRoute || dgVoodooRoute;
    }

    private static boolean shouldUseVortekPrimaryRoute(String routingMode) {
        return !VortekConfigDialog.ROUTING_OPENGL_FIRST.equals(routingMode);
    }

    private void applyGraphicsRouteDefaults(boolean vulkanPrimaryRoute, String socClass) {
        envVars.put("AERO_GRAPHICS_STACK_PROFILE", "vulkan-first-with-gl-fallback");
        envVars.put("AERO_GRAPHICS_SOC_CLASS", socClass);
        envVars.put("AERO_GRAPHICS_VULKAN_PROVIDER", "turnip-vulkan");
        envVars.put("AERO_GRAPHICS_OPENGL_PROVIDER", "freedreno-opengl");
        envVars.put("AERO_GL_FALLBACK_ENGINE", "wined3d");
        envVars.put("AERO_DXVK_LEGACY_DX89_PATH", "wined3d");
        envVars.put("AERO_DXVK_GL_FALLBACK", "1");
        envVars.put("AERO_VKD3D_GL_FALLBACK", "1");

        if (vulkanPrimaryRoute) {
            envVars.put("AERO_GRAPHICS_ACTIVE_ROUTE", "turnip-primary");
            envVars.put("AERO_DXVK_ROUTE_MODE", "turnip-first");
            envVars.put("AERO_VKD3D_ROUTE_MODE", "turnip-first");
            envVars.put("GALLIUM_DRIVER", "zink");
        } else {
            envVars.put("AERO_GRAPHICS_ACTIVE_ROUTE", "zink-primary");
            envVars.put("AERO_DXVK_ROUTE_MODE", "zink-first");
            envVars.put("AERO_VKD3D_ROUTE_MODE", "zink-first");
            envVars.put("GALLIUM_DRIVER", "zink");
        }
    }

    private void applyGraphicsDriverPackages(String selectedDriverId, boolean vulkanPrimaryRoute) {
        AdrenotoolsManager adrenotoolsManager = new AdrenotoolsManager(this);
        AdrenotoolsManager.DriverPackageInfo selectedInfo = adrenotoolsManager.getDriverPackageInfo(selectedDriverId);
        boolean systemSelection = selectedInfo != null && selectedInfo.isSystemSelection();
        AdrenotoolsManager.DriverPackageInfo turnipInfo = null;
        AdrenotoolsManager.DriverPackageInfo openGlInfo = null;
        AdrenotoolsManager.DriverPackageInfo activeInfo = null;
        AdrenotoolsManager.DriverPackageInfo companionInfo = null;

        if (!systemSelection) {
            turnipInfo = adrenotoolsManager.resolvePreferredDriverForLane("turnip-vulkan", selectedInfo);
            openGlInfo = adrenotoolsManager.resolvePreferredDriverForLane("freedreno-opengl", selectedInfo);
            if (turnipInfo != null && openGlInfo == null && !safeTrim(turnipInfo.companionProviderLane).isEmpty()) {
                openGlInfo = ensureGraphicsProviderLaneInstalled(adrenotoolsManager, turnipInfo.companionProviderLane, turnipInfo);
            }
            if (openGlInfo != null && turnipInfo == null && !safeTrim(openGlInfo.companionProviderLane).isEmpty()) {
                turnipInfo = ensureGraphicsProviderLaneInstalled(adrenotoolsManager, openGlInfo.companionProviderLane, openGlInfo);
            }
            if (vulkanPrimaryRoute) {
                activeInfo = turnipInfo != null ? turnipInfo : openGlInfo;
                companionInfo = turnipInfo != null ? openGlInfo : null;
            } else {
                activeInfo = openGlInfo != null ? openGlInfo : turnipInfo;
                companionInfo = openGlInfo != null ? turnipInfo : null;
            }
        }

        if (activeInfo != null && !activeInfo.isSystemSelection() && !activeInfo.isOpenGlProvider()) {
            adrenotoolsManager.setDriverByInfo(envVars, imageFs, activeInfo);
            if (!activeInfo.preferredGalliumDriver.isEmpty()) {
                envVars.put("GALLIUM_DRIVER", activeInfo.preferredGalliumDriver);
            }
        } else if (activeInfo != null && activeInfo.isOpenGlProvider()
                && !activeInfo.preferredGalliumDriver.isEmpty()) {
            // OpenGL provider is delivered as an overlay package; keep Vulkan ICD routing
            // separate and only expose the Gallium selection for the active GL path.
            envVars.put("GALLIUM_DRIVER", activeInfo.preferredGalliumDriver);
        }

        adrenotoolsManager.restoreManagedOverlay(imageFs);
        boolean openGlOverlayApplied = openGlInfo != null && adrenotoolsManager.applyManagedOverlay(imageFs, openGlInfo);
        boolean vulkanPrimaryProviderMissing = vulkanPrimaryRoute && turnipInfo == null && openGlInfo != null;
        String requiredCompanionLane = activeInfo == null ? "" : safeTrim(activeInfo.companionProviderLane);
        boolean companionMissing = !requiredCompanionLane.isEmpty() && companionInfo == null;
        boolean legacyRouteDegraded = "route-degraded".equals(safeTrim(legacyGraphicsPolicy));
        String routeDegradedReason = joinNonEmptyCsv(
                legacyRouteDegraded ? "legacy_external_renderer_route" : "",
                vulkanPrimaryProviderMissing ? "vulkan_primary_provider_missing" : "",
                companionMissing ? "missing_companion_provider" : ""
        );
        AdrenotoolsManager.DriverPackageInfo effectiveActiveInfo = activeInfo != null ? activeInfo : selectedInfo;

        setOrClearEnv("AERO_GRAPHICS_SELECTED_DRIVER_ENTRY", selectedDriverId);
        setOrClearEnv("AERO_GRAPHICS_LEGACY_REQUESTED_DRIVER", safeTrim(legacyGraphicsRequestedDriver));
        setOrClearEnv("AERO_GRAPHICS_LEGACY_HINT", safeTrim(legacyGraphicsProviderHint));
        setOrClearEnv("AERO_GRAPHICS_LEGACY_POLICY", safeTrim(legacyGraphicsPolicy));
        setOrClearEnv("AERO_GRAPHICS_SELECTED_DRIVER_PACKAGE", selectedInfo == null ? "" : selectedInfo.name);
        setOrClearEnv("AERO_GRAPHICS_SELECTED_DRIVER_LANE", selectedInfo == null ? "" : selectedInfo.providerLane);
        setOrClearEnv("AERO_GRAPHICS_ACTIVE_PROVIDER_LANE", effectiveActiveInfo == null ? "" : effectiveActiveInfo.providerLane);
        setOrClearEnv("AERO_GRAPHICS_ACTIVE_PROVIDER_PACKAGE", effectiveActiveInfo == null ? "" : effectiveActiveInfo.name);
        setOrClearEnv("AERO_GRAPHICS_ACTIVE_PROVIDER_VERSION", effectiveActiveInfo == null ? "" : effectiveActiveInfo.driverVersion);
        setOrClearEnv("AERO_GRAPHICS_COMPANION_PROVIDER_LANE", companionInfo == null ? "" : companionInfo.providerLane);
        setOrClearEnv("AERO_GRAPHICS_COMPANION_PROVIDER_PACKAGE", companionInfo == null ? "" : companionInfo.name);
        setOrClearEnv("AERO_GRAPHICS_COMPANION_PROVIDER_VERSION", companionInfo == null ? "" : companionInfo.driverVersion);
        setOrClearEnv("AERO_OPENGL_OVERLAY_ACTIVE", openGlOverlayApplied ? "1" : "0");
        setOrClearEnv("AERO_OPENGL_OVERLAY_PACKAGE", openGlInfo == null ? "" : openGlInfo.name);
        setOrClearEnv("AERO_OPENGL_OVERLAY_ENTRY", openGlInfo == null ? "" : openGlInfo.entryId);
        setOrClearEnv("AERO_OPENGL_OVERLAY_VERSION", openGlInfo == null ? "" : openGlInfo.driverVersion);
        setOrClearEnv("AERO_TURNIP_PACKAGE", turnipInfo == null ? "" : turnipInfo.name);
        setOrClearEnv("AERO_TURNIP_VERSION", turnipInfo == null ? "" : turnipInfo.driverVersion);
        setOrClearEnv("AERO_TURNIP_SOURCE_REPO", turnipInfo == null ? "" : turnipInfo.sourceRepo);
        setOrClearEnv("AERO_TURNIP_RELEASE_TAG", turnipInfo == null ? "" : turnipInfo.releaseTag);
        setOrClearEnv("AERO_TURNIP_GALLIUM_BRIDGE", turnipInfo == null ? "" : turnipInfo.preferredGalliumDriver);
        setOrClearEnv("AERO_TURNIP_API_FOCUS", turnipInfo == null ? "" : joinCsv(turnipInfo.apiFocus));
        setOrClearEnv("AERO_TURNIP_FORENSIC_LOG_PREFIXES", turnipInfo == null ? "" : joinCsv(turnipInfo.forensicLogPrefixes));
        setOrClearEnv("AERO_OPENGL_PACKAGE", openGlInfo == null ? "" : openGlInfo.name);
        setOrClearEnv("AERO_OPENGL_VERSION", openGlInfo == null ? "" : openGlInfo.driverVersion);
        setOrClearEnv("AERO_OPENGL_SOURCE_REPO", openGlInfo == null ? "" : openGlInfo.sourceRepo);
        setOrClearEnv("AERO_OPENGL_RELEASE_TAG", openGlInfo == null ? "" : openGlInfo.releaseTag);
        setOrClearEnv("AERO_OPENGL_GALLIUM_DRIVER", openGlInfo == null ? "" : openGlInfo.preferredGalliumDriver);
        setOrClearEnv("AERO_OPENGL_API_FOCUS", openGlInfo == null ? "" : joinCsv(openGlInfo.apiFocus));
        setOrClearEnv("AERO_OPENGL_FORENSIC_LOG_PREFIXES", openGlInfo == null ? "" : joinCsv(openGlInfo.forensicLogPrefixes));
        setOrClearEnv("AERO_GRAPHICS_COMPANION_REQUIRED_LANE", requiredCompanionLane);
        setOrClearEnv("AERO_GRAPHICS_COMPANION_READY", requiredCompanionLane.isEmpty() ? "" : (companionMissing ? "0" : "1"));
        setOrClearEnv("AERO_GRAPHICS_ROUTE_DEGRADED", routeDegradedReason.isEmpty() ? "" : "1");
        setOrClearEnv("AERO_GRAPHICS_ROUTE_DEGRADED_REASON", routeDegradedReason);

        if (legacyRouteDegraded || companionMissing) {
            ForensicLogger.logEvent(
                    this,
                    "warn",
                    "GRAPHICS_PROVIDER_CONTRACT_DEGRADED",
                    null,
                    "graphics_provider",
                    "graphics_provider_contract_degraded",
                    ForensicLogger.fields(
                            "selected_driver_id", selectedDriverId,
                            "legacy_requested_driver", legacyGraphicsRequestedDriver,
                            "legacy_policy", legacyGraphicsPolicy,
                            "active_provider_lane", effectiveActiveInfo == null ? "" : effectiveActiveInfo.providerLane,
                            "required_companion_lane", requiredCompanionLane,
                            "selected_provider_lane", selectedInfo == null ? "" : selectedInfo.providerLane,
                            "degrade_reason", routeDegradedReason
                    )
            );
        }

        ForensicLogger.logEvent(
                this,
                "info",
                "GRAPHICS_PROVIDER_CONTRACT_APPLIED",
                null,
                "graphics_provider",
                "graphics_provider_contract_applied",
                ForensicLogger.fields(
                        "selected_driver_id", selectedDriverId,
                        "legacy_requested_driver", legacyGraphicsRequestedDriver,
                        "legacy_policy", legacyGraphicsPolicy,
                        "selected_provider_lane", selectedInfo == null ? "" : selectedInfo.providerLane,
                        "active_provider_lane", effectiveActiveInfo == null ? "" : effectiveActiveInfo.providerLane,
                        "active_provider_package", effectiveActiveInfo == null ? "" : effectiveActiveInfo.name,
                        "active_provider_version", effectiveActiveInfo == null ? "" : effectiveActiveInfo.driverVersion,
                        "active_provider_route", effectiveActiveInfo == null ? "" : effectiveActiveInfo.driverRoute,
                        "companion_provider_lane", companionInfo == null ? "" : companionInfo.providerLane,
                        "companion_provider_package", companionInfo == null ? "" : companionInfo.name,
                        "opengl_overlay_active", openGlOverlayApplied ? "1" : "0",
                        "turnip_provider_package", turnipInfo == null ? "" : turnipInfo.name,
                        "opengl_provider_package", openGlInfo == null ? "" : openGlInfo.name
                )
        );
    }

    private void applyRuntimeWrapperEnvFromProfile(@Nullable ContentProfile profile) {
        if (profile == null) return;
        envVars.put("AERO_RUNTIME_WRAPPER_ENV_SOURCE", ContentsManager.getEntryName(profile));
        File envFile = new File(ContentsManager.getInstallDir(this, profile), "ae-runtime-wrapper.env");
        if (!envFile.isFile()) return;

        String content = FileUtils.readString(envFile);
        if (content == null || content.isEmpty()) return;

        String[] lines = content.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            int idx = trimmed.indexOf('=');
            if (idx <= 0 || idx >= trimmed.length() - 1) continue;
            String key = trimmed.substring(0, idx).trim();
            String value = trimmed.substring(idx + 1).trim();
            if (!key.isEmpty() && !value.isEmpty()) envVars.put(key, value);
        }
    }

    private void applyRuntimeContractFromProfile(@Nullable ContentProfile profile, String socClass) {
        if (profile == null) return;
        envVars.put("AERO_RUNTIME_WRAPPER_PACKAGE", ContentsManager.getEntryName(profile));
        envVars.put("AERO_RUNTIME_WRAPPER_VERSION", profile.verName == null ? "" : profile.verName);
        File contractFile = new File(ContentsManager.getInstallDir(this, profile), "ae-runtime-contract.json");
        if (!contractFile.isFile()) return;

        try {
            JSONObject runtimeContract = new JSONObject(FileUtils.readString(contractFile));
            String lane = runtimeContract.optString("lane", "").trim();
            if (!lane.isEmpty()) envVars.put("AERO_GRAPHICS_WRAPPER_LANE", lane);

            String runtimeRoute = runtimeContract.optString("runtimeRoute", "").trim();
            if (!runtimeRoute.isEmpty()) envVars.put("AERO_RUNTIME_ROUTE", runtimeRoute);
            String compatLayer = runtimeContract.optString("compatLayer", "").trim();
            if (!compatLayer.isEmpty()) envVars.put("AERO_RUNTIME_COMPAT_LAYER", compatLayer);

            JSONObject wrapperContract = runtimeContract.optJSONObject("wrapperContract");
            if (wrapperContract == null) return;

            String selectedProfile = wrapperContract.optString("defaultProfile", "balanced").trim();
            JSONObject socClassProfiles = wrapperContract.optJSONObject("socClassProfiles");
            if (socClassProfiles != null) {
                String socMappedProfile = socClassProfiles.optString(socClass, "").trim();
                if (!socMappedProfile.isEmpty()) selectedProfile = socMappedProfile;
            }

            JSONObject profileEnv = wrapperContract.optJSONObject("profileEnv");
            if (profileEnv != null) {
                JSONObject selectedProfileEnv = profileEnv.optJSONObject(selectedProfile);
                if (selectedProfileEnv != null) {
                    Iterator<String> keys = selectedProfileEnv.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        String value = selectedProfileEnv.optString(key, "");
                        if (!value.isEmpty()) envVars.put(key, value);
                    }
                }
            }

            JSONObject routeHints = wrapperContract.optJSONObject("routeHints");
            if (routeHints != null) {
                String primaryProvider = routeHints.optString("primaryProvider", "").trim();
                String fallbackProvider = routeHints.optString("fallbackProvider", "").trim();
                String legacyEngine = routeHints.optString("legacyFallbackEngine", "").trim();
                if (!primaryProvider.isEmpty()) envVars.put("AERO_GRAPHICS_PRIMARY_PROVIDER", primaryProvider);
                if (!fallbackProvider.isEmpty()) envVars.put("AERO_GRAPHICS_FALLBACK_PROVIDER", fallbackProvider);
                if (!legacyEngine.isEmpty()) envVars.put("AERO_GL_FALLBACK_ENGINE", legacyEngine);
                String passthroughTool = routeHints.optString("passthroughTool", "").trim();
                if (!passthroughTool.isEmpty()) envVars.put("AERO_RUNTIME_PASSTHROUGH_TOOL", passthroughTool);
            }

            if (wrapperContract.has("noProton")) {
                envVars.put("AERO_RUNTIME_NO_PROTON", wrapperContract.optBoolean("noProton", false) ? "1" : "0");
            }
            String wrapperCompatLayer = wrapperContract.optString("compatLayer", "").trim();
            if (!wrapperCompatLayer.isEmpty()) {
                envVars.put("AERO_RUNTIME_COMPAT_LAYER", wrapperCompatLayer);
            }

            envVars.put("AERO_GRAPHICS_WRAPPER_PROFILE", selectedProfile);
            envVars.put("AERO_GRAPHICS_WRAPPER_SOC_CLASS", socClass);

            ForensicLogger.logEvent(
                    this,
                    "info",
                    "RUNTIME_WRAPPER_CONTRACT_APPLIED",
                    null,
                    "runtime_contract",
                    ContentsManager.getEntryName(profile),
                    ForensicLogger.fields(
                            "wrapper_package", ContentsManager.getEntryName(profile),
                            "wrapper_version", profile.verName == null ? "" : profile.verName,
                            "wrapper_lane", lane,
                            "runtime_route", envVars.get("AERO_RUNTIME_ROUTE"),
                            "compat_layer", envVars.get("AERO_RUNTIME_COMPAT_LAYER"),
                            "wrapper_profile", selectedProfile,
                            "no_proton", envVars.get("AERO_RUNTIME_NO_PROTON"),
                            "passthrough_tool", envVars.get("AERO_RUNTIME_PASSTHROUGH_TOOL")
                    )
            );
        } catch (Exception e) {
            Log.w(TAG, "Unable to parse runtime contract for profile " + profile.verName, e);
        }
    }

    private void applyWrapperContractsForCurrentRoute(boolean dxvkRoute, String socClass) {
        if (!dxvkRoute) return;

        String dxvkVersion = dxwrapperConfig.get("version");
        if (!dxvkVersion.isEmpty()) {
            String dxvkEntry = "dxvk-" + dxvkVersion;
            ContentProfile dxvkProfile = contentsManager.getProfileByEntryName(dxvkEntry);
            applyRuntimeWrapperEnvFromProfile(dxvkProfile);
            applyRuntimeContractFromProfile(dxvkProfile, socClass);
        }

        String vkd3dVersion = dxwrapperConfig.get("vkd3dVersion");
        if (!vkd3dVersion.isEmpty() && !"None".equalsIgnoreCase(vkd3dVersion)) {
            String vkd3dEntry = "vkd3d-" + vkd3dVersion;
            ContentProfile vkd3dProfile = contentsManager.getProfileByEntryName(vkd3dEntry);
            applyRuntimeWrapperEnvFromProfile(vkd3dProfile);
            applyRuntimeContractFromProfile(vkd3dProfile, socClass);
        }
    }

    private void extractGraphicsDriverFiles() {
        String normalizedGraphicsDriver = GraphicsDrivers.normalize(graphicsDriver);
        String adrenoToolsDriverId = trimToEmpty(graphicsDriverConfig.get("version"));
        if (!GraphicsDrivers.usesKeyValueConfig(normalizedGraphicsDriver) && adrenoToolsDriverId.isEmpty()) {
            adrenoToolsDriverId = DefaultVersion.WRAPPER;
        }
        KeyValueSet driverKeyValueConfig = getGraphicsDriverKeyValueConfig();
        String forensicDriverId = GraphicsDrivers.isVortek(normalizedGraphicsDriver)
                ? firstNonEmpty(trimToEmpty(driverKeyValueConfig.get("vortekPackageVersion")), GraphicsDrivers.VORTEK)
                : adrenoToolsDriverId;

        Log.d("GraphicsDriverExtraction", "Graphics driver=" + normalizedGraphicsDriver + " driverId=" + forensicDriverId);

        File rootDir = imageFs.getRootDir();
        boolean dxvkRoute = dxwrapper.contains("dxvk");
        boolean dgVoodooRoute = dxwrapper.contains("dgvoodoo");
        boolean vulkanPrimaryRoute = shouldUseVulkanPrimaryRoute(dxvkRoute, dgVoodooRoute);
        String socClass = detectSoCClass();

        if (dxvkRoute) {
            DXVKConfigDialog.setEnvVars(this, dxwrapperConfig, envVars);
            String version = dxwrapperConfig.get("version");
            if (version.equals("1.11.1-sarek")) {
                Log.d("GraphicsDriverExtraction", "Disabling Wrapper PATCH_OPCONSTCOMP SPIR-V pass");
                envVars.put("WRAPPER_NO_PATCH_OPCONSTCOMP", "1");
            }
            envVars.put("AERO_DXWRAPPER_ACTIVE", "dxvk+vkd3d");
        } else if (dgVoodooRoute) {
            DgVoodooManager dgVoodooManager = new DgVoodooManager(this);
            KeyValueSet dgConfig = DgVoodooConfigDialog.parseConfig(dxwrapperConfig);
            DgVoodooConfigDialog.setEnvVars(this, dgConfig, envVars, dgVoodooManager);
            envVars.put("AERO_DXWRAPPER_ACTIVE", "dgvoodoo");
        } else {
            WineD3DConfigDialog.setEnvVars(this, dxwrapperConfig, envVars);
            envVars.put("AERO_DXWRAPPER_ACTIVE", "wined3d");
        }

        String dri3Mode = preferences.getString("dri3_mode", preferences.getBoolean("use_dri3", true) ? "auto" : "off");
        if (dri3Mode == null || dri3Mode.trim().isEmpty()) dri3Mode = "auto";
        boolean useDRI3 = !"off".equalsIgnoreCase(dri3Mode);
        boolean dri3PresentWait = preferences.getBoolean("dri3_present_wait", true);
        boolean dri3ForceSwWsi = preferences.getBoolean("dri3_force_sw_wsi", false);
        envVars.put("AERO_DRI3_MODE", dri3Mode);
        envVars.put("AERO_DRI3_ENABLED", useDRI3 ? "1" : "0");
        envVars.put("AERO_DRI3_PRESENT_WAIT", dri3PresentWait ? "1" : "0");
        envVars.put("AERO_DRI3_FORCE_SW_WSI", dri3ForceSwWsi ? "1" : "0");
        if (!useDRI3 || dri3ForceSwWsi) {
            envVars.put("MESA_VK_WSI_DEBUG", "sw");
        }

        if (firstTimeBoot) {
            Log.d("XServerDisplayActivity", "First time container boot, re-extracting libs");
            TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "graphics_driver/wrapper.tzst", rootDir);
            TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "layers.tzst", rootDir);
            TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "graphics_driver/extra_libs.tzst", rootDir);
            if (wineInfo.isArm64EC() && !GPUInformation.getRenderer(null, null).contains("Mali")) {
                TarCompressorUtils.extract(
                        TarCompressorUtils.Type.ZSTD,
                        this,
                        "graphics_driver/zink_dlls.tzst",
                        new File(WineUtils.resolveHostWineDriveCRoot(rootDir), "windows")
                );
            }
        }

        applyWrapperContractsForCurrentRoute(dxvkRoute, socClass);

        AdrenotoolsManager adrenotoolsManager = new AdrenotoolsManager(this);
        adrenotoolsManager.restoreManagedOverlay(imageFs);

        AdrenotoolsManager.DriverPackageInfo selectedWrapperInfo = null;
        AdrenotoolsManager.DriverPackageInfo turnipInfo = null;
        if (!GraphicsDrivers.isVortek(normalizedGraphicsDriver)
                && !GraphicsDrivers.isGladio(normalizedGraphicsDriver)
                && !GraphicsDrivers.isAeMaliGallium(normalizedGraphicsDriver)) {
            selectedWrapperInfo = adrenoToolsDriverId.isEmpty()
                    ? adrenotoolsManager.getDriverPackageInfo(DefaultVersion.WRAPPER)
                    : adrenotoolsManager.getDriverPackageInfo(adrenoToolsDriverId);
            if (selectedWrapperInfo != null) {
                turnipInfo = selectedWrapperInfo.isSystemSelection()
                        ? selectedWrapperInfo
                        : adrenotoolsManager.resolvePreferredDriverForLane("turnip-vulkan", selectedWrapperInfo);
            }
        }

        if (GraphicsDrivers.isWrapper(normalizedGraphicsDriver)) {
            File selectedWrapperIcd = resolveWrapperIcdFile();
            if (selectedWrapperIcd != null && selectedWrapperIcd.isFile()) {
                String wrapperIcdPath = selectedWrapperIcd.getAbsolutePath();
                envVars.put("VK_ICD_FILENAMES", wrapperIcdPath);
                envVars.put("VK_DRIVER_FILES", wrapperIcdPath);
            } else {
                envVars.remove("VK_ICD_FILENAMES");
                envVars.remove("VK_DRIVER_FILES");
                envVars.put("AERO_VULKAN_WRAPPER_DEGRADED", "android_host_dependency_closure_unsatisfied");
            }
            applyGraphicsRouteDefaults(vulkanPrimaryRoute, socClass);
            applyGraphicsDriverPackages(adrenoToolsDriverId, vulkanPrimaryRoute);
            applyWrapperGraphicsConfigEnv(rootDir, adrenoToolsDriverId, useDRI3, dri3ForceSwWsi, dri3PresentWait);
        } else if (GraphicsDrivers.isMesaOpenGlBridge(normalizedGraphicsDriver) || GraphicsDrivers.isVirgl(normalizedGraphicsDriver)) {
            boolean virglRoute = GraphicsDrivers.isVirgl(normalizedGraphicsDriver);
            boolean aeMaliGalliumRoute = GraphicsDrivers.isAeMaliGallium(normalizedGraphicsDriver);
            boolean openGlRouteNeedsVulkanCompanion = (dxvkRoute || dgVoodooRoute) && (virglRoute || aeMaliGalliumRoute);
            boolean openGlRouteWithoutVulkanCompanion = (virglRoute || aeMaliGalliumRoute) && !openGlRouteNeedsVulkanCompanion;
            boolean renderNodeAvailable = hasAccessibleRenderNode();
            boolean maliSoC = isMaliSocClass(socClass);
            String requestedBridgeVulkanApi = normalizeRequestedVulkanApi(graphicsDriverConfig.get("vulkanVersion"));
            AdrenotoolsManager.DriverPackageInfo companionTurnipInfo = openGlRouteWithoutVulkanCompanion
                    ? null
                    : resolveOpenGlBridgeTurnipInfo(adrenotoolsManager, adrenoToolsDriverId);
            VortekVulkanDriverPackageManager.PackageInfo companionAeMaliPanvkInfo = null;
            String openGlBridgeVulkanDegradedReason = "";
            File selectedWrapperIcd = resolveWrapperIcdFile();
            int wrapperIcdApiMinor = resolveWrapperIcdApiMinor(selectedWrapperIcd);
            if (!openGlRouteWithoutVulkanCompanion) {
                if (selectedWrapperIcd != null && selectedWrapperIcd.isFile()) {
                    String wrapperIcdPath = selectedWrapperIcd.getAbsolutePath();
                    envVars.put("VK_ICD_FILENAMES", wrapperIcdPath);
                    envVars.put("VK_DRIVER_FILES", wrapperIcdPath);
                } else {
                    envVars.remove("VK_ICD_FILENAMES");
                    envVars.remove("VK_DRIVER_FILES");
                    envVars.put("AERO_VULKAN_WRAPPER_DEGRADED", "android_host_dependency_closure_unsatisfied");
                }
            }

            if (!openGlRouteWithoutVulkanCompanion && !maliSoC && companionTurnipInfo != null) {
                adrenotoolsManager.setDriverByInfo(envVars, imageFs, companionTurnipInfo);
            }

            String vulkanProviderLane = "";
            String vulkanProviderPackage = "";
            String vulkanProviderVersion = "";
            if (!openGlRouteWithoutVulkanCompanion && maliSoC) {
                VortekWrapperState bridgeVortekState = resolveVortekWrapperState("");
                ensureGraphicsDriverAssetExtracted(
                        rootDir,
                        bridgeVortekState.bundledAsset.assetPath,
                        bridgeVortekState.bundledAsset.extractProbePath
                );
                VortekVulkanDriverPackageManager packageManager = new VortekVulkanDriverPackageManager(this);
                String aeMaliPanvkEntry = resolveBundledAeMaliPanvkEntry();
                if (aeMaliPanvkEntry.isEmpty()) {
                    openGlBridgeVulkanDegradedReason = "aemali_panvk_bundle_missing";
                } else {
                    companionAeMaliPanvkInfo = packageManager.getPackageInfo(aeMaliPanvkEntry);
                    String rootLibraryPath = packageManager.resolveRootLibraryPath(aeMaliPanvkEntry);
                    boolean overlayReady = companionAeMaliPanvkInfo != null
                            && !trimToEmpty(rootLibraryPath).isEmpty()
                            && packageManager.deployPackageToRoot(rootDir, aeMaliPanvkEntry)
                            && new File(rootDir, rootLibraryPath).isFile();
                    File aeMaliIcdFile = overlayReady
                            ? rewriteVortekIcdFile(rootDir, rootLibraryPath, requestedBridgeVulkanApi)
                            : null;
                    if (aeMaliIcdFile == null) {
                        openGlBridgeVulkanDegradedReason = overlayReady
                                ? "aemali_panvk_icd_rewrite_failed"
                                : "aemali_panvk_overlay_deploy_failed";
                    } else {
                        String aeMaliVersion = firstNonEmpty(
                                trimToEmpty(companionAeMaliPanvkInfo.version),
                                GraphicsDrivers.getDisplayVersion(this, GraphicsDrivers.AEMALI_PANVK, "")
                        );
                        envVars.put("VK_ICD_FILENAMES", aeMaliIcdFile.getAbsolutePath());
                        envVars.put("VK_DRIVER_FILES", aeMaliIcdFile.getAbsolutePath());
                        setOrClearEnv("AERO_VULKAN_RUNTIME_SOURCE", buildVortekRuntimeSource(companionAeMaliPanvkInfo));
                        setOrClearEnv("AERO_VULKAN_WRAPPER_ICD", aeMaliIcdFile.getAbsolutePath());
                        setOrClearEnv("AERO_VULKAN_WRAPPER_API_MAX", requestedBridgeVulkanApi);
                        setOrClearEnv("AERO_VULKAN_API_MIN_AVAILABLE", "");
                        setOrClearEnv("AERO_VULKAN_API_MAX_AVAILABLE", requestedBridgeVulkanApi);
                        setOrClearEnv("AERO_VULKAN_VALIDATION_LAYER_MANIFEST", "");
                        setOrClearEnv("AERO_VULKAN_API_SELECTED", requestedBridgeVulkanApi);
                        setOrClearEnv("WRAPPER_VK_VERSION", "");
                        applyAeMaliPolicyEnv(
                                companionAeMaliPanvkInfo,
                                VortekExtensionPolicy.PROFILE_MALI_SYSTEM,
                                requestedBridgeVulkanApi,
                                renderNodeAvailable
                        );
                        vulkanProviderLane = firstNonEmpty(trimToEmpty(companionAeMaliPanvkInfo.providerLane), "aemali-panvk");
                        vulkanProviderPackage = companionAeMaliPanvkInfo.getDisplayLabel();
                        vulkanProviderVersion = aeMaliVersion;
                    }
                }
            } else if (!openGlRouteWithoutVulkanCompanion && companionTurnipInfo != null) {
                setOrClearEnv("AERO_VULKAN_RUNTIME_SOURCE", selectedWrapperIcd != null && selectedWrapperIcd.isFile() ? "wrapper-embedded" : "wrapper-missing");
                setOrClearEnv("AERO_VULKAN_WRAPPER_ICD", selectedWrapperIcd != null && selectedWrapperIcd.isFile() ? selectedWrapperIcd.getAbsolutePath() : "");
                setOrClearEnv("AERO_VULKAN_WRAPPER_API_MAX", wrapperIcdApiMinor > 0 ? "1." + wrapperIcdApiMinor : "");
                setOrClearEnv("AERO_VULKAN_API_MIN_AVAILABLE", "");
                setOrClearEnv("AERO_VULKAN_API_MAX_AVAILABLE", wrapperIcdApiMinor > 0 ? "1." + wrapperIcdApiMinor : "");
                setOrClearEnv("AERO_VULKAN_VALIDATION_LAYER_MANIFEST", "");
                setOrClearEnv("AERO_VULKAN_API_SELECTED", requestedBridgeVulkanApi);
                setOrClearEnv("WRAPPER_VK_VERSION", requestedBridgeVulkanApi + "." + resolveDriverVulkanPatch(adrenoToolsDriverId));
                vulkanProviderLane = firstNonEmpty(trimToEmpty(companionTurnipInfo.providerLane), "turnip-vulkan");
                vulkanProviderPackage = firstNonEmpty(trimToEmpty(companionTurnipInfo.name), trimToEmpty(companionTurnipInfo.entryId));
                vulkanProviderVersion = trimToEmpty(companionTurnipInfo.driverVersion);
            }

            String requestedPackageVersion = driverKeyValueConfig.get("packageVersion");
            VirGLDriverPackageManager virglPackageManager = virglRoute ? new VirGLDriverPackageManager(this) : null;
            MesaOpenGLDriverPackageManager mesaOpenGlPackageManager = aeMaliGalliumRoute
                    ? new MesaOpenGLDriverPackageManager(this, normalizedGraphicsDriver)
                    : null;
            boolean virglCustomPackage = virglRoute && VirGLDriverPackageManager.isCustomPackageEntry(requestedPackageVersion);
            boolean aeMaliCustomPackage = aeMaliGalliumRoute && MesaOpenGLDriverPackageManager.isCustomPackageEntry(requestedPackageVersion);
            boolean virglPackageDegraded = false;
            String virglPackageDegradedReason = "";
            boolean aeMaliPackageDegraded = false;
            String aeMaliPackageDegradedReason = "";
            GraphicsDrivers.BundledDriverAsset openGlAsset = virglCustomPackage || aeMaliCustomPackage
                    ? null
                    : GraphicsDrivers.getBundledDriverAsset(
                            this,
                            normalizedGraphicsDriver,
                            requestedPackageVersion
                    );
            VirGLDriverPackageManager.PackageInfo virglPackageInfo = virglRoute && virglPackageManager != null
                    ? virglPackageManager.getPackageInfo(virglCustomPackage
                            ? requestedPackageVersion
                            : (openGlAsset == null ? requestedPackageVersion : openGlAsset.version))
                    : null;
            MesaOpenGLDriverPackageManager.PackageInfo mesaPackageInfo = aeMaliGalliumRoute && mesaOpenGlPackageManager != null
                    ? mesaOpenGlPackageManager.getPackageInfo(aeMaliCustomPackage
                            ? requestedPackageVersion
                            : (openGlAsset == null ? requestedPackageVersion : openGlAsset.version))
                    : null;
            String packagePreferredGalliumDriver = aeMaliGalliumRoute && mesaPackageInfo != null
                    ? firstNonEmpty(
                            trimToEmpty(mesaPackageInfo.preferredGalliumDriver),
                            trimToEmpty(mesaPackageInfo.fallbackGalliumDriver)
                    )
                    : "";
            String defaultGalliumDriver = virglRoute
                    ? GraphicsDrivers.getVirglGalliumDriver()
                    : aeMaliGalliumRoute
                    ? firstNonEmpty(packagePreferredGalliumDriver, GraphicsDrivers.getMesaGalliumDriver(normalizedGraphicsDriver))
                    : GraphicsDrivers.getMesaGalliumDriver(normalizedGraphicsDriver);
            String galliumDriver = virglRoute
                    ? VirGLConfigDialog.normalizeGalliumDriver(driverKeyValueConfig.get("galliumDriver", defaultGalliumDriver))
                    : aeMaliGalliumRoute
                    ? normalizeMaliGalliumDriver(driverKeyValueConfig.get("galliumDriver", defaultGalliumDriver), defaultGalliumDriver)
                    : normalizeAdrenoGalliumDriver(driverKeyValueConfig.get("galliumDriver", defaultGalliumDriver), defaultGalliumDriver);
            String openGlLane = normalizedGraphicsDriver;
            String openGlPackage = virglPackageInfo != null
                    ? virglPackageInfo.getDisplayLabel()
                    : mesaPackageInfo != null
                    ? mesaPackageInfo.getDisplayLabel()
                    : openGlAsset == null ? GraphicsDrivers.getDisplayLabel(normalizedGraphicsDriver) : openGlAsset.packageLabel;
            String openGlVersion = virglPackageInfo != null && !trimToEmpty(virglPackageInfo.version).isEmpty()
                    ? virglPackageInfo.version
                    : mesaPackageInfo != null && !trimToEmpty(mesaPackageInfo.version).isEmpty()
                    ? mesaPackageInfo.version
                    : GraphicsDrivers.getDisplayVersion(this, normalizedGraphicsDriver, rawGraphicsDriverConfig);
            String openGlActiveEntry = (virglCustomPackage && !virglPackageDegraded)
                    || (aeMaliCustomPackage && !aeMaliPackageDegraded)
                    ? requestedPackageVersion
                    : openGlAsset == null ? requestedPackageVersion : openGlAsset.version;
            String openGlSourceRepo = virglPackageInfo == null
                    ? mesaPackageInfo == null
                    ? "https://gitlab.freedesktop.org/mesa/mesa"
                    : firstNonEmpty(trimToEmpty(mesaPackageInfo.sourceRepo), "https://gitlab.freedesktop.org/mesa/mesa")
                    : firstNonEmpty(trimToEmpty(virglPackageInfo.sourceRepo), "https://gitlab.freedesktop.org/mesa/mesa");
            boolean aeMaliOpenGlRequiresRenderNode = mesaPackageInfo != null && mesaPackageInfo.requiresRenderNode;
            boolean aeMaliOpenGlDegraded = aeMaliGalliumRoute && aeMaliOpenGlRequiresRenderNode && !renderNodeAvailable;
            String aeMaliOpenGlDegradedReason = aeMaliOpenGlDegraded ? "aemali_gallium_requires_render_node" : "";
            String virglProviderLane = virglPackageInfo == null || virglPackageDegraded
                    ? "virgl-universal"
                    : firstNonEmpty(trimToEmpty(virglPackageInfo.providerLane), "virgl-universal");
            String virglDriverKind = virglPackageInfo == null || virglPackageDegraded
                    ? "virgl-mesa-bridge"
                    : firstNonEmpty(trimToEmpty(virglPackageInfo.driverKind), "virgl-mesa-bridge");
            String virglTransport = virglPackageInfo == null || virglPackageDegraded
                    ? "userspace-virtio-gpu"
                    : firstNonEmpty(trimToEmpty(virglPackageInfo.transport), "userspace-virtio-gpu");
            String virglSupportClass = virglPackageInfo == null || virglPackageDegraded
                    ? "separate-transport"
                    : trimToEmpty(virglPackageInfo.supportClass);
            String virglKernelEvidenceClass = virglPackageInfo == null || virglPackageDegraded
                    ? ""
                    : trimToEmpty(virglPackageInfo.kernelEvidenceClass);
            String virglTransportRequirements = virglPackageInfo == null || virglPackageDegraded
                    ? "mesa-virpipe-gallium,companion-virglrenderer-host,virtual-gpu-host-surface"
                    : trimToEmpty(virglPackageInfo.transportRequirements);
            String virglOwnerLane = virglPackageInfo == null || virglPackageDegraded
                    ? ""
                    : trimToEmpty(virglPackageInfo.ownerLane);
            String virglRouteId = virglPackageInfo == null || virglPackageDegraded
                    ? "virgl-universal-virtual-gpu"
                    : firstNonEmpty(trimToEmpty(virglPackageInfo.routeId), "virgl-universal-virtual-gpu");
            String virglRankedKernelDonors = virglPackageInfo == null || virglPackageDegraded
                    ? ""
                    : trimToEmpty(virglPackageInfo.rankedKernelDonors);
            String virglDiagnosticKeys = virglPackageInfo == null || virglPackageDegraded
                    ? ""
                    : trimToEmpty(virglPackageInfo.diagnosticKeys);
            boolean virglRequiresRenderNode = virglPackageInfo != null && !virglPackageDegraded && virglPackageInfo.requiresRenderNode;

            setOrClearEnv("AERO_GRAPHICS_STACK_PROFILE", virglRoute ? "universal-virgl" : aeMaliGalliumRoute ? "aemali-universal" : "builtin-opengl-companion");
            setOrClearEnv("AERO_GRAPHICS_SOC_CLASS", socClass);
            setOrClearEnv("AERO_GRAPHICS_SELECTED_DRIVER_ENTRY", normalizedGraphicsDriver);
            setOrClearEnv("AERO_GRAPHICS_SELECTED_DRIVER_LANE", normalizedGraphicsDriver);
            setOrClearEnv("AERO_GRAPHICS_VULKAN_PROVIDER", vulkanProviderLane);
            setOrClearEnv("AERO_GRAPHICS_OPENGL_PROVIDER", openGlLane);
            setOrClearEnv("AERO_GL_FALLBACK_ENGINE", "wined3d");
            setOrClearEnv("AERO_DXVK_LEGACY_DX89_PATH", "wined3d");
            setOrClearEnv("AERO_DXVK_GL_FALLBACK", "1");
            setOrClearEnv("AERO_VKD3D_GL_FALLBACK", "1");
            setOrClearEnv("AERO_TURNIP_PACKAGE", maliSoC ? "" : vulkanProviderPackage);
            setOrClearEnv("AERO_TURNIP_VERSION", maliSoC ? "" : vulkanProviderVersion);
            setOrClearEnv("AERO_TURNIP_SOURCE_REPO", companionTurnipInfo == null || maliSoC ? "" : companionTurnipInfo.sourceRepo);
            setOrClearEnv("AERO_TURNIP_RELEASE_TAG", companionTurnipInfo == null || maliSoC ? "" : companionTurnipInfo.releaseTag);
            setOrClearEnv("AERO_TURNIP_GALLIUM_BRIDGE", companionTurnipInfo == null || maliSoC ? "" : companionTurnipInfo.preferredGalliumDriver);
            setOrClearEnv("AERO_TURNIP_API_FOCUS", companionTurnipInfo == null || maliSoC ? "" : joinCsv(companionTurnipInfo.apiFocus));
            setOrClearEnv("AERO_TURNIP_FORENSIC_LOG_PREFIXES", companionTurnipInfo == null || maliSoC ? "" : joinCsv(companionTurnipInfo.forensicLogPrefixes));

            if (virglCustomPackage) {
                boolean deployed = virglPackageManager != null && virglPackageManager.deployPackageToRoot(rootDir, requestedPackageVersion);
                if (!deployed) {
                    virglPackageDegraded = true;
                    virglPackageDegradedReason = "virgl_custom_package_deploy_failed";
                    openGlAsset = GraphicsDrivers.getBundledDriverAsset(this, normalizedGraphicsDriver);
                    virglPackageInfo = openGlAsset == null || virglPackageManager == null
                            ? null
                            : virglPackageManager.getPackageInfo(openGlAsset.version);
                    if (openGlAsset != null) {
                        openGlPackage = virglPackageInfo == null ? openGlAsset.packageLabel : virglPackageInfo.getDisplayLabel();
                        openGlVersion = virglPackageInfo != null && !trimToEmpty(virglPackageInfo.version).isEmpty()
                                ? trimToEmpty(virglPackageInfo.version)
                                : openGlAsset.version;
                        openGlSourceRepo = virglPackageInfo == null
                                ? "https://gitlab.freedesktop.org/mesa/mesa"
                                : trimToEmpty(virglPackageInfo.sourceRepo);
                        ensureGraphicsDriverAssetExtracted(rootDir, openGlAsset.assetPath, openGlAsset.extractProbePath);
                    }
                }
            } else if (openGlAsset != null) {
                ensureGraphicsDriverAssetExtracted(rootDir, openGlAsset.assetPath, openGlAsset.extractProbePath);
            }
            if (aeMaliCustomPackage) {
                boolean deployed = mesaOpenGlPackageManager != null
                        && mesaPackageInfo != null
                        && mesaOpenGlPackageManager.deployPackageToRoot(rootDir, requestedPackageVersion);
                if (!deployed) {
                    aeMaliPackageDegraded = true;
                    aeMaliPackageDegradedReason = mesaPackageInfo == null
                            ? "aemali_gallium_custom_package_missing"
                            : "aemali_gallium_custom_package_deploy_failed";
                    openGlAsset = GraphicsDrivers.getBundledDriverAsset(this, normalizedGraphicsDriver);
                    mesaPackageInfo = openGlAsset == null || mesaOpenGlPackageManager == null
                            ? null
                            : mesaOpenGlPackageManager.getPackageInfo(openGlAsset.version);
                    if (openGlAsset != null) {
                        openGlPackage = mesaPackageInfo == null ? openGlAsset.packageLabel : mesaPackageInfo.getDisplayLabel();
                        openGlVersion = mesaPackageInfo != null && !trimToEmpty(mesaPackageInfo.version).isEmpty()
                                ? trimToEmpty(mesaPackageInfo.version)
                                : openGlAsset.version;
                        openGlSourceRepo = mesaPackageInfo == null
                                ? "https://gitlab.freedesktop.org/mesa/mesa"
                                : trimToEmpty(mesaPackageInfo.sourceRepo);
                        ensureGraphicsDriverAssetExtracted(rootDir, openGlAsset.assetPath, openGlAsset.extractProbePath);
                    }
                }
            }
            if (aeMaliGalliumRoute) {
                String resolvedDefaultGalliumDriver = firstNonEmpty(
                        mesaPackageInfo == null ? "" : trimToEmpty(mesaPackageInfo.preferredGalliumDriver),
                        mesaPackageInfo == null ? "" : trimToEmpty(mesaPackageInfo.fallbackGalliumDriver),
                        GraphicsDrivers.getMesaGalliumDriver(normalizedGraphicsDriver)
                );
                galliumDriver = normalizeMaliGalliumDriver(
                        driverKeyValueConfig.get("galliumDriver", resolvedDefaultGalliumDriver),
                        resolvedDefaultGalliumDriver
                );
            }
            openGlActiveEntry = (virglCustomPackage && !virglPackageDegraded)
                    || (aeMaliCustomPackage && !aeMaliPackageDegraded)
                    ? requestedPackageVersion
                    : openGlAsset == null ? requestedPackageVersion : openGlAsset.version;
            virglProviderLane = virglPackageInfo == null || virglPackageDegraded
                    ? "virgl-universal"
                    : firstNonEmpty(trimToEmpty(virglPackageInfo.providerLane), "virgl-universal");
            virglDriverKind = virglPackageInfo == null || virglPackageDegraded
                    ? "virgl-mesa-bridge"
                    : firstNonEmpty(trimToEmpty(virglPackageInfo.driverKind), "virgl-mesa-bridge");
            virglTransport = virglPackageInfo == null || virglPackageDegraded
                    ? "userspace-virtio-gpu"
                    : firstNonEmpty(trimToEmpty(virglPackageInfo.transport), "userspace-virtio-gpu");
            virglSupportClass = virglPackageInfo == null || virglPackageDegraded
                    ? "separate-transport"
                    : trimToEmpty(virglPackageInfo.supportClass);
            virglKernelEvidenceClass = virglPackageInfo == null || virglPackageDegraded
                    ? ""
                    : trimToEmpty(virglPackageInfo.kernelEvidenceClass);
            virglTransportRequirements = virglPackageInfo == null || virglPackageDegraded
                    ? "mesa-virpipe-gallium,companion-virglrenderer-host,virtual-gpu-host-surface"
                    : trimToEmpty(virglPackageInfo.transportRequirements);
            virglOwnerLane = virglPackageInfo == null || virglPackageDegraded
                    ? ""
                    : trimToEmpty(virglPackageInfo.ownerLane);
            virglRouteId = virglPackageInfo == null || virglPackageDegraded
                    ? "virgl-universal-virtual-gpu"
                    : firstNonEmpty(trimToEmpty(virglPackageInfo.routeId), "virgl-universal-virtual-gpu");
            virglRankedKernelDonors = virglPackageInfo == null || virglPackageDegraded
                    ? ""
                    : trimToEmpty(virglPackageInfo.rankedKernelDonors);
            virglDiagnosticKeys = virglPackageInfo == null || virglPackageDegraded
                    ? ""
                    : trimToEmpty(virglPackageInfo.diagnosticKeys);
            virglRequiresRenderNode = virglPackageInfo != null && !virglPackageDegraded && virglPackageInfo.requiresRenderNode;
            setOrClearEnv("AERO_GRAPHICS_SELECTED_DRIVER_PACKAGE", openGlPackage);
            setOrClearEnv("AERO_OPENGL_PACKAGE", openGlPackage);
            setOrClearEnv("AERO_OPENGL_VERSION", openGlVersion);
            setOrClearEnv("AERO_OPENGL_SOURCE_REPO", virglRoute || aeMaliGalliumRoute ? openGlSourceRepo : "");
            setOrClearEnv("AERO_OPENGL_PACKAGE_ENTRY_REQUESTED", virglRoute || aeMaliGalliumRoute ? requestedPackageVersion : "");
            setOrClearEnv("AERO_OPENGL_PACKAGE_ENTRY", virglRoute || aeMaliGalliumRoute ? openGlActiveEntry : "");
            setOrClearEnv("AERO_OPENGL_CONTAINER_SOURCE", virglRoute
                    ? (virglCustomPackage && !virglPackageDegraded ? "custom-overlay" : "bundled-virgl")
                    : aeMaliGalliumRoute
                    ? (aeMaliCustomPackage && !aeMaliPackageDegraded ? "custom-aemali-gallium" : "bundled-aemali-gallium")
                    : "");
            if (virglRoute) {
                applyGraphicsDriverMetadataEnv(
                        "AERO_OPENGL",
                        virglProviderLane,
                        virglDriverKind,
                        virglTransport,
                        virglSupportClass,
                        virglKernelEvidenceClass,
                        virglTransportRequirements,
                        virglOwnerLane,
                        virglRouteId,
                        virglRankedKernelDonors,
                        virglDiagnosticKeys,
                        virglRequiresRenderNode
                );
            } else if (aeMaliGalliumRoute && mesaPackageInfo != null) {
                applyGraphicsDriverMetadataEnv(
                        "AERO_OPENGL",
                        mesaPackageInfo.providerLane,
                        mesaPackageInfo.driverKind,
                        mesaPackageInfo.transport,
                        mesaPackageInfo.supportClass,
                        mesaPackageInfo.kernelEvidenceClass,
                        mesaPackageInfo.transportRequirements,
                        mesaPackageInfo.ownerLane,
                        mesaPackageInfo.routeId,
                        mesaPackageInfo.rankedKernelDonors,
                        mesaPackageInfo.diagnosticKeys,
                        mesaPackageInfo.requiresRenderNode
                );
                applyAeMaliOpenGlPolicyEnv(
                        mesaPackageInfo.providerLane,
                        mesaPackageInfo.driverKind,
                        mesaPackageInfo.sourceRepo,
                        mesaPackageInfo.transport,
                        mesaPackageInfo.supportClass,
                        mesaPackageInfo.kernelEvidenceClass,
                        mesaPackageInfo.transportRequirements,
                        mesaPackageInfo.ownerLane,
                        mesaPackageInfo.routeId,
                        mesaPackageInfo.rankedKernelDonors,
                        mesaPackageInfo.diagnosticKeys,
                        mesaPackageInfo.requiresRenderNode,
                        renderNodeAvailable,
                        galliumDriver,
                        mesaPackageInfo.graphicsStackProfile
                );
            }
            if (!galliumDriver.isEmpty()) envVars.put("GALLIUM_DRIVER", galliumDriver);
            if (virglRoute) {
                envVars.put("VIRGL_SERVER_PATH", rootDir.getAbsolutePath() + UnixSocketConfig.VIRGL_SERVER_PATH);
                setOrClearEnv("AERO_VIRGL_PACKAGE", openGlPackage);
                setOrClearEnv("AERO_VIRGL_PACKAGE_VERSION", openGlVersion);
                setOrClearEnv("AERO_VIRGL_PACKAGE_SOURCE", virglCustomPackage && !virglPackageDegraded ? "custom" : "bundled");
                setOrClearEnv("AERO_VIRGL_PACKAGE_ENTRY_REQUESTED", requestedPackageVersion);
                setOrClearEnv("AERO_VIRGL_PACKAGE_ENTRY", openGlActiveEntry);
                setOrClearEnv("AERO_VIRGL_ROUTE_DEGRADED_REASON", virglPackageDegradedReason);
                setOrClearEnv("AERO_VIRGL_GALLIUM_DRIVER", galliumDriver);
                applyGraphicsDriverMetadataEnv(
                        "AERO_VIRGL",
                        virglProviderLane,
                        virglDriverKind,
                        virglTransport,
                        virglSupportClass,
                        virglKernelEvidenceClass,
                        virglTransportRequirements,
                        virglOwnerLane,
                        virglRouteId,
                        virglRankedKernelDonors,
                        virglDiagnosticKeys,
                        virglRequiresRenderNode
                );
                setOrClearEnv("AERO_VIRGL_RENDERER_SOURCE_REPO", "https://gitlab.freedesktop.org/virgl/virglrenderer");
                setOrClearEnv("AERO_VIRGL_RENDERER_SOURCE_REV", "ca50e008863837e094747a69974dde3ae148aeaa+mr1613:6bfe93b5c4d6aaf0f50888f80d751ba3bfbae854");
                setOrClearEnv("AERO_VIRGL_RENDERER_SOURCE_BASE_REV", "ca50e008863837e094747a69974dde3ae148aeaa");
                setOrClearEnv("AERO_VIRGL_RENDERER_SOURCE_OVERLAY_REV", "6bfe93b5c4d6aaf0f50888f80d751ba3bfbae854");
                VirGLConfigDialog.setEnvVars(driverKeyValueConfig, envVars);
            } else {
                MesaOpenGLConfigDialog.setEnvVars(driverKeyValueConfig, envVars, normalizedGraphicsDriver);
            }

            boolean openGlBridgeUsesVulkanPrimary = openGlRouteNeedsVulkanCompanion && !vulkanProviderLane.isEmpty();
            boolean vulkanPrimaryProviderMissing = openGlRouteNeedsVulkanCompanion && vulkanProviderLane.isEmpty();
            if (openGlBridgeUsesVulkanPrimary) {
                setOrClearEnv("AERO_GRAPHICS_ACTIVE_ROUTE", vulkanProviderLane + "-primary");
                setOrClearEnv("AERO_DXVK_ROUTE_MODE", vulkanProviderLane + "-first");
                setOrClearEnv("AERO_VKD3D_ROUTE_MODE", vulkanProviderLane + "-first");
                setOrClearEnv("AERO_GRAPHICS_ACTIVE_PROVIDER_LANE", vulkanProviderLane);
                setOrClearEnv("AERO_GRAPHICS_ACTIVE_PROVIDER_PACKAGE", vulkanProviderPackage);
                setOrClearEnv("AERO_GRAPHICS_ACTIVE_PROVIDER_VERSION", vulkanProviderVersion);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_PROVIDER_LANE", openGlLane);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_PROVIDER_PACKAGE", openGlPackage);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_PROVIDER_VERSION", openGlVersion);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_REQUIRED_LANE", openGlLane);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_READY", "1");
            } else {
                setOrClearEnv("AERO_GRAPHICS_ACTIVE_ROUTE", openGlLane + "-primary");
                setOrClearEnv("AERO_DXVK_ROUTE_MODE", openGlLane + "-first");
                setOrClearEnv("AERO_VKD3D_ROUTE_MODE", openGlLane + "-first");
                setOrClearEnv("AERO_GRAPHICS_ACTIVE_PROVIDER_LANE", openGlLane);
                setOrClearEnv("AERO_GRAPHICS_ACTIVE_PROVIDER_PACKAGE", openGlPackage);
                setOrClearEnv("AERO_GRAPHICS_ACTIVE_PROVIDER_VERSION", openGlVersion);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_PROVIDER_LANE", vulkanProviderLane);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_PROVIDER_PACKAGE", vulkanProviderPackage);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_PROVIDER_VERSION", vulkanProviderVersion);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_REQUIRED_LANE", vulkanProviderLane);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_READY", vulkanProviderLane.isEmpty() ? "" : "1");
            }

            String openGlBridgeRouteDegradedReason = joinNonEmptyCsv(
                    virglPackageDegraded ? virglPackageDegradedReason : "",
                    aeMaliPackageDegraded ? aeMaliPackageDegradedReason : "",
                    aeMaliOpenGlDegraded ? aeMaliOpenGlDegradedReason : "",
                    openGlBridgeVulkanDegradedReason,
                    vulkanPrimaryProviderMissing ? "vulkan_primary_provider_missing" : ""
            );
            setOrClearEnv("AERO_GRAPHICS_ROUTE_DEGRADED", openGlBridgeRouteDegradedReason.isEmpty() ? "" : "1");
            setOrClearEnv("AERO_GRAPHICS_ROUTE_DEGRADED_REASON", openGlBridgeRouteDegradedReason);
            if (!virglRoute && !aeMaliGalliumRoute) {
                applyWrapperGraphicsConfigEnv(rootDir, adrenoToolsDriverId, useDRI3, dri3ForceSwWsi, dri3PresentWait);
            }
        } else if (GraphicsDrivers.isGladio(normalizedGraphicsDriver)) {
            VortekWrapperState vortekState = resolveVortekWrapperState(driverKeyValueConfig.get("vortekPackageVersion"));
            ensureGraphicsDriverAssetExtracted(rootDir, vortekState.bundledAsset.assetPath, vortekState.bundledAsset.extractProbePath);
            GladioOverlayState gladioState = resolveGladioOverlayState(
                    rootDir,
                    driverKeyValueConfig.get("gladioPackageVersion")
            );

            String selectedVulkanApi = firstNonEmpty(
                    trimToEmpty(driverKeyValueConfig.get("vkMaxVersion")),
                    VortekConfigDialog.DEFAULT_VK_MAX_VERSION
            );
            VortekVulkanDriverPackageManager vortekVulkanDriverPackageManager = new VortekVulkanDriverPackageManager(this);
            String requestedVulkanDriverEntry = VortekVulkanDriverPackageManager.normalizeEntry(
                    driverKeyValueConfig.get("vulkanDriverEntry", VortekVulkanDriverPackageManager.SYSTEM_ENTRY)
            );
            boolean nonSystemVulkanDriverRequested = !VortekVulkanDriverPackageManager.isSystemEntry(requestedVulkanDriverEntry);
            boolean renderNodeAvailable = hasAccessibleRenderNode();
            boolean vortekRouteDegraded = false;
            String vortekRouteDegradedReason = "";
            boolean customVulkanOverlayReady = false;
            String activeVulkanDriverEntry = requestedVulkanDriverEntry;
            VortekVulkanDriverPackageManager.PackageInfo activeVulkanDriverInfo =
                    vortekVulkanDriverPackageManager.getPackageInfo(requestedVulkanDriverEntry);
            String hostVulkanLibraryPath = vortekVulkanDriverPackageManager.resolveLibraryPath(requestedVulkanDriverEntry);
            String rootVulkanLibraryPath = vortekVulkanDriverPackageManager.resolveRootLibraryPath(requestedVulkanDriverEntry);
            if (nonSystemVulkanDriverRequested) {
                if (activeVulkanDriverInfo == null || trimToEmpty(hostVulkanLibraryPath).isEmpty()) {
                    vortekRouteDegraded = true;
                    vortekRouteDegradedReason = "vortek_vulkan_package_host_library_missing";
                    activeVulkanDriverEntry = VortekVulkanDriverPackageManager.SYSTEM_ENTRY;
                    activeVulkanDriverInfo = vortekVulkanDriverPackageManager.getPackageInfo(activeVulkanDriverEntry);
                    hostVulkanLibraryPath = null;
                    rootVulkanLibraryPath = "";
                } else if (activeVulkanDriverInfo.requiresRenderNode && !renderNodeAvailable) {
                    vortekRouteDegraded = true;
                    vortekRouteDegradedReason = "vortek_vulkan_package_requires_render_node";
                    activeVulkanDriverEntry = VortekVulkanDriverPackageManager.SYSTEM_ENTRY;
                    activeVulkanDriverInfo = vortekVulkanDriverPackageManager.getPackageInfo(activeVulkanDriverEntry);
                    hostVulkanLibraryPath = null;
                    rootVulkanLibraryPath = "";
                } else if (!trimToEmpty(rootVulkanLibraryPath).isEmpty()) {
                    customVulkanOverlayReady = vortekVulkanDriverPackageManager.deployPackageToRoot(rootDir, requestedVulkanDriverEntry)
                            && new File(rootDir, rootVulkanLibraryPath).isFile();
                    if (!customVulkanOverlayReady) {
                        vortekRouteDegraded = true;
                        vortekRouteDegradedReason = "vortek_vulkan_package_overlay_deploy_failed";
                        rootVulkanLibraryPath = "";
                    }
                }
            }
            File vortekIcdFile = rewriteVortekIcdFile(
                    rootDir,
                    customVulkanOverlayReady ? rootVulkanLibraryPath : "",
                    selectedVulkanApi
            );
            if (vortekIcdFile == null && !vortekRouteDegraded) {
                vortekRouteDegraded = true;
                vortekRouteDegradedReason = "vortek_wrapper_icd_bionic_incompatible";
            }
            File validationLayerManifest = new File(imageFs.getShareDir(), "vulkan/explicit_layer.d/VkLayer_khronos_validation.json");
            String routingMode = VortekConfigDialog.normalizeRoutingMode(driverKeyValueConfig.get("routingMode", VortekConfigDialog.ROUTING_AUTO));
            String extensionProfile = VortekExtensionPolicy.normalizeProfile(driverKeyValueConfig.get("extensionProfile", VortekExtensionPolicy.PROFILE_MALI_SYSTEM));
            String exposedExtensions = driverKeyValueConfig.get("exposedDeviceExtensions");
            if (exposedExtensions.isEmpty()) {
                exposedExtensions = VortekExtensionPolicy.joinExtensions(
                        VortekExtensionPolicy.getSelectedExtensionsForProfile(
                                extensionProfile,
                                VortekExtensionPolicy.buildCandidateExtensions(GPUHelper.vkGetDeviceExtensions())
                        )
                );
            }
            String disabledExtensions = driverKeyValueConfig.get("disabledDeviceExtensions", "");
            if (disabledExtensions.isEmpty()) {
                disabledExtensions = VortekExtensionPolicy.joinExtensions(VortekExtensionPolicy.getDisabledExtensionsForProfile(extensionProfile));
            }
            boolean useVortekPrimary = shouldUseVortekPrimaryRoute(routingMode);
            String gladioTransportBlockReason = gladioState.requiresRenderNode
                    && isAeMaliOpenGlProvider(gladioState.providerLane, gladioState.driverKind, gladioState.sourceRepo)
                    && !renderNodeAvailable
                    ? "gladio_aemali_gallium_requires_render_node"
                    : "";
            String routeDegradedReason = joinNonEmptyCsv(
                    vortekRouteDegraded ? vortekRouteDegradedReason : "",
                    gladioState.degraded ? gladioState.degradedReason : "",
                    gladioTransportBlockReason
            );

            if (vortekIcdFile != null) {
                envVars.put("VK_ICD_FILENAMES", vortekIcdFile.getAbsolutePath());
                envVars.put("VK_DRIVER_FILES", vortekIcdFile.getAbsolutePath());
            }
            envVars.put("VORTEK_SERVER_PATH", rootDir.getAbsolutePath() + UnixSocketConfig.VORTEK_SERVER_PATH);
            envVars.put("WINEVKUSEPLACEDADDR", "1");
            if (driverKeyValueConfig.getBoolean("gladioNoError", true)) envVars.put("GLADIO_NO_ERROR", "1");

            String graphicsStackProfile = firstNonEmpty(
                    trimToEmpty(gladioState.graphicsStackProfile),
                    trimToEmpty(vortekState.graphicsStackProfile),
                    "mediatek-gladio-vortek"
            );
            String openGlGalliumDriver = trimToEmpty(gladioState.preferredGalliumDriver);
            boolean aeMaliOpenGlOverlay = isAeMaliOpenGlProvider(
                    gladioState.providerLane,
                    gladioState.driverKind,
                    gladioState.sourceRepo
            );
            if (aeMaliOpenGlOverlay) {
                String defaultOpenGlGalliumDriver = firstNonEmpty(openGlGalliumDriver, "panfrost");
                openGlGalliumDriver = normalizeMaliGalliumDriver(
                        driverKeyValueConfig.get("galliumDriver", defaultOpenGlGalliumDriver),
                        defaultOpenGlGalliumDriver
                );
            }
            if (!openGlGalliumDriver.isEmpty()) envVars.put("GALLIUM_DRIVER", openGlGalliumDriver);
            applyAeMaliOpenGlMesaCompatEnv(driverKeyValueConfig, aeMaliOpenGlOverlay, openGlGalliumDriver);
            setOrClearEnv("AERO_GRAPHICS_STACK_PROFILE", graphicsStackProfile);
            setOrClearEnv("AERO_GRAPHICS_SOC_CLASS", socClass);
            setOrClearEnv("AERO_GRAPHICS_SELECTED_DRIVER_ENTRY", normalizedGraphicsDriver);
            setOrClearEnv("AERO_GRAPHICS_SELECTED_DRIVER_LANE", normalizedGraphicsDriver);
            setOrClearEnv("AERO_GRAPHICS_SELECTED_DRIVER_PACKAGE", gladioState.packageLabel);
            setOrClearEnv("AERO_GRAPHICS_VULKAN_PROVIDER", vortekState.providerLane);
            setOrClearEnv("AERO_GRAPHICS_OPENGL_PROVIDER", gladioState.providerLane);
            setOrClearEnv("AERO_VORTEK_PACKAGE", vortekState.packageLabel);
            setOrClearEnv("AERO_VORTEK_PACKAGE_VERSION", vortekState.version);
            setOrClearEnv("AERO_VORTEK_PACKAGE_ENTRY", vortekState.activeEntry);
            setOrClearEnv("AERO_VORTEK_PACKAGE_SOURCE_REPO", vortekState.sourceRepo);
            applyGraphicsDriverMetadataEnv(
                    "AERO_VORTEK_PACKAGE",
                    vortekState.providerLane,
                    vortekState.driverKind,
                    vortekState.transport,
                    vortekState.supportClass,
                    vortekState.kernelEvidenceClass,
                    vortekState.transportRequirements,
                    vortekState.ownerLane,
                    vortekState.routeId,
                    vortekState.rankedKernelDonors,
                    vortekState.diagnosticKeys,
                    vortekState.requiresRenderNode
            );
            setOrClearEnv("AERO_OPENGL_PACKAGE", gladioState.packageLabel);
            setOrClearEnv("AERO_OPENGL_VERSION", gladioState.version);
            setOrClearEnv("AERO_OPENGL_SOURCE_REPO", gladioState.sourceRepo);
            setOrClearEnv("AERO_OPENGL_GALLIUM_DRIVER", openGlGalliumDriver);
            setOrClearEnv("AERO_OPENGL_OVERLAY_ACTIVE", gladioState.customOverlayReady ? "1" : "0");
            setOrClearEnv("AERO_OPENGL_OVERLAY_PACKAGE", gladioState.customOverlayReady ? gladioState.packageLabel : "");
            setOrClearEnv("AERO_OPENGL_OVERLAY_ENTRY", gladioState.customOverlayReady ? gladioState.activeEntry : "");
            setOrClearEnv("AERO_OPENGL_OVERLAY_VERSION", gladioState.customOverlayReady ? gladioState.version : "");
            setOrClearEnv("AERO_OPENGL_PACKAGE_ENTRY_REQUESTED", gladioState.requestedEntry);
            setOrClearEnv("AERO_OPENGL_PACKAGE_ENTRY", gladioState.activeEntry);
            applyGraphicsDriverMetadataEnv(
                    "AERO_OPENGL",
                    gladioState.providerLane,
                    gladioState.driverKind,
                    gladioState.transport,
                    gladioState.supportClass,
                    gladioState.kernelEvidenceClass,
                    gladioState.transportRequirements,
                    gladioState.ownerLane,
                    gladioState.routeId,
                    gladioState.rankedKernelDonors,
                    gladioState.diagnosticKeys,
                    gladioState.requiresRenderNode
            );
            applyAeMaliOpenGlPolicyEnv(
                    gladioState.providerLane,
                    gladioState.driverKind,
                    gladioState.sourceRepo,
                    gladioState.transport,
                    gladioState.supportClass,
                    gladioState.kernelEvidenceClass,
                    gladioState.transportRequirements,
                    gladioState.ownerLane,
                    gladioState.routeId,
                    gladioState.rankedKernelDonors,
                    gladioState.diagnosticKeys,
                    gladioState.requiresRenderNode,
                    renderNodeAvailable,
                    openGlGalliumDriver,
                    gladioState.graphicsStackProfile
            );
            setOrClearEnv("AERO_OPENGL_CONTAINER_SOURCE", gladioState.customOverlayReady
                    ? (aeMaliOpenGlOverlay ? "aemali-gallium-overlay" : "custom-overlay")
                    : "bundled-gladio");
            setOrClearEnv("AERO_TURNIP_PACKAGE", "");
            setOrClearEnv("AERO_TURNIP_VERSION", "");
            setOrClearEnv("AERO_TURNIP_SOURCE_REPO", "");
            setOrClearEnv("AERO_TURNIP_RELEASE_TAG", "");
            setOrClearEnv("AERO_TURNIP_GALLIUM_BRIDGE", "");
            setOrClearEnv("AERO_TURNIP_API_FOCUS", "");
            setOrClearEnv("AERO_TURNIP_FORENSIC_LOG_PREFIXES", "");
            setOrClearEnv("AERO_GL_FALLBACK_ENGINE", "wined3d");
            setOrClearEnv("AERO_DXVK_LEGACY_DX89_PATH", "wined3d");
            setOrClearEnv("AERO_DXVK_GL_FALLBACK", "1");
            setOrClearEnv("AERO_VKD3D_GL_FALLBACK", "1");
            boolean activeAeMaliPanvkOverlay = VortekVulkanDriverPackageManager.isBundledAeMaliPackageEntry(activeVulkanDriverEntry);
            String vortekRuntimeSource = activeVulkanDriverInfo == null || activeVulkanDriverInfo.builtin
                    ? "vortek-builtin"
                    : activeAeMaliPanvkOverlay
                    ? (customVulkanOverlayReady ? "aemali-panvk-full" : "aemali-panvk-host")
                    : (customVulkanOverlayReady ? "vortek-custom-full" : "vortek-custom-host");
            setOrClearEnv("AERO_VULKAN_RUNTIME_SOURCE", vortekRuntimeSource);
            setOrClearEnv("AERO_VULKAN_WRAPPER_ICD", vortekIcdFile == null ? "" : vortekIcdFile.getAbsolutePath());
            setOrClearEnv("AERO_VULKAN_WRAPPER_API_MAX", selectedVulkanApi);
            setOrClearEnv("AERO_VULKAN_API_MIN_AVAILABLE", "");
            setOrClearEnv("AERO_VULKAN_API_MAX_AVAILABLE", selectedVulkanApi);
            setOrClearEnv("AERO_VULKAN_VALIDATION_LAYER_MANIFEST", validationLayerManifest.isFile() ? validationLayerManifest.getAbsolutePath() : "");
            setOrClearEnv("AERO_VULKAN_API_SELECTED", selectedVulkanApi);
            setOrClearEnv("WRAPPER_VK_VERSION", "");
            setOrClearEnv("AERO_MEDIATEK_WRAPPER_MODE", routingMode);
            setOrClearEnv("AERO_VORTEK_VULKAN_SOURCE", buildVortekRuntimeSource(activeVulkanDriverInfo));
            setOrClearEnv("AERO_VORTEK_VULKAN_DRIVER_ENTRY_REQUESTED", requestedVulkanDriverEntry);
            setOrClearEnv("AERO_VORTEK_VULKAN_DRIVER_ENTRY", activeVulkanDriverEntry);
            setOrClearEnv("AERO_VORTEK_VULKAN_DRIVER_NAME", activeVulkanDriverInfo == null ? "" : activeVulkanDriverInfo.name);
            setOrClearEnv("AERO_VORTEK_VULKAN_DRIVER_VERSION", activeVulkanDriverInfo == null ? "" : activeVulkanDriverInfo.version);
            applyGraphicsDriverMetadataEnv(
                    "AERO_VORTEK_VULKAN",
                    activeVulkanDriverInfo == null ? "" : activeVulkanDriverInfo.providerLane,
                    activeVulkanDriverInfo == null ? "" : activeVulkanDriverInfo.driverKind,
                    activeVulkanDriverInfo == null ? "" : activeVulkanDriverInfo.transport,
                    activeVulkanDriverInfo == null ? "" : activeVulkanDriverInfo.supportClass,
                    activeVulkanDriverInfo == null ? "" : activeVulkanDriverInfo.kernelEvidenceClass,
                    activeVulkanDriverInfo == null ? "" : activeVulkanDriverInfo.transportRequirements,
                    activeVulkanDriverInfo == null ? "" : activeVulkanDriverInfo.ownerLane,
                    activeVulkanDriverInfo == null ? "" : activeVulkanDriverInfo.routeId,
                    activeVulkanDriverInfo == null ? "" : activeVulkanDriverInfo.rankedKernelDonors,
                    activeVulkanDriverInfo == null ? "" : activeVulkanDriverInfo.diagnosticKeys,
                    activeVulkanDriverInfo != null && activeVulkanDriverInfo.requiresRenderNode
            );
            setOrClearEnv("AERO_VORTEK_VULKAN_DRIVER_SOURCE_REPO", activeVulkanDriverInfo == null ? "" : activeVulkanDriverInfo.sourceRepo);
            setOrClearEnv("AERO_VORTEK_VULKAN_LIBRARY_PATH", hostVulkanLibraryPath);
            setOrClearEnv("AERO_VORTEK_VULKAN_CONTAINER_SOURCE", customVulkanOverlayReady
                    ? (activeAeMaliPanvkOverlay ? "aemali-panvk-overlay" : "custom-overlay")
                    : "bundled-vortek");
            setOrClearEnv(
                    "AERO_VORTEK_VULKAN_CONTAINER_LIBRARY",
                    customVulkanOverlayReady && !trimToEmpty(rootVulkanLibraryPath).isEmpty()
                            ? new File(rootDir, rootVulkanLibraryPath).getAbsolutePath()
                            : new File(rootDir, firstNonEmpty(trimToEmpty(vortekState.rootLibraryPath), "usr/lib/libvulkan_vortek.so")).getAbsolutePath()
            );
            setOrClearEnv("AERO_VORTEK_VULKAN_RENDER_NODE_AVAILABLE", renderNodeAvailable ? "1" : "");
            setOrClearEnv("AERO_VORTEK_VULKAN_DRIVER_EXPERIMENTAL", activeVulkanDriverInfo != null && activeVulkanDriverInfo.experimental ? "1" : "");
            setOrClearEnv("AERO_VORTEK_MALI_NATIVE_VULKAN", VortekExtensionPolicy.isMaliProfile(extensionProfile) ? "1" : "");
            setOrClearEnv("AERO_VORTEK_EXTENSION_PROFILE", extensionProfile);
            setOrClearEnv("AERO_VORTEK_EXPOSED_EXTENSIONS", exposedExtensions);
            setOrClearEnv("AERO_VORTEK_DISABLED_EXTENSIONS", disabledExtensions);
            applyAeMaliPolicyEnv(activeVulkanDriverInfo, extensionProfile, selectedVulkanApi, renderNodeAvailable);

            if (useVortekPrimary) {
                setOrClearEnv("AERO_GRAPHICS_ACTIVE_ROUTE", "vortek-primary");
                setOrClearEnv("AERO_DXVK_ROUTE_MODE", "vortek-first");
                setOrClearEnv("AERO_VKD3D_ROUTE_MODE", "vortek-first");
                setOrClearEnv("AERO_GRAPHICS_ACTIVE_PROVIDER_LANE", vortekState.providerLane);
                setOrClearEnv("AERO_GRAPHICS_ACTIVE_PROVIDER_PACKAGE", vortekState.packageLabel);
                setOrClearEnv("AERO_GRAPHICS_ACTIVE_PROVIDER_VERSION", vortekState.version);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_PROVIDER_LANE", gladioState.providerLane);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_PROVIDER_PACKAGE", gladioState.packageLabel);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_PROVIDER_VERSION", gladioState.version);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_REQUIRED_LANE", gladioState.providerLane);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_READY", "1");
            } else {
                setOrClearEnv("AERO_GRAPHICS_ACTIVE_ROUTE", "gladio-primary");
                setOrClearEnv("AERO_DXVK_ROUTE_MODE", "gladio-first");
                setOrClearEnv("AERO_VKD3D_ROUTE_MODE", "gladio-first");
                setOrClearEnv("AERO_GRAPHICS_ACTIVE_PROVIDER_LANE", gladioState.providerLane);
                setOrClearEnv("AERO_GRAPHICS_ACTIVE_PROVIDER_PACKAGE", gladioState.packageLabel);
                setOrClearEnv("AERO_GRAPHICS_ACTIVE_PROVIDER_VERSION", gladioState.version);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_PROVIDER_LANE", vortekState.providerLane);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_PROVIDER_PACKAGE", vortekState.packageLabel);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_PROVIDER_VERSION", vortekState.version);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_REQUIRED_LANE", vortekState.providerLane);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_READY", "1");
            }

            setOrClearEnv("AERO_GRAPHICS_ROUTE_DEGRADED", routeDegradedReason.isEmpty() ? "" : "1");
            setOrClearEnv("AERO_GRAPHICS_ROUTE_DEGRADED_REASON", routeDegradedReason);
        } else if (GraphicsDrivers.isVortek(normalizedGraphicsDriver)) {
            VortekWrapperState vortekState = resolveVortekWrapperState(driverKeyValueConfig.get("vortekPackageVersion"));
            ensureGraphicsDriverAssetExtracted(rootDir, vortekState.bundledAsset.assetPath, vortekState.bundledAsset.extractProbePath);
            GladioOverlayState gladioState = resolveGladioOverlayState(
                    rootDir,
                    driverKeyValueConfig.get("gladioPackageVersion")
            );

            String selectedVulkanApi = firstNonEmpty(
                    trimToEmpty(driverKeyValueConfig.get("vkMaxVersion")),
                    VortekConfigDialog.DEFAULT_VK_MAX_VERSION
            );
            VortekVulkanDriverPackageManager vortekVulkanDriverPackageManager = new VortekVulkanDriverPackageManager(this);
            String requestedVulkanDriverEntry = VortekVulkanDriverPackageManager.normalizeEntry(
                    driverKeyValueConfig.get("vulkanDriverEntry", VortekVulkanDriverPackageManager.SYSTEM_ENTRY)
            );
            boolean nonSystemVulkanDriverRequested = !VortekVulkanDriverPackageManager.isSystemEntry(requestedVulkanDriverEntry);
            boolean renderNodeAvailable = hasAccessibleRenderNode();
            boolean vortekRouteDegraded = false;
            String vortekRouteDegradedReason = "";
            boolean customVulkanOverlayReady = false;
            String activeVulkanDriverEntry = requestedVulkanDriverEntry;
            VortekVulkanDriverPackageManager.PackageInfo activeVulkanDriverInfo =
                    vortekVulkanDriverPackageManager.getPackageInfo(requestedVulkanDriverEntry);
            String hostVulkanLibraryPath = vortekVulkanDriverPackageManager.resolveLibraryPath(requestedVulkanDriverEntry);
            String rootVulkanLibraryPath = vortekVulkanDriverPackageManager.resolveRootLibraryPath(requestedVulkanDriverEntry);
            if (nonSystemVulkanDriverRequested) {
                if (activeVulkanDriverInfo == null || trimToEmpty(hostVulkanLibraryPath).isEmpty()) {
                    vortekRouteDegraded = true;
                    vortekRouteDegradedReason = "vortek_vulkan_package_host_library_missing";
                    activeVulkanDriverEntry = VortekVulkanDriverPackageManager.SYSTEM_ENTRY;
                    activeVulkanDriverInfo = vortekVulkanDriverPackageManager.getPackageInfo(activeVulkanDriverEntry);
                    hostVulkanLibraryPath = null;
                    rootVulkanLibraryPath = "";
                } else if (activeVulkanDriverInfo.requiresRenderNode && !renderNodeAvailable) {
                    vortekRouteDegraded = true;
                    vortekRouteDegradedReason = "vortek_vulkan_package_requires_render_node";
                    activeVulkanDriverEntry = VortekVulkanDriverPackageManager.SYSTEM_ENTRY;
                    activeVulkanDriverInfo = vortekVulkanDriverPackageManager.getPackageInfo(activeVulkanDriverEntry);
                    hostVulkanLibraryPath = null;
                    rootVulkanLibraryPath = "";
                } else if (!trimToEmpty(rootVulkanLibraryPath).isEmpty()) {
                    customVulkanOverlayReady = vortekVulkanDriverPackageManager.deployPackageToRoot(rootDir, requestedVulkanDriverEntry)
                            && new File(rootDir, rootVulkanLibraryPath).isFile();
                    if (!customVulkanOverlayReady) {
                        vortekRouteDegraded = true;
                        vortekRouteDegradedReason = "vortek_vulkan_package_overlay_deploy_failed";
                        rootVulkanLibraryPath = "";
                    }
                }
            }
            File vortekIcdFile = rewriteVortekIcdFile(
                    rootDir,
                    customVulkanOverlayReady ? rootVulkanLibraryPath : "",
                    selectedVulkanApi
            );
            if (vortekIcdFile == null && !vortekRouteDegraded) {
                vortekRouteDegraded = true;
                vortekRouteDegradedReason = "vortek_wrapper_icd_bionic_incompatible";
            }
            File validationLayerManifest = new File(imageFs.getShareDir(), "vulkan/explicit_layer.d/VkLayer_khronos_validation.json");
            String routingMode = VortekConfigDialog.normalizeRoutingMode(driverKeyValueConfig.get("routingMode", VortekConfigDialog.ROUTING_AUTO));
            String extensionProfile = VortekExtensionPolicy.normalizeProfile(driverKeyValueConfig.get("extensionProfile", VortekExtensionPolicy.PROFILE_MALI_SYSTEM));
            String exposedExtensions = driverKeyValueConfig.get("exposedDeviceExtensions");
            if (exposedExtensions.isEmpty()) {
                exposedExtensions = VortekExtensionPolicy.joinExtensions(
                        VortekExtensionPolicy.getSelectedExtensionsForProfile(
                                extensionProfile,
                                VortekExtensionPolicy.buildCandidateExtensions(GPUHelper.vkGetDeviceExtensions())
                        )
                );
            }
            String disabledExtensions = driverKeyValueConfig.get("disabledDeviceExtensions", "");
            if (disabledExtensions.isEmpty()) {
                disabledExtensions = VortekExtensionPolicy.joinExtensions(VortekExtensionPolicy.getDisabledExtensionsForProfile(extensionProfile));
            }
            boolean useVortekPrimary = shouldUseVortekPrimaryRoute(routingMode);
            String gladioTransportBlockReason = gladioState.requiresRenderNode
                    && isAeMaliOpenGlProvider(gladioState.providerLane, gladioState.driverKind, gladioState.sourceRepo)
                    && !renderNodeAvailable
                    ? "gladio_aemali_gallium_requires_render_node"
                    : "";
            String routeDegradedReason = joinNonEmptyCsv(
                    vortekRouteDegraded ? vortekRouteDegradedReason : "",
                    gladioState.degraded ? gladioState.degradedReason : "",
                    gladioTransportBlockReason
            );

            if (vortekIcdFile != null) {
                envVars.put("VK_ICD_FILENAMES", vortekIcdFile.getAbsolutePath());
                envVars.put("VK_DRIVER_FILES", vortekIcdFile.getAbsolutePath());
            }

            envVars.put("VORTEK_SERVER_PATH", rootDir.getAbsolutePath() + UnixSocketConfig.VORTEK_SERVER_PATH);
            envVars.put("WINEVKUSEPLACEDADDR", "1");
            if (driverKeyValueConfig.getBoolean("gladioNoError", true)) envVars.put("GLADIO_NO_ERROR", "1");
            String graphicsStackProfile = firstNonEmpty(
                    trimToEmpty(gladioState.graphicsStackProfile),
                    trimToEmpty(vortekState.graphicsStackProfile),
                    "vortek-gladio"
            );
            String openGlGalliumDriver = trimToEmpty(gladioState.preferredGalliumDriver);
            boolean aeMaliOpenGlOverlay = isAeMaliOpenGlProvider(
                    gladioState.providerLane,
                    gladioState.driverKind,
                    gladioState.sourceRepo
            );
            if (aeMaliOpenGlOverlay) {
                String defaultOpenGlGalliumDriver = firstNonEmpty(openGlGalliumDriver, "panfrost");
                openGlGalliumDriver = normalizeMaliGalliumDriver(
                        driverKeyValueConfig.get("galliumDriver", defaultOpenGlGalliumDriver),
                        defaultOpenGlGalliumDriver
                );
            }
            if (!openGlGalliumDriver.isEmpty()) envVars.put("GALLIUM_DRIVER", openGlGalliumDriver);
            applyAeMaliOpenGlMesaCompatEnv(driverKeyValueConfig, aeMaliOpenGlOverlay, openGlGalliumDriver);
            setOrClearEnv("AERO_GRAPHICS_STACK_PROFILE", graphicsStackProfile);
            setOrClearEnv("AERO_GRAPHICS_SOC_CLASS", socClass);
            setOrClearEnv("AERO_GRAPHICS_SELECTED_DRIVER_ENTRY", normalizedGraphicsDriver);
            setOrClearEnv("AERO_GRAPHICS_SELECTED_DRIVER_LANE", normalizedGraphicsDriver);
            setOrClearEnv("AERO_GRAPHICS_SELECTED_DRIVER_PACKAGE", vortekState.packageLabel);
            setOrClearEnv("AERO_GRAPHICS_VULKAN_PROVIDER", vortekState.providerLane);
            setOrClearEnv("AERO_GRAPHICS_OPENGL_PROVIDER", gladioState.providerLane);
            setOrClearEnv("AERO_VORTEK_PACKAGE", vortekState.packageLabel);
            setOrClearEnv("AERO_VORTEK_PACKAGE_VERSION", vortekState.version);
            setOrClearEnv("AERO_VORTEK_PACKAGE_ENTRY", vortekState.activeEntry);
            setOrClearEnv("AERO_VORTEK_PACKAGE_SOURCE_REPO", vortekState.sourceRepo);
            applyGraphicsDriverMetadataEnv(
                    "AERO_VORTEK_PACKAGE",
                    vortekState.providerLane,
                    vortekState.driverKind,
                    vortekState.transport,
                    vortekState.supportClass,
                    vortekState.kernelEvidenceClass,
                    vortekState.transportRequirements,
                    vortekState.ownerLane,
                    vortekState.routeId,
                    vortekState.rankedKernelDonors,
                    vortekState.diagnosticKeys,
                    vortekState.requiresRenderNode
            );
            setOrClearEnv("AERO_OPENGL_PACKAGE", gladioState.packageLabel);
            setOrClearEnv("AERO_OPENGL_VERSION", gladioState.version);
            setOrClearEnv("AERO_OPENGL_SOURCE_REPO", gladioState.sourceRepo);
            setOrClearEnv("AERO_OPENGL_GALLIUM_DRIVER", openGlGalliumDriver);
            setOrClearEnv("AERO_OPENGL_OVERLAY_ACTIVE", gladioState.customOverlayReady ? "1" : "0");
            setOrClearEnv("AERO_OPENGL_OVERLAY_PACKAGE", gladioState.customOverlayReady ? gladioState.packageLabel : "");
            setOrClearEnv("AERO_OPENGL_OVERLAY_ENTRY", gladioState.customOverlayReady ? gladioState.activeEntry : "");
            setOrClearEnv("AERO_OPENGL_OVERLAY_VERSION", gladioState.customOverlayReady ? gladioState.version : "");
            setOrClearEnv("AERO_OPENGL_PACKAGE_ENTRY_REQUESTED", gladioState.requestedEntry);
            setOrClearEnv("AERO_OPENGL_PACKAGE_ENTRY", gladioState.activeEntry);
            applyGraphicsDriverMetadataEnv(
                    "AERO_OPENGL",
                    gladioState.providerLane,
                    gladioState.driverKind,
                    gladioState.transport,
                    gladioState.supportClass,
                    gladioState.kernelEvidenceClass,
                    gladioState.transportRequirements,
                    gladioState.ownerLane,
                    gladioState.routeId,
                    gladioState.rankedKernelDonors,
                    gladioState.diagnosticKeys,
                    gladioState.requiresRenderNode
            );
            applyAeMaliOpenGlPolicyEnv(
                    gladioState.providerLane,
                    gladioState.driverKind,
                    gladioState.sourceRepo,
                    gladioState.transport,
                    gladioState.supportClass,
                    gladioState.kernelEvidenceClass,
                    gladioState.transportRequirements,
                    gladioState.ownerLane,
                    gladioState.routeId,
                    gladioState.rankedKernelDonors,
                    gladioState.diagnosticKeys,
                    gladioState.requiresRenderNode,
                    renderNodeAvailable,
                    openGlGalliumDriver,
                    gladioState.graphicsStackProfile
            );
            setOrClearEnv("AERO_OPENGL_CONTAINER_SOURCE", gladioState.customOverlayReady
                    ? (aeMaliOpenGlOverlay ? "aemali-gallium-overlay" : "custom-overlay")
                    : "bundled-gladio");
            setOrClearEnv("AERO_TURNIP_PACKAGE", "");
            setOrClearEnv("AERO_TURNIP_VERSION", "");
            setOrClearEnv("AERO_TURNIP_SOURCE_REPO", "");
            setOrClearEnv("AERO_TURNIP_RELEASE_TAG", "");
            setOrClearEnv("AERO_TURNIP_GALLIUM_BRIDGE", "");
            setOrClearEnv("AERO_TURNIP_API_FOCUS", "");
            setOrClearEnv("AERO_TURNIP_FORENSIC_LOG_PREFIXES", "");
            setOrClearEnv("AERO_GL_FALLBACK_ENGINE", "wined3d");
            setOrClearEnv("AERO_DXVK_LEGACY_DX89_PATH", "wined3d");
            setOrClearEnv("AERO_DXVK_GL_FALLBACK", "1");
            setOrClearEnv("AERO_VKD3D_GL_FALLBACK", "1");
            boolean activeAeMaliPanvkOverlay = VortekVulkanDriverPackageManager.isBundledAeMaliPackageEntry(activeVulkanDriverEntry);
            String vortekRuntimeSource = activeVulkanDriverInfo == null || activeVulkanDriverInfo.builtin
                    ? "vortek-builtin"
                    : activeAeMaliPanvkOverlay
                    ? (customVulkanOverlayReady ? "aemali-panvk-full" : "aemali-panvk-host")
                    : (customVulkanOverlayReady ? "vortek-custom-full" : "vortek-custom-host");
            setOrClearEnv("AERO_VULKAN_RUNTIME_SOURCE", vortekRuntimeSource);
            setOrClearEnv("AERO_VULKAN_WRAPPER_ICD", vortekIcdFile == null ? "" : vortekIcdFile.getAbsolutePath());
            setOrClearEnv("AERO_VULKAN_WRAPPER_API_MAX", selectedVulkanApi);
            setOrClearEnv("AERO_VULKAN_API_MIN_AVAILABLE", "");
            setOrClearEnv("AERO_VULKAN_API_MAX_AVAILABLE", selectedVulkanApi);
            setOrClearEnv("AERO_VULKAN_VALIDATION_LAYER_MANIFEST", validationLayerManifest.isFile() ? validationLayerManifest.getAbsolutePath() : "");
            setOrClearEnv("AERO_VULKAN_API_SELECTED", selectedVulkanApi);
            setOrClearEnv("WRAPPER_VK_VERSION", "");
            setOrClearEnv("AERO_MEDIATEK_WRAPPER_MODE", routingMode);
            setOrClearEnv("AERO_VORTEK_VULKAN_SOURCE", buildVortekRuntimeSource(activeVulkanDriverInfo));
            setOrClearEnv("AERO_VORTEK_VULKAN_DRIVER_ENTRY_REQUESTED", requestedVulkanDriverEntry);
            setOrClearEnv("AERO_VORTEK_VULKAN_DRIVER_ENTRY", activeVulkanDriverEntry);
            setOrClearEnv("AERO_VORTEK_VULKAN_DRIVER_NAME", activeVulkanDriverInfo == null ? "" : activeVulkanDriverInfo.name);
            setOrClearEnv("AERO_VORTEK_VULKAN_DRIVER_VERSION", activeVulkanDriverInfo == null ? "" : activeVulkanDriverInfo.version);
            applyGraphicsDriverMetadataEnv(
                    "AERO_VORTEK_VULKAN",
                    activeVulkanDriverInfo == null ? "" : activeVulkanDriverInfo.providerLane,
                    activeVulkanDriverInfo == null ? "" : activeVulkanDriverInfo.driverKind,
                    activeVulkanDriverInfo == null ? "" : activeVulkanDriverInfo.transport,
                    activeVulkanDriverInfo == null ? "" : activeVulkanDriverInfo.supportClass,
                    activeVulkanDriverInfo == null ? "" : activeVulkanDriverInfo.kernelEvidenceClass,
                    activeVulkanDriverInfo == null ? "" : activeVulkanDriverInfo.transportRequirements,
                    activeVulkanDriverInfo == null ? "" : activeVulkanDriverInfo.ownerLane,
                    activeVulkanDriverInfo == null ? "" : activeVulkanDriverInfo.routeId,
                    activeVulkanDriverInfo == null ? "" : activeVulkanDriverInfo.rankedKernelDonors,
                    activeVulkanDriverInfo == null ? "" : activeVulkanDriverInfo.diagnosticKeys,
                    activeVulkanDriverInfo != null && activeVulkanDriverInfo.requiresRenderNode
            );
            setOrClearEnv("AERO_VORTEK_VULKAN_DRIVER_SOURCE_REPO", activeVulkanDriverInfo == null ? "" : activeVulkanDriverInfo.sourceRepo);
            setOrClearEnv("AERO_VORTEK_VULKAN_LIBRARY_PATH", hostVulkanLibraryPath);
            setOrClearEnv("AERO_VORTEK_VULKAN_CONTAINER_SOURCE", customVulkanOverlayReady
                    ? (activeAeMaliPanvkOverlay ? "aemali-panvk-overlay" : "custom-overlay")
                    : "bundled-vortek");
            setOrClearEnv(
                    "AERO_VORTEK_VULKAN_CONTAINER_LIBRARY",
                    customVulkanOverlayReady && !trimToEmpty(rootVulkanLibraryPath).isEmpty()
                            ? new File(rootDir, rootVulkanLibraryPath).getAbsolutePath()
                            : new File(rootDir, firstNonEmpty(trimToEmpty(vortekState.rootLibraryPath), "usr/lib/libvulkan_vortek.so")).getAbsolutePath()
            );
            setOrClearEnv("AERO_VORTEK_VULKAN_RENDER_NODE_AVAILABLE", renderNodeAvailable ? "1" : "");
            setOrClearEnv("AERO_VORTEK_VULKAN_DRIVER_EXPERIMENTAL", activeVulkanDriverInfo != null && activeVulkanDriverInfo.experimental ? "1" : "");
            setOrClearEnv("AERO_VORTEK_MALI_NATIVE_VULKAN", VortekExtensionPolicy.isMaliProfile(extensionProfile) ? "1" : "");
            setOrClearEnv("AERO_VORTEK_EXTENSION_PROFILE", extensionProfile);
            setOrClearEnv("AERO_VORTEK_EXPOSED_EXTENSIONS", exposedExtensions);
            setOrClearEnv("AERO_VORTEK_DISABLED_EXTENSIONS", disabledExtensions);
            applyAeMaliPolicyEnv(activeVulkanDriverInfo, extensionProfile, selectedVulkanApi, renderNodeAvailable);

            if (useVortekPrimary) {
                setOrClearEnv("AERO_GRAPHICS_ACTIVE_ROUTE", "vortek-primary");
                setOrClearEnv("AERO_DXVK_ROUTE_MODE", "vortek-first");
                setOrClearEnv("AERO_VKD3D_ROUTE_MODE", "vortek-first");
                setOrClearEnv("AERO_GRAPHICS_ACTIVE_PROVIDER_LANE", vortekState.providerLane);
                setOrClearEnv("AERO_GRAPHICS_ACTIVE_PROVIDER_PACKAGE", vortekState.packageLabel);
                setOrClearEnv("AERO_GRAPHICS_ACTIVE_PROVIDER_VERSION", vortekState.version);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_PROVIDER_LANE", gladioState.providerLane);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_PROVIDER_PACKAGE", gladioState.packageLabel);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_PROVIDER_VERSION", gladioState.version);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_REQUIRED_LANE", gladioState.providerLane);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_READY", "1");
            } else {
                setOrClearEnv("AERO_GRAPHICS_ACTIVE_ROUTE", "gladio-primary");
                setOrClearEnv("AERO_DXVK_ROUTE_MODE", "gladio-first");
                setOrClearEnv("AERO_VKD3D_ROUTE_MODE", "gladio-first");
                setOrClearEnv("AERO_GRAPHICS_ACTIVE_PROVIDER_LANE", gladioState.providerLane);
                setOrClearEnv("AERO_GRAPHICS_ACTIVE_PROVIDER_PACKAGE", gladioState.packageLabel);
                setOrClearEnv("AERO_GRAPHICS_ACTIVE_PROVIDER_VERSION", gladioState.version);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_PROVIDER_LANE", vortekState.providerLane);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_PROVIDER_PACKAGE", vortekState.packageLabel);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_PROVIDER_VERSION", vortekState.version);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_REQUIRED_LANE", vortekState.providerLane);
                setOrClearEnv("AERO_GRAPHICS_COMPANION_READY", "1");
            }

            setOrClearEnv("AERO_GRAPHICS_ROUTE_DEGRADED", routeDegradedReason.isEmpty() ? "" : "1");
            setOrClearEnv("AERO_GRAPHICS_ROUTE_DEGRADED_REASON", routeDegradedReason);
        }

        ForensicLogger.logEvent(
                this,
                "info",
                "GRAPHICS_ROUTE_APPLIED",
                null,
                "graphics_route",
                "graphics_route_applied",
                ForensicLogger.fields(
                        "graphics_driver", graphicsDriver,
                        "legacy_requested_driver", envVars.get("AERO_GRAPHICS_LEGACY_REQUESTED_DRIVER"),
                        "legacy_policy", envVars.get("AERO_GRAPHICS_LEGACY_POLICY"),
                        "route_degraded_reason", envVars.get("AERO_GRAPHICS_ROUTE_DEGRADED_REASON"),
                        "driver_id", forensicDriverId,
                        "dxwrapper_active", envVars.get("AERO_DXWRAPPER_ACTIVE"),
                        "dri3_mode", envVars.get("AERO_DRI3_MODE"),
                        "dri3_enabled", envVars.get("AERO_DRI3_ENABLED"),
                        "dri3_present_wait", envVars.get("AERO_DRI3_PRESENT_WAIT"),
                        "dri3_force_sw_wsi", envVars.get("AERO_DRI3_FORCE_SW_WSI"),
                        "selected_driver_entry", envVars.get("AERO_GRAPHICS_SELECTED_DRIVER_ENTRY"),
                        "active_provider_lane", envVars.get("AERO_GRAPHICS_ACTIVE_PROVIDER_LANE"),
                        "active_provider_package", envVars.get("AERO_GRAPHICS_ACTIVE_PROVIDER_PACKAGE"),
                        "active_provider_version", envVars.get("AERO_GRAPHICS_ACTIVE_PROVIDER_VERSION"),
                        "companion_provider_lane", envVars.get("AERO_GRAPHICS_COMPANION_PROVIDER_LANE"),
                        "opengl_overlay_active", envVars.get("AERO_OPENGL_OVERLAY_ACTIVE"),
                        "vulkan_api_selected", envVars.get("AERO_VULKAN_API_SELECTED"),
                        "vulkan_runtime_source", envVars.get("AERO_VULKAN_RUNTIME_SOURCE"),
                        "vulkan_wrapper_icd", envVars.get("AERO_VULKAN_WRAPPER_ICD"),
                        "vulkan_wrapper_api_max", envVars.get("AERO_VULKAN_WRAPPER_API_MAX"),
                        "wrapper_vk_version", envVars.get("WRAPPER_VK_VERSION")
                )
        );

        applyUpscalerEnvVars(vulkanPrimaryRoute, socClass);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        boolean handledByWinHandler = false;
        boolean handledByTouchpadView = false;

        // Let winHandler process the event if available
        if (winHandler != null) {
            handledByWinHandler = winHandler.onGenericMotionEvent(event);
            if (handledByWinHandler) {
                //Log.d("XServerDisplayActivity", "Event handled by winHandler");
            }
        }

        // Let touchpadView process the event if available
        if (touchpadView != null) {
            handledByTouchpadView = touchpadView.onExternalMouseEvent(event);
            if (handledByTouchpadView) {
                //Log.d("XServerDisplayActivity", "Event handled by touchpadView");
            }
        }

        // Pass the event to the super method to ensure system-level handling
        boolean handledBySuper = super.dispatchGenericMotionEvent(event);
        if (!handledBySuper) {
            //Log.d("XServerDisplayActivity", "Event not handled by super");
        }

        // Combine the results: any handler consuming the event indicates it was handled
        return handledByWinHandler || handledByTouchpadView || handledBySuper;
    }


    private static final int RECAPTURE_DELAY_MS = 10000; // 10 seconds

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (isRuntimeDrawerShortcutEvent(event)) {
            boolean handledByInputControls = inputControlsView != null && inputControlsView.onKeyEvent(event);
            boolean handledByWinHandler = winHandler != null && winHandler.onKeyEvent(event);
            boolean handledByXServerKeyboard = xServer != null && xServer.keyboard != null && xServer.keyboard.onKeyEvent(event);
            boolean wasVisible = runtimeDrawerVisible;
            showRuntimeDrawer();
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "RUNTIME_DRAWER_SHORTCUT_TRIGGERED",
                    null,
                    "xserver_input",
                    "runtime_drawer_shortcut",
                    ForensicLogger.fields(
                            "key_code", event.getKeyCode(),
                            "scan_code", event.getScanCode(),
                            "device_id", event.getDeviceId(),
                            "device_name", event.getDevice() != null ? event.getDevice().getName() : "-",
                            "handled_by_input_controls", handledByInputControls,
                            "handled_by_winhandler", handledByWinHandler,
                            "handled_by_xserver_keyboard", handledByXServerKeyboard,
                            "runtime_drawer_visible_before", wasVisible,
                            "runtime_drawer_visible_after", runtimeDrawerVisible
                    )
            );
            return true;
        }

        boolean handledByInputControls = inputControlsView != null && inputControlsView.onKeyEvent(event);
        boolean handledByWinHandler = winHandler != null && winHandler.onKeyEvent(event);
        boolean handledByXServerKeyboard = xServer != null && xServer.keyboard != null && xServer.keyboard.onKeyEvent(event);
        boolean handledBySuper = !ExternalController.isGameController(event.getDevice()) && super.dispatchKeyEvent(event);

        return handledByInputControls || handledByWinHandler || handledByXServerKeyboard || handledBySuper;
    }

    private boolean isRuntimeDrawerShortcutEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
        if (!ExternalController.isGameController(event.getDevice())) return false;
        int keyCode = event.getKeyCode();
        return keyCode == KeyEvent.KEYCODE_BUTTON_MODE
                || keyCode == KeyEvent.KEYCODE_HOME
                || keyCode == KeyEvent.KEYCODE_BUTTON_SELECT;
    }

    public InputControlsView getInputControlsView() {
        return inputControlsView;
    }

    private static final String TAG = "DXWrapperExtraction";

    private void extractDXWrapperFiles(String dxwrapper) {
        final String[] dlls = {"d3d10.dll", "d3d10_1.dll", "d3d10core.dll", "d3d11.dll", "d3d12.dll", "d3d12core.dll", "d3d8.dll", "d3d9.dll", "dxgi.dll", "ddraw.dll", "d3dimm.dll"};

        File rootDir = imageFs.getRootDir();
        File windowsDir = new File(WineUtils.resolveHostWinePrefixDir(rootDir), "drive_c/windows");
        cleanupDgVoodooRuntimeStage(rootDir);

        if (dxwrapper.contains("dxvk")) {
            Log.d(TAG, "Extracting DXVK wrapper files, version: " + dxwrapper);
            String dxvkWrapper = resolveConfiguredDxvkWrapper(dxwrapperConfig);
            String vkd3dWrapper = resolveConfiguredVkd3dWrapper(dxwrapperConfig);
            boolean dxvkReady = stageDxvkCompanionPayload(dxvkWrapper, windowsDir);
            boolean vkd3dReady = !"None".equalsIgnoreCase(vkd3dWrapper)
                    && stageVkd3dCompanionPayload(vkd3dWrapper, windowsDir);

            if ("None".equalsIgnoreCase(vkd3dWrapper) || !vkd3dReady) {
                Log.d(TAG, "No VKD3D has been selected, restoring original d3d12");
                restoreOriginalDllFiles(new String[]{"d3d12.dll", "d3d12core.dll"});
            }

            // Older DDraw wrapper payloads are superseded in the DXVK lane.
            // DDraw/D3D1-7/Glide routing is handled by dedicated dgVoodoo lanes when selected.
            restoreOriginalDllFiles(new String[]{ "ddraw.dll", "d3dimm.dll" });

            Log.d(TAG, "Finished extraction of DXVK wrapper files, version: " + dxwrapper);
            boolean routeReady = dxvkReady && ("None".equalsIgnoreCase(vkd3dWrapper) || vkd3dReady);
            ForensicLogger.logEvent(
                    this,
                    routeReady ? "info" : "warn",
                    "DXWRAPPER_RUNTIME_STAGE_READY",
                    null,
                    "dxwrapper",
                    routeReady ? "dxvk_runtime_stage_ready" : "dxvk_runtime_stage_incomplete",
                    ForensicLogger.fields(
                            "dxwrapper", dxwrapper,
                            "dxvk_wrapper", dxvkWrapper,
                            "dxvk_ready", dxvkReady ? "1" : "0",
                            "vkd3d_wrapper", vkd3dWrapper,
                            "vkd3d_ready", vkd3dReady ? "1" : "0"
                    )
            );
        } else if (dxwrapper.contains("dgvoodoo")) {
            Log.d(TAG, "Staging dgVoodoo runtime for older API route.");
            restoreOriginalDllFiles(dlls);

            DgVoodooManager manager = new DgVoodooManager(this);
            String shortcutPath = shortcut != null ? shortcut.path : "";
            KeyValueSet config = DgVoodooConfigDialog.parseConfig(dxwrapperConfig);
            WineUtils.WindowsLaunchTarget shortcutLaunchTarget = resolveEffectiveShortcutLaunchTarget();
            if (shortcutLaunchTarget == null) {
                shortcutLaunchTarget = resolveShortcutLaunchTarget(rootDir);
            }
            File stageTarget = manager.resolveShortcutTargetDir(shortcutLaunchTarget);
            if (stageTarget == null || !stageTarget.isDirectory()) {
                stageTarget = new File(windowsDir, "system32");
            }

            String activeArch = manager.resolvePreferredArch(shortcutLaunchTarget, config.get("dgvoodooArch"), wineInfo);
            String packageLane = DgVoodooManager.resolvePackageLaneForRuntimeArch(activeArch);
            boolean staged = manager.stageRuntime(stageTarget, activeArch);
            boolean dgVoodooVulkanBridge = supportsDgVoodooVulkanBridge(graphicsDriver);
            String dxvkWrapper = DgVoodooConfigDialog.resolveCompanionDxvkVersion(
                    config,
                    activeArch,
                    dgVoodooVulkanBridge,
                    contentsManager.getInstalledVersionNames(ContentProfile.ContentType.CONTENT_TYPE_DXVK, true)
            );
            String vkd3dWrapper = DgVoodooConfigDialog.resolveCompanionVkd3dVersion(config, dgVoodooVulkanBridge);
            boolean forceD3d11 = DgVoodooConfigDialog.resolveCompanionForceD3d11(config, dgVoodooVulkanBridge);
            boolean dxvkReady = false;
            boolean vkd3dReady = false;
            String routeState = staged ? "dgvoodoo" : "wined3d-fallback";
            String degradeReason = staged ? "" : "missing_package_lane:" + packageLane;
            String outputApi = "";

            if (staged && dgVoodooVulkanBridge) {
                dxvkReady = stageDxvkCompanionPayload(dxvkWrapper, windowsDir);
                if (!dxvkReady) {
                    routeState = "wined3d-fallback";
                    degradeReason = "missing_dxvk:" + dxvkWrapper;
                } else {
                    if (!forceD3d11 && !"None".equalsIgnoreCase(vkd3dWrapper)) {
                        vkd3dReady = stageVkd3dCompanionPayload(vkd3dWrapper, windowsDir);
                        if (!vkd3dReady) {
                            routeState = "wined3d-fallback";
                            degradeReason = "missing_vkd3d:" + vkd3dWrapper;
                        }
                    }

                    if (degradeReason.isEmpty()) {
                        outputApi = resolveDgVoodooOutputApi(forceD3d11, dxvkReady, vkd3dReady);
                        if (outputApi.isEmpty() || !writeDgVoodooRuntimeConfig(stageTarget, outputApi)) {
                            routeState = "wined3d-fallback";
                            degradeReason = "dgvoodoo_config_write_failed";
                        } else {
                            routeState = vkd3dReady && !forceD3d11 ? "dgvoodoo+dxvk+vkd3d" : "dgvoodoo+dxvk";
                            envVars.put("AERO_DXWRAPPER_ACTIVE", routeState);
                        }
                    }
                }
            } else if (staged && !dgVoodooVulkanBridge) {
                routeState = "wined3d-fallback";
                degradeReason = "graphics_driver_no_vulkan_bridge:" + Container.normalizeGraphicsDriver(graphicsDriver);
            }

            envVars.put("AERO_DGVOODOO_STAGE_TARGET", stageTarget.getAbsolutePath());
            envVars.put("AERO_DGVOODOO_ARCH_ACTIVE", activeArch);
            envVars.put("AERO_DGVOODOO_PACKAGE_LANE", packageLane);
            envVars.put("AERO_DGVOODOO_STAGE_READY", staged ? "1" : "0");
            envVars.put("AERO_DGVOODOO_DXVK_WRAPPER", dxvkWrapper);
            envVars.put("AERO_DGVOODOO_VKD3D_WRAPPER", vkd3dWrapper);
            envVars.put("AERO_DGVOODOO_ROUTE_STATE", routeState);
            envVars.put("AERO_DGVOODOO_OUTPUT_API", outputApi);
            envVars.put("AERO_DGVOODOO_DEGRADE_REASON", degradeReason);
            if (!staged || !degradeReason.isEmpty()) {
                WineD3DConfigDialog.setEnvVars(this, config, envVars);
                envVars.put("AERO_DXWRAPPER_ACTIVE", "wined3d");
                Log.w(TAG, "dgVoodoo runtime stage failed for target " + stageTarget.getAbsolutePath()
                        + " (required lane: " + packageLane + ", arch: " + activeArch + ", degradeReason=" + degradeReason + ")");
            }
            ForensicLogger.logEvent(
                    this,
                    staged && degradeReason.isEmpty() ? "info" : "warn",
                    "DXWRAPPER_RUNTIME_STAGE_READY",
                    null,
                    "dxwrapper",
                    staged && degradeReason.isEmpty() ? "dgvoodoo_runtime_stage_ready" : "dgvoodoo_runtime_stage_failed",
                    ForensicLogger.fields(
                            "dxwrapper", dxwrapper,
                            "stage_target", stageTarget.getAbsolutePath(),
                            "package_lane", packageLane,
                            "arch_active", activeArch,
                            "stage_ready", staged ? "1" : "0",
                            "route_state", routeState,
                            "dxvk_wrapper", dxvkWrapper,
                            "dxvk_ready", dxvkReady ? "1" : "0",
                            "vkd3d_wrapper", vkd3dWrapper,
                            "vkd3d_ready", vkd3dReady ? "1" : "0",
                            "output_api", outputApi,
                            "degrade_reason", degradeReason
                    )
            );
        } else if (dxwrapper.contains("wined3d")) {
            Log.d(TAG, "Restoring original DLL files for wined3d.");
            restoreOriginalDllFiles(dlls);
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "DXWRAPPER_RUNTIME_STAGE_READY",
                    null,
                    "dxwrapper",
                    "wined3d_runtime_route_restored",
                    ForensicLogger.fields("dxwrapper", dxwrapper)
            );
        }
    }

    private String resolveConfiguredDxvkWrapper(KeyValueSet config) {
        return sanitizeConfiguredWrapperVersion(config.get("version"), DefaultVersion.DXVK);
    }

    private String resolveConfiguredDxvkWrapper(String config) {
        return resolveConfiguredDxvkWrapper(DXVKConfigDialog.parseConfig(config));
    }

    private String resolveConfiguredVkd3dWrapper(KeyValueSet config) {
        return sanitizeConfiguredWrapperVersion(config.get("vkd3dVersion"), "None");
    }

    private String resolveConfiguredVkd3dWrapper(String config) {
        return resolveConfiguredVkd3dWrapper(DXVKConfigDialog.parseConfig(config));
    }

    private String sanitizeConfiguredWrapperVersion(String value, String fallback) {
        if (AppUtils.isMissingComponentValue(value)) return fallback;
        if (value == null || value.trim().isEmpty()) return fallback;
        return value.trim();
    }

    private boolean stageDxvkCompanionPayload(String dxvkWrapper, File windowsDir) {
        if (dxvkWrapper == null || dxvkWrapper.trim().isEmpty()) return false;
        ContentProfile dxvkProfile = contentsManager.findInstalledProfileByVersion(
                ContentProfile.ContentType.CONTENT_TYPE_DXVK,
                dxvkWrapper,
                true
        );
        if (dxvkProfile != null) {
            Log.d(TAG, "Applying user-defined DXVK content profile: " + dxvkWrapper);
            boolean applied = contentsManager.applyContent(dxvkProfile);
            boolean verified = applied && verifyWindowsPayloadStage(
                    "dxvk",
                    dxvkWrapper,
                    windowsDir,
                    "d3d10.dll",
                    "d3d10_1.dll",
                    "d3d10core.dll",
                    "d3d11.dll",
                    "dxgi.dll"
            );
            if (verified) return true;
        }

        Log.d(TAG, "Extracting secondary DXVK .tzst archive: " + dxvkWrapper);
        boolean extracted = TarCompressorUtils.extract(
                TarCompressorUtils.Type.ZSTD,
                this,
                "dxwrapper/dxvk-" + dxvkWrapper + ".tzst",
                windowsDir,
                onExtractFileListener
        );
        if (!extracted) return false;

        if (compareVersion(dxvkWrapper, "2.4") < 0) {
            Log.d(TAG, "Extracting d8vk as part of DXVK version " + dxvkWrapper);
            TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "dxwrapper/d8vk-" + DefaultVersion.D8VK + ".tzst", windowsDir, onExtractFileListener);
        }
        return verifyWindowsPayloadStage(
                "dxvk",
                dxvkWrapper,
                windowsDir,
                "d3d10.dll",
                "d3d10_1.dll",
                "d3d10core.dll",
                "d3d11.dll",
                "dxgi.dll"
        );
    }

    private boolean stageVkd3dCompanionPayload(String vkd3dWrapper, File windowsDir) {
        if (vkd3dWrapper == null || vkd3dWrapper.trim().isEmpty() || "None".equalsIgnoreCase(vkd3dWrapper)) {
            return false;
        }
        ContentProfile vkd3dProfile = contentsManager.findInstalledProfileByVersion(
                ContentProfile.ContentType.CONTENT_TYPE_VKD3D,
                vkd3dWrapper,
                true
        );
        if (vkd3dProfile != null) {
            Log.d(TAG, "Applying user-defined VKD3D content profile: " + vkd3dWrapper);
            boolean applied = contentsManager.applyContent(vkd3dProfile);
            boolean verified = applied && verifyWindowsPayloadStage(
                    "vkd3d",
                    vkd3dWrapper,
                    windowsDir,
                    "d3d12.dll",
                    "d3d12core.dll"
            );
            if (verified) return true;
        }

        Log.d(TAG, "Extracting secondary VKD3D .tzst archive: " + vkd3dWrapper);
        boolean extracted = TarCompressorUtils.extract(
                TarCompressorUtils.Type.ZSTD,
                this,
                "dxwrapper/vkd3d-" + vkd3dWrapper + ".tzst",
                windowsDir,
                onExtractFileListener
        );
        return extracted && verifyWindowsPayloadStage(
                "vkd3d",
                vkd3dWrapper,
                windowsDir,
                "d3d12.dll",
                "d3d12core.dll"
        );
    }

    private boolean verifyWindowsPayloadStage(String owner, String version, File windowsDir, String... dllNames) {
        File system32Dir = new File(windowsDir, "system32");
        File syswow64Dir = new File(windowsDir, "syswow64");
        boolean checkSysWow64 = syswow64Dir.isDirectory();
        ArrayList<String> missing = new ArrayList<>();
        int checked = 0;

        for (String dllName : dllNames) {
            if (dllName == null || dllName.trim().isEmpty()) continue;
            checked++;
            File system32Dll = new File(system32Dir, dllName);
            if (!system32Dll.isFile() || system32Dll.length() == 0L) {
                addSample(missing, "system32/" + dllName);
            }
            if (checkSysWow64) {
                checked++;
                File syswow64Dll = new File(syswow64Dir, dllName);
                if (!syswow64Dll.isFile() || syswow64Dll.length() == 0L) {
                    addSample(missing, "syswow64/" + dllName);
                }
            }
        }

        boolean ready = missing.isEmpty();
        ForensicLogger.logEvent(
                this,
                ready ? "info" : "warn",
                "DXWRAPPER_PAYLOAD_STAGE_VERIFY",
                null,
                "dxwrapper",
                ready ? "dxwrapper_payload_stage_verified" : "dxwrapper_payload_stage_missing_files",
                ForensicLogger.fields(
                        "owner", owner == null ? "" : owner,
                        "version", version == null ? "" : version,
                        "windows_dir", windowsDir != null ? windowsDir.getAbsolutePath() : "",
                        "checked", checked,
                        "syswow64_checked", checkSysWow64 ? "1" : "0",
                        "missing_count", missing.size(),
                        "missing", String.join(",", missing)
                )
        );
        return ready;
    }

    private String resolveDgVoodooOutputApi(boolean forceD3d11, boolean dxvkReady, boolean vkd3dReady) {
        if (!dxvkReady) return "";
        if (forceD3d11 || !vkd3dReady) return "d3d11_fl11_0";
        return "bestavailable";
    }

    private boolean writeDgVoodooRuntimeConfig(File targetDir, String outputApi) {
        if (targetDir == null || !targetDir.isDirectory() || outputApi == null || outputApi.trim().isEmpty()) {
            return false;
        }
        File configFile = new File(targetDir, "dgVoodoo.conf");
        String current = configFile.isFile() ? FileUtils.readString(configFile) : "";
        String updated = upsertIniKey(current == null ? "" : current, "General", "OutputAPI", outputApi);
        return FileUtils.writeString(configFile, updated);
    }

    private String upsertIniKey(String source, String section, String key, String value) {
        String[] lines = source.isEmpty() ? new String[0] : source.split("\\r?\\n", -1);
        StringBuilder builder = new StringBuilder();
        boolean inSection = false;
        boolean sectionFound = false;
        boolean keyWritten = false;

        for (String line : lines) {
            String trimmed = line.trim();
            boolean sectionLine = trimmed.startsWith("[") && trimmed.endsWith("]");
            if (sectionLine) {
                if (inSection && !keyWritten) {
                    builder.append(key).append(" = ").append(value).append('\n');
                    keyWritten = true;
                }
                inSection = trimmed.equalsIgnoreCase("[" + section + "]");
                if (inSection) sectionFound = true;
                builder.append(line).append('\n');
                continue;
            }

            if (inSection && isIniKeyLine(trimmed, key)) {
                builder.append(key).append(" = ").append(value).append('\n');
                keyWritten = true;
                continue;
            }

            builder.append(line).append('\n');
        }

        if (!sectionFound) {
            if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '\n') builder.append('\n');
            builder.append("[")
                    .append(section)
                    .append("]")
                    .append('\n')
                    .append(key)
                    .append(" = ")
                    .append(value)
                    .append('\n');
        } else if (!keyWritten) {
            builder.append(key).append(" = ").append(value).append('\n');
        }

        return builder.toString();
    }

    private boolean isIniKeyLine(String trimmed, String key) {
        if (trimmed.isEmpty() || trimmed.startsWith(";") || trimmed.startsWith("#")) return false;
        int delimiter = trimmed.indexOf('=');
        if (delimiter < 0) delimiter = trimmed.indexOf(':');
        if (delimiter < 0) return false;
        String currentKey = trimmed.substring(0, delimiter).trim();
        return currentKey.equalsIgnoreCase(key);
    }

    private void applyWrapperGraphicsConfigEnv(
            File rootDir,
            String adrenoToolsDriverId,
            boolean useDRI3,
            boolean dri3ForceSwWsi,
            boolean dri3PresentWait
    ) {
        String requestedVulkanApi = normalizeRequestedVulkanApi(graphicsDriverConfig.get("vulkanVersion"));
        purgeLegacyVulkanRuntimeResidue(rootDir);
        File wrapperIcdFile = resolveWrapperIcdFile();
        int wrapperApiMinor = resolveWrapperIcdApiMinor(wrapperIcdFile);
        boolean wrapperIcdAvailable = wrapperIcdFile != null && wrapperIcdFile.isFile();
        File validationLayerManifest = new File(imageFs.getShareDir(), "vulkan/explicit_layer.d/VkLayer_khronos_validation.json");
        String wrapperRuntimeSource = !wrapperIcdAvailable
                ? "wrapper-missing"
                : wrapperIcdFile.getName().contains(".android-host.")
                ? "wrapper-host-native"
                : "wrapper-embedded";
        setOrClearEnv("AERO_VULKAN_RUNTIME_SOURCE", wrapperRuntimeSource);
        setOrClearEnv("AERO_VULKAN_WRAPPER_ICD", wrapperIcdAvailable ? wrapperIcdFile.getAbsolutePath() : "");
        setOrClearEnv("AERO_VULKAN_WRAPPER_API_MAX", wrapperApiMinor > 0 ? "1." + wrapperApiMinor : "");
        setOrClearEnv("AERO_VULKAN_API_MIN_AVAILABLE", "");
        setOrClearEnv("AERO_VULKAN_API_MAX_AVAILABLE", wrapperApiMinor > 0 ? "1." + wrapperApiMinor : "");
        setOrClearEnv("AERO_VULKAN_VALIDATION_LAYER_MANIFEST", validationLayerManifest.isFile() ? validationLayerManifest.getAbsolutePath() : "");
        envVars.put("AERO_VULKAN_API_SELECTED", requestedVulkanApi);
        envVars.put("WRAPPER_VK_VERSION", requestedVulkanApi + "." + resolveDriverVulkanPatch(adrenoToolsDriverId));

        String blacklistedExtensions = graphicsDriverConfig.get("blacklistedExtensions");
        envVars.put("WRAPPER_EXTENSION_BLACKLIST", blacklistedExtensions);

        String gpuName = graphicsDriverConfig.get("gpuName");
        if (!gpuName.equals("Device")) {
            envVars.put("WRAPPER_DEVICE_NAME", gpuName);
            envVars.put("WRAPPER_DEVICE_ID", WineD3DConfigDialog.getDeviceIdFromGPUName(this, gpuName));
            envVars.put("WRAPPER_VENDOR_ID", WineD3DConfigDialog.getVendorIdFromGPUName(this, gpuName));
        }

        String maxDeviceMemory = graphicsDriverConfig.get("maxDeviceMemory");
        if (maxDeviceMemory != null && Integer.parseInt(maxDeviceMemory) > 0) {
            envVars.put("WRAPPER_VMEM_MAX_SIZE", maxDeviceMemory);
        }

        String presentMode = graphicsDriverConfig.get("presentMode");
        if (presentMode.contains("immediate")) {
            envVars.put("WRAPPER_MAX_IMAGE_COUNT", "1");
        }
        envVars.put("MESA_VK_WSI_PRESENT_MODE", presentMode);

        String resourceType = graphicsDriverConfig.get("resourceType");
        envVars.put("WRAPPER_RESOURCE_TYPE", resourceType);

        String syncFrame = graphicsDriverConfig.get("syncFrame");
        if (syncFrame.equals("1") && !dri3ForceSwWsi && useDRI3) {
            envVars.put("MESA_VK_WSI_DEBUG", "forcesync");
        }

        String disablePresentWait = graphicsDriverConfig.get("disablePresentWait");
        if (!dri3PresentWait) disablePresentWait = "1";
        envVars.put("WRAPPER_DISABLE_PRESENT_WAIT", disablePresentWait);

        String bcnEmulation = graphicsDriverConfig.get("bcnEmulation");
        String bcnEmulationType = graphicsDriverConfig.get("bcnEmulationType");

        switch (bcnEmulation) {
            case "auto" -> {
                if (bcnEmulationType.equals("compute") && GPUInformation.getVendorID(null, null) != 0x5143) {
                    envVars.put("ENABLE_BCN_COMPUTE", "1");
                    envVars.put("BCN_COMPUTE_AUTO", "1");
                }
                envVars.put("WRAPPER_EMULATE_BCN", "3");
            }
            case "full" -> {
                if (bcnEmulationType.equals("compute") && GPUInformation.getVendorID(null, null) != 0x5143) {
                    envVars.put("ENABLE_BCN_COMPUTE", "1");
                    envVars.put("BCN_COMPUTE_AUTO", "0");
                }
                envVars.put("WRAPPER_EMULATE_BCN", "2");
            }
            case "none" -> envVars.put("WRAPPER_EMULATE_BCN", "0");
            default -> envVars.put("WRAPPER_EMULATE_BCN", "1");
        }

        String bcnEmulationCache = graphicsDriverConfig.get("bcnEmulationCache");
        envVars.put("WRAPPER_USE_BCN_CACHE", bcnEmulationCache);

        String wrapperGalliumDriver = GraphicsDrivers.getWrapperGalliumDriver(graphicsDriverConfig.get("galliumDriver"));
        envVars.put("GALLIUM_DRIVER", wrapperGalliumDriver);
        setOrClearEnv("AERO_OPENGL_GALLIUM_DRIVER", wrapperGalliumDriver);
        setOrClearEnv("AERO_WRAPPER_OPENGL_DRIVER", wrapperGalliumDriver);

        boolean zinkOpenGlRoute = GraphicsDrivers.isWrapperZinkOpenGlDriver(wrapperGalliumDriver);
        String mesaExtensionOverride = "";
        if (zinkOpenGlRoute) {
            ArrayList<String> disabledExtensions = new ArrayList<>();
            if (isEnabledConfigValue(graphicsDriverConfig.get("disableGLKHRDebug"), true)) {
                disabledExtensions.add("GL_KHR_debug");
            }
            if (isEnabledConfigValue(graphicsDriverConfig.get("disableVertexArrayBGRA"), true)) {
                disabledExtensions.add("GL_EXT_vertex_array_bgra");
            }
            for (String disabledExtension : disabledExtensions) {
                mesaExtensionOverride += (mesaExtensionOverride.isEmpty() ? "" : " ") + "-" + disabledExtension;
            }
        }
        setOrClearEnv("MESA_EXTENSION_OVERRIDE", mesaExtensionOverride);
        setOrClearEnv(
                "MESA_GL_VERSION_OVERRIDE",
                zinkOpenGlRoute
                        ? GraphicsDrivers.normalizeWrapperGlVersion(graphicsDriverConfig.get("glVersion"), wrapperGalliumDriver)
                        : ""
        );
    }

    private boolean isEnabledConfigValue(@Nullable String value, boolean fallback) {
        String normalized = trimToEmpty(value).toLowerCase(Locale.US);
        if (normalized.isEmpty()) return fallback;
        if ("1".equals(normalized) || "true".equals(normalized) || "t".equals(normalized)) return true;
        if ("0".equals(normalized) || "false".equals(normalized) || "f".equals(normalized)) return false;
        return fallback;
    }

    private void cleanupDgVoodooRuntimeStage(File rootDir) {
        if (rootDir == null || shortcut == null) return;
        DgVoodooManager manager = new DgVoodooManager(this);
        WineUtils.WindowsLaunchTarget launchTarget = resolveEffectiveShortcutLaunchTarget();
        if (launchTarget == null) launchTarget = resolveShortcutLaunchTarget(rootDir);
        File stageTarget = manager.resolveShortcutTargetDir(launchTarget);
        if (stageTarget != null) manager.cleanupStagedRuntime(stageTarget);
    }

    private String normalizeAdrenoGalliumDriver(String requestedDriver, String fallbackDriver) {
        String normalized = trimToEmpty(requestedDriver).toLowerCase(Locale.US);
        if ("freedreno".equals(normalized) || "zink".equals(normalized)) return normalized;
        return trimToEmpty(fallbackDriver).isEmpty() ? "zink" : fallbackDriver;
    }

    private String normalizeMaliGalliumDriver(String requestedDriver, String fallbackDriver) {
        String normalized = trimToEmpty(requestedDriver).toLowerCase(Locale.US);
        if ("panfrost".equals(normalized)
                || "lima".equals(normalized)
                || "zink".equals(normalized)
                || "softpipe".equals(normalized)) {
            return normalized;
        }
        String fallback = trimToEmpty(fallbackDriver).toLowerCase(Locale.US);
        return fallback.isEmpty() ? "panfrost" : fallback;
    }

    private static int compareVersion(String varA, String varB) {
        int[] a = parseSemverLoose(varA);
        int[] b = parseSemverLoose(varB);

        if (a[0] != b[0]) return a[0] - b[0];
        if (a[1] != b[1]) return a[1] - b[1];
        return a[2] - b[2];
    }

    private static final Pattern SEMVER_LOOSE =
            Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    private static int[] parseSemverLoose(String s) {
        if (s == null) return new int[]{0, 0, 0};

        Matcher m = SEMVER_LOOSE.matcher(s);

        String g1 = null, g2 = null, g3 = null;
        while (m.find()) {
            g1 = m.group(1);
            g2 = m.group(2);
            g3 = m.group(3);
        }

        if (g1 == null || g2 == null) {
            return new int[]{0, 0, 0};
        }

        int major = safeParseInt(g1);
        int minor = safeParseInt(g2);
        int patch = safeParseInt(g3);
        return new int[]{major, minor, patch};
    }

    private static int safeParseInt(String s) {
        if (s == null || s.isEmpty()) return 0;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void extractWinComponentFiles() {
        Log.d("XServerDisplayActivity", "Extracting WinComponents");
        File rootDir = imageFs.getRootDir();
        File prefixDir = WineUtils.resolveHostWinePrefixDir(rootDir);
        File windowsDir = new File(prefixDir, "drive_c/windows");
        File systemRegFile = new File(prefixDir, "system.reg");

        try {
            JSONObject wincomponentsJSONObject = new JSONObject(FileUtils.readString(this, "wincomponents/wincomponents.json"));
            ArrayList<String> dlls = new ArrayList<>();
            String wincomponents = shortcut != null ? shortcut.getExtra("wincomponents", container.getWinComponents()) : container.getWinComponents();

            Iterator<String[]> oldWinComponentsIter = new KeyValueSet(container.getExtra("wincomponents", Container.FALLBACK_WINCOMPONENTS)).iterator();

            for (String[] wincomponent : new KeyValueSet(wincomponents)) {
                if (wincomponent[1].equals(oldWinComponentsIter.next()[1]) && !firstTimeBoot) continue;
                String identifier = wincomponent[0];
                boolean useNative = wincomponent[1].equals("1");

                if (useNative) {
                    TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "wincomponents/"+identifier+".tzst", windowsDir, onExtractFileListener);
                }
                else {
                    JSONArray dlnames = wincomponentsJSONObject.getJSONArray(identifier);
                    for (int i = 0; i < dlnames.length(); i++) {
                        String dlname = dlnames.getString(i);
                        dlls.add(!dlname.endsWith(".exe") ? dlname+".dll" : dlname);
                    }
                }
                Log.d("XServerDisplayActivity", "Setting wincomponent " + identifier + " to " + String.valueOf(useNative));
                WineUtils.overrideWinComponentDlls(this, container, identifier, useNative);
                WineUtils.setWinComponentRegistryKeys(systemRegFile, identifier, useNative, this);
            }

            if (!dlls.isEmpty()) restoreOriginalDllFiles(dlls.toArray(new String[0]));
        }
        catch (JSONException e) {
            Log.w("XServerDisplayActivity", "Failed to restore original DLL files from wincomponent config", e);
        }
    }

    private boolean syncOpenAlRuntimeDlls() {
        String dllOverrides = resolveRequestedDllOverrides();
        String normalizedDllOverrides = dllOverrides.toLowerCase(Locale.US);
        boolean needsOpenAlDlls = normalizedDllOverrides.contains("openal32")
                || normalizedDllOverrides.contains("soft_oal");
        String openAlState = needsOpenAlDlls ? "yes" : "no";
        if (openAlState.equals(container.getExtra("openal_dlls")) && !firstTimeBoot) {
            return false;
        }

        boolean extracted = false;
        String reason = "";
        if (needsOpenAlDlls) {
            if (FileUtils.getSize(this, "wincomponents/openal.tzst") > 0) {
                File rootDir = imageFs.getRootDir();
                File prefixDir = WineUtils.resolveHostWinePrefixDir(rootDir);
                File windowsDir = new File(prefixDir, "drive_c/windows");
                extracted = TarCompressorUtils.extract(
                        TarCompressorUtils.Type.ZSTD,
                        this,
                        "wincomponents/openal.tzst",
                        windowsDir,
                        onExtractFileListener
                );
            } else {
                reason = "embedded_openal_asset_missing";
            }
        }

        container.putExtra("openal_dlls", openAlState);
        ForensicLogger.logEvent(
                this,
                needsOpenAlDlls && !extracted ? "warn" : "info",
                "OPENAL_RUNTIME_DLL_SYNC",
                null,
                "wine_components",
                "openal_runtime_dll_sync",
                ForensicLogger.fields(
                        "requested", needsOpenAlDlls,
                        "state", openAlState,
                        "extracted", extracted,
                        "reason", reason,
                        "dll_overrides", dllOverrides
                )
        );
        return true;
    }

    private String resolveRequestedDllOverrides() {
        EnvVars requestedEnv = new EnvVars();
        if (container != null) requestedEnv.putAll(container.getEnvVars());
        if (shortcut != null) {
            String shortcutEnv = shortcut.getExtra("envVars");
            if (shortcutEnv != null && !shortcutEnv.trim().isEmpty()) {
                requestedEnv.putAll(shortcutEnv);
            }
        }
        String dllOverrides = requestedEnv.get("WINEDLLOVERRIDES");
        return dllOverrides == null ? "" : dllOverrides;
    }

    private void restoreOriginalDllFiles(final String... dlls) {
        File rootDir = imageFs.getRootDir();
        File windowsDir = new File(WineUtils.resolveHostWinePrefixDir(rootDir), "drive_c/windows");
        File runtimeWineLibDir = WineUtils.resolveRuntimeWineLibDir(new File(imageFs.getWinePath()));
        if (runtimeWineLibDir == null) return;
        File system32dlls = wineInfo.usesAarch64WindowsTree()
                ? new File(runtimeWineLibDir, "aarch64-windows")
                : new File(runtimeWineLibDir, "x86_64-windows");
        File syswow64dlls = new File(runtimeWineLibDir, "i386-windows");


        int restored = 0;
        int missingSource = 0;
        int failed = 0;
        ArrayList<String> samples = new ArrayList<>();
        for (String dll : dlls) {
            File srcFile = new File(system32dlls, dll);
            File dstFile = new File(windowsDir, "system32/" + dll);
            if (copyRuntimeDllWithVerification(srcFile, dstFile)) restored++;
            else {
                if (!srcFile.isFile()) missingSource++;
                else failed++;
                addSample(samples, "system32/" + dll);
            }
            srcFile = new File(syswow64dlls, dll);
            dstFile = new File(windowsDir, "syswow64/" + dll);
            if (copyRuntimeDllWithVerification(srcFile, dstFile)) restored++;
            else {
                if (!srcFile.isFile()) missingSource++;
                else failed++;
                addSample(samples, "syswow64/" + dll);
            }
        }

        ForensicLogger.logEvent(
                this,
                missingSource == 0 && failed == 0 ? "info" : "warn",
                "DXWRAPPER_ORIGINAL_DLL_RESTORE",
                null,
                "dxwrapper",
                missingSource == 0 && failed == 0 ? "original_dll_restore_complete" : "original_dll_restore_incomplete",
                ForensicLogger.fields(
                        "runtime_wine_lib_dir", runtimeWineLibDir.getAbsolutePath(),
                        "windows_dir", windowsDir.getAbsolutePath(),
                        "requested", dlls != null ? dlls.length : 0,
                        "restored", restored,
                        "missing_source", missingSource,
                        "failed", failed,
                        "sample_count", samples.size(),
                        "samples", String.join(" | ", samples)
                )
        );
   }

    private boolean copyRuntimeDllWithVerification(File sourceFile, File targetFile) {
        if (sourceFile == null || targetFile == null || !sourceFile.isFile()) return false;
        return FileUtils.copy(sourceFile, targetFile)
                && targetFile.isFile()
                && targetFile.length() == sourceFile.length();
    }

    private void addSample(ArrayList<String> samples, String sample) {
        if (samples == null || sample == null || sample.trim().isEmpty()) return;
        if (samples.size() < 12) samples.add(sample);
    }

    @Nullable
    private WineUtils.WindowsLaunchTarget resolveEffectiveShortcutLaunchTarget() {
        if (shortcut == null) return null;

        File rootDir = imageFs != null ? imageFs.getRootDir() : null;
        if (rootDir == null) return resolveShortcutLaunchTarget(null);
        if (effectiveShortcutLaunchTarget != null) return effectiveShortcutLaunchTarget;

        WineUtils.WindowsLaunchTarget launchTarget = resolveShortcutLaunchTarget(rootDir);
        effectiveShortcutLaunchTarget = resolveNoexecMirroredLaunchTarget(rootDir, launchTarget);
        return effectiveShortcutLaunchTarget;
    }

    @NonNull
    private WineUtils.WindowsLaunchTarget resolveShortcutLaunchTarget(@Nullable File rootDir) {
        return WineUtils.resolveWindowsLaunchTarget(rootDir, resolveShortcutCommandSpec());
    }

    @NonNull
    private String resolveShortcutCommandSpec() {
        if (shortcut == null) return "";
        if (shortcut.file != null && shortcut.file.isFile()) {
            for (String line : FileUtils.readLines(shortcut.file)) {
                if (line == null) continue;
                String trimmed = line.trim();
                if (!trimmed.startsWith("Exec=")) continue;
                String execPayload = WineUtils.extractWineExecPayload(trimmed.substring(5));
                if (!execPayload.isEmpty()) return execPayload;
            }
        }
        return shortcut.path == null ? "" : shortcut.path;
    }

    @NonNull
    private WineUtils.WindowsLaunchTarget resolveNoexecMirroredLaunchTarget(
            @Nullable File rootDir,
            @NonNull WineUtils.WindowsLaunchTarget launchTarget
    ) {
        if (rootDir == null
                || !launchTarget.hasCommandPath()
                || launchTarget.isShortcutLink()
                || launchTarget.hostTargetDir == null
                || launchTarget.hostTargetFile == null) {
            return launchTarget;
        }
        if (!shouldMirrorNoexecLaunchTarget(rootDir, launchTarget.hostTargetDir)) return launchTarget;

        File mirrorDir = materializeNoexecLaunchMirror(rootDir, launchTarget.hostTargetDir, launchTarget.hostTargetFile);
        if (mirrorDir == null) {
            ForensicLogger.logEvent(
                    this,
                    "warn",
                    "NOEXEC_LAUNCH_MIRROR_FAILED",
                    null,
                    "xserver",
                    "noexec_launch_mirror_failed",
                    ForensicLogger.fields(
                            "source_host_dir", launchTarget.hostTargetDir.getAbsolutePath(),
                            "source_host_file", launchTarget.hostTargetFile.getAbsolutePath()
                    )
            );
            return launchTarget;
        }

        File mirroredTargetFile = new File(mirrorDir, launchTarget.hostTargetFile.getName());
        if (!mirroredTargetFile.isFile()) return launchTarget;

        WineUtils.WindowsLaunchTarget mirroredTarget =
                WineUtils.remapWindowsLaunchTarget(rootDir, launchTarget, mirroredTargetFile);
        if (mirroredTarget == null || !mirroredTarget.hasCommandPath()) return launchTarget;

        ForensicLogger.logEvent(
                this,
                "info",
                "NOEXEC_LAUNCH_MIRROR_APPLIED",
                null,
                "xserver",
                "noexec_launch_mirror_applied",
                ForensicLogger.fields(
                        "source_host_dir", launchTarget.hostTargetDir.getAbsolutePath(),
                        "mirror_host_dir", mirrorDir.getAbsolutePath(),
                        "source_command_path", launchTarget.commandPath,
                        "mirror_command_path", mirroredTarget.commandPath
                )
        );
        return mirroredTarget;
    }

    private boolean shouldMirrorNoexecLaunchTarget(@Nullable File rootDir, @Nullable File hostTargetDir) {
        String canonicalPath = canonicalPath(hostTargetDir);
        if (canonicalPath.isEmpty()) return false;
        if (isPathInside(canonicalPath, canonicalPath(rootDir))) return false;
        if (isPathInside(canonicalPath, canonicalPath(getFilesDir()))) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && isPathInside(canonicalPath, canonicalPath(getDataDir()))) {
            return false;
        }

        MountInfo mountInfo = findBestMountInfo(canonicalPath);
        if (mountInfo != null && mountInfo.hasOption("noexec")) return true;
        return isKnownAndroidSharedStoragePath(canonicalPath);
    }

    @Nullable
    private File materializeNoexecLaunchMirror(File rootDir, File sourceDir, File sourceFile) {
        File driveCRoot = WineUtils.resolveHostWineDriveCRoot(rootDir);
        File mirrorBaseDir = new File(driveCRoot, NOEXEC_LAUNCH_MIRROR_DIR);
        if (!mirrorBaseDir.isDirectory() && !mirrorBaseDir.mkdirs()) return null;

        File mirrorDir = new File(
                mirrorBaseDir,
                buildNoexecLaunchMirrorId(sourceDir) + "-" + sanitizeLaunchMirrorLeaf(sourceDir.getName())
        );
        if (isFreshNoexecLaunchMirror(mirrorDir, sourceDir, sourceFile)) return mirrorDir;

        if (mirrorDir.exists() && !FileUtils.delete(mirrorDir)) return null;
        if (!FileUtils.copy(sourceDir, mirrorDir)) return null;

        File mirroredTargetFile = new File(mirrorDir, sourceFile.getName());
        if (!mirroredTargetFile.isFile()) return null;

        writeNoexecLaunchMirrorStamp(mirrorDir, sourceDir, sourceFile);
        return mirrorDir;
    }

    private boolean isFreshNoexecLaunchMirror(File mirrorDir, File sourceDir, File sourceFile) {
        if (!mirrorDir.isDirectory()) return false;
        File mirroredTargetFile = new File(mirrorDir, sourceFile.getName());
        if (!mirroredTargetFile.isFile()) return false;

        File stampFile = new File(mirrorDir, NOEXEC_LAUNCH_MIRROR_STAMP);
        if (!stampFile.isFile()) return false;

        try {
            String stampString = FileUtils.readString(stampFile);
            if (stampString == null || stampString.trim().isEmpty()) return false;
            JSONObject stamp = new JSONObject(stampString);
            return canonicalPath(sourceDir).equals(stamp.optString("source_dir"))
                    && sourceFile.getName().equals(stamp.optString("source_file"))
                    && sourceFile.length() == stamp.optLong("source_file_size", -1L)
                    && sourceFile.lastModified() == stamp.optLong("source_file_mtime", -1L);
        } catch (JSONException e) {
            return false;
        }
    }

    private void writeNoexecLaunchMirrorStamp(File mirrorDir, File sourceDir, File sourceFile) {
        try {
            JSONObject stamp = new JSONObject();
            stamp.put("source_dir", canonicalPath(sourceDir));
            stamp.put("source_file", sourceFile.getName());
            stamp.put("source_file_size", sourceFile.length());
            stamp.put("source_file_mtime", sourceFile.lastModified());
            FileUtils.writeString(new File(mirrorDir, NOEXEC_LAUNCH_MIRROR_STAMP), stamp.toString());
        } catch (JSONException ignored) {
        }
    }

    private String buildNoexecLaunchMirrorId(File sourceDir) {
        String canonicalPath = canonicalPath(sourceDir);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonicalPath.getBytes(StandardCharsets.UTF_8));
            return hexPrefix(hash, 8);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(canonicalPath.hashCode());
        }
    }

    private String sanitizeLaunchMirrorLeaf(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) return "shortcut";
        return normalized.replaceAll("[^A-Za-z0-9._-]+", "_");
    }

    private String hexPrefix(byte[] bytes, int byteCount) {
        if (bytes == null || bytes.length == 0 || byteCount <= 0) return "mirror";
        StringBuilder builder = new StringBuilder(byteCount * 2);
        int limit = Math.min(bytes.length, byteCount);
        for (int i = 0; i < limit; i++) {
            builder.append(String.format(Locale.US, "%02x", bytes[i] & 0xff));
        }
        return builder.toString();
    }

    private String canonicalPath(@Nullable File file) {
        if (file == null) return "";
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }

    private String canonicalLowerPath(@Nullable File file) {
        return canonicalPath(file).toLowerCase(Locale.US);
    }

    private boolean isKnownAndroidSharedStoragePath(String path) {
        String normalized = path == null ? "" : path.toLowerCase(Locale.US);
        return isPathInside(normalized, "/storage/emulated")
                || isPathInside(normalized, "/storage/self/primary")
                || isPathInside(normalized, "/sdcard")
                || isPathInside(normalized, "/mnt/sdcard")
                || isPathInside(normalized, "/mnt/runtime/default/emulated")
                || isPathInside(normalized, "/mnt/runtime/read/emulated")
                || isPathInside(normalized, "/mnt/runtime/write/emulated")
                || isPathInside(normalized, "/mnt/user/0/primary")
                || isPathInside(normalized, "/mnt/media_rw");
    }

    private boolean isPathInside(String path, String root) {
        if (path == null || root == null || path.isEmpty() || root.isEmpty()) return false;
        String normalizedPath = stripTrailingSlashes(path);
        String normalizedRoot = stripTrailingSlashes(root);
        return normalizedPath.equals(normalizedRoot) || normalizedPath.startsWith(normalizedRoot + "/");
    }

    private String stripTrailingSlashes(String value) {
        if (value == null) return "";
        String normalized = value.trim();
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    @Nullable
    private MountInfo findBestMountInfo(String path) {
        if (path == null || path.isEmpty()) return null;
        MountInfo best = null;
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/mounts"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\s+");
                if (fields.length < 4) continue;
                String mountPoint = decodeMountToken(fields[1]);
                if (mountPoint.isEmpty() || !isPathInside(path, mountPoint)) continue;
                if (best == null || mountPoint.length() > best.mountPoint.length()) {
                    best = new MountInfo(mountPoint, fields[3]);
                }
            }
        } catch (IOException ignored) {
        }
        return best;
    }

    private String decodeMountToken(String token) {
        if (token == null || token.isEmpty()) return "";
        return token
                .replace("\\040", " ")
                .replace("\\011", "\t")
                .replace("\\012", "\n")
                .replace("\\134", "\\");
    }

    private static final class MountInfo {
        final String mountPoint;
        final String options;

        MountInfo(String mountPoint, String options) {
            this.mountPoint = mountPoint == null ? "" : mountPoint;
            this.options = options == null ? "" : options;
        }

        boolean hasOption(String option) {
            if (option == null || option.isEmpty()) return false;
            String[] parts = options.split(",");
            for (String part : parts) {
                if (option.equals(part)) return true;
            }
            return false;
        }
    }

    private String buildGuestExecutable() {
        if (shortcut == null) {
            if (shouldUseDirectDesktopShellBootstrap()) {
                desktopShellLaunchMode = DESKTOP_SHELL_LAUNCH_MODE_DIRECT_EXPLORER;
                return buildDirectDesktopShellGuestExecutable(getOverrideEnvVars());
            }
            if (shouldUseWinHandlerDesktopShellBootstrap()) {
                desktopShellLaunchMode = DESKTOP_SHELL_LAUNCH_MODE_WINHANDLER;
                return buildDesktopShellWinHandlerGuestExecutable();
            }
        }
        return buildExplorerHostedDesktopShellGuestExecutable(getWineStartCommand());
    }

    private String buildDirectDesktopShellGuestExecutable(EnvVars envVars) {
        String shellExecutable = buildDesktopShellStartArgs(envVars);
        return buildExplorerHostedDesktopShellGuestExecutable(shellExecutable);
    }

    private String buildDesktopShellWinHandlerGuestExecutable() {
        String resolvedHandlerCommand = resolveContainerShellExecutableCommand("winhandler.exe");
        String resolvedShellCommand = resolveContainerShellExecutableCommand("wfm.exe");
        String handlerCommand = WineUtils.canonicalDesktopShellExecutableName(resolvedHandlerCommand, "winhandler.exe");
        String shellCommand = WineUtils.canonicalDesktopShellExecutableName(resolvedShellCommand, "wfm.exe");
        String bridgeCommand = buildWinHandlerShellCommand(handlerCommand, shellCommand);
        String guestExecutable = buildExplorerHostedDesktopShellGuestExecutable(bridgeCommand);
        ForensicLogger.logEvent(
                this,
                "info",
                "DESKTOP_SHELL_ROUTE_SELECTED",
                null,
                "xserver",
                "desktop_shell_route_selected",
                ForensicLogger.fields(
                        "shell_executable", "wfm.exe",
                        "shell_command", shellCommand,
                        "resolved_shell_command", resolvedShellCommand,
                        "handler_command", handlerCommand,
                        "resolved_handler_command", resolvedHandlerCommand,
                        "desktop_shell_launch_mode", desktopShellLaunchMode,
                        "wfm_present", hasContainerShellExecutable("wfm.exe"),
                        "explorer_present", hasContainerShellExecutable("explorer.exe"),
                        "container_variant", container != null ? container.getContainerVariant() : "",
                        "wine_version", container != null ? container.getWineVersion() : "",
                        "desktop_hosted", true,
                        "donor_compat_route", "explorer /desktop=shell winhandler.exe \"wfm.exe\"",
                        "execution_route", "wine_explorer_desktop_winhandler_bridge",
                        "guest_executable", guestExecutable
                )
        );
        return guestExecutable;
    }

    private String buildExplorerHostedDesktopShellGuestExecutable(String payloadCommand) {
        return WineUtils.buildExplorerDesktopShellCommand(String.valueOf(xServer.screenInfo), payloadCommand);
    }

    private String buildDesktopShellWinHandlerFallbackExecutable() {
        return buildDesktopShellWinHandlerGuestExecutable();
    }

    private String getWineStartCommand() {
        // Initialize overrideEnvVars if not already done
        EnvVars envVars = getOverrideEnvVars();

        // Define default arguments
        String args = "";

        if (shortcut != null) {
            WineUtils.WindowsLaunchTarget launchTarget = resolveEffectiveShortcutLaunchTarget();
            if (launchTarget == null) launchTarget = resolveShortcutLaunchTarget(null);
            String shortcutCommandPath = launchTarget.commandPath;
            if (shortcutCommandPath.isEmpty()) shortcutCommandPath = shortcut.path;
            String shortcutInlineArgs = launchTarget.commandArgs;
            String combinedExecArgs = container != null ? container.getExecArgs() : "";
            if (!shortcutInlineArgs.isEmpty()) {
                combinedExecArgs = combinedExecArgs.isEmpty() ? shortcutInlineArgs : shortcutInlineArgs + " " + combinedExecArgs;
            }
            String shortcutExecArgs = shortcut.getExtra("execArgs");
            if (!shortcutExecArgs.isEmpty()) {
                combinedExecArgs = combinedExecArgs.isEmpty() ? shortcutExecArgs : combinedExecArgs + " " + shortcutExecArgs;
            }
            String execArgs = !combinedExecArgs.trim().isEmpty() ? " " + combinedExecArgs.trim() : "";

            if (launchTarget.isShortcutLink()) {
                args += "\"" + shortcutCommandPath + "\"" + execArgs;
            } else {
                String exeDir = launchTarget.workingDir;
                String filename = launchTarget.getExecutableName();
                if (filename.isEmpty()) filename = FileUtils.getName(shortcutCommandPath);

                args += "/dir " + StringUtils.escapeDOSPath(exeDir) + " \"" + filename + "\"" + execArgs;
            }
        } else {
            args += buildDesktopShellStartArgs(envVars);
            if (!desktopShellRequiresWinHandler()) {
                return args;
            }
        }
        return "winhandler.exe " + args;
    }

    private String buildDesktopShellStartArgs(EnvVars envVars) {
        String shellExecutable = resolveDesktopShellExecutable();
        String resolvedShellCommand = resolveContainerShellExecutableCommand(shellExecutable);
        String shellCommand = WineUtils.canonicalDesktopShellExecutableName(resolvedShellCommand, shellExecutable);
        desktopShellLaunchMode = shortcut == null && shouldUseDirectDesktopShellBootstrap()
                ? DESKTOP_SHELL_LAUNCH_MODE_DIRECT_EXPLORER
                : DESKTOP_SHELL_LAUNCH_MODE_WINHANDLER;
        if (envVars != null && envVars.has("EXTRA_EXEC_ARGS")) {
            String staleExtraExecArgs = envVars.get("EXTRA_EXEC_ARGS");
            envVars.remove("EXTRA_EXEC_ARGS");
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "DESKTOP_SHELL_EXTRA_EXEC_IGNORED",
                    null,
                    "xserver",
                    "desktop_shell_extra_exec_ignored",
                    ForensicLogger.fields(
                            "requested_shell", staleExtraExecArgs == null ? "" : staleExtraExecArgs,
                            "resolved_shell", shellExecutable,
                            "resolved_shell_command", resolvedShellCommand,
                            "desktop_payload_shell_command", shellCommand
                    )
            );
        }
        ForensicLogger.logEvent(
                this,
                "info",
                "DESKTOP_SHELL_ROUTE_SELECTED",
                null,
                "xserver",
                "desktop_shell_route_selected",
                ForensicLogger.fields(
                        "shell_executable", shellExecutable,
                        "shell_command", shellCommand,
                        "resolved_shell_command", resolvedShellCommand,
                        "desktop_shell_launch_mode", desktopShellLaunchMode,
                        "wfm_present", hasContainerShellExecutable("wfm.exe"),
                        "explorer_present", hasContainerShellExecutable("explorer.exe"),
                        "container_variant", container != null ? container.getContainerVariant() : "",
                        "wine_version", container != null ? container.getWineVersion() : ""
                )
        );
        return WineUtils.buildExplorerDesktopShellPayload(shellCommand);
    }

    private void configureDesktopShellRegistry() {
        if (container == null || xServer == null) return;
        File userRegFile = new File(WineUtils.resolveHostWinePrefixDir(container.getRootDir()), "user.reg");
        String desktopName = "shell";
        String desktopGeometry = String.valueOf(xServer.screenInfo);
        try (WineRegistryEditor registryEditor = new WineRegistryEditor(userRegFile)) {
            registryEditor.setStringValue("Software\\Wine\\Explorer", "Desktop", desktopName);
            registryEditor.setStringValue("Software\\Wine\\Explorer\\Desktops", desktopName, desktopGeometry);
            registryEditor.setDwordValue("Software\\Wine\\Explorer\\Desktops\\" + desktopName, "EnableShell", 1);
            registryEditor.setDwordValue("Software\\Wine\\Explorer\\Desktops\\" + desktopName, "ShowSystray", 1);
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "DESKTOP_SHELL_REGISTRY_APPLIED",
                    null,
                    "xserver",
                    "desktop_shell_registry_applied",
                    ForensicLogger.fields(
                            "desktop_name", desktopName,
                            "desktop_geometry", desktopGeometry,
                            "shortcut_launch", shortcut != null
                    )
            );
        } catch (Exception e) {
            Log.w(TAG, "Failed to configure desktop shell registry", e);
            ForensicLogger.logEvent(
                    this,
                    "warn",
                    "DESKTOP_SHELL_REGISTRY_FAILED",
                    null,
                    "xserver",
                    "desktop_shell_registry_failed",
                    ForensicLogger.fields(
                            "desktop_name", desktopName,
                            "desktop_geometry", desktopGeometry,
                            "error", e.getMessage() == null ? "" : e.getMessage()
                    )
            );
        }
    }

    private void ensureBionicGraphicsDriverRegistry() {
        if (container == null) return;
        if (!Container.BIONIC.equalsIgnoreCase(container.getContainerVariant())) return;

        try {
            File runtimeRootDir = wineInfo != null && wineInfo.path != null ? new File(wineInfo.path) : null;
            String preferredGraphicsDriver = WineUtils.resolvePreferredGraphicsDriver(runtimeRootDir, wineInfo);
            if (preferredGraphicsDriver.isEmpty()) {
                ForensicLogger.logEvent(
                        this,
                        "info",
                        "BIONIC_GRAPHICS_DRIVER_REGISTRY_SKIPPED",
                        null,
                        "xserver",
                        "bionic_graphics_driver_registry_skipped",
                        ForensicLogger.fields(
                                "container_id", container.id,
                                "runtime_root", runtimeRootDir == null ? "" : runtimeRootDir.getPath(),
                                "runtime_profile", selectedRuntimeProfile == null ? "" : ContentsManager.getEntryName(selectedRuntimeProfile),
                                "reason", "no_runtime_driver_surface"
                        )
                );
                return;
            }

            WineUtils.ensureGraphicsDriverRegistry(container.getRootDir(), preferredGraphicsDriver);
            boolean x11OpenGlBackendContractApplied = WineUtils.graphicsDriverIncludesX11(preferredGraphicsDriver)
                    && WineUtils.ensureX11OpenGlBackendRegistry(container.getRootDir(), true);
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "BIONIC_GRAPHICS_DRIVER_REGISTRY_APPLIED",
                    null,
                    "xserver",
                    "bionic_graphics_driver_registry_applied",
                    ForensicLogger.fields(
                            "container_id", container.id,
                            "graphics_driver", preferredGraphicsDriver,
                            "x11_use_egl", x11OpenGlBackendContractApplied ? "N" : "",
                            "x11_force_glx_registry", x11OpenGlBackendContractApplied,
                            "runtime_root", runtimeRootDir == null ? "" : runtimeRootDir.getPath(),
                            "runtime_profile", selectedRuntimeProfile == null ? "" : ContentsManager.getEntryName(selectedRuntimeProfile)
                    )
            );
        } catch (Exception e) {
            Log.w(TAG, "Failed to apply bionic graphics driver registry baseline", e);
            ForensicLogger.logEvent(
                    this,
                    "warn",
                    "BIONIC_GRAPHICS_DRIVER_REGISTRY_FAILED",
                    null,
                    "xserver",
                    "bionic_graphics_driver_registry_failed",
                    ForensicLogger.fields(
                            "container_id", container.id,
                            "runtime_root", (wineInfo == null || wineInfo.path == null) ? "" : wineInfo.path,
                            "error", e.getMessage() == null ? "" : e.getMessage()
                    )
            );
        }
    }

    private String getExecutable() {
        String filename = "";
        if (shortcut != null) {
            WineUtils.WindowsLaunchTarget launchTarget = resolveEffectiveShortcutLaunchTarget();
            if (launchTarget == null) launchTarget = resolveShortcutLaunchTarget(null);
            filename = launchTarget.getExecutableName();
            if (filename.isEmpty()) filename = FileUtils.getName(shortcut.path);
        }
        else {
            filename = resolveDesktopShellExecutable();
        }
        return filename;
    }

    private String resolveDesktopShellExecutable() {
        if (!shouldUseDirectDesktopShellBootstrap() && hasContainerShellExecutable("wfm.exe")) return "wfm.exe";
        return "explorer.exe";
    }

    private String resolveContainerShellExecutableCommand(String executableName) {
        String dosPath = resolveContainerShellExecutableDosPath(executableName);
        return dosPath.isEmpty() ? executableName : dosPath;
    }

    private String resolveContainerShellExecutableDosPath(String executableName) {
        if (container == null || executableName == null || executableName.isEmpty()) return "";
        File rootDir = container.getRootDir();
        if (rootDir == null) return "";
        File windowsDir = new File(WineUtils.resolveHostWineDriveCRoot(rootDir), "windows");

        File[] candidates = new File[] {
                new File(windowsDir, executableName),
                new File(windowsDir, "system32/" + executableName),
                new File(windowsDir, "syswow64/" + executableName)
        };
        String[] dosCandidates = new String[] {
                "C:\\windows\\" + executableName,
                "C:\\windows\\system32\\" + executableName,
                "C:\\windows\\syswow64\\" + executableName
        };
        for (int i = 0; i < candidates.length; i++) {
            if (candidates[i].isFile()) return dosCandidates[i];
        }
        return "";
    }

    private boolean shouldUseDirectDesktopShellBootstrap() {
        if (shortcut != null) return false;
        if (container == null || wineInfo == null) return false;
        if (!wineInfo.isArm64EC()) return false;
        return Container.BIONIC.equalsIgnoreCase(container.getContainerVariant());
    }

    private boolean shouldUseWinHandlerDesktopShellBootstrap() {
        return shortcut == null
                && container != null
                && wineInfo != null
                && wineInfo.isArm64EC()
                && hasContainerShellExecutable("winhandler.exe")
                && hasContainerShellExecutable("wfm.exe");
    }

    private boolean isDesktopShellGuestExecutable(String guestExecutable) {
        if (guestExecutable == null) return false;
        String lowered = guestExecutable.trim().toLowerCase(Locale.ROOT);
        if (lowered.isEmpty()) return false;
        if (lowered.contains("explorer /desktop=shell")
                || lowered.contains("explorer.exe /desktop=shell")) {
            return true;
        }
        return lowered.startsWith("wine winhandler.exe")
                || lowered.startsWith("wine \"winhandler.exe\"")
                || lowered.startsWith("wine c:\\windows\\winhandler.exe")
                || lowered.startsWith("wine \"c:\\windows\\winhandler.exe\"")
                || lowered.contains(" winhandler.exe \"wfm.exe\"")
                || lowered.contains("\\winhandler.exe\" \"")
                || lowered.contains("\\winhandler.exe ");
    }

    private String buildWinHandlerShellCommand(String handlerExecutable, String shellExecutable) {
        return WineUtils.buildWinHandlerDesktopShellPayload(handlerExecutable, shellExecutable);
    }

    private boolean desktopShellRequiresWinHandler() {
        return !DESKTOP_SHELL_LAUNCH_MODE_DIRECT_EXPLORER.equals(desktopShellLaunchMode);
    }

    private boolean hasContainerShellExecutable(String executableName) {
        if (container == null || executableName == null || executableName.isEmpty()) return false;
        File rootDir = container.getRootDir();
        if (rootDir == null) return false;
        File windowsDir = new File(WineUtils.resolveHostWineDriveCRoot(rootDir), "windows");

        File[] candidates = new File[] {
                new File(windowsDir, executableName),
                new File(windowsDir, "system32/" + executableName),
                new File(windowsDir, "syswow64/" + executableName)
        };
        for (File candidate : candidates) {
            if (candidate.isFile()) return true;
        }
        return false;
    }


    public XServer getXServer() {
        return xServer;
    }

    public WinHandler getWinHandler() {
        return winHandler;
    }

    public XServerView getXServerView() {
        return xServerView;
    }

    public Container getContainer() {
        return container;
    }

    public void setDXWrapper(String dxwrapper) {
        this.dxwrapper = dxwrapper;
    }

    public EnvVars getOverrideEnvVars() {
        if (overrideEnvVars == null) {
            overrideEnvVars = new EnvVars();
        }
        return overrideEnvVars;
    }

    private void changeWineAudioDriver() {
        if (!audioDriver.equals(container.getExtra("audioDriver"))) {
            File userRegFile = new File(WineUtils.resolveHostWinePrefixDir(imageFs.getRootDir()), "user.reg");
            try (WineRegistryEditor registryEditor = new WineRegistryEditor(userRegFile)) {
                if (audioDriver.equals("alsa")) {
                    registryEditor.setStringValue("Software\\Wine\\Drivers", "Audio", "alsa");
                }
                else if (audioDriver.equals("pulseaudio")) {
                    registryEditor.setStringValue("Software\\Wine\\Drivers", "Audio", "pulse");
                }
            }
            container.putExtra("audioDriver", audioDriver);
            container.saveData();
        }
    }

    private void applyGeneralPatches(Container container) {
        File rootDir = imageFs.getRootDir();
        boolean glibcVariant = container != null && Container.GLIBC.equalsIgnoreCase(container.getContainerVariant());
        if (glibcVariant) {
            if (!ImageFsInstaller.extractSupportArchive(this, "imagefs_patches_gamenative.tzst", TarCompressorUtils.Type.ZSTD, rootDir)) {
                Log.w("XServerDisplayActivity", "Missing glibc runtime patch archive: imagefs_patches_gamenative.tzst");
            }
            if (!ImageFsInstaller.extractSupportArchive(this, "extras.tzst", TarCompressorUtils.Type.ZSTD, rootDir)) {
                Log.w("XServerDisplayActivity", "Missing extras overlay archive: extras.tzst");
            }
            if (!TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "pulseaudio-gamenative.tzst", new File(getFilesDir(), "pulseaudio"))) {
                TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "pulseaudio.tzst", new File(getFilesDir(), "pulseaudio"));
            }
        } else {
            TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "container_pattern_common.tzst", rootDir);
            TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "pulseaudio.tzst", new File(getFilesDir(), "pulseaudio"));
        }
        WineUtils.applySystemTweaks(this, wineInfo);
        container.putExtra("graphicsDriver", null);
        container.putExtra("desktopTheme", null);
    }

    private void assignTaskAffinity(Window window) {
        if (taskAffinityMask == 0 && taskAffinityMaskWoW64 == 0) return;
        int processId = window.getProcessId();
        String className = window.getClassName();
        int processAffinity = window.isWoW64() ? taskAffinityMaskWoW64 : taskAffinityMask;
        if (processAffinity == 0) return;

        if (processId > 0) {
            winHandler.setProcessAffinity(processId, processAffinity);
        }
        else if (!className.isEmpty()) {
            winHandler.setProcessAffinity(window.getClassName(), processAffinity);
        }
    }

    private void changeFrameRatingVisibility(Window window, Property property) {
        if (frameRating == null) return;

        if (property != null) {
            if (frameRatingWindowId == -1 && property.nameAsString().contains("_MESA_DRV")) {
                frameRatingWindowId = window.id;
                Log.d("XServerDisplayActivity", "Showing hud for Window " + window.getName());
                frameRating.update();
            }
            if (property.nameAsString().contains("_MESA_DRV_ENGINE_NAME")) {
                runOnUiThread(() -> frameRating.setRenderer(property.toString()));
            }
            if (property.nameAsString().contains("_MESA_DRV_GPU_NAME")) {
                runOnUiThread(() -> frameRating.setGpuName(property.toString()));
            }
        }
        else if (frameRatingWindowId != -1) {
            frameRatingWindowId = -1;
            Log.d("XServerDisplayActivity", "Hiding hud for Window " + window.getName());
            runOnUiThread(() -> frameRating.setVisibility(View.GONE));
            runOnUiThread(() -> frameRating.reset());
        }
    }

    public String getScreenEffectProfile() {
        return screenEffectProfile;
    }

    public void setScreenEffectProfile(String screenEffectProfile) {
        this.screenEffectProfile = screenEffectProfile;
    }

}
