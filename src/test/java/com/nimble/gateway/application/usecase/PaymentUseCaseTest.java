package com.nimble.gateway.application.usecase;

import com.nimble.gateway.application.dto.DepositDTO;
import com.nimble.gateway.application.dto.PayChargeDTO;
import com.nimble.gateway.application.dto.PaymentDTO;
import com.nimble.gateway.domain.entity.Charge;
import com.nimble.gateway.domain.entity.Payment;
import com.nimble.gateway.domain.entity.User;
import com.nimble.gateway.domain.repository.ChargeRepository;
import com.nimble.gateway.domain.repository.PaymentRepository;
import com.nimble.gateway.domain.repository.UserRepository;
import com.nimble.gateway.domain.exception.UserNotFoundException;
import com.nimble.gateway.domain.exception.InsufficientBalanceException;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentUseCase - Unit Tests")
class PaymentUseCaseTest {

    @Mock
    private ChargeRepository chargeRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthorizerService authorizerService;

    @InjectMocks
    private PaymentUseCase paymentUseCase;

    private User payer;
    private User recipient;
    private Charge testCharge;
    private PayChargeDTO payChargeDTO;
    private DepositDTO depositDTO;

    @BeforeEach
    void setUp() {
        payer = User.builder()
                .id(1L)
                .name("João Silva")
                .cpf("12345678901")
                .email("joao@teste.com")
                .balance(BigDecimal.valueOf(1000.00))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        recipient = User.builder()
                .id(2L)
                .name("Maria Santos")
                .cpf("98765432100")
                .email("maria@teste.com")
                .balance(BigDecimal.valueOf(500.00))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        testCharge = Charge.builder()
                .id(1L)
                .originator(recipient)
                .recipient(payer)
                .amount(BigDecimal.valueOf(100.00))
                .description("Pagamento de serviços")
                .status(Charge.ChargeStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        payChargeDTO = PayChargeDTO.builder()
                .chargeId(1L)
                .method("BALANCE")
                .build();

        depositDTO = DepositDTO.builder()
                .amount(BigDecimal.valueOf(200.00))
                .build();
    }

    @Nested
    @DisplayName("Scenario: Payment with Balance")
    class PayWithBalanceTests {

        @Test
        @DisplayName("Given sufficient balance, when paying with balance, then should process payment and update balances")
        void givenSufficientBalance_whenPayWithBalance_thenShouldProcessPaymentAndUpdateBalances() {
            // Given
            when(chargeRepository.findById(1L)).thenReturn(Optional.of(testCharge));
            when(userRepository.findById(1L)).thenReturn(Optional.of(payer));
            when(userRepository.save(any(User.class))).thenReturn(payer);
            when(chargeRepository.save(any(Charge.class))).thenReturn(testCharge);
            when(paymentRepository.save(any(Payment.class))).thenReturn(
                    Payment.builder()
                            .id(1L)
                            .charge(testCharge)
                            .payer(payer)
                            .amount(BigDecimal.valueOf(100.00))
                            .method(Payment.PaymentMethod.BALANCE)
                            .paymentDate(LocalDateTime.now())
                            .build()
            );

            // When
            PaymentDTO result = paymentUseCase.payCharge(payChargeDTO, 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getChargeId()).isEqualTo(1L);
            assertThat(result.getPayerId()).isEqualTo(1L);
            assertThat(result.getAmount()).isEqualTo(BigDecimal.valueOf(100.00));
            assertThat(result.getMethod()).isEqualTo("BALANCE");

            verify(chargeRepository).findById(1L);
            verify(userRepository).findById(1L);
            verify(userRepository, times(2)).save(any(User.class)); // payer e recipient
            verify(chargeRepository).save(any(Charge.class));
            verify(paymentRepository).save(any(Payment.class));
        }

        @Test
        @DisplayName("Given insufficient balance, when paying with balance, then should throw exception")
        void givenInsufficientBalance_whenPayWithBalance_thenShouldThrowException() {
            // Given
            payer.setBalance(BigDecimal.valueOf(50.00)); // Saldo insuficiente
            when(chargeRepository.findById(1L)).thenReturn(Optional.of(testCharge));
            when(userRepository.findById(1L)).thenReturn(Optional.of(payer));

            // When & Then
            assertThatThrownBy(() -> paymentUseCase.payCharge(payChargeDTO, 1L))
                    .isInstanceOf(InsufficientBalanceException.class)
                    .hasMessage("Insufficient balance");

            verify(chargeRepository).findById(1L);
            verify(userRepository).findById(1L);
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Given non-pending charge, when paying, then should throw exception")
        void givenNonPendingCharge_whenPay_thenShouldThrowException() {
            // Given
            testCharge.setStatus(Charge.ChargeStatus.PAID);
            when(chargeRepository.findById(1L)).thenReturn(Optional.of(testCharge));

            // When & Then
            assertThatThrownBy(() -> paymentUseCase.payCharge(payChargeDTO, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Charge is not pending");

            verify(chargeRepository).findById(1L);
            verify(userRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("Given user who is not recipient, when paying, then should throw exception")
        void givenNonRecipientUser_whenPay_thenShouldThrowException() {
            // Given
            when(chargeRepository.findById(1L)).thenReturn(Optional.of(testCharge));
            when(userRepository.findById(2L)).thenReturn(Optional.of(recipient)); // recipient tentando pagar

            // When & Then
            assertThatThrownBy(() -> paymentUseCase.payCharge(payChargeDTO, 2L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Only the recipient can pay this charge");

            verify(chargeRepository).findById(1L);
            verify(userRepository).findById(2L);
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Scenario: Payment with Credit Card")
    class PayWithCreditCardTests {

        @BeforeEach
        void setUp() {
            payChargeDTO = PayChargeDTO.builder()
                    .chargeId(1L)
                    .method("CREDIT_CARD")
                    .cardNumber("4111111111111111")
                    .expiryDate("12/25")
                    .cvv("123")
                    .build();
        }

        @Test
        @DisplayName("Given approved authorization, when paying with credit card, then should process payment")
        void givenApprovedAuthorization_whenPayWithCreditCard_thenShouldProcessPayment() {
            // Given
            when(chargeRepository.findById(1L)).thenReturn(Optional.of(testCharge));
            when(userRepository.findById(1L)).thenReturn(Optional.of(payer));
            when(authorizerService.authorizePayment()).thenReturn(Mono.just(true));
            when(userRepository.save(any(User.class))).thenReturn(recipient);
            when(chargeRepository.save(any(Charge.class))).thenReturn(testCharge);
            when(paymentRepository.save(any(Payment.class))).thenReturn(
                    Payment.builder()
                            .id(1L)
                            .charge(testCharge)
                            .payer(payer)
                            .amount(BigDecimal.valueOf(100.00))
                            .method(Payment.PaymentMethod.CREDIT_CARD)
                            .paymentDate(LocalDateTime.now())
                            .build()
            );

            // When
            PaymentDTO result = paymentUseCase.payCharge(payChargeDTO, 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getMethod()).isEqualTo("CREDIT_CARD");
            assertThat(result.getAmount()).isEqualTo(BigDecimal.valueOf(100.00));

            verify(authorizerService).authorizePayment();
            verify(userRepository).save(any(User.class));
            verify(chargeRepository).save(any(Charge.class));
            verify(paymentRepository).save(any(Payment.class));
        }

        @Test
        @DisplayName("Given rejected authorization, when paying with credit card, then should throw exception")
        void givenRejectedAuthorization_whenPayWithCreditCard_thenShouldThrowException() {
            // Given
            when(chargeRepository.findById(1L)).thenReturn(Optional.of(testCharge));
            when(userRepository.findById(1L)).thenReturn(Optional.of(payer));
            when(authorizerService.authorizePayment()).thenReturn(Mono.just(false));

            // When & Then
            assertThatThrownBy(() -> paymentUseCase.payCharge(payChargeDTO, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Payment authorization failed");

            verify(authorizerService).authorizePayment();
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Given incomplete card data, when paying with credit card, then should throw exception")
        void givenIncompleteCardData_whenPayWithCreditCard_thenShouldThrowException() {
            // Given
            PayChargeDTO incompleteDTO = PayChargeDTO.builder()
                    .chargeId(1L)
                    .method("CREDIT_CARD")
                    .cardNumber("4111111111111111")
                    .expiryDate("12/25")
                    .cvv(null) // CVV ausente
                    .build();

            when(chargeRepository.findById(1L)).thenReturn(Optional.of(testCharge));
            when(userRepository.findById(1L)).thenReturn(Optional.of(payer));

            // When & Then
            assertThatThrownBy(() -> paymentUseCase.payCharge(incompleteDTO, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Credit card information is required");

            verify(authorizerService, never()).authorizePayment();
        }
    }

    @Nested
    @DisplayName("Scenario: Deposit")
    class DepositTests {

        @Test
        @DisplayName("Given approved authorization, when making deposit, then should add balance")
        void givenApprovedAuthorization_whenDeposit_thenShouldAddBalance() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(payer));
            when(authorizerService.authorizeDeposit()).thenReturn(Mono.just(true));
            when(userRepository.save(any(User.class))).thenReturn(payer);
            when(paymentRepository.save(any(Payment.class))).thenReturn(
                    Payment.builder()
                            .id(1L)
                            .payer(payer)
                            .amount(BigDecimal.valueOf(200.00))
                            .method(Payment.PaymentMethod.BALANCE)
                            .paymentDate(LocalDateTime.now())
                            .build()
            );

            // When
            PaymentDTO result = paymentUseCase.deposit(depositDTO, 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualTo(BigDecimal.valueOf(200.00));
            assertThat(result.getMethod()).isEqualTo("BALANCE");

            verify(authorizerService).authorizeDeposit();
            verify(userRepository).save(any(User.class));
            verify(paymentRepository).save(any(Payment.class));
        }

        @Test
        @DisplayName("Given rejected authorization, when making deposit, then should throw exception")
        void givenRejectedAuthorization_whenDeposit_thenShouldThrowException() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(payer));
            when(authorizerService.authorizeDeposit()).thenReturn(Mono.just(false));

            // When & Then
            assertThatThrownBy(() -> paymentUseCase.deposit(depositDTO, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Deposit authorization failed");

            verify(authorizerService).authorizeDeposit();
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Given non-existent user, when making deposit, then should throw exception")
        void givenNonExistentUser_whenDeposit_thenShouldThrowException() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> paymentUseCase.deposit(depositDTO, 999L))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found");

            verify(userRepository).findById(999L);
            verify(authorizerService, never()).authorizeDeposit();
        }
    }

    @Nested
    @DisplayName("Scenario: Charge Cancellation")
    class CancelChargeTests {

        @Test
        @DisplayName("Given pending charge, when cancelling, then should mark as cancelled")
        void givenPendingCharge_whenCancel_thenShouldMarkAsCancelled() {
            // Given
            when(chargeRepository.findById(1L)).thenReturn(Optional.of(testCharge));
            when(chargeRepository.save(any(Charge.class))).thenReturn(testCharge);

            // When - Use the originator ID (2L) instead of recipient ID (1L)
            paymentUseCase.cancelCharge(1L, 2L);

            // Then
            assertThat(testCharge.getStatus()).isEqualTo(Charge.ChargeStatus.CANCELLED);
            assertThat(testCharge.getCancelledAt()).isNotNull();

            verify(chargeRepository).findById(1L);
            verify(chargeRepository).save(any(Charge.class));
        }

        @Test
        @DisplayName("Given already cancelled charge, when cancelling, then should throw exception")
        void givenAlreadyCancelledCharge_whenCancel_thenShouldThrowException() {
            // Given
            testCharge.setStatus(Charge.ChargeStatus.CANCELLED);
            when(chargeRepository.findById(1L)).thenReturn(Optional.of(testCharge));

            // When & Then
            assertThatThrownBy(() -> paymentUseCase.cancelCharge(1L, 2L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Charge is already cancelled");

            verify(chargeRepository).findById(1L);
            verify(chargeRepository, never()).save(any(Charge.class));
        }

        @Test
        @DisplayName("Given non-existent charge, when cancelling, then should throw exception")
        void givenNonExistentCharge_whenCancel_thenShouldThrowException() {
            // Given
            when(chargeRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> paymentUseCase.cancelCharge(999L, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Charge not found");

            verify(chargeRepository).findById(999L);
            verify(chargeRepository, never()).save(any(Charge.class));
        }
    }
}
