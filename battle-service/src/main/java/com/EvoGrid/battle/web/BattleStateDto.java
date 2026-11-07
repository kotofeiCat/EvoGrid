package com.EvoGrid.battle.web;

import com.EvoGrid.battle.session.BattleStatus;
import com.EvoGrid.common.dto.AbilityDto;

import java.util.List;

public record BattleStateDto(
        Long battleId,
        Long userId,
        Long spawnId,
        int turnNumber,
        BattleStatus status,
        String playerPokemonName,
        String playerElement,
        int playerCurrentHp,
        int playerMaxHp,
        int playerAttack,
        int playerDefense,
        int playerSpeed,
        String enemyPokemonName,
        String enemyElement,
        String enemyArchetype,
        int enemyCurrentHp,
        int enemyMaxHp,
        int enemyAttack,
        int enemyDefense,
        int enemySpeed,
        List<String> combatLog,
        int playerEffectiveSpeed,
        int enemyEffectiveSpeed,
        List<String> playerActiveStatuses,
        List<String> enemyActiveStatuses,
        List<AbilityDto> playerAbilities,
        List<AbilityDto> enemyAbilities,
        long turnDeadlineEpochMs,
        int turnSecondsLeft,
        int turnSecondsTotal,
        boolean playerSkipsTurn,
        Long playerSpeciesId,
        Long enemySpeciesId,
        List<Fighter> team,
        int activeIndex
) {
    public record Fighter(
            int index,
            Long ownedPokemonId,
            Long speciesId,
            String name,
            String element,
            int currentHp,
            int maxHp,
            boolean active,
            boolean fainted,
            List<String> statuses
    ) {}
}
