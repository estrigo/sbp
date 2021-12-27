package kz.spt.app.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.app.config.ParkingProperties;
import kz.spt.app.job.StatusCheckJob;
import kz.spt.app.model.dto.GateStatusDto;
import kz.spt.lib.service.PluginService;
import kz.spt.lib.utils.StaticValues;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.model.*;
import kz.spt.lib.service.CarStateService;
import kz.spt.lib.service.EventLogService;
import kz.spt.lib.service.EventLogService.ArmEventType;
import kz.spt.lib.model.dto.CarEventDto;
import kz.spt.app.service.BarrierService;
import kz.spt.app.service.CameraService;
import kz.spt.lib.service.CarEventService;
import kz.spt.lib.service.CarsService;
import kz.spt.lib.service.CarImageService;
import lombok.extern.java.Log;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Log
@Service
public class CarEventServiceImpl implements CarEventService {

    private final CarsService carsService;
    private final CameraService cameraService;
    private final EventLogService eventLogService;
    private final CarStateService carStateService;
    private final CarImageService carImageService;
    private final BarrierService barrierService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PluginService pluginService;
    private final ParkingProperties parkingProperties;
    private String dateFormat = "yyyy-MM-dd'T'HH:mm";

    private static Map<String, Long> concurrentHashMap = new ConcurrentHashMap<>();
    private ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag(LocaleContextHolder.getLocale().toString().substring(0,2)));

    public CarEventServiceImpl(CarsService carsService, CameraService cameraService, EventLogService eventLogService,
                               CarStateService carStateService, CarImageService carImageService,
                               BarrierService barrierService, PluginService pluginService, ParkingProperties parkingProperties){
        this.carsService = carsService;
        this.cameraService = cameraService;
        this.eventLogService = eventLogService;
        this.carStateService = carStateService;
        this.carImageService = carImageService;
        this.barrierService = barrierService;
        this.pluginService = pluginService;
        this.parkingProperties = parkingProperties;
    }


    @Override
    public void handleTempCarEvent(MultipartFile file, String json) throws Exception {

        Map<String,String> camerasIpMap = parkingProperties.getCameras();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(json);

        String ip_address = camerasIpMap.get(jsonNode.get("data").get("camera_id").asText());
        log.info(jsonNode.get("data").get("camera_id").asText()  + " " + camerasIpMap.get(jsonNode.get("data").get("camera_id").asText()));

        String car_number = ((ArrayNode) jsonNode.get("data").get("results")).get(0).get("plate").asText().toUpperCase();

        String base64 = null;
        try {
            base64 = StringUtils.newStringUtf8(Base64.encodeBase64(file.getInputStream().readAllBytes(), false));
        } catch (IOException e) {
            e.printStackTrace();
        }

        CarEventDto eventDto = new CarEventDto();
        eventDto.car_number = car_number;
        eventDto.event_time = new Date();
        eventDto.ip_address = ip_address;
        eventDto.car_picture = base64;
        saveCarEvent(eventDto);
    }

    @Override
    public boolean passCar(Long cameraId, String platenumber) throws Exception {
        Boolean barrierResult = false;
        if(platenumber != null){
            Camera camera = cameraService.getCameraById(cameraId);
            if(camera != null){
                String username = "";
                if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CurrentUser) {
                    CurrentUser currentUser = (CurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                    if(currentUser!=null){
                        username = currentUser.getUsername();
                    }
                }

                SimpleDateFormat format = new SimpleDateFormat(dateFormat);

                Map<String, Object> properties = new HashMap<>();
                properties.put("eventTime", format.format(new Date()));
                properties.put("cameraIp", camera.getIp());
                properties.put("gateName", camera.getGate().getName());
                properties.put("gateDescription", camera.getGate().getDescription());
                properties.put("gateType", camera.getGate().getGateType().toString());
                properties.put("type", EventLogService.EventType.Allow);

                eventLogService.sendSocketMessage(EventLogService.ArmEventType.CarEvent, camera.getId(), "", "Ручной поропуск Авто с гос. номером " + platenumber + ". Пользователь " + username + " открыл шлагбаум для " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "выезда":"въезда/выезда")) + " " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName());
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, "Ручной поропуск Авто с гос. номером " + platenumber + ". Пользователь " + username + " открыл шлагбаум для " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "выезда":"въезда/выезда")) + " " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName());
                
                CarEventDto eventDto = new CarEventDto();
                eventDto.event_time = new Date();
                eventDto.car_number = platenumber;
                eventDto.ip_address = camera.getIp();
                eventDto.car_picture = null;
                eventDto.lp_rect = null;
                eventDto.lp_picture = null;
                eventDto.manualOpen = true;

                saveCarEvent(eventDto);
            }
        }
        return barrierResult;
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
            if(!eventDto.manualOpen){
                if(concurrentHashMap.containsKey(eventDto.ip_address)){
                    Long timeDiffInMillis = System.currentTimeMillis() - concurrentHashMap.get(eventDto.ip_address);
                    if(timeDiffInMillis < (camera.getTimeout() == null ? 0 : camera.getTimeout()*1000)){ // If interval smaller than timeout then ignore else proceed
                        log.info("Ignored event from camera: " + eventDto.ip_address  + " time: " +  timeDiffInMillis);
                        return;
                    } else {
                        concurrentHashMap.put(eventDto.ip_address, System.currentTimeMillis());
                    }
                } else {
                    concurrentHashMap.put(eventDto.ip_address, System.currentTimeMillis());
                }
            }

            log.info("handling event from camera: " + eventDto.ip_address);

            properties.put("gateName", camera.getGate().getName());
            properties.put("gateId", camera.getGate().getId());
            properties.put("gateDescription", camera.getGate().getDescription());
            properties.put("gateType", camera.getGate().getGateType().toString());

            if(!eventDto.manualOpen) {
                String carImageUrl = carImageService.saveImage(eventDto.car_picture, eventDto.event_time, eventDto.car_number);
                properties.put(StaticValues.carImagePropertyName, carImageUrl);
                properties.put(StaticValues.carSmallImagePropertyName, carImageUrl.replace(StaticValues.carImageExtension, "") + StaticValues.carImageSmallAddon + StaticValues.carImageExtension);
            }

            for(GateStatusDto gate: StatusCheckJob.globalGateDtos){
                if(gate.gateId == camera.getGate().getId()){
                    log.info("Camera belongs to gate: " + gate.gateId);
                    if(gate.frontCamera != null && gate.frontCamera.id == camera.getId()){
                        gate.frontCamera.carEventDto = eventDto;
                        gate.frontCamera.properties  = properties;
                    } else if(gate.backCamera != null && gate.backCamera.id == camera.getId()){
                        gate.backCamera.carEventDto = eventDto;
                        gate.backCamera.properties  = properties;
                    }
                    if(eventDto.manualOpen || gate.lastClosedTime == null || System.currentTimeMillis() - gate.lastClosedTime > 5500){ //если последний раз закрыли больше 6 секунды
                        if(Gate.GateType.REVERSE.equals(camera.getGate().getGateType())){
                            JsonNode whitelistCheckResults = getWhiteLists(camera.getGate().getParking().getId(), eventDto.car_number, eventDto.event_time, format, properties);
                            boolean hasAccess = checkSimpleWhiteList(eventDto, camera, properties, whitelistCheckResults);
                            if(hasAccess){
                                boolean openResult = barrierService.openBarrier(camera.getGate().getBarrier(), properties);
                                if(openResult){
                                    gate.gateStatus = GateStatusDto.GateStatus.Open;
                                    gate.sensor1 = GateStatusDto.SensorStatus.Triggerred;
                                    gate.sensor2 = GateStatusDto.SensorStatus.WAIT;
                                    gate.directionStatus = GateStatusDto.DirectionStatus.FORWARD;
                                    gate.lastTriggeredTime = System.currentTimeMillis();
                                }
                            }
                        } else if(Gate.GateType.IN.equals(camera.getGate().getGateType())) {
                            log.info("Gate type: " + camera.getGate().getGateType());
                            JsonNode whitelistCheckResults = null;
                            if(Parking.ParkingType.WHITELIST.equals(camera.getGate().getParking().getParkingType()) || Parking.ParkingType.WHITELIST_PAYMENT.equals(camera.getGate().getParking().getParkingType())){
                                whitelistCheckResults = getWhiteLists(camera.getGate().getParking().getId(), eventDto.car_number, eventDto.event_time, format, properties);
                            }
                            boolean hasAccess;
                            if(gate.isSimpleWhitelist){
                                log.info("Simple whitelist check");
                                hasAccess = checkSimpleWhiteList(eventDto, camera, properties, whitelistCheckResults);
                            } else {
                                log.info("Complex whitelist check");
                                hasAccess = handleCarInEvent(eventDto, camera, properties, format, whitelistCheckResults);
                            }
                            log.info("hasAccess: " + hasAccess);
                            if(hasAccess){
                                boolean openResult = barrierService.openBarrier(camera.getGate().getBarrier(), properties);
                                if(openResult){
                                    gate.gateStatus = GateStatusDto.GateStatus.Open;
                                    gate.sensor1 = GateStatusDto.SensorStatus.Triggerred;
                                    gate.sensor2 = GateStatusDto.SensorStatus.WAIT;
                                    gate.directionStatus = GateStatusDto.DirectionStatus.FORWARD;
                                    gate.lastTriggeredTime = System.currentTimeMillis();
                                    if(!gate.isSimpleWhitelist){
                                        saveCarInState(eventDto, camera, whitelistCheckResults, properties);
                                    }
                                }
                            } else {

                            }
                        } else if(Gate.GateType.OUT.equals(camera.getGate().getGateType())){
                            boolean hasAccess;
                            boolean isWhitelistCar = false;
                            BigDecimal balance = BigDecimal.ZERO;
                            BigDecimal rateResult = null;
                            CarState carState = carStateService.getLastNotLeft(eventDto.car_number);

                            if(gate.isSimpleWhitelist){
                                hasAccess = true;
                            } else {
                                if(Parking.ParkingType.WHITELIST_PAYMENT.equals(camera.getGate().getParking().getParkingType()) || Parking.ParkingType.PAYMENT.equals(camera.getGate().getParking().getParkingType())){
                                    if(Parking.ParkingType.WHITELIST_PAYMENT.equals(camera.getGate().getParking().getParkingType())){
                                        if(carState != null && carState.getPaid() != null && !carState.getPaid()){
                                            isWhitelistCar = true;
                                        }
                                    }

                                    if(!isWhitelistCar){
                                        if(carState == null){
                                            eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "Не найден запись о вьезде. Авто с гос. номером " + eventDto.car_number);
                                            properties.put("type", EventLogService.EventType.Deny);
                                            eventLogService.createEventLog(CarState.class.getSimpleName(), null, properties, "Не найден запись о вьезде. Авто с гос. номером " + eventDto.car_number);
                                        } else {
                                            PluginRegister ratePluginRegister = pluginService.getPluginRegister(StaticValues.ratePlugin);
                                            if(ratePluginRegister != null){
                                                ObjectNode node = this.objectMapper.createObjectNode();
                                                node.put("parkingId", camera.getGate().getParking().getId());
                                                node.put("inDate", format.format(carState.getInTimestamp()));
                                                node.put("outDate", format.format(eventDto.event_time));
                                                node.put("cashlessPayment", carState.getCashlessPayment() != null ? carState.getCashlessPayment() : false);

                                                JsonNode result = ratePluginRegister.execute(node);
                                                rateResult = result.get("rateResult").decimalValue().setScale(2);
                                            } else {
                                                properties.put("type", EventLogService.EventType.Error);
                                                eventLogService.createEventLog("Rate", null, properties, "Плагин вычисления стоимости парковки не найден или не запущен. Авто с гос. номером" + eventDto.car_number);
                                            }

                                            PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
                                            if(billingPluginRegister != null){
                                                ObjectNode billinNode = this.objectMapper.createObjectNode();
                                                billinNode.put("command", "getCurrentBalance");
                                                billinNode.put("plateNumber", carState.getCarNumber());
                                                JsonNode billingResult = billingPluginRegister.execute(billinNode);
                                                balance = billingResult.get("currentBalance").decimalValue().setScale(2);
                                            } else {
                                                properties.put("type", EventLogService.EventType.Error);
                                                eventLogService.createEventLog("Billing", null, properties, "Плагин работы с балансами не найден или не запущен. Авто с гос. номером" + eventDto.car_number);
                                            }
                                        }
                                    }
                                }
                                hasAccess = handleCarOutEvent(eventDto, camera, properties, format, carState, isWhitelistCar, balance, rateResult);
                            }
                            if(hasAccess){
                                boolean openResult = barrierService.openBarrier(camera.getGate().getBarrier(), properties);
                                if(openResult){
                                    gate.gateStatus = GateStatusDto.GateStatus.Open;
                                    gate.sensor1 = GateStatusDto.SensorStatus.Triggerred;
                                    gate.sensor2 = GateStatusDto.SensorStatus.WAIT;
                                    gate.directionStatus = GateStatusDto.DirectionStatus.FORWARD;
                                    gate.lastTriggeredTime = System.currentTimeMillis();
                                    if(!gate.isSimpleWhitelist && carState != null){
                                        saveCarOutState(eventDto, camera, carState, properties, isWhitelistCar, balance,  rateResult, format);
                                    }
                                }
                            }
                        }
                    } else {
                        if(gate.lastClosedTime != null){
                            log.info("last closed date diff: " + (System.currentTimeMillis() - gate.lastClosedTime > 5500));
                        } else {
                            log.info("last closed date is null");
                        }
                    }
                }
            }
        } else {
            properties.put("type", EventLogService.EventType.Error);
            eventLogService.createEventLog(null, null, properties, bundle.getString("events.newLicensePlateIdentified") + " " + eventDto.car_number + " от неизвестной камеры с ip " + eventDto.ip_address);
        }
    }

    private boolean handleCarInEvent(CarEventDto eventDto, Camera camera, Map<String, Object> properties, SimpleDateFormat format, JsonNode whitelistCheckResults) throws Exception{
        eventLogService.sendSocketMessage(ArmEventType.Photo, camera.getId(), eventDto.car_number, eventDto.car_picture);
        properties.put("type", EventLogService.EventType.Success);

        if(!carStateService.checkIsLastEnteredNotLeft(eventDto.car_number)){
            log.info("not las entered not left");
            eventLogService.createEventLog(Camera.class.getSimpleName(), camera.getId(), properties, "Зафиксирован новый номер авто " + eventDto.car_number);

            carsService.createCar(eventDto.car_number);

            if(Parking.ParkingType.WHITELIST.equals(camera.getGate().getParking().getParkingType())){
                log.info("las entered not left");
                return checkWhiteList(eventDto, camera, properties, whitelistCheckResults);
            } else {
                return true;
            }
        } else {
            log.info("las entered not left");
        }
        return false;
    }

    private void saveCarInState(CarEventDto eventDto, Camera camera, JsonNode whitelistCheckResults, Map<String, Object> properties){
        if(Parking.ParkingType.WHITELIST.equals(camera.getGate().getParking().getParkingType())){
            carStateService.createINState(eventDto.car_number, eventDto.event_time, camera, false, whitelistCheckResults.toString());
        } else {
            if(whitelistCheckResults == null){
                carStateService.createINState(eventDto.car_number, eventDto.event_time, camera, true, null);
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " на платной основе");
                properties.put("type", EventLogService.EventType.Allow);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " на платной основе");
            } else {
                ArrayNode whitelistCheckResultArray = (ArrayNode) whitelistCheckResults;
                boolean hasAccess = false;
                JsonNode nodeDetails = null;
                Iterator<JsonNode> iterator = whitelistCheckResultArray.iterator();
                while (iterator.hasNext()){
                    JsonNode node = iterator.next();
                    if(!node.has("exceedPlaceLimit") || (node.has("exceedPlaceLimit") && !node.get("exceedPlaceLimit").booleanValue())){
                        hasAccess = true;
                    } else {
                        nodeDetails = node;
                    }
                }
                if(hasAccess){
                    carStateService.createINState(eventDto.car_number, eventDto.event_time, camera, false, whitelistCheckResults.toString());
                    eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " в рамках белого листа");

                    properties.put("type", EventLogService.EventType.Allow);
                    eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " в рамках белого листа");
                } else {
                    carStateService.createINState(eventDto.car_number, eventDto.event_time, camera, true, whitelistCheckResults.toString());
                    eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " на платной основе. Все места для группы " + nodeDetails.get("groupName").textValue() + " заняты следующим списком " + nodeDetails.get("placeOccupiedCars").toString());

                    properties.put("type", EventLogService.EventType.Allow);
                    eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " на платной основе. " + (nodeDetails.has("placeName") ? nodeDetails.get("placeName") + " место " : "Все места " ) + "для группы " + nodeDetails.get("groupName").textValue() + " заняты следующим списком " + nodeDetails.get("placeOccupiedCars").toString());
                }
            }
        }
    }

    private boolean checkWhiteList(CarEventDto eventDto, Camera camera, Map<String, Object> properties, JsonNode whitelistCheckResults) throws Exception{
        if(whitelistCheckResults != null){
            ArrayNode whitelistCheckResultArray = (ArrayNode) whitelistCheckResults;
            JsonNode customDetails = null;
            boolean hasAccess = false;
            Iterator<JsonNode> iterator = whitelistCheckResultArray.iterator();
            while (iterator.hasNext()){
                JsonNode node = iterator.next();

                if("CUSTOM".equals(node.get("type").asText())){
                    if(!node.has("exceedPlaceLimit") || (node.has("exceedPlaceLimit") && !node.get("exceedPlaceLimit").booleanValue())){
                        hasAccess = true;
                    } else {
                        customDetails = node;
                    }
                } else {
                    hasAccess = true;
                }
            }
            if(hasAccess){
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " присутствует в белом листе.");
                properties.put("type", EventLogService.EventType.Allow);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties,  "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " присутствует в белом листе.");
                return true;
            } else {
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "В проезде отказано: Все места в для группы " + customDetails.get("groupName").textValue() + " заняты следующим списком " + customDetails.get("placeOccupiedCars").toString() + ". Авто "  + eventDto.car_number);
                properties.put("type", EventLogService.EventType.Deny);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties,  "В проезде отказано: Все места в для группы " + customDetails.get("groupName").textValue() + " заняты следующим списком " + customDetails.get("placeOccupiedCars").toString() + ". Авто "  + eventDto.car_number);
                return false;
            }
        } else {
            eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "В проезде отказано: Авто не найдено в белом листе " + eventDto.car_number);
            properties.put("type", EventLogService.EventType.Deny);
            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties,  "В проезде отказано: Авто не найдено в белом листе " + eventDto.car_number);
            return false;
        }
    }

    private boolean checkSimpleWhiteList(CarEventDto eventDto, Camera camera, Map<String, Object> properties, JsonNode whitelistCheckResults) {

        eventLogService.sendSocketMessage(ArmEventType.Photo, camera.getId(), eventDto.car_number, eventDto.car_picture);
        eventLogService.createEventLog(Camera.class.getSimpleName(), camera.getId(), properties, "Зафиксирован новый номер авто " + eventDto.car_number);
        carsService.createCar(eventDto.car_number);

        boolean hasAccess = false;
        if(whitelistCheckResults != null){
            ArrayNode whitelistCheckResultArray = (ArrayNode) whitelistCheckResults;
            JsonNode customDetails = null;
            Iterator<JsonNode> iterator = whitelistCheckResultArray.iterator();
            while (iterator.hasNext()){
                JsonNode node = iterator.next();
                if("CUSTOM".equals(node.get("type").asText())){
                    if(!node.has("exceedPlaceLimit") || (node.has("exceedPlaceLimit") && !node.get("exceedPlaceLimit").booleanValue())){
                        hasAccess = true;
                    } else {
                        customDetails = node;
                    }
                } else {
                    hasAccess = true;
                }
            }
            if(hasAccess){
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " присутствует в белом листе.");
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties,  "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " присутствует в белом листе.");
            } else {
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "В проезде отказано: Все места в для группы " + customDetails.get("groupName").textValue() + " заняты следующим списком " + customDetails.get("placeOccupiedCars").toString() + ". Авто "  + eventDto.car_number);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties,  "В проезде отказано: Все места в для группы " + customDetails.get("groupName").textValue() + " заняты следующим списком " + customDetails.get("placeOccupiedCars").toString() + ". Авто "  + eventDto.car_number);
            }
        } else {
            eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "В проезде отказано: Авто не найдено в белом листе " + eventDto.car_number);
            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties,  "В проезде отказано: Авто не найдено в белом листе " + eventDto.car_number);
        }
        return hasAccess;
    }

    private JsonNode getWhiteLists(Long parkingId, String car_number, Date event_time, SimpleDateFormat format, Map<String, Object> properties) throws Exception {
        ObjectNode node = this.objectMapper.createObjectNode();
        JsonNode whitelistCheckResult = null;
        node.put("parkingId", parkingId);
        node.put("car_number", car_number);
        node.put("event_time", format.format(event_time));

        PluginRegister whitelistPluginRegister = pluginService.getPluginRegister(StaticValues.whitelistPlugin);
        if(whitelistPluginRegister != null){
            JsonNode result = whitelistPluginRegister.execute(node);
            whitelistCheckResult = result.get("whitelistCheckResult");
        } else {
            properties.put("type", EventLogService.EventType.Error);
            eventLogService.createEventLog("Whitelist", null, properties, "Плагин белого листа не найден или не запущен. Авто с гос. номером " + car_number);
        }
        return whitelistCheckResult;
    }

    private Boolean openGateBarrier(Camera camera, Map<String, Object> properties) throws IOException, ParseException, InterruptedException {
        log.info("Called to open barrier on gate " + camera.getGate().getName());
        Barrier barrier = camera.getGate().getBarrier();
        return barrierService.openBarrier(barrier, properties);
    }

    private boolean handleCarOutEvent(CarEventDto eventDto, Camera camera, Map<String, Object> properties, SimpleDateFormat format, CarState carState, boolean isWhitelistCar, BigDecimal balance, BigDecimal rateResult) throws Exception {
        eventLogService.sendSocketMessage(ArmEventType.Photo, camera.getId(), eventDto.car_number, eventDto.car_picture);

        if(carState == null){
            if(!carStateService.checkIsLastLeft(eventDto.car_number, eventDto.ip_address)){
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "Не найден запись о вьезде. Авто с гос. номером " + eventDto.car_number);
                properties.put("type", EventLogService.EventType.Deny);
                eventLogService.createEventLog(CarState.class.getSimpleName(), null, properties, "Не найден запись о вьезде. Авто с гос. номером " + eventDto.car_number);
                if(Parking.ParkingType.WHITELIST.equals(camera.getGate().getParking().getParkingType())){
                    return true;
                } else {
                    return false;
                }
            }
        }

        if(Parking.ParkingType.WHITELIST.equals(camera.getGate().getParking().getParkingType())){
            eventLogService.sendSocketMessage(ArmEventType.Photo, camera.getId(), eventDto.car_number, eventDto.car_picture);
            eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "Выпускаем авто: Авто с гос. номером " + eventDto.car_number);
            properties.put("type", EventLogService.EventType.Allow);
            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "Выпускаем авто: Авто с гос. номером " + eventDto.car_number);
            return true;
        } else {
            if(!isWhitelistCar){
                if(balance.compareTo(rateResult) >= 0){
                    return true;
                } else {
                    eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "В проезде отказано: Не достаточно средств для списания оплаты за паркинг. Сумма к оплате: " + rateResult + " тенге. Баланс: " + balance +" тенге. В проезде отказано.");
                    properties.put("type", EventLogService.EventType.Deny);
                    eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "В проезде отказано: Не достаточно средств для списания оплаты за паркинг. Сумма к оплате: " + rateResult + " тенге. Баланс: " + balance +" тенге. В проезде отказано.");
                    return false;
                }
            }  else {
                eventLogService.sendSocketMessage(ArmEventType.Photo, camera.getId(), eventDto.car_number, eventDto.car_picture);
                return true;
            }
        }
    }

    private void saveCarOutState(CarEventDto eventDto, Camera camera, CarState carState, Map<String, Object> properties, boolean isWhitelistCar, BigDecimal balance, BigDecimal rateResult, SimpleDateFormat format) throws Exception {
        if(Parking.ParkingType.WHITELIST.equals(camera.getGate().getParking().getParkingType())){
            carStateService.createOUTState(eventDto.car_number, eventDto.event_time, camera, carState);
        } else {
            if(!isWhitelistCar){
                BigDecimal subtractResult = balance.subtract(rateResult);
                PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
                if(billingPluginRegister != null){
                    ObjectNode billingSubtractNode = this.objectMapper.createObjectNode();
                    billingSubtractNode.put("command", "decreaseCurrentBalance");
                    billingSubtractNode.put("amount", rateResult);
                    billingSubtractNode.put("plateNumber", carState.getCarNumber());
                    subtractResult = billingPluginRegister.execute(billingSubtractNode).get("currentBalance").decimalValue();
                }
                subtractResult = subtractResult.setScale(2);

                carState.setRateAmount(rateResult);
                carStateService.createOUTState(eventDto.car_number, eventDto.event_time, camera, carState);
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "Пропускаем авто: Оплата за паркинг присутствует. Сумма к оплате: " + rateResult + " тенге. Остаток баланса: " + subtractResult + " тенге. Проезд разрешен.");

                properties.put("type", EventLogService.EventType.Allow);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "Пропускаем авто: Оплата за паркинг присутствует. Сумма к оплате: " + rateResult + " тенге. Остаток баланса: " + subtractResult + " тенге. Проезд разрешен.");

                if(billingPluginRegister != null){
                    ObjectNode addTimestampNode = this.objectMapper.createObjectNode();
                    addTimestampNode.put("command", "addOutTimestampToPayments");
                    addTimestampNode.put("outTimestamp", format.format(eventDto.event_time));
                    addTimestampNode.put("carStateId", carState.getId());
                    billingPluginRegister.execute(addTimestampNode);
                }
            } else {
                carStateService.createOUTState(eventDto.car_number, eventDto.event_time, camera, carState);
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "Пропускаем авто: Присуствовал в белом списке. Авто с гос. номером " + eventDto.car_number);

                properties.put("type", EventLogService.EventType.Allow);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "Пропускаем авто: Присуствовал в белом списке. Авто с гос. номером " + eventDto.car_number);
            }
        }
    }
}
