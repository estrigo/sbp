package kz.spt.megaplugin;

import kz.spt.lib.plugin.CustomPlugin;
import org.laxture.sbp.SpringBootPlugin;
import org.laxture.sbp.spring.boot.SharedDataSourceSpringBootstrap;
import org.laxture.sbp.spring.boot.SpringBootstrap;
import org.pf4j.PluginWrapper;

import java.util.*;

public class MegaPlugin extends SpringBootPlugin implements CustomPlugin {

    public static MegaPlugin INSTANCE;

    public MegaPlugin(PluginWrapper wrapper) {
        super(wrapper);
        INSTANCE = this;
    }

    @Override
    protected SpringBootstrap createSpringBootstrap() {
        return new SharedDataSourceSpringBootstrap(this, MegaPluginApplication.class);
    }


    @Override
    public String getTemplateUrl() {
        return "mega";
    }

    public List<Map<String, Object>> getLinks(){
//        Locale locale = LocaleContextHolder.getLocale();
//        String language = "en";
//        if (locale.toString().equals("ru")) {
//            language = "ru-RU";
//        }
//
//        ResourceBundle bundle = ResourceBundle.getBundle("mega-plugin", Locale.forLanguageTag(language));
//        List<Map<String, Object>> list = new ArrayList<>();
//        Map<String, Object> mainMenu = new HashMap<>();
//        mainMenu.put("label", bundle.getString("mega.title"));
//        mainMenu.put("url", "mega/list");
//        mainMenu.put("cssClass", "ti-file");
//        mainMenu.put("role", "ADMIN");
//        list.add(mainMenu);
//
//        return list;
        return null;


    }
}