package com.EvoGrid.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class JwtAuthGatewayFilter implements GlobalFilter, Ordered {

    static final String USER_ID_HEADER = "X-User-Id";
    private static final String JWT_COOKIE = "evogrid_jwt";

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/users/login",
            "/api/users/register",
            "/api/users/logout",
            "/api/users/check-email",
            "/api/pokemons/starters",
            "/api/config/public"
    );

    private final SecretKey secretKey;

    public JwtAuthGatewayFilter(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> headers.remove(USER_ID_HEADER))
                .build();
        String path = request.getURI().getPath();

        if (!path.startsWith("/api/") || PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange.mutate().request(request).build());
        }

        Long userId = authenticate(request);
        if (userId == null) {
            log.warn("Запрос без действующего токена отклонён: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        ServerHttpRequest withUser = request.mutate()
                .header(USER_ID_HEADER, String.valueOf(userId))
                .build();
        return chain.filter(exchange.mutate().request(withUser).build());
    }

    private Long authenticate(ServerHttpRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return null;
        }
        try {
            Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
            Number userId = claims.get("userId", Number.class);
            return userId == null ? null : userId.longValue();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Недействительный JWT: {}", e.getMessage());
            return null;
        }
    }

    private static String extractToken(ServerHttpRequest request) {
        HttpCookie cookie = request.getCookies().getFirst(JWT_COOKIE);
        if (cookie != null && !cookie.getValue().isBlank()) {
            return cookie.getValue();
        }
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    @Override
    public int getOrder() {
        return -50;
    }
}
