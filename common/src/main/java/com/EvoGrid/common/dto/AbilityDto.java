package com.EvoGrid.common.dto;

import com.EvoGrid.common.game.AbilityCategory;
import com.EvoGrid.common.game.StatusEffectType;

/**
 * Способность эвона. Справочник хранит pokemon-service,
 * battle-service добавляет текущую перезарядку в бою.
 */
public record AbilityDto(
        int slot,
        String code,
        String name,
        String description,
        AbilityCategory category,
        String element,
        String target,
        int power,
        int accuracy,
        int maxCooldown,
        int currentCooldown,
        boolean stab,
        int blockPercent,
        int evasionChancePercent,
        int healPercentMaxHp,
        boolean doubleHealLowHp,
        int selfDamagePercentMaxHp,
        StatusEffectType primaryEffect,
        int primaryEffectChancePercent,
        int primaryEffectDuration,
        StatusEffectType secondaryEffect,
        int secondaryEffectDuration
) {
    public boolean targetsSelf() {
        return "SELF".equalsIgnoreCase(target);
    }

    public AbilityDto withCurrentCooldown(int cooldown) {
        return new AbilityDto(slot, code, name, description, category, element, target,
                power, accuracy, maxCooldown, Math.max(0, cooldown), stab,
                blockPercent, evasionChancePercent, healPercentMaxHp, doubleHealLowHp, selfDamagePercentMaxHp,
                primaryEffect, primaryEffectChancePercent, primaryEffectDuration,
                secondaryEffect, secondaryEffectDuration);
    }
}
