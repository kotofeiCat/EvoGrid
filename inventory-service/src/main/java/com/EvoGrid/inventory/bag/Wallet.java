package com.EvoGrid.inventory.bag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Кошелёк игрока. Создаётся при первом обращении игрока к инвентарю. */
@Entity
@Table(name = "wallets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet {

    /** id игрока из user-service — внешнего ключа нет, у сервисов разные базы. */
    @Id
    private Long userId;

    private int coins;

    @Version
    private long version;

    public Wallet(Long userId, int coins) {
        this.userId = userId;
        this.coins = coins;
    }

    public void add(int amount) {
        coins += amount;
    }

    public void pay(int amount) {
        if (amount > coins) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Не хватает монет: нужно " + amount + ", у вас " + coins);
        }
        coins -= amount;
    }
}
