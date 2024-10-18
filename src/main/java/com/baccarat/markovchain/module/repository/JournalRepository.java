package com.baccarat.markovchain.module.repository;

import com.baccarat.markovchain.module.data.Journal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JournalRepository extends JpaRepository<Journal, Integer> {
    // You can define custom query methods here if needed
    List<Journal> findByUserUuidAndDateCreated(String userUuid, LocalDate dateCreated);

    List<Journal> findByUserUuidAndDateLastModified(String userUuid, LocalDateTime dateCreated);

    @Query(value = "SELECT SUM(j.profit) AS total_profit, DATE(j.date_created) AS created_date " +
            "FROM journal j " +
            "WHERE j.user_uuid = :userUuid " +
            "GROUP BY DATE(j.date_created)", nativeQuery = true)
    List<Object[]> getTotalProfitByDate(@Param("userUuid") String userUuid);

    @Query(value = "SELECT SUM(j.profit) AS total_profit, WEEK(j.date_created) AS week_number " +
            "FROM journal j " +
            "WHERE j.user_uuid = :userUuid " +
            "GROUP BY YEAR(j.date_created),WEEK(j.date_created)", nativeQuery = true)
    List<Object[]> getTotalProfitByWeek(@Param("userUuid") String userUuid);

}
