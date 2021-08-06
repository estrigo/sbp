package kz.spt.app.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.api.extension.PluginRegister;
import kz.spt.api.model.Barrier;
import kz.spt.api.model.Camera;
import kz.spt.api.model.Gate;
import kz.spt.api.model.Parking;
import kz.spt.api.service.CarStateService;
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

    private CarsService carsService;
    private CameraService cameraService;
    private EventLogService eventLogService;
    private PluginManager pluginManager;
    private CarStateService carStateService;
    private final String dateFotmat = "yyyy-MM-dd'T'HH:mm";

    public CarEventServiceImpl(CarsService carsService, CameraService cameraService, EventLogService eventLogService,
                               PluginManager pluginManager, CarStateService carStateService){
        this.carsService = carsService;
        this.cameraService = cameraService;
        this.eventLogService = eventLogService;
        this.pluginManager = pluginManager;
        this.carStateService = carStateService;
    }

    @Override
    public void saveCarEvent(CarEventDto eventDto) throws Exception {

        SimpleDateFormat format = new SimpleDateFormat(dateFotmat);
        Camera camera = cameraService.findCameraByIp(eventDto.ip_address);

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put("carNumber", eventDto.car_number);
        eventProperties.put("eventTime", format.format(eventDto.event_time));
        eventProperties.put("lp_rect", eventDto.lp_rect);
        eventProperties.put("ip", eventDto.ip_address);

        if(camera!=null){
            eventDto.car_number = eventDto.car_number.toUpperCase();

            Map<String, Object> gateProperties = new HashMap<>();
            gateProperties.put("carNumber", eventDto.car_number);
            gateProperties.put("name", camera.getGate().getName());
            gateProperties.put("description", camera.getGate().getDescription());
            gateProperties.put("gate_type", camera.getGate().getGateType().toString());

            if(Gate.GateType.IN.equals(camera.getGate().getGateType())){
                handleCarInEvent(eventDto, camera, gateProperties, eventProperties, format);
            } else if(Gate.GateType.OUT.equals(camera.getGate().getGateType())){
                handleCarOutEvent(eventDto, camera, gateProperties);
            }
        } else {
            eventLogService.createEventLog(null, null, eventProperties, "Зафиксирован новый номер авто " + eventDto.car_number + " от неизвестной камеры с ip " + eventDto.ip_address);
        }
    }

    private void handleCarInEvent(CarEventDto eventDto, Camera camera, Map<String, Object> gateProperties, Map<String, Object> eventProperties,  SimpleDateFormat format) throws Exception{
        PluginWrapper whitelistPlugin = pluginManager.getPlugin("whitelist-plugin");

        if(carStateService.checkIsLastEnteredNotLeft(eventDto.car_number)){
            eventLogService.sendSocketMessage(ArmEventType.Photo, camera.getId(), eventDto.car_number, eventDto.car_picture);
        } else {
            eventLogService.sendSocketMessage(ArmEventType.Photo, camera.getId(), eventDto.car_number, eventDto.car_picture);
            eventLogService.createEventLog(Camera.class.getSimpleName(), camera.getId(), eventProperties, "Зафиксирован новый номер авто " + eventDto.car_number);

            carsService.createCar(eventDto.car_number);

            if(Parking.ParkingType.WHITELIST.equals(camera.getGate().getParking().getParkingType())){
                checkWhiteList(whitelistPlugin, eventDto, camera, gateProperties, format);
            } else {
                openGateBarrier(eventDto, camera, gateProperties);
            }
        }
    }

    private void checkWhiteList(PluginWrapper whitelistPlugin, CarEventDto eventDto, Camera camera, Map<String, Object> gateProperties, SimpleDateFormat format) throws Exception{
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
                    carStateService.createINState(eventDto.car_number, eventDto.event_time, camera);
                } else {
                    eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "В проезде отказано: Авто не найдено в белом листе " + eventDto.car_number);
                    eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), gateProperties,  "В проезде отказано: Авто не найдено в белом листе " + eventDto.car_number);
                }
            }
        } else {
            eventLogService.createEventLog("Whitelist", null, gateProperties, "Плагин белого листа не найден или не запущен");
        }
    }

    private void openGateBarrier(CarEventDto eventDto, Camera camera, Map<String, Object> gateProperties){
        Barrier barrier = camera.getGate().getBarrier();
        String ip = barrier.getIp();

        carStateService.createINState(eventDto.car_number, eventDto.event_time, camera);
        eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number);
        eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), gateProperties, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number);
    }

    private void handleCarOutEvent(CarEventDto eventDto, Camera camera, Map<String, Object> gateProperties){
        if(Parking.ParkingType.WHITELIST.equals(camera.getGate().getParking().getParkingType())){
            Barrier barrier = camera.getGate().getBarrier();
            String ip = barrier.getIp();

            eventLogService.sendSocketMessage(ArmEventType.Photo, camera.getId(), eventDto.car_number, eventDto.car_picture);
            carStateService.createOUTState(eventDto.car_number, eventDto.event_time, camera, null, null, null);
            eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "Выпускаем авто: Авто с гос. номером " + eventDto.car_number);
            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), gateProperties, "Выпускаем авто: Авто с гос. номером " + eventDto.car_number);
        } else {
            //TODO: check payment plugin or open gate to leave
            //carStateService.createOUTState();
        }
    }
}
