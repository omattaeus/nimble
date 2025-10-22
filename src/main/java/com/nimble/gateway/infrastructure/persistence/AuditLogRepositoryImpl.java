package com.nimble.gateway.infrastructure.persistence;

import com.nimble.gateway.domain.entity.AuditLog;
import com.nimble.gateway.domain.repository.AuditLogRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepositoryImpl extends JpaRepository<AuditLog, Long>, AuditLogRepository {
    
    @Override
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId ORDER BY a.createdAt DESC")
    List<AuditLog> findByUserId(@Param("userId") Long userId);
    
    @Override
    @Query("SELECT a FROM AuditLog a WHERE a.action = :action ORDER BY a.createdAt DESC")
    List<AuditLog> findByAction(@Param("action") String action);
    
    @Override
    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.createdAt DESC")
    List<AuditLog> findByEntityTypeAndEntityId(@Param("entityType") String entityType, @Param("entityId") Long entityId);
    
    @Override
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    List<AuditLog> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Override
    @Query("SELECT a FROM AuditLog a WHERE a.status = :status ORDER BY a.createdAt DESC")
    List<AuditLog> findByStatus(@Param("status") String status);
}
