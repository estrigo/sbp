package kz.spt.zerotouchplugin;

import kz.spt.lib.plugin.CustomPlugin;
import org.laxture.sbp.SpringBootPlugin;
import org.laxture.sbp.spring.boot.SharedDataSourceSpringBootstrap;
import org.laxture.sbp.spring.boot.SpringBootstrap;
import org.pf4j.PluginWrapper;

import java.util.List;
import java.util.Map;

public class ZerotouchPlugin extends SpringBootPlugin implements CustomPlugin {

    public static ZerotouchPlugin INSTANCE;

    public ZerotouchPlugin(PluginWrapper wrapper) {
        super(wrapper);
        INSTANCE = this;
    }

    @Override
    protected SpringBootstrap createSpringBootstrap() {
        return new SharedDataSourceSpringBootstrap(this, ZerotouchPluginStarter.class);
    }

    @Override
    public String getTemplateUrl() {
        return "zerotouch";
    }

    public List<Map<String, Object>> getLinks(){
        return null;
    }
}