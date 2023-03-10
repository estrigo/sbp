package kz.spt.app.thread;

import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import kz.spt.app.job.SensorStatusCheckJob;
import kz.spt.app.job.StatusCheckJob;
import kz.spt.app.model.dto.GateStatusDto;
import kz.spt.app.model.dto.SensorStatusDto;
import kz.spt.app.model.strategy.barrier.close.AbstractCloseStrategy;
import kz.spt.app.model.strategy.barrier.close.AutoCloseStrategy;
import kz.spt.app.model.strategy.barrier.close.ForwardCloseStrategy;
import kz.spt.app.model.strategy.barrier.close.ReverseCloseStrategy;
import kz.spt.app.model.strategy.barrier.open.AbstractOpenStrategy;
import kz.spt.app.model.strategy.barrier.open.LoopOpenStrategy;
import kz.spt.app.service.BarrierService;
import kz.spt.lib.model.Gate;
import lombok.extern.java.Log;

import java.io.IOException;
import java.text.ParseException;

@Log
public class GateStatusCheckThread extends Thread {

    private static int SENSOR_ON = 0;
    private static int SENSOR_OFF = 1;
    private static int SENSOR_UNDEFINED = -1;
    private GateStatusDto gateStatusDto;
    private BarrierService barrierService;

    public GateStatusCheckThread(GateStatusDto gateStatusDto, BarrierService barrierService) {
        this.gateStatusDto = gateStatusDto;
        this.barrierService = barrierService;
    }

