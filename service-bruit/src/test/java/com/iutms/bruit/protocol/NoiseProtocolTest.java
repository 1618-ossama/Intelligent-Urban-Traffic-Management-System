package com.iutms.bruit.protocol;

import com.iutms.bruit.model.NoiseData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NoiseProtocolTest {

    private static final String TIMESTAMP = "2026-02-17T10:30:00";

    // ── Pipe-delimited ────────────────────────────────────────────────────

    @Test
    void decode_validPipeDelimited_returnsNoiseData() {
        NoiseData data = NoiseProtocol.decode("NOISE|zone-center|85.3|" + TIMESTAMP);

        assertEquals("zone-center", data.getZoneId());
        assertEquals(85.3, data.getDecibelLevel(), 1e-9);
        assertEquals(TIMESTAMP, data.getTimestamp());
    }

    @Test
    void decode_wrongPrefix_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> NoiseProtocol.decode("WRONG|zone-center|85.3|" + TIMESTAMP));
    }

    @Test
    void decode_tooFewPipeFields_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> NoiseProtocol.decode("NOISE|zone-center|85.3"));
    }

    @Test
    void decode_wrongDelimiter_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> NoiseProtocol.decode("NOISE,zone-center,85.3," + TIMESTAMP));
    }

    @Test
    void decode_nonNumericDecibelInPipe_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class,
                () -> NoiseProtocol.decode("NOISE|zone-center|notADouble|" + TIMESTAMP));
    }

    // ── JSON ─────────────────────────────────────────────────────────────

    @Test
    void decode_validJson_returnsNoiseData() {
        String json = "{\"zoneId\":\"zone-north\",\"decibelLevel\":90.0,\"timestamp\":\"" + TIMESTAMP + "\"}";
        NoiseData data = NoiseProtocol.decode(json);

        assertEquals("zone-north", data.getZoneId());
        assertEquals(90.0, data.getDecibelLevel(), 1e-9);
        assertEquals(TIMESTAMP, data.getTimestamp());
    }

    @Test
    void decode_jsonMissingZoneId_throwsIllegalArgument() {
        String json = "{\"decibelLevel\":85.0,\"timestamp\":\"" + TIMESTAMP + "\"}";
        assertThrows(IllegalArgumentException.class, () -> NoiseProtocol.decode(json));
    }

    @Test
    void decode_jsonMissingDecibelLevel_throwsIllegalArgument() {
        // decibelLevel=0.0 fails the > 0 guard (Gson default for absent numeric field)
        String json = "{\"zoneId\":\"zone-center\",\"decibelLevel\":0.0,\"timestamp\":\"" + TIMESTAMP + "\"}";
        assertThrows(IllegalArgumentException.class, () -> NoiseProtocol.decode(json));
    }

    @Test
    void decode_jsonMissingTimestamp_throwsIllegalArgument() {
        String json = "{\"zoneId\":\"zone-center\",\"decibelLevel\":85.0}";
        assertThrows(IllegalArgumentException.class, () -> NoiseProtocol.decode(json));
    }

    @Test
    void decode_jsonEmptyTimestamp_throwsIllegalArgument() {
        String json = "{\"zoneId\":\"zone-center\",\"decibelLevel\":85.0,\"timestamp\":\"\"}";
        assertThrows(IllegalArgumentException.class, () -> NoiseProtocol.decode(json));
    }

    @Test
    void decode_malformedJson_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> NoiseProtocol.decode("{not valid json"));
    }
}
