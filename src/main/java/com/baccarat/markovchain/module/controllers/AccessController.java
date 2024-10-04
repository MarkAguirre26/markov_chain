package com.baccarat.markovchain.module.controllers;

import com.baccarat.markovchain.module.data.User;
import com.baccarat.markovchain.module.services.impl.UserServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class AccessController {

    private static final Logger logger = LoggerFactory.getLogger(AccessController.class);

    private final UserServiceImpl userService;

    @Autowired
    public AccessController(UserServiceImpl userService) {
        this.userService = userService;
    }


    @GetMapping("/authentication")
    public String authenticationPage(Model model, CsrfToken csrfToken) {

        logger.info("Authentication page requested. "+csrfToken.getToken());

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
        return "authentication"; // Return the name of the HTML file without the .html extension
    }
    // Handle the login request
    @PostMapping("/authentication")
    public String authenticateUser(@RequestParam String username,
                                   @RequestParam String password,
                                   Model model) {
        // Custom authentication logic
        User user = userService.findUserByUsername(username); // Replace with your method to find a user
        if (user != null && passwordMatches(password, user.getPassword())) { // Implement your password matching logic
            // Successful authentication logic
            // Set user session, etc.
            return "redirect:/"; // Redirect to homepage after successful login
        } else {
            // Authentication failed
            model.addAttribute("error", "Invalid username or password");
            return "authentication"; // Return to login page
        }
    }


//    @GetMapping("/authentication")
//    public String authenticationPage(Model model, CsrfToken csrfToken) {
//        // Get the current authentication status
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//
//        // Check if the user is already authenticated
//        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
//            // Redirect to home page if authenticated
//            return "redirect:/";
//        }
//
//        // If not authenticated, proceed to the login page
//        model.addAttribute("_csrf", csrfToken);
//        return "authentication";  // Your login page template
//    }



    private boolean passwordMatches(String rawPassword, String encodedPassword) {
        return rawPassword.equals(encodedPassword); // Replace this with proper password encoding comparison
    }


}