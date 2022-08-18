package kz.spt.app.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import net.bull.javamelody.MonitoringFilter;
import net.bull.javamelody.SessionListener;

@Configuration
public class FilterConfig {
    /**
     * Настройка мониторинга javamelody
     * Spring boot будет фильтровать по порядку в соответствии со стоимостью заказа, от малого до большого
     */
    @Bean
    @Order(Integer.MAX_VALUE-1)
    public FilterRegistrationBean<MonitoringFilter> monitoringFilter() {
        FilterRegistrationBean<MonitoringFilter>  registration = new FilterRegistrationBean<MonitoringFilter>();
        registration.setFilter(new MonitoringFilter());
        registration.addUrlPatterns("/*");
        registration.setName("monitoring");
        return registration;
    }

    /**
     * Настроить javamelody слушатель sessionListener
     */
    @Bean
    public ServletListenerRegistrationBean<SessionListener> servletListenerRegistrationBean() {
        ServletListenerRegistrationBean<SessionListener> slrBean = new ServletListenerRegistrationBean<SessionListener>();
        slrBean.setListener(new SessionListener());
        return slrBean;
    }
}
