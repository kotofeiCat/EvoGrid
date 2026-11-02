package com.EvoGrid.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class LoggingGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getPath();

        log.info(">>> [GATEWAY] TraceID={} | {} {}", traceId, method, path);

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-Trace-Id", traceId)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .then(Mono.fromRunnable(() ->
                        log.info("<<< [GATEWAY] TraceID={} | Status={}",
                                traceId, exchange.getResponse().getStatusCode())
                ));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}