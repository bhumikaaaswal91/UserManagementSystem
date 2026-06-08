package com.example.UserMangementSystem.controller;

import com.example.UserMangementSystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class DashboardController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        long verifiedUsers = userRepository.countByVerified(true);
        long unverifiedUsers = userRepository.countByVerified(false);

        model.addAttribute("verifiedUsers", verifiedUsers);
        model.addAttribute("unverifiedUsers", unverifiedUsers);

        return "dashboard";
    }
}