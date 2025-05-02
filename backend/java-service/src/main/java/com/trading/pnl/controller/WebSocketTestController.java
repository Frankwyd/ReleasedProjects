package com.trading.pnl.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/test")
public class WebSocketTestController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private com.trading.pnl.websocket.WebSocketSessionManager sessionManager;

    @GetMapping("/send")
    public String sendTestMessage() {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "test");
        message.put("content", "Test message " + System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/trades", message);
        return "Test message sent!";
    }

    @GetMapping("/status")
    public Map<String, Object> getWebSocketStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("activeSessions", sessionManager.getActiveSessionCount());
        status.put("timestamp", LocalDateTime.now());
        return status;
    }
}