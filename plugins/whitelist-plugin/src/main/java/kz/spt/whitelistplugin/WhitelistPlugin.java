package kz.spt.whitelistplugin;

import kz.spt.api.plugin.CustomPlugin;
import org.laxture.sbp.SpringBootPlugin;
import org.laxture.sbp.spring.boot.SpringBootstrap;
import org.pf4j.PluginWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WhitelistPlugin extends SpringBootPlugin implements CustomPlugin {

    public static WhitelistPlugin INSTANCE;

    public WhitelistPlugin(PluginWrapper wrapper) {
        super(wrapper);
        INSTANCE = this;
    }

    @Override
    protected SpringBootstrap createSpringBootstrap() {
        return new SpringBootstrap(this, WhitelistPluginApplication.class);
    }

    @Override
    public String getTemplateUrl() {
        return "whitelist";
    }

    public List<Map<String, Object>> getLinks(){
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> link = new HashMap<>();
        link.put("label", "White list");
        link.put("url", "whitelist/list");
        link.put("cssClass", "ti-file");
        link.put("role", "MANAGER");
        list.add(link);
        return list;
    }
}
