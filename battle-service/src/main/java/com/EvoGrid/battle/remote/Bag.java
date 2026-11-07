package com.EvoGrid.battle.remote;

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

@Slf4j
@Component
@RequiredArgsConstructor
public class Bag {

    private final InventoryClient client;

    private final Map<String, ItemDto> catalog = new ConcurrentHashMap<>();

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "catalogUnavailable")
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
    public void consume(Long userId, String operationId, String itemCode) {
        client.consume(userId, new ConsumeItemRequest(operationId, itemCode));
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "grantFailed")
    public boolean grantCoins(Long userId, String operationId, int coins) {
        client.grant(userId, GrantRequest.coins(operationId, coins));
        return true;
    }

    private ItemDto catalogUnavailable(String code, Throwable cause) {
        if (cause instanceof ResponseStatusException e) throw e;
        log.warn("Circuit Breaker inventoryService: каталог предметов недоступен ({})", cause.toString());
        throw unavailable();
    }

    private void consumeFailed(Long userId, String operationId, String itemCode, Throwable cause) {
        if (cause instanceof FeignException.Conflict) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Этот предмет закончился");
        }
        log.warn("Circuit Breaker inventoryService: предмет {} не списан ({})", itemCode, cause.toString());
        throw unavailable();
    }

    private boolean grantFailed(Long userId, String operationId, int coins, Throwable cause) {
        log.warn("Circuit Breaker inventoryService: {} монет игроку {} начислим позже ({})", coins, userId, cause.toString());
        return false;
    }

    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Сумка временно недоступна — пока можно сражаться только способностями");
    }
}
