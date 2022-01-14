package kz.spt.rateplugin.extension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.rateplugin.RatePlugin;
import kz.spt.rateplugin.model.ParkingRate;
import kz.spt.rateplugin.service.RateService;
import org.pf4j.Extension;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;

@Extension
public class CommandExecutor implements PluginRegister {

    private RateService rateService;

    @Override
    public JsonNode execute(JsonNode command) throws Exception {

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode node = objectMapper.createObjectNode();
        node.put("rateResult", BigDecimal.ZERO);
        node.put("rateFreeMinutes", 0);

        if(command!=null){
            if(command.get("parkingId")!=null){
                if(command.get("inDate")!=null && command.get("outDate")!=null){
                    node.put("rateResult", getRateService().calculatePayment(command.get("parkingId").longValue(), format.parse(command.get("inDate").textValue()), format.parse(command.get("outDate").textValue()), command.get("cashlessPayment").booleanValue(), (command.has("paymentsJson") && command.get("paymentsJson")!=null ? command.get("paymentsJson").textValue() : null)));
                }
                ParkingRate parkingRate = getRateService().getByParkingId(command.get("parkingId").longValue());
                node.put("rateFreeMinutes", parkingRate.getAfterFreeMinutes());
                node.put("rateId", parkingRate.getId());
                node.put("rateName", parkingRate.getName());
            }
        }
        return node;
    }

    private RateService getRateService(){
        if(rateService == null) {
            rateService = (RateService) RatePlugin.INSTANCE.getApplicationContext().getBean("rateServiceImpl");
        }
        return rateService;
    }
}
