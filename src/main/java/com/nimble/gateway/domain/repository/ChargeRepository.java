package com.nimble.gateway.domain.repository;

import com.nimble.gateway.domain.entity.Charge;
import com.nimble.gateway.domain.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChargeRepository {
    
    Charge save(Charge charge);
    Optional<Charge> findById(UUID id);
    List<Charge> findByOriginator(User originator);
    List<Charge> findByRecipient(User recipient);
    List<Charge> findByOriginatorAndStatus(User originator, Charge.ChargeStatus status);
    List<Charge> findByRecipientAndStatus(User recipient, Charge.ChargeStatus status);
    void deleteById(UUID id);
}