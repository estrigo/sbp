package kz.spt.abonomentplugin.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.abonomentplugin.AbonomentPlugin;
import kz.spt.abonomentplugin.service.RootServicesGetterService;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.service.CarsService;
import kz.spt.lib.service.ParkingService;
import kz.spt.lib.service.PluginService;
import kz.spt.lib.utils.StaticValues;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional(noRollbackFor = Exception.class)
public class RootServicesGetterServiceImpl implements RootServicesGetterService {

    private CarsService carsService;
    private ParkingService parkingService;

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
            ObjectNode billingSubtractNode = StaticValues.objectMapper.createObjectNode();
            billingSubtractNode.put("command", "decreaseCurrentBalance");
            billingSubtractNode.put("amount", value);
            billingSubtractNode.put("plateNumber", plateNumber);
            billingSubtractNode.put("reason", "Оплата абономента паркинга " + parkingName);
            billingSubtractNode.put("reasonEn", "Payment for paid permit of parking " + parkingName);
            billingSubtractNode.put("reasonLocal", "Zahlung für bezahlten Parkausweis " + parkingName);
            billingSubtractNode.put("provider", "Subscription fee");
            billingPluginRegister.execute(billingSubtractNode).get("currentBalance").decimalValue();
        }
    }
}
