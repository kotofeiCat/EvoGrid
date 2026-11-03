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
        List<AbilityDto> enemyAbilities,
        /** Момент, до которого нужно выбрать действие (epoch ms), 0 — бой окончен. */
        long turnDeadlineEpochMs,
        /** Сколько секунд осталось на ход (считается сервером, без расхождения часов). */
        int turnSecondsLeft,
        /** Сколько всего секунд даётся на ход. */
        int turnSecondsTotal,
        /** Покемон игрока спит или оглушён: этот ход он пропустит. */
        boolean playerSkipsTurn,
        Long playerSpeciesId,
        Long enemySpeciesId
) {}
