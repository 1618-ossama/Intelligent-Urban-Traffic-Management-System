package com.iutms.bruit.protocol;

import com.iutms.bruit.model.NoiseData;
import com.iutms.common.util.JsonUtil;
import java.util.regex.Pattern;

/**
 * Multi-format protocol for noise sensor data.
 * Supports both pipe-delimited and JSON formats.
 * 
 * Pipe-delimited (Java): NOISE|zoneId|decibelLevel|timestamp
 * Example: NOISE|zone-center|85.3|2026-02-17T10:30:00
 * 
 * JSON (Python): {"zoneId":"zone-center","decibelLevel":85.3,"timestamp":"2026-02-17T10:30:00"}
 */
public class NoiseProtocol {

    public static final String DELIMITER = "|";
    public static final String PREFIX = "NOISE";

    public static String encode(NoiseData data) {
        return PREFIX + DELIMITER + data.getZoneId() + DELIMITER +
               data.getDecibelLevel() + DELIMITER + data.getTimestamp();
    }

    public static NoiseData decode(String message) {
        message = message.trim();
        if (message.startsWith("{")) {
            try {
                NoiseData data = JsonUtil.fromJson(message, NoiseData.class);
                if (data.getZoneId() != null && data.getDecibelLevel() > 0
                        && data.getTimestamp() != null && !data.getTimestamp().isEmpty()) {
                    return data;
                }
                throw new IllegalArgumentException(
                    "JSON noise message missing required field: timestamp. " +
                    "Expected: {\"zoneId\":\"...\",\"decibelLevel\":...,\"timestamp\":\"...\"}"
                );
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid JSON noise message: " + message);
            }
        }
        String[] parts = message.split(Pattern.quote(DELIMITER));
        if (parts.length != 4 || !parts[0].equals(PREFIX)) {
            throw new IllegalArgumentException(
                "Invalid noise message. Expected format: NOISE|zoneId|decibelLevel|timestamp " +
                "or JSON: {\"zoneId\":\"...\",\"decibelLevel\":...,\"timestamp\":\"...\"}"
            );
        }
        return new NoiseData(parts[1], Double.parseDouble(parts[2]), parts[3]);
    }
}
