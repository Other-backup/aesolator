package com.winlator.cmod.core;

import android.content.Context;
import android.util.Log;

import com.winlator.cmod.container.Container;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public abstract class WineStartMenuCreator {
    private static final String LEGACY_PREFIX_PACK_SHORTCUT = "Prefix Pack Loader.lnk";

    private static int parseShowCommand(String value) {
        if (value.equals("SW_SHOWMAXIMIZED")) {
            return MSLink.SW_SHOWMAXIMIZED;
        }
        else if (value.equals("SW_SHOWMINNOACTIVE")) {
            return MSLink.SW_SHOWMINNOACTIVE;
        }
        else return MSLink.SW_SHOWNORMAL;
    }

    private static boolean shouldCreateDesktopShortcut(JSONObject item) {
        return item.optBoolean("desktopShortcut", false);
    }

    private static void createLink(JSONObject item, File currentDir) throws JSONException {
        File outputFile = new File(currentDir, item.getString("name") + ".lnk");
        MSLink.Options options = new MSLink.Options();
        options.targetPath = item.getString("path");
        options.cmdArgs = item.optString("cmdArgs");
        options.iconLocation = item.optString("iconLocation", options.targetPath);
        options.iconIndex = item.optInt("iconIndex", 0);
        if (item.has("showCommand")) options.showCommand = parseShowCommand(item.getString("showCommand"));
        MSLink.createFile(options, outputFile);
    }

    private static void createMenuEntry(JSONObject item, File currentDir, File desktopDir) throws JSONException {
        if (item.has("children")) {
            currentDir = new File(currentDir, item.getString("name"));
            currentDir.mkdirs();

            JSONArray children = item.getJSONArray("children");
            for (int i = 0; i < children.length(); i++) createMenuEntry(children.getJSONObject(i), currentDir, desktopDir);
        }
        else {
            createLink(item, currentDir);
            if (desktopDir != null && shouldCreateDesktopShortcut(item)) {
                desktopDir.mkdirs();
                createLink(item, desktopDir);
            }
        }
    }

    private static void removeMenuEntry(JSONObject item, File currentDir, File desktopDir) throws JSONException {
        if (item.has("children")) {
            currentDir = new File(currentDir, item.getString("name"));

            JSONArray children = item.getJSONArray("children");
            for (int i = 0; i < children.length(); i++) removeMenuEntry(children.getJSONObject(i), currentDir, desktopDir);

            if (FileUtils.isEmpty(currentDir)) currentDir.delete();
        }
        else {
            (new File(currentDir, item.getString("name") + ".lnk")).delete();
            if (desktopDir != null && shouldCreateDesktopShortcut(item)) {
                (new File(desktopDir, item.getString("name") + ".lnk")).delete();
            }
        }
    }

    private static void removeOldMenu(File containerStartMenuFile, File startMenuDir, File desktopDir) throws JSONException {
        if (!containerStartMenuFile.isFile()) return;
        JSONArray data = new JSONArray(FileUtils.readString(containerStartMenuFile));
        for (int i = 0; i < data.length(); i++) removeMenuEntry(data.getJSONObject(i), startMenuDir, desktopDir);
    }

    private static void removeLegacyPrefixPackArtifacts(File startMenuDir, File desktopDir) {
        if (desktopDir != null) {
            new File(desktopDir, LEGACY_PREFIX_PACK_SHORTCUT).delete();
        }
        if (startMenuDir != null) {
            File legacyFolder = new File(new File(startMenuDir, "System Tools"), "Ae.solator Prefix Pack");
            if (legacyFolder.exists()) FileUtils.delete(legacyFolder);
        }
    }

    public static void create(Context context, Container container) {
        try {
            File startMenuDir = container.getStartMenuDir();
            File desktopDir = container.getDesktopDir();
            File containerStartMenuFile = new File(container.getRootDir(), ".startmenu");
            removeLegacyPrefixPackArtifacts(startMenuDir, desktopDir);
            removeOldMenu(containerStartMenuFile, startMenuDir, desktopDir);

            JSONArray data = new JSONArray(FileUtils.readString(context, "wine_startmenu.json"));
            FileUtils.writeString(containerStartMenuFile, data.toString());
            for (int i = 0; i < data.length(); i++) createMenuEntry(data.getJSONObject(i), startMenuDir, desktopDir);
        }
        catch (JSONException e) {
            Log.w("WineStartMenuCreator", "Failed to materialize start menu for container " + container.getName(), e);
        }
    }
}
