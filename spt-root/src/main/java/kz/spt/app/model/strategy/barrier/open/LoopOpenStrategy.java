package kz.spt.app.model.strategy.barrier.open;

import kz.spt.app.component.SpringContext;
import kz.spt.app.job.StatusCheckJob;
import kz.spt.app.model.dto.GateStatusDto;
import kz.spt.app.service.BarrierService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.java.Log;

@Log
@Builder
@AllArgsConstructor
public class LoopOpenStrategy extends AbstractOpenStrategy {
    @Override
    public void success() {
        GateStatusDto gate = StatusCheckJob.findGateStatusDtoById(gateId);
        gate.lastTriggeredTime = System.currentTimeMillis();
        gate.sensorsReverse();
        gate.sensor1 = GateStatusDto.SensorStatus.Triggerred;
        gate.sensor2 = GateStatusDto.SensorStatus.WAIT;
        gate.directionStatus = GateStatusDto.DirectionStatus.REVERSE;
        log.info("Gate opened with ip magnetic loop: " + gate.gateId);
    }

    @Override
    public void carEvent() {

    }

    @Override
    public void error() {

    }

    @Override
    public boolean open() {
        BarrierService barrierService = SpringContext.getBean(BarrierService.class);
        GateStatusDto gate = StatusCheckJob.findGateStatusDtoById(gateId);
        try {
            return barrierService.openBarrier(gate, null, gate.barrier);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
