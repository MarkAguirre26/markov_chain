package com.baccarat.markovchain.module.repository;

import com.baccarat.markovchain.module.data.GameResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameResponseRepository extends JpaRepository<GameResponse, Integer> {

    GameResponse findByUserUuid(String userUuid);
}
