package com.winlator.cmod.xconnector;

import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.ForensicLogger;

import java.io.File;
import java.nio.file.Files;

public class UnixSocketConfig {
    public static final String SYSVSHM_SERVER_PATH = "/tmp/.sysvshm/SM0";
    public static final String ALSA_SERVER_PATH = "/tmp/.sound/AS0";
    public static final String PULSE_SERVER_PATH = "/tmp/.sound/PS0";
    public static final String XSERVER_PATH = "/tmp/.X11-unix/X0";
    public static final String VIRGL_SERVER_PATH = "/tmp/.virgl/V0";
    public static final String VORTEK_SERVER_PATH = "/tmp/.vortek/V0";
    public static final String STEAM_PIPE_PATH = "/tmp/.steam/steam_pipe";
    public final String path;
    public final boolean abstractNamespace;

    private UnixSocketConfig(String path) {
        this(path, false);
    }

    private UnixSocketConfig(String path, boolean abstractNamespace) {
        this.path = path;
        this.abstractNamespace = abstractNamespace;
    }

    public static UnixSocketConfig createSocket(String rootPath, String relativePath) {
        String normalizedRelativePath = normalizeSocketRelativePath(relativePath);
        ensureRootedSocketParents(rootPath, normalizedRelativePath);
        File socketFile = new File(rootPath, normalizedRelativePath);

        String dirname = FileUtils.getDirname(normalizedRelativePath);
        if (dirname.lastIndexOf("/") >= 0) {
            File socketDir = new File(rootPath, dirname);
            FileUtils.delete(socketDir);
            socketDir.mkdirs();
            ensureCompatSocketLink(socketDir, socketFile, relativePath);
        }
        else socketFile.delete();

        return new UnixSocketConfig(socketFile.getPath());
    }

    public static UnixSocketConfig createAbstractSocket(String abstractPath) {
        String normalized = abstractPath == null ? "" : abstractPath.trim().replace('\\', '/');
        while (normalized.startsWith("@")) normalized = normalized.substring(1);
        return new UnixSocketConfig(normalized, true);
    }

    private static String normalizeSocketRelativePath(String path) {
        if (path == null) return "";
        String normalized = path.trim().replace('\\', '/');
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        return normalized;
    }

    private static void ensureRootedSocketParents(String rootPath, String normalizedRelativePath) {
        if (rootPath == null || rootPath.trim().isEmpty()
                || normalizedRelativePath == null || normalizedRelativePath.trim().isEmpty()) {
            return;
        }

        String[] segments = normalizedRelativePath.split("/");
        if (segments.length <= 1) return;

        File current = new File(rootPath);
        for (int i = 0; i < segments.length - 1; i++) {
            String segment = segments[i];
            if (segment == null || segment.isEmpty()) continue;
            current = new File(current, segment);
            ensureRealSocketDirectory(current, i == 0 ? "root_tmp" : "socket_parent");
        }
    }

    private static void ensureRealSocketDirectory(File directory, String role) {
        if (directory == null) return;

        boolean symlink = Files.isSymbolicLink(directory.toPath());
        boolean exists = directory.exists();
        boolean directoryReady = exists && directory.isDirectory() && !symlink;
        if (directoryReady) return;

        String linkTarget = "";
        if (symlink) {
            linkTarget = FileUtils.readSymlink(directory);
            if (!directory.delete()) {
                logSocketParentRepair("SOCKET_PARENT_REPAIR_FAILED", directory, role, linkTarget, "delete_symlink_failed");
                return;
            }
        } else if (exists && !directory.isDirectory()) {
            if (!directory.delete()) {
                logSocketParentRepair("SOCKET_PARENT_REPAIR_FAILED", directory, role, "", "delete_non_directory_failed");
                return;
            }
        }

        if (!directory.exists() && !directory.mkdirs()) {
            logSocketParentRepair("SOCKET_PARENT_REPAIR_FAILED", directory, role, linkTarget, "mkdirs_failed");
            return;
        }

        if (!directory.isDirectory()) {
            logSocketParentRepair("SOCKET_PARENT_REPAIR_FAILED", directory, role, linkTarget, "not_directory_after_repair");
            return;
        }

        if (symlink || !exists) {
            logSocketParentRepair("SOCKET_PARENT_REPAIRED", directory, role, linkTarget, symlink ? "replaced_symlink_with_directory" : "created_missing_directory");
        }
    }

