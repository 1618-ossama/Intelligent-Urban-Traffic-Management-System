package com.iutms.bruit.server;

import com.iutms.bruit.kafka.NoiseKafkaProducer;
import com.iutms.bruit.model.NoiseData;
import com.iutms.bruit.protocol.NoiseProtocol;
import com.iutms.common.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

/**
 * TCP Server for noise sensor data.
 * Accepts connections from noise sensor clients and bridges data to Kafka.
 * Also starts an embedded simulator client for demo purposes.
 */
public class NoiseTCPServer {

    private static final Logger log = LoggerFactory.getLogger(NoiseTCPServer.class);
    private static final int PORT = 9090;

    public static void main(String[] args) throws IOException {
        NoiseKafkaProducer kafka = new NoiseKafkaProducer();

        // Start the simulator client in a separate thread
        Thread simThread = new Thread(() -> startSimulatorClient(), "noise-sim-client");
        simThread.setDaemon(true);

        ServerSocket serverSocket = new ServerSocket(PORT);
        log.info("╔══════════════════════════════════════════════╗");
        log.info("║  ServiceBruit (TCP Socket) is running         ║");
        log.info("║  Listening on port {}                         ║", PORT);
        log.info("║  Protocol: NOISE|zoneId|dB|timestamp          ║");
        log.info("╚══════════════════════════════════════════════╝");

        // Start simulator after small delay so server is ready
        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            simThread.start();
        }).start();

        // Accept client connections
        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(new ClientHandler(clientSocket, kafka),
                    "handler-" + clientSocket.getInetAddress()).start();
        }
    }

    /**
     * Embedded simulator client that connects to this server
     * and sends noise data every 3 seconds.
     */
    private static void startSimulatorClient() {
        Random random = new Random();
        String[] zones = {"zone-center", "zone-north", "zone-south", "zone-east"};

        try {
            Thread.sleep(1000); // Wait for server to start
            Socket socket = new Socket("localhost", PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            log.info("Noise simulator client connected to localhost:{}", PORT);

            while (true) {
                for (String zone : zones) {
                    double dB = 40 + random.nextDouble() * 80; // 40-120 dB
                    NoiseData data = new NoiseData(zone,
                            Math.round(dB * 10.0) / 10.0, TimeUtil.now());
                    String msg = NoiseProtocol.encode(data);
                    out.println(msg);
                    String ack = in.readLine();
                    log.debug("Sent: {} | Response: {}", msg, ack);
                }
                Thread.sleep(3000);
            }
        } catch (Exception e) {
            log.error("Simulator client error: {}", e.getMessage());
        }
    }
}
