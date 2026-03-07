package com.iutms.bruit.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Java equivalent of the Python noise data client simulator.
 * Sends noise measurements to the Noise TCP Server on localhost:9090.
 * 
 * Usage:
 *   // Single measurement
 *   NoiseDataSimulator.sendNoiseData("zone-west", 75.5);
 *   
 *   // Continuous monitoring
 *   NoiseDataSimulator.startContinuousMonitoring("zone-center", 3);
 */
public class NoiseDataSimulator {

    private static final Logger log = LoggerFactory.getLogger(NoiseDataSimulator.class);
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9090;
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    private static final Random random = new Random();

    /**
     * Send a single noise data measurement to the server.
     *
     * @param zoneId         Identifier for the zone (e.g., "zone-center", "zone-north")
     * @param decibelLevel   Noise level in decibels (dB)
     * @return Server response (ACK or ERR), or null if error occurred
     */
    public static String sendNoiseData(String zoneId, double decibelLevel) {
        return sendNoiseData(zoneId, decibelLevel, Instant.now().toString());
    }

    /**
     * Send noise data with custom timestamp.
     *
     * @param zoneId         Zone identifier
     * @param decibelLevel   Noise level in decibels (dB)
     * @param timestamp      ISO format timestamp
     * @return Server response
     */
    public static String sendNoiseData(String zoneId, double decibelLevel, String timestamp) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Create message map
            Map<String, Object> message = new HashMap<>();
            message.put("zoneId", zoneId);
            message.put("decibelLevel", decibelLevel);
            message.put("timestamp", timestamp);

            // Serialize to JSON
            String jsonStr = gson.toJson(message);

            // Send data
            out.println(jsonStr);

            // Receive response
            String response = in.readLine();
            return response != null ? response.trim() : null;

        } catch (Exception e) {
            log.error("Error sending noise data: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Continuously send noise data for a zone at specified interval.
     *
     * @param zoneId  Zone identifier
     * @param intervalSeconds  Seconds between measurements
     */
    public static void startContinuousMonitoring(String zoneId, int intervalSeconds) {
        log.info("Starting continuous monitoring for {}", zoneId);

        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            log.info("Connected to Noise Server on {}:{}", SERVER_HOST, SERVER_PORT);

            int iteration = 0;
            while (true) {
                // Generate realistic noise levels (40-100 dB for urban areas)
                // Mean ~70 dB, standard deviation ~10
                double decibelLevel = 40 + random.nextGaussian() * 10 + 30;
                decibelLevel = Math.max(40, Math.min(120, decibelLevel)); // Clamp to 40-120
                decibelLevel = Math.round(decibelLevel * 10.0) / 10.0; // Round to 1 decimal

                String timestamp = Instant.now().toString();

                // Create message
                Map<String, Object> message = new HashMap<>();
                message.put("zoneId", zoneId);
                message.put("decibelLevel", decibelLevel);
                message.put("timestamp", timestamp);

                String jsonStr = gson.toJson(message);

                // Send data
                out.println(jsonStr);

                // Receive response
                String response = in.readLine();
                response = response != null ? response.trim() : "NO RESPONSE";

                iteration++;
                log.info("[{}] Sent: {} @ {} dB | Response: {}", iteration, zoneId, decibelLevel, response);

                // Wait for next interval
                Thread.sleep(intervalSeconds * 1000L);
            }

        } catch (InterruptedException e) {
            log.info("Monitoring stopped for {}", zoneId);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Connection error: {}", e.getMessage());
        }
    }

    /**
     * Run example scenarios demonstrating the simulator.
     */
    public static void main(String[] args) throws InterruptedException {
        log.info("╔═════════════════════════════════════════════════╗");
        log.info("║   Noise Data Simulator - Java Edition          ║");
        log.info("║   Equivalent to Python client_example.py       ║");
        log.info("╚═════════════════════════════════════════════════╝");
        log.info("");

        // Example 1: Send a single measurement
        log.info("=== Example 1: Single measurement ===");
        String response = sendNoiseData("zone-west", 75.5);
        log.info("Response: {}\n", response);

        Thread.sleep(1000);

        // Example 2: Send multiple measurements
        log.info("=== Example 2: Multiple measurements ===");
        String[] zones = {"zone-center", "zone-north", "zone-south"};
        for (String zone : zones) {
            double db = Math.round((45 + random.nextDouble() * 60) * 10.0) / 10.0;
            response = sendNoiseData(zone, db);
            log.info("{}: {}", zone, response);
            Thread.sleep(500);
        }

        log.info("");

        // Example 3: Continuous monitoring (uncomment to enable)
        log.info("=== Example 3: Continuous monitoring ===");
        log.info("Uncomment the line below to enable continuous monitoring:");
        log.info("// startContinuousMonitoring(\"zone-center\", 3);");
        
        // Uncomment the line below to run continuous monitoring
        // startContinuousMonitoring("zone-center", 3);
    }
}
