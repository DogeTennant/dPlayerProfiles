package com.dogetennant.dplayerprofiles.lang;

public record Placeholder(String key, String value) {

    public static Placeholder of(String key, Object value) {
        return new Placeholder(key, String.valueOf(value));
    }
}
