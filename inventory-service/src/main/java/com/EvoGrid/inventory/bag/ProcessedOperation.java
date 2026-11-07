package com.EvoGrid.inventory.bag;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "processed_operations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedOperation {

    @Id
    private String operationId;

    private Long userId;

    private Instant processedAt;

    public ProcessedOperation(String operationId, Long userId) {
        this.operationId = operationId;
        this.userId = userId;
        this.processedAt = Instant.now();
    }
}
