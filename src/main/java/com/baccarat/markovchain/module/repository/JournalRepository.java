package com.baccarat.markovchain.module.repository;

import com.baccarat.markovchain.module.model.Journal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JournalRepository extends JpaRepository<Journal, Integer> {
    // You can define custom query methods here if needed
    List<Journal> findByUserUuidAndDateCreated(String userUuid,LocalDate dateCreated);
}
