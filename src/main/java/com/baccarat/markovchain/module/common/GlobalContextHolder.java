package com.baccarat.markovchain.module.common;

public class GlobalContextHolder {
    private static final ThreadLocal<String> userUuidHolder = new ThreadLocal<>();

    public static void setUserUuid(String uuid) {
        userUuidHolder.set(uuid);
    }

    public static String getUserUuid() {
        return userUuidHolder.get();
    }
}
