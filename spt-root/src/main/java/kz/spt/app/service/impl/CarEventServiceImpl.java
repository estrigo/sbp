package kz.spt.app.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.api.extension.PluginRegister;
import kz.spt.api.model.Barrier;
import kz.spt.api.model.Camera;
import kz.spt.api.model.Gate;
import kz.spt.api.model.Parking;
import kz.spt.api.service.EventLogService;
import kz.spt.api.service.EventLogService.ArmEventType;
import kz.spt.app.entity.dto.CarEventDto;
import kz.spt.app.service.CameraService;
import kz.spt.app.service.CarEventService;
import kz.spt.api.service.CarsService;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CarEventServiceImpl implements CarEventService {

    @Autowired
    private CarsService carsService;

    @Autowired
    private CameraService cameraService;

    @Autowired
    private EventLogService eventLogService;

    @Lazy
    @Autowired
    private PluginManager pluginManager;

    @Override
    public void saveCarEvent(CarEventDto eventDto) throws Exception {

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        PluginWrapper whitelistPlugin = pluginManager.getPlugin("whitelist-plugin");

        Camera camera = cameraService.findCameraByIp(eventDto.ip_address);

        if(camera!=null){

            eventDto.car_number = eventDto.car_number.toUpperCase();

            Map<String, Object> properties = new HashMap<>();
            properties.put("carNumber", eventDto.car_number);
            properties.put("eventTime", format.format(eventDto.event_time));
            properties.put("lp_rect", eventDto.lp_rect);
            properties.put("ip", eventDto.ip_address);

            eventLogService.sendSocketMessage(ArmEventType.Photo, camera.getId(), eventDto.car_number, eventDto.car_picture);
            eventLogService.createEventLog(Camera.class.getSimpleName(), camera.getId(), properties, "Зафиксирован новый номер авто " + eventDto.car_number);

            carsService.createCar(eventDto.car_number);

            Map<String, Object> gateProperties = new HashMap<>();
            properties.put("name", camera.getGate().getName());
            properties.put("description", camera.getGate().getDescription());
            properties.put("gate_type", camera.getGate().getGateType().toString());

            if(Gate.GateType.IN.equals(camera.getGate().getGateType())){
                if(Parking.ParkingType.WHITELIST.equals(camera.getGate().getParking().getParkingType())){
                    if(whitelistPlugin!=null && whitelistPlugin.getPluginState().equals(PluginState.STARTED)){
                        ObjectMapper objectMapper = new ObjectMapper();
                        ObjectNode node = objectMapper.createObjectNode();
                        node.put("car_number", eventDto.car_number);
                        node.put("event_time", format.format(eventDto.event_time));

                        List<PluginRegister> pluginRegisters =  pluginManager.getExtensions(PluginRegister.class, whitelistPlugin.getPluginId());
                        if(pluginRegisters.size() > 0){
                            PluginRegister pluginRegister = pluginRegisters.get(0);
                            JsonNode result = pluginRegister.execute(node);

                            boolean whitelistCheckResult = result.get("whitelistCheckResult").booleanValue();

                            if(whitelistCheckResult){
                                eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " присутствует в белом листе.");
                                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), gateProperties,  "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " присутствует в белом листе.");

                                Barrier barrier = camera.getGate().getBarrier();
                                String ip = barrier.getIp();
                                //TODO: open barrier
                            } else {
                                eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "В проезде отказано: Авто не найдено в белом листе " + eventDto.car_number);
                                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), gateProperties,  "В проезде отказано: Авто не найдено в белом листе " + eventDto.car_number);
                            }
                        }
                    } else {
                        eventLogService.createEventLog("Whitelist", null, null, "Плагин белого листа не найден или не запущен");
                    }
                } else {
                    Barrier barrier = camera.getGate().getBarrier();
                    String ip = barrier.getIp();

                    eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number);
                    eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), gateProperties, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number);
                }
            } else if(Gate.GateType.OUT.equals(camera.getGate().getGateType())){
                if(Parking.ParkingType.WHITELIST.equals(camera.getGate().getParking().getParkingType())){
                    Barrier barrier = camera.getGate().getBarrier();
                    String ip = barrier.getIp();

                    eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), gateProperties, "Выпускаем авто: Авто с гос. номером " + eventDto.car_number);
                    eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), gateProperties, "Выпускаем авто: Авто с гос. номером " + eventDto.car_number);
                } else {
                    //TODO: check payment plugin or open gate to leave
                }
            }
        } else {
            eventLogService.createEventLog(null, null, null, "Зафиксирован новый номер авто " + eventDto.car_number + " от неизвестной камеры с ip " + eventDto.ip_address);
        }
    }
}
