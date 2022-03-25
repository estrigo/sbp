package kz.spt.app.model.dto;

import kz.spt.lib.model.Barrier;
import kz.spt.lib.model.Camera;
import kz.spt.lib.model.Gate;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class GateStatusDto {

    public enum GateStatus {Open,Closed};
    public enum SensorStatus {Quit, Triggerred, WAIT, ON, PASSED};
    public enum DirectionStatus {QUIT, FORWARD, REVERSE};

    public Long gateId;
    public String gateName;
    public Gate.GateType gateType;
    public Long parkingId;
    public Boolean isSimpleWhitelist;
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

    public static GateStatusDto fromGate(Gate gate){
        GateStatusDto gateStatusDto = new GateStatusDto();
        gateStatusDto.gateId = gate.getId();
        gateStatusDto.gateName = gate.getName();
        gateStatusDto.parkingId = gate.getParking().getId();
        gateStatusDto.gateType = gate.getGateType();

        Barrier barrier = gate.getBarrier();
        if (barrier != null) {
            if (Barrier.SensorsType.AUTOMATIC.equals(barrier.getSensorsType()) || (Barrier.SensorsType.MANUAL.equals(barrier.getSensorsType()) && barrier.getIp() != null && barrier.getPassword() != null && barrier.getOpenOid() != null && barrier.getCloseOid() != null)) {
                gateStatusDto.barrier = BarrierStatusDto.fromBarrier(barrier);
            }

            if(!StringUtils.isEmpty(barrier.getLoopIp()) && !StringUtils.isEmpty(barrier.getLoopPassword()) && barrier.getLoopOid() != null && barrier.getLoopType() != null){
                gateStatusDto.loop = new SensorStatusDto();
                gateStatusDto.loop.barrierId = barrier.getId();
                gateStatusDto.loop.sensorName = "loop";
                gateStatusDto.loop.type = barrier.getLoopType();
                gateStatusDto.loop.ip = barrier.getLoopIp();
                gateStatusDto.loop.password = barrier.getLoopPassword();
                gateStatusDto.loop.oid = barrier.getLoopOid();
                gateStatusDto.loop.snmpVersion = barrier.getLoopSnmpVersion();
                gateStatusDto.loop.defaultValue = barrier.getLoopDefaultValue();
                gateStatusDto.loop.modbusRegister = barrier.getLoopModbusRegister();
                gateStatusDto.loop.modbusDeviceVersion = barrier.getModbusDeviceVersion();
            }else if(Barrier.BarrierType.JETSON.equals(barrier.getBarrierType()) && barrier.getLoopJetsonPin() != null) {
                gateStatusDto.loop = new SensorStatusDto();
                gateStatusDto.loop.barrierId = barrier.getId();
                gateStatusDto.loop.sensorName = "loop";
                gateStatusDto.loop.type = barrier.getBarrierType();
                gateStatusDto.loop.ip = barrier.getIp();
                gateStatusDto.loop.oid = barrier.getLoopJetsonPin().toString();
            }

            if(!StringUtils.isEmpty(barrier.getPhotoElementIp()) && !StringUtils.isEmpty(barrier.getPhotoElementPassword()) && barrier.getPhotoElementOid() != null && barrier.getPhotoElementType() != null){
                gateStatusDto.photoElement = new SensorStatusDto();
                gateStatusDto.photoElement.barrierId = barrier.getId();
                gateStatusDto.photoElement.sensorName = "photoElement";
                gateStatusDto.photoElement.type = barrier.getPhotoElementType();
                gateStatusDto.photoElement.ip = barrier.getPhotoElementIp();
                gateStatusDto.photoElement.password = barrier.getPhotoElementPassword();
                gateStatusDto.photoElement.oid = barrier.getPhotoElementOid();
                gateStatusDto.photoElement.snmpVersion = barrier.getPhotoElementSnmpVersion();
                gateStatusDto.loop.defaultValue = barrier.getPhotoElementDefaultValue();
                gateStatusDto.photoElement.modbusRegister = barrier.getPhotoElementModbusRegister();
                gateStatusDto.photoElement.modbusDeviceVersion = barrier.getModbusDeviceVersion();
            }else if(Barrier.BarrierType.JETSON.equals(barrier.getBarrierType()) && barrier.getPhotoElementJetsonPin() != null) {
                gateStatusDto.photoElement = new SensorStatusDto();
                gateStatusDto.photoElement.barrierId = barrier.getId();
                gateStatusDto.photoElement.sensorName = "photoElement";
                gateStatusDto.photoElement.type = barrier.getBarrierType();
                gateStatusDto.photoElement.ip = barrier.getIp();
                gateStatusDto.photoElement.oid = barrier.getPhotoElementJetsonPin().toString();
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
                    } else {
                        gateStatusDto.frontCamera2 = new CameraStatusDto();
                        gateStatusDto.frontCamera2.id = camera.getId();
                        gateStatusDto.frontCamera2.ip = camera.getIp();
                    }
                }
                if (Camera.CameraType.BACK.equals(camera.getCameraType())) {
                    gateStatusDto.backCamera = new CameraStatusDto();
                    gateStatusDto.backCamera.id = camera.getId();
                }
            }
        }
        gateStatusDto.isSimpleWhitelist = (Gate.GateType.REVERSE.equals(gate.getGateType()) && gateStatusDto.backCamera == null) || (Gate.GateType.OUT.equals(gate.getGateType()) && gateStatusDto.frontCamera == null);

        return gateStatusDto;
    }
}
