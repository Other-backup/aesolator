package com.winlator.cmod.xenvironment.components;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.util.Log;

import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.NetworkHelper;
import com.winlator.cmod.xenvironment.EnvironmentComponent;

import java.io.File;
import java.util.List;

public class NetworkInfoUpdateComponent extends EnvironmentComponent {
    private BroadcastReceiver broadcastReceiver;

    @Override
    public void start() {
        Log.d("NetworkInfoUpdateComponent", "Starting...");
        Context context = environment.getContext();
        final NetworkHelper networkHelper = new NetworkHelper(context);
        updateIFAddrsFile(networkHelper.getIFAddresses());
        updateEtcHostsFile(networkHelper.getIPv4Address());
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateIFAddrsFile(networkHelper.getIFAddresses());
                updateEtcHostsFile(networkHelper.getIPv4Address());
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(broadcastReceiver, filter);
    }

    @Override
    public void stop() {
        Log.d("NetworkInfoUpdateComponent", "Stopping...");
        if (broadcastReceiver != null) {
            try {
                environment.getContext().unregisterReceiver(broadcastReceiver);
            } catch (Exception e) {
                Log.e("NetworkInfoUpdateComponent", "Failed to unregister broadcast receiver", e);
            }
            broadcastReceiver = null;
        }
    }

    public void updateIFAddrsFile(List<NetworkHelper.IFAddress> ifAddresses) {
        File file = new File(environment.getImageFs().getTmpDir(), "ifaddrs");
        String content = "";
        if (!ifAddresses.isEmpty()) {
            for (NetworkHelper.IFAddress ifAddress : ifAddresses) {
                content += (!content.isEmpty() ? "\n" : "") + ifAddress.toString();
            }
        } else {
            content = new NetworkHelper.IFAddress().toString();
        }
        FileUtils.writeString(file, content);
    }

    public void updateEtcHostsFile(String ipAddress) {
        String ip = ipAddress != null ? ipAddress : "127.0.0.1";
        File file = new File(environment.getImageFs().getRootDir(), "etc/hosts");
        FileUtils.writeString(file, ip + "\tlocalhost\n");
    }
}
