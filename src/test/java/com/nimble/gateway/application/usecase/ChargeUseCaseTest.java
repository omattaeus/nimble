package com.nimble.gateway.application.usecase;

import com.nimble.gateway.application.dto.ChargeDTO;
import com.nimble.gateway.application.dto.CreateChargeDTO;
import com.nimble.gateway.domain.entity.Charge;
import com.nimble.gateway.domain.entity.User;
import com.nimble.gateway.domain.repository.ChargeRepository;
import com.nimble.gateway.domain.repository.UserRepository;
import com.nimble.gateway.domain.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChargeUseCase - Unit Tests")
class ChargeUseCaseTest {

    @Mock
    private ChargeRepository chargeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChargeUseCase chargeUseCase;

    private User originator;
    private User recipient;
    private Charge testCharge;
    private CreateChargeDTO createChargeDTO;

    @BeforeEach
    void setUp() {
        originator = User.builder()
                .id(1L)
                .name("João Silva")
                .cpf("12345678901")
                .email("joao@teste.com")
                .balance(BigDecimal.valueOf(1000.00))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        recipient = User.builder()
                .id(2L)
                .name("Maria Santos")
                .cpf("98765432100")
                .email("maria@teste.com")
                .balance(BigDecimal.valueOf(500.00))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        testCharge = Charge.builder()
                .id(1L)
                .originator(originator)
                .recipient(recipient)
                .amount(BigDecimal.valueOf(100.00))
                .description("Pagamento de serviços")
                .status(Charge.ChargeStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        createChargeDTO = CreateChargeDTO.builder()
                .recipientCpf("98765432100")
                .amount(BigDecimal.valueOf(100.00))
                .description("Pagamento de serviços")
                .build();
    }

    @Nested
    @DisplayName("Scenario: Charge Creation")
    class CreateChargeTests {

        @Test
        @DisplayName("Given valid data, when creating charge, then should return ChargeDTO")
        void givenValidData_whenCreateCharge_thenShouldReturnChargeDTO() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(originator));
            when(userRepository.findByCpf("98765432100")).thenReturn(Optional.of(recipient));
            when(chargeRepository.save(any(Charge.class))).thenReturn(testCharge);

            // When
            ChargeDTO result = chargeUseCase.createCharge(createChargeDTO, 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOriginatorId()).isEqualTo(1L);
            assertThat(result.getOriginatorName()).isEqualTo("João Silva");
            assertThat(result.getRecipientId()).isEqualTo(2L);
            assertThat(result.getRecipientName()).isEqualTo("Maria Santos");
            assertThat(result.getAmount()).isEqualTo(BigDecimal.valueOf(100.00));
            assertThat(result.getDescription()).isEqualTo("Pagamento de serviços");
            assertThat(result.getStatus()).isEqualTo("PENDING");

            verify(userRepository).findById(1L);
            verify(userRepository).findByCpf("98765432100");
            verify(chargeRepository).save(any(Charge.class));
        }

