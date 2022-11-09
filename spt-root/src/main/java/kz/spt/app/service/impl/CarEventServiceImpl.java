package kz.spt.app.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.app.job.SensorStatusCheckJob;
import kz.spt.app.job.StatusCheckJob;
import kz.spt.app.model.dto.CameraStatusDto;
import kz.spt.app.model.dto.GateStatusDto;
import kz.spt.app.model.dto.Period;
import kz.spt.app.model.strategy.barrier.open.AbstractOpenStrategy;
import kz.spt.app.model.strategy.barrier.open.CarInEventStrategy;
import kz.spt.app.model.strategy.barrier.open.CarOutEventStrategy;
import kz.spt.app.model.strategy.barrier.open.CarReverseEventStrategy;
import kz.spt.app.repository.CarModelRepository;
import kz.spt.app.service.*;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.model.*;
import kz.spt.lib.model.dto.CarEventDto;
import kz.spt.lib.service.*;
import kz.spt.lib.utils.StaticValues;
import lombok.extern.java.Log;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.dialect.springdata.util.Strings;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.logging.Level;

import static kz.spt.lib.service.EventLogService.ArmEventType;

@Log
@Service
@Transactional(noRollbackFor = Exception.class)
public class CarEventServiceImpl implements CarEventService {

    private static Hashtable<String, Long> cameraTimeoutHashtable = new Hashtable<>();
    private static Hashtable<Long, String> barrierInProcessingHashtable = new Hashtable<>();
    private static Hashtable<Long, String> barrierOutProcessingHashtable = new Hashtable<>();
    private final CarsService carsService;
    private final CameraService cameraService;
    private final GateService gateService;
    private final ParkingService parkingService;
    private final EventLogService eventLogService;
    private final CarStateService carStateService;
    private final CarImageService carImageService;
    private final BarrierService barrierService;
    private final BlacklistService blacklistService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PluginService pluginService;
    private final QrPanelService qrPanelService;
    private final AbonomentService abonomentService;
    private final CarModelService carModelService;
    private final CarModelRepository carModelRepository;
    private final WhitelistRootService whitelistRootService;
    private final PaymentService paymentService;

    private final TabloService tabloService;

    @Value("${parking.has.access.unknown.cases}")
    Boolean parkingHasAccessUnknownCases;

    @Value("${parking.only.register.cars}")
    Boolean parkingOnlyRegisterCars;

    @Value("${parking.ignore.left.seconds}")
    int parkingIgnoreLeftSeconds;

    @Value("${parking.ignore.entered.seconds}")
    int parkingIgnoreEnteredSeconds;

    @Value("${notification.send}")
    Boolean notification;

    @Value("${notification.parkingUid}")
    Object parking_uid;

    @Value("${tablo.connected}")
    Boolean tabloConnected;

    @Value("${notification.url}")
    String notificationUrl;

    @Value("${notification.token}")
    String magnumNotificationToken;

    @Value("${thirdPartyPayment.url}")
    String thirdPartyPaymentUrl;

    @Value("${parkings.uid}")
    String parkingUid;

    @Value("${booking.check.out}")
    boolean bookingCheckOut;

