package kz.spt.testplugin;

import kz.spt.lib.plugin.CustomPlugin;
import org.laxture.sbp.SpringBootPlugin;
import org.laxture.sbp.spring.boot.SpringBootstrap;
import org.pf4j.PluginWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestPlugin extends SpringBootPlugin implements CustomPlugin {

    public TestPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected SpringBootstrap createSpringBootstrap() {
        return new SpringBootstrap(this, TestPluginStarter.class);
    }


    @Override
    public String getTemplateUrl() {
        return "test";
    }

    public List<Map<String, Object>> getLinks(){
/*        List<Map<String, Object>> list = new ArrayList<>();

        Map<String, Object> mainMenu = new HashMap<>();
        mainMenu.put("label", "Test menu");
        mainMenu.put("cssClass", "ti-file");
        mainMenu.put("role", "MANAGER");

        List<Map<String, Object>> subMenus = new ArrayList<>();

        Map<String, Object> subMenu1 = new HashMap<>();
        subMenu1.put("label", "Test sub menu 1");
        subMenu1.put("url", "test/list");
        subMenu1.put("role", "MANAGER");
        subMenus.add(subMenu1);

        Map<String, Object> subMenu2 = new HashMap<>();
        subMenu2.put("label", "Test sub menu 2");
        subMenu2.put("url", "test/list2");
        subMenu2.put("role", "MANAGER");
        subMenus.add(subMenu2);

        mainMenu.put("subMenus", subMenus);

        list.add(mainMenu);

        return list;*/
        return null;
    }
}