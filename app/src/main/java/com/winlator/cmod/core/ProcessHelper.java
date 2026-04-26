package com.winlator.cmod.core;

import android.os.Process;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

public abstract class ProcessHelper {
    public static final boolean PRINT_DEBUG = false;
    private static final ArrayList<Callback<String>> debugCallbacks = new ArrayList<>();
    private static final byte SIGCONT = 18;
    private static final byte SIGSTOP = 19;
    private static final byte SIGTERM = 15;
    private static final byte SIGKILL = 9;
    private static volatile boolean nativeLifecycleLoadAttempted = false;
    private static volatile boolean nativeLifecycleAvailable = false;

    public static class ProcessInfo {
        public final int pid;
        public final int ppid;
        public final String name;
        public final long rssBytes;

        public ProcessInfo(int pid, int ppid, String name) {
            this(pid, ppid, name, 0L);
        }

        public ProcessInfo(int pid, int ppid, String name, long rssBytes) {
            this.pid = pid;
            this.ppid = ppid;
            this.name = name;
            this.rssBytes = rssBytes;
        }
    }

    public static void suspendProcess(int pid) {
        Process.sendSignal(pid, SIGSTOP);
        Log.d("ProcessHelper", "Process suspended with pid: " + pid);
    }

    public static void resumeProcess(int pid) {
        Process.sendSignal(pid, SIGCONT);
        Log.d("ProcessHelper", "Process resumed with pid: " + pid);
    }

    public static void terminateProcess(int pid) {
        Process.sendSignal(pid, SIGTERM);
        Log.d("ProcessHelper", "Process terminated with pid: " + pid);
    }

    public static void killProcess(int pid) {
        Process.sendSignal(pid, SIGKILL);
        Log.d("ProcessHelper", "Process killed with pid: " + pid);
    }

    public static void terminateAllWineProcesses() {
        for (String process : listRunningWineProcesses()) {
            terminateProcess(Integer.parseInt(process));
        }
        startNativeReaperWindow(2500);
    }

    public static void killAllWineProcesses() {
        for (String process : listRunningWineProcesses()) {
            killProcess(Integer.parseInt(process));
        }
        startNativeReaperWindow(2500);
    }

    public static void hardKillStaleWineProcesses() throws InterruptedException {
        hardKillStaleWineProcesses(5000L);
    }

    public static void hardKillStaleWineProcesses(long timeoutMs) throws InterruptedException {
        long deadlineMs = System.currentTimeMillis() + Math.max(0L, timeoutMs);
        List<String> stalePids = listRunningWineProcesses();
        if (stalePids.isEmpty()) {
            ForensicLogger.logEvent(
                    ForensicLogger.getAppContext(),
                    "info",
                    "PRELAUNCH_STALE_WINE_SCAN_CLEAN",
                    null,
                    "process_helper",
                    "prelaunch_stale_wine_scan_clean",
                    ForensicLogger.fields("timeout_ms", timeoutMs)
            );
            return;
        }

        Log.w("ProcessHelper", "Found stale Wine processes before launch; hard-killing: " + String.join(", ", stalePids));
        ForensicLogger.logEvent(
                ForensicLogger.getAppContext(),
                "warn",
                "PRELAUNCH_STALE_WINE_KILL",
                null,
                "process_helper",
                "prelaunch_stale_wine_kill",
                ForensicLogger.fields(
                        "stale_pid_count", stalePids.size(),
                        "stale_pids", String.join(",", stalePids),
                        "timeout_ms", timeoutMs
                )
        );
        killAllWineProcesses();

        List<String> remaining;
        do {
            Thread.sleep(100L);
            remaining = listRunningWineProcesses();
        } while (!remaining.isEmpty() && System.currentTimeMillis() < deadlineMs);

        if (!remaining.isEmpty()) {
            String remainingText = String.join(",", remaining);
            ForensicLogger.logEvent(
                    ForensicLogger.getAppContext(),
                    "error",
                    "PRELAUNCH_STALE_WINE_KILL_FAILED",
                    null,
                    "process_helper",
                    "prelaunch_stale_wine_kill_failed",
                    ForensicLogger.fields(
                            "remaining_pid_count", remaining.size(),
                            "remaining_pids", remainingText
                    )
            );
            throw new IllegalStateException("Wine processes still present after hard-kill attempt: " + remainingText);
        }

        ForensicLogger.logEvent(
                ForensicLogger.getAppContext(),
                "info",
                "PRELAUNCH_STALE_WINE_KILL_DONE",
                null,
                "process_helper",
                "prelaunch_stale_wine_kill_done",
                ForensicLogger.fields("initial_pid_count", stalePids.size())
        );
    }

