package com.EvoGrid.inventory.bag;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BagItemRepository extends JpaRepository<BagItem, Long> {

    List<BagItem> findByUserId(Long userId);

    Optional<BagItem> findByUserIdAndItemCode(Long userId, String itemCode);
}
