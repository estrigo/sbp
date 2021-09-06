package kz.spt.billingplugin;

import kz.spt.api.plugin.CustomPlugin;
import org.laxture.sbp.SpringBootPlugin;
import org.laxture.sbp.spring.boot.SpringBootstrap;
import org.modelmapper.ModelMapper;
import org.pf4j.PluginWrapper;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> link = new HashMap<>();
        link.put("label", "Billing");
        link.put("url", "billing/payments/list");
        link.put("cssClass", "fa fa-money");
        link.put("role", "MANAGER");
        list.add(link);
        return list;
    }

    @Bean
    public static ModelMapper modelMapper() {
        return new ModelMapper();
    }
}