package com.nimble.gateway.domain.repository;

import com.nimble.gateway.domain.entity.Charge;
import com.nimble.gateway.domain.entity.Payment;
import com.nimble.gateway.domain.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {
    
    Payment save(Payment payment);
    Optional<Payment> findById(UUID id);
    List<Payment> findByPayer(User payer);
    List<Payment> findByPayerAndStatus(User payer, Payment.PaymentStatus status);
    Optional<Payment> findByExternalTransactionId(String externalTransactionId);
    Optional<Payment> findByCharge(Charge charge);
    void deleteById(UUID id);
}