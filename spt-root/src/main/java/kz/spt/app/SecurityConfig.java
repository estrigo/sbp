package kz.spt.app;

import kz.spt.app.auth.DefaultUrlAuthenticationSuccessHandler;
import kz.spt.app.config.CorsAllowedOrigins;
import kz.spt.lib.plugin.CustomPlugin;
import kz.spt.app.service.SpringDataUserDetailsService;
import lombok.extern.java.Log;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Log
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final PluginManager pluginManager;

    public SecurityConfig(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SpringDataUserDetailsService customUserDetailsService() {
        return new SpringDataUserDetailsService();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(customUserDetailsService()).passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.httpBasic().and().authorizeRequests()
                .antMatchers("/balance/**").hasAnyRole("ADMIN", "BAQORDA")
                .antMatchers("/users/delete/**").hasAnyRole("ADMIN", "OPERATOR", "READ")
                .antMatchers( "/events/**", "/journal/**", "/arm/**").hasAnyRole("AUDIT", "ADMIN", "MANAGER", "OPERATOR", "OPERATOR_NO_REVENUE_SHARE","RTA","ACCOUNTANT", "READ")
                .antMatchers( "/customers/**", "/register/**", "/cars/**", "/parking/**", "/customer/**").hasAnyRole("AUDIT", "ADMIN", "MANAGER", "READ")
                .antMatchers("/customer/edit/**", "/users/**", "/cars/edit/**","/parking/edit/**","/parking/details/**").hasAnyRole( "ADMIN", "MANAGER")
                .antMatchers("/parking/**").hasAnyRole("ADMIN", "OPERATOR_NO_REVENUE_SHARE", "READ")
                .antMatchers("/rest/external/**").fullyAuthenticated()
                .antMatchers("/admin-place/**").fullyAuthenticated();

        List<PluginWrapper> plugins = pluginManager.getPlugins();

        for (PluginWrapper pluginWrapper : plugins) {
            if (pluginWrapper.getPlugin() instanceof CustomPlugin) {
                CustomPlugin plugin = (CustomPlugin) pluginWrapper.getPlugin();

                if (plugin.getTemplateUrl() != null) {
                    http.authorizeRequests().antMatchers("/" + plugin.getTemplateUrl() + "/**").fullyAuthenticated();
                }

                if (plugin.getLinks() != null) {
                    for (Map<String, Object> link : plugin.getLinks()) {
                        if (link.containsKey("url") && link.containsKey("role")) {
                            http.authorizeRequests().antMatchers("/" + link.get("url")).hasRole((String) link.get("role"));
                        }
                        if (link.containsKey("subMenus")) {
                            List<Map<String, Object>> subMenus = (List<Map<String, Object>>) link.get("subMenus");
                            for (Map<String, Object> subLink : subMenus) {
                                if (subLink.containsKey("url") && subLink.containsKey("role")) {
                                    http.authorizeRequests().antMatchers("/" + subLink.get("url")).hasRole((String) subLink.get("role"));
                                }
                            }
                        }
                    }
                }
            }
        }

        http.authorizeRequests()
                .anyRequest().permitAll()
                .and()
                .formLogin().loginPage("/login").successHandler(defaultAuthenticationSuccessHandler())
                .and()
                .logout().logoutSuccessUrl("/login").permitAll()
                .and()
                .exceptionHandling().accessDeniedPage("/403");
        http.csrf().disable();
    }

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public AuthenticationSuccessHandler defaultAuthenticationSuccessHandler(){
        return new DefaultUrlAuthenticationSuccessHandler();
    }
}
