package com.iutms.flux.simulator;

import com.iutms.common.config.GeographyConfig;
import com.iutms.flux.model.VehicleFlowData;
import com.iutms.common.util.TimeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Simulates vehicle flow sensor data.
 * In a real system, this would read from physical sensors.
 * Geography (roads) is loaded dynamically from GeographyConfig.
 */
public class FluxSimulator {

    private final Random random = new Random();
    private final List<String> roads;

    public FluxSimulator() {
        this.roads = GeographyConfig.getInstance().getRoads();
    }

    public VehicleFlowData generateFlowData(String roadId, String zoneId) {
        int vehicleCount = 20 + random.nextInt(150);  // 20-170 vehicles
        double flowRate = vehicleCount * (0.5 + random.nextDouble()); // veh/min
        return new VehicleFlowData(roadId, zoneId, vehicleCount,
                Math.round(flowRate * 10.0) / 10.0, TimeUtil.now());
    }

    public List<VehicleFlowData> generateZoneData(String zoneId) {
        List<VehicleFlowData> list = new ArrayList<>();
        for (String road : roads) {
            list.add(generateFlowData(road, zoneId));
        }
        return list;
    }
}
