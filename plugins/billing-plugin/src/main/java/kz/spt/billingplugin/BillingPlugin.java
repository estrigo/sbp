package kz.spt.billingplugin;

import kz.spt.api.plugin.CustomPlugin;
import org.laxture.sbp.SpringBootPlugin;
import org.laxture.sbp.spring.boot.SpringBootstrap;
import org.pf4j.PluginWrapper;

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
    public Boolean hasTemplates() {
        return true;
    }

    @Override
    public String getMenuLabel() {
        return "Billing";
    }

    @Override
    public String getMenuUrl() {
        return "payments/list";
    }

    @Override
    public String getTemplateUrl() {
        return "billing";
    }

    @Override
    public String getMenuCssClass() {
        return "fa fa-money";
    }

    @Override
    public String getRole() {
        return "MANAGER";
    }
}