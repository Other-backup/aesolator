package com.winlator.cmod.core;

import android.content.Context;
import android.view.View;
import android.widget.Spinner;

import com.winlator.cmod.xenvironment.ImageFs;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public abstract class GeneralComponents {

    public enum Type {
        BOX64,
        TURNIP,
        DXVK,
        VKD3D,
        WINED3D,
        SOUNDFONT,
        ADRENOTOOLS_DRIVER;

        public String lowerName() {
            return name().toLowerCase(Locale.ENGLISH);
        }

        public String title() {
            return switch (this) {
                case BOX64 -> "Box64";
                case TURNIP -> "Turnip";
                case DXVK -> "DXVK";
                case VKD3D -> "VKD3D";
                case WINED3D -> "WineD3D";
                case SOUNDFONT -> "SoundFont";
                case ADRENOTOOLS_DRIVER -> "Adrenotools Driver";
            };
        }

        private String assetFolder() {
            return switch (this) {
                case BOX64 -> "box64";
                case TURNIP -> "graphics_driver";
                case DXVK, VKD3D, WINED3D -> "dxwrapper";
                case SOUNDFONT -> "soundfont";
                case ADRENOTOOLS_DRIVER -> "";
            };
        }

        private File getSource(Context context, String identifier) {
            File componentDir = GeneralComponents.getComponentDir(this, context);
            return switch (this) {
                case SOUNDFONT -> new File(componentDir, identifier + ".sf2");
                case ADRENOTOOLS_DRIVER -> new File(componentDir, identifier);
                default -> new File(componentDir, lowerName() + "-" + identifier + ".tzst");
            };
        }

        public File getDestination(Context context) {
            ImageFs imageFs = ImageFs.find(context);
            File rootDir = imageFs.getRootDir();
            return switch (this) {
                case DXVK, VKD3D, WINED3D -> new File(imageFs.getWinePrefixDir(), "drive_c/windows");
                case SOUNDFONT -> {
                    File destination = new File(context.getCacheDir(), "soundfont");
                    if (!destination.isDirectory()) destination.mkdirs();
                    yield destination;
                }
                default -> rootDir;
            };
        }

        public int getInstallModes() {
            if (this == SOUNDFONT || this == ADRENOTOOLS_DRIVER) return 2;
            return 1;
        }

        public boolean isVersioned() {
            return this == BOX64 || this == TURNIP || this == DXVK || this == VKD3D || this == WINED3D;
        }
    }

    public static ArrayList<String> getBuiltinComponentNames(Type type) {
        String[] items = new String[0];
        switch (type) {
            case BOX64 -> items = new String[]{"0.3.4", "0.3.6"};
            case TURNIP -> items = new String[]{"25.1.0"};
            case DXVK -> items = new String[]{"1.10.3", "2.4.1"};
            case VKD3D -> items = new String[]{"2.13"};
            case WINED3D -> items = new String[]{"9.2"};
            case SOUNDFONT -> items = new String[]{"SONiVOX-EAS-GM-Wavetable"};
            case ADRENOTOOLS_DRIVER -> items = new String[]{"System"};
        }
        return new ArrayList<>(Arrays.asList(items));
    }

    public static File getComponentDir(Type type, Context context) {
        File file = new File(context.getFilesDir(), "/installed_components/" + type.lowerName());
        if (!file.isDirectory()) file.mkdirs();
        return file;
    }

    public static void initViews(Type type, View toolbox, Spinner spinner, String selectedValue, String defaultValue) {
        if (spinner == null) return;
        Context context = spinner.getContext();
        ArrayList<String> items = getBuiltinComponentNames(type);
        File componentDir = getComponentDir(type, context);
        File[] installedComponents = componentDir.listFiles();
        if (installedComponents != null) {
            for (File component : installedComponents) {
                String name = component.getName();
                if (!name.isEmpty() && !items.contains(name)) items.add(name);
            }
        }

        String selected = selectedValue == null ? "" : selectedValue.trim();
        String fallback = defaultValue == null ? "" : defaultValue.trim();
        if (items.isEmpty() && !fallback.isEmpty()) items.add(fallback);
        if (!selected.isEmpty() && !items.contains(selected)) items.add(selected);

        spinner.setAdapter(SpinnerAdapters.create(context, items));
        if (!selected.isEmpty()) {
            AppUtils.setSpinnerSelectionFromValue(spinner, selected);
        } else if (!fallback.isEmpty()) {
            AppUtils.setSpinnerSelectionFromValue(spinner, fallback);
        }
        if (toolbox != null) toolbox.setVisibility(items.size() > 1 ? View.VISIBLE : View.GONE);
    }

    public static boolean isBuiltinComponent(Type type, String identifier) {
        for (String builtin : getBuiltinComponentNames(type)) {
            if (builtin.equalsIgnoreCase(identifier)) return true;
        }
        return false;
    }

    public static String getDefinitivePath(Type type, Context context, String identifier) {
        if (identifier == null || identifier.isEmpty()) return null;

        if (type == Type.SOUNDFONT && isBuiltinComponent(type, identifier)) {
            File destination = type.getDestination(context);
            FileUtils.clear(destination);
            String filename = identifier + ".sf2";
            File destinationFile = new File(destination, filename);
            FileUtils.copy(context, type.assetFolder() + "/" + filename, destinationFile);
            return destinationFile.getPath();
        }

        if (type == Type.ADRENOTOOLS_DRIVER) {
            if (isBuiltinComponent(type, identifier)) return null;
            File source = type.getSource(context, identifier);
            File[] manifestFiles = source.listFiles((file, name) -> name.endsWith(".json"));
            if (manifestFiles != null && manifestFiles.length > 0) {
                try {
                    JSONObject manifest = new JSONObject(FileUtils.readString(manifestFiles[0]));
                    String libraryName = manifest.optString("libraryName", "");
                    File libraryFile = new File(source, libraryName);
                    if (libraryFile.isFile()) return libraryFile.getPath();
                    return null;
                }
                catch (JSONException e) {
                    return null;
                }
            }
        }

        return type.getSource(context, identifier).getPath();
    }
}
