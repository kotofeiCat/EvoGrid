package com.EvoGrid.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class LoggingGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.currentTimeMillis();
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getPath();

        log.info(">>> {} {}", method, path);

        return chain.filter(exchange)
                .doFinally(signal -> log.info("<<< {} {} -> {} за {} мс",
                        method, path,
                        exchange.getResponse().getStatusCode(),
                        System.currentTimeMillis() - start));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
