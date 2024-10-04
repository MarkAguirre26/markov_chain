package com.baccarat.markovchain.module.controllers;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String homePage(Model model, CsrfToken csrfToken) {
        // Add CSRF token to the model
        model.addAttribute("_csrf", csrfToken);
        return "index";  // Return the name of your home page template (index.html)
    }
}
