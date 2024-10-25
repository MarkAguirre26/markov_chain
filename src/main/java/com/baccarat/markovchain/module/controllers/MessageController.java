package com.baccarat.markovchain.module.controllers;


import com.baccarat.markovchain.module.services.impl.WebSocketMessageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessageController {

    private final WebSocketMessageService webSocketMessageService;

    public MessageController(WebSocketMessageService webSocketMessageService) {
        this.webSocketMessageService = webSocketMessageService;
    }

    @PostMapping("/sendMessage")
    public void sendMessage(@RequestBody String message) {
        System.out.println("Received message: " + message);
        webSocketMessageService.sendMessage(message);
    }
}
