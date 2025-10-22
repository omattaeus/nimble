package com.nimble.gateway.infrastructure.persistence;

import com.nimble.gateway.domain.entity.Deposit;
import com.nimble.gateway.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DepositJpaRepository extends JpaRepository<Deposit, UUID> {
    
    List<Deposit> findByUser(User user);
    List<Deposit> findByUserAndStatus(User user, Deposit.DepositStatus status);
}