    private static void logSocketParentRepair(String eventId, File directory, String role, String linkTarget, String result) {
        ForensicLogger.logEvent(
                ForensicLogger.getAppContext(),
                eventId.endsWith("_FAILED") ? "warn" : "info",
                eventId,
                null,
                "xconnector",
                "socket_parent_directory_repair",
                ForensicLogger.fields(
                        "path", directory != null ? directory.getPath() : "",
                        "role", role != null ? role : "",
                        "link_target", linkTarget != null ? linkTarget : "",
                        "result", result != null ? result : ""
                )
        );
    }

    private static void ensureCompatSocketLink(File rootedDir, File rootedSocketFile, String originalPath) {
        if (rootedDir == null || rootedSocketFile == null || originalPath == null || originalPath.isEmpty()) return;
        if (originalPath.startsWith("/tmp/")) return;

        File compatDir = new File(FileUtils.getDirname(originalPath));
        if (compatDir.getAbsolutePath().equals(rootedDir.getAbsolutePath())) return;

        File compatParent = compatDir.getParentFile();
        if (!ensureWritableCompatDirectory(compatParent, rootedDir, originalPath, "compat_parent")) return;

        if (!compatDir.exists()) {
            createCompatSymlink(rootedDir.getAbsolutePath(), compatDir.getAbsolutePath(), originalPath, "compat_directory");
            return;
        }

        if (!compatDir.isDirectory()) {
            logCompatSocketLinkSkipped(compatDir, rootedDir, originalPath, "compat_dir_not_directory");
            return;
        }

        if (!compatDir.canWrite()) {
            logCompatSocketLinkSkipped(compatDir, rootedDir, originalPath, "compat_dir_not_writable");
            return;
        }

        File compatSocketFile = new File(compatDir, rootedSocketFile.getName());
        if (!compatSocketFile.exists()) {
            createCompatSymlink(rootedSocketFile.getAbsolutePath(), compatSocketFile.getAbsolutePath(), originalPath, "compat_socket");
        }
    }

    private static boolean ensureWritableCompatDirectory(File directory, File rootedDir, String originalPath, String role) {
        if (directory == null) return true;
        if (!directory.exists() && !directory.mkdirs()) {
            logCompatSocketLinkSkipped(directory, rootedDir, originalPath, role + "_mkdirs_failed");
            return false;
        }
        if (!directory.isDirectory()) {
            logCompatSocketLinkSkipped(directory, rootedDir, originalPath, role + "_not_directory");
            return false;
        }
        if (!directory.canWrite()) {
            logCompatSocketLinkSkipped(directory, rootedDir, originalPath, role + "_not_writable");
            return false;
        }
        return true;
    }

    private static void createCompatSymlink(String target, String link, String originalPath, String role) {
        if (!FileUtils.symlink(target, link)) {
            ForensicLogger.logEvent(
                    ForensicLogger.getAppContext(),
                    "warn",
                    "XCONNECTOR_COMPAT_SOCKET_LINK_FAILED",
                    null,
                    "xconnector",
                    "xconnector_compat_socket_link_failed",
                    ForensicLogger.fields(
                            "role", role != null ? role : "",
                            "original_path", originalPath != null ? originalPath : "",
                            "link", link != null ? link : "",
                            "target", target != null ? target : ""
                    )
            );
        }
    }

    private static void logCompatSocketLinkSkipped(File path, File rootedDir, String originalPath, String result) {
        ForensicLogger.logEvent(
                ForensicLogger.getAppContext(),
                "info",
                "XCONNECTOR_COMPAT_SOCKET_LINK_SKIPPED",
                null,
                "xconnector",
                "xconnector_compat_socket_link_skipped",
                ForensicLogger.fields(
                        "path", path != null ? path.getAbsolutePath() : "",
                        "rooted_dir", rootedDir != null ? rootedDir.getAbsolutePath() : "",
                        "original_path", originalPath != null ? originalPath : "",
                        "result", result != null ? result : ""
                )
        );
    }
}
