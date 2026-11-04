package com.EvoGrid.user.web;

import com.EvoGrid.common.dto.UserProfileDto;
import com.EvoGrid.user.auth.AuthCookie;
import com.EvoGrid.user.auth.JwtService;
import com.EvoGrid.user.profile.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Регистрация и вход. Эти адреса шлюз пропускает без токена. */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Вход и регистрация", description = "JWT выдаётся в HttpOnly-куке evogrid_jwt")
public class AuthController {

    private final AccountService accounts;
    private final JwtService jwtService;

    @GetMapping("/check-email")
    @Operation(summary = "Занят ли email")
    public Map<String, Boolean> checkEmail(@RequestParam String email) {
        return Map.of("exists", accounts.isEmailTaken(email));
    }

    @PostMapping("/register")
    @Operation(summary = "Регистрация со стартовым эвоном")
    public ResponseEntity<UserProfileDto> register(@Valid @RequestBody RegisterRequest request) {
        UserProfileDto profile = accounts.register(request.email(), request.password(),
                request.starterSpeciesId(), request.latitude(), request.longitude());
        return withAuthCookie(profile);
    }

    @PostMapping("/login")
    @Operation(summary = "Вход по email и паролю")
    public ResponseEntity<UserProfileDto> login(@Valid @RequestBody LoginRequest request) {
        return withAuthCookie(accounts.login(request.email(), request.password()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Выход: удаляет куку с токеном")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, AuthCookie.cleared()).build();
    }

    private ResponseEntity<UserProfileDto> withAuthCookie(UserProfileDto profile) {
        String token = jwtService.issueToken(profile.id(), profile.email());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, AuthCookie.withToken(token)).body(profile);
    }

    public record RegisterRequest(
            @NotBlank(message = "Введите email") @Email(message = "Некорректный email") String email,
            @NotBlank(message = "Введите пароль") @Size(min = 4, message = "Пароль — минимум 4 символа") String password,
            @NotNull(message = "Выберите стартового эвона") Long starterSpeciesId,
            @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @DecimalMin("-180") @DecimalMax("180") Double longitude
    ) {}

    public record LoginRequest(
            @NotBlank(message = "Введите email") String email,
            @NotBlank(message = "Введите пароль") String password
    ) {}
}
