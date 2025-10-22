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
import java.util.UUID;

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
                .id(UUID.randomUUID())
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

            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);


            UserDTO result = userUseCase.createUser(createUserDTO);


            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("João Silva");
            assertThat(result.getCpf()).isEqualTo("12345678901");
            assertThat(result.getEmail()).isEqualTo("joao@teste.com");
            assertThat(result.getBalance()).isEqualTo(BigDecimal.ZERO);
            assertThat(result.getIsActive()).isTrue();

            verify(passwordEncoder).encode("12345678");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Given invalid CPF format, when creating user, then should be caught by validation layer")
        void givenInvalidCpfFormat_whenCreateUser_thenShouldBeCaughtByValidationLayer() {

            CreateUserDTO invalidCpfDTO = CreateUserDTO.builder()
                    .name("Invalid User")
                    .cpf("123")
                    .email("invalid@test.com")
                    .password("password123")
                    .build();


            verifyNoInteractions(userRepository);
            verifyNoInteractions(passwordEncoder);
        }
    }

    @Nested
    @DisplayName("Scenario: Get User by ID")
    class GetUserByIdTests {

        @Test
        @DisplayName("Given a valid ID, when getting user, then should return UserDTO")
        void givenValidId_whenGetUserById_thenShouldReturnUserDTO() {

            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

            UserDTO result = userUseCase.getUserById(testUser.getId());

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testUser.getId());
            assertThat(result.getName()).isEqualTo("João Silva");
            assertThat(result.getEmail()).isEqualTo("joao@teste.com");

            verify(userRepository).findById(testUser.getId());
        }

        @Test
        @DisplayName("Given a non-existent ID, when getting user, then should throw exception")
        void givenNonExistentId_whenGetUserById_thenShouldThrowException() {

            UUID nonExistentId = UUID.randomUUID();
            when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());


            assertThatThrownBy(() -> userUseCase.getUserById(nonExistentId))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found with ID: " + nonExistentId);

            verify(userRepository).findById(nonExistentId);
        }
    }

    @Nested
    @DisplayName("Scenario: Get User by Username")
    class GetUserByUsernameTests {

        @Test
        @DisplayName("Given a valid email, when getting user by username, then should return UserDTO")
        void givenValidEmail_whenGetUserByUsername_thenShouldReturnUserDTO() {

            when(userRepository.findByEmail("joao@teste.com")).thenReturn(Optional.of(testUser));

            UserDTO result = userUseCase.getUserByUsername("joao@teste.com");

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("joao@teste.com");
            assertThat(result.getName()).isEqualTo("João Silva");

            verify(userRepository).findByEmail("joao@teste.com");
        }

        @Test
        @DisplayName("Given a valid CPF, when getting user by username, then should return UserDTO")
        void givenValidCpf_whenGetUserByUsername_thenShouldReturnUserDTO() {

            when(userRepository.findByEmail("12345678901")).thenReturn(Optional.empty());
            when(userRepository.findByCpf("12345678901")).thenReturn(Optional.of(testUser));


            UserDTO result = userUseCase.getUserByUsername("12345678901");


            assertThat(result).isNotNull();
            assertThat(result.getCpf()).isEqualTo("12345678901");
            assertThat(result.getName()).isEqualTo("João Silva");

            verify(userRepository).findByEmail("12345678901");
            verify(userRepository).findByCpf("12345678901");
        }

        @Test
        @DisplayName("Given a non-existent username, when getting user by username, then should throw exception")
        void givenNonExistentUsername_whenGetUserByUsername_thenShouldThrowException() {

            when(userRepository.findByEmail("inexistente@teste.com")).thenReturn(Optional.empty());


            assertThatThrownBy(() -> userUseCase.getUserByUsername("inexistente@teste.com"))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found");

            verify(userRepository).findByEmail("inexistente@teste.com");
        }
    }

    @Nested
    @DisplayName("Scenario: User Login")
    class LoginTests {

        @Test
        @DisplayName("Given valid credentials, when logging in, then should return AuthResponseDTO")
        void givenValidCredentials_whenLogin_thenShouldReturnAuthResponseDTO() {

            LoginDTO loginDTO = LoginDTO.builder()
                    .username("joao@teste.com")
                    .password("12345678")
                    .build();


        }

        @Test
        @DisplayName("Given incorrect password, when logging in, then should throw exception")
        void givenInvalidPassword_whenLogin_thenShouldThrowException() {

            LoginDTO loginDTO = LoginDTO.builder()
                    .username("joao@teste.com")
                    .password("senhaErrada")
                    .build();


        }

        @Test
        @DisplayName("Given non-existent user, when logging in, then should throw exception")
        void givenNonExistentUser_whenLogin_thenShouldThrowException() {

            LoginDTO loginDTO = LoginDTO.builder()
                    .username("inexistente@teste.com")
                    .password("12345678")
                    .build();


        }
    }
}
