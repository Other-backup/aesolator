package com.winlator.cmod.winhandler;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.CompoundButtonCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.winlator.cmod.R;
import com.winlator.cmod.XServerDisplayActivity;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.contentdialog.ContentDialog;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.CPUStatus;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.core.ProcessHelper;
import com.winlator.cmod.core.SpinnerAdapters;
import com.winlator.cmod.core.StringUtils;
import com.winlator.cmod.widget.CPUListView;
import com.winlator.cmod.xenvironment.ImageFs;
import com.winlator.cmod.xserver.Window;
import com.winlator.cmod.xserver.XLock;
import com.winlator.cmod.xserver.XServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class TaskManagerDialog extends ContentDialog implements OnGetProcessInfoListener {
    private static final int TAB_WINDOWS = 0;
    private static final int TAB_LINUX = 1;
    private static final int MAX_LINUX_ROWS = 80;
    private static final int MAX_WINDOWS_THREAD_PREVIEW = 12;
    private static final long TASKMGR_REFRESH_INTERVAL_MS = 1000L;
    private static final long TASKMGR_REFRESH_LOG_INTERVAL_MS = 10000L;
    private static final String WINDOWS_SORT_MEMORY_DESC = "memory_desc";
    private static final String WINDOWS_SORT_NAME_ASC = "name_asc";
    private static final String WINDOWS_SORT_PID_ASC = "pid_asc";
    private static final String WINDOWS_SORT_ARCH_LANE = "arch_lane";
    private static final int ARCH_FILTER_ALL = 0;
    private static final int ARCH_FILTER_WOW64 = 1;
    private static final int ARCH_FILTER_ARM64EC = 2;
    private static final int ARCH_FILTER_NATIVE = 3;
    private static final String PREF_WINDOWS_WINDOWED_ONLY = "taskmgr_windows_windowed_only";
    private static final String PREF_WINDOWS_SORT_MODE = "taskmgr_windows_sort_mode";
    private static final String PREF_WINDOWS_ARCH_FILTER = "taskmgr_windows_arch_filter";

    private static final String[] RUNTIME_HINT_TOKENS = new String[] {
            "wine", "wineserver", "freewine", "box64", "box86", "proton",
            "winhandler", "explorer.exe", "services.exe", ".exe", "aeolator", "aesolator"
    };

    private final XServerDisplayActivity activity;
    private final LayoutInflater inflater;
    private final LinuxTelemetrySampler linuxTelemetrySampler = new LinuxTelemetrySampler();
    private final ExecutorService telemetryExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean telemetryInFlight = new AtomicBoolean(false);
    private final ArrayList<WindowsProcessEntry> windowsPending = new ArrayList<>();
    private final boolean arm64ecRuntime;
    private Timer timer;
    private final Object lock = new Object();
    private int selectedTab = TAB_LINUX;
    private int lastWindowsTotal = 0;
    private int lastWindowsVisible = 0;
    private int lastLinuxTotal = 0;
    private int lastLinuxVisible = 0;
    private String windowsSearchQuery = "";
    private String windowsSortMode = WINDOWS_SORT_MEMORY_DESC;
    private int windowsArchFilterMode = ARCH_FILTER_ALL;
    private LinuxTelemetrySampler.HostSample lastHostSample;
    private long lastRefreshLogAtMs = 0L;
    private int lastLoggedWindowsVisible = -1;
    private int lastLoggedWindowsTotal = -1;
    private int lastLoggedLinuxVisible = -1;
    private int lastLoggedLinuxTotal = -1;
    private boolean lastWindowsPathSupport = false;
    private int lastWindowsRenderedRows = -1;
    private int lastLinuxRenderedRows = -1;
    private int linuxInitialScrollResetsRemaining = 1;
    private final ArrayList<LinuxTelemetrySampler.ProcessSample> currentLinuxSamples = new ArrayList<>();
    private LinuxTelemetrySampler.ProcessSample selectedLinuxSample;
    private int selectedLinuxPid = -1;
    private RecyclerView linuxRecyclerView;
    private LinuxProcessAdapter linuxProcessAdapter;
    private LastLinuxRowInsetDecoration lastLinuxRowInsetDecoration;
    public TaskManagerDialog(XServerDisplayActivity activity) {
        super(activity, R.layout.task_manager_dialog);
        this.activity = activity;
        this.arm64ecRuntime = detectArm64ecRuntime(activity);
        setCancelable(false);
        setTitle(R.string.task_manager);
        setIcon(R.drawable.ae_icon_task_manager);

        Button cancelButton = findViewById(R.id.BTCancel);
        if (cancelButton != null) cancelButton.setVisibility(View.GONE);
        Button confirmButton = findViewById(R.id.BTConfirm);
        if (confirmButton != null) confirmButton.setVisibility(View.GONE);

        Button runtimeRunCommandButton = findViewById(R.id.BTRuntimeRunCommand);
        if (runtimeRunCommandButton != null) {
            runtimeRunCommandButton.setOnClickListener((v) -> {
                dismiss();
                showRuntimeRunCommandDialog();
            });
        }
        Button runtimeCloseButton = findViewById(R.id.BTRuntimeClose);
        if (runtimeCloseButton != null) {
            runtimeCloseButton.setOnClickListener((v) -> dismiss());
        }

        setupLinuxRuntimeControls();
        setupLinuxRecyclerView();
        bindLinuxActionButtons();
        applyThemeState();
        applyHostTelemetryViews(null);

        setOnDismissListener((dialog) -> {
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
            telemetryExecutor.shutdownNow();
            activity.getWinHandler().setOnGetProcessInfoListener(null);
            ForensicLogger.logEvent(
                    activity,
                    "info",
                    "TASKMGR_CLOSE",
                    null,
                    "task_manager",
                    "Task Manager closed",
                    ForensicLogger.fields(
                            "selected_tab", selectedTab == TAB_WINDOWS ? "windows" : "linux",
                            "windows_visible", lastWindowsVisible,
                            "windows_total", lastWindowsTotal,
                            "linux_visible", lastLinuxVisible,
                            "linux_total", lastLinuxTotal
                    )
            );
        });

        FileUtils.clear(getIconDir(activity));
        inflater = LayoutInflater.from(activity);
    }

    private void update() {
        synchronized (lock) {
            refreshLinuxProcessPanelAsync();
        }

        refreshHostTelemetryAsync();
        updateCPUInfoView();
        updateMemoryInfoView();
    }

    private void setupLinuxRuntimeControls() {
        CheckBox linuxRuntimeOnly = findViewById(R.id.CBLinuxRuntimeOnly);
        if (linuxRuntimeOnly != null) {
            linuxRuntimeOnly.setOnCheckedChangeListener((buttonView, isChecked) -> update());
        }
    }

    private void setupLinuxRecyclerView() {
        linuxRecyclerView = findViewById(R.id.RVLinuxProcessList);
        if (linuxRecyclerView == null) return;
        linuxRecyclerView.setLayoutManager(new LinearLayoutManager(activity));
        linuxRecyclerView.setItemAnimator(null);
        linuxRecyclerView.setHasFixedSize(false);
        linuxRecyclerView.setClipToPadding(false);
        linuxRecyclerView.setPadding(
                linuxRecyclerView.getPaddingLeft(),
                0,
                linuxRecyclerView.getPaddingRight(),
                0
        );
        linuxRecyclerView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        lastLinuxRowInsetDecoration = new LastLinuxRowInsetDecoration(dp(0));
        linuxRecyclerView.addItemDecoration(lastLinuxRowInsetDecoration);
        linuxProcessAdapter = new LinuxProcessAdapter();
        linuxRecyclerView.setAdapter(linuxProcessAdapter);
    }

    private void showRuntimeRunCommandDialog() {
        ContentDialog dialog = new ContentDialog(activity);
        dialog.setTitle(R.string.new_task);
        EditText editText = dialog.findViewById(R.id.EditText);
        if (editText != null) {
            editText.setHint(R.string.untitled);
            editText.setText("taskmgr.exe");
            editText.setVisibility(View.VISIBLE);
            editText.setTextColor(ContextCompat.getColor(activity, R.color.surface_runtime_taskmgr_text));
            editText.setHintTextColor(ContextCompat.getColor(activity, R.color.surface_runtime_taskmgr_muted));
            editText.setBackgroundResource(R.drawable.surface_runtime_taskmgr_input_background);
        }
        dialog.setOnConfirmCallback(() -> {
            if (editText == null) return;
            String command = editText.getText() != null ? editText.getText().toString().trim() : "";
            if (!command.isEmpty()) {
                activity.getWinHandler().exec(command);
            }
        });
        dialog.show();
        styleTaskManagerNestedDialog(dialog);
    }

    private void bindLinuxActionButtons() {
        bindLinuxActionButton(R.id.BTLinuxInspect, () -> {
            LinuxTelemetrySampler.ProcessSample sample = requireSelectedLinuxSample();
            if (sample != null) showLinuxProcessDetails(sample);
        });
        bindLinuxActionButton(R.id.BTLinuxPauseResume, () -> {
            LinuxTelemetrySampler.ProcessSample sample = requireSelectedLinuxSample();
            if (sample == null) return;
            try {
                if (isSuspended(sample)) {
                    ProcessHelper.resumeProcess(sample.pid);
                    logLinuxProcessAction("resume_process", sample);
                    AppUtils.showToast(activity, activity.getString(R.string.task_manager_action_resume_done, sample.commandName));
                } else {
                    ProcessHelper.suspendProcess(sample.pid);
                    logLinuxProcessAction("suspend_process", sample);
                    AppUtils.showToast(activity, activity.getString(R.string.task_manager_action_pause_done, sample.commandName));
                }
                update();
            } catch (Exception e) {
                AppUtils.showToast(activity, activity.getString(R.string.task_manager_action_failed, sample.commandName));
            }
        });
        bindLinuxActionButton(R.id.BTLinuxTerminate, () -> {
            LinuxTelemetrySampler.ProcessSample sample = requireSelectedLinuxSample();
            if (sample == null) return;
            showTaskManagerConfirmDialog(
                    activity.getString(R.string.task_manager_confirm_terminate, sample.commandName, sample.pid),
                    () -> {
                        try {
                            ProcessHelper.terminateProcess(sample.pid);
                            logLinuxProcessAction("terminate_process", sample);
                            AppUtils.showToast(activity, activity.getString(R.string.task_manager_action_terminate_done, sample.commandName));
                            update();
                        } catch (Exception e) {
                            AppUtils.showToast(activity, activity.getString(R.string.task_manager_action_failed, sample.commandName));
                        }
                    }
            );
        });
        bindLinuxActionButton(R.id.BTLinuxKill, () -> {
            LinuxTelemetrySampler.ProcessSample sample = requireSelectedLinuxSample();
            if (sample == null) return;
            showTaskManagerConfirmDialog(
                    activity.getString(R.string.task_manager_confirm_kill, sample.commandName, sample.pid),
                    () -> {
                        try {
                            ProcessHelper.killProcess(sample.pid);
                            logLinuxProcessAction("kill_process", sample);
                            AppUtils.showToast(activity, activity.getString(R.string.task_manager_action_kill_done, sample.commandName));
                            update();
                        } catch (Exception e) {
                            AppUtils.showToast(activity, activity.getString(R.string.task_manager_action_failed, sample.commandName));
                        }
                    }
            );
        });
    }

    private void showTaskManagerConfirmDialog(String message, Runnable action) {
        ContentDialog dialog = new ContentDialog(activity);
        dialog.setMessage(message);
        dialog.setOnConfirmCallback(action);
        dialog.show();
        styleTaskManagerNestedDialog(dialog);
    }

    private void bindLinuxActionButton(int buttonId, Runnable action) {
        Button button = findViewById(buttonId);
        if (button == null) return;
        button.setOnClickListener(v -> action.run());
        button.setFocusable(false);
        button.setFocusableInTouchMode(false);
    }

    private void showListItemMenu(final View anchorView, final ProcessInfo processInfo) {
        PopupMenu listItemMenu = new PopupMenu(activity, anchorView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) listItemMenu.setForceShowIcon(true);

        listItemMenu.inflate(R.menu.process_popup_menu);
        listItemMenu.setOnMenuItemClickListener((menuItem) -> {
            int itemId = menuItem.getItemId();
            final WinHandler winHandler = activity.getWinHandler();
            if (itemId == R.id.process_affinity) {
                logProcessAction("set_affinity_open", processInfo);
                showProcessorAffinityDialog(processInfo);
            }
            else if (itemId == R.id.bring_to_front) {
                logProcessAction("bring_to_front", processInfo);
                winHandler.bringToFront(processInfo.name);
                dismiss();
            }
            else if (itemId == R.id.process_end) {
                ContentDialog.confirm(activity, R.string.do_you_want_to_end_this_process, () -> {
                    logProcessAction("kill_process", processInfo);
                    winHandler.killProcess(processInfo.name);
                });
            }
            return true;
        });
        listItemMenu.show();
    }

    private void showProcessorAffinityDialog(final ProcessInfo processInfo) {
        ContentDialog dialog = new ContentDialog(activity, R.layout.cpu_list_dialog);
        dialog.setTitle(processInfo.name);
        dialog.setIcon(R.drawable.ae_icon_cpu);
        final CPUListView cpuListView = dialog.findViewById(R.id.CPUListView);
        cpuListView.setCheckedCPUList(processInfo.getCPUList());
        dialog.setOnConfirmCallback(() -> {
            WinHandler winHandler = activity.getWinHandler();
            logProcessAction("set_affinity_apply", processInfo);
            winHandler.setProcessAffinity(processInfo.pid, ProcessHelper.getAffinityMask(cpuListView.getCheckedCPUList()));
            update();
        });
        dialog.show();
    }

    public static File getIconDir(Context context) {
        File iconDir = new File(ImageFs.find(context).getHomeDir(), ".local/share/icons/taskmgr");
        if (!iconDir.isDirectory()) iconDir.mkdirs();
        return iconDir;
    }

    @Override
    public void show() {
        ForensicLogger.logEvent(
                activity,
                "info",
                "TASKMGR_OPEN",
                null,
                "task_manager",
                "Task Manager opened",
                ForensicLogger.fields("arm64ec_runtime", arm64ecRuntime)
        );
        update();
        linuxInitialScrollResetsRemaining = 1;
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                activity.runOnUiThread(TaskManagerDialog.this::update);
            }
        }, 0, TASKMGR_REFRESH_INTERVAL_MS);
        super.show();
        int screenWidth = AppUtils.getScreenWidth();
        int screenHeight = AppUtils.getScreenHeight();
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(0));
            getWindow().setLayout(
                    Math.round(screenWidth * 0.996f),
                    Math.round(screenHeight * 0.978f)
            );
        }
        ViewGroup.LayoutParams rootParams = getContentView().getLayoutParams();
        if (rootParams != null) {
            rootParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            rootParams.height = Math.round(screenHeight * 0.972f);
            getContentView().setLayoutParams(rootParams);
        }
        getContentView().setMinimumHeight(Math.round(screenHeight * 0.972f));
        compactDialogChrome();
        getContentView().post(this::logTaskManagerLayoutReady);
    }

    private void compactDialogChrome() {
        getContentView().setPadding(dp(7), dp(5), dp(7), dp(5));
        getContentView().setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
        View frameLayout = getContentView().findViewById(R.id.FrameLayout);
        if (frameLayout != null) {
            frameLayout.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
        }
        View titleBar = getContentView().findViewById(R.id.LLTitleBar);
        if (titleBar != null) {
            titleBar.setVisibility(View.GONE);
        }
        View bottomBar = getContentView().findViewById(R.id.LLBottomBar);
        if (bottomBar != null) {
            bottomBar.setVisibility(View.GONE);
        }
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                activity.getResources().getDisplayMetrics()
        ));
    }

    private void logTaskManagerLayoutReady() {
        View body = findViewById(R.id.LLTaskManagerBody);
        View titleBar = getContentView().findViewById(R.id.LLTitleBar);
        View bottomBar = getContentView().findViewById(R.id.LLBottomBar);
        View windowsViewport = findViewById(R.id.FLWindowsProcessViewport);
        View linuxViewport = findViewById(R.id.FLLinuxProcessViewport);
        ScrollView windowsScroll = findViewById(R.id.SVWindowsProcessList);
        RecyclerView linuxList = findViewById(R.id.RVLinuxProcessList);
        ForensicLogger.logEvent(
                activity,
                "info",
                "TASKMGR_LAYOUT_READY",
                null,
                "task_manager",
                "Task Manager layout prepared",
                ForensicLogger.fields(
                        "root_height", getContentView().getHeight(),
                        "body_height", body != null ? body.getHeight() : -1,
                        "title_height", titleBar != null ? titleBar.getHeight() : -1,
                        "bottom_height", bottomBar != null ? bottomBar.getHeight() : -1,
                        "windows_viewport_height", windowsViewport != null ? windowsViewport.getHeight() : -1,
                        "linux_viewport_height", linuxViewport != null ? linuxViewport.getHeight() : -1,
                        "windows_scroll_height", windowsScroll != null ? windowsScroll.getHeight() : -1,
                        "linux_scroll_height", linuxList != null ? linuxList.getHeight() : -1
                )
        );
    }

    @Override
    public void onGetProcessInfo(int index, int numProcesses, ProcessInfo processInfo) {
        activity.runOnUiThread(() -> {
            synchronized (lock) {
                if (!isShowing() || selectedTab != TAB_WINDOWS) return;
                if (index == 0) {
                    windowsPending.clear();
                    lastWindowsTotal = numProcesses;
                    lastWindowsPathSupport = false;
                }

                if (numProcesses == 0) {
                    lastWindowsVisible = 0;
                    windowsPending.clear();
                    renderWindowsProcessRows();
                    updateBottomBarSummary();
                    return;
                }

                if (processInfo == null) {
                    renderWindowsProcessRows();
                    updateBottomBarSummary();
                    finalizeWindowsList(index, numProcesses);
                    return;
                }

                XServer xServer = activity.getXServer();
                Window window;
                if (processInfo.path != null && !processInfo.path.trim().isEmpty()) {
                    lastWindowsPathSupport = true;
                }

                try (XLock xlock = xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
                    window = xServer.windowManager.findWindowWithProcessId(processInfo.pid);
                }

                windowsPending.add(new WindowsProcessEntry(processInfo, window, resolveArchLane(processInfo)));
                renderWindowsProcessRows();
                updateBottomBarSummary();
                finalizeWindowsList(index, numProcesses);
            }
        });
    }

    private void finalizeWindowsList(int index, int numProcesses) {
        if (index != numProcesses - 1) return;
        renderWindowsProcessRows();
        updateBottomBarSummary();
        maybeLogRefreshCycle("windows");
    }

    private void renderWindowsProcessRows() {
        final LinearLayout container = findViewById(R.id.LLProcessList);
        ArrayList<WindowsProcessEntry> rows = new ArrayList<>();
        for (WindowsProcessEntry entry : windowsPending) {
            if (isWindowsOnlyWindowedEnabled() && !entry.windowed) continue;
            if (!matchesArchFilter(entry)) continue;
            if (!matchesWindowsQuery(entry)) continue;
            rows.add(entry);
        }
        if (rows.isEmpty() && !windowsPending.isEmpty()) {
            rows.addAll(windowsPending);
        }
        sortWindowsRows(rows);
        bindWindowsProcessRows(rows);
    }

    private void bindWindowsProcessRows(ArrayList<WindowsProcessEntry> rows) {
        final LinearLayout container = findViewById(R.id.LLProcessList);
        int childCount = container.getChildCount();
        for (int i = 0; i < rows.size(); i++) {
            View itemView = i < childCount
                    ? container.getChildAt(i)
                    : inflater.inflate(R.layout.process_info_list_item, container, false);
            bindWindowsProcessRow(itemView, rows.get(i));
            if (i >= childCount) container.addView(itemView);
        }
        for (int i = container.getChildCount() - 1; i >= rows.size(); i--) {
            container.removeViewAt(i);
        }

        lastWindowsVisible = rows.size();
        findViewById(R.id.TVEmptyWindowsText).setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        TextView titleView = findViewById(R.id.TVWindowsListTitle);
        if (titleView != null) {
            titleView.setText(activity.getString(R.string.task_manager_windows_list_title) + "  " + rows.size() + "/" + lastWindowsTotal);
        }
        maybeLogRenderedRows("windows", rows.size());
    }

    private void bindWindowsProcessRow(View itemView, WindowsProcessEntry entry) {
        ProcessInfo processInfo = entry.processInfo;
        LinuxTelemetrySampler.ProcessSample runtimeSample = linuxTelemetrySampler.sampleProcess(processInfo.pid);
        String cpuPercent = runtimeSample != null ? formatPercent(runtimeSample.cpuPercent) : "--";

        ((TextView) itemView.findViewById(R.id.TVName)).setText(processInfo.name);
        ((TextView) itemView.findViewById(R.id.TVArchLane)).setText(entry.archLane);
        ((TextView) itemView.findViewById(R.id.TVPID)).setText(String.valueOf(processInfo.pid));
        ((TextView) itemView.findViewById(R.id.TVCPU)).setText(cpuPercent);
        ((TextView) itemView.findViewById(R.id.TVMemoryUsage)).setText(processInfo.getFormattedMemoryUsage());
        ((TextView) itemView.findViewById(R.id.TVIO)).setText(runtimeSample != null ? formatCompactIoRate(runtimeSample) : "--");
        ((TextView) itemView.findViewById(R.id.TVDetail)).setText(buildWindowsDetailLine(entry, processInfo, runtimeSample));
        itemView.findViewById(R.id.BTMenu).setOnClickListener((v) -> showListItemMenu(v, processInfo));
        itemView.findViewById(R.id.BTQuickEnd).setOnClickListener(v -> ContentDialog.confirm(
                activity,
                R.string.do_you_want_to_end_this_process,
                () -> {
                    logProcessAction("kill_process_quick", processInfo);
                    activity.getWinHandler().killProcess(processInfo.name);
                }
        ));
        itemView.setOnClickListener(v -> showWindowsProcessDetails(entry));
        itemView.setOnLongClickListener(v -> {
            showListItemMenu(v, processInfo);
            return true;
        });

        ImageView ivIcon = itemView.findViewById(R.id.IVIcon);
        ivIcon.setImageResource(R.drawable.taskmgr_process);
        if (entry.window != null) {
            Bitmap icon = activity.getXServer().pixmapManager.getWindowIcon(entry.window);
            if (icon != null) {
                ivIcon.clearColorFilter();
                ivIcon.setImageBitmap(icon);
            } else {
                ivIcon.setColorFilter(ContextCompat.getColor(activity, R.color.surface_runtime_taskmgr_text));
            }
        } else {
            ivIcon.setColorFilter(ContextCompat.getColor(activity, R.color.surface_runtime_taskmgr_text));
        }
    }

    private String buildWindowsDetailLine(WindowsProcessEntry entry, ProcessInfo processInfo, LinuxTelemetrySampler.ProcessSample runtimeSample) {
        String threadCount = runtimeSample != null ? String.valueOf(runtimeSample.threadCount) : "--";
        String windowTitle = entry.window != null ? safeValue(entry.window.getName()) : "";
        if (!windowTitle.isEmpty()) {
            return "THR " + threadCount + "  |  " + windowTitle;
        }
        if (processInfo.path != null && !processInfo.path.trim().isEmpty()) {
            return "THR " + threadCount + "  |  " + processInfo.path;
        }
        if (runtimeSample != null && runtimeSample.commandLine != null && !runtimeSample.commandLine.trim().isEmpty()) {
            return "THR " + threadCount + "  |  " + runtimeSample.commandLine;
        }
        return "THR " + threadCount + "  |  " + activity.getString(R.string.task_manager_linux_details_not_available);
    }

    private void sortWindowsRows(ArrayList<WindowsProcessEntry> rows) {
        switch (windowsSortMode) {
            case WINDOWS_SORT_NAME_ASC:
                rows.sort((left, right) -> left.processInfo.name.compareToIgnoreCase(right.processInfo.name));
                break;
            case WINDOWS_SORT_PID_ASC:
                rows.sort(Comparator.comparingInt(left -> left.processInfo.pid));
                break;
            case WINDOWS_SORT_ARCH_LANE:
                rows.sort((left, right) -> {
                    int laneCompare = archLaneRank(left.archLane) - archLaneRank(right.archLane);
                    if (laneCompare != 0) return laneCompare;
                    return left.processInfo.name.compareToIgnoreCase(right.processInfo.name);
                });
                break;
            case WINDOWS_SORT_MEMORY_DESC:
            default:
                rows.sort((left, right) -> Long.compare(right.processInfo.memoryUsage, left.processInfo.memoryUsage));
                break;
        }
    }

    private int archLaneRank(String archLane) {
        if (activity.getString(R.string.task_manager_arch_wow64).equals(archLane)) return 0;
        if (activity.getString(R.string.task_manager_arch_arm64ec).equals(archLane)) return 1;
        return 2;
    }

    private boolean matchesWindowsQuery(WindowsProcessEntry entry) {
        String query = windowsSearchQuery;
        if (query == null || query.isEmpty()) return true;
        String windowTitle = entry.window != null ? safeValue(entry.window.getName()) : "";
        String windowClass = entry.window != null ? safeValue(entry.window.getClassName()) : "";
        String haystack = (
                entry.processInfo.name + " "
                + entry.processInfo.path + " "
                + entry.processInfo.pid + " "
                + entry.archLane + " "
                + windowTitle + " "
                + windowClass
        ).toLowerCase(Locale.ENGLISH);
        return haystack.contains(query);
    }

    private boolean matchesArchFilter(WindowsProcessEntry entry) {
        switch (windowsArchFilterMode) {
            case ARCH_FILTER_WOW64:
                return entry.processInfo.wow64Process;
            case ARCH_FILTER_ARM64EC:
                return !entry.processInfo.wow64Process && arm64ecRuntime;
            case ARCH_FILTER_NATIVE:
                return !entry.processInfo.wow64Process && !arm64ecRuntime;
            case ARCH_FILTER_ALL:
            default:
                return true;
        }
    }

    private void showWindowsProcessDetails(WindowsProcessEntry entry) {
        ProcessInfo processInfo = entry.processInfo;
        LinuxTelemetrySampler.ProcessSample runtimeSample = linuxTelemetrySampler.sampleProcess(processInfo.pid);
        String threadPreview = buildWindowsThreadPreview(processInfo.pid, MAX_WINDOWS_THREAD_PREVIEW);
        ContentDialog dialog = new ContentDialog(activity);
        dialog.setTitle(activity.getString(R.string.task_manager_windows_details_title, processInfo.pid));

        Window focusedWindow = null;
        if (entry.window != null) {
            XServer xServer = activity.getXServer();
            try (XLock xlock = xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
                focusedWindow = xServer.windowManager.getFocusedWindow();
            }
        }

        boolean isForeground = false;
        if (focusedWindow != null) {
            isForeground = focusedWindow.getProcessId() == processInfo.pid
                    || focusedWindow == entry.window
                    || (entry.window != null && entry.window.isAncestorOf(focusedWindow));
        }

        String yesNo = isForeground ? activity.getString(R.string.task_manager_yes) : activity.getString(R.string.task_manager_no);
        StringBuilder details = new StringBuilder();
        details.append(activity.getString(R.string.task_manager_windows_details_process)).append(": ")
                .append(processInfo.name).append('\n');
        details.append(activity.getString(R.string.task_manager_windows_details_path)).append(": ")
                .append(safeValue(processInfo.path)).append('\n');
        details.append(activity.getString(R.string.task_manager_windows_details_arch)).append(": ")
                .append(entry.archLane).append('\n');
        details.append(activity.getString(R.string.task_manager_windows_details_memory)).append(": ")
                .append(processInfo.getFormattedMemoryUsage()).append('\n');
        details.append(activity.getString(R.string.task_manager_windows_details_foreground)).append(": ")
                .append(yesNo).append('\n');
        details.append(activity.getString(R.string.cpu_short)).append(": ")
                .append(runtimeSample != null ? formatPercent(runtimeSample.cpuPercent) : activity.getString(R.string.task_manager_linux_details_not_available)).append('\n');
        details.append(activity.getString(R.string.task_manager_linux_details_threads)).append(": ")
                .append(runtimeSample != null ? String.valueOf(runtimeSample.threadCount) : activity.getString(R.string.task_manager_linux_details_not_available)).append('\n');
        details.append(activity.getString(R.string.task_manager_linux_details_io_rate)).append(": ")
                .append(runtimeSample != null ? formatIoRate(runtimeSample) : activity.getString(R.string.task_manager_linux_details_not_available)).append('\n');
        details.append(activity.getString(R.string.task_manager_linux_details_state)).append(": ")
                .append(runtimeSample != null ? runtimeSample.state : activity.getString(R.string.task_manager_linux_details_not_available)).append('\n');
        details.append(activity.getString(R.string.task_manager_linux_details_cmd)).append(": ")
                .append(runtimeSample != null ? safeValue(runtimeSample.commandLine) : activity.getString(R.string.task_manager_linux_details_not_available)).append('\n');
        details.append(activity.getString(R.string.task_manager_windows_details_threads_preview)).append(":\n")
                .append(threadPreview).append('\n');

        if (entry.window != null) {
            details.append(activity.getString(R.string.task_manager_windows_details_window_status)).append(": ")
                    .append(activity.getString(R.string.task_manager_windows_details_window_present)).append('\n');
            details.append(activity.getString(R.string.task_manager_windows_details_window_title)).append(": ")
                    .append(safeValue(entry.window.getName())).append('\n');
            details.append(activity.getString(R.string.task_manager_windows_details_window_class)).append(": ")
                    .append(safeValue(entry.window.getClassName())).append('\n');
            details.append(activity.getString(R.string.task_manager_windows_details_window_handle)).append(": 0x")
                    .append(Long.toHexString(entry.window.getHandle())).append('\n');
            details.append(activity.getString(R.string.task_manager_windows_details_window_xid)).append(": 0x")
                    .append(Integer.toHexString(entry.window.id)).append('\n');
            details.append(activity.getString(R.string.task_manager_windows_details_window_map_state)).append(": ")
                    .append(entry.window.getMapState().name()).append('\n');
            details.append(activity.getString(R.string.task_manager_windows_details_window_geometry)).append(": ")
                    .append(entry.window.getX()).append(",").append(entry.window.getY())
                    .append(" ").append(entry.window.getWidth()).append("x").append(entry.window.getHeight());
        } else {
            details.append(activity.getString(R.string.task_manager_windows_details_window_status)).append(": ")
                    .append(activity.getString(R.string.task_manager_windows_details_window_absent));
        }

        dialog.setMessage(details.toString());
        dialog.findViewById(R.id.BTCancel).setVisibility(View.GONE);
        logProcessDetailsOpen(entry, runtimeSample);
        dialog.show();
    }

    private String safeValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return activity.getString(R.string.task_manager_linux_details_not_available);
        }
        return value.trim();
    }

    private String buildWindowsThreadPreview(int pid, int maxThreads) {
        File taskDir = new File(String.format(Locale.US, "/proc/%d/task", pid));
        String[] entries = taskDir.list();
        if (entries == null || entries.length == 0) {
            return activity.getString(R.string.task_manager_windows_details_threads_preview_none);
        }

        ArrayList<Integer> tids = new ArrayList<>();
        for (String entry : entries) {
            if (!isNumericPid(entry)) continue;
            try {
                tids.add(Integer.parseInt(entry));
            } catch (NumberFormatException ignored) {
            }
        }
        if (tids.isEmpty()) {
            return activity.getString(R.string.task_manager_windows_details_threads_preview_none);
        }
        Collections.sort(tids);

        StringBuilder builder = new StringBuilder();
        int shown = 0;
        for (Integer tid : tids) {
            if (shown >= maxThreads) break;
            String name = safeValue(readFirstLine(new File(String.format(Locale.US, "/proc/%d/task/%d/comm", pid, tid))));
            String state = safeValue(readThreadState(pid, tid));
            String scheduling = safeValue(readThreadScheduling(pid, tid));
            if (shown > 0) builder.append('\n');
            builder.append("TID ").append(tid)
                    .append(" | ").append(name)
                    .append(" | ").append(state)
                    .append(" | ").append(scheduling);
            shown++;
        }
        int hidden = tids.size() - shown;
        if (hidden > 0) {
            builder.append('\n').append(activity.getString(R.string.task_manager_windows_details_threads_preview_more, hidden));
        }
        return builder.length() == 0
                ? activity.getString(R.string.task_manager_windows_details_threads_preview_none)
                : builder.toString();
    }

    private String readThreadState(int pid, int tid) {
        File statusFile = new File(String.format(Locale.US, "/proc/%d/task/%d/status", pid, tid));
        if (!statusFile.isFile()) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(statusFile)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("State:")) {
                    return line.substring("State:".length()).trim();
                }
            }
        } catch (IOException ignored) {
        }
        return "";
    }

    private String readThreadScheduling(int pid, int tid) {
        String statLine = readFirstLine(new File(String.format(Locale.US, "/proc/%d/task/%d/stat", pid, tid)));
        if (statLine.isEmpty()) return "";
        int marker = statLine.lastIndexOf(") ");
        if (marker <= 0 || marker + 2 >= statLine.length()) return "";
        String tail = statLine.substring(marker + 2).trim();
        String[] fields = tail.split("\\s+");
        if (fields.length <= 16) return "";
        String priority = fields[15];
        String nice = fields[16];
        return "prio " + priority + " nice " + nice;
    }

    private String readFirstLine(File file) {
        if (file == null || !file.isFile()) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line = reader.readLine();
            return line == null ? "" : line.trim();
        } catch (IOException ignored) {
            return "";
        }
    }

    private void updateCPUInfoView() {
        LinearLayout llCPUInfo = findViewById(R.id.LLCPUInfo);
        llCPUInfo.removeAllViews();
        short[] clockSpeeds = CPUStatus.getCurrentClockSpeeds();
        TextView tvCPUTitle = findViewById(R.id.TVCPUTitle);
        if (clockSpeeds.length == 0) {
            tvCPUTitle.setText(R.string.task_manager_cpu_title_empty);
            return;
        }
        int totalClockSpeed = 0;
        short maxClockSpeed = 0;

        for (int i = 0; i < clockSpeeds.length; i++) {
            totalClockSpeed += clockSpeeds[i];
            maxClockSpeed = (short)Math.max(maxClockSpeed, CPUStatus.getMaxClockSpeed(i));
        }

        int start = 0;
        while (start < clockSpeeds.length) {
            short groupMaxClock = CPUStatus.getMaxClockSpeed(start);
            int end = start;
            int groupTotalClock = clockSpeeds[start];
            while (end + 1 < clockSpeeds.length && CPUStatus.getMaxClockSpeed(end + 1) == groupMaxClock) {
                end++;
                groupTotalClock += clockSpeeds[end];
            }

            int groupCurrentClock = Math.max(0, groupTotalClock / Math.max(1, (end - start + 1)));
            TextView textView = new TextView(activity);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            textView.setTextColor(ContextCompat.getColor(activity, R.color.surface_runtime_taskmgr_muted));
            String coreLabel = start == end ? "C" + start : "C" + start + "-" + end;
            textView.setText(coreLabel + "  " + groupCurrentClock + "/" + groupMaxClock + " MHz");
            llCPUInfo.addView(textView);
            start = end + 1;
        }

        int avgClockSpeed = totalClockSpeed / clockSpeeds.length;
        byte cpuUsagePercent = (byte)(((float)avgClockSpeed / Math.max(1, maxClockSpeed)) * 100.0f);
        tvCPUTitle.setText(activity.getString(R.string.task_manager_cpu_title_value, cpuUsagePercent));
    }

    private void updateMemoryInfoView() {
        ActivityManager activityManager = (ActivityManager)activity.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        long totalMem = Math.max(0L, memoryInfo.totalMem);
        long usedMem = Math.max(0L, totalMem - memoryInfo.availMem);
        byte memUsagePercent = totalMem > 0L ? (byte)(((double)usedMem / totalMem) * 100.0f) : 0;

        TextView tvMemoryTitle = findViewById(R.id.TVMemoryTitle);
        tvMemoryTitle.setText(activity.getString(R.string.memory)+" ("+memUsagePercent+"%)");

        TextView tvMemoryInfo = findViewById(R.id.TVMemoryInfo);
        tvMemoryInfo.setTextColor(ContextCompat.getColor(activity, R.color.surface_runtime_taskmgr_muted));
        tvMemoryInfo.setText(StringUtils.formatBytes(usedMem, false)+"/"+StringUtils.formatBytes(totalMem));
    }

    private void setupProcessTabs() {
        AppUtils.setupTabLayout(getContentView(), R.id.TabLayoutProcessScope, R.id.LLTabWindowsProcesses, R.id.LLTabLinuxProcesses);
        TabLayout tabLayout = findViewById(R.id.TabLayoutProcessScope);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTab = tab.getPosition();
                updateTabSpecificPanels();
                update();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                selectedTab = tab.getPosition();
                updateTabSpecificPanels();
                update();
            }
        });
        updateTabSpecificPanels();
    }

    private void updateTabSpecificPanels() {
        View windowsControls = findViewById(R.id.LLWindowsControlsCard);
        View linuxControls = findViewById(R.id.LLLinuxControlsCard);
        if (windowsControls != null) {
            windowsControls.setVisibility(selectedTab == TAB_WINDOWS ? View.VISIBLE : View.GONE);
        }
        if (linuxControls != null) {
            linuxControls.setVisibility(selectedTab == TAB_LINUX ? View.VISIBLE : View.GONE);
        }
    }

    private void setupFilters() {
        SharedPreferences preferences = getPreferences();
        windowsSortMode = preferences.getString(PREF_WINDOWS_SORT_MODE, WINDOWS_SORT_MEMORY_DESC);
        int savedArchFilter = preferences.getInt(PREF_WINDOWS_ARCH_FILTER, ARCH_FILTER_ALL);
        windowsArchFilterMode = sanitizeArchFilterMode(savedArchFilter);

        CheckBox windowsOnlyWindowed = findViewById(R.id.CBWindowsWindowedOnly);
        windowsOnlyWindowed.setChecked(preferences.getBoolean(PREF_WINDOWS_WINDOWED_ONLY, false));
        windowsOnlyWindowed.setOnCheckedChangeListener((buttonView, isChecked) -> {
            getPreferences().edit().putBoolean(PREF_WINDOWS_WINDOWED_ONLY, isChecked).apply();
            renderWindowsProcessRows();
            updateBottomBarSummary();
        });

        setupWindowsQuickFilters();

        final EditText etWindowsSearch = findViewById(R.id.ETWindowsSearch);
        etWindowsSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                windowsSearchQuery = s == null ? "" : s.toString().trim().toLowerCase(Locale.ENGLISH);
                renderWindowsProcessRows();
                updateBottomBarSummary();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        boolean darkMode = PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("dark_mode", false);
        final Spinner sWindowsSort = findViewById(R.id.SWindowsSort);
        ArrayList<String> sortEntries = new ArrayList<>(Arrays.asList(
                activity.getResources().getStringArray(R.array.task_manager_windows_sort_entries)
        ));
        sWindowsSort.setAdapter(SpinnerAdapters.create(activity, darkMode, sortEntries));
        sWindowsSort.setPopupBackgroundResource(darkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background);
        String[] sortValues = activity.getResources().getStringArray(R.array.task_manager_windows_sort_values);
        int selectedSort = 0;
        for (int i = 0; i < sortValues.length; i++) {
            if (sortValues[i].equals(windowsSortMode)) {
                selectedSort = i;
                break;
            }
        }
        sWindowsSort.setSelection(selectedSort, false);
        sWindowsSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] values = activity.getResources().getStringArray(R.array.task_manager_windows_sort_values);
                if (position >= 0 && position < values.length) {
                    windowsSortMode = values[position];
                    getPreferences().edit().putString(PREF_WINDOWS_SORT_MODE, windowsSortMode).apply();
                    renderWindowsProcessRows();
                    updateBottomBarSummary();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        CheckBox linuxRuntimeOnly = findViewById(R.id.CBLinuxRuntimeOnly);
        linuxRuntimeOnly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (selectedTab == TAB_LINUX) update();
        });
    }

    private void setupWindowsQuickFilters() {
        Button btAll = findViewById(R.id.BTWindowsFilterAll);
        Button btWow64 = findViewById(R.id.BTWindowsFilterWow64);
        Button btArm64ec = findViewById(R.id.BTWindowsFilterArm64ec);
        Button btNative = findViewById(R.id.BTWindowsFilterNative);

        btAll.setOnClickListener(v -> {
            windowsArchFilterMode = ARCH_FILTER_ALL;
            saveWindowsArchFilter();
            refreshWindowsFilterButtons();
            renderWindowsProcessRows();
            updateBottomBarSummary();
        });
        btWow64.setOnClickListener(v -> {
            windowsArchFilterMode = ARCH_FILTER_WOW64;
            saveWindowsArchFilter();
            refreshWindowsFilterButtons();
            renderWindowsProcessRows();
            updateBottomBarSummary();
        });
        btArm64ec.setOnClickListener(v -> {
            windowsArchFilterMode = ARCH_FILTER_ARM64EC;
            saveWindowsArchFilter();
            refreshWindowsFilterButtons();
            renderWindowsProcessRows();
            updateBottomBarSummary();
        });
        btNative.setOnClickListener(v -> {
            windowsArchFilterMode = ARCH_FILTER_NATIVE;
            saveWindowsArchFilter();
            refreshWindowsFilterButtons();
            renderWindowsProcessRows();
            updateBottomBarSummary();
        });
        refreshWindowsFilterButtons();
    }

    private void refreshWindowsFilterButtons() {
        Button btAll = findViewById(R.id.BTWindowsFilterAll);
        Button btWow64 = findViewById(R.id.BTWindowsFilterWow64);
        Button btArm64ec = findViewById(R.id.BTWindowsFilterArm64ec);
        Button btNative = findViewById(R.id.BTWindowsFilterNative);

        setFilterButtonState(btAll, windowsArchFilterMode == ARCH_FILTER_ALL);
        setFilterButtonState(btWow64, windowsArchFilterMode == ARCH_FILTER_WOW64);
        setFilterButtonState(btArm64ec, windowsArchFilterMode == ARCH_FILTER_ARM64EC);
        setFilterButtonState(btNative, windowsArchFilterMode == ARCH_FILTER_NATIVE);
    }

    private void setFilterButtonState(Button button, boolean active) {
        button.setBackgroundResource(active ? R.drawable.surface_runtime_button_positive : R.drawable.surface_runtime_button_neutral);
        button.setTextColor(ContextCompat.getColor(activity,
                active ? R.color.surface_runtime_button_positive_text : R.color.surface_runtime_button_text));
        button.setAlpha(1.0f);
    }

    private void saveWindowsArchFilter() {
        getPreferences().edit().putInt(PREF_WINDOWS_ARCH_FILTER, windowsArchFilterMode).apply();
    }

    private static int sanitizeArchFilterMode(int value) {
        if (value < ARCH_FILTER_ALL || value > ARCH_FILTER_NATIVE) return ARCH_FILTER_ALL;
        return value;
    }

    private SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(activity);
    }

    private void applyThemeState() {
        boolean darkMode = PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("dark_mode", false);
        int brightText = ContextCompat.getColor(activity, R.color.surface_runtime_taskmgr_text);
        int mutedText = ContextCompat.getColor(activity, R.color.surface_runtime_taskmgr_muted);
        TabLayout tabLayout = findViewById(R.id.TabLayoutProcessScope);
        if (tabLayout != null) {
            tabLayout.setBackgroundResource(darkMode ? R.drawable.tab_layout_background_dark : R.drawable.tab_layout_background);
            tabLayout.setTabTextColors(mutedText, brightText);
        }
        EditText etWindowsSearch = findViewById(R.id.ETWindowsSearch);
        if (etWindowsSearch != null) {
            if (darkMode) {
                etWindowsSearch.setTextColor(0xFFFFFFFF);
                etWindowsSearch.setHintTextColor(0xFF9E9E9E);
                etWindowsSearch.setBackgroundResource(R.drawable.edit_text_dark);
            } else {
                etWindowsSearch.setTextColor(0xFF000000);
                etWindowsSearch.setHintTextColor(0xFF6E6E6E);
                etWindowsSearch.setBackgroundResource(R.drawable.edit_text);
            }
        }
        int[] brightIds = new int[] {
                R.id.TVEmptyWindowsText,
                R.id.TVEmptyLinuxText,
                R.id.TVMemoryTitle,
                R.id.TVMemoryInfo,
                R.id.TVCPUTitle,
                R.id.TVSelectedLinuxProcessTitle,
                R.id.TVSelectedLinuxProcessName,
                R.id.TVHostTelemetryTitle,
                R.id.TVHostLoadInfo,
                R.id.TVHostNetInfo,
                R.id.TVHostPressureInfo,
                R.id.TVProcessCountersInfo
        };
        for (int id : brightIds) {
            TextView textView = findViewById(id);
            if (textView != null) textView.setTextColor(brightText);
        }
        int[] mutedIds = new int[] {
                R.id.TVSelectedLinuxProcessMeta,
                R.id.TVSelectedLinuxProcessCommand
        };
        for (int id : mutedIds) {
            TextView textView = findViewById(id);
            if (textView != null) textView.setTextColor(mutedText);
        }
        CheckBox cbWindows = findViewById(R.id.CBWindowsWindowedOnly);
        if (cbWindows != null) cbWindows.setTextColor(brightText);
        CheckBox cbLinux = findViewById(R.id.CBLinuxRuntimeOnly);
        if (cbLinux != null) {
            cbLinux.setTextColor(brightText);
            CompoundButtonCompat.setButtonTintList(
                    cbLinux,
                    ColorStateList.valueOf(ContextCompat.getColor(activity, R.color.surface_runtime_taskmgr_border))
            );
        }
    }

    private void refreshHostTelemetryAsync() {
        if (!telemetryInFlight.compareAndSet(false, true)) return;
        telemetryExecutor.execute(() -> {
            LinuxTelemetrySampler.HostSample hostSample = linuxTelemetrySampler.sampleHost(activity);
            activity.runOnUiThread(() -> {
                telemetryInFlight.set(false);
                if (!isShowing()) return;
                lastHostSample = hostSample;
                applyHostTelemetryViews(hostSample);
                updateBottomBarSummary();
            });
        });
    }

    private void refreshLinuxProcessPanelAsync() {
        if (!telemetryInFlight.compareAndSet(false, true)) return;
        final boolean runtimeOnly = isLinuxRuntimeOnlyEnabled();
        telemetryExecutor.execute(() -> {
            LinuxTelemetrySampler.HostSample hostSample = linuxTelemetrySampler.sampleHost(activity);
            LinuxBatch batch = collectLinuxBatch(runtimeOnly);
            activity.runOnUiThread(() -> {
                telemetryInFlight.set(false);
                if (!isShowing() || selectedTab != TAB_LINUX) return;
                lastHostSample = hostSample;
                lastLinuxTotal = batch.total;
                lastLinuxVisible = batch.visible;
                bindLinuxProcessRows(batch.samples);
                applyHostTelemetryViews(hostSample);
                updateBottomBarSummary();
                maybeLogRefreshCycle("linux");
            });
        });
    }

    private LinuxBatch collectLinuxBatch(boolean runtimeOnly) {
        File procDir = new File("/proc");
        String[] entries = procDir.list();
        ArrayList<LinuxTelemetrySampler.ProcessSample> samples = new ArrayList<>();
        Set<Integer> livePids = new HashSet<>();
        int total = 0;
        int visible = 0;

        if (entries != null) {
            for (String entry : entries) {
                if (!isNumericPid(entry)) continue;
                int pid;
                try {
                    pid = Integer.parseInt(entry);
                } catch (NumberFormatException ignored) {
                    continue;
                }
                livePids.add(pid);
                LinuxTelemetrySampler.ProcessSample sample = linuxTelemetrySampler.sampleProcess(pid);
                if (sample == null) continue;
                total++;
                if (runtimeOnly && !isRuntimeProcess(sample)) continue;
                visible++;
                samples.add(sample);
            }
        }

        linuxTelemetrySampler.retainOnly(livePids);
        samples.sort(new Comparator<LinuxTelemetrySampler.ProcessSample>() {
            @Override
            public int compare(LinuxTelemetrySampler.ProcessSample left, LinuxTelemetrySampler.ProcessSample right) {
                String leftName = left != null && left.commandName != null ? left.commandName : "";
                String rightName = right != null && right.commandName != null ? right.commandName : "";
                int nameCompare = leftName.compareToIgnoreCase(rightName);
                if (nameCompare != 0) return nameCompare;
                return Integer.compare(left.pid, right.pid);
            }
        });

        if (samples.size() > MAX_LINUX_ROWS) {
            samples.subList(MAX_LINUX_ROWS, samples.size()).clear();
        }
        return new LinuxBatch(samples, total, visible);
    }

    private void bindLinuxProcessRows(ArrayList<LinuxTelemetrySampler.ProcessSample> samples) {
        if (samples == null) samples = new ArrayList<>();
        currentLinuxSamples.clear();
        currentLinuxSamples.addAll(samples);
        selectedLinuxSample = resolveSelectedLinuxSample(samples);
        selectedLinuxPid = selectedLinuxSample != null ? selectedLinuxSample.pid : -1;
        findViewById(R.id.TVEmptyLinuxText).setVisibility(samples.isEmpty() ? View.VISIBLE : View.GONE);
        TextView titleView = findViewById(R.id.TVLinuxListTitle);
        if (titleView != null) {
            titleView.setText(activity.getString(R.string.task_manager_linux_list_title) + "  " + samples.size() + "/" + lastLinuxTotal);
        }
        applySelectedLinuxCard(selectedLinuxSample);
        if (linuxProcessAdapter != null) {
            linuxProcessAdapter.submitRows(samples);
        }
        adjustLinuxViewportHeight(samples.size(), false);
        if (linuxRecyclerView != null) {
            linuxRecyclerView.post(() -> {
                linuxRecyclerView.invalidateItemDecorations();
                adjustLinuxViewportHeight(currentLinuxSamples.size(), true);
            });
        }
        if (linuxInitialScrollResetsRemaining > 0) {
            if (linuxRecyclerView != null) {
                linuxRecyclerView.post(() -> {
                    RecyclerView.LayoutManager layoutManager = linuxRecyclerView.getLayoutManager();
                    if (layoutManager instanceof LinearLayoutManager) {
                        ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(0, linuxRecyclerView.getPaddingTop());
                    } else {
                        linuxRecyclerView.scrollToPosition(0);
                    }
                    linuxRecyclerView.requestFocus();
                });
            }
            linuxInitialScrollResetsRemaining--;
        }
        maybeLogRenderedRows("linux", samples.size());
    }

    private void adjustLinuxViewportHeight(int rowCount, boolean preferMeasuredRows) {
        View surface = findViewById(R.id.LLLinuxProcessSurface);
        View viewport = findViewById(R.id.FLLinuxProcessViewport);
        if (surface == null || viewport == null) return;
        int desiredContentHeight = resolveLinuxDesiredContentHeightPx(rowCount, preferMeasuredRows);
        int maxViewportHeight = resolveLinuxAvailableViewportHeightPx();
        ViewGroup.LayoutParams params = viewport.getLayoutParams();
        if (params == null) return;
        boolean layoutChanged = false;
        if (params instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams linearParams = (LinearLayout.LayoutParams) params;
            int resolvedHeight = desiredContentHeight > 0 ? desiredContentHeight : dp(220);
            if (maxViewportHeight > 0) {
                resolvedHeight = Math.min(maxViewportHeight, Math.max(dp(170), resolvedHeight));
            }
            if (linearParams.height != resolvedHeight) {
                linearParams.height = resolvedHeight;
                layoutChanged = true;
            }
            if (linearParams.weight != 0f) {
                linearParams.weight = 0f;
                layoutChanged = true;
            }
        } else if (params.height != Math.max(dp(170), desiredContentHeight)) {
            params.height = Math.max(dp(170), desiredContentHeight);
            layoutChanged = true;
        }
        if (layoutChanged) {
            viewport.setLayoutParams(params);
        }
        ViewGroup.LayoutParams surfaceParams = surface.getLayoutParams();
        if (surfaceParams instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams linearParams = (LinearLayout.LayoutParams) surfaceParams;
            if (linearParams.height != ViewGroup.LayoutParams.WRAP_CONTENT || linearParams.weight != 0f) {
                linearParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                linearParams.weight = 0f;
                surface.setLayoutParams(linearParams);
            }
        } else if (surfaceParams != null && surfaceParams.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
            surfaceParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            surface.setLayoutParams(surfaceParams);
        }
        if (linuxRecyclerView != null) {
            boolean denseList = rowCount >= 8;
            int bottomInset = rowCount > 0 ? dp(denseList ? 5 : 4) : dp(0);
            linuxRecyclerView.setPadding(
                    linuxRecyclerView.getPaddingLeft(),
                    0,
                    linuxRecyclerView.getPaddingRight(),
                    bottomInset
            );
            if (lastLinuxRowInsetDecoration != null) {
                lastLinuxRowInsetDecoration.setBottomInsetPx(rowCount > 0 ? dp(denseList ? 14 : 12) : 0);
            }
            linuxRecyclerView.requestLayout();
        }
    }

    private int resolveLinuxDesiredContentHeightPx(int rowCount, boolean preferMeasuredRows) {
        if (rowCount <= 0) return dp(170);
        int exactVisibleHeight = resolveLinuxExactVisibleHeightPx(rowCount, preferMeasuredRows);
        if (exactVisibleHeight > 0) return exactVisibleHeight;
        int estimatedRowHeight = resolveLinuxMeasuredRowHeightPx(preferMeasuredRows);
        if (estimatedRowHeight <= 0) {
            estimatedRowHeight = rowCount <= 6 ? dp(52) : dp(49);
        }
        int paddingTop = linuxRecyclerView != null ? linuxRecyclerView.getPaddingTop() : 0;
        int paddingBottom = linuxRecyclerView != null ? linuxRecyclerView.getPaddingBottom() : dp(16);
        return rowCount * estimatedRowHeight + paddingTop + paddingBottom + dp(12);
    }

    private int resolveLinuxExactVisibleHeightPx(int rowCount, boolean preferMeasuredRows) {
        if (!preferMeasuredRows || linuxRecyclerView == null) return -1;
        int scrollRange = linuxRecyclerView.computeVerticalScrollRange();
        if (scrollRange > 0 && rowCount <= Math.max(1, currentLinuxSamples.size())) {
            return scrollRange + dp(18);
        }
        RecyclerView.LayoutManager layoutManager = linuxRecyclerView.getLayoutManager();
        if (layoutManager == null) return -1;
        int childCount = linuxRecyclerView.getChildCount();
        if (childCount <= 0) return -1;
        int visibleCount = Math.min(childCount, rowCount);
        if (visibleCount <= 0) return -1;
        View firstChild = linuxRecyclerView.getChildAt(0);
        View lastChild = linuxRecyclerView.getChildAt(visibleCount - 1);
        if (firstChild == null || lastChild == null) return -1;
        int decoratedTop = layoutManager.getDecoratedTop(firstChild);
        int decoratedBottom = layoutManager.getDecoratedBottom(lastChild);
        if (decoratedBottom <= decoratedTop) return -1;
        if (rowCount <= childCount) {
            return (decoratedBottom - decoratedTop)
                    + linuxRecyclerView.getPaddingTop()
                    + linuxRecyclerView.getPaddingBottom()
                    + dp(18);
        }
        int measuredRowHeight = resolveLinuxMeasuredRowHeightPx(true);
        if (measuredRowHeight <= 0) return -1;
        return (rowCount * measuredRowHeight)
                + linuxRecyclerView.getPaddingTop()
                + linuxRecyclerView.getPaddingBottom()
                + dp(18);
    }

    private int resolveLinuxAvailableViewportHeightPx() {
        View body = findViewById(R.id.LLTaskManagerBody);
        View leftPane = findViewById(R.id.LLTabLinuxProcesses);
        View surface = findViewById(R.id.LLLinuxProcessSurface);
        View title = findViewById(R.id.TVLinuxListTitle);
        View header = findViewById(R.id.LLLinuxTableHead);
        View viewport = findViewById(R.id.FLLinuxProcessViewport);
        if (surface == null || viewport == null) return -1;
        int paneHeight = leftPane != null ? leftPane.getHeight() : 0;
        if (paneHeight <= 0 && body != null) {
            paneHeight = body.getHeight();
        }
        if (paneHeight <= 0) {
            paneHeight = Math.round(AppUtils.getScreenHeight() * 0.74f);
        }
        int occupied = surface.getPaddingTop() + surface.getPaddingBottom();
        occupied += title != null ? title.getHeight() : 0;
        occupied += header != null ? header.getHeight() : 0;
        ViewGroup.MarginLayoutParams headerParams = header != null && header.getLayoutParams() instanceof ViewGroup.MarginLayoutParams
                ? (ViewGroup.MarginLayoutParams) header.getLayoutParams()
                : null;
        if (headerParams != null) {
            occupied += headerParams.topMargin + headerParams.bottomMargin;
        }
        ViewGroup.MarginLayoutParams viewportParams = viewport.getLayoutParams() instanceof ViewGroup.MarginLayoutParams
                ? (ViewGroup.MarginLayoutParams) viewport.getLayoutParams()
                : null;
        if (viewportParams != null) {
            occupied += viewportParams.topMargin + viewportParams.bottomMargin;
        }
        occupied += dp(2);
        return Math.max(dp(170), paneHeight - occupied);
    }

    private int resolveLinuxMeasuredRowHeightPx(boolean preferMeasuredRows) {
        if (!preferMeasuredRows || linuxRecyclerView == null) return -1;
        RecyclerView.LayoutManager layoutManager = linuxRecyclerView.getLayoutManager();
        if (layoutManager == null) return -1;
        int childCount = linuxRecyclerView.getChildCount();
        if (childCount <= 0) return -1;
        int measuredTotal = 0;
        int measuredCount = 0;
        int limit = Math.min(childCount, 4);
        for (int i = 0; i < limit; i++) {
            View child = linuxRecyclerView.getChildAt(i);
            if (child == null) continue;
            int height = layoutManager.getDecoratedBottom(child) - layoutManager.getDecoratedTop(child);
            if (height <= 0) continue;
            measuredTotal += height;
            measuredCount++;
        }
        if (measuredCount <= 0) return -1;
        return Math.max(dp(44), Math.round((float) measuredTotal / (float) measuredCount));
    }

    private void bindLinuxProcessRow(View itemView, LinuxTelemetrySampler.ProcessSample sample) {
        boolean selected = sample != null && sample.pid == selectedLinuxPid;
        itemView.setActivated(selected);
        itemView.setSelected(selected);
        itemView.setTag(sample != null ? sample.pid : -1);
        ((TextView) itemView.findViewById(R.id.TVLinuxName)).setText(sample.commandName);
        ((TextView) itemView.findViewById(R.id.TVLinuxState)).setText(formatLinuxStateBadge(String.valueOf(sample.state)));
        ((TextView) itemView.findViewById(R.id.TVLinuxPid)).setText(String.valueOf(sample.pid));
        ((TextView) itemView.findViewById(R.id.TVLinuxCpu)).setText(formatPercent(sample.cpuPercent));
        ((TextView) itemView.findViewById(R.id.TVLinuxMemory)).setText(StringUtils.formatBytes(sample.residentBytes));
        ((TextView) itemView.findViewById(R.id.TVLinuxIo)).setText(formatCompactIoRate(sample));
        ((TextView) itemView.findViewById(R.id.TVLinuxDetail)).setText(buildLinuxDetailLine(sample));
        itemView.setOnClickListener((v) -> {
            if (selectedLinuxPid == sample.pid) {
                showLinuxProcessDetails(sample);
                return;
            }
            selectedLinuxPid = sample.pid;
            selectedLinuxSample = sample;
            applySelectedLinuxCard(sample);
            refreshLinuxRowSelectionState();
        });
        itemView.setOnLongClickListener((v) -> {
            showLinuxProcessDetails(sample);
            return true;
        });
    }

    private LinuxTelemetrySampler.ProcessSample resolveSelectedLinuxSample(ArrayList<LinuxTelemetrySampler.ProcessSample> samples) {
        if (samples == null || samples.isEmpty()) return null;
        for (LinuxTelemetrySampler.ProcessSample sample : samples) {
            if (sample != null && sample.pid == selectedLinuxPid) return sample;
        }
        return samples.get(0);
    }

    private void applySelectedLinuxCard(LinuxTelemetrySampler.ProcessSample sample) {
        TextView nameView = findViewById(R.id.TVSelectedLinuxProcessName);
        TextView metaView = findViewById(R.id.TVSelectedLinuxProcessMeta);
        TextView commandView = findViewById(R.id.TVSelectedLinuxProcessCommand);
        Button inspectButton = findViewById(R.id.BTLinuxInspect);
        Button pauseResumeButton = findViewById(R.id.BTLinuxPauseResume);
        Button terminateButton = findViewById(R.id.BTLinuxTerminate);
        Button killButton = findViewById(R.id.BTLinuxKill);

        boolean enabled = sample != null;
        if (nameView != null) {
            nameView.setText(enabled
                    ? sample.commandName
                    : activity.getString(R.string.task_manager_selected_process_none));
        }
        if (metaView != null) {
            metaView.setText(enabled
                    ? activity.getString(
                    R.string.task_manager_selected_process_meta,
                    sample.pid,
                    formatLinuxStateBadge(String.valueOf(sample.state)),
                    formatPercent(sample.cpuPercent),
                    StringUtils.formatBytes(sample.residentBytes),
                    formatCompactIoRate(sample)
            )
                    : activity.getString(R.string.task_manager_selected_process_hint));
        }
        if (commandView != null) {
            String command = enabled
                    ? firstNonEmpty(sample.commandLine, buildLinuxDetailLine(sample))
                    : activity.getString(R.string.task_manager_selected_process_detail_hint);
            commandView.setText(clipMiddle(command, 180));
        }
        if (pauseResumeButton != null) {
            pauseResumeButton.setText(enabled && isSuspended(sample)
                    ? R.string.task_manager_action_resume
                    : R.string.task_manager_action_pause);
        }
        setEnabled(inspectButton, enabled);
        setEnabled(pauseResumeButton, enabled);
        setEnabled(terminateButton, enabled);
        setEnabled(killButton, enabled);
    }

    private void setEnabled(Button button, boolean enabled) {
        if (button == null) return;
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.48f);
    }

    private String buildLinuxDetailLine(LinuxTelemetrySampler.ProcessSample sample) {
        String source = sample.commandLine != null && !sample.commandLine.trim().isEmpty()
                ? sample.commandLine.trim()
                : safeValue(sample.waitChannel);
        if (source.isEmpty()) {
            source = activity.getString(R.string.task_manager_linux_details_not_available);
        }
        return formatDuration(sample.ageMs)
                + "  |  THR " + sample.threadCount
                + "  |  FD " + sample.fileDescriptorCount
                + "  |  SOCK " + (sample.inetSocketCount + sample.unixSocketCount)
                + "  |  " + clipMiddle(source, 84);
    }

    private void showLinuxProcessDetails(LinuxTelemetrySampler.ProcessSample sample) {
        ContentDialog dialog = new ContentDialog(activity);
        dialog.setTitle(activity.getString(R.string.task_manager_linux_details_title, sample.pid));

        String notAvailable = activity.getString(R.string.task_manager_linux_details_not_available);
        String cpuset = sample.cpuSetList == null || sample.cpuSetList.isEmpty() ? notAvailable : sample.cpuSetList;
        String waitChannel = sample.waitChannel == null || sample.waitChannel.isEmpty() ? notAvailable : sample.waitChannel;
        String ioRate = formatIoRate(sample);
        String contextSwitchRate = sample.hasContextSwitchRate() ? formatFloat(sample.contextSwitchesPerSecond) + "/s" : notAvailable;
        String sockets = String.format(Locale.ENGLISH, "inet=%d (tcp=%d udp=%d), unix=%d",
                sample.inetSocketCount, sample.tcpSocketCount, sample.udpSocketCount, sample.unixSocketCount);

        StringBuilder details = new StringBuilder();
        details.append(activity.getString(R.string.task_manager_linux_details_name)).append(": ").append(sample.commandName).append('\n');
        details.append(activity.getString(R.string.task_manager_linux_details_cmd)).append(": ").append(sample.commandLine).append('\n');
        details.append(activity.getString(R.string.task_manager_linux_details_state)).append(": ").append(formatLinuxStateBadge(String.valueOf(sample.state))).append('\n');
        details.append(activity.getString(R.string.task_manager_linux_details_threads)).append(": ").append(sample.threadCount).append('\n');
        details.append(activity.getString(R.string.task_manager_linux_details_cpuset)).append(": ").append(cpuset).append('\n');
        details.append(activity.getString(R.string.task_manager_linux_details_fd)).append(": ").append(sample.fileDescriptorCount).append('\n');
        details.append(activity.getString(R.string.task_manager_linux_details_io_rate)).append(": ").append(ioRate).append('\n');
        details.append(activity.getString(R.string.task_manager_linux_details_ctx_switch)).append(": ").append(contextSwitchRate).append('\n');
        details.append(activity.getString(R.string.task_manager_linux_details_sockets)).append(": ").append(sockets).append('\n');
        details.append(activity.getString(R.string.task_manager_linux_details_wait_channel)).append(": ").append(waitChannel).append('\n');
        details.append(activity.getString(R.string.task_manager_linux_details_oom)).append(": ").append(sample.oomScore).append('\n');
        details.append(activity.getString(R.string.task_manager_linux_details_age)).append(": ").append(formatDuration(sample.ageMs));

        dialog.setMessage(details.toString());
        dialog.findViewById(R.id.BTCancel).setVisibility(View.GONE);
        dialog.show();
        styleTaskManagerNestedDialog(dialog);
    }

    private void maybeLogRenderedRows(String scope, int renderedRows) {
        if ("windows".equals(scope)) {
            if (lastWindowsRenderedRows == renderedRows) return;
            lastWindowsRenderedRows = renderedRows;
        }
        else {
            if (lastLinuxRenderedRows == renderedRows) return;
            lastLinuxRenderedRows = renderedRows;
        }

        ScrollView windowsScroll = findViewById(R.id.SVWindowsProcessList);
        RecyclerView linuxList = findViewById(R.id.RVLinuxProcessList);
        ForensicLogger.logEvent(
                activity,
                "info",
                "TASKMGR_RENDER_ROWS",
                null,
                "task_manager",
                "Task Manager rendered process rows",
                ForensicLogger.fields(
                        "scope", scope,
                        "rendered_rows", renderedRows,
                        "windows_scroll_height", windowsScroll != null ? windowsScroll.getHeight() : -1,
                        "linux_scroll_height", linuxList != null ? linuxList.getHeight() : -1,
                        "selected_tab", selectedTab == TAB_WINDOWS ? "windows" : "linux",
                        "linux_padding_top", linuxList != null ? linuxList.getPaddingTop() : -1,
                        "linux_padding_bottom", linuxList != null ? linuxList.getPaddingBottom() : -1
                )
        );
    }

    private void applyHostTelemetryViews(LinuxTelemetrySampler.HostSample sample) {
        TextView tvLoadInfo = findViewById(R.id.TVHostLoadInfo);
        TextView tvNetInfo = findViewById(R.id.TVHostNetInfo);
        TextView tvPressureInfo = findViewById(R.id.TVHostPressureInfo);
        TextView tvCountersInfo = findViewById(R.id.TVProcessCountersInfo);

        if (sample == null) {
            tvLoadInfo.setText(R.string.task_manager_host_load_empty);
            tvNetInfo.setText(R.string.task_manager_host_net_empty);
            tvPressureInfo.setText(R.string.task_manager_host_pressure_empty);
            tvCountersInfo.setText(R.string.task_manager_host_counters_empty);
            return;
        }

        tvLoadInfo.setText(activity.getString(R.string.task_manager_host_load_value,
                sample.cpuPercent, sample.loadAverage1m, sample.loadAverage5m, sample.loadAverage15m));
        tvNetInfo.setText(activity.getString(R.string.task_manager_host_net_value,
                formatRate(sample.rxBytesPerSecond), formatRate(sample.txBytesPerSecond)));
        tvPressureInfo.setText(activity.getString(R.string.task_manager_host_pressure_value,
                formatPsi(sample.cpuPressureSome10), formatPsi(sample.cpuPressureFull10),
                formatPsi(sample.ioPressureSome10), formatPsi(sample.ioPressureFull10),
                formatPsi(sample.memoryPressureSome10), formatPsi(sample.memoryPressureFull10)));
        tvCountersInfo.setText(activity.getString(R.string.task_manager_host_counters_value,
                lastWindowsVisible, lastWindowsTotal, lastLinuxVisible, lastLinuxTotal));
    }

    private void updateBottomBarSummary() {
        String summary = String.format(Locale.ENGLISH, "Linux %d/%d",
                lastLinuxVisible, lastLinuxTotal);
        TextView bottomText = findViewById(R.id.TVBottomBarText);
        if (bottomText != null) {
            bottomText.setText(summary);
            bottomText.setVisibility(View.GONE);
        }

        if (lastHostSample != null) {
            applyHostTelemetryViews(lastHostSample);
        }
    }

    private boolean isWindowsOnlyWindowedEnabled() {
        CheckBox checkBox = findViewById(R.id.CBWindowsWindowedOnly);
        return checkBox != null && checkBox.isChecked();
    }

    private boolean isLinuxRuntimeOnlyEnabled() {
        CheckBox checkBox = findViewById(R.id.CBLinuxRuntimeOnly);
        return checkBox != null && checkBox.isChecked();
    }

    private static boolean isNumericPid(String value) {
        if (value == null || value.isEmpty()) return false;
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) return false;
        }
        return true;
    }

    private static boolean isRuntimeProcess(LinuxTelemetrySampler.ProcessSample sample) {
        String haystack = (sample.commandName + " " + sample.commandLine).toLowerCase(Locale.ENGLISH);
        for (String token : RUNTIME_HINT_TOKENS) {
            if (haystack.contains(token)) return true;
        }
        return false;
    }

    private static String formatLinuxStateBadge(String rawState) {
        String normalized = rawState == null ? "" : rawState.trim().toUpperCase(Locale.ENGLISH);
        if (normalized.isEmpty()) return "UNK";
        switch (normalized.charAt(0)) {
            case 'R':
                return "RUN";
            case 'S':
                return "SLEEP";
            case 'D':
                return "WAIT";
            case 'T':
                return "STOP";
            case 'Z':
                return "ZOMB";
            case 'I':
                return "IDLE";
            default:
                return normalized.length() > 4 ? normalized.substring(0, 4) : normalized;
        }
    }

    private static String clipMiddle(String value, int maxLength) {
        if (value == null) return "";
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        if (normalized.length() <= maxLength) return normalized;
        int head = Math.max(12, (maxLength / 2) - 2);
        int tail = Math.max(10, maxLength - head - 3);
        return normalized.substring(0, head) + "..." + normalized.substring(normalized.length() - tail);
    }

    private void refreshLinuxRowSelectionState() {
        if (linuxProcessAdapter != null) {
            linuxProcessAdapter.notifyDataSetChanged();
        }
    }

    private LinuxTelemetrySampler.ProcessSample requireSelectedLinuxSample() {
        if (selectedLinuxSample != null) return selectedLinuxSample;
        AppUtils.showToast(activity, R.string.task_manager_selected_process_hint);
        return null;
    }

    private static boolean isSuspended(LinuxTelemetrySampler.ProcessSample sample) {
        if (sample == null) return false;
        return String.valueOf(sample.state).toUpperCase(Locale.ENGLISH).startsWith("T");
    }

    private void logLinuxProcessAction(String action, LinuxTelemetrySampler.ProcessSample sample) {
        if (sample == null) return;
        ForensicLogger.logEvent(
                activity,
                "info",
                "TASKMGR_ACTION",
                null,
                "task_manager",
                action,
                ForensicLogger.fields(
                        "pid", sample.pid,
                        "name", sample.commandName,
                        "state", sample.state,
                        "threads", sample.threadCount,
                        "cpu_percent", sample.cpuPercent,
                        "resident_bytes", sample.residentBytes,
                        "selected_tab", "linux"
                )
        );
    }

    private static String firstNonEmpty(String first, String fallback) {
        return first != null && !first.trim().isEmpty() ? first.trim() : fallback;
    }

    private void styleTaskManagerNestedDialog(ContentDialog dialog) {
        if (dialog == null) return;
        int brightText = ContextCompat.getColor(activity, R.color.surface_runtime_taskmgr_text);
        int subtleText = ContextCompat.getColor(activity, R.color.surface_runtime_taskmgr_muted);
        View root = dialog.getContentView();
        if (root != null) {
            root.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
        }
        TextView titleView = dialog.findViewById(R.id.TVTitle);
        if (titleView != null) titleView.setTextColor(brightText);
        TextView messageView = dialog.findViewById(R.id.TVMessage);
        if (messageView != null) messageView.setTextColor(subtleText);
        ImageView iconView = dialog.findViewById(R.id.IVIcon);
        if (iconView != null) iconView.setColorFilter(brightText);
        Button confirmButton = dialog.findViewById(R.id.BTConfirm);
        if (confirmButton != null) {
            confirmButton.setBackgroundResource(R.drawable.surface_runtime_button_neutral);
            confirmButton.setTextColor(ContextCompat.getColor(activity, R.color.surface_runtime_button_text));
        }
        Button cancelButton = dialog.findViewById(R.id.BTCancel);
        if (cancelButton != null && cancelButton.getVisibility() == View.VISIBLE) {
            cancelButton.setBackgroundResource(R.drawable.surface_runtime_button_neutral);
            cancelButton.setTextColor(ContextCompat.getColor(activity, R.color.surface_runtime_button_text));
        }
    }

    private final class LinuxProcessViewHolder extends RecyclerView.ViewHolder {
        private LinuxProcessViewHolder(View itemView) {
            super(itemView);
        }
    }

    private final class LinuxProcessAdapter extends RecyclerView.Adapter<LinuxProcessViewHolder> {
        private final ArrayList<LinuxTelemetrySampler.ProcessSample> rows = new ArrayList<>();

        private LinuxProcessAdapter() {
            setHasStableIds(true);
        }

        @Override
        public long getItemId(int position) {
            LinuxTelemetrySampler.ProcessSample sample = position >= 0 && position < rows.size() ? rows.get(position) : null;
            return sample != null ? sample.pid : RecyclerView.NO_ID;
        }

        @Override
        public LinuxProcessViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new LinuxProcessViewHolder(inflater.inflate(R.layout.linux_process_info_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(LinuxProcessViewHolder holder, int position) {
            bindLinuxProcessRow(holder.itemView, rows.get(position));
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        private void submitRows(ArrayList<LinuxTelemetrySampler.ProcessSample> samples) {
            rows.clear();
            if (samples != null) rows.addAll(samples);
            notifyDataSetChanged();
        }
    }

    private static final class LastLinuxRowInsetDecoration extends RecyclerView.ItemDecoration {
        private int bottomInsetPx;

        private LastLinuxRowInsetDecoration(int bottomInsetPx) {
            this.bottomInsetPx = bottomInsetPx;
        }

        private void setBottomInsetPx(int bottomInsetPx) {
            this.bottomInsetPx = Math.max(0, bottomInsetPx);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            int childPosition = parent.getChildAdapterPosition(view);
            if (childPosition == RecyclerView.NO_POSITION) return;
            RecyclerView.Adapter<?> adapter = parent.getAdapter();
            int itemCount = adapter != null ? adapter.getItemCount() : 0;
            outRect.bottom = childPosition == itemCount - 1 ? bottomInsetPx : 0;
        }
    }

    private static String formatPercent(float value) {
        if (value < 0f) return "--";
        return String.format(Locale.ENGLISH, "%.1f%%", value);
    }

    private static String formatPsi(float value) {
        if (value < 0f) return "--";
        return String.format(Locale.ENGLISH, "%.1f", value);
    }

    private static String formatFloat(float value) {
        if (value < 0f) return "--";
        return String.format(Locale.ENGLISH, "%.1f", value);
    }

    private static String formatRate(long bytesPerSecond) {
        if (bytesPerSecond < 0L) return "--";
        return StringUtils.formatBytes(bytesPerSecond);
    }

    private static String formatIoRate(LinuxTelemetrySampler.ProcessSample sample) {
        if (!sample.hasIoRate()) return "R -- W --";
        String readRate = sample.readRateBytes >= 0L ? StringUtils.formatBytes(sample.readRateBytes) + "/s" : "--";
        String writeRate = sample.writeRateBytes >= 0L ? StringUtils.formatBytes(sample.writeRateBytes) + "/s" : "--";
        return "R " + readRate + " W " + writeRate;
    }

    private static String formatCompactIoRate(LinuxTelemetrySampler.ProcessSample sample) {
        if (sample == null || !sample.hasIoRate()) return "--";
        String readRate = sample.readRateBytes >= 0L ? StringUtils.formatBytes(sample.readRateBytes) : "--";
        String writeRate = sample.writeRateBytes >= 0L ? StringUtils.formatBytes(sample.writeRateBytes) : "--";
        return readRate + "/" + writeRate;
    }

    private static String formatDuration(long milliseconds) {
        long totalSeconds = Math.max(0L, milliseconds / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) return String.format(Locale.ENGLISH, "%dh %dm %ds", hours, minutes, seconds);
        if (minutes > 0L) return String.format(Locale.ENGLISH, "%dm %ds", minutes, seconds);
        return String.format(Locale.ENGLISH, "%ds", seconds);
    }

    private static boolean detectArm64ecRuntime(XServerDisplayActivity activity) {
        try {
            Container container = activity.getContainer();
            if (container == null) return false;
            String wineVersion = container.getWineVersion();
            return wineVersion != null && wineVersion.toLowerCase(Locale.ENGLISH).contains("arm64ec");
        } catch (Exception ignored) {
            return false;
        }
    }

    private String resolveArchLane(ProcessInfo processInfo) {
        if (processInfo.wow64Process) return activity.getString(R.string.task_manager_arch_wow64);
        if (arm64ecRuntime) return activity.getString(R.string.task_manager_arch_arm64ec);
        return activity.getString(R.string.task_manager_arch_native);
    }

    private void logProcessAction(String action, ProcessInfo processInfo) {
        ForensicLogger.logEvent(
                activity,
                "info",
                "TASKMGR_ACTION",
                null,
                "task_manager",
                action,
                ForensicLogger.fields(
                        "pid", processInfo.pid,
                        "name", processInfo.name,
                        "path", processInfo.path,
                        "wow64", processInfo.wow64Process,
                        "arch_lane", resolveArchLane(processInfo),
                        "selected_tab", selectedTab == TAB_WINDOWS ? "windows" : "linux"
                )
        );
    }

    private void logProcessDetailsOpen(WindowsProcessEntry entry, LinuxTelemetrySampler.ProcessSample sample) {
        ProcessInfo processInfo = entry.processInfo;
        ForensicLogger.logEvent(
                activity,
                "info",
                "TASKMGR_DETAILS_OPEN",
                null,
                "task_manager",
                "open_process_details",
                ForensicLogger.fields(
                        "pid", processInfo.pid,
                        "name", processInfo.name,
                        "path", processInfo.path,
                        "wow64", processInfo.wow64Process,
                        "arch_lane", resolveArchLane(processInfo),
                        "windowed", entry.windowed,
                        "cpu_percent", sample != null ? sample.cpuPercent : -1f,
                        "threads", sample != null ? sample.threadCount : -1,
                        "state", sample != null ? sample.state : "",
                        "selected_tab", selectedTab == TAB_WINDOWS ? "windows" : "linux"
                )
        );
    }

    private void maybeLogRefreshCycle(String lane) {
        long now = System.currentTimeMillis();
        boolean countersChanged =
                lastLoggedWindowsVisible != lastWindowsVisible
                || lastLoggedWindowsTotal != lastWindowsTotal
                || lastLoggedLinuxVisible != lastLinuxVisible
                || lastLoggedLinuxTotal != lastLinuxTotal;
        if (!countersChanged && now - lastRefreshLogAtMs < TASKMGR_REFRESH_LOG_INTERVAL_MS) return;

        lastRefreshLogAtMs = now;
        lastLoggedWindowsVisible = lastWindowsVisible;
        lastLoggedWindowsTotal = lastWindowsTotal;
        lastLoggedLinuxVisible = lastLinuxVisible;
        lastLoggedLinuxTotal = lastLinuxTotal;

        ForensicLogger.logEvent(
                activity,
                "info",
                "TASKMGR_REFRESH",
                null,
                "task_manager",
                "refresh_cycle",
                ForensicLogger.fields(
                        "lane", lane,
                        "selected_tab", selectedTab == TAB_WINDOWS ? "windows" : "linux",
                        "windows_visible", lastWindowsVisible,
                        "windows_total", lastWindowsTotal,
                        "linux_visible", lastLinuxVisible,
                        "linux_total", lastLinuxTotal,
                        "path_support", lastWindowsPathSupport ? "present" : "legacy"
                )
        );
    }

    private static final class WindowsProcessEntry {
        final ProcessInfo processInfo;
        final Window window;
        final boolean windowed;
        final String archLane;

        WindowsProcessEntry(ProcessInfo processInfo, Window window, String archLane) {
            this.processInfo = processInfo;
            this.window = window;
            this.windowed = window != null;
            this.archLane = archLane;
        }
    }

    private static final class LinuxBatch {
        final ArrayList<LinuxTelemetrySampler.ProcessSample> samples;
        final int total;
        final int visible;

        LinuxBatch(ArrayList<LinuxTelemetrySampler.ProcessSample> samples, int total, int visible) {
            this.samples = samples;
            this.total = total;
            this.visible = visible;
        }
    }
}
