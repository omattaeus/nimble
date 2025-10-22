package com.nimble.gateway.infrastructure.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseHealthIndicator {
    
    private final DataSource dataSource;
    
    public boolean isHealthy() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) {
                try (PreparedStatement stmt = connection.prepareStatement(
                        "SELECT COUNT(*) FROM users LIMIT 1")) {
                    ResultSet rs = stmt.executeQuery();
                    rs.next();
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return false;
        }
    }
}