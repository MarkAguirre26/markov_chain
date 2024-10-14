package com.baccarat.markovchain.module.response;


import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class JournalResponse {

    private Integer journalId;
    private Integer shoe;
    private Integer hand;
    private Integer profit;
    private String winLose;
    private LocalDate dateCreated;

    public JournalResponse(Integer journalId, Integer shoe, Integer hand, Integer profit, String winLose, LocalDate dateCreated) {
        this.journalId = journalId;
        this.shoe = shoe;
        this.hand = hand;
        this.profit = profit;
        this.winLose = winLose;
        this.dateCreated = dateCreated;
    }

}
