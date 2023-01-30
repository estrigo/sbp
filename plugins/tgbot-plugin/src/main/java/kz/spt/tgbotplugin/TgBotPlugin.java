package kz.spt.tgbotplugin;


import com.atomikos.jdbc.AtomikosDataSourceBean;
import org.laxture.sbp.SpringBootPlugin;
import org.laxture.sbp.spring.boot.SharedJtaSpringBootstrap;
import org.laxture.sbp.spring.boot.SpringBootstrap;
import org.pf4j.PluginWrapper;

public class TgBotPlugin extends SpringBootPlugin {
    public static TgBotPlugin INSTANCE;

    public TgBotPlugin(PluginWrapper wrapper) {
        super(wrapper);
        INSTANCE = this;
    }

    @Override
    protected SpringBootstrap createSpringBootstrap() {
        return new SharedJtaSpringBootstrap(this, TgbotPluginApplication.class)
                .importBean("whitelistRepository")
                .importBean("whitelistGroupsRepository");
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
