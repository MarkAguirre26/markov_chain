package com.baccarat.markovchain.module.services.impl;



import com.baccarat.markovchain.module.model.Session;
import com.baccarat.markovchain.module.repository.SessionRepository;
import com.baccarat.markovchain.module.services.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;

    @Autowired
    public SessionServiceImpl(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    public Session createSession(Session session) {
        session.setDateCreated(LocalDateTime.now());
        return sessionRepository.save(session);
    }

    @Override
    public Optional<Session> getSessionById(Integer sessionId) {
        return sessionRepository.findById(sessionId);
    }

    @Override
    public Optional<Session> getSessionByValueAndExpired(String value,Integer expired) {
        return Optional.ofNullable(sessionRepository.findByValueAndExpired(value,expired));
    }

    @Override
    public List<Session> getAllSessions() {
        return sessionRepository.findAll();
    }

    @Override
    public Session updateSession(Integer sessionId, Session sessionDetails) {
        Optional<Session> optionalSession = sessionRepository.findById(sessionId);
        if (optionalSession.isPresent()) {
            Session session = optionalSession.get();
            session.setValue(sessionDetails.getValue());
            session.setUserUuid(sessionDetails.getUserUuid());
            return sessionRepository.save(session);
        }
        return null;  // Or throw an exception if you prefer
    }

    @Override
    public void deleteSession(Integer sessionId) {
        sessionRepository.deleteById(sessionId);
    }
}
