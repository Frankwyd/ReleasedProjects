package com.trading.pnl.websocket;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketInterceptor implements ChannelInterceptor {

    private final WebSocketSessionManager sessionManager;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                log.info("Client connecting: {}", accessor.getSessionId());
                sessionManager.addSession(accessor.getSessionId());
            } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                log.info("Client disconnecting: {}", accessor.getSessionId());
                sessionManager.removeSession(accessor.getSessionId());
            }
        }

        return message;
    }
}