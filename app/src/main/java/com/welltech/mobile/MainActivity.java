package com.welltech.mobile;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(8,16,13);
    private static final int PANEL = Color.rgb(16,26,22);
    private static final int PANEL2 = Color.rgb(20,35,28);
    private static final int GREEN = Color.rgb(120,240,76);
    private static final int TEXT = Color.rgb(244,247,245);
    private static final int MUTED = Color.rgb(152,166,159);

    private LinearLayout content;
    private DeviceDiagnostics.Snapshot snapshot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        showOverview();
    }

    @Override
    protected void onResume() {
        super.onResume();
        snapshot = DeviceDiagnostics.collect(this);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        TextView brand = new TextView(this);
        brand.setText("WELLTECH  MOBILE");
        brand.setTextColor(GREEN);
        brand.setTextSize(22);
        brand.setTypeface(null, 1);
        brand.setPadding(dp(18), dp(18), dp(18), dp(8));
        root.addView(brand);

        TextView subtitle = new TextView(this);
        subtitle.setText("Diagnóstico técnico no próprio aparelho");
        subtitle.setTextColor(MUTED);
        subtitle.setTextSize(13);
        subtitle.setPadding(dp(18), 0, dp(18), dp(14));
        root.addView(subtitle);

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(dp(12), 0, dp(12), dp(10));

        String[] labels = {"Resumo","Sistema","Bateria","Apps","Relatório"};
        for (String label : labels) {
            Button b = new Button(this);
            b.setText(label);
            b.setTextSize(11);
            b.setTextColor(TEXT);
            b.setBackgroundColor(PANEL2);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1f);
            lp.setMargins(dp(3),0,dp(3),0);
            b.setLayoutParams(lp);
            b.setOnClickListener(v -> navigate(label));
            nav.addView(b);
        }
        root.addView(nav);

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(4), dp(14), dp(24));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    private void navigate(String label) {
        switch (label) {
            case "Sistema": showSystem(); break;
            case "Bateria": showBattery(); break;
            case "Apps": showApps(); break;
            case "Relatório": showReport(); break;
            default: showOverview();
        }
    }

    private void refresh() { snapshot = DeviceDiagnostics.collect(this); }

    private void clear(String title, String subtitle) {
        refresh();
        content.removeAllViews();
        title(title);
        if (subtitle != null) muted(subtitle);
    }

    private void showOverview() {
        clear("Visão geral", "Alpha 0.1.0 • somente leitura");

        card("DISPOSITIVO", snapshot.manufacturer + " " + snapshot.model +
                "\nAndroid " + snapshot.androidVersion + " • API " + snapshot.sdk +
                "\nPatch: " + snapshot.securityPatch);

        long used = Math.max(0, snapshot.storageTotal - snapshot.storageFree);
        int storagePct = snapshot.storageTotal > 0 ? (int)Math.round(used * 100d / snapshot.storageTotal) : 0;
        card("ARMAZENAMENTO", "Total: " + DeviceDiagnostics.bytes(snapshot.storageTotal) +
                "\nLivre: " + DeviceDiagnostics.bytes(snapshot.storageFree) +
                "\nUso estimado: " + storagePct + "%");

        int ramPct = snapshot.ramTotal > 0 ? (int)Math.round(snapshot.ramAvailable * 100d / snapshot.ramTotal) : 0;
        card("MEMÓRIA", "RAM total: " + DeviceDiagnostics.bytes(snapshot.ramTotal) +
                "\nDisponível: " + DeviceDiagnostics.bytes(snapshot.ramAvailable) +
                "\nDisponível agora: " + ramPct + "%");

        card("BATERIA", "Nível: " + snapshot.batteryLevel + "%" +
                "\nEstado: " + snapshot.batteryStatus +
                "\nSaúde reportada: " + snapshot.batteryHealth +
                "\nTemperatura: " + snapshot.batteryTempC + " °C");

        Button update = action("ATUALIZAR LEITURA");
        update.setOnClickListener(v -> showOverview());
        content.addView(update);
    }

    private void showSystem() {
        clear("Sistema", "Inventário local do Android");
        card("ANDROID / BUILD", "Android: " + snapshot.androidVersion +
                "\nAPI: " + snapshot.sdk +
                "\nPatch de segurança: " + snapshot.securityPatch +
                "\nBuild ID: " + snapshot.buildId +
                "\nFingerprint:\n" + snapshot.buildFingerprint);
        card("HARDWARE", "Fabricante: " + snapshot.manufacturer +
                "\nMarca: " + snapshot.brand +
                "\nModelo: " + snapshot.model +
                "\nDevice: " + snapshot.device +
                "\nProduct: " + snapshot.product +
                "\nBoard: " + snapshot.board +
                "\nHardware: " + snapshot.hardware);
        card("CPU", "CPU: " + snapshot.cpuName +
                "\nNúcleos disponíveis: " + snapshot.cpuCores +
                "\nABIs: " + snapshot.abis);
        card("TEMPO LIGADO", DeviceDiagnostics.uptime(snapshot.uptimeMs));
    }

    private void showBattery() {
        clear("Bateria", "Leituras disponibilizadas pelas APIs públicas do Android");
        card("ESTADO ATUAL", "Nível: " + snapshot.batteryLevel + "%" +
                "\nEstado: " + snapshot.batteryStatus +
                "\nSaúde reportada: " + snapshot.batteryHealth +
                "\nTemperatura: " + snapshot.batteryTempC + " °C" +
                "\nTensão: " + snapshot.batteryVoltageMv + " mV");
        card("OBSERVAÇÃO TÉCNICA",
                "Nem todo Android expõe capacidade de projeto, ciclos ou saúde percentual real. " +
                "O Welltech não inventa valores que o aparelho não fornece.");
    }

    private void showApps() {
        clear("Uso de aplicativos", "Top apps por tempo em primeiro plano nas últimas 24 horas");

        if (!UsageStatsHelper.hasPermission(this)) {
            card("ACESSO NECESSÁRIO",
                    "O Android exige autorização manual para consultar tempo de uso. Esse acesso é somente leitura.");
            Button permission = action("CONCEDER ACESSO DE USO");
            permission.setOnClickListener(v -> UsageStatsHelper.openPermissionSettings(this));
            content.addView(permission);
            return;
        }

        List<UsageStatsHelper.AppUsage> apps = UsageStatsHelper.topApps(this, 24L * 60L * 60L * 1000L, 20);
        if (apps.isEmpty()) {
            card("SEM DADOS", "Nenhum tempo de uso foi retornado para o período.");
            return;
        }

        int i = 1;
        for (UsageStatsHelper.AppUsage a : apps) {
            card("#" + i + "  " + a.label,
                    a.packageName +
                    "\nTempo em primeiro plano: " + UsageStatsHelper.duration(a.foregroundMs) +
                    "\nÚltimo uso: " + UsageStatsHelper.date(a.lastUsed));
            i++;
        }
    }

    private void showReport() {
        clear("Relatório", "Resumo técnico para compartilhar");
        String report = buildReport();
        card("RELATÓRIO WELLTECH", report);

        Button share = action("COMPARTILHAR RELATÓRIO");
        share.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Relatório Welltech Mobile");
            intent.putExtra(Intent.EXTRA_TEXT, report);
            startActivity(Intent.createChooser(intent, "Compartilhar relatório"));
        });
        content.addView(share);
    }

    private String buildReport() {
        long used = Math.max(0, snapshot.storageTotal - snapshot.storageFree);
        int storagePct = snapshot.storageTotal > 0 ? (int)Math.round(used * 100d / snapshot.storageTotal) : 0;

        return "WELLTECH MOBILE - ALPHA 0.1.0\n\n" +
                "Dispositivo: " + snapshot.manufacturer + " " + snapshot.model + "\n" +
                "Android: " + snapshot.androidVersion + " (API " + snapshot.sdk + ")\n" +
                "Patch: " + snapshot.securityPatch + "\n" +
                "CPU: " + snapshot.cpuName + "\n" +
                "Núcleos: " + snapshot.cpuCores + "\n" +
                "RAM total: " + DeviceDiagnostics.bytes(snapshot.ramTotal) + "\n" +
                "RAM disponível: " + DeviceDiagnostics.bytes(snapshot.ramAvailable) + "\n" +
                "Armazenamento: " + DeviceDiagnostics.bytes(snapshot.storageTotal) + "\n" +
                "Armazenamento livre: " + DeviceDiagnostics.bytes(snapshot.storageFree) + "\n" +
                "Armazenamento usado: " + storagePct + "%\n" +
                "Bateria: " + snapshot.batteryLevel + "%\n" +
                "Estado: " + snapshot.batteryStatus + "\n" +
                "Saúde reportada: " + snapshot.batteryHealth + "\n" +
                "Temperatura: " + snapshot.batteryTempC + " °C\n" +
                "Tensão: " + snapshot.batteryVoltageMv + " mV\n" +
                "Tempo ligado: " + DeviceDiagnostics.uptime(snapshot.uptimeMs) + "\n\n" +
                "Modo: somente leitura.";
    }

    private void title(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(TEXT);
        v.setTextSize(26);
        v.setTypeface(null, 1);
        v.setPadding(dp(4), dp(8), dp(4), dp(4));
        content.addView(v);
    }

    private void muted(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(MUTED);
        v.setTextSize(13);
        v.setPadding(dp(4), 0, dp(4), dp(14));
        content.addView(v);
    }

    private void card(String heading, String body) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(14), dp(16), dp(14));
        box.setBackgroundColor(PANEL);

        TextView h = new TextView(this);
        h.setText(heading);
        h.setTextColor(GREEN);
        h.setTextSize(13);
        h.setTypeface(null, 1);
        box.addView(h);

        TextView b = new TextView(this);
        b.setText(body);
        b.setTextColor(TEXT);
        b.setTextSize(15);
        b.setLineSpacing(0, 1.15f);
        b.setPadding(0, dp(8), 0, 0);
        box.addView(b);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        box.setLayoutParams(lp);
        content.addView(box);
    }

    private Button action(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.rgb(7,16,8));
        b.setTextSize(14);
        b.setTypeface(null, 1);
        b.setBackgroundColor(GREEN);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        lp.setMargins(0, dp(6), 0, dp(10));
        b.setLayoutParams(lp);
        return b;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
