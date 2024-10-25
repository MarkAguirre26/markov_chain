package com.baccarat.markovchain.module.services.impl;

import com.baccarat.markovchain.module.data.Session;
import com.baccarat.markovchain.module.repository.SessionRepository;
import com.baccarat.markovchain.module.services.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class SessionServiceImpl implements SessionService {

    @Autowired
    private SessionRepository sessionRepository;

    @Override
    public List<Session> getAllSessions() {
        return sessionRepository.findAll();
    }

    @Override
    public Optional<Session> getSessionById(Integer id) {
        return sessionRepository.findById(id);
    }

    @Override
    public Session createOrUpdateSession(Session session) {
        return sessionRepository.save(session);
    }

    @Override
    public void deleteSession(Integer id) {
        sessionRepository.deleteById(id);
    }
}
