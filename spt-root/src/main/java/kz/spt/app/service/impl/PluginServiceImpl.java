package kz.spt.app.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.model.CurrentUser;
import kz.spt.lib.model.Parking;
import kz.spt.lib.plugin.CustomPlugin;
import kz.spt.lib.service.ParkingService;
import kz.spt.lib.service.PluginService;
import kz.spt.lib.utils.StaticValues;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PluginServiceImpl implements PluginService {

    private PluginManager pluginManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PluginServiceImpl(PluginManager pluginManager){
        this.pluginManager = pluginManager;
    }

    @Override
    public PluginRegister getPluginRegister(String pluginId) {
        PluginWrapper pluginWrapper = pluginManager.getPlugin(pluginId);
        if (pluginWrapper != null && pluginWrapper.getPluginState().equals(PluginState.STARTED)) {
            List<PluginRegister> pluginRegisters = pluginManager.getExtensions(PluginRegister.class, pluginWrapper.getPluginId());
            if (pluginRegisters.size() > 0) {
                return pluginRegisters.get(0);
            }
        }
        return null;
    }

    @Override
    public List<Map<String, Object>> getTemplateMenus() {
        List<Map<String, Object>> menus = new ArrayList<>();

        List<PluginWrapper> plugins = pluginManager.getStartedPlugins();
        CurrentUser currentUser = (CurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        for (PluginWrapper pluginWrapper : plugins) {
            if (pluginWrapper.getPlugin() instanceof CustomPlugin) {
                CustomPlugin plugin = (CustomPlugin) pluginWrapper.getPlugin();
                if (plugin.getLinks() != null) {
                    for (Map<String, Object> link : plugin.getLinks()) {
                        if(currentUser.getUser().getRoles().get(0).getName().equals("ROLE_ACCOUNTANT")){
                            if (plugin.toString().substring(7,20).equals("billingplugin")) {
                                menus.add(link);
                            }
                        }
                        if (currentUser.getUser().getRoles().get(0).getName().equals("ROLE_BAQORDA")){
                            String pluginName = plugin.toString().substring(7,10);
                            if (pluginName.equals("whi")||pluginName.equals("bil")||pluginName.equals("abo")){
                                menus.add(link);
                            }
                        }
                        else {
                            menus.add(link);
                        }
                    }
                }
            }
        }
        return menus;
    }

    @Override
    public ArrayNode getWhitelist(Parking parking, String platenumber) throws Exception {
        PluginRegister whitelistPluginRegister = getPluginRegister(StaticValues.whitelistPlugin);
        if(whitelistPluginRegister != null) {
            if (parking != null && (Parking.ParkingType.WHITELIST.equals(parking.getParkingType()) || Parking.ParkingType.WHITELIST_PAYMENT.equals(parking.getParkingType()))) {
                ObjectNode node = this.objectMapper.createObjectNode();
                JsonNode whitelistCheckResult = null;
                node.put("parkingId", parking.getId());
                node.put("car_number", platenumber);
                whitelistCheckResult = whitelistPluginRegister.execute(node);
            }
        }
        return null;
    }

    @Override
    public BigDecimal checkBalance(String platenumber) throws Exception {
        PluginRegister billingPluginRegister = getPluginRegister(StaticValues.billingPlugin);
        if(billingPluginRegister != null){
            ObjectNode node = this.objectMapper.createObjectNode();
            JsonNode balanceCheckResult = null;
            node.put("command", "getCurrentBalance");
            node.put("plateNumber", platenumber);
            balanceCheckResult = billingPluginRegister.execute(node);
            return balanceCheckResult.get("currentBalance").decimalValue().setScale(2);
        }
        return null;
    }

    @Override
    public BigDecimal changeBalance(String platenumber, BigDecimal value) throws Exception {
        PluginRegister billingPluginRegister = getPluginRegister(StaticValues.billingPlugin);
        if(billingPluginRegister != null){
            ObjectNode node = this.objectMapper.createObjectNode();
            JsonNode balanceCheckResult = null;
            node.put("command", "increaseCurrentBalance");
            node.put("plateNumber", platenumber);
            node.put("amount", value);

            String username = "";
            if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CurrentUser) {
                CurrentUser currentUser = (CurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                if (currentUser != null) {
                    username = currentUser.getUsername();
                }
            }

            if(BigDecimal.ZERO.compareTo(value) == -1) {
                node.put("reason", "Ручное пополнение пользователем " + username);
                node.put("reasonEn", "Manual top up by user " + username);
            } else {
                node.put("reason", "Ручное списание пользователем " + username );
                node.put("reasonEn", "Manual write-off by user  " + username);
            }
            balanceCheckResult = billingPluginRegister.execute(node);
            return balanceCheckResult.get("currentBalance").decimalValue().setScale(2);
        }

        return null;
    }
}
