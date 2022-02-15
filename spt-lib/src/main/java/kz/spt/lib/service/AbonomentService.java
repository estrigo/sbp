package kz.spt.lib.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface AbonomentService {

    JsonNode createAbonomentType(int period, int price) throws Exception;

    JsonNode deleteAbonomentType(Long id) throws Exception;
}
