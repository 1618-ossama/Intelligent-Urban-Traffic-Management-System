package com.iutms.bruit.server;

import com.iutms.bruit.kafka.NoiseKafkaProducer;
import com.iutms.bruit.model.NoiseData;
import com.iutms.bruit.protocol.NoiseProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

/**
 * Handles a single TCP client connection (one noise sensor).
 * Reads protocol messages, decodes them, and forwards to Kafka.
 */
public class ClientHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);
    private final Socket socket;
    private final NoiseKafkaProducer kafka;

    public ClientHandler(Socket socket, NoiseKafkaProducer kafka) {
        this.socket = socket;
        this.kafka = kafka;
    }

    @Override
    public void run() {
        String clientAddr = socket.getInetAddress().getHostAddress();
        log.info("Noise sensor connected: {}", clientAddr);

        try (BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(
                     socket.getOutputStream(), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                try {
                    NoiseData data = NoiseProtocol.decode(line);
                    kafka.send(data);
                    out.println("ACK");
                    log.debug("Received from {}: {}", clientAddr, data);
                } catch (IllegalArgumentException e) {
                    out.println("ERR: " + e.getMessage());
                    log.warn("Invalid message from {}: {}", clientAddr, line);
                }
            }
        } catch (IOException e) {
            log.info("Sensor disconnected: {} ({})", clientAddr, e.getMessage());
        }
    }
}
