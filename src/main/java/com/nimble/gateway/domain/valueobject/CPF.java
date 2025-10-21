package com.nimble.gateway.domain.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.regex.Pattern;

@Getter
@EqualsAndHashCode
public final class CPF {
    
    private static final Pattern CPF_PATTERN = Pattern.compile("^\\d{11}$");
    private static final int[] WEIGHTS_FIRST_DIGIT = {10, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] WEIGHTS_SECOND_DIGIT = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};
    
    private final String value;
    
    public CPF(String cpf) {
        if (cpf == null || cpf.trim().isEmpty()) throw new IllegalArgumentException("CPF cannot be null or empty");
        
        String cleanCpf = cpf.replaceAll("\\D", "");
        
        if (!CPF_PATTERN.matcher(cleanCpf).matches()) throw new IllegalArgumentException("CPF must contain exactly 11 digits");
        if (isAllSameDigits(cleanCpf)) throw new IllegalArgumentException("CPF cannot have all same digits");
        if (!isValidCpf(cleanCpf)) throw new IllegalArgumentException("Invalid CPF");

        this.value = cleanCpf;
    }
    
    private boolean isAllSameDigits(String cpf) {
        return cpf.chars().allMatch(c -> c == cpf.charAt(0));
    }
    
    private boolean isValidCpf(String cpf) {
        return isValidFirstDigit(cpf) && isValidSecondDigit(cpf);
    }
    
    private boolean isValidFirstDigit(String cpf) {
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += Character.getNumericValue(cpf.charAt(i)) * WEIGHTS_FIRST_DIGIT[i];
        }
        int remainder = sum % 11;
        int firstDigit = remainder < 2 ? 0 : 11 - remainder;
        return firstDigit == Character.getNumericValue(cpf.charAt(9));
    }
    
    private boolean isValidSecondDigit(String cpf) {
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += Character.getNumericValue(cpf.charAt(i)) * WEIGHTS_SECOND_DIGIT[i];
        }
        int remainder = sum % 11;
        int secondDigit = remainder < 2 ? 0 : 11 - remainder;
        return secondDigit == Character.getNumericValue(cpf.charAt(10));
    }
    
    public String getFormatted() {
        return String.format("%s.%s.%s-%s", 
            value.substring(0, 3),
            value.substring(3, 6),
            value.substring(6, 9),
            value.substring(9, 11)
        );
    }
    
    @Override
    public String toString() {
        return getFormatted();
    }
}