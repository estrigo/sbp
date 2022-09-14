package kz.spt.app.service.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kz.spt.app.model.dto.ReportDataSet;
import kz.spt.app.model.dto.GenericResponse;
import kz.spt.app.model.dto.WhlResponse;
import kz.spt.app.repository.PropertyRepository;
import kz.spt.app.service.WhitelistRootService;
import kz.spt.lib.model.Property;
import kz.spt.lib.model.dto.adminPlace.*;

import kz.spt.lib.model.dto.adminPlace.enums.WhlProcessEnum;
import kz.spt.lib.service.AdminService;
import kz.spt.lib.service.GitInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Log
public class AdminServiceImpl implements AdminService {

    private final PropertyRepository propertyRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final GitInfoService gitInfoService;

    private final WhitelistRootService whitelistRootService;
    @Value("${restAdmin.login}")
    private String login;
    @Value("${restAdmin.password}")
    private String password;
    private final static String ENCODE = "UTF8";
    private final static String KEY_UID = "uid";
    private final static String KEY_HOST = "admin.host";
    private final static String URL = "%s%s";
    private final static String WHL_ADD = "/rest/whitelist/add";
    private final static String WHL_DEL = "/rest/whitelist/deleted";
    private final static String WHL_G_ADD = "/rest/whitelist/group/add";
    private final static String WHL_G_DEL = "/rest/whitelist/group/deleted";
    private final static String WHL_SPECIAL = "rest/whitelist/special";
    private final static String REPORT = "/rest/report/%s/%s";

    @Override
    public String getProperty(String key) {
        Optional<Property> property = propertyRepository.findFirstByKey(key);
        if (property.isPresent()) {
            return property.get().getValue();
        }
        return StringUtils.EMPTY;
    }

    @Override
    public ResponseEntity<?> updateProperty(AdminCommandDto commandDto) {
        for (Prop prop : commandDto.getProps()) {
            Optional<Property> property = propertyRepository.findFirstByKey(prop.getPropKey());
            if (property.isPresent()) {
                property.get().setValue(prop.getPropValue());
            } else {
                property = Optional.of(new Property());
                property.get().setKey(prop.getPropKey());
                property.get().setValue(prop.getPropValue());
            }
            propertyRepository.save(property.get());
        }
        return getBasicResponse();

    }


    public ResponseEntity<?> getBasicResponse() {
        Optional<Property> property = propertyRepository.findFirstByKey(KEY_UID);
        return property.map(value -> new ResponseEntity<>(
                new GenericResponse<>(value.getValue(), gitInfoService.gitInfo()), HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(
                new GenericResponse<>(), HttpStatus.OK));
    }

    @Override
    public void whlProcess(GenericWhlEvent<?> whlEvent) {
        if (whlEvent.getObject() instanceof WhiteListEvent) {
            whlProcess((WhiteListEvent) whlEvent.getObject(), whlEvent.getProcess());
        }
        if (whlEvent.getObject() instanceof WhiteListGroupEvent) {
            whlProcess((WhiteListGroupEvent) whlEvent.getObject(), whlEvent.getProcess());
        }
    }


    public void whlProcess(WhiteListEvent whiteListEvent, WhlProcessEnum processEnum) {
        switch (processEnum) {
            case CREATE:
                uploadWhiteList(whiteListEvent);
                break;
            case DELETE:
                deletedWhiteList(whiteListEvent);
                break;
            case UPDATE:
            default:
        }

    }

    public void whlProcess(WhiteListGroupEvent whiteListGroupEvent, WhlProcessEnum processEnum) {
        switch (processEnum) {
            case CREATE:
                uploadWhiteListGroup(whiteListGroupEvent);
                break;
            case DELETE:
                deletedWhiteListGroup(whiteListGroupEvent);
                break;
            case UPDATE:
            default:
        }

    }

    private void deletedWhiteListGroup(WhiteListGroupEvent whiteListGroupEvent) {
        String host = getProperty(KEY_HOST);
        if (!ObjectUtils.isEmpty(host)) {
            execute(String.format(URL, host, WHL_G_DEL), adminCommandDto(whiteListGroupEvent));
        }
    }

    private void uploadWhiteList(WhiteListEvent whiteListEvent) {
        String host = getProperty(KEY_HOST);
        if (!ObjectUtils.isEmpty(host)) {
            execute(String.format(URL, host, WHL_ADD), adminCommandDto(whiteListEvent));
        }
    }


    private void deletedWhiteList(WhiteListEvent whiteListEvent) {
        String host = getProperty(KEY_HOST);
        if (!ObjectUtils.isEmpty(host)) {
            execute(String.format(URL, host, WHL_DEL), adminCommandDto(whiteListEvent));
        }
    }


    @Async
    public void synchronizeWhl() throws Exception {
        String host = getProperty(KEY_HOST);
        if (!ObjectUtils.isEmpty(host)) {
            HttpEntity<?> request = null;
            try {
                request = execute(
                        String.format(URL, host, WHL_SPECIAL),
                        adminCommandDto(null), WhlResponse.class);
            } catch (Exception e) {
                log.warning("ERROR synchronizeWhl");
            }
            if (!ObjectUtils.isEmpty(request)) {
                whitelistRootService.updateWhlByGroupId((WhlResponse) request.getBody());
            }
        }
    }

    @Override
    public byte[] report(List<?> list, String reportName, String format) {
        String host = getProperty(KEY_HOST);
        if (!ObjectUtils.isEmpty(host)) {
            String uri = String.format(URL, host, String.format(REPORT, reportName, format));
            try {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(new ReportDataSet(list));
//                json = URLEncoder.encode(json, ENCODE);
                return postForEntity(uri,json, byte[].class);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public byte[] report(Object dto, String reportName, String format) {
        String host = getProperty(KEY_HOST);
        if (!ObjectUtils.isEmpty(host)) {
            String uri = String.format(URL, host, String.format(REPORT, reportName, format));
            try {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(new ReportDataSet(dto));
//                json = URLEncoder.encode(json, ENCODE);
                return postForEntity(uri,json, byte[].class);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private void uploadWhiteListGroup(WhiteListGroupEvent whiteListGroupEvent) {
        String host = getProperty(KEY_HOST);
        if (!ObjectUtils.isEmpty(host)) {
            execute(String.format(URL, host, WHL_G_ADD), adminCommandDto(whiteListGroupEvent));
        }
    }

    private AdminCommandDto adminCommandDto(Object o) {
        return new AdminCommandDto(o, gitInfoService.gitInfo(), getProperty(KEY_UID));
    }

    public <T> ResponseEntity<?> execute(String url, Object object, Class<T> clazz) {
        HttpEntity<?> request = new HttpEntity<>(object, headers());
        return restTemplate.exchange(
                url,
                HttpMethod.POST, request, clazz);
    }

    public <T> T postForEntity(String uri, Object object, Class<T> clazz) {
        HttpEntity<?> request = new HttpEntity<>(object, headers());
        return restTemplate.postForObject(uri, request, clazz);
    }

    public ResponseEntity<?> execute(String url, Object object) {
        HttpEntity<?> request = new HttpEntity<>(object, headers());
        return restTemplate.exchange(
                url,
                HttpMethod.POST, request, String.class);
    }

    private HttpHeaders headers() {
        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders createHeaders() {
        return new HttpHeaders() {{
            String auth = login + ":" + password;
            byte[] encodedAuth = Base64.encodeBase64(
                    auth.getBytes(StandardCharsets.US_ASCII));
            String authHeader = "Basic " + new String(encodedAuth);
            set("Authorization", authHeader);
        }};
    }
}
