package com.EvoGrid.battle.engine;

import com.EvoGrid.common.dto.ItemDto;

public final class CatchRules {

    static final double BASE_CHANCE = 0.25;
    static final double LOW_HP_BONUS = 0.60;
    static final double MAX_CHANCE = 0.95;

    private CatchRules() {
    }

    public static double chance(Combatant enemy, ItemDto sphere) {
        double base = BASE_CHANCE + (1.0 - enemy.hpRatio()) * LOW_HP_BONUS;
        return Math.min(MAX_CHANCE, base * sphere.catchMultiplierAgainst(enemy.element));
    }
}
