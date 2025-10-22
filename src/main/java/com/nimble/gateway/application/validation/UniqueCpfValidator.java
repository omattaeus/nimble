package com.nimble.gateway.application.validation;

import com.nimble.gateway.domain.repository.UserRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UniqueCpfValidator implements ConstraintValidator<UniqueCpf, String> {
    
    private final UserRepository userRepository;
    
    @Override
    public boolean isValid(String cpf, ConstraintValidatorContext context) {
        if (cpf == null || cpf.trim().isEmpty()) {
            return true; // Let @NotBlank handle null/empty validation
        }
        
        return !userRepository.existsByCpf(cpf);
    }
}
