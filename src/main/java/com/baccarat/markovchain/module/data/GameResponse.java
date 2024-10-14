package com.baccarat.markovchain.module.data;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "game_response")
public class GameResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_response_id")
    private int gameResponseId;

    @Column(name = "useruuid", length = 254)
    private String userUuid;

    @Column(name = "base_bet_unit", columnDefinition = "int default 1")
    private int baseBetUnit = 1;

    @Column(name = "suggested_bet_unit", columnDefinition = "int default 1")
    private int suggestedBetUnit = 1;

    @Column(name = "initial_Playing_units", columnDefinition = "int default 100")
    private int initialPlayingUnits = 100;

    @Column(name = "loss_counter", columnDefinition = "int default 0")
    private int lossCounter = 0;


    @Column(name = "recommended_bet", length = 10)
    private String recommendedBet;

    @Column(name = "sequence", length = 50)
    private String sequence;

    @Column(name = "hand_result", length = 50)
    private String handResult;

    @Column(name = "message", length = 50)
    private String message;

    @Column(name = "risk_level", length = 50)
    private String riskLevel;

    @Column(name = "date_last_updated")
    private LocalDateTime dateLastUpdated;
}
