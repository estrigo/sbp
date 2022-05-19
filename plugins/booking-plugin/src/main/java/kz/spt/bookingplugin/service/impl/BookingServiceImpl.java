package kz.spt.bookingplugin.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.bookingplugin.model.BookingLog;
import kz.spt.bookingplugin.repository.BookingRepository;
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
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

@Log
@Service
public class BookingServiceImpl implements BookingService {

    private static String halaparkToken;
    private static Long lastTokenCheck;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private BookingRepository bookingRepository;
    @Value("${booking.halapark.check}")
    private Boolean bookingHalaparkCheck;
    @Value("${booking.halapark.tokenUrl}")
    private String halaparkTokenUrl;
    @Value("${booking.halapark.postUrl}")
    private String halaparkPostUrl;

    public BookingServiceImpl(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public Boolean checkBookingValid(String plateNumber, String position) throws IOException, URISyntaxException {
        boolean valid = false;
        if (bookingHalaparkCheck) {
            String desiredFormat = convertToHalaparkRequestFormat(plateNumber);
            log.info("halapark checking platenumer: " + plateNumber + "by desired format: " + desiredFormat);

            CloseableHttpClient halaparkHttpClient = HttpClients.custom().setConnectionTimeToLive(5, TimeUnit.SECONDS).build();

            getToken();

            ObjectNode halaparkPostNode = objectMapper.createObjectNode();
            halaparkPostNode.put("medium", "Plate Number"); //medium : Plate Number (As default for Parquor)
            halaparkPostNode.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
            halaparkPostNode.put("lane_id", "1");
            halaparkPostNode.put("site_id", "2010"); // Reference id for building its unique and for concord its 2010
            halaparkPostNode.put("identifier", desiredFormat); //  Plate Number (Emirate Code - Plate Code - Plate No)
            halaparkPostNode.put("position", position); // position  = 1 (Entry ), position  = 2 (Exit )

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

            BookingLog bookingLog = BookingLog.builder().build();
            bookingLog.setRequest(halaparkPostNode.toString());
            bookingLog.setResonse(halaparkPostResponseBodyString);
            bookingLog.setPlatenumber(plateNumber);
            bookingLog.setHasBooking(false);

            JsonNode halaparkResponseNode = objectMapper.readTree(halaparkPostResponseBodyString);
            //{"response":{"status":false,"message":"Token has been expired"}}
            //{"response":{"status":true,"message":"Valid Booking","result":[{"identifier":"3-S-12345"},{"identifier":"3-S-12345"}]}}
            if (halaparkResponseNode.has("response") && halaparkResponseNode.get("response") != null) {
                JsonNode responseData = halaparkResponseNode.get("response");
                if (responseData.has("status") && responseData.get("status").booleanValue()) {
                    String parqourCheckFormat = parqourCheckFormat(plateNumber);
                    if (responseData.has("result")) {
                        ArrayNode results = (ArrayNode) responseData.get("result");
                        Iterator<JsonNode> iterator = results.iterator();
                        while (iterator.hasNext()) {
                            JsonNode result = iterator.next();
                            if (result.has("identifier")) {
                                String halaparkNumber = result.get("identifier").textValue();
                                if (halaparkNumber.toUpperCase().replaceAll(" ", "").endsWith(parqourCheckFormat)) {
                                    bookingLog.setHasBooking(true);
                                    valid = true;
                                }
                            }
                        }
                    }
                }
            }
            halaparkHttpClient.close();
            bookingRepository.save(bookingLog);
        }
        return valid;
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

    private String convertToHalaparkRequestFormat(String platenumber) {
/*        if(platenumber.contains("-")){
            return platenumber;
        } else {
            String copy = platenumber;
            if(platenumber.length() < 7){
                copy = "0-" + copy.substring(0, 1) + "-" + copy.substring(1);
            } else {
                copy = "0-" + copy.substring(0, 2) + "-" + copy.substring(2);
            }
            return copy;
        }
*/
        // Temporary cod
        String copy = platenumber;
        if (platenumber.length() < 7) {
            copy = copy.substring(1);
        } else {
            copy = copy.substring(2);
        }
        return copy;
    }

    private String parqourCheckFormat(String platenumber) {
        String copy = platenumber;
        if (platenumber.length() < 7) {
            copy = copy.substring(0, 1) + "-" + copy.substring(1);
        } else {
            copy = copy.substring(0, 2) + "-" + copy.substring(2);
        }
        return copy;
    }
}
