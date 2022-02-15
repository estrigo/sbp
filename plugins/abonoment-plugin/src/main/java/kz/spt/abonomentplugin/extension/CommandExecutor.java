package kz.spt.abonomentplugin.extension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.abonomentplugin.AbonomentPlugin;
import kz.spt.abonomentplugin.model.AbonomentTypes;
import kz.spt.abonomentplugin.service.AbonomentPluginService;
import kz.spt.lib.extension.PluginRegister;
import lombok.extern.java.Log;
import org.pf4j.Extension;

@Log
@Extension
public class CommandExecutor implements PluginRegister {

    private AbonomentPluginService abonomentPluginService;

    @Override
    public JsonNode execute(JsonNode command) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode node = objectMapper.createObjectNode();
        node.put("result", false);

        if(command.has("command")){
            if("createType".equals(command)){
                int period = command.get("period").intValue();
                int price = command.get("price").intValue();
                AbonomentTypes abonomentTypes = getAbonomentPluginService().createType(period, price);
                node.put("result", true);
            } else if("createType".equals(command)){
                Long typeId = command.get("id").longValue();
                getAbonomentPluginService().deleteType(typeId);
                node.put("result", true);
            } else if("getTypeList".equals(command)){
                Long typeId = command.get("id").longValue();
                getAbonomentPluginService().deleteType(typeId);
                node.put("result", true);
            }

        } else {
            node.put("error", "unknownCommand");
        }

        return node;
    }

    private AbonomentPluginService getAbonomentPluginService() {
        if(abonomentPluginService == null) {
            abonomentPluginService = (AbonomentPluginService) AbonomentPlugin.INSTANCE.getApplicationContext().getBean("abonomentPluginServiceImpl");
        }
        return abonomentPluginService;
    }
}
