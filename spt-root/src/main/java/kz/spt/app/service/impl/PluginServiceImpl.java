package kz.spt.app.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.model.Parking;
import kz.spt.lib.plugin.CustomPlugin;
import kz.spt.lib.service.ParkingService;
import kz.spt.lib.service.PluginService;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PluginServiceImpl implements PluginService {

    private PluginManager pluginManager;
    private ParkingService parkingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PluginServiceImpl(PluginManager pluginManager, ParkingService parkingService){
        this.pluginManager = pluginManager;
        this.parkingService = parkingService;
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

        for (PluginWrapper pluginWrapper : plugins) {
            if (pluginWrapper.getPlugin() instanceof CustomPlugin) {
                CustomPlugin plugin = (CustomPlugin) pluginWrapper.getPlugin();

                if (plugin.getLinks() != null) {
                    for (Map<String, Object> link : plugin.getLinks()) {
                        menus.add(link);
                    }
                }

            }
        }
        return menus;
    }

    @Override
    public ArrayNode getWhitelist(Long parkingId, String platenumber) throws Exception {
        PluginRegister whitelistPluginRegister = getPluginRegister("whitelist-plugin");
        if(whitelistPluginRegister != null) {
            Parking parking = parkingService.findById(parkingId);
            if (parking != null && (Parking.ParkingType.WHITELIST.equals(parking.getParkingType()) || Parking.ParkingType.WHITELIST_PAYMENT.equals(parking.getParkingType()))) {
                ObjectNode node = this.objectMapper.createObjectNode();
                JsonNode whitelistCheckResult = null;
                node.put("parkingId", parkingId);
                node.put("car_number", platenumber);
                whitelistCheckResult = whitelistPluginRegister.execute(node);
            }
        }
        return null;
    }
}
