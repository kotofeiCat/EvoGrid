package com.EvoGrid.common.game;

import java.util.List;

public final class Leveling {

    public static final int MAX_LEVEL = 10;
    public static final double GROWTH_PER_LEVEL = 0.07;

    private Leveling() {
    }

    public static int clamp(Integer level) {
        if (level == null) return 1;
        return Math.max(1, Math.min(MAX_LEVEL, level));
    }

    public static double multiplier(int level) {
        return 1.0 + GROWTH_PER_LEVEL * (clamp(level) - 1);
    }

    public static int scale(int base, int level) {
        return (int) Math.round(base * multiplier(level));
    }

    public static int unscale(int value, int level) {
        return (int) Math.round(value / multiplier(level));
    }

    public static int xpToNext(int level) {
        return clamp(level) >= MAX_LEVEL ? 0 : 30 + 20 * clamp(level);
    }

    public static int xpForVictory(int enemyLevel) {
        return 20 + 5 * clamp(enemyLevel);
    }

    public static int xpForLoss() {
        return 8;
    }

    public static int[] addExperience(int level, int experience, int gained) {
        level = clamp(level);
        experience = Math.max(0, experience) + Math.max(0, gained);
        while (level < MAX_LEVEL && experience >= xpToNext(level)) {
            experience -= xpToNext(level);
            level++;
        }
        if (level >= MAX_LEVEL) experience = 0;
        return new int[] {level, experience};
    }

    public static int averageLevel(List<Integer> levels) {
        if (levels == null || levels.isEmpty()) return 1;
        double avg = levels.stream().mapToInt(Leveling::clamp).average().orElse(1);
        return clamp((int) Math.round(avg));
    }
}
