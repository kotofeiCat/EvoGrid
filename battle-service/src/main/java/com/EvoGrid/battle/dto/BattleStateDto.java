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
        List<String> combatLog,
        int playerEffectiveSpeed,
        int enemyEffectiveSpeed,
        List<String> playerActiveStatuses,
        List<String> enemyActiveStatuses,
        List<AbilityDto> playerAbilities,
        List<AbilityDto> enemyAbilities
) {
    // Конструктор обратной совместимости со старым форматом (21 параметр)
    public BattleStateDto(
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
    ) {
        this(
                battleId, userId, spawnId, turnNumber, status,
                playerPokemonName, playerElement, playerCurrentHp, playerMaxHp,
                playerAttack, playerDefense, playerSpeed,
                enemyPokemonName, enemyElement, enemyArchetype,
                enemyCurrentHp, enemyMaxHp, enemyAttack, enemyDefense, enemySpeed,
                combatLog,
                playerSpeed, enemySpeed,
                List.of(), List.of(),
                List.of(), List.of()
        );
    }
}