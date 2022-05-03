package kz.spt.app.model.dto;

import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import kz.spt.app.service.BarrierService;
import kz.spt.lib.model.Barrier;
import kz.spt.lib.model.Camera;
import kz.spt.lib.model.Gate;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;

import java.net.UnknownHostException;
import java.util.List;

@Log
public class GateStatusDto {

    public enum GateStatus {Open,Closed};
    public enum SensorStatus {
        Quit, //событий нет, пока тишина
        Triggerred, //машина начинает проезжать
        WAIT, //ждем пока машина не подойдет
        ON,//машина проезжает
        PASSED//машина проеахала
    };
    public enum DirectionStatus {QUIT, FORWARD, REVERSE};

    public Long gateId;
    public String gateName;
    public Gate.GateType gateType;
    public Long parkingId;
    public Boolean isSimpleWhitelist;
    public Boolean notControlBarrier = false;
    public BarrierStatusDto barrier;
    public SensorStatusDto loop;
    public SensorStatusDto photoElement;
    public CameraStatusDto frontCamera;
    public CameraStatusDto frontCamera2;
    public CameraStatusDto backCamera;

    public GateStatus gateStatus = GateStatus.Closed;
    public SensorStatus cameraStatus = SensorStatus.Quit;
    public SensorStatus loopStatus = SensorStatus.Quit;
    public SensorStatus photoElementStatus = SensorStatus.Quit;
    public DirectionStatus directionStatus = DirectionStatus.QUIT;
    public SensorStatus sensor1 = cameraStatus;
    public SensorStatus sensor2 = loopStatus;

    public Long lastTriggeredTime = null;
    public Long lastClosedTime = null;

    public void sensorsReverse(){
        sensor1 = loopStatus;
        sensor2 = photoElementStatus;
    }

    public void sensorsForward(){
        sensor1 = cameraStatus;
        sensor2 = loopStatus;
    }

