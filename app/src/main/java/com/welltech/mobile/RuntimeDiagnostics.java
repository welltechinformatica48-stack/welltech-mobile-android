package com.welltech.mobile;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.ProxyInfo;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

public final class RuntimeDiagnostics {
    public static class Snapshot {
        public String networkTransport;
        public String wifiIpv4;
        public boolean vpnActive;
        public boolean proxyConfigured;
        public String proxyHost;
        public Integer proxyPort;
        public String privateDnsMode;
        public String privateDnsSpecifier;
        public boolean screenInteractive;
        public Integer thermalStatus;
        public String thermalLabel;
        public int ramAvailablePercent;
        public int storageFreePercent;
    }

    private RuntimeDiagnostics() {}

    public static Snapshot collect(Context context, DeviceDiagnostics.Snapshot device) {
        Snapshot s = new Snapshot();
        s.wifiIpv4 = WifiBridgeInfo.localIpv4(context);

        try {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network active = cm == null ? null : cm.getActiveNetwork();
            NetworkCapabilities caps = active == null || cm == null ? null : cm.getNetworkCapabilities(active);
            if (caps != null) {
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) s.networkTransport = "wifi";
                else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) s.networkTransport = "cellular";
                else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) s.networkTransport = "ethernet";
                else s.networkTransport = "other";
                s.vpnActive = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
            } else {
                s.networkTransport = "offline";
                s.vpnActive = false;
            }

            LinkProperties lp = active == null || cm == null ? null : cm.getLinkProperties(active);
            if (lp != null) {
                ProxyInfo proxy = lp.getHttpProxy();
                if (proxy != null && proxy.getHost() != null && !proxy.getHost().trim().isEmpty()) {
                    s.proxyConfigured = true;
                    s.proxyHost = proxy.getHost();
                    s.proxyPort = proxy.getPort();
                }
                if (Build.VERSION.SDK_INT >= 28) {
                    s.privateDnsMode = lp.isPrivateDnsActive() ? "active" : "inactive";
                    s.privateDnsSpecifier = lp.getPrivateDnsServerName();
                }
            }
        } catch (Throwable ignored) {
            s.networkTransport = "unknown";
        }

        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                s.screenInteractive = pm.isInteractive();
                if (Build.VERSION.SDK_INT >= 29) {
                    s.thermalStatus = pm.getCurrentThermalStatus();
                    s.thermalLabel = thermalLabel(s.thermalStatus);
                }
            }
        } catch (Throwable ignored) {}

        s.ramAvailablePercent = device.ramTotal > 0
                ? (int)Math.round(device.ramAvailable * 100d / device.ramTotal) : -1;
        s.storageFreePercent = device.storageTotal > 0
                ? (int)Math.round(device.storageFree * 100d / device.storageTotal) : -1;

        try {
            if (s.privateDnsMode == null) {
                String mode = Settings.Global.getString(context.getContentResolver(), "private_dns_mode");
                String spec = Settings.Global.getString(context.getContentResolver(), "private_dns_specifier");
                s.privateDnsMode = mode;
                s.privateDnsSpecifier = spec;
            }
        } catch (Throwable ignored) {}

        return s;
    }

    public static String riskLevel(Snapshot s, DeviceDiagnostics.Snapshot d) {
        if (d.batteryTempC >= 45f) return "attention";
        if (s.storageFreePercent >= 0 && s.storageFreePercent <= 5) return "attention";
        if (s.ramAvailablePercent >= 0 && s.ramAvailablePercent <= 8) return "attention";
        if (s.proxyConfigured || s.vpnActive) return "review";
        return "normal";
    }

    private static String thermalLabel(Integer status) {
        if (status == null) return "unknown";
        switch (status) {
            case PowerManager.THERMAL_STATUS_NONE: return "normal";
            case PowerManager.THERMAL_STATUS_LIGHT: return "light";
            case PowerManager.THERMAL_STATUS_MODERATE: return "moderate";
            case PowerManager.THERMAL_STATUS_SEVERE: return "severe";
            case PowerManager.THERMAL_STATUS_CRITICAL: return "critical";
            case PowerManager.THERMAL_STATUS_EMERGENCY: return "emergency";
            case PowerManager.THERMAL_STATUS_SHUTDOWN: return "shutdown";
            default: return "unknown";
        }
    }
}
