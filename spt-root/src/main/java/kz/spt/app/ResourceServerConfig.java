package kz.spt.app;

import kz.spt.app.config.CorsAllowedOrigins;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collections;


@Log
@Configuration
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {

    private CorsAllowedOrigins corsAllowedOrigins;

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http
                .cors().configurationSource(configurationSource()).and()
                .antMatcher("/admin/**")
                .authorizeRequests()
                .antMatchers("/admin/**")
                .authenticated();

    }

    @Autowired
    public void configureGlobal(CorsAllowedOrigins corsAllowedOrigins) throws Exception {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    private CorsConfigurationSource configurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Collections.singletonList("*"));
        config.setAllowedMethods(Collections.singletonList("*"));
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}