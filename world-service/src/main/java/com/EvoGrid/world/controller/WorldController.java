package com.EvoGrid.world.controller;

import com.EvoGrid.world.dto.WorldSpawnDto;
import com.EvoGrid.world.service.WorldSpawnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/world")
@Tag(name = "World Geo-Map API", description = "Генерация и поиск диких покемонов по GPS-координатам")
public class WorldController {

    private final WorldSpawnService worldSpawnService;

    public WorldController(WorldSpawnService worldSpawnService) {
        this.worldSpawnService = worldSpawnService;
    }

    @GetMapping("/spawns")
    @Operation(summary = "Получить или сгенерировать покемонов вокруг координат (lat, lon)")
    public List<WorldSpawnDto> getSpawnsNear(
            @RequestParam(defaultValue = "55.7558") double lat,
            @RequestParam(defaultValue = "37.6173") double lon) {
        return worldSpawnService.getOrGenerateSpawnsNear(lat, lon);
    }

    @GetMapping("/spawns/{spawnId}")
    @Operation(summary = "Получить данные конкретного спавна по ID")
    public WorldSpawnDto getSpawnById(@PathVariable Long spawnId) {
        return worldSpawnService.getSpawnById(spawnId);
    }

    @DeleteMapping("/spawns/{spawnId}")
    @Operation(summary = "Убрать покемона с карты после боя или поимки")
    public void deactivateSpawn(@PathVariable Long spawnId) {
        worldSpawnService.deactivateSpawn(spawnId);
    }
}