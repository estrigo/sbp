package kz.spt.app.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import kz.spt.app.component.HttpRequestFactoryDigestAuth;
import kz.spt.app.job.SensorStatusCheckJob;
import kz.spt.app.job.StatusCheckJob;
import kz.spt.app.model.dto.BarrierStatusDto;
import kz.spt.app.model.dto.CameraStatusDto;
import kz.spt.app.model.strategy.barrier.close.AbstractCloseStrategy;
import kz.spt.app.model.strategy.barrier.close.ManualCloseStrategy;
import kz.spt.app.model.strategy.barrier.open.AbstractOpenStrategy;
import kz.spt.app.model.strategy.barrier.open.ManualOpenStrategy1;
import kz.spt.app.model.strategy.barrier.open.ManualOpenStrategy2;
import kz.spt.app.service.BarrierService;
import kz.spt.app.service.CameraService;
import kz.spt.lib.model.*;
import kz.spt.lib.model.dto.CarEventDto;
import kz.spt.lib.service.*;
import kz.spt.lib.utils.StaticValues;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import lombok.val;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Future;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
@Log
@Service
public class ArmServiceImpl implements ArmService {
    @Value("${hardcode.string.noPicture}")
    private String noPicture;
    public static Hashtable<String, Long> hashtable = new Hashtable<>();

    private ObjectMapper objectMapper = new ObjectMapper();
    private CameraService cameraService;
    private BarrierService barrierService;
    private EventLogService eventLogService;
    private String dateFormat = "yyyy-MM-dd'T'HH:mm";
    private CarEventService carEventService;
    private CarImageService carImageService;
    private PaymentService paymentService;

    public ArmServiceImpl(CameraService cameraService, BarrierService barrierService, EventLogService eventLogService,
                          CarEventService carEventService, CarImageService carImageService,
                          PaymentService paymentService) {
        this.cameraService = cameraService;
        this.barrierService = barrierService;
        this.eventLogService = eventLogService;
        this.carEventService = carEventService;
        this.carImageService = carImageService;
        this.paymentService = paymentService;
    }

