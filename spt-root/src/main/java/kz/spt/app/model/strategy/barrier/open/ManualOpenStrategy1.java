package kz.spt.app.model.strategy.barrier.open;

import kz.spt.app.component.SpringContext;
import kz.spt.app.service.BarrierService;
import kz.spt.lib.utils.MessageKey;
import kz.spt.lib.model.Camera;
import kz.spt.lib.model.EventLog;
import kz.spt.lib.model.Gate;
import kz.spt.lib.service.EventLogService;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.HashMap;
import java.util.Map;

@Builder
@AllArgsConstructor
public class ManualOpenStrategy1 extends AbstractOpenStrategy {
    private Camera camera;
    private Map<String, Object> properties;

    @Override
    public void success() {

    }

    @Override
    public void carEvent() {
        EventLogService eventLogService = SpringContext.getBean(EventLogService.class);
        String username = "";

        Map<String, Object> messageValues = new HashMap<>();
        messageValues.put("username", username);
        messageValues.put("description", camera.getGate().getDescription());
        messageValues.put("parking", camera.getGate().getParking().getName());

        String key = camera.getGate().getGateType().equals(Gate.GateType.IN) ? MessageKey.MANUAL_OPEN_IN :
                (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? MessageKey.MANUAL_OPEN_OUT : MessageKey.MANUAL_OPEN);

        eventLogService.sendSocketMessage(EventLogService.ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), "", messageValues, key);
        eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, messageValues, key);
    }

    @Override
    public void error() {

    }

    @Override
    public boolean open() {
        BarrierService barrierService = SpringContext.getBean(BarrierService.class);
        try {
            return barrierService.openBarrier(camera.getGate().getBarrier(), properties);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
