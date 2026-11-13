package com.EvoGrid.inventory.repository;

import com.EvoGrid.inventory.model.ProcessedOperation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedOperationRepository extends JpaRepository<ProcessedOperation, String> {
}
