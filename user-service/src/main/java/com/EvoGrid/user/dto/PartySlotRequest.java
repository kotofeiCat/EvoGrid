package com.EvoGrid.user.dto;

public record PartySlotRequest(
        int slot,
        Long ownedPokemonId
) {}