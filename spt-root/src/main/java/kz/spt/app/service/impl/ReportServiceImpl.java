package kz.spt.app.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.app.service.ReportService;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.service.PluginService;
import kz.spt.lib.utils.StaticValues;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
@Transactional
public class ReportServiceImpl implements ReportService {

    private final PluginService pluginService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReportServiceImpl(PluginService pluginService){
        this.pluginService = pluginService;
    }

    @Override
    public List<String> getPaymentProviders() throws Exception {
        PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
        List<String> providerNames = new ArrayList<>();
        if(billingPluginRegister != null){
            ObjectNode node = this.objectMapper.createObjectNode();
            node.put("command", "getProviderNames");

            JsonNode result = billingPluginRegister.execute(node);
            if(result.has("providerNames") && result.get("providerNames") != null){
                ArrayNode providerNamesArray = (ArrayNode) result.get("providerNames");
                Iterator<JsonNode> iterator = providerNamesArray.iterator();
                while (iterator.hasNext()){
                    String providerName = iterator.next().asText();
                    providerNames.add(providerName);
                }
            }
        }
        return providerNames;
    }
}
