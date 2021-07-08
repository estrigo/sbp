package kz.spt.whitelistplugin;

import crm.model.Cars;
import kz.spt.whitelistplugin.model.Whitelist;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = {"crm.model", "kz.spt.whitelistplugin.model"}, basePackageClasses = {Cars.class, Whitelist.class})
public class WhitelistPluginApplication {

    public static void main(String[] args) {
        SpringApplication.run(WhitelistPluginApplication.class, args);
    }

}
