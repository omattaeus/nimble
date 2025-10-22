package com.nimble.gateway.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.nimble.gateway.application.validation.ValidAmount;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositDTO {
    
    @NotNull(message = "Amount is required")
    @ValidAmount(min = 0.01, max = 50000.00, message = "Deposit amount must be between 0.01 and 50,000.00")
    private BigDecimal amount;
}