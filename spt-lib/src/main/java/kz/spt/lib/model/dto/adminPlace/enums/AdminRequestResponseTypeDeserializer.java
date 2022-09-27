package kz.spt.lib.model.dto.adminPlace.enums;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.util.stream.Stream;

public class AdminRequestResponseTypeDeserializer  extends JsonDeserializer<AdminRequestResponseType> {
    @Override
    public AdminRequestResponseType deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        String name = node.asText();
        if (ObjectUtils.isEmpty(name)){
            name = node.get("type").asText();
        }
        String finalName = name;
        return Stream.of(AdminRequestResponseType.values())
                .filter(enumValue -> enumValue.name().equals(finalName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("name "+ finalName +" is not recognized"));
    }
}
