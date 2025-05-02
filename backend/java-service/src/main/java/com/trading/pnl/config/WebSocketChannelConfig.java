package com.trading.pnl.config;

import com.trading.pnl.websocket.WebSocketInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebSocketChannelConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketInterceptor webSocketInterceptor;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketInterceptor);
    }
}