package kz.spt.bookingplugin;

import kz.spt.lib.plugin.CustomPlugin;
import org.laxture.sbp.SpringBootPlugin;
import org.laxture.sbp.spring.boot.SharedJtaSpringBootstrap;
import org.laxture.sbp.spring.boot.SpringBootstrap;
import org.pf4j.PluginWrapper;
import com.atomikos.jdbc.AtomikosDataSourceBean;

import java.util.List;
import java.util.Map;

public class BookingPlugin extends SpringBootPlugin implements CustomPlugin {

    public static BookingPlugin INSTANCE;
    public BookingPlugin(PluginWrapper wrapper) {
        super(wrapper);
        INSTANCE = this;
    }

    @Override
    protected SpringBootstrap createSpringBootstrap() {
        return new SharedJtaSpringBootstrap(this, BookingPluginStarter.class);
    }

    @Override
    public String getTemplateUrl() {
        return "booking";
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