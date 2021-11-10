package kz.spt.lib.service;

import java.io.IOException;
import java.text.ParseException;

public interface ArmService {

    Boolean openGate(Long cameraId) throws IOException, ParseException, InterruptedException;

    Boolean closeGate(Long cameraId) throws IOException, ParseException, InterruptedException;
}
