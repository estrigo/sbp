package kz.spt.abonomentplugin.extension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.abonomentplugin.AbonomentPlugin;
import kz.spt.abonomentplugin.model.Abonement;
import kz.spt.abonomentplugin.model.AbonomentTypes;
import kz.spt.abonomentplugin.service.AbonomentPluginService;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.service.LanguagePropertiesService;
import kz.spt.lib.utils.MessageKey;
import kz.spt.lib.utils.StaticValues;
import lombok.extern.java.Log;
import org.pf4j.Extension;

import java.text.SimpleDateFormat;
import java.util.Date;

@Log
@Extension
public class CommandExecutor implements PluginRegister {

    private AbonomentPluginService abonomentPluginService;
    private LanguagePropertiesService languagePropertiesService;

    @Override
    public JsonNode execute(JsonNode jsonCommand) throws Exception {

        SimpleDateFormat format = new SimpleDateFormat(StaticValues.dateFormatTZ);

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode node = objectMapper.createObjectNode();
        node.put("result", false);

        if(jsonCommand.has("command")){
            String command = jsonCommand.get("command").textValue();

            if("createType".equals(command)){
                int period = jsonCommand.get("period").intValue();
                int price = jsonCommand.get("price").intValue();
                String customJson = jsonCommand.get("customJson").textValue();
                String type = jsonCommand.get("type").textValue();
                String createdUser = jsonCommand.get("createdUser").textValue();
                AbonomentTypes abonomentTypes = getAbonomentPluginService().createType(period, customJson, type, price, createdUser);
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
                Boolean checked = jsonCommand.get("checked").booleanValue();
                String createdUser = jsonCommand.get("createdUser").textValue();
                if(getAbonomentPluginService().checkAbonomentIntersection(platenumber, parkingId, typeId, dateStart, checked)){
                    node.put("result", false);
                    node.put("error", getLanguagePropertiesService().getMessageFromProperties(MessageKey.ABONNEMENT_ERROR_DATES_OVERLAP_PLATENUMBER));
                } else {
                    Abonement abonement = getAbonomentPluginService().createAbonoment(platenumber, parkingId, typeId, dateStart, checked, createdUser);
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
            }else if("hasPaidNotExpiredAbonoment".equals(command)){
                String plateNumber = jsonCommand.get("plateNumber").textValue();
                JsonNode paidNotExpiredAbonoment = getAbonomentPluginService().getPaidNotExpiredAbonoment(plateNumber);
                node.set("paidNotExpiredAbonoment", paidNotExpiredAbonoment);
            } else if("setAbonomentPaid".equals(command)){
                Long id = jsonCommand.get("id").longValue();
                getAbonomentPluginService().setAbonomentPaid(id);
            } else if("getSatisfiedAbonomentDetails".equals(command)){
                Long parkingId = jsonCommand.get("parkingId").longValue();
                String plateNumber = jsonCommand.get("plateNumber").textValue();
                Date carInDate = format.parse(jsonCommand.get("carInDate").textValue());
                JsonNode result = getAbonomentPluginService().getPaidNotExpiredAbonoment(plateNumber, parkingId, carInDate);
                node.set("abonementsDetails", result);
            } else if ("deleteParkingAbonoments".equals(command)) {
                Long parkingId = jsonCommand.get("parkingId").longValue();
                getAbonomentPluginService().deleteAbonomentByParkingID(parkingId);
//                getWhitelistService().deleteAllByParkingId(parkingId);
                node.put("reply: ", "deleted whitelist lists");
            } else if ("removeNotPaid".equals(command)) {
                getAbonomentPluginService().deleteNotPaidExpired();
            } else if ("renewAbonement".equals(command)) {
                getAbonomentPluginService().creteNewForOld();
            } else if ("checkExpiration".equals(command)) {
                Long parkingId = jsonCommand.get("parkingId").longValue();
                String plateNumber = jsonCommand.get("plateNumber").textValue();
                String result = getAbonomentPluginService().checkExpiration(plateNumber, parkingId);
                node.put("expirationResult", result);
            }
            else {
                throw new RuntimeException("Abonement plugin: unknown command");
            }
        } else {
            throw new RuntimeException("Abonement plugin: command not provided");
        }
        return node;
    }

    private AbonomentPluginService getAbonomentPluginService() {
        if(abonomentPluginService == null) {
            abonomentPluginService = (AbonomentPluginService) AbonomentPlugin.INSTANCE.getApplicationContext().getBean("abonomentPluginServiceImpl");
        }
        return abonomentPluginService;
    }

    private LanguagePropertiesService getLanguagePropertiesService() {
        if (languagePropertiesService == null) {
            languagePropertiesService = (LanguagePropertiesService) AbonomentPlugin.INSTANCE.getMainApplicationContext().getBean("languagePropertiesServiceImpl");
        }
        return languagePropertiesService;
    }
}
