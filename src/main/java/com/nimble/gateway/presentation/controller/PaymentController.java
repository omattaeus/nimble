package com.nimble.gateway.presentation.controller;

import com.nimble.gateway.application.dto.DepositDTO;
import com.nimble.gateway.application.dto.PayChargeDTO;
import com.nimble.gateway.application.dto.PaymentDTO;
import com.nimble.gateway.application.usecase.PaymentUseCase;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "payment-controller")
@SecurityRequirement(name = "bearer-key")
public class PaymentController {
    
    private final PaymentUseCase paymentUseCase;
    
    @PostMapping("/pay")
    public ResponseEntity<PaymentDTO> payCharge(
            @Valid @RequestBody PayChargeDTO payChargeDTO,
            @RequestParam UUID payerId) {
        
        log.info("Processing payment for charge {} by user {}", payChargeDTO.getChargeId(), payerId);
        
        PaymentDTO paymentDTO = paymentUseCase.payCharge(payChargeDTO, payerId).block();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentDTO);
    }
    
    @PostMapping("/deposit")
    public ResponseEntity<PaymentDTO> deposit(
            @Valid @RequestBody DepositDTO depositDTO,
            @RequestParam UUID userId) {
        
        log.info("Processing deposit of {} for user {}", depositDTO.getAmount(), userId);
        
        PaymentDTO paymentDTO = paymentUseCase.deposit(depositDTO, userId).block();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentDTO);
    }
    
    @PostMapping("/cancel/{chargeId}")
    public ResponseEntity<Void> cancelCharge(
            @PathVariable UUID chargeId,
            @RequestParam UUID userId) {
        
        log.info("Cancelling charge {} by user {}", chargeId, userId);
        
        paymentUseCase.cancelCharge(chargeId, userId).block();
        
        return ResponseEntity.ok().build();
    }
}