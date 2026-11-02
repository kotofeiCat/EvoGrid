package com.EvoGrid.user.dto;

public record OwnedPokemonDto(
        Long id,
        Long speciesId,
        String name,
        String element,
        String archetype,
        int maxHp,
        int attack,
        int defense,
        int speed,
        Integer partySlot
) {}