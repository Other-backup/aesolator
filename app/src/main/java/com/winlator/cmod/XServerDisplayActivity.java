package com.winlator.cmod;

import static com.winlator.cmod.core.AppUtils.showToast;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.Spinner;
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
import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.container.Shortcut;
import com.winlator.cmod.contentdialog.ContentDialog;
import com.winlator.cmod.contentdialog.DXVKConfigDialog;
import com.winlator.cmod.contentdialog.DebugDialog;
import com.winlator.cmod.contentdialog.DgVoodooConfigDialog;
import com.winlator.cmod.contentdialog.GraphicsDriverConfigDialog;
import com.winlator.cmod.contentdialog.ScreenEffectDialog;
import com.winlator.cmod.contentdialog.WineD3DConfigDialog;
import com.winlator.cmod.contract.RuntimeSignalContract;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.contents.AdrenotoolsManager;
import com.winlator.cmod.contents.DgVoodooManager;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.Callback;
import com.winlator.cmod.core.DefaultVersion;
import com.winlator.cmod.core.EnvVars;
import com.winlator.cmod.core.FileDebugLogger;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.ForensicConfig;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.core.GPUInformation;
import com.winlator.cmod.core.KeyValueSet;
import com.winlator.cmod.core.LaunchSecurity;
import com.winlator.cmod.core.OnExtractFileListener;
import com.winlator.cmod.core.PreloaderDialog;
import com.winlator.cmod.core.ProcessHelper;
import com.winlator.cmod.core.SpinnerAdapters;
import com.winlator.cmod.core.StringUtils;
import com.winlator.cmod.core.ThemeAssetPainter;
import com.winlator.cmod.core.TarCompressorUtils;
import com.winlator.cmod.core.UpscalerProfileStore;
import com.winlator.cmod.core.WineInfo;
import com.winlator.cmod.core.WineRegistryEditor;
import com.winlator.cmod.core.WineRequestHandler;
import com.winlator.cmod.core.WineStartMenuCreator;
import com.winlator.cmod.core.WineThemeManager;
import com.winlator.cmod.core.WineUtils;
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
import com.winlator.cmod.xenvironment.XEnvironment;
import com.winlator.cmod.xenvironment.components.ALSAServerComponent;
import com.winlator.cmod.xenvironment.components.GuestProgramLauncherComponent;
import com.winlator.cmod.xenvironment.components.PulseAudioComponent;
import com.winlator.cmod.xenvironment.components.SysVSharedMemoryComponent;
import com.winlator.cmod.xenvironment.components.XServerComponent;
import com.winlator.cmod.xserver.Pointer;
import com.winlator.cmod.xserver.Property;
import com.winlator.cmod.xserver.ScreenInfo;
import com.winlator.cmod.xserver.Window;
import com.winlator.cmod.xserver.WindowManager;
import com.winlator.cmod.xserver.XServer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.sherlock.com.sun.media.sound.SF2Soundbank;

public class XServerDisplayActivity extends AppCompatActivity {
    public static String NOTIFICATION_CHANNEL_ID = "Aesolator";
    public static int NOTIFICATION_ID = -1;
    private XServerView xServerView;
    private InputControlsView inputControlsView;
    private TouchpadView touchpadView;
    private XEnvironment environment;
    private View xserverRootView;
    private View runtimeDrawerScrim;
    private View runtimeDrawerView;
    private boolean runtimeDrawerVisible = false;
    private ContainerManager containerManager;
    protected Container container;
    private XServer xServer;
    private InputControlsManager inputControlsManager;
    private ImageFs imageFs;
    private FrameRating frameRating = null;
    private Runnable editInputControlsCallback;
    private Shortcut shortcut;
    private String graphicsDriver = Container.DEFAULT_GRAPHICS_DRIVER;
    private HashMap<String, String> graphicsDriverConfig;
    private String audioDriver = Container.DEFAULT_AUDIO_DRIVER;
    private String emulator = Container.DEFAULT_EMULATOR;
    private String dxwrapper = Container.DEFAULT_DXWRAPPER;
    private KeyValueSet dxwrapperConfig;
    private String startupSelection;
    private WineInfo wineInfo;
    private final EnvVars envVars = new EnvVars();
    private boolean firstTimeBoot = false;
    private SharedPreferences preferences;
    private OnExtractFileListener onExtractFileListener;
    private WinHandler winHandler;
    private WineRequestHandler wineRequestHandler;
    private float globalCursorSpeed = 1.0f;
    private MagnifierView magnifierView;
    private DebugDialog debugDialog;
    private final ArrayList<Callback<String>> forensicRuntimeCallbacks = new ArrayList<>();
    private short taskAffinityMask = 0;
    private short taskAffinityMaskWoW64 = 0;
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
    PreloaderDialog preloaderDialog = null;
    private Runnable configChangedCallback = null;
    private boolean isPaused = false;
    private boolean isRelativeMouseMovement = false;
    private final LinkedHashSet<Integer> mappedApplicationWindowIds = new LinkedHashSet<>();
    private volatile boolean desktopShellBootstrapActive = false;
    private volatile boolean guestLauncherExited = false;
    private volatile int guestLauncherExitStatus = Integer.MIN_VALUE;
    private boolean desktopGestureExclusionListenerAttached = false;

    // Inside the XServerDisplayActivity class
    private SensorManager sensorManager;
    private Sensor gyroSensor;
    private ExternalController controller;

    // Playtime stats tracking
    private long startTime;
    private SharedPreferences playtimePrefs;
    private String shortcutName;
    private Handler handler;
    private Runnable savePlaytimeRunnable;
    private static final long SAVE_INTERVAL_MS = 1000;
    private final ExecutorService exitTeardownExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean exitInProgress = new AtomicBoolean(false);

