package kz.spt.app.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.service.AbonomentService;
import kz.spt.lib.service.PluginService;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;

import static kz.spt.lib.utils.StaticValues.abonomentPlugin;

@Log
@Service
public class AbonomentServiceImpl implements AbonomentService {

    private final PluginService pluginService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AbonomentServiceImpl(PluginService pluginService){
        this.pluginService = pluginService;
    }

    @Override
    public JsonNode createAbonomentType(int period, int price) throws Exception {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("result", false);

        PluginRegister abonomentPluginRegister = pluginService.getPluginRegister(abonomentPlugin);
        if(abonomentPluginRegister != null){
            ObjectNode command = objectMapper.createObjectNode();
            command.put("command", "createType");
            command.put("period", period);
            command.put("price", price);
            JsonNode abonomentResult = abonomentPluginRegister.execute(command);
            result.put("result", abonomentResult.get("result").booleanValue());
            if(abonomentResult.has("error")){
                result.put("error", abonomentResult.get("error").textValue());
            }
        } else {
            result.put("error", "abonomentPluginNotStarted");
        }
        return result;
    }

    @Override
    public JsonNode deleteAbonomentType(Long id) throws Exception {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("result", false);

        PluginRegister abonomentPluginRegister = pluginService.getPluginRegister(abonomentPlugin);
        if(abonomentPluginRegister != null){
            ObjectNode command = objectMapper.createObjectNode();
            command.put("command", "deleteType");
            command.put("id", id);
            JsonNode abonomentResult = abonomentPluginRegister.execute(command);
            result.put("result", abonomentResult.get("result").booleanValue());
            if(abonomentResult.has("error")){
                result.put("error", abonomentResult.get("error").textValue());
            }
        } else {
            result.put("error", "abonomentPluginNotStarted");
        }

        return result;
    }

    @Override
    public JsonNode createAbonoment(String platenumber, Long parkingId, Long typeId, String dateStart) throws Exception {

        ObjectNode result = objectMapper.createObjectNode();
        result.put("result", false);

        PluginRegister abonomentPluginRegister = pluginService.getPluginRegister(abonomentPlugin);
        if(abonomentPluginRegister != null){
            ObjectNode command = objectMapper.createObjectNode();
            command.put("command", "createAbonoment");
            command.put("platenumber", platenumber);
            command.put("parkingId", parkingId);
            command.put("typeId", typeId);
            command.put("dateStart", dateStart);
            JsonNode abonomentResult = abonomentPluginRegister.execute(command);
            result.put("result", abonomentResult.get("result").booleanValue());
            if(abonomentResult.has("error")){
                result.put("error", abonomentResult.get("error").textValue());
            }
        } else {
            result.put("error", "abonomentPluginNotStarted");
        }
        return result;
    }

    @Override
    public JsonNode deleteAbonoment(Long id) throws Exception {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("result", false);

        PluginRegister abonomentPluginRegister = pluginService.getPluginRegister(abonomentPlugin);
        if(abonomentPluginRegister != null){
            ObjectNode command = objectMapper.createObjectNode();
            command.put("command", "deleteAbonoment");
            command.put("id", id);
            JsonNode abonomentResult = abonomentPluginRegister.execute(command);
            result.put("result", abonomentResult.get("result").booleanValue());
            if(abonomentResult.has("error")){
                result.put("error", abonomentResult.get("error").textValue());
            }
        } else {
            result.put("error", "abonomentPluginNotStarted");
        }

        return result;
    }
}
