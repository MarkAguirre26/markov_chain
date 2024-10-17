package com.baccarat.markovchain.config;


import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SessionListener implements HttpSessionListener {

    private final AtomicInteger activeSessions = new AtomicInteger();

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        activeSessions.incrementAndGet();
        System.out.println("Session Created: " + se.getSession().getId() + " | Active Sessions: " + activeSessions.get());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        activeSessions.decrementAndGet();
        System.out.println("Session Destroyed: " + se.getSession().getId() + " | Active Sessions: " + activeSessions.get());
    }

    public int getActiveSessionCount() {
        return activeSessions.get();
    }
}
