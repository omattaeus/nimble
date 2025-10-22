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
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_charge", columnList = "charge_id"),
    @Index(name = "idx_payment_payer", columnList = "payer_id"),
    @Index(name = "idx_payment_status", columnList = "status"),
    @Index(name = "idx_payment_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charge_id", nullable = true)
    private Charge charge;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", nullable = false)
    private User payer;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;
    
    @Column(name = "external_transaction_id", length = 100)
    private String externalTransactionId;
    
    @Column(name = "card_number", length = 19)
    private String cardNumber;
    
    @Column(name = "card_expiry", length = 7)
    private String cardExpiry;
    
    @Column(name = "card_cvv", length = 4)
    private String cardCvv;
    
    @Column(name = "authorization_code", length = 50)
    private String authorizationCode;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "payment_date", nullable = false)
    @Builder.Default
    private LocalDateTime paymentDate = LocalDateTime.now();
    
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
    
    public void markAsProcessed(String authorizationCode) {
        if (this.status != PaymentStatus.PENDING) throw new IllegalStateException("Only pending payments can be processed");
        this.status = PaymentStatus.PROCESSED;
        this.authorizationCode = authorizationCode;
        this.processedAt = LocalDateTime.now();
    }
    
    public void markAsFailed(String reason) {
        if (this.status != PaymentStatus.PENDING) throw new IllegalStateException("Only pending payments can be marked as failed");
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.failedAt = LocalDateTime.now();
    }
    
    public void markAsRefunded() {
        if (this.status != PaymentStatus.PROCESSED) throw new IllegalStateException("Only processed payments can be refunded");
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isProcessed() {
        return this.status == PaymentStatus.PROCESSED;
    }
    
    public boolean isFailed() {
        return this.status == PaymentStatus.FAILED;
    }
    
    public boolean isPending() {
        return this.status == PaymentStatus.PENDING;
    }
    
    public boolean isRefunded() {
        return this.status == PaymentStatus.REFUNDED;
    }
    
    public enum PaymentStatus {
        PENDING, PROCESSED, FAILED, REFUNDED
    }
    
    public enum PaymentMethod {
        BALANCE, CREDIT_CARD
    }
}