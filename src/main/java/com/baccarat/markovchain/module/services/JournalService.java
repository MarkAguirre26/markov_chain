package com.baccarat.markovchain.module.services;

import com.baccarat.markovchain.module.data.Journal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface JournalService {
    Journal saveJournal(Journal journal);
    Journal getJournalById(Integer journalId);
    List<Journal> getAllJournals();
    void deleteJournal(Integer journalId);
    List<Journal> getJournalsByUserUuidAndDateCreated(String userUuid, LocalDate dateCreated);
    List<Journal> getJournalsByUserUuidAndDateLastModified(String userUuid, LocalDateTime dateCreated);
}

