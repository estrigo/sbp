package kz.spt.whitelistplugin;

import kz.spt.lib.plugin.CustomPlugin;
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

    @Override
    public List<Map<String, Object>> getLinks(){
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> mainMenu = new HashMap<>();
        mainMenu.put("label", "Белый лист");
        mainMenu.put("url", "whitelist/list");
        mainMenu.put("cssClass", "ti-file");
        mainMenu.put("role", "MANAGER");
        list.add(mainMenu);
        return list;
    }
}
