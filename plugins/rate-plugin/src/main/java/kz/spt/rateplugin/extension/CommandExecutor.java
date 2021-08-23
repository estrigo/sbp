package kz.spt.rateplugin.extension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.api.extension.PluginRegister;
import kz.spt.rateplugin.RatePlugin;
import kz.spt.rateplugin.service.RateService;
import org.pf4j.Extension;

import java.text.SimpleDateFormat;

@Extension
public class CommandExecutor implements PluginRegister {

    private RateService rateService;

    @Override
    public JsonNode execute(JsonNode command) throws Exception {

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode node = objectMapper.createObjectNode();
        node.put("rateResult", -1);

        if(command!=null && command.get("parkingId")!=null && command.get("inDate")!=null && command.get("outDate")!=null){
            node.put("rateResult", getRateService().calculatePayment(command.get("parkingId").longValue(), format.parse(command.get("inDate").textValue()), format.parse(command.get("outDate").textValue())));
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
