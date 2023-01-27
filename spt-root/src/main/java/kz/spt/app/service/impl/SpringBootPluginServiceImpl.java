package kz.spt.app.service.impl;

import kz.spt.app.service.SpringBootPluginService;
import org.laxture.sbp.SpringBootPluginManager;
import org.laxture.sbp.spring.boot.model.PluginInfo;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginRuntimeException;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class SpringBootPluginServiceImpl implements SpringBootPluginService {

    @Autowired
    private SpringBootPluginManager pluginManager;

    @Override
    public Map<String, String> getPluginsStatusInfo() {
        Map<String, String> plugins = new TreeMap<>();
        getPluginsInfo()
                .forEach(pluginInfo -> plugins.put(pluginInfo.getPluginId(), pluginInfo.getPluginState().toString()));

        return plugins;
    }

    // get full info from all plugins
    private List<PluginInfo> getPluginsInfo() {
        List<PluginWrapper> loadedPlugins = pluginManager.getPlugins();

        // loaded plugins
        List<PluginInfo> plugins = loadedPlugins.stream().map(pluginWrapper -> {
            PluginDescriptor descriptor = pluginWrapper.getDescriptor();
            PluginDescriptor latestDescriptor = null;
            try {
                latestDescriptor = pluginManager.getPluginDescriptorFinder()
                        .find(pluginWrapper.getPluginPath());
            } catch (PluginRuntimeException ignored) {
            }
            String newVersion = null;
            if (latestDescriptor != null && !descriptor.getVersion().equals(latestDescriptor.getVersion())) {
                newVersion = latestDescriptor.getVersion();
            }

            return PluginInfo.build(descriptor,
                    pluginWrapper.getPluginState(), newVersion,
                    pluginManager.getPluginStartingError(pluginWrapper.getPluginId()),
                    latestDescriptor == null);
        }).collect(Collectors.toList());

        return plugins;
    }
}
