package com.nimble.gateway.application.validation;

import com.nimble.gateway.application.dto.CreateUserDTO;
import com.nimble.gateway.application.dto.CreateChargeDTO;
import com.nimble.gateway.application.dto.DepositDTO;
import com.nimble.gateway.application.dto.PayChargeDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Validation Rules - BDD Tests")
class ValidationTest {

    private Validator validator;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.byDefaultProvider()
                .configure()
                .constraintValidatorFactory(new jakarta.validation.ConstraintValidatorFactory() {
                    @Override
                    public <T extends jakarta.validation.ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
                        if (key == com.nimble.gateway.application.validation.UniqueCpfValidator.class) {
                            @SuppressWarnings("unchecked")
                            T validator = (T) new com.nimble.gateway.application.validation.UniqueCpfValidator(null) {
                                @Override
                                public boolean isValid(String cpf, jakarta.validation.ConstraintValidatorContext context) {
                                    return true;
                                }
                            };
                            return validator;
                        }
                        try {
                            return key.getDeclaredConstructor().newInstance();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void releaseInstance(jakarta.validation.ConstraintValidator<?, ?> instance) {
                    }
                })
                .buildValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("User Registration Validation Scenarios")
    class UserRegistrationValidationScenarios {

        @Test
        @DisplayName("GIVEN valid user data WHEN validating THEN should pass all validations")
        void givenValidUserData_whenValidating_thenShouldPassAllValidations() {
            // Given
            CreateUserDTO validUser = CreateUserDTO.builder()
                    .name("João Silva")
                    .cpf("12345678901")
                    .email("joao@teste.com")
                    .password("senha123")
                    .build();

            // When
            Set<ConstraintViolation<CreateUserDTO>> violations = validator.validate(validUser);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("GIVEN empty name WHEN validating user THEN should fail validation")
        void givenEmptyName_whenValidatingUser_thenShouldFailValidation() {
            // Given
            CreateUserDTO userWithEmptyName = CreateUserDTO.builder()
                    .name("")
                    .cpf("12345678901")
                    .email("joao@teste.com")
                    .password("senha123")
                    .build();

            // When
            Set<ConstraintViolation<CreateUserDTO>> violations = validator.validate(userWithEmptyName);

            // Then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
        }

        @Test
        @DisplayName("GIVEN invalid CPF format WHEN validating user THEN should fail validation")
        void givenInvalidCpfFormat_whenValidatingUser_thenShouldFailValidation() {
            // Given
            CreateUserDTO userWithInvalidCpf = CreateUserDTO.builder()
                    .name("João Silva")
                    .cpf("123")
                    .email("joao@teste.com")
                    .password("senha123")
                    .build();

            // When
            Set<ConstraintViolation<CreateUserDTO>> violations = validator.validate(userWithInvalidCpf);

            // Then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("cpf"));
        }

        @Test
        @DisplayName("GIVEN invalid email format WHEN validating user THEN should fail validation")
        void givenInvalidEmailFormat_whenValidatingUser_thenShouldFailValidation() {
            // Given
            CreateUserDTO userWithInvalidEmail = CreateUserDTO.builder()
                    .name("João Silva")
                    .cpf("12345678901")
                    .email("invalid-email")
                    .password("senha123")
                    .build();

            // When
            Set<ConstraintViolation<CreateUserDTO>> violations = validator.validate(userWithInvalidEmail);

            // Then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }

        @Test
        @DisplayName("GIVEN short password WHEN validating user THEN should fail validation")
        void givenShortPassword_whenValidatingUser_thenShouldFailValidation() {
            // Given
            CreateUserDTO userWithShortPassword = CreateUserDTO.builder()
                    .name("João Silva")
                    .cpf("12345678901")
                    .email("joao@teste.com")
                    .password("123")
                    .build();

            // When
            Set<ConstraintViolation<CreateUserDTO>> violations = validator.validate(userWithShortPassword);

            // Then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        }

        @Test
        @DisplayName("GIVEN null values WHEN validating user THEN should fail validation")
        void givenNullValues_whenValidatingUser_thenShouldFailValidation() {
            // Given
            CreateUserDTO userWithNulls = CreateUserDTO.builder()
                    .name(null)
                    .cpf(null)
                    .email(null)
                    .password(null)
                    .build();

            // When
            Set<ConstraintViolation<CreateUserDTO>> violations = validator.validate(userWithNulls);

            // Then
            assertThat(violations).isNotEmpty();
            assertThat(violations).hasSize(4);
        }
    }

    @Nested
    @DisplayName("Charge Creation Validation Scenarios")
    class ChargeCreationValidationScenarios {

        @Test
        @DisplayName("GIVEN valid charge data WHEN validating THEN should pass all validations")
        void givenValidChargeData_whenValidating_thenShouldPassAllValidations() {
            // Given
            CreateChargeDTO validCharge = CreateChargeDTO.builder()
                    .recipientCpf("98765432100")
                    .amount(new BigDecimal("100.00"))
                    .description("Pagamento de serviços")
                    .build();

            // When
            Set<ConstraintViolation<CreateChargeDTO>> violations = validator.validate(validCharge);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("GIVEN negative amount WHEN validating charge THEN should fail validation")
        void givenNegativeAmount_whenValidatingCharge_thenShouldFailValidation() {
            // Given
            CreateChargeDTO chargeWithNegativeAmount = CreateChargeDTO.builder()
                    .recipientCpf("98765432100")
                    .amount(new BigDecimal("-100.00"))
                    .description("Pagamento de serviços")
                    .build();

            // When
            Set<ConstraintViolation<CreateChargeDTO>> violations = validator.validate(chargeWithNegativeAmount);

            // Then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("amount"));
        }

        @Test
        @DisplayName("GIVEN zero amount WHEN validating charge THEN should fail validation")
        void givenZeroAmount_whenValidatingCharge_thenShouldFailValidation() {
            // Given
            CreateChargeDTO chargeWithZeroAmount = CreateChargeDTO.builder()
                    .recipientCpf("98765432100")
                    .amount(BigDecimal.ZERO)
                    .description("Pagamento de serviços")
                    .build();

            // When
            Set<ConstraintViolation<CreateChargeDTO>> violations = validator.validate(chargeWithZeroAmount);

            // Then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("amount"));
        }

        @Test
        @DisplayName("GIVEN invalid recipient CPF WHEN validating charge THEN should fail validation")
        void givenInvalidRecipientCpf_whenValidatingCharge_thenShouldFailValidation() {
            // Given
            CreateChargeDTO chargeWithInvalidCpf = CreateChargeDTO.builder()
                    .recipientCpf("123")
                    .amount(new BigDecimal("100.00"))
                    .description("Pagamento de serviços")
                    .build();

            // When
            Set<ConstraintViolation<CreateChargeDTO>> violations = validator.validate(chargeWithInvalidCpf);

            // Then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("recipientCpf"));
        }

        @Test
        @DisplayName("GIVEN null recipient CPF WHEN validating charge THEN should fail validation")
        void givenNullRecipientCpf_whenValidatingCharge_thenShouldFailValidation() {
            // Given
            CreateChargeDTO chargeWithNullCpf = CreateChargeDTO.builder()
                    .recipientCpf(null)
                    .amount(new BigDecimal("100.00"))
                    .description("Pagamento de serviços")
                    .build();

            // When
            Set<ConstraintViolation<CreateChargeDTO>> violations = validator.validate(chargeWithNullCpf);

            // Then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("recipientCpf"));
        }
    }

    @Nested
    @DisplayName("Payment Validation Scenarios")
    class PaymentValidationScenarios {

        @Test
        @DisplayName("GIVEN valid payment data WHEN validating THEN should pass all validations")
        void givenValidPaymentData_whenValidating_thenShouldPassAllValidations() {
            // Given
            PayChargeDTO validPayment = PayChargeDTO.builder()
                    .chargeId(UUID.randomUUID())
                    .method("BALANCE")
                    .build();

            // When
            Set<ConstraintViolation<PayChargeDTO>> violations = validator.validate(validPayment);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("GIVEN valid credit card payment data WHEN validating THEN should pass all validations")
        void givenValidCreditCardPaymentData_whenValidating_thenShouldPassAllValidations() {
            // Given
            PayChargeDTO validCardPayment = PayChargeDTO.builder()
                    .chargeId(UUID.randomUUID())
                    .method("CREDIT_CARD")
                    .cardNumber("1234567890123456")
                    .expiryDate("12/25")
                    .cvv("123")
                    .build();

            // When
            Set<ConstraintViolation<PayChargeDTO>> violations = validator.validate(validCardPayment);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("GIVEN invalid card number WHEN validating credit card payment THEN should fail validation")
        void givenInvalidCardNumber_whenValidatingCreditCardPayment_thenShouldFailValidation() {
            // Given
            PayChargeDTO invalidCardPayment = PayChargeDTO.builder()
                    .chargeId(UUID.randomUUID())
                    .method("CREDIT_CARD")
                    .cardNumber("12345678901234567890")
                    .expiryDate("12/25")
                    .cvv("123")
                    .build();

            // When
            Set<ConstraintViolation<PayChargeDTO>> violations = validator.validate(invalidCardPayment);

            // Then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("cardNumber"));
        }

        @Test
        @DisplayName("GIVEN invalid expiry date WHEN validating credit card payment THEN should fail validation")
        void givenInvalidExpiryDate_whenValidatingCreditCardPayment_thenShouldFailValidation() {
            // Given
            PayChargeDTO invalidExpiryPayment = PayChargeDTO.builder()
                    .chargeId(UUID.randomUUID())
                    .method("CREDIT_CARD")
                    .cardNumber("1234567890123456")
                    .expiryDate("12/2025")
                    .cvv("123")
                    .build();

            // When
            Set<ConstraintViolation<PayChargeDTO>> violations = validator.validate(invalidExpiryPayment);

            // Then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("expiryDate"));
        }

        @Test
        @DisplayName("GIVEN invalid CVV WHEN validating credit card payment THEN should fail validation")
        void givenInvalidCvv_whenValidatingCreditCardPayment_thenShouldFailValidation() {
            // Given
            PayChargeDTO invalidCvvPayment = PayChargeDTO.builder()
                    .chargeId(UUID.randomUUID())
                    .method("CREDIT_CARD")
                    .cardNumber("1234567890123456")
                    .expiryDate("12/25")
                    .cvv("12345")
                    .build();

            // When
            Set<ConstraintViolation<PayChargeDTO>> violations = validator.validate(invalidCvvPayment);

            // Then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("cvv"));
        }
    }

    @Nested
    @DisplayName("Deposit Validation Scenarios")
    class DepositValidationScenarios {

        @Test
        @DisplayName("GIVEN valid deposit data WHEN validating THEN should pass all validations")
        void givenValidDepositData_whenValidating_thenShouldPassAllValidations() {
            // Given
            DepositDTO validDeposit = DepositDTO.builder()
                    .amount(new BigDecimal("100.00"))
                    .build();

            // When
            Set<ConstraintViolation<DepositDTO>> violations = validator.validate(validDeposit);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("GIVEN negative deposit amount WHEN validating THEN should fail validation")
        void givenNegativeDepositAmount_whenValidating_thenShouldFailValidation() {
            // Given
            DepositDTO negativeDeposit = DepositDTO.builder()
                    .amount(new BigDecimal("-100.00"))
                    .build();

            // When
            Set<ConstraintViolation<DepositDTO>> violations = validator.validate(negativeDeposit);

            // Then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("amount"));
        }

        @Test
        @DisplayName("GIVEN zero deposit amount WHEN validating THEN should fail validation")
        void givenZeroDepositAmount_whenValidating_thenShouldFailValidation() {
            // Given
            DepositDTO zeroDeposit = DepositDTO.builder()
                    .amount(BigDecimal.ZERO)
                    .build();

            // When
            Set<ConstraintViolation<DepositDTO>> violations = validator.validate(zeroDeposit);

            // Then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("amount"));
        }

        @Test
        @DisplayName("GIVEN null deposit amount WHEN validating THEN should fail validation")
        void givenNullDepositAmount_whenValidating_thenShouldFailValidation() {
            // Given
            DepositDTO nullDeposit = DepositDTO.builder()
                    .amount(null)
                    .build();

            // When
            Set<ConstraintViolation<DepositDTO>> violations = validator.validate(nullDeposit);

            // Then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("amount"));
        }
    }

    @Nested
    @DisplayName("Edge Cases Validation Scenarios")
    class EdgeCasesValidationScenarios {

        @Test
        @DisplayName("GIVEN very large amount WHEN validating charge THEN should pass validation")
        void givenVeryLargeAmount_whenValidatingCharge_thenShouldPassValidation() {
            // Given
            CreateChargeDTO largeAmountCharge = CreateChargeDTO.builder()
                    .recipientCpf("98765432100")
                    .amount(new BigDecimal("100000.00"))
                    .description("Pagamento de serviços")
                    .build();

            // When
            Set<ConstraintViolation<CreateChargeDTO>> violations = validator.validate(largeAmountCharge);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("GIVEN very long description WHEN validating charge THEN should pass validation")
        void givenVeryLongDescription_whenValidatingCharge_thenShouldPassValidation() {
            // Given
            String longDescription = "A".repeat(255);
            CreateChargeDTO longDescriptionCharge = CreateChargeDTO.builder()
                    .recipientCpf("98765432100")
                    .amount(new BigDecimal("100.00"))
                    .description(longDescription)
                    .build();

            // When
            Set<ConstraintViolation<CreateChargeDTO>> violations = validator.validate(longDescriptionCharge);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("GIVEN empty description WHEN validating charge THEN should pass validation")
        void givenEmptyDescription_whenValidatingCharge_thenShouldPassValidation() {
            // Given
            CreateChargeDTO emptyDescriptionCharge = CreateChargeDTO.builder()
                    .recipientCpf("98765432100")
                    .amount(new BigDecimal("100.00"))
                    .description("")
                    .build();

            // When
            Set<ConstraintViolation<CreateChargeDTO>> violations = validator.validate(emptyDescriptionCharge);

            // Then
            assertThat(violations).isEmpty();
        }
    }
}
