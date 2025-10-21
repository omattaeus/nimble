package com.nimble.gateway.domain.repository;

import com.nimble.gateway.domain.entity.User;

import java.util.Optional;

public interface UserRepository {
    
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByCpf(String cpf);
    Optional<User> findByEmail(String email);
    boolean existsByCpf(String cpf);
    boolean existsByEmail(String email);
    void deleteById(Long id);
}