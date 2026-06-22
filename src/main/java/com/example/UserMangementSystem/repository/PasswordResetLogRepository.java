package com.example.UserMangementSystem.repository;

import com.example.UserMangementSystem.entity.PasswordResetLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PasswordResetLogRepository
        extends JpaRepository<PasswordResetLog, Long> {

    @Query(value = """
        SELECT DATE(reset_time) as resetDate,
               COUNT(*) as total
        FROM password_reset_logs
        WHERE reset_time >= :startDate
        GROUP BY DATE(reset_time)
        ORDER BY DATE(reset_time)
        """,
        nativeQuery = true)
    List<Object[]> getPasswordResetStats(
            @Param("startDate") LocalDateTime startDate);
}