package com.nimble.gateway.application.usecase;

import com.nimble.gateway.application.dto.DepositDTO;
import com.nimble.gateway.application.dto.PayChargeDTO;
import com.nimble.gateway.application.dto.PaymentDTO;
import com.nimble.gateway.domain.entity.Charge;
import com.nimble.gateway.domain.entity.Payment;
import com.nimble.gateway.domain.entity.User;
import com.nimble.gateway.domain.exception.InsufficientBalanceException;
import com.nimble.gateway.domain.exception.PaymentAuthorizationException;
import com.nimble.gateway.domain.exception.UserNotFoundException;
import com.nimble.gateway.domain.repository.ChargeRepository;
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
@DisplayName("Payment Processing - BDD Tests")
class PaymentTest {

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
    private Payment testPaymentBalance;
    private Payment testPaymentCreditCard;

    @BeforeEach
    void setUp() {
        UUID payerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID recipientId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID chargeId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        
        payer = User.builder()
                .id(payerId)
                .name("Payer User")
                .cpf("11122233344")
                .email("payer@test.com")
                .password("password")
                .balance(new BigDecimal("500.00"))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        recipient = User.builder()
                .id(recipientId)
                .name("Recipient User")
                .cpf("55566677788")
                .email("recipient@test.com")
                .password("password")
                .balance(new BigDecimal("100.00"))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testCharge = Charge.builder()
                .id(chargeId)
                .originator(recipient)
                .recipient(payer)
                .amount(new BigDecimal("100.00"))
                .description("Pagamento de serviços")
                .status(Charge.ChargeStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        testPaymentBalance = Payment.builder()
                .id(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                .charge(testCharge)
                .payer(payer)
                .amount(new BigDecimal("100.00"))
                .method(Payment.PaymentMethod.BALANCE)
                .paymentDate(LocalDateTime.now())
                .build();

        testPaymentCreditCard = Payment.builder()
                .id(UUID.fromString("55555555-5555-5555-5555-555555555555"))
                .charge(testCharge)
                .payer(payer)
                .amount(new BigDecimal("100.00"))
                .method(Payment.PaymentMethod.CREDIT_CARD)
                .paymentDate(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Payment by Balance Scenarios")
    class PaymentByBalanceScenarios {

        @Test
        @DisplayName("GIVEN sufficient balance WHEN paying charge by balance THEN should process payment successfully")
        void givenSufficientBalance_whenPayingChargeByBalance_thenShouldProcessPaymentSuccessfully() {

            PayChargeDTO payChargeDTO = PayChargeDTO.builder()
                    .chargeId(payer.getId())
                    .method("BALANCE")
                    .build();

            when(chargeRepository.findById(payer.getId())).thenReturn(Optional.of(testCharge));
            when(userRepository.findById(payer.getId())).thenReturn(Optional.of(payer));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPaymentBalance);
            when(chargeRepository.save(any(Charge.class))).thenReturn(testCharge);

            PaymentDTO result = paymentUseCase.payCharge(payChargeDTO, payer.getId()).block();

            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualTo(testCharge.getAmount());
            assertThat(result.getMethod()).isEqualTo("BALANCE");
            assertThat(result.getPayerId()).isEqualTo(payer.getId());

            verify(chargeRepository).findById(payer.getId());
            verify(userRepository).findById(payer.getId());
            verify(paymentRepository).save(any(Payment.class));
            verify(chargeRepository).save(any(Charge.class));
        }

        @Test
        @DisplayName("GIVEN insufficient balance WHEN paying charge by balance THEN should throw exception")
        void givenInsufficientBalance_whenPayingChargeByBalance_thenShouldThrowException() {

            User poorPayer = User.builder()
                    .id(payer.getId())
                    .name("Poor Payer")
                    .cpf("11122233344")
                    .email("poor@test.com")
                    .password("password")
                    .balance(new BigDecimal("50.00"))
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            PayChargeDTO payChargeDTO = PayChargeDTO.builder()
                    .chargeId(payer.getId())
                    .method("BALANCE")
                    .build();

            when(chargeRepository.findById(payer.getId())).thenReturn(Optional.of(testCharge));
            when(userRepository.findById(payer.getId())).thenReturn(Optional.of(poorPayer));

            assertThatThrownBy(() -> paymentUseCase.payCharge(payChargeDTO, payer.getId()).block())
                    .isInstanceOf(InsufficientBalanceException.class)
                    .hasMessage("Insufficient balance");

            verify(chargeRepository).findById(payer.getId());
            verify(userRepository).findById(payer.getId());
            verify(paymentRepository, never()).save(any(Payment.class));
        }

        @Test
        @DisplayName("GIVEN non-existing charge WHEN paying charge THEN should throw exception")
        void givenNonExistingCharge_whenPayingCharge_thenShouldThrowException() {

            UUID nonExistentChargeId = UUID.randomUUID();
            PayChargeDTO payChargeDTO = PayChargeDTO.builder()
                    .chargeId(nonExistentChargeId)
                    .method("BALANCE")
                    .build();

            when(chargeRepository.findById(nonExistentChargeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentUseCase.payCharge(payChargeDTO, payer.getId()).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Charge not found");

            verify(chargeRepository).findById(nonExistentChargeId);
            verify(userRepository, never()).findById(any(UUID.class));
        }

        @Test
        @DisplayName("GIVEN already paid charge WHEN paying charge THEN should throw exception")
        void givenAlreadyPaidCharge_whenPayingCharge_thenShouldThrowException() {

            Charge paidCharge = Charge.builder()
                    .id(payer.getId())
                    .originator(recipient)
                    .recipient(payer)
                    .amount(new BigDecimal("100.00"))
                    .description("Already paid charge")
                    .status(Charge.ChargeStatus.PAID)
                    .createdAt(LocalDateTime.now())
                    .build();

            PayChargeDTO payChargeDTO = PayChargeDTO.builder()
                    .chargeId(payer.getId())
                    .method("BALANCE")
                    .build();

            when(chargeRepository.findById(payer.getId())).thenReturn(Optional.of(paidCharge));

            assertThatThrownBy(() -> paymentUseCase.payCharge(payChargeDTO, payer.getId()).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Charge is not pending");

            verify(chargeRepository).findById(payer.getId());
            verify(userRepository, never()).findById(any(UUID.class));
        }

        @Test
        @DisplayName("GIVEN unauthorized user WHEN paying charge THEN should throw exception")
        void givenUnauthorizedUser_whenPayingCharge_thenShouldThrowException() {

            PayChargeDTO payChargeDTO = PayChargeDTO.builder()
                    .chargeId(payer.getId())
                    .method("BALANCE")
                    .build();

            when(chargeRepository.findById(payer.getId())).thenReturn(Optional.of(testCharge));
            UUID unauthorizedUserId = UUID.randomUUID();
            when(userRepository.findById(unauthorizedUserId)).thenReturn(Optional.of(payer));

            assertThatThrownBy(() -> paymentUseCase.payCharge(payChargeDTO, unauthorizedUserId).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Only the recipient can pay this charge");

            verify(chargeRepository).findById(payer.getId());
            verify(userRepository).findById(unauthorizedUserId);
        }
    }

    @Nested
    @DisplayName("Payment by Credit Card Scenarios")
    class PaymentByCreditCardScenarios {

        @Test
        @DisplayName("GIVEN valid credit card data WHEN paying charge by credit card THEN should process payment successfully")
        void givenValidCreditCardData_whenPayingChargeByCreditCard_thenShouldProcessPaymentSuccessfully() {

            PayChargeDTO payChargeDTO = PayChargeDTO.builder()
                    .chargeId(payer.getId())
                    .method("CREDIT_CARD")
                    .cardNumber("1234567890123456")
                    .expiryDate("12/25")
                    .cvv("123")
                    .build();

            when(chargeRepository.findById(payer.getId())).thenReturn(Optional.of(testCharge));
            when(userRepository.findById(payer.getId())).thenReturn(Optional.of(payer));
            when(authorizerService.authorizePayment(any(BigDecimal.class))).thenReturn(Mono.just(true));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPaymentCreditCard);
            when(chargeRepository.save(any(Charge.class))).thenReturn(testCharge);

            PaymentDTO result = paymentUseCase.payCharge(payChargeDTO, payer.getId()).block();

            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualTo(testCharge.getAmount());
            assertThat(result.getMethod()).isEqualTo("CREDIT_CARD");
            assertThat(result.getPayerId()).isEqualTo(payer.getId());

            verify(chargeRepository).findById(payer.getId());
            verify(userRepository).findById(payer.getId());
            verify(authorizerService).authorizePayment(any(BigDecimal.class));
            verify(userRepository).save(any(User.class));
            verify(paymentRepository).save(any(Payment.class));
            verify(chargeRepository).save(any(Charge.class));
        }

        @Test
        @DisplayName("GIVEN missing credit card data WHEN paying charge by credit card THEN should throw exception")
        void givenMissingCreditCardData_whenPayingChargeByCreditCard_thenShouldThrowException() {

            PayChargeDTO payChargeDTO = PayChargeDTO.builder()
                    .chargeId(payer.getId())
                    .method("CREDIT_CARD")
                    .cardNumber("1234567890123456")
                    .expiryDate("12/25")
                    .cvv(null)
                    .build();

            when(chargeRepository.findById(payer.getId())).thenReturn(Optional.of(testCharge));
            when(userRepository.findById(payer.getId())).thenReturn(Optional.of(payer));

            assertThatThrownBy(() -> paymentUseCase.payCharge(payChargeDTO, payer.getId()).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Credit card information is required");

            verify(chargeRepository).findById(payer.getId());
            verify(userRepository).findById(payer.getId());
            verify(authorizerService, never()).authorizePayment();
        }

        @Test
        @DisplayName("GIVEN authorization failure WHEN paying charge by credit card THEN should throw exception")
        void givenAuthorizationFailure_whenPayingChargeByCreditCard_thenShouldThrowException() {

            PayChargeDTO payChargeDTO = PayChargeDTO.builder()
                    .chargeId(payer.getId())
                    .method("CREDIT_CARD")
                    .cardNumber("1234567890123456")
                    .expiryDate("12/25")
                    .cvv("123")
                    .build();

            when(chargeRepository.findById(payer.getId())).thenReturn(Optional.of(testCharge));
            when(userRepository.findById(payer.getId())).thenReturn(Optional.of(payer));
            when(authorizerService.authorizePayment(any(BigDecimal.class))).thenReturn(Mono.just(false));


            assertThatThrownBy(() -> paymentUseCase.payCharge(payChargeDTO, payer.getId()).block())
                    .isInstanceOf(PaymentAuthorizationException.class)
                    .hasMessage("Payment authorization failed");

            verify(chargeRepository).findById(payer.getId());
            verify(userRepository).findById(payer.getId());
            verify(authorizerService).authorizePayment(any(BigDecimal.class));
            verify(paymentRepository, never()).save(any(Payment.class));
        }
    }

    @Nested
    @DisplayName("Deposit Scenarios")
    class DepositScenarios {

        @Test
        @DisplayName("GIVEN valid deposit data WHEN processing deposit THEN should process deposit successfully")
        void givenValidDepositData_whenProcessingDeposit_thenShouldProcessDepositSuccessfully() {

            DepositDTO depositDTO = DepositDTO.builder()
                    .amount(new BigDecimal("200.00"))
                    .build();

            when(userRepository.findById(payer.getId())).thenReturn(Optional.of(payer));
            when(authorizerService.authorizeDeposit(any(BigDecimal.class))).thenReturn(Mono.just(true));
            when(userRepository.save(any(User.class))).thenReturn(payer);

            Payment depositPayment = Payment.builder()
                    .id(UUID.randomUUID())
                    .charge(null)
                    .payer(payer)
                    .amount(new BigDecimal("200.00"))
                    .method(Payment.PaymentMethod.BALANCE)
                    .paymentDate(LocalDateTime.now())
                    .build();
            when(paymentRepository.save(any(Payment.class))).thenReturn(depositPayment);

            PaymentDTO result = paymentUseCase.deposit(depositDTO, payer.getId()).block();

            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualTo(depositDTO.getAmount());
            assertThat(result.getMethod()).isEqualTo("BALANCE");
            assertThat(result.getPayerId()).isEqualTo(payer.getId());

            verify(userRepository).findById(payer.getId());
            verify(authorizerService).authorizeDeposit(any(BigDecimal.class));
            verify(userRepository).save(any(User.class));
            verify(paymentRepository).save(any(Payment.class));
        }

        @Test
        @DisplayName("GIVEN authorization failure WHEN processing deposit THEN should throw exception")
        void givenAuthorizationFailure_whenProcessingDeposit_thenShouldThrowException() {

            DepositDTO depositDTO = DepositDTO.builder()
                    .amount(new BigDecimal("200.00"))
                    .build();

            when(userRepository.findById(payer.getId())).thenReturn(Optional.of(payer));
            when(authorizerService.authorizeDeposit(any(BigDecimal.class))).thenReturn(Mono.just(false));

            assertThatThrownBy(() -> paymentUseCase.deposit(depositDTO, payer.getId()).block())
                    .isInstanceOf(PaymentAuthorizationException.class)
                    .hasMessage("Deposit authorization failed");

            verify(userRepository).findById(payer.getId());
            verify(authorizerService).authorizeDeposit(any(BigDecimal.class));
            verify(userRepository, never()).save(any(User.class));
            verify(paymentRepository, never()).save(any(Payment.class));
        }

        @Test
        @DisplayName("GIVEN non-existing user WHEN processing deposit THEN should throw exception")
        void givenNonExistingUser_whenProcessingDeposit_thenShouldThrowException() {

            DepositDTO depositDTO = DepositDTO.builder()
                    .amount(new BigDecimal("200.00"))
                    .build();

            UUID nonExistentUserId = UUID.randomUUID();
            when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentUseCase.deposit(depositDTO, nonExistentUserId).block())
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found");

            verify(userRepository).findById(nonExistentUserId);
            verify(authorizerService, never()).authorizeDeposit();
        }
    }
}