        @Test
        @DisplayName("Given non-existent originator, when creating charge, then should throw exception")
        void givenNonExistentOriginator_whenCreateCharge_thenShouldThrowException() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> chargeUseCase.createCharge(createChargeDTO, 999L))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("Originator not found");

            verify(userRepository).findById(999L);
            verify(userRepository, never()).findByCpf(anyString());
            verify(chargeRepository, never()).save(any(Charge.class));
        }

        @Test
        @DisplayName("Given non-existent recipient, when creating charge, then should throw exception")
        void givenNonExistentRecipient_whenCreateCharge_thenShouldThrowException() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(originator));
            when(userRepository.findByCpf("99999999999")).thenReturn(Optional.empty());

            CreateChargeDTO invalidDTO = CreateChargeDTO.builder()
                    .recipientCpf("99999999999")
                    .amount(BigDecimal.valueOf(100.00))
                    .description("Pagamento de serviços")
                    .build();

            // When & Then
            assertThatThrownBy(() -> chargeUseCase.createCharge(invalidDTO, 1L))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("Recipient not found with CPF: 99999999999");

            verify(userRepository).findById(1L);
            verify(userRepository).findByCpf("99999999999");
            verify(chargeRepository, never()).save(any(Charge.class));
        }

        @Test
        @DisplayName("Given originator trying to create charge for himself, when creating charge, then should throw exception")
        void givenOriginatorCreatingChargeForHimself_whenCreateCharge_thenShouldThrowException() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(originator));
            when(userRepository.findByCpf("12345678901")).thenReturn(Optional.of(originator));

            CreateChargeDTO selfChargeDTO = CreateChargeDTO.builder()
                    .recipientCpf("12345678901")
                    .amount(BigDecimal.valueOf(100.00))
                    .description("Pagamento de serviços")
                    .build();

            // When & Then
            assertThatThrownBy(() -> chargeUseCase.createCharge(selfChargeDTO, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Cannot create charge to yourself");

            verify(userRepository).findById(1L);
            verify(userRepository).findByCpf("12345678901");
            verify(chargeRepository, never()).save(any(Charge.class));
        }
    }

    @Nested
    @DisplayName("Scenario: Get Charges by Originator")
    class GetChargesByOriginatorTests {

        @Test
        @DisplayName("Given valid originator, when getting sent charges, then should return ChargeDTO list")
        void givenValidOriginator_whenGetChargesByOriginator_thenShouldReturnChargeDTOList() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(originator));
            when(chargeRepository.findByOriginator(originator)).thenReturn(Arrays.asList(testCharge));

            // When
            List<ChargeDTO> result = chargeUseCase.getChargesByOriginator(1L, null);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOriginatorId()).isEqualTo(1L);
            assertThat(result.get(0).getRecipientId()).isEqualTo(2L);
            assertThat(result.get(0).getStatus()).isEqualTo("PENDING");

            verify(userRepository).findById(1L);
            verify(chargeRepository).findByOriginator(originator);
        }

        @Test
        @DisplayName("Given valid originator with status filter, when getting sent charges, then should return filtered list")
        void givenValidOriginatorWithStatusFilter_whenGetChargesByOriginator_thenShouldReturnFilteredList() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(originator));
            when(chargeRepository.findByOriginatorAndStatus(originator, Charge.ChargeStatus.PENDING))
                    .thenReturn(Arrays.asList(testCharge));

            // When
            List<ChargeDTO> result = chargeUseCase.getChargesByOriginator(1L, "PENDING");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo("PENDING");

            verify(userRepository).findById(1L);
            verify(chargeRepository).findByOriginatorAndStatus(originator, Charge.ChargeStatus.PENDING);
        }

        @Test
        @DisplayName("Given non-existent originator, when getting sent charges, then should throw exception")
        void givenNonExistentOriginator_whenGetChargesByOriginator_thenShouldThrowException() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> chargeUseCase.getChargesByOriginator(999L, null))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found with ID: 999");

            verify(userRepository).findById(999L);
            verify(chargeRepository, never()).findByOriginator(any(User.class));
        }
    }

    @Nested
    @DisplayName("Scenario: Get Charges by Recipient")
    class GetChargesByRecipientTests {

        @Test
        @DisplayName("Given valid recipient, when getting received charges, then should return ChargeDTO list")
        void givenValidRecipient_whenGetChargesByRecipient_thenShouldReturnChargeDTOList() {
            // Given
            when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));
            when(chargeRepository.findByRecipient(recipient)).thenReturn(Arrays.asList(testCharge));

            // When
            List<ChargeDTO> result = chargeUseCase.getChargesByRecipient(2L, null);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOriginatorId()).isEqualTo(1L);
            assertThat(result.get(0).getRecipientId()).isEqualTo(2L);
            assertThat(result.get(0).getStatus()).isEqualTo("PENDING");

            verify(userRepository).findById(2L);
            verify(chargeRepository).findByRecipient(recipient);
        }

        @Test
        @DisplayName("Given valid recipient with status filter, when getting received charges, then should return filtered list")
        void givenValidRecipientWithStatusFilter_whenGetChargesByRecipient_thenShouldReturnFilteredList() {
            // Given
            when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));
            when(chargeRepository.findByRecipientAndStatus(recipient, Charge.ChargeStatus.PENDING))
                    .thenReturn(Arrays.asList(testCharge));

            // When
            List<ChargeDTO> result = chargeUseCase.getChargesByRecipient(2L, "PENDING");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo("PENDING");

            verify(userRepository).findById(2L);
            verify(chargeRepository).findByRecipientAndStatus(recipient, Charge.ChargeStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("Scenario: Get Charge by ID")
    class GetChargeByIdTests {

        @Test
        @DisplayName("Given valid ID, when getting charge, then should return ChargeDTO")
        void givenValidId_whenGetChargeById_thenShouldReturnChargeDTO() {
            // Given
            when(chargeRepository.findById(1L)).thenReturn(Optional.of(testCharge));

            // When
            ChargeDTO result = chargeUseCase.getChargeById(1L, 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getOriginatorId()).isEqualTo(1L);
            assertThat(result.getRecipientId()).isEqualTo(2L);
            assertThat(result.getAmount()).isEqualTo(BigDecimal.valueOf(100.00));

            verify(chargeRepository).findById(1L);
        }

        @Test
        @DisplayName("Given non-existent ID, when getting charge, then should throw exception")
        void givenNonExistentId_whenGetChargeById_thenShouldThrowException() {
            // Given
            when(chargeRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> chargeUseCase.getChargeById(999L, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Charge not found");

            verify(chargeRepository).findById(999L);
        }
    }
}
