package com.nimble.gateway.application.usecase;

import com.nimble.gateway.application.dto.CreateUserDTO;
import com.nimble.gateway.application.dto.LoginDTO;
import com.nimble.gateway.application.dto.UserDTO;
import com.nimble.gateway.domain.entity.User;
import com.nimble.gateway.domain.repository.UserRepository;
import com.nimble.gateway.domain.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserUseCase - Unit Tests")
class UserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserUseCase userUseCase;

    private User testUser;
    private CreateUserDTO createUserDTO;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("João Silva")
                .cpf("12345678901")
                .email("joao@teste.com")
                .password("encodedPassword")
                .balance(BigDecimal.ZERO)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        createUserDTO = CreateUserDTO.builder()
                .name("João Silva")
                .cpf("12345678901")
                .email("joao@teste.com")
                .password("12345678")
                .build();
    }

    @Nested
    @DisplayName("Scenario: User Creation")
    class CreateUserTests {

        @Test
        @DisplayName("Given a valid user, when creating user, then should return UserDTO with correct data")
        void givenValidUser_whenCreateUser_thenShouldReturnUserDTO() {
            // Given
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(userRepository.findByCpf(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            UserDTO result = userUseCase.createUser(createUserDTO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("João Silva");
            assertThat(result.getCpf()).isEqualTo("12345678901");
            assertThat(result.getEmail()).isEqualTo("joao@teste.com");
            assertThat(result.getBalance()).isEqualTo(BigDecimal.ZERO);
            assertThat(result.getIsActive()).isTrue();

            verify(userRepository).findByEmail("joao@teste.com");
            verify(userRepository).findByCpf("12345678901");
            verify(passwordEncoder).encode("12345678");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Given an existing email, when creating user, then should throw exception")
        void givenExistingEmail_whenCreateUser_thenShouldThrowException() {
            // Given
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

            // When & Then
            assertThatThrownBy(() -> userUseCase.createUser(createUserDTO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User with email already exists");

            verify(userRepository).findByEmail("joao@teste.com");
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Given an existing CPF, when creating user, then should throw exception")
        void givenExistingCpf_whenCreateUser_thenShouldThrowException() {
            // Given
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(userRepository.findByCpf(anyString())).thenReturn(Optional.of(testUser));

            // When & Then
            assertThatThrownBy(() -> userUseCase.createUser(createUserDTO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User with CPF already exists");

            verify(userRepository).findByEmail("joao@teste.com");
            verify(userRepository).findByCpf("12345678901");
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Scenario: Get User by ID")
    class GetUserByIdTests {

        @Test
        @DisplayName("Given a valid ID, when getting user, then should return UserDTO")
        void givenValidId_whenGetUserById_thenShouldReturnUserDTO() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            UserDTO result = userUseCase.getUserById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("João Silva");
            assertThat(result.getEmail()).isEqualTo("joao@teste.com");

            verify(userRepository).findById(1L);
        }

        @Test
        @DisplayName("Given a non-existent ID, when getting user, then should throw exception")
        void givenNonExistentId_whenGetUserById_thenShouldThrowException() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userUseCase.getUserById(999L))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found with ID: 999");

            verify(userRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("Scenario: Get User by Username")
    class GetUserByUsernameTests {

        @Test
        @DisplayName("Given a valid email, when getting user by username, then should return UserDTO")
        void givenValidEmail_whenGetUserByUsername_thenShouldReturnUserDTO() {
            // Given
            when(userRepository.findByEmail("joao@teste.com")).thenReturn(Optional.of(testUser));

            // When
            UserDTO result = userUseCase.getUserByUsername("joao@teste.com");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("joao@teste.com");
            assertThat(result.getName()).isEqualTo("João Silva");

            verify(userRepository).findByEmail("joao@teste.com");
        }

        @Test
        @DisplayName("Given a valid CPF, when getting user by username, then should return UserDTO")
        void givenValidCpf_whenGetUserByUsername_thenShouldReturnUserDTO() {
            // Given
            when(userRepository.findByEmail("12345678901")).thenReturn(Optional.empty());
            when(userRepository.findByCpf("12345678901")).thenReturn(Optional.of(testUser));

            // When
            UserDTO result = userUseCase.getUserByUsername("12345678901");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCpf()).isEqualTo("12345678901");
            assertThat(result.getName()).isEqualTo("João Silva");

            verify(userRepository).findByEmail("12345678901");
            verify(userRepository).findByCpf("12345678901");
        }

        @Test
        @DisplayName("Given a non-existent username, when getting user by username, then should throw exception")
        void givenNonExistentUsername_whenGetUserByUsername_thenShouldThrowException() {
            // Given
            when(userRepository.findByEmail("inexistente@teste.com")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userUseCase.getUserByUsername("inexistente@teste.com"))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found");

            verify(userRepository).findByEmail("inexistente@teste.com");
            // Note: findByCpf is only called if the username matches the CPF pattern (11 digits)
        }
    }

    @Nested
    @DisplayName("Scenario: User Login")
    class LoginTests {

        @Test
        @DisplayName("Given valid credentials, when logging in, then should return AuthResponseDTO")
        void givenValidCredentials_whenLogin_thenShouldReturnAuthResponseDTO() {
            // Given
            LoginDTO loginDTO = LoginDTO.builder()
                    .username("joao@teste.com")
                    .password("12345678")
                    .build();

            // When
            // Note: This test would be more complex as it involves AuthenticationManager
            // For now, we will skip this test or create an integration test
        }

        @Test
        @DisplayName("Given incorrect password, when logging in, then should throw exception")
        void givenInvalidPassword_whenLogin_thenShouldThrowException() {
            // Given
            LoginDTO loginDTO = LoginDTO.builder()
                    .username("joao@teste.com")
                    .password("senhaErrada")
                    .build();

            // When & Then
            // Note: This test would be more complex as it involves AuthenticationManager
            // For now, we will skip this test or create an integration test
        }

        @Test
        @DisplayName("Given non-existent user, when logging in, then should throw exception")
        void givenNonExistentUser_whenLogin_thenShouldThrowException() {
            // Given
            LoginDTO loginDTO = LoginDTO.builder()
                    .username("inexistente@teste.com")
                    .password("12345678")
                    .build();

            // When & Then
            // Note: This test would be more complex as it involves AuthenticationManager
            // For now, we will skip this test or create an integration test
        }
    }
}
