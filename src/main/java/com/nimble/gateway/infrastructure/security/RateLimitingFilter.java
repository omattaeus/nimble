package com.nimble.gateway.infrastructure.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    
    private final Cache<String, AtomicInteger> requestCounts = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(Duration.ofMinutes(1))
            .build();
    
    private final Cache<String, AtomicInteger> loginAttempts = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(15))
            .build();
    
    private static final int MAX_REQUESTS_PER_MINUTE = 1000;
    private static final int MAX_LOGIN_ATTEMPTS_PER_15_MINUTES = 50;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                 FilterChain filterChain) throws ServletException, IOException {
        
        String clientIp = getClientIpAddress(request);
        String requestPath = request.getRequestURI();
        
        if (isPublicEndpoint(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        if (requestPath.contains("/api/auth/login")) {
            if (isLoginRateLimited(clientIp)) {
                log.warn("Login rate limit exceeded for IP: {}", clientIp);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("{\"error\":\"Too many login attempts. Please try again later.\"}");
                response.setContentType("application/json");
                return;
            }
        }
        
        if (isGeneralRateLimited(clientIp)) {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\"}");
            response.setContentType("application/json");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    private boolean isLoginRateLimited(String clientIp) {
        AtomicInteger attempts = loginAttempts.get(clientIp, k -> new AtomicInteger(0));
        int currentAttempts = attempts.incrementAndGet();
        return currentAttempts > MAX_LOGIN_ATTEMPTS_PER_15_MINUTES;
    }
    
    private boolean isGeneralRateLimited(String clientIp) {
        AtomicInteger requests = requestCounts.get(clientIp, k -> new AtomicInteger(0));
        int currentRequests = requests.incrementAndGet();
        return currentRequests > MAX_REQUESTS_PER_MINUTE;
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) return xRealIp;
        
        return request.getRemoteAddr();
    }
    
    private boolean isPublicEndpoint(String requestPath) {
        return requestPath.startsWith("/actuator/") || 
               requestPath.startsWith("/swagger-ui") || 
               requestPath.equals("/swagger-ui.html") ||
               requestPath.startsWith("/v3/api-docs") ||
               requestPath.startsWith("/swagger-resources") ||
               requestPath.startsWith("/webjars") ||
               (requestPath.startsWith("/api/auth/") && !requestPath.contains("/login"));
    }
}