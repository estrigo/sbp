package kz.spt.lib.service;

import com.fasterxml.jackson.databind.JsonNode;
import kz.spt.lib.model.CarState;

import java.text.SimpleDateFormat;

public interface AbonomentService {

    JsonNode createAbonomentType(int period,String customJson,String type, int price) throws Exception;

    JsonNode deleteAbonomentType(Long id) throws Exception;

    JsonNode createAbonoment(String platenumber, Long parkingId, Long typeId, String dateStart, Boolean checked) throws Exception;

    JsonNode deleteAbonoment(Long id) throws Exception;

    JsonNode getAbonomentsDetails(String plateNumber, CarState carState, SimpleDateFormat format) throws Exception;
}
