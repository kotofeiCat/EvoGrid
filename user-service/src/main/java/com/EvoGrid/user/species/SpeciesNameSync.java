package com.EvoGrid.user.species;

import com.EvoGrid.common.dto.PokemonSpeciesDto;
import com.EvoGrid.user.account.OwnedPokemon;
import com.EvoGrid.user.account.OwnedPokemonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Один раз после старта сверяет названия пойманных эвонов с каталогом pokemon-service:
 * старые записи могли сохраниться под прежними именами.
 * Повторяет попытку раз в минуту, пока каталог не станет доступен.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpeciesNameSync {

    private final SpeciesLookup speciesLookup;
    private final OwnedPokemonRepository ownedRepository;

    private volatile boolean done;

    @Scheduled(initialDelayString = "PT30S", fixedDelayString = "PT1M")
    @Transactional
    public void syncNames() {
        if (done) {
            return;
        }
        List<PokemonSpeciesDto> catalog = speciesLookup.getAll();
        if (catalog.isEmpty()) {
            return;
        }
        int renamed = 0;
        for (PokemonSpeciesDto species : catalog) {
            for (OwnedPokemon pokemon : ownedRepository.findBySpeciesId(species.id())) {
                if (pokemon.renameFrom(species)) {
                    renamed++;
                }
            }
        }
        done = true;
        log.info("Названия пойманных эвонов сверены с каталогом, исправлено: {}", renamed);
    }
}
