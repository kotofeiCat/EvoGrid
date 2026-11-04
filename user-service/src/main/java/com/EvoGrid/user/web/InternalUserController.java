package com.EvoGrid.user.web;

import com.EvoGrid.common.dto.BattleRewardRequest;
import com.EvoGrid.common.dto.UserProfileDto;
import com.EvoGrid.user.profile.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Адреса только для других сервисов (battle-service).
 * Снаружи недоступны: в шлюзе нет маршрута на /internal/**.
 */
@RestController
@RequestMapping("/internal/users/{userId}")
@RequiredArgsConstructor
@Tag(name = "Внутреннее API", description = "Вызывается battle-service, через шлюз недоступно")
public class InternalUserController {

    private final ProfileService profiles;

    @GetMapping
    @Operation(summary = "Профиль игрока для начала боя")
    public UserProfileDto profile(@PathVariable Long userId) {
        return profiles.getProfile(userId);
    }

    @PostMapping("/rewards")
    @Operation(summary = "Начислить итог боя")
    public UserProfileDto applyReward(@PathVariable Long userId, @RequestBody BattleRewardRequest reward) {
        return profiles.applyBattleReward(userId, reward);
    }
}
