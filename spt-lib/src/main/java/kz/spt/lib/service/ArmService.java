package kz.spt.lib.service;

import kz.spt.lib.model.Camera;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.text.ParseException;

public interface ArmService {

    Boolean openGate(Long cameraId) throws IOException, ParseException, InterruptedException;

    Boolean closeGate(Long cameraId) throws IOException, ParseException, InterruptedException;

    Boolean setEmergencyOpen(Boolean value, UserDetails currentUser);

    Boolean getEmergencyStatus();

    Boolean passCar(Long cameraId, String platenumber, String snapshot) throws Exception;

    String snapshot(String ip, String login, String password, String url) throws Throwable;
    void enableSnapshot(Long cameraId) throws Throwable;
    void disableSnapshot() throws Throwable;
}
