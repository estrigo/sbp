package kz.spt.carmodelplugin;

import com.atomikos.jdbc.AtomikosDataSourceBean;
import kz.spt.lib.plugin.CustomPlugin;
import org.laxture.sbp.SpringBootPlugin;
import org.laxture.sbp.spring.boot.SharedJtaSpringBootstrap;
import org.laxture.sbp.spring.boot.SpringBootstrap;
import org.pf4j.PluginWrapper;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.*;

public class CarmodelPlugin extends SpringBootPlugin implements CustomPlugin {

    public static CarmodelPlugin INSTANCE;

    public CarmodelPlugin(PluginWrapper wrapper) {
        super(wrapper);
        INSTANCE = this;
    }

    @Override
    protected SpringBootstrap createSpringBootstrap() {
        return new SharedJtaSpringBootstrap(this, CarmodelPluginApplication.class);
    }

    @Override
    public String getTemplateUrl() {
        return "carmodel";
    }

    public List<Map<String, Object>> getLinks(){
        Locale locale = LocaleContextHolder.getLocale();
        String language = locale.getLanguage();

        ResourceBundle bundle = ResourceBundle.getBundle("carmodel-plugin", Locale.forLanguageTag(language));
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> mainMenu = new HashMap<>();
        mainMenu.put("label", bundle.getString("carmodel.title"));
        mainMenu.put("url", "carmodel/list");
        mainMenu.put("cssClass", "ti-file");
        mainMenu.put("role", "MANAGER");
        list.add(mainMenu);

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