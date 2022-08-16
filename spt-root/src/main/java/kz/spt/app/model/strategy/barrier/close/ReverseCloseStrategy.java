package kz.spt.app.model.strategy.barrier.close;

import kz.spt.app.component.SpringContext;
import kz.spt.app.job.StatusCheckJob;
import kz.spt.app.model.dto.BarrierStatusDto;
import kz.spt.app.model.dto.GateStatusDto;
import kz.spt.app.service.BarrierService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.java.Log;

@Log
@Builder
@AllArgsConstructor
public class ReverseCloseStrategy extends AbstractCloseStrategy {
    @Override
    public void success() {
        GateStatusDto gate = StatusCheckJob.findGateStatusDtoById(gateId);
        gate.gateStatus = GateStatusDto.GateStatus.Closed;
        gate.directionStatus = GateStatusDto.DirectionStatus.QUIT;
        gate.lastClosedTime = System.currentTimeMillis();
        gate.sensorsForward();
        log.info("We closed the gate: " + gate.gateId);
    }

    @Override
    public void carEvent() {

    }

    @Override
    public void error() {

    }

    @Override
    public boolean close() {
        BarrierService barrierService = SpringContext.getBean(BarrierService.class);
        GateStatusDto gate = StatusCheckJob.findGateStatusDtoById(gateId);
        try {
            return barrierService.closeBarrier(gate, null, gate.barrier);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
