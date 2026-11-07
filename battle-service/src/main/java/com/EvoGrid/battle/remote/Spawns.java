package com.EvoGrid.battle.remote;

import com.EvoGrid.common.dto.WorldSpawnDto;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Component
@RequiredArgsConstructor
public class Spawns {

    private final WorldClient worldClient;

    @CircuitBreaker(name = "worldService", fallbackMethod = "reserveFailed")
    public WorldSpawnDto reserve(Long spawnId, Long userId) {
        return worldClient.reserve(spawnId, userId);
    }

    @CircuitBreaker(name = "worldService", fallbackMethod = "removeFailed")
    public void remove(Long spawnId) {
        worldClient.remove(spawnId);
    }

    private WorldSpawnDto reserveFailed(Long spawnId, Long userId, Throwable cause) {
        if (cause instanceof FeignException.NotFound) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Этот эвон уже исчез с карты");
        }
        if (cause instanceof FeignException.Conflict) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "С этим эвоном уже сражается другой игрок");
        }
        log.warn("Circuit Breaker worldService: спавн {} недоступен ({})", spawnId, cause.toString());
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Сервис карты недоступен, попробуйте позже");
    }

    private void removeFailed(Long spawnId, Throwable cause) {
        log.warn("Circuit Breaker worldService: спавн {} не убран с карты ({})", spawnId, cause.toString());
    }
}
