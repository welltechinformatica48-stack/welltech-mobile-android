package com.welltech.mobile;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.Locale;

public final class DeviceDiagnostics {

    public static class Snapshot {
        public String manufacturer, brand, model, device, product, board, hardware;
        public String androidVersion, securityPatch, buildId, buildFingerprint, abis, cpuName;
        public int sdk, cpuCores, batteryLevel, batteryVoltageMv;
        public Integer batteryCurrentNowMicroA, batteryCurrentAverageMicroA, batteryChargeCounterMicroAh;
        public Long batteryEnergyCounterNanoWh;
        public Integer batteryCycleCount;
        public long ramTotal, ramAvailable, storageTotal, storageFree, uptimeMs;
        public float batteryTempC;
        public String batteryStatus, batteryHealth, batteryPowerSource;
    }

    private DeviceDiagnostics() {}

    public static Snapshot collect(Context context) {
        Snapshot s = new Snapshot();
        s.manufacturer = safe(Build.MANUFACTURER);
        s.brand = safe(Build.BRAND);
        s.model = safe(Build.MODEL);
        s.device = safe(Build.DEVICE);
        s.product = safe(Build.PRODUCT);
        s.board = safe(Build.BOARD);
        s.hardware = safe(Build.HARDWARE);
        s.androidVersion = safe(Build.VERSION.RELEASE);
        s.sdk = Build.VERSION.SDK_INT;
        s.securityPatch = Build.VERSION.SDK_INT >= 23 ? safe(Build.VERSION.SECURITY_PATCH) : "N/D";
        s.buildId = safe(Build.ID);
        s.buildFingerprint = safe(Build.FINGERPRINT);
        s.abis = Build.SUPPORTED_ABIS == null ? "N/D" : String.join(", ", Build.SUPPORTED_ABIS);
        s.cpuCores = Runtime.getRuntime().availableProcessors();
        s.cpuName = readCpuName();

        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        if (am != null) {
            am.getMemoryInfo(mi);
            s.ramTotal = mi.totalMem;
            s.ramAvailable = mi.availMem;
        }

        File data = Environment.getDataDirectory();
        StatFs stat = new StatFs(data.getAbsolutePath());
        s.storageTotal = stat.getTotalBytes();
        s.storageFree = stat.getAvailableBytes();

        Intent battery = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery != null) {
            int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            s.batteryLevel = level >= 0 && scale > 0 ? Math.round(level * 100f / scale) : -1;
            s.batteryTempC = battery.hasExtra(BatteryManager.EXTRA_TEMPERATURE)
                    ? battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f : Float.NaN;
            s.batteryVoltageMv = battery.hasExtra(BatteryManager.EXTRA_VOLTAGE)
                    ? battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) : -1;
            s.batteryStatus = batteryStatus(battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1));
            s.batteryHealth = batteryHealth(battery.getIntExtra(BatteryManager.EXTRA_HEALTH, -1));
            s.batteryPowerSource = batteryPowerSource(battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0));
            if (Build.VERSION.SDK_INT >= 34 && battery.hasExtra(BatteryManager.EXTRA_CYCLE_COUNT)) {
                int cycles = battery.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1);
                s.batteryCycleCount = cycles >= 0 ? cycles : null;
            }
        } else {
            s.batteryLevel = -1;
            s.batteryVoltageMv = -1;
            s.batteryTempC = Float.NaN;
            s.batteryStatus = "Desconhecido";
            s.batteryHealth = "Desconhecida";
            s.batteryPowerSource = "Desconhecida";
        }

        BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        if (bm != null) {
            s.batteryCurrentNowMicroA = validIntProperty(bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW));
            s.batteryCurrentAverageMicroA = validIntProperty(bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE));
            s.batteryChargeCounterMicroAh = validIntProperty(bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER));
            long energy = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);
            s.batteryEnergyCounterNanoWh = energy == Long.MIN_VALUE ? null : energy;
        }

        s.uptimeMs = SystemClock.elapsedRealtime();
        return s;
    }

    private static Integer validIntProperty(int value) {
        return value == Integer.MIN_VALUE ? null : value;
    }

    private static String readCpuName() {
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String lower = line.toLowerCase(Locale.ROOT);
                if (lower.startsWith("hardware") || lower.startsWith("model name")) {
                    int idx = line.indexOf(':');
                    if (idx >= 0 && idx + 1 < line.length()) return line.substring(idx + 1).trim();
                }
            }
        } catch (Exception ignored) {}
        return "Não exposto pelo Android";
    }

    private static String batteryStatus(int status) {
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING: return "Carregando";
            case BatteryManager.BATTERY_STATUS_DISCHARGING: return "Descarregando";
            case BatteryManager.BATTERY_STATUS_FULL: return "Cheia";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING: return "Conectada / sem carregar";
            default: return "Desconhecido";
        }
    }

    private static String batteryHealth(int health) {
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD: return "Boa";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "Superaquecimento";
            case BatteryManager.BATTERY_HEALTH_DEAD: return "Ruim";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return "Sobretensão";
            case BatteryManager.BATTERY_HEALTH_COLD: return "Muito fria";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE: return "Falha não especificada";
            default: return "Desconhecida";
        }
    }

    private static String batteryPowerSource(int plugged) {
        if ((plugged & BatteryManager.BATTERY_PLUGGED_AC) != 0) return "AC";
        if ((plugged & BatteryManager.BATTERY_PLUGGED_USB) != 0) return "USB";
        if ((plugged & BatteryManager.BATTERY_PLUGGED_WIRELESS) != 0) return "Wireless";
        if (Build.VERSION.SDK_INT >= 33 && (plugged & BatteryManager.BATTERY_PLUGGED_DOCK) != 0) return "Dock";
        return plugged == 0 ? "Bateria" : "Outra";
    }

    public static String bytes(long value) {
        double gb = value / 1024d / 1024d / 1024d;
        return new DecimalFormat("0.00").format(gb) + " GB";
    }

    public static String uptime(long ms) {
        long totalMin = ms / 60000;
        long days = totalMin / 1440;
        long hours = (totalMin % 1440) / 60;
        long mins = totalMin % 60;
        if (days > 0) return days + " d " + hours + " h " + mins + " min";
        if (hours > 0) return hours + " h " + mins + " min";
        return mins + " min";
    }

    private static String safe(String s) {
        return s == null || s.trim().isEmpty() ? "N/D" : s.trim();
    }
}
