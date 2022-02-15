package kz.spt.abonomentplugin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = {"kz.spt.lib.model", "kz.spt.abonomentplugin.model"})
public class AbonomentPluginStarter {

    public static void main(String[] args) {
        SpringApplication.run(AbonomentPluginStarter.class, args);
    }

}
