package com.EvoGrid.inventory.web;

import com.EvoGrid.common.dto.InventoryDto;
import com.EvoGrid.common.dto.ItemDto;
import com.EvoGrid.common.web.CurrentUser;
import com.EvoGrid.inventory.bag.InventoryService;
import com.EvoGrid.inventory.catalog.ItemCatalog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Магазин и сумка")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ShopController {

    private final ItemCatalog catalog;
    private final InventoryService inventory;

    @Operation(summary = "Ассортимент магазина")
    @GetMapping("/shop")
    public List<ItemDto> shop() {
        return catalog.all();
    }

    @Operation(summary = "Купить предмет")
    @PostMapping("/shop/buy")
    public InventoryDto buy(@CurrentUser Long userId, @Valid @RequestBody BuyRequest request) {
        return inventory.buy(userId, request.itemCode(), request.quantity() == null ? 1 : request.quantity());
    }

    @Operation(summary = "Монеты и предметы текущего игрока")
    @GetMapping("/inventory")
    public InventoryDto myInventory(@CurrentUser Long userId) {
        return inventory.get(userId);
    }

    public record BuyRequest(
            @NotBlank(message = "Выберите предмет") String itemCode,
            @Min(value = 1, message = "Количество — минимум 1") Integer quantity
    ) {}
}
