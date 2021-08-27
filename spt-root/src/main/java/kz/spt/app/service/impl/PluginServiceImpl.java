package kz.spt.app.service.impl;

import kz.spt.api.plugin.CustomPlugin;
import kz.spt.app.service.PluginService;
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

                if (plugin.getLinks() != null) {
                    for(Map<String,Object> link: plugin.getLinks()){
                        menus.add(link);
                    }
                }
            }
        }

        return menus;
    }
}
