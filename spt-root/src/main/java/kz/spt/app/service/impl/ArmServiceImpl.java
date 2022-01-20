package kz.spt.app.service.impl;

import kz.spt.app.component.HttpRequestFactoryDigestAuth;
import kz.spt.app.job.StatusCheckJob;
import kz.spt.lib.model.Camera;
import kz.spt.lib.model.CurrentUser;
import kz.spt.lib.model.Gate;
import kz.spt.lib.service.CarEventService;
import kz.spt.lib.service.EventLogService;
import kz.spt.lib.service.ArmService;
import kz.spt.app.service.BarrierService;
import kz.spt.app.service.CameraService;
import lombok.SneakyThrows;
import lombok.val;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ArmServiceImpl implements ArmService {

    private CameraService cameraService;
    private BarrierService barrierService;
    private EventLogService eventLogService;
    private String dateFormat = "yyyy-MM-dd'T'HH:mm";
    private CarEventService carEventService;

    public ArmServiceImpl(CameraService cameraService, BarrierService barrierService, EventLogService eventLogService,
                          CarEventService carEventService) {
        this.cameraService = cameraService;
        this.barrierService = barrierService;
        this.eventLogService = eventLogService;
        this.carEventService = carEventService;
    }

    @Override
    public Boolean openGate(Long cameraId) throws IOException, ParseException, InterruptedException {

        Camera camera = cameraService.getCameraById(cameraId);
        if (camera != null && camera.getGate() != null && camera.getGate().getBarrier() != null) {
            SimpleDateFormat format = new SimpleDateFormat(dateFormat);
            Map<String, Object> properties = new HashMap<>();
            properties.put("eventTime", format.format(new Date()));
            properties.put("cameraIp", camera.getIp());
            properties.put("gateName", camera.getGate().getName());
            properties.put("gateDescription", camera.getGate().getDescription());
            properties.put("gateType", camera.getGate().getGateType().toString());
            properties.put("type", EventLogService.EventType.Allow);

            Boolean result = barrierService.openBarrier(camera.getGate().getBarrier(), properties);
            if (result) {
                String username = "";
                if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CurrentUser) {
                    CurrentUser currentUser = (CurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                    if (currentUser != null) {
                        username = currentUser.getUsername();
                    }
                }
                eventLogService.sendSocketMessage(EventLogService.ArmEventType.CarEvent, EventLogService.EventType.Allow, camera.getId(), "", "Ручное открытие шлагбаума: Пользователь " + username + " открыл шлагбаум для " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "выезда" : "въезда/выезда")) + " " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName(), "Manual opening gate: User " + username + " opened gate for " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "enter" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName());
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, "Ручное открытие шлагбаума: Пользователь " + username + " открыл шлагбаум для " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "выезда" : "въезда/выезда")) + " " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName(), "Manual gate opening: User " + username + " opened gate for " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "enter" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName());
            }
            return result;
        }

        return false;
    }

    @Override
    public Boolean closeGate(Long cameraId) throws IOException, ParseException, InterruptedException {

        Camera camera = cameraService.getCameraById(cameraId);
        if (camera != null && camera.getGate() != null && camera.getGate().getBarrier() != null) {
            SimpleDateFormat format = new SimpleDateFormat(dateFormat);
            Map<String, Object> properties = new HashMap<>();
            properties.put("eventTime", format.format(new Date()));
            properties.put("cameraIp", camera.getIp());
            properties.put("gateName", camera.getGate().getName());
            properties.put("gateDescription", camera.getGate().getDescription());
            properties.put("gateType", camera.getGate().getGateType().toString());
            properties.put("type", EventLogService.EventType.Allow);

            Boolean result = barrierService.closeBarrier(camera.getGate().getBarrier(), properties);
            if (result) {
                String username = "";
                if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CurrentUser) {
                    CurrentUser currentUser = (CurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                    if (currentUser != null) {
                        username = currentUser.getUsername();
                    }
                }
                eventLogService.sendSocketMessage(EventLogService.ArmEventType.CarEvent, EventLogService.EventType.Allow, camera.getId(), "", "Ручное закрытие шлагбаума: Пользователь " + username + " закрыл шлагбаум для " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "выезда" : "въезда/выезда")) + " " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName(), "Manual closing gate: User " + username + " closed gate for " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "enter" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName());
                eventLogService.createEventLog(Gate.class.getSimpleName(), camera.getGate().getId(), properties, "Ручное закрытие шлагбаума: Пользователь " + username + " закрыл шлагбаум для " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "въезда" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "выезда" : "въезда/выезда")) + " " + camera.getGate().getDescription() + " парковки " + camera.getGate().getParking().getName(), "Manual closing gate: User " + username + " closed gate for " + (camera.getGate().getGateType().equals(Gate.GateType.IN) ? "enter" : (camera.getGate().getGateType().equals(Gate.GateType.OUT) ? "exit" : "enter/exit")) + " " + camera.getGate().getDescription() + " parking " + camera.getGate().getParking().getName());
            }
            return result;
        }

        return false;
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
    public Boolean passCar(Long cameraId, String platenumber) throws Exception {
        return carEventService.passCar(cameraId, platenumber);
    }

    @Override
    public void snapshot(Camera camera) throws Throwable {
        HttpHost host = new HttpHost(camera.getIp(), 8080, "http");
        CloseableHttpClient client = HttpClientBuilder.create().
                setDefaultCredentialsProvider(provider(camera))
                .useSystemProperties()
                .build();
        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpRequestFactoryDigestAuth(host, client);

        var restTemplate = new RestTemplate(requestFactory);
        val headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);

        StringBuilder url = new StringBuilder();
        url.append("http://");
        url.append(camera.getIp());
        url.append(camera.getSnapshotUrl());
        //url.append("/cgi-bin/snapshot.cgi");

        HttpEntity entity = new HttpEntity(headers);
        byte[] img = restTemplate.getForObject(url.toString(), byte[].class, entity);

        ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(img))
                .size(500, 500)
                .outputFormat("JPEG")
                .outputQuality(1)
                .toOutputStream(resultStream);

        byte[] thumbnais = resultStream.toByteArray();
        String base64 = null;
        base64 = StringUtils.newStringUtf8(Base64.encodeBase64(thumbnais, false));

        eventLogService.sendSocketMessage(EventLogService.ArmEventType.Photo, EventLogService.EventType.Success, camera.getId(), "", base64, base64);
    }

    private CredentialsProvider provider(Camera camera) {
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials =
                new UsernamePasswordCredentials(camera.getLogin(), camera.getPassword());
        provider.setCredentials(AuthScope.ANY, credentials);
        return provider;
    }
}
