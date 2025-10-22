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
@Table(name = "charges", indexes = {
    @Index(name = "idx_charge_originator", columnList = "originator_id"),
    @Index(name = "idx_charge_recipient", columnList = "recipient_id"),
    @Index(name = "idx_charge_status", columnList = "status"),
    @Index(name = "idx_charge_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Charge {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "originator_id", nullable = false)
    private User originator;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(length = 500)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ChargeStatus status = ChargeStatus.PENDING;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.BALANCE;
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public void markAsPaid() {
        if (this.status != ChargeStatus.PENDING) throw new IllegalStateException("Only pending charges can be marked as paid");
        this.status = ChargeStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }
    
    public void cancel() {
        if (this.status == ChargeStatus.CANCELLED) throw new IllegalStateException("Charge is already cancelled");
        this.status = ChargeStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }
    
    public boolean isPending() {
        return this.status == ChargeStatus.PENDING;
    }
    
    public boolean isPaid() {
        return this.status == ChargeStatus.PAID;
    }
    
    public boolean isCancelled() {
        return this.status == ChargeStatus.CANCELLED;
    }
    
    public enum ChargeStatus {
        PENDING, PAID, CANCELLED
    }
    
    public enum PaymentMethod {
        BALANCE, CREDIT_CARD
    }
}