package kz.spt.prkstatusplugin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"kz.spt.lib.model","kz.spt.prkstatusplugin"})
public class PrkstatusPluginStarter {

    public static void main(String[] args) {
        SpringApplication.run(PrkstatusPluginStarter.class, args);
    }

}
