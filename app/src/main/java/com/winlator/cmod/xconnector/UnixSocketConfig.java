package com.winlator.cmod.xconnector;

import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.ForensicLogger;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class UnixSocketConfig {
    private static final int UNIX_SOCKET_PATH_SAFE_BYTES = 100;
    public static final String SYSVSHM_SERVER_PATH = "/tmp/.sysvshm/SM0";
    public static final String ALSA_SERVER_PATH = "/tmp/.sound/AS0";
    public static final String PULSE_SERVER_PATH = "/tmp/.sound/PS0";
    public static final String XSERVER_PATH = "/tmp/.X11-unix/X0";
    public static final String VIRGL_SERVER_PATH = "/tmp/.virgl/V0";
    public static final String VORTEK_SERVER_PATH = "/tmp/.vortek/V0";
    public static final String STEAM_PIPE_PATH = "/tmp/.steam/steam_pipe";
    public final String path;
    public final String guestPath;
    public final String rootPath;
    public final String relativePath;
    public final boolean abstractNamespace;
    public final boolean relocated;

    private UnixSocketConfig(String path) {
        this(path, false);
    }

    private UnixSocketConfig(String path, boolean abstractNamespace) {
        this(path, abstractNamespace, path, "", "", false);
    }

    private UnixSocketConfig(String path,
                             boolean abstractNamespace,
                             String guestPath,
                             String rootPath,
                             String relativePath,
                             boolean relocated) {
        this.path = path;
        this.guestPath = guestPath;
        this.rootPath = rootPath;
        this.relativePath = relativePath;
        this.abstractNamespace = abstractNamespace;
        this.relocated = relocated;
    }

    public static UnixSocketConfig createSocket(String rootPath, String relativePath) {
        String normalizedRelativePath = normalizeSocketRelativePath(relativePath);
        ensureRootedSocketParents(rootPath, normalizedRelativePath);
        File guestSocketFile = new File(rootPath, normalizedRelativePath);
        File socketFile = guestSocketFile;
        boolean relocated = false;

        if (shouldRelocateSocketPath(rootPath, normalizedRelativePath, socketFile.getPath())) {
            File shortRoot = buildShortSocketRoot(rootPath, normalizedRelativePath);
            ensureRootedSocketParents(shortRoot.getAbsolutePath(), normalizedRelativePath);
            socketFile = new File(shortRoot, normalizedRelativePath);
            relocated = true;
        }

        String dirname = FileUtils.getDirname(normalizedRelativePath);
        if (dirname.lastIndexOf("/") >= 0) {
            File socketDir = new File(socketFile.getParent());
            FileUtils.delete(socketDir);
            socketDir.mkdirs();
            if (relocated) {
                ensureGuestSocketSymlink(guestSocketFile, socketFile);
                logSocketRelocated(rootPath, normalizedRelativePath, guestSocketFile, socketFile);
            }
            else ensureCompatSocketLink(socketDir, socketFile, relativePath);
        }
        else socketFile.delete();

        return new UnixSocketConfig(
                socketFile.getPath(),
                false,
                guestSocketFile.getPath(),
                rootPath,
                normalizedRelativePath,
                relocated
        );
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

    private static boolean isPathTooLongForSockaddr(String path) {
        if (path == null) return false;
        return path.getBytes(StandardCharsets.UTF_8).length > UNIX_SOCKET_PATH_SAFE_BYTES;
    }

    private static boolean shouldRelocateSocketPath(String rootPath, String normalizedRelativePath, String socketPath) {
        if (isPathTooLongForSockaddr(socketPath)) return true;
        if (rootPath == null || normalizedRelativePath == null) return false;

        String normalizedRoot = rootPath.trim().replace('\\', '/');
        if (!normalizedRoot.contains("/files/imagefs-runtime-")) return false;
        return normalizedRelativePath.startsWith("tmp/");
    }

    private static File buildShortSocketRoot(String rootPath, String normalizedRelativePath) {
        String basePath = deriveFilesRoot(rootPath);
        String hash = ForensicLogger.sha256Hex((rootPath != null ? rootPath : "") + "|" + (normalizedRelativePath != null ? normalizedRelativePath : ""));
        String suffix = hash.length() >= 16 ? hash.substring(0, 16) : Long.toHexString(System.nanoTime());
        return new File(basePath, ".sockets/" + suffix);
    }

    private static String deriveFilesRoot(String rootPath) {
        String normalized = rootPath == null || rootPath.trim().isEmpty()
                ? ""
                : rootPath.trim().replace('\\', '/');
        int filesIndex = normalized.indexOf("/files/");
        if (filesIndex >= 0) return normalized.substring(0, filesIndex + "/files".length());
        android.content.Context context = ForensicLogger.getAppContext();
        String appFiles = context != null && context.getFilesDir() != null
                ? context.getFilesDir().getAbsolutePath()
                : "";
        if (!appFiles.isEmpty()) return appFiles;
        return normalized.isEmpty() ? "." : normalized;
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

    private static void ensureGuestSocketSymlink(File guestSocketFile, File hostSocketFile) {
        if (guestSocketFile == null || hostSocketFile == null) return;
        File guestDir = guestSocketFile.getParentFile();
        if (guestDir != null) ensureRealSocketDirectory(guestDir, "guest_socket_parent");

        boolean symlink = Files.isSymbolicLink(guestSocketFile.toPath());
        boolean exists = guestSocketFile.exists();
        if (symlink) {
            String target = FileUtils.readSymlink(guestSocketFile);
            if (hostSocketFile.getAbsolutePath().equals(target)) return;
            guestSocketFile.delete();
        }
        else if (exists) {
            FileUtils.delete(guestSocketFile);
        }
        FileUtils.symlink(hostSocketFile.getAbsolutePath(), guestSocketFile.getAbsolutePath());
    }

    private static void logSocketRelocated(String rootPath, String normalizedRelativePath, File guestSocketFile, File hostSocketFile) {
        ForensicLogger.logEvent(
                ForensicLogger.getAppContext(),
                "info",
                "XCONNECTOR_SOCKET_PATH_RELOCATED",
                null,
                "xconnector",
                "xconnector_socket_path_relocated",
                ForensicLogger.fields(
                        "root_path", rootPath != null ? rootPath : "",
                        "relative_path", normalizedRelativePath != null ? normalizedRelativePath : "",
                        "guest_socket_path", guestSocketFile != null ? guestSocketFile.getAbsolutePath() : "",
                        "host_socket_path", hostSocketFile != null ? hostSocketFile.getAbsolutePath() : "",
                        "guest_path_bytes", guestSocketFile != null ? guestSocketFile.getAbsolutePath().getBytes(StandardCharsets.UTF_8).length : 0,
                        "host_path_bytes", hostSocketFile != null ? hostSocketFile.getAbsolutePath().getBytes(StandardCharsets.UTF_8).length : 0
                )
        );
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
