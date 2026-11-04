package com.EvoGrid.pokemon.catalog;

import com.EvoGrid.common.game.AbilityCategory;
import com.EvoGrid.common.game.StatusEffectType;

import java.util.List;

/** Описание вида в файле data/species.json. */
public record SpeciesDefinition(
        long id,
        String name,
        String element,
        String archetype,
        String description,
        Stats stats,
        boolean starter,
        boolean spawnable,
        List<AbilityDefinition> abilities
) {

    public record Stats(int hp, int attack, int defense, int speed) {}

    /** Способность в JSON: необязательные поля можно не писать. */
    public record AbilityDefinition(
            int slot,
            String code,
            String name,
            String description,
            AbilityCategory category,
            String element,
            String target,
            int power,
            Integer accuracy,
            int cooldown,
            boolean stab,
            int blockPercent,
            int evasionChancePercent,
            int healPercentMaxHp,
            boolean doubleHealLowHp,
            int selfDamagePercentMaxHp,
            StatusEffectType effect,
            Integer effectChance,
            int effectTurns,
            StatusEffectType secondEffect,
            int secondEffectTurns
    ) {}
}
