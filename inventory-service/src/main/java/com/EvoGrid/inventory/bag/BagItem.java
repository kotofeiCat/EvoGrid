package com.EvoGrid.inventory.bag;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Сколько штук одного предмета лежит в сумке игрока. */
@Entity
@Table(name = "bag_items", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "item_code"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BagItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String itemCode;

    private int quantity;

    @Version
    private long version;

    public BagItem(Long userId, String itemCode) {
        this.userId = userId;
        this.itemCode = itemCode;
    }

    public void add(int amount) {
        quantity += amount;
    }

    public boolean takeOne() {
        if (quantity <= 0) return false;
        quantity--;
        return true;
    }
}
