package com.nimble.gateway.application.usecase;

import com.nimble.gateway.application.dto.DepositDTO;
import com.nimble.gateway.application.dto.PaymentDTO;
import com.nimble.gateway.domain.entity.Payment;
import com.nimble.gateway.domain.entity.User;
import com.nimble.gateway.domain.exception.PaymentAuthorizationException;
import com.nimble.gateway.domain.exception.UserNotFoundException;
import com.nimble.gateway.domain.repository.PaymentRepository;
import com.nimble.gateway.domain.repository.UserRepository;
import com.nimble.gateway.infrastructure.external.AuthorizerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Deposit Processing - BDD Tests")
class DepositTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private AuthorizerService authorizerService;

    @InjectMocks
    private PaymentUseCase paymentUseCase;

    private User testUser;
    private DepositDTO validDepositDTO;
    private Payment testDepositPayment;

    @BeforeEach
    void setUp() {
        UUID testUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        testUser = User.builder()
                .id(testUserId)
                .name("Test User")
                .cpf("12345678901")
                .email("test@example.com")
                .password("password123")
                .balance(new BigDecimal("100.00"))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        validDepositDTO = DepositDTO.builder()
                .amount(new BigDecimal("200.00"))
                .build();

        testDepositPayment = Payment.builder()
                .id(testUser.getId())
                .charge(null)
                .payer(testUser)
                .amount(new BigDecimal("200.00"))
                .method(Payment.PaymentMethod.BALANCE)
                .paymentDate(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Successful Deposit Scenarios")
    class SuccessfulDepositScenarios {

        @Test
        @DisplayName("GIVEN valid user and deposit amount WHEN processing deposit THEN should process deposit successfully")
        void givenValidUserAndDepositAmount_whenProcessingDeposit_thenShouldProcessDepositSuccessfully() {
            // Given
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(authorizerService.authorizeDeposit(any(BigDecimal.class))).thenReturn(Mono.just(true));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(paymentRepository.save(any(Payment.class))).thenReturn(testDepositPayment);

            // When
            PaymentDTO result = paymentUseCase.deposit(validDepositDTO, testUser.getId()).block();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualTo(validDepositDTO.getAmount());
            assertThat(result.getMethod()).isEqualTo("BALANCE");
            assertThat(result.getPayerId()).isEqualTo(testUser.getId());
            assertThat(result.getChargeId()).isNull();

            verify(userRepository).findById(testUser.getId());
            verify(authorizerService).authorizeDeposit(any(BigDecimal.class));
            verify(userRepository).save(any(User.class));
            verify(paymentRepository).save(any(Payment.class));
        }

        @Test
        @DisplayName("GIVEN user with zero balance WHEN processing deposit THEN should add amount to balance")
        void givenUserWithZeroBalance_whenProcessingDeposit_thenShouldAddAmountToBalance() {
            // Given
            User userWithZeroBalance = User.builder()
                    .id(testUser.getId())
                    .name("Test User")
                    .cpf("12345678901")
                    .email("test@example.com")
                    .password("password123")
                    .balance(BigDecimal.ZERO)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(userWithZeroBalance));
            when(authorizerService.authorizeDeposit(any(BigDecimal.class))).thenReturn(Mono.just(true));
            when(userRepository.save(any(User.class))).thenReturn(userWithZeroBalance);
            when(paymentRepository.save(any(Payment.class))).thenReturn(testDepositPayment);

            // When
            PaymentDTO result = paymentUseCase.deposit(validDepositDTO, testUser.getId()).block();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualTo(validDepositDTO.getAmount());

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("GIVEN user with existing balance WHEN processing deposit THEN should add to existing balance")
        void givenUserWithExistingBalance_whenProcessingDeposit_thenShouldAddToExistingBalance() {
            // Given
            BigDecimal existingBalance = new BigDecimal("150.00");
            User userWithBalance = User.builder()
                    .id(testUser.getId())
                    .name("Test User")
                    .cpf("12345678901")
                    .email("test@example.com")
                    .password("password123")
                    .balance(existingBalance)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(userWithBalance));
            when(authorizerService.authorizeDeposit(any(BigDecimal.class))).thenReturn(Mono.just(true));
            when(userRepository.save(any(User.class))).thenReturn(userWithBalance);
            when(paymentRepository.save(any(Payment.class))).thenReturn(testDepositPayment);

            // When
            PaymentDTO result = paymentUseCase.deposit(validDepositDTO, testUser.getId()).block();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualTo(validDepositDTO.getAmount());

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("GIVEN large deposit amount WHEN processing deposit THEN should handle large amounts correctly")
        void givenLargeDepositAmount_whenProcessingDeposit_thenShouldHandleLargeAmountsCorrectly() {
            // Given
            DepositDTO largeDepositDTO = DepositDTO.builder()
                    .amount(new BigDecimal("10000.00"))
                    .build();

            Payment largeDepositPayment = Payment.builder()
                    .id(testUser.getId())
                    .charge(null)
                    .payer(testUser)
                    .amount(new BigDecimal("10000.00"))
                    .method(Payment.PaymentMethod.BALANCE)
                    .paymentDate(LocalDateTime.now())
                    .build();

            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(authorizerService.authorizeDeposit(any(BigDecimal.class))).thenReturn(Mono.just(true));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(paymentRepository.save(any(Payment.class))).thenReturn(largeDepositPayment);

            // When
            PaymentDTO result = paymentUseCase.deposit(largeDepositDTO, testUser.getId()).block();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualTo(largeDepositDTO.getAmount());

            verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Authorization Failure Scenarios")
    class AuthorizationFailureScenarios {

        @Test
        @DisplayName("GIVEN authorization service returns false WHEN processing deposit THEN should throw PaymentAuthorizationException")
        void givenAuthorizationServiceReturnsFalse_whenProcessingDeposit_thenShouldThrowPaymentAuthorizationException() {
            // Given
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(authorizerService.authorizeDeposit(any(BigDecimal.class))).thenReturn(Mono.just(false));

            // When & Then
            assertThatThrownBy(() -> paymentUseCase.deposit(validDepositDTO, testUser.getId()).block())
                    .isInstanceOf(PaymentAuthorizationException.class)
                    .hasMessage("Deposit authorization failed");

            verify(userRepository).findById(testUser.getId());
            verify(authorizerService).authorizeDeposit(any(BigDecimal.class));
            verify(userRepository, never()).save(any(User.class));
            verify(paymentRepository, never()).save(any(Payment.class));
        }

        @Test
        @DisplayName("GIVEN authorization service throws exception WHEN processing deposit THEN should throw RuntimeException")
        void givenAuthorizationServiceThrowsException_whenProcessingDeposit_thenShouldThrowRuntimeException() {
            // Given
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(authorizerService.authorizeDeposit(any(BigDecimal.class))).thenReturn(Mono.error(new RuntimeException("Service unavailable")));

            // When & Then
            assertThatThrownBy(() -> paymentUseCase.deposit(validDepositDTO, testUser.getId()).block())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Service unavailable");

            verify(userRepository).findById(testUser.getId());
            verify(authorizerService).authorizeDeposit(any(BigDecimal.class));
            verify(userRepository, never()).save(any(User.class));
            verify(paymentRepository, never()).save(any(Payment.class));
        }
    }

    @Nested
    @DisplayName("User Not Found Scenarios")
    class UserNotFoundScenarios {

        @Test
        @DisplayName("GIVEN non-existing user ID WHEN processing deposit THEN should throw UserNotFoundException")
        void givenNonExistingUserId_whenProcessingDeposit_thenShouldThrowUserNotFoundException() {
            // Given
            UUID nonExistentUserId = UUID.randomUUID();
            when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> paymentUseCase.deposit(validDepositDTO, nonExistentUserId).block())
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found");

            verify(userRepository).findById(nonExistentUserId);
            verify(authorizerService, never()).authorizeDeposit(any(BigDecimal.class));
            verify(userRepository, never()).save(any(User.class));
            verify(paymentRepository, never()).save(any(Payment.class));
        }

        @Test
        @DisplayName("GIVEN null user ID WHEN processing deposit THEN should throw UserNotFoundException")
        void givenNullUserId_whenProcessingDeposit_thenShouldThrowUserNotFoundException() {
            // Given
            when(userRepository.findById(null)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> paymentUseCase.deposit(validDepositDTO, null).block())
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found");

            verify(userRepository).findById(null);
            verify(authorizerService, never()).authorizeDeposit(any(BigDecimal.class));
        }
    }

    @Nested
    @DisplayName("Edge Cases Scenarios")
    class EdgeCasesScenarios {

        @Test
        @DisplayName("GIVEN minimum deposit amount WHEN processing deposit THEN should process successfully")
        void givenMinimumDepositAmount_whenProcessingDeposit_thenShouldProcessSuccessfully() {
            // Given
            DepositDTO minimumDepositDTO = DepositDTO.builder()
                    .amount(new BigDecimal("0.01"))
                    .build();

            Payment minimumDepositPayment = Payment.builder()
                    .id(testUser.getId())
                    .charge(null)
                    .payer(testUser)
                    .amount(new BigDecimal("0.01"))
                    .method(Payment.PaymentMethod.BALANCE)
                    .paymentDate(LocalDateTime.now())
                    .build();

            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(authorizerService.authorizeDeposit(any(BigDecimal.class))).thenReturn(Mono.just(true));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(paymentRepository.save(any(Payment.class))).thenReturn(minimumDepositPayment);

            // When
            PaymentDTO result = paymentUseCase.deposit(minimumDepositDTO, testUser.getId()).block();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualTo(minimumDepositDTO.getAmount());

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("GIVEN inactive user WHEN processing deposit THEN should process successfully")
        void givenInactiveUser_whenProcessingDeposit_thenShouldProcessSuccessfully() {
            // Given
            User inactiveUser = User.builder()
                    .id(testUser.getId())
                    .name("Test User")
                    .cpf("12345678901")
                    .email("test@example.com")
                    .password("password123")
                    .balance(new BigDecimal("100.00"))
                    .isActive(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(inactiveUser));
            when(authorizerService.authorizeDeposit(any(BigDecimal.class))).thenReturn(Mono.just(true));
            when(userRepository.save(any(User.class))).thenReturn(inactiveUser);
            when(paymentRepository.save(any(Payment.class))).thenReturn(testDepositPayment);

            // When
            PaymentDTO result = paymentUseCase.deposit(validDepositDTO, testUser.getId()).block();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualTo(validDepositDTO.getAmount());

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("GIVEN deposit with decimal precision WHEN processing deposit THEN should maintain precision")
        void givenDepositWithDecimalPrecision_whenProcessingDeposit_thenShouldMaintainPrecision() {
            // Given
            DepositDTO preciseDepositDTO = DepositDTO.builder()
                    .amount(new BigDecimal("123.45"))
                    .build();

            Payment preciseDepositPayment = Payment.builder()
                    .id(testUser.getId())
                    .charge(null)
                    .payer(testUser)
                    .amount(new BigDecimal("123.45"))
                    .method(Payment.PaymentMethod.BALANCE)
                    .paymentDate(LocalDateTime.now())
                    .build();

            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(authorizerService.authorizeDeposit(any(BigDecimal.class))).thenReturn(Mono.just(true));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(paymentRepository.save(any(Payment.class))).thenReturn(preciseDepositPayment);

            // When
            PaymentDTO result = paymentUseCase.deposit(preciseDepositDTO, testUser.getId()).block();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualTo(preciseDepositDTO.getAmount());

            verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Repository Interaction Scenarios")
    class RepositoryInteractionScenarios {

        @Test
        @DisplayName("GIVEN successful deposit WHEN processing deposit THEN should call all required repository methods")
        void givenSuccessfulDeposit_whenProcessingDeposit_thenShouldCallAllRequiredRepositoryMethods() {
            // Given
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(authorizerService.authorizeDeposit(any(BigDecimal.class))).thenReturn(Mono.just(true));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(paymentRepository.save(any(Payment.class))).thenReturn(testDepositPayment);

            // When
            paymentUseCase.deposit(validDepositDTO, testUser.getId()).block();

            // Then
            verify(userRepository, times(1)).findById(testUser.getId());
            verify(authorizerService, times(1)).authorizeDeposit(any(BigDecimal.class));
            verify(userRepository, times(1)).save(any(User.class));
            verify(paymentRepository, times(1)).save(any(Payment.class));
        }

        @Test
        @DisplayName("GIVEN authorization failure WHEN processing deposit THEN should not save user or payment")
        void givenAuthorizationFailure_whenProcessingDeposit_thenShouldNotSaveUserOrPayment() {
            // Given
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(authorizerService.authorizeDeposit(any(BigDecimal.class))).thenReturn(Mono.just(false));

            // When & Then
            assertThatThrownBy(() -> paymentUseCase.deposit(validDepositDTO, testUser.getId()).block())
                    .isInstanceOf(PaymentAuthorizationException.class);

            verify(userRepository, times(1)).findById(testUser.getId());
            verify(authorizerService, times(1)).authorizeDeposit(any(BigDecimal.class));
            verify(userRepository, never()).save(any(User.class));
            verify(paymentRepository, never()).save(any(Payment.class));
        }

        @Test
        @DisplayName("GIVEN user not found WHEN processing deposit THEN should not call authorization service")
        void givenUserNotFound_whenProcessingDeposit_thenShouldNotCallAuthorizationService() {
            // Given
            UUID nonExistentUserId = UUID.randomUUID();
            when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> paymentUseCase.deposit(validDepositDTO, nonExistentUserId).block())
                    .isInstanceOf(UserNotFoundException.class);

            verify(userRepository, times(1)).findById(nonExistentUserId);
            verify(authorizerService, never()).authorizeDeposit(any(BigDecimal.class));
            verify(userRepository, never()).save(any(User.class));
            verify(paymentRepository, never()).save(any(Payment.class));
        }
    }
}
