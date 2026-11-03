package com.EvoGrid.battle.dto;

import com.EvoGrid.battle.model.AbilityCategory;
import com.EvoGrid.battle.model.StatusEffectType;

public record AbilityDto(
        int slot,                           // 1, 2, 3, 4
        String code,                        // Уникальный системный код
        String name,                        // Отображаемое название
        String description,                 // Краткое описание эффекта для UI
        AbilityCategory category,           // BASIC, SPECIAL, DEFENSE, UTILITY
        String element,                     // FIRE, WATER, FOREST, NEUTRAL
        String target,                      // ENEMY или SELF
        int power,                          // Бонус к силе атаки
        int accuracy,                       // Точность в % (обычно 100)
        int maxCooldown,                    // Перезарядка в ходах (0..4)
        int currentCooldown,                // Текущий остаток перезарядки в бою
        boolean stab,                       // Получает ли бонус STAB (x1.25)
        int blockPercent,                   // Процент блока (45..70)
        int evasionChancePercent,           // Шанс уворота (65%)
        int healPercentMaxHp,               // Процент исцеления от MaxHP
        boolean doubleHealLowHp,            // Удвоение лечения при HP < 25% (Синдерхут)
        int selfDamagePercentMaxHp,         // Жертва собственным HP в % (Пихач: 10%)
        StatusEffectType primaryEffect,     // Основной накладываемый статус/бафф
        int primaryEffectChancePercent,     // Шанс срабатывания основного эффекта (0..100)
        int primaryEffectDuration,          // Длительность основного эффекта в ходах
        StatusEffectType secondaryEffect,   // Дополнительный статус/бафф
        int secondaryEffectDuration         // Длительность доп. эффекта
) {
    public AbilityDto withCurrentCooldown(int cd) {
        return new AbilityDto(
                slot, code, name, description, category, element, target,
                power, accuracy, maxCooldown, Math.max(0, cd), stab,
                blockPercent, evasionChancePercent, healPercentMaxHp,
                doubleHealLowHp, selfDamagePercentMaxHp,
                primaryEffect, primaryEffectChancePercent, primaryEffectDuration,
                secondaryEffect, secondaryEffectDuration
        );
    }
}