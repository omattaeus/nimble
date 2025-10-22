package com.nimble.gateway.infrastructure.health;

import com.nimble.gateway.infrastructure.external.AuthorizerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizerHealthIndicator {
    
    private final AuthorizerService authorizerService;
    
    public boolean isHealthy() {
        try {
            Boolean isHealthy = authorizerService.authorizePayment()
                    .timeout(java.time.Duration.ofSeconds(5))
                    .block();
            
            return isHealthy != null;
        } catch (Exception e) {
            log.warn("Authorizer health check failed: {}", e.getMessage());
            return false;
        }
    }
}