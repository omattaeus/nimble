package com.nimble.gateway.infrastructure.persistence;

import com.nimble.gateway.domain.entity.Charge;
import com.nimble.gateway.domain.entity.User;
import com.nimble.gateway.domain.repository.ChargeRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChargeRepositoryImpl extends JpaRepository<Charge, UUID>, ChargeRepository {
    
    @Override
    List<Charge> findByOriginator(User originator);
    
    @Override
    List<Charge> findByRecipient(User recipient);
    
    @Override
    List<Charge> findByOriginatorAndStatus(User originator, Charge.ChargeStatus status);
    
    @Override
    List<Charge> findByRecipientAndStatus(User recipient, Charge.ChargeStatus status);
    
    @Query("SELECT c FROM Charge c WHERE c.originator = :originator ORDER BY c.createdAt DESC")
    List<Charge> findChargesCreatedByUser(@Param("originator") User originator);
    
    @Query("SELECT c FROM Charge c WHERE c.recipient = :recipient ORDER BY c.createdAt DESC")
    List<Charge> findChargesReceivedByUser(@Param("recipient") User recipient);
}