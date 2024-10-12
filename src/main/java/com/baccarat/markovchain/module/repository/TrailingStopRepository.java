package com.baccarat.markovchain.module.repository;

import com.baccarat.markovchain.module.data.TrailingStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrailingStopRepository extends JpaRepository<TrailingStop, Integer> {
    TrailingStop findByUserUuid(String userUuid);
}