package kz.spt.billingplugin;

import kz.spt.api.plugin.CustomPlugin;
import org.laxture.sbp.SpringBootPlugin;
import org.laxture.sbp.spring.boot.SpringBootstrap;
import org.modelmapper.ModelMapper;
import org.pf4j.PluginWrapper;
import org.springframework.context.annotation.Bean;

import java.util.*;

public class BillingPlugin extends SpringBootPlugin implements CustomPlugin {
    public static BillingPlugin INSTANCE;
    public BillingPlugin(PluginWrapper wrapper) {
        super(wrapper);
        INSTANCE = this;
    }

    @Override
    protected SpringBootstrap createSpringBootstrap() {
        return new SpringBootstrap(this, BillingPluginStarter.class);
    }

    @Override
    public String getTemplateUrl() {
        return "billing";
    }

    public List<Map<String, Object>> getLinks(){
        ResourceBundle bundle = ResourceBundle.getBundle("billing-plugin", Locale.forLanguageTag("ru-RU"));
        List<Map<String, Object>> list = new ArrayList<>();

        Map<String, Object> mainMenu = new HashMap<>();
        mainMenu.put("label",bundle.getString("menu.billing.title"));
        mainMenu.put("cssClass", "fa fa-money");
        mainMenu.put("role", "MANAGER");

        List<Map<String, Object>> subMenus = new ArrayList<>();

        Map<String, Object> subMenu1 = new HashMap<>();
        subMenu1.put("label", bundle.getString("menu.billing.payments"));
        subMenu1.put("url", "billing/payments/list");
        subMenu1.put("role", "MANAGER");
        subMenus.add(subMenu1);

        Map<String, Object> subMenu2 = new HashMap<>();
        subMenu2.put("label", bundle.getString("menu.billing.providers"));
        subMenu2.put("url", "billing/providers/list");
        subMenu2.put("role", "MANAGER");
        subMenus.add(subMenu2);

        mainMenu.put("subMenus", subMenus);

        list.add(mainMenu);
        return list;
    }

    @Bean
    public static ModelMapper modelMapper() {
        return new ModelMapper();
    }
}