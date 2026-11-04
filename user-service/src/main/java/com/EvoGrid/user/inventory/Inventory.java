package com.EvoGrid.user.inventory;

import com.EvoGrid.common.dto.ConsumeItemRequest;
import com.EvoGrid.common.dto.GrantRequest;
import com.EvoGrid.common.dto.ItemDto;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** inventory-service под Circuit Breaker: описание предметов, списание и начисление. */
@Slf4j
@Component
@RequiredArgsConstructor
public class Inventory {

    private final InventoryClient client;

    /** Каталог меняется только с новой версией сервиса, поэтому кэшируем его навсегда. */
    private final Map<String, ItemDto> catalog = new ConcurrentHashMap<>();

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "unavailable")
    public ItemDto item(String code) {
        if (catalog.isEmpty()) {
            client.catalog().forEach(item -> catalog.put(item.code(), item));
        }
        ItemDto item = catalog.get(code);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Такого предмета нет: " + code);
        }
        return item;
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "consumeFailed")
    public ItemDto consume(Long userId, String operationId, String itemCode) {
        return client.consume(userId, new ConsumeItemRequest(operationId, itemCode));
    }

    /** Без fallback: если сервис недоступен, вызывающий (доставка сфер) просто повторит позже. */
    @CircuitBreaker(name = "inventoryService")
    public void grantSpheres(Long userId, String operationId, int amount) {
        client.grant(userId, new GrantRequest(operationId, 0, Map.of("SPHERE_BASIC", amount)));
    }

    private ItemDto unavailable(String code, Throwable cause) {
        if (cause instanceof ResponseStatusException e) throw e;
        log.warn("Circuit Breaker inventoryService: каталог недоступен ({})", cause.toString());
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Инвентарь временно недоступен");
    }

    private ItemDto consumeFailed(Long userId, String operationId, String itemCode, Throwable cause) {
        if (cause instanceof FeignException.Conflict) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Этот предмет закончился");
        }
        log.warn("Circuit Breaker inventoryService: предмет {} не списан ({})", itemCode, cause.toString());
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Инвентарь временно недоступен");
    }
}
