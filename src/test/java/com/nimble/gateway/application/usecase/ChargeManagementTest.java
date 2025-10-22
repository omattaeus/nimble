package com.nimble.gateway.application.usecase;

import com.nimble.gateway.application.dto.ChargeDTO;
import com.nimble.gateway.application.dto.CreateChargeDTO;
import com.nimble.gateway.domain.entity.Charge;
import com.nimble.gateway.domain.entity.User;
import com.nimble.gateway.domain.exception.UserNotFoundException;
import com.nimble.gateway.domain.repository.ChargeRepository;
import com.nimble.gateway.domain.repository.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Charge Management - BDD Tests")
class ChargeManagementTest {

    @Mock
    private ChargeRepository chargeRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChargeUseCase chargeUseCase;

    private User originator;
    private User recipient;
    private Charge pendingCharge;
    private Charge paidCharge;
    private CreateChargeDTO createChargeDTO;

    @BeforeEach
    void setUp() {
        originator = User.builder()
                .id(UUID.randomUUID())
                .name("Originator User")
                .cpf("11122233344")
                .email("originator@test.com")
                .password("password")
                .balance(new BigDecimal("1000.00"))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        recipient = User.builder()
                .id(UUID.randomUUID())
                .name("Recipient User")
                .cpf("55566677788")
                .email("recipient@test.com")
                .password("password")
                .balance(new BigDecimal("500.00"))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        pendingCharge = Charge.builder()
                .id(UUID.randomUUID())
                .originator(originator)
                .recipient(recipient)
                .amount(new BigDecimal("150.00"))
                .description("New charge")
                .status(Charge.ChargeStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        paidCharge = Charge.builder()
                .id(UUID.randomUUID())
                .originator(originator)
                .recipient(recipient)
                .amount(new BigDecimal("200.00"))
                .description("Paid charge")
                .status(Charge.ChargeStatus.PAID)
                .createdAt(LocalDateTime.now())
                .build();

        createChargeDTO = CreateChargeDTO.builder()
                .recipientCpf("55566677788")
                .amount(new BigDecimal("150.00"))
                .description("New charge")
                .build();
    }

    @Nested
    @DisplayName("Charge Creation Scenarios")
    class ChargeCreationScenarios {

        @Test
        @DisplayName("GIVEN valid charge data WHEN creating charge THEN should create charge successfully")
        void givenValidChargeData_whenCreatingCharge_thenShouldCreateChargeSuccessfully() {

            when(userRepository.findById(originator.getId())).thenReturn(Optional.of(originator));
            when(userRepository.findByCpf(createChargeDTO.getRecipientCpf())).thenReturn(Optional.of(recipient));
            when(chargeRepository.save(any(Charge.class))).thenReturn(pendingCharge);


            ChargeDTO result = chargeUseCase.createCharge(createChargeDTO, originator.getId());


            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualTo(createChargeDTO.getAmount());
            assertThat(result.getDescription()).isEqualTo(createChargeDTO.getDescription());
            assertThat(result.getStatus()).isEqualTo("PENDING");

            verify(userRepository).findById(originator.getId());
            verify(userRepository).findByCpf(createChargeDTO.getRecipientCpf());
            verify(chargeRepository).save(any(Charge.class));
        }

        @Test
        @DisplayName("GIVEN non-existing recipient CPF WHEN creating charge THEN should throw exception")
        void givenNonExistingRecipientCpf_whenCreatingCharge_thenShouldThrowException() {

            when(userRepository.findById(originator.getId())).thenReturn(Optional.of(originator));
            when(userRepository.findByCpf(createChargeDTO.getRecipientCpf())).thenReturn(Optional.empty());


            assertThatThrownBy(() -> chargeUseCase.createCharge(createChargeDTO, originator.getId()))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("Recipient not found with CPF: " + createChargeDTO.getRecipientCpf());

            verify(userRepository).findById(originator.getId());
            verify(userRepository).findByCpf(createChargeDTO.getRecipientCpf());
            verify(chargeRepository, never()).save(any(Charge.class));
        }

        @Test
        @DisplayName("GIVEN same user as originator and recipient WHEN creating charge THEN should throw exception")
        void givenSameUserAsOriginatorAndRecipient_whenCreatingCharge_thenShouldThrowException() {

            CreateChargeDTO selfChargeDTO = CreateChargeDTO.builder()
                    .recipientCpf("11122233344")
                    .amount(new BigDecimal("100.00"))
                    .description("Self charge")
                    .build();

            when(userRepository.findById(originator.getId())).thenReturn(Optional.of(originator));
            when(userRepository.findByCpf(selfChargeDTO.getRecipientCpf())).thenReturn(Optional.of(originator));


            assertThatThrownBy(() -> chargeUseCase.createCharge(selfChargeDTO, originator.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Cannot create charge to yourself");

            verify(userRepository).findById(originator.getId());
            verify(userRepository).findByCpf(selfChargeDTO.getRecipientCpf());
            verify(chargeRepository, never()).save(any(Charge.class));
        }
    }

    @Nested
    @DisplayName("Charge Query Scenarios")
    class ChargeQueryScenarios {

        @Test
        @DisplayName("GIVEN existing charge ID WHEN getting charge by ID THEN should return charge DTO")
        void givenExistingChargeId_whenGettingChargeById_thenShouldReturnChargeDTO() {

            UUID chargeId = pendingCharge.getId();
            when(chargeRepository.findById(chargeId)).thenReturn(Optional.of(pendingCharge));

            ChargeDTO result = chargeUseCase.getChargeById(chargeId, originator.getId());

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(chargeId);
            assertThat(result.getAmount()).isEqualTo(pendingCharge.getAmount());

            verify(chargeRepository).findById(chargeId);
        }

        @Test
        @DisplayName("GIVEN non-existing charge ID WHEN getting charge by ID THEN should throw exception")
        void givenNonExistingChargeId_whenGettingChargeById_thenShouldThrowException() {

            UUID chargeId = UUID.randomUUID();
            when(chargeRepository.findById(chargeId)).thenReturn(Optional.empty());


            assertThatThrownBy(() -> chargeUseCase.getChargeById(chargeId, originator.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Charge not found");

            verify(chargeRepository).findById(chargeId);
        }

        @Test
        @DisplayName("GIVEN unauthorized user WHEN getting charge by ID THEN should throw exception")
        void givenUnauthorizedUser_whenGettingChargeById_thenShouldThrowException() {

            UUID chargeId = originator.getId();
            UUID unauthorizedUserId = UUID.randomUUID();
            when(chargeRepository.findById(chargeId)).thenReturn(Optional.of(pendingCharge));


            assertThatThrownBy(() -> chargeUseCase.getChargeById(chargeId, unauthorizedUserId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Access denied to this charge");

            verify(chargeRepository).findById(chargeId);
        }
    }

    @Nested
    @DisplayName("Charge Status Scenarios")
    class ChargeStatusScenarios {

        @Test
        @DisplayName("GIVEN pending charges WHEN querying sent charges by status THEN should return pending charges")
        void givenPendingCharges_whenQueryingSentChargesByStatus_thenShouldReturnPendingCharges() {

            List<Charge> charges = Arrays.asList(pendingCharge);
            when(userRepository.findById(originator.getId())).thenReturn(Optional.of(originator));
            when(chargeRepository.findByOriginatorAndStatus(originator, Charge.ChargeStatus.PENDING)).thenReturn(charges);


            List<ChargeDTO> result = chargeUseCase.getChargesByOriginator(originator.getId(), "PENDING");


            assertThat(result).isNotNull().hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo("PENDING");

            verify(userRepository).findById(originator.getId());
            verify(chargeRepository).findByOriginatorAndStatus(originator, Charge.ChargeStatus.PENDING);
        }

        @Test
        @DisplayName("GIVEN paid charges WHEN querying received charges by status THEN should return paid charges")
        void givenPaidCharges_whenQueryingReceivedChargesByStatus_thenShouldReturnPaidCharges() {

            List<Charge> charges = Arrays.asList(paidCharge);
            when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));
            when(chargeRepository.findByRecipientAndStatus(recipient, Charge.ChargeStatus.PAID)).thenReturn(charges);


            List<ChargeDTO> result = chargeUseCase.getChargesByRecipient(recipient.getId(), "PAID");


            assertThat(result).isNotNull().hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo("PAID");

            verify(userRepository).findById(recipient.getId());
            verify(chargeRepository).findByRecipientAndStatus(recipient, Charge.ChargeStatus.PAID);
        }

        @Test
        @DisplayName("GIVEN no status filter WHEN querying charges THEN should return all charges")
        void givenNoStatusFilter_whenQueryingCharges_thenShouldReturnAllCharges() {

            List<Charge> charges = Arrays.asList(pendingCharge, paidCharge);
            when(userRepository.findById(originator.getId())).thenReturn(Optional.of(originator));
            when(chargeRepository.findByOriginator(originator)).thenReturn(charges);


            List<ChargeDTO> result = chargeUseCase.getChargesByOriginator(originator.getId(), null);


            assertThat(result).isNotNull().hasSize(2);

            verify(userRepository).findById(originator.getId());
            verify(chargeRepository).findByOriginator(originator);
        }
    }
}