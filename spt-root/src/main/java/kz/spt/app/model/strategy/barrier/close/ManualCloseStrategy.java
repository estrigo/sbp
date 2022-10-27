package kz.spt.app.model.strategy.barrier.close;

import kz.spt.app.component.SpringContext;
import kz.spt.app.job.StatusCheckJob;
import kz.spt.app.model.dto.GateStatusDto;
import kz.spt.app.service.BarrierService;
import kz.spt.lib.model.Camera;
import kz.spt.lib.model.CurrentUser;
import kz.spt.lib.model.EventLog;
import kz.spt.lib.model.Gate;
import kz.spt.lib.service.EventLogService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.security.core.context.SecurityContextHolder;

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

        eventLogService.sendSocketMessage(EventLogService.ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), "",
                "Ручное закрытие шлагбаума: Пользователь " + username + " закрыл шлагбаум для " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "выезда" : "въезда/выезда")) + " " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName(),
                "Manual closing gate: User " + username + " closed gate for " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "enter" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName(),
                "Manual closing gate: User " + username + " closed gate for " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "enter" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName());
        eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,
                "Ручное закрытие шлагбаума: Пользователь " + username + " закрыл шлагбаум для " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "выезда" : "въезда/выезда")) + " " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName(),
                "Manual closing gate: User " + username + " closed gate for " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "enter" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName(),
                "Manual closing gate: User " + username + " closed gate for " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "enter" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName());
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
