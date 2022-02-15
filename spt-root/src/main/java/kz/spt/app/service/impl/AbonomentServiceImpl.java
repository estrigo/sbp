package kz.spt.app.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.service.AbonomentService;
import kz.spt.lib.service.PluginService;
import org.springframework.stereotype.Service;

import static kz.spt.lib.utils.StaticValues.abonomentPlugin;

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
}
