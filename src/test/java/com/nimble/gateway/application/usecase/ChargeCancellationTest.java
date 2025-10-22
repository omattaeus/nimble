package com.nimble.gateway.application.usecase;

import com.nimble.gateway.domain.entity.Charge;
import com.nimble.gateway.domain.entity.Payment;
import com.nimble.gateway.domain.entity.User;
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

import com.nimble.gateway.domain.exception.ConflictException;
import com.nimble.gateway.domain.exception.PaymentAuthorizationException;
import com.nimble.gateway.domain.exception.UserNotFoundException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Charge Cancellation - BDD Tests")
class ChargeCancellationTest {

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

    private User originator;
    private User recipient;
    private Charge pendingCharge;
    private Charge paidCharge;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        originator = User.builder()
                .id(UUID.randomUUID())
                .name("Originator User")
                .cpf("11122233344")
                .email("originator@test.com")
                .password("password")
                .balance(new BigDecimal("1000.00"))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        recipient = User.builder()
                .id(UUID.randomUUID())
                .name("Recipient User")
                .cpf("55566677788")
                .email("recipient@test.com")
                .password("password")
                .balance(new BigDecimal("500.00"))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        pendingCharge = Charge.builder()
                .id(UUID.randomUUID())
                .originator(originator)
                .recipient(recipient)
                .amount(new BigDecimal("100.00"))
                .description("Pending charge")
                .status(Charge.ChargeStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        paidCharge = Charge.builder()
                .id(UUID.randomUUID())
                .originator(originator)
                .recipient(recipient)
                .amount(new BigDecimal("200.00"))
                .description("Paid charge")
                .status(Charge.ChargeStatus.PAID)
                .paymentMethod(Charge.PaymentMethod.BALANCE)
                .createdAt(LocalDateTime.now())
                .build();

        testPayment = Payment.builder()
                .id(UUID.randomUUID())
                .charge(paidCharge)
                .payer(recipient)
                .amount(new BigDecimal("200.00"))
                .method(Payment.PaymentMethod.BALANCE)
                .paymentDate(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Pending Charge Cancellation Scenarios")
    class PendingChargeCancellationScenarios {

        @Test
        @DisplayName("GIVEN pending charge WHEN originator cancels charge THEN should cancel successfully")
        void givenPendingCharge_whenOriginatorCancelsCharge_thenShouldCancelSuccessfully() {

            UUID chargeId = pendingCharge.getId();
            UUID originatorId = originator.getId();

            when(chargeRepository.findById(chargeId)).thenReturn(Optional.of(pendingCharge));
            when(chargeRepository.save(any(Charge.class))).thenReturn(pendingCharge);

            paymentUseCase.cancelCharge(chargeId, originatorId).block();

            verify(chargeRepository).findById(chargeId);
            verify(chargeRepository).save(any(Charge.class));
            verify(authorizerService, never()).authorizeCancellation(any(BigDecimal.class));
        }

        @Test
        @DisplayName("GIVEN pending charge WHEN non-originator tries to cancel THEN should throw exception")
        void givenPendingCharge_whenNonOriginatorTriesToCancel_thenShouldThrowException() {

            UUID chargeId = pendingCharge.getId();
            UUID nonOriginatorId = recipient.getId();

            when(chargeRepository.findById(chargeId)).thenReturn(Optional.of(pendingCharge));


            assertThatThrownBy(() -> paymentUseCase.cancelCharge(chargeId, nonOriginatorId).block())
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Only the originator can cancel this charge");

            verify(chargeRepository).findById(chargeId);
            verify(chargeRepository, never()).save(any(Charge.class));
        }

        @Test
        @DisplayName("GIVEN non-existing charge WHEN trying to cancel THEN should throw exception")
        void givenNonExistingCharge_whenTryingToCancel_thenShouldThrowException() {

            UUID chargeId = UUID.randomUUID();
            UUID originatorId = originator.getId();

            when(chargeRepository.findById(chargeId)).thenReturn(Optional.empty());


            assertThatThrownBy(() -> paymentUseCase.cancelCharge(chargeId, originatorId).block())
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("Charge not found");

            verify(chargeRepository).findById(chargeId);
            verify(chargeRepository, never()).save(any(Charge.class));
        }
    }

    @Nested
    @DisplayName("Paid Charge Cancellation Scenarios")
    class PaidChargeCancellationScenarios {

        @Test
        @DisplayName("GIVEN paid charge with balance payment WHEN originator cancels THEN should refund and cancel")
        void givenPaidChargeWithBalancePayment_whenOriginatorCancels_thenShouldRefundAndCancel() {

            UUID chargeId = recipient.getId();
            UUID originatorId = originator.getId();

            when(chargeRepository.findById(chargeId)).thenReturn(Optional.of(paidCharge));
            when(paymentRepository.findByCharge(paidCharge)).thenReturn(Optional.of(testPayment));
            when(userRepository.save(any(User.class))).thenReturn(recipient);
            when(chargeRepository.save(any(Charge.class))).thenReturn(paidCharge);


            paymentUseCase.cancelCharge(chargeId, originatorId).block();


            verify(chargeRepository).findById(chargeId);
            verify(paymentRepository).findByCharge(paidCharge);
            verify(userRepository, atLeast(2)).save(any(User.class));
            verify(chargeRepository).save(any(Charge.class));
            verify(authorizerService, never()).authorizeCancellation(any(BigDecimal.class));
        }

        @Test
        @DisplayName("GIVEN paid charge with credit card payment WHEN originator cancels THEN should authorize and cancel")
        void givenPaidChargeWithCreditCardPayment_whenOriginatorCancels_thenShouldAuthorizeAndCancel() {

            Charge creditCardCharge = Charge.builder()
                    .id(UUID.randomUUID())
                    .originator(originator)
                    .recipient(recipient)
                    .amount(new BigDecimal("300.00"))
                    .description("Credit card charge")
                    .status(Charge.ChargeStatus.PAID)
                    .paymentMethod(Charge.PaymentMethod.CREDIT_CARD)
                    .createdAt(LocalDateTime.now())
                    .build();

            UUID chargeId = UUID.randomUUID();
            UUID originatorId = originator.getId();

            when(chargeRepository.findById(chargeId)).thenReturn(Optional.of(creditCardCharge));
            when(authorizerService.authorizeCancellation(any(BigDecimal.class))).thenReturn(Mono.just(true));
            when(chargeRepository.save(any(Charge.class))).thenReturn(creditCardCharge);


            paymentUseCase.cancelCharge(chargeId, originatorId).block();


            verify(chargeRepository).findById(chargeId);
            verify(authorizerService).authorizeCancellation(any(BigDecimal.class));
            verify(chargeRepository).save(any(Charge.class));
            verify(paymentRepository, never()).findByCharge(any(Charge.class));
        }

        @Test
        @DisplayName("GIVEN paid charge with credit card payment WHEN authorization fails THEN should throw exception")
        void givenPaidChargeWithCreditCardPayment_whenAuthorizationFails_thenShouldThrowException() {

            Charge creditCardCharge = Charge.builder()
                    .id(UUID.randomUUID())
                    .originator(originator)
                    .recipient(recipient)
                    .amount(new BigDecimal("300.00"))
                    .description("Credit card charge")
                    .status(Charge.ChargeStatus.PAID)
                    .paymentMethod(Charge.PaymentMethod.CREDIT_CARD)
                    .createdAt(LocalDateTime.now())
                    .build();

            UUID chargeId = UUID.randomUUID();
            UUID originatorId = originator.getId();

            when(chargeRepository.findById(chargeId)).thenReturn(Optional.of(creditCardCharge));
            when(authorizerService.authorizeCancellation(any(BigDecimal.class))).thenReturn(Mono.just(false));


            assertThatThrownBy(() -> paymentUseCase.cancelCharge(chargeId, originatorId).block())
                    .isInstanceOf(PaymentAuthorizationException.class)
                    .hasMessage("Cancellation authorization failed");

            verify(chargeRepository).findById(chargeId);
            verify(authorizerService).authorizeCancellation(any(BigDecimal.class));
            verify(chargeRepository, never()).save(any(Charge.class));
        }
    }

    @Nested
    @DisplayName("Already Cancelled Charge Scenarios")
    class AlreadyCancelledChargeScenarios {

        @Test
        @DisplayName("GIVEN already cancelled charge WHEN trying to cancel THEN should throw exception")
        void givenAlreadyCancelledCharge_whenTryingToCancel_thenShouldThrowException() {

            Charge cancelledCharge = Charge.builder()
                    .id(UUID.randomUUID())
                    .originator(originator)
                    .recipient(recipient)
                    .amount(new BigDecimal("150.00"))
                    .description("Cancelled charge")
                    .status(Charge.ChargeStatus.CANCELLED)
                    .createdAt(LocalDateTime.now())
                    .cancelledAt(LocalDateTime.now())
                    .build();

            UUID chargeId = UUID.randomUUID();
            UUID originatorId = originator.getId();

            when(chargeRepository.findById(chargeId)).thenReturn(Optional.of(cancelledCharge));


            assertThatThrownBy(() -> paymentUseCase.cancelCharge(chargeId, originatorId).block())
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Charge is already cancelled");

            verify(chargeRepository).findById(chargeId);
            verify(chargeRepository, never()).save(any(Charge.class));
        }
    }

    @Nested
    @DisplayName("Refund Scenarios")
    class RefundScenarios {

        @Test
        @DisplayName("GIVEN payment not found WHEN cancelling paid charge THEN should throw exception")
        void givenPaymentNotFound_whenCancellingPaidCharge_thenShouldThrowException() {

            UUID chargeId = recipient.getId();
            UUID originatorId = originator.getId();

            when(chargeRepository.findById(chargeId)).thenReturn(Optional.of(paidCharge));
            when(paymentRepository.findByCharge(paidCharge)).thenReturn(Optional.empty());


            assertThatThrownBy(() -> paymentUseCase.cancelCharge(chargeId, originatorId).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Payment not found");

            verify(chargeRepository).findById(chargeId);
            verify(paymentRepository).findByCharge(paidCharge);
            verify(userRepository, never()).save(any(User.class));
            verify(chargeRepository, never()).save(any(Charge.class));
        }
    }
}