package com.winlator.cmod.winhandler;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

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
    private int selectedTab = TAB_WINDOWS;
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

    public TaskManagerDialog(XServerDisplayActivity activity) {
        super(activity, R.layout.task_manager_dialog);
        this.activity = activity;
        this.arm64ecRuntime = detectArm64ecRuntime(activity);
        setCancelable(false);
        setTitle(R.string.task_manager);
        setIcon(R.drawable.icon_task_manager);

        Button cancelButton = findViewById(R.id.BTCancel);
        cancelButton.setText(R.string.new_task);
        cancelButton.setOnClickListener((v) -> {
            dismiss();
            ContentDialog.prompt(activity, R.string.new_task, "taskmgr.exe", (command) -> activity.getWinHandler().exec(command));
        });

        setupProcessTabs();
        setupFilters();
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
            if (selectedTab == TAB_WINDOWS) {
                activity.getWinHandler().listProcesses();
                final LinearLayout container = findViewById(R.id.LLProcessList);
                if (container.getChildCount() == 0) findViewById(R.id.TVEmptyWindowsText).setVisibility(View.VISIBLE);
            } else {
                refreshLinuxProcessPanelAsync();
            }
        }

        refreshHostTelemetryAsync();
        updateCPUInfoView();
        updateMemoryInfoView();
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
        dialog.setIcon(R.drawable.icon_cpu);
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
        File iconDir = new File(ImageFs.find(context).getRootDir(), "home/xuser/.local/share/icons/taskmgr");
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
        activity.getWinHandler().setOnGetProcessInfoListener(this);
        update();

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                activity.runOnUiThread(TaskManagerDialog.this::update);
            }
        }, 0, 1000);
        super.show();
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

                if (numProcesses == 0 || processInfo == null) {
                    lastWindowsVisible = 0;
                    windowsPending.clear();
                    renderWindowsProcessRows();
                    updateBottomBarSummary();
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
        container.removeAllViews();

        ArrayList<WindowsProcessEntry> rows = new ArrayList<>();
        for (WindowsProcessEntry entry : windowsPending) {
            if (isWindowsOnlyWindowedEnabled() && !entry.windowed) continue;
            if (!matchesArchFilter(entry)) continue;
            if (!matchesWindowsQuery(entry)) continue;
            rows.add(entry);
        }
        sortWindowsRows(rows);

        for (WindowsProcessEntry entry : rows) {
            ProcessInfo processInfo = entry.processInfo;
            View itemView = inflater.inflate(R.layout.process_info_list_item, container, false);
            TextView tvName = itemView.findViewById(R.id.TVName);
            tvName.setText(processInfo.name + " [" + entry.archLane + "]");
            ((TextView) itemView.findViewById(R.id.TVPID)).setText(String.valueOf(processInfo.pid));
            LinuxTelemetrySampler.ProcessSample runtimeSample = linuxTelemetrySampler.sampleProcess(processInfo.pid);
            String cpuPercent = runtimeSample != null ? formatPercent(runtimeSample.cpuPercent) : "--";
            ((TextView) itemView.findViewById(R.id.TVMemoryUsage)).setText(
                    String.format(Locale.ENGLISH, "%s | CPU %s", processInfo.getFormattedMemoryUsage(), cpuPercent)
            );
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
                if (icon != null) ivIcon.setImageBitmap(icon);
            }
            container.addView(itemView);
        }

        lastWindowsVisible = rows.size();
        int childCount = container.getChildCount();
        findViewById(R.id.TVEmptyWindowsText).setVisibility(childCount == 0 ? View.VISIBLE : View.GONE);
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
            tvCPUTitle.setText("CPU (--%)");
            return;
        }
        int totalClockSpeed = 0;
        short maxClockSpeed = 0;

        for (int i = 0; i < clockSpeeds.length; i++) {
            TextView textView = new TextView(activity);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            short clockSpeed = CPUStatus.getMaxClockSpeed(i);
            textView.setText(clockSpeeds[i]+"/"+clockSpeed+" MHz");
            llCPUInfo.addView(textView);
            totalClockSpeed += clockSpeeds[i];
            maxClockSpeed = (short)Math.max(maxClockSpeed, clockSpeed);
        }

        int avgClockSpeed = totalClockSpeed / clockSpeeds.length;
        byte cpuUsagePercent = (byte)(((float)avgClockSpeed / Math.max(1, maxClockSpeed)) * 100.0f);
        tvCPUTitle.setText("CPU ("+cpuUsagePercent+"%)");
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
        tvMemoryInfo.setText(StringUtils.formatBytes(usedMem, false)+"/"+StringUtils.formatBytes(totalMem));
    }

    private void setupProcessTabs() {
        AppUtils.setupTabLayout(getContentView(), R.id.TabLayoutProcessScope, R.id.LLTabWindowsProcesses, R.id.LLTabLinuxProcesses);
        TabLayout tabLayout = findViewById(R.id.TabLayoutProcessScope);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTab = tab.getPosition();
                update();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                selectedTab = tab.getPosition();
                update();
            }
        });
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

        final Spinner sWindowsSort = findViewById(R.id.SWindowsSort);
        ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(
                activity,
                R.array.task_manager_windows_sort_entries,
                android.R.layout.simple_spinner_dropdown_item);
        sWindowsSort.setAdapter(sortAdapter);
        boolean darkMode = PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("dark_mode", false);
        sWindowsSort.setPopupBackgroundResource(darkMode ? R.drawable.content_dialog_background_dark : R.drawable.content_dialog_background);
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
        button.setBackgroundResource(active ? R.drawable.button_positive : R.drawable.button_neutral);
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
        TabLayout tabLayout = findViewById(R.id.TabLayoutProcessScope);
        tabLayout.setBackgroundResource(darkMode ? R.drawable.tab_layout_background_dark : R.drawable.tab_layout_background);
        EditText etWindowsSearch = findViewById(R.id.ETWindowsSearch);
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
                int cpuCompare = Float.compare(right.cpuPercent, left.cpuPercent);
                if (cpuCompare != 0) return cpuCompare;
                int memCompare = Long.compare(right.residentBytes, left.residentBytes);
                if (memCompare != 0) return memCompare;
                return Integer.compare(left.pid, right.pid);
            }
        });

        if (samples.size() > MAX_LINUX_ROWS) {
            samples.subList(MAX_LINUX_ROWS, samples.size()).clear();
        }
        return new LinuxBatch(samples, total, visible);
    }

    private void bindLinuxProcessRows(ArrayList<LinuxTelemetrySampler.ProcessSample> samples) {
        final LinearLayout container = findViewById(R.id.LLLinuxProcessList);
        container.removeAllViews();
        for (LinuxTelemetrySampler.ProcessSample sample : samples) {
            View itemView = inflater.inflate(R.layout.linux_process_info_list_item, container, false);
            ((TextView)itemView.findViewById(R.id.TVLinuxName)).setText(sample.commandName + " [" + sample.state + "]");
            ((TextView)itemView.findViewById(R.id.TVLinuxPid)).setText(String.valueOf(sample.pid));
            ((TextView)itemView.findViewById(R.id.TVLinuxCpu)).setText(formatPercent(sample.cpuPercent));
            ((TextView)itemView.findViewById(R.id.TVLinuxMemory)).setText(StringUtils.formatBytes(sample.residentBytes));
            ((TextView)itemView.findViewById(R.id.TVLinuxIo)).setText(formatIoRate(sample));
            itemView.setOnClickListener((v) -> showLinuxProcessDetails(sample));
            container.addView(itemView);
        }

        findViewById(R.id.TVEmptyLinuxText).setVisibility(samples.isEmpty() ? View.VISIBLE : View.GONE);
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
        details.append(activity.getString(R.string.task_manager_linux_details_state)).append(": ").append(sample.state).append('\n');
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
    }

    private void applyHostTelemetryViews(LinuxTelemetrySampler.HostSample sample) {
        TextView tvLoadInfo = findViewById(R.id.TVHostLoadInfo);
        TextView tvNetInfo = findViewById(R.id.TVHostNetInfo);
        TextView tvPressureInfo = findViewById(R.id.TVHostPressureInfo);
        TextView tvCountersInfo = findViewById(R.id.TVProcessCountersInfo);

        if (sample == null) {
            tvLoadInfo.setText("CPU -- | Load -- -- --");
            tvNetInfo.setText("Net RX -- | TX --");
            tvPressureInfo.setText("PSI cpu --/-- io --/-- mem --/--");
            tvCountersInfo.setText("Windows 0/0 | Linux 0/0");
            return;
        }

        tvLoadInfo.setText(String.format(Locale.ENGLISH, "CPU %.1f%% | Load %.2f %.2f %.2f",
                sample.cpuPercent, sample.loadAverage1m, sample.loadAverage5m, sample.loadAverage15m));
        tvNetInfo.setText(String.format(Locale.ENGLISH, "Net RX %s/s | TX %s/s",
                formatRate(sample.rxBytesPerSecond), formatRate(sample.txBytesPerSecond)));
        tvPressureInfo.setText(String.format(Locale.ENGLISH, "PSI cpu %s/%s io %s/%s mem %s/%s",
                formatPsi(sample.cpuPressureSome10), formatPsi(sample.cpuPressureFull10),
                formatPsi(sample.ioPressureSome10), formatPsi(sample.ioPressureFull10),
                formatPsi(sample.memoryPressureSome10), formatPsi(sample.memoryPressureFull10)));
        tvCountersInfo.setText(String.format(Locale.ENGLISH, "Windows %d/%d | Linux %d/%d",
                lastWindowsVisible, lastWindowsTotal, lastLinuxVisible, lastLinuxTotal));
    }

    private void updateBottomBarSummary() {
        String summary = String.format(Locale.ENGLISH, "Windows %d/%d | Linux %d/%d",
                lastWindowsVisible, lastWindowsTotal, lastLinuxVisible, lastLinuxTotal);
        setBottomBarText(summary);

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
