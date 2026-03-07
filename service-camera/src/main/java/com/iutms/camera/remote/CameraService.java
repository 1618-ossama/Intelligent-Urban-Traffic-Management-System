package com.iutms.camera.remote;

import com.iutms.camera.model.CameraEvent;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Java RMI Remote Interface for the Camera surveillance service.
 */
public interface CameraService extends Remote {

    CameraEvent getCameraStatus(String cameraId) throws RemoteException;

    boolean detectIncident(String cameraId) throws RemoteException;

    List<CameraEvent> getZoneEvents(String zoneId) throws RemoteException;
}
