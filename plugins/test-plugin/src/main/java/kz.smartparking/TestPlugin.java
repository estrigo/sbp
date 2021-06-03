package kz.smartparking;

import org.laxture.sbp.SpringBootPlugin;
import org.laxture.sbp.spring.boot.SpringBootstrap;
import org.pf4j.PluginWrapper;

public class TestPlugin extends SpringBootPlugin {

    public TestPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected SpringBootstrap createSpringBootstrap() {
        return new SpringBootstrap(this, TestPluginStarter.class);
    }
}