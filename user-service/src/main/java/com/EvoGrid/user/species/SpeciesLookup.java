package com.EvoGrid.user.species;

import com.EvoGrid.common.dto.PokemonSpeciesDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpeciesLookup {

    private static final PokemonSpeciesDto DEFAULT_STARTER = new PokemonSpeciesDto(
            2L, "Красная панда", "FIRE", "Атакующий", "Стартер по умолчанию",
            70, 35, 20, 25, true, true);

    private final PokemonClient pokemonClient;

    @CircuitBreaker(name = "pokemonService", fallbackMethod = "defaultStarter")
    public PokemonSpeciesDto getSpecies(Long speciesId) {
        return pokemonClient.getById(speciesId);
    }

    @CircuitBreaker(name = "pokemonService", fallbackMethod = "noCatalog")
    public List<PokemonSpeciesDto> getAll() {
        return pokemonClient.getAll();
    }

    private PokemonSpeciesDto defaultStarter(Long speciesId, Throwable cause) {
        log.warn("Circuit Breaker pokemonService: вид {} не получен ({}), выдаём стартера по умолчанию",
                speciesId, cause.toString());
        return DEFAULT_STARTER;
    }

    private List<PokemonSpeciesDto> noCatalog(Throwable cause) {
        log.warn("Circuit Breaker pokemonService: каталог недоступен ({})", cause.toString());
        return List.of();
    }
}
