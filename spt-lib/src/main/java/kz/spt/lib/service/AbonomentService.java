package kz.spt.lib.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.RequestParam;

public interface AbonomentService {

    JsonNode createAbonomentType(int period, int price) throws Exception;

    JsonNode deleteAbonomentType(Long id) throws Exception;

    JsonNode createAbonoment(String platenumber, Long parkingId, Long typeId, String dateStart) throws Exception;

    JsonNode deleteAbonoment(Long id) throws Exception;
}
