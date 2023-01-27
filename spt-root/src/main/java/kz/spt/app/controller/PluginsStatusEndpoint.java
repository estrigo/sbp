package kz.spt.app.controller;

import kz.spt.app.service.SpringBootPluginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.Map;

@Endpoint(id = "plugins")
public class PluginsStatusEndpoint {

    @Autowired
    private SpringBootPluginService pluginService;

    @ReadOperation
    public Map<String, String> getPluginsStatusInfo() {
        return pluginService.getPluginsStatusInfo();
    }
}