    public static GateStatusDto fromGate(Gate gate, List<Gate> allGates, BarrierService barrierService) throws UnknownHostException, ModbusIOException {
        GateStatusDto gateStatusDto = new GateStatusDto();
        gateStatusDto.gateId = gate.getId();
        gateStatusDto.gateName = gate.getName();
        gateStatusDto.parkingId = gate.getParking().getId();
        gateStatusDto.gateType = gate.getGateType();
        gateStatusDto.notControlBarrier = gate.getNotControlBarrier();

        Barrier barrier = gate.getBarrier();
        if (barrier != null) {
            if (Barrier.SensorsType.AUTOMATIC.equals(barrier.getSensorsType()) || (Barrier.SensorsType.MANUAL.equals(barrier.getSensorsType()) && barrier.getIp() != null && barrier.getPassword() != null && barrier.getOpenOid() != null && barrier.getCloseOid() != null)) {
                gateStatusDto.barrier = BarrierStatusDto.fromBarrier(barrier);
            }
            if(Barrier.BarrierType.MODBUS.equals(barrier.getBarrierType()) && barrier.getIp()!= null && barrier.getIp().contains(".")){
                try {
                    barrierService.addGlobalModbusMaster(barrier);
                } catch (Exception e){
                    log.info(" Modbus connection error: " + e.getMessage());
                }
            }

            if(Barrier.BarrierType.SNMP.equals(barrier.getBarrierType()) && !StringUtils.isEmpty(barrier.getLoopIp()) && !StringUtils.isEmpty(barrier.getLoopPassword()) && barrier.getLoopOid() != null){
                gateStatusDto.loop = new SensorStatusDto();
                gateStatusDto.loop.barrierId = barrier.getId();
                gateStatusDto.loop.barrierIp = barrier.getIp();
                gateStatusDto.loop.sensorName = "loop";
                gateStatusDto.loop.ip = barrier.getLoopIp();
                gateStatusDto.loop.password = barrier.getLoopPassword();
                gateStatusDto.loop.oid = barrier.getLoopOid();
                gateStatusDto.loop.snmpVersion = barrier.getLoopSnmpVersion();
                gateStatusDto.loop.defaultValue = barrier.getLoopDefaultValue();
                gateStatusDto.loop.gateNotControlBarrier = gate.getNotControlBarrier() != null ? gate.getNotControlBarrier() : false;
                gateStatusDto.loop.type = barrier.getBarrierType();
            } else if(Barrier.BarrierType.MODBUS.equals(barrier.getBarrierType()) && barrier.getLoopModbusRegister() != null){
                gateStatusDto.loop = new SensorStatusDto();
                gateStatusDto.loop.barrierId = barrier.getId();
                gateStatusDto.loop.barrierIp = barrier.getIp();
                gateStatusDto.loop.sensorName = "loop";
                gateStatusDto.loop.ip = barrier.getLoopIp();
                gateStatusDto.loop.modbusRegister = barrier.getLoopModbusRegister();
                gateStatusDto.loop.modbusDeviceVersion = barrier.getModbusDeviceVersion();
                gateStatusDto.loop.gateNotControlBarrier = gate.getNotControlBarrier() != null ? gate.getNotControlBarrier() : false;
                gateStatusDto.loop.type = barrier.getBarrierType();
            } else if(Barrier.BarrierType.JETSON.equals(barrier.getBarrierType()) && barrier.getLoopJetsonPin() != null) {
                gateStatusDto.loop = new SensorStatusDto();
                gateStatusDto.loop.barrierId = barrier.getId();
                gateStatusDto.loop.sensorName = "loop";
                gateStatusDto.loop.type = barrier.getBarrierType();
                gateStatusDto.loop.ip = barrier.getIp();
                gateStatusDto.loop.oid = barrier.getLoopJetsonPin().toString();
                gateStatusDto.loop.gateNotControlBarrier = gate.getNotControlBarrier() != null ? gate.getNotControlBarrier() : false;
            }

            if(Barrier.BarrierType.SNMP.equals(barrier.getBarrierType()) && !StringUtils.isEmpty(barrier.getPhotoElementIp()) && !StringUtils.isEmpty(barrier.getPhotoElementPassword()) && barrier.getPhotoElementOid() != null){
                gateStatusDto.photoElement = new SensorStatusDto();
                gateStatusDto.photoElement.barrierId = barrier.getId();
                gateStatusDto.photoElement.barrierIp = barrier.getIp();
                gateStatusDto.photoElement.sensorName = "photoElement";
                gateStatusDto.photoElement.ip = barrier.getPhotoElementIp();
                gateStatusDto.photoElement.password = barrier.getPhotoElementPassword();
                gateStatusDto.photoElement.oid = barrier.getPhotoElementOid();
                gateStatusDto.photoElement.snmpVersion = barrier.getPhotoElementSnmpVersion();
                gateStatusDto.loop.defaultValue = barrier.getPhotoElementDefaultValue();
                gateStatusDto.photoElement.gateNotControlBarrier = gate.getNotControlBarrier() != null ? gate.getNotControlBarrier() : false;
                gateStatusDto.photoElement.type = barrier.getBarrierType();
            } else if(Barrier.BarrierType.MODBUS.equals(barrier.getBarrierType()) && barrier.getPhotoElementModbusRegister() != null){
                gateStatusDto.photoElement = new SensorStatusDto();
                gateStatusDto.photoElement.barrierId = barrier.getId();
                gateStatusDto.photoElement.barrierIp = barrier.getIp();
                gateStatusDto.photoElement.sensorName = "photoElement";
                gateStatusDto.photoElement.modbusRegister = barrier.getPhotoElementModbusRegister();
                gateStatusDto.photoElement.modbusDeviceVersion = barrier.getModbusDeviceVersion();
                gateStatusDto.photoElement.gateNotControlBarrier = gate.getNotControlBarrier() != null ? gate.getNotControlBarrier() : false;
                gateStatusDto.photoElement.type = barrier.getBarrierType();
            } else if(Barrier.BarrierType.JETSON.equals(barrier.getBarrierType()) && barrier.getPhotoElementJetsonPin() != null) {
                gateStatusDto.photoElement = new SensorStatusDto();
                gateStatusDto.photoElement.barrierId = barrier.getId();
                gateStatusDto.photoElement.sensorName = "photoElement";
                gateStatusDto.photoElement.type = barrier.getBarrierType();
                gateStatusDto.photoElement.ip = barrier.getIp();
                gateStatusDto.photoElement.oid = barrier.getPhotoElementJetsonPin().toString();
                gateStatusDto.photoElement.gateNotControlBarrier = gate.getNotControlBarrier() != null ? gate.getNotControlBarrier() : false;
            }
        }

        List<Camera> cameraList = gate.getCameraList();
        if (cameraList.size() > 0) {
            for (Camera camera : cameraList) {
                if (Camera.CameraType.FRONT.equals(camera.getCameraType())) {
                    if (gateStatusDto.frontCamera == null) {
                        gateStatusDto.frontCamera = new CameraStatusDto();
                        gateStatusDto.frontCamera.id = camera.getId();
                        gateStatusDto.frontCamera.ip = camera.getIp();
                        gateStatusDto.frontCamera.timeout = camera.getTimeout() != null ? camera.getTimeout(): 1;
                        gateStatusDto.frontCamera.enabled = camera.isEnabled();
                        gateStatusDto.frontCamera.gateId = gate.getId();
                    } else {
                        gateStatusDto.frontCamera2 = new CameraStatusDto();
                        gateStatusDto.frontCamera2.id = camera.getId();
                        gateStatusDto.frontCamera2.ip = camera.getIp();
                        gateStatusDto.frontCamera2.timeout = camera.getTimeout() != null ? camera.getTimeout(): 1;
                        gateStatusDto.frontCamera2.enabled = camera.isEnabled();
                        gateStatusDto.frontCamera2.gateId = gate.getId();
                    }
                }
                if (Camera.CameraType.BACK.equals(camera.getCameraType())) {
                    gateStatusDto.backCamera = new CameraStatusDto();
                    gateStatusDto.backCamera.id = camera.getId();
                    gateStatusDto.backCamera.timeout = camera.getTimeout() != null ? camera.getTimeout(): 1;
                    gateStatusDto.backCamera.enabled = camera.isEnabled();
                    gateStatusDto.backCamera.gateId = gate.getId();
                }
            }
        }
        gateStatusDto.isSimpleWhitelist = (Gate.GateType.REVERSE.equals(gate.getGateType()) && gateStatusDto.backCamera == null) || (Gate.GateType.OUT.equals(gate.getGateType()) && gateStatusDto.frontCamera == null);
        if(Gate.GateType.IN.equals(gate.getGateType()) && !gateStatusDto.isSimpleWhitelist){
            Gate outGate = null;
            for(Gate checkOutGate : allGates){
                if(gate.getParking().getId() == checkOutGate.getParking().getId() && Gate.GateType.OUT.equals(checkOutGate.getGateType())){
                    outGate = checkOutGate;
                }
            }
            if(outGate == null){
                gateStatusDto.isSimpleWhitelist = true;
            }
        }

        return gateStatusDto;
    }
}
