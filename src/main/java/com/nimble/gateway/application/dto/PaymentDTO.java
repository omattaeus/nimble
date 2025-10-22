package com.nimble.gateway.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {
    private Long id;
    private Long chargeId;
    private Long payerId;
    private String payerName;
    private BigDecimal amount;
    private String method;
    private LocalDateTime paymentDate;
}