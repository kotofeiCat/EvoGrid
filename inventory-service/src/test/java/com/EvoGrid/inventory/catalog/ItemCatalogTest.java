package com.EvoGrid.inventory.catalog;

import com.EvoGrid.common.dto.ItemDto;
import com.EvoGrid.common.game.ItemKind;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ItemCatalogTest {

    private final ItemCatalog catalog = new ItemCatalog(
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false));

    ItemCatalogTest() throws Exception {
    }

    @Test
    @DisplayName("Каталог читается: шесть сфер, три лечилки, одна «Искра жизни»")
    void catalogLoads() {
        assertThat(catalog.all()).hasSize(10);
        assertThat(catalog.all().stream().filter(i -> i.kind() == ItemKind.CAPTURE)).hasSize(6);
        assertThat(catalog.all().stream().filter(i -> i.kind() == ItemKind.HEAL)).hasSize(3);
        assertThat(catalog.get("REVIVE").revivePercent()).isEqualTo(50);
    }

    @Test
    @DisplayName("Стихийная сфера даёт бонус только против своей стихии")
    void elementalSphere() {
        ItemDto water = catalog.get("SPHERE_WATER");
        assertThat(water.catchMultiplierAgainst("WATER")).isEqualTo(3.0);
        assertThat(water.catchMultiplierAgainst("FIRE")).isEqualTo(1.0);
        assertThat(catalog.get("SPHERE_STAR").catchMultiplierAgainst("FIRE")).isEqualTo(2.0);
    }
}
