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
import kz.spt.app.service.BarrierService;
import kz.spt.app.service.CameraService;
import kz.spt.app.service.CarEventService;
import kz.spt.api.service.CarsService;
import kz.spt.app.service.CarImageService;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CarEventServiceImpl implements CarEventService {

    private final CarsService carsService;
    private final CameraService cameraService;
    private final EventLogService eventLogService;
    private final PluginManager pluginManager;
    private final CarStateService carStateService;
    private final CarImageService carImageService;
    private final BarrierService barrierService;

    public CarEventServiceImpl(CarsService carsService, CameraService cameraService, EventLogService eventLogService,
                               PluginManager pluginManager, CarStateService carStateService, CarImageService carImageService,
                               BarrierService barrierService){
        this.carsService = carsService;
        this.cameraService = cameraService;
        this.eventLogService = eventLogService;
        this.pluginManager = pluginManager;
        this.carStateService = carStateService;
        this.carImageService = carImageService;
        this.barrierService = barrierService;
    }

    @Override
    public void saveCarEvent(CarEventDto eventDto) throws Exception {

        String dateFormat = "yyyy-MM-dd'T'HH:mm";
        SimpleDateFormat format = new SimpleDateFormat(dateFormat);
        Camera camera = cameraService.findCameraByIp(eventDto.ip_address);

        Map<String, Object> properties = new HashMap<>();
        properties.put("carNumber", eventDto.car_number);
        properties.put("eventTime", format.format(eventDto.event_time));
        properties.put("lp_rect", eventDto.lp_rect);
        properties.put("cameraIp", eventDto.ip_address);

        if(camera!=null){
            eventDto.car_number = eventDto.car_number.toUpperCase();

            properties.put("gateName", camera.getGate().getName());
            properties.put("gateDescription", camera.getGate().getDescription());
            properties.put("gateType", camera.getGate().getGateType().toString());
            String carImageUrl = carImageService.saveImage(eventDto.car_picture, eventDto.event_time, eventDto.car_number);

            if(Gate.GateType.IN.equals(camera.getGate().getGateType())){
                handleCarInEvent(eventDto, camera, properties, format,carImageUrl);
            } else if(Gate.GateType.OUT.equals(camera.getGate().getGateType())){
                handleCarOutEvent(eventDto, camera, properties, carImageUrl);
            }
        } else {
            eventLogService.createEventLog(null, null, properties, "Зафиксирован новый номер авто " + eventDto.car_number + " от неизвестной камеры с ip " + eventDto.ip_address);
        }
    }

    private void handleCarInEvent(CarEventDto eventDto, Camera camera, Map<String, Object> properties,  SimpleDateFormat format, String carImageUrl) throws Exception{
        PluginWrapper whitelistPlugin = pluginManager.getPlugin("whitelist-plugin");

        if(carStateService.checkIsLastEnteredNotLeft(eventDto.car_number)){
            eventLogService.sendSocketMessage(ArmEventType.Photo, camera.getId(), eventDto.car_number, eventDto.car_picture);
        } else {
            properties.put("carImageUrl", carImageUrl);
            eventLogService.sendSocketMessage(ArmEventType.Photo, camera.getId(), eventDto.car_number, eventDto.car_picture);
            eventLogService.createEventLog(Camera.class.getSimpleName(), camera.getId(), properties, "Зафиксирован новый номер авто " + eventDto.car_number);

            carsService.createCar(eventDto.car_number);

            if(Parking.ParkingType.WHITELIST.equals(camera.getGate().getParking().getParkingType())){
                checkWhiteList(whitelistPlugin, eventDto, camera, properties, format);
            } else {
                Boolean barrierResult = openGateBarrier(eventDto, camera, properties);
                if(barrierResult){
                    carStateService.createINState(eventDto.car_number, eventDto.event_time, camera);
                    eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number);
                    eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number);
                }
            }
        }
    }

    private void checkWhiteList(PluginWrapper whitelistPlugin, CarEventDto eventDto, Camera camera, Map<String, Object> properties, SimpleDateFormat format) throws Exception{
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
                    Boolean barrierResult = openGateBarrier(eventDto, camera, properties);
                    if(barrierResult){
                        carStateService.createINState(eventDto.car_number, eventDto.event_time, camera);
                        eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " присутствует в белом листе.");
                        eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties,  "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " присутствует в белом листе.");
                    }
                } else {
                    eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "В проезде отказано: Авто не найдено в белом листе " + eventDto.car_number);
                    eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties,  "В проезде отказано: Авто не найдено в белом листе " + eventDto.car_number);
                }
            }
        } else {
            eventLogService.createEventLog("Whitelist", null, properties, "Плагин белого листа не найден или не запущен");
        }
    }

    private Boolean openGateBarrier(CarEventDto eventDto, Camera camera, Map<String, Object> properties) throws IOException, ParseException, InterruptedException {
        Barrier barrier = camera.getGate().getBarrier();
        return barrierService.openBarrier(barrier, properties);
    }

    private void handleCarOutEvent(CarEventDto eventDto, Camera camera, Map<String, Object> properties, String carImageUrl) throws IOException, ParseException, InterruptedException {

        properties.put("carImageUrl", carImageUrl);

        if(Parking.ParkingType.WHITELIST.equals(camera.getGate().getParking().getParkingType())){
            eventLogService.sendSocketMessage(ArmEventType.Photo, camera.getId(), eventDto.car_number, eventDto.car_picture);
            Boolean barrierResult = openGateBarrier(eventDto, camera, properties);
            if(barrierResult){
                carStateService.createOUTState(eventDto.car_number, eventDto.event_time, camera, null, null, null);
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "Выпускаем авто: Авто с гос. номером " + eventDto.car_number);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "Выпускаем авто: Авто с гос. номером " + eventDto.car_number);
            }
        } else {
            //TODO: check payment plugin or open gate to leave
            //carStateService.createOUTState();
        }
    }
}
