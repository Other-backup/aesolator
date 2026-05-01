package com.winlator.cmod.core;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.PrintWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ForensicLogger {
    private static final String TAG = "ForensicLogger";
    private static final Object FILE_LOCK = new Object();
    private static final ExecutorService FILE_WRITE_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "ForensicLoggerWriter");
        thread.setDaemon(true);
        return thread;
    });
    private static final String SINK_EXTERNAL = "external";
    private static final String SINK_APP_EXTERNAL = "app_external";
    private static final String SINK_APP_PRIVATE = "app_private";
    private static final ThreadLocal<SimpleDateFormat> TS_FORMAT = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
    );
    private static final ArrayList<String> activeFileSinkKeys = new ArrayList<>();
    private static volatile Context appContext;
    private static volatile boolean crashHandlerInstalled = false;
    private static volatile String activeTraceId = "";

    private ForensicLogger() {}

    public static void initialize(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
        }
    }

    public static Context getAppContext() {
        return appContext;
    }

    public static void setActiveTraceId(String traceId) {
        activeTraceId = normalizeTraceId(traceId);
    }

    public static void clearActiveTraceId(String expectedTraceId) {
        String expected = normalizeTraceId(expectedTraceId);
        String current = activeTraceId;
        if (expected.isEmpty() || expected.equals(current)) {
            activeTraceId = "";
        }
    }

    public static String getActiveTraceId() {
        String traceId = activeTraceId;
        return traceId == null || traceId.isEmpty() ? null : traceId;
    }

    public static void installCrashHandler(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
        }
        if (crashHandlerInstalled || appContext == null) return;
        synchronized (FILE_LOCK) {
            if (crashHandlerInstalled || appContext == null) return;
            final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
                try {
                    writeFatalCrashSync(appContext, thread, error, "uncaught_exception");
                } catch (Throwable sinkError) {
                    Log.e(TAG, "Failed to persist fatal crash forensic", sinkError);
                }

                if (previous != null) {
                    previous.uncaughtException(thread, error);
                } else {
                    System.exit(10);
                }
            });
            crashHandlerInstalled = true;
        }
    }

    public static String newTraceId() {
        long now = System.currentTimeMillis();
        int salt = (int) (Math.random() * 0xffff);
        return String.format(Locale.US, "tr-%x-%04x", now, salt);
    }

    public static void info(Context context, String eventId, String traceId, String stage, String message) {
        logEvent(context, "info", eventId, traceId, stage, message, null);
    }

    public static void appInfo(String eventId, String stage, String message) {
        logEvent(appContext, "info", eventId, null, stage, message, null);
    }

    public static void warn(Context context, String eventId, String traceId, String stage, String message, JSONObject fields) {
        logEvent(context, "warn", eventId, traceId, stage, message, fields);
    }

    public static void appWarn(String eventId, String stage, String message, JSONObject fields) {
        logEvent(appContext, "warn", eventId, null, stage, message, fields);
    }

    public static void checkpoint(Context context, String severity, String eventId, String traceId, String stage, String message, JSONObject fields) {
        String line = buildEventLine(severity, eventId, traceId, stage, message, fields);
        if (line == null) return;
        logcat(severity, line);
        if (context == null) return;
        synchronized (FILE_LOCK) {
            appendForensicLine(context.getApplicationContext(), line);
        }
    }

    public static void appCheckpoint(String severity, String eventId, String stage, String message, JSONObject fields) {
        checkpoint(appContext, severity, eventId, null, stage, message, fields);
    }

    public static void error(Context context, String eventId, String traceId, String stage, String message, Throwable error, JSONObject fields) {
        JSONObject merged = new JSONObject();
        if (fields != null) {
            JSONArray names = fields.names();
            if (names != null) {
                for (int i = 0; i < names.length(); i++) {
                    String key = names.optString(i, "");
                    if (key.isEmpty()) continue;
                    try {
                        merged.put(key, fields.opt(key));
                    }
                    catch (JSONException ignored) {
                        // Skip one malformed field and preserve the rest of the forensic payload.
                    }
                }
            }
        }
        if (error != null) {
            try {
                merged.put("error_class", error.getClass().getName());
                merged.put("error_detail", String.valueOf(error.getMessage()));
            }
            catch (JSONException ignored) {
                // Keep the event even if one error detail cannot be encoded.
            }
        }
        logEvent(context, "error", eventId, traceId, stage, message, merged);
    }

    public static void appError(String eventId, String stage, String message, Throwable error, JSONObject fields) {
        error(appContext, eventId, null, stage, message, error, fields);
    }

    public static void logEvent(Context context, String severity, String eventId, String traceId, String stage, String message, JSONObject fields) {
        String line = buildEventLine(severity, eventId, traceId, stage, message, fields);
        if (line == null) return;
        logcat(severity, line);
        if (context == null) return;
        Context sinkContext = context.getApplicationContext();
        FILE_WRITE_EXECUTOR.execute(() -> {
            synchronized (FILE_LOCK) {
                appendForensicLine(sinkContext, line);
            }
        });
    }

    private static String buildEventLine(String severity, String eventId, String traceId, String stage, String message, JSONObject fields) {
        JSONObject obj = new JSONObject();
        try {
            String resolvedTraceId = resolveTraceId(traceId);
            obj.put("ts", TS_FORMAT.get().format(new Date()));
            obj.put("event_id", sanitize(eventId));
            obj.put("severity", sanitize(severity));
            obj.put("trace_id", resolvedTraceId == null ? JSONObject.NULL : resolvedTraceId);
            obj.put("stage", stage == null ? JSONObject.NULL : stage);
            obj.put("message", message == null ? "" : message);

            if (fields != null) {
                JSONArray names = fields.names();
                if (names != null) {
                    for (int i = 0; i < names.length(); i++) {
                        String key = names.optString(i, "");
                        if (key.isEmpty()) continue;
                        obj.put(key, fields.opt(key));
                    }
                }
            }
        }
        catch (JSONException e) {
            Log.e(TAG, "Failed to build forensic event", e);
            return null;
        }
        return obj.toString();
    }

    private static String resolveTraceId(String traceId) {
        String explicitTraceId = normalizeTraceId(traceId);
        if (!explicitTraceId.isEmpty()) return explicitTraceId;
        String current = activeTraceId;
        return current == null || current.isEmpty() ? null : current;
    }

    private static String normalizeTraceId(String traceId) {
        return traceId == null ? "" : traceId.trim();
    }

    public static JSONObject fields(Object... keyValues) {
        JSONObject obj = new JSONObject();
        if (keyValues == null) return obj;
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            String key = String.valueOf(keyValues[i]);
            Object value = keyValues[i + 1];
            try {
                obj.put(key, value == null ? JSONObject.NULL : value);
            }
            catch (JSONException ignored) {
                // Preserve remaining fields when one value is not JSON-compatible.
            }
        }
        return obj;
    }

    public static String hashEnvVars(EnvVars envVars) {
        if (envVars == null || envVars.isEmpty()) return "";
        String[] values = envVars.toStringArray();
        Arrays.sort(values);
        return sha256Hex(String.join("\n", values));
    }

    public static String sha256Hex(String value) {
        return sha256Hex(value != null ? value.getBytes(StandardCharsets.UTF_8) : new byte[0]);
    }

    public static String sha256Hex(byte[] value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] out = digest.digest(value);
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) sb.append(String.format(Locale.US, "%02x", b));
            return sb.toString();
        }
        catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    public static File getForensicsDir(Context context) {
        File dir = getWritableForensicsDir(context);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static File getAppPrivateForensicsDir(Context context) {
        File dir = new File(new File(context.getFilesDir(), "Winlator/logs"), "forensics");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static File getCurrentLogFile(Context context) {
        String fileName = "forensics_" + DateFormat.format("yyyy-MM-dd", new Date()) + ".jsonl";
        return new File(getWritableForensicsDir(context), fileName);
    }

    public static File getLatestLogFile(Context context) {
        ArrayList<File> files = getForensicLogFiles(context);
        return files.isEmpty() ? null : files.get(0);
    }

    public static ArrayList<File> getForensicLogFiles(Context context) {
        ArrayList<File> out = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (ForensicSink sink : getForensicSinks(context)) {
            File[] files = sink.dir.listFiles((d, name) -> name.endsWith(".jsonl"));
            if (files == null) continue;
            for (File file : files) {
                if (seen.add(file.getAbsolutePath())) out.add(file);
            }
        }
        out.sort((left, right) -> Long.compare(right.lastModified(), left.lastModified()));
        return out;
    }

    public static File createExportFile(Context context, String fileName) {
        String safeName = fileName == null || fileName.trim().isEmpty()
                ? "forensics_session_" + DateFormat.format("yyyy-MM-dd", new Date()) + ".jsonl"
                : fileName.trim();
        for (ForensicSink sink : getForensicSinks(context)) {
            File dir = new File(sink.dir, "exports");
            if (!dir.exists() && !dir.mkdirs() && !dir.exists()) continue;
            File out = new File(dir, safeName);
            try (FileWriter ignored = new FileWriter(out, true)) {
                return out;
            }
            catch (IOException ignored) {
            }
        }
        return new File(getForensicsDir(context), safeName);
    }

    public static String buildExportBody(Context context, File preferredFile, String fallbackTail) {
        return buildExportBodyForDay(context, preferredFile, fallbackTail, DateFormat.format("yyyy-MM-dd", new Date()).toString());
    }

    public static String buildExportBodyForDay(Context context, File preferredFile, String fallbackTail, String dayKey) {
        ArrayList<File> files = getForensicLogFiles(context);
        if (dayKey != null && !dayKey.trim().isEmpty()) {
            String normalizedDay = dayKey.trim();
            files.removeIf(file -> file == null || !isForensicFileForDay(file, normalizedDay));
        }
        if (preferredFile != null && preferredFile.isFile()) {
            String preferredPath = preferredFile.getAbsolutePath();
            files.removeIf(file -> preferredPath.equals(file.getAbsolutePath()));
            files.add(0, preferredFile);
        }
        if (files.isEmpty()) return fallbackTail == null ? "" : fallbackTail;

        StringBuilder out = new StringBuilder();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (File file : files) {
            if (file == null || !file.isFile() || !seen.add(file.getAbsolutePath())) continue;
            out.append(buildExportSourceLine(file)).append('\n');
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.append(line).append('\n');
                }
            }
            catch (IOException e) {
                out.append(buildExportReadErrorLine(file, e)).append('\n');
            }
        }
        return out.toString().trim();
    }

    private static boolean isForensicFileForDay(File file, String dayKey) {
        if (file == null || dayKey == null || dayKey.isEmpty()) return false;
        String name = file.getName();
        if (name.contains(dayKey)) return true;
        Calendar fileCal = Calendar.getInstance();
        fileCal.setTimeInMillis(file.lastModified());
        String fileDay = String.format(Locale.US, "%04d-%02d-%02d",
                fileCal.get(Calendar.YEAR),
                fileCal.get(Calendar.MONTH) + 1,
                fileCal.get(Calendar.DAY_OF_MONTH));
        return dayKey.equals(fileDay);
    }

    public static String describeLatestTrace(Context context) {
        File file = getLatestLogFile(context);
        if (file == null || !file.isFile()) return "No forensic trace file found.";

        String lastLine = null;
        ArrayList<String> lastLines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                lastLine = line;
                if (lastLines.size() == 5) lastLines.remove(0);
                lastLines.add(line);
            }
        }
        catch (IOException e) {
            return "Failed reading forensic log: " + e.getMessage();
        }

        if (lastLine == null) return "Forensic log is empty: " + file.getAbsolutePath();
        try {
            JSONObject obj = new JSONObject(lastLine);
            return "file=" + file.getName()
                    + ", event=" + obj.optString("event_id", "?")
                    + ", trace=" + obj.optString("trace_id", "")
                    + ", stage=" + obj.optString("stage", "")
                    + ", msg=" + obj.optString("message", "");
        }
        catch (JSONException e) {
            return "Latest forensic line is not JSON (" + file.getName() + "): " + lastLine;
        }
    }

    public static JSONObject snapshotFields(JSONObject base, String prefix, JSONObject source) {
        JSONObject out = base != null ? base : new JSONObject();
        if (source == null) return out;
        JSONArray names = source.names();
        if (names == null) return out;
        for (int i = 0; i < names.length(); i++) {
            String key = names.optString(i, "");
            if (key.isEmpty()) continue;
            try {
                out.put(prefix + key, source.opt(key));
            }
            catch (JSONException ignored) {
                // Preserve remaining prefixed fields when one value cannot be encoded.
            }
        }
        return out;
    }

    public static String readLatestTraceTail(Context context, int maxLines, int maxChars) {
        if (context == null) return "";
        File file = getLatestLogFile(context);
        if (file == null || !file.isFile()) return "";

        int safeMaxLines = Math.max(1, maxLines);
        int safeMaxChars = Math.max(0, maxChars);
        ArrayDeque<String> lines = new ArrayDeque<>(safeMaxLines);

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (lines.size() >= safeMaxLines) lines.removeFirst();
                lines.addLast(line);
            }
        } catch (IOException e) {
            return "Failed reading forensic log: " + e.getMessage();
        }

        if (lines.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append('\n');
        }

        if (safeMaxChars > 0 && sb.length() > safeMaxChars) {
            return "[tail truncated]\n" + sb.substring(sb.length() - safeMaxChars);
        }
        return sb.toString().trim();
    }

    private static void logcat(String severity, String line) {
        switch (severity == null ? "info" : severity.toLowerCase(Locale.US)) {
            case "warn":
                Log.w(TAG, line);
                break;
            case "error":
                Log.e(TAG, line);
                break;
            default:
                Log.i(TAG, line);
                break;
        }
    }

    private static String sanitize(String value) {
        return value == null ? "" : value;
    }

    private static File getExternalForensicsDir(Context context) {
        return new File(WinlatorLogUtils.getLogsDir(context), "forensics");
    }

    private static File getAppExternalForensicsDir(Context context) {
        return new File(WinlatorLogUtils.getAppExternalLogsDir(context), "forensics");
    }

    private static File getWritableForensicsDir(Context context) {
        for (ForensicSink sink : getForensicSinks(context)) {
            File dir = sink.dir;
            if (!dir.exists() && !dir.mkdirs() && !dir.exists()) continue;
            File probe = new File(dir, ".probe");
            try (FileWriter ignored = new FileWriter(probe, true)) {
                if (probe.length() == 0) probe.delete();
                return dir;
            }
            catch (IOException ignored) {
            }
        }
        return getAppPrivateForensicsDir(context);
    }

    private static List<ForensicSink> getForensicSinks(Context context) {
        ArrayList<ForensicSink> sinks = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        addForensicSink(sinks, seen, SINK_APP_EXTERNAL, getAppExternalForensicsDir(context));
        addForensicSink(sinks, seen, SINK_EXTERNAL, getExternalForensicsDir(context));
        addForensicSink(sinks, seen, SINK_APP_PRIVATE, getAppPrivateForensicsDir(context));
        return sinks;
    }

    private static void addForensicSink(ArrayList<ForensicSink> sinks, LinkedHashSet<String> seen, String sinkId, File dir) {
        if (dir == null) return;
        String path = dir.getAbsolutePath();
        if (!seen.add(path)) return;
        sinks.add(new ForensicSink(sinkId, dir));
    }

    private static File getCurrentLogFile(File dir) {
        String fileName = "forensics_" + DateFormat.format("yyyy-MM-dd", new Date()) + ".jsonl";
        return new File(dir, fileName);
    }

    private static File getLatestJsonlInDir(File dir) {
        File[] files = dir.listFiles((d, name) -> name.endsWith(".jsonl"));
        if (files == null || files.length == 0) return null;
        File latest = files[0];
        for (File file : files) {
            if (file.lastModified() > latest.lastModified()) latest = file;
        }
        return latest;
    }

    private static void appendForensicLine(Context context, String line) {
        int writes = 0;
        IOException firstError = null;
        for (ForensicSink sink : getForensicSinks(context)) {
            try {
                appendLineWithSink(context, getCurrentLogFile(sink.dir), line, sink.sinkId, firstError);
                writes++;
            }
            catch (IOException e) {
                if (firstError == null) firstError = e;
                Log.w(TAG, "Forensic sink unavailable: " + sink.sinkId + " " + sink.dir.getAbsolutePath(), e);
            }
        }
        if (writes == 0) {
            Log.e(TAG, "Failed writing forensic jsonl to all sinks", firstError);
        }
    }

    private static void writeFatalCrashSync(Context context, Thread thread, Throwable error, String stage) {
        if (context == null || error == null) return;

        String stackTrace = stackTraceString(error);
        Throwable rootCause = deepestCause(error);
        JSONObject obj = new JSONObject();
        try {
            obj.put("ts", TS_FORMAT.get().format(new Date()));
            obj.put("event_id", "APP_FATAL_CRASH");
            obj.put("severity", "error");
            obj.put("trace_id", JSONObject.NULL);
            obj.put("stage", stage == null ? "uncaught_exception" : stage);
            obj.put("message", String.valueOf(error.getMessage()));
            obj.put("error_class", error.getClass().getName());
            obj.put("thread_name", thread != null ? thread.getName() : "");
            obj.put("thread_id", thread != null ? thread.getId() : -1);
            obj.put("root_error_class", rootCause.getClass().getName());
            obj.put("root_error_detail", String.valueOf(rootCause.getMessage()));
            obj.put("stacktrace_hash", sha256Hex(stackTrace));
            obj.put("stacktrace_line_count", stackTraceLineCount(stackTrace));
            obj.put("stacktrace_head", stackTraceHead(stackTrace, 14));
        }
        catch (JSONException ignored) {
            // Fatal crash emission is best-effort; keep the partial JSON payload intact.
        }

        String line = obj.toString();
        logcat("error", line);
        synchronized (FILE_LOCK) {
            appendForensicLine(context, line);
            writeCrashTextFile(context, thread, error);
        }
    }

    private static Throwable deepestCause(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static String stackTraceString(Throwable error) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        error.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    private static int stackTraceLineCount(String stackTrace) {
        if (stackTrace == null || stackTrace.isEmpty()) return 0;
        int count = 1;
        for (int i = 0; i < stackTrace.length(); i++) {
            if (stackTrace.charAt(i) == '\n') count++;
        }
        return count;
    }

    private static String stackTraceHead(String stackTrace, int lineLimit) {
        if (stackTrace == null || stackTrace.isEmpty() || lineLimit <= 0) return "";
        String[] lines = stackTrace.split("\\r?\\n");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length && i < lineLimit; i++) {
            if (builder.length() > 0) builder.append('\n');
            builder.append(lines[i]);
        }
        return builder.toString();
    }

    private static void writeCrashTextFile(Context context, Thread thread, Throwable error) {
        File out = WinlatorLogUtils.createTimestampedLogFile(context, "fatal_crash");
        File dir = out.getParentFile();
        if (dir == null || (!dir.exists() && !dir.mkdirs() && !dir.exists())) {
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(out, false))) {
            writer.write("thread=");
            writer.write(thread != null ? thread.getName() : "");
            writer.newLine();
            writer.write("error_class=");
            writer.write(error.getClass().getName());
            writer.newLine();
            writer.write("error_message=");
            writer.write(String.valueOf(error.getMessage()));
            writer.newLine();
            writer.newLine();

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            error.printStackTrace(pw);
            pw.flush();
            writer.write(sw.toString());
        } catch (IOException e) {
            Log.e(TAG, "Failed writing fatal crash text file", e);
        }
    }

    private static void appendLineWithSink(Context context, File out, String line, String sinkId, IOException switchReason) throws IOException {
        File dir = out.getParentFile();
        if (dir == null || (!dir.exists() && !dir.mkdirs() && !dir.exists())) {
            throw new IOException("Unable to create forensic dir: " + (dir == null ? "<null>" : dir.getAbsolutePath()));
        }

        String sinkSwitchLine = null;
        String sinkKey = sinkId + "|" + out.getAbsolutePath();
        if (!activeFileSinkKeys.contains(sinkKey)) {
            sinkSwitchLine = buildSinkSwitchLine(context, out, sinkId, switchReason);
            activeFileSinkKeys.add(sinkKey);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(out, true))) {
            if (sinkSwitchLine != null) {
                writer.write(sinkSwitchLine);
                writer.newLine();
            }
            writer.write(line);
            writer.newLine();
        }
    }

    private static String buildSinkSwitchLine(Context context, File out, String sinkId, IOException switchReason) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("ts", TS_FORMAT.get().format(new Date()));
            obj.put("event_id", "FORENSIC_SINK_SWITCH");
            obj.put("severity", "info");
            obj.put("trace_id", JSONObject.NULL);
            obj.put("stage", "forensics_file_sink");
            obj.put("message", "Forensic file sink active: " + describeSink(sinkId));
            obj.put("sink_id", sinkId);
            obj.put("sink_path", out.getAbsolutePath());
            obj.put("package_name", context != null ? context.getPackageName() : "");
            if (switchReason != null) {
                obj.put("fallback_reason", String.valueOf(switchReason.getMessage()));
                obj.put("fallback_error_class", switchReason.getClass().getName());
            }
        }
        catch (JSONException e) {
            return null;
        }

        Log.i(TAG, "Forensic file sink active (" + sinkId + "): " + out.getAbsolutePath());
        return obj.toString();
    }

    private static String describeSink(String sinkId) {
        if (SINK_APP_EXTERNAL.equals(sinkId)) return "app-specific external storage";
        if (SINK_APP_PRIVATE.equals(sinkId)) return "app-private storage";
        return "public external storage";
    }

    private static String buildExportSourceLine(File file) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("ts", TS_FORMAT.get().format(new Date()));
            obj.put("event_id", "FORENSIC_EXPORT_SOURCE");
            obj.put("severity", "info");
            obj.put("trace_id", JSONObject.NULL);
            obj.put("stage", "forensics_export");
            obj.put("message", "forensic_export_source");
            obj.put("source_path", file.getAbsolutePath());
            obj.put("source_size", file.length());
            obj.put("source_last_modified", file.lastModified());
        }
        catch (JSONException ignored) {
        }
        return obj.toString();
    }

    private static String buildExportReadErrorLine(File file, IOException error) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("ts", TS_FORMAT.get().format(new Date()));
            obj.put("event_id", "FORENSIC_EXPORT_SOURCE_READ_FAILED");
            obj.put("severity", "warn");
            obj.put("trace_id", JSONObject.NULL);
            obj.put("stage", "forensics_export");
            obj.put("message", "forensic_export_source_read_failed");
            obj.put("source_path", file != null ? file.getAbsolutePath() : "");
            obj.put("error_class", error.getClass().getName());
            obj.put("error_detail", String.valueOf(error.getMessage()));
        }
        catch (JSONException ignored) {
        }
        return obj.toString();
    }

    private static final class ForensicSink {
        final String sinkId;
        final File dir;

        ForensicSink(String sinkId, File dir) {
            this.sinkId = sinkId;
            this.dir = dir;
        }
    }
}
