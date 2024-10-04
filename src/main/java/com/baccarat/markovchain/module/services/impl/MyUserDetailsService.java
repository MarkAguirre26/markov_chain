package com.baccarat.markovchain.module.services.impl;

import com.baccarat.markovchain.module.data.User;
import com.baccarat.markovchain.module.model.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MyUserDetailsService implements UserDetailsService {


    private final UserServiceImpl userService;
    private final UserConfigService configService;

    @Autowired
    public MyUserDetailsService(UserServiceImpl userService, UserConfigService configService) {
        this.userService = userService;
        this.configService = configService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userService.findByUsernameAndIsActive(username, 1);
        if (user == null) {
            System.out.println(username+": User Not Found");
            throw new UsernameNotFoundException("user not found");
        }
        System.out.println("User logged-in: " + user.getUsername());


        return new UserPrincipal(user, user.getUuid());
    }
}
