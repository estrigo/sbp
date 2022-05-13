package kz.spt.megaplugin.extension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.megaplugin.MegaPlugin;
import kz.spt.megaplugin.service.ThirdPartyPaymentService;
import lombok.extern.java.Log;
import org.pf4j.Extension;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

@Extension
@Log
public class CommandExecutor implements PluginRegister {

    private ThirdPartyPaymentService thirdPartyPaymentService;

    @Override
    public JsonNode execute(JsonNode command) throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode node = objectMapper.createObjectNode();

        if (command != null && command.get("plateNumber")!=null) {
            String commandName = command.has("command") ? command.get("command").textValue() : "";
            if (commandName.equals("checkInThirdPartyPayment")) {
                String carNumber = command.get("plateNumber").textValue();
                Boolean res = getThirdPartyPaymentService().checkCarIfThirdPartyPayment(carNumber);
                node.put("paidByThirdParty", res);
            } else if (commandName.equals("sendPaymentToThPP")) {
                String carNumber = command.get("plateNumber").textValue();
                Date entryDate = format.parse(command.get("entryDate").textValue());
                Date exitDate = format.parse(command.get("exitDate").textValue());
                BigDecimal rateAmount = command.get("rateAmount").decimalValue();
                String parkingUid = command.get("parkingUid").textValue();
                thirdPartyPaymentService.saveThirdPartyPayment(carNumber, entryDate, exitDate, rateAmount, parkingUid);
            }
        }
        return node;
    }

    private ThirdPartyPaymentService getThirdPartyPaymentService () {
        if(thirdPartyPaymentService == null) {
            thirdPartyPaymentService = (ThirdPartyPaymentService) MegaPlugin.INSTANCE.
                    getApplicationContext().getBean("thirdPartyPaymentServiceImpl");
        }
        return thirdPartyPaymentService;
    }

}
