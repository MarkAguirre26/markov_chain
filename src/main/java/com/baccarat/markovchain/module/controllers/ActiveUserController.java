package com.baccarat.markovchain.module.controllers;

import com.baccarat.markovchain.config.SessionListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ActiveUserController {

    private final SessionListener sessionListener;

    public ActiveUserController(SessionListener sessionListener) {
        this.sessionListener = sessionListener;
    }

    @GetMapping("/active-users")
    public int getActiveUsers() {
        return sessionListener.getActiveSessionCount();
    }
}
