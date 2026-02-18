package com.iutms.camera.server;

import com.iutms.camera.remote.CameraService;
import com.iutms.camera.remote.CameraServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class CameraRMIServer {

    private static final Logger log = LoggerFactory.getLogger(CameraRMIServer.class);

    public static void main(String[] args) throws Exception {
        // Set hostname for Docker
        String hostname = System.getenv("RMI_HOSTNAME");
        if (hostname != null) {
            System.setProperty("java.rmi.server.hostname", hostname);
        }

        LocateRegistry.createRegistry(1099);
        CameraService service = new CameraServiceImpl();
        Naming.rebind("rmi://0.0.0.0:1099/CameraService", service);

        log.info("╔══════════════════════════════════════════════╗");
        log.info("║  ServiceCamera (Java RMI) is running         ║");
        log.info("║  Registry: rmi://localhost:1099/CameraService ║");
        log.info("╚══════════════════════════════════════════════╝");

        // Auto-simulator
        Thread sim = new Thread(() -> {
            String[] cameras = {"cam-01", "cam-02", "cam-03", "cam-04"};
            while (true) {
                try {
                    for (String cam : cameras) {
                        ((CameraServiceImpl) service).getCameraStatus(cam);
                    }
                    Thread.sleep(5000);
                } catch (Exception e) {
                    log.error("Simulator error: {}", e.getMessage());
                }
            }
        }, "camera-simulator");
        sim.setDaemon(true);
        sim.start();
    }
}
