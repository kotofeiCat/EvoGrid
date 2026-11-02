package com.EvoGrid.battle.dto;

public record WorldSpawnDto(
        Long spawnId,
        Long speciesId,
        String name,
        String element,
        String archetype,
        int currentHp,
        int maxHp,
        int attack,
        int defense,
        int speed,
        double latitude,
        double longitude,
        boolean active
) {}