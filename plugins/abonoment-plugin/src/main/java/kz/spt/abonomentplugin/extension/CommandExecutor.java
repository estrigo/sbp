package kz.spt.abonomentplugin.extension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.abonomentplugin.AbonomentPlugin;
import kz.spt.abonomentplugin.model.Abonoment;
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
    public JsonNode execute(JsonNode jsonCommand) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode node = objectMapper.createObjectNode();
        node.put("result", false);

        if(jsonCommand.has("command")){
            String command = jsonCommand.get("command").textValue();

            if("createType".equals(command)){
                int period = jsonCommand.get("period").intValue();
                int price = jsonCommand.get("price").intValue();
                AbonomentTypes abonomentTypes = getAbonomentPluginService().createType(period, price);
                node.put("result", true);
            } else if("deleteType".equals(command)){
                Long typeId = jsonCommand.get("id").longValue();
                getAbonomentPluginService().deleteType(typeId);
                node.put("result", true);
            } else if("createAbonoment".equals(command)){
                String platenumber = jsonCommand.get("platenumber").textValue();
                Long parkingId = jsonCommand.get("parkingId").longValue();
                Long typeId = jsonCommand.get("typeId").longValue();
                String dateStart = jsonCommand.get("dateStart").textValue();
                if(getAbonomentPluginService().checkAbonomentIntersection(platenumber, parkingId, typeId, dateStart)){
                    node.put("result", false);
                    node.put("error", "Даты Абономента пересекаются с другим на этот номер авто");
                } else {
                    Abonoment abonoment = getAbonomentPluginService().createAbonoment(platenumber, parkingId, typeId, dateStart);
                    node.put("result", true);
                }
            } else if("deleteAbonoment".equals(command)){
                Long typeId = jsonCommand.get("id").longValue();
                getAbonomentPluginService().deleteAbonoment(typeId);
                node.put("result", true);
            } else if("hasUnpaidNotExpiredAbonoment".equals(command)){
                String plateNumber = jsonCommand.get("plateNumber").textValue();
                JsonNode unpaidNotExpiredAbonoment = getAbonomentPluginService().getUnpaidNotExpiredAbonoment(plateNumber);
                node.set("unPaidNotExpiredAbonoment", unpaidNotExpiredAbonoment);
            } else if("setAbonomentPaid".equals(command)){
                Long id = jsonCommand.get("id").longValue();
                getAbonomentPluginService().setAbonomentPaid(id);
            } else if("hasPaidNotExpiredAbonoment".equals(command)){
                Long parkingId = jsonCommand.get("parkingId").longValue();
                String plateNumber = jsonCommand.get("plateNumber").textValue();
                Boolean result = getAbonomentPluginService().hasPaidNotExpiredAbonoment(plateNumber, parkingId);
                node.put("hasPaidNotExpiredAbonoment", result);
            } else if("getPaidNotExpiredAbonomentDetails".equals(command)){
                Long parkingId = jsonCommand.get("parkingId").longValue();
                String plateNumber = jsonCommand.get("plateNumber").textValue();
                JsonNode result = getAbonomentPluginService().getPaidNotExpiredAbonoment(plateNumber, parkingId);
                node.set("abonomentDetails", result);
            } else {

                throw new RuntimeException("Abonent plugin: unkown command");
            }
        } else {
            throw new RuntimeException("Abonent plugin: command not provided");
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
