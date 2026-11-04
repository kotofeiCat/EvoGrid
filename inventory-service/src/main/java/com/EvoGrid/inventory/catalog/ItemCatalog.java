package com.EvoGrid.inventory.catalog;

import com.EvoGrid.common.dto.ItemDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ассортимент магазина из data/items.json. Чтобы добавить предмет или поменять цену,
 * достаточно поправить JSON — код трогать не нужно.
 */
@Component
public class ItemCatalog {

    private static final String FILE = "data/items.json";

    private final Map<String, ItemDto> byCode = new LinkedHashMap<>();

    public ItemCatalog(ObjectMapper objectMapper) throws IOException {
        try (InputStream in = new ClassPathResource(FILE).getInputStream()) {
            List<ItemDto> items = objectMapper.readValue(in, new TypeReference<>() {});
            for (ItemDto item : items) {
                if (item.price() <= 0 || item.kind() == null) {
                    throw new IllegalStateException(FILE + ": у предмета " + item.code() + " нет цены или вида");
                }
                if (byCode.put(item.code(), item) != null) {
                    throw new IllegalStateException(FILE + ": код " + item.code() + " повторяется");
                }
            }
        }
    }

    public List<ItemDto> all() {
        return List.copyOf(byCode.values());
    }

    public ItemDto get(String code) {
        ItemDto item = byCode.get(code);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Такого предмета нет в магазине: " + code);
        }
        return item;
    }

    public boolean exists(String code) {
        return byCode.containsKey(code);
    }
}
