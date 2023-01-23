package kz.spt.qrpanel;

import kz.spt.lib.plugin.CustomPlugin;
import org.laxture.sbp.SpringBootPlugin;
import org.laxture.sbp.spring.boot.SharedDataSourceSpringBootstrap;
import org.laxture.sbp.spring.boot.SpringBootstrap;
import org.pf4j.PluginWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QrpanelPlugin extends SpringBootPlugin implements CustomPlugin {

    public QrpanelPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected SpringBootstrap createSpringBootstrap() {
        return new SharedDataSourceSpringBootstrap(this, QrpanelPluginStarter.class);
    }


    @Override
    public String getTemplateUrl() {
        return null;
    }

    public List<Map<String, Object>> getLinks(){
        return  new ArrayList<>();
    }
}