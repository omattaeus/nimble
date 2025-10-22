package com.nimble.gateway.infrastructure.health;

import com.nimble.gateway.infrastructure.external.AuthorizerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizerHealthIndicator {
    
    private final AuthorizerService authorizerService;
    
    public Mono<Boolean> health() {
        return authorizerService.authorizePayment()
                .timeout(Duration.ofSeconds(5))
                .map(authorized -> {
                    log.debug("Authorizer health check: {}", authorized ? "UP" : "DOWN");
                    return authorized;
                })
                .onErrorReturn(false)
                .doOnNext(isHealthy -> {
                    if (isHealthy) log.debug("Authorizer health check: UP");
                    else log.warn("Authorizer health check: DOWN");
                });
    }
    
    public boolean isHealthy() {
        try {
            Boolean isHealthy = authorizerService.authorizePayment()
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            return isHealthy != null;
        } catch (Exception e) {
            log.warn("Authorizer health check failed: {}", e.getMessage());
            return false;
        }
    }
}