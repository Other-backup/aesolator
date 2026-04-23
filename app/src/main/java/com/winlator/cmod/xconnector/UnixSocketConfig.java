package com.winlator.cmod.xconnector;

import com.winlator.cmod.core.FileUtils;

import java.io.File;

public class UnixSocketConfig {
    public static final String SYSVSHM_SERVER_PATH = "/tmp/.sysvshm/SM0";
    public static final String ALSA_SERVER_PATH = "/tmp/.sound/AS0";
    public static final String PULSE_SERVER_PATH = "/tmp/.sound/PS0";
    public static final String XSERVER_PATH = "/tmp/.X11-unix/X0";
    public static final String VIRGL_SERVER_PATH = "/tmp/.virgl/V0";
    public static final String VORTEK_SERVER_PATH = "/tmp/.vortek/V0";
    public static final String STEAM_PIPE_PATH = "/tmp/.steam/steam_pipe";
    public final String path;

    private UnixSocketConfig(String path) {
        this.path = path;
    }

    public static UnixSocketConfig createSocket(String rootPath, String relativePath) {
        String normalizedRelativePath = normalizeSocketRelativePath(relativePath);
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

    private static String normalizeSocketRelativePath(String path) {
        if (path == null) return "";
        String normalized = path.trim().replace('\\', '/');
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        return normalized;
    }

    private static void ensureCompatSocketLink(File rootedDir, File rootedSocketFile, String originalPath) {
        if (rootedDir == null || rootedSocketFile == null || originalPath == null || originalPath.isEmpty()) return;
        if (originalPath.startsWith("/tmp/")) return;

        File compatDir = new File(FileUtils.getDirname(originalPath));
        if (compatDir.getAbsolutePath().equals(rootedDir.getAbsolutePath())) return;

        File compatParent = compatDir.getParentFile();
        if (compatParent != null && !compatParent.exists()) {
            compatParent.mkdirs();
        }

        if (!compatDir.exists()) {
            FileUtils.symlink(rootedDir.getAbsolutePath(), compatDir.getAbsolutePath());
            return;
        }

        if (!compatDir.isDirectory()) return;

        File compatSocketFile = new File(compatDir, rootedSocketFile.getName());
        if (!compatSocketFile.exists()) {
            FileUtils.symlink(rootedSocketFile.getAbsolutePath(), compatSocketFile.getAbsolutePath());
        }
    }
}
