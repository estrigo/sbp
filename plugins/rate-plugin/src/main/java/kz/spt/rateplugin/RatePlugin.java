package kz.spt.rateplugin;

import com.atomikos.jdbc.AtomikosDataSourceBean;
import kz.spt.lib.plugin.CustomPlugin;
import org.laxture.sbp.SpringBootPlugin;
import org.laxture.sbp.spring.boot.SharedJtaSpringBootstrap;
import org.laxture.sbp.spring.boot.SpringBootstrap;
import org.pf4j.PluginWrapper;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.*;

public class RatePlugin extends SpringBootPlugin implements CustomPlugin {

    public static RatePlugin INSTANCE;

    public RatePlugin(PluginWrapper wrapper) {
        super(wrapper);
        INSTANCE = this;
    }

    @Override
    protected SpringBootstrap createSpringBootstrap() {
        return new SharedJtaSpringBootstrap(this, RatePluginApplication.class);
    }

    @Override
    public String getTemplateUrl() {
        return "rate";
    }

    @Override
    public List<Map<String, Object>> getLinks(){
        Locale locale = LocaleContextHolder.getLocale();
        String language = locale.getLanguage();

        ResourceBundle bundle = ResourceBundle.getBundle("rate-plugin", Locale.forLanguageTag(language));

        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> link = new HashMap<>();
        link.put("label", bundle.getString("rate.title"));
        link.put("url", "rate/list");
        link.put("cssClass", "ti-money");
        link.put("role", "MANAGER");
        list.add(link);
        return list;
    }

    @Override
    public void stop() {
        releaseAdditionalResources();
        super.stop();
    }

    @Override
    public void releaseAdditionalResources() {
        AtomikosDataSourceBean dataSource = (AtomikosDataSourceBean)
                getApplicationContext().getBean("dataSource");
        dataSource.close();
    }
}
