package com.baccarat.markovchain.module.services.impl;

import com.baccarat.markovchain.module.data.GameResponse;
import com.baccarat.markovchain.module.repository.GameResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GameResponseService {

    private final GameResponseRepository gameResponseRepository;

    /**
     * Fetch all game responses from the database.
     *
     * @return List of GameResponse
     */
    public List<GameResponse> getAllGameResponses() {
        return gameResponseRepository.findAll();
    }

    /**
     * Fetch a game response by its ID.
     *
     * @param id The ID of the GameResponse to retrieve
     * @return An optional containing the GameResponse if found, or empty if not
     */
    public Optional<GameResponse> getGameResponseById(Integer id) {
        return gameResponseRepository.findById(id);
    }

    public GameResponse getGameResponseByUserUuid(String userUuid) {
        return gameResponseRepository.findByUserUuid(userUuid);
    }


    /**
     * Create a new GameResponse and save it to the database.
     *
     * @param gameResponse The GameResponse entity to save
     * @return The saved GameResponse
     */
    public GameResponse createOrUpdateGameResponse(GameResponse gameResponse) {
        return gameResponseRepository.save(gameResponse);
    }

    /**
     * Update an existing GameResponse in the database.
     *
     * @param id           The ID of the GameResponse to update
     * @param newResponse  The updated GameResponse entity
     * @return The updated GameResponse entity, or throws an exception if not found
     */
    @Transactional
    public GameResponse updateGameResponse(Integer id, GameResponse newResponse) {
        return gameResponseRepository.findById(id)
                .map(existingResponse -> {
                    existingResponse.setUserUuid(newResponse.getUserUuid());
                    existingResponse.setBaseBetUnit(newResponse.getBaseBetUnit());
                    existingResponse.setSuggestedBetUnit(newResponse.getSuggestedBetUnit());
                    existingResponse.setInitialPlayingUnits(newResponse.getInitialPlayingUnits());
                    existingResponse.setRecommendedBet(newResponse.getRecommendedBet());
                    existingResponse.setSequence(newResponse.getSequence());
                    existingResponse.setMessage(newResponse.getMessage());
                    existingResponse.setDateLastUpdated(newResponse.getDateLastUpdated());
                    return gameResponseRepository.save(existingResponse);
                })
                .orElseThrow(() -> new RuntimeException("GameResponse with id " + id + " not found"));
    }

    /**
     * Delete a GameResponse by its ID.
     *
     * @param id The ID of the GameResponse to delete
     */
    public void deleteGameResponse(Integer id) {
        if (!gameResponseRepository.existsById(id)) {
            throw new RuntimeException("GameResponse with id " + id + " does not exist.");
        }
        gameResponseRepository.deleteById(id);
    }
}
