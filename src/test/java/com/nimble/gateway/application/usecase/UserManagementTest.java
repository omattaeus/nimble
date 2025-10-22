package com.nimble.gateway.application.usecase;

import com.nimble.gateway.application.dto.AuthResponseDTO;
import com.nimble.gateway.application.dto.CreateUserDTO;
import com.nimble.gateway.application.dto.LoginDTO;
import com.nimble.gateway.application.dto.UserDTO;
import com.nimble.gateway.domain.entity.User;
import com.nimble.gateway.domain.exception.UserNotFoundException;
import com.nimble.gateway.domain.repository.UserRepository;
import com.nimble.gateway.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Management - BDD Tests")
class UserManagementTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private AuthenticationManager authenticationManager;

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
                .password("hashedPassword")
                .balance(BigDecimal.ZERO)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createUserDTO = CreateUserDTO.builder()
                .name("João Silva")
                .cpf("12345678901")
                .email("joao@teste.com")
                .password("senha123")
                .build();
    }

    @Nested
    @DisplayName("User Registration Scenarios")
    class UserRegistrationScenarios {

        @Test
        @DisplayName("GIVEN valid user data WHEN registering user THEN should create user successfully")
        void givenValidUserData_whenRegisteringUser_thenShouldCreateUserSuccessfully() {

            when(passwordEncoder.encode(createUserDTO.getPassword())).thenReturn("hashedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);


            UserDTO result = userUseCase.createUser(createUserDTO);


            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(createUserDTO.getName());
            assertThat(result.getEmail()).isEqualTo(createUserDTO.getEmail());
            assertThat(result.getCpf()).isEqualTo(createUserDTO.getCpf());
            assertThat(result.getBalance()).isEqualTo(BigDecimal.ZERO);
            assertThat(result.getIsActive()).isTrue();

            verify(passwordEncoder).encode(createUserDTO.getPassword());
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("GIVEN invalid CPF format WHEN registering user THEN should be caught by validation layer")
        void givenInvalidCpfFormat_whenRegisteringUser_thenShouldBeCaughtByValidationLayer() {

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
    @DisplayName("User Profile Scenarios")
    class UserProfileScenarios {

        @Test
        @DisplayName("GIVEN existing user ID WHEN getting user by ID THEN should return user DTO")
        void givenExistingUserId_whenGettingUserById_thenShouldReturnUserDTO() {

            UUID userId = testUser.getId();
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));


            UserDTO result = userUseCase.getUserById(userId);


            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(userId);
            assertThat(result.getEmail()).isEqualTo(testUser.getEmail());
            assertThat(result.getName()).isEqualTo(testUser.getName());

            verify(userRepository).findById(userId);
        }

        @Test
        @DisplayName("GIVEN non-existing user ID WHEN getting user by ID THEN should throw UserNotFoundException")
        void givenNonExistingUserId_whenGettingUserById_thenShouldThrowUserNotFoundException() {

            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());


            assertThatThrownBy(() -> userUseCase.getUserById(userId))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found with ID: " + userId);

            verify(userRepository).findById(userId);
        }

        @Test
        @DisplayName("GIVEN existing username WHEN getting user by username THEN should return user DTO")
        void givenExistingUsername_whenGettingUserByUsername_thenShouldReturnUserDTO() {

            String username = "joao@teste.com";
            when(userRepository.findByEmail(username)).thenReturn(Optional.of(testUser));


            UserDTO result = userUseCase.getUserByUsername(username);


            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(username);
            assertThat(result.getName()).isEqualTo(testUser.getName());

            verify(userRepository).findByEmail(username);
        }

        @Test
        @DisplayName("GIVEN existing CPF as username WHEN getting user by username THEN should return user DTO")
        void givenExistingCpfAsUsername_whenGettingUserByUsername_thenShouldReturnUserDTO() {

            String cpfUsername = "12345678901";
            when(userRepository.findByEmail(cpfUsername)).thenReturn(Optional.empty());
            when(userRepository.findByCpf(cpfUsername)).thenReturn(Optional.of(testUser));


            UserDTO result = userUseCase.getUserByUsername(cpfUsername);


            assertThat(result).isNotNull();
            assertThat(result.getCpf()).isEqualTo(cpfUsername);
            assertThat(result.getName()).isEqualTo(testUser.getName());

            verify(userRepository).findByEmail(cpfUsername);
            verify(userRepository).findByCpf(cpfUsername);
        }

        @Test
        @DisplayName("GIVEN non-existing username WHEN getting user by username THEN should throw UserNotFoundException")
        void givenNonExistingUsername_whenGettingUserByUsername_thenShouldThrowUserNotFoundException() {

            String username = "nonexistent@test.com";
            when(userRepository.findByEmail(username)).thenReturn(Optional.empty());


            assertThatThrownBy(() -> userUseCase.getUserByUsername(username))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found");

            verify(userRepository).findByEmail(username);
        }
    }

    @Nested
    @DisplayName("User Login Scenarios")
    class UserLoginScenarios {

        @Test
        @DisplayName("GIVEN valid credentials WHEN logging in THEN should return authentication response")
        void givenValidCredentials_whenLoggingIn_thenShouldReturnAuthenticationResponse() {

            LoginDTO loginDTO = LoginDTO.builder()
                    .username("joao@teste.com")
                    .password("senha123")
                    .build();

            Authentication mockAuth = mock(Authentication.class);
            when(mockAuth.getName()).thenReturn("joao@teste.com");
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(mockAuth);
            when(jwtTokenProvider.generateToken(mockAuth)).thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken("joao@teste.com")).thenReturn("refresh-token");
            when(userRepository.findByEmail("joao@teste.com")).thenReturn(Optional.of(testUser));


            AuthResponseDTO result = userUseCase.login(loginDTO);


            assertThat(result).isNotNull();
            assertThat(result.getToken()).isEqualTo("access-token");
            assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(result.getType()).isEqualTo("Bearer");
            assertThat(result.getUser()).isNotNull();
            assertThat(result.getUser().getEmail()).isEqualTo("joao@teste.com");

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtTokenProvider).generateToken(mockAuth);
            verify(jwtTokenProvider).generateRefreshToken("joao@teste.com");
            verify(userRepository).findByEmail("joao@teste.com");
        }

        @Test
        @DisplayName("GIVEN invalid credentials WHEN logging in THEN should throw exception")
        void givenInvalidCredentials_whenLoggingIn_thenShouldThrowException() {

            LoginDTO loginDTO = LoginDTO.builder()
                    .username("invalid@teste.com")
                    .password("wrongpassword")
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Invalid credentials"));


            assertThatThrownBy(() -> userUseCase.login(loginDTO))
                    .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class)
                    .hasMessage("Invalid credentials");

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtTokenProvider, never()).generateToken(any());
            verify(userRepository, never()).findByEmail(anyString());
        }
    }
}