package kz.spt.api.extension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.java.Log;

import javax.persistence.AttributeConverter;
import java.io.IOException;
import java.util.Map;

@Log
public class HashMapConverter implements AttributeConverter<Map<String, Object>, String> {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, Object> properties) {

        String propertiesJson = null;
        try {
            propertiesJson = objectMapper.writeValueAsString(properties);
        } catch (final JsonProcessingException e) {
            log.warning("JSON writing error: " + e.getMessage());
        }

        return propertiesJson;
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String propertiesJson) {

        Map<String, Object> properties = null;
        try {
            properties = objectMapper.readValue(propertiesJson, Map.class);
        } catch (final IOException e) {
            log.warning("JSON writing error: " + e.getMessage());
        }
        return properties;
    }
}
