package com.nimble.gateway.infrastructure.persistence;

import com.nimble.gateway.domain.entity.User;
import com.nimble.gateway.domain.repository.UserRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepositoryImpl extends JpaRepository<User, Long>, UserRepository {
    
    @Override
    Optional<User> findByCpf(String cpf);
    
    @Override
    Optional<User> findByEmail(String email);
    
    @Override
    boolean existsByCpf(String cpf);
    
    @Override
    boolean existsByEmail(String email);
}