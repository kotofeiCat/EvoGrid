package com.EvoGrid.common.dto;

import java.util.Map;

public record GrantRequest(String operationId, int coins, Map<String, Integer> items) {

    public static GrantRequest coins(String operationId, int coins) {
        return new GrantRequest(operationId, coins, Map.of());
    }
}
