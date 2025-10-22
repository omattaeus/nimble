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
import java.util.UUID;

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
        UUID originatorId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID recipientId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID chargeId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        
        originator = User.builder()
                .id(originatorId)
                .name("João Silva")
                .cpf("12345678901")
                .email("joao@teste.com")
                .balance(BigDecimal.valueOf(1000.00))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        recipient = User.builder()
                .id(recipientId)
                .name("Maria Santos")
                .cpf("98765432100")
                .email("maria@teste.com")
                .balance(BigDecimal.valueOf(500.00))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        testCharge = Charge.builder()
                .id(chargeId)
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

            when(userRepository.findById(originator.getId())).thenReturn(Optional.of(originator));
            when(userRepository.findByCpf("98765432100")).thenReturn(Optional.of(recipient));
            when(chargeRepository.save(any(Charge.class))).thenReturn(testCharge);

            ChargeDTO result = chargeUseCase.createCharge(createChargeDTO, originator.getId());

            assertThat(result).isNotNull();
            assertThat(result.getOriginatorId()).isEqualTo(originator.getId());
            assertThat(result.getOriginatorName()).isEqualTo("João Silva");
            assertThat(result.getRecipientId()).isEqualTo(recipient.getId());
            assertThat(result.getRecipientName()).isEqualTo("Maria Santos");
            assertThat(result.getAmount()).isEqualTo(BigDecimal.valueOf(100.00));
            assertThat(result.getDescription()).isEqualTo("Pagamento de serviços");
            assertThat(result.getStatus()).isEqualTo("PENDING");

            verify(userRepository).findById(originator.getId());
            verify(userRepository).findByCpf("98765432100");
            verify(chargeRepository).save(any(Charge.class));
        }

        @Test
        @DisplayName("Given non-existent originator, when creating charge, then should throw exception")
        void givenNonExistentOriginator_whenCreateCharge_thenShouldThrowException() {

            UUID nonExistentUserId = UUID.fromString("99999999-9999-9999-9999-999999999999");
            when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chargeUseCase.createCharge(createChargeDTO, nonExistentUserId))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("Originator not found");

            verify(userRepository).findById(any(UUID.class));
            verify(userRepository, never()).findByCpf(anyString());
            verify(chargeRepository, never()).save(any(Charge.class));
        }

        @Test
        @DisplayName("Given non-existent recipient, when creating charge, then should throw exception")
        void givenNonExistentRecipient_whenCreateCharge_thenShouldThrowException() {

            when(userRepository.findById(originator.getId())).thenReturn(Optional.of(originator));
            when(userRepository.findByCpf("99999999999")).thenReturn(Optional.empty());

            CreateChargeDTO invalidDTO = CreateChargeDTO.builder()
                    .recipientCpf("99999999999")
                    .amount(BigDecimal.valueOf(100.00))
                    .description("Pagamento de serviços")
                    .build();

            assertThatThrownBy(() -> chargeUseCase.createCharge(invalidDTO, originator.getId()))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("Recipient not found with CPF: 99999999999");

            verify(userRepository).findById(originator.getId());
            verify(userRepository).findByCpf("99999999999");
            verify(chargeRepository, never()).save(any(Charge.class));
        }

        @Test
        @DisplayName("Given originator trying to create charge for himself, when creating charge, then should throw exception")
        void givenOriginatorCreatingChargeForHimself_whenCreateCharge_thenShouldThrowException() {

            when(userRepository.findById(originator.getId())).thenReturn(Optional.of(originator));
            when(userRepository.findByCpf("12345678901")).thenReturn(Optional.of(originator));

            CreateChargeDTO selfChargeDTO = CreateChargeDTO.builder()
                    .recipientCpf("12345678901")
                    .amount(BigDecimal.valueOf(100.00))
                    .description("Pagamento de serviços")
                    .build();

            assertThatThrownBy(() -> chargeUseCase.createCharge(selfChargeDTO, originator.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Cannot create charge to yourself");

            verify(userRepository).findById(originator.getId());
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

            when(userRepository.findById(originator.getId())).thenReturn(Optional.of(originator));
            when(chargeRepository.findByOriginator(originator)).thenReturn(Arrays.asList(testCharge));

            List<ChargeDTO> result = chargeUseCase.getChargesByOriginator(originator.getId(), null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOriginatorId()).isEqualTo(originator.getId());
            assertThat(result.get(0).getRecipientId()).isEqualTo(recipient.getId());
            assertThat(result.get(0).getStatus()).isEqualTo("PENDING");

            verify(userRepository).findById(originator.getId());
            verify(chargeRepository).findByOriginator(originator);
        }

        @Test
        @DisplayName("Given valid originator with status filter, when getting sent charges, then should return filtered list")
        void givenValidOriginatorWithStatusFilter_whenGetChargesByOriginator_thenShouldReturnFilteredList() {

            when(userRepository.findById(originator.getId())).thenReturn(Optional.of(originator));
            when(chargeRepository.findByOriginatorAndStatus(originator, Charge.ChargeStatus.PENDING))
                    .thenReturn(Arrays.asList(testCharge));

            List<ChargeDTO> result = chargeUseCase.getChargesByOriginator(originator.getId(), "PENDING");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo("PENDING");

            verify(userRepository).findById(originator.getId());
            verify(chargeRepository).findByOriginatorAndStatus(originator, Charge.ChargeStatus.PENDING);
        }

        @Test
        @DisplayName("Given non-existent originator, when getting sent charges, then should throw exception")
        void givenNonExistentOriginator_whenGetChargesByOriginator_thenShouldThrowException() {

            UUID nonExistentUserId = UUID.fromString("99999999-9999-9999-9999-999999999999");
            when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chargeUseCase.getChargesByOriginator(nonExistentUserId, null))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found with ID: 99999999-9999-9999-9999-999999999999");

            verify(userRepository).findById(any(UUID.class));
            verify(chargeRepository, never()).findByOriginator(any(User.class));
        }
    }

    @Nested
    @DisplayName("Scenario: Get Charges by Recipient")
    class GetChargesByRecipientTests {

        @Test
        @DisplayName("Given valid recipient, when getting received charges, then should return ChargeDTO list")
        void givenValidRecipient_whenGetChargesByRecipient_thenShouldReturnChargeDTOList() {

            when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));
            when(chargeRepository.findByRecipient(recipient)).thenReturn(Arrays.asList(testCharge));

            List<ChargeDTO> result = chargeUseCase.getChargesByRecipient(recipient.getId(), null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOriginatorId()).isEqualTo(originator.getId());
            assertThat(result.get(0).getRecipientId()).isEqualTo(recipient.getId());
            assertThat(result.get(0).getStatus()).isEqualTo("PENDING");

            verify(userRepository).findById(recipient.getId());
            verify(chargeRepository).findByRecipient(recipient);
        }

        @Test
        @DisplayName("Given valid recipient with status filter, when getting received charges, then should return filtered list")
        void givenValidRecipientWithStatusFilter_whenGetChargesByRecipient_thenShouldReturnFilteredList() {

            when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));
            when(chargeRepository.findByRecipientAndStatus(recipient, Charge.ChargeStatus.PENDING))
                    .thenReturn(Arrays.asList(testCharge));

            List<ChargeDTO> result = chargeUseCase.getChargesByRecipient(recipient.getId(), "PENDING");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo("PENDING");

            verify(userRepository).findById(recipient.getId());
            verify(chargeRepository).findByRecipientAndStatus(recipient, Charge.ChargeStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("Scenario: Get Charge by ID")
    class GetChargeByIdTests {

        @Test
        @DisplayName("Given valid ID, when getting charge, then should return ChargeDTO")
        void givenValidId_whenGetChargeById_thenShouldReturnChargeDTO() {

            when(chargeRepository.findById(testCharge.getId())).thenReturn(Optional.of(testCharge));

            ChargeDTO result = chargeUseCase.getChargeById(testCharge.getId(), originator.getId());

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testCharge.getId());
            assertThat(result.getOriginatorId()).isEqualTo(originator.getId());
            assertThat(result.getRecipientId()).isEqualTo(recipient.getId());
            assertThat(result.getAmount()).isEqualTo(BigDecimal.valueOf(100.00));

            verify(chargeRepository).findById(testCharge.getId());
        }

        @Test
        @DisplayName("Given non-existent ID, when getting charge, then should throw exception")
        void givenNonExistentId_whenGetChargeById_thenShouldThrowException() {

            UUID nonExistentChargeId = UUID.fromString("99999999-9999-9999-9999-999999999999");
            when(chargeRepository.findById(nonExistentChargeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chargeUseCase.getChargeById(nonExistentChargeId, originator.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Charge not found");

            verify(chargeRepository).findById(any(UUID.class));
        }
    }
}