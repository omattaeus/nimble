package com.nimble.gateway.infrastructure.security;

import com.nimble.gateway.domain.entity.User;
import com.nimble.gateway.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);
        
        Optional<User> user = userRepository.findByEmail(username);
        
        if (user.isEmpty() && username.matches("\\d{11}")) user = userRepository.findByCpf(username);
        
        User foundUser = user.orElseThrow(() -> {
            log.warn("User not found with username: {}", username);
            return new UsernameNotFoundException("User not found: " + username);
        });
        
        log.debug("User found: {} with authorities: {}", foundUser.getEmail(), foundUser.getAuthorities());
        return foundUser;
    }
}
