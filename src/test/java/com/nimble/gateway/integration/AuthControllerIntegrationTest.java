package com.nimble.gateway.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimble.gateway.application.dto.CreateUserDTO;
import com.nimble.gateway.application.dto.LoginDTO;
import com.nimble.gateway.domain.entity.User;
import com.nimble.gateway.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@DisplayName("AuthController - Integration Tests")
class AuthControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("Scenario: User Registration")
    class RegisterUserTests {

        @Test
        @DisplayName("Given valid data, when registering user, then should return 201 with user data")
        void givenValidData_whenRegisterUser_thenShouldReturn201WithUserData() throws Exception {
            // Given
            CreateUserDTO createUserDTO = CreateUserDTO.builder()
                    .name("João Silva")
                    .cpf("12345678901")
                    .email("joao@teste.com")
                    .password("12345678")
                    .build();

            // When
            var response = restTemplate.postForEntity("http://localhost:" + port + "/api/auth/register", createUserDTO, Object.class);

            // Then
            assertThat(response.getStatusCode().value()).isEqualTo(201);
        }

        @Test
        @DisplayName("Given invalid data, when registering user, then should return 400")
        void givenInvalidData_whenRegisterUser_thenShouldReturn400() throws Exception {
            // Given
            CreateUserDTO invalidDTO = CreateUserDTO.builder()
                    .name("")
                    .cpf("123")
                    .email("email-invalido")
                    .password("123")
                    .build();

            // When
            var response = restTemplate.postForEntity("http://localhost:" + port + "/api/auth/register", invalidDTO, Object.class);

            // Then
            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }
    }

    @Nested
    @DisplayName("Scenario: User Login")
    class LoginTests {

        @Test
        @DisplayName("Given valid credentials, when logging in, then should return 200 with token")
        void givenValidCredentials_whenLogin_thenShouldReturn200WithToken() throws Exception {
            // Given
            User user = User.builder()
                    .name("João Silva")
                    .cpf("12345678901")
                    .email("joao@teste.com")
                    .password(passwordEncoder.encode("12345678"))
                    .balance(BigDecimal.valueOf(1000.00))
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            User savedUser = userRepository.save(user);

            // When
            LoginDTO loginDTO = LoginDTO.builder()
                    .username("joao@teste.com")
                    .password("12345678")
                    .build();


            var response = restTemplate.postForEntity("http://localhost:" + port + "/api/auth/login", loginDTO, Object.class);

            // Then
            assertThat(response.getStatusCode().value()).isEqualTo(200);
        }

        @Test
        @DisplayName("Given invalid credentials, when logging in, then should return 401")
        void givenInvalidCredentials_whenLogin_thenShouldReturn401() throws Exception {
            // Given
            User user = User.builder()
                    .name("João Silva")
                    .cpf("12345678901")
                    .email("joao@teste.com")
                    .password(passwordEncoder.encode("12345678"))
                    .balance(BigDecimal.ZERO)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            userRepository.save(user);

            // When
            LoginDTO loginDTO = LoginDTO.builder()
                    .username("joao@teste.com")
                    .password("wrongPassword")
                    .build();

            // When
            var response = restTemplate.postForEntity("http://localhost:" + port + "/api/auth/login", loginDTO, Object.class);

            // Then
            assertThat(response.getStatusCode().value()).isEqualTo(401);
        }

        @Test
        @DisplayName("Given non-existent user, when logging in, then should return 401")
        void givenNonExistentUser_whenLogin_thenShouldReturn401() throws Exception {

            LoginDTO loginDTO = LoginDTO.builder()
                    .username("nonexistent@teste.com")
                    .password("12345678")
                    .build();


            var response = restTemplate.postForEntity("http://localhost:" + port + "/api/auth/login", loginDTO, Object.class);

            assertThat(response.getStatusCode().value()).isEqualTo(401);
        }
    }

    @Nested
    @DisplayName("Scenario: Get Current User")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Given no token, when getting current user, then should return 401")
        void givenNoToken_whenGetCurrentUser_thenShouldReturn401() throws Exception {

            var response = restTemplate.getForEntity("http://localhost:" + port + "/api/auth/me", Object.class);

            assertThat(response.getStatusCode().value()).isEqualTo(401);
        }
    }
}