package com.nimble.gateway.presentation.controller;

import com.nimble.gateway.application.dto.ChargeDTO;
import com.nimble.gateway.application.dto.CreateChargeDTO;
import com.nimble.gateway.application.usecase.ChargeUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/charges")
@RequiredArgsConstructor
@Tag(name = "Charges", description = "Charge management operations")
@SecurityRequirement(name = "bearer-key")
public class ChargeController {
    
    private final ChargeUseCase chargeUseCase;
    
    @PostMapping
    @Operation(summary = "Create charge", description = "Create a new charge for a recipient")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Charge created successfully",
                    content = @Content(schema = @Schema(implementation = ChargeDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Recipient not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ChargeDTO> createCharge(
            @Valid @RequestBody CreateChargeDTO createChargeDTO,
            @Parameter(description = "ID of the user creating the charge") @RequestParam UUID originatorId) {
        
        log.info("Creating charge from user {} to CPF {}", originatorId, createChargeDTO.getRecipientCpf());
        
        ChargeDTO chargeDTO = chargeUseCase.createCharge(createChargeDTO, originatorId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(chargeDTO);
    }
    
    @GetMapping("/sent")
    public ResponseEntity<List<ChargeDTO>> getSentCharges(
            @RequestParam UUID userId,
            @RequestParam(required = false) String status) {
        
        log.info("Getting sent charges for user {} with status {}", userId, status);
        
        List<ChargeDTO> charges = chargeUseCase.getChargesByOriginator(userId, status);
        
        return ResponseEntity.ok(charges);
    }
    
    @GetMapping("/received")
    public ResponseEntity<List<ChargeDTO>> getReceivedCharges(
            @RequestParam UUID userId,
            @RequestParam(required = false) String status) {
        
        log.info("Getting received charges for user {} with status {}", userId, status);
        
        List<ChargeDTO> charges = chargeUseCase.getChargesByRecipient(userId, status);
        
        return ResponseEntity.ok(charges);
    }
    
    @GetMapping("/{chargeId}")
    public ResponseEntity<ChargeDTO> getChargeById(
            @PathVariable UUID chargeId,
            @RequestParam UUID userId) {
        
        log.info("Getting charge {} for user {}", chargeId, userId);
        
        ChargeDTO chargeDTO = chargeUseCase.getChargeById(chargeId, userId);
        
        return ResponseEntity.ok(chargeDTO);
    }
}