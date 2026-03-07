package com.iutms.flux.service;

import com.iutms.flux.model.VehicleFlowData;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import java.util.List;

/**
 * JAX-WS SOAP Web Service interface for vehicle flow data.
 */
@WebService
public interface FluxVehiculesService {

    @WebMethod
    VehicleFlowData getVehicleCount(
            @WebParam(name = "roadId") String roadId);

    @WebMethod
    double getFlowRate(
            @WebParam(name = "roadId") String roadId);

    @WebMethod
    List<VehicleFlowData> getAllFlowData(
            @WebParam(name = "zoneId") String zoneId);
}
