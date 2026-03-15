package com.winlator.cmod.xenvironment.components;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.winlator.cmod.xenvironment.EnvironmentComponent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WineRequestComponent extends EnvironmentComponent {
    abstract static class RequestCodes {
        static final int OPEN_URL = 1;
        static final int GET_WINE_CLIPBOARD = 2;
        static final int SET_WINE_CLIPBOARD = 3;
    }

    private ServerSocket serverSocket;
    private volatile boolean isRunning = false;
    private ExecutorService executor;

    @Override
    public void start() {
        isRunning = true;
        executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                serverSocket = createServerSocket();
                while (isRunning) {
                    try (Socket socket = serverSocket.accept();
                         DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                         DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream())) {
                        int requestCode = inputStream.readInt();
                        handleRequest(inputStream, outputStream, requestCode);
                    }
                }
            } catch (IOException e) {
                if (isRunning) {
                    Log.e("WineRequestComponent", "Wine request server stopped unexpectedly", e);
                }
            }
        });
    }

    @Override
    public void stop() {
        isRunning = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }

    private ServerSocket createServerSocket() throws IOException {
        try {
            return new ServerSocket(20000, 50, InetAddress.getByName("127.0.0.1"));
        } catch (IOException first) {
            try {
                return new ServerSocket(20000, 50, InetAddress.getLocalHost());
            } catch (IOException second) {
                return new ServerSocket(20000);
            }
        }
    }

    private void handleRequest(DataInputStream inputStream, DataOutputStream outputStream, int requestCode) throws IOException {
        switch (requestCode) {
            case RequestCodes.OPEN_URL:
                openURL(inputStream);
                break;
            case RequestCodes.GET_WINE_CLIPBOARD:
                importWineClipboard(inputStream);
                break;
            case RequestCodes.SET_WINE_CLIPBOARD:
                exportAndroidClipboard(outputStream);
                break;
            default:
                Log.w("WineRequestComponent", "Unknown request code: " + requestCode);
                break;
        }
    }

    private void openURL(DataInputStream inputStream) throws IOException {
        Context context = environment.getContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean openWithAndroidBrowser = preferences.getBoolean("open_with_android_browser", false);

        int messageLength = inputStream.readInt();
        byte[] data = new byte[messageLength];
        inputStream.readFully(data);
        String url = new String(data, StandardCharsets.UTF_8);

        if (!openWithAndroidBrowser) {
            Log.d("WineRequestComponent", "OPEN_URL ignored because browser bridge is disabled");
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private void importWineClipboard(DataInputStream inputStream) throws IOException {
        Context context = environment.getContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (!preferences.getBoolean("share_android_clipboard", false)) {
            Log.d("WineRequestComponent", "GET_WINE_CLIPBOARD ignored because clipboard bridge is disabled");
            return;
        }

        int format = inputStream.readInt();
        int size = inputStream.readInt();
        byte[] data = new byte[size];
        inputStream.readFully(data);
        if (format == 13) {
            String clipboardData = new String(data, StandardCharsets.UTF_16LE).replace("\0", "");
            ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboardManager != null) {
                clipboardManager.setPrimaryClip(ClipData.newPlainText("", clipboardData));
            }
        }
    }

    private void exportAndroidClipboard(DataOutputStream outputStream) throws IOException {
        Context context = environment.getContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (!preferences.getBoolean("share_android_clipboard", false)) {
            outputStream.writeInt(13);
            outputStream.writeInt(0);
            return;
        }

        String clipText = "";
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager != null && clipboardManager.getPrimaryClip() != null && clipboardManager.getPrimaryClip().getItemCount() > 0) {
            CharSequence text = clipboardManager.getPrimaryClip().getItemAt(0).getText();
            if (text != null) clipText = text.toString();
        }

        byte[] data = (clipText + "\0").getBytes(StandardCharsets.UTF_16LE);
        outputStream.writeInt(13);
        outputStream.writeInt(data.length);
        outputStream.write(data);
    }
}
