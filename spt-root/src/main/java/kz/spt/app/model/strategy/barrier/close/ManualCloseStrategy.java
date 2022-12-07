package kz.spt.app.model.strategy.barrier.close;

import kz.spt.app.component.SpringContext;
import kz.spt.app.service.BarrierService;
import kz.spt.lib.utils.MessageKey;
import kz.spt.lib.model.Camera;
import kz.spt.lib.model.CurrentUser;
import kz.spt.lib.model.EventLog;
import kz.spt.lib.model.Gate;
import kz.spt.lib.service.EventLogService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.Map;

@Builder
@AllArgsConstructor
public class ManualCloseStrategy extends AbstractCloseStrategy {
    private Camera camera;
    private Map<String, Object> properties;

    @Override
    public void success() {

    }

    @Override
    public void carEvent() {
        EventLogService eventLogService = SpringContext.getBean(EventLogService.class);

        String username = "";
        if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CurrentUser) {
            CurrentUser currentUser = (CurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (currentUser != null) {
                username = currentUser.getUsername();
            }
        }

        Map<String, Object> messageValues = new HashMap<>();
        messageValues.put("username", username);
        messageValues.put("description", camera.getGate().getDescription());
        messageValues.put("parking", camera.getGate().getParking().getName());

        String key = camera.getGate().getGateType().equals(Gate.GateType.IN) ? MessageKey.MANUAL_CLOSE_IN :
                (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? MessageKey.MANUAL_CLOSE_OUT : MessageKey.MANUAL_CLOSE);

        eventLogService.sendSocketMessage(EventLogService.ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), "", messageValues, key);
        eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, messageValues, key);
    }

    @Override
    public void error() {

    }

    @Override
    public boolean close() {
        BarrierService barrierService = SpringContext.getBean(BarrierService.class);
        try {
            return barrierService.closeBarrier(camera.getGate().getBarrier(), properties);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
