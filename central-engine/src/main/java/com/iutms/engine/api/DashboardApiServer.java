package com.iutms.engine.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iutms.common.discovery.SensorRegistrationRequest;
import com.iutms.common.discovery.ServiceEndpointDescriptor;
import com.iutms.common.util.JsonUtil;
import com.iutms.engine.dao.TrafficDAO;
import com.iutms.engine.model.Alert;
import com.iutms.engine.model.Recommendation;
import com.iutms.engine.registry.SensorRegistry;
import com.iutms.engine.registry.ZoneRegistry;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight REST API built on JDK HttpServer.
 * Provides endpoints for the web dashboard and sensor discovery.
 */
public class DashboardApiServer {

    private static final Logger log = LoggerFactory.getLogger(DashboardApiServer.class);

    private final TrafficDAO     dao;
    private final SensorRegistry sensorRegistry;
    private final ZoneRegistry   zoneRegistry;
    private HttpServer server;

    public DashboardApiServer(TrafficDAO dao,
                              SensorRegistry sensorRegistry,
                              ZoneRegistry zoneRegistry) {
        this.dao            = dao;
        this.sensorRegistry = sensorRegistry;
        this.zoneRegistry   = zoneRegistry;
    }

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // ── Existing dashboard endpoints ────────────────────────────────
        server.createContext("/api/alerts",          withCors(this::handleAlerts));
        server.createContext("/api/recommendations", withCors(this::handleRecommendations));
        server.createContext("/api/dashboard",       withCors(this::handleDashboard));
        server.createContext("/api/geography",       withCors(this::handleGeography));
        server.createContext("/api/health",          withCors(this::handleHealth));
        server.createContext("/api/zones",           withCors(this::handleZoneSummary));

        // Alert acknowledge (POST /api/alerts/{id}/acknowledge)
        server.createContext("/api/alerts/", exchange -> {
            addCorsHeaders(exchange, "GET, POST, OPTIONS");
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            handleAcknowledgeAlert(exchange);
        });

        // ── Sensor discovery endpoints (/api/sensors/**) ─────────────────
        server.createContext("/api/sensors", withFullCors(this::handleSensors));

        // ── Zone config CRUD endpoints (/api/zones/config/**) ────────────
        // Registered after /api/zones so JDK picks the longer prefix for config paths
        server.createContext("/api/zones/config", withFullCors(this::handleZonesConfig));

