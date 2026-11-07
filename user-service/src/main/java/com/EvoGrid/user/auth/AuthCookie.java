package com.EvoGrid.user.auth;

import org.springframework.http.ResponseCookie;

import java.time.Duration;

public final class AuthCookie {

    public static final String NAME = "evogrid_jwt";

    private AuthCookie() {
    }

    public static String withToken(String token) {
        return build(token, JwtService.TOKEN_LIFETIME);
    }

    public static String cleared() {
        return build("", Duration.ZERO);
    }

    private static String build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build()
                .toString();
    }
}