    public static void pauseAllWineProcesses() {
        for (String process : listRunningWineProcesses()) {
            suspendProcess(Integer.parseInt(process));
        }
    }

    public static void resumeAllWineProcesses() {
        for (String process : listRunningWineProcesses()) {
            resumeProcess(Integer.parseInt(process));
        }
    }

    public static void killProcessTree(int rootPid) {
        signalProcessTree(rootPid, SIGKILL);
        startNativeReaperWindow(2500);
    }

    public static void terminateProcessTree(int rootPid) {
        signalProcessTree(rootPid, SIGTERM);
        startNativeReaperWindow(2500);
    }

    private static void signalProcessTree(int rootPid, byte signal) {
        if (rootPid <= 0) return;

        Set<Integer> signaled = new HashSet<>();
        for (ProcessInfo process : listDescendantProcesses(rootPid)) {
            if (process.pid > 0 && signaled.add(process.pid)) {
                Process.sendSignal(process.pid, signal);
                Log.d("ProcessHelper", "Signalled child process pid=" + process.pid + " ppid=" + process.ppid + " name=" + process.name + " signal=" + signal);
            }
        }
        if (signaled.add(rootPid)) {
            Process.sendSignal(rootPid, signal);
            Log.d("ProcessHelper", "Signalled root process pid=" + rootPid + " signal=" + signal);
        }
    }

    public static int exec(String command) {
        return exec(command, null);
    }

    public static int exec(String command, String[] envp) {
        return exec(command, envp, null);
    }

    public static int exec(String command, String[] envp, File workingDir) {
        return exec(command, envp, workingDir, null);
    }

    public static int exec(String command, String[] envp, File workingDir, Callback<Integer> terminationCallback) {
        Log.d("ProcessHelper", "env: " + Arrays.toString(envp) + "\ncmd: " + command);

        // Store env vars for future use
        EnvironmentManager.setEnvVars(envp);

        int pid = -1;
        boolean forensicMode = isForensicModeEnv(envp);
        int callbackCount = getDebugCallbackCount();
        boolean nativeLifecycleReady = ensureNativeLifecycleAvailable();
        try {
            Log.d("ProcessHelper", "Splitting command: " + command);
            String[] splitCommand = splitCommand(command);
            Log.d("ProcessHelper", "Split command result: " + Arrays.toString(splitCommand));
            Log.d("ProcessHelper", "Starting process...");
            ProcessBuilder pb = new ProcessBuilder(splitCommand);
            pb.directory(workingDir);
            pb.environment().putAll(EnvironmentManager.getEnvVars());
            if (callbackCount == 0 && !forensicMode) {
                File null_file = new File("/dev/null");
                pb.redirectError(null_file);
                pb.redirectOutput(null_file);
            }
            ForensicLogger.logEvent(
                    ForensicLogger.getAppContext(),
                    "info",
                    "PROCESS_EXEC_START",
                    null,
                    "process_helper",
                    "process_exec_start",
                    ForensicLogger.fields(
                            "command", command,
                            "working_dir", workingDir != null ? workingDir.getAbsolutePath() : "",
                            "forensic_mode", forensicMode ? "1" : "0",
                            "debug_callback_count", callbackCount,
                            "stream_capture", (callbackCount > 0 || forensicMode) ? "1" : "0",
                            "native_lifecycle_ready", nativeLifecycleReady ? "1" : "0",
                            "env_hash", hashEnvp(envp)
                    )
            );
            java.lang.Process process = pb.start();

            // Accessing hidden field
            Log.d("ProcessHelper", "Accessing hidden field to get PID");
            Field pidField = process.getClass().getDeclaredField("pid");
            pidField.setAccessible(true);
            pid = pidField.getInt(process);
            pidField.setAccessible(false);
            Log.d("ProcessHelper", "Process started with pid: " + pid);
            ForensicLogger.logEvent(
                    ForensicLogger.getAppContext(),
                    "info",
                    "PROCESS_EXEC_READY",
                    null,
                    "process_helper",
                    "process_exec_ready",
                    ForensicLogger.fields(
                            "pid", pid,
                            "command", command,
                            "forensic_mode", forensicMode ? "1" : "0",
                            "debug_callback_count", callbackCount,
                            "stream_capture", (callbackCount > 0 || forensicMode) ? "1" : "0"
                    )
            );

            if (callbackCount > 0 || forensicMode) {
                createDebugThread(process.getInputStream());
                createDebugThread(process.getErrorStream());
            }

            if (terminationCallback != null) createWaitForThread(process, terminationCallback);

        }
        catch (Exception e) {
            Log.e("ProcessHelper", "Error executing command: " + command, e);
        }
        return pid;
    }

