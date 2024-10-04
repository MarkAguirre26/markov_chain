package com.baccarat.markovchain.module.common.concrete;

import lombok.Getter;

@Getter
public enum UserRole {


    ADMIN("ADMIN"),
    USER("USER");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

}
