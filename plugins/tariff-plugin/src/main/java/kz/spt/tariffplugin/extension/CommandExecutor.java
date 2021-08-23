package kz.spt.tariffplugin.extension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.api.extension.PluginRegister;
import kz.spt.tariffplugin.TariffPlugin;
import kz.spt.tariffplugin.service.TariffService;
import org.pf4j.Extension;

import java.text.SimpleDateFormat;

@Extension
public class CommandExecutor implements PluginRegister {

    private TariffService tariffService;

    @Override
    public JsonNode execute(JsonNode command) throws Exception {

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode node = objectMapper.createObjectNode();
        node.put("tariffResult", -1);

        if(command!=null && command.get("parkingId")!=null && command.get("inDate")!=null && command.get("outDate")!=null){
            node.put("tariffResult", getTariffService().calculatePayment(command.get("parkingId").longValue(), format.parse(command.get("inDate").textValue()), format.parse(command.get("outDate").textValue())));
        }

        return node;
    }

    private TariffService getTariffService(){
        if(tariffService == null) {
            tariffService = (TariffService) TariffPlugin.INSTANCE.getApplicationContext().getBean("tariffServiceImpl");
        }
        return tariffService;
    }
}
