package com.EvoGrid.world.spawn;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("world.spawns")
public record SpawnSettings(
        @DefaultValue("130") double searchRadiusMeters,
        @DefaultValue("70") double spawnRadiusMeters,
        @DefaultValue("6") int minNearby,
        @DefaultValue("15m") Duration lifetime,
        @DefaultValue("5m") Duration reservation
) {}
