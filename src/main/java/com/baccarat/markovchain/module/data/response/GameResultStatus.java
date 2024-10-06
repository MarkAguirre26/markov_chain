package com.baccarat.markovchain.module.data.response;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Setter
@Getter
public class GameResultStatus {



    private Integer handCount;
    private Integer wins;
    private Integer losses;
    private Integer profit;
    private Integer playingUnits;

    public GameResultStatus(Integer handCount, Integer wins, Integer losses, Integer profit, Integer playingUnits) {
        this.handCount = handCount;
        this.wins = wins;
        this.losses = losses;
        this.profit = profit;
        this.playingUnits = playingUnits;
    }

    public GameResultStatus() {
    }



}
