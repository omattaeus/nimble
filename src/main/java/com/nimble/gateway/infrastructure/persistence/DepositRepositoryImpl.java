package com.nimble.gateway.infrastructure.persistence;

import com.nimble.gateway.domain.entity.Deposit;
import com.nimble.gateway.domain.entity.User;
import com.nimble.gateway.domain.repository.DepositRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DepositRepositoryImpl implements DepositRepository {
    
    private final DepositJpaRepository jpaRepository;
    
    @Override
    public Deposit save(Deposit deposit) {
        return jpaRepository.save(deposit);
    }
    
    @Override
    public Optional<Deposit> findById(UUID id) {
        return jpaRepository.findById(id);
    }
    
    @Override
    public List<Deposit> findByUser(User user) {
        return jpaRepository.findByUser(user);
    }
    
    @Override
    public List<Deposit> findByUserAndStatus(User user, Deposit.DepositStatus status) {
        return jpaRepository.findByUserAndStatus(user, status);
    }
    
    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
