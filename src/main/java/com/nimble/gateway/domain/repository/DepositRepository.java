package com.nimble.gateway.domain.repository;

import com.nimble.gateway.domain.entity.Deposit;
import com.nimble.gateway.domain.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepositRepository {
    
    Deposit save(Deposit deposit);
    Optional<Deposit> findById(UUID id);
    List<Deposit> findByUser(User user);
    List<Deposit> findByUserAndStatus(User user, Deposit.DepositStatus status);
    void deleteById(UUID id);
}
