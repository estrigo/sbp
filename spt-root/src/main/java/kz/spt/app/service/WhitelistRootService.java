package kz.spt.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import kz.spt.app.model.dto.Period;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface WhitelistRootService {

    JsonNode getWhiteLists(Long parkingId, String car_number, Date event_time, SimpleDateFormat format, Map<String, Object> properties) throws Exception;

    List<Period> calculatePaymentPeriods(JsonNode whitelistJson, Date inDate, Date outDate) throws JsonProcessingException, ParseException;

    JsonNode getValidWhiteListsInPeriod(Long parkingId, String platenumber, Date inDate, Date outDate, SimpleDateFormat format) throws Exception;
}
