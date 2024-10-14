package com.baccarat.markovchain.module.services.impl;

import com.baccarat.markovchain.module.data.GamesArchive;
import com.baccarat.markovchain.module.repository.GamesArchiveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class GamesArchiveService {

    private final GamesArchiveRepository gamesArchiveRepository;

    @Autowired
    public GamesArchiveService(GamesArchiveRepository gamesArchiveRepository) {
        this.gamesArchiveRepository = gamesArchiveRepository;
    }

    // Get all games archive records
    public List<GamesArchive> getAllGamesArchive() {
        return gamesArchiveRepository.findAll();
    }

    // Get a single game archive by its ID
    public Optional<GamesArchive> getGameArchiveById(Integer id) {
        return gamesArchiveRepository.findById(id);
    }

    public GamesArchive findByJournalId(Long id) {
        return gamesArchiveRepository.findByJournalId(id);
    }

    // Add a new game archive record
    public GamesArchive addGameArchive(GamesArchive gamesArchive) {
        return gamesArchiveRepository.save(gamesArchive);
    }

    // Update an existing game archive record
    public GamesArchive updateGameArchive(Integer id, GamesArchive updatedGameArchive) {
        return gamesArchiveRepository.findById(id).map(game -> {
            game.setJournalId(updatedGameArchive.getJournalId());
            game.setBaseBetUnit(updatedGameArchive.getBaseBetUnit());
            game.setSuggestedBetUnit(updatedGameArchive.getSuggestedBetUnit());
            game.setLossCounter(updatedGameArchive.getLossCounter());
            game.setRecommendedBet(updatedGameArchive.getRecommendedBet());
            game.setSequence(updatedGameArchive.getSequence());
            game.setHandResult(updatedGameArchive.getHandResult());
            game.setMessage(updatedGameArchive.getMessage());
            return gamesArchiveRepository.save(game);
        }).orElseThrow(() -> new RuntimeException("Game archive not found with id: " + id));
    }

    // Delete a game archive by its ID
    public void deleteGameArchive(Integer id) {
        gamesArchiveRepository.deleteById(id);
    }
}
