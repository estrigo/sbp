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

    Boolean openGate(Long cameraId, String reason) throws Exception;

    JsonNode openPermanentGate(Long cameraId) throws ModbusProtocolException, ModbusNumberException, IOException, ParseException, InterruptedException, ModbusIOException;

    Boolean closeGate(Long cameraId) throws IOException, ParseException, InterruptedException, ModbusProtocolException, ModbusNumberException, ModbusIOException;

    JsonNode closePermanentGate(Long cameraId) throws IOException, ParseException, InterruptedException, ModbusProtocolException, ModbusNumberException, ModbusIOException;

    void manualEnter(Long cameraId, String plateNumber);

    Boolean restartParkomat(String ip);

    Boolean setEmergencyOpen(Boolean value, UserDetails currentUser) throws ModbusProtocolException, ModbusNumberException, IOException, ParseException, InterruptedException, ModbusIOException;

    Boolean getEmergencyStatus();

    Boolean passCar(Long cameraId, String platenumber) throws Exception;

    byte[] snapshot(Long cameraId) throws Throwable;

    Future<byte[]> getSnapshot(String ip, String login, String password, String url) throws Throwable;

    JsonNode getTabsWithCameraList();

    Boolean configureArm(String json) throws JsonProcessingException;

    JsonNode getCameraList();

    JsonNode getBarrierOpenCameraIds();
}
