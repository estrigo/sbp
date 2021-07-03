package kz.spt.whitelistplugin;

import crm.plugin.CustomPlugin;
import org.laxture.sbp.SpringBootPlugin;
import org.laxture.sbp.spring.boot.SpringBootstrap;
import org.pf4j.PluginWrapper;

public class WhitelistPlugin  extends SpringBootPlugin implements CustomPlugin {

    public WhitelistPlugin(PluginWrapper wrapper) {
        super(wrapper);
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
        return "whitelist";
    }

    @Override
    public String getMenuCssClass() {
        return "ti-file";
    }
}
