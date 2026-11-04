package com.EvoGrid.common.dto;

import java.util.List;

/** Кошелёк и сумка игрока. */
public record InventoryDto(
        Long userId,
        int coins,
        List<Slot> items
) {
    public record Slot(ItemDto item, int quantity) {}

    public int quantityOf(String itemCode) {
        return items.stream()
                .filter(slot -> slot.item().code().equals(itemCode))
                .mapToInt(Slot::quantity)
                .sum();
    }
}
