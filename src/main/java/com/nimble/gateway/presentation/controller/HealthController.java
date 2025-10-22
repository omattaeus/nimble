package com.nimble.gateway.presentation.controller;

import com.nimble.gateway.infrastructure.health.AuthorizerHealthIndicator;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Health check and monitoring endpoints")
public class HealthController {
    
    private final AuthorizerHealthIndicator authorizerHealthIndicator;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    
    @GetMapping("/authorizer")
    @Operation(summary = "Check authorizer service health")
    public ResponseEntity<Map<String, Object>> checkAuthorizerHealth() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean isHealthy = authorizerHealthIndicator.isHealthy();
            response.put("healthy", isHealthy);
            response.put("status", isHealthy ? "UP" : "DOWN");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            log.warn("Authorizer service is in invalid state: {}", e.getMessage());
            response.put("healthy", false);
            response.put("status", "DOWN");
            response.put("error", "Service unavailable");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(503).body(response);
        } catch (RuntimeException e) {
            log.error("Unexpected error checking authorizer health", e);
            response.put("healthy", false);
            response.put("status", "DOWN");
            response.put("error", "Internal server error");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    @GetMapping("/circuit-breaker")
    @Operation(summary = "Get circuit breaker status")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStatus() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("authorizerService");
            
            Map<String, Object> circuitBreakerInfo = new HashMap<>();
            circuitBreakerInfo.put("state", circuitBreaker.getState());
            circuitBreakerInfo.put("failureRate", circuitBreaker.getMetrics().getFailureRate());
            circuitBreakerInfo.put("numberOfBufferedCalls", circuitBreaker.getMetrics().getNumberOfBufferedCalls());
            circuitBreakerInfo.put("numberOfFailedCalls", circuitBreaker.getMetrics().getNumberOfFailedCalls());
            circuitBreakerInfo.put("numberOfSuccessfulCalls", circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
            circuitBreakerInfo.put("numberOfNotPermittedCalls", circuitBreaker.getMetrics().getNumberOfNotPermittedCalls());
            
            response.put("circuitBreaker", circuitBreakerInfo);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Circuit breaker 'authorizerService' not found: {}", e.getMessage());
            response.put("error", "Circuit breaker not configured");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(404).body(response);
        } catch (RuntimeException e) {
            log.error("Unexpected error getting circuit breaker status", e);
            response.put("error", "Internal server error");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(500).body(response);
        }
    }
}