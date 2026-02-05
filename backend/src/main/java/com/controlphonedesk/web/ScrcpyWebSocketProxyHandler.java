package com.controlphonedesk.web;

import com.controlphonedesk.AppProperties;
import com.controlphonedesk.adb.AdbService;
import com.controlphonedesk.rbac.seed.DefaultPermissions;
import com.controlphonedesk.rbac.service.UserDeviceScopeService;
import com.controlphonedesk.scrcpy.ScrcpyService;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

@Component
public class ScrcpyWebSocketProxyHandler extends BinaryWebSocketHandler {
    private static final String ATTR_REMOTE = "remote";
    private static final String ATTR_UDID = "udid";
    private static final String ATTR_PORT = "localPort";

    private final ScrcpyService scrcpyService;
    private final AdbService adbService;
    private final AppProperties properties;
    private final UserDeviceScopeService userDeviceScopeService;
    private final HttpClient httpClient;

    public ScrcpyWebSocketProxyHandler(
        ScrcpyService scrcpyService,
        AdbService adbService,
        AppProperties properties,
        UserDeviceScopeService userDeviceScopeService
    ) {
        this.scrcpyService = scrcpyService;
        this.adbService = adbService;
        this.properties = properties;
        this.userDeviceScopeService = userDeviceScopeService;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    /**
     * 浏览器连接后：确保 scrcpy server 启动 -> 建立 adb 端口转发 -> 连接设备侧 WebSocket。
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 提高二进制消息大小限制，避免视频帧被容器直接丢弃
        session.setBinaryMessageSizeLimit(5 * 1024 * 1024);
        session.setTextMessageSizeLimit(1 * 1024 * 1024);
        String udid = getQueryParam(session, "udid");
        if (udid == null || udid.isBlank()) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        Long uid = getSessionUserId(session);
        if (uid != null && shouldApplyScope(session)) {
            if (!userDeviceScopeService.canAccessDevice(uid, udid)) {
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }
        }
        scrcpyService.ensureServerRunning(udid);
        int localPort = PortUtils.findFreePort();
        adbService.forward(udid, localPort, "tcp:" + properties.getScrcpy().getServerPort());

        URI remoteUri = URI.create("ws://127.0.0.1:" + localPort + "/");
        try {
            WebSocket remote = connectRemoteWithRetry(session, remoteUri);
            session.getAttributes().put(ATTR_REMOTE, remote);
            session.getAttributes().put(ATTR_UDID, udid);
            session.getAttributes().put(ATTR_PORT, localPort);
        } catch (Exception ex) {
            adbService.removeForward(udid, localPort);
            session.close(CloseStatus.SERVER_ERROR);
            throw ex;
        }
    }

    /**
     * 浏览器发来的二进制数据（控制指令等）转发到设备 WebSocket。
     */
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        WebSocket remote = getRemote(session);
        if (remote == null) {
            session.close(CloseStatus.SERVER_ERROR);
            return;
        }
        ByteBuffer source = message.getPayload();
        ByteBuffer payload = ByteBuffer.allocate(source.remaining());
        payload.put(source);
        payload.flip();
        remote.sendBinary(payload, true);
    }

    /**
     * 浏览器发来的文本数据转发到设备 WebSocket（当前主要用于兼容）。
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        WebSocket remote = getRemote(session);
        if (remote == null) {
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (Exception ignored) {
            }
            return;
        }
        remote.sendText(message.getPayload(), true);
    }

    /**
     * 连接关闭时清理远端 WebSocket 和 adb 端口转发。
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        WebSocket remote = getRemote(session);
        if (remote != null) {
            remote.abort();
        }
        cleanupForward(session);
    }

    private WebSocket getRemote(WebSocketSession session) {
        Object remote = session.getAttributes().get(ATTR_REMOTE);
        if (remote instanceof WebSocket) {
            return (WebSocket) remote;
        }
        return null;
    }

    /**
     * 关闭时清理 adb forward，避免端口泄漏。
     */
    private void cleanupForward(WebSocketSession session) {
        String udid = Objects.toString(session.getAttributes().get(ATTR_UDID), null);
        Object portObj = session.getAttributes().get(ATTR_PORT);
        if (udid == null || portObj == null) {
            return;
        }
        int port = (int) portObj;
        adbService.removeForward(udid, port);
    }

