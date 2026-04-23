package com.winlator.cmod.core;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.format.DateFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ForensicIssueComposer {
    private static final String[] RUNTIME_LOG_PREFIXES = {
            "graphics_mesa",
            "wine_loader",
            "box64",
            "fex_runtime",
            "turnip_mesa",
            "vulkan_api_dump",
            "vulkan_loader",
            "dxvk",
            "vkd3d",
            "pulse",
            "alsa"
    };

    private ForensicIssueComposer() {}

    public static IssueBundleResult createIssueBundle(Context context, String issueTitle, String issueReason, Integer containerId) throws IOException {
        return createIssueBundle(context, issueTitle, issueReason, containerId, null, null);
    }

    public static IssueBundleResult createIssueBundle(Context context, String issueTitle, String issueReason,
                                                      Integer containerId, Map<String, String> extraTextFiles,
                                                      JSONObject supplementalMetadata) throws IOException {
        File baseDir = new File(ForensicLogger.getForensicsDir(context), "issue-bundles");
        if (!baseDir.exists() && !baseDir.mkdirs() && !baseDir.exists()) {
            throw new IOException("Unable to create issue bundle root: " + baseDir.getAbsolutePath());
        }

        String stamp = String.valueOf(DateFormat.format("yyyy-MM-dd_HH-mm-ss", new Date()));
        File bundleDir = new File(baseDir, "issue_" + stamp);
        if (!bundleDir.exists() && !bundleDir.mkdirs() && !bundleDir.exists()) {
            throw new IOException("Unable to create issue bundle dir: " + bundleDir.getAbsolutePath());
        }

        ForensicConfig.Snapshot config = ForensicConfig.load(context);
        Map<String, File> includedFiles = new LinkedHashMap<>();

        File latestForensic = ForensicLogger.getLatestLogFile(context);
        if (latestForensic != null && latestForensic.isFile()) {
            File copied = new File(bundleDir, latestForensic.getName());
            FileUtils.copy(latestForensic, copied);
            includedFiles.put("forensic_jsonl", copied);
        }

        File logsDir = WinlatorLogUtils.getLogsDir(context);
        for (String prefix : RUNTIME_LOG_PREFIXES) {
            File candidate = findLatestLogWithPrefix(logsDir, prefix);
            if (candidate == null) continue;
            File copied = new File(bundleDir, candidate.getName());
            FileUtils.copy(candidate, copied);
            includedFiles.put(prefix, copied);
        }

        try {
            JSONObject runtimeSnapshot = ForensicRuntimeSnapshot.capture();
            File runtimeSnapshotFile = new File(bundleDir, "runtime-snapshot.json");
            writeText(runtimeSnapshotFile, runtimeSnapshot.toString(2));
            includedFiles.put("runtime_snapshot", runtimeSnapshotFile);
            ForensicLogger.logEvent(
                    context,
                    "info",
                    "FORENSIC_RUNTIME_SNAPSHOT_CAPTURED",
                    null,
                    "issue_bundle",
                    "Runtime snapshot captured for issue bundle",
                    ForensicLogger.fields(
                            "process_count", runtimeSnapshot.optInt("processCount", -1),
                            "snapshot_contract", runtimeSnapshot.optString("snapshotContract", "")
                    )
            );
        }
        catch (Throwable t) {
            ForensicLogger.warn(
                    context,
                    "FORENSIC_RUNTIME_SNAPSHOT_FAILED",
                    null,
                    "issue_bundle",
                    "Failed to capture runtime snapshot for issue bundle",
                    ForensicLogger.fields("error", String.valueOf(t.getMessage()))
            );
        }

        if (extraTextFiles != null) {
            for (Map.Entry<String, String> entry : extraTextFiles.entrySet()) {
                if (entry.getKey() == null || entry.getKey().trim().isEmpty()) continue;
                File extraFile = new File(bundleDir, entry.getKey().trim());
                writeText(extraFile, entry.getValue());
                includedFiles.put("extra_" + extraFile.getName(), extraFile);
            }
        }

        File metadataFile = new File(bundleDir, "issue-metadata.json");
        writeText(metadataFile, buildMetadataText(context, config, issueTitle, issueReason, containerId, includedFiles, supplementalMetadata));
        includedFiles.put("metadata", metadataFile);

        File contractFile = new File(bundleDir, "capture-contract.txt");
        writeText(contractFile, buildCaptureContract(context, config, bundleDir));
        includedFiles.put("capture_contract", contractFile);

        File markdownFile = new File(bundleDir, "ISSUE.md");
        String markdown = buildIssueMarkdown(context, config, issueTitle, issueReason, containerId, includedFiles);
        writeText(markdownFile, markdown);
        includedFiles.put("issue_markdown", markdownFile);

        File zipFile = zipDirectory(bundleDir, new File(baseDir, bundleDir.getName() + ".zip"));
        return new IssueBundleResult(bundleDir, markdownFile, zipFile, markdown, latestForensic != null ? latestForensic.getAbsolutePath() : "");
    }

    private static JSONObject buildMetadata(Context context, ForensicConfig.Snapshot config,
                                            String issueTitle, String issueReason, Integer containerId,
                                            Map<String, File> includedFiles, JSONObject supplementalMetadata) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("createdAt", DateFormat.format("yyyy-MM-dd HH:mm:ss", new Date()).toString());
            obj.put("issueTitle", issueTitle == null ? "" : issueTitle);
            obj.put("issueReason", issueReason == null ? "" : issueReason);
            obj.put("containerId", containerId == null ? JSONObject.NULL : containerId);
            obj.put("packageName", context.getPackageName());
            obj.put("appVersionName", getVersionName(context));
            obj.put("appVersionCode", AppUtils.getVersionCode(context));
            obj.put("device", Build.MANUFACTURER + " " + Build.MODEL);
            obj.put("androidRelease", Build.VERSION.RELEASE);
            obj.put("sdkInt", Build.VERSION.SDK_INT);
            obj.put("abi", Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "");
            obj.put("runtimeLibc", "bionic");
            obj.put("runtimeBionicOnly", true);
            obj.put("hostArch", System.getProperty("os.arch", ""));
            JSONArray abiList = new JSONArray();
            if (Build.SUPPORTED_ABIS != null) {
                for (String abi : Build.SUPPORTED_ABIS) abiList.put(abi);
            }
            obj.put("supportedAbis", abiList);
            obj.put("runtimeSnapshotIncluded", includedFiles.containsKey("runtime_snapshot"));
            obj.put("runtimeSnapshotContract", ForensicRuntimeSnapshot.SNAPSHOT_CONTRACT);
            obj.put("forensicConfig", ForensicConfig.toJson(context, config));
            obj.put("latestTraceSummary", ForensicLogger.describeLatestTrace(context));

            JSONArray files = new JSONArray();
            for (Map.Entry<String, File> entry : includedFiles.entrySet()) {
                JSONObject row = new JSONObject();
                row.put("key", entry.getKey());
                row.put("name", entry.getValue().getName());
                row.put("size", entry.getValue().length());
                files.put(row);
            }
            obj.put("files", files);
            if (supplementalMetadata != null) {
                obj.put("supplemental", supplementalMetadata);
            }
        }
        catch (JSONException ignored) { /* best-effort path; keep surrounding flow intact. */ }
        return obj;
    }

    private static String buildMetadataText(Context context, ForensicConfig.Snapshot config,
                                            String issueTitle, String issueReason, Integer containerId,
                                            Map<String, File> includedFiles, JSONObject supplementalMetadata) {
        try {
            return buildMetadata(context, config, issueTitle, issueReason, containerId, includedFiles, supplementalMetadata).toString(2);
        }
        catch (JSONException ignored) {
            return buildMetadata(context, config, issueTitle, issueReason, containerId, includedFiles, supplementalMetadata).toString();
        }
    }

    private static String buildCaptureContract(Context context, ForensicConfig.Snapshot config, File bundleDir) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ae.solator forensic capture contract\n");
        sb.append("bundle_dir=").append(bundleDir.getAbsolutePath()).append('\n');
        sb.append("package=").append(context.getPackageName()).append('\n');
        sb.append("runtime_profile=").append(ForensicConfig.buildRuntimeSummary(config)).append('\n');
        sb.append("capture_profile=").append(ForensicConfig.buildCaptureSummary(context, config)).append('\n');
        sb.append("adb_script=").append(
                        ForensicConfig.buildIssueCaptureCommand(context, config.enableRootCapture)
                                .replace(" --bundle-dir <out-dir>", " --bundle-dir " + bundleDir.getAbsolutePath())
                )
                .append('\n');
        sb.append("adb_browse=").append(ForensicConfig.buildIssueBrowseCommand(context)).append('\n');
        sb.append("issue_markdown=ISSUE.md\n");
        return sb.toString();
    }

    private static String buildIssueMarkdown(Context context, ForensicConfig.Snapshot config,
                                             String issueTitle, String issueReason, Integer containerId,
                                             Map<String, File> includedFiles) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Ae.solator forensic issue bundle\n\n");
        if (issueTitle != null && !issueTitle.trim().isEmpty()) {
            sb.append("- title: `").append(issueTitle.trim()).append("`\n");
        }
        if (issueReason != null && !issueReason.trim().isEmpty()) {
            sb.append("- reason: `").append(issueReason.trim()).append("`\n");
        }
        if (containerId != null) {
            sb.append("- container_id: `").append(containerId).append("`\n");
        }
        sb.append("- app: `").append(getVersionName(context)).append("` / `")
                .append(AppUtils.getVersionCode(context)).append("`\n");
        sb.append("- package: `").append(context.getPackageName()).append("`\n");
        sb.append("- device: `").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append("`\n");
        sb.append("- android: `").append(Build.VERSION.RELEASE).append("` (`sdk=").append(Build.VERSION.SDK_INT).append("`)\n");
        sb.append("- latest_trace: `").append(ForensicLogger.describeLatestTrace(context)).append("`\n");
        sb.append("\n## Runtime profile\n");
        sb.append("- ").append(ForensicConfig.buildRuntimeSummary(config)).append('\n');
        sb.append("- ").append(ForensicConfig.buildCaptureSummary(context, config)).append('\n');
        sb.append("- wine_debug_channels: `").append(ForensicConfig.normalizeChannels(config.wineDebugChannels)).append("`\n");
        sb.append("\n## Included files\n");
        for (Map.Entry<String, File> entry : includedFiles.entrySet()) {
            sb.append("- `").append(entry.getKey()).append("`: `")
                    .append(entry.getValue().getName()).append("` (`")
                    .append(entry.getValue().length()).append("` bytes)\n");
        }
        File latestForensic = ForensicLogger.getLatestLogFile(context);
        if (latestForensic != null && latestForensic.isFile()) {
            sb.append("\n## Latest forensic tail\n\n````text\n");
            sb.append(readTail(latestForensic, 12));
            sb.append("\n````\n");
        }
        sb.append("\n## ADB follow-up\n\n````bash\n");
        sb.append(ForensicConfig.buildIssueCaptureCommand(context, config.enableRootCapture)
                .replace(" --bundle-dir <out-dir>", " --bundle-dir <host-out-dir>"));
        sb.append('\n');
        sb.append(ForensicConfig.buildIssueBrowseCommand(context));
        sb.append("\n````\n");
        return sb.toString();
    }

    private static String readTail(File file, int maxLines) {
        ArrayList<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (lines.size() == maxLines) lines.remove(0);
                lines.add(line);
            }
        }
        catch (IOException e) {
            return "tail_read_failed: " + e.getMessage();
        }
        return String.join("\n", lines);
    }

    private static File findLatestLogWithPrefix(File dir, String prefix) {
        if (dir == null || !dir.isDirectory()) return null;
        File[] files = dir.listFiles((candidateDir, name) -> name.startsWith(prefix + "_") && name.endsWith(".txt"));
        if (files == null || files.length == 0) return null;
        File latest = files[0];
        for (File file : files) {
            if (file.lastModified() > latest.lastModified()) latest = file;
        }
        return latest;
    }

    private static void writeText(File file, String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content == null ? "" : content);
        }
    }

    private static File zipDirectory(File sourceDir, File output) throws IOException {
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(output))) {
            zipRecursive(sourceDir, sourceDir, out);
        }
        return output;
    }

    private static void zipRecursive(File root, File current, ZipOutputStream out) throws IOException {
        File[] children = current.listFiles();
        if (children == null) return;
        byte[] buffer = new byte[8192];
        for (File child : children) {
            String rel = root.toPath().relativize(child.toPath()).toString();
            if (child.isDirectory()) {
                zipRecursive(root, child, out);
                continue;
            }
            out.putNextEntry(new ZipEntry(rel));
            try (FileInputStream in = new FileInputStream(child)) {
                int read;
                while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            }
            out.closeEntry();
        }
    }

    private static String getVersionName(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionName == null ? "" : info.versionName;
        }
        catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    public static final class IssueBundleResult {
        public final File bundleDir;
        public final File markdownFile;
        public final File zipFile;
        public final String markdown;
        public final String tracePath;

        public IssueBundleResult(File bundleDir, File markdownFile, File zipFile, String markdown, String tracePath) {
            this.bundleDir = bundleDir;
            this.markdownFile = markdownFile;
            this.zipFile = zipFile;
            this.markdown = markdown;
            this.tracePath = tracePath;
        }
    }
}
