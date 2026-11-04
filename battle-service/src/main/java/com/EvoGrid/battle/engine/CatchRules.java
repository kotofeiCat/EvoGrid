package com.EvoGrid.battle.engine;

import com.EvoGrid.common.dto.ItemDto;

/** Шанс поимки: чем меньше здоровья у дикого эвона и чем лучше сфера, тем выше шанс. */
public final class CatchRules {

    static final double BASE_CHANCE = 0.25;
    static final double LOW_HP_BONUS = 0.60;
    static final double MAX_CHANCE = 0.95;

    private CatchRules() {
    }

    /** 25% при полном здоровье, до 85% почти без здоровья; сфера умножает шанс, максимум 95%. */
    public static double chance(Combatant enemy, ItemDto sphere) {
        double base = BASE_CHANCE + (1.0 - enemy.hpRatio()) * LOW_HP_BONUS;
        return Math.min(MAX_CHANCE, base * sphere.catchMultiplierAgainst(enemy.element));
    }
}
