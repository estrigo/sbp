package kz.spt.reportplugin;

import kz.spt.lib.plugin.CustomPlugin;
import org.laxture.sbp.SpringBootPlugin;
import org.laxture.sbp.spring.boot.SpringBootstrap;
import org.pf4j.PluginWrapper;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.*;

public class ReportPlugin extends SpringBootPlugin implements CustomPlugin {
    public static ReportPlugin INSTANCE;

    public ReportPlugin(PluginWrapper wrapper) {
        super(wrapper);
        INSTANCE = this;
    }

    @Override
    protected SpringBootstrap createSpringBootstrap() {
        SpringBootstrap bootstrap = new SpringBootstrap(this,ReportPluginApplication.class);
        bootstrap.importBean("paymentService");
        return bootstrap;
    }

    @Override
    public String getTemplateUrl() {
        return "report";
    }

    @Override
    public List<Map<String, Object>> getLinks() {
        Locale locale = LocaleContextHolder.getLocale();
        String language = "en";
        if (locale.toString().equals("ru")) {
            language = "ru-RU";
        }

        ResourceBundle bundle = ResourceBundle.getBundle("report-plugin", Locale.forLanguageTag(language));
        List<Map<String, Object>> list = new ArrayList<>();

        Map<String, Object> mainMenu = new HashMap<>();
        mainMenu.put("label",bundle.getString("report.title"));
        mainMenu.put("cssClass", "fa fa-dashboard");

        List<Map<String, Object>> subMenus = new ArrayList<>();

        Map<String, Object> subMenu1 = new HashMap<>();
        subMenu1.put("label", bundle.getString("menu.report.journal"));
        subMenu1.put("url", "report/journal");
        subMenus.add(subMenu1);

        mainMenu.put("subMenus", subMenus);
        list.add(mainMenu);
        return list;
    }
}
