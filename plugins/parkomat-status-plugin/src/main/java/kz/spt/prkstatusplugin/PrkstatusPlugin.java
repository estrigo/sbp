package kz.spt.prkstatusplugin;

import kz.spt.lib.plugin.CustomPlugin;
import org.laxture.sbp.SpringBootPlugin;
import org.laxture.sbp.spring.boot.SpringBootstrap;
import org.pf4j.PluginWrapper;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.*;

public class PrkstatusPlugin extends SpringBootPlugin implements CustomPlugin {

    public PrkstatusPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected SpringBootstrap createSpringBootstrap() {
        return new SpringBootstrap(this, PrkstatusPluginStarter.class);
    }


    @Override
    public String getTemplateUrl() {
        return "prkstatus";
    }

    public List<Map<String, Object>> getLinks(){
        Locale locale = LocaleContextHolder.getLocale();
        String language = "en";
        if (locale.toString().equals("ru")) {
            language = "ru-RU";
        }
        ResourceBundle bundle = ResourceBundle.getBundle("prkstatus-plugin", Locale.forLanguageTag(language));
        List<Map<String, Object>> list = new ArrayList<>();

        Map<String, Object> mainMenu = new HashMap<>();
        mainMenu.put("label", bundle.getString("parkomat.plugin.title"));
        mainMenu.put("cssClass", "ti-signal");

        List<Map<String, Object>> subMenus = new ArrayList<>();

        Map<String, Object> status = new HashMap<>();
        status.put("label",bundle.getString("parkomat.status.title"));
        status.put("url", "prkstatus/status/list");
        status.put("role", "MANAGER");
        subMenus.add(status);

        Map<String, Object> log = new HashMap<>();
        log.put("label", bundle.getString("parkomat.log.title"));
        log.put("url", "parkomat-monitor/log");
        log.put("role", "MANAGER");
        subMenus.add(log);

        Map<String, Object> update = new HashMap<>();
        update.put("label", bundle.getString("parkomat.update.title"));
        update.put("url", "parkomat-monitor/update");
        update.put("role", "MANAGER");
        subMenus.add(update);

        mainMenu.put("subMenus", subMenus);

        list.add(mainMenu);

        return list;


    }
}