package crm.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import crm.plugin.PluginWrapperSerializer;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/plugins")
public class PluginController {

    @Autowired
    private PluginManager pluginManager;

    @Autowired
    private ApplicationContext applicationContext;

    @GetMapping(value = "/all")
    public ArrayNode allPlugins() {
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(PluginWrapper.class, new PluginWrapperSerializer());
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(simpleModule);
        return objectMapper.convertValue(pluginManager.getPlugins(), ArrayNode.class);
    }
}
