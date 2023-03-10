package kz.spt.app.model.dto;

import kz.spt.app.job.StatusCheckJob;
import kz.spt.app.thread.ModbusProtocolThread;
import kz.spt.lib.model.Barrier;
import kz.spt.lib.model.Camera;
import kz.spt.lib.model.Gate;
import kz.spt.lib.model.dto.CameraStatusDto;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Log
public class GateStatusDto {

    private static Map<String, ModbusProtocolThread> modbusMasterThreadMap = new ConcurrentHashMap<>();

    public enum GateStatus {Open,Closed};
    public enum SensorStatus {
        Quit, //событий нет, пока тишина
        Triggerred, //машина начинает проезжать
        WAIT, //ждем пока машина не подьедет
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

    public SensorStatusDto fullOpenedSensor; //датчик ворота полностью открыты
    public SensorStatusDto fullClosedSensor; //датчик ворота полностью закрыты
    public Long lastLoopTriggeredTime = null;
    public Long lastPhotoTriggeredTime = null;
    public Long lastOpenCommandSentTime = null;
    public Long lastCloseCommandSentTime = null;

    public void sensorsReverse(){
        sensor1 = loopStatus;
        sensor2 = photoElementStatus;
    }

    public void sensorsForward(){
        sensor1 = cameraStatus;
        sensor2 = loopStatus;
    }