        server.setExecutor(null);
        server.start();
        log.info("Dashboard API server started on port {}", port);
    }

    // ── CORS helpers ─────────────────────────────────────────────────────

    private HttpHandler withCors(HttpHandler handler) {
        return exchange -> {
            addCorsHeaders(exchange, "GET, OPTIONS");
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            handler.handle(exchange);
        };
    }

    private HttpHandler withFullCors(HttpHandler handler) {
        return exchange -> {
            addCorsHeaders(exchange, "GET, POST, PUT, PATCH, DELETE, OPTIONS");
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            handler.handle(exchange);
        };
    }

    private void addCorsHeaders(HttpExchange exchange, String methods) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", methods);
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().add("Content-Type", "application/json");
    }

    // ── Existing handlers ────────────────────────────────────────────────

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
        dashboard.put("traffic", dao.getLatestTrafficByRoad());
        dashboard.put("pollution", dao.getLatestPollutionByZone());
        dashboard.put("signals", dao.getLatestSignalStates());
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

    private void handleZoneSummary(HttpExchange exchange) throws IOException {
        sendJson(exchange, dao.getZoneSummary());
    }

    private void handleGeography(HttpExchange exchange) throws IOException {
        List<ZoneRegistry.ZoneInfo> zones = zoneRegistry.getAllZones();
        List<String> zoneIds = zones.stream()
                .map(ZoneRegistry.ZoneInfo::id)
                .collect(java.util.stream.Collectors.toList());
        Map<String, Object> geo = new java.util.LinkedHashMap<>();
        geo.put("zones", zoneIds);
        geo.put("zoneDetails", zones);
        sendJson(exchange, geo);
    }

    private void handleAcknowledgeAlert(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (!"POST".equals(exchange.getRequestMethod()) || !path.endsWith("/acknowledge")) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        String[] parts = path.split("/");
        long alertId;
        try {
            alertId = Long.parseLong(parts[3]);
        } catch (NumberFormatException e) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }
        dao.acknowledgeAlert(alertId);
        sendJson(exchange, Map.of("status", "acknowledged", "id", alertId));
    }

    // ── Sensor discovery handlers ─────────────────────────────────────────

    private void handleSensors(HttpExchange exchange) throws IOException {
        String path   = exchange.getRequestURI().getPath(); // /api/sensors[/...]
        String method = exchange.getRequestMethod();

        // POST /api/sensors/register
        if ("POST".equals(method) && path.endsWith("/register")) {
            handleSensorRegister(exchange);
            return;
        }

        // POST /api/sensors/{id}/heartbeat
        if ("POST".equals(method) && path.endsWith("/heartbeat")) {
            String sensorId = extractSegment(path, "/api/sensors/", "/heartbeat");
            sensorRegistry.heartbeat(sensorId);
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        // DELETE /api/sensors/{id}
        if ("DELETE".equals(method) && !path.equals("/api/sensors")) {
            String sensorId = path.substring("/api/sensors/".length());
            sensorRegistry.deregister(sensorId);
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        // GET /api/sensors/{id}
        if ("GET".equals(method) && !path.equals("/api/sensors")) {
            String sensorId = path.substring("/api/sensors/".length());
            Map<String, Object> sensor = sensorRegistry.getSensorById(sensorId);
            if (sensor == null) { exchange.sendResponseHeaders(404, -1); return; }
            sendJson(exchange, sensor);
            return;
        }

        // GET /api/sensors  — list all
        if ("GET".equals(method)) {
            sendJson(exchange, sensorRegistry.getAllSensors());
            return;
        }

        exchange.sendResponseHeaders(404, -1);
    }

    private void handleSensorRegister(HttpExchange exchange) throws IOException {
        try {
            String body = readBody(exchange);
            SensorRegistrationRequest req =
                    JsonUtil.fromJson(body, SensorRegistrationRequest.class);
            if (req.getSensorId() == null || req.getSensorType() == null || req.getZoneId() == null) {
                sendJsonStatus(exchange, 400,
                        Map.of("error", "sensorId, sensorType, and zoneId are required"));
                return;
            }
            ServiceEndpointDescriptor descriptor = sensorRegistry.register(req);
            sendJsonStatus(exchange, 200, descriptor);
        } catch (IllegalArgumentException e) {
            sendJsonStatus(exchange, 422, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Sensor registration error: {}", e.getMessage());
            sendJsonStatus(exchange, 500, Map.of("error", "internal error"));
        }
    }

    // ── Zone config handlers ───────────────────────────────────────────────

    private void handleZonesConfig(HttpExchange exchange) throws IOException {
        String path   = exchange.getRequestURI().getPath(); // /api/zones/config[/...]
        String method = exchange.getRequestMethod();

        // GET /api/zones/config/{id}/sensors  — merged zone+sensors response
        if ("GET".equals(method) && path.contains("/api/zones/config/") && path.endsWith("/sensors")) {
            String zoneId = extractSegment(path, "/api/zones/config/", "/sensors");
            handleZoneWithSensors(exchange, zoneId);
            return;
        }

        // GET /api/zones/config  — list all
        if ("GET".equals(method) && path.equals("/api/zones/config")) {
            sendJson(exchange, dao.getZoneConfigs());
            return;
        }

        // POST /api/zones/config  — create
        if ("POST".equals(method) && path.equals("/api/zones/config")) {
            handleCreateZone(exchange);
            return;
        }

        // Remaining paths: /api/zones/config/{id}
        if (!path.equals("/api/zones/config")) {
            String zoneId = path.substring("/api/zones/config/".length());

            if ("GET".equals(method)) {
                Map<String, Object> zone = dao.getZoneConfig(zoneId);
                if (zone == null) { exchange.sendResponseHeaders(404, -1); return; }
                sendJson(exchange, zone);
                return;
            }

            if ("PATCH".equals(method)) {
                handlePatchZone(exchange, zoneId);
                return;
            }

            if ("DELETE".equals(method)) {
                zoneRegistry.deactivateZone(zoneId);
                exchange.sendResponseHeaders(204, -1);
                return;
            }
        }

        exchange.sendResponseHeaders(404, -1);
    }

    private void handleZoneWithSensors(HttpExchange exchange, String zoneId) throws IOException {
        Map<String, Object> zone = dao.getZoneConfig(zoneId);
        if (zone == null) { exchange.sendResponseHeaders(404, -1); return; }
        Map<String, Object> response = new LinkedHashMap<>(zone);
        response.put("sensors", sensorRegistry.getSensorsByZone(zoneId));
        sendJson(exchange, response);
    }

    private void handleCreateZone(HttpExchange exchange) throws IOException {
        try {
            JsonObject json = JsonParser.parseString(readBody(exchange)).getAsJsonObject();
            String id   = jsonStr(json, "id");
            String name = jsonStr(json, "name");
            if (id == null || name == null
                    || !json.has("lat") || !json.has("lng") || !json.has("radius_m")) {
                sendJsonStatus(exchange, 400,
                        Map.of("error", "id, name, lat, lng, radius_m are required"));
                return;
            }
            double lat      = json.get("lat").getAsDouble();
            double lng      = json.get("lng").getAsDouble();
            int    radiusM  = json.get("radius_m").getAsInt();
            int    partIdx  = json.has("partition_idx") ? json.get("partition_idx").getAsInt() : -1;
            zoneRegistry.createZone(id, name, lat, lng, radiusM, partIdx);
            sendJsonStatus(exchange, 201, Map.of("id", id, "status", "created"));
        } catch (RuntimeException e) {
            sendJsonStatus(exchange, 422, Map.of("error", e.getMessage()));
        }
    }

    private void handlePatchZone(HttpExchange exchange, String zoneId) throws IOException {
        try {
            JsonObject json    = JsonParser.parseString(readBody(exchange)).getAsJsonObject();
            Map<String, Object> updates = new LinkedHashMap<>();
            if (json.has("lat"))           updates.put("latitude",      json.get("lat").getAsDouble());
            if (json.has("lng"))           updates.put("longitude",     json.get("lng").getAsDouble());
            if (json.has("radius_m"))      updates.put("radius_m",      json.get("radius_m").getAsInt());
            if (json.has("partition_idx")) updates.put("partition_idx", json.get("partition_idx").getAsInt());
            if (json.has("name"))          updates.put("name",          json.get("name").getAsString());
            zoneRegistry.updateZone(zoneId, updates);
            sendJson(exchange, Map.of("id", zoneId, "status", "updated"));
        } catch (RuntimeException e) {
            sendJsonStatus(exchange, 422, Map.of("error", e.getMessage()));
        }
    }

    // ── Send helpers ──────────────────────────────────────────────────────

    private void sendJson(HttpExchange exchange, Object data) throws IOException {
        String json  = JsonUtil.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private void sendJsonStatus(HttpExchange exchange, int status, Object data) throws IOException {
        String json  = JsonUtil.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    // ── Parse helpers ─────────────────────────────────────────────────────

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Extract the segment between {@code prefix} and {@code suffix} in a path. */
    private String extractSegment(String path, String prefix, String suffix) {
        int start = prefix.length();
        int end   = path.lastIndexOf(suffix);
        return path.substring(start, end);
    }

    private String jsonStr(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? json.get(key).getAsString() : null;
    }
}
