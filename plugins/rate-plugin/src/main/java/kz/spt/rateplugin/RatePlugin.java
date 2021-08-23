package kz.spt.rateplugin;

import kz.spt.api.plugin.CustomPlugin;
import org.laxture.sbp.SpringBootPlugin;
import org.laxture.sbp.spring.boot.SpringBootstrap;
import org.pf4j.PluginWrapper;

public class RatePlugin extends SpringBootPlugin implements CustomPlugin {

    public static RatePlugin INSTANCE;

    public RatePlugin(PluginWrapper wrapper) {
        super(wrapper);
        INSTANCE = this;
    }

    @Override
    protected SpringBootstrap createSpringBootstrap() {
        return new SpringBootstrap(this, RatePluginApplication.class);
    }

    @Override
    public Boolean hasTemplates() {
        return true;
    }

    @Override
    public String getMenuLabel() {
        return "Rate list";
    }

    @Override
    public String getMenuUrl() {
        return "rate/list";
    }

    @Override
    public String getTemplateUrl() {
        return "rate";
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
