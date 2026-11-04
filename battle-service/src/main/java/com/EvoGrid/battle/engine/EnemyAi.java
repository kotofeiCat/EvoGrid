package com.EvoGrid.battle.engine;

import com.EvoGrid.battle.session.BattleSession;
import com.EvoGrid.common.dto.AbilityDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

import static com.EvoGrid.common.game.StatusEffectType.*;

/** Выбор способности дикого эвона: простое дерево решений по приоритетам. */
@Component
@RequiredArgsConstructor
public class EnemyAi {

    private final BattleEngine engine;

    public AbilityDto chooseAbility(BattleSession b) {
        Combatant me = b.enemy();
        Combatant foe = b.player();
        AbilityDto basic = me.ability(1);
        AbilityDto special = me.ability(2);
        AbilityDto defense = me.ability(3);
        AbilityDto utility = me.ability(4);

        boolean specialReady = me.cooldown(2) == 0;
        boolean defenseReady = me.cooldown(3) == 0;
        boolean utilityReady = me.cooldown(4) == 0;
        double hp = me.hpRatio();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // 1. Добить, если хватает урона
        if (specialReady && engine.estimateDamage(me, foe, special) >= foe.hp) return special;
        if (engine.estimateDamage(me, foe, basic) >= foe.hp) return basic;

        // 2. Лечение или барьер при низком HP, очищение под дебаффами
        if (utilityReady) {
            boolean healsOrShields = utility.healPercentMaxHp() > 0 || utility.primaryEffect() == SHELL_BARRIER;
            boolean cleanses = utility.primaryEffect() == CLEANSE || utility.secondaryEffect() == CLEANSE;
            boolean debuffed = me.has(BURN) || me.has(POISON) || me.has(BLIND);
            if (healsOrShields && hp <= 0.45) return utility;
            if (cleanses && debuffed && hp <= 0.70) return utility;
        }

        // 3. Защита, если игрок усилен или бьёт по слабой стихии
        if (defenseReady && hp <= 0.65) {
            boolean foeBuffed = foe.has(ATK_UP) || foe.has(GUARANTEED_CRIT) || foe.has(WATER_BOOST) || foe.has(RAGE_SPEED_PIERCE);
            boolean foeHasAdvantage = BattleEngine.elementMultiplier(foe.element, me.element) > 1.0;
            if ((foeBuffed || foeHasAdvantage) && random.nextInt(100) < 65) return defense;
        }

        // 4. Бафф или контроль, пока здоровья достаточно
        if (utilityReady && hp > 0.50 && utility.healPercentMaxHp() == 0) {
            boolean alreadyBuffed = me.has(ATK_UP) || me.has(RAGE_SPEED_PIERCE) || me.has(GUARANTEED_CRIT) || me.has(SHELL_BARRIER);
            if (!alreadyBuffed && random.nextInt(100) < 75) return utility;
        }

        // 5. Особая атака, иначе обычная
        return specialReady ? special : basic;
    }
}
