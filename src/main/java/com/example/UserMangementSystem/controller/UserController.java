package com.example.UserMangementSystem.controller;

import com.example.UserMangementSystem.dto.ChangePasswordDto;
import com.example.UserMangementSystem.dto.ResetPasswordDto;
import com.example.UserMangementSystem.entity.User;
import com.example.UserMangementSystem.service.EmailService;
import com.example.UserMangementSystem.service.UserService;
import com.example.UserMangementSystem.util.OtpUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
public class UserController {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    // ── Dashboard / User List ────────────────────────────────

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "index";
    }

    // ── Login ────────────────────────────────────────────────

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // ── Register ─────────────────────────────────────────────

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid User user, BindingResult result, Model model) {
        // Check for duplicate email
        if (user.getEmail() != null && userService.emailExists(user.getEmail())) {
            result.rejectValue("email", "duplicate", "This email is already registered");
        }

        if (result.hasErrors()) {
            return "register";
        }

        String otp = OtpUtil.generateOtp();

        user.setOtp(otp);
        user.setOtpGeneratedTime(LocalDateTime.now());
        user.setVerified(false);

        // Encode password before saving
        user.setPassword(
            passwordEncoder.encode(user.getPassword())
        );

        userService.saveUser(user);

        emailService.sendOtp(user.getEmail(), otp);

        model.addAttribute("email", user.getEmail());

        return "verifyOtp";
    }

    // ── Verify OTP ───────────────────────────────────────────

    @PostMapping("/verify")
    public String verifyOtp(
            @RequestParam String email,
            @RequestParam String otp,
            Model model) {

        User user = userService.findByEmail(email);

        if (user == null) {
            model.addAttribute("error", "User not found");
            return "verifyOtp";
        }

        if (user.getOtp() != null &&
            user.getOtp().equals(otp)) {

            user.setVerified(true);
            user.setOtp(null);

            userService.saveUser(user);

            return "redirect:/login?verified";
        }

        model.addAttribute("error", "Invalid OTP");
        model.addAttribute("email", email);

        return "verifyOtp";
    }

    // ── Edit User ────────────────────────────────────────────

    @GetMapping("/edit/{id}")
    public String showEditPage(
            @PathVariable("id") Long id,
            Model model) {

        User user = userService.getUserById(id);
        // Clear password so form shows empty (user can leave blank to keep existing)
        user.setPassword("");
        model.addAttribute("user", user);

        return "edit";
    }

    @PostMapping("/edit/{id}")
    public String updateUser(
            @PathVariable("id") Long id,
            @Valid User user,
            BindingResult result,
            Model model) {

        User existingUser = userService.getUserById(id);

        // Check for duplicate email (exclude current user)
        if (user.getEmail() != null && userService.emailExistsForOtherUser(user.getEmail(), id)) {
            result.rejectValue("email", "duplicate", "This email is already registered to another user");
        }

        // If password is blank, keep existing hashed password
        boolean passwordChanged = user.getPassword() != null && !user.getPassword().isBlank();

        if (!passwordChanged) {
            // Filter out password-related errors since it's optional on edit
            boolean hasNonPasswordErrors = result.getFieldErrors().stream()
                    .anyMatch(e -> !"password".equals(e.getField()));
            if (hasNonPasswordErrors) {
                return "edit";
            }
            user.setPassword(existingUser.getPassword());
        } else {
            if (result.hasErrors()) {
                return "edit";
            }
            // Hash the new password
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        user.setId(id);
        // Preserve fields not in the edit form
        user.setRole(existingUser.getRole());
        user.setOtp(existingUser.getOtp());
        user.setOtpGeneratedTime(existingUser.getOtpGeneratedTime());
        user.setResetToken(existingUser.getResetToken());
        user.setResetTokenExpiry(existingUser.getResetTokenExpiry());

        userService.saveUser(user);

        return "redirect:/";
    }

    // ── Delete User ──────────────────────────────────────────

    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable("id") Long id) {

        userService.deleteUser(id);

        return "redirect:/";
    }

    // ── Forgot Password ──────────────────────────────────────

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(
            @RequestParam("email") String email,
            HttpServletRequest request,
            Model model) {

        if (email == null || email.isBlank()) {
            model.addAttribute("error", "Please enter your email address");
            return "forgot-password";
        }

        User user = userService.findByEmail(email.trim());
        if (user == null) {
            model.addAttribute("error", "No account found with that email address");
            model.addAttribute("email", email);
            return "forgot-password";
        }

        String token = userService.createPasswordResetToken(user);

        // Build full reset link
        String baseUrl = request.getScheme() + "://" + request.getServerName()
                + ":" + request.getServerPort();
        String resetLink = baseUrl + "/reset-password?token=" + token;

        // Send email with reset link
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
            model.addAttribute("success", "A password reset link has been sent to your email address.");
        } catch (Exception e) {
            // Fallback: show the link on screen if email fails
            model.addAttribute("success", "Reset link generated (email delivery failed):");
            model.addAttribute("resetLink", "/reset-password?token=" + token);
        }

        return "forgot-password";
    }

    // ── Reset Password ───────────────────────────────────────

    @GetMapping("/reset-password")
    public String showResetPasswordPage(
            @RequestParam("token") String token,
            Model model) {

        if (!userService.isValidResetToken(token)) {
            model.addAttribute("invalidToken", true);
            model.addAttribute("resetSuccess", false);
            return "reset-password";
        }

        ResetPasswordDto dto = new ResetPasswordDto();
        dto.setToken(token);
        model.addAttribute("resetPasswordDto", dto);
        model.addAttribute("invalidToken", false);
        model.addAttribute("resetSuccess", false);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(
            @Valid ResetPasswordDto dto,
            BindingResult result,
            Model model) {

        // Cross-field validation: passwords must match
        if (dto.getNewPassword() != null && !dto.getNewPassword().equals(dto.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
        }

        if (result.hasErrors()) {
            model.addAttribute("resetPasswordDto", dto);
            model.addAttribute("invalidToken", false);
            model.addAttribute("resetSuccess", false);
            return "reset-password";
        }

        boolean success = userService.resetPassword(dto.getToken(), dto.getNewPassword());
        if (!success) {
            model.addAttribute("invalidToken", true);
            model.addAttribute("resetSuccess", false);
            return "reset-password";
        }

        model.addAttribute("resetSuccess", true);
        model.addAttribute("invalidToken", false);
        return "reset-password";
    }

    // ── Change Password ──────────────────────────────────────

    @GetMapping("/change-password/{id}")
    public String showChangePasswordPage(
            @PathVariable("id") Long id,
            Model model) {

        // Verify user exists
        userService.getUserById(id);
        model.addAttribute("changePasswordDto", new ChangePasswordDto());
        model.addAttribute("userId", id);
        return "change-password";
    }

    @PostMapping("/change-password/{id}")
    public String processChangePassword(
            @PathVariable("id") Long id,
            @Valid ChangePasswordDto dto,
            BindingResult result,
            Model model) {

        model.addAttribute("userId", id);

        // Cross-field validation: passwords must match
        if (dto.getNewPassword() != null && !dto.getNewPassword().equals(dto.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
        }

        if (result.hasErrors()) {
            return "change-password";
        }

        boolean success = userService.changePassword(id, dto.getCurrentPassword(), dto.getNewPassword());
        if (!success) {
            result.rejectValue("currentPassword", "incorrect", "Current password is incorrect");
            return "change-password";
        }

        model.addAttribute("changeSuccess", true);
        return "change-password";
    }
}