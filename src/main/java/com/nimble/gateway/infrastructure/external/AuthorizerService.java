package com.nimble.gateway.infrastructure.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizerService {
    
    private final AuthorizerClient authorizerClient;
    
    public Mono<Boolean> authorizePayment() {
        log.info("Authorizing payment transaction");
        
        return authorizerClient.authorizePayment()
                .doOnNext(response -> log.info("Payment authorization result: {}", response.isApproved()))
                .doOnError(error -> log.error("Payment authorization failed", error))
                .map(AuthorizerResponse::isApproved)
                .onErrorReturn(false);
    }
    
    public Mono<Boolean> authorizeDeposit() {
        log.info("Authorizing deposit transaction");
        
        return authorizerClient.authorizeDeposit()
                .doOnNext(response -> log.info("Deposit authorization result: {}", response.isApproved()))
                .doOnError(error -> log.error("Deposit authorization failed", error))
                .map(AuthorizerResponse::isApproved)
                .onErrorReturn(false);
    }
    
    public Mono<Boolean> authorizeCancellation() {
        log.info("Authorizing cancellation transaction");
        
        return authorizerClient.authorizeCancellation()
                .doOnNext(response -> log.info("Cancellation authorization result: {}", response.isApproved()))
                .doOnError(error -> log.error("Cancellation authorization failed", error))
                .map(AuthorizerResponse::isApproved)
                .onErrorReturn(false);
    }
}