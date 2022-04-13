package kz.spt.app.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.app.job.StatusCheckJob;
import kz.spt.app.model.dto.GateStatusDto;
import kz.spt.app.service.BarrierService;
import kz.spt.app.service.BlacklistService;
import kz.spt.app.service.CameraService;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.model.*;
import kz.spt.lib.model.dto.CarEventDto;
import kz.spt.lib.service.*;
import kz.spt.lib.service.EventLogService.ArmEventType;
import kz.spt.lib.utils.StaticValues;
import kz.spt.lib.utils.Utils;
import lombok.extern.java.Log;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

@Log
@Service
public class CarEventServiceImpl implements CarEventService {

    private static Map<String, Long> concurrentHashMap = new ConcurrentHashMap<>();
    private final CarsService carsService;
    private final CameraService cameraService;
    private final EventLogService eventLogService;
    private final CarStateService carStateService;
    private final CarImageService carImageService;
    private final BarrierService barrierService;
    private final BlacklistService blacklistService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PluginService pluginService;
    private final QrPanelService qrPanelService;

    @Value("${parking.has.access.unknown.cases}")
    Boolean parkingHasAccessUnknownCases;

    @Value("${parking.only.register.cars}")
    Boolean parkingOnlyRegisterCars;

    private String dateFormat = "yyyy-MM-dd'T'HH:mm";
    private ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag(LocaleContextHolder.getLocale().toString().substring(0, 2)));

    public CarEventServiceImpl(CarsService carsService, CameraService cameraService, EventLogService eventLogService,
                               CarStateService carStateService, CarImageService carImageService,
                               BarrierService barrierService, BlacklistService blacklistService, PluginService pluginService, QrPanelService qrPanelService) {
        this.carsService = carsService;
        this.cameraService = cameraService;
        this.eventLogService = eventLogService;
        this.carStateService = carStateService;
        this.carImageService = carImageService;
        this.barrierService = barrierService;
        this.blacklistService = blacklistService;
        this.pluginService = pluginService;
        this.qrPanelService = qrPanelService;
    }

    @Override
    public boolean passCar(Long cameraId, String platenumber, String snapshot) throws Exception {
        Boolean barrierResult = false;
        if (platenumber != null) {
            Camera camera = cameraService.getCameraById(cameraId);
            if (camera != null) {
                String username = "";
                if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CurrentUser) {
                    CurrentUser currentUser = (CurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                    if (currentUser != null) {
                        username = currentUser.getUsername();
                    }
                }

                CarEventDto eventDto = new CarEventDto();
                eventDto.event_date_time = new Date();
                eventDto.car_number = platenumber;
                eventDto.ip_address = camera.getIp();
                eventDto.lp_rect = null;
                eventDto.lp_picture = null;
                eventDto.manualEnter = true;

                if (snapshot != null && !"".equals(snapshot) && !"undefined".equals(snapshot) && !"null".equals(snapshot)) {
                    eventDto.car_picture = snapshot;
                } else {
                    eventDto.car_picture = null;
                }
                SimpleDateFormat format = new SimpleDateFormat(dateFormat);

                Map<String, Object> properties = new HashMap<>();
                properties.put("carNumber", platenumber);
                properties.put("eventTime", format.format(new Date()));
                properties.put("cameraIp", camera.getIp());
                properties.put("gateName", camera.getGate().getName());
                properties.put("gateDescription", camera.getGate().getDescription());
                properties.put("gateType", camera.getGate().getGateType().toString());
                properties.put("type", EventLog.StatusType.Allow);

                if (eventDto.car_picture != null && !"null".equals(snapshot) && !"".equals(snapshot) && !"undefined".equals(snapshot) && !"data:image/jpg;base64,null".equals(snapshot)) {
                    String carImageUrl = carImageService.saveImage(eventDto.car_picture, eventDto.event_date_time, eventDto.car_number);
                    properties.put(StaticValues.carImagePropertyName, carImageUrl);
                    properties.put(StaticValues.carSmallImagePropertyName, carImageUrl.replace(StaticValues.carImageExtension, "") + StaticValues.carImageSmallAddon + StaticValues.carImageExtension);
                }

                String messageRu = "Ручной запуск Авто с гос. номером " + platenumber + ". Пользователь " + username + " инициировал ручной запуск на " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезд" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "выезд" : "въезд/выезд")) + " для " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName();
                String messageEn = "Manual pass. Car with license plate " + platenumber + ". User " + username + " initiated manual open gate for " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "pass" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName();
                eventLogService.sendSocketMessage(EventLogService.ArmEventType.CarEvent,
                        EventLog.StatusType.Success, camera.getId(),
                        platenumber,
                        messageRu,
                        messageEn);

                eventLogService.createEventLog(Gate.class.getSimpleName(),
                        camera.getGate().getId(),
                        properties,
                        messageRu,
                        messageEn);

                saveCarEvent(eventDto);
            }
        }
        return barrierResult;
    }

    @Override
    public void saveCarEvent(CarEventDto eventDto) throws Exception {

        SimpleDateFormat format = new SimpleDateFormat(StaticValues.dateFormatTZ);
        eventDto.car_number = eventDto.car_number.toUpperCase();

        Camera camera = cameraService.findCameraByIp(eventDto.ip_address);
        Map<String, Object> properties = new HashMap<>();
        properties.put("carNumber", eventDto.car_number);
        properties.put("eventTime", format.format(eventDto.event_date_time));
        properties.put("lp_rect", eventDto.lp_rect);
        properties.put("cameraIp", eventDto.ip_address);
        if(eventDto.region != null){
            properties.put("region", eventDto.region);
        }
        if(eventDto.vecihleType != null){
            properties.put("vecihleType", eventDto.vecihleType);
        }
        if(eventDto.car_model != null){
            properties.put("car_model", eventDto.car_model);
        }

        if (camera != null) {

            GateStatusDto gate = StatusCheckJob.findGateStatusDtoById(camera.getGate().getId());

            String secondCameraIp = (gate.frontCamera2 != null) ? (eventDto.ip_address.equals(gate.frontCamera.ip) ? gate.frontCamera2.ip : gate.frontCamera.ip) : null; // If there is two camera, then ignore second by timeout

            if(!eventDto.manualOpen){
                if (concurrentHashMap.containsKey(eventDto.ip_address) || (secondCameraIp != null && concurrentHashMap.containsKey(secondCameraIp))) {
                    Long timeDiffInMillis = System.currentTimeMillis() - (concurrentHashMap.containsKey(eventDto.ip_address) ? concurrentHashMap.get(eventDto.ip_address) : 0);
                    int timeout = (camera.getTimeout() == null ? 0 : camera.getTimeout() * 1000);
                    if (secondCameraIp != null) {
                        Long secondCameraTimeDiffInMillis = System.currentTimeMillis() - (concurrentHashMap.containsKey(secondCameraIp) ? concurrentHashMap.get(secondCameraIp) : 0);
                        timeDiffInMillis = timeDiffInMillis < secondCameraTimeDiffInMillis ? timeDiffInMillis : secondCameraTimeDiffInMillis;
                    }

                    if (timeDiffInMillis < timeout) { // If interval smaller than timeout then ignore else proceed
                        log.info("Ignored event from camera: " + eventDto.ip_address + " time: " + timeDiffInMillis);
                        return;
                    } else {
                        concurrentHashMap.put(eventDto.ip_address, System.currentTimeMillis());
                    }
                } else {
                    concurrentHashMap.put(eventDto.ip_address, System.currentTimeMillis());
                }
            }

            log.info("handling event from camera: " + eventDto.ip_address + " for car: " + eventDto.car_number);

            properties.put("gateName", camera.getGate().getName());
            properties.put("gateId", camera.getGate().getId());
            properties.put("gateDescription", camera.getGate().getDescription());
            properties.put("gateType", camera.getGate().getGateType().toString());

            if (eventDto.car_picture != null && !"".equals(eventDto.car_picture) && !"null".equals(eventDto.car_picture) && !"undefined".equals(eventDto.car_picture) && !"data:image/jpg;base64,null".equals(eventDto.car_picture)) {
                String carImageUrl = carImageService.saveImage(eventDto.car_picture, eventDto.event_date_time, eventDto.car_number);
                properties.put(StaticValues.carImagePropertyName, carImageUrl);
                properties.put(StaticValues.carSmallImagePropertyName, carImageUrl.replace(StaticValues.carImageExtension, "") + StaticValues.carImageSmallAddon + StaticValues.carImageExtension);
            }

            log.info("Camera belongs to gate: " + gate.gateId);
            if (gate.frontCamera != null && gate.frontCamera.id == camera.getId()) {
                gate.frontCamera.carEventDto = eventDto;
                gate.frontCamera.properties = properties;
            } else if (gate.backCamera != null && gate.backCamera.id == camera.getId()) {
                gate.backCamera.carEventDto = eventDto;
                gate.backCamera.properties = properties;
            }

            if (eventDto.manualOpen || isAllow(eventDto, camera, properties, gate)) {
                log.info("Gate type: " + camera.getGate().getGateType());
                createNewCarEvent(camera, eventDto, properties);

                if (Gate.GateType.REVERSE.equals(camera.getGate().getGateType())) {
                    handleCarReverseInEvent(eventDto, camera, gate, properties, format);
                } else if (Gate.GateType.IN.equals(camera.getGate().getGateType())) {
                    handleCarInEvent(eventDto, camera, gate, properties, format);
                } else if (Gate.GateType.OUT.equals(camera.getGate().getGateType())) {
                    handleCarOutEvent(eventDto, camera, gate, properties, format);
                }
            } else {
                if (gate.lastClosedTime != null) {
                    log.info("last closed date diff: " + (System.currentTimeMillis() - gate.lastClosedTime));
                } else {
                    log.info("last closed date is null");
                }
            }
        } else {
            properties.put("type", EventLog.StatusType.Error);
            eventLogService.createEventLog(null, null, properties, "Зафиксирован новый номер авто " + " " + eventDto.car_number + " от неизвестной камеры с ip " + eventDto.ip_address, "Identified new car with number " + " " + eventDto.car_number + " from unknown camera with ip " + eventDto.ip_address);
        }
    }

    private boolean isAllow(CarEventDto carEvent, Camera camera, Map<String, Object> properties, GateStatusDto gate) {
        if (blacklistService.findByPlate(carEvent.car_number).isPresent()) {
            properties.put("type", EventLog.StatusType.Deny);
            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Deny, camera.getId(), carEvent.car_number, "В проезде отказано: Авто с гос. номером " + carEvent.car_number + " в черном списке", "Not allowed to enter: Car with number " + carEvent.car_number + " in blacklist");
            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "В проезде отказано: Авто с гос. номером " + carEvent.car_number + " в черном списке", "Not allowed to enter: Car with number " + carEvent.car_number + " in blacklist");
            return false;
        }
        return gate.lastClosedTime == null || System.currentTimeMillis() - gate.lastClosedTime > 5000; ////если последний раз закрыли больше 5 секунды
    }

    @Override
    public void handleTempCarEvent(MultipartFile file, String json) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(json);

        String ip_address = jsonNode.get("data").get("camera_id").asText();
        log.info(jsonNode.get("data").get("camera_id").asText() + " " + jsonNode.get("data").get("camera_id").asText());

        JsonNode result = ((ArrayNode) jsonNode.get("data").get("results")).get(0);
        String car_number = result.get("plate").asText().toUpperCase();
        String region = null;
        String vecihleType = null;
        if (result.has("region") && result.get("region").has("code")) {
            region = result.get("region").get("code").asText();
        }
        if (result.has("vehicle") && result.get("vehicle").has("type")) {
            vecihleType = result.get("vehicle").get("type").asText();
        }

        String base64 = null;
        String base64_lp = null;
        try {
            base64 = StringUtils.newStringUtf8(Base64.encodeBase64(file.getInputStream().readAllBytes(), false));
            //base64_lp = StringUtils.newStringUtf8(Base64.encodeBase64(file2.getInputStream().readAllBytes(), false));
        } catch (IOException e) {
            e.printStackTrace();
        }

        CarEventDto eventDto = new CarEventDto();
        eventDto.car_number = car_number;
        eventDto.event_date_time = new Date();
        eventDto.ip_address = ip_address;
        eventDto.car_picture = base64;
        eventDto.lp_picture = base64_lp;
        eventDto.region = region;
        eventDto.vecihleType = vecihleType;
        saveCarEvent(eventDto);
    }

    private void handleCarReverseInEvent(CarEventDto eventDto, Camera camera, GateStatusDto gate, Map<String, Object> properties, SimpleDateFormat format) throws Exception {
        JsonNode whitelistCheckResults = getWhiteLists(camera.getGate().getParking().getId(), eventDto.car_number, eventDto.event_date_time, format, properties);
        boolean hasAccess = checkSimpleWhiteList(eventDto, camera, properties, whitelistCheckResults);
        if (hasAccess) {
            boolean openResult = barrierService.openBarrier(camera.getGate().getBarrier(), properties);
            if (openResult) {
                gate.gateStatus = GateStatusDto.GateStatus.Open;
                gate.sensor1 = GateStatusDto.SensorStatus.Triggerred;
                gate.sensor2 = GateStatusDto.SensorStatus.WAIT;
                gate.directionStatus = GateStatusDto.DirectionStatus.FORWARD;
                gate.lastTriggeredTime = System.currentTimeMillis();
            }
        }
    }

    private void handleCarInEvent(CarEventDto eventDto, Camera camera, GateStatusDto gate, Map<String, Object> properties, SimpleDateFormat format) throws Exception {
        boolean hasAccess;
        JsonNode whitelistCheckResults = null;
        Boolean enteredFromThisSecondsBefore = false;

        // проверить если машины выезжала или заезжала 20 секунд минуты назад
        Calendar now = Calendar.getInstance();
        now.add(Calendar.SECOND, -20);
        Boolean hasLeft = carStateService.getIfHasLastFromOtherCamera(eventDto.car_number, eventDto.ip_address, now.getTime());
        if (hasLeft) {
            return;
        }

        CarState carState = carStateService.getLastNotLeft(eventDto.car_number);
        if (carState != null) {
            Calendar current = Calendar.getInstance();
            if(carState.getParking().equals(camera.getGate().getParking()) && (current.getTime().getTime() - carState.getInTimestamp().getTime() <= 5*60*1000)){
                enteredFromThisSecondsBefore = true;
            } else {
                carStateService.createOUTManual(eventDto.car_number, new Date(), carState); // Принудительная закрытие предыдущей сессий
                carState = null;
            }
        }

        if(!enteredFromThisSecondsBefore){
            // Проверка долга
            Boolean hasDebt = false;
            BigDecimal debt = BigDecimal.ZERO;
            if(!eventDto.manualEnter && (Parking.ParkingType.PAYMENT.equals(camera.getGate().getParking().getParkingType()) || Parking.ParkingType.WHITELIST_PAYMENT.equals(camera.getGate().getParking().getParkingType()))){
                PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
                if (billingPluginRegister != null) {
                    ObjectNode billinNode = this.objectMapper.createObjectNode();
                    billinNode.put("command", "getCurrentBalance");
                    billinNode.put("plateNumber", eventDto.car_number);
                    JsonNode billingResult = billingPluginRegister.execute(billinNode);
                    debt = billingResult.get("currentBalance").decimalValue().setScale(2);
                    if(debt.compareTo(BigDecimal.ZERO) == -1){
                        hasDebt = true;
                    }
                }
            }

            if(hasDebt){
                properties.put("type", EventLog.StatusType.Debt);
                String descriptionRu = "В проезде отказано: Авто " + eventDto.car_number + " имеет задолженность " + debt;
                String descriptionEn = "Not allowed to enter: Car " + eventDto.car_number + " is in debt " + debt;
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Debt, camera.getId(), eventDto.car_number, descriptionRu, descriptionEn);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, descriptionRu, descriptionEn);
                hasAccess = false;
            } else if (Parking.ParkingType.PAYMENT.equals(camera.getGate().getParking().getParkingType())) {
                if (carState == null) {
                    hasAccess = true;
                } else {
                    properties.put("type", EventLog.StatusType.Debt);
                    eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Debt, camera.getId(), eventDto.car_number, "В проезде отказано: Авто " + eventDto.car_number + " имеет задолженность", "Not allowed to enter: Car " + eventDto.car_number + " is in debt");
                    eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "В проезде отказано: Авто " + eventDto.car_number + " имеет задолженность", "Not allowed to enter: Car " + eventDto.car_number + " is in debt");
                    hasAccess = false;
                }
            } else if (Parking.ParkingType.PREPAID.equals(camera.getGate().getParking().getParkingType())) {
                if(carState == null){
                    carStateService.createINState(eventDto.car_number,eventDto.event_date_time,camera,true,null,properties.containsKey(StaticValues.carSmallImagePropertyName) ? properties.get(StaticValues.carSmallImagePropertyName).toString() : null);
                    carState = carStateService.getLastNotLeft(eventDto.car_number);
                }
                JsonNode currentBalanceResult = getCurrentBalance(eventDto.car_number, properties);
                BigDecimal balance = currentBalanceResult.get("currentBalance").decimalValue();

                JsonNode prepaidValueResult = getPrepaidValue(camera.getGate().getParking().getId(), properties);
                BigDecimal prepaid = BigDecimal.valueOf(prepaidValueResult.get("prepaidValue").longValue());

                if ((balance != null && prepaid != null) && balance.subtract(prepaid).compareTo(BigDecimal.ZERO) >= 0) {
                    decreaseBalance(eventDto.car_number, camera.getGate().getParking().getName(), carState.getId(), prepaid, properties);
                    hasAccess = true;
                } else {
                    properties.put("type", EventLog.StatusType.Deny);
                    eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Deny, camera.getId(), eventDto.car_number, "В проезде отказано: Не достаточно средств для списания оплаты за паркинг. Сумма к оплате: " + prepaid + ". Баланс: " + balance, "Not allowed to exit: Not enough balance to pay for parking. Total sum to pay: " + prepaid + ". Balance: " + balance);
                    eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "В проезде отказано: Не достаточно средств для списания оплаты за паркинг. Сумма к оплате: " + prepaid + ". Баланс: " + balance, "Not allowed to exit: Not enough balance to pay for parking. Total sum to pay: " + prepaid + ". Balance: " + balance);
                    hasAccess = false;
                }
            } else {
                whitelistCheckResults = getWhiteLists(camera.getGate().getParking().getId(), eventDto.car_number, eventDto.event_date_time, format, properties);
                if (gate.isSimpleWhitelist) {
                    log.info("Simple whitelist check");
                    hasAccess = checkSimpleWhiteList(eventDto, camera, properties, whitelistCheckResults);
                } else {
                    log.info("Complex whitelist check");
                    if (carState == null) {
                        log.info("not last entered not left");
                        if (Parking.ParkingType.WHITELIST.equals(camera.getGate().getParking().getParkingType())) {
                            hasAccess = checkWhiteList(eventDto, camera, properties, whitelistCheckResults);
                        } else {
                            hasAccess = true;
                        }
                    } else {
                        log.info("last entered not left");
                        if (Parking.ParkingType.WHITELIST.equals(camera.getGate().getParking().getParkingType())) {
                            hasAccess = checkWhiteList(eventDto, camera, properties, whitelistCheckResults);
                        } else {
                            properties.put("type", EventLog.StatusType.Debt);
                            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Debt, camera.getId(), eventDto.car_number, "В проезде отказано: Авто " + eventDto.car_number + " имеет задолженность", "Not allowed to enter: Car " + eventDto.car_number + " is in debt");
                            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "В проезде отказано: Авто " + eventDto.car_number + " имеет задолженность", "Not allowed to enter: Car " + eventDto.car_number + " is in debt");
                            hasAccess = false;
                        }
                    }
                }
            }
        } else {
            hasAccess = true;
        }

        log.info("enteredFromThisSecondsBefore: " + enteredFromThisSecondsBefore);
        log.info("hasAccess: " + hasAccess);

        if (hasAccess) {
            boolean openResult = barrierService.openBarrier(camera.getGate().getBarrier(), properties);
            if (openResult) {
                gate.gateStatus = GateStatusDto.GateStatus.Open;
                gate.sensor1 = GateStatusDto.SensorStatus.Triggerred;
                gate.sensor2 = GateStatusDto.SensorStatus.WAIT;
                gate.directionStatus = GateStatusDto.DirectionStatus.FORWARD;
                gate.lastTriggeredTime = System.currentTimeMillis();
                if (!gate.isSimpleWhitelist && !enteredFromThisSecondsBefore) {
                    saveCarInState(eventDto, camera, whitelistCheckResults, properties);
                }
            }
        }
    }

    private void saveCarInState(CarEventDto eventDto, Camera camera, JsonNode whitelistCheckResults, Map<String, Object> properties) {
        if (Parking.ParkingType.WHITELIST.equals(camera.getGate().getParking().getParkingType())) {
            properties.put("type", EventLog.StatusType.Allow);
            carStateService.createINState(eventDto.car_number, eventDto.event_date_time, camera, false, whitelistCheckResults != null ? whitelistCheckResults.toString() : null,properties.containsKey(StaticValues.carSmallImagePropertyName) ? properties.get(StaticValues.carSmallImagePropertyName).toString() : null);
            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.car_number, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " в рамках белого листа", "Permitted: Car with number " + eventDto.car_number + " from white list");
            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " в рамках белого листа", "Permitted: Car with number " + eventDto.car_number + " from white list");
        } else if(Parking.ParkingType.PREPAID.equals(camera.getGate().getParking().getParkingType())){
            properties.put("type", EventLog.StatusType.Allow);
            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.car_number, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " по предоплате", "Permitted: Car with number " + eventDto.car_number + " on prepaid basis");
            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " по предоплате", "Permitted: Car with number " + eventDto.car_number + " on prepaid basis");
        }else {
            if (whitelistCheckResults == null) {
                properties.put("type", EventLog.StatusType.Allow);
                carStateService.createINState(eventDto.car_number, eventDto.event_date_time, camera, true, null,properties.containsKey(StaticValues.carSmallImagePropertyName) ? properties.get(StaticValues.carSmallImagePropertyName).toString() : null);
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), (eventDto.region != null ? Utils.convertRegion(eventDto.region) + " " : "") + eventDto.car_number + (eventDto.vecihleType != null ? "["+ eventDto.vecihleType +"]" :""), "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " на платной основе", "Permitted: Car with number " + eventDto.car_number + " on paid basis");
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " на платной основе", "Permitted: Car with number " + eventDto.car_number + " on paid basis");
            } else {
                ArrayNode whitelistCheckResultArray = (ArrayNode) whitelistCheckResults;
                boolean hasAccess = false;
                JsonNode nodeDetails = null;
                Iterator<JsonNode> iterator = whitelistCheckResultArray.iterator();
                while (iterator.hasNext()) {
                    JsonNode node = iterator.next();
                    if (!node.has("exceedPlaceLimit") || (node.has("exceedPlaceLimit") && !node.get("exceedPlaceLimit").booleanValue())) {
                        hasAccess = true;
                    } else {
                        nodeDetails = node;
                    }
                }
                if (hasAccess) {
                    properties.put("type", EventLog.StatusType.Allow);
                    carStateService.createINState(eventDto.car_number, eventDto.event_date_time, camera, false, whitelistCheckResults != null ? whitelistCheckResults.toString() : null,properties.containsKey(StaticValues.carSmallImagePropertyName) ? properties.get(StaticValues.carSmallImagePropertyName).toString() : null);
                    eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.car_number, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " в рамках белого листа", "Permitted: Car with number " + eventDto.car_number + " from white list");
                    eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " в рамках белого листа", "Permitted: Car with number " + eventDto.car_number + " from white list");
                } else {
                    properties.put("type", EventLog.StatusType.Allow);
                    carStateService.createINState(eventDto.car_number, eventDto.event_date_time, camera, true, whitelistCheckResults != null ? whitelistCheckResults.toString() : null,properties.containsKey(StaticValues.carSmallImagePropertyName) ? properties.get(StaticValues.carSmallImagePropertyName).toString() : null);
                    eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.car_number, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " на платной основе. Все места для группы " + nodeDetails.get("groupName").textValue() + " заняты следующим списком " + nodeDetails.get("placeOccupiedCars").toString(), "Permitted: Car with license number " + eventDto.car_number + " on paid basis. " + (nodeDetails.has("placeName") ? nodeDetails.get("placeName") + " spot " : "Все места ") + "for the group " + nodeDetails.get("groupName").textValue() + " taken by the next list " + nodeDetails.get("placeOccupiedCars").toString());
                    eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " на платной основе. " + (nodeDetails.has("placeName") ? nodeDetails.get("placeName") + " место " : "Все места ") + "для группы " + nodeDetails.get("groupName").textValue() + " заняты следующим списком " + nodeDetails.get("placeOccupiedCars").toString(), "Permitted: Car with license number " + eventDto.car_number + " on paid basis. " + (nodeDetails.has("placeName") ? nodeDetails.get("placeName") + " spot " : "Все места ") + "for the group " + nodeDetails.get("groupName").textValue() + " taken by the next list " + nodeDetails.get("placeOccupiedCars").toString());
                }
            }
        }
    }

    private boolean checkWhiteList(CarEventDto eventDto, Camera camera, Map<String, Object> properties, JsonNode whitelistCheckResults) throws Exception {
        if (whitelistCheckResults != null) {
            ArrayNode whitelistCheckResultArray = (ArrayNode) whitelistCheckResults;
            JsonNode node = null;
            boolean hasAccess = false;
            Iterator<JsonNode> iterator = whitelistCheckResultArray.iterator();
            while (iterator.hasNext()) {
                node = iterator.next();

                if (!node.has("exceedPlaceLimit") || (node.has("exceedPlaceLimit") && !node.get("exceedPlaceLimit").booleanValue())) {
                    hasAccess = true;
                }
            }
            if (hasAccess) {
                return true;
            } else if (checkBooking(eventDto.car_number)) {
                properties.put("type", EventLog.StatusType.Allow);
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.car_number, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " имеется валидный пропуск.", "Allow entrance: Car with plate number " + eventDto.car_number + " has valid booking.");
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " имеется валидный пропуск.", "Allow entrance: Car with plate number " + eventDto.car_number + " has valid booking.");
                return true;
            } else {
                properties.put("type", EventLog.StatusType.Deny);
                String normalList = node.get("placeOccupiedCars").toString().substring(2).replaceAll("\"","").replaceFirst("]", "");
                String description = "В проезде отказано: Все места в для группы " + node.get("groupName").textValue() + " заняты следующим списком " + normalList + ". Авто " + eventDto.car_number;
                String description_en =  "Not allowed: All parking lost for the groups " + node.get("groupName").textValue() + " were taken by the next list " + normalList + ". Car " + eventDto.car_number;
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Deny, camera.getId(), eventDto.car_number, description, description_en);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, description, description_en);
                return false;
            }
        } else if (checkBooking(eventDto.car_number)) {
            properties.put("type", EventLog.StatusType.Allow);
            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.car_number, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " имеется валидный пропуск.", "Allow entrance: Car with plate number " + eventDto.car_number + " has valid booking.");
            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " имеется валидный пропуск.", "Allow entrance: Car with plate number " + eventDto.car_number + " has valid booking.");
            return true;
        } else {
            properties.put("type", EventLog.StatusType.Deny);
            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Deny, camera.getId(), eventDto.car_number, "В проезде отказано: Авто не найдено в белом листе " + eventDto.car_number, "Not allowed to enter: Car not found in white list " + eventDto.car_number);
            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "В проезде отказано: Авто не найдено в белом листе " + eventDto.car_number, "Not allowed to enter: Car not found in white list " + eventDto.car_number);
            return false;
        }
    }

    private boolean checkBooking(String platenumber) throws Exception {
        PluginRegister bookingPluginRegister = pluginService.getPluginRegister(StaticValues.bookingPlugin);
        if (bookingPluginRegister != null) {
            ObjectNode node = this.objectMapper.createObjectNode();
            node.put("platenumber", platenumber);
            node.put("command", "checkBooking");
            JsonNode result = bookingPluginRegister.execute(node);
            return result.get("bookingResult").booleanValue();
        } else {
            return false;
        }
    }

    private boolean checkSimpleWhiteList(CarEventDto eventDto, Camera camera, Map<String, Object> properties, JsonNode whitelistCheckResults) {

        boolean hasAccess = false;
        if (whitelistCheckResults != null) {
            ArrayNode whitelistCheckResultArray = (ArrayNode) whitelistCheckResults;
            JsonNode customDetails = null;
            Iterator<JsonNode> iterator = whitelistCheckResultArray.iterator();
            while (iterator.hasNext()) {
                JsonNode node = iterator.next();
                if (!node.has("exceedPlaceLimit") || (node.has("exceedPlaceLimit") && !node.get("exceedPlaceLimit").booleanValue())) {
                    hasAccess = true;
                } else {
                    customDetails = node;
                }
            }

            if (hasAccess) {
                properties.put("type", EventLog.StatusType.Allow);
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.car_number, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " в рамках белого листа", "Permitted: Car with number " + eventDto.car_number + " from white list");
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " в рамках белого листа", "Permitted: Car with number " + eventDto.car_number + " from white list");
            } else {
                properties.put("type", EventLog.StatusType.Deny);
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Deny, camera.getId(), eventDto.car_number, "В проезде отказано: Все места в для группы " + customDetails.get("groupName").textValue() + " заняты следующим списком " + customDetails.get("placeOccupiedCars").toString() + ". Авто " + eventDto.car_number, "Not allowed: All spots for the group " + customDetails.get("groupName").textValue() + " taken by the next list " + customDetails.get("placeOccupiedCars").toString() + ". Car " + eventDto.car_number);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "В проезде отказано: Все места в для группы " + customDetails.get("groupName").textValue() + " заняты следующим списком " + customDetails.get("placeOccupiedCars").toString() + ". Авто " + eventDto.car_number, "Not allowed: All spots for the group " + customDetails.get("groupName").textValue() + " taken by the next list " + customDetails.get("placeOccupiedCars").toString() + ". Car " + eventDto.car_number);
            }
        } else {
            properties.put("type", EventLog.StatusType.Deny);
            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Deny, camera.getId(), eventDto.car_number, "В проезде отказано: Авто не найдено в белом листе " + eventDto.car_number, "Not allowed: Car not found in white list " + eventDto.car_number);
            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "В проезде отказано: Авто не найдено в белом листе " + eventDto.car_number, "Not allowed: Car not found in white list " + eventDto.car_number);
        }
        return hasAccess;
    }

    private JsonNode getCurrentBalance(String car_number, Map<String, Object> properties) throws Exception {
        ObjectNode node = this.objectMapper.createObjectNode();
        JsonNode currentBalanceResult = null;
        node.put("command", "getCurrentBalance");
        node.put("plateNumber", car_number);

        PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
        if (billingPluginRegister != null) {
            currentBalanceResult = billingPluginRegister.execute(node);
        } else {
            properties.put("type", EventLog.StatusType.Error);
            eventLogService.createEventLog("Balance", null, properties, "Плагин биллинга не найден или не запущен. Авто с гос. номером " + car_number,
                    "Plugin for billing not found or not launched. Car with license plate " + car_number);
        }
        return currentBalanceResult;
    }

    private JsonNode decreaseBalance(String carNumber, String parkingName, Long carStateId, BigDecimal amaount, Map<String, Object> properties) throws Exception {
        ObjectNode node = this.objectMapper.createObjectNode();
        JsonNode decreaseBalanceResult = null;
        node.put("command", "decreaseCurrentBalance");
        node.put("amount", amaount);
        node.put("plateNumber", carNumber);
        node.put("parkingName", parkingName);
        node.put("reason", "Оплата паркинга " + parkingName);
        node.put("reasonEn", "Payment for parking " + parkingName);
        node.put("carStateId", carStateId);

        PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
        if (billingPluginRegister != null) {
            decreaseBalanceResult = billingPluginRegister.execute(node);
        } else {
            properties.put("type", EventLog.StatusType.Error);
            eventLogService.createEventLog("Balance", null, properties, "Плагин биллинга не найден или не запущен. Авто с гос. номером " + carNumber,
                    "Plugin for billing not found or not launched. Car with license plate " + carNumber);
        }
        return decreaseBalanceResult;
    }

    private JsonNode getPrepaidValue(Long parkingId, Map<String, Object> properties) throws Exception {
        ObjectNode node = this.objectMapper.createObjectNode();
        JsonNode prepaidValueResult = null;
        node.put("command", "getPrepaidValue");
        node.put("parkingId", parkingId);

        PluginRegister ratePluginRegister = pluginService.getPluginRegister(StaticValues.ratePlugin);
        if (ratePluginRegister != null) {
            prepaidValueResult = ratePluginRegister.execute(node);
        } else {
            properties.put("type", EventLog.StatusType.Error);
            eventLogService.createEventLog("ParkingRate", null, properties, "Плагин тарифа не найден или не запущен",
                    "Plugin for rate not found or not launched");
        }
        return prepaidValueResult;
    }

    private JsonNode getWhiteLists(Long parkingId, String car_number, Date event_time, SimpleDateFormat format, Map<String, Object> properties) throws Exception {
        ObjectNode node = this.objectMapper.createObjectNode();
        JsonNode whitelistCheckResult = null;
        node.put("parkingId", parkingId);
        node.put("car_number", car_number);
        node.put("event_time", format.format(event_time));

        PluginRegister whitelistPluginRegister = pluginService.getPluginRegister(StaticValues.whitelistPlugin);
        if (whitelistPluginRegister != null) {
            JsonNode result = whitelistPluginRegister.execute(node);
            whitelistCheckResult = result.get("whitelistCheckResult");
        }
        return whitelistCheckResult;
    }

    private void handleCarOutEvent(CarEventDto eventDto, Camera camera, GateStatusDto gate, Map<String, Object> properties, SimpleDateFormat format) throws Exception {
        boolean hasAccess;
        CarState carState = null;
        boolean isWhitelistCar = false;
        BigDecimal balance = BigDecimal.ZERO;
        BigDecimal rateResult = null;
        BigDecimal zerotouchValue = null;
        StaticValues.CarOutBy carOutBy = null;
        Boolean leftFromThisSecondsBefore = false;

        if(gate.notControlBarrier != null && gate.notControlBarrier){
            carState = carStateService.getLastNotLeft(eventDto.car_number);
            carOutBy = StaticValues.CarOutBy.REGISTER;
            saveCarOutState(eventDto, camera, carState, properties, isWhitelistCar, balance, rateResult, zerotouchValue, format, carOutBy);
            return;
        }

        try {
            qrPanelService.clear(camera.getGate());
        } catch (Exception ex) {
            log.log(Level.WARNING, "Error while clearing qrpanel for gate " + gate.gateName);
        }
        // проверить если машины выезжала или заезжала 20 секунд назад
        Calendar now = Calendar.getInstance();
        now.add(Calendar.SECOND, -20);
        Boolean hasLeft = carStateService.getIfHasLastFromOtherCamera(eventDto.car_number, eventDto.ip_address, now.getTime());
        if (hasLeft) {
            return;
        }

        if (gate.isSimpleWhitelist) {
            carOutBy = StaticValues.CarOutBy.WHITELIST;
            hasAccess = true;
        } else {
            carState = carStateService.getLastNotLeft(eventDto.car_number);
            if (carState == null) {
                now = Calendar.getInstance();
                now.add(Calendar.MINUTE, -5);
                leftFromThisSecondsBefore = carStateService.getIfHasLastFromThisCamera(eventDto.car_number, eventDto.ip_address, now.getTime()); // Если выезжал 5 минут назад но не заезжал
                if (leftFromThisSecondsBefore) {
                    hasAccess = true;
                } else {
                    if (Parking.ParkingType.WHITELIST.equals(camera.getGate().getParking().getParkingType())) {
                        properties.put("type", EventLog.StatusType.Allow);
                        eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.car_number, "Не найден запись о въезде. Авто с гос. номером " + eventDto.car_number + ". Для белого списка выезд разрешен.", "Entering record not found. Car with license plate " + eventDto.car_number + ". For white list exit is allowed");
                        eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, "Не найден запись о въезде. Авто с гос. номером " + eventDto.car_number + ". Для белого списка выезд разрешен.", "Entering record not found. Car with license plate " + eventDto.car_number + ". For white list exit is allowed");
                        carOutBy = StaticValues.CarOutBy.WHITELIST;
                        hasAccess = true;
                    } else if (Parking.ParkingType.PREPAID.equals(camera.getGate().getParking().getParkingType())) {
                        properties.put("type", EventLog.StatusType.Allow);
                        eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.car_number, "По пердоплате выезд разрешен.. Авто с гос. номером " + eventDto.car_number, "For prepaid exit is allowed. Car with license plate " + eventDto.car_number);
                        eventLogService.createEventLog(CarState.class.getSimpleName(), null, properties, "По пердоплате выезд разрешен.. Авто с гос. номером " + eventDto.car_number, "For prepaid exit is allowed. Car with license plate " + eventDto.car_number);
                        carOutBy = StaticValues.CarOutBy.PREPAID;
                        hasAccess = true;
                    } else if(parkingOnlyRegisterCars){
                        carOutBy = StaticValues.CarOutBy.REGISTER;
                        hasAccess = true;
                    } else if (parkingHasAccessUnknownCases) {
                        properties.put("type", EventLog.StatusType.Allow);
                        eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Deny, camera.getId(), eventDto.car_number, "Не найден запись о въезде. Авто с гос. номером " + eventDto.car_number + ". Для этого паркинга выезд разрешен.", "No record found about entering. Car with license number " + eventDto.car_number + ". For this parking exit is allowed");
                        eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, "Не найден запись о въезде. Авто с гос. номером " + eventDto.car_number + ". Для этого паркинга выезд разрешен.", "No record found about entering. Car with license number " + eventDto.car_number + ". For this parking exit is allowed");
                        hasAccess = true;
                    } else {
                        ArrayNode whitelistCheckResultArray = (ArrayNode) getWhiteLists(camera.getGate().getParking().getId(), eventDto.car_number, new Date(), format, properties);
                        if(whitelistCheckResultArray != null && whitelistCheckResultArray.size() > 0){
                            properties.put("type", EventLog.StatusType.Allow);
                            String description = "Не найден запись о въезде. Авто с гос. номером " + eventDto.car_number + ". Для белого листа выезд разрешен.";
                            String descriptionEn = "No record found about entering. Car with license number " + eventDto.car_number + ". For white list exit is allowed";
                            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.car_number, description, descriptionEn);
                            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,  description, descriptionEn);
                            carOutBy = StaticValues.CarOutBy.WHITELIST;
                            hasAccess = true;
                        } else {
                            properties.put("type", EventLog.StatusType.Deny);
                            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Deny, camera.getId(), eventDto.car_number, "Не найден запись о въезде. Авто с гос. номером " + eventDto.car_number + ". Для платного паркинга выезд запрещен.", "No record found about entering. Car with license number " + eventDto.car_number + ". For paid parking, exit is prohibited");
                            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, "Не найден запись о въезде. Авто с гос. номером " + eventDto.car_number + ". Для платного паркинга выезд запрещен.", "No record found about entering. Car with license number " + eventDto.car_number + ". For paid parking, exit is prohibited");
                            hasAccess = false;
                        }
                    }
                }
            } else {
                CarState lastLeft = carStateService.getIfLastLeft(eventDto.car_number, eventDto.ip_address);
                if (lastLeft == null || System.currentTimeMillis() - lastLeft.getOutTimestamp().getTime() > 1000 * 60) { // Повторное прием фотособытии на выезд и если выехал 1 мин назад
                    if(Parking.ParkingType.PREPAID.equals(camera.getGate().getParking().getParkingType())){
                        hasAccess = true;
                        carOutBy = StaticValues.CarOutBy.PREPAID;
                    }else{
                        if (Parking.ParkingType.WHITELIST_PAYMENT.equals(camera.getGate().getParking().getParkingType()) && carState.getPaid() != null && !carState.getPaid()) {
                            isWhitelistCar = true;
                        }
                        if (isWhitelistCar || Parking.ParkingType.WHITELIST.equals(camera.getGate().getParking().getParkingType())) {
                            carOutBy = StaticValues.CarOutBy.WHITELIST;
                            hasAccess = true;
                        } else if(hasValidAbonoment(eventDto.car_number, carState.getParking().getId())){ // Проверяем абономент, если валидна выпускаем
                            carOutBy = StaticValues.CarOutBy.ABONOMENT;
                            hasAccess = true;
                        } else if(parkingOnlyRegisterCars){
                            carOutBy = StaticValues.CarOutBy.REGISTER;
                            hasAccess = true;
                        } else {
                            PluginRegister ratePluginRegister = pluginService.getPluginRegister(StaticValues.ratePlugin);
                            if (ratePluginRegister != null) {
                                ObjectNode ratePluginNode = this.objectMapper.createObjectNode();
                                ratePluginNode.put("parkingId", camera.getGate().getParking().getId());
                                ratePluginNode.put("inDate", format.format(carState.getInTimestamp()));
                                ratePluginNode.put("outDate", format.format(eventDto.event_date_time));
                                ratePluginNode.put("cashlessPayment", carState.getCashlessPayment() != null ? carState.getCashlessPayment() : true);
                                ratePluginNode.put("isCheck", false);
                                ratePluginNode.put("paymentsJson", carState.getPaymentJson());

                                JsonNode ratePluginResult = ratePluginRegister.execute(ratePluginNode);
                                rateResult = ratePluginResult.get("rateResult").decimalValue().setScale(2);

                                if (rateResult == null) {
                                    properties.put("type", EventLog.StatusType.Error);
                                    String descriptionRu = "Ошибка расчета плагина вычисления стоимости парковки. Авто с гос. номером " + eventDto.car_number;
                                    String descriptionEn = "Error calculating the parking cost calculation plugin. Car with number " + eventDto.car_number;
                                    eventLogService.createEventLog("Rate", null, properties, descriptionRu, descriptionEn);
                                    eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Deny, camera.getId(), eventDto.car_number, descriptionRu, descriptionEn);
                                    hasAccess = false;
                                } else if (BigDecimal.ZERO.compareTo(rateResult) == 0) {
                                    carOutBy = StaticValues.CarOutBy.PAYMENT_PROVIDER;
                                    hasAccess = true;
                                } else {
                                    PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
                                    if (billingPluginRegister != null) {
                                        ObjectNode billinNode = this.objectMapper.createObjectNode();
                                        billinNode.put("command", "getCurrentBalance");
                                        billinNode.put("plateNumber", carState.getCarNumber());
                                        JsonNode billingResult = billingPluginRegister.execute(billinNode);
                                        balance = billingResult.get("currentBalance").decimalValue().setScale(2);

                                        if (balance.compareTo(rateResult) >= 0) {
                                            carOutBy = StaticValues.CarOutBy.PAYMENT_PROVIDER;
                                            hasAccess = true;
                                        } else {
                                            PluginRegister zerotouchPluginRegister = pluginService.getPluginRegister(StaticValues.zerotouchPlugin);
                                            ObjectNode zerotouchRequestNode = this.objectMapper.createObjectNode();
                                            if (zerotouchPluginRegister != null) {
                                                zerotouchValue = rateResult.subtract(balance);
                                                zerotouchRequestNode.put("command", "checkZeroTouch");
                                                zerotouchRequestNode.put("plateNumber", carState.getCarNumber());
                                                zerotouchRequestNode.put("carStateId", carState.getId());
                                                zerotouchRequestNode.put("rate", zerotouchValue);
                                                JsonNode zerotouchResult = zerotouchPluginRegister.execute(zerotouchRequestNode);
                                                if (zerotouchResult.has("zeroTouchResult") && zerotouchResult.get("zeroTouchResult").booleanValue()) {
                                                    carOutBy = StaticValues.CarOutBy.ZERO_TOUCH;
                                                    hasAccess = true;
                                                } else {
                                                    hasAccess = false;
                                                }
                                            } else {
                                                hasAccess = false;
                                            }
                                        }
                                        if (!hasAccess) {
                                            properties.put("type", EventLog.StatusType.Deny);
                                            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Deny, camera.getId(), eventDto.car_number, "В проезде отказано: Не достаточно средств для списания оплаты за паркинг. Сумма к оплате: " + rateResult + ". Баланс: " + balance, "Not allowed to exit: Not enough balance to pay for parking. Total sum to pay: " + rateResult + ". Balance: " + balance);
                                            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "В проезде отказано: Не достаточно средств для списания оплаты за паркинг. Сумма к оплате: " + rateResult + ". Баланс: " + balance, "Not allowed to exit: Not enough balance to pay for parking. Total sum to pay: " + rateResult + ". Balance: " + balance);
                                        }
                                    } else {
                                        properties.put("type", EventLog.StatusType.Error);
                                        String descriptionRu = "Плагин работы с балансами не найден или не запущен. Авто с гос. номером " + eventDto.car_number;
                                        String descriptionEn = "Balance plugin not found or not launched. Car with license number " + eventDto.car_number;
                                        eventLogService.createEventLog("Billing", null, properties, descriptionRu, descriptionEn);
                                        eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Deny, camera.getId(), eventDto.car_number, descriptionRu, descriptionEn);
                                        hasAccess = false;
                                    }
                                }
                            } else {
                                properties.put("type", EventLog.StatusType.Error);
                                String descriptionRu = "Плагин вычисления стоимости парковки не найден или не запущен. Авто с гос. номером " + eventDto.car_number;
                                String descriptionEn = "Plugin for calculating total sum not found or not launched. Car with number " + eventDto.car_number;
                                eventLogService.createEventLog("Rate", null, properties, descriptionRu, descriptionEn);
                                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Deny, camera.getId(), eventDto.car_number, descriptionRu, descriptionEn);
                                hasAccess = false;
                            }
                        }
                    }
                } else {
                    hasAccess = false;
                }
            }
        }
        if (hasAccess) {
            boolean openResult = barrierService.openBarrier(camera.getGate().getBarrier(), properties);
            if (openResult) {
                gate.gateStatus = GateStatusDto.GateStatus.Open;
                gate.sensor1 = GateStatusDto.SensorStatus.Triggerred;
                gate.sensor2 = GateStatusDto.SensorStatus.WAIT;
                gate.directionStatus = GateStatusDto.DirectionStatus.FORWARD;
                gate.lastTriggeredTime = System.currentTimeMillis();
                if (!gate.isSimpleWhitelist && !leftFromThisSecondsBefore && carState != null) {
                    saveCarOutState(eventDto, camera, carState, properties, isWhitelistCar, balance, rateResult, zerotouchValue, format, carOutBy);
                } else if (leftFromThisSecondsBefore) {
                    properties.put("type", EventLog.StatusType.Allow);
                    eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.car_number, "Выпускаем авто: Авто с гос. номером " + eventDto.car_number, "Releasing: Car with license plate " + eventDto.car_number);
                    eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "Выпускаем авто: Авто с гос. номером " + eventDto.car_number, "Releasing: Car with license plate " + eventDto.car_number);
                }
            }
        } else {
            try {
                qrPanelService.display(camera.getGate(), eventDto.car_number);
            } catch (Exception ex) {
                log.log(Level.WARNING, "Error while display qrpanel for gate " + gate.gateName);
            }
        }
    }

    private void saveCarOutState(CarEventDto eventDto, Camera camera, CarState carState, Map<String, Object> properties, boolean isWhitelistCar, BigDecimal balance, BigDecimal rateResult, BigDecimal zerotouchValue, SimpleDateFormat format, StaticValues.CarOutBy carOutBy) throws Exception {
        if (StaticValues.CarOutBy.WHITELIST.equals(carOutBy)) {
            carStateService.createOUTState(eventDto.car_number, eventDto.event_date_time, camera, carState,properties.containsKey(StaticValues.carSmallImagePropertyName) ? properties.get(StaticValues.carSmallImagePropertyName).toString() : null);

            properties.put("type", EventLog.StatusType.Allow);
            if (isWhitelistCar) {
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.car_number, "Пропускаем авто: Присуствовал в белом списке. Авто с гос. номером " + eventDto.car_number, "Car is allowed: Exist in whitelist. Car with license plate " + eventDto.car_number);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "Пропускаем авто: Присуствовал в белом списке. Авто с гос. номером " + eventDto.car_number, "Car is allowed: Exist in whitelist. Car with license plate " + eventDto.car_number);
            } else {
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.car_number, "Выпускаем авто: Авто с гос. номером " + eventDto.car_number, "Releasing: Car with license plate " + eventDto.car_number);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, "Выпускаем авто: Авто с гос. номером " + eventDto.car_number, "Releasing: Car with license plate " + eventDto.car_number);
            }
        } else if (StaticValues.CarOutBy.PREPAID.equals(carOutBy)) {
            carStateService.createOUTState(eventDto.car_number, eventDto.event_date_time, camera, carState,properties.containsKey(StaticValues.carSmallImagePropertyName) ? properties.get(StaticValues.carSmallImagePropertyName).toString() : null);
            properties.put("type", EventLog.StatusType.Allow);
            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.car_number, "Выпускаем авто по предоплате: Авто с гос. номером" + eventDto.car_number, "For prepaid exit is allowed on prepaid basis. Car with license plate " + eventDto.car_number);
            eventLogService.createEventLog(CarState.class.getSimpleName(), null, properties, "Выпускаем авто по предоплате: Авто с гос. номером" + eventDto.car_number, "For prepaid exit is allowed on prepaid basis. Car with license plate " + eventDto.car_number);
        } else if (StaticValues.CarOutBy.ABONOMENT.equals(carOutBy)) {
            carStateService.createOUTState(eventDto.car_number, eventDto.event_date_time, camera, carState,properties.containsKey(StaticValues.carSmallImagePropertyName) ? properties.get(StaticValues.carSmallImagePropertyName).toString() : null);
            properties.put("type", EventLog.StatusType.Allow);
            String message_ru = "Пропускаем авто: Найдено действущий абономент на номер авто " + eventDto.car_number + ". Проезд разрешен.";
            String message_en = "Allowed: Found valid subscription for car late number " + eventDto.car_number + ". Allowed.";
            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.car_number, message_ru, message_en);
            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, message_ru, message_en);
            carStateService.setAbonomentDetails(carState.getId(), getAbonomentDetails(eventDto.car_number, carState.getParking().getId()));
        } else if (StaticValues.CarOutBy.REGISTER.equals(carOutBy)) {
            if(carState != null){
                carStateService.createOUTState(eventDto.car_number, eventDto.event_date_time, camera, carState,properties.containsKey(StaticValues.carSmallImagePropertyName) ? properties.get(StaticValues.carSmallImagePropertyName).toString() : null);
                properties.put("type", EventLog.StatusType.Allow);
                String message_ru = "Пропускаем авто: Авто с номером " + eventDto.car_number + " выехал с паркинга";
                String message_en = "Allowed: Car with plate number " + eventDto.car_number + " left the parking";
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.car_number, message_ru, message_en);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, message_ru, message_en);
            } else {
                properties.put("type", EventLog.StatusType.Allow);
                String message_ru = "Не найден запись о въезде: Авто с номером " + eventDto.car_number + " выехал с паркинга";
                String message_en = "No record found about entering: Car with plate number " + eventDto.car_number + " left the parking";
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.car_number, message_ru, message_en);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, message_ru, message_en);
            }
        } else {
            if (StaticValues.CarOutBy.ZERO_TOUCH.equals(carOutBy)) {
                properties.put("type", EventLog.StatusType.Allow);
                carStateService.createOUTState(eventDto.car_number, eventDto.event_date_time, camera, carState,properties.containsKey(StaticValues.carSmallImagePropertyName) ? properties.get(StaticValues.carSmallImagePropertyName).toString() : null);
                String message_ru = "Пропускаем авто: Безусловная оплата прошла удачно. Сумма к оплате: " + rateResult + ". Проезд разрешен.";
                String message_en = "Allowed: Zero touch payment received. Total sum: " + rateResult + ". Allowed.";
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.car_number, message_ru, message_en);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, message_ru, message_en);
                rateResult = rateResult.subtract(zerotouchValue);
            } else {
                BigDecimal subtractResult = balance.subtract(rateResult);
                properties.put("type", EventLog.StatusType.Allow);
                String descriptionRu = "Пропускаем авто: Оплата за паркинг присутствует. Сумма к оплате: " + rateResult + ". Остаток баланса: " + subtractResult + ". Проезд разрешен.";
                String descriptionEn = "Allowed: Paid for parking. Total sum: " + rateResult + ". Balance left: " + subtractResult + ". Allowed.";
                if (BigDecimal.ZERO.compareTo(rateResult) == 0) {
                    descriptionRu = "Пропускаем авто: Оплата не требуется. Проезд разрешен.";
                    descriptionEn = "Allowed: No payment required. Allowed";
                }
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.car_number, descriptionRu, descriptionEn);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getId(), properties, descriptionRu, descriptionEn);
            }

            PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
            if (billingPluginRegister != null && BigDecimal.ZERO.compareTo(rateResult) != 0) {
                decreaseBalance(carState.getCarNumber(), carState.getParking().getName(), carState.getId(), rateResult, properties);
            }
            carState.setRateAmount(zerotouchValue != null ? rateResult.add(zerotouchValue) : rateResult);
            carStateService.createOUTState(eventDto.car_number, eventDto.event_date_time, camera, carState,properties.containsKey(StaticValues.carSmallImagePropertyName) ? properties.get(StaticValues.carSmallImagePropertyName).toString() : null);

            if (billingPluginRegister != null) {
                ObjectNode addTimestampNode = this.objectMapper.createObjectNode();
                addTimestampNode.put("command", "addOutTimestampToPayments");
                addTimestampNode.put("outTimestamp", format.format(eventDto.event_date_time));
                addTimestampNode.put("carStateId", carState.getId());
                billingPluginRegister.execute(addTimestampNode);
            }
        }
    }

    private void createNewCarEvent(Camera camera, CarEventDto eventDto, Map<String, Object> properties) {
        properties.put("type", EventLog.StatusType.Success);
        eventLogService.sendSocketMessage(ArmEventType.Photo, EventLog.StatusType.Success, camera.getId(), eventDto.car_number, eventDto.car_picture, null);
        eventLogService.sendSocketMessage(ArmEventType.Lp, EventLog.StatusType.Success, camera.getId(), eventDto.car_number, eventDto.lp_picture, null);
        eventLogService.createEventLog(Camera.class.getSimpleName(), camera.getId(), properties, "Зафиксирован новый номер авто " + eventDto.car_number, "New license plate number identified " + eventDto.car_number);
        carsService.createCar(eventDto.car_number, eventDto.region, eventDto.vecihleType, Gate.GateType.OUT.equals(camera.getGate().getGateType()) ? null : eventDto.car_model); // При выезде не сохранять тип авто
    }

    private boolean hasValidAbonoment(String plateNumber, Long parkingId) throws Exception {
        PluginRegister abonomentPluginRegister = pluginService.getPluginRegister(StaticValues.abonomentPlugin);
        if (abonomentPluginRegister != null) {
            ObjectNode node = this.objectMapper.createObjectNode();
            node.put("command", "hasPaidNotExpiredAbonoment");
            node.put("plateNumber", plateNumber);
            node.put("parkingId", parkingId);

            JsonNode result = abonomentPluginRegister.execute(node);
            if(result.has("hasPaidNotExpiredAbonoment")){
                return result.get("hasPaidNotExpiredAbonoment").booleanValue();
            }
        }
        return false;
    }

    private JsonNode getAbonomentDetails(String plateNumber, Long parkingId) throws Exception {
        PluginRegister abonomentPluginRegister = pluginService.getPluginRegister(StaticValues.abonomentPlugin);
        if (abonomentPluginRegister != null) {
            ObjectNode node = this.objectMapper.createObjectNode();
            node.put("command", "getPaidNotExpiredAbonomentDetails");
            node.put("plateNumber", plateNumber);
            node.put("parkingId", parkingId);

            JsonNode result = abonomentPluginRegister.execute(node);
            if(result.has("abonomentDetails")){
                return result.get("abonomentDetails");
            }
        }
        return null;
    }
}