package com.nimble.gateway.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "deposits", indexes = {
    @Index(name = "idx_deposit_user", columnList = "user_id"),
    @Index(name = "idx_deposit_status", columnList = "status"),
    @Index(name = "idx_deposit_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Deposit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DepositStatus status = DepositStatus.PENDING;
    
    @Column(name = "external_transaction_id", length = 100)
    private String externalTransactionId;
    
    @Column(name = "authorization_code", length = 50)
    private String authorizationCode;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Column(name = "failed_at")
    private LocalDateTime failedAt;
    
    @Column(name = "failure_reason", length = 500)
    private String failureReason;
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public void markAsApproved(String authorizationCode) {
        if (this.status != DepositStatus.PENDING) {
            throw new IllegalStateException("Only pending deposits can be approved");
        }
        this.status = DepositStatus.APPROVED;
        this.authorizationCode = authorizationCode;
        this.processedAt = LocalDateTime.now();
    }
    
    public void markAsRejected(String reason) {
        if (this.status != DepositStatus.PENDING) {
            throw new IllegalStateException("Only pending deposits can be rejected");
        }
        this.status = DepositStatus.REJECTED;
        this.failureReason = reason;
        this.failedAt = LocalDateTime.now();
    }
    
    public void markAsFailed(String reason) {
        if (this.status != DepositStatus.PENDING) {
            throw new IllegalStateException("Only pending deposits can be marked as failed");
        }
        this.status = DepositStatus.FAILED;
        this.failureReason = reason;
        this.failedAt = LocalDateTime.now();
    }
    
    public boolean isPending() {
        return this.status == DepositStatus.PENDING;
    }
    
    public boolean isApproved() {
        return this.status == DepositStatus.APPROVED;
    }
    
    public boolean isRejected() {
        return this.status == DepositStatus.REJECTED;
    }
    
    public boolean isFailed() {
        return this.status == DepositStatus.FAILED;
    }
    
    public enum DepositStatus {
        PENDING, APPROVED, REJECTED, FAILED
    }
}
