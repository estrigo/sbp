package kz.spt.lib.extension;

import com.fasterxml.jackson.databind.JsonNode;
import org.pf4j.ExtensionPoint;

public interface PluginRegister extends ExtensionPoint {

    JsonNode execute(JsonNode command) throws Exception;
}
