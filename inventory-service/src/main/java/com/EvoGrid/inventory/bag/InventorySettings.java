package com.EvoGrid.inventory.bag;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Настройки экономики (config-server: inventory-service.yml).
 *
 * @param starterCoins монеты нового игрока
 * @param starterItems стартовый набор: код предмета → количество
 * @param maxPerPurchase сколько штук можно купить за раз
 */
@ConfigurationProperties("inventory")
public record InventorySettings(Integer starterCoins, Map<String, Integer> starterItems, Integer maxPerPurchase) {

    public InventorySettings {
        if (starterCoins == null) starterCoins = 500;
        if (starterItems == null) starterItems = Map.of("SPHERE_BASIC", 10, "POTION", 3);
        if (maxPerPurchase == null) maxPerPurchase = 99;
    }
}
