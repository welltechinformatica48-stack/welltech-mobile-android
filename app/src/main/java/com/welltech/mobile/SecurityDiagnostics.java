package com.welltech.mobile;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

public final class SecurityDiagnostics {
    public static class Snapshot {
        public boolean developerOptionsEnabled;
        public boolean adbEnabled;
        public String securityPatch;
        public boolean vpnActive;
        public boolean proxyConfigured;
        public String privateDnsMode;
        public String riskLevel;
        public String[] findings;
    }

    private SecurityDiagnostics() {}

    public static Snapshot collect(Context context) {
        DeviceDiagnostics.Snapshot d = DeviceDiagnostics.collect(context);
        RuntimeDiagnostics.Snapshot r = RuntimeDiagnostics.collect(context, d);
        Snapshot s = new Snapshot();

        try {
            s.developerOptionsEnabled =
                    Settings.Global.getInt(context.getContentResolver(),
                            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1;
        } catch (Throwable ignored) {}

        try {
            s.adbEnabled =
                    Settings.Global.getInt(context.getContentResolver(),
                            Settings.Global.ADB_ENABLED, 0) == 1;
        } catch (Throwable ignored) {}

        s.securityPatch = d.securityPatch;
        s.vpnActive = r.vpnActive;
        s.proxyConfigured = r.proxyConfigured;
        s.privateDnsMode = r.privateDnsMode;

        java.util.ArrayList<String> findings = new java.util.ArrayList<>();
        if (r.vpnActive) findings.add("vpn_active");
        if (r.proxyConfigured) findings.add("proxy_configured");
        if (d.batteryTempC >= 45f) findings.add("battery_temperature_high");
        if (r.storageFreePercent >= 0 && r.storageFreePercent <= 5) findings.add("storage_critically_low");
        if (r.ramAvailablePercent >= 0 && r.ramAvailablePercent <= 8) findings.add("memory_pressure_high");

        s.riskLevel = RuntimeDiagnostics.riskLevel(r, d);
        s.findings = findings.toArray(new String[0]);
        return s;
    }
}
