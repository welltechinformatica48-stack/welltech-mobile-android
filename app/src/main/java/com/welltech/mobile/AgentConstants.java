package com.welltech.mobile;

public final class AgentConstants {
    public static final String SERVICE_NAME = "welltech-mobile-agent";
    public static final String AGENT_VERSION = "0.2.0-alpha";
    public static final int PROTOCOL_MAJOR = 1;
    public static final int PROTOCOL_MINOR = 0;
    public static final String PROTOCOL = "1.0";
    public static final String API_BASE = "/api/v1";
    public static final int DEVICE_PORT = 37183;
    public static final long HEARTBEAT_MS = 5_000L;
    public static final long PAIRING_WINDOW_MS = 60_000L;

    private AgentConstants() {}
}
