package com.EvoGrid.battle.client;

import com.EvoGrid.battle.dto.WorldSpawnDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "world-service")
public interface WorldClient {

    @GetMapping("/api/world/spawns/{spawnId}")
    WorldSpawnDto getSpawnById(@PathVariable("spawnId") Long spawnId);

    @DeleteMapping("/api/world/spawns/{spawnId}")
    void deactivateSpawn(@PathVariable("spawnId") Long spawnId);
}