    private Handler  timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable hideControlsRunnable;
    private static final long DESKTOP_RUNTIME_PAUSE_GRACE_MS = 1800L;
    private static final long DESKTOP_SHELL_TERMINATION_GRACE_MS = 8000L;
    private final Handler runtimePauseHandler = new Handler(Looper.getMainLooper());
    private boolean deferredDesktopPauseScheduled = false;
    private boolean deferredGuestTerminationScheduled = false;
    private long desktopShellBootstrapStartedAtMs = 0L;
    private int debugStartProbeTargetX = Integer.MIN_VALUE;
    private int debugStartProbeTargetY = Integer.MIN_VALUE;
    private int debugStartProbeTapCount = 1;
    private int debugStartProbeTapIntervalMs = 110;
    private final Runnable deferredDesktopPauseRunnable = new Runnable() {
        @Override
        public void run() {
            deferredDesktopPauseScheduled = false;
            pauseDesktopRuntime("deferred_background_pause");
        }
    };
    private final Runnable deferredGuestTerminationRunnable = new Runnable() {
        @Override
        public void run() {
            deferredGuestTerminationScheduled = false;
            int trackedCount = getTrackedApplicationWindowCount();
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
    private static final String UPSCALER_BACKEND_MOBFGSR = "mobfgsr";
    private static final String UPSCALER_EFFECT_NONE = "none";
    private static final String FG_SOURCE_NATIVE = "native";
    private static final String FG_SOURCE_OPTI_FG = "opti_fg";
    private static final String FG_OUTPUT_AUTO = "auto";
    private static final String FG_OUTPUT_MOBFGSR = "mobfgsr";
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
                || intent.hasExtra("disableXinput");
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

        boolean isOpenWithAndroidBrowser = preferences.getBoolean("open_with_android_browser", false);
        boolean isShareAndroidClipboard = preferences.getBoolean("share_android_clipboard", false);

        // Initialize the WinHandler after context is set up
        winHandler = new WinHandler(this);
        winHandler.initializeController();
        controller = winHandler.getCurrentController();

        if (isOpenWithAndroidBrowser || isShareAndroidClipboard)
            wineRequestHandler = new WineRequestHandler(this);

        if (controller != null) {
            int triggerType = preferences.getInt("trigger_type", ExternalController.TRIGGER_IS_AXIS); // Default to TRIGGER_IS_AXIS
            controller.setTriggerType((byte) triggerType); // Cast to byte if needed
        }

        Intent launchIntent = getIntent();
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
                        "debug_start_probe_armed", debugStartProbeArmed,
                        "debug_probe_target_x", debugStartProbeTargetX,
                        "debug_probe_target_y", debugStartProbeTargetY,
                        "debug_probe_tap_count", debugStartProbeTapCount,
                        "debug_probe_tap_interval_ms", debugStartProbeTapIntervalMs,
                        "requires_signature", requiresSignedLaunchIntent(launchIntent),
                        "has_signature", LaunchSecurity.hasXServerLaunchSignature(launchIntent),
                        "trust_state", launchTrustState
                )
        );
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
                        "share_android_clipboard", isShareAndroidClipboard,
                        "open_with_android_browser", isOpenWithAndroidBrowser
                )
        );



        // Check if xinputDisabled extra is passed
        boolean xinputDisabledFromShortcut = false;




        // Initialize SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        boolean gyroEnabled = preferences.getBoolean("gyro_enabled", true);

        if (gyroEnabled) {
            // Register the sensor event listener
            sensorManager.registerListener(gyroListener, gyroSensor, SensorManager.SENSOR_DELAY_GAME);
        }



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


        // Handler and Runnable to manage timeout for hiding controls

        boolean isTimeoutEnabled = preferences.getBoolean("touchscreen_timeout_enabled", true);

        hideControlsRunnable = () -> {
            if (isTimeoutEnabled) {
                inputControlsView.setVisibility(View.GONE);
                Log.d("XServerDisplayActivity", "Touchscreen controls hidden after timeout.");
            }
        };


        contentsManager = new ContentsManager(this);
        contentsManager.syncContents();

        xserverRootView = findViewById(R.id.XServerRoot);

        imageFs = ImageFs.find(this);

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

        containerManager.activateContainer(container);

        if (shortcutPath != null && !shortcutPath.isEmpty()) {
            shortcut = new Shortcut(container, new File(shortcutPath));
        }

        taskAffinityMask = (short) ProcessHelper.getAffinityMask(container.getCPUList(true));
        taskAffinityMaskWoW64 = (short) ProcessHelper.getAffinityMask(container.getCPUListWoW64(true));

        if (shortcut != null) {
            taskAffinityMask = (short) ProcessHelper.getAffinityMask(shortcut.getExtra("cpuList", container.getCPUList(true)));
            taskAffinityMaskWoW64 = taskAffinityMask;
        }

        // Determine the class name for the startup workarounds
        String wmClass = shortcut != null ? shortcut.getExtra("wmClass", "") : "";
        Log.d("XServerDisplayActivity", "Startup wmClass: " + wmClass);

        firstTimeBoot = container.getExtra("appVersion").isEmpty();

        String wineVersion = container.getWineVersion();
        wineInfo = WineInfo.fromIdentifier(this, contentsManager, wineVersion);

        imageFs.setWinePath(wineInfo.path);

        boolean enableLogs = preferences.getBoolean("enable_wine_debug", false)
                || preferences.getBoolean("enable_box64_logs", false);

        ProcessHelper.removeAllDebugCallbacks();
        if (enableLogs) {
            LogView.setFilename(getExecutable());
            ProcessHelper.addDebugCallback(debugDialog = new DebugDialog(this));
        }
        installForensicRuntimeLogCallbacks(ForensicConfig.load(this));

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

        // Log the entire intent to verify the extras
        Log.d("XServerDisplayActivity", "Intent Extras: " + launchIntent.getExtras());

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
            parseUpscalerFromShortcut(shortcut);
            Log.d("XServerDisplayActivity", "XInput Disabled from Shortcut: " + xinputDisabledFromShortcut);
        }

        this.graphicsDriverConfig = GraphicsDriverConfigDialog.parseGraphicsDriverConfig(graphicsDriverConfig);
        this.dxwrapperConfig = DXVKConfigDialog.parseConfig(dxwrapperConfig);
        setupRuntimeDrawer();

        if (!wineInfo.isWin64()) {
            onExtractFileListener = (file, size) -> {
                String path = file.getPath();
                if (path.contains("system32/")) return null;
                return new File(path.replace("syswow64/", "system32/"));
            };
        }

        preloaderDialog.show(R.string.starting_up);

        inputControlsManager = new InputControlsManager(this);
        xServer = new XServer(new ScreenInfo(screenSize));
        xServer.setWinHandler(winHandler);

        boolean[] winStarted = {false};

        Runnable[] markGuestWindowStarted = new Runnable[1];
        markGuestWindowStarted[0] = () -> {
            if (!winStarted[0]) {
                xServerView.getRenderer().setCursorVisible(true);
                preloaderDialog.closeOnUiThread();
                winStarted[0] = true;
            }
        };

        // Add the OnWindowModificationListener for dynamic workarounds
        xServer.windowManager.addOnWindowModificationListener(new WindowManager.OnWindowModificationListener() {
            @Override
            public void onUpdateWindowContent(Window window) {
                if (!winStarted[0] && window.isApplicationWindow()) {
                    markGuestWindowStarted[0].run();
                }

                if (frameRatingWindowId == window.id) frameRating.update();
            }

            @Override
            public void onMapWindow(Window window) {
                if (!winStarted[0] && window.isApplicationWindow()) {
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
                    markGuestWindowStarted[0].run();
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
                    } catch (Exception e2) {}
                }
            };
            try {
                if (midiSoundFont.equals(MidiManager.DEFAULT_SF2_FILE)) {
                    in = getAssets().open(MidiManager.SF2_ASSETS_DIR + "/" + midiSoundFont);
                    MidiManager.load(in, callback);
                } else
                    MidiManager.load(new File(MidiManager.getSoundFontDir(this), midiSoundFont), callback);
            } catch (Exception e) {}
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
        notificationIntent.putExtra("disableXinput", xinputDisabledFromShortcut ? "1" : "0");
        LaunchSecurity.signXServerLaunchIntent(this, notificationIntent);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_ab_gear_0011)
                .setContentTitle(getString(R.string.notification_runtime_title))
                .setContentText(getString(R.string.notification_runtime_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false);

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build());

        Runnable runnable = () -> {
            setupUI();
            if (controlsProfile.isEmpty()) {
                // No profile defined, run the simulated dialog confirmation for input controls
                simulateConfirmInputControlsDialog();
            }
            Executors.newSingleThreadExecutor().execute(() -> {
                setupWineSystemFiles();
                extractGraphicsDriverFiles();
                changeWineAudioDriver();
                try {
                    setupXEnvironment();
                } catch (PackageManager.NameNotFoundException e) {
                    throw new RuntimeException(e);
                }
            });
        };

        boolean landscapeReady = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        boolean portraitScreenInfo = xServer.screenInfo.height > xServer.screenInfo.width;
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
            configChangedCallback = runnable;
        } else {
            runnable.run();
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
    public void onResume() {
        super.onResume();
        cancelDeferredDesktopRuntimePause("resume");
        boolean gyroEnabled = preferences.getBoolean("gyro_enabled", true);

        if (gyroEnabled) {
            // Re-register the sensor listener when the activity is resumed
            sensorManager.registerListener(gyroListener, gyroSensor, SensorManager.SENSOR_DELAY_GAME);
        }

        if (environment != null) {
            xServerView.onResume();
            environment.onResume();
        }
        startTime = System.currentTimeMillis();
        handler.postDelayed(savePlaytimeRunnable, SAVE_INTERVAL_MS);
        ProcessHelper.resumeAllWineProcesses();
    }

    @Override
    public void onPause() {
        super.onPause();
        boolean gyroEnabled = preferences.getBoolean("gyro_enabled", true);

        if (gyroEnabled) {
            // Unregister the sensor listener when the activity is paused
            sensorManager.unregisterListener(gyroListener);
        }

        boolean enteringPictureInPicture = isInPictureInPictureMode();
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
            if (sensorManager != null) sensorManager.unregisterListener(gyroListener);
            if (winHandler != null) winHandler.stop();
            if (wineRequestHandler != null) wineRequestHandler.stop();
            if (environment != null) environment.stopEnvironmentComponents();
            ProcessHelper.terminateAllWineProcesses();
            waitForWineProcessesToTerminate(1500);

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

    private void exit() {
        if (!exitInProgress.compareAndSet(false, true)) return;
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
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (deferredDesktopPauseScheduled) {
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
        bindRuntimeDrawerAction(R.id.LLRuntimeActionMagnifier, R.id.main_menu_magnifier);
        bindRuntimeDrawerAction(R.id.LLRuntimeActionLogs, R.id.main_menu_logs);
        bindRuntimeDrawerAction(R.id.LLRuntimeActionExit, R.id.main_menu_exit);

        runtimeDrawerView.post(() -> runtimeDrawerView.setTranslationX(getRuntimeDrawerHiddenOffset()));
        refreshRuntimeDrawerState();
        applyRuntimeThemeAssetPass();
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

    private void refreshRuntimeDrawerState() {
        TextView tvContainer = findViewById(R.id.TVRuntimeDrawerContainerName);
        TextView tvShortcut = findViewById(R.id.TVRuntimeDrawerShortcutName);
        TextView tvRoute = findViewById(R.id.TVRuntimeDrawerRoute);
        TextView tvHint = findViewById(R.id.TVRuntimeDrawerHint);

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

        boolean fullscreenActive = xServerView != null && xServerView.getRenderer() != null && xServerView.getRenderer().isFullscreen();
        boolean logsEnabled = debugDialog != null
                && (preferences.getBoolean("enable_wine_debug", false) || preferences.getBoolean("enable_box64_logs", false));
        boolean magnifierEnabled = !XrActivity.isEnabled(this);

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
                logsEnabled ? R.string.runtime_drawer_logs_summary : R.string.runtime_drawer_logs_disabled,
                R.drawable.ae_icon_diagnostics,
                logsEnabled
        );

        applyRuntimeThemeAssetPass();
    }

    private void updateRuntimeDrawerAction(int rowId, int titleId, int summaryId, int iconId,
                                           int titleResId, int summaryResId, int iconResId, boolean enabled) {
        View row = findViewById(rowId);
        TextView title = findViewById(titleId);
        TextView summary = findViewById(summaryId);
        View icon = findViewById(iconId);
        if (row == null || title == null || summary == null || icon == null) return;

        title.setText(titleResId);
        summary.setText(summaryResId);
        if (icon instanceof android.widget.ImageView) {
            ((android.widget.ImageView) icon).setImageResource(iconResId);
        }
        row.setEnabled(enabled);
        row.setClickable(enabled);
        row.setAlpha(enabled ? 1.0f : 0.56f);
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private void handleRuntimeAction(int actionId) {
        final GLRenderer renderer = xServerView.getRenderer();
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
                renderer.toggleFullscreen();
                touchpadView.toggleFullscreen();
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
                break;
            case R.id.main_menu_logs:
                if (debugDialog != null) debugDialog.show();
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
            updateDesktopGestureExclusionRects(touchpadView);
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

    private void extractInputDLLs() {
        String inputAsset = "input_dlls.tzst";
        File wineFolder = new File(imageFs.getWinePath() + "/lib/wine/");
        boolean success = TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, inputAsset, wineFolder);
        if (!success)
            Log.d("XServerDisplayActivity", "Failed to extract input dlls");
    }

    private boolean isTrackedApplicationWindow(Window window) {
        if (window == null) return false;
        return window.getWidth() > 1
                && window.getHeight() > 1
                && window.getWMHintsValue(Window.WMHints.WINDOW_GROUP) == window.id;
    }

    private void noteApplicationWindowMapped(Window window) {
        if (!isTrackedApplicationWindow(window)) return;
        int trackedCount;
        synchronized (mappedApplicationWindowIds) {
            if (!mappedApplicationWindowIds.add(window.id)) return;
            trackedCount = mappedApplicationWindowIds.size();
        }
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
        synchronized (mappedApplicationWindowIds) {
            if (!mappedApplicationWindowIds.isEmpty()) return true;
        }
        return Math.max(0L, System.currentTimeMillis() - desktopShellBootstrapStartedAtMs)
                < DESKTOP_SHELL_TERMINATION_GRACE_MS;
    }

    private int getTrackedApplicationWindowCount() {
        synchronized (mappedApplicationWindowIds) {
            return mappedApplicationWindowIds.size();
        }
    }

    private void scheduleDeferredGuestTermination(int status) {
        long elapsedMs = Math.max(0L, System.currentTimeMillis() - desktopShellBootstrapStartedAtMs);
        long delayMs = Math.max(0L, DESKTOP_SHELL_TERMINATION_GRACE_MS - elapsedMs);
        guestLauncherExitStatus = status;
        runtimePauseHandler.removeCallbacks(deferredGuestTerminationRunnable);
        deferredGuestTerminationScheduled = true;
        runtimePauseHandler.postDelayed(deferredGuestTerminationRunnable, delayMs);
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
        if (deferredDesktopPauseScheduled) return;
        deferredDesktopPauseScheduled = true;
        runtimePauseHandler.postDelayed(deferredDesktopPauseRunnable, DESKTOP_RUNTIME_PAUSE_GRACE_MS);
    }

    private void cancelDeferredDesktopRuntimePause(String reason) {
        if (!deferredDesktopPauseScheduled) return;
        runtimePauseHandler.removeCallbacks(deferredDesktopPauseRunnable);
        deferredDesktopPauseScheduled = false;
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
            xServerView.onPause();
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

    private void setupWineSystemFiles() {
        String appVersion = String.valueOf(AppUtils.getVersionCode(this));
        String imgVersion = String.valueOf(imageFs.getVersion());
        boolean containerDataChanged = false;

        if (!container.getExtra("appVersion").equals(appVersion) || !container.getExtra("imgVersion").equals(imgVersion)) {
            applyGeneralPatches(container);
            container.putExtra("appVersion", appVersion);
            container.putExtra("imgVersion", imgVersion);
            containerDataChanged = true;
        }

        String dxwrapperMode = this.dxwrapper;
        String dxwrapperSignature = dxwrapperMode;

        if (dxwrapperMode.contains("dxvk")) {
            String dxvkWrapper = "dxvk-" + dxwrapperConfig.get("version");
            String vkd3dWrapper = "vkd3d-" + dxwrapperConfig.get("vkd3dVersion");
            dxwrapperSignature = dxvkWrapper + ";" + vkd3dWrapper;
        } else if (dxwrapperMode.contains("dgvoodoo")) {
            KeyValueSet dgConfig = DgVoodooConfigDialog.parseConfig(dxwrapperConfig);
            String archRequested = DgVoodooConfigDialog.normalizeArch(dgConfig.get("dgvoodooArch"));
            String versionHint = dgConfig.get("dgvoodooVersionHint");
            dxwrapperSignature = "dgvoodoo:" + archRequested + ":" + versionHint;
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

        String desktopTheme = container.getDesktopTheme();
        if (!(desktopTheme+","+xServer.screenInfo).equals(container.getExtra("desktopTheme"))) {
            WineThemeManager.apply(this, new WineThemeManager.ThemeInfo(desktopTheme), xServer.screenInfo);
            container.putExtra("desktopTheme", desktopTheme+","+xServer.screenInfo);
            containerDataChanged = true;
        }

        WineStartMenuCreator.create(this, container);
        WineUtils.createDosdevicesSymlinks(container);

        if (shortcut != null)
            startupSelection = shortcut.getExtra("startupSelection", String.valueOf(container.getStartupSelection()));
        else
            startupSelection = String.valueOf(container.getStartupSelection());

        if (!startupSelection.equals(container.getExtra("startupSelection"))) {
            WineUtils.changeServicesStatus(container, Byte.parseByte(startupSelection) != Container.STARTUP_SELECTION_NORMAL);
            container.putExtra("startupSelection", startupSelection);
            containerDataChanged = true;
        }

        extractInputDLLs();

        if (containerDataChanged) container.saveData();
    }

    private void setupXEnvironment() throws PackageManager.NameNotFoundException {
        ForensicConfig.Snapshot forensicSnapshot = ForensicConfig.load(this);
        composeLaunchEnvVars(forensicSnapshot);

        // Clear any temporary directory
        String rootPath = imageFs.getRootDir().getPath();
        FileUtils.clear(imageFs.getTmpDir());

        int bindingPathCount = 0;
        synchronized (mappedApplicationWindowIds) {
            mappedApplicationWindowIds.clear();
        }
        guestLauncherExited = false;
        guestLauncherExitStatus = Integer.MIN_VALUE;
        desktopShellBootstrapActive = false;
        desktopShellBootstrapStartedAtMs = 0L;
        cancelDeferredGuestTermination("setup_xenvironment");

        guestProgramLauncherComponent = new GuestProgramLauncherComponent(
                contentsManager,
                contentsManager.resolveBestRuntimeProfile(container.getWineVersion()),
                shortcut
        );

        // Additional container checks and environment configuration
        if (container != null) {
            if (Byte.parseByte(startupSelection) == Container.STARTUP_SELECTION_AGGRESSIVE) {
                winHandler.killProcess("services.exe");
            }
            guestProgramLauncherComponent.setContainer(this.container);
            guestProgramLauncherComponent.setWineInfo(this.wineInfo);

            if (shortcut == null) {
                configureDesktopShellRegistry();
            }
            String guestExecutable = "wine explorer /desktop=shell," + xServer.screenInfo + " " + getWineStartCommand();
            desktopShellBootstrapActive = shortcut == null
                    && guestExecutable.toLowerCase(java.util.Locale.ROOT).contains("explorer /desktop=shell");
            if (desktopShellBootstrapActive) {
                desktopShellBootstrapStartedAtMs = System.currentTimeMillis();
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

        // Audio driver logic
        if (audioDriver.equals("alsa")) {
            envVars.put("ANDROID_ALSA_SERVER", rootPath + UnixSocketConfig.ALSA_SERVER_PATH);
            envVars.put("ANDROID_ASERVER_USE_SHM", "true");
            environment.addComponent(
                    new ALSAServerComponent(
                            UnixSocketConfig.createSocket(rootPath, UnixSocketConfig.ALSA_SERVER_PATH)
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

        // Pass final envVars to the launcher
        guestProgramLauncherComponent.setEnvVars(envVars);
        guestProgramLauncherComponent.setTerminationCallback((status) -> {
            guestLauncherExited = true;
            guestLauncherExitStatus = status;
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
                                "termination_grace_ms", DESKTOP_SHELL_TERMINATION_GRACE_MS
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
                        "has_wine_request_handler", wineRequestHandler != null
                )
        );

        // Start all environment components (XServer, Audio, etc.)
        environment.startEnvironmentComponents();

        // Start the WinHandler
        winHandler.start();

        if (wineRequestHandler != null) wineRequestHandler.start();

        ForensicLogger.logEvent(
                this,
                "info",
                "RUNTIME_ENV_COMPONENTS_STARTED",
                null,
                "xserver",
                "runtime_environment_components_started",
                ForensicLogger.fields(
                        "audio_driver", audioDriver,
                        "has_wine_request_handler", wineRequestHandler != null
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
        rootView.addView(touchpadView);
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
            touchpadView.resetGestureRuntimeTuning();
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

        targetView.post(() -> updateDesktopGestureExclusionRects(targetView));
        if (!desktopGestureExclusionListenerAttached) {
            targetView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                    updateDesktopGestureExclusionRects(targetView));
            desktopGestureExclusionListenerAttached = true;
        }
    }

    private void updateDesktopGestureExclusionRects(View targetView) {
        if (targetView == null || shortcut != null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;

        int width = targetView.getWidth();
        int height = targetView.getHeight();
        if (width <= 0 || height <= 0) return;

        ArrayList<Rect> exclusionRects = new ArrayList<>();
        exclusionRects.add(new Rect(0, 0, width, height));
        targetView.setSystemGestureExclusionRects(exclusionRects);

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

    private String resolveUpscalerValue(@NonNull Shortcut activeShortcut, @NonNull String key, @NonNull String fallback) {
        String shortcutValue = activeShortcut.getExtra(key, "");
        if (shortcutValue != null && !shortcutValue.trim().isEmpty()) return shortcutValue;
        if (container != null) {
            String containerValue = container.getExtra(key, "");
            if (containerValue != null && !containerValue.trim().isEmpty()) return containerValue;
        }
        return fallback;
    }

    private void parseUpscalerFromShortcut(@NonNull Shortcut activeShortcut) {
        UpscalerProfileStore.Profile globalProfile = UpscalerProfileStore.getSelectedProfile(preferences);
        String backend = resolveUpscalerValue(activeShortcut, "upscalerBackend", globalProfile.backend);
        backend = StringUtils.parseIdentifier(backend);
        if (!UPSCALER_BACKEND_VKBASALT.equals(backend) && !UPSCALER_BACKEND_MOBFGSR.equals(backend)) {
            backend = UpscalerProfileStore.normalizeBackend(globalProfile.backend);
        }

        String effect = resolveUpscalerValue(activeShortcut, "upscalerEffect", globalProfile.effect);
        effect = normalizeUpscalerEffect(effect);

        String presetRaw = resolveUpscalerValue(activeShortcut, "upscalerPreset", globalProfile.preset);
        if (presetRaw == null || presetRaw.trim().isEmpty()) presetRaw = UPSCALER_PRESET_AUTO;
        upscalerPreset = normalizeUpscalerPreset(presetRaw);
        upscalerBackend = backend;
        upscalerEffect = effect;

        String scaleRaw = resolveUpscalerValue(activeShortcut, "upscalerScale", String.valueOf(globalProfile.scalePercent));
        upscalerScalePercent = parseBoundedInt(
                scaleRaw,
                100,
                100,
                200
        );

        String sharpnessRaw = resolveUpscalerValue(activeShortcut, "upscalerSharpness", String.valueOf(globalProfile.sharpness));
        upscalerSharpnessPercent = parseBoundedIntAllowZero(
                sharpnessRaw,
                100,
                0,
                100
        );

        String denoiseRaw = resolveUpscalerValue(activeShortcut, "upscalerDenoise", String.valueOf(globalProfile.denoise));
        upscalerDenoisePercent = parseBoundedIntAllowZero(
                denoiseRaw,
                100,
                0,
                100
        );

        String framegenRaw = resolveUpscalerValue(
                activeShortcut,
                "upscalerFrameGeneration",
                globalProfile.frameGeneration ? "1" : "0"
        );
        upscalerFrameGeneration = parseBoolean(framegenRaw);

        String generatedFramesRaw = resolveUpscalerValue(
                activeShortcut,
                "upscalerGeneratedFrames",
                String.valueOf(globalProfile.generatedFrames)
        );
        upscalerGeneratedFrames = parseBoundedInt(
                generatedFramesRaw,
                1,
                1,
                3
        );

        String fgSourceRaw = resolveUpscalerValue(activeShortcut, "upscalerFgSource", globalProfile.fgSource);
        upscalerFgSource = normalizeFgSource(fgSourceRaw);

        String fgOutputRaw = resolveUpscalerValue(activeShortcut, "upscalerFgOutput", globalProfile.fgOutput);
        upscalerFgOutput = normalizeFgOutput(fgOutputRaw);

        String framegenModeRaw = resolveUpscalerValue(activeShortcut, "upscalerFramegenMode", globalProfile.framegenMode);
        upscalerFramegenMode = normalizeFramegenMode(framegenModeRaw);

        String thermalGuardRaw = resolveUpscalerValue(
                activeShortcut,
                "upscalerThermalGuard",
                globalProfile.thermalGuard ? "1" : "0"
        );
        upscalerThermalGuard = parseBoolean(thermalGuardRaw);

        String targetFpsRaw = resolveUpscalerValue(activeShortcut, "upscalerTargetFps", String.valueOf(globalProfile.targetFps));
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
        );
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
        );
        upscalerDebugOverlay = parseBoolean(debugOverlayRaw);

        String debugTearRaw = resolveUpscalerValue(
                activeShortcut,
                "upscalerDebugTearLines",
                globalProfile.debugTearLines ? "1" : "0"
        );
        upscalerDebugTearLines = parseBoolean(debugTearRaw);

        String interpolatedOnlyRaw = resolveUpscalerValue(
                activeShortcut,
                "upscalerInterpolatedOnly",
                globalProfile.interpolatedOnly ? "1" : "0"
        );
        upscalerInterpolatedOnly = parseBoolean(interpolatedOnlyRaw);

        String vkValidationRaw = resolveUpscalerValue(
                activeShortcut,
                "vulkanValidationLayer",
                globalProfile.vulkanValidationLayer ? "1" : "0"
        );
        upscalerVulkanValidationLayer = parseBoolean(vkValidationRaw);
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
            case FG_OUTPUT_MOBFGSR -> FG_OUTPUT_MOBFGSR;
            case "dlssg_to_fsr3", "dlssg-to-fsr3", "dlssgtofsr3" -> FG_OUTPUT_MOBFGSR;
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

    private void applyUpscalerEnvVars(boolean dxvkRoute, String socClass) {
        String guardReason = "none";
        String normalizedSocClass = socClass == null || socClass.trim().isEmpty() ? "unknown" : socClass.trim();
        boolean upscalerEnabled = !UPSCALER_BACKEND_OFF.equals(upscalerBackend)
                && !UPSCALER_EFFECT_NONE.equals(upscalerEffect);
        boolean frameGenerationRequested = upscalerFrameGeneration && upscalerEnabled;
        boolean frameGenerationBackendSupported = UPSCALER_BACKEND_MOBFGSR.equals(upscalerBackend);
        boolean frameGenerationActive = frameGenerationRequested && frameGenerationBackendSupported;
        if (frameGenerationRequested && !frameGenerationBackendSupported) {
            guardReason = "framegen_requires_mobfgsr_backend";
        }
        String resolvedFgOutput = upscalerFgOutput;
        if (FG_OUTPUT_AUTO.equals(resolvedFgOutput)) {
            resolvedFgOutput = UPSCALER_BACKEND_MOBFGSR.equals(upscalerBackend) ? FG_OUTPUT_MOBFGSR : FG_OUTPUT_AUTO;
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

        boolean mobfgsrDebugBridgeActive = frameGenerationActive && UPSCALER_BACKEND_MOBFGSR.equals(upscalerBackend);
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

        if (frameGenerationActive && !dxvkRoute) {
            frameGenerationActive = false;
            if ("none".equals(guardReason)) {
                guardReason = "framegen_requires_dxvk_route";
            }
            mobfgsrDebugBridgeActive = false;
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
        int vulkanSdkProfileCount = safeParseInt(envVars.get("AERO_VULKAN_SDK_PROFILE_COUNT"));
        boolean vulkanSdkAvailable = vulkanSdkProfileCount > 0;
        boolean requestedValidationLayer = upscalerEnabled && upscalerVulkanValidationLayer;
        Set<String> availableVkLayers = resolveAvailableVulkanLayerNames(envVars);
        boolean validationLayerAvailable = availableVkLayers.contains("VK_LAYER_KHRONOS_validation");
        boolean upscalerValidationLayerActive = requestedValidationLayer && vulkanSdkAvailable && validationLayerAvailable;
        if (requestedValidationLayer && !vulkanSdkAvailable && "none".equals(guardReason)) {
            guardReason = "vk_validation_requires_vulkansdk";
        }
        if (requestedValidationLayer && vulkanSdkAvailable && !validationLayerAvailable && "none".equals(guardReason)) {
            guardReason = "vk_validation_layer_missing";
        }
        setOrClearEnv("AERO_VK_VALIDATION_REQUESTED", requestedValidationLayer ? "1" : "0");
        setOrClearEnv("AERO_VK_VALIDATION_LAYER", upscalerValidationLayerActive ? "1" : "0");
        setOrClearEnv(
                "AERO_VK_VALIDATION_GUARD",
                requestedValidationLayer && !vulkanSdkAvailable
                        ? "vulkan_sdk_missing"
                        : requestedValidationLayer && vulkanSdkAvailable && !validationLayerAvailable
                        ? "vulkan_validation_layer_missing"
                        : ""
        );
        String vkLayers = removeVkInstanceLayer(envVars.get("VK_INSTANCE_LAYERS"), "VK_LAYER_KHRONOS_validation");
        if (upscalerValidationLayerActive) {
            vkLayers = appendVkInstanceLayers(vkLayers, "VK_LAYER_KHRONOS_validation");
        } else if (requestedValidationLayer && vulkanSdkAvailable) {
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
                mobfgsrDebugBridgeActive && upscalerDebugOverlay ? "1" : "0"
        );
        setOrClearEnv(
                "AERO_FRAMEGEN_DEBUG_TEAR_LINES",
                mobfgsrDebugBridgeActive && upscalerDebugTearLines ? "1" : "0"
        );
        setOrClearEnv(
                "AERO_FRAMEGEN_INTERPOLATED_ONLY",
                mobfgsrDebugBridgeActive && upscalerInterpolatedOnly ? "1" : "0"
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

        if (UPSCALER_BACKEND_MOBFGSR.equals(upscalerBackend) && upscalerEnabled) {
            setOrClearEnv("AERO_MOBFGSR_ENABLE_SR", "1");
            setOrClearEnv("AERO_MOBFGSR_ENABLE_INTERP", frameGenerationActive ? "1" : "0");
            setOrClearEnv("AERO_MOBFGSR_PRESET", effectivePreset);
            setOrClearEnv("AERO_MOBFGSR_SOC_CLASS", normalizedSocClass);
            setOrClearEnv("AERO_MOBFGSR_GENERATED_FRAMES", String.valueOf(effectiveGeneratedFrames));
            setOrClearEnv("AERO_MOBFGSR_RENDER_SCALE", String.format(Locale.US, "%.2f", upscalerScalePercent / 100.0f));
            setOrClearEnv("AERO_MOBFGSR_MODE", upscalerFramegenMode);
            setOrClearEnv("AERO_MOBFGSR_THERMAL_GUARD", effectiveThermalGuard ? "1" : "0");
            setOrClearEnv("AERO_MOBFGSR_FG_SOURCE", upscalerFgSource);
            setOrClearEnv("AERO_MOBFGSR_FG_OUTPUT", resolvedFgOutput);
            setOrClearEnv("AERO_MOBFGSR_MODEL_SCALE", String.format(Locale.US, "%.2f", modeScale));
            setOrClearEnv("AERO_MOBFGSR_QUALITY", String.format(Locale.US, "%.2f", modeQuality));
            setOrClearEnv("AERO_MOBFGSR_FRAME_BUDGET_MS", String.format(Locale.US, "%.2f", modeBudgetMs));
            setOrClearEnv("AERO_MOBFGSR_TARGET_FPS", String.valueOf(effectiveTargetFps));
            setOrClearEnv(
                    "AERO_MOBFGSR_INTERPOLATION_FACTOR",
                    frameGenerationActive ? String.valueOf(effectiveInterpolationFactor) : ""
            );
            setOrClearEnv("AERO_MOBFGSR_DEBUG_OVERLAY", upscalerDebugOverlay ? "1" : "0");
            setOrClearEnv("AERO_MOBFGSR_DEBUG_TEAR_LINES", upscalerDebugTearLines ? "1" : "0");
            setOrClearEnv("AERO_MOBFGSR_INTERPOLATED_ONLY", upscalerInterpolatedOnly ? "1" : "0");
            setOrClearEnv("AERO_MOBFGSR_DEPTH_DIFF_THRESHOLD_SR", String.format(Locale.US, "%.4f", depthDiffThresholdSr));
            setOrClearEnv("AERO_MOBFGSR_COLOR_DIFF_THRESHOLD_FG", String.format(Locale.US, "%.4f", colorDiffThresholdFg));
            setOrClearEnv("AERO_MOBFGSR_DEPTH_DIFF_THRESHOLD_FG", String.format(Locale.US, "%.4f", depthDiffThresholdFg));
        }
        else {
            setOrClearEnv("AERO_MOBFGSR_ENABLE_SR", "");
            setOrClearEnv("AERO_MOBFGSR_ENABLE_INTERP", "");
            setOrClearEnv("AERO_MOBFGSR_PRESET", "");
            setOrClearEnv("AERO_MOBFGSR_SOC_CLASS", "");
            setOrClearEnv("AERO_MOBFGSR_GENERATED_FRAMES", "");
            setOrClearEnv("AERO_MOBFGSR_RENDER_SCALE", "");
            setOrClearEnv("AERO_MOBFGSR_MODE", "");
            setOrClearEnv("AERO_MOBFGSR_THERMAL_GUARD", "");
            setOrClearEnv("AERO_MOBFGSR_FG_SOURCE", "");
            setOrClearEnv("AERO_MOBFGSR_FG_OUTPUT", "");
            setOrClearEnv("AERO_MOBFGSR_MODEL_SCALE", "");
            setOrClearEnv("AERO_MOBFGSR_QUALITY", "");
            setOrClearEnv("AERO_MOBFGSR_FRAME_BUDGET_MS", "");
            setOrClearEnv("AERO_MOBFGSR_TARGET_FPS", "");
            setOrClearEnv("AERO_MOBFGSR_INTERPOLATION_FACTOR", "");
            setOrClearEnv("AERO_MOBFGSR_DEBUG_OVERLAY", "");
            setOrClearEnv("AERO_MOBFGSR_DEBUG_TEAR_LINES", "");
            setOrClearEnv("AERO_MOBFGSR_INTERPOLATED_ONLY", "");
            setOrClearEnv("AERO_MOBFGSR_DEPTH_DIFF_THRESHOLD_SR", "");
            setOrClearEnv("AERO_MOBFGSR_COLOR_DIFF_THRESHOLD_FG", "");
            setOrClearEnv("AERO_MOBFGSR_DEPTH_DIFF_THRESHOLD_FG", "");
        }

        String runtimeGuardReason = envVars.get(RuntimeSignalContract.WINLATOR_RUNTIME_PRESET_GUARD_REASON);
        RuntimeSignalContract.putSignalPolicyMarkers(
                envVars,
                "aero-signal-v1",
                "shortcut+graphics+runtime",
                runtimeGuardReason,
                guardReason
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
                        "preset_requested", requestedPreset,
                        "preset_effective", effectivePreset,
                        "soc_class", normalizedSocClass,
                        "effect", upscalerEffect,
                        "scale_percent", upscalerScalePercent,
                        "sharpness_percent", upscalerSharpnessPercent,
                        "denoise_percent", upscalerDenoisePercent,
                        "vk_validation_layer_requested", requestedValidationLayer ? "1" : "0",
                        "vk_validation_layer", upscalerValidationLayerActive ? "1" : "0",
                        "vk_validation_guard", requestedValidationLayer && !vulkanSdkAvailable ? "vulkan_sdk_missing" : "none",
                        "vulkan_sdk_profile_count", vulkanSdkProfileCount,
                        "framegen_enabled", frameGenerationActive ? "1" : "0",
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

            sProfile.setAdapter(SpinnerAdapters.create(this, isDarkMode, profileItems));
            sProfile.setSelection(selectedPosition);
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
                        catch (Throwable ignored) {}
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

    private static final Pattern SOC_ADRENO_PATTERN =
            Pattern.compile("adreno\\s*(\\d{3,4})", Pattern.CASE_INSENSITIVE);
    private static final Pattern VULKAN_API_MINOR_PATTERN = Pattern.compile("1\\.(\\d+)");

    private String detectSoCClass() {
        String renderer = GPUInformation.getRenderer(null, this);
        if (renderer == null) return "unknown";
        String normalized = renderer.toLowerCase(Locale.US);

        if (normalized.contains("adreno")) {
            Matcher matcher = SOC_ADRENO_PATTERN.matcher(normalized);
            if (matcher.find()) {
                int generation = safeParseInt(matcher.group(1));
                if (generation >= 700) return "adreno-7xx";
            }
            return "adreno-6xx-and-older";
        }
        if (normalized.contains("xclipse")) return "xclipse-rdna-mobile";
        if (normalized.contains("mali")) {
            if (normalized.contains("g7") || normalized.contains("g8") || normalized.contains("g9")) {
                return "mali-g7xx-or-newer";
            }
        }
        return "unknown";
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
        EnvVars mergedEnv = new EnvVars();
        // Preserve graphics/runtime route env prepared before setupXEnvironment().
        mergedEnv.putAll(envVars);

        mergedEnv.put("LC_ALL", lc_all);
        mergedEnv.put("WINEPREFIX", imageFs.wineprefix);

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
        applyForensicEnvVars(mergedEnv, forensicSnapshot);

        if (!mergedEnv.has("WINEESYNC")) {
            mergedEnv.put("WINEESYNC", "1");
        }

        if (overrideEnvVars != null) {
            mergedEnv.putAll(overrideEnvVars);
            overrideEnvVars.clear();
        }
        mergedEnv.put("AERO_ENV_LAYER_ORDER", "graphics->container->shortcut->runtime->forensic->override");
        mergedEnv.put("AERO_FORENSIC_RUNTIME_SUMMARY", ForensicConfig.buildRuntimeSummary(forensicSnapshot));
        mergedEnv.put("AERO_FORENSIC_CAPTURE_SUMMARY", ForensicConfig.buildCaptureSummary(this, forensicSnapshot));

        ForensicLogger.logEvent(
                this,
                "info",
                "FORENSIC_ENV_APPLIED",
                null,
                "xserver",
                "forensic_env_applied",
                ForensicLogger.fields(
                        "runtime_summary", ForensicConfig.buildRuntimeSummary(forensicSnapshot),
                        "capture_summary", ForensicConfig.buildCaptureSummary(this, forensicSnapshot),
                        "wine_debug", forensicSnapshot.enableWineDebug ? "1" : "0",
                        "loader_trace", ForensicConfig.shouldEnableLoaderTrace(forensicSnapshot, false) ? "1" : "0",
                        "trace_mode", ForensicConfig.buildLoaderTraceMode(forensicSnapshot),
                        "env_hash", ForensicLogger.hashEnvVars(mergedEnv)
                )
        );

        envVars.clear();
        envVars.putAll(mergedEnv);
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
        String vkLayers = removeVkInstanceLayer(targetEnv.get("VK_INSTANCE_LAYERS"), "VK_LAYER_LUNARG_api_dump");
        vkLayers = removeVkInstanceLayer(vkLayers, "VK_LAYER_KHRONOS_validation");
        if (snapshot.enableVulkanApiDump) {
            if (availableVkLayers.contains("VK_LAYER_LUNARG_api_dump")) {
                vkLayers = appendVkInstanceLayers(vkLayers, "VK_LAYER_LUNARG_api_dump");
            } else {
                logMissingVulkanLayer("forensic", "VK_LAYER_LUNARG_api_dump", availableVkLayers);
            }
        }
        if (snapshot.enableVulkanValidation) {
            if (availableVkLayers.contains("VK_LAYER_KHRONOS_validation")) {
                vkLayers = appendVkInstanceLayers(vkLayers, "VK_LAYER_KHRONOS_validation");
            } else {
                logMissingVulkanLayer("forensic", "VK_LAYER_KHRONOS_validation", availableVkLayers);
            }
        }
        setOrClearEnv(targetEnv, "VK_INSTANCE_LAYERS", vkLayers);
    }

    private void installForensicRuntimeLogCallbacks(ForensicConfig.Snapshot snapshot) {
        forensicRuntimeCallbacks.clear();
        if (snapshot == null) return;

        addForensicRuntimeFileCallback(snapshot.enableWineDebug || snapshot.enableLoaderTrace,
                "wine_loader", "wine", "loaddll", "module", "ntdll", "kernel32");
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
        targetEnv.put("AERO_RUNTIME_LIBC", "bionic");
        targetEnv.put("AERO_RUNTIME_ANDROID_BIONIC_ONLY", "1");
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

    private String normalizeRequestedVulkanApi(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "1.3";
        Matcher matcher = VULKAN_API_MINOR_PATTERN.matcher(raw);
        if (!matcher.find()) return "1.3";
        try {
            int minor = Integer.parseInt(matcher.group(1));
            if (minor < 1) minor = 1;
            return "1." + minor;
        } catch (NumberFormatException ignored) {
            return "1.3";
        }
    }

    private int getVulkanApiMinor(String apiVersion) {
        if (apiVersion == null || apiVersion.trim().isEmpty()) return 3;
        Matcher matcher = VULKAN_API_MINOR_PATTERN.matcher(apiVersion);
        if (!matcher.find()) return 3;
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return 3;
        }
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

    private String detectVulkanSdkLaneArch(ContentProfile profile) {
        if (profile == null) return "generic";
        boolean hasArm64 = false;
        boolean hasArm64Ec = false;
        boolean hasX64 = false;
        if (profile.fileList != null) {
            for (ContentProfile.ContentFile contentFile : profile.fileList) {
                if (contentFile == null || contentFile.source == null) continue;
                String lowerSource = contentFile.source.toLowerCase(Locale.US);
                hasArm64 |= lowerSource.contains("/arm64/") || lowerSource.contains("/aarch64/");
                hasArm64Ec |= lowerSource.contains("/arm64ec/") || lowerSource.contains("/arm64-ec/");
                hasX64 |= lowerSource.contains("/x86_64/") || lowerSource.contains("/x86-64/") || lowerSource.contains("/amd64/");
            }
        }
        if ((hasArm64 || hasArm64Ec) && hasX64) return "bundle";
        String combined = (
                (profile.verName == null ? "" : profile.verName) + " " +
                (profile.desc == null ? "" : profile.desc) + " " +
                (profile.releaseTag == null ? "" : profile.releaseTag) + " " +
                (profile.remoteUrl == null ? "" : profile.remoteUrl)
        ).toLowerCase(Locale.US);
        if (combined.contains("bundle") || combined.contains("unified") || combined.contains("all-arch")) return "bundle";
        if (combined.contains("arm64ec")) return "arm64ec";
        if (combined.contains("x86_64") || combined.contains("x86-64") || combined.contains("amd64")) return "x86_64";
        if (combined.contains("arm64")) return "arm64";
        return "generic";
    }

    @NonNull
    private List<ContentProfile> resolveVulkanSdkProfilesForApi(String requestedApiVersion) {
        int requestedMinor = getVulkanApiMinor(requestedApiVersion);
        List<ContentProfile> profiles = contentsManager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_VULKAN_SDK);
        if (profiles == null || profiles.isEmpty()) return new ArrayList<>();

        HashMap<String, ContentProfile> bestByArch = new HashMap<>();
        HashMap<String, Integer> bestScoreByArch = new HashMap<>();
        HashMap<String, Integer> bestMaxByArch = new HashMap<>();

        for (ContentProfile profile : profiles) {
            if (profile == null || !profile.locallyInstalled) continue;
            int minMinor = parseMinVulkanMinor(profile);
            int maxMinor = parseMaxVulkanMinor(profile);
            if (minMinor <= 0 || maxMinor <= 0) continue;

            int score;
            if (requestedMinor < minMinor) {
                score = (minMinor - requestedMinor) + 100;
            } else if (requestedMinor <= maxMinor) {
                score = maxMinor - requestedMinor;
            } else {
                score = 1000 + (requestedMinor - maxMinor);
            }

            String archKey = detectVulkanSdkLaneArch(profile);
            Integer currentScore = bestScoreByArch.get(archKey);
            Integer currentMax = bestMaxByArch.get(archKey);
            if (currentScore == null
                    || score < currentScore
                    || (score == currentScore && (currentMax == null || maxMinor > currentMax))) {
                bestScoreByArch.put(archKey, score);
                bestMaxByArch.put(archKey, maxMinor);
                bestByArch.put(archKey, profile);
            }
        }

        ArrayList<ContentProfile> selected = new ArrayList<>();
        String[] preferredOrder = wineInfo != null && wineInfo.isArm64EC()
                ? new String[] {"bundle", "arm64ec", "x86_64", "arm64", "generic"}
                : new String[] {"bundle", "arm64", "x86_64", "arm64ec", "generic"};
        for (String arch : preferredOrder) {
            ContentProfile profile = bestByArch.get(arch);
            if (profile != null) selected.add(profile);
        }
        for (Map.Entry<String, ContentProfile> entry : bestByArch.entrySet()) {
            if (!selected.contains(entry.getValue())) selected.add(entry.getValue());
        }
        return selected;
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

    private void purgeLegacyVulkanSdkDirs(File rootDir) {
        if (rootDir == null) return;
        File legacyShare = new File(rootDir, "usr/share/vulkan-sdk");
        File legacyLib = new File(rootDir, "usr/lib/vulkan-sdk");
        if (legacyShare.exists()) FileUtils.delete(legacyShare);
        if (legacyLib.exists()) FileUtils.delete(legacyLib);
    }

    private void applyGraphicsRouteDefaults(boolean dxvkRoute, String socClass) {
        envVars.put("AERO_GRAPHICS_STACK_PROFILE", "vulkan-first-with-gl-fallback");
        envVars.put("AERO_GRAPHICS_SOC_CLASS", socClass);
        envVars.put("AERO_GRAPHICS_VULKAN_PROVIDER", "turnip-vulkan");
        envVars.put("AERO_GRAPHICS_OPENGL_PROVIDER", "freedreno-opengl");
        envVars.put("AERO_GL_FALLBACK_ENGINE", "wined3d");
        envVars.put("AERO_DXVK_LEGACY_DX89_PATH", "wined3d");
        envVars.put("AERO_DXVK_GL_FALLBACK", "1");
        envVars.put("AERO_VKD3D_GL_FALLBACK", "1");

        if (dxvkRoute) {
            envVars.put("AERO_GRAPHICS_ACTIVE_ROUTE", "turnip-primary");
            envVars.put("AERO_DXVK_ROUTE_MODE", "turnip-first");
            envVars.put("AERO_VKD3D_ROUTE_MODE", "turnip-first");
            envVars.put("GALLIUM_DRIVER", "zink");
        } else {
            envVars.put("AERO_GRAPHICS_ACTIVE_ROUTE", "freedreno-primary");
            envVars.put("AERO_DXVK_ROUTE_MODE", "freedreno-first");
            envVars.put("AERO_VKD3D_ROUTE_MODE", "freedreno-first");
            envVars.put("GALLIUM_DRIVER", "freedreno");
        }
    }

    private void applyGraphicsDriverPackages(String selectedDriverId, boolean dxvkRoute) {
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
            activeInfo = dxvkRoute ? turnipInfo : openGlInfo;
            companionInfo = dxvkRoute ? openGlInfo : turnipInfo;
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

        setOrClearEnv("AERO_GRAPHICS_SELECTED_DRIVER_ENTRY", selectedDriverId);
        setOrClearEnv("AERO_GRAPHICS_SELECTED_DRIVER_PACKAGE", selectedInfo == null ? "" : selectedInfo.name);
        setOrClearEnv("AERO_GRAPHICS_SELECTED_DRIVER_LANE", selectedInfo == null ? "" : selectedInfo.providerLane);
        setOrClearEnv("AERO_GRAPHICS_ACTIVE_PROVIDER_LANE", activeInfo == null ? "" : activeInfo.providerLane);
        setOrClearEnv("AERO_GRAPHICS_ACTIVE_PROVIDER_PACKAGE", activeInfo == null ? "" : activeInfo.name);
        setOrClearEnv("AERO_GRAPHICS_ACTIVE_PROVIDER_VERSION", activeInfo == null ? "" : activeInfo.driverVersion);
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

        ForensicLogger.logEvent(
                this,
                "info",
                "GRAPHICS_PROVIDER_CONTRACT_APPLIED",
                null,
                "graphics_provider",
                "graphics_provider_contract_applied",
                ForensicLogger.fields(
                        "selected_driver_id", selectedDriverId,
                        "selected_provider_lane", selectedInfo == null ? "" : selectedInfo.providerLane,
                        "active_provider_lane", activeInfo == null ? "" : activeInfo.providerLane,
                        "active_provider_package", activeInfo == null ? "" : activeInfo.name,
                        "active_provider_version", activeInfo == null ? "" : activeInfo.driverVersion,
                        "active_provider_route", activeInfo == null ? "" : activeInfo.driverRoute,
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
        String adrenoToolsDriverId = graphicsDriverConfig.get("version");

        Log.d("GraphicsDriverExtraction", "Adrenotools DriverID: " + adrenoToolsDriverId);

        File rootDir = imageFs.getRootDir();
        boolean dxvkRoute = dxwrapper.contains("dxvk");
        boolean dgVoodooRoute = dxwrapper.contains("dgvoodoo");
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

        envVars.put("VK_ICD_FILENAMES", imageFs.getShareDir() + "/vulkan/icd.d/wrapper_icd.aarch64.json");
        applyGraphicsRouteDefaults(dxvkRoute, socClass);
        applyWrapperContractsForCurrentRoute(dxvkRoute, socClass);

        if (firstTimeBoot) {
            Log.d("XServerDisplayActivity", "First time container boot, re-extracting libs");
            TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "graphics_driver/wrapper" + ".tzst", rootDir);
            TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "layers" + ".tzst", rootDir);
            TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "graphics_driver/extra_libs" + ".tzst", rootDir);
            if (wineInfo.isArm64EC() && !GPUInformation.getRenderer(null,null).contains("Mali"))
                TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "graphics_driver/zink_dlls" + ".tzst", new File(rootDir, imageFs.WINEPREFIX + "/drive_c/windows"));
        }

        applyGraphicsDriverPackages(adrenoToolsDriverId, dxvkRoute);

        String requestedVulkanApi = normalizeRequestedVulkanApi(graphicsDriverConfig.get("vulkanVersion"));
        List<ContentProfile> selectedVulkanSdkProfiles = resolveVulkanSdkProfilesForApi(requestedVulkanApi);
        if (!selectedVulkanSdkProfiles.isEmpty()) {
            purgeLegacyVulkanSdkDirs(rootDir);

            int effectiveMinMinor = 0;
            int effectiveMaxMinor = 0;
            ArrayList<String> selectedEntries = new ArrayList<>();
            ArrayList<String> selectedSdkVersions = new ArrayList<>();

            for (ContentProfile profile : selectedVulkanSdkProfiles) {
                contentsManager.applyContent(profile);

                String entry = ContentsManager.getEntryName(profile);
                if (!selectedEntries.contains(entry)) selectedEntries.add(entry);

                String sdkVersion = profile.vulkanSdkVersion == null ? "" : profile.vulkanSdkVersion.trim();
                if (!sdkVersion.isEmpty() && !selectedSdkVersions.contains(sdkVersion)) {
                    selectedSdkVersions.add(sdkVersion);
                }

                int profileMinMinor = parseMinVulkanMinor(profile);
                int profileMaxMinor = parseMaxVulkanMinor(profile);
                if (profileMaxMinor <= 0) continue;

                if (profileMinMinor > effectiveMinMinor) {
                    effectiveMinMinor = profileMinMinor;
                }
                if (effectiveMaxMinor <= 0 || profileMaxMinor < effectiveMaxMinor) {
                    effectiveMaxMinor = profileMaxMinor;
                }
            }

            if (effectiveMaxMinor > 0 && effectiveMinMinor > effectiveMaxMinor) {
                // If profile ranges don't intersect, fallback to broadest upper bound.
                effectiveMinMinor = 0;
                effectiveMaxMinor = 0;
                for (ContentProfile profile : selectedVulkanSdkProfiles) {
                    int profileMaxMinor = parseMaxVulkanMinor(profile);
                    if (profileMaxMinor > effectiveMaxMinor) {
                        effectiveMaxMinor = profileMaxMinor;
                    }
                }
            }

            int requestedMinor = getVulkanApiMinor(requestedVulkanApi);
            if (effectiveMinMinor > 0 && requestedMinor < effectiveMinMinor) {
                requestedMinor = effectiveMinMinor;
            }
            if (effectiveMaxMinor > 0 && requestedMinor > effectiveMaxMinor) {
                requestedMinor = effectiveMaxMinor;
            }
            requestedVulkanApi = "1." + requestedMinor;

            envVars.put("AERO_VULKAN_SDK_PROFILE", joinCsv(selectedEntries));
            envVars.put("AERO_VULKAN_SDK_PROFILE_COUNT", String.valueOf(selectedEntries.size()));
            setOrClearEnv("AERO_VULKAN_SDK_VERSION", joinCsv(selectedSdkVersions));
            setOrClearEnv("AERO_VULKAN_API_MIN_AVAILABLE", effectiveMinMinor > 0 ? "1." + effectiveMinMinor : "");
            setOrClearEnv("AERO_VULKAN_API_MAX_AVAILABLE", effectiveMaxMinor > 0 ? "1." + effectiveMaxMinor : "");
        } else {
            envVars.put("AERO_VULKAN_SDK_PROFILE", "none");
            envVars.put("AERO_VULKAN_SDK_PROFILE_COUNT", "0");
            setOrClearEnv("AERO_VULKAN_SDK_VERSION", "");
            setOrClearEnv("AERO_VULKAN_API_MIN_AVAILABLE", "");
            setOrClearEnv("AERO_VULKAN_API_MAX_AVAILABLE", "");
        }
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
        if (maxDeviceMemory != null && Integer.parseInt(maxDeviceMemory) > 0)
            envVars.put("WRAPPER_VMEM_MAX_SIZE", maxDeviceMemory);

        String presentMode = graphicsDriverConfig.get("presentMode");
        if (presentMode.contains("immediate")) {
            envVars.put("WRAPPER_MAX_IMAGE_COUNT", "1");
        }
        envVars.put("MESA_VK_WSI_PRESENT_MODE", presentMode);

        String resourceType = graphicsDriverConfig.get("resourceType");
        envVars.put("WRAPPER_RESOURCE_TYPE", resourceType);

        String syncFrame = graphicsDriverConfig.get("syncFrame");
        if (syncFrame.equals("1") && !dri3ForceSwWsi && useDRI3)
            envVars.put("MESA_VK_WSI_DEBUG", "forcesync");

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

        ForensicLogger.logEvent(
                this,
                "info",
                "GRAPHICS_ROUTE_APPLIED",
                null,
                "graphics_route",
                "graphics_route_applied",
                ForensicLogger.fields(
                        "graphics_driver", graphicsDriver,
                        "driver_id", adrenoToolsDriverId,
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
                        "vulkan_sdk_profiles", envVars.get("AERO_VULKAN_SDK_PROFILE"),
                        "vulkan_sdk_profile_count", envVars.get("AERO_VULKAN_SDK_PROFILE_COUNT"),
                        "wrapper_vk_version", envVars.get("WRAPPER_VK_VERSION")
                )
        );

        applyUpscalerEnvVars(dxvkRoute, socClass);
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

        // Handle the PlayStation or Xbox Home button to open the drawer
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_MODE || event.getKeyCode() == KeyEvent.KEYCODE_HOME || event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_SELECT) {
                boolean handled = inputControlsView.onKeyEvent(event) || (winHandler != null && winHandler.onKeyEvent(event)) && (xServer != null && xServer.keyboard.onKeyEvent(event));
                return true;
            }
        }

        boolean handledByInputControls = inputControlsView != null && inputControlsView.onKeyEvent(event);
        boolean handledByWinHandler = winHandler != null && winHandler.onKeyEvent(event);
        boolean handledByXServerKeyboard = xServer != null && xServer.keyboard != null && xServer.keyboard.onKeyEvent(event);
        boolean handledBySuper = !ExternalController.isGameController(event.getDevice()) && super.dispatchKeyEvent(event);

        return handledByInputControls || handledByWinHandler || handledByXServerKeyboard || handledBySuper;
    }

    public InputControlsView getInputControlsView() {
        return inputControlsView;
    }

    private static final String TAG = "DXWrapperExtraction";

    private void extractDXWrapperFiles(String dxwrapper) {
        final String[] dlls = {"d3d10.dll", "d3d10_1.dll", "d3d10core.dll", "d3d11.dll", "d3d12.dll", "d3d12core.dll", "d3d8.dll", "d3d9.dll", "dxgi.dll", "ddraw.dll", "d3dimm.dll"};

        File rootDir = imageFs.getRootDir();
        File windowsDir = new File(rootDir, ImageFs.WINEPREFIX + "/drive_c/windows");
        cleanupDgVoodooRuntimeStage(rootDir);

        if (dxwrapper.contains("dxvk")) {
            Log.d(TAG, "Extracting DXVK wrapper files, version: " + dxwrapper);

            String[] wrapperParts = dxwrapper.split(";");
            String dxvkWrapper = wrapperParts.length > 0 ? wrapperParts[0] : "";
            String vkd3dWrapper = wrapperParts.length > 1 ? wrapperParts[1] : "vkd3d-None";
            ContentProfile dxvkProfile = contentsManager.getProfileByEntryName(dxvkWrapper);
            if (dxvkProfile != null) {
                Log.d(TAG, "Applying user-defined DXVK content profile: " + dxvkWrapper);
                contentsManager.applyContent(dxvkProfile);
            } else {
                Log.d(TAG, "Extracting fallback DXVK .tzst archive: " + dxvkWrapper);
                TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "dxwrapper/" + dxvkWrapper + ".tzst", windowsDir, onExtractFileListener);

                if (compareVersion(dxvkWrapper, "2.4") < 0) {
                    Log.d(TAG, "Extracting d8vk as part of DXVK version " + dxvkWrapper);
                    TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "dxwrapper/d8vk-" + DefaultVersion.D8VK + ".tzst", windowsDir, onExtractFileListener);
                }
            }

            if (vkd3dWrapper.contains("None")) {
                Log.d(TAG, "No VKD3D has been selected, restoring original d3d12");
                restoreOriginalDllFiles(new String[]{"d3d12.dll", "d3d12core.dll"});
            }
            else {
                ContentProfile vkd3dProfile = contentsManager.getProfileByEntryName(vkd3dWrapper);
                if (vkd3dProfile != null) {
                    Log.d(TAG, "Applying user-defined VKD3D content profile: " + vkd3dWrapper);
                    contentsManager.applyContent(vkd3dProfile);
                } else {
                    Log.d(TAG, "Extracting fallback VKD3D .tzst archive: " + vkd3dWrapper);
                    TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "dxwrapper/" + vkd3dWrapper + ".tzst", windowsDir, onExtractFileListener);
                }
            }

            // Legacy DDraw wrapper payloads are deprecated in DXVK lane.
            // DDraw/D3D1-7/Glide routing is handled by dedicated dgVoodoo lanes when selected.
            restoreOriginalDllFiles(new String[]{ "ddraw.dll", "d3dimm.dll" });

            Log.d(TAG, "Finished extraction of DXVK wrapper files, version: " + dxwrapper);
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "DXWRAPPER_RUNTIME_STAGE_READY",
                    null,
                    "dxwrapper",
                    "dxvk_runtime_stage_ready",
                    ForensicLogger.fields(
                            "dxwrapper", dxwrapper,
                            "dxvk_wrapper", dxvkWrapper,
                            "vkd3d_wrapper", vkd3dWrapper
                    )
            );
        } else if (dxwrapper.contains("dgvoodoo")) {
            Log.d(TAG, "Staging dgVoodoo runtime for legacy API route.");
            restoreOriginalDllFiles(dlls);

            DgVoodooManager manager = new DgVoodooManager(this);
            String shortcutPath = shortcut != null ? shortcut.path : "";
            KeyValueSet config = DgVoodooConfigDialog.parseConfig(dxwrapperConfig);
            File stageTarget = manager.resolveShortcutTargetDir(rootDir, shortcutPath);
            if (stageTarget == null || !stageTarget.isDirectory()) {
                stageTarget = new File(windowsDir, "system32");
            }

            String activeArch = manager.resolvePreferredArch(shortcutPath, config.get("dgvoodooArch"));
            boolean staged = manager.stageRuntime(stageTarget, activeArch);
            envVars.put("AERO_DGVOODOO_STAGE_TARGET", stageTarget.getAbsolutePath());
            envVars.put("AERO_DGVOODOO_ARCH_ACTIVE", activeArch);
            envVars.put("AERO_DGVOODOO_STAGE_READY", staged ? "1" : "0");
            if (!staged) {
                Log.w(TAG, "dgVoodoo runtime stage failed for target " + stageTarget.getAbsolutePath());
            }
            ForensicLogger.logEvent(
                    this,
                    staged ? "info" : "warn",
                    "DXWRAPPER_RUNTIME_STAGE_READY",
                    null,
                    "dxwrapper",
                    staged ? "dgvoodoo_runtime_stage_ready" : "dgvoodoo_runtime_stage_failed",
                    ForensicLogger.fields(
                            "dxwrapper", dxwrapper,
                            "stage_target", stageTarget.getAbsolutePath(),
                            "arch_active", activeArch,
                            "stage_ready", staged ? "1" : "0"
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

    private void cleanupDgVoodooRuntimeStage(File rootDir) {
        if (rootDir == null || shortcut == null) return;
        DgVoodooManager manager = new DgVoodooManager(this);
        File stageTarget = manager.resolveShortcutTargetDir(rootDir, shortcut.path);
        if (stageTarget != null) manager.cleanupStagedRuntime(stageTarget);
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
        File windowsDir = new File(rootDir, ImageFs.WINEPREFIX+"/drive_c/windows");
        File systemRegFile = new File(rootDir, ImageFs.WINEPREFIX+"/system.reg");

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
        catch (JSONException e) {}
    }

    private void restoreOriginalDllFiles(final String... dlls) {
        File rootDir = imageFs.getRootDir();
        File windowsDir = new File(rootDir, ImageFs.WINEPREFIX+"/drive_c/windows");
        File system32dlls = null;
        File syswow64dlls = null;

        if (wineInfo.isArm64EC())
            system32dlls = new File(imageFs.getWinePath() + "/lib/wine/aarch64-windows");
        else
            system32dlls = new File(imageFs.getWinePath() + "/lib/wine/x86_64-windows");

        syswow64dlls = new File(imageFs.getWinePath() + "/lib/wine/i386-windows");


        for (String dll : dlls) {
            File srcFile = new File(system32dlls, dll);
            File dstFile = new File(windowsDir, "system32/" + dll);
            FileUtils.copy(srcFile, dstFile);
            srcFile = new File(syswow64dlls, dll);
            dstFile = new File(windowsDir, "syswow64/" + dll);
            FileUtils.copy(srcFile, dstFile);
        }
   }

    private String getWineStartCommand() {
        // Initialize overrideEnvVars if not already done
        EnvVars envVars = getOverrideEnvVars();
        boolean directDesktopShellBootstrap = shortcut == null
                && wineInfo != null
                && wineInfo.isArm64EC()
                && !envVars.has("EXTRA_EXEC_ARGS");

        // Define default arguments
        String args = "";

        if (shortcut != null) {
            String execArgs = shortcut.getExtra("execArgs");
            execArgs = !execArgs.isEmpty() ? " " + execArgs : "";

            if (shortcut.path.endsWith(".lnk")) {
                args += "\"" + shortcut.path + "\"" + execArgs;
            } else {
                String exeDir = FileUtils.getDirname(shortcut.path);
                String filename = FileUtils.getName(shortcut.path);

                int dotIndex = filename.lastIndexOf(".");
                int spaceIndex = (dotIndex != -1) ? filename.indexOf(" ", dotIndex) : -1;

                if (spaceIndex != -1) {
                    execArgs = filename.substring(spaceIndex + 1) + execArgs;
                    filename = filename.substring(0, spaceIndex);
                }

                args += "/dir " + StringUtils.escapeDOSPath(exeDir) + " \"" + filename + "\"" + execArgs;
            }
        } else {
            // Append EXTRA_EXEC_ARGS from overrideEnvVars if it exists
            if (envVars.has("EXTRA_EXEC_ARGS")) {
                args += " " + envVars.get("EXTRA_EXEC_ARGS");
                envVars.remove("EXTRA_EXEC_ARGS"); // Remove the key after use
            } else if (directDesktopShellBootstrap) {
                // Let Wine own the virtual desktop shell directly for clean container boot.
                args = "";
            } else {
                args += "\"wfm.exe\"";
            }
        }
        // ARM64EC desktop bootstrap is more stable when the shell starts directly,
        // bypassing the intermediate winhandler wrapper that trips libarm64ecfex.
        String command = directDesktopShellBootstrap ? args.trim() : "winhandler.exe " + args;

        return command;
    }

    private void configureDesktopShellRegistry() {
        if (container == null || xServer == null) return;
        File userRegFile = new File(container.getRootDir(), ".wine/user.reg");
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

    private String getExecutable() {
        String filename = "";
        if (shortcut != null) {
            filename = FileUtils.getName(shortcut.path);
        }
        else {
            boolean directDesktopShellBootstrap = wineInfo != null
                    && wineInfo.isArm64EC()
                    && !getOverrideEnvVars().has("EXTRA_EXEC_ARGS");
            filename = directDesktopShellBootstrap ? "explorer.exe" : "wfm.exe";
        }
        return filename;
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
            File rootDir = imageFs.getRootDir();
            File userRegFile = new File(rootDir, ImageFs.WINEPREFIX+"/user.reg");
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
        TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "container_pattern_common.tzst", rootDir);
        TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "pulseaudio.tzst", new File(getFilesDir(), "pulseaudio"));
        WineUtils.applySystemTweaks(this, wineInfo);
        container.putExtra("graphicsDriver", null);
        container.putExtra("desktopTheme", null);
    }

    private void assignTaskAffinity(Window window) {
        if (taskAffinityMask == 0 || taskAffinityMaskWoW64 == 0) return;
        int processId = window.getProcessId();
        String className = window.getClassName();
        int processAffinity = window.isWoW64() ? taskAffinityMaskWoW64 : taskAffinityMask;

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
