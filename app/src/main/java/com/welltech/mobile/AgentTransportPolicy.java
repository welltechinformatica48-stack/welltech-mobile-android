package com.welltech.mobile;

/**
 * Centraliza os modos de transporte do Welltech Mobile Agent.
 *
 * Segurança: somente USB_LOOPBACK fica habilitado por padrão. Wi-Fi local e
 * acesso remoto permanecem bloqueados até existir canal criptografado,
 * autorização explícita e política de revogação implementadas.
 */
public final class AgentTransportPolicy {
    public enum Mode {
        USB_LOOPBACK,
        WIFI_LOCAL_SECURE,
        REMOTE_RELAY
    }

    private static volatile Mode requestedMode = Mode.USB_LOOPBACK;

    private AgentTransportPolicy() {}

    public static Mode getRequestedMode() {
        return requestedMode;
    }

    public static synchronized boolean requestMode(Mode mode) {
        if (mode == null) return false;

        // O Agent 0.3 inicia com USB validado. Modos externos só serão
        // liberados quando a camada criptográfica estiver implementada.
        if (mode != Mode.USB_LOOPBACK) {
            return false;
        }

        requestedMode = mode;
        return true;
    }

    public static String bindAddress() {
        return "127.0.0.1";
    }

    public static boolean requiresEncryption(Mode mode) {
        return mode == Mode.WIFI_LOCAL_SECURE || mode == Mode.REMOTE_RELAY;
    }

    public static boolean isLanExposureAllowed() {
        return false;
    }

    public static boolean isRemoteExposureAllowed() {
        return false;
    }

    public static String publicLabel() {
        switch (requestedMode) {
            case WIFI_LOCAL_SECURE:
                return "Wi-Fi local seguro";
            case REMOTE_RELAY:
                return "Acesso remoto seguro";
            default:
                return "USB / ADB";
        }
    }
}
