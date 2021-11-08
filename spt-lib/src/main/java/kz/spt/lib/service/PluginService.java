package kz.spt.lib.service;


import com.fasterxml.jackson.databind.node.ArrayNode;
import kz.spt.lib.extension.PluginRegister;

import java.util.List;
import java.util.Map;

public interface PluginService {

    PluginRegister getPluginRegister(String pluginId);

    List<Map<String, Object>> getTemplateMenus();

    ArrayNode getWhitelist(Long parkingId, String platenumber) throws Exception;
}
