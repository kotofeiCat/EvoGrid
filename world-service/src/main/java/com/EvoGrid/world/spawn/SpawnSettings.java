package com.EvoGrid.world.spawn;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Настройки спавнов (world.spawns.* в config/world-service.yml на Config Server).
 *
 * @param searchRadiusMeters  в каком радиусе от игрока показываем эвонов
 * @param spawnRadiusMeters   в каком радиусе от игрока создаём новых
 * @param minNearby           сколько эвонов должно быть рядом
 * @param lifetime            сколько живёт спавн, потом исчезает
 * @param reservation         на сколько спавн бронируется под начатый бой
 */
@ConfigurationProperties("world.spawns")
public record SpawnSettings(
        @DefaultValue("130") double searchRadiusMeters,
        @DefaultValue("70") double spawnRadiusMeters,
        @DefaultValue("6") int minNearby,
        @DefaultValue("15m") Duration lifetime,
        @DefaultValue("5m") Duration reservation
) {}
