package com.EvoGrid.battle.engine;

import com.EvoGrid.common.dto.ItemDto;
import com.EvoGrid.common.game.ItemKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CatchRulesTest {

    private static final ItemDto BASIC = sphere(1.0, null);
    private static final ItemDto STAR = sphere(2.0, null);
    private static final ItemDto WATER = sphere(3.0, "WATER");

    @Test
    @DisplayName("Чем меньше здоровья у противника, тем выше шанс")
    void lowHpRaisesChance() {
        Combatant otter = new Combatant(6L, "Выдра", "WATER", "", 100, 10, 10, 10, List.of());
        double full = CatchRules.chance(otter, BASIC);
        otter.hp = 10;
        double low = CatchRules.chance(otter, BASIC);
        assertThat(full).isCloseTo(0.25, within(0.001));
        assertThat(low).isGreaterThan(full).isCloseTo(0.79, within(0.001));
    }

    @Test
    @DisplayName("Сфера умножает шанс, стихийная — только против своей стихии, максимум 95%")
    void spheresMultiplyChance() {
        Combatant otter = new Combatant(6L, "Выдра", "WATER", "", 100, 10, 10, 10, List.of());
        Combatant panda = new Combatant(2L, "Панда", "FIRE", "", 100, 10, 10, 10, List.of());
        assertThat(CatchRules.chance(otter, STAR)).isCloseTo(0.50, within(0.001));
        assertThat(CatchRules.chance(otter, WATER)).isCloseTo(0.75, within(0.001));
        assertThat(CatchRules.chance(panda, WATER)).isCloseTo(0.25, within(0.001));
        otter.hp = 1;
        assertThat(CatchRules.chance(otter, WATER)).isEqualTo(0.95);
    }

    private static ItemDto sphere(double multiplier, String element) {
        return new ItemDto("S", "Сфера", ItemKind.CAPTURE, "", 100, multiplier, element, 0, false, 0);
    }
}
