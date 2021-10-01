package kz.spt.billingplugin.extension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.billingplugin.BillingPlugin;
import kz.spt.billingplugin.service.PaymentProviderService;
import kz.spt.lib.extension.PluginRegister;
import lombok.extern.java.Log;
import org.pf4j.Extension;

@Log
@Extension
public class CommandExecutor implements PluginRegister {

    private PaymentProviderService paymentProviderService;

    @Override
    public JsonNode execute(JsonNode command) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode node = objectMapper.createObjectNode();

        if(command!=null && command.has("command") && "getPasswordHash".equals(command.get("command").textValue())){
            if(command.has("client_id") && command.get("client_id").isTextual()){
                String passwordHash = getPaymentProviderService().getClientPasswordHash(command.get("client_id").textValue());
                if(passwordHash != null){
                    node.put("passwordHash", passwordHash);
                }
            }
        }

        return node;
    }

    private PaymentProviderService getPaymentProviderService(){
        if(paymentProviderService == null) {
            paymentProviderService = (PaymentProviderService) BillingPlugin.INSTANCE.getApplicationContext().getBean("paymentProviderServiceImpl");
        }
        return paymentProviderService;
    }
}
