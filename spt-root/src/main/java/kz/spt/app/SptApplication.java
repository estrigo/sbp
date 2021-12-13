package kz.spt.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.java.Log;
import org.laxture.spring.util.ApplicationContextProvider;
import org.pf4j.AbstractPluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

@Log
@SpringBootApplication
@EntityScan(basePackages = {"kz.spt.lib.model", "kz.spt.app.model"})
public class SptApplication {

    public static void main(String[] args) {
        SpringApplication.run(SptApplication.class, args);
    }

    @Bean
    public ApplicationContextAware multiApplicationContextProviderRegister() {
        return ApplicationContextProvider::registerApplicationContext;
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry
                        .addMapping("/**")
                        .allowedMethods("*")
                        .allowedOrigins("*");
            }
        };
    }

    @Autowired
    public void configureJackson(ObjectMapper objectMapper) {
        objectMapper.setTimeZone(TimeZone.getDefault());

        log.info("Timezone : " + TimeZone.getDefault().getDisplayName());

        int mb = 1024 * 1024;
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long xmx = memoryBean.getHeapMemoryUsage().getMax() / mb;
        long xms = memoryBean.getHeapMemoryUsage().getInit() / mb;
        log.info("Initial Memory (xmx) : " + xmx + "mb");
        log.info("Initial Memory (xms) : " + xms + "mb");
        /*String pluginsDir = System.getProperty(AbstractPluginManager.PLUGINS_DIR_PROPERTY_NAME);
        if (pluginsDir != null && !pluginsDir.isEmpty()) {
            List<Path> paths = Arrays.stream(pluginsDir.split(","))
                    .map(String::trim)
                    .map(Paths::get)
                    .collect(Collectors.toList());
            log.info("1");
            for(Path p:paths){
                log.info(p.toAbsolutePath().toString());
            }
        } else {
            pluginsDir = AbstractPluginManager.DEVELOPMENT_PLUGINS_DIR;
            List<Path> paths = Collections.singletonList(Paths.get(pluginsDir));
            log.info("1");
            for(Path p:paths){
                log.info(p.toAbsolutePath().toString());
            }
        }*/
    }
}