    public void run() {
        try {
            SensorStatusDto photoElement = gateStatusDto.photoElement;
            SensorStatusDto loop = gateStatusDto.loop;
            if (photoElement != null && loop != null) { // Данные фотоэлемента и петли заполнены
                if (triggerPassed() && getSensorStatus(photoElement) == SENSOR_OFF && getSensorStatus(loop) == SENSOR_OFF && GateStatusDto.GateStatus.Open.equals(gateStatusDto.gateStatus)) { // Закрыть шлагбаум если сенсоры пусты и шлагбаум открыть
                    try {
                        if (!gateStatusDto.barrier.statusCheck) {
                            boolean result = barrierService.closeBarrier(gateStatusDto, null, gateStatusDto.barrier);
                            if (result) {
                                gateStatusDto.gateStatus = GateStatusDto.GateStatus.Closed;
                                gateStatusDto.photoElementStatus = GateStatusDto.SensorStatus.Quit;
                                gateStatusDto.loopStatus = GateStatusDto.SensorStatus.Quit;
                                gateStatusDto.directionStatus = GateStatusDto.DirectionStatus.QUIT;
                                gateStatusDto.sensorsForward();
                                log.info("Gate auto closed : " + gateStatusDto.gateId);
                            }
                        }
                    } catch (IOException | ParseException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (GateStatusDto.SensorStatus.Quit.equals(gateStatusDto.sensor1) && GateStatusDto.SensorStatus.Quit.equals(gateStatusDto.sensor2)
                        && GateStatusDto.DirectionStatus.QUIT.equals(gateStatusDto.directionStatus) && GateStatusDto.SensorStatus.Quit.equals(gateStatusDto.photoElementStatus)
                        && Gate.GateType.REVERSE.equals(gateStatusDto.gateType)) {
                    if (getSensorStatus(loop) == SENSOR_ON && Gate.GateType.REVERSE.equals(gateStatusDto.gateType)) {
                        if (gateStatusDto.barrier.statusCheck) {
                            AbstractOpenStrategy strategy = LoopOpenStrategy.builder().build();
                            strategy.gateId = gateStatusDto.gateId;
                            strategy.isWaitPhel = true;
                            SensorStatusCheckJob.add(strategy);
                        } else {
                            boolean result = openGateForCarOut(gateStatusDto, gateStatusDto.isSimpleWhitelist);
                            if (result) {
                                gateStatusDto.lastTriggeredTime = System.currentTimeMillis();
                                gateStatusDto.sensorsReverse();
                                gateStatusDto.sensor1 = GateStatusDto.SensorStatus.Triggerred;
                                gateStatusDto.sensor2 = GateStatusDto.SensorStatus.WAIT;
                                gateStatusDto.directionStatus = GateStatusDto.DirectionStatus.REVERSE;
                                log.info("Gate opened with ip magnetic loop: " + gateStatusDto.gateId);
                            }
                        }
                    }
                }

                if (GateStatusDto.DirectionStatus.FORWARD.equals(gateStatusDto.directionStatus)) {
                    log.info("DIRECTION FORWARD");
                    if (GateStatusDto.SensorStatus.Triggerred.equals(gateStatusDto.sensor1) && GateStatusDto.SensorStatus.WAIT.equals(gateStatusDto.sensor2)) {
                        if (getSensorStatus(gateStatusDto.loop) == SENSOR_ON) {
                            log.info("CAR TRIGGERED CVT, ML TRIGGERED");
                            gateStatusDto.sensor1 = GateStatusDto.SensorStatus.PASSED;
                            gateStatusDto.sensor2 = GateStatusDto.SensorStatus.ON;
                            gateStatusDto.lastTriggeredTime = System.currentTimeMillis();
                            gateStatusDto.lastLoopTriggeredTime = System.currentTimeMillis();
                        } else if (getSensorStatus(gateStatusDto.loop) == SENSOR_OFF) {
                            log.info("CAR TRIGGERED CVT, ML WAITING");
                        }
                    } else if (GateStatusDto.SensorStatus.PASSED.equals(gateStatusDto.sensor1) && GateStatusDto.SensorStatus.ON.equals(gateStatusDto.sensor2)) {
                        if (getSensorStatus(gateStatusDto.loop) == SENSOR_ON) {
                            log.info("CAR PASSED CVT, CAR ON ML");
                            gateStatusDto.sensor1 = GateStatusDto.SensorStatus.Quit;
                            gateStatusDto.sensor2 = GateStatusDto.SensorStatus.ON;
                            gateStatusDto.lastTriggeredTime = System.currentTimeMillis();
                            gateStatusDto.lastLoopTriggeredTime = System.currentTimeMillis();
                        }
                    } else if (GateStatusDto.SensorStatus.Quit.equals(gateStatusDto.sensor1) && GateStatusDto.SensorStatus.ON.equals(gateStatusDto.sensor2)) {
                        if (getSensorStatus(gateStatusDto.loop) == SENSOR_ON) {
                            log.info("CVT IN QUIET MODE, CAR ON ML");
                            gateStatusDto.lastTriggeredTime = System.currentTimeMillis();
                            gateStatusDto.lastLoopTriggeredTime = System.currentTimeMillis();
                        } else if (getSensorStatus(gateStatusDto.loop) == SENSOR_OFF) {
                            log.info("CVT IN QUIET MODE, CAR PASSED ML");
                            gateStatusDto.sensor1 = GateStatusDto.SensorStatus.Quit;
                            gateStatusDto.sensor2 = GateStatusDto.SensorStatus.PASSED;
                        }
                    } else if (GateStatusDto.SensorStatus.Quit.equals(gateStatusDto.sensor1) && GateStatusDto.SensorStatus.PASSED.equals(gateStatusDto.sensor2)) {
                        if (getSensorStatus(gateStatusDto.loop) == SENSOR_ON) {
                            gateStatusDto.lastTriggeredTime = System.currentTimeMillis();
                            gateStatusDto.lastLoopTriggeredTime = System.currentTimeMillis();
                        } else if (getSensorStatus(gateStatusDto.loop) == SENSOR_OFF) {
                            log.info("CVT IN QUIET MODE, CAR FULLY PASSED ML AND ML NOT ACTIVATED");
                            gateStatusDto.sensor2 = GateStatusDto.SensorStatus.Quit;

                            try {
                                if (gateStatusDto.barrier.statusCheck) {
                                    AbstractCloseStrategy strategy = ForwardCloseStrategy.builder().build();
                                    strategy.gateId = gateStatusDto.gateId;
                                    SensorStatusCheckJob.add(strategy);
                                } else {
                                    boolean result = barrierService.closeBarrier(gateStatusDto, gateStatusDto.frontCamera.carEventDto.car_number, gateStatusDto.barrier);
                                    if (result) {
                                        gateStatusDto.gateStatus = GateStatusDto.GateStatus.Closed;
                                        gateStatusDto.directionStatus = GateStatusDto.DirectionStatus.QUIT;
                                        gateStatusDto.lastClosedTime = System.currentTimeMillis();
                                    }
                                }
                            } catch (IOException | ParseException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else if (GateStatusDto.DirectionStatus.REVERSE.equals(gateStatusDto.directionStatus)) {
                    if (GateStatusDto.SensorStatus.Triggerred.equals(gateStatusDto.sensor1) && GateStatusDto.SensorStatus.WAIT.equals(gateStatusDto.sensor2)) {
                        if (getSensorStatus(gateStatusDto.photoElement) == SENSOR_ON) {
                            log.info("CAR ON ML AND PHE triggerred");
                            gateStatusDto.sensor2 = GateStatusDto.SensorStatus.ON;
                            gateStatusDto.lastTriggeredTime = System.currentTimeMillis();
                            gateStatusDto.lastPhotoTriggeredTime = System.currentTimeMillis();
                        }
                    } else if (GateStatusDto.SensorStatus.Triggerred.equals(gateStatusDto.sensor1) && GateStatusDto.SensorStatus.ON.equals(gateStatusDto.sensor2)) {
                        if (getSensorStatus(gateStatusDto.photoElement) == SENSOR_OFF && getSensorStatus(gateStatusDto.loop) == SENSOR_OFF) {
                            log.info("CAR leaved ML AND PHE not triggered");
                            gateStatusDto.sensor2 = GateStatusDto.SensorStatus.PASSED;
                            gateStatusDto.sensor1 = GateStatusDto.SensorStatus.Quit;
                        } else if (getSensorStatus(gateStatusDto.photoElement) == SENSOR_ON || getSensorStatus(gateStatusDto.loop) == SENSOR_ON) {
                            log.info("CAR on ML AND/or on PHE");
                            gateStatusDto.lastTriggeredTime = System.currentTimeMillis();
                        }
                    } else if (GateStatusDto.SensorStatus.Quit.equals(gateStatusDto.sensor1) && GateStatusDto.SensorStatus.PASSED.equals(gateStatusDto.sensor2)) {
                        if (getSensorStatus(gateStatusDto.photoElement) == SENSOR_OFF && getSensorStatus(gateStatusDto.loop) == SENSOR_OFF) {
                            try {
                                if (gateStatusDto.barrier.statusCheck) {
                                    AbstractCloseStrategy strategy = ReverseCloseStrategy.builder().build();
                                    strategy.gateId = gateStatusDto.gateId;
                                    SensorStatusCheckJob.add(strategy);
                                } else {
                                    log.info("Start closing the gate: " + gateStatusDto.gateId);

                                    boolean result = barrierService.closeBarrier(gateStatusDto, null, gateStatusDto.barrier);
                                    if (result) {
                                        log.info("We closed the gate: " + gateStatusDto.gateId);
                                        gateStatusDto.gateStatus = GateStatusDto.GateStatus.Closed;
                                        gateStatusDto.directionStatus = GateStatusDto.DirectionStatus.QUIT;
                                        gateStatusDto.sensorsForward();
                                        gateStatusDto.lastClosedTime = System.currentTimeMillis();
                                    }
                                }
                            } catch (IOException | ParseException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        StatusCheckJob.isGatesProcessing.put(gateStatusDto.gateId, false);
    }

    private boolean triggerPassed() {
        if (gateStatusDto.lastTriggeredTime == null) {
            return false;
        }
        return System.currentTimeMillis() - gateStatusDto.lastTriggeredTime > 10000;  // больше 10ти секунд
    }

    private int getSensorStatus(SensorStatusDto sensor) throws ModbusProtocolException, ModbusNumberException, ModbusIOException {
        int status = SENSOR_UNDEFINED;
        try {
            status = barrierService.getSensorStatus(sensor);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return status;
    }

    private boolean openGateForCarOut(GateStatusDto gate, Boolean isSimple) throws ModbusProtocolException, ModbusNumberException, ModbusIOException {
        boolean result = false;
        if (isSimple) {
            try {
                result = barrierService.openBarrier(gate, null, gate.barrier);
            } catch (IOException | ParseException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
