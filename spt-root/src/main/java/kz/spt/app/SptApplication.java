package kz.spt.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.java.Log;
import org.laxture.spring.util.ApplicationContextProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.*;

@Log
@SpringBootApplication
@EntityScan(basePackages = {"kz.spt.lib.model", "kz.spt.app.model"})
@EnableScheduling
public class SptApplication {

    public static void main(String[] args) {
        SpringApplication.run(SptApplication.class, args);
    }

    @Bean
    public ApplicationContextAware multiApplicationContextProviderRegister() {
        return ApplicationContextProvider::registerApplicationContext;
    }

    @Autowired
    public void configureJackson(ObjectMapper objectMapper) {
        objectMapper.setTimeZone(TimeZone.getDefault());

        log.info("Timezone : " + TimeZone.getDefault().getDisplayName());

        int mb = 1024 * 1024;
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long xmx = memoryBean.getHeapMemoryUsage().getMax() / mb;
        long xms = memoryBean.getHeapMemoryUsage().getInit() / mb;
        log.info("Maximum Memory (xmx) : " + xmx + "mb");
        log.info("Initial Memory (xms) : " + xms + "mb");
    }

    @Bean
    public ServletServerContainerFactoryBean createServletServerContainerFactoryBean() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(1024 * 1024);
        container.setMaxBinaryMessageBufferSize(1024 * 1024);
        log.info("Websocket factory returned");
        return container;
    }
}
