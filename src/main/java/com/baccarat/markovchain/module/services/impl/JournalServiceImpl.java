package com.baccarat.markovchain.module.services.impl;

import com.baccarat.markovchain.module.data.Journal;
import com.baccarat.markovchain.module.repository.JournalRepository;
import com.baccarat.markovchain.module.services.JournalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class JournalServiceImpl implements JournalService {

    private final JournalRepository journalRepository;

    @Autowired
    public JournalServiceImpl(JournalRepository journalRepository) {
        this.journalRepository = journalRepository;
    }

    @Override
    public Journal saveJournal(Journal journal) {
        return journalRepository.save(journal);
    }


    @Override
    public Journal getJournalById(Integer journalId) {
        Optional<Journal> optionalJournal = journalRepository.findById(journalId);
        return optionalJournal.orElse(null); // Return null if not found, or you can throw an exception
    }

    @Override
    public List<Journal> getAllJournals() {
        return journalRepository.findAll();
    }

    @Override
    public void deleteJournal(Integer journalId) {
        journalRepository.deleteById(journalId);
    }

    @Override
    public List<Journal> getJournalsByUserUuidAndDateCreated(String userUuid, LocalDate dateCreated) {
        return journalRepository.findByUserUuidAndDateCreated(userUuid,dateCreated);
    }
}
