package kz.spt.app.thread;

import kz.spt.app.job.StatusCheckJob;
import kz.spt.app.model.dto.BarrierStatusDto;
import kz.spt.app.model.dto.CameraStatusDto;
import kz.spt.app.model.dto.GateStatusDto;
import kz.spt.app.model.dto.SensorStatusDto;
import kz.spt.app.service.BarrierService;
import kz.spt.lib.model.Gate;
import kz.spt.lib.service.CarEventService;
import lombok.extern.java.Log;

import java.io.IOException;
import java.text.ParseException;

@Log
public class GateStatusCheckThread extends Thread {

    private GateStatusDto gateStatusDto;
    private BarrierService barrierService;

    public GateStatusCheckThread(GateStatusDto gateStatusDto, BarrierService barrierService){
        this.gateStatusDto = gateStatusDto;
        this.barrierService = barrierService;
    }

    public void run() {
        log.info("Thread for - " + gateStatusDto.gateId);

        BarrierStatusDto barrier = gateStatusDto.barrier;
        CameraStatusDto cameraStatusDto = gateStatusDto.frontCamera;
        if(barrier != null && cameraStatusDto != null){ // Данные шлагбаума и камеры заполнены
            SensorStatusDto photoElement = gateStatusDto.photoElement;
            SensorStatusDto loop = gateStatusDto.loop;
            if(photoElement != null && loop != null){ // Данные фотоэлемента и петли заполнены
                if(triggerPassed() && getSensorStatus(photoElement) == 0 && getSensorStatus(loop) == 0 && gateStatusDto.gateStatus == GateStatusDto.GateStatus.Open){ // Закрыть шлагбаум если сенсоры пусты и шлагбаум открыть
                    gateStatusDto.photoElementStatus = GateStatusDto.SensorStatus.Quit;
                    gateStatusDto.loopStatus = GateStatusDto.SensorStatus.Quit;
                    gateStatusDto.directionStatus = GateStatusDto.DirectionStatus.QUIT;
                    gateStatusDto.sensorsForward();
                    try {
                        boolean result = barrierService.openBarrier(gateStatusDto.gateType, null, gateStatusDto.barrier);
                        if(result){
                            gateStatusDto.gateStatus  = GateStatusDto.GateStatus.Closed;
                        }
                    } catch (IOException | ParseException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if(GateStatusDto.SensorStatus.Quit.equals(gateStatusDto.sensor1) && GateStatusDto.SensorStatus.Quit.equals(gateStatusDto.sensor2)
                        && GateStatusDto.DirectionStatus.QUIT.equals(gateStatusDto.directionStatus) && GateStatusDto.SensorStatus.Quit.equals(gateStatusDto.photoElementStatus)
                        && Gate.GateType.REVERSE.equals(gateStatusDto.gateType)){
                    if(getSensorStatus(loop) == 1){
                        boolean result = openGateForCarOut(gateStatusDto.backCamera, gateStatusDto, gateStatusDto.isSimpleWhitelist);
                        if(result){
                            gateStatusDto.lastTriggeredTime = System.currentTimeMillis();
                            gateStatusDto.sensorsReverse();
                            gateStatusDto.sensor1 = GateStatusDto.SensorStatus.Triggerred;
                            gateStatusDto.sensor2 = GateStatusDto.SensorStatus.WAIT;
                            gateStatusDto.directionStatus = GateStatusDto.DirectionStatus.REVERSE;
                            log.info("Gate opened with ip magnetic loop: " + gateStatusDto.gateId);
                        }
                    }
                    log.info("WAITING REVERSE MODE or TRIGGER FROM CVT");
                }

                if(GateStatusDto.DirectionStatus.FORWARD.equals(gateStatusDto.directionStatus)){
                    log.info("DIRECTION FORWARD");
                    if(GateStatusDto.SensorStatus.Triggerred.equals(gateStatusDto.sensor1) && GateStatusDto.SensorStatus.WAIT.equals(gateStatusDto.sensor2)){
                        if(getSensorStatus(gateStatusDto.loop) == 1){
                            log.info("CAR TRIGGERED CVT, ML TRIGGERED");
                            gateStatusDto.sensor1 = GateStatusDto.SensorStatus.PASSED;
                            gateStatusDto.sensor2 = GateStatusDto.SensorStatus.ON;
                            gateStatusDto.lastTriggeredTime = System.currentTimeMillis();
                        } else if(getSensorStatus(gateStatusDto.loop) == 0){
                            log.info("CAR TRIGGERED CVT, ML WAITING");
                        }
                    } else if(GateStatusDto.SensorStatus.PASSED.equals(gateStatusDto.sensor1) && GateStatusDto.SensorStatus.ON.equals(gateStatusDto.sensor2)){
                        if(getSensorStatus(gateStatusDto.loop) == 1){
                            log.info("CAR PASSED CVT, CAR ON ML");
                            gateStatusDto.sensor1 = GateStatusDto.SensorStatus.Quit;
                            gateStatusDto.sensor2 = GateStatusDto.SensorStatus.ON;
                            gateStatusDto.lastTriggeredTime = System.currentTimeMillis();
                        }
                    } else if(GateStatusDto.SensorStatus.Quit.equals(gateStatusDto.sensor1) && GateStatusDto.SensorStatus.ON.equals(gateStatusDto.sensor2)){
                        if(getSensorStatus(gateStatusDto.loop) == 1){
                            log.info("CVT IN QUIET MODE, CAR ON ML");
                            gateStatusDto.lastTriggeredTime = System.currentTimeMillis();
                        } else if(getSensorStatus(gateStatusDto.loop) == 0){
                            log.info("CVT IN QUIET MODE, CAR PASSED ML");
                            gateStatusDto.sensor1 = GateStatusDto.SensorStatus.Quit;
                            gateStatusDto.sensor2 = GateStatusDto.SensorStatus.PASSED;
                        }
                    } else if(GateStatusDto.SensorStatus.Quit.equals(gateStatusDto.sensor1) && GateStatusDto.SensorStatus.PASSED.equals(gateStatusDto.sensor2)){
                        if(getSensorStatus(gateStatusDto.loop) == 1) {
                            gateStatusDto.lastTriggeredTime = System.currentTimeMillis();
                        } else if(getSensorStatus(gateStatusDto.loop) == 0){
                            log.info("CVT IN QUIET MODE, CAR FULLY PASSED ML AND ML NOT ACTIVATED");
                            gateStatusDto.sensor2 = GateStatusDto.SensorStatus.Quit;
                            try {
                                boolean result = barrierService.closeBarrier(gateStatusDto.gateType, null, gateStatusDto.barrier);
                                if(result){
                                    gateStatusDto.gateStatus  = GateStatusDto.GateStatus.Closed;
                                    gateStatusDto.directionStatus = GateStatusDto.DirectionStatus.QUIT;
                                }
                            } catch (IOException | ParseException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else if(GateStatusDto.DirectionStatus.REVERSE.equals(gateStatusDto.directionStatus)){
                    if(GateStatusDto.SensorStatus.Triggerred.equals(gateStatusDto.sensor1) && GateStatusDto.SensorStatus.WAIT.equals(gateStatusDto.sensor2)){
                        if(getSensorStatus(gateStatusDto.photoElement) == 1) {
                            gateStatusDto.sensor2 = GateStatusDto.SensorStatus.ON;
                            gateStatusDto.lastTriggeredTime = System.currentTimeMillis();
                        }
                    } else if(GateStatusDto.SensorStatus.Triggerred.equals(gateStatusDto.sensor1) && GateStatusDto.SensorStatus.ON.equals(gateStatusDto.sensor2)){
                        if(getSensorStatus(gateStatusDto.photoElement) == 0 && getSensorStatus(gateStatusDto.loop) == 0){
                            gateStatusDto.sensor2 = GateStatusDto.SensorStatus.PASSED;
                            gateStatusDto.sensor1 = GateStatusDto.SensorStatus.Quit;
                        } else if(getSensorStatus(gateStatusDto.photoElement) == 1 || getSensorStatus(gateStatusDto.loop) == 1){
                            gateStatusDto.lastTriggeredTime = System.currentTimeMillis();
                        }
                    } else if(GateStatusDto.SensorStatus.Quit.equals(gateStatusDto.sensor1) && GateStatusDto.SensorStatus.PASSED.equals(gateStatusDto.sensor2)) {
                        if(getSensorStatus(gateStatusDto.photoElement) == 0 && getSensorStatus(gateStatusDto.loop) == 0){
                            log.info("We close the gate: " + gateStatusDto.gateId);
                            try {
                                boolean result = barrierService.closeBarrier(gateStatusDto.gateType, null, gateStatusDto.barrier);
                                if(result){
                                    gateStatusDto.gateStatus  = GateStatusDto.GateStatus.Closed;
                                    gateStatusDto.directionStatus = GateStatusDto.DirectionStatus.QUIT;
                                    gateStatusDto.sensorsForward();
                                    gateStatusDto.lastClosedTime = System.currentTimeMillis();
                                }
                            } catch (IOException | ParseException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
        StatusCheckJob.isGatesProcessing.put(gateStatusDto.gateId, false);
    }

    private boolean triggerPassed(){
        if(gateStatusDto.lastTriggeredTime == null){
            return false;
        }
        return System.currentTimeMillis() - gateStatusDto.lastTriggeredTime > 10000;  // больше 10ти секунд
    }

    private int getSensorStatus(SensorStatusDto sensor){
        int status = -1;
        try {
            status = barrierService.getSensorStatus(sensor);
        } catch (IOException | ParseException e) { e.printStackTrace(); }
        return status;
    }

    private boolean openGateForCarOut(CameraStatusDto camera, GateStatusDto gate, Boolean isSimple) {
        boolean result = false;
        if(isSimple){
            try {
                result = barrierService.openBarrier(gate.gateType, null, gate.barrier);
            } catch (IOException | ParseException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
