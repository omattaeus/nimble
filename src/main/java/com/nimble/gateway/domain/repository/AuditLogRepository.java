package com.nimble.gateway.domain.repository;

import com.nimble.gateway.domain.entity.AuditLog;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository {
    
    AuditLog save(AuditLog auditLog);
    List<AuditLog> findByUserId(Long userId);
    List<AuditLog> findByAction(String action);
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);
    List<AuditLog> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    List<AuditLog> findByStatus(String status);
}
