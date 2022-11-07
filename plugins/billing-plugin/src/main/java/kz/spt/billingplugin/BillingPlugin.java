package kz.spt.billingplugin;

import kz.spt.billingplugin.service.BalanceService;
import kz.spt.lib.plugin.CustomPlugin;
import org.laxture.sbp.SpringBootPlugin;
import org.laxture.sbp.spring.boot.SpringBootstrap;
import org.modelmapper.ModelMapper;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.i18n.LocaleContextHolder;

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

    @Override
    public List<Map<String, Object>> getLinks(){
        Locale locale = LocaleContextHolder.getLocale();
        String language = locale.toString().equals("de") ? "de" : "en";
        if (locale.toString().equals("ru")) {
            language = "ru-RU";
        }

        ResourceBundle bundle = ResourceBundle.getBundle("billing-plugin", Locale.forLanguageTag(language));
        List<Map<String, Object>> list = new ArrayList<>();

        Map<String, Object> mainMenu = new HashMap<>();
        mainMenu.put("label",bundle.getString("billing.title"));
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

        Map<String, Object> subMenu3 = new HashMap<>();
        subMenu3.put("label", bundle.getString("menu.billing.billing"));
        subMenu3.put("url", "billing/balance/list");
        subMenu3.put("role", "MANAGER");
        subMenus.add(subMenu3);

        Map<String, Object> subMenu5 = new HashMap<>();
        subMenu5.put("label", bundle.getString("menu.report.log"));
        subMenu5.put("url", "billing/payments/log");
        subMenu5.put("role", "MANAGER");
        subMenus.add(subMenu5);

        BalanceService balanceService = super.getApplicationContext() != null ? (BalanceService) super.getApplicationContext().getBean("balanceServiceImpl") : null;

        if(balanceService!= null && balanceService.showBalanceDebtLog()){
            Map<String, Object> subMenu6 = new HashMap<>();
            subMenu6.put("label", bundle.getString("menu.billing.cleared.debts"));
            subMenu6.put("url", "billing/balance/cleared/debts");
            subMenu6.put("role", "MANAGER");
            subMenus.add(subMenu6);
        }

        mainMenu.put("subMenus", subMenus);

        list.add(mainMenu);
        return list;
    }

    @Bean
    public static ModelMapper modelMapper() {
        return new ModelMapper();
    }
}