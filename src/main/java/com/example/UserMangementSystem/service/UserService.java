package com.example.UserMangementSystem.service;

import com.example.UserMangementSystem.entity.User;
import com.example.UserMangementSystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ── CRUD Operations ──────────────────────────────────────

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    // ── Lookup ───────────────────────────────────────────────

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    public boolean emailExistsForOtherUser(String email, Long userId) {
        Optional<User> existing = userRepository.findByEmail(email);
        return existing.isPresent() && !existing.get().getId().equals(userId);
    }

    // ── Password Reset Token ─────────────────────────────────

    @Transactional
    public String createPasswordResetToken(User user) {
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
        userRepository.save(user);
        return token;
    }

    public User findByResetToken(String token) {
        return userRepository.findByResetToken(token).orElse(null);
    }

    public boolean isValidResetToken(String token) {
        User user = findByResetToken(token);
        return user != null && user.getResetTokenExpiry() != null
                && LocalDateTime.now().isBefore(user.getResetTokenExpiry());
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        User user = findByResetToken(token);
        if (user == null || user.getResetTokenExpiry() == null
                || LocalDateTime.now().isAfter(user.getResetTokenExpiry())) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
        return true;
    }

    // ── Change Password ──────────────────────────────────────

    @Transactional
    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUserById(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return false;
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }
}