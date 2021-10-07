package kz.spt.whitelistplugin.extension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.whitelistplugin.WhitelistPlugin;
import kz.spt.whitelistplugin.service.WhitelistService;
import org.pf4j.Extension;

import java.text.SimpleDateFormat;

@Extension
public class CommandExecutor implements PluginRegister {

    private WhitelistService whitelistService;

    @Override
    public JsonNode execute(JsonNode command) throws Exception {

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode node = objectMapper.createObjectNode();

        if(command!=null && command.get("car_number")!=null && command.get("event_time")!=null){
            JsonNode result = getWhitelistService().hasAccess(command.get("car_number").textValue(), format.parse(command.get("event_time").textValue()));
            if(result != null){
                node.set("whitelistCheckResult", result);
            }
        }

        return node;
    }

    private WhitelistService getWhitelistService(){
        if(whitelistService == null) {
            whitelistService = (WhitelistService) WhitelistPlugin.INSTANCE.getApplicationContext().getBean("whitelistServiceImpl");
        }
        return whitelistService;
    }
}
