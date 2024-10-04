package com.baccarat.markovchain.module.services.impl;

import com.baccarat.markovchain.module.data.Config;
import com.baccarat.markovchain.module.data.User;
import com.baccarat.markovchain.module.model.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

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
        User user = userService.findUserByUsername(username);
        if (user == null) {
            System.out.println("User Not Found");
            throw new UsernameNotFoundException("user not found");
        }



        return new UserPrincipal(user, user.getUuid());
    }
}
