package kz.spt.billingplugin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = {"kz.spt.lib.model", "kz.spt.billingplugin.model"})
public class BillingPluginStarter {

    public static void main(String[] args) {
        SpringApplication.run(BillingPluginStarter.class, args);
    }

}
