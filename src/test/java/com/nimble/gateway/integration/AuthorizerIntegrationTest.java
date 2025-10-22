package com.nimble.gateway.integration;

import com.nimble.gateway.infrastructure.external.AuthorizerClient;
import com.nimble.gateway.infrastructure.external.AuthorizerResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Authorizer External Service Integration Tests")
class AuthorizerIntegrationTest {

    @Autowired
    private AuthorizerClient authorizerClient;

    @Test
    @DisplayName("GIVEN external authorizer WHEN calling authorize THEN should return valid response")
    void givenExternalAuthorizer_whenCallingAuthorize_thenShouldReturnValidResponse() {
        // When
        Mono<AuthorizerResponse> responseMono = authorizerClient.authorize("payment");

        // Then
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getStatus()).isIn("success", "fail");
                    assertThat(response.getData()).isNotNull();
                    assertThat(response.getData().getAuthorized()).isNotNull();
                    
                    // Log the response for debugging
                    System.out.println("Authorizer Response: " + response);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN external authorizer WHEN calling multiple times THEN should handle different responses")
    void givenExternalAuthorizer_whenCallingMultipleTimes_thenShouldHandleDifferentResponses() {
        // When & Then - Make multiple calls to see different responses
        for (int i = 0; i < 3; i++) {
            final int callNumber = i + 1;
            Mono<AuthorizerResponse> responseMono = authorizerClient.authorize("payment");
            
            StepVerifier.create(responseMono)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.getStatus()).isIn("success", "fail");
                        assertThat(response.getData()).isNotNull();
                        assertThat(response.getData().getAuthorized()).isNotNull();
                        
                        System.out.println("Call " + callNumber + " - Status: " + response.getStatus() + 
                                         ", Authorized: " + response.getData().getAuthorized() +
                                         ", isApproved(): " + response.isApproved());
                    })
                    .verifyComplete();
        }
    }

    @Test
    @DisplayName("GIVEN external authorizer WHEN calling authorizePayment THEN should return valid response")
    void givenExternalAuthorizer_whenCallingAuthorizePayment_thenShouldReturnValidResponse() {
        // When
        Mono<AuthorizerResponse> responseMono = authorizerClient.authorizePayment();

        // Then
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getStatus()).isIn("success", "fail");
                    assertThat(response.getData()).isNotNull();
                    assertThat(response.getData().getAuthorized()).isNotNull();
                    
                    System.out.println("Payment Authorization Response: " + response);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN external authorizer WHEN calling authorizeDeposit THEN should return valid response")
    void givenExternalAuthorizer_whenCallingAuthorizeDeposit_thenShouldReturnValidResponse() {
        // When
        Mono<AuthorizerResponse> responseMono = authorizerClient.authorizeDeposit();

        // Then
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getStatus()).isIn("success", "fail");
                    assertThat(response.getData()).isNotNull();
                    assertThat(response.getData().getAuthorized()).isNotNull();
                    
                    System.out.println("Deposit Authorization Response: " + response);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN external authorizer WHEN calling authorizeCancellation THEN should return valid response")
    void givenExternalAuthorizer_whenCallingAuthorizeCancellation_thenShouldReturnValidResponse() {
        // When
        Mono<AuthorizerResponse> responseMono = authorizerClient.authorizeCancellation();

        // Then
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getStatus()).isIn("success", "fail");
                    assertThat(response.getData()).isNotNull();
                    assertThat(response.getData().getAuthorized()).isNotNull();
                    
                    System.out.println("Cancellation Authorization Response: " + response);
                })
                .verifyComplete();
    }
}
