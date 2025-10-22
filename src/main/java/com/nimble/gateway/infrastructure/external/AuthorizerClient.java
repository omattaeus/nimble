package com.nimble.gateway.infrastructure.external;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;

@Component
public class AuthorizerClient {
    
    private final WebClient webClient;
    
    public AuthorizerClient(
            WebClient.Builder webClientBuilder,
            @Value("${external.authorizer.url:https://zsy6tx7aql.execute-api.sa-east-1.amazonaws.com}") String authorizerUrl) {
        this.webClient = webClientBuilder
                .baseUrl(authorizerUrl)
                .build();
    }
    
    public Mono<AuthorizerResponse> authorize(String transactionType) {
        return authorize(transactionType, null);
    }
    
    public Mono<AuthorizerResponse> authorize(String transactionType, BigDecimal amount) {
        return webClient
                .get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/authorizer");
                    if (transactionType != null) builder.queryParam("type", transactionType);
                    if (amount != null) builder.queryParam("amount", amount);
                    return builder.build();
                })
                .retrieve()
                .bodyToMono(AuthorizerResponse.class)
                .timeout(Duration.ofSeconds(10))
                .onErrorReturn(createErrorResponse("Authorization service unavailable"));
    }
    
    public Mono<AuthorizerResponse> authorizePayment(BigDecimal amount) {
        return authorize("payment", amount);
    }
    
    public Mono<AuthorizerResponse> authorizeDeposit(BigDecimal amount) {
        return authorize("deposit", amount);
    }
    
    public Mono<AuthorizerResponse> authorizeCancellation(BigDecimal amount) {
        return authorize("cancellation", amount);
    }
    
    public Mono<AuthorizerResponse> authorizePayment() {
        return authorize("payment");
    }
    
    public Mono<AuthorizerResponse> authorizeDeposit() {
        return authorize("deposit");
    }
    
    public Mono<AuthorizerResponse> authorizeCancellation() {
        return authorize("cancellation");
    }
    
    private AuthorizerResponse createErrorResponse(String message) {
        return AuthorizerResponse.builder()
                .status("fail")
                .data(AuthorizerResponse.AuthorizerData.builder()
                        .authorized(false)
                        .build())
                .build();
    }
}