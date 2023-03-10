package kz.spt.prkstatusplugin;

import kz.spt.lib.plugin.CustomPlugin;
import org.laxture.sbp.SpringBootPlugin;
import org.laxture.sbp.spring.boot.SharedDataSourceSpringBootstrap;
import org.laxture.sbp.spring.boot.SpringBootstrap;
import org.pf4j.PluginWrapper;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.*;

public class PrkstatusPlugin extends SpringBootPlugin implements CustomPlugin {

    public static PrkstatusPlugin INSTANCE;

    public PrkstatusPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected SpringBootstrap createSpringBootstrap() {
        return new SharedDataSourceSpringBootstrap(this, PrkstatusPluginStarter.class);
    }


    @Override
    public String getTemplateUrl() {
        return "prkstatus";
    }

    public List<Map<String, Object>> getLinks(){
        Locale locale = LocaleContextHolder.getLocale();
        String language = locale.getLanguage();

        ResourceBundle bundle = ResourceBundle.getBundle("parkomat-status-plugin", Locale.forLanguageTag(language));
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
        log.put("url", "prkstatus/log/list");
        log.put("role", "MANAGER");
        subMenus.add(log);

        Map<String, Object> update = new HashMap<>();
        update.put("label", bundle.getString("parkomat.update.title"));
        update.put("url", "prkstatus/update/list");
        update.put("role", "MANAGER");
        subMenus.add(update);

        mainMenu.put("subMenus", subMenus);

        list.add(mainMenu);

        return list;


    }
}