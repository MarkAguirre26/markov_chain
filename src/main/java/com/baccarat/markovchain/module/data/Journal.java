package com.baccarat.markovchain.module.data;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
//@AllArgsConstructor
@Entity
@Table(name = "journal")
public class Journal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "journal_id")
    private Integer journalId;

    @Column(name = "user_uuid")
    private String userUuid;

    @Column(name = "win_lose")
    private String winLose;

    @Column(name = "hand")
    private Integer hand;

    @Column(name = "profit")
    private Integer profit;

    @Column(name = "strategy")
    private String strategy;

    @Column(name = "date_last_modified")
    private LocalDateTime dateLastModified;

    @Column(name = "date_created", updatable = false)
    private LocalDate dateCreated;

    public Journal(Integer journalId, String userUuid, String winLose, Integer hand, Integer profit, String strategy) {
        this.journalId = journalId;
        this.userUuid = userUuid;
        this.winLose = winLose;
        this.hand = hand;
        this.profit = profit;
        this.strategy = strategy;
    }


}
