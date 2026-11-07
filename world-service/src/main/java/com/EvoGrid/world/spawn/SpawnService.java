package com.EvoGrid.world.spawn;

import com.EvoGrid.common.dto.PokemonSpeciesDto;
import com.EvoGrid.common.dto.WorldSpawnDto;
import com.EvoGrid.world.species.SpeciesCatalog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpawnService {

    private final WorldSpawnRepository repository;
    private final SpeciesCatalog speciesCatalog;
    private final SpawnSettings settings;

    @Transactional
    public List<WorldSpawnDto> around(double lat, double lon, Long userId) {
        Map<Long, PokemonSpeciesDto> spawnable = speciesCatalog.spawnable().stream()
                .collect(Collectors.toMap(PokemonSpeciesDto::id, Function.identity()));

        GeoArea search = new GeoArea(lat, lon, settings.searchRadiusMeters());
        List<WorldSpawn> nearby = new ArrayList<>(repository.findAliveInArea(
                lat - search.latDelta(), lat + search.latDelta(),
                lon - search.lonDelta(), lon + search.lonDelta(),
                bornAfter()));

        if (!spawnable.isEmpty()) {
            nearby.removeIf(spawn -> {
                boolean outdated = !spawnable.containsKey(spawn.getSpeciesId());
                if (outdated) {
                    spawn.remove();
                }
                return outdated;
            });
        }

        int missing = settings.minNearby() - nearby.size();
        if (missing > 0 && !spawnable.isEmpty()) {
            List<PokemonSpeciesDto> species = List.copyOf(spawnable.values());
            GeoArea spawnArea = new GeoArea(lat, lon, settings.spawnRadiusMeters());
            for (int i = 0; i < missing; i++) {
                PokemonSpeciesDto kind = species.get(ThreadLocalRandom.current().nextInt(species.size()));
                double[] point = spawnArea.randomPoint();
                nearby.add(repository.save(new WorldSpawn(kind, point[0], point[1])));
            }
            log.info("Создано {} новых спавнов около {}, {}", missing, String.format("%.5f", lat), String.format("%.5f", lon));
        }

        Instant now = Instant.now();
        return nearby.stream()
                .filter(spawn -> !spawn.isReservedByOther(userId, now))
                .map(SpawnService::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorldSpawnDto get(Long spawnId) {
        return toDto(findAlive(spawnId));
    }

    @Transactional
    public WorldSpawnDto reserve(Long spawnId, Long userId) {
        WorldSpawn spawn = findAlive(spawnId);
        Instant now = Instant.now();
        if (spawn.isReservedByOther(userId, now)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "С этим эвоном уже сражается другой игрок");
        }
        spawn.reserveFor(userId, now, settings.reservation());
        log.info("Спавн {} ({}) забронирован под бой игроком {}", spawnId, spawn.getName(), userId);
        return toDto(spawn);
    }

    @Transactional
    public void remove(Long spawnId) {
        repository.findById(spawnId).ifPresent(spawn -> {
            spawn.remove();
            log.info("Спавн {} ({}) убран с карты", spawnId, spawn.getName());
        });
    }

    @Transactional
    public int cleanUp() {
        return repository.deleteGoneOrOlderThan(bornAfter());
    }

    private WorldSpawn findAlive(Long spawnId) {
        return repository.findById(spawnId)
                .filter(spawn -> spawn.isActive() && spawn.getSpawnedAt().isAfter(bornAfter()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Этот эвон уже исчез с карты"));
    }

    private LocalDateTime bornAfter() {
        return LocalDateTime.now().minus(settings.lifetime());
    }

    private static WorldSpawnDto toDto(WorldSpawn s) {
        return new WorldSpawnDto(s.getId(), s.getSpeciesId(), s.getName(), s.getElement(), s.getArchetype(),
                s.getMaxHp(), s.getAttack(), s.getDefense(), s.getSpeed(),
                s.getLatitude(), s.getLongitude(), s.isActive());
    }
}
