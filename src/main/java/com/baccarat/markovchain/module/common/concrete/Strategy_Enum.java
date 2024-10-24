package com.baccarat.markovchain.module.common.concrete;

import lombok.Getter;

@Getter
public enum Strategy_Enum {

   
    WINLOCK("WINLOCK"),
    TREND_OF_TWO("TREND_OF_TWO"),
    TREND_OF_THREE("TREND_OF_THREE"),
    FREEHAND("FREEHAND"),
    ONE_THREE_TWO_SIX("ONE_THREE_TWO_SIX"),
    STRATEGY("STRATEGY");
   
//    BASE_BET("BASE_BET");

    private final String value;

    Strategy_Enum(String value) {
        this.value = value;
    }

}
