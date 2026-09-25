package com.welltech.mobile;

import android.os.SystemClock;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class AgentSessionManager {
    public enum State {
        IDLE,
        PAIRING,
        PAIRED,
        ACTIVE
    }

    public static final class PairResult {
        public final String token;
        public final String sessionId;

        PairResult(String token, String sessionId) {
            this.token = token;
            this.sessionId = sessionId;
        }
    }

    private static final AgentSessionManager INSTANCE = new AgentSessionManager();
    private final SecureRandom random = new SecureRandom();
    private final AtomicLong sequence = new AtomicLong(0L);

    private State state = State.IDLE;
    private String pairingCode;
    private long pairingExpiresElapsedMs;
    private String sessionToken;
    private String sessionId;

    private AgentSessionManager() {}

    public static AgentSessionManager get() {
        return INSTANCE;
    }

    public synchronized String openPairingWindow() {
        revokeLocked();
        int value = random.nextInt(1_000_000);
        pairingCode = String.format(Locale.US, "%06d", value);
        pairingExpiresElapsedMs = SystemClock.elapsedRealtime() + AgentConstants.PAIRING_WINDOW_MS;
        state = State.PAIRING;
        return pairingCode;
    }

    public synchronized PairResult pair(String suppliedCode) {
        refreshExpiryLocked();
        if (state != State.PAIRING || pairingCode == null || suppliedCode == null) {
            return null;
        }
        if (!constantTimeEquals(pairingCode, suppliedCode.trim())) {
            return null;
        }

        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        sessionToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        sessionId = UUID.randomUUID().toString();
        pairingCode = null;
        pairingExpiresElapsedMs = 0L;
        sequence.set(0L);
        state = State.PAIRED;
        return new PairResult(sessionToken, sessionId);
    }

    public synchronized boolean startSession() {
        refreshExpiryLocked();
        if (state == State.PAIRED || state == State.ACTIVE) {
            state = State.ACTIVE;
            return true;
        }
        return false;
    }

    public synchronized void stopSessionAndRevoke() {
        revokeLocked();
    }

    public synchronized void revoke() {
        revokeLocked();
    }

    public synchronized State getState() {
        refreshExpiryLocked();
        return state;
    }

    public synchronized boolean isPairingOpen() {
        refreshExpiryLocked();
        return state == State.PAIRING;
    }

    public synchronized String getPairingCode() {
        refreshExpiryLocked();
        return state == State.PAIRING ? pairingCode : null;
    }

    public synchronized long getPairingRemainingSeconds() {
        refreshExpiryLocked();
        if (state != State.PAIRING) return 0L;
        long remaining = pairingExpiresElapsedMs - SystemClock.elapsedRealtime();
        return Math.max(0L, (remaining + 999L) / 1000L);
    }

    public synchronized boolean authenticateHeader(String authorizationHeader) {
        refreshExpiryLocked();
        if (sessionToken == null || authorizationHeader == null) return false;
        String prefix = "Bearer ";
        if (!authorizationHeader.regionMatches(true, 0, prefix, 0, prefix.length())) return false;
        String supplied = authorizationHeader.substring(prefix.length()).trim();
        return constantTimeEquals(sessionToken, supplied);
    }

    public synchronized boolean isPaired() {
        State s = getState();
        return s == State.PAIRED || s == State.ACTIVE;
    }

    public synchronized boolean isActive() {
        return getState() == State.ACTIVE;
    }

    public synchronized String getSessionId() {
        return sessionId;
    }

    public long nextSeq() {
        return sequence.incrementAndGet();
    }

    private void refreshExpiryLocked() {
        if (state == State.PAIRING && pairingExpiresElapsedMs > 0L &&
                SystemClock.elapsedRealtime() >= pairingExpiresElapsedMs) {
            revokeLocked();
        }
    }

    private void revokeLocked() {
        state = State.IDLE;
        pairingCode = null;
        pairingExpiresElapsedMs = 0L;
        sessionToken = null;
        sessionId = null;
        sequence.set(0L);
    }

    private static boolean constantTimeEquals(String expected, String supplied) {
        if (expected == null || supplied == null) return false;
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8));
    }
}
