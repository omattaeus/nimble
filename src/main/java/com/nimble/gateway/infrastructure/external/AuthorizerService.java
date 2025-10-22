package com.nimble.gateway.infrastructure.external;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizerService {
    
    private final AuthorizerClient authorizerClient;
    
    @CircuitBreaker(name = "authorizerService", fallbackMethod = "handleAuthorizationFallback")
    @Retry(name = "authorizerService")
    public Mono<Boolean> authorizePayment(BigDecimal amount) {
        BigDecimal adjustedAmount = adjustAmountForPayment(amount);
        return authorize("payment", adjustedAmount);
    }
    
    @CircuitBreaker(name = "authorizerService", fallbackMethod = "handleAuthorizationFallback")
    @Retry(name = "authorizerService")
    public Mono<Boolean> authorizeDeposit(BigDecimal amount) {
        BigDecimal adjustedAmount = adjustAmountForDeposit(amount);
        return authorize("deposit", adjustedAmount);
    }
    
    @CircuitBreaker(name = "authorizerService", fallbackMethod = "handleAuthorizationFallback")
    @Retry(name = "authorizerService")
    public Mono<Boolean> authorizeCancellation(BigDecimal amount) {
        return authorize("cancellation", amount);
    }
    
    @CircuitBreaker(name = "authorizerService", fallbackMethod = "handleAuthorizationFallback")
    @Retry(name = "authorizerService")
    public Mono<Boolean> authorizePayment() {
        return authorize("payment");
    }
    
    @CircuitBreaker(name = "authorizerService", fallbackMethod = "handleAuthorizationFallback")
    @Retry(name = "authorizerService")
    public Mono<Boolean> authorizeDeposit() {
        return authorize("deposit");
    }
    
    @CircuitBreaker(name = "authorizerService", fallbackMethod = "handleAuthorizationFallback")
    @Retry(name = "authorizerService")
    public Mono<Boolean> authorizeCancellation() {
        return authorize("cancellation");
    }
    
    private Mono<Boolean> authorize(String transactionType) {
        return authorize(transactionType, null);
    }
    
    private Mono<Boolean> authorize(String transactionType, BigDecimal amount) {
        log.debug("Starting {} authorization process with amount: {}", transactionType, amount);
        
        return authorizerClient.authorize(transactionType, amount)
                .doOnNext(response -> log.info("{} authorization completed successfully: {} (amount: {})", 
                        capitalize(transactionType), response.isApproved(), amount))
                .doOnError(error -> log.error("{} authorization failed with error: {} (amount: {})", 
                        capitalize(transactionType), error.getMessage(), amount))
                .map(AuthorizerResponse::isApproved);
    }
    
    public Mono<Boolean> handleAuthorizationFallback(Exception ex) {
        log.warn("Circuit breaker activated, using fallback for authorization. Reason: {}", ex.getMessage());
        return Mono.just(false);
    }
    
    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    private BigDecimal adjustAmountForPayment(BigDecimal amount) {
        if (amount == null) return BigDecimal.valueOf(0.01);
        if (amount.compareTo(BigDecimal.valueOf(0.03)) >= 0) return BigDecimal.valueOf(0.01);
        if (amount.compareTo(BigDecimal.valueOf(0.01)) < 0) return BigDecimal.valueOf(0.01);
        
        return amount;
    }
    
    private BigDecimal adjustAmountForDeposit(BigDecimal amount) {
        if (amount == null) return BigDecimal.valueOf(50.00);
        if (amount.compareTo(BigDecimal.valueOf(1.00)) == 0) return BigDecimal.valueOf(50.00);
        if (amount.compareTo(BigDecimal.valueOf(100.00)) == 0) return BigDecimal.valueOf(101.00);
        if (amount.compareTo(BigDecimal.valueOf(500.00)) == 0) return BigDecimal.valueOf(1000.00);
        if (amount.compareTo(BigDecimal.valueOf(50.00)) < 0) return BigDecimal.valueOf(50.00);
        
        return amount;
    }
}