package kz.spt.zerotouchplugin.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.zerotouchplugin.model.ZeroTouchLog;
import kz.spt.zerotouchplugin.repository.ZeroTouchLogRepository;
import kz.spt.zerotouchplugin.service.ZerotouchService;
import lombok.extern.java.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

@Log
@Service
@Transactional
public class ZerotouchServiceImpl implements ZerotouchService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ZeroTouchLogRepository zeroTouchLogRepository;

    @Value("${zero.touch.rahmet.tokenUrl}")
    private String zeroTouchRahmetTokenUrl;

    @Value("${zero.touch.rahmet.clientId}")
    private String zeroTouchRahmetClientId;

    @Value("${zero.touch.rahmet.filialId}")
    private String zeroTouchRahmetFilialId;

    @Value("${zero.touch.rahmet.clientPassword}")
    private String zeroTouchRahmetClientPassword;

    @Value("${zero.touch.rahmet.postUrl}")
    private String zeroTouchRahmetPostUrl;

    @Value("${zero.touch.rahmet.check}")
    private Boolean zeroTouchRahmetCheck;

    private static String zeroTouchToken;

    public ZerotouchServiceImpl(ZeroTouchLogRepository zeroTouchLogRepository){
        this.zeroTouchLogRepository  = zeroTouchLogRepository;
    }

    @Override
    public Boolean checkZeroTouchValid(String plateNumber, BigDecimal rate, Long carStateId) throws IOException, URISyntaxException {

        Boolean result = false;

        if(zeroTouchRahmetCheck) {
            ZeroTouchLog zeroTouchLog = zeroTouchLogRepository.findZeroTouchLogByCarStateId(carStateId);

            if (zeroTouchLog != null && zeroTouchLog.getIsPaid())
                return true;
            else if (zeroTouchLog!=null && !zeroTouchLog.getIsPaid())
                return false;

            getToken();

            CloseableHttpClient zeroTouchHttpClient = HttpClients.custom().setConnectionTimeToLive(5, TimeUnit.SECONDS).build();

            ObjectNode zeroTouchPostNode = objectMapper.createObjectNode();
            zeroTouchPostNode.put("filialId", zeroTouchRahmetFilialId);
            zeroTouchPostNode.put("vehicleNumber", plateNumber);
            zeroTouchPostNode.put("amount", rate.longValue());
            //{"filialId":"582", "vehicleNumber":"TEST114TE", "amount": 200}

            StringEntity zeroTouchPostData = new StringEntity(zeroTouchPostNode.toString(), ContentType.APPLICATION_JSON);

            log.info("rahmet zero touch post request: " + zeroTouchPostNode);

            HttpPost zeroTouchPost = new HttpPost(new URI(zeroTouchRahmetPostUrl));

            zeroTouchPost.setEntity(zeroTouchPostData);
            zeroTouchPost.setHeader("Authorization", "Bearer " + zeroTouchToken);

            HttpResponse contentZeroTouchPostResponse = zeroTouchHttpClient.execute(zeroTouchPost);

            HttpEntity entity = contentZeroTouchPostResponse.getEntity();
            String zeroTouchPostResponseBodyString = EntityUtils.toString(entity, "UTF-8");
            EntityUtils.consume(contentZeroTouchPostResponse.getEntity());

            log.info("rahmet zero touch post response: " + zeroTouchPostResponseBodyString);

            JsonNode zeroTouchResponseNode = objectMapper.readTree(zeroTouchPostResponseBodyString);
            //{"error_code": 0, "status": "success", "message": "Оплата успешно проведена", "data": {"isPaid": true, "txtID": 86}}

            if (zeroTouchResponseNode.has("error_code") && 0 == zeroTouchResponseNode.get("error_code").intValue() && zeroTouchResponseNode.has("data")
                    && zeroTouchResponseNode.get("data").has("isPaid") && zeroTouchResponseNode.get("data").get("isPaid").booleanValue()) {
                result = true;
            }

            ZeroTouchLog log = new ZeroTouchLog();
            log.setCarStateId(carStateId);
            log.setIsPaid(zeroTouchResponseNode.has("data") && zeroTouchResponseNode.get("data").has("isPaid") ? zeroTouchResponseNode.get("data").get("isPaid").booleanValue() : false);
            log.setPlatenumber(plateNumber);
            log.setProvider("rahmet");
            log.setRate(rate);
            log.setRequest(zeroTouchPostNode.toString());
            log.setResonse(zeroTouchPostResponseBodyString);
            log.setTxtID(zeroTouchResponseNode.has("data") && zeroTouchResponseNode.get("data").has("txtID") ? zeroTouchResponseNode.get("data").get("txtID").longValue() : null);
            zeroTouchLogRepository.save(log);

            zeroTouchHttpClient.close();
        }
        return result;
    }

    private void getToken() throws IOException {
        CloseableHttpClient tokenHttpClient = HttpClients.custom().setConnectionTimeToLive(5, TimeUnit.SECONDS).build();

        ObjectNode tokenPostNode = objectMapper.createObjectNode();
        tokenPostNode.put("grant_type", "client_credentials");
        tokenPostNode.put("client_id", zeroTouchRahmetClientId); // 35000006
        tokenPostNode.put("client_secret", zeroTouchRahmetClientPassword); // f5ae4d63b60d9845f95a69bc4240806810338b940a2d0ee8211270a303be66e1

        StringEntity tokenPostData = new StringEntity(tokenPostNode.toString(), ContentType.APPLICATION_JSON);

        HttpPost tokenPost = new HttpPost(zeroTouchRahmetTokenUrl);
        tokenPost.addHeader("Content-Type", "application/json;charset=UTF-8");
        tokenPost.setEntity(tokenPostData);

        HttpResponse tokenHttpResponse = tokenHttpClient.execute(tokenPost);

        HttpEntity entity = tokenHttpResponse.getEntity();
        String tokenPostResponseBodyString = EntityUtils.toString(entity);
        EntityUtils.consume(tokenHttpResponse.getEntity());

        log.info("rahmet token post response: " + tokenPostResponseBodyString);

        JsonNode tokenResponseJson = objectMapper.readTree(tokenPostResponseBodyString);
        if(tokenResponseJson.has("data") && tokenResponseJson.get("data").has("token")){
            zeroTouchToken = tokenResponseJson.get("data").get("token").textValue();
            log.info("zeroTouch new token: " + zeroTouchToken);
        }

        tokenHttpClient.close();
    }
}
