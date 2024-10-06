package com.baccarat.markovchain.module.controllers;

import com.baccarat.markovchain.module.data.GameStatus;
import com.baccarat.markovchain.module.services.impl.GameStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/game-statuses")
public class GameStatusController {

    private final GameStatusService gameStatusService;

    @Autowired
    public GameStatusController(GameStatusService gameStatusService) {
        this.gameStatusService = gameStatusService;
    }

    // Get all game statuses
    @GetMapping
    public ResponseEntity<List<GameStatus>> getAllGameStatuses() {
        List<GameStatus> gameStatuses = gameStatusService.findAll();
        return ResponseEntity.ok(gameStatuses);
    }

    // Get a specific game status by ID
    @GetMapping("/{id}")
    public ResponseEntity<GameStatus> getGameStatusById(@PathVariable("id") Integer id) {
        Optional<GameStatus> gameStatus = gameStatusService.findById(id);
        return gameStatus.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Create a new game status
    @PostMapping
    public ResponseEntity<GameStatus> createGameStatus(@RequestBody GameStatus gameStatus) {
        GameStatus newGameStatus = gameStatusService.save(gameStatus);
        return ResponseEntity.ok(newGameStatus);
    }

    // Update an existing game status
//    @PutMapping("/{id}")
//    public ResponseEntity<GameStatus> updateGameStatus(@PathVariable("id") Integer id, @RequestBody GameStatus updatedGameStatus) {
//        GameStatus gameStatus = gameStatusService.updateGameStatus(id, updatedGameStatus);
//        if (gameStatus == null) {
//            return ResponseEntity.notFound().build();
//        }
//        return ResponseEntity.ok(gameStatus);
//    }

    // Delete a game status by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGameStatus(@PathVariable("id") Integer id) {
        Optional<GameStatus> gameStatus = gameStatusService.findById(id);
        if (gameStatus.isPresent()) {
            gameStatusService.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
