package kz.smartparking;

import crm.plugin.CustomPlugin;
import org.laxture.sbp.SpringBootPlugin;
import org.laxture.sbp.spring.boot.SpringBootstrap;
import org.pf4j.PluginWrapper;

public class TestPlugin extends SpringBootPlugin implements CustomPlugin {

    public TestPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected SpringBootstrap createSpringBootstrap() {
        return new SpringBootstrap(this, TestPluginStarter.class);
    }

    @Override
    public Boolean hasTemplates() {
        return true;
    }

    @Override
    public String getMenuHtml() {
        return "test.html";
    }
}