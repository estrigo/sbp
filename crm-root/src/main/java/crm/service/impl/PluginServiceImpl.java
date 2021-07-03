package crm.service.impl;

import crm.plugin.CustomPlugin;
import crm.service.PluginService;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PluginServiceImpl implements PluginService {

    @Autowired
    private PluginManager pluginManager;

    public List<Map<String, Object>> getTemplateMenus(){
        List<Map<String, Object>> menus = new ArrayList<>();

        List<PluginWrapper> plugins = pluginManager.getStartedPlugins();

        for(PluginWrapper pluginWrapper: plugins) {
            if (pluginWrapper.getPlugin() instanceof CustomPlugin) {
                CustomPlugin plugin = (CustomPlugin) pluginWrapper.getPlugin();

                if (plugin.hasTemplates()) {
                    Map<String, Object> attrs = new HashMap<>();
                    attrs.put("label", plugin.getMenuLabel());
                    attrs.put("url", plugin.getMenuUrl());
                    attrs.put("cssClass", plugin.getMenuCssClass());
                    menus.add(attrs);
                }
            }
        }

        return menus;
    }
}
