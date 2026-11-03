package com.EvoGrid.battle.service;

import com.EvoGrid.battle.dto.AbilityDto;
import com.EvoGrid.battle.model.BattleSession;
import com.EvoGrid.battle.model.Combatant;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.EvoGrid.battle.model.StatusEffectType.*;

/** Выбор способности дикого покемона. Логика та же, что была, но в одном месте. */
@Component
public class EnemyAi {

    private final AbilityCatalog catalog;
    private final BattleEngine engine;

    public EnemyAi(AbilityCatalog catalog, BattleEngine engine) {
        this.catalog = catalog;
        this.engine = engine;
    }

    public AbilityDto chooseAbility(BattleSession b) {
        Combatant me = b.enemy();
        Combatant foe = b.player();
        List<AbilityDto> abilities = catalog.getAbilitiesForPokemon(me.speciesId, me.name);
        AbilityDto basic = abilities.get(0);
        AbilityDto special = abilities.get(1);
        AbilityDto defense = abilities.get(2);
        AbilityDto utility = abilities.get(3);

        boolean specialReady = me.cooldown(2) == 0;
        boolean defenseReady = me.cooldown(3) == 0;
        boolean utilityReady = me.cooldown(4) == 0;
        double hp = me.hpRatio();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        // 1. Добить, если хватает урона
        if (specialReady && engine.estimateDamage(me, foe, special) >= foe.hp) return special;
        if (engine.estimateDamage(me, foe, basic) >= foe.hp) return basic;

        // 2. Лечение / барьер при низком HP, очищение под дебаффами
        if (utilityReady) {
            boolean healOrBarrier = utility.healPercentMaxHp() > 0 || utility.primaryEffect() == SHELL_BARRIER;
            boolean cleanses = utility.primaryEffect() == CLEANSE || utility.secondaryEffect() == CLEANSE;
            boolean debuffed = me.has(BURN) || me.has(POISON) || me.has(BLIND);
            if (healOrBarrier && hp <= 0.45) return utility;
            if (cleanses && debuffed && hp <= 0.70) return utility;
        }

        // 3. Защита, если игрок усилен или имеет преимущество по стихии
        if (defenseReady && hp <= 0.65) {
            boolean foeBuffed = foe.has(ATK_UP) || foe.has(GUARANTEED_CRIT) || foe.has(WATER_BOOST) || foe.has(RAGE_SPEED_PIERCE);
            boolean foeAdvantage = BattleEngine.elementMultiplier(foe.element, me.element) > 1.0;
            if ((foeBuffed || foeAdvantage) && rnd.nextInt(100) < 65) return defense;
        }

        // 4. Бафф / контроль, пока HP комфортное
        if (utilityReady && hp > 0.50 && utility.healPercentMaxHp() == 0) {
            boolean buffed = me.has(ATK_UP) || me.has(RAGE_SPEED_PIERCE) || me.has(GUARANTEED_CRIT) || me.has(SHELL_BARRIER);
            if (!buffed && rnd.nextInt(100) < 75) return utility;
        }

        // 5. Особая атака, иначе обычная
        return specialReady ? special : basic;
    }
}
