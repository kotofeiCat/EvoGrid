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
        Integer partySlot
) {
    public boolean fainted() {
        return currentHp <= 0;
    }
}
