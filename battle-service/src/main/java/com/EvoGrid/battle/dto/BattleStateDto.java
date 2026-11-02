package com.EvoGrid.battle.dto;

import com.EvoGrid.battle.model.BattleStatus;
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
        List<String> combatLog
) {}