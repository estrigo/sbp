package kz.spt.parkomatplugin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"kz.spt.billingplugin"})
public class ParkomatStatusPluginStarter {

    public static void main(String[] args) {
        SpringApplication.run(ParkomatStatusPluginStarter.class, args);
    }

}
