package com.EvoGrid.world.dto;

public record PokemonSpeciesDto(
        Long id,
        String name,
        String element,
        String archetype,
        String description,
        int baseHp,
        int baseAttack,
        int baseDefense,
        int baseSpeed,
        boolean starter
) {}