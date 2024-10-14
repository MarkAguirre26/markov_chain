package com.baccarat.markovchain.module.common.concrete;

import lombok.Getter;

@Getter
public enum RiskLevel {


    LOW("LOW"),
    VERY_LOW("VERY_LOW");

    private final String value;

    RiskLevel(String value) {
        this.value = value;
    }

}
