package kz.spt.whitelistplugin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = {"kz.spt.lib.model", "kz.spt.whitelistplugin.model"})
public class WhitelistPluginApplication {

    public static void main(String[] args) {
        SpringApplication.run(WhitelistPluginApplication.class, args);
    }

}
