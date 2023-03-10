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
import kz.spt.app.model.dto.GateStatusDto;
import kz.spt.lib.model.dto.CameraStatusDto;
import kz.spt.app.model.strategy.barrier.close.AbstractCloseStrategy;
import kz.spt.app.model.strategy.barrier.close.ManualCloseStrategy;
import kz.spt.app.model.strategy.barrier.open.AbstractOpenStrategy;
import kz.spt.app.model.strategy.barrier.open.ManualOpenStrategy1;
import kz.spt.app.model.strategy.barrier.open.ManualOpenStrategy2;
import kz.spt.app.service.BarrierService;
import kz.spt.app.service.CameraService;
import kz.spt.lib.utils.MessageKey;
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
import org.springframework.context.annotation.Bean;
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
import org.springframework.transaction.annotation.Transactional;
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
@Transactional(noRollbackFor = Exception.class)
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

    private PropertyService propertyService;

    public ArmServiceImpl(CameraService cameraService, BarrierService barrierService, EventLogService eventLogService,
                          CarEventService carEventService, CarImageService carImageService,
                          PaymentService paymentService, PropertyService propertyService) {
        this.cameraService = cameraService;
        this.barrierService = barrierService;
        this.eventLogService = eventLogService;
        this.carEventService = carEventService;
        this.carImageService = carImageService;
        this.paymentService = paymentService;
        this.propertyService = propertyService;
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

                    Map<String, Object> messageValues = new HashMap<>();
                    messageValues.put("username", username);
                    messageValues.put("description", camera.getGate().getDescription());
                    messageValues.put("parking", camera.getGate().getParking().getName());

                    String key = camera.getGate().getGateType().equals(Gate.GateType.IN) ? MessageKey.MANUAL_OPEN_IN :
                            (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? MessageKey.MANUAL_OPEN_OUT : MessageKey.MANUAL_OPEN);

                    eventLogService.sendSocketMessage(EventLogService.ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), "", messageValues, key);
                    eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, messageValues, key);
                }
            }

            return result;
        }

        return false;
    }

    @Override
    public Boolean openGate(Long cameraId, String reason) throws Exception {
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
                String newSnapShot;
                String carImageUrl;
                try {
                    CameraStatusDto cameraStatusDtoById = StatusCheckJob.findCameraStatusDtoById(cameraId);
                    byte[] bytes = carImageService.manualSnapShot(cameraStatusDtoById);
                    newSnapShot = "data:image/jpg;base64," + carImageService.encodeBase64StringWithSize(bytes);
                    carImageUrl = carImageService.saveImage(newSnapShot, new Date(), null);
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
                                if (timeDiffInMillis > 2 * 1000) { // ???????? ???????????? 2 ???????????? ???? ?????????????????? ??????????????
                                    hashtable.put(debtPlatenumber, System.currentTimeMillis());
                                } else {
                                    return false;
                                }
                            } else {
                                hashtable.put(debtPlatenumber, System.currentTimeMillis());
                            }
                        }

                        eventLogService.sendSocketMessage(EventLogService.ArmEventType.SNAPSHOT, EventLog.StatusType.Success, camera.getId(), debtPlatenumber, carImageUrl);

                        Map<String, Object> messageValues = new HashMap<>();
                        messageValues.put("username", username);
                        messageValues.put("description", camera.getGate().getDescription());
                        messageValues.put("parking", camera.getGate().getParking().getName());
                        messageValues.put("additionalMessage", MessageKey.REASON);
                        messageValues.put("reason", reason);

                        String key = camera.getGate().getGateType().equals(Gate.GateType.IN) ? MessageKey.MANUAL_OPEN_IN :
                                (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? MessageKey.MANUAL_OPEN_OUT : MessageKey.MANUAL_OPEN);

                        eventLogService.sendSocketMessage(EventLogService.ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), debtPlatenumber, messageValues, key);
                        eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, messageValues, key, EventLog.EventType.MANUAL_GATE_OPEN);

                        if (debtPlatenumber != null) {
                            String imageUrl = carImageService.saveImage(newSnapShot, new Date(), debtPlatenumber);
                            properties.put(StaticValues.carImagePropertyName, imageUrl);
                            properties.put(StaticValues.carSmallImagePropertyName, imageUrl.replace(StaticValues.carImageExtension, "") + StaticValues.carImageSmallAddon + StaticValues.carImageExtension);
                            paymentService.createDebtAndOUTState(debtPlatenumber, camera, properties);
                        }
                    } else if (Gate.GateType.IN.equals(camera.getGate().getGateType())) {
                        String debtPlatenumber = eventLogService.findLastWithDebts(camera.getGate().getId());

                        if (debtPlatenumber != null) {
                            properties.put("carNumber", debtPlatenumber);
                        }

                        eventLogService.sendSocketMessage(EventLogService.ArmEventType.SNAPSHOT, EventLog.StatusType.Success, camera.getId(), debtPlatenumber, carImageUrl);

                        Map<String, Object> messageValues = new HashMap<>();
                        messageValues.put("username", username);
                        messageValues.put("description", camera.getGate().getDescription());
                        messageValues.put("parking", camera.getGate().getParking().getName());
                        messageValues.put("additionalMessage", MessageKey.REASON);
                        messageValues.put("reason", reason);

                        String key = camera.getGate().getGateType().equals(Gate.GateType.IN) ? MessageKey.MANUAL_OPEN_IN :
                                (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? MessageKey.MANUAL_OPEN_OUT : MessageKey.MANUAL_OPEN);

                        eventLogService.sendSocketMessage(EventLogService.ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), debtPlatenumber, messageValues, key);
                        eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, messageValues, key, EventLog.EventType.MANUAL_GATE_OPEN);

                        if (debtPlatenumber != null) {
                            String imageUrl = carImageService.saveImage(newSnapShot, new Date(), debtPlatenumber);
                            properties.put(StaticValues.carImagePropertyName, imageUrl);
                            properties.put(StaticValues.carSmallImagePropertyName, imageUrl.replace(StaticValues.carImageExtension, "") + StaticValues.carImageSmallAddon + StaticValues.carImageExtension);

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
    public JsonNode openPermanentGate(Long cameraId) throws ModbusProtocolException, ModbusNumberException, IOException, ParseException, InterruptedException, ModbusIOException {
        ObjectNode objectNode = StaticValues.objectMapper.createObjectNode();
        Camera camera = cameraService.getCameraById(cameraId);
        if (camera != null && camera.getGate() != null && camera.getGate().getBarrier() != null) {

            String username = "";
            if (SecurityContextHolder.getContext().getAuthentication() != null && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CurrentUser) {
                CurrentUser currentUser = (CurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                if (currentUser != null) {
                    username = currentUser.getUsername();
                }
            } else {
                username = "system";
            }

            Map<String, Object> properties = new HashMap<>();
            SimpleDateFormat format = new SimpleDateFormat(dateFormat);
            properties.put("eventTime", format.format(new Date()));
            properties.put("cameraIp", camera.getIp());
            properties.put("cameraId", cameraId);
            properties.put("gateName", camera.getGate().getName());
            properties.put("gateDescription", camera.getGate().getDescription());
            properties.put("gateType", camera.getGate().getGateType().toString());
            properties.put("type", EventLog.StatusType.Allow);
            properties.put("event", EventLog.EventType.MANUAL_GATE_OPEN);

            Map<String, Object> messageValues = new HashMap<>();
            messageValues.put("username", username);
            messageValues.put("description", camera.getGate().getDescription());
            messageValues.put("parking", camera.getGate().getParking().getName());


            String key = camera.getGate().getGateType().equals(Gate.GateType.IN) ? MessageKey.MANUAL_OPEN_IN :
                    (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? MessageKey.MANUAL_OPEN_OUT : MessageKey.MANUAL_PASS);

            eventLogService.sendSocketMessage(EventLogService.ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), "", messageValues, key);
            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, messageValues, key, EventLog.EventType.MANUAL_GATE_OPEN);

            Boolean result = barrierService.openPermanentBarrier(camera.getGate().getBarrier());
            objectNode.put("result", result);

            objectNode.set("permanentOpenCameraIds", getBarrierOpenCameraIds());
        }
        return objectNode;
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

                    Map<String, Object> messageValues = new HashMap<>();
                    messageValues.put("username", username);
                    messageValues.put("description", camera.getGate().getDescription());
                    messageValues.put("parking", camera.getGate().getParking().getName());


                    String key = camera.getGate().getGateType().equals(Gate.GateType.IN) ? MessageKey.MANUAL_CLOSE_IN :
                            (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? MessageKey.MANUAL_CLOSE_OUT : MessageKey.MANUAL_CLOSE);

                    eventLogService.sendSocketMessage(EventLogService.ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), "", messageValues, key);
                    eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, messageValues, key);
                }
            }

            return result;
        }

        return false;
    }

    @Override
    public JsonNode closePermanentGate(Long cameraId) throws IOException, ParseException, InterruptedException, ModbusProtocolException, ModbusNumberException, ModbusIOException {
        ObjectNode objectNode = StaticValues.objectMapper.createObjectNode();

        Camera camera = cameraService.getCameraById(cameraId);
        if (!StatusCheckJob.emergencyModeOn && camera != null && camera.getGate() != null && camera.getGate().getBarrier() != null) {

            String username = "";
            if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CurrentUser) {
                CurrentUser currentUser = (CurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                if (currentUser != null) {
                    username = currentUser.getUsername();
                }
            }

            Map<String, Object> properties = new HashMap<>();
            SimpleDateFormat format = new SimpleDateFormat(dateFormat);
            properties.put("eventTime", format.format(new Date()));
            properties.put("cameraIp", camera.getIp());
            properties.put("cameraId", cameraId);
            properties.put("gateName", camera.getGate().getName());
            properties.put("gateDescription", camera.getGate().getDescription());
            properties.put("gateType", camera.getGate().getGateType().toString());
            properties.put("type", EventLog.StatusType.Allow);
            properties.put("event", EventLog.EventType.MANUAL_GATE_CLOSE);

            Map<String, Object> messageValues = new HashMap<>();
            messageValues.put("username", username);
            messageValues.put("description", camera.getGate().getDescription());
            messageValues.put("parking", camera.getGate().getParking().getName());


            String key = camera.getGate().getGateType().equals(Gate.GateType.IN) ? MessageKey.MANUAL_CLOSE_IN :
                    (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? MessageKey.MANUAL_CLOSE_OUT : MessageKey.MANUAL_CLOSE);

            eventLogService.sendSocketMessage(EventLogService.ArmEventType.CarEvent, EventLog.StatusType.Allow, camera.getId(), "", messageValues, key);
            eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, messageValues, key, EventLog.EventType.MANUAL_GATE_CLOSE);

            Boolean result = barrierService.closePermanentBarrier(camera.getGate().getBarrier(), properties);

            objectNode.put("result", result);
            objectNode.set("permanentOpenCameraIds", getBarrierOpenCameraIds());
        }
        return objectNode;
    }

    @Override
    public JsonNode getBarrierOpenCameraIds(){

        ArrayNode cameraIdArray = StaticValues.objectMapper.createArrayNode();

        List<Long> openBarrierCameraIds = barrierService.getBarrierOpenCameraIdsList();
        for(Long id : openBarrierCameraIds){
            cameraIdArray.add(id);
        }

        return cameraIdArray;
    }

    @SneakyThrows
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

            Map<String, Object> messageValues = new HashMap<>();
            messageValues.put("username", username);

            eventLogService.createEventLog(Gate.class.getSimpleName(), null, null, messageValues, MessageKey.MANUAL_RESTART);
        }
        return change;
    }

    @Bean
    public void updateEmergencyStatus() throws ModbusProtocolException, ModbusNumberException, IOException, ParseException, InterruptedException, ModbusIOException {
        String emergencyStatusValue= propertyService.getValue(StaticValues.emergencyStatusKey);
        if(StaticValues.emergencyStatusOpenValue.equals(emergencyStatusValue)){
            for (GateStatusDto gateStatusDto : StatusCheckJob.globalGateDtos) {
                if(Gate.GateType.OUT.equals(gateStatusDto.gateType)){
                    CameraStatusDto cameraStatusDto = gateStatusDto.frontCamera;
                    openPermanentGate(cameraStatusDto.id);
                }
            }
            StatusCheckJob.emergencyModeOn = true;
        }
    }

    @Override
    public Boolean setEmergencyOpen(Boolean value, UserDetails currentUser) throws ModbusProtocolException, ModbusNumberException, IOException, ParseException, InterruptedException, ModbusIOException {
        if (currentUser != null) {
            if (value) {
                StatusCheckJob.emergencyModeOn = value;

                for (GateStatusDto gateStatusDto : StatusCheckJob.globalGateDtos) {
                    if(Gate.GateType.OUT.equals(gateStatusDto.gateType)){
                        CameraStatusDto cameraStatusDto = gateStatusDto.frontCamera;
                        openPermanentGate(cameraStatusDto.id);
                    }
                }
                propertyService.setValue(StaticValues.emergencyStatusKey, StaticValues.emergencyStatusOpenValue);

            } else {
                final Collection<? extends GrantedAuthority> authorities = currentUser.getAuthorities();
                for (final GrantedAuthority grantedAuthority : authorities) {
                    String authorityName = grantedAuthority.getAuthority();
                    if ("ROLE_ADMIN".equals(authorityName) || "ROLE_SUPERADMIN".equals(authorityName)) {
                        StatusCheckJob.emergencyModeOn = value;

                        for (GateStatusDto gateStatusDto : StatusCheckJob.globalGateDtos) {
                            if(Gate.GateType.OUT.equals(gateStatusDto.gateType)){
                                CameraStatusDto cameraStatusDto = gateStatusDto.frontCamera;
                                closePermanentGate(cameraStatusDto.id);
                            }
                        }
                        propertyService.setValue(StaticValues.emergencyStatusKey, StaticValues.emergencyStatusClosedValue);
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
    public Boolean passCar(Long cameraId, String platenumber) throws Exception {
        return carEventService.passCar(cameraId, platenumber);
    }

    @Override
    public byte[] snapshot(Long cameraId) throws Throwable {
        CameraStatusDto cameraStatusDto = StatusCheckJob.findCameraStatusDtoById(cameraId);
        if (cameraStatusDto == null || org.apache.commons.lang3.StringUtils.isEmpty(cameraStatusDto.getSnapshotUrl())) return null;
        Future<byte[]> future = getSnapshot(cameraStatusDto.getIp(), cameraStatusDto.getLogin(), cameraStatusDto.getPassword(), cameraStatusDto.getSnapshotUrl());
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
                setDefaultCredentialsProvider(carImageService.provider(login, password))
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

        HttpEntity entity = new HttpEntity(headers);
        log.info("getSnapshot(): address- " + address.toString() + "and entity - " + entity);
        return new AsyncResult<>(restTemplate.getForObject(address.toString(), byte[].class, entity));
    }

    @Override
    public JsonNode getTabsWithCameraList() {
        Locale locale = LocaleContextHolder.getLocale();
        String language = locale.getLanguage();

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
                cameraNode.put("startTime", String.valueOf(camera.getStartTime()));
                cameraNode.put("endTime", String.valueOf(camera.getEndTime()));
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
                cameraNode.put("startTime", String.valueOf(camera.getStartTime()));
                cameraNode.put("endTime", String.valueOf(camera.getEndTime()));
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

    @Override
    public JsonNode getCameraList() {
        List<Camera> cameraList = cameraService.cameraList();

        List<Long> barrierOpenCameraIds = barrierService.getBarrierOpenCameraIdsList();

        ArrayNode cameras = objectMapper.createArrayNode();
        if (cameraList.size() > 0) {
            for (Camera camera : cameraList) {
                ObjectNode cameraNode = objectMapper.createObjectNode();
                cameraNode.put("id", camera.getId());
                cameraNode.put("name", camera.getName());
                cameraNode.put("ip", camera.getIp());
                cameraNode.put("parking", camera.getGate().getParking().getName());
                cameraNode.put("permanentlyOpen", barrierOpenCameraIds.contains(camera.getId()));
                cameras.add(cameraNode);
            }
        }

        return cameras;
    }

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