    private static boolean isForensicModeEnv(String[] envp) {
        if (envp == null) return false;
        for (String entry : envp) {
            if (entry == null) continue;
            if (entry.startsWith("AERO_FORENSIC_MODE=")) {
                String value = entry.substring("AERO_FORENSIC_MODE=".length()).trim();
                return "1".equals(value) || "true".equalsIgnoreCase(value);
            }
        }
        return false;
    }

    private static int getDebugCallbackCount() {
        synchronized (debugCallbacks) {
            return debugCallbacks.size();
        }
    }

    private static String hashEnvp(String[] envp) {
        if (envp == null || envp.length == 0) return "";
        String[] copy = Arrays.copyOf(envp, envp.length);
        Arrays.sort(copy);
        return ForensicLogger.sha256Hex(String.join("\n", copy));
    }

    private static void createDebugThread(final InputStream inputStream) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (PRINT_DEBUG) System.out.println(line);
                    synchronized (debugCallbacks) {
                        if (!debugCallbacks.isEmpty()) {
                            for (Callback<String> callback : debugCallbacks) callback.call(line);
                        }
                    }
                }
            }
            catch (IOException e) {
                Log.e("ProcessHelper", "Error in debug thread", e);
            }
        });
    }

    private static void createWaitForThread(java.lang.Process process, final Callback<Integer> terminationCallback) {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    int status = process.waitFor();
                    startNativeReaperWindow(1500);
                    terminationCallback.call(status);
                }
                catch (InterruptedException e) {
                    Log.e("ProcessHelper", "Error waiting for process termination", e);
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    public static int reapDeadChildrenNow() {
        if (!ensureNativeLifecycleAvailable()) return 0;
        try {
            return nativeReapDeadChildrenNow();
        }
        catch (UnsatisfiedLinkError e) {
            nativeLifecycleAvailable = false;
            Log.w("ProcessHelper", "Native lifecycle reaper is unavailable", e);
            return 0;
        }
    }

    public static void startNativeReaperWindow(int durationMs) {
        if (durationMs <= 0 || !ensureNativeLifecycleAvailable()) return;
        try {
            nativeStartNativeReaperWindow(durationMs);
        }
        catch (UnsatisfiedLinkError e) {
            nativeLifecycleAvailable = false;
            Log.w("ProcessHelper", "Native lifecycle reaper is unavailable", e);
        }
    }

    private static boolean ensureNativeLifecycleAvailable() {
        if (nativeLifecycleAvailable) return true;
        if (nativeLifecycleLoadAttempted) return false;
        synchronized (ProcessHelper.class) {
            if (nativeLifecycleAvailable) return true;
            if (nativeLifecycleLoadAttempted) return false;
            nativeLifecycleLoadAttempted = true;
            try {
                WinlatorNative.ensureLoaded("process_lifecycle");
                nativeLifecycleAvailable = true;
                return true;
            }
            catch (Throwable e) {
                Log.w("ProcessHelper", "Native lifecycle support is unavailable", e);
                return false;
            }
        }
    }

    public static void removeAllDebugCallbacks() {
        synchronized (debugCallbacks) {
            debugCallbacks.clear();
            Log.d("ProcessHelper", "All debug callbacks removed");
        }
    }

    public static void addDebugCallback(Callback<String> callback) {
        synchronized (debugCallbacks) {
            if (!debugCallbacks.contains(callback)) debugCallbacks.add(callback);
            Log.d("ProcessHelper", "Added debug callback: " + callback.toString());
        }
    }

    public static void removeDebugCallback(Callback<String> callback) {
        synchronized (debugCallbacks) {
            debugCallbacks.remove(callback);
            Log.d("ProcessHelper", "Removed debug callback: " + callback.toString());
        }
    }

    public static String[] splitCommand(String command) {
        ArrayList<String> result = new ArrayList<>();
        boolean startedQuotes = false;
        String value = "";
        char currChar, nextChar;
        for (int i = 0, count = command.length(); i < count; i++) {
            currChar = command.charAt(i);

            if (startedQuotes) {
                if (currChar == '"') {
                    startedQuotes = false;
                    continue;
                }
                else value += currChar;
            }
            else if (currChar == '"') {
                startedQuotes = true;
            }
            else {
                nextChar = i < count-1 ? command.charAt(i+1) : '\0';
                if (currChar == ' ' || (currChar == '\\' && nextChar == ' ')) {
                    if (currChar == '\\') {
                        value += ' ';
                        i++;
                    }
                    else if (!value.isEmpty()) {
                        result.add(value);
                        value = "";
                    }
                }
                else {
                    value += currChar;
                    if (i == count-1) {
                        result.add(value);
                        value = "";
                    }
                }
            }
        }

        if (!value.isEmpty()) {
            result.add(value);
        }
        return result.toArray(new String[0]);
    }

    public static String getAffinityMaskAsHexString(String cpuList) {
        String[] values = cpuList.split(",");
        int affinityMask = 0;
        for (String value : values) {
            byte index = Byte.parseByte(value);
            affinityMask |= (int)Math.pow(2, index);
        }
        return Integer.toHexString(affinityMask);
    }

    public static int getAffinityMask(String cpuList) {
        if (cpuList == null || cpuList.isEmpty()) return 0;
        String[] values = cpuList.split(",");
        int affinityMask = 0;
        for (String value : values) {
            byte index = Byte.parseByte(value);
            affinityMask |= (int)Math.pow(2, index);
        }
        return affinityMask;
    }

    public static int getAffinityMask(boolean[] cpuList) {
        int affinityMask = 0;
        for (int i = 0; i < cpuList.length; i++) {
            if (cpuList[i]) affinityMask |= (int)Math.pow(2, i);
        }
        return affinityMask;
    }

    public static int getAffinityMask(int from, int to) {
        int affinityMask = 0;
        for (int i = from; i < to; i++) affinityMask |= (int)Math.pow(2, i);
        return affinityMask;
    }

    public static ArrayList<String> listRunningWineProcesses(){
        File proc = new File("/proc");
        String[] filters = {"wine", "exe"};
        String[] allPids;
        ArrayList<String> filteredPids = new ArrayList<String>();
        List<String> filterList = Arrays.asList(filters);
        allPids = proc.list(new FilenameFilter(){
            public boolean accept(File proc, String filename){
                return new File(proc, filename).isDirectory() && filename.matches("[0-9]+");
            }
        });
        if (allPids == null) return filteredPids;

        for (int index = 0; index < allPids.length; index++){
            String data = "";
            try {
                FileInputStream fr = new FileInputStream(proc + "/" + allPids[index] + "/stat");
                BufferedReader br = new BufferedReader(new InputStreamReader(fr));
                data = br.readLine();
            }
            catch (IOException e) {
                if (PRINT_DEBUG) {
                    Log.d("ProcessHelper", "Failed to read /proc stat for pid " + allPids[index], e);
                }
            }
            for (String filter : filterList) {
                if (data.contains(filter))
                    filteredPids.add(allPids[index]);
            }
        }
        return filteredPids;
    }

    public static List<ProcessInfo> listSubProcesses() {
        List<ProcessInfo> processes = new ArrayList<>();
        String myUser = null;

        try {
            java.lang.Process idProcess = Runtime.getRuntime().exec("id");
            try (
                    InputStreamReader isr = new InputStreamReader(idProcess.getInputStream());
                    BufferedReader idReader = new BufferedReader(isr)
            ) {
                String idOutput = idReader.readLine();
                if (idOutput != null) {
                    int startIndex = idOutput.indexOf('(');
                    int endIndex = idOutput.indexOf(')');
                    if (startIndex != -1 && endIndex != -1) {
                        myUser = idOutput.substring(startIndex + 1, endIndex);
                    }
                }
            }
        } catch (IOException e) {
            Log.e("ProcessHelper", "Failed to retrieve user id in order to list processes", e);
            return processes;
        }

        if (myUser == null) return processes;

        try {
            java.lang.Process process = Runtime.getRuntime().exec("ps -A -o USER,PID,PPID,VSZ,RSS,WCHAN,ADDR,S,NAME");
            try (
                    InputStreamReader isr = new InputStreamReader(process.getInputStream());
                    BufferedReader reader = new BufferedReader(isr)
            ) {
                String line;
                reader.readLine();

                while ((line = reader.readLine()) != null) {
                    String[] parts = line.trim().split("\\s+", 9);
                    if (parts.length >= 9) {
                        String user = parts[0];
                        int pid = Integer.parseInt(parts[1]);
                        int ppid = Integer.parseInt(parts[2]);
                        long rssKb = Long.parseLong(parts[4]);
                        String processName = parts[8];
                        if (user.equals(myUser) && pid != Process.myPid()) {
                            processes.add(new ProcessInfo(pid, ppid, processName, rssKb * 1024L));
                        }
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            Log.e("ProcessHelper", "Failed to list sub-processes", e);
        }

        return processes;
    }

    public static List<ProcessInfo> listDescendantProcesses(int rootPid) {
        if (rootPid <= 0) return Collections.emptyList();

        List<ProcessInfo> processes = listProcessesFromProc();
        ArrayList<ProcessInfo> descendants = new ArrayList<>();
        Set<Integer> frontier = new HashSet<>();
        frontier.add(rootPid);

        boolean changed;
        do {
            changed = false;
            for (ProcessInfo process : processes) {
                if (frontier.contains(process.ppid) && !frontier.contains(process.pid)) {
                    frontier.add(process.pid);
                    descendants.add(process);
                    changed = true;
                }
            }
        } while (changed);

        Collections.reverse(descendants);
        return descendants;
    }

    public static List<ProcessInfo> listProcessesFromProc() {
        File proc = new File("/proc");
        String[] allPids = proc.list((dir, filename) -> new File(dir, filename).isDirectory() && filename.matches("[0-9]+"));
        if (allPids == null || allPids.length == 0) return Collections.emptyList();

        ArrayList<ProcessInfo> processes = new ArrayList<>();
        for (String pidName : allPids) {
            File statFile = new File(new File(proc, pidName), "stat");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(statFile)))) {
                ProcessInfo info = parseProcStat(pidName, reader.readLine());
                if (info != null) processes.add(info);
            }
            catch (IOException | NumberFormatException e) {
                if (PRINT_DEBUG) Log.d("ProcessHelper", "Failed to read process stat for pid " + pidName, e);
            }
        }
        return processes;
    }

    static ProcessInfo parseProcStat(String pidName, String statLine) {
        if (statLine == null || statLine.isEmpty()) return null;

        int pid = Integer.parseInt(pidName);
        int nameStart = statLine.indexOf('(');
        int nameEnd = statLine.lastIndexOf(')');
        if (nameStart < 0 || nameEnd <= nameStart) return null;

        String name = statLine.substring(nameStart + 1, nameEnd);
        String tail = statLine.substring(nameEnd + 1).trim();
        String[] fields = tail.split("\\s+");
        if (fields.length < 2) return null;

        int ppid = Integer.parseInt(fields[1]);
        return new ProcessInfo(pid, ppid, name);
    }

    private static native int nativeReapDeadChildrenNow();

    private static native void nativeStartNativeReaperWindow(int durationMs);
}
