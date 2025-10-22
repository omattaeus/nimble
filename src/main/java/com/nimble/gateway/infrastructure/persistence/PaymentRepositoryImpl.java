package com.nimble.gateway.infrastructure.persistence;

import com.nimble.gateway.domain.entity.Payment;
import com.nimble.gateway.domain.entity.User;
import com.nimble.gateway.domain.repository.PaymentRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepositoryImpl extends JpaRepository<Payment, UUID>, PaymentRepository {
    
    @Override
    List<Payment> findByPayer(User payer);
    
    @Override
    List<Payment> findByPayerAndStatus(User payer, Payment.PaymentStatus status);
    
    @Override
    Optional<Payment> findByExternalTransactionId(String externalTransactionId);
    
    @Query("SELECT p FROM Payment p WHERE p.payer = :payer ORDER BY p.createdAt DESC")
    List<Payment> findPaymentsByPayer(@Param("payer") User payer);
    
    @Query("SELECT p FROM Payment p WHERE p.charge.recipient = :recipient ORDER BY p.createdAt DESC")
    List<Payment> findPaymentsReceivedByUser(@Param("recipient") User recipient);
}