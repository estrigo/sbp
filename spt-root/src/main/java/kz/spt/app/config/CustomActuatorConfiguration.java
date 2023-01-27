package kz.spt.app.config;

import kz.spt.app.controller.PluginsStatusEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomActuatorConfiguration {

    @Bean
    public PluginsStatusEndpoint pluginsStatusEndpoint() {
        return new PluginsStatusEndpoint();
    }
}
