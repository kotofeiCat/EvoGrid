package com.EvoGrid.user.dto;

public record BattleRewardRequest(
        int expGained,
        boolean pokeballUsed,
        boolean captured,
        Long speciesId,
        String name,
        String element,
        String archetype,
        int maxHp,
        int attack,
        int defense,
        int speed
) {}