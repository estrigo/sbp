package kz.spt.app.thread;

import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import kz.spt.app.job.StatusCheckJob;
import kz.spt.app.model.dto.GateStatusDto;
import kz.spt.lib.model.Gate;
import kz.spt.lib.model.dto.CameraStatusDto;
import kz.spt.lib.model.dto.EmergencySignalConfigDto;
import kz.spt.app.model.dto.SensorStatusDto;
import kz.spt.app.service.BarrierService;
import kz.spt.lib.model.Barrier;
import kz.spt.lib.service.ArmService;
import lombok.extern.java.Log;

import java.io.IOException;
import java.text.ParseException;

@Log
public class EmergencyStatusCheckThread extends Thread {

    private static int SENSOR_ON = 0;
    private static int SENSOR_OFF = 1;
    private static int SENSOR_UNDEFINED = -1;

    private EmergencySignalConfigDto emergencySignalConfigDto;

    private BarrierService barrierService;

    private ArmService armService;
    public EmergencyStatusCheckThread(EmergencySignalConfigDto emergencySignalConfigDto, BarrierService barrierService, ArmService armService){
        this.emergencySignalConfigDto = emergencySignalConfigDto;
        this.barrierService = barrierService;
        this.armService = armService;
    }

    public void run() {
        try {
            getSensorStatus();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private int getSensorStatus() throws ModbusProtocolException, ModbusNumberException, ModbusIOException, InterruptedException {
        SensorStatusDto sensorStatusDto = new SensorStatusDto();
        sensorStatusDto.ip = emergencySignalConfigDto.ip;
        sensorStatusDto.defaultValue = emergencySignalConfigDto.defaultValue;
        sensorStatusDto.modbusRegister = emergencySignalConfigDto.modbusRegister;
        sensorStatusDto.modbusDeviceVersion = "icpdas";
        sensorStatusDto.type = Barrier.BarrierType.MODBUS;

        int status = SENSOR_UNDEFINED;
        try {
            status = barrierService.getEmergencySensorStatus(sensorStatusDto);
            if(status == StatusCheckJob.emergencySignalConfigDto.defaultValue && !StatusCheckJob.emergencyModeOn){
                for (GateStatusDto gateStatusDto : StatusCheckJob.globalGateDtos) {
                    CameraStatusDto cameraStatusDto = gateStatusDto.frontCamera;
                    armService.openPermanentGate(cameraStatusDto.id);
                }
                StatusCheckJob.emergencyModeOn = true;
            } else if(status != StatusCheckJob.emergencySignalConfigDto.defaultValue && StatusCheckJob.emergencyModeOn){
                for (GateStatusDto gateStatusDto : StatusCheckJob.globalGateDtos) {
                    CameraStatusDto cameraStatusDto = gateStatusDto.frontCamera;
                    armService.closePermanentGate(cameraStatusDto.id);
                }
                StatusCheckJob.emergencyModeOn = false;
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return status;
    }
}
