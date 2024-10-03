package com.baccarat.markovchain.module.services;




import com.baccarat.markovchain.module.model.Session;

import java.util.List;
import java.util.Optional;

public interface SessionService {

    // Create a new session
    Session createSession(Session session);

    // Retrieve a session by ID
    Optional<Session> getSessionById(Integer sessionId);

    // Retrieve a session by ID
    Optional<Session> getSessionByValueAndExpired(String value,Integer expired);



    // Retrieve all sessions
    List<Session> getAllSessions();

    // Update a session
    Session updateSession(Integer sessionId, Session sessionDetails);

    // Delete a session
    void deleteSession(Integer sessionId);
}
