package com.winlator.cmod.core;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ForensicRuntimeSnapshot {
    public static final String SNAPSHOT_CONTRACT = "apk_gamehub_winmonitor_perf_lane_v1";
    private static final int MAX_PROCESSES = 64;
    private static final long FALLBACK_PAGE_SIZE = 4096L;

    private ForensicRuntimeSnapshot() {}

    public static JSONObject capture() {
        JSONObject root = new JSONObject();
        try {
            root.put("snapshotContract", SNAPSHOT_CONTRACT);
            root.put("host", buildHostSnapshot());
            JSONArray processRows = buildProcessRows();
            root.put("processCount", processRows.length());
            root.put("processes", processRows);
        }
        catch (JSONException ignored) { /* best-effort path; keep surrounding flow intact. */ }
        return root;
    }

    private static JSONObject buildHostSnapshot() {
        JSONObject host = new JSONObject();
        try {
            host.put("uptimeSeconds", readUptimeSeconds());

            String cpuLine = readFirstLine(new File("/proc/stat"));
            if (cpuLine != null && cpuLine.startsWith("cpu ")) {
                String[] parts = cpuLine.trim().split("\\s+");
                long user = parts.length > 1 ? parseLong(parts[1], 0L) : 0L;
                long nice = parts.length > 2 ? parseLong(parts[2], 0L) : 0L;
                long system = parts.length > 3 ? parseLong(parts[3], 0L) : 0L;
                long idle = parts.length > 4 ? parseLong(parts[4], 0L) : 0L;
                long ioWait = parts.length > 5 ? parseLong(parts[5], 0L) : 0L;
                long irq = parts.length > 6 ? parseLong(parts[6], 0L) : 0L;
                long softIrq = parts.length > 7 ? parseLong(parts[7], 0L) : 0L;
                long steal = parts.length > 8 ? parseLong(parts[8], 0L) : 0L;
                long total = user + nice + system + idle + ioWait + irq + softIrq + steal;
                JSONObject cpu = new JSONObject();
                cpu.put("userTicks", user);
                cpu.put("niceTicks", nice);
                cpu.put("systemTicks", system);
                cpu.put("idleTicks", idle);
                cpu.put("ioWaitTicks", ioWait);
                cpu.put("irqTicks", irq);
                cpu.put("softIrqTicks", softIrq);
                cpu.put("stealTicks", steal);
                cpu.put("totalTicks", total);
                host.put("cpu", cpu);
            }

            String loadLine = readFirstLine(new File("/proc/loadavg"));
            if (loadLine != null) {
                String[] parts = loadLine.trim().split("\\s+");
                JSONObject load = new JSONObject();
                load.put("avg1", parts.length > 0 ? parseFloat(parts[0], 0f) : 0f);
                load.put("avg5", parts.length > 1 ? parseFloat(parts[1], 0f) : 0f);
                load.put("avg15", parts.length > 2 ? parseFloat(parts[2], 0f) : 0f);
                host.put("loadAverage", load);
            }

            Map<String, Long> memInfoKb = readMemInfoKb();
            JSONObject memory = new JSONObject();
            long totalKb = memInfoKb.getOrDefault("MemTotal", -1L);
            long availableKb = memInfoKb.getOrDefault("MemAvailable", -1L);
            long freeKb = memInfoKb.getOrDefault("MemFree", -1L);
            long swapTotalKb = memInfoKb.getOrDefault("SwapTotal", -1L);
            long swapFreeKb = memInfoKb.getOrDefault("SwapFree", -1L);
            memory.put("totalBytes", kbToBytes(totalKb));
            memory.put("availableBytes", kbToBytes(availableKb));
            memory.put("freeBytes", kbToBytes(freeKb));
            memory.put("swapTotalBytes", kbToBytes(swapTotalKb));
            memory.put("swapFreeBytes", kbToBytes(swapFreeKb));
            memory.put("pageSizeBytes", getPageSizeBytes());
            host.put("memory", memory);
        }
        catch (JSONException ignored) { /* best-effort path; keep surrounding flow intact. */ }
        return host;
    }

    private static JSONArray buildProcessRows() {
        JSONArray out = new JSONArray();
        List<ProcessRow> rows = collectProcessRows();
        int limit = Math.min(MAX_PROCESSES, rows.size());
        for (int i = 0; i < limit; i++) {
            ProcessRow row = rows.get(i);
            JSONObject obj = new JSONObject();
            try {
                obj.put("pid", row.pid);
                obj.put("parentPid", row.parentPid);
                obj.put("state", row.state);
                obj.put("threads", row.threads);
                obj.put("priority", row.priority);
                obj.put("nice", row.nice);
                obj.put("cpuTicks", row.cpuTicks);
                obj.put("rssBytes", row.rssBytes);
                obj.put("virtualBytes", row.virtualBytes);
                obj.put("comm", row.comm);
                obj.put("cmdline", row.cmdline);
            }
            catch (JSONException ignored) { /* best-effort path; keep surrounding flow intact. */ }
            out.put(obj);
        }
        return out;
    }

    private static List<ProcessRow> collectProcessRows() {
        File proc = new File("/proc");
        File[] entries = proc.listFiles();
        if (entries == null || entries.length == 0) {
            return Collections.emptyList();
        }

        List<ProcessRow> rows = new ArrayList<>();
        for (File entry : entries) {
            if (!entry.isDirectory()) continue;
            String name = entry.getName();
            if (!isNumeric(name)) continue;
            int pid = parseInt(name, -1);
            if (pid <= 0) continue;
            ProcessRow row = readProcessRow(pid);
            if (row != null) rows.add(row);
        }

        rows.sort(Comparator
                .comparingLong((ProcessRow r) -> r.rssBytes).reversed()
                .thenComparingInt(r -> r.pid));
        return rows;
    }

    private static ProcessRow readProcessRow(int pid) {
        String statLine = readFirstLine(new File(String.format(Locale.US, "/proc/%d/stat", pid)));
        if (statLine == null || statLine.isEmpty()) {
            return null;
        }

        int openParen = statLine.indexOf('(');
        int closeParen = statLine.lastIndexOf(')');
        if (openParen < 0 || closeParen <= openParen) {
            return null;
        }

        String comm = statLine.substring(openParen + 1, closeParen).trim();
        String[] parts = statLine.substring(closeParen + 2).trim().split("\\s+");
        if (parts.length < 22) {
            return null;
        }

        String state = parts[0];
        int parentPid = parseInt(parts[1], -1);
        long userTicks = parseLong(parts[11], 0L);
        long systemTicks = parseLong(parts[12], 0L);
        long priority = parts.length > 15 ? parseLong(parts[15], 0L) : 0L;
        long nice = parts.length > 16 ? parseLong(parts[16], 0L) : 0L;
        int threads = parts.length > 17 ? parseInt(parts[17], 0) : 0;
        long virtualBytes = parts.length > 20 ? parseLong(parts[20], 0L) : 0L;
        long rssPages = parts.length > 21 ? parseLong(parts[21], 0L) : 0L;
        long rssBytes = Math.max(0L, rssPages) * getPageSizeBytes();
        long cpuTicks = userTicks + systemTicks;
        String cmdline = readCmdline(pid);
        if (cmdline.isEmpty()) cmdline = comm;

        return new ProcessRow(
                pid,
                parentPid,
                state,
                threads,
                priority,
                nice,
                cpuTicks,
                rssBytes,
                virtualBytes,
                comm,
                cmdline
        );
    }

    private static Map<String, Long> readMemInfoKb() {
        Map<String, Long> values = new HashMap<>();
        File memInfo = new File("/proc/meminfo");
        if (!memInfo.isFile()) {
            return values;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(memInfo)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int colon = line.indexOf(':');
                if (colon <= 0) continue;
                String key = line.substring(0, colon).trim();
                String payload = line.substring(colon + 1).trim();
                String[] parts = payload.split("\\s+");
                long valueKb = parts.length > 0 ? parseLong(parts[0], -1L) : -1L;
                values.put(key, valueKb);
            }
        }
        catch (IOException ignored) { /* best-effort path; keep surrounding flow intact. */ }
        return values;
    }

    private static long getPageSizeBytes() {
        return FALLBACK_PAGE_SIZE;
    }

    private static long readUptimeSeconds() {
        String line = readFirstLine(new File("/proc/uptime"));
        if (line == null || line.isEmpty()) {
            return -1L;
        }
        String[] parts = line.trim().split("\\s+");
        if (parts.length == 0) {
            return -1L;
        }
        return (long) parseFloat(parts[0], -1f);
    }

    private static String readCmdline(int pid) {
        File cmdlineFile = new File(String.format(Locale.US, "/proc/%d/cmdline", pid));
        if (!cmdlineFile.isFile()) {
            return "";
        }
        try (FileInputStream in = new FileInputStream(cmdlineFile)) {
            byte[] buffer = new byte[4096];
            int read = in.read(buffer);
            if (read <= 0) return "";
            StringBuilder builder = new StringBuilder(read);
            for (int i = 0; i < read; i++) {
                char ch = (char) buffer[i];
                builder.append(ch == '\0' ? ' ' : ch);
            }
            return builder.toString().trim().replaceAll("\\s+", " ");
        }
        catch (IOException ignored) {
            return "";
        }
    }

    private static String readFirstLine(File file) {
        if (!file.isFile()) return null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            return reader.readLine();
        }
        catch (IOException ignored) {
            return null;
        }
    }

    private static boolean isNumeric(String value) {
        if (value == null || value.isEmpty()) return false;
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) return false;
        }
        return true;
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        }
        catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static long parseLong(String raw, long fallback) {
        try {
            return Long.parseLong(raw);
        }
        catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static float parseFloat(String raw, float fallback) {
        try {
            return Float.parseFloat(raw);
        }
        catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static long kbToBytes(long kilobytes) {
        if (kilobytes < 0L) return -1L;
        return kilobytes * 1024L;
    }

    private static final class ProcessRow {
        final int pid;
        final int parentPid;
        final String state;
        final int threads;
        final long priority;
        final long nice;
        final long cpuTicks;
        final long rssBytes;
        final long virtualBytes;
        final String comm;
        final String cmdline;

        ProcessRow(int pid, int parentPid, String state, int threads, long priority, long nice,
                   long cpuTicks, long rssBytes, long virtualBytes, String comm, String cmdline) {
            this.pid = pid;
            this.parentPid = parentPid;
            this.state = state;
            this.threads = threads;
            this.priority = priority;
            this.nice = nice;
            this.cpuTicks = cpuTicks;
            this.rssBytes = rssBytes;
            this.virtualBytes = virtualBytes;
            this.comm = comm;
            this.cmdline = cmdline;
        }
    }
}
