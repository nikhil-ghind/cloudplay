package com.cloudplay.signaling.config;

import com.cloudplay.signaling.handler.SignalingWebSocketHandler;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * Pulls the {@code sessionId} from the WebSocket handshake query string and puts
 * it into the session attributes before the handler runs. Rejects handshakes
 * without a sessionId.
 */
public class SessionHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Map<String, List<String>> params = UriComponentsBuilder
                .fromUri(request.getURI()).build().getQueryParams();
        List<String> sessionIds = params.get("sessionId");
        if (sessionIds == null || sessionIds.isEmpty() || sessionIds.get(0).isBlank()) {
            response.setStatusCode(org.springframework.http.HttpStatus.BAD_REQUEST);
            return false;
        }
        attributes.put(SignalingWebSocketHandler.ATTR_SESSION_ID, sessionIds.get(0));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