    /**
     * 从 WebSocket URL 查询参数获取 udid 等值。
     */
    private static String getQueryParam(WebSocketSession session, String name) {
        if (session.getUri() == null || session.getUri().getQuery() == null) {
            return null;
        }
        String query = session.getUri().getQuery();
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=");
            if (parts.length == 2 && parts[0].equals(name)) {
                return java.net.URLDecoder.decode(parts[1], java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private Long getSessionUserId(WebSocketSession session) {
        Object uid = session.getAttributes().get("uid");
        if (uid instanceof Number number) {
            return number.longValue();
        }
        if (uid instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean shouldApplyScope(WebSocketSession session) {
        Object superAdmin = session.getAttributes().get("superAdmin");
        if (Boolean.TRUE.equals(superAdmin)) {
            return false;
        }
        Set<String> permissions = getSessionPermissions(session);
        if (permissions.isEmpty()) {
            return true;
        }
        return !(permissions.contains(DefaultPermissions.DEVICE_UPDATE)
            || permissions.contains(DefaultPermissions.GROUP_CREATE)
            || permissions.contains(DefaultPermissions.GROUP_UPDATE)
            || permissions.contains(DefaultPermissions.GROUP_DELETE));
    }

    @SuppressWarnings("unchecked")
    private Set<String> getSessionPermissions(WebSocketSession session) {
        Object perms = session.getAttributes().get("permissions");
        if (perms instanceof Set<?> set) {
            return (Set<String>) set;
        }
        return Set.of();
    }

    /**
     * 设备侧 WebSocket 监听器：把设备数据转发给浏览器。
     */
    private static class ProxyListener implements WebSocket.Listener {
        private final WebSocketSession session;
        private final AtomicBoolean closed = new AtomicBoolean(false);
        // 处理 WebSocket 分片：remote 可能把一条消息拆成多段发送
        private final java.io.ByteArrayOutputStream binaryBuffer = new java.io.ByteArrayOutputStream();
        private final StringBuilder textBuffer = new StringBuilder();

        private ProxyListener(WebSocketSession session) {
            this.session = session;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            if (closed.get()) {
                return CompletableFuture.completedFuture(null);
            }
            byte[] bytes = new byte[data.remaining()];
            data.get(bytes);
            try {
                // 分片聚合：只有 last==true 才转发给浏览器
                binaryBuffer.write(bytes);
                if (last) {
                    byte[] merged = binaryBuffer.toByteArray();
                    binaryBuffer.reset();
                    synchronized (session) {
                        if (session.isOpen()) {
                            session.sendMessage(new BinaryMessage(merged));
                        }
                    }
                }
            } catch (Exception ex) {
                closeSession();
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            if (closed.get()) {
                return CompletableFuture.completedFuture(null);
            }
            try {
                textBuffer.append(data);
                if (last) {
                    String merged = textBuffer.toString();
                    textBuffer.setLength(0);
                    synchronized (session) {
                        if (session.isOpen()) {
                            session.sendMessage(new TextMessage(merged));
                        }
                    }
                }
            } catch (Exception ex) {
                closeSession();
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            closeSession();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            closeSession();
        }

        /**
         * 安全关闭浏览器会话，避免重复关闭导致异常。
         */
        private void closeSession() {
            if (closed.compareAndSet(false, true)) {
                try {
                    if (session.isOpen()) {
                        session.close(CloseStatus.SERVER_ERROR);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * 连接设备侧 WebSocket，做简单的重试（避免刚启动 server 时握手失败）。
     */
    private WebSocket connectRemoteWithRetry(WebSocketSession session, URI remoteUri) throws Exception {
        int attempts = 0;
        Exception lastError = null;
        while (attempts++ < 5) {
            CompletableFuture<WebSocket> future = httpClient.newWebSocketBuilder()
                .buildAsync(remoteUri, new ProxyListener(session));
            try {
                return future.get(2, TimeUnit.SECONDS);
            } catch (Exception ex) {
                lastError = ex;
                Thread.sleep(200L * attempts);
            }
        }
        throw lastError != null ? lastError : new IOException("Failed to connect remote WebSocket");
    }
}
