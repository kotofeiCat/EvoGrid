package com.EvoGrid.user.dto;

public record RegisterRequest(
        String email,
        String password,
        Long starterSpeciesId,
        Double latitude,
        Double longitude
) {}