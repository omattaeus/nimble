package com.nimble.gateway.infrastructure.audit;

import com.nimble.gateway.domain.entity.AuditLog;
import com.nimble.gateway.domain.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {
    
    private final AuditLogRepository auditLogRepository;
    
    public void logAction(String action, String entityType, String entityId, 
                         String oldValues, String newValues, UUID userId) {
        try {
            HttpServletRequest request = getCurrentRequest();
            String ipAddress = getClientIpAddress(request);
            String userAgent = request != null ? request.getHeader("User-Agent") : null;
            
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .oldValues(oldValues)
                    .newValues(newValues)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            auditLogRepository.save(auditLog);
            log.debug("Audit log created for action: {} by user: {}", action, userId);
            
        } catch (DataAccessException e) {
            log.error("Database error creating audit log for action: {}", action, e);
        } catch (RuntimeException e) {
            log.error("Unexpected error creating audit log for action: {}", action, e);
        }
    }
    
    public void logError(String action, String entityType, String entityId, 
                       String errorMessage, UUID userId) {
        try {
            HttpServletRequest request = getCurrentRequest();
            String ipAddress = getClientIpAddress(request);
            String userAgent = request != null ? request.getHeader("User-Agent") : null;
            
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            auditLogRepository.save(auditLog);
            log.warn("Audit error log created for action: {} by user: {}", action, userId);
            
        } catch (DataAccessException e) {
            log.error("Database error creating audit error log for action: {}", action, e);
        } catch (RuntimeException e) {
            log.error("Unexpected error creating audit error log for action: {}", action, e);
        }
    }
    
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (IllegalStateException e) {
            log.debug("Request context not available: {}", e.getMessage());
            return null;
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) return "unknown";
        
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) return xForwardedFor.split(",")[0].trim();
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) return xRealIp;
        
        return request.getRemoteAddr();
    }
}