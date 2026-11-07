package com.EvoGrid.battle.engine;

import com.EvoGrid.battle.session.BattleSession;
import com.EvoGrid.common.dto.AbilityDto;
import com.EvoGrid.common.game.AbilityCategory;
import com.EvoGrid.common.game.StatusEffectType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static com.EvoGrid.common.game.StatusEffectType.*;
import static org.assertj.core.api.Assertions.assertThat;

class BattleEngineTest {

    private static final Random NO_LUCK = new Random() {
        @Override
        public int nextInt(int bound) {
            return bound - 1;
        }

        @Override
        public double nextDouble() {
            return 0.0;
        }

        @Override
        public boolean nextBoolean() {
            return true;
        }
    };

    private final BattleEngine engine = new BattleEngine(() -> NO_LUCK);

    @Test
    @DisplayName("Треугольник стихий: огонь > лес > вода > огонь (x1.3 / x0.77)")
    void elementTriangle() {
        assertThat(BattleEngine.elementMultiplier("FIRE", "FOREST")).isEqualTo(1.3);
        assertThat(BattleEngine.elementMultiplier("FOREST", "WATER")).isEqualTo(1.3);
        assertThat(BattleEngine.elementMultiplier("WATER", "FIRE")).isEqualTo(1.3);
        assertThat(BattleEngine.elementMultiplier("FOREST", "FIRE")).isEqualTo(0.77);
        assertThat(BattleEngine.elementMultiplier("NEUTRAL", "FIRE")).isEqualTo(1.0);
        assertThat(BattleEngine.elementMultiplier("FIRE", "FIRE")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Атака своей стихии (STAB) бьёт сильнее нейтральной")
    void stabDealsMoreDamage() {
        Combatant attacker = fighter("Панда", "FIRE", 25);
        Combatant target = fighter("Грызун", "NEUTRAL", 25);
        int neutral = engine.estimateDamage(attacker, target, attack(1, "NEUTRAL", 16, 0, NONE, 0));
        int stab = engine.estimateDamage(attacker, target, attack(2, "FIRE", 16, 2, NONE, 0));
        assertThat(stab).isGreaterThan(neutral);
    }

    @Test
    @DisplayName("Первым бьёт тот, кто быстрее")
    void fasterActsFirst() {
        Combatant fast = fighter("Быстрый", "NEUTRAL", 40);
        Combatant slow = fighter("Медленный", "NEUTRAL", 10);
        BattleSession battle = new BattleSession(1L, 1L, List.of(fast), 0, slow, "старт", null);

        engine.playRound(battle, attack(1, "NEUTRAL", 10, 0, NONE, 0), attack(1, "NEUTRAL", 10, 0, NONE, 0));

        List<String> log = battle.logLines();
        assertThat(log.get(2)).startsWith("Быстрый");
        assertThat(log.get(3)).startsWith("Дикий Медленный");
    }

    @Test
    @DisplayName("Спящий боец пропускает ход, сон после этого проходит")
    void sleepSkipsTurn() {
        Combatant player = fighter("Игрок", "NEUTRAL", 40);
        Combatant enemy = fighter("Враг", "NEUTRAL", 10);
        player.setEffect(SLEEP, 1);
        BattleSession battle = new BattleSession(1L, 1L, List.of(player), 0, enemy, "старт", null);
        int enemyHpBefore = enemy.hp;

        engine.playRound(battle, attack(1, "NEUTRAL", 10, 0, NONE, 0), attack(1, "NEUTRAL", 10, 0, NONE, 0));

        assertThat(enemy.hp).isEqualTo(enemyHpBefore);
        assertThat(player.has(SLEEP)).isFalse();
        assertThat(String.join("\n", battle.logLines())).contains("Игрок крепко спит");
    }

    @Test
    @DisplayName("Способность уходит на перезарядку и откатывается по ходам")
    void cooldownTicksDown() {
        Combatant player = fighter("Игрок", "FIRE", 40);
        Combatant enemy = fighter("Враг", "NEUTRAL", 10);
        BattleSession battle = new BattleSession(1L, 1L, List.of(player), 0, enemy, "старт", null);
        AbilityDto special = attack(2, "FIRE", 16, 2, NONE, 0);
        AbilityDto basic = attack(1, "NEUTRAL", 10, 0, NONE, 0);

        engine.playRound(battle, special, basic);
        assertThat(player.cooldown(2)).isEqualTo(2);

        engine.playRound(battle, basic, basic);
        assertThat(player.cooldown(2)).isEqualTo(1);

        engine.playRound(battle, basic, basic);
        assertThat(player.cooldown(2)).isZero();
    }

    @Test
    @DisplayName("Горение наносит 8% максимального HP в конце хода")
    void burnTicksAtEndOfRound() {
        Combatant player = fighter("Игрок", "FIRE", 40);
        Combatant enemy = fighter("Враг", "NEUTRAL", 10);
        BattleSession battle = new BattleSession(1L, 1L, List.of(player), 0, enemy, "старт", null);

        engine.playRound(battle, attack(2, "FIRE", 16, 2, BURN, 2), defense(3));

        assertThat(enemy.has(BURN)).isTrue();
        assertThat(String.join("\n", battle.logLines())).contains("Дикий Враг горит: −8 HP");
    }

    @Test
    @DisplayName("Блок снижает урон")
    void blockReducesDamage() {
        Combatant attacker = fighter("Игрок", "NEUTRAL", 10);
        Combatant blocker = fighter("Враг", "NEUTRAL", 40);
        BattleSession battle = new BattleSession(1L, 1L, List.of(attacker), 0, blocker, "старт", null);
        AbilityDto hit = attack(1, "NEUTRAL", 10, 0, NONE, 0);
        int fullDamage = engine.estimateDamage(attacker, blocker, hit);

        engine.playRound(battle, hit, defense(3));

        assertThat(blocker.maxHp - blocker.hp).isLessThan(fullDamage);
    }

    @Test
    @DisplayName("Защита снижает урон, но не обнуляет его")
    void defenseReducesDamage() {
        Combatant attacker = new Combatant(1L, "А", "NEUTRAL", "", 100, 35, 20, 20, List.of());
        Combatant soft = new Combatant(2L, "Мягкий", "NEUTRAL", "", 100, 20, 10, 20, List.of());
        Combatant hard = new Combatant(3L, "Твёрдый", "NEUTRAL", "", 100, 20, 40, 20, List.of());
        AbilityDto hit = attack(1, "NEUTRAL", 10, 0, NONE, 0);
        int vsSoft = engine.estimateDamage(attacker, soft, hit);
        int vsHard = engine.estimateDamage(attacker, hard, hit);
        assertThat(vsSoft).isGreaterThan(vsHard);
        assertThat(vsHard).isGreaterThan(10);
    }

    @Test
    @DisplayName("Даже самый сильный удар по слабой стихии без крита не убивает с одного раза")
    void noOneShots() {
        Combatant strong = new Combatant(1L, "Сильный", "FIRE", "", 85, 35, 25, 5, List.of());
        Combatant weak = new Combatant(2L, "Хрупкий", "FOREST", "", 65, 30, 20, 35, List.of());
        AbilityDto strongest = attack(2, "FIRE", 26, 2, NONE, 0);
        int maxDamage = (int) Math.round(engine.estimateDamage(strong, weak, strongest) * (1 + BattleEngine.DAMAGE_SPREAD));
        assertThat(maxDamage).isLessThan(weak.maxHp);
    }

    private static Combatant fighter(String name, String element, int speed) {
        return new Combatant(1L, name, element, "Тест", 100, 30, 20, speed, List.of());
    }

    private static AbilityDto attack(int slot, String element, int power, int cooldown, StatusEffectType effect, int turns) {
        return new AbilityDto(slot, "ATTACK_" + slot, "Удар " + slot, "", slot == 1 ? AbilityCategory.BASIC : AbilityCategory.SPECIAL,
                element, "ENEMY", power, 100, cooldown, 0, false, 0, 0, 0, false, 0,
                effect, 100, turns, NONE, 0);
    }

    private static AbilityDto defense(int slot) {
        return new AbilityDto(slot, "BLOCK", "Блок", "", AbilityCategory.DEFENSE,
                "NEUTRAL", "SELF", 0, 100, 3, 0, false, 50, 0, 0, false, 0,
                NONE, 100, 0, NONE, 0);
    }
}
