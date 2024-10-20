package com.baccarat.markovchain.module.common.concrete;

import lombok.Getter;

@Getter
public enum UserConfig {

    DAILY_LIMIT("DAILY_LIMIT"),
    FREEZE("FREEZE"),
    VIRTUAL_WIN("VIRTUAL_WIN");
//    BASE_BET("BASE_BET");

    private final String value;

    UserConfig(String value) {
        this.value = value;
    }

}
