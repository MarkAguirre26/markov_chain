package com.baccarat.markovchain.module.services.impl;

import com.baccarat.markovchain.module.controllers.BaccaratController;
import com.baccarat.markovchain.module.data.User;
import com.baccarat.markovchain.module.model.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MyUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(MyUserDetailsService.class);
    private final UserServiceImpl userService;
//    private final UserConfigService configService;

    @Autowired
    public MyUserDetailsService(UserServiceImpl userService) {
        this.userService = userService;
//        this.configService = configService;
    }


   @Override
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    // Attempt to find the user by username
    User user = userService.findByUsernameAndIsActive(username, 1);

    // If not found by username, attempt to find by email
    if (user == null) {
        logger.warn("{}: User Not Found by Username", username);
        System.out.println(String.format("%s: User Not Found by Username", username));
        user = userService.findByEmailAndIsActive(username, 1);
    }

    // Throw an exception if the user is not found
    if (user == null) {
        logger.warn("{}: User Not Found by Email", username);
        System.out.println(String.format("%s: User Not Found by Email", username));
        throw new UsernameNotFoundException("User not found with username or email: " + username);
    }

    // Log successful login
       logger.info("User logged-in: {}", user.getUsername());

    return new UserPrincipal(user, user.getUuid());
}

}
