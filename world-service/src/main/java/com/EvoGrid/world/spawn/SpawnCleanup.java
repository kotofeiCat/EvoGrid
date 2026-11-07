package com.EvoGrid.world.spawn;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpawnCleanup {

    private final SpawnService spawns;

    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
    public void cleanUp() {
        int removed = spawns.cleanUp();
        if (removed > 0) {
            log.info("Удалено {} устаревших спавнов", removed);
        }
    }
}
