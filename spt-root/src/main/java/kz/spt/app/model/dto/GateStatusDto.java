package kz.spt.app.model.dto;

import kz.spt.lib.model.Barrier;
import kz.spt.lib.model.Camera;
import kz.spt.lib.model.Gate;
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
        if(barrier != null){
            if(Barrier.SensorsType.AUTOMATIC.equals(barrier.getSensorsType()) || (Barrier.SensorsType.MANUAL.equals(barrier.getSensorsType()) &&  barrier.getIp() != null && barrier.getPassword() != null && barrier.getOpenOid() != null && barrier.getCloseOid() != null)){
                gateStatusDto.barrier = BarrierStatusDto.fromBarrier(barrier);
            }
            if(barrier.getLoopIp() != null && barrier.getLoopPassword() != null && barrier.getLoopOid() != null && barrier.getLoopType() != null){
                gateStatusDto.loop = new SensorStatusDto();
                gateStatusDto.loop.barrierId = barrier.getId();
                gateStatusDto.loop.ip = barrier.getLoopIp();
                gateStatusDto.loop.password = barrier.getLoopPassword();
                gateStatusDto.loop.oid = barrier.getLoopOid();
                gateStatusDto.loop.snmpVersion = barrier.getLoopSnmpVersion();
                gateStatusDto.loop.type = barrier.getLoopType();
                gateStatusDto.loop.sensorName = "loop";
            }
            if(barrier.getPhotoElementIp() != null && barrier.getPhotoElementPassword() != null && barrier.getPhotoElementOid() != null && barrier.getPhotoElementType() != null){
                gateStatusDto.photoElement = new SensorStatusDto();
                gateStatusDto.photoElement.barrierId = barrier.getId();
                gateStatusDto.photoElement.ip = barrier.getPhotoElementIp();
                gateStatusDto.photoElement.password = barrier.getPhotoElementPassword();
                gateStatusDto.photoElement.oid = barrier.getPhotoElementOid();
                gateStatusDto.photoElement.snmpVersion = barrier.getPhotoElementSnmpVersion();
                gateStatusDto.photoElement.type = barrier.getPhotoElementType();
                gateStatusDto.photoElement.sensorName = "photoElement";

            }
        }
        List<Camera> cameraList = gate.getCameraList();
        if(cameraList.size() > 0){
            for(Camera camera: cameraList){
                if(Camera.CameraType.FRONT.equals(camera.getCameraType())){
                    gateStatusDto.frontCamera = new CameraStatusDto();
                    gateStatusDto.frontCamera.id = camera.getId();
                }
                if(Camera.CameraType.BACK.equals(camera.getCameraType())){
                    gateStatusDto.backCamera = new CameraStatusDto();
                    gateStatusDto.backCamera.id = camera.getId();
                }
            }
        }
        gateStatusDto.isSimpleWhitelist = (Gate.GateType.REVERSE.equals(gate.getGateType()) && gateStatusDto.backCamera == null) || (Gate.GateType.OUT.equals(gate.getGateType()) && gateStatusDto.frontCamera == null);

        return gateStatusDto;
    }
}
