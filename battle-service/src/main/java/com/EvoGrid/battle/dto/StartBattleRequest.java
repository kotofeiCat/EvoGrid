package com.EvoGrid.battle.dto;

public record StartBattleRequest(
        Long userId,
        Long spawnId,
        Integer partySlot
) {}