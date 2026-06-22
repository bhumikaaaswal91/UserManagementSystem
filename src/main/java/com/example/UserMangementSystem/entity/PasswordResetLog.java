package com.example.UserMangementSystem.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_logs")
public class PasswordResetLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private LocalDateTime resetTime;

    public PasswordResetLog() {
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getResetTime() {
        return resetTime;
    }

    public void setResetTime(LocalDateTime resetTime) {
        this.resetTime = resetTime;
    }
}