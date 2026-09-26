package com.welltech.mobile;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends Activity {
    public static final String ACTION_DESKTOP_PREPARE = "com.welltech.mobile.action.DESKTOP_PREPARE";
    public static final String EXTRA_DESKTOP_SOURCE = "welltech_desktop_source";
    public static final String EXTRA_PREPARE_AGENT = "welltech_prepare_agent";

    private static final int BG = Color.rgb(8,16,13);
    private static final int PANEL = Color.rgb(16,26,22);
    private static final int PANEL2 = Color.rgb(20,35,28);
    private static final int GREEN = Color.rgb(120,240,76);
    private static final int TEXT = Color.rgb(244,247,245);
    private static final int MUTED = Color.rgb(152,166,159);
    private static final int WARN = Color.rgb(255,200,87);
    private static final int REQUEST_NOTIFICATIONS = 7001;

    private LinearLayout content;
    private DeviceDiagnostics.Snapshot snapshot;
    private TextView agentState;
    private TextView pairingCode;
    private Button pairButton;
    private Button stopButton;
    private TextView desktopRequestState;
    private TextView wifiState;
    private TextView liveSummaryText;
    private boolean desktopRequested;
    private boolean pairingPendingAfterNotificationPermission;
    private final Handler statusHandler = new Handler(Looper.getMainLooper());

    private final Runnable statusTick = new Runnable() {
        @Override public void run() {
            updateAgentUi();
            updateLiveSummary();
            if (wifiState != null) wifiState.setText(WifiBridgeInfo.desktopHint(MainActivity.this));
            statusHandler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            buildUi();
            showOverview();
            handleLaunchIntent(getIntent());
        } catch (Throwable startupError) {
            showStartupRecovery(startupError);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleLaunchIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        snapshot = DeviceDiagnostics.collect(this);
        statusHandler.removeCallbacks(statusTick);
        statusHandler.post(statusTick);
    }

    @Override
    protected void onPause() {
        statusHandler.removeCallbacks(statusTick);
        super.onPause();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        TextView brand = new TextView(this);
        brand.setText("WELLTECH MOBILE AGENT  •  " + AgentConstants.AGENT_VERSION);
        brand.setTextColor(GREEN);
        brand.setTextSize(22);
        brand.setTypeface(null, 1);
        brand.setPadding(dp(18), dp(18), dp(18), dp(8));
        root.addView(brand);

        TextView subtitle = new TextView(this);
        subtitle.setText("Diagnóstico técnico • USB / Wi‑Fi ADB • sessão autorizada");
        subtitle.setTextColor(MUTED);
        subtitle.setTextSize(13);
        subtitle.setPadding(dp(18), 0, dp(18), dp(12));
        root.addView(subtitle);

        LinearLayout agentPanel = new LinearLayout(this);
        agentPanel.setOrientation(LinearLayout.VERTICAL);
        agentPanel.setPadding(dp(14), dp(12), dp(14), dp(12));
        agentPanel.setBackgroundColor(PANEL);

        agentState = new TextView(this);
        agentState.setTextColor(TEXT);
        agentState.setTextSize(14);
        agentState.setTypeface(null, 1);
        agentPanel.addView(agentState);

        pairingCode = new TextView(this);
        pairingCode.setTextColor(GREEN);
        pairingCode.setTextSize(24);
        pairingCode.setTypeface(null, 1);
        pairingCode.setPadding(0, dp(4), 0, dp(8));
        agentPanel.addView(pairingCode);

        LinearLayout agentActions = new LinearLayout(this);
        agentActions.setOrientation(LinearLayout.HORIZONTAL);

        pairButton = miniAction("ABRIR PAREAMENTO");
        pairButton.setOnClickListener(v -> requestPairing());
        agentActions.addView(pairButton, weightedButtonParams());

        stopButton = miniGhost("ENCERRAR AGENTE");
        stopButton.setOnClickListener(v -> {
            AgentService.stop(this);
            Toast.makeText(this, "Agente encerrado", Toast.LENGTH_SHORT).show();
        });
        LinearLayout.LayoutParams stopLp = weightedButtonParams();
        stopLp.setMargins(dp(6), 0, 0, 0);
        agentActions.addView(stopButton, stopLp);

        agentPanel.addView(agentActions);

        desktopRequestState = new TextView(this);
        desktopRequestState.setTextColor(MUTED);
        desktopRequestState.setTextSize(12);
        desktopRequestState.setPadding(0, dp(8), 0, 0);
        desktopRequestState.setText("Desktop: nenhuma solicitação USB/ADB recebida");
        agentPanel.addView(desktopRequestState);

        TextView localOnly = new TextView(this);
        localOnly.setText("Transporte: USB ou Wi‑Fi ADB seguro • túnel local 127.0.0.1:" + AgentConstants.DEVICE_PORT + " • nenhuma porta do Agent exposta na rede");
        localOnly.setTextColor(MUTED);
        localOnly.setTextSize(11);
        localOnly.setPadding(0, dp(8), 0, 0);
        agentPanel.addView(localOnly);

        wifiState = new TextView(this);
        wifiState.setTextColor(MUTED);
        wifiState.setTextSize(12);
        wifiState.setPadding(0, dp(10), 0, dp(8));
        wifiState.setText(WifiBridgeInfo.desktopHint(this));
        agentPanel.addView(wifiState);

        Button wifiButton = miniGhost("ABRIR DEPURAÇÃO SEM FIO");
        wifiButton.setOnClickListener(v -> {
            boolean opened = WifiBridgeInfo.openWirelessDebugging(this);
            Toast.makeText(this,
                    opened
                            ? "Na tela do Android, toque em Parear dispositivo com código. O Desktop vai procurar IP/porta automaticamente."
                            : "Não foi possível abrir a Depuração sem fio neste Android.",
                    Toast.LENGTH_LONG).show();
        });
        LinearLayout.LayoutParams wifiLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(46));
        wifiLp.setMargins(0, 0, 0, dp(4));
        agentPanel.addView(wifiButton, wifiLp);

        LinearLayout.LayoutParams agentLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        agentLp.setMargins(dp(14), 0, dp(14), dp(10));
        root.addView(agentPanel, agentLp);

        HorizontalScrollView navScroll = new HorizontalScrollView(this);
        navScroll.setHorizontalScrollBarEnabled(false);
        navScroll.setFillViewport(false);

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(dp(12), 0, dp(12), dp(10));

        String[] labels = {"Resumo","Sistema","Bateria","Apps","Segurança","Relatório"};
        for (String label : labels) {
            Button b = new Button(this);
            b.setText(label);
            b.setAllCaps(false);
            b.setTextSize(12);
            b.setTextColor(TEXT);
            b.setBackgroundColor(PANEL2);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(104), dp(48));
            lp.setMargins(dp(3),0,dp(3),0);
            b.setLayoutParams(lp);
            b.setOnClickListener(v -> navigate(label));
            nav.addView(b);
        }
        navScroll.addView(nav);
        root.addView(navScroll);

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(4), dp(14), dp(24));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        updateAgentUi();
    }

    private void showStartupRecovery(Throwable error) {
        try {
            LinearLayout root = new LinearLayout(this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(dp(18), dp(24), dp(18), dp(24));
            root.setBackgroundColor(BG);

            TextView title = new TextView(this);
            title.setText("WELLTECH MOBILE AGENT");
            title.setTextColor(GREEN);
            title.setTextSize(22);
            title.setTypeface(null, 1);
            root.addView(title);

            TextView message = new TextView(this);
            message.setText("O aplicativo abriu em modo de recuperação. Uma leitura do Android não respondeu como esperado, mas o app não será encerrado.\n\nDetalhe técnico: " +
                    (error == null ? "indisponível" : error.getClass().getSimpleName()));
            message.setTextColor(TEXT);
            message.setTextSize(15);
            message.setPadding(0, dp(18), 0, dp(18));
            root.addView(message);

            Button retry = action("TENTAR ABRIR DIAGNÓSTICO");
            retry.setOnClickListener(v -> recreate());
            root.addView(retry);
            setContentView(root);
        } catch (Throwable ignored) {
            // Last-resort: Android keeps the Activity alive instead of crashing repeatedly.
        }
    }

    private void handleLaunchIntent(Intent intent) {
        if (intent == null) return;

        boolean requestedByDesktop =
                ACTION_DESKTOP_PREPARE.equals(intent.getAction()) ||
                intent.getBooleanExtra(EXTRA_PREPARE_AGENT, false) ||
                "desktop".equalsIgnoreCase(intent.getStringExtra(EXTRA_DESKTOP_SOURCE));

        if (!requestedByDesktop) return;

        desktopRequested = true;
        try {
            AgentService.start(this);
            if (desktopRequestState != null) {
                desktopRequestState.setText("Desktop: solicitação USB/ADB recebida • Agent preparado");
                desktopRequestState.setTextColor(GREEN);
            }
            updateAgentUi();
            Toast.makeText(this,
                    "Welltech Desktop detectado. Toque em ABRIR PAREAMENTO para autorizar.",
                    Toast.LENGTH_LONG).show();
        } catch (Throwable error) {
            if (desktopRequestState != null) {
                desktopRequestState.setText("Desktop: solicitação recebida • falha ao preparar Agent");
                desktopRequestState.setTextColor(WARN);
            }
        }
    }

    private void requestPairing() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pairingPendingAfterNotificationPermission = true;
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
            return;
        }
        openPairingNow();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATIONS && pairingPendingAfterNotificationPermission) {
            pairingPendingAfterNotificationPermission = false;
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notificações negadas. O Agent pode iniciar, mas o Android pode ocultar o aviso da sessão.", Toast.LENGTH_LONG).show();
            }
            openPairingNow();
        }
    }

    private void openPairingNow() {
        String code = AgentSessionManager.get().openPairingWindow();
        AgentService.start(this);
        updateAgentUi();
        Toast.makeText(this, "Pareamento aberto por 60 segundos: " + code, Toast.LENGTH_SHORT).show();
    }

    private void updateAgentUi() {
        if (agentState == null || pairingCode == null) return;
        AgentSessionManager sm = AgentSessionManager.get();
        AgentSessionManager.State state = sm.getState();
        switch (state) {
            case PAIRING:
                agentState.setText("● AGUARDANDO WELLTECH • " + sm.getPairingRemainingSeconds() + " s");
                agentState.setTextColor(WARN);
                pairingCode.setText("Código: " + sm.getPairingCode());
                pairButton.setText("GERAR NOVO CÓDIGO");
                stopButton.setEnabled(true);
                break;
            case PAIRED:
                agentState.setText("● COMPUTADOR PAREADO • aguardando início da sessão");
                agentState.setTextColor(GREEN);
                pairingCode.setText("Sessão autenticada");
                pairButton.setText("REFAZER PAREAMENTO");
                stopButton.setEnabled(true);
                break;
            case ACTIVE:
                agentState.setText("● CONECTADO • diagnóstico em andamento");
                agentState.setTextColor(GREEN);
                pairingCode.setText("Protocolo " + AgentConstants.PROTOCOL + " • Agent " + AgentConstants.AGENT_VERSION);
                pairButton.setText("NOVO PAREAMENTO");
                stopButton.setEnabled(true);
                break;
            default:
                if (desktopRequested) {
                    agentState.setText("○ DESKTOP DETECTADO • aguardando autorização");
                } else {
                    agentState.setText("○ DESCONECTADO • agente aguardando autorização");
                }
                agentState.setTextColor(MUTED);
                pairingCode.setText("Nenhuma sessão ativa");
                pairButton.setText("ABRIR PAREAMENTO");
                stopButton.setEnabled(false);
        }
    }

    private void navigate(String label) {
        switch (label) {
            case "Sistema": showSystem(); break;
            case "Bateria": showBattery(); break;
            case "Apps": showApps(); break;
            case "Segurança": showSecurity(); break;
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
        clear("Visão geral", "Agent " + AgentConstants.AGENT_VERSION + " • Protocolo " + AgentConstants.PROTOCOL);

        liveSummaryCard();

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

        card("BATERIA", "Nível: " + displayBatteryLevel() +
                "\nEstado: " + snapshot.batteryStatus +
                "\nSaúde reportada: " + snapshot.batteryHealth +
                "\nTemperatura: " + displayBatteryTemp());

        card("CONEXÃO", "Modo: USB / Wi‑Fi ADB\n" +
                WifiBridgeInfo.desktopHint(this) + "\n" +
                "Agent: " + AgentConstants.AGENT_VERSION + "\n" +
                "Servidor local: 127.0.0.1:" + AgentConstants.DEVICE_PORT +
                "\nAPI: /api/v1" +
                "\nPareamento: código temporário + token forte" +
                "\nStream: WebSocket com heartbeat de 5 s" +
                "\nNuvem: desativada por padrão");

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
        card("ESTADO ATUAL", "Nível: " + displayBatteryLevel() +
                "\nEstado: " + snapshot.batteryStatus +
                "\nSaúde reportada: " + snapshot.batteryHealth +
                "\nFonte: " + snapshot.batteryPowerSource +
                "\nTemperatura: " + displayBatteryTemp() +
                "\nTensão: " + (snapshot.batteryVoltageMv >= 0 ? snapshot.batteryVoltageMv + " mV" : "Não disponível"));

        card("TELEMETRIA AVANÇADA",
                "Corrente instantânea: " + numberOrUnavailable(snapshot.batteryCurrentNowMicroA, " µA") +
                "\nCorrente média: " + numberOrUnavailable(snapshot.batteryCurrentAverageMicroA, " µA") +
                "\nCharge counter: " + numberOrUnavailable(snapshot.batteryChargeCounterMicroAh, " µAh") +
                "\nEnergy counter: " + numberOrUnavailable(snapshot.batteryEnergyCounterNanoWh, " nWh") +
                "\nCiclos: " + numberOrUnavailable(snapshot.batteryCycleCount, ""));

        card("OBSERVAÇÃO TÉCNICA",
                "Nem todo Android expõe corrente, capacidade, ciclos ou saúde percentual real. " +
                "Quando a API não fornece um dado confiável, o Welltech marca como não disponível em vez de inventar um valor.");
    }

    private void showApps() {
        clear("Uso de aplicativos", "Top apps por tempo em primeiro plano nas últimas 24 horas");

        if (!UsageStatsHelper.hasPermission(this)) {
            card("ACESSO NECESSÁRIO",
                    "O Android exige autorização manual para consultar tempo de uso. Esse acesso é somente leitura e aparece como capability permission_required no Agent.");
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

    private void liveSummaryCard() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(14), dp(16), dp(14));
        box.setBackgroundColor(PANEL);

        TextView h = new TextView(this);
        h.setText("TEMPO REAL");
        h.setTextColor(GREEN);
        h.setTextSize(13);
        h.setTypeface(null, 1);
        box.addView(h);

        liveSummaryText = new TextView(this);
        liveSummaryText.setTextColor(TEXT);
        liveSummaryText.setTextSize(15);
        liveSummaryText.setLineSpacing(0, 1.15f);
        liveSummaryText.setPadding(0, dp(8), 0, 0);
        box.addView(liveSummaryText);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        box.setLayoutParams(lp);
        content.addView(box);
        updateLiveSummary();
    }

    private void updateLiveSummary() {
        if (liveSummaryText == null) return;
        try {
            DeviceDiagnostics.Snapshot d = DeviceDiagnostics.collect(this);
            RuntimeDiagnostics.Snapshot r = RuntimeDiagnostics.collect(this, d);
            String battery = d.batteryLevel >= 0 ? d.batteryLevel + "%" : "N/D";
            String temp = Float.isNaN(d.batteryTempC) ? "N/D" : d.batteryTempC + " °C";
            String ram = r.ramAvailablePercent >= 0 ? r.ramAvailablePercent + "%" : "N/D";
            String storage = r.storageFreePercent >= 0 ? r.storageFreePercent + "%" : "N/D";
            String ip = r.wifiIpv4 == null ? "N/D" : r.wifiIpv4;
            String thermal = r.thermalLabel == null ? "N/D" : r.thermalLabel;
            liveSummaryText.setText(
                    "Bateria: " + battery + " • " + temp +
                    "\nRAM disponível: " + ram +
                    "\nArmazenamento livre: " + storage +
                    "\nRede: " + r.networkTransport + " • IP " + ip +
                    "\nVPN: " + (r.vpnActive ? "ativa" : "não") +
                    " • Proxy: " + (r.proxyConfigured ? "configurado" : "não") +
                    "\nTérmico: " + thermal +
                    " • Tela: " + (r.screenInteractive ? "ativa" : "apagada") +
                    "\nTempo ligado: " + DeviceDiagnostics.uptime(d.uptimeMs) +
                    "\nRisco atual: " + RuntimeDiagnostics.riskLevel(r, d)
            );
        } catch (Throwable ignored) {
            liveSummaryText.setText("Leitura em tempo real temporariamente indisponível.");
        }
    }

    private void showSecurity() {
        clear("Segurança", "Sinais técnicos de risco • não substitui um motor antivírus");
        SecurityDiagnostics.Snapshot s = SecurityDiagnostics.collect(this);

        StringBuilder findings = new StringBuilder();
        if (s.findings == null || s.findings.length == 0) {
            findings.append("Nenhum alerta básico detectado nesta leitura.");
        } else {
            for (String finding : s.findings) {
                if (findings.length() > 0) findings.append("\n");
                findings.append("• ").append(finding);
            }
        }

        card("ESTADO DE SEGURANÇA",
                "Nível: " + s.riskLevel +
                "\nPatch Android: " + s.securityPatch +
                "\nOpções do desenvolvedor: " + (s.developerOptionsEnabled ? "ativadas" : "desativadas") +
                "\nADB: " + (s.adbEnabled ? "ativado" : "desativado") +
                "\nVPN: " + (s.vpnActive ? "ativa" : "não") +
                "\nProxy: " + (s.proxyConfigured ? "configurado" : "não") +
                "\nDNS privado: " + (s.privateDnsMode == null ? "N/D" : s.privateDnsMode));

        card("ALERTAS / ACHADOS", findings.toString());

        card("LIMITE DESTA VERSÃO",
                "Esta versão analisa sinais do sistema e comportamento básico. " +
                "Ela ainda não possui motor de assinaturas/reputação para afirmar que um aparelho está livre de malware.");
    }

    private void showReport() {
        clear("Relatório", "Resumo técnico local para compartilhar manualmente");
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

        return "WELLTECH MOBILE AGENT - " + AgentConstants.AGENT_VERSION + "\n\n" +
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
                "Bateria: " + displayBatteryLevel() + "\n" +
                "Estado: " + snapshot.batteryStatus + "\n" +
                "Saúde reportada: " + snapshot.batteryHealth + "\n" +
                "Temperatura: " + displayBatteryTemp() + "\n" +
                "Tensão: " + (snapshot.batteryVoltageMv >= 0 ? snapshot.batteryVoltageMv + " mV" : "Não disponível") + "\n" +
                "Tempo ligado: " + DeviceDiagnostics.uptime(snapshot.uptimeMs) + "\n\n" +
                "Modo: diagnóstico local somente leitura. Metadados da sessão do Agent não são anexados automaticamente a este relatório.";
    }

    private String displayBatteryLevel() {
        return snapshot.batteryLevel >= 0 ? snapshot.batteryLevel + "%" : "Não disponível";
    }

    private String displayBatteryTemp() {
        return Float.isNaN(snapshot.batteryTempC) ? "Não disponível" : snapshot.batteryTempC + " °C";
    }

    private static String numberOrUnavailable(Number value, String unit) {
        return value == null ? "Não disponível" : value + unit;
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

    private Button miniAction(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.rgb(7,16,8));
        b.setTextSize(11);
        b.setTypeface(null, 1);
        b.setBackgroundColor(GREEN);
        return b;
    }

    private Button miniGhost(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(TEXT);
        b.setTextSize(11);
        b.setTypeface(null, 1);
        b.setBackgroundColor(PANEL2);
        return b;
    }

    private LinearLayout.LayoutParams weightedButtonParams() {
        return new LinearLayout.LayoutParams(0, dp(46), 1f);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
