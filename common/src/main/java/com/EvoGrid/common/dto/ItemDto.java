package com.EvoGrid.common.dto;

import com.EvoGrid.common.game.ItemKind;

public record ItemDto(
        String code,
        String name,
        ItemKind kind,
        String description,
        int price,
        double catchMultiplier,
        String catchElement,
        int healHp,
        boolean fullHeal,
        int revivePercent
) {
    public double catchMultiplierAgainst(String element) {
        if (catchElement == null) return catchMultiplier;
        return catchElement.equalsIgnoreCase(element) ? catchMultiplier : 1.0;
    }
}
