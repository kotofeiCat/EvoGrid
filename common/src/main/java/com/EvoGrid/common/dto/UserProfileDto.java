package com.EvoGrid.common.dto;

import java.util.List;

/** Профиль игрока: прогресс, позиция, отряд и вся коллекция. Предметы и монеты — в inventory-service. */
public record UserProfileDto(
        Long id,
        String email,
        int level,
        int experience,
        int activePartySlot,
        double latitude,
        double longitude,
        List<OwnedPokemonDto> party,
        List<OwnedPokemonDto> collection
) {}
