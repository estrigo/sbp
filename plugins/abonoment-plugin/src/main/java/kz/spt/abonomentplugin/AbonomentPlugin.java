package kz.spt.abonomentplugin;

import kz.spt.lib.plugin.CustomPlugin;
import org.laxture.sbp.SpringBootPlugin;
import org.laxture.sbp.spring.boot.SpringBootstrap;
import org.pf4j.PluginWrapper;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.*;

public class AbonomentPlugin extends SpringBootPlugin implements CustomPlugin {

    public static AbonomentPlugin INSTANCE;

    public AbonomentPlugin(PluginWrapper wrapper) {
        super(wrapper);
        INSTANCE = this;
    }


    @Override
    protected SpringBootstrap createSpringBootstrap() {
        return new SpringBootstrap(this, AbonomentPluginStarter.class);
    }


    @Override
    public String getTemplateUrl() {
        return "abonoment";
    }

    public List<Map<String, Object>> getLinks(){
        Locale locale = LocaleContextHolder.getLocale();
        String language = locale.getLanguage();

        ResourceBundle bundle = ResourceBundle.getBundle("abonoment-plugin", Locale.forLanguageTag(language));
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> mainMenu = new HashMap<>();
        mainMenu.put("label", bundle.getString("abonoment.title"));
        mainMenu.put("url", "abonoment/list");
        mainMenu.put("cssClass", "mdi mdi-calendar-multiple-check");
        mainMenu.put("role", "MANAGER");
        list.add(mainMenu);
        return list;
    }
}