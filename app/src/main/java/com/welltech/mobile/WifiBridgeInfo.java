package com.welltech.mobile;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.provider.Settings;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

public final class WifiBridgeInfo {
    private WifiBridgeInfo() {}

    public static String localIpv4(Context context) {
        try {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                Network active = cm.getActiveNetwork();
                if (active != null) {
                    NetworkCapabilities caps = cm.getNetworkCapabilities(active);
                    if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        LinkProperties props = cm.getLinkProperties(active);
                        if (props != null) {
                            for (LinkAddress link : props.getLinkAddresses()) {
                                InetAddress address = link.getAddress();
                                if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                                    return address.getHostAddress();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        try {
            for (NetworkInterface nif : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nif.isUp() || nif.isLoopback()) continue;
                for (InetAddress address : Collections.list(nif.getInetAddresses())) {
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Throwable ignored) {}

        return null;
    }

    public static boolean openWirelessDebugging(Context context) {
        try {
            Intent intent = new Intent("android.settings.WIRELESS_DEBUGGING_SETTINGS");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Throwable ignored) {
            try {
                Intent fallback = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(fallback);
                return true;
            } catch (Throwable ignoredAgain) {
                return false;
            }
        }
    }

    public static String desktopHint(Context context) {
        String ip = localIpv4(context);
        if (ip == null) {
            return "Wi-Fi: sem IPv4 detectado";
        }
        return "Wi-Fi IP: " + ip + " • o Desktop deve descobrir as portas ADB via mDNS";
    }
}
