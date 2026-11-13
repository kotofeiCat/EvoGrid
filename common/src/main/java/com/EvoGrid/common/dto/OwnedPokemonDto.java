package com.EvoGrid.common.dto;

public record OwnedPokemonDto(
        Long id,
        Long speciesId,
        String name,
        String element,
        String archetype,
        int maxHp,
        int currentHp,
        int attack,
        int defense,
        int speed,
        Integer partySlot,
        int level,
        int experience,
        int experienceToNext
) {
    public boolean fainted() {
        return currentHp <= 0;
    }
}
