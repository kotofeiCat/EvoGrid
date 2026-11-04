package com.EvoGrid.battle.web;

import com.EvoGrid.battle.session.BattleStatus;
import com.EvoGrid.common.dto.AbilityDto;

import java.util.List;

/** Состояние боя для экрана битвы. */
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
        /** Эвон игрока спит или оглушён: атаковать не может, но можно сменить бойца или применить предмет. */
        boolean playerSkipsTurn,
        Long playerSpeciesId,
        Long enemySpeciesId,
        /** Весь отряд игрока: кто на поле, кто в запасе, кто без сил. */
        List<Fighter> team,
        /** Номер бойца на поле в списке team. */
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
