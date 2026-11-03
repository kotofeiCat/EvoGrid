package com.EvoGrid.world.service;

import com.EvoGrid.world.client.PokemonClient;
import com.EvoGrid.world.dto.PokemonSpeciesDto;
import com.EvoGrid.world.dto.WorldSpawnDto;
import com.EvoGrid.world.model.WorldSpawn;
import com.EvoGrid.world.repository.WorldSpawnRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class WorldSpawnService {

    private static final Logger log = LoggerFactory.getLogger(WorldSpawnService.class);
    private static final double SEARCH_RADIUS_DEG = 0.0012;
    private static final double SPAWN_OFFSET_DEG = 0.00062;
    private static final int MIN_NEARBY_SPAWNS = 6;

    private final WorldSpawnRepository spawnRepository;
    private final PokemonClient pokemonClient;

    public WorldSpawnService(WorldSpawnRepository spawnRepository, PokemonClient pokemonClient) {
        this.spawnRepository = spawnRepository;
        this.pokemonClient = pokemonClient;
    }

    @Transactional
    public List<WorldSpawnDto> getOrGenerateSpawnsNear(double lat, double lon) {
        List<PokemonSpeciesDto> catalog = fetchCatalog();
        Set<String> validNames = catalog.stream()
                .map(PokemonSpeciesDto::name)
                .collect(Collectors.toSet());

        List<WorldSpawn> rawExisting = spawnRepository.findByActiveTrueAndLatitudeBetweenAndLongitudeBetween(
                lat - SEARCH_RADIUS_DEG, lat + SEARCH_RADIUS_DEG,
                lon - SEARCH_RADIUS_DEG, lon + SEARCH_RADIUS_DEG
        );

        List<WorldSpawn> existing = new ArrayList<>();
        for (WorldSpawn sp : rawExisting) {
            if (!validNames.contains(sp.getName())) {
                spawnRepository.delete(sp);
            } else {
                existing.add(sp);
            }
        }

        int missingCount = MIN_NEARBY_SPAWNS - existing.size();
        if (missingCount > 0) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < missingCount; i++) {
                PokemonSpeciesDto s = catalog.get(random.nextInt(catalog.size()));
                double randomLat = lat + random.nextDouble(-SPAWN_OFFSET_DEG, SPAWN_OFFSET_DEG);
                double randomLon = lon + random.nextDouble(-SPAWN_OFFSET_DEG, SPAWN_OFFSET_DEG);
                existing.add(spawnRepository.save(new WorldSpawn(
                        s.id(), s.name(), s.element(), s.archetype(),
                        s.baseHp(), s.baseAttack(), s.baseDefense(), s.baseSpeed(),
                        randomLat, randomLon
                )));
            }
        }
        return existing.stream().map(this::toDto).toList();
    }

    public WorldSpawnDto getSpawnById(Long spawnId) {
        return toDto(findSpawnOrThrow(spawnId));
    }

    @Transactional
    public void deactivateSpawn(Long spawnId) {
        WorldSpawn w = findSpawnOrThrow(spawnId);
        w.setActive(false);
        w.setCurrentHp(0);
    }

    private List<PokemonSpeciesDto> fetchCatalog() {
        try {
            return pokemonClient.getAllSpecies();
        } catch (Exception t) {
            log.warn("pokemon-service недоступен ({}). Используется Fallback!", t.getMessage());
            return List.of(new PokemonSpeciesDto(
                    1L, "Серый грызун", "NEUTRAL", "Баланс",
                    "Резервный покемон", 75, 25, 25, 25, false
            ));
        }
    }

    private WorldSpawn findSpawnOrThrow(Long spawnId) {
        return spawnRepository.findById(spawnId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Спавн не найден: " + spawnId));
    }

    private WorldSpawnDto toDto(WorldSpawn w) {
        return new WorldSpawnDto(
                w.getId(), w.getSpeciesId(), w.getName(), w.getElement(), w.getArchetype(),
                w.getCurrentHp(), w.getMaxHp(), w.getAttack(), w.getDefense(), w.getSpeed(),
                w.getLatitude(), w.getLongitude(), w.isActive()
        );
    }
}