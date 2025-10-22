package com.nimble.gateway.domain.repository;

import com.nimble.gateway.domain.entity.AuditLog;
import com.nimble.gateway.domain.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository {
    
    AuditLog save(AuditLog auditLog);
    List<AuditLog> findByUser(User user);
    List<AuditLog> findByAction(String action);
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId);
    List<AuditLog> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}
