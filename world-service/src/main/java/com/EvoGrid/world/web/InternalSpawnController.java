package com.EvoGrid.world.web;

import com.EvoGrid.common.dto.WorldSpawnDto;
import com.EvoGrid.world.spawn.SpawnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/spawns/{spawnId}")
@RequiredArgsConstructor
@Tag(name = "Внутреннее API", description = "Вызывается battle-service, через шлюз недоступно")
public class InternalSpawnController {

    private final SpawnService spawns;

    @GetMapping
    @Operation(summary = "Спавн по ID")
    public WorldSpawnDto get(@PathVariable Long spawnId) {
        return spawns.get(spawnId);
    }

    @PostMapping("/reserve")
    @Operation(summary = "Забронировать эвона под бой")
    public WorldSpawnDto reserve(@PathVariable Long spawnId, @RequestParam Long userId) {
        return spawns.reserve(spawnId, userId);
    }

    @DeleteMapping
    @Operation(summary = "Убрать эвона с карты")
    public void remove(@PathVariable Long spawnId) {
        spawns.remove(spawnId);
    }
}
