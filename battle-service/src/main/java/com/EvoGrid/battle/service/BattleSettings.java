package com.EvoGrid.battle.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("battle")
public record BattleSettings(
        @DefaultValue("30s") Duration turnTime,
        @DefaultValue("100") double maxDistanceMeters,
        @DefaultValue("35") int xpForWin,
        @DefaultValue("50") int xpForCatch,
        @DefaultValue("10") int xpForLoss,
        @DefaultValue("60") int coinsForWin,
        @DefaultValue("25") int coinsForCatch,
        @DefaultValue("15") int coinsForLoss
) {}
