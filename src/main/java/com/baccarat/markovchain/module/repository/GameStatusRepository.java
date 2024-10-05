package com.baccarat.markovchain.module.repository;

import com.baccarat.markovchain.module.data.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameStatusRepository extends JpaRepository<GameStatus, Integer> {
    // Additional query methods (if needed) can be defined here
    GameStatus findByUserUuid(String userUuid);
}
