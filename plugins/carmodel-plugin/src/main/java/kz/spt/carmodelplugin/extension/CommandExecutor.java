package kz.spt.carmodelplugin.extension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.lib.extension.PluginRegister;
import org.pf4j.Extension;

import java.text.SimpleDateFormat;

@Extension
public class CommandExecutor implements PluginRegister {

    @Override
    public JsonNode execute(JsonNode command) throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode node = objectMapper.createObjectNode();

        return null;
    }
}