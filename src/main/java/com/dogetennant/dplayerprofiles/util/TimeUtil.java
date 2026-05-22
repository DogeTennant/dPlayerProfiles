package com.dogetennant.dplayerprofiles.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TimeUtil {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    private TimeUtil() {}

    public static String format(long seconds) {
        if (seconds <= 0) return "0s";

        long days    = seconds / 86400;
        long hours   = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs    = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0)    sb.append(days).append("d ");
        if (hours > 0)   sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (secs > 0 || sb.isEmpty()) sb.append(secs).append("s");

        return sb.toString().trim();
    }

    public static String formatDate(long epochMs) {
        return DATE_FMT.format(Instant.ofEpochMilli(epochMs));
    }

    public static String today() {
        return DATE_FMT.format(Instant.now());
    }

    public static boolean isYesterday(String isoDate) {
        java.time.LocalDate date = java.time.LocalDate.parse(isoDate);
        return date.equals(java.time.LocalDate.now(ZoneId.systemDefault()).minusDays(1));
    }

    public static boolean isToday(String isoDate) {
        java.time.LocalDate date = java.time.LocalDate.parse(isoDate);
        return date.equals(java.time.LocalDate.now(ZoneId.systemDefault()));
    }
}
