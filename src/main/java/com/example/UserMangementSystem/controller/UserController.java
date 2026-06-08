package com.example.UserMangementSystem.controller;

import com.example.UserMangementSystem.entity.User;
import com.example.UserMangementSystem.service.UserService;
import com.example.UserMangementSystem.service.EmailService;
import com.example.UserMangementSystem.util.OtpUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;

@Controller
public class UserController {
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "index";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }
    
    @PostMapping("/register")
    public String registerUser(User user, Model model) {

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

    @GetMapping("/edit/{id}")
    public String showEditPage(
            @PathVariable("id") Long id,
            Model model) {

        User user = userService.getUserById(id);
        model.addAttribute("user", user);

        return "edit";
    }

    @PostMapping("/edit/{id}")
    public String updateUser(
            @PathVariable("id") Long id,
            User user) {

        user.setId(id);
        userService.saveUser(user);

        return "redirect:/";
    }

    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable("id") Long id) {

        userService.deleteUser(id);

        return "redirect:/";
    }
}