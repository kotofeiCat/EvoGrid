package com.EvoGrid.battle.remote;

import com.EvoGrid.common.dto.WorldSpawnDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "world-service", path = "/internal/spawns")
public interface WorldClient {

    @PostMapping("/{spawnId}/reserve")
    WorldSpawnDto reserve(@PathVariable("spawnId") Long spawnId, @RequestParam("userId") Long userId);

    @DeleteMapping("/{spawnId}")
    void remove(@PathVariable("spawnId") Long spawnId);
}
