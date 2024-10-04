package com.baccarat.markovchain.module.response;



import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class JournalResponse {

    private Integer shoe;
    private Integer hand;
    private Integer profit;
    private LocalDate dateCreated;

    public JournalResponse(Integer shoe, Integer hand, Integer profit, LocalDate dateCreated) {
        this.shoe = shoe;
        this.hand = hand;
        this.profit = profit;
        this.dateCreated = dateCreated;
    }

}
