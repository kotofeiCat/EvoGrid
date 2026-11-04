package com.EvoGrid.common.dto;

import java.util.List;

/**
 * Итог боя, который battle-service отправляет в user-service.
 *
 * @param partyHp здоровье бойцов отряда после боя — оно сохраняется до следующего боя
 * @param caught  пойманный эвон или null, если никого не поймали
 */
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
