package com.baccarat.markovchain.module.services.impl;

import com.baccarat.markovchain.module.data.GameStatus;
import com.baccarat.markovchain.module.repository.GameStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Optional;

@Service
public class GameStatusService {

    private final GameStatusRepository gameStatusRepository;

    @Autowired
    public GameStatusService(GameStatusRepository gameStatusRepository) {
        this.gameStatusRepository = gameStatusRepository;
    }

    // Retrieve all game statuses
    public List<GameStatus> findAll() {
        return gameStatusRepository.findAll();
    }

    // Find a game status by its ID
    public Optional<GameStatus> findById(Integer gameResponseId) {
        return gameStatusRepository.findById(gameResponseId);
    }

    public Optional<GameStatus> findByUserUuid(String userUuid) {
        return Optional.ofNullable(gameStatusRepository.findByUserUuid(userUuid));
    }

    // Save a new or updated game status
    public GameStatus save(GameStatus gameStatus) {
        return gameStatusRepository.save(gameStatus);
    }

    // Delete a game status by its ID
    public void deleteById(Integer gameResponseId) {
        gameStatusRepository.deleteById(gameResponseId);
    }

    // Update game status data
    public GameStatus updateGameStatus(Integer gameResponseId, GameStatus updatedGameStatus) {
        Optional<GameStatus> existingGameStatus = gameStatusRepository.findById(gameResponseId);
        if (existingGameStatus.isPresent()) {
            GameStatus gameStatus = existingGameStatus.get();
            gameStatus.setUserUuid(updatedGameStatus.getUserUuid());
            gameStatus.setHandCount(updatedGameStatus.getHandCount());
            gameStatus.setWins(updatedGameStatus.getWins());
            gameStatus.setLosses(updatedGameStatus.getLosses());
            gameStatus.setProfit(updatedGameStatus.getProfit());
            gameStatus.setPlayingUnits(updatedGameStatus.getPlayingUnits());
            gameStatus.setDateLastUpdated(updatedGameStatus.getDateLastUpdated());
            return gameStatusRepository.save(gameStatus);
        }
        return null;  // Or handle not-found case as appropriate
    }
}
