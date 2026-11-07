package com.EvoGrid.inventory.bag;

import com.EvoGrid.common.dto.GrantRequest;
import com.EvoGrid.common.dto.InventoryDto;
import com.EvoGrid.common.dto.ItemDto;
import com.EvoGrid.inventory.catalog.ItemCatalog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final WalletRepository wallets;
    private final BagItemRepository bag;
    private final ProcessedOperationRepository operations;
    private final ItemCatalog catalog;
    private final InventorySettings settings;

    @Transactional
    public InventoryDto get(Long userId) {
        walletOf(userId);
        return snapshot(userId);
    }

    @Transactional
    public InventoryDto buy(Long userId, String itemCode, int quantity) {
        if (quantity < 1 || quantity > settings.maxPerPurchase()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Можно купить от 1 до " + settings.maxPerPurchase() + " штук за раз");
        }
        ItemDto item = catalog.get(itemCode);
        walletOf(userId).pay(item.price() * quantity);
        slotOf(userId, itemCode).add(quantity);
        log.info("Игрок {} купил {} × {} за {} монет", userId, quantity, item.name(), item.price() * quantity);
        return snapshot(userId);
    }

    @Transactional
    public ItemDto consume(Long userId, String operationId, String itemCode) {
        ItemDto item = catalog.get(itemCode);
        if (alreadyDone(operationId)) {
            return item;
        }
        walletOf(userId);
        boolean taken = bag.findByUserIdAndItemCode(userId, itemCode)
                .map(BagItem::takeOne)
                .orElse(false);
        if (!taken) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "«" + item.name() + "» закончился");
        }
        remember(operationId, userId);
        log.info("Игрок {} использовал «{}» (операция {})", userId, item.name(), operationId);
        return item;
    }

    @Transactional
    public InventoryDto grant(Long userId, GrantRequest request) {
        if (alreadyDone(request.operationId())) {
            return snapshot(userId);
        }
        if (request.coins() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нельзя начислить отрицательную сумму");
        }
        walletOf(userId).add(request.coins());
        addItems(userId, request.items());
        remember(request.operationId(), userId);
        log.info("Игроку {} начислено: {} монет, предметы {} (операция {})",
                userId, request.coins(), request.items(), request.operationId());
        return snapshot(userId);
    }

    private Wallet walletOf(Long userId) {
        return wallets.findById(userId).orElseGet(() -> {
            Wallet wallet = wallets.save(new Wallet(userId, settings.starterCoins()));
            addItems(userId, settings.starterItems());
            log.info("Игрок {} получил стартовый набор: {} монет и {}", userId, settings.starterCoins(), settings.starterItems());
            return wallet;
        });
    }

    private void addItems(Long userId, Map<String, Integer> items) {
        if (items == null) return;
        items.forEach((code, amount) -> {
            if (amount == null || amount <= 0) return;
            if (!catalog.exists(code)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Такого предмета нет в магазине: " + code);
            }
            slotOf(userId, code).add(amount);
        });
    }

    private BagItem slotOf(Long userId, String itemCode) {
        return bag.findByUserIdAndItemCode(userId, itemCode)
                .orElseGet(() -> bag.save(new BagItem(userId, itemCode)));
    }

    private boolean alreadyDone(String operationId) {
        if (operationId == null || operationId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не указан operationId");
        }
        return operations.existsById(operationId);
    }

    private void remember(String operationId, Long userId) {
        operations.save(new ProcessedOperation(operationId, userId));
    }

    private InventoryDto snapshot(Long userId) {
        Map<String, Integer> counts = new java.util.HashMap<>();
        bag.findByUserId(userId).forEach(slot -> counts.put(slot.getItemCode(), slot.getQuantity()));
        var items = catalog.all().stream()
                .filter(item -> counts.getOrDefault(item.code(), 0) > 0)
                .map(item -> new InventoryDto.Slot(item, counts.get(item.code())))
                .toList();
        int coins = wallets.findById(userId).map(Wallet::getCoins).orElse(0);
        return new InventoryDto(userId, coins, items);
    }
}
