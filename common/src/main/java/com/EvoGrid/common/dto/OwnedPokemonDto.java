package com.EvoGrid.common.dto;

/**
 * Эвон в коллекции игрока. partySlot = 1..3, если он в отряде, иначе null.
 * currentHp сохраняется между боями и понемногу восстанавливается со временем.
 */
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
