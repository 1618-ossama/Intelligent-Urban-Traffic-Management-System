package com.iutms.engine.api;

import com.iutms.common.util.JsonUtil;
import com.iutms.engine.dao.TrafficDAO;
import com.iutms.engine.model.Alert;
import com.iutms.engine.model.Recommendation;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight REST API built on JDK HttpServer.
 * Provides endpoints for the web dashboard to consume.
 */
public class DashboardApiServer {

    private static final Logger log = LoggerFactory.getLogger(DashboardApiServer.class);
    private final TrafficDAO dao;
    private HttpServer server;

    public DashboardApiServer(TrafficDAO dao) {
        this.dao = dao;
    }

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // CORS + JSON wrapper
        server.createContext("/api/alerts", withCors(this::handleAlerts));
        server.createContext("/api/recommendations", withCors(this::handleRecommendations));
        server.createContext("/api/dashboard", withCors(this::handleDashboard));
        server.createContext("/api/health", withCors(this::handleHealth));

        server.setExecutor(null);
        server.start();
        log.info("Dashboard API server started on port {}", port);
    }

    private HttpHandler withCors(HttpHandler handler) {
        return exchange -> {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.getResponseHeaders().add("Content-Type", "application/json");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            handler.handle(exchange);
        };
    }

    private void handleAlerts(HttpExchange exchange) throws IOException {
        List<Alert> alerts = dao.getActiveAlerts();
        sendJson(exchange, alerts);
    }

    private void handleRecommendations(HttpExchange exchange) throws IOException {
        List<Recommendation> recs = dao.getRecentRecommendations();
        sendJson(exchange, recs);
    }

    private void handleDashboard(HttpExchange exchange) throws IOException {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("alerts", dao.getActiveAlerts());
        dashboard.put("recommendations", dao.getRecentRecommendations());
        dashboard.put("status", "running");
        dashboard.put("timestamp", com.iutms.common.util.TimeUtil.now());
        sendJson(exchange, dashboard);
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "central-engine");
        health.put("timestamp", com.iutms.common.util.TimeUtil.now());
        sendJson(exchange, health);
    }

    private void sendJson(HttpExchange exchange, Object data) throws IOException {
        String json = JsonUtil.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
