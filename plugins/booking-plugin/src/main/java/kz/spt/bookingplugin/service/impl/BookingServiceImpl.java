package kz.spt.bookingplugin.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.bookingplugin.service.BookingService;
import lombok.extern.java.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

@Log
@Service
public class BookingServiceImpl implements BookingService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${booking.halapark.check}")
    private Boolean bookingHalaparkCheck;

    @Value("${booking.halapark.tokenUrl}")
    private String halaparkTokenUrl;

    @Value("${booking.halapark.postUrl}")
    private String halaparkPostUrl;

    private static String halaparkToken;
    private static Long lastTokenCheck;

    @Override
    public Boolean checkBookingValid(String plateNumber) throws IOException, URISyntaxException {

        boolean result = false;

        if(bookingHalaparkCheck){
            CloseableHttpClient halaparkHttpClient = HttpClients.custom().setConnectionTimeToLive(5, TimeUnit.SECONDS).build();

            getToken();

            ObjectNode halaparkPostNode = objectMapper.createObjectNode();
            halaparkPostNode.put("medium","Plate Number"); //medium : Plate Number (As default for Parquor)
            halaparkPostNode.put("timestamp", System.currentTimeMillis());
            halaparkPostNode.put("lane_id","1");
            halaparkPostNode.put("site_id","2010"); // Reference id for building its unique and for concord its 2010
            halaparkPostNode.put("identifier", plateNumber); //  Plate Number (Emirate Code - Plate Code - Plate No)

            //{"identifier":"1-22-15788","site_id":"2010","lane_id":"1","medium":"Plate Number","timestamp":"1641798345"}

            StringEntity halaparkPostData = new StringEntity(halaparkPostNode.toString(), ContentType.APPLICATION_JSON);

            HttpPost halaparkPost = new HttpPost(new URI(halaparkPostUrl));
            halaparkPost.setHeader("Authtoken", "Bearer " + halaparkToken);
            halaparkPost.addHeader("Content-Type", "application/json;charset=UTF-8");
            halaparkPost.setEntity(halaparkPostData);

            HttpResponse contentHalaparkPostResponse = halaparkHttpClient.execute(halaparkPost);

            HttpEntity entity = contentHalaparkPostResponse.getEntity();
            String halaparkPostResponseBodyString = EntityUtils.toString(entity);
            EntityUtils.consume(contentHalaparkPostResponse.getEntity());

            log.info("halapark post response: " + halaparkPostResponseBodyString);

            JsonNode halaparkResponseNode = objectMapper.readTree(halaparkPostResponseBodyString);
            //{"response":{"status":false,"message":"Token has been expired"}}
            //{"response":{"status":true,"message":"Valid Booking"}}
            if(halaparkResponseNode.has("response") && halaparkResponseNode.get("response") != null){
                JsonNode responseData = halaparkResponseNode.get("response");
                if(responseData.has("status") && responseData.get("status").booleanValue()){
                    result = true;
                }
            }
            halaparkHttpClient.close();
        }
        return result;
    }

    private void getToken() throws IOException {
        CloseableHttpClient halaparkHttpClient = HttpClients.custom().setConnectionTimeToLive(5, TimeUnit.SECONDS).build();

        HttpGet tokenGet = new HttpGet(halaparkTokenUrl);
        HttpResponse halaparkTokenHttpResponse = halaparkHttpClient.execute(tokenGet);

        HttpEntity tokenEntity = halaparkTokenHttpResponse.getEntity();
        halaparkToken = EntityUtils.toString(tokenEntity);

        log.info("halapark new token: " + halaparkToken);

        EntityUtils.consume(halaparkTokenHttpResponse.getEntity());
        halaparkHttpClient.close();
        lastTokenCheck = System.currentTimeMillis();
    }
}