    private String dateFormat = "yyyy-MM-dd'T'HH:mm";
    private ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag(LocaleContextHolder.getLocale().toString().substring(0, 2)));

    public CarEventServiceImpl(CarsService carsService, CameraService cameraService, EventLogService eventLogService,
                               CarStateService carStateService, CarImageService carImageService,
                               BarrierService barrierService, BlacklistService blacklistService, PluginService pluginService, QrPanelService qrPanelService,
                               AbonomentService abonomentService, CarModelService carModelService, CarModelRepository carModelRepository,
                               WhitelistRootService whitelistRootService, GateService gateService, ParkingService parkingService,
                               PaymentService paymentService, TabloService tabloService) {
        this.carsService = carsService;
        this.cameraService = cameraService;
        this.eventLogService = eventLogService;
        this.carStateService = carStateService;
        this.carImageService = carImageService;
        this.barrierService = barrierService;
        this.blacklistService = blacklistService;
        this.pluginService = pluginService;
        this.qrPanelService = qrPanelService;
        this.abonomentService = abonomentService;
        this.carModelService = carModelService;
        this.carModelRepository = carModelRepository;
        this.whitelistRootService = whitelistRootService;
        this.gateService = gateService;
        this.parkingService = parkingService;
        this.paymentService = paymentService;
        this.tabloService = tabloService;
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

                Optional<CarState> carStateForCheckDuplicate = Optional.ofNullable(carStateService.getLastNotLeft(platenumber));
                if(camera.getGate().getGateType().equals(Gate.GateType.OUT)
                        && carStateForCheckDuplicate.isPresent()
                        && !camera.getGate().getParking().getId().equals(carStateForCheckDuplicate.get().getParking().getId())){
                    camera = cameraService.findCameraByIpAndParking(camera.getIp(), carStateForCheckDuplicate.get().getParking()).get();
                    cameraId = camera.getId();
                }

                CarEventDto eventDto = new CarEventDto();
                eventDto.event_date_time = new Date();
                eventDto.car_number = platenumber;
                eventDto.ip_address = camera.getIp();
                eventDto.lp_rect = null;
                eventDto.lp_picture = null;
                eventDto.manualEnter = true;
                eventDto.cameraId = cameraId;

                if (snapshot != null && !"".equals(snapshot) && !"undefined".equals(snapshot) && !"null".equals(snapshot) && !"data:image/jpg;base64,null".equals(snapshot)) {
                    eventDto.car_picture = snapshot;
                } else {
                    eventDto.car_picture = null;
                }
                SimpleDateFormat format = new SimpleDateFormat(dateFormat);

                Map<String, Object> properties = new HashMap<>();
                properties.put("carNumber", platenumber);
                properties.put("eventTime", format.format(new Date()));
                properties.put("cameraIp", camera.getIp());
                properties.put("cameraId", cameraId);
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
                String messageDe = "Manueller Durchgang. Auto mit Kennzeichen " + platenumber + ". Benutzer " + username + " initiiert einen manuellen Start auf " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "eingang" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "ausfahrt" : "einfahrt/ausfahrt")) + " " + camera.getGate().getDescription() + " parken " + camera.getGate().getParking().getName();
                eventLogService.sendSocketMessage(EventLogService.ArmEventType.CarEvent,
                        EventLog.StatusType.Success, camera.getId(),
                        platenumber,
                        messageRu,
                        messageEn,
                        messageDe);

                eventLogService.createEventLog(Gate.class.getSimpleName(),
                        camera.getGate().getId(),
                        properties,
                        messageRu,
                        messageEn,
                        messageDe,
                        EventLog.EventType.MANUAL_GATE_OPEN);

                saveCarEvent(eventDto);
            }
        }
        return barrierResult;
    }

    @Override
    public void handleRtaCarEvent(MultipartFile event_image_0, MultipartFile event_cropped_image_0, String event_descriptor, String event_timestamp) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(event_descriptor);

        String detectorID = jsonNode.get("DetectorID").asText();
        Camera camera = cameraService.findCameraByDetectorId(detectorID);

        if (camera != null) {
            log.warning("Camera " + camera.getIp() + " found for detector id = " + detectorID);
            String car_number = jsonNode.get("EventInfo").get("Text").textValue();

            log.info("EventInfo: " + jsonNode.get("EventInfo"));
            log.info("event_timestamp: " + event_timestamp);

            String country = null;
            if (jsonNode.get("EventInfo").has("Country") && jsonNode.get("EventInfo").get("Country") != null && jsonNode.get("EventInfo").get("Country").textValue() != null) {
                country = jsonNode.get("EventInfo").get("Country").textValue();
            }

            String base64 = null;
            String base64_lp = null;
            try {
                base64 = StringUtils.newStringUtf8(Base64.encodeBase64(event_image_0.getInputStream().readAllBytes(), false));
                base64_lp = event_cropped_image_0 != null ? StringUtils.newStringUtf8(Base64.encodeBase64(event_cropped_image_0.getInputStream().readAllBytes(), false)) : null;
            } catch (IOException e) {
                e.printStackTrace();
            }

            CarEventDto eventDto = new CarEventDto();
            eventDto.car_number = car_number;
            eventDto.event_date_time = event_timestamp != null ? new Date(Long.valueOf(event_timestamp)) : new Date();
            eventDto.ip_address = camera.getIp();
            eventDto.car_picture = base64;
            eventDto.lp_picture = base64_lp;
            eventDto.lp_region = country;
            eventDto.vecihleType = null;
            saveCarEvent(eventDto);
        } else {
            log.warning("Camera not found for detector id = " + detectorID);
        }
    }

    @Override
    public void handleLiveStreamEvent(MultipartFile event_image_0, String event_descriptor, String event_timestamp) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(event_descriptor);

        String detectorID = jsonNode.get("DetectorID").asText();
        Camera camera = cameraService.findCameraByDetectorId(detectorID);

        if (camera != null) {
            log.warning("Camera " + camera.getIp() + " found for detector id = " + detectorID);
            String car_number = jsonNode.get("EventInfo").get("Text").textValue();

            log.info("EventInfo: " + jsonNode.get("EventInfo"));
            log.info("event_timestamp: " + event_timestamp);

            String country = null;
            if (jsonNode.get("EventInfo").has("Country") && jsonNode.get("EventInfo").get("Country") != null && jsonNode.get("EventInfo").get("Country").textValue() != null) {
                country = jsonNode.get("EventInfo").get("Country").textValue();
            }

            String base64 = null;
            try {
                if (event_image_0 != null) {
                    base64 = StringUtils.newStringUtf8(Base64.encodeBase64(event_image_0.getInputStream().readAllBytes(), false));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            CarEventDto eventDto = new CarEventDto();
            eventDto.car_number = car_number;
            eventDto.event_date_time = event_timestamp != null ? new Date(Long.valueOf(event_timestamp)) : new Date();
            eventDto.ip_address = camera.getIp();
            eventDto.car_picture = base64;
            eventDto.lp_region = country;
            eventDto.vecihleType = null;
            saveCarEvent(eventDto);
        } else {
            log.warning("Camera not found for detector id = " + detectorID);
        }
    }

    @Override
    public void handleLiveStreamEvent(byte[] event_image, String event_descriptor, String event_timestamp) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree("{" + event_descriptor);

        String detectorID = jsonNode.get("DetectorID").asText();
        Camera camera = cameraService.findCameraByDetectorId(detectorID);

        if (camera != null) {
            log.warning("Camera " + camera.getIp() + " found for detector id = " + detectorID);
            String car_number = jsonNode.get("EventInfo").get("Text").textValue();

            log.info("EventInfo: " + jsonNode.get("EventInfo"));
            log.info("event_timestamp: " + event_timestamp);

            String country = null;
            if (jsonNode.get("EventInfo").has("Country") && jsonNode.get("EventInfo").get("Country") != null && jsonNode.get("EventInfo").get("Country").textValue() != null) {
                country = jsonNode.get("EventInfo").get("Country").textValue();
            }

            String base64 = null;
            String base64_lp = null;
            base64 = StringUtils.newStringUtf8(Base64.encodeBase64(event_image, false));

            CarEventDto eventDto = new CarEventDto();
            eventDto.car_number = car_number;
            eventDto.event_date_time = event_timestamp != null ? new Date(Long.valueOf(event_timestamp)) : new Date();
            eventDto.ip_address = camera.getIp();
            eventDto.car_picture = base64;
            eventDto.lp_picture = base64_lp;
            eventDto.lp_region = country;
            eventDto.vecihleType = null;
            saveCarEvent(eventDto);
        } else {
            log.warning("Camera not found for detector id = " + detectorID);
        }
    }

    @Override
    public void handleRtaCarEvent(String event_descriptor) throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(event_descriptor);

        String detectorID = jsonNode.get("camera_id").asText();
        String event_timestamp = jsonNode.get("timestamp").asText();
        Camera camera = cameraService.findCameraByDetectorId(detectorID);

        if (camera != null) {
            log.warning("Camera " + camera.getIp() + " found for detector id = " + detectorID);
            String car_number = jsonNode.get("plate_no").textValue();

            log.info("plate_number: " + car_number);
            log.info("event_timestamp: " + event_timestamp);

            CarEventDto eventDto = new CarEventDto();
            eventDto.car_number = car_number;
            eventDto.event_date_time = event_timestamp != null ? format.parse(event_timestamp) : new Date();
            eventDto.ip_address = camera.getIp();
            saveCarEvent(eventDto);
        } else {
            log.warning("Camera not found for detector id = " + detectorID);
        }
    }

    @Override
    public void handleManualEnter(Long cameraId, String plateNumber) throws Exception {
        if(plateNumber!=null){
            Camera camera = cameraService.getCameraById(cameraId);

            String username = "";
            if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CurrentUser) {
                CurrentUser currentUser = (CurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                if (currentUser != null) {
                    username = currentUser.getUsername();
                }
            }

            Optional<CarState> carStateForCheckDuplicate = Optional.ofNullable(carStateService.getLastNotLeft(plateNumber));
            if(camera.getGate().getGateType().equals(Gate.GateType.OUT)
                    && carStateForCheckDuplicate.isPresent()
                    && !camera.getGate().getParking().getId().equals(carStateForCheckDuplicate.get().getParking().getId())){
                camera = cameraService.findCameraByIpAndParking(camera.getIp(), carStateForCheckDuplicate.get().getParking()).get();
                cameraId = camera.getId();
            }

            CarEventDto eventDto = new CarEventDto();
            eventDto.event_date_time = new Date();
            eventDto.car_number = plateNumber;
            eventDto.ip_address = camera.getIp();
            eventDto.lp_rect = null;
            eventDto.lp_picture = null;
            eventDto.manualEnter = true;
            eventDto.cameraId = cameraId;

            SimpleDateFormat format = new SimpleDateFormat(dateFormat);

            Map<String, Object> properties = new HashMap<>();
            properties.put("carNumber", plateNumber);
            properties.put("eventTime", format.format(new Date()));
            properties.put("cameraIp", camera.getIp());
            properties.put("cameraId", cameraId);
            properties.put("gateName", camera.getGate().getName());
            properties.put("gateDescription", camera.getGate().getDescription());
            properties.put("gateType", camera.getGate().getGateType().toString());
            properties.put("type", EventLog.StatusType.Allow);

            String message_ru = "Ручное запуск без открытие шлагбаума: Пользователь " + username + " открыл для " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "выезда" : "въезда/выезда")) + " " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName();
            String message_en = "Manual enter without open gate: User " + username + " opened for " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "enter" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName();
            String message_de = "Manuelle Eingabe ohne offenes Tor: Benutzer " + username + " geöffnet für " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "einfahrt" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "ausfahrt" : "einfahrt/ausfahrt")) + " " + camera.getGate().getDescription() + " parken " + camera.getGate().getParking().getName();
            eventLogService.sendSocketMessage(EventLogService.ArmEventType.CarEvent,
                    EventLog.StatusType.Success, camera.getId(),
                    plateNumber,
                    message_ru,
                    message_en,
                    message_de);

            eventLogService.createEventLog(Gate.class.getSimpleName(),
                    camera.getGate().getId(),
                    properties,
                    message_ru,
                    message_en,
                    message_de,
                    EventLog.EventType.MANUAL_GATE_OPEN);

            saveCarEvent(eventDto);
        }
    }

    @Override
    public void saveCarEvent(CarEventDto eventDto) throws Exception {

        SimpleDateFormat format = new SimpleDateFormat(StaticValues.dateFormatTZ);
        eventDto.car_number = eventDto.car_number.toUpperCase();

        if (eventDto.manualEnter){
            Optional<CarState> carStateForCheckDuplicate = Optional.ofNullable(carStateService.getLastNotLeft(eventDto.car_number));
            if(eventDto.cameraId!=null){
                Camera camera = cameraService.getCameraById(eventDto.cameraId);
                if(camera.getGate().getGateType().equals(Gate.GateType.OUT)
                        && carStateForCheckDuplicate.isPresent()
                        && !camera.getGate().getParking().getId().equals(carStateForCheckDuplicate.get().getParking().getId())) {

                    camera = cameraService.findCameraByIpAndParking(camera.getIp(), carStateForCheckDuplicate.get().getParking()).get();
                    eventDto.cameraId = camera.getId();
                }
            }

        }

        CameraStatusDto cameraStatusDto = eventDto.cameraId != null ? StatusCheckJob.findCameraStatusDtoById(eventDto.cameraId) : StatusCheckJob.findCameraStatusDtoByIp(eventDto.ip_address);

        //Если камера не зайдействована, то не фиксировать события по ней. Задача #170
        if (cameraStatusDto != null && !cameraStatusDto.enabled)
            return;

        Map<String, Object> properties = new HashMap<>();
        properties.put("carNumber", eventDto.car_number);
        properties.put("eventTime", format.format(eventDto.event_date_time));
        properties.put("lp_rect", eventDto.lp_rect);
        properties.put("cameraIp", eventDto.ip_address);
        properties.put("cameraId", eventDto.cameraId);
        if (eventDto.lp_region != null) {
            properties.put("lp_region", eventDto.lp_region);
        }
        if (eventDto.vecihleType != null) {
            properties.put("vecihleType", eventDto.vecihleType);
        }
        if (eventDto.car_model != null) {
            properties.put("car_model", eventDto.car_model);
        }

        if (cameraStatusDto != null) {

            GateStatusDto gate = StatusCheckJob.findGateStatusDtoById(cameraStatusDto.gateId);

            String secondCameraIp = (gate.frontCamera2 != null) ? (eventDto.ip_address.equals(gate.frontCamera.ip) ? gate.frontCamera2.ip : gate.frontCamera.ip) : null; // If there is two camera, then ignore second by timeout

            if (!eventDto.manualOpen) {
                if (cameraTimeoutHashtable.containsKey(eventDto.ip_address) || (secondCameraIp != null && cameraTimeoutHashtable.containsKey(secondCameraIp))) {
                    Long timeDiffInMillis = System.currentTimeMillis() - (cameraTimeoutHashtable.containsKey(eventDto.ip_address) ? cameraTimeoutHashtable.get(eventDto.ip_address) : 0);
                    int timeout = (cameraStatusDto.timeout == 0 ? 1 : cameraStatusDto.timeout * 1000);
                    if (secondCameraIp != null) {
                        Long secondCameraTimeDiffInMillis = System.currentTimeMillis() - (cameraTimeoutHashtable.containsKey(secondCameraIp) ? cameraTimeoutHashtable.get(secondCameraIp) : 0);
                        timeDiffInMillis = timeDiffInMillis < secondCameraTimeDiffInMillis ? timeDiffInMillis : secondCameraTimeDiffInMillis;
                    }

                    if (timeDiffInMillis < timeout) { // If interval smaller than timeout then ignore else proceed
                        log.info("Ignored event from camera: " + eventDto.ip_address + " time: " + timeDiffInMillis);
                        return;
                    } else {
                        cameraTimeoutHashtable.put(eventDto.ip_address, System.currentTimeMillis());
                    }
                } else {
                    cameraTimeoutHashtable.put(eventDto.ip_address, System.currentTimeMillis());
                }
            } else {
                cameraTimeoutHashtable.put(eventDto.ip_address, System.currentTimeMillis());
            }

            log.info("handling event from camera: " + eventDto.ip_address + " for car: " + eventDto.car_number + " model - " + eventDto.car_model);

            properties.put("gateName", gate.gateName);
            properties.put("gateId", gate.gateId);
            properties.put("gateType", gate.gateType);

            if (eventDto.car_picture != null && !"".equals(eventDto.car_picture) && !"null".equals(eventDto.car_picture) && !"undefined".equals(eventDto.car_picture) && !"data:image/jpg;base64,null".equals(eventDto.car_picture)) {
                String carImageUrl = carImageService.saveImage(eventDto.car_picture, eventDto.event_date_time, eventDto.car_number);
                properties.put(StaticValues.carImagePropertyName, carImageUrl);
                properties.put(StaticValues.carSmallImagePropertyName, carImageUrl.replace(StaticValues.carImageExtension, "") + StaticValues.carImageSmallAddon + StaticValues.carImageExtension);
            }

            log.info("Camera belongs to gate: " + gate.gateId);
            if (gate.frontCamera != null && gate.frontCamera.id == cameraStatusDto.id) {
                gate.frontCamera.carEventDto = eventDto;
                gate.frontCamera.properties = properties;
            } else if (gate.backCamera != null && gate.backCamera.id == cameraStatusDto.id) {
                gate.backCamera.carEventDto = eventDto;
                gate.backCamera.properties = properties;
            }

            if (eventDto.manualOpen || isAllow(eventDto, cameraStatusDto, properties, gate)) {
                log.info("Gate type: " + gate.gateType);

                Camera camera = null;
                if (eventDto.cameraId != null) {
                    camera = cameraService.getCameraById(eventDto.cameraId);
                } else {
                    List<Camera> cameraList = cameraService.findCameraByIp(cameraStatusDto.ip);
                    if (Gate.GateType.OUT.equals(gate.gateType)) {
                        if (cameraList.size() > 1) {
                            CarState carStateForCheckGateType = carStateService.getLastNotLeft(eventDto.car_number);
                            if (carStateForCheckGateType != null) { // Check Last not left parking
                                for (Camera cm : cameraList) {
                                    if (cm.getGate().getParking().equals(carStateForCheckGateType.getParking())) {
                                        camera = cm;
                                    }
                                }
                            } else { // if not left parking absent then check last left parking
                                carStateForCheckGateType = carStateService.getLastCarState(eventDto.car_number);
                                if (carStateForCheckGateType != null) {
                                    for (Camera cm : cameraList) {
                                        if (cm.getGate().getParking().equals(carStateForCheckGateType.getParking())) {
                                            camera = cm;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (camera == null) {
                        camera = cameraList.get(0);
                    }
                }


                createNewCarEvent(camera.getId(), gate, eventDto, properties);

                if (Gate.GateType.REVERSE.equals(gate.gateType)) {
                    handleCarReverseInEvent(eventDto, camera, gate, properties, format);
                } else if (Gate.GateType.IN.equals(gate.gateType)) {
                    if ((cameraStatusDto.getStartTime() != null) && (cameraStatusDto.getEndTime() != null)) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(eventDto.event_date_time);
                        boolean isAfterStartTime  = cameraStatusDto.getStartTime().isBefore
                                (LocalTime.of(calendar.get(Calendar.HOUR_OF_DAY),calendar.get(Calendar.MINUTE)));
                        boolean isBeforeEndTime  = cameraStatusDto.getEndTime().isAfter
                                (LocalTime.of(calendar.get(Calendar.HOUR_OF_DAY),calendar.get(Calendar.MINUTE)));
                        if(!eventDto.manualEnter) {
                            if ((isAfterStartTime) && (isBeforeEndTime)) {
                                eventPropertiesOfIgnoringType(eventDto, properties, gate, camera);
                                return;
                            }
                        }
                    }
                    handleCarInEvent(eventDto, camera, gate, properties, format);
                } else if (Gate.GateType.OUT.equals(gate.gateType)) {
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
            eventLogService.createEventLog(null, null, properties,
                    "Зафиксирован новый номер авто " + " " + eventDto.car_number + " от неизвестной камеры с ip " + eventDto.ip_address,
                    "Identified new car with number " + " " + eventDto.car_number + " from unknown camera with ip " + eventDto.ip_address,
                    "Identifiziertes neues Auto mit Kennzeichen " + " " + eventDto.car_number + " von unbekannter Kamera mit ip " + eventDto.ip_address,
                    EventLog.EventType.NEW_CAR_DETECTED);
        }
    }

    private void eventPropertiesOfIgnoringType(CarEventDto eventDto, Map<String, Object> properties,
                                               GateStatusDto gate, Camera camera) {
            properties.put("gateName", gate.gateName);
            properties.put("gateId", gate.gateId);
            properties.put("gateType", gate.gateType);
            properties.put("type", EventLog.StatusType.Ignoring);
            String descriptionRu = "Ошибка открытия шлагбаума: Идентифицировано авто в нерабочее с номером: " + eventDto.car_number ;
            String descriptionEn = "Error while barrier open: Identified a car during non-working hours with plate number  " + eventDto.car_number;
            String descriptionDe = "Fehler beim Öffnen der Schranke: Identifiziert ein Auto während der Nicht-Arbeitszeit mit dem Nummernschild  " + eventDto.car_number;
            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Ignoring, camera.getId(), eventDto.getCarNumberWithRegion(), descriptionRu, descriptionEn, descriptionDe);
            eventLogService.createEventLog(null, null, properties,
                    "Зафиксирован новый номер авто " + eventDto.car_number + " в нерабочее время парковки",
                    "Identified a new car" + eventDto.car_number + " at night time",
                    "Identifiziertes neues Auto mit Kennzeichen" + eventDto.car_number + " bei Nacht",
                    EventLog.EventType.NEW_CAR_DETECTED);
    }

    private boolean isAllow(CarEventDto carEvent, CameraStatusDto cameraStatusDto, Map<String, Object> properties, GateStatusDto gate) {
        if (blacklistService.findByPlate(carEvent.car_number).isPresent()) {
            properties.put("type", EventLog.StatusType.Deny);
            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Deny, cameraStatusDto.id, carEvent.getCarNumberWithRegion(),
                    "В проезде отказано: Авто с гос. номером " + carEvent.car_number + " в черном списке",
                    "Not allowed to enter: Car with number " + carEvent.car_number + " in ban",
                    "Eingabe nicht erlaubt: Auto mit Kennzeichen " + carEvent.car_number + " im Verbot");
            eventLogService.createEventLog(Gate.class.getSimpleName(), cameraStatusDto.gateId, properties,
                    "В проезде отказано: Авто с гос. номером " + carEvent.car_number + " в черном списке",
                    "Not allowed to enter: Car with number " + carEvent.car_number + " in ban",
                    "Eingabe nicht erlaubt: Auto mit Kennzeichen " + carEvent.car_number + " im Verbot",
                    EventLog.EventType.NOT_PASS);
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
        eventDto.lp_region = region;
        eventDto.vecihleType = vecihleType;
        saveCarEvent(eventDto);
    }

    private void handleCarReverseInEvent(CarEventDto eventDto, Camera camera, GateStatusDto gate, Map<String, Object> properties, SimpleDateFormat format) throws Exception {
        JsonNode whitelistCheckResults = getWhiteLists(camera.getGate().getParking().getId(), eventDto.car_number, eventDto.event_date_time, format, properties);
        boolean hasAccess = checkSimpleWhiteList(eventDto, camera, properties, whitelistCheckResults, true);
        if (hasAccess) {
            if (camera.getGate().getBarrier().isStatusCheck()) {
                AbstractOpenStrategy strategy = CarReverseEventStrategy.builder()
                        .camera(camera)
                        .eventDto(eventDto)
                        .properties(properties)
                        .build();
                strategy.gateId = gate.gateId;
                strategy.isWaitLoop = true;
                SensorStatusCheckJob.add(strategy);
            } else {
                boolean openResult = barrierService.openBarrier(camera.getGate().getBarrier(), properties);
                if (openResult) {


                    gate.gateStatus = GateStatusDto.GateStatus.Open;
                    gate.sensor1 = GateStatusDto.SensorStatus.Triggerred;
                    gate.sensor2 = GateStatusDto.SensorStatus.WAIT;
                    gate.directionStatus = GateStatusDto.DirectionStatus.FORWARD;
                    gate.lastTriggeredTime = System.currentTimeMillis();
                } else {
                    String descriptionRu = "Ошибка открытия шлагбаума: На контроллер шлагбаума не удалось присвоит значение на открытие для авто " + eventDto.car_number + " на " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезд" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "выезд" : "въезд/выезд")) + " для " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName();
                    String descriptionEn = "Error while barrier open: Cannot assign value to open barrier for car " + eventDto.car_number + " to " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "pass" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName();
                    String descriptionDe = "Fehler beim Öffnen der Schranke: Wert für offene Schranke für Auto kann nicht zugewiesen werden " + eventDto.car_number + " an " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "passieren" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "ausfahrt" : "einfahrt/ausfahrt")) + " " + camera.getGate().getDescription() + " parken " + camera.getGate().getParking().getName();
                    eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Error, camera.getId(), eventDto.getCarNumberWithRegion(), descriptionRu, descriptionEn, descriptionDe);
                    eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, descriptionRu, descriptionEn, descriptionDe, EventLog.EventType.ERROR);
                }
            }
        }
    }

    private void handleCarInEvent(CarEventDto eventDto, Camera camera, GateStatusDto gate, Map<String, Object> properties, SimpleDateFormat format) throws Exception {
        boolean hasAccess;
        JsonNode whitelistCheckResults = null;
        Boolean enteredFromThisSecondsBefore = false;
        JsonNode abonements = null;
        abonements = abonomentService.getAbonomentsDetails(eventDto.car_number, camera.getGate().getParking().getId(), new Date(), format);

        // проверить выезжала ли с другого выезда, по умолчанию не больше 20 секунд
        Calendar now = Calendar.getInstance();
        now.add(Calendar.SECOND, (-1) * parkingIgnoreLeftSeconds);
        Boolean hasLeft = carStateService.getIfHasLastFromOtherCamera(eventDto.car_number, eventDto.ip_address, now.getTime());
        if (hasLeft) {
            return;
        }

        // Проверка абонемента на истечение срока или близко к истечению срока
        abonomentService.checkAbonementExpireDate(eventDto.car_number, camera.getId(), gate.parkingId, properties);

        CarState carState = carStateService.getLastNotLeft(eventDto.car_number);
        if (carState != null) {
            Calendar current = Calendar.getInstance();
            if (carState.getParking().equals(camera.getGate().getParking()) && (current.getTime().getTime() - carState.getInTimestamp().getTime() <= 5 * 60 * 1000)) {
                enteredFromThisSecondsBefore = true;
            } else { // Закрывать предыдущий сессию, чтобы авто могла заехать
                carStateService.createOUTManual(eventDto.car_number, new Date(), carState); // Принудительная закрытие предыдущей сессий
                carState = null;
            }
        }

        if (!enteredFromThisSecondsBefore) {
            // Проверка долга
            Boolean hasDebt = false;
            BigDecimal debt = BigDecimal.ZERO;
            if (!eventDto.manualEnter && (Parking.ParkingType.PAYMENT.equals(camera.getGate().getParking().getParkingType()) || Parking.ParkingType.WHITELIST_PAYMENT.equals(camera.getGate().getParking().getParkingType()))) {
                PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
                if (billingPluginRegister != null) {
                    ObjectNode billinNode = this.objectMapper.createObjectNode();
                    billinNode.put("command", "getCurrentBalance");
                    billinNode.put("plateNumber", eventDto.car_number);
                    JsonNode billingResult = billingPluginRegister.execute(billinNode);
                    debt = billingResult.get("currentBalance").decimalValue().setScale(2);
                    if (debt.compareTo(BigDecimal.ZERO) == -1) {
                        hasDebt = true;
                    }
                }
            }
            whitelistCheckResults = getWhiteLists(camera.getGate().getParking().getId(), eventDto.car_number, eventDto.event_date_time, format, properties);

            if (hasDebt) {
                properties.put("type", EventLog.StatusType.Debt);
                String descriptionRu = "В проезде отказано: Авто " + eventDto.car_number + " имеет задолженность " + debt;
                String descriptionEn = "Not allowed to enter: Car " + eventDto.car_number + " is in debt " + debt;
                String descriptionDe = "Betreten nicht erlaubt: Auto " + eventDto.car_number + " ist verschuldet " + debt;
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Debt, camera.getId(), eventDto.getCarNumberWithRegion(), descriptionRu, descriptionEn, descriptionDe);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, descriptionRu, descriptionEn, descriptionDe, EventLog.EventType.DEBT);
                hasAccess = false;
            } else if (Parking.ParkingType.PAYMENT.equals(camera.getGate().getParking().getParkingType())) {
                if (carState == null) {
                    hasAccess = true;
                } else {
                    properties.put("type", EventLog.StatusType.Debt);
                    eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Debt, camera.getId(), eventDto.getCarNumberWithRegion(),
                            "В проезде отказано: Авто " + eventDto.car_number + " имеет задолженность",
                            "Not allowed to enter: Car " + eventDto.car_number + " is in debt",
                            "Betreten nicht erlaubt: Auto " + eventDto.car_number + " ist verschuldet");
                    eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,
                            "В проезде отказано: Авто " + eventDto.car_number + " имеет задолженность",
                            "Not allowed to enter: Car " + eventDto.car_number + " is in debt",
                            "Betreten nicht erlaubt: Auto " + eventDto.car_number + " ist verschuldet",
                            EventLog.EventType.NOT_PASS);
                    hasAccess = false;
                }
            } else if (Parking.ParkingType.PREPAID.equals(camera.getGate().getParking().getParkingType())) {
                if (gate.isSimpleWhitelist) {
                    log.info("Simple whitelist check");
                    hasAccess = checkSimpleWhiteList(eventDto, camera, properties, whitelistCheckResults, false);
                } else {
                    log.info("Complex whitelist check");
                    hasAccess = checkWhiteList(eventDto, camera, properties, whitelistCheckResults, false);
                }
                if (!hasAccess) {
                    JsonNode currentBalanceResult = getCurrentBalance(eventDto.car_number, properties);
                    BigDecimal balance = currentBalanceResult.get("currentBalance").decimalValue();

                    JsonNode prepaidValueResult = getPrepaidValue(camera.getGate().getParking().getId(), properties);
                    BigDecimal prepaid = BigDecimal.valueOf(prepaidValueResult.get("prepaidValue").longValue());

                    if ((balance != null && prepaid != null) && balance.subtract(prepaid).compareTo(BigDecimal.ZERO) >= 0) {
                        decreaseBalance(eventDto.car_number, camera.getGate().getParking().getName(), null, prepaid, properties);
                        String descriptionRu = "Пропускаем авто: Сумма за предоплату присутствует. Сумма к оплате: " + prepaid + ". Остаток баланса: " + balance.subtract(prepaid) + ". Проезд разрешен.";
                        String descriptionEn = "Allowed: Sum for prepayment exist. Total sum: " + prepaid + ". Balance left: " + balance.subtract(prepaid) + ". Allowed.";
                        String descriptionDe = "Erlaubt: Summe für Vorauszahlung vorhanden. Gesamtsumme: " + prepaid + ". Restbetrag übrig: " + balance.subtract(prepaid) + ". Erlaubt.";
                        eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.getCarNumberWithRegion(), descriptionRu, descriptionEn, descriptionDe);
                        eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, descriptionRu, descriptionEn, descriptionDe, EventLog.EventType.PREPAID);
                        hasAccess = true;
                    } else {
                        properties.put("type", EventLog.StatusType.Deny);
                        eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Deny, camera.getId(), eventDto.getCarNumberWithRegion(),
                                "В проезде отказано: Не достаточно средств для списания оплаты за паркинг. Сумма к оплате: " + prepaid + ". Баланс: " + balance + ". Выезд с " + carState.getParking().getName(),
                                "Not allowed to exit: Not enough balance to pay for parking. Total sum to pay: " + prepaid + ". Balance: " + balance + ". Departure from " + carState.getParking().getName(),
                                "Ausfahrt nicht erlaubt: Nicht genug Guthaben, um das Parken zu bezahlen. Gesamtsumme zu zahlen: " + prepaid + ". Saldo: " + balance + ". Abfahrt von " + carState.getParking().getName());
                        eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,
                                "В проезде отказано: Не достаточно средств для списания оплаты за паркинг. Сумма к оплате: " + prepaid + ". Баланс: " + balance + ". Выезд с " + carState.getParking().getName(),
                                "Not allowed to exit: Not enough balance to pay for parking. Total sum to pay: " + prepaid + ". Balance: " + balance + ". Departure from " + carState.getParking().getName(),
                                "Ausfahrt nicht erlaubt: Nicht genug Guthaben, um das Parken zu bezahlen. Gesamtsumme zu zahlen: " + prepaid + ". Saldo: " + balance + ". Abfahrt von " + carState.getParking().getName(),
                                EventLog.EventType.NOT_ENOUGH_BALANCE);
                        hasAccess = false;
                    }
                }
            } else {
                if (gate.isSimpleWhitelist) {
                    log.info("Simple whitelist check");
                    hasAccess = checkSimpleWhiteList(eventDto, camera, properties, whitelistCheckResults, true);
                } else {
                    log.info("not last entered not left");
                    if (Parking.ParkingType.WHITELIST.equals(camera.getGate().getParking().getParkingType())) {
                        if (abonements != null && abonements.isArray() && abonements.size() > 0) {
                            hasAccess = true;
                        } else {
                            hasAccess = checkWhiteList(eventDto, camera, properties, whitelistCheckResults, true);
                        }
                    } else {
                        hasAccess = true;
                    }
                }
            }
        } else {
            hasAccess = true;
        }

        log.info("enteredFromThisSecondsBefore: " + enteredFromThisSecondsBefore);
        log.info("hasAccess: " + hasAccess);

        if (hasAccess) {
            String currentPlateNumber = eventDto.car_number;
            if (barrierInProcessingHashtable.containsKey(camera.getGate().getBarrier().getId()) && currentPlateNumber.equals(barrierInProcessingHashtable.get(camera.getGate().getBarrier().getId()))) {
                return;
            }
            barrierInProcessingHashtable.put(camera.getGate().getBarrier().getId(), currentPlateNumber);
            boolean openResult = false;
            try {
                openResult = true;
                if (camera.getGate().getBarrier().isStatusCheck()) {
                    AbstractOpenStrategy strategy = CarInEventStrategy.builder()
                            .camera(camera)
                            .eventDto(eventDto)
                            .properties(properties)
                            .whitelistCheckResults(whitelistCheckResults)
                            .enteredFromThisSecondsBefore(enteredFromThisSecondsBefore)
                            .build();
                    strategy.gateId = gate.gateId;
                    strategy.isWaitLoop = true;
                    SensorStatusCheckJob.add(strategy);
                } else {
                    openResult = barrierService.openBarrier(camera.getGate().getBarrier(), properties);
                }
            } catch (Throwable e) {
                log.info("Error opening barrier: " + e.getMessage());
                String descriptionRu = "Ошибка открытия шлагбаума: На контроллер шлагбаума не удалось присвоит значение на открытие для авто " + eventDto.car_number + " на " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезд" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "выезд" : "въезд/выезд")) + " для " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName();
                String descriptionEn = "Error while barrier open: Cannot assign value to open barrier for car " + eventDto.car_number + " to " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "pass" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName();
                String descriptionDe = "Fehler beim Öffnen der Schranke: Wert für offene Schranke für Auto kann nicht zugewiesen werden " + eventDto.car_number + " an " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "passieren" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "ausfahrt" : "einfahrt/ausfahrt")) + " " + camera.getGate().getDescription() + " parken " + camera.getGate().getParking().getName();
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Error, camera.getId(), eventDto.getCarNumberWithRegion(), descriptionRu, descriptionEn, descriptionDe);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, descriptionRu, descriptionEn, descriptionDe, EventLog.EventType.ERROR);
            }
            if (openResult) {
                gate.gateStatus = GateStatusDto.GateStatus.Open;
                gate.sensor1 = GateStatusDto.SensorStatus.Triggerred;
                gate.sensor2 = GateStatusDto.SensorStatus.WAIT;
                gate.directionStatus = GateStatusDto.DirectionStatus.FORWARD;
                gate.lastTriggeredTime = System.currentTimeMillis();
                if (Parking.ParkingType.PREPAID.equals(camera.getGate().getParking().getParkingType())) {
                    log.info("gate.isSimpleWhitelist: " + gate.isSimpleWhitelist);
                    gate.isSimpleWhitelist = false;
                }

                Boolean photoElementPassed;
                Boolean loopPassed;

                if(camera.getGate().getBarrier().isConfirmCarPass()){
                    photoElementPassed = false;
                    loopPassed = false;
                    int confirmPassTimeout = camera.getGate().getBarrier().getConfirmCarPassTimeout();
                    long currentMillis = System.currentTimeMillis();

                    while (currentPlateNumber.equals(barrierInProcessingHashtable.get(camera.getGate().getBarrier().getId())) && System.currentTimeMillis() - currentMillis <= confirmPassTimeout*1000 && (!photoElementPassed || !loopPassed)){
                        if(!photoElementPassed){
                            photoElementPassed = barrierService.getSensorStatus(StatusCheckJob.findGateStatusDtoById(camera.getGate().getId()).photoElement) == 1;
                        }
                        if(!loopPassed){
                            loopPassed = barrierService.getSensorStatus(StatusCheckJob.findGateStatusDtoById(camera.getGate().getId()).loop) == 1;
                        }
                    }
                } else {
                    photoElementPassed = true;
                    loopPassed = true;
                }

                if(photoElementPassed && loopPassed){
                    if (!gate.isSimpleWhitelist && !enteredFromThisSecondsBefore) {
                        saveCarInState(eventDto, camera, whitelistCheckResults, properties);
                    } else if (enteredFromThisSecondsBefore) {
                        properties.put("type", EventLog.StatusType.Allow);
                        eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.getCarNumberWithRegion(),
                                "Запускаем авто: Авто с гос. номером " + eventDto.car_number,
                                "Allow entrance: Car with license plate " + eventDto.car_number,
                                "Einfahrt gestatt: Auto mit Kennzeichen " + eventDto.car_number);
                        eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,
                                "Запускаем авто: Авто с гос. номером " + eventDto.car_number,
                                "Allow entrance: Car with license plate " + eventDto.car_number,
                                "Einfahrt gestatt: Auto mit Kennzeichen " + eventDto.car_number,
                                EventLog.EventType.PASS);
                    }
                } else {
                    properties.put("type", EventLog.StatusType.Error);
                    eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Error, camera.getId(), eventDto.getCarNumberWithRegion(),
                            "Отмена запуска авто: Авто с гос. номером " + eventDto.car_number,
                            "Cancel entrance: Car with license plate " + eventDto.car_number,
                            "Einfahrt verboten: Auto mit Kennzeichen " + eventDto.car_number);
                    eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,
                            "Отмена запуска авто: Авто с гос. номером " + eventDto.car_number,
                            "Cancel entrance: Car with license plate " + eventDto.car_number,
                            "Einfahrt verboten: Auto mit Kennzeichen " + eventDto.car_number,
                            EventLog.EventType.PASS);
                }
            } else {
                String descriptionRu = "Ошибка открытия шлагбаума: На контроллер шлагбаума не удалось присвоит значение на открытие для авто " + eventDto.car_number + " на " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезд" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "выезд" : "въезд/выезд")) + " для " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName();
                String descriptionEn = "Error while barrier open: Cannot assign value to open barrier for car " + eventDto.car_number + " to " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "pass" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName();
                String descriptionDe = "Fehler beim Öffnen der Schranke: Wert für offene Schranke für Auto kann nicht zugewiesen werden " + eventDto.car_number + " an " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "passieren" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "ausfahrt" : "einfahrt/ausfahrt")) + " " + camera.getGate().getDescription() + " parken " + camera.getGate().getParking().getName();
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Error, camera.getId(), eventDto.getCarNumberWithRegion(), descriptionRu, descriptionEn, descriptionDe);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, descriptionRu, descriptionEn, descriptionDe, EventLog.EventType.ERROR);
            }
            if(currentPlateNumber.equals(barrierInProcessingHashtable.get(camera.getGate().getBarrier().getId()))){
                barrierInProcessingHashtable.remove(camera.getGate().getBarrier().getId());
            }
        }

        if(tabloConnected){
            tabloService.updateOnIn(camera.getGate());
        }
    }

    @Override
    public void saveCarInState(CarEventDto eventDto, Camera camera, JsonNode whitelistCheckResults, Map<String, Object> properties) {
        PluginRegister carModelPluginRegister = pluginService.getPluginRegister(StaticValues.carmodelPlugin);
        String eventWithDimensionRu = "";
        String eventWithDimensionEn = "";
        String eventWithDimensionDe = "";
        if (carModelPluginRegister != null) {
            CarModel carModel = null;
            if (eventDto.car_model != null) {
                carModel = carModelRepository.getByModel(eventDto.car_model);
            }
            String dimension;
            if (carModel != null && carModel.getType() == 1) {
                dimension = "легковой";
            } else if (carModel != null && carModel.getType() == 2) {
                dimension = "микро автобус";
            } else if (carModel != null && carModel.getType() == 3) {
                dimension = "грузовик";
            } else {
                dimension = "не распознан";
            }
            eventWithDimensionRu = ", габарит: " + dimension;
            eventWithDimensionEn = ", dimension: " + dimension;
            eventWithDimensionDe = ", Grösse: " + dimension;

        }
        if (Parking.ParkingType.WHITELIST.equals(camera.getGate().getParking().getParkingType())) {
            properties.put("type", EventLog.StatusType.Allow);
            String message_ru = "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " в рамках белого листа" + eventWithDimensionRu;
            String message_en = "Allowed: Found valid paid permit for car late number " + eventDto.car_number;
            String message_de = "Erlaubt: Gültige bezahlte Genehmigung für das Kennzeichen gefunden " + eventDto.car_number;
            if (whitelistCheckResults == null) {
                message_ru = "Пропускаем авто: Найден действующий абонемент на номер авто " + eventDto.car_number;
                message_en = "Allowed: Found valid paid permit for car late number " + eventDto.car_number;
                message_de = "Erlaubt: Gültige bezahlte Genehmigung für das Kennzeichen gefunden " + eventDto.car_number;
            }
            carStateService.createINState(eventDto.car_number, eventDto.event_date_time, camera, false, whitelistCheckResults != null ? whitelistCheckResults.toString() : null, properties.containsKey(StaticValues.carSmallImagePropertyName) ? properties.get(StaticValues.carSmallImagePropertyName).toString() : null);
            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.getCarNumberWithRegion(), message_ru, message_en, message_de);
            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, message_ru, message_en, message_de, EventLog.EventType.WHITELIST);
        } else if (Parking.ParkingType.PREPAID.equals(camera.getGate().getParking().getParkingType())) {
            properties.put("type", EventLog.StatusType.Allow);
            carStateService.createINState(eventDto.car_number, eventDto.event_date_time, camera, false, null, properties.containsKey(StaticValues.carSmallImagePropertyName) ? properties.get(StaticValues.carSmallImagePropertyName).toString() : null);
            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.getCarNumberWithRegion(),
                    "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " по предоплате" + eventWithDimensionRu,
                    "Permitted: Car with number " + eventDto.car_number + " on prepaid basis" + eventWithDimensionEn,
                    "Erlaubt: Auto mit Kennzeichen  " + eventDto.car_number + " auf Prepaid-Basis" + eventWithDimensionDe);
            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,
                    "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " по предоплате" + eventWithDimensionRu,
                    "Permitted: Car with number " + eventDto.car_number + " on prepaid basis" + eventWithDimensionEn,
                    "Erlaubt: Auto mit Kennzeichen  " + eventDto.car_number + " auf Prepaid-Basis" + eventWithDimensionDe,
                    EventLog.EventType.PREPAID);
        } else {
            if (whitelistCheckResults == null) {
                properties.put("type", EventLog.StatusType.Allow);
                carStateService.createINState(eventDto.car_number, eventDto.event_date_time, camera, true, null, properties.containsKey(StaticValues.carSmallImagePropertyName) ? properties.get(StaticValues.carSmallImagePropertyName).toString() : null);
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.getCarNumberWithRegion() + (eventDto.vecihleType != null ? "[" + eventDto.vecihleType + "]" : ""),
                        "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " на платной основе" + eventWithDimensionRu,
                        "Permitted: Car with number " + eventDto.car_number + " on paid basis" + eventWithDimensionEn,
                        "Erlaubt: Auto mit Kennzeichen  " + eventDto.car_number + " auf bezahlter Basis" + eventWithDimensionDe);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,
                        "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " на платной основе" + eventWithDimensionRu,
                        "Permitted: Car with number " + eventDto.car_number + " on paid basis" + eventWithDimensionEn,
                        "Erlaubt: Auto mit Kennzeichen  " + eventDto.car_number + " auf bezahlter Basis" + eventWithDimensionDe,
                        EventLog.EventType.PAID);
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
                    carStateService.createINState(eventDto.car_number, eventDto.event_date_time, camera, false, whitelistCheckResults != null ? whitelistCheckResults.toString() : null, properties.containsKey(StaticValues.carSmallImagePropertyName) ? properties.get(StaticValues.carSmallImagePropertyName).toString() : null);
                    eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.getCarNumberWithRegion(),
                            "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " в рамках белого листа" + eventWithDimensionRu,
                            "Permitted: Car with number " + eventDto.car_number + " from free permits " + eventWithDimensionEn,
                            "Erlaubt: Fahrzeug mit speziellen Kennzeichen  " + eventDto.car_number + " von freien Genehmigungen " + eventWithDimensionDe);
                    eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,
                            "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " в рамках белого листа" + eventWithDimensionRu,
                            "Permitted: Car with number " + eventDto.car_number + " from free permits " + eventWithDimensionEn,
                            "Erlaubt: Fahrzeug mit speziellen Kennzeichen  " + eventDto.car_number + " von freien Genehmigungen " + eventWithDimensionDe,
                            EventLog.EventType.WHITELIST);
                } else {
                    properties.put("type", EventLog.StatusType.Allow);
                    carStateService.createINState(eventDto.car_number, eventDto.event_date_time, camera, true, whitelistCheckResults != null ? whitelistCheckResults.toString() : null, properties.containsKey(StaticValues.carSmallImagePropertyName) ? properties.get(StaticValues.carSmallImagePropertyName).toString() : null);
                    eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.getCarNumberWithRegion(),
                            "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " на платной основе. Все места для группы " + nodeDetails.get("groupName").textValue() + " заняты следующим списком " + nodeDetails.get("placeOccupiedCars").toString() + eventWithDimensionRu,
                            "Permitted: Car with license number " + eventDto.car_number + " on paid basis. " + (nodeDetails.has("placeName") ? nodeDetails.get("placeName") + " spot " : "Все места ") + "for the group " + nodeDetails.get("groupName").textValue() + " taken by the next list " + nodeDetails.get("placeOccupiedCars").toString() + eventWithDimensionEn,
                            "Erlaubt: Fahrzeug mit speziellen Kennzeichen " + eventDto.car_number + " auf bezahlter Basis. " + (nodeDetails.has("placeName") ? nodeDetails.get("placeName") + " Platz " : "Alle Räume ") + "für die Gruppe " + nodeDetails.get("groupName").textValue() + " von der nächsten Liste genommen " + nodeDetails.get("placeOccupiedCars").toString() + eventWithDimensionDe);
                    eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,
                            "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " на платной основе. " + (nodeDetails.has("placeName") ? nodeDetails.get("placeName") + " место " : "Все места ") + "для группы " + nodeDetails.get("groupName").textValue() + " заняты следующим списком " + nodeDetails.get("placeOccupiedCars").toString() + eventWithDimensionRu,
                            "Permitted: Car with license number " + eventDto.car_number + " on paid basis. " + (nodeDetails.has("placeName") ? nodeDetails.get("placeName") + " spot " : "Все места ") + "for the group " + nodeDetails.get("groupName").textValue() + " taken by the next list " + nodeDetails.get("placeOccupiedCars").toString() + eventWithDimensionEn,
                            "Erlaubt: Fahrzeug mit speziellen Kennzeichen " + eventDto.car_number + " auf bezahlter Basis. " + (nodeDetails.has("placeName") ? nodeDetails.get("placeName") + " Platz " : "Alle Räume ") + "für die Gruppe " + nodeDetails.get("groupName").textValue() + " von der nächsten Liste genommen " + nodeDetails.get("placeOccupiedCars").toString() + eventWithDimensionDe,
                            EventLog.EventType.PAID);
                }
            }
        }
    }

    private boolean checkWhiteList(CarEventDto eventDto, Camera camera, Map<String, Object> properties, JsonNode whitelistCheckResults, Boolean sendEventMessage) throws Exception {
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
            } else if (checkBooking(eventDto.car_number, eventDto.lp_region, "1")) {
                properties.put("type", EventLog.StatusType.Allow);
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.getCarNumberWithRegion(),
                        "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " имеется валидный пропуск.",
                        "Allow entrance: Car with plate number " + eventDto.car_number + " has valid booking.",
                        "Einfahrt gestatt: Auto mit Kennzeichen " + eventDto.car_number + " hat eine gültige Reservierung.");
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,
                        "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " имеется валидный пропуск.",
                        "Allow entrance: Car with plate number " + eventDto.car_number + " has valid booking.",
                        "Einfahrt gestatt: Auto mit Kennzeichen " + eventDto.car_number + " hat eine gültige Reservierung.",
                        EventLog.EventType.BOOKING_PASS);
                return true;
            } else if (sendEventMessage) {
                properties.put("type", EventLog.StatusType.Deny);
                String normalList = node.get("placeOccupiedCars").toString().substring(2).replaceAll("\"", "").replaceFirst("]", "");
                String description = "В проезде отказано: Все места в для группы " + node.get("groupName").textValue() + " заняты следующим списком " + normalList + ". Авто " + eventDto.car_number;
                String description_en = "Not allowed: All parking lost for the groups " + node.get("groupName").textValue() + " were taken by the next list " + normalList + ". Car " + eventDto.car_number;
                String description_de = "Nicht erlaubt: Alle für die Gruppen " + node.get("groupName").textValue() + " verlorenen Parkplätze wurden von der nächsten Liste genommen " + normalList + ". Auto " + eventDto.car_number;
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Deny, camera.getId(), eventDto.getCarNumberWithRegion(), description, description_en, description_de);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, description, description_en, description_de, EventLog.EventType.NOT_PASS);
                return false;
            }
        } else if (checkBooking(eventDto.car_number, eventDto.lp_region, "1")) {
            log.info(eventDto.car_number + ": booking check booking return true");
            properties.put("type", EventLog.StatusType.Allow);
            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.getCarNumberWithRegion(),
                    "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " имеется валидный пропуск.",
                    "Allow entrance: Car with plate number " + eventDto.car_number + " has valid booking.",
                    "Einfahrt gestatt: Auto mit Kennzeichen " + eventDto.car_number + " hat eine gültige Reservierung.");
            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,
                    "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " имеется валидный пропуск.",
                    "Allow entrance: Car with plate number " + eventDto.car_number + " has valid booking.",
                    "Einfahrt gestatt: Auto mit Kennzeichen " + eventDto.car_number + " hat eine gültige Reservierung.",
                    EventLog.EventType.BOOKING_PASS);
            return true;
        } else if (sendEventMessage) {
            properties.put("type", EventLog.StatusType.Deny);
            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Deny, camera.getId(), eventDto.getCarNumberWithRegion(),
                    "В проезде отказано: Нет доступа для въезда " + eventDto.car_number,
                    "Not allowed to enter:  No entry access " + eventDto.car_number,
                    "Eintritt nicht erlaubt:  Keine Einfahrt möglich " + eventDto.car_number);
            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,
                    "В проезде отказано: Нет доступа для въезда " + eventDto.car_number,
                    "Not allowed to enter:  No entry access " + eventDto.car_number,
                    "Eintritt nicht erlaubt:  Keine Einfahrt möglich " + eventDto.car_number,
                    EventLog.EventType.NOT_PASS);
            return false;
        }
        return false;
    }

    private boolean checkBooking(String plateNumber, String region, String position) throws Exception {
        PluginRegister bookingPluginRegister = pluginService.getPluginRegister(StaticValues.bookingPlugin);
        if (bookingPluginRegister != null) {
            ObjectNode node = this.objectMapper.createObjectNode();
            node.put("platenumber", plateNumber);
            node.put("position", position);
            node.put("region", region);
            node.put("command", "checkBooking");
            JsonNode result = bookingPluginRegister.execute(node);
            return result.get("bookingResult").booleanValue();
        } else {
            return false;
        }
    }

    private boolean checkSimpleWhiteList(CarEventDto eventDto, Camera camera, Map<String, Object> properties, JsonNode whitelistCheckResults, Boolean sendEventMessage) {

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
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.getCarNumberWithRegion(),
                        "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " в рамках белого листа",
                        "Permitted: Car with number " + eventDto.car_number + " from free permits",
                        "Erlaubt: Auto mit Kennzeichen  " + eventDto.car_number + " von freien Genehmigungen");
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,
                        "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " в рамках белого листа",
                        "Permitted: Car with number " + eventDto.car_number + " from free permits",
                        "Erlaubt: Fahrzeug mit speziellen Kennzeichen  " + eventDto.car_number + " von freien Genehmigungen",
                        EventLog.EventType.WHITELIST);
            } else if (sendEventMessage) {
                properties.put("type", EventLog.StatusType.Deny);
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Deny, camera.getId(), eventDto.getCarNumberWithRegion(),
                        "В проезде отказано: Все места в для группы " + customDetails.get("groupName").textValue() + " заняты следующим списком " + customDetails.get("placeOccupiedCars").toString() + ". Авто " + eventDto.car_number,
                        "Not allowed: All spots for the group " + customDetails.get("groupName").textValue() + " taken by the next list " + customDetails.get("placeOccupiedCars").toString() + ". Car " + eventDto.car_number,
                        "Nicht erlaubt: Alle für die Gruppen " + customDetails.get("groupName").textValue() + " von der nächsten Liste genommen " + customDetails.get("placeOccupiedCars").toString() + ". Auto " + eventDto.car_number);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,
                        "В проезде отказано: Все места в для группы " + customDetails.get("groupName").textValue() + " заняты следующим списком " + customDetails.get("placeOccupiedCars").toString() + ". Авто " + eventDto.car_number,
                        "Not allowed: All spots for the group " + customDetails.get("groupName").textValue() + " taken by the next list " + customDetails.get("placeOccupiedCars").toString() + ". Car " + eventDto.car_number,
                        "Nicht erlaubt: Alle für die Gruppen " + customDetails.get("groupName").textValue() + " von der nächsten Liste genommen " + customDetails.get("placeOccupiedCars").toString() + ". Auto " + eventDto.car_number,
                        EventLog.EventType.NOT_PASS);
            }
        } else if (sendEventMessage) {
            properties.put("type", EventLog.StatusType.Deny);
            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Deny, camera.getId(), eventDto.getCarNumberWithRegion(),
                    "В проезде отказано: Авто не найдено в белом листе " + eventDto.car_number,
                    "Not allowed: Car not found in free permits " + eventDto.car_number,
                    "Betreten nicht erlaubt: Auto nicht in freien Genehmigungen gefunden " + eventDto.car_number);
            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,
                    "В проезде отказано: Авто не найдено в белом листе " + eventDto.car_number,
                    "Not allowed: Car not found in free permits " + eventDto.car_number,
                    "Betreten nicht erlaubt: Auto nicht in freien Genehmigungen gefunden " + eventDto.car_number,
                    EventLog.EventType.NOT_PASS);
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
            eventLogService.createEventLog("Balance", null, properties,
                    "Плагин биллинга не найден или не запущен. Авто с гос. номером " + car_number,
                    "Plugin for billing not found or not launched. Car with license plate " + car_number,
                    "Plugin für Abrechnung nicht gefunden oder nicht gestartet. Auto mit speziellen Kennzeichen " + car_number);
        }
        return currentBalanceResult;
    }

    private JsonNode decreaseBalance(String carNumber, String parkingName, Long carStateId, BigDecimal amount, Map<String, Object> properties) throws Exception {
        ObjectNode node = this.objectMapper.createObjectNode();
        JsonNode decreaseBalanceResult = null;
        node.put("command", "decreaseCurrentBalance");
        node.put("amount", amount);
        node.put("plateNumber", carNumber);
        node.put("parkingName", parkingName);
        node.put("reason", "Оплата паркинга " + parkingName);
        node.put("reasonEn", "Payment for parking " + parkingName);
        node.put("provider", "Parking fee");
        if (carStateId != null) {
            node.put("carStateId", carStateId);
        }

        PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
        if (billingPluginRegister != null) {
            decreaseBalanceResult = billingPluginRegister.execute(node);
        } else {
            properties.put("type", EventLog.StatusType.Error);
            eventLogService.createEventLog("Balance", null, properties,
                    "Плагин биллинга не найден или не запущен. Авто с гос. номером " + carNumber,
                    "Plugin for billing not found or not launched. Car with license plate " + carNumber,
                    "Plugin für Abrechnung nicht gefunden oder nicht gestartet. Auto mit speziellen Kennzeichen " + carNumber);
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
            eventLogService.createEventLog("ParkingRate", null, properties,
                    "Плагин тарифа не найден или не запущен",
                    "Plugin for rate not found or not launched",
                    "Plugin für Tarif nicht gefunden oder nicht gestartet");
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
        boolean hasAccess = false;
        CarState carState = null;
        BigDecimal balance = BigDecimal.ZERO;
        BigDecimal rateResult = null;
        BigDecimal zerotouchValue = null;
        Boolean paidByThirdParty = false;
        StaticValues.CarOutBy carOutBy = null;
        Boolean leftFromThisSecondsBefore = false;
        JsonNode whitelists = null;
        JsonNode abonements = null;

        if (gate.notControlBarrier != null && gate.notControlBarrier) {
            carState = carStateService.getLastNotLeft(eventDto.car_number);
            carOutBy = StaticValues.CarOutBy.REGISTER;
            saveCarOutState(eventDto, camera, carState, properties, balance, rateResult, zerotouchValue, format, carOutBy, abonements, whitelists);
            return;
        }

        try {
            qrPanelService.clear(camera.getGate());
            try {
                ObjectNode socketMessage = this.objectMapper.createObjectNode();
                socketMessage.put("carnumber", eventDto.car_number);
                socketMessage.put("action", "clear");
                socketMessage.put("gate", camera.getGate().getId());
                eventLogService.sendSocketMessage("qrpanel", socketMessage.toString());
            } catch (Exception ex) {

            }
        } catch (Exception ex) {
            log.log(Level.WARNING, "Error while clearing qrpanel for gate " + gate.gateName);
        }
        // проверить если машины заезжала, по умолчанию 20 секунд назад
        Calendar now = Calendar.getInstance();
        now.add(Calendar.SECOND, (-1) * parkingIgnoreEnteredSeconds);
        Boolean hasLeft = carStateService.getIfHasLastFromOtherCamera(eventDto.car_number, eventDto.ip_address, now.getTime());
        if (hasLeft) {
            return;
        }

        // Проверка абонемента на истечение срока или близко к истечению срока
        abonomentService.checkAbonementExpireDate(eventDto.car_number, camera.getId(), gate.parkingId, properties);

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
                    carState = new CarState();
                    Parking parking = new Parking();
                    parking.setId(gate.parkingId);
                    carState.setParking(parking);
                    carState.setInTimestamp(eventDto.event_date_time);
                    abonements = abonomentService.getAbonomentsDetails(eventDto.car_number, carState, format);

                    if (Parking.ParkingType.WHITELIST.equals(camera.getGate().getParking().getParkingType())) {
                        if (bookingCheckOut) {
                            hasAccess = checkBooking(eventDto.car_number, eventDto.lp_region, "2");
                            if (hasAccess) {
                                properties.put("type", EventLog.StatusType.Allow);
                                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.getCarNumberWithRegion(),
                                        "Не найдена запись автомобиля о въезде с гос. номером " + eventDto.car_number + ". Для белого списка выезд разрешен.",
                                        "Entering record not found. Car with license plate " + eventDto.car_number + ". For free permits exit is allowed",
                                        "Eingehender Datensatz nicht gefunden. Auto mit speziellen Kennzeichen " + eventDto.car_number + ". Bei freien Genehmigungen ist die Ausfahrt erlaubt");
                                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,
                                        "Не найдена запись автомобиля о въезде с гос. номером " + eventDto.car_number + ". Для белого списка выезд разрешен.",
                                        "Entering record not found. Car with license plate " + eventDto.car_number + ". For free permits exit is allowed",
                                        "Eingehender Datensatz nicht gefunden. Auto mit speziellen Kennzeichen " + eventDto.car_number + ". Bei freien Genehmigungen ist die Ausfahrt erlaubt",
                                        EventLog.EventType.BOOKING_PASS);
                                carOutBy = StaticValues.CarOutBy.WHITELIST;
                            } else {
                                properties.put("type", EventLog.StatusType.Deny);
                                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.NotFound, camera.getId(), eventDto.getCarNumberWithRegion(),
                                 "Не найдена запись автомобиля о въезде с гос. номером " + eventDto.car_number + ". Выезд запрещен.",
                                  "Entering record not found. Car with license plate " + eventDto.car_number + ". Exit is prohibited",
                                  "Eingehender Datensatz nicht gefunden. Auto mit speziellen Kennzeichen " + eventDto.car_number + ". Ausfahrt ist nicht erlaubt");
                                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,
                                 "Не найдена запись автомобиля о въезде с гос. номером " + eventDto.car_number + ". Выезд запрещен.",
                                  "Entering record not found. Car with license plate " + eventDto.car_number + ". Exit is prohibited",
                                  "Eingehender Datensatz nicht gefunden. Auto mit speziellen Kennzeichen " + eventDto.car_number + ". Ausfahrt ist nicht erlaubt",
                                EventLog.EventType.NOT_PASS);
                            }
                        } else {
                            ArrayNode whitelistCheckResultArray = (ArrayNode) getWhiteLists(camera.getGate().getParking().getId(), eventDto.getCarNumberWithRegion().trim(), new Date(), format, properties);
                            if (whitelistCheckResultArray != null && whitelistCheckResultArray.size() > 0) {
                                properties.put("type", EventLog.StatusType.Allow);
                                String description = "Не найдена запись автомобиля о въезде с гос. номером " + eventDto.car_number + ". Для белого листа выезд разрешен.";
                                String descriptionEn = "No record found about entering. Car with license number " + eventDto.car_number + ". For free permits exit is allowed";
                                String descriptionDe = "Eingehender Datensatz nicht gefunden. Auto mit speziellen Kennzeichen " + eventDto.car_number + ". Bei freien Genehmigungen ist die Ausfahrt erlaubt";
                                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.getCarNumberWithRegion(), description, descriptionEn, descriptionDe);
                                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, description, descriptionEn, descriptionDe, EventLog.EventType.WHITELIST_OUT);
                                hasAccess = true;
                            } else {
                                properties.put("type", EventLog.StatusType.NotFound);
                                String description = "Не найдена запись автомобиля о въезде с гос. номером " + eventDto.car_number + ". Выезд запрещен.";
                                String descriptionEn = "Entering record not found. Car with license plate " + eventDto.car_number + ". Exit is prohibited";
                                String descriptionDe = "Eingehender Datensatz nicht gefunden. Auto mit speziellen Kennzeichen " + eventDto.car_number + ". Bei freien Genehmigungen ist die Ausfahrt erlaubt";
                                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.NotFound, camera.getId(), eventDto.getCarNumberWithRegion(), description, descriptionEn, descriptionDe);
                                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, description, descriptionEn,descriptionDe, EventLog.EventType.NOT_PASS);
                                hasAccess = false;
                            }
                        }

                    } else if (Parking.ParkingType.PREPAID.equals(camera.getGate().getParking().getParkingType())) {
                        properties.put("type", EventLog.StatusType.Allow);
                        eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.getCarNumberWithRegion(),
                                "По пердоплате выезд разрешен.. Авто с гос. номером " + eventDto.car_number,
                                "For prepaid exit is allowed. Car with license plate " + eventDto.car_number,
                                "Für vorausbezahlte Ausfahrt ist erlaubt. Auto mit speziellen Kennzeichen " + eventDto.car_number);
                        eventLogService.createEventLog(CarState.class.getSimpleName(), null, properties,
                                "По пердоплате выезд разрешен.. Авто с гос. номером " + eventDto.car_number,
                                "For prepaid exit is allowed. Car with license plate " + eventDto.car_number,
                                "Für vorausbezahlte Ausfahrt ist erlaubt. Auto mit speziellen Kennzeichen " + eventDto.car_number,
                                EventLog.EventType.PREPAID);
                        carOutBy = StaticValues.CarOutBy.PREPAID;
                        hasAccess = true;
                    } else if (parkingOnlyRegisterCars) {
                        carOutBy = StaticValues.CarOutBy.REGISTER;
                        hasAccess = true;
                    } else if (parkingHasAccessUnknownCases) {
                        properties.put("type", EventLog.StatusType.Allow);
                        eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Deny, camera.getId(), eventDto.getCarNumberWithRegion(),
                                "Не найдена запись автомобиля о въезде с гос. номером " + eventDto.car_number + ". Для этого паркинга выезд разрешен.",
                                "No record found about entering. Car with license number " + eventDto.car_number + ". For this parking exit is allowed",
                                "Kein Datensatz über die Einfahrt eines Fahrzeugs mit speziellen Kennzeichen gefunden " + eventDto.car_number + ". Für diesen Parkplatz ist die Ausfahrt erlaubt");
                        eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,
                                "Не найдена запись автомобиля о въезде с гос. номером " + eventDto.car_number + ". Для этого паркинга выезд разрешен.",
                                "No record found about entering. Car with license number " + eventDto.car_number + ". For this parking exit is allowed",
                                "Kein Datensatz über die Einfahrt eines Fahrzeugs mit speziellen Kennzeichen gefunden " + eventDto.car_number + ". Für diesen Parkplatz ist die Ausfahrt erlaubt",
                                EventLog.EventType.PASS);
                        hasAccess = true;
                    } else if (abonements != null && abonements.isArray() && abonements.size() > 0) {
                        PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
                        if (billingPluginRegister != null) {
                            ObjectNode billinNode = this.objectMapper.createObjectNode();
                            billinNode.put("command", "getCurrentBalance");
                            billinNode.put("plateNumber", eventDto.car_number);
                            JsonNode billingResult = billingPluginRegister.execute(billinNode);
                            balance = billingResult.get("currentBalance").decimalValue().setScale(2);
                            if (balance.compareTo(BigDecimal.ZERO) < 0) {
                                properties.put("type", EventLog.StatusType.Debt);
                                String descriptionRu = "В проезде отказано: Авто " + eventDto.car_number + " имеет задолженность " + balance;
                                String descriptionEn = "Not allowed to enter: Car " + eventDto.car_number + " is in debt " + balance;
                                String descriptionDe = "Betreten nicht erlaubt: Auto " + eventDto.car_number + " ist verschuldet " + balance;
                                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Debt, camera.getId(), eventDto.getCarNumberWithRegion(), descriptionRu, descriptionEn, descriptionDe);
                                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, descriptionRu, descriptionEn, descriptionDe, EventLog.EventType.DEBT);
                            } else {
                                carOutBy = StaticValues.CarOutBy.ABONOMENT_WO_ENTRY;
                                carState = carStateService.createCarStateOutWhenNoEntryRecord(eventDto.car_number, eventDto.event_date_time, camera, true,
                                        properties.containsKey(StaticValues.carSmallImagePropertyName) ? properties.get(StaticValues.carSmallImagePropertyName).toString() : null);
                                carStateService.setAbonomentDetails(carState.getId(), abonements);
                                carState.setCarOutType(CarState.CarOutType.ABONEMENT_PASS);
                                hasAccess = true;
                            }
                        }
                    } else {
                        properties.put("type", EventLog.StatusType.Deny);
                        eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.NotFound, camera.getId(), eventDto.getCarNumberWithRegion(),
                         "Не найдена запись автомобиля о въезде с гос. номером " + eventDto.car_number + ". Для платного паркинга выезд запрещен.",
                          "No record found about entering. Car with license number " + eventDto.car_number + ". For paid parking, exit is prohibited",
                          "Kein Datensatz über die Einfahrt eines Fahrzeugs mit speziellen Kennzeichen gefunden " + eventDto.car_number + ". Für gebührenpflichtige Parkplätze ist die Ausfahrt verboten");
                        eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,
                        "Не найдена запись автомобиля о въезде с гос. номером " + eventDto.car_number + ". Для платного паркинга выезд запрещен.",
                        "No record found about entering. Car with license number " + eventDto.car_number + ". For paid parking, exit is prohibited",
                        "Kein Datensatz über die Einfahrt eines Fahrzeugs mit speziellen Kennzeichen gefunden " + eventDto.car_number + ". Für gebührenpflichtige Parkplätze ist die Ausfahrt verboten",
                        EventLog.EventType.NOT_PASS);
                        hasAccess = false;
                    }
                }
            } else {
                CarState lastLeft = carStateService.getIfLastLeft(eventDto.car_number, eventDto.ip_address);
                if (lastLeft == null || System.currentTimeMillis() - lastLeft.getOutTimestamp().getTime() > 1000 * 60) { // Повторное прием фотособытии на выезд и если выехал 1 мин назад
                    PluginRegister megaPluginRegister = pluginService.getPluginRegister((StaticValues.megaPlugin));
                    if (megaPluginRegister != null) {
                        ObjectNode node = this.objectMapper.createObjectNode();
                        node.put("command", "checkInThirdPartyPayment");
                        node.put("plateNumber", carState.getCarNumber());
                        JsonNode nodeResult = megaPluginRegister.execute(node);
                        paidByThirdParty = nodeResult.get("paidByThirdParty").booleanValue();
                    }
                    whitelists = whitelistRootService.getValidWhiteListsInPeriod(carState.getParking().getId(), eventDto.car_number, carState.getInTimestamp(), eventDto.event_date_time, format);

                    if (Parking.ParkingType.PREPAID.equals(camera.getGate().getParking().getParkingType())) {
                        hasAccess = true;
                        carOutBy = StaticValues.CarOutBy.PREPAID;
                    } else if (parkingOnlyRegisterCars) {
                        hasAccess = true;
                        carOutBy = StaticValues.CarOutBy.REGISTER;
                    } else if (Parking.ParkingType.WHITELIST.equals(camera.getGate().getParking().getParkingType())) {
                        ArrayNode whitelistCheckResultArray = (ArrayNode) getWhiteLists(camera.getGate().getParking().getId(), eventDto.car_number, new Date(), format, properties);
                        if (whitelistCheckResultArray != null && whitelistCheckResultArray.size() > 0) {
                            hasAccess = true;
                            carOutBy = StaticValues.CarOutBy.WHITELIST;
                        } else if (bookingCheckOut) {
                            hasAccess = checkBooking(eventDto.car_number, eventDto.lp_region, "2");
                            if (hasAccess) {
                                hasAccess = true;
                                carOutBy = StaticValues.CarOutBy.BOOKING;
                            } else {
                                properties.put("type", EventLog.StatusType.Deny);
                                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Deny, camera.getId(), eventDto.getCarNumberWithRegion(),
                                        "В проезде отказано: Нет доступа для выезда " + eventDto.car_number,
                                        "Not allowed to exit:  No entry access " + eventDto.car_number,
                                        "Ausfahrt nicht erlaubt:  Keine Einfahrt möglich " + eventDto.car_number);

                                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,
                                        "В проезде отказано: Нет доступа для выезда " + eventDto.car_number,
                                        "Not allowed to exit:  No entry access " + eventDto.car_number,
                                        "Ausfahrt nicht erlaubt:  Keine Einfahrt möglich " + eventDto.car_number,
                                        EventLog.EventType.NOT_PASS);
                                hasAccess = false;
                            }
                        } else {
                            hasAccess = true;
                            carOutBy = StaticValues.CarOutBy.WHITELIST;
                        }
                    } else {
                        if (carState.getWhitelistJson() == null && carState.getPaid() != null && !carState.getPaid()) {
                            carOutBy = StaticValues.CarOutBy.FREE;
                            hasAccess = true;
                        } else {
                            PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
                            if (billingPluginRegister != null) {
                                ObjectNode billinNode = this.objectMapper.createObjectNode();
                                billinNode.put("command", "getCurrentBalance");
                                billinNode.put("plateNumber", carState.getCarNumber());

                                JsonNode billingResult = billingPluginRegister.execute(billinNode);
                                balance = billingResult.get("currentBalance").decimalValue().setScale(2);

                                log.info("whitelists: " + whitelists);
                                if (whitelists != null && whitelists.isArray() && whitelists.size() > 0) {
                                    carOutBy = StaticValues.CarOutBy.WHITELIST;
                                    rateResult = calculateWhitelistExtraPayment(camera, carState, eventDto, whitelists, format, properties);
                                    if (balance.compareTo(rateResult) >= 0) {
                                        hasAccess = true;
                                    } else {
                                        properties.put("type", EventLog.StatusType.Deny);
                                        String description = "В проезде отказано: Не достаточно средств для списания оплаты за паркинг вне рамки белого списка. Сумма к оплате: " + rateResult + ". Баланс: " + balance;
                                        String descriptionEn = "Not allowed to exit: Not enough balance to pay for parking outside the scope of the free permits. Total sum to pay: " + rateResult + ". Balance: " + balance;
                                        String descriptionDe = "Ausfahrt nicht erlaubt: Das Guthaben reicht nicht aus, um das Parken außerhalb des Geltungsbereichs der kostenlosen Parkscheine zu bezahlen. Gesamtbetrag zu zahlen: " + rateResult + ". Saldo: " + balance;
                                        eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Deny, camera.getId(), eventDto.getCarNumberWithRegion(), description, descriptionEn, descriptionDe);
                                        eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, description, descriptionEn, descriptionDe, EventLog.EventType.NOT_ENOUGH_BALANCE);
                                        hasAccess = false;
                                    }
                                    log.info("whitelists hasAccess = " + hasAccess);
                                } else {
                                    abonements = abonomentService.getAbonomentsDetails(eventDto.car_number, carState, format);
                                    if (abonements != null && abonements.isArray() && abonements.size() > 0) {
                                        carOutBy = StaticValues.CarOutBy.ABONOMENT;
                                        rateResult = calculateAbonomentExtraPayment(camera, carState, eventDto, abonements, format, properties);
                                        if (balance.compareTo(rateResult) >= 0) {
                                            hasAccess = true;
                                        } else {
                                            properties.put("type", EventLog.StatusType.Deny);
                                            String description = "В проезде отказано: Не достаточно средств для списания оплаты за паркинг вне рамки абонемента. Сумма к оплате: " + rateResult + ". Баланс: " + balance;
                                            String descriptionEn = "Not allowed to exit: Not enough balance to pay for parking outside the scope of the paid permit. Total sum to pay: " + rateResult + ". Balance: " + balance;
                                            String descriptionDe = "Not allowed to exit: Not enough balance to pay for parking outside the scope of the paid permit. Total sum to pay: " + rateResult + ". Balance: " + balance;
                                            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Deny, camera.getId(), eventDto.getCarNumberWithRegion(), description, descriptionEn, descriptionDe);
                                            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, description, descriptionEn, descriptionDe, EventLog.EventType.NOT_ENOUGH_BALANCE);
                                            hasAccess = false;
                                        }
                                        log.info("abonements hasAccess = " + hasAccess);
                                    } else {
                                        rateResult = calculateRate(carState.getInTimestamp(), eventDto.event_date_time, camera, carState, eventDto, format, properties, false);
                                        if (rateResult == null) {
                                            properties.put("type", EventLog.StatusType.Error);
                                            String descriptionRu = "Ошибка расчета плагина вычисления стоимости парковки. Авто с гос. номером " + eventDto.car_number;
                                            String descriptionEn = "Error calculating the parking cost calculation plugin. Car with number " + eventDto.car_number;
                                            String descriptionDe = "Fehler bei der Berechnung des Parkkostenberechnungssteckers. Auto mit Kennzeichen " + eventDto.car_number;
                                            eventLogService.createEventLog("Rate", null, properties, descriptionRu, descriptionEn, descriptionDe, EventLog.EventType.ERROR);
                                            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Deny, camera.getId(), eventDto.getCarNumberWithRegion(), descriptionRu, descriptionEn, descriptionDe);
                                            hasAccess = false;
                                        } else if (BigDecimal.ZERO.compareTo(rateResult) == 0) {
                                            if(BigDecimal.ZERO.compareTo(balance) > 0){
                                                properties.put("type", EventLog.StatusType.Debt);
                                                String descriptionRu = "В проезде отказано: Авто " + eventDto.car_number + " имеет задолженность " + balance;
                                                String descriptionEn = "Not allowed to enter: Car " + eventDto.car_number + " is in debt " + balance;
                                                String descriptionDe = "Betreten nicht erlaubt: Auto " + eventDto.car_number + " ist verschuldet " + balance;
                                                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Debt, camera.getId(), eventDto.getCarNumberWithRegion(), descriptionRu, descriptionEn, descriptionDe);
                                                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, descriptionRu, descriptionEn, descriptionDe, EventLog.EventType.DEBT);
                                                hasAccess = false;
                                            } else {
                                                carOutBy = StaticValues.CarOutBy.PAYMENT_PROVIDER;
                                                hasAccess = true;
                                            }
                                        } else if (paidByThirdParty) {
                                            ObjectNode nodeThPP = this.objectMapper.createObjectNode();
                                            nodeThPP.put("command", "sendPaymentToThPP");
                                            nodeThPP.put("plateNumber", carState.getCarNumber());
                                            String entryDate = format.format(carState.getInTimestamp());
                                            String exitDate = format.format(eventDto.event_date_time);
                                            nodeThPP.put("entryDate", entryDate);
                                            nodeThPP.put("exitDate", exitDate);
                                            nodeThPP.put("rateAmount", rateResult);
                                            nodeThPP.put("parkingUid", parkingUid);
                                            nodeThPP.put("thPPUrl", thirdPartyPaymentUrl);
                                            megaPluginRegister.execute(nodeThPP);
                                            carOutBy = StaticValues.CarOutBy.THIRD_PARTY_PAYMENT;
                                            hasAccess = true;
                                            ObjectNode billinNode2 = this.objectMapper.createObjectNode();
                                            billinNode2.put("command", "saveOnlyPayment");
                                            billinNode2.put("clientId", "1");
                                            billinNode2.put("carNumber", carState.getCarNumber());
                                            billinNode2.put("rateResult", rateResult);
                                            billinNode2.put("entryDate", entryDate);
                                            billinNode2.put("exitDate", entryDate);
                                            billinNode2.put("carStateId", carState.getId());
                                            JsonNode billingPl = billingPluginRegister.execute(billinNode2);

                                        } else {
                                            if (balance.compareTo(rateResult) >= 0) {
                                                if(BigDecimal.ZERO.compareTo(balance) > 0){
                                                    properties.put("type", EventLog.StatusType.Debt);
                                                    String descriptionRu = "В проезде отказано: Авто " + eventDto.car_number + " имеет задолженность " + balance;
                                                    String descriptionEn = "Not allowed to enter: Car " + eventDto.car_number + " is in debt " + balance;
                                                    String descriptionDe = "Betreten nicht erlaubt: Auto " + eventDto.car_number + " ist verschuldet " + balance;
                                                    eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Debt, camera.getId(), eventDto.getCarNumberWithRegion(), descriptionRu, descriptionEn, descriptionDe);
                                                    eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, descriptionRu, descriptionEn, descriptionDe, EventLog.EventType.DEBT);
                                                    hasAccess = false;
                                                } else {
                                                    carOutBy = StaticValues.CarOutBy.PAYMENT_PROVIDER;
                                                    hasAccess = true;
                                                }
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
                                                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Deny, camera.getId(), eventDto.getCarNumberWithRegion(),
                                                        "В проезде отказано: Не достаточно средств для списания оплаты за паркинг. Сумма к оплате: " + rateResult + ". Баланс: " + balance + ". Выезд с " + carState.getParking().getName(),
                                                        "Not allowed to exit: Not enough balance to pay for parking. Total sum to pay: " + rateResult + ". Balance: " + balance + ". Departure from " + carState.getParking().getName(),
                                                        "Ausfahrt nicht erlaubt: Nicht genug Guthaben, um das Parken zu bezahlen. Gesamtsumme zu zahlen: " + rateResult + ". Saldo: " + balance + ". Abfahrt von " + carState.getParking().getName());
                                                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,
                                                        "В проезде отказано: Не достаточно средств для списания оплаты за паркинг. Сумма к оплате: " + rateResult + ". Баланс: " + balance + ". Выезд с " + carState.getParking().getName(),
                                                        "Not allowed to exit: Not enough balance to pay for parking. Total sum to pay: " + rateResult + ". Balance: " + balance + ". Departure from " + carState.getParking().getName(),
                                                        "Ausfahrt nicht erlaubt: Nicht genug Guthaben, um das Parken zu bezahlen. Gesamtsumme zu zahlen: " + rateResult + ". Saldo: " + balance + ". Abfahrt von " + carState.getParking().getName(),
                                                        EventLog.EventType.NOT_ENOUGH_BALANCE);
                                            }
                                        }
                                    }
                                }
                            } else {
                                properties.put("type", EventLog.StatusType.Error);
                                String descriptionRu = "Плагин работы с балансами не найден или не запущен. Авто с гос. номером " + eventDto.car_number;
                                String descriptionEn = "Balance plugin not found or not launched. Car with license number " + eventDto.car_number;
                                String descriptionDe = "Nicht genug Guthaben, um für das Parken zu bezahlen. Auto mit Nummer " + eventDto.car_number;
                                eventLogService.createEventLog("Billing", null, properties, descriptionRu, descriptionEn, descriptionDe);
                                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Deny, camera.getId(), eventDto.getCarNumberWithRegion(), descriptionRu, descriptionEn, descriptionDe);
                                hasAccess = false;
                            }
                        }
                    }
                } else {
                    hasAccess = false;
                }
            }
        }
        String currentPlateNumber = eventDto.car_number;
        if (hasAccess) {
            if (barrierOutProcessingHashtable.containsKey(camera.getGate().getBarrier().getId()) && currentPlateNumber.equals(barrierOutProcessingHashtable.get(camera.getGate().getBarrier().getId()))) {
                return;
            }
            barrierOutProcessingHashtable.put(camera.getGate().getBarrier().getId(), currentPlateNumber);
            boolean openResult = false;

            try {
                ObjectNode socketMessage = this.objectMapper.createObjectNode();
                socketMessage.put("qr", Strings.EMPTY);
                socketMessage.put("carnumber", eventDto.car_number);
                socketMessage.put("debt", 0);
                socketMessage.put("sum", rateResult);
                socketMessage.put("action", "allow");
                socketMessage.put("gate", camera.getGate().getId());
                eventLogService.sendSocketMessage("qrpanel", socketMessage.toString());
            } catch (Exception ex) {

            }

            try {
                if (camera.getGate().getBarrier().isStatusCheck()) {
                    AbstractOpenStrategy strategy = CarOutEventStrategy.builder()
                            .camera(camera)
                            .format(format)
                            .balance(balance)
                            .carState(carState)
                            .eventDto(eventDto)
                            .carOutBy(carOutBy)
                            .whitelist(whitelists)
                            .abonements(abonements)
                            .rateResult(rateResult)
                            .properties(properties)
                            .zeroTouchValue(zerotouchValue)
                            .leftFromThisSecondsBefore(leftFromThisSecondsBefore)
                            .build();
                    strategy.gateId = gate.gateId;
                    strategy.isWaitPhel = true;
                    SensorStatusCheckJob.add(strategy);
                }else{
                    openResult = barrierService.openBarrier(camera.getGate().getBarrier(), properties);
                    if (openResult) {
                        gate.gateStatus = GateStatusDto.GateStatus.Open;
                        gate.sensor1 = GateStatusDto.SensorStatus.Triggerred;
                        gate.sensor2 = GateStatusDto.SensorStatus.WAIT;
                        gate.directionStatus = GateStatusDto.DirectionStatus.FORWARD;
                        gate.lastTriggeredTime = System.currentTimeMillis();

                        Boolean photoElementPassed;
                        Boolean loopPassed;

                        if(camera.getGate().getBarrier().isConfirmCarPass()){
                            photoElementPassed = false;
                            loopPassed = false;
                            int confirmPassTimeout = camera.getGate().getBarrier().getConfirmCarPassTimeout();
                            long currentMillis = System.currentTimeMillis();

                            while (currentPlateNumber.equals(barrierOutProcessingHashtable.get(camera.getGate().getBarrier().getId())) && System.currentTimeMillis() - currentMillis <= confirmPassTimeout*1000 && (!photoElementPassed || !loopPassed)){
                                if(!photoElementPassed){
                                    photoElementPassed = barrierService.getSensorStatus(StatusCheckJob.findGateStatusDtoById(camera.getGate().getId()).photoElement) == 1;
                                }
                                if(!loopPassed){
                                    loopPassed = barrierService.getSensorStatus(StatusCheckJob.findGateStatusDtoById(camera.getGate().getId()).loop) == 1;
                                }
                            }
                        } else {
                            photoElementPassed = true;
                            loopPassed = true;
                        }

                        if(photoElementPassed && loopPassed){
                            if (!gate.isSimpleWhitelist && !leftFromThisSecondsBefore && carState != null) {
                                saveCarOutState(eventDto, camera, carState, properties, balance, rateResult, zerotouchValue, format, carOutBy, abonements, whitelists);
                            } else if (leftFromThisSecondsBefore) {
                                properties.put("type", EventLog.StatusType.Allow);
                                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.getCarNumberWithRegion(),
                                        "Выпускаем авто: Авто с гос. номером " + eventDto.car_number,
                                        "Releasing: Car with license plate " + eventDto.car_number,
                                        "Freigeben: Auto mit Kennzeichen" + eventDto.car_number);
                                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,
                                        "Выпускаем авто: Авто с гос. номером " + eventDto.car_number,
                                        "Releasing: Car with license plate " + eventDto.car_number,
                                        "Freigeben: Auto mit Kennzeichen" + eventDto.car_number,  EventLog.EventType.PASS);
                            }
                        } else {
                            properties.put("type", EventLog.StatusType.Error);
                            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Error, camera.getId(), eventDto.getCarNumberWithRegion(),
                                    "Отмена выезда авто: Авто с гос. номером " + eventDto.car_number,
                                    "Cancel exit: Car with license plate " + eventDto.car_number,
                                    "Ausfahrt abbrechen: Auto mit speziellen Kennzeichen " + eventDto.car_number);
                            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,
                                    "Отмена выезда авто: Авто с гос. номером " + eventDto.car_number,
                                    "Cancel exit: Car with license plate " + eventDto.car_number,
                                    "Ausfahrt abbrechen: Auto mit speziellen Kennzeichen " + eventDto.car_number,
                                    EventLog.EventType.PASS);
                        }
                    } else {
                        String descriptionRu = "Ошибка открытия шлагбаума: На контроллер шлагбаума не удалось присвоит значение на открытие для авто " + eventDto.car_number + " на " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезд" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "выезд" : "въезд/выезд")) + " для " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName();
                        String descriptionEn = "Error while barrier open: Cannot assign value to open barrier for car " + eventDto.car_number + " to " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "pass" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName();
                        String descriptionDe = "Fehler beim Öffnen der Schranke: Wert für offene Schranke für Auto kann nicht zugewiesen werden " + eventDto.car_number + " an " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "passieren" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "ausfahrt" : "einfahrt/ausfahrt")) + " " + camera.getGate().getDescription() + " parken " + camera.getGate().getParking().getName();
                        eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Error, camera.getId(), eventDto.getCarNumberWithRegion(), descriptionRu, descriptionEn, descriptionDe);
                        eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, descriptionRu, descriptionEn, descriptionDe, EventLog.EventType.ERROR);
                    }
                }
            } catch (Exception e) {
                String descriptionRu = "Ошибка открытия шлагбаума: На контроллер шлагбаума не удалось присвоит значение на открытие для авто " + eventDto.car_number + " на " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезд" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "выезд" : "въезд/выезд")) + " для " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName();
                String descriptionEn = "Error while barrier open: Cannot assign value to open barrier for car " + eventDto.car_number + " to " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "pass" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName();
                String descriptionDe = "Fehler beim Öffnen der Schranke: Wert für offene Schranke für Auto kann nicht zugewiesen werden" + eventDto.car_number + " an " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "passieren" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "ausfahrt" : "einfahrt/ausfahrt")) + " " + camera.getGate().getDescription() + " parken " + camera.getGate().getParking().getName();
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Error, camera.getId(), eventDto.getCarNumberWithRegion(), descriptionRu, descriptionEn, descriptionDe);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, descriptionRu, descriptionEn, descriptionDe, EventLog.EventType.ERROR);
                log.info("Error opening barrier: " + e.getMessage());
            }

            if(currentPlateNumber.equals(barrierOutProcessingHashtable.get(camera.getGate().getBarrier().getId()))){
                barrierOutProcessingHashtable.remove(camera.getGate().getBarrier().getId());
            }
