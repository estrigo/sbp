package kz.spt.app.model.strategy.barrier.open;

import com.fasterxml.jackson.databind.JsonNode;
import kz.spt.app.component.SpringContext;
import kz.spt.app.job.StatusCheckJob;
import kz.spt.app.model.dto.GateStatusDto;
import kz.spt.app.service.BarrierService;
import kz.spt.lib.model.Camera;
import kz.spt.lib.model.EventLog;
import kz.spt.lib.model.Gate;
import kz.spt.lib.model.dto.CarEventDto;
import kz.spt.lib.service.CarEventService;
import kz.spt.lib.service.EventLogService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;

import java.util.Map;

@Builder
@AllArgsConstructor
@ToString
public class CarInEventStrategy extends AbstractOpenStrategy {
    private Camera camera;
    private CarEventDto eventDto;
    private Map<String, Object> properties;
    private JsonNode whitelistCheckResults;
    private Boolean enteredFromThisSecondsBefore;

    @Override
    public void success() {
        GateStatusDto gate = StatusCheckJob.findGateStatusDtoById(gateId);
        gate.gateStatus = GateStatusDto.GateStatus.Open;
        gate.sensor1 = GateStatusDto.SensorStatus.Triggerred;
        gate.sensor2 = GateStatusDto.SensorStatus.WAIT;
        gate.directionStatus = GateStatusDto.DirectionStatus.FORWARD;
        gate.lastTriggeredTime = System.currentTimeMillis();
    }

    @Override
    public void carEvent() {
        CarEventService carEventService = SpringContext.getBean(CarEventService.class);
        GateStatusDto gate = StatusCheckJob.findGateStatusDtoById(gateId);
        if (!gate.isSimpleWhitelist && !enteredFromThisSecondsBefore) {
            carEventService.saveCarInState(eventDto, camera, whitelistCheckResults, properties);
        }
    }

    @Override
    public void error() {
        EventLogService eventLogService = SpringContext.getBean(EventLogService.class);

        String descriptionRu = "Ошибка открытия шлагбаума: На контроллер шлагбаума не удалось присвоит значение на открытие для авто " + eventDto.car_number + " на " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезд" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "выезд" : "въезд/выезд")) + " для " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName();
        String descriptionEn = "Error while barrier open: Cannot assign value to open barrier for car " + eventDto.car_number + " to " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "pass" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName();
        String descriptionDe = "Fehler beim Öffnen der Schranke: Wert für offene Schranke für Auto kann nicht zugewiesen werden" + eventDto.car_number + " to " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "pass" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName();
        eventLogService.sendSocketMessage(EventLogService.ArmEventType.CarEvent, EventLog.StatusType.Error, camera.getId(), eventDto.car_number, descriptionRu, descriptionEn, descriptionDe);
        eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, descriptionRu, descriptionEn, descriptionDe);
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
