package com.EvoGrid.battle.service;

import com.EvoGrid.common.game.Leveling;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LevelingTest {

    @Test
    void statsGrowWithLevel() {
        assertEquals(100, Leveling.scale(100, 1));
        assertEquals(163, Leveling.scale(100, 10));
        assertTrue(Leveling.scale(40, 5) > Leveling.scale(40, 4));
    }

    @Test
    void experienceCarriesOverSeveralLevels() {
        assertArrayEquals(new int[] {2, 10}, Leveling.addExperience(1, 40, 20));
        assertArrayEquals(new int[] {3, 0}, Leveling.addExperience(1, 0, 50 + 70));
    }

    @Test
    void levelStopsAtTen() {
        assertArrayEquals(new int[] {10, 0}, Leveling.addExperience(9, 0, 10_000));
        assertEquals(0, Leveling.xpToNext(10));
    }

    @Test
    void baseStatsSurviveRoundTrip() {
        for (int base = 10; base <= 120; base++) {
            for (int level = 1; level <= 10; level++) {
                assertEquals(base, Leveling.unscale(Leveling.scale(base, level), level));
            }
        }
    }

    @Test
    void enemyMatchesAveragePartyLevel() {
        assertEquals(4, Leveling.averageLevel(List.of(3, 4, 5)));
        assertEquals(1, Leveling.averageLevel(List.of()));
    }
}
