package kz.spt.billingplugin.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.spt.billingplugin.model.dto.webkassa.AuthRequestDTO;
import kz.spt.billingplugin.model.dto.webkassa.Check;
import kz.spt.billingplugin.model.dto.webkassa.CheckResponse;
import kz.spt.billingplugin.model.dto.webkassa.ZReport;
import kz.spt.billingplugin.service.WebKassaService;
import lombok.AllArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Collections;

@Service
@AllArgsConstructor
@Slf4j
public class WebKassaServiceImpl implements WebKassaService {

    String token;

    @Value("${webkassa.host}")
    private String webkassaHost;

    private final ObjectMapper objectMapper = new ObjectMapper();

    long tokenExpireTime = 10000;

    private static String zeroTouchToken;
    private static Long lastTokenCheck;
    private static Long tokenTimeToLive;

    public WebKassaServiceImpl() {
    }

    @Override
    public CheckResponse registerCheck(Check check, AuthRequestDTO authRequestDTO) {

        try {

            check.token = getToken(authRequestDTO);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            ObjectMapper mapper = new ObjectMapper();
            String requestBody = mapper.writeValueAsString(check);
            HttpEntity<String> request = new HttpEntity(requestBody, headers);
            CheckResponse checkResponse =
                    restTemplate.postForObject(webkassaHost+"/api/Check", request, CheckResponse.class);
            return checkResponse;
        } catch (Exception ex) {
            log.error(ex.getMessage(),ex);
        }
        return null;
    }

    @Override
    public String closeOperationDay(ZReport zReport, AuthRequestDTO authRequestDTO) {
        try {

            String token = getToken(authRequestDTO);
            zReport.token = token;

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            ObjectMapper mapper = new ObjectMapper();
            String requestBody = mapper.writeValueAsString(zReport);
            HttpEntity<String> request = new HttpEntity(requestBody, headers);
            String zReportResponse =
                    restTemplate.postForObject(webkassaHost+"/api/ZReport", request, String.class);
            return zReportResponse;
        } catch (Exception ex) {
            log.error(ex.getMessage(),ex);
        }
        return null;


    }

    private String getToken(AuthRequestDTO authRequestDTO) throws IOException {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            ObjectMapper mapper = new ObjectMapper();
            String requestBody = mapper.writeValueAsString(authRequestDTO);
            HttpEntity<String> request = new HttpEntity(requestBody, headers);
            String authResponse =
                    restTemplate.postForObject(webkassaHost+"/api/Authorize", request, String.class);
            JsonNode node = mapper.readTree(authResponse);
            if (node.has("Data") && node.get("Data").has("Token")) {
                token = node.get("Data").get("Token").textValue();

            }
        } catch (Exception ex) {
            log.error(ex.getMessage(),ex);
        }
        return token;
    }
}
