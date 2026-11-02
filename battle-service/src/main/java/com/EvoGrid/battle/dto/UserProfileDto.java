package com.EvoGrid.battle.dto;

import java.util.List;

public record UserProfileDto(
        Long id,
        String email,
        int level,
        int experience,
        int pokeballs,
        int activePartySlot,
        double latitude,
        double longitude,
        List<OwnedPokemonDto> party,
        List<OwnedPokemonDto> collection
) {}