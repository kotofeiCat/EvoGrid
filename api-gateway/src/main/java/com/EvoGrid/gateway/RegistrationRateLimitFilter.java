package com.EvoGrid.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RegistrationRateLimitFilter implements GlobalFilter, Ordered {

    private static final Set<String> PROTECTED_PATHS = Set.of(
            "/api/users/register",
            "/api/users/login",
            "/api/users/check-email"
    );

    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_SECONDS = 60;

    private final Map<String, RequestCounter> limits = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        if (PROTECTED_PATHS.contains(path)) {
            String clientIp = getClientIp(request);
            long now = Instant.now().getEpochSecond();

            String key = clientIp + ":" + path;
            RequestCounter counter = limits.compute(key, (k, existing) -> {
                if (existing == null || now - existing.windowStartTime >= WINDOW_SECONDS) {
                    return new RequestCounter(now, 1);
                }
                existing.count.incrementAndGet();
                return existing;
            });

            if (counter.count.get() > MAX_REQUESTS) {
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                return response.setComplete();
            }
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -200;
    }

    private String getClientIp(ServerHttpRequest request) {
        String xfHeader = request.getHeaders().getFirst("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return "unknown";
    }

    private static class RequestCounter {
        final long windowStartTime;
        final AtomicInteger count;

        RequestCounter(long windowStartTime, int initialCount) {
            this.windowStartTime = windowStartTime;
            this.count = new AtomicInteger(initialCount);
        }
    }
}