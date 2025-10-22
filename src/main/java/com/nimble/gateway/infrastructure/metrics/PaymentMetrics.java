package com.nimble.gateway.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public void recordPaymentSuccess(String method) {
        Counter.builder("payment.success")
                .description("Number of successful payments")
                .register(meterRegistry)
                .increment();
        
        if ("BALANCE".equals(method)) {
            Counter.builder("payment.method.balance")
                    .description("Number of payments by balance")
                    .register(meterRegistry)
                    .increment();
        } else if ("CREDIT_CARD".equals(method)) {
            Counter.builder("payment.method.credit_card")
                    .description("Number of payments by credit card")
                    .register(meterRegistry)
                    .increment();
        }
        
        log.info("Payment success recorded for method: {}", method);
    }
    
    public void recordPaymentFailure(String reason) {
        Counter.builder("payment.failure")
                .description("Number of failed payments")
                .register(meterRegistry)
                .increment();
        
        log.warn("Payment failure recorded: {}", reason);
    }
    
    public void recordChargeCreated() {
        Counter.builder("charge.created")
                .description("Number of charges created")
                .register(meterRegistry)
                .increment();
        
        log.info("Charge created metric recorded");
    }
    
    public void recordChargeCancelled() {
        Counter.builder("charge.cancelled")
                .description("Number of charges cancelled")
                .register(meterRegistry)
                .increment();
        
        log.info("Charge cancelled metric recorded");
    }
    
    public void recordDepositSuccess(BigDecimal amount) {
        Counter.builder("deposit.success")
                .description("Number of successful deposits")
                .register(meterRegistry)
                .increment();
        
        log.info("Deposit success recorded for amount: {}", amount);
    }
    
    public Timer.Sample startPaymentProcessingTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void stopPaymentProcessingTimer(Timer.Sample sample) {
        Timer timer = Timer.builder("payment.processing.time")
                .description("Time taken to process payments")
                .register(meterRegistry);
        sample.stop(timer);
    }
    
    public Timer.Sample startAuthorizationTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void stopAuthorizationTimer(Timer.Sample sample) {
        Timer timer = Timer.builder("authorization.time")
                .description("Time taken for external authorization")
                .register(meterRegistry);
        sample.stop(timer);
    }
}