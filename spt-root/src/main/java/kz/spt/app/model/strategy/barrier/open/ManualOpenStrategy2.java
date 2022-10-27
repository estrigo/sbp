package kz.spt.app.model.strategy.barrier.open;

import kz.spt.app.component.SpringContext;
import kz.spt.app.job.StatusCheckJob;
import kz.spt.app.model.dto.GateStatusDto;
import kz.spt.app.service.BarrierService;
import kz.spt.app.service.impl.ArmServiceImpl;
import kz.spt.lib.model.Camera;
import kz.spt.lib.model.EventLog;
import kz.spt.lib.model.Gate;
import kz.spt.lib.model.dto.CarEventDto;
import kz.spt.lib.service.CarEventService;
import kz.spt.lib.service.CarImageService;
import kz.spt.lib.service.EventLogService;
import kz.spt.lib.service.PaymentService;
import kz.spt.lib.utils.StaticValues;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.SneakyThrows;

import java.util.Date;
import java.util.Map;

@Builder
@AllArgsConstructor
public class ManualOpenStrategy2 extends AbstractOpenStrategy {
    private Camera camera;
    private String snapshot;
    private String debtPlateNumber;
    private Map<String, Object> properties;

    @SneakyThrows
    @Override
    public void success() {

    }

    @SneakyThrows
    @Override
    public void carEvent() {
        PaymentService paymentService = SpringContext.getBean(PaymentService.class);
        EventLogService eventLogService = SpringContext.getBean(EventLogService.class);
        CarImageService carImageService = SpringContext.getBean(CarImageService.class);
        CarEventService carEventService = SpringContext.getBean(CarEventService.class);

        String username = "";
        if (Gate.GateType.OUT.equals(camera.getGate().getGateType()) || Gate.GateType.REVERSE.equals(camera.getGate().getGateType())) {
            String debtPlatenumber = eventLogService.findLastNotEnoughFunds(camera.getGate().getId());

            if (debtPlatenumber != null) {
                properties.put("carNumber", debtPlatenumber);

                if (ArmServiceImpl.hashtable.containsKey(debtPlatenumber)) {
                    Long timeDiffInMillis = System.currentTimeMillis() - ArmServiceImpl.hashtable.get(debtPlatenumber);
                    if (timeDiffInMillis > 2 * 1000) { // если больше 2 секунд то принимать команду
                        ArmServiceImpl.hashtable.put(debtPlatenumber, System.currentTimeMillis());
                    } else {
                        return;
                    }
                } else {
                    ArmServiceImpl.hashtable.put(debtPlatenumber, System.currentTimeMillis());
                }
            }

            eventLogService.sendSocketMessage(EventLogService.ArmEventType.Photo, EventLog.StatusType.Success, camera.getId(), debtPlatenumber, snapshot, null, null);
            eventLogService.sendSocketMessage(EventLogService.ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), debtPlatenumber,
                    "Ручное открытие шлагбаума: Пользователь " + username + " открыл шлагбаум для " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "выезда" : "въезда/выезда")) + " " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName(),
                    "Manual opening gate: User " + username + " opened gate for " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "enter" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName(),
                    "Manuelles Öffnen des Tores: Benutzer"+ username + " geöffnetes Tor für " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "enter" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName());

            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,
                    "Ручное открытие шлагбаума: Пользователь " + username + " открыл шлагбаум для " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "выезда" : "въезда/выезда")) + " " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName(),
                    "Manual gate opening: User " + username + " opened gate for " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "enter" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName(),
                    "Manuelles Öffnen des Tores: Benutzer"+ username + " geöffnetes Tor für " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "enter" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName());

            if (debtPlatenumber != null) {
                if (snapshot != null && !"".equals(snapshot) && !"null".equals(snapshot) && !"undefined".equals(snapshot) && !"data:image/jpg;base64,null".equals(snapshot)) {
                    String carImageUrl = carImageService.saveImage(snapshot, new Date(), debtPlatenumber);
                    properties.put(StaticValues.carImagePropertyName, carImageUrl);
                    properties.put(StaticValues.carSmallImagePropertyName, carImageUrl.replace(StaticValues.carImageExtension, "") + StaticValues.carImageSmallAddon + StaticValues.carImageExtension);
                }
                paymentService.createDebtAndOUTState(debtPlatenumber, camera, properties);
            }
        } else if (Gate.GateType.IN.equals(camera.getGate().getGateType()) || Gate.GateType.REVERSE.equals(camera.getGate().getGateType())) {
            String debtPlatenumber = eventLogService.findLastWithDebts(camera.getGate().getId());

            if (debtPlatenumber != null) {
                properties.put("carNumber", debtPlatenumber);
            }

            eventLogService.sendSocketMessage(EventLogService.ArmEventType.Photo, EventLog.StatusType.Success, camera.getId(), debtPlatenumber, snapshot, null, null);
            eventLogService.sendSocketMessage(EventLogService.ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), debtPlatenumber,
                    "Ручное открытие шлагбаума: Пользователь " + username + " открыл шлагбаум для " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "выезда" : "въезда/выезда")) + " " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName(),
                    "Manual opening gate: User " + username + " opened gate for " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "enter" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName(),
                    "Manuelles Öffnen des Tores: Benutzer"+ username + " geöffnetes Tor für " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "enter" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName());

            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,
                    "Ручное открытие шлагбаума: Пользователь " + username + " открыл шлагбаум для " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "выезда" : "въезда/выезда")) + " " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName(),
                    "Manual gate opening: User " + username + " opened gate for " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "enter" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName(),
                    "Manuelles Öffnen des Tores: Benutzer"+ username + " geöffnetes Tor für " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "enter" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName());

            if (debtPlatenumber != null) {
                if (snapshot != null && !"".equals(snapshot) && !"null".equals(snapshot) && !"undefined".equals(snapshot) && !"data:image/jpg;base64,null".equals(snapshot)) {
                    String carImageUrl = carImageService.saveImage(snapshot, new Date(), debtPlatenumber);
                    properties.put(StaticValues.carImagePropertyName, carImageUrl);
                    properties.put(StaticValues.carSmallImagePropertyName, carImageUrl.replace(StaticValues.carImageExtension, "") + StaticValues.carImageSmallAddon + StaticValues.carImageExtension);
                }

                CarEventDto eventDto = new CarEventDto();
                eventDto.event_date_time = new Date();
                eventDto.car_number = debtPlatenumber;
                eventDto.ip_address = camera.getIp();
                eventDto.lp_rect = null;
                eventDto.lp_picture = null;
                eventDto.manualEnter = true;
                eventDto.manualOpen = true;

                carEventService.saveCarEvent(eventDto);
            }
        }
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