    @Override
    public Boolean openGate(Long cameraId) throws IOException, ParseException, InterruptedException, ModbusProtocolException, ModbusNumberException, ModbusIOException {

        Camera camera = cameraService.getCameraById(cameraId);
        if (camera != null && camera.getGate() != null && camera.getGate().getBarrier() != null) {
            SimpleDateFormat format = new SimpleDateFormat(dateFormat);
            Map<String, Object> properties = new HashMap<>();
            properties.put("eventTime", format.format(new Date()));
            properties.put("cameraIp", camera.getIp());
            properties.put("cameraId", cameraId);
            properties.put("gateName", camera.getGate().getName());
            properties.put("gateDescription", camera.getGate().getDescription());
            properties.put("gateType", camera.getGate().getGateType().toString());
            properties.put("type", EventLog.StatusType.Allow);

            Boolean result = true;
            if (camera.getGate().getBarrier().isStatusCheck()) {
                BarrierStatusDto barrierStatusDto = BarrierStatusDto.fromBarrier(camera.getGate().getBarrier());
                AbstractOpenStrategy strategy = ManualOpenStrategy1.builder()
                        .camera(camera)
                        .properties(properties)
                        .build();
                strategy.gateId = barrierStatusDto.gateId;
                SensorStatusCheckJob.add(strategy);
            } else {
                result = barrierService.openBarrier(camera.getGate().getBarrier(), properties);
                if (result) {
                    String username = "";
                    if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CurrentUser) {
                        CurrentUser currentUser = (CurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                        if (currentUser != null) {
                            username = currentUser.getUsername();
                        }
                    }
                    eventLogService.sendSocketMessage(EventLogService.ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), "", "Ручное открытие шлагбаума: Пользователь " + username + " открыл шлагбаум для " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "выезда" : "въезда/выезда")) + " " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName(), "Manual opening gate: User " + username + " opened gate for " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "enter" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName());
                    eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, "Ручное открытие шлагбаума: Пользователь " + username + " открыл шлагбаум для " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "выезда" : "въезда/выезда")) + " " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName(), "Manual gate opening: User " + username + " opened gate for " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "enter" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName());
                }
            }

            return result;
        }

        return false;
    }

    @Override
    public Boolean openGate(Long cameraId, String snapshot, String reason) throws Exception {
        Camera camera = cameraService.getCameraById(cameraId);
        if (camera != null && camera.getGate() != null && camera.getGate().getBarrier() != null) {
            SimpleDateFormat format = new SimpleDateFormat(dateFormat);
            Map<String, Object> properties = new HashMap<>();
            properties.put("eventTime", format.format(new Date()));
            properties.put("cameraIp", camera.getIp());
            properties.put("cameraId", cameraId);
            properties.put("gateName", camera.getGate().getName());
            properties.put("gateDescription", camera.getGate().getDescription());
            properties.put("gateType", camera.getGate().getGateType().toString());
            properties.put("type", EventLog.StatusType.Allow);
            properties.put("event", EventLog.EventType.MANUAL_GATE_OPEN);

            Boolean result = true;
            if (camera.getGate().getBarrier().isStatusCheck()) {
                BarrierStatusDto barrierStatusDto = BarrierStatusDto.fromBarrier(camera.getGate().getBarrier());
                AbstractOpenStrategy strategy = ManualOpenStrategy2.builder()
                        .camera(camera)
                        .properties(properties)
                        .build();
                strategy.gateId = barrierStatusDto.gateId;
                SensorStatusCheckJob.add(strategy);
            } else {
                result = barrierService.openBarrier(camera.getGate().getBarrier(), properties);
                try {
                    CameraStatusDto cameraStatusDtoById = StatusCheckJob.findCameraStatusDtoById(cameraId);
                    byte[] bytes = manualSnapShot(cameraStatusDtoById);
                    String newSnapShot = "data:image/jpg;base64," + encodeBase64StringWithSize(bytes);
                    String carImageUrl = carImageService.saveImage(newSnapShot, new Date(), null);
                    properties.put(StaticValues.carImagePropertyName, carImageUrl);
                    properties.put(StaticValues.carSmallImagePropertyName, carImageUrl.replace(StaticValues.carImageExtension, "") + StaticValues.carImageSmallAddon + StaticValues.carImageExtension);
                } catch (Throwable e) {
                    log.warning("ERROR carImageUrl " + e.getMessage());
                    throw new RuntimeException(e);
                }
                if (result) {
                    String username = "";
                    if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CurrentUser) {
                        CurrentUser currentUser = (CurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                        if (currentUser != null) {
                            username = currentUser.getUsername();
                        }
                    }

                    if (Gate.GateType.OUT.equals(camera.getGate().getGateType()) || Gate.GateType.REVERSE.equals(camera.getGate().getGateType())) {
                        String debtPlatenumber = eventLogService.findLastNotEnoughFunds(camera.getGate().getId());

                        if (debtPlatenumber != null) {
                            properties.put("carNumber", debtPlatenumber);

                            if (hashtable.containsKey(debtPlatenumber)) {
                                Long timeDiffInMillis = System.currentTimeMillis() - hashtable.get(debtPlatenumber);
                                if (timeDiffInMillis > 2 * 1000) { // если больше 2 секунд то принимать команду
                                    hashtable.put(debtPlatenumber, System.currentTimeMillis());
                                } else {
                                    return false;
                                }
                            } else {
                                hashtable.put(debtPlatenumber, System.currentTimeMillis());
                            }
                        }

                        eventLogService.sendSocketMessage(EventLogService.ArmEventType.Photo, EventLog.StatusType.Success, camera.getId(), debtPlatenumber, snapshot, null);

                        String description = "Ручное открытие шлагбаума: Пользователь " + username + " открыл шлагбаум для " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "выезда" : "въезда/выезда")) + " " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName() + " Причина: " + reason;
                        String descriptionEn = "Manual opening gate: User " + username + " opened gate for " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "enter" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName() + " Reason: " + reason;
                        eventLogService.sendSocketMessage(EventLogService.ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), debtPlatenumber, description, descriptionEn);
                        eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, description, descriptionEn);

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

                        eventLogService.sendSocketMessage(EventLogService.ArmEventType.Photo, EventLog.StatusType.Success, camera.getId(), debtPlatenumber, snapshot, null);

                        String description = "Ручное открытие шлагбаума: Пользователь " + username + " открыл шлагбаум для " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "выезда" : "въезда/выезда")) + " " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName() + " Причина: " + reason;
                        String descriptionEn = "Manual opening gate: User " + username + " opened gate for " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "enter" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName() + " Reason: " + reason;
                        eventLogService.sendSocketMessage(EventLogService.ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), debtPlatenumber, description, descriptionEn);
                        eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, description, descriptionEn);

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
                            eventDto.cameraId = cameraId;

                            carEventService.saveCarEvent(eventDto);
                        }
                    }

                }
            }

            return result;
        }

        return null;
    }

    @Override
    public Boolean closeGate(Long cameraId) throws IOException, ParseException, InterruptedException, ModbusProtocolException, ModbusNumberException, ModbusIOException {

        Camera camera = cameraService.getCameraById(cameraId);
        if (camera != null && camera.getGate() != null && camera.getGate().getBarrier() != null) {
            SimpleDateFormat format = new SimpleDateFormat(dateFormat);
            Map<String, Object> properties = new HashMap<>();
            properties.put("eventTime", format.format(new Date()));
            properties.put("cameraIp", camera.getIp());
            properties.put("gateName", camera.getGate().getName());
            properties.put("gateDescription", camera.getGate().getDescription());
            properties.put("gateType", camera.getGate().getGateType().toString());
            properties.put("type", EventLog.StatusType.Allow);
            properties.put("event", EventLog.EventType.MANUAL_GATE_CLOSE);

            Boolean result = true;
            if (camera.getGate().getBarrier().isStatusCheck()) {
                BarrierStatusDto barrierStatusDto = BarrierStatusDto.fromBarrier(camera.getGate().getBarrier());
                AbstractCloseStrategy strategy = ManualCloseStrategy.builder()
                        .camera(camera)
                        .properties(properties)
                        .build();
                strategy.gateId = barrierStatusDto.gateId;
                SensorStatusCheckJob.add(strategy);
            } else {
                result = barrierService.closeBarrier(camera.getGate().getBarrier(), properties);
                if (result) {
                    String username = "";
                    if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CurrentUser) {
                        CurrentUser currentUser = (CurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                        if (currentUser != null) {
                            username = currentUser.getUsername();
                        }
                    }
                    eventLogService.sendSocketMessage(EventLogService.ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), "", "Ручное закрытие шлагбаума: Пользователь " + username + " закрыл шлагбаум для " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "выезда" : "въезда/выезда")) + " " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName(), "Manual closing gate: User " + username + " closed gate for " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "enter" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName());
                    eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, "Ручное закрытие шлагбаума: Пользователь " + username + " закрыл шлагбаум для " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "выезда" : "въезда/выезда")) + " " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName(), "Manual closing gate: User " + username + " closed gate for " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "enter" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName());
                }
            }

            return result;
        }

        return false;
    }

    @Override
    public void manualEnter(Long cameraId, String plateNumber) {
        carEventService.handleManualEnter(cameraId, plateNumber);
    }

    @SneakyThrows
    @Override
    public Boolean restartParkomat(String ip) {
        val headers = new HttpHeaders();
        headers.add("Content-Type", "text/plain");
        var restTemplate = new RestTemplateBuilder().basicAuthentication("admin", "TrassaE95").build();
        var result = restTemplate.exchange("http://" + ip + "/relay_ctrl.cgi",
                HttpMethod.POST,
                new HttpEntity<>("data=010101", headers),
                Void.class);
        log.info(result.toString());

        Boolean change = result.getStatusCodeValue() == 200;
        if (change) {
            String username = "";
            if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CurrentUser) {
                CurrentUser currentUser = (CurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                if (currentUser != null) {
                    username = currentUser.getUsername();
                }
            }
            eventLogService.createEventLog(Gate.class.getSimpleName(), null, null, "Ручная перезвгрузка паркомата: Пользователь " + username, "Manual restart parkomat: User " + username);
        }
        return change;
    }

    @Override
    public Boolean setEmergencyOpen(Boolean value, UserDetails currentUser) {
        if (currentUser != null) {
            if (value) {
                StatusCheckJob.emergencyModeOn = value;
            } else {
                final Collection<? extends GrantedAuthority> authorities = currentUser.getAuthorities();
                for (final GrantedAuthority grantedAuthority : authorities) {
                    String authorityName = grantedAuthority.getAuthority();
                    if ("ROLE_ADMIN".equals(authorityName)) {
                        StatusCheckJob.emergencyModeOn = value;
                    }
                }
            }
        }
        return StatusCheckJob.emergencyModeOn;
    }

    @Override
    public Boolean getEmergencyStatus() {
        return StatusCheckJob.emergencyModeOn;
    }

    @Override
    public Boolean passCar(Long cameraId, String platenumber, String snapshot) throws Exception {
        return carEventService.passCar(cameraId, platenumber, snapshot);
    }

    @Override
    public byte[] snapshot(Long cameraId) throws Throwable {
        Camera camera = cameraService.getCameraById(cameraId);
        if (camera == null || org.apache.commons.lang3.StringUtils.isEmpty(camera.getSnapshotUrl())) return null;
        Future<byte[]> future = getSnapshot(camera.getIp(), camera.getLogin(), camera.getPassword(), camera.getSnapshotUrl());
        while (true) {
            if (future.isDone()) {
                return future.get();
            }
        }
    }

    @Override
    @Async
    public Future<byte[]> getSnapshot(String ip, String login, String password, String url) {
        HttpHost host = new HttpHost(ip, 8080, "http");
        CloseableHttpClient client = HttpClientBuilder.create().
                setDefaultCredentialsProvider(provider(login, password))
                .useSystemProperties()
                .build();
        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpRequestFactoryDigestAuth(host, client);

        var restTemplate = new RestTemplate(requestFactory);
        val headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);

        StringBuilder address = new StringBuilder();
        address.append("http://");
        address.append(ip);
        address.append(url);
        //url.append("/cgi-bin/snapshot.cgi");

        HttpEntity entity = new HttpEntity(headers);
        log.info("getSnapshot(): address- " + address.toString() + "and entity - " + entity);
        return new AsyncResult<>(restTemplate.getForObject(address.toString(), byte[].class, entity));
    }

    @Override
    public JsonNode getTabsWithCameraList() {
        Locale locale = LocaleContextHolder.getLocale();
        String language = "en";
        if (locale.toString().equals("ru")) {
            language = "ru";
        }
        ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag(language));

        ArrayNode tabsWithCameras = objectMapper.createArrayNode();

        ObjectNode camerasWithoutTab = objectMapper.createObjectNode();
        camerasWithoutTab.put("name", bundle.getString("arm.withoutTab"));
        camerasWithoutTab.put("id", 0);

        List<Camera> cameraListWithoutTab = cameraService.cameraListWithoutTab();
        ArrayNode camerasWithoutTabs = objectMapper.createArrayNode();
        if (cameraListWithoutTab.size() > 0) {
            for (Camera camera : cameraListWithoutTab) {
                ObjectNode cameraNode = objectMapper.createObjectNode();
                cameraNode.put("id", camera.getId());
                cameraNode.put("name", camera.getName());
                cameraNode.put("ip", camera.getIp());
                cameraNode.put("parking", camera.getGate().getParking().getName());
                camerasWithoutTabs.add(cameraNode);
            }
        }
        camerasWithoutTab.set("cameras", camerasWithoutTabs);
        tabsWithCameras.add(camerasWithoutTab);

        List<CameraTab> cameraTabs = cameraService.cameraTabList();
        for (CameraTab cameraTab : cameraTabs) {
            ObjectNode cameraTabNode = objectMapper.createObjectNode();
            cameraTabNode.put("name", cameraTab.getName());
            cameraTabNode.put("id", cameraTab.getId());
            ArrayNode cameras = objectMapper.createArrayNode();
            for (Camera camera : cameraTab.getCameraList()) {
                ObjectNode cameraNode = objectMapper.createObjectNode();
                cameraNode.put("id", camera.getId());
                cameraNode.put("name", camera.getName());
                cameraNode.put("ip", camera.getIp());
                cameraNode.put("parking", camera.getGate().getParking().getName());
                cameras.add(cameraNode);
            }
            cameraTabNode.set("cameras", cameras);
            tabsWithCameras.add(cameraTabNode);
        }

        return tabsWithCameras;
    }

    @Override
    public Boolean configureArm(String json) throws JsonProcessingException {
        Map<Long, CameraTab> cameraTabMap = new HashMap<>(10);
        List<Long> tabsToDelete = new ArrayList<>(10);

        List<CameraTab> cameraTabList = cameraService.cameraTabList();
        List<Long> allPreviousTabs = new ArrayList<>(cameraTabList.size());
        for (CameraTab cameraTab : cameraTabList) {
            allPreviousTabs.add(cameraTab.getId());
        }

        JsonNode jsonNode = objectMapper.readTree(json);
        Iterator<JsonNode> tabIterator = jsonNode.iterator();
        while (tabIterator.hasNext()) {
            JsonNode tab = tabIterator.next();
            if ("0".equals(tab.get("id").textValue())) {
                JsonNode cameraJsonNode = tab.get("cameraArray");
                Iterator<JsonNode> cameraIterator = cameraJsonNode.iterator();
                while (cameraIterator.hasNext()) {
                    JsonNode camera = cameraIterator.next();
                    Camera c = cameraService.getCameraById(Long.parseLong(camera.get("id").textValue()));
                    c.setCameraTab(null);
                    cameraService.saveCamera(c, false);
                }
            } else {
                String tabIdString = tab.get("id").textValue();
                String tabName = tab.get("name").textValue();
                JsonNode cameraJsonNode = tab.get("cameraArray");
                if (cameraJsonNode.size() > 0) {
                    if (allPreviousTabs.contains(Long.parseLong(tabIdString))) {
                        allPreviousTabs.remove(Long.parseLong(tabIdString));
                    }
                    Iterator<JsonNode> cameraIterator = cameraJsonNode.iterator();
                    while (cameraIterator.hasNext()) {
                        JsonNode camera = cameraIterator.next();
                        CameraTab cameraTab = null;
                        if (cameraTabMap.containsKey(Long.parseLong(tabIdString))) {
                            cameraTab = cameraTabMap.get(Long.parseLong(tabIdString));
                        } else {
                            cameraTab = cameraService.findCameraTabByIdOrReturnNull(Long.parseLong(tabIdString));
                        }
                        if (cameraTab == null) {
                            cameraTab = new CameraTab();
                            cameraTab.setName(tabName);
                            cameraService.saveCameraTab(cameraTab);
                        }
                        Camera c = cameraService.getCameraById(Long.parseLong(camera.get("id").textValue()));
                        c.setCameraTab(cameraTab);
                        cameraService.saveCamera(c, false);
                        cameraTabMap.put(Long.parseLong(tabIdString), cameraTab);
                    }
                } else {
                    tabsToDelete.add(Long.parseLong(tabIdString));
                }
            }
        }

        tabsToDelete.addAll(allPreviousTabs);

        for (Long tabId : tabsToDelete) {
            CameraTab cameraTab = cameraService.findCameraTabByIdOrReturnNull(tabId);
            if (cameraTab != null) {
                cameraService.deleteCameraTab(cameraTab);
            }
        }

        return null;
    }

