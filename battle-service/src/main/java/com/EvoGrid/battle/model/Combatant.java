package com.EvoGrid.battle.model;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.EvoGrid.battle.model.StatusEffectType.*;

/**
 * Один участник боя (покемон игрока или дикий покемон).
 * Вместо десятков отдельных полей вида playerBurnTurns / enemyBurnTurns
 * все статусы хранятся в одном словаре «эффект → сколько ходов осталось».
 * Хранится в БД как JSON (см. BattleSession).
 */
public class Combatant {

    /** Негативные статусы, которые снимает очищение (CLEANSE). */
    public static final Set<StatusEffectType> DEBUFFS = EnumSet.of(BURN, POISON, SLEEP, STUN, SLOW, BLIND);

    public Long speciesId;
    public String name;
    public String element;
    public String archetype;

    public int maxHp;
    public int hp;
    public int attack;
    public int defense;
    public int speed;

    /** Процент блока от защитной стойки; сбрасывается перед следующим действием бойца. */
    public int blockPercent;

    /** Активные статусы и баффы: эффект → оставшиеся ходы. */
    public Map<StatusEffectType, Integer> effects = new HashMap<>();

    /** Перезарядка способностей: слот → оставшиеся ходы. */
    public Map<Integer, Integer> cooldowns = new HashMap<>();

    public Combatant() {
        // для Jackson
    }

    public Combatant(Long speciesId, String name, String element, String archetype,
                     int maxHp, int attack, int defense, int speed) {
        this.speciesId = speciesId;
        this.name = name;
        this.element = element;
        this.archetype = archetype;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.attack = attack;
        this.defense = defense;
        this.speed = speed;
    }

    // ---------- здоровье ----------

    public boolean alive() {
        return hp > 0;
    }

    public void takeDamage(int amount) {
        hp = Math.max(0, hp - amount);
    }

    /** @return сколько HP реально восстановлено */
    public int heal(int amount) {
        int before = hp;
        hp = Math.min(maxHp, hp + amount);
        return hp - before;
    }

    public double hpRatio() {
        return maxHp == 0 ? 0 : (double) hp / maxHp;
    }

    public int percentOfMaxHp(int percent) {
        return Math.max(1, (int) Math.round(maxHp * percent / 100.0));
    }

    // ---------- статусы ----------

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

    /** Пропускает ли боец свой ход (сон или оглушение). */
    public boolean skipsTurn() {
        return has(SLEEP) || has(STUN);
    }

    public int effectiveSpeed() {
        double spd = speed;
        if (has(SLOW)) spd *= 0.65;
        if (has(RAGE_SPEED_PIERCE)) spd *= 2.0;
        return Math.max(1, (int) Math.round(spd));
    }

    // ---------- перезарядки ----------

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

    // ---------- для интерфейса ----------

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
