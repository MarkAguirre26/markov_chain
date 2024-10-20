package com.baccarat.markovchain.module.controllers;

import com.baccarat.markovchain.module.common.concrete.UserConfig;
import com.baccarat.markovchain.module.common.concrete.UserRole;
import com.baccarat.markovchain.module.data.Config;
import com.baccarat.markovchain.module.data.User;
import com.baccarat.markovchain.module.services.impl.UserConfigService;
import com.baccarat.markovchain.module.services.impl.UserServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
public class RegistrationController {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationController.class);
    private final UserServiceImpl userService;
    private final UserConfigService userConfigService;

    @Autowired
    public RegistrationController(UserServiceImpl userService, UserConfigService userConfigService) {
        this.userService = userService;
        this.userConfigService = userConfigService;
    }


    @PostMapping("/join")
    public ResponseEntity<?> join(@RequestBody User user) {
        logger.info("Creating user: {}", user);

        User existingUsername = userService.findUserByUsername(user.getUsername());
        if (existingUsername != null) {
            logger.warn("Username already exists: {}", user.getUsername());
            return ResponseEntity.badRequest().body("username is not available!");
        }

        User existingUserEmailAddress = userService.findUserByEmail(user.getEmail());
        if (existingUserEmailAddress != null) {
            logger.warn("Email address already exists: {}", user.getEmail());
            return ResponseEntity.badRequest().body("email address is not available!");
        }


        user.setUuid(java.util.UUID.randomUUID().toString());
        user.setIsActive(1);
        user.setRole(UserRole.USER.getValue());
        user.setDateLastModified(LocalDateTime.now());
        user.setDateCreated(LocalDateTime.now());
        User createdUser = userService.createUser(user);


        Config dailyLimitConfig = new Config(createdUser.getUuid(), UserConfig.DAILY_LIMIT.getValue(), "100");
        userConfigService.saveOrUpdateConfig(dailyLimitConfig);
        logger.info("Saving daily limit config for {}", createdUser.getUsername());


        Config freezeConfig = new Config(createdUser.getUuid(), UserConfig.FREEZE.getValue(), "NO");
        userConfigService.saveOrUpdateConfig(freezeConfig);
        logger.info("Saving daily limit config for {}", createdUser.getUsername());



//        logger.info("User created: {}", createdUser);
        return ResponseEntity.ok("Created user" + user.getUsername());
    }
}
