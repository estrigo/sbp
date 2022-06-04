package kz.spt.lib.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import kz.spt.lib.model.Camera;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.text.ParseException;
import java.util.concurrent.Future;

public interface ArmService {

    Boolean openGate(Long cameraId) throws IOException, ParseException, InterruptedException, ModbusProtocolException, ModbusNumberException, ModbusIOException;

    Boolean openGate(Long cameraId, String snapshot) throws Exception;

    Boolean closeGate(Long cameraId) throws IOException, ParseException, InterruptedException, ModbusProtocolException, ModbusNumberException, ModbusIOException;

    Boolean restartParkomat(String ip);

    Boolean setEmergencyOpen(Boolean value, UserDetails currentUser);

    Boolean getEmergencyStatus();

    Boolean passCar(Long cameraId, String platenumber, String snapshot) throws Exception;

    byte[] snapshot(Long cameraId) throws Throwable;

    Future<byte[]> getSnapshot(String ip, String login, String password, String url) throws Throwable;

    JsonNode getTabsWithCameraList();

    Boolean configureArm(String json) throws JsonProcessingException;
}
