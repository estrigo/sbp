package kz.spt.zerotouchplugin.extension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.zerotouchplugin.ZerotouchPlugin;
import kz.spt.zerotouchplugin.service.ZerotouchService;
import lombok.extern.java.Log;
import org.pf4j.Extension;

@Log
@Extension
public class CommandExecutor implements PluginRegister {

    private ZerotouchService zerotouchService;

    @Override
    public JsonNode execute(JsonNode command) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode node = objectMapper.createObjectNode();
        node.put("zeroTouchResult", false);

        if(command!=null && command.has("command")){
            if("checkZeroTouch".equals(command.get("command").textValue())){
                if(command.has("platenumber") && command.get("platenumber").isTextual() && command.has("carStateId") && command.get("carStateId").isLong()){
                    Boolean result = getZerotouchService().checkZeroTouchValid(command.get("platenumber").textValue(), command.get("carStateId").longValue());
                    node.put("zeroTouchResult", result);
                }
            }
        }

        return node;
    }

    private ZerotouchService getZerotouchService(){
        if(zerotouchService == null) {
            zerotouchService = (ZerotouchService) ZerotouchPlugin.INSTANCE.getApplicationContext().getBean("zerotouchServiceImpl");
        }
        return zerotouchService;
    }
}
