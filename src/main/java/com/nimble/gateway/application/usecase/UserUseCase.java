package com.nimble.gateway.application.usecase;

import com.nimble.gateway.application.dto.AuthResponseDTO;
import com.nimble.gateway.application.dto.CreateUserDTO;
import com.nimble.gateway.application.dto.LoginDTO;
import com.nimble.gateway.application.dto.UserDTO;
import com.nimble.gateway.domain.entity.User;
import com.nimble.gateway.domain.exception.UserNotFoundException;
import com.nimble.gateway.domain.repository.UserRepository;
import com.nimble.gateway.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserUseCase {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    
    @Transactional
    public UserDTO createUser(CreateUserDTO createUserDTO) {
        log.info("Creating user with email: {}", createUserDTO.getEmail());
        
        if (userRepository.findByEmail(createUserDTO.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User with email already exists");
        }
        
        if (userRepository.findByCpf(createUserDTO.getCpf()).isPresent()) {
            throw new IllegalArgumentException("User with CPF already exists");
        }
        
        User user = User.builder()
                .name(createUserDTO.getName())
                .cpf(createUserDTO.getCpf())
                .email(createUserDTO.getEmail())
                .password(passwordEncoder.encode(createUserDTO.getPassword()))
                .balance(java.math.BigDecimal.ZERO)
                .isActive(true)
                .build();
        
        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getId());
        
        return UserDTO.builder()
                .id(savedUser.getId())
                .name(savedUser.getName())
                .cpf(savedUser.getCpf())
                .email(savedUser.getEmail())
                .balance(savedUser.getBalance())
                .createdAt(savedUser.getCreatedAt())
                .updatedAt(savedUser.getUpdatedAt())
                .isActive(savedUser.getIsActive())
                .build();
    }
    
    public AuthResponseDTO login(LoginDTO loginDTO) {
        log.info("Attempting login for username: {}", loginDTO.getUsername());
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDTO.getUsername(),
                            loginDTO.getPassword()
                    )
            );
            
            String token = jwtTokenProvider.generateToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(authentication.getName());
            
            User user = findUserByUsername(loginDTO.getUsername());
            
            log.info("Login successful for user: {}", user.getEmail());
            
            return AuthResponseDTO.builder()
                    .token(token)
                    .refreshToken(refreshToken)
                    .type("Bearer")
                    .expiresIn(86400000L)
                    .user(UserDTO.builder()
                            .id(user.getId())
                            .name(user.getName())
                            .cpf(user.getCpf())
                            .email(user.getEmail())
                            .balance(user.getBalance())
                            .createdAt(user.getCreatedAt())
                            .updatedAt(user.getUpdatedAt())
                            .isActive(user.getIsActive())
                            .build())
                    .build();
                    
        } catch (org.springframework.security.core.AuthenticationException e) {
            log.error("Login failed for username: {}", loginDTO.getUsername(), e);
            throw new org.springframework.security.authentication.BadCredentialsException("Invalid credentials");
        } catch (Exception e) {
            log.error("Login failed for username: {}", loginDTO.getUsername(), e);
            throw new IllegalArgumentException("Invalid credentials");
        }
    }
    
    public UserDTO getUserById(Long userId) {
        log.info("Getting user by ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        
        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .cpf(user.getCpf())
                .email(user.getEmail())
                .balance(user.getBalance())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .isActive(user.getIsActive())
                .build();
    }
    
    public UserDTO getUserByUsername(String username) {
        log.info("Getting user by username: {}", username);
        
        User user = findUserByUsername(username);
        
        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .cpf(user.getCpf())
                .email(user.getEmail())
                .balance(user.getBalance())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .isActive(user.getIsActive())
                .build();
    }
    
    private User findUserByUsername(String username) {
        Optional<User> user = userRepository.findByEmail(username);
        
        if (user.isEmpty() && username.matches("\\d{11}")) user = userRepository.findByCpf(username);
        
        return user.orElseThrow(() -> new UserNotFoundException("User not found"));
    }
}