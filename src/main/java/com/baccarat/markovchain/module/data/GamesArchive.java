package com.baccarat.markovchain.module.data;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "games_archive")
public class GamesArchive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_archive_id")
    private Integer gameArchiveId;

    @Column(name = "journal_id")
    private Integer journalId;

    @Column(name = "base_bet_unit", nullable = false, columnDefinition = "int default 1")
    private Integer baseBetUnit = 1;

    @Column(name = "suggested_bet_unit", nullable = false, columnDefinition = "int default 1")
    private Integer suggestedBetUnit = 1;

    @Column(name = "loss_counter", nullable = false, columnDefinition = "int default 0")
    private Integer lossCounter = 0;

    @Column(name = "recommended_bet", length = 10)
    private String recommendedBet;

    @Column(name = "sequence", length = 254, nullable = false, columnDefinition = "varchar(254) default '1111'")
    private String sequence = "1111";

    @Column(name = "hand_result", length = 254)
    private String handResult;

    @Column(name = "message", length = 50)
    private String message;


    @Column(name = "hand_count")
    private Integer handCount;

    @Column(name = "wins")
    private Integer wins;

    @Column(name = "losses")
    private Integer losses;


    @Column(name = "profit")
    private Integer profit;

    @Column(name = "playing_units")
    private Integer playingUnits;

    @Column(name = "risk_level")
    private String riskLevel;

    public GamesArchive(Integer journalId, Integer baseBetUnit, Integer suggestedBetUnit, Integer lossCounter,
                        String recommendedBet, String sequence, String handResult, String message, Integer handCount,
                        Integer wins, Integer losses, Integer profit, Integer playingUnits, String riskLevel) {
        this.journalId = journalId;
        this.baseBetUnit = baseBetUnit;
        this.suggestedBetUnit = suggestedBetUnit;
        this.lossCounter = lossCounter;
        this.recommendedBet = recommendedBet;
        this.sequence = sequence;
        this.handResult = handResult;
        this.message = message;
        this.handCount = handCount;
        this.wins = wins;
        this.losses = losses;
        this.profit = profit;
        this.playingUnits = playingUnits;
        this.riskLevel = riskLevel;
    }
}
