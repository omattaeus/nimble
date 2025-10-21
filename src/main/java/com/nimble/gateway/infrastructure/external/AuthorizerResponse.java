package com.nimble.gateway.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizerResponse {
    
    @JsonProperty("approved")
    private Boolean approved;
    
    @JsonProperty("authorization_code")
    private String authorizationCode;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("transaction_id")
    private String transactionId;
    
    public boolean isApproved() {
        return Boolean.TRUE.equals(approved);
    }
}