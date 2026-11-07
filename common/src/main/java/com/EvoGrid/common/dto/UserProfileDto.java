package com.EvoGrid.common.dto;

import java.util.List;

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
