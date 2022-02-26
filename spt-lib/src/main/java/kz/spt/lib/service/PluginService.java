package kz.spt.lib.service;


import com.fasterxml.jackson.databind.node.ArrayNode;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.model.Parking;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PluginService {

    PluginRegister getPluginRegister(String pluginId);

    List<Map<String, Object>> getTemplateMenus();

    ArrayNode getWhitelist(Parking parking, String platenumber) throws Exception;

    BigDecimal checkBalance(String platenumber) throws Exception;

    BigDecimal changeBalance(String platenumber, BigDecimal value) throws Exception;
}
