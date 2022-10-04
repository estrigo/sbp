package kz.spt.whitelistplugin.extension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.whitelistplugin.WhitelistPlugin;
import kz.spt.whitelistplugin.model.Whitelist;
import kz.spt.whitelistplugin.model.WhitelistGroups;
import kz.spt.whitelistplugin.service.WhitelistService;
import lombok.extern.java.Log;
import org.pf4j.Extension;
import org.springframework.util.ObjectUtils;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

@Extension
@Log
public class CommandExecutor implements PluginRegister {

    private WhitelistService whitelistService;

    @Override
    public JsonNode execute(JsonNode command) throws Exception {

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode node = objectMapper.createObjectNode();

        if (command != null && command.get("parkingId") != null && command.get("car_number") != null) {
            String commandName = command.has("command") ? command.get("command").textValue() : "";
            log.info("commandName: " + commandName);
            if (command.get("event_time") != null) {
                JsonNode result = getWhitelistService().hasAccess(command.get("parkingId").longValue(), command.get("car_number").textValue(), format.parse(command.get("event_time").textValue()));
                if (result != null) {
                    node.set("whitelistCheckResult", result);
                }
            } else if (commandName.equals("deleteWhitelists")) {
                Long parkingId = command.get("parkingId").longValue();
                getWhitelistService().deleteAllByParkingId(parkingId);
                node.put("reply: ", "deleted whitelist lists");
            } else if (commandName.equals("getValidWhiteListsInPeriod")) {
                Long parkingId = command.get("parkingId").longValue();
                String platenumber = command.get("car_number").textValue();
                String inDate = command.get("inDate").textValue();
                String outDate = command.get("outDate").textValue();
                JsonNode result = getWhitelistService().getValidWhiteListsInPeriod(parkingId, platenumber, format.parse(inDate), format.parse(outDate));
                node.set("validWhiteListsInPeriodResult", result);
            } else {
                JsonNode result = getWhitelistService().getList(command.get("parkingId").longValue(), command.get("car_number").textValue());
                if (result != null) {
                    node.set("whitelistListResult", result);
                }
            }
        }

        if (command != null) {
            String commandName = command.has("command") ? command.get("command").textValue() : "";
            if (commandName.equals("updateSpecialWhl")) {
                if (command.has("specialWhlGroup") && command.has("specialWhl")) {
                    Long specialWhlGroup = command.get("specialWhlGroup").asLong();
                    String specialWhl = command.get("specialWhl").asText();

                    List<Whitelist> whitelists = getWhitelistService().listByGroupId(specialWhlGroup);
                    Whitelist whl = null;
                    for (Whitelist whitelist : whitelists) {
                        if (whitelist.getCar().getPlatenumber().equals(specialWhl)) {
                            whl = whitelist;
                        }
                    }
                    if (ObjectUtils.isEmpty(whl)) {
                        Optional<WhitelistGroups> whitelistGroups = getWhitelistService().findWhitelistGroups(specialWhlGroup);
                        if (whitelistGroups.isPresent()) {
                            getWhitelistService().saveWhitelistFromGroup(
                                    specialWhl,
                                    whitelistGroups.get(),
                                    "rest_admin",
                                    whitelistGroups.get().getParking());
                        }
                    }
                }
            }

        }
        return node;
    }

    private WhitelistService getWhitelistService() {
        if (whitelistService == null) {
            whitelistService = (WhitelistService) WhitelistPlugin.INSTANCE.getApplicationContext().getBean("whitelistServiceImpl");
        }
        return whitelistService;
    }
}
