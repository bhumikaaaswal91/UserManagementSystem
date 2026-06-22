package com.example.UserMangementSystem.dto;

public class PasswordResetStatsDto {

    private String date;
    private Long count;

    public PasswordResetStatsDto(String date, Long count) {
        this.date = date;
        this.count = count;
    }

    public String getDate() {
        return date;
    }

    public Long getCount() {
        return count;
    }
}