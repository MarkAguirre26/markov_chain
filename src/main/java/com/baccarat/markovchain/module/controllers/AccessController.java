package com.baccarat.markovchain.module.controllers;

import com.baccarat.markovchain.module.data.User;
import com.baccarat.markovchain.module.services.impl.UserServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AccessController {

    private static final Logger logger = LoggerFactory.getLogger(AccessController.class);

    private final UserServiceImpl userService;

    @Autowired
    public AccessController(UserServiceImpl userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String showRegistrationPage(Model model) {
        // Add any model attributes if needed
        return "register";  // Return the name of your registration page (register.html)
    }

    @GetMapping("/auth")
    public String authenticationPage(Model model, CsrfToken csrfToken) {

        // Get the current authentication status
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if the user is already authenticated
        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            logger.info("User is already authenticated. Redirecting to home page.");
            // Redirect to home page if authenticated
            return "redirect:/";
        }

        // If not authenticated, proceed to the login page
        model.addAttribute("_csrf", csrfToken);
        return "auth"; // Return the name of the HTML file without the .html extension
    }


    // Handle the login request
    @PostMapping("/auth")
    public String authenticateUser(@RequestParam String username,
                                   @RequestParam String password,
                                   Model model) {
        // Custom authentication logic
        User user = userService.findUserByUsername(username); // Replace with your method to find a user
        if (user != null && passwordMatches(password, user.getPassword()) && user.getIsActive() == 1) { // Implement your password matching logic
            // Successful authentication logic
            // Set user session, etc.
            logger.info(user.getUsername() + ": successfully logged in.");
            return "redirect:/"; // Redirect to homepage after successful login
        } else {

            // Authentication failed
            model.addAttribute("error", "Invalid username or password");
            return "auth"; // Return to login page
        }
    }


    private boolean passwordMatches(String rawPassword, String encodedPassword) {
        return rawPassword.equals(encodedPassword); // Replace this with proper password encoding comparison
    }


}