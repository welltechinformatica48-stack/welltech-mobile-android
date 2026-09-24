package com.welltech.mobile;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Process;
import android.provider.Settings;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class UsageStatsHelper {
    public static class AppUsage {
        public String label, packageName;
        public long foregroundMs, lastUsed;
    }

    private UsageStatsHelper() {}

    public static boolean hasPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow("android:get_usage_stats", Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    public static void openPermissionSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static List<AppUsage> topApps(Context context, long intervalMs, int limit) {
        List<AppUsage> out = new ArrayList<>();
        if (!hasPermission(context)) return out;

        long end = System.currentTimeMillis();
        long start = end - intervalMs;
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        List<UsageStats> list = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end);
        if (list == null) return out;

        PackageManager pm = context.getPackageManager();
        for (UsageStats u : list) {
            if (u.getTotalTimeInForeground() <= 0) continue;
            AppUsage a = new AppUsage();
            a.packageName = u.getPackageName();
            a.foregroundMs = u.getTotalTimeInForeground();
            a.lastUsed = u.getLastTimeUsed();
            try {
                ApplicationInfo info = pm.getApplicationInfo(a.packageName, 0);
                a.label = pm.getApplicationLabel(info).toString();
            } catch (Exception e) {
                a.label = a.packageName;
            }
            out.add(a);
        }
        out.sort((a,b) -> Long.compare(b.foregroundMs, a.foregroundMs));
        if (out.size() > limit) return new ArrayList<>(out.subList(0, limit));
        return out;
    }

    public static String duration(long ms) {
        long totalMin = ms / 60000;
        long h = totalMin / 60;
        long m = totalMin % 60;
        return h > 0 ? h + " h " + m + " min" : m + " min";
    }

    public static String date(long when) {
        if (when <= 0) return "N/D";
        return new SimpleDateFormat("dd/MM HH:mm", new Locale("pt","BR")).format(new Date(when));
    }
}
