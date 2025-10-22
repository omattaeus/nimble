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
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("data")
    private AuthorizerData data;
    
    public boolean isApproved() {
        return "success".equals(status) && data != null && Boolean.TRUE.equals(data.getAuthorized());
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorizerData {
        @JsonProperty("authorized")
        private Boolean authorized;
    }
}