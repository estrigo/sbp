package kz.spt.app.model.strategy.barrier.close;

import kz.spt.app.component.SpringContext;
import kz.spt.app.job.StatusCheckJob;
import kz.spt.app.model.dto.BarrierStatusDto;
import kz.spt.app.model.dto.GateStatusDto;
import kz.spt.app.service.BarrierService;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
public class ForwardCloseStrategy extends AbstractCloseStrategy {
    @Override
    public void success() {
        GateStatusDto gate = StatusCheckJob.findGateStatusDtoById(gateId);
        gate.gateStatus = GateStatusDto.GateStatus.Closed;
        gate.directionStatus = GateStatusDto.DirectionStatus.QUIT;
        gate.lastClosedTime = System.currentTimeMillis();
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
            return barrierService.closeBarrier(gate, gate.frontCamera.carEventDto.car_number, gate.barrier);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
