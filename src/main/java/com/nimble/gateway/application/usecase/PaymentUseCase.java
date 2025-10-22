package com.nimble.gateway.application.usecase;

import com.nimble.gateway.application.dto.DepositDTO;
import com.nimble.gateway.application.dto.PayChargeDTO;
import com.nimble.gateway.application.dto.PaymentDTO;
import com.nimble.gateway.domain.entity.Charge;
import com.nimble.gateway.domain.entity.Payment;
import com.nimble.gateway.domain.entity.User;
import com.nimble.gateway.domain.exception.InsufficientBalanceException;
import com.nimble.gateway.domain.exception.UserNotFoundException;
import com.nimble.gateway.domain.repository.ChargeRepository;
import com.nimble.gateway.domain.repository.PaymentRepository;
import com.nimble.gateway.domain.repository.UserRepository;
import com.nimble.gateway.infrastructure.external.AuthorizerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentUseCase {
    
    private final ChargeRepository chargeRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final AuthorizerService authorizerService;
    
    @Transactional
    public PaymentDTO payCharge(PayChargeDTO payChargeDTO, Long payerId) {
        log.info("Processing payment for charge {} by user {}", payChargeDTO.getChargeId(), payerId);
        
        Charge charge = chargeRepository.findById(payChargeDTO.getChargeId())
                .orElseThrow(() -> new IllegalArgumentException("Charge not found"));
        
        if (charge.getStatus() != Charge.ChargeStatus.PENDING) throw new IllegalArgumentException("Charge is not pending");
        
        User payer = userRepository.findById(payerId)
                .orElseThrow(() -> new UserNotFoundException("Payer not found"));
        
        if (!charge.getRecipient().getId().equals(payerId)) throw new IllegalArgumentException("Only the recipient can pay this charge");
        
        Payment.PaymentMethod method = Payment.PaymentMethod.valueOf(payChargeDTO.getMethod());
        
        if (method == Payment.PaymentMethod.BALANCE) {
            return payWithBalance(charge, payer);
        } else if (method == Payment.PaymentMethod.CREDIT_CARD) {
            return payWithCreditCard(charge, payer, payChargeDTO);
        } else {
            throw new IllegalArgumentException("Invalid payment method");
        }
    }
    
    @Transactional
    public PaymentDTO deposit(DepositDTO depositDTO, Long userId) {
        log.info("Processing deposit of {} for user {}", depositDTO.getAmount(), userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        boolean authorized = authorizerService.authorizeDeposit().block();
        if (authorized) {
            user.setBalance(user.getBalance().add(depositDTO.getAmount()));
            user.setUpdatedAt(LocalDateTime.now());
            User updatedUser = userRepository.save(user);
            
            Payment payment = Payment.builder()
                    .payer(updatedUser)
                    .amount(depositDTO.getAmount())
                    .method(Payment.PaymentMethod.BALANCE)
                    .paymentDate(LocalDateTime.now())
                    .build();
            
            Payment savedPayment = paymentRepository.save(payment);
            
            log.info("Deposit successful for user {}: {}", userId, depositDTO.getAmount());
            
            return PaymentDTO.builder()
                    .id(savedPayment.getId())
                    .payerId(savedPayment.getPayer().getId())
                    .payerName(savedPayment.getPayer().getName())
                    .amount(savedPayment.getAmount())
                    .method(savedPayment.getMethod().name())
                    .paymentDate(savedPayment.getPaymentDate())
                    .build();
        } else {
            log.warn("Deposit authorization failed for user {}", userId);
            throw new IllegalArgumentException("Deposit authorization failed");
        }
    }
    
    @Transactional
    public void cancelCharge(Long chargeId, Long userId) {
        log.info("Cancelling charge {} by user {}", chargeId, userId);
        
        Charge charge = chargeRepository.findById(chargeId)
                .orElseThrow(() -> new IllegalArgumentException("Charge not found"));
        
        if (!charge.getOriginator().getId().equals(userId)) throw new IllegalArgumentException("Only the originator can cancel this charge");
        if (charge.getStatus() == Charge.ChargeStatus.CANCELLED) throw new IllegalArgumentException("Charge is already cancelled");
        
        if (charge.getStatus() == Charge.ChargeStatus.PAID) {
            if (charge.getPaymentMethod() == Charge.PaymentMethod.BALANCE) {
                refundPayment(charge);
            } else if (charge.getPaymentMethod() == Charge.PaymentMethod.CREDIT_CARD) {
                boolean authorized = authorizerService.authorizeCancellation().block();
                if (!authorized) {
                    throw new IllegalArgumentException("Cancellation authorization failed");
                }
            }
        }
        
        charge.setStatus(Charge.ChargeStatus.CANCELLED);
        charge.setCancelledAt(LocalDateTime.now());
        chargeRepository.save(charge);
        
        log.info("Charge {} cancelled successfully", chargeId);
    }
    
    private PaymentDTO payWithBalance(Charge charge, User payer) {
        if (payer.getBalance().compareTo(charge.getAmount()) < 0) throw new InsufficientBalanceException("Insufficient balance");
        
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
        
        Payment payment = Payment.builder()
                .charge(charge)
                .payer(payer)
                .amount(charge.getAmount())
                .method(Payment.PaymentMethod.BALANCE)
                .paymentDate(LocalDateTime.now())
                .build();
        
        Payment savedPayment = paymentRepository.save(payment);
        
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
    }
    
    private PaymentDTO payWithCreditCard(Charge charge, User payer, PayChargeDTO payChargeDTO) {
        if (payChargeDTO.getCardNumber() == null || payChargeDTO.getExpiryDate() == null || payChargeDTO.getCvv() == null) {
            throw new IllegalArgumentException("Credit card information is required");
        }
        
        boolean authorized = authorizerService.authorizePayment().block();
        if (!authorized) {
            throw new IllegalArgumentException("Payment authorization failed");
        }
        
        User recipient = charge.getRecipient();
        recipient.setBalance(recipient.getBalance().add(charge.getAmount()));
        recipient.setUpdatedAt(LocalDateTime.now());
        userRepository.save(recipient);
        
        charge.setStatus(Charge.ChargeStatus.PAID);
        charge.setPaidAt(LocalDateTime.now());
        charge.setPaymentMethod(Charge.PaymentMethod.CREDIT_CARD);
        chargeRepository.save(charge);
        
        Payment payment = Payment.builder()
                .charge(charge)
                .payer(payer)
                .amount(charge.getAmount())
                .method(Payment.PaymentMethod.CREDIT_CARD)
                .paymentDate(LocalDateTime.now())
                .build();
        
        Payment savedPayment = paymentRepository.save(payment);
        
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