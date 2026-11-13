package com.EvoGrid.inventory.repository;

import com.EvoGrid.inventory.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
}
