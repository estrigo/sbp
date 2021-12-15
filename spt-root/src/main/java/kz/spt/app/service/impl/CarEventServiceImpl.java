package kz.spt.app.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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

    private static Map<String, Long> concurrentHashMap = new ConcurrentHashMap<>();

    public CarEventServiceImpl(CarsService carsService, CameraService cameraService, EventLogService eventLogService,
                               CarStateService carStateService, CarImageService carImageService,
                               BarrierService barrierService, PluginService pluginService){
        this.carsService = carsService;
        this.cameraService = cameraService;
        this.eventLogService = eventLogService;
        this.carStateService = carStateService;
        this.carImageService = carImageService;
        this.barrierService = barrierService;
        this.pluginService = pluginService;
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

            log.info("handling event from camera: " + eventDto.ip_address);

            properties.put("gateName", camera.getGate().getName());
            properties.put("gateId", camera.getGate().getId());
            properties.put("gateDescription", camera.getGate().getDescription());
            properties.put("gateType", camera.getGate().getGateType().toString());

            String carImageUrl = carImageService.saveImage(eventDto.car_picture, eventDto.event_time, eventDto.car_number);
            properties.put(StaticValues.carImagePropertyName, carImageUrl);
            properties.put(StaticValues.carSmallImagePropertyName, carImageUrl.replace(StaticValues.carImageExtension, "") + StaticValues.carImageSmallAddon + StaticValues.carImageExtension);

            if(Gate.GateType.IN.equals(camera.getGate().getGateType())){
                handleCarInEvent(eventDto, camera, properties, format);
            } else if(Gate.GateType.OUT.equals(camera.getGate().getGateType())){
                handleCarOutEvent(eventDto, camera, properties, format);
            }
        } else {
            properties.put("type", EventLogService.EventType.Success);
            eventLogService.createEventLog(null, null, properties, "Зафиксирован новый номер авто " + eventDto.car_number + " от неизвестной камеры с ip " + eventDto.ip_address);
        }
    }

    @Override
    public void handleTempCarEvent(MultipartFile file, String json) throws Exception {
        Map<String,String> camerasIpMap = new HashMap<>();
        camerasIpMap.put("camera-1", "10.66.22.20");
        camerasIpMap.put("camera-2","10.66.22.23");

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

    private void handleCarInEvent(CarEventDto eventDto, Camera camera, Map<String, Object> properties, SimpleDateFormat format) throws Exception{
        eventLogService.sendSocketMessage(ArmEventType.Photo, camera.getId(), eventDto.car_number, eventDto.car_picture);

        if(!carStateService.checkIsLastEnteredNotLeft(eventDto.car_number)){
            eventLogService.sendSocketMessage(ArmEventType.Photo, camera.getId(), eventDto.car_number, eventDto.car_picture);
            properties.put("type", EventLogService.EventType.Success);
            eventLogService.createEventLog(Camera.class.getSimpleName(), camera.getId(), properties, "Зафиксирован новый номер авто " + eventDto.car_number);

            carsService.createCar(eventDto.car_number);

            if(Parking.ParkingType.WHITELIST.equals(camera.getGate().getParking().getParkingType())){
                checkWhiteList(eventDto, camera, properties, format);
            }
            else {
                JsonNode whitelistCheckResults = null;
                if(Parking.ParkingType.WHITELIST_PAYMENT.equals(camera.getGate().getParking().getParkingType())){
                    whitelistCheckResults = getWhiteLists(camera.getGate().getParking().getId(), eventDto.car_number, eventDto.event_time, format, properties);
                }
                Boolean barrierResult = openGateBarrier(camera, properties);
                if(barrierResult){
                    Boolean carPassed = checkCarPassed(camera, properties);

                    if(carPassed){
                        if(whitelistCheckResults == null){
                            carStateService.createINState(eventDto.car_number, eventDto.event_time, camera, true, null);
                            eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " на платной основе");

                            properties.put("type", EventLogService.EventType.Allow);
                            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " на платной основе");
                        } else {
                            ArrayNode whitelistCheckResultArray = (ArrayNode) whitelistCheckResults;
                            boolean hasAccess = false;
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
                                carStateService.createINState(eventDto.car_number, eventDto.event_time, camera, false, whitelistCheckResults.toString());
                                eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " в рамках белого листа");

                                properties.put("type", EventLogService.EventType.Allow);
                                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " в рамках белого листа");
                            } else {
                                carStateService.createINState(eventDto.car_number, eventDto.event_time, camera, true, whitelistCheckResults.toString());
                                eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " на платной основе. Все места в для группы " + customDetails.get("groupName").textValue() + " заняты следующим списком " + customDetails.get("placeOccupiedCars").toString());

                                properties.put("type", EventLogService.EventType.Allow);
                                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " на платной основе. Все места в для группы " + customDetails.get("groupName").textValue() + " заняты следующим списком " + customDetails.get("placeOccupiedCars").toString());
                            }
                        }
                    }
                    Boolean closed = closeGateBarrier(camera, properties);
                }
            }
        }
    }

    private void checkWhiteList(CarEventDto eventDto, Camera camera, Map<String, Object> properties, SimpleDateFormat format) throws Exception{
        JsonNode whitelistCheckResults = getWhiteLists(camera.getGate().getParking().getId(), eventDto.car_number, eventDto.event_time, format, properties);
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
                Boolean barrierResult = openGateBarrier(camera, properties);
                if(barrierResult){
                    carStateService.createINState(eventDto.car_number, eventDto.event_time, camera, false, whitelistCheckResults.toString());
                    eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " присутствует в белом листе.");

                    properties.put("type", EventLogService.EventType.Allow);
                    eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties,  "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " присутствует в белом листе.");
                }
            } else {
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "В проезде отказано: Все места в для группы " + customDetails.get("groupName").textValue() + " заняты следующим списком " + customDetails.get("placeOccupiedCars").toString() + ". Авто "  + eventDto.car_number);

                properties.put("type", EventLogService.EventType.Deny);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties,  "В проезде отказано: Все места в для группы " + customDetails.get("groupName").textValue() + " заняты следующим списком " + customDetails.get("placeOccupiedCars").toString() + ". Авто "  + eventDto.car_number);
            }
        } else {
            eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "В проезде отказано: Авто не найдено в белом листе " + eventDto.car_number);

            properties.put("type", EventLogService.EventType.Deny);
            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties,  "В проезде отказано: Авто не найдено в белом листе " + eventDto.car_number);
        }
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

    private Boolean closeGateBarrier(Camera camera, Map<String, Object> properties) throws IOException, ParseException, InterruptedException {
        Barrier barrier = camera.getGate().getBarrier();
        return barrierService.closeBarrier(barrier, properties);
    }

    private Boolean checkCarPassed(Camera camera, Map<String, Object> properties) throws IOException, ParseException, InterruptedException {
        Barrier barrier = camera.getGate().getBarrier();
        return barrierService.checkCarPassed(barrier, properties);
    }

    private void handleCarOutEvent(CarEventDto eventDto, Camera camera, Map<String, Object> properties, SimpleDateFormat format) throws Exception {
        eventLogService.sendSocketMessage(ArmEventType.Photo, camera.getId(), eventDto.car_number, eventDto.car_picture);

        CarState carState = carStateService.getLastNotLeft(eventDto.car_number);
        if(carState == null){
            if(!carStateService.checkIsLastLeft(eventDto.car_number, eventDto.ip_address)){
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "Не найден запись о вьезде. Авто с гос. номером " + eventDto.car_number);

                properties.put("type", EventLogService.EventType.Deny);
                eventLogService.createEventLog(CarState.class.getSimpleName(), null, properties, "Не найден запись о вьезде. Авто с гос. номером " + eventDto.car_number);
            }
            return;
        }

        if(Parking.ParkingType.WHITELIST.equals(camera.getGate().getParking().getParkingType())){
            eventLogService.sendSocketMessage(ArmEventType.Photo, camera.getId(), eventDto.car_number, eventDto.car_picture);
            Boolean barrierResult = openGateBarrier(camera, properties);
            if(barrierResult){
                carStateService.createOUTState(eventDto.car_number, eventDto.event_time, camera, carState);
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "Выпускаем авто: Авто с гос. номером " + eventDto.car_number);

                properties.put("type", EventLogService.EventType.Allow);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "Выпускаем авто: Авто с гос. номером " + eventDto.car_number);
            }
        } else {
            Boolean isWhitelistCar = false;
            if(Parking.ParkingType.WHITELIST_PAYMENT.equals(camera.getGate().getParking().getParkingType())){
                if(carState != null && carState.getPaid() != null && !carState.getPaid()){
                    isWhitelistCar = true;
                }
            }

            if(!isWhitelistCar){
                PluginRegister ratePluginRegister = pluginService.getPluginRegister(StaticValues.ratePlugin);
                if(ratePluginRegister != null){
                    ObjectNode node = this.objectMapper.createObjectNode();
                    node.put("parkingId", camera.getGate().getParking().getId());
                    node.put("inDate", format.format(carState.getInTimestamp()));
                    node.put("outDate", format.format(eventDto.event_time));
                    node.put("cashlessPayment", carState.getCashlessPayment() != null ? carState.getCashlessPayment() : false);

                    JsonNode result = ratePluginRegister.execute(node);
                    BigDecimal rateResult = result.get("rateResult").decimalValue().setScale(2);

                    BigDecimal balance = BigDecimal.ZERO;
                    PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
                    if(billingPluginRegister != null){
                        ObjectNode billinNode = this.objectMapper.createObjectNode();
                        billinNode.put("command", "getCurrentBalance");
                        billinNode.put("plateNumber", carState.getCarNumber());
                        JsonNode billingResult = billingPluginRegister.execute(billinNode);
                        balance = billingResult.get("currentBalance").decimalValue().setScale(2);
                    }

                    if(balance.compareTo(rateResult) >= 0){
                        carState.setOutTimestamp(eventDto.event_time);
                        carState.setRateAmount(rateResult);
                        carStateService.save(carState);

                        BigDecimal subtractResult = balance.subtract(rateResult);
                        if(billingPluginRegister != null){
                            ObjectNode billingSubtractNode = this.objectMapper.createObjectNode();
                            billingSubtractNode.put("command", "decreaseCurrentBalance");
                            billingSubtractNode.put("amount", rateResult);
                            billingSubtractNode.put("plateNumber", carState.getCarNumber());
                            subtractResult = billingPluginRegister.execute(billingSubtractNode).get("currentBalance").decimalValue();
                        }
                        subtractResult = subtractResult.setScale(2);

                        Boolean barrierResult = openGateBarrier(camera, properties);
                        if(barrierResult){
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
                        }
                    } else {
                        eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "В проезде отказано: Не достаточно средств для списания оплаты за паркинг. Сумма к оплате: " + rateResult + " тенге. Баланс: " + balance +" тенге. В проезде отказано.");

                        properties.put("type", EventLogService.EventType.Deny);
                        eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "В проезде отказано: Не достаточно средств для списания оплаты за паркинг. Сумма к оплате: " + rateResult + " тенге. Баланс: " + balance +" тенге. В проезде отказано.");
                    }
                }
                else {
                    properties.put("type", EventLogService.EventType.Error);
                    eventLogService.createEventLog("Rate", null, properties, "Плагин вычисления стоимости парковки не найден или не запущен. Авто с гос. номером" + eventDto.car_number);
                }
            }  else {
                eventLogService.sendSocketMessage(ArmEventType.Photo, camera.getId(), eventDto.car_number, eventDto.car_picture);
                Boolean barrierResult = openGateBarrier(camera, properties);
                if(barrierResult){
                    carStateService.createOUTState(eventDto.car_number, eventDto.event_time, camera, carState);
                    eventLogService.sendSocketMessage(ArmEventType.CarEvent, camera.getId(), eventDto.car_number, "Пропускаем авто: Присуствовал в белом списке. Авто с гос. номером " + eventDto.car_number);

                    properties.put("type", EventLogService.EventType.Allow);
                    eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "Пропускаем авто: Присуствовал в белом списке. Авто с гос. номером " + eventDto.car_number);
                }
            }
        }
    }
}
