package kz.spt.billingplugin.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.spt.billingplugin.model.PaymentProvider;
import kz.spt.billingplugin.model.dto.OfdCheckData;
import kz.spt.billingplugin.model.dto.rekassa.AuthRequestDTO;
import kz.spt.billingplugin.model.dto.rekassa.AuthResponseDTO;
import kz.spt.billingplugin.model.dto.rekassa.RekassaCheckRequest;
import kz.spt.billingplugin.model.dto.rekassa.RekassaCheckResponse;
import kz.spt.billingplugin.model.dto.webkassa.Check;
import kz.spt.billingplugin.model.dto.webkassa.CheckResponse;
import kz.spt.billingplugin.model.dto.webkassa.ZReport;
import kz.spt.billingplugin.service.WebKassaService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
@Slf4j
@Service
@AllArgsConstructor
@Transactional(noRollbackFor = Exception.class)
public class ReKassaServiceImpl implements WebKassaService {

    @Value("${rekassa.host}")
    private String rekassaHost;

    public ReKassaServiceImpl() {
    }

    @Override
    public OfdCheckData registerCheck(Object check, PaymentProvider provider) {
        RekassaCheckRequest rekassaCheckRequest = (RekassaCheckRequest) check;
        AuthResponseDTO authResponseDTO = getToken(provider.getWebKassaLogin(), provider.getWebKassaID(), provider.getWebKassaPassword());
        OfdCheckData ofdCheckData = new OfdCheckData();
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.setBearerAuth(authResponseDTO.getToken());
            ObjectMapper mapper = new ObjectMapper();
            String requestBody = mapper.writeValueAsString(rekassaCheckRequest);
            HttpEntity<String> request = new HttpEntity(requestBody, headers);
            RekassaCheckResponse checkResponse =
                    restTemplate.postForObject(rekassaHost + "/crs/"+authResponseDTO.getId()+"/tickets", request, RekassaCheckResponse.class);
            if (checkResponse != null && checkResponse.getTicketNumber() != null) {
                ofdCheckData = new OfdCheckData();
                ofdCheckData.setCheckNumber(checkResponse.getTicketNumber());
                ofdCheckData.setCheckUrl(checkResponse.getQrCode());
            }
            return ofdCheckData;
        } catch (Exception ex) {

        }
        return ofdCheckData;
    }

    @Override
    public String closeOperationDay(ZReport zReport, PaymentProvider authRequestDTO) {
        return null;
    }

    private AuthResponseDTO getToken(String apiKey, String number, String password) {
        AuthResponseDTO authResponseDTO = null;
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));


            AuthRequestDTO authRequestDTO = new AuthRequestDTO();
            authRequestDTO.setNumber(number);
            authRequestDTO.setPassword(password);

            ObjectMapper mapper = new ObjectMapper();
            String requestBody = mapper.writeValueAsString(authRequestDTO);
            HttpEntity<String> request = new HttpEntity(requestBody, headers);
            String authResponse =
                    restTemplate.postForObject(rekassaHost+"/auth/login?format=json&apiKey="+apiKey, request, String.class);
            JsonNode node = mapper.readTree(authResponse);
            if (node.has("token")) {
                authResponseDTO = new AuthResponseDTO();
                authResponseDTO.setToken(node.get("token").textValue());
                authResponseDTO.setId(node.get("id").intValue());

            }
        } catch (Exception ex) {
            log.error(ex.getMessage(),ex);
        }
        return authResponseDTO;

    }
}
