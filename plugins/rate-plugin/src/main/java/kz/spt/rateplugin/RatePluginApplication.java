package kz.spt.rateplugin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = {"kz.spt.lib.model", "kz.spt.rateplugin.model"})
public class RatePluginApplication {

    public static void main(String[] args) {
        SpringApplication.run(RatePluginApplication.class, args);
    }

}
