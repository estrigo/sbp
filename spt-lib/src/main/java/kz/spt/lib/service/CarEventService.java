package kz.spt.lib.service;

import com.fasterxml.jackson.databind.JsonNode;
import kz.spt.lib.model.Camera;
import kz.spt.lib.model.CarState;
import kz.spt.lib.model.dto.CarEventDto;
import kz.spt.lib.model.dto.temp.CarTempReqBodyJsonDto;
import kz.spt.lib.utils.StaticValues;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

public interface CarEventService {

    void saveCarEvent(CarEventDto eventDto) throws Exception;

    void handleTempCarEvent(MultipartFile file, String json) throws Exception;

    boolean passCar(Long cameraId, String platenumber, String snapshot) throws Exception;

    void handleRtaCarEvent(MultipartFile event_image_0, MultipartFile  event_cropped_image_0, String event_descriptor, String event_timestamp) throws Exception;

    void handleLiveStreamEvent(MultipartFile event_image_0, String event_descriptor, String event_timestamp) throws Exception;

    void handleLiveStreamEvent(byte[] event_image,String event_descriptor, String event_timestamp) throws Exception;

    void saveCarInState(CarEventDto eventDto, Camera camera, JsonNode whitelistCheckResults, Map<String, Object> properties);

    void saveCarOutState(CarEventDto eventDto, Camera camera, CarState carState, Map<String, Object> properties, BigDecimal balance, BigDecimal rateResult, BigDecimal zerotouchValue, SimpleDateFormat format, StaticValues.CarOutBy carOutBy, JsonNode abonements, JsonNode whitelists) throws Exception;

    void handleRtaCarEvent(String event_descriptor) throws Exception;

    void handleManualEnter(Long cameraId, String plateNumber);
}
