package com.EvoGrid.user.inventory;

import com.EvoGrid.common.dto.ConsumeItemRequest;
import com.EvoGrid.common.dto.GrantRequest;
import com.EvoGrid.common.dto.InventoryDto;
import com.EvoGrid.common.dto.ItemDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "inventory-service")
public interface InventoryClient {

    @GetMapping("/api/shop")
    List<ItemDto> catalog();

    @PostMapping("/internal/inventory/{userId}/consume")
    ItemDto consume(@PathVariable Long userId, @RequestBody ConsumeItemRequest request);

    @PostMapping("/internal/inventory/{userId}/grant")
    InventoryDto grant(@PathVariable Long userId, @RequestBody GrantRequest request);
}
