package com.EvoGrid.common.dto;

/**
 * Списать один предмет.
 *
 * @param operationId уникальный id операции: повторный запрос с тем же id ничего не спишет второй раз
 */
public record ConsumeItemRequest(String operationId, String itemCode) {}
