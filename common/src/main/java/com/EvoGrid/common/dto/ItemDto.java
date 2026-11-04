package com.EvoGrid.common.dto;

import com.EvoGrid.common.game.ItemKind;

/**
 * Предмет магазина. Каталог лежит в inventory-service (items.json).
 *
 * @param catchMultiplier во сколько раз сфера повышает шанс поимки (для CAPTURE)
 * @param catchElement    стихия, против которой действует бонус сферы; null — против всех
 * @param healHp          сколько HP восстанавливает (для HEAL)
 * @param fullHeal        восстанавливает всё здоровье и снимает негативные статусы
 * @param revivePercent   с каким процентом здоровья поднимает (для REVIVE)
 */
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
    /** Множитель шанса поимки против эвона данной стихии. */
    public double catchMultiplierAgainst(String element) {
        if (catchElement == null) return catchMultiplier;
        return catchElement.equalsIgnoreCase(element) ? catchMultiplier : 1.0;
    }
}
