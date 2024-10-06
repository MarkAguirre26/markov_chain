package com.baccarat.markovchain.module.repository;

import com.baccarat.markovchain.module.data.GameResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameResponseRepository extends JpaRepository<GameResponse, Integer> {

    GameResponse findByUserUuid(String userUuid);

   Optional<GameResponse> findFirstByUserUuidOrderByGameResponseIdDesc(String userUuid);

    List<GameResponse> findAllByUserUuid(String userUuid);

    void deleteByUserUuid(String userUuid);
}
