package kz.spt.carmodelplugin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = {"kz.spt.lib.model", "kz.spt.carmodelplugin.model"})
public class CarmodelPluginApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarmodelPluginApplication.class, args);
    }

}
