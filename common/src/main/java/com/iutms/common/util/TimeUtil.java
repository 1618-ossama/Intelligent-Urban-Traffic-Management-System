package com.iutms.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimeUtil {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }

    public static LocalDateTime parse(String timestamp) {
        return LocalDateTime.parse(timestamp, FORMATTER);
    }
}
