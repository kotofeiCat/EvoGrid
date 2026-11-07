package com.EvoGrid.battle.remote;

import com.EvoGrid.common.dto.AbilityDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AbilityBook {

    private final PokemonClient pokemonClient;
    private final Map<Long, List<AbilityDto>> cache = new ConcurrentHashMap<>();

    @CircuitBreaker(name = "pokemonService", fallbackMethod = "fromCacheOnly")
    public List<AbilityDto> forSpecies(Long speciesId) {
        List<AbilityDto> cached = cache.get(speciesId);
        if (cached != null) {
            return cached;
        }
        List<AbilityDto> loaded = List.copyOf(pokemonClient.getAbilities(speciesId));
        cache.put(speciesId, loaded);
        return loaded;
    }

    private List<AbilityDto> fromCacheOnly(Long speciesId, Throwable cause) {
        List<AbilityDto> cached = cache.get(speciesId);
        if (cached != null) {
            return cached;
        }
        log.warn("Circuit Breaker pokemonService: способности вида {} недоступны ({})", speciesId, cause.toString());
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Каталог эвонов недоступен, попробуйте позже");
    }
}
