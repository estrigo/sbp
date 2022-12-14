package kz.spt.bookingplugin.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.bookingplugin.exceptions.BookingValidException;
import kz.spt.bookingplugin.model.BookingLog;
import kz.spt.bookingplugin.repository.BookingRepository;
import kz.spt.bookingplugin.service.BookingService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Transactional(noRollbackFor = Exception.class)
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
    @Value("${booking.esentai.check}")
    private Boolean esentaiCheck;
    @Value("${booking.esentai.tokenUrl}")
    private String esentaiTokenUrl;
    @Value("${booking.esentai.postUrl}")
    private String esentaiPostUrl;
    @Value("${booking.esentai.login}")
    private String login;
    @Value("${booking.esentai.password}")
    private String password;

    public BookingServiceImpl(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public Boolean checkBookingValid(String plateNumber, String region, String position, String entrance) throws BookingValidException {
        boolean valid = false;
        try {
            if (bookingHalaparkCheck) {
                String desiredFormat = convertToHalaparkRequestFormat(plateNumber, region);
                log.info("halapark checking platenumer: " + plateNumber + " by desired format: " + desiredFormat);

                CloseableHttpClient halaparkHttpClient = HttpClients.custom().setConnectionTimeToLive(5, TimeUnit.SECONDS).build();

                getToken();

                log.info("halapark token retrieve finished: " + plateNumber);

                ObjectNode halaparkPostNode = objectMapper.createObjectNode();
                halaparkPostNode.put("medium", "Plate Number"); //medium : Plate Number (As default for Parquor)
                halaparkPostNode.put("timestamp", String.valueOf(Instant.now().getEpochSecond()));
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

                log.info("halapark main request retrieve finished: " + plateNumber);
                log.info(plateNumber + " : " + halaparkPostResponseBodyString);

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
                        bookingLog.setHasBooking(true);
                        valid = true;

                    /*String parqourCheckFormat = parqourCheckFormat(plateNumber);
                    if (responseData.has("result")) {
                        ArrayNode results = (ArrayNode) responseData.get("result");
                        Iterator<JsonNode> iterator = results.iterator();
                        while (iterator.hasNext()) {
                            JsonNode result = iterator.next();
                            if (result.has("identifier")) {
                                String halaparkNumber = result.get("identifier").textValue();
                                if (halaparkNumber.toUpperCase().replaceAll(" ", "").endsWith(parqourCheckFormat)) {

                                }
                            }
                        }
                    }*/
                    }
                }
                halaparkHttpClient.close();
                bookingRepository.save(bookingLog);
            }
            else if (esentaiCheck) {
                String token = String.valueOf(getEsentaiToken());
                RestTemplate restTemplate = new RestTemplate();
                Map<String, Object> params = new HashMap<>();
                params.put("vehicle_gov_number", plateNumber);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                org.springframework.http.HttpEntity<Map<String, Object>> request =
                        new org.springframework.http.HttpEntity<>(params, headers);
                if (entrance.equals("entry")) {
                    try {
                        ResponseEntity<String> responseEntity =
                                restTemplate.postForEntity(esentaiPostUrl + "/start", request, String.class);
                        log.info("[Esentai] response for entry {}", responseEntity.getBody());
                        if (responseEntity.getBody() != null) {
                            JSONObject jsonResp = new JSONObject(responseEntity.getBody());
                            valid = (Boolean) jsonResp.get("found");
                        }
                    } catch (Exception e) {
                        log.info("[Esentai] error: {}", e.getMessage());
                    }
                } else if (entrance.equals("exit")) {
                    try {
                        ResponseEntity<String> responseEntity =
                                restTemplate.postForEntity(esentaiPostUrl + "/finish", request, String.class);
                        log.info("[Esentai] response for exit {}", responseEntity.getBody());
                        if (responseEntity.getBody() != null) {
                            JSONObject jsonResp = new JSONObject(responseEntity.getBody());
                            valid = (Boolean) jsonResp.get("allowed_to_exit");
                        }
                    } catch (Exception e) {
                        log.info("[Esentai] error: {}", e.getMessage());
                    }
                }
            }
            return valid;
        } catch (Exception e) {
            throw new BookingValidException(e.getMessage());
        }
    }

    private Object getEsentaiToken() {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> params = new HashMap<>();
        params.put("email", login);
        params.put("password", password);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        org.springframework.http.HttpEntity<Map<String, Object>> request =
                new org.springframework.http.HttpEntity<>(params, headers);
        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(esentaiTokenUrl, request, String.class);
            Object token = null;
            if (responseEntity.getBody() != null) {
                JSONObject jsonResp = new JSONObject(responseEntity.getBody());
                token = jsonResp.get("auth_token");
            }
            return token;
        } catch (Exception e) {
            log.info("[Esentai] get token error {}", e.getMessage());
            return e.getMessage();
        }
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

    private String convertToHalaparkRequestFormat(String platenumber, String region) {
        /*if (platenumber.contains("-")) {
            return platenumber;
        } else {
            String copy = platenumber;
            if (platenumber.length() < 7) {
                copy = "0-" + copy.substring(0, 1) + "-" + copy.substring(1);
            } else {
                copy = "0-" + copy.substring(0, 2) + "-" + copy.substring(2);
            }
            return copy;
        }*/

        // Temporary cod
        String copy = parqourCheckFormat(platenumber);
        String code = region != null ? convertRegion(region) : "";
        return (code.isEmpty() ? "" : code + "-") + platenumber;
    }

    private String convertRegion(String region) {
        log.info("Region: " + region);
        switch (region) {
            case "ae-az":
            case "abu-dhabi":
                return "1";
            case "ae-aj":
            case "ajman":
                return "2";
            case "ae-du":
            case "dubai":
                return "3";
            case "ae-fu":
            case "fujairah":
                return "4";
            case "ae-rk":
            case "ras-al-khaimah":
                return "5";
            case "ae-sh":
            case "sharjah":
                return "6";
            case "ae-uq":
            case "alquwain":
                return "7";
            default:
                return "";
        }
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
