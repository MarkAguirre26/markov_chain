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

    @Query(value = "SELECT \n" +
            "    SUM(j.profit) AS total_profit, \n" +
            "    DATE(j.date_created) AS created_date \n" +
            "FROM \n" +
            "    journal j \n" +
            "WHERE \n" +
            "    j.user_uuid = :userUuid\n" +
            "GROUP BY \n" +
            "    DATE(j.date_created);\n", nativeQuery = true)
    List<Object[]> getTotalProfitByDate(@Param("userUuid") String userUuid);

    @Query(value = "SELECT \n" +
            "    SUM(j.profit) AS total_profit, \n" +
            "    WEEK(j.date_created) AS week_number \n" +
            "FROM \n" +
            "    journal j \n" +
            "WHERE \n" +
            "    j.user_uuid = :userUuid\n" +
            "GROUP BY \n" +
            "   YEAR(j.date_created),WEEK(j.date_created)", nativeQuery = true)
    List<Object[]> getTotalProfitByWeek(@Param("userUuid") String userUuid);

}
