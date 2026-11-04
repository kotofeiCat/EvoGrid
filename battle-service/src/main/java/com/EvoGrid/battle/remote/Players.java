package com.EvoGrid.battle.remote;

import com.EvoGrid.common.dto.BattleRewardRequest;
import com.EvoGrid.common.dto.UserProfileDto;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** user-service под Circuit Breaker: профиль игрока и награды за бой. */
@Slf4j
@Component
@RequiredArgsConstructor
public class Players {

    private final UserClient userClient;

    @CircuitBreaker(name = "userService", fallbackMethod = "profileUnavailable")
    public UserProfileDto profile(Long userId) {
        return userClient.getProfile(userId);
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "rewardLost")
    public void reward(Long userId, BattleRewardRequest reward) {
        userClient.applyReward(userId, reward);
    }

    private UserProfileDto profileUnavailable(Long userId, Throwable cause) {
        if (cause instanceof FeignException.NotFound) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Игрок не найден");
        }
        log.warn("Circuit Breaker userService: профиль {} недоступен ({})", userId, cause.toString());
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Сервис профилей недоступен, попробуйте позже");
    }

    private void rewardLost(Long userId, BattleRewardRequest reward, Throwable cause) {
        log.error("Circuit Breaker userService: награда игроку {} не начислена ({}): {}", userId, cause.toString(), reward);
    }
}
