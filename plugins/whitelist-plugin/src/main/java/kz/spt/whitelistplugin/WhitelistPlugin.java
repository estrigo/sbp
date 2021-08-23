package kz.spt.whitelistplugin;

import kz.spt.api.plugin.CustomPlugin;
import org.laxture.sbp.SpringBootPlugin;
import org.laxture.sbp.spring.boot.SpringBootstrap;
import org.pf4j.PluginWrapper;

public class WhitelistPlugin extends SpringBootPlugin implements CustomPlugin {

    public static WhitelistPlugin INSTANCE;

    public WhitelistPlugin(PluginWrapper wrapper) {
        super(wrapper);
        INSTANCE = this;
    }

    @Override
    protected SpringBootstrap createSpringBootstrap() {
        return new SpringBootstrap(this, WhitelistPluginApplication.class);
    }

    @Override
    public Boolean hasTemplates() {
        return true;
    }

    @Override
    public String getMenuLabel() {
        return "White list";
    }

    @Override
    public String getMenuUrl() {
        return "whitelist/list";
    }

    @Override
    public String getTemplateUrl() {
        return "whitelist";
    }

    @Override
    public String getMenuCssClass() {
        return "ti-file";
    }

    @Override
    public String getRole() {
        return "MANAGER";
    }
}
