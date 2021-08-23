package kz.spt.tariffplugin;

import kz.spt.api.plugin.CustomPlugin;
import org.laxture.sbp.SpringBootPlugin;
import org.laxture.sbp.spring.boot.SpringBootstrap;
import org.pf4j.PluginWrapper;

public class TariffPlugin extends SpringBootPlugin implements CustomPlugin {

    public static TariffPlugin INSTANCE;

    public TariffPlugin(PluginWrapper wrapper) {
        super(wrapper);
        INSTANCE = this;
    }

    @Override
    protected SpringBootstrap createSpringBootstrap() {
        return new SpringBootstrap(this, TariffPluginApplication.class);
    }

    @Override
    public Boolean hasTemplates() {
        return true;
    }

    @Override
    public String getMenuLabel() {
        return "Tariff list";
    }

    @Override
    public String getMenuUrl() {
        return "tariff/list";
    }

    @Override
    public String getTemplateUrl() {
        return "tariff";
    }

    @Override
    public String getMenuCssClass() {
        return "ti-money";
    }

    @Override
    public String getRole() {
        return "MANAGER";
    }
}
