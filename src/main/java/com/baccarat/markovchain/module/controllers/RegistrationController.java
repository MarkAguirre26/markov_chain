package com.baccarat.markovchain.module.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RegistrationController {

    @GetMapping("/register")
    public String showRegistrationPage(Model model) {
        // Add any model attributes if needed
        return "register";  // Return the name of your registration page (register.html)
    }
}
