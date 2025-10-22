package com.nimble.gateway.infrastructure.external;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class AuthorizerClient {
    
    private final WebClient webClient;
    
    public AuthorizerClient(
            WebClient.Builder webClientBuilder,
            @Value("${external.authorizer.url:https://zsy6tx7aql.execute-api.saeast-1.amazonaws.com}") String authorizerUrl) {
        this.webClient = webClientBuilder
                .baseUrl(authorizerUrl)
                .build();
    }
    
    public Mono<AuthorizerResponse> authorizePayment() {
        return webClient
                .get()
                .uri("/")
                .retrieve()
                .bodyToMono(AuthorizerResponse.class)
                .timeout(Duration.ofSeconds(10))
                .onErrorReturn(createErrorResponse("Authorization service unavailable"));
    }
    
    public Mono<AuthorizerResponse> authorizeDeposit() {
        return webClient
                .get()
                .uri("/")
                .retrieve()
                .bodyToMono(AuthorizerResponse.class)
                .timeout(Duration.ofSeconds(10))
                .onErrorReturn(createErrorResponse("Authorization service unavailable"));
    }
    
    public Mono<AuthorizerResponse> authorizeCancellation() {
        return webClient
                .get()
                .uri("/")
                .retrieve()
                .bodyToMono(AuthorizerResponse.class)
                .timeout(Duration.ofSeconds(10))
                .onErrorReturn(createErrorResponse("Authorization service unavailable"));
    }
    
    private AuthorizerResponse createErrorResponse(String message) {
        return AuthorizerResponse.builder()
                .approved(false)
                .message(message)
                .build();
    }
}