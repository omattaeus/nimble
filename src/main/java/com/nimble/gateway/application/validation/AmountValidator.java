package com.nimble.gateway.application.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class AmountValidator implements ConstraintValidator<ValidAmount, BigDecimal> {
    
    private double min;
    private double max;
    
    @Override
    public void initialize(ValidAmount constraintAnnotation) {
        this.min = constraintAnnotation.min();
        this.max = constraintAnnotation.max();
    }
    
    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        
        // Check if amount is positive
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        // Check if amount is within limits
        BigDecimal minValue = BigDecimal.valueOf(min);
        BigDecimal maxValue = BigDecimal.valueOf(max);
        
        return value.compareTo(minValue) >= 0 && value.compareTo(maxValue) <= 0;
    }
}
