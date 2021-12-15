package kz.spt.app.service.impl;

import kz.spt.lib.model.Camera;
import kz.spt.lib.model.CurrentUser;
import kz.spt.lib.model.Gate;
import kz.spt.lib.service.EventLogService;
import kz.spt.lib.service.ArmService;
import kz.spt.app.service.BarrierService;
import kz.spt.app.service.CameraService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class ArmServiceImpl implements ArmService {

    private CameraService cameraService;
    private BarrierService barrierService;
    private EventLogService eventLogService;
    private String dateFormat = "yyyy-MM-dd'T'HH:mm";

    public ArmServiceImpl(CameraService cameraService, BarrierService barrierService, EventLogService eventLogService){
        this.cameraService = cameraService;
        this.barrierService = barrierService;
        this.eventLogService = eventLogService;
    }

    @Override
    public Boolean openGate(Long cameraId) throws IOException, ParseException, InterruptedException {

        Camera camera = cameraService.getCameraById(cameraId);
        if(camera != null && camera.getGate() != null && camera.getGate().getBarrier() != null){
            SimpleDateFormat format = new SimpleDateFormat(dateFormat);
            Map<String, Object> properties = new HashMap<>();
            properties.put("eventTime", format.format(new Date()));
            properties.put("cameraIp", camera.getIp());
            properties.put("gateName", camera.getGate().getName());
            properties.put("gateDescription", camera.getGate().getDescription());
            properties.put("gateType", camera.getGate().getGateType().toString());
            properties.put("type", EventLogService.EventType.Allow);

            Boolean result = barrierService.openBarrier(camera.getGate().getBarrier(), properties);
            if(result){
                String username = "";
                if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CurrentUser) {
                    CurrentUser currentUser = (CurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                    if(currentUser!=null){
                        username = currentUser.getUsername();
                    }
                }
                eventLogService.sendSocketMessage(EventLogService.ArmEventType.CarEvent, camera.getId(), "", "Ручное открытие шлагбаума: Пользователь " + username + " открыл шлагбаум для " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "выезда":"въезда/выезда")) + " " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName());
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, "Ручное открытие шлагбаума: Пользователь " + username + " открыл шлагбаум для " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "выезда":"въезда/выезда")) + " " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName());
            }
            return result;
        }

        return false;
    }

    @Override
    public Boolean closeGate(Long cameraId) throws IOException, ParseException, InterruptedException {

        Camera camera = cameraService.getCameraById(cameraId);
        if(camera != null && camera.getGate() != null && camera.getGate().getBarrier() != null){
            SimpleDateFormat format = new SimpleDateFormat(dateFormat);
            Map<String, Object> properties = new HashMap<>();
            properties.put("eventTime", format.format(new Date()));
            properties.put("cameraIp", camera.getIp());
            properties.put("gateName", camera.getGate().getName());
            properties.put("gateDescription", camera.getGate().getDescription());
            properties.put("gateType", camera.getGate().getGateType().toString());
            properties.put("type", EventLogService.EventType.Allow);

            Boolean result = barrierService.closeBarrier(camera.getGate().getBarrier(), properties);
            if(result){
                String username = "";
                if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CurrentUser) {
                    CurrentUser currentUser = (CurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                    if(currentUser!=null){
                        username = currentUser.getUsername();
                    }
                }
                eventLogService.sendSocketMessage(EventLogService.ArmEventType.CarEvent, camera.getId(), "", "Ручное закрытие шлагбаума: Пользователь " + username + " закрыл шлагбаум для " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "выезда":"въезда/выезда")) + " " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName());
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, "Ручное закрытие шлагбаума: Пользователь " + username + " закрыл шлагбаум для " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "выезда":"въезда/выезда")) + " " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName());
            }
            return result;
        }

        return false;
    }
}
