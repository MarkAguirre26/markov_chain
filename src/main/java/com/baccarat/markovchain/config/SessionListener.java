package com.baccarat.markovchain.config;

import com.baccarat.markovchain.module.data.Session;
import com.baccarat.markovchain.module.services.impl.SessionServiceImpl;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SessionListener implements HttpSessionListener {

    private static final Logger logger = LoggerFactory.getLogger(SessionListener.class);

    private final AtomicInteger activeSessions = new AtomicInteger();
    private final AtomicInteger highestSessionCount = new AtomicInteger();

    @Autowired
    private SessionServiceImpl sessionService;

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        int currentActiveSessions = activeSessions.incrementAndGet();
        System.out.println("Session Created: " + se.getSession().getId() + " | Active Sessions: " + currentActiveSessions);

        // Update highest session count if current active sessions exceed the highest count
        updateHighestSessionCount(currentActiveSessions);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        int currentActiveSessions = activeSessions.decrementAndGet();
        System.out.println("Session Destroyed: " + se.getSession().getId() + " | Active Sessions: " + currentActiveSessions);
    }

    public int getActiveSessionCount() {
        return activeSessions.get();
    }

    public int getHighestSessionCount() {
        return highestSessionCount.get();
    }

    private synchronized void updateHighestSessionCount(int currentActiveSessions) {
        // Double-check within synchronized method to avoid race conditions
        if (currentActiveSessions > highestSessionCount.get()) {
            highestSessionCount.set(currentActiveSessions);

            // Save the new highest session count to the database
            saveHighestSessionCountToDatabase(currentActiveSessions);

            System.out.println("New Highest Session Count: " + currentActiveSessions);
        }
    }


    private void saveHighestSessionCountToDatabase(int highestSessionCount) {

        Session session = new Session();
        session.setCount(highestSessionCount);
        session.setDateCreated(LocalDateTime.now());
        sessionService.createOrUpdateSession(session);

        logger.info("Highest Session Count Updated and Saved to Database: " + highestSessionCount);

    }
}
