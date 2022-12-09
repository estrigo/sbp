package kz.spt.app.model.strategy.barrier.open;

import com.fasterxml.jackson.databind.JsonNode;
import kz.spt.app.component.SpringContext;
import kz.spt.app.job.StatusCheckJob;
import kz.spt.app.model.dto.GateStatusDto;
import kz.spt.app.service.BarrierService;
import kz.spt.lib.utils.MessageKey;
import kz.spt.lib.model.Camera;
import kz.spt.lib.model.EventLog;
import kz.spt.lib.model.Gate;
import kz.spt.lib.model.dto.CarEventDto;
import kz.spt.lib.service.CarEventService;
import kz.spt.lib.service.EventLogService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.HashMap;
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
    public void carEvent() throws Exception {
        CarEventService carEventService = SpringContext.getBean(CarEventService.class);
        GateStatusDto gate = StatusCheckJob.findGateStatusDtoById(gateId);
        if (!gate.isSimpleWhitelist && !enteredFromThisSecondsBefore) {
            carEventService.saveCarInState(eventDto, camera, whitelistCheckResults, properties, null);
        }
    }

    @Override
    public void error() {
        EventLogService eventLogService = SpringContext.getBean(EventLogService.class);
        Map<String, Object> messageValues = new HashMap<>();
        messageValues.put("platenumber", eventDto.car_number);
        messageValues.put("description", camera.getGate().getDescription());
        messageValues.put("parking", camera.getGate().getParking().getName());

        String key = camera.getGate().getGateType().equals(Gate.GateType.IN) ? MessageKey.ERROR_BARRIER_CANNOT_ASSIGN_VALUE_IN :
                (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? MessageKey.ERROR_BARRIER_CANNOT_ASSIGN_VALUE_OUT : MessageKey.ERROR_BARRIER_CANNOT_ASSIGN_VALUE);

        eventLogService.sendSocketMessage(EventLogService.ArmEventType.CarEvent, EventLog.StatusType.Error, camera.getId(), eventDto.car_number, messageValues, key);
        eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, messageValues, key);
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
