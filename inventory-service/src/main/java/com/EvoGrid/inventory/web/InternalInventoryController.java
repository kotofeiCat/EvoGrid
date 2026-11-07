package com.EvoGrid.inventory.web;

import com.EvoGrid.common.dto.ConsumeItemRequest;
import com.EvoGrid.common.dto.GrantRequest;
import com.EvoGrid.common.dto.InventoryDto;
import com.EvoGrid.common.dto.ItemDto;
import com.EvoGrid.inventory.bag.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Внутреннее API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/inventory/{userId}")
public class InternalInventoryController {

    private final InventoryService inventory;

    @Operation(summary = "Сумка игрока")
    @GetMapping
    public InventoryDto get(@PathVariable Long userId) {
        return inventory.get(userId);
    }

    @Operation(summary = "Списать один предмет (идемпотентно по operationId)")
    @PostMapping("/consume")
    public ItemDto consume(@PathVariable Long userId, @RequestBody ConsumeItemRequest request) {
        return inventory.consume(userId, request.operationId(), request.itemCode());
    }

    @Operation(summary = "Начислить монеты и предметы (идемпотентно по operationId)")
    @PostMapping("/grant")
    public InventoryDto grant(@PathVariable Long userId, @RequestBody GrantRequest request) {
        return inventory.grant(userId, request);
    }
}
