package kz.spt.app.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.app.utils.StaticValues;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.model.*;
import kz.spt.lib.service.CarStateService;
import kz.spt.lib.service.EventLogService;
import kz.spt.lib.service.EventLogService.ArmEventType;
import kz.spt.app.model.dto.CarEventDto;
import kz.spt.app.service.BarrierService;
import kz.spt.app.service.CameraService;
import kz.spt.app.service.CarEventService;
import kz.spt.lib.service.CarsService;
import kz.spt.lib.service.CarImageService;
import lombok.extern.java.Log;
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

@Log
@Service
public class CarEventServiceImpl implements CarEventService {

    private final CarsService carsService;
    private final CameraService cameraService;
    private final EventLogService eventLogService;
    private final PluginManager pluginManager;
    private final CarStateService carStateService;
    private final CarImageService carImageService;
    private final BarrierService barrierService;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        SimpleDateFormat format = new SimpleDateFormat(StaticValues.dateFormat);
        eventDto.car_number = eventDto.car_number.toUpperCase();

        Camera camera = cameraService.findCameraByIp(eventDto.ip_address);
        Map<String, Object> properties = new HashMap<>();
        properties.put("carNumber", eventDto.car_number);
        properties.put("eventTime", format.format(eventDto.event_time));
        properties.put("lp_rect", eventDto.lp_rect);
        properties.put("cameraIp", eventDto.ip_address);

        if(camera!=null){
            properties.put("gateName", camera.getGate().getName());
            properties.put("gateDescription", camera.getGate().getDescription());
            properties.put("gateType", camera.getGate().getGateType().toString());

            String carImageUrl = carImageService.saveImage(eventDto.car_picture, eventDto.event_time, eventDto.car_number);

            if(Gate.GateType.IN.equals(camera.getGate().getGateType())){
                handleCarInEvent(eventDto, camera, properties, format, carImageUrl);
            } else if(Gate.GateType.OUT.equals(camera.getGate().getGateType())){
                handleCarOutEvent(eventDto, camera, properties, format, carImageUrl);
            }
        } else {
            eventLogService.createEventLog(null, null, properties, "Зафиксирован новый номер авто " + eventDto.car_number + " от неизвестной камеры с ip " + eventDto.ip_address);
        }
    }

    private void handleCarInEvent(CarEventDto eventDto, Camera camera, Map<String, Object> properties, SimpleDateFormat format, String carImageUrl) throws Exception{
        eventLogService.sendSocketMessage(ArmEventType.Photo, camera.getId(), eventDto.car_number, eventDto.car_picture);

        if(!carStateService.checkIsLastEnteredNotLeft(eventDto.car_number)){
            properties.put("carImageUrl", carImageUrl);
            eventLogService.sendSocketMessage(ArmEventType.Photo, camera.getId(), eventDto.car_number, eventDto.car_picture);
            eventLogService.createEventLog(Camera.class.getSimpleName(), camera.getId(), properties, "Зафиксирован новый номер авто " + eventDto.car_number);

            carsService.createCar(eventDto.car_number);

            if(Parking.ParkingType.WHITELIST.equals(camera.getGate().getParking().getParkingType())){
                checkWhiteList(eventDto, camera, properties, format);
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

    private void checkWhiteList(CarEventDto eventDto, Camera camera, Map<String, Object> properties, SimpleDateFormat format) throws Exception{
        PluginWrapper whitelistPlugin = pluginManager.getPlugin("whitelist-plugin");
        if(whitelistPlugin!=null && whitelistPlugin.getPluginState().equals(PluginState.STARTED)){
            List<PluginRegister> pluginRegisters =  pluginManager.getExtensions(PluginRegister.class, whitelistPlugin.getPluginId());
            if(pluginRegisters.size() > 0){
                ObjectNode node = this.objectMapper.createObjectNode();
                node.put("car_number", eventDto.car_number);
                node.put("event_time", format.format(eventDto.event_time));

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
            eventLogService.createEventLog("Whitelist", null, properties, "Плагин белого листа не найден или не запущен. Авто с гос. номером" + eventDto.car_number);
        }
    }

    private Boolean openGateBarrier(CarEventDto eventDto, Camera camera, Map<String, Object> properties) throws IOException, ParseException, InterruptedException {
        Barrier barrier = camera.getGate().getBarrier();
        return barrierService.openBarrier(barrier, properties);
    }

    private void handleCarOutEvent(CarEventDto eventDto, Camera camera, Map<String, Object> properties, SimpleDateFormat format, String carImageUrl) throws Exception {

        properties.put("carImageUrl", carImageUrl);
        eventLogService.sendSocketMessage(ArmEventType.Photo, camera.getId(), eventDto.car_number, eventDto.car_picture);

        if(Parking.ParkingType.WHITELIST.equals(camera.getGate().getParking().getParkingType())){
            eventLogService.sendSocketMessage(ArmEventType.Photo, camera.getId(), eventDto.car_number, eventDto.car_picture);
            Boolean barrierResult = openGateBarrier(eventDto, camera, properties);
            if(barrierResult){
                carStateService.createOUTState(eventDto.car_number, eventDto.event_time, camera, null, null, null);
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "Выпускаем авто: Авто с гос. номером " + eventDto.car_number);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "Выпускаем авто: Авто с гос. номером " + eventDto.car_number);
            }
        } else if(Parking.ParkingType.PAYMENT.equals(camera.getGate().getParking().getParkingType())){
            PluginWrapper ratePlugin = pluginManager.getPlugin("rate-plugin");
            if(ratePlugin!=null && ratePlugin.getPluginState().equals(PluginState.STARTED)){
                List<PluginRegister> pluginRegisters = pluginManager.getExtensions(PluginRegister.class, ratePlugin.getPluginId());
                if(pluginRegisters.size() > 0){
                    CarState carState = carStateService.getLastNotLeft(eventDto.car_number);
                    if(carState != null){
                        ObjectNode node = this.objectMapper.createObjectNode();
                        node.put("parkingId", camera.getGate().getParking().getId());
                        node.put("inDate", format.format(carState.getInTimestamp()));
                        node.put("outDate", format.format(eventDto.event_time));

                        PluginRegister pluginRegister = pluginRegisters.get(0);
                        JsonNode result = pluginRegister.execute(node);

                        int rateResult = result.get("rateResult").intValue();

                        //TODO: check payment plugin or open gate to leave
                        eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "Пропускаем авто: Оплата за паркинг присутствует. Сумма к оплате: " + rateResult + " тенге. Долг: 0 тенге. Проезд разрешен.");
                        eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "Пропускаем авто: Оплата за паркинг присутствует. Сумма к оплате: " + rateResult + " тенге. Долг: 0 тенге. Проезд разрешен.");
                        Boolean barrierResult = openGateBarrier(eventDto, camera, properties);
                        if(barrierResult){
                            carStateService.createOUTState(eventDto.car_number, eventDto.event_time, camera, null, null, null);
                            eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "Выпускаем авто: Авто с гос. номером " + eventDto.car_number);
                            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "Выпускаем авто: Авто с гос. номером " + eventDto.car_number);
                        }
                    } else {
                        eventLogService.createEventLog(CarState.class.getSimpleName(), null, properties, "Не найден запись о вьезде. Авто с гос. номером" + eventDto.car_number);
                    }
                }
            } else {
                eventLogService.createEventLog("Rate", null, properties, "Плагин вычисления стоимости парковки не найден или не запущен. Авто с гос. номером" + eventDto.car_number);
            }
        } else {
            throw new RuntimeException("Unknown parking type");
        }
    }
}
