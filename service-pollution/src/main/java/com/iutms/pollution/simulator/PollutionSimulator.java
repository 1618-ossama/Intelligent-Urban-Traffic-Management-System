package com.iutms.pollution.simulator;

import com.iutms.pollution.model.PollutionData;
import com.iutms.common.util.TimeUtil;

import java.util.*;

public class PollutionSimulator {

    private final Random random = new Random();
    private static final String[] ZONES = {"zone-center", "zone-north", "zone-south", "zone-east"};

    public PollutionData generate(String zoneId) {
        double co2 = 300 + random.nextDouble() * 200;   // 300-500 ppm
        double nox = 10 + random.nextDouble() * 90;      // 10-100 ppb
        double pm25 = 5 + random.nextDouble() * 45;      // 5-50 µg/m³
        return new PollutionData(zoneId,
                Math.round(co2 * 10.0) / 10.0,
                Math.round(nox * 10.0) / 10.0,
                Math.round(pm25 * 10.0) / 10.0,
                TimeUtil.now());
    }

    public Map<String, PollutionData> getAllZonesData() {
        Map<String, PollutionData> map = new LinkedHashMap<>();
        for (String zone : ZONES) {
            map.put(zone, generate(zone));
        }
        return map;
    }

    public List<PollutionData> getHistory(String zoneId, int hours) {
        List<PollutionData> history = new ArrayList<>();
        for (int i = 0; i < hours * 12; i++) { // one record per 5 min
            history.add(generate(zoneId));
        }
        return history;
    }
}
