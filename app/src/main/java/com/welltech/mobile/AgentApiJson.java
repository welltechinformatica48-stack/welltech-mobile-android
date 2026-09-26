package com.welltech.mobile;

import android.content.Context;
import android.os.SystemClock;

import org.json.JSONException;
import org.json.JSONObject;

public final class AgentApiJson {
    private AgentApiJson() {}

    public static JSONObject health() throws JSONException {
        AgentSessionManager sm = AgentSessionManager.get();
        JSONObject o = new JSONObject();
        o.put("service", AgentConstants.SERVICE_NAME);
        o.put("protocolMajor", AgentConstants.PROTOCOL_MAJOR);
        o.put("protocolMinor", AgentConstants.PROTOCOL_MINOR);
        o.put("agentVersion", AgentConstants.AGENT_VERSION);
        o.put("connectionState", connectionState(sm.getState()));
        o.put("readyForPairing", sm.isPairingOpen());
        o.put("pairingRemainingSeconds", sm.getPairingRemainingSeconds());
        o.put("paired", sm.isPaired());
        o.put("active", sm.isActive());
        return o;
    }

    public static JSONObject pairingResponse(AgentSessionManager.PairResult pair) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("protocol", AgentConstants.PROTOCOL);
        o.put("agentVersion", AgentConstants.AGENT_VERSION);
        o.put("sessionId", pair.sessionId);
        o.put("token", pair.token);
        o.put("tokenType", "Bearer");
        o.put("connectionState", "paired");
        return o;
    }

    public static JSONObject sessionState(String state) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("protocol", AgentConstants.PROTOCOL);
        o.put("agentVersion", AgentConstants.AGENT_VERSION);
        o.put("sessionId", valueOrNull(AgentSessionManager.get().getSessionId()));
        o.put("state", state);
        return o;
    }

    public static JSONObject capabilities(Context context) throws JSONException {
        JSONObject caps = new JSONObject();
        caps.put("device.basic", capability("available", null, null));
        caps.put("battery.basic", capability("available", null, null));
        caps.put("battery.advanced", capability("available", "metric_level_support_varies_by_device", null));
        caps.put("runtime.summary", capability("available", null, null));
        caps.put("security.status", capability("available", "signals_only_no_antimalware_engine", null));

        if (UsageStatsHelper.hasPermission(context)) {
            caps.put("usage.foreground", capability("available", null, null));
            caps.put("usage.history", capability("available", null, null));
        } else {
            caps.put("usage.foreground", capability("permission_required", "usage_access_not_granted", "usage_access"));
            caps.put("usage.history", capability("permission_required", "usage_access_not_granted", "usage_access"));
        }

        caps.put("media.session", capability("disabled", "not_implemented_in_agent_0_2_0", null));
        caps.put("media.notification", capability("disabled", "not_implemented_in_agent_0_2_0", null));
        caps.put("media.accessibility_fallback", capability("disabled", "optional_future_capability", null));
        caps.put("network.total", capability("disabled", "not_implemented_in_agent_0_2_0", null));
        caps.put("network.per_uid", capability("disabled", "not_implemented_in_agent_0_2_0", null));
        caps.put("storage.per_app", capability("disabled", "not_implemented_in_agent_0_2_0", null));
        caps.put("apps.inventory", capability("disabled", "not_implemented_in_agent_0_2_0", null));
        caps.put("telephony.basic", capability("disabled", "not_implemented_in_agent_0_2_0", null));
        caps.put("telephony.cell", capability("disabled", "not_implemented_in_agent_0_2_0", null));
        caps.put("sensors.inventory", capability("disabled", "not_implemented_in_agent_0_2_0", null));
        caps.put("sensors.live", capability("disabled", "not_implemented_in_agent_0_2_0", null));
        caps.put("fleet.remote_checkin", capability("disabled", "relay_not_configured", null));

        JSONObject out = new JSONObject();
        out.put("protocol", AgentConstants.PROTOCOL);
        out.put("agentVersion", AgentConstants.AGENT_VERSION);
        out.put("capabilities", caps);
        return out;
    }

    public static JSONObject deviceEnvelope(Context context) throws JSONException {
        DeviceDiagnostics.Snapshot s = DeviceDiagnostics.collect(context);
        JSONObject p = new JSONObject();
        p.put("manufacturer", metricString(s.manufacturer));
        p.put("brand", metricString(s.brand));
        p.put("model", metricString(s.model));
        p.put("device", metricString(s.device));
        p.put("product", metricString(s.product));
        p.put("board", metricString(s.board));
        p.put("hardware", metricString(s.hardware));
        p.put("androidVersion", metricString(s.androidVersion));
        p.put("apiLevel", metricNumber(s.sdk));
        p.put("securityPatch", metricString(s.securityPatch));
        p.put("buildId", metricString(s.buildId));
        p.put("abis", metricString(s.abis));
        p.put("cpuName", metricString(s.cpuName));
        p.put("cpuCores", metricNumber(s.cpuCores));
        p.put("ramTotalBytes", metricNumber(s.ramTotal));
        p.put("ramAvailableBytes", metricNumber(s.ramAvailable));
        p.put("storageTotalBytes", metricNumber(s.storageTotal));
        p.put("storageFreeBytes", metricNumber(s.storageFree));
        p.put("uptimeMs", metricNumber(s.uptimeMs));
        return envelope("device.snapshot/1", "agent.device", p);
    }

    public static JSONObject batteryEnvelope(Context context) throws JSONException {
        DeviceDiagnostics.Snapshot s = DeviceDiagnostics.collect(context);
        JSONObject p = new JSONObject();
        p.put("levelPercent", s.batteryLevel >= 0 ? metricNumber(s.batteryLevel) : unavailable("temporarily_unavailable", "battery_broadcast_missing_level"));
        p.put("status", metricString(s.batteryStatus));
        p.put("healthReported", metricString(s.batteryHealth));
        p.put("powerSource", metricString(s.batteryPowerSource));
        p.put("temperatureC", Float.isNaN(s.batteryTempC) ? unavailable("temporarily_unavailable", "battery_temperature_not_exposed") : metricNumber(s.batteryTempC));
        p.put("voltageMv", s.batteryVoltageMv >= 0 ? metricNumber(s.batteryVoltageMv) : unavailable("temporarily_unavailable", "battery_voltage_not_exposed"));
        p.put("currentNowMicroA", nullableMetric(s.batteryCurrentNowMicroA, "device_does_not_expose_property"));
        p.put("currentAverageMicroA", nullableMetric(s.batteryCurrentAverageMicroA, "device_does_not_expose_property"));
        p.put("chargeCounterMicroAh", nullableMetric(s.batteryChargeCounterMicroAh, "device_does_not_expose_property"));
        p.put("energyCounterNanoWh", nullableMetric(s.batteryEnergyCounterNanoWh, "device_does_not_expose_property"));
        p.put("cycleCount", nullableMetric(s.batteryCycleCount, "device_does_not_expose_cycle_count"));
        return envelope("battery.snapshot/1", "agent.battery_manager", p);
    }

    public static JSONObject summaryEnvelope(Context context) throws JSONException {
        DeviceDiagnostics.Snapshot d = DeviceDiagnostics.collect(context);
        RuntimeDiagnostics.Snapshot r = RuntimeDiagnostics.collect(context, d);

        JSONObject p = new JSONObject();
        p.put("manufacturer", d.manufacturer);
        p.put("model", d.model);
        p.put("androidVersion", d.androidVersion);
        p.put("securityPatch", d.securityPatch);
        p.put("batteryLevelPercent", d.batteryLevel >= 0 ? d.batteryLevel : JSONObject.NULL);
        p.put("batteryTemperatureC", Float.isNaN(d.batteryTempC) ? JSONObject.NULL : d.batteryTempC);
        p.put("batteryStatus", d.batteryStatus);
        p.put("ramAvailableBytes", d.ramAvailable);
        p.put("ramTotalBytes", d.ramTotal);
        p.put("ramAvailablePercent", r.ramAvailablePercent >= 0 ? r.ramAvailablePercent : JSONObject.NULL);
        p.put("storageFreeBytes", d.storageFree);
        p.put("storageTotalBytes", d.storageTotal);
        p.put("storageFreePercent", r.storageFreePercent >= 0 ? r.storageFreePercent : JSONObject.NULL);
        p.put("uptimeMs", d.uptimeMs);
        p.put("networkTransport", r.networkTransport);
        p.put("wifiIpv4", r.wifiIpv4 == null ? JSONObject.NULL : r.wifiIpv4);
        p.put("vpnActive", r.vpnActive);
        p.put("proxyConfigured", r.proxyConfigured);
        p.put("privateDnsMode", r.privateDnsMode == null ? JSONObject.NULL : r.privateDnsMode);
        p.put("screenInteractive", r.screenInteractive);
        p.put("thermalStatus", r.thermalStatus == null ? JSONObject.NULL : r.thermalStatus);
        p.put("thermalLabel", r.thermalLabel == null ? JSONObject.NULL : r.thermalLabel);
        p.put("riskLevel", RuntimeDiagnostics.riskLevel(r, d));
        return envelope("runtime.summary/1", "agent.runtime", p);
    }

    public static JSONObject securityEnvelope(Context context) throws JSONException {
        SecurityDiagnostics.Snapshot s = SecurityDiagnostics.collect(context);
        JSONObject p = new JSONObject();
        p.put("assessmentType", "signals_only");
        p.put("malwareEngine", "not_installed");
        p.put("securityPatch", s.securityPatch);
        p.put("developerOptionsEnabled", s.developerOptionsEnabled);
        p.put("adbEnabled", s.adbEnabled);
        p.put("vpnActive", s.vpnActive);
        p.put("proxyConfigured", s.proxyConfigured);
        p.put("privateDnsMode", s.privateDnsMode == null ? JSONObject.NULL : s.privateDnsMode);
        p.put("riskLevel", s.riskLevel);

        org.json.JSONArray findings = new org.json.JSONArray();
        if (s.findings != null) {
            for (String finding : s.findings) findings.put(finding);
        }
        p.put("findings", findings);
        return envelope("security.status/1", "agent.security_signals", p);
    }

    public static JSONObject heartbeatEnvelope() throws JSONException {
        JSONObject p = new JSONObject();
        p.put("state", AgentSessionManager.get().isActive() ? "active" : "inactive");
        return envelope("session.heartbeat/1", "agent.session", p);
    }

    public static JSONObject sessionStateEnvelope(String state) throws JSONException {
        JSONObject p = new JSONObject();
        p.put("state", state);
        return envelope("session.state/1", "agent.session", p);
    }

    public static JSONObject error(String code, String message) {
        JSONObject o = new JSONObject();
        try {
            o.put("error", code);
            o.put("message", message);
        } catch (JSONException ignored) {}
        return o;
    }

    private static String connectionState(AgentSessionManager.State state) {
        if (state == AgentSessionManager.State.PAIRING) return "waiting_pair_code";
        if (state == AgentSessionManager.State.PAIRED) return "paired";
        if (state == AgentSessionManager.State.ACTIVE) return "connected";
        return "idle";
    }

    private static JSONObject envelope(String schema, String source, JSONObject payload) throws JSONException {
        AgentSessionManager sm = AgentSessionManager.get();
        JSONObject o = new JSONObject();
        o.put("protocol", AgentConstants.PROTOCOL);
        o.put("schema", schema);
        o.put("sessionId", valueOrNull(sm.getSessionId()));
        o.put("seq", sm.nextSeq());
        o.put("capturedAtEpochMs", System.currentTimeMillis());
        o.put("deviceElapsedRealtimeMs", SystemClock.elapsedRealtime());
        o.put("source", source);
        o.put("payload", payload);
        return o;
    }

    private static JSONObject capability(String state, String reason, String permission) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("state", state);
        if (reason != null) o.put("reason", reason);
        if (permission != null) {
            org.json.JSONArray arr = new org.json.JSONArray();
            arr.put(permission);
            o.put("requiredPermissions", arr);
        }
        return o;
    }

    private static JSONObject metricString(String value) throws JSONException {
        if (value == null || value.trim().isEmpty() || "N/D".equals(value)) {
            return unavailable("unsupported", "value_not_exposed");
        }
        JSONObject o = new JSONObject();
        o.put("value", value);
        o.put("status", "ok");
        return o;
    }

    private static JSONObject metricNumber(Number value) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("value", value);
        o.put("status", "ok");
        return o;
    }

    private static JSONObject nullableMetric(Number value, String reason) throws JSONException {
        if (value == null) return unavailable("unsupported", reason);
        return metricNumber(value);
    }

    private static JSONObject unavailable(String status, String reason) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("value", JSONObject.NULL);
        o.put("status", status);
        o.put("reason", reason);
        return o;
    }

    private static Object valueOrNull(String value) {
        return value == null ? JSONObject.NULL : value;
    }
}
