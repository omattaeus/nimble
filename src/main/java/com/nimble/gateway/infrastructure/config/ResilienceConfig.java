package com.nimble.gateway.infrastructure.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class ResilienceConfig {
    
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) 
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
        
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        
        registry.getEventPublisher().onEntryAdded(event -> {
            CircuitBreaker circuitBreaker = event.getAddedEntry();
            circuitBreaker.getEventPublisher()
                    .onStateTransition(event1 -> log.info("Circuit Breaker state transition: {} -> {}", 
                            event1.getStateTransition().getFromState(), 
                            event1.getStateTransition().getToState()))
                    .onFailureRateExceeded(event1 -> log.warn("Circuit Breaker failure rate exceeded: {}%", 
                            event1.getFailureRate()))
                    .onCallNotPermitted(event1 -> log.warn("Circuit Breaker call not permitted: {}", 
                            event1.getEventType()));
        });
        
        return registry;
    }
    
    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .build();
        
        RetryRegistry registry = RetryRegistry.of(config);
        
        registry.getEventPublisher().onEntryAdded(event -> {
            Retry retry = event.getAddedEntry();
            retry.getEventPublisher()
                    .onRetry(event1 -> log.info("Retry attempt {} for {}", 
                            event1.getNumberOfRetryAttempts(), 
                            event1.getName()))
                    .onError(event1 -> log.warn("Retry failed after {} attempts: {}", 
                            event1.getNumberOfRetryAttempts(), 
                            event1.getLastThrowable().getMessage()));
        });
        
        return registry;
    }
}