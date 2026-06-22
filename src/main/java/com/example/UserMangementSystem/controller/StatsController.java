package com.example.UserMangementSystem.controller;

import com.example.UserMangementSystem.dto.PasswordResetStatsDto;
import com.example.UserMangementSystem.repository.PasswordResetLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private PasswordResetLogRepository passwordResetLogRepository;

    @GetMapping("/password-resets")
    public List<PasswordResetStatsDto> getPasswordResetStats(
            @RequestParam(defaultValue = "30") int days) {

        LocalDateTime startDate =
                LocalDateTime.now().minusDays(days);

        return passwordResetLogRepository
                .getPasswordResetStats(startDate)
                .stream()
                .map(row -> new PasswordResetStatsDto(
                        row[0].toString(),
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }
}