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
        mainMenu.put("label", "White list");
//        mainMenu.put("url", "whitelist/list");
        mainMenu.put("cssClass", "ti-file");
        mainMenu.put("role", "MANAGER");

        List<Map<String, Object>> subMenus = new ArrayList<>();

        Map<String, Object> subMenu1 = new HashMap<>();
        subMenu1.put("label", "White list");
        subMenu1.put("url", "whitelist/list");
        subMenu1.put("role", "MANAGER");
        subMenus.add(subMenu1);

        Map<String, Object> subMenu2 = new HashMap<>();
        subMenu2.put("label", "Cars in parking");
        subMenu2.put("url", "whitelist/current-status");
        subMenu2.put("role", "MANAGER");
        subMenus.add(subMenu2);

        mainMenu.put("subMenus", subMenus);

        list.add(mainMenu);
        return list;
    }
}
