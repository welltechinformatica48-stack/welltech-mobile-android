package com.welltech.mobile;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.io.IOException;

public class AgentService extends Service implements LocalAgentServer.Listener {
    public static final String ACTION_START = "com.welltech.mobile.action.START_AGENT";
    public static final String ACTION_STOP = "com.welltech.mobile.action.STOP_AGENT";

    private static final String CHANNEL_ID = "welltech_agent_session";
    private static final int NOTIFICATION_ID = 37183;

    private LocalAgentServer server;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void start(Context context) {
        Intent intent = new Intent(context, AgentService.class).setAction(ACTION_START);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
        else context.startService(intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, AgentService.class).setAction(ACTION_STOP);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
        else context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        enterForeground();
        server = new LocalAgentServer(this, this);
        try {
            server.start();
        } catch (IOException e) {
            onServerError(e.getMessage());
            stopAgent();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopAgent();
            return START_NOT_STICKY;
        }
        updateNotification();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (server != null) server.stop();
        AgentSessionManager.get().revoke();
        removeForegroundNotification();
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopAgent();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStateChanged() {
        mainHandler.post(this::updateNotification);
    }

    @Override
    public void onServerError(String message) {
        mainHandler.post(this::updateNotification);
    }

    @Override
    public void onTimeout(int startId, int fgsType) {
        stopAgent();
    }

    private void stopAgent() {
        if (server != null) server.stop();
        AgentSessionManager.get().revoke();
        removeForegroundNotification();
        stopSelf();
    }

    private void enterForeground() {
        Notification n = buildNotification();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, n);
        }
    }

    private void updateNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIFICATION_ID, buildNotification());
    }

    private Notification buildNotification() {
        Intent openApp = new Intent(this, MainActivity.class);
        PendingIntent openIntent = PendingIntent.getActivity(
                this, 1, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stop = new Intent(this, AgentService.class).setAction(ACTION_STOP);
        PendingIntent stopIntent = PendingIntent.getService(
                this, 2, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AgentSessionManager.State state = AgentSessionManager.get().getState();
        String text;
        switch (state) {
            case PAIRING:
                text = "Aguardando pareamento autorizado";
                break;
            case PAIRED:
                text = "Computador pareado • aguardando sessão";
                break;
            case ACTIVE:
                text = "Sessão de diagnóstico ativa";
                break;
            default:
                text = "Agente local ativo";
        }

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        return builder
                .setSmallIcon(com.welltech.mobile.R.drawable.ic_agent)
                .setContentTitle("Welltech Mobile Agent")
                .setContentText(text)
                .setContentIntent(openIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .addAction(new Notification.Action.Builder(
                        com.welltech.mobile.R.drawable.ic_agent, "Encerrar", stopIntent).build())
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Sessão Welltech Mobile Agent",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Mostra quando o agente local da Welltech está ativo durante um diagnóstico.");
        nm.createNotificationChannel(channel);
    }

    private void removeForegroundNotification() {
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE);
        else stopForeground(true);
    }
}
