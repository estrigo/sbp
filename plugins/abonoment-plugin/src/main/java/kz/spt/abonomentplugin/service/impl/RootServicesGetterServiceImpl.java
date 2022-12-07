package kz.spt.abonomentplugin.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.abonomentplugin.AbonomentPlugin;
import kz.spt.abonomentplugin.service.RootServicesGetterService;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.service.*;
import kz.spt.lib.utils.Language;
import kz.spt.lib.utils.MessageKey;
import kz.spt.lib.utils.StaticValues;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional(noRollbackFor = Exception.class)
public class RootServicesGetterServiceImpl implements RootServicesGetterService {

    private CarsService carsService;
    private ParkingService parkingService;
    private LanguagePropertiesService languagePropertiesService;

    private PluginService pluginService;

    @Override
    public CarsService getCarsService() {
        if (this.carsService == null) {
            carsService = (CarsService) AbonomentPlugin.INSTANCE.getMainApplicationContext().getBean("carsServiceImpl");
        }
        return this.carsService;
    }

    @Override
    public ParkingService getParkingService() {
        if (this.parkingService == null){
            parkingService = (ParkingService) AbonomentPlugin.INSTANCE.getMainApplicationContext().getBean("parkingServiceImpl");
        }
        return this.parkingService;
    }

    @Override
    public PluginService getPluginService(){
        if (pluginService == null) {
            pluginService = (PluginService) AbonomentPlugin.INSTANCE.getMainApplicationContext().getBean("pluginServiceImpl");
        }

        return pluginService;
    }

    @Override
    public LanguagePropertiesService getLanguageService() {
        if (languagePropertiesService == null) {
            languagePropertiesService = (LanguagePropertiesService) AbonomentPlugin.INSTANCE.getMainApplicationContext().getBean("languagePropertiesServiceImpl");
        }

        return languagePropertiesService;
    }

    @Override
    public  BigDecimal getBalance(String plateNumber) throws Exception {
        PluginRegister billingPluginRegister = getPluginService().getPluginRegister(StaticValues.billingPlugin);

        JsonNode currentBalanceResult = null;
        if (billingPluginRegister != null) {
            ObjectNode node = StaticValues.objectMapper.createObjectNode();
            node.put("command", "getCurrentBalance");
            node.put("plateNumber", plateNumber);
            currentBalanceResult = billingPluginRegister.execute(node);
        }
        if (currentBalanceResult != null && currentBalanceResult.has("currentBalance")) {
            return currentBalanceResult.get("currentBalance").decimalValue().setScale(2);
        }
        return BigDecimal.ZERO;
    }



    @Override
    public void decreaseBalance(String plateNumber, BigDecimal value, String parkingName) throws Exception {
        PluginRegister billingPluginRegister = getPluginService().getPluginRegister(StaticValues.billingPlugin);
        if (billingPluginRegister != null) {
            Map<String, Object> messageValues = new HashMap<>();
            messageValues.put("parking", parkingName);

            Map<String, String> messages = languagePropertiesService.getWithDifferentLanguages(MessageKey.BILLING_REASON_PAYMENT_PAID_PERMIT, messageValues);

            ObjectNode billingSubtractNode = StaticValues.objectMapper.createObjectNode();
            billingSubtractNode.put("command", "decreaseCurrentBalance");
            billingSubtractNode.put("amount", value);
            billingSubtractNode.put("plateNumber", plateNumber);
            billingSubtractNode.put("reason", messages.get(Language.RU));
            billingSubtractNode.put("reasonEn", messages.get(Language.EN));
            billingSubtractNode.put("reasonLocal", messages.get(Language.LOCAL));
            billingSubtractNode.put("provider", "Subscription fee");
            billingPluginRegister.execute(billingSubtractNode).get("currentBalance").decimalValue();
        }
    }
}
