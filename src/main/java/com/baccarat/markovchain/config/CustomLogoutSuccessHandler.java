package com.baccarat.markovchain.config;

import com.baccarat.markovchain.module.data.User;
import com.baccarat.markovchain.module.services.impl.UserServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.io.IOException;

public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {


    private final  UserServiceImpl userService;

    public CustomLogoutSuccessHandler(UserServiceImpl userService) {
        this.userService = userService;
    }




    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        // You can add custom actions here
//        System.out.println("User logged out: " + authentication.getDetails());
        // Redirect to a specific page after logout
//        String username = authentication.getName();
//        User user = userService.findUserByUsername(username);
//        user.setLogged(0);
//        userService.updateUser(user);



        response.sendRedirect("/auth");
    }
}
