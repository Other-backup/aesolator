package com.winlator.cmod.xenvironment.components;

import android.util.Log;

import com.winlator.cmod.steampipeserver.SteamPipeServer;
import com.winlator.cmod.xenvironment.EnvironmentComponent;

public class SteamClientComponent extends EnvironmentComponent {
    private SteamPipeServer connector;

    @Override
    public void start() {
        Log.d("SteamClientComponent", "Starting...");
        stop();
        connector = new SteamPipeServer();
        connector.start();
    }

    @Override
    public void stop() {
        Log.d("SteamClientComponent", "Stopping...");
        if (connector != null) {
            connector.stop();
            connector = null;
        }
    }
}
