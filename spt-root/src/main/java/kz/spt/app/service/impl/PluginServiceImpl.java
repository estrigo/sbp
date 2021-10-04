package kz.spt.app.service.impl;

import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.plugin.CustomPlugin;
import kz.spt.app.service.PluginService;
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

    @Autowired
    private PluginManager pluginManager;

    @Override
    public PluginRegister getPluginRegister(String pluginId) {
        PluginWrapper whitelistPlugin = pluginManager.getPlugin(pluginId);
        if (whitelistPlugin != null && whitelistPlugin.getPluginState().equals(PluginState.STARTED)) {
            List<PluginRegister> pluginRegisters = pluginManager.getExtensions(PluginRegister.class, whitelistPlugin.getPluginId());
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
}
