package kz.spt.lib.service;

import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.text.ParseException;

public interface ArmService {

    Boolean openGate(Long cameraId) throws IOException, ParseException, InterruptedException;

    Boolean closeGate(Long cameraId) throws IOException, ParseException, InterruptedException;

    Boolean setEmergencyOpen(Boolean value, UserDetails currentUser);

    Boolean getEmergencyStatus();

    Boolean passCar(Long cameraId, String platenumber) throws Exception;
}
