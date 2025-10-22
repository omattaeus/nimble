package com.nimble.gateway.application.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UniqueCpfValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueCpf {
    String message() default "CPF already exists";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
