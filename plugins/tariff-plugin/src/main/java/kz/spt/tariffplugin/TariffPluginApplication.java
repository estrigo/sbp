package kz.spt.tariffplugin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = {"kz.spt.api.model", "kz.spt.tariffplugin.model"})
public class TariffPluginApplication {

    public static void main(String[] args) {
        SpringApplication.run(TariffPluginApplication.class, args);
    }

}
