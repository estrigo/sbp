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

@Log
@Service
public class ArmServiceImpl implements ArmService {
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

                        CameraStatusDto cameraStatusDtoById = StatusCheckJob.findCameraStatusDtoById(cameraId);
                        try {
                            String newSnapShot = ("data:image/jpg;base64,"+ (snapshot(cameraStatusDtoById.ip, cameraStatusDtoById.login, cameraStatusDtoById.password, cameraStatusDtoById.snapshotUrl)));
                            String carImageUrl = carImageService.saveImage(newSnapShot, new Date(), debtPlatenumber);
                            properties.put(StaticValues.carImagePropertyName, carImageUrl);
                            properties.put(StaticValues.carSmallImagePropertyName, carImageUrl.replace(StaticValues.carImageExtension, "") + StaticValues.carImageSmallAddon + StaticValues.carImageExtension);
                            System.out.println("check");
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }

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

    public String snapshot(String ip, String login, String password, String url) throws Throwable {
        Future<byte[]> img = getSnapshot(ip, login, password, url);

        ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(img.get()))
                .size(500, 500)
                .outputFormat("JPEG")
                .outputQuality(1)
                .toOutputStream(resultStream);

        String base64 = StringUtils.newStringUtf8(Base64.encodeBase64(resultStream.toByteArray(), false));
        return base64;
    }

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
}
