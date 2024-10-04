package com.baccarat.markovchain.module.repository;


import com.baccarat.markovchain.module.data.Config;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConfigRepository extends JpaRepository<Config, Integer> {
    List<Config> findByUserUuid(String userUuid);
}