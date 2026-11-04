package com.EvoGrid.common.dto;

/** Дикий эвон на карте. */
public record WorldSpawnDto(
        Long spawnId,
        Long speciesId,
        String name,
        String element,
        String archetype,
        int maxHp,
        int attack,
        int defense,
        int speed,
        double latitude,
        double longitude,
        boolean active
) {}
