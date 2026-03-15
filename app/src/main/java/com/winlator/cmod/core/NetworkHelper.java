package com.winlator.cmod.core;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.system.OsConstants;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class NetworkHelper {
    private final ConnectivityManager connectivityManager;
    private final WifiManager wifiManager;

    public static class IFAddress {
        public String name = "eth0";
        public int flags = 0;
        public int family = OsConstants.AF_INET;
        public int scopeId = 0;
        public String address = "0";
        public String netmask = "0";

        @Override
        public String toString() {
            return name + "," + flags + "," + family + "," + scopeId + "," + address + "," + netmask;
        }
    }

    public NetworkHelper(Context context) {
        connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
    }

    public boolean isConnected() {
        NetworkInfo networkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
        if (networkInfo == null) return false;
        int type = networkInfo.getType();
        return networkInfo.isAvailable() && networkInfo.isConnectedOrConnecting()
                && (type == ConnectivityManager.TYPE_WIFI
                || type == ConnectivityManager.TYPE_ETHERNET
                || type == ConnectivityManager.TYPE_MOBILE);
    }

    public String getIPv4Address() {
        if (!isConnected() || connectivityManager == null) return null;
        Network activeNetwork = connectivityManager.getActiveNetwork();
        LinkProperties linkProperties = connectivityManager.getLinkProperties(activeNetwork);
        if (linkProperties == null) return null;
        for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
            InetAddress address = linkAddress.getAddress();
            if (address instanceof Inet4Address) {
                return address.getHostAddress();
            }
        }
        return null;
    }

    public List<IFAddress> getIFAddresses() {
        ArrayList<IFAddress> result = new ArrayList<>();
        if (connectivityManager == null) return result;
        Network activeNetwork = connectivityManager.getActiveNetwork();
        LinkProperties linkProperties = connectivityManager.getLinkProperties(activeNetwork);
        if (activeNetwork == null || linkProperties == null) return result;
        for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
            InetAddress address = linkAddress.getAddress();
            if (!(address instanceof Inet4Address) && !(address instanceof Inet6Address)) continue;
            IFAddress ifAddress = new IFAddress();
            ifAddress.address = address.getHostAddress();
            ifAddress.netmask = formatNetmask(linkAddress.getPrefixLength());
            ifAddress.flags = OsConstants.IFF_UP | OsConstants.IFF_RUNNING;
            if (address instanceof Inet6Address) {
                ifAddress.family = OsConstants.AF_INET6;
                ifAddress.scopeId = ((Inet6Address) address).getScopeId();
            }
            result.add(ifAddress);
        }
        return result;
    }

    public int getIpAddress() {
        return wifiManager != null ? wifiManager.getConnectionInfo().getIpAddress() : 0;
    }

    public int getNetmask() {
        if (wifiManager == null) return 0;

        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
        if (dhcpInfo == null) return 0;

        int netmask = Integer.bitCount(dhcpInfo.netmask);
        if (dhcpInfo.netmask < 8 || dhcpInfo.netmask > 32) {
            try {
                InetAddress inetAddress = InetAddress.getByName(formatIpAddress(getIpAddress()));
                NetworkInterface networkInterface = NetworkInterface.getByInetAddress(inetAddress);
                if (networkInterface != null) {
                    for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                        if (inetAddress != null && inetAddress.equals(address.getAddress())) {
                            netmask = address.getNetworkPrefixLength();
                            break;
                        }
                    }
                }
            }
            catch (SocketException | UnknownHostException ignored) {}
        }

        return netmask;
    }

    public int getGateway() {
        if (wifiManager == null) return 0;
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
        return dhcpInfo != null ? dhcpInfo.gateway : 0;
    }

    public static String formatIpAddress(int ipAddress) {
        return (ipAddress & 255)+"."+((ipAddress >> 8) & 255)+"."+((ipAddress >> 16) & 255)+"."+((ipAddress >> 24) & 255);
    }

    public static String formatNetmask(int netmask) {
        switch (netmask) {
            case 8:
                return "255.0.0.0";
            case 16:
                return "255.255.0.0";
            case 24:
                return "255.255.255.0";
            case 32:
                return "255.255.255.255";
            case 64:
                return "ffff:ffff:ffff:ffff::";
            default:
                return "0.0.0.0";
        }
    }
}
