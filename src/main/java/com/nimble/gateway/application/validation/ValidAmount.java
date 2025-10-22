package com.nimble.gateway.application.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = AmountValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAmount {
    String message() default "Invalid amount";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    double min() default 0.01;
    double max() default 100000.00;
}
