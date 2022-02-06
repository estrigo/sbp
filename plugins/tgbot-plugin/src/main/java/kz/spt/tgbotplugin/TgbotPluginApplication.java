package kz.spt.tgbotplugin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = {"kz.spt.lib.model", "kz.spt.tgbotplugin.model"})
public class TgbotPluginApplication {
    public static void main(String[] args) {
        SpringApplication.run(TgbotPluginApplication.class, args);
    }
}
