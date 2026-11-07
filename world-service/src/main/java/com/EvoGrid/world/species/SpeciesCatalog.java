package com.EvoGrid.world.species;

import com.EvoGrid.common.dto.PokemonSpeciesDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpeciesCatalog {

    private final PokemonClient pokemonClient;
    private final AtomicReference<List<PokemonSpeciesDto>> lastKnown = new AtomicReference<>(List.of());

    @CircuitBreaker(name = "pokemonService", fallbackMethod = "lastKnownSpawnable")
    public List<PokemonSpeciesDto> spawnable() {
        List<PokemonSpeciesDto> fresh = pokemonClient.getAll().stream()
                .filter(PokemonSpeciesDto::spawnable)
                .toList();
        lastKnown.set(fresh);
        return fresh;
    }

    private List<PokemonSpeciesDto> lastKnownSpawnable(Throwable cause) {
        List<PokemonSpeciesDto> cached = lastKnown.get();
        log.warn("Circuit Breaker pokemonService: каталог недоступен ({}), используем сохранённый: {} видов",
                cause.toString(), cached.size());
        return cached;
    }
}
