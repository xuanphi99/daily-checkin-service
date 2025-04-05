package com.wiinvent.lotus.checkin.util;

public enum CacheKeys {
    USER_CHECK_IN("user:check_in"),
    USER_PROFILE("user:profile");
    private final String key;
    private static final String USER_KEY_PREFIX = "wiinvent";

    CacheKeys(String key) {
        this.key = key;
    }
    public String buildKey(long id) {
        return String.format("%s:%s:%d", USER_KEY_PREFIX, key, id);
    }
}
