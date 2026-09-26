package com.welltech.mobile;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LocalAgentServer {
    public interface Listener {
        void onStateChanged();
        void onServerError(String message);
    }

    private static final int MAX_HEADER_BYTES = 16 * 1024;
    private static final int MAX_BODY_BYTES = 64 * 1024;
    private static final String WS_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private final Context context;
    private final Listener listener;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService clients = Executors.newCachedThreadPool();
    private ServerSocket serverSocket;
    private Thread acceptThread;

    public LocalAgentServer(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    public synchronized void start() throws IOException {
        if (running.get()) return;
        ServerSocket socket = new ServerSocket();
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), AgentConstants.DEVICE_PORT));
        serverSocket = socket;
        running.set(true);
        acceptThread = new Thread(this::acceptLoop, "welltech-agent-accept");
        acceptThread.start();
    }

    public synchronized void stop() {
        running.set(false);
        if (serverSocket != null) {
            try { serverSocket.close(); } catch (IOException ignored) {}
            serverSocket = null;
        }
        if (acceptThread != null) {
            acceptThread.interrupt();
            acceptThread = null;
        }
        clients.shutdownNow();
    }

    public boolean isRunning() {
        return running.get();
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                clients.execute(() -> handleClient(socket));
            } catch (IOException e) {
                if (running.get() && listener != null) listener.onServerError(e.getMessage());
            }
        }
    }

    private void handleClient(Socket socket) {
        try (Socket s = socket) {
            s.setSoTimeout(15_000);
            BufferedInputStream in = new BufferedInputStream(s.getInputStream());
            BufferedOutputStream out = new BufferedOutputStream(s.getOutputStream());
            Request req = readRequest(in);
            if (req == null) return;

            if (isWebSocketUpgrade(req) && (AgentConstants.API_BASE + "/stream").equals(req.path)) {
                handleWebSocket(s, in, out, req);
                return;
            }

            Response response = route(req);
            writeHttpResponse(out, response.status, response.body);
        } catch (Exception ignored) {
            // Local diagnostic transport: failures are isolated to this connection.
        }
    }

    private Response route(Request req) {
        try {
            if ("GET".equals(req.method) && (AgentConstants.API_BASE + "/health").equals(req.path)) {
                return ok(AgentApiJson.health());
            }

            if ("POST".equals(req.method) && (AgentConstants.API_BASE + "/pair").equals(req.path)) {
                if (!AgentSessionManager.get().isPairingOpen()) {
                    return json(403, AgentApiJson.error("pairing_window_closed", "Abra o pareamento no aparelho antes de conectar."));
                }
                JSONObject body = parseBody(req.body);
                String code = body.optString("code", "");
                AgentSessionManager.PairResult pair = AgentSessionManager.get().pair(code);
                if (pair == null) {
                    return json(403, AgentApiJson.error("pairing_failed", "Código inválido ou janela de pareamento expirada."));
                }
                notifyStateChanged();
                return ok(AgentApiJson.pairingResponse(pair));
            }

            if (!AgentSessionManager.get().authenticateHeader(req.headers.get("authorization"))) {
                return json(401, AgentApiJson.error("unauthorized", "Token de sessão ausente, inválido ou expirado."));
            }

            if ("POST".equals(req.method) && (AgentConstants.API_BASE + "/session/start").equals(req.path)) {
                if (!AgentSessionManager.get().startSession()) {
                    return json(409, AgentApiJson.error("session_not_paired", "Pareamento válido é necessário antes de iniciar a sessão."));
                }
                notifyStateChanged();
                return ok(AgentApiJson.sessionState("active"));
            }

            if ("POST".equals(req.method) && (AgentConstants.API_BASE + "/session/stop").equals(req.path)) {
                JSONObject response = AgentApiJson.sessionState("stopped");
                AgentSessionManager.get().stopSessionAndRevoke();
                notifyStateChanged();
                return ok(response);
            }

            if ("GET".equals(req.method) && (AgentConstants.API_BASE + "/capabilities").equals(req.path)) {
                return ok(AgentApiJson.capabilities(context));
            }

            if (!AgentSessionManager.get().isActive()) {
                return json(409, AgentApiJson.error("session_not_active", "Inicie a sessão antes de solicitar telemetria."));
            }

            if ("GET".equals(req.method) && (AgentConstants.API_BASE + "/device").equals(req.path)) {
                return ok(AgentApiJson.deviceEnvelope(context));
            }

            if ("GET".equals(req.method) && (AgentConstants.API_BASE + "/battery").equals(req.path)) {
                return ok(AgentApiJson.batteryEnvelope(context));
            }

            if ("GET".equals(req.method) && (AgentConstants.API_BASE + "/summary").equals(req.path)) {
                return ok(AgentApiJson.summaryEnvelope(context));
            }

            if ("GET".equals(req.method) && (AgentConstants.API_BASE + "/security").equals(req.path)) {
                return ok(AgentApiJson.securityEnvelope(context));
            }

            return json(404, AgentApiJson.error("not_found", "Endpoint não encontrado nesta versão do Agent."));
        } catch (Exception e) {
            return json(500, AgentApiJson.error("internal_error", "Falha interna ao processar a requisição."));
        }
    }

    private void handleWebSocket(Socket socket, InputStream in, OutputStream out, Request req) throws Exception {
        AgentSessionManager sm = AgentSessionManager.get();
        if (!sm.authenticateHeader(req.headers.get("authorization"))) {
            writeHttpResponse(out, 401, AgentApiJson.error("unauthorized", "Token inválido para o stream.").toString());
            return;
        }
        if (!sm.isActive()) {
            writeHttpResponse(out, 409, AgentApiJson.error("session_not_active", "Inicie a sessão antes de abrir o stream.").toString());
            return;
        }

        String key = req.headers.get("sec-websocket-key");
        if (key == null || key.trim().isEmpty()) {
            writeHttpResponse(out, 400, AgentApiJson.error("bad_websocket_request", "Sec-WebSocket-Key ausente.").toString());
            return;
        }

        String accept = websocketAccept(key.trim());
        String headers = "HTTP/1.1 101 Switching Protocols\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Accept: " + accept + "\r\n\r\n";
        out.write(headers.getBytes(StandardCharsets.US_ASCII));
        out.flush();

        socket.setSoTimeout(0);
        sendWebSocketText(out, AgentApiJson.sessionStateEnvelope("active").toString());
        sendWebSocketText(out, AgentApiJson.deviceEnvelope(context).toString());
        sendWebSocketText(out, AgentApiJson.batteryEnvelope(context).toString());
        sendWebSocketText(out, AgentApiJson.summaryEnvelope(context).toString());
        sendWebSocketText(out, AgentApiJson.securityEnvelope(context).toString());

        int realtimeTick = 0;
        while (running.get() && sm.isActive()) {
            long started = System.currentTimeMillis();
            try {
                sendWebSocketText(out, AgentApiJson.summaryEnvelope(context).toString());
                sendWebSocketText(out, AgentApiJson.batteryEnvelope(context).toString());
                if ((realtimeTick++ % 3) == 0) {
                    sendWebSocketText(out, AgentApiJson.heartbeatEnvelope().toString());
                    sendWebSocketText(out, AgentApiJson.securityEnvelope(context).toString());
                }
            } catch (IOException e) {
                break;
            }

            long elapsed = System.currentTimeMillis() - started;
            long sleep = Math.max(250L, AgentConstants.REALTIME_INTERVAL_MS - elapsed);
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        try { sendWebSocketClose(out); } catch (IOException ignored) {}
    }

    private static Request readRequest(InputStream in) throws IOException {
        String requestLine = readLine(in, MAX_HEADER_BYTES);
        if (requestLine == null || requestLine.trim().isEmpty()) return null;
        String[] parts = requestLine.split(" ");
        if (parts.length < 2) throw new IOException("invalid request line");

        Request req = new Request();
        req.method = parts[0].toUpperCase(Locale.ROOT);
        String rawPath = parts[1];
        int q = rawPath.indexOf('?');
        req.path = q >= 0 ? rawPath.substring(0, q) : rawPath;

        int headerBytes = requestLine.length();
        while (true) {
            String line = readLine(in, MAX_HEADER_BYTES - headerBytes);
            if (line == null || line.isEmpty()) break;
            headerBytes += line.length();
            if (headerBytes > MAX_HEADER_BYTES) throw new IOException("headers too large");
            int colon = line.indexOf(':');
            if (colon > 0) {
                String name = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
                String value = line.substring(colon + 1).trim();
                req.headers.put(name, value);
            }
        }

        int contentLength = 0;
        String length = req.headers.get("content-length");
        if (length != null && !length.isEmpty()) {
            try { contentLength = Integer.parseInt(length); }
            catch (NumberFormatException e) { throw new IOException("invalid content length"); }
        }
        if (contentLength < 0 || contentLength > MAX_BODY_BYTES) throw new IOException("body too large");
        if (contentLength > 0) {
            byte[] body = readExact(in, contentLength);
            req.body = new String(body, StandardCharsets.UTF_8);
        } else {
            req.body = "";
        }
        return req;
    }

    private static String readLine(InputStream in, int maxBytes) throws IOException {
        if (maxBytes <= 0) throw new IOException("line too large");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int previous = -1;
        while (buffer.size() < maxBytes) {
            int b = in.read();
            if (b < 0) {
                if (buffer.size() == 0) return null;
                break;
            }
            if (previous == '\r' && b == '\n') {
                byte[] raw = buffer.toByteArray();
                int len = Math.max(0, raw.length - 1);
                return new String(raw, 0, len, StandardCharsets.UTF_8);
            }
            buffer.write(b);
            previous = b;
        }
        if (buffer.size() >= maxBytes) throw new IOException("line too large");
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private static byte[] readExact(InputStream in, int count) throws IOException {
        byte[] data = new byte[count];
        int offset = 0;
        while (offset < count) {
            int n = in.read(data, offset, count - offset);
            if (n < 0) throw new EOFException("unexpected end of stream");
            offset += n;
        }
        return data;
    }

    private static JSONObject parseBody(String body) throws JSONException {
        if (body == null || body.trim().isEmpty()) return new JSONObject();
        return new JSONObject(body);
    }

    private static boolean isWebSocketUpgrade(Request req) {
        String upgrade = req.headers.get("upgrade");
        String connection = req.headers.get("connection");
        return "GET".equals(req.method) && upgrade != null && "websocket".equalsIgnoreCase(upgrade.trim()) &&
                connection != null && connection.toLowerCase(Locale.ROOT).contains("upgrade");
    }

    private static String websocketAccept(String key) throws Exception {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] digest = sha1.digest((key + WS_GUID).getBytes(StandardCharsets.US_ASCII));
        return Base64.getEncoder().encodeToString(digest);
    }

    private static synchronized void sendWebSocketText(OutputStream out, String text) throws IOException {
        byte[] payload = text.getBytes(StandardCharsets.UTF_8);
        writeWebSocketFrame(out, 0x1, payload);
    }

    private static void sendWebSocketClose(OutputStream out) throws IOException {
        writeWebSocketFrame(out, 0x8, new byte[0]);
    }

    private static void writeWebSocketFrame(OutputStream out, int opcode, byte[] payload) throws IOException {
        out.write(0x80 | (opcode & 0x0F));
        int len = payload.length;
        if (len <= 125) {
            out.write(len);
        } else if (len <= 0xFFFF) {
            out.write(126);
            out.write((len >>> 8) & 0xFF);
            out.write(len & 0xFF);
        } else {
            out.write(127);
            long longLen = len;
            for (int i = 7; i >= 0; i--) out.write((int) ((longLen >>> (8 * i)) & 0xFF));
        }
        out.write(payload);
        out.flush();
    }

    private static void writeHttpResponse(OutputStream out, int status, String body) throws IOException {
        byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        String header = "HTTP/1.1 " + status + " " + reason(status) + "\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Cache-Control: no-store\r\n" +
                "X-Content-Type-Options: nosniff\r\n" +
                "Content-Length: " + bytes.length + "\r\n" +
                "Connection: close\r\n\r\n";
        out.write(header.getBytes(StandardCharsets.US_ASCII));
        out.write(bytes);
        out.flush();
    }

    private static String reason(int status) {
        switch (status) {
            case 200: return "OK";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 409: return "Conflict";
            default: return "Internal Server Error";
        }
    }

    private static Response ok(JSONObject body) {
        return json(200, body);
    }

    private static Response json(int status, JSONObject body) {
        return new Response(status, body == null ? "{}" : body.toString());
    }

    private void notifyStateChanged() {
        if (listener != null) listener.onStateChanged();
    }

    private static final class Request {
        String method;
        String path;
        String body;
        final Map<String, String> headers = new LinkedHashMap<>();
    }

    private static final class Response {
        final int status;
        final String body;

        Response(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }
}
