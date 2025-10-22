package com.nimble.gateway.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayChargeDTO {
    
    @NotNull(message = "Charge ID is required")
    private UUID chargeId;
    
    @NotBlank(message = "Payment method is required")
    @Pattern(regexp = "BALANCE|CREDIT_CARD", message = "Payment method must be BALANCE or CREDIT_CARD")
    private String method;
    
    @Size(max = 19, message = "Card number must not exceed 19 characters")
    private String cardNumber;
    
    @Size(max = 5, message = "Expiry date must not exceed 5 characters (MM/YY)")
    private String expiryDate;
    
    @Size(max = 4, message = "CVV must not exceed 4 characters")
    private String cvv;
}