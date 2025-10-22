package com.nimble.gateway.application.usecase;

import com.nimble.gateway.application.dto.ChargeDTO;
import com.nimble.gateway.application.dto.CreateChargeDTO;
import com.nimble.gateway.domain.entity.Charge;
import com.nimble.gateway.domain.entity.User;
import com.nimble.gateway.domain.exception.UserNotFoundException;
import com.nimble.gateway.domain.repository.ChargeRepository;
import com.nimble.gateway.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChargeUseCase {
    
    private final ChargeRepository chargeRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public ChargeDTO createCharge(CreateChargeDTO createChargeDTO, UUID originatorId) {
        log.info("Creating charge from user {} to CPF {}", originatorId, createChargeDTO.getRecipientCpf());
        
        User originator = userRepository.findById(originatorId)
                .orElseThrow(() -> new UserNotFoundException("Originator not found"));
        
        User recipient = userRepository.findByCpf(createChargeDTO.getRecipientCpf())
                .orElseThrow(() -> new UserNotFoundException("Recipient not found with CPF: " + createChargeDTO.getRecipientCpf()));
        
        if (originator.getId().equals(recipient.getId())) throw new IllegalArgumentException("Cannot create charge to yourself");
        
        Charge charge = Charge.builder()
                .originator(originator)
                .recipient(recipient)
                .amount(createChargeDTO.getAmount())
                .description(createChargeDTO.getDescription())
                .status(Charge.ChargeStatus.PENDING)
                .build();
        
        Charge savedCharge = chargeRepository.save(charge);
        log.info("Charge created successfully with ID: {}", savedCharge.getId());
        
        return ChargeDTO.builder()
                .id(savedCharge.getId())
                .originatorId(savedCharge.getOriginator().getId())
                .originatorName(savedCharge.getOriginator().getName())
                .recipientId(savedCharge.getRecipient().getId())
                .recipientName(savedCharge.getRecipient().getName())
                .amount(savedCharge.getAmount())
                .description(savedCharge.getDescription())
                .status(savedCharge.getStatus().name())
                .createdAt(savedCharge.getCreatedAt())
                .paidAt(savedCharge.getPaidAt())
                .cancelledAt(savedCharge.getCancelledAt())
                .build();
    }
    
    public List<ChargeDTO> getChargesByOriginator(UUID originatorId, String status) {
        log.info("Getting charges by originator {} with status {}", originatorId, status);
        
        User originator = userRepository.findById(originatorId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + originatorId));
        
        List<Charge> charges;
        if (status != null && !status.isEmpty()) {
            Charge.ChargeStatus chargeStatus = Charge.ChargeStatus.valueOf(status.toUpperCase());
            charges = chargeRepository.findByOriginatorAndStatus(originator, chargeStatus);
        } else {
            charges = chargeRepository.findByOriginator(originator);
        }
        
        return charges.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<ChargeDTO> getChargesByRecipient(UUID recipientId, String status) {
        log.info("Getting charges by recipient {} with status {}", recipientId, status);
        
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new UserNotFoundException("Recipient not found"));
        
        List<Charge> charges;
        if (status != null && !status.isEmpty()) {
            Charge.ChargeStatus chargeStatus = Charge.ChargeStatus.valueOf(status.toUpperCase());
            charges = chargeRepository.findByRecipientAndStatus(recipient, chargeStatus);
        } else {
            charges = chargeRepository.findByRecipient(recipient);
        }
        
        return charges.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public ChargeDTO getChargeById(UUID chargeId, UUID userId) {
        log.info("Getting charge {} for user {}", chargeId, userId);
        
        Charge charge = chargeRepository.findById(chargeId)
                .orElseThrow(() -> new IllegalArgumentException("Charge not found"));
        
        if (!charge.getOriginator().getId().equals(userId) && !charge.getRecipient().getId().equals(userId)) {
            throw new IllegalArgumentException("Access denied to this charge");
        }
        
        return mapToDTO(charge);
    }
    
    private ChargeDTO mapToDTO(Charge charge) {
        return ChargeDTO.builder()
                .id(charge.getId())
                .originatorId(charge.getOriginator().getId())
                .originatorName(charge.getOriginator().getName())
                .recipientId(charge.getRecipient().getId())
                .recipientName(charge.getRecipient().getName())
                .amount(charge.getAmount())
                .description(charge.getDescription())
                .status(charge.getStatus().name())
                .createdAt(charge.getCreatedAt())
                .paidAt(charge.getPaidAt())
                .cancelledAt(charge.getCancelledAt())
                .build();
    }
}