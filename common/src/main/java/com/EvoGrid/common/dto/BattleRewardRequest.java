package com.EvoGrid.common.dto;

import java.util.List;

public record BattleRewardRequest(
        int experience,
        List<FighterHp> partyHp,
        CaughtPokemon caught
) {
    public record FighterHp(Long ownedPokemonId, int hp) {}

    public record CaughtPokemon(
            Long speciesId,
            String name,
            String element,
            String archetype,
            int maxHp,
            int currentHp,
            int attack,
            int defense,
            int speed
    ) {}
}
