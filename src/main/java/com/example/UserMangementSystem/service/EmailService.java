package com.example.UserMangementSystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOtp(String to, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("OTP Verification");
        message.setText("Your OTP is: " + otp);

        mailSender.send(message);
    }

    public void sendPasswordResetEmail(String to, String resetLink) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("Password Reset - User Management System");
        message.setText(
            "Hello,\n\n" +
            "You requested a password reset. Click the link below to set a new password:\n\n" +
            resetLink + "\n\n" +
            "This link will expire in 30 minutes.\n\n" +
            "If you did not request this, please ignore this email.\n\n" +
            "Regards,\nUser Management System"
        );

        mailSender.send(message);
    }
}