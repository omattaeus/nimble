package com.nimble.gateway.presentation.controller;

import com.nimble.gateway.application.dto.DepositDTO;
import com.nimble.gateway.application.dto.PayChargeDTO;
import com.nimble.gateway.application.dto.PaymentDTO;
import com.nimble.gateway.application.usecase.PaymentUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentUseCase paymentUseCase;
    
    @PostMapping("/pay")
    public ResponseEntity<PaymentDTO> payCharge(
            @Valid @RequestBody PayChargeDTO payChargeDTO,
            @RequestParam Long payerId) {
        
        log.info("Processing payment for charge {} by user {}", payChargeDTO.getChargeId(), payerId);
        
        PaymentDTO paymentDTO = paymentUseCase.payCharge(payChargeDTO, payerId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentDTO);
    }
    
    @PostMapping("/deposit")
    public ResponseEntity<PaymentDTO> deposit(
            @Valid @RequestBody DepositDTO depositDTO,
            @RequestParam Long userId) {
        
        log.info("Processing deposit of {} for user {}", depositDTO.getAmount(), userId);
        
        PaymentDTO paymentDTO = paymentUseCase.deposit(depositDTO, userId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentDTO);
    }
    
    @PostMapping("/cancel/{chargeId}")
    public ResponseEntity<Void> cancelCharge(
            @PathVariable Long chargeId,
            @RequestParam Long userId) {
        
        log.info("Cancelling charge {} by user {}", chargeId, userId);
        
        paymentUseCase.cancelCharge(chargeId, userId);
        
        return ResponseEntity.ok().build();
    }
}