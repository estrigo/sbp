package kz.spt.zerotouchplugin;

import com.atomikos.jdbc.AtomikosDataSourceBean;
import kz.spt.lib.plugin.CustomPlugin;
import org.laxture.sbp.SpringBootPlugin;
import org.laxture.sbp.spring.boot.SharedJtaSpringBootstrap;
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
        return new SharedJtaSpringBootstrap(this, ZerotouchPluginStarter.class);
    }

    @Override
    public String getTemplateUrl() {
        return "zerotouch";
    }

    public List<Map<String, Object>> getLinks(){
        return null;
    }

    @Override
    public void stop() {
        releaseAdditionalResources();
        super.stop();
    }

    @Override
    public void releaseAdditionalResources() {
        AtomikosDataSourceBean dataSource = (AtomikosDataSourceBean)
                getApplicationContext().getBean("dataSource");
        dataSource.close();
    }
}