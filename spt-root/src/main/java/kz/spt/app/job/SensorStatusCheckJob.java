package kz.spt.app.job;

import kz.spt.app.model.dto.GateStatusDto;
import kz.spt.app.model.strategy.AbstractStrategy;
import kz.spt.app.model.strategy.barrier.close.AbstractCloseStrategy;
import kz.spt.app.model.strategy.barrier.close.AutoCloseStrategy;
import kz.spt.app.model.strategy.barrier.open.AbstractOpenStrategy;
import kz.spt.app.service.BarrierService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Log
@Component
@EnableScheduling
@RequiredArgsConstructor
public class SensorStatusCheckJob {
    public static Queue<AbstractStrategy> events = new ConcurrentLinkedQueue<>();
    private static Queue<AbstractOpenStrategy> openCommands = new ConcurrentLinkedQueue<>();
    private static Queue<AbstractCloseStrategy> closeCommands = new ConcurrentLinkedQueue<>();
    private static AbstractOpenStrategy currentOpenCommand;
    private static AbstractCloseStrategy currentCloseCommand;
    private final int SENSOR_ON = 0;
    private final BarrierService barrierService;

    public static void add(AbstractOpenStrategy strategy) {
        boolean flag = true;
        for (AbstractOpenStrategy item : openCommands) {
            if (item.gateId.equals(strategy.gateId) && !item.status.equals(GateStatusDto.SensorStatus.PASSED)) {
                flag = false;
            }
        }
        if (flag) openCommands.add(strategy);
        log.info("Open commands: " + openCommands.size() + ", close commands: " + closeCommands.size());
    }

    public static void add(AbstractCloseStrategy strategy) {
        if (!closeCommands.isEmpty()) return;
        closeCommands.add(strategy);
        log.info("Open commands: " + openCommands.size() + ", close commands: " + closeCommands.size());
    }

    public static boolean isEmpty() {
        return openCommands.isEmpty() && closeCommands.isEmpty();
    }

    @SneakyThrows
    @Scheduled(fixedDelay = 500)
    public void event() {
        AbstractStrategy strategy = events.peek();
        if (strategy == null) return;

        GateStatusDto gate = StatusCheckJob.findGateStatusDtoById(strategy.gateId);
        if (strategy.isWaitLoop) {
            if (gate.lastLoopTriggeredTime != null) {
                strategy.carEvent();
                gate.lastLoopTriggeredTime = null;
            }
        } else if (strategy.isWaitPhel) {
            if (gate.lastPhotoTriggeredTime != null) {
                strategy.carEvent();
                gate.lastPhotoTriggeredTime = null;
            }
        } else {
            strategy.carEvent();
            events.remove(strategy);
        }
    }

    @SneakyThrows
    @Scheduled(fixedDelay = 500)
    public void open() {
        currentOpenCommand = openCommands.peek();
        if (currentOpenCommand == null) return;

        GateStatusDto gate = StatusCheckJob.findGateStatusDtoById(currentOpenCommand.gateId);
        currentOpenCommand.status = GateStatusDto.SensorStatus.Triggerred;
        if (barrierService.getSensorStatus(gate.fullOpenedSensor) == SENSOR_ON) {
            log.info("Gate full opened " + currentOpenCommand.getClass().getSimpleName());
            gate.lastOpenCommandSentTime = null;
            openCommands.remove(currentOpenCommand);
            currentOpenCommand = null;
            log.info("Open commands: " + openCommands.size() + ", close commands: " + closeCommands.size());
        } else if (barrierService.getSensorStatus(gate.fullClosedSensor) == SENSOR_ON) {
            Long timeDiffInMillis = System.currentTimeMillis() - (gate.lastOpenCommandSentTime == null ? System.currentTimeMillis() : gate.lastOpenCommandSentTime);
            if (gate.lastOpenCommandSentTime == null || timeDiffInMillis > 5000) {
                log.info("Gate full closed");
                if (currentOpenCommand.open()) {
                    log.info("Gate open command sent successfully");
                    gate.lastOpenCommandSentTime = System.currentTimeMillis();
                    currentOpenCommand.status = GateStatusDto.SensorStatus.WAIT;
                    currentOpenCommand.success();
                } else {
                    log.info("Gate open command sent error");
                    currentOpenCommand.error();
                }
            } else {
                log.info("Gate open command sent " + timeDiffInMillis);
            }
        }
    }

    @SneakyThrows
    @Scheduled(fixedDelay = 500)
    public void close() {
        currentCloseCommand = closeCommands.peek();
        if (currentCloseCommand == null) return;

        if (!GateStatusDto.SensorStatus.WAIT.equals(currentCloseCommand.status) && !openCommands.isEmpty()) return;

        GateStatusDto gate = StatusCheckJob.findGateStatusDtoById(currentCloseCommand.gateId);
        currentCloseCommand.status = GateStatusDto.SensorStatus.Triggerred;
        if (barrierService.getSensorStatus(gate.fullClosedSensor) == SENSOR_ON) {
            log.info("Gate full closed " + currentCloseCommand.getClass().getSimpleName());
            gate.lastCloseCommandSentTime = null;
            closeCommands.remove(currentCloseCommand);
            currentCloseCommand = null;
            log.info("Open commands: " + openCommands.size() + ", close commands: " + closeCommands.size());
        } else if (barrierService.getSensorStatus(gate.fullOpenedSensor) == SENSOR_ON) {
            Long timeDiffInMillis = System.currentTimeMillis() - (gate.lastCloseCommandSentTime == null ? System.currentTimeMillis() : gate.lastCloseCommandSentTime);
            if (gate.lastCloseCommandSentTime == null || timeDiffInMillis > 5000) {
                log.info("Gate full opened");
                if (currentCloseCommand.close()) {
                    log.info("Gate close command sent successfully");
                    gate.lastCloseCommandSentTime = System.currentTimeMillis();
                    currentCloseCommand.status = GateStatusDto.SensorStatus.WAIT;
                    currentCloseCommand.success();
                } else {
                    log.info("Gate close command sent error");
                    currentCloseCommand.error();
                }
            } else {
                log.info("Gate close command sent " + timeDiffInMillis);
            }
        }
    }

    @SneakyThrows
    @Scheduled(fixedDelay = 3000)
    public void status() {
        for (GateStatusDto gate : StatusCheckJob.globalGateDtos) {
            if (gate.fullClosedSensor == null) continue;
            if (barrierService.getSensorStatus(gate.fullClosedSensor) == SENSOR_ON && GateStatusDto.DirectionStatus.REVERSE.equals(gate.directionStatus)) {
                gate.gateStatus = GateStatusDto.GateStatus.Closed;
                gate.photoElementStatus = GateStatusDto.SensorStatus.Quit;
                gate.loopStatus = GateStatusDto.SensorStatus.Quit;
                gate.directionStatus = GateStatusDto.DirectionStatus.QUIT;
                gate.sensorsForward();
                log.info("Gate auto closed : " + gate.gateId);
            }
        }
    }
}
