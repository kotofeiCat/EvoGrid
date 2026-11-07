package com.EvoGrid.battle.engine;

import com.EvoGrid.common.dto.AbilityDto;
import com.EvoGrid.common.game.StatusEffectType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.EvoGrid.common.game.StatusEffectType.*;

public class Combatant {

    public static final Set<StatusEffectType> DEBUFFS = EnumSet.of(BURN, POISON, SLEEP, STUN, SLOW, BLIND);

    public Long ownedPokemonId;

    public Long speciesId;
    public String name;
    public String element;
    public String archetype;

    public int maxHp;
    public int hp;
    public int attack;
    public int defense;
    public int speed;

    public int blockPercent;

    public Map<StatusEffectType, Integer> effects = new HashMap<>();

    public Map<Integer, Integer> cooldowns = new HashMap<>();

    public List<AbilityDto> abilities = new ArrayList<>();

    public Combatant() {
    }

    public Combatant(Long speciesId, String name, String element, String archetype,
                     int maxHp, int attack, int defense, int speed, List<AbilityDto> abilities) {
        this(null, speciesId, name, element, archetype, maxHp, maxHp, attack, defense, speed, abilities);
    }

    public Combatant(Long ownedPokemonId, Long speciesId, String name, String element, String archetype,
                     int maxHp, int hp, int attack, int defense, int speed, List<AbilityDto> abilities) {
        this.ownedPokemonId = ownedPokemonId;
        this.abilities = new ArrayList<>(abilities);
        this.speciesId = speciesId;
        this.name = name;
        this.element = element;
        this.archetype = archetype;
        this.maxHp = maxHp;
        this.hp = Math.max(0, Math.min(maxHp, hp));
        this.attack = attack;
        this.defense = defense;
        this.speed = speed;
    }

    public boolean alive() {
        return hp > 0;
    }

    public void takeDamage(int amount) {
        hp = Math.max(0, hp - amount);
    }

    public int heal(int amount) {
        int before = hp;
        hp = Math.min(maxHp, hp + amount);
        return hp - before;
    }

    public int revive(int percent) {
        hp = Math.max(1, maxHp * percent / 100);
        effects.clear();
        cooldowns.clear();
        blockPercent = 0;
        return hp;
    }

    public double hpRatio() {
        return maxHp == 0 ? 0 : (double) hp / maxHp;
    }

    public int percentOfMaxHp(int percent) {
        return Math.max(1, (int) Math.round(maxHp * percent / 100.0));
    }

    public int turns(StatusEffectType type) {
        return effects.getOrDefault(type, 0);
    }

    public boolean has(StatusEffectType type) {
        return turns(type) > 0;
    }

    public void setEffect(StatusEffectType type, int turns) {
        if (turns > 0) {
            effects.put(type, turns);
        } else {
            effects.remove(type);
        }
    }

    public void clearEffect(StatusEffectType type) {
        effects.remove(type);
    }

    public void decrementEffect(StatusEffectType type) {
        setEffect(type, turns(type) - 1);
    }

    public void cleanseDebuffs() {
        DEBUFFS.forEach(effects::remove);
    }

    public boolean skipsTurn() {
        return has(SLEEP) || has(STUN);
    }

    public int effectiveSpeed() {
        double spd = speed;
        if (has(SLOW)) spd *= 0.65;
        if (has(RAGE_SPEED_PIERCE)) spd *= 2.0;
        return Math.max(1, (int) Math.round(spd));
    }

    public AbilityDto ability(int slot) {
        return abilities.stream()
                .filter(a -> a.slot() == slot)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(name + ": нет способности в слоте " + slot));
    }

    public List<AbilityDto> abilitiesWithCooldowns() {
        return abilities.stream().map(a -> a.withCurrentCooldown(cooldown(a.slot()))).toList();
    }

    public int cooldown(int slot) {
        return cooldowns.getOrDefault(slot, 0);
    }

    public void startCooldown(int slot, int turns) {
        if (turns > 0) {
            cooldowns.put(slot, turns);
        }
    }

    public void tickCooldowns() {
        cooldowns.replaceAll((slot, left) -> left - 1);
        cooldowns.values().removeIf(left -> left <= 0);
    }

    public List<String> statusBadges() {
        List<String> list = new ArrayList<>();
        addBadge(list, BURN, "Горение");
        addBadge(list, POISON, "Яд");
        addBadge(list, SLEEP, "Сон");
        addBadge(list, STUN, "Оглушен");
        addBadge(list, SLOW, "Замедлен");
        addBadge(list, BLIND, "Ослеплен");
        if (blockPercent > 0) list.add("Блок " + blockPercent + "%");
        if (has(EVASION)) list.add("Уворот 65%");
        if (has(SHELL_BARRIER)) list.add("Барьер");
        addBadge(list, CRIT_IMMUNE, "Анти-крит");
        addBadge(list, CC_IMMUNE, "Иммунитет");
        addBadge(list, REGEN, "Реген");
        addBadge(list, ATK_UP, "Атака +30%");
        addBadge(list, DEF_UP, "Защита +35%");
        addBadge(list, THORNS, "Шипы");
        addBadge(list, RAGE_SPEED_PIERCE, "Раж SPD x2");
        if (has(NEXT_HIT_BURN)) list.add("Заряд огня");
        if (has(WATER_BOOST)) list.add("Водный заряд +35%");
        if (has(GUARANTEED_CRIT)) list.add("Точный крит");
        return list;
    }

    private void addBadge(List<String> list, StatusEffectType type, String label) {
        if (has(type)) {
            list.add(label + " (" + turns(type) + ")");
        }
    }
}
