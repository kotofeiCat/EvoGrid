package com.EvoGrid.user.inventory;

import com.EvoGrid.user.account.UserAccount;
import com.EvoGrid.user.account.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Доставляет в inventory-service сферы за новые уровни и старые покеболы из прошлой версии игры.
 * Если inventory-service недоступен, ничего не теряется: попытка повторится через 30 секунд.
 * Id операции «spheres-{игрок}-{сколько уже доставлено}» не даёт выдать одни и те же сферы дважды.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SphereDelivery {

    private final UserAccountRepository users;
    private final Inventory inventory;
    private final TransactionTemplate transaction;

    @Scheduled(initialDelay = 20_000, fixedDelay = 30_000)
    public void deliver() {
        for (UserAccount user : users.findTop50ByPendingSpheresGreaterThan(0)) {
            int amount = user.getPendingSpheres();
            String operationId = "spheres-" + user.getId() + "-" + user.getSpheresDelivered();
            try {
                inventory.grantSpheres(user.getId(), operationId, amount);
            } catch (Exception e) {
                log.debug("Сферы игрока {} пока не доставлены: {}", user.getId(), e.toString());
                return;
            }
            transaction.executeWithoutResult(status ->
                    users.markSpheresDelivered(user.getId(), amount, user.getSpheresDelivered()));
            log.info("Игроку {} доставлено {} сфер", user.getId(), amount);
        }
    }
}
