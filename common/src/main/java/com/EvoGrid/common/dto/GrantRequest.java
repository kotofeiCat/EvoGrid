package com.EvoGrid.common.dto;

import java.util.Map;

/**
 * Начислить монеты и/или предметы (награда за бой, перенос старых покеболов).
 *
 * @param operationId уникальный id операции: повторное начисление с тем же id игнорируется
 * @param items       код предмета → количество
 */
public record GrantRequest(String operationId, int coins, Map<String, Integer> items) {

    public static GrantRequest coins(String operationId, int coins) {
        return new GrantRequest(operationId, coins, Map.of());
    }
}
