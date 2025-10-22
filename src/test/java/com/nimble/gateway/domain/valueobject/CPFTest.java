package com.nimble.gateway.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CPFTest {
    
    @Test
    void shouldCreateValidCPF() {
        CPF cpf = new CPF("11144477735");
        assertEquals("11144477735", cpf.getValue());
        assertEquals("111.444.777-35", cpf.getFormatted());
    }
    
    @Test
    void shouldFormatCPFWithDotsAndDash() {
        CPF cpf = new CPF("11144477735");
        assertEquals("111.444.777-35", cpf.getFormatted());
    }
    
    @Test
    void shouldRemoveNonNumericCharacters() {
        CPF cpf = new CPF("111.444.777-35");
        assertEquals("11144477735", cpf.getValue());
    }
    
    @Test
    void shouldRejectAllSameDigits() {
        assertThrows(IllegalArgumentException.class, () -> new CPF("00000000000"));
        assertThrows(IllegalArgumentException.class, () -> new CPF("11111111111"));
        assertThrows(IllegalArgumentException.class, () -> new CPF("22222222222"));
    }
    
    @Test
    void shouldRejectInvalidLength() {
        assertThrows(IllegalArgumentException.class, () -> new CPF("1234567890"));  // 10 digits
        assertThrows(IllegalArgumentException.class, () -> new CPF("123456789012")); // 12 digits
        assertThrows(IllegalArgumentException.class, () -> new CPF("123456789"));    // 9 digits
        assertThrows(IllegalArgumentException.class, () -> new CPF(""));             // empty
        assertThrows(IllegalArgumentException.class, () -> new CPF("abc123456789")); // contains letters
    }
    
    @Test
    void shouldRejectInvalidCPF() {
        assertThrows(IllegalArgumentException.class, () -> new CPF("12345678900"));
        assertThrows(IllegalArgumentException.class, () -> new CPF("98765432101"));
        assertThrows(IllegalArgumentException.class, () -> new CPF("11144477700"));
    }
    
    @Test
    void shouldRejectNullCPF() {
        assertThrows(IllegalArgumentException.class, () -> new CPF(null));
    }
    
    @Test
    void shouldRejectEmptyCPF() {
        assertThrows(IllegalArgumentException.class, () -> new CPF(""));
    }
    
    @Test
    void shouldRejectWhitespaceOnlyCPF() {
        assertThrows(IllegalArgumentException.class, () -> new CPF("   "));
    }
}