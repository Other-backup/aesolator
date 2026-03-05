package com.winlator.cmod.winhandler;

import android.app.ActivityManager;
import android.content.Context;
import android.os.SystemClock;
import android.system.Os;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.SparseArray;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class LinuxTelemetrySampler {
    private static final long FALLBACK_PAGE_SIZE = 4096L;
    private static final long FALLBACK_CLOCK_TICKS = 100L;
    private static final float UNAVAILABLE_FLOAT = -1f;
    private final SparseArray<ProcessSample> previousSamples = new SparseArray<>();
    private CpuTotals previousCpuTotals;
    private NetTotals previousNetTotals;
    private SocketTables latestSocketTables;
    private long previousHostSampleElapsedMs;

    HostSample sampleHost(Context context) {
        long nowElapsedMs = SystemClock.elapsedRealtime();
        CpuTotals currentTotals = readCpuTotals();
        float cpuPercent = 0f;
        if (currentTotals != null && previousCpuTotals != null) {
            long deltaTotal = currentTotals.total - previousCpuTotals.total;
            long deltaIdle = currentTotals.idle - previousCpuTotals.idle;
            if (deltaTotal > 0) {
                cpuPercent = Math.max(0f, Math.min(100f, ((float) (deltaTotal - deltaIdle) * 100f) / (float) deltaTotal));
            }
        }
        previousCpuTotals = currentTotals;

        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        if (activityManager != null) {
            activityManager.getMemoryInfo(memoryInfo);
        }

        long totalBytes = memoryInfo.totalMem;
        long availableBytes = memoryInfo.availMem;
        long usedBytes = Math.max(0L, totalBytes - availableBytes);

        NetTotals currentNetTotals = readNetworkTotals();
        long rxBytesPerSecond = -1L;
        long txBytesPerSecond = -1L;
        if (currentNetTotals != null && previousNetTotals != null && previousHostSampleElapsedMs > 0L) {
            long deltaMs = nowElapsedMs - previousHostSampleElapsedMs;
            if (deltaMs > 0L) {
                long deltaRx = Math.max(0L, currentNetTotals.rxBytes - previousNetTotals.rxBytes);
                long deltaTx = Math.max(0L, currentNetTotals.txBytes - previousNetTotals.txBytes);
                rxBytesPerSecond = (deltaRx * 1000L) / deltaMs;
                txBytesPerSecond = (deltaTx * 1000L) / deltaMs;
            }
        }

        previousNetTotals = currentNetTotals;
        previousHostSampleElapsedMs = nowElapsedMs;
        latestSocketTables = readSocketTables();

        float[] loadAverage = readLoadAverage();
        PressureSample cpuPressure = readPressureSample("cpu");
        PressureSample ioPressure = readPressureSample("io");
        PressureSample memoryPressure = readPressureSample("memory");
        return new HostSample(
                cpuPercent,
                usedBytes,
                totalBytes,
                availableBytes,
                loadAverage[0],
                loadAverage[1],
                loadAverage[2],
                rxBytesPerSecond,
                txBytesPerSecond,
                cpuPressure.someAvg10,
                cpuPressure.fullAvg10,
                ioPressure.someAvg10,
                ioPressure.fullAvg10,
                memoryPressure.someAvg10,
                memoryPressure.fullAvg10);
    }

    @Nullable
    ProcessSample sampleProcess(int pid) {
        long nowElapsedMs = SystemClock.elapsedRealtime();
        ProcessSample previous = previousSamples.get(pid);
        ProcessSample current = readProcessSample(pid, previous, nowElapsedMs);
        if (current != null) {
            previousSamples.put(pid, current);
        } else {
            previousSamples.remove(pid);
        }
        return current;
    }

    void retainOnly(Set<Integer> livePids) {
        for (int i = previousSamples.size() - 1; i >= 0; i--) {
            int pid = previousSamples.keyAt(i);
            if (!livePids.contains(pid)) {
                previousSamples.removeAt(i);
            }
        }
    }

    @Nullable
    private ProcessSample readProcessSample(int pid, @Nullable ProcessSample previous, long nowElapsedMs) {
        File statFile = new File(String.format(Locale.US, "/proc/%d/stat", pid));
        if (!statFile.isFile()) {
            return null;
        }

        String statLine = readFirstLine(statFile);
        if (TextUtils.isEmpty(statLine)) {
            return null;
        }

        int openParen = statLine.indexOf('(');
        int closeParen = statLine.lastIndexOf(')');
        if (openParen < 0 || closeParen <= openParen) {
            return null;
        }

        String commandName = statLine.substring(openParen + 1, closeParen).trim();
        String[] fields = statLine.substring(closeParen + 2).trim().split("\\s+");
        if (fields.length < 22) {
            return null;
        }

        char state = fields[0].charAt(0);
        int parentPid = parseInt(fields[1], 0);
        long userTicks = parseLong(fields[11], 0L);
        long systemTicks = parseLong(fields[12], 0L);
        long priority = fields.length > 15 ? parseLong(fields[15], 0L) : 0L;
        long nice = fields.length > 16 ? parseLong(fields[16], 0L) : 0L;
        int threadCount = parseInt(fields[17], 0);
        long startTicks = parseLong(fields[19], 0L);
        long virtualBytes = parseLong(fields[20], 0L);
        long residentPages = parseLong(fields[21], 0L);
        long residentBytes = residentPages * getPageSizeBytes();
        long totalCpuTicks = userTicks + systemTicks;
        int lastCpu = fields.length > 36 ? parseInt(fields[36], -1) : -1;
        int schedulingPolicy = fields.length > 38 ? parseInt(fields[38], -1) : -1;

        long ageMs = Math.max(0L, nowElapsedMs - ticksToMillis(startTicks));
        float cpuPercent = 0f;
        if (previous != null) {
            long deltaTicks = totalCpuTicks - previous.totalCpuTicks;
            long deltaMs = nowElapsedMs - previous.sampleElapsedRealtimeMs;
            if (deltaTicks > 0L && deltaMs > 0L) {
                cpuPercent = ((float) deltaTicks * 1000f * 100f) / ((float) deltaMs * (float) getClockTicksPerSecond());
            }
        }

        StatusSnapshot statusSnapshot = readStatusSnapshot(pid);
        long[] ioBytes = readIoBytes(pid);
        long readRateBytes = -1L;
        long writeRateBytes = -1L;
        if (previous != null) {
            long deltaMs = nowElapsedMs - previous.sampleElapsedRealtimeMs;
            if (deltaMs > 0L) {
                if (ioBytes[0] >= 0L && previous.readBytes >= 0L) {
                    readRateBytes = (Math.max(0L, ioBytes[0] - previous.readBytes) * 1000L) / deltaMs;
                }
                if (ioBytes[1] >= 0L && previous.writeBytes >= 0L) {
                    writeRateBytes = (Math.max(0L, ioBytes[1] - previous.writeBytes) * 1000L) / deltaMs;
                }
            }
        }
        long totalContextSwitches = statusSnapshot.totalContextSwitches;
        float contextSwitchesPerSecond = UNAVAILABLE_FLOAT;
        if (previous != null && totalContextSwitches >= 0L && previous.totalContextSwitches >= 0L) {
            long deltaMs = nowElapsedMs - previous.sampleElapsedRealtimeMs;
            if (deltaMs > 0L) {
                long deltaContextSwitches = Math.max(0L, totalContextSwitches - previous.totalContextSwitches);
                contextSwitchesPerSecond = (deltaContextSwitches * 1000f) / (float) deltaMs;
            }
        }

        String commandLine = readCmdline(pid);
        int fileDescriptorCount = countDirectoryEntries(new File(String.format(Locale.US, "/proc/%d/fd", pid)));
        String waitChannel = sanitizeKernelValue(readFirstLine(new File(String.format(Locale.US, "/proc/%d/wchan", pid))));
        int oomScore = parseInt(readFirstLine(new File(String.format(Locale.US, "/proc/%d/oom_score", pid))), -1);
        SocketSnapshot socketSnapshot = readProcessSocketSnapshot(pid, latestSocketTables);
        if (TextUtils.isEmpty(commandLine)) {
            commandLine = commandName;
        }

        return new ProcessSample(
                pid,
                parentPid,
                commandName,
                commandLine,
                state,
                threadCount,
                residentBytes,
                virtualBytes,
                ioBytes[0],
                ioBytes[1],
                readRateBytes,
                writeRateBytes,
                statusSnapshot.cpuSetList,
                fileDescriptorCount,
                totalCpuTicks,
                cpuPercent,
                priority,
                nice,
                lastCpu,
                schedulingPolicy,
                statusSnapshot.swapBytes,
                statusSnapshot.rssAnonBytes,
                statusSnapshot.rssFileBytes,
                statusSnapshot.rssShmemBytes,
                statusSnapshot.voluntaryContextSwitches,
                statusSnapshot.nonVoluntaryContextSwitches,
                totalContextSwitches,
                contextSwitchesPerSecond,
                waitChannel,
                oomScore,
                socketSnapshot.tcpSocketCount,
                socketSnapshot.udpSocketCount,
                socketSnapshot.inetSocketCount,
                socketSnapshot.unixSocketCount,
                socketSnapshot.inetEndpointSummary,
                socketSnapshot.unixEndpointSummary,
                ageMs,
                nowElapsedMs);
    }

    private static CpuTotals readCpuTotals() {
        String line = readFirstLine(new File("/proc/stat"));
        if (TextUtils.isEmpty(line) || !line.startsWith("cpu ")) {
            return null;
        }

        String[] parts = line.trim().split("\\s+");
        if (parts.length < 5) {
            return null;
        }

        long total = 0L;
        for (int i = 1; i < parts.length; i++) {
            total += parseLong(parts[i], 0L);
        }
        long idle = parseLong(parts[4], 0L);
        long ioWait = parts.length > 5 ? parseLong(parts[5], 0L) : 0L;
        return new CpuTotals(total, idle + ioWait);
    }

    private static float[] readLoadAverage() {
        String line = readFirstLine(new File("/proc/loadavg"));
        float[] fallback = new float[]{0f, 0f, 0f};
        if (TextUtils.isEmpty(line)) {
            return fallback;
        }

        String[] parts = line.trim().split("\\s+");
        if (parts.length < 3) {
            return fallback;
        }
        return new float[]{
                parseFloat(parts[0], 0f),
                parseFloat(parts[1], 0f),
                parseFloat(parts[2], 0f)
        };
    }

    @Nullable
    private static NetTotals readNetworkTotals() {
        File netDev = new File("/proc/net/dev");
        if (!netDev.isFile()) {
            return null;
        }

        long rxBytes = 0L;
        long txBytes = 0L;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(netDev)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int colonIndex = line.indexOf(':');
                if (colonIndex <= 0) {
                    continue;
                }

                String iface = line.substring(0, colonIndex).trim();
                if ("lo".equals(iface)) {
                    continue;
                }

                String[] parts = line.substring(colonIndex + 1).trim().split("\\s+");
                if (parts.length < 16) {
                    continue;
                }

                rxBytes += parseLong(parts[0], 0L);
                txBytes += parseLong(parts[8], 0L);
            }
        } catch (IOException ignored) {
            return null;
        }

        return new NetTotals(rxBytes, txBytes);
    }

    private static long[] readIoBytes(int pid) {
        File ioFile = new File(String.format(Locale.US, "/proc/%d/io", pid));
        if (!ioFile.isFile()) {
            return new long[]{-1L, -1L};
        }

        long readBytes = -1L;
        long writeBytes = -1L;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(ioFile)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("read_bytes:")) {
                    readBytes = parseLong(line.substring(line.indexOf(':') + 1).trim(), -1L);
                } else if (line.startsWith("write_bytes:")) {
                    writeBytes = parseLong(line.substring(line.indexOf(':') + 1).trim(), -1L);
                }
            }
        } catch (IOException ignored) {
            return new long[]{-1L, -1L};
        }
        return new long[]{readBytes, writeBytes};
    }

    private static StatusSnapshot readStatusSnapshot(int pid) {
        File statusFile = new File(String.format(Locale.US, "/proc/%d/status", pid));
        if (!statusFile.isFile()) {
            return StatusSnapshot.empty();
        }

        String cpuSetList = "";
        long swapBytes = -1L;
        long rssAnonBytes = -1L;
        long rssFileBytes = -1L;
        long rssShmemBytes = -1L;
        long voluntaryContextSwitches = -1L;
        long nonVoluntaryContextSwitches = -1L;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(statusFile)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int colonIndex = line.indexOf(':');
                if (colonIndex <= 0) {
                    continue;
                }

                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                switch (key) {
                    case "Cpus_allowed_list":
                        cpuSetList = value;
                        break;
                    case "VmSwap":
                        swapBytes = parseMemoryValueToBytes(value);
                        break;
                    case "RssAnon":
                        rssAnonBytes = parseMemoryValueToBytes(value);
                        break;
                    case "RssFile":
                        rssFileBytes = parseMemoryValueToBytes(value);
                        break;
                    case "RssShmem":
                        rssShmemBytes = parseMemoryValueToBytes(value);
                        break;
                    case "voluntary_ctxt_switches":
                        voluntaryContextSwitches = parseLong(value, -1L);
                        break;
                    case "nonvoluntary_ctxt_switches":
                        nonVoluntaryContextSwitches = parseLong(value, -1L);
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException ignored) {
            return StatusSnapshot.empty();
        }

        return new StatusSnapshot(
                cpuSetList,
                swapBytes,
                rssAnonBytes,
                rssFileBytes,
                rssShmemBytes,
                voluntaryContextSwitches,
                nonVoluntaryContextSwitches);
    }

    private static String readCmdline(int pid) {
        File cmdlineFile = new File(String.format(Locale.US, "/proc/%d/cmdline", pid));
        if (!cmdlineFile.isFile()) {
            return "";
        }

        try (FileInputStream inputStream = new FileInputStream(cmdlineFile)) {
            byte[] buffer = new byte[4096];
            int count = inputStream.read(buffer);
            if (count <= 0) {
                return "";
            }

            StringBuilder builder = new StringBuilder(count);
            for (int i = 0; i < count; i++) {
                char ch = (char) buffer[i];
                if (ch == '\0') {
                    builder.append(' ');
                } else {
                    builder.append(ch);
                }
            }
            return builder.toString().trim().replaceAll("\\s+", " ");
        } catch (IOException ignored) {
            return "";
        }
    }

    private static String readFirstLine(File file) {
        if (!file.isFile()) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            return reader.readLine();
        } catch (IOException ignored) {
            return null;
        }
    }

    private static int countDirectoryEntries(File directory) {
        if (!directory.isDirectory()) {
            return -1;
        }

        String[] children = directory.list();
        return children != null ? children.length : -1;
    }

    private static SocketTables readSocketTables() {
        Map<Long, InetSocketEntry> inetSockets = new HashMap<>();
        Map<Long, UnixSocketEntry> unixSockets = new HashMap<>();
        readProcNetInetTable("/proc/net/tcp", "TCP", false, inetSockets);
        readProcNetInetTable("/proc/net/tcp6", "TCP6", true, inetSockets);
        readProcNetInetTable("/proc/net/udp", "UDP", false, inetSockets);
        readProcNetInetTable("/proc/net/udp6", "UDP6", true, inetSockets);
        readProcUnixTable("/proc/net/unix", unixSockets);
        return new SocketTables(inetSockets, unixSockets);
    }

    private static void readProcNetInetTable(String path, String protocol, boolean ipv6,
                                             Map<Long, InetSocketEntry> target) {
        File procNetFile = new File(path);
        if (!procNetFile.isFile()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(procNetFile)))) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }

                String[] parts = line.trim().split("\\s+");
                if (parts.length < 10) {
                    continue;
                }

                long inode = parseLong(parts[9], -1L);
                if (inode < 0L) {
                    continue;
                }
                target.put(inode, new InetSocketEntry(
                        protocol,
                        decodeProcAddress(parts[1], ipv6),
                        decodeProcAddress(parts[2], ipv6),
                        decodeSocketState(parts[3], protocol)));
            }
        } catch (IOException ignored) {
        }
    }

    private static void readProcUnixTable(String path, Map<Long, UnixSocketEntry> target) {
        File procUnixFile = new File(path);
        if (!procUnixFile.isFile()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(procUnixFile)))) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }

                String[] parts = line.trim().split("\\s+");
                if (parts.length < 7) {
                    continue;
                }

                long inode = parseLong(parts[6], -1L);
                if (inode < 0L) {
                    continue;
                }

                String pathValue = parts.length > 7 ? line.substring(line.indexOf(parts[7])) : "";
                target.put(inode, new UnixSocketEntry(parts[4], parts[5], pathValue));
            }
        } catch (IOException ignored) {
        }
    }

    private static SocketSnapshot readProcessSocketSnapshot(int pid, @Nullable SocketTables socketTables) {
        if (socketTables == null) {
            return SocketSnapshot.empty();
        }

        File fdDir = new File(String.format(Locale.US, "/proc/%d/fd", pid));
        if (!fdDir.isDirectory()) {
            return SocketSnapshot.empty();
        }

        String[] fdEntries = fdDir.list();
        if (fdEntries == null || fdEntries.length == 0) {
            return SocketSnapshot.empty();
        }

        LinkedHashSet<Long> socketInodes = new LinkedHashSet<>();
        for (String fdEntry : fdEntries) {
            try {
                String linkTarget = Files.readSymbolicLink(new File(fdDir, fdEntry).toPath()).toString();
                long inode = parseSocketInode(linkTarget);
                if (inode >= 0L) {
                    socketInodes.add(inode);
                }
            } catch (Exception ignored) {
            }
        }

        if (socketInodes.isEmpty()) {
            return SocketSnapshot.empty();
        }

        int inetSocketCount = 0;
        int tcpSocketCount = 0;
        int udpSocketCount = 0;
        int unixSocketCount = 0;
        LinkedHashSet<String> inetSummaries = new LinkedHashSet<>();
        LinkedHashSet<String> unixSummaries = new LinkedHashSet<>();
        for (Long inode : socketInodes) {
            InetSocketEntry inetSocketEntry = socketTables.inetSockets.get(inode);
            if (inetSocketEntry != null) {
                inetSocketCount++;
                if (inetSocketEntry.protocol.startsWith("TCP")) {
                    tcpSocketCount++;
                } else if (inetSocketEntry.protocol.startsWith("UDP")) {
                    udpSocketCount++;
                }
                if (inetSummaries.size() < 2) {
                    inetSummaries.add(formatInetSocketSummary(inetSocketEntry));
                }
                continue;
            }

            UnixSocketEntry unixSocketEntry = socketTables.unixSockets.get(inode);
            if (unixSocketEntry != null) {
                unixSocketCount++;
                if (unixSummaries.size() < 2) {
                    unixSummaries.add(formatUnixSocketSummary(unixSocketEntry));
                }
            }
        }

        return new SocketSnapshot(
                tcpSocketCount,
                udpSocketCount,
                inetSocketCount,
                unixSocketCount,
                TextUtils.join("; ", inetSummaries),
                TextUtils.join("; ", unixSummaries));
    }

    private static long parseSocketInode(String linkTarget) {
        if (TextUtils.isEmpty(linkTarget) || !linkTarget.startsWith("socket:[") || !linkTarget.endsWith("]")) {
            return -1L;
        }
        return parseLong(linkTarget.substring(8, linkTarget.length() - 1), -1L);
    }

    private static String formatInetSocketSummary(InetSocketEntry socketEntry) {
        if ("LISTEN".equals(socketEntry.state) || isUnboundRemote(socketEntry.remoteAddress)) {
            return socketEntry.protocol + " " + socketEntry.state + " " + socketEntry.localAddress;
        }
        return socketEntry.protocol + " " + socketEntry.state + " " + socketEntry.localAddress + "->" + socketEntry.remoteAddress;
    }

    private static String formatUnixSocketSummary(UnixSocketEntry socketEntry) {
        if (!TextUtils.isEmpty(socketEntry.path)) {
            return "UNIX " + trimKernelString(socketEntry.path, 34);
        }
        return "UNIX type=" + socketEntry.type + " st=" + socketEntry.state;
    }

    private static boolean isUnboundRemote(String remoteAddress) {
        return TextUtils.isEmpty(remoteAddress)
                || remoteAddress.endsWith(":0")
                || remoteAddress.startsWith("0.0.0.0:")
                || remoteAddress.startsWith("[::]:");
    }

    private static String decodeProcAddress(String rawAddress, boolean ipv6) {
        if (TextUtils.isEmpty(rawAddress)) {
            return "";
        }

        int colonIndex = rawAddress.lastIndexOf(':');
        if (colonIndex <= 0 || colonIndex >= rawAddress.length() - 1) {
            return rawAddress;
        }

        String addressHex = rawAddress.substring(0, colonIndex);
        int port = parseInt(rawAddress.substring(colonIndex + 1), 16);
        String host = ipv6 ? decodeIpv6HexAddress(addressHex) : decodeIpv4HexAddress(addressHex);
        return host + ":" + Math.max(port, 0);
    }

    private static String decodeIpv4HexAddress(String addressHex) {
        if (TextUtils.isEmpty(addressHex) || addressHex.length() != 8) {
            return addressHex;
        }

        byte[] address = new byte[4];
        for (int i = 0; i < 4; i++) {
            int sourceIndex = (3 - i) * 2;
            address[i] = (byte) parseInt(addressHex.substring(sourceIndex, sourceIndex + 2), 16);
        }
        return formatIpAddress(address, addressHex);
    }

    private static String decodeIpv6HexAddress(String addressHex) {
        if (TextUtils.isEmpty(addressHex) || addressHex.length() != 32) {
            return addressHex;
        }

        byte[] address = new byte[16];
        for (int chunk = 0; chunk < 4; chunk++) {
            int chunkOffset = chunk * 8;
            for (int byteIndex = 0; byteIndex < 4; byteIndex++) {
                int sourceIndex = chunkOffset + ((3 - byteIndex) * 2);
                address[(chunk * 4) + byteIndex] = (byte) parseInt(addressHex.substring(sourceIndex, sourceIndex + 2), 16);
            }
        }
        return formatIpAddress(address, addressHex);
    }

    private static String formatIpAddress(byte[] address, String fallback) {
        try {
            return java.net.InetAddress.getByAddress(address).getHostAddress();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String decodeSocketState(String rawState, String protocol) {
        if (TextUtils.isEmpty(rawState)) {
            return protocol.startsWith("UDP") ? "UNCONN" : "UNKNOWN";
        }
        switch (rawState) {
            case "01":
                return "EST";
            case "02":
                return "SYN_SENT";
            case "03":
                return "SYN_RECV";
            case "04":
                return "FIN_WAIT1";
            case "05":
                return "FIN_WAIT2";
            case "06":
                return "TIME_WAIT";
            case "07":
                return protocol.startsWith("UDP") ? "UNCONN" : "CLOSE";
            case "08":
                return "CLOSE_WAIT";
            case "09":
                return "LAST_ACK";
            case "0A":
                return "LISTEN";
            case "0B":
                return "CLOSING";
            default:
                return rawState;
        }
    }

    private static String trimKernelString(String value, int maxLength) {
        if (TextUtils.isEmpty(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(1, maxLength - 3)) + "...";
    }

    private static PressureSample readPressureSample(String category) {
        File pressureFile = new File("/proc/pressure/" + category);
        if (!pressureFile.isFile()) {
            return PressureSample.unavailable();
        }

        float someAvg10 = UNAVAILABLE_FLOAT;
        float fullAvg10 = UNAVAILABLE_FLOAT;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(pressureFile)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("some ")) {
                    someAvg10 = parsePsiAvg10(line);
                } else if (line.startsWith("full ")) {
                    fullAvg10 = parsePsiAvg10(line);
                }
            }
        } catch (IOException ignored) {
            return PressureSample.unavailable();
        }
        return new PressureSample(someAvg10, fullAvg10);
    }

    private static float parsePsiAvg10(String line) {
        String[] parts = line.trim().split("\\s+");
        for (String part : parts) {
            if (part.startsWith("avg10=")) {
                return parseFloat(part.substring("avg10=".length()), UNAVAILABLE_FLOAT);
            }
        }
        return UNAVAILABLE_FLOAT;
    }

    private static long parseMemoryValueToBytes(String rawValue) {
        if (TextUtils.isEmpty(rawValue)) {
            return -1L;
        }

        String[] parts = rawValue.trim().split("\\s+");
        if (parts.length == 0) {
            return -1L;
        }

        long value = parseLong(parts[0], -1L);
        if (value < 0L) {
            return -1L;
        }
        if (parts.length > 1 && "kb".equalsIgnoreCase(parts[1])) {
            return value * 1024L;
        }
        return value;
    }

    private static String sanitizeKernelValue(@Nullable String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return "0".equals(normalized) ? "" : normalized;
    }

    private static long getPageSizeBytes() {
        try {
            return Os.sysconf(OsConstants._SC_PAGESIZE);
        } catch (Exception ignored) {
            return FALLBACK_PAGE_SIZE;
        }
    }

    private static long getClockTicksPerSecond() {
        try {
            return Os.sysconf(OsConstants._SC_CLK_TCK);
        } catch (Exception ignored) {
            return FALLBACK_CLOCK_TICKS;
        }
    }

    private static long ticksToMillis(long ticks) {
        return (ticks * 1000L) / Math.max(1L, getClockTicksPerSecond());
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    static final class HostSample {
        final float cpuPercent;
        final long usedBytes;
        final long totalBytes;
        final long availableBytes;
        final float loadAverage1m;
        final float loadAverage5m;
        final float loadAverage15m;
        final long rxBytesPerSecond;
        final long txBytesPerSecond;
        final float cpuPressureSome10;
        final float cpuPressureFull10;
        final float ioPressureSome10;
        final float ioPressureFull10;
        final float memoryPressureSome10;
        final float memoryPressureFull10;

        HostSample(float cpuPercent, long usedBytes, long totalBytes, long availableBytes,
                   float loadAverage1m, float loadAverage5m, float loadAverage15m,
                   long rxBytesPerSecond, long txBytesPerSecond,
                   float cpuPressureSome10, float cpuPressureFull10,
                   float ioPressureSome10, float ioPressureFull10,
                   float memoryPressureSome10, float memoryPressureFull10) {
            this.cpuPercent = cpuPercent;
            this.usedBytes = usedBytes;
            this.totalBytes = totalBytes;
            this.availableBytes = availableBytes;
            this.loadAverage1m = loadAverage1m;
            this.loadAverage5m = loadAverage5m;
            this.loadAverage15m = loadAverage15m;
            this.rxBytesPerSecond = rxBytesPerSecond;
            this.txBytesPerSecond = txBytesPerSecond;
            this.cpuPressureSome10 = cpuPressureSome10;
            this.cpuPressureFull10 = cpuPressureFull10;
            this.ioPressureSome10 = ioPressureSome10;
            this.ioPressureFull10 = ioPressureFull10;
            this.memoryPressureSome10 = memoryPressureSome10;
            this.memoryPressureFull10 = memoryPressureFull10;
        }
    }

    static final class ProcessSample {
        final int pid;
        final int parentPid;
        final String commandName;
        final String commandLine;
        final char state;
        final int threadCount;
        final long residentBytes;
        final long virtualBytes;
        final long readBytes;
        final long writeBytes;
        final long readRateBytes;
        final long writeRateBytes;
        final String cpuSetList;
        final int fileDescriptorCount;
        final long totalCpuTicks;
        final float cpuPercent;
        final long priority;
        final long nice;
        final int lastCpu;
        final int schedulingPolicy;
        final long swapBytes;
        final long rssAnonBytes;
        final long rssFileBytes;
        final long rssShmemBytes;
        final long voluntaryContextSwitches;
        final long nonVoluntaryContextSwitches;
        final long totalContextSwitches;
        final float contextSwitchesPerSecond;
        final String waitChannel;
        final int oomScore;
        final int tcpSocketCount;
        final int udpSocketCount;
        final int inetSocketCount;
        final int unixSocketCount;
        final String inetEndpointSummary;
        final String unixEndpointSummary;
        final long ageMs;
        final long sampleElapsedRealtimeMs;

        ProcessSample(
                int pid,
                int parentPid,
                String commandName,
                String commandLine,
                char state,
                int threadCount,
                long residentBytes,
                long virtualBytes,
                long readBytes,
                long writeBytes,
                long readRateBytes,
                long writeRateBytes,
                String cpuSetList,
                int fileDescriptorCount,
                long totalCpuTicks,
                float cpuPercent,
                long priority,
                long nice,
                int lastCpu,
                int schedulingPolicy,
                long swapBytes,
                long rssAnonBytes,
                long rssFileBytes,
                long rssShmemBytes,
                long voluntaryContextSwitches,
                long nonVoluntaryContextSwitches,
                long totalContextSwitches,
                float contextSwitchesPerSecond,
                String waitChannel,
                int oomScore,
                int tcpSocketCount,
                int udpSocketCount,
                int inetSocketCount,
                int unixSocketCount,
                String inetEndpointSummary,
                String unixEndpointSummary,
                long ageMs,
                long sampleElapsedRealtimeMs) {
            this.pid = pid;
            this.parentPid = parentPid;
            this.commandName = commandName;
            this.commandLine = commandLine;
            this.state = state;
            this.threadCount = threadCount;
            this.residentBytes = residentBytes;
            this.virtualBytes = virtualBytes;
            this.readBytes = readBytes;
            this.writeBytes = writeBytes;
            this.readRateBytes = readRateBytes;
            this.writeRateBytes = writeRateBytes;
            this.cpuSetList = cpuSetList;
            this.fileDescriptorCount = fileDescriptorCount;
            this.totalCpuTicks = totalCpuTicks;
            this.cpuPercent = cpuPercent;
            this.priority = priority;
            this.nice = nice;
            this.lastCpu = lastCpu;
            this.schedulingPolicy = schedulingPolicy;
            this.swapBytes = swapBytes;
            this.rssAnonBytes = rssAnonBytes;
            this.rssFileBytes = rssFileBytes;
            this.rssShmemBytes = rssShmemBytes;
            this.voluntaryContextSwitches = voluntaryContextSwitches;
            this.nonVoluntaryContextSwitches = nonVoluntaryContextSwitches;
            this.totalContextSwitches = totalContextSwitches;
            this.contextSwitchesPerSecond = contextSwitchesPerSecond;
            this.waitChannel = waitChannel;
            this.oomScore = oomScore;
            this.tcpSocketCount = tcpSocketCount;
            this.udpSocketCount = udpSocketCount;
            this.inetSocketCount = inetSocketCount;
            this.unixSocketCount = unixSocketCount;
            this.inetEndpointSummary = inetEndpointSummary;
            this.unixEndpointSummary = unixEndpointSummary;
            this.ageMs = ageMs;
            this.sampleElapsedRealtimeMs = sampleElapsedRealtimeMs;
        }

        boolean hasIoStats() {
            return readBytes >= 0L || writeBytes >= 0L;
        }

        boolean hasIoRate() {
            return readRateBytes >= 0L || writeRateBytes >= 0L;
        }

        boolean hasContextSwitchRate() {
            return contextSwitchesPerSecond >= 0f;
        }

        boolean hasRssBreakdown() {
            return rssAnonBytes >= 0L || rssFileBytes >= 0L || rssShmemBytes >= 0L || swapBytes >= 0L;
        }

        boolean hasSocketTelemetry() {
            return inetSocketCount > 0 || unixSocketCount > 0;
        }

        boolean hasTcpSockets() {
            return tcpSocketCount > 0;
        }

        boolean hasUdpSockets() {
            return udpSocketCount > 0;
        }

        boolean hasUnixSockets() {
            return unixSocketCount > 0;
        }
    }

    private static final class CpuTotals {
        final long total;
        final long idle;

        CpuTotals(long total, long idle) {
            this.total = total;
            this.idle = idle;
        }
    }

    private static final class NetTotals {
        final long rxBytes;
        final long txBytes;

        NetTotals(long rxBytes, long txBytes) {
            this.rxBytes = rxBytes;
            this.txBytes = txBytes;
        }
    }

    private static final class SocketTables {
        final Map<Long, InetSocketEntry> inetSockets;
        final Map<Long, UnixSocketEntry> unixSockets;

        SocketTables(Map<Long, InetSocketEntry> inetSockets, Map<Long, UnixSocketEntry> unixSockets) {
            this.inetSockets = inetSockets;
            this.unixSockets = unixSockets;
        }
    }

    private static final class InetSocketEntry {
        final String protocol;
        final String localAddress;
        final String remoteAddress;
        final String state;

        InetSocketEntry(String protocol, String localAddress, String remoteAddress, String state) {
            this.protocol = protocol;
            this.localAddress = localAddress;
            this.remoteAddress = remoteAddress;
            this.state = state;
        }
    }

    private static final class UnixSocketEntry {
        final String type;
        final String state;
        final String path;

        UnixSocketEntry(String type, String state, String path) {
            this.type = type;
            this.state = state;
            this.path = path;
        }
    }

    private static final class SocketSnapshot {
        final int tcpSocketCount;
        final int udpSocketCount;
        final int inetSocketCount;
        final int unixSocketCount;
        final String inetEndpointSummary;
        final String unixEndpointSummary;

        SocketSnapshot(int tcpSocketCount, int udpSocketCount, int inetSocketCount, int unixSocketCount,
                       String inetEndpointSummary, String unixEndpointSummary) {
            this.tcpSocketCount = tcpSocketCount;
            this.udpSocketCount = udpSocketCount;
            this.inetSocketCount = inetSocketCount;
            this.unixSocketCount = unixSocketCount;
            this.inetEndpointSummary = inetEndpointSummary;
            this.unixEndpointSummary = unixEndpointSummary;
        }

        static SocketSnapshot empty() {
            return new SocketSnapshot(0, 0, 0, 0, "", "");
        }
    }

    private static final class StatusSnapshot {
        final String cpuSetList;
        final long swapBytes;
        final long rssAnonBytes;
        final long rssFileBytes;
        final long rssShmemBytes;
        final long voluntaryContextSwitches;
        final long nonVoluntaryContextSwitches;
        final long totalContextSwitches;

        StatusSnapshot(String cpuSetList, long swapBytes, long rssAnonBytes, long rssFileBytes, long rssShmemBytes,
                       long voluntaryContextSwitches, long nonVoluntaryContextSwitches) {
            this.cpuSetList = cpuSetList;
            this.swapBytes = swapBytes;
            this.rssAnonBytes = rssAnonBytes;
            this.rssFileBytes = rssFileBytes;
            this.rssShmemBytes = rssShmemBytes;
            this.voluntaryContextSwitches = voluntaryContextSwitches;
            this.nonVoluntaryContextSwitches = nonVoluntaryContextSwitches;
            if (voluntaryContextSwitches < 0L && nonVoluntaryContextSwitches < 0L) {
                this.totalContextSwitches = -1L;
            } else {
                this.totalContextSwitches = Math.max(0L, voluntaryContextSwitches) + Math.max(0L, nonVoluntaryContextSwitches);
            }
        }

        static StatusSnapshot empty() {
            return new StatusSnapshot("", -1L, -1L, -1L, -1L, -1L, -1L);
        }
    }

    private static final class PressureSample {
        final float someAvg10;
        final float fullAvg10;

        PressureSample(float someAvg10, float fullAvg10) {
            this.someAvg10 = someAvg10;
            this.fullAvg10 = fullAvg10;
        }

        static PressureSample unavailable() {
            return new PressureSample(UNAVAILABLE_FLOAT, UNAVAILABLE_FLOAT);
        }
    }
}
