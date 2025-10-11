package com.cloudplay.gateway.filter;

import com.cloudplay.common.error.ApiErrorResponse;
import com.cloudplay.common.error.ErrorCode;
import com.cloudplay.gateway.config.GatewayProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Edge authentication filter. Validates the {@code X-API-Key} header against the
 * configured key set, bypassing health/metrics/public paths. On failure it
 * short-circuits with a uniform {@link ApiErrorResponse} 401, so unauthenticated
 * traffic never reaches the backend services.
 *
 * <p>Runs first in the reactive filter chain.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";

    private final GatewayProperties props;
    private final ObjectMapper mapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final Set<String> apiKeys;

    public AuthFilter(GatewayProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
        this.apiKeys = new HashSet<>(props.getAuth().getApiKeys());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (!props.getAuth().isEnabled() || isOpenPath(path)) {
            return chain.filter(exchange);
        }

        String key = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);
        if (key == null || key.isBlank() || !apiKeys.contains(key)) {
            log.debug("Rejected unauthenticated request to {}", path);
            return unauthorized(exchange, path);
        }
        return chain.filter(exchange);
    }

    private boolean isOpenPath(String path) {
        return props.getAuth().getOpenPaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String path) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        ApiErrorResponse body = new ApiErrorResponse(401, ErrorCode.UNAUTHORIZED,
                "Missing or invalid API key", path);
        try {
            byte[] bytes = mapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return response.setComplete();
        }
    }
}
