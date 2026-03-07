package com.iutms.pollution.resource;

import com.iutms.pollution.kafka.PollutionKafkaProducer;
import com.iutms.pollution.model.PollutionData;
import com.iutms.pollution.simulator.PollutionSimulator;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * JAX-RS REST resource for pollution sensor data.
 * Endpoints: GET /pollution/{zoneId}, GET /pollution/{zoneId}/history, GET /pollution/zones
 */
@Path("/pollution")
@Produces(MediaType.APPLICATION_JSON)
public class PollutionResource {

    private static final Logger log = LoggerFactory.getLogger(PollutionResource.class);
    private final PollutionSimulator simulator = new PollutionSimulator();
    private final PollutionKafkaProducer kafka = new PollutionKafkaProducer();

    @GET
    @Path("/{zoneId}")
    public Response getPollution(@PathParam("zoneId") String zoneId) {
        PollutionData data = simulator.generate(zoneId);
        kafka.send(data);
        log.info("REST GET /pollution/{}: {}", zoneId, data);
        return Response.ok(data).build();
    }

    @GET
    @Path("/{zoneId}/history")
    public Response getHistory(@PathParam("zoneId") String zoneId,
                               @QueryParam("hours") @DefaultValue("1") int hours) {
        List<PollutionData> history = simulator.getHistory(zoneId, hours);
        history.forEach(kafka::send);
        log.info("REST GET /pollution/{}/history: {} records", zoneId, history.size());
        return Response.ok(history).build();
    }

    @GET
    @Path("/zones")
    public Response getAllZones() {
        Map<String, PollutionData> data = simulator.getAllZonesData();
        data.values().forEach(kafka::send);
        log.info("REST GET /pollution/zones: {} zones", data.size());
        return Response.ok(data).build();
    }
}
