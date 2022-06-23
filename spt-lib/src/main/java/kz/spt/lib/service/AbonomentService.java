package kz.spt.lib.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import kz.spt.app.model.dto.Period;
import kz.spt.lib.model.CarState;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface AbonomentService {

    JsonNode createAbonomentType(int period,String customJson,String type, int price) throws Exception;

    JsonNode deleteAbonomentType(Long id) throws Exception;

    JsonNode createAbonoment(String platenumber, Long parkingId, Long typeId, String dateStart, Boolean checked) throws Exception;

    JsonNode deleteAbonoment(Long id) throws Exception;

    JsonNode getAbonomentsDetails(String plateNumber, CarState carState, SimpleDateFormat format) throws Exception;

    JsonNode getAbonomentsDetails(String plateNumber, Long parkingId, Date date, SimpleDateFormat format) throws Exception;

    List<Period> calculatePaymentPeriods(JsonNode abonementJson, Date inDate, Date outDate) throws JsonProcessingException, ParseException;

    void checkAbonementExpireDate(String plateNumber, Long cameraId, Long parkingId, Map<String, Object> properties) throws Exception;
}
