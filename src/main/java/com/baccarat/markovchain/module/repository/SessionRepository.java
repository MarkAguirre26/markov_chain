package com.baccarat.markovchain.module.repository;

import com.baccarat.markovchain.module.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionRepository extends JpaRepository<Session, Integer> {

    Session findByValueAndExpired(String value,Integer expired);
}
