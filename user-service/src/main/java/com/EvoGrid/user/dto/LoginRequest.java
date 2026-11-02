package com.EvoGrid.user.dto;

public record LoginRequest(
        String email,
        String password
) {}