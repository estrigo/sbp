package kz.spt.lib.plugin;

import java.util.List;
import java.util.Map;

public interface CustomPlugin {

    String getTemplateUrl();

    List<Map<String, Object>> getLinks();
}
