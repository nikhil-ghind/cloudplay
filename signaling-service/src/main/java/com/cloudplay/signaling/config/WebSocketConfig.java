package com.cloudplay.signaling.config;

import com.cloudplay.signaling.handler.SignalingWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Registers the signaling WebSocket endpoint at {@code /signaling}, guarded by
 * {@link SessionHandshakeInterceptor} which requires a {@code sessionId} query
 * parameter.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SignalingWebSocketHandler handler;

    @Value("${cloudplay.signaling.allowed-origins:*}")
    private String[] allowedOrigins;

    public WebSocketConfig(SignalingWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/signaling")
                .addInterceptors(new SessionHandshakeInterceptor())
                .setAllowedOriginPatterns(allowedOrigins);
    }
}
