package com.baccarat.markovchain.module.data.response;

import com.baccarat.markovchain.module.data.GameStatus;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GameResultResponse {


    private String message;
    private String sequence;
    private GameResultStatus gameStatus;
    private int baseBetUnit;
    private int suggestedBetUnit;
    private int initialPlayingUnits;
    private String recommendedBet;
    private boolean isDailyLimitReached;
    private int lossCounter;
    private String trailingStop;
    private String handResult;
    private String riskLevel;

    public GameResultResponse(String message, String sequence, GameResultStatus gameStatus, int baseBetUnit,
                              int suggestedBetUnit, int initialPlayingUnits, String recommendedBet,
                              boolean isDailyLimitReached, int lossCounter) {
        this.message = message;
        this.sequence = sequence;
        this.gameStatus = gameStatus;
        this.baseBetUnit = baseBetUnit;
        this.suggestedBetUnit = suggestedBetUnit;
        this.initialPlayingUnits = initialPlayingUnits;
        this.recommendedBet = recommendedBet;
        this.isDailyLimitReached = isDailyLimitReached;
        this.lossCounter = lossCounter;

    }

    public GameResultResponse(String message, String sequence, GameResultStatus gameStatus, int baseBetUnit,
                              int suggestedBetUnit, int initialPlayingUnits, String recommendedBet,int lossCounter) {
        this.message = message;
        this.sequence = sequence;
        this.gameStatus = gameStatus;
        this.baseBetUnit = baseBetUnit;
        this.suggestedBetUnit = suggestedBetUnit;
        this.initialPlayingUnits = initialPlayingUnits;
        this.recommendedBet = recommendedBet;
        this.lossCounter = lossCounter;

    }

    public GameResultResponse() {
    }

    @Override
    public String toString() {
        return "GameResultResponse{" +
                "message='" + message + '\'' +
                ", sequence='" + sequence + '\'' +
                ", gameStatus=" + gameStatus +
                ", baseBetUnit=" + baseBetUnit +
                ", suggestedBetUnit=" + suggestedBetUnit +
                ", initialPlayingUnits=" + initialPlayingUnits +
                ", recommendedBet='" + recommendedBet + '\'' +
                ", isDailyLimitReached=" + isDailyLimitReached +
                ", lossCounter=" + lossCounter +
                ", trailingStop='" + trailingStop + '\'' +
                ", handResult='" + handResult + '\'' +
                '}';
    }
}
