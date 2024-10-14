package com.baccarat.markovchain.module.repository;

import com.baccarat.markovchain.module.data.GamesArchive;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GamesArchiveRepository extends JpaRepository<GamesArchive, Integer> {

    GamesArchive findByJournalId(Long journalId);

}
