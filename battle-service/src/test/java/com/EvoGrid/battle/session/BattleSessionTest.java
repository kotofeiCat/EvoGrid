package com.EvoGrid.battle.session;

import com.EvoGrid.battle.engine.Combatant;
import com.EvoGrid.common.dto.AbilityDto;
import com.EvoGrid.common.game.AbilityCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static com.EvoGrid.common.game.StatusEffectType.BURN;
import static com.EvoGrid.common.game.StatusEffectType.NONE;
import static org.assertj.core.api.Assertions.assertThat;

class BattleSessionTest {

    private static final AbilityDto CLAW = new AbilityDto(1, "HOT_CLAW", "Раскалённый коготь", "", AbilityCategory.BASIC,
            "NEUTRAL", "ENEMY", 10, 100, 0, 0, false, 0, 0, 0, false, 0, NONE, 100, 0, NONE, 0);

    @Test
    @DisplayName("Состояние отряда и противника сохраняется в JSON и восстанавливается")
    void teamSurvivesJsonRoundTrip() throws Exception {
        Combatant panda = new Combatant(11L, 2L, "Красная панда", "FIRE", "Атакующий", 70, 42, 35, 20, 25, List.of(CLAW));
        panda.setEffect(BURN, 2);
        panda.startCooldown(2, 3);
        Combatant otter = new Combatant(12L, 6L, "Речная выдра", "WATER", "Танк", 85, 0, 20, 35, 10, List.of(CLAW));
        Combatant enemy = new Combatant(6L, "Речная выдра", "WATER", "Танк", 85, 20, 35, 10, List.of(CLAW));

        BattleSession battle = new BattleSession(7L, 99L, List.of(panda, otter), 0, enemy, "старт", null);
        battle.syncState();
        forget(battle, "team");
        forget(battle, "enemy");

        Combatant restored = battle.player();
        assertThat(restored).isNotSameAs(panda);
        assertThat(restored.ownedPokemonId).isEqualTo(11L);
        assertThat(restored.hp).isEqualTo(42);
        assertThat(restored.turns(BURN)).isEqualTo(2);
        assertThat(restored.cooldown(2)).isEqualTo(3);
        assertThat(restored.ability(1).name()).isEqualTo("Раскалённый коготь");
        assertThat(battle.team()).hasSize(2);
        assertThat(battle.team().get(1).alive()).isFalse();
        assertThat(battle.enemy().maxHp).isEqualTo(85);
    }

    @Test
    @DisplayName("Следующий живой боец: павшие пропускаются, -1 если живых нет")
    void nextAliveSkipsFainted() {
        Combatant a = new Combatant(1L, 1L, "A", "FIRE", "", 50, 0, 10, 10, 10, List.of(CLAW));
        Combatant b = new Combatant(2L, 1L, "B", "FIRE", "", 50, 0, 10, 10, 10, List.of(CLAW));
        Combatant c = new Combatant(3L, 1L, "C", "FIRE", "", 50, 20, 10, 10, 10, List.of(CLAW));
        Combatant enemy = new Combatant(1L, "E", "FIRE", "", 50, 10, 10, 10, List.of(CLAW));
        BattleSession battle = new BattleSession(1L, 1L, List.of(a, b, c), 0, enemy, "старт", null);

        assertThat(battle.nextAliveIndex()).isEqualTo(2);
        battle.switchTo(2);
        assertThat(battle.player().name).isEqualTo("C");
        assertThat(battle.nextAliveIndex()).isEqualTo(-1);
        assertThat(battle.teamDefeated()).isFalse();

        c.takeDamage(100);
        assertThat(battle.teamDefeated()).isTrue();
    }

    @Test
    @DisplayName("Бой из версии без отрядов читается как отряд из одного бойца")
    void legacyBattleWithoutTeam() throws Exception {
        Combatant enemy = new Combatant(6L, "E", "WATER", "", 85, 20, 35, 10, List.of(CLAW));
        BattleSession battle = new BattleSession(7L, 99L, List.of(enemy), 0, enemy, "старт", null);
        battle.syncState();
        Field teamJson = BattleSession.class.getDeclaredField("teamJson");
        teamJson.setAccessible(true);
        Field playerJson = BattleSession.class.getDeclaredField("playerJson");
        playerJson.setAccessible(true);
        playerJson.set(battle, teamJson.get(battle).toString().replaceAll("^\\[|\\]$", ""));
        teamJson.set(battle, null);
        forget(battle, "team");

        assertThat(battle.team()).hasSize(1);
        assertThat(battle.player().name).isEqualTo("E");
    }

    /** Сбрасывает закэшированное состояние, чтобы оно прочиталось из JSON заново. */
    private static void forget(BattleSession battle, String field) throws Exception {
        Field f = BattleSession.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(battle, null);
    }
}
