package com.EvoGrid.pokemon.dto;

import com.EvoGrid.pokemon.model.ElementType;

public record PokemonSpeciesDto(
        Long id,
        String name,
        ElementType element,
        String archetype,
        String description,
        int baseHp,
        int baseAttack,
        int baseDefense,
        int baseSpeed,
        boolean starter
) { }