/*    public String snapshot(String ip, String login, String password, String url) throws Throwable {
        try {
            Future<byte[]> img = getSnapshot(ip, login, password, url);
            log.info("snapshot64 img: " + img);
            ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(img.get()))
                    .size(500, 500)
                    .outputFormat("JPEG")
                    .outputQuality(1)
                    .toOutputStream(resultStream);
            return StringUtils.newStringUtf8(Base64.encodeBase64(resultStream.toByteArray(), false));
        } catch (Exception e) {
            log.warning("Error ByteArrayInputStream img.Get: " + e);
            return "/9j/4AAQSkZJRgABAQEBLAEsAAD/4QCKRXhpZgAASUkqAAgAAAABAA4BAgBoAAAAGgAAAAAAAABObyBpbWFnZSBhdmFpbGFibGUgc2lnbi4gSW50ZXJuZXQgd2ViIGljb24gdG8gaW5kaWNhdGUgdGhlIGFic2VuY2Ugb2YgaW1hZ2UgdW50aWwgaXQgd2lsbCBiZSBkb3dubG9hZGVkLv/hBYZodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+Cjx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iPgoJPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4KCQk8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIiB4bWxuczpwaG90b3Nob3A9Imh0dHA6Ly9ucy5hZG9iZS5jb20vcGhvdG9zaG9wLzEuMC8iIHhtbG5zOklwdGM0eG1wQ29yZT0iaHR0cDovL2lwdGMub3JnL3N0ZC9JcHRjNHhtcENvcmUvMS4wL3htbG5zLyIgICB4bWxuczpHZXR0eUltYWdlc0dJRlQ9Imh0dHA6Ly94bXAuZ2V0dHlpbWFnZXMuY29tL2dpZnQvMS4wLyIgeG1sbnM6ZGM9Imh0dHA6Ly9wdXJsLm9yZy9kYy9lbGVtZW50cy8xLjEvIiB4bWxuczpwbHVzPSJodHRwOi8vbnMudXNlcGx1cy5vcmcvbGRmL3htcC8xLjAvIiAgeG1sbnM6aXB0Y0V4dD0iaHR0cDovL2lwdGMub3JnL3N0ZC9JcHRjNHhtcEV4dC8yMDA4LTAyLTI5LyIgeG1sbnM6eG1wUmlnaHRzPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvcmlnaHRzLyIgcGhvdG9zaG9wOkNyZWRpdD0iR2V0dHkgSW1hZ2VzL2lTdG9ja3Bob3RvIiBHZXR0eUltYWdlc0dJRlQ6QXNzZXRJRD0iOTIyOTYyMzU0IiB4bXBSaWdodHM6V2ViU3RhdGVtZW50PSJodHRwczovL3d3dy5pc3RvY2twaG90by5jb20vbGVnYWwvbGljZW5zZS1hZ3JlZW1lbnQ/dXRtX21lZGl1bT1vcmdhbmljJmFtcDt1dG1fc291cmNlPWdvb2dsZSZhbXA7dXRtX2NhbXBhaWduPWlwdGN1cmwiID4KPGRjOmNyZWF0b3I+PHJkZjpTZXE+PHJkZjpsaT5QZTNjaGVjazwvcmRmOmxpPjwvcmRmOlNlcT48L2RjOmNyZWF0b3I+PGRjOmRlc2NyaXB0aW9uPjxyZGY6QWx0PjxyZGY6bGkgeG1sOmxhbmc9IngtZGVmYXVsdCI+Tm8gaW1hZ2UgYXZhaWxhYmxlIHNpZ24uIEludGVybmV0IHdlYiBpY29uIHRvIGluZGljYXRlIHRoZSBhYnNlbmNlIG9mIGltYWdlIHVudGlsIGl0IHdpbGwgYmUgZG93bmxvYWRlZC48L3JkZjpsaT48L3JkZjpBbHQ+PC9kYzpkZXNjcmlwdGlvbj4KPHBsdXM6TGljZW5zb3I+PHJkZjpTZXE+PHJkZjpsaSByZGY6cGFyc2VUeXBlPSdSZXNvdXJjZSc+PHBsdXM6TGljZW5zb3JVUkw+aHR0cHM6Ly93d3cuaXN0b2NrcGhvdG8uY29tL3Bob3RvL2xpY2Vuc2UtZ205MjI5NjIzNTQtP3V0bV9tZWRpdW09b3JnYW5pYyZhbXA7dXRtX3NvdXJjZT1nb29nbGUmYW1wO3V0bV9jYW1wYWlnbj1pcHRjdXJsPC9wbHVzOkxpY2Vuc29yVVJMPjwvcmRmOmxpPjwvcmRmOlNlcT48L3BsdXM6TGljZW5zb3I+CgkJPC9yZGY6RGVzY3JpcHRpb24+Cgk8L3JkZjpSREY+CjwveDp4bXBtZXRhPgo8P3hwYWNrZXQgZW5kPSJ3Ij8+Cv/tALRQaG90b3Nob3AgMy4wADhCSU0EBAAAAAAAlxwCUAAIUGUzY2hlY2scAngAaE5vIGltYWdlIGF2YWlsYWJsZSBzaWduLiBJbnRlcm5ldCB3ZWIgaWNvbiB0byBpbmRpY2F0ZSB0aGUgYWJzZW5jZSBvZiBpbWFnZSB1bnRpbCBpdCB3aWxsIGJlIGRvd25sb2FkZWQuHAJuABhHZXR0eSBJbWFnZXMvaVN0b2NrcGhvdG8A/9sAQwAKBwcIBwYKCAgICwoKCw4YEA4NDQ4dFRYRGCMfJSQiHyIhJis3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AAAAAAAEEAAAAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEAgAAAEAggAAAAAAAAAAAAAAAAAAAAAAAAAAAAEgEkkkkgEkAAAgAAAAAAAAAAAAAAAAAAAAkgAAAAAAAAAAAEkkkgEAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAkAAAAAAAAAAAAAAAAAAgAAAAAAAAAAAAAAAgAAAAAAAAAAAAAAAAAAAAAgAAAAAAAAAAAAAAAEAAAAEAAAAAAAAAAAAAAAAgAAAAAAAAAgkkAAAAAAAAAgAAAAAAAAAAAAAAAEAAAAAAAAkgAAEEAAAAAAAAgAAAAAAAAAAAAAAAgAAAAAAEkAAAAAAAAAAAAAEAAAAAAAAAAAAAAAEAAAAAAEAAAkkkAAgAAAAAAgAAAAAAAAAAAAAAAgAAAAAAgEAAAAEAAgAAAAAEAAAAAAAAAAAAAAAEAAAAAAgEAAAAAEAEAAAAAAgAAAAAAAAAAAAAAAgAAAAAEEAAAAAAEAEAAAAAEAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAAAgAAAAAAAAAAAAAAAgAAAAAgAAAAAAAAgEAAAAAEAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAAAgAAAAAAAAAAAAAAAgAAAAAAAAAAAAAAEAAAAAAEAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAEAAAAAgAAAAAAAAAAAAAAAgAAAAEAgAAAAAAAEAgAAAAEAAAAAAAAAAAAAAAEAAAAAAEAAAAAAAAgAAAAAAgAAAAAAAAAAAAAAAgAAAAEAgAAAAAAAEAAAAAAEAAAAAAAAAAAAAAAEAAAAAgEgAAAAAAAAAAAAAAgAAAAAAAAAAAAAAAgAAAAAgAgAAAAAAAEAAAAAEAAAAAAAAAAAAAAAEAAAAAEgEAAAAAAgEgAAAAAgAAAAAAAAAAAAAAAgAAAAAEAkgAAAAgAAAAAAAEAAAAAAAAAAAAAAAEAAAAAAkAEgAAEgAkAAAAAAgAAAAAAAAAAAAAAAgAAAAAAAAAAkkgAAAAAAAAEAAAAAAAAAAAAAAAAAAAAAAAkAAAAAAgAAAAAAAAAAAAAAAAAAAAAAAkAAAAAAAAkAAAAgAAAAAAAEAAAAAAAAAAAAAAAAAAAAAAAAAEkgkAAAAAAAAEgAAAAAAAAAAAAAAAAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAEgAAAAAAAAAAAAAAAAAEEkkkkkkkkkkkkkkkkkAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAgAAAAEgAAAAAAAAAAAAAAAAAAAAAAAAAAAAEgAAAAAAEkAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAAkAAAEEEAkEkgAEAggEAAAAAAAAAAAAAAAEggAAkgAAAkkkgAkEgAAEkgAAAAAAAAAAAAAAAAgAEEgAEAAAAEgAAkgAAkEkgAAAAAAAAAAAAAAAEAAgEAEAAAAAEEgkEAgAggkgAAAAAAAAAAAAAAAgEgAgEgAAAAAgkAgAkAkEAAAAAAAAAAAAAAAAAEAAAAgAAAAAAEEgkggEEAgAkAAAAAAAAAAAAAAAkAEgkgAAEEAAgAAAkAgkgEAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEkgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEkEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEAAAAgAAkAAAAAAAAAAAAAAAAAAAAAAAAAAAAAkAAAAEgAEAAAAAAAAAAAAAAAAAAAAAkEggAgAAAAAEgAkgggAkEAAAAAAAAAAAAAAAAAAkggEEggkkAEEgEkkkAAkAAAAAAAAAAAAAAAAAAEgEggAgAkgAAkgkAAgEgAgAAAAAAAAAAAAAAAAAEAAgAAEEkAEggEgAkAkkgAAAAAAAAAAAAAAAAEkkAAAkAgkgAEAAkAAgEgAAAAAAAAAAAAAAAAAAkAkAkEAgEkAAgEEgAEAkEEAAAAAAAAAAAAAAAAAkAAggAAEEggEggkAgkgAAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAH//EABQRAQAAAAAAAAAAAAAAAAAAAMD/2gAIAQMBAT8QOU//xAAUEQEAAAAAAAAAAAAAAAAAAADA/9oACAECAQE/EDlP/8QALRABAAEBBQcFAAMBAQEAAAAAAREAITFBUWEQcYGhsdHwMFCRwfEgQOFggJD/2gAIAQEAAT8Q/wDdqTKVDID/AGv2/ev2/ev2/ev2/ev2/ev2/ev2/ev2/ev2/ev2/ev2/ev2/ejSEhpJjju/4Cb0twT/AGdhRrwJr9871++d6/fO9fvnev3zvX753r9871++d6lp1oC/Y8jEDfWNCJJaPv1oECG+sKWWXZZamJaR6LWEKHKRnbNiT4pdyj36Za7sn3t5X09G78LHbNzkh/nTYsX1oPmtB81oPmtB81oPmtB8+6qAqwF9PK3HcWHLbNsOAky1K0PhpWh8NK0PhpWh8NK0PhpWh8NK0PhpWh8NKe5OUInIJ37bpt71OPIk0aIkBtRjY1LNqWbUs2pZtSzalm0hCqdb2w9qBnTbT4piN6X3Ffie9fje9fje9fje9fje9fje9fje9fje9AKQCAE345f0U1ZwQTJjVpl5hp1rTU6anTU6anTU6akIAYXAe9IxqSfU1fEZLXxf7MQbEOOhUmd13WHCnCkvVlfZRUIolyU4LlKxue9FSNeYrJPY4kwlaao2KSwd/YAVgJaKAJdAnhfU0xnuYXfz02z3HJpRZC0b1iPsTniiOP8Aj2GByxbg4uwywIRJEq5U90OHzP8ANwtjewwP17DfrN8CaXyWJq/0oOIckfJsqKTMiVRi0dDuod+8HtSxYnce1Mv3QdlDro8l91LojAT8j0HhSXDfaeabXqlM0Kr0/mKAYRkazlBd+PP2BnmI+SH9GPUvAv3GNHii6/wXUAEBAegEhbVdmjAxYNjefyyah2wUUVjIs4llScLcwg3zfTuGynF9Biv+xP37BzfQ9d2gWCjB3ovd99UQJfwPkPulDHnc6mfr3/hYewc30PWx06cBm1BKNblvYfwWBt6wUkwLLB8tNMZrV9UpZug+6Ut3gU9Zm8vulxIxTmHapcPjN/BbaC7vWlI9UdwzPWv/AAsPYOb6HqrdZAMaGQWnMct21QKoBetJDAsXc3Z1ks6Ww3GH8zJviopkJXAWcSjpgSBkdoQRex1nUzZw66nq3/hYewc30PVC5RYfk47QDllLgpyEm7HW9vTJIs3nyKASWRNoPZGSOIerf+Fh7BzfQ9SFU/HGHGgAAQFwbFglp0tJeYs92Xqy+VO0UZMCRLk2y0STLHE8z9S/8LD2Dm+h6llEL2O/HaoRAtHjb39dZgGVxMThtsOnkN58epf+Fh7BzfQ9O+IMcWiPgQNDYDJASuVKRZAMhd67D2De0pspImjsQRG0asxsfgvOXp3/AIWHsHN9D05sJPpfva7VEI438p/orISngbTq7YkLBneMdI9O/wDCw9g5voenL/pRe22ArePCA6v9GKTYHwY+9sv+9D/PTv8AwsPYOb6HpjeC5O2bLN5/0YmZDptEpkej6d/4WHsHN9D09wl5O2PCrPh/3+iuYDmbYs4ej6d/4WHsHN9D07dLr4P+7Y0L98Sfr+i6osAu+16G29F6nI9O/wDCw9g5voenb9gmuN3ONqCp4yLf6LGIVW5u5BtsW2FxNv2enf8AhYewc30PTVeBDeVcSscdl9OUQnGdrvXngi0ZYmjLgIAwNjgwCrkVerPuGHp3/hYewc30PUv+udS/3rtvjS6LEpIYb/WRIhWDfgfN/wAbZyIsertx9S/8LD2Dm+h6k8ikHNXlH8GIYjtWaXaJc5Pv1XtxZbF22giABKuFI0w7Qx4+pf8AhYewc30PVNXRtTntIJCSNMSqtO817KRGEhPSUQv0sd/IoyYEAEAbRawFs+Dj6t/4WHsHN9D1QSIGRMKAmCt05n8JsE2DfvMaaVh1Djl/NsXTxMeFOQBtA+A/hGEC3OrpTtnyl6+rf+Fh7BzfQ9ZALZC8pSgS7DWdv4rLPaz5KfXSQD6qTb96PupFj3J+6anTgd1Lg5qOdXXfwdxCvLtWkUtlX1r/AMLD2BoGEuT10BERkTCnMm4CWm5jvq4qEkj6bchJUgKAyIssNx90mF0qSvroScUcvYHMJTb0Wc/6Meq33t4UeP3n8LznUZzvI/nqGIihXPF+jU5Zw2A4f0VUIU/G379hQ6D8BvOD/SKBVyoaLAEwB1W0d8fP2pcHh/mlweP+a61bsqVFBwBzX0pcMVL/AEgQX5MhfQAAICwPYWIgtlnluadu+EvH2kECjAF7Vm5faWHsadoSzBoe9JwnO53OPs91BAKhcoF5ua6+yp8txduypx0Jw+b6fsrSPUr8BX4CvwFfgK/AV+Ar8BX4CvwFfgK/AV+Ar8BX4CvwFfgK/AV+Ar8BX4CvwFfgK/AV+Ar8BX4CvwFfgK/AV+Ar8BX4CvwFfgKRuWcvQpI0BQfLWZSoLXe4/wDqS0g6BcMUoItrCNT+Fp10G4aw1QM09KxaFmZWCjsM3Aho0Iglz734vNQ03OmA7JTYSU7szb4vNrw+T0lXQIkvG8afDEw06NKCCD3vzeasdKJ3WP18VdoW0+Nu3xebXh8mxeA4uO4xpAM8Ao1qmjKFzPYCsdz/ABJkDsMYKAwohRZLGwMDZlEWTWDJTIZme1ImWZDHyxq+5kUaQLAm8HcqzsNxbuCp/QG1fihDy48DeUEM0gyJsBinqO4UZBmh5FXszRlGi2AWRaO1yYEqsAU5Wmb9KvQ5BjQ4yt2LucaSYuECGhKQMjhJOyHUupuCrUT4H6SoCZubzpxuQOB7B5PNULJQ3lTxS5yGDQBcDUydni82vD5KbqJj2q3QlAuGAU6KpNg0LL6VCTBYlGhyK5OtW2QSjah/B4/KvC5mwWmno1OoOYEJsN80MGkywaFxpiJ0kg0aNJklsMzEaQLeLxhDWhz5ptdqTFvFeOTVsJC2wzPvYYImZE2rzpQLMy3QpgJcFCVYADByUhTKVxyfjYmceI+UFW7rgjk6UDbTvHWpxJFngM5UpotKM6MFl0SrlUmM3AoMLNgMD9BR0zY2cClpY1ps4lS+HLrk7JWNgoycT5/v+TzV47Iq+wEkX4h91Mmiz5AoBQyGJVjy7a8PkpkET6gf68qdjMFvWH3tUBKNNLaUSEeahkkx2+PyrwuZs5P0aaDI4NbjrtNqCTG+37oMLWGbMdDYxi1D8x0WmWYpcR2iToto8KcjeF9xT1wBEJmIx30+nb61CF4G+LKFzIparUVhA0Nh7S4MxrXcK8bkUiaxvCsOrTCqgzGL36r83X5urMYrJtGnTb+NyD1n+/5PNXjcjZdeRSLsQq+EJmyxPv5oQfCa8PkqWh9xTI8Y2yB7g5+KxoITeCfjb4/KvC5mzk/Rrw2Zt5b0K5XqbOQdFeRo7EUKHYmtOFE7Ssv20ITekH3RLxyRujTWuV+9TYsvUpibn2fw531rxuRUtC58tAL5g2bpwK/d7K/d7K/d7KHHlJsRGv8Af8nmrxuRswuXQGiOphm76avicOkhZXh8lEZNnuP+hRJAZLg4Pz123LmjvvfigYSC6Ba8v4ePyrwuZs5P0a8NmbeW9CuV6mzkHRXkaOx07UfmhWCVZipLsOzJhlMR0rlfvRp40spKxw3LkjTSiG0zYmxxAhY+ApKK9Za8bkUIljeNadGoPhGXJh8dPY/J5q8bkbYCxUExzcNnh8lTvkDMpSBdYljo60ic0KsGudMMhkR90WmsC2GgVLkIwmXL+Hj8q8LmbOT9GvDZm3lvQrleps5B0V5GjsnYkYS284UFksX8MkaEhTcxA+L6bzpEvLDaaWVvInN2TAW6vdSmyEtklbyrvv8AOPipcB7Ag9M2hu4ocGjB+cFHRJQ6a1H5AZuW5rQYYLJvkzphsn4d6ag6bZT6pnJENz/fRRYkr22iPgQ3G1GYCEcSl371GPBg0NjixshI02B3Cw+GalBOUh9VAEW+8uL/ABNOWBMWNHOiSV5sDthAIt8aVNCkts8NrrglDKUdcSzjOxtwIAxQGKSluxWtYUm4WRE5lFyS6cnwWURhAAYhN1TVlJbZt3GaQ/JUGM6o+FplkIwIAKbiTYzVaMe4DYvcbwkpQM+FnzmhNoN4+qWBGxbkuoAAAIAw/wCCJLIm1DCz5oYsnCR8NQgWopOVs4YkyKt+VJccB9/8TM7AgJQdKXgnMvuKYhGp7lR5g1V4tGUuA/8AD6NgCVcCmJoxdKZx/tRGkTMaUcBhUJaUjMEi0fQLprBQuq/QoUv8W5rWE4qaosracPeQWQKk1IprNEENjPOsEjTm1y3qV4zP0PMZfy03Ncw15zL2kiVszK8C2l41ATq0AlWNjymrdGQkjsUIxS8/wqAUyGI7bIBplR8FT+TXgt41qYdxTaatZ84qDCZj9NZtKi83mG1QjaLPApwpAR0rlvUrxmexEILVWApNIWHWXULBMzj3pqE5bkN+3zGWzTdp3jY43VEhc0xyazaVF5vMNjc1zDXnMtji9skBSpIY2POKBb1s+S0Pa7kn2SUwpTJG2woW8AAfdJkLIIBnrSi4irk3OyzaOYX85qbSY3Fafexj4UnQpWCsQ3thQiv4hvedRA2V2TH7nlRhowA5zQGoSwy3iXVbghnxzGrjZDTZzqvJZVy3qV4zPYFFGQMAue9FALXRxe1PeQEifVSJvT3mZRNZffZOzzGVAdMuNO4KGETPDe0pAzOHdQpjQjzEoEIWz5gpua5hrzmVTvgTIoYckPJa04jO+Ozi0Npksjt4l1OmAEjjnxqJsiaPskUdmBQxXGxINANSFIymykXvY4lQgm2b1jzippQU9955rsnFETuve3Go2SEt+HO3htgQBisUrBnKFL4gVkmLaVRuDr97OdV5LKuW9SvGZ/wZhvIBpNchtoE/ynJNg/B/3Z5jKoIm+MM3ntjMYDJc/HSlRbnNRj75U3Ncw15zKlVQs7h/qfFCTMTTW9+Ou0gYi3hZ2pWaVhubft9iRMAlXAqCN2WfgyKtHfFmn7acWxoLTsMqpMYYrw+Shyli3NJxxbUb6u2z3KjEmBxWv1UcELPo78dkUQ2PaadWmIsm2F9FHTo5L9VJ+RkIvrwWRs51Xksq5b1K8ZnsNKZu5ulQq+5ZDh3os6YpeVG0rMEiya531NnmMv4aAu3kHw1Ow1Pim5rmGvOZUjhCXNoiggsBNed7153vSIpl0p+6TAmjFZ7EpMIp0tXpR2A1rGLudXEGxSpSHSWvD5NkMLluP8irNRPjW9Rq36nrIWXlQowABkU5d/xJNIQRCs1vobYYA2OAZROja/deCyNnOq8llXLepXjM9iirITILKK5DnnI2Cn2phoH+1zvqbPMZVPS3KaN33tkFegywPunfLgdV7DTc1zDXnMqeMWQp5n3QFkQBng/Xsw4IYBjF/JaMcFjMG+gosttinCnfGsMAaa0rd0KvKUnPptkcJHin+TTWRwdI/VRrsHFx+vnYLLA/zIooWNdEpMEGFJ0aBjisXG9qCchKM2FOTy6Rs51Xksq5b1K8ZnsWAzy5zfzqUnlMWJk5U+lcLQ0rlCAuLdOrSgs31NinR6ChPkwm5xGijiFTzKbSwUIG9aUFYwD4CjThtgxo3Ncw15zKn5hbcouaS0+A4lDMUgSfZSZPibqs1ubyDnQAGRJPY5KaljKfqkQBmJPSg6o3BBuFSbSzeRZnNKjZdhExsSSGknMJawyvqAIvwiTe7X5qJEF+8xoEE5mR6UDUzC+TFpGyEBKR8lQKhbmwsjPYtktZ8zVosxSzgoUbLFiYtoUKPcZjjstGwtCxWlHeRII8pqNQzDfxe1AgIjAlkb3OikFurC/jsdzsMSIAz0qE4EIhkLzKpdyi1HE7UrgpVzipsrkNv/jY2lJGVrPmatKmKWcbB0kZLiVLJpgfYUlsZpL9UiO5k+bamPhCWbD/AOFv/9k=";
        }
    }*/

   /* @Override
    public void enableSnapshot(Long cameraId) {
        Camera camera = cameraService.getCameraById(cameraId);
        String name = "snapshot-camera-" + camera.getId().toString();

        if (CameraSnapshotJob.threads.containsKey(name)) {
           return;
        }
        CameraSnapshotJob.threads.put(name, SnapshotThreadDto.builder()
                .isActive(false)
                .thread(new GetSnapshotThread(name,
                        camera.getId(),
                        camera.getIp(),
                        camera.getLogin(),
                        camera.getPassword(),
                        camera.getSnapshotUrl(),
                        this,
                        carImageService))
                .build());
    }*/

    private CredentialsProvider provider(String login, String password) {
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials =
                new UsernamePasswordCredentials(login, password);
        provider.setCredentials(AuthScope.ANY, credentials);
        return provider;
    }

    public String encodeBase64StringWithSize(byte[] bytes)  {
        try {
            ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(bytes))
                    .size(500, 500)
                    .outputFormat("JPEG")
                    .outputQuality(1)
                    .toOutputStream(resultStream);
            return StringUtils.newStringUtf8(Base64.encodeBase64(resultStream.toByteArray(), false));
        }
        catch (Exception e) {
            log.warning("Error of encodeBase64StringAndSize: " + e);
            return noPicture;
        }
    }
    public byte[] manualSnapShot(CameraStatusDto cameraStatusDto) {
        try {
            final int CONN_TIMEOUT = 10;
            String ip = cameraStatusDto.ip;
            HttpHost host = new HttpHost(ip, 8080, "http");
            CloseableHttpClient client = HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(provider(cameraStatusDto.login, cameraStatusDto.password))
                    .useSystemProperties()
                    .build();
            HttpComponentsClientHttpRequestFactory requestFactory =
                    new HttpRequestFactoryDigestAuth(host, client);
            requestFactory.setConnectTimeout(CONN_TIMEOUT * 1000);
            requestFactory.setConnectionRequestTimeout(CONN_TIMEOUT * 1000);
            requestFactory.setReadTimeout(CONN_TIMEOUT * 1000);
            var restTemplate = new RestTemplate(requestFactory);
            val headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            StringBuilder address = new StringBuilder();
            address.append("http://");
            address.append(ip);
            address.append(cameraStatusDto.snapshotUrl);
            HttpEntity entity = new HttpEntity(headers);
            byte[] imageBytes = restTemplate.getForObject(address.toString(), byte[].class, entity);
            return imageBytes;
        }
        catch (Exception e) {
            log.warning("RestTemplate timeout for snapshot: " + e);
            byte[] encodeToString = Base64.decodeBase64(noPicture);
            return encodeToString ;
        }
    }
}