//          send notification to third party
            log.info("notification: " + notification);
            if (notification) {
                sendNotification(carState, eventDto.event_date_time, rateResult);
            }
        } else {
            try {
                qrPanelService.display(camera.getGate(), eventDto.car_number);
                String qrUrl = qrPanelService.generateUrl(camera.getGate(), eventDto.car_number);

                ObjectNode socketMessage = this.objectMapper.createObjectNode();
                socketMessage.put("qr", qrUrl);
                socketMessage.put("carnumber", eventDto.car_number);
                socketMessage.put("debt", balance);
                socketMessage.put("action", "display");
                socketMessage.put("gate", camera.getGate().getId());
                eventLogService.sendSocketMessage("qrpanel", socketMessage.toString());
            } catch (Exception ex) {
                log.log(Level.WARNING, "Error while display qrpanel for gate " + gate.gateName);
            }

            if (barrierOutProcessingHashtable.containsKey(camera.getGate().getBarrier().getId()) && currentPlateNumber.equals(barrierOutProcessingHashtable.get(camera.getGate().getBarrier().getId()))) {
                return;
            }
            barrierOutProcessingHashtable.put(camera.getGate().getBarrier().getId(), currentPlateNumber);

            boolean barrierStatusResult;
            try {
                barrierStatusResult = barrierService.getBarrierStatus(camera.getGate().getBarrier(), properties);
                log.info("barrierStatusResult: " + barrierStatusResult);
                if(barrierStatusResult){
                    paymentService.createDebtAndOUTState(eventDto.car_number, camera, properties);
                }
            } catch (Exception e) {
                String descriptionRu = "Ошибка считования значении шлагбаума: С контроллера шлагбаума не удалось считать значение для проверки состояние для авто " + eventDto.car_number + " на " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезд" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "выезд" : "въезд/выезд")) + " для " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName();
                String descriptionEn = "Error while barrier state read: Could not read state from barrier for car " + eventDto.car_number + " to " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "pass" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName();
                String descriptionDe = "Fehler beim Lesen des Schrankenstatus: Der Status der Schranke für das Auto konnte nicht gelesen werden " + eventDto.car_number + " an " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "passieren" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "ausfahrt" : "einfahrt/ausfahrt")) + " " + camera.getGate().getDescription() + " parken " + camera.getGate().getParking().getName();
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Error, camera.getId(), eventDto.getCarNumberWithRegion(), descriptionRu, descriptionEn, descriptionDe);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, descriptionRu, descriptionEn, descriptionDe, EventLog.EventType.ERROR);
                log.info("Error reading barrier state: " + e.getMessage());
            }
            if(currentPlateNumber.equals(barrierOutProcessingHashtable.get(camera.getGate().getBarrier().getId()))){
                barrierOutProcessingHashtable.remove(camera.getGate().getBarrier().getId());
            }
        }

        if(tabloConnected){
            tabloService.updateOnOut(camera.getGate());
        }
    }

    private void sendNotification(CarState carState, Date dateOut, BigDecimal rate) {
        ZoneId id = ZoneId.systemDefault();
        String dt_start = String.valueOf(ZonedDateTime.ofInstant(carState.getInTimestamp().toInstant(), id).withFixedOffsetZone());
        String dt_finish = String.valueOf(ZonedDateTime.ofInstant(dateOut.toInstant(), id).withFixedOffsetZone());

        RestTemplate restTemplate = new RestTemplate();
        String url = notificationUrl;
        Map<String, Object> params = new HashMap<>();
        params.put("plate_number", carState.getCarNumber());
        params.put("parking_uid", parking_uid.toString());
        params.put("sum", rate.intValue());
        params.put("dt_start", dt_start);
        params.put("dt_finish", dt_finish);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + magnumNotificationToken);
        HttpEntity request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);
            log.info("Magnum notification response status: " + responseEntity.getStatusCode() + ", plate_number: " + carState.getCarNumber());
        } catch (Exception e) {
//            e.printStackTrace();
            log.info("Magnum notification response error.");
        }

    }

    @Override
    public void saveCarOutState(CarEventDto eventDto, Camera camera, CarState carState, Map<String, Object> properties, BigDecimal balance, BigDecimal rateResult, BigDecimal zerotouchValue, SimpleDateFormat format, StaticValues.CarOutBy carOutBy, JsonNode abonements, JsonNode whitelists) throws Exception {
        if (StaticValues.CarOutBy.FREE.equals(carOutBy)) {
            properties.put("type", EventLog.StatusType.Allow);
            carState.setCarOutType(CarState.CarOutType.FREE_PASS);
            carStateService.createOUTState(eventDto.car_number, eventDto.event_date_time, camera, carState, properties.containsKey(StaticValues.carSmallImagePropertyName) ? properties.get(StaticValues.carSmallImagePropertyName).toString() : null);
            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.getCarNumberWithRegion(),
                    "Выпускаем авто: Авто с гос. номером " + eventDto.car_number,
                    "Releasing: Car with license plate " + eventDto.car_number,
                    "Freigeben: Auto mit Kennzeichen" + eventDto.car_number);
            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,
                    "Выпускаем авто: Авто с гос. номером " + eventDto.car_number,
                    "Releasing: Car with license plate " + eventDto.car_number,
                    "Freigeben: Auto mit Kennzeichen" + eventDto.car_number, EventLog.EventType.FREE_PASS);
        } else if (StaticValues.CarOutBy.WHITELIST.equals(carOutBy)) {
            properties.put("type", EventLog.StatusType.Allow);
            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.getCarNumberWithRegion(),
                    "Пропускаем авто: Присутствовал в белом списке. Авто с гос. номером " + eventDto.car_number,
                    "Car is allowed: Exist in whitelist. Car with license plate " + eventDto.car_number,
                    "Auto ist erlaubt: Vorhanden in der Zulassungsliste . Auto mit speziellen Kennzeichen " + eventDto.car_number);
            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,
                    "Пропускаем авто: Присутствовал в белом списке. Авто с гос. номером " + eventDto.car_number,
                    "Car is allowed: Exist in whitelist. Car with license plate " + eventDto.car_number,
                    "Auto ist erlaubt: Vorhanden in der Zulassungsliste . Auto mit speziellen Kennzeichen " + eventDto.car_number,
                    EventLog.EventType.WHITELIST_OUT);
            carState.setWhitelistJson(whitelists != null ? whitelists.toString() : null);
            carState.setCarOutType(CarState.CarOutType.WHITELIST_OUT);

            if (rateResult != null) {
                PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
                if (billingPluginRegister != null && BigDecimal.ZERO.compareTo(rateResult) != 0) {
                    decreaseBalance(carState.getCarNumber(), carState.getParking().getName(), carState.getId(), rateResult, properties);
                }
                carState.setRateAmount(zerotouchValue != null ? rateResult.add(zerotouchValue) : rateResult);

                if (billingPluginRegister != null) {
                    ObjectNode addTimestampNode = this.objectMapper.createObjectNode();
                    addTimestampNode.put("command", "addOutTimestampToPayments");
                    addTimestampNode.put("outTimestamp", format.format(eventDto.event_date_time));
                    addTimestampNode.put("carStateId", carState.getId());
                    billingPluginRegister.execute(addTimestampNode);
                }
            }
            carStateService.createOUTState(eventDto.car_number, eventDto.event_date_time, camera, carState, properties.containsKey(StaticValues.carSmallImagePropertyName) ? properties.get(StaticValues.carSmallImagePropertyName).toString() : null);
        } else if (StaticValues.CarOutBy.PREPAID.equals(carOutBy)) {
            carStateService.createOUTState(eventDto.car_number, eventDto.event_date_time, camera, carState, properties.containsKey(StaticValues.carSmallImagePropertyName) ? properties.get(StaticValues.carSmallImagePropertyName).toString() : null);
            properties.put("type", EventLog.StatusType.Allow);
            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.getCarNumberWithRegion(),
                    "Выпускаем авто по предоплате: Авто с гос. номером" + eventDto.car_number,
                    "For prepaid exit is allowed on prepaid basis. Car with license plate " + eventDto.car_number,
                    "Für Prepaid-Ausfahrt ist auf Prepaid-Basis erlaubt. Auto mit speziellen Kennzeichen " + eventDto.car_number);
            eventLogService.createEventLog(CarState.class.getSimpleName(), null, properties,
                    "Выпускаем авто по предоплате: Авто с гос. номером" + eventDto.car_number,
                    "For prepaid exit is allowed on prepaid basis. Car with license plate " + eventDto.car_number,
                    "Für Prepaid-Ausfahrt ist auf Prepaid-Basis erlaubt. Auto mit speziellen Kennzeichen " + eventDto.car_number,
                    EventLog.EventType.PREPAID);
        } else if (StaticValues.CarOutBy.THIRD_PARTY_PAYMENT.equals(carOutBy)) {
            carState.setRateAmount(rateResult);
            carStateService.createOUTState(eventDto.car_number, eventDto.event_date_time, camera, carState, properties.containsKey(StaticValues.carSmallImagePropertyName) ? properties.get(StaticValues.carSmallImagePropertyName).toString() : null);
            properties.put("type", EventLog.StatusType.Allow);
            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.getCarNumberWithRegion(),
                    "Выпускаем авто, оплата будет на стороннем приложении: Авто с гос. номером " + eventDto.car_number,
                    "For prepaid exit is allowed, payment will be on third party application. Car with license plate " + eventDto.car_number,
                    "Für Prepaid-Ausfahrt ist erlaubt, die Zahlung wird auf Drittanbieter-Anwendung sein. Auto mit Nummernschild. Auto mit speziellen Kennzeichen " + eventDto.car_number);
            eventLogService.createEventLog(CarState.class.getSimpleName(), null, properties,
                    "Выпускаем авто, оплата будет на стороннем приложении: Авто с гос. номером " + eventDto.car_number,
                    "For prepaid exit is allowed, payment will be on third party application. Car with license plate " + eventDto.car_number,
                    "Für Prepaid-Ausfahrt ist erlaubt, die Zahlung wird auf Drittanbieter-Anwendung sein. Auto mit Nummernschild. Auto mit speziellen Kennzeichen " + eventDto.car_number,
                    EventLog.EventType.PREPAID);
        } else if (StaticValues.CarOutBy.ABONOMENT.equals(carOutBy)) {
            properties.put("type", EventLog.StatusType.Allow);
            String message_ru = "Пропускаем авто: Найдено действущий абономент на номер авто " + eventDto.car_number + ". Проезд разрешен.";
            String message_en = "Allowed: Found valid paid permit for car late number " + eventDto.car_number + ". Allowed.";
            String message_de = "Erlaubt: Gültige bezahlte Genehmigung für das Kennzeichen gefunden " + eventDto.car_number + ". Erlaubt.";
            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.getCarNumberWithRegion(), message_ru, message_en, message_de);
            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, message_ru, message_en, message_de, EventLog.EventType.ABONEMENT_PASS);
            carStateService.setAbonomentDetails(carState.getId(), abonements);
            carState.setCarOutType(CarState.CarOutType.ABONEMENT_PASS);

            PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
            if (billingPluginRegister != null && BigDecimal.ZERO.compareTo(rateResult) != 0) {
                decreaseBalance(carState.getCarNumber(), carState.getParking().getName(), carState.getId(), rateResult, properties);
            }
            carState.setRateAmount(zerotouchValue != null ? rateResult.add(zerotouchValue) : rateResult);
            carStateService.createOUTState(eventDto.car_number, eventDto.event_date_time, camera, carState, properties.containsKey(StaticValues.carSmallImagePropertyName) ? properties.get(StaticValues.carSmallImagePropertyName).toString() : null);

            if (billingPluginRegister != null) {
                ObjectNode addTimestampNode = this.objectMapper.createObjectNode();
                addTimestampNode.put("command", "addOutTimestampToPayments");
                addTimestampNode.put("outTimestamp", format.format(eventDto.event_date_time));
                addTimestampNode.put("carStateId", carState.getId());
                billingPluginRegister.execute(addTimestampNode);
            }
        } else if (StaticValues.CarOutBy.ABONOMENT_WO_ENTRY.equals(carOutBy)) {
            properties.put("type", EventLog.StatusType.Success);
            String descriptionRu = "Пропускаем авто: Найден действующий абонемент на номер авто " + eventDto.car_number;
            String descriptionEn = "Allowed: Found valid paid permit for car late number " + eventDto.car_number;
            String descriptionDe = "Erlaubt: Gültige bezahlte Genehmigung für das Kennzeichen gefunden " + eventDto.car_number;
            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Skip, camera.getId(), eventDto.getCarNumberWithRegion(), descriptionRu, descriptionEn, descriptionDe);
            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, descriptionRu, descriptionEn, descriptionDe, EventLog.EventType.ABONEMENT_PASS);
        } else if (StaticValues.CarOutBy.REGISTER.equals(carOutBy)) {
            if (carState != null) {
                carState.setCarOutType(CarState.CarOutType.REGISTER_PASS);
                carStateService.createOUTState(eventDto.car_number, eventDto.event_date_time, camera, carState, properties.containsKey(StaticValues.carSmallImagePropertyName) ? properties.get(StaticValues.carSmallImagePropertyName).toString() : null);
                properties.put("type", EventLog.StatusType.Allow);
                String message_ru = "Пропускаем авто: Авто с номером " + eventDto.car_number + " выехал с паркинга";
                String message_en = "Allowed: Car with plate number " + eventDto.car_number + " left the parking";
                String message_de = "Erlaubt: Fahrzeug mit speziellen Kennzeichen " + eventDto.car_number + " Parkplatz verlassen";
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.getCarNumberWithRegion(), message_ru, message_en, message_de);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, message_ru, message_en, message_de, EventLog.EventType.REGISTER_PASS);
            } else {
                properties.put("type", EventLog.StatusType.Allow);
                String message_ru = "Не найдена запись автомобиля о въезде с гос. номером " + eventDto.car_number + " выехал с паркинга";
                String message_en = "No record found about entering: Car with plate number " + eventDto.car_number + " left the parking";
                String message_de = "No record found about entering: Car with plate number " + eventDto.car_number + " Parkplatz verlassen";
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.getCarNumberWithRegion(), message_ru, message_en, message_de);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, message_ru, message_en, message_de, EventLog.EventType.PASS);
            }
        } else if (StaticValues.CarOutBy.BOOKING.equals(carOutBy)) {
            properties.put("type", EventLog.StatusType.Allow);
            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.getCarNumberWithRegion(),
                    "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " имеется валидный пропуск.",
                    "Allow entrance: Car with plate number " + eventDto.car_number + " has valid booking.",
                    "Einfahrt gestatt: Auto mit Kennzeichen " + eventDto.car_number + " hat eine gültige Reservierung.");
            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties,
                    "Пропускаем авто: Авто с гос. номером " + eventDto.car_number + " имеется валидный пропуск.",
                    "Allow entrance: Car with plate number " + eventDto.car_number + " has valid booking.",
                    "Einfahrt gestatt: Auto mit Kennzeichen " + eventDto.car_number + " hat eine gültige Reservierung.",
                    EventLog.EventType.BOOKING_PASS);
            carState.setCarOutType(CarState.CarOutType.BOOKING_PASS);
            carStateService.createOUTState(eventDto.car_number, eventDto.event_date_time, camera, carState, properties.containsKey(StaticValues.carSmallImagePropertyName) ? properties.get(StaticValues.carSmallImagePropertyName).toString() : null);
        } else {
            EventLog.EventType eventType;
            if (StaticValues.CarOutBy.ZERO_TOUCH.equals(carOutBy)) {
                properties.put("type", EventLog.StatusType.Allow);
                carStateService.createOUTState(eventDto.car_number, eventDto.event_date_time, camera, carState, properties.containsKey(StaticValues.carSmallImagePropertyName) ? properties.get(StaticValues.carSmallImagePropertyName).toString() : null);
                String message_ru = "Пропускаем авто: Безусловная оплата прошла удачно. Сумма к оплате: " + rateResult + ". Проезд разрешен.";
                String message_en = "Allowed: Zero touch payment received. Total sum: " + rateResult + ". Allowed.";
                String message_de = "Erlaubt: Die bedingungslose Zahlung war erfolgreich. Gesamtbetrag: " + rateResult + ". Erlaubt.";
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.getCarNumberWithRegion(), message_ru, message_en, message_de);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, message_ru, message_en, message_de, EventLog.EventType.ZERO_TOUCH);
                rateResult = rateResult.subtract(zerotouchValue);
            } else {
                BigDecimal subtractResult = balance.subtract(rateResult);
                properties.put("type", EventLog.StatusType.Allow);
                String descriptionRu = "Пропускаем авто: Оплата за паркинг присутствует. Сумма к оплате: " + rateResult + ". Остаток баланса: " + subtractResult + ". Проезд разрешен. " + "Выезд с " + carState.getParking().getName();
                String descriptionEn = "Allowed: Paid for parking. Total sum: " + rateResult + ". Balance left: " + subtractResult + ". Allowed." + ". Departure from " + carState.getParking().getName() ;
                String descriptionDe = "Erlaubt: Bezahltes Parken. Gesamtbetrag: " + rateResult + ". Restbetrag übrig: " + subtractResult + ". Erlaubt." + ". Abfahrt von " + carState.getParking().getName() ;
                eventType = EventLog.EventType.PAID_PASS;
                carState.setCarOutType(CarState.CarOutType.PAID_PASS);
                if (BigDecimal.ZERO.compareTo(rateResult) == 0) {
                    int freeMinutesValue = 15;
                    PluginRegister ratePluginRegister = pluginService.getPluginRegister(StaticValues.ratePlugin);
                    if (ratePluginRegister != null) {
                        ObjectNode ratePluginNode = this.objectMapper.createObjectNode();
                        ratePluginNode.put("parkingId", camera.getGate().getParking().getId());
                        ratePluginNode.put("command", "getBeforeFreeMinutesValue");
                        JsonNode ratePluginResult = ratePluginRegister.execute(ratePluginNode);
                        if (ratePluginResult.has("beforeFreeMinutesValue")) {
                            freeMinutesValue = ratePluginResult.get("beforeFreeMinutesValue").intValue();
                        }
                    }
                    if ((eventDto.event_date_time.getTime() - carState.getInTimestamp().getTime()) <= freeMinutesValue * 60 * 1000) {
                        descriptionRu = "Пропускаем авто: Первые " + freeMinutesValue + " минут бесплатно. Проезд разрешен.";
                        descriptionEn = "Allowed: First " + freeMinutesValue + " minutes free. Allowed";
                        descriptionDe = "Erlaubt: Erste " + freeMinutesValue +"Minuten frei. Erlaubt";
                        eventType = EventLog.EventType.FIFTEEN_FREE;
                        carState.setCarOutType(CarState.CarOutType.FIFTEEN_FREE);
                    } else {
                        descriptionRu = "Пропускаем авто: Оплата не требуется. Проезд разрешен.";
                        descriptionEn = "Allowed: No payment required. Allowed";
                        descriptionDe = "Erlaubt: Keine Zahlung erforderlich. Erlaubt";

                        eventType = EventLog.EventType.FIFTEEN_FREE;
                        carState.setCarOutType(CarState.CarOutType.FIFTEEN_FREE);
                    }
                }
                eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), eventDto.getCarNumberWithRegion(), descriptionRu, descriptionEn, descriptionDe);
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, descriptionRu, descriptionEn,descriptionDe, eventType);
            }

            PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
            if (billingPluginRegister != null && BigDecimal.ZERO.compareTo(rateResult) != 0) {
                decreaseBalance(carState.getCarNumber(), carState.getParking().getName(), carState.getId(), rateResult, properties);
            }
            carState.setRateAmount(zerotouchValue != null ? rateResult.add(zerotouchValue) : rateResult);
            carStateService.createOUTState(eventDto.car_number, eventDto.event_date_time, camera, carState, properties.containsKey(StaticValues.carSmallImagePropertyName) ? properties.get(StaticValues.carSmallImagePropertyName).toString() : null);

            if (billingPluginRegister != null) {
                ObjectNode addTimestampNode = this.objectMapper.createObjectNode();
                addTimestampNode.put("command", "addOutTimestampToPayments");
                addTimestampNode.put("outTimestamp", format.format(eventDto.event_date_time));
                addTimestampNode.put("carStateId", carState.getId());
                billingPluginRegister.execute(addTimestampNode);
            }
        }
    }

    private void createNewCarEvent(Long id, GateStatusDto gateStatusDto, CarEventDto eventDto, Map<String, Object> properties) {
        properties.put("type", EventLog.StatusType.Success);
        eventLogService.sendSocketMessage(ArmEventType.Photo, EventLog.StatusType.Success, id, eventDto.getCarNumberWithRegion(), eventDto.car_picture, null, null);
        eventLogService.sendSocketMessage(ArmEventType.Lp, EventLog.StatusType.Success, id, eventDto.car_number, eventDto.lp_picture, null, null);
        eventLogService.createEventLog(Camera.class.getSimpleName(), id, properties,
                "Зафиксирован новый номер авто " + eventDto.car_number,
                "New license plate number identified " + eventDto.car_number,
                "Neues Nummernschild identifiziert " + eventDto.car_number,
                EventLog.EventType.NEW_CAR_DETECTED);
        carsService.createCar(eventDto.car_number, eventDto.lp_region, eventDto.vecihleType, Gate.GateType.OUT.equals(gateStatusDto.gateType) ? null : eventDto.car_model); // При выезде не сохранять тип авто
    }

    private BigDecimal calculateRate(Date inDate, Date outDate, Camera camera, CarState carState, CarEventDto eventDto, SimpleDateFormat format, Map<String, Object> properties, Boolean isCheck) throws Exception {
        PluginRegister ratePluginRegister = pluginService.getPluginRegister(StaticValues.ratePlugin);
        if (ratePluginRegister != null) {
            ObjectNode ratePluginNode = this.objectMapper.createObjectNode();
            ratePluginNode.put("parkingId", camera.getGate().getParking().getId());
            ratePluginNode.put("inDate", format.format(inDate));
            ratePluginNode.put("outDate", format.format(outDate));
            ratePluginNode.put("plateNumber", carState.getCarNumber());
            ratePluginNode.put("cashlessPayment", carState.getCashlessPayment() != null ? carState.getCashlessPayment() : true);
            ratePluginNode.put("isCheck", isCheck); // Включать бесплатные минуты в начале или нет
            ratePluginNode.put("paymentsJson", carState.getPaymentJson());
            Cars cars = carsService.findByPlatenumber(carState.getCarNumber());
            CarModel carModel = carModelService.getByModel(cars.getModel());
            if (carModel != null)
                ratePluginNode.put("carType", carModel.getType());
            JsonNode ratePluginResult = ratePluginRegister.execute(ratePluginNode);
            return ratePluginResult.get("rateResult").decimalValue().setScale(2);
        } else {
            properties.put("type", EventLog.StatusType.Error);
            String descriptionRu = "Плагин вычисления стоимости парковки не найден или не запущен. Авто с гос. номером " + eventDto.car_number;
            String descriptionEn = "Plugin for calculating total sum not found or not launched. Car with number " + eventDto.car_number;
            String descriptionDe = "Plugin zur Berechnung der Gesamtsumme nicht gefunden oder nicht gestartet. Auto mit Nummer " + eventDto.car_number;
            eventLogService.createEventLog("Rate", null, properties, descriptionRu, descriptionEn, descriptionDe);
            eventLogService.sendSocketMessage(ArmEventType.CarEvent, EventLog.StatusType.Deny, camera.getId(), eventDto.getCarNumberWithRegion(), descriptionRu, descriptionEn, descriptionDe);
            return null;
        }
    }

    private BigDecimal calculateAbonomentExtraPayment(Camera camera, CarState carState, CarEventDto eventDto, JsonNode abonementJson, SimpleDateFormat format, Map<String, Object> properties) throws Exception {

        Date inDate = carState.getInTimestamp();
        Date outDate = eventDto.event_date_time;

        List<Period> periods = abonomentService.calculatePaymentPeriods(abonementJson, inDate, outDate);

        log.info("abonoments periods size = " + periods.size());
        if (periods.size() == 0) {
            return BigDecimal.ZERO;
        } else {
            Calendar calcEndCalendar = Calendar.getInstance();
            Date calcBegin = periods.get(0).getStart();
            calcEndCalendar.setTime(periods.get(0).getStart());

            for (Period p : periods) {
                log.info("adding period: begin: " + p.getStart() + " end: " + p.getEnd());
                calcEndCalendar.add(Calendar.MILLISECOND, (int) (p.getEnd().getTime() - p.getStart().getTime()));
            }
            log.info("calculate rate for: " + calcBegin + " end: " + calcEndCalendar.getTime());
            BigDecimal rate = calculateRate(calcBegin, calcEndCalendar.getTime(), camera, carState, eventDto, format, properties, true);

            return rate;
        }
    }

    private BigDecimal calculateWhitelistExtraPayment(Camera camera, CarState carState, CarEventDto eventDto, JsonNode whitelistJson, SimpleDateFormat format, Map<String, Object> properties) throws Exception {

        Date inDate = carState.getInTimestamp();
        Date outDate = eventDto.event_date_time;

        List<Period> periods = whitelistRootService.calculatePaymentPeriods(whitelistJson, inDate, outDate);

        log.info("whitelist periods size = " + periods.size());
        if (periods.size() == 0) {
            return BigDecimal.ZERO;
        } else {
            Calendar calcEndCalendar = Calendar.getInstance();
            Date calcBegin = periods.get(0).getStart();
            calcEndCalendar.setTime(periods.get(0).getStart());

            for (Period p : periods) {
                log.info("adding period: begin: " + p.getStart() + " end: " + p.getEnd());
                calcEndCalendar.add(Calendar.MILLISECOND, (int) (p.getEnd().getTime() - p.getStart().getTime()));
            }
            log.info("calculate rate for: " + calcBegin + " end: " + calcEndCalendar.getTime());
            BigDecimal rate = calculateRate(calcBegin, calcEndCalendar.getTime(), camera, carState, eventDto, format, properties, false);

            return rate;
        }
    }
}