    public static GateStatusDto fromGate(Gate gate, List<Gate> allGates) {
        GateStatusDto gateStatusDto = new GateStatusDto();
        gateStatusDto.gateId = gate.getId();
        gateStatusDto.gateName = gate.getName();
        gateStatusDto.parkingId = gate.getParking().getId();
        gateStatusDto.gateType = gate.getGateType();
        gateStatusDto.notControlBarrier = gate.getNotControlBarrier();

        Barrier barrier = gate.getBarrier();
        if (barrier != null && barrier.getIp() != null) {
            if (Barrier.SensorsType.AUTOMATIC.equals(barrier.getSensorsType()) || (Barrier.SensorsType.MANUAL.equals(barrier.getSensorsType()) && barrier.getIp() != null && barrier.getPassword() != null && barrier.getOpenOid() != null && barrier.getCloseOid() != null) || (Barrier.SensorsType.MANUAL.equals(barrier.getSensorsType()) && barrier.getIp() != null && barrier.getModbusOpenRegister()!=null)) {
                gateStatusDto.barrier = BarrierStatusDto.fromBarrier(barrier);
                if(!StatusCheckJob.barrierStatusDtoMap.containsKey(barrier.getIp())){
                    StatusCheckJob.barrierStatusDtoMap.put(barrier.getIp(), gateStatusDto.barrier);
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
                gateStatusDto.loop.ip = barrier.getIp();
                gateStatusDto.loop.modbusRegister = barrier.getLoopModbusRegister();
                gateStatusDto.loop.modbusDeviceVersion = barrier.getModbusDeviceVersion();
                gateStatusDto.loop.gateNotControlBarrier = gate.getNotControlBarrier() != null ? gate.getNotControlBarrier() : false;
                gateStatusDto.loop.type = barrier.getBarrierType();
                StatusCheckJob.loopStatusDtoMap.put(barrier.getIp(), gateStatusDto.loop);
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
                gateStatusDto.photoElement.ip = barrier.getIp();
                gateStatusDto.photoElement.modbusRegister = barrier.getPhotoElementModbusRegister();
                gateStatusDto.photoElement.modbusDeviceVersion = barrier.getModbusDeviceVersion();
                gateStatusDto.photoElement.gateNotControlBarrier = gate.getNotControlBarrier() != null ? gate.getNotControlBarrier() : false;
                gateStatusDto.photoElement.type = barrier.getBarrierType();
                StatusCheckJob.pheStatusDtoMap.put(barrier.getIp(), gateStatusDto.photoElement);
            } else if(Barrier.BarrierType.JETSON.equals(barrier.getBarrierType()) && barrier.getPhotoElementJetsonPin() != null) {
                gateStatusDto.photoElement = new SensorStatusDto();
                gateStatusDto.photoElement.barrierId = barrier.getId();
                gateStatusDto.photoElement.sensorName = "photoElement";
                gateStatusDto.photoElement.type = barrier.getBarrierType();
                gateStatusDto.photoElement.ip = barrier.getIp();
                gateStatusDto.photoElement.oid = barrier.getPhotoElementJetsonPin().toString();
                gateStatusDto.photoElement.gateNotControlBarrier = gate.getNotControlBarrier() != null ? gate.getNotControlBarrier() : false;
            }

            if(barrier.isStatusCheck() && barrier.getOpenStatusOid() != null){
                gateStatusDto.fullOpenedSensor = new SensorStatusDto();
                gateStatusDto.fullOpenedSensor.barrierId = barrier.getId();
                gateStatusDto.fullOpenedSensor.barrierIp = barrier.getIp();
                gateStatusDto.fullOpenedSensor.sensorName = "fullOpenedSensor";
                gateStatusDto.fullOpenedSensor.type = barrier.getBarrierType();
                gateStatusDto.fullOpenedSensor.ip = barrier.getIp();
                gateStatusDto.fullOpenedSensor.password = barrier.getPassword();
                gateStatusDto.fullOpenedSensor.oid = barrier.getOpenStatusOid();
                gateStatusDto.fullOpenedSensor.defaultValue = barrier.getOpenStatusDefault();
                gateStatusDto.fullOpenedSensor.snmpVersion = barrier.getSnmpVersion();
            }

            if(barrier.isStatusCheck() && barrier.getCloseStatusOid() != null){
                gateStatusDto.fullClosedSensor = new SensorStatusDto();
                gateStatusDto.fullClosedSensor.barrierId = barrier.getId();
                gateStatusDto.fullClosedSensor.barrierIp = barrier.getIp();
                gateStatusDto.fullClosedSensor.sensorName = "fullClosedSensor";
                gateStatusDto.fullClosedSensor.type = barrier.getBarrierType();
                gateStatusDto.fullClosedSensor.ip = barrier.getIp();
                gateStatusDto.fullClosedSensor.password = barrier.getPassword();
                gateStatusDto.fullClosedSensor.oid = barrier.getCloseStatusOid();
                gateStatusDto.fullClosedSensor.defaultValue = barrier.getCloseStatusDefault();
                gateStatusDto.fullClosedSensor.snmpVersion = barrier.getSnmpVersion();
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
                        gateStatusDto.frontCamera.login = camera.getLogin();
                        gateStatusDto.frontCamera.password = camera.getPassword();
                        gateStatusDto.frontCamera.snapshotUrl = camera.getSnapshotUrl();
                        gateStatusDto.frontCamera.carmenIp = camera.getCarmenIp();
                        gateStatusDto.frontCamera.carmenLogin = camera.getCarmenLogin();
                        gateStatusDto.frontCamera.carmenPassword = camera.getCarmenPassword();
                        gateStatusDto.frontCamera.snapshotEnabled = camera.getSnapshotEnabled();
                        gateStatusDto.frontCamera.startTime = camera.getStartTime();
                        gateStatusDto.frontCamera.endTime = camera.getEndTime();
                        gateStatusDto.frontCamera.updatedTime = camera.getUpdatedTime();
                        gateStatusDto.frontCamera.updatedTimeBy = camera.getUpdatedTimeBy();
                    } else {
                        gateStatusDto.frontCamera2 = new CameraStatusDto();
                        gateStatusDto.frontCamera2.id = camera.getId();
                        gateStatusDto.frontCamera2.ip = camera.getIp();
                        gateStatusDto.frontCamera2.timeout = camera.getTimeout() != null ? camera.getTimeout(): 1;
                        gateStatusDto.frontCamera2.enabled = camera.isEnabled();
                        gateStatusDto.frontCamera2.gateId = gate.getId();
                        gateStatusDto.frontCamera2.login = camera.getLogin();
                        gateStatusDto.frontCamera2.password = camera.getPassword();
                        gateStatusDto.frontCamera2.snapshotUrl = camera.getSnapshotUrl();
                        gateStatusDto.frontCamera2.carmenIp = camera.getCarmenIp();
                        gateStatusDto.frontCamera2.carmenLogin = camera.getCarmenLogin();
                        gateStatusDto.frontCamera2.carmenPassword = camera.getCarmenPassword();
                        gateStatusDto.frontCamera2.snapshotEnabled = camera.getSnapshotEnabled();
                        gateStatusDto.frontCamera2.startTime = camera.getStartTime();
                        gateStatusDto.frontCamera2.endTime = camera.getEndTime();
                        gateStatusDto.frontCamera2.updatedTime = camera.getUpdatedTime();
                        gateStatusDto.frontCamera2.updatedTimeBy = camera.getUpdatedTimeBy();
                    }
                }
                if (Camera.CameraType.BACK.equals(camera.getCameraType())) {
                    gateStatusDto.backCamera = new CameraStatusDto();
                    gateStatusDto.backCamera.id = camera.getId();
                    gateStatusDto.backCamera.timeout = camera.getTimeout() != null ? camera.getTimeout(): 1;
                    gateStatusDto.backCamera.enabled = camera.isEnabled();
                    gateStatusDto.backCamera.gateId = gate.getId();
                    gateStatusDto.backCamera.login = camera.getLogin();
                    gateStatusDto.backCamera.password = camera.getPassword();
                    gateStatusDto.backCamera.snapshotUrl = camera.getSnapshotUrl();
                    gateStatusDto.backCamera.carmenIp = camera.getCarmenIp();
                    gateStatusDto.backCamera.carmenLogin = camera.getCarmenLogin();
                    gateStatusDto.backCamera.carmenPassword = camera.getCarmenPassword();
                    gateStatusDto.backCamera.snapshotEnabled = camera.getSnapshotEnabled();
                    gateStatusDto.backCamera.startTime = camera.getStartTime();
                    gateStatusDto.backCamera.endTime = camera.getEndTime();
                    gateStatusDto.backCamera.updatedTime = camera.getUpdatedTime();
                    gateStatusDto.backCamera.updatedTimeBy = camera.getUpdatedTimeBy();
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

    public static Boolean getModbusMasterOutputValue(String barrierIp, int register){
        if(!modbusMasterThreadMap.containsKey(barrierIp)){
            createNewThread(barrierIp);
            try {
                Thread.sleep(700); // Wait modbus barrier initialization
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return modbusMasterThreadMap.get(barrierIp).getOutputValue(register);
    }

    public static Boolean getEmergencyModbusMasterOutputValue(String barrierIp, int register){
        if(!modbusMasterThreadMap.containsKey(barrierIp)){
            createEmergencyNewThread(barrierIp);
            try {
                Thread.sleep(700); // Wait modbus barrier initialization
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return modbusMasterThreadMap.get(barrierIp).getOutputValue(register);
    }

    public static Boolean getModbusMasterInputValue(String barrierIp, int register){
        if(!modbusMasterThreadMap.containsKey(barrierIp)){
            createNewThread(barrierIp);
        }
        return modbusMasterThreadMap.get(barrierIp).getInputValue(register);
    }

    public static void setModbusMasterWriteValue(String barrierIp, int register, boolean value){
        if(!modbusMasterThreadMap.containsKey(barrierIp)){
            createNewThread(barrierIp);
        }
        modbusMasterThreadMap.get(barrierIp).setWriteValue(register, value);
    }

    private static void createNewThread(String barrierIp){
        ModbusProtocolThread thread = new ModbusProtocolThread(StatusCheckJob.barrierStatusDtoMap.get(barrierIp), true);
        thread.start();
        modbusMasterThreadMap.put(barrierIp, thread);
        log.info("Adding barrier: " + barrierIp  + " to modbusMasterThreadMap");
    }

    private static void createEmergencyNewThread(String barrierIp){
        BarrierStatusDto barrierStatusDto = new BarrierStatusDto();
        barrierStatusDto.ip = barrierIp;
        barrierStatusDto.type = Barrier.BarrierType.MODBUS;
        barrierStatusDto.modbusDeviceVersion = "icpdas";

        ModbusProtocolThread thread = new ModbusProtocolThread(barrierStatusDto, false);
        thread.start();
        modbusMasterThreadMap.put(barrierIp, thread);
        log.info("Adding barrier: " + barrierIp  + " to modbusMasterThreadMap");
    }

    public static void removeThread(String barrierIp){
        modbusMasterThreadMap.remove(barrierIp);
    }
}
