package kz.spt.app.service;


import kz.spt.lib.extension.PluginRegister;

import java.util.List;
import java.util.Map;

public interface PluginService {

    PluginRegister getPluginRegister(String pluginId);

    List<Map<String, Object>> getTemplateMenus();
}
