package com.nimble.gateway.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimble.gateway.application.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import(TestConfig.class)
@DisplayName("Payment Gateway - BDD Integration Tests")
class PaymentGatewayIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @LocalServerPort
    private int port;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
    }

    @Nested
    @DisplayName("Complete Payment Flow Scenarios")
    class CompletePaymentFlowScenarios {

        @Test
        @DisplayName("GIVEN two registered users WHEN completing full payment flow THEN should process successfully")
        void givenTwoRegisteredUsers_whenCompletingFullPaymentFlow_thenShouldProcessSuccessfully() throws Exception {
            // Given
            UserDTO originator = registerUser("João Silva", "12345678901", "joao@teste.com", "senha123");
            UserDTO recipient = registerUser("Maria Santos", "98765432100", "maria@teste.com", "senha456");

            String originatorToken = authenticateUser("joao@teste.com", "senha123");

            // When
            CreateChargeDTO createChargeDTO = CreateChargeDTO.builder()
                    .recipientCpf("98765432100")
                    .amount(new BigDecimal("100.00"))
                    .description("Pagamento de serviços")
                    .build();

            ResponseEntity<ChargeDTO> chargeResponse = restTemplate.exchange(
                    baseUrl + "/api/charges?originatorId=" + originator.getId(),
                    HttpMethod.POST,
                    createAuthenticatedRequest(createChargeDTO, originatorToken),
                    ChargeDTO.class
            );

            // Then
            assertThat(chargeResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            ChargeDTO createdCharge = chargeResponse.getBody();
            assertThat(createdCharge).isNotNull();

            String recipientToken = authenticateUser("maria@teste.com", "senha456");
            DepositDTO depositDTO = DepositDTO.builder()
                    .amount(new BigDecimal("200.00"))
                    .build();

            ResponseEntity<String> depositResponse = restTemplate.exchange(
                    baseUrl + "/api/payments/deposit?userId=" + recipient.getId(),
                    HttpMethod.POST,
                    createAuthenticatedRequest(depositDTO, recipientToken),
                    String.class
            );

            assertThat(depositResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            PayChargeDTO payChargeDTO = PayChargeDTO.builder()
                    .chargeId(createdCharge.getId())
                    .method("BALANCE")
                    .build();

            ResponseEntity<String> paymentResponse = restTemplate.exchange(
                    baseUrl + "/api/payments/pay?payerId=" + recipient.getId(),
                    HttpMethod.POST,
                    createAuthenticatedRequest(payChargeDTO, recipientToken),
                    String.class
            );

            assertThat(paymentResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        @Test
        @DisplayName("GIVEN registered user with insufficient balance WHEN paying charge THEN should fail payment")
        void givenRegisteredUserWithInsufficientBalance_whenPayingCharge_thenShouldFailPayment() throws Exception {
            // Given
            UserDTO originator = registerUser("João Silva", "12345678901", "joao@teste.com", "senha123");
            UserDTO recipient = registerUser("Maria Santos", "98765432100", "maria@teste.com", "senha456");

            String originatorToken = authenticateUser("joao@teste.com", "senha123");
            String recipientToken = authenticateUser("maria@teste.com", "senha456");

            // When
            CreateChargeDTO createChargeDTO = CreateChargeDTO.builder()
                    .recipientCpf("98765432100")
                    .amount(new BigDecimal("1000.00"))
                    .description("Pagamento de serviços")
                    .build();

            ResponseEntity<ChargeDTO> chargeResponse = restTemplate.exchange(
                    baseUrl + "/api/charges?originatorId=" + originator.getId(),
                    HttpMethod.POST,
                    createAuthenticatedRequest(createChargeDTO, originatorToken),
                    ChargeDTO.class
            );

            assertThat(chargeResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            ChargeDTO createdCharge = chargeResponse.getBody();
            assertThat(createdCharge).isNotNull();

            PayChargeDTO payChargeDTO = PayChargeDTO.builder()
                    .chargeId(createdCharge.getId())
                    .method("BALANCE")
                    .build();

            ResponseEntity<String> paymentResponse = restTemplate.exchange(
                    baseUrl + "/api/payments/pay?payerId=" + recipient.getId(),
                    HttpMethod.POST,
                    createAuthenticatedRequest(payChargeDTO, recipientToken),
                    String.class
            );

            // Then
            assertThat(paymentResponse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    @Nested
    @DisplayName("Charge Management Flow Scenarios")
    class ChargeManagementFlowScenarios {

        @Test
        @DisplayName("GIVEN registered user WHEN creating and managing charges THEN should handle all operations successfully")
        void givenRegisteredUser_whenCreatingAndManagingCharges_thenShouldHandleAllOperationsSuccessfully() throws Exception {
            // Given
            UserDTO originator = registerUser("João Silva", "12345678901", "joao@teste.com", "senha123");
            registerUser("Maria Santos", "98765432100", "maria@teste.com", "senha456");

            String originatorToken = authenticateUser("joao@teste.com", "senha123");

            // When
            CreateChargeDTO createChargeDTO = CreateChargeDTO.builder()
                    .recipientCpf("98765432100")
                    .amount(new BigDecimal("100.00"))
                    .description("Pagamento de serviços")
                    .build();

            ResponseEntity<ChargeDTO> createResponse = restTemplate.exchange(
                    baseUrl + "/api/charges?originatorId=" + originator.getId(),
                    HttpMethod.POST,
                    createAuthenticatedRequest(createChargeDTO, originatorToken),
                    ChargeDTO.class
            );

            // Then
            assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            ChargeDTO createdCharge = createResponse.getBody();
            assertThat(createdCharge).isNotNull();
            
            ResponseEntity<String> sentChargesResponse = restTemplate.exchange(
                    baseUrl + "/api/charges/sent?userId=" + originator.getId(),
                    HttpMethod.GET,
                    createAuthenticatedRequest(null, originatorToken),
                    String.class
            );

            assertThat(sentChargesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            ResponseEntity<String> cancelResponse = restTemplate.exchange(
                    baseUrl + "/api/payments/cancel/" + createdCharge.getId() + "?userId=" + originator.getId(),
                    HttpMethod.POST,
                    createAuthenticatedRequest(null, originatorToken),
                    String.class
            );

            assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("Authentication Flow Scenarios")
    class AuthenticationFlowScenarios {

        @Test
        @DisplayName("GIVEN valid credentials WHEN authenticating THEN should return valid token")
        void givenValidCredentials_whenAuthenticating_thenShouldReturnValidToken() throws Exception {
            // Given
            registerUser("João Silva", "12345678901", "joao@teste.com", "senha123");

            // When
            LoginDTO loginDTO = LoginDTO.builder()
                    .username("joao@teste.com")
                    .password("senha123")
                    .build();


            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/api/auth/login",
                    loginDTO,
                    String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).contains("token");
        }

        @Test
        @DisplayName("GIVEN invalid credentials WHEN authenticating THEN should return unauthorized")
        void givenInvalidCredentials_whenAuthenticating_thenShouldReturnUnauthorized() throws Exception {
            // Given
            LoginDTO loginDTO = LoginDTO.builder()
                    .username("invalid@teste.com")
                    .password("wrongpassword")
                    .build();

            // When
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/api/auth/login",
                    loginDTO,
                    String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("Error Handling Scenarios")
    class ErrorHandlingScenarios {

        @Test
        @DisplayName("GIVEN invalid data WHEN creating user THEN should return validation errors")
        void givenInvalidData_whenCreatingUser_thenShouldReturnValidationErrors() throws Exception {
            // Given
            CreateUserDTO invalidUserDTO = CreateUserDTO.builder()
                    .name("")
                    .cpf("123")
                    .email("invalid-email")
                    .password("123")
                    .build();

            // When
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/api/auth/register",
                    invalidUserDTO,
                    String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).contains("Validation Failed");
        }

        @Test
        @DisplayName("GIVEN unauthorized request WHEN accessing protected endpoint THEN should return forbidden")
        void givenUnauthorizedRequest_whenAccessingProtectedEndpoint_thenShouldReturnForbidden() throws Exception {
            // Given
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(null, headers);

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/api/charges/sent?userId=1",
                    HttpMethod.GET,
                    request,
                    String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    private UserDTO registerUser(String name, String cpf, String email, String password) throws Exception {
        CreateUserDTO createUserDTO = CreateUserDTO.builder()
                .name(name)
                .cpf(cpf)
                .email(email)
                .password(password)
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/auth/register",
                createUserDTO,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return objectMapper.readValue(response.getBody(), UserDTO.class);
    }

    private String authenticateUser(String email, String password) throws Exception {
        LoginDTO loginDTO = LoginDTO.builder()
                .username(email)
                .password(password)
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/auth/login",
                loginDTO,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AuthResponseDTO authResponse = objectMapper.readValue(response.getBody(), AuthResponseDTO.class);
        return authResponse.getToken();
    }

    private <T> HttpEntity<T> createAuthenticatedRequest(T body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }
}
