package com.winlator.cmod.core;

import android.util.Log;

import com.winlator.cmod.core.envvars.EnvVars;
import com.winlator.cmod.winhandler.WinHandler;
import com.winlator.cmod.xserver.Window;
import com.winlator.cmod.xserver.XServer;

import java.util.Locale;

public class Win32AppWorkarounds {
    private static final String TAG = "Win32AppWorkarounds";

    private final XServer xServer;
    private volatile short taskAffinityMask;
    private volatile short taskAffinityMaskWoW64;

    private interface DXWrapperConfigWorkaround extends Workaround {
        void setValue(String value, KeyValueSet keyValueSet);
    }

    private interface DXWrapperWorkaround extends Workaround {
        String getValue();
    }

    private interface EnvVarsWorkaround extends Workaround {
        void apply(EnvVars envVars);
    }

    private interface GraphicsDriverWorkaround extends Workaround {
        String getValue();
    }

    private interface ScreenSizeWorkaround extends Workaround {
        String getValue();
    }

    private interface WinComponentsWorkaround extends Workaround {
        void setValue(KeyValueSet keyValueSet);
    }

    private interface WindowWorkaround extends Workaround {
        void apply(Window window);
    }

    private interface Workaround {
    }

    private static class MultiWorkaround implements Workaround {
        private final Workaround[] list;

        private MultiWorkaround(Workaround... list) {
            this.list = list;
        }
    }

    public Win32AppWorkarounds(XServer xServer) {
        this.xServer = xServer;
    }

    public void setTaskAffinityMasks(int taskAffinityMask, int taskAffinityMaskWoW64) {
        this.taskAffinityMask = (short) taskAffinityMask;
        this.taskAffinityMaskWoW64 = (short) taskAffinityMaskWoW64;
    }

    private void applyWorkaround(Workaround workaround) {
        if (workaround instanceof WindowWorkaround) {
            return;
        }
    }

    public void applyStartupWorkarounds(String className) {
        Workaround workaround = getWorkaroundFor(className);
        if (workaround == null) return;

        if (workaround instanceof MultiWorkaround) {
            for (Workaround nested : ((MultiWorkaround) workaround).list) {
                applyWorkaround(nested);
            }
            return;
        }
        applyWorkaround(workaround);
    }

    private void setProcessAffinity(Window window, int processAffinity) {
        int processId = window.getProcessId();
        String className = window.getClassName();
        WinHandler winHandler = xServer.getWinHandler();
        if ("steam.exe".equals(className)) {
            Log.i(TAG, "steam.exe found, skipping affinity override");
            return;
        }
        if (processId > 0) {
            winHandler.setProcessAffinity(processId, processAffinity);
        } else if (!className.isEmpty()) {
            winHandler.setProcessAffinity(window.getClassName(), processAffinity);
        }
    }

    public void applyWindowWorkarounds(Window window) {
        Workaround workaround = getWorkaroundFor(window.getClassName());
        boolean canApplyProcessAffinity = false;
        if (workaround instanceof WindowWorkaround) {
            ((WindowWorkaround) workaround).apply(window);
        } else if (workaround instanceof MultiWorkaround) {
            for (Workaround nested : ((MultiWorkaround) workaround).list) {
                if (nested instanceof WindowWorkaround) {
                    ((WindowWorkaround) nested).apply(window);
                    break;
                }
            }
        }

        int windowGroup = window.getWMHintsValue(Window.WMHints.WINDOW_GROUP);
        if (window.isRenderable() && !window.getClassName().isEmpty() && windowGroup == window.id) {
            canApplyProcessAffinity = true;
        }
        if (canApplyProcessAffinity) {
            int processAffinity = window.isWoW64() ? taskAffinityMaskWoW64 : taskAffinityMask;
            if (processAffinity != 0) {
                setProcessAffinity(window, processAffinity);
            }
        }
    }

    private Workaround getWorkaroundFor(String className) {
        if (className == null) return null;
        switch (className.toLowerCase(Locale.ENGLISH)) {
            default:
                return null;
        }
    }
}
