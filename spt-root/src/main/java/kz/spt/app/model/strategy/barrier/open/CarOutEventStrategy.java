package kz.spt.app.model.strategy.barrier.open;

import com.fasterxml.jackson.databind.JsonNode;
import kz.spt.app.component.SpringContext;
import kz.spt.app.job.StatusCheckJob;
import kz.spt.app.model.dto.GateStatusDto;
import kz.spt.app.service.BarrierService;
import kz.spt.lib.service.MessageKey;
import kz.spt.lib.model.Camera;
import kz.spt.lib.model.CarState;
import kz.spt.lib.model.EventLog;
import kz.spt.lib.model.Gate;
import kz.spt.lib.model.dto.CarEventDto;
import kz.spt.lib.service.CarEventService;
import kz.spt.lib.service.EventLogService;
import kz.spt.lib.utils.StaticValues;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.SneakyThrows;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

@Builder
@AllArgsConstructor
public class CarOutEventStrategy extends AbstractOpenStrategy {
    private Camera camera;
    private CarState carState;
    private JsonNode whitelist;
    private JsonNode abonements;
    private CarEventDto eventDto;
    private BigDecimal balance;
    private BigDecimal rateResult;
    private BigDecimal zeroTouchValue;
    private SimpleDateFormat format;
    private StaticValues.CarOutBy carOutBy;
    private Map<String, Object> properties;
    private boolean leftFromThisSecondsBefore;

    @SneakyThrows
    @Override
    public void success() {
        GateStatusDto gate = StatusCheckJob.findGateStatusDtoById(gateId);
        gate.gateStatus = GateStatusDto.GateStatus.Open;
        gate.sensor1 = GateStatusDto.SensorStatus.Triggerred;
        gate.sensor2 = GateStatusDto.SensorStatus.WAIT;
        gate.directionStatus = GateStatusDto.DirectionStatus.FORWARD;
        gate.lastTriggeredTime = System.currentTimeMillis();
    }

    @SneakyThrows
    @Override
    public void carEvent() {
        EventLogService eventLogService = SpringContext.getBean(EventLogService.class);
        CarEventService carEventService = SpringContext.getBean(CarEventService.class);
        GateStatusDto gate = StatusCheckJob.findGateStatusDtoById(gateId);
        if (!gate.isSimpleWhitelist && !leftFromThisSecondsBefore && carState != null) {
            carEventService.saveCarOutState(eventDto, camera, carState, properties, balance, rateResult, zeroTouchValue, format, carOutBy, abonements, whitelist);
        } else if (leftFromThisSecondsBefore) {
            properties.put("type", EventLog.StatusType.Allow);

            Map<String, Object> messageValues = new HashMap<>();
            messageValues.put("platenumber", eventDto.car_number);

            eventLogService.sendSocketMessage(EventLogService.ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.car_number, messageValues, MessageKey.PASS);
            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, messageValues, MessageKey.PASS);
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
        eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, messageValues, key, EventLog.EventType.ERROR);
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
