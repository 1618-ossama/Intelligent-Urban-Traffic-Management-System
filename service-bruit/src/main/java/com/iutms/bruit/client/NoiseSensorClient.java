package com.iutms.bruit.client;

import com.iutms.bruit.model.NoiseData;
import com.iutms.bruit.protocol.NoiseProtocol;
import com.iutms.common.util.TimeUtil;

import java.io.*;
import java.net.Socket;
import java.util.Random;

/**
 * Standalone noise sensor client for external testing.
 * Usage: java NoiseSensorClient [host] [port]
 */
public class NoiseSensorClient {

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9090;

        Socket socket = new Socket(host, port);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));

        System.out.println("Connected to noise server at " + host + ":" + port);
        Random rand = new Random();

        while (true) {
            double dB = 40 + rand.nextDouble() * 80;
            NoiseData data = new NoiseData("zone-center",
                    Math.round(dB * 10.0) / 10.0, TimeUtil.now());
            String msg = NoiseProtocol.encode(data);
            out.println(msg);
            String ack = in.readLine();
            System.out.println("Sent: " + msg + " | Ack: " + ack);
            Thread.sleep(2000);
        }
    }
}
