package com.nimble.gateway.application.usecase;

import com.nimble.gateway.application.dto.DepositDTO;
import com.nimble.gateway.application.dto.PayChargeDTO;
import com.nimble.gateway.application.dto.PaymentDTO;
import com.nimble.gateway.domain.entity.Charge;
import com.nimble.gateway.domain.entity.Payment;
import com.nimble.gateway.domain.entity.User;
import com.nimble.gateway.domain.exception.InsufficientBalanceException;
import com.nimble.gateway.domain.exception.UserNotFoundException;
import com.nimble.gateway.domain.exception.ConflictException;
import com.nimble.gateway.domain.exception.PaymentAuthorizationException;
import com.nimble.gateway.domain.repository.ChargeRepository;
import com.nimble.gateway.domain.repository.PaymentRepository;
import com.nimble.gateway.domain.repository.UserRepository;
import com.nimble.gateway.infrastructure.external.AuthorizerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentUseCase {
    
    private final ChargeRepository chargeRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final AuthorizerService authorizerService;
    
    @Transactional
    public Mono<PaymentDTO> payCharge(PayChargeDTO payChargeDTO, UUID payerId) {
        log.info("Processing payment for charge {} by user {}", payChargeDTO.getChargeId(), payerId);
        
        return Mono.fromCallable(() -> chargeRepository.findById(payChargeDTO.getChargeId()))
                .flatMap(chargeOpt -> {
                    if (chargeOpt.isEmpty()) return Mono.error(new IllegalArgumentException("Charge not found"));
                    return Mono.just(chargeOpt.get());
                })
                .flatMap(charge -> {
                    if (charge.getStatus() != Charge.ChargeStatus.PENDING) {
                        return Mono.error(new IllegalArgumentException("Charge is not pending"));
                    }
                    return Mono.just(charge);
                })
                .flatMap(charge -> Mono.fromCallable(() -> userRepository.findById(payerId))
                        .flatMap(payerOpt -> {
                            if (payerOpt.isEmpty()) return Mono.error(new UserNotFoundException("Payer not found"));
                            return Mono.just(payerOpt.get());
                        })
                        .flatMap(payer -> {
                            if (!charge.getRecipient().getId().equals(payerId)) {
                                return Mono.error(new IllegalArgumentException("Only the recipient can pay this charge"));
                            }
                            return Mono.just(payer);
                        })
                        .flatMap(payer -> {
                            Payment.PaymentMethod method = Payment.PaymentMethod.valueOf(payChargeDTO.getMethod());
                            
                            if (method == Payment.PaymentMethod.BALANCE) {
                                return payWithBalance(charge, payer);
                            } else if (method == Payment.PaymentMethod.CREDIT_CARD) {
                                return payWithCreditCard(charge, payer, payChargeDTO);
                            } else {
                                return Mono.error(new IllegalArgumentException("Invalid payment method"));
                            }
                        }));
    }
    
    @Transactional
    public Mono<PaymentDTO> deposit(DepositDTO depositDTO, UUID userId) {
        log.info("Processing deposit of {} for user {}", depositDTO.getAmount(), userId);
        
        return Mono.fromCallable(() -> userRepository.findById(userId))
                .flatMap(userOpt -> {
                    if (userOpt.isEmpty()) return Mono.error(new UserNotFoundException("User not found"));
                    return Mono.just(userOpt.get());
                })
                .flatMap(user -> authorizerService.authorizeDeposit(depositDTO.getAmount())
                        .flatMap(authorized -> {
                            if (authorized) {
                                return processDeposit(user, depositDTO.getAmount());
                            } else {
                                log.warn("Deposit authorization failed for user {}", userId);
                                return Mono.error(new PaymentAuthorizationException("Deposit authorization failed"));
                            }
                        }));
    }
    
    @Transactional
    public Mono<Void> cancelCharge(UUID chargeId, UUID userId) {
        log.info("Cancelling charge {} by user {}", chargeId, userId);
        
        return Mono.fromCallable(() -> chargeRepository.findById(chargeId))
                .flatMap(chargeOpt -> {
                    if (chargeOpt.isEmpty()) return Mono.error(new UserNotFoundException("Charge not found"));
                    return Mono.just(chargeOpt.get());
                })
                .flatMap(charge -> {
                    if (!charge.getOriginator().getId().equals(userId)) {
                        return Mono.error(new ConflictException("Only the originator can cancel this charge"));
                    }
                    return Mono.just(charge);
                })
                .flatMap(charge -> {
                    if (charge.getStatus() == Charge.ChargeStatus.CANCELLED) {
                        return Mono.error(new ConflictException("Charge is already cancelled"));
                    }
                    return Mono.just(charge);
                })
                .flatMap(charge -> {
                    if (charge.getStatus() == Charge.ChargeStatus.PAID) {
                        if (charge.getPaymentMethod() == Charge.PaymentMethod.BALANCE) {
                            return Mono.fromRunnable(() -> refundPayment(charge))
                                    .then(Mono.just(charge));
                        } else if (charge.getPaymentMethod() == Charge.PaymentMethod.CREDIT_CARD) {
                            return authorizerService.authorizeCancellation(charge.getAmount())
                                    .flatMap(authorized -> {
                                        if (authorized) {
                                            return Mono.just(charge);
                                        } else {
                                            return Mono.error(new PaymentAuthorizationException("Cancellation authorization failed"));
                                        }
                                    });
                        }
                    }
                    return Mono.just(charge);
                })
                .flatMap(charge -> {
                    charge.cancel();
                    chargeRepository.save(charge);
                    log.info("Charge {} cancelled successfully", chargeId);
                    return Mono.empty();
                });
    }
    
    private Mono<PaymentDTO> payWithBalance(Charge charge, User payer) {
        return Mono.fromCallable(() -> {
            if (payer.getBalance().compareTo(charge.getAmount()) < 0) throw new InsufficientBalanceException("Insufficient balance");
            return null;
        })
        .then(Mono.fromRunnable(() -> {
            payer.setBalance(payer.getBalance().subtract(charge.getAmount()));
            payer.setUpdatedAt(LocalDateTime.now());
            
            User recipient = charge.getRecipient();
            recipient.setBalance(recipient.getBalance().add(charge.getAmount()));
            recipient.setUpdatedAt(LocalDateTime.now());
            
            userRepository.save(payer);
            userRepository.save(recipient);
            
            charge.setStatus(Charge.ChargeStatus.PAID);
            charge.setPaidAt(LocalDateTime.now());
            charge.setPaymentMethod(Charge.PaymentMethod.BALANCE);
            chargeRepository.save(charge);
        }))
        .then(Mono.fromCallable(() -> {
            Payment payment = Payment.builder()
                    .charge(charge)
                    .payer(payer)
                    .amount(charge.getAmount())
                    .method(Payment.PaymentMethod.BALANCE)
                    .paymentDate(LocalDateTime.now())
                    .build();
            
            return paymentRepository.save(payment);
        }))
        .map(savedPayment -> {
            log.info("Payment with balance successful for charge {}", charge.getId());
            
            return PaymentDTO.builder()
                    .id(savedPayment.getId())
                    .chargeId(savedPayment.getCharge().getId())
                    .payerId(savedPayment.getPayer().getId())
                    .payerName(savedPayment.getPayer().getName())
                    .amount(savedPayment.getAmount())
                    .method(savedPayment.getMethod().name())
                    .paymentDate(savedPayment.getPaymentDate())
                    .build();
        });
    }
    
    private Mono<PaymentDTO> payWithCreditCard(Charge charge, User payer, PayChargeDTO payChargeDTO) {
        return Mono.defer(() -> {
            if (payChargeDTO.getCardNumber() == null || payChargeDTO.getExpiryDate() == null || payChargeDTO.getCvv() == null) {
                return Mono.error(new IllegalArgumentException("Credit card information is required"));
            }
            return authorizerService.authorizePayment(charge.getAmount());
        })
        .flatMap(authorized -> {
            if (!authorized) {
                return Mono.error(new PaymentAuthorizationException("Payment authorization failed"));
            }
            return Mono.just(authorized);
        })
        .then(Mono.fromRunnable(() -> {
            User recipient = charge.getRecipient();
            recipient.setBalance(recipient.getBalance().add(charge.getAmount()));
            recipient.setUpdatedAt(LocalDateTime.now());
            userRepository.save(recipient);
            
            charge.setStatus(Charge.ChargeStatus.PAID);
            charge.setPaidAt(LocalDateTime.now());
            charge.setPaymentMethod(Charge.PaymentMethod.CREDIT_CARD);
            chargeRepository.save(charge);
        }))
        .then(Mono.fromCallable(() -> {
            Payment payment = Payment.builder()
                    .charge(charge)
                    .payer(payer)
                    .amount(charge.getAmount())
                    .method(Payment.PaymentMethod.CREDIT_CARD)
                    .paymentDate(LocalDateTime.now())
                    .build();
            
            return paymentRepository.save(payment);
        }))
        .map(savedPayment -> {
            log.info("Payment with credit card successful for charge {}", charge.getId());
            
            return PaymentDTO.builder()
                    .id(savedPayment.getId())
                    .chargeId(savedPayment.getCharge().getId())
                    .payerId(savedPayment.getPayer().getId())
                    .payerName(savedPayment.getPayer().getName())
                    .amount(savedPayment.getAmount())
                    .method(savedPayment.getMethod().name())
                    .paymentDate(savedPayment.getPaymentDate())
                    .build();
        });
    }
    
    private Mono<PaymentDTO> processDeposit(User user, java.math.BigDecimal amount) {
        return Mono.fromRunnable(() -> {
            user.setBalance(user.getBalance().add(amount));
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        })
        .then(Mono.fromCallable(() -> {
            Payment payment = Payment.builder()
                    .payer(user)
                    .amount(amount)
                    .method(Payment.PaymentMethod.BALANCE)
                    .paymentDate(LocalDateTime.now())
                    .build();
            
            return paymentRepository.save(payment);
        }))
        .map(savedPayment -> {
            log.info("Deposit successful for user {}: {}", user.getId(), amount);
            
            return PaymentDTO.builder()
                    .id(savedPayment.getId())
                    .payerId(savedPayment.getPayer().getId())
                    .payerName(savedPayment.getPayer().getName())
                    .amount(savedPayment.getAmount())
                    .method(savedPayment.getMethod().name())
                    .paymentDate(savedPayment.getPaymentDate())
                    .build();
        });
    }
    
    private void refundPayment(Charge charge) {
        Payment payment = paymentRepository.findByCharge(charge)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        
        if (payment.getMethod() == Payment.PaymentMethod.BALANCE) {
            User payer = payment.getPayer();
            payer.setBalance(payer.getBalance().add(payment.getAmount()));
            payer.setUpdatedAt(LocalDateTime.now());
            userRepository.save(payer);
            
            User recipient = charge.getRecipient();
            recipient.setBalance(recipient.getBalance().subtract(payment.getAmount()));
            recipient.setUpdatedAt(LocalDateTime.now());
            userRepository.save(recipient);
            
            log.info("Payment refunded for charge {}", charge.getId());
        }
    }
}