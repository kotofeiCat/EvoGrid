package com.EvoGrid.user.controller;

import com.EvoGrid.user.dto.*;
import com.EvoGrid.user.security.JwtService;
import com.EvoGrid.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User & JWT Auth API", description = "JWT-авторизация через HttpOnly Cookie и управление профилем")
public class UserController {

    private static final String JWT_COOKIE = "evogrid_jwt";
    private static final String SLOT_COOKIE = "evogrid_slot";

    private final UserService userService;
    private final JwtService jwtService;

    public UserController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @GetMapping("/check-email")
    @Operation(summary = "Проверка занятости email")
    public Map<String, Boolean> checkEmail(@RequestParam String email) {
        return Map.of("exists", userService.isEmailTaken(email));
    }

    @PostMapping("/register")
    @Operation(summary = "Регистрация по Email и установка HttpOnly JWT-куки")
    public ResponseEntity<UserProfileDto> register(@RequestBody RegisterRequest request) {
        return buildAuthCookieResponse(userService.register(request, 1), 1);
    }

    @PostMapping("/login")
    @Operation(summary = "Вход по Email и установка HttpOnly JWT-куки")
    public ResponseEntity<UserProfileDto> login(
            @RequestBody LoginRequest request,
            @CookieValue(name = SLOT_COOKIE, defaultValue = "1") int activeSlot) {
        return buildAuthCookieResponse(userService.login(request, activeSlot), activeSlot);
    }

    @PostMapping("/logout")
    @Operation(summary = "Очистка HttpOnly кук")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createCookie(JWT_COOKIE, "", 0))
                .header(HttpHeaders.SET_COOKIE, createCookie(SLOT_COOKIE, "", 0))
                .build();
    }

    @GetMapping("/me")
    @Operation(summary = "Текущий профиль по JWT-куке")
    public UserProfileDto getCurrentUser(
            @RequestHeader(name = "X-User-Id", required = false) Long gatewayUserId,
            @CookieValue(name = JWT_COOKIE, required = false) String jwtCookie,
            @CookieValue(name = SLOT_COOKIE, defaultValue = "1") int activeSlot) {
        return userService.getUserProfile(resolveUserId(gatewayUserId, jwtCookie), activeSlot);
    }

    @PutMapping("/active-slot")
    @Operation(summary = "Выбор активного слота (1, 2 или 3)")
    public ResponseEntity<UserProfileDto> selectActiveSlot(
            @RequestParam int slot,
            @RequestHeader(name = "X-User-Id", required = false) Long gatewayUserId,
            @CookieValue(name = JWT_COOKIE, required = false) String jwtCookie) {
        if (slot < 1 || slot > 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Слот должен быть 1, 2 или 3");
        }
        UserProfileDto profile = userService.getUserProfile(resolveUserId(gatewayUserId, jwtCookie), slot);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createCookie(SLOT_COOKIE, String.valueOf(slot), 7))
                .body(profile);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Профиль по ID (для battle-service через OpenFeign)")
    public UserProfileDto getUserProfile(
            @PathVariable Long userId,
            @CookieValue(name = SLOT_COOKIE, defaultValue = "1") int activeSlot) {
        return userService.getUserProfile(userId, activeSlot);
    }

    @PutMapping("/{userId}/location")
    public UserProfileDto updateLocation(
            @PathVariable Long userId,
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestHeader(name = "X-User-Id", required = false) Long gatewayUserId,
            @CookieValue(name = JWT_COOKIE, required = false) String jwtCookie,
            @CookieValue(name = SLOT_COOKIE, defaultValue = "1") int activeSlot) {
        Long authUserId = resolveUserId(gatewayUserId, jwtCookie);
        if (!authUserId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Доступ к чужому профилю запрещен");
        }
        if (Double.isNaN(lat) || Double.isNaN(lon) || lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректные координаты");
        }
        return userService.updateLocation(userId, lat, lon, activeSlot);
    }

    @PutMapping("/{userId}/party")
    public ResponseEntity<UserProfileDto> assignPartySlot(
            @PathVariable Long userId,
            @RequestHeader(name = "X-User-Id", required = false) Long gatewayUserId,
            @CookieValue(name = JWT_COOKIE, required = false) String jwtCookie,
            @RequestBody PartySlotRequest request) {
        Long authUserId = resolveUserId(gatewayUserId, jwtCookie);
        if (!authUserId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Доступ к чужой команде запрещен");
        }
        UserProfileDto profile = userService.assignPartySlot(userId, request, request.slot());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createCookie(SLOT_COOKIE, String.valueOf(request.slot()), 7))
                .body(profile);
    }

    @PostMapping("/{userId}/rewards")
    @Operation(summary = "Начисление наград за бой")
    public UserProfileDto applyBattleReward(
            @PathVariable Long userId,
            @RequestBody BattleRewardRequest request) {
        return userService.applyBattleReward(userId, request);
    }

    private ResponseEntity<UserProfileDto> buildAuthCookieResponse(UserProfileDto profile, int slot) {
        String token = jwtService.generateToken(profile.id(), profile.email());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createCookie(JWT_COOKIE, token, 7))
                .header(HttpHeaders.SET_COOKIE, createCookie(SLOT_COOKIE, String.valueOf(slot), 7))
                .body(profile);
    }

    private String createCookie(String name, String value, long days) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofDays(days))
                .sameSite("Lax")
                .build()
                .toString();
    }

    private Long resolveUserId(Long gatewayUserId, String jwtCookie) {
        if (gatewayUserId != null) {
            return gatewayUserId;
        }
        if (jwtCookie != null && !jwtCookie.isBlank()) {
            try {
                return jwtService.extractUserId(jwtCookie);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Невалидный JWT-токен");
            }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется авторизация");
    }
}