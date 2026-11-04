package com.EvoGrid.battle.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Настройки боя (battle.* в config/battle-service.yml на Config Server).
 *
 * @param turnTime            сколько игрок выбирает действие
 * @param maxDistanceMeters   с какого расстояния можно начать бой (радиус 70 м + запас на погрешность GPS)
 * @param xpForWin            опыт за победу
 * @param xpForCatch          опыт за поимку
 * @param xpForLoss           опыт за поражение
 * @param coinsForWin         монеты за победу
 * @param coinsForCatch       монеты за поимку
 * @param coinsForLoss        монеты за поражение
 */
@ConfigurationProperties("battle")
public record BattleSettings(
        @DefaultValue("30s") Duration turnTime,
        @DefaultValue("100") double maxDistanceMeters,
        @DefaultValue("35") int xpForWin,
        @DefaultValue("50") int xpForCatch,
        @DefaultValue("10") int xpForLoss,
        @DefaultValue("40") int coinsForWin,
        @DefaultValue("25") int coinsForCatch,
        @DefaultValue("10") int coinsForLoss
) {}
