package com.winlator.cmod;

import android.app.Application;

import com.winlator.cmod.core.ForensicLogger;

public class WinlatorApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ForensicLogger.initialize(this);
        ForensicLogger.installCrashHandler(this);
    }
}
