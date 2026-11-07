package com.EvoGrid.pokemon.catalog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogSync implements ApplicationRunner {

    private final SpeciesData speciesData;
    private final PokemonSpeciesRepository repository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int created = 0;
        for (SpeciesDefinition definition : speciesData.all()) {
            PokemonSpecies existing = repository.findById(definition.id()).orElse(null);
            if (existing == null) {
                repository.save(new PokemonSpecies(definition));
                created++;
            } else {
                existing.updateFrom(definition);
            }
        }
        log.info("Каталог видов синхронизирован: {} видов, новых {}", speciesData.all().size(), created);
    }
}
