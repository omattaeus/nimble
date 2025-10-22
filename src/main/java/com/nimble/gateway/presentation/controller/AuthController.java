package com.nimble.gateway.presentation.controller;

import com.nimble.gateway.application.dto.AuthResponseDTO;
import com.nimble.gateway.application.dto.CreateUserDTO;
import com.nimble.gateway.application.dto.LoginDTO;
import com.nimble.gateway.application.dto.UserDTO;
import com.nimble.gateway.application.usecase.UserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and registration")
public class AuthController {
    
    private final UserUseCase userUseCase;
    
    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Create a new user account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully",
                    content = @Content(schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "User already exists")
    })
    public ResponseEntity<UserDTO> register(@Valid @RequestBody CreateUserDTO createUserDTO) {
        log.info("Registering new user with email: {}", createUserDTO.getEmail());
        
        UserDTO userDTO = userUseCase.createUser(createUserDTO);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(userDTO);
    }
    
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
        log.info("Login attempt for username: {}", loginDTO.getUsername());
        
        AuthResponseDTO authResponse = userUseCase.login(loginDTO);
        
        return ResponseEntity.ok(authResponse);
    }
    
    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get details of the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        log.info("Getting current user from authentication");
        
        String username = authentication.getName();
        UserDTO userDTO = userUseCase.getUserByUsername(username);
        
        return ResponseEntity.ok(userDTO);
    }
}