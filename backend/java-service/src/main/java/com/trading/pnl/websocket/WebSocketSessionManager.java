package com.trading.pnl.websocket;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class WebSocketSessionManager {
    private final ConcurrentHashMap<String, String> sessions = new ConcurrentHashMap<>();

    public void addSession(String sessionId) {
        sessions.put(sessionId, sessionId);
        log.info("New WebSocket connection: {} (Total active: {})",
                sessionId, sessions.size());
    }

    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
        log.info("WebSocket connection closed: {} (Total active: {})",
                sessionId, sessions.size());
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }
}