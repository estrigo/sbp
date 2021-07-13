package kz.spt.app.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import kz.spt.app.plugin.PluginWrapperSerializer;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/plugins")
public class PluginController {

    @Autowired @Lazy
    private PluginManager pluginManager;

    @GetMapping(value = "/all")
    public ArrayNode allPlugins() {
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(PluginWrapper.class, new PluginWrapperSerializer());
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(simpleModule);
        return objectMapper.convertValue(pluginManager.getPlugins(), ArrayNode.class);
    }
}
