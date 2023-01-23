package kz.spt.tgbotplugin;


import org.laxture.sbp.SpringBootPlugin;
import org.laxture.sbp.spring.boot.SharedDataSourceSpringBootstrap;
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
        return new SharedDataSourceSpringBootstrap(this,TgbotPluginApplication.class)
        .importBean("whitelistRepository")
        .importBean("whitelistGroupsRepository");
    